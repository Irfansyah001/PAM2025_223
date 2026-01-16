package com.umy.medremindid.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import com.umy.medremindid.data.repository.MedicationScheduleRepository
import com.umy.medremindid.data.session.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class ScheduleListUiState(
    val query: String = "",
    val activeOnly: Boolean = false,
    val schedules: List<MedicationScheduleEntity> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null
)

data class ScheduleFormState(
    val scheduleId: Long = 0L,
    val medicineName: String = "",
    val dosage: String = "",
    val instructions: String = "",
    val timeOfDay: LocalTime? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val createdAt: Instant? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class MedicationScheduleViewModel(
    private val session: SessionManager,
    private val repo: MedicationScheduleRepository
) : ViewModel() {

    // ---- User session (wajib ada) ----
    // Pastikan SessionManager Anda punya userIdFlow: Flow<Long?>
    private val userIdFlow: Flow<Long> =
        session.userIdFlow.filterNotNull()

    // ---- List state ----
    private val _listState = MutableStateFlow(ScheduleListUiState())
    val listState: StateFlow<ScheduleListUiState> = _listState.asStateFlow()

    private val queryFlow = _listState.map { it.query }.distinctUntilChanged()
    private val activeOnlyFlow = _listState.map { it.activeOnly }.distinctUntilChanged()

    val schedulesFlow: StateFlow<List<MedicationScheduleEntity>> =
        combine(userIdFlow, queryFlow, activeOnlyFlow) { userId, query, activeOnly ->
            Triple(userId, query, activeOnly)
        }.flatMapLatest { (userId, query, activeOnly) ->
            val base = if (query.isBlank()) {
                repo.observeAllByUser(userId)
            } else {
                repo.observeSearch(userId, query)
            }
            base.map { list ->
                if (activeOnly) list.filter { it.isActive } else list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            schedulesFlow.collect { list ->
                _listState.update { it.copy(schedules = list) }
            }
        }
    }

    fun setQuery(q: String) {
        _listState.update { it.copy(query = q) }
    }

    fun setActiveOnly(activeOnly: Boolean) {
        _listState.update { it.copy(activeOnly = activeOnly) }
    }

    fun clearListMessage() {
        _listState.update { it.copy(message = null) }
    }

    fun toggleActive(scheduleId: Long, active: Boolean) {
        viewModelScope.launch {
            runCatching {
                val userId = userIdFlow.first()
                repo.setActive(userId, scheduleId, active)
            }.onFailure {
                _listState.update { s -> s.copy(message = it.message ?: "Gagal mengubah status jadwal") }
            }
        }
    }

    fun delete(scheduleId: Long) {
        viewModelScope.launch {
            runCatching {
                val userId = userIdFlow.first()
                repo.deleteById(userId, scheduleId)
            }.onFailure {
                _listState.update { s -> s.copy(message = it.message ?: "Gagal menghapus jadwal") }
            }
        }
    }

    // ---- Form state ----
    private val _formState = MutableStateFlow(ScheduleFormState())
    val formState: StateFlow<ScheduleFormState> = _formState.asStateFlow()

    fun startCreate() {
        _formState.value = ScheduleFormState(
            scheduleId = 0L,
            medicineName = "",
            dosage = "",
            instructions = "",
            timeOfDay = LocalTime.of(8, 0),
            startDate = LocalDate.now(),
            endDate = null,
            isActive = true,
            createdAt = null,
            loading = false,
            error = null
        )
    }

    fun startEdit(scheduleId: Long) {
        viewModelScope.launch {
            _formState.update { it.copy(loading = true, error = null) }
            runCatching {
                val userId = userIdFlow.first()
                repo.getById(userId, scheduleId)
            }.onSuccess { entity ->
                if (entity == null) {
                    _formState.update { it.copy(loading = false, error = "Data jadwal tidak ditemukan") }
                } else {
                    _formState.value = ScheduleFormState(
                        scheduleId = entity.scheduleId,
                        medicineName = entity.medicineName,
                        dosage = entity.dosage,
                        instructions = entity.instructions ?: "",
                        timeOfDay = entity.timeOfDay,
                        startDate = entity.startDate,
                        endDate = entity.endDate,
                        isActive = entity.isActive,
                        createdAt = entity.createdAt,
                        loading = false,
                        error = null
                    )
                }
            }.onFailure { e ->
                _formState.update { it.copy(loading = false, error = e.message ?: "Gagal memuat data jadwal") }
            }
        }
    }

    fun setMedicineName(v: String) = _formState.update { it.copy(medicineName = v, error = null) }
    fun setDosage(v: String) = _formState.update { it.copy(dosage = v, error = null) }
    fun setInstructions(v: String) = _formState.update { it.copy(instructions = v, error = null) }
    fun setTimeOfDay(v: LocalTime) = _formState.update { it.copy(timeOfDay = v, error = null) }
    fun setStartDate(v: LocalDate) = _formState.update { it.copy(startDate = v, error = null) }
    fun setEndDate(v: LocalDate?) = _formState.update { it.copy(endDate = v, error = null) }
    fun setIsActive(v: Boolean) = _formState.update { it.copy(isActive = v, error = null) }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val st = _formState.value

            val medicineName = st.medicineName.trim()
            val dosage = st.dosage.trim()
            val time = st.timeOfDay
            val start = st.startDate
            val end = st.endDate

            // Validasi minimal (SRS-safe)
            if (medicineName.isBlank()) {
                _formState.update { it.copy(error = "Nama obat wajib diisi") }
                return@launch
            }
            if (dosage.isBlank()) {
                _formState.update { it.copy(error = "Dosis wajib diisi") }
                return@launch
            }
            if (time == null) {
                _formState.update { it.copy(error = "Waktu minum wajib dipilih") }
                return@launch
            }
            if (start == null) {
                _formState.update { it.copy(error = "Tanggal mulai wajib dipilih") }
                return@launch
            }
            if (end != null && end.isBefore(start)) {
                _formState.update { it.copy(error = "End date tidak boleh sebelum start date") }
                return@launch
            }

            _formState.update { it.copy(loading = true, error = null) }

            runCatching {
                val userId = userIdFlow.first()
                val now = Instant.now()

                val entity = MedicationScheduleEntity(
                    scheduleId = st.scheduleId,
                    userId = userId,
                    medicineName = medicineName,
                    dosage = dosage,
                    instructions = st.instructions.trim().ifBlank { null },
                    timeOfDay = time,
                    startDate = start,
                    endDate = end,
                    isActive = st.isActive,
                    createdAt = st.createdAt ?: now,
                    updatedAt = now
                )

                repo.upsertForUser(userId, entity)
            }.onSuccess {
                _formState.update { it.copy(loading = false) }
                onSuccess()
            }.onFailure { e ->
                _formState.update { it.copy(loading = false, error = e.message ?: "Gagal menyimpan jadwal") }
            }
        }
    }
}
