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
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
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
import kotlinx.coroutines.launch

private enum class WearDestination {
    NOW_PLAYING,
    HOME,
    TRACKS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    QUEUE,
}

private data class WearScreenSpec(
    val isRound: Boolean,
    val compact: Boolean,
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val itemSpacing: Dp,
    val cardCorner: Dp,
    val artworkSize: Dp,
    val headerActionSize: Dp,
    val sideControlSize: Dp,
    val sideIconSize: Dp,
    val primaryControlSize: Dp,
    val primaryIconSize: Dp,
    val secondaryControlSize: Dp,
    val headerHeight: Dp,
)

@Composable
private fun rememberWearScreenSpec(): WearScreenSpec {
    val configuration = LocalConfiguration.current
    val shortestEdgeDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val compact = shortestEdgeDp <= 240
    val isRound = configuration.isScreenRound

    return WearScreenSpec(
        isRound = isRound,
        compact = compact,
        horizontalPadding =
            when {
                isRound && compact -> 22.dp
                isRound -> 30.dp
                compact -> 14.dp
                else -> 18.dp
            },
        topPadding =
            when {
                isRound && compact -> 14.dp
                isRound -> 30.dp
                compact -> 14.dp
                else -> 18.dp
            },
        bottomPadding =
            when {
                isRound && compact -> 8.dp
                isRound -> 12.dp
                else -> 10.dp
            },
        itemSpacing = if (compact) 6.dp else 8.dp,
        cardCorner = if (isRound) 24.dp else 18.dp,
        artworkSize =
            when {
                compact -> 32.dp
                isRound -> 44.dp
                else -> 50.dp
            },
        headerActionSize = if (compact) 40.dp else 44.dp,
        sideControlSize = if (compact) 44.dp else 50.dp,
        sideIconSize = if (compact) 21.dp else 24.dp,
        primaryControlSize = if (compact) 52.dp else 62.dp,
        primaryIconSize = if (compact) 32.dp else 40.dp,
        secondaryControlSize = if (compact) 40.dp else 42.dp,
        headerHeight = if (compact) 40.dp else 44.dp,
    )
}

@Composable
fun NowPlayingScreen(modifier: Modifier = Modifier) {
    val state by PhoneSyncManager.nowPlaying.collectAsState()
    val context = LocalContext.current
    val rotaryFocusRequester = remember { FocusRequester() }
    var routeName by rememberSaveable { mutableStateOf(WearDestination.NOW_PLAYING.name) }
    val destination = WearDestination.valueOf(routeName)

    LaunchedEffect(destination) {
        if (destination == WearDestination.NOW_PLAYING) {
            rotaryFocusRequester.requestFocus()
        }
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
                if (destination != WearDestination.NOW_PLAYING) {
                    return@onRotaryScrollEvent false
                }
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
            .focusable(enabled = destination == WearDestination.NOW_PLAYING),
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
    val spec = rememberWearScreenSpec()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = spec.horizontalPadding,
                end = spec.horizontalPadding,
                top = spec.topPadding,
                bottom = spec.bottomPadding,
            ),
    ) {
        NowPlayingTopBar(
            state = state,
            onBack = onOpenHome,
            spec = spec,
        )
        Spacer(Modifier.height(if (spec.compact) 4.dp else spec.itemSpacing))

        NowPlayingIdentityCard(state = state, spec = spec)
        Spacer(Modifier.height(if (spec.compact) 4.dp else spec.itemSpacing))

        if (state.durationMs > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(position),
                    color = AuriqoWearColors.Muted,
                    fontSize = if (spec.compact) 7.sp else 8.sp,
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
                    fontSize = if (spec.compact) 7.sp else 8.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(if (spec.compact) 4.dp else spec.itemSpacing))
        TransportControls(state = state, context = context, spec = spec)
        Spacer(Modifier.height(if (spec.compact) 3.dp else 6.dp))
        SecondaryControls(state = state, context = context, spec = spec)
    }
}

@Composable
private fun NowPlayingTopBar(
    state: NowPlaying,
    onBack: () -> Unit,
    spec: WearScreenSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(spec.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            iconRes = R.drawable.ic_library,
            description = "Abrir biblioteca",
            onClick = onBack,
            spec = spec,
        )
        Spacer(Modifier.width(if (spec.compact) 8.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Reproduciendo",
                color = AuriqoWearColors.OnSurface,
                fontSize = if (spec.compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (spec.compact) 5.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (state.connected) AuriqoWearColors.Accent else AuriqoWearColors.Outline),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (state.connected) "Teléfono" else "Sin conexión",
                    color = AuriqoWearColors.Muted,
                    fontSize = if (spec.compact) 7.sp else 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_auriqo_wear),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(if (spec.compact) 18.dp else 21.dp),
        )
    }
}

