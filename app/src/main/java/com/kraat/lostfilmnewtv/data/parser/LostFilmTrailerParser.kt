package com.kraat.lostfilmnewtv.data.parser

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document

internal fun Document.seriesTrailerUrl(): String? =
    select(".video-block, .video_block").firstNotNullOfOrNull { block ->
        val label = block.select(".title, .description").text()
        if (!label.contains("трейлер", ignoreCase = true) && !label.contains("trailer", ignoreCase = true)) {
            return@firstNotNullOfOrNull null
        }
        val url = block.selectFirst(".play-btn[data-src]")?.attr("data-src")?.toHttpUrlOrNull()
            ?: return@firstNotNullOfOrNull null
        url.takeIf {
            it.isHttps && it.port == 443 && it.username.isEmpty() && it.password.isEmpty() &&
                (it.host == "store.bogi.ru" || it.host == BASE_URL.toHttpUrlOrNull()?.host) &&
                it.encodedPath.endsWith(".mp4", ignoreCase = true)
        }?.toString()
    }
