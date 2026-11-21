package com.kraat.lostfilmnewtv.data.auth

import com.kraat.lostfilmnewtv.data.model.LostFilmSession
import com.kraat.lostfilmnewtv.data.network.LostFilmSessionVerifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class DirectAuthRepositoryTest {
    @Test
    fun savesVerifiedSessionAndSelectsOfficialHostWithoutQrServer() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("<a href=\"/logout\">Выйти</a>"))
        server.start()
        try {
            val store = MemorySessionStore()
            var selectedHost = false
            val repository = DirectAuthRepository(store, LostFilmSessionVerifier(OkHttpClient(), server.url("/").toString())) {
                selectedHost = true
            }
            assertEquals(AuthCompletionResult.Authenticated, repository.completeLogin("session=abc=="))
            assertTrue(selectedHost)
            assertEquals("session=abc==", store.session!!.toCookieString())
            assertEquals("session=abc==", server.takeRequest().getHeader("Cookie"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun doesNotReplaceSessionOrHostWhenVerificationFails() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("<form id=\"lf-login-form\"></form>"))
        server.start()
        try {
            val store = MemorySessionStore()
            val original = LostFilmSession(emptyList(), accountId = "existing")
            store.session = original
            val repository = DirectAuthRepository(store, LostFilmSessionVerifier(OkHttpClient(), server.url("/").toString())) {
                fail("Must not change host before a successful verification")
            }
            assertEquals(AuthCompletionResult.VerificationFailed, repository.completeLogin("session=invalid"))
            assertEquals(original, store.session)
            assertEquals(AuthCompletionResult.VerificationFailed, repository.completeLogin(null))
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun doesNotForwardCookiesAcrossRedirects() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/unexpected"))
        server.start()
        try {
            val store = MemorySessionStore()
            val repository = DirectAuthRepository(store, LostFilmSessionVerifier(OkHttpClient(), server.url("/").toString())) {}
            assertEquals(AuthCompletionResult.NetworkError, repository.completeLogin("session=private"))
            assertNull(store.session)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private class MemorySessionStore : SessionStore {
        var session: LostFilmSession? = null
        override suspend fun read() = session
        override suspend fun save(session: LostFilmSession) { this.session = session }
        override suspend fun clear() { session = null }
        override suspend fun isExpired() = false
        override suspend fun markExpired() = Unit
        override fun changes() = MutableSharedFlow<Unit>()
    }
}
