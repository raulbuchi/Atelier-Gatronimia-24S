package com.example.data.curriculum

import com.example.domain.model.RecipeSpec

/**
 * Catálogo oficial unificado alimentado diretamente por `curso-24-semanas-dados-v4.json`
 * (45 receitas = 23 clássicos Swissmilk AG..ZH + 10 fichas Chefkoch passo a passo + 12 receitas Base & Mundo).
 */
object SwissmilkAndBaseRecipesCatalog {

    val allRecipes: List<RecipeSpec>
        get() = CourseJsonDatasetLoader.dataset.recipes

    private val recipeByIdMap: Map<String, RecipeSpec> by lazy {
        allRecipes.associateBy { it.id }
    }

    fun findById(id: String): RecipeSpec? = recipeByIdMap[id]

    fun recipesForWeek(weekNumber: Int): List<RecipeSpec> {
        val week = CurriculumWeeksAndGuides.weeks.find { it.weekNumber == weekNumber }
        if (week != null && week.practicalRecipeIds.isNotEmpty()) {
            val exact = week.practicalRecipeIds.mapNotNull { recipeByIdMap[it] }
            if (exact.isNotEmpty()) return exact
        }
        return allRecipes.filter { weekNumber in it.weeksAppearing || it.weekNumber == weekNumber }
    }

    val availableCantons: List<String> by lazy {
        allRecipes
            .filter { it.isSwissmilkClassic && it.cantonCode.isNotBlank() }
            .map { it.cantonCode }
            .distinct()
            .sorted()
    }
}
