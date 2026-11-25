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
    fun interruptedPlaybackReturnsTheActualPositionInMilliseconds() {
        val data = Intent("net.gtvbox.videoplayer.result")
            .putExtra("position", 742_500).putExtra("duration", 2_700_000)
        assertEquals(742_500L, ViMuPlayback.positionOnExit(Activity.RESULT_CANCELED, data))
        assertNull(ViMuPlayback.positionOnExit(Activity.RESULT_CANCELED, null))
        assertNull(ViMuPlayback.positionOnExit(Activity.RESULT_CANCELED, Intent()))
        assertNull(ViMuPlayback.positionOnExit(Activity.RESULT_CANCELED, data.putExtra("position", 3_000_000)))
        assertNull(ViMuPlayback.positionOnExit(ViMuPlayback.COMPLETED, data))
    }

    @Test
    fun doesNotPassCredentialsInTheStreamUrl() {
        val source = "https://user:password@example.org/stream/episode.mkv?link=hash&index=1&play="
        assertNull(ViMuPlayback.intent(source, "Фонари", "net.gtvbox.videoplayer"))
    }
}
