package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

private const val compactCountSuffixPattern =
    "KkMmBbTt\\u4e07\\u842c\\u5104\\u4ebf\\u5146\\u5343\\ucc9c\\ub9cc\\uc5b5"
private val countTextRegex =
    Regex("""\p{Nd}[\p{Nd}\s,.\uFF0C\uFF0E]*[$compactCountSuffixPattern]*""")
private val separatedSuffixRegex = Regex("""\s+(?=[$compactCountSuffixPattern]$)""")

fun Runs?.extractCountText(): String? {
    val texts = this?.runs
        ?.map { it.text.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    return texts
        .joinToString(separator = "")
        .extractCountValue()
        ?: texts.firstNotNullOfOrNull { it.extractCountValue() }
}

private fun String.extractCountValue(): String? =
    countTextRegex.find(this)
        ?.value
        ?.trim()
        ?.replace(separatedSuffixRegex, "")
        ?.takeIf { value -> value.any { it.isDigit() } }

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text == " • ") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

/**
 * Best-effort extraction of the localized "view count" text from a song's subtitle runs,
 * e.g. "1.2M views", "45K views", "12 mil vistas". Returns null when the subtitle does not
 * contain a view-count token (playlists and albums typically omit it).
 */
fun List<Run>.extractViewCountText(): String? {
    val viewPattern = Regex(
        """\b\d[\d\s.,]*\s*(?:[KkMmBbTt]|mil|k|m|b)?\s*(?:views?|vistas?|vues|reproducciones?|visualizaciones?)\b""",
        RegexOption.IGNORE_CASE
    )
    val joined = joinToString(separator = " ") { it.text }
    return viewPattern.find(joined)?.value?.trim()?.let {
        it.replace(Regex("""\s+"""), " ")
    } ?: splitBySeparator().firstNotNullOfOrNull { segment ->
        segment.joinToString(" ") { it.text }
            .trim()
            .takeIf { token -> viewPattern.containsMatchIn(token) }
    }
}

fun List<List<Run>>.clean(): List<List<Run>> =
    if (getOrNull(0)?.getOrNull(0)?.navigationEndpoint != null ||
        (getOrNull(0)?.getOrNull(0)?.text?.contains(regex = Regex("[&,]"))) != false
    ) {
        this
    } else {
        this.drop(1)
    }

fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }
