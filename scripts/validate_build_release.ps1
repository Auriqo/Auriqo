param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$appBuild = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/build.gradle.kts') -Raw
$releaseWorkflow = Get-Content -LiteralPath (Join-Path $ProjectRoot '.github/workflows/gradle.yml') -Raw
$gradleProperties = Get-Content -LiteralPath (Join-Path $ProjectRoot 'gradle.properties') -Raw
$proguardRules = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/proguard-rules.pro') -Raw
$listenTogetherProto = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/proto/listentogether.proto') -Raw
$databaseSource = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/db/MusicDatabase.kt') -Raw
$gmsManifest = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/gms/AndroidManifest.xml') -Raw
$fossTelemetry = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/foss/kotlin/com/auriqo/music/privacy/VariantTelemetry.kt') -Raw
$gmsTelemetry = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/gms/kotlin/com/auriqo/music/privacy/VariantTelemetry.kt') -Raw
$telemetryDialog = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/privacy/TelemetryConsentDialog.kt') -Raw
$privacySettings = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/ui/screens/settings/PrivacySettings.kt') -Raw
$privacyLicensesStrings = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/res/values/privacy_licenses_strings.xml') -Raw
$releaseArtifacts = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/appupdate/ReleaseApkArtifacts.kt') -Raw
$updateNotification = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/appupdate/UpdateNotificationHelper.kt') -Raw
$updater = Get-Content -LiteralPath (Join-Path $ProjectRoot 'app/src/main/kotlin/com/auriqo/music/appupdate/updater/AuriqoUpdater.kt') -Raw

function Require-Match([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) { throw $Message }
}

