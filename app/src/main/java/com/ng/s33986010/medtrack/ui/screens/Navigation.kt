package com.ng.s33986010.medtrack.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.ng.s33986010.medtrack.viewmodel.*
import com.ng.s33986010.medtrack.BuildConfig

const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object ClaimAccount : Screen("claim_account")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Symptoms : Screen("symptoms")
    object MedCoach : Screen("medcoach")
    object Settings : Screen("settings")
    object AddMedication : Screen("add_medication")
    object ClinicianLogin : Screen("clinician_login")
    object ClinicianDashboard : Screen("clinician_dashboard")
    object SymptomTrend : Screen("symptom_trend")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    factory: ViewModelProvider.Factory
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val dest = navController.currentBackStackEntry?.destination?.route
                if (dest == Screen.Welcome.route || dest == Screen.Login.route ||
                    dest == Screen.ClaimAccount.route || dest == Screen.SignUp.route || dest == null) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            }
            is AuthState.LoggedOut -> {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onSignUpClick = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onClaimAccountClick = { navController.navigate(Screen.ClaimAccount.route) },
                onSignUpClick = { navController.navigate(Screen.SignUp.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ClaimAccount.route) {
            ClaimAccountScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            MainScaffold(
                currentRoute = Screen.Home.route,
                navController = navController
            ) {
                HomeScreen(
                    patientId = sessionId,
                    factory = factory,
                    onAddMedication = { navController.navigate(Screen.AddMedication.route) }
                )
            }
        }
        composable(Screen.Symptoms.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            MainScaffold(
                currentRoute = Screen.Symptoms.route,
                navController = navController
            ) {
                SymptomsScreen(
                    patientId = sessionId,
                    factory = factory,
                    onViewTrends = { navController.navigate(Screen.SymptomTrend.route) }
                )
            }
        }
        composable(Screen.MedCoach.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            MainScaffold(
                currentRoute = Screen.MedCoach.route,
                navController = navController
            ) {
                MedCoachScreen(
                    patientId = sessionId,
                    factory = factory,
                    geminiApiKey = GEMINI_API_KEY
                )
            }
        }
        composable(Screen.Settings.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            MainScaffold(
                currentRoute = Screen.Settings.route,
                navController = navController
            ) {
                SettingsScreen(
                    patientId = sessionId,
                    factory = factory,
                    authViewModel = authViewModel,
                    onClinicianLogin = { navController.navigate(Screen.ClinicianLogin.route) }
                )
            }
        }
        composable(Screen.AddMedication.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            AddMedicationScreen(
                patientId = sessionId,
                factory = factory,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SymptomTrend.route) {
            val sessionId = remember { (authState as? AuthState.Success)?.patientId ?: "" }
            SymptomTrendScreen(
                patientId = sessionId,
                factory = factory,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ClinicianLogin.route) {
            ClinicianLoginScreen(
                onSuccess = {
                    navController.navigate(Screen.ClinicianDashboard.route) {
                        popUpTo(Screen.ClinicianLogin.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ClinicianDashboard.route) {
            ClinicianDashboardScreen(
                factory = factory,
                geminiApiKey = GEMINI_API_KEY,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScaffold(
    currentRoute: String,
    navController: NavController,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Screen.Home.route, Icons.Default.Home, "Home"),
                    Triple(Screen.Symptoms.route, Icons.Default.HealthAndSafety, "Symptoms"),
                    Triple(Screen.MedCoach.route, Icons.Default.Psychology, "MedCoach"),
                    Triple(Screen.Settings.route, Icons.Default.Settings, "Settings")
                ).forEach { (route, icon, label) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            content()
        }
    }
}
