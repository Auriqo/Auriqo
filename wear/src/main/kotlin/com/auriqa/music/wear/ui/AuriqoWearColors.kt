package com.auriqo.music.wear.ui

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

object AuriqoWearColors {
    val Accent = Color(0xFFD8F36A)
    val AccentInk = Color(0xFF17302A)
    val Surface = Color(0xFF050606)
    val RaisedSurface = Color(0xFF151916)
    val Muted = Color(0xFFA6ADA7)

    val themeColors =
        Colors(
            primary = Accent,
            primaryVariant = Accent,
            secondary = Accent,
            secondaryVariant = Accent,
            background = Surface,
            surface = Surface,
            error = Color(0xFFB3261E),
            onPrimary = AccentInk,
            onSecondary = AccentInk,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Muted,
            onError = Color.White,
        )
}
