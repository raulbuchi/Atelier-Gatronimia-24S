package com.example.core.timers

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AtelierDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerCoordinatorTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: AtelierDatabase
    private lateinit var coordinator: TimerCoordinator
    private var now = 1_000_000L

    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)
    private val notificationManager get() = app.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        db = Room.inMemoryDatabaseBuilder(app, AtelierDatabase::class.java).allowMainThreadQueries().build()
        coordinator = TimerCoordinator(app, db.atelierDao(), TimerAlarmScheduler(app)) { now }
    }

    @After
    fun tearDown() = db.close()

    private fun alertNotifications(): List<Notification> =
        notificationManager.activeNotifications
            .filter { it.id != TimerNotifications.ONGOING_NOTIFICATION_ID }
            .map { it.notification }

    @Test
    fun kitchenTimer_schedulesAlarm_thenAlertsExactlyOnceWhenDue() = runTest {
        val emitted = mutableListOf<TimerTransition>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { coordinator.transitions.toList(emitted) }

        coordinator.updateKitchen("berner_20") { _, t -> TimerMath.startKitchen("berner_20", "Berner Platte", 20, t) }

        val alarm = shadowOf(alarmManager).peekNextScheduledAlarm()
        assertNotNull("alarme do prazo deve estar agendado", alarm)
        assertEquals(now + 20 * 60_000L, alarm!!.triggerAtMs)
        assertNotNull(
            "notificação contínua com contagem regressiva",
            notificationManager.activeNotifications.find { it.id == TimerNotifications.ONGOING_NOTIFICATION_ID }
        )

        // O processo \"morre\" e o alarme acorda o app depois do prazo.
        now += 21 * 60_000L
        coordinator.sync()

        val stored = db.atelierDao().getKitchenTimerOnce("berner_20")!!
        assertFalse(stored.isRunning)
        assertEquals(0, stored.remainingSeconds)
        assertNull("alarme cancelado após o fim", shadowOf(alarmManager).peekNextScheduledAlarm())
        assertNull(notificationManager.activeNotifications.find { it.id == TimerNotifications.ONGOING_NOTIFICATION_ID })

        val alerts = alertNotifications()
        assertEquals(1, alerts.size)
        assertEquals("Cronômetro concluído", alerts.single().extras.getString(Notification.EXTRA_TITLE))
        assertEquals(listOf(TimerTransition.KitchenTimerFinished("berner_20", "Berner Platte")), emitted)

        // Uma segunda sincronização (ex.: UI + receiver) não repete o alerta.
        coordinator.sync()
        assertEquals(1, emitted.size)
    }

    @Test
    fun studyBlock_survivesProcessDeath_andAnnouncesOnlyLatestPhase() = runTest {
        coordinator.updateStudy(4) { t, n -> TimerMath.startStudy(t, n) }
        assertEquals(now + 10 * 60_000L, shadowOf(alarmManager).peekNextScheduledAlarm()!!.triggerAtMs)

        // App fechado por 35 min: Conceito e Visualização venceram; deve estar na Aplicação.
        now += 35 * 60_000L
        coordinator.sync()

        val stored = db.atelierDao().getTimerOnce(4)!!
        assertTrue(stored.isRunning)
        assertEquals(2, stored.currentPhaseIndex)
        assertEquals(stored.phaseEndsAtEpochMs, shadowOf(alarmManager).peekNextScheduledAlarm()!!.triggerAtMs)
        val alerts = alertNotifications()
        assertEquals(1, alerts.size)
        assertTrue(alerts.single().extras.getString(Notification.EXTRA_TEXT)!!.contains("Aplicação"))
    }

    @Test
    fun removingKitchenTimer_cancelsItsAlarm() = runTest {
        coordinator.updateKitchen("x") { _, t -> TimerMath.startKitchen("x", "Fundo escuro", 90, t) }
        assertNotNull(shadowOf(alarmManager).peekNextScheduledAlarm())

        coordinator.updateKitchen("x") { _, _ -> null }

        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
        assertNull(db.atelierDao().getKitchenTimerOnce("x"))
    }
}
