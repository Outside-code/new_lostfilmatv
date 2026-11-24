package com.kraat.lostfilmnewtv.data.network

import com.kraat.lostfilmnewtv.data.network.TmdbRequestThrottle.Priority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TmdbRequestThrottleTest {
    @Test
    fun titleSearchOvertakesImageSelectionAndMetadataWithoutSkippingTheRequestInterval() = runTest {
        val throttle = TmdbRequestThrottle(300) { testScheduler.currentTime }
        val order = mutableListOf<Pair<String, Long>>()
        throttle.awaitTurn(Priority.ARTWORK)

        val description = launch {
            throttle.awaitTurn(Priority.METADATA)
            order += "description" to testScheduler.currentTime
        }
        runCurrent()
        val artwork = launch {
            throttle.awaitTurn(Priority.ARTWORK)
            order += "artwork" to testScheduler.currentTime
        }
        runCurrent()
        val search = launch {
            throttle.awaitTurn(Priority.TITLE_SEARCH)
            order += "title search" to testScheduler.currentTime
        }
        search.join()
        artwork.join()
        description.join()

        assertEquals(listOf("title search" to 300L, "artwork" to 600L, "description" to 900L), order)
    }

    @Test
    fun cancellingArtworkUnblocksMetadata() = runTest {
        val throttle = TmdbRequestThrottle(300) { testScheduler.currentTime }
        throttle.awaitTurn(Priority.ARTWORK)
        val artwork = launch { throttle.awaitTurn(Priority.ARTWORK) }
        runCurrent()
        artwork.cancelAndJoin()

        throttle.awaitTurn(Priority.METADATA)
        assertEquals(300L, testScheduler.currentTime)
    }
}
