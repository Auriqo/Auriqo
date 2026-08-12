package com.auriqo.music.wear.media

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val connected: Boolean = false,
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
)

object MediaBrowserManager {
    private const val PHONE_PACKAGE = "com.auriqo.music"
    private const val PHONE_SERVICE = "com.auriqo.music.playback.MusicService"
    const val ACTION_TOGGLE_LIKE = "TOGGLE_LIKE"
    const val ACTION_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
    const val ACTION_TOGGLE_REPEAT_MODE = "TOGGLE_REPEAT_MODE"

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var connected = false

    @Volatile
    private var initialized = false

    fun ensureConnected(context: Context) {
        if (connected) return
        if (initialized) {
            mediaBrowser?.connect()
            return
        }
        synchronized(this) {
            if (initialized) return
            initialized = true
        }
        val appContext = context.applicationContext
        mediaBrowser =
            MediaBrowserCompat(
                appContext,
                ComponentName(PHONE_PACKAGE, PHONE_SERVICE),
                object : MediaBrowserCompat.ConnectionCallback() {
                    override fun onConnected() {
                        connected = true
                        mediaController = MediaControllerCompat.getMediaController(appContext)
                        mediaController?.registerCallback(
                            controllerCallback,
                        )
                        publishState()
                    }

                    override fun onConnectionSuspended() {
                        connected = false
                        _nowPlaying.value = _nowPlaying.value.copy(connected = false)
                    }

                    override fun onConnectionFailed() {
                        connected = false
                        _nowPlaying.value = _nowPlaying.value.copy(connected = false)
                    }
                },
                null,
            )
        mediaBrowser?.connect()
    }

    private val controllerCallback =
        object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                publishState()
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                publishState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                publishState()
            }

            override fun onShuffleModeChanged(shuffleMode: Int) {
                publishState()
            }
        }

    private fun publishState() {
        val metadata = mediaController?.metadata
        val state = mediaController?.playbackState

        val likeAction =
            state
                ?.customActions
                ?.firstOrNull { it.action == ACTION_TOGGLE_LIKE }
                ?.action

        _nowPlaying.value =
            NowPlaying(
                connected = connected && mediaController != null,
                title = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                artworkUri = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTWORK_URI),
                isPlaying = state?.state == PlaybackStateCompat.STATE_PLAYING,
                positionMs = state?.position ?: 0L,
                durationMs =
                    metadata
                        ?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        ?: 0L,
                shuffleEnabled = mediaController?.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL,
                repeatMode = mediaController?.repeatMode ?: 0,
                canSkipNext = state?.actions?.and(PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L,
                canSkipPrevious = state?.actions?.and(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L,
                likeAction = likeAction,
            )
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (_nowPlaying.value.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipToNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun toggleLike() {
        mediaController
            ?.transportControls
            ?.sendCustomAction(_nowPlaying.value.likeAction ?: ACTION_TOGGLE_LIKE, null)
    }

    fun toggleShuffle() {
        mediaController
            ?.transportControls
            ?.sendCustomAction(ACTION_TOGGLE_SHUFFLE, null)
    }

    fun toggleRepeatMode() {
        mediaController
            ?.transportControls
            ?.sendCustomAction(ACTION_TOGGLE_REPEAT_MODE, null)
    }
}
