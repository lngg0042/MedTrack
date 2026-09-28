package com.ng.s33986010.medtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.ui.screens.AppNavigation
import com.ng.s33986010.medtrack.ui.theme.MedTrackTheme
import com.ng.s33986010.medtrack.viewmodel.AuthViewModel
import com.ng.s33986010.medtrack.viewmodel.MedTrackViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedTrackTheme {
                val factory = MedTrackViewModelFactory(application)
                val authViewModel: AuthViewModel = viewModel(factory = factory)

                LaunchedEffect(Unit) {
                    authViewModel.seedDatabaseIfNeeded()
                    authViewModel.checkSession()
                }

                AppNavigation(
                    authViewModel = authViewModel,
                    factory = factory
                )
            }
        }
    }
}
