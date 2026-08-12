package com.auriqo.music.wear.tile

import android.graphics.Bitmap
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.StateBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.SuspendingTileService
import androidx.wear.tiles.update.TileUpdateRequester
import com.auriqa.music.wear.media.MediaBrowserManager
import com.auriqa.music.wear.media.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

private const val ACTION_KEY = "action"
private const val ARTWORK_RESOURCE_ID = "artwork"
private const val RESOURCES_VERSION = "1"

private const val ACTION_PLAY_PAUSE = "play_pause"
private const val ACTION_NEXT = "next"
private const val ACTION_PREV = "prev"
private const val ACTION_LIKE = "like"
private const val ACTION_SHUFFLE = "shuffle"
private const val ACTION_REPEAT = "repeat"

private val COLOR_BACKGROUND = ColorBuilders.argb(0xFF0F0F0F)
private val COLOR_ACCENT = ColorBuilders.argb(0xFFFFB20F)
private val COLOR_TEXT = ColorBuilders.argb(0xFFFFFFFF)
private val COLOR_TEXT_MUTED = ColorBuilders.argb(0xFFB0AFA8)
private val COLOR_BUTTON = ColorBuilders.argb(0xFF2A2A28)

class AuriqoMediaTileService : SuspendingTileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var updateLoopStarted = false
    private var artworkUri: String? = null

    private val tileUpdateRequester by lazy { TileUpdateRequester.create(this) }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        MediaBrowserManager.ensureConnected(applicationContext)
        ArtworkFetcher.init(applicationContext)

        handleRequestedAction(requestParams.currentState)
        ensureUpdateLoop()
        maybeFetchArtwork(MediaBrowserManager.nowPlaying.value)

        return buildTile(MediaBrowserManager.nowPlaying.value)
    }

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ResourceBuilders.Resources {
        val state = MediaBrowserManager.nowPlaying.value
        val artworkUrl = state.artworkUri
        if (artworkUrl.isNullOrBlank()) return ResourceBuilders.Resources.Builder().build()

        val bitmap = ArtworkFetcher.getCached(artworkUrl) ?: return ResourceBuilders.Resources.Builder().build()

        return ResourceBuilders.Resources.Builder()
            .addIdToImageMapping(ARTWORK_RESOURCE_ID, bitmap.toImageResource())
            .build()
    }

    private fun ensureUpdateLoop() {
        if (updateLoopStarted) return
        updateLoopStarted = true

        serviceScope.launch {
            MediaBrowserManager.nowPlaying
                .distinctUntilChanged()
                .drop(1)
                .collect { state ->
                    val newArtwork = state.artworkUri
                    if (newArtwork != artworkUri) {
                        artworkUri = newArtwork
                        launch(Dispatchers.IO) { maybeFetchArtwork(state) }
                    }
                    runCatching {
                        tileUpdateRequester.requestUpdate(AuriqoMediaTileService::class.java)
                    }
                }
        }
    }

    private suspend fun maybeFetchArtwork(state: NowPlaying) {
        val url = state.artworkUri ?: return
        if (artworkUri == url && ArtworkFetcher.getCached(url) != null) return
        artworkUri = url

        val wasCached = ArtworkFetcher.getCached(url) != null
        if (wasCached) return

        kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (ArtworkFetcher.fetch(url) != null) {
                runCatching {
                    tileUpdateRequester.requestUpdate(AuriqoMediaTileService::class.java)
                }
            }
        }
    }

    private fun handleRequestedAction(state: StateBuilders.State?) {
        val action = state?.stringVal?.get(ACTION_KEY) ?: return
        when (action) {
            ACTION_PLAY_PAUSE -> MediaBrowserManager.togglePlayPause()
            ACTION_NEXT -> MediaBrowserManager.skipToNext()
            ACTION_PREV -> MediaBrowserManager.skipToPrevious()
            ACTION_LIKE -> MediaBrowserManager.toggleLike()
            ACTION_SHUFFLE -> MediaBrowserManager.toggleShuffle()
            ACTION_REPEAT -> MediaBrowserManager.toggleRepeatMode()
        }
    }

    private fun buildTile(state: NowPlaying): TileBuilders.Tile {
        val column = LayoutElementBuilders.Column.Builder()

        column.addContent(header())
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4)).build())

        if (state.connected && !state.title.isNullOrBlank()) {
            column.addContent(nowPlayingSection(state))
            column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4)).build())
            column.addContent(controlsSection(state))
        } else {
            column.addContent(disconnectedSection())
        }

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TileBuilders.Timeline.fromLayoutElement(
                    LayoutElementBuilders.Box.Builder()
                        .setBackgroundColor(COLOR_BACKGROUND)
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .addContent(column.build())
                        .build(),
                ),
            )
            .build()
    }

    private fun header(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText("☀ AURIQO")
            .setFontStyle(
                androidx.wear.tiles.FontStylesBuilder.Builder()
                    .setSize(DimensionBuilders.sp(11))
                    .setColor(COLOR_ACCENT)
                    .build(),
            )
            .setMaxLines(1)
            .build()

    private fun nowPlayingSection(state: NowPlaying): LayoutElementBuilders.LayoutElement {
        val row = LayoutElementBuilders.Row.Builder()

        if (state.artworkUri != null && ArtworkFetcher.getCached(state.artworkUri) != null) {
            row.addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(ARTWORK_RESOURCE_ID)
                    .setWidth(DimensionBuilders.dp(44))
                    .setHeight(DimensionBuilders.dp(44))
                    .build(),
            )
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8)).build())
        }

        val textColumn = LayoutElementBuilders.Column.Builder()
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(state.title ?: "")
                .setFontStyle(
                    androidx.wear.tiles.FontStylesBuilder.Builder()
                        .setSize(DimensionBuilders.sp(13))
                        .setColor(COLOR_TEXT)
                        .build(),
                )
                .setMaxLines(2)
                .build(),
        )
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(state.artist ?: "")
                .setFontStyle(
                    androidx.wear.tiles.FontStylesBuilder.Builder()
                        .setSize(DimensionBuilders.sp(11))
                        .setColor(COLOR_TEXT_MUTED)
                        .build(),
                )
                .setMaxLines(1)
                .build(),
        )

        row.addContent(textColumn.build())
        return row.build()
    }

    private fun controlsSection(state: NowPlaying): LayoutElementBuilders.LayoutElement {
        val row = LayoutElementBuilders.Row.Builder()

        if (state.canSkipPrevious) {
            row.addContent(iconButton("⏮", ACTION_PREV))
        }
        row.addContent(iconButton(if (state.isPlaying) "⏸" else "▶", ACTION_PLAY_PAUSE, primary = true))
        if (state.canSkipNext) {
            row.addContent(iconButton("⏭", ACTION_NEXT))
        }

        if (state.likeAction != null) {
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12)).build())
            row.addContent(iconButton("♡", ACTION_LIKE))
        }

        row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12)).build())
        row.addContent(iconButton("⇄", ACTION_SHUFFLE, active = state.shuffleEnabled))
        row.addContent(iconButton("↻", ACTION_REPEAT, active = state.repeatMode != 0))

        return row.build()
    }

    private fun disconnectedSection(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText("Auriqo no conectado")
            .setFontStyle(
                androidx.wear.tiles.FontStylesBuilder.Builder()
                    .setSize(DimensionBuilders.sp(13))
                    .setColor(COLOR_TEXT_MUTED)
                    .build(),
            )
            .setMaxLines(2)
            .build()

    private fun iconButton(
        glyph: String,
        action: String,
        primary: Boolean = false,
        active: Boolean = false,
    ): LayoutElementBuilders.LayoutElement {
        val buttonColor = if (primary || active) COLOR_ACCENT else COLOR_BUTTON
        val glyphColor = if (primary || active) COLOR_BACKGROUND else COLOR_TEXT

        val loadAction =
            ActionBuilders.LoadAction.Builder()
                .setRequestState(
                    StateBuilders.State.Builder()
                        .addStringVal(ACTION_KEY, action)
                        .build(),
                )
                .build()

        val text =
            LayoutElementBuilders.Text.Builder()
                .setText(glyph)
                .setFontStyle(
                    androidx.wear.tiles.FontStylesBuilder.Builder()
                        .setSize(DimensionBuilders.sp(13))
                        .setColor(glyphColor)
                        .build(),
                )
                .setMaxLines(1)
                .build()

        return LayoutElementBuilders.Box.Builder()
            .setBackgroundColor(buttonColor)
            .setWidth(DimensionBuilders.dp(if (primary) 40 else 28))
            .setHeight(DimensionBuilders.dp(28))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setOnClick(loadAction)
                            .build(),
                    )
                    .build(),
            )
            .addContent(text)
            .build()
    }

    private fun Bitmap.toImageResource(): ResourceBuilders.ImageResource {
        val bytes = ByteArray(byteCount)
        copyPixelsToBuffer(ByteBuffer.wrap(bytes))

        return ResourceBuilders.ImageResource.Builder()
            .setInlineResource(
                ResourceBuilders.InlineImageResource.Builder()
                    .setData(bytes)
                    .setWidthPx(width)
                    .setHeightPx(height)
                    .setFormat(ResourceBuilders.IMAGE_FORMAT_ARGB_8888)
                    .build(),
            )
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        updateLoopStarted = false
        super.onDestroy()
    }
}
