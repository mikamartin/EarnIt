package com.earnit.app

import androidx.test.core.app.ApplicationProvider
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestStateResetEntryPoint {
    fun repository(): EarnItRepository

    fun settingsRepository(): SettingsRepository
}

/**
 * Wipes the shared Room database and resets DataStore settings to defaults.
 *
 * TestAppModule scopes both @Singleton to the process-wide Hilt SingletonComponent, so
 * without this every @HiltAndroidTest UI test leaks its tasks/rewards/settings into every
 * test that runs after it in the same instrumentation run — see docs/TESTING.md. Call this
 * as the first line of each test class's @Before, right after hiltRule.inject().
 */
fun resetAppState() {
    val entryPoint =
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext(),
            TestStateResetEntryPoint::class.java,
        )
    runBlocking {
        entryPoint.repository().clearAll()
        entryPoint.settingsRepository().resetToDefaults()
    }
}
