package com.umy.medremindid.ui.nav

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    const val SCHEDULE_LIST = "schedule_list"
    const val SCHEDULE_FORM_ROUTE = "schedule_form?sid={sid}"
    const val ADHERENCE_HISTORY = "adherence_history"
    const val ADHERENCE_SUMMARY = "adherence_summary"
    const val NOTIF_PREFS = "notif_prefs"

    fun scheduleFormRoute(scheduleId: Long? = null): String {
        return "schedule_form?sid=${scheduleId ?: -1L}"
    }
}
