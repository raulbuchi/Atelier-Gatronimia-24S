package com.example.domain.calculators

import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fórmulas de Fachrechnen, precificação suíça, SM-2 e Ficha MEP (JVM puro). */
class CulinaryCalculatorsTest {

    private val eps = 1e-6

    // ---------- SM-2 ----------

    @Test
    fun sm2_firstTwoSuccessfulReviews_use1And6DayIntervals() {
        val first = CulinaryCalculators.computeNextSm2State(null, "fc-1", 5, todayEpochDay = 100)
        assertEquals(1, first.repetitions)
        assertEquals(1, first.intervalDays)
        assertEquals(101L, first.nextReviewEpochDay)
        assertEquals(2.6, first.easinessFactor, eps)
        assertEquals(5, first.lastGrade)

        val second = CulinaryCalculators.computeNextSm2State(first, "fc-1", 5, todayEpochDay = 101)
        assertEquals(2, second.repetitions)
        assertEquals(6, second.intervalDays)
        assertEquals(107L, second.nextReviewEpochDay)
        assertEquals(2.7, second.easinessFactor, eps)
    }

    @Test
    fun sm2_thirdReview_multipliesPreviousIntervalByNewEasiness() {
        val second = CulinaryCalculators.computeNextSm2State(
            CulinaryCalculators.computeNextSm2State(null, "fc", 5, 0), "fc", 5, 1
        )
        // q = 4 mantém o EF (0,1 − 1 × 0,1 = 0) → intervalo = round(6 × 2,7) = 16.
        val third = CulinaryCalculators.computeNextSm2State(second, "fc", 4, 7)
        assertEquals(3, third.repetitions)
        assertEquals(2.7, third.easinessFactor, eps)
        assertEquals(16, third.intervalDays)
        assertEquals(23L, third.nextReviewEpochDay)
    }

    @Test
    fun sm2_failedReview_resetsRepetitionsAndPenalizesEasiness() {
        val learned = Sm2SeedHelper.card(repetitions = 4, ef = 2.7, interval = 40)
        val failed = CulinaryCalculators.computeNextSm2State(learned, "fc", 2, 500)
        assertEquals(0, failed.repetitions)
        assertEquals(1, failed.intervalDays)
        assertEquals(501L, failed.nextReviewEpochDay)
        assertEquals(2.7 - 0.32, failed.easinessFactor, eps)
    }

    @Test
    fun sm2_easinessNeverDropsBelow1_3_andGradesAreClamped() {
        val hard = Sm2SeedHelper.card(repetitions = 1, ef = 1.4, interval = 1)
        assertEquals(1.3, CulinaryCalculators.computeNextSm2State(hard, "fc", 0, 0).easinessFactor, eps)
        assertEquals(5, CulinaryCalculators.computeNextSm2State(null, "fc", 9, 0).lastGrade)
        assertEquals(0, CulinaryCalculators.computeNextSm2State(null, "fc", -3, 0).lastGrade)
    }

    // ---------- Precificação suíça ----------

    @Test
    fun swissPricing_computesBvpEvpPrimeCostAndResults() {
        val p = CulinaryCalculators.calculateSwissPricing(
            foodCostWarenkosten = 10.0,
            directLaborPersonalkosten = 5.0,
            overheadGemeinkosten = 3.5,
            targetFoodCostPercent = 25.0,
            mwstRatePercent = 8.0
        )
        assertEquals(40.0, p.netSellingPriceBvpBrl, eps)
        assertEquals(43.2, p.grossMenuPriceEvpBrl, eps)
        assertEquals(15.0, p.primeCostBrl, eps)
        assertEquals(30.0, p.grossMarginBe1Brl, eps)
        assertEquals(21.5, p.operatingProfitBe2Brl, eps)
        assertEquals(25.0, p.foodCostPercent, eps)
    }

    @Test
    fun swissPricing_clampsUnrealisticTargetsAndHandlesZeroFoodCost() {
        val clamped = CulinaryCalculators.calculateSwissPricing(10.0, 0.0, 0.0, targetFoodCostPercent = 5.0)
        assertEquals("meta abaixo de 10% é limitada a 10%", 100.0, clamped.netSellingPriceBvpBrl, eps)

        val noFood = CulinaryCalculators.calculateSwissPricing(0.0, 8.0, 2.0, 30.0)
        assertEquals(12.5, noFood.netSellingPriceBvpBrl, eps)
        assertEquals(0.0, noFood.foodCostPercent, eps)
    }

