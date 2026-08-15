package com.auriqo.music.wear.media

import android.content.Intent
import android.os.Build
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives phone playback updates even when the Wear Activity has never been opened.
 * The system media surface needs a live local session; starting it from this callback
 * is what makes Auriqo behave like a real Wear media app instead of an on-demand remote.
 */
class AuriqoDataLayerListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        PhoneSyncManager.onDataChanged(dataEvents)

        if (PhoneSyncManager.nowPlaying.value.title.isNullOrBlank()) return

        val intent = Intent(this, AuriqoMediaSessionService::class.java)
            .setAction(AuriqoMediaSessionService.ACTION_SYNC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
