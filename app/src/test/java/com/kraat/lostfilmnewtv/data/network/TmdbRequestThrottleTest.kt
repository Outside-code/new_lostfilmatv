package com.kraat.lostfilmnewtv.data.network

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
    fun artworkOvertakesWaitingMetadataWithoutSkippingTheRequestInterval() = runTest {
        val throttle = TmdbRequestThrottle(300) { testScheduler.currentTime }
        val order = mutableListOf<Pair<String, Long>>()
        throttle.awaitTurn(artwork = true)

        val description = launch {
            throttle.awaitTurn(artwork = false)
            order += "description" to testScheduler.currentTime
        }
        runCurrent()
        val artwork = launch {
            throttle.awaitTurn(artwork = true)
            order += "artwork" to testScheduler.currentTime
        }
        artwork.join()
        description.join()

        assertEquals(listOf("artwork" to 300L, "description" to 600L), order)
    }

    @Test
    fun cancellingArtworkUnblocksMetadata() = runTest {
        val throttle = TmdbRequestThrottle(300) { testScheduler.currentTime }
        throttle.awaitTurn(artwork = true)
        val artwork = launch { throttle.awaitTurn(artwork = true) }
        runCurrent()
        artwork.cancelAndJoin()

        throttle.awaitTurn(artwork = false)
        assertEquals(300L, testScheduler.currentTime)
    }
}
