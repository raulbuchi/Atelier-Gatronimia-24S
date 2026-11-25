package com.example.data.curriculum

import com.example.domain.model.CookingStepSpec
import com.example.domain.model.SmartKitchenAlertType

internal object CourseRecipeStepEnricher {

    private val tempRegex = Regex("""(-?\d{1,3})(?:[.,]\d+)?\s*°C""")
    private val minRegex = Regex("""~?\s*(\d{1,3})(?:[–-](\d{1,3}))?\s*min""", RegexOption.IGNORE_CASE)
    private val hourRegex = Regex("""~?\s*(\d{1,2})(?:[.,](\d))?\s*h(?:oras?)?\b""", RegexOption.IGNORE_CASE)

    fun normalizeSector(rawSector: String): String {
        val s = rawSector.lowercase()
        return when {
            "açougue" in s || "charcutaria" in s || "peixaria" in s || "pescado" in s -> "Açougue & Peixaria"
            "hortifr" in s -> "Hortifrúti"
            "latic" in s || "ovo" in s || "queijo" in s -> "Laticínios & Ovos"
            "tempero" in s || "especiar" in s || "bebida" in s || "líquido" in s -> "Especiarias & Fundos"
            else -> "Mercearia Seca"
        }
    }

    fun mapEquipmentToCanonicalIds(
        recipeId: String,
        rawEquipment: List<String>,
        recommendedCookware: List<String>
    ): List<String> {
        val blob = (rawEquipment + recommendedCookware + recipeId).joinToString(" | ").lowercase()
        // Somente ids do inventário canônico (`equipment_tiers` do JSON v4); a faca de legumes
        // (Rüstmesser) já faz parte de `eq-faca-chef`. Coberto por CourseDatasetIntegrityTest.
        val ids = linkedSetOf("eq-balanca", "eq-faca-chef", "eq-tabuas")
        fun has(vararg keys: String) = keys.any { it in blob }

        // Kit 1 — Essencial
        if (has("termômetro", "sonda", "°c")) ids += "eq-termometro"
        if (has("timer")) ids += "eq-timer"
        if (has("jarra", "copo medidor", "colheres-medida", "colher de chá")) ids += "eq-medidores"
        if (has("descascador", "sparschäler")) ids += "eq-descascador"
        if (has("bowl", "tigela", "recipiente gn", "pote hermético")) ids += "eq-bowls"
        if (has("peneira", "escorredor")) ids += "eq-peneira"
        if (has("fouet", "schwingbesen", "colher de pau", "espátula de confeiteiro")) ids += "eq-fouet"
        if (has("caldeir", "marmite", "panela")) ids += "eq-panelas"
        if (has("liquidificador")) ids += "eq-liquidificador"
        if (has("faca de torneado", "tourniermesser")) ids += "eq-tourniermesser"
        if (has("antiaderente", "antihaft", "rosti", "rösti")) ids += "eq-antihaft"
        // Kit 2 — Cozinha quente e suíça
        if (has("ralador", "röstiraffel", "bircherraffel", "rosti", "rösti")) ids += "eq-rostiraffel"
        if (has("bratschaufel", "espátula larga", "prato raso grande")) ids += "eq-bratschaufel"
        if (has("caquelon", "fondue", "rechaud")) ids += "eq-caquelon"
        if (has("sauteuse", "frigideira larga", "sauté pan", "bratpfanne")) ids += "eq-sauteuse"
        if (has("chinois", "passiertuch")) ids += "eq-chinois"
        if (has("mixer", "stabmixer", "processador")) ids += "eq-mixer"
        if (has("refratári", "travessa", "assadeira")) ids += "eq-refratario"
        if (has("braisi", "schmor", "brattopf", "barro", "holand")) ids += "eq-braisiere"
        if (has("rôtissoire", "rotissoire", "assadeira funda")) ids += "eq-rotissoire"
        if (has("sautoir", "rondeau", "flachkasserolle", "caçarola")) ids += "eq-rondeau-sautoir"
        if (has("lyoner", "frigideira de aço", "ferro")) ids += "eq-lyoner"
        if (has("buntschneide", "kanneliermesser", "zester", "ondulad")) ids += "eq-buntschneide"
        // Kit 3 — Panificação, confeitaria e técnicas especiais
        if (has("rolo de massa", "wallholz", "teighorn", "raspador", "folhad", "blatterteig")) ids += "eq-rolo"
        if (has("forma ", "forma de", "formas", "aro removível", "desmontável", "donuts")) ids += "eq-formas"
        if (has("batedeira", "grade de resfriamento")) ids += "eq-batedeira"
        if (has("saco de confeitar", "bico", "pincel")) ids += "eq-saco-confeitar"
        if (has("calda", "caramelo", "chocolate", "infravermelho", "frit")) ids += "eq-termometro-calda"
        if (has("termocirculador", "sous-vide", "seladora", "vácuo") || "-sv" in recipeId) ids += "eq-sous-vide"
        if (has("pressão", "vapor", "dampfkochtopf")) ids += "eq-pressao"
        if (has("tamis", "timbale", "kokotten", "ramequin")) ids += "eq-tamis-timbale"

        return ids.toList()
    }

