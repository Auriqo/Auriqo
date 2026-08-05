package com.auriqo.music.lastfm

import com.auriqo.music.models.lastfm.Authentication
import com.auriqo.music.models.lastfm.LastFmError
import com.auriqo.music.models.lastfm.TokenResponse
import com.auriqo.music.utils.lastfm.LastFM
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LastFmSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `auth and error payloads decode without live Lastfm access`() {
        val auth = json.decodeFromString<Authentication>("""{"session":{"name":"auri","key":"session-key","subscriber":1}}""")
        val token = json.decodeFromString<TokenResponse>("""{"token":"token-value"}""")
        val error = json.decodeFromString<LastFmError>("""{"error":9,"message":"Invalid session key"}""")

        assertEquals("auri", auth.session.name)
        assertEquals("session-key", auth.session.key)
        assertEquals("token-value", token.token)
        assertEquals(9, error.error)
    }

    @Test
    fun `lastfm exception retains its code and safe diagnostic representation`() {
        val error = LastFM.LastFmException(9, "Invalid session key")

        assertEquals(9, error.code)
        assertEquals("LastFmException(code=9, message=Invalid session key)", error.toString())
    }
}
