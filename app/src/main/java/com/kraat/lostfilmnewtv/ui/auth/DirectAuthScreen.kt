package com.kraat.lostfilmnewtv.ui.auth

import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kraat.lostfilmnewtv.data.auth.DIRECT_LOGIN_BASE_URL
import com.kraat.lostfilmnewtv.data.auth.DIRECT_LOGIN_URL
import com.kraat.lostfilmnewtv.data.auth.isDirectLoginUrl
import com.kraat.lostfilmnewtv.data.network.OkHttpLostFilmHttpClient

@Composable
fun DirectAuthScreen(
    onAuthComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DirectAuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var browser by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val cancel = {
        viewModel.cancelLogin()
        onNavigateBack()
    }
    BackHandler(onBack = cancel)

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthComplete()
    }
    DisposableEffect(Unit) {
        onDispose {
            val previousBrowser = browser
            browser = null
            previousBrowser?.apply {
                stopLoading()
                clearHistory()
                destroy()
            }
            // The verified session is kept only in EncryptedSessionStore, not in a browser jar.
            runCatching { CookieManager.getInstance().removeAllCookies(null) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Вход в LostFilm", style = MaterialTheme.typography.headlineSmall)
                Text("lostfilm.tv · вход на телевизоре", color = MaterialTheme.colorScheme.primary)
                Text("Введите почту, пароль и код с картинки. Затем нажмите «Я вошёл».")
            }
            Button(
                enabled = browser != null && !isLoading && !state.isVerifying,
                onClick = {
                    viewModel.completeLogin(CookieManager.getInstance().getCookie(DIRECT_LOGIN_BASE_URL))
                },
            ) { Text(if (state.isVerifying) "Проверяем вход…" else "Я вошёл") }
            OutlinedButton(onClick = { browser?.loadUrl(DIRECT_LOGIN_URL) }, enabled = !state.isVerifying) {
                Text("Обновить")
            }
            OutlinedButton(onClick = cancel) { Text("Назад") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (isLoading || state.isVerifying) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                FrameLayout(context).apply {
                    try {
                        val webView = WebView(context)
                        browser = webView
                        webView.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = OkHttpLostFilmHttpClient.USER_AGENT_PUBLIC
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        webView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                        webView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val blocked = !isDirectLoginUrl(request.url.toString())
                                if (blocked && request.isForMainFrame) {
                                    viewModel.showBrowserError("Используйте вход по почте и паролю на lostfilm.tv.")
                                }
                                return blocked
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    viewModel.showBrowserError("Не удалось открыть lostfilm.tv. Проверьте интернет и нажмите «Обновить».")
                                }
                            }

                            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                                handler.cancel()
                                isLoading = false
                                viewModel.showBrowserError("Не удалось проверить защищённое соединение с lostfilm.tv.")
                            }
                        }
                        addView(webView, FrameLayout.LayoutParams(-1, -1))
                        val cookies = CookieManager.getInstance()
                        cookies.setAcceptCookie(true)
                        cookies.setAcceptThirdPartyCookies(webView, false)
                        // Start a fresh login, including after logout or an interrupted sign-in.
                        cookies.removeAllCookies {
                            if (browser === webView) {
                                webView.loadUrl(DIRECT_LOGIN_URL)
                                webView.requestFocus()
                            }
                        }
                    } catch (_: Exception) {
                        isLoading = false
                        post { viewModel.showBrowserError("На устройстве недоступен Android System WebView. Установите или обновите его для входа.") }
                    }
                }
            },
        )
    }
}
