package com.example.feature.plans

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.TripDatabase
import com.example.core.database.dao.TripDao
import com.example.core.database.entity.TripEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI State for Trip management screens.
 */
data class TripUiState(
    val trips: List<TripEntity> = emptyList(),
    val selectedTrip: TripEntity? = null,
    val selectedFilterStatus: String? = null, // null for All, "upcoming", "ongoing", "completed"
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel to manage Trip state using Room's TripDao and expose it to the UI via StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModel(
    private val tripDao: TripDao
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<String?>(null)
    private val _selectedTripId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    // Pair of (userMessage, errorMessage)
    private val _messageFlow = MutableStateFlow<Pair<String?, String?>>(Pair(null, null))

    // Reactive flow of trips according to the active status filter
    private val _tripsFlow: Flow<List<TripEntity>> = _filterStatus.flatMapLatest { status ->
        if (status.isNullOrBlank()) {
            tripDao.getAllTrips()
        } else {
            tripDao.getTripsByStatus(status)
        }
    }

    // Combined StateFlow for UI consumption (using typed 5-flow combine overload)
    val uiState: StateFlow<TripUiState> = combine(
        _tripsFlow,
        _selectedTripId,
        _filterStatus,
        _isLoading,
        _messageFlow
    ) { trips, selectedId, filter, isLoading, messages ->
        val selected = selectedId?.let { id ->
            trips.find { it.id == id } ?: tripDao.getTripById(id)
        }
        TripUiState(
            trips = trips,
            selectedTrip = selected,
            selectedFilterStatus = filter,
            isLoading = isLoading,
            userMessage = messages.first,
            errorMessage = messages.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TripUiState(isLoading = true)
    )

    /**
     * Filter trips by status ("upcoming", "ongoing", "completed", or null for all).
     */
    fun filterByStatus(status: String?) {
        _filterStatus.value = status
    }

    /**
     * Select a specific trip to view details.
     */
    fun selectTrip(tripId: String?) {
        _selectedTripId.value = tripId
    }

    /**
     * Create and insert a new trip into Room.
     */
    fun createTrip(
        tripName: String,
        destination: String,
        startDate: String,
        endDate: String,
        ownerUserId: String,
        description: String = "",
        budget: Double = 0.0,
        status: String = "upcoming",
        imageUrl: String? = null
    ) {
        if (tripName.isBlank() || destination.isBlank()) {
            _messageFlow.value = Pair(null, "Trip name and destination are required")
            return
        }

        val newTrip = TripEntity(
            id = UUID.randomUUID().toString(),
            tripName = tripName.trim(),
            destination = destination.trim(),
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            ownerUserId = ownerUserId.trim(),
            description = description.trim(),
            budget = budget,
            status = status.lowercase().trim(),
            imageUrl = imageUrl
        )

        insertTrip(newTrip)
    }

    /**
     * Insert a trip entity into Room.
     */
    fun insertTrip(trip: TripEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                tripDao.insertTrip(trip)
                _messageFlow.value = Pair("Trip '${trip.tripName}' saved successfully", null)
            } catch (e: Exception) {
                _messageFlow.value = Pair(null, "Failed to save trip: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing trip in Room.
     */
    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                tripDao.updateTrip(trip)
                _messageFlow.value = Pair("Trip updated successfully", null)
            } catch (e: Exception) {
                _messageFlow.value = Pair(null, "Failed to update trip: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update the status of a trip (e.g. from upcoming -> ongoing -> completed).
     */
    fun updateTripStatus(tripId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val current = tripDao.getTripById(tripId)
                if (current != null) {
                    val updated = current.copy(status = newStatus.lowercase().trim())
                    tripDao.updateTrip(updated)
                    _messageFlow.value = Pair("Status updated to ${newStatus.replaceFirstChar { it.uppercase() }}", null)
                }
            } catch (e: Exception) {
                _messageFlow.value = Pair(null, "Failed to update trip status: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Delete a trip from Room.
     */
    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            try {
                tripDao.deleteTrip(trip)
                if (_selectedTripId.value == trip.id) {
                    _selectedTripId.value = null
                }
                _messageFlow.value = Pair("Trip deleted", null)
            } catch (e: Exception) {
                _messageFlow.value = Pair(null, "Failed to delete trip: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Delete a trip by its ID.
     */
    fun deleteTripById(tripId: String) {
        viewModelScope.launch {
            try {
                tripDao.deleteTripById(tripId)
                if (_selectedTripId.value == tripId) {
                    _selectedTripId.value = null
                }
                _messageFlow.value = Pair("Trip deleted", null)
            } catch (e: Exception) {
                _messageFlow.value = Pair(null, "Failed to delete trip: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Dismiss/clear transient user message.
     */
    fun clearUserMessage() {
        _messageFlow.value = Pair(null, _messageFlow.value.second)
    }

    /**
     * Dismiss/clear error message.
     */
    fun clearErrorMessage() {
        _messageFlow.value = Pair(_messageFlow.value.first, null)
    }

    /**
     * Companion factory helper for easy instantiation in Activities and Composables.
     */
    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
                        val database = TripDatabase.getDatabase(context.applicationContext)
                        return TripViewModel(database.tripDao()) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
