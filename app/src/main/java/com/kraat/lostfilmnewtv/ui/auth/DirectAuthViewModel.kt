package com.kraat.lostfilmnewtv.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kraat.lostfilmnewtv.data.auth.AuthCompletionResult
import com.kraat.lostfilmnewtv.data.auth.DirectAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DirectAuthUiState(
    val isVerifying: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DirectAuthViewModel @Inject constructor(
    private val repository: DirectAuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DirectAuthUiState())
    val uiState = mutableState.asStateFlow()
    private var verificationJob: Job? = null

    fun completeLogin(cookieHeader: String?) {
        if (mutableState.value.isVerifying) return
        verificationJob = viewModelScope.launch {
            mutableState.value = DirectAuthUiState(isVerifying = true)
            mutableState.value = when (repository.completeLogin(cookieHeader)) {
                AuthCompletionResult.Authenticated -> DirectAuthUiState(isAuthenticated = true)
                AuthCompletionResult.NetworkError -> DirectAuthUiState(error = "Не удалось проверить вход. Проверьте интернет и попробуйте ещё раз.")
                else -> DirectAuthUiState(error = "Вход не подтверждён. Введите данные на сайте и нажмите «Я вошёл».")
            }
        }
    }

    fun showBrowserError(message: String) {
        if (!mutableState.value.isVerifying) mutableState.value = DirectAuthUiState(error = message)
    }

    fun cancelLogin() {
        verificationJob?.cancel()
        mutableState.value = DirectAuthUiState()
    }
}
