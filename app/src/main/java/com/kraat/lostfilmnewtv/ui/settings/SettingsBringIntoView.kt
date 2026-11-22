package com.kraat.lostfilmnewtv.ui.settings

import androidx.compose.foundation.gestures.BringIntoViewSpec
import kotlin.math.abs
import kotlin.math.roundToInt

/** Keep fully visible fields still instead of aligning the blinking caret to the TV pivot. */
internal object SettingsBringIntoView : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = offset + size
        val distance = when {
            offset >= 0f && end <= containerSize -> 0f
            offset < 0f && end > containerSize -> 0f
            abs(offset) < abs(end - containerSize) -> offset
            else -> end - containerSize
        }
        return if (abs(distance) <= 1f) 0f else distance.roundToInt().toFloat()
    }
}
