package com.kraat.lostfilmnewtv.data.repository

import android.content.Context
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FavoriteSeriesCache @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("favorite_series", Context.MODE_PRIVATE)

    @Serializable
    data class Snapshot(
        val sessionKey: String,
        val fetchedAt: Long,
        val items: List<ReleaseSummary>,
    )

    fun read(sessionKey: String): Snapshot? {
        val saved = preferences.getString("snapshot", null) ?: return null
        return try {
            Json.decodeFromString<Snapshot>(saved).takeIf { it.sessionKey == sessionKey }
        } catch (_: SerializationException) {
            null
        }
    }

    fun write(snapshot: Snapshot) {
        preferences.edit().putString("snapshot", Json.encodeToString(snapshot)).apply()
    }
}
