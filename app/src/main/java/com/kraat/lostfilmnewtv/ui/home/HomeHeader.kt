@file:OptIn(ExperimentalFoundationApi::class)

package com.kraat.lostfilmnewtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kraat.lostfilmnewtv.R
import com.kraat.lostfilmnewtv.playback.history.PlaybackHistoryEntry
import com.kraat.lostfilmnewtv.playback.history.episodeAndPositionLabel
import com.kraat.lostfilmnewtv.ui.theme.HomeAccentGold
import com.kraat.lostfilmnewtv.ui.theme.FocusBorder
import com.kraat.lostfilmnewtv.ui.theme.HomeTextSecondary
import com.kraat.lostfilmnewtv.ui.theme.SelectionSurface
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary

private val ExpandedMenuWidth = 168.dp
private val CollapsedMenuWidth = 56.dp
private val MenuLogoHeight = 44.dp
private val MenuLogoSpacing = 9.dp
private val MenuItemSpacing = 4.dp
private val MenuButtonHeight = 36.dp
private val ContinueButtonHeight = 66.dp
private val ContinueSubtitleStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
private val MenuButtonShape = RoundedCornerShape(10.dp)

private fun Modifier.homeMenuItemAppearance(selected: Boolean, focused: Boolean): Modifier =
    background(if (selected) SelectionSurface else Color.Transparent, MenuButtonShape)
        .then(if (focused) Modifier.border(1.5.dp, FocusBorder, MenuButtonShape) else Modifier)

