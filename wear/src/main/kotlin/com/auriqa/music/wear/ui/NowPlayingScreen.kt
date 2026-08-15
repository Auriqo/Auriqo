package com.auriqo.music.wear.ui

import android.content.Context
import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import coil3.compose.AsyncImage
import com.auriqo.music.wear.R
import com.auriqo.music.wear.media.NowPlaying
import com.auriqo.music.wear.media.PhoneSyncManager
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(modifier: Modifier = Modifier) {
    val state by PhoneSyncManager.nowPlaying.collectAsState()
    val context = LocalContext.current
    val displayPosition by produceState(
        initialValue = state.positionAt(SystemClock.elapsedRealtime()),
        key1 = state,
    ) {
        while (true) {
            value = state.positionAt(SystemClock.elapsedRealtime())
            delay(if (state.isPlaying) 250L else 1_000L)
        }
    }

    Box(
        modifier =
            modifier
                .background(AuriqoWearColors.Surface),
    ) {
        state.artworkUri?.let { artwork ->
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(28.dp)
                        .alpha(0.22f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xB8080B0A),
                        0.45f to Color(0xD9080B0A),
                        1f to AuriqoWearColors.Surface,
                    ),
                ),
        )

        if (state.connected && !state.title.isNullOrBlank()) {
            ConnectedPlayer(
                state = state,
                displayPosition = displayPosition,
                context = context,
            )
        } else {
            DisconnectedPlayer(state = state, context = context)
        }
    }
}

@Composable
private fun ConnectedPlayer(
    state: NowPlaying,
    displayPosition: Long,
    context: Context,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 25.dp, bottom = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AuriqoStatus(connected = true)

        ArtworkProgress(
            artworkUri = state.artworkUri,
            progress = if (state.durationMs > 0L) displayPosition.toFloat() / state.durationMs else 0f,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.title.orEmpty(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.artist.orEmpty(),
                color = AuriqoWearColors.Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.durationMs > 0L) {
                Text(
                    text = "${formatTime(displayPosition)}  /  ${formatTime(state.durationMs)}",
                    color = AuriqoWearColors.Muted.copy(alpha = 0.82f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        TransportControls(state = state, context = context)
        SecondaryControls(state = state, context = context)
    }
}

@Composable
private fun AuriqoStatus(connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_auriqo_wear),
            contentDescription = "Auriqo",
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (connected) AuriqoWearColors.Accent else AuriqoWearColors.Muted),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (connected) "TELÉFONO" else "SIN ENLACE",
            color = AuriqoWearColors.Muted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
        )
    }
}

@Composable
private fun ArtworkProgress(artworkUri: String?, progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(86.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = AuriqoWearColors.Accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(AuriqoWearColors.RaisedSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUri.isNullOrBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_auriqo_wear),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(38.dp),
                )
            } else {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = "Carátula",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TransportControls(state: NowPlaying, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            iconRes = R.drawable.ic_previous,
            description = "Canción anterior",
            enabled = state.canSkipPrevious,
            size = 40.dp,
            onClick = { PhoneSyncManager.skipToPrevious(context) },
        )
        Spacer(Modifier.width(9.dp))
        ControlButton(
            iconRes = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            description = if (state.isPlaying) "Pausar" else "Reproducir",
            primary = true,
            size = 52.dp,
            onClick = { PhoneSyncManager.togglePlayPause(context) },
        )
        Spacer(Modifier.width(9.dp))
        ControlButton(
            iconRes = R.drawable.ic_next,
            description = "Canción siguiente",
            enabled = state.canSkipNext,
            size = 40.dp,
            onClick = { PhoneSyncManager.skipToNext(context) },
        )
    }
}

@Composable
private fun SecondaryControls(state: NowPlaying, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            iconRes = R.drawable.ic_heart,
            description = if (state.isLiked) "Quitar Me gusta" else "Me gusta",
            enabled = state.canLike,
            active = state.isLiked,
            size = 36.dp,
            onClick = { PhoneSyncManager.toggleLike(context) },
        )
        Spacer(Modifier.width(7.dp))
        ControlButton(
            iconRes = R.drawable.ic_shuffle,
            description = if (state.shuffleEnabled) "Desactivar aleatorio" else "Activar aleatorio",
            active = state.shuffleEnabled,
            size = 36.dp,
            onClick = { PhoneSyncManager.toggleShuffle(context) },
        )
        Spacer(Modifier.width(7.dp))
        ControlButton(
            iconRes = R.drawable.ic_repeat,
            description = "Cambiar repetición",
            active = state.repeatMode != 0,
            badge = if (state.repeatMode == 1) "1" else null,
            size = 36.dp,
            onClick = { PhoneSyncManager.toggleRepeatMode(context) },
        )
    }
}

@Composable
private fun ControlButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    enabled: Boolean = true,
    primary: Boolean = false,
    active: Boolean = false,
    badge: String? = null,
) {
    val highlighted = primary || active
    val background = if (highlighted) AuriqoWearColors.Accent else AuriqoWearColors.RaisedSurface
    val foreground = if (highlighted) AuriqoWearColors.AccentInk else Color.White
    Box(
        modifier =
            Modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.34f)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (highlighted) Modifier else Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(if (primary) 25.dp else 18.dp),
        )
        badge?.let {
            Text(
                text = it,
                color = foreground,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 13.dp, bottom = 11.dp),
            )
        }
    }
}

@Composable
private fun DisconnectedPlayer(state: NowPlaying, context: Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_auriqo_wear),
            contentDescription = "Auriqo",
            tint = Color.Unspecified,
            modifier = Modifier.size(58.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Abrí Auriqo en el teléfono",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        state.error?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                color = AuriqoWearColors.Muted,
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(AuriqoWearColors.Accent)
                    .clickable(role = Role.Button) { PhoneSyncManager.ensureConnected(context) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "RECONECTAR",
                color = AuriqoWearColors.AccentInk,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
