package com.auriqo.music.wear.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * Wear Material tokens derived from the same Material You source as the phone
 * app. Wear still uses the Wear Material components so that circular layout
 * behavior and rotary-friendly surfaces remain intact.
 */
private data class WearPalette(
    val accent: Color,
    val accentInk: Color,
    val accentContainer: Color,
    val accentContainerInk: Color,
    val secondary: Color,
    val secondaryInk: Color,
    val secondaryContainer: Color,
    val secondaryContainerInk: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val raisedSurface: Color,
    val muted: Color,
    val onSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
)

private val FallbackPalette =
    WearPalette(
        accent = Color(0xFFD8F36A),
        accentInk = Color(0xFF17302A),
        accentContainer = Color(0xFF3C4A16),
        accentContainerInk = Color(0xFFE9FF9C),
        secondary = Color(0xFFD8F36A),
        secondaryInk = Color(0xFF17302A),
        secondaryContainer = Color(0xFF28322A),
        secondaryContainerInk = Color(0xFFD8E8D2),
        surface = Color(0xFF050606),
        surfaceContainer = Color(0xFF101411),
        raisedSurface = Color(0xFF151916),
        muted = Color(0xFFA6ADA7),
        onSurface = Color.White,
        outline = Color(0xFF6F786F),
        outlineVariant = Color(0xFF2F372F),
        error = Color(0xFFB3261E),
        onError = Color.White,
    )

private val LocalPalette = staticCompositionLocalOf { FallbackPalette }

private fun ColorScheme.toWearPalette(): WearPalette =
    WearPalette(
        accent = primary,
        accentInk = onPrimary,
        accentContainer = primaryContainer,
        accentContainerInk = onPrimaryContainer,
        secondary = secondary,
        secondaryInk = onSecondary,
        secondaryContainer = secondaryContainer,
        secondaryContainerInk = onSecondaryContainer,
        surface = background,
        surfaceContainer = surface,
        raisedSurface = surfaceVariant,
        muted = onSurfaceVariant,
        onSurface = onBackground,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
    )

private fun resolvePalette(context: Context, darkTheme: Boolean): WearPalette {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return FallbackPalette

    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    return colorScheme.toWearPalette()
}

private fun WearPalette.toWearColors() =
    Colors(
        primary = accent,
        primaryVariant = accent,
        secondary = secondary,
        secondaryVariant = secondary,
        background = surface,
        surface = surface,
        error = error,
        onPrimary = accentInk,
        onSecondary = secondaryInk,
        onBackground = onSurface,
        onSurface = onSurface,
        onSurfaceVariant = muted,
        onError = onError,
    )

object AuriqoWearColors {
    /** Applies Material You colors while retaining Wear Material components. */
    @Composable
    fun Theme(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val darkTheme = isSystemInDarkTheme()
        val palette = remember(context, darkTheme) {
            resolvePalette(context, darkTheme)
        }

        CompositionLocalProvider(LocalPalette provides palette) {
            MaterialTheme(
                colors = palette.toWearColors(),
                content = content,
            )
        }
    }

    val Accent: Color
        @Composable get() = LocalPalette.current.accent

    val AccentInk: Color
        @Composable get() = LocalPalette.current.accentInk

    val AccentContainer: Color
        @Composable get() = LocalPalette.current.accentContainer

    val AccentContainerInk: Color
        @Composable get() = LocalPalette.current.accentContainerInk

    val SecondaryContainer: Color
        @Composable get() = LocalPalette.current.secondaryContainer

    val SecondaryContainerInk: Color
        @Composable get() = LocalPalette.current.secondaryContainerInk

    val Surface: Color
        @Composable get() = LocalPalette.current.surface

    val SurfaceContainer: Color
        @Composable get() = LocalPalette.current.surfaceContainer

    val RaisedSurface: Color
        @Composable get() = LocalPalette.current.raisedSurface

    val Muted: Color
        @Composable get() = LocalPalette.current.muted

    val OnSurface: Color
        @Composable get() = LocalPalette.current.onSurface

    val Outline: Color
        @Composable get() = LocalPalette.current.outline

    val OutlineVariant: Color
        @Composable get() = LocalPalette.current.outlineVariant
}
