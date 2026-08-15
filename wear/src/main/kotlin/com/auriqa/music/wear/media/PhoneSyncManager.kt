package com.auriqo.music.wear.media

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "AuriqoWear"

object WearSyncPaths {
    const val NOW_PLAYING = "/auriqo/now_playing"
    const val COMMAND = "/auriqo/command"
    const val BROWSE_REQUEST = "/auriqo/browse_request"
    const val BROWSE_STATE = "/auriqo/browse_state"
    const val BROWSE_COMMAND = "/auriqo/browse_command"
    const val LEGACY_NOW_PLAYING = "/auriqa/now_playing"
}

private const val CMD_PLAY_PAUSE = "play_pause"
private const val CMD_NEXT = "next"
private const val CMD_PREV = "prev"
private const val CMD_LIKE = "like"
private const val CMD_SHUFFLE = "shuffle"
private const val CMD_REPEAT = "repeat"
private const val CMD_SEEK_PREFIX = "seek:"
private const val CMD_VOLUME_PREFIX = "volume:"

data class NowPlaying(
    val connected: Boolean = false,
    val error: String? = null,
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canLike: Boolean = false,
    val isLiked: Boolean = false,
    val receivedAtElapsedRealtimeMs: Long = 0L,
    val sourceSession: String? = null,
    val sourceBootCount: Int = -1,
    val sourceSessionStartedElapsedMs: Long = 0L,
    val sourceSequence: Long = 0L,
    val sourceUpdatedAtEpochMs: Long = 0L,
) {
    fun positionAt(elapsedRealtimeMs: Long): Long {
        val elapsed =
            if (isPlaying && receivedAtElapsedRealtimeMs > 0L) {
                ((elapsedRealtimeMs - receivedAtElapsedRealtimeMs).coerceAtLeast(0L) * playbackSpeed).toLong()
            } else {
                0L
            }
        val estimated = (positionMs + elapsed).coerceAtLeast(0L)
        return if (durationMs > 0L) estimated.coerceAtMost(durationMs) else estimated
    }
}

enum class BrowseSection(
    val wireName: String,
    val label: String,
) {
    TRACKS("tracks", "TRACKS"),
    ALBUMS("albums", "ALBUMS"),
    ARTISTS("artists", "ARTISTS"),
    PLAYLISTS("playlists", "PLAYLISTS"),
    QUEUE("queue", "QUEUE"),
    ;

    companion object {
        fun fromWire(value: String?): BrowseSection? =
            entries.firstOrNull { it.wireName == value }
    }
}

data class BrowseItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUri: String?,
    val kind: String,
)

data class BrowseSnapshot(
    val section: BrowseSection? = null,
    val items: List<BrowseItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAt: Long = 0L,
)

