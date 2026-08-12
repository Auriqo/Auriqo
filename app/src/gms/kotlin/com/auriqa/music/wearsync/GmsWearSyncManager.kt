package com.auriqo.music.wearsync

import android.net.Uri
import com.auriqo.music.extensions.toggleRepeatMode
import com.auriqo.music.playback.MusicService
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object WearSyncPaths {
    const val NOW_PLAYING = "/auriqa/now_playing"
    const val COMMAND = "/auriqa/command"
}

private const val CMD_PLAY_PAUSE = "play_pause"
private const val CMD_NEXT = "next"
private const val CMD_PREV = "prev"
private const val CMD_LIKE = "like"
private const val CMD_SHUFFLE = "shuffle"
private const val CMD_REPEAT = "repeat"

private data class SyncSnapshot(
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false,
    val canNext: Boolean = false,
    val canPrev: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GmsWearSyncManager(
    private val service: MusicService,
) : WearSyncManager, DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {
    private var started = false
    private val snapshot = MutableStateFlow(SyncSnapshot())

    private val playerListener =
        object : androidx.media3.common.Player.Listener {
            override fun onEvents(
                player: androidx.media3.common.Player,
                events: androidx.media3.common.Player.Events,
            ) {
                updateSnapshot()
            }
        }

    override fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        Wearable.getDataClient(service).addListener(this)
        Wearable.getMessageClient(service).addListener(this)

        scope.launch {
            service.playerFlow.filterNotNull().collect { player ->
                player.addListener(playerListener)
                updateSnapshot()
            }
        }

        scope.launch {
            service.currentMediaMetadata.collect {
                updateSnapshot()
            }
        }

        scope.launch {
            snapshot
                .debounce(300)
                .collect { putNowPlaying(it) }
        }
    }

    override fun stop() {
        if (!started) return
        started = false
        runCatching { Wearable.getDataClient(service).removeListener(this) }
        runCatching { Wearable.getMessageClient(service).removeListener(this) }
        runCatching { service.playerFlow.value?.removeListener(playerListener) }
        runCatching {
            Wearable.getDataClient(service)
                .deleteDataItems(Uri.parse("wear://*/${WearSyncPaths.NOW_PLAYING}"))
        }
    }

    private fun updateSnapshot() {
        val player = service.playerFlow.value ?: return
        val metadata = service.currentMediaMetadata.value
        snapshot.value =
            SyncSnapshot(
                title = metadata?.title,
                artist = metadata?.artists?.firstOrNull()?.name,
                artworkUri = metadata?.thumbnailUrl,
                isPlaying = player.isPlaying,
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                canNext = player.hasNextMediaItem(),
                canPrev = player.hasPreviousMediaItem(),
            )
    }

    private fun putNowPlaying(state: SyncSnapshot) {
        val title = state.title ?: return
        val artist = state.artist ?: ""
        val artworkUri = state.artworkUri ?: ""
        val request =
            PutDataMapRequest.create(WearSyncPaths.NOW_PLAYING).apply {
                dataMap.putString("title", title)
                dataMap.putString("artist", artist)
                dataMap.putString("artwork_uri", artworkUri)
                dataMap.putBoolean("is_playing", state.isPlaying)
                dataMap.putInt("repeat_mode", state.repeatMode)
                dataMap.putBoolean("shuffle", state.shuffleEnabled)
                dataMap.putBoolean("can_next", state.canNext)
                dataMap.putBoolean("can_prev", state.canPrev)
                dataMap.putLong("updated_at", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
        runCatching {
            Wearable.getDataClient(service).putDataItem(request)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearSyncPaths.COMMAND) return
        val player = service.playerFlow.value ?: return
        val command = String(messageEvent.data, Charsets.UTF_8)
        runCatching {
            when (command) {
                CMD_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
                CMD_NEXT -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                CMD_PREV -> if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                CMD_LIKE -> service.toggleLike()
                CMD_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                CMD_REPEAT -> player.toggleRepeatMode()
            }
        }
        updateSnapshot()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.release()
    }
}
