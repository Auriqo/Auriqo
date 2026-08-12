

package com.auriqo.music.models

import com.music.innertube.models.YTItem
import com.auriqo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