    fun buildStepsForRecipe(
        recipeId: String,
        rawSteps: List<String>,
        recommendedCookware: List<String>,
        rawEquipment: List<String>
    ): List<CookingStepSpec> {
        val defaultCookware = recommendedCookware.firstOrNull()
            ?: rawEquipment.firstOrNull()
            ?: "Bancada inox • Faca do Chef (Chefmesser)"

        return rawSteps.mapIndexed { idx, stepText ->
            val stepNum = idx + 1
            val tempC = extractTargetTempC(stepText)
            val mins = extractTimerMinutes(stepText)
            val title = inferStepTitle(stepNum, rawSteps.size, stepText)
            val cookware = inferStepCookware(stepText, defaultCookware)
            val (alertType, alertMessage) = detectSmartAlert(recipeId, stepNum, rawSteps.size, stepText)

            CookingStepSpec(
                stepNumber = stepNum,
                title = title,
                instruction = stepText,
                cookwareDeFrPt = cookware,
                targetTempC = tempC,
                timerMinutes = mins,
                alertType = alertType,
                alertBannerMessage = alertMessage
            )
        }
    }

    private fun extractTargetTempC(text: String): Int? {
        val matches = tempRegex.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        return matches.firstOrNull { it in -20..260 }
    }

    private fun extractTimerMinutes(text: String): Int? {
        minRegex.find(text)?.let { m ->
            val a = m.groupValues[1].toIntOrNull()
            val b = m.groupValues.getOrNull(2)?.toIntOrNull()
            if (a != null) return b ?: a
        }
        if (text.contains("2h30", ignoreCase = true)) return 150
        hourRegex.find(text)?.let { h ->
            val hrs = h.groupValues[1].toIntOrNull() ?: return null
            val dec = h.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            return (hrs * 60) + (dec * 6)
        }
        return null
    }

    private fun inferStepTitle(stepNum: Int, totalSteps: Int, text: String): String {
        val low = text.lowercase()
        return when {
            "salmoura" in low -> "Salmoura Osmótica Controlada"
            "vácuo" in low || "embalar" in low -> "Selagem a Vácuo & Banho Térmico"
            "desossar" in low || "farce" in low -> "Desossa & Emulsão Fria (Farce)"
            "dobra" in low || "laminar" in low || "tour" in low -> "Laminação & Dobras (Tours)"
            "marinar" in low || "marinada" in low -> "Marinada Refrigerada & Secagem"
            "selar" in low || "saltear" in low || "dourar" in low -> "Selagem & Reação de Maillard"
            "deglaçar" in low || "reduzir" in low -> "Deglaçagem & Construção do Molho"
            "pochar" in low || "banho-maria" in low -> "Pochage em Sub-Ebulição Controlada"
            "assar" in low || "forno" in low -> "Cocção de Forno & Controle Térmico"
            "fritar" in low || "óleo" in low -> "Fritura sob Controle de Acrilamida"
            stepNum == 1 -> "Mise en Place & Pré-Preparo (AVOR)"
            stepNum == totalSteps -> "Finalização, Empratamento & Passe"
            else -> "Execução Técnica — Etapa $stepNum"
        }
    }

    private fun inferStepCookware(text: String, fallback: String): String {
        val low = text.lowercase()
        return when {
            "rôtissoire" in low || "assadeira funda" in low -> "Bratgeschirr / Rôtissoire (Assadeira funda de forno)"
            "sautoir" in low || "flachkasserolle" in low -> "Flachkasserolle / Sautoir (Caçarola rasa de bordas retas)"
            "antiaderente" in low || "antihaft" in low -> "Bratpfanne Antihaft / Poêle antiadhésif (Frigideira antiaderente)"
            "lyoner" in low -> "Lyoner Pfanne / Poêle lyonnaise (Frigideira de aço)"
            "braisi" in low || "schmor" in low -> "Schmorgeschirr / Braisière (Caçarola com tampa)"
            "tamis" in low || "peneira" in low || "passiertuch" in low -> "Haarsieb / Tamis & Passiertuch (Peneira fina / Étamine)"
            "termocirculador" in low || "vácuo" in low -> "Sous-vide-Garer & Vakuumierer (Termocirculador + Vácuo)"
            "caquelon" in low -> "Caquelon (Panela tradicional de fondue)"
            else -> fallback
        }
    }

