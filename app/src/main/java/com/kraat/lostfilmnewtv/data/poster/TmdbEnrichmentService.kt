package com.kraat.lostfilmnewtv.data.poster

import com.kraat.lostfilmnewtv.data.network.LostFilmConcurrencyLimits.SUMMARY_ENRICHMENT_CONCURRENCY
import com.kraat.lostfilmnewtv.data.db.ReleaseDao
import com.kraat.lostfilmnewtv.data.db.ReleaseSummaryEntity
import com.kraat.lostfilmnewtv.data.model.LostFilmSearchItem
import android.util.Log
import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.data.model.TmdbImageUrls
import com.kraat.lostfilmnewtv.data.parser.extractYear
import com.kraat.lostfilmnewtv.data.parser.toSummaryEntities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

interface TmdbEnrichmentService {
    suspend fun enrichSummaries(
        items: List<ReleaseSummary>,
        persistToCache: Boolean = false,
    ): List<ReleaseSummary>

    suspend fun enrichSearchItems(items: List<LostFilmSearchItem>): List<LostFilmSearchItem>

    /**
     * Обогащает постеры постепенно: эмитит каждый item по мере готовности и
     * точечно пишет art-поля в Room. Холодный flow — обогащение отменяется
     * вместе с коллектором.
     *
     * Выбирать его нужно для ленты, которая рисуется до прихода арт-полей:
     * [enrichSummaries] ждёт все N резолвов, возвращает все N элементов и
     * сохраняет их полным upsert, который затирает isWatched.
     *
     * Карточки обновляются по мере готовности, а их порядок на странице
     * сохраняет коллектор. Описание может прийти после постера.
     */
    fun enrichProgressively(
        items: List<ReleaseSummary>,
        persistToCache: Boolean = true,
    ): Flow<ReleaseSummary>
}

private const val TAG = "TmdbEnrichmentService"

