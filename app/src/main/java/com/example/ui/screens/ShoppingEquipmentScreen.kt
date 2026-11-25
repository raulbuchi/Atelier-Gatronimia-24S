package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.ShoppingFifoEntity
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingEquipmentScreen(
    pantryVm: PantryViewModel
) {
    val selectedWeekNum by pantryVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val shoppingItems by pantryVm.shoppingItems.collectAsStateWithLifecycle()
    val ownedEquipmentIds by pantryVm.ownedEquipmentIds.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Compras & Estoque PVPS", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("shopping_tab_fifo")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Bateria & Cutelaria (4 Níveis)", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("shopping_tab_equipment")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (subTab == 0) {
                ShoppingFifoSection(
                    selectedWeekNum = selectedWeekNum,
                    items = shoppingItems,
                    onSelectWeek = { pantryVm.selectWeek(it) },
                    onGenerateWeekList = { mult -> pantryVm.generateShoppingForWeek(selectedWeekNum, mult) },
                    onTogglePurchased = { pantryVm.toggleShoppingPurchased(it) },
                    onUpdateFifo = { item, batch, exp -> pantryVm.updateShoppingFifoDates(item, batch, exp) }
                )
            } else {
                EquipmentTiersChecklistSection(
                    selectedWeekNum = selectedWeekNum,
                    ownedIds = ownedEquipmentIds,
                    onToggleOwned = { pantryVm.toggleEquipmentOwned(it) }
                )
            }
        }
    }
}

@Composable
private fun ShoppingFifoSection(
    selectedWeekNum: Int,
    items: List<ShoppingFifoEntity>,
    onSelectWeek: (Int) -> Unit,
    onGenerateWeekList: (Double) -> Unit,
    onTogglePurchased: (ShoppingFifoEntity) -> Unit,
    onUpdateFifo: (ShoppingFifoEntity, String, String) -> Unit
) {
    var scaleMultiplier by rememberSaveable { mutableDoubleStateOf(1.0) }

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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("REQUISIÇÃO DE COMPRAS • PB = PL × FC • GIRO PVPS/PEPS", SwissRedContainer, OnSwissRedContainer)
            Text(
                "Consolida automaticamente todos os insumos das fichas técnicas da Semana $selectedWeekNum agrupados por setor de recebimento.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { scaleMultiplier = 1.0 },
                    border = BorderStroke(if (scaleMultiplier == 1.0) 2.dp else 1.dp, if (scaleMultiplier == 1.0) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Base 1,0x (4 porções)")
                }
                OutlinedButton(
                    onClick = { scaleMultiplier = 2.5 },
                    border = BorderStroke(if (scaleMultiplier == 2.5) 2.dp else 1.dp, if (scaleMultiplier == 2.5) SwissRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Escala 2,5x (10 porções)")
                }
            }
            Button(
                onClick = { onGenerateWeekList(scaleMultiplier) },
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("consolidate_week_shopping_button")
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gerar Requisição da Semana $selectedWeekNum (${scaleMultiplier}x)", fontWeight = FontWeight.Bold)
            }
        }
    }

    val weekItems = items.filter { it.weekNumber == selectedWeekNum }
    if (weekItems.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = InfoBlueBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Toque em 'Gerar Requisição da Semana $selectedWeekNum' para apurar Peso Líquido (PL), Fator de Correção (FC), Peso Bruto de Compra (PB) e controle de validade PVPS/PEPS.",
                style = MaterialTheme.typography.bodyMedium,
                color = InfoBlue,
                modifier = Modifier.padding(14.dp)
            )
        }
    } else {
        val grouped = weekItems.groupBy { it.sector }
        grouped.forEach { (sector, sectorItems) ->
            Text(sector.uppercase(), style = MaterialTheme.typography.labelLarge, color = SwissRed, fontWeight = FontWeight.Bold)
            sectorItems.forEach { item ->
                var batch by remember(item.id, item.batchDate) { mutableStateOf(item.batchDate) }
                var expiry by remember(item.id, item.expiryDate) { mutableStateOf(item.expiryDate) }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isPurchased) PccGreenBg.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isPurchased,
                                onCheckedChange = { onTogglePurchased(item) },
                                modifier = Modifier.testTag("shopping_check_${item.id}")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.ingredientName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "PL ${"%.0f".format(item.netQuantityPl)}${item.unit} × FC ${"%.2f".format(item.correctionFactorFc)} = PB ${"%.0f".format(item.grossQuantityPb)}${item.unit}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = batch,
                                onValueChange = {
                                    batch = it
                                    onUpdateFifo(item, it, expiry)
                                },
                                label = { Text("Lote de Entrada") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expiry,
                                onValueChange = {
                                    expiry = it
                                    onUpdateFifo(item, batch, it)
                                },
                                label = { Text("Validade PVPS/PEPS") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentTiersChecklistSection(
    selectedWeekNum: Int,
    ownedIds: Set<String>,
    onToggleOwned: (String) -> Unit
) {
    val weekRequiredIds = remember(selectedWeekNum) {
        SwissmilkAndBaseRecipesCatalog.recipesForWeek(selectedWeekNum)
            .flatMap { it.requiredEquipmentIds }
            .toSet()
    }
    val missingForWeek = weekRequiredIds.filterNot { it in ownedIds }
    val totalCatalogItems = TechnicalAtlasCatalog.allEquipmentTierItems.size

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (missingForWeek.isEmpty()) PccGreenBg else PccAmberBg
        ),
        border = BorderStroke(1.dp, if (missingForWeek.isEmpty()) PccGreen else PccAmber),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, tint = if (missingForWeek.isEmpty()) PccGreen else PccAmber)
                Text(
                    text = if (missingForWeek.isEmpty()) {
                        "Enxoval pronto para a Semana $selectedWeekNum!"
                    } else {
                        "Atenção Semana $selectedWeekNum: ${missingForWeek.size} equipamento(s) pendente(s) no inventário"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Marque abaixo os equipamentos que você já possui na sua cozinha (${ownedIds.size}/$totalCatalogItems nos 4 ciclos oficiais).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    TechnicalAtlasCatalog.equipmentTiers.forEach { tierSpec ->
        Text(
            text = "Nível ${tierSpec.tierNumber} — ${tierSpec.title} (${tierSpec.items.size} itens)",
            style = MaterialTheme.typography.titleSmall,
            color = SwissRed,
            fontWeight = FontWeight.Bold
        )
        tierSpec.items.forEach { eq ->
            val isOwned = eq.id in ownedIds
            val isNeededThisWeek = eq.id in weekRequiredIds
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    if (isNeededThisWeek && !isOwned) 1.5.dp else 1.dp,
                    if (isNeededThisWeek && !isOwned) PccAmber else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleOwned(eq.id) }
                    .testTag("equipment_item_${eq.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isOwned,
                        onCheckedChange = { onToggleOwned(eq.id) }
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(eq.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (isNeededThisWeek) {
                                TagPill("EXIGIDO S${"%02d".format(selectedWeekNum)}", SwissRedContainer, OnSwissRedContainer)
                            }
                        }
                        Text(
                            "ID: ${eq.id} • Categoria: ${eq.category}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = InfoBlue
                        )
                    }
                }
            }
        }
    }
}
