package com.auriqo.music.wear.ui

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import coil3.compose.AsyncImage
import com.auriqo.music.wear.R
import com.auriqo.music.wear.media.BrowseItem
import com.auriqo.music.wear.media.BrowseSection
import com.auriqo.music.wear.media.NowPlaying
import com.auriqo.music.wear.media.PhoneSyncManager
import kotlinx.coroutines.delay

private enum class WearDestination {
    NOW_PLAYING,
    HOME,
    TRACKS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    QUEUE,
}

@Composable
fun NowPlayingScreen(modifier: Modifier = Modifier) {
    val state by PhoneSyncManager.nowPlaying.collectAsState()
    val context = LocalContext.current
    val rotaryFocusRequester = remember { FocusRequester() }
    var routeName by rememberSaveable { mutableStateOf(WearDestination.NOW_PLAYING.name) }
    val destination = WearDestination.valueOf(routeName)

    LaunchedEffect(destination) {
        rotaryFocusRequester.requestFocus()
    }

    BackHandler(enabled = destination != WearDestination.NOW_PLAYING) {
        routeName = if (destination == WearDestination.HOME) {
            WearDestination.NOW_PLAYING.name
        } else {
            WearDestination.HOME.name
        }
    }

    Box(
        modifier = modifier
            .background(AuriqoWearColors.Surface)
            .onRotaryScrollEvent { event ->
                val direction =
                    when {
                        event.verticalScrollPixels > 0f -> 1
                        event.verticalScrollPixels < 0f -> -1
                        else -> 0
                    }
                if (direction != 0) {
                    PhoneSyncManager.adjustVolume(context, direction)
                }
                direction != 0
            }
            .focusRequester(rotaryFocusRequester)
            .focusable(),
    ) {
        when (destination) {
            WearDestination.NOW_PLAYING -> NowPlayingSurface(
                state = state,
                context = context,
                onOpenHome = { routeName = WearDestination.HOME.name },
            )

            WearDestination.HOME -> HomeSurface(
                state = state,
                onOpen = { target -> routeName = target.name },
                onBack = { routeName = WearDestination.NOW_PLAYING.name },
            )

            WearDestination.TRACKS,
            WearDestination.ALBUMS,
            WearDestination.ARTISTS,
            WearDestination.PLAYLISTS,
            WearDestination.QUEUE -> BrowseSurface(
                section = destination.toBrowseSection(),
                state = state,
                context = context,
                onBack = { routeName = WearDestination.HOME.name },
                onOpenNowPlaying = { routeName = WearDestination.NOW_PLAYING.name },
            )
        }
    }
}

private fun WearDestination.toBrowseSection(): BrowseSection =
    when (this) {
        WearDestination.TRACKS -> BrowseSection.TRACKS
        WearDestination.ALBUMS -> BrowseSection.ALBUMS
        WearDestination.ARTISTS -> BrowseSection.ARTISTS
        WearDestination.PLAYLISTS -> BrowseSection.PLAYLISTS
        WearDestination.QUEUE -> BrowseSection.QUEUE
        else -> error("Not a browse destination: $this")
    }

@Composable
private fun NowPlayingSurface(
    state: NowPlaying,
    context: Context,
    onOpenHome: () -> Unit,
) {
    val displayPosition by produceState(
        initialValue = state.positionAt(SystemClock.elapsedRealtime()),
        key1 = state,
    ) {
        while (true) {
            value = state.positionAt(SystemClock.elapsedRealtime())
            delay(if (state.isPlaying) 250L else 1_000L)
        }
    }

    if (state.connected && !state.title.isNullOrBlank()) {
        ConnectedNowPlaying(
            state = state,
            position = displayPosition,
            context = context,
            onOpenHome = onOpenHome,
        )
    } else {
        DisconnectedPlayer(state = state, context = context, onOpenHome = onOpenHome)
    }
}

@Composable
private fun ConnectedNowPlaying(
    state: NowPlaying,
    position: Long,
    context: Context,
    onOpenHome: () -> Unit,
) {
    var showModes by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 1.dp),
    ) {
        ScreenHeader(
            label = "NOW PLAYING",
            onBack = onOpenHome,
            backDescription = "Abrir biblioteca",
        )
        Spacer(Modifier.height(9.dp))

        Text(
            text = state.title.orEmpty(),
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = state.artist.orEmpty(),
            color = AuriqoWearColors.Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(7.dp))

        if (state.durationMs > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(position),
                    color = AuriqoWearColors.Muted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(5.dp))
                ProgressRail(
                    positionMs = position,
                    durationMs = state.durationMs,
                    modifier = Modifier.weight(1f),
                    onSeek = { PhoneSyncManager.seekTo(context, it) },
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = formatTime(state.durationMs),
                    color = AuriqoWearColors.Muted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(7.dp))
        TransportControls(state = state, context = context)
        Spacer(Modifier.weight(1f))

        if (showModes) {
            SecondaryControls(state = state, context = context)
            Spacer(Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auriqo_wear),
                contentDescription = if (showModes) "Ocultar controles adicionales" else "Mostrar controles adicionales",
                tint = Color.Unspecified,
                modifier = Modifier
                    .offset(y = (-7).dp)
                    .size(22.dp)
                    .clickable { showModes = !showModes }
                    .semantics { role = Role.Button },
            )
        }
    }
}

