package com.kraat.lostfilmnewtv.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryEntry
import com.kraat.lostfilmnewtv.playback.history.episodeAndPositionLabel
import com.kraat.lostfilmnewtv.playback.history.latestUnfinishedSeriesEpisode
import com.kraat.lostfilmnewtv.playback.season.openSeriesTrailer

@Composable
fun SeriesOverviewRoute(
    playbackHistory: List<PlaybackHistoryEntry> = emptyList(),
    isResumingEpisode: Boolean = false,
    onResumeEpisode: (PlaybackHistoryEntry) -> Unit = {},
) {
    val viewModel: SeriesOverviewViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val resumeEpisode = state.value.overview?.seriesUrl?.let { seriesUrl ->
        playbackHistory.latestUnfinishedSeriesEpisode(seriesUrl)
    }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onStart() }

    SeriesOverviewScreen(
        state = state.value,
        onRetry = viewModel::onRetry,
        resumeEpisodeLabel = resumeEpisode?.episodeAndPositionLabel(),
        isResumingEpisode = isResumingEpisode,
        onResumeEpisode = { resumeEpisode?.let(onResumeEpisode) },
        onTrailerClick = {
            state.value.overview?.let { overview ->
                openSeriesTrailer(context, overview.titleRu, overview.trailerUrl)
            }
        },
    )
}
