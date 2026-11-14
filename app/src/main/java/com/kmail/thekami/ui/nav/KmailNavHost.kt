package com.kmail.thekami.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.LoadingState
import com.kmail.thekami.ui.home.HomeScreen
import com.kmail.thekami.ui.reader.ReaderScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kmail.thekami.ui.setup.ConnectState
import com.kmail.thekami.ui.setup.ConnectedScreen
import com.kmail.thekami.ui.setup.ConnectingScreen
import com.kmail.thekami.ui.setup.CredentialsScreen
import com.kmail.thekami.ui.setup.ServerScreen
import com.kmail.thekami.ui.setup.ServerSide
import com.kmail.thekami.ui.setup.SetupViewModel
import com.kmail.thekami.ui.setup.WelcomeScreen
import com.kmail.thekami.ui.setup.hintRes

@Composable
fun KmailNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val appStart: AppStartViewModel = hiltViewModel()
    // One ViewModel for the whole setup flow, owned at the NavHost level.
    val vm: SetupViewModel = hiltViewModel()

    val start by appStart.startRoute.collectAsStateWithLifecycle()
    val startRoute = start ?: run {
        LoadingState(message = stringResource(R.string.loading_generic))
        return
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(Routes.CREDENTIALS) })
        }

        composable(Routes.CREDENTIALS) {
            val state by vm.credentials.collectAsStateWithLifecycle()
            CredentialsScreen(
                state = state,
                onEmailChange = vm::onEmailChange,
                onPasswordChange = vm::onPasswordChange,
                onContinue = { if (vm.submitCredentials()) navController.navigate(Routes.INCOMING) },
            )
        }

        composable(Routes.INCOMING) {
            val state by vm.incoming.collectAsStateWithLifecycle()
            val note by vm.note.collectAsStateWithLifecycle()
            val hintId = note?.hintRes()
            ServerScreen(
                title = stringResource(R.string.incoming_title),
                protocol = "IMAP",
                hint = hintId?.let { stringResource(it) },
                state = state,
                onHostChange = { vm.onServerHost(ServerSide.INCOMING, it) },
                onPortChange = { vm.onServerPort(ServerSide.INCOMING, it) },
                onSecurityChange = { vm.onServerSecurity(ServerSide.INCOMING, it) },
                onUsernameChange = { vm.onServerUsername(ServerSide.INCOMING, it) },
                onPasswordChange = { vm.onServerPassword(ServerSide.INCOMING, it) },
                continueLabel = stringResource(R.string.server_continue),
                onContinue = {
                    if (vm.submitServer(ServerSide.INCOMING)) navController.navigate(Routes.OUTGOING)
                },
            )
        }

        composable(Routes.OUTGOING) {
            val state by vm.outgoing.collectAsStateWithLifecycle()
            ServerScreen(
                title = stringResource(R.string.outgoing_title),
                protocol = "SMTP",
                hint = null,
                state = state,
                onHostChange = { vm.onServerHost(ServerSide.OUTGOING, it) },
                onPortChange = { vm.onServerPort(ServerSide.OUTGOING, it) },
                onSecurityChange = { vm.onServerSecurity(ServerSide.OUTGOING, it) },
                onUsernameChange = { vm.onServerUsername(ServerSide.OUTGOING, it) },
                onPasswordChange = { vm.onServerPassword(ServerSide.OUTGOING, it) },
                continueLabel = stringResource(R.string.server_connect),
                onContinue = {
                    if (vm.submitServer(ServerSide.OUTGOING)) navController.navigate(Routes.CONNECTING)
                },
            )
        }

        composable(Routes.CONNECTING) {
            val state by vm.connectState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { vm.connect() }
            LaunchedEffect(state) {
                if (state is ConnectState.Connected) {
                    navController.navigate(Routes.CONNECTED) {
                        popUpTo(Routes.CREDENTIALS) { inclusive = true }
                    }
                }
            }
            BackHandler(enabled = state is ConnectState.Testing) {
                vm.cancelConnect()
                navController.popBackStack()
            }
            ConnectingScreen(state = state, onRetry = vm::connect)
        }

        composable(Routes.CONNECTED) {
            val state by vm.connectState.collectAsStateWithLifecycle()
            val email = (state as? ConnectState.Connected)?.emailAddress.orEmpty()
            val openInbox = {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                Unit
            }
            BackHandler { openInbox() }
            ConnectedScreen(
                emailAddress = email,
                onOpenInbox = { openInbox() },
                onAddAnother = {
                    vm.startNewAccount()
                    navController.navigate(Routes.CREDENTIALS) {
                        popUpTo(Routes.CONNECTED) { inclusive = true }
                    }
                },
            )
        }

        composable(
            Routes.READER,
            arguments = listOf(navArgument("messageId") { type = NavType.LongType }),
        ) {
            ReaderScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenMessage = { navController.navigate(Routes.reader(it)) },
                onAddAccount = {
                    vm.startNewAccount()
                    navController.navigate(Routes.CREDENTIALS)
                },
            )
        }
    }
}
