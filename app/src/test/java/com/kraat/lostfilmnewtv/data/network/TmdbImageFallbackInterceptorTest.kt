package com.kraat.lostfilmnewtv.data.network

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TmdbImageFallbackInterceptorTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun thumbnailUrl_usesSmallerTmdbPosterOnlyWhenItCoversTheCardWidth() {
        val poster = "https://image.tmdb.org/t/p/w780/poster.jpg"
        val backdrop = "https://image.tmdb.org/t/p/original/backdrop.jpg"
        val other = "https://example.org/t/p/w780/poster.jpg"

        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", TmdbImageFallbackInterceptor.thumbnailUrl(poster, 239, 1920))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", TmdbImageFallbackInterceptor.thumbnailUrl(poster, 477, 3840))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", TmdbImageFallbackInterceptor.thumbnailUrl(poster, 239, 3840))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", TmdbImageFallbackInterceptor.thumbnailUrl(poster, 350, 1920))
        assertEquals(poster, TmdbImageFallbackInterceptor.thumbnailUrl(poster, 640, 3840))
        assertEquals(backdrop, TmdbImageFallbackInterceptor.thumbnailUrl(backdrop, 239, 1920))
        assertEquals(other, TmdbImageFallbackInterceptor.thumbnailUrl(other, 239, 1920))
    }

    @Test
    fun intercept_downloadsDirectlyForNewAndSavedImageUrls() = runTest {
        val previous = "https://lost.example.test/api/tmdb/t/p/"
        val interceptor = TmdbImageFallbackInterceptor(previousCachePrefix = previous)
        val direct = TmdbImageFallbackInterceptor.TMDB_DIRECT_IMAGE_PREFIX
        val proxy = TmdbImageFallbackInterceptor.AUTH_BRIDGE_TMDB_IMAGE_PREFIX
        val path = "original/backdrop.jpg"

        for (prefix in listOf(direct, proxy, previous)) {
            val request = ImageRequest.Builder(context).data(prefix + path).build()
            val urls = mutableListOf<String>()
            val chain = FakeChain(request) { req ->
                urls += req.data.toString()
                assertEquals(previous + path, req.diskCacheKey)
                SuccessResult(ColorDrawable(Color.BLUE), req, coil.decode.DataSource.NETWORK)
            }

            assertTrue(interceptor.intercept(chain) is SuccessResult)
            assertEquals(listOf(direct + path), urls)
        }
    }

    @Test
    fun intercept_triesSharedProxyWhenDirectCdnFails() = runTest {
        val previous = "https://lost.example.test/api/tmdb/t/p/"
        val interceptor = TmdbImageFallbackInterceptor(previousCachePrefix = previous)
        val direct = TmdbImageFallbackInterceptor.TMDB_DIRECT_IMAGE_PREFIX
        val proxy = TmdbImageFallbackInterceptor.AUTH_BRIDGE_TMDB_IMAGE_PREFIX
        val path = "w342/poster.jpg"
        val request = ImageRequest.Builder(context).data(previous + path).build()
        val urls = mutableListOf<String>()
        val chain = FakeChain(request) { req ->
            val url = req.data.toString()
            urls += url
            assertEquals(previous + path, req.diskCacheKey)
            if (url == proxy + path) {
                SuccessResult(ColorDrawable(Color.BLUE), req, coil.decode.DataSource.NETWORK)
            } else {
                ErrorResult(null, req, java.io.IOException("CDN unavailable"))
            }
        }

        assertTrue(interceptor.intercept(chain) is SuccessResult)
        assertEquals(listOf(direct + path, proxy + path), urls)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun intercept_movesToSharedProxyWhenSmallCdnImageStalls() = runTest {
        val direct = TmdbImageFallbackInterceptor.TMDB_DIRECT_IMAGE_PREFIX
        val proxy = TmdbImageFallbackInterceptor.AUTH_BRIDGE_TMDB_IMAGE_PREFIX
        val path = "w500/poster.jpg"
        val request = ImageRequest.Builder(context).data(direct + path).build()
        val urls = mutableListOf<String>()
        val chain = FakeChain(request) { req ->
            urls += req.data.toString()
            if (req.data == direct + path) awaitCancellation()
            SuccessResult(ColorDrawable(Color.BLUE), req, coil.decode.DataSource.NETWORK)
        }

        assertTrue(TmdbImageFallbackInterceptor().intercept(chain) is SuccessResult)
        assertEquals(listOf(direct + path, proxy + path), urls)
        assertEquals(2_500L, testScheduler.currentTime)
    }

    @Test
    fun intercept_leavesOtherImageUrlsAlone() = runTest {
        val url = "https://kinopoiskapiunofficial.tech/images/poster.jpg"
        val request = ImageRequest.Builder(context).data(url).build()
        val urls = mutableListOf<String>()
        val chain = FakeChain(request) { req ->
            urls += req.data.toString()
            ErrorResult(null, req, java.io.IOException("Not found"))
        }

        assertTrue(TmdbImageFallbackInterceptor().intercept(chain) is ErrorResult)
        assertEquals(listOf(url), urls)
    }

    @Test
    fun intercept_stopsAfterTwoSourcesWhenBothFail() = runTest {
        val direct = TmdbImageFallbackInterceptor.TMDB_DIRECT_IMAGE_PREFIX
        val request = ImageRequest.Builder(context).data(direct + "w342/missing.jpg").build()
        val chain = FakeChain(request) { req -> ErrorResult(null, req, java.io.IOException("Offline")) }
        assertTrue(TmdbImageFallbackInterceptor().intercept(chain) is ErrorResult)
        assertEquals(2, chain.callCount)
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private class FakeChain(
        override val request: ImageRequest,
        override val size: Size = Size.ORIGINAL,
        private val handler: suspend (ImageRequest) -> ImageResult,
    ) : Interceptor.Chain {
        var callCount = 0

        override suspend fun proceed(request: ImageRequest): ImageResult {
            callCount++
            return handler(request)
        }

        override fun withSize(size: Size): Interceptor.Chain = this

        override fun withRequest(request: ImageRequest): Interceptor.Chain =
            FakeChain(request, size, handler).also { it.callCount = this.callCount }
    }
}
