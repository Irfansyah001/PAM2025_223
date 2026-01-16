package com.umy.medremindid.di

import android.content.Context
import com.umy.medremindid.data.local.db.AppDatabase
import com.umy.medremindid.data.repository.AuthRepository
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.session.SessionManager

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

    val session: SessionManager = sessionManager
}
