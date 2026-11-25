package com.example.domain.progress

import com.example.domain.model.CourseWeek

/**
 * Progresso de uma semana do curso, derivado dos 4 critérios de conclusão:
 * 1. Bloco de estudo de 80 min concluído;
 * 2. Ficha Técnica MEP salva para a semana (ou para uma receita prática da semana);
 * 3. Flashcards SM-2 da semana revisados ao menos uma vez;
 * 4. Autoavaliação de bancada (Selbstkontrolle) registrada.
 *
 * A conclusão formal ([completedAtEpochMs]) é gravada pelo aluno quando todos os critérios são
 * atendidos e permanece estável mesmo que os dados de origem mudem depois.
 */
data class WeekProgress(
    val weekNumber: Int,
    val cycleNumber: Int,
    val studyBlockDone: Boolean,
    val mepSheetDone: Boolean,
    val flashcardsReviewed: Int,
    val flashcardsTotal: Int,
    val selfEvalDone: Boolean,
    val completedAtEpochMs: Long?
) {
    val flashcardsDone: Boolean get() = flashcardsReviewed >= flashcardsTotal

    val criteriaMetCount: Int
        get() = listOf(studyBlockDone, mepSheetDone, flashcardsDone, selfEvalDone).count { it }

    val allCriteriaMet: Boolean get() = criteriaMetCount == CRITERIA_COUNT

    val isCompleted: Boolean get() = completedAtEpochMs != null

    companion object {
        const val CRITERIA_COUNT = 4
    }
}

data class CycleProgress(
    val cycleNumber: Int,
    val cycleName: String,
    val weekNumbers: List<Int>,
    val completedWeeks: Int,
    val deliverables: List<String>,
    val deliverablesDone: Set<Int>
) {
    val isCompleted: Boolean
        get() = weekNumbers.isNotEmpty() && completedWeeks == weekNumbers.size &&
            deliverablesDone.size >= deliverables.size
}

data class CourseProgressSnapshot(
    val startEpochDay: Long?,
    val calendarWeek: Int?,
    val weeks: List<WeekProgress>,
    val cycles: List<CycleProgress>
) {
    val totalWeeks: Int get() = weeks.size
    val completedWeeksCount: Int get() = weeks.count { it.isCompleted }
    val overallFraction: Float
        get() = if (weeks.isEmpty()) 0f else completedWeeksCount.toFloat() / weeks.size

    /** Primeira semana ainda não concluída (ordem do cronograma), ou null se o curso terminou. */
    val nextPendingWeek: Int? get() = weeks.firstOrNull { !it.isCompleted }?.weekNumber

    fun week(weekNumber: Int): WeekProgress? = weeks.firstOrNull { it.weekNumber == weekNumber }

    fun cycle(cycleNumber: Int): CycleProgress? = cycles.firstOrNull { it.cycleNumber == cycleNumber }

    companion object {
        val EMPTY = CourseProgressSnapshot(null, null, emptyList(), emptyList())
    }
}

object CourseProgressEngine {

    const val TOTAL_WEEKS = 24

    /** Semana do cronograma em que o aluno "deveria" estar, contando 7 dias por semana desde o início. */
    fun calendarWeekFor(startEpochDay: Long, todayEpochDay: Long): Int {
        if (todayEpochDay < startEpochDay) return 1
        return ((todayEpochDay - startEpochDay) / 7 + 1).toInt().coerceIn(1, TOTAL_WEEKS)
    }

    fun compute(
        weeks: List<CourseWeek>,
        startEpochDay: Long?,
        todayEpochDay: Long,
        completedStudyBlocksByWeek: Map<Int, Int>,
        mepSheetWeeks: Set<Int>,
        mepSheetRecipeIds: Set<String>,
        reviewedFlashcardIds: Set<String>,
        selfEvaluatedWeeks: Set<Int>,
        completionsByWeek: Map<Int, Long>,
        deliverableChecks: Set<Pair<Int, Int>>
    ): CourseProgressSnapshot {
        val weekProgress = weeks.sortedBy { it.weekNumber }.map { w ->
            WeekProgress(
                weekNumber = w.weekNumber,
                cycleNumber = w.cycleNumber,
                studyBlockDone = (completedStudyBlocksByWeek[w.weekNumber] ?: 0) >= 1,
                mepSheetDone = w.weekNumber in mepSheetWeeks ||
                    w.practicalRecipeIds.any { it in mepSheetRecipeIds },
                flashcardsReviewed = w.flashcardIds.count { it in reviewedFlashcardIds },
                flashcardsTotal = w.flashcardIds.size,
                selfEvalDone = w.weekNumber in selfEvaluatedWeeks,
                completedAtEpochMs = completionsByWeek[w.weekNumber]
            )
        }

        val cycles = weeks.groupBy { it.cycleNumber }.toSortedMap().map { (cycleNum, cycleWeeks) ->
            val deliverables = cycleWeeks.firstOrNull { it.cycleDeliverables.isNotEmpty() }
                ?.cycleDeliverables.orEmpty()
            val weekNums = cycleWeeks.map { it.weekNumber }.sorted()
            CycleProgress(
                cycleNumber = cycleNum,
                cycleName = cycleWeeks.first().cycleName,
                weekNumbers = weekNums,
                completedWeeks = weekNums.count { it in completionsByWeek },
                deliverables = deliverables,
                deliverablesDone = deliverables.indices
                    .filter { (cycleNum to it) in deliverableChecks }
                    .toSet()
            )
        }

        return CourseProgressSnapshot(
            startEpochDay = startEpochDay,
            calendarWeek = startEpochDay?.let { calendarWeekFor(it, todayEpochDay) },
            weeks = weekProgress,
            cycles = cycles
        )
    }
}
