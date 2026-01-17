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
import com.umy.medremindid.ui.settings.NotificationSettingsViewModel
import com.umy.medremindid.ui.symptom.SymptomNoteViewModel
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
                        appContext = applicationContext,
                        session = container.session,
                        repo = container.adherenceLogRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val symptomVmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SymptomNoteViewModel::class.java)) {
                    return SymptomNoteViewModel(
                        session = container.session,
                        symptomRepo = container.symptomNoteRepository,
                        scheduleRepo = container.medicationScheduleRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val notifVmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java)) {
                    return NotificationSettingsViewModel(
                        session = container.session,
                        repo = container.notificationPreferenceRepository
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

                val symptomViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<SymptomNoteViewModel>(factory = symptomVmFactory)

                val notifSettingsViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<NotificationSettingsViewModel>(factory = notifVmFactory)

                val loggedIn by container.session.isLoggedInFlow.collectAsState(initial = false)

                PermissionGate(enabled = loggedIn) {
                    AppNavHost(
                        navController = navController,
                        session = container.session,
                        authViewModel = authViewModel,
                        scheduleViewModel = scheduleViewModel,
                        adherenceViewModel = adherenceViewModel,
                        symptomViewModel = symptomViewModel,
                        notifSettingsViewModel = notifSettingsViewModel
                    )
                }
            }
        }
    }
}