@Composable
fun HomeHeader(
    selectedMode: HomeFeedMode,
    availableModes: List<HomeFeedMode>,
    onModeActivated: (HomeFeedMode) -> Unit,
    onHeaderInteraction: () -> Unit,
    onBackToContent: () -> Boolean,
    onSearchClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    selectedNavItem: NavItem,
    onNavItemSelected: (NavItem) -> Unit,
    onNavItemLongClick: (NavItem) -> Unit = {},
    updateVersionText: String?,
    isUpdateDownloading: Boolean,
    updateDownloadProgress: Int?,
    modeFocusRequesters: Map<HomeFeedMode, FocusRequester>,
    searchFocusRequester: FocusRequester,
    scheduleFocusRequester: FocusRequester,
    historyFocusRequester: FocusRequester,
    updateFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    downTarget: FocusRequester?,
    showLabels: Boolean,
    isVisualFocusEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onContinueWatching: (() -> Unit)? = null,
    continueWatchingEntry: PlaybackHistoryEntry? = null,
    isContinuingWatching: Boolean = false,
) {
    val visibleModes = remember(availableModes) {
        buildList {
            add(HomeFeedMode.AllNew)
            add(HomeFeedMode.Favorites)
            add(HomeFeedMode.FavoriteSeries)
            add(HomeFeedMode.Movies)
            add(HomeFeedMode.Series)
        }.filter { mode -> mode in availableModes }
    }
    val hasModeToggle = visibleModes.isNotEmpty()
    val firstModeRequester = if (hasModeToggle) modeFocusRequesters[visibleModes.first()] else null
    val lastModeRequester = if (hasModeToggle) modeFocusRequesters[visibleModes.last()] else null
    val hasUpdate = !updateVersionText.isNullOrBlank()
    val continueRequester = remember { FocusRequester() }
    val afterContinue = if (hasUpdate) updateFocusRequester else settingsFocusRequester
    val afterHistory = if (onContinueWatching != null) continueRequester else afterContinue
    val beforeUpdate = if (onContinueWatching != null) continueRequester else historyFocusRequester
    val updateActionLabel = when {
        isUpdateDownloading && updateDownloadProgress != null -> "Скачивание $updateDownloadProgress%"
        isUpdateDownloading -> "Скачивание"
        else -> "Обновить"
    }
    val updateStatusLabel = when {
        isUpdateDownloading && updateDownloadProgress != null -> "Загрузка $updateDownloadProgress%"
        isUpdateDownloading -> "Загрузка"
        else -> "Можно обновить"
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .then(if (showLabels) Modifier.width(ExpandedMenuWidth) else Modifier.width(CollapsedMenuWidth))
            .padding(vertical = 8.dp),
    ) {
        val regularButtonCount = visibleModes.size + 4 + if (hasUpdate) 1 else 0
        val modeGaps = (visibleModes.size - 1).coerceAtLeast(0)
        val menuGaps = 8 + if (hasUpdate) 1 else 0
        val requiredHeight = MenuLogoHeight + MenuLogoSpacing + ContinueButtonHeight +
            MenuButtonHeight * regularButtonCount + MenuItemSpacing * (modeGaps + menuGaps)
        val buttonHeight = if (showLabels && onContinueWatching != null && requiredHeight > maxHeight) {
            ((maxHeight - requiredHeight) / regularButtonCount.toFloat() + MenuButtonHeight).coerceAtLeast(30.dp)
        } else {
            MenuButtonHeight
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(MenuItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeMenuLogo(showLabels = showLabels)
            Spacer(modifier = Modifier.height(MenuLogoSpacing))
            if (hasModeToggle) {
                HomeHeaderModeSegmentedControl(
                    currentMode = selectedMode,
                    availableModes = visibleModes,
                    modeFocusRequesters = modeFocusRequesters,
                    leftEdgeRequester = null,
                    rightEdgeRequester = downTarget,
                    nextDownRequester = scheduleFocusRequester,
                    upEdgeRequester = settingsFocusRequester,
                    onModeClick = { mode ->
                        onHeaderInteraction()
                        onNavItemSelected(NavItem.HOME)
                        onModeActivated(mode)
                    },
                    onModeLongClick = { onNavItemLongClick(NavItem.HOME) },
                    onInteraction = onHeaderInteraction,
                    onBackClick = onBackToContent,
                    showLabels = showLabels,
                    buttonHeight = buttonHeight,
                    isVisualFocusEnabled = isVisualFocusEnabled,
                    modifier = Modifier.testTag("home-mode-control"),
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            HomeHeaderActionButton(
                label = "Расписание",
                subtitle = "Календарь",
                leadingIcon = HeaderActionIcon.Calendar,
                menuButtonHeight = buttonHeight,
                onClick = {
                    onNavItemSelected(NavItem.SCHEDULE)
                    onScheduleClick()
                },
                onLongClick = { onNavItemLongClick(NavItem.SCHEDULE) },
                onInteraction = onHeaderInteraction,
                onBackClick = onBackToContent,
                isPrimary = selectedNavItem == NavItem.SCHEDULE,
                isVisualFocusEnabled = isVisualFocusEnabled,
                minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                hideSubtitle = true,
                showLabel = showLabels,
                modifier = Modifier
                    .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                    .testTag("home-action-schedule")
                    .focusRequester(scheduleFocusRequester)
                    .focusProperties {
                        up = lastModeRequester ?: searchFocusRequester
                        down = searchFocusRequester
                        right = downTarget ?: FocusRequester.Default
                        left = FocusRequester.Cancel
                    },
            )
            HomeHeaderActionButton(
                label = "Поиск",
                subtitle = "Сериал или фильм",
                leadingIcon = HeaderActionIcon.Search,
                menuButtonHeight = buttonHeight,
                onClick = {
                    onNavItemSelected(NavItem.SEARCH)
                    onSearchClick()
                },
                onLongClick = { onNavItemLongClick(NavItem.SEARCH) },
                onInteraction = onHeaderInteraction,
                onBackClick = onBackToContent,
                isPrimary = selectedNavItem == NavItem.SEARCH,
                isVisualFocusEnabled = isVisualFocusEnabled,
                minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                hideSubtitle = true,
                showLabel = showLabels,
                modifier = Modifier
                    .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                    .testTag("home-action-search")
                    .focusRequester(searchFocusRequester)
                    .focusProperties {
                        up = scheduleFocusRequester
                        down = historyFocusRequester
                        left = FocusRequester.Cancel
                        right = downTarget ?: FocusRequester.Default
                    },
            )
            HomeHeaderActionButton(
                label = "История",
                subtitle = "Недавние просмотры",
                leadingIcon = HeaderActionIcon.History,
                menuButtonHeight = buttonHeight,
                onClick = {
                    onNavItemSelected(NavItem.HISTORY)
                    onHistoryClick()
                },
                onInteraction = onHeaderInteraction,
                onBackClick = onBackToContent,
                isPrimary = selectedNavItem == NavItem.HISTORY,
                isVisualFocusEnabled = isVisualFocusEnabled,
                minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                hideSubtitle = true,
                showLabel = showLabels,
                modifier = Modifier
                    .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                    .testTag("home-action-history")
                    .focusRequester(historyFocusRequester)
                    .focusProperties {
                        up = searchFocusRequester
                        down = afterHistory
                        left = FocusRequester.Cancel
                        right = downTarget ?: FocusRequester.Default
                    },
            )
            if (onContinueWatching != null) {
                HomeHeaderActionButton(
                    label = if (isContinuingWatching) "Загружаем серию…" else "Продолжить просмотр",
                    subtitle = continueWatchingEntry?.title.orEmpty(),
                    episodePosition = continueWatchingEntry?.episodeAndPositionLabel(),
                    leadingIcon = HeaderActionIcon.Play,
                    onClick = onContinueWatching,
                    onInteraction = onHeaderInteraction,
                    onBackClick = onBackToContent,
                    isVisualFocusEnabled = isVisualFocusEnabled,
                    minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                    hideSubtitle = true,
                    showLabel = showLabels,
                    wrapLabel = true,
                    modifier = Modifier
                        .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                        .testTag("home-action-continue")
                        .focusRequester(continueRequester)
                        .focusProperties {
                            up = historyFocusRequester
                            down = afterContinue
                            left = FocusRequester.Cancel
                            right = downTarget ?: FocusRequester.Default
                        },
                )
            }
            if (hasUpdate) {
                HomeHeaderActionButton(
                    label = updateActionLabel,
                    subtitle = updateVersionText.orEmpty(),
                    statusLabel = updateStatusLabel,
                    leadingIcon = HeaderActionIcon.Refresh,
                    menuButtonHeight = buttonHeight,
                    onClick = {
                        onNavItemSelected(NavItem.UPDATE)
                        onUpdateClick()
                    },
                    onLongClick = { onNavItemLongClick(NavItem.UPDATE) },
                    onInteraction = onHeaderInteraction,
                    onBackClick = onBackToContent,
                    isPrimary = selectedNavItem == NavItem.UPDATE,
                    isVisualFocusEnabled = isVisualFocusEnabled,
                    minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                    hideSubtitle = true,
                    showLabel = showLabels,
                    modifier = Modifier
                        .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                        .testTag("home-action-update")
                        .focusRequester(updateFocusRequester)
                        .focusProperties {
                            up = beforeUpdate
                            down = settingsFocusRequester
                            left = FocusRequester.Cancel
                            right = downTarget ?: FocusRequester.Default
                        },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            HomeHeaderActionButton(
                label = "Настройки",
                subtitle = "Приложение",
                leadingIcon = HeaderActionIcon.Settings,
                menuButtonHeight = buttonHeight,
                onClick = {
                    onNavItemSelected(NavItem.SETTINGS)
                    onSettingsClick()
                },
                onLongClick = { onNavItemLongClick(NavItem.SETTINGS) },
                onInteraction = onHeaderInteraction,
                onBackClick = onBackToContent,
                isPrimary = selectedNavItem == NavItem.SETTINGS,
                isVisualFocusEnabled = isVisualFocusEnabled,
                minWidth = if (showLabels) ExpandedMenuWidth - 36.dp else CollapsedMenuWidth - 20.dp,
                hideSubtitle = true,
                showLabel = showLabels,
                modifier = Modifier
                    .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp))
                    .testTag("home-action-settings")
                    .focusRequester(settingsFocusRequester)
                    .focusProperties {
                        up = if (hasUpdate) updateFocusRequester else beforeUpdate
                        down = firstModeRequester ?: scheduleFocusRequester
                        left = FocusRequester.Cancel
                        right = downTarget ?: FocusRequester.Default
                    },
            )

        }
    }
}

@Composable
private fun HomeMenuLogo(showLabels: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MenuLogoHeight)
            .padding(horizontal = if (showLabels) 12.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                id = if (showLabels) R.drawable.lostfilm_wordmark else R.drawable.ic_lf_logo,
            ),
            contentDescription = "Логотип LostFilm",
            contentScale = if (showLabels) ContentScale.Crop else ContentScale.Fit,
            modifier = if (showLabels) Modifier.fillMaxWidth().height(MenuLogoHeight).clipToBounds()
                else Modifier.size(34.dp),
        )
    }
}

