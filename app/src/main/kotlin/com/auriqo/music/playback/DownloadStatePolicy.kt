package com.auriqo.music.playback

import androidx.media3.exoplayer.offline.Download

/** Maps Media3 terminal download states to the persisted offline flag. */
internal object DownloadStatePolicy {
    fun downloadedValueFor(state: Int): Boolean? = when (state) {
        Download.STATE_COMPLETED -> true
        Download.STATE_FAILED,
        Download.STATE_STOPPED,
        Download.STATE_REMOVING -> false
        else -> null
    }
}
