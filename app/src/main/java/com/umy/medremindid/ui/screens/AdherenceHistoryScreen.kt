package com.umy.medremindid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import com.umy.medremindid.ui.adherence.PeriodFilter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceHistoryScreen(
    viewModel: AdherenceViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val filter by viewModel.filter.collectAsState()

    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val zone = ZoneId.systemDefault()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Kepatuhan") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) { Text("Kembali") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = filter == PeriodFilter.TODAY,
                    onClick = { viewModel.setFilter(PeriodFilter.TODAY) },
                    label = { Text("Hari ini") }
                )
                FilterChip(
                    selected = filter == PeriodFilter.DAYS_7,
                    onClick = { viewModel.setFilter(PeriodFilter.DAYS_7) },
                    label = { Text("7 hari") }
                )
                FilterChip(
                    selected = filter == PeriodFilter.DAYS_30,
                    onClick = { viewModel.setFilter(PeriodFilter.DAYS_30) },
                    label = { Text("30 hari") }
                )
                FilterChip(
                    selected = filter == PeriodFilter.ALL,
                    onClick = { viewModel.setFilter(PeriodFilter.ALL) },
                    label = { Text("Semua") }
                )
            }

            Text("Total log: ${logs.size}", style = MaterialTheme.typography.bodySmall)

            if (logs.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Belum ada riwayat.", style = MaterialTheme.typography.titleMedium)
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs, key = { it.logId }) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val title = listOfNotNull(r.medicineName, r.dosage).joinToString(" • ").ifBlank { "Schedule ${r.scheduleId}" }
                            Text(title, style = MaterialTheme.typography.titleMedium)

                            val planned = r.plannedTime.atZone(zone).toLocalDateTime().format(fmt)
                            Text("Planned: $planned", style = MaterialTheme.typography.bodySmall)

                            val taken = r.takenTime?.atZone(zone)?.toLocalDateTime()?.format(fmt)
                            if (taken != null) Text("Taken: $taken", style = MaterialTheme.typography.bodySmall)

                            Text("Status: ${r.status}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