@Composable
private fun HomeHeaderModeSegmentedControl(
    currentMode: HomeFeedMode,
    availableModes: List<HomeFeedMode>,
    modeFocusRequesters: Map<HomeFeedMode, FocusRequester>,
    leftEdgeRequester: FocusRequester?,
    rightEdgeRequester: FocusRequester?,
    nextDownRequester: FocusRequester,
    upEdgeRequester: FocusRequester?,
    onModeClick: (HomeFeedMode) -> Unit,
    onModeLongClick: (HomeFeedMode) -> Unit,
    onInteraction: () -> Unit,
    onBackClick: () -> Boolean,
    showLabels: Boolean,
    buttonHeight: Dp,
    isVisualFocusEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = MenuButtonShape

    Column(
        modifier = modifier
            .then(if (showLabels) Modifier.fillMaxWidth() else Modifier.width(46.dp)),
        verticalArrangement = Arrangement.spacedBy(MenuItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        availableModes.forEachIndexed { index, mode ->
            val requester = modeFocusRequesters.getValue(mode)
            val leftRequester = leftEdgeRequester
            val downRequester = if (index == availableModes.lastIndex) {
                nextDownRequester
            } else {
                modeFocusRequesters[availableModes[index + 1]]
            }
            HomeModeSegmentButton(
                mode = mode,
                label = mode.segmentLabel(),
                selected = currentMode == mode,
                focusRequester = requester,
                leftRequester = leftRequester,
                rightRequester = rightEdgeRequester,
                downTarget = downRequester,
                upTarget = if (index == 0) upEdgeRequester else modeFocusRequesters[availableModes[index - 1]],
                onClick = { onModeClick(mode) },
                onLongClick = { onModeLongClick(mode) },
                onInteraction = onInteraction,
                onBackClick = onBackClick,
                showLabel = showLabels,
                buttonHeight = buttonHeight,
                isVisualFocusEnabled = isVisualFocusEnabled,
                modifier = if (showLabels) Modifier.fillMaxWidth() else Modifier.width(CollapsedMenuWidth - 20.dp),
            )
        }
    }
}

@Composable
private fun HomeModeSegmentButton(
    mode: HomeFeedMode,
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    leftRequester: FocusRequester?,
    rightRequester: FocusRequester?,
    downTarget: FocusRequester?,
    upTarget: FocusRequester?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInteraction: () -> Unit,
    onBackClick: () -> Boolean,
    showLabel: Boolean,
    buttonHeight: Dp,
    isVisualFocusEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val showFocusedStyle = isFocused && isVisualFocusEnabled
    val active = selected || showFocusedStyle
    val contentColor = if (active) TextPrimary else HomeTextSecondary
    Box(
        modifier = modifier
            .height(buttonHeight)
            .testTag(if (selected) "home-mode-toggle" else "home-mode-${mode.storageValue}")
            .focusRequester(focusRequester)
            .focusProperties {
                left = leftRequester ?: FocusRequester.Cancel
                rightRequester?.let { right = it }
                upTarget?.let { up = it }
                downTarget?.let { down = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionRight && event.type == KeyEventType.KeyDown) {
                    onInteraction()
                    return@onPreviewKeyEvent onBackClick()
                }
                if (event.key.isHeaderActivationKey()) {
                    if (event.type == KeyEventType.KeyDown) {
                        onInteraction()
                        onClick()
                    }
                    return@onPreviewKeyEvent true
                }
                if (event.type == KeyEventType.KeyDown && event.key.isHeaderInteractionKey()) {
                    onInteraction()
                }
                false
            }
            .homeMenuItemAppearance(selected, showFocusedStyle)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onInteraction()
            }
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .then(if (showLabel) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = if (showLabel) 15.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderModeIcon(mode = mode, color = contentColor)
            if (showLabel) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class HeaderActionIcon {
    Search,
    Calendar,
    History,
    Play,
    Refresh,
    Settings,
}

@Composable
private fun HomeHeaderActionButton(
    label: String,
    subtitle: String,
    statusLabel: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onInteraction: () -> Unit = {},
    onBackClick: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    observeKeyInteractions: Boolean = true,
    minWidth: androidx.compose.ui.unit.Dp = 156.dp,
    compact: Boolean = false,
    hideSubtitle: Boolean = false,
    leadingIcon: HeaderActionIcon? = null,
    showLabel: Boolean = true,
    isVisualFocusEnabled: Boolean = true,
    wrapLabel: Boolean = false,
    episodePosition: String? = null,
    menuButtonHeight: Dp = MenuButtonHeight,
) {
    var isFocused by remember { mutableStateOf(false) }
    val showFocusedStyle = isFocused && isVisualFocusEnabled
    val textColor = if (isPrimary || showFocusedStyle) TextPrimary else HomeTextSecondary
    val subtitleColor = if (isPrimary || showFocusedStyle) HomeAccentGold else HomeTextSecondary
    val buttonHeight = when {
        showLabel && episodePosition != null -> ContinueButtonHeight
        compact -> 38.dp
        else -> menuButtonHeight
    }

    Box(
        modifier = modifier
            .then(
                if (observeKeyInteractions) {
                    Modifier.onPreviewKeyEvent { event ->
                        if (event.key == Key.DirectionRight && event.type == KeyEventType.KeyDown) {
                            onInteraction()
                            return@onPreviewKeyEvent onBackClick()
                        }
                        if (event.key.isHeaderActivationKey()) {
                            if (event.type == KeyEventType.KeyDown) {
                                onInteraction()
                                onClick()
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (event.type == KeyEventType.KeyDown && event.key.isHeaderInteractionKey()) {
                            onInteraction()
                        }
                        false
                    }
                } else {
                    Modifier
                },
            )
            .height(buttonHeight)
            .widthIn(min = minWidth)
            .homeMenuItemAppearance(isPrimary, showFocusedStyle)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onInteraction()
                }
            }
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = if (!showLabel) 0.dp else if (compact) 11.dp else 15.dp,
                vertical = if (compact) 3.dp else 3.dp,
            ),
        contentAlignment = if (showLabel) Alignment.CenterStart else Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { icon ->
                HeaderVectorIcon(icon = icon, color = textColor)
            }
            if (showLabel) {
                Column {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = if (compact || wrapLabel) 14.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (wrapLabel) 2 else 1,
                        lineHeight = if (wrapLabel) 14.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (episodePosition != null) {
                        Text(
                            text = subtitle,
                            color = subtitleColor,
                            modifier = Modifier.height(14.dp),
                            style = ContinueSubtitleStyle,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = episodePosition,
                            color = subtitleColor,
                            modifier = Modifier.height(14.dp),
                            style = ContinueSubtitleStyle,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (showLabel) statusLabel?.let { status ->
                Text(
                    text = status,
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0f),
                    color = subtitleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showLabel && episodePosition == null) {
                Text(
                    text = subtitle,
                    modifier = if (hideSubtitle) {
                        Modifier
                            .size(1.dp)
                            .alpha(0f)
                    } else {
                        Modifier
                    },
                    color = subtitleColor,
                    fontSize = if (compact) 8.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HeaderVectorIcon(icon: HeaderActionIcon, color: Color) {
    when (icon) {
        HeaderActionIcon.Search -> SearchIcon(color = color)
        HeaderActionIcon.Calendar -> CalendarIcon(color = color)
        HeaderActionIcon.History -> HistoryIcon(color = color)
        HeaderActionIcon.Play -> androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val triangle = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * .28f, size.height * .17f)
                lineTo(size.width * .82f, size.height * .50f)
                lineTo(size.width * .28f, size.height * .83f)
                close()
            }
            drawPath(triangle, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
        }
        HeaderActionIcon.Refresh -> RefreshIcon(color = color)
        HeaderActionIcon.Settings -> SettingsLineIcon(color = color)
    }
}

@Composable
private fun HeaderModeIcon(mode: HomeFeedMode, color: Color) {
    when (mode) {
        HomeFeedMode.AllNew -> SparklesIcon(color = color)
        HomeFeedMode.Favorites -> HeartIcon(color = color)
        HomeFeedMode.FavoriteSeries -> HeartIcon(color = color)
        HomeFeedMode.Movies -> ClapperIcon(color = color)
        HomeFeedMode.Series -> TvIcon(color = color)
    }
}

@Composable
private fun SparklesIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp).offset(x = 1.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val main = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.33f, size.height * 0.16f)
            cubicTo(size.width * 0.35f, size.height * 0.38f, size.width * 0.41f, size.height * 0.46f, size.width * 0.63f, size.height * 0.50f)
            cubicTo(size.width * 0.41f, size.height * 0.54f, size.width * 0.35f, size.height * 0.62f, size.width * 0.33f, size.height * 0.84f)
            cubicTo(size.width * 0.31f, size.height * 0.62f, size.width * 0.25f, size.height * 0.54f, size.width * 0.03f, size.height * 0.50f)
            cubicTo(size.width * 0.25f, size.height * 0.46f, size.width * 0.31f, size.height * 0.38f, size.width * 0.33f, size.height * 0.16f)
        }
        drawPath(path = main, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(size.width * 0.72f, size.height * 0.16f),
            end = Offset(size.width * 0.72f, size.height * 0.32f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.24f),
            end = Offset(size.width * 0.80f, size.height * 0.24f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun HeartIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp).offset(x = 1.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.1f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.80f)
            cubicTo(size.width * 0.23f, size.height * 0.64f, size.width * 0.13f, size.height * 0.46f, size.width * 0.18f, size.height * 0.31f)
            cubicTo(size.width * 0.24f, size.height * 0.13f, size.width * 0.46f, size.height * 0.13f, size.width * 0.50f, size.height * 0.34f)
            cubicTo(size.width * 0.54f, size.height * 0.13f, size.width * 0.76f, size.height * 0.13f, size.width * 0.82f, size.height * 0.31f)
            cubicTo(size.width * 0.87f, size.height * 0.46f, size.width * 0.77f, size.height * 0.64f, size.width * 0.50f, size.height * 0.80f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
private fun ClapperIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val left = size.width * 0.18f
        val top = size.height * 0.38f
        val right = size.width * 0.82f
        val bottom = size.height * 0.78f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            style = stroke,
        )
        drawLine(color, Offset(left, top), Offset(right, size.height * 0.25f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.34f, size.height * 0.35f), Offset(size.width * 0.45f, size.height * 0.30f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.58f, size.height * 0.30f), Offset(size.width * 0.70f, size.height * 0.27f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(left, size.height * 0.51f), Offset(right, size.height * 0.51f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun TvIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.30f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            style = stroke,
        )
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.84f), Offset(size.width * 0.65f, size.height * 0.84f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.72f), Offset(size.width * 0.50f, size.height * 0.84f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.38f, size.height * 0.22f), Offset(size.width * 0.50f, size.height * 0.30f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.62f, size.height * 0.22f), Offset(size.width * 0.50f, size.height * 0.30f), strokeWidth = 2.0f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun FavoriteSeriesIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.18f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.42f, size.height * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            style = stroke,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.38f, size.height * 0.24f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.42f, size.height * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.46f, size.height * 0.42f),
            end = Offset(size.width * 0.54f, size.height * 0.50f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.54f, size.height * 0.50f),
            end = Offset(size.width * 0.72f, size.height * 0.34f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun SearchIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.1f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.25f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.61f, size.height * 0.61f),
            end = Offset(size.width * 0.83f, size.height * 0.83f),
            strokeWidth = 2.1f,
        )
    }
}



@Composable
private fun CalendarIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.1f)
        val left = size.width * 0.18f
        val top = size.height * 0.22f
        val right = size.width * 0.82f
        val bottom = size.height * 0.82f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(left, size.height * 0.40f),
            end = Offset(right, size.height * 0.40f),
            strokeWidth = 2.1f,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.34f, size.height * 0.14f),
            end = Offset(size.width * 0.34f, size.height * 0.28f),
            strokeWidth = 2.1f,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.66f, size.height * 0.14f),
            end = Offset(size.width * 0.66f, size.height * 0.28f),
            strokeWidth = 2.1f,
        )
    }
}