@Composable
private fun HomeSurface(
    state: NowPlaying,
    onOpen: (WearDestination) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 5.dp),
    ) {
        ScreenHeader(label = "AURIQO", onBack = onBack, backDescription = "Volver a Now Playing")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "YOUR MUSIC",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(5.dp))

        if (!state.title.isNullOrBlank()) {
            MiniNowPlaying(
                state = state,
                onClick = { onOpen(WearDestination.NOW_PLAYING) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = "LIBRARY",
            color = AuriqoWearColors.Accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(2.dp))
        HomeAction("TRACKS", "Canciones guardadas") { onOpen(WearDestination.TRACKS) }
        HomeAction("ALBUMS", "Álbumes de tu biblioteca") { onOpen(WearDestination.ALBUMS) }
        HomeAction("ARTISTS", "Artistas guardados") { onOpen(WearDestination.ARTISTS) }
        HomeAction("PLAYLISTS", "Tus listas") { onOpen(WearDestination.PLAYLISTS) }
        HomeAction("QUEUE", "Orden de reproducción") { onOpen(WearDestination.QUEUE) }
    }
}

@Composable
private fun BrowseSurface(
    section: BrowseSection,
    state: NowPlaying,
    context: Context,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val browseState by PhoneSyncManager.browse.collectAsState()
    LaunchedEffect(section) {
        PhoneSyncManager.requestBrowse(context, section)
    }

    val remoteItems = browseState.items.takeIf { browseState.section == section }.orEmpty()
    val items = remoteItems.ifEmpty { fallbackItems(section, state) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 2.dp),
    ) {
        ScreenHeader(label = section.label, onBack = onBack, backDescription = "Abrir biblioteca")
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (browseState.loading && remoteItems.isEmpty()) "SINCRONIZANDO CON AURIQO" else "TOCAR PARA REPRODUCIR",
            color = AuriqoWearColors.Muted,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.85.sp,
        )
        Spacer(Modifier.height(3.dp))

        if (items.isEmpty()) {
            EmptyBrowse(
                message = browseState.error ?: "No hay elementos guardados todavía",
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEach { item ->
                    BrowseRow(
                        item = item,
                        onClick = {
                            PhoneSyncManager.playBrowseItem(context, item)
                            onOpenNowPlaying()
                        },
                    )
                }
            }
        }
    }
}

private fun fallbackItems(section: BrowseSection, state: NowPlaying): List<BrowseItem> =
    if (section == BrowseSection.TRACKS || section == BrowseSection.QUEUE) {
        state.title?.takeIf(String::isNotBlank)?.let { title ->
            listOf(
                BrowseItem(
                    id = state.mediaId ?: "current",
                    title = title,
                    subtitle = state.artist.orEmpty(),
                    artworkUri = state.artworkUri,
                    kind = if (section == BrowseSection.QUEUE) "queue" else "track",
                ),
            )
        }.orEmpty()
    } else {
        emptyList()
    }

@Composable
private fun ScreenHeader(
    label: String,
    onBack: () -> Unit,
    backDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(27.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics {
                    contentDescription = backDescription
                    role = Role.Button
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "‹",
                color = AuriqoWearColors.Accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Text(
            text = label,
            color = AuriqoWearColors.Accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.25.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "AURIQO",
            color = AuriqoWearColors.Muted.copy(alpha = 0.82f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
        )
    }
}

@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(43.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = title
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .background(AuriqoWearColors.Accent),
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.45.sp,
            )
            Text(
                text = subtitle,
                color = AuriqoWearColors.Muted,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "›",
            color = AuriqoWearColors.Muted,
            fontSize = 19.sp,
        )
    }
}

