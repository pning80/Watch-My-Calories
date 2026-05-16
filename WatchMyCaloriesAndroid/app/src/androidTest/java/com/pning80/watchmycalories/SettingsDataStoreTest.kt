package com.pning80.watchmycalories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        // Use Application Context for DataStore to simulate standard Environment
        val context = ApplicationProvider.getApplicationContext<Context>()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testDefaultMetricPreferenceIsTrue() = runBlocking {
        // Based on the code implementation, it should default to true
        val isMetric = settingsDataStore.isMetricFlow.first()
        assertTrue("Metric should default to true", isMetric)
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
