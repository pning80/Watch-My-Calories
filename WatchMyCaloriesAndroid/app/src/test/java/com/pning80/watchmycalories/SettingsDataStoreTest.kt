package com.pning80.watchmycalories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.datastore.preferences.core.edit
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.ui.settings.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsDataStoreTest {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var context: Context

    @Before
    fun setup() {
        // Use Application Context for DataStore to simulate standard Environment
        context = ApplicationProvider.getApplicationContext<Context>()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testDefaultMetricPreferenceFollowsLocale() = runBlocking {
        // Clear any persisted value (the shared DataStore may carry one written
        // by a sibling test) so the locale-based default actually applies.
        context.dataStore.edit { it.remove(SettingsDataStore.IS_METRIC) }
        // Default now mirrors iOS (SettingsStore.localeDefault): US region →
        // imperial (false), everywhere else → metric (true).
        val expected = java.util.Locale.getDefault().country != "US"
        val isMetric = settingsDataStore.isMetricFlow.first()
        assertEquals("Unit default should follow locale (US → imperial)", expected, isMetric)
    }

    @Test
    fun testSetMetricToFalseUpdatesFlow() = runBlocking {
        // Toggle the datastore value natively 
        settingsDataStore.setMetric(false)

        val updatedValue = settingsDataStore.isMetricFlow.first()
        assertFalse("Metric should successfully update to false", updatedValue)

        // Reset state
        settingsDataStore.setMetric(true)
    }
}
