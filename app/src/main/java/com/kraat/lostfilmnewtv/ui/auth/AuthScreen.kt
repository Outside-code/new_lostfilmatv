package com.kraat.lostfilmnewtv.ui.auth

import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kraat.lostfilmnewtv.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kraat.lostfilmnewtv.data.model.PairingSession
import com.kraat.lostfilmnewtv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    autoStart: Boolean = true,
    onAuthComplete: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deviceId = remember(context) {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    val leave = {
        viewModel.cancelAuth()
        onNavigateBack()
    }
    BackHandler(onBack = leave)
    LaunchedEffect(autoStart) {
        if (autoStart && state is AuthUiState.Idle) viewModel.startAuth()
    }
    LaunchedEffect(state) {
        if (state is AuthUiState.Authenticated) onAuthComplete()
    }
    val pairing = when (val current = state) {
        is AuthUiState.WaitingForPhoneOpen -> current.pairing
        is AuthUiState.WaitingForPhoneLogin -> current.pairing
        is AuthUiState.VerifyingSession -> current.pairing
        else -> null
    }

    Row(
        Modifier.fillMaxSize()
            .background(Brush.horizontalGradient(listOf(BackgroundPrimary, BackgroundSurface)))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.lostfilm_wordmark),
                contentDescription = "Lost Film",
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(240.dp).height(52.dp),
            )
            Text("Вход с телефона", color = TextPrimary, fontSize = 34.sp,
                lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            Text("Любимые сериалы и история просмотра —\nна вашем телевизоре.",
                color = HomeTextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
            AuthStep("01", "Наведите камеру на QR-код", "Откройте ссылку на телефоне.")
            AuthStep("02", "Войдите в аккаунт LostFilm", "Введите данные в браузере телефона.")
            AuthStep("03", "Продолжайте на телевизоре", "Экран обновится автоматически.")
        }
        Column(
            Modifier.width(304.dp).verticalScroll(rememberScrollState())
                .background(HomePanelSurfaceStrong, RoundedCornerShape(14.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pairing != null) {
                PairingCode(pairing, deviceId, state)
                AuthAction("Отменить", state::class, leave)
            } else when (val current = state) {
                AuthUiState.CreatingCode -> {
                    CircularProgressIndicator(color = HomeAccentGold)
                    Text("Создаём QR-код…", color = TextPrimary)
                    Text("Подключаемся к серверу авторизации", color = HomeTextSecondary)
                    AuthAction("Назад", state::class, leave)
                }
                AuthUiState.Authenticated -> Text("Вход выполнен", color = HomeAccentGold)
                AuthUiState.Idle -> {
                    Text("Подключите аккаунт", color = TextPrimary, fontSize = 22.sp)
                    AuthAction("Получить QR-код", state::class, viewModel::startAuth)
                    AuthAction("Назад", null, leave)
                }
                else -> {
                    Text(if (current is AuthUiState.Expired) "Код истёк" else "Не удалось войти",
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(when (current) {
                        is AuthUiState.Expired -> current.message
                        is AuthUiState.RecoverableError -> current.message
                        else -> "Попробуйте получить новый код."
                    }, color = HomeTextSecondary, lineHeight = 22.sp)
                    AuthAction("Получить новый код", state::class, viewModel::retryAuth)
                    AuthAction("Назад", null, leave)
                }
            }
        }
    }
}

@Composable
private fun PairingCode(pairing: PairingSession, deviceId: String?, state: AuthUiState) {
    val qrUrl = remember(pairing.verificationUrl, deviceId) {
        Uri.parse(pairing.verificationUrl).buildUpon().apply {
            if (!deviceId.isNullOrBlank()) appendQueryParameter("device_id", deviceId)
        }.build().toString()
    }
    val bitmap = remember(qrUrl) { QrCodeGenerator.generateImageBitmap(qrUrl, 768) }
    var secondsLeft by remember(pairing.pairingId) { mutableIntStateOf(pairing.expiresIn) }
    LaunchedEffect(pairing.pairingId, pairing.expiresIn) {
        secondsLeft = pairing.expiresIn.coerceAtLeast(0)
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }
    Text("СКАНИРУЙТЕ КАМЕРОЙ", color = HomeTextSecondary,
        fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    Image(bitmap, "QR-код для входа в LostFilm",
        Modifier.size(208.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(6.dp))
    Text(pairing.userCode, color = HomeAccentGold, fontSize = 28.sp,
        letterSpacing = 5.sp, fontWeight = FontWeight.Bold)
    Text("Код действует ещё ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
        color = HomeTextMuted, fontSize = 12.sp)
    Text(when (state) {
        is AuthUiState.WaitingForPhoneLogin -> "Телефон подключён. Завершите вход."
        is AuthUiState.VerifyingSession -> "Проверяем вход…"
        else -> "Ожидаем подключения телефона…"
    }, color = HomeTextSecondary, fontSize = 13.sp)
}

@Composable
private fun AuthStep(number: String, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(HomePanelSurfaceStrong, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Text(number, color = HomeAccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = HomeTextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AuthAction(label: String, focusKey: Any?, onClick: () -> Unit) {
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focusKey) { if (focusKey != null) requester.requestFocus() }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .focusRequester(requester).onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (focused) 2.dp else 1.dp, if (focused) FocusBorder else HomePanelBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (focused) FocusBackground else Color.Transparent,
            contentColor = TextPrimary,
        ),
    ) { Text(label) }
}
