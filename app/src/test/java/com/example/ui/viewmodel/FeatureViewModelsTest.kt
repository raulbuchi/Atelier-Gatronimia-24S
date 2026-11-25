package com.example.ui.viewmodel

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.core.timers.TimerAlarmScheduler
import com.example.core.timers.TimerCoordinator
import com.example.data.curriculum.CourseContent
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.local.AtelierDatabase
import com.example.data.local.CourseProgressEntity
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.SelfEvalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Integração entre os ViewModels por funcionalidade via sessão do curso e canal de avisos compartilhados. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureViewModelsTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: AtelierDatabase
    private lateinit var repo: LocalKitchenRepository
    private val messages = UserMessages()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CourseContent.install(app)
        db = Room.inMemoryDatabaseBuilder(app, AtelierDatabase::class.java).allowMainThreadQueries().build()
        repo = LocalKitchenRepository(db.atelierDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun coordinator() = TimerCoordinator(app, db.atelierDao(), TimerAlarmScheduler(app))

    /** O Room responde em threads reais; espera em tempo real (o runTest usa tempo virtual). */
    private suspend fun <T> awaitReal(block: suspend () -> T): T =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(5_000) { block() } }

    @Test
    fun session_restoresSavedFocusedWeek() = runTest {
        db.atelierDao().upsertCourseProgress(CourseProgressEntity(startEpochDay = 20_000, focusedWeekNumber = 7))
        val session = CourseSession(repo, backgroundScope)
        assertEquals(7, awaitReal { session.selectedWeek.first { it == 7 } })
    }

    @Test
    fun kitchen_followsWeekSelectedInAnotherTab_andWeekIsPersisted() = runTest {
        val session = CourseSession(repo, backgroundScope)
        val course = CourseViewModel(repo, session, coordinator(), messages)
        val kitchen = KitchenViewModel(repo, session, coordinator(), messages)

        course.selectWeek(8)

        val expected = SwissmilkAndBaseRecipesCatalog.recipesForWeek(8).first().id
        assertEquals(expected, awaitReal { kitchen.selectedRecipeId.first { it == expected } })
        assertEquals(8, kitchen.selectedWeekNumber.value)
        val saved = awaitReal { repo.courseProgress.first { it?.focusedWeekNumber == 8 } }
        assertEquals(8, saved?.focusedWeekNumber)
    }

    @Test
    fun messagesFromDifferentFeatures_reachTheSharedChannel() = runTest {
        val received = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { messages.messages.toList(received) }
        val session = CourseSession(repo, backgroundScope)
        val course = CourseViewModel(repo, session, coordinator(), messages)
        val assessment = AssessmentViewModel(repo, session, messages)

        course.completeWeek(1) // critérios não atendidos → aviso imediato
        assessment.saveSelfEvaluation(SelfEvalEntity(weekNumber = 1))
        awaitReal { repo.selfEvaluations.first { it.isNotEmpty() } }
        awaitReal { while (received.size < 2) kotlinx.coroutines.delay(10) }

        assertTrue(received[0].contains("Conclua os 4 critérios da Semana 1"))
        assertTrue(received[1].contains("Autoavaliação de Bancada da Semana 1 registrada"))
    }
}
