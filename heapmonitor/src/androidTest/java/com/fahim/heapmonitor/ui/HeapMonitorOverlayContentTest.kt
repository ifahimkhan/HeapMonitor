package com.fahim.heapmonitor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fahim.heapmonitor.HeapMonitorConfig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HeapMonitorOverlayContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactChipShowsFormattedText() {
        composeRule.setContent {
            HeapMonitorOverlayContent(fakeSnapshot(), HeapMonitorConfig(), onForceGc = {})
        }
        composeRule.onNodeWithText("84.0/200.0 MB · GC 17").assertIsDisplayed()
        composeRule.onNodeWithTag(OverlayTags.EXPANDED).assertDoesNotExist()
    }

    @Test
    fun tapChipExpandsAndCollapseHides() {
        composeRule.setContent {
            HeapMonitorOverlayContent(fakeSnapshot(gcDelta = 2), HeapMonitorConfig(), onForceGc = {})
        }
        composeRule.onNodeWithTag(OverlayTags.OVERLAY).performClick()
        composeRule.onNodeWithTag(OverlayTags.EXPANDED).assertIsDisplayed()
        composeRule.onNodeWithText("17 (+2)").assertIsDisplayed()

        composeRule.onNodeWithTag(OverlayTags.COLLAPSE).performClick()
        composeRule.onNodeWithTag(OverlayTags.EXPANDED).assertDoesNotExist()
    }

    @Test
    fun forceGcInvokesCallbackOnce() {
        var calls = 0
        composeRule.setContent {
            HeapMonitorOverlayContent(
                snapshot = fakeSnapshot(),
                config = HeapMonitorConfig(startExpanded = true),
                onForceGc = { calls++ },
            )
        }
        composeRule.onNodeWithTag(OverlayTags.FORCE_GC).performClick()
        composeRule.waitForIdle()
        assertEquals(1, calls)
    }

    @Test
    fun forceGcHiddenWhenDisabled() {
        composeRule.setContent {
            HeapMonitorOverlayContent(
                snapshot = fakeSnapshot(),
                config = HeapMonitorConfig(startExpanded = true, showForceGcButton = false),
                onForceGc = {},
            )
        }
        composeRule.onNodeWithTag(OverlayTags.EXPANDED).assertIsDisplayed()
        composeRule.onNodeWithTag(OverlayTags.FORCE_GC).assertDoesNotExist()
    }

    @Test
    fun usageBarTagReflectsSeverity() {
        composeRule.setContent {
            HeapMonitorOverlayContent(
                snapshot = fakeSnapshot(usedMb = 190, maxMb = 200),
                config = HeapMonitorConfig(startExpanded = true),
                onForceGc = {},
            )
        }
        composeRule.onNodeWithTag(OverlayTags.USAGE_CRITICAL).assertIsDisplayed()
        composeRule.onNodeWithTag(OverlayTags.USAGE_OK).assertDoesNotExist()
    }

    @Test
    fun usageBarOkBelowWarnThreshold() {
        composeRule.setContent {
            HeapMonitorOverlayContent(
                snapshot = fakeSnapshot(usedMb = 84, maxMb = 200),
                config = HeapMonitorConfig(startExpanded = true),
                onForceGc = {},
            )
        }
        composeRule.onNodeWithTag(OverlayTags.USAGE_OK).assertIsDisplayed()
    }
}