object PhoneSyncManager : DataClient.OnDataChangedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val _browse = MutableStateFlow(BrowseSnapshot())
    val browse: StateFlow<BrowseSnapshot> = _browse.asStateFlow()

    private var nodes: List<Node> = emptyList()

    @Volatile
    private var initialized = false

    fun ensureConnected(context: Context) {
        if (initialized) {
            loadExistingState(context.applicationContext)
            refreshNodes(context.applicationContext)
            return
        }
        synchronized(this) {
            if (initialized) return
            initialized = true
        }
        val applicationContext = context.applicationContext
        Wearable.getDataClient(applicationContext).addListener(this)
        loadExistingState(applicationContext)
        refreshNodes(applicationContext)
        startConnectionWatcher(applicationContext)
    }

    private fun loadExistingState(context: Context) {
        Wearable.getDataClient(context).dataItems
            .addOnSuccessListener { items ->
                try {
                    items
                        .mapNotNull(::parseNowPlaying)
                        .reduceOrNull(::newestNowPlaying)
                        ?.let(::applyIncoming)
                } finally {
                    items.release()
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "could not read existing Data Layer state", error)
            }
    }

    private fun refreshNodes(context: Context) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { result ->
            nodes = result
            if (result.isEmpty()) {
                _nowPlaying.value =
                    _nowPlaying.value.copy(
                        connected = false,
                        error = "Teléfono no vinculado",
                    )
            } else if (_nowPlaying.value.error == "Teléfono no vinculado") {
                _nowPlaying.value = _nowPlaying.value.copy(error = null)
            }
        }
    }

    private fun startConnectionWatcher(context: Context) {
        scope.launch {
            while (isActive) {
                delay(15_000L)
                refreshNodes(context)
                val state = _nowPlaying.value
                if (
                    state.isPlaying &&
                    state.receivedAtElapsedRealtimeMs > 0L &&
                    SystemClock.elapsedRealtime() - state.receivedAtElapsedRealtimeMs > 60_000L
                ) {
                    _nowPlaying.value =
                        state.copy(connected = false, error = "Sin datos recientes del teléfono")
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents
                .asSequence()
                .filter { it.type != DataEvent.TYPE_DELETED }
                .forEach { event ->
                    parseNowPlaying(event.dataItem)?.let(::applyIncoming)
                    parseBrowse(event.dataItem)?.let { snapshot ->
                        val current = _browse.value
                        if (snapshot.updatedAt >= current.updatedAt || snapshot.section != current.section) {
                            _browse.value = snapshot
                        }
                    }
                }
        } finally {
            dataEvents.release()
        }
    }

    private fun parseNowPlaying(item: DataItem): NowPlaying? {
        if (item.uri.path !in setOf(WearSyncPaths.NOW_PLAYING, WearSyncPaths.LEGACY_NOW_PLAYING)) return null
        val data = DataMapItem.fromDataItem(item).dataMap
        val title = data.getString("title")?.takeIf(String::isNotBlank) ?: return null
        val receivedAt = SystemClock.elapsedRealtime()
        return NowPlaying(
            connected = true,
            error = null,
            mediaId = data.getString("media_id")?.takeIf(String::isNotBlank),
            title = title,
            artist = data.getString("artist"),
            artworkUri = data.getString("artwork_uri")?.takeIf(String::isNotBlank),
            isPlaying = data.getBoolean("is_playing", false),
            positionMs = data.getLong("position_ms", 0L).coerceAtLeast(0L),
            durationMs = data.getLong("duration_ms", 0L).coerceAtLeast(0L),
            playbackSpeed = data.getFloat("playback_speed", 1f).takeIf { it > 0f } ?: 1f,
            shuffleEnabled = data.getBoolean("shuffle", false),
            repeatMode = data.getInt("repeat_mode", 0),
            canSkipNext = data.getBoolean("can_next", false),
            canSkipPrevious = data.getBoolean("can_prev", false),
            canLike = data.containsKey("liked"),
            isLiked = data.getBoolean("liked", false),
            receivedAtElapsedRealtimeMs = receivedAt,
            sourceSession = data.getString("source_session")?.takeIf(String::isNotBlank),
            sourceBootCount = data.getInt("source_boot_count", -1),
            sourceSessionStartedElapsedMs = data
                .getLong("source_session_started_elapsed_ms", 0L)
                .coerceAtLeast(0L),
            sourceSequence = data.getLong("state_sequence", 0L).coerceAtLeast(0L),
            sourceUpdatedAtEpochMs = data.getLong("updated_at", 0L),
        )
    }

    private fun parseBrowse(item: DataItem): BrowseSnapshot? {
        if (item.uri.path != WearSyncPaths.BROWSE_STATE) return null
        val data = DataMapItem.fromDataItem(item).dataMap
        val section = BrowseSection.fromWire(data.getString("section")) ?: return null
        val ids = data.getStringArrayList("ids") ?: return null
        val titles = data.getStringArrayList("titles") ?: return null
        val subtitles = data.getStringArrayList("subtitles") ?: arrayListOf()
        val artworkUris = data.getStringArrayList("artwork_uris") ?: arrayListOf()
        val kinds = data.getStringArrayList("kinds") ?: arrayListOf()
        val count = minOf(ids.size, titles.size)
        return BrowseSnapshot(
            section = section,
            items = (0 until count).map { index ->
                BrowseItem(
                    id = ids[index],
                    title = titles[index],
                    subtitle = subtitles.getOrNull(index).orEmpty(),
                    artworkUri = artworkUris.getOrNull(index)?.takeIf(String::isNotBlank),
                    kind = kinds.getOrNull(index).orEmpty(),
                )
            },
            loading = false,
            error = null,
            updatedAt = data.getLong("updated_at", 0L),
        )
    }

    private fun applyIncoming(next: NowPlaying) {
        val current = _nowPlaying.value
        if (!shouldApplyIncoming(current, next)) return
        _nowPlaying.value = next
        Log.d(TAG, "now playing state updated; playing=${next.isPlaying}")
    }

    private fun sendCommand(
        context: Context,
        command: String,
        optimistic: (NowPlaying, Long) -> NowPlaying = { state, _ -> state },
    ) {
        val now = SystemClock.elapsedRealtime()
        _nowPlaying.value = optimistic(_nowPlaying.value, now).copy(error = null)
        val applicationContext = context.applicationContext
        val currentNodes = nodes
        if (currentNodes.isEmpty()) {
            Wearable.getNodeClient(applicationContext).connectedNodes.addOnSuccessListener { connected ->
                nodes = connected
                if (connected.isEmpty()) {
                    _nowPlaying.value = _nowPlaying.value.copy(connected = false, error = "Teléfono no vinculado")
                } else {
                    dispatchCommand(applicationContext, connected, command)
                }
            }
            return
        }
        dispatchCommand(applicationContext, currentNodes, command)
    }

    private fun dispatchCommand(context: Context, targets: List<Node>, command: String) {
        val messageClient = Wearable.getMessageClient(context)
        targets.forEach { node ->
            messageClient
                .sendMessage(node.id, WearSyncPaths.COMMAND, command.toByteArray(Charsets.UTF_8))
                .addOnFailureListener { error ->
                    Log.e(TAG, "command failed: $command", error)
                    _nowPlaying.value = _nowPlaying.value.copy(error = "No se pudo enviar el control")
                }
        }
    }

    fun requestBrowse(context: Context, section: BrowseSection) {
        _browse.value = BrowseSnapshot(section = section, loading = true)
        dispatchBrowseMessage(context.applicationContext, WearSyncPaths.BROWSE_REQUEST, section.wireName)
    }

    fun playBrowseItem(context: Context, item: BrowseItem) {
        dispatchBrowseMessage(
            context.applicationContext,
            WearSyncPaths.BROWSE_COMMAND,
            "${item.kind}|${item.id}",
        )
    }

    private fun dispatchBrowseMessage(context: Context, path: String, payload: String) {
        val currentNodes = nodes
        if (currentNodes.isNotEmpty()) {
            sendBrowseMessage(context, currentNodes, path, payload)
            return
        }
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { connected ->
            nodes = connected
            if (connected.isEmpty()) {
                _browse.value = _browse.value.copy(
                    loading = false,
                    error = "Teléfono no vinculado",
                )
            } else {
                sendBrowseMessage(context, connected, path, payload)
            }
        }.addOnFailureListener {
            _browse.value = _browse.value.copy(
                loading = false,
                error = "No se pudo consultar el teléfono",
            )
        }
    }

    private fun sendBrowseMessage(context: Context, targets: List<Node>, path: String, payload: String) {
        val messageClient = Wearable.getMessageClient(context)
        targets.forEach { node ->
            messageClient
                .sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8))
                .addOnFailureListener { error ->
                    Log.e(TAG, "browse message failed: $path", error)
                    _browse.value = _browse.value.copy(
                        loading = false,
                        error = "No se pudo consultar el teléfono",
                    )
                }
        }
    }

    fun togglePlayPause(context: Context) =
        sendCommand(context, CMD_PLAY_PAUSE) { state, now ->
            state.copy(
                isPlaying = !state.isPlaying,
                positionMs = state.positionAt(now),
                receivedAtElapsedRealtimeMs = now,
            )
        }

    fun play(context: Context) {
        if (_nowPlaying.value.isPlaying) return
        togglePlayPause(context)
    }

    fun pause(context: Context) {
        if (!_nowPlaying.value.isPlaying) return
        togglePlayPause(context)
    }

    fun skipToNext(context: Context) = sendCommand(context, CMD_NEXT)

    fun skipToPrevious(context: Context) = sendCommand(context, CMD_PREV)

    fun toggleLike(context: Context) =
        sendCommand(context, CMD_LIKE) { state, _ -> state.copy(isLiked = !state.isLiked) }

    fun toggleShuffle(context: Context) =
        sendCommand(context, CMD_SHUFFLE) { state, _ -> state.copy(shuffleEnabled = !state.shuffleEnabled) }

    fun toggleRepeatMode(context: Context) =
        sendCommand(context, CMD_REPEAT) { state, _ ->
            state.copy(
                repeatMode =
                    when (state.repeatMode) {
                        0 -> 2
                        2 -> 1
                        else -> 0
                    },
            )
        }

    fun setShuffleMode(context: Context, enabled: Boolean) {
        if (_nowPlaying.value.shuffleEnabled != enabled) toggleShuffle(context)
    }

    fun setRepeatMode(context: Context, mode: Int) {
        val target = mode.coerceIn(0, 2)
        var current = _nowPlaying.value.repeatMode
        repeat(3) {
            if (current == target) return
            toggleRepeatMode(context)
            current = when (current) {
                0 -> 2
                2 -> 1
                else -> 0
            }
        }
    }

    fun seekTo(context: Context, positionMs: Long) {
        val position = positionMs.coerceAtLeast(0L)
        sendCommand(context, "$CMD_SEEK_PREFIX$position") { state, now ->
            state.copy(
                positionMs = if (state.durationMs > 0L) position.coerceAtMost(state.durationMs) else position,
                receivedAtElapsedRealtimeMs = now,
            )
        }
    }

    fun adjustVolume(context: Context, direction: Int) {
        val normalizedDirection = direction.coerceIn(-1, 1)
        if (normalizedDirection == 0) return
        sendCommand(context, "$CMD_VOLUME_PREFIX$normalizedDirection")
    }
}

