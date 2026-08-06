package com.auriqo.music.widget

/** Layout rules shared by the playlist widget's responsive shortcut grid. */
internal object WidgetLayoutPolicy {
    fun maxQuickPicks(minWidth: Int, minHeight: Int): Int = when {
        minWidth < 210 -> 0
        minHeight < 330 -> 4
        minWidth >= 360 -> 8
        else -> 4
    }

    fun balancedDisplayCount(available: Int, maxItems: Int): Int {
        val capped = minOf(available, maxItems, 8)
        return when {
            capped >= 8 -> 8
            capped >= 4 -> 4
            else -> capped
        }
    }

    fun reservedEmptySlots(displayCount: Int): Set<Int> = when (displayCount) {
        1 -> setOf(1, 2, 3)
        2 -> setOf(2, 3)
        3 -> setOf(3)
        else -> emptySet()
    }
}
