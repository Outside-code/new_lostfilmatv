package com.kraat.lostfilmnewtv.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.kraat.lostfilmnewtv.playback.season.openSeriesTrailer

@Composable
fun SeriesOverviewRoute() {
    val viewModel: SeriesOverviewViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onStart() }

    SeriesOverviewScreen(
        state = state.value,
        onRetry = viewModel::onRetry,
        onTrailerClick = {
            state.value.overview?.let { overview ->
                openSeriesTrailer(context, overview.titleRu, overview.trailerUrl)
            }
        },
    )
}
