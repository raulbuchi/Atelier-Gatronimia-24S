package com.example.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.core.timers.TimerCoordinator
import com.example.core.timers.body
import com.example.core.timers.title
import com.example.data.local.AtelierDatabase
import com.example.data.local.LocalKitchenRepository
import com.example.ui.viewmodel.AppShellViewModel
import com.example.ui.viewmodel.AssessmentViewModel
import com.example.ui.viewmodel.CourseViewModel
import com.example.ui.viewmodel.KitchenViewModel
import com.example.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contêiner de dependências manual (um por processo). Os ViewModels recebem daqui o repositório,
 * a sessão do curso, os avisos e o coordenador de timers, em vez de cada tela montar o grafo.
 */
class AppContainer private constructor(context: Context) {

    private val appContext = context.applicationContext

    /** Escopo do processo para trabalho que não pertence a uma tela (restauração da sessão, avisos). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val localRepository = LocalKitchenRepository(AtelierDatabase.getInstance(appContext).atelierDao())
    val timerCoordinator = TimerCoordinator.get(appContext)
    val courseSession = CourseSession(localRepository, appScope)
    val userMessages = UserMessages()

    /** Fábrica única dos ViewModels locais (um `initializer` por classe). */
    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { CourseViewModel(localRepository, courseSession, timerCoordinator, userMessages) }
        initializer { KitchenViewModel(localRepository, courseSession, timerCoordinator, userMessages) }
        initializer { PantryViewModel(localRepository, courseSession, userMessages) }
        initializer { AssessmentViewModel(localRepository, courseSession, userMessages) }
        initializer { AppShellViewModel(localRepository, timerCoordinator, userMessages) }
    }

    init {
        appScope.launch {
            timerCoordinator.transitions.collect { userMessages.show("${it.title()}: ${it.body()}") }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun from(context: Context): AppContainer = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppContainer(context).also { INSTANCE = it }
        }
    }
}
