package com.example.domain.calculators

import com.example.data.local.Sm2ProgressEntity
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class CascadeLossResult(
    val netPortionGrams: Double,
    val combinedYieldFactor: Double,
    val correctionFactorFc: Double,
    val grossPurchaseGrams: Double,
    val totalWastePercent: Double
)

data class SwissPricingResult(
    val foodCostChf: Double,
    val laborDirectChf: Double,
    val primeCostChf: Double,
    val grossMarginBe1Chf: Double,
    val operatingProfitBe2Chf: Double,
    val netSellingPriceBvpChf: Double,
    val grossMenuPriceEvpChf: Double, // Incl. MWST 8.0%
    val foodCostPercent: Double
)

data class BreakEvenAndGradeResult(
    val breakEvenUnits: Int,
    val contributionMarginPerUnit: Double,
    val swissGrade1To6: Double
)

data class SousVideBrineResult(
    val waterLiters: Double,
    val brinePercent: Double,
    val saltGramsRequired: Double,
    val recommendedBrineMinutes: Int,
    val targetCoreTempC: Double,
    val recommendedBathTempC: Double,
    val technicalProtocolNote: String
)

data class PuffPastrySimResult(
    val flourGrams: Double,
    val totalFatGrams: Double,
    val waterGrams: Double,
    val saltGrams: Double,
    val maltGrams: Double,
    val totalDoughGrams: Double,
    val fatToFlourRatio: Double,
    val recommendedDoubleFolds: Int,
    val theoreticalFatLayers: Int,
    val pastryCategory: String,
    val storageWarning: String
)

object CulinaryCalculators {

    /**
     * (a) Perdas em Cascata (Fachrechnen):
     * Compra = Porção / prod(1 - perda_i)
     */
    fun calculateCascadeLosses(
        netPortionGrams: Double,
        lossPercentages: List<Double>
    ): CascadeLossResult {
        val safePortion = netPortionGrams.coerceAtLeast(1.0)
        val combinedYield = lossPercentages
            .map { (1.0 - (it.coerceIn(0.0, 95.0) / 100.0)) }
            .fold(1.0) { acc, factor -> acc * factor }
            .coerceAtLeast(0.05)

        val grossPurchase = safePortion / combinedYield
        val fc = 1.0 / combinedYield
        val wastePct = (1.0 - combinedYield) * 100.0

        return CascadeLossResult(
            netPortionGrams = safePortion,
            combinedYieldFactor = combinedYield,
            correctionFactorFc = fc,
            grossPurchaseGrams = grossPurchase,
            totalWastePercent = wastePct
        )
    }

    /**
     * (b) Precificação Suíça (BAB/BeBu — BVP, EVP c/ MWST 8%, Prime-Cost, BE1, BE2)
     */
    fun calculateSwissPricing(
        foodCostWarenkosten: Double,
        directLaborPersonalkosten: Double,
        overheadGemeinkosten: Double,
        targetFoodCostPercent: Double,
        mwstRatePercent: Double = 8.0
    ): SwissPricingResult {
        val safeFood = foodCostWarenkosten.coerceAtLeast(0.0)
        val safeLabor = directLaborPersonalkosten.coerceAtLeast(0.0)
        val safeOverhead = overheadGemeinkosten.coerceAtLeast(0.0)
        val safeTargetPct = targetFoodCostPercent.coerceIn(10.0, 85.0) / 100.0

        val bvp = if (safeFood > 0.0) safeFood / safeTargetPct else (safeLabor + safeOverhead) * 1.25
        val evp = bvp * (1.0 + mwstRatePercent.coerceAtLeast(0.0) / 100.0)
        val primeCost = safeFood + safeLabor
        val be1 = bvp - safeFood // Bruttoergebnis 1 (Rohgewinn)
        val be2 = bvp - primeCost - safeOverhead // Betriebsergebnis 2

        return SwissPricingResult(
            foodCostChf = safeFood,
            laborDirectChf = safeLabor,
            primeCostChf = primeCost,
            grossMarginBe1Chf = be1,
            operatingProfitBe2Chf = be2,
            netSellingPriceBvpChf = bvp,
            grossMenuPriceEvpChf = evp,
            foodCostPercent = if (bvp > 0) (safeFood / bvp) * 100.0 else 0.0
        )
    }

