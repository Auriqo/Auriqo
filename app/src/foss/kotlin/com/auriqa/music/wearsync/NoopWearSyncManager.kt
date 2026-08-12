package com.auriqa.music.wearsync

class NoopWearSyncManager : WearSyncManager {
    override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit

    override fun stop() = Unit
}
