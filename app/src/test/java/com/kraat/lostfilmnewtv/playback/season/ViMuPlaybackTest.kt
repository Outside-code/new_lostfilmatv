package com.kraat.lostfilmnewtv.playback.season

import android.app.Activity
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ViMuPlaybackTest {
    @Test
    fun openingViMuKeepsItsResumeChoice() {
        val source = "http://127.0.0.1:8090/stream/episode.mkv?link=hash&index=1&play="
        val intent = checkNotNull(ViMuPlayback.intent(source, "Фонари", "net.gtvbox.videoplayer"))
        assertEquals("net.gtvbox.videoplayer", intent.`package`)
        assertEquals(source.removeSuffix("="), intent.dataString)
        assertTrue(intent.getBooleanExtra("forceresume", false))
        assertFalse(intent.hasExtra("startfrom"))
    }

    @Test
    fun goingBackPartwaySavesProgressWithoutAdvancing() {
        val data = position(742_500)
        val interrupted = ViMuPlayback.Result.Interrupted(742_500)
        assertEquals(interrupted, ViMuPlayback.playbackResult(Activity.RESULT_CANCELED, data))
        assertEquals(interrupted, ViMuPlayback.playbackResult(2, data))
        assertEquals(ViMuPlayback.Result.Interrupted(), ViMuPlayback.playbackResult(Activity.RESULT_CANCELED, null))
        assertEquals(
            ViMuPlayback.Result.Interrupted(),
            ViMuPlayback.playbackResult(Activity.RESULT_CANCELED, position(3_000_000)),
        )
    }

    @Test
    fun recognizesEndOfEpisodeAcrossViMuVersions() {
        val completed = ViMuPlayback.Result.Completed
        assertEquals(completed, ViMuPlayback.playbackResult(Activity.RESULT_FIRST_USER, null))
        assertEquals(completed, ViMuPlayback.playbackResult(2, position(2_699_600)))
        assertEquals(completed, ViMuPlayback.playbackResult(Activity.RESULT_CANCELED, position(2_700_000)))
        assertEquals(
            ViMuPlayback.Result.Interrupted(2_699_600),
            ViMuPlayback.playbackResult(Activity.RESULT_CANCELED, position(2_699_600)),
        )
        assertEquals(ViMuPlayback.Result.Failed, ViMuPlayback.playbackResult(4, null))
    }

    private fun position(positionMs: Int) = Intent("net.gtvbox.videoplayer.result")
        .putExtra("position", positionMs).putExtra("duration", 2_700_000)

    @Test
    fun doesNotPassCredentialsInTheStreamUrl() {
        val source = "https://user:password@example.org/stream/episode.mkv?link=hash&index=1&play="
        assertNull(ViMuPlayback.intent(source, "Фонари", "net.gtvbox.videoplayer"))
    }
}
