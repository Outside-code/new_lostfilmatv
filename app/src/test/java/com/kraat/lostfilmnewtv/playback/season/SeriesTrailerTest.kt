package com.kraat.lostfilmnewtv.playback.season

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.data.poster.CatalogTrailerIndex
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
class SeriesTrailerTest {
    @Test
    fun indexedTrailerOpensTheExactVideoInYouTubeOnTv() {
        val index = CatalogTrailerIndex { """{"/series/Silo":"8ZYhuvIv1pA"}""" }
        val url = requireNotNull(index.youtubeUrl("https://example.test/series/Silo/"))
        assertTrue(isYouTubeTrailer(url))
        val intent = youtubeTrailerIntent(url)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.google.android.youtube.tv", intent.`package`)
        assertEquals("https://www.youtube.com/watch?v=8ZYhuvIv1pA", intent.dataString)
        assertTrue(intent.getBooleanExtra("android.intent.extra.START_PLAYBACK", false))
    }

    @Test
    fun incompleteIndexDoesNotOpenSearchOrInvalidVideo() {
        val index = CatalogTrailerIndex { """{"/series/Invalid":"fake&search_query=x"}""" }
        assertNull(index.youtubeUrl("https://example.test/series/Invalid/"))
        assertNull(index.youtubeUrl("https://example.test/series/Unknown/"))
    }

    @Test
    fun trailerHasNoSeriesQueueOrProgressKey() {
        val stream = "https://store.bogi.ru/trailer.mp4"
        val intent = SeasonPlayerActivity.createTrailerIntent(ApplicationProvider.getApplicationContext(), stream, "Сериал")
        assertTrue(intent.getBooleanExtra("isTrailer", false))
        assertFalse(intent.getBooleanExtra("autoAdvance", true))
        assertEquals(listOf(stream), intent.getStringArrayListExtra("urls"))
        assertEquals(listOf(""), intent.getStringArrayListExtra("detailsUrls"))
    }
}
