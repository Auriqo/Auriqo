package com.auriqo.music.wear.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.media.MediaBrowserService
import com.auriqo.music.wear.R
import com.auriqo.music.wear.tile.ArtworkFetcher
import com.auriqo.music.wear.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A small local MediaSession proxy for the phone player.
 *
 * Wear OS only exposes the system media surface for a session that exists on the
 * watch. The actual player remains on the phone; commands cross the Data Layer.
 */
@Suppress("DEPRECATION")
class AuriqoMediaSessionService : MediaBrowserService() {
    companion object {
        const val ACTION_SYNC = "com.auriqo.music.wear.action.SYNC"
        const val ACTION_PLAY_PAUSE = "com.auriqo.music.wear.action.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.auriqo.music.wear.action.PREVIOUS"
        const val ACTION_NEXT = "com.auriqo.music.wear.action.NEXT"
        const val ACTION_STOP = "com.auriqo.music.wear.action.STOP"
        private const val ACTION_SHUFFLE = "com.auriqo.music.wear.action.SHUFFLE"
        private const val ACTION_REPEAT = "com.auriqo.music.wear.action.REPEAT"
        private const val ACTION_LIKE = "com.auriqo.music.wear.action.LIKE"

        private const val CHANNEL_ID = "auriqo_wear_media"
        private const val NOTIFICATION_ID = 7341
        private const val REQUEST_PLAY_PAUSE = 1
        private const val REQUEST_PREVIOUS = 2
        private const val REQUEST_NEXT = 3

        fun start(context: android.content.Context) {
            val intent = Intent(context, AuriqoMediaSessionService::class.java)
                .setAction(ACTION_SYNC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager
    private var foregroundStarted = false
    private var artworkUrl: String? = null

    private val callback = object : MediaSession.Callback() {
        override fun onPlay() = PhoneSyncManager.play(this@AuriqoMediaSessionService)

        override fun onPause() = PhoneSyncManager.pause(this@AuriqoMediaSessionService)

        override fun onSkipToNext() = PhoneSyncManager.skipToNext(this@AuriqoMediaSessionService)

        override fun onSkipToPrevious() = PhoneSyncManager.skipToPrevious(this@AuriqoMediaSessionService)

        override fun onSeekTo(pos: Long) = PhoneSyncManager.seekTo(this@AuriqoMediaSessionService, pos)

        override fun onCustomAction(action: String, extras: Bundle?) {
            when (action) {
                ACTION_SHUFFLE -> PhoneSyncManager.toggleShuffle(this@AuriqoMediaSessionService)
                ACTION_REPEAT -> PhoneSyncManager.toggleRepeatMode(this@AuriqoMediaSessionService)
                ACTION_LIKE -> PhoneSyncManager.toggleLike(this@AuriqoMediaSessionService)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        mediaSession = MediaSession(this, "Auriqo Wear")
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        mediaSession.setCallback(callback)
        mediaSession.setSessionActivity(
            PendingIntent.getActivity(
                this,
                11,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        mediaSession.isActive = true
        sessionToken = mediaSession.sessionToken

        ArtworkFetcher.init(this)
        PhoneSyncManager.ensureConnected(this)
        serviceScope.launch {
            PhoneSyncManager.nowPlaying.collect(::publishState)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PhoneSyncManager.togglePlayPause(this)
            ACTION_PREVIOUS -> PhoneSyncManager.skipToPrevious(this)
            ACTION_NEXT -> PhoneSyncManager.skipToNext(this)
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot("auriqo", null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<android.media.browse.MediaBrowser.MediaItem>>,
    ) {
        result.sendResult(mutableListOf())
    }

    private fun publishState(state: NowPlaying) {
        val title = state.title?.takeIf(String::isNotBlank)
        if (title == null) {
            mediaSession.isActive = false
            if (foregroundStarted) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                foregroundStarted = false
            }
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        mediaSession.isActive = true
        val metadata = buildMetadata(state)
        mediaSession.setMetadata(metadata)
        val nextArtworkUrl = state.artworkUri?.takeIf(String::isNotBlank)
        if (nextArtworkUrl != artworkUrl) {
            artworkUrl = nextArtworkUrl
            nextArtworkUrl?.let { url ->
                serviceScope.launch(Dispatchers.IO) {
                    val bitmap = ArtworkFetcher.fetch(url)
                    if (bitmap != null) {
                        withContext(Dispatchers.Main.immediate) {
                            if (artworkUrl == url && mediaSession.isActive) {
                                val latest = PhoneSyncManager.nowPlaying.value
                                if (latest.artworkUri == url) {
                                    mediaSession.setMetadata(buildMetadata(latest, bitmap))
                                }
                            }
                        }
                    }
                }
            }
        }

        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SEEK_TO
        val customActions = listOf(
            PlaybackState.CustomAction.Builder(
                ACTION_LIKE,
                if (state.isLiked) "Quitar Me gusta" else "Me gusta",
                R.drawable.ic_heart,
            ).build(),
            PlaybackState.CustomAction.Builder(
                ACTION_SHUFFLE,
                if (state.shuffleEnabled) "Desactivar aleatorio" else "Activar aleatorio",
                R.drawable.ic_shuffle,
            ).build(),
            PlaybackState.CustomAction.Builder(
                ACTION_REPEAT,
                if (state.repeatMode == 0) "Repetir" else "Cambiar repetición",
                R.drawable.ic_repeat,
            ).build(),
        )
        val position = state.positionAt(SystemClock.elapsedRealtime())
        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                position,
                state.playbackSpeed,
            )
            .setBufferedPosition(position)
        customActions.forEach(playbackState::addCustomAction)
        mediaSession.setPlaybackState(playbackState.build())

        val notification = buildNotification(state)
        if (!foregroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildMetadata(state: NowPlaying, artwork: Bitmap? = null): MediaMetadata =
        MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, state.title.orEmpty())
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, state.title.orEmpty())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, state.artist.orEmpty())
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, state.artist.orEmpty())
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs.takeIf { it > 0L } ?: 0L)
            .apply {
                state.artworkUri?.takeIf(String::isNotBlank)?.let { uri ->
                    putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, uri)
                }
                artwork?.let { bitmap -> putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap) }
            }
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Auriqo",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Controles de reproducción de Auriqo"
                },
            )
        }
    }

    private fun buildNotification(state: NowPlaying): Notification {
        val title = state.title.orEmpty()
        val artist = state.artist.orEmpty()
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_auriqo_wear)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    10,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .addAction(action(R.drawable.ic_previous, "Anterior", ACTION_PREVIOUS, REQUEST_PREVIOUS))
            .addAction(
                action(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (state.isPlaying) "Pausar" else "Reproducir",
                    ACTION_PLAY_PAUSE,
                    REQUEST_PLAY_PAUSE,
                ),
            )
            .addAction(action(R.drawable.ic_next, "Siguiente", ACTION_NEXT, REQUEST_NEXT))
            .build()
    }

    private fun action(
        iconRes: Int,
        title: String,
        action: String,
        requestCode: Int,
    ): Notification.Action =
        Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, iconRes),
            title,
            PendingIntent.getService(
                this,
                requestCode,
                Intent(this, AuriqoMediaSessionService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

    override fun onDestroy() {
        serviceScope.cancel()
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
