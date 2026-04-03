$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

$u = 'ops_multi_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$p = 'OpsFlow#2026'
$registerBody = @{ username = $u; password = $p; role = 'ADMIN' } | ConvertTo-Json -Compress -Depth 5
$reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $registerBody -TimeoutSec 30
$headers = @{ Authorization = "Bearer $($reg.token)" }

$warehouses = @(Invoke-RestMethod -Uri "$base/api/v1/warehouses" -Headers $headers -Method GET -TimeoutSec 30)
$warehouseId = $null
$selectedProductIds = @()

foreach ($w in $warehouses) {
  $stocksRaw = Invoke-RestMethod -Uri "$base/api/v1/warehouses/$($w.id)/stock" -Headers $headers -Method GET -TimeoutSec 30
  $stocks = @($stocksRaw)
  if ($stocks.Count -eq 1 -and $stocks[0] -is [System.Array]) {
    $stocks = @($stocks[0])
  }

  $eligible = @($stocks | Where-Object { $_.available -ge 1 } | Select-Object -ExpandProperty productId -Unique)
  if ($eligible.Count -ge 2) {
    $warehouseId = [long]$w.id
    $selectedProductIds = @([long]$eligible[0], [long]$eligible[1])
    break
  }
}

if ($null -eq $warehouseId -or $selectedProductIds.Count -lt 2) {
  throw 'NO_WAREHOUSE_WITH_TWO_STOCKED_PRODUCTS'
}

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerBody = @{
  code = "REL-MULTI-CUS-$ts"
  name = "Release Multi Customer $ts"
  phone = '0900000033'
  email = "release.multi.$ts@example.com"
  active = $true
} | ConvertTo-Json -Compress -Depth 5
$customer = Invoke-RestMethod -Uri "$base/api/v1/customers" -Headers $headers -Method POST -ContentType 'application/json' -Body $customerBody -TimeoutSec 30

$orderBody = @{
  customerId = [long]$customer.id
  warehouseId = $warehouseId
  items = @(
    @{ productId = $selectedProductIds[0]; quantity = 1 },
    @{ productId = $selectedProductIds[1]; quantity = 2 }
  )
} | ConvertTo-Json -Compress -Depth 8

Write-Output "DEBUG_MULTI_ORDER_BODY=$orderBody"
$order = Invoke-RestMethod -Uri "$base/api/v1/orders" -Headers $headers -Method POST -ContentType 'application/json' -Body $orderBody -TimeoutSec 30

if ($null -eq $order -or $null -eq $order.id) {
  throw 'ORDER_CREATE_FAILED'
}

if ($null -eq $order.items -or @($order.items).Count -lt 2) {
  throw 'ORDER_ITEMS_NOT_MULTI_LINE'
}

$itemCount = @($order.items).Count
$sumQty = (@($order.items) | Measure-Object -Property quantity -Sum).Sum

if ($sumQty -lt 3) {
  throw 'ORDER_QUANTITY_SUM_UNEXPECTED'
}

if ([decimal]$order.totalAmount -le 0) {
  throw 'ORDER_TOTAL_AMOUNT_INVALID'
}

$confirm = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/confirm" -Headers $headers -Method POST -TimeoutSec 30
if ($confirm.status -ne 'CONFIRMED') {
  throw "ORDER_NOT_CONFIRMED: $($confirm.status)"
}

Write-Output "MULTI_ORDER_WAREHOUSE_ID=$warehouseId"
Write-Output "MULTI_ORDER_PRODUCT_IDS=$($selectedProductIds -join ',')"
Write-Output "MULTI_ORDER_ID=$($order.id)"
Write-Output "MULTI_ORDER_ITEM_COUNT=$itemCount"
Write-Output "MULTI_ORDER_TOTAL_QTY=$sumQty"
Write-Output "MULTI_ORDER_TOTAL_AMOUNT=$($order.totalAmount)"
Write-Output "MULTI_ORDER_STATUS=$($confirm.status)"
Write-Output 'MULTI_ORDER_SMOKE_RESULT=PASS'
