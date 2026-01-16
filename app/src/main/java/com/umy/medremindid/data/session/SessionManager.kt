package com.umy.medremindid.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }

    val isLoggedInFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_IS_LOGGED_IN] ?: false }

    val currentUserIdFlow: Flow<Long?> =
        context.dataStore.data.map { prefs ->
            val loggedIn = prefs[KEY_IS_LOGGED_IN] ?: false
            if (!loggedIn) null else prefs[KEY_CURRENT_USER_ID]
        }

    val userIdFlow: Flow<Long?> = currentUserIdFlow

    suspend fun setLoggedIn(userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_CURRENT_USER_ID] = userId
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs.remove(KEY_CURRENT_USER_ID)
        }
    }
}
