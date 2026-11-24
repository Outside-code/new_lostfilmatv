package com.kraat.lostfilmnewtv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.kraat.lostfilmnewtv.ui.theme.BackgroundPrimary
import kotlin.math.roundToInt

// Upgrade cached TMDB URLs as well as fresh ones. Other image providers keep their URLs.
internal fun highQualityBackdropUrl(url: String): String = url.replace(
    Regex("/t/p/(?:w[0-9]+|original)/"),
    "/t/p/original/",
)

@Composable
internal fun rememberBackdropImageRequest(url: String?): ImageRequest {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    // Decode for the actual display, capped at 4K to bound TV memory usage.
    val width = (configuration.screenWidthDp * density).roundToInt().coerceIn(1920, 3840)
    val height = (configuration.screenHeightDp * density).roundToInt().coerceIn(1080, 2160)
    return remember(context, url, width, height) {
        ImageRequest.Builder(context)
            .data(url?.let(::highQualityBackdropUrl))
            .size(width, height)
            .precision(Precision.EXACT)
            .allowRgb565(false)
            .build()
    }
}

@Composable
internal fun TvBackdrop(url: String?, wideText: Boolean = false) {
    if (url.isNullOrBlank()) return
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = rememberBackdropImageRequest(url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.68f).fillMaxHeight(),
        )
        Box(Modifier.fillMaxSize().drawWithCache {
            val leftFade = if (wideText) {
                Brush.horizontalGradient(
                    0.32f to BackgroundPrimary,
                    0.58f to BackgroundPrimary.copy(alpha = 0.74f),
                    0.87f to BackgroundPrimary.copy(alpha = 0.55f),
                    1f to BackgroundPrimary.copy(alpha = 0.22f),
                )
            } else {
                Brush.horizontalGradient(
                    0.32f to BackgroundPrimary,
                    0.45f to BackgroundPrimary.copy(alpha = 0.70f),
                    0.61f to BackgroundPrimary.copy(alpha = 0.18f),
                    0.77f to Color.Transparent,
                )
            }
            val bottomFade = Brush.verticalGradient(
                0.45f to Color.Transparent,
                0.76f to BackgroundPrimary.copy(alpha = 0.62f),
                1f to BackgroundPrimary,
            )
            onDrawBehind {
                drawRect(leftFade)
                drawRect(bottomFade)
            }
        })
    }
}
