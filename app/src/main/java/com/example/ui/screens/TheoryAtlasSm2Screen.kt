package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.curriculum.TechnicalAtlasCatalog
import com.example.data.local.StudyTimerEntity
import com.example.domain.model.SmartKitchenAlertType
import com.example.ui.components.KeepScreenOnEffect
import com.example.ui.components.SmartKitchenAlertBanner
import com.example.ui.components.TagPill
import com.example.ui.components.formatSecondsMmSs
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TheoryAtlasSm2Screen(
    atelierVm: AtelierViewModel,
    onJumpToRecipe: (String) -> Unit
) {
    val selectedWeekNum by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val timersMap by atelierVm.timersMap.collectAsStateWithLifecycle()
    val sm2Map by atelierVm.sm2Map.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val subTabs = listOf(
        "Aula 80 min",
        "Schnittarten (12)",
        "Bateria PT/DE/FR",
        "Anatomia Rind A–H",
        "Swissmilk & Eixos",
        "Flashcards SM-2"
    )

    val currentWeek = CurriculumWeeksAndGuides.weeks.find { it.weekNumber == selectedWeekNum }
        ?: CurriculumWeeksAndGuides.weeks.first()
    val timerState = timersMap[selectedWeekNum] ?: StudyTimerEntity(weekNumber = selectedWeekNum)

    KeepScreenOnEffect(enabled = timerState.isRunning)

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = subTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("atlas_subtab_$index")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (subTab) {
                0 -> Lesson80MinSection(
                    currentWeek = currentWeek,
                    timerState = timerState,
                    onToggleTimer = { atelierVm.toggleStudyTimer(currentWeek.weekNumber) },
                    onSelectWeek = { atelierVm.selectWeek(it) }
                )
                1 -> SchnittartenSection()
                2 -> KochgeschirrartenSection()
                3 -> SwissBeefAnatomySection()
                4 -> BrochuresAndSwissmilkCantonsSection(onJumpToRecipe = onJumpToRecipe)
                5 -> Sm2FlashcardsTrainerSection(
                    sm2Map = sm2Map,
                    onGrade = { cardId, grade -> atelierVm.gradeFlashcard(cardId, grade) }
                )
            }
        }
    }
}

