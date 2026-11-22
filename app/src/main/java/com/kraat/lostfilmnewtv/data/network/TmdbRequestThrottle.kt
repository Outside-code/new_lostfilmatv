package com.kraat.lostfilmnewtv.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class TmdbRequestThrottle(
    private val intervalMs: Long,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val mutex = Mutex()
    private val artworkWaiters = MutableStateFlow(0)
    private var lastRequestAt: Long? = null

    suspend fun awaitTurn(artwork: Boolean) {
        if (artwork) artworkWaiters.update { it + 1 }
        try {
            while (true) {
                if (!artwork) artworkWaiters.first { it == 0 }
                mutex.withLock {
                    if (!artwork && artworkWaiters.value > 0) return@withLock
                    lastRequestAt?.let { last -> delay(intervalMs - (nowMs() - last)) }
                    // A picture lookup can arrive while a metadata request waits for its slot.
                    if (!artwork && artworkWaiters.value > 0) return@withLock
                    lastRequestAt = nowMs()
                    return
                }
            }
        } finally {
            if (artwork) artworkWaiters.update { it - 1 }
        }
    }
}
