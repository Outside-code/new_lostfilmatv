package com.kraat.lostfilmnewtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBringIntoViewTest {
    @Test fun visibleInputAndBlinkingCaretDoNotMoveThePanel() {
        assertEquals(0f, SettingsBringIntoView.calculateScrollDistance(120.25f, 64f, 400f))
        assertEquals(0f, SettingsBringIntoView.calculateScrollDistance(120.75f, 22f, 400f))
        assertEquals(0f, SettingsBringIntoView.calculateScrollDistance(-0.75f, 64f, 400f))
    }
    @Test fun clippedControlsAreScrolledIntoViewUsingWholePixels() {
        assertEquals(64f, SettingsBringIntoView.calculateScrollDistance(400f, 64f, 400f))
        assertEquals(-24f, SettingsBringIntoView.calculateScrollDistance(-24f, 64f, 400f))
    }
}
