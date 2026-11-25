package com.example.data.curriculum

import com.example.domain.model.BrigadeStationSpec
import com.example.domain.model.BrochureGuideSpec
import com.example.domain.model.CookwareBladeSpec
import com.example.domain.model.CourseWeek
import com.example.domain.model.EquipmentTierItemSpec
import com.example.domain.model.EquipmentTierSpec
import com.example.domain.model.FiberEquivalentSpec
import com.example.domain.model.FlashcardSpec
import com.example.domain.model.IngredientSpec
import com.example.domain.model.RecipeSpec
import com.example.domain.model.SousVideTargetSpec
import com.example.domain.model.SugarStageSpec
import com.example.domain.model.SwissCutSpec
import org.json.JSONArray
import org.json.JSONObject

data class CanonicalCourseDataset(
    val courseTitle: String,
    val courseSubtitle: String,
    val equipmentTiers: List<EquipmentTierSpec>,
    val allEquipmentItems: List<EquipmentTierItemSpec>,
    val brochures: List<BrochureGuideSpec>,
    val weeks: List<CourseWeek>,
    val recipes: List<RecipeSpec>,
    val flashcards: List<FlashcardSpec>,
    val swissCuts: List<SwissCutSpec>,
    val cookwareAndBlades: List<CookwareBladeSpec>,
    val fiberEquivalentsFor10g: List<FiberEquivalentSpec>,
    val sousVideTargets: List<SousVideTargetSpec>,
    val sugarStagesLcb: List<SugarStageSpec>,
    val classicBrigade13Stations: List<BrigadeStationSpec>
)

object CourseJsonDatasetLoader {

    private const val RESOURCE_FILE_NAME = "curso-24-semanas-dados-v4_3370cebc.json"

    /** Carregado em IO por [CourseContent.preload] antes da UI ser montada. */
    val dataset: CanonicalCourseDataset by lazy { parseCanonicalDataset() }

    private fun readRawJsonText(): String = CourseContent.readText(RESOURCE_FILE_NAME)

