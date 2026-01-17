package com.umy.medremindid.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.umy.medremindid.MainActivity
import com.umy.medremindid.R
import com.umy.medremindid.data.local.db.AppDatabase
import com.umy.medremindid.data.local.entity.NotificationPreferenceEntity
import com.umy.medremindid.data.repository.AdherenceLogRepository
import com.umy.medremindid.data.repository.NotificationPreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                handle(appContext, intent)
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val userId = intent.getLongExtra(ReminderConstants.EXTRA_USER_ID, -1L)
        val scheduleId = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULE_ID, -1L)
        val plannedMillisRaw = intent.getLongExtra(ReminderConstants.EXTRA_PLANNED_TIME_MILLIS, -1L)

        if (userId <= 0 || scheduleId <= 0) return

        val db = AppDatabase.getInstance(context)
        val adherenceRepo = AdherenceLogRepository(db.adherenceLogDao())
        val prefRepo = NotificationPreferenceRepository(db.notificationPreferenceDao())
        val scheduleDao = db.medicationScheduleDao()

        when (action) {
            ReminderConstants.ACTION_REMIND -> {
                val schedule = scheduleDao.getById(userId, scheduleId) ?: run {
                    ReminderScheduler.cancelForSchedule(context, userId, scheduleId)
                    cancelMissedByTag(context, userId, scheduleId)
                    return
                }

                if (!schedule.isActive) {
                    ReminderScheduler.cancelForSchedule(context, userId, scheduleId)
                    cancelMissedByTag(context, userId, scheduleId)
                    return
                }

                val pref = prefRepo.getOrDefault(userId)
                if (!pref.notificationsEnabled) {
                    cancelMissedByTag(context, userId, scheduleId)
                    return
                }

                val effectivePlannedMillis =
                    if (plannedMillisRaw > 0) plannedMillisRaw else System.currentTimeMillis()

                val nowLocal = LocalTime.now()
                if (prefRepo.isInQuietHours(pref, nowLocal)) {
                    val snoozeMinutes = minutesUntilQuietEnd(pref, nowLocal)

                    ReminderScheduler.scheduleSnooze(
                        context = context,
                        userId = userId,
                        scheduleId = scheduleId,
                        plannedMillis = effectivePlannedMillis,
                        snoozeMinutes = snoozeMinutes
                    )

                    enqueueMissedWorker(
                        context = context,
                        userId = userId,
                        scheduleId = scheduleId,
                        plannedMillis = effectivePlannedMillis,
                        delayMinutes = snoozeMinutes + ReminderConstants.DEFAULT_GRACE_MINUTES
                    )
                    return
                }

                val channelId = ensureReminderChannel(context, pref)

                showReminderNotification(
                    context = context,
                    channelId = channelId,
                    pref = pref,
                    userId = userId,
                    scheduleId = scheduleId,
                    plannedMillis = effectivePlannedMillis,
                    title = "Waktu Minum Obat",
                    body = "${schedule.medicineName} • ${schedule.dosage}"
                )

                enqueueMissedWorker(
                    context = context,
                    userId = userId,
                    scheduleId = scheduleId,
                    plannedMillis = effectivePlannedMillis,
                    delayMinutes = ReminderConstants.DEFAULT_GRACE_MINUTES
                )

                // Stabil: selalu jadwalkan pengingat berikutnya walaupun user mengabaikan notifikasi
                ReminderScheduler.scheduleNextForSchedule(context, schedule)
            }

            ReminderConstants.ACTION_TAKEN -> {
                val plannedMillis = plannedMillisRaw
                if (plannedMillis <= 0) return

                adherenceRepo.markTaken(userId, scheduleId, Instant.ofEpochMilli(plannedMillis))
                cancelMissedUnique(context, userId, scheduleId, plannedMillis)
                dismissNotification(context, scheduleId, plannedMillis)
            }

            ReminderConstants.ACTION_SKIPPED -> {
                val plannedMillis = plannedMillisRaw
                if (plannedMillis <= 0) return

                adherenceRepo.markSkipped(userId, scheduleId, Instant.ofEpochMilli(plannedMillis))
                cancelMissedUnique(context, userId, scheduleId, plannedMillis)
                dismissNotification(context, scheduleId, plannedMillis)
            }

            ReminderConstants.ACTION_SNOOZE -> {
                val plannedMillis = plannedMillisRaw
                if (plannedMillis <= 0) return

                ReminderScheduler.scheduleSnooze(
                    context = context,
                    userId = userId,
                    scheduleId = scheduleId,
                    plannedMillis = plannedMillis,
                    snoozeMinutes = ReminderConstants.DEFAULT_SNOOZE_MINUTES
                )

                cancelMissedUnique(context, userId, scheduleId, plannedMillis)

                enqueueMissedWorker(
                    context = context,
                    userId = userId,
                    scheduleId = scheduleId,
                    plannedMillis = plannedMillis,
                    delayMinutes = ReminderConstants.DEFAULT_SNOOZE_MINUTES + ReminderConstants.DEFAULT_GRACE_MINUTES
                )

                dismissNotification(context, scheduleId, plannedMillis)
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        channelId: String,
        pref: NotificationPreferenceEntity,
        userId: Long,
        scheduleId: Long,
        plannedMillis: Long,
        title: String,
        body: String
    ) {
        if (!canPostNotifications(context)) return

        val notifId = makeNotificationId(scheduleId, plannedMillis)

        val takenPI =
            PendingIntents.action(context, ReminderConstants.ACTION_TAKEN, userId, scheduleId, plannedMillis)
        val skipPI =
            PendingIntents.action(context, ReminderConstants.ACTION_SKIPPED, userId, scheduleId, plannedMillis)
        val snoozePI =
            PendingIntents.action(context, ReminderConstants.ACTION_SNOOZE, userId, scheduleId, plannedMillis)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPI = PendingIntent.getActivity(
            context,
            ("OPEN|$scheduleId|$plannedMillis").hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPI)
            .addAction(0, "Sudah", takenPI)
            .addAction(0, "Lewati", skipPI)
            .addAction(0, "Tunda", snoozePI)

        // Best-effort untuk Android < 8 (karena Android 8+ mengikuti channel)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (pref.allowVibration) {
                builder.setVibrate(longArrayOf(0, 250, 250, 250))
            }
            val soundUri = pref.ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            if (soundUri != null) builder.setSound(soundUri)
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureReminderChannel(context: Context, pref: NotificationPreferenceEntity): String {
        val channelId = ReminderConstants.channelId(pref.allowVibration, pref.ringtoneUri)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(channelId)
        if (existing != null) return channelId

        val channel = NotificationChannel(
            channelId,
            ReminderConstants.CHANNEL_NAME_REMINDERS,
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.enableVibration(pref.allowVibration)

        val soundUri = pref.ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (soundUri != null) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            channel.setSound(soundUri, attrs)
        }

        nm.createNotificationChannel(channel)
        return channelId
    }

    private fun enqueueMissedWorker(
        context: Context,
        userId: Long,
        scheduleId: Long,
        plannedMillis: Long,
        delayMinutes: Long
    ) {
        val workName = ReminderConstants.missedWorkName(userId, scheduleId, plannedMillis)
        val tag = ReminderConstants.missedWorkTag(userId, scheduleId)

        val req = OneTimeWorkRequestBuilder<MissedDoseWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(tag)
            .setInputData(
                workDataOf(
                    ReminderConstants.EXTRA_USER_ID to userId,
                    ReminderConstants.EXTRA_SCHEDULE_ID to scheduleId,
                    ReminderConstants.EXTRA_PLANNED_TIME_MILLIS to plannedMillis
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    private fun cancelMissedUnique(context: Context, userId: Long, scheduleId: Long, plannedMillis: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(ReminderConstants.missedWorkName(userId, scheduleId, plannedMillis))
    }

    private fun cancelMissedByTag(context: Context, userId: Long, scheduleId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(ReminderConstants.missedWorkTag(userId, scheduleId))
    }

    private fun dismissNotification(context: Context, scheduleId: Long, plannedMillis: Long) {
        if (!canPostNotifications(context)) return
        try {
            NotificationManagerCompat.from(context).cancel(makeNotificationId(scheduleId, plannedMillis))
        } catch (_: SecurityException) {
        }
    }

    private fun makeNotificationId(scheduleId: Long, plannedMillis: Long): Int {
        return ("N|$scheduleId|$plannedMillis").hashCode()
    }

    private fun minutesUntilQuietEnd(
        pref: NotificationPreferenceEntity,
        now: LocalTime
    ): Long {
        val start = pref.quietHoursStart ?: return ReminderConstants.DEFAULT_SNOOZE_MINUTES
        val end = pref.quietHoursEnd ?: return ReminderConstants.DEFAULT_SNOOZE_MINUTES

        return if (start <= end) {
            java.time.Duration.between(now, end).toMinutes().coerceAtLeast(1)
        } else {
            val minutes = if (now >= start) {
                java.time.Duration.between(now, LocalTime.MIDNIGHT).toMinutes() +
                        java.time.Duration.between(LocalTime.MIN, end).toMinutes()
            } else {
                java.time.Duration.between(now, end).toMinutes()
            }
            minutes.coerceAtLeast(1)
        }
    }
}

private object PendingIntents {
    fun action(
        context: Context,
        action: String,
        userId: Long,
        scheduleId: Long,
        plannedMillis: Long
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderConstants.EXTRA_USER_ID, userId)
            putExtra(ReminderConstants.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(ReminderConstants.EXTRA_PLANNED_TIME_MILLIS, plannedMillis)
        }

        val requestCode = ("A|$action|$userId|$scheduleId|$plannedMillis").hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
