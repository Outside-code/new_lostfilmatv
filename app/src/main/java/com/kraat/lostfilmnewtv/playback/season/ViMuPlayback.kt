package com.kraat.lostfilmnewtv.playback.season

import android.app.Activity
import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object ViMuPlayback {
    val packages = listOf("net.gtvbox.videoplayer", "net.gtvbox.vimuhd")
    const val COMPLETED = 1
    const val FAILED = 4
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

    fun positionOnExit(resultCode: Int, data: Intent?): Long? {
        if (resultCode != Activity.RESULT_CANCELED || data == null) return null
        if (data.action != null && data.action !in resultActions) return null
        val position = data.getIntExtra("position", -1).takeIf { it >= 0 } ?: return null
        val duration = data.getIntExtra("duration", 0)
        return position.toLong().takeIf { duration <= 0 || position <= duration }
    }
}
