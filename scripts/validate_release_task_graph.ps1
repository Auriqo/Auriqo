param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$JdkHome = 'C:\tmp\auriqo-toolchain\jdk-stage\jdk-21.0.12+8',
    [string]$AndroidSdkHome = 'C:\tmp\auriqo-toolchain\android-sdk'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin\java.exe'))) {
    throw "JDK 21 was not found at $JdkHome."
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$(Join-Path $JdkHome 'bin');$env:Path"
$env:ANDROID_HOME = $AndroidSdkHome
$env:ANDROID_SDK_ROOT = $AndroidSdkHome

$taskList = & (Join-Path $ProjectRoot 'gradlew.bat') --no-daemon :app:tasks --all 2>&1
if ($LASTEXITCODE -ne 0) { throw 'Gradle could not configure the app task model.' }
$taskListText = $taskList -join "`n"
foreach ($task in @(
    'assembleUniversalFossDebug', 'assembleUniversalGmsDebug',
    'auriqoApi30UniversalFossDebugAndroidTest',
    'assembleUniversalFossRelease', 'assembleArm64FossRelease', 'assembleArmeabiFossRelease',
    'assembleX86FossRelease', 'assembleX86_64FossRelease'
)) {
    if ($taskListText -notmatch "(?m)^$task\b") { throw "Missing expected Gradle task: $task." }
}

$dryRun = & (Join-Path $ProjectRoot 'gradlew.bat') --no-daemon -PrequireReleaseSigning=true :app:assembleUniversalFossRelease --dry-run 2>&1
if ($LASTEXITCODE -ne 0) { throw 'Signed release dry-run task graph failed.' }
$dryRunText = $dryRun -join "`n"
if ($dryRunText -notmatch '(?m)^:app:checkReleaseSigning\b') {
    throw 'Signed release task graph does not include checkReleaseSigning.'
}
if (([regex]::Matches($dryRunText, '(?m)^:app:checkReleaseSigning\b')).Count -ne 1) {
    throw 'Signed release task graph contains a duplicate or cyclic checkReleaseSigning task.'
}

Write-Output 'Gradle task graph validation passed: FOSS/GMS universal tasks, the FOSS managed-device instrumentation task, all FOSS release ABI tasks, and exactly one signing guard in the signed release dry-run.'
