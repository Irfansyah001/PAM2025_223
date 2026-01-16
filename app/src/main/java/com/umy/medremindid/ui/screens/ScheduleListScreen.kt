package com.umy.medremindid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umy.medremindid.data.local.entity.MedicationScheduleEntity
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: MedicationScheduleViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val state by viewModel.listState.collectAsState()
    val schedules by viewModel.schedulesFlow.collectAsState()

    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    val timeFmt = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Hapus Jadwal") },
            text = { Text("Apakah Anda yakin ingin menghapus jadwal ini?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(confirmDeleteId!!)
                    confirmDeleteId = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Batal") }
            }
        )
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearListMessage()
        }
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
                    TextButton(onClick = onAdd) { Text("Tambah") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Cari (nama obat / dosis)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = state.activeOnly,
                    onCheckedChange = viewModel::setActiveOnly
                )
                Spacer(Modifier.width(8.dp))
                Text("Tampilkan yang aktif saja")
            }

            Spacer(Modifier.height(12.dp))

            if (state.message != null) {
                Text(
                    text = state.message ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            if (schedules.isEmpty()) {
                Text("Belum ada jadwal. Tekan 'Tambah' untuk membuat jadwal.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(schedules) { item ->
                        ScheduleItemCard(
                            item = item,
                            timeFmt = timeFmt,
                            dateFmt = dateFmt,
                            onEdit = { onEdit(item.scheduleId) },
                            onToggleActive = { active -> viewModel.toggleActive(item.scheduleId, active) },
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
