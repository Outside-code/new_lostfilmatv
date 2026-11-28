package com.kraat.lostfilmnewtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.kraat.lostfilmnewtv.data.model.ReleaseSummary
import com.kraat.lostfilmnewtv.playback.history.isSeriesEpisodeUrl
import com.kraat.lostfilmnewtv.ui.theme.DetailsAccentBlue
import com.kraat.lostfilmnewtv.ui.theme.DetailsBorderDefault
import com.kraat.lostfilmnewtv.ui.theme.DetailsSurfaceSoft
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurfaceStrong
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary
import androidx.compose.ui.graphics.Color

internal enum class HomePosterAction(val label: String) {
    AddFavorite("Добавить в избранное"),
    OpenTrailer("Открыть трейлер"),
    MarkWatched("Отметить серию просмотренной"),
}

@Composable
internal fun HomePosterActionsDialog(
    item: ReleaseSummary,
    onDismiss: () -> Unit,
    onAction: (HomePosterAction) -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    val actions = HomePosterAction.entries.filter { action ->
        action != HomePosterAction.MarkWatched || (!item.isWatched && item.detailsUrl.isSeriesEpisodeUrl())
    }
    val panelShape = RoundedCornerShape(20.dp)

    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(firstAction) { requestFocusWhenReady(firstAction) }
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(HomePanelSurfaceStrong, panelShape)
                .border(1.dp, DetailsBorderDefault, panelShape)
                .padding(20.dp)
                .testTag("home-poster-actions"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.titleRu,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            actions.forEachIndexed { index, action ->
                Button(
                    onClick = { onAction(action) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstAction) else Modifier)
                        .testTag("home-poster-action-${action.name}"),
                    colors = ButtonDefaults.colors(
                        containerColor = DetailsSurfaceSoft,
                        contentColor = TextPrimary,
                        focusedContainerColor = DetailsAccentBlue,
                        focusedContentColor = Color(0xFF081222),
                    ),
                ) {
                    Text(action.label, fontSize = 16.sp)
                }
            }
        }
    }
}
