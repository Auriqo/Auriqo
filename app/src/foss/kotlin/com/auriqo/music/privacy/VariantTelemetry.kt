package com.auriqo.music.privacy

import android.content.Context

/** FOSS intentionally ships no Firebase SDK; consent has no telemetry side effects. */
object VariantTelemetry {
    fun isAvailable(context: Context): Boolean = false

    fun setCollectionEnabled(context: Context, enabled: Boolean) = Unit
}
