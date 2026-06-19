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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
object TestAppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): EarnItDatabase =
        Room
            .inMemoryDatabaseBuilder(context, EarnItDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(Dispatchers.Main.immediate)
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
