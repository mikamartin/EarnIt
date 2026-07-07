package com.earnit.app.di

import android.content.Context
import androidx.room.Room
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.MockTipRepository
import com.earnit.app.data.SettingsRepository
import com.earnit.app.data.TipRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// Marks the process-lifetime CoroutineScope for work that must outlive the screen that
// started it (e.g. a widget activity finishing before its DB write completes) — see
// WidgetTaskLogActivity for the motivating case.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): EarnItDatabase =
        Room
            .databaseBuilder(context, EarnItDatabase::class.java, "earnit.db")
            // No migrations registered yet — v1 is the launch baseline. The next version bump
            // MUST add a real Migration here (see docs/DEV_PLAYBOOK.md §6): this fallback drops
            // every table — every task, reward, and permanent History entry — with no warning.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideRepository(database: EarnItDatabase): EarnItRepository = EarnItRepository(database)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideTipRepository(): TipRepository = MockTipRepository()
}
