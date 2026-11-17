package com.example.data.local

import com.example.domain.calculators.MepCalculationSummary
import org.json.JSONArray
import org.json.JSONObject

class LocalKitchenRepository(private val dao: AtelierDao) {

    val allTimers = dao.observeAllTimers()
    val sm2Progress = dao.observeSm2Progress()
    val mepSheets = dao.observeAllMepSheets()
    val mepLines = dao.observeAllMepLines()
    val shoppingItems = dao.observeAllShoppingItems()
    val ownedEquipment = dao.observeOwnedEquipment()
    val selfEvaluations = dao.observeSelfEvaluations()

    suspend fun upsertTimer(entity: StudyTimerEntity) = dao.upsertTimer(entity)
    suspend fun upsertSm2(entity: Sm2ProgressEntity) = dao.upsertSm2(entity)
    suspend fun upsertMepSheet(sheet: MepSheetEntity) = dao.upsertMepSheet(sheet)
    suspend fun saveMepComplete(sheet: MepSheetEntity, lines: List<MepIngredientLineEntity>) =
        dao.saveMepSheetWithLines(sheet, lines)
    suspend fun deleteMepComplete(recipeId: String) = dao.deleteMepSheetComplete(recipeId)
    suspend fun upsertShoppingItem(item: ShoppingFifoEntity) = dao.upsertShoppingItem(item)
    suspend fun replaceWeekShopping(weekNumber: Int, items: List<ShoppingFifoEntity>) {
        dao.clearShoppingForWeek(weekNumber)
        dao.insertShoppingItems(items)
    }
    suspend fun deleteShoppingItem(id: Long) = dao.deleteShoppingItem(id)
    suspend fun upsertEquipment(entity: EquipmentOwnedEntity) = dao.upsertEquipmentOwned(entity)
    suspend fun upsertSelfEvaluation(entity: SelfEvalEntity) = dao.upsertSelfEvaluation(entity)

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", "atelier-gastronomia-24s-v4.2")
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        val sm2Arr = JSONArray()
        dao.getAllSm2Once().forEach { s ->
            sm2Arr.put(
                JSONObject().apply {
                    put("cardId", s.cardId)
                    put("repetitions", s.repetitions)
                    put("easinessFactor", s.easinessFactor)
                    put("intervalDays", s.intervalDays)
                    put("nextReviewEpochDay", s.nextReviewEpochDay)
                    put("lastGrade", s.lastGrade)
                }
            )
        }
        root.put("sm2Progress", sm2Arr)

        val mepArr = JSONArray()
        dao.getAllMepSheetsOnce().forEach { m ->
            mepArr.put(
                JSONObject().apply {
                    put("recipeId", m.recipeId)
                    put("title", m.title)
                    put("weekNumber", m.weekNumber)
                    put("stationBrigade", m.stationBrigade)
                    put("baseServings", m.baseServings)
                    put("targetServings", m.targetServings)
                    put("portionWeightGrams", m.portionWeightGrams)
                    put("cookingShrinkagePercent", m.cookingShrinkagePercent)
                    put("targetCmvPercent", m.targetCmvPercent)
                    put("directLaborCost", m.directLaborCost)
                    put("pccTargetTempC", m.pccTargetTempC)
                    put("plateTempC", m.plateTempC)
                    put("allergensCsv", m.allergensCsv)
                    put("actualBenchYieldGrams", m.actualBenchYieldGrams)
                    put("executionScore1To5", m.executionScore1To5)
                    put("isCustomSheet", m.isCustomSheet)
                    put("block1Identification", m.block1Identification)
                    put("block2IngredientsPbPlFc", m.block2IngredientsPbPlFc)
                    put("block3CookwareBlades", m.block3CookwareBlades)
                    put("block4PrePrepCuts", m.block4PrePrepCuts)
                    put("block5ThermalFlowPcc", m.block5ThermalFlowPcc)
                    put("block6PlatingFinishing", m.block6PlatingFinishing)
                    put("block7HygieneAllergens", m.block7HygieneAllergens)
                    put("block8Selbstkontrolle", m.block8Selbstkontrolle)
                    put("localPhotoUri", m.localPhotoUri)
                    put("cloudImageLink", m.cloudImageLink)
                    put("cloudMediaId", m.cloudMediaId)
                    put("photoThumbnailBase64", m.photoThumbnailBase64)
                }
            )
        }
        root.put("mepSheets", mepArr)

