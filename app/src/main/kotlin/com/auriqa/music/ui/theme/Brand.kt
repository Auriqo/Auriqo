package com.auriqo.music.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.auriqo.music.R

/**
 * The Auriqo wordmark is part of the product identity, not user-customisable typography.
 *
 * Keep this family separate from the app/lyrics/player font targets so changing a custom font
 * cannot alter the name of the app.
 */
val AuriqoBrandFontFamily = FontFamily(
    Font(R.font.cabinet_grotesk_regular, FontWeight.Normal),
    Font(R.font.cabinet_grotesk_bold, FontWeight.Bold),
)
