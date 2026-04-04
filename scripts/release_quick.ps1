param(
  [Parameter(Mandatory = $true)]
  [string]$Version,

  [string]$ReleaseDate = (Get-Date -Format 'yyyy-MM-dd'),

  [string]$OutputPath,

  [switch]$NoChangelogUpdate,

  [switch]$SkipDeployDryRun,

  [int]$HealthTimeoutSec = 90
)

$ErrorActionPreference = 'Stop'

$startedAt = Get-Date
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

function Get-PythonCandidates {
  $candidates = @()

  $venvPython = Join-Path $repoRoot '.venv\Scripts\python.exe'
  if (Test-Path $venvPython) {
    $candidates += [PSCustomObject]@{ Exe = $venvPython; PrefixArgs = @() }
  }

  $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
  if ($null -ne $pythonCmd) {
    $candidates += [PSCustomObject]@{ Exe = $pythonCmd.Source; PrefixArgs = @() }
  }

  $pyCmd = Get-Command py -ErrorAction SilentlyContinue
  if ($null -ne $pyCmd) {
    $candidates += [PSCustomObject]@{ Exe = $pyCmd.Source; PrefixArgs = @('-3') }
  }

  $unique = @()
  $seen = @{}
  foreach ($item in $candidates) {
    $key = "$($item.Exe)|$($item.PrefixArgs -join ',')"
    if (-not $seen.ContainsKey($key)) {
      $seen[$key] = $true
      $unique += $item
    }
  }
  return $unique
}

function Resolve-PythonExe {
  $candidates = Get-PythonCandidates
  if ($candidates.Count -eq 0) {
    throw 'Python executable not found. Install Python or create .venv first.'
  }
  return $candidates[0]
}

function Test-PythonModule {
  param(
    [pscustomobject]$Python,
    [string]$ModuleName
  )

  $prevEap = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    & $Python.Exe @($Python.PrefixArgs + @('-c', "import $ModuleName")) *> $null
    return $LASTEXITCODE -eq 0
  } finally {
    $ErrorActionPreference = $prevEap
  }
}

function Resolve-PythonExeWithModule {
  param([string]$ModuleName)

  $candidates = Get-PythonCandidates
  foreach ($candidate in $candidates) {
    if (Test-PythonModule -Python $candidate -ModuleName $ModuleName) {
      return $candidate
    }
  }
  throw "No Python interpreter found with required module: $ModuleName"
}

function Invoke-PythonScript {
  param(
    [pscustomobject]$Python,
    [string]$ScriptPath,
    [string[]]$Arguments
  )

  & $Python.Exe @($Python.PrefixArgs + @($ScriptPath) + $Arguments)

  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code ${LASTEXITCODE}: $ScriptPath $($Arguments -join ' ')"
  }
}

Write-Output "RELEASE_QUICK_STARTED_AT=$($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
Write-Output "RELEASE_QUICK_VERSION=$Version"
Write-Output "RELEASE_QUICK_DATE=$ReleaseDate"

$pythonExe = Resolve-PythonExe
Write-Output "RELEASE_QUICK_PYTHON=$($pythonExe.Exe) $($pythonExe.PrefixArgs -join ' ')"

$releaseScript = Join-Path $scriptDir 'new_release_note.py'
$releaseArgs = @('--version', $Version, '--date', $ReleaseDate)

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
  $releaseArgs += @('--output', $OutputPath)
}

if (-not $NoChangelogUpdate) {
  $releaseArgs += '--update-changelog'
}

Write-Output '--- STEP 1: Generate release docs ---'
Invoke-PythonScript -Python $pythonExe -ScriptPath $releaseScript -Arguments $releaseArgs

if (-not $SkipDeployDryRun) {
  $deployPythonExe = Resolve-PythonExeWithModule -ModuleName 'paramiko'
  Write-Output "RELEASE_QUICK_DEPLOY_PYTHON=$($deployPythonExe.Exe) $($deployPythonExe.PrefixArgs -join ' ')"
  $deployScript = Join-Path $scriptDir 'deploy_prod.py'
  $deployArgs = @('--dry-run', '--print-json', '--health-timeout-sec', "$HealthTimeoutSec")
  Write-Output '--- STEP 2: Deploy dry-run ---'
  Invoke-PythonScript -Python $deployPythonExe -ScriptPath $deployScript -Arguments $deployArgs
} else {
  Write-Output '--- STEP 2: Deploy dry-run skipped ---'
}

Write-Output '--- STEP 3: Pre-release checklist snapshot ---'
Write-Output '1) Confirm CI green (CI + Deploy CLI regression)'
Write-Output '2) Confirm DB migration/rollback reviewed'
Write-Output '3) Confirm backup plan ready before production deploy'
Write-Output '4) Confirm SMOKE_PASSWORD secret is valid'

$endedAt = Get-Date
$durationSec = [int]($endedAt - $startedAt).TotalSeconds
Write-Output "RELEASE_QUICK_FINISHED_AT=$($endedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
Write-Output "RELEASE_QUICK_DURATION_SEC=$durationSec"
Write-Output 'RELEASE_QUICK_RESULT=PASS'
