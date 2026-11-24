package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.audio.KitchenAudioNarrator
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.DidacticAxisBrochure
import com.example.data.curriculum.DidacticBlock
import com.example.data.curriculum.DidacticLibraryLoader
import com.example.data.curriculum.DidacticSection
import com.example.ui.components.AudioNarrationPlayerCard
import com.example.ui.components.TagPill
import com.example.ui.theme.AtelierCharcoal
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
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DidacticLibrarySection(
    selectedAxisNumber: Int,
    selectedWeekNumber: Int,
    audioNarrator: KitchenAudioNarrator,
    onSelectAxis: (Int) -> Unit,
    onPracticeAxisInSm2: (Int) -> Unit
) {
    val catalog = remember { DidacticLibraryLoader.catalog }
    val activeAxis = remember(selectedAxisNumber, catalog) {
        DidacticLibraryLoader.findAxis(selectedAxisNumber)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterCurrentWeekOnly by rememberSaveable { mutableStateOf(false) }
    var focusedSectionId by rememberSaveable(selectedAxisNumber) { mutableStateOf<String?>(null) }
    var showFlashcardsAccordion by rememberSaveable(selectedAxisNumber) { mutableStateOf(true) }
    var showBibliographyAccordion by rememberSaveable(selectedAxisNumber) { mutableStateOf(false) }
    var showMepGuideModal by rememberSaveable { mutableStateOf(false) }

    val searchHits = remember(searchQuery) {
        DidacticLibraryLoader.searchAcrossLibrary(searchQuery)
    }

    val visibleSections = remember(activeAxis, filterCurrentWeekOnly, selectedWeekNumber, focusedSectionId) {
        val weekFiltered = if (filterCurrentWeekOnly) {
            activeAxis.sections.filter { selectedWeekNumber in it.weeks }
        } else {
            activeAxis.sections
        }
        if (focusedSectionId != null) {
            val exact = weekFiltered.filter { it.id == focusedSectionId }
            exact.ifEmpty { weekFiltered }
        } else {
            weekFiltered
        }
    }

    // Header Banner + Global Search + Filter Controls
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("didactic_library_header_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TagPill("CONSULTA NATIVA NO APP • ZERO DOWNLOADS", PccGreenBg, PccGreen)
                TagPill("${catalog.axes.size} EIXOS • 60 CARTÕES", SwissRedContainer, OnSwissRedContainer)
            }
            Text(
                text = catalog.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = catalog.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.didactic_library_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("didactic_library_search_input")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterCurrentWeekOnly,
                    onClick = {
                        filterCurrentWeekOnly = !filterCurrentWeekOnly
                        focusedSectionId = null
                    },
                    label = {
                        Text(
                            text = "Apenas Semana Atual (S${"%02d".format(selectedWeekNumber)})",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwissRedContainer,
                        selectedLabelColor = OnSwissRedContainer
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("didactic_filter_current_week")
                )

                if (focusedSectionId != null) {
                    FilterChip(
                        selected = true,
                        onClick = { focusedSectionId = null },
                        label = { Text("Exibir Todas as Seções do Eixo ${activeAxis.axisNumber}") },
                        modifier = Modifier.height(48.dp)
                    )
                }

                OutlinedButton(
                    onClick = { showMepGuideModal = true },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("didactic_open_mep_guide_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = InfoBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("Guia & Modelo Ficha MEP (8 Blocos)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Real-time Search Results Panel across all 6 Eixos
    if (searchQuery.trim().length >= 2) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, InfoBlue),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("didactic_search_results_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RESULTADOS EM TODOS OS 6 EIXOS (${searchHits.size} ocorrências para \"${searchQuery.trim()}\")",
                    style = MaterialTheme.typography.labelLarge,
                    color = InfoBlue,
                    fontWeight = FontWeight.Bold
                )
                if (searchHits.isEmpty()) {
                    Text(
                        text = "Nenhum termo correspondente encontrado nas 23 seções, tabelas ou cartões.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    searchHits.take(16).forEachIndexed { idx, hit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAxis(hit.axisNumber)
                                    filterCurrentWeekOnly = false
                                    focusedSectionId = hit.sectionId
                                    if (hit.sectionId == null) {
                                        showFlashcardsAccordion = true
                                    }
                                    searchQuery = ""
                                }
                                .testTag("didactic_search_hit_$idx")
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TagPill("EIXO ${hit.axisNumber}", SwissRedContainer, OnSwissRedContainer)
                                    TagPill(hit.hitKind, InfoBlueBg, InfoBlue)
                                }
                                Text(
                                    text = "${hit.sectionNumber} • ${hit.sectionTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(hit.snippet, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Adaptive 2-Pane Reader (>= 720.dp) vs Mobile Selector + Reader (< 720.dp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isTwoPane = maxWidth >= 720.dp
        if (isTwoPane) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.width(270.dp)
                ) {
                    Text(
                        text = "OS 6 EIXOS DA GASTRONOMIA",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    catalog.axes.forEach { axis ->
                        AxisSelectorCard(
                            axis = axis,
                            isSelected = axis.axisNumber == activeAxis.axisNumber,
                            onClick = {
                                onSelectAxis(axis.axisNumber)
                                focusedSectionId = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SectionIndexCard(
                        axis = activeAxis,
                        focusedSectionId = focusedSectionId,
                        onSelectSection = { secId ->
                            focusedSectionId = if (focusedSectionId == secId) null else secId
                        }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AxisReadingPane(
                        axis = activeAxis,
                        visibleSections = visibleSections,
                        selectedWeekNumber = selectedWeekNumber,
                        filterCurrentWeekOnly = filterCurrentWeekOnly,
                        audioNarrator = audioNarrator,
                        showFlashcards = showFlashcardsAccordion,
                        onToggleFlashcards = { showFlashcardsAccordion = !showFlashcardsAccordion },
                        showBibliography = showBibliographyAccordion,
                        onToggleBibliography = { showBibliographyAccordion = !showBibliographyAccordion },
                        onDisableWeekFilter = { filterCurrentWeekOnly = false },
                        onPracticeAxisInSm2 = { onPracticeAxisInSm2(activeAxis.axisNumber) }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    catalog.axes.forEach { axis ->
                        AxisSelectorCard(
                            axis = axis,
                            isSelected = axis.axisNumber == activeAxis.axisNumber,
                            onClick = {
                                onSelectAxis(axis.axisNumber)
                                focusedSectionId = null
                            },
                            modifier = Modifier.width(248.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = focusedSectionId == null,
                        onClick = { focusedSectionId = null },
                        label = { Text("Todas (${activeAxis.sections.size})") },
                        modifier = Modifier.height(48.dp)
                    )
                    activeAxis.sections.forEach { sec ->
                        FilterChip(
                            selected = focusedSectionId == sec.id,
                            onClick = { focusedSectionId = if (focusedSectionId == sec.id) null else sec.id },
                            label = { Text("${sec.number} ${sec.title.take(24)}") },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("didactic_section_chip_${sec.id}")
                        )
                    }
                }

                AxisReadingPane(
                    axis = activeAxis,
                    visibleSections = visibleSections,
                    selectedWeekNumber = selectedWeekNumber,
                    filterCurrentWeekOnly = filterCurrentWeekOnly,
                    audioNarrator = audioNarrator,
                    showFlashcards = showFlashcardsAccordion,
                    onToggleFlashcards = { showFlashcardsAccordion = !showFlashcardsAccordion },
                    showBibliography = showBibliographyAccordion,
                    onToggleBibliography = { showBibliographyAccordion = !showBibliographyAccordion },
                    onDisableWeekFilter = { filterCurrentWeekOnly = false },
                    onPracticeAxisInSm2 = { onPracticeAxisInSm2(activeAxis.axisNumber) }
                )
            }
        }
    }

    if (showMepGuideModal) {
        MepGuideConsultationDialog(onDismiss = { showMepGuideModal = false })
    }
}

@Composable
private fun AxisSelectorCard(
    axis: DidacticAxisBrochure,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SwissRedContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SwissRed else MaterialTheme.colorScheme.outline),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("didactic_axis_card_${axis.axisNumber}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TagPill("EIXO ${axis.axisNumber}", SwissRedContainer, OnSwissRedContainer)
                Text(
                    text = "~${axis.estimatedReadMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily
                )
            }
            Text(axis.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(
                text = "Semanas: ${axis.weeksCovered.joinToString(", ") { "S%02d".format(it) }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionIndexCard(
    axis: DidacticAxisBrochure,
    focusedSectionId: String?,
    onSelectSection: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SUMÁRIO DO EIXO ${axis.axisNumber}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            axis.sections.forEach { sec ->
                val isSel = focusedSectionId == sec.id
                Text(
                    text = "${sec.number} • ${sec.title}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) SwissRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSection(sec.id) }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AxisReadingPane(
    axis: DidacticAxisBrochure,
    visibleSections: List<DidacticSection>,
    selectedWeekNumber: Int,
    filterCurrentWeekOnly: Boolean,
    audioNarrator: KitchenAudioNarrator,
    showFlashcards: Boolean,
    onToggleFlashcards: () -> Unit,
    showBibliography: Boolean,
    onToggleBibliography: () -> Unit,
    onDisableWeekFilter: () -> Unit,
    onPracticeAxisInSm2: () -> Unit
) {
    val narrationState by audioNarrator.state.collectAsStateWithLifecycle()
    val axisSourceId = "axis_${axis.axisNumber}"

    // Axis Hero & Overview Cards
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TagPill("EIXO ${axis.axisNumber} • LEITURA ~${axis.estimatedReadMinutes} MIN", SwissRedContainer, OnSwissRedContainer)
                axis.badges.forEach { badge ->
                    TagPill(badge, InfoBlueBg, InfoBlue)
                }
            }
            Text(axis.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(axis.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            axis.overviewCards.forEach { ov ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ov.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SwissRed, fontWeight = FontWeight.Bold)
                        Text(ov.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Native pt-BR Audio Narration Player for the entire Axis
    AudioNarrationPlayerCard(
        sourceId = axisSourceId,
        title = "Áudio do Eixo ${axis.axisNumber} • ${axis.title}",
        subtitle = "Narração integral em pt-BR das ${axis.sections.size} seções (controle de velocidade e avanço por trecho)",
        narrationState = narrationState,
        onTogglePlayPause = {
            audioNarrator.togglePlayPause(
                fallbackSourceId = axisSourceId,
                fallbackTitle = "Eixo ${axis.axisNumber} • ${axis.title}",
                fallbackSegmentsProvider = { KitchenAudioNarrator.buildAxisSegments(axis) }
            )
        },
        onSkipPrevious = { audioNarrator.skipPrevious() },
        onSkipNext = { audioNarrator.skipNext() },
        onStop = { audioNarrator.stop() },
        onSelectSpeed = { audioNarrator.setSpeechRate(it) }
    )

    if (visibleSections.isEmpty() && filterCurrentWeekOnly) {
        Card(colors = CardDefaults.cardColors(containerColor = PccAmberBg), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "O Eixo ${axis.axisNumber} não possui seções vinculadas diretamente à Semana S${"%02d".format(selectedWeekNumber)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PccAmber
                )
                OutlinedButton(onClick = onDisableWeekFilter, modifier = Modifier.height(48.dp)) {
                    Text("Mostrar todas as ${axis.sections.size} seções deste Eixo")
                }
            }
        }
    }

    visibleSections.forEach { section ->
        val sectionSourceId = "axis_${axis.axisNumber}_sec_${section.id}"
        val isPlayingThisSection = narrationState.activeSourceId == sectionSourceId && narrationState.isPlaying
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(if (isPlayingThisSection) 1.5.dp else 1.dp, if (isPlayingThisSection) InfoBlue else MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("didactic_section_card_${section.id}")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TagPill("SEÇÃO ${section.number}", SwissRedContainer, OnSwissRedContainer)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = section.weeks.joinToString(", ") { "S%02d".format(it) },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = InfoBlue
                        )
                        OutlinedButton(
                            onClick = {
                                audioNarrator.togglePlayPause(
                                    fallbackSourceId = sectionSourceId,
                                    fallbackTitle = "Eixo ${axis.axisNumber} • Seção ${section.number}",
                                    fallbackSegmentsProvider = {
                                        KitchenAudioNarrator.buildSingleSectionSegments(axis.axisNumber, section)
                                    }
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("didactic_section_audio_${section.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ouvir Seção ${section.number}",
                                tint = if (isPlayingThisSection) SwissRed else InfoBlue
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlayingThisSection) "Pausar" else "Ouvir", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                section.blocks.forEach { block ->
                    RenderDidacticBlock(block)
                }
            }
        }
    }

    // Footer 1: Flashcards de Revisão do Eixo + Atalho SM-2
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, InfoBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleFlashcards() }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TagPill("FIXAÇÃO ATIVA • REPETIÇÃO ESPAÇADA", InfoBlueBg, InfoBlue)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Cartões de Revisão do Eixo ${axis.axisNumber} (${axis.flashcards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (showFlashcards) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Alternar lista de cartões"
                )
            }

            Button(
                onClick = onPracticeAxisInSm2,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("didactic_practice_sm2_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.didactic_practice_sm2)} (Eixo ${axis.axisNumber})",
                    fontWeight = FontWeight.Bold
                )
            }

            if (showFlashcards) {
                axis.flashcards.forEach { fc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${fc.id.uppercase()} • ${fc.topic}", style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMonoFamily, color = InfoBlue)
                            Text("P: ${fc.question}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("R: ${fc.answer}", style = MaterialTheme.typography.bodySmall, color = PccGreen)
                        }
                    }
                }
            }
        }
    }

    // Footer 2: Bibliografia Comentada do Eixo
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleBibliography() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SwissRed)
                    Text(
                        text = "Bibliografia Técnica Integrada (${axis.bibliography.size} obras)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (showBibliography) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Alternar bibliografia"
                )
            }
            if (showBibliography) {
                axis.bibliography.forEach { ref ->
                    Text("• $ref", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderDidacticBlock(block: DidacticBlock) {
    when (block) {
        is DidacticBlock.Paragraph -> {
            Text(
                text = parseEditorialMarkdown(block.text),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        is DidacticBlock.Bullets -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = SwissRed, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseEditorialMarkdown(item),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        is DidacticBlock.Table -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (block.caption.isNotBlank()) {
                        Text(block.caption, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SwissRed)
                    }
                    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier
                                .background(AtelierCharcoal, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            block.headers.forEach { h ->
                                Text(
                                    text = h,
                                    color = PureSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .widthIn(min = 128.dp, max = 220.dp)
                                        .padding(horizontal = 6.dp)
                                )
                            }
                        }
                        block.rows.forEachIndexed { rIdx, row ->
                            val rowBg = if (rIdx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .padding(vertical = 7.dp, horizontal = 6.dp)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = parseEditorialMarkdown(cell),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .widthIn(min = 128.dp, max = 220.dp)
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
        is DidacticBlock.Callout -> {
            val (bg, border, badgeLabel) = when (block.variant) {
                "critical" -> Triple(PccDangerBg, PccDanger, "PONTO CRÍTICO (PCC)")
                "mep" -> Triple(InfoBlueBg, InfoBlue, "MISE EN PLACE & AVOR")
                "troubleshooting" -> Triple(PccAmberBg, PccAmber, "DIAGNÓSTICO DE BANCADA")
                else -> Triple(PccGreenBg, PccGreen, "BOAS PRÁTICAS & DICA")
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.5.dp, border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(badgeLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = border)
                    Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(parseEditorialMarkdown(block.body), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        is DidacticBlock.MetricStrip -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (block.title.isNotBlank()) {
                    Text(block.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    block.items.forEach { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AtelierCharcoal),
                            modifier = Modifier.widthIn(min = 148.dp, max = 240.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(m.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD0D7DF))
                                Text(m.value, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PureSurface)
                                Text(m.detail, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE5E9EE))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MepGuideConsultationDialog(onDismiss: () -> Unit) {
    var guideTab by rememberSaveable { mutableIntStateOf(0) }
    val example = CurriculumWeeksAndGuides.exampleMepZurcherRosti

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TagPill("CONSULTA NO APP • SEM DOWNLOAD EXTERNO", PccGreenBg, PccGreen)
                Text("Guia & Modelo da Ficha Técnica MEP (8 Blocos)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = guideTab == 0,
                        onClick = { guideTab = 0 },
                        label = { Text("Estrutura (8 Blocos)") },
                        modifier = Modifier.height(44.dp)
                    )
                    FilterChip(
                        selected = guideTab == 1,
                        onClick = { guideTab = 1 },
                        label = { Text("Exemplo Zurique (S08)") },
                        modifier = Modifier.height(44.dp)
                    )
                }

                if (guideTab == 0) {
                    CurriculumWeeksAndGuides.blankMepTemplateMarkdown
                        .lines()
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            val clean = line.removePrefix("# ").removePrefix("## ")
                            val isHeader = line.startsWith("#")
                            Text(
                                text = parseEditorialMarkdown(clean),
                                style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeader) SwissRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                } else {
                    listOf(
                        "1. Identificação & Escala" to example.block1Identification,
                        "2. Matriz PB = PL × FC" to example.block2IngredientsPbPlFc,
                        "3. Bateria & Cutelaria" to example.block3CookwareBlades,
                        "4. Pré-Preparo & Cortes (AVOR)" to example.block4PrePrepCuts,
                        "5. Fluxo Térmico & PCC (≥ ${example.pccTargetTempC} °C)" to example.block5ThermalFlowPcc,
                        "6. Empratamento (${example.plateTempC} °C)" to example.block6PlatingFinishing,
                        "7. Higiene & Alergênicos" to example.block7HygieneAllergens,
                        "8. Selbstkontrolle (${example.executionScore1To5}/5)" to example.block8Selbstkontrolle
                    ).forEach { (heading, content) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(heading, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SwissRed)
                                Text(content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("close_mep_guide_dialog_button")
            ) {
                Text("Fechar Consulta", fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun parseEditorialMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < raw.length) {
        if (raw.startsWith("**", i)) {
            val end = raw.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(raw.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (raw[i] == '*' && (i + 1 < raw.length && raw[i + 1] != ' ')) {
            val end = raw.indexOf('*', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(raw.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(raw[i])
        i++
    }
}
