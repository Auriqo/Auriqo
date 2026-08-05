package com.auriqo.music.privacy

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** GMS-only Firebase bridge. It is never referenced by the FOSS source set. */
object VariantTelemetry {
    fun isAvailable(context: Context): Boolean = firebaseApp(context) != null

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        // google-services.json is intentionally optional for local/FOSS validation.
        // Do not initialize or collect when the GMS app has no Firebase configuration.
        val firebaseApp = firebaseApp(context) ?: return
        FirebaseAnalytics.getInstance(firebaseApp.applicationContext)
        .setAnalyticsCollectionEnabled(enabled)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    private fun firebaseApp(context: Context): FirebaseApp? =
        FirebaseApp.initializeApp(context.applicationContext)
}
