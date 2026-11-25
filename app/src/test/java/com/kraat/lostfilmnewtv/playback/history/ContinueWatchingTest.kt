package com.kraat.lostfilmnewtv.playback.history

import com.kraat.lostfilmnewtv.data.model.SeriesGuide
import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.data.model.SeriesGuideSeason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinueWatchingTest {
    @Test
    fun startsAfterLatestSeriesEpisodeAndSkipsUnavailableAndWatchedEpisodes() {
        val anchor = history(episode(1, 1).detailsUrl)
        val locallyCompleted = history(episode(2, 1).detailsUrl.replace("example.org", "mirror.example.org"), true)
        val next = episode(2, 2)
        val history = listOf(history("https://example.org/movies/a-film"), anchor, locallyCompleted)
        val guide = guide(
            SeriesGuideSeason(2, listOf(episode(2, 3), next, episode(2, 1))),
            SeriesGuideSeason(1, listOf(
                episode(1, 1),
                episode(1, 2).copy(isWatched = true),
                episode(1, 3).copy(isAvailable = false),
                episode(1, 999),
            )),
        )

        assertEquals(anchor, history.latestUnfinishedSeriesEpisode())
        assertEquals(next, guide.nextUnwatchedEpisode(anchor.detailsUrl, history))
    }

    @Test
    fun doesNotReturnEarlierEpisodesAfterFinishingASeries() {
        val anchor = history(episode(1, 2).detailsUrl)
        val guide = guide(SeriesGuideSeason(1, listOf(episode(1, 1), episode(1, 2))))
        assertNull(guide.nextUnwatchedEpisode(anchor.detailsUrl, listOf(anchor)))
        assertNull(listOf(history("https://example.org/movies/a-film")).latestUnfinishedSeriesEpisode())
    }

    @Test
    fun continueSelectsTheInterruptedEpisodeWithItsSavedPosition() {
        val interrupted = history(episode(1, 3).detailsUrl).copy(positionMs = 120_000)
        val finished = history(episode(2, 1).detailsUrl, completed = true)
        val history = listOf(finished, interrupted)

        assertEquals(interrupted, history.latestUnfinishedSeriesEpisode())
        assertEquals(120_000L, history.latestUnfinishedSeriesEpisode()?.positionMs)
        assertNull(listOf(finished).latestUnfinishedSeriesEpisode())
    }

    @Test
    fun resumeLabelShowsTheEpisodeAndElapsedPlaybackTime() {
        val entry = history(episode(2, 4).detailsUrl + "/?from=history")
        assertEquals("S02E04 · 18:42", entry.copy(positionMs = 1_122_500).episodeAndPositionLabel())
        assertEquals("S02E04 · 1:01:02", entry.copy(positionMs = 3_662_000).episodeAndPositionLabel())
        assertEquals("S02E04 · 0:00", entry.episodeAndPositionLabel())
        assertNull(history("https://example.org/movies/a-film").episodeAndPositionLabel())
    }

    private fun episode(season: Int, number: Int) = SeriesGuideEpisode(
        detailsUrl = "https://example.org/series/a-series/season_${season}/episode_$number",
        episodeId = null,
        seasonNumber = season,
        episodeNumber = number,
        episodeTitleRu = null,
        releaseDateRu = "",
        isWatched = false,
    )

    private fun history(url: String, completed: Boolean = false) = PlaybackHistoryEntry(
        detailsUrl = url, title = "Release", startedAt = 1L, completed = completed,
    )

    private fun guide(vararg seasons: SeriesGuideSeason) = SeriesGuide(
        seriesTitleRu = "Series", posterUrl = null, selectedEpisodeDetailsUrl = null, seasons = seasons.toList(),
    )
}
