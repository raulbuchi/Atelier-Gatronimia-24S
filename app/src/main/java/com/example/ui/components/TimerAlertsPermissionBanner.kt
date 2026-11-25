package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.core.timers.TimerNotifications
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg

/**
 * Enquanto houver cronômetro rodando: pede a permissão de notificação (Android 13+) uma vez e,
 * se faltar notificação ou alarme exato, mostra um aviso com atalho para as configurações.
 * [onPermissionsChanged] é chamado ao voltar ao app para reagendar os alarmes.
 */
@Composable
fun TimerAlertsPermissionBanner(
    anyTimerRunning: Boolean,
    canScheduleExactAlarms: () -> Boolean,
    onPermissionsChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
        onPermissionsChanged()
    }

    LifecycleResumeEffect(Unit) {
        refreshTick++
        onPermissionsChanged()
        onPauseOrDispose { }
    }

    LaunchedEffect(anyTimerRunning) {
        if (anyTimerRunning && !askedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !TimerNotifications.canPost(context)
        ) {
            askedNotificationPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val notificationsOk = remember(refreshTick, anyTimerRunning) { TimerNotifications.canPost(context) }
    val exactOk = remember(refreshTick, anyTimerRunning) { canScheduleExactAlarms() }

    if (!anyTimerRunning || dismissed || (notificationsOk && exactOk)) return

    Surface(color = PccAmberBg, modifier = modifier.fillMaxWidth().testTag("timer_permission_banner")) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PccAmber)
                Text(
                    text = if (!notificationsOk) {
                        "Ative as notificações para ser avisado quando o cronômetro terminar com o app fechado."
                    } else {
                        "Com a tela apagada o alerta pode atrasar alguns minutos. Permita alarmes exatos para avisos pontuais."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PccAmber,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = { dismissed = true }) { Text("Agora não") }
                TextButton(
                    onClick = {
                        context.startActivity(
                            if (!notificationsOk) notificationSettingsIntent(context) else exactAlarmSettingsIntent(context)
                        )
                    },
                    modifier = Modifier.testTag("timer_permission_open_settings")
                ) { Text(if (!notificationsOk) "Ativar" else "Permitir") }
            }
        }
    }
}

private fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    }

private fun exactAlarmSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    }
