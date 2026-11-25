package com.kraat.lostfilmnewtv.playback.history

import com.kraat.lostfilmnewtv.data.model.ReleaseDetails
import com.kraat.lostfilmnewtv.data.model.SeriesGuide
import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.playback.season.playbackEpisodes

private val episodePath = Regex("""/series/[^/]+/season_(\d+)/episode_(\d+)""")

internal fun List<PlaybackHistoryEntry>.latestSeriesEpisode(): PlaybackHistoryEntry? =
    firstOrNull { episodePath.matches(it.key) }

internal fun SeriesGuide.nextUnwatchedEpisode(
    anchor: PlaybackHistoryEntry,
    history: List<PlaybackHistoryEntry>,
): SeriesGuideEpisode? {
    val match = episodePath.matchEntire(anchor.key) ?: return null
    val seasonNumber = match.groupValues[1].toIntOrNull() ?: return null
    val episodeNumber = match.groupValues[2].toIntOrNull() ?: return null
    val completedKeys = history.filter { it.completed }.mapTo(mutableSetOf()) { it.key }
    return seasons.sortedBy { it.seasonNumber }.asSequence()
        .flatMap { it.playbackEpisodes() }
        .firstOrNull { episode ->
            val comesAfterAnchor = episode.seasonNumber > seasonNumber ||
                (episode.seasonNumber == seasonNumber && episode.episodeNumber > episodeNumber)
            comesAfterAnchor && !episode.isWatched && episode.detailsUrl.releaseKey() !in completedKeys
        }
}

internal fun ReleaseDetails.toPlaybackHistoryEntry(startedAt: Long): PlaybackHistoryEntry {
    val episodeLabel = seasonNumber?.let { season ->
        episodeNumber?.let { episode -> "Сезон $season · серия $episode" }
    }
    return PlaybackHistoryEntry(
        detailsUrl = detailsUrl,
        title = titleRu,
        subtitle = listOfNotNull(episodeLabel, episodeTitleRu)
            .filter { it.isNotBlank() }.joinToString(" · "),
        posterUrl = posterUrl,
        startedAt = startedAt,
    )
}

internal sealed interface ContinueWatchingResult {
    data class Ready(val details: ReleaseDetails, val torrentUrl: String) : ContinueWatchingResult
    data class Unavailable(val message: String) : ContinueWatchingResult
}
