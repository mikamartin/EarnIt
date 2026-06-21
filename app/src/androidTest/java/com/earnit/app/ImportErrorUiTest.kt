package com.earnit.app

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earnit.app.ui.Strings
import com.earnit.app.viewmodel.EarnItViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ImportErrorUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun navigateToDataScreen() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText(Strings.SETTINGS_DATA_SUBTITLE).performScrollTo().performClick()
    }

    private fun triggerImport(
        uri: Uri,
        replace: Boolean,
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            ViewModelProvider(activity)[EarnItViewModel::class.java].importFromFile(context, uri, replace)
        }
    }

    @Test
    fun importReplace_withInvalidJson_showsInvalidJsonError() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.filesDir, "ui_bad.json")
        // Must contain an EarnIt key so the schema check passes and the parser throws InvalidJsonException.
        file.writeText("{\"tasks\": [broken json here}")
        val uri = Uri.fromFile(file)

        navigateToDataScreen()
        triggerImport(uri, replace = true)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(Strings.IMPORT_ERROR_INVALID_JSON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.IMPORT_ERROR_INVALID_JSON).assertIsDisplayed()
        file.delete()
    }

    @Test
    fun importMerge_withWrongSchemaJson_showsWrongSchemaError() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.filesDir, "ui_wrong.json")
        file.writeText("{\"name\": \"not earnit\", \"foo\": 42}")
        val uri = Uri.fromFile(file)

        navigateToDataScreen()
        triggerImport(uri, replace = false)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(Strings.IMPORT_ERROR_WRONG_SCHEMA).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(Strings.IMPORT_ERROR_WRONG_SCHEMA).assertIsDisplayed()
        file.delete()
    }
}
