package com.umy.medremindid.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    viewModel: NotificationPreferencesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val picked: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.setRingtoneUri(picked?.toString())
    }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        val msg = state.errorMessage ?: state.infoMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Notifikasi") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kelola preferensi notifikasi pengingat obat.",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()

            RowSwitch(
                title = "Aktifkan Notifikasi",
                checked = state.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )

            RowSwitch(
                title = "Getar",
                checked = state.allowVibration,
                onCheckedChange = { viewModel.setAllowVibration(it) }
            )

            Divider()

            Text(
                text = "Jam Tenang (Quiet Hours) (Opsional)",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Jika sedang jam tenang, sistem tidak menampilkan notifikasi dan akan menunda (snooze) hingga jam tenang selesai.",
                style = MaterialTheme.typography.bodySmall
            )

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.quietStartText,
                onValueChange = { viewModel.updateQuietStartText(it) },
                label = { Text("Mulai (HH:mm)") },
                placeholder = { Text("Contoh: 22:00") },
                singleLine = true
            )

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.quietEndText,
                onValueChange = { viewModel.updateQuietEndText(it) },
                label = { Text("Selesai (HH:mm)") },
                placeholder = { Text("Contoh: 06:00") },
                singleLine = true
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.saveQuietHours() }
            ) {
                Text("Simpan Jam Tenang")
            }

            Divider()

            Text(
                text = "Nada Notifikasi (Opsional)",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = if (state.ringtoneUri.isNullOrBlank())
                    "Nada: Default sistem"
                else
                    "Nada: Tersimpan",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            state.ringtoneUri?.let { Uri.parse(it) }
                        )
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            ) {
                Text("Pilih Nada Notifikasi")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.setRingtoneUri(null) }
            ) {
                Text("Reset ke Default")
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text("Kembali")
            }
        }
    }
}

@Composable
private fun RowSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