@Composable
private fun HistoryIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.width * 0.045f
        drawCircle(
            color = color,
            radius = size.width * 0.34f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(color, center.copy(y = size.height * 0.28f), center, strokeWidth = stroke)
        drawLine(color, center, Offset(size.width * 0.67f, size.height * 0.59f), strokeWidth = stroke)
    }
}

@Composable
private fun RefreshIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val arcSize = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.66f)
        val arcTopLeft = Offset(size.width * 0.17f, size.height * 0.17f)
        drawArc(
            color = color,
            startAngle = 205f,
            sweepAngle = 205f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.76f, size.height * 0.19f),
            end = Offset(size.width * 0.76f, size.height * 0.36f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.76f, size.height * 0.19f),
            end = Offset(size.width * 0.58f, size.height * 0.19f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawArc(
            color = color,
            startAngle = 25f,
            sweepAngle = 205f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.24f, size.height * 0.81f),
            end = Offset(size.width * 0.24f, size.height * 0.64f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.24f, size.height * 0.81f),
            end = Offset(size.width * 0.42f, size.height * 0.81f),
            strokeWidth = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsLineIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.0f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val cx = size.width * 0.50f
        val cy = size.height * 0.50f
        val teeth = 8
        val inner = size.minDimension * 0.31f
        val outer = size.minDimension * 0.42f
        val toothHalfAngle = Math.PI / 18.0
        val gapHalfAngle = Math.PI / teeth - toothHalfAngle
        val gear = androidx.compose.ui.graphics.Path()
        for (index in 0 until teeth) {
            val center = -Math.PI / 2.0 + index * 2.0 * Math.PI / teeth
            val points = listOf(
                center - gapHalfAngle to inner,
                center - toothHalfAngle to outer,
                center + toothHalfAngle to outer,
                center + gapHalfAngle to inner,
            )
            points.forEach { (angle, radius) ->
                val point = Offset(
                    x = cx + kotlin.math.cos(angle).toFloat() * radius,
                    y = cy + kotlin.math.sin(angle).toFloat() * radius,
                )
                if (index == 0 && angle == points.first().first) {
                    gear.moveTo(point.x, point.y)
                } else {
                    gear.lineTo(point.x, point.y)
                }
            }
        }
        gear.close()
        drawPath(path = gear, color = color, style = stroke)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.14f,
            center = Offset(cx, cy),
            style = stroke,
        )
    }
}

private fun HomeFeedMode.segmentLabel(): String {
    return when (this) {
        HomeFeedMode.AllNew -> "Новые"
        HomeFeedMode.Favorites -> "Избранное"
        HomeFeedMode.FavoriteSeries -> "Избранное"
        HomeFeedMode.Movies -> "Фильмы"
        HomeFeedMode.Series -> "Сериалы"
    }
}

private fun Key.isHeaderInteractionKey(): Boolean {
    return when (this) {
        Key.DirectionUp,
        Key.DirectionDown,
        Key.DirectionLeft,
        Key.DirectionRight,
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter,
        -> true

        else -> false
    }
}

private fun Key.isHeaderActivationKey(): Boolean {
    return when (this) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter,
        -> true

        else -> false
    }
}
