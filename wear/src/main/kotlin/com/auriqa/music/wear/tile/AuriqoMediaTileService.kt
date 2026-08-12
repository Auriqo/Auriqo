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
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.auriqo.music.wear.media.PhoneSyncManager
import com.auriqo.music.wear.media.NowPlaying
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

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
        PhoneSyncManager.ensureConnected(applicationContext)
        ArtworkFetcher.init(applicationContext)

        handleRequestedAction(requestParams.currentState)
        ensureUpdateLoop()
        fetchArtworkIfNeeded(PhoneSyncManager.nowPlaying.value)

        return Futures.immediateFuture(buildTile(PhoneSyncManager.nowPlaying.value))
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val state = PhoneSyncManager.nowPlaying.value
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
            PhoneSyncManager.nowPlaying
                .drop(1)
                .collect { state ->
                    if (state.artworkUri != artworkUri) {
                        artworkUri = state.artworkUri
                        fetchArtworkIfNeeded(state)
                    }
                    runCatching {
                        TileService.getUpdater(applicationContext)
                            .requestUpdate(AuriqoMediaTileService::class.java)
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
                    TileService.getUpdater(applicationContext)
                        .requestUpdate(AuriqoMediaTileService::class.java)
                }
            }
        }
    }

    private fun handleRequestedAction(state: StateBuilders.State?) {
        val clickedId = state?.lastClickableId ?: return
        when (clickedId) {
            ACTION_PLAY_PAUSE -> PhoneSyncManager.togglePlayPause(applicationContext)
            ACTION_NEXT -> PhoneSyncManager.skipToNext(applicationContext)
            ACTION_PREV -> PhoneSyncManager.skipToPrevious(applicationContext)
            ACTION_LIKE -> PhoneSyncManager.toggleLike(applicationContext)
            ACTION_SHUFFLE -> PhoneSyncManager.toggleShuffle(applicationContext)
            ACTION_REPEAT -> PhoneSyncManager.toggleRepeatMode(applicationContext)
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

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(rootBox))
            .build()
    }

    private fun header(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(textProp("☀ AURIQO"))
            .setFontStyle(fontStyle(sizeSp = 11f, color = COLOR_ACCENT))
            .setMaxLines(int32Prop(1))
            .build()

    private fun nowPlayingSection(state: NowPlaying): LayoutElementBuilders.LayoutElement {
        val row = LayoutElementBuilders.Row.Builder()

        val artworkUrl = state.artworkUri
        if (artworkUrl != null && ArtworkFetcher.getCached(artworkUrl) != null) {
            row.addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(textProp(ARTWORK_RESOURCE_ID))
                    .setWidth(DimensionBuilders.dp(44f))
                    .setHeight(DimensionBuilders.dp(44f))
                    .build(),
            )
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
        }

        val textColumn = LayoutElementBuilders.Column.Builder()
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(textProp(state.title ?: ""))
                .setFontStyle(fontStyle(sizeSp = 13f, color = COLOR_TEXT))
                .setMaxLines(int32Prop(2))
                .build(),
        )
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(textProp(state.artist ?: ""))
                .setFontStyle(fontStyle(sizeSp = 11f, color = COLOR_TEXT_MUTED))
                .setMaxLines(int32Prop(1))
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
            .setText(textProp("Auriqo no conectado"))
            .setFontStyle(fontStyle(sizeSp = 13f, color = COLOR_TEXT_MUTED))
            .setMaxLines(int32Prop(2))
            .build()

    private fun textProp(value: String): TypeBuilders.StringProp =
        TypeBuilders.StringProp.Builder(value).build()

    private fun int32Prop(value: Int): TypeBuilders.Int32Prop =
        TypeBuilders.Int32Prop.Builder().setValue(value).build()

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
                .setText(textProp(glyph))
                .setFontStyle(fontStyle(sizeSp = 13f, color = glyphColor))
                .setMaxLines(int32Prop(1))
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
        copyPixelsToBuffer(ByteBuffer.wrap(bytes))

        val inline =
            ResourceBuilders.InlineImageResource.Builder()
                .setData(bytes)
                .setWidthPx(width)
                .setHeightPx(height)
                .setFormat(ResourceBuilders.IMAGE_FORMAT_ARGB_8888)
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