@Composable
private fun MiniNowPlaying(
    state: NowPlaying,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Abrir Now Playing"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(uri = state.artworkUri, title = state.title.orEmpty(), size = 34.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.orEmpty(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist.orEmpty(),
                color = AuriqoWearColors.Muted,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "▶",
            color = AuriqoWearColors.Accent,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BrowseRow(
    item: BrowseItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Reproducir ${item.title}"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(uri = item.artworkUri, title = item.title, size = 35.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                color = AuriqoWearColors.Muted,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "▶",
            color = AuriqoWearColors.Accent.copy(alpha = 0.85f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ArtworkThumbnail(uri: String?, title: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(AuriqoWearColors.RaisedSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (uri.isNullOrBlank()) {
            Text(
                text = title.take(1).uppercase(),
                color = AuriqoWearColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EmptyBrowse(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "NO HAY DATOS",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = message,
            color = AuriqoWearColors.Muted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TransportControls(state: NowPlaying, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainControl(
            iconRes = R.drawable.ic_previous,
            description = "Canción anterior",
            enabled = state.canSkipPrevious,
            size = 50.dp,
            iconSize = 27.dp,
            onClick = { PhoneSyncManager.skipToPrevious(context) },
        )
        PlainControl(
            iconRes = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            description = if (state.isPlaying) "Pausar" else "Reproducir",
            enabled = true,
            size = 64.dp,
            iconSize = 43.dp,
            tint = AuriqoWearColors.Accent,
            onClick = { PhoneSyncManager.togglePlayPause(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_next,
            description = "Canción siguiente",
            enabled = state.canSkipNext,
            size = 50.dp,
            iconSize = 27.dp,
            onClick = { PhoneSyncManager.skipToNext(context) },
        )
    }
}

@Composable
private fun SecondaryControls(state: NowPlaying, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainControl(
            iconRes = R.drawable.ic_heart,
            description = if (state.isLiked) "Quitar Me gusta" else "Me gusta",
            enabled = state.canLike,
            tint = if (state.isLiked) AuriqoWearColors.Accent else Color.White,
            size = 42.dp,
            iconSize = 18.dp,
            onClick = { PhoneSyncManager.toggleLike(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_shuffle,
            description = if (state.shuffleEnabled) "Desactivar aleatorio" else "Activar aleatorio",
            enabled = true,
            tint = if (state.shuffleEnabled) AuriqoWearColors.Accent else Color.White,
            size = 42.dp,
            iconSize = 18.dp,
            onClick = { PhoneSyncManager.toggleShuffle(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_repeat,
            description = "Cambiar repetición",
            enabled = true,
            tint = if (state.repeatMode != 0) AuriqoWearColors.Accent else Color.White,
            size = 42.dp,
            iconSize = 18.dp,
            badge = if (state.repeatMode == 1) "1" else null,
            onClick = { PhoneSyncManager.toggleRepeatMode(context) },
        )
    }
}

@Composable
private fun PlainControl(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean,
    tint: Color = Color.White,
    badge: String? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.28f)
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
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
        badge?.let {
            Text(
                text = it,
                color = tint,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
            )
        }
    }
}

@Composable
private fun ProgressRail(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
) {
    val progress =
        if (durationMs > 0L) {
            positionMs.toFloat() / durationMs.toFloat()
        } else {
            0f
        }

    Box(
        modifier = modifier
            .height(26.dp)
            .pointerInput(durationMs) {
                if (durationMs > 0L) {
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    fun seekAt(x: Float) {
                        val fraction = (x / width).coerceIn(0f, 1f)
                        onSeek((durationMs * fraction).toLong())
                    }
                    detectDragGestures(
                        onDragStart = { offset -> seekAt(offset.x) },
                        onDrag = { change, _ ->
                            change.consume()
                            seekAt(change.position.x)
                        },
                    )
                }
            }
            .semantics {
                contentDescription = "Arrastrar para buscar en la canción"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            val y = size.height / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = AuriqoWearColors.Accent,
                start = Offset(0f, y),
                end = Offset(size.width * progress.coerceIn(0f, 1f), y),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DisconnectedPlayer(
    state: NowPlaying,
    context: Context,
    onOpenHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(label = "NOW PLAYING", onBack = onOpenHome, backDescription = "Abrir biblioteca")
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_auriqo_wear),
            contentDescription = "Auriqo",
            tint = Color.Unspecified,
            modifier = Modifier.size(47.dp),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "SIN SESIÓN",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Reproducí algo en Auriqo",
            color = AuriqoWearColors.Muted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
        state.error?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                color = AuriqoWearColors.Muted.copy(alpha = 0.8f),
                fontSize = 8.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = "RECONECTAR",
            color = AuriqoWearColors.Accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .clickable(role = Role.Button) { PhoneSyncManager.ensureConnected(context) }
                .semantics {
                    contentDescription = "Reconectar con el teléfono"
                    role = Role.Button
                },
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_auriqo_wear),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
