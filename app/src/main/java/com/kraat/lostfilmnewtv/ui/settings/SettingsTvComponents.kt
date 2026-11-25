package com.kraat.lostfilmnewtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kraat.lostfilmnewtv.ui.theme.BackgroundSurface
import com.kraat.lostfilmnewtv.ui.theme.FocusBackground
import com.kraat.lostfilmnewtv.ui.theme.FocusBorder
import com.kraat.lostfilmnewtv.ui.theme.HomeAccentGold
import com.kraat.lostfilmnewtv.ui.theme.HomeAccentGoldGlow
import com.kraat.lostfilmnewtv.ui.theme.HomePanelBorder
import com.kraat.lostfilmnewtv.ui.theme.HomePanelSurface
import com.kraat.lostfilmnewtv.ui.theme.HomeTextMuted
import com.kraat.lostfilmnewtv.ui.theme.HomeTextSecondary
import com.kraat.lostfilmnewtv.ui.theme.SelectionSurface
import com.kraat.lostfilmnewtv.ui.theme.TextPrimary

private val SettingsControlShape = RoundedCornerShape(10.dp)

@Composable
fun SettingsTvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    summary: String? = null,
    tag: String? = null,
    summaryTag: String? = null,
    onFocused: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isSelected -> SelectionSurface
        isFocused && enabled -> FocusBackground
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> HomeTextMuted
        isSelected || isFocused -> TextPrimary
        else -> HomeTextSecondary
    }
    val summaryColor = if (enabled && isSelected) HomeAccentGoldGlow else HomeTextMuted

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SettingsControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = textColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFocused && enabled) Modifier.border(2.dp, FocusBorder, SettingsControlShape) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused?.invoke()
            }
            .semantics { selected = isSelected }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    color = summaryColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (summaryTag != null) Modifier.testTag(summaryTag) else Modifier,
                )
            }
        }
    }
}

@Composable
fun SettingsOverviewCard(
    title: String,
    subtitle: String? = null,
    tag: String = "settings-overview-card",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
            .focusable(false)
            .background(BackgroundSurface, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = HomeTextMuted,
                    fontSize = 14.sp,
                )
            }
        }
        content()
    }
}

@Composable
fun SettingsOverviewValue(text: String) {
    Text(
        text = text,
        color = HomeTextSecondary,
        fontSize = 15.sp,
    )
}

@Composable
fun SettingsRowButton(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    tag: String? = null,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onFocused: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isSelected -> SelectionSurface
        isFocused && enabled -> FocusBackground
        else -> Color.Transparent
    }
    val valueColor = when {
        !enabled -> HomeTextMuted
        isSelected -> HomeAccentGoldGlow
        else -> HomeAccentGold
    }
    val textColor = if (enabled) TextPrimary else HomeTextMuted

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SettingsControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = textColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFocused && enabled) Modifier.border(2.dp, FocusBorder, SettingsControlShape) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused?.invoke()
            }
            .semantics { selected = isSelected }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = HomeTextMuted,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onFocused: (() -> Unit)? = null,
    isPassword: Boolean = false,
    sectionRequester: FocusRequester,
    downRequester: FocusRequester,
    upRequester: FocusRequester? = null,
) {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (editing) {
            withFrameNanos { }
            keyboard?.show()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !editing,
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
        label = { Text(text = label) },
        shape = SettingsControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = HomeAccentGoldGlow,
            unfocusedLabelColor = HomeTextMuted,
            cursorColor = HomeAccentGoldGlow,
            focusedBorderColor = FocusBorder,
            unfocusedBorderColor = HomePanelBorder,
            focusedContainerColor = FocusBackground,
            unfocusedContainerColor = HomePanelSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || (editing && ime.getBottom(density) > 0)) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionUp -> {
                        upRequester?.requestFocus(FocusDirection.Up)
                        true
                    }
                    Key.DirectionDown -> downRequester.requestFocus(FocusDirection.Down)
                    Key.DirectionLeft -> sectionRequester.requestFocus(FocusDirection.Left)
                    Key.DirectionCenter, Key.Enter -> {
                        if (editing) keyboard?.show() else editing = true
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged {
                if (it.isFocused) onFocused?.invoke() else editing = false
            }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}
