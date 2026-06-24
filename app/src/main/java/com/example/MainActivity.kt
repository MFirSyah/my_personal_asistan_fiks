package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceActivityViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FinanceActivityViewModel = viewModel()
                val authState by viewModel.authState.collectAsState()
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!onboardingCompleted) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onFinished = { viewModel.completeOnboarding() }
                        )
                    } else if (authState.isLoggedIn) {
                        MainScreen(
                            viewModel = viewModel,
                            onLogout = { viewModel.handleLogout() }
                        )
                    } else {
                        AuthScreen(
                            viewModel = viewModel,
                            onBypass = { viewModel.handleSandboxBypass() }
                        )
                    }
                }
            }
        }
    }
}
