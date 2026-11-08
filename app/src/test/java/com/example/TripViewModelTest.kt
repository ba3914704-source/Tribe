package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.TripDatabase
import com.example.core.database.dao.TripDao
import com.example.core.database.entity.TripEntity
import com.example.feature.plans.TripViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TripDatabase
    private lateinit var tripDao: TripDao
    private lateinit var viewModel: TripViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tripDao = database.tripDao()
        viewModel = TripViewModel(tripDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `test TripViewModel inserts and observes trips via StateFlow`() = runTest(testDispatcher) {
        val trip = TripEntity(
            id = "trip_vm_1",
            tripName = "Cox's Bazar Sunset Tour",
            destination = "Cox's Bazar",
            startDate = "2025-02-10",
            endDate = "2025-02-14",
            ownerUserId = "user_001",
            budget = 12000.0,
            status = "upcoming"
        )

        viewModel.insertTrip(trip)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.trips.isNotEmpty() }
        assertEquals(1, state.trips.size)
        assertEquals("Cox's Bazar Sunset Tour", state.trips[0].tripName)
        assertEquals("Cox's Bazar", state.trips[0].destination)
    }

    @Test
    fun `test TripViewModel filter by status`() = runTest(testDispatcher) {
        val tripUpcoming = TripEntity(
            id = "t_up",
            tripName = "Upcoming Tour",
            destination = "Bandarban",
            startDate = "2025-03-01",
            endDate = "2025-03-04",
            ownerUserId = "u1",
            status = "upcoming"
        )
        val tripOngoing = TripEntity(
            id = "t_on",
            tripName = "Ongoing Tour",
            destination = "Sajek",
            startDate = "2025-01-01",
            endDate = "2025-01-04",
            ownerUserId = "u1",
            status = "ongoing"
        )

        tripDao.insertTrip(tripUpcoming)
        tripDao.insertTrip(tripOngoing)
        advanceUntilIdle()

        viewModel.filterByStatus("ongoing")
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.selectedFilterStatus == "ongoing" && it.trips.size == 1 }
        assertEquals(1, state.trips.size)
        assertEquals("Ongoing Tour", state.trips[0].tripName)

        // Reset filter
        viewModel.filterByStatus(null)
        advanceUntilIdle()

        val allState = viewModel.uiState.first { it.selectedFilterStatus == null && it.trips.size == 2 }
        assertEquals(2, allState.trips.size)
    }

    @Test
    fun `test TripViewModel selection and deletion`() = runTest(testDispatcher) {
        val trip = TripEntity(
            id = "t_del",
            tripName = "To Be Deleted",
            destination = "Sundarbans",
            startDate = "2025-04-01",
            endDate = "2025-04-04",
            ownerUserId = "u1"
        )
        tripDao.insertTrip(trip)
        advanceUntilIdle()

        viewModel.selectTrip("t_del")
        advanceUntilIdle()

        val selectedState = viewModel.uiState.first { it.selectedTrip != null }
        assertEquals("t_del", selectedState.selectedTrip?.id)

        viewModel.deleteTripById("t_del")
        advanceUntilIdle()

        val deletedState = viewModel.uiState.first { it.trips.isEmpty() }
        assertNull(deletedState.selectedTrip)
        assertEquals(0, deletedState.trips.size)
    }
}
