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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.MepSheetEntity
import com.example.domain.model.RecipeSpec
import com.example.ui.components.KeepScreenOnEffect
import com.example.ui.components.SmartKitchenAlertBanner
import com.example.ui.components.TagPill
import com.example.ui.components.formatSecondsMmSs
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
import com.example.ui.viewmodel.ParallelKitchenTimer

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipesKitchenMepScreen(
    atelierVm: AtelierViewModel,
    onNavigateToPhotoRegister: () -> Unit
) {
    val selectedRecipeId by atelierVm.selectedRecipeId.collectAsStateWithLifecycle()
    val targetServings by atelierVm.targetServings.collectAsStateWithLifecycle()
    val isKitchenMode by atelierVm.isKitchenModeActive.collectAsStateWithLifecycle()
    val activeStepIdx by atelierVm.activeCookingStepIndex.collectAsStateWithLifecycle()
    val parallelTimers by atelierVm.parallelTimers.collectAsStateWithLifecycle()
    val ownedEquipmentIds by atelierVm.ownedEquipmentIds.collectAsStateWithLifecycle()
    val mepSheetsMap by atelierVm.mepSheetsMap.collectAsStateWithLifecycle()

    var filterMode by rememberSaveable { mutableIntStateOf(0) } // 0 Todos, 1 Chefkoch v4, 2 Swissmilk
    var subViewTab by rememberSaveable { mutableIntStateOf(0) } // 0 Receita & Escala, 1 Ficha MEP 8 Blocos

    val activeRecipe = remember(selectedRecipeId) {
        SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == selectedRecipeId }
            ?: SwissmilkAndBaseRecipesCatalog.allRecipes.first()
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
            onExit = { atelierVm.exitKitchenMode() }
        )
        return
    }

    val filteredRecipes = remember(filterMode) {
        when (filterMode) {
            1 -> SwissmilkAndBaseRecipesCatalog.allRecipes.filter { it.isChefkochV4 }
            2 -> SwissmilkAndBaseRecipesCatalog.allRecipes.filter { it.isSwissmilkClassic }
            else -> SwissmilkAndBaseRecipesCatalog.allRecipes
        }
    }

    val missingTools = remember(activeRecipe, ownedEquipmentIds) {
        if (ownedEquipmentIds.isEmpty()) emptyList()
        else activeRecipe.requiredEquipmentIds.mapNotNull { id ->
            if (id in ownedEquipmentIds) null
            else TechnicalAtlasCatalog.cookwareAndBlades.find { it.id == id }?.namePt
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
                text = { Text("Receita & Escala FC", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("recipes_tab_spec")
            )
            Tab(
                selected = subViewTab == 1,
                onClick = { subViewTab = 1 },
                text = { Text("Ficha MEP (8 Blocos)", fontWeight = FontWeight.Bold) },
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
            // Recipe Selector Bar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    0 to "Todas (${SwissmilkAndBaseRecipesCatalog.allRecipes.size})",
                    1 to "Chefkoch v4 (8)",
                    2 to "Swissmilk (23)"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = filterMode == idx,
                        onClick = { filterMode = idx },
                        label = { Text(label) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredRecipes.forEach { rec ->
                    val isSel = rec.id == activeRecipe.id
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
                    onEnterKitchenMode = { atelierVm.enterKitchenMode(activeRecipe) }
                )
            } else {
                MepEightBlocksEditorSection(
                    recipe = activeRecipe,
                    targetServings = targetServings,
                    savedSheet = mepSheetsMap[activeRecipe.id],
                    onLoadZurcherExample = {
                        atelierVm.selectRecipe(CurriculumWeeksAndGuides.exampleMepZurcherRosti.recipeId)
                    },
                    onSaveSheet = { atelierVm.saveMepSheet(it) },
                    onRegisterTrainingPhoto = { sheet ->
                        atelierVm.preparePhotoTransferFromMep(activeRecipe, sheet)
                        onNavigateToPhotoRegister()
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
    onEnterKitchenMode: () -> Unit
) {
    val conversionFactor = targetServings.toDouble() / recipe.baseServings.coerceAtLeast(1)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TagPill("SEMANA ${recipe.weekNumber}", SwissRedContainer, OnSwissRedContainer)
                if (recipe.cantonCode.isNotBlank()) TagPill("CANTÃO ${recipe.cantonCode}", InfoBlueBg, InfoBlue)
                if (recipe.isChefkochV4) TagPill("CHEFKOCH STEP-BY-STEP V4", PccAmberBg, PccAmber)
                if (recipe.isSwissmilkClassic) TagPill("CLÁSSICO SWISSMILK", PccGreenBg, PccGreen)
            }
            Text(recipe.title, style = MaterialTheme.typography.headlineSmall)
            Text(recipe.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("PCC & Controle Crítico: ${recipe.pccTargetSummary}", fontWeight = FontWeight.SemiBold, color = SwissRed)

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

            // Quick 4 vs 10 Servings + FC Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onServingsChange(4) },
                    border = BorderStroke(if (targetServings == 4) 2.dp else 1.dp, if (targetServings == 4) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("servings_4_button")
                ) {
                    Text("4 Porções (Casa)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onServingsChange(10) },
                    border = BorderStroke(if (targetServings == 10) 2.dp else 1.dp, if (targetServings == 10) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("servings_10_button")
                ) {
                    Text("10 Porções (Chefkoch)", fontWeight = FontWeight.Bold)
                }
                TagPill(
                    text = "FC Escala = ${"%.2f".format(conversionFactor)}x",
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
                Text("Bateria/Lâmina: ${step.cookwareDeFrPt}", style = MaterialTheme.typography.labelMedium, color = InfoBlue)
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

        // Parallel Kitchen Timers Bar (essential for Berner Platte 120m / 90m / 60m / 20m)
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
                                    contentDescription = "Pausar ou retomar timer paralelo"
                                )
                            }
                            IconButton(onClick = { onRemoveTimer(t.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover timer")
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
                        Text("Disparar Timer Paralelo ($mins min)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Giant Low-Touch Navigation Buttons (>= 60 dp)
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
    }
}

@Composable
private fun MepEightBlocksEditorSection(
    recipe: RecipeSpec,
    targetServings: Int,
    savedSheet: MepSheetEntity?,
    onLoadZurcherExample: () -> Unit,
    onSaveSheet: (MepSheetEntity) -> Unit,
    onRegisterTrainingPhoto: (MepSheetEntity) -> Unit
) {
    var b1 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block1Identification ?: "${recipe.title} | Semana ${recipe.weekNumber} | $targetServings porções")
    }
    var b2 by remember(recipe.id, savedSheet) {
        mutableStateOf(
            savedSheet?.block2IngredientsPbPlFc ?: recipe.ingredients.joinToString(" | ") {
                "${it.name}: PL ${it.netAmountPl}${it.unit} × FC ${it.correctionFactorFc}"
            }
        )
    }
    var b3 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block3CookwareBlades ?: recipe.steps.joinToString("; ") { it.cookwareDeFrPt })
    }
    var b4 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block4PrePrepCuts ?: "Cortes geométricos verificados com Chefmesser afiada.")
    }
    var b5 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block5ThermalFlowPcc ?: recipe.pccTargetSummary)
    }
    var b6 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block6PlatingFinishing ?: "Prato aquecido 55 °C, bordas limpas, altura e brilho nappé.")
    }
    var b7 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block7HygieneAllergens ?: "Cadeia fria ≤ 5 °C respeitada; higienização e desinfecção 99,99%.")
    }
    var b8 by remember(recipe.id, savedSheet) {
        mutableStateOf(savedSheet?.block8Selbstkontrolle ?: "")
    }

    fun currentEntity() = MepSheetEntity(
        recipeId = recipe.id,
        weekNumber = recipe.weekNumber,
        targetServings = targetServings,
        block1Identification = b1,
        block2IngredientsPbPlFc = b2,
        block3CookwareBlades = b3,
        block4PrePrepCuts = b4,
        block5ThermalFlowPcc = b5,
        block6PlatingFinishing = b6,
        block7HygieneAllergens = b7,
        block8Selbstkontrolle = b8
    )

    OutlinedButton(
        onClick = onLoadZurcherExample,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("load_zurcher_mep_example")
    ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Carregar Modelo Preenchido: Zürcher Geschnetzeltes + Rösti")
    }

    OutlinedTextField(b1, { b1 = it }, label = { Text("Bloco 1 — Identificação & Rendimento") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b2, { b2 = it }, label = { Text("Bloco 2 — Insumos (PB, PL & Fator de Correção)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b3, { b3 = it }, label = { Text("Bloco 3 — Bateria & Lâminas (PT/DE/FR)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b4, { b4 = it }, label = { Text("Bloco 4 — Pré-preparo & Geometria Schnittarten") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b5, { b5 = it }, label = { Text("Bloco 5 — Fluxo Térmico Cronometrado & PCCs") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b6, { b6 = it }, label = { Text("Bloco 6 — Empratamento & Acabamento Sensorial") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b7, { b7 = it }, label = { Text("Bloco 7 — Higiene Crítica & Alergênicos") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b8, { b8 = it }, label = { Text("Bloco 8 — Selbstkontrolle Pós-Prática (Erkenntnisse / Offene Punkte)") }, modifier = Modifier.fillMaxWidth())

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onSaveSheet(currentEntity()) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .testTag("save_mep_sheet_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Salvar MEP", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                val sheet = currentEntity()
                onSaveSheet(sheet)
                onRegisterTrainingPhoto(sheet)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
            modifier = Modifier
                .weight(1.3f)
                .height(56.dp)
                .testTag("register_training_photo_button")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.register_training_photo), fontWeight = FontWeight.Bold)
        }
    }
}
