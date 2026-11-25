package com.kraat.lostfilmnewtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.ui.components.ShimmerSkeletonBox
import com.kraat.lostfilmnewtv.ui.components.rememberShimmerSkeletonBrush
import com.kraat.lostfilmnewtv.ui.components.PosterCard
import com.kraat.lostfilmnewtv.ui.components.rememberPosterRequestFactory
import com.kraat.lostfilmnewtv.ui.theme.HomePanelBorder
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurfaceStrong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest

private const val HOME_PAGING_SKELETON_COUNT = 4
private const val CATALOG_POSTERS_AHEAD = 2
private val HOME_RAIL_CARD_SPACING = 9.dp
private val HOME_RAIL_HORIZONTAL_PADDING = 6.dp

@Composable
fun HomeRail(
    railId: String,
    items: List<ReleaseSummary>,
    focusedItemKey: String?,
    entryFocusRequester: FocusRequester,
    cardFocusRequesters: Map<String, FocusRequester>,
    shouldRequestFocus: Boolean,
    returnFocusRequestVersion: Int,
    upTargetRequester: FocusRequester?,
    leftTargetRequester: FocusRequester?,
    downTargetRequester: FocusRequester?,
    isPaging: Boolean,
    onItemFocused: (String) -> Unit,
    onRailFocusChanged: (Boolean) -> Unit,
    onOpenDetails: (String) -> Unit,
    onMoveToPreviousMode: (() -> Unit)? = null,
    onMoveToNextMode: (() -> Unit)? = null,
    onEndReached: () -> Unit,
    modifier: Modifier = Modifier,
    pageWarmupDistance: Int = 0,
    nextPage: Int? = null,
    seriesWithNewEpisodes: Set<String> = emptySet(),
) {
    val itemKeys = remember(items) { items.map { it.detailsUrl } }
    val targetKey = if (focusedItemKey in itemKeys) {
        focusedItemKey
    } else {
        itemKeys.firstOrNull()
    }
    val targetIndex = targetKey?.let(itemKeys::indexOf) ?: -1
    val targetRequester = targetKey?.let { detailsUrl ->
        cardFocusRequesters[homeItemKey(railId, detailsUrl)]
    }
    val listState = remember(railId) { LazyListState() }
    var focusedCardKey by remember(railId) { mutableStateOf<String?>(null) }
    var handledReturnFocusRequestVersion by remember(railId) {
        mutableStateOf(returnFocusRequestVersion)
    }
    val requestNextPage by rememberUpdatedState(onEndReached)
    val currentItems by rememberUpdatedState(items)
    val requestForPoster = rememberPosterRequestFactory()
    val imageLoader = LocalContext.current.imageLoader

    LaunchedEffect(railId, items.size, pageWarmupDistance, nextPage) {
        if (items.isEmpty() || pageWarmupDistance == 0 || nextPage == null) return@LaunchedEffect
        val preloadFromIndex = (items.lastIndex - pageWarmupDistance).coerceAtLeast(0)
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            focusedCardKey != null && lastVisible >= preloadFromIndex
        }.first { it }
        requestNextPage()
    }

    LaunchedEffect(railId, pageWarmupDistance, imageLoader, requestForPoster) {
        if (pageWarmupDistance == 0) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            if (focusedCardKey == null || lastVisible == null) emptyList() else {
                currentItems.subList(
                    (lastVisible + 1).coerceAtMost(currentItems.size),
                    (lastVisible + 1 + CATALOG_POSTERS_AHEAD).coerceAtMost(currentItems.size),
                ).mapNotNull { it.posterUrl.takeIf(String::isNotBlank) }
            }
        }.collectLatest { urls ->
            for (url in urls) imageLoader.execute(requestForPoster(url))
        }
    }

    fun handleVerticalNavigation(key: Key): Boolean {
        return when {
            key == Key.DirectionUp && onMoveToPreviousMode != null -> {
                onMoveToPreviousMode()
                true
            }
            key == Key.DirectionDown && onMoveToNextMode != null -> {
                onMoveToNextMode()
                true
            }
            key == Key.DirectionUp || key == Key.DirectionDown -> true
            else -> false
        }
    }

    LaunchedEffect(shouldRequestFocus, returnFocusRequestVersion, targetIndex, targetRequester) {
        val hasReturnFocusRequest = returnFocusRequestVersion != handledReturnFocusRequestVersion
        if ((!shouldRequestFocus && !hasReturnFocusRequest) || targetRequester == null || targetIndex < 0) {
            return@LaunchedEffect
        }

        handledReturnFocusRequestVersion = returnFocusRequestVersion
        listState.scrollToItem(targetIndex)
        requestFocusWhenReady(targetRequester)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(entryFocusRequester)
                .focusProperties {
                    up = FocusRequester.Cancel
                    down = FocusRequester.Cancel
                }
                .onPreviewKeyEvent { event ->
                    event.type == KeyEventType.KeyDown && handleVerticalNavigation(event.key)
                }
                .height(218.dp),
            horizontalArrangement = Arrangement.spacedBy(HOME_RAIL_CARD_SPACING),
            contentPadding = PaddingValues(
                start = HOME_RAIL_HORIZONTAL_PADDING,
                top = 12.dp,
                end = 10.dp,
                bottom = 20.dp,
            ),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.detailsUrl }) { index, item ->
                val itemKey = homeItemKey(railId, item.detailsUrl)
                PosterCard(
                    item = item,
                    onClick = { onOpenDetails(item.detailsUrl) },
                    isFocused = focusedCardKey == item.detailsUrl,
                    hasNewEpisode = seriesWithNewEpisodes.isNotEmpty() && item.detailsUrl.seriesSlug() in seriesWithNewEpisodes,
                    modifier = Modifier
                        .focusRequester(cardFocusRequesters.getValue(itemKey))
                        .focusProperties {
                            up = FocusRequester.Cancel
                            if (leftTargetRequester != null && index == 0) {
                                left = leftTargetRequester
                            }
                            down = FocusRequester.Cancel
                            if (isPaging && index == items.lastIndex) {
                                right = FocusRequester.Cancel
                            }
                        }
                        .testTag(posterTag(railId, item.detailsUrl))
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onRailFocusChanged(true)
                                focusedCardKey = item.detailsUrl
                                onItemFocused(item.detailsUrl)
                                if (pageWarmupDistance == 0 && index == items.lastIndex) {
                                    onEndReached()
                                }
                            } else if (focusedCardKey == item.detailsUrl) {
                                onRailFocusChanged(false)
                                focusedCardKey = null
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }

                            when {
                                event.key == Key.Back && leftTargetRequester != null -> {
                                    leftTargetRequester.requestFocus()
                                    true
                                }
                                handleVerticalNavigation(event.key) -> true
                                else -> false
                            }
                        },
                )
            }

            if (isPaging) {
                items(
                    count = HOME_PAGING_SKELETON_COUNT,
                    key = { skeletonIndex -> "paging-skeleton-$skeletonIndex" },
                ) { skeletonIndex ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        HomePagingPosterSkeleton(
                            modifier = Modifier
                                .focusable(false)
                                .testTag("home-paging-skeleton-$skeletonIndex"),
                        )
                    }
                }
            }
        }
    }
}

internal fun posterTag(detailsUrl: String): String = posterTag(HOME_RAIL_ALL_NEW, detailsUrl)

internal fun posterTag(railId: String, detailsUrl: String): String = "poster:$railId:$detailsUrl"

@Composable
private fun HomePagingPosterSkeleton(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val shimmerBrush = rememberShimmerSkeletonBrush(
        label = "homePagingSkeleton",
        baseColor = HomePanelSurfaceStrong,
        highlightColor = Color.White,
        baseAlpha = 0.72f,
        highlightAlpha = 0.12f,
        startOffset = -220f,
        endOffset = 420f,
        shimmerWidth = 180f,
        verticalOffset = 264f,
        durationMillis = 1_150,
    )

    ShimmerSkeletonBox(
        brush = shimmerBrush,
        modifier = modifier
            .size(width = 112.dp, height = 172.dp),
        shape = shape,
        baseColor = HomePanelSurfaceStrong,
        borderColor = HomePanelBorder.copy(alpha = 0.20f),
    )
}
