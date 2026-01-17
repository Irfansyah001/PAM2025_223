package com.umy.medremindid.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.symptom.SymptomNoteViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomFormScreen(
    viewModel: SymptomNoteViewModel,
    noteId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val st by viewModel.form.collectAsState()
    val schedules by viewModel.scheduleOptions.collectAsState()

    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    LaunchedEffect(noteId) {
        if (noteId == null) viewModel.startCreate() else viewModel.loadForEdit(noteId)
    }

    fun parseDateOrNull(text: String): LocalDate? {
        return try {
            LocalDate.parse(text.trim())
        } catch (_: Exception) {
            null
        }
    }

    fun openDatePicker(current: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val init = current ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            init.year,
            init.monthValue - 1,
            init.dayOfMonth
        ).show()
    }

    val title = if (noteId == null) "Tambah Catatan" else "Edit Catatan"

    val datePreview = parseDateOrNull(st.noteDateText)?.format(dateFmt) ?: st.noteDateText.ifBlank { "-" }
    val scheduleLabel = st.scheduleId?.let { sid ->
        schedules.firstOrNull { it.scheduleId == sid }?.label
    } ?: "Tidak terkait jadwal"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            st.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            OutlinedTextField(
                value = st.title,
                onValueChange = { v -> viewModel.updateForm { it.copy(title = v) } },
                label = { Text("Judul Keluhan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = st.description,
                onValueChange = { v -> viewModel.updateForm { it.copy(description = v) } },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tanggal")
                    Text(datePreview)
                }
                Button(onClick = {
                    openDatePicker(parseDateOrNull(st.noteDateText)) { picked ->
                        viewModel.updateForm { it.copy(noteDateText = picked.toString()) }
                    }
                }) { Text("Pilih") }
            }

            OutlinedTextField(
                value = st.severityText,
                onValueChange = { v -> viewModel.updateForm { it.copy(severityText = v) } },
                label = { Text("Keparahan 1–5 (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Text("Terkait Jadwal (opsional)", style = MaterialTheme.typography.titleSmall)
            Text(scheduleLabel, style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateForm { it.copy(scheduleId = null) } }
                ) { Text("Clear") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val first = schedules.firstOrNull()
                        if (first != null) {
                            viewModel.updateForm { it.copy(scheduleId = first.scheduleId) }
                        }
                    }
                ) { Text("Pilih Pertama") }
            }

            if (schedules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    schedules.take(6).forEach { opt ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.updateForm { it.copy(scheduleId = opt.scheduleId) } }
                        ) {
                            Text(opt.label)
                        }
                    }
                    if (schedules.size > 6) {
                        Text("Catatan: Untuk daftar penuh, kamu bisa ubah jadi dropdown/scroll jika mau.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = { viewModel.saveForm(onSaved) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !st.isSaving
            ) {
                Text(if (st.isSaving) "Menyimpan..." else "Simpan")
            }
        }
    }
}
