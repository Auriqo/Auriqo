package com.auriqo.music.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryConsentPolicyTest {
    @Test
    fun unknown_prompts_and_never_enables_collection() {
        assertTrue(TelemetryConsentPolicy.shouldPrompt(true, TelemetryConsent.UNKNOWN))
        assertFalse(TelemetryConsentPolicy.collectionEnabled(TelemetryConsent.UNKNOWN))
    }

    @Test
    fun accepted_does_not_prompt_and_enables_collection() {
        assertFalse(TelemetryConsentPolicy.shouldPrompt(true, TelemetryConsent.ACCEPTED))
        assertTrue(TelemetryConsentPolicy.collectionEnabled(TelemetryConsent.ACCEPTED))
    }

    @Test
    fun declined_or_revoked_does_not_prompt_or_enable_collection() {
        assertFalse(TelemetryConsentPolicy.shouldPrompt(true, TelemetryConsent.DECLINED))
        assertFalse(TelemetryConsentPolicy.collectionEnabled(TelemetryConsent.DECLINED))
    }

    @Test
    fun unavailable_telemetry_never_prompts_or_shows_controls() {
        assertFalse(TelemetryConsentPolicy.shouldPrompt(false, TelemetryConsent.UNKNOWN))
        assertFalse(TelemetryConsentPolicy.shouldShowControls(false))
        assertTrue(TelemetryConsentPolicy.shouldShowControls(true))
    }
}
