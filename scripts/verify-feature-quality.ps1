Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Require-Text([string] $content, [string] $pattern, [string] $description) {
    if ($content -notmatch $pattern) {
        throw "Missing safeguard: $description"
    }
}

$root = Split-Path -Parent $PSScriptRoot
$buildFile = Join-Path $root "app/build.gradle.kts"
$appFile = Join-Path $root "app/src/main/kotlin/com/auriqo/music/App.kt"
$mainActivityFile = Join-Path $root "app/src/main/kotlin/com/auriqo/music/MainActivity.kt"
$widgetDirectory = Join-Path $root "app/src/main/kotlin/com/auriqo/music/widget"
$build = Get-Content -Raw $buildFile
$app = Get-Content -Raw $appFile
$mainActivity = Get-Content -Raw $mainActivityFile

Require-Text $build "isMinifyEnabled\s*=\s*true" "release code shrinking"
Require-Text $build "isShrinkResources\s*=\s*true" "release resource shrinking"
Require-Text $build "proguard-rules\.pro" "release ProGuard rules"
foreach ($abi in "arm64-v8a", "armeabi-v7a", '"x86"', "x86_64") {
    Require-Text $build ([regex]::Escape($abi)) "ABI split for $abi"
}

Require-Text $app 'Trace\.beginSection\("Auriqo\.CipherDeobfuscator\.initialize"\)' "cold-start Perfetto trace"
Require-Text $app "StartupMetrics\.mark" "cold-start Logcat measurements"
Require-Text $app "MemoryCache\.Builder" "bounded Coil memory cache"
Require-Text $app "DiskCache\.Builder" "bounded Coil disk cache"
if ($app -match "runBlocking") { throw "Startup must not block while configuring Coil" }
Require-Text $mainActivity "private fun isUiSmokeTest\(\): Boolean = BuildConfig\.DEBUG" "debug-gated instrumentation controls"
Require-Text $mainActivity 'it in setOf\("home", "search_input", "settings"\)' "allowlisted smoke start destinations"

$widgetSources = Get-ChildItem -Path $widgetDirectory -Filter *.kt -Recurse | Get-Content -Raw
if ($widgetSources -match "ImageLoader\.Builder") { throw "Widgets must reuse the application Coil singleton" }
$cachedRequestCount = ([regex]::Matches($widgetSources, "diskCachePolicy\(CachePolicy\.ENABLED\)")).Count
if ($cachedRequestCount -lt 3) { throw "Widget artwork requests must opt into Coil disk caching" }

$manifest = [xml](Get-Content -Raw (Join-Path $root "app/src/main/AndroidManifest.xml"))
if ($null -eq $manifest.manifest) { throw "AndroidManifest.xml did not parse" }

$featureTests = Get-Content -Raw (Join-Path $root "app/src/androidTest/kotlin/com/auriqo/music/ui/FeatureSurfaceSmokeTest.kt")
Require-Text $featureTests "ActivityScenarioRule<MainActivity>\(smokeActivityIntent\(\)\)" "configured MainActivity smoke launch"
Require-Text $featureTests "createEmptyComposeRule\(\)" "Compose rule attached before activity launch"
Require-Text $featureTests "import androidx\.compose\.ui\.test\.junit4\.createComposeRule" "supported JUnit4 Compose rule"
Require-Text $featureTests "PlayerTransportButton" "production player component coverage"
Require-Text $featureTests "EXTRA_UI_SMOKE_TEST, true" "initial smoke intent flag"
Require-Text $featureTests "import androidx\.test\.ext\.junit\.rules\.ActivityScenarioRule" "supported AndroidX ActivityScenarioRule"
if ($featureTests -match "import androidx\.test\.rule\.ActivityScenarioRule") {
    throw "Use androidx.test.ext.junit.rules.ActivityScenarioRule from the declared dependency"
}
if ($featureTests -match "activity\.intent\s*=|\.recreate\(\)") {
    throw "Smoke tests must not mutate MainActivity intent or recreate it after launch"
}
if ($featureTests -match "import androidx\.compose\.ui\.test\.createComposeRule") {
    throw "Use androidx.compose.ui.test.junit4.createComposeRule for Android instrumentation tests"
}
if ($featureTests -match "@Composable\s+private\s+fun") {
    throw "Smoke tests must not define test-local composable surface substitutes"
}
foreach ($surface in "home.surface", "player.transport", "search.surface", "settings.surface", "settings.player.surface") {
    Require-Text $featureTests ([regex]::Escape($surface)) "Compose smoke coverage for $surface"
}
foreach ($tagSource in @(
    "app/src/main/kotlin/com/auriqo/music/ui/screens/HomeScreen.kt",
    "app/src/main/kotlin/com/auriqo/music/ui/player/Player.kt",
    "app/src/main/kotlin/com/auriqo/music/ui/screens/search/SearchScreen.kt",
    "app/src/main/kotlin/com/auriqo/music/ui/screens/settings/SettingsScreen.kt",
    "app/src/main/kotlin/com/auriqo/music/ui/screens/settings/PlayerSettings.kt",
    "app/src/main/kotlin/com/auriqo/music/ui/component/FloatingNavigationToolbar.kt"
)) {
    $source = Get-Content -Raw (Join-Path $root $tagSource)
    Require-Text $source "testTag" "production semantics in $tagSource"
}
$floatingToolbar = Get-Content -Raw (Join-Path $root "app/src/main/kotlin/com/auriqo/music/ui/component/FloatingNavigationToolbar.kt")
Require-Text $floatingToolbar 'testTag\("navigation\.\$\{screen\.route\}"\)' "compact navigation route semantics"

