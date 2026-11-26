package com.kraat.lostfilmnewtv.data.parser

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LostFilmTrailerParserTest {
    @Test
    fun picksThePublishedTrailerAndSkipsInterviewsAndUnrelatedSources() {
        val page = Jsoup.parse("""
            <div class="video-block"><div class="description">Интервью</div>
              <div class="play-btn" data-src="https://store.bogi.ru/interview.mp4"></div></div>
            <div class="video-block"><div class="description">Трейлер</div>
              <div class="play-btn" data-src="https://example.test/trailer.mp4"></div></div>
            <div class="video-block"><div class="description">Озвученный трейлер</div>
              <div class="play-btn" data-src="https://store.bogi.ru/trailer.mp4"></div></div>
        """.trimIndent())

        assertEquals("https://store.bogi.ru/trailer.mp4", page.seriesTrailerUrl())
        assertNull(Jsoup.parse("<h1>Сериал без видео</h1>").seriesTrailerUrl())
    }
}
