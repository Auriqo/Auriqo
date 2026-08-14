package com.auriqo.music.ui.component

import androidx.compose.ui.graphics.Color
import com.auriqo.music.constants.LyricsSkin

data class LyricsSkinSpec(
    val activeLineColors: List<Color>,
    val glowColor: Color,
    val glowStrength: Float,
    val inactiveDimAlpha: Float,
    val activeAnimation: ActiveAnimation,
    val blurProfile: BlurProfile,
    val fontSizeMultiplier: Float,
) {
    enum class ActiveAnimation { SCALE, JUMP, NONE }
    enum class BlurProfile { OFF, LIGHT, MEDIUM, HEAVY }
}

fun LyricsSkin.spec(): LyricsSkinSpec = when (this) {
    LyricsSkin.DEFAULT -> LyricsSkinSpec(
        activeLineColors = emptyList(),
        glowColor = Color.White,
        glowStrength = 1f,
        inactiveDimAlpha = 0.33f,
        activeAnimation = LyricsSkinSpec.ActiveAnimation.SCALE,
        blurProfile = LyricsSkinSpec.BlurProfile.MEDIUM,
        fontSizeMultiplier = 1f,
    )
    LyricsSkin.HARMONY_GLOW -> LyricsSkinSpec(
        activeLineColors = listOf(
            Color(0xFF8E44AD),
            Color(0xFFE75480),
            Color(0xFFE79C3C),
        ),
        glowColor = Color(0xFFFFD700),
        glowStrength = 1.6f,
        inactiveDimAlpha = 0.4f,
        activeAnimation = LyricsSkinSpec.ActiveAnimation.JUMP,
        blurProfile = LyricsSkinSpec.BlurProfile.MEDIUM,
        fontSizeMultiplier = 1f,
    )
    LyricsSkin.LUXURIOUS_GLASS -> LyricsSkinSpec(
        activeLineColors = listOf(Color.White, Color(0xFFBBDDFF)),
        glowColor = Color.White,
        glowStrength = 2f,
        inactiveDimAlpha = 0.35f,
        activeAnimation = LyricsSkinSpec.ActiveAnimation.SCALE,
        blurProfile = LyricsSkinSpec.BlurProfile.MEDIUM,
        fontSizeMultiplier = 1f,
    )
    LyricsSkin.PASTEL -> LyricsSkinSpec(
        activeLineColors = listOf(
            Color(0xFFFFB3BA),
            Color(0xFFFFDBA8),
            Color(0xFFFFE6A8),
        ),
        glowColor = Color(0xFFFFDBA8),
        glowStrength = 1.2f,
        inactiveDimAlpha = 0.5f,
        activeAnimation = LyricsSkinSpec.ActiveAnimation.SCALE,
        blurProfile = LyricsSkinSpec.BlurProfile.LIGHT,
        fontSizeMultiplier = 1f,
    )
    LyricsSkin.TV_BLURRY -> LyricsSkinSpec(
        activeLineColors = listOf(Color.White, Color(0xFFDDDDFF)),
        glowColor = Color.White,
        glowStrength = 1.3f,
        inactiveDimAlpha = 0.25f,
        activeAnimation = LyricsSkinSpec.ActiveAnimation.NONE,
        blurProfile = LyricsSkinSpec.BlurProfile.HEAVY,
        fontSizeMultiplier = 1.55f,
    )
}

fun LyricsSkinSpec.distantLineBlur(distanceFromCurrent: Int): Float = when (blurProfile) {
    LyricsSkinSpec.BlurProfile.OFF -> 0f
    LyricsSkinSpec.BlurProfile.LIGHT -> when (distanceFromCurrent) {
        1, 2 -> 0f
        3 -> 1f
        4 -> 2f
        else -> 3f
    }
    LyricsSkinSpec.BlurProfile.MEDIUM -> when (distanceFromCurrent) {
        1, 2 -> 0f
        3 -> 2f
        4 -> 4f
        else -> 6f
    }
    LyricsSkinSpec.BlurProfile.HEAVY -> when (distanceFromCurrent) {
        1, 2 -> 0f
        3 -> 6f
        4 -> 10f
        else -> 16f
    }
}
