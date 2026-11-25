package com.example.domain.calculators

import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity

data class WeeklyMepCostPoint(
    val weekNumber: Int,
    val cycleNumber: Int,
    val sheetsCount: Int,
    val totalServingsProduced: Int,
    val totalBatchFoodCostBrl: Double,
    val totalBatchLaborCostBrl: Double,
    val totalPrimeCostBrl: Double,
    val averagePortionFoodCostBrl: Double,
    val averageCmvPercent: Double,
    val cumulativeFoodCostBrl: Double,
    val cumulativePrimeCostBrl: Double,
    val sheetIds: List<String>,
    val sheetTitles: List<String>
)

data class WeeklyMepCostAnalytics(
    val all24WeekPoints: List<WeeklyMepCostPoint>,
    val activeWeekPoints: List<WeeklyMepCostPoint>,
    val totalSheetsCount: Int,
    val grandTotalFoodCostBrl: Double,
    val grandTotalPrimeCostBrl: Double,
    val averageWeeklyFoodCostBrl: Double,
    val peakWeek: WeeklyMepCostPoint?,
    val latestDeltaPercent: Double?,
    val isUsingReferenceBaseline: Boolean
)

object MepFinancialAnalyticsEngine {

    /**
     * Agrega em memória (O(N) sobre fichas locais Room, zero leituras no Firestore)
     * a evolução semanal do custo total de insumos (CMV de lote / Warenkosten),
     * mão de obra direta (Personalkosten) e Prime-Cost das Fichas Técnicas MEP.
     */
    fun computeWeeklyEvolution(
        persistedSheets: List<MepSheetEntity>,
        linesByRecipeId: Map<String, List<MepIngredientLineEntity>>,
        fallbackLinesProvider: (String) -> List<MepIngredientLineEntity>,
        referenceFallbackSheet: MepSheetEntity? = null
    ): WeeklyMepCostAnalytics {
        val usingBaseline = persistedSheets.isEmpty() && referenceFallbackSheet != null
        val effectiveSheets = if (persistedSheets.isNotEmpty()) {
            persistedSheets
        } else {
            listOfNotNull(referenceFallbackSheet)
        }

        val sheetsByWeek = effectiveSheets.groupBy { it.weekNumber.coerceIn(1, 24) }

        var runningCumulativeFood = 0.0
        var runningCumulativePrime = 0.0

        val all24 = (1..24).map { week ->
            val cycle = ((week - 1) / 6) + 1
            val weekSheets = sheetsByWeek[week].orEmpty()

            if (weekSheets.isEmpty()) {
                WeeklyMepCostPoint(
                    weekNumber = week,
                    cycleNumber = cycle,
                    sheetsCount = 0,
                    totalServingsProduced = 0,
                    totalBatchFoodCostBrl = 0.0,
                    totalBatchLaborCostBrl = 0.0,
                    totalPrimeCostBrl = 0.0,
                    averagePortionFoodCostBrl = 0.0,
                    averageCmvPercent = 0.0,
                    cumulativeFoodCostBrl = runningCumulativeFood,
                    cumulativePrimeCostBrl = runningCumulativePrime,
                    sheetIds = emptyList(),
                    sheetTitles = emptyList()
                )
            } else {
                var weekFoodCost = 0.0
                var weekLaborCost = 0.0
                var weekServings = 0
                var weightedBvpSum = 0.0

                weekSheets.forEach { sheet ->
                    val lines = linesByRecipeId[sheet.recipeId]
                        ?.takeIf { it.isNotEmpty() }
                        ?: fallbackLinesProvider(sheet.recipeId)
                    val safeTargetServings = sheet.targetServings.coerceAtLeast(1)
                    val summary = CulinaryCalculators.calculateMepSheetSummary(
                        baseServings = sheet.baseServings,
                        targetServings = safeTargetServings,
                        cookingShrinkagePercent = sheet.cookingShrinkagePercent,
                        targetCmvPercent = sheet.targetCmvPercent,
                        directLaborPerPortionBrl = sheet.directLaborCost,
                        actualBenchYieldGrams = sheet.actualBenchYieldGrams,
                        ingredientLines = lines
                    )
                    val batchLabor = sheet.directLaborCost.coerceAtLeast(0.0) * safeTargetServings
                    weekFoodCost += summary.totalBatchFoodCostBrl
                    weekLaborCost += batchLabor
                    weekServings += safeTargetServings
                    weightedBvpSum += summary.pricingPerPortion.netSellingPriceBvpBrl * safeTargetServings
                }

                val weekPrime = weekFoodCost + weekLaborCost
                runningCumulativeFood += weekFoodCost
                runningCumulativePrime += weekPrime

                val avgPortionFood = if (weekServings > 0) weekFoodCost / weekServings else 0.0
                val avgCmvPct = if (weightedBvpSum > 0.0) (weekFoodCost / weightedBvpSum) * 100.0 else 0.0

                WeeklyMepCostPoint(
                    weekNumber = week,
                    cycleNumber = cycle,
                    sheetsCount = weekSheets.size,
                    totalServingsProduced = weekServings,
                    totalBatchFoodCostBrl = weekFoodCost,
                    totalBatchLaborCostBrl = weekLaborCost,
                    totalPrimeCostBrl = weekPrime,
                    averagePortionFoodCostBrl = avgPortionFood,
                    averageCmvPercent = avgCmvPct,
                    cumulativeFoodCostBrl = runningCumulativeFood,
                    cumulativePrimeCostBrl = runningCumulativePrime,
                    sheetIds = weekSheets.map { it.recipeId },
                    sheetTitles = weekSheets.map { it.title.ifBlank { it.recipeId } }
                )
            }
        }

        val activePoints = all24.filter { it.sheetsCount > 0 }
        val totalSheets = if (usingBaseline) 0 else effectiveSheets.size
        val avgWeeklyFood = if (activePoints.isNotEmpty()) {
            runningCumulativeFood / activePoints.size
        } else {
            0.0
        }
        val peak = activePoints.maxByOrNull { it.totalBatchFoodCostBrl }

        val deltaPct = if (activePoints.size >= 2) {
            val prev = activePoints[activePoints.size - 2].totalBatchFoodCostBrl
            val curr = activePoints.last().totalBatchFoodCostBrl
            if (prev > 0.001) ((curr - prev) / prev) * 100.0 else null
        } else {
            null
        }

        return WeeklyMepCostAnalytics(
            all24WeekPoints = all24,
            activeWeekPoints = activePoints,
            totalSheetsCount = totalSheets,
            grandTotalFoodCostBrl = runningCumulativeFood,
            grandTotalPrimeCostBrl = runningCumulativePrime,
            averageWeeklyFoodCostBrl = avgWeeklyFood,
            peakWeek = peak,
            latestDeltaPercent = deltaPct,
            isUsingReferenceBaseline = usingBaseline
        )
    }
}
