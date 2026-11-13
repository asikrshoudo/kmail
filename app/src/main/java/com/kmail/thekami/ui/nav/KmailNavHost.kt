package com.kmail.thekami.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmail.thekami.ui.home.HomeScreen
import com.kmail.thekami.ui.setup.CredentialsScreen
import com.kmail.thekami.ui.setup.SetupViewModel
import com.kmail.thekami.ui.setup.WelcomeScreen

@Composable
fun KmailNavHost(
    navController: NavHostController = rememberNavController(),
) {
    // One ViewModel for the whole setup flow, owned at the NavHost level.
    val setupViewModel: SetupViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(Routes.CREDENTIALS) })
        }
        composable(Routes.CREDENTIALS) {
            val state by setupViewModel.credentials.collectAsStateWithLifecycle()
            CredentialsScreen(
                state = state,
                onEmailChange = setupViewModel::onEmailChange,
                onPasswordChange = setupViewModel::onPasswordChange,
                onContinue = {
                    if (setupViewModel.submitCredentials()) {
                        // Step 2.6 navigates to the incoming server screen here.
                    }
                },
            )
        }
        composable(Routes.HOME) { HomeScreen() }
    }
}
