package com.auriqa.music.utils.potoken

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PoTokenAssetTest {
    @Test
    fun bundledAsset_containsBotGuardBridgeFunctions() {
        val asset = sequenceOf(
            File("src/main/assets/po_token.html"),
            File("app/src/main/assets/po_token.html"),
        ).first { it.isFile }
        val html = asset.readText()

        assertTrue(html.contains("function runBotGuard"))
        assertTrue(html.contains("function obtainPoToken"))
        assertTrue(html.contains("</script>"))
    }
}
