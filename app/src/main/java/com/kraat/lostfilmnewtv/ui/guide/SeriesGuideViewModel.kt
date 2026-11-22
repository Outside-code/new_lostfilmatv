package com.kraat.lostfilmnewtv.ui.guide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kraat.lostfilmnewtv.data.repository.LostFilmRepository
import com.kraat.lostfilmnewtv.data.repository.FavoritesRepository
import com.kraat.lostfilmnewtv.data.model.FavoriteMutationResult
import com.kraat.lostfilmnewtv.playback.season.SeasonPlaybackPreparer
import com.kraat.lostfilmnewtv.playback.season.SeasonLoginRequired
import kotlinx.coroutines.CancellationException
import com.kraat.lostfilmnewtv.data.repository.SeriesGuideResult
import com.kraat.lostfilmnewtv.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SeriesGuideViewModel @Inject constructor(
    private val repository: LostFilmRepository,
    private val savedStateHandle: SavedStateHandle,
    private val favoritesRepository: FavoritesRepository,
    private val seasonPlaybackPreparer: SeasonPlaybackPreparer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesGuideUiState())
    val uiState: StateFlow<SeriesGuideUiState> = _uiState.asStateFlow()

    private var started = false
    private var loadRequestToken = 0
    private var loadJob: Job? = null

    fun onStart() {
        if (started) return
        started = true
        loadGuide()
    }

    fun onRetry() = loadGuide()

    fun onSeasonSelected(index: Int) {
        _uiState.update { it.copy(selectedSeasonIndex = index) }
    }

    fun onFavoriteClick() {
        val state = _uiState.value
        if (state.isFavoriteBusy) return
        val url = state.seriesUrl.ifBlank {
            savedStateHandle.get<String>(AppDestination.SeriesGuide.detailsUrlArg).orEmpty().substringBefore("/season_")
        }
        val target = state.isFavorite != true
        _uiState.update { it.copy(isFavoriteBusy = true, actionMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            try {
                when (favoritesRepository.setFavorite(url, target)) {
                    FavoriteMutationResult.Updated, FavoriteMutationResult.NoOp -> _uiState.update {
                        it.copy(isFavorite = target, favoriteContentVersion = it.favoriteContentVersion + 1,
                            actionMessage = if (target) "Добавлено в избранное" else "Удалено из избранного")
                    }
                    is FavoriteMutationResult.RequiresLogin -> _uiState.update { it.copy(requiresLogin = true) }
                    is FavoriteMutationResult.Error -> _uiState.update { it.copy(actionMessage = "Не удалось обновить избранное") }
                }
            } finally { _uiState.update { it.copy(isFavoriteBusy = false) } }
        }
    }

    fun onPlaySeasonClick() {
        val state = _uiState.value
        if (state.isPreparingSeason || state.seasons.isEmpty()) return
        val season = state.seasons[state.selectedSeasonIndex.coerceIn(state.seasons.indices)]
        _uiState.update { it.copy(isPreparingSeason = true, actionMessage = "Готовим сезон…") }
        viewModelScope.launch(ioDispatcher) {
            try {
                val playlist = seasonPlaybackPreparer.prepare(season, state.title, state.posterUrl.orEmpty()) { message ->
                    _uiState.update { it.copy(actionMessage = message) }
                }
                _uiState.update { it.copy(seasonPlaylist = playlist, actionMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: SeasonLoginRequired) {
                _uiState.update { it.copy(requiresLogin = true, actionMessage = null) }
            } catch (error: Exception) {
                _uiState.update { it.copy(actionMessage = error.message ?: "Не удалось подготовить сезон") }
            } finally { _uiState.update { it.copy(isPreparingSeason = false) } }
        }
    }

    fun consumePlaylist() { _uiState.update { it.copy(seasonPlaylist = null) } }
    fun consumeLoginRequest() { _uiState.update { it.copy(requiresLogin = false) } }

    private fun loadGuide() {
        val detailsUrl = savedStateHandle.get<String>(AppDestination.SeriesGuide.detailsUrlArg).orEmpty()
        val requestToken = ++loadRequestToken
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        loadJob = viewModelScope.launch(ioDispatcher) {
            when (val result = repository.loadSeriesGuide(detailsUrl)) {
                is SeriesGuideResult.Success -> {
                    if (loadRequestToken != requestToken) return@launch
                    _uiState.value = SeriesGuideUiState(
                        title = result.guide.seriesTitleRu,
                        seriesUrl = result.guide.seriesUrl,
                        isFavorite = result.guide.isFavorite,
                        posterUrl = result.guide.posterUrl,
                        seasons = result.guide.seasons,
                        selectedEpisodeDetailsUrl = result.guide.selectedEpisodeDetailsUrl,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is SeriesGuideResult.Error -> {
                    if (loadRequestToken != requestToken) return@launch
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
