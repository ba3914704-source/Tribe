package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.designsystem.NavigationTab
import com.example.core.network.SupabaseNetworkProvider
import com.example.core.preferences.AppPreferences
import com.example.core.security.GoogleAuthHelper
import com.example.core.security.SessionManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.DefaultTravelRepository
import com.example.data.repository.SupabaseAuthRepository
import com.example.data.repository.TravelRepository
import com.example.domain.model.*
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed class AppScreen {
    object Welcome : AppScreen()
    object Login : AppScreen()
    object Register : AppScreen()
    object Main : AppScreen()
    data class TripDetails(val tourId: String) : AppScreen()
    object Notifications : AppScreen()
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.Welcome,
    val currentTab: NavigationTab = NavigationTab.HOME,
    val session: UserSession? = null,
    val isGuest: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "en",
    val destinations: List<Destination> = emptyList(),
    val tours: List<Tour> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val selectedTour: Tour? = null,
    val selectedTourMembers: List<TourMember> = emptyList(),
    val selectedTourItinerary: List<ItineraryItem> = emptyList(),
    val selectedTourExpenses: List<Expense> = emptyList(),
    val selectedTourExpenseSummary: ExpenseSummary = ExpenseSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
    val selectedTourSettlements: List<Settlement> = emptyList(),
    val selectedTourChecklist: List<ChecklistItem> = emptyList(),
    val selectedTourComments: List<TourComment> = emptyList(),
    val isCreatePlanOpen: Boolean = false,
    val isLoading: Boolean = false,
    val authError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val appPreferences = AppPreferences(application)
    val googleAuthHelper = GoogleAuthHelper(application)
    private val authApi = SupabaseNetworkProvider.createAuthApi(sessionManager = sessionManager)
    private val restApi = SupabaseNetworkProvider.createRestApi(sessionManager = sessionManager)
    val authRepository: AuthRepository = SupabaseAuthRepository(authApi, sessionManager)
    val travelRepository: TravelRepository = DefaultTravelRepository(restApi)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionFlow.collect { session ->
                _uiState.update { current ->
                    current.copy(
                        session = session,
                        currentScreen = if (session != null || current.isGuest) {
                            if (current.currentScreen is AppScreen.Welcome || current.currentScreen is AppScreen.Login || current.currentScreen is AppScreen.Register) AppScreen.Main
                            else current.currentScreen
                        } else current.currentScreen
                    )
                }
            }
        }
        viewModelScope.launch {
            authRepository.isGuestFlow.collect { isGuest ->
                _uiState.update { current ->
                    current.copy(
                        isGuest = isGuest,
                        currentScreen = if (isGuest && (current.currentScreen is AppScreen.Welcome || current.currentScreen is AppScreen.Login)) AppScreen.Main
                        else current.currentScreen
                    )
                }
            }
        }
        viewModelScope.launch {
            appPreferences.themeModeFlow.collect { theme -> _uiState.update { it.copy(themeMode = theme) } }
        }
        viewModelScope.launch {
            appPreferences.languageFlow.collect { lang -> _uiState.update { it.copy(language = lang) } }
        }
        viewModelScope.launch {
            travelRepository.destinationsFlow.collect { dests -> _uiState.update { it.copy(destinations = dests) } }
        }
        viewModelScope.launch {
            travelRepository.toursFlow.collect { toursList -> _uiState.update { it.copy(tours = toursList) } }
        }
        viewModelScope.launch {
            travelRepository.notificationsFlow.collect { notifs -> _uiState.update { it.copy(notifications = notifs) } }
        }
        viewModelScope.launch { travelRepository.refreshData() }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, authError = null) }
        if (screen is AppScreen.TripDetails) loadTourDetails(screen.tourId)
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab, currentScreen = AppScreen.Main) }
    }

    fun setCreatePlanSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isCreatePlanOpen = open) }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            authRepository.signInWithEmail(email, pass).fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message ?: "Sign in failed. Please try again.") }
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, fullName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            authRepository.signUpWithEmail(email, pass, fullName).fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message ?: "Registration failed. Please try again.") }
                }
            )
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            authRepository.continueAsGuest()
            _uiState.update { it.copy(isGuest = true, currentScreen = AppScreen.Main) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                it.copy(session = null, isGuest = false, currentScreen = AppScreen.Login, currentTab = NavigationTab.HOME)
            }
        }
    }

    fun onGoogleSignInSuccess(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            authRepository.signInWithGoogleToken(idToken, rawNonce).fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message ?: "Google sign-in failed.") }
                }
            )
        }
    }

    fun loadTourDetails(tourId: String) {
        viewModelScope.launch {
            val tour = travelRepository.getTourById(tourId)
            val members = travelRepository.getTourMembers(tourId)
            val itinerary = travelRepository.getItinerary(tourId)
            val expenses = travelRepository.getExpenses(tourId)
            val summary = travelRepository.getExpenseSummary(tourId, _uiState.value.session?.fullName ?: "Me")
            val settlements = travelRepository.getSettlements(tourId)
            val checklist = travelRepository.getChecklist(tourId)
            val comments = travelRepository.getTourComments(tourId)
            _uiState.update {
                it.copy(
                    selectedTour = tour,
                    selectedTourMembers = members,
                    selectedTourItinerary = itinerary,
                    selectedTourExpenses = expenses,
                    selectedTourExpenseSummary = summary,
                    selectedTourSettlements = settlements,
                    selectedTourChecklist = checklist,
                    selectedTourComments = comments
                )
            }
        }
    }

    fun toggleChecklistItem(itemId: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.toggleChecklistItem(tourId, itemId)
            _uiState.update { it.copy(selectedTourChecklist = travelRepository.getChecklist(tourId)) }
        }
    }

    fun addChecklistItem(title: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addChecklistItem(tourId, title)
            _uiState.update { it.copy(selectedTourChecklist = travelRepository.getChecklist(tourId)) }
        }
    }

    fun addSettlement(fromUser: String, toUser: String, amount: BigDecimal) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addSettlement(tourId, fromUser, toUser, amount)
            loadTourDetails(tourId)
        }
    }

    fun createPlan(title: String, destinationName: String, startDate: String, endDate: String, budget: BigDecimal, isPublic: Boolean, description: String) {
        viewModelScope.launch {
            travelRepository.createPlan(title, destinationName, startDate, endDate, budget, isPublic, description)
            _uiState.update { it.copy(isCreatePlanOpen = false) }
        }
    }

    fun addExpense(title: String, amount: BigDecimal, category: ExpenseCategory, payer: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addExpense(tourId, title, amount, category, payer, "current_user")
            loadTourDetails(tourId)
        }
    }

    fun addItineraryItem(day: Int, time: String, location: String, title: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addItineraryItem(tourId, day, time, location, title)
            loadTourDetails(tourId)
        }
    }

    fun inviteMember(name: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addTourMember(tourId, name)
            loadTourDetails(tourId)
        }
    }

    fun addComment(text: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        val userName = _uiState.value.session?.fullName ?: "Traveler"
        viewModelScope.launch {
            travelRepository.addComment(tourId, userName, text)
            loadTourDetails(tourId)
        }
    }

    fun toggleSaveTour(tourId: String) {
        viewModelScope.launch { travelRepository.toggleSaveTour(tourId) }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { travelRepository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { travelRepository.markAllNotificationsRead() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { appPreferences.setLanguage(lang) }
    }
}
