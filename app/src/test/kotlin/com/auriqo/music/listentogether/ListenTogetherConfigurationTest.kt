package com.auriqo.music.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherConfigurationTest {
    @Test
    fun `blank endpoints leave listen together unavailable without a link`() {
        val configuration = ListenTogetherConfiguration("   ", "")

        assertFalse(configuration.isAvailable)
        assertNull(configuration.serverUrl)
        assertNull(configuration.inviteLink("ABCD1234"))
    }

    @Test
    fun `configured secure endpoints preserve protocol and generate invite links`() {
        val configuration = ListenTogetherConfiguration(
            " wss://sync.example.test/ws ",
            "https://share.example.test/base/",
        )

        assertTrue(configuration.isAvailable)
        assertEquals("wss://sync.example.test/ws", configuration.serverUrl)
        assertEquals(
            "https://share.example.test/base/listen?code=ABCD1234",
            configuration.inviteLink("ABCD1234"),
        )
    }

    @Test
    fun `invalid schemes never create connections or fake invite links`() {
        val configuration = ListenTogetherConfiguration(
            "https://sync.example.test/ws",
            "wss://share.example.test",
        )

        assertFalse(configuration.isAvailable)
        assertNull(configuration.inviteLink("ABCD1234"))
    }

    @Test
    fun `a secure stored server overrides deployment while insecure input falls back`() {
        val configuration = ListenTogetherConfiguration(
            "wss://deployment.example.test/ws",
            "https://share.example.test",
        )

        assertEquals(
            "wss://custom.example.test/ws",
            configuration.resolveServerUrl(" wss://custom.example.test/ws "),
        )
        assertEquals(
            "wss://deployment.example.test/ws",
            configuration.resolveServerUrl("ws://insecure.example.test/ws"),
        )
    }
}
