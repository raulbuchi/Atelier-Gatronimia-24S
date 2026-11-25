package com.example.domain.progress

import com.example.domain.model.CourseWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseProgressEngineTest {

    private fun week(
        n: Int,
        recipes: List<String> = listOf("rec-$n"),
        cards: List<String> = listOf("fc-$n-a", "fc-$n-b"),
        deliverables: List<String> = listOf("Portfólio A", "Portfólio B")
    ) = CourseWeek(
        weekNumber = n,
        cycleNumber = (n - 1) / 6 + 1,
        cycleName = "Ciclo ${(n - 1) / 6 + 1}",
        axisId = 1,
        title = "Semana $n",
        concept10Min = "",
        visualization20Min = "",
        application30Min = "",
        practice15Min = "",
        synthesis5Min = "",
        mepFocus = "",
        brochureId = "eixo-1",
        practicalRecipeIds = recipes,
        flashcardIds = cards,
        cycleDeliverables = deliverables
    )

    private val weeks = (1..24).map { week(it) }

    private fun compute(
        start: Long? = 100L,
        today: Long = 100L,
        blocks: Map<Int, Int> = emptyMap(),
        mepWeeks: Set<Int> = emptySet(),
        mepRecipes: Set<String> = emptySet(),
        cards: Set<String> = emptySet(),
        evals: Set<Int> = emptySet(),
        completions: Map<Int, Long> = emptyMap(),
        deliverables: Set<Pair<Int, Int>> = emptySet()
    ) = CourseProgressEngine.compute(
        weeks, start, today, blocks, mepWeeks, mepRecipes, cards, evals, completions, deliverables
    )

    @Test
    fun calendarWeek_countsSevenDayBlocksAndClamps() {
        assertEquals(1, CourseProgressEngine.calendarWeekFor(100, 90))
        assertEquals(1, CourseProgressEngine.calendarWeekFor(100, 100))
        assertEquals(1, CourseProgressEngine.calendarWeekFor(100, 106))
        assertEquals(2, CourseProgressEngine.calendarWeekFor(100, 107))
        assertEquals(24, CourseProgressEngine.calendarWeekFor(100, 100 + 7 * 30))
    }

    @Test
    fun freshStudent_hasNoCriteriaAndFirstWeekPending() {
        val snap = compute()
        assertEquals(0, snap.completedWeeksCount)
        assertEquals(1, snap.nextPendingWeek)
        assertEquals(0, snap.week(1)!!.criteriaMetCount)
        assertFalse(snap.week(1)!!.allCriteriaMet)
    }

    @Test
    fun allFourCriteria_makeWeekEligibleButNotCompletedUntilConfirmed() {
        val snap = compute(
            blocks = mapOf(3 to 1),
            mepRecipes = setOf("rec-3"),
            cards = setOf("fc-3-a", "fc-3-b"),
            evals = setOf(3)
        )
        val w3 = snap.week(3)!!
        assertTrue(w3.allCriteriaMet)
        assertFalse(w3.isCompleted)
    }

    @Test
    fun mepCriterion_acceptsSheetSavedWithWeekNumber() {
        val snap = compute(mepWeeks = setOf(5))
        assertTrue(snap.week(5)!!.mepSheetDone)
        assertFalse(snap.week(6)!!.mepSheetDone)
    }

    @Test
    fun partialFlashcards_doNotSatisfyCriterion_andWeekWithoutCardsDoes() {
        val custom = listOf(week(1), week(2, cards = emptyList()))
        val snap = CourseProgressEngine.compute(
            custom, null, 0, emptyMap(), emptySet(), emptySet(),
            setOf("fc-1-a"), emptySet(), emptyMap(), emptySet()
        )
        assertEquals(1, snap.week(1)!!.flashcardsReviewed)
        assertFalse(snap.week(1)!!.flashcardsDone)
        assertTrue(snap.week(2)!!.flashcardsDone)
        assertNull(snap.calendarWeek)
    }

    @Test
    fun completions_driveOverallAndCycleProgress() {
        val completions = (1..6).associateWith { 1L } + (7 to 1L)
        val snap = compute(
            completions = completions,
            deliverables = setOf(1 to 0, 1 to 1, 2 to 0)
        )
        assertEquals(7, snap.completedWeeksCount)
        assertEquals(8, snap.nextPendingWeek)
        assertEquals(7f / 24f, snap.overallFraction, 0.0001f)
        assertTrue(snap.cycle(1)!!.isCompleted)
        assertFalse(snap.cycle(2)!!.isCompleted)
        assertEquals(setOf(0), snap.cycle(2)!!.deliverablesDone)
    }

    @Test
    fun allWeeksCompleted_hasNoPendingWeek() {
        val snap = compute(completions = (1..24).associateWith { 1L })
        assertNull(snap.nextPendingWeek)
        assertEquals(1f, snap.overallFraction, 0.0001f)
    }
}
