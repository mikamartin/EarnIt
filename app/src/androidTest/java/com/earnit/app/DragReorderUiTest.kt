package com.earnit.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real long-press-then-drag gesture via Compose's touch-injection test tooling
 * (down, an explicit stationary [advanceEventTime] to clear the long-press threshold, then
 * moveTo) rather than testing HomeReorder's pure math in isolation. moveTo() coordinates are
 * relative to the dragged node's own top-left, not root, so each target's root-space center
 * is translated into the dragged card's local coordinate space before use.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DragReorderUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        resetAppState()
    }

    private fun addReward(name: String) {
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()
        composeTestRule.onNodeWithContentDescription("New Reward").performClick()
        composeTestRule.onNodeWithText("Reward name").performTextInput(name)
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addTask(name: String) {
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()
        composeTestRule.onNodeWithContentDescription("New Task").performClick()
        composeTestRule.onNodeWithText("Task name").performTextInput(name)
        composeTestRule.onNodeWithText("SAVE").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun dragPastTwo(
        draggedName: String,
        pastFirstName: String,
        pastSecondName: String,
    ) {
        val draggedBounds = composeTestRule.onNodeWithText(draggedName).fetchSemanticsNode().boundsInRoot
        val firstBounds = composeTestRule.onNodeWithText(pastFirstName).fetchSemanticsNode().boundsInRoot
        val secondBounds = composeTestRule.onNodeWithText(pastSecondName).fetchSemanticsNode().boundsInRoot

        fun localTarget(rootBounds: Rect) = Offset(rootBounds.center.x - draggedBounds.left, rootBounds.center.y - draggedBounds.top)

        composeTestRule.onNodeWithText(draggedName).performTouchInput {
            down(center)
            advanceEventTime(600)
            moveTo(localTarget(firstBounds))
            advanceEventTime(100)
            moveTo(localTarget(secondBounds))
            advanceEventTime(100)
            up()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun longPressDrag_reordersRewardCardsOnHome() {
        addReward("Card A")
        addReward("Card B")
        addReward("Card C")
        composeTestRule.onNodeWithContentDescription("Prizes").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Card A").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Card C").fetchSemanticsNodes().isNotEmpty()
        }

        dragPastTwo(draggedName = "Card A", pastFirstName = "Card B", pastSecondName = "Card C")

        val aY =
            composeTestRule
                .onNodeWithText("Card A")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val bY =
            composeTestRule
                .onNodeWithText("Card B")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val cY =
            composeTestRule
                .onNodeWithText("Card C")
                .fetchSemanticsNode()
                .boundsInRoot.top

        assertTrue("Card A should have moved below Card B (aY=$aY, bY=$bY)", aY > bY)
        assertTrue("Card A should have moved below Card C (aY=$aY, cY=$cY)", aY > cY)
    }

    @Test
    fun longPressDrag_reordersTaskCardsOnTasksScreen() {
        addTask("Task A")
        addTask("Task B")
        addTask("Task C")
        composeTestRule.onNodeWithContentDescription("Tasks").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Task A").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Task C").fetchSemanticsNodes().isNotEmpty()
        }

        dragPastTwo(draggedName = "Task A", pastFirstName = "Task B", pastSecondName = "Task C")

        val aY =
            composeTestRule
                .onNodeWithText("Task A")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val bY =
            composeTestRule
                .onNodeWithText("Task B")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val cY =
            composeTestRule
                .onNodeWithText("Task C")
                .fetchSemanticsNode()
                .boundsInRoot.top

        assertTrue("Task A should have moved below Task B (aY=$aY, bY=$bY)", aY > bY)
        assertTrue("Task A should have moved below Task C (aY=$aY, cY=$cY)", aY > cY)
    }
}
