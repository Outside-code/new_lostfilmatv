package com.kraat.lostfilmnewtv.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kraat.lostfilmnewtv.data.model.LostFilmSearchItem

@Composable
fun SearchRoute(
    onOpenItem: (LostFilmSearchItem) -> Unit,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    var voiceMessage by remember { mutableStateOf<String?>(null) }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val query = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
            if (!query.isNullOrBlank()) {
                viewModel.onQueryChanged(query)
                viewModel.onSearchTriggered()
                voiceMessage = null
            } else voiceMessage = "Не удалось разобрать речь. Попробуйте ещё раз."
        }
    }

    LaunchedEffect(Unit) { viewModel.onStart() }

    SearchScreen(
        state = state.value,
        voiceMessage = voiceMessage,
        onVoiceSearch = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Название сериала или фильма")
            try {
                voiceLauncher.launch(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                voiceMessage = "На телевизоре недоступен сервис голосового ввода"
            } catch (_: SecurityException) {
                voiceMessage = "Голосовой сервис телевизора не разрешил запуск"
            }
        },
        onQueryChanged = viewModel::onQueryChanged,
        onSearchTriggered = viewModel::onSearchTriggered,
        onRetry = viewModel::onRetry,
        onOpenItem = onOpenItem,
    )
}