internal fun shouldApplyIncoming(current: NowPlaying, next: NowPlaying): Boolean {
    val nextSession = next.sourceSession
    val currentSession = current.sourceSession
    if (nextSession != null) {
        if (currentSession == null) return true
        if (next.sourceBootCount >= 0 && current.sourceBootCount >= 0) {
            if (next.sourceBootCount != current.sourceBootCount) {
                return next.sourceBootCount > current.sourceBootCount
            }
            if (next.sourceSessionStartedElapsedMs != current.sourceSessionStartedElapsedMs) {
                return next.sourceSessionStartedElapsedMs > current.sourceSessionStartedElapsedMs
            }
        } else if (
            next.sourceSessionStartedElapsedMs > current.sourceSessionStartedElapsedMs &&
            next.sourceSessionStartedElapsedMs > 0L
        ) {
            return true
        }
        if (nextSession != currentSession) {
            return next.sourceUpdatedAtEpochMs > 0L &&
                current.sourceUpdatedAtEpochMs <= next.sourceUpdatedAtEpochMs
        }
        return next.sourceSequence > current.sourceSequence
    }
    if (currentSession != null) return false
    return next.sourceUpdatedAtEpochMs <= 0L ||
        current.sourceUpdatedAtEpochMs <= next.sourceUpdatedAtEpochMs
}

private fun newestNowPlaying(current: NowPlaying, candidate: NowPlaying): NowPlaying =
    if (shouldApplyIncoming(current, candidate)) candidate else current
