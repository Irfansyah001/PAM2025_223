package com.umy.medremindid.ui.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.session.SessionManager
import com.umy.medremindid.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class ScheduleFormState(
    val scheduleId: Long? = null,
    val medicineName: String = "",
    val dosage: String = "",
    val instructions: String = "",
    val timeOfDayText: String = "",
    val startDateText: String = "",
    val endDateText: String = "",
    val isActive: Boolean = true,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class MedicationScheduleViewModel(
    private val appContext: Context,
    private val session: SessionManager,
    private val repo: MedicationScheduleRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val searchQueryState: StateFlow<String> = searchQuery.asStateFlow()

    private val userIdFlow = session.userIdFlow.filterNotNull()

    val schedules: StateFlow<List<MedicationScheduleEntity>> =
        combine(userIdFlow, searchQuery) { userId, q -> userId to q.trim() }
            .flatMapLatest { (userId, q) ->
                if (q.isBlank()) repo.observeAllByUser(userId)
                else repo.observeSearch(userId, q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(
        ScheduleFormState(
            timeOfDayText = "08:00",
            startDateText = LocalDate.now().toString(),
            endDateText = ""
        )
    )
    val form: StateFlow<ScheduleFormState> = _form.asStateFlow()

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun startCreate() {
        _form.value = ScheduleFormState(
            scheduleId = null,
            medicineName = "",
            dosage = "",
            instructions = "",
            timeOfDayText = "08:00",
            startDateText = LocalDate.now().toString(),
            endDateText = "",
            isActive = true,
            errorMessage = null,
            isSaving = false
        )
    }

    fun loadForEdit(scheduleId: Long) {
        viewModelScope.launch {
            val userId = requireUserId()

            val entity = repo.getById(userId, scheduleId)
            if (entity == null) {
                _form.value = _form.value.copy(errorMessage = "Jadwal tidak ditemukan.")
                return@launch
            }

            _form.value = ScheduleFormState(
                scheduleId = entity.scheduleId,
                medicineName = entity.medicineName,
                dosage = entity.dosage,
                instructions = entity.instructions.orEmpty(),
                timeOfDayText = entity.timeOfDay.toString(),
                startDateText = entity.startDate.toString(),
                endDateText = entity.endDate?.toString().orEmpty(),
                isActive = entity.isActive,
                errorMessage = null,
                isSaving = false
            ).normalizeTimeText()
        }
    }

    fun updateForm(transform: (ScheduleFormState) -> ScheduleFormState) {
        _form.value = transform(_form.value).copy(errorMessage = null)
    }

    fun saveForm(onSaved: (scheduleId: Long) -> Unit) {
        viewModelScope.launch {
            val current = _form.value
            _form.value = current.copy(isSaving = true, errorMessage = null)

            val userId = requireUserId()

            val medicineName = current.medicineName.trim()
            val dosage = current.dosage.trim()
            val instructions = current.instructions.trim().ifBlank { null }

            if (medicineName.isBlank()) {
                failSave("Nama obat wajib diisi.")
                return@launch
            }
            if (dosage.isBlank()) {
                failSave("Dosis wajib diisi.")
                return@launch
            }

            val time = parseLocalTime(current.timeOfDayText)
            if (time == null) {
                failSave("Format jam salah. Gunakan HH:mm (contoh 08:00).")
                return@launch
            }

            val startDate = parseLocalDate(current.startDateText)
            if (startDate == null) {
                failSave("Format tanggal mulai salah. Gunakan yyyy-MM-dd (contoh 2026-01-17).")
                return@launch
            }

            val endDate = if (current.endDateText.trim().isBlank()) null else parseLocalDate(current.endDateText)
            if (current.endDateText.trim().isNotBlank() && endDate == null) {
                failSave("Format tanggal akhir salah. Gunakan yyyy-MM-dd.")
                return@launch
            }
            if (endDate != null && endDate.isBefore(startDate)) {
                failSave("Tanggal akhir tidak boleh lebih kecil dari tanggal mulai.")
                return@launch
            }

            if (current.isActive) {
                val conflictCount = repo.countActiveAtTime(
                    userId = userId,
                    timeOfDay = time,
                    excludeScheduleId = current.scheduleId
                )
                if (conflictCount > 0) {
                    failSave("Sudah ada jadwal aktif pada jam ${time.toString().take(5)}. Nonaktifkan salah satu atau ganti jam.")
                    return@launch
                }
            }

            val now = Instant.now()

            val entity = MedicationScheduleEntity(
                scheduleId = current.scheduleId ?: 0L,
                userId = userId,
                medicineName = medicineName,
                dosage = dosage,
                instructions = instructions,
                timeOfDay = time,
                startDate = startDate,
                endDate = endDate,
                isActive = current.isActive,
                createdAt = now,
                updatedAt = now
            )

            try {
                val id = repo.upsert(entity)
                val finalScheduleId = current.scheduleId ?: id
                val finalEntity = entity.copy(scheduleId = finalScheduleId)

                if (finalEntity.isActive) {
                    ReminderScheduler.scheduleNextForSchedule(appContext, finalEntity)
                } else {
                    ReminderScheduler.cancelForSchedule(appContext, userId, finalScheduleId)
                }

                _form.value = _form.value.copy(isSaving = false, errorMessage = null)
                onSaved(finalScheduleId)
            } catch (e: Exception) {
                failSave("Gagal menyimpan jadwal: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun deleteSchedule(scheduleId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = requireUserId()
            try {
                repo.deleteById(userId, scheduleId)
                ReminderScheduler.cancelForSchedule(appContext, userId, scheduleId)
                onDeleted()
            } catch (_: Exception) {
            }
        }
    }

    fun setActive(scheduleId: Long, active: Boolean) {
        viewModelScope.launch {
            val userId = requireUserId()

            if (active) {
                val entity = repo.getById(userId, scheduleId) ?: return@launch
                val conflictCount = repo.countActiveAtTime(
                    userId = userId,
                    timeOfDay = entity.timeOfDay,
                    excludeScheduleId = scheduleId
                )
                if (conflictCount > 0) {
                    _form.value = _form.value.copy(
                        errorMessage = "Tidak bisa diaktifkan: ada jadwal aktif lain di jam ${entity.timeOfDay.toString().take(5)}."
                    )
                    return@launch
                }
            }

            try {
                repo.setActive(userId, scheduleId, active)

                if (active) {
                    val updated = repo.getById(userId, scheduleId) ?: return@launch
                    ReminderScheduler.scheduleNextForSchedule(appContext, updated)
                } else {
                    ReminderScheduler.cancelForSchedule(appContext, userId, scheduleId)
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun requireUserId(): Long {
        return userIdFlow.first()
    }

    private fun failSave(msg: String) {
        _form.value = _form.value.copy(isSaving = false, errorMessage = msg)
    }

    private fun parseLocalTime(text: String): LocalTime? {
        val t = text.trim()
        return try {
            LocalTime.parse(t).let { time ->
                if (t.length == 5) time else LocalTime.of(time.hour, time.minute)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLocalDate(text: String): LocalDate? {
        return try {
            LocalDate.parse(text.trim())
        } catch (_: Exception) {
            null
        }
    }

    private fun ScheduleFormState.normalizeTimeText(): ScheduleFormState {
        val t = timeOfDayText.trim()
        return if (t.length >= 5) copy(timeOfDayText = t.take(5)) else this
    }
}
