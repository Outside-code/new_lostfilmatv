package com.kraat.lostfilmnewtv.data.auth

fun interface DirectAuthRepositoryContract {
    suspend fun completeLogin(cookieHeader: String?): AuthCompletionResult
}
