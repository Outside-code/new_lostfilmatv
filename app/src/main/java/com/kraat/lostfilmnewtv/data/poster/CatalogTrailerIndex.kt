package com.kraat.lostfilmnewtv.data.poster

import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class CatalogTrailerIndex internal constructor(read: () -> String) {
    private val videoIds by lazy {
        try {
            Json.decodeFromString<Map<String, String>>(read())
        } catch (_: FileNotFoundException) {
            emptyMap()
        } catch (error: IOException) {
            Log.w("CatalogTrailerIndex", "Unable to read bundled trailer index", error)
            emptyMap()
        } catch (error: SerializationException) {
            Log.w("CatalogTrailerIndex", "Unable to decode bundled trailer index", error)
            emptyMap()
        }
    }

    internal fun youtubeUrl(seriesUrl: String): String? {
        val key = CatalogPosterIndex.key(seriesUrl)?.takeIf { it.startsWith("/series/") } ?: return null
        return videoIds[key]?.takeIf(YOUTUBE_ID::matches)?.let { "https://www.youtube.com/watch?v=$it" }
    }

    companion object {
        internal const val FILE_NAME = "catalog-trailers.json"
        private val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}
