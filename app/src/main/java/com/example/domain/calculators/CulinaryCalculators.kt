package com.example.domain.calculators

import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.Sm2ProgressEntity
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class MepCalculatedLine(
    val source: MepIngredientLineEntity,
    val effectiveFc: Double,
    val effectiveTrimLossPercent: Double,
    val scaledPl: Double,
    val scaledPb: Double,
    val lineCostChf: Double
)

data class MepCalculationSummary(
    val scaleFactor: Double,
    val lines: List<MepCalculatedLine>,
    val totalScaledPlGramsEquiv: Double,
    val totalScaledPbGramsEquiv: Double,
    val weightedAverageFc: Double,
    val theoreticalCookedYieldGrams: Double,
    val theoreticalPortionCookedGrams: Double,
    val totalBatchFoodCostChf: Double,
    val foodCostPerPortionChf: Double,
    val pricingPerPortion: SwissPricingResult,
    val benchYieldDeviationPercent: Double?
)

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
     * Motor de cálculo completo da Ficha MEP (8 Blocos):
     * Escala por fator de conversão, PB = PL × FC, perda térmica pós-cocção,
     * CMV por porção, BVP/EVP (MWST 8%) e desvio de rendimento real na bancada.
     */
    fun calculateMepSheetSummary(
        baseServings: Int,
        targetServings: Int,
        cookingShrinkagePercent: Double,
        targetCmvPercent: Double,
        directLaborPerPortionChf: Double,
        actualBenchYieldGrams: Double,
        ingredientLines: List<MepIngredientLineEntity>
    ): MepCalculationSummary {
        val safeBase = baseServings.coerceAtLeast(1)
        val safeTarget = targetServings.coerceAtLeast(1)
        val scaleFactor = safeTarget.toDouble() / safeBase.toDouble()
        val safeShrinkage = cookingShrinkagePercent.coerceIn(0.0, 85.0)

        val calcLines = ingredientLines.map { line ->
            val safeTrim = line.trimLossPercent.coerceIn(0.0, 90.0)
            val fcFromTrim = if (safeTrim > 0.0) 1.0 / (1.0 - safeTrim / 100.0) else 1.0
            val effectiveFc = if (line.correctionFactorFc > 1.0001 && safeTrim <= 0.001) {
                line.correctionFactorFc.coerceIn(1.0, 10.0)
            } else if (safeTrim > 0.001) {
                fcFromTrim
            } else {
                line.correctionFactorFc.coerceIn(1.0, 10.0)
            }
            val effectiveTrim = ((1.0 - (1.0 / effectiveFc)) * 100.0).coerceAtLeast(0.0)

            val scaledPl = line.baseNetPl.coerceAtLeast(0.0) * scaleFactor
            val scaledPb = scaledPl * effectiveFc

            val pbKgOrL = when (line.unit.trim().lowercase()) {
                "kg", "l" -> scaledPb
                "un", "unid", "unid." -> scaledPb // unitCost treated as per unit
                else -> scaledPb / 1000.0         // g, ml -> kg/L
            }
            val cost = pbKgOrL * line.unitCostPerKgOrL.coerceAtLeast(0.0)

            MepCalculatedLine(
                source = line,
                effectiveFc = effectiveFc,
                effectiveTrimLossPercent = effectiveTrim,
                scaledPl = scaledPl,
                scaledPb = scaledPb,
                lineCostChf = cost
            )
        }

        fun toGramsEquiv(amount: Double, unit: String): Double = when (unit.trim().lowercase()) {
            "kg", "l" -> amount * 1000.0
            "un", "unid", "unid." -> amount * 55.0 // egg/unit average culinary weight
            else -> amount
        }

        val totalPlG = calcLines.sumOf { toGramsEquiv(it.scaledPl, it.source.unit) }
        val totalPbG = calcLines.sumOf { toGramsEquiv(it.scaledPb, it.source.unit) }
        val weightedFc = if (totalPlG > 0.0) totalPbG / totalPlG else 1.0
        val cookedYieldG = totalPlG * (1.0 - safeShrinkage / 100.0)
        val cookedPortionG = cookedYieldG / safeTarget
        val totalCost = calcLines.sumOf { it.lineCostChf }
        val portionCost = totalCost / safeTarget

        val pricing = calculateSwissPricing(
            foodCostWarenkosten = portionCost,
            directLaborPersonalkosten = directLaborPerPortionChf.coerceAtLeast(0.0),
            overheadGemeinkosten = (portionCost * 0.35),
            targetFoodCostPercent = targetCmvPercent.coerceIn(12.0, 80.0),
            mwstRatePercent = 8.0
        )

        val deviationPct = if (actualBenchYieldGrams > 0.0 && cookedYieldG > 0.0) {
            ((actualBenchYieldGrams - cookedYieldG) / cookedYieldG) * 100.0
        } else {
            null
        }

        return MepCalculationSummary(
            scaleFactor = scaleFactor,
            lines = calcLines,
            totalScaledPlGramsEquiv = totalPlG,
            totalScaledPbGramsEquiv = totalPbG,
            weightedAverageFc = weightedFc,
            theoreticalCookedYieldGrams = cookedYieldG,
            theoreticalPortionCookedGrams = cookedPortionG,
            totalBatchFoodCostChf = totalCost,
            foodCostPerPortionChf = portionCost,
            pricingPerPortion = pricing,
            benchYieldDeviationPercent = deviationPct
        )
    }

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
            "Pescado delicado: imersão em salmoura por exatos 10 min evita exsudação de albumina branca coagulada na superfície. Espessura máxima de 7 cm, sem espinhas pontiagudas."
        } else {
            "Corte cárneo alto ou rico em colágeno (ex.: peito de vitela): marinar em salmoura refrigerada (≤ 4 °C) por 3 a 4 h para retenção hídrica miofibrilar antes da termosselagem a vácuo (espessura máx. 7 cm, desossado)."
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
     * (e) Simulador de Massa Folhada Clássica, Meia-Folhada e Rápida
     */
    fun simulateSwissPuffPastry(
        targetBaseWeightGrams: Double,
        isHalfPuffHalbblaetterteig: Boolean
    ): PuffPastrySimResult {
        val scale = targetBaseWeightGrams.coerceIn(250.0, 10000.0) / 1000.0
        val flour = 420.0 * scale
        val baseFat = 365.0 * scale // 300 g gordura de folhado + 65 g manteiga na détrempe por kg
        val fat = if (isHalfPuffHalbblaetterteig) baseFat * 0.55 else baseFat
        val water = 210.0 * scale
        val salt = 10.0 * scale
        val malt = 2.0 * scale
        val total = flour + fat + water + salt + malt
        val ratio = fat / max(1.0, flour)

        val doubleFolds = if (ratio < 0.60) 3 else 4
        // 1 dobra dupla (livro / 4 camadas) multiplica por 4: 3 dobras = 64, 4 dobras = 256
        var layers = 1
        repeat(doubleFolds) { layers *= 4 }

        val category = if (isHalfPuffHalbblaetterteig) {
            "Massa Meia-Folhada (~0,48 kg de gordura/kg de farinha → 3 dobras duplas, repousos ≥ 30 min)"
        } else {
            "Massa Folhada Clássica / Rápida (~0,87 kg de gordura/kg de farinha Tipo 1 → 4 dobras duplas, repousos ≥ 30 min)"
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
            storageWarning = "ALERTA CRÍTICO DE CONFEITARIA: Nunca armazene massa folhada crua refrigerada a 5 °C por longo período — ocorre acidificação e acinzentamento por oxidação enzimática. Congele imediatamente (-18 °C) caso não vá assar na mesma produção."
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
        "1. Vol-au-vent / Caixinhas folhadas (cilindros ocos para ragus e veloutés)",
        "2. Fleurons (meias-luas folhadas para guarnição clássica de pescados e carnes)",
        "3. Palmiers (leques/corações caramelizados em dobras simétricas)",
        "4. Allumettes (tiras retangulares glaceadas com glacê real)",
        "5. Jalousies (retângulos duplos recheados com fendas paralelas no topo)",
        "6. Pithiviers (disco duplo escarificado em espiral recheado com creme frangipane)",
        "7. Chaussons (meias-luas seladas recheadas com compota de maçã)",
        "8. Cornets / Canudinhos folhados (tiras assadas em molde cônico metálico)",
        "9. Sacristains (bastões torcidos polvilhados com açúcar e amêndoas laminadas)",
        "10. Dartois (faixa retangular recheada salgada ou doce escarificada)",
        "11. Mil-folhas / Mille-feuilles (lâminas furadas e assadas sob peso para creme)",
        "12. Palitos folhados de queijo (tiras torcidas com Gruyère AOP maturado)",
        "13. Cata-ventos / Moulin à vent (quadrados dobrados nas pontas ao centro)"
    )

    /**
     * (f) Conversor de Gelatina & Calculadora de Hidrocoloides (Canella-Rawls, Diego Costa Senac & James Peterson)
     * - 1 envelope padrão (12 g pó) = 6 folhas de gelatina (~2,0 g/folha, Bloom prata ~180–200)
     * - 1 colher de chá nivelada (cc, ~2,0 g pó) = 1 folha
     * - Hidratação da gelatina em pó: 5× o peso do pó em água fria antes de fundir (45–55 °C, sem ferver)
     */
    fun calculateGelatinAndHydrocolloid(
        gelatinInputAmount: Double,
        inputIsSheets: Boolean,
        liquidBaseGrams: Double,
        agent: HydrocolloidAgent
    ): GelatinHydrocolloidResult {
        val safeInput = gelatinInputAmount.coerceIn(0.1, 500.0)
        val equivalentSheets: Double
        val equivalentPowderGrams: Double
        if (inputIsSheets) {
            equivalentSheets = safeInput
            equivalentPowderGrams = safeInput * 2.0 // 6 folhas = 12 g pó -> 1 folha = 2 g pó
        } else {
            equivalentPowderGrams = safeInput
            equivalentSheets = safeInput / 2.0
        }
        val envelopes12g = equivalentPowderGrams / 12.0
        val teaspoonsCc = equivalentSheets // 1 cc pó = 1 folha
        val hydrationWaterForPowderGrams = equivalentPowderGrams * 5.0

        val safeBase = liquidBaseGrams.coerceIn(10.0, 50_000.0)
        val minGrams = safeBase * (agent.minPercent / 100.0)
        val recommendedGrams = safeBase * (agent.recommendedPercent / 100.0)
        val maxGrams = safeBase * (agent.maxPercent / 100.0)

        return GelatinHydrocolloidResult(
            equivalentPowderGrams = equivalentPowderGrams,
            equivalentSheets = equivalentSheets,
            equivalentEnvelopes12g = envelopes12g,
            equivalentTeaspoonsCc = teaspoonsCc,
            hydrationWaterGrams = hydrationWaterForPowderGrams,
            liquidBaseGrams = safeBase,
            agent = agent,
            minDosageGrams = minGrams,
            recommendedDosageGrams = recommendedGrams,
            maxDosageGrams = maxGrams
        )
    }
}

enum class HydrocolloidAgent(
    val labelPt: String,
    val minPercent: Double,
    val recommendedPercent: Double,
    val maxPercent: Double,
    val thermalHydrationRule: String,
    val culinaryUseNote: String
) {
    XANTHAN(
        labelPt = "Goma Xantana (0,1%–0,5%)",
        minPercent = 0.10,
        recommendedPercent = 0.25,
        maxPercent = 0.50,
        thermalHydrationRule = "Dispersão a frio ou quente sob cisalhamento (mixer de imersão); dispensa aquecimento.",
        culinaryUseNote = "Suspensão de partículas e estabilização de molhos contemporâneos sem mascarar sabor (James Peterson)."
    ),
    LECITHIN(
        labelPt = "Lecitina em Pó (1,0%–2,0%)",
        minPercent = 1.00,
        recommendedPercent = 1.20,
        maxPercent = 2.00,
        thermalHydrationRule = "Hidrata a frio ou morno (até 60 °C); incorporar ar na superfície com mixer inclinado.",
        culinaryUseNote = "Emulsificante anfifílico para vinagretes, molhos untuosos e ares/espumas leves (James Peterson)."
    ),
    PGA_ALGINATE(
        labelPt = "Alginato de Propilenoglicol — PGA (0,3%–0,9%)",
        minPercent = 0.30,
        recommendedPercent = 0.50,
        maxPercent = 0.90,
        thermalHydrationRule = "Dispersar em mixer a frio em meio aquoso/ácido e aguardar 15 min para hidratação completa.",
        culinaryUseNote = "Estabiliza emulsões ácidas, reduções cítricas e molhos engarrafados tolerando pH baixo (Peterson)."
    ),
    AGAR_FLUID_GEL(
        labelPt = "Ágar-ágar Fluid Gel (0,01%–0,9%)",
        minPercent = 0.01,
        recommendedPercent = 0.45,
        maxPercent = 0.90,
        thermalHydrationRule = "Dispersar a frio, ferver ≥ 90 °C por 2 min, gelificar a < 35 °C e processar até gel fluido acetinado.",
        culinaryUseNote = "Géis fluidos brilhantes termorreversíveis (~85 °C) para empratamento quente e frio (Peterson / Canella-Rawls)."
    ),
    AGAR_FIRM(
        labelPt = "Ágar-ágar Gel de Corte (0,8%–1,5%)",
        minPercent = 0.80,
        recommendedPercent = 1.00,
        maxPercent = 1.50,
        thermalHydrationRule = "Fervura obrigatória ≥ 90 °C por 2 min; gelifica entre 32–40 °C sem exigir refrigeração.",
        culinaryUseNote = "Cubos, véus quentes e confeitaria vegetal; reduzir caso o meio seja muito ácido (Canella-Rawls)."
    ),
    CARRAGEENAN(
        labelPt = "Carragena Iota / Kappa (0,2%–0,8%)",
        minPercent = 0.20,
        recommendedPercent = 0.45,
        maxPercent = 0.80,
        thermalHydrationRule = "Dispersar a frio e aquecer acima de 72–80 °C na presença de cálcio/laticínios.",
        culinaryUseNote = "Flans lácteos elásticos (Iota) ou géis firmes brilhantes de glaçagem (Kappa) (Canella-Rawls)."
    ),
    PECTIN_NH(
        labelPt = "Pectina NH Termorreversível (0,8%–1,5%)",
        minPercent = 0.80,
        recommendedPercent = 1.00,
        maxPercent = 1.50,
        thermalHydrationRule = "Misturar a seco com 5× seu peso em açúcar, polvilhar a ~40 °C e ferver por 1 min com toque ácido.",
        culinaryUseNote = "Nappages espelhados, inserts de frutas e geleias de confeitaria fina (Canella-Rawls / Senac)."
    ),
    ULTRA_SPERSE_3(
        labelPt = "Amido Modificado Ultra-Sperse 3 (1,0%–3,0%)",
        minPercent = 1.00,
        recommendedPercent = 1.80,
        maxPercent = 3.00,
        thermalHydrationRule = "Espessamento instantâneo a frio sob vigorosa agitação, sem aquecimento e estável ao congelamento.",
        culinaryUseNote = "Cremes frios de frutas sensíveis ao calor e correção imediata de sinérese no passe (Canella-Rawls)."
    )
}

data class GelatinHydrocolloidResult(
    val equivalentPowderGrams: Double,
    val equivalentSheets: Double,
    val equivalentEnvelopes12g: Double,
    val equivalentTeaspoonsCc: Double,
    val hydrationWaterGrams: Double,
    val liquidBaseGrams: Double,
    val agent: HydrocolloidAgent,
    val minDosageGrams: Double,
    val recommendedDosageGrams: Double,
    val maxDosageGrams: Double
)

