package com.umy.medremindid.ui.nav

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    const val SCHEDULE_LIST = "schedule_list"
    const val SCHEDULE_FORM_ROUTE = "schedule_form?sid={sid}"
    fun scheduleFormRoute(scheduleId: Long? = null): String =
        "schedule_form?sid=${scheduleId ?: -1L}"

    const val ADHERENCE_HISTORY = "adherence_history"
    const val ADHERENCE_SUMMARY = "adherence_summary"
    const val REPORT = "report"

    const val SYMPTOM_LIST = "symptom_list"
    const val SYMPTOM_FORM_ROUTE = "symptom_form?nid={nid}"
    fun symptomFormRoute(noteId: Long? = null): String =
        "symptom_form?nid=${noteId ?: -1L}"

    const val NOTIF_SETTINGS = "notif_settings"
}
