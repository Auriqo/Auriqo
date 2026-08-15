package com.auriqo.music.wearsync

import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.media3.common.C
import androidx.media3.common.Player
import com.auriqo.music.extensions.toggleRepeatMode
import com.auriqo.music.playback.MusicService
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object WearSyncPaths {
    const val NOW_PLAYING = "/auriqo/now_playing"
    const val COMMAND = "/auriqo/command"

    // Read/accept the pre-rebrand paths for one compatibility cycle.
    const val LEGACY_NOW_PLAYING = "/auriqa/now_playing"
    const val LEGACY_COMMAND = "/auriqa/command"
}

private const val CMD_PLAY_PAUSE = "play_pause"
private const val CMD_NEXT = "next"
private const val CMD_PREV = "prev"
private const val CMD_LIKE = "like"
private const val CMD_SHUFFLE = "shuffle"
private const val CMD_REPEAT = "repeat"
private const val CMD_SEEK_PREFIX = "seek:"

private data class SyncSnapshot(
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val liked: Boolean = false,
    val canNext: Boolean = false,
    val canPrev: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GmsWearSyncManager(
    private val service: MusicService,
) : WearSyncManager, MessageClient.OnMessageReceivedListener {
    private var started = false
    private var syncJob: Job? = null
    private var observedPlayer: Player? = null
    private val snapshot = MutableStateFlow(SyncSnapshot())
    private val sourceSession = UUID.randomUUID().toString()
    private val sourceBootCount = runCatching {
        Settings.Global.getInt(service.contentResolver, Settings.Global.BOOT_COUNT, -1)
    }.getOrDefault(-1)
    private val sourceSessionStartedElapsedMs = SystemClock.elapsedRealtime()
    private val sourceSequence = AtomicLong(0L)

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateSnapshot()
            }
        }

    override fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        Wearable.getMessageClient(service).addListener(this)

        syncJob =
            scope.launch {
                launch {
                    service.playerFlow.filterNotNull().collect { player ->
                        observedPlayer?.removeListener(playerListener)
                        observedPlayer = player
                        player.addListener(playerListener)
                        updateSnapshot()
                    }
                }
                launch { service.currentMediaMetadata.collect { updateSnapshot() } }
                launch { service.currentSongLiked.collect { updateSnapshot() } }
                launch {
                    snapshot
                        .debounce(150)
                        .collect { state ->
                            if (state.title.isNullOrBlank()) clearNowPlaying() else putNowPlaying(state)
                        }
                }
                launch {
                    while (isActive) {
                        delay(if (observedPlayer?.isPlaying == true) 15_000L else 45_000L)
                        updateSnapshot()
                        snapshot.value.let { state ->
                            if (state.title.isNullOrBlank()) clearNowPlaying() else putNowPlaying(state)
                        }
                    }
                }
            }
    }

    override fun stop() {
        if (!started) return
        started = false
        syncJob?.cancel()
        syncJob = null
        observedPlayer?.removeListener(playerListener)
        observedPlayer = null
        runCatching { Wearable.getMessageClient(service).removeListener(this) }
        clearNowPlaying()
    }

    private fun updateSnapshot() {
        val player = service.playerFlow.value
        val metadata = service.currentMediaMetadata.value
        if (player == null || metadata == null) {
            snapshot.value = SyncSnapshot()
            return
        }

        val durationMs =
            player.duration.takeIf { it > 0L && it != C.TIME_UNSET }
                ?: metadata.duration.takeIf { it > 0 }?.toLong()?.times(1_000L)
                ?: 0L
        snapshot.value =
            SyncSnapshot(
                mediaId = metadata.id,
                title = metadata.title,
                artist = metadata.artists.joinToString { it.name },
                artworkUri = metadata.thumbnailUrl,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L).let { position ->
                    if (durationMs > 0L) position.coerceAtMost(durationMs) else position
                },
                durationMs = durationMs,
                playbackSpeed = player.playbackParameters.speed,
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                liked = service.currentSongLiked.value,
                canNext = player.hasNextMediaItem(),
                canPrev = player.hasPreviousMediaItem(),
            )
    }

    private fun putNowPlaying(state: SyncSnapshot) {
        val sequence = sourceSequence.incrementAndGet()
        val updatedAt = System.currentTimeMillis()
        for (path in listOf(WearSyncPaths.NOW_PLAYING, WearSyncPaths.LEGACY_NOW_PLAYING)) {
            val request =
                PutDataMapRequest.create(path).apply {
                    dataMap.putInt("protocol_version", 2)
                    dataMap.putString("source_session", sourceSession)
                    dataMap.putInt("source_boot_count", sourceBootCount)
                    dataMap.putLong("source_session_started_elapsed_ms", sourceSessionStartedElapsedMs)
                    dataMap.putLong("state_sequence", sequence)
                    dataMap.putString("media_id", state.mediaId.orEmpty())
                    dataMap.putString("title", state.title.orEmpty())
                    dataMap.putString("artist", state.artist.orEmpty())
                    dataMap.putString("artwork_uri", state.artworkUri.orEmpty())
                    dataMap.putBoolean("is_playing", state.isPlaying)
                    dataMap.putLong("position_ms", state.positionMs)
                    dataMap.putLong("duration_ms", state.durationMs)
                    dataMap.putFloat("playback_speed", state.playbackSpeed)
                    dataMap.putInt("repeat_mode", state.repeatMode)
                    dataMap.putBoolean("shuffle", state.shuffleEnabled)
                    dataMap.putBoolean("liked", state.liked)
                    dataMap.putBoolean("can_next", state.canNext)
                    dataMap.putBoolean("can_prev", state.canPrev)
                    dataMap.putLong("updated_at", updatedAt)
                }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(service).putDataItem(request)
        }
    }

    private fun clearNowPlaying() {
        for (path in listOf(WearSyncPaths.NOW_PLAYING, WearSyncPaths.LEGACY_NOW_PLAYING)) {
            runCatching {
                Wearable.getDataClient(service).deleteDataItems(Uri.parse("wear://*$path"))
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path !in setOf(WearSyncPaths.COMMAND, WearSyncPaths.LEGACY_COMMAND)) return
        if (messageEvent.data.size > 64) return
        val player = service.playerFlow.value ?: return
        val command = String(messageEvent.data, Charsets.UTF_8)
        runCatching {
            when {
                command == CMD_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
                command == CMD_NEXT -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                command == CMD_PREV -> if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                command == CMD_LIKE -> service.toggleLike()
                command == CMD_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                command == CMD_REPEAT -> player.toggleRepeatMode()
                command.startsWith(CMD_SEEK_PREFIX) ->
                    command.removePrefix(CMD_SEEK_PREFIX).toLongOrNull()?.let { position ->
                        player.seekTo(position.coerceAtLeast(0L))
                    }
                else -> return
            }
        }
        updateSnapshot()
    }
}
