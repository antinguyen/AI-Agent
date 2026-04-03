$ErrorActionPreference = 'Continue'

$steps = @(
  @{ Name = 'Security: Privilege Escalation'; Script = '.\\scripts\\qc_privilege_escalation_check.ps1' },
  @{ Name = 'Auth/Validation Strict Matrix'; Script = '.\\scripts\\qc_strict_prod.ps1' },
  @{ Name = 'Orders+VAT Resilient'; Script = '.\\scripts\\qc_v21_resilient.ps1' },
  @{ Name = 'Shipment+Payment+RBAC'; Script = '.\\scripts\\qc_step3_staff.ps1' }
)

$results = @()

foreach ($step in $steps) {
  Write-Output "`n=== RUN: $($step.Name) ==="
  pwsh -NoProfile -ExecutionPolicy Bypass -File $step.Script
  $code = $LASTEXITCODE
  if ($null -eq $code) { $code = 0 }
  $status = if ($code -eq 0) { 'PASS' } else { 'FAIL' }
  $results += [PSCustomObject]@{
    Step = $step.Name
    ExitCode = $code
    Status = $status
  }
  Write-Output "=== END: $($step.Name) => $status (exit=$code) ==="
}

Write-Output "`n=== QC SUMMARY ==="
$results | Format-Table -AutoSize | Out-String | Write-Output

$failed = @($results | Where-Object { $_.ExitCode -ne 0 })
if ($failed.Count -gt 0) {
  Write-Output 'QC_PRODUCTION_FULL=FAIL'
  exit 1
}

Write-Output 'QC_PRODUCTION_FULL=PASS'
exit 0
