package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
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
    val createPlanError: String? = null,
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
        // Collect session
        viewModelScope.launch {
            authRepository.sessionFlow.collect { session ->
                _uiState.update { current ->
                    current.copy(
                        session = session,
                        currentScreen = if (session != null || current.isGuest) {
                            if (current.currentScreen is AppScreen.Welcome || current.currentScreen is AppScreen.Login || current.currentScreen is AppScreen.Register) {
                                AppScreen.Main
                            } else current.currentScreen
                        } else current.currentScreen
                    )
                }
                if (session != null) {
                    viewModelScope.launch {
                        travelRepository.refreshData()
                        travelRepository.refreshSavedTours(session.userId)
                        travelRepository.refreshUserNotifications(session.userId)
                    }
                }
            }
        }

        // Collect guest mode
        viewModelScope.launch {
            authRepository.isGuestFlow.collect { isGuest ->
                _uiState.update { current ->
                    current.copy(
                        isGuest = isGuest,
                        currentScreen = if (isGuest && (current.currentScreen is AppScreen.Welcome || current.currentScreen is AppScreen.Login)) {
                            AppScreen.Main
                        } else current.currentScreen
                    )
                }
            }
        }

        // Collect preferences
        viewModelScope.launch {
            appPreferences.themeModeFlow.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }

        viewModelScope.launch {
            appPreferences.languageFlow.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }

        // Collect repository data
        viewModelScope.launch {
            travelRepository.destinationsFlow.collect { dests ->
                _uiState.update { it.copy(destinations = dests) }
            }
        }

        viewModelScope.launch {
            travelRepository.toursFlow.collect { toursList ->
                _uiState.update { it.copy(tours = toursList) }
            }
        }

        viewModelScope.launch {
            travelRepository.notificationsFlow.collect { notifs ->
                _uiState.update { it.copy(notifications = notifs) }
            }
        }

        // Trigger initial data refresh
        viewModelScope.launch {
            travelRepository.refreshData()
        }
    }

    // Navigation methods
    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, authError = null) }
        if (screen is AppScreen.TripDetails) {
            loadTourDetails(screen.tourId)
        }
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab, currentScreen = AppScreen.Main) }
    }

    fun setCreatePlanSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isCreatePlanOpen = open, createPlanError = null) }
    }

    // Auth methods
    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val result = authRepository.signInWithEmail(email, pass)
            result.fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message ?: "Sign in failed") }
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, fullName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val result = authRepository.signUpWithEmail(email, pass, fullName)
            result.fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message ?: "Account creation failed") }
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
                it.copy(
                    session = null,
                    isGuest = false,
                    currentScreen = AppScreen.Login,
                    currentTab = NavigationTab.HOME
                )
            }
        }
    }

    fun onGoogleSignInSuccess(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val result = authRepository.signInWithGoogleToken(idToken, rawNonce)
            result.fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, currentScreen = AppScreen.Main) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, authError = error.message) }
                }
            )
        }
    }

    // Trip & Tour details
    fun loadTourDetails(tourId: String) {
        viewModelScope.launch {
            val tour = travelRepository.getTourById(tourId)
            val members = travelRepository.getTourMembers(tourId)
            val itinerary = travelRepository.getItinerary(tourId)
            val userId = _uiState.value.session?.userId
            val expenses = travelRepository.getExpenses(tourId, userId)
            val summary = travelRepository.getExpenseSummary(tourId, _uiState.value.session?.fullName ?: "Me")
            val settlements = travelRepository.getSettlements(tourId)
            val checklist = travelRepository.getChecklist(tourId)
            val comments = travelRepository.getTourComments(tourId, _uiState.value.session?.userId)

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
            val checklist = travelRepository.getChecklist(tourId)
            _uiState.update { it.copy(selectedTourChecklist = checklist) }
        }
    }

    fun addChecklistItem(title: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addChecklistItem(tourId, title)
            val checklist = travelRepository.getChecklist(tourId)
            _uiState.update { it.copy(selectedTourChecklist = checklist) }
        }
    }

    fun addSettlement(fromUser: String, toUser: String, amount: BigDecimal) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        viewModelScope.launch {
            travelRepository.addSettlement(tourId, fromUser, toUser, amount)
            loadTourDetails(tourId)
        }
    }

    fun createPlan(
        title: String,
        destinationName: String,
        startDate: String,
        endDate: String,
        budget: BigDecimal,
        isPublic: Boolean,
        description: String
    ) {
        viewModelScope.launch {
            val ownerId = _uiState.value.session?.userId
            if (ownerId.isNullOrBlank()) {
                _uiState.update { it.copy(createPlanError = "Sign in to create and save a tour.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, createPlanError = null) }
            val result = travelRepository.createPlan(
                ownerId,
                title,
                destinationName,
                startDate,
                endDate,
                budget,
                isPublic,
                description
            )
            result.fold(
                onSuccess = { tour ->
                    _uiState.update { it.copy(isLoading = false, isCreatePlanOpen = false) }
                    navigateTo(AppScreen.TripDetails(tour.id))
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, createPlanError = error.message ?: "Tour could not be saved")
                    }
                }
            )
        }
    }

    fun addExpense(title: String, amount: BigDecimal, category: ExpenseCategory, payer: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            travelRepository.addExpense(tourId, userId, title, amount, category, payer, userId)
            loadTourDetails(tourId)
        }
    }

    fun addItineraryItem(day: Int, time: String, location: String, title: String) {
        val tourId = _uiState.value.selectedTour?.id ?: return
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            travelRepository.addItineraryItem(tourId, userId, day, time, location, title)
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
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            travelRepository.addComment(tourId, userId, text)
            loadTourDetails(tourId)
        }
    }

    fun toggleSaveTour(tourId: String) {
        viewModelScope.launch {
            travelRepository.toggleSaveTour(tourId, _uiState.value.session?.userId)
        }
    }

    fun markNotificationRead(id: String) {
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            travelRepository.markNotificationRead(userId, id)
        }
    }

    fun markAllNotificationsRead() {
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            travelRepository.markAllNotificationsRead(userId)
        }
    }

    // Preferences
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            appPreferences.setLanguage(lang)
        }
    }
}
