package com.kraat.lostfilmnewtv.data.poster

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.data.db.LostFilmDatabase
import com.kraat.lostfilmnewtv.data.db.TmdbPosterMappingEntity
import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.data.model.TmdbMediaType
import com.kraat.lostfilmnewtv.data.network.OkHttpLostFilmHttpClient
import com.kraat.lostfilmnewtv.data.network.TmdbPosterClient
import com.kraat.lostfilmnewtv.data.parser.LostFilmListParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmSeriesCatalogParser
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opt-in generator run with the same catalog parsers and matching as the app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GenerateCatalogPosterIndex {
    @Test
    fun generate() = runBlocking {
        val destination = System.getenv("LOSTFILM_POSTER_INDEX_OUTPUT")
        assumeTrue("Poster index generation is opt-in", !destination.isNullOrBlank())
        val output = File(requireNotNull(destination)).absoluteFile
        val pages = System.getenv("LOSTFILM_POSTER_INDEX_PAGES")?.toInt() ?: 5
        require(pages > 0)
        val json = Json { prettyPrint = true }
        var snapshot = if (output.isFile) {
            json.decodeFromString<CatalogPosterIndex.Snapshot>(output.readText())
        } else {
            CatalogPosterIndex.Snapshot()
        }
        val index = snapshot.entries.toMutableMap()
        val http = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
        val lostFilm = OkHttpLostFilmHttpClient(okHttpClient = http)
        val tmdb = object : TmdbPosterClient(
            okHttpClient = http,
            apiKey = "",
            baseUrl = "https://auth.bazuka.pp.ua/api/tmdb",
        ) {
            override suspend fun getMovieOverviewRu(tmdbId: Int): String? = null
            override suspend fun getSeriesOverviewRu(tmdbId: Int): String? = null
            override suspend fun getRating(tmdbId: Int, type: TmdbMediaType): String? = null
        }
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LostFilmDatabase::class.java,
        ).build()
        try {
            val dao = database.tmdbPosterDao()
            val resolver = TmdbPosterResolverImpl(tmdb, dao)
            val seen = mutableSetOf<String>()
            for (page in 1..pages) {
                val fetchMovies = async { retryCatalog { LostFilmListParser().parse(lostFilm.fetchMoviesPage(page), page) } }
                val fetchSeries = async {
                    retryCatalog {
                        LostFilmSeriesCatalogParser().parseSearchJson(lostFilm.fetchSeriesCatalogPage(page), page)
                    }
                }
                val movies = fetchMovies.await()
                val series = fetchSeries.await()
                val catalog = movies + series
                val pending = catalog.mapNotNull { item ->
                    val key = CatalogPosterIndex.key(item.detailsUrl) ?: return@mapNotNull null
                    if (!seen.add(key) || index[key]?.let { it.year == item.originalReleaseYear } == true) {
                        return@mapNotNull null
                    }
                    key to item
                }
                for (batch in pending.chunked(5)) {
                    val resolved = batch.map { (key, item) ->
                        async {
                            resolver.resolve(item.detailsUrl, item.titleRu, item.kind, item.originalReleaseYear)
                            val cacheKey = item.detailsUrl.trimEnd('/') + if (item.kind == ReleaseKind.SERIES) "/" else ""
                            val cached = dao.getByDetailsUrl(cacheKey) ?: return@async null
                            if (cached.isNegative || cached.source != TmdbPosterMappingEntity.SOURCE_TMDB) return@async null
                            val poster = imagePath(cached.posterUrl) ?: return@async null
                            key to CatalogPosterIndex.Entry(
                                tmdbId = cached.tmdbId,
                                posterPath = poster,
                                backdropPath = imagePath(cached.backdropUrl),
                                rating = cached.rating,
                                year = item.originalReleaseYear,
                            )
                        }
                    }.awaitAll()
                    index.putAll(resolved.filterNotNull())
                }
                val matchedMovies = movies.count { CatalogPosterIndex.key(it.detailsUrl) in index }
                val matchedSeries = series.count { CatalogPosterIndex.key(it.detailsUrl) in index }
                println("Page $page: $matchedMovies/${movies.size} movies, $matchedSeries/${series.size} series")
                check(page > 1 || (matchedMovies > 0 && matchedSeries > 0))
                if (page == 1) {
                    snapshot = CatalogPosterIndex.Snapshot(
                        generatedAt = System.currentTimeMillis(),
                        movies = movies.mapNotNull(::catalogCard),
                        series = series.mapNotNull(::catalogCard),
                    )
                }
                writeIndex(output, json.encodeToString(snapshot.copy(entries = index.toSortedMap())) + "\n")
                if (catalog.isEmpty()) break
            }
        } finally {
            database.close()
        }
        Unit
    }

    private suspend fun retryCatalog(load: suspend () -> List<ReleaseSummary>): List<ReleaseSummary> {
        repeat(2) {
            try {
                return load()
            } catch (_: IOException) {
                delay(500)
            }
        }
        return load()
    }

    private fun imagePath(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        if (parsed.host != "image.tmdb.org") return null
        return ("/" + parsed.pathSegments.last()).takeIf(CatalogPosterIndex.IMAGE_PATH::matches)
    }

    private fun catalogCard(item: ReleaseSummary): CatalogPosterIndex.Card? {
        val url = item.detailsUrl.toHttpUrlOrNull() ?: return null
        if (CatalogPosterIndex.key(item.detailsUrl) == null) return null
        return CatalogPosterIndex.Card(
            path = url.encodedPath,
            title = item.titleRu,
            originalTitle = item.episodeTitleRu,
            releaseDate = item.releaseDateRu,
            availability = item.availabilityLabel,
            year = item.originalReleaseYear,
        )
    }

    private fun writeIndex(output: File, content: String) {
        val directory = requireNotNull(output.parentFile)
        directory.mkdirs()
        val staging = Files.createTempFile(directory.toPath(), "catalog-posters", ".json")
        try {
            staging.toFile().writeText(content)
            Files.move(staging, output.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(staging)
        }
    }
}
