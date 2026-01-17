package com.umy.medremindid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoSchedules: () -> Unit,
    onGoAdherenceHistory: () -> Unit,
    onGoAdherenceSummary: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("MedRemindID") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = onGoSchedules, modifier = Modifier.fillMaxWidth()) {
                Text("Jadwal Obat")
            }

            OutlinedButton(onClick = onGoAdherenceHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Riwayat Kepatuhan")
            }

            OutlinedButton(onClick = onGoAdherenceSummary, modifier = Modifier.fillMaxWidth()) {
                Text("Ringkasan Kepatuhan")
            }

            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Logout")
            }
        }
    }
}
