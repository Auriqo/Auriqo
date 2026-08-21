package com.auriqo.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auriqo.music.R

/**
 * Displays YouTube metrics without mixing their labels into the item subtitle.
 */
@Composable
fun MediaStatsRow(
    views: String? = null,
    likes: String? = null,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary,
) {
    if (views == null && likes == null) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        views?.let {
            MediaStat(
                icon = R.drawable.view_count,
                description = stringResource(R.string.views),
                value = compactMetricText(it),
                color = color,
            )
        }
        likes?.let {
            MediaStat(
                icon = R.drawable.favorite_border,
                description = stringResource(R.string.likes),
                value = compactMetricText(it),
                color = color,
            )
        }
    }
}

@Composable
private fun MediaStat(
    icon: Int,
    description: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun compactMetricText(value: String): String = value
    .replace(
        Regex(
            "\\s*(views?|vistas?|vues|reproducciones?|visualizaciones?)$",
            RegexOption.IGNORE_CASE,
        ),
        "",
    )
    .trim()
