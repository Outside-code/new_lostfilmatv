package com.kraat.lostfilmnewtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BackdropImageRequestTest {
    @Test
    fun upgradesCachedTmdbImagesWithoutChangingProxyOriginOrQuery() {
        assertEquals(
            "https://auth.example.com/api/tmdb/t/p/original/backdrop.jpg?v=1",
            highQualityBackdropUrl("https://auth.example.com/api/tmdb/t/p/w1280/backdrop.jpg?v=1"),
        )
        assertEquals(
            "https://image.tmdb.org/t/p/original/poster.jpg",
            highQualityBackdropUrl("https://image.tmdb.org/t/p/w780/poster.jpg"),
        )
    }

    @Test
    fun preservesOriginalAndOtherProviders() {
        listOf(
            "https://image.tmdb.org/t/p/original/backdrop.jpg",
            "https://st.kp.yandex.net/images/film_iphone/123.jpg",
            "https://lostfilm.today/posters/123.jpg",
        ).forEach { assertEquals(it, highQualityBackdropUrl(it)) }
    }
}
