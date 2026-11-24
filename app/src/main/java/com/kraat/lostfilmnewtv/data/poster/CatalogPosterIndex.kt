package com.kraat.lostfilmnewtv.data.poster

import android.util.Log
import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.data.model.TmdbImageUrls
import com.kraat.lostfilmnewtv.data.network.TmdbImageFallbackInterceptor.Companion.TMDB_DIRECT_IMAGE_PREFIX
import com.kraat.lostfilmnewtv.data.parser.resolveUrl
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CatalogPosterIndex internal constructor(read: () -> String) {
    private val snapshot by lazy {
        try {
            Json.decodeFromString<Snapshot>(read())
        } catch (error: Exception) {
            Log.w("CatalogPosterIndex", "Unable to load bundled poster index", error)
            Snapshot()
        }
    }

    internal fun entry(detailsUrl: String, year: Int?): Entry? {
        val path = key(detailsUrl) ?: return null
        return snapshot.entries[path]?.takeIf {
            it.tmdbId > 0 && IMAGE_PATH.matches(it.posterPath) &&
                (year == null || it.year == null || it.year == year)
        }
    }

    internal fun firstPage(kind: ReleaseKind, now: Long): List<ReleaseSummary> {
        if (now - snapshot.generatedAt !in 0..SNAPSHOT_MAX_AGE_MS) return emptyList()
        val cards = if (kind == ReleaseKind.MOVIE) snapshot.movies else snapshot.series
        return cards.mapIndexedNotNull { position, card ->
            val url = resolveUrl(card.path)
            val artwork = entry(url, card.year)?.artwork()
            if (key(url) == null) return@mapIndexedNotNull null
            ReleaseSummary(
                id = url,
                kind = kind,
                titleRu = card.title,
                episodeTitleRu = card.originalTitle,
                seasonNumber = null,
                episodeNumber = null,
                releaseDateRu = card.releaseDate,
                posterUrl = artwork?.posterUrl.orEmpty(),
                detailsUrl = url,
                pageNumber = 1,
                positionInPage = position,
                fetchedAt = snapshot.generatedAt,
                availabilityLabel = card.availability,
                originalReleaseYear = card.year,
                backdropUrl = artwork?.backdropUrl,
                tmdbRating = artwork?.rating,
            )
        }
    }

    @Serializable
    internal data class Snapshot(
        val entries: Map<String, Entry> = emptyMap(),
        val generatedAt: Long = 0,
        val movies: List<Card> = emptyList(),
        val series: List<Card> = emptyList(),
    )

    @Serializable
    internal data class Card(
        val path: String,
        val title: String,
        val originalTitle: String? = null,
        val releaseDate: String = "",
        val availability: String? = null,
        val year: Int? = null,
    )

    @Serializable
    internal data class Entry(
        val tmdbId: Int,
        val posterPath: String,
        val backdropPath: String? = null,
        val rating: String? = null,
        val year: Int? = null,
    ) {
        fun artwork() = TmdbImageUrls(
            posterUrl = "${TMDB_DIRECT_IMAGE_PREFIX}w780$posterPath",
            backdropUrl = backdropPath?.takeIf(IMAGE_PATH::matches)
                ?.let { "${TMDB_DIRECT_IMAGE_PREFIX}w1280$it" }.orEmpty(),
            rating = rating,
        )
    }

    companion object {
        internal const val FILE_NAME = "catalog-posters.json"
        private const val SNAPSHOT_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
        private val CATALOG_PATH = Regex("/(?:movies|series)/[^/]+")
        internal val IMAGE_PATH = Regex("/[A-Za-z0-9_.-]+\\.(?:jpg|png|webp)")

        internal fun key(detailsUrl: String): String? =
            detailsUrl.toHttpUrlOrNull()?.encodedPath?.trimEnd('/')?.takeIf(CATALOG_PATH::matches)
    }
}
