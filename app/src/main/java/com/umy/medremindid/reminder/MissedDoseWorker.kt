package com.umy.medremindid.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.umy.medremindid.data.local.db.AppDatabase
import com.umy.medremindid.data.repository.AdherenceLogRepository
import java.time.Instant

class MissedDoseWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getLong(ReminderConstants.EXTRA_USER_ID, -1L)
        val scheduleId = inputData.getLong(ReminderConstants.EXTRA_SCHEDULE_ID, -1L)
        val plannedMillis = inputData.getLong(ReminderConstants.EXTRA_PLANNED_TIME_MILLIS, -1L)
        if (userId <= 0 || scheduleId <= 0 || plannedMillis <= 0) return Result.failure()

        return try {
            val db = AppDatabase.getInstance(applicationContext)

            val adherenceRepo = AdherenceLogRepository(db.adherenceLogDao())
            adherenceRepo.markMissedIfAbsent(
                userId = userId,
                scheduleId = scheduleId,
                plannedTime = Instant.ofEpochMilli(plannedMillis)
            )

            val scheduleDao = db.medicationScheduleDao()
            val schedule = scheduleDao.getById(userId, scheduleId)

            if (schedule == null) {
                ReminderScheduler.cancelForSchedule(applicationContext, userId, scheduleId)
                return Result.success()
            }

            if (!schedule.isActive) {
                ReminderScheduler.cancelForSchedule(applicationContext, userId, scheduleId)
                return Result.success()
            }

            ReminderScheduler.scheduleNextForSchedule(applicationContext, schedule)

            Result.success()
        } catch (_: IllegalArgumentException) {
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
