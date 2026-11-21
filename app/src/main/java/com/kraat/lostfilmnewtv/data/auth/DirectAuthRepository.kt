package com.kraat.lostfilmnewtv.data.auth

import com.kraat.lostfilmnewtv.data.network.LostFilmSessionVerifier
import java.io.IOException
import kotlinx.coroutines.CancellationException

class DirectAuthRepository(
    private val sessionStore: SessionStore,
    private val sessionVerifier: LostFilmSessionVerifier,
    private val selectOfficialHost: () -> Unit,
) {
    suspend fun completeLogin(cookieHeader: String?): AuthCompletionResult {
        val session = directLoginSession(cookieHeader) ?: return AuthCompletionResult.VerificationFailed
        return try {
            if (!sessionVerifier.verify(session, baseUrl = DIRECT_LOGIN_BASE_URL)) {
                return AuthCompletionResult.VerificationFailed
            }
            // Subsequent account requests must use the same domain as the browser session.
            selectOfficialHost()
            sessionStore.save(session)
            AuthCompletionResult.Authenticated
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            AuthCompletionResult.NetworkError
        } catch (_: Exception) {
            AuthCompletionResult.RecoverableFailure()
        }
    }
}
