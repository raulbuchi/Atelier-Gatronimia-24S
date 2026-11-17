package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.EquipmentOwnedEntity
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity
import com.example.data.local.SelfEvalEntity
import com.example.data.local.ShoppingFifoEntity
import com.example.data.local.Sm2ProgressEntity
import com.example.data.local.StudyTimerEntity
import com.example.data.remote.MepSheetCloudLink
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.calculators.MepFinancialAnalyticsEngine
import com.example.domain.calculators.WeeklyMepCostAnalytics
import com.example.domain.model.RecipeSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val mepSummary: String,
    val cloudImageLink: String = "",
    val cloudMediaId: String = "",
    val photoThumbnailBase64: String = "",
    val localPhotoUri: String = "",
    val executionScore1To5: Int = 0,
    val block8Selbstkontrolle: String = ""
)

data class ChefSosRequest(
    val weekNumber: Int,
    val contextSummary: String,
    val prefilledPrompt: String
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

        fun defaultUnitCostForSector(sector: String, unit: String): Double {
            if (unit.trim().lowercase() in setOf("un", "unid", "unid.")) return 0.75
            return when {
                sector.contains("Açougue", ignoreCase = true) || sector.contains("Peixaria", ignoreCase = true) -> 42.0
                sector.contains("Laticínios", ignoreCase = true) -> 18.5
                sector.contains("Especiarias", ignoreCase = true) || sector.contains("Fundos", ignoreCase = true) -> 24.0
                sector.contains("Hortifrúti", ignoreCase = true) -> 8.5
                else -> 11.0
            }
        }
    }

    private val initialRecipeId = SwissmilkAndBaseRecipesCatalog.allRecipes.firstOrNull()?.id.orEmpty()

    private val _selectedWeekNumber = MutableStateFlow(1)
    val selectedWeekNumber: StateFlow<Int> = _selectedWeekNumber.asStateFlow()

    private val _selectedRecipeId = MutableStateFlow(initialRecipeId)
    val selectedRecipeId: StateFlow<String> = _selectedRecipeId.asStateFlow()

    private val _activeMepSheetId = MutableStateFlow(initialRecipeId)
    val activeMepSheetId: StateFlow<String> = _activeMepSheetId.asStateFlow()

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

    private val _pendingChefSos = MutableStateFlow<ChefSosRequest?>(null)
    val pendingChefSos: StateFlow<ChefSosRequest?> = _pendingChefSos.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedLibraryAxis = MutableStateFlow(1)
    val selectedLibraryAxis: StateFlow<Int> = _selectedLibraryAxis.asStateFlow()

    private val _requestedAtlasSubTab = MutableStateFlow<Int?>(null)
    val requestedAtlasSubTab: StateFlow<Int?> = _requestedAtlasSubTab.asStateFlow()

    private val _sm2AxisFilter = MutableStateFlow(0)
    val sm2AxisFilter: StateFlow<Int> = _sm2AxisFilter.asStateFlow()

    val timersMap: StateFlow<Map<Int, StudyTimerEntity>> = localRepo.allTimers
        .map { list -> list.associateBy { it.weekNumber } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val sm2Map: StateFlow<Map<String, Sm2ProgressEntity>> = localRepo.sm2Progress
        .map { list -> list.associateBy { it.cardId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val persistedMepSheetsList: StateFlow<List<MepSheetEntity>> = localRepo.mepSheets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mepSheetsMap: StateFlow<Map<String, MepSheetEntity>> = localRepo.mepSheets
        .map { list ->
            val map = list.associateBy { it.recipeId }.toMutableMap()
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

    val mepLinesByRecipeMap: StateFlow<Map<String, List<MepIngredientLineEntity>>> = localRepo.mepLines
        .map { list -> list.groupBy { it.recipeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val weeklyMepCostAnalytics: StateFlow<WeeklyMepCostAnalytics> = combine(
        localRepo.mepSheets,
        localRepo.mepLines
    ) { sheets, allLines ->
        val linesMap = allLines.groupBy { it.recipeId }
        MepFinancialAnalyticsEngine.computeWeeklyEvolution(
            persistedSheets = sheets,
            linesByRecipeId = linesMap,
            fallbackLinesProvider = ::buildCatalogFallbackLines,
            referenceFallbackSheet = CurriculumWeeksAndGuides.exampleMepZurcherRosti
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        MepFinancialAnalyticsEngine.computeWeeklyEvolution(
            persistedSheets = emptyList(),
            linesByRecipeId = emptyMap(),
            fallbackLinesProvider = ::buildCatalogFallbackLines,
            referenceFallbackSheet = CurriculumWeeksAndGuides.exampleMepZurcherRosti
        )
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
        val weekRecipe = SwissmilkAndBaseRecipesCatalog.recipesForWeek(coerced).firstOrNull()
        if (weekRecipe != null) {
            selectRecipe(weekRecipe.id)
        }
    }

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

    fun selectRecipe(recipeId: String) {
        val recipe = SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == recipeId } ?: return
        _selectedRecipeId.value = recipeId
        _activeMepSheetId.value = recipeId
        val savedServings = mepSheetsMap.value[recipeId]?.targetServings
        _targetServings.value = savedServings ?: recipe.baseServings
        _activeCookingStepIndex.value = 0
    }

    fun selectMepSheet(sheetId: String) {
        val catalogMatch = SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == sheetId }
        if (catalogMatch != null) {
            selectRecipe(catalogMatch.id)
        } else {
            _activeMepSheetId.value = sheetId
            mepSheetsMap.value[sheetId]?.let { saved ->
                _targetServings.value = saved.targetServings
            }
        }
    }

    fun createCustomMepSheet() {
        val customId = "custom_mep_${System.currentTimeMillis()}"
        val week = _selectedWeekNumber.value
        val newSheet = MepSheetEntity(
            recipeId = customId,
            title = "Ficha Autoral S${"%02d".format(week)}",
            weekNumber = week,
            stationBrigade = "Saucier",
            baseServings = 4,
            targetServings = 4,
            portionWeightGrams = 220.0,
            cookingShrinkagePercent = 12.0,
            targetCmvPercent = 28.0,
            directLaborCost = 8.5,
            pccTargetTempC = 70,
            plateTempC = 55,
            allergensCsv = "Lactose",
            actualBenchYieldGrams = 0.0,
            executionScore1To5 = 4,
            isCustomSheet = true,
            block1Identification = "Ficha Autoral Semana $week | Praça Saucier | Base 4 porções",
            block2IngredientsPbPlFc = "Proteína principal PL 600g × FC 1.12 | Guarnição vegetal PL 400g × FC 1.18",
            block3CookwareBlades = "Caçarola Sautoir, frigideira antiaderente profissional, Faca do Chef afiada",
            block4PrePrepCuts = "Brunoise (~1,5 mm) aromática; Jardinière (~5 mm) de legumes em frigideira antiaderente.",
            block5ThermalFlowPcc = "Selagem rápida a 180 °C em lotes reduzidos; redução em ponto nappé a 82 °C; centro térmico PCC ≥ 70 °C.",
            block6PlatingFinishing = "Prato aquecido a 55 °C, bordas limpas, proteína espelhada sem umedecer o elemento crocante.",
            block7HygieneAllergens = "Cadeia fria ≤ 4 °C; higienização + antissepsia das mãos (RDC 216); declaração de alergênicos revisada.",
            block8Selbstkontrolle = ""
        )
        val starterLines = listOf(
            MepIngredientLineEntity(
                recipeId = customId,
                sortOrder = 0,
                name = "Proteína Principal Limpa",
                sector = "Açougue & Peixaria",
                unit = "g",
                baseNetPl = 600.0,
                trimLossPercent = 10.7,
                correctionFactorFc = 1.12,
                unitCostPerKgOrL = 46.0
            ),
            MepIngredientLineEntity(
                recipeId = customId,
                sortOrder = 1,
                name = "Guarnição Vegetal Padronizada",
                sector = "Hortifrúti",
                unit = "g",
                baseNetPl = 400.0,
                trimLossPercent = 15.3,
                correctionFactorFc = 1.18,
                unitCostPerKgOrL = 9.5
            ),
            MepIngredientLineEntity(
                recipeId = customId,
                sortOrder = 2,
                name = "Manteiga Clarificada / Fundo Reduzido",
                sector = "Laticínios & Ovos",
                unit = "g",
                baseNetPl = 140.0,
                trimLossPercent = 0.0,
                correctionFactorFc = 1.0,
                unitCostPerKgOrL = 24.0
            )
        )
        viewModelScope.launch {
            localRepo.saveMepComplete(newSheet, starterLines)
            _activeMepSheetId.value = customId
            _targetServings.value = 4
            _statusBanner.value = "Nova Ficha Técnica autoral criada no acervo local."
        }
    }

    fun resolveSeedLinesForSheet(sheetId: String): List<MepIngredientLineEntity> {
        val persisted = mepLinesByRecipeMap.value[sheetId]
        if (!persisted.isNullOrEmpty()) return persisted
        return buildCatalogFallbackLines(sheetId)
    }

    private fun buildCatalogFallbackLines(sheetId: String): List<MepIngredientLineEntity> {
        val catalogRecipe = SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == sheetId }
        if (catalogRecipe != null) {
            return catalogRecipe.ingredients.mapIndexed { idx, ing ->
                val fc = ing.correctionFactorFc.coerceAtLeast(1.0)
                val trimPct = if (fc > 1.0) ((1.0 - (1.0 / fc)) * 100.0) else 0.0
                MepIngredientLineEntity(
                    recipeId = sheetId,
                    sortOrder = idx,
                    name = ing.name,
                    sector = ing.sector,
                    unit = ing.unit,
                    baseNetPl = ing.netAmountPl,
                    trimLossPercent = trimPct,
                    correctionFactorFc = fc,
                    unitCostPerKgOrL = defaultUnitCostForSector(ing.sector, ing.unit)
                )
            }
        }
        return emptyList()
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

    // Save 8-Block Digital MEP Sheet + Structured Ingredient Lines in Room
    fun saveMepComplete(sheet: MepSheetEntity, lines: List<MepIngredientLineEntity>) {
        val calc = CulinaryCalculators.calculateMepSheetSummary(
            baseServings = sheet.baseServings,
            targetServings = sheet.targetServings,
            cookingShrinkagePercent = sheet.cookingShrinkagePercent,
            targetCmvPercent = sheet.targetCmvPercent,
            directLaborPerPortionChf = sheet.directLaborCost,
            actualBenchYieldGrams = sheet.actualBenchYieldGrams,
            ingredientLines = lines
        )
        val generatedBlock2 = calc.lines.joinToString(" | ") { l ->
            "${l.source.name}: PL ${"%.0f".format(l.scaledPl)}${l.source.unit} × FC ${"%.2f".format(l.effectiveFc)} = PB ${"%.0f".format(l.scaledPb)}${l.source.unit}"
        }
        val stamped = sheet.copy(
            block2IngredientsPbPlFc = generatedBlock2.ifBlank { sheet.block2IngredientsPbPlFc },
            updatedAtEpochMs = System.currentTimeMillis()
        )
        _targetServings.value = stamped.targetServings
        viewModelScope.launch {
            localRepo.saveMepComplete(stamped, lines)
            val label = stamped.title.ifBlank { stamped.recipeId }
            _statusBanner.value = "Ficha Técnica \"$label\" salva localmente (${lines.size} insumos • 8 blocos)."
        }
    }

    fun saveMepSheet(sheet: MepSheetEntity) {
        val lines = resolveSeedLinesForSheet(sheet.recipeId)
        saveMepComplete(sheet, lines)
    }

    fun deleteMepSheet(sheetId: String) {
        viewModelScope.launch {
            localRepo.deleteMepComplete(sheetId)
            if (_activeMepSheetId.value == sheetId) {
                _activeMepSheetId.value = _selectedRecipeId.value
            }
            _statusBanner.value = "Ficha Técnica removida do acervo local."
        }
    }

    fun preparePhotoTransferFromMep(recipe: RecipeSpec?, mepSheet: MepSheetEntity) {
        val basePax = mepSheet.baseServings.coerceAtLeast(1)
        val scale = mepSheet.targetServings.toDouble() / basePax
        val summary = buildString {
            append("Praça: ${mepSheet.stationBrigade} | Porções: ${mepSheet.targetServings} (FC ${"%.2f".format(scale)}x)")
            append(" | PCC ≥ ${mepSheet.pccTargetTempC} °C: ${mepSheet.block5ThermalFlowPcc.ifBlank { recipe?.pccTargetSummary.orEmpty() }}")
            if (mepSheet.allergensCsv.isNotBlank()) {
                append(" | Alergênicos: ${mepSheet.allergensCsv}")
            }
            if (mepSheet.block8Selbstkontrolle.isNotBlank()) {
                append(" | Reflexão (${mepSheet.executionScore1To5}/5): ${mepSheet.block8Selbstkontrolle}")
            }
            if (mepSheet.cloudImageLink.isNotBlank()) {
                append(" | Foto Firestore: ${mepSheet.cloudImageLink}")
            }
        }
        _pendingPhotoTransfer.value = PendingMepPhotoTransfer(
            weekNumber = mepSheet.weekNumber,
            cycleNumber = recipe?.cycleNumber ?: (((mepSheet.weekNumber - 1) / 6) + 1),
            axisId = recipe?.axisId ?: 6,
            recipeId = mepSheet.recipeId,
            recipeTitle = mepSheet.title.ifBlank { recipe?.title ?: "Ficha MEP Autoral" },
            isSwissmilkClassic = recipe?.isSwissmilkClassic ?: false,
            isChefkochV4 = recipe?.isChefkochV4 ?: false,
            cantonCode = recipe?.cantonCode.orEmpty(),
            mepSummary = summary.take(1200),
            cloudImageLink = mepSheet.cloudImageLink,
            cloudMediaId = mepSheet.cloudMediaId,
            photoThumbnailBase64 = mepSheet.photoThumbnailBase64,
            localPhotoUri = mepSheet.localPhotoUri,
            executionScore1To5 = mepSheet.executionScore1To5,
            block8Selbstkontrolle = mepSheet.block8Selbstkontrolle
        )
    }

    fun attachCreationPhotoToMepSheet(
        sheet: MepSheetEntity,
        lines: List<MepIngredientLineEntity>,
        localPhotoUri: String,
        cloudLink: MepSheetCloudLink
    ) {
        val calc = CulinaryCalculators.calculateMepSheetSummary(
            baseServings = sheet.baseServings,
            targetServings = sheet.targetServings,
            cookingShrinkagePercent = sheet.cookingShrinkagePercent,
            targetCmvPercent = sheet.targetCmvPercent,
            directLaborPerPortionChf = sheet.directLaborCost,
            actualBenchYieldGrams = sheet.actualBenchYieldGrams,
            ingredientLines = lines
        )
        val generatedBlock2 = calc.lines.joinToString(" | ") { l ->
            "${l.source.name}: PL ${"%.0f".format(l.scaledPl)}${l.source.unit} × FC ${"%.2f".format(l.effectiveFc)} = PB ${"%.0f".format(l.scaledPb)}${l.source.unit}"
        }
        val updated = sheet.copy(
            block2IngredientsPbPlFc = generatedBlock2.ifBlank { sheet.block2IngredientsPbPlFc },
            localPhotoUri = localPhotoUri,
            cloudImageLink = cloudLink.imageLink,
            cloudMediaId = cloudLink.mediaId,
            photoThumbnailBase64 = cloudLink.thumbnailBase64,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            localRepo.saveMepComplete(updated, lines)
        }
    }

    fun syncCloudMepLinksIntoLocal(cloudLinks: List<MepSheetCloudLink>) {
        if (cloudLinks.isEmpty()) return
        viewModelScope.launch {
            val currentMap = mepSheetsMap.value
            cloudLinks.forEach { remote ->
                val existing = currentMap[remote.recipeId]
                if (existing != null && existing.cloudImageLink != remote.imageLink) {
                    val merged = existing.copy(
                        cloudImageLink = remote.imageLink,
                        cloudMediaId = remote.mediaId,
                        photoThumbnailBase64 = remote.thumbnailBase64.ifBlank { existing.photoThumbnailBase64 },
                        localPhotoUri = remote.localPhotoUri ?: existing.localPhotoUri,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                    val lines = resolveSeedLinesForSheet(remote.recipeId)
                    localRepo.saveMepComplete(merged, lines)
                }
            }
        }
    }

    fun consumePhotoTransfer() {
        _pendingPhotoTransfer.value = null
    }

    fun requestChefSos(weekNumber: Int, contextSummary: String, prefilledPrompt: String) {
        _pendingChefSos.value = ChefSosRequest(
            weekNumber = weekNumber,
            contextSummary = contextSummary,
            prefilledPrompt = prefilledPrompt
        )
    }

    fun consumeChefSos() {
        _pendingChefSos.value = null
    }

    // Consolidated Weekly Shopping List (PB = PL * FC + Conversion Factor)
    fun generateShoppingForWeek(weekNumber: Int, portionsMultiplier: Double = 1.0) {
        val recipesOfWeek = SwissmilkAndBaseRecipesCatalog.recipesForWeek(weekNumber)
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
                        expiryDate = if (ing.sector == "Açougue & Peixaria") "+2 dias (≤ 4 °C)" else "+7 dias (PVPS/PEPS)"
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
            _statusBanner.value = "Requisição de compras da Semana $weekNumber gerada (PB = PL × FC • PVPS/PEPS)."
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

    // Autoavaliação de Bancada Save
    fun saveSelfEvaluation(eval: SelfEvalEntity) {
        viewModelScope.launch {
            localRepo.upsertSelfEvaluation(eval.copy(updatedAtEpochMs = System.currentTimeMillis()))
            _statusBanner.value = "Autoavaliação de Bancada da Semana ${eval.weekNumber} registrada."
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
