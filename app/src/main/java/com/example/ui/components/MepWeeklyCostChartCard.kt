package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calculators.WeeklyMepCostAnalytics
import com.example.domain.calculators.WeeklyMepCostPoint
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.AtelierCharcoalLight
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
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MepWeeklyCostChartCard(
    analytics: WeeklyMepCostAnalytics,
    onOpenSheetById: (String) -> Unit,
    onCreateNewSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = Evolução Semanal (CMV Lote × Prime-Cost), 1 = Curva Acumulada (R$)
    var chartMode by rememberSaveable { mutableIntStateOf(0) }
    // false = apenas semanas com fichas criadas, true = panorama completo S01–S24
    var showAll24Weeks by rememberSaveable { mutableStateOf(false) }

    val displayedPoints: List<WeeklyMepCostPoint> = remember(analytics, showAll24Weeks) {
        if (showAll24Weeks) {
            analytics.all24WeekPoints
        } else {
            analytics.activeWeekPoints.ifEmpty { analytics.all24WeekPoints.take(6) }
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(displayedPoints, chartMode) {
        if (displayedPoints.isEmpty()) return@LaunchedEffect
        val cmvSeries = displayedPoints.map {
            if (chartMode == 0) it.totalBatchFoodCostBrl else it.cumulativeFoodCostBrl
        }
        val primeSeries = displayedPoints.map {
            if (chartMode == 0) it.totalPrimeCostBrl else it.cumulativePrimeCostBrl
        }
        modelProducer.runTransaction {
            columnSeries {
                series(cmvSeries)
            }
            lineSeries {
                series(primeSeries)
            }
        }
    }

    val weekLabels = remember(displayedPoints) {
        displayedPoints.map { "S%02d".format(it.weekNumber) }
    }

    val bottomFormatter = remember(weekLabels) {
        CartesianValueFormatter { _, x, _ ->
            weekLabels.getOrElse(x.toInt()) { "S--" }
        }
    }

    val startBrlFormatter = remember {
        CartesianValueFormatter { _, y, _ ->
            "R$ %.0f".format(y)
        }
    }

    val chartContentDesc = stringResource(R.string.cd_mep_cost_chart)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mep_weekly_cost_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = SwissRed
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.mep_cost_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.mep_cost_chart_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPill(
                    text = if (analytics.isUsingReferenceBaseline) {
                        stringResource(R.string.mep_cost_reference_badge)
                    } else {
                        "${analytics.totalSheetsCount} FICHAS SALVAS • ${analytics.activeWeekPoints.size} SEMANAS ATIVAS"
                    },
                    containerColor = if (analytics.isUsingReferenceBaseline) PccAmberBg else PccGreenBg,
                    contentColor = if (analytics.isUsingReferenceBaseline) PccAmber else PccGreen
                )

                analytics.latestDeltaPercent?.let { delta ->
                    val isIncrease = delta > 0.5
                    TagPill(
                        text = "Δ SEMANAL: ${if (delta >= 0) "+" else ""}${"%.1f".format(delta)}%",
                        containerColor = if (isIncrease) PccDangerBg else PccGreenBg,
                        contentColor = if (isIncrease) PccDanger else PccGreen
                    )
                }
            }

            // KPI Summary Strip (Charcoal High-Contrast Kitchen Strip)
            Card(
                colors = CardDefaults.cardColors(containerColor = AtelierCharcoal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FinancialKpiBox(
                            label = "CMV Total (Lotes)",
                            value = "R$ ${"%.2f".format(analytics.grandTotalFoodCostBrl)}",
                            sub = "Warenkosten somados",
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiBox(
                            label = "Prime-Cost Total",
                            value = "R$ ${"%.2f".format(analytics.grandTotalPrimeCostBrl)}",
                            sub = "CMV + Mão de Obra",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FinancialKpiBox(
                            label = "Média / Semana Ativa",
                            value = "R$ ${"%.2f".format(analytics.averageWeeklyFoodCostBrl)}",
                            sub = "Custo médio semanal",
                            modifier = Modifier.weight(1f)
                        )
                        val peak = analytics.peakWeek
                        FinancialKpiBox(
                            label = "Semana de Maior Custo",
                            value = if (peak != null) {
                                "S%02d • R$ %.0f".format(peak.weekNumber, peak.totalBatchFoodCostBrl)
                            } else {
                                "—"
                            },
                            sub = peak?.let { "${it.sheetsCount} ficha(s) • Ciclo ${it.cycleNumber}" } ?: "Sem registros",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Low-Touch Mode & Scope Filter Chips (>= 48.dp touch target)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = chartMode == 0,
                    onClick = { chartMode = 0 },
                    label = { Text(stringResource(R.string.mep_cost_mode_weekly), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mep_cost_mode_chip_0")
                )
                FilterChip(
                    selected = chartMode == 1,
                    onClick = { chartMode = 1 },
                    label = { Text(stringResource(R.string.mep_cost_mode_cumulative), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mep_cost_mode_chip_1")
                )
                FilterChip(
                    selected = !showAll24Weeks,
                    onClick = { showAll24Weeks = false },
                    label = { Text(stringResource(R.string.mep_cost_scope_active), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = InfoBlueBg,
                        selectedLabelColor = InfoBlue
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mep_cost_scope_chip_active")
                )
                FilterChip(
                    selected = showAll24Weeks,
                    onClick = { showAll24Weeks = true },
                    label = { Text(stringResource(R.string.mep_cost_scope_all24), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = InfoBlueBg,
                        selectedLabelColor = InfoBlue
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mep_cost_scope_chip_all24")
                )
            }

            // Legend Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendSwatch(
                    color = SwissRed,
                    label = if (chartMode == 0) "Colunas: CMV Insumos (R$)" else "Colunas: CMV Acumulado (R$)"
                )
                LegendSwatch(
                    color = InfoBlue,
                    label = if (chartMode == 0) "Linha: Prime-Cost Total (R$)" else "Linha: Prime-Cost Acumulado (R$)"
                )
            }

            // Vico Native Jetpack Compose Hardware-Accelerated Cartesian Chart
            ProvideVicoTheme(
                rememberM3VicoTheme(
                    columnCartesianLayerColors = listOf(SwissRed),
                    lineCartesianLayerColors = listOf(InfoBlue)
                )
            ) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = startBrlFormatter
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomFormatter
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .semantics { contentDescription = chartContentDesc }
                        .testTag("mep_weekly_cost_vico_chart")
                )
            }

            // Weekly Drill-Down Cards (Tap to inspect/open that week's MEP Technical Sheet)
            if (analytics.activeWeekPoints.isNotEmpty()) {
                Text(
                    text = "DETALHAMENTO SEMANAL POR LOTE (TOQUE PARA ABRIR A FICHA)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    analytics.activeWeekPoints.forEach { pt ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .width(225.dp)
                                .clickable {
                                    pt.sheetIds.firstOrNull()?.let(onOpenSheetById)
                                }
                                .testTag("mep_cost_week_card_${pt.weekNumber}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TagPill(
                                        text = "S%02d • C%d".format(pt.weekNumber, pt.cycleNumber),
                                        containerColor = SwissRedContainer,
                                        contentColor = OnSwissRedContainer
                                    )
                                    Text(
                                        text = "${pt.sheetsCount} ficha(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "CMV: R$ ${"%.2f".format(pt.totalBatchFoodCostBrl)}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SwissRed
                                )
                                Text(
                                    text = "Prime: R$ ${"%.2f".format(pt.totalPrimeCostBrl)} • CMV% ${"%.1f".format(pt.averageCmvPercent)}%",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = pt.sheetTitles.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onCreateNewSheet,
                colors = ButtonDefaults.buttonColors(containerColor = AtelierCharcoal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("mep_cost_create_sheet_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mep_cost_create_cta), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinancialKpiBox(
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
private fun LegendSwatch(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