    private fun parseCanonicalDataset(): CanonicalCourseDataset {
        val root = JSONObject(readRawJsonText())
        val meta = root.getJSONObject("course_metadata")

        // 1. Equipment Tiers (4 tiers, 37 items eq-...)
        val tiersArr = root.getJSONArray("equipment_tiers")
        val equipmentTiers = (0 until tiersArr.length()).map { i ->
            val tObj = tiersArr.getJSONObject(i)
            val tierNum = tObj.getInt("tier")
            val tierTitle = tObj.optString("name", tObj.optString("title", "Kit Nível $tierNum"))
            val itemsArr = tObj.getJSONArray("items")
            val items = (0 until itemsArr.length()).map { j ->
                val itObj = itemsArr.getJSONObject(j)
                EquipmentTierItemSpec(
                    id = itObj.getString("id"),
                    tierNumber = tierNum,
                    tierTitle = tierTitle,
                    name = itObj.getString("name"),
                    category = itObj.getString("category")
                )
            }
            EquipmentTierSpec(tierNumber = tierNum, title = tierTitle, items = items)
        }
        val allEquipmentItems = equipmentTiers.flatMap { it.items }

        // 2. Cycles metadata (for deliverables & names)
        val cyclesArr = root.getJSONArray("cycles")
        val cycleNameByNum = mutableMapOf<Int, String>()
        val cycleDeliverablesByNum = mutableMapOf<Int, List<String>>()
        for (i in 0 until cyclesArr.length()) {
            val cObj = cyclesArr.getJSONObject(i)
            val num = cObj.getInt("cycle_number")
            cycleNameByNum[num] = "Ciclo $num: ${cObj.getString("name")}"
            cycleDeliverablesByNum[num] = cObj.getJSONArray("portfolio_deliverables").toStringList()
        }

        // 3. Support Materials: Brochures (upgraded to -v3.pdf + v5 literature rules) & Swissmilk 23 index
        val supportObj = root.getJSONObject("support_materials")
        val brochuresArr = supportObj.getJSONArray("brochures")
        val v5BulletsByAxis = buildV5BrochureBulletsByAxis()
        val brochureByAxisKey = mutableMapOf<String, BrochureGuideSpec>()
        val brochures = (0 until brochuresArr.length()).map { i ->
            val bObj = brochuresArr.getJSONObject(i)
            val shortKey = bObj.getString("id") // "eixo-1" .. "eixo-6"
            val axisNum = bObj.getInt("axis_id")
            val v3FileName = bObj.getString("filename").replace("-v2.pdf", "-v3.pdf")
            val weeksCov = bObj.getJSONArray("weeks_covered").toIntList()
            val diagrams = bObj.getJSONArray("key_diagrams").toStringList()
            val featuredSummary = when (val feat = bObj.opt("swissmilk_recipes_featured")) {
                is JSONArray -> "Destaques Swissmilk: ${feat.toStringList().joinToString(", ")}"
                is String -> feat
                else -> ""
            }
            val spec = BrochureGuideSpec(
                id = v3FileName,
                axisNumber = axisNum,
                fileNameRef = v3FileName,
                title = bObj.getString("title").replace("Brochura ", "") + " (v3)",
                summary = "$featuredSummary • Semanas cobertas: ${weeksCov.joinToString(", ") { "S%02d".format(it) }}.",
                keyBulletRules = v5BulletsByAxis[axisNum].orEmpty(),
                weeksCovered = weeksCov,
                keyDiagrams = diagrams
            )
            brochureByAxisKey[shortKey] = spec
            spec
        }

        // Swissmilk 23 collection lookup (recipe_id -> (num, shortCanton, cycle, week))
        data class SwissmilkMeta(val num: Int, val cantonCode: String, val cycle: Int, val week: Int)
        val swissmilkMap = mutableMapOf<String, SwissmilkMeta>()
        val smArr = supportObj.getJSONArray("swissmilk_23_collection")
        for (i in 0 until smArr.length()) {
            val sObj = smArr.getJSONObject(i)
            swissmilkMap[sObj.getString("recipe_id")] = SwissmilkMeta(
                num = sObj.getInt("num"),
                cantonCode = sObj.getString("canton"),
                cycle = sObj.getInt("cycle"),
                week = sObj.getInt("week")
            )
        }

        // 4. First pass over Weeks to map recipe->weeks and flashcard->week
        val weeksArr = root.getJSONArray("weeks")
        val recipeWeeksMap = mutableMapOf<String, MutableList<Int>>()
        val recipeAxisMap = mutableMapOf<String, Int>()
        val flashcardWeekMap = mutableMapOf<String, Int>()
        val catalogObj = root.getJSONObject("recipes_catalog")

        for (i in 0 until weeksArr.length()) {
            val wObj = weeksArr.getJSONObject(i)
            val wk = wObj.getInt("week")
            val primaryAxis = wObj.getJSONArray("axis").optInt(0, 1)
            wObj.getJSONArray("practical_recipes").toStringList().forEach { recId ->
                recipeWeeksMap.getOrPut(recId) { mutableListOf() }.add(wk)
                recipeAxisMap.putIfAbsent(recId, primaryAxis)
            }
            wObj.getJSONArray("flashcards_ids").toStringList().forEach { fcId ->
                flashcardWeekMap.putIfAbsent(fcId, wk)
            }
        }

        // 5. Parse all 45 canonical recipes from recipes_catalog
        val parsedRecipes = mutableListOf<RecipeSpec>()
        val recKeys = catalogObj.keys()
        while (recKeys.hasNext()) {
            val key = recKeys.next()
            val rObj = catalogObj.getJSONObject(key)
            val recId = rObj.getString("id")
            val track = rObj.optString("recipe_track", "base_global")
            val smMeta = swissmilkMap[recId]
            val weeksForRec = recipeWeeksMap[recId]?.distinct()?.sorted()
                ?: listOfNotNull(smMeta?.week).ifEmpty { listOf(1) }
            val primaryWeek = weeksForRec.first()
            val cycleNum = ((primaryWeek - 1) / 6) + 1
            val axisId = recipeAxisMap[recId] ?: when (track) {
                "swissmilk_23" -> 2
                "chefkoch_step_by_step" -> 1
                else -> 4
            }

            val rawEquip = rObj.optJSONArray("equipment")?.toStringList().orEmpty()
            val recCookware = rObj.optJSONArray("recommended_cookware")?.toStringList().orEmpty()
            val eqIds = CourseRecipeStepEnricher.mapEquipmentToCanonicalIds(recId, rawEquip, recCookware)

            val cantonFull = rObj.optString("canton", "")
            val shortCanton = smMeta?.cantonCode ?: extractShortCanton(cantonFull)

            val nutObj = rObj.optJSONObject("nutrition_per_serving")
            val svObj = rObj.optJSONObject("sous_vide_params")
            val svSummary = if (svObj != null) {
                buildString {
                    val brine = svObj.optString("brine", "")
                    if (brine.isNotBlank()) append("Salmoura: $brine • ")
                    append("Centro/Banho: ${svObj.optDouble("core_temp_c", 0.0)} °C (${svObj.optString("bath_temp_rule", "+0,8 °C")}) por ${svObj.optString("cook_time", "")}")
                }
            } else ""

            val ingredientsArr = rObj.getJSONArray("ingredients")
            val ingredients = (0 until ingredientsArr.length()).map { idx ->
                val ingObj = ingredientsArr.getJSONObject(idx)
                IngredientSpec(
                    name = ingObj.getString("item"),
                    netAmountPl = ingObj.optDouble("qty", 1.0),
                    unit = ingObj.optString("unit", "g"),
                    correctionFactorFc = ingObj.optDouble("fc", 1.0).coerceAtLeast(1.0),
                    sector = CourseRecipeStepEnricher.normalizeSector(ingObj.optString("sector", "Mercearia"))
                )
            }

            val rawSteps = rObj.getJSONArray("steps").toStringList()
            val steps = CourseRecipeStepEnricher.buildStepsForRecipe(recId, rawSteps, recCookware, rawEquip)
            val criticalControls = rObj.optJSONArray("critical_controls")?.toStringList().orEmpty()
            val sourceRef = rObj.optString("source", "")
            val diff = rObj.optString("difficulty", "Base")
            val prepMin = rObj.optInt("prep_time_min", 0)
            val totalMin = rObj.optInt("total_time_min", 0)
            val yieldUnit = rObj.optString("yield_unit", "porções")

            val subtitle = buildString {
                if (cantonFull.isNotBlank()) append("$cantonFull • ")
                append("Nível $diff")
                if (totalMin > 0) append(" • ${totalMin} min total")
                if (sourceRef.isNotBlank()) append(" • $sourceRef")
            }

            parsedRecipes += RecipeSpec(
                id = recId,
                weekNumber = primaryWeek,
                cycleNumber = cycleNum,
                axisId = axisId,
                title = rObj.getString("name"),
                subtitle = subtitle,
                baseServings = rObj.optInt("base_servings", 4),
                cantonCode = shortCanton,
                isSwissmilkClassic = track == "swissmilk_23",
                isChefkochV4 = track == "chefkoch_step_by_step",
                requiredEquipmentIds = eqIds,
                pccTargetSummary = criticalControls.joinToString(" | ").ifBlank { "Cumprir boas práticas GHP/APPCC e controle térmico." },
                ingredients = ingredients,
                steps = steps,
                sourceRef = sourceRef,
                difficultyLevel = diff,
                altScaleServings = if (rObj.has("alt_scale_servings") && !rObj.isNull("alt_scale_servings")) rObj.getInt("alt_scale_servings") else null,
                yieldUnit = yieldUnit,
                prepTimeMin = prepMin,
                totalTimeMin = totalMin,
                caloriesKcal = if (rObj.has("calories_kcal") && !rObj.isNull("calories_kcal")) rObj.getInt("calories_kcal") else null,
                proteinG = nutObj?.optDouble("protein_g"),
                carbsG = nutObj?.optDouble("carbs_g"),
                fatG = nutObj?.optDouble("fat_g"),
                swissmilkNum = smMeta?.num ?: if (rObj.has("swissmilk_num") && !rObj.isNull("swissmilk_num")) rObj.getInt("swissmilk_num") else null,
                cantonFullLabel = cantonFull,
                sousVideSummary = svSummary,
                equipmentNames = (recCookware + rawEquip).distinct(),
                weeksAppearing = weeksForRec
            )
        }

        val sortedRecipes = parsedRecipes.sortedWith(
            compareBy<RecipeSpec> { it.weekNumber }
                .thenByDescending { it.isChefkochV4 }
                .thenBy { it.swissmilkNum ?: 999 }
                .thenBy { it.title }
        )
        val recipeById = sortedRecipes.associateBy { it.id }

        // 6. Build 24 CourseWeeks with full 80-min 5-phase blocks derived from authentic JSON topics & recipes
        val weeks = (0 until weeksArr.length()).map { i ->
            val wObj = weeksArr.getJSONObject(i)
            val wk = wObj.getInt("week")
            val cyc = wObj.getInt("cycle")
            val axes = wObj.getJSONArray("axis").toIntList()
            val primaryAxis = axes.firstOrNull() ?: 1
            val level = wObj.optString("level", "Base")
            val title = wObj.getString("title")
            val topics = wObj.getJSONArray("theory_topics").toStringList()
            val pracIds = wObj.getJSONArray("practical_recipes").toStringList()
            val fcIds = wObj.getJSONArray("flashcards_ids").toStringList()
            val brochureKeys = wObj.getJSONArray("support_brochures").toStringList()
            val mappedBrochureIds = brochureKeys.mapNotNull { brochureByAxisKey[it]?.id }
                .ifEmpty { listOfNotNull(brochures.firstOrNull()?.id) }
            val mepFocus = wObj.getString("ficha_mep_focus")
            val recipeNames = pracIds.mapNotNull { recipeById[it]?.title }.joinToString(" + ")

            CourseWeek(
                weekNumber = wk,
                cycleNumber = cyc,
                cycleName = cycleNameByNum[cyc] ?: "Ciclo $cyc",
                axisId = primaryAxis,
                title = title,
                concept10Min = topics.getOrNull(0) ?: title,
                visualization20Min = topics.drop(1).joinToString(" • ").ifBlank {
                    "Esquemas visuais (~25% diagramas) das brochuras ${mappedBrochureIds.joinToString(", ")}."
                },
                application30Min = "Execução prática orientada: $recipeNames — aplicando $mepFocus.",
                practice15Min = "Preenchimento da Ficha Técnica MEP (Frente Blocos 1–4 pré-fogo; Verso Blocos 5–8 e PCCs) para ${pracIds.size} preparo(s).",
                synthesis5Min = "Revisão espaçada SM-2 dos cartões ${fcIds.joinToString(", ")} e autoavaliação Selbstkontrolle (1–5).",
                mepFocus = mepFocus,
                brochureId = mappedBrochureIds.first(),
                levelName = level,
                axesIds = axes,
                theoryTopics = topics,
                practicalRecipeIds = pracIds,
                flashcardIds = fcIds,
                supportBrochureIds = mappedBrochureIds,
                cycleDeliverables = cycleDeliverablesByNum[cyc].orEmpty()
            )
        }

        // 7. Parse all 60 canonical flashcards from flashcards_bank
        val fcArr = root.getJSONArray("flashcards_bank")
        val flashcards = (0 until fcArr.length()).map { i ->
            val fObj = fcArr.getJSONObject(i)
            val fcId = fObj.getString("id")
            val axis = fObj.getInt("axis")
            val week = flashcardWeekMap[fcId] ?: ((axis - 1) * 4 + 1).coerceIn(1, 24)
            FlashcardSpec(
                id = fcId,
                weekNumber = week,
                category = fObj.getString("topic"),
                frontQuestion = fObj.getString("q"),
                backAnswer = fObj.getString("a"),
                axisId = axis
            )
        }

        // 8. Technical References (Cuts + v5 George Bento, Cookware & Knives, Fiber 10 items, Sous-vide, Sugar, Brigade)
        val techObj = root.getJSONObject("technical_references")
        val cutsArr = techObj.getJSONArray("swiss_cuts_matrix")
        val swissCuts = (0 until cutsArr.length()).map { i ->
            val cObj = cutsArr.getJSONObject(i)
            val cutName = cObj.getString("cut")
            val rawSize = cObj.getString("size")
            val enrichedSize = if (cutName.equals("Julienne", ignoreCase = true) && !rawSize.contains("George Bento")) {
                "$rawSize (variação 3 mm — George Bento)"
            } else {
                rawSize
            }
            SwissCutSpec(
                id = cutName.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                name = cutName,
                dimensionSwiss = enrichedSize,
                bladeTool = cObj.getString("tool"),
                typicalApplications = cObj.getString("uses")
            )
        }

        val ckObj = techObj.getJSONObject("cookware_and_knives")
        val cookwareArr = ckObj.getJSONArray("cookware")
        val knivesArr = ckObj.getJSONArray("knives")
        val cookwareSpecs = (0 until cookwareArr.length()).map { i ->
            val o = cookwareArr.getJSONObject(i)
            val fr = o.getString("fr")
            val tier = when {
                fr.contains("antiadhésif", true) -> 1
                fr.contains("Poissonnière", true) || fr.contains("pression", true) -> 4
                fr.contains("Rondeau", true) -> 3
                else -> 2
            }
            CookwareBladeSpec(
                id = "ck_${fr.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                namePt = "${o.getString("fr")} (${o.getString("en")})",
                nameDe = o.getString("de"),
                nameFr = fr,
                category = "Bateria Térmica",
                tierLevel = tier,
                kitchenRule = o.getString("pt_rule")
            )
        }
        val knifeSpecs = (0 until knivesArr.length()).map { i ->
            val o = knivesArr.getJSONObject(i)
            val de = o.getString("de")
            CookwareBladeSpec(
                id = "kn_${de.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                namePt = o.getString("pt"),
                nameDe = de,
                nameFr = o.getString("fr"),
                category = "Lâminas & Corte",
                tierLevel = if (de.contains("Rüstmesser")) 1 else 3,
                kitchenRule = "Cutelaria de precisão suíça (${o.getString("de")} / ${o.getString("fr")}): manter fio agudo para não esmagar células vegetais."
            )
        } + listOf(
            CookwareBladeSpec(
                id = "kn_kochmesser",
                namePt = "Faca do Chef 20–25 cm",
                nameDe = "Kochmesser / Chefmesser",
                nameFr = "Couteau de chef",
                category = "Lâminas & Corte",
                tierLevel = 1,
                kitchenRule = "Lâmina mestra para Brunoise (1,5 mm), Jardinière (5 mm), Macédoine (8 mm), Julienne e Bâtonnets."
            ),
            CookwareBladeSpec(
                id = "ck_tamis",
                namePt = "Peneira fina de tambor (Tamis)",
                nameDe = "Haarsieb / Passiersieb",
                nameFr = "Tamis",
                category = "Confeitaria & Precisão",
                tierLevel = 3,
                kitchenRule = "Refinar farce mousseline de galantinas (< 10 °C), cremes pochados, purês e fluid gels."
            )
        )

        val nutTech = techObj.getJSONObject("nutrition_carbs_and_fibers")
        val fibArr = nutTech.getJSONArray("ten_grams_fiber_equivalents")
        val fiberSpecs = (0 until fibArr.length()).map { i ->
            val o = fibArr.getJSONObject(i)
            val grams = o.getInt("grams_for_10g_fiber")
            FiberEquivalentSpec(
                foodName = o.getString("food"),
                gramsFor10gFiber = grams,
                culinaryNote = when {
                    grams <= 55 -> "Altíssima densidade de fibras (3 porções equivalentes atingem ≥ 30 g/dia com baixo volume)."
                    grams <= 150 -> "Fonte equilibrada para guarnições integrais, saladas mornas e panificação."
                    else -> "Baixa densidade de fibras por peso — compare com equivalentes integrais e leguminosas."
                }
            )
        }

        val svTech = techObj.getJSONObject("sous_vide_protocols").getJSONArray("validated_targets")
        val svTargets = (0 until svTech.length()).map { i ->
            val o = svTech.getJSONObject(i)
            SousVideTargetSpec(
                item = o.getString("item"),
                thickness = o.getString("thickness"),
                tempC = o.getDouble("temp_c"),
                time = o.getString("time")
            )
        }

        val sugarArr = techObj.getJSONObject("puff_pastry_and_sugar_specs").getJSONArray("sugar_stages_lcb")
        val sugarStages = (0 until sugarArr.length()).map { i ->
            val o = sugarArr.getJSONObject(i)
            SugarStageSpec(
                stage = o.getString("stage"),
                tempC = o.getString("temp_c"),
                uses = o.getString("uses")
            )
        }

        val rolesArr = techObj.getJSONObject("brigade_and_org_charts").getJSONArray("classic_13_roles")
        val brigadeRoles = (0 until rolesArr.length()).map { i ->
            val o = rolesArr.getJSONObject(i)
            val de = o.getString("de")
            BrigadeStationSpec(
                order = i + 1,
                titleDe = de,
                titleFr = o.getString("fr"),
                rolePt = de,
                coreResponsibility = o.getString("role"),
                isHighlightedSpecialRole = de.contains("Mitarbeiterkoch") || de.contains("Diätkoch")
            )
        }

        return CanonicalCourseDataset(
            courseTitle = meta.getString("title"),
            courseSubtitle = meta.optString("subtitle", "Currículo Suíço Ampliado • ${meta.optInt("total_weeks", 24)} Semanas • ${meta.optInt("daily_study_block_minutes", 80)} min/bloco"),
            equipmentTiers = equipmentTiers,
            allEquipmentItems = allEquipmentItems,
            brochures = brochures,
            weeks = weeks,
            recipes = sortedRecipes,
            flashcards = flashcards,
            swissCuts = swissCuts,
            cookwareAndBlades = cookwareSpecs + knifeSpecs,
            fiberEquivalentsFor10g = fiberSpecs,
            sousVideTargets = svTargets,
            sugarStagesLcb = sugarStages,
            classicBrigade13Stations = brigadeRoles
        )
    }

