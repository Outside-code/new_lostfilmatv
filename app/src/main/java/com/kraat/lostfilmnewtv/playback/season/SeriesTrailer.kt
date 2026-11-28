package com.kraat.lostfilmnewtv.playback.season

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

private const val YOUTUBE_TV_PACKAGE = "com.google.android.youtube.tv"
private const val EXTRA_START_PLAYBACK = "android.intent.extra.START_PLAYBACK"

internal fun isYouTubeTrailer(trailerUrl: String): Boolean {
    val uri = Uri.parse(trailerUrl)
    return uri.scheme == "https" && uri.host == "www.youtube.com" && uri.path == "/watch"
}

internal fun youtubeTrailerIntent(trailerUrl: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
        .putExtra(EXTRA_START_PLAYBACK, true)
        .setPackage(YOUTUBE_TV_PACKAGE)

internal fun openSeriesTrailer(context: Context, seriesTitle: String, trailerUrl: String?) {
    if (trailerUrl == null) return
    if (!isYouTubeTrailer(trailerUrl)) {
        context.startActivity(SeasonPlayerActivity.createTrailerIntent(context, trailerUrl, seriesTitle))
        return
    }
    val intent = youtubeTrailerIntent(trailerUrl)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(intent.setPackage(null))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Не удалось открыть YouTube на этом устройстве", Toast.LENGTH_LONG).show()
        }
    }
}
