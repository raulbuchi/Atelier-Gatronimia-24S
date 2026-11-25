package com.example.core.timers

import com.example.data.local.KitchenTimerEntity
import com.example.data.local.StudyTimerEntity

/** Eventos produzidos quando um prazo é atingido (viram notificação e aviso na UI). */
sealed interface TimerTransition {
    data class StudyPhaseAdvanced(val weekNumber: Int, val newPhaseIndex: Int) : TimerTransition
    data class StudyBlockCompleted(val weekNumber: Int) : TimerTransition
    data class KitchenTimerFinished(val id: String, val label: String) : TimerTransition
}

data class Resolved<T>(val value: T, val transitions: List<TimerTransition>) {
    val changed: Boolean get() = transitions.isNotEmpty()
}

/**
 * Regras de tempo baseadas em prazo (epoch ms). Todas as funções são puras e idempotentes:
 * `resolve(resolve(s, t1), t2) == resolve(s, t2)` para `t1 <= t2`, de modo que a UI, o receiver
 * de alarme e o boot podem resolver o mesmo estado sem duplicar transições.
 */
object TimerMath {

    /** Bloco de 80 min: Conceito 10 → Visualização 20 → Aplicação 30 → Treino 15 → Síntese 5. */
    val STUDY_PHASE_DURATIONS_SEC = listOf(10 * 60, 20 * 60, 30 * 60, 15 * 60, 5 * 60)
    val STUDY_PHASE_LABELS = listOf(
        "1. Conceito (10 min)",
        "2. Visualização (20 min)",
        "3. Aplicação (30 min)",
        "4. Treino (15 min)",
        "5. Síntese (5 min)"
    )
    val STUDY_LAST_PHASE = STUDY_PHASE_DURATIONS_SEC.lastIndex

    private fun ceilSeconds(ms: Long): Int = ((ms + 999) / 1000).toInt().coerceAtLeast(0)

    // ---------- Bloco de estudo ----------

    fun newStudyTimer(weekNumber: Int, nowMs: Long) = StudyTimerEntity(
        weekNumber = weekNumber,
        currentPhaseIndex = 0,
        remainingSeconds = STUDY_PHASE_DURATIONS_SEC[0],
        isRunning = false,
        updatedAtEpochMs = nowMs
    )

    fun studyRemainingSeconds(t: StudyTimerEntity, nowMs: Long): Int =
        if (t.isRunning && t.phaseEndsAtEpochMs > 0) ceilSeconds(t.phaseEndsAtEpochMs - nowMs)
        else t.remainingSeconds

    fun startStudy(t: StudyTimerEntity, nowMs: Long): StudyTimerEntity {
        if (t.isRunning) return t
        val remaining = t.remainingSeconds.takeIf { it > 0 }
            ?: STUDY_PHASE_DURATIONS_SEC.getOrElse(t.currentPhaseIndex) { STUDY_PHASE_DURATIONS_SEC[0] }
        return t.copy(
            isRunning = true,
            remainingSeconds = remaining,
            phaseEndsAtEpochMs = nowMs + remaining * 1000L,
            updatedAtEpochMs = nowMs
        )
    }

    fun pauseStudy(t: StudyTimerEntity, nowMs: Long): StudyTimerEntity {
        if (!t.isRunning) return t
        return t.copy(
            isRunning = false,
            remainingSeconds = studyRemainingSeconds(t, nowMs),
            phaseEndsAtEpochMs = 0L,
            updatedAtEpochMs = nowMs
        )
    }

    /** Pular para a próxima fase (pausado) ou reiniciar o bloco. Concluir a Síntese conta um bloco. */
    fun skipOrResetStudy(t: StudyTimerEntity, resetAll: Boolean, nowMs: Long): StudyTimerEntity {
        val nextPhase = if (resetAll) 0 else (t.currentPhaseIndex + 1) % STUDY_PHASE_DURATIONS_SEC.size
        val completedInc = if (!resetAll && t.currentPhaseIndex == STUDY_LAST_PHASE) 1 else 0
        return t.copy(
            currentPhaseIndex = nextPhase,
            remainingSeconds = STUDY_PHASE_DURATIONS_SEC[nextPhase],
            isRunning = false,
            phaseEndsAtEpochMs = 0L,
            completedBlocksCount = t.completedBlocksCount + completedInc,
            updatedAtEpochMs = nowMs
        )
    }

