package com.umy.medremindid.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
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
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormScreen(
    viewModel: MedicationScheduleViewModel,
    scheduleId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val st by viewModel.form.collectAsState()

    val timeFmt = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    LaunchedEffect(scheduleId) {
        if (scheduleId == null) viewModel.startCreate()
        else viewModel.loadForEdit(scheduleId)
    }

    fun parseTimeOrNull(text: String): LocalTime? {
        return try {
            LocalTime.parse(text.trim())
        } catch (_: Exception) {
            null
        }
    }

    fun parseDateOrNull(text: String): LocalDate? {
        return try {
            LocalDate.parse(text.trim())
        } catch (_: Exception) {
            null
        }
    }

    fun openTimePicker() {
        val init = parseTimeOrNull(st.timeOfDayText) ?: LocalTime.of(8, 0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val t = LocalTime.of(hour, minute)
                viewModel.updateForm { it.copy(timeOfDayText = t.toString().take(5)) }
            },
            init.hour,
            init.minute,
            true
        ).show()
    }

    fun openDatePicker(
        current: LocalDate?,
        onPicked: (LocalDate) -> Unit
    ) {
        val init = current ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                onPicked(LocalDate.of(y, m + 1, d))
            },
            init.year,
            init.monthValue - 1,
            init.dayOfMonth
        ).show()
    }

    val title = if (scheduleId == null) "Tambah Jadwal" else "Edit Jadwal"

    val timePreview = parseTimeOrNull(st.timeOfDayText)?.format(timeFmt) ?: st.timeOfDayText.ifBlank { "-" }
    val startPreview = parseDateOrNull(st.startDateText)?.format(dateFmt) ?: st.startDateText.ifBlank { "-" }
    val endPreview = if (st.endDateText.isBlank()) "-" else (parseDateOrNull(st.endDateText)?.format(dateFmt) ?: st.endDateText)

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
            st.errorMessage?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = st.medicineName,
                onValueChange = { v -> viewModel.updateForm { it.copy(medicineName = v) } },
                label = { Text("Nama Obat") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = st.dosage,
                onValueChange = { v -> viewModel.updateForm { it.copy(dosage = v) } },
                label = { Text("Dosis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = st.instructions,
                onValueChange = { v -> viewModel.updateForm { it.copy(instructions = v) } },
                label = { Text("Instruksi (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Waktu Minum")
                    Text(timePreview)
                }
                Button(onClick = { openTimePicker() }) { Text("Pilih Waktu") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Start Date")
                    Text(startPreview)
                }
                Button(onClick = {
                    openDatePicker(parseDateOrNull(st.startDateText)) { picked ->
                        viewModel.updateForm { it.copy(startDateText = picked.toString()) }
                    }
                }) { Text("Pilih") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("End Date (opsional)")
                    Text(endPreview)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        openDatePicker(parseDateOrNull(st.endDateText)) { picked ->
                            viewModel.updateForm { it.copy(endDateText = picked.toString()) }
                        }
                    }) { Text("Pilih") }

                    OutlinedButton(onClick = {
                        viewModel.updateForm { it.copy(endDateText = "") }
                    }) { Text("Clear") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aktif")
                Switch(
                    checked = st.isActive,
                    onCheckedChange = { v -> viewModel.updateForm { it.copy(isActive = v) } }
                )
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    viewModel.saveForm { _ -> onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !st.isSaving
            ) {
                Text(if (st.isSaving) "Menyimpan..." else "Simpan")
            }
        }
    }
}
