package com.auriqo.music.wear.ui

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

object AuriqoWearColors {
    val Accent = Color(0xFFFFB20F)
    val Surface = Color(0xFF0F0F0F)
    val Muted = Color(0xFFB0AFA8)

    val themeColors =
        Colors(
            primary = Accent,
            primaryVariant = Accent,
            secondary = Accent,
            secondaryVariant = Accent,
            background = Surface,
            surface = Surface,
            error = Color(0xFFB3261E),
            onPrimary = Surface,
            onSecondary = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Muted,
            onError = Color.White,
        )
}
