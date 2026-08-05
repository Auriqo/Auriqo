<#
Generates Auriqo's local brand rasters from the A-and-wave geometry in
assets/auriqo-logo.svg. Run from the repository root:
  powershell -ExecutionPolicy Bypass -File scripts/generate_visual_assets.ps1
#>
param([string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$midnight = [System.Drawing.ColorTranslator]::FromHtml('#1A1A2E')
$navy = [System.Drawing.ColorTranslator]::FromHtml('#16213E')
$gold = [System.Drawing.ColorTranslator]::FromHtml('#FFB20F')
$amber = [System.Drawing.ColorTranslator]::FromHtml('#FF8C00')
$offWhite = [System.Drawing.ColorTranslator]::FromHtml('#F7F5EF')

function New-Canvas([int]$width, [int]$height) {
    $bitmap = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    return @($bitmap, $graphics)
}

function Save-Png([System.Drawing.Bitmap]$bitmap, [string]$path) {
    $directory = Split-Path -Parent $path
    if (!(Test-Path $directory)) { New-Item -ItemType Directory -Path $directory -Force | Out-Null }
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Draw-AuriqoMark([System.Drawing.Graphics]$graphics, [float]$x, [float]$y, [float]$size, [bool]$monochrome = $false) {
    $state = $graphics.Save()
    $graphics.TranslateTransform($x, $y)
    $graphics.ScaleTransform($size / 108.0, $size / 108.0)
    $start = if ($monochrome) { [System.Drawing.Color]::White } else { $gold }
    $end = if ($monochrome) { [System.Drawing.Color]::White } else { $amber }
    $accent = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.PointF]::new(31, 31), [System.Drawing.PointF]::new(77, 78), $start, $end)
    $left = [System.Drawing.PointF[]]@([System.Drawing.PointF]::new(31,78), [System.Drawing.PointF]::new(47,31), [System.Drawing.PointF]::new(52,31), [System.Drawing.PointF]::new(41,78))
    $right = [System.Drawing.PointF[]]@([System.Drawing.PointF]::new(77,78), [System.Drawing.PointF]::new(61,31), [System.Drawing.PointF]::new(56,31), [System.Drawing.PointF]::new(67,78))
    $graphics.FillPolygon($accent, $left)
    $graphics.FillPolygon($accent, $right)
    $graphics.FillEllipse($accent, 62, 34, 8, 8)
    $wave = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $wave.AddBezier(39, 58.5, 43, 58.5, 45, 58.5, 46, 58.5)
    $wave.AddBezier(46, 58.5, 49, 52, 52, 52, 54, 58.5)
    $wave.AddBezier(54, 58.5, 56, 65, 59, 65, 61, 58.5)
    $wave.AddBezier(61, 58.5, 63, 52, 66, 52, 68, 58.5)
    $wave.AddBezier(68, 58.5, 70, 65, 73, 65, 75, 58.5)
    $wave.AddBezier(75, 58.5, 76, 58.5, 77, 58.5, 78, 58.5)
    $pen = [System.Drawing.Pen]::new($accent, 3.5)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawPath($pen, $wave)
    $pen.Dispose(); $wave.Dispose(); $accent.Dispose()
    $graphics.Restore($state)
}

function Draw-Launcher([int]$size, [bool]$round = $false, [bool]$monochrome = $false) {
    $bitmap, $graphics = New-Canvas $size $size
    $graphics.Clear([System.Drawing.Color]::Transparent)
    if (!$monochrome) {
        $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.Point]::new(0,0), [System.Drawing.Point]::new($size,$size), $midnight, $navy)
        $graphics.FillRectangle($background, 0, 0, $size, $size)
        $background.Dispose()
        $ring = [System.Drawing.Pen]::new($gold, [Math]::Max(1, $size * 0.008))
        $ring.Color = [System.Drawing.Color]::FromArgb(90, $gold)
        $graphics.DrawEllipse($ring, $size * .10, $size * .10, $size * .80, $size * .80)
        $ring.Dispose()
    }
    Draw-AuriqoMark $graphics ($size * .10) ($size * .10) ($size * .80) $monochrome
    $graphics.Dispose()
    return $bitmap
}

function Draw-Foreground([int]$size, [bool]$monochrome = $false) {
    $bitmap, $graphics = New-Canvas $size $size
    $graphics.Clear([System.Drawing.Color]::Transparent)
    # The artwork occupies the central 66% of the 108dp adaptive viewport.
    Draw-AuriqoMark $graphics ($size * .17) ($size * .17) ($size * .66) $monochrome
    $graphics.Dispose()
    return $bitmap
}

