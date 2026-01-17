package com.umy.medremindid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.umy.medremindid.di.AppContainer
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import com.umy.medremindid.ui.auth.AuthViewModel
import com.umy.medremindid.ui.nav.AppNavHost
import com.umy.medremindid.ui.permissions.PermissionGate
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import com.umy.medremindid.ui.theme.MedRemindIDTheme

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = AppContainer(applicationContext)

        val authVmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    return AuthViewModel(container.authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val scheduleVmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MedicationScheduleViewModel::class.java)) {
                    return MedicationScheduleViewModel(
                        appContext = applicationContext,
                        session = container.session,
                        repo = container.medicationScheduleRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val adherenceVmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AdherenceViewModel::class.java)) {
                    return AdherenceViewModel(
                        session = container.session,
                        repo = container.adherenceLogRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            MedRemindIDTheme {
                val navController = rememberNavController()

                val authViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<AuthViewModel>(factory = authVmFactory)

                val scheduleViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<MedicationScheduleViewModel>(factory = scheduleVmFactory)

                val adherenceViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<AdherenceViewModel>(factory = adherenceVmFactory)

                val loggedIn by container.session.isLoggedInFlow.collectAsState(initial = false)

                PermissionGate(enabled = loggedIn) {
                    AppNavHost(
                        navController = navController,
                        session = container.session,
                        authViewModel = authViewModel,
                        scheduleViewModel = scheduleViewModel,
                        adherenceViewModel = adherenceViewModel
                    )
                }
            }
        }
    }
}
