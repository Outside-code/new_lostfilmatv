package com.kraat.lostfilmnewtv.data.network

import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import com.kraat.lostfilmnewtv.BuildConfig
import kotlinx.coroutines.withTimeoutOrNull

/** Download from TMDB first, with the shared image proxy as a fallback. */
class TmdbImageFallbackInterceptor(
    private val proxyPrefix: String = AUTH_BRIDGE_TMDB_IMAGE_PREFIX,
    private val directPrefix: String = TMDB_DIRECT_IMAGE_PREFIX,
    private val previousCachePrefix: String = PREVIOUS_CACHE_PREFIX,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data as? String ?: return chain.proceed(request)
        val matchedPrefix = listOf(directPrefix, proxyPrefix, previousCachePrefix)
            .firstOrNull { url.startsWith(it) } ?: return chain.proceed(request)
        val imagePath = url.removePrefix(matchedPrefix)
        // Reuse downloaded posters from previous versions independently of the network host.
        val cachedRequest = request.newBuilder().diskCacheKey(request.diskCacheKey ?: previousCachePrefix + imagePath)
        val directRequest = cachedRequest.data(directPrefix + imagePath).build()
        if (proxyPrefix == directPrefix) return chain.proceed(directRequest)
        val directResult = when (imagePath.substringBefore('/')) {
            "w342", "w500" -> withTimeoutOrNull(CDN_THUMBNAIL_TIMEOUT_MS) { chain.proceed(directRequest) }
            else -> chain.proceed(directRequest)
        }
        if (directResult != null && directResult !is ErrorResult) return directResult
        return chain.proceed(cachedRequest.data(proxyPrefix + imagePath).build())
    }

    companion object {
        private const val CDN_THUMBNAIL_TIMEOUT_MS = 2_500L
        const val AUTH_BRIDGE_TMDB_IMAGE_PREFIX = "https://auth.bazuka.pp.ua/api/tmdb/t/p/"
        const val TMDB_DIRECT_IMAGE_PREFIX = "https://image.tmdb.org/t/p/"
        private val PREVIOUS_CACHE_PREFIX = "${BuildConfig.AUTH_BRIDGE_BASE_URL}/api/tmdb/t/p/"
        private val thumbnailPrefixes = listOf(
            TMDB_DIRECT_IMAGE_PREFIX,
            AUTH_BRIDGE_TMDB_IMAGE_PREFIX,
            PREVIOUS_CACHE_PREFIX,
        )

        internal fun thumbnailUrl(url: String, targetWidthPx: Int, displayWidthPx: Int): String {
            if (targetWidthPx > 500) return url
            val size = if (displayWidthPx <= 1920 && targetWidthPx <= 342) "w342" else "w500"
            val prefix = thumbnailPrefixes.firstOrNull { url.startsWith(it + "w780/") } ?: return url
            return prefix + size + "/" + url.removePrefix(prefix + "w780/")
        }
    }
}
