package com.smartmeasure.ar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Modifier

/**
 * Shared steps for the usage tests. Every visible text is resolved from the app's resources, so the
 * tests run in any device locale (the project AVD is pt-BR). No testTag exists in the UI yet: nodes
 * are found by resource text and semantics (set-text / scroll-to-node actions).
 */
object UsageTestSupport {
    const val TIMEOUT_MS = 5_000L

    val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun str(@StringRes id: Int, vararg args: Any): String = targetContext.getString(id, *args)

    /** Removes the trials file and its temporary/backup siblings so each test starts empty. */
    fun clearAppData() {
        targetContext.filesDir
            .listFiles { file -> file.name.startsWith(FIELD_TRIALS_FILE) }
            ?.forEach { it.deleteRecursively() }
    }

    /** Every string resource visible to the app (its own and its libraries'), in the current locale. */
    fun allAppStrings(): Set<String> =
        R.string::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
            .mapNotNull { field -> runCatching { targetContext.getString(field.getInt(null)) }.getOrNull() }
            .toSet()

    /** Texts of every Text node on screen (unmerged tree), excluding editable field contents. */
    fun ComposeTestRule.visibleTexts(): List<String> =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }

    /**
     * Equivalent of a rotation: recreates the activity, then waits until only the new activity's
     * Compose root is registered and idle, so the next query cannot hit the destroyed hierarchy.
     */
    fun ComposeTestRule.recreateAndSettle(scenario: ActivityScenario<*>) {
        scenario.recreate()
        waitUntil(TIMEOUT_MS) { onAllNodes(isRoot()).fetchSemanticsNodes().size == 1 }
        waitForIdle()
    }

    /** Clicks the button labelled with [id], scrolling first only when it sits in a scrollable column. */
    fun ComposeTestRule.clickText(@StringRes id: Int) {
        hideKeyboard()
        val button = hasText(str(id)) and hasClickAction()
        val scrollable = onAllNodes(button and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes()
        val node = onNode(button)
        if (scrollable.isNotEmpty()) node.performScrollTo()
        node.performClick()
    }

    /**
     * Closes the soft keyboard left open by typing: Compose clicks are injected touches, and a button
     * scrolled to the bottom edge can sit under the IME, which then receives the touch.
     */
    fun ComposeTestRule.hideKeyboard() {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }

    /**
     * Whether the trials screen is showing: its LazyColumn is the app's only node with a
     * scroll-to-node action. Not the subtitle: the list may open scrolled (see [openFieldTrials]),
     * leaving the first items uncomposed; and in pt-BR the screen title equals the Diagnostics
     * button that opens it ("Ensaios de precisão").
     */
    fun ComposeTestRule.isOnTrialsScreen(): Boolean =
        onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty()

    fun ComposeTestRule.openManualMode() = clickText(R.string.open_manual_mode)

    /**
     * Opens the trials screen and checks its subtitle by scrolling to it. After an activity
     * recreation the LazyColumn's saved scroll position (rememberLazyListState is saveable) is
     * restored the first time the screen is composed again, so the list can open scrolled down and
     * the subtitle item is not composed until the list scrolls back.
     */
    fun ComposeTestRule.openFieldTrials() {
        clickText(R.string.open_field_trials)
        scrollTrialsTo(hasText(str(R.string.trials_subtitle)))
    }

    /** Types into the OutlinedTextField whose label is the given resource. */
    fun ComposeTestRule.typeInto(@StringRes labelId: Int, text: String) {
        onNode(hasSetTextAction() and hasText(str(labelId))).performTextInput(text)
    }

    /**
     * Scrolls the trials LazyColumn until [matcher] is composed, retrying while the file repository
     * (Dispatchers.IO, invisible to Compose idling) finishes writing.
     */
    fun ComposeTestRule.scrollTrialsTo(matcher: SemanticsMatcher) {
        waitUntil(TIMEOUT_MS) {
            runCatching {
                onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
                true
            }.getOrDefault(false)
        }
    }

    private const val FIELD_TRIALS_FILE = "field_trials.tsv"
}
