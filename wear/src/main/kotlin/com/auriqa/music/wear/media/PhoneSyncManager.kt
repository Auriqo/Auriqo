package com.auriqo.music.wear.media

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "AuriqoWear"

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

data class NowPlaying(
    val connected: Boolean = false,
    val error: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val likeAction: String? = null,
    val updatedAt: Long = 0L,
)

object PhoneSyncManager : DataClient.OnDataChangedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private var nodes: List<Node> = emptyList()
    private var staleJob: kotlinx.coroutines.Job? = null

    @Volatile
    private var initialized = false

    fun ensureConnected(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
        }
        val appContext = context.applicationContext
        Log.i(TAG, "initializing Data Layer sync")
        Wearable.getDataClient(appContext).addListener(this)
        refreshNodes(appContext)
        startStaleWatcher()
    }

    private fun refreshNodes(context: Context) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { result ->
            nodes = result
            Log.i(TAG, "connected nodes: ${result.map { it.displayName }}")
            if (result.isEmpty()) {
                _nowPlaying.value =
                    _nowPlaying.value.copy(
                        connected = false,
                        error = "Sin nodos conectados (¿teléfono vinculado?)",
                    )
            }
        }
    }

    private fun startStaleWatcher() {
        staleJob?.cancel()
        staleJob =
            scope.launch {
                while (isActive) {
                    kotlinx.coroutines.delay(15_000)
                    val state = _nowPlaying.value
                    if (state.updatedAt > 0L &&
                        System.currentTimeMillis() - state.updatedAt > 60_000
                    ) {
                        _nowPlaying.value =
                            state.copy(connected = false, error = "Sin datos recientes del teléfono")
                    }
                }
            }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_DELETED) continue
                if (event.dataItem.uri.path != WearSyncPaths.NOW_PLAYING) continue
                val item = DataMapItem.fromDataItem(event.dataItem).dataMap
                _nowPlaying.value =
                    NowPlaying(
                        connected = true,
                        error = null,
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        artworkUri = item.getString("artwork_uri"),
                        isPlaying = item.getBoolean("is_playing", false),
                        shuffleEnabled = item.getBoolean("shuffle", false),
                        repeatMode = item.getInt("repeat_mode", 0),
                        canSkipNext = item.getBoolean("can_next", false),
                        canSkipPrevious = item.getBoolean("can_prev", false),
                        likeAction = "like",
                        updatedAt = System.currentTimeMillis(),
                    )
                Log.i(
                    TAG,
                    "now playing update: ${_nowPlaying.value.title} playing=${_nowPlaying.value.isPlaying}",
                )
            }
        } finally {
            dataEvents.release()
        }
    }

    private fun sendCommand(context: Context, command: String) {
        val messageClient = Wearable.getMessageClient(context.applicationContext)
        if (nodes.isEmpty()) {
            Log.w(TAG, "no nodes to send command to")
            _nowPlaying.value =
                _nowPlaying.value.copy(error = "Sin nodos conectados")
            refreshNodes(context)
            return
        }
        nodes.forEach { node ->
            messageClient
                .sendMessage(node.id, WearSyncPaths.COMMAND, command.toByteArray(Charsets.UTF_8))
                .addOnSuccessListener {
                    Log.i(TAG, "command sent: $command to ${node.displayName}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "command failed: $command", e)
                }
        }
    }

    fun togglePlayPause(context: Context) = sendCommand(context, CMD_PLAY_PAUSE)

    fun skipToNext(context: Context) = sendCommand(context, CMD_NEXT)

    fun skipToPrevious(context: Context) = sendCommand(context, CMD_PREV)

    fun toggleLike(context: Context) = sendCommand(context, CMD_LIKE)

    fun toggleShuffle(context: Context) = sendCommand(context, CMD_SHUFFLE)

    fun toggleRepeatMode(context: Context) = sendCommand(context, CMD_REPEAT)
}
