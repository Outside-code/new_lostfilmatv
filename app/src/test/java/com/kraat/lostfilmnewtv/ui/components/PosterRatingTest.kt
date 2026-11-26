package com.kraat.lostfilmnewtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosterRatingTest {
    @Test
    fun acceptsValidTmdbScoreOnItsArtwork() {
        assertEquals("8.2", tmdbPosterRating("8.2", "https://image.tmdb.org/t/p/w780/test.jpg"))
    }

    @Test
    fun skipsMissingRatingsAndRatingsFromOtherProviders() {
        val tmdbPoster = "https://image.tmdb.org/t/p/w780/test.jpg"
        assertNull(tmdbPosterRating(null, tmdbPoster))
        assertNull(tmdbPosterRating("0.0", tmdbPoster))
        assertNull(tmdbPosterRating("NaN", tmdbPoster))
        assertNull(tmdbPosterRating("8.2", "https://kinopoiskapiunofficial.tech/images/posters/kp/test.jpg"))
    }
}