@Composable
private fun NowPlayingIdentityCard(
    state: NowPlaying,
    spec: WearScreenSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spec.cardCorner))
            .background(AuriqoWearColors.SurfaceContainer)
            .padding(
                horizontal = if (spec.compact) 8.dp else 10.dp,
                vertical = if (spec.compact) 6.dp else 9.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(
            uri = state.artworkUri,
            title = state.title.orEmpty(),
            size = spec.artworkSize,
            corner = if (spec.isRound) 15.dp else 13.dp,
        )
        Spacer(Modifier.width(if (spec.compact) 8.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.orEmpty(),
                color = AuriqoWearColors.OnSurface,
                fontSize = if (spec.compact) 14.sp else 16.sp,
                lineHeight = if (spec.compact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (spec.compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = state.artist.orEmpty(),
                color = AuriqoWearColors.Muted,
                fontSize = if (spec.compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
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
    val spec = rememberWearScreenSpec()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                scope.launch { scrollState.scrollBy(event.verticalScrollPixels) }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .verticalScroll(scrollState)
            .padding(
                start = spec.horizontalPadding,
                end = spec.horizontalPadding,
                top = spec.topPadding,
                bottom = spec.bottomPadding,
            ),
    ) {
        ScreenHeader(label = "Auriqo", onBack = onBack, backDescription = "Volver a Now Playing", spec = spec)
        Spacer(Modifier.height(spec.itemSpacing))
        Text(
            text = "Tu música",
            color = AuriqoWearColors.OnSurface,
            fontSize = if (spec.compact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(if (spec.compact) 4.dp else 5.dp))

        if (!state.title.isNullOrBlank()) {
            MiniNowPlaying(
                state = state,
                spec = spec,
                onClick = { onOpen(WearDestination.NOW_PLAYING) },
            )
            Spacer(Modifier.height(spec.itemSpacing))
        }

        Text(
            text = "Biblioteca",
            color = AuriqoWearColors.Accent,
            fontSize = if (spec.compact) 8.sp else 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        HomeAction("Canciones", "Guardadas en Auriqo", spec) { onOpen(WearDestination.TRACKS) }
        Spacer(Modifier.height(5.dp))
        HomeAction("Álbumes", "Tu biblioteca", spec) { onOpen(WearDestination.ALBUMS) }
        Spacer(Modifier.height(5.dp))
        HomeAction("Artistas", "Guardados", spec) { onOpen(WearDestination.ARTISTS) }
        Spacer(Modifier.height(5.dp))
        HomeAction("Playlists", "Tus listas", spec) { onOpen(WearDestination.PLAYLISTS) }
        Spacer(Modifier.height(5.dp))
        HomeAction("Cola", "Orden de reproducción", spec) { onOpen(WearDestination.QUEUE) }
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
    val spec = rememberWearScreenSpec()
    val browseState by PhoneSyncManager.browse.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(section) {
        PhoneSyncManager.requestBrowse(context, section)
        focusRequester.requestFocus()
    }

    val remoteItems = browseState.items.takeIf { browseState.section == section }.orEmpty()
    val items = remoteItems.ifEmpty { fallbackItems(section, state) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                scope.launch { scrollState.scrollBy(event.verticalScrollPixels) }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .padding(
                start = spec.horizontalPadding,
                end = spec.horizontalPadding,
                top = spec.topPadding,
                bottom = spec.bottomPadding,
            ),
    ) {
        ScreenHeader(label = section.displayLabel(), onBack = onBack, backDescription = "Abrir biblioteca", spec = spec)
        Spacer(Modifier.height(if (spec.compact) 4.dp else 5.dp))
        Text(
            text = if (browseState.loading && remoteItems.isEmpty()) "Sincronizando" else "${items.size} elementos",
            color = AuriqoWearColors.Muted,
            fontSize = if (spec.compact) 8.sp else 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(if (spec.compact) 5.dp else 6.dp))

        if (items.isEmpty()) {
            EmptyBrowse(
                message = browseState.error ?: "No hay elementos guardados todavía",
                spec = spec,
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                items.forEach { item ->
                    BrowseRow(
                        item = item,
                        spec = spec,
                        onClick = {
                            PhoneSyncManager.playBrowseItem(context, item)
                            onOpenNowPlaying()
                        },
                    )
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    }
}

private fun BrowseSection.displayLabel(): String =
    when (this) {
        BrowseSection.TRACKS -> "Canciones"
        BrowseSection.ALBUMS -> "Álbumes"
        BrowseSection.ARTISTS -> "Artistas"
        BrowseSection.PLAYLISTS -> "Playlists"
        BrowseSection.QUEUE -> "Cola"
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
    spec: WearScreenSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(spec.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            iconRes = R.drawable.ic_chevron_left,
            description = backDescription,
            onClick = onBack,
            spec = spec,
        )
        Spacer(Modifier.width(if (spec.compact) 7.dp else 9.dp))
        Text(
            text = label,
            color = AuriqoWearColors.OnSurface,
            fontSize = if (spec.compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Auriqo",
            color = AuriqoWearColors.Muted.copy(alpha = 0.82f),
            fontSize = if (spec.compact) 7.sp else 8.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeaderIconButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    spec: WearScreenSpec,
) {
    Box(
        modifier = Modifier
            .size(spec.headerActionSize)
            .clip(CircleShape)
            .background(AuriqoWearColors.SurfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AuriqoWearColors.OnSurface,
            modifier = Modifier.size(if (spec.compact) 17.dp else 19.dp),
        )
    }
}

@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    spec: WearScreenSpec,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (spec.compact) 44.dp else 50.dp)
            .clip(RoundedCornerShape(spec.cardCorner))
            .background(AuriqoWearColors.SurfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (spec.compact) 10.dp else 12.dp)
            .semantics {
                contentDescription = title
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(if (spec.compact) 6.dp else 7.dp)
                .clip(CircleShape)
                .background(AuriqoWearColors.Accent),
        )
        Spacer(Modifier.width(if (spec.compact) 9.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = AuriqoWearColors.OnSurface,
                fontSize = if (spec.compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = AuriqoWearColors.Muted,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = AuriqoWearColors.Muted,
            modifier = Modifier.size(if (spec.compact) 15.dp else 17.dp),
        )
    }
}

@Composable
private fun MiniNowPlaying(
    state: NowPlaying,
    spec: WearScreenSpec,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (spec.compact) 48.dp else 54.dp)
            .clip(RoundedCornerShape(spec.cardCorner))
            .background(AuriqoWearColors.AccentContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (spec.compact) 8.dp else 10.dp)
            .semantics {
                contentDescription = "Abrir Now Playing"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(
            uri = state.artworkUri,
            title = state.title.orEmpty(),
            size = if (spec.compact) 32.dp else 36.dp,
            corner = if (spec.isRound) 12.dp else 10.dp,
        )
        Spacer(Modifier.width(if (spec.compact) 8.dp else 9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.orEmpty(),
                color = AuriqoWearColors.AccentContainerInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist.orEmpty(),
                color = AuriqoWearColors.AccentContainerInk.copy(alpha = 0.75f),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = null,
            tint = AuriqoWearColors.AccentContainerInk,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BrowseRow(
    item: BrowseItem,
    spec: WearScreenSpec,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (spec.compact) 48.dp else 54.dp)
            .clip(RoundedCornerShape(spec.cardCorner))
            .background(AuriqoWearColors.SurfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (spec.compact) 8.dp else 10.dp)
            .semantics {
                contentDescription = "Reproducir ${item.title}"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(
            uri = item.artworkUri,
            title = item.title,
            size = if (spec.compact) 32.dp else 36.dp,
            corner = if (spec.isRound) 12.dp else 10.dp,
        )
        Spacer(Modifier.width(if (spec.compact) 8.dp else 9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = AuriqoWearColors.OnSurface,
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
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = null,
            tint = AuriqoWearColors.Muted,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ArtworkThumbnail(
    uri: String?,
    title: String,
    size: Dp,
    corner: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(AuriqoWearColors.SecondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (uri.isNullOrBlank()) {
            Text(
                text = title.take(1).uppercase(),
                color = AuriqoWearColors.SecondaryContainerInk,
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
private fun EmptyBrowse(message: String, spec: WearScreenSpec) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(spec.cardCorner))
            .background(AuriqoWearColors.SurfaceContainer)
            .padding(horizontal = if (spec.compact) 14.dp else 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sin datos",
            color = AuriqoWearColors.OnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
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
private fun TransportControls(state: NowPlaying, context: Context, spec: WearScreenSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainControl(
            iconRes = R.drawable.ic_previous,
            description = "Canción anterior",
            enabled = state.canSkipPrevious,
            size = spec.sideControlSize,
            iconSize = spec.sideIconSize,
            container = AuriqoWearColors.SurfaceContainer,
            onClick = { PhoneSyncManager.skipToPrevious(context) },
        )
        PlainControl(
            iconRes = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            description = if (state.isPlaying) "Pausar" else "Reproducir",
            enabled = true,
            size = spec.primaryControlSize,
            iconSize = spec.primaryIconSize,
            tint = AuriqoWearColors.AccentInk,
            container = AuriqoWearColors.Accent,
            onClick = { PhoneSyncManager.togglePlayPause(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_next,
            description = "Canción siguiente",
            enabled = state.canSkipNext,
            size = spec.sideControlSize,
            iconSize = spec.sideIconSize,
            container = AuriqoWearColors.SurfaceContainer,
            onClick = { PhoneSyncManager.skipToNext(context) },
        )
    }
}

@Composable
private fun SecondaryControls(state: NowPlaying, context: Context, spec: WearScreenSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainControl(
            iconRes = R.drawable.ic_heart,
            description = if (state.isLiked) "Quitar Me gusta" else "Me gusta",
            enabled = state.canLike,
            tint = if (state.isLiked) AuriqoWearColors.AccentContainerInk else AuriqoWearColors.OnSurface,
            container = if (state.isLiked) AuriqoWearColors.AccentContainer else AuriqoWearColors.SurfaceContainer,
            size = spec.secondaryControlSize,
            iconSize = if (spec.compact) 16.dp else 18.dp,
            onClick = { PhoneSyncManager.toggleLike(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_shuffle,
            description = if (state.shuffleEnabled) "Desactivar aleatorio" else "Activar aleatorio",
            enabled = true,
            tint = if (state.shuffleEnabled) AuriqoWearColors.AccentContainerInk else AuriqoWearColors.OnSurface,
            container = if (state.shuffleEnabled) AuriqoWearColors.AccentContainer else AuriqoWearColors.SurfaceContainer,
            size = spec.secondaryControlSize,
            iconSize = if (spec.compact) 16.dp else 18.dp,
            onClick = { PhoneSyncManager.toggleShuffle(context) },
        )
        PlainControl(
            iconRes = R.drawable.ic_repeat,
            description = "Cambiar repetición",
            enabled = true,
            tint = if (state.repeatMode != 0) AuriqoWearColors.AccentContainerInk else AuriqoWearColors.OnSurface,
            container = if (state.repeatMode != 0) AuriqoWearColors.AccentContainer else AuriqoWearColors.SurfaceContainer,
            size = spec.secondaryControlSize,
            iconSize = if (spec.compact) 16.dp else 18.dp,
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
    tint: Color? = null,
    container: Color? = null,
    badge: String? = null,
) {
    val resolvedTint = tint ?: AuriqoWearColors.OnSurface

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container ?: Color.Transparent)
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
            tint = resolvedTint,
            modifier = Modifier.size(iconSize),
        )
        badge?.let {
            Text(
                text = it,
                color = resolvedTint,
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
    var dragProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!isDragging) dragProgress = progress.coerceIn(0f, 1f)
    }

    val track = AuriqoWearColors.OutlineVariant
    val accent = AuriqoWearColors.Accent
    val thumb = AuriqoWearColors.AccentInk

    Box(
        modifier = modifier
            .height(26.dp)
            .pointerInput(durationMs) {
                if (durationMs > 0L) {
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    var pendingProgress = dragProgress
                    fun seekAt(x: Float) {
                        pendingProgress = (x / width).coerceIn(0f, 1f)
                        dragProgress = pendingProgress
                    }
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            seekAt(offset.x)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            seekAt(change.position.x)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((durationMs * pendingProgress).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = progress.coerceIn(0f, 1f)
                        },
                    )
                }
            }
            .semantics {
                contentDescription = "Progreso de la canción"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress.coerceIn(0f, 1f),
                    range = 0f..1f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val y = size.height / 2f
            val strokeWidth = if (isDragging) 5.dp.toPx() else 4.dp.toPx()
            val activeEnd = size.width * dragProgress.coerceIn(0f, 1f)
            drawLine(
                color = track,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accent,
                start = Offset(0f, y),
                end = Offset(activeEnd, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = thumb,
                radius = if (isDragging) 4.5.dp.toPx() else 3.5.dp.toPx(),
                center = Offset(activeEnd.coerceIn(0f, size.width), y),
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
    val spec = rememberWearScreenSpec()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = spec.horizontalPadding,
                end = spec.horizontalPadding,
                top = spec.topPadding,
                bottom = spec.bottomPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NowPlayingTopBar(state = state, onBack = onOpenHome, spec = spec)
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(spec.cardCorner))
                .background(AuriqoWearColors.SurfaceContainer)
                .padding(horizontal = if (spec.compact) 14.dp else 18.dp, vertical = if (spec.compact) 14.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auriqo_wear),
                contentDescription = "Auriqo",
                tint = Color.Unspecified,
                modifier = Modifier.size(if (spec.compact) 40.dp else 47.dp),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Sin sesión",
                color = AuriqoWearColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Reconectar",
                color = AuriqoWearColors.AccentContainerInk,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AuriqoWearColors.AccentContainer)
                    .clickable(role = Role.Button) { PhoneSyncManager.ensureConnected(context) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
                    .semantics {
                        contentDescription = "Reconectar con el teléfono"
                        role = Role.Button
                    },
            )
        }
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
