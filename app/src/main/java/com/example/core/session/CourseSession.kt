package com.example.core.session

import com.example.data.local.LocalKitchenRepository
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Dia local (fuso do aparelho) em epochDay — compatível com minSdk 24 sem java.time. */
fun localEpochDayNow(): Long {
    val now = System.currentTimeMillis()
    return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
}

/**
 * Semana em foco compartilhada por todas as abas (Curso, Aula, Receitas, Compras, Gestão),
 * persistida em `course_progress.focusedWeekNumber` e restaurada ao abrir o app.
 */
class CourseSession(
    private val repository: LocalKitchenRepository,
    private val scope: CoroutineScope
) {
    private val _selectedWeek = MutableStateFlow(1)
    val selectedWeek: StateFlow<Int> = _selectedWeek.asStateFlow()

    /** Evita que a restauração assíncrona sobrescreva uma semana escolhida antes do carregamento. */
    @Volatile
    private var pickedBeforeRestore = false

    init {
        scope.launch {
            val saved = repository.getOrCreateCourseProgress(localEpochDayNow())
            if (!pickedBeforeRestore) {
                _selectedWeek.value = saved.focusedWeekNumber.coerceIn(1, TOTAL_WEEKS)
            }
        }
    }

    fun selectWeek(week: Int) {
        val coerced = week.coerceIn(1, TOTAL_WEEKS)
        pickedBeforeRestore = true
        _selectedWeek.value = coerced
        scope.launch { repository.updateFocusedWeek(coerced, localEpochDayNow()) }
    }

    companion object {
        const val TOTAL_WEEKS = 24
    }
}

/** Avisos curtos para o aluno (snackbar), emitidos por qualquer ViewModel ou pelo coordenador de timers. */
class UserMessages {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}
