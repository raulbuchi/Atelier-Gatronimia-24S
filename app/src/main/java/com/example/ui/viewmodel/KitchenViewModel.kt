package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.core.timers.TimerCoordinator
import com.example.core.timers.TimerMath
import com.example.core.timers.timerUiClock
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity
import com.example.data.remote.MepSheetCloudLink
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.calculators.MepFinancialAnalyticsEngine
import com.example.domain.calculators.WeeklyMepCostAnalytics
import com.example.domain.model.RecipeSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/**
 * Receitas e bancada: receita/porções selecionadas, Modo Cozinha, cronômetros paralelos,
 * Fichas MEP (8 blocos) com custos e a ponte para foto no portfólio / Chef IA.
 * Telas: Receitas & MEP (e partes da Gestão e da Comunidade que usam fichas).
 */
class KitchenViewModel(
    private val localRepo: LocalKitchenRepository,
    private val session: CourseSession,
    private val timerCoordinator: TimerCoordinator,
    private val messages: UserMessages
) : ViewModel() {

    companion object {
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

    val selectedWeekNumber: StateFlow<Int> = session.selectedWeek

    private val initialRecipeId = SwissmilkAndBaseRecipesCatalog.allRecipes.firstOrNull()?.id.orEmpty()

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

    private val _pendingPhotoTransfer = MutableStateFlow<PendingMepPhotoTransfer?>(null)
    val pendingPhotoTransfer: StateFlow<PendingMepPhotoTransfer?> = _pendingPhotoTransfer.asStateFlow()

    private val _pendingChefSos = MutableStateFlow<ChefSosRequest?>(null)
    val pendingChefSos: StateFlow<ChefSosRequest?> = _pendingChefSos.asStateFlow()

    val ownedEquipmentIds: StateFlow<Set<String>> = localRepo.ownedEquipment
        .map { list -> list.filter { it.isOwned }.map { it.equipmentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    // ---------- Fichas MEP ----------

    val persistedMepSheetsList: StateFlow<List<MepSheetEntity>> = localRepo.mepSheets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mepSheetsMap: StateFlow<Map<String, MepSheetEntity>> = localRepo.mepSheets
        .map { list ->
            val map = list.associateBy { it.recipeId }.toMutableMap()
            val ex = CurriculumWeeksAndGuides.exampleMepZurcherRosti
            if (!map.containsKey(ex.recipeId)) map[ex.recipeId] = ex
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

    val weeklyMepCostAnalytics: StateFlow<WeeklyMepCostAnalytics> =
        combine(localRepo.mepSheets, localRepo.mepLines) { sheets, allLines ->
            MepFinancialAnalyticsEngine.computeWeeklyEvolution(
                persistedSheets = sheets,
                linesByRecipeId = allLines.groupBy { it.recipeId },
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

    // ---------- Cronômetros paralelos ----------

    private var timerSyncJob: Job? = null

    val parallelTimers: StateFlow<List<ParallelKitchenTimer>> =
        combine(localRepo.kitchenTimers, timerUiClock()) { list, now ->
            var anyDue = false
            val display = list.map { stored ->
                val resolved = TimerMath.resolveKitchen(stored, now)
                if (resolved.changed) anyDue = true
                val k = resolved.value
                ParallelKitchenTimer(
                    id = k.id,
                    label = k.label,
                    totalSeconds = k.totalSeconds,
                    remainingSeconds = TimerMath.kitchenRemainingSeconds(k, now),
                    isRunning = k.isRunning
                )
            }
            if (anyDue) requestTimerSync()
            display
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        // Ao trocar de semana (em qualquer aba), a bancada abre a primeira receita prática dela.
        viewModelScope.launch {
            session.selectedWeek.collect { week ->
                SwissmilkAndBaseRecipesCatalog.recipesForWeek(week).firstOrNull()?.let { selectRecipe(it.id) }
            }
        }
    }

    // ---------- Receita e Modo Cozinha ----------

    fun selectRecipe(recipeId: String) {
        val recipe = SwissmilkAndBaseRecipesCatalog.findById(recipeId) ?: return
        _selectedRecipeId.value = recipeId
        _activeMepSheetId.value = recipeId
        _targetServings.value = mepSheetsMap.value[recipeId]?.targetServings ?: recipe.baseServings
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

    fun addOrRestartParallelTimer(id: String, label: String, durationMinutes: Int) {
        viewModelScope.launch {
            timerCoordinator.updateKitchen(id) { _, now -> TimerMath.startKitchen(id, label, durationMinutes, now) }
        }
    }

    fun toggleParallelTimer(id: String) {
        viewModelScope.launch {
            timerCoordinator.updateKitchen(id) { k, now -> k?.let { TimerMath.toggleKitchen(it, now) } }
        }
    }

    fun removeParallelTimer(id: String) {
        viewModelScope.launch { timerCoordinator.updateKitchen(id) { _, _ -> null } }
    }

    private fun requestTimerSync() {
        if (timerSyncJob?.isActive == true) return
        timerSyncJob = viewModelScope.launch { timerCoordinator.sync() }
    }

    // ---------- Fichas MEP: seleção, criação, salvamento ----------

    fun selectMepSheet(sheetId: String) {
        if (SwissmilkAndBaseRecipesCatalog.findById(sheetId) != null) {
            selectRecipe(sheetId)
        } else {
            _activeMepSheetId.value = sheetId
            mepSheetsMap.value[sheetId]?.let { _targetServings.value = it.targetServings }
        }
    }

    fun createCustomMepSheet() {
        val customId = "custom_mep_${System.currentTimeMillis()}"
        val week = session.selectedWeek.value
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
                recipeId = customId, sortOrder = 0, name = "Proteína Principal Limpa",
                sector = "Açougue & Peixaria", unit = "g", baseNetPl = 600.0,
                trimLossPercent = 10.7, correctionFactorFc = 1.12, unitCostPerKgOrL = 46.0
            ),
            MepIngredientLineEntity(
                recipeId = customId, sortOrder = 1, name = "Guarnição Vegetal Padronizada",
                sector = "Hortifrúti", unit = "g", baseNetPl = 400.0,
                trimLossPercent = 15.3, correctionFactorFc = 1.18, unitCostPerKgOrL = 9.5
            ),
            MepIngredientLineEntity(
                recipeId = customId, sortOrder = 2, name = "Manteiga Clarificada / Fundo Reduzido",
                sector = "Laticínios & Ovos", unit = "g", baseNetPl = 140.0,
                trimLossPercent = 0.0, correctionFactorFc = 1.0, unitCostPerKgOrL = 24.0
            )
        )
        viewModelScope.launch {
            localRepo.saveMepComplete(newSheet, starterLines)
            _activeMepSheetId.value = customId
            _targetServings.value = 4
            messages.show("Nova Ficha Técnica autoral criada no acervo local.")
        }
    }

    fun resolveSeedLinesForSheet(sheetId: String): List<MepIngredientLineEntity> {
        val persisted = mepLinesByRecipeMap.value[sheetId]
        if (!persisted.isNullOrEmpty()) return persisted
        return buildCatalogFallbackLines(sheetId)
    }

    private fun buildCatalogFallbackLines(sheetId: String): List<MepIngredientLineEntity> {
        val catalogRecipe = SwissmilkAndBaseRecipesCatalog.findById(sheetId) ?: return emptyList()
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

    /** Recalcula o Bloco 2 (PB = PL × FC) a partir das linhas estruturadas. */
    private fun withGeneratedBlock2(sheet: MepSheetEntity, lines: List<MepIngredientLineEntity>): MepSheetEntity {
        val calc = CulinaryCalculators.calculateMepSheetSummary(
            baseServings = sheet.baseServings,
            targetServings = sheet.targetServings,
            cookingShrinkagePercent = sheet.cookingShrinkagePercent,
            targetCmvPercent = sheet.targetCmvPercent,
            directLaborPerPortionBrl = sheet.directLaborCost,
            actualBenchYieldGrams = sheet.actualBenchYieldGrams,
            ingredientLines = lines
        )
        val generatedBlock2 = calc.lines.joinToString(" | ") { l ->
            "${l.source.name}: PL ${"%.0f".format(l.scaledPl)}${l.source.unit} × FC ${"%.2f".format(l.effectiveFc)} = PB ${"%.0f".format(l.scaledPb)}${l.source.unit}"
        }
        return sheet.copy(
            block2IngredientsPbPlFc = generatedBlock2.ifBlank { sheet.block2IngredientsPbPlFc },
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    fun saveMepComplete(sheet: MepSheetEntity, lines: List<MepIngredientLineEntity>) {
        val stamped = withGeneratedBlock2(sheet, lines)
        _targetServings.value = stamped.targetServings
        viewModelScope.launch {
            localRepo.saveMepComplete(stamped, lines)
            val label = stamped.title.ifBlank { stamped.recipeId }
            messages.show("Ficha Técnica \"$label\" salva localmente (${lines.size} insumos • 8 blocos).")
        }
    }

    fun deleteMepSheet(sheetId: String) {
        viewModelScope.launch {
            localRepo.deleteMepComplete(sheetId)
            if (_activeMepSheetId.value == sheetId) {
                _activeMepSheetId.value = _selectedRecipeId.value
            }
            messages.show("Ficha Técnica removida do acervo local.")
        }
    }

    // ---------- Portfólio, nuvem e Chef IA ----------

    fun preparePhotoTransferFromMep(recipe: RecipeSpec?, mepSheet: MepSheetEntity) {
        val basePax = mepSheet.baseServings.coerceAtLeast(1)
        val scale = mepSheet.targetServings.toDouble() / basePax
        val summary = buildString {
            append("Praça: ${mepSheet.stationBrigade} | Porções: ${mepSheet.targetServings} (FC ${"%.2f".format(scale)}x)")
            append(" | PCC ≥ ${mepSheet.pccTargetTempC} °C: ${mepSheet.block5ThermalFlowPcc.ifBlank { recipe?.pccTargetSummary.orEmpty() }}")
            if (mepSheet.allergensCsv.isNotBlank()) append(" | Alergênicos: ${mepSheet.allergensCsv}")
            if (mepSheet.block8Selbstkontrolle.isNotBlank()) {
                append(" | Reflexão (${mepSheet.executionScore1To5}/5): ${mepSheet.block8Selbstkontrolle}")
            }
            if (mepSheet.cloudImageLink.isNotBlank()) append(" | Foto Firestore: ${mepSheet.cloudImageLink}")
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
        val updated = withGeneratedBlock2(sheet, lines).copy(
            localPhotoUri = localPhotoUri,
            cloudImageLink = cloudLink.imageLink,
            cloudMediaId = cloudLink.mediaId,
            photoThumbnailBase64 = cloudLink.thumbnailBase64
        )
        viewModelScope.launch { localRepo.saveMepComplete(updated, lines) }
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
                    localRepo.saveMepComplete(merged, resolveSeedLinesForSheet(remote.recipeId))
                }
            }
        }
    }

    fun consumePhotoTransfer() {
        _pendingPhotoTransfer.value = null
    }

    fun requestChefSos(weekNumber: Int, contextSummary: String, prefilledPrompt: String) {
        _pendingChefSos.value = ChefSosRequest(weekNumber, contextSummary, prefilledPrompt)
    }

    fun consumeChefSos() {
        _pendingChefSos.value = null
    }
}
