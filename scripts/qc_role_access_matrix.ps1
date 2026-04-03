$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

function Get-Token($username, $password) {
  $body = @{ username = $username; password = $password } | ConvertTo-Json -Compress
  $res = Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method POST -ContentType 'application/json' -Body $body -TimeoutSec 30
  if ($res.token) { return $res.token }
  return $res.accessToken
}

function Get-Status($uri, $headers) {
  try {
    Invoke-RestMethod -Uri $uri -Headers $headers -Method GET -TimeoutSec 30 | Out-Null
    return 200
  } catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      return [int]$_.Exception.Response.StatusCode
    }
    throw
  }
}

function Print-Case($name, $status, $expected) {
  $pass = $expected -contains $status
  $result = if ($pass) { 'PASS' } else { 'FAIL' }
  Write-Output "RBAC_CASE=$name RESULT=$result STATUS=$status EXPECTED=$($expected -join ',')"
  return $pass
}

$allPass = $true

# Admin default creds from README
$adminToken = Get-Token -username 'admin1' -password 'password123'
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

$staffUser = 'qc_staff_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$staffPass = 'QcFlow#2026'
$register = @{ username = $staffUser; password = $staffPass } | ConvertTo-Json -Compress
$staffReg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $register -TimeoutSec 30
$staffToken = if ($staffReg.token) { $staffReg.token } else { $staffReg.accessToken }
$staffHeaders = @{ Authorization = "Bearer $staffToken" }

$adminUsers = Get-Status -uri "$base/api/v1/users?page=0&size=1" -headers $adminHeaders
if (-not (Print-Case -name 'ADMIN_CAN_LIST_USERS' -status $adminUsers -expected @(200))) { $allPass = $false }

$staffUsers = Get-Status -uri "$base/api/v1/users?page=0&size=1" -headers $staffHeaders
if (-not (Print-Case -name 'STAFF_CANNOT_LIST_USERS' -status $staffUsers -expected @(403))) { $allPass = $false }

$adminRevenue = Get-Status -uri "$base/api/v1/reports/revenue?from=2026-04-01&to=2026-04-03" -headers $adminHeaders
if (-not (Print-Case -name 'ADMIN_CAN_VIEW_REVENUE_REPORT' -status $adminRevenue -expected @(200))) { $allPass = $false }

$staffRevenue = Get-Status -uri "$base/api/v1/reports/revenue?from=2026-04-01&to=2026-04-03" -headers $staffHeaders
if (-not (Print-Case -name 'STAFF_CANNOT_VIEW_REVENUE_REPORT' -status $staffRevenue -expected @(403))) { $allPass = $false }

$staffProducts = Get-Status -uri "$base/api/v1/products?page=0&size=1" -headers $staffHeaders
if (-not (Print-Case -name 'STAFF_CAN_VIEW_PRODUCTS' -status $staffProducts -expected @(200))) { $allPass = $false }

if ($allPass) {
  Write-Output 'RBAC_SUMMARY=PASS'
  exit 0
}
Write-Output 'RBAC_SUMMARY=FAIL'
exit 1
