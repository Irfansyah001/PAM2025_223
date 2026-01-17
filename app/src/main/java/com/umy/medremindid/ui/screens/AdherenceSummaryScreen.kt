package com.umy.medremindid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import com.umy.medremindid.ui.adherence.AdherencePeriod
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceSummaryScreen(
    viewModel: AdherenceViewModel,
    onBack: () -> Unit
) {
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ringkasan Kepatuhan") }) }
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
                AssistChip(onClick = { viewModel.setPeriod(AdherencePeriod.TODAY) }, label = { Text("Hari ini") })
                AssistChip(onClick = { viewModel.setPeriod(AdherencePeriod.LAST_7_DAYS) }, label = { Text("7 hari") })
                AssistChip(onClick = { viewModel.setPeriod(AdherencePeriod.LAST_30_DAYS) }, label = { Text("30 hari") })
                AssistChip(onClick = { viewModel.setPeriod(AdherencePeriod.ALL) }, label = { Text("Semua") })
            }

            if (summary.isLoading) {
                Text("Memuat ringkasan...", style = MaterialTheme.typography.bodyMedium)
            } else if (summary.errorMessage != null) {
                Text("Error: ${summary.errorMessage}", color = MaterialTheme.colorScheme.error)
            } else {
                val percent = (summary.adherenceRate * 100.0).roundToInt()

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Periode: ${summary.period.name.replace('_', ' ')}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Total: ${summary.total}", style = MaterialTheme.typography.bodyMedium)
                        Text("Taken: ${summary.taken}", style = MaterialTheme.typography.bodyMedium)
                        Text("Skipped: ${summary.skipped}", style = MaterialTheme.typography.bodyMedium)
                        Text("Missed: ${summary.missed}", style = MaterialTheme.typography.bodyMedium)

                        Text(
                            text = "Adherence Rate: $percent%",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { summary.adherenceRate.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
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
