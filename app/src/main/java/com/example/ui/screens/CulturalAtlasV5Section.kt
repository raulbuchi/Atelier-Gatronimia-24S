package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.CulturalScientificAtlasV5
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CulturalAtlasV5Section() {
    var selectedSchool by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "ATLAS CULTURAL & CIENTÍFICO RÁPIDO (v5)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Fundamentos comparados de Marcella Hazan (Itália), Luis Carlos Paravati & Carlos Alberto Dória (Brasil — 5 Regiões) e Mina Holland (Volta ao Mundo — 7 Refogados & 9 Blends).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSchool == 0,
                onClick = { selectedSchool = 0 },
                label = { Text("Itália (Marcella Hazan)") },
                modifier = Modifier.testTag("cultural_tab_italy")
            )
            FilterChip(
                selected = selectedSchool == 1,
                onClick = { selectedSchool = 1 },
                label = { Text("Brasil — 5 Regiões (Paravati & Dória)") },
                modifier = Modifier.testTag("cultural_tab_brazil")
            )
            FilterChip(
                selected = selectedSchool == 2,
                onClick = { selectedSchool = 2 },
                label = { Text("Mundo — 7 Refogados & 9 Blends (Holland)") },
                modifier = Modifier.testTag("cultural_tab_world")
            )
        }

        when (selectedSchool) {
            0 -> ItalianHazanCards()
            1 -> BrazilFiveRegionsCards()
            else -> WorldHollandAromaticsAndSpicesCards()
        }
    }
}

@Composable
private fun ItalianHazanCards() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CulturalScientificAtlasV5.italianFoundationsHazan.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.pillarTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TagPill("MARCELLA HAZAN", InfoBlueBg, InfoBlue)
                    }
                    Text(
                        text = item.italianTerm,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.technicalPrinciple,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PccDangerBg),
                        border = BorderStroke(1.dp, PccDanger)
                    ) {
                        Text(
                            text = "ERRO DE BANCADA A EVITAR: ${item.benchErrorToAvoid}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrazilFiveRegionsCards() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PccGreenBg),
            border = BorderStroke(1.5.dp, PccGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = CulturalScientificAtlasV5.BRAZIL_BIODIVERSITY_CHAIN,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
        }

        CulturalScientificAtlasV5.brazilFiveRegionsMatrices.forEach { reg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reg.regionName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TagPill("PARAVATI & DÓRIA", PccGreenBg, PccGreen)
                    }
                    Text("Ingredientes de Bioma: ${reg.signatureIngredients}", style = MaterialTheme.typography.bodySmall)
                    Text("Produtos Derivados: ${reg.derivedProducts}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Receitas Emblemáticas: ${reg.emblemRecipes}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "Controle Técnico / PCC: ${reg.technicalControlNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PccAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldHollandAromaticsAndSpicesCards() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "OS 7 REFOGADOS AROMÁTICOS REGIONAIS (MINA HOLLAND)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        CulturalScientificAtlasV5.worldSevenAromaticBases.forEach { base ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(base.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TagPill(base.originSchool, InfoBlueBg, InfoBlue)
                    }
                    Text("Composição: ${base.proportionAndIngredients}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Gordura & Método Térmico: ${base.fatAndThermalMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "AS 9 MISTURAS GLOBAIS DE ESPECIARIAS (MINA HOLLAND)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        CulturalScientificAtlasV5.worldNineSpiceBlends.forEach { spice ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(spice.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TagPill(spice.region, PccAmberBg, PccAmber)
                    }
                    Text("Especiarias-Chave: ${spice.coreSpices}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Aplicação na Praça: ${spice.culinaryApplication}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
