package com.umy.medremindid.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.schedule.MedicationScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormScreen(
    viewModel: MedicationScheduleViewModel,
    scheduleId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val st by viewModel.formState.collectAsState()

    val timeFmt = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    LaunchedEffect(scheduleId) {
        if (scheduleId == null) viewModel.startCreate()
        else viewModel.startEdit(scheduleId)
    }

    fun openTimePicker(current: LocalTime?) {
        val init = current ?: LocalTime.of(8, 0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                viewModel.setTimeOfDay(LocalTime.of(hour, minute))
            },
            init.hour,
            init.minute,
            true
        ).show()
    }

    fun openDatePicker(current: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val cal = Calendar.getInstance()
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
            if (st.error != null) {
                Text(st.error ?: "", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = st.medicineName,
                onValueChange = viewModel::setMedicineName,
                label = { Text("Nama Obat") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = st.dosage,
                onValueChange = viewModel::setDosage,
                label = { Text("Dosis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = st.instructions,
                onValueChange = viewModel::setInstructions,
                label = { Text("Instruksi (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Waktu Minum")
                    Text(st.timeOfDay?.format(timeFmt) ?: "-")
                }
                Button(onClick = { openTimePicker(st.timeOfDay) }) {
                    Text("Pilih Waktu")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Start Date")
                    Text(st.startDate?.format(dateFmt) ?: "-")
                }
                Button(onClick = {
                    openDatePicker(st.startDate) { picked ->
                        viewModel.setStartDate(picked)
                    }
                }) { Text("Pilih") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("End Date (opsional)")
                    Text(st.endDate?.format(dateFmt) ?: "-")
                }
                Row {
                    OutlinedButton(onClick = {
                        openDatePicker(st.endDate) { picked ->
                            viewModel.setEndDate(picked)
                        }
                    }) { Text("Pilih") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.setEndDate(null) }) { Text("Clear") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aktif")
                Switch(
                    checked = st.isActive,
                    onCheckedChange = viewModel::setIsActive
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !st.loading
            ) {
                Text(if (st.loading) "Menyimpan..." else "Simpan")
            }
        }
    }
}
