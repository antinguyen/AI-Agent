$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$multiScript = Join-Path $root 'release_smoke_multiline_order.ps1'
$vatScript = Join-Path $root 'release_smoke_multiline_vat_assert.ps1'

if (-not (Test-Path $multiScript)) {
  throw "MISSING_SCRIPT: $multiScript"
}
if (-not (Test-Path $vatScript)) {
  throw "MISSING_SCRIPT: $vatScript"
}

Write-Output 'V21_SUITE_STEP=RUN_MULTILINE_ORDER'
& powershell -ExecutionPolicy Bypass -File $multiScript
if ($LASTEXITCODE -ne 0) {
  throw 'V21_SUITE_FAIL_MULTILINE_ORDER'
}

Write-Output 'V21_SUITE_STEP=RUN_VAT_ASSERT'
& powershell -ExecutionPolicy Bypass -File $vatScript
if ($LASTEXITCODE -ne 0) {
  throw 'V21_SUITE_FAIL_VAT_ASSERT'
}

Write-Output 'V21_SUITE_RESULT=PASS'
