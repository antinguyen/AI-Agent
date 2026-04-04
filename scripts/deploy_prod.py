import paramiko, sys, os, subprocess, json
from datetime import datetime, timezone

EXIT_SUCCESS = 0
EXIT_GENERAL_ERROR = 1
EXIT_INVALID_ARGS = 2
EXIT_BUILD_FAILED = 3
EXIT_DEPLOY_FAILED = 4

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
NO_BUILD = '--no-build' in sys.argv
PRINT_JSON = '--print-json' in sys.argv
PRINT_JSON_ONLY = '--print-json-only' in sys.argv
HELP = '--help' in sys.argv or '-h' in sys.argv


def utc_now_iso():
    return datetime.now(timezone.utc).isoformat()


def exit_with_payload(exit_code, status='success', message=None, error_code=None, data=None):
    if PRINT_JSON or PRINT_JSON_ONLY:
        payload = {
            'timestampUtc': utc_now_iso(),
            'exitCode': exit_code,
            'status': status,
        }
        if data:
            payload.update(data)
        if message:
            payload['message'] = message
        if error_code or status == 'error':
            payload['error'] = {
                'code': error_code or 'unknown_error',
                'message': message or 'Unknown error',
            }
        print(json.dumps(payload, ensure_ascii=False))
    sys.exit(exit_code)


def print_usage():
    print('Usage: python scripts/deploy_prod.py [options]')
    print('Options:')
    print('  --help, -h            Show this help message')
    print('  --dry-run             Resolve file list and stop before SSH/deploy')
    print('  --force               Deploy using DEFAULT_DEPLOY_FILES list')
    print('  --base-ref <ref>      Deploy files changed from <ref>...HEAD')
    print('  --no-build            Skip docker compose build step')
    print('  --print-json          Print machine-readable JSON summary')
    print('  --print-json-only     Print JSON only (no text logs, implies --dry-run)')
    print('  --verbose             Print skipped-file reasons')
    print('Exit codes:')
    print(f'  {EXIT_SUCCESS}: success/no-op')
    print(f'  {EXIT_GENERAL_ERROR}: general failure')
    print(f'  {EXIT_INVALID_ARGS}: invalid arguments')
    print(f'  {EXIT_BUILD_FAILED}: build step failed')
    print(f'  {EXIT_DEPLOY_FAILED}: deploy/upload failed')


def parse_option_value(option_name):
    if option_name not in sys.argv:
        return None
    index = sys.argv.index(option_name)
    if index + 1 >= len(sys.argv):
        exit_with_payload(EXIT_INVALID_ARGS, status='error', message=f'Missing value for {option_name}', error_code='missing_option_value')
    return sys.argv[index + 1].strip()


BASE_REF = parse_option_value('--base-ref')


def validate_args():
    known_switches = {
        '--help', '-h', '--force', '--dry-run', '--verbose', '--no-build', '--print-json', '--print-json-only', '--base-ref',
    }
    skip_next = False
    for index, arg in enumerate(sys.argv[1:], start=1):
        if skip_next:
            skip_next = False
            continue
        if arg == '--base-ref':
            if index + 1 >= len(sys.argv):
                exit_with_payload(EXIT_INVALID_ARGS, status='error', message='Missing value for --base-ref', error_code='missing_option_value')
            skip_next = True
            continue
        if arg.startswith('-') and arg not in known_switches:
            exit_with_payload(EXIT_INVALID_ARGS, status='error', message=f'Unknown option: {arg}', error_code='unknown_option')


if HELP:
    print_usage()
    exit_with_payload(EXIT_SUCCESS, status='success', message='Help displayed')

validate_args()

if FORCE_DEPLOY and BASE_REF:
    exit_with_payload(EXIT_INVALID_ARGS, status='error', message='Invalid options: --force cannot be used together with --base-ref', error_code='invalid_option_combo')
if PRINT_JSON_ONLY and not DRY_RUN:
    exit_with_payload(EXIT_INVALID_ARGS, status='error', message='Invalid options: --print-json-only requires --dry-run', error_code='invalid_option_combo')
