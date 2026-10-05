package com.smartmeasure.ar

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartmeasure.ar.UsageTestSupport.TIMEOUT_MS
import com.smartmeasure.ar.UsageTestSupport.clearAppData
import com.smartmeasure.ar.UsageTestSupport.clickText
import com.smartmeasure.ar.UsageTestSupport.hideKeyboard
import com.smartmeasure.ar.UsageTestSupport.isOnTrialsScreen
import com.smartmeasure.ar.UsageTestSupport.openFieldTrials
import com.smartmeasure.ar.UsageTestSupport.openManualMode
import com.smartmeasure.ar.UsageTestSupport.recreateAndSettle
import com.smartmeasure.ar.UsageTestSupport.scrollTrialsTo
import com.smartmeasure.ar.UsageTestSupport.str
import com.smartmeasure.ar.UsageTestSupport.typeInto
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Regression for the flows that work today (usage scripts R1–R4). Must stay green. */
@RunWith(AndroidJUnit4::class)
class HappyPathUsageTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launch() {
        clearAppData()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario.close()
    }

    @Test
    fun diagnosticsOpensWithEntryPoints() {
        composeRule.onNodeWithText(str(R.string.diagnostics_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.open_manual_mode)).assertExists()
        composeRule.onNodeWithText(str(R.string.open_field_trials)).assertExists()
    }

    @Test
    fun manualModeComputesAreaAndPerimeterOfFourByThree() {
        composeRule.openManualMode()
        composeRule.onNodeWithText(str(R.string.manual_measurement_title)).assertIsDisplayed()

        composeRule.typeInto(R.string.width_meters, "4")
        composeRule.typeInto(R.string.length_meters, "3")
        composeRule.clickText(R.string.calculate)

        composeRule.onNodeWithText(str(R.string.area_result, decimal(12.0))).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.perimeter_result, decimal(14.0))).assertIsDisplayed()
    }

    @Test
    fun manualBackButtonReturnsToDiagnostics() {
        composeRule.openManualMode()
        composeRule.clickText(R.string.back)
        composeRule.onNodeWithText(str(R.string.diagnostics_title)).assertIsDisplayed()
    }

    @Test
    fun savedTrialIsListedAndDeletedAfterConfirmation() {
        composeRule.openFieldTrials()
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_empty)))

        saveTrial(ar = "2,50", reference = "2,48")
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_list_title, 1)))

        composeRule.hideKeyboard()
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_delete)) and hasClickAction())
        composeRule.onNode(hasText(str(R.string.trials_delete)) and hasClickAction()).performClick()
        composeRule.onNodeWithText(str(R.string.trials_delete_title)).assertIsDisplayed()
        composeRule.onNode(
            hasText(str(R.string.trials_delete_confirm)) and hasClickAction() and hasAnyAncestor(isDialog()),
        ).performClick()

        composeRule.scrollTrialsTo(hasText(str(R.string.trials_empty)))
        composeRule.onAllNodesWithText(str(R.string.trials_list_title, 1)).assertCountEquals(0)
    }

    @Test
    fun savedTrialSurvivesActivityRecreation() {
        composeRule.openFieldTrials()
        saveTrial(ar = "3,10", reference = "3,00")
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_list_title, 1)))

        composeRule.recreateAndSettle(scenario)

        // Today recreation lands on Diagnostics (defect R6.1); after the fix it stays on Trials.
        // Either way the saved trial must still be there. The list may reopen scrolled to its saved
        // position, so every check scrolls the LazyColumn instead of expecting the first items.
        if (!composeRule.isOnTrialsScreen()) composeRule.openFieldTrials()
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_list_title, 1)))
    }

    private fun saveTrial(ar: String, reference: String) {
        composeRule.typeInto(R.string.trials_ar_value, ar)
        composeRule.typeInto(R.string.trials_reference_value, reference)
        composeRule.hideKeyboard()
        composeRule.scrollTrialsTo(hasText(str(R.string.trials_save)) and hasClickAction())
        composeRule.onNode(hasText(str(R.string.trials_save)) and hasClickAction()).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(str(R.string.trials_saved)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Same formatting as ManualMeasurementScreen (two decimals, default locale). */
    private fun decimal(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
}
