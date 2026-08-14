package com.auriqo.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auriqo.music.api.formatPlaylistAttributionDate

@Composable
fun PlaylistProvenanceLine(
    addedBy: String?,
    addedAt: String?,
    source: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(16.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = buildString {
                append(addedBy ?: "Autor no disponible")
                formatPlaylistAttributionDate(addedAt)?.let {
                    append(" · ")
                    append(it)
                }
                append(" · ")
                append(source)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

@Composable
fun PlaylistStatsStrip(
    songCount: Int,
    durationLabel: String,
    contributorCount: Int,
    latestAddedAt: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaylistStat("Canciones", songCount.toString(), Modifier.weight(1f))
        PlaylistStat("Duración", durationLabel, Modifier.weight(1f))
        PlaylistStat("Autores", contributorCount.toString(), Modifier.weight(1f))
        PlaylistStat(
            "Última alta",
            formatPlaylistAttributionDate(latestAddedAt) ?: "Sin datos",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlaylistStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}
