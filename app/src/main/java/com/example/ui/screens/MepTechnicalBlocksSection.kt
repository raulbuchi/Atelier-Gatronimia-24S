package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.domain.calculators.MepCalculationSummary
import com.example.domain.model.SmartKitchenAlertType
import com.example.ui.components.SmartKitchenAlertBanner
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
import kotlin.math.abs

private val ALLERGEN_CATALOG = listOf(
    "Glúten", "Lactose", "Ovos", "Peixes", "Crustáceos",
    "Castanhas/Nozes", "Salsão (Aipo)", "Mostarda", "Sulfitos", "Soja"
)

private val PCC_PRESETS = listOf(
    55 to "55 °C Sous-Vide Pescado",
    63 to "63 °C Pescado Grelhado",
    65 to "65 °C Galantina",
    68 to "68 °C Iscas de Vitela",
    70 to "70 °C Geral / Regeneração",
    71 to "71 °C Carne Moída",
    74 to "74 °C Aves",
    80 to "80 °C Creme em Banho-Maria",
    175 to "175 °C Limite Fritura"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MepTechnicalBlocksSection(
    summary: MepCalculationSummary,
    hasVegetableIngredients: Boolean,
    block3Cookware: String,
    onBlock3Change: (String) -> Unit,
    block4Cuts: String,
    onBlock4Change: (String) -> Unit,
    pccTargetTempC: Int,
    onPccTempChange: (Int) -> Unit,
    block5ThermalPcc: String,
    onBlock5Change: (String) -> Unit,
    plateTempC: Int,
    onPlateTempChange: (Int) -> Unit,
    block6Plating: String,
    onBlock6Change: (String) -> Unit,
    allergensCsv: String,
    onAllergensCsvChange: (String) -> Unit,
    block7Hygiene: String,
    onBlock7Change: (String) -> Unit,
    actualBenchYieldText: String,
    onActualBenchYieldChange: (String) -> Unit,
    executionScore1To5: Int,
    onExecutionScoreChange: (Int) -> Unit,
    block8Selbstkontrolle: String,
    onBlock8Change: (String) -> Unit
) {
    val triggersIronPanVitaminCHazard = hasVegetableIngredients &&
        (block3Cookware.contains("aço carbono", ignoreCase = true) ||
            block3Cookware.contains("Lyoner", ignoreCase = true) ||
            block3Cookware.contains("lyonnaise", ignoreCase = true)) &&
        !block3Cookware.contains("antiaderente", ignoreCase = true) &&
        !block3Cookware.contains("Antihaft", ignoreCase = true) &&
        !block3Cookware.contains("antiadhésif", ignoreCase = true)

    // BLOCO 3 — Bateria de Cozinha & Cutelaria
    MepBlockCard(
        blockBadge = "BLOCO 3 / 8",
        title = "Bateria de Cozinha & Cutelaria",
        subtitle = "Toque rápido para vincular panelas e lâminas à ficha técnica sem digitação extensa."
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TechnicalAtlasCatalog.cookwareAndBlades.take(10).forEach { tool ->
                val token = "${tool.namePt} (${tool.nameFr})"
                val isSelected = block3Cookware.contains(tool.namePt, ignoreCase = true) ||
                    block3Cookware.contains(tool.nameFr, ignoreCase = true) ||
                    block3Cookware.contains(tool.nameDe, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = toggleTokenInText(block3Cookware, token, separator = "; ")
                        onBlock3Change(updated)
                    },
                    label = { Text(tool.namePt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        if (triggersIronPanVitaminCHazard) {
            SmartKitchenAlertBanner(
                alertType = SmartKitchenAlertType.NON_STICK_VITAMIN_C,
                message = "Frigideira de aço carbono detectada com insumos de Hortifrúti: íons metálicos oxidam e destroem a Vitamina C. Adicione frigideira antiaderente para os vegetais."
            )
        }
        OutlinedTextField(
            value = block3Cookware,
            onValueChange = onBlock3Change,
            label = { Text("Especificação de Bateria & Cutelaria") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block3_input")
        )
    }

    // BLOCO 4 — Pré-preparo & Padronização de Cortes
    MepBlockCard(
        blockBadge = "BLOCO 4 / 8",
        title = "Pré-preparo & Padronização de Cortes",
        subtitle = "Selecione os cortes clássicos milimétricos para garantir cocção uniforme."
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TechnicalAtlasCatalog.swissCuts.forEach { cut ->
                val token = "${cut.name} (${cut.dimensionSwiss})"
                val isSelected = block4Cuts.contains(cut.name, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onBlock4Change(toggleTokenInText(block4Cuts, token, separator = "; "))
                    },
                    label = { Text(cut.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        Text(
            text = TechnicalAtlasCatalog.DULL_BLADE_QUALITY_RULE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = block4Cuts,
            onValueChange = onBlock4Change,
            label = { Text("Protocolo de Pré-preparo & Cortes") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block4_input")
        )
    }

    // BLOCO 5 — Fluxo Térmico Cronometrado & PCC
    MepBlockCard(
        blockBadge = "BLOCO 5 / 8",
        title = "Fluxo Térmico Cronometrado & PCC",
        subtitle = "Defina o Ponto Crítico de Controle térmico (centro geométrico ou temperatura máxima do óleo)."
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PCC Alvo Selecionado:", fontWeight = FontWeight.SemiBold)
            TagPill(
                text = "PCC ≥ $pccTargetTempC °C",
                containerColor = if (pccTargetTempC >= 165) PccAmberBg else PccDangerBg,
                contentColor = if (pccTargetTempC >= 165) PccAmber else PccDanger
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PCC_PRESETS.forEach { (temp, label) ->
                FilterChip(
                    selected = pccTargetTempC == temp,
                    onClick = {
                        onPccTempChange(temp)
                        if (!block5ThermalPcc.contains("$temp °C")) {
                            val suffix = if (block5ThermalPcc.isBlank()) "PCC validado: $label."
                            else "$block5ThermalPcc | PCC: $label"
                            onBlock5Change(suffix)
                        }
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        if (pccTargetTempC >= 170) {
            SmartKitchenAlertBanner(
                alertType = SmartKitchenAlertType.DEEP_FRY_ACRYLAMIDE,
                message = "Fritura por imersão: pré-fritar a 140 °C e finalizar a 165–175 °C. Nunca exceder 175 °C contra acrilamida."
            )
        }
        OutlinedTextField(
            value = block5ThermalPcc,
            onValueChange = onBlock5Change,
            label = { Text("Sequência Térmica, Tempos & Termometria de Centro") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block5_input")
        )
    }

    // BLOCO 6 — Empratamento & Acabamento no Passe
    MepBlockCard(
        blockBadge = "BLOCO 6 / 8",
        title = "Empratamento & Temperatura de Passe",
        subtitle = "Logística térmica de louça, arquitetura visual, altura e brilho nappé."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                55 to "Quente (55 °C)",
                4 to "Frio (4 °C)",
                -12 to "Gelado (-12 °C)"
            ).forEach { (temp, label) ->
                val sel = plateTempC == temp
                OutlinedButton(
                    onClick = { onPlateTempChange(temp) },
                    border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) SwissRed else MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (sel) SwissRedContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        OutlinedTextField(
            value = block6Plating,
            onValueChange = onBlock6Change,
            label = { Text("Arquitetura de Empratamento & Guarnição") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block6_input")
        )
    }

    // BLOCO 7 — Higiene Crítica (HACCP / RDC 216) & Alergênicos
    val activeAllergens = allergensCsv.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    MepBlockCard(
        blockBadge = "BLOCO 7 / 8",
        title = "Boas Práticas (APPCC / RDC 216) & Alergênicos",
        subtitle = "Declaração obrigatória de alergênicos e rastreabilidade sanitária de bancada."
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ALLERGEN_CATALOG.forEach { allergen ->
                val isSelected = allergen in activeAllergens
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val next = if (isSelected) activeAllergens - allergen else activeAllergens + allergen
                        onAllergensCsvChange(next.joinToString(", "))
                    },
                    label = { Text(allergen) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PccAmberBg,
                        selectedLabelColor = PccAmber
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        OutlinedTextField(
            value = block7Hygiene,
            onValueChange = onBlock7Change,
            label = { Text("Controle Sanitário, Cadeia Fria ≤ 4 °C & Antissepsia") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block7_input")
        )
    }

    // BLOCO 8 — Autoavaliação de Bancada & Rendimento Real
    MepBlockCard(
        blockBadge = "BLOCO 8 / 8",
        title = "Autoavaliação de Bancada & Rendimento Real",
        subtitle = "Compare o peso pós-cocção aferido na balança com o rendimento teórico calculado."
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = actualBenchYieldText,
                onValueChange = onActualBenchYieldChange,
                label = { Text("Peso Real Pós-Cocção (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mep_actual_yield_input")
            )
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = "Teórico: ${"%.0f".format(summary.theoreticalCookedYieldGrams)} g",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                summary.benchYieldDeviationPercent?.let { dev ->
                    val absDev = abs(dev)
                    val (bg, fg) = when {
                        absDev <= 5.0 -> PccGreenBg to PccGreen
                        absDev <= 12.0 -> PccAmberBg to PccAmber
                        else -> PccDangerBg to PccDanger
                    }
                    TagPill(
                        text = "Desvio ${"%+.1f".format(dev)}%",
                        containerColor = bg,
                        contentColor = fg
                    )
                } ?: TagPill("Informe peso real", InfoBlueBg, InfoBlue)
            }
        }

        Text("Nota de Execução na Bancada (1 a 5):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { score ->
                val sel = executionScore1To5 == score
                OutlinedButton(
                    onClick = { onExecutionScoreChange(score) },
                    border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) SwissRed else MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (sel) SwissRedContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("mep_score_$score")
                ) {
                    Text("$score★", fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = block8Selbstkontrolle,
            onValueChange = onBlock8Change,
            label = { Text("Aprendizados Consolidados & Pontos de Melhoria") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mep_block8_input")
        )
    }
}

@Composable
fun MepBlockCard(
    blockBadge: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagPill(blockBadge, SwissRedContainer, OnSwissRedContainer)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

private fun toggleTokenInText(current: String, token: String, separator: String): String {
    val parts = current.split(separator.trim())
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableList()
    val existingIdx = parts.indexOfFirst { it.equals(token, ignoreCase = true) }
    if (existingIdx >= 0) {
        parts.removeAt(existingIdx)
    } else {
        parts.add(token)
    }
    return parts.joinToString(separator)
}
