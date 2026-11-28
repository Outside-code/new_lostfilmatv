package com.kraat.lostfilmnewtv.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryEntry
import com.kraat.lostfilmnewtv.playback.history.episodeAndPositionLabel
import com.kraat.lostfilmnewtv.playback.history.latestUnfinishedSeriesEpisode
import com.kraat.lostfilmnewtv.playback.season.SeasonPlayerActivity
import com.kraat.lostfilmnewtv.playback.season.openSeriesTrailer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SeriesGuideRoute(
    onOpenDetails: (String) -> Unit,
    viewModel: SeriesGuideViewModel? = null,
    onOpenAuth: () -> Unit = {},
    onFavoriteChanged: (String, Boolean) -> Unit = { _, _ -> },
    playbackHistory: List<PlaybackHistoryEntry> = emptyList(),
    isResumingEpisode: Boolean = false,
    onResumeEpisode: (PlaybackHistoryEntry) -> Unit = {},
) {
    val routeViewModel: SeriesGuideViewModel = viewModel ?: hiltViewModel()
    val state = routeViewModel.uiState.collectAsStateWithLifecycle()
    val resumeEpisode = playbackHistory.latestUnfinishedSeriesEpisode(state.value.seriesUrl)

    LaunchedEffect(Unit) { routeViewModel.onStart() }
    val context = LocalContext.current
    LaunchedEffect(state.value.seasonPlaylist) {
        state.value.seasonPlaylist?.let { playlist ->
            context.startActivity(SeasonPlayerActivity.createIntent(context, playlist))
            routeViewModel.consumePlaylist()
        }
    }
    LaunchedEffect(state.value.requiresLogin) {
        if (state.value.requiresLogin) { routeViewModel.consumeLoginRequest(); onOpenAuth() }
    }
    LaunchedEffect(state.value.favoriteContentVersion) {
        if (state.value.favoriteContentVersion > 0) {
            onFavoriteChanged(state.value.seriesUrl, state.value.isFavorite == true)
        }
    }

    SeriesGuideScreen(
        state = state.value,
        onRetry = routeViewModel::onRetry,
        onSeasonSelected = routeViewModel::onSeasonSelected,
        onEpisodeClick = onOpenDetails,
        onPlaySeason = routeViewModel::onPlaySeasonClick,
        onFavoriteClick = routeViewModel::onFavoriteClick,
        resumeEpisodeLabel = resumeEpisode?.episodeAndPositionLabel(),
        isResumingEpisode = isResumingEpisode,
        onResumeEpisode = { resumeEpisode?.let(onResumeEpisode) },
        onTrailerClick = {
            val guide = state.value
            openSeriesTrailer(context, guide.title, guide.trailerUrl)
        },
    )
}
