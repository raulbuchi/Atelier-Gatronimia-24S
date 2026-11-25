package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.progress.CourseProgressSnapshot
import com.example.domain.progress.CycleProgress
import com.example.domain.progress.WeekProgress
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private fun formatEpochDay(epochDay: Long): String =
    SimpleDateFormat("dd/MM/yyyy", PT_BR)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochDay * MILLIS_PER_DAY))

private fun formatEpochMs(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", PT_BR).format(Date(epochMs))

/**
 * Painel de progressão do aluno: avanço geral (x/24), semana do calendário, checklist dos
 * 4 critérios da semana em foco, conclusão formal e entregáveis de portfólio do ciclo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseProgressCard(
    snapshot: CourseProgressSnapshot,
    focusedWeekNumber: Int,
    onSelectWeek: (Int) -> Unit,
    onChangeStartDate: (Long) -> Unit,
    onCompleteWeek: (Int) -> Unit,
    onReopenWeek: (Int) -> Unit,
    onOpenMepSheet: (() -> Unit)?,
    onReviewFlashcards: () -> Unit,
    onOpenSelfEvaluation: () -> Unit,
    onToggleDeliverable: (cycleNumber: Int, index: Int, isDone: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val week = snapshot.week(focusedWeekNumber)
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("course_progress_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. Avanço geral
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MEU PROGRESSO NO CURSO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${snapshot.completedWeeksCount} / ${snapshot.totalWeeks.coerceAtLeast(24)} semanas",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("course_progress_completed_count")
                )
            }
            LinearProgressIndicator(
                progress = { snapshot.overallFraction },
                color = PccGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            // 2. Calendário
            snapshot.startEpochDay?.let { start ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SwissRed)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Início: ${formatEpochDay(start)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        snapshot.calendarWeek?.let { cw ->
                            Text(
                                text = "Pelo calendário você estaria na semana S${"%02d".format(cw)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("course_change_start_date")
                    ) { Text("Alterar") }
                }
            }

            // Atalhos de navegação de semana
            val calendarWeek = snapshot.calendarWeek
            val nextPending = snapshot.nextPendingWeek
            if ((calendarWeek != null && calendarWeek != focusedWeekNumber) ||
                (nextPending != null && nextPending != focusedWeekNumber)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nextPending != null && nextPending != focusedWeekNumber) {
                        OutlinedButton(
                            onClick = { onSelectWeek(nextPending) },
                            modifier = Modifier.testTag("course_go_next_pending")
                        ) { Text("Próxima pendente: S${"%02d".format(nextPending)}") }
                    }
                    if (calendarWeek != null && calendarWeek != focusedWeekNumber && calendarWeek != nextPending) {
                        OutlinedButton(
                            onClick = { onSelectWeek(calendarWeek) },
                            modifier = Modifier.testTag("course_go_calendar_week")
                        ) { Text("Ir para S${"%02d".format(calendarWeek)}") }
                    }
                }
            }

            HorizontalDivider()

            // 3. Checklist da semana em foco
            if (week != null) {
                WeekChecklist(
                    week = week,
                    onOpenMepSheet = onOpenMepSheet,
                    onReviewFlashcards = onReviewFlashcards,
                    onOpenSelfEvaluation = onOpenSelfEvaluation
                )
                WeekCompletionActions(
                    week = week,
                    totalWeeks = snapshot.totalWeeks,
                    onCompleteWeek = onCompleteWeek,
                    onReopenWeek = onReopenWeek,
                    onSelectWeek = onSelectWeek
                )
            }

            // 4. Entregáveis do ciclo
            val cycle = week?.let { snapshot.cycle(it.cycleNumber) }
            if (cycle != null && cycle.deliverables.isNotEmpty()) {
                HorizontalDivider()
                CycleDeliverables(cycle = cycle, onToggleDeliverable = onToggleDeliverable)
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = (snapshot.startEpochDay ?: 0L) * MILLIS_PER_DAY
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onChangeStartDate(it / MILLIS_PER_DAY) }
                        showDatePicker = false
                    }
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun WeekChecklist(
    week: WeekProgress,
    onOpenMepSheet: (() -> Unit)?,
    onReviewFlashcards: () -> Unit,
    onOpenSelfEvaluation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "CRITÉRIOS DA SEMANA S${"%02d".format(week.weekNumber)} " +
                "(${week.criteriaMetCount}/${WeekProgress.CRITERIA_COUNT})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CriterionRow(
            done = week.studyBlockDone,
            title = "Bloco de estudo de 80 min",
            detail = "Use o cronômetro do bloco de estudo abaixo",
            tag = "criterion_study_block"
        )
        CriterionRow(
            done = week.mepSheetDone,
            title = "Ficha Técnica MEP salva",
            detail = "Salve a ficha de uma receita da semana",
            actionLabel = if (onOpenMepSheet != null) "Abrir" else null,
            onAction = onOpenMepSheet,
            tag = "criterion_mep_sheet"
        )
        CriterionRow(
            done = week.flashcardsDone,
            title = "Flashcards revisados (${week.flashcardsReviewed}/${week.flashcardsTotal})",
            detail = "Revise os cartões SM-2 desta semana",
            actionLabel = "Revisar",
            onAction = onReviewFlashcards,
            tag = "criterion_flashcards"
        )
        CriterionRow(
            done = week.selfEvalDone,
            title = "Autoavaliação de bancada",
            detail = "Registre as notas e reflexões (Selbstkontrolle)",
            actionLabel = "Avaliar",
            onAction = onOpenSelfEvaluation,
            tag = "criterion_self_eval"
        )
    }
}

@Composable
private fun CriterionRow(
    done: Boolean,
    title: String,
    detail: String,
    tag: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (done) "Critério atendido" else "Critério pendente",
            tint = if (done) PccGreen else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (!done) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!done && actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun WeekCompletionActions(
    week: WeekProgress,
    totalWeeks: Int,
    onCompleteWeek: (Int) -> Unit,
    onReopenWeek: (Int) -> Unit,
    onSelectWeek: (Int) -> Unit
) {
    val completedAt = week.completedAtEpochMs
    if (completedAt != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PccGreenBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = PccGreen)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Concluída em ${formatEpochMs(completedAt)}",
                    color = PccGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { onReopenWeek(week.weekNumber) },
                    modifier = Modifier.testTag("course_reopen_week")
                ) { Text("Reabrir") }
            }
        }
        if (week.weekNumber < totalWeeks) {
            Button(
                onClick = { onSelectWeek(week.weekNumber + 1) },
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("course_go_next_week")
            ) { Text("Começar a Semana S${"%02d".format(week.weekNumber + 1)}", fontWeight = FontWeight.Bold) }
        }
    } else {
        Button(
            onClick = { onCompleteWeek(week.weekNumber) },
            enabled = week.allCriteriaMet,
            colors = ButtonDefaults.buttonColors(containerColor = PccGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("course_complete_week")
        ) {
            Text(
                text = if (week.allCriteriaMet) {
                    "Concluir Semana S${"%02d".format(week.weekNumber)}"
                } else {
                    "Faltam ${WeekProgress.CRITERIA_COUNT - week.criteriaMetCount} critério(s) para concluir"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CycleDeliverables(
    cycle: CycleProgress,
    onToggleDeliverable: (cycleNumber: Int, index: Int, isDone: Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ENTREGÁVEIS DO CICLO ${cycle.cycleNumber} (${cycle.deliverablesDone.size}/${cycle.deliverables.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TagPill(
                text = "${cycle.completedWeeks}/${cycle.weekNumbers.size} SEMANAS",
                containerColor = if (cycle.isCompleted) PccGreenBg else if (cycle.completedWeeks > 0) PccAmberBg else SwissRedContainer,
                contentColor = if (cycle.isCompleted) PccGreen else if (cycle.completedWeeks > 0) PccAmber else OnSwissRedContainer
            )
        }
        cycle.deliverables.forEachIndexed { index, text ->
            val checked = index in cycle.deliverablesDone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_deliverable_${cycle.cycleNumber}_$index")
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggleDeliverable(cycle.cycleNumber, index, it) },
                    colors = CheckboxDefaults.colors(checkedColor = PccGreen)
                )
                Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
    }
}
