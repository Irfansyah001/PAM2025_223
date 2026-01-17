package com.umy.medremindid.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.adherence.AdherenceViewModel
import com.umy.medremindid.ui.adherence.PeriodFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: AdherenceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val filter by viewModel.filter.collectAsState()
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan") },
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ringkasan Laporan", style = MaterialTheme.typography.titleMedium)
                    Text("Periode: ${summary.label}")
                    Text("Total log: ${summary.total}")
                    Text("Taken: ${summary.taken}")
                    Text("Skipped: ${summary.skipped}")
                    Text("Missed: ${summary.missed}")
                    Text("Adherence Rate: ${summary.adherenceRate}%")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val text = viewModel.buildShareText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
                    }
                ) { Text("Share") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.exportCsv(
                            onReady = { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export CSV"))
                            },
                            onError = { }
                        )
                    }
                ) { Text("Export CSV") }
            }
        }
    }
}
