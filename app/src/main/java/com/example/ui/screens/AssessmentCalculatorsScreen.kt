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
import androidx.compose.material.icons.filled.Download
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
    atelierVm: AtelierViewModel
) {
    val selectedWeekNum by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val selfEvalsMap by atelierVm.selfEvaluationsMap.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        "Selbstkontrolle & Backup",
        "5 Calculadoras Suíças",
        "Nutrição (≥30g Fibra)",
        "Brigada (13) & Org"
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
                0 -> SelbstkontrolleAndBackupSection(
                    selectedWeekNum = selectedWeekNum,
                    savedEval = selfEvalsMap[selectedWeekNum],
                    onSelectWeek = { atelierVm.selectWeek(it) },
                    onSaveEval = { atelierVm.saveSelfEvaluation(it) },
                    atelierVm = atelierVm
                )
                1 -> FiveCulinaryCalculatorsSection()
                2 -> ErnaehrungFiberBuilderSection()
                3 -> BrigadeAndOrgChartsSection()
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
            TagPill("SELBSTKONTROLLE • SEMANA $selectedWeekNum", SwissRedContainer, OnSwissRedContainer)

            ScoreSelectorRow("1. Mise en Place & Organização", sMep) { sMep = it }
            ScoreSelectorRow("2. Técnica & Geometria de Corte", sTech) { sTech = it }
            ScoreSelectorRow("3. Higiene HACCP & PCC Térmico", sHyg) { sHyg = it }
            ScoreSelectorRow("4. Resultado Sensorial & Empratamento", sSens) { sSens = it }

            OutlinedTextField(
                value = erkenntnisse,
                onValueChange = { erkenntnisse = it },
                label = { Text("Erkenntnisse (Aprendizados consolidados no turno)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = offenePunkte,
                onValueChange = { offenePunkte = it },
                label = { Text("Offene Punkte (Pontos a corrigir na próxima prática)") },
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
                Text("Salvar Selbstkontrolle da Semana $selectedWeekNum", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Export Dossier Lernreflexion + Backup JSON
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("DOSSIER LERNREFLEXION & BACKUP PORTÁVEL JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val md = atelierVm.buildLernreflexionMarkdown()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Dossier Lernreflexion — Atelier Gastronomia 24S")
                                putExtra(Intent.EXTRA_TEXT, md)
                            }
                            context.startActivity(Intent.createChooser(send, "Exportar Dossier Lernreflexion"))
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_lernreflexion_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar Dossier")
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
                            context.startActivity(Intent.createChooser(send, "Exportar Backup JSON"))
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_backup_json_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar JSON")
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
            title = { Text("Restaurar Backup JSON Portável") },
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

    CalculatorCard("(a) Perdas em Cascata (Fachrechnen)", "Compra = Porção / [(1 - apara) × (1 - cocção)]") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(netPortion, { netPortion = it }, label = { Text("Porção Líquida (g)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(trimLoss, { trimLoss = it }, label = { Text("Apara (%)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(cookLoss, { cookLoss = it }, label = { Text("Cocção (%)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Rendimento combinado: ${"%.1f".format(cascade.combinedYieldFactor * 100)}% (FC ${"%.2f".format(cascade.correctionFactorFc)}) → Compra Bruta = ${"%.1f".format(cascade.grossPurchaseGrams)} g",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = SwissRed
        )
    }

    // (b) Swiss Pricing BAB/BeBu + MWST 8%
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

    CalculatorCard("(b) Precificação Suíça BAB/BeBu (MWST 8%)", "Prime-Cost, BE1, BE2, BVP & EVP") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(foodCost, { foodCost = it }, label = { Text("CMV (CHF)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(laborCost, { laborCost = it }, label = { Text("Pessoal (CHF)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(targetCmvPct, { targetCmvPct = it }, label = { Text("CMV Alvo %") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Prime-Cost: CHF ${"%.2f".format(pricing.primeCostChf)} | BE1: CHF ${"%.2f".format(pricing.grossMarginBe1Chf)} | BE2: CHF ${"%.2f".format(pricing.operatingProfitBe2Chf)}\nBVP (s/ IVA): CHF ${"%.2f".format(pricing.netSellingPriceBvpChf)} → EVP (c/ MWST 8%): CHF ${"%.2f".format(pricing.grossMenuPriceEvpChf)}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }

    // (c) Break-even & Swiss Grade 1-6
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

    CalculatorCard("(c) Ponto de Equilíbrio x_cr & Nota Suíça 1–6", "x_cr = Kf / (p - kv) | Nota = (Pts/Máx)×5 + 1") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(fixedKf, { fixedKf = it }, label = { Text("Fixo Kf") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(priceP, { priceP = it }, label = { Text("Preço p") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(varKv, { varKv = it }, label = { Text("Var. kv") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(ptsGot, { ptsGot = it }, label = { Text("Pontos Obtidos") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(ptsMax, { ptsMax = it }, label = { Text("Pontos Máx.") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Text(
            "Ponto Crítico x_cr = ${beGrade.breakEvenUnits} pratos | Nota Oficial Suíça = ${beGrade.swissGrade1To6} / 6.0",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = PccGreen
        )
    }

    // (d) Sous-vide Brine Calculator (5-10%)
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

    CalculatorCard("(d) Salmoura Sous-Vide (5–10% = 50–100 g/L)", "Ajuste banho +0,8 °C acima do centro térmico") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(waterL, { waterL = it }, label = { Text("Água (L)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(brinePct, { brinePct = it }, label = { Text("Salmoura %") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(coreTemp, { coreTemp = it }, label = { Text("Centro °C") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = isFish, onCheckedChange = { isFish = it })
            Text(if (isFish) "Peixe delicado (10 min)" else "Carne grande / colágeno (3–4 h)")
        }
        Text(
            "Sal exigido: ${"%.0f".format(brine.saltGramsRequired)} g | Tempo: ${brine.recommendedBrineMinutes} min | Banho: ${"%.1f".format(brine.recommendedBathTempC)} °C",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = InfoBlue
        )
        Text(brine.technicalProtocolNote, style = MaterialTheme.typography.bodySmall)
    }

    // (e) Swiss Puff Pastry Simulator + 13 Shapes
    var doughGrams by rememberSaveable { mutableStateOf("1000") }
    var isHalfPuff by rememberSaveable { mutableStateOf(false) }
    val puff = CulinaryCalculators.simulateSwissPuffPastry(
        targetBaseWeightGrams = doughGrams.toDoubleOrNull() ?: 1000.0,
        isHalfPuffHalbblaetterteig = isHalfPuff
    )

    CalculatorCard("(e) Simulador Folhado Suíço (Teige)", "Deutscher (4 dobras duplas) vs. Halbblätterteig (3 dobras duplas)") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(doughGrams, { doughGrams = it }, label = { Text("Escala Base (g)") }, singleLine = true, modifier = Modifier.weight(1f))
            Switch(checked = isHalfPuff, onCheckedChange = { isHalfPuff = it })
            Text(if (isHalfPuff) "Halbblätterteig" else "Deutscher")
        }
        Text(puff.pastryCategory, fontWeight = FontWeight.Bold)
        Text(
            "Farinha T400: ${"%.0f".format(puff.flourGrams)}g | Gordura total: ${"%.0f".format(puff.totalFatGrams)}g | Água: ${"%.0f".format(puff.waterGrams)}g | Camadas: ${puff.theoreticalFatLayers}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall
        )
        Card(colors = CardDefaults.cardColors(containerColor = PccDangerBg)) {
            Text(puff.storageWarning, color = PccDanger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
        }
        Text("13 Formatos Clássicos (Blätterteigkonfekt):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        CulinaryCalculators.swissPuffPastry13Shapes.forEach { shape ->
            Text(shape, style = MaterialTheme.typography.bodySmall)
        }
    }
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

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, PccGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TagPill("ERNAEHRUNG • META DIÁRIA ≥ 30 g DE FIBRAS", PccGreenBg, PccGreen)
            Text("Mecanismo de Redução do Colesterol Sérico:", fontWeight = FontWeight.Bold)
            Text(
                "As fibras alimentares (Nahrungsfasern) ligam-se aos ácidos biliares no lúmen intestinal promovendo sua excreção fecal. Para repor o pool biliar, o fígado capta colesterol da corrente sanguínea, reduzindo o LDL sérico.",
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

    Text("EQUIVALÊNCIAS OFICIAIS PARA 10 g DE FIBRA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
    Text("3 ORGANOGRAMAS OPERACIONAIS (BETRIEBSORGANISATION)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                Text("Trade-off: ${org.operationalTradeOff}", style = MaterialTheme.typography.bodySmall, color = PccAmber)
            }
        }
    }

    Text("OS 13 POSTOS DA BRIGADA CLÁSSICA SUÍÇA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    Text("${st.order}. ${st.titleDe} (${st.titleFr})", fontWeight = FontWeight.Bold)
                    if (st.isHighlightedSpecialRole) {
                        TagPill("DESTAQUE V4", SwissRedContainer, OnSwissRedContainer)
                    }
                }
                Text(st.rolePt, style = MaterialTheme.typography.labelLarge, color = SwissRed)
                Text(st.coreResponsibility, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
