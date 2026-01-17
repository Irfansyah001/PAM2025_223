package com.umy.medremindid.ui.adherence

import android.content.Context
import com.umy.medremindid.data.local.dao.AdherenceLogDao
import com.umy.medremindid.data.local.entity.AdherenceStatus
import com.umy.medremindid.data.repository.AdherenceLogRepository
import com.umy.medremindid.data.session.SessionManager
import com.umy.medremindid.ui.report.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PeriodFilter { TODAY, DAYS_7, DAYS_30, ALL }

data class AdherenceSummary(
    val total: Int = 0,
    val taken: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0,
    val adherenceRate: Int = 0,
    val label: String = "TODAY"
)

data class DateRange(
    val from: Instant,
    val to: Instant,
    val label: String
)

class AdherenceViewModel(
    private val appContext: Context,
    private val session: SessionManager,
    private val repo: AdherenceLogRepository
) : ViewModel() {

    private val userIdFlow = session.userIdFlow.filterNotNull()

    private val _filter = MutableStateFlow(PeriodFilter.TODAY)
    val filter: StateFlow<PeriodFilter> = _filter.asStateFlow()

    val range: StateFlow<DateRange> =
        _filter.map { toRange(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toRange(PeriodFilter.TODAY))

    val logs: StateFlow<List<AdherenceLogDao.AdherenceLogRow>> =
        combine(userIdFlow, range) { uid, r -> uid to r }
            .flatMapLatest { (uid, r) -> repo.observeRowsInRange(uid, r.from, r.to) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<AdherenceSummary> =
        combine(filter, logs) { f, list ->
            val total = list.size
            val taken = list.count { it.status == AdherenceStatus.TAKEN }
            val skipped = list.count { it.status == AdherenceStatus.SKIPPED }
            val missed = list.count { it.status == AdherenceStatus.MISSED }
            val rate = if (total == 0) 0 else ((taken.toDouble() / total.toDouble()) * 100.0).toInt()
            AdherenceSummary(
                total = total,
                taken = taken,
                skipped = skipped,
                missed = missed,
                adherenceRate = rate,
                label = when (f) {
                    PeriodFilter.TODAY -> "TODAY"
                    PeriodFilter.DAYS_7 -> "7 DAYS"
                    PeriodFilter.DAYS_30 -> "30 DAYS"
                    PeriodFilter.ALL -> "ALL"
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdherenceSummary())

    fun setFilter(value: PeriodFilter) {
        _filter.value = value
    }

    fun buildShareText(): String {
        val s = summary.value
        return buildString {
            appendLine("Laporan Kepatuhan MedRemindID")
            appendLine("Periode: ${s.label}")
            appendLine("Total log: ${s.total}")
            appendLine("Taken: ${s.taken}")
            appendLine("Skipped: ${s.skipped}")
            appendLine("Missed: ${s.missed}")
            appendLine("Adherence Rate: ${s.adherenceRate}%")
        }
    }

    fun exportCsv(onReady: (uri: android.net.Uri) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val uid = userIdFlow.first()
                val r = range.value
                val rows = repo.exportRowsInRange(uid, r.from, r.to)
                val uri = ReportExporter.exportCsv(appContext, rows)
                onReady(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Export gagal")
            }
        }
    }

    private fun toRange(filter: PeriodFilter): DateRange {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = when (filter) {
            PeriodFilter.TODAY -> today
            PeriodFilter.DAYS_7 -> today.minusDays(6)
            PeriodFilter.DAYS_30 -> today.minusDays(29)
            PeriodFilter.ALL -> LocalDate.of(1970, 1, 1)
        }
        val from = start.atStartOfDay(zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant()
        val label = when (filter) {
            PeriodFilter.TODAY -> "TODAY"
            PeriodFilter.DAYS_7 -> "7 DAYS"
            PeriodFilter.DAYS_30 -> "30 DAYS"
            PeriodFilter.ALL -> "ALL"
        }
        return DateRange(from = from, to = to, label = label)
    }
}