Require-Match $appBuild 'versionCode\s*=\s*527' 'versionCode must be 527.'
Require-Match $appBuild 'versionName\s*=\s*"1\.0\.0"' 'versionName must be 1.0.0.'
foreach ($flavor in 'foss', 'gms', 'universal', 'arm64', 'armeabi', 'x86', 'x86_64') {
    Require-Match $appBuild "create\(`"$flavor`"\)" "Missing required flavor: $flavor."
}
foreach ($abi in 'universal', 'arm64', 'armeabi', 'x86', 'x86_64') {
    Require-Match $appBuild "buildConfigField\(`"String`", `"ARCHITECTURE`", `"\\`"$abi\\`"`"\)" "ABI flavor $abi must define its own BuildConfig.ARCHITECTURE."
}
if ($appBuild -match 'buildConfigField\("String", "ARCHITECTURE", "\\"(?:release|debug)\\""\)') {
    throw 'Build types must not override the ABI flavor BuildConfig.ARCHITECTURE value.'
}
foreach ($field in 'LISTEN_TOGETHER_SERVER_URL', 'LISTEN_TOGETHER_SHARE_BASE_URL') {
    Require-Match $appBuild "`"$field`"" "Missing BuildConfig field: $field."
}
Require-Match $appBuild 'buildConfigString\(projectValue\("LISTEN_TOGETHER_SERVER_URL"\)\)' 'Listen Together server URL must remain blank-safe.'
Require-Match $appBuild 'checkReleaseSigning' 'Missing actionable signed-release validation task.'
Require-Match $appBuild 'val releaseSigningGuardTaskName = "checkReleaseSigning"' 'Release signing guard task name must be centralized.'
Require-Match $appBuild 'val releaseSigningTaskPrefixes = listOf\("validateSigning", "package", "bundle", "sign"\)' 'Signed release guard must target signing/package/bundle tasks only.'
Require-Match $appBuild 'name != releaseSigningGuardTaskName && isReleaseSigningTask' 'Release signing guard must exclude itself.'
if ($appBuild -match 'if \(name\.contains\("Release", ignoreCase = true\)\)') {
    throw 'Release signing guard is overly broad and can self-depend.'
}
Require-Match $appBuild 'STORE_PASSWORD' 'Release signing must use STORE_PASSWORD.'
Require-Match $appBuild 'KEY_ALIAS' 'Release signing must use KEY_ALIAS.'
Require-Match $appBuild 'KEY_PASSWORD' 'Release signing must use KEY_PASSWORD.'
if ($gradleProperties -match '(?m)^\s*(?:FLOW_NEURO_API_KEY|LASTFM_API_KEY|GH_CLIENT_SECRET)\s*=') {
    throw 'gradle.properties must not carry API keys or client secrets.'
}
if ($proguardRules -match 'com\.soniqo\.music') { throw 'ProGuard rules still use the legacy com.soniqo.music namespace.' }
Require-Match $proguardRules 'com\.auriqo\.music\.listentogether' 'ProGuard rules must preserve Auriqo Listen Together serialization.'
Require-Match $listenTogetherProto '(?m)^package com\.auriqo\.music\.listentogether\.proto;' 'Listen Together proto package must use com.auriqo.'
Require-Match $listenTogetherProto 'option java_package = "com\.auriqo\.music\.listentogether\.proto";' 'Listen Together Java package must use com.auriqo.'
Require-Match $databaseSource 'version\s*=\s*44' 'InternalDatabase Room version must remain 44.'
Require-Match $databaseSource 'exportSchema\s*=\s*true' 'InternalDatabase must export Room schemas.'
Require-Match $releaseWorkflow ':app:assembleUniversalFossRelease' 'Release workflow must build the universal FOSS release APK.'
foreach ($task in 'assembleArm64FossRelease', 'assembleArmeabiFossRelease', 'assembleX86FossRelease', 'assembleX86_64FossRelease') {
    if ($releaseWorkflow -match ":app:$task") { throw "Release workflow must not build ABI-specific APKs: $task." }
}
Require-Match $releaseWorkflow '-PrequireReleaseSigning=true' 'Release workflow must explicitly require signing.'
foreach ($task in 'assembleUniversalFossDebug', 'testUniversalFossDebugUnitTest', 'lintUniversalFossDebug', 'assembleUniversalGmsDebug', 'testUniversalGmsDebugUnitTest', 'lintUniversalGmsDebug') {
    Require-Match $releaseWorkflow ":app:$task" "Validation workflow must exercise $task."
}
Require-Match $appBuild 'managedDevices\s*\{\s*localDevices\s*\{\s*create\("auriqoApi30"\)' 'Missing auriqoApi30 Gradle Managed Device.'
Require-Match $appBuild 'device\s*=\s*"Pixel 2"' 'Gradle Managed Device must use the Pixel 2 hardware profile.'
Require-Match $appBuild 'apiLevel\s*=\s*30' 'Gradle Managed Device must use the stable API 30 image.'
Require-Match $appBuild 'systemImageSource\s*=\s*"aosp-atd"' 'Gradle Managed Device must use the lightweight aosp-atd image.'
Require-Match $releaseWorkflow ':app:auriqoApi30UniversalFossDebugAndroidTest' 'Validation workflow must run the Universal FOSS managed-device instrumentation task.'
Require-Match $releaseWorkflow 'timeout-minutes:\s*30' 'Managed-device instrumentation must have a bounded CI timeout.'
Require-Match $releaseWorkflow 'app/build/reports/androidTests/' 'Managed-device reports must be uploaded on failure.'
Require-Match $releaseWorkflow 'app/build/outputs/managed_device_android_test_additional_output/' 'Managed-device additional output must be uploaded on failure.'
Require-Match $releaseWorkflow 'app/build/outputs/apk/universalFoss/debug/\*\.apk' 'Debug artifact upload must use the actual universalFoss output directory.'
Require-Match $releaseWorkflow 'app/build/outputs/apk/universalFoss/release/\*\.apk' 'Release artifact staging must use the actual universalFoss output directory.'
Require-Match $releaseWorkflow 'release-artifacts/auriqo-foss-universal\.apk' 'Release artifact staging must use the deterministic universal APK name.'
Require-Match $releaseWorkflow '\(cd release-artifacts && sha256sum auriqo-foss-universal\.apk > SHA256SUMS\)' 'Release workflow must create a SHA-256 checksum with the universal APK basename.'
if ($releaseWorkflow -match 'sha256sum release-artifacts/') {
    throw 'Release workflow must not write directory-prefixed APK paths into SHA256SUMS.'
}
Require-Match $releaseWorkflow 'stat -c' 'Release workflow must enforce APK size budgets.'
Require-Match $releaseWorkflow 'release-artifacts/\*' 'Release publication must use staged artifacts.'
$candidateJob = $releaseWorkflow.IndexOf('release-candidate:')
$publishJob = $releaseWorkflow.IndexOf('publish-release:')
if ($candidateJob -lt 0 -or $publishJob -lt 0 -or $candidateJob -gt $publishJob) {
    throw 'Workflow must build an explicit release-candidate job before publication.'
}
$candidateWorkflow = $releaseWorkflow.Substring($candidateJob, $publishJob - $candidateJob)
Require-Match $candidateWorkflow 'Build unsigned FOSS universal release candidate APK' 'Release candidate job must truthfully identify its unsigned universal build.'
Require-Match $candidateWorkflow 'auriqo-foss-release-candidate-unsigned-' 'Release candidate upload must be explicitly labeled unsigned.'
if ($candidateWorkflow -match 'requireReleaseSigning|RELEASE_KEYSTORE_BASE64|RELEASE_STORE_PASSWORD') {
    throw 'Unsigned release candidate job must not consume release signing secrets.'
}
$signingGate = $releaseWorkflow.IndexOf('Verify release signing configuration', $publishJob)
$pendingGate = $releaseWorkflow.IndexOf("grep -q '^PENDING:' RELEASE_INFO.md")
$publishStep = $releaseWorkflow.IndexOf('Publish GitHub release')
if ($signingGate -lt 0 -or $pendingGate -lt 0 -or $publishStep -lt 0 -or $signingGate -gt $publishStep -or $pendingGate -gt $publishStep) {
    throw 'Release publication must remain after signing and PENDING gates.'
}
Require-Match $gmsManifest 'firebase_analytics_collection_enabled"\s*\r?\n\s*android:value="false"' 'GMS analytics must default to disabled.'
Require-Match $gmsManifest 'firebase_crashlytics_collection_enabled"\s*\r?\n\s*android:value="false"' 'GMS Crashlytics must default to disabled.'
if ($fossTelemetry -match 'com\.google\.firebase') { throw 'FOSS telemetry implementation must not reference Firebase.' }
Require-Match $fossTelemetry 'fun isAvailable\(context: Context\): Boolean = false' 'FOSS telemetry must report unavailable.'
Require-Match $gmsTelemetry 'fun isAvailable\(context: Context\): Boolean = firebaseApp\(context\) != null' 'GMS availability must require Firebase configuration.'
Require-Match $telemetryDialog 'shouldPrompt\(telemetryAvailable, choice!!\)' 'First-launch telemetry prompt must be availability-gated.'
Require-Match $privacySettings 'shouldShowControls\(telemetryAvailable\)' 'Privacy telemetry controls must be availability-gated.'
Require-Match $gmsTelemetry 'setAnalyticsCollectionEnabled\(enabled\)' 'GMS analytics must follow persisted consent.'
Require-Match $gmsTelemetry 'setCrashlyticsCollectionEnabled\(enabled\)' 'GMS Crashlytics must follow persisted consent.'
Require-Match $privacyLicensesStrings '<string name="telemetry_consent_decline">Don\\''t allow</string>' 'Telemetry decline text must escape its apostrophe for AAPT while displaying "Don''t allow".'
if ($privacyLicensesStrings -match '<string\b[^>]*>[^<]*(?<!\\)''') {
    throw 'privacy_licenses_strings.xml contains an unescaped apostrophe in a string value; Android resources require \\''.'
}
if ($appBuild -match 'versionName\s*=\s*"5\.2\.84"|versionCode\s*=\s*526') { throw 'Stale release version remains active.' }
Require-Match $releaseArtifacts 'auriqo-foss-\$\{canonicalArchitecture\(architecture\)\}\.apk' 'Release APK naming must be centralized.'
Require-Match $releaseArtifacts 'return byName\[requested\] \?: byName\[assetNameFor\("universal"\)\.lowercase\(\)\]' 'Updater must choose ABI-specific APKs and only then universal fallback.'
if ($updater -match 'assetName\.endsWith\("\.apk"' -or $updater -match '/auriqo\.apk') {
    throw 'Updater must not pick the first APK or use the retired auriqo.apk URL.'
}
if ($updateNotification -match 'auriqo\.apk') {
    throw 'Update notification must use the selected compatible release asset, not a nonexistent auriqo.apk URL.'
}
Require-Match $updateNotification 'fun showUpdateNotification\(context: Context, versionName: String, apkUrl: String\)' 'Update notification must receive the updater-selected compatible asset URL.'

Write-Output 'build release validation passed: ABI BuildConfig preservation, deterministic universal release artifact staging, compatible updater selection with universal fallback, unsigned candidate/publication separation, signing gates, FOSS/GMS debug validation, and existing privacy/release checks.'
