package com.smartmeasure.ar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartmeasure.ar.UsageTestSupport.allAppStrings
import com.smartmeasure.ar.UsageTestSupport.clearAppData
import com.smartmeasure.ar.UsageTestSupport.clickText
import com.smartmeasure.ar.UsageTestSupport.openFieldTrials
import com.smartmeasure.ar.UsageTestSupport.openManualMode
import com.smartmeasure.ar.UsageTestSupport.recreateAndSettle
import com.smartmeasure.ar.UsageTestSupport.str
import com.smartmeasure.ar.UsageTestSupport.typeInto
import com.smartmeasure.ar.UsageTestSupport.visibleTexts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Defects found in the first emulator usage round (tools/emulator/run-scenarios.sh). TDD RED: each
 * test describes the expected behavior and fails until the owning specialist fixes production code.
 * Pending (not covered here): R6.3, process death returns to Diagnostics — needs a process-kill
 * harness (e.g. am kill from a UiAutomator test or the adb script), not ActivityScenario.
 */
@RunWith(AndroidJUnit4::class)
class UsageDefectsTest {
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

    // Defeito R2.11: system back on the manual screen closes the app instead of returning to Diagnostics.
    @Test
    fun systemBackOnManualScreenReturnsToDiagnostics() {
        composeRule.openManualMode()
        composeRule.onNodeWithText(str(R.string.manual_measurement_title)).assertIsDisplayed()

        // Unconditionally: plain pressBack() throws when the activity finishes, hiding the assertion.
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(
            "Defeito R2.11: 'voltar' do sistema no modo manual deve manter a activity viva",
            Lifecycle.State.RESUMED,
            scenario.state,
        )
        composeRule.onNodeWithText(str(R.string.diagnostics_title)).assertIsDisplayed()
    }

    // Defeito R6.1: rotating (activity recreation) on the trials screen returns to Diagnostics.
    @Test
    fun recreationKeepsTrialsScreen() {
        composeRule.openFieldTrials()

        composeRule.recreateAndSettle(scenario) // equivalent of rotating the screen

        // Subtitle, not title: in pt-BR the title text equals the Diagnostics button that opens it.
        // (Compose 1.12 reports a missing node here as "is not displayed".)
        composeRule.onNodeWithText(str(R.string.trials_subtitle)).assertIsDisplayed()
    }

    // Defeito R6.2: rotating (activity recreation) on the trials screen loses the typed draft.
    @Test
    fun recreationKeepsTypedTrialDraft() {
        composeRule.openFieldTrials()
        composeRule.typeInto(R.string.trials_ar_value, "2,5")
        composeRule.typeInto(R.string.trials_reference_value, "2,4")

        composeRule.recreateAndSettle(scenario) // equivalent of rotating the screen

        composeRule.onNode(hasSetTextAction() and hasText(str(R.string.trials_ar_value)))
            .assertTextContains("2,5")
        composeRule.onNode(hasSetTextAction() and hasText(str(R.string.trials_reference_value)))
            .assertTextContains("2,4")
    }

    /*
     * Defeito R5.3: the manual-mode validation message is a hardcoded English literal.
     * Assertion chosen: the message that appears after "Calculate" with empty fields must be one of
     * the app's string resources in the current locale. This holds in any locale (no dependency on
     * pt-BR wording or on the future resource name) and fails for any hardcoded literal, not only
     * the current English one. The message is isolated as the text that appears on screen after the
     * click (diff of visible texts), since the UI has no testTag for it.
     */
    @Test
    fun manualValidationMessageComesFromStringResource() {
        composeRule.openManualMode()
        val before = composeRule.visibleTexts().toSet()

        composeRule.clickText(R.string.calculate)
        composeRule.waitForIdle()

        val appeared = composeRule.visibleTexts().filterNot { it in before }
        assertEquals("Esperava exatamente uma mensagem de validação nova: $appeared", 1, appeared.size)
        val message = appeared.single()
        assertTrue(
            "Defeito R5.3: mensagem '$message' não vem de um recurso de string do app " +
                "(locale ${UsageTestSupport.targetContext.resources.configuration.locales[0]})",
            message in allAppStrings(),
        )
    }
}