function Draw-FeatureGraphic {
    $bitmap, $graphics = New-Canvas 1024 500
    $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.Point]::new(0,0), [System.Drawing.Point]::new(1024,500), $midnight, $navy)
    $graphics.FillRectangle($background, 0, 0, 1024, 500); $background.Dispose()
    $wavePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(86, $gold), 3)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddBezier(0,430,171,368,288,483,462,426); $path.AddBezier(462,426,650,364,740,362,1024,452)
    $graphics.DrawPath($wavePen, $path); $path.Dispose(); $wavePen.Dispose()
    $ring = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, $gold), 4)
    $disc = [System.Drawing.SolidBrush]::new($navy)
    $graphics.FillEllipse($disc, 94, 94, 308, 308); $disc.Dispose()
    $graphics.DrawEllipse($ring, 94, 94, 308, 308); $ring.Dispose()
    Draw-AuriqoMark $graphics 126 126 244 $false
    $titleFont = [System.Drawing.Font]::new('Segoe UI', 72, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $labelFont = [System.Drawing.Font]::new('Segoe UI', 18, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $bodyFont = [System.Drawing.Font]::new('Segoe UI', 22, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawString('Auriqo', $titleFont, [System.Drawing.SolidBrush]::new($offWhite), 438, 136)
    $graphics.DrawString('L I S T E N   W I T H   I N T E N T', $labelFont, [System.Drawing.SolidBrush]::new($gold), 444, 250)
    $divider = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(64, $offWhite), 1); $graphics.DrawLine($divider, 444, 313, 878, 313); $divider.Dispose()
    $graphics.DrawString('Your music, in focus.', $bodyFont, [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(194, $offWhite)), 444, 333)
    $titleFont.Dispose(); $labelFont.Dispose(); $bodyFont.Dispose(); $graphics.Dispose()
    return $bitmap
}

function Draw-TvBanner {
    $bitmap, $graphics = New-Canvas 320 180
    $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.Point]::new(0,0), [System.Drawing.Point]::new(320,180), $midnight, $navy)
    $graphics.FillRectangle($background, 0, 0, 320, 180); $background.Dispose()
    Draw-AuriqoMark $graphics 26 36 108 $false
    $title = [System.Drawing.Font]::new('Segoe UI', 28, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $sub = [System.Drawing.Font]::new('Segoe UI', 10, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawString('Auriqo', $title, [System.Drawing.SolidBrush]::new($offWhite), 142, 60)
    $graphics.DrawString('LISTEN WITH INTENT', $sub, [System.Drawing.SolidBrush]::new($gold), 145, 98)
    $title.Dispose(); $sub.Dispose(); $graphics.Dispose()
    return $bitmap
}

$res = Join-Path $RepositoryRoot 'app/src/main/res'
$feature = Draw-FeatureGraphic; Save-Png $feature (Join-Path $RepositoryRoot 'assets/auriqo-feature-graphic.png'); $feature.Dispose()
$banner = Draw-TvBanner; Save-Png $banner (Join-Path $res 'mipmap-xhdpi/tv_banner.png'); $banner.Dispose()

$densitySizes = @{ 'ldpi' = 36; 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
foreach ($density in $densitySizes.Keys) {
    $size = $densitySizes[$density]
    foreach ($name in @('ic_launcher.png', 'ic_launcher_round.png', 'legacy_icon.png', 'legacy_icon_round.png')) {
        $icon = Draw-Launcher $size ($name -match 'round') $false
        Save-Png $icon (Join-Path $res "mipmap-$density/$name"); $icon.Dispose()
    }
}
foreach ($density in @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')) {
    $multiplier = @{ 'mdpi' = 1; 'hdpi' = 1.5; 'xhdpi' = 2; 'xxhdpi' = 3; 'xxxhdpi' = 4 }[$density]
    $size = [int](108 * $multiplier)
    foreach ($pair in @(@('ic_launcher_foreground.png',$false), @('ic_launcher_monochrome.png',$true), @('legacy_icon_foreground.png',$false), @('legacy_icon_monochrome.png',$true))) {
        $icon = Draw-Foreground $size $pair[1]
        Save-Png $icon (Join-Path $res "mipmap-$density/$($pair[0])"); $icon.Dispose()
    }
}
foreach ($density in @('mdpi', 'hdpi', 'xxhdpi', 'xxxhdpi')) {
    $icon = Draw-Launcher 512 $false $false
    Save-Png $icon (Join-Path $res "mipmap-$density/ic_launcher_static.png"); $icon.Dispose()
}
foreach ($pair in @(@('auriqo_launcher_legacy.png', 512, $false), @('ic_launcher_foreground.png', 432, $false), @('ic_launcher_monochrome.png', 432, $true), @('ic_launcher_nobg.png', 1080, $false), @('auriqonotification.png', 512, $false), @('legacy_icon_raster.png', 512, $false))) {
    $icon = if ($pair[0] -eq 'ic_launcher_foreground.png' -or $pair[0] -eq 'ic_launcher_monochrome.png' -or $pair[0] -eq 'ic_launcher_nobg.png') { Draw-Foreground $pair[1] $pair[2] } else { Draw-Launcher $pair[1] $false $false }
    Save-Png $icon (Join-Path $res "drawable/$($pair[0])"); $icon.Dispose()
}

Write-Output "Generated Auriqo feature graphic, TV banner, 6 legacy launcher densities, 5 adaptive foreground densities, and drawable fallbacks."
