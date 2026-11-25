package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity
import com.example.data.remote.MepSheetCloudLink
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.model.RecipeSpec
import com.example.ui.components.CameraXCaptureDialog
import com.example.ui.components.MepCreationPhotoCard
import com.example.ui.components.TagPill
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.AtelierCharcoalLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer

private val BRIGADE_STATIONS = listOf(
    "Saucier", "Rôtisseur", "Entremetier", "Garde-manger", "Pâtissier", "Dietoterapia"
)

private val UNITS_LIST = listOf("g", "ml", "kg", "L", "un")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MepCalculatorWorkstation(
    activeSheetId: String,
    catalogRecipe: RecipeSpec?,
    targetServingsFromVm: Int,
    savedSheet: MepSheetEntity?,
    cloudSheetLink: MepSheetCloudLink?,
    isSyncingPhoto: Boolean,
    fullMediaCache: Map<String, Bitmap>,
    persistedRoomSheets: List<MepSheetEntity>,
    initialLines: List<MepIngredientLineEntity>,
    onSelectSheet: (String) -> Unit,
    onCreateCustomSheet: () -> Unit,
    onLoadZurcherExample: () -> Unit,
    onResetLinesToCatalog: () -> List<MepIngredientLineEntity>,
    onSaveComplete: (MepSheetEntity, List<MepIngredientLineEntity>) -> Unit,
    onDeleteSheet: (String) -> Unit,
    onCaptureOrLinkPhoto: (Context, Uri, MepSheetEntity, List<MepIngredientLineEntity>) -> Unit,
    onLoadFullResolutionPhoto: (String) -> Unit,
    onRegisterTrainingPhoto: (MepSheetEntity, List<MepIngredientLineEntity>) -> Unit,
    onRequestChefSos: (MepSheetEntity) -> Unit = {}
) {
    val context = LocalContext.current
    var showCameraXDialog by remember { mutableStateOf(false) }
    var showBlankMepGuideDialog by remember { mutableStateOf(false) }
    if (showBlankMepGuideDialog) {
        BackHandler { showBlankMepGuideDialog = false }
        MepGuideConsultationDialog(onDismiss = { showBlankMepGuideDialog = false })
    }
    val stateKey = remember(activeSheetId, savedSheet?.updatedAtEpochMs, initialLines.size) {
        "$activeSheetId|${savedSheet?.updatedAtEpochMs ?: 0L}|${initialLines.size}"
    }

    var title by remember(stateKey) {
        mutableStateOf(savedSheet?.title?.ifBlank { catalogRecipe?.title.orEmpty() } ?: catalogRecipe?.title ?: "Ficha Técnica MEP")
    }
    var weekNumberText by remember(stateKey) {
        mutableStateOf((savedSheet?.weekNumber ?: catalogRecipe?.weekNumber ?: 1).toString())
    }
    var stationBrigade by remember(stateKey) {
        mutableStateOf(savedSheet?.stationBrigade ?: "Saucier")
    }
    var baseServings by remember(stateKey) {
        mutableIntStateOf(savedSheet?.baseServings ?: catalogRecipe?.baseServings ?: 4)
    }
    var targetServings by remember(stateKey, targetServingsFromVm) {
        mutableIntStateOf(savedSheet?.targetServings ?: targetServingsFromVm)
    }
    var shrinkageText by remember(stateKey) {
        mutableStateOf((savedSheet?.cookingShrinkagePercent ?: 12.0).toString())
    }
    var targetCmvText by remember(stateKey) {
        mutableStateOf((savedSheet?.targetCmvPercent ?: 28.0).toString())
    }
    var directLaborText by remember(stateKey) {
        mutableStateOf((savedSheet?.directLaborCost ?: 8.5).toString())
    }
    var pccTempC by remember(stateKey) {
        mutableIntStateOf(savedSheet?.pccTargetTempC ?: catalogRecipe?.steps?.mapNotNull { it.targetTempC }?.maxOrNull() ?: 70)
    }
    var plateTempC by remember(stateKey) {
        mutableIntStateOf(savedSheet?.plateTempC ?: 55)
    }
    var allergensCsv by remember(stateKey) {
        mutableStateOf(savedSheet?.allergensCsv.orEmpty())
    }
    var actualBenchYieldText by remember(stateKey) {
        mutableStateOf(
            if ((savedSheet?.actualBenchYieldGrams ?: 0.0) > 0.0) {
                "%.0f".format(savedSheet?.actualBenchYieldGrams ?: 0.0)
            } else ""
        )
    }
    var executionScore by remember(stateKey) {
        mutableIntStateOf(savedSheet?.executionScore1To5 ?: 4)
    }

    var b1 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block1Identification?.ifBlank { null }
                ?: "${catalogRecipe?.title ?: title} | Semana ${catalogRecipe?.weekNumber ?: 1} | Base $baseServings porções"
        )
    }
    var b3 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block3CookwareBlades?.ifBlank { null }
                ?: catalogRecipe?.steps?.joinToString("; ") { it.cookwareDeFrPt }.orEmpty()
        )
    }
    var b4 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block4PrePrepCuts?.ifBlank { null }
                ?: "Cortes clássicos padronizados com Faca do Chef afiada sobre tábua estabilizada."
        )
    }
    var b5 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block5ThermalFlowPcc?.ifBlank { null }
                ?: catalogRecipe?.pccTargetSummary.orEmpty()
        )
    }
    var b6 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block6PlatingFinishing?.ifBlank { null }
                ?: "Prato aquecido a 55 °C, bordas higienizadas, separação nítida entre molho em ponto nappé e guarnição crocante."
        )
    }
    var b7 by remember(stateKey) {
        mutableStateOf(
            savedSheet?.block7HygieneAllergens?.ifBlank { null }
                ?: "Cadeia fria ≤ 4 °C respeitada; higienização e antissepsia das mãos (ANVISA RDC 216 / APPCC)."
        )
    }
    var b8 by remember(stateKey) {
        mutableStateOf(savedSheet?.block8Selbstkontrolle.orEmpty())
    }

    val editableLines = remember(stateKey) {
        mutableStateListOf<MepIngredientLineEntity>().apply { addAll(initialLines) }
    }

    val shrinkagePct = shrinkageText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 85.0) ?: 12.0
    val targetCmvPct = targetCmvText.replace(',', '.').toDoubleOrNull()?.coerceIn(12.0, 80.0) ?: 28.0
    val directLaborBrl = directLaborText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 8.5
    val actualYieldGrams = actualBenchYieldText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val parsedWeek = weekNumberText.toIntOrNull()?.coerceIn(1, 24) ?: (catalogRecipe?.weekNumber ?: 1)

    val summary = remember(
        baseServings,
        targetServings,
        shrinkagePct,
        targetCmvPct,
        directLaborBrl,
        actualYieldGrams,
        editableLines.toList()
    ) {
        CulinaryCalculators.calculateMepSheetSummary(
            baseServings = baseServings,
            targetServings = targetServings,
            cookingShrinkagePercent = shrinkagePct,
            targetCmvPercent = targetCmvPct,
            directLaborPerPortionBrl = directLaborBrl,
            actualBenchYieldGrams = actualYieldGrams,
            ingredientLines = editableLines.toList()
        )
    }

    fun buildCurrentSheet(): MepSheetEntity {
        val b2Summary = summary.lines.joinToString(" | ") { l ->
            "${l.source.name}: PL ${"%.0f".format(l.scaledPl)}${l.source.unit} × FC ${"%.2f".format(l.effectiveFc)} = PB ${"%.0f".format(l.scaledPb)}${l.source.unit}"
        }
        return MepSheetEntity(
            recipeId = activeSheetId,
            title = title.ifBlank { catalogRecipe?.title ?: "Ficha Técnica MEP" },
            weekNumber = parsedWeek,
            stationBrigade = stationBrigade,
            baseServings = baseServings.coerceAtLeast(1),
            targetServings = targetServings.coerceAtLeast(1),
            portionWeightGrams = summary.theoreticalPortionCookedGrams,
            cookingShrinkagePercent = shrinkagePct,
            targetCmvPercent = targetCmvPct,
            directLaborCost = directLaborBrl,
            pccTargetTempC = pccTempC,
            plateTempC = plateTempC,
            allergensCsv = allergensCsv,
            actualBenchYieldGrams = actualYieldGrams,
            executionScore1To5 = executionScore,
            isCustomSheet = savedSheet?.isCustomSheet ?: (catalogRecipe == null),
            block1Identification = b1,
            block2IngredientsPbPlFc = b2Summary,
            block3CookwareBlades = b3,
            block4PrePrepCuts = b4,
            block5ThermalFlowPcc = b5,
            block6PlatingFinishing = b6,
            block7HygieneAllergens = b7,
            block8Selbstkontrolle = b8,
            localPhotoUri = savedSheet?.localPhotoUri ?: cloudSheetLink?.localPhotoUri.orEmpty(),
            cloudImageLink = savedSheet?.cloudImageLink?.ifBlank { cloudSheetLink?.imageLink.orEmpty() }
                ?: cloudSheetLink?.imageLink.orEmpty(),
            cloudMediaId = savedSheet?.cloudMediaId?.ifBlank { cloudSheetLink?.mediaId.orEmpty() }
                ?: cloudSheetLink?.mediaId.orEmpty(),
            photoThumbnailBase64 = savedSheet?.photoThumbnailBase64?.ifBlank { cloudSheetLink?.thumbnailBase64.orEmpty() }
                ?: cloudSheetLink?.thumbnailBase64.orEmpty()
        )
    }

    val mepGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCaptureOrLinkPhoto(context, uri, buildCurrentSheet(), editableLines.toList())
        }
    }

    if (showCameraXDialog) {
        CameraXCaptureDialog(
            recipeId = activeSheetId,
            sheetTitle = title.ifBlank { catalogRecipe?.title ?: "Ficha Técnica MEP" },
            onPhotoCaptured = { capturedUri ->
                showCameraXDialog = false
                onCaptureOrLinkPhoto(context, capturedUri, buildCurrentSheet(), editableLines.toList())
            },
            onDismiss = { showCameraXDialog = false }
        )
    }

    val isPersistedInRoom = remember(persistedRoomSheets, activeSheetId) {
        persistedRoomSheets.any { it.recipeId == activeSheetId }
    }

    // 0. ROOM PERSISTENCE VAULT BAR
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TagPill("ACERVO LOCAL (OFFLINE)", PccGreenBg, PccGreen)
                    Text(
                        text = "${persistedRoomSheets.size} fichas salvas",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isPersistedInRoom) {
                    IconButton(
                        onClick = { onDeleteSheet(activeSheetId) },
                        modifier = Modifier.testTag("delete_mep_room_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir ficha técnica salva",
                            tint = SwissRed
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onCreateCustomSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("create_custom_mep_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.new_custom_mep), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onLoadZurcherExample,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("load_zurcher_mep_example")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Modelo Zurique", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showBlankMepGuideDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_blank_mep_template")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = InfoBlue)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.blank_mep_template), fontWeight = FontWeight.Bold)
            }

            if (persistedRoomSheets.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    persistedRoomSheets.forEach { roomSheet ->
                        val selected = roomSheet.recipeId == activeSheetId
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSheet(roomSheet.recipeId) },
                            label = {
                                val label = roomSheet.title.ifBlank { roomSheet.recipeId }
                                Text("S${"%02d".format(roomSheet.weekNumber)} • ${label.take(22)} (${roomSheet.targetServings}p)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SwissRedContainer,
                                selectedLabelColor = OnSwissRedContainer
                            ),
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }
        }
    }

    // LIVE TELEMETRY HEADER STRIP (#272E36 & #AE2127)
    Card(
        colors = CardDefaults.cardColors(containerColor = AtelierCharcoal),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mep_live_kpi_strip")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PAINEL DE ENGENHARIA DA FICHA TÉCNICA • TEMPO REAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = PureSurface,
                    fontWeight = FontWeight.Bold
                )
                TagPill(
                    text = "ESCALA ${"%.2f".format(summary.scaleFactor)}x",
                    containerColor = SwissRed,
                    contentColor = PureSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricTile(
                    label = "PL Total / PB Compra",
                    value = "${"%.0f".format(summary.totalScaledPlGramsEquiv)}g → ${"%.0f".format(summary.totalScaledPbGramsEquiv)}g",
                    sub = "FC Médio ${"%.2f".format(summary.weightedAverageFc)}",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTile(
                    label = "Pós-Cocção (-${"%.0f".format(shrinkagePct)}%)",
                    value = "${"%.0f".format(summary.theoreticalCookedYieldGrams)} g",
                    sub = "${"%.0f".format(summary.theoreticalPortionCookedGrams)} g / porção",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricTile(
                    label = "CMV / Porção (${"%.0f".format(targetCmvPct)}%)",
                    value = "R$ ${"%.2f".format(summary.foodCostPerPortionBrl)}",
                    sub = "Lote R$ ${"%.2f".format(summary.totalBatchFoodCostBrl)}",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTile(
                    label = "Preço Sugerido",
                    value = "Cardápio R$ ${"%.2f".format(summary.pricingPerPortion.grossMenuPriceEvpBrl)}",
                    sub = "Líquido R$ ${"%.2f".format(summary.pricingPerPortion.netSellingPriceBvpBrl)} (+8% MWST ref. suíça)",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // BLOCO 1 — Identificação, Praça & Escalonamento Operacional
    MepBlockCard(
        blockBadge = "BLOCO 1 / 8",
        title = "Identificação, Praça da Brigada & Escala",
        subtitle = "Ajuste porções com botões amplos (≥ 56 dp) e parametrize quebra térmica e CMV alvo."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título da Ficha Técnica") },
                singleLine = true,
                modifier = Modifier
                    .weight(2.2f)
                    .testTag("mep_title_input")
            )
            OutlinedTextField(
                value = weekNumberText,
                onValueChange = { weekNumberText = it },
                label = { Text("Sem.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f)
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BRIGADE_STATIONS.forEach { st ->
                FilterChip(
                    selected = stationBrigade == st,
                    onClick = { stationBrigade = st },
                    label = { Text(st) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { targetServings = (targetServings - 1).coerceAtLeast(1) },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("mep_servings_minus")
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease_servings))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = "$targetServings PORÇÕES",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Base da receita: $baseServings porções", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { targetServings = (targetServings + 1).coerceAtMost(200) },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("mep_servings_plus")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase_servings))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(4, 10, 25, 50).forEach { pax ->
                val sel = targetServings == pax
                OutlinedButton(
                    onClick = { targetServings = pax },
                    border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("mep_preset_pax_$pax")
                ) {
                    Text("${pax}p", fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = shrinkageText,
                onValueChange = { shrinkageText = it },
                label = { Text("Quebra Térmica %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetCmvText,
                onValueChange = { targetCmvText = it },
                label = { Text("Meta CMV %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = directLaborText,
                onValueChange = { directLaborText = it },
                label = { Text("Mão Obra/porç.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = b1,
            onValueChange = { b1 = it },
            label = { Text("Resumo Técnico de Identificação") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block1_input")
        )
    }

    // BLOCO 2 — Calculadora Matricial de Insumos (PB = PL × FC & CMV)
    MepBlockCard(
        blockBadge = "BLOCO 2 / 8",
        title = "Matriz Calculadora de Insumos (PB = PL × FC)",
        subtitle = "Edite Peso Líquido Base, Apara de Limpeza (%) e Custo (R$/kg ou L) para recalcular compra bruta e CMV."
    ) {
        summary.lines.forEachIndexed { idx, calcLine ->
            val raw = editableLines.getOrNull(idx) ?: return@forEachIndexed
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = raw.name,
                            onValueChange = { editableLines[idx] = raw.copy(name = it) },
                            label = { Text("Insumo #${idx + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val nextUnitIdx = (UNITS_LIST.indexOf(raw.unit) + 1) % UNITS_LIST.size
                                editableLines[idx] = raw.copy(unit = UNITS_LIST[nextUnitIdx])
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(raw.unit, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { if (editableLines.size > 1) editableLines.removeAt(idx) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_ingredient),
                                tint = SwissRed
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        DecimalLineCell(
                            label = "PL Base (${raw.unit})",
                            initialValue = raw.baseNetPl,
                            onValidChange = { editableLines[idx] = raw.copy(baseNetPl = it) },
                            modifier = Modifier.weight(1f)
                        )
                        DecimalLineCell(
                            label = "Apara %",
                            initialValue = calcLine.effectiveTrimLossPercent,
                            onValidChange = { pct ->
                                val clamped = pct.coerceIn(0.0, 88.0)
                                val newFc = if (clamped > 0.0) 1.0 / (1.0 - clamped / 100.0) else 1.0
                                editableLines[idx] = raw.copy(trimLossPercent = clamped, correctionFactorFc = newFc)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DecimalLineCell(
                            label = "R$/kg ou L",
                            initialValue = raw.unitCostPerKgOrL,
                            onValidChange = { editableLines[idx] = raw.copy(unitCostPerKgOrL = it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TagPill("FC ${"%.2f".format(calcLine.effectiveFc)}", InfoBlueBg, InfoBlue)
                        Text(
                            text = "PL ${"%.0f".format(calcLine.scaledPl)}${raw.unit} → PB ${"%.0f".format(calcLine.scaledPb)}${raw.unit} • R$ ${"%.2f".format(calcLine.lineCostBrl)}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    editableLines.add(
                        MepIngredientLineEntity(
                            recipeId = activeSheetId,
                            sortOrder = editableLines.size,
                            name = "Novo Insumo",
                            sector = "Mercearia Seca",
                            unit = "g",
                            baseNetPl = 100.0,
                            trimLossPercent = 8.0,
                            correctionFactorFc = 1.09,
                            unitCostPerKgOrL = 16.0
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mep_add_ingredient_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Adicionar Insumo", fontWeight = FontWeight.Bold)
            }
            if (catalogRecipe != null) {
                OutlinedButton(
                    onClick = {
                        val defaults = onResetLinesToCatalog()
                        editableLines.clear()
                        editableLines.addAll(defaults)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Base Receita")
                }
            }
        }
    }

    val hasVegetableLines = remember(editableLines.toList()) {
        editableLines.any { it.sector.contains("Hortifrúti", ignoreCase = true) }
    }

    // BLOCOS 3 A 8
    MepTechnicalBlocksSection(
        summary = summary,
        hasVegetableIngredients = hasVegetableLines,
        block3Cookware = b3,
        onBlock3Change = { b3 = it },
        block4Cuts = b4,
        onBlock4Change = { b4 = it },
        pccTargetTempC = pccTempC,
        onPccTempChange = { pccTempC = it },
        block5ThermalPcc = b5,
        onBlock5Change = { b5 = it },
        plateTempC = plateTempC,
        onPlateTempChange = { plateTempC = it },
        block6Plating = b6,
        onBlock6Change = { b6 = it },
        allergensCsv = allergensCsv,
        onAllergensCsvChange = { allergensCsv = it },
        block7Hygiene = b7,
        onBlock7Change = { b7 = it },
        actualBenchYieldText = actualBenchYieldText,
        onActualBenchYieldChange = { actualBenchYieldText = it },
        executionScore1To5 = executionScore,
        onExecutionScoreChange = { executionScore = it },
        block8Selbstkontrolle = b8,
        onBlock8Change = { b8 = it }
    )

    val effectiveCloudLink = savedSheet?.cloudImageLink?.ifBlank { cloudSheetLink?.imageLink.orEmpty() }
        ?: cloudSheetLink?.imageLink.orEmpty()
    val effectiveMediaId = savedSheet?.cloudMediaId?.ifBlank { cloudSheetLink?.mediaId.orEmpty() }
        ?: cloudSheetLink?.mediaId.orEmpty()
    val effectiveThumb = savedSheet?.photoThumbnailBase64?.ifBlank { cloudSheetLink?.thumbnailBase64.orEmpty() }
        ?: cloudSheetLink?.thumbnailBase64.orEmpty()

    MepCreationPhotoCard(
        sheetTitle = title.ifBlank { catalogRecipe?.title ?: "Ficha Técnica MEP" },
        cloudImageLink = effectiveCloudLink,
        cloudMediaId = effectiveMediaId,
        photoThumbnailBase64 = effectiveThumb,
        isSyncingPhoto = isSyncingPhoto,
        fullResolutionBitmap = fullMediaCache[effectiveMediaId],
        onOpenCameraX = { showCameraXDialog = true },
        onPickFromGallery = {
            mepGalleryPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRequestFullResolution = onLoadFullResolutionPhoto,
        onAuditWithChefAi = {
            val sheet = buildCurrentSheet()
            val currentLines = editableLines.toList()
            onSaveComplete(sheet, currentLines)
            onRegisterTrainingPhoto(sheet, currentLines)
        }
    )

    // PRIMARY PERSISTENCE & EXPORT ACTIONS (>= 56 dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onSaveComplete(buildCurrentSheet(), editableLines.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = AtelierCharcoal),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("save_mep_sheet_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.save_mep_room), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    val sheet = buildCurrentSheet()
                    onSaveComplete(sheet, editableLines.toList())
                    val md = LocalKitchenRepository.formatSingleMepSheetMarkdown(sheet, summary)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Ficha Técnica", md))
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Ficha Técnica — ${sheet.title}")
                        putExtra(Intent.EXTRA_TEXT, md)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Exportar Ficha Técnica"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("export_mep_markdown_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.export_mep_markdown), fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = {
                val sheet = buildCurrentSheet()
                val currentLines = editableLines.toList()
                onSaveComplete(sheet, currentLines)
                onRegisterTrainingPhoto(sheet, currentLines)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("register_training_photo_button")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.register_training_photo), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                val sheet = buildCurrentSheet()
                onSaveComplete(sheet, editableLines.toList())
                onRequestChefSos(sheet)
            },
            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("sos_chef_ai_mep_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sos_chef_ai), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KpiMetricTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AtelierCharcoalLight),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD0D7DF))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleSmall,
                color = PureSurface,
                fontWeight = FontWeight.Bold
            )
            Text(sub, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE5E9EE))
        }
    }
}

@Composable
private fun DecimalLineCell(
    label: String,
    initialValue: Double,
    onValidChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(initialValue) {
        mutableStateOf(
            if (initialValue % 1.0 == 0.0) "%.0f".format(initialValue) else "%.1f".format(initialValue)
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.replace(',', '.').toDoubleOrNull()?.let { parsed ->
                onValidChange(parsed)
            }
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
