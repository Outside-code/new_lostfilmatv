package com.kraat.lostfilmnewtv.di

import android.content.Context
import com.kraat.lostfilmnewtv.data.auth.AuthRepository
import com.kraat.lostfilmnewtv.data.auth.AuthRepositoryContract
import com.kraat.lostfilmnewtv.data.auth.EncryptedSessionStore
import com.kraat.lostfilmnewtv.data.db.ReleaseDao
import com.kraat.lostfilmnewtv.data.db.TmdbPosterDao
import com.kraat.lostfilmnewtv.data.network.AuthBridgeClient
import com.kraat.lostfilmnewtv.data.network.KinoPoiskClient
import com.kraat.lostfilmnewtv.data.network.LostFilmSessionVerifier
import com.kraat.lostfilmnewtv.data.network.LostFilmHttpClient
import com.kraat.lostfilmnewtv.data.network.TmdbPosterClient
import com.kraat.lostfilmnewtv.data.parser.LostFilmDetailsParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmFavoriteSeriesParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmListParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmSearchParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmScheduleParser
import com.kraat.lostfilmnewtv.data.parser.LostFilmSeasonEpisodesParser
import com.kraat.lostfilmnewtv.data.poster.CatalogPosterIndex
import com.kraat.lostfilmnewtv.data.poster.CatalogTrailerIndex
import com.kraat.lostfilmnewtv.data.poster.TmdbPosterResolver
import com.kraat.lostfilmnewtv.data.poster.TmdbPosterResolverImpl
import com.kraat.lostfilmnewtv.data.poster.TmdbEnrichmentService
import com.kraat.lostfilmnewtv.data.poster.TmdbEnrichmentServiceImpl
import com.kraat.lostfilmnewtv.data.repository.FavoriteSeriesCache
import com.kraat.lostfilmnewtv.data.repository.FavoritesRepository
import com.kraat.lostfilmnewtv.data.repository.FavoritesRepositoryImpl
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepositoryImpl
import com.kraat.lostfilmnewtv.data.repository.ScheduleCache
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.ByteString.Companion.encodeUtf8

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        authBridgeClient: AuthBridgeClient,
        sessionStore: EncryptedSessionStore,
        sessionVerifier: LostFilmSessionVerifier,
    ): AuthRepositoryContract = AuthRepository(authBridgeClient, sessionStore, sessionVerifier)

    @Provides
    @Singleton
    fun provideLostFilmSessionVerifier(okHttpClient: OkHttpClient): LostFilmSessionVerifier =
        LostFilmSessionVerifier(okHttpClient)

    @Provides
    @Singleton
    fun provideCatalogPosterIndex(@ApplicationContext context: Context): CatalogPosterIndex =
        CatalogPosterIndex {
            context.assets.open(CatalogPosterIndex.FILE_NAME).bufferedReader().use { it.readText() }
        }

    @Provides
    @Singleton
    fun provideCatalogTrailerIndex(@ApplicationContext context: Context): CatalogTrailerIndex =
        CatalogTrailerIndex {
            context.assets.open(CatalogTrailerIndex.FILE_NAME).bufferedReader().use { it.readText() }
        }

    @Provides
    @Singleton
    fun provideTmdbPosterResolver(
        tmdbClient: TmdbPosterClient,
        tmdbDao: TmdbPosterDao,
        kinoPoiskClient: KinoPoiskClient,
        catalogPosterIndex: CatalogPosterIndex,
    ): TmdbPosterResolver = TmdbPosterResolverImpl(
        tmdbClient,
        tmdbDao,
        kinoPoiskClient,
        catalogPosterIndex = catalogPosterIndex,
    )

    @Provides
    @Singleton
    fun provideTmdbEnrichmentService(
        tmdbResolver: TmdbPosterResolver,
        releaseDao: ReleaseDao,
    ): TmdbEnrichmentService = TmdbEnrichmentServiceImpl(tmdbResolver, releaseDao)

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @AuthenticatedHttpClient httpClient: LostFilmHttpClient,
        releaseDao: ReleaseDao,
        tmdbEnrichmentService: TmdbEnrichmentService,
        sessionStore: EncryptedSessionStore,
        favoriteSeriesCache: FavoriteSeriesCache,
    ): FavoritesRepository = FavoritesRepositoryImpl(
        httpClient = httpClient,
        releaseDao = releaseDao,
        tmdbEnrichmentService = tmdbEnrichmentService,
        hasAuthenticatedSession = {
            val session = sessionStore.read()
            session != null && !sessionStore.isExpired()
        },
        favoriteSeriesCache = favoriteSeriesCache,
        activeSessionKey = {
            sessionStore.read()?.takeIf { !sessionStore.isExpired() }
                ?.toCookieString()?.encodeUtf8()?.sha256()?.hex()
        },
    )

    @Provides
    @Singleton
    fun provideLostFilmRepository(
        @AuthenticatedHttpClient httpClient: LostFilmHttpClient,
        @AnonymousHttpClient anonymousHttpClient: LostFilmHttpClient,
        releaseDao: ReleaseDao,
        tmdbPosterDao: TmdbPosterDao,
        tmdbResolver: TmdbPosterResolver,
        tmdbEnrichmentService: TmdbEnrichmentService,
        favoritesRepository: Lazy<FavoritesRepository>,
        sessionStore: EncryptedSessionStore,
        catalogPosterIndex: CatalogPosterIndex,
        scheduleCache: ScheduleCache,
        catalogTrailerIndex: CatalogTrailerIndex,
    ): LostFilmRepository = LostFilmRepositoryImpl(
        httpClient = httpClient,
        anonymousHttpClient = anonymousHttpClient,
        releaseDao = releaseDao,
        tmdbPosterDao = tmdbPosterDao,
        listParser = LostFilmListParser(),
        detailsParser = LostFilmDetailsParser(),
        favoriteSeriesParser = LostFilmFavoriteSeriesParser(),
        seasonEpisodesParser = LostFilmSeasonEpisodesParser(),
        searchParser = LostFilmSearchParser(),
        scheduleParser = LostFilmScheduleParser(),
        tmdbResolver = tmdbResolver,
        tmdbEnrichmentService = tmdbEnrichmentService,
        favoritesRepository = favoritesRepository,
        catalogPosterIndex = catalogPosterIndex,
        scheduleCache = scheduleCache,
        catalogTrailerIndex = catalogTrailerIndex,
        hasAuthenticatedSession = {
            val session = sessionStore.read()
            session != null && !sessionStore.isExpired()
        },
    )
}
