package com.kraat.lostfilmnewtv.ui.settings

import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.platform.torrserve.TorrServeAvailabilityChecker
import com.kraat.lostfilmnewtv.playback.PlaybackPreferencesStore
import kotlinx.coroutines.test.runTest
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TorrServeEndpointCheckerTest {
    @Test
    fun queriesTheProtectedApiUsingSavedCredentials() = runTest {
        MockWebServer().use { server ->
            val address = server.url("/").toString()
            val preferences = PlaybackPreferencesStore(ApplicationProvider.getApplicationContext())
            preferences.writeTorrServeBaseUrl(address)
            preferences.writeTorrServeCredentials("viewer", "password")
            val checker = checker(preferences)

            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(checker.check(address).isEndpointReachable)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/torrents", request.path)
            assertEquals("""{"action":"list"}""", request.body.readUtf8())
            assertEquals(Credentials.basic("viewer", "password"), request.getHeader("Authorization"))

            server.enqueue(MockResponse().setResponseCode(401))
            val rejected = checker.check(address)
            assertTrue(rejected.credentialsRejected)
            assertFalse(rejected.isEndpointReachable)
        }
    }

    @Test
    fun neverSendsSavedCredentialsToAnAlternativeAddress() = runTest {
        MockWebServer().use { server ->
            val preferences = PlaybackPreferencesStore(ApplicationProvider.getApplicationContext())
            preferences.writeTorrServeBaseUrl("https://example.org")
            preferences.writeTorrServeCredentials("viewer", "password")

            server.enqueue(MockResponse().setResponseCode(401))
            val result = checker(preferences).check(server.url("/").toString())
            assertTrue(result.requiresAuthentication)
            assertFalse(result.credentialsRejected)
            assertNull(server.takeRequest().getHeader("Authorization"))
        }
    }

    private fun checker(preferences: PlaybackPreferencesStore) = OkHttpTorrServeEndpointChecker(
        OkHttpClient(),
        object : TorrServeAvailabilityChecker {
            override suspend fun isAvailable() = false
        },
        preferences,
    )
}