    // ---------- Ponto de equilíbrio e nota suíça ----------

    @Test
    fun breakEven_roundsUpToWholeUnits() {
        assertEquals(200, CulinaryCalculators.calculateBreakEvenAndSwissGrade(3000.0, 25.0, 10.0, 0.0, 1.0).breakEvenUnits)
        assertEquals(201, CulinaryCalculators.calculateBreakEvenAndSwissGrade(3001.0, 25.0, 10.0, 0.0, 1.0).breakEvenUnits)
        val r = CulinaryCalculators.calculateBreakEvenAndSwissGrade(3000.0, 25.0, 10.0, 0.0, 1.0)
        assertEquals(15.0, r.contributionMarginPerUnit, eps)
    }

    @Test
    fun breakEven_withPriceBelowVariableCost_usesMinimumMarginInsteadOfDividingByZero() {
        val r = CulinaryCalculators.calculateBreakEvenAndSwissGrade(100.0, 10.0, 12.0, 0.0, 1.0)
        assertEquals(0.01, r.contributionMarginPerUnit, eps)
        assertEquals(10_000, r.breakEvenUnits)
    }

    @Test
    fun swissGrade_followsPointsOverMaxTimes5Plus1_roundedToTenths() {
        fun grade(points: Double, max: Double) =
            CulinaryCalculators.calculateBreakEvenAndSwissGrade(0.0, 1.0, 0.0, points, max).swissGrade1To6
        assertEquals(1.0, grade(0.0, 60.0), eps)
        assertEquals(6.0, grade(60.0, 60.0), eps)
        assertEquals(4.8, grade(45.0, 60.0), eps) // 4,75 → 4,8
        assertEquals(3.5, grade(30.0, 60.0), eps)
        assertEquals(6.0, grade(80.0, 60.0), eps)
    }

    // ---------- Perdas em cascata ----------

    @Test
    fun cascadeLosses_multiplyYieldsAndNeverSumPercentages() {
        val r = CulinaryCalculators.calculateCascadeLosses(200.0, listOf(10.0, 10.0))
        assertEquals(0.81, r.combinedYieldFactor, eps)
        assertEquals(19.0, r.totalWastePercent, eps) // não 20%
        assertEquals(200.0 / 0.81, r.grossPurchaseGrams, eps)
        assertEquals(1 / 0.81, r.correctionFactorFc, eps)
    }

    @Test
    fun cascadeLosses_clampExtremeInputs() {
        val r = CulinaryCalculators.calculateCascadeLosses(0.0, listOf(99.0))
        assertEquals(1.0, r.netPortionGrams, eps)
        assertEquals(0.05, r.combinedYieldFactor, eps)
        assertEquals(20.0, r.grossPurchaseGrams, eps)
        assertEquals(1.0, CulinaryCalculators.calculateCascadeLosses(150.0, emptyList()).combinedYieldFactor, eps)
    }

    // ---------- Ficha MEP: unidades e Fator de Correção ----------

    private fun line(
        unit: String,
        pl: Double,
        trim: Double = 0.0,
        fc: Double = 1.0,
        cost: Double = 10.0
    ) = MepIngredientLineEntity(
        recipeId = "r",
        name = "insumo",
        unit = unit,
        baseNetPl = pl,
        trimLossPercent = trim,
        correctionFactorFc = fc,
        unitCostPerKgOrL = cost
    )

    private fun summary(lines: List<MepIngredientLineEntity>, base: Int = 4, target: Int = 4, bench: Double = 0.0) =
        CulinaryCalculators.calculateMepSheetSummary(base, target, 0.0, 30.0, 0.0, bench, lines)

    @Test
    fun mep_trimLossTakesPrecedenceOverManualFc() {
        val fromTrim = summary(listOf(line("g", 1000.0, trim = 20.0, fc = 1.4))).lines.single()
        assertEquals(1.25, fromTrim.effectiveFc, eps)
        assertEquals(1250.0, fromTrim.scaledPb, eps)

        val fromFc = summary(listOf(line("g", 1000.0, trim = 0.0, fc = 1.4))).lines.single()
        assertEquals(1.4, fromFc.effectiveFc, eps)
        assertEquals((1 - 1 / 1.4) * 100, fromFc.effectiveTrimLossPercent, eps)
    }

