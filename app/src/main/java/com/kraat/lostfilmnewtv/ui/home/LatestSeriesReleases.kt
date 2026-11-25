package com.kraat.lostfilmnewtv.ui.home

import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.playback.history.releaseKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val seriesPath = Regex("/series/([^/?#]+)", RegexOption.IGNORE_CASE)
private val releaseDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")

internal fun String.seriesSlug(): String? = seriesPath.find(this)?.groupValues?.get(1)

/** Preserve feed order, but show only the latest released episode of each series. */
internal fun List<ReleaseSummary>.latestSeriesReleases(): List<ReleaseSummary> {
    val latest = linkedMapOf<String, ReleaseSummary>()
    for (item in this) {
        val series = if (item.kind == ReleaseKind.SERIES) item.detailsUrl.seriesSlug() else null
        val key = series?.let { "series:$it" } ?: item.detailsUrl
        val previous = latest[key]
        if (previous == null || (series != null && newestRelease.compare(item, previous) > 0)) latest[key] = item
    }
    return latest.values.toList()
}

internal fun List<ReleaseSummary>.seriesWithNewEpisodes(
    today: LocalDate,
    completedEpisodeKeys: Set<String>,
): Set<String> {
    val recentDays = today.minusDays(6)..today
    return mapNotNullTo(mutableSetOf()) { item ->
        val episode = item.episodeNumber
        if (item.kind != ReleaseKind.SERIES || episode == null || episode == 999 || item.isWatched ||
            item.availabilityLabel.equals("Скоро", ignoreCase = true) || item.detailsUrl.releaseKey() in completedEpisodeKeys
        ) {
            return@mapNotNullTo null
        }
        val releaseDate = item.releasedOn() ?: return@mapNotNullTo null
        item.detailsUrl.seriesSlug()?.takeIf { releaseDate in recentDays }
    }
}

private fun ReleaseSummary.releasedOn(): LocalDate? =
    runCatching { LocalDate.parse(releaseDateRu, releaseDateFormat) }.getOrNull()

private val newestRelease = compareBy<ReleaseSummary>(
    { it.releasedOn()?.toEpochDay() ?: 0L },
    { it.seasonNumber ?: 0 },
    { it.episodeNumber?.takeUnless { number -> number == 999 } ?: -1 },
)
