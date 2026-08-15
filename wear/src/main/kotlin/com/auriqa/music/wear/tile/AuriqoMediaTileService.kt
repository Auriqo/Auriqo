package com.auriqo.music.wear.tile

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
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
import com.auriqo.music.wear.R
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
private const val LOGO_RESOURCE_ID = "auriqo_logo"
private const val PLAY_RESOURCE_ID = "play"
private const val PAUSE_RESOURCE_ID = "pause"
private const val PREVIOUS_RESOURCE_ID = "previous"
private const val NEXT_RESOURCE_ID = "next"
private const val HEART_RESOURCE_ID = "heart"
private const val SHUFFLE_RESOURCE_ID = "shuffle"
private const val REPEAT_RESOURCE_ID = "repeat"
private const val RESOURCES_VERSION = "4"

private const val ACTION_PLAY_PAUSE = "play_pause"
private const val ACTION_NEXT = "next"
private const val ACTION_PREV = "prev"
private const val ACTION_LIKE = "like"
private const val ACTION_SHUFFLE = "shuffle"
private const val ACTION_REPEAT = "repeat"

private data class TileColors(
    val background: ColorBuilders.ColorProp,
    val accent: ColorBuilders.ColorProp,
    val text: ColorBuilders.ColorProp,
    val mutedText: ColorBuilders.ColorProp,
    val button: ColorBuilders.ColorProp,
)

