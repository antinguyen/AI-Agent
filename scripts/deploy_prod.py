import paramiko, sys, os, subprocess

HOST = '192.168.1.200'
USER = 'saleadmin'
PASS = 'Kcgqhdltd1@'
REMOTE_BASE = '/home/saleadmin/sales-management-deploy/current'
LOCAL_BASE = os.path.join(os.path.dirname(os.path.dirname(__file__)))

DEFAULT_DEPLOY_FILES = [
    'src/main/java/com/sales/management/auth/AuthController.java',
    'src/main/java/com/sales/management/auth/AuthService.java',
    'src/main/java/com/sales/management/auth/dto/PublicRegisterRequest.java',
    'src/main/resources/db/migration/V21__add_product_category_and_vat.sql',
    'src/main/java/com/sales/management/product/Product.java',
    'src/main/java/com/sales/management/product/ProductService.java',
    'src/main/java/com/sales/management/product/ProductController.java',
    'src/main/java/com/sales/management/product/ProductRepository.java',
    'src/main/java/com/sales/management/product/ProductOptionsResponse.java',
    'src/main/java/com/sales/management/product/dto/ProductCreateRequest.java',
    'src/main/java/com/sales/management/product/dto/ProductResponse.java',
    'src/test/java/com/sales/management/product/ProductCachingIntegrationTest.java',
    'src/test/java/com/sales/management/product/ProductServiceUnitTest.java',
    'src/main/java/com/sales/management/order/SalesOrderService.java',
    'src/test/java/com/sales/management/order/SalesOrderIntegrationTest.java',
    'frontend/src/lib/types.ts',
    'frontend/src/lib/api.ts',
    'frontend/src/pages/ProductsPage.tsx',
    'frontend/src/pages/OrdersPage.tsx',
    'frontend/src/pages/FinancePage.tsx',
    'frontend/src/pages/ReportsPage.tsx',
    'frontend/src/pages/ReturnsPage.tsx',
    'frontend/src/pages/UsersPage.tsx',
]

IGNORED_PATH_PREFIXES = (
    '.git/',
    'target/',
    'node_modules/',
    'scripts/out/',
)

ALLOWED_PATH_PREFIXES = (
    'src/',
    'frontend/',
)

ALLOWED_EXACT_FILES = {
    'pom.xml',
    'Dockerfile',
    'docker-compose.yml',
}

FORCE_DEPLOY = '--force' in sys.argv
DRY_RUN = '--dry-run' in sys.argv
VERBOSE = '--verbose' in sys.argv


def collect_git_file_list(args):
    cmd = ['git', '-C', LOCAL_BASE, *args]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        return []
    return [line.strip().replace('\\', '/') for line in result.stdout.splitlines() if line.strip()]


def get_non_deployable_reason(rel_path):
    normalized = rel_path.replace('\\', '/').strip()
    if not normalized:
        return 'empty-path'
    if any(normalized.startswith(prefix) for prefix in IGNORED_PATH_PREFIXES):
        return 'ignored-prefix'
    if normalized.endswith('.pyc'):
        return 'compiled-python-artifact'
    if normalized in ALLOWED_EXACT_FILES:
        return None
    if any(normalized.startswith(prefix) for prefix in ALLOWED_PATH_PREFIXES):
        return None
    return 'outside-deploy-scope'


def is_deployable_file(rel_path):
    return get_non_deployable_reason(rel_path) is None


def resolve_changed_files(force_mode=False):
    combined = []
    seen = set()
    skipped = []

    for args in (
        ['diff', '--name-only', '--cached'],
        ['diff', '--name-only'],
        ['ls-files', '--others', '--exclude-standard'],
        ['show', '--name-only', '--pretty=format:', 'HEAD'],
    ):
        for rel_path in collect_git_file_list(args):
            if rel_path in seen:
                continue
            seen.add(rel_path)
            combined.append(rel_path)

    deployable = []
    for rel_path in combined:
        reason = get_non_deployable_reason(rel_path)
        if reason:
            skipped.append((rel_path, reason))
            continue
        local_path = os.path.join(LOCAL_BASE, rel_path.replace('/', os.sep))
        if os.path.isfile(local_path):
            deployable.append(rel_path)
        else:
            skipped.append((rel_path, 'file-not-found'))

    if force_mode:
        fallback = []
        for rel_path in DEFAULT_DEPLOY_FILES:
            local_path = os.path.join(LOCAL_BASE, rel_path.replace('/', os.sep))
            if os.path.isfile(local_path):
                fallback.append(rel_path)
        return fallback, 'FORCE_DEFAULT_LIST', combined, skipped

    if combined:
        return deployable, 'GIT_CHANGES', combined, skipped

    fallback = []
    for rel_path in DEFAULT_DEPLOY_FILES:
        local_path = os.path.join(LOCAL_BASE, rel_path.replace('/', os.sep))
        if os.path.isfile(local_path):
            fallback.append(rel_path)
    return fallback, 'DEFAULT_BOOTSTRAP', combined, skipped

