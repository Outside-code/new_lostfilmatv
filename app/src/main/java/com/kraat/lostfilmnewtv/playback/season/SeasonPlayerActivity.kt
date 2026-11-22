package com.kraat.lostfilmnewtv.playback.season

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.playback.PlaybackPreferencesStore
import com.kraat.lostfilmnewtv.playback.WatchedMarkingMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SeasonPlayerActivity : ComponentActivity() {
    @Inject lateinit var repository: LostFilmRepository
    @Inject lateinit var preferences: PlaybackPreferencesStore
    private lateinit var view: PlayerView
    private lateinit var caption: TextView
    private var player: ExoPlayer? = null
    private var index = 0
    private var position = 0L
    private var resumePlayback = true
    private var resolution = ""
    private val completed = mutableSetOf<Int>()
    private val urls by lazy { intent.getStringArrayListExtra("urls").orEmpty() }
    private val titles by lazy { intent.getStringArrayListExtra("titles").orEmpty() }
    private val detailsUrls by lazy { intent.getStringArrayListExtra("detailsUrls").orEmpty() }
    private val episodeIds by lazy { intent.getStringArrayListExtra("episodeIds").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (urls.isEmpty()) { finish(); return }
        index = savedInstanceState?.getInt("index") ?: 0
        position = savedInstanceState?.getLong("position") ?: 0L
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        view = PlayerView(this).apply {
            tag = "season-player"
            useController = true
            setShowNextButton(true)
            setShowPreviousButton(true)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
        root.addView(view, FrameLayout.LayoutParams(-1, -1))
        caption = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 18f
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
            setPadding(32, 20, 32, 20)
            setBackgroundColor(0x90000000.toInt())
        }
        root.addView(caption, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { caption.visibility = it })
        setContentView(root)
        view.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        if (urls.isEmpty()) return
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(preferences.torrServeHeaders())
        val sources = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            androidx.media3.datasource.DefaultDataSource.Factory(this, http),
        )
        val current = ExoPlayer.Builder(this).setMediaSourceFactory(sources).build()
        player = current
        view.player = current
        current.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                resolution = ""
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) markCompleted(current.currentMediaItemIndex - 1)
                updateCaption()
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                resolution = "${videoSize.width}×${videoSize.height}"
                updateCaption()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) markCompleted(current.currentMediaItemIndex)
            }
            override fun onPlayerError(error: PlaybackException) {
                android.app.AlertDialog.Builder(this@SeasonPlayerActivity)
                    .setTitle("Не удалось воспроизвести серию")
                    .setMessage("Проверьте подключение к TorrServe. Можно повторить загрузку или перейти к следующей серии.")
                    .setPositiveButton("Повторить") { _, _ -> current.prepare(); current.play() }
                    .setNeutralButton("Следующая серия") { _, _ ->
                        if (current.hasNextMediaItem()) { current.seekToNextMediaItem(); current.prepare(); current.play() }
                    }
                    .setNegativeButton("Закрыть") { _, _ -> finish() }.show()
            }
        })
        current.setMediaItems(urls.mapIndexed { i, url ->
            MediaItem.Builder().setUri(url).setMediaId(detailsUrls.getOrElse(i) { i.toString() })
                .setMediaMetadata(MediaMetadata.Builder().setTitle(titles.getOrElse(i) { "Серия ${i + 1}" }).build()).build()
        }, index.coerceIn(urls.indices), position)
        current.repeatMode = Player.REPEAT_MODE_OFF
        current.shuffleModeEnabled = false
        current.prepare()
        current.playWhenReady = resumePlayback
        updateCaption()
    }

    private fun updateCaption() {
        val i = player?.currentMediaItemIndex ?: index
        caption.text = listOfNotNull(
            "${i + 1}/${urls.size} · ${titles.getOrElse(i) { "Серия ${i + 1}" }}",
            resolution.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    }

    private fun markCompleted(i: Int) {
        if (i !in urls.indices || !completed.add(i) || preferences.readWatchedMarkingMode() != WatchedMarkingMode.AUTO) return
        val id = episodeIds.getOrNull(i)?.takeIf { it.isNotBlank() } ?: return
        val url = detailsUrls.getOrNull(i)?.takeIf { it.isNotBlank() } ?: return
        lifecycleScope.launch(Dispatchers.IO) { runCatching { repository.setEpisodeWatched(url, id, true) } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("index", player?.currentMediaItemIndex ?: index)
        outState.putLong("position", player?.currentPosition ?: position)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        player?.let { index = it.currentMediaItemIndex; position = it.currentPosition; resumePlayback = it.playWhenReady; it.release() }
        player = null
        if (::view.isInitialized) view.player = null
        super.onStop()
    }

    companion object {
        fun createIntent(context: Context, playlist: SeasonPlaylist): Intent = Intent(context, SeasonPlayerActivity::class.java).apply {
            putStringArrayListExtra("urls", ArrayList(playlist.items.map { it.url }))
            putStringArrayListExtra("titles", ArrayList(playlist.items.map { it.title }))
            putStringArrayListExtra("detailsUrls", ArrayList(playlist.items.map { it.detailsUrl }))
            putStringArrayListExtra("episodeIds", ArrayList(playlist.items.map { it.episodeId.orEmpty() }))
        }
    }
}