@Composable
private fun Lesson80MinSection(
    currentWeek: com.example.domain.model.CourseWeek,
    timerState: StudyTimerEntity,
    onToggleTimer: () -> Unit,
    onSelectWeek: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..24).forEach { w ->
            FilterChip(
                selected = w == currentWeek.weekNumber,
                onClick = { onSelectWeek(w) },
                label = { Text("S${"%02d".format(w)}", fontFamily = JetBrainsMonoFamily) },
                modifier = Modifier.height(48.dp)
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("SEMANA ${currentWeek.weekNumber} • ${currentWeek.cycleName}", SwissRedContainer, OnSwissRedContainer)
            Text(currentWeek.title, style = MaterialTheme.typography.titleLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${AtelierViewModel.STUDY_PHASE_LABELS[timerState.currentPhaseIndex]} • ${formatSecondsMmSs(timerState.remainingSeconds)}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("theory_timer_toggle")
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Alternar cronômetro"
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (timerState.isRunning) "Pausar" else "Iniciar")
                }
            }
        }
    }

    val phases = listOf(
        "1. Conceito Técnico (10 min)" to currentWeek.concept10Min,
        "2. Visualização & Geometria (20 min)" to currentWeek.visualization20Min,
        "3. Aplicação Estruturada (30 min)" to currentWeek.application30Min,
        "4. Treino Deliberado (15 min)" to currentWeek.practice15Min,
        "5. Síntese & PCC (5 min)" to currentWeek.synthesis5Min
    )
    phases.forEachIndexed { idx, (label, body) ->
        val isActive = timerState.currentPhaseIndex == idx
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) SwissRedContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) SwissRed else MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SchnittartenSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PccDangerBg),
        border = BorderStroke(1.5.dp, PccDanger),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = TechnicalAtlasCatalog.DULL_BLADE_QUALITY_RULE,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = PccDanger,
            modifier = Modifier.padding(14.dp)
        )
    }

    TechnicalAtlasCatalog.swissCuts.forEach { cut ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(cut.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TagPill(cut.dimensionSwiss, SwissRedContainer, OnSwissRedContainer)
                }
                Text("Lâmina oficial: ${cut.bladeTool}", style = MaterialTheme.typography.labelLarge, color = InfoBlue)
                Text("Aplicações suíças: ${cut.typicalApplications}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun KochgeschirrartenSection() {
    var query by rememberSaveable { mutableStateOf("") }

    SmartKitchenAlertBanner(
        alertType = SmartKitchenAlertType.NON_STICK_VITAMIN_C,
        message = "Regra Kochgeschirrarten: NUNCA saltear vegetais ricos em vitamina C em Lyoner Pfanne (aço puro), pois íons metálicos oxidam e destroem a vitamina C. Exija Poêle antiadhésif (Antihaft-Pfanne)."
    )

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Buscar em Português, Alemão ou Francês (ex.: Sautoir, Flachkasserolle)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cookware_trilingual_search")
    )

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) TechnicalAtlasCatalog.cookwareAndBlades
        else TechnicalAtlasCatalog.cookwareAndBlades.filter {
            it.namePt.lowercase().contains(q) ||
                it.nameDe.lowercase().contains(q) ||
                it.nameFr.lowercase().contains(q) ||
                it.kitchenRule.lowercase().contains(q)
        }
    }

    filtered.forEach { item ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TagPill("NÍVEL ${item.tierLevel} • ${item.category}", InfoBlueBg, InfoBlue)
                }
                Text("PT: ${item.namePt}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "DE: ${item.nameDe}  |  FR: ${item.nameFr}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = SwissRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(item.kitchenRule, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwissBeefAnatomySection() {
    var selectedCode by rememberSaveable { mutableStateOf("B") }
    val selectedPrimal = TechnicalAtlasCatalog.swissBeefPrimals.find { it.code == selectedCode }
        ?: TechnicalAtlasCatalog.swissBeefPrimals.first()

    Text(
        text = "MAPA INTERATIVO BOVINO SUÍÇO (RIND A–H)",
        style = MaterialTheme.typography.titleMedium
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TechnicalAtlasCatalog.swissBeefPrimals.forEach { primal ->
            val isSel = primal.code == selectedCode
            OutlinedButton(
                onClick = { selectedCode = primal.code },
                border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) SwissRed else MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSel) SwissRedContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("rind_primal_${primal.code}")
            ) {
                Text(
                    text = "${primal.code} • ${primal.nameDe}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("QUARTO ${selectedPrimal.code} • FR: ${selectedPrimal.nameFr}", SwissRedContainer, OnSwissRedContainer)
            Text("${selectedPrimal.nameDe} — ${selectedPrimal.namePt}", style = MaterialTheme.typography.titleLarge)
            Text("Tecido conectivo: ${selectedPrimal.connectiveTissueTier}", style = MaterialTheme.typography.labelLarge, color = PccAmber)

            Text("Subcortes Oficiais Suíços:", fontWeight = FontWeight.Bold)
            selectedPrimal.subcuts.forEach { sub ->
                Text("• $sub", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Técnicas de Cocção Ideais (Gartechniken):", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedPrimal.idealTechniques.forEach { tech ->
                    TagPill(tech, PccGreenBg, PccGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrochuresAndSwissmilkCantonsSection(onJumpToRecipe: (String) -> Unit) {
    var selectedCanton by rememberSaveable { mutableStateOf("TODOS") }
    val cantons = remember {
        listOf("TODOS") + SwissmilkAndBaseRecipesCatalog.allRecipes
            .map { it.cantonCode }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    Text("FILTRAR CLÁSSICOS REGIONAIS SWISSMILK POR CANTÃO", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cantons.forEach { canton ->
            FilterChip(
                selected = selectedCanton == canton,
                onClick = { selectedCanton = canton },
                label = { Text(canton, fontFamily = JetBrainsMonoFamily) },
                modifier = Modifier.height(48.dp)
            )
        }
    }

    val swissmilkRecipes = remember(selectedCanton) {
        SwissmilkAndBaseRecipesCatalog.allRecipes.filter {
            it.isSwissmilkClassic && (selectedCanton == "TODOS" || it.cantonCode == selectedCanton)
        }
    }

    swissmilkRecipes.forEach { rec ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onJumpToRecipe(rec.id) }
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagPill("CANTÃO ${rec.cantonCode}", SwissRedContainer, OnSwissRedContainer)
                    TagPill("SEMANA ${rec.weekNumber}", InfoBlueBg, InfoBlue)
                }
                Text(rec.title, style = MaterialTheme.typography.titleMedium)
                Text(rec.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Sm2FlashcardsTrainerSection(
    sm2Map: Map<String, com.example.data.local.Sm2ProgressEntity>,
    onGrade: (String, Int) -> Unit
) {
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val sortedCards = remember(sm2Map) {
        TechnicalAtlasCatalog.flashcards.sortedBy { card ->
            val st = sm2Map[card.id]
            if (st == null || st.nextReviewEpochDay <= todayEpochDay) 0L else st.nextReviewEpochDay
        }
    }

    var currentIdx by rememberSaveable { mutableIntStateOf(0) }
    var isAnswerRevealed by rememberSaveable { mutableStateOf(false) }

    val activeCard = sortedCards[currentIdx % sortedCards.size]
    val cardState = sm2Map[activeCard.id]

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TagPill("${activeCard.category} • S${"%02d".format(activeCard.weekNumber)}", SwissRedContainer, OnSwissRedContainer)
                TagPill(
                    text = if (cardState == null) "NOVO CARD" else "EF ${"%.2f".format(cardState.easinessFactor)} • Int ${cardState.intervalDays}d",
                    containerColor = InfoBlueBg,
                    contentColor = InfoBlue
                )
            }

            Text(
                text = "Card ${(currentIdx % sortedCards.size) + 1} de ${sortedCards.size}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFamily
            )

            Text(
                text = activeCard.frontQuestion,
                style = MaterialTheme.typography.titleLarge
            )

            if (!isAnswerRevealed) {
                Button(
                    onClick = { isAnswerRevealed = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("reveal_flashcard_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Revelar Resposta Técnica", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccGreenBg),
                    border = BorderStroke(1.dp, PccGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeCard.backAnswer,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Text(
                    text = "Avalie sua retenção (Algoritmo SuperMemo SM-2: 0 = Branco total → 5 = Domínio imediato):",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (0..5).forEach { grade ->
                        OutlinedButton(
                            onClick = {
                                onGrade(activeCard.id, grade)
                                isAnswerRevealed = false
                                currentIdx = (currentIdx + 1) % sortedCards.size
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("sm2_grade_$grade")
                        ) {
                            Text("Nota $grade", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
