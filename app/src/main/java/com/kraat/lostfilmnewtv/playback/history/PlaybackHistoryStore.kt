package com.kraat.lostfilmnewtv.playback.history

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class PlaybackHistoryEntry(
    val detailsUrl: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val startedAt: Long,
    val completed: Boolean = false,
) {
    // The same release can be opened through different LostFilm mirrors.
    val key: String
        get() = detailsUrl.releaseKey()
}

private fun String.releaseKey(): String =
    toHttpUrlOrNull()?.encodedPath?.trimEnd('/')
        ?.takeIf { it.startsWith("/series/") || it.startsWith("/movies/") }
        ?: this

@Singleton
class PlaybackHistoryStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("playback_history", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val entries = state.asStateFlow()

    @Synchronized
    fun recordLaunch(entry: PlaybackHistoryEntry) {
        if (entry.detailsUrl.isBlank() || entry.title.isBlank()) return
        val key = entry.key
        val existing = state.value.firstOrNull { it.key == key }
        val latest = entry.copy(completed = entry.completed || existing?.completed == true)
        val next = listOf(latest) + state.value.filterNot { it.key == key }
        save(next.take(200))
    }

    @Synchronized
    fun markCompleted(detailsUrl: String) {
        val key = detailsUrl.releaseKey()
        val entry = state.value.firstOrNull { it.key == key } ?: return
        if (entry.completed) return
        save(state.value.map { if (it.key == key) it.copy(completed = true) else it })
    }

    private fun save(entries: List<PlaybackHistoryEntry>) {
        preferences.edit().putString("entries", Json.encodeToString(entries)).apply()
        state.value = entries
    }

    private fun read(): List<PlaybackHistoryEntry> {
        val saved = preferences.getString("entries", null) ?: return emptyList()
        return try {
            Json.decodeFromString(saved)
        } catch (_: SerializationException) {
            emptyList()
        }
    }
}
