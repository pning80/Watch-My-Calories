package com.pning80.watchmycalories.parity

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.pning80.watchmycalories.MainActivity
import org.junit.After
import org.junit.Rule

/**
 * Base class for instrumented parity-audit tests on Pixel 9a (Phase 3 of
 * `PARITY_FIX_PLAN.md`). Mirrors iOS `WatchMyCaloriesUITestBase` launch fixtures.
 *
 * Uses `createEmptyComposeRule()` + `ActivityScenario.launch(intent)` so each
 * test controls its own launch intent. (`createAndroidComposeRule<T>()` is unusable
 * here — it launches the activity at rule-creation time with the default Intent
 * and we need custom extras for the `TestSeed` system.)
 *
 * Usage:
 *   class FooTest : MainActivityComposeTest() {
 *     @Test fun bar() {
 *       launchWithSeedData()
 *       composeTestRule.onNodeWithText("...").assertIsDisplayed()
 *     }
 *   }
 */
abstract class MainActivityComposeTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    // --- Launch fixtures (mirror iOS WatchMyCaloriesUITestBase) ---

    protected fun launchEmpty() = launch(TestHooks.intentEmpty())
    protected fun launchWithSeedData() = launch(TestHooks.intentWithSeedData())
    protected fun launchWithHistoryData() = launch(TestHooks.intentWithHistoryData())
    protected fun launchWithMultiItemMeal() = launch(TestHooks.intentWithMultiItemMeal())
    protected fun launchWithMenuScans() = launch(TestHooks.intentWithMenuScans())
    protected fun launchWithAIConsentAccepted() = launch(TestHooks.intentWithAIConsentAccepted())
    protected fun launchResetOnboarding() = launch(TestHooks.intentResetOnboarding())
    protected fun launchWithMockEstimationSuccess() = launch(TestHooks.intentWithMockEstimationSuccess())
    protected fun launchWithMockEstimationError() = launch(TestHooks.intentWithMockEstimationError())
    protected fun launchWithMockEstimationNoFood() = launch(TestHooks.intentWithMockEstimationNoFood())
    protected fun launchWithMockMenuAnalysisSuccess() = launch(TestHooks.intentWithMockMenuAnalysisSuccess())
    protected fun launchWithMockMenuAnalysisNotAMenu() = launch(TestHooks.intentWithMockMenuAnalysisNotAMenu())

    /** Custom launch — set arbitrary extras yourself via `TestHooks.intent { ... }`. */
    protected fun launch(intent: Intent) {
        scenario?.close()
        scenario = ActivityScenario.launch(intent)
        composeTestRule.waitForIdle()
    }
}
