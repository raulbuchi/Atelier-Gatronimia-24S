package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.SelfEvalEntity
import com.example.domain.calculators.CulinaryCalculators
import com.example.ui.components.MepWeeklyCostChartCard
import com.example.ui.components.TagPill
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentCalculatorsScreen(
    atelierVm: AtelierViewModel,
    onOpenMepSheet: (String) -> Unit = {},
    onCreateNewMepSheet: () -> Unit = {}
) {
    val selectedWeekNum by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val selfEvalsMap by atelierVm.selfEvaluationsMap.collectAsStateWithLifecycle()
    val costAnalytics by atelierVm.weeklyMepCostAnalytics.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        "Evolução Custos MEP",
        "Autoavaliação & Cópia",
        "Calculadoras v5 (a–f)",
        "Nutrição (Amido 80/20 & ≥ 30g)",
        "Brigada (13) & Praças"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = subTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("gestion_subtab_$idx")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (subTab) {
                0 -> MepWeeklyCostChartCard(
                    analytics = costAnalytics,
                    onOpenSheetById = { sheetId ->
                        atelierVm.selectMepSheet(sheetId)
                        onOpenMepSheet(sheetId)
                    },
                    onCreateNewSheet = {
                        atelierVm.createCustomMepSheet()
                        onCreateNewMepSheet()
                    }
                )
                1 -> SelbstkontrolleAndBackupSection(
                    selectedWeekNum = selectedWeekNum,
                    savedEval = selfEvalsMap[selectedWeekNum],
                    onSelectWeek = { atelierVm.selectWeek(it) },
                    onSaveEval = { atelierVm.saveSelfEvaluation(it) },
                    atelierVm = atelierVm
                )
                2 -> FiveCulinaryCalculatorsSection()
                3 -> ErnaehrungFiberBuilderSection()
                4 -> BrigadeAndOrgChartsSection()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelbstkontrolleAndBackupSection(
    selectedWeekNum: Int,
    savedEval: SelfEvalEntity?,
    onSelectWeek: (Int) -> Unit,
    onSaveEval: (SelfEvalEntity) -> Unit,
    atelierVm: AtelierViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    var sMep by remember(selectedWeekNum, savedEval) { mutableIntStateOf(savedEval?.scoreMiseEnPlace ?: 4) }
    var sTech by remember(selectedWeekNum, savedEval) { mutableIntStateOf(savedEval?.scoreTechnique ?: 4) }
    var sHyg by remember(selectedWeekNum, savedEval) { mutableIntStateOf(savedEval?.scoreHygienePcc ?: 5) }
    var sSens by remember(selectedWeekNum, savedEval) { mutableIntStateOf(savedEval?.scoreSensory ?: 4) }
    var erkenntnisse by remember(selectedWeekNum, savedEval) { mutableStateOf(savedEval?.erkenntnisse ?: "") }
    var offenePunkte by remember(selectedWeekNum, savedEval) { mutableStateOf(savedEval?.offenePunkte ?: "") }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..24).forEach { w ->
            FilterChip(
                selected = w == selectedWeekNum,
                onClick = { onSelectWeek(w) },
                label = { Text("S${"%02d".format(w)}", fontFamily = JetBrainsMonoFamily) },
                modifier = Modifier.height(48.dp)
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("AUTOAVALIAÇÃO DE BANCADA • SEMANA $selectedWeekNum", SwissRedContainer, OnSwissRedContainer)

            ScoreSelectorRow("1. Mise en Place & Organização de Praça", sMep) { sMep = it }
            ScoreSelectorRow("2. Técnica & Padronização de Cortes", sTech) { sTech = it }
            ScoreSelectorRow("3. Boas Práticas (APPCC / RDC 216) & PCC", sHyg) { sHyg = it }
            ScoreSelectorRow("4. Resultado Sensorial & Empratamento", sSens) { sSens = it }

            OutlinedTextField(
                value = erkenntnisse,
                onValueChange = { erkenntnisse = it },
                label = { Text("Aprendizados consolidados no turno") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = offenePunkte,
                onValueChange = { offenePunkte = it },
                label = { Text("Pontos de melhoria para a próxima produção") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSaveEval(
                        SelfEvalEntity(
                            weekNumber = selectedWeekNum,
                            scoreMiseEnPlace = sMep,
                            scoreTechnique = sTech,
                            scoreHygienePcc = sHyg,
                            scoreSensory = sSens,
                            erkenntnisse = erkenntnisse,
                            offenePunkte = offenePunkte
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_selbstkontrolle_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar Autoavaliação da Semana $selectedWeekNum", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Export Dossiê de Evolução Técnica + Cópia JSON
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("DOSSIÊ DE EVOLUÇÃO TÉCNICA & CÓPIA DE SEGURANÇA (JSON)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val md = atelierVm.buildLernreflexionMarkdown()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Dossiê de Evolução Técnica — Atelier Gastronomia 24S")
                                putExtra(Intent.EXTRA_TEXT, md)
                            }
                            context.startActivity(Intent.createChooser(send, "Exportar Dossiê de Evolução Técnica"))
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_lernreflexion_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar Dossiê")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val json = atelierVm.buildBackupJson()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "atelier-gastronomia-24s-backup.json")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(send, "Exportar Cópia de Segurança JSON"))
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_backup_json_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Compartilhar Cópia JSON")
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("import_backup_json_button")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar JSON")
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restaurar Cópia de Segurança (JSON)") },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("Cole o conteúdo JSON exportado") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        atelierVm.restoreFromBackupJson(importJsonInput)
                        showImportDialog = false
                    }
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ScoreSelectorRow(label: String, current: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { v ->
                FilterChip(
                    selected = current == v,
                    onClick = { onSelect(v) },
                    label = { Text("$v", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun FiveCulinaryCalculatorsSection() {
    // (a) Fachrechnen Cascade Losses
    var netPortion by rememberSaveable { mutableStateOf("180") }
    var trimLoss by rememberSaveable { mutableStateOf("20") }
    var cookLoss by rememberSaveable { mutableStateOf("15") }
    val cascade = CulinaryCalculators.calculateCascadeLosses(
        netPortionGrams = netPortion.toDoubleOrNull() ?: 180.0,
        lossPercentages = listOf(trimLoss.toDoubleOrNull() ?: 0.0, cookLoss.toDoubleOrNull() ?: 0.0)
    )

    CalculatorCard("(a) Perdas em Cascata (Rendimento Reverso)", "Compra = Porção / [(1 - apara) × (1 - cocção)]") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(netPortion, { netPortion = it }, label = { Text("Porção Líquida (g)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(trimLoss, { trimLoss = it }, label = { Text("Apara (%)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(cookLoss, { cookLoss = it }, label = { Text("Quebra Térmica (%)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Rendimento combinado: ${"%.1f".format(cascade.combinedYieldFactor * 100)}% (FC ${"%.2f".format(cascade.correctionFactorFc)}) → Compra Bruta = ${"%.1f".format(cascade.grossPurchaseGrams)} g",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = SwissRed
        )
    }

    // (b) Precificação Gastronômica (CMV & Custo Primário)
    var foodCost by rememberSaveable { mutableStateOf("9.50") }
    var laborCost by rememberSaveable { mutableStateOf("11.00") }
    var overheadCost by rememberSaveable { mutableStateOf("6.50") }
    var targetCmvPct by rememberSaveable { mutableStateOf("28") }
    val pricing = CulinaryCalculators.calculateSwissPricing(
        foodCostWarenkosten = foodCost.toDoubleOrNull() ?: 9.5,
        directLaborPersonalkosten = laborCost.toDoubleOrNull() ?: 11.0,
        overheadGemeinkosten = overheadCost.toDoubleOrNull() ?: 6.5,
        targetFoodCostPercent = targetCmvPct.toDoubleOrNull() ?: 28.0
    )

    CalculatorCard("(b) Engenharia de Preço, CMV & Custo Primário", "Custo Primário, Margem Bruta, Resultado & Cardápio") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(foodCost, { foodCost = it }, label = { Text("CMV (R$)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(laborCost, { laborCost = it }, label = { Text("Mão de Obra (R$)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(targetCmvPct, { targetCmvPct = it }, label = { Text("CMV Alvo %") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Custo Primário: R$ ${"%.2f".format(pricing.primeCostChf)} | Margem Bruta: R$ ${"%.2f".format(pricing.grossMarginBe1Chf)} | Resultado Operacional: R$ ${"%.2f".format(pricing.operatingProfitBe2Chf)}\nPreço Líquido: R$ ${"%.2f".format(pricing.netSellingPriceBvpChf)} → Preço de Cardápio (+8%): R$ ${"%.2f".format(pricing.grossMenuPriceEvpChf)}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }

    // (c) Ponto de Equilíbrio Operacional & Escala de Nota 1-6
    var fixedKf by rememberSaveable { mutableStateOf("18000") }
    var priceP by rememberSaveable { mutableStateOf("36") }
    var varKv by rememberSaveable { mutableStateOf("11") }
    var ptsGot by rememberSaveable { mutableStateOf("42") }
    var ptsMax by rememberSaveable { mutableStateOf("50") }
    val beGrade = CulinaryCalculators.calculateBreakEvenAndSwissGrade(
        fixedCostsKf = fixedKf.toDoubleOrNull() ?: 18000.0,
        unitSellingPriceP = priceP.toDoubleOrNull() ?: 36.0,
        unitVariableCostKv = varKv.toDoubleOrNull() ?: 11.0,
        achievedPoints = ptsGot.toDoubleOrNull() ?: 42.0,
        maxPoints = ptsMax.toDoubleOrNull() ?: 50.0
    )

    CalculatorCard("(c) Ponto de Equilíbrio & Escala Técnica (1–6)", "PE = Fixos / (Preço - Variável) | Nota = (Pts/Máx)×5 + 1") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(fixedKf, { fixedKf = it }, label = { Text("Fixos (R$)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(priceP, { priceP = it }, label = { Text("Preço Unit. (R$)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(varKv, { varKv = it }, label = { Text("Var. Unit. (R$)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(ptsGot, { ptsGot = it }, label = { Text("Pontos Obtidos") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(ptsMax, { ptsMax = it }, label = { Text("Pontos Máx.") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Ponto de Equilíbrio = ${beGrade.breakEvenUnits} porções | Nota Técnica = ${beGrade.swissGrade1To6} / 6,0",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = PccGreen
        )
    }

    // (d) Salmoura Equilibrada para Sous-Vide (5-10%)
    var waterL by rememberSaveable { mutableStateOf("2.0") }
    var brinePct by rememberSaveable { mutableStateOf("7.0") }
    var coreTemp by rememberSaveable { mutableStateOf("55.0") }
    var isFish by rememberSaveable { mutableStateOf(true) }
    val brine = CulinaryCalculators.calculateSousVideBrine(
        waterLiters = waterL.toDoubleOrNull() ?: 2.0,
        brineConcentrationPercent = brinePct.toDoubleOrNull() ?: 7.0,
        isDelicateFish = isFish,
        targetCoreTempC = coreTemp.toDoubleOrNull() ?: 55.0
    )

    CalculatorCard("(d) Salmoura Equilibrada para Sous-Vide (5–10% = 50–100 g/L)", "Ajuste do banho +0,8 °C acima do centro térmico") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(waterL, { waterL = it }, label = { Text("Água (L)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(brinePct, { brinePct = it }, label = { Text("Salmoura %") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(coreTemp, { coreTemp = it }, label = { Text("Centro °C") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = isFish, onCheckedChange = { isFish = it })
            Text(if (isFish) "Pescado delicado (10 min)" else "Peça alta / rico em colágeno (3–4 h)")
        }
        Text(
            "Sal necessário: ${"%.0f".format(brine.saltGramsRequired)} g | Tempo: ${brine.recommendedBrineMinutes} min | Banho: ${"%.1f".format(brine.recommendedBathTempC)} °C",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = InfoBlue
        )
        Text(brine.technicalProtocolNote, style = MaterialTheme.typography.bodySmall)
    }

    // (e) Simulador de Massa Folhada + 13 Modelagens
    var doughGrams by rememberSaveable { mutableStateOf("1000") }
    var isHalfPuff by rememberSaveable { mutableStateOf(false) }
    val puff = CulinaryCalculators.simulateSwissPuffPastry(
        targetBaseWeightGrams = doughGrams.toDoubleOrNull() ?: 1000.0,
        isHalfPuffHalbblaetterteig = isHalfPuff
    )

    CalculatorCard("(e) Simulador de Massa Folhada (Laminação)", "Clássica (4 dobras duplas) vs. Meia-Folhada (3 dobras duplas)") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(doughGrams, { doughGrams = it }, label = { Text("Escala Base (g)") }, singleLine = true, modifier = Modifier.weight(1f))
            Switch(checked = isHalfPuff, onCheckedChange = { isHalfPuff = it })
            Text(if (isHalfPuff) "Meia-Folhada" else "Clássica")
        }
        Text(puff.pastryCategory, fontWeight = FontWeight.Bold)
        Text(
            "Farinha Tipo 1: ${"%.0f".format(puff.flourGrams)}g | Gordura total: ${"%.0f".format(puff.totalFatGrams)}g | Água: ${"%.0f".format(puff.waterGrams)}g | Camadas: ${puff.theoreticalFatLayers}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall
        )
        Card(colors = CardDefaults.cardColors(containerColor = PccDangerBg)) {
            Text(puff.storageWarning, color = PccDanger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
        }
        Text("13 Modelagens Clássicas de Confeitaria Folhada:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        CulinaryCalculators.swissPuffPastry13Shapes.forEach { shape ->
            Text(shape, style = MaterialTheme.typography.bodySmall)
        }
    }

    // (f) Conversor de Gelatina & Calculadora de Hidrocoloides (v5)
    HydrocolloidCalculatorSection()
}

@Composable
private fun CalculatorCard(title: String, formula: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = SwissRed)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TagPill(formula, InfoBlueBg, InfoBlue)
            content()
        }
    }
}

@Composable
private fun ErnaehrungFiberBuilderSection() {
    var portionsSelected by rememberSaveable { mutableIntStateOf(3) }

    StarchComposition8020Card()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, PccGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagPill("NUTRIÇÃO APLICADA • META DIÁRIA ≥ 30 g DE FIBRAS", PccGreenBg, PccGreen)
            Text("Mecanismo de Redução do Colesterol Sérico:", fontWeight = FontWeight.Bold)
            Text(
                "As fibras alimentares ligam-se aos ácidos biliares no lúmen intestinal, promovendo sua excreção fecal. Para repor os sais biliares, o fígado capta colesterol da corrente sanguínea, reduzindo o LDL sérico.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Simulador Diário: $portionsSelected blocos selecionados = ${portionsSelected * 10} g de fibra (${if (portionsSelected >= 3) "META ≥ 30 g ATINGIDA" else "Abaixo de 30 g"})",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = if (portionsSelected >= 3) PccGreen else PccAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { n ->
                    FilterChip(
                        selected = portionsSelected == n,
                        onClick = { portionsSelected = n },
                        label = { Text("${n * 10}g") },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }
    }

    Text("EQUIVALÊNCIAS PRÁTICAS PARA 10 g DE FIBRA ALIMENTAR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    TechnicalAtlasCatalog.fiberEquivalentsFor10g.forEach { item ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(item.foodName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TagPill("${item.gramsFor10gFiber} g = 10g fibra", PccGreenBg, PccGreen)
                }
                Text(item.culinaryNote, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrigadeAndOrgChartsSection() {
    Text("3 MODELOS DE ORGANOGRAMA OPERACIONAL DE COZINHA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    TechnicalAtlasCatalog.operationalOrgCharts.forEach { org ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(org.operationType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(org.hierarchySummary, style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    org.keyStationsActive.forEach { st -> TagPill(st, InfoBlueBg, InfoBlue) }
                }
                Text("Equilíbrio operacional: ${org.operationalTradeOff}", style = MaterialTheme.typography.bodySmall, color = PccAmber)
            }
        }
    }

    Text("AS 13 PRAÇAS E FUNÇÕES DA BRIGADA CLÁSSICA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    TechnicalAtlasCatalog.classicBrigade13Stations.forEach { st ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (st.isHighlightedSpecialRole) PccAmberBg.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                if (st.isHighlightedSpecialRole) 1.5.dp else 1.dp,
                if (st.isHighlightedSpecialRole) PccAmber else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("${st.order}. ${st.rolePt} (${st.titleFr})", fontWeight = FontWeight.Bold)
                    if (st.isHighlightedSpecialRole) {
                        TagPill("DESTAQUE OPERACIONAL", SwissRedContainer, OnSwissRedContainer)
                    }
                }
                Text("Referência clássica: ${st.titleDe}", style = MaterialTheme.typography.labelLarge, color = SwissRed)
                Text(st.coreResponsibility, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
