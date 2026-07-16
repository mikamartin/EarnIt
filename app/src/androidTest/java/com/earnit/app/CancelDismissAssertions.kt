package com.earnit.app

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * Clicks a dialog's Cancel/dismiss text button and asserts the dialog is gone afterward. Scoped
 * to the dialog's own window via [isDialog] since several dialogs share their Cancel button's
 * exact text ("CANCEL") with a button on the screen behind them, which would otherwise make
 * `onNodeWithText(cancelText)` match two nodes at once.
 */
fun SemanticsNodeInteractionsProvider.cancelDialogAndAssertDismissed(
    dialogMarkerText: String,
    cancelText: String = "CANCEL",
) {
    onNode(hasAnyAncestor(isDialog()) and hasText(cancelText)).performClick()
    onNodeWithText(dialogMarkerText).assertDoesNotExist()
}
