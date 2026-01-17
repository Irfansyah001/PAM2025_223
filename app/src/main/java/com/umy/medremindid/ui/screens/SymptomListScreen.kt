package com.umy.medremindid.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.symptom.SymptomNoteViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomListScreen(
    viewModel: SymptomNoteViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (noteId: Long) -> Unit
) {
    val items by viewModel.notes.collectAsState()
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Hapus Catatan") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan ini?") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    val id = confirmDeleteId ?: return@Button
                    confirmDeleteId = null
                    viewModel.deleteNote(id) { }
                }) { Text("Hapus") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { confirmDeleteId = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catatan Keluhan/Gejala") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada catatan.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Button(onClick = onAdd) { Text("Tambah Catatan") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.noteId }) { itx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(itx.noteId) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(itx.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    itx.noteDate.format(dateFmt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                IconButton(onClick = { onEdit(itx.noteId) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { confirmDeleteId = itx.noteId }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(itx.description, style = MaterialTheme.typography.bodyMedium)

                        val rel = itx.medicineName?.let { mn ->
                            val time = itx.timeOfDay?.toString()?.take(5)
                            val ds = itx.dosage ?: ""
                            if (time != null) "Terkait: $mn • $ds • $time" else "Terkait: $mn • $ds"
                        }
                        val sev = itx.severity?.let { "Keparahan: $it/5" }

                        if (rel != null || sev != null) {
                            Spacer(Modifier.height(8.dp))
                            if (rel != null) Text(rel, style = MaterialTheme.typography.bodySmall)
                            if (sev != null) Text(sev, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
