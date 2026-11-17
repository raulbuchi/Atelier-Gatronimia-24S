package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.calculators.CulinaryCalculators
import com.example.domain.calculators.HydrocolloidAgent
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HydrocolloidCalculatorSection() {
    var gelatinInputText by remember { mutableStateOf("12") }
    var inputIsSheets by remember { mutableStateOf(false) }
    var liquidBaseGramsText by remember { mutableStateOf("500") }
    var selectedAgent by remember { mutableStateOf(HydrocolloidAgent.XANTHAN) }

    val calc = CulinaryCalculators.calculateGelatinAndHydrocolloid(
        gelatinInputAmount = gelatinInputText.toDoubleOrNull() ?: 12.0,
        inputIsSheets = inputIsSheets,
        liquidBaseGrams = liquidBaseGramsText.toDoubleOrNull() ?: 500.0,
        agent = selectedAgent
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculator_f_hydrocolloids")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "(f) Conversor de Gelatina & Hidrocoloides (v5)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TagPill("PETERSON • SENAC", PccGreenBg, PccGreen)
            }
            Text(
                text = "Regra oficial Canella-Rawls / Senac: 1 envelope (12 g pó) = 6 folhas (~2 g/folha) • 1 colher de chá nivelada (cc) = 1 folha • Hidratar pó em 5× água fria.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !inputIsSheets,
                    onClick = {
                        inputIsSheets = false
                        gelatinInputText = "12"
                    },
                    label = { Text("Entrada em Gramas (Pó)") },
                    modifier = Modifier.testTag("gelatin_mode_powder")
                )
                FilterChip(
                    selected = inputIsSheets,
                    onClick = {
                        inputIsSheets = true
                        gelatinInputText = "6"
                    },
                    label = { Text("Entrada em Folhas") },
                    modifier = Modifier.testTag("gelatin_mode_sheets")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gelatinInputText,
                    onValueChange = { gelatinInputText = it },
                    label = { Text(if (inputIsSheets) "Qtd. Folhas" else "Gelatina Pó (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gelatin_input_field")
                )
                OutlinedTextField(
                    value = liquidBaseGramsText,
                    onValueChange = { liquidBaseGramsText = it },
                    label = { Text("Base Líquida / Molho (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hydrocolloid_base_field")
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = InfoBlueBg),
                border = BorderStroke(1.dp, InfoBlue)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "EQUIVALÊNCIA DE GELATINA: ${"%.1f".format(calc.equivalentPowderGrams)} g pó = ${"%.1f".format(calc.equivalentSheets)} folhas = ${"%.2f".format(calc.equivalentEnvelopes12g)} env. (12 g) = ${"%.1f".format(calc.equivalentTeaspoonsCc)} cc",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Água fria p/ hidratar o pó (5×): ${"%.1f".format(calc.hydrationWaterGrams)} g (fundir a 45–55 °C sem ferver).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()
            Text(
                text = "Dosagem de Hidrocoloide para ${"%.0f".format(calc.liquidBaseGrams)} g de base:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HydrocolloidAgent.entries.forEach { agent ->
                    FilterChip(
                        selected = selectedAgent == agent,
                        onClick = { selectedAgent = agent },
                        label = { Text(agent.labelPt.substringBefore(" (")) }
                    )
                }
            }

            Text(
                text = "${selectedAgent.labelPt} → Mín: ${"%.2f".format(calc.minDosageGrams)} g | Alvo: ${"%.2f".format(calc.recommendedDosageGrams)} g | Máx: ${"%.2f".format(calc.maxDosageGrams)} g",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hidratação/Temperatura: ${selectedAgent.thermalHydrationRule}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Aplicação: ${selectedAgent.culinaryUseNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StarchComposition8020Card() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PccAmberBg),
        border = BorderStroke(1.5.dp, PccAmber),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BIOQUÍMICA DO AMIDO: 80% AMILOPECTINA / 20% AMILOSE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TagPill("80 / 20", PccGreenBg, PccGreen)
            }
            Text(
                text = "• Amilopectina (~80% — cadeia ramificada): confere viscosidade acetinada, brilho, retenção de água e estabilidade ao congelamento (ex.: risotos Carnaroli all'onda, féculas e amido ceroso).\n" +
                    "• Amilose (~20% — cadeia linear helicoidal): forma géis firmes de corte após resfriamento, mas sofre retrogradação e sinérese no congelamento (essencial retrogradar batata por 24 h a 4 °C antes da Rösti!).\n" +
                    "• Alerta Canella-Rawls: em cremes com gema + amido (Crème Pâtissière), ferver plenamente (98–100 °C por 1–2 min) é obrigatório para inativar a enzima alfa-amilase da gema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
