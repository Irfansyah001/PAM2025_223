package com.umy.medremindid.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.umy.medremindid.data.local.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pending = goAsync()
        val appContext = context.applicationContext

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val activeSchedules = db.medicationScheduleDao().getAllActive()
                activeSchedules.forEach { schedule ->
                    try {
                        ReminderScheduler.scheduleNextForSchedule(appContext, schedule)
                    } catch (_: Exception) {
                    }
                }
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
