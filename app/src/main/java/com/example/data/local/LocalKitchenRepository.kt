package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

class LocalKitchenRepository(private val dao: AtelierDao) {

    val allTimers = dao.observeAllTimers()
    val sm2Progress = dao.observeSm2Progress()
    val mepSheets = dao.observeAllMepSheets()
    val shoppingItems = dao.observeAllShoppingItems()
    val ownedEquipment = dao.observeOwnedEquipment()
    val selfEvaluations = dao.observeSelfEvaluations()

    suspend fun upsertTimer(entity: StudyTimerEntity) = dao.upsertTimer(entity)
    suspend fun upsertSm2(entity: Sm2ProgressEntity) = dao.upsertSm2(entity)
    suspend fun upsertMepSheet(sheet: MepSheetEntity) = dao.upsertMepSheet(sheet)
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
        root.put("schemaVersion", "atelier-gastronomia-24s-v4")
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
                    put("weekNumber", m.weekNumber)
                    put("targetServings", m.targetServings)
                    put("block1Identification", m.block1Identification)
                    put("block2IngredientsPbPlFc", m.block2IngredientsPbPlFc)
                    put("block3CookwareBlades", m.block3CookwareBlades)
                    put("block4PrePrepCuts", m.block4PrePrepCuts)
                    put("block5ThermalFlowPcc", m.block5ThermalFlowPcc)
                    put("block6PlatingFinishing", m.block6PlatingFinishing)
                    put("block7HygieneAllergens", m.block7HygieneAllergens)
                    put("block8Selbstkontrolle", m.block8Selbstkontrolle)
                }
            )
        }
        root.put("mepSheets", mepArr)

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
                    weekNumber = o.optInt("weekNumber", 1),
                    targetServings = o.optInt("targetServings", 4),
                    block1Identification = o.optString("block1Identification"),
                    block2IngredientsPbPlFc = o.optString("block2IngredientsPbPlFc"),
                    block3CookwareBlades = o.optString("block3CookwareBlades"),
                    block4PrePrepCuts = o.optString("block4PrePrepCuts"),
                    block5ThermalFlowPcc = o.optString("block5ThermalFlowPcc"),
                    block6PlatingFinishing = o.optString("block6PlatingFinishing"),
                    block7HygieneAllergens = o.optString("block7HygieneAllergens"),
                    block8Selbstkontrolle = o.optString("block8Selbstkontrolle")
                )
            }
            dao.upsertAllMepSheets(list)
            restoredCount += list.size
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
            appendLine("# Dossier Lernreflexion — Atelier Gastronomia 24S (v4)")
            appendLine()
            if (evals.isEmpty()) {
                appendLine("Nenhuma autoavaliação (Selbstkontrolle) registrada até o momento.")
            } else {
                evals.forEach { ev ->
                    val avg = (ev.scoreMiseEnPlace + ev.scoreTechnique + ev.scoreHygienePcc + ev.scoreSensory) / 4.0
                    appendLine("## Semana ${ev.weekNumber} — Média Selbstkontrolle: ${"%.2f".format(avg)} / 5.0")
                    appendLine("- Organização & MEP: ${ev.scoreMiseEnPlace}/5 | Técnica: ${ev.scoreTechnique}/5 | Higiene/PCC: ${ev.scoreHygienePcc}/5 | Sensorial: ${ev.scoreSensory}/5")
                    appendLine("- **Erkenntnisse (Aprendizados):** ${ev.erkenntnisse.ifBlank { "—" }}")
                    appendLine("- **Offene Punkte (Pontos em Aberto):** ${ev.offenePunkte.ifBlank { "—" }}")
                    appendLine()
                }
            }
            if (meps.isNotEmpty()) {
                appendLine("## Fichas MEP Preenchidas (${meps.size})")
                meps.forEach { m ->
                    appendLine("- **${m.recipeId}** (Sem. ${m.weekNumber}, ${m.targetServings} porções): PCC `${m.block5ThermalFlowPcc.take(120)}`")
                }
            }
        }
    }
}
