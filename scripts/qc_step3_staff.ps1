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

$u = 'ops_flow_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$p = 'OpsFlow#2026'
$registerBody = @{ username = $u; password = $p } | ConvertTo-Json -Compress -Depth 5
$reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $registerBody -TimeoutSec 30
$token = if ($reg.token) { $reg.token } else { $reg.accessToken }
$headers = @{ Authorization = "Bearer $token" }

$warehouses = @(Invoke-RestMethod -Uri "$base/api/v1/warehouses" -Headers $headers -Method GET -TimeoutSec 30)
$warehouseId = $null
$productId = $null
foreach ($w in $warehouses) {
  $stocksRaw = Invoke-RestMethod -Uri "$base/api/v1/warehouses/$($w.id)/stock" -Headers $headers -Method GET -TimeoutSec 30
  $stocks = @($stocksRaw)
  if ($stocks.Count -eq 1 -and $stocks[0] -is [System.Array]) { $stocks = @($stocks[0]) }
  $eligible = @($stocks | Where-Object { $_.available -ge 1 })
  if ($eligible.Count -gt 0) {
    $warehouseId = [long]$w.id
    $productId = [long]$eligible[0].productId
    break
  }
}

if ($null -eq $warehouseId -or $null -eq $productId) { throw 'NO_STOCKED_PRODUCT_FOUND' }

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerBody = @{ code = "QC-CUS-$ts"; name = "QC Customer $ts"; phone = '0900000022'; email = "qc$ts@example.com"; active = $true } | ConvertTo-Json -Compress -Depth 5
$customer = Invoke-RestMethod -Uri "$base/api/v1/customers" -Headers $headers -Method POST -ContentType 'application/json' -Body $customerBody -TimeoutSec 30

$orderBody = @{ customerId = $customer.id; warehouseId = $warehouseId; items = @(@{ productId = $productId; quantity = 1 }) } | ConvertTo-Json -Compress -Depth 8
$order = Invoke-RestMethod -Uri "$base/api/v1/orders" -Headers $headers -Method POST -ContentType 'application/json' -Body $orderBody -TimeoutSec 30
$confirm = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/confirm" -Headers $headers -Method POST -TimeoutSec 30

$shipmentBody = @{ orderId = $order.id; note = 'QC shipment flow' } | ConvertTo-Json -Compress -Depth 5
$shipment = Invoke-RestMethod -Uri "$base/api/v1/shipments" -Headers $headers -Method POST -ContentType 'application/json' -Body $shipmentBody -TimeoutSec 30
$shipped = Invoke-RestMethod -Uri "$base/api/v1/shipments/$($shipment.id)/ship" -Headers $headers -Method POST -TimeoutSec 30

$paymentBody = @{ method = 'CASH'; note = 'QC payment flow' } | ConvertTo-Json -Compress -Depth 5
$payment = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/payments" -Headers $headers -Method POST -ContentType 'application/json' -Body $paymentBody -TimeoutSec 30

$from = (Get-Date).AddDays(-7).ToString('yyyy-MM-dd')
$to = (Get-Date).ToString('yyyy-MM-dd')
$reportStatus = Get-StatusCode { Invoke-RestMethod -Uri "$base/api/v1/reports/revenue?from=$from&to=$to" -Headers $headers -Method GET -TimeoutSec 30 }

Write-Output "QC_STEP3_ORDER_ID=$($order.id)"
Write-Output "QC_STEP3_SHIPMENT_STATUS=$($shipped.status)"
Write-Output "QC_STEP3_PAYMENT_METHOD=$($payment.method)"
Write-Output "QC_STEP3_STAFF_REPORT_STATUS=$reportStatus"
if ($reportStatus -eq 403) {
  Write-Output 'QC_STEP3_SUMMARY=PASS'
  exit 0
}
Write-Output 'QC_STEP3_SUMMARY=FAIL'
exit 1
