package com.kraat.lostfilmnewtv.ui.schedule

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.kraat.lostfilmnewtv.data.model.ReleaseKind
import com.kraat.lostfilmnewtv.data.model.ScheduleDay
import com.kraat.lostfilmnewtv.data.model.ScheduleItem
import com.kraat.lostfilmnewtv.data.model.ScheduleMonth
import com.kraat.lostfilmnewtv.ui.theme.LostFilmTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalTestApi::class)
class ScheduleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun posterUpdateKeepsFocusOnTheCurrentRow() {
        val state = mutableStateOf(ScheduleUiState(schedule = schedule()))
        composeRule.setContent {
            LostFilmTheme {
                ScheduleScreen(state = state.value, onRetry = {}, onOpenItem = {})
            }
        }

        pressDownTwice()
        composeRule.runOnIdle {
            val schedule = state.value.schedule!!
            val firstDay = schedule.days.first()
            val updatedDay = firstDay.copy(
                items = listOf(firstDay.items.first().copy(posterUrl = "https://example.test/poster.jpg")),
            )
            state.value = state.value.copy(schedule = schedule.copy(days = listOf(updatedDay) + schedule.days.drop(1)))
        }

        composeRule.onNodeWithText("Release 3").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("Release 4").assertIsFocused()
    }

    @Test
    fun returningFromAReleaseRestoresItsRowAcrossDayBoundaries() {
        val state = ScheduleUiState(schedule = schedule())
        val showSchedule = mutableStateOf(true)
        composeRule.setContent {
            val holder = rememberSaveableStateHolder()
            LostFilmTheme {
                if (showSchedule.value) {
                    holder.SaveableStateProvider("schedule") {
                        ScheduleScreen(state = state, onRetry = {}, onOpenItem = { showSchedule.value = false })
                    }
                }
            }
        }

        pressDownTwice()
        composeRule.onNodeWithText("Release 3").performClick()
        composeRule.onNodeWithText("Release 3").assertDoesNotExist()
        composeRule.runOnIdle { showSchedule.value = true }
        composeRule.onNodeWithText("Release 3").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("Release 4").assertIsFocused()
    }

    private fun pressDownTwice() {
        composeRule.onNodeWithText("Release 1").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("Release 2").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("Release 3").assertIsFocused()
    }

    private fun schedule(): ScheduleMonth {
        val today = LocalDate.of(2026, 11, 25)
        return ScheduleMonth(
            title = "Расписание релизов",
            days = (1..6).map { number ->
                ScheduleDay(
                    date = today.plusDays(number - 1L),
                    label = "Day $number",
                    isToday = number == 1,
                    items = listOf(
                        ScheduleItem(
                            title = "Release $number",
                            episodeLabel = null,
                            targetUrl = "https://www.lostfilm.today/movies/Release_$number",
                            kind = ReleaseKind.MOVIE,
                        ),
                    ),
                )
            },
        )
    }
}
