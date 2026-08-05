package com.auriqo.music.discord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscordPreferenceBoundaryTest {
    @Test
    fun `unknown persisted values fall back to safe listening and online defaults`() {
        assertEquals(DiscordActivityType.Listening, DiscordActivityType.fromPreference("unknown"))
        assertEquals(DiscordOnlineStatus.Online, DiscordOnlineStatus.fromPreference("unknown"))
        assertEquals(DiscordActivityPlatform.Android.bit, DiscordActivityPlatform.fromPreference("unknown"))
    }

    @Test
    fun `gateway activity mappings round trip known activity types only`() {
        val listening = ActivityTypes.fromString("LISTENING")

        assertEquals("LISTENING", listening?.let(ActivityTypes::fromInt))
        assertNull(ActivityTypes.fromString("NOT_A_REAL_ACTIVITY"))
        assertNull(ActivityTypes.fromInt(99))
    }
}
