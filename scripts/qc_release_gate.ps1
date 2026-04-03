$ErrorActionPreference = 'Continue'

$startedAt = Get-Date
Write-Output "RELEASE_GATE_STARTED_AT=$($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))"

# Run full production QC and capture output for parsing
$qcOutput = & .\scripts\qc_production_full.ps1 2>&1
$qcExit = $LASTEXITCODE

$qcOutput | ForEach-Object { Write-Output $_ }

$securityPass = ($qcOutput -join "`n") -match 'PRIV_ESC_TEST=PASS'
$strictPass = ($qcOutput -join "`n") -match 'QC_STRICT_SUMMARY=PASS'
$v21Pass = ($qcOutput -join "`n") -match 'QC_V21_RESILIENT_RESULT=PASS'
$step3Pass = ($qcOutput -join "`n") -match 'QC_STEP3_SUMMARY=PASS'
$fullPass = ($qcOutput -join "`n") -match 'QC_PRODUCTION_FULL=PASS'

Write-Output "RELEASE_GATE_SECURITY_PASS=$securityPass"
Write-Output "RELEASE_GATE_STRICT_PASS=$strictPass"
Write-Output "RELEASE_GATE_ORDER_VAT_PASS=$v21Pass"
Write-Output "RELEASE_GATE_LOGISTICS_PASS=$step3Pass"
Write-Output "RELEASE_GATE_FULL_QC_PASS=$fullPass"

$decision = if ($qcExit -eq 0 -and $securityPass -and $strictPass -and $v21Pass -and $step3Pass -and $fullPass) { 'GO' } else { 'NO_GO' }

$finishedAt = Get-Date
$durationSec = [int]($finishedAt - $startedAt).TotalSeconds

Write-Output "RELEASE_GATE_DECISION=$decision"
Write-Output "RELEASE_GATE_QC_EXIT=$qcExit"
Write-Output "RELEASE_GATE_DURATION_SEC=$durationSec"
Write-Output "RELEASE_GATE_FINISHED_AT=$($finishedAt.ToString('yyyy-MM-dd HH:mm:ss'))"

$releaseNote = @(
  "RELEASE_NOTE_TIME=$($finishedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
  "RELEASE_NOTE_DECISION=$decision"
  "RELEASE_NOTE_SECURITY_PASS=$securityPass"
  "RELEASE_NOTE_STRICT_PASS=$strictPass"
  "RELEASE_NOTE_ORDER_VAT_PASS=$v21Pass"
  "RELEASE_NOTE_LOGISTICS_PASS=$step3Pass"
  "RELEASE_NOTE_FULL_QC_PASS=$fullPass"
  "RELEASE_NOTE_DURATION_SEC=$durationSec"
)

Write-Output "`n=== RELEASE NOTE (SHORT) ==="
$releaseNote | ForEach-Object { Write-Output $_ }

$outDir = Join-Path $PSScriptRoot 'out'
if (-not (Test-Path $outDir)) {
  New-Item -ItemType Directory -Path $outDir | Out-Null
}

$latestFile = Join-Path $outDir 'release_gate_latest.txt'
$releaseNote | Set-Content -Path $latestFile -Encoding UTF8
Write-Output "RELEASE_NOTE_FILE=$latestFile"

$historyLine = "{0}|DECISION={1}|SECURITY={2}|STRICT={3}|ORDER_VAT={4}|LOGISTICS={5}|FULL={6}|DURATION_SEC={7}" -f
  $finishedAt.ToString('yyyy-MM-dd HH:mm:ss'),
  $decision,
  $securityPass,
  $strictPass,
  $v21Pass,
  $step3Pass,
  $fullPass,
  $durationSec

$historyFile = Join-Path $outDir 'release_gate_history.log'
Add-Content -Path $historyFile -Value $historyLine -Encoding UTF8
Write-Output "RELEASE_NOTE_HISTORY_FILE=$historyFile"

$timestampFile = Join-Path $outDir ("release_gate_{0}.txt" -f $finishedAt.ToString('yyyyMMdd_HHmmss'))
$releaseNote | Set-Content -Path $timestampFile -Encoding UTF8
Write-Output "RELEASE_NOTE_TIMESTAMP_FILE=$timestampFile"

if ($decision -eq 'GO') {
  exit 0
}
exit 1
