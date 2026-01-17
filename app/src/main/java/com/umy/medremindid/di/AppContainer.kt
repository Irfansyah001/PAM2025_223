package com.umy.medremindid.di

import android.content.Context
import com.umy.medremindid.data.local.db.AppDatabase
import com.umy.medremindid.data.repository.AdherenceLogRepository
import com.umy.medremindid.data.repository.AuthRepository
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.repository.NotificationPreferenceRepository
import com.umy.medremindid.data.repository.SymptomNoteRepository
import com.umy.medremindid.data.session.SessionManager

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val sessionManager = SessionManager(context)

    val session: SessionManager = sessionManager

    val authRepository: AuthRepository = AuthRepository(
        userDao = db.userDao(),
        sessionManager = sessionManager
    )

    val medicationScheduleRepository: MedicationScheduleRepository =
        MedicationScheduleRepository(db.medicationScheduleDao())

    val adherenceLogRepository: AdherenceLogRepository =
        AdherenceLogRepository(db.adherenceLogDao())

    val notificationPreferenceRepository: NotificationPreferenceRepository =
        NotificationPreferenceRepository(db.notificationPreferenceDao())

    val symptomNoteRepository: SymptomNoteRepository =
        SymptomNoteRepository(db.symptomNoteDao())
}