if PRINT_JSON_ONLY:
    PRINT_JSON = True


def git_ref_exists(ref_name):
    if not ref_name:
        return False
    cmd = ['git', '-C', LOCAL_BASE, 'rev-parse', '--verify', '--quiet', ref_name]
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.returncode == 0


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


def resolve_changed_files(force_mode=False, base_ref=None):
    combined = []
    seen = set()
    skipped = []

    if base_ref:
        if not git_ref_exists(base_ref):
            exit_with_payload(EXIT_INVALID_ARGS, status='error', message=f'Invalid --base-ref: {base_ref}', error_code='invalid_base_ref')
        arg_sets = (
            ['diff', '--name-only', f'{base_ref}...HEAD'],
            ['diff', '--name-only', '--cached'],
            ['diff', '--name-only'],
            ['ls-files', '--others', '--exclude-standard'],
        )
    else:
        arg_sets = (
            ['diff', '--name-only', '--cached'],
            ['diff', '--name-only'],
            ['ls-files', '--others', '--exclude-standard'],
            ['show', '--name-only', '--pretty=format:', 'HEAD'],
        )

    for args in arg_sets:
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
        mode = f'BASE_REF({base_ref})' if base_ref else 'GIT_CHANGES'
        return deployable, mode, combined, skipped

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

CHANGED_FILES, selection_mode, all_candidates, skipped_files = resolve_changed_files(FORCE_DEPLOY, BASE_REF)
payload_context = {
        'selectionMode': selection_mode,
        'baseRef': BASE_REF,
        'forceDeploy': FORCE_DEPLOY,
        'dryRun': DRY_RUN,
        'noBuild': NO_BUILD,
        'printJsonOnly': PRINT_JSON_ONLY,
        'selectedCount': len(CHANGED_FILES),
        'selectedFiles': CHANGED_FILES,
        'candidateCount': len(all_candidates),
        'isNoOp': len(CHANGED_FILES) == 0,
}
if VERBOSE:
    payload_context['skippedFiles'] = [
        {'path': path, 'reason': reason}
        for path, reason in skipped_files
    ]
if PRINT_JSON_ONLY:
    exit_with_payload(EXIT_SUCCESS, status='success', data=payload_context)
if PRINT_JSON:
    print(json.dumps({'timestampUtc': utc_now_iso(), 'exitCode': EXIT_SUCCESS, 'status': 'success', **payload_context}, ensure_ascii=False))
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
    exit_with_payload(EXIT_SUCCESS, status='success', message='No deployable files detected from git changes; nothing to deploy', data=payload_context)
if DRY_RUN:
    print('Dry-run mode enabled; stopping before SSH/deploy steps')
    exit_with_payload(EXIT_SUCCESS, status='success', message='Dry-run mode enabled; stopping before SSH/deploy steps', data=payload_context)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    client.connect(HOST, username=USER, password=PASS, timeout=10)
except Exception as ex:
    exit_with_payload(EXIT_DEPLOY_FAILED, status='error', message=f'Failed to connect SSH host: {ex}', error_code='ssh_connect_failed', data=payload_context)
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
        client.close()
        exit_with_payload(EXIT_DEPLOY_FAILED, status='error', message=f'Failed to upload file: {rel_path}', error_code='upload_failed', data=payload_context)
sftp.close()

# ── STEP 2: Rebuild ONLY the app (backend) — frontend already rebuilt ──
if NO_BUILD:
    print('\n--- STEP 2: Build skipped (--no-build) ---')
else:
    print('\n--- STEP 2: docker compose build app (frontend already done) ---')
    rc = run(client, f'cd {REMOTE_BASE} && docker compose build --no-cache app 2>&1', 900)
    if rc != 0:
        print('BUILD FAILED — aborting')
        client.close()
        exit_with_payload(EXIT_BUILD_FAILED, status='error', message='Build step failed', error_code='build_failed', data=payload_context)

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
exit_with_payload(EXIT_SUCCESS, status='success', message='Deploy completed', data=payload_context)

