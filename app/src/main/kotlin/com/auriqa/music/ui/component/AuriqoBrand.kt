package com.auriqo.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auriqo.music.R
import com.auriqo.music.ui.theme.AuriqoBrandFontFamily

private val AuriqoLime = Color(0xFFD8F36A)
private val AuriqoInk = Color(0xFF17302A)

/**
 * Stable wordmark used wherever the app identifies itself in the interface.
 *
 * It deliberately does not read [MaterialTheme.typography], which is driven by the custom-font
 * preference. Cabinet Grotesk therefore remains the Auriqo face in every user configuration.
 */
@Composable
fun AuriqoWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = stringResource(R.string.app_name),
        modifier = modifier,
        color = color,
        fontFamily = AuriqoBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = (-0.35).sp,
        maxLines = 1,
    )
}

/**
 * The complete Auriqo lockup: the Cabinet Grotesk Q orbit with its lowercase a plus the fixed wordmark.
 */
@Composable
fun AuriqoBrand(
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    wordmarkFontSize: TextUnit = 22.sp,
    wordmarkColor: Color = MaterialTheme.colorScheme.onSurface,
    showWordmark: Boolean = true,
) {
    val appName = stringResource(R.string.app_name)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.28f))
                .background(AuriqoLime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.auriqo_logo_orbit),
                contentDescription = if (showWordmark) null else appName,
                tint = Color.Unspecified,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(iconSize * 0.16f),
            )
            Text(
                text = "a",
                color = AuriqoInk,
                modifier = Modifier.clearAndSetSemantics {},
                style = TextStyle(
                    fontFamily = AuriqoBrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (iconSize.value * 0.48f).sp,
                    letterSpacing = (-0.06).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
            )
        }

        if (showWordmark) {
            AuriqoWordmark(
                fontSize = wordmarkFontSize,
                color = wordmarkColor,
            )
        }
    }
}
