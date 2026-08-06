<# Validates Auriqo's generated visual assets without requiring an Android SDK. #>
param([string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Set-Location $RepositoryRoot

function Assert-True([bool]$condition, [string]$message) {
    if (!$condition) { throw "VALIDATION FAILED: $message" }
}

function Get-PngSize([string]$path) {
    $image = [System.Drawing.Image]::FromFile($path)
    try { return @($image.Width, $image.Height) } finally { $image.Dispose() }
}

$xmlPaths = @(
    'app/src/main/AndroidManifest.xml',
    'app/src/main/res/values/colors.xml',
    'app/src/main/res/values/ic_launcher_background.xml',
    'app/src/main/res/values/styles.xml',
    'app/src/main/res/values-night/styles.xml',
    'app/src/main/res/values-v31/styles.xml',
    'app/src/main/res/drawable/ic_launcher.xml',
    'app/src/main/res/drawable/auriqo_music_icon.xml',
    'app/src/main/res/drawable/ic_launcher_background_v31.xml',
    'app/src/main/res/drawable-v31/ic_launcher_background_v31.xml',
    'app/src/main/res/drawable/ic_auriqo_foreground.xml',
    'app/src/main/res/drawable/ic_auriqo_monochrome.xml',
    'app/src/main/res/drawable/ic_auriqo_notification.xml',
    'app/src/main/res/drawable/ic_auriqo_splash.xml',
    'app/src/main/res/mipmap-anydpi/ic_launcher_static.xml',
    'app/src/main/res/mipmap-anydpi/ic_launcher_static_round.xml',
    'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    'app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml',
    'app/src/main/res/mipmap-anydpi-v26/legacy_icon.xml',
    'app/src/main/res/mipmap-anydpi-v26/legacy_icon_round.xml',
    'assets/auriqo-logo.svg',
    'assets/auriqo-feature-graphic.svg'
)
foreach ($path in $xmlPaths) {
    $document = [System.Xml.XmlDocument]::new()
    $document.Load((Join-Path $RepositoryRoot $path))
}

$adaptivePaths = $xmlPaths | Where-Object { $_ -match 'mipmap-anydpi' }
foreach ($path in $adaptivePaths) {
    $text = Get-Content -Raw $path
    Assert-True ($text -match '<adaptive-icon') "$path is not an adaptive icon"
    Assert-True ($text -match '<monochrome android:drawable="@drawable/ic_auriqo_monochrome"') "$path lacks Auriqo's monochrome layer"
}

$notification = Get-Content -Raw 'app/src/main/res/drawable/ic_auriqo_notification.xml'
Assert-True ($notification -match '<vector') 'notification small icon is not a vector'
Assert-True ($notification -match 'fillColor="#FFFFFFFF"') 'notification small icon is not monochrome white'
Assert-True ($notification -notmatch '<path[^>]+fillColor="#FF[0-9A-Fa-f]{6}"[^>]+pathData="M0,0') 'notification small icon contains a background'

$manifest = Get-Content -Raw 'app/src/main/AndroidManifest.xml'
Assert-True ($manifest -match 'android:name="\.MainActivity"[\s\S]*?android:theme="@style/Theme\.auriqo\.Starting"') 'MainActivity does not use Theme.auriqo.Starting'
$splashStyles = Get-Content -Raw 'app/src/main/res/values/styles.xml'
Assert-True ($splashStyles -match '<style name="Theme\.auriqo\.Starting" parent="Theme\.SplashScreen">') 'AndroidX SplashScreen parent missing'
Assert-True ($splashStyles -match 'windowSplashScreenAnimatedIcon">@drawable/ic_auriqo_splash') 'splash icon is missing'
Assert-True ($splashStyles -match 'postSplashScreenTheme">@style/Theme\.auriqo') 'postSplashScreenTheme is missing'
$nightStylesDocument = [System.Xml.XmlDocument]::new()
$nightStylesDocument.Load((Join-Path $RepositoryRoot 'app/src/main/res/values-night/styles.xml'))
$dottedNightStyles = $nightStylesDocument.SelectNodes('/resources/style[contains(@name, ".")]')
foreach ($style in $dottedNightStyles) {
    Assert-True $style.HasAttribute('parent') "dotted night style $($style.GetAttribute('name')) has an implicit parent"
}
$nightTheme = $nightStylesDocument.SelectSingleNode('/resources/style[@name="Theme.auriqo"]')
Assert-True ($nightTheme.GetAttribute('parent') -eq 'android:Theme.Material.NoActionBar') 'night Auriqo theme does not use an explicit dark Android parent'

$expected = @{}
$sizes = @{ 'ldpi' = 36; 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
foreach ($density in $sizes.Keys) {
    foreach ($name in @('ic_launcher.png', 'ic_launcher_round.png', 'legacy_icon.png', 'legacy_icon_round.png')) {
        $expected["app/src/main/res/mipmap-$density/$name"] = @($sizes[$density], $sizes[$density])
    }
}
foreach ($density in @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')) {
    $multiplier = @{ 'mdpi' = 1; 'hdpi' = 1.5; 'xhdpi' = 2; 'xxhdpi' = 3; 'xxxhdpi' = 4 }[$density]
    foreach ($name in @('ic_launcher_foreground.png', 'ic_launcher_monochrome.png', 'legacy_icon_foreground.png', 'legacy_icon_monochrome.png')) {
        $expected["app/src/main/res/mipmap-$density/$name"] = @([int](108 * $multiplier), [int](108 * $multiplier))
    }
}
foreach ($density in @('mdpi', 'hdpi', 'xxhdpi', 'xxxhdpi')) { $expected["app/src/main/res/mipmap-$density/ic_launcher_static.png"] = @(512, 512) }
foreach ($name in @('auriqo_launcher_legacy.png', 'ic_launcher_foreground.png', 'ic_launcher_monochrome.png', 'ic_launcher_nobg.png', 'auriqonotification.png', 'legacy_icon_raster.png')) {
    $size = @{ 'auriqo_launcher_legacy.png' = 512; 'ic_launcher_foreground.png' = 432; 'ic_launcher_monochrome.png' = 432; 'ic_launcher_nobg.png' = 1080; 'auriqonotification.png' = 512; 'legacy_icon_raster.png' = 512 }[$name]
    $expected["app/src/main/res/drawable/$name"] = @($size, $size)
}
$expected['app/src/main/res/mipmap-xhdpi/tv_banner.png'] = @(320, 180)
$expected['assets/auriqo-feature-graphic.png'] = @(1024, 500)

$pngCount = 0
foreach ($path in $expected.Keys) {
    $size = Get-PngSize (Join-Path $RepositoryRoot $path)
    Assert-True ($size[0] -eq $expected[$path][0] -and $size[1] -eq $expected[$path][1]) "$path is $($size[0])x$($size[1]), expected $($expected[$path][0])x$($expected[$path][1])"
    $pngCount++
}

$monochromePaths = $expected.Keys | Where-Object { $_ -match 'monochrome\.png$' }
foreach ($path in $monochromePaths) {
    $bitmap = [System.Drawing.Bitmap]::new((Join-Path $RepositoryRoot $path))
    try {
        Assert-True ($bitmap.GetPixel(0, 0).A -eq 0) "$path lacks transparent corners"
        $hasOpaquePixel = $false
        for ($x = 0; $x -lt $bitmap.Width -and !$hasOpaquePixel; $x += [Math]::Max(1, [int]($bitmap.Width / 32))) {
            for ($y = 0; $y -lt $bitmap.Height; $y += [Math]::Max(1, [int]($bitmap.Height / 32))) {
                if ($bitmap.GetPixel($x, $y).A -gt 0) { $hasOpaquePixel = $true; break }
            }
        }
        Assert-True $hasOpaquePixel "$path has no visible monochrome mark"
    } finally { $bitmap.Dispose() }
}

$brandTextResources = $xmlPaths | Where-Object { $_ -match 'app/src/main/res|assets/auriqo' }
$redReferences = rg -n -i 'e63c28|f13[0-9a-f]{3}|echo' $brandTextResources 2>$null
Assert-True (!$redReferences) 'inherited Echo/red identity remains in owned brand resources'

Write-Output "Validated $($xmlPaths.Count) XML/SVG files, $pngCount readable PNGs, $($adaptivePaths.Count) adaptive icons, and a 1024x500 feature graphic."
