package com.auriqo.music.utils.cipher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FunctionNameExtractorTest {
    @Test
    fun signatureTimestampUsesTheAnchoredPlayerLiteral() {
        val playerJs = """var config = {"signatureTimestamp": 20476, "sts": 1};"""

        assertEquals(20476, FunctionNameExtractor.extractSignatureTimestamp(playerJs))
    }

    @Test
    fun signatureTimestampFallbackDoesNotMatchTheEndOfAnotherIdentifier() {
        val playerJs = """var config = {"requests": 4};"""

        assertNull(FunctionNameExtractor.extractSignatureTimestamp(playerJs, knownHash = "not-a-player-hash"))
    }
}
