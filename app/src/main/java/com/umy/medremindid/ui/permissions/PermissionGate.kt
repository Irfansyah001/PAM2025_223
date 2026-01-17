package com.umy.medremindid.ui.permissions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PermissionGate(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    var skipped by rememberSaveable { mutableStateOf(false) }
    var notifRequestedOnce by rememberSaveable { mutableStateOf(false) }

    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var exactAlarmGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.canScheduleExactAlarms()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldShowGate by remember {
        derivedStateOf {
            if (skipped) false else (!notifGranted || !exactAlarmGranted)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notifGranted = isGranted
        notifRequestedOnce = true
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
    }

    if (!shouldShowGate) {
        content()
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Perizinan untuk Pengingat Obat",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Agar pengingat obat berjalan konsisten, aplikasi membutuhkan izin notifikasi dan akses alarm tepat waktu (exact alarms). Anda dapat mengaktifkan sekarang atau lanjut tanpa izin (fitur pengingat bisa kurang optimal).",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            PermissionRow(
                title = "Notifikasi",
                status = if (notifGranted) "Aktif" else "Belum aktif",
                description = "Dibutuhkan untuk menampilkan pengingat dan tombol aksi (Taken / Skip / Snooze).",
                primaryButtonText = if (notifGranted) "OK" else "Izinkan Notifikasi",
                primaryEnabled = !notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                onPrimaryClick = {
                    notifRequestedOnce = true
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                secondaryButtonText = "Buka Pengaturan Aplikasi",
                secondaryEnabled = !notifGranted && isNotifPermanentlyDenied(activity, notifRequestedOnce, notifGranted),
                onSecondaryClick = {
                    settingsLauncher.launch(openAppDetailsSettingsIntent(context))
                }
            )

            HorizontalDivider()

            PermissionRow(
                title = "Exact Alarm (Alarm & reminders)",
                status = if (exactAlarmGranted) "Aktif" else "Belum aktif",
                description = "Dibutuhkan agar jadwal pengingat dapat dipicu tepat waktu (lebih stabil).",
                primaryButtonText = if (exactAlarmGranted) "OK" else "Aktifkan Exact Alarm",
                primaryEnabled = !exactAlarmGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onPrimaryClick = {
                    settingsLauncher.launch(openExactAlarmSettingsIntent(context))
                },
                secondaryButtonText = "Buka Pengaturan Aplikasi",
                secondaryEnabled = !exactAlarmGranted,
                onSecondaryClick = {
                    settingsLauncher.launch(openAppDetailsSettingsIntent(context))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { skipped = true }
                ) {
                    Text("Lanjut Tanpa Izin")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                            notifRequestedOnce = true
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@Button
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmGranted) {
                            settingsLauncher.launch(openExactAlarmSettingsIntent(context))
                            return@Button
                        }
                    }
                ) {
                    Text("Aktifkan Sekarang")
                }
            }

            Text(
                text = "Catatan: Anda bisa mengubah izin kapan pun lewat Settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    status: String,
    description: String,
    primaryButtonText: String,
    primaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String,
    secondaryEnabled: Boolean,
    onSecondaryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = status, style = MaterialTheme.typography.bodyMedium)
        }

        Text(text = description, style = MaterialTheme.typography.bodySmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onPrimaryClick,
                enabled = primaryEnabled
            ) {
                Text(primaryButtonText)
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onSecondaryClick,
                enabled = secondaryEnabled
            ) {
                Text(secondaryButtonText)
            }
        }
    }
}

private fun openAppDetailsSettingsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private fun openExactAlarmSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        openAppDetailsSettingsIntent(context)
    }
}

private fun isNotifPermanentlyDenied(
    activity: Activity?,
    requestedOnce: Boolean,
    granted: Boolean
): Boolean {
    if (granted) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (!requestedOnce) return false
    if (activity == null) return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
