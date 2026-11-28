package com.kraat.lostfilmnewtv.playback

import com.kraat.lostfilmnewtv.tvchannel.AndroidTvChannelMode
import com.kraat.lostfilmnewtv.ui.settings.normalizeLostFilmHost
import com.kraat.lostfilmnewtv.ui.settings.normalizeTorrServeBaseUrl
import com.kraat.lostfilmnewtv.updates.UpdateCheckMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
internal data class SettingsBackup(
    val version: Int,
    val format: String,
    val quality: String,
    val updateMode: String,
    val channelMode: String,
    val watchedMode: String,
    val favoriteReleasesEnabled: Boolean,
    val favoriteSeriesEnabled: Boolean,
    val moviesEnabled: Boolean,
    val seriesEnabled: Boolean,
    val menuLabelsEnabled: Boolean,
    val torrServeUrl: String,
    val lostFilmUrl: String,
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val VERSION = 1
        const val FORMAT = "LostFilm.TV settings"
        private val json = Json { prettyPrint = true }

        fun decode(content: String): SettingsBackup {
            val backup = json.decodeFromString(serializer(), content)
            require(backup.format == FORMAT && backup.version == VERSION)
            require(PlaybackQualityPreference.entries.any { it.storageValue == backup.quality })
            require(UpdateCheckMode.entries.any { it.storageValue == backup.updateMode })
            require(AndroidTvChannelMode.entries.any { it.storageValue == backup.channelMode })
            require(WatchedMarkingMode.entries.any { it.storageValue == backup.watchedMode })
            val torrServeUrl = requireNotNull(normalizeTorrServeBaseUrl(backup.torrServeUrl))
            val lostFilmUrl = requireNotNull(normalizeLostFilmHost(backup.lostFilmUrl))
            require(torrServeUrl == torrServeUrl.withoutUserInfo())
            require(lostFilmUrl == lostFilmUrl.withoutUserInfo())
            return backup.copy(torrServeUrl = torrServeUrl, lostFilmUrl = lostFilmUrl)
        }
    }
}

internal fun String.withoutUserInfo(): String =
    toHttpUrlOrNull()?.let { url ->
        if (url.username.isEmpty() && url.password.isEmpty()) this
        else url.newBuilder().username("").password("").build().toString().trimEnd('/')
    } ?: this
