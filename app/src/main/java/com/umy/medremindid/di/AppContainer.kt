package com.umy.medremindid.di

import android.content.Context
import com.umy.medremindid.data.local.db.AppDatabase
import com.umy.medremindid.data.repository.AuthRepository
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.session.SessionManager
import com.umy.medremindid.data.repository.AdherenceLogRepository
import com.umy.medremindid.data.repository.NotificationPreferenceRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val sessionManager = SessionManager(context)

    val authRepository: AuthRepository = AuthRepository(
        userDao = db.userDao(),
        sessionManager = sessionManager
    )

    val medicationScheduleRepository: MedicationScheduleRepository =
        MedicationScheduleRepository(
            dao = db.medicationScheduleDao()
        )

    val adherenceLogRepository: AdherenceLogRepository =
        AdherenceLogRepository(
            dao = db.adherenceLogDao()
        )

    val notificationPreferenceRepository: NotificationPreferenceRepository =
        NotificationPreferenceRepository(
            dao = db.notificationPreferenceDao()
        )

    val session: SessionManager = sessionManager
}
