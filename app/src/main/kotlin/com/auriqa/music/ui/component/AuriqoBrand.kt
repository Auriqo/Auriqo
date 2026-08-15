package com.auriqo.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auriqo.music.R

private val AuriqoLime = Color(0xFFD8F36A)

/**
 * Stable wordmark used wherever the app identifies itself in the interface.
 *
 * It is fixed vector artwork extracted from the Cabinet Grotesk outlines, so it does not read
 * [MaterialTheme.typography] and cannot be changed by the custom-font preference.
 */
@Composable
fun AuriqoWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val em = with(LocalDensity.current) { fontSize.toDp() }
    Icon(
        painter = painterResource(R.drawable.auriqo_wordmark),
        contentDescription = stringResource(R.string.app_name),
        tint = color,
        modifier = modifier
            .width(em * 2.899f)
            .height(em * 0.856f),
    )
}

/**
 * The complete Auriqo lockup: the Cabinet Grotesk Q orbit with its lowercase a plus the fixed
 * outlined wordmark.
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
                painter = painterResource(R.drawable.auriqo_logo_mark),
                contentDescription = if (showWordmark) null else appName,
                tint = Color.Unspecified,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(iconSize * 0.16f),
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