    /**
     * (c) Ponto de Equilíbrio x_cr = Kf / (p - kv) & Conversor de Nota Suíça 1–6
     */
    fun calculateBreakEvenAndSwissGrade(
        fixedCostsKf: Double,
        unitSellingPriceP: Double,
        unitVariableCostKv: Double,
        achievedPoints: Double,
        maxPoints: Double
    ): BreakEvenAndGradeResult {
        val margin = (unitSellingPriceP - unitVariableCostKv).coerceAtLeast(0.01)
        val xCr = ceil(fixedCostsKf.coerceAtLeast(0.0) / margin).toInt()
        val safeMax = maxPoints.coerceAtLeast(1.0)
        val ratio = (achievedPoints.coerceIn(0.0, safeMax)) / safeMax
        // Fórmula oficial suíça: Nota = (Pontos / Máx) * 5 + 1
        val rawGrade = (ratio * 5.0) + 1.0
        val roundedHalfTenth = (rawGrade * 10.0).roundToInt() / 10.0

        return BreakEvenAndGradeResult(
            breakEvenUnits = xCr,
            contributionMarginPerUnit = margin,
            swissGrade1To6 = roundedHalfTenth.coerceIn(1.0, 6.0)
        )
    }

    /**
     * (d) Calculadora de Salmoura Sous-vide (5% a 10% = 50 a 100 g/L)
     */
    fun calculateSousVideBrine(
        waterLiters: Double,
        brineConcentrationPercent: Double,
        isDelicateFish: Boolean,
        targetCoreTempC: Double,
        bathOffsetC: Double = 0.8
    ): SousVideBrineResult {
        val safeWater = waterLiters.coerceIn(0.2, 50.0)
        val safePct = brineConcentrationPercent.coerceIn(5.0, 10.0)
        val saltGrams = safeWater * (safePct * 10.0) // 5% = 50 g/L, 10% = 100 g/L
        val minutes = if (isDelicateFish) 10 else 210 // 10 min peixes vs. 3-4h (210m) carnes grandes
        val safeOffset = bathOffsetC.coerceIn(0.5, 1.0)
        val bathTemp = targetCoreTempC + safeOffset

        val note = if (isDelicateFish) {
            "Peixe delicado: salmourar por exatos 10 min evita exsudação de albumina branca na superfície. Espessura máx. 7 cm sem espinhas pontiagudas."
        } else {
            "Carne grande/colágeno (ex.: Kalbsbrust): salmourar por 3 a 4 h refrigerado (≤ 5 °C) para retenção hídrica miofibrilar antes de embalar a vácuo (espessura máx. 7 cm sem osso)."
        }

        return SousVideBrineResult(
            waterLiters = safeWater,
            brinePercent = safePct,
            saltGramsRequired = saltGrams,
            recommendedBrineMinutes = minutes,
            targetCoreTempC = targetCoreTempC,
            recommendedBathTempC = bathTemp,
            technicalProtocolNote = note
        )
    }

