package com.umy.medremindid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: MedicationScheduleViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val query by viewModel.searchQueryState.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    var activeOnly by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    val timeFmt = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val shown = remember(schedules, activeOnly) {
        if (!activeOnly) schedules else schedules.filter { it.isActive }
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Hapus Jadwal") },
            text = { Text("Apakah Anda yakin ingin menghapus jadwal ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = confirmDeleteId ?: return@TextButton
                        viewModel.deleteSchedule(id)
                        confirmDeleteId = null
                    }
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Obat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = onAdd) { Text("Tambah") }
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
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Cari (nama obat / dosis)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = activeOnly,
                    onCheckedChange = { activeOnly = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Tampilkan yang aktif saja")
            }

            if (shown.isEmpty()) {
                Text("Belum ada jadwal. Tekan 'Tambah' untuk membuat jadwal.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown) { item ->
                        ScheduleItemCard(
                            item = item,
                            timeFmt = timeFmt,
                            dateFmt = dateFmt,
                            onEdit = { onEdit(item.scheduleId) },
                            onToggleActive = { active -> viewModel.setActive(item.scheduleId, active) },
                            onDelete = { confirmDeleteId = item.scheduleId }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleItemCard(
    item: MedicationScheduleEntity,
    timeFmt: DateTimeFormatter,
    dateFmt: DateTimeFormatter,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.medicineName, style = MaterialTheme.typography.titleMedium)
                    Text("Dosis: ${item.dosage}", style = MaterialTheme.typography.bodyMedium)
                    Text("Waktu: ${item.timeOfDay.format(timeFmt)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Mulai: ${item.startDate.format(dateFmt)}", style = MaterialTheme.typography.bodySmall)
                    item.endDate?.let {
                        Text("Sampai: ${it.format(dateFmt)}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Switch(
                    checked = item.isActive,
                    onCheckedChange = onToggleActive
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) { Text("Hapus") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}
