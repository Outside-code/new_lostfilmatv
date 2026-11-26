package com.kraat.lostfilmnewtv.playback.season

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SeriesTrailerTest {
    @Test
    fun youtubeSearchEncodesTheWholeSeriesTitle() {
        val url = youtubeTrailerSearchUrl("  Love & Death (2023)  ")
        assertEquals("https", url.scheme)
        assertEquals("www.youtube.com", url.host)
        assertEquals("/results", url.path)
        assertEquals("Love & Death (2023) официальный трейлер", url.getQueryParameter("search_query"))
        assertEquals(setOf("search_query"), url.queryParameterNames)
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
