package com.umy.medremindid.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umy.medremindid.ui.settings.NotificationSettingsViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val st by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    fun parseTimeOrDefault(text: String, def: LocalTime): LocalTime {
        return try {
            val t = text.trim()
            if (t.isBlank()) def else LocalTime.parse(t).let { LocalTime.of(it.hour, it.minute) }
        } catch (_: Exception) {
            def
        }
    }

    fun openTimePicker(initial: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.update { it.copy(ringtoneUri = uri?.toString().orEmpty()) }
    }

    fun launchRingtonePicker(currentUri: String) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            val existing = currentUri.trim().ifBlank { null }?.let { Uri.parse(it) }
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
        }
        ringtoneLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Notifikasi") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            st.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifikasi Aktif")
                Switch(
                    checked = st.notificationsEnabled,
                    onCheckedChange = { v -> viewModel.update { it.copy(notificationsEnabled = v) } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jam Tenang (opsional)")
                Switch(
                    checked = st.quietEnabled,
                    onCheckedChange = { v ->
                        viewModel.update {
                            if (!v) it.copy(quietEnabled = false, quietStartText = "", quietEndText = "")
                            else it.copy(quietEnabled = true)
                        }
                    }
                )
            }

            if (st.quietEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val init = parseTimeOrDefault(st.quietStartText, LocalTime.of(22, 0))
                            openTimePicker(init) { picked ->
                                viewModel.update { it.copy(quietStartText = picked.toString().take(5)) }
                            }
                        }
                    ) {
                        Text(if (st.quietStartText.isBlank()) "Mulai" else "Mulai: ${st.quietStartText}")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val init = parseTimeOrDefault(st.quietEndText, LocalTime.of(6, 0))
                            openTimePicker(init) { picked ->
                                viewModel.update { it.copy(quietEndText = picked.toString().take(5)) }
                            }
                        }
                    ) {
                        Text(if (st.quietEndText.isBlank()) "Selesai" else "Selesai: ${st.quietEndText}")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Getar (opsional)")
                Switch(
                    checked = st.allowVibration,
                    onCheckedChange = { v -> viewModel.update { it.copy(allowVibration = v) } }
                )
            }

            Text("Nada Notifikasi (opsional)", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (st.ringtoneUri.isBlank()) "Default / Tidak dipilih" else st.ringtoneUri,
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { launchRingtonePicker(st.ringtoneUri) }
                ) { Text("Pilih Nada") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.update { it.copy(ringtoneUri = "") } }
                ) { Text("Clear") }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !st.isSaving
            ) {
                Text(if (st.isSaving) "Menyimpan..." else "Simpan")
            }
        }
    }
}
