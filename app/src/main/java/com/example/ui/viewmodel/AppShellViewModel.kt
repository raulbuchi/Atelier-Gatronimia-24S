package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.UserMessages
import com.example.core.timers.TimerCoordinator
import com.example.data.local.LocalKitchenRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado do contêiner principal: avisos (snackbar) e permissões/reagendamento dos timers. */
class AppShellViewModel(
    localRepo: LocalKitchenRepository,
    private val timerCoordinator: TimerCoordinator,
    messages: UserMessages
) : ViewModel() {

    val userMessages: SharedFlow<String> = messages.messages

    /** Usado para pedir permissões de notificação/alarme exato só quando há timer ativo. */
    val anyTimerRunning: StateFlow<Boolean> =
        combine(localRepo.allTimers, localRepo.kitchenTimers) { study, kitchen ->
            study.any { it.isRunning } || kitchen.any { it.isRunning }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private var syncJob: Job? = null

    init {
        // Resolve prazos vencidos com o app fechado e reagenda alarmes na abertura.
        refreshTimers()
    }

    /** Reagenda alarmes (ex.: após o usuário conceder alarmes exatos ou notificações). */
    fun refreshTimers() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch { timerCoordinator.sync() }
    }

    fun canScheduleExactTimerAlarms(): Boolean = timerCoordinator.canScheduleExactAlarms()
}
