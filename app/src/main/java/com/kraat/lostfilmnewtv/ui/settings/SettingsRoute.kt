package com.kraat.lostfilmnewtv.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kraat.lostfilmnewtv.ui.home.requestFocusWhenReady

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
    initialSection: String? = null,
    isAuthenticated: Boolean = false,
    onAuthClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    initialBackupUri: Uri? = null,
    onInitialBackupRead: () -> Unit = {},
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val files = remember(context) { SettingsBackupFiles(context.contentResolver) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            viewModel.onSaveSettingsBackup { content ->
                files.write(uri, content)
                "выбранный файл"
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri -> viewModel.onReadSettingsBackup { files.read(uri) } }
    }

    LaunchedEffect(Unit) { viewModel.onScreenShown() }
    LaunchedEffect(initialSection) {
        if (initialSection != null) viewModel.onDeepLinkSection(initialSection)
    }
    LaunchedEffect(initialBackupUri) {
        initialBackupUri?.let { uri ->
            viewModel.onReadSettingsBackup { files.read(uri) }
            onInitialBackupRead()
        }
    }

    SettingsScreen(
        currentSection = state.value.currentSection,
        onSectionSelected = viewModel::onSectionSelected,
        onSectionBack = viewModel::onSectionBack,
        onNavigateBack = onNavigateBack,
        selectedQuality = state.value.playbackQuality,
        onQualitySelected = viewModel::onPlaybackQualitySelected,
        selectedUpdateMode = state.value.updateMode,
        selectedChannelMode = state.value.channelMode,
        isHomeFavoritesRailEnabled = state.value.isHomeFavoritesRailEnabled,
        isHomeFavoriteSeriesEnabled = state.value.isHomeFavoriteSeriesEnabled,
        isHomeMoviesEnabled = state.value.isHomeMoviesEnabled,
        isHomeSeriesEnabled = state.value.isHomeSeriesEnabled,
        isHomeMenuLabelsEnabled = state.value.isHomeMenuLabelsEnabled,
        selectedWatchedMarkingMode = state.value.watchedMarkingMode,
        onWatchedMarkingModeSelected = viewModel::onWatchedMarkingModeSelected,
        torrServeBaseUrl = state.value.torrServeBaseUrl,
        torrServeUsername = state.value.torrServeUsername,
        torrServePassword = state.value.torrServePassword,
        onTorrServeUsernameChanged = viewModel::onTorrServeUsernameChanged,
        onTorrServePasswordChanged = viewModel::onTorrServePasswordChanged,
        lostFilmHost = state.value.lostFilmHost,
        lostFilmHostStatusText = state.value.lostFilmHostStatusText,
        onLostFilmHostChanged = viewModel::onLostFilmHostChanged,
        onSaveLostFilmHostClick = viewModel::onSaveLostFilmHostClick,
        onResetLostFilmHostClick = viewModel::onResetLostFilmHostClick,
        torrServeStatusText = state.value.torrServeStatusText,
        isCheckingTorrServe = state.value.isCheckingTorrServe,
        onTorrServeBaseUrlChanged = viewModel::onTorrServeBaseUrlChanged,
        onSaveTorrServeClick = viewModel::onSaveTorrServeBaseUrlClick,
        onResetTorrServeClick = viewModel::onResetTorrServeBaseUrlClick,
        onCheckTorrServeClick = viewModel::onCheckTorrServeClick,
        isAuthenticated = isAuthenticated,
        onAuthClick = onAuthClick,
        installedVersionText = state.value.installedVersionText,
        latestVersionText = state.value.savedAppUpdate?.latestVersion ?: state.value.latestVersionText,
        statusText = state.value.statusText,
        isCheckingForUpdates = state.value.isCheckingForUpdates,
        isDownloadingUpdate = state.value.isDownloadingUpdate,
        installDownloadProgress = state.value.installDownloadProgress,
        installUrl = state.value.installUrl,
        onUpdateModeSelected = viewModel::onUpdateModeSelected,
        onChannelModeSelected = viewModel::onChannelModeSelected,
        onHomeFavoritesRailVisibilitySelected = viewModel::onHomeFavoritesRailVisibilitySelected,
        onHomeFavoriteSeriesVisibilitySelected = viewModel::onHomeFavoriteSeriesVisibilitySelected,
        onHomeMoviesVisibilitySelected = viewModel::onHomeMoviesVisibilitySelected,
        onHomeSeriesVisibilitySelected = viewModel::onHomeSeriesVisibilitySelected,
        onHomeMenuLabelsVisibilitySelected = viewModel::onHomeMenuLabelsVisibilitySelected,
        onCheckForUpdatesClick = viewModel::onCheckForUpdatesClick,
        onInstallUpdateClick = {
            state.value.installUrl?.let { url ->
                viewModel.installUpdate(url)
            }
        },
        isBackupBusy = state.value.isBackupBusy,
        backupStatusText = state.value.backupStatusText,
        onSaveBackup = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewModel.onSaveSettingsBackup(files::saveInDownloads)
            } else {
                val picker = context.settingsBackupPicker(Intent.ACTION_CREATE_DOCUMENT, SettingsBackupFiles.JSON_TYPE)
                if (picker == null) {
                    viewModel.onBackupPickerUnavailable()
                } else {
                    try {
                        createBackup.launch(picker.putExtra(Intent.EXTRA_TITLE, SettingsBackupFiles.newFileName()))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onBackupPickerUnavailable()
                    }
                }
            }
        },
        onRestoreBackup = {
            val picker = context.settingsBackupPicker(Intent.ACTION_OPEN_DOCUMENT, "*/*")
                ?: context.settingsBackupPicker(Intent.ACTION_GET_CONTENT, SettingsBackupFiles.JSON_TYPE)
            if (picker == null) {
                viewModel.onReadSettingsBackup(files::readLatest)
            } else {
                try {
                    openBackup.launch(picker)
                } catch (_: ActivityNotFoundException) {
                    viewModel.onReadSettingsBackup(files::readLatest)
                }
            }
        },
    )

    state.value.backupConfirmation?.let { summary ->
        val cancelFocus = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = viewModel::onCancelSettingsRestore,
            title = { Text("Восстановить настройки?") },
            text = { Text(summary) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmSettingsRestore) { Text("Восстановить") }
            },
            dismissButton = {
                LaunchedEffect(cancelFocus) { requestFocusWhenReady(cancelFocus) }
                TextButton(
                    onClick = viewModel::onCancelSettingsRestore,
                    modifier = Modifier.focusRequester(cancelFocus),
                ) { Text("Отмена") }
            },
        )
    }
}
