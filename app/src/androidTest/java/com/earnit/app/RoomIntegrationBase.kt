package com.earnit.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.earnit.app.data.EarnItDatabase
import com.earnit.app.data.EarnItRepository
import org.junit.After
import org.junit.Before

abstract class RoomIntegrationBase {
    protected lateinit var database: EarnItDatabase
    protected lateinit var repository: EarnItRepository

    @Before
    fun setUpRoom() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    EarnItDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository = EarnItRepository(database)
    }

    @After
    fun tearDownRoom() {
        database.close()
    }
}
