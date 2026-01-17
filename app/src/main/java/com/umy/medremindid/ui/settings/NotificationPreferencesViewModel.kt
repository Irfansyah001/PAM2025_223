package com.umy.medremindid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.repository.NotificationPreferenceRepository
import com.umy.medremindid.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

data class NotificationPreferencesState(
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val allowVibration: Boolean = true,
    val quietStartText: String = "",
    val quietEndText: String = "",
    val ringtoneUri: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class NotificationPreferencesViewModel(
    private val session: SessionManager,
    private val repo: NotificationPreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationPreferencesState())
    val state: StateFlow<NotificationPreferencesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, infoMessage = null)

            val userId = requireUserId()
            val pref = repo.getOrDefault(userId)

            _state.value = NotificationPreferencesState(
                isLoading = false,
                notificationsEnabled = pref.notificationsEnabled,
                allowVibration = pref.allowVibration,
                quietStartText = pref.quietHoursStart?.toString()?.take(5).orEmpty(),
                quietEndText = pref.quietHoursEnd?.toString()?.take(5).orEmpty(),
                ringtoneUri = pref.ringtoneUri,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val userId = requireUserId()
            repo.setNotificationsEnabled(userId, enabled)
            _state.value = _state.value.copy(
                notificationsEnabled = enabled,
                errorMessage = null,
                infoMessage = "Preferensi notifikasi diperbarui."
            )
        }
    }

    fun setAllowVibration(allow: Boolean) {
        viewModelScope.launch {
            val userId = requireUserId()
            repo.setVibration(userId, allow)
            _state.value = _state.value.copy(
                allowVibration = allow,
                errorMessage = null,
                infoMessage = "Preferensi getar diperbarui."
            )
        }
    }

    fun updateQuietStartText(value: String) {
        _state.value = _state.value.copy(quietStartText = value, errorMessage = null, infoMessage = null)
    }

    fun updateQuietEndText(value: String) {
        _state.value = _state.value.copy(quietEndText = value, errorMessage = null, infoMessage = null)
    }

    fun saveQuietHours() {
        viewModelScope.launch {
            val userId = requireUserId()

            val startRaw = _state.value.quietStartText.trim()
            val endRaw = _state.value.quietEndText.trim()

            if (startRaw.isBlank() && endRaw.isBlank()) {
                repo.setQuietHours(userId, null, null)
                _state.value = _state.value.copy(
                    quietStartText = "",
                    quietEndText = "",
                    errorMessage = null,
                    infoMessage = "Jam tenang dinonaktifkan."
                )
                return@launch
            }

            if (startRaw.isBlank() || endRaw.isBlank()) {
                _state.value = _state.value.copy(
                    errorMessage = "Jam tenang harus diisi lengkap (start dan end), atau kosongkan keduanya untuk menonaktifkan."
                )
                return@launch
            }

            val start = parseLocalTime(startRaw)
            val end = parseLocalTime(endRaw)
            if (start == null || end == null) {
                _state.value = _state.value.copy(errorMessage = "Format jam salah. Gunakan HH:mm (contoh 22:00).")
                return@launch
            }

            repo.setQuietHours(userId, start, end)
            _state.value = _state.value.copy(
                quietStartText = start.toString().take(5),
                quietEndText = end.toString().take(5),
                errorMessage = null,
                infoMessage = "Jam tenang disimpan."
            )
        }
    }

    fun setRingtoneUri(uriString: String?) {
        viewModelScope.launch {
            val userId = requireUserId()
            repo.setRingtoneUri(userId, uriString)
            _state.value = _state.value.copy(
                ringtoneUri = uriString,
                errorMessage = null,
                infoMessage = "Nada notifikasi diperbarui."
            )
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }

    private suspend fun requireUserId(): Long {
        return session.userIdFlow.filterNotNull().first()
    }

    private fun parseLocalTime(text: String): LocalTime? {
        return try {
            val t = text.trim()
            val parsed = LocalTime.parse(t)
            LocalTime.of(parsed.hour, parsed.minute)
        } catch (_: Exception) {
            null
        }
    }
}
