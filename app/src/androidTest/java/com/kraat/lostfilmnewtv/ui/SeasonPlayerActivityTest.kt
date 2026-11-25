package com.kraat.lostfilmnewtv.ui

import android.content.Context
import android.os.SystemClock
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.data.repository.SeriesGuideResult
import com.kraat.lostfilmnewtv.data.model.SeriesGuide
import com.kraat.lostfilmnewtv.di.TestFakeRepository
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryStore
import com.kraat.lostfilmnewtv.playback.season.SeasonMediaItem
import com.kraat.lostfilmnewtv.playback.season.SeasonPlaylist
import com.kraat.lostfilmnewtv.playback.season.SeasonPlayerActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SeasonPlayerActivityTest {
    @Inject lateinit var repository: LostFilmRepository
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Before fun setup() { hiltRule.inject() }

    @Test fun completionAutomaticallyStartsNextItemWithoutRemoteInput() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstEpisode = "https://www.lostfilm.tv/series/Playback_Test/season_1/episode_1/"
        val secondEpisode = "https://www.lostfilm.tv/series/Playback_Test/season_1/episode_2/"
        val playlist = SeasonPlaylist(
            items = listOf(
                SeasonMediaItem(clip(context, "first.wav", 1), "Первая серия", firstEpisode, null, "S01E01"),
                SeasonMediaItem(clip(context, "second.wav", 5), "Вторая серия", secondEpisode, null, "S01E02"),
            ),
            seriesTitle = "Проверка сезона",
        )
        ActivityScenario.launch<SeasonPlayerActivity>(SeasonPlayerActivity.createIntent(context, playlist)).use { scenario ->
            var advanced = false
            val deadline = SystemClock.elapsedRealtime() + 15000
            while (!advanced && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity { activity ->
                    val view = activity.window.decorView.findViewWithTag<PlayerView>("season-player")
                    val history = activity.history.entries.value
                    advanced = view.player?.currentMediaItemIndex == 1 && history.firstOrNull()?.detailsUrl == secondEpisode
                    if (advanced) {
                        assertEquals("S01E02", history.first().subtitle)
                        assertTrue(history.single { it.detailsUrl == firstEpisode }.completed)
                    }
                }
                if (!advanced) SystemClock.sleep(100)
            }
            assertTrue("The second item must start automatically and enter history after the first ends", advanced)
        }
    }

    @Test fun finishingASingleEpisodeRequestsTheNextOneAutomatically() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fake = repository as TestFakeRepository
        fake.lastSeriesGuideRequest = null
        fake.seriesGuideResult = SeriesGuideResult.Success(SeriesGuide("Сериал", null, null, emptyList()))
        val url = "https://www.lostfilm.tv/series/Single_Episode_Test/season_1/episode_1/"
        val playlist = SeasonPlaylist(listOf(SeasonMediaItem(clip(context, "single.wav", 3), "Серия", url, null)))
        val intent = SeasonPlayerActivity.createIntent(context, playlist, autoAdvance = true)

        ActivityScenario.launch<SeasonPlayerActivity>(intent).use { scenario ->
            awaitPosition(scenario, 100)
            assertNull(fake.lastSeriesGuideRequest)
            val deadline = SystemClock.elapsedRealtime() + 12_000
            while (fake.lastSeriesGuideRequest == null && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(50)
            }
            assertEquals(url, fake.lastSeriesGuideRequest)
            scenario.onActivity { activity ->
                assertTrue(activity.history.entries.value.first { it.detailsUrl == url }.completed)
            }
        }
    }

    @Test fun reopeningAnInterruptedEpisodeSeeksToTheSavedPosition() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val url = "https://www.lostfilm.tv/series/Resume_Test/season_1/episode_1/"
        val playlist = SeasonPlaylist(
            items = listOf(SeasonMediaItem(clip(context, "resume.wav", 150), "Серия", url, null)),
            seriesTitle = "Проверка продолжения",
        )
        lateinit var history: PlaybackHistoryStore
        ActivityScenario.launch<SeasonPlayerActivity>(SeasonPlayerActivity.createIntent(context, playlist, autoAdvance = true)).use { scenario ->
            awaitPosition(scenario, 100)
            scenario.onActivity { activity ->
                history = activity.history
                activity.window.decorView.findViewWithTag<PlayerView>("season-player").player!!.seekTo(120_000)
            }
            awaitPosition(scenario, 120_000)
        }
        val entry = history.entries.value.first { it.detailsUrl == url }
        val savedPosition = checkNotNull(entry.positionMs)
        assertTrue(savedPosition in 120_000L..130_000L)
        assertTrue(!entry.completed)

        val resumed = SeasonPlayerActivity.createIntent(context, playlist, startPositionMs = savedPosition, autoAdvance = true)
        ActivityScenario.launch<SeasonPlayerActivity>(resumed).use { scenario ->
            awaitPosition(scenario, savedPosition)
        }
    }

    private fun awaitPosition(scenario: ActivityScenario<SeasonPlayerActivity>, minimum: Long) {
        var reached = false
        val deadline = SystemClock.elapsedRealtime() + 12_000
        while (!reached && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { activity ->
                val player = activity.window.decorView.findViewWithTag<PlayerView>("season-player").player
                reached = player != null && player.isPlaying && player.currentPosition >= minimum
            }
            if (!reached) SystemClock.sleep(50)
        }
        assertTrue("Playback must reach position $minimum", reached)
    }

    private fun clip(context: Context, name: String, seconds: Int): String {
        val length = 16000 * 2 * seconds
        val buffer = ByteBuffer.allocate(44 + length).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + length).put("WAVEfmt ".toByteArray())
        buffer.putInt(16).putShort(1).putShort(1).putInt(16000).putInt(32000).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(length)
        val file = File(context.cacheDir, name).apply { writeBytes(buffer.array()) }
        return android.net.Uri.fromFile(file).toString()
    }

}