        val linesArr = JSONArray()
        dao.getAllMepLinesOnce().forEach { l ->
            linesArr.put(
                JSONObject().apply {
                    put("recipeId", l.recipeId)
                    put("sortOrder", l.sortOrder)
                    put("name", l.name)
                    put("sector", l.sector)
                    put("unit", l.unit)
                    put("baseNetPl", l.baseNetPl)
                    put("trimLossPercent", l.trimLossPercent)
                    put("correctionFactorFc", l.correctionFactorFc)
                    put("unitCostPerKgOrL", l.unitCostPerKgOrL)
                }
            )
        }
        root.put("mepIngredientLines", linesArr)

        val eqArr = JSONArray()
        dao.getAllEquipmentOnce().forEach { e ->
            eqArr.put(
                JSONObject().apply {
                    put("equipmentId", e.equipmentId)
                    put("isOwned", e.isOwned)
                }
            )
        }
        root.put("equipmentOwned", eqArr)

        val evalArr = JSONArray()
        dao.getAllSelfEvaluationsOnce().forEach { ev ->
            evalArr.put(
                JSONObject().apply {
                    put("weekNumber", ev.weekNumber)
                    put("scoreMiseEnPlace", ev.scoreMiseEnPlace)
                    put("scoreTechnique", ev.scoreTechnique)
                    put("scoreHygienePcc", ev.scoreHygienePcc)
                    put("scoreSensory", ev.scoreSensory)
                    put("erkenntnisse", ev.erkenntnisse)
                    put("offenePunkte", ev.offenePunkte)
                }
            )
        }
        root.put("selfEvaluations", evalArr)

