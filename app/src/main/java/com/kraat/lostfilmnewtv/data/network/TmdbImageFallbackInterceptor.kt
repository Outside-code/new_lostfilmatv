package com.kraat.lostfilmnewtv.data.network

import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import com.kraat.lostfilmnewtv.BuildConfig

/** Try our image cache first, then the legacy proxy and TMDB's CDN. Cached URLs keep working. */
class TmdbImageFallbackInterceptor(
    private val proxyPrefix: String = AUTH_BRIDGE_TMDB_IMAGE_PREFIX,
    private val directPrefix: String = TMDB_DIRECT_IMAGE_PREFIX,
    private val primaryPrefix: String = "${BuildConfig.AUTH_BRIDGE_BASE_URL}/api/tmdb/t/p/",
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data as? String ?: return chain.proceed(request)
        val sources = listOf(primaryPrefix, proxyPrefix, directPrefix).distinct()
        val matchedPrefix = sources.firstOrNull { url.startsWith(it) } ?: return chain.proceed(request)
        val imagePath = url.removePrefix(matchedPrefix)
        var result: ImageResult? = null
        for (source in sources) {
            result = chain.proceed(request.newBuilder().data(source + imagePath).build())
            if (result !is ErrorResult) return result
        }
        return checkNotNull(result)
    }

    companion object {
        const val AUTH_BRIDGE_TMDB_IMAGE_PREFIX = "https://auth.bazuka.pp.ua/api/tmdb/t/p/"
        const val TMDB_DIRECT_IMAGE_PREFIX = "https://image.tmdb.org/t/p/"
    }
}
