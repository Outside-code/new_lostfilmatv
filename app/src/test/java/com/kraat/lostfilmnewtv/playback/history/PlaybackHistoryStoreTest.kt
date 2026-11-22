package com.kraat.lostfilmnewtv.playback.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
}
