$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

function Get-StatusCode {
  param([scriptblock]$Action)
  try {
    & $Action | Out-Null
    return 200
  } catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      return [int]$_.Exception.Response.StatusCode
    }
    throw
  }
}

function Assert-Case {
  param(
    [string]$Name,
    [int]$Actual,
    [int[]]$Expected
  )
  $pass = $Expected -contains $Actual
  $expectedText = ($Expected -join ',')
  if ($pass) {
    Write-Host "QC_CASE=$Name RESULT=PASS STATUS=$Actual EXPECTED=$expectedText"
  } else {
    Write-Host "QC_CASE=$Name RESULT=FAIL STATUS=$Actual EXPECTED=$expectedText"
  }
  return $pass
}

$allPass = $true

# 1) Unauthenticated access should be blocked
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/products?page=0&size=1" -Method GET -TimeoutSec 30 }
if (-not (Assert-Case -Name 'UNAUTH_PRODUCTS_BLOCKED' -Actual $status -Expected @(401,403))) { $allPass = $false }

# 2) Register random user
$rand = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$username = "qc_user_$rand"
$password = "P@ssw0rd!$rand"
$registerBody = (@{ username = $username; password = $password } | ConvertTo-Json -Compress)
$reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $registerBody -TimeoutSec 30
$token = if ($reg.accessToken) { $reg.accessToken } elseif ($reg.token) { $reg.token } else { '' }

if ([string]::IsNullOrWhiteSpace($token)) {
  Write-Output 'QC_CASE=REGISTER_RETURNS_TOKEN RESULT=FAIL STATUS=0 EXPECTED=token'
  $allPass = $false
} else {
  Write-Output 'QC_CASE=REGISTER_RETURNS_TOKEN RESULT=PASS STATUS=200 EXPECTED=token'
}

$headers = @{ Authorization = "Bearer $token" }

# 3) Wrong password login should fail
$badLoginBody = (@{ username = $username; password = 'wrong-pass' } | ConvertTo-Json -Compress)
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method POST -ContentType 'application/json' -Body $badLoginBody -TimeoutSec 30 }
if (-not (Assert-Case -Name 'LOGIN_WRONG_PASSWORD_REJECTED' -Actual $status -Expected @(400,401))) { $allPass = $false }

# 4) Authenticated products access should work
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/products?page=0&size=1" -Method GET -Headers $headers -TimeoutSec 30 }
if (-not (Assert-Case -Name 'AUTH_PRODUCTS_OK' -Actual $status -Expected @(200))) { $allPass = $false }

# 5) USER should not access admin users list
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/users?page=0&size=1" -Method GET -Headers $headers -TimeoutSec 30 }
if (-not (Assert-Case -Name 'USER_CANNOT_LIST_USERS' -Actual $status -Expected @(403))) { $allPass = $false }

# 6) Input validation: invalid order payload should be rejected
$invalidOrder = (@{ customerId = $null; items = @(); warehouseId = 1 } | ConvertTo-Json -Compress)
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/orders" -Method POST -Headers $headers -ContentType 'application/json' -Body $invalidOrder -TimeoutSec 30 }
if (-not (Assert-Case -Name 'INVALID_ORDER_REJECTED' -Actual $status -Expected @(400,422))) { $allPass = $false }

# 7) Robustness: suspicious filter string should not crash server
$status = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/products?page=0&size=5&name=' OR '1'='1" -Method GET -Headers $headers -TimeoutSec 30 }
if (-not (Assert-Case -Name 'SUSPICIOUS_FILTER_NO_CRASH' -Actual $status -Expected @(200,400))) { $allPass = $false }

if ($allPass) {
  Write-Output 'QC_STRICT_SUMMARY=PASS'
  exit 0
}

Write-Output 'QC_STRICT_SUMMARY=FAIL'
exit 1
