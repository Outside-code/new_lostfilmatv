package com.kraat.lostfilmnewtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.request.ImageRequest
import coil.size.Precision
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