    /** Avança as fases vencidas até [nowMs]; o bloco para (pausado na fase 1) ao terminar a Síntese. */
    fun resolveStudy(t: StudyTimerEntity, nowMs: Long): Resolved<StudyTimerEntity> {
        if (!t.isRunning || t.phaseEndsAtEpochMs <= 0 || nowMs < t.phaseEndsAtEpochMs) {
            return Resolved(t, emptyList())
        }
        val transitions = mutableListOf<TimerTransition>()
        var phase = t.currentPhaseIndex.coerceIn(0, STUDY_LAST_PHASE)
        var endsAt = t.phaseEndsAtEpochMs
        while (nowMs >= endsAt) {
            if (phase >= STUDY_LAST_PHASE) {
                transitions += TimerTransition.StudyBlockCompleted(t.weekNumber)
                return Resolved(
                    t.copy(
                        currentPhaseIndex = 0,
                        remainingSeconds = STUDY_PHASE_DURATIONS_SEC[0],
                        isRunning = false,
                        phaseEndsAtEpochMs = 0L,
                        completedBlocksCount = t.completedBlocksCount + 1,
                        updatedAtEpochMs = nowMs
                    ),
                    transitions
                )
            }
            phase += 1
            transitions += TimerTransition.StudyPhaseAdvanced(t.weekNumber, phase)
            endsAt += STUDY_PHASE_DURATIONS_SEC[phase] * 1000L
        }
        return Resolved(
            t.copy(
                currentPhaseIndex = phase,
                remainingSeconds = STUDY_PHASE_DURATIONS_SEC[phase],
                phaseEndsAtEpochMs = endsAt,
                updatedAtEpochMs = nowMs
            ),
            transitions
        )
    }

    // ---------- Cronômetros da cozinha ----------

    fun startKitchen(id: String, label: String, durationMinutes: Int, nowMs: Long): KitchenTimerEntity {
        val total = durationMinutes.coerceAtLeast(1) * 60
        return KitchenTimerEntity(
            id = id,
            label = label,
            totalSeconds = total,
            remainingSeconds = total,
            endsAtEpochMs = nowMs + total * 1000L,
            isRunning = true,
            createdAtEpochMs = nowMs
        )
    }

    fun kitchenRemainingSeconds(k: KitchenTimerEntity, nowMs: Long): Int =
        if (k.isRunning && k.endsAtEpochMs > 0) ceilSeconds(k.endsAtEpochMs - nowMs)
        else k.remainingSeconds

    /** Pausa se estiver rodando; retoma se pausado com tempo restante; timer zerado não muda. */
    fun toggleKitchen(k: KitchenTimerEntity, nowMs: Long): KitchenTimerEntity = when {
        k.isRunning -> k.copy(isRunning = false, remainingSeconds = kitchenRemainingSeconds(k, nowMs), endsAtEpochMs = 0L)
        k.remainingSeconds > 0 -> k.copy(isRunning = true, endsAtEpochMs = nowMs + k.remainingSeconds * 1000L)
        else -> k
    }

    fun resolveKitchen(k: KitchenTimerEntity, nowMs: Long): Resolved<KitchenTimerEntity> {
        if (!k.isRunning || k.endsAtEpochMs <= 0 || nowMs < k.endsAtEpochMs) return Resolved(k, emptyList())
        return Resolved(
            k.copy(isRunning = false, remainingSeconds = 0, endsAtEpochMs = 0L),
            listOf(TimerTransition.KitchenTimerFinished(k.id, k.label))
        )
    }
}
