package com.kraat.lostfilmnewtv.playback.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackHistoryStoreTest {
    @Test
    fun keepsLatestLaunchFirstAndRetainsCompletionAcrossMirrorsAndRestarts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PlaybackHistoryStore(context)
        val episode = PlaybackHistoryEntry(
            detailsUrl = "https://www.lostfilm.tv/series/Test/season_1/episode_1/",
            title = "Сериал",
            subtitle = "S01E01",
            startedAt = 1L,
        )
        val movie = PlaybackHistoryEntry(
            detailsUrl = "https://www.lostfilm.tv/movies/Test/",
            title = "Фильм",
            startedAt = 2L,
        )
        store.recordLaunch(episode)
        store.markCompleted(episode.detailsUrl)
        store.recordLaunch(movie)
        assertEquals(listOf(movie.title, episode.title), store.entries.value.map { it.title })

        val revisit = episode.copy(
            detailsUrl = "https://www.lostfilm.today/series/Test/season_1/episode_1",
            startedAt = 3L,
        )
        store.recordLaunch(revisit)

        val restored = PlaybackHistoryStore(context).entries.value
        assertEquals(listOf(revisit.copy(completed = true), movie), restored)
        assertTrue(restored.first().completed)
    }

    @Test
    fun savesAnInterruptedPositionAndClearsItWhenEpisodeEnds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val url = "https://www.lostfilm.today/series/example/season_1/episode_2/"
        val initialStore = PlaybackHistoryStore(context)
        initialStore.recordLaunch(PlaybackHistoryEntry(url, "Сериал", startedAt = 1L))
        initialStore.savePosition(url, 120_000L)

        val reopenedStore = PlaybackHistoryStore(context)
        assertEquals(120_000L, reopenedStore.entries.value.first().positionMs)
        reopenedStore.markCompleted(url)
        reopenedStore.savePosition(url, 130_000L)

        val completed = PlaybackHistoryStore(context).entries.value.first()
        assertTrue(completed.completed)
        assertNull(completed.positionMs)
    }
    @Test
    fun launchWithoutAPositionKeepsTheSavedTimeUntilThePlayerReturnsARealPosition() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val url = "https://www.lostfilm.today/series/example/season_1/episode_3/"
        val entry = PlaybackHistoryEntry(url, "Сериал", startedAt = 1L, positionMs = 145_000)
        val store = PlaybackHistoryStore(context)
        store.recordLaunch(entry)
        store.recordLaunch(entry.copy(startedAt = 2L, positionMs = null))

        assertEquals(145_000L, store.entries.value.first().positionMs)
        store.savePosition(url, 0)
        assertEquals(0L, store.entries.value.first().positionMs)
    }

}
