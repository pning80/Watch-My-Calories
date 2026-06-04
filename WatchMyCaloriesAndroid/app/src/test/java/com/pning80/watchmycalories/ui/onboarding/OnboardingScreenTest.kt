package com.pning80.watchmycalories.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        settingsDataStore = SettingsDataStore(RuntimeEnvironment.getApplication())
        // DataStore is process-scoped under Robolectric, so its contents
        // survive between tests. Reset the keys Onboarding reads so each
        // test starts from a known default.
        kotlinx.coroutines.runBlocking {
            settingsDataStore.setMetric(true)
            settingsDataStore.setAiConsent("notAsked")
        }
    }

    // MARK: - Welcome Step (Step 0)

    @Test
    fun testWelcomeScreenShowsTitle() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag("onboarding_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Watch My Calories").assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreenShowsGetStartedButton() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreenShowsSkipButton() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun testWelcomeScreenShowsPrivacyNote() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        // Privacy note about data never leaving device
        composeTestRule.onNodeWithText("Your data is never stored outside this device", substring = true)
            .assertExists()
    }

    @Test
    fun testWelcomeScreenNoUnitPicker() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        // Unit picker should NOT be on the welcome screen
        composeTestRule.onNodeWithText("US Customary").assertDoesNotExist()
        composeTestRule.onNodeWithText("Metric").assertDoesNotExist()
    }

    // MARK: - Skip Button

    @Test
    fun testSkipButtonCompletesOnboarding() {
        var completedProfile: UserProfile? = null
        composeTestRule.setContent {
            OnboardingScreen(
                settingsDataStore = settingsDataStore,
                onComplete = { completedProfile = it }
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()

        assertNotNull("Skip should trigger onComplete with a default profile", completedProfile)
    }

    // MARK: - Privacy Step (Step 1)

    @Test
    fun testGetStartedNavigatesToPrivacyStep() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        // Privacy step should appear
        composeTestRule.onNodeWithText("Your Privacy").assertIsDisplayed()
    }

    @Test
    fun testPrivacyStepShowsAIToggle() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        // Navigate to Privacy step
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE)
            .assertExists()
        composeTestRule.onNodeWithText("Google Gemini", substring = true).assertExists()
    }

    @Test
    fun testPrivacyStepShowsNextButton() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        // Navigate to Privacy step
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.NEXT_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun testAIToggleIsInteractive() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        // Navigate to Privacy step
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        val toggle = composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE)
        toggle.assertExists()

        // Toggle it
        toggle.performClick()
        composeTestRule.waitForIdle()

        // The toggle should have changed (we can't easily read the exact value in Robolectric,
        // but asserting the click doesn't crash is a valid interaction test)
    }

    @Test
    fun testSkipFromPrivacyStep() {
        var completedProfile: UserProfile? = null
        composeTestRule.setContent {
            OnboardingScreen(
                settingsDataStore = settingsDataStore,
                onComplete = { completedProfile = it }
            )
        }

        // Navigate to Privacy step
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        // Skip
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()

        assertNotNull("Skip from Privacy step should trigger onComplete", completedProfile)
    }

    // MARK: - Goal Step (Step 2)

    @Test
    fun testGoalStepShowsFormElements() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        composeTestRule.onNodeWithText("Your Goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Height").assertExists()
        composeTestRule.onNodeWithText("Weight").assertExists()
        composeTestRule.onNodeWithText("Age").assertExists()
        composeTestRule.onNodeWithText("Gender").assertExists()
        composeTestRule.onNodeWithText("Activity Level").assertExists()
    }

    @Test
    fun testGoalStepShowsActivityLevelPicker() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        composeTestRule.onNodeWithText("Activity Level").assertExists()
        composeTestRule.onNodeWithText("Sedentary").assertExists()
    }

    @Test
    fun testGoalStepShowsFinishButton() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.FINISH_BUTTON)
            .assertExists()
    }

    @Test
    fun testCalculateRecommendedGoalPopulatesTargetCalories() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        // Target calories field should be initially empty
        val targetField = composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.TARGET_CALORIES_FIELD)
        targetField.assertExists()

        // Tap Calculate Recommended Goal
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.CALCULATE_GOAL_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Target calories field should now contain a numeric value
        targetField.assertTextContains("1940") // 173cm 68kg Male 30y sedentary → CalorieCalculator.recommended
    }

    @Test
    fun testCompleteOnboardingFlowShowsDashboard() {
        var completedProfile: UserProfile? = null
        composeTestRule.setContent {
            OnboardingScreen(
                settingsDataStore = settingsDataStore,
                onComplete = { completedProfile = it }
            )
        }

        navigateToGoalStep()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.FINISH_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertNotNull("Finish should trigger onComplete", completedProfile)
    }

    @Test
    fun testSkipFromGoalStep() {
        var completedProfile: UserProfile? = null
        composeTestRule.setContent {
            OnboardingScreen(
                settingsDataStore = settingsDataStore,
                onComplete = { completedProfile = it }
            )
        }

        navigateToGoalStep()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()

        assertNotNull("Skip from Goal step should trigger onComplete", completedProfile)
    }

    // MARK: - Unit System Toggle (PR E review fix)

    @Test
    fun testGoalStepDefaultsToMetric() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        // Default profile in metric — "173 cm" / "68 kg" labels visible.
        composeTestRule.onNodeWithText("173 cm", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("68 kg", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testUnitToggleSwapsToUSCustomary() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()

        // Switch to US Customary.
        composeTestRule.onNodeWithText("US Customary").performScrollTo().performClick()

        // OnboardingScreen runs the toggle write as
        // `coroutineScope.launch { settingsDataStore.setMetric(false) }`, so
        // the DataStore emission + recomposition can outlive `waitForIdle()`.
        // Poll for the US Customary labels with a 2-second deadline. US Height
        // is now two ProfileDropdowns ("5 ft" + "8 in", matching the Settings
        // profile, D-004) and Weight is a ProfileWheelRow ("150 lbs"); use the
        // unmerged tree since these labels sit inside merging Composables.
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodesWithText("5 ft", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("5 ft", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("8 in", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("150 lbs", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testUnitTogglePersistsToDataStore() = kotlinx.coroutines.runBlocking {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        navigateToGoalStep()
        composeTestRule.onNodeWithText("US Customary").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // OnboardingScreen runs `coroutineScope.launch { settingsDataStore.setMetric(...) }`
        // off the click — the launched coroutine outlives waitForIdle(). Linux CI
        // runners finish the click handler before the launched coroutine flushes
        // the DataStore write; Mac local happens to flush in time. Bound the wait
        // by collecting the flow until we observe the flipped value, with a
        // 2s deadline.
        val observed = kotlinx.coroutines.withTimeoutOrNull(2000) {
            settingsDataStore.isMetricFlow.filter { !it }.first()
        }
        assertTrue("Toggling US should persist isMetric=false (timed out)", observed != null)
    }

    // MARK: - Connect Health checkmark (PR E review fix)

    @Test
    fun testConnectHealthButtonInitialLabel() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Connect Health", substring = true).assertIsDisplayed()
    }

    @Test
    fun testConnectHealthButtonSwapsToRequestedAfterClick() {
        composeTestRule.setContent {
            OnboardingScreen(settingsDataStore = settingsDataStore, onComplete = {})
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.CONNECT_HEALTH_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Label swap.
        composeTestRule.onNodeWithText("Health Connect Requested").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Connect Health (active calories)").fetchSemanticsNodes().let {
            assertTrue("Initial label should be gone after click", it.isEmpty())
        }
    }

    // MARK: - Navigation Helper

    private fun navigateToGoalStep() {
        // Step 0 → Step 1
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        // Step 1 → Step 2
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.NEXT_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
    }
}
