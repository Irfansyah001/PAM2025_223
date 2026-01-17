package com.umy.medremindid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.umy.medremindid.data.local.entity.AdherenceStatus
import com.umy.medremindid.data.local.model.AdherenceLogWithSchedule
import com.umy.medremindid.ui.adherence.AdherencePeriod
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceHistoryScreen(
    viewModel: AdherenceViewModel,
    onBack: () -> Unit
) {
    val period by viewModel.period.collectAsState()
    val items by viewModel.history.collectAsState()

    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Riwayat Kepatuhan") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { viewModel.setPeriod(AdherencePeriod.TODAY) },
                    label = { Text("Hari ini") }
                )
                AssistChip(
                    onClick = { viewModel.setPeriod(AdherencePeriod.LAST_7_DAYS) },
                    label = { Text("7 hari") }
                )
                AssistChip(
                    onClick = { viewModel.setPeriod(AdherencePeriod.LAST_30_DAYS) },
                    label = { Text("30 hari") }
                )
                AssistChip(
                    onClick = { viewModel.setPeriod(AdherencePeriod.ALL) },
                    label = { Text("Semua") }
                )
            }

            Text(
                text = "Filter: ${period.name.replace('_', ' ')} • Total log: ${items.size}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (items.isEmpty()) {
                Text(
                    text = "Belum ada riwayat kepatuhan pada periode ini.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items) { row ->
                        HistoryItem(row, fmt)
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text("Kembali")
            }
        }
    }
}

@Composable
private fun HistoryItem(
    row: AdherenceLogWithSchedule,
    fmt: DateTimeFormatter
) {
    val planned = row.log.plannedTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val taken = row.log.takenTime?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

    val statusText = when (row.log.status) {
        AdherenceStatus.TAKEN -> "TAKEN"
        AdherenceStatus.SKIPPED -> "SKIPPED"
        AdherenceStatus.MISSED -> "MISSED"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${row.medicineName} • ${row.dosage}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Planned: ${planned.format(fmt)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (taken != null) {
                Text(
                    text = "Taken: ${taken.format(fmt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Status: $statusText",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!row.log.note.isNullOrBlank()) {
                Text(text = "Catatan: ${row.log.note}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
