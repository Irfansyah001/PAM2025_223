package com.umy.medremindid.reminder

object ReminderConstants {
    const val CHANNEL_ID_REMINDERS = "medremind_reminders"
    const val CHANNEL_NAME_REMINDERS = "Medication Reminders"

    const val ACTION_REMIND = "com.umy.medremindid.ACTION_REMIND"
    const val ACTION_TAKEN = "com.umy.medremindid.ACTION_TAKEN"
    const val ACTION_SKIPPED = "com.umy.medremindid.ACTION_SKIPPED"
    const val ACTION_SNOOZE = "com.umy.medremindid.ACTION_SNOOZE"

    const val EXTRA_USER_ID = "extra_user_id"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_PLANNED_TIME_MILLIS = "extra_planned_time_millis"

    const val DEFAULT_SNOOZE_MINUTES = 10L
    const val DEFAULT_GRACE_MINUTES = 30L

    fun missedWorkName(userId: Long, scheduleId: Long, plannedMillis: Long): String =
        "missed_${userId}_${scheduleId}_${plannedMillis}"

    fun missedWorkTag(userId: Long, scheduleId: Long): String =
        "missed_tag_${userId}_${scheduleId}"
}
