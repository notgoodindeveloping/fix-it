package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.SessionManager
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.viewmodel.ExploreViewModel
import com.example.ui.viewmodel.LoginViewModel
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.DetailViewModel

object Routes {
    const val LOGIN = "login"
    const val EXPLORE = "explore"
    const val REPORT = "report"
    const val DETAIL = "detail/{reportId}"
}

@Composable
fun FixItNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Auth Guard check for start destination
    val startDestination = if (sessionManager.isLoggedIn) {
        Routes.EXPLORE
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.EXPLORE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.EXPLORE) {
            val exploreViewModel: ExploreViewModel = viewModel()
            // Make sure we refresh data on entry
            LaunchedEffect(Unit) {
                exploreViewModel.refreshReports()
            }
            ExploreScreen(
                viewModel = exploreViewModel,
                onNavigateToAddReport = {
                    navController.navigate(Routes.REPORT)
                },
                onNavigateToDetail = { reportId ->
                    navController.navigate("detail/$reportId")
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.EXPLORE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REPORT) {
            val reportViewModel: ReportViewModel = viewModel()
            ReportScreen(
                viewModel = reportViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.DETAIL) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId")?.toLongOrNull() ?: 0L
            val detailViewModel: DetailViewModel = viewModel()
            DetailScreen(
                viewModel = detailViewModel,
                reportId = reportId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
