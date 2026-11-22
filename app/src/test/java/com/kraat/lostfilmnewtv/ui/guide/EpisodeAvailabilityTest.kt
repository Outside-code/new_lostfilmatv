package com.kraat.lostfilmnewtv.ui.guide

import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeAvailabilityTest {
    private val today = LocalDate.of(2026, 11, 22)
    private fun episode(date: String, available: Boolean) = SeriesGuideEpisode(
        detailsUrl = "episode", episodeId = null, seasonNumber = 1, episodeNumber = 1,
        episodeTitleRu = null, releaseDateRu = date, isWatched = false, isAvailable = available,
    )

    @Test fun releasedEpisodeShowsAvailability() {
        assertEquals("Доступна · 21.11.2026", episode("21.11.2026", true).availabilityLabel(today))
    }
    @Test fun upcomingEpisodeShowsDaysUntilRussianRelease() {
        assertEquals("До выхода: 5 дн. · 27.11.2026", episode("27.11.2026", false).availabilityLabel(today))
    }
    @Test fun passedDateDoesNotMakeUnavailableEpisodeAvailable() {
        assertEquals("Ещё не вышло · 21.11.2026", episode("21.11.2026", false).availabilityLabel(today))
        assertEquals("Ещё не вышло · ожидается сегодня", episode("22.11.2026", false).availabilityLabel(today))
    }
    @Test fun unknownOrInvalidDateDoesNotInventCountdown() {
        for (date in listOf("", "TBA", "31.02.2027")) {
            assertEquals("Ещё не вышло · дата не объявлена", episode(date, false).availabilityLabel(today))
        }
    }
}
