package com.kraat.lostfilmnewtv.playback.season

import android.app.Activity
import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object ViMuPlayback {
    val packages = listOf("net.gtvbox.videoplayer", "net.gtvbox.vimuhd")
    private const val RESULT_ENDED = 2
    private const val RESULT_STARTED = 3
    private const val RESULT_FAILED = 4
    private const val END_POSITION_TOLERANCE_MS = 2_000
    private val resultActions = packages.mapTo(mutableSetOf()) { "$it.result" }

    fun intent(url: String, title: String, playerPackage: String): Intent? {
        val source = url.toHttpUrlOrNull() ?: return null
        if (source.username.isNotEmpty() || source.password.isNotEmpty()) return null
        // Keep the same stream identifier TorrServe hands to ViMu, including the bare play flag.
        val playbackUrl = source.newBuilder().removeAllQueryParameters("play")
            .addQueryParameter("play", null).build()
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage(playerPackage)
            setDataAndType(Uri.parse(playbackUrl.toString()), "video/*")
            putExtra("forcename", title)
            putExtra("forcedirect", true)
            putExtra("forceresume", true)
        }
    }

    sealed interface Result {
        data object Completed : Result
        data object Failed : Result
        data class Interrupted(val positionMs: Long? = null) : Result
    }

    fun playbackResult(resultCode: Int, data: Intent?): Result {
        if (data?.action != null && data.action !in resultActions) return Result.Interrupted()
        if (resultCode == Activity.RESULT_FIRST_USER) return Result.Completed
        if (resultCode == RESULT_FAILED) return Result.Failed
        if (resultCode !in setOf(Activity.RESULT_CANCELED, RESULT_ENDED, RESULT_STARTED)) {
            return Result.Interrupted()
        }

        val position = data?.getIntExtra("position", -1)?.takeIf { it >= 0 }
        val duration = data?.getIntExtra("duration", -1)?.takeIf { it > 0 }
        // Some ViMu versions use result 2 when the player closes; a reported early position still means Back.
        if (resultCode == RESULT_ENDED &&
            (position == null || duration == null || duration - position <= END_POSITION_TOLERANCE_MS)
        ) {
            return Result.Completed
        }
        if (position != null && duration != null && position == duration) return Result.Completed
        return Result.Interrupted(position?.takeIf { duration == null || it <= duration }?.toLong())
    }
}
