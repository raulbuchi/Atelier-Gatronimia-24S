package com.example.core.timers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMathTest {

    private val t0 = 1_000_000L
    private fun min(m: Int) = m * 60_000L

    @Test
    fun study_remainingIsDerivedFromDeadline() {
        val running = TimerMath.startStudy(TimerMath.newStudyTimer(3, t0), t0)
        assertTrue(running.isRunning)
        assertEquals(t0 + min(10), running.phaseEndsAtEpochMs)
        assertEquals(600, TimerMath.studyRemainingSeconds(running, t0))
        assertEquals(301, TimerMath.studyRemainingSeconds(running, t0 + 299_500))
    }

    @Test
    fun study_pauseFreezesRemainingAndResumeSetsNewDeadline() {
        val running = TimerMath.startStudy(TimerMath.newStudyTimer(1, t0), t0)
        val paused = TimerMath.pauseStudy(running, t0 + min(4))
        assertFalse(paused.isRunning)
        assertEquals(360, paused.remainingSeconds)
        assertEquals(0L, paused.phaseEndsAtEpochMs)

        val resumed = TimerMath.startStudy(paused, t0 + min(60))
        assertEquals(t0 + min(60) + 360_000, resumed.phaseEndsAtEpochMs)
    }

    @Test
    fun study_resolveAdvancesPhasesChainedFromDeadlines() {
        val running = TimerMath.startStudy(TimerMath.newStudyTimer(2, t0), t0)
        // 10 min de Conceito + 20 de Visualização + 5 min dentro da Aplicação.
        val r = TimerMath.resolveStudy(running, t0 + min(35))
        assertEquals(2, r.value.currentPhaseIndex)
        assertTrue(r.value.isRunning)
        assertEquals(t0 + min(60), r.value.phaseEndsAtEpochMs)
        assertEquals(
            listOf(TimerTransition.StudyPhaseAdvanced(2, 1), TimerTransition.StudyPhaseAdvanced(2, 2)),
            r.transitions
        )
        assertEquals(25 * 60, TimerMath.studyRemainingSeconds(r.value, t0 + min(35)))
    }

    @Test
    fun study_resolveIsIdempotent() {
        val running = TimerMath.startStudy(TimerMath.newStudyTimer(2, t0), t0)
        val direct = TimerMath.resolveStudy(running, t0 + min(50)).value
        val stepped = TimerMath.resolveStudy(TimerMath.resolveStudy(running, t0 + min(15)).value, t0 + min(50))
        assertEquals(direct.copy(updatedAtEpochMs = 0), stepped.value.copy(updatedAtEpochMs = 0))
        assertTrue(TimerMath.resolveStudy(direct, t0 + min(50)).transitions.isEmpty())
    }

    @Test
    fun study_blockCompletesAfter80MinutesAndStops() {
        val running = TimerMath.startStudy(TimerMath.newStudyTimer(5, t0), t0)
        val r = TimerMath.resolveStudy(running, t0 + min(300))
        assertFalse(r.value.isRunning)
        assertEquals(0, r.value.currentPhaseIndex)
        assertEquals(1, r.value.completedBlocksCount)
        assertEquals(600, r.value.remainingSeconds)
        assertEquals(TimerTransition.StudyBlockCompleted(5), r.transitions.last())
    }

    @Test
    fun study_skipFromLastPhaseCountsBlock() {
        val atSynthesis = TimerMath.newStudyTimer(1, t0).copy(currentPhaseIndex = 4, remainingSeconds = 120)
        val skipped = TimerMath.skipOrResetStudy(atSynthesis, resetAll = false, nowMs = t0)
        assertEquals(0, skipped.currentPhaseIndex)
        assertEquals(1, skipped.completedBlocksCount)
        val reset = TimerMath.skipOrResetStudy(atSynthesis, resetAll = true, nowMs = t0)
        assertEquals(0, reset.completedBlocksCount)
    }

    @Test
    fun kitchen_finishesOnceAtDeadline() {
        val k = TimerMath.startKitchen("berner_120", "Berner Platte", 120, t0)
        assertTrue(TimerMath.resolveKitchen(k, t0 + min(119)).transitions.isEmpty())
        val done = TimerMath.resolveKitchen(k, t0 + min(121))
        assertFalse(done.value.isRunning)
        assertEquals(0, done.value.remainingSeconds)
        assertEquals(listOf(TimerTransition.KitchenTimerFinished("berner_120", "Berner Platte")), done.transitions)
        assertTrue(TimerMath.resolveKitchen(done.value, t0 + min(200)).transitions.isEmpty())
    }

    @Test
    fun kitchen_toggleKeepsRemainingAcrossPause() {
        val k = TimerMath.startKitchen("x", "Fundo", 10, t0)
        val paused = TimerMath.toggleKitchen(k, t0 + min(3))
        assertEquals(420, paused.remainingSeconds)
        val resumed = TimerMath.toggleKitchen(paused, t0 + min(30))
        assertEquals(t0 + min(30) + 420_000, resumed.endsAtEpochMs)
        val finished = TimerMath.resolveKitchen(resumed, t0 + min(60)).value
        assertEquals(finished, TimerMath.toggleKitchen(finished, t0 + min(61)))
    }
}
