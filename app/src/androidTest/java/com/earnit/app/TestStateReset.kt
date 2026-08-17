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
 *
 * Marks onboarding as already seen: EarnItApp auto-navigates to Create Reward whenever it
 * isn't, and resetToDefaults() would otherwise leave every test starting there instead of on
 * Home. OnboardingFlowUiTest explicitly reverses this via resetOnboardingFlag() to test the
 * real first-launch flow.
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
        entryPoint.settingsRepository().markOnboardingSeen()
    }
}

/** Reverses the markOnboardingSeen() call in [resetAppState] so a test can exercise the real first-launch flow. */
fun resetOnboardingFlag() {
    val entryPoint =
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext(),
            TestStateResetEntryPoint::class.java,
        )
    runBlocking { entryPoint.settingsRepository().resetOnboarding() }
}
