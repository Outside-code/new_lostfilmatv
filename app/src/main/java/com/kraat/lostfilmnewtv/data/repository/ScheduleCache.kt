package com.kraat.lostfilmnewtv.data.repository

import android.content.Context
import com.kraat.lostfilmnewtv.data.model.ScheduleMonth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DateTimeException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScheduleCache @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)

    @Serializable
    data class Snapshot(
        val server: String,
        val day: Long,
        val fetchedAt: Long,
        val schedule: ScheduleMonth,
    )

    fun read(server: String, today: LocalDate): Snapshot? =
        readSnapshot()?.takeIf { it.server == server && it.day == today.toEpochDay() }

    @Synchronized
    fun write(snapshot: Snapshot) {
        val previous = readSnapshot()
        if (previous?.server == snapshot.server && previous.day == snapshot.day &&
            previous.fetchedAt > snapshot.fetchedAt
        ) return
        preferences.edit().putString("snapshot", Json.encodeToString(snapshot)).apply()
    }

    private fun readSnapshot(): Snapshot? {
        val json = preferences.getString("snapshot", null) ?: return null
        return try {
            Json.decodeFromString<Snapshot>(json)
        } catch (_: SerializationException) {
            null
        } catch (_: DateTimeException) {
            null
        }
    }
}
