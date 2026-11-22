package com.kraat.lostfilmnewtv.ui.home

import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val seriesPath = Regex("/series/([^/?#]+)", RegexOption.IGNORE_CASE)
private val releaseDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")

/** Preserve feed order, but show only the latest released episode of each series. */
internal fun List<ReleaseSummary>.latestSeriesReleases(): List<ReleaseSummary> {
    val latest = linkedMapOf<String, ReleaseSummary>()
    for (item in this) {
        val series = if (item.kind == ReleaseKind.SERIES) seriesPath.find(item.detailsUrl)?.groupValues?.get(1) else null
        val key = series?.let { "series:$it" } ?: item.detailsUrl
        val previous = latest[key]
        if (previous == null || (series != null && newestRelease.compare(item, previous) > 0)) latest[key] = item
    }
    return latest.values.toList()
}

private val newestRelease = compareBy<ReleaseSummary>(
    { runCatching { LocalDate.parse(it.releaseDateRu, releaseDateFormat).toEpochDay() }.getOrDefault(0L) },
    { it.seasonNumber ?: 0 },
    { it.episodeNumber?.takeUnless { number -> number == 999 } ?: -1 },
)
