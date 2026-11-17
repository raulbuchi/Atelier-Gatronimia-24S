package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.media.MediaCompressor
import com.example.data.local.AtelierDatabase
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.MepIngredientLineEntity
import com.example.data.local.MepSheetEntity
import com.example.domain.calculators.CulinaryCalculators
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context matches Atelier Gastronomia 24S`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atelier Gastronomia 24S", appName)
    }

    @Test
    fun `fachrechnen cascade losses and sous-vide brine math are accurate`() {
        val cascade = CulinaryCalculators.calculateCascadeLosses(180.0, listOf(20.0, 15.0))
        assertEquals(0.68, cascade.combinedYieldFactor, 0.001)
        assertEquals(264.71, cascade.grossPurchaseGrams, 0.05)

        val brine = CulinaryCalculators.calculateSousVideBrine(
            waterLiters = 2.0,
            brineConcentrationPercent = 7.0,
            isDelicateFish = true,
            targetCoreTempC = 55.0,
            bathOffsetC = 0.8
        )
        assertEquals(140.0, brine.saltGramsRequired, 0.01)
        assertEquals(10, brine.recommendedBrineMinutes)
        assertEquals(55.8, brine.recommendedBathTempC, 0.01)

        val fullPuff = CulinaryCalculators.simulateSwissPuffPastry(1000.0, isHalfPuffHalbblaetterteig = false)
        val halfPuff = CulinaryCalculators.simulateSwissPuffPastry(1000.0, isHalfPuffHalbblaetterteig = true)
        assertEquals(4, fullPuff.recommendedDoubleFolds)
        assertEquals(256, fullPuff.theoreticalFatLayers)
        assertEquals(3, halfPuff.recommendedDoubleFolds)
        assertEquals(64, halfPuff.theoreticalFatLayers)
        assertTrue(fullPuff.storageWarning.contains("5 °C"))
    }

    @Test
    fun `mep 8-block calculator summary and Room persistence round-trip work accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AtelierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalKitchenRepository(db.atelierDao())

        try {
            val lines = listOf(
                MepIngredientLineEntity(
                    recipeId = "custom_mep_test",
                    sortOrder = 0,
                    name = "Noz de Vitela",
                    sector = "Açougue & Peixaria",
                    unit = "g",
                    baseNetPl = 600.0,
                    trimLossPercent = 20.0, // FC = 1 / 0.80 = 1.25
                    correctionFactorFc = 1.25,
                    unitCostPerKgOrL = 40.0
                ),
                MepIngredientLineEntity(
                    recipeId = "custom_mep_test",
                    sortOrder = 1,
                    name = "Batata Gschwellti",
                    sector = "Hortifrúti",
                    unit = "g",
                    baseNetPl = 800.0,
                    trimLossPercent = 0.0,
                    correctionFactorFc = 1.0,
                    unitCostPerKgOrL = 5.0
                )
            )

            // Base 4 -> Target 8 servings (scale 2.0x)
            // Vitela: PL 1200g, FC 1.25 -> PB 1500g (1.5 kg * 40 = 60 CHF)
            // Batata: PL 1600g, FC 1.00 -> PB 1600g (1.6 kg * 5 = 8 CHF)
            // Total PL = 2800g, Total PB = 3100g, Shrinkage 10% -> Cooked Yield = 2520g
            val summary = CulinaryCalculators.calculateMepSheetSummary(
                baseServings = 4,
                targetServings = 8,
                cookingShrinkagePercent = 10.0,
                targetCmvPercent = 25.0,
                directLaborPerPortionChf = 8.0,
                actualBenchYieldGrams = 2469.6, // -2% deviation
                ingredientLines = lines
            )

            assertEquals(2.0, summary.scaleFactor, 0.001)
            assertEquals(2800.0, summary.totalScaledPlGramsEquiv, 0.01)
            assertEquals(3100.0, summary.totalScaledPbGramsEquiv, 0.01)
            assertEquals(2520.0, summary.theoreticalCookedYieldGrams, 0.01)
            assertEquals(68.0, summary.totalBatchFoodCostChf, 0.01)
            assertEquals(8.5, summary.foodCostPerPortionChf, 0.01)
            assertNotNull(summary.benchYieldDeviationPercent)
            assertEquals(-2.0, summary.benchYieldDeviationPercent ?: 0.0, 0.1)

            val sheet = MepSheetEntity(
                recipeId = "custom_mep_test",
                title = "Émincé de Vitela de Bancada",
                weekNumber = 7,
                stationBrigade = "Saucier",
                baseServings = 4,
                targetServings = 8,
                portionWeightGrams = summary.theoreticalPortionCookedGrams,
                cookingShrinkagePercent = 10.0,
                targetCmvPercent = 25.0,
                directLaborCost = 8.0,
                pccTargetTempC = 68,
                plateTempC = 55,
                allergensCsv = "Lactose, Sulfitos",
                actualBenchYieldGrams = 2469.6,
                executionScore1To5 = 5,
                isCustomSheet = true,
                block1Identification = "Bloco 1 teste",
                block2IngredientsPbPlFc = "Bloco 2 teste",
                block3CookwareBlades = "Sautoir; Poêle antiadhésif",
                block4PrePrepCuts = "Brunoise (~1,5 mm)",
                block5ThermalFlowPcc = "PCC >= 68 °C",
                block6PlatingFinishing = "Passe 55 °C",
                block7HygieneAllergens = "Cadeia fria <= 5 °C",
                block8Selbstkontrolle = "Execução limpa",
                localPhotoUri = "content://com.aistudio.ateliergastronomia.v4kxmp.fileprovider/mep_creations/emince.jpg",
                cloudImageLink = "firestore://portfolio_media/mep_media_unit_01",
                cloudMediaId = "mep_media_unit_01",
                photoThumbnailBase64 = "dGh1bWI="
            )

            repo.saveMepComplete(sheet, lines)

            val savedSheets = repo.mepSheets.first()
            val savedLines = repo.mepLines.first()
            assertEquals(1, savedSheets.size)
            assertEquals("Émincé de Vitela de Bancada", savedSheets.first().title)
            assertEquals(8, savedSheets.first().targetServings)
            assertEquals("firestore://portfolio_media/mep_media_unit_01", savedSheets.first().cloudImageLink)
            assertEquals(2, savedLines.size)
            assertEquals("Noz de Vitela", savedLines[0].name)

            val markdown = LocalKitchenRepository.formatSingleMepSheetMarkdown(savedSheets.first(), summary)
            assertTrue(markdown.contains("FICHA TÉCNICA MEP (8 BLOCOS)"))
            assertTrue(markdown.contains("Noz de Vitela"))
            assertTrue(markdown.contains("firestore://portfolio_media/mep_media_unit_01"))

            repo.deleteMepComplete("custom_mep_test")
            assertTrue(repo.mepSheets.first().isEmpty())
            assertTrue(repo.mepLines.first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `camerax mep creation file provider uri and compression pipeline work end to end`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (photoFile, contentUri) = MediaCompressor.createMepCreationPhotoFile(
            context = context,
            recipeId = "zurcher_geschnetzeltes_rosti"
        )
        assertTrue(contentUri.toString().contains("mep_creations"))

        val syntheticDish = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(174, 33, 39))
        }
        FileOutputStream(photoFile).use { out ->
            syntheticDish.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        val compressed = MediaCompressor.compressFromUri(context, contentUri).getOrThrow()
        assertEquals(1200, compressed.width)
        assertEquals(900, compressed.height)
        assertTrue(compressed.fullBase64.isNotBlank())
        assertTrue(compressed.thumbnailBase64.isNotBlank())
        assertEquals(contentUri.toString(), compressed.localUriString)
        photoFile.delete()
    }

    @Test
    fun `canonical v4 json dataset and v5 cultural atlas and hydrocolloid calculator match spec`() {
        val allRecipes = com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.allRecipes
        assertEquals(45, allRecipes.size)
        assertEquals(23, allRecipes.count { it.isSwissmilkClassic })
        assertEquals(10, allRecipes.count { it.isChefkochV4 })
        assertEquals(12, allRecipes.count { it.recipeTrack == "base_global" })

        assertNotNull(com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.findById("rec-zurcher-geschnetzeltes"))
        assertNotNull(com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.findById("rec-brauner-kalbsfond"))
        assertNotNull(com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.findById("rec-kabeljau-sv"))

        val week8Recipes = com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.recipesForWeek(8).map { it.id }
        assertTrue("rec-zurcher-geschnetzeltes" in week8Recipes)
        assertTrue("rec-rosti-gschwellti" in week8Recipes)

        val cantons = com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog.availableCantons
        assertEquals("AG", cantons.first())
        assertEquals("ZH", cantons.last())
        assertTrue("BE" in cantons)

        assertEquals(60, com.example.data.curriculum.TechnicalAtlasCatalog.flashcards.size)
        assertTrue(com.example.data.curriculum.TechnicalAtlasCatalog.flashcards.any { it.id == "fc-e1-1" })
        assertTrue(com.example.data.curriculum.TechnicalAtlasCatalog.flashcards.any { it.id == "fc-e6-10" })

        assertEquals(4, com.example.data.curriculum.TechnicalAtlasCatalog.equipmentTiers.size)
        assertEquals(37, com.example.data.curriculum.TechnicalAtlasCatalog.allEquipmentTierItems.size)
        assertEquals(10, com.example.data.curriculum.TechnicalAtlasCatalog.fiberEquivalentsFor10g.size)
        assertEquals(6, com.example.data.curriculum.TechnicalAtlasCatalog.sousVideValidatedTargets.size)
        assertEquals(4, com.example.data.curriculum.TechnicalAtlasCatalog.sugarStagesLcb.size)

        val julienne = com.example.data.curriculum.TechnicalAtlasCatalog.swissCuts.first { it.name == "Julienne" }
        assertTrue(julienne.dimensionSwiss.contains("George Bento"))

        val brochures = com.example.data.curriculum.CurriculumWeeksAndGuides.brochures
        assertEquals(6, brochures.size)
        assertTrue(brochures.all { it.fileNameRef.endsWith("-v3.pdf") })
        assertEquals(24, com.example.data.curriculum.CurriculumWeeksAndGuides.weeks.size)
        assertTrue(com.example.data.curriculum.CurriculumWeeksAndGuides.weeks.all { it.brochureId.endsWith("-v3.pdf") })
        assertTrue(com.example.data.curriculum.CurriculumWeeksAndGuides.blankMepTemplateMarkdown.contains("8 BLOCOS"))
        assertEquals("rec-zurcher-geschnetzeltes", com.example.data.curriculum.CurriculumWeeksAndGuides.exampleMepZurcherRosti.recipeId)

        assertEquals(4, com.example.data.curriculum.CulturalScientificAtlasV5.italianFoundationsHazan.size)
        assertEquals(5, com.example.data.curriculum.CulturalScientificAtlasV5.brazilFiveRegionsMatrices.size)
        assertEquals(7, com.example.data.curriculum.CulturalScientificAtlasV5.worldSevenAromaticBases.size)
        assertEquals(9, com.example.data.curriculum.CulturalScientificAtlasV5.worldNineSpiceBlends.size)

        assertTrue(com.example.data.ai.ChefAiRepository.SYSTEM_INSTRUCTIONS_V5.contains("Canella-Rawls"))
        assertTrue(com.example.data.ai.ChefAiRepository.SYSTEM_INSTRUCTIONS_V5.contains("James Peterson"))

        val hydro = CulinaryCalculators.calculateGelatinAndHydrocolloid(
            gelatinInputAmount = 12.0,
            inputIsSheets = false,
            liquidBaseGrams = 500.0,
            agent = com.example.domain.calculators.HydrocolloidAgent.XANTHAN
        )
        assertEquals(6.0, hydro.equivalentSheets, 0.001)
        assertEquals(1.0, hydro.equivalentEnvelopes12g, 0.001)
        assertEquals(6.0, hydro.equivalentTeaspoonsCc, 0.001)
        assertEquals(60.0, hydro.hydrationWaterGrams, 0.001)
        assertEquals(0.5, hydro.minDosageGrams, 0.001)
        assertEquals(2.5, hydro.maxDosageGrams, 0.001)
    }

    @Test
    fun `weekly mep cost evolution aggregates multiple sheets per week and computes delta accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AtelierDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalKitchenRepository(db.atelierDao())

        try {
            // Semana 2: 1 ficha (PL 1000g * FC 1.0 = 1kg * 20 CHF = 20 CHF; Mão de obra 4 pax * 5 CHF = 20 CHF; Prime = 40 CHF)
            val sheetW2 = MepSheetEntity(
                recipeId = "mep_w2_a",
                title = "Fundo Claro S02",
                weekNumber = 2,
                baseServings = 4,
                targetServings = 4,
                directLaborCost = 5.0
            )
            val linesW2 = listOf(
                MepIngredientLineEntity(
                    recipeId = "mep_w2_a",
                    sortOrder = 0,
                    name = "Ossos de Vitela",
                    sector = "Açougue & Peixaria",
                    unit = "g",
                    baseNetPl = 1000.0,
                    trimLossPercent = 0.0,
                    correctionFactorFc = 1.0,
                    unitCostPerKgOrL = 20.0
                )
            )

            // Semana 8: 2 fichas (Ficha B: 30 CHF CMV + 24 CHF Labor = 54 CHF; Ficha C: 20 CHF CMV + 16 CHF Labor = 36 CHF)
            // Total Semana 8: CMV = 50 CHF, Labor = 40 CHF, Prime = 90 CHF; Delta CMV S02->S08 = (50 - 20)/20 = +150%
            val sheetW8B = MepSheetEntity(
                recipeId = "mep_w8_b",
                title = "Émincé S08",
                weekNumber = 8,
                baseServings = 4,
                targetServings = 4,
                directLaborCost = 6.0
            )
            val linesW8B = listOf(
                MepIngredientLineEntity(
                    recipeId = "mep_w8_b",
                    sortOrder = 0,
                    name = "Noz de Vitela",
                    sector = "Açougue & Peixaria",
                    unit = "g",
                    baseNetPl = 500.0,
                    trimLossPercent = 0.0,
                    correctionFactorFc = 1.2, // PB = 600g -> 0.6kg * 50 CHF = 30 CHF
                    unitCostPerKgOrL = 50.0
                )
            )
            val sheetW8C = MepSheetEntity(
                recipeId = "mep_w8_c",
                title = "Rösti S08",
                weekNumber = 8,
                baseServings = 4,
                targetServings = 4,
                directLaborCost = 4.0
            )
            val linesW8C = listOf(
                MepIngredientLineEntity(
                    recipeId = "mep_w8_c",
                    sortOrder = 0,
                    name = "Batata Gschwellti",
                    sector = "Hortifrúti",
                    unit = "kg",
                    baseNetPl = 2.0,
                    trimLossPercent = 0.0,
                    correctionFactorFc = 1.0, // PB = 2kg * 10 CHF = 20 CHF
                    unitCostPerKgOrL = 10.0
                )
            )

            repo.saveMepComplete(sheetW2, linesW2)
            repo.saveMepComplete(sheetW8B, linesW8B)
            repo.saveMepComplete(sheetW8C, linesW8C)

            val savedSheets = repo.mepSheets.first()
            val savedLinesMap = repo.mepLines.first().groupBy { it.recipeId }

            val analytics = com.example.domain.calculators.MepFinancialAnalyticsEngine.computeWeeklyEvolution(
                persistedSheets = savedSheets,
                linesByRecipeId = savedLinesMap,
                fallbackLinesProvider = { emptyList() }
            )

            assertEquals(false, analytics.isUsingReferenceBaseline)
            assertEquals(3, analytics.totalSheetsCount)
            assertEquals(24, analytics.all24WeekPoints.size)
            assertEquals(2, analytics.activeWeekPoints.size)

            val w2 = analytics.activeWeekPoints[0]
            assertEquals(2, w2.weekNumber)
            assertEquals(1, w2.sheetsCount)
            assertEquals(20.0, w2.totalBatchFoodCostChf, 0.01)
            assertEquals(40.0, w2.totalPrimeCostChf, 0.01)

            val w8 = analytics.activeWeekPoints[1]
            assertEquals(8, w8.weekNumber)
            assertEquals(2, w8.sheetsCount)
            assertEquals(50.0, w8.totalBatchFoodCostChf, 0.01)
            assertEquals(90.0, w8.totalPrimeCostChf, 0.01)
            assertEquals(70.0, w8.cumulativeFoodCostChf, 0.01)
            assertEquals(130.0, w8.cumulativePrimeCostChf, 0.01)

            assertEquals(70.0, analytics.grandTotalFoodCostChf, 0.01)
            assertEquals(130.0, analytics.grandTotalPrimeCostChf, 0.01)
            assertEquals(35.0, analytics.averageWeeklyFoodCostChf, 0.01)
            assertEquals(8, analytics.peakWeek?.weekNumber)
            assertEquals(150.0, analytics.latestDeltaPercent ?: 0.0, 0.1)
        } finally {
            db.close()
        }
    }

    @Test
    fun `mural comunitario models preserve boolean PropertyName annotations and localized strings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Mural Comunitário da Brigada", context.getString(R.string.mural_comunitario_title))
        assertEquals("Curtir", context.getString(R.string.mural_like_action))
        assertEquals("Curtido", context.getString(R.string.mural_liked_action))

        val post = com.example.data.remote.PortfolioPost(
            id = "post_s08_zurcher",
            userId = "chef_uid_1",
            authorName = "Chef Aluno",
            visibility = "public",
            weekNumber = 8,
            cycleNumber = 2,
            axisId = 2,
            materialType = "prato_final",
            recipeId = "rec-zurcher-geschnetzeltes",
            recipeTitle = "Zürcher Geschnetzeltes",
            isSwissmilkClassic = true,
            isChefkochV4 = false,
            cantonCode = "ZH",
            caption = "Molho nappé brilhante.",
            reactedUserIds = listOf("chef_uid_2", "chef_uid_3"),
            reactionCount = 2,
            commentCount = 1
        )
        assertTrue(post.isSwissmilkClassic)
        assertEquals(2, post.reactionCount)
        assertEquals(1, post.commentCount)

        val getterSwissmilk = com.example.data.remote.PortfolioPost::class.java.getMethod("isSwissmilkClassic")
        val propAnnotation = getterSwissmilk.getAnnotation(com.google.firebase.firestore.PropertyName::class.java)
        assertNotNull(propAnnotation)
        assertEquals("isSwissmilkClassic", propAnnotation?.value)
    }

    @Test
    fun `native didactic library loader parses 6 axes 23 sections 60 flashcards and global search`() {
        val catalog = com.example.data.curriculum.DidacticLibraryLoader.catalog
        assertEquals(6, catalog.axes.size)
        assertEquals(23, catalog.axes.sumOf { it.sections.size })
        assertEquals(60, catalog.axes.sumOf { it.flashcards.size })

        val allBlocks = catalog.axes.flatMap { ax -> ax.sections.flatMap { it.blocks } }
        assertTrue(allBlocks.any { it is com.example.data.curriculum.DidacticBlock.Paragraph })
        assertTrue(allBlocks.any { it is com.example.data.curriculum.DidacticBlock.Bullets })
        assertTrue(allBlocks.any { it is com.example.data.curriculum.DidacticBlock.Table })
        assertTrue(allBlocks.any { it is com.example.data.curriculum.DidacticBlock.Callout })
        assertTrue(allBlocks.any { it is com.example.data.curriculum.DidacticBlock.MetricStrip })

        val acrylamideHits = com.example.data.curriculum.DidacticLibraryLoader.searchAcrossLibrary("Acrilamida")
        assertTrue(acrylamideHits.isNotEmpty())
        assertTrue(acrylamideHits.any { it.axisNumber == 1 })
        assertTrue(acrylamideHits.any { it.axisNumber == 4 })

        val amylaseHits = com.example.data.curriculum.DidacticLibraryLoader.searchAcrossLibrary("Alfa-amilase")
        assertTrue(amylaseHits.isNotEmpty())
        assertTrue(amylaseHits.any { it.axisNumber == 3 })
        assertTrue(amylaseHits.any { it.axisNumber == 4 })
    }

    @Test
    fun `mediaUpload parity composites transparent PNG onto white background and compresses 400px avatar`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val transparentBmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
        }
        val composited = MediaCompressor.compositeOnWhiteCanvas(transparentBmp)
        assertEquals(Color.WHITE, composited.getPixel(400, 400))

        val avatarFile = java.io.File(context.cacheDir, "chef_avatar.png")
        FileOutputStream(avatarFile).use { out ->
            transparentBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val avatarPayload = MediaCompressor.compressAvatarFromUri(
            context,
            android.net.Uri.fromFile(avatarFile)
        ).getOrThrow()
        assertEquals(400, avatarPayload.width)
        assertEquals(400, avatarPayload.height)
        assertTrue(avatarPayload.compressedSizeBytes in 1..38_000)
        assertEquals("512 KB", MediaCompressor.formatBytes(512L * 1024L))
        avatarFile.delete()
    }
}


