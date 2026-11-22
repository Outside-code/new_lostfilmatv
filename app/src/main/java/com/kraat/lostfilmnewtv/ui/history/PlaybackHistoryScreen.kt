package com.kraat.lostfilmnewtv.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryEntry
import com.kraat.lostfilmnewtv.ui.components.PosterImage
import com.kraat.lostfilmnewtv.ui.home.requestFocusWhenReady
import com.kraat.lostfilmnewtv.ui.theme.BackgroundPrimary
import com.kraat.lostfilmnewtv.ui.theme.DetailsSurfaceReadable
import com.kraat.lostfilmnewtv.ui.theme.DetailsTextSecondary
import com.kraat.lostfilmnewtv.ui.theme.HomePanelBorderFocus
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val historyDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm")

@Composable
fun PlaybackHistoryScreen(
    entries: List<PlaybackHistoryEntry>,
    onOpenDetails: (String) -> Unit,
) {
    var lastOpenedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val focusKey = entries.firstOrNull { it.key == lastOpenedKey }?.key ?: entries.firstOrNull()?.key
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val index = entries.indexOfFirst { it.key == focusKey }
        if (index >= 0) {
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                listState.scrollToItem(index)
            }
            requestFocusWhenReady(focusRequester)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary)
            .padding(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("История просмотра", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Здесь появятся фильмы и серии, которые вы запустите",
                    color = DetailsTextSecondary,
                    fontSize = 18.sp,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.testTag("playback-history-list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries, key = { it.key }) { entry ->
                    val date = remember(entry.startedAt) {
                        Instant.ofEpochMilli(entry.startedAt).atZone(ZoneId.systemDefault()).format(historyDateFormat)
                    }
                    Button(
                        onClick = {
                            lastOpenedKey = entry.key
                            onOpenDetails(entry.detailsUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                            .then(if (entry.key == focusKey) Modifier.focusRequester(focusRequester) else Modifier),
                        colors = ButtonDefaults.colors(
                            containerColor = DetailsSurfaceReadable,
                            contentColor = TextPrimary,
                            focusedContainerColor = HomePanelBorderFocus,
                            focusedContentColor = TextPrimary,
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1f),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            PosterImage(
                                model = entry.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(6.dp)),
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.title, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (entry.subtitle.isNotBlank()) {
                                    Text(entry.subtitle, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Text(if (entry.completed) "Досмотрено" else "Запущено", fontSize = 14.sp)
                            Text(date, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
