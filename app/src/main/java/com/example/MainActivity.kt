package com.example

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.NavigationTab
import com.example.core.designsystem.TribeBottomBar
import com.example.feature.auth.LoginScreen
import com.example.feature.auth.RegisterScreen
import com.example.feature.auth.WelcomeScreen
import com.example.feature.explore.ExploreScreen
import com.example.feature.home.HomeScreen
import com.example.feature.notifications.NotificationsScreen
import com.example.feature.plans.CreatePlanSheet
import com.example.feature.plans.MyPlansScreen
import com.example.feature.profile.ProfileScreen
import com.example.feature.tourdetails.TripDetailsScreen
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.TribeTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle possible deep link callback
        handleDeepLink(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val coroutineScope = rememberCoroutineScope()

            // Apply selected language locale
            val currentLocale = remember(uiState.language) {
                val locale = Locale(uiState.language)
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
                locale
            }

            TribeTheme(themeMode = uiState.themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            is AppScreen.Welcome -> {
                                WelcomeScreen(
                                    onContinue = { viewModel.navigateTo(AppScreen.Login) }
                                )
                            }
                            is AppScreen.Login -> {
                                LoginScreen(
                                    onLoginClick = { email, pass ->
                                        viewModel.signInWithEmail(email, pass)
                                    },
                                    onGoogleSignInClick = {
                                        coroutineScope.launch {
                                            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                                            if (clientId.isBlank() || clientId.contains("placeholder", ignoreCase = true)) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Google Sign-In is not configured. Please check the Web OAuth Client ID.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            val result = viewModel.googleAuthHelper.signInWithGoogle(
                                                activityContext = this@MainActivity,
                                                serverClientId = clientId
                                            )
                                            result.fold(
                                                onSuccess = { res ->
                                                    viewModel.onGoogleSignInSuccess(res.idToken, res.rawNonce)
                                                },
                                                onFailure = { err ->
                                                    val message = when (err) {
                                                        is com.example.core.security.GoogleAuthException.Cancelled ->
                                                            "Google Sign-In cancelled"
                                                        is com.example.core.security.GoogleAuthException.NoCredentials ->
                                                            "No Google account available. Try the account chooser or add a Google account in Android Settings."
                                                        is com.example.core.security.GoogleAuthException.ConfigurationError ->
                                                            "Configuration error: ${err.message}"
                                                        else ->
                                                            err.message ?: "Google Sign-In failed"
                                                    }
                                                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    onRegisterClick = { viewModel.navigateTo(AppScreen.Register) },
                                    onForgotPasswordClick = { email ->
                                        coroutineScope.launch {
                                            val result = viewModel.authRepository.recoverPassword(email)
                                            result.fold(
                                                onSuccess = {
                                                    Toast.makeText(this@MainActivity, "If the account exists, password reset instructions have been sent.", Toast.LENGTH_LONG).show()
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        error.message ?: "Could not send password reset email.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            )
                                        }
                                    },
                                    onGuestClick = { viewModel.continueAsGuest() },
                                    isLoading = uiState.isLoading,
                                    errorMessage = uiState.authError
                                )
                            }
                            is AppScreen.Register -> {
                                BackHandler { viewModel.navigateTo(AppScreen.Login) }
                                RegisterScreen(
                                    onRegisterClick = { email, pass, name ->
                                        viewModel.signUpWithEmail(email, pass, name)
                                    },
                                    onLoginClick = { viewModel.navigateTo(AppScreen.Login) },
                                    isLoading = uiState.isLoading,
                                    errorMessage = uiState.authError
                                )
                            }
                            is AppScreen.Main -> {
                                Scaffold(
                                    bottomBar = {
                                        TribeBottomBar(
                                            currentTab = uiState.currentTab,
                                            onTabSelected = { viewModel.selectTab(it) },
                                            onCreatePlanClick = { viewModel.setCreatePlanSheetOpen(true) }
                                        )
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        when (uiState.currentTab) {
                                            NavigationTab.HOME -> {
                                                HomeScreen(
                                                    userName = uiState.session?.fullName ?: "Traveler",
                                                    destinations = uiState.destinations,
                                                    userPlans = uiState.tours,
                                                    onSearchClick = { viewModel.selectTab(NavigationTab.EXPLORE) },
                                                    onDestinationClick = { dest ->
                                                        val tour = uiState.tours.find { it.destinationName.contains(dest.name, ignoreCase = true) }
                                                        if (tour != null) {
                                                            viewModel.navigateTo(AppScreen.TripDetails(tour.id))
                                                        } else {
                                                            viewModel.selectTab(NavigationTab.EXPLORE)
                                                        }
                                                    },
                                                    onTourClick = { tour ->
                                                        viewModel.navigateTo(AppScreen.TripDetails(tour.id))
                                                    },
                                                    onSeeAllPlansClick = { viewModel.selectTab(NavigationTab.MY_PLANS) },
                                                    onSeeAllDestinationsClick = { viewModel.selectTab(NavigationTab.EXPLORE) },
                                                    onNotificationsClick = { viewModel.navigateTo(AppScreen.Notifications) }
                                                )
                                            }
                                            NavigationTab.MY_PLANS -> {
                                                MyPlansScreen(
                                                    plans = uiState.tours,
                                                    onPlanClick = { tour ->
                                                        viewModel.navigateTo(AppScreen.TripDetails(tour.id))
                                                    },
                                                    onCreatePlanClick = { viewModel.setCreatePlanSheetOpen(true) }
                                                )
                                            }
                                            NavigationTab.EXPLORE -> {
                                                ExploreScreen(
                                                    tours = uiState.tours,
                                                    onTourClick = { tour ->
                                                        viewModel.navigateTo(AppScreen.TripDetails(tour.id))
                                                    },
                                                    onToggleSaveTour = { tourId ->
                                                        viewModel.toggleSaveTour(tourId)
                                                    }
                                                )
                                            }
                                            NavigationTab.PROFILE -> {
                                                ProfileScreen(
                                                    userName = uiState.session?.fullName ?: "Dhaka Rider",
                                                    userEmail = uiState.session?.email ?: "dh.rider@gmail.com",
                                                    currentTheme = uiState.themeMode,
                                                    currentLanguage = uiState.language,
                                                    onThemeChanged = { viewModel.setThemeMode(it) },
                                                    onLanguageChanged = { lang ->
                                                        viewModel.setLanguage(lang)
                                                        recreate()
                                                    },
                                                    onLogoutClick = { viewModel.logout() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is AppScreen.TripDetails -> {
                                BackHandler { viewModel.navigateTo(AppScreen.Main) }
                                val selectedTour = uiState.selectedTour ?: uiState.tours.firstOrNull()
                                if (selectedTour != null) {
                                    TripDetailsScreen(
                                        tour = selectedTour,
                                        members = uiState.selectedTourMembers,
                                        itinerary = uiState.selectedTourItinerary,
                                        expenses = uiState.selectedTourExpenses,
                                        expenseSummary = uiState.selectedTourExpenseSummary,
                                        settlements = uiState.selectedTourSettlements,
                                        checklist = uiState.selectedTourChecklist,
                                        comments = uiState.selectedTourComments,
                                        onBackClick = { viewModel.navigateTo(AppScreen.Main) },
                                        onAddExpense = { title, amt, cat, payer ->
                                            viewModel.addExpense(title, amt, cat, payer)
                                        },
                                        onAddItineraryItem = { day, time, loc, title ->
                                            viewModel.addItineraryItem(day, time, loc, title)
                                        },
                                        onInviteMember = { name ->
                                            viewModel.inviteMember(name)
                                        },
                                        onAddComment = { text ->
                                            viewModel.addComment(text)
                                        },
                                        onToggleChecklistItem = { viewModel.toggleChecklistItem(it) },
                                        onAddChecklistItem = { viewModel.addChecklistItem(it) },
                                        onSettleUp = { from, to, amt -> viewModel.addSettlement(from, to, amt) },
                                        onShareClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Join my travel tribe trip to ${selectedTour.title} on TRIBE app!")
                                                type = "text/plain"
                                            }
                                            startActivity(Intent.createChooser(sendIntent, "Share Trip"))
                                        }
                                    )
                                }
                            }
                            is AppScreen.Notifications -> {
                                BackHandler { viewModel.navigateTo(AppScreen.Main) }
                                NotificationsScreen(
                                    notifications = uiState.notifications,
                                    onBackClick = { viewModel.navigateTo(AppScreen.Main) },
                                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                    onNotificationClick = { notif ->
                                        viewModel.markNotificationRead(notif.id)
                                    }
                                )
                            }
                        }
                    }

                    // Create Plan Modal Bottom Sheet
                    if (uiState.isCreatePlanOpen) {
                        CreatePlanSheet(
                            onDismiss = { viewModel.setCreatePlanSheetOpen(false) },
                            onCreatePlan = { title, dest, start, end, budget, isPub, desc ->
                                viewModel.createPlan(title, dest, start, end, budget, isPub, desc)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "tribe" && data.host == "auth-callback") {
            val code = data.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                Toast.makeText(this, "Authentication callback received", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
