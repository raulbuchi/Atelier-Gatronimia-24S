package com.example.data.curriculum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integridade do `curso-24-semanas-dados-v4.json` e da biblioteca didática: garante que cada
 * referência usada pelas telas (receitas, flashcards, brochuras, equipamentos) existe de fato.
 * Roda no Robolectric porque o parser usa `org.json` do Android.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CourseDatasetIntegrityTest {

    private val data = CourseJsonDatasetLoader.dataset

    private fun <T> List<T>.duplicatesBy(key: (T) -> Any): List<Any> =
        groupBy(key).filterValues { it.size > 1 }.keys.toList()

    @Test
    fun schedule_has24ContiguousWeeksIn4CyclesOf6() {
        assertEquals((1..24).toList(), data.weeks.map { it.weekNumber }.sorted())
        data.weeks.forEach { w ->
            assertEquals("ciclo da S${w.weekNumber}", (w.weekNumber - 1) / 6 + 1, w.cycleNumber)
            assertTrue("título da S${w.weekNumber}", w.title.isNotBlank())
        }
        assertEquals(listOf(6, 6, 6, 6), data.weeks.groupBy { it.cycleNumber }.toSortedMap().values.map { it.size })
    }

    @Test
    fun everyWeekHasAtLeastOneRecipe_andReferencedRecipesExist() {
        val recipeIds = data.recipes.map { it.id }.toSet()
        val broken = data.weeks.flatMap { w ->
            w.practicalRecipeIds.filterNot { it in recipeIds }.map { "S${w.weekNumber} → $it" }
        }
        assertTrue("receitas inexistentes: $broken", broken.isEmpty())
        val empty = (1..24).filter { SwissmilkAndBaseRecipesCatalog.recipesForWeek(it).isEmpty() }
        assertTrue("semanas sem receita prática: $empty", empty.isEmpty())
    }

    @Test
    fun referencedFlashcardsAndBrochuresExist() {
        val cardIds = data.flashcards.map { it.id }.toSet()
        val brochureIds = data.brochures.map { it.id }.toSet()
        val brokenCards = data.weeks.flatMap { w ->
            w.flashcardIds.filterNot { it in cardIds }.map { "S${w.weekNumber} → $it" }
        }
        val brokenBrochures = data.weeks.flatMap { w ->
            w.supportBrochureIds.filterNot { it in brochureIds }.map { "S${w.weekNumber} → $it" }
        }
        assertTrue("flashcards inexistentes: $brokenCards", brokenCards.isEmpty())
        assertTrue("brochuras inexistentes: $brokenBrochures", brokenBrochures.isEmpty())
    }

    @Test
    fun idsAreUnique() {
        assertTrue("receitas duplicadas", data.recipes.duplicatesBy { it.id }.isEmpty())
        assertTrue("flashcards duplicados", data.flashcards.duplicatesBy { it.id }.isEmpty())
        assertTrue("equipamentos duplicados", data.allEquipmentItems.duplicatesBy { it.id }.isEmpty())
        assertTrue("brochuras duplicadas", data.brochures.duplicatesBy { it.id }.isEmpty())
    }

    @Test
    fun recipeCatalog_matchesPublishedComposition() {
        assertEquals(45, data.recipes.size)
        val swissmilk = data.recipes.filter { it.isSwissmilkClassic }
        assertEquals(23, swissmilk.size)
        assertTrue("todo clássico Swissmilk tem cantão", swissmilk.all { it.cantonCode.isNotBlank() })
        assertEquals(10, data.recipes.count { it.isChefkochV4 })
        assertEquals(60, data.flashcards.size)
        assertTrue(data.flashcards.all { it.weekNumber in 1..24 })
    }

    @Test
    fun recipesAreCookable() {
        data.recipes.forEach { r ->
            assertTrue("${r.id}: sem ingredientes", r.ingredients.isNotEmpty())
            assertTrue("${r.id}: sem passos", r.steps.isNotEmpty())
            assertTrue("${r.id}: porções base", r.baseServings > 0)
            assertTrue("${r.id}: semana", r.weekNumber in 1..24)
            r.ingredients.forEach { ing ->
                assertTrue("${r.id}/${ing.name}: quantidade", ing.netAmountPl > 0.0)
                assertTrue("${r.id}/${ing.name}: FC ≥ 1", ing.correctionFactorFc >= 1.0)
            }
            r.steps.forEach { s ->
                s.timerMinutes?.let { assertTrue("${r.id} passo ${s.stepNumber}: timer", it > 0) }
            }
        }
    }

    @Test
    fun requiredEquipmentExistsInInventory() {
        val equipmentIds = data.allEquipmentItems.map { it.id }.toSet()
        val broken = data.recipes.flatMap { r ->
            r.requiredEquipmentIds.filterNot { it in equipmentIds }.map { "${r.id} → $it" }
        }
        assertTrue("equipamentos inexistentes: $broken", broken.isEmpty())
    }

    @Test
    fun equipmentMapping_picksTheToolsEachClassicNeeds() {
        fun eq(id: String) = SwissmilkAndBaseRecipesCatalog.findById(id)!!.requiredEquipmentIds
        assertTrue(eq("rec-rosti-gschwellti").containsAll(listOf("eq-antihaft", "eq-rostiraffel")))
        assertTrue(eq("rec-fondue-moitie").contains("eq-caquelon"))
        assertTrue(eq("rec-kabeljau-sv").containsAll(listOf("eq-sous-vide", "eq-termometro")))
        assertTrue(eq("rec-blatterteig").contains("eq-rolo"))
        // Todas têm o kit mínimo de bancada.
        assertTrue(data.recipes.all { it.requiredEquipmentIds.containsAll(listOf("eq-balanca", "eq-faca-chef", "eq-tabuas")) })
    }

    @Test
    fun cycleDeliverablesArePresentForEveryCycle() {
        (1..4).forEach { c ->
            val deliverables = data.weeks.first { it.cycleNumber == c }.cycleDeliverables
            assertTrue("ciclo $c sem entregáveis", deliverables.isNotEmpty())
        }
    }

    @Test
    fun didacticLibrary_hasSixAxesWithValidWeekLinks() {
        val axes = DidacticLibraryLoader.catalog.axes
        assertEquals((1..6).toList(), axes.map { it.axisNumber }.sorted())
        axes.forEach { axis ->
            assertTrue("eixo ${axis.axisNumber} sem seções", axis.sections.isNotEmpty())
            axis.sections.forEach { s ->
                assertTrue("eixo ${axis.axisNumber}/${s.id}: semanas ${s.weeks}", s.weeks.all { it in 1..24 })
            }
        }
    }
}
