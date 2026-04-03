param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = 'Stop'

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$wrapperPropertiesPath = Join-Path $PSScriptRoot 'maven-wrapper.properties'

if (-not (Test-Path $wrapperPropertiesPath)) {
    throw "Missing wrapper properties file: $wrapperPropertiesPath"
}

$distributionUrl = (
    Get-Content $wrapperPropertiesPath |
    Where-Object { $_ -match '^distributionUrl=' } |
    Select-Object -First 1
) -replace '^distributionUrl=', ''

if (-not $distributionUrl) {
    throw 'distributionUrl is missing from maven-wrapper.properties'
}

$mavenUserHome = if ($env:MAVEN_USER_HOME) {
    $env:MAVEN_USER_HOME
} else {
    Join-Path $HOME '.m2'
}

$zipName = Split-Path -Path $distributionUrl -Leaf
$distributionName = [System.IO.Path]::GetFileNameWithoutExtension($zipName)
$extractDirName = if ($distributionName.EndsWith('-bin')) {
    $distributionName.Substring(0, $distributionName.Length - 4)
} else {
    $distributionName
}

$distributionRoot = Join-Path $mavenUserHome (Join-Path 'wrapper\dists' $distributionName)
$mavenHome = Join-Path $distributionRoot $extractDirName
$mvnCmd = Join-Path $mavenHome 'bin\mvn.cmd'

if (-not (Test-Path $mvnCmd)) {
    New-Item -ItemType Directory -Force -Path $distributionRoot | Out-Null
    $zipPath = Join-Path $distributionRoot $zipName
    Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath -UseBasicParsing
    Expand-Archive -Path $zipPath -DestinationPath $distributionRoot -Force
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
}

if (-not (Test-Path $mvnCmd)) {
    throw "Unable to resolve Maven executable at $mvnCmd"
}

Push-Location $projectRoot
try {
    & $mvnCmd @MavenArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}