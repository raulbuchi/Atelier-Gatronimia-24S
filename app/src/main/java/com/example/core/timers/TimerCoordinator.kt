package com.example.core.timers

import android.content.Context
import com.example.data.local.AtelierDao
import com.example.data.local.AtelierDatabase
import com.example.data.local.KitchenTimerEntity
import com.example.data.local.StudyTimerEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Único ponto que altera cronômetros. A cada operação: resolve prazos vencidos, grava o estado,
 * notifica as transições (uma vez só, graças ao [mutex]), reagenda os alarmes e atualiza a
 * notificação contínua. Chamado pelo ViewModel, pelo [TimerSyncReceiver] (alarme/boot) e na abertura.
 */
class TimerCoordinator internal constructor(
    private val appContext: Context,
    private val dao: AtelierDao,
    private val scheduler: TimerAlarmScheduler = TimerAlarmScheduler(appContext),
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()

    private val _transitions = MutableSharedFlow<TimerTransition>(extraBufferCapacity = 16)

    /** Transições ocorridas neste processo, para avisos dentro do app. */
    val transitions: SharedFlow<TimerTransition> = _transitions.asSharedFlow()

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    suspend fun sync() = mutex.withLock { syncLocked(clock()) }

    suspend fun updateStudy(weekNumber: Int, transform: (StudyTimerEntity, Long) -> StudyTimerEntity) =
        mutex.withLock {
            val now = clock()
            syncLocked(now)
            val current = dao.getTimerOnce(weekNumber) ?: TimerMath.newStudyTimer(weekNumber, now)
            dao.upsertTimer(transform(current, now))
            syncLocked(now)
        }

    /** [transform] recebe o cronômetro atual (ou null) e devolve o novo estado, ou null para removê-lo. */
    suspend fun updateKitchen(id: String, transform: (KitchenTimerEntity?, Long) -> KitchenTimerEntity?) =
        mutex.withLock {
            val now = clock()
            syncLocked(now)
            val next = transform(dao.getKitchenTimerOnce(id), now)
            if (next == null) {
                dao.deleteKitchenTimer(id)
                scheduler.cancel(TimerAlarmScheduler.kitchenKey(id))
            } else {
                dao.upsertKitchenTimer(next)
            }
            syncLocked(now)
        }

    private suspend fun syncLocked(now: Long) {
        val running = mutableListOf<RunningTimerInfo>()

        dao.getAllTimersOnce().forEach { timer ->
            val resolved = TimerMath.resolveStudy(timer, now)
            if (resolved.changed) {
                dao.upsertTimer(resolved.value)
                announce(resolved.transitions)
            }
            val t = resolved.value
            val key = TimerAlarmScheduler.studyKey(t.weekNumber)
            if (t.isRunning) {
                scheduler.schedule(key, t.phaseEndsAtEpochMs)
                val phase = TimerMath.STUDY_PHASE_LABELS.getOrElse(t.currentPhaseIndex) { "" }
                running += RunningTimerInfo("Estudo S${"%02d".format(t.weekNumber)} • $phase", t.phaseEndsAtEpochMs)
            } else {
                scheduler.cancel(key)
            }
        }

        dao.getAllKitchenTimersOnce().forEach { timer ->
            val resolved = TimerMath.resolveKitchen(timer, now)
            if (resolved.changed) {
                dao.upsertKitchenTimer(resolved.value)
                announce(resolved.transitions)
            }
            val k = resolved.value
            val key = TimerAlarmScheduler.kitchenKey(k.id)
            if (k.isRunning) {
                scheduler.schedule(key, k.endsAtEpochMs)
                running += RunningTimerInfo(k.label, k.endsAtEpochMs)
            } else {
                scheduler.cancel(key)
            }
        }

        TimerNotifications.updateOngoing(appContext, running)
    }

    /** Após muito tempo fechado várias fases podem vencer de uma vez: alerta só a mais recente. */
    private fun announce(transitions: List<TimerTransition>) {
        val latest = transitions.lastOrNull() ?: return
        TimerNotifications.notifyTransition(appContext, latest)
        _transitions.tryEmit(latest)
    }

    companion object {
        @Volatile
        private var INSTANCE: TimerCoordinator? = null

        fun get(context: Context): TimerCoordinator = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val app = context.applicationContext
                TimerCoordinator(app, AtelierDatabase.getInstance(app).atelierDao()).also { INSTANCE = it }
            }
        }
    }
}