$buildDocumentation = Get-Content -Raw (Join-Path $root "BUILD.md")
$architectureDocumentation = Get-Content -Raw (Join-Path $root "ARCHITECTURE.md")
$planDocumentation = Get-Content -Raw (Join-Path $root "PLAN.md")
$releaseDocumentation = Get-Content -Raw (Join-Path $root "RELEASE_INFO.md")
$readmeDocumentation = Get-Content -Raw (Join-Path $root "README.md")
$contributingDocumentation = Get-Content -Raw (Join-Path $root "CONTRIBUTING.md")

Require-Text $buildDocumentation '4/4' "recorded GMD smoke result in BUILD.md"
Require-Text $buildDocumentation 'universal release candidate' "universal-candidate status in BUILD.md"
Require-Text $buildDocumentation 'GitHub' "candidate-validation location in BUILD.md"
Require-Text $architectureDocumentation 'four smoke tests' "recorded GMD smoke result in ARCHITECTURE.md"
Require-Text $architectureDocumentation 'universal release candidate' "universal-candidate status in ARCHITECTURE.md"
Require-Text $planDocumentation '2026-08-05' "recorded GMD smoke date in PLAN.md"
Require-Text $planDocumentation 'APK universal' "universal-candidate status in PLAN.md"
Require-Text $releaseDocumentation 'all 4 smoke tests' "recorded GMD smoke result in RELEASE_INFO.md"
Require-Text $releaseDocumentation 'universal-candidate' "universal-candidate status in RELEASE_INFO.md"
Require-Text $readmeDocumentation '4/4' "recorded GMD smoke result in README.md"
Require-Text $readmeDocumentation 'universal candidate validation' "universal-candidate status in README.md"
Require-Text $contributingDocumentation 'do not claim Room or DataStore migration execution coverage' "truthful migration-test guidance"

if ($contributingDocumentation -match 'data-migration') {
    throw "CONTRIBUTING.md must not claim data-migration coverage without an executing migration test"
}

$staleDeviceClaim = 'Local Android-device execution remains pending|local Android execution remains pending|GMD local execution pending|pending Android SDK license/toolchain'
$releaseDocumentationSet = [string]::Join("`n", @(
    $buildDocumentation,
    $architectureDocumentation,
    $planDocumentation,
    $releaseDocumentation,
    $readmeDocumentation
))
if ($releaseDocumentationSet -match $staleDeviceClaim) {
    throw "Release documentation contains a stale claim that managed-device execution is still pending"
}

$welcomeDialog = Get-Content -Raw (Join-Path $root "app/src/main/kotlin/com/auriqo/music/ui/screens/WelcomeDialog.kt")
$defaultStrings = Get-Content -Raw (Join-Path $root "app/src/main/res/values/auriqo_strings.xml")
Require-Text $welcomeDialog 'stringResource\(R\.string\.welcome_tagline\)' "localized welcome tagline usage"
Require-Text $defaultStrings '<string name="welcome_tagline">' "default welcome-tagline resource"

Write-Output "FEATURE_QUALITY_CHECK=pass"
Write-Output "COIL_WIDGET_DISK_CACHE_REQUESTS=$cachedRequestCount"
Write-Output "APK_SIZE_SAFEGUARDS=shrink-resources,proguard,abi-flavors"
