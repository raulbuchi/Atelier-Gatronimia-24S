package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Garante que nenhuma atualização do app apague o progresso do aluno.
 *
 * Os schemas vêm de `app/schemas/` (exportados pelo Room via KSP). O `3.json` foi reconstruído a
 * partir do `4.json` removendo as tabelas criadas na v4 (o histórico do git confirma que a v4
 * apenas adicionou tabelas); seu identityHash é um marcador, pois a v3 nunca exportou schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AtelierDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AtelierDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun seedV3(dbName: String) {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO sm2_progress (cardId, repetitions, easinessFactor, intervalDays, " +
                    "nextReviewEpochDay, lastGrade, updatedAtEpochMs) VALUES ('fc-e1-1', 3, 2.6, 6, 20500, 5, 1)"
            )
            execSQL(
                "INSERT INTO study_timer_state (weekNumber, currentPhaseIndex, remainingSeconds, isRunning, " +
                    "completedBlocksCount, updatedAtEpochMs) VALUES (1, 2, 1200, 0, 2, 1)"
            )
            execSQL(
                "INSERT INTO self_evaluations (weekNumber, scoreMiseEnPlace, scoreTechnique, scoreHygienePcc, " +
                    "scoreSensory, erkenntnisse, offenePunkte, updatedAtEpochMs) " +
                    "VALUES (1, 5, 4, 5, 4, 'Bancada organizada', 'Afiar faca', 1)"
            )
            execSQL(
                "INSERT INTO mep_sheets (recipeId, title, weekNumber, stationBrigade, baseServings, targetServings, " +
                    "portionWeightGrams, cookingShrinkagePercent, targetCmvPercent, directLaborCost, pccTargetTempC, " +
                    "plateTempC, allergensCsv, actualBenchYieldGrams, executionScore1To5, isCustomSheet, " +
                    "block1Identification, block2IngredientsPbPlFc, block3CookwareBlades, block4PrePrepCuts, " +
                    "block5ThermalFlowPcc, block6PlatingFinishing, block7HygieneAllergens, block8Selbstkontrolle, " +
                    "localPhotoUri, cloudImageLink, cloudMediaId, photoThumbnailBase64, updatedAtEpochMs) VALUES " +
                    "('rec-smoothie-cafe', 'Smoothie', 1, 'Garde Manger', 4, 6, 220.0, 0.0, 28.0, 8.0, 70, 5, '', " +
                    "0.0, 4, 0, 'b1', 'b2', 'b3', 'b4', 'b5', 'b6', 'b7', 'b8', '', '', '', '', 1)"
            )
            execSQL(
                "INSERT INTO mep_ingredient_lines (recipeId, sortOrder, name, sector, unit, baseNetPl, " +
                    "trimLossPercent, correctionFactorFc, unitCostPerKgOrL) " +
                    "VALUES ('rec-smoothie-cafe', 0, 'Banana', 'Hortifrúti', 'g', 300.0, 30.0, 1.43, 8.5)"
            )
            close()
        }
    }

    @Test
    fun migrate3To4_createsProgressTablesAndKeepsStudentData() {
        val dbName = "migration-3-4.db"
        seedV3(dbName)

        val db = helper.runMigrationsAndValidate(dbName, 4, true, AtelierDatabase.MIGRATION_3_4)

        db.query("SELECT lastGrade FROM sm2_progress WHERE cardId = 'fc-e1-1'").use {
            assertTrue(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM mep_ingredient_lines").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        listOf("course_progress", "week_completions", "cycle_deliverable_checks").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                it.moveToFirst()
                assertEquals("$table deveria iniciar vazia", 0, it.getInt(0))
            }
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsDeadlineColumnAndKitchenTimers_andPausesLegacyRunningBlocks() {
        val dbName = "migration-4-5.db"
        helper.createDatabase(dbName, 4).apply {
            execSQL(
                "INSERT INTO study_timer_state (weekNumber, currentPhaseIndex, remainingSeconds, isRunning, " +
                    "completedBlocksCount, updatedAtEpochMs) VALUES (7, 1, 900, 1, 3, 1)"
            )
            execSQL("INSERT INTO week_completions (weekNumber, completedAtEpochMs) VALUES (6, 42)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, AtelierDatabase.MIGRATION_4_5)

        db.query(
            "SELECT isRunning, remainingSeconds, phaseEndsAtEpochMs, completedBlocksCount, currentPhaseIndex " +
                "FROM study_timer_state WHERE weekNumber = 7"
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals("bloco legado sem prazo deve ser pausado", 0, it.getInt(0))
            assertEquals(900, it.getInt(1))
            assertEquals(0L, it.getLong(2))
            assertEquals(3, it.getInt(3))
            assertEquals(1, it.getInt(4))
        }
        db.query("SELECT COUNT(*) FROM week_completions").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM kitchen_timers").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        db.close()
    }

    /** Abre com o Room real (código gerado + todas as migrações) a partir da v3 mais antiga suportada. */
    @Test
    fun openFromV3WithAllMigrations_roomReadsPreservedDataAndNewTablesWork() = runTest {
        val dbName = "migration-all.db"
        seedV3(dbName)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val room = Room.databaseBuilder(context, AtelierDatabase::class.java, dbName)
            .addMigrations(*AtelierDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = room.atelierDao()
            assertEquals(ATELIER_DB_VERSION, room.openHelper.readableDatabase.version)

            assertEquals(1, dao.getAllSm2Once().size)
            assertEquals(2, dao.getAllTimersOnce().single().completedBlocksCount)
            assertEquals("Bancada organizada", dao.getAllSelfEvaluationsOnce().single().erkenntnisse)
            assertEquals(6, dao.getAllMepSheetsOnce().single().targetServings)
            assertEquals("Banana", dao.getMepLinesForSheetOnce("rec-smoothie-cafe").single().name)

            assertNull(dao.getCourseProgressOnce())
            dao.upsertCourseProgress(CourseProgressEntity(startEpochDay = 20500, focusedWeekNumber = 3))
            dao.upsertWeekCompletion(WeekCompletionEntity(weekNumber = 1, completedAtEpochMs = 42))
            dao.upsertCycleDeliverableCheck(CycleDeliverableCheckEntity(1, 0, isDone = true))
            assertEquals(3, dao.getCourseProgressOnce()?.focusedWeekNumber)
            assertEquals(1, dao.getAllWeekCompletionsOnce().size)
            assertEquals(1, dao.getAllCycleDeliverableChecksOnce().size)
            assertEquals(0L, dao.getAllTimersOnce().single().phaseEndsAtEpochMs)
            assertTrue(dao.getAllKitchenTimersOnce().isEmpty())
        } finally {
            room.close()
        }
    }
}
