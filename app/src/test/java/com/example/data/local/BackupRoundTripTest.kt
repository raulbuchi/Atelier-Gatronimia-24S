package com.example.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRoundTripTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var source: AtelierDatabase
    private lateinit var target: AtelierDatabase

    @Before
    fun setUp() {
        source = Room.inMemoryDatabaseBuilder(app, AtelierDatabase::class.java).allowMainThreadQueries().build()
        target = Room.inMemoryDatabaseBuilder(app, AtelierDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun backup_carriesStudyBlocksAndProgress_andReimportDoesNotDuplicateShopping() = runTest {
        val dao = source.atelierDao()
        val now = System.currentTimeMillis()
        // Semana 3 com bloco rodando (prazo em ~5 min) e 2 blocos já concluídos.
        dao.upsertTimer(
            StudyTimerEntity(
                weekNumber = 3, currentPhaseIndex = 2, remainingSeconds = 1800, isRunning = true,
                completedBlocksCount = 2, phaseEndsAtEpochMs = now + 300_000
            )
        )
        dao.upsertWeekCompletion(WeekCompletionEntity(weekNumber = 2, completedAtEpochMs = 42))
        dao.upsertCourseProgress(CourseProgressEntity(startEpochDay = 20_000, focusedWeekNumber = 3))
        dao.insertShoppingItems(
            listOf(
                ShoppingFifoEntity(
                    weekNumber = 3, ingredientName = "Batata", sector = "Hortifrúti", unit = "g",
                    netQuantityPl = 800.0, correctionFactorFc = 1.1, grossQuantityPb = 880.0
                )
            )
        )

        val json = LocalKitchenRepository(dao).exportBackupJson()
        val restoreRepo = LocalKitchenRepository(target.atelierDao())
        restoreRepo.importBackupJson(json).getOrThrow()
        restoreRepo.importBackupJson(json).getOrThrow() // restaurar duas vezes não pode duplicar

        val restoredTarget = target.atelierDao()
        val timer = restoredTarget.getTimerOnce(3)!!
        assertEquals(2, timer.completedBlocksCount)
        assertEquals(2, timer.currentPhaseIndex)
        assertFalse("restaurado pausado", timer.isRunning)
        assertEquals(0L, timer.phaseEndsAtEpochMs)
        assertEquals(300.0, timer.remainingSeconds.toDouble(), 2.0)

        assertEquals(1, restoredTarget.getAllShoppingItemsOnce().size)
        assertEquals(1, restoredTarget.getAllWeekCompletionsOnce().size)
        assertEquals(3, restoredTarget.getCourseProgressOnce()?.focusedWeekNumber)
    }
}
