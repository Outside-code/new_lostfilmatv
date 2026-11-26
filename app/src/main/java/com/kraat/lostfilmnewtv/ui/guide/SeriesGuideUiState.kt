package com.kraat.lostfilmnewtv.ui.guide

import com.kraat.lostfilmnewtv.data.model.SeriesGuideSeason

data class SeriesGuideUiState(
    val title: String = "",
    val posterUrl: String? = null,
    val seasons: List<SeriesGuideSeason> = emptyList(),
    val selectedSeasonIndex: Int = 0,
    val selectedEpisodeDetailsUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val seriesUrl: String = "",
    val isFavorite: Boolean? = null,
    val isFavoriteBusy: Boolean = false,
    val favoriteContentVersion: Int = 0,
    val actionMessage: String? = null,
    val isPreparingSeason: Boolean = false,
    val requiresLogin: Boolean = false,
    val trailerUrl: String? = null,
    val tmdbRating: String? = null,
    val seasonPlaylist: com.kraat.lostfilmnewtv.playback.season.SeasonPlaylist? = null,
)
