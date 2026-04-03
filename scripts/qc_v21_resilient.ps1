$ErrorActionPreference = 'Stop'
$base = 'http://192.168.1.200:8080'

function To-Decimal($value) {
  return [decimal]::Parse(($value).ToString(), [System.Globalization.CultureInfo]::InvariantCulture)
}

$u = 'qc_v21_' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$p = 'QcFlow#2026'
$registerBody = @{ username = $u; password = $p } | ConvertTo-Json -Compress -Depth 5
$reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method POST -ContentType 'application/json' -Body $registerBody -TimeoutSec 30
$token = if ($reg.token) { $reg.token } else { $reg.accessToken }
$headers = @{ Authorization = "Bearer $token" }

$warehouses = @(Invoke-RestMethod -Uri "$base/api/v1/warehouses" -Headers $headers -Method GET -TimeoutSec 30)
$warehouseId = $null
$productA = $null
$productB = $null
$qtyA = 1
$qtyB = 2
$fallbackSingle = $false

foreach ($w in $warehouses) {
  $stocksRaw = Invoke-RestMethod -Uri "$base/api/v1/warehouses/$($w.id)/stock" -Headers $headers -Method GET -TimeoutSec 30
  $stocks = @($stocksRaw)
  if ($stocks.Count -eq 1 -and $stocks[0] -is [System.Array]) { $stocks = @($stocks[0]) }

  $eligible = @($stocks | Where-Object { $_.available -ge 1 })

  if ($eligible.Count -ge 2) {
    $first = $eligible[0]
    $second = $eligible | Where-Object { $_.productId -ne $first.productId } | Select-Object -First 1
    if ($null -ne $second) {
      $warehouseId = [long]$w.id
      $productA = [long]$first.productId
      $productB = [long]$second.productId
      # Prefer stronger case qty=2 for second item, but gracefully fallback to qty=1.
      $qtyB = if ([int]$second.available -ge 2) { 2 } else { 1 }
      break
    }
  }
}

# Fallback: if no warehouse has 2 SKUs in stock, run VAT check on 1 SKU to avoid false NO_GO due to depleted test data.
if ($null -eq $warehouseId -or $null -eq $productA -or $null -eq $productB) {
  foreach ($w in $warehouses) {
    $stocksRaw = Invoke-RestMethod -Uri "$base/api/v1/warehouses/$($w.id)/stock" -Headers $headers -Method GET -TimeoutSec 30
    $stocks = @($stocksRaw)
    if ($stocks.Count -eq 1 -and $stocks[0] -is [System.Array]) { $stocks = @($stocks[0]) }
    $single = @($stocks | Where-Object { $_.available -ge 1 } | Select-Object -First 1)
    if ($single.Count -gt 0) {
      $warehouseId = [long]$w.id
      $productA = [long]$single[0].productId
      $productB = $null
      $qtyA = 1
      $qtyB = 0
      $fallbackSingle = $true
      break
    }
  }
}

if ($null -eq $warehouseId -or $null -eq $productA) {
  throw 'QC_V21_NO_WAREHOUSE_WITH_AVAILABLE_STOCK'
}

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerBody = @{ code = "QC-V21-CUS-$ts"; name = "QC V21 Customer $ts"; phone = '0900000099'; email = "qc.v21.$ts@example.com"; active = $true } | ConvertTo-Json -Compress -Depth 5
$customer = Invoke-RestMethod -Uri "$base/api/v1/customers" -Headers $headers -Method POST -ContentType 'application/json' -Body $customerBody -TimeoutSec 30

$p1 = Invoke-RestMethod -Uri "$base/api/v1/products/$productA" -Headers $headers -Method GET -TimeoutSec 30
$p2 = if ($null -ne $productB) { Invoke-RestMethod -Uri "$base/api/v1/products/$productB" -Headers $headers -Method GET -TimeoutSec 30 } else { $null }

$vat1 = if ($null -ne $p1.vatRate) { To-Decimal $p1.vatRate } else { [decimal]0 }
$vat2 = if ($null -ne $p2 -and $null -ne $p2.vatRate) { To-Decimal $p2.vatRate } else { [decimal]0 }
$price1 = To-Decimal $p1.price
$price2 = if ($null -ne $p2) { To-Decimal $p2.price } else { [decimal]0 }

$line1 = [decimal]::Round($price1 * (1 + $vat1 / 100) * $qtyA, 2, [System.MidpointRounding]::AwayFromZero)
$line2 = [decimal]::Round($price2 * (1 + $vat2 / 100) * $qtyB, 2, [System.MidpointRounding]::AwayFromZero)
$expectedTotal = [decimal]::Round($line1 + $line2, 2, [System.MidpointRounding]::AwayFromZero)

$items = @(@{ productId = $productA; quantity = $qtyA })
if ($null -ne $productB -and $qtyB -gt 0) {
  $items += @{ productId = $productB; quantity = $qtyB }
}

$orderBody = @{ customerId = [long]$customer.id; warehouseId = $warehouseId; items = $items } | ConvertTo-Json -Compress -Depth 8
$order = Invoke-RestMethod -Uri "$base/api/v1/orders" -Headers $headers -Method POST -ContentType 'application/json' -Body $orderBody -TimeoutSec 30
$confirm = Invoke-RestMethod -Uri "$base/api/v1/orders/$($order.id)/confirm" -Headers $headers -Method POST -TimeoutSec 30

$actualTotal = To-Decimal $order.totalAmount
$delta = [decimal]::Abs($actualTotal - $expectedTotal)

Write-Output "QC_V21_WAREHOUSE_ID=$warehouseId"
Write-Output "QC_V21_PRODUCTS=$productA,$productB"
Write-Output "QC_V21_QTIES=$qtyA,$qtyB"
Write-Output "QC_V21_FALLBACK_SINGLE=$fallbackSingle"
Write-Output "QC_V21_ORDER_ID=$($order.id)"
Write-Output "QC_V21_STATUS=$($confirm.status)"
Write-Output "QC_V21_EXPECTED_TOTAL=$expectedTotal"
Write-Output "QC_V21_ACTUAL_TOTAL=$actualTotal"
Write-Output "QC_V21_DELTA=$delta"

if ($confirm.status -ne 'CONFIRMED') { throw 'QC_V21_ORDER_NOT_CONFIRMED' }
if ($delta -gt 0.01) { throw 'QC_V21_VAT_DELTA_TOO_HIGH' }

Write-Output 'QC_V21_RESILIENT_RESULT=PASS'
