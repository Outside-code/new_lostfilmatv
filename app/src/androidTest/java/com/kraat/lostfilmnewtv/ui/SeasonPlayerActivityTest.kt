package com.kraat.lostfilmnewtv.ui

import android.os.SystemClock
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.playback.season.SeasonMediaItem
import com.kraat.lostfilmnewtv.playback.season.SeasonPlaylist
import com.kraat.lostfilmnewtv.playback.season.SeasonPlayerActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SeasonPlayerActivityTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Before fun setup() { hiltRule.inject() }

    @Test fun completionAutomaticallyStartsNextItemWithoutRemoteInput() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        fun clip(name: String, seconds: Int): String {
            val length = 16000 * 2 * seconds
            val buffer = ByteBuffer.allocate(44 + length).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + length).put("WAVEfmt ".toByteArray())
            buffer.putInt(16).putShort(1).putShort(1).putInt(16000).putInt(32000).putShort(2).putShort(16)
            buffer.put("data".toByteArray()).putInt(length)
            val file = File(context.cacheDir, name).apply { writeBytes(buffer.array()) }
            return android.net.Uri.fromFile(file).toString()
        }
        val firstEpisode = "https://www.lostfilm.tv/series/Playback_Test/season_1/episode_1/"
        val secondEpisode = "https://www.lostfilm.tv/series/Playback_Test/season_1/episode_2/"
        val playlist = SeasonPlaylist(
            items = listOf(
                SeasonMediaItem(clip("first.wav", 1), "Первая серия", firstEpisode, null, "S01E01"),
                SeasonMediaItem(clip("second.wav", 5), "Вторая серия", secondEpisode, null, "S01E02"),
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
}
