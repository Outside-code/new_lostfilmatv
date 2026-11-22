package com.kraat.lostfilmnewtv.playback.season

import android.content.Context
import com.kraat.lostfilmnewtv.data.auth.AuthRepositoryContract
import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.data.model.SeriesGuideSeason
import com.kraat.lostfilmnewtv.data.repository.DetailsResult
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.platform.torrserve.TorrServeTorrentDownloader
import com.kraat.lostfilmnewtv.playback.PlaybackPreferencesStore
import com.kraat.lostfilmnewtv.ui.details.resolvePreferredTorrentRow
import com.kraat.lostfilmnewtv.ui.details.toTorrentRows
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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
        val quality = preferences.readDefaultQuality()
        val items = episodes.mapIndexed { index, episode ->
            progress("Готовим серию ${index + 1} из ${episodes.size}")
            val loaded = repository.loadDetails(episode.detailsUrl) as? DetailsResult.Success
                ?: throw IOException("Не удалось получить серию ${episode.episodeNumber}")
            val details = if (loaded.details.torrentLinks.isEmpty()) {
                (repository.refreshDetailsExtras(loaded.details) as? DetailsResult.Success)?.details ?: loaded.details
            } else loaded.details
            val torrent = resolvePreferredTorrentRow(quality, details.toTorrentRows())
                ?: throw IOException("Для серии ${episode.episodeNumber} пока нет раздачи")
            // A whole-season launch must not silently change the selected quality.
            if (!torrent.label.contains(quality.storageValue)) {
                throw IOException("${quality.storageValue}p для серии ${episode.episodeNumber} недоступно. Выберите качество в настройках.")
            }
            val seasonNumber = episode.seasonNumber.toString().padStart(2, '0')
            val episodeNumber = episode.episodeNumber.toString().padStart(2, '0')
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
            val result = client.newCall(Request.Builder().url(baseUrl + endpoint).apply { preferences.torrServeHeaders().forEach { (key, value) -> header(key, value) } }.post(body).build()).execute().use { response ->
                if (response.code == 401) throw IOException("Укажите логин и пароль TorrServe в настройках приложения")
                if (!response.isSuccessful) throw IOException("TorrServe не принял серию ${episode.episodeNumber}: HTTP ${response.code}")
                response.body?.string() ?: throw IOException("TorrServe вернул пустой ответ")
            }
            SeasonMediaItem(
                url = torrentStreamUrl(baseUrl, result), title = episodeTitle,
                detailsUrl = episode.detailsUrl, episodeId = details.playEpisodeId ?: episode.episodeId,
                episodeLabel = episodeLabel,
            )
        }
        SeasonPlaylist(items, posterUrl = poster, seriesTitle = title)
    }
}

internal fun torrentStreamUrl(baseUrl: String, response: String): String {
    val json = Json.parseToJsonElement(response).jsonObject
    val hash = json["hash"]?.jsonPrimitive?.content?.takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) }
        ?: throw IOException("TorrServe не вернул хеш торрента")
    val video = json["file_stats"]?.jsonArray.orEmpty().map { it.jsonObject }
        .filter { file -> file["path"]?.jsonPrimitive?.content.orEmpty().substringAfterLast('.').lowercase() in
            setOf("mkv", "mp4", "m4v", "avi", "ts", "webm", "mov") }
        .maxByOrNull { it["length"]?.jsonPrimitive?.longOrNull ?: 0L }
        ?: throw IOException("В торренте не найден видеофайл")
    val id = video["id"]?.jsonPrimitive?.intOrNull ?: throw IOException("TorrServe не вернул номер файла")
    val name = video["path"]!!.jsonPrimitive.content.substringAfterLast('/')
    return baseUrl.toHttpUrl().newBuilder().addPathSegment("stream").addPathSegment(name)
        .addQueryParameter("link", hash).addQueryParameter("index", id.toString())
        .addQueryParameter("play", "").build().toString()
}
