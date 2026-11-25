package com.kraat.lostfilmnewtv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kraat.lostfilmnewtv.R
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun PosterImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    catalogPreviewUrl: String? = null,
    placeholderTitle: String? = null,
) {
    val effectiveModel = when (model) {
        is String -> model.takeIf { it.isNotBlank() }
        is ImageRequest -> {
            val data = model.data
            if (data is String && data.isBlank()) null else model
        }
        null -> null
        else -> model
    }

    var posterReady by remember(effectiveModel) { mutableStateOf(false) }
    var posterFailed by remember(effectiveModel) { mutableStateOf(false) }
    var previewAllowed by remember(catalogPreviewUrl) { mutableStateOf(false) }

    LaunchedEffect(effectiveModel, catalogPreviewUrl, posterReady, posterFailed) {
        if (catalogPreviewUrl == null || posterReady || previewAllowed) return@LaunchedEffect
        if (effectiveModel != null && !posterFailed) delay(350)
        previewAllowed = true
    }

    Box(modifier) {
        if (!posterReady) {
            PosterPlaceholder(Modifier.fillMaxSize(), placeholderTitle)
            if (catalogPreviewUrl != null && previewAllowed) {
                AsyncImage(
                    model = catalogPreviewUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(bottom = 42.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        if (effectiveModel != null) {
            AsyncImage(
                model = effectiveModel,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alpha = alpha,
                onLoading = {
                    posterReady = false
                    posterFailed = false
                },
                onSuccess = { posterReady = true },
                onError = {
                    posterReady = false
                    posterFailed = true
                },
            )
        }
    }
}

@Composable
private fun PosterPlaceholder(modifier: Modifier, title: String?) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (title == null) {
            Image(
                painter = painterResource(R.drawable.ic_lf_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.3f),
            )
        } else {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp).clearAndSetSemantics { },
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
