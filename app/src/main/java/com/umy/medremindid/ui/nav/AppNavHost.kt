package com.umy.medremindid.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.umy.medremindid.data.session.SessionManager
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import com.umy.medremindid.ui.auth.AuthViewModel
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import com.umy.medremindid.ui.screens.AdherenceHistoryScreen
import com.umy.medremindid.ui.screens.AdherenceSummaryScreen
import com.umy.medremindid.ui.screens.HomeScreen
import com.umy.medremindid.ui.screens.LoginScreen
import com.umy.medremindid.ui.screens.RegisterScreen
import com.umy.medremindid.ui.screens.ScheduleFormScreen
import com.umy.medremindid.ui.screens.ScheduleListScreen
import com.umy.medremindid.ui.screens.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    session: SessionManager,
    authViewModel: AuthViewModel,
    scheduleViewModel: MedicationScheduleViewModel,
    adherenceViewModel: AdherenceViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            val loggedIn by session.isLoggedInFlow.collectAsState(initial = false)

            LaunchedEffect(loggedIn) {
                navController.navigate(if (loggedIn) Routes.HOME else Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }

            SplashScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onGoLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onGoSchedules = { navController.navigate(Routes.SCHEDULE_LIST) },
                onGoAdherenceHistory = { navController.navigate(Routes.ADHERENCE_HISTORY) },
                onGoAdherenceSummary = { navController.navigate(Routes.ADHERENCE_SUMMARY) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.SCHEDULE_LIST) {
            ScheduleListScreen(
                viewModel = scheduleViewModel,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.scheduleFormRoute(null)) },
                onEdit = { sid -> navController.navigate(Routes.scheduleFormRoute(sid)) }
            )
        }

        composable(
            route = Routes.SCHEDULE_FORM_ROUTE,
            arguments = listOf(
                navArgument("sid") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val sid = backStackEntry.arguments?.getLong("sid") ?: -1L
            val scheduleId = if (sid == -1L) null else sid

            ScheduleFormScreen(
                viewModel = scheduleViewModel,
                scheduleId = scheduleId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.SCHEDULE_LIST, false) }
            )
        }

        composable(Routes.ADHERENCE_HISTORY) {
            AdherenceHistoryScreen(
                viewModel = adherenceViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADHERENCE_SUMMARY) {
            AdherenceSummaryScreen(
                viewModel = adherenceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
