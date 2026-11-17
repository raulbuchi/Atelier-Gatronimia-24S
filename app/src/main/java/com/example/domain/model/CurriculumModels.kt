package com.example.domain.model

data class CourseWeek(
    val weekNumber: Int,
    val cycleNumber: Int,
    val cycleName: String,
    val axisId: Int,
    val title: String,
    val concept10Min: String,
    val visualization20Min: String,
    val application30Min: String,
    val practice15Min: String,
    val synthesis5Min: String,
    val mepFocus: String,
    val brochureId: String,
    val levelName: String = "",
    val axesIds: List<Int> = listOf(axisId),
    val theoryTopics: List<String> = emptyList(),
    val practicalRecipeIds: List<String> = emptyList(),
    val flashcardIds: List<String> = emptyList(),
    val supportBrochureIds: List<String> = listOf(brochureId),
    val cycleDeliverables: List<String> = emptyList()
)

data class IngredientSpec(
    val name: String,
    val netAmountPl: Double,
    val unit: String,
    val correctionFactorFc: Double = 1.0,
    val sector: String
)

enum class SmartKitchenAlertType {
    NONE,
    NON_STICK_VITAMIN_C,        // Proibição de aço puro p/ vegetais (preservar vitamina C)
    DEEP_FRY_ACRYLAMIDE,        // 140 °C pré-fritura / 165-175 °C teto rígido contra acrilamida
    POELIEREN_MATIGNON,         // 140-160 °C tampado sobre matignon sem líquido adicionado
    SOUS_VIDE_PROTOCOL,         // Salmoura 5-10% + banho +0.5 a +1.0 °C acima do centro térmico
    GALANTINE_PCC_65C,          // Centro térmico de 65 °C na galantina + farce sobre gelo
    PUFF_PASTRY_OXIDATION,      // Alerta contra repouso longo a 5 °C (acinzentamento/acidificação)
    PARALLEL_POACHING_82C,      // Controle paralelo Berner Platte / Bollito misto (~82 °C)
    CONCENTRATION_VS_EXPANSION, // Institut Paul Bocuse: concentração (calor/líquido quente) vs. expansão (água fria)
    STARCH_AMYLASE_CREAMS,      // Canella-Rawls: fervura 98-100 °C p/ alfa-amilase (gema+amido) vs. anglaise <= 79-80 °C
    MODERNIST_HYDROCOLLOIDS     // James Peterson: xantana 0,1-0,5%, lecitina 1-2%, alginato PG 0,3-0,9%, fluid gel ágar
}

data class CookingStepSpec(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val cookwareDeFrPt: String,
    val targetTempC: Int? = null,
    val timerMinutes: Int? = null,
    val alertType: SmartKitchenAlertType = SmartKitchenAlertType.NONE,
    val alertBannerMessage: String = ""
)

data class RecipeSpec(
    val id: String,
    val weekNumber: Int,
    val cycleNumber: Int,
    val axisId: Int,
    val title: String,
    val subtitle: String,
    val baseServings: Int,
    val cantonCode: String = "",
    val isSwissmilkClassic: Boolean = false,
    val isChefkochV4: Boolean = false,
    val requiredEquipmentIds: List<String>,
    val pccTargetSummary: String,
    val ingredients: List<IngredientSpec>,
    val steps: List<CookingStepSpec>,
    val sourceRef: String = "",
    val difficultyLevel: String = "",
    val altScaleServings: Int? = null,
    val yieldUnit: String = "porções",
    val prepTimeMin: Int = 0,
    val totalTimeMin: Int = 0,
    val caloriesKcal: Int? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val swissmilkNum: Int? = null,
    val cantonFullLabel: String = "",
    val sousVideSummary: String = "",
    val equipmentNames: List<String> = emptyList(),
    val weeksAppearing: List<Int> = listOf(weekNumber)
) {
    val recipeTrack: String
        get() = when {
            isChefkochV4 -> "chefkoch_step_by_step"
            isSwissmilkClassic -> "swissmilk_23"
            else -> "base_global"
        }
}

data class EquipmentTierItemSpec(
    val id: String,
    val tierNumber: Int,
    val tierTitle: String,
    val name: String,
    val category: String
)

data class EquipmentTierSpec(
    val tierNumber: Int,
    val title: String,
    val items: List<EquipmentTierItemSpec>
)

data class SousVideTargetSpec(
    val item: String,
    val thickness: String,
    val tempC: Double,
    val time: String
)

data class SugarStageSpec(
    val stage: String,
    val tempC: String,
    val uses: String
)

data class SwissCutSpec(
    val id: String,
    val name: String,
    val dimensionSwiss: String,
    val bladeTool: String,
    val typicalApplications: String
)

data class CookwareBladeSpec(
    val id: String,
    val namePt: String,
    val nameDe: String,
    val nameFr: String,
    val category: String,
    val tierLevel: Int,
    val kitchenRule: String
)

data class SwissBeefPrimalSpec(
    val code: String,
    val nameDe: String,
    val nameFr: String,
    val namePt: String,
    val connectiveTissueTier: String,
    val subcuts: List<String>,
    val idealTechniques: List<String>
)

data class BrochureGuideSpec(
    val id: String,
    val axisNumber: Int,
    val fileNameRef: String,
    val title: String,
    val summary: String,
    val keyBulletRules: List<String>,
    val weeksCovered: List<Int> = emptyList(),
    val keyDiagrams: List<String> = emptyList()
)

data class FlashcardSpec(
    val id: String,
    val weekNumber: Int,
    val category: String,
    val frontQuestion: String,
    val backAnswer: String,
    val axisId: Int = 1
)

data class FiberEquivalentSpec(
    val foodName: String,
    val gramsFor10gFiber: Int,
    val culinaryNote: String
)

data class BrigadeStationSpec(
    val order: Int,
    val titleDe: String,
    val titleFr: String,
    val rolePt: String,
    val coreResponsibility: String,
    val isHighlightedSpecialRole: Boolean = false
)

data class OrgChartModelSpec(
    val id: String,
    val operationType: String,
    val hierarchySummary: String,
    val keyStationsActive: List<String>,
    val operationalTradeOff: String
)

data class ItalianFoundationRuleSpec(
    val id: String,
    val pillarTitle: String,
    val italianTerm: String,
    val technicalPrinciple: String,
    val benchErrorToAvoid: String
)

data class BrazilRegionMatrixSpec(
    val regionName: String,
    val signatureIngredients: String,
    val derivedProducts: String,
    val emblemRecipes: String,
    val technicalControlNote: String
)

data class WorldAromaticBaseSpec(
    val id: String,
    val name: String,
    val originSchool: String,
    val proportionAndIngredients: String,
    val fatAndThermalMethod: String
)

data class WorldSpiceBlendSpec(
    val id: String,
    val name: String,
    val region: String,
    val coreSpices: String,
    val culinaryApplication: String
)
