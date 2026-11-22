package com.kraat.lostfilmnewtv.ui.home

import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class LatestSeriesReleasesTest {
    private fun episode(slug: String, season: Int, number: Int, page: Int = 1) = ReleaseSummary(
        id = "$slug-$season-$number", kind = ReleaseKind.SERIES, titleRu = slug, episodeTitleRu = "Серия $number",
        seasonNumber = season, episodeNumber = number, releaseDateRu = "22.11.2026", posterUrl = "",
        detailsUrl = "https://www.lostfilm.today/series/$slug/season_$season/episode_$number/",
        pageNumber = page, positionInPage = 0, fetchedAt = 0L,
    )

    @Test fun twoEpisodesBecomeOneCardForTheLatestEpisode() {
        val first = episode("Alpha", 1, 1)
        val second = episode("Alpha", 1, 2)
        assertEquals(listOf(second), listOf(first, second).latestSeriesReleases())
    }
    @Test fun paginationDoesNotReintroduceAnOlderEpisodeOrMoveOtherSeries() {
        val newest = episode("Alpha", 2, 2)
        val other = episode("Beta", 1, 1)
        val older = episode("Alpha", 1, 9, page = 2)
        assertEquals(listOf(newest, other), listOf(newest, other, older).latestSeriesReleases())
    }
    @Test fun theLatestReleaseDateWinsEvenForEarlierEpisodeNumbers() {
        val fresh = episode("Alpha", 1, 1)
        val old = episode("Alpha", 1, 2).copy(releaseDateRu = "21.11.2026")
        assertEquals(listOf(fresh), listOf(old, fresh).latestSeriesReleases())
    }
    @Test fun distinctSeriesAndMoviesWithMatchingTitlesAreKept() {
        val first = episode("Alpha", 1, 1)
        val second = episode("Beta", 1, 1).copy(titleRu = first.titleRu)
        val movie = first.copy(kind = ReleaseKind.MOVIE, detailsUrl = "https://www.lostfilm.today/movies/Alpha/")
        assertEquals(listOf(first, second, movie), listOf(first, second, movie).latestSeriesReleases())
    }
}
