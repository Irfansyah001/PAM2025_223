package com.umy.medremindid.ui.nav

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    const val SCHEDULE_LIST = "schedule_list"
    const val SCHEDULE_FORM = "schedule_form"
    const val SCHEDULE_FORM_ROUTE = "schedule_form?sid={sid}"

    fun scheduleFormRoute(scheduleId: Long? = null): String {
        return if (scheduleId == null) SCHEDULE_FORM
        else "schedule_form?sid=$scheduleId"
    }
}
