$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

function To-Decimal($value) {
  return [decimal]::Parse(($value).ToString(), [System.Globalization.CultureInfo]::InvariantCulture)
}

$u = 'ops_vat_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
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

  $eligible = @($stocks | Where-Object { $_.available -ge 2 } | Select-Object -ExpandProperty productId -Unique)
  if ($eligible.Count -ge 2) {
    $warehouseId = [long]$w.id
    $selectedProductIds = @([long]$eligible[0], [long]$eligible[1])
    break
  }
}

if ($null -eq $warehouseId -or $selectedProductIds.Count -lt 2) {
  throw 'NO_WAREHOUSE_WITH_TWO_STOCKED_PRODUCTS_FOR_VAT_ASSERT'
}

$p1 = Invoke-RestMethod -Uri "$base/api/v1/products/$($selectedProductIds[0])" -Headers $headers -Method GET -TimeoutSec 30
$p2 = Invoke-RestMethod -Uri "$base/api/v1/products/$($selectedProductIds[1])" -Headers $headers -Method GET -TimeoutSec 30

$q1 = 1
$q2 = 2

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerBody = @{
  code = "REL-VAT-CUS-$ts"
  name = "Release VAT Customer $ts"
  phone = '0900000044'
  email = "release.vat.$ts@example.com"
  active = $true
} | ConvertTo-Json -Compress -Depth 5
$customer = Invoke-RestMethod -Uri "$base/api/v1/customers" -Headers $headers -Method POST -ContentType 'application/json' -Body $customerBody -TimeoutSec 30

$orderBody = @{
  customerId = [long]$customer.id
  warehouseId = $warehouseId
  items = @(
    @{ productId = $selectedProductIds[0]; quantity = $q1 },
    @{ productId = $selectedProductIds[1]; quantity = $q2 }
  )
} | ConvertTo-Json -Compress -Depth 8

Write-Output "DEBUG_VAT_ORDER_BODY=$orderBody"
$order = Invoke-RestMethod -Uri "$base/api/v1/orders" -Headers $headers -Method POST -ContentType 'application/json' -Body $orderBody -TimeoutSec 30

if ($null -eq $order -or $null -eq $order.id) {
  throw 'VAT_ASSERT_ORDER_CREATE_FAILED'
}

if ($null -eq $order.items -or @($order.items).Count -ne 2) {
  throw 'VAT_ASSERT_ORDER_NOT_TWO_LINES'
}

$price1 = To-Decimal $p1.price
$price2 = To-Decimal $p2.price
$vat1 = if ($null -eq $p1.vatRate) { [decimal]0 } else { To-Decimal $p1.vatRate }
$vat2 = if ($null -eq $p2.vatRate) { [decimal]0 } else { To-Decimal $p2.vatRate }

$line1 = [decimal]::Round($price1 * $q1 * (1 + ($vat1 / 100)), 2, [System.MidpointRounding]::AwayFromZero)
$line2 = [decimal]::Round($price2 * $q2 * (1 + ($vat2 / 100)), 2, [System.MidpointRounding]::AwayFromZero)
$expected = [decimal]::Round($line1 + $line2, 2, [System.MidpointRounding]::AwayFromZero)
$actual = To-Decimal $order.totalAmount
$delta = [System.Math]::Abs($actual - $expected)

if ($delta -gt [decimal]0.01) {
  throw "VAT_ASSERT_TOTAL_MISMATCH expected=$expected actual=$actual delta=$delta"
}

$confirm = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/confirm" -Headers $headers -Method POST -TimeoutSec 30
if ($confirm.status -ne 'CONFIRMED') {
  throw "VAT_ASSERT_ORDER_NOT_CONFIRMED: $($confirm.status)"
}

Write-Output "VAT_ASSERT_WAREHOUSE_ID=$warehouseId"
Write-Output "VAT_ASSERT_PRODUCT_IDS=$($selectedProductIds -join ',')"
Write-Output "VAT_ASSERT_PRODUCT1_PRICE=$price1 VAT=$vat1 QTY=$q1"
Write-Output "VAT_ASSERT_PRODUCT2_PRICE=$price2 VAT=$vat2 QTY=$q2"
Write-Output "VAT_ASSERT_EXPECTED_TOTAL=$expected"
Write-Output "VAT_ASSERT_ACTUAL_TOTAL=$actual"
Write-Output "VAT_ASSERT_DELTA=$delta"
Write-Output "VAT_ASSERT_ORDER_ID=$($order.id)"
Write-Output "VAT_ASSERT_ORDER_STATUS=$($confirm.status)"
Write-Output 'VAT_ASSERT_SMOKE_RESULT=PASS'
