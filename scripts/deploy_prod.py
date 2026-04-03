import paramiko, sys, os

HOST = '192.168.1.200'
USER = 'saleadmin'
PASS = 'Kcgqhdltd1@'
REMOTE_BASE = '/home/saleadmin/sales-management-deploy/current'
LOCAL_BASE = os.path.join(os.path.dirname(os.path.dirname(__file__)))

# Files changed in the UI/UX fix + backend improvement session
CHANGED_FILES = [
    # Auth security hotfix: prevent public role escalation
    'src/main/java/com/sales/management/auth/AuthController.java',
    'src/main/java/com/sales/management/auth/AuthService.java',
    'src/main/java/com/sales/management/auth/dto/PublicRegisterRequest.java',

    # DB migration — V21 adds category + vat_rate columns
    'src/main/resources/db/migration/V21__add_product_category_and_vat.sql',

    # Product entity + full product package (has category/vatRate changes)
    'src/main/java/com/sales/management/product/Product.java',
    'src/main/java/com/sales/management/product/ProductService.java',
    'src/main/java/com/sales/management/product/ProductController.java',
    'src/main/java/com/sales/management/product/ProductRepository.java',
    'src/main/java/com/sales/management/product/ProductOptionsResponse.java',
    'src/main/java/com/sales/management/product/dto/ProductCreateRequest.java',
    'src/main/java/com/sales/management/product/dto/ProductResponse.java',

    # Product test files (updated with correct ProductService.list() signature)
    'src/test/java/com/sales/management/product/ProductCachingIntegrationTest.java',
    'src/test/java/com/sales/management/product/ProductServiceUnitTest.java',

    # Order service (uses product.getVatRate())
    'src/main/java/com/sales/management/order/SalesOrderService.java',
    'src/test/java/com/sales/management/order/SalesOrderIntegrationTest.java',

    # Frontend lib types/api (used by all pages)
    'frontend/src/lib/types.ts',
    'frontend/src/lib/api.ts',

    # UI pages with UI/UX fixes
    'frontend/src/pages/ProductsPage.tsx',
    'frontend/src/pages/OrdersPage.tsx',
    'frontend/src/pages/FinancePage.tsx',
    'frontend/src/pages/ReportsPage.tsx',
    'frontend/src/pages/ReturnsPage.tsx',
    'frontend/src/pages/UsersPage.tsx',
]

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

print('\n--- STEP 5: Backend health ping ---')
run(client, 'curl -sf http://localhost:8080/api/v1/products?page=0&size=1 -o /dev/null && echo BACKEND_OK || echo BACKEND_FAIL', 15)

print('\n--- STEP 6: Frontend health ping ---')
run(client, 'curl -sf http://localhost/ -o /dev/null && echo FRONTEND_OK || echo FRONTEND_FAIL', 10)

client.close()
print('\nDEPLOY_RESULT=DONE')

