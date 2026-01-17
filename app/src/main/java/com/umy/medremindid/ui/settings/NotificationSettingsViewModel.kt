package com.umy.medremindid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.local.entity.NotificationPreferenceEntity
import com.umy.medremindid.data.repository.NotificationPreferenceRepository
import com.umy.medremindid.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

data class NotificationSettingsState(
    val notificationsEnabled: Boolean = true,
    val quietEnabled: Boolean = false,
    val quietStartText: String = "",
    val quietEndText: String = "",
    val allowVibration: Boolean = true,
    val ringtoneUri: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class NotificationSettingsViewModel(
    private val session: SessionManager,
    private val repo: NotificationPreferenceRepository
) : ViewModel() {

    private val userIdFlow = session.userIdFlow.filterNotNull()

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val uid = userIdFlow.first()
            val pref = repo.getOrDefault(uid)
            _state.value = fromEntity(pref)
        }
    }

    fun update(transform: (NotificationSettingsState) -> NotificationSettingsState) {
        _state.value = transform(_state.value).copy(errorMessage = null)
    }

    fun save(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val uid = userIdFlow.first()
            val current = _state.value
            _state.value = current.copy(isSaving = true, errorMessage = null)

            val quietEnabled = current.quietEnabled
            val start = if (quietEnabled) parseTime(current.quietStartText) else null
            val end = if (quietEnabled) parseTime(current.quietEndText) else null

            if (quietEnabled && (start == null || end == null)) {
                fail("Jam tenang aktif: waktu mulai dan selesai wajib diisi (HH:mm).")
                return@launch
            }

            val pref = repo.getOrDefault(uid).copy(
                notificationsEnabled = current.notificationsEnabled,
                quietHoursStart = start,
                quietHoursEnd = end,
                allowVibration = current.allowVibration,
                ringtoneUri = current.ringtoneUri.trim().ifBlank { null }
            )

            try {
                repo.save(uid, pref)
                _state.value = _state.value.copy(isSaving = false, errorMessage = null)
                onSaved()
            } catch (e: Exception) {
                fail("Gagal menyimpan pengaturan: ${e.message ?: "unknown error"}")
            }
        }
    }

    private fun fromEntity(pref: NotificationPreferenceEntity): NotificationSettingsState {
        val quietEnabled = pref.quietHoursStart != null && pref.quietHoursEnd != null
        return NotificationSettingsState(
            notificationsEnabled = pref.notificationsEnabled,
            quietEnabled = quietEnabled,
            quietStartText = pref.quietHoursStart?.toString()?.take(5).orEmpty(),
            quietEndText = pref.quietHoursEnd?.toString()?.take(5).orEmpty(),
            allowVibration = pref.allowVibration,
            ringtoneUri = pref.ringtoneUri.orEmpty(),
            errorMessage = null,
            isSaving = false
        )
    }

    private fun parseTime(text: String): LocalTime? {
        return try {
            val t = text.trim()
            if (t.isBlank()) null else LocalTime.parse(t).let { LocalTime.of(it.hour, it.minute) }
        } catch (_: Exception) {
            null
        }
    }

    private fun fail(msg: String) {
        _state.value = _state.value.copy(isSaving = false, errorMessage = msg)
    }
}
