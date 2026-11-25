package com.kraat.lostfilmnewtv.playback.season

import android.content.Context
import com.kraat.lostfilmnewtv.data.auth.AuthRepositoryContract
import com.kraat.lostfilmnewtv.data.model.ReleaseDetails
import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.data.model.SeriesGuideSeason
import com.kraat.lostfilmnewtv.data.repository.DetailsResult
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.data.repository.SeriesGuideResult
import com.kraat.lostfilmnewtv.platform.torrserve.TorrServeTorrentDownloader
import com.kraat.lostfilmnewtv.playback.PlaybackPreferencesStore
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryEntry
import com.kraat.lostfilmnewtv.playback.history.nextUnwatchedEpisode
import com.kraat.lostfilmnewtv.ui.details.DetailsTorrentRowUiModel
import com.kraat.lostfilmnewtv.ui.details.resolvePreferredTorrentRow
import com.kraat.lostfilmnewtv.ui.details.toTorrentRows
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl

class SeasonLoginRequired : IOException("Войдите в LostFilm для просмотра сезона")
data class SeasonMediaItem(
    val url: String,
    val title: String,
    val detailsUrl: String,
    val episodeId: String?,
    val episodeLabel: String = "",
)

data class SeasonPlaylist(
    val items: List<SeasonMediaItem>,
    val posterUrl: String = "",
    val seriesTitle: String = "",
)

fun interface SeasonPlaybackPreparer {
    suspend fun prepare(season: SeriesGuideSeason, title: String, poster: String, progress: (String) -> Unit): SeasonPlaylist
}

internal fun SeriesGuideSeason.playbackEpisodes(): List<SeriesGuideEpisode> = episodes
    .filter { it.isAvailable && it.episodeNumber != 999 }
    .distinctBy { it.episodeNumber }
    .sortedBy { it.episodeNumber }

@Singleton
class SeasonPlaybackCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LostFilmRepository,
    private val authRepository: AuthRepositoryContract,
    private val preferences: PlaybackPreferencesStore,
    private val downloader: TorrServeTorrentDownloader,
    private val client: OkHttpClient,
) : SeasonPlaybackPreparer {
    override suspend fun prepare(
        season: SeriesGuideSeason, title: String, poster: String, progress: (String) -> Unit,
    ): SeasonPlaylist = withContext(Dispatchers.IO) {
        if (!authRepository.getAuthState().isAuthenticated) throw SeasonLoginRequired()
        val episodes = season.playbackEpisodes()
        if (episodes.isEmpty()) throw IOException("В этом сезоне ещё нет доступных серий")
        val baseUrl = preferences.readTorrServeBaseUrl().trimEnd('/')
        val headers = preferences.torrServeHeaders()
        val quality = preferences.readDefaultQuality()
        val items = episodes.mapIndexed { index, episode ->
            progress("Готовим серию ${index + 1} из ${episodes.size}")
            val details = loadPlayableDetails(episode)
            val torrent = resolvePreferredTorrentRow(quality, details.toTorrentRows())
                ?: throw IOException("Для серии ${episode.episodeNumber} пока нет раздачи")
            // A whole-season launch must not silently change the selected quality.
            if (!torrent.label.contains(quality.storageValue)) {
                throw IOException("${quality.storageValue}p для серии ${episode.episodeNumber} недоступно. Выберите качество в настройках.")
            }
            prepareItem(details, torrent, title, poster, baseUrl, headers, episode.episodeId)
        }
        SeasonPlaylist(items, posterUrl = poster, seriesTitle = title)
    }

    suspend fun prepareEpisode(
        details: ReleaseDetails,
        torrent: DetailsTorrentRowUiModel,
    ): SeasonPlaylist = withContext(Dispatchers.IO) {
        val item = prepareItem(
            details = details,
            torrent = torrent,
            title = details.titleRu,
            poster = details.posterUrl,
            baseUrl = preferences.readTorrServeBaseUrl().trimEnd('/'),
            headers = preferences.torrServeHeaders(),
        )
        SeasonPlaylist(listOf(item), posterUrl = details.posterUrl, seriesTitle = details.titleRu)
    }

    suspend fun prepareNextEpisode(
        previousDetailsUrl: String,
        history: List<PlaybackHistoryEntry>,
    ): SeasonPlaylist? = withContext(Dispatchers.IO) {
        val guide = when (val result = repository.loadSeriesGuide(previousDetailsUrl)) {
            is SeriesGuideResult.Success -> result.guide
            is SeriesGuideResult.Error -> throw IOException("Не удалось загрузить список серий")
        }
        val next = guide.nextUnwatchedEpisode(previousDetailsUrl, history) ?: return@withContext null
        val details = loadPlayableDetails(next)
        val torrent = resolvePreferredTorrentRow(preferences.readDefaultQuality(), details.toTorrentRows())
            ?: throw IOException("Для следующей серии пока нет раздачи")
        prepareEpisode(details, torrent)
    }

    private suspend fun loadPlayableDetails(episode: SeriesGuideEpisode): ReleaseDetails {
        val loaded = repository.loadDetails(episode.detailsUrl) as? DetailsResult.Success
            ?: throw IOException("Не удалось получить серию ${episode.episodeNumber}")
        return if (loaded.details.torrentLinks.isEmpty()) {
            (repository.refreshDetailsExtras(loaded.details) as? DetailsResult.Success)?.details ?: loaded.details
        } else loaded.details
    }

    private suspend fun prepareItem(
        details: ReleaseDetails,
        torrent: DetailsTorrentRowUiModel,
        title: String,
        poster: String,
        baseUrl: String,
        headers: Map<String, String>,
        fallbackEpisodeId: String? = null,
    ): SeasonMediaItem {
        val seasonNumber = (details.seasonNumber ?: 0).toString().padStart(2, '0')
        val episodeNumber = (details.episodeNumber ?: 0).toString().padStart(2, '0')
        val episodeLabel = "S${seasonNumber}E$episodeNumber"
        val episodeTitle = "$title · $episodeLabel · ${torrent.label}"
        val prepared = downloader.prepare(context, torrent.url) ?: throw IOException("Не удалось скачать торрент")
        val body = if (prepared.startsWith("content://")) {
            val bytes = context.contentResolver.openInputStream(android.net.Uri.parse(prepared))?.use { it.readBytes() }
                ?: throw IOException("Не удалось прочитать торрент")
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file1", "episode.torrent", bytes.toRequestBody("application/x-bittorrent".toMediaType()))
                .addFormDataPart("title", episodeTitle).addFormDataPart("poster", poster)
                .addFormDataPart("category", "tv").addFormDataPart("save", "true").build()
        } else {
            buildJsonObject {
                put("action", "add"); put("link", prepared); put("title", episodeTitle)
                put("poster", poster); put("category", "tv"); put("save_to_db", true)
            }.toString().toRequestBody("application/json".toMediaType())
        }
        val endpoint = if (prepared.startsWith("content://")) "/torrent/upload" else "/torrents"
        val result = postTorrServe(baseUrl + endpoint, body, headers)
        val streamUrl = awaitTorrentStreamUrl(baseUrl, result) { hash ->
            val request = buildJsonObject {
                put("action", "get")
                put("hash", hash)
            }.toString().toRequestBody("application/json".toMediaType())
            postTorrServe("$baseUrl/torrents", request, headers)
        }
        return SeasonMediaItem(
            url = streamUrl, title = episodeTitle,
            detailsUrl = details.detailsUrl, episodeId = details.playEpisodeId ?: fallbackEpisodeId,
            episodeLabel = episodeLabel,
        )
    }

    private fun postTorrServe(url: String, body: okhttp3.RequestBody, headers: Map<String, String>): String {
        val request = Request.Builder().url(url).post(body)
            .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        return client.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                val message = if (headers.isEmpty()) "Укажите логин и пароль TorrServe в настройках приложения"
                else "TorrServe отклонил сохранённый логин или пароль"
                throw IOException(message)
            }
            if (!response.isSuccessful) throw IOException("Не удалось получить поток TorrServe: HTTP ${response.code}")
            response.body?.string() ?: throw IOException("TorrServe вернул пустой ответ")
        }
    }
}

