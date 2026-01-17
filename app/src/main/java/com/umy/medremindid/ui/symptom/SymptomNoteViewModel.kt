package com.umy.medremindid.ui.symptom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.local.dao.SymptomNoteDao
import com.umy.medremindid.data.local.entity.SymptomNoteEntity
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.repository.SymptomNoteRepository
import com.umy.medremindid.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SymptomFormState(
    val noteId: Long? = null,
    val title: String = "",
    val description: String = "",
    val noteDateText: String = LocalDate.now().toString(),
    val severityText: String = "",
    val scheduleId: Long? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

data class ScheduleOption(
    val scheduleId: Long,
    val label: String
)

class SymptomNoteViewModel(
    private val session: SessionManager,
    private val symptomRepo: SymptomNoteRepository,
    private val scheduleRepo: MedicationScheduleRepository
) : ViewModel() {

    private val userIdFlow = session.userIdFlow.filterNotNull()

    val notes: StateFlow<List<SymptomNoteDao.SymptomNoteItem>> =
        userIdFlow.flatMapLatest { uid -> symptomRepo.observeByUser(uid) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scheduleOptions: StateFlow<List<ScheduleOption>> =
        userIdFlow.flatMapLatest { uid -> scheduleRepo.observeAllByUser(uid) }
            .map { list ->
                list.map {
                    val time = it.timeOfDay.toString().take(5)
                    ScheduleOption(
                        scheduleId = it.scheduleId,
                        label = "${it.medicineName} • ${it.dosage} • $time"
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(SymptomFormState())
    val form: StateFlow<SymptomFormState> = _form.asStateFlow()

    fun startCreate() {
        _form.value = SymptomFormState(
            noteId = null,
            title = "",
            description = "",
            noteDateText = LocalDate.now().toString(),
            severityText = "",
            scheduleId = null,
            errorMessage = null,
            isSaving = false
        )
    }

    fun loadForEdit(noteId: Long) {
        viewModelScope.launch {
            val uid = requireUserId()
            val entity = symptomRepo.getById(uid, noteId)
            if (entity == null) {
                _form.value = _form.value.copy(errorMessage = "Catatan tidak ditemukan.")
                return@launch
            }
            _form.value = SymptomFormState(
                noteId = entity.noteId,
                title = entity.title,
                description = entity.description,
                noteDateText = entity.noteDate.toString(),
                severityText = entity.severity?.toString().orEmpty(),
                scheduleId = entity.scheduleId,
                errorMessage = null,
                isSaving = false
            )
        }
    }

    fun updateForm(transform: (SymptomFormState) -> SymptomFormState) {
        _form.value = transform(_form.value).copy(errorMessage = null)
    }

    fun saveForm(onSaved: () -> Unit) {
        viewModelScope.launch {
            val current = _form.value
            _form.value = current.copy(isSaving = true, errorMessage = null)

            val uid = requireUserId()

            val title = current.title.trim()
            val desc = current.description.trim()
            if (title.isBlank()) {
                fail("Judul keluhan wajib diisi.")
                return@launch
            }
            if (desc.isBlank()) {
                fail("Deskripsi keluhan wajib diisi.")
                return@launch
            }

            val date = parseDate(current.noteDateText)
            if (date == null) {
                fail("Format tanggal salah. Gunakan yyyy-MM-dd.")
                return@launch
            }

            val severity = current.severityText.trim().ifBlank { null }?.toIntOrNull()
            if (severity != null && (severity < 1 || severity > 5)) {
                fail("Keparahan (opsional) harus 1–5.")
                return@launch
            }

            val entity = SymptomNoteEntity(
                noteId = current.noteId ?: 0L,
                userId = uid,
                scheduleId = current.scheduleId,
                title = title,
                description = desc,
                noteDate = date,
                severity = severity
            )

            try {
                symptomRepo.upsertForUser(uid, entity)
                _form.value = _form.value.copy(isSaving = false, errorMessage = null)
                onSaved()
            } catch (e: Exception) {
                fail("Gagal menyimpan catatan: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun deleteNote(noteId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val uid = requireUserId()
            try {
                symptomRepo.deleteById(uid, noteId)
                onDeleted()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun requireUserId(): Long = userIdFlow.first()

    private fun parseDate(text: String): LocalDate? {
        return try {
            LocalDate.parse(text.trim())
        } catch (_: Exception) {
            null
        }
    }

    private fun fail(msg: String) {
        _form.value = _form.value.copy(isSaving = false, errorMessage = msg)
    }
}
