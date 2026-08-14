package com.auriqo.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

val DefaultMeshPalette = listOf(
    Color(0xFF8E44AD),
    Color(0xFF2C3E9C),
    Color(0xFFE75480),
    Color(0xFFE79C3C),
)

@Composable
fun rememberArtPalette(thumbnailUrl: String?): List<Color> {
    val context = LocalContext.current
    var colors by remember(thumbnailUrl) { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl.isNullOrBlank()) return@LaunchedEffect
        colors = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap()
                    ?: return@runCatching emptyList<Color>()
                val palette = Palette.from(bitmap)
                    .maximumColorCount(8)
                    .resizeBitmapArea(100 * 100)
                    .generate()
                listOfNotNull(
                    palette.vibrantSwatch?.rgb,
                    palette.lightVibrantSwatch?.rgb,
                    palette.darkVibrantSwatch?.rgb,
                    palette.mutedSwatch?.rgb,
                    palette.dominantSwatch?.rgb,
                )
                    .take(4)
                    .map { Color(it) }
                    .ifEmpty {
                        val dominant = palette.getDominantColor(DefaultMeshPalette.first().toArgb())
                        listOf(Color(dominant), DefaultMeshPalette[1], DefaultMeshPalette[2], DefaultMeshPalette[3])
                    }
            }.getOrDefault(emptyList())
        }
    }

    return colors
}

@Composable
fun LyricsMeshBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.6f,
) {
    val palette = colors.ifEmpty { DefaultMeshPalette }

    val transition = rememberInfiniteTransition(label = "lyricsMesh")

    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blob1",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 21000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blob2",
    )
    val phase3 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blob3",
    )
    val phase4 by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blob4",
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val blobs = listOf(
                Triple(palette[0], phase1, 1.1f),
                Triple(palette.getOrElse(1) { palette[0] }, phase2, 0.9f),
                Triple(palette.getOrElse(2) { palette[0] }, phase3, 0.8f),
                Triple(palette.getOrElse(3) { palette[0] }, phase4, 1.2f),
            )
            blobs.forEachIndexed { index, (color, phase, radiusFactor) ->
                val angle = phase * 2f * PI.toFloat() + index * 1.7f
                val center = Offset(
                    x = size.width * (0.5f + 0.32f * sin(angle)),
                    y = size.height * (0.5f + 0.28f * sin(angle * 1.37f + 0.8f)),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f)),
                        center = center,
                        radius = minDim * radiusFactor,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha)),
        )
    }
}
