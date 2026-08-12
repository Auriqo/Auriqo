package com.auriqo.music.wear

import android.app.Application
import com.auriqo.music.wear.media.PhoneSyncManager
import com.auriqo.music.wear.tile.ArtworkFetcher

class AuriqoWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ArtworkFetcher.init(this)
        PhoneSyncManager.ensureConnected(this)
    }
}
