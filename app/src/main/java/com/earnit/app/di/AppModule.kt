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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): EarnItDatabase =
        Room
            .databaseBuilder(context, EarnItDatabase::class.java, "earnit.db")
            .addMigrations(EarnItDatabase.MIGRATION_9_10)
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
