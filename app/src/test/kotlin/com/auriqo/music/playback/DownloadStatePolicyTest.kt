package com.auriqo.music.playback

import androidx.media3.exoplayer.offline.Download
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStatePolicyTest {
    @Test
    fun `terminal states persist the correct download flag`() {
        assertEquals(true, DownloadStatePolicy.downloadedValueFor(Download.STATE_COMPLETED))
        assertEquals(false, DownloadStatePolicy.downloadedValueFor(Download.STATE_FAILED))
        assertEquals(false, DownloadStatePolicy.downloadedValueFor(Download.STATE_STOPPED))
        assertEquals(false, DownloadStatePolicy.downloadedValueFor(Download.STATE_REMOVING))
    }

    @Test
    fun `active queue states do not overwrite persisted storage state`() {
        assertNull(DownloadStatePolicy.downloadedValueFor(Download.STATE_QUEUED))
        assertNull(DownloadStatePolicy.downloadedValueFor(Download.STATE_DOWNLOADING))
    }
}
