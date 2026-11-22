package com.kraat.lostfilmnewtv.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.kraat.lostfilmnewtv.playback.season.SeasonPlayerActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SeriesGuideRoute(
    onOpenDetails: (String) -> Unit,
    viewModel: SeriesGuideViewModel? = null,
    onOpenAuth: () -> Unit = {},
    onFavoriteChanged: (String, Boolean) -> Unit = { _, _ -> },
) {
    val routeViewModel: SeriesGuideViewModel = viewModel ?: hiltViewModel()
    val state = routeViewModel.uiState.collectAsStateWithLifecycle()

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
    )
}
