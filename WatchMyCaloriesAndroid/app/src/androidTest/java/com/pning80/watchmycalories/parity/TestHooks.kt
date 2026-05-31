package com.pning80.watchmycalories.parity

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pning80.watchmycalories.MainActivity
import com.pning80.watchmycalories.TestSeed

/**
 * Helpers for building MainActivity launch intents under the parity-audit
 * test infrastructure (Phase 3 of PARITY_FIX_PLAN.md).
 *
 * Mirrors the iOS `WatchMyCaloriesUITestBase` launch fixtures
 * (`launchEmpty`, `launchWithSeedData`, `launchWithMultiItemMeal`, etc.)
 * that landed in PR #1.
 *
 * Usage:
 *   val intent = TestHooks.intent { putExtra(TestSeed.EXTRA_SEED_DATA, true) }
 *   ActivityScenario.launch<MainActivity>(intent)
 */
object TestHooks {
    /** Build a fresh launch intent with `EXTRA_UI_TESTING = true` plus any extras you set. */
    fun intent(extras: Intent.() -> Unit = {}): Intent {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(TestSeed.EXTRA_UI_TESTING, true)
            extras()
        }
    }

    /** Convenience: launch with no seeded state (Room wiped, onboarding complete). */
    fun intentEmpty(): Intent = intent()

    /** Launch with the basic seed (UserProfile + 2 today entries) — iOS `--seed-data`. */
    fun intentWithSeedData(): Intent = intent {
        putExtra(TestSeed.EXTRA_SEED_DATA, true)
    }

    /** Launch with multi-day history — iOS `--seed-history`. */
    fun intentWithHistoryData(): Intent = intent {
        putExtra(TestSeed.EXTRA_SEED_HISTORY, true)
    }

    /** Launch with a multi-item meal group — iOS `--seed-multi-item-meal`. */
    fun intentWithMultiItemMeal(): Intent = intent {
        putExtra(TestSeed.EXTRA_SEED_MULTI_ITEM_MEAL, true)
    }

    /** Launch with menu scans seeded — iOS `--seed-menu-scans`. */
    fun intentWithMenuScans(): Intent = intent {
        putExtra(TestSeed.EXTRA_SEED_MENU_SCANS, true)
    }

    /** Launch with AI consent pre-accepted — iOS `--ai-consent-accepted`. */
    fun intentWithAIConsentAccepted(): Intent = intent {
        putExtra(TestSeed.EXTRA_AI_CONSENT, "accepted")
    }
}
