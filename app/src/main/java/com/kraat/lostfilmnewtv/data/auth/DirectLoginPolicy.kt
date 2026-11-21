package com.kraat.lostfilmnewtv.data.auth

import com.kraat.lostfilmnewtv.data.model.LostFilmCookie
import com.kraat.lostfilmnewtv.data.model.LostFilmSession
import java.net.URI

const val DIRECT_LOGIN_BASE_URL = "https://www.lostfilm.tv"
const val DIRECT_LOGIN_URL = "$DIRECT_LOGIN_BASE_URL/login"

/** Only the official HTTPS origin may be opened in the sign-in browser. */
fun isDirectLoginUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.host?.lowercase() in setOf("lostfilm.tv", "www.lostfilm.tv") &&
        uri.userInfo == null && uri.port in setOf(-1, 443)
}.getOrDefault(false)

fun directLoginSession(cookieHeader: String?): LostFilmSession? {
    if (cookieHeader.isNullOrBlank() || cookieHeader.any { it == '\r' || it == '\n' }) return null
    val cookies = cookieHeader.split(';').mapNotNull { part ->
        val name = part.substringBefore('=').trim()
        val value = part.substringAfter('=', "").trim()
        if (!part.contains('=') || name.isBlank() || value.isBlank()) return@mapNotNull null
        LostFilmCookie(name = name, value = value, domain = "www.lostfilm.tv")
    }
    return cookies.takeIf { it.isNotEmpty() }?.let { LostFilmSession(cookies = it) }
}