    @Test
    fun mep_fcBelowOneIsTreatedAsOne() {
        val l = summary(listOf(line("g", 500.0, fc = 0.8))).lines.single()
        assertEquals(1.0, l.effectiveFc, eps)
        assertEquals(500.0, l.scaledPb, eps)
    }

    @Test
    fun mep_costsByUnit_gramsKilosAndUnits() {
        val s = summary(
            listOf(
                line("g", 500.0, cost = 20.0),   // 0,5 kg × 20 = 10
                line("kg", 2.0, cost = 5.0),     // 2 kg × 5 = 10
                line("un", 6.0, cost = 0.75)     // 6 un × 0,75 = 4,5
            )
        )
        assertEquals(listOf(10.0, 10.0, 4.5), s.lines.map { it.lineCostBrl })
        assertEquals(24.5, s.totalBatchFoodCostBrl, eps)
        // Equivalente em gramas: 500 g + 2000 g + 6 × 55 g.
        assertEquals(2830.0, s.totalScaledPlGramsEquiv, eps)
    }

    @Test
    fun mep_invalidServingsAreCoercedAndMissingBenchYieldHasNoDeviation() {
        val s = summary(listOf(line("g", 400.0)), base = 4, target = 0)
        assertEquals(0.25, s.scaleFactor, eps)
        assertEquals(100.0, s.totalScaledPlGramsEquiv, eps)
        assertNull(s.benchYieldDeviationPercent)
    }

    // ---------- Gelatina e hidrocoloides ----------

    @Test
    fun gelatin_sheetsInputConvertsToPowderAndEnvelopes() {
        val r = CulinaryCalculators.calculateGelatinAndHydrocolloid(6.0, inputIsSheets = true, 1000.0, HydrocolloidAgent.AGAR_FIRM)
        assertEquals(12.0, r.equivalentPowderGrams, eps)
        assertEquals(1.0, r.equivalentEnvelopes12g, eps)
        assertEquals(60.0, r.hydrationWaterGrams, eps)
    }

    @Test
    fun hydrocolloidAgents_haveConsistentDosageRanges() {
        HydrocolloidAgent.entries.forEach { agent ->
            assertTrue("${agent.name}: mín ≤ recomendado", agent.minPercent <= agent.recommendedPercent)
            assertTrue("${agent.name}: recomendado ≤ máx", agent.recommendedPercent <= agent.maxPercent)
            assertTrue("${agent.name}: dosagem positiva", agent.minPercent > 0.0)
            val r = CulinaryCalculators.calculateGelatinAndHydrocolloid(1.0, false, 1000.0, agent)
            assertEquals(agent.recommendedPercent * 10.0, r.recommendedDosageGrams, eps)
        }
    }

    // ---------- Evolução semanal de custos ----------

    @Test
    fun weeklyAnalytics_withoutSheets_usesReferenceBaselineButReportsZeroSheets() {
        val reference = MepSheetEntity(recipeId = "ref", weekNumber = 8, baseServings = 4, targetServings = 4)
        val analytics = MepFinancialAnalyticsEngine.computeWeeklyEvolution(
            persistedSheets = emptyList(),
            linesByRecipeId = emptyMap(),
            fallbackLinesProvider = { listOf(line("kg", 1.0, cost = 30.0)) },
            referenceFallbackSheet = reference
        )
        assertTrue(analytics.isUsingReferenceBaseline)
        assertEquals(0, analytics.totalSheetsCount)
        assertEquals(8, analytics.activeWeekPoints.single().weekNumber)
        assertEquals(30.0, analytics.grandTotalFoodCostBrl, eps)
        assertNull(analytics.latestDeltaPercent)
    }

    @Test
    fun weeklyAnalytics_clampsOutOfRangeWeeks() {
        val sheet = MepSheetEntity(recipeId = "s", weekNumber = 99, baseServings = 1, targetServings = 1)
        val analytics = MepFinancialAnalyticsEngine.computeWeeklyEvolution(
            listOf(sheet), mapOf("s" to listOf(line("g", 100.0))), { emptyList() }
        )
        assertFalse(analytics.isUsingReferenceBaseline)
        assertEquals(24, analytics.activeWeekPoints.single().weekNumber)
    }
}

private object Sm2SeedHelper {
    fun card(repetitions: Int, ef: Double, interval: Int) = com.example.data.local.Sm2ProgressEntity(
        cardId = "fc",
        repetitions = repetitions,
        easinessFactor = ef,
        intervalDays = interval,
        nextReviewEpochDay = 0,
        lastGrade = 4
    )
}
