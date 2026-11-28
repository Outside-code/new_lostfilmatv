package com.kraat.lostfilmnewtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Border
import coil.request.ImageRequest
import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.data.network.TmdbImageFallbackInterceptor
import com.kraat.lostfilmnewtv.ui.theme.FocusBackground
import com.kraat.lostfilmnewtv.ui.theme.FocusBorder
import com.kraat.lostfilmnewtv.ui.theme.BadgeSurface
import com.kraat.lostfilmnewtv.ui.theme.HomeAccentGoldGlow
import com.kraat.lostfilmnewtv.ui.theme.HomePanelBorder
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurface
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurfaceStrong
import kotlin.math.roundToInt

private val POSTER_CARD_WIDTH = 112.dp
private val POSTER_CARD_HEIGHT = 172.dp
private const val POSTER_FOCUSED_SCALE = 1.04f

@Composable
fun PosterCard(
    item: ReleaseSummary,
    onClick: () -> Unit,
    isFocused: Boolean = false,
    modifier: Modifier = Modifier,
    hasNewEpisode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val overlayColor = if (isFocused) FocusBackground.copy(alpha = 0.82f) else HomePanelSurface.copy(alpha = 0.62f)
    val requestForPoster = rememberPosterRequestFactory()
    val posterRequest = remember(item.posterUrl, requestForPoster) {
        item.posterUrl.takeIf { it.isNotBlank() }?.let(requestForPoster)
    }

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(width = POSTER_CARD_WIDTH, height = POSTER_CARD_HEIGHT)
            .semantics { contentDescription = item.titleRu },
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = POSTER_FOCUSED_SCALE),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = HomeAccentGoldGlow.copy(alpha = 0.18f),
                elevation = 8.dp
            )
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(0.5.dp, HomePanelBorder.copy(alpha = 0.65f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, FocusBorder),
                shape = shape
            )
        ),
        colors = CardDefaults.colors(
            containerColor = HomePanelSurfaceStrong,
            focusedContainerColor = HomePanelSurfaceStrong
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PosterImage(
                model = posterRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                catalogPreviewUrl = item.catalogPreviewUrl,
                placeholderTitle = item.titleRu,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.42f to Color.Transparent,
                            1f to overlayColor,
                        ),
                    ),
            )

            if (isFocused) seasonEpisodeLabel(item)?.let { label ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .testTag("poster-meta:${item.detailsUrl}")
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to overlayColor,
                            ),
                        )
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            PosterBadges(
                status = if (hasNewEpisode) "Новая серия" else item.availabilityLabel,
                rating = tmdbPosterRating(item.tmdbRating, item.posterUrl),
                isWatched = item.isWatched,
                isFocused = isFocused,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun PosterBadges(
    status: String?,
    rating: String?,
    isWatched: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    if (status.isNullOrBlank() && rating == null && !isWatched) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.weight(1f).padding(start = 6.dp, top = 6.dp)) {
            if (!status.isNullOrBlank()) {
                Text(
                    text = status,
                    color = HomeAccentGoldGlow,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(BadgeSurface, RoundedCornerShape(999.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (rating != null) PosterRatingBadge(rating)
            if (isWatched) {
                val badgeColor = if (isFocused) HomeAccentGoldGlow else HomePanelSurfaceStrong
                Text(
                    text = "✓",
                    color = if (isFocused) Color(0xFF0D1930) else HomeAccentGoldGlow,
                    fontSize = 8.25.sp,
                    lineHeight = 8.25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = if (rating == null) 6.dp else 0.dp, end = 6.dp)
                        .background(badgeColor.copy(alpha = 0.86f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 4.5.dp, vertical = 2.25.dp),
                )
            }
        }
    }
}

@Composable
internal fun rememberPosterRequestFactory(): (String) -> ImageRequest {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetWidthPx = with(density) { (POSTER_CARD_WIDTH.toPx() * POSTER_FOCUSED_SCALE).roundToInt().coerceAtLeast(1) }
    val targetHeightPx = with(density) { (POSTER_CARD_HEIGHT.toPx() * POSTER_FOCUSED_SCALE).roundToInt().coerceAtLeast(1) }
    val windowWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val outputWidthPx = LocalView.current.display?.mode?.physicalWidth ?: 0
    val displayWidthPx = maxOf(windowWidthPx, outputWidthPx)

    return remember(context, targetWidthPx, targetHeightPx, displayWidthPx) {
        { posterUrl: String ->
            ImageRequest.Builder(context)
                .data(TmdbImageFallbackInterceptor.thumbnailUrl(posterUrl, targetWidthPx, displayWidthPx))
                .size(targetWidthPx, targetHeightPx)
                .build()
        }
    }
}

private fun seasonEpisodeLabel(item: ReleaseSummary): String? {
    if (item.kind != ReleaseKind.SERIES) return null
    val seasonNumber = item.seasonNumber ?: return null
    val episodeNumber = item.episodeNumber ?: return null
    return "Сезон $seasonNumber, серия $episodeNumber"
}
