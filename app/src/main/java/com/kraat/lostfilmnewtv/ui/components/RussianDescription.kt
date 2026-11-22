package com.kraat.lostfilmnewtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

fun interface DescriptionTranslator {
    suspend fun translate(text: String): String?
}

val LocalDescriptionTranslator = staticCompositionLocalOf<DescriptionTranslator?> { null }

internal fun String.containsRussianText(): Boolean = any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }

@Composable
fun rememberRussianDescription(text: String, russianFallback: String = ""): String {
    if (text.isBlank() || text.containsRussianText()) return text
    val translator = LocalDescriptionTranslator.current ?: return text
    val fallback = russianFallback.takeIf { it.containsRussianText() }.orEmpty()
    val translated by produceState(
        initialValue = fallback.ifBlank { "Переводим описание…" },
        key1 = text, key2 = translator,
    ) {
        value = fallback.ifBlank { "Переводим описание…" }
        delay(250)
        value = try {
            translator.translate(text)?.takeIf { it.containsRussianText() }
                ?: fallback.ifBlank { "Описание на русском пока недоступно" }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            fallback.ifBlank { "Описание на русском пока недоступно" }
        }
    }
    return translated
}
