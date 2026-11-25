package com.example.core.timers

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class RunningTimerInfo(val label: String, val endsAtEpochMs: Long)

/** Textos em pt-BR de cada transição (usados na notificação e no aviso dentro do app). */
fun TimerTransition.title(): String = when (this) {
    is TimerTransition.StudyPhaseAdvanced -> "Semana S${"%02d".format(weekNumber)} • nova fase do estudo"
    is TimerTransition.StudyBlockCompleted -> "Bloco de estudo de 80 min concluído"
    is TimerTransition.KitchenTimerFinished -> "Cronômetro concluído"
}

fun TimerTransition.body(): String = when (this) {
    is TimerTransition.StudyPhaseAdvanced ->
        "Agora: ${TimerMath.STUDY_PHASE_LABELS.getOrElse(newPhaseIndex) { "" }}"
    is TimerTransition.StudyBlockCompleted ->
        "Semana S${"%02d".format(weekNumber)}: registre a síntese e a autoavaliação."
    is TimerTransition.KitchenTimerFinished -> label
}

object TimerNotifications {

    const val CHANNEL_ALERTS = "timer_alerts"
    const val CHANNEL_ONGOING = "timer_ongoing"
    const val ONGOING_NOTIFICATION_ID = 7000
    private const val STUDY_BASE_ID = 7100
    private const val KITCHEN_BASE_ID = 7200

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alertas de cronômetro", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Fim de fases do bloco de estudo e cronômetros do Modo Cozinha"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Cronômetros em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Contagem regressiva dos cronômetros ativos"
                setShowBadge(false)
            }
        )
    }

    fun canPost(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun idFor(transition: TimerTransition): Int = when (transition) {
        is TimerTransition.StudyPhaseAdvanced -> STUDY_BASE_ID + transition.weekNumber
        is TimerTransition.StudyBlockCompleted -> STUDY_BASE_ID + transition.weekNumber
        is TimerTransition.KitchenTimerFinished -> KITCHEN_BASE_ID + (transition.id.hashCode() and 0xFFF)
    }

    @SuppressLint("MissingPermission") // Guardado por canPost().
    fun notifyTransition(context: Context, transition: TimerTransition) {
        if (!canPost(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(transition.title())
            .setContentText(transition.body())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(idFor(transition), notification)
        } catch (_: SecurityException) {
            // Permissão revogada entre a verificação e o envio: o aviso dentro do app continua valendo.
        }
    }

    /** Notificação silenciosa com contagem regressiva até o próximo prazo; removida quando nada roda. */
    @SuppressLint("MissingPermission") // Guardado por canPost().
    fun updateOngoing(context: Context, running: List<RunningTimerInfo>) {
        val manager = NotificationManagerCompat.from(context)
        if (running.isEmpty() || !canPost(context)) {
            manager.cancel(ONGOING_NOTIFICATION_ID)
            return
        }
        ensureChannels(context)
        val next = running.minBy { it.endsAtEpochMs }
        val title = if (running.size == 1) "Cronômetro em andamento" else "${running.size} cronômetros em andamento"
        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText("Próximo: ${next.label}")
            .setWhen(next.endsAtEpochMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            manager.notify(ONGOING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
