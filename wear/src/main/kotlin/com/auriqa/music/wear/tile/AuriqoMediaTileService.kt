package com.auriqo.music.wear.tile

import android.graphics.Bitmap
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.proto.ResourceProto
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.auriqo.music.wear.media.MediaBrowserManager
import com.auriqo.music.wear.media.NowPlaying
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val ARTWORK_RESOURCE_ID = "artwork"
private const val RESOURCES_VERSION = "1"

private const val ACTION_PLAY_PAUSE = "play_pause"
private const val ACTION_NEXT = "next"
private const val ACTION_PREV = "prev"
private const val ACTION_LIKE = "like"
private const val ACTION_SHUFFLE = "shuffle"
private const val ACTION_REPEAT = "repeat"

private val COLOR_BACKGROUND = ColorBuilders.argb(0xFF0F0F0F.toInt())
private val COLOR_ACCENT = ColorBuilders.argb(0xFFFFB20F.toInt())
private val COLOR_TEXT = ColorBuilders.argb(0xFFFFFFFF.toInt())
private val COLOR_TEXT_MUTED = ColorBuilders.argb(0xFFB0AFA8.toInt())
private val COLOR_BUTTON = ColorBuilders.argb(0xFF2A2A28.toInt())

class AuriqoMediaTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var updateLoopStarted = false
    private var artworkUri: String? = null

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        MediaBrowserManager.ensureConnected(applicationContext)
        ArtworkFetcher.init(applicationContext)

        handleRequestedAction(requestParams.currentState)
        ensureUpdateLoop()
        fetchArtworkIfNeeded(MediaBrowserManager.nowPlaying.value)

        return Futures.immediateFuture(buildTile(MediaBrowserManager.nowPlaying.value))
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val state = MediaBrowserManager.nowPlaying.value
        val artworkUrl = state.artworkUri
        if (artworkUrl.isNullOrBlank()) {
            return Futures.immediateFuture(ResourceBuilders.Resources.Builder().build())
        }

        val bitmap = ArtworkFetcher.getCached(artworkUrl)
            ?: return Futures.immediateFuture(ResourceBuilders.Resources.Builder().build())

        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .addIdToImageMapping(ARTWORK_RESOURCE_ID, bitmap.toImageResource())
                .build(),
        )
    }

    private fun ensureUpdateLoop() {
        if (updateLoopStarted) return
        updateLoopStarted = true

        serviceScope.launch {
            MediaBrowserManager.nowPlaying
                .drop(1)
                .collect { state ->
                    if (state.artworkUri != artworkUri) {
                        artworkUri = state.artworkUri
                        fetchArtworkIfNeeded(state)
                    }
                    runCatching {
                        TileService.getUpdater(applicationContext).requestUpdate(AuriqoMediaTileService::class.java)
                    }
                }
        }
    }

    private fun fetchArtworkIfNeeded(state: NowPlaying) {
        val url = state.artworkUri ?: return
        if (artworkUri == url && ArtworkFetcher.getCached(url) != null) return
        artworkUri = url
        if (ArtworkFetcher.getCached(url) != null) return

        serviceScope.launch(Dispatchers.IO) {
            if (ArtworkFetcher.fetch(url) != null) {
                runCatching {
                    TileService.getUpdater(applicationContext).requestUpdate(AuriqoMediaTileService::class.java)
                }
            }
        }
    }

    private fun handleRequestedAction(state: StateBuilders.State?) {
        val clickedId = state?.lastClickableId ?: return
        when (clickedId) {
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
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

        if (state.connected && !state.title.isNullOrBlank()) {
            column.addContent(nowPlayingSection(state))
            column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
            column.addContent(controlsSection(state))
        } else {
            column.addContent(disconnectedSection())
        }

        val rootBox =
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(COLOR_BACKGROUND)
                                .build(),
                        )
                        .build(),
                )
                .addContent(column.build())
                .build()

        val timelineEntry =
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(rootBox)
                .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(timelineEntry)
                    .build(),
            )
            .build()
    }

    private fun header(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText("☀ AURIQO")
            .setFontStyle(fontStyle(sizeSp = 11f, color = COLOR_ACCENT))
            .setMaxLines(1)
            .build()

    private fun nowPlayingSection(state: NowPlaying): LayoutElementBuilders.LayoutElement {
        val row = LayoutElementBuilders.Row.Builder()

        val artworkUrl = state.artworkUri
        if (artworkUrl != null && ArtworkFetcher.getCached(artworkUrl) != null) {
            row.addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(ARTWORK_RESOURCE_ID)
                    .setWidth(DimensionBuilders.dp(44f))
                    .setHeight(DimensionBuilders.dp(44f))
                    .build(),
            )
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
        }

        val textColumn = LayoutElementBuilders.Column.Builder()
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(state.title ?: "")
                .setFontStyle(fontStyle(sizeSp = 13f, color = COLOR_TEXT))
                .setMaxLines(2)
                .build(),
        )
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(state.artist ?: "")
                .setFontStyle(fontStyle(sizeSp = 11f, color = COLOR_TEXT_MUTED))
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
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12f)).build())
            row.addContent(iconButton("♡", ACTION_LIKE))
        }

        row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12f)).build())
        row.addContent(iconButton("⇄", ACTION_SHUFFLE, active = state.shuffleEnabled))
        row.addContent(iconButton("↻", ACTION_REPEAT, active = state.repeatMode != 0))

        return row.build()
    }

    private fun disconnectedSection(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText("Auriqo no conectado")
            .setFontStyle(fontStyle(sizeSp = 13f, color = COLOR_TEXT_MUTED))
            .setMaxLines(2)
            .build()

    private fun fontStyle(
        sizeSp: Float,
        color: ColorBuilders.ColorProp,
    ): LayoutElementBuilders.FontStyle =
        LayoutElementBuilders.FontStyle.Builder()
            .setSize(DimensionBuilders.sp(sizeSp))
            .setColor(color)
            .build()

    private fun iconButton(
        glyph: String,
        action: String,
        primary: Boolean = false,
        active: Boolean = false,
    ): LayoutElementBuilders.LayoutElement {
        val buttonColor = if (primary || active) COLOR_ACCENT else COLOR_BUTTON
        val glyphColor = if (primary || active) COLOR_BACKGROUND else COLOR_TEXT

        val text =
            LayoutElementBuilders.Text.Builder()
                .setText(glyph)
                .setFontStyle(fontStyle(sizeSp = 13f, color = glyphColor))
                .setMaxLines(1)
                .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(if (primary) 40f else 28f))
            .setHeight(DimensionBuilders.dp(28f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(buttonColor)
                            .build(),
                    )
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId(action)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .build(),
            )
            .addContent(text)
            .build()
    }

    private fun Bitmap.toImageResource(): ResourceBuilders.ImageResource {
        val bytes = ByteArray(byteCount)
        copyPixelsToBuffer(java.nio.ByteBuffer.wrap(bytes))

        val inline =
            ResourceBuilders.InlineImageResource.Builder()
                .setData(bytes)
                .setWidthPx(width)
                .setHeightPx(height)
                .setFormat(ResourceProto.ImageFormat.IMAGE_FORMAT_ARGB_8888)
                .build()

        return ResourceBuilders.ImageResource.Builder()
            .setInlineResource(inline)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        updateLoopStarted = false
        super.onDestroy()
    }
}