    private fun extractShortCanton(cantonFull: String): String {
        val m = Regex("""\(([A-Z/]+)\)""").find(cantonFull)
        return m?.groupValues?.get(1).orEmpty()
    }

    private fun buildV5BrochureBulletsByAxis(): Map<Int, List<String>> = mapOf(
        1 to listOf(
            "Schnittarten suíços + variação clássica de 3 mm na Julienne (George Bento); facas cegas esmagam células e oxidam vegetais.",
            "Concentração vs. Expansão (Institut Paul Bocuse), dépouiller e vanner (Escoffier) e estabilização contemporânea de molhos (James Peterson: xantana 0,1–0,5%, lecitina 1–2%, alginato PG 0,3–0,9%, fluid gel ágar).",
            "Kochgeschirrarten: Sautoir/Rondeau para molhos de fundo, Braisière para Poelieren (140–160 °C s/ líquido) e antiaderente obrigatória para preservar vitamina C."
        ),
        2 to listOf(
            "Catálogo integral dos 23 Clássicos Cantonais Swissmilk (AG a ZH) distribuídos por técnica e ciclo.",
            "Cozinha Clássica Italiana (Marcella Hazan): sequência battuto → soffritto → insaporire, sfoglia fresca sem sal/óleo/água e risotto all'onda vs. piemontês.",
            "Cozinha Brasileira em 5 regiões (Paravati & Dória: Biodiversidade → Ingrediente → Produto → Receita) e Atlas Mundial (Mina Holland: 7 refogados e 9 blends)."
        ),
        3 to listOf(
            "Teige (Massas Folhadas): 0,5 kg gordura/kg farinha → 3 dobras duplas (Halb); 0,6–0,7 kg → 4 dobras duplas (Deutscher); congelar crua (-18 °C) para evitar oxidação a 5 °C.",
            "Ciência da Confeitaria (Canella-Rawls): fervura plena (98–100 °C) obrigatória em cremes com gema + amido para inativar alfa-amilase vs. teto de 79–80 °C na crème anglaise.",
            "Conversão de gelatina (Diego Costa — Senac): 1 envelope (12 g pó) = 6 folhas (~2 g/folha); 1 colher de chá pó = 1 folha (hidratar pó em 5× água fria) + escala LCB (116–145 °C)."
        ),
        4 to listOf(
            "Higienização das mãos (99%) seguida de antissepsia alcoólica em mãos secas (99,99%), sanitização 200 ppm/15 min e 4 POPs RDC 216.",
            "Acrilamida na fritura: pré-fritar a 140 °C e finalizar a 165–175 °C (teto estrito de 175 °C; pirólise lipídica > 180 °C).",
            "Sous-vide & Galantina: salmoura 5–10% (10 min peixes / 3–4 h carnes), banho +0,5 a +1,0 °C acima do centro, peças ≤ 7 cm e PCC 65 °C na Geflügelgalantine."
        ),
        5 to listOf(
            "Fisiologia multissensorial: refutação do mapa da língua, umami (Ikeda, 1912), ISO 3972, teste genético PROP (25/50/25%) e degustação ótima a 20–30 °C.",
            "Enologia WSET SAT em 4 etapas (Aspecto, Olfato 1º/2º/3º, Paladar, Conclusões) e patrimônio vitivinícola suíço (~250 castas; Valais > 50).",
            "Normas SCA CVA de cafés especiais (espresso 90–96 °C em 25–30 s), infusões ≤ 83 °C e Lista Oficial IBA de coquetelaria."
        ),
        6 to listOf(
            "Fachrechnen: perdas multiplicativas em cascata PB = PL / ∏(1 - perda_i), precificação BVP/EVP com MWST 8%, Prime-Cost (~72%) e Break-even x_cr = Kf / (p - kv).",
            "Ernaehrung & Dietoterapia (TBCA-USP): amido 80% amilopectina / 20% amilose, meta ≥ 30 g/dia de fibras (sequestro biliar anticolesterol) e redução de potássio até 60% na DRC.",
            "Betriebsorganisation & Führung: 3 organogramas (Hotel, Restaurante, Coletiva), 13 postos da brigada (destaque Diätkoch e Mitarbeiterkoch) e Dossier Lernreflexion."
        )
    )

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun JSONArray.toIntList(): List<Int> =
        (0 until length()).map { getInt(it) }
}
