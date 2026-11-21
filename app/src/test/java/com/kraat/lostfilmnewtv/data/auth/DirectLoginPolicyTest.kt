package com.kraat.lostfilmnewtv.data.auth

import com.kraat.lostfilmnewtv.data.parser.LostFilmBaseUrl
import com.kraat.lostfilmnewtv.data.parser.resolveUrl
import org.junit.Assert.*
import org.junit.Test

class DirectLoginPolicyTest {
    @Test
    fun browserAllowsOnlyOfficialHttpsPages() {
        assertTrue(isDirectLoginUrl(DIRECT_LOGIN_URL))
        assertTrue(isDirectLoginUrl("https://lostfilm.tv/my"))
        listOf(
            "http://www.lostfilm.tv/login", "https://www.lostfilm.today/login",
            "https://auth.bazuka.pp.ua", "https://lostfilm.tv.evil.test/login",
            "https://lostfilm.tv@evil.test", "https://evil.test@lostfilm.tv",
            "https://lostfilm.tv:8443/login", "javascript:alert(1)", "file:///etc/passwd",
        ).forEach { assertFalse(it, isDirectLoginUrl(it)) }
    }

    @Test
    fun importsBrowserCookiesWithoutLosingEqualsInValues() {
        val session = directLoginSession("session=abc==; id=42")!!
        assertEquals("session=abc==; id=42", session.toCookieString())
        assertTrue(session.cookies.all { it.domain == "www.lostfilm.tv" })
        assertEquals("", session.toCookieStringForHost("www.lostfilm.today"))
        assertEquals("", session.toCookieStringForHost("www.lostfilm.tv.evil.test"))
        assertEquals(session.toCookieString(), session.toCookieStringForHost("www.lostfilm.tv"))
        assertNull(directLoginSession(null))
        assertNull(directLoginSession("not-a-cookie"))
        assertNull(directLoginSession("session=a\r\nInjected: b"))
    }

    @Test
    fun cachedMirrorLinksFollowOfficialHostAfterLogin() {
        val previous = LostFilmBaseUrl.get()
        try {
            LostFilmBaseUrl.set(DIRECT_LOGIN_BASE_URL)
            assertEquals("https://www.lostfilm.tv/series/Test/?a=1#season", resolveUrl("https://www.lostfilm.today/series/Test/?a=1#season"))
            assertEquals("https://external.test/image.jpg", resolveUrl("https://external.test/image.jpg"))
        } finally {
            LostFilmBaseUrl.set(previous)
        }
    }
}
