package com.umy.medremindid.ui.adherence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.local.entity.AdherenceStatus
import com.umy.medremindid.data.local.model.AdherenceLogWithSchedule
import com.umy.medremindid.data.repository.AdherenceLogRepository
import com.umy.medremindid.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AdherencePeriod {
    TODAY, LAST_7_DAYS, LAST_30_DAYS, ALL
}

data class AdherenceSummaryState(
    val period: AdherencePeriod = AdherencePeriod.TODAY,
    val taken: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0,
    val total: Int = 0,
    val adherenceRate: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AdherenceViewModel(
    private val session: SessionManager,
    private val repo: AdherenceLogRepository
) : ViewModel() {

    private val userIdFlow = session.userIdFlow.filterNotNull()

    private val _period = MutableStateFlow(AdherencePeriod.TODAY)
    val period: StateFlow<AdherencePeriod> = _period.asStateFlow()

    val history: StateFlow<List<AdherenceLogWithSchedule>> =
        userIdFlow.flatMapLatest { userId ->
            _period.flatMapLatest { p ->
                val (from, to) = computeRange(p)
                if (p == AdherencePeriod.ALL) {
                    repo.observeByUserWithSchedule(userId)
                } else {
                    repo.observeByPlannedRangeWithSchedule(userId, from, to)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _summary = MutableStateFlow(AdherenceSummaryState())
    val summary: StateFlow<AdherenceSummaryState> = _summary.asStateFlow()

    init {
        refreshSummary()
    }

    fun setPeriod(p: AdherencePeriod) {
        _period.value = p
        refreshSummary()
    }

    fun refreshSummary() {
        viewModelScope.launch {
            _summary.value = _summary.value.copy(
                period = _period.value,
                isLoading = true,
                errorMessage = null
            )

            try {
                val userId = userIdFlow.stateIn(viewModelScope).value ?: return@launch
                val (from, to) = computeRange(_period.value)

                val taken = repo.countByStatusInRange(userId, AdherenceStatus.TAKEN, from, to)
                val skipped = repo.countByStatusInRange(userId, AdherenceStatus.SKIPPED, from, to)
                val missed = repo.countByStatusInRange(userId, AdherenceStatus.MISSED, from, to)

                val total = taken + skipped + missed
                val rate = if (total <= 0) 0.0 else (taken.toDouble() / total.toDouble())

                _summary.value = AdherenceSummaryState(
                    period = _period.value,
                    taken = taken,
                    skipped = skipped,
                    missed = missed,
                    total = total,
                    adherenceRate = rate,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _summary.value = _summary.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Gagal memuat ringkasan."
                )
            }
        }
    }

    private fun computeRange(p: AdherencePeriod): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        val startDate = when (p) {
            AdherencePeriod.TODAY -> today
            AdherencePeriod.LAST_7_DAYS -> today.minusDays(6)
            AdherencePeriod.LAST_30_DAYS -> today.minusDays(29)
            AdherencePeriod.ALL -> LocalDate.of(1970, 1, 1)
        }

        val from = startDate.atStartOfDay(zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant()
        return from to to
    }
}
