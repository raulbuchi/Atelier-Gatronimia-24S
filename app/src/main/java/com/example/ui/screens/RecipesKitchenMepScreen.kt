package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.domain.model.RecipeSpec
import com.example.ui.components.KeepScreenOnEffect
import com.example.ui.components.MepWeeklyCostChartCard
import com.example.ui.components.SmartKitchenAlertBanner
import com.example.ui.components.TagPill
import com.example.ui.components.formatSecondsMmSs
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.ParallelKitchenTimer
import com.example.ui.viewmodel.StreamUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipesKitchenMepScreen(
    atelierVm: AtelierViewModel,
    communityVm: CommunityAiViewModel,
    onNavigateToPhotoRegister: () -> Unit,
    onNavigateToChefSos: () -> Unit = {}
) {
    val selectedRecipeId by atelierVm.selectedRecipeId.collectAsStateWithLifecycle()
    val activeMepSheetId by atelierVm.activeMepSheetId.collectAsStateWithLifecycle()
    val targetServings by atelierVm.targetServings.collectAsStateWithLifecycle()
    val isKitchenMode by atelierVm.isKitchenModeActive.collectAsStateWithLifecycle()
    val activeStepIdx by atelierVm.activeCookingStepIndex.collectAsStateWithLifecycle()
    val parallelTimers by atelierVm.parallelTimers.collectAsStateWithLifecycle()
    val ownedEquipmentIds by atelierVm.ownedEquipmentIds.collectAsStateWithLifecycle()
    val mepSheetsMap by atelierVm.mepSheetsMap.collectAsStateWithLifecycle()
    val persistedMepSheets by atelierVm.persistedMepSheetsList.collectAsStateWithLifecycle()
    val mepLinesMap by atelierVm.mepLinesByRecipeMap.collectAsStateWithLifecycle()
    val costAnalytics by atelierVm.weeklyMepCostAnalytics.collectAsStateWithLifecycle()
    val myMepLinksState by communityVm.myMepLinksState.collectAsStateWithLifecycle()
    val isSyncingMepPhoto by communityVm.isSyncingMepPhoto.collectAsStateWithLifecycle()
    val fullMediaCache by communityVm.loadedFullMediaCache.collectAsStateWithLifecycle()

    var filterMode by rememberSaveable { mutableIntStateOf(0) } // 0 Todas, 1 Passo a Passo (10), 2 Swissmilk (23), 3 Base & Mundo (12)
    var filterCanton by rememberSaveable { mutableStateOf("TODOS") }
    var filterWeek by rememberSaveable { mutableIntStateOf(0) } // 0 = Todas Semanas
    var subViewTab by rememberSaveable { mutableIntStateOf(0) } // 0 Receita & Escala, 1 Calculadora Ficha MEP 8 Blocos

    val activeRecipe = remember(selectedRecipeId) {
        SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == selectedRecipeId }
            ?: SwissmilkAndBaseRecipesCatalog.allRecipes.first()
    }

    val catalogRecipeForMep = remember(activeMepSheetId) {
        SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == activeMepSheetId }
    }

    val initialLinesForActiveMep = remember(activeMepSheetId, mepLinesMap) {
        mepLinesMap[activeMepSheetId] ?: atelierVm.resolveSeedLinesForSheet(activeMepSheetId)
    }

    if (isKitchenMode) {
        BackHandler { atelierVm.exitKitchenMode() }
        KeepScreenOnEffect(enabled = true)
        LowTouchKitchenModeView(
            recipe = activeRecipe,
            targetServings = targetServings,
            stepIndex = activeStepIdx,
            parallelTimers = parallelTimers,
            onStepChange = { atelierVm.setCookingStep(it, activeRecipe.steps.size) },
            onStartTimer = { id, label, mins -> atelierVm.addOrRestartParallelTimer(id, label, mins) },
            onToggleTimer = { atelierVm.toggleParallelTimer(it) },
            onRemoveTimer = { atelierVm.removeParallelTimer(it) },
            onRequestChefSos = { currentStep ->
                atelierVm.requestChefSos(
                    weekNumber = activeRecipe.weekNumber,
                    contextSummary = "Modo Cozinha: ${activeRecipe.title} ($targetServings porções) • Etapa ${currentStep.stepNumber}: ${currentStep.title} (${currentStep.cookwareDeFrPt})",
                    prefilledPrompt = "Chef, estou na Etapa ${currentStep.stepNumber} (${currentStep.title}) de ${activeRecipe.title} para $targetServings porções. Qual o diagnóstico rápido e controle crítico para não errar o ponto?"
                )
                onNavigateToChefSos()
            },
            onExit = { atelierVm.exitKitchenMode() }
        )
        return
    }

    val filteredRecipes = remember(filterMode, filterCanton, filterWeek) {
        SwissmilkAndBaseRecipesCatalog.allRecipes.filter { rec ->
            val trackOk = when (filterMode) {
                1 -> rec.isChefkochV4
                2 -> rec.isSwissmilkClassic
                3 -> rec.recipeTrack == "base_global"
                else -> true
            }
            val cantonOk = filterCanton == "TODOS" || rec.cantonCode == filterCanton
            val weekOk = filterWeek == 0 || filterWeek in rec.weeksAppearing || rec.weekNumber == filterWeek
            trackOk && cantonOk && weekOk
        }
    }

    val missingTools = remember(activeRecipe, ownedEquipmentIds) {
        if (ownedEquipmentIds.isEmpty()) emptyList()
        else activeRecipe.requiredEquipmentIds.mapNotNull { id ->
            if (id in ownedEquipmentIds) null
            else TechnicalAtlasCatalog.allEquipmentTierItems.find { it.id == id }?.name
                ?: TechnicalAtlasCatalog.cookwareAndBlades.find { it.id == id }?.namePt
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subViewTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = subViewTab == 0,
                onClick = { subViewTab = 0 },
                text = { Text("Receita & Escala", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("recipes_tab_spec")
            )
            Tab(
                selected = subViewTab == 1,
                onClick = { subViewTab = 1 },
                text = { Text("Ficha Técnica (8 Blocos)", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("recipes_tab_mep8")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Recipe Track Filter Row (45 Total = 10 Step-by-Step + 23 Swissmilk + 12 Base & Mundo)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Todas (${SwissmilkAndBaseRecipesCatalog.allRecipes.size})",
                    1 to "Passo a Passo (10)",
                    2 to "Swissmilk (23)",
                    3 to "Base & Mundo (12)"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = filterMode == idx,
                        onClick = { filterMode = idx },
                        label = { Text(label) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("recipe_track_filter_$idx")
                    )
                }
            }

            // Canton & Week Quick Filters
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (listOf("TODOS") + SwissmilkAndBaseRecipesCatalog.availableCantons).forEach { canton ->
                    FilterChip(
                        selected = filterCanton == canton,
                        onClick = { filterCanton = canton },
                        label = { Text(if (canton == "TODOS") "Cantão: Todos" else canton, fontFamily = JetBrainsMonoFamily) },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (listOf(0) + (1..24).toList()).forEach { wk ->
                    FilterChip(
                        selected = filterWeek == wk,
                        onClick = { filterWeek = wk },
                        label = { Text(if (wk == 0) "Sem.: Todas" else "S${"%02d".format(wk)}", fontFamily = JetBrainsMonoFamily) },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredRecipes.forEach { rec ->
                    val isSel = if (subViewTab == 1) rec.id == activeMepSheetId else rec.id == activeRecipe.id
                    OutlinedButton(
                        onClick = { atelierVm.selectRecipe(rec.id) },
                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) SwissRed else MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSel) SwissRedContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("select_recipe_${rec.id}")
                    ) {
                        Text("S${"%02d".format(rec.weekNumber)} • ${rec.title.take(24)}")
                    }
                }
            }

            if (subViewTab == 0) {
                RecipeDetailAndScalerSection(
                    recipe = activeRecipe,
                    targetServings = targetServings,
                    missingTools = missingTools,
                    onServingsChange = { atelierVm.setTargetServings(it) },
                    onEnterKitchenMode = { atelierVm.enterKitchenMode(activeRecipe) },
                    onOpenMepCalculator = {
                        atelierVm.selectRecipe(activeRecipe.id)
                        subViewTab = 1
                    }
                )
            } else {
                val activeCloudLink = (myMepLinksState as? StreamUiState.Success)
                    ?.data
                    ?.firstOrNull { it.recipeId == activeMepSheetId }

                MepWeeklyCostChartCard(
                    analytics = costAnalytics,
                    onOpenSheetById = { atelierVm.selectMepSheet(it) },
                    onCreateNewSheet = { atelierVm.createCustomMepSheet() }
                )

                MepCalculatorWorkstation(
                    activeSheetId = activeMepSheetId,
                    catalogRecipe = catalogRecipeForMep,
                    targetServingsFromVm = targetServings,
                    savedSheet = mepSheetsMap[activeMepSheetId],
                    cloudSheetLink = activeCloudLink,
                    isSyncingPhoto = isSyncingMepPhoto,
                    fullMediaCache = fullMediaCache,
                    persistedRoomSheets = persistedMepSheets,
                    initialLines = initialLinesForActiveMep,
                    onSelectSheet = { atelierVm.selectMepSheet(it) },
                    onCreateCustomSheet = { atelierVm.createCustomMepSheet() },
                    onLoadZurcherExample = {
                        atelierVm.selectRecipe(CurriculumWeeksAndGuides.exampleMepZurcherRosti.recipeId)
                    },
                    onResetLinesToCatalog = {
                        val rec = catalogRecipeForMep ?: return@MepCalculatorWorkstation emptyList()
                        rec.ingredients.mapIndexed { idx, ing ->
                            val fc = ing.correctionFactorFc.coerceAtLeast(1.0)
                            com.example.data.local.MepIngredientLineEntity(
                                recipeId = rec.id,
                                sortOrder = idx,
                                name = ing.name,
                                sector = ing.sector,
                                unit = ing.unit,
                                baseNetPl = ing.netAmountPl,
                                trimLossPercent = if (fc > 1.0) ((1.0 - (1.0 / fc)) * 100.0) else 0.0,
                                correctionFactorFc = fc,
                                unitCostPerKgOrL = AtelierViewModel.defaultUnitCostForSector(ing.sector, ing.unit)
                            )
                        }
                    },
                    onSaveComplete = { sheet, lines -> atelierVm.saveMepComplete(sheet, lines) },
                    onDeleteSheet = { sheetId -> atelierVm.deleteMepSheet(sheetId) },
                    onCaptureOrLinkPhoto = { ctx, uri, sheet, lines ->
                        communityVm.captureAndLinkPhotoToMepSheet(
                            context = ctx,
                            uri = uri,
                            sheet = sheet,
                            onLinked = { localUriStr, cloudLink ->
                                atelierVm.attachCreationPhotoToMepSheet(
                                    sheet = sheet,
                                    lines = lines,
                                    localPhotoUri = localUriStr,
                                    cloudLink = cloudLink
                                )
                            }
                        )
                    },
                    onLoadFullResolutionPhoto = { mediaId ->
                        communityVm.loadFullResolutionPhoto(mediaId)
                    },
                    onRegisterTrainingPhoto = { sheet, _ ->
                        atelierVm.preparePhotoTransferFromMep(catalogRecipeForMep, sheet)
                        onNavigateToPhotoRegister()
                    },
                    onRequestChefSos = { sheet ->
                        atelierVm.requestChefSos(
                            weekNumber = sheet.weekNumber,
                            contextSummary = "Ficha MEP: ${sheet.title} | Praça ${sheet.stationBrigade} | ${sheet.targetServings} porções | PCC ≥ ${sheet.pccTargetTempC} °C",
                            prefilledPrompt = "Chef, preciso de socorro técnico na Ficha MEP \"${sheet.title}\" (${sheet.targetServings} porções, praça ${sheet.stationBrigade}, PCC ≥ ${sheet.pccTargetTempC} °C). Como corrigir textura/emulsão/rendimento sem comprometer o passe?"
                        )
                        onNavigateToChefSos()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeDetailAndScalerSection(
    recipe: RecipeSpec,
    targetServings: Int,
    missingTools: List<String>,
    onServingsChange: (Int) -> Unit,
    onEnterKitchenMode: () -> Unit,
    onOpenMepCalculator: () -> Unit
) {
    val conversionFactor = targetServings.toDouble() / recipe.baseServings.coerceAtLeast(1)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TagPill("SEMANA ${recipe.weeksAppearing.joinToString("/") { "S%02d".format(it) }}", SwissRedContainer, OnSwissRedContainer)
                if (recipe.cantonCode.isNotBlank()) TagPill("CANTÃO ${recipe.cantonCode}", InfoBlueBg, InfoBlue)
                if (recipe.isChefkochV4) TagPill("CHEFKOCH PASSO A PASSO", PccAmberBg, PccAmber)
                if (recipe.isSwissmilkClassic) {
                    val smLabel = recipe.swissmilkNum?.let { "SWISSMILK #$it" } ?: "CLÁSSICO SWISSMILK"
                    TagPill(smLabel, PccGreenBg, PccGreen)
                }
            }
            Text(recipe.title, style = MaterialTheme.typography.headlineSmall)
            Text(recipe.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("PCC & Controle Crítico: ${recipe.pccTargetSummary}", fontWeight = FontWeight.SemiBold, color = SwissRed)

            if (recipe.sousVideSummary.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InfoBlueBg),
                    border = BorderStroke(1.dp, InfoBlue)
                ) {
                    Text(
                        text = "Parâmetros Sous-Vide Validados: ${recipe.sousVideSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InfoBlue,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (recipe.caloriesKcal != null) {
                val macros = buildString {
                    append("Nutrição/porção: ${recipe.caloriesKcal} kcal")
                    recipe.proteinG?.let { append(" • Prot ${"%.0f".format(it)}g") }
                    recipe.carbsG?.let { append(" • Carb ${"%.0f".format(it)}g") }
                    recipe.fatG?.let { append(" • Lip ${"%.0f".format(it)}g") }
                }
                TagPill(macros, PccGreenBg, PccGreen)
            }

            if (missingTools.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccAmberBg),
                    border = BorderStroke(1.dp, PccAmber)
                ) {
                    Text(
                        text = "Utensílios ainda não marcados no seu inventário: ${missingTools.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            val baseScaleOption = recipe.baseServings
            val altScaleOption = recipe.altScaleServings ?: if (baseScaleOption == 10) 4 else 10
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onServingsChange(baseScaleOption) },
                    border = BorderStroke(if (targetServings == baseScaleOption) 2.dp else 1.dp, if (targetServings == baseScaleOption) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("servings_4_button")
                ) {
                    Text("$baseScaleOption ${recipe.yieldUnit} (Base)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onServingsChange(altScaleOption) },
                    border = BorderStroke(if (targetServings == altScaleOption) 2.dp else 1.dp, if (targetServings == altScaleOption) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("servings_10_button")
                ) {
                    Text("$altScaleOption ${recipe.yieldUnit} (Produção)", fontWeight = FontWeight.Bold)
                }
                TagPill(
                    text = "Escala = ${"%.2f".format(conversionFactor)}x",
                    containerColor = InfoBlueBg,
                    contentColor = InfoBlue
                )
            }

            Button(
                onClick = onEnterKitchenMode,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("enter_kitchen_mode_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ABRIR MODO COZINHA (BAIXO TOQUE • TELA ATIVA)", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOpenMepCalculator,
                colors = ButtonDefaults.buttonColors(containerColor = AtelierCharcoal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("open_mep_calculator_button")
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("CALCULADORA DE FICHA TÉCNICA (8 BLOCOS)", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Scaled Ingredients PB = PL * FC
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "INSUMOS ESCALONADOS ($targetServings PORÇÕES • PL vs. PB = PL × FC)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            recipe.ingredients.forEach { ing ->
                val pl = ing.netAmountPl * conversionFactor
                val pb = pl * ing.correctionFactorFc
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(ing.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = "PL ${"%.0f".format(pl)}${ing.unit} | PB ${"%.0f".format(pb)}${ing.unit}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Steps Overview with Smart Alerts
    recipe.steps.forEach { step ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Etapa ${step.stepNumber}: ${step.title}", style = MaterialTheme.typography.titleMedium)
                    step.targetTempC?.let { TagPill("$it °C", PccDangerBg, PccDanger) }
                }
                Text("Utensílio / Cutelaria: ${step.cookwareDeFrPt}", style = MaterialTheme.typography.labelMedium, color = InfoBlue)
                Text(step.instruction, style = MaterialTheme.typography.bodyMedium)
                SmartKitchenAlertBanner(alertType = step.alertType, message = step.alertBannerMessage)
            }
        }
    }
}

@Composable
private fun LowTouchKitchenModeView(
    recipe: RecipeSpec,
    targetServings: Int,
    stepIndex: Int,
    parallelTimers: List<ParallelKitchenTimer>,
    onStepChange: (Int) -> Unit,
    onStartTimer: (String, String, Int) -> Unit,
    onToggleTimer: (String) -> Unit,
    onRemoveTimer: (String) -> Unit,
    onRequestChefSos: (com.example.domain.model.CookingStepSpec) -> Unit,
    onExit: () -> Unit
) {
    val step = recipe.steps.getOrElse(stepIndex) { recipe.steps.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TagPill(stringResource(R.string.keep_screen_on_badge), PccGreenBg, PccGreen)
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .height(56.dp)
                    .testTag("exit_kitchen_mode_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Sair do Modo Cozinha")
                Spacer(Modifier.width(6.dp))
                Text("Sair da Bancada", fontWeight = FontWeight.Bold)
            }
        }

        Text("${recipe.title} ($targetServings porções)", style = MaterialTheme.typography.headlineSmall)

        if (parallelTimers.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CRONÔMETROS PARALELOS ATIVOS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    parallelTimers.forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(t.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(
                                formatSecondsMmSs(t.remainingSeconds),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (t.remainingSeconds == 0) PccDanger else SwissRed
                            )
                            IconButton(onClick = { onToggleTimer(t.id) }) {
                                Icon(
                                    imageVector = if (t.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Pausar ou retomar cronômetro paralelo"
                                )
                            }
                            IconButton(onClick = { onRemoveTimer(t.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover cronômetro")
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, SwissRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    TagPill("PASSO ${step.stepNumber} / ${recipe.steps.size}", SwissRedContainer, OnSwissRedContainer)
                    step.targetTempC?.let { TagPill("ALVO $it °C", PccDangerBg, PccDanger) }
                }
                Text(step.title, style = MaterialTheme.typography.headlineMedium)
                Text("Utensílio: ${step.cookwareDeFrPt}", style = MaterialTheme.typography.titleSmall, color = InfoBlue)
                Text(step.instruction, style = MaterialTheme.typography.bodyLarge)

                SmartKitchenAlertBanner(alertType = step.alertType, message = step.alertBannerMessage)

                step.timerMinutes?.let { mins ->
                    Button(
                        onClick = {
                            onStartTimer("${recipe.id}_s${step.stepNumber}", "${step.title} (${mins}m)", mins)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_step_timer_button")
                    ) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Acionar Cronômetro Paralelo ($mins min)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onStepChange(stepIndex - 1) },
                enabled = stepIndex > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("prev_cooking_step_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Passo anterior")
                Spacer(Modifier.width(8.dp))
                Text("Anterior", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { onStepChange(stepIndex + 1) },
                enabled = stepIndex < recipe.steps.lastIndex,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("next_cooking_step_button")
            ) {
                Text("Próximo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo passo")
            }
        }

        Button(
            onClick = { onRequestChefSos(step) },
            colors = ButtonDefaults.buttonColors(containerColor = PccAmber),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("sos_chef_ai_kitchen_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sos_chef_ai), fontWeight = FontWeight.Bold)
        }
    }
}
