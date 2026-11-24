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
    enum class Priority { METADATA, ARTWORK, TITLE_SEARCH }

    private val mutex = Mutex()
    private val waiters = MutableStateFlow(List(Priority.entries.size) { 0 })
    private var lastRequestAt: Long? = null

    suspend fun awaitTurn(priority: Priority) {
        val rank = priority.ordinal
        fun hasHigherWaiter(counts: List<Int>) =
            (rank + 1 until counts.size).any { counts[it] > 0 }

        changeWaiters(rank, 1)
        try {
            while (true) {
                waiters.first { !hasHigherWaiter(it) }
                mutex.withLock {
                    if (hasHigherWaiter(waiters.value)) return@withLock
                    lastRequestAt?.let { last -> delay(intervalMs - (nowMs() - last)) }
                    // A higher-priority lookup can arrive while this request waits for its slot.
                    if (hasHigherWaiter(waiters.value)) return@withLock
                    lastRequestAt = nowMs()
                    return
                }
            }
        } finally {
            changeWaiters(rank, -1)
        }
    }

    private fun changeWaiters(rank: Int, delta: Int) {
        waiters.update { current ->
            current.mapIndexed { index, count -> if (index == rank) count + delta else count }
        }
    }
}
