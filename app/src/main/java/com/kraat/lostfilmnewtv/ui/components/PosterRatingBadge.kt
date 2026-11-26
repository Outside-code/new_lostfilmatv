package com.kraat.lostfilmnewtv.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kraat.lostfilmnewtv.data.network.TmdbImageFallbackInterceptor
import com.kraat.lostfilmnewtv.ui.theme.BackgroundDeep
import com.kraat.lostfilmnewtv.ui.theme.HomeAccentGoldGlow
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary

private val RatingFont by lazy { FontFamily(Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)) }

internal fun tmdbPosterRating(rating: String?, posterUrl: String): String? =
    rating?.trim()?.takeIf {
        val number = it.toDoubleOrNull()
        number != null && number > 0.0 && number <= 10.0 &&
            TmdbImageFallbackInterceptor.isTmdbArtworkUrl(posterUrl)
    }

@Composable
internal fun PosterRatingBadge(rating: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .padding(top = 5.dp, end = 5.dp)
            .testTag("poster-tmdb-rating")
            .semantics { contentDescription = "TMDB $rating" }
            .background(BackgroundDeep.copy(alpha = 0.50f), shape)
            .border(0.75.dp, HomeAccentGoldGlow.copy(alpha = 0.75f), shape)
            .defaultMinSize(minWidth = 21.dp, minHeight = 15.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rating,
            color = TextPrimary,
            style = TextStyle(
                fontFamily = RatingFont,
                fontSize = 7.sp,
                lineHeight = 8.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            maxLines = 1,
        )
    }
}
