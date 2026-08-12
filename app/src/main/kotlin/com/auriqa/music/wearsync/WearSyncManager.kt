package com.auriqo.music.wearsync

import kotlinx.coroutines.CoroutineScope

interface WearSyncManager {
    fun start(scope: CoroutineScope)
    fun stop()
}
