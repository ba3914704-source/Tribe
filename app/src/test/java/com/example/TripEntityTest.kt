package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.TripDatabase
import com.example.core.database.dao.TripDao
import com.example.core.database.entity.TripEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripEntityTest {

    private lateinit var database: TripDatabase
    private lateinit var tripDao: TripDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tripDao = database.tripDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test TripEntity fields and insertion into Room database`() = runTest {
        val trip = TripEntity(
            id = "trip_123",
            tripName = "Winter Bandarban Escape",
            destination = "Bandarban",
            startDate = "2024-12-15",
            endDate = "2024-12-17",
            ownerUserId = "user_dhaka_rider_001",
            description = "Exploring Nilgiri and Amiakhum waterfall",
            budget = 18500.0,
            status = "ongoing"
        )

        tripDao.insertTrip(trip)

        val retrievedTrip = tripDao.getTripById("trip_123")
        assertNotNull(retrievedTrip)
        assertEquals("Winter Bandarban Escape", retrievedTrip?.tripName)
        assertEquals("Bandarban", retrievedTrip?.destination)
        assertEquals("2024-12-15", retrievedTrip?.startDate)
        assertEquals("2024-12-17", retrievedTrip?.endDate)
        assertEquals("user_dhaka_rider_001", retrievedTrip?.ownerUserId)
        assertEquals("ongoing", retrievedTrip?.status)

        val userTrips = tripDao.getTripsByOwner("user_dhaka_rider_001").first()
        assertEquals(1, userTrips.size)
        assertEquals("trip_123", userTrips[0].id)
    }

    @Test
    fun `test TripEntity deletion`() = runTest {
        val trip = TripEntity(
            id = "trip_456",
            tripName = "Sundarbans Safari",
            destination = "Sundarbans",
            startDate = "2025-01-10",
            endDate = "2025-01-12",
            ownerUserId = "user_002"
        )

        tripDao.insertTrip(trip)
        assertEquals(1, tripDao.getAllTrips().first().size)

        tripDao.deleteTripById("trip_456")
        assertEquals(0, tripDao.getAllTrips().first().size)
    }
}
