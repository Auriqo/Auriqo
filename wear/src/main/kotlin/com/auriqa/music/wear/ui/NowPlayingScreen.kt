package com.auriqo.music.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil3.compose.AsyncImage
import com.auriqo.music.wear.media.MediaBrowserManager
import com.auriqo.music.wear.media.NowPlaying

private val AccentColor = Color(0xFFFFB20F)
private val SurfaceColor = Color(0xFF0F0F0F)
private val MutedTextColor = Color(0xFFB0AFA8)

@Composable
fun NowPlayingScreen() {
    val state by MediaBrowserManager.nowPlaying.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SurfaceColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "☀ AURIQO",
            color = AccentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        if (state.connected && !state.title.isNullOrBlank()) {
            Artwork(state = state)

            Spacer(Modifier.height(10.dp))

            Text(
                text = state.title.orEmpty(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.artist.orEmpty(),
                color = MutedTextColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            TransportControls(state = state)
        } else {
            Text(
                text = "Auriqo no conectado",
                color = MutedTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            state.error?.let { error ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = error,
                    color = AccentColor,
                    fontSize = 10.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            CompactChip(
                onClick = { MediaBrowserManager.ensureConnected(context) },
                label = { Text("Reintentar") },
            )
        }
    }
}

@Composable
private fun Artwork(state: NowPlaying) {
    val shape = CircleShape
    AsyncImage(
        model = state.artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(96.dp)
                .clip(shape)
                .background(Color(0xFF2A2A28)),
    )
}

@Composable
private fun TransportControls(state: NowPlaying) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.canSkipPrevious) {
            CompactChip(
                onClick = { MediaBrowserManager.skipToPrevious() },
                label = { Text("⏮") },
                colors = chipColors(),
            )
        }

        Button(
            onClick = { MediaBrowserManager.togglePlayPause() },
            colors =
                ButtonDefaults.buttonColors(
                    backgroundColor = AccentColor,
                    contentColor = SurfaceColor,
                ),
            modifier = Modifier.size(48.dp),
        ) {
            Text(if (state.isPlaying) "⏸" else "▶")
        }

        if (state.canSkipNext) {
            CompactChip(
                onClick = { MediaBrowserManager.skipToNext() },
                label = { Text("⏭") },
                colors = chipColors(),
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.likeAction != null) {
            CompactChip(
                onClick = { MediaBrowserManager.toggleLike() },
                label = { Text("♡") },
                colors = chipColors(),
            )
            Spacer(Modifier.width(4.dp))
        }
        CompactChip(
            onClick = { MediaBrowserManager.toggleShuffle() },
            label = { Text("⇄") },
            colors = chipColors(highlighted = state.shuffleEnabled),
        )
        Spacer(Modifier.width(4.dp))
        CompactChip(
            onClick = { MediaBrowserManager.toggleRepeatMode() },
            label = { Text("↻") },
            colors = chipColors(highlighted = state.repeatMode != 0),
        )
    }
}

@Composable
private fun chipColors(highlighted: Boolean = false) =
    if (highlighted) {
        ChipDefaults.chipColors(
            backgroundColor = AccentColor,
            contentColor = SurfaceColor,
        )
    } else {
        ChipDefaults.chipColors(
            backgroundColor = Color(0xFF2A2A28),
            contentColor = Color.White,
        )
    }