internal fun torrentStreamUrl(baseUrl: String, response: String): String =
    parseTorrentStatus(response).streamUrl(baseUrl) ?: throw IOException("В торренте не найден видеофайл")

internal suspend fun awaitTorrentStreamUrl(
    baseUrl: String,
    response: String,
    loadStatus: suspend (String) -> String,
): String {
    val initial = parseTorrentStatus(response)
    initial.streamUrl(baseUrl)?.let { return it }
    val hash = initial.torrentHash()
    repeat(3) { attempt ->
        delay(500L * (attempt + 1))
        parseTorrentStatus(loadStatus(hash)).streamUrl(baseUrl)?.let { return it }
    }
    throw IOException("В торренте не найден видеофайл")
}

private fun parseTorrentStatus(response: String): JsonObject =
    when (val body = Json.parseToJsonElement(response)) {
        is JsonObject -> body
        is JsonArray -> body.singleOrNull() as? JsonObject
        else -> null
    } ?: throw IOException("TorrServe не вернул данные торрента")

private fun JsonObject.torrentHash(): String =
    this["hash"]?.jsonPrimitive?.content?.takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) }
        ?: throw IOException("TorrServe не вернул хеш торрента")

private fun JsonObject.streamUrl(baseUrl: String): String? {
    val hash = torrentHash()
    val video = this["file_stats"]?.jsonArray.orEmpty().map { it.jsonObject }
        .filter { file -> file["path"]?.jsonPrimitive?.content.orEmpty().substringAfterLast('.').lowercase() in
            setOf("mkv", "mp4", "m4v", "avi", "ts", "webm", "mov") }
        .maxByOrNull { it["length"]?.jsonPrimitive?.longOrNull ?: 0L } ?: return null
    val id = video["id"]?.jsonPrimitive?.intOrNull ?: throw IOException("TorrServe не вернул номер файла")
    val name = video["path"]!!.jsonPrimitive.content.substringAfterLast('/')
    return baseUrl.toHttpUrl().newBuilder().addPathSegment("stream").addPathSegment(name)
        .addQueryParameter("link", hash).addQueryParameter("index", id.toString())
        .addQueryParameter("play", "").build().toString()
}
