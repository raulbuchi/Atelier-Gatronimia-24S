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
    val brochureId: String
)

data class IngredientSpec(
    val name: String,
    val netAmountPl: Double,
    val unit: String,
    val correctionFactorFc: Double = 1.0,
    val sector: String // Hortifrúti, Açougue & Peixaria, Laticínios & Ovos, Mercearia Seca, Especiarias & Fundos
)

enum class SmartKitchenAlertType {
    NONE,
    NON_STICK_VITAMIN_C,    // Proibição de aço puro p/ vegetais (preservar vitamina C)
    DEEP_FRY_ACRYLAMIDE,    // 140 °C pré-fritura / 165-175 °C teto rígido contra acrilamida
    POELIEREN_MATIGNON,     // 140-160 °C tampado sobre matignon sem líquido adicionado
    SOUS_VIDE_PROTOCOL,     // Salmoura 5-10% + banho +0.5 a +1.0 °C acima do centro térmico
    GALANTINE_PCC_65C,      // Centro térmico de 65 °C na galantina + farce sobre gelo
    PUFF_PASTRY_OXIDATION,  // Alerta contra repouso longo a 5 °C (acinzentamento/acidificação)
    PARALLEL_POACHING_82C   // Controle paralelo Berner Platte / Bollito misto (~82 °C)
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
    val steps: List<CookingStepSpec>
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
    val category: String, // "Bateria Térmica" | "Lâminas & Corte" | "Confeitaria & Precisão"
    val tierLevel: Int,   // 1 (Essencial) a 4 (Chefkoch Profissional)
    val kitchenRule: String
)

data class SwissBeefPrimalSpec(
    val code: String, // A..H
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
    val keyBulletRules: List<String>
)

data class FlashcardSpec(
    val id: String,
    val weekNumber: Int,
    val category: String,
    val frontQuestion: String,
    val backAnswer: String
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
