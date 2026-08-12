package com.auriqo.music.wearsync

object WearSyncProvider {
    fun create(service: com.auriqo.music.playback.MusicService): WearSyncManager =
        GmsWearSyncManager(service)
}
