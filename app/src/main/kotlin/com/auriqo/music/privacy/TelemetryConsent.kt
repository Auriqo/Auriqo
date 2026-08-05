package com.auriqo.music.privacy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.auriqo.music.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A recorded choice is deliberately different from an unanswered first-launch prompt. */
enum class TelemetryConsent {
    UNKNOWN,
    ACCEPTED,
    DECLINED,
}

internal object TelemetryConsentPolicy {
    fun shouldPrompt(isAvailable: Boolean, choice: TelemetryConsent): Boolean =
        isAvailable && choice == TelemetryConsent.UNKNOWN

    fun shouldShowControls(isAvailable: Boolean): Boolean = isAvailable

    fun collectionEnabled(choice: TelemetryConsent): Boolean = choice == TelemetryConsent.ACCEPTED
}

/**
 * Canonical, variant-independent consent API. The selected product flavor supplies
 * [VariantTelemetry], so common UI never loads a Firebase class in FOSS builds.
 */
object TelemetryConsentStore {
    private val consentKey = stringPreferencesKey("telemetry_consent")

    fun consent(context: Context): Flow<TelemetryConsent> = context.dataStore.data
        .map { preferences ->
            preferences[consentKey]
                ?.let { value -> runCatching { TelemetryConsent.valueOf(value) }.getOrNull() }
                ?: TelemetryConsent.UNKNOWN
        }
        .distinctUntilChanged()

    fun isAvailable(context: Context): Boolean =
        VariantTelemetry.isAvailable(context.applicationContext)

    suspend fun synchronize(context: Context) {
        if (!isAvailable(context)) return
        VariantTelemetry.setCollectionEnabled(
            context = context.applicationContext,
            enabled = TelemetryConsentPolicy.collectionEnabled(consent(context).first()),
        )
    }

    suspend fun record(context: Context, consent: TelemetryConsent) {
        require(consent != TelemetryConsent.UNKNOWN) { "A persisted consent choice must be accepted or declined." }
        context.dataStore.edit { preferences ->
            preferences[consentKey] = consent.name
        }
        if (!isAvailable(context)) return
        VariantTelemetry.setCollectionEnabled(
            context = context.applicationContext,
            enabled = TelemetryConsentPolicy.collectionEnabled(consent),
        )
    }
}
