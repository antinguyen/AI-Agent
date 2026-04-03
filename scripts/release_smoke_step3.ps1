$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

$u = 'ops_flow_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$p = 'OpsFlow#2026'
$registerBody = @{ username = $u; password = $p; role = 'ADMIN' } | ConvertTo-Json -Compress -Depth 5
$reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $registerBody -TimeoutSec 30
$headers = @{ Authorization = "Bearer $($reg.token)" }

$warehouses = @(Invoke-RestMethod -Uri "$base/api/v1/warehouses" -Headers $headers -Method GET -TimeoutSec 30)
$warehouseId = $null
$productId = $null
foreach ($w in $warehouses) {
  $stocksRaw = Invoke-RestMethod -Uri "$base/api/v1/warehouses/$($w.id)/stock" -Headers $headers -Method GET -TimeoutSec 30
  $stocks = @($stocksRaw)
  if ($stocks.Count -eq 1 -and $stocks[0] -is [System.Array]) {
    $stocks = @($stocks[0])
  }
  $eligible = @($stocks | Where-Object { $_.available -ge 1 })
  $candidate = if ($eligible.Count -gt 0) { $eligible[0] } else { $null }
  if ($null -ne $candidate) {
    $warehouseId = [long]$w.id
    $productId = [long]$candidate.productId
    break
  }
}

if ($null -eq $warehouseId -or $null -eq $productId) {
  throw 'NO_STOCKED_PRODUCT_FOUND'
}

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerBody = @{ code = "REL-CUS-$ts"; name = "Release Customer $ts"; phone = '0900000022'; email = "release$ts@example.com"; active = $true } | ConvertTo-Json -Compress -Depth 5
$customer = Invoke-RestMethod -Uri "$base/api/v1/customers" -Headers $headers -Method POST -ContentType 'application/json' -Body $customerBody -TimeoutSec 30

$orderBody = @{ customerId = $customer.id; warehouseId = $warehouseId; items = @(@{ productId = $productId; quantity = 1 }) } | ConvertTo-Json -Compress -Depth 8
Write-Output "DEBUG_ORDER_BODY=$orderBody"
$order = Invoke-RestMethod -Uri "$base/api/v1/orders" -Headers $headers -Method POST -ContentType 'application/json' -Body $orderBody -TimeoutSec 30

$confirm = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/confirm" -Headers $headers -Method POST -TimeoutSec 30

$shipmentBody = @{ orderId = $order.id; note = 'Release smoke shipment' } | ConvertTo-Json -Compress -Depth 5
$shipment = Invoke-RestMethod -Uri "$base/api/v1/shipments" -Headers $headers -Method POST -ContentType 'application/json' -Body $shipmentBody -TimeoutSec 30
$shipped = Invoke-RestMethod -Uri "$base/api/v1/shipments/$($shipment.id)/ship" -Headers $headers -Method POST -TimeoutSec 30

$paymentBody = @{ method = 'CASH'; note = 'Release smoke payment' } | ConvertTo-Json -Compress -Depth 5
$payment = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/payments" -Headers $headers -Method POST -ContentType 'application/json' -Body $paymentBody -TimeoutSec 30

$from = (Get-Date).AddDays(-7).ToString('yyyy-MM-dd')
$to = (Get-Date).ToString('yyyy-MM-dd')
$revenue = Invoke-RestMethod -Uri "$base/api/v1/reports/revenue?from=$from&to=$to" -Headers $headers -Method GET -TimeoutSec 30
$summary = Invoke-RestMethod -Uri "$base/api/v1/reports/order-summary" -Headers $headers -Method GET -TimeoutSec 30

Write-Output "STEP3_WAREHOUSE_ID=$warehouseId"
Write-Output "STEP3_PRODUCT_ID=$productId"
Write-Output "STEP3_ORDER_ID=$($order.id)"
Write-Output "STEP3_ORDER_STATUS=$($confirm.status)"
Write-Output "STEP3_SHIPMENT_ID=$($shipment.id)"
Write-Output "STEP3_SHIPMENT_STATUS=$($shipped.status)"
Write-Output "STEP3_PAYMENT_METHOD=$($payment.method)"
Write-Output "STEP3_REPORT_CURRENCY=$($revenue.currencyCode)"
Write-Output "STEP3_ORDER_SUMMARY_TOTAL=$($summary.totalOrders)"
Write-Output 'STEP3_FLOW_RESULT=PASS'
