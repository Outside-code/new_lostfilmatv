package com.kraat.lostfilmnewtv.playback.season

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

internal fun youtubeTrailerSearchUrl(seriesTitle: String): Uri =
    Uri.parse("https://www.youtube.com/results").buildUpon()
        .appendQueryParameter("search_query", "${seriesTitle.trim()} официальный трейлер")
        .build()

internal fun openSeriesTrailer(context: Context, seriesTitle: String, trailerUrl: String?) {
    if (trailerUrl != null) {
        context.startActivity(SeasonPlayerActivity.createTrailerIntent(context, trailerUrl, seriesTitle))
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, youtubeTrailerSearchUrl(seriesTitle)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Не удалось открыть YouTube на этом устройстве", Toast.LENGTH_LONG).show()
    }
}
