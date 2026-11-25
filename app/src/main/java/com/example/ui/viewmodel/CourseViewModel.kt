package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.core.session.localEpochDayNow
import com.example.core.timers.TimerCoordinator
import com.example.core.timers.TimerMath
import com.example.core.timers.timerUiClock
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.Sm2ProgressEntity
import com.example.data.local.StudyTimerEntity
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.progress.CourseProgressEngine
import com.example.domain.progress.CourseProgressSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Curso e estudo: semana em foco, progressão (4 critérios + conclusão + entregáveis), bloco de
 * estudo de 80 min, flashcards SM-2 e navegação interna da Aula & Atlas.
 * Telas: Painel do Curso e Aula & Atlas.
 */
class CourseViewModel(
    private val localRepo: LocalKitchenRepository,
    private val session: CourseSession,
    private val timerCoordinator: TimerCoordinator,
    private val messages: UserMessages
) : ViewModel() {

    companion object {
        val STUDY_PHASE_DURATIONS_SEC = TimerMath.STUDY_PHASE_DURATIONS_SEC
        val STUDY_PHASE_LABELS = TimerMath.STUDY_PHASE_LABELS
    }

    val selectedWeekNumber: StateFlow<Int> = session.selectedWeek

    fun selectWeek(week: Int) = session.selectWeek(week)

    // ---------- Progressão ----------

    private data class ProgressSources(
        val startEpochDay: Long?,
        val completedBlocksByWeek: Map<Int, Int>,
        val mepSheetWeeks: Set<Int>,
        val mepSheetRecipeIds: Set<String>,
        val reviewedFlashcardIds: Set<String>
    )

    private val progressSources = combine(
        localRepo.courseProgress,
        localRepo.allTimers,
        localRepo.mepSheets,
        localRepo.sm2Progress
    ) { progress, timers, sheets, sm2 ->
        ProgressSources(
            startEpochDay = progress?.startEpochDay,
            completedBlocksByWeek = timers.associate { it.weekNumber to it.completedBlocksCount },
            mepSheetWeeks = sheets.map { it.weekNumber }.toSet(),
            mepSheetRecipeIds = sheets.map { it.recipeId }.toSet(),
            reviewedFlashcardIds = sm2.filter { it.lastGrade >= 0 }.map { it.cardId }.toSet()
        )
    }

    val courseProgress: StateFlow<CourseProgressSnapshot> = combine(
        progressSources,
        localRepo.selfEvaluations,
        localRepo.weekCompletions,
        localRepo.cycleDeliverableChecks
    ) { src, evals, completions, deliverables ->
        CourseProgressEngine.compute(
            weeks = CurriculumWeeksAndGuides.weeks,
            startEpochDay = src.startEpochDay,
            todayEpochDay = localEpochDayNow(),
            completedStudyBlocksByWeek = src.completedBlocksByWeek,
            mepSheetWeeks = src.mepSheetWeeks,
            mepSheetRecipeIds = src.mepSheetRecipeIds,
            reviewedFlashcardIds = src.reviewedFlashcardIds,
            selfEvaluatedWeeks = evals.map { it.weekNumber }.toSet(),
            completionsByWeek = completions.associate { it.weekNumber to it.completedAtEpochMs },
            deliverableChecks = deliverables.filter { it.isDone }
                .map { it.cycleNumber to it.deliverableIndex }
                .toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CourseProgressSnapshot.EMPTY)

    fun completeWeek(weekNumber: Int) {
        val progress = courseProgress.value.week(weekNumber)
        if (progress == null || !progress.allCriteriaMet) {
            messages.show("Conclua os 4 critérios da Semana $weekNumber antes de encerrá-la.")
            return
        }
        viewModelScope.launch {
            localRepo.markWeekCompleted(weekNumber)
            messages.show(
                if (weekNumber < CourseProgressEngine.TOTAL_WEEKS) {
                    "Semana $weekNumber concluída! A Semana ${weekNumber + 1} está pronta para começar."
                } else {
                    "Semana 24 concluída — parabéns por completar o Atelier Gastronomia 24S!"
                }
            )
        }
    }

    fun reopenWeek(weekNumber: Int) {
        viewModelScope.launch {
            localRepo.reopenWeek(weekNumber)
            messages.show("Semana $weekNumber reaberta.")
        }
    }

    fun setCourseStartDate(startEpochDay: Long) {
        viewModelScope.launch {
            localRepo.updateCourseStartDate(startEpochDay)
            messages.show("Data de início do curso atualizada.")
        }
    }

    fun setCycleDeliverableDone(cycleNumber: Int, deliverableIndex: Int, isDone: Boolean) {
        viewModelScope.launch { localRepo.setCycleDeliverableDone(cycleNumber, deliverableIndex, isDone) }
    }

    // ---------- Bloco de estudo de 80 min ----------

    private var timerSyncJob: Job? = null

    /** Blocos de estudo por semana, com o tempo restante derivado do prazo a cada segundo. */
    val timersMap: StateFlow<Map<Int, StudyTimerEntity>> =
        combine(localRepo.allTimers, timerUiClock()) { list, now ->
            var anyDue = false
            val display = list.associate { stored ->
                val resolved = TimerMath.resolveStudy(stored, now)
                if (resolved.changed) anyDue = true
                val t = resolved.value
                t.weekNumber to t.copy(remainingSeconds = TimerMath.studyRemainingSeconds(t, now))
            }
            if (anyDue) requestTimerSync()
            display
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    fun toggleStudyTimer(weekNumber: Int) {
        viewModelScope.launch {
            timerCoordinator.updateStudy(weekNumber) { t, now ->
                if (t.isRunning) TimerMath.pauseStudy(t, now) else TimerMath.startStudy(t, now)
            }
        }
    }

    fun advanceOrResetStudyPhase(weekNumber: Int, resetAll: Boolean = false) {
        viewModelScope.launch {
            timerCoordinator.updateStudy(weekNumber) { t, now -> TimerMath.skipOrResetStudy(t, resetAll, now) }
        }
    }

    private fun requestTimerSync() {
        if (timerSyncJob?.isActive == true) return
        timerSyncJob = viewModelScope.launch { timerCoordinator.sync() }
    }

    // ---------- Flashcards SM-2 ----------

    val sm2Map: StateFlow<Map<String, Sm2ProgressEntity>> = localRepo.sm2Progress
        .map { list -> list.associateBy { it.cardId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    fun gradeFlashcard(cardId: String, grade0To5: Int) {
        val todayEpochDay = localEpochDayNow()
        val next = CulinaryCalculators.computeNextSm2State(sm2Map.value[cardId], cardId, grade0To5, todayEpochDay)
        viewModelScope.launch { localRepo.upsertSm2(next) }
    }

    fun dueFlashcardsCount(): Int {
        val todayEpochDay = localEpochDayNow()
        val map = sm2Map.value
        return TechnicalAtlasCatalog.flashcards.count { card ->
            val state = map[card.id]
            state == null || state.nextReviewEpochDay <= todayEpochDay
        }
    }

    // ---------- Navegação interna da Aula & Atlas ----------

    private val _selectedLibraryAxis = MutableStateFlow(1)
    val selectedLibraryAxis: StateFlow<Int> = _selectedLibraryAxis.asStateFlow()

    private val _requestedAtlasSubTab = MutableStateFlow<Int?>(null)
    val requestedAtlasSubTab: StateFlow<Int?> = _requestedAtlasSubTab.asStateFlow()

    private val _sm2AxisFilter = MutableStateFlow(0)
    val sm2AxisFilter: StateFlow<Int> = _sm2AxisFilter.asStateFlow()

    fun selectLibraryAxis(axisNumber: Int) {
        _selectedLibraryAxis.value = axisNumber.coerceIn(1, 6)
    }

    fun openDidacticLibraryAxis(axisNumber: Int) {
        _selectedLibraryAxis.value = axisNumber.coerceIn(1, 6)
        _requestedAtlasSubTab.value = 5 // Sub-aba "Biblioteca Didática (6 Eixos)"
    }

    fun practiceAxisInSm2(axisNumber: Int) {
        _sm2AxisFilter.value = axisNumber.coerceIn(1, 6)
        _requestedAtlasSubTab.value = 7 // Sub-aba "Cartões SM-2 (60)"
    }

    fun setSm2AxisFilter(axisNumber: Int) {
        _sm2AxisFilter.value = axisNumber.coerceIn(0, 6)
    }

    fun consumeAtlasSubTabRequest() {
        _requestedAtlasSubTab.value = null
    }
}
