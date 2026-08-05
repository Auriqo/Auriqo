package com.auriqo.music.utils

import android.os.SystemClock
import com.auriqo.music.BuildConfig
import timber.log.Timber

/**
 * Lightweight cold-start measurement points visible in Android Studio's Logcat.
 *
 * They only log in debug builds, so release startup has no logging overhead. The
 * corresponding Trace section in [com.auriqo.music.App] is available to Perfetto.
 */
object StartupMetrics {
    private val processStartedAtMs = SystemClock.elapsedRealtime()

    fun mark(stage: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag("Startup").d("%s +%dms", stage, SystemClock.elapsedRealtime() - processStartedAtMs)
        }
    }
}
