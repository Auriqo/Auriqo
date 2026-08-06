param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$asset = Join-Path $ProjectRoot 'app/src/main/assets/oss-licenses.tsv'
$appBuild = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/build.gradle.kts') -Raw
$rows = @()
Get-Content -LiteralPath $asset | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $rows += ,($_ -split "`t")
}

if (($rows | Where-Object { $_.Count -ne 5 }).Count -gt 0) { throw 'Every OSS attribution row must have five tab-separated fields.' }
foreach ($row in $rows) {
    if ([string]::IsNullOrWhiteSpace($row[1]) -or [string]::IsNullOrWhiteSpace($row[2]) -or $row[3] -notmatch '^https://') {
        throw "Invalid OSS attribution row: $($row -join '|')"
    }
}
$ids = @{}
foreach ($row in $rows) { $ids[$row[0]] = $true }

$required = @('app')
$dependencyPattern = '(?m)^\s*(?:implementation|debugImplementation|coreLibraryDesugaring|"gmsImplementation")\((?:platform\()?([^\)\r\n]+)'
foreach ($match in [regex]::Matches($appBuild, $dependencyPattern)) {
    $value = $match.Groups[1].Value.Trim()
    if ($value -match '^libs\.') {
        $required += $value
    } elseif ($value -match 'firebase-bom') {
        $required += 'firebase-bom'
    } elseif ($value -match 'firebase-analytics') {
        $required += 'firebase-analytics'
    } elseif ($value -match 'firebase-crashlytics') {
        $required += 'firebase-crashlytics'
    }
}
foreach ($match in [regex]::Matches($appBuild, 'implementation\(project\(":([^"]+)"\)\)')) {
    $required += $match.Groups[1].Value
}
foreach ($id in ($required | Sort-Object -Unique)) {
    if (-not $ids.ContainsKey($id)) { throw "Missing OSS attribution for direct runtime dependency $id." }
}
if ($rows.Count -lt 70) { throw 'OSS attribution data is unexpectedly incomplete.' }
if ($appBuild -notmatch 'implementation\(libs\.compose\.material\.icons\.extended\)') { throw 'Material icons must use the version catalog alias.' }
Write-Output "OSS license validation passed: $($rows.Count) offline notices covering $((($required | Sort-Object -Unique).Count)) direct runtime dependencies/modules."
