package com.kraat.lostfilmnewtv.playback.history

import com.kraat.lostfilmnewtv.data.model.ReleaseDetails
import com.kraat.lostfilmnewtv.data.model.SeriesGuide
import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.data.repository.DetailsResult
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.playback.PlaybackPreferencesStore
import com.kraat.lostfilmnewtv.playback.season.playbackEpisodes
import com.kraat.lostfilmnewtv.ui.details.DetailsTorrentRowUiModel
import com.kraat.lostfilmnewtv.ui.details.resolvePreferredTorrentRow
import com.kraat.lostfilmnewtv.ui.details.toTorrentRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val episodePath = Regex("""/series/[^/]+/season_(\d+)/episode_(\d+)""")
private val seriesPath = Regex("""^/series/[^/]+""")

internal fun String.isSeriesEpisodeUrl(): Boolean = episodePath.matches(releaseKey())

internal fun List<PlaybackHistoryEntry>.latestUnfinishedSeriesEpisode(
    seriesUrl: String? = null,
): PlaybackHistoryEntry? {
    val series = seriesUrl?.let { seriesPath.find(it.releaseKey())?.value }
    if (seriesUrl != null && series == null) return null
    return firstOrNull { entry ->
        !entry.completed && episodePath.matches(entry.key) &&
            (series == null || seriesPath.find(entry.key)?.value == series)
    }
}

internal suspend fun LostFilmRepository.prepareEpisodeResume(
    entry: PlaybackHistoryEntry,
    preferences: PlaybackPreferencesStore,
    supportsSource: (String) -> Boolean,
): ContinueWatchingResult = withContext(Dispatchers.IO) {
    val loaded = when (val result = loadDetails(entry.detailsUrl)) {
        is DetailsResult.Success -> result.details
        is DetailsResult.Error -> return@withContext ContinueWatchingResult.Unavailable("Не удалось загрузить серию")
    }
    val details = if (loaded.torrentLinks.isEmpty()) {
        (refreshDetailsExtras(loaded) as? DetailsResult.Success)?.details ?: loaded
    } else {
        loaded
    }
    val torrent = resolvePreferredTorrentRow(
        preferences.readDefaultQuality(),
        details.toTorrentRows().filter { supportsSource(it.url) },
    ) ?: return@withContext ContinueWatchingResult.Unavailable("Для этой серии пока нет раздачи")
    ContinueWatchingResult.Ready(details, torrent, entry.positionMs?.coerceAtLeast(0) ?: 0L)
}

internal fun PlaybackHistoryEntry.episodeAndPositionLabel(): String? {
    val match = episodePath.matchEntire(key) ?: return null
    val season = match.groupValues[1].padStart(2, '0')
    val episode = match.groupValues[2].padStart(2, '0')
    val code = "S${season}E$episode"
    val savedPosition = positionMs?.takeIf { it >= 1000 } ?: return code
    val totalSeconds = savedPosition / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds / 60) % 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    val position = if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
    return "$code · $position"
}

internal fun SeriesGuide.nextUnwatchedEpisode(
    previousDetailsUrl: String,
    history: List<PlaybackHistoryEntry>,
): SeriesGuideEpisode? {
    val match = episodePath.matchEntire(previousDetailsUrl.releaseKey()) ?: return null
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
    data class Ready(
        val details: ReleaseDetails,
        val torrent: DetailsTorrentRowUiModel,
        val positionMs: Long,
    ) : ContinueWatchingResult
    data class Unavailable(val message: String) : ContinueWatchingResult
}
