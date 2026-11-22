package com.kraat.lostfilmnewtv.ui.search

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.kraat.lostfilmnewtv.ui.theme.LostFilmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchRemoteTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun voiceButtonStartsWithCenterAndCanBeReachedFromTextField() {
        var voiceClicks = 0
        composeRule.setContent {
            LostFilmTheme { SearchScreen(SearchUiState(), {}, {}, {}, {}, onVoiceSearch = { voiceClicks++ }) }
        }
        composeRule.onNodeWithText("Найти").assertDoesNotExist()
        composeRule.onNodeWithTag("search-voice").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, voiceClicks) }
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag("search-query-input").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("search-voice").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(2, voiceClicks) }
    }
}
