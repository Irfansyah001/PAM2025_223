package com.umy.medremindid.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    fun scheduleNextForSchedule(context: Context, schedule: MedicationScheduleEntity) {
        val userId = schedule.userId
        val scheduleId = schedule.scheduleId

        if (!schedule.isActive) {
            cancelForSchedule(context, userId, scheduleId)
            return
        }

        val now = LocalDateTime.now()
        val next = computeNextOccurrence(
            now = now,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            timeOfDay = schedule.timeOfDay
        ) ?: run {
            cancelForSchedule(context, userId, scheduleId)
            return
        }

        val nextMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val plannedMillis = nextMillis

        val pi = pendingIntentForRemind(
            context = context,
            userId = userId,
            scheduleId = scheduleId,
            plannedMillis = plannedMillis,
            requestCode = requestCodeMain(userId, scheduleId)
        )

        setAlarm(context, nextMillis, pi)
    }

    fun scheduleSnooze(
        context: Context,
        userId: Long,
        scheduleId: Long,
        plannedMillis: Long,
        snoozeMinutes: Long
    ) {
        val triggerAt = System.currentTimeMillis() + (snoozeMinutes.coerceAtLeast(1) * 60_000L)

        val pi = pendingIntentForRemind(
            context = context,
            userId = userId,
            scheduleId = scheduleId,
            plannedMillis = plannedMillis,
            requestCode = requestCodeMain(userId, scheduleId)
        )

        setAlarm(context, triggerAt, pi)
    }

    fun cancelForSchedule(context: Context, userId: Long, scheduleId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pi = pendingIntentForRemind(
            context = context,
            userId = userId,
            scheduleId = scheduleId,
            plannedMillis = 0L,
            requestCode = requestCodeMain(userId, scheduleId)
        )

        alarm.cancel(pi)
        pi.cancel()
    }

    private fun computeNextOccurrence(
        now: LocalDateTime,
        startDate: LocalDate,
        endDate: LocalDate?,
        timeOfDay: LocalTime
    ): LocalDateTime? {

        val startCandidate = LocalDateTime.of(startDate, timeOfDay)

        val base = if (now.isBefore(startCandidate)) {
            startCandidate
        } else {
            val todayCandidate = LocalDateTime.of(now.toLocalDate(), timeOfDay)
            if (now.isBefore(todayCandidate)) todayCandidate else todayCandidate.plusDays(1)
        }

        if (endDate != null) {
            val endLimit = LocalDateTime.of(endDate, timeOfDay)
            if (base.isAfter(endLimit)) return null
        }

        return base
    }

    private fun pendingIntentForRemind(
        context: Context,
        userId: Long,
        scheduleId: Long,
        plannedMillis: Long,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderConstants.ACTION_REMIND
            putExtra(ReminderConstants.EXTRA_USER_ID, userId)
            putExtra(ReminderConstants.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(ReminderConstants.EXTRA_PLANNED_TIME_MILLIS, plannedMillis)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, pi: PendingIntent) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarm.cancel(pi)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarm.canScheduleExactAlarms()) {
                    alarm.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun requestCodeMain(userId: Long, scheduleId: Long): Int =
        "REMIND|$userId|$scheduleId".hashCode()
}
