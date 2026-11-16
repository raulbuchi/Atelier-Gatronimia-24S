package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.EquipmentOwnedEntity
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.MepSheetEntity
import com.example.data.local.SelfEvalEntity
import com.example.data.local.ShoppingFifoEntity
import com.example.data.local.Sm2ProgressEntity
import com.example.data.local.StudyTimerEntity
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.model.RecipeSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParallelKitchenTimer(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean
)

data class PendingMepPhotoTransfer(
    val weekNumber: Int,
    val cycleNumber: Int,
    val axisId: Int,
    val recipeId: String,
    val recipeTitle: String,
    val isSwissmilkClassic: Boolean,
    val isChefkochV4: Boolean,
    val cantonCode: String,
    val mepSummary: String
)

class AtelierViewModel(
    private val localRepo: LocalKitchenRepository
) : ViewModel() {

    companion object {
        val STUDY_PHASE_DURATIONS_SEC = listOf(10 * 60, 20 * 60, 30 * 60, 15 * 60, 5 * 60)
        val STUDY_PHASE_LABELS = listOf(
            "1. Conceito (10 min)",
            "2. Visualização (20 min)",
            "3. Aplicação (30 min)",
            "4. Treino (15 min)",
            "5. Síntese (5 min)"
        )
    }

    private val _selectedWeekNumber = MutableStateFlow(1)
    val selectedWeekNumber: StateFlow<Int> = _selectedWeekNumber.asStateFlow()

    private val _selectedRecipeId = MutableStateFlow(
        SwissmilkAndBaseRecipesCatalog.allRecipes.firstOrNull()?.id.orEmpty()
    )
    val selectedRecipeId: StateFlow<String> = _selectedRecipeId.asStateFlow()

    private val _targetServings = MutableStateFlow(4)
    val targetServings: StateFlow<Int> = _targetServings.asStateFlow()

    private val _isKitchenModeActive = MutableStateFlow(false)
    val isKitchenModeActive: StateFlow<Boolean> = _isKitchenModeActive.asStateFlow()

    private val _activeCookingStepIndex = MutableStateFlow(0)
    val activeCookingStepIndex: StateFlow<Int> = _activeCookingStepIndex.asStateFlow()

    private val _parallelTimers = MutableStateFlow<List<ParallelKitchenTimer>>(emptyList())
    val parallelTimers: StateFlow<List<ParallelKitchenTimer>> = _parallelTimers.asStateFlow()

    private val _pendingPhotoTransfer = MutableStateFlow<PendingMepPhotoTransfer?>(null)
    val pendingPhotoTransfer: StateFlow<PendingMepPhotoTransfer?> = _pendingPhotoTransfer.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val timersMap: StateFlow<Map<Int, StudyTimerEntity>> = localRepo.allTimers
        .map { list -> list.associateBy { it.weekNumber } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val sm2Map: StateFlow<Map<String, Sm2ProgressEntity>> = localRepo.sm2Progress
        .map { list -> list.associateBy { it.cardId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val mepSheetsMap: StateFlow<Map<String, MepSheetEntity>> = localRepo.mepSheets
        .map { list ->
            val map = list.associateBy { it.recipeId }.toMutableMap()
            // Pre-populate Zürcher Geschnetzeltes + Rösti reference sheet if absent
            val ex = CurriculumWeeksAndGuides.exampleMepZurcherRosti
            if (!map.containsKey(ex.recipeId)) {
                map[ex.recipeId] = ex
            }
            map
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            mapOf(CurriculumWeeksAndGuides.exampleMepZurcherRosti.recipeId to CurriculumWeeksAndGuides.exampleMepZurcherRosti)
        )

    val shoppingItems: StateFlow<List<ShoppingFifoEntity>> = localRepo.shoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ownedEquipmentIds: StateFlow<Set<String>> = localRepo.ownedEquipment
        .map { list -> list.filter { it.isOwned }.map { it.equipmentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val selfEvaluationsMap: StateFlow<Map<Int, SelfEvalEntity>> = localRepo.selfEvaluations
        .map { list -> list.associateBy { it.weekNumber } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private var studyTimerJob: Job? = null
    private var parallelTimersJob: Job? = null

    init {
        startParallelKitchenTicker()
    }

    fun selectWeek(week: Int) {
        val coerced = week.coerceIn(1, 24)
        _selectedWeekNumber.value = coerced
        val weekRecipe = SwissmilkAndBaseRecipesCatalog.allRecipes.firstOrNull { it.weekNumber == coerced }
        if (weekRecipe != null) {
            selectRecipe(weekRecipe.id)
        }
    }

    fun selectRecipe(recipeId: String) {
        val recipe = SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == recipeId } ?: return
        _selectedRecipeId.value = recipeId
        _targetServings.value = recipe.baseServings
        _activeCookingStepIndex.value = 0
    }

    fun setTargetServings(servings: Int) {
        _targetServings.value = servings.coerceIn(1, 200)
    }

    fun enterKitchenMode(recipe: RecipeSpec) {
        _selectedRecipeId.value = recipe.id
        _activeCookingStepIndex.value = 0
        _isKitchenModeActive.value = true
    }

    fun exitKitchenMode() {
        _isKitchenModeActive.value = false
    }

    fun setCookingStep(index: Int, maxSteps: Int) {
        _activeCookingStepIndex.value = index.coerceIn(0, (maxSteps - 1).coerceAtLeast(0))
    }

    // 80-Minute Study Block Engine
    fun toggleStudyTimer(weekNumber: Int) {
        val existing = timersMap.value[weekNumber] ?: StudyTimerEntity(
            weekNumber = weekNumber,
            currentPhaseIndex = 0,
            remainingSeconds = STUDY_PHASE_DURATIONS_SEC[0],
            isRunning = false
        )
        val nextRunning = !existing.isRunning
        viewModelScope.launch {
            localRepo.upsertTimer(existing.copy(isRunning = nextRunning, updatedAtEpochMs = System.currentTimeMillis()))
            if (nextRunning) {
                runStudyCountdown(weekNumber)
            } else {
                studyTimerJob?.cancel()
            }
        }
    }

    fun advanceOrResetStudyPhase(weekNumber: Int, resetAll: Boolean = false) {
        studyTimerJob?.cancel()
        val existing = timersMap.value[weekNumber] ?: StudyTimerEntity(weekNumber = weekNumber)
        val nextPhase = if (resetAll) 0 else (existing.currentPhaseIndex + 1) % STUDY_PHASE_DURATIONS_SEC.size
        val completedInc = if (!resetAll && existing.currentPhaseIndex == STUDY_PHASE_DURATIONS_SEC.lastIndex) 1 else 0
        viewModelScope.launch {
            localRepo.upsertTimer(
                existing.copy(
                    currentPhaseIndex = nextPhase,
                    remainingSeconds = STUDY_PHASE_DURATIONS_SEC[nextPhase],
                    isRunning = false,
                    completedBlocksCount = existing.completedBlocksCount + completedInc,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun runStudyCountdown(weekNumber: Int) {
        studyTimerJob?.cancel()
        studyTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = timersMap.value[weekNumber] ?: break
                if (!current.isRunning) break
                if (current.remainingSeconds > 1) {
                    localRepo.upsertTimer(
                        current.copy(
                            remainingSeconds = current.remainingSeconds - 1,
                            updatedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Advance automatically to next phase of the 80-min block
                    val isLast = current.currentPhaseIndex >= STUDY_PHASE_DURATIONS_SEC.lastIndex
                    if (isLast) {
                        localRepo.upsertTimer(
                            current.copy(
                                currentPhaseIndex = 0,
                                remainingSeconds = STUDY_PHASE_DURATIONS_SEC[0],
                                isRunning = false,
                                completedBlocksCount = current.completedBlocksCount + 1,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        _statusBanner.value = "Bloco de Estudo de 80 min da Semana $weekNumber concluído!"
                        break
                    } else {
                        val nextIdx = current.currentPhaseIndex + 1
                        localRepo.upsertTimer(
                            current.copy(
                                currentPhaseIndex = nextIdx,
                                remainingSeconds = STUDY_PHASE_DURATIONS_SEC[nextIdx],
                                isRunning = true,
                                updatedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    // Parallel Kitchen Timers (e.g. Berner Platte 120m / 90m / 60m / 20m)
    fun addOrRestartParallelTimer(id: String, label: String, durationMinutes: Int) {
        val totalSec = (durationMinutes.coerceAtLeast(1)) * 60
        _parallelTimers.update { current ->
            val filtered = current.filterNot { it.id == id }
            filtered + ParallelKitchenTimer(
                id = id,
                label = label,
                totalSeconds = totalSec,
                remainingSeconds = totalSec,
                isRunning = true
            )
        }
    }

    fun toggleParallelTimer(id: String) {
        _parallelTimers.update { list ->
            list.map { if (it.id == id) it.copy(isRunning = !it.isRunning) else it }
        }
    }

    fun removeParallelTimer(id: String) {
        _parallelTimers.update { list -> list.filterNot { it.id == id } }
    }

    private fun startParallelKitchenTicker() {
        parallelTimersJob?.cancel()
        parallelTimersJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _parallelTimers.update { list ->
                    list.map { timer ->
                        if (timer.isRunning && timer.remainingSeconds > 0) {
                            val next = timer.remainingSeconds - 1
                            if (next == 0) {
                                _statusBanner.value = "CRONÔMETRO CONCLUÍDO: ${timer.label}"
                                timer.copy(remainingSeconds = 0, isRunning = false)
                            } else {
                                timer.copy(remainingSeconds = next)
                            }
                        } else {
                            timer
                        }
                    }
                }
            }
        }
    }

    // SM-2 Flashcard Grading
    fun gradeFlashcard(cardId: String, grade0To5: Int) {
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        val prev = sm2Map.value[cardId]
        val next = CulinaryCalculators.computeNextSm2State(prev, cardId, grade0To5, todayEpochDay)
        viewModelScope.launch {
            localRepo.upsertSm2(next)
        }
    }

    fun dueFlashcardsCount(): Int {
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        val map = sm2Map.value
        return TechnicalAtlasCatalog.flashcards.count { card ->
            val state = map[card.id]
            state == null || state.nextReviewEpochDay <= todayEpochDay
        }
    }

    // Save 8-Block Digital MEP Sheet
    fun saveMepSheet(sheet: MepSheetEntity) {
        viewModelScope.launch {
            localRepo.upsertMepSheet(sheet.copy(updatedAtEpochMs = System.currentTimeMillis()))
            _statusBanner.value = "Ficha MEP salva localmente (8 blocos)."
        }
    }

    fun preparePhotoTransferFromMep(recipe: RecipeSpec, mepSheet: MepSheetEntity?) {
        val summary = buildString {
            append("Porções: ${_targetServings.value} (FC ${"%.2f".format(_targetServings.value.toDouble() / recipe.baseServings)})")
            append(" | PCC: ${mepSheet?.block5ThermalFlowPcc?.ifBlank { recipe.pccTargetSummary } ?: recipe.pccTargetSummary}")
            if (!mepSheet?.block8Selbstkontrolle.isNullOrBlank()) {
                append(" | Reflexão: ${mepSheet?.block8Selbstkontrolle}")
            }
        }
        _pendingPhotoTransfer.value = PendingMepPhotoTransfer(
            weekNumber = recipe.weekNumber,
            cycleNumber = recipe.cycleNumber,
            axisId = recipe.axisId,
            recipeId = recipe.id,
            recipeTitle = recipe.title,
            isSwissmilkClassic = recipe.isSwissmilkClassic,
            isChefkochV4 = recipe.isChefkochV4,
            cantonCode = recipe.cantonCode,
            mepSummary = summary.take(1200)
        )
    }

    fun consumePhotoTransfer() {
        _pendingPhotoTransfer.value = null
    }

    // Consolidated Weekly Shopping List (PB = PL * FC + Conversion Factor)
    fun generateShoppingForWeek(weekNumber: Int, portionsMultiplier: Double = 1.0) {
        val recipesOfWeek = SwissmilkAndBaseRecipesCatalog.allRecipes.filter { it.weekNumber == weekNumber }
        val aggregated = mutableMapOf<String, ShoppingFifoEntity>()

        recipesOfWeek.forEach { recipe ->
            val scale = portionsMultiplier.coerceIn(0.25, 20.0)
            recipe.ingredients.forEach { ing ->
                val key = "${ing.sector}|${ing.name.lowercase()}|${ing.unit}"
                val pl = ing.netAmountPl * scale
                val pb = pl * ing.correctionFactorFc
                val prev = aggregated[key]
                if (prev == null) {
                    aggregated[key] = ShoppingFifoEntity(
                        weekNumber = weekNumber,
                        ingredientName = ing.name,
                        sector = ing.sector,
                        unit = ing.unit,
                        netQuantityPl = pl,
                        correctionFactorFc = ing.correctionFactorFc,
                        grossQuantityPb = pb,
                        batchDate = "Lote Sem. $weekNumber",
                        expiryDate = if (ing.sector == "Açougue & Peixaria") "+2 dias (≤ 4 °C)" else "+7 dias (FIFO)"
                    )
                } else {
                    aggregated[key] = prev.copy(
                        netQuantityPl = prev.netQuantityPl + pl,
                        grossQuantityPb = prev.grossQuantityPb + pb
                    )
                }
            }
        }

        viewModelScope.launch {
            localRepo.replaceWeekShopping(weekNumber, aggregated.values.toList())
            _statusBanner.value = "Compras da Semana $weekNumber consolidadas (PB = PL × FC + FIFO)."
        }
    }

    fun toggleShoppingPurchased(item: ShoppingFifoEntity) {
        viewModelScope.launch {
            localRepo.upsertShoppingItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun updateShoppingFifoDates(item: ShoppingFifoEntity, batchDate: String, expiryDate: String) {
        viewModelScope.launch {
            localRepo.upsertShoppingItem(item.copy(batchDate = batchDate, expiryDate = expiryDate))
        }
    }

    // Equipment Inventory Toggle
    fun toggleEquipmentOwned(equipmentId: String) {
        val currentlyOwned = equipmentId in ownedEquipmentIds.value
        viewModelScope.launch {
            localRepo.upsertEquipment(EquipmentOwnedEntity(equipmentId = equipmentId, isOwned = !currentlyOwned))
        }
    }

    // Selbstkontrolle Save
    fun saveSelfEvaluation(eval: SelfEvalEntity) {
        viewModelScope.launch {
            localRepo.upsertSelfEvaluation(eval.copy(updatedAtEpochMs = System.currentTimeMillis()))
            _statusBanner.value = "Selbstkontrolle da Semana ${eval.weekNumber} registrada."
        }
    }

    suspend fun buildBackupJson(): String = localRepo.exportBackupJson()

    fun restoreFromBackupJson(json: String) {
        viewModelScope.launch {
            localRepo.importBackupJson(json)
                .onSuccess { count ->
                    _statusBanner.value = "Backup JSON restaurado com sucesso ($count registros)."
                }
                .onFailure { err ->
                    _statusBanner.value = "Erro ao importar JSON: ${err.localizedMessage}"
                }
        }
    }

    suspend fun buildLernreflexionMarkdown(): String = localRepo.exportDossierLernreflexionMarkdown()

    fun clearBanner() {
        _statusBanner.value = null
    }
}
