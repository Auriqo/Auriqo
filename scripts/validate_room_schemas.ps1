param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$databaseSource = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/db/MusicDatabase.kt') -Raw
$schemasRoot = Join-Path $ProjectRoot 'app/schemas'
$currentSchemas = Join-Path $schemasRoot 'com.auriqo.music.db.InternalDatabase'
$legacySchemas = Join-Path $schemasRoot 'iad1tya.echo.music.db.InternalDatabase'
$olderLegacySchemas = Join-Path $schemasRoot 'com.music.echo.db.InternalDatabase'

if ($databaseSource -notmatch 'version\s*=\s*44' -or $databaseSource -notmatch 'exportSchema\s*=\s*true') {
    throw 'InternalDatabase must remain at version 44 with Room schema export enabled.'
}
foreach ($directory in @($currentSchemas, $legacySchemas, $olderLegacySchemas)) {
    if (-not (Test-Path -LiteralPath $directory)) { throw "Required Room schema history is missing: $directory" }
}

foreach ($version in 1..44) {
    $name = "$version.json"
    $current = Join-Path $currentSchemas $name
    $legacy = Join-Path $legacySchemas $name
    if (-not (Test-Path -LiteralPath $current) -or -not (Test-Path -LiteralPath $legacy)) {
        throw "Missing Room schema version $version for current or legacy InternalDatabase."
    }
    $schema = Get-Content -LiteralPath $current -Raw | ConvertFrom-Json
    if ($schema.database.version -ne $version) { throw "Current Room schema $name has incorrect database version." }
    $currentHash = (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash
    $legacyHash = (Get-FileHash -LiteralPath $legacy -Algorithm SHA256).Hash
    if ($currentHash -ne $legacyHash) { throw "Current Room schema $name diverges from preserved v$version legacy history." }
}

Write-Output 'Room schema validation passed: current com.auriqo v1-44 history matches preserved legacy v1-44 history; older com.music v1-38 history remains present.'