private fun tileColors(context: Context): TileColors {
    val dark =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun systemOrFallback(resourceId: Int, fallback: Int): ColorBuilders.ColorProp {
        val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(resourceId)
        } else {
            fallback
        }
        return ColorBuilders.argb(color)
    }

    return if (dark) {
        TileColors(
            background = systemOrFallback(android.R.color.system_neutral1_1000, 0xFF050606.toInt()),
            accent = systemOrFallback(android.R.color.system_accent1_200, 0xFFD8F36A.toInt()),
            text = systemOrFallback(android.R.color.system_neutral1_50, 0xFFFFFFFF.toInt()),
            mutedText = systemOrFallback(android.R.color.system_neutral2_200, 0xFFA6ADA7.toInt()),
            button = systemOrFallback(android.R.color.system_neutral1_900, 0xFF151916.toInt()),
        )
    } else {
        TileColors(
            background = systemOrFallback(android.R.color.system_neutral1_10, 0xFFF9F7FF.toInt()),
            accent = systemOrFallback(android.R.color.system_accent1_600, 0xFF4F64A0.toInt()),
            text = systemOrFallback(android.R.color.system_neutral1_900, 0xFF1A1B20.toInt()),
            mutedText = systemOrFallback(android.R.color.system_neutral2_700, 0xFF5E5E66.toInt()),
            button = systemOrFallback(android.R.color.system_neutral1_100, 0xFFE4E2EB.toInt()),
        )
    }
}

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
        val resources =
            ResourceBuilders.Resources.Builder()
                .addAndroidDrawable(LOGO_RESOURCE_ID, R.drawable.ic_auriqo_wear)
                .addAndroidDrawable(PLAY_RESOURCE_ID, R.drawable.ic_play)
                .addAndroidDrawable(PAUSE_RESOURCE_ID, R.drawable.ic_pause)
                .addAndroidDrawable(PREVIOUS_RESOURCE_ID, R.drawable.ic_previous)
                .addAndroidDrawable(NEXT_RESOURCE_ID, R.drawable.ic_next)
                .addAndroidDrawable(HEART_RESOURCE_ID, R.drawable.ic_heart)
                .addAndroidDrawable(SHUFFLE_RESOURCE_ID, R.drawable.ic_shuffle)
                .addAndroidDrawable(REPEAT_RESOURCE_ID, R.drawable.ic_repeat)
        val state = PhoneSyncManager.nowPlaying.value
        val artworkUrl = state.artworkUri
        if (artworkUrl.isNullOrBlank()) {
            return Futures.immediateFuture(resources.build())
        }

        val bitmap = ArtworkFetcher.getCached(artworkUrl)
            ?: return Futures.immediateFuture(resources.build())

        return Futures.immediateFuture(
            resources
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
        val colors = tileColors(applicationContext)
        val column = LayoutElementBuilders.Column.Builder()

        column.addContent(header())
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

        if (state.connected && !state.title.isNullOrBlank()) {
            column.addContent(nowPlayingSection(state, colors))
            column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
            column.addContent(controlsSection(state, colors))
        } else {
            column.addContent(disconnectedSection(colors))
        }

        val rootBox =
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(colors.background)
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
        LayoutElementBuilders.Image.Builder()
            .setResourceId(textProp(LOGO_RESOURCE_ID))
            .setWidth(DimensionBuilders.dp(18f))
            .setHeight(DimensionBuilders.dp(18f))
            .build()

    private fun nowPlayingSection(
        state: NowPlaying,
        colors: TileColors,
    ): LayoutElementBuilders.LayoutElement {
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
                .setFontStyle(fontStyle(sizeSp = 13f, color = colors.text))
                .setMaxLines(int32Prop(2))
                .build(),
        )
        textColumn.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(textProp(state.artist ?: ""))
                .setFontStyle(fontStyle(sizeSp = 11f, color = colors.mutedText))
                .setMaxLines(int32Prop(1))
                .build(),
        )

        row.addContent(textColumn.build())
        return row.build()
    }

    private fun controlsSection(
        state: NowPlaying,
        colors: TileColors,
    ): LayoutElementBuilders.LayoutElement {
        val row = LayoutElementBuilders.Row.Builder()

        if (state.canSkipPrevious) {
            row.addContent(iconButton(PREVIOUS_RESOURCE_ID, ACTION_PREV, colors = colors))
        }
        row.addContent(
            iconButton(
                if (state.isPlaying) PAUSE_RESOURCE_ID else PLAY_RESOURCE_ID,
                ACTION_PLAY_PAUSE,
                primary = true,
                colors = colors,
            ),
        )
        if (state.canSkipNext) {
            row.addContent(iconButton(NEXT_RESOURCE_ID, ACTION_NEXT, colors = colors))
        }

        if (state.canLike) {
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12f)).build())
            row.addContent(iconButton(HEART_RESOURCE_ID, ACTION_LIKE, active = state.isLiked, colors = colors))
        }

        row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12f)).build())
        row.addContent(iconButton(SHUFFLE_RESOURCE_ID, ACTION_SHUFFLE, active = state.shuffleEnabled, colors = colors))
        row.addContent(iconButton(REPEAT_RESOURCE_ID, ACTION_REPEAT, active = state.repeatMode != 0, colors = colors))

        return row.build()
    }

    private fun disconnectedSection(colors: TileColors): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(textProp("Auriqo no conectado"))
            .setFontStyle(fontStyle(sizeSp = 13f, color = colors.mutedText))
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
        imageResourceId: String,
        action: String,
        primary: Boolean = false,
        active: Boolean = false,
        colors: TileColors,
    ): LayoutElementBuilders.LayoutElement {
        val buttonColor = if (primary || active) colors.accent else colors.button
        val glyphColor = if (primary || active) colors.background else colors.text

        val icon =
            LayoutElementBuilders.Image.Builder()
                .setResourceId(textProp(imageResourceId))
                .setWidth(DimensionBuilders.dp(if (primary) 18f else 15f))
                .setHeight(DimensionBuilders.dp(if (primary) 18f else 15f))
                .setColorFilter(
                    LayoutElementBuilders.ColorFilter.Builder()
                        .setTint(glyphColor)
                        .build(),
                )
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
            .addContent(icon)
            .build()
    }

    private fun ResourceBuilders.Resources.Builder.addAndroidDrawable(
        id: String,
        drawableResId: Int,
    ): ResourceBuilders.Resources.Builder =
        addIdToImageMapping(
            id,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(drawableResId)
                        .build(),
                )
                .build(),
        )

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
