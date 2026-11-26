package com.kraat.lostfilmnewtv.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kraat.lostfilmnewtv.data.model.CatalogSort
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.data.model.SeriesStatus
import com.kraat.lostfilmnewtv.ui.theme.FocusBackground
import com.kraat.lostfilmnewtv.ui.theme.FocusBorder
import com.kraat.lostfilmnewtv.ui.theme.HomePanelBorder
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurfaceStrong
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary

internal enum class ReleaseWatchFilter(val label: String) {
    All("Все релизы"),
    Unwatched("Непросмотренные"),
    Watched("Просмотренные");

    fun apply(items: List<ReleaseSummary>): List<ReleaseSummary> = when (this) {
        All -> items
        Unwatched -> items.filterNot { it.isWatched }
        Watched -> items.filter { it.isWatched }
    }
}

@Composable
internal fun HomeFilterControls(
    state: HomeUiState,
    watchFilter: ReleaseWatchFilter,
    onWatchFilterChanged: (ReleaseWatchFilter) -> Unit,
    newFavoritesOnly: Boolean,
    onNewFavoritesOnlyChanged: (Boolean) -> Unit,
    onSortChanged: (CatalogSort) -> Unit,
    onStatusChanged: (SeriesStatus) -> Unit,
    focusRequester: FocusRequester,
    menuRequester: FocusRequester,
    contentRequester: FocusRequester?,
) {
    val statusRequester = remember { FocusRequester() }
    val primary = Modifier.testTag("home-filter-primary").focusRequester(focusRequester).focusProperties {
        up = menuRequester
        left = menuRequester
        contentRequester?.let { down = it }
        if (state.selectedMode == HomeFeedMode.Series) right = statusRequester
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.selectedMode) {
            HomeFeedMode.Movies, HomeFeedMode.Series -> {
                val isSeries = state.selectedMode == HomeFeedMode.Series
                val sorts = if (isSeries) CatalogSort.entries else CatalogSort.entries.filter { it != CatalogSort.CompletionDate }
                FilterMenu(
                    sorts, if (isSeries) state.seriesSort else state.movieSort, CatalogSort::label,
                    onSortChanged, primary,
                )
                if (isSeries) {
                    FilterMenu(
                        SeriesStatus.entries, state.seriesStatus, SeriesStatus::label, onStatusChanged,
                        Modifier.testTag("home-filter-status").focusRequester(statusRequester).focusProperties {
                            up = menuRequester
                            left = focusRequester
                            contentRequester?.let { down = it }
                        },
                    )
                }
            }
            HomeFeedMode.AllNew, HomeFeedMode.Favorites ->
                FilterMenu(ReleaseWatchFilter.entries, watchFilter, ReleaseWatchFilter::label, onWatchFilterChanged, primary)
            HomeFeedMode.FavoriteSeries -> FilterMenu(
                listOf(false, true), newFavoritesOnly,
                { if (it) "С новой серией" else "Все избранные" },
                onNewFavoritesOnlyChanged, primary,
            )
        }
    }
}

@Composable
private fun <T> FilterMenu(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val selectedOptionFocus = remember { FocusRequester() }
    Box {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = modifier.height(32.dp).onFocusChanged { focused = it.isFocused },
                contentPadding = PaddingValues(horizontal = 12.dp),
                border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) FocusBorder else HomePanelBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (focused) FocusBackground else HomePanelSurfaceStrong,
                    contentColor = TextPrimary,
                ),
            ) {
                Text(label(selected))
                Text("▾", Modifier.padding(start = 6.dp))
            }
        }
        if (expanded) {
            DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
                LaunchedEffect(selected) { requestFocusWhenReady(selectedOptionFocus) }
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = if (option == selected) Modifier.focusRequester(selectedOptionFocus) else Modifier,
                        text = { Text(label(option)) },
                        onClick = {
                            expanded = false
                            if (option != selected) onSelected(option)
                        },
                    )
                }
            }
        }
    }
}

private val CatalogSort.label: String
    get() = when (this) {
        CatalogSort.Added -> "По добавлению"
        CatalogSort.Newest -> "По дате выхода"
        CatalogSort.Alphabetical -> "По алфавиту"
        CatalogSort.Rating -> "По рейтингу"
        CatalogSort.CompletionDate -> "По дате завершения"
    }

private val SeriesStatus.label: String
    get() = when (this) {
        SeriesStatus.All -> "Все сериалы"
        SeriesStatus.New -> "Новые"
        SeriesStatus.Ongoing -> "Выходят"
        SeriesStatus.Completed -> "Завершённые"
        SeriesStatus.Favorites -> "Избранные"
    }