class TmdbEnrichmentServiceImpl @Inject constructor(
    private val tmdbResolver: TmdbPosterResolver,
    private val releaseDao: ReleaseDao,
) : TmdbEnrichmentService {
    private val tmdbEnrichCache = ConcurrentHashMap<String, Deferred<TmdbImageUrls?>>()

    override suspend fun enrichSummaries(
        items: List<ReleaseSummary>,
        persistToCache: Boolean,
    ): List<ReleaseSummary> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val needsEnrichment = items.filterNot { it.hasCompleteArt() }
        if (needsEnrichment.isEmpty()) {
            if (persistToCache) {
                upsertChangedSummaries(items.toSummaryEntities())
            }
            return items
        }

        val semaphore = Semaphore(SUMMARY_ENRICHMENT_CONCURRENCY)
        val enrichedItems = coroutineScope {
            items.map { item ->
                async {
                    if (item.hasCompleteArt()) {
                        return@async item
                    }
                    val tmdbUrlsDeferred = tmdbEnrichCache.getOrPut(item.detailsUrl) {
                        async {
                            semaphore.withPermit {
                                tmdbResolver.resolve(
                                    detailsUrl = item.detailsUrl,
                                    titleRu = item.titleRu,
                                    kind = item.kind,
                                    originalReleaseYear = item.originalReleaseYear,
                                )
                            }
                        }
                    }
                    val tmdbUrls = try {
                        tmdbUrlsDeferred.await()
                    } catch (exception: CancellationException) {
                        tmdbEnrichCache.remove(item.detailsUrl, tmdbUrlsDeferred)
                        throw exception
                    } catch (exception: Exception) {
                        tmdbEnrichCache.remove(item.detailsUrl, tmdbUrlsDeferred)
                        throw exception
                    }
                    tmdbEnrichCache.remove(item.detailsUrl, tmdbUrlsDeferred)
                    TmdbPosterEnricher.enrichSummary(item, tmdbUrls)
                }
            }.awaitAll()
        }

        if (persistToCache) {
            upsertChangedSummaries(enrichedItems.toSummaryEntities())
        }

        return enrichedItems
    }

    override suspend fun enrichSearchItems(items: List<LostFilmSearchItem>): List<LostFilmSearchItem> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val semaphore = Semaphore(6) // Formerly LostFilmConcurrencyLimits.SEARCH_ENRICHMENT_CONCURRENCY
        return coroutineScope {
            items.map { item ->
                async {
                    semaphore.withPermit {
                        val tmdbUrls = tmdbResolver.resolve(
                            detailsUrl = item.targetUrl,
                            titleRu = item.titleRu,
                            kind = item.kind,
                            originalReleaseYear = item.subtitle?.extractYear(),
                        )
                        item.copy(
                            posterUrl = tmdbUrls?.posterUrl?.ifBlank { item.posterUrl.orEmpty() }
                                ?.takeIf { it.isNotBlank() }
                                ?: item.posterUrl,
                            tmdbRating = tmdbUrls?.rating?.takeIf { it.isNotBlank() } ?: item.tmdbRating,
                        )
                    }
                }
            }.awaitAll()
        }
    }

    override fun enrichProgressively(
        items: List<ReleaseSummary>,
        persistToCache: Boolean,
    ): Flow<ReleaseSummary> = channelFlow {
        val pending = items.filterNot { it.hasCompleteArt() }
        if (pending.isEmpty()) {
            return@channelFlow
        }

        val artworkSlots = Semaphore(SUMMARY_ENRICHMENT_CONCURRENCY)
        for (item in pending) {
            // Enter the queue in display order even when the collector runs on IO.
            launch(start = CoroutineStart.UNDISPATCHED) {
                var current = item
                suspend fun publish(enriched: ReleaseSummary) {
                    if (enriched.hasSameArtworkAs(current)) return
                    if (persistToCache) releaseDao.updateSummaryArtwork(
                        detailsUrl = enriched.detailsUrl,
                        posterUrl = enriched.posterUrl,
                        backdropUrl = enriched.backdropUrl,
                        episodeOverviewRu = enriched.episodeOverviewRu,
                        episodeOverviewSource = enriched.episodeOverviewSource,
                        seriesOverviewRu = enriched.seriesOverviewRu,
                        movieOverviewRu = enriched.movieOverviewRu,
                        tmdbRating = enriched.tmdbRating,
                    )
                    current = enriched
                    send(enriched)
                }

                artworkSlots.acquire()
                var holdsArtworkSlot = true
                val urls = try {
                    tmdbResolver.resolveWithArtwork(
                        detailsUrl = item.detailsUrl,
                        titleRu = item.titleRu,
                        kind = item.kind,
                        originalReleaseYear = item.originalReleaseYear,
                        onArtwork = { artwork ->
                            // Text and ratings should not occupy a spot in the artwork queue.
                            if (holdsArtworkSlot) {
                                holdsArtworkSlot = false
                                artworkSlots.release()
                            }
                            if (artwork.posterUrl.isNotBlank() || artwork.backdropUrl.isNotBlank()) {
                                publish(TmdbPosterEnricher.enrichSummary(item, artwork))
                            }
                        },
                    )
                } finally {
                    if (holdsArtworkSlot) artworkSlots.release()
                }
                Log.d(
                    TAG,
                    "enrich ${item.detailsUrl}: poster=${!urls?.posterUrl.isNullOrBlank()} " +
                        "episodeRu=${urls?.episodeOverviewRu?.length ?: 0}",
                )
                if (urls != null) publish(TmdbPosterEnricher.enrichSummary(item, urls))
            }
        }
    }

    private fun ReleaseSummary.hasSameArtworkAs(other: ReleaseSummary): Boolean =
        posterUrl == other.posterUrl &&
            backdropUrl == other.backdropUrl &&
            episodeOverviewRu == other.episodeOverviewRu &&
            episodeOverviewSource == other.episodeOverviewSource &&
            seriesOverviewRu == other.seriesOverviewRu &&
            movieOverviewRu == other.movieOverviewRu &&
            tmdbRating == other.tmdbRating

    /**
     * Готовой считаем карточку, у которой есть и картинка, и то описание,
     * которое показывает лента. Раньше проверялись только постер с фоном,
     * поэтому эпизод с постером, но без описания выпадал из обработки
     * навсегда: описание могло прийти только в том же прогоне, что и постер,
     * и если запрос тогда не удался, карточка больше не обогащалась никогда.
     */
    private fun ReleaseSummary.hasCompleteArt(): Boolean {
        if (posterUrl.isBlank() || backdropUrl.isNullOrBlank()) return false
        return when {
            seasonNumber != null -> !episodeOverviewRu.isNullOrBlank()
            kind == ReleaseKind.MOVIE -> !movieOverviewRu.isNullOrBlank()
            else -> true
        }
    }

    private suspend fun upsertChangedSummaries(entities: List<ReleaseSummaryEntity>) {
        if (entities.isEmpty()) return
        releaseDao.upsertSummaries(entities)
    }
}
