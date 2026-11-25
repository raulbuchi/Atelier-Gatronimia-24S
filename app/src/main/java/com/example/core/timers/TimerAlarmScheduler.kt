package com.example.core.timers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri

/**
 * Agenda um alarme por cronômetro no prazo final. Usa alarme exato quando o usuário permite
 * (`SCHEDULE_EXACT_ALARM`); caso contrário, alarme inexato que o sistema pode atrasar em Doze.
 */
class TimerAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    fun schedule(key: String, atEpochMs: Long) {
        val am = alarmManager ?: return
        val pi = pendingIntent(key, create = true) ?: return
        try {
            if (canScheduleExact()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
        }
    }

    fun cancel(key: String) {
        val pi = pendingIntent(key, create = false) ?: return
        alarmManager?.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(key: String, create: Boolean): PendingIntent? {
        val intent = Intent(context, TimerSyncReceiver::class.java)
            .setAction(ACTION_TIMER_DUE)
            .setData("atelier-timer://$key".toUri())
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        const val ACTION_TIMER_DUE = "com.example.timers.ACTION_TIMER_DUE"

        fun studyKey(weekNumber: Int) = "study/$weekNumber"
        fun kitchenKey(id: String) = "kitchen/${Uri.encode(id)}"
    }
}
