package com.contextguard.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.contextguard.app.ui.screens.*
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun ContextGuardNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onEnterClicked = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAnalyze = { navController.navigate(Screen.Analyze.route) },
                onNavigateToDemo = { navController.navigate(Screen.Demo.route) },
                onNavigateToSupervisor = { navController.navigate(Screen.Supervisor.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Analyze.route) {
            AnalyzeScreen(
                viewModel = viewModel,
                onResultReady = { navController.navigate(Screen.Result.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Demo.route) {
            DemoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onResultReady = { navController.navigate(Screen.Result.route) }
            )
        }

        composable(Screen.Supervisor.route) {
            SupervisorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
