package com.auriqo.music.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutPolicyTest {
    @Test
    fun `responsive layout uses compact and full grid breakpoints`() {
        assertEquals(0, WidgetLayoutPolicy.maxQuickPicks(209, 500))
        assertEquals(4, WidgetLayoutPolicy.maxQuickPicks(250, 329))
        assertEquals(8, WidgetLayoutPolicy.maxQuickPicks(360, 400))
    }

    @Test
    fun `layout avoids an incomplete second row and reserves empty slots`() {
        assertEquals(4, WidgetLayoutPolicy.balancedDisplayCount(7, 8))
        assertEquals(8, WidgetLayoutPolicy.balancedDisplayCount(12, 8))
        assertEquals(setOf(2, 3), WidgetLayoutPolicy.reservedEmptySlots(2))
    }
}