    private fun detectSmartAlert(
        recipeId: String,
        stepNum: Int,
        totalSteps: Int,
        text: String
    ): Pair<SmartKitchenAlertType, String> {
        val low = text.lowercase()
        return when {
            recipeId in setOf("rec-kabeljau-sv", "rec-kalbsbrust-sv", "rec-artischocken-sv") &&
                ("salmoura" in low || "vácuo" in low || "banho" in low || stepNum == 1) ->
                SmartKitchenAlertType.SOUS_VIDE_PROTOCOL to
                    "Protocolo Sous-Vide: salmoura de 5–10% (10 min pescados / 3–4 h carnes), peças ≤ 7 cm sem ossos e banho regulado +0,5 a +1,0 °C acima do centro desejado."

            recipeId == "rec-geflugelgalantine" && ("farce" in low || "65 °c" in low || "alumínio" in low) ->
                SmartKitchenAlertType.GALANTINE_PCC_65C to
                    "PCC Galantina (65 °C): manter farce mousseline sobre gelo (< 10 °C), furar o alumínio para liberar ar/vapor e pochar até exatos 65 °C no centro geométrico."

            recipeId in setOf("rec-blatterteig", "rec-luzerner-chugelipastete", "rec-neuenburger-saucisson") &&
                ("dobra" in low || "folhada" in low || "congel" in low || stepNum == totalSteps) ->
                SmartKitchenAlertType.PUFF_PASTRY_OXIDATION to
                    "Conservação de Folhados (Teige): jamais estocar massa folhada crua por vários dias a 5 °C (acidificação e oxidação acinzentada); congelar crua a -18 °C."

            recipeId in setOf("rec-berner-platte", "rec-bollito-misto", "rec-papet-vaudois") &&
                ("82 °c" in low || "80" in low || "pochar" in low || "120 min" in low || "90 min" in low) ->
                SmartKitchenAlertType.PARALLEL_POACHING_82C to
                    "Sub-Ebulição & Sincronia (~82 °C): pochar embutidos e cortes curados sem ferver para impedir rompimento da tripa e ressecamento proteico."

            (recipeId == "rec-donuts-blueberry" || "fritar" in low || "170 °c" in low || "175 °c" in low) &&
                ("óleo" in low || "frit" in low) ->
                SmartKitchenAlertType.DEEP_FRY_ACRYLAMIDE to
                    "Controle de Acrilamida: pré-fritar a 140 °C quando aplicável e finalizar entre 165–175 °C. Nunca exceder 175 °C (formação acentuada de acrilamida e pirólise > 180 °C)."

            recipeId in setOf("rec-brauner-kalbsfond", "rec-fundo-vegetais") && ("água fria" in low || "fervura branda" in low) ->
                SmartKitchenAlertType.CONCENTRATION_VS_EXPANSION to
                    "Cocção por Expansão (Institut Paul Bocuse): partir sempre de água fria para extrair colágeno e compostos aromáticos por osmose, mantendo fervura branda (dépouiller constante)."

            recipeId in setOf("rec-zurcher-geschnetzeltes", "rec-bison-mignons") && ("selar" in low || "saltear" in low || "porções" in low || "lotes" in low) ->
                SmartKitchenAlertType.CONCENTRATION_VS_EXPANSION to
                    "Cocção por Concentração (Paul Bocuse): selar em Sautoir bem quente em lotes pequenos para coagular a superfície sem derrubar a temperatura nem exsudar suco."

            recipeId in setOf("rec-vanilleauflauf", "rec-karamellcreme", "rec-walliser-aprikosenflan") &&
                ("80 °c" in low || "banho-maria" in low || "amido" in low || "ferv" in low) ->
                SmartKitchenAlertType.STARCH_AMYLASE_CREAMS to
                    "Ciência da Confeitaria (Canella-Rawls): cremes com gema + amido exigem fervura plena (98–100 °C) para inativar a alfa-amilase; já cremes pochados sem amido operam a 80–82 °C sem ferver."

            recipeId == "rec-aquafaba-mayo" && stepNum == 2 ->
                SmartKitchenAlertType.MODERNIST_HYDROCOLLOIDS to
                    "Emulsões & Hidrocoloides (James Peterson): verter o óleo em fio sob cisalhamento contínuo; para estabilizar vinagretes/emulsões longas, 0,1–0,3% de goma xantana evita coalescência."

            ("antiaderente" in low || "vagem" in low || "espinafre" in low || "couve-flor" in low || "legumes" in low) &&
                ("saltear" in low || "refogar" in low || "frigideira" in low) ->
                SmartKitchenAlertType.NON_STICK_VITAMIN_C to
                    "Preservação de Vitamina C: saltear vegetais exclusivamente em frigideira antiaderente (Poêle antiadhésif) ou inox revestido — o aço carbono puro destrói a vitamina C."

            else -> SmartKitchenAlertType.NONE to ""
        }
    }
}