        val shopArr = JSONArray()
        dao.getAllShoppingItemsOnce().forEach { item ->
            shopArr.put(
                JSONObject().apply {
                    put("weekNumber", item.weekNumber)
                    put("ingredientName", item.ingredientName)
                    put("sector", item.sector)
                    put("unit", item.unit)
                    put("netQuantityPl", item.netQuantityPl)
                    put("correctionFactorFc", item.correctionFactorFc)
                    put("grossQuantityPb", item.grossQuantityPb)
                    put("batchDate", item.batchDate)
                    put("expiryDate", item.expiryDate)
                    put("isPurchased", item.isPurchased)
                    put("fifoPriorityNote", item.fifoPriorityNote)
                }
            )
        }
        root.put("shoppingFifo", shopArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonContent: String): Result<Int> = runCatching {
        val root = JSONObject(jsonContent)
        var restoredCount = 0

        root.optJSONArray("sm2Progress")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Sm2ProgressEntity(
                    cardId = o.getString("cardId"),
                    repetitions = o.optInt("repetitions", 0),
                    easinessFactor = o.optDouble("easinessFactor", 2.5),
                    intervalDays = o.optInt("intervalDays", 0),
                    nextReviewEpochDay = o.optLong("nextReviewEpochDay", 0L),
                    lastGrade = o.optInt("lastGrade", -1)
                )
            }
            dao.upsertAllSm2(list)
            restoredCount += list.size
        }

        root.optJSONArray("mepSheets")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MepSheetEntity(
                    recipeId = o.getString("recipeId"),
                    title = o.optString("title", ""),
                    weekNumber = o.optInt("weekNumber", 1),
                    stationBrigade = o.optString("stationBrigade", "Saucier"),
                    baseServings = o.optInt("baseServings", 4),
                    targetServings = o.optInt("targetServings", 4),
                    portionWeightGrams = o.optDouble("portionWeightGrams", 220.0),
                    cookingShrinkagePercent = o.optDouble("cookingShrinkagePercent", 12.0),
                    targetCmvPercent = o.optDouble("targetCmvPercent", 28.0),
                    directLaborCost = o.optDouble("directLaborCost", 8.0),
                    pccTargetTempC = o.optInt("pccTargetTempC", 70),
                    plateTempC = o.optInt("plateTempC", 55),
                    allergensCsv = o.optString("allergensCsv", ""),
                    actualBenchYieldGrams = o.optDouble("actualBenchYieldGrams", 0.0),
                    executionScore1To5 = o.optInt("executionScore1To5", 4),
                    isCustomSheet = o.optBoolean("isCustomSheet", false),
                    block1Identification = o.optString("block1Identification"),
                    block2IngredientsPbPlFc = o.optString("block2IngredientsPbPlFc"),
                    block3CookwareBlades = o.optString("block3CookwareBlades"),
                    block4PrePrepCuts = o.optString("block4PrePrepCuts"),
                    block5ThermalFlowPcc = o.optString("block5ThermalFlowPcc"),
                    block6PlatingFinishing = o.optString("block6PlatingFinishing"),
                    block7HygieneAllergens = o.optString("block7HygieneAllergens"),
                    block8Selbstkontrolle = o.optString("block8Selbstkontrolle"),
                    localPhotoUri = o.optString("localPhotoUri", ""),
                    cloudImageLink = o.optString("cloudImageLink", ""),
                    cloudMediaId = o.optString("cloudMediaId", ""),
                    photoThumbnailBase64 = o.optString("photoThumbnailBase64", "")
                )
            }
            dao.upsertAllMepSheets(list)
            restoredCount += list.size
        }

        root.optJSONArray("mepIngredientLines")?.let { arr ->
            val grouped = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MepIngredientLineEntity(
                    recipeId = o.getString("recipeId"),
                    sortOrder = o.optInt("sortOrder", i),
                    name = o.getString("name"),
                    sector = o.optString("sector", "Mercearia Seca"),
                    unit = o.optString("unit", "g"),
                    baseNetPl = o.optDouble("baseNetPl", 100.0),
                    trimLossPercent = o.optDouble("trimLossPercent", 0.0),
                    correctionFactorFc = o.optDouble("correctionFactorFc", 1.0),
                    unitCostPerKgOrL = o.optDouble("unitCostPerKgOrL", 28.0)
                )
            }.groupBy { it.recipeId }
            grouped.forEach { (recId, lines) ->
                dao.deleteMepLinesForSheet(recId)
                dao.insertMepLines(lines)
            }
            restoredCount += grouped.values.sumOf { it.size }
        }

        root.optJSONArray("equipmentOwned")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                EquipmentOwnedEntity(
                    equipmentId = o.getString("equipmentId"),
                    isOwned = o.optBoolean("isOwned", false)
                )
            }
            dao.upsertAllEquipmentOwned(list)
            restoredCount += list.size
        }

        root.optJSONArray("selfEvaluations")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SelfEvalEntity(
                    weekNumber = o.getInt("weekNumber"),
                    scoreMiseEnPlace = o.optInt("scoreMiseEnPlace", 4),
                    scoreTechnique = o.optInt("scoreTechnique", 4),
                    scoreHygienePcc = o.optInt("scoreHygienePcc", 5),
                    scoreSensory = o.optInt("scoreSensory", 4),
                    erkenntnisse = o.optString("erkenntnisse"),
                    offenePunkte = o.optString("offenePunkte")
                )
            }
            dao.upsertAllSelfEvaluations(list)
            restoredCount += list.size
        }

        root.optJSONArray("shoppingFifo")?.let { arr ->
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ShoppingFifoEntity(
                    weekNumber = o.optInt("weekNumber", 1),
                    ingredientName = o.getString("ingredientName"),
                    sector = o.optString("sector", "Mercearia Seca"),
                    unit = o.optString("unit", "g"),
                    netQuantityPl = o.optDouble("netQuantityPl", 100.0),
                    correctionFactorFc = o.optDouble("correctionFactorFc", 1.0),
                    grossQuantityPb = o.optDouble("grossQuantityPb", 100.0),
                    batchDate = o.optString("batchDate", ""),
                    expiryDate = o.optString("expiryDate", ""),
                    isPurchased = o.optBoolean("isPurchased", false),
                    fifoPriorityNote = o.optString("fifoPriorityNote", "PEPS/FIFO")
                )
            }
            dao.insertShoppingItems(list)
            restoredCount += list.size
        }

        restoredCount
    }

    suspend fun exportDossierLernreflexionMarkdown(): String {
        val evals = dao.getAllSelfEvaluationsOnce().sortedBy { it.weekNumber }
        val meps = dao.getAllMepSheetsOnce()
        return buildString {
            appendLine("# Dossiê de Evolução Técnica — Atelier Gastronomia 24S")
            appendLine()
            if (evals.isEmpty()) {
                appendLine("Nenhuma autoavaliação técnica de bancada registrada até o momento.")
            } else {
                evals.forEach { ev ->
                    val avg = (ev.scoreMiseEnPlace + ev.scoreTechnique + ev.scoreHygienePcc + ev.scoreSensory) / 4.0
                    appendLine("## Semana ${ev.weekNumber} — Média da Autoavaliação: ${"%.2f".format(avg)} / 5,0")
                    appendLine("- Mise en Place & Organização: ${ev.scoreMiseEnPlace}/5 | Técnica & Cortes: ${ev.scoreTechnique}/5 | Boas Práticas & PCC: ${ev.scoreHygienePcc}/5 | Análise Sensorial: ${ev.scoreSensory}/5")
                    appendLine("- **Aprendizados Consolidados:** ${ev.erkenntnisse.ifBlank { "—" }}")
                    appendLine("- **Pontos de Melhoria para o Próximo Turno:** ${ev.offenePunkte.ifBlank { "—" }}")
                    appendLine()
                }
            }
            if (meps.isNotEmpty()) {
                appendLine("## Fichas Técnicas de Preparo Gravadas (${meps.size})")
                meps.forEach { m ->
                    val label = m.title.ifBlank { m.recipeId }
                    val linkNote = if (m.cloudImageLink.isNotBlank()) " • Foto Firestore: `${m.cloudImageLink}`" else ""
                    appendLine("- **$label** (Sem. ${m.weekNumber} • Praça ${m.stationBrigade} • ${m.targetServings} porções • PCC ≥ ${m.pccTargetTempC} °C$linkNote): `${m.block5ThermalFlowPcc.take(120)}`")
                }
            }
        }
    }

    companion object {
        fun formatSingleMepSheetMarkdown(
            sheet: MepSheetEntity,
            summary: MepCalculationSummary
        ): String = buildString {
            val heading = sheet.title.ifBlank { sheet.recipeId }
            appendLine("# FICHA TÉCNICA MEP (8 BLOCOS) — $heading")
            appendLine("- **Identificador:** `${sheet.recipeId}` | **Semana:** ${sheet.weekNumber} | **Praça da Brigada:** ${sheet.stationBrigade}")
            appendLine("- **Rendimento Escalonado:** ${sheet.targetServings} porções (Base ${sheet.baseServings} • Fator de Escala ${"%.2f".format(summary.scaleFactor)}x)")
            appendLine("- **Rendimento Pós-Cocção (${"%.0f".format(sheet.cookingShrinkagePercent)}% de quebra térmica):** ${"%.0f".format(summary.theoreticalCookedYieldGrams)} g total (${"%.0f".format(summary.theoreticalPortionCookedGrams)} g/porção)")
            appendLine("- **CMV / Porção:** R$ ${"%.2f".format(summary.foodCostPerPortionChf)} | **Preço Líquido Sugerido:** R$ ${"%.2f".format(summary.pricingPerPortion.netSellingPriceBvpChf)} | **Preço de Cardápio (c/ taxas 8%):** R$ ${"%.2f".format(summary.pricingPerPortion.grossMenuPriceEvpChf)}")
            if (sheet.cloudImageLink.isNotBlank()) {
                appendLine("- **Registro Fotográfico da Criação (Firestore):** `${sheet.cloudImageLink}`")
            }
            appendLine()
            appendLine("## 1. Identificação, Praça & Rendimento")
            appendLine(sheet.block1Identification)
            appendLine()
            appendLine("## 2. Matriz de Insumos (PB = PL × FC)")
            summary.lines.forEach { l ->
                appendLine("- **${l.source.name}** (${l.source.sector}): PL ${"%.1f".format(l.scaledPl)} ${l.source.unit} | FC ${"%.2f".format(l.effectiveFc)} (${"%.0f".format(l.effectiveTrimLossPercent)}% apara) | PB **${"%.1f".format(l.scaledPb)} ${l.source.unit}** | R$ ${"%.2f".format(l.lineCostChf)}")
            }
            appendLine()
            appendLine("## 3. Bateria de Cozinha & Cutelaria")
            appendLine(sheet.block3CookwareBlades)
            appendLine()
            appendLine("## 4. Pré-preparo & Padronização de Cortes")
            appendLine(sheet.block4PrePrepCuts)
            appendLine()
            appendLine("## 5. Fluxo Térmico Cronometrado & PCC (Alvo ≥ ${sheet.pccTargetTempC} °C)")
            appendLine(sheet.block5ThermalFlowPcc)
            appendLine()
            appendLine("## 6. Empratamento, Finalização & Passe (${sheet.plateTempC} °C)")
            appendLine(sheet.block6PlatingFinishing)
            appendLine()
            appendLine("## 7. Boas Práticas (RDC 216 / APPCC) & Alergênicos (${sheet.allergensCsv.ifBlank { "Nenhum declarado" }})")
            appendLine(sheet.block7HygieneAllergens)
            appendLine()
            appendLine("## 8. Autoavaliação de Bancada & Auditoria de Rendimento (${sheet.executionScore1To5}/5)")
            summary.benchYieldDeviationPercent?.let { dev ->
                appendLine("- Peso Pós-Cocção Aferido na Balança: ${"%.0f".format(sheet.actualBenchYieldGrams)} g (Desvio sobre o teórico: ${"%+.1f".format(dev)}%)")
            }
            appendLine(sheet.block8Selbstkontrolle.ifBlank { "Sem observações pós-produção." })
        }
    }
}