    /**
     * (e) Simulador de Folhado Suíço (Deutscher / Halbblätterteig / Blitzblätterteig)
     */
    fun simulateSwissPuffPastry(
        targetBaseWeightGrams: Double,
        isHalfPuffHalbblaetterteig: Boolean
    ): PuffPastrySimResult {
        val scale = targetBaseWeightGrams.coerceIn(250.0, 10000.0) / 1000.0
        val flour = 420.0 * scale
        val baseFat = 365.0 * scale // 300g margarina folhado + 65g manteiga por kg no Deutscher
        val fat = if (isHalfPuffHalbblaetterteig) baseFat * 0.55 else baseFat
        val water = 210.0 * scale
        val salt = 10.0 * scale
        val malt = 2.0 * scale
        val total = flour + fat + water + salt + malt
        val ratio = fat / max(1.0, flour)

        val doubleFolds = if (ratio < 0.60) 3 else 4
        // 1 dobra dupla (4-fold / Tour double) multiplica camadas por 4: 3 dobras = 64, 4 dobras = 256
        var layers = 1
        repeat(doubleFolds) { layers *= 4 }

        val category = if (isHalfPuffHalbblaetterteig) {
            "Halbblätterteig (~0,48 kg gordura/kg farinha → 3 dobras duplas, descansos ≥ 30 min)"
        } else {
            "Deutscher / Blitzblätterteig (~0,87 kg gordura/kg farinha T400 → 4 dobras duplas, descansos ≥ 30 min)"
        }

        return PuffPastrySimResult(
            flourGrams = flour,
            totalFatGrams = fat,
            waterGrams = water,
            saltGrams = salt,
            maltGrams = malt,
            totalDoughGrams = total,
            fatToFlourRatio = ratio,
            recommendedDoubleFolds = doubleFolds,
            theoreticalFatLayers = layers,
            pastryCategory = category,
            storageWarning = "ALERTA CRÍTICO (Teige): NUNCA armazene massa folhada crua por longo período a 5 °C — ela acidifica e acinzenta por oxidação enzimática. Congele imediatamente (-18 °C) se não assar no turno."
        )
    }

    /**
     * Algoritmo de repetição espaçada SuperMemo SM-2 (notas 0 a 5)
     */
    fun computeNextSm2State(
        previous: Sm2ProgressEntity?,
        cardId: String,
        grade: Int,
        todayEpochDay: Long
    ): Sm2ProgressEntity {
        val q = grade.coerceIn(0, 5)
        val prevReps = previous?.repetitions ?: 0
        val prevEf = previous?.easinessFactor ?: 2.5
        val prevInterval = previous?.intervalDays ?: 0

        val newEf = max(
            1.3,
            prevEf + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        )

        val (newReps, newInterval) = if (q < 3) {
            0 to 1
        } else {
            val nextReps = prevReps + 1
            val nextInt = when (nextReps) {
                1 -> 1
                2 -> 6
                else -> (prevInterval * newEf).roundToInt().coerceAtLeast(1)
            }
            nextReps to nextInt
        }

        return Sm2ProgressEntity(
            cardId = cardId,
            repetitions = newReps,
            easinessFactor = newEf,
            intervalDays = newInterval,
            nextReviewEpochDay = todayEpochDay + newInterval,
            lastGrade = q,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    val swissPuffPastry13Shapes: List<String> = listOf(
        "1. Vol-au-vent / Pastetli (caixas cilíndricas p/ ragus)",
        "2. Fleurons (meias-luas p/ guarnição de peixes e carnes)",
        "3. Palmiers / Prussiens (corações caramelizados em dobras simétricas)",
        "4. Allumettes (tiras retangulares c/ glacê real)",
        "5. Jalousies (tiras duplas recheadas c/ fendas paralelas)",
        "6. Pithiviers (disco duplo escarificado em espiral c/ frangipane)",
        "7. Chaussons (meias-luas seladas c/ maçã/compota)",
        "8. Cornets / Schillerlocken (cones enrolados em tubo metálico)",
        "9. Sacristains (bastões torcidos c/ açúcar e amêndoas)",
        "10. Dartois (faixa retangular recheada salgada ou doce)",
        "11. Mille-feuilles / Crèmeschnitten (lâminas picadas e prensadas)",
        "12. Feuilletés au fromage / Käsegebäck (palitos c/ Gruyère AOP)",
        "13. Moulin à vent (cata-ventos dobrados nas pontas)"
    )
}
