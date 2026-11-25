package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.SelfEvalEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Autoavaliação (Selbstkontrolle), backup JSON e Dossier Lernreflexion. Tela: Gestão & Calculadoras. */
class AssessmentViewModel(
    private val localRepo: LocalKitchenRepository,
    private val session: CourseSession,
    private val messages: UserMessages
) : ViewModel() {

    val selectedWeekNumber: StateFlow<Int> = session.selectedWeek

    fun selectWeek(week: Int) = session.selectWeek(week)

    val selfEvaluationsMap: StateFlow<Map<Int, SelfEvalEntity>> = localRepo.selfEvaluations
        .map { list -> list.associateBy { it.weekNumber } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    fun saveSelfEvaluation(eval: SelfEvalEntity) {
        viewModelScope.launch {
            localRepo.upsertSelfEvaluation(eval.copy(updatedAtEpochMs = System.currentTimeMillis()))
            messages.show("Autoavaliação de Bancada da Semana ${eval.weekNumber} registrada.")
        }
    }

    suspend fun buildBackupJson(): String = localRepo.exportBackupJson()

    fun restoreFromBackupJson(json: String) {
        viewModelScope.launch {
            localRepo.importBackupJson(json)
                .onSuccess { count -> messages.show("Backup JSON restaurado com sucesso ($count registros).") }
                .onFailure { err -> messages.show("Erro ao importar JSON: ${err.localizedMessage}") }
        }
    }

    suspend fun buildLernreflexionMarkdown(): String = localRepo.exportDossierLernreflexionMarkdown()
}