def run(client, cmd, timeout=60):
    print(f'  $ {cmd}')
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode()
    err = stderr.read().decode()
    if out.strip(): print(out.strip())
    if err.strip(): print('[STDERR]', err.strip()[:600])
    rc = stdout.channel.recv_exit_status()
    print(f'  [exit {rc}]')
    return rc

CHANGED_FILES, selection_mode, all_candidates, skipped_files = resolve_changed_files(FORCE_DEPLOY)
print(f'Selection mode: {selection_mode}')
print(f'Prepared {len(CHANGED_FILES)} file(s) for upload')
if CHANGED_FILES:
    print('Selected files:')
    for rel_path in CHANGED_FILES:
        print(f'  - {rel_path}')
if VERBOSE and skipped_files:
    print('Skipped files:')
    for rel_path, reason in skipped_files:
        print(f'  - {rel_path} ({reason})')
if not CHANGED_FILES:
    print('No deployable files detected from git changes; nothing to deploy')
    sys.exit(0)
if DRY_RUN:
    print('Dry-run mode enabled; stopping before SSH/deploy steps')
    sys.exit(0)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASS, timeout=10)
print(f'Connected to {HOST}')

# ── STEP 1: Upload changed source files via SFTP ──
print('\n--- STEP 1: Upload changed source files ---')
sftp = client.open_sftp()
for rel_path in CHANGED_FILES:
    local_path = os.path.join(LOCAL_BASE, rel_path.replace('/', os.sep))
    remote_path = f'{REMOTE_BASE}/{rel_path}'
    remote_dir = remote_path.rsplit('/', 1)[0]
    try:
        run(client, f'mkdir -p {remote_dir}', 10)
        sftp.put(local_path, remote_path)
        print(f'  UPLOADED: {rel_path}')
    except Exception as e:
        print(f'  FAILED:   {rel_path} -> {e}')
sftp.close()

# ── STEP 2: Rebuild ONLY the app (backend) — frontend already rebuilt ──
print('\n--- STEP 2: docker compose build app (frontend already done) ---')
rc = run(client, f'cd {REMOTE_BASE} && docker compose build --no-cache app 2>&1', 900)
if rc != 0:
    print('BUILD FAILED — aborting')
    client.close()
    sys.exit(1)

# ── STEP 3: Bring up new containers ──
print('\n--- STEP 3: docker compose up -d ---')
run(client, f'cd {REMOTE_BASE} && docker compose up -d 2>&1', 120)

# ── STEP 4: Health check ──
print('\n--- STEP 4: Container status ---')
run(client, 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"', 15)

print('\n--- STEP 5: Backend health ping (wait up to 60s for Spring Boot) ---')
run(
    client,
    "for i in $(seq 1 12); do "
    "  code=$(curl -s -o /dev/null -w '%{http_code}' 'http://localhost:8080/api/v1/products?page=0&size=1' || true); "
    "  if [ \"$code\" = \"200\" ] || [ \"$code\" = \"401\" ] || [ \"$code\" = \"403\" ]; then "
    "    echo BACKEND_OK_HTTP_$code; "
    "    break; "
    "  fi; "
    "  echo \"Waiting for backend... attempt $i/12 (http=$code)\"; "
    "  sleep 5; "
    "done",
    75,
)

print('\n--- STEP 6: Frontend health ping ---')
run(client, 'curl -sf http://localhost/ -o /dev/null && echo FRONTEND_OK || echo FRONTEND_FAIL', 10)

client.close()
print('\nDEPLOY_RESULT=DONE')

