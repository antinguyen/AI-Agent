$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'
$u = 'qc_admin_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$p = 'P@ssw0rd!' + (Get-Random -Minimum 1000 -Maximum 9999)
$body = (@{ username = $u; password = $p; role = 'ADMIN' } | ConvertTo-Json -Compress)

try {
  $reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $body -TimeoutSec 30
  $token = if ($reg.accessToken) { $reg.accessToken } elseif ($reg.token) { $reg.token } else { '' }

  if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Output 'PRIV_ESC_TEST=PASS REGISTER_ADMIN_NO_TOKEN'
    exit 0
  }

  $headers = @{ Authorization = "Bearer $token" }
  try {
    Invoke-RestMethod -Uri "$base/api/v1/users?page=0&size=1" -Method GET -Headers $headers -TimeoutSec 30 | Out-Null
    Write-Output 'PRIV_ESC_TEST=CRITICAL_FAIL REGISTER_ADMIN_ALLOWED_AND_USERS_ACCESS=200'
    exit 1
  } catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $sc = [int]$_.Exception.Response.StatusCode
      Write-Output "PRIV_ESC_TEST=PASS USERS_ACCESS_BLOCKED_STATUS=$sc"
      exit 0
    }
    throw
  }
} catch {
  if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
    $sc = [int]$_.Exception.Response.StatusCode
    Write-Output "PRIV_ESC_TEST=PASS REGISTER_ADMIN_BLOCKED_STATUS=$sc"
    exit 0
  }
  throw
}
