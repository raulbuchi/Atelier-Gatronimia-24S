package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.audio.KitchenAudioNarrator
import com.example.core.audio.OfflinePodcastPlayer
import com.example.data.curriculum.CurriculumWeeksAndGuides
import com.example.data.curriculum.DidacticLibraryLoader
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.local.StudyTimerEntity
import com.example.domain.model.BrochureGuideSpec
import com.example.ui.components.AtelierFullLogoBanner
import com.example.ui.components.AudioNarrationPlayerCard
import com.example.ui.components.CoursePodcastPlayerCard
import com.example.ui.components.KeepScreenOnEffect
import com.example.ui.components.TagPill
import com.example.ui.components.formatSecondsMmSs
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.StreamUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Dashboard24SScreen(
    atelierVm: AtelierViewModel,
    communityVm: CommunityAiViewModel,
    audioNarrator: KitchenAudioNarrator,
    podcastPlayer: OfflinePodcastPlayer,
    onOpenTheoryTab: () -> Unit,
    onOpenRecipeKitchen: (String) -> Unit,
    onOpenCommunityTab: () -> Unit
) {
    val selectedWeekNum by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val timersMap by atelierVm.timersMap.collectAsStateWithLifecycle()
    val sm2Map by atelierVm.sm2Map.collectAsStateWithLifecycle()
    val selfEvalsMap by atelierVm.selfEvaluationsMap.collectAsStateWithLifecycle()
    val mepSheetsMap by atelierVm.mepSheetsMap.collectAsStateWithLifecycle()
    val myPostsState by communityVm.myPostsState.collectAsStateWithLifecycle()
    val narrationState by audioNarrator.state.collectAsStateWithLifecycle()
    val podcastState by podcastPlayer.state.collectAsStateWithLifecycle()

    val currentWeek = remember(selectedWeekNum) {
        CurriculumWeeksAndGuides.weeks.find { it.weekNumber == selectedWeekNum }
            ?: CurriculumWeeksAndGuides.weeks.first()
    }
    var selectedCycle by remember(currentWeek.cycleNumber) { mutableIntStateOf(currentWeek.cycleNumber) }

    val timerState = timersMap[selectedWeekNum] ?: StudyTimerEntity(
        weekNumber = selectedWeekNum,
        currentPhaseIndex = 0,
        remainingSeconds = AtelierViewModel.STUDY_PHASE_DURATIONS_SEC[0],
        isRunning = false
    )

    // Keep screen active during 80-min bench study block
    KeepScreenOnEffect(enabled = timerState.isRunning)

    val dueCardsCount = remember(sm2Map) { atelierVm.dueFlashcardsCount() }
    val submittedPhotosCount = when (val st = myPostsState) {
        is StreamUiState.Success -> st.data.size
        else -> 0
    }
    val completedWeeksCount = selfEvalsMap.size
    val weekRecipes = remember(selectedWeekNum) {
        SwissmilkAndBaseRecipesCatalog.recipesForWeek(selectedWeekNum)
    }
    val weekBrochures = remember(currentWeek.supportBrochureIds) {
        currentWeek.supportBrochureIds.mapNotNull { id ->
            CurriculumWeeksAndGuides.brochures.find { it.id == id }
        }
    }
    val weekQuickPhotos = remember(selectedWeekNum, myPostsState, mepSheetsMap) {
        val fromPosts = (myPostsState as? StreamUiState.Success)?.data
            ?.filter { it.weekNumber == selectedWeekNum && it.thumbnailBase64.isNotBlank() }
            ?.map { it.recipeTitle.ifBlank { "Registro S${"%02d".format(selectedWeekNum)}" } to it.thumbnailBase64 }
            .orEmpty()
        val fromMep = mepSheetsMap.values
            .filter { it.weekNumber == selectedWeekNum && it.photoThumbnailBase64.isNotBlank() }
            .map { it.title.ifBlank { "Ficha MEP" } to it.photoThumbnailBase64 }
        (fromPosts + fromMep).distinctBy { it.second }.take(6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner with Official Full Horizontal Logo
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagPill(
                        text = "CRONOGRAMA 24 SEMANAS • 4 CICLOS (v5)",
                        containerColor = SwissRedContainer,
                        contentColor = OnSwissRedContainer
                    )
                    if (timerState.isRunning) {
                        TagPill(
                            text = stringResource(R.string.keep_screen_on_badge),
                            containerColor = PccGreenBg,
                            contentColor = PccGreen
                        )
                    }
                }
                AtelierFullLogoBanner(
                    maxHeight = 96.dp,
                    modifier = Modifier.testTag("dashboard_brand_logo_wide")
                )

                // KPI Summary Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatChip(
                        label = "Semanas Avaliadas",
                        value = "$completedWeeksCount / 24",
                        bg = PccGreenBg,
                        fg = PccGreen,
                        onClick = {}
                    )
                    KpiStatChip(
                        label = "Revisões SM-2",
                        value = "$dueCardsCount / 60",
                        bg = PccAmberBg,
                        fg = PccAmber,
                        onClick = onOpenTheoryTab
                    )
                    KpiStatChip(
                        label = "Fotos no Portfólio",
                        value = "$submittedPhotosCount registros",
                        bg = InfoBlueBg,
                        fg = InfoBlue,
                        onClick = onOpenCommunityTab
                    )
                }
            }
        }

        // 4 Cycles Selector (6 weeks per cycle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "LINHA DO TEMPO • 4 CICLOS DE 6 SEMANAS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cycleLabels = listOf(
                    1 to "Ciclo 1: Base & Segurança (S1–S6)",
                    2 to "Ciclo 2: Carnes & Clássicos (S7–S12)",
                    3 to "Ciclo 3: Confeitaria & Sous-Vide (S13–S18)",
                    4 to "Ciclo 4: Chefkoch / Gestor (S19–S24)"
                )
                cycleLabels.forEach { (cycleNum, label) ->
                    FilterChip(
                        selected = selectedCycle == cycleNum,
                        onClick = {
                            selectedCycle = cycleNum
                            val firstWeekInCycle = (cycleNum - 1) * 6 + 1
                            atelierVm.selectWeek(firstWeekInCycle)
                        },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SwissRedContainer,
                            selectedLabelColor = OnSwissRedContainer
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("cycle_chip_$cycleNum")
                    )
                }
            }

            // 6 Weeks inside selected Cycle
            val weeksInCycle = CurriculumWeeksAndGuides.weeks.filter { it.cycleNumber == selectedCycle }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                weeksInCycle.forEach { week ->
                    val isSelected = week.weekNumber == selectedWeekNum
                    val isDone = selfEvalsMap.containsKey(week.weekNumber)
                    OutlinedButton(
                        onClick = { atelierVm.selectWeek(week.weekNumber) },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SwissRed else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) SwissRedContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) OnSwissRedContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("week_button_${week.weekNumber}")
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Semana avaliada",
                                tint = PccGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "S${"%02d".format(week.weekNumber)}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Player de Episódios Deep Dive (100% Offline embutidos no app)
        CoursePodcastPlayerCard(
            currentCycleNumber = currentWeek.cycleNumber,
            podcastState = podcastState,
            onSelectAndPlay = { episode -> podcastPlayer.playEpisode(episode) },
            onTogglePlayPause = { episode -> podcastPlayer.togglePlayPause(episode) },
            onSeekTo = { ms -> podcastPlayer.seekTo(ms) },
            onSkipBy = { delta -> podcastPlayer.skipBy(delta) },
            onStop = { podcastPlayer.stop() },
            onSelectSpeed = { speed -> podcastPlayer.setPlaybackSpeed(speed) }
        )

        // Current Week Hero Card + 80-Min Study Block Engine
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, SwissRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TagPill(
                        text = "SEMANA ATUAL S${"%02d".format(currentWeek.weekNumber)} • EIXO ${currentWeek.axisId}",
                        containerColor = SwissRedContainer,
                        contentColor = OnSwissRedContainer
                    )
                    TagPill(
                        text = "Blocos 80m concluídos: ${timerState.completedBlocksCount}",
                        containerColor = PccGreenBg,
                        contentColor = PccGreen
                    )
                }

                Text(
                    text = currentWeek.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Foco da Ficha Técnica: ${currentWeek.mepFocus}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                weekBrochures.forEachIndexed { idx, brochure ->
                    OutlinedButton(
                        onClick = {
                            atelierVm.openDidacticLibraryAxis(brochure.axisNumber)
                            onOpenTheoryTab()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(if (idx == 0) "current_week_brochure_v3_button" else "current_week_brochure_v3_extra_$idx")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SwissRed)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Consultar Biblioteca • Eixo ${brochure.axisNumber}: ${brochure.title.take(30)}",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // 80-min Phase Progress
                val phaseLabel = AtelierViewModel.STUDY_PHASE_LABELS.getOrElse(timerState.currentPhaseIndex) {
                    AtelierViewModel.STUDY_PHASE_LABELS[0]
                }
                val totalPhaseSec = AtelierViewModel.STUDY_PHASE_DURATIONS_SEC.getOrElse(timerState.currentPhaseIndex) { 600 }
                val progress = 1f - (timerState.remainingSeconds.toFloat() / totalPhaseSec.coerceAtLeast(1))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.study_timer_section_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = phaseLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatSecondsMmSs(timerState.remainingSeconds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (timerState.isRunning) SwissRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            color = SwissRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { atelierVm.toggleStudyTimer(currentWeek.weekNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (timerState.isRunning) PccAmber else SwissRed
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("start_study_block_button")
                            ) {
                                Icon(
                                    imageVector = if (timerState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_timer_toggle)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (timerState.isRunning) "Pausar Bloco" else stringResource(R.string.start_study_block),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { atelierVm.advanceOrResetStudyPhase(currentWeek.weekNumber, resetAll = false) },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("next_study_phase_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Avançar etapa do bloco de estudo"
                                )
                            }
                            OutlinedButton(
                                onClick = { atelierVm.advanceOrResetStudyPhase(currentWeek.weekNumber, resetAll = true) },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("reset_study_phase_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reiniciar bloco de 80 min"
                                )
                            }
                        }
                    }
                }

                // Player de Áudio Nativo (Narração pt-BR) do Eixo Atual e da Aula da Semana
                val activeAxisBrochure = remember(currentWeek.axisId) {
                    DidacticLibraryLoader.findAxis(currentWeek.axisId)
                }
                val axisSourceId = "axis_${activeAxisBrochure.axisNumber}"
                val weekSourceId = "week_${currentWeek.weekNumber}"
                val activeDashboardAudioId = if (narrationState.activeSourceId == weekSourceId) weekSourceId else axisSourceId

                AudioNarrationPlayerCard(
                    sourceId = activeDashboardAudioId,
                    title = if (activeDashboardAudioId == weekSourceId) {
                        "Narração da Aula • Semana S${"%02d".format(currentWeek.weekNumber)} (5 Etapas)"
                    } else {
                        "Ouvir Eixo ${activeAxisBrochure.axisNumber} • ${activeAxisBrochure.title}"
                    },
                    subtitle = if (activeDashboardAudioId == weekSourceId) {
                        "Roteiro completo dos 80 minutos narrado em português (pt-BR) para estudo de bancada"
                    } else {
                        "Áudio narrado em pt-BR das ${activeAxisBrochure.sections.size} seções do Eixo ${activeAxisBrochure.axisNumber} (sem downloads)"
                    },
                    narrationState = narrationState,
                    onTogglePlayPause = {
                        if (activeDashboardAudioId == weekSourceId) {
                            audioNarrator.togglePlayPause(
                                fallbackSourceId = weekSourceId,
                                fallbackTitle = "Semana S${"%02d".format(currentWeek.weekNumber)} • ${currentWeek.title}",
                                fallbackSegmentsProvider = { KitchenAudioNarrator.buildWeekLessonSegments(currentWeek) }
                            )
                        } else {
                            audioNarrator.togglePlayPause(
                                fallbackSourceId = axisSourceId,
                                fallbackTitle = "Eixo ${activeAxisBrochure.axisNumber} • ${activeAxisBrochure.title}",
                                fallbackSegmentsProvider = { KitchenAudioNarrator.buildAxisSegments(activeAxisBrochure) }
                            )
                        }
                    },
                    onSkipPrevious = { audioNarrator.skipPrevious() },
                    onSkipNext = { audioNarrator.skipNext() },
                    onStop = { audioNarrator.stop() },
                    onSelectSpeed = { audioNarrator.setSpeechRate(it) },
                    secondaryActionLabel = if (activeDashboardAudioId == weekSourceId) {
                        "Trocar para Áudio Completo do Eixo ${activeAxisBrochure.axisNumber}"
                    } else {
                        "Ouvir Roteiro da Aula S${"%02d".format(currentWeek.weekNumber)} (5 Etapas)"
                    },
                    onSecondaryAction = {
                        if (activeDashboardAudioId == weekSourceId) {
                            audioNarrator.playSegments(
                                sourceId = axisSourceId,
                                title = "Eixo ${activeAxisBrochure.axisNumber} • ${activeAxisBrochure.title}",
                                segments = KitchenAudioNarrator.buildAxisSegments(activeAxisBrochure)
                            )
                        } else {
                            audioNarrator.playSegments(
                                sourceId = weekSourceId,
                                title = "Semana S${"%02d".format(currentWeek.weekNumber)} • ${currentWeek.title}",
                                segments = KitchenAudioNarrator.buildWeekLessonSegments(currentWeek)
                            )
                        }
                    }
                )

                // Week Recipes & Badges
                Text(
                    text = "PREPAROS & CLÁSSICOS DA SEMANA",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                weekRecipes.forEach { recipe ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRecipeKitchen(recipe.id) }
                            .testTag("dashboard_recipe_${recipe.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (recipe.cantonCode.isNotBlank()) {
                                    TagPill("CANTÃO ${recipe.cantonCode}", SwissRedContainer, OnSwissRedContainer)
                                }
                                if (recipe.isSwissmilkClassic) {
                                    val smNum = recipe.swissmilkNum?.let { " #$it" }.orEmpty()
                                    TagPill("SWISSMILK$smNum", InfoBlueBg, InfoBlue)
                                }
                                if (recipe.isChefkochV4) {
                                    TagPill("CHEFKOCH PASSO A PASSO", PccAmberBg, PccAmber)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        recipe.pccTargetSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "Abrir Receita e MEP",
                                    tint = SwissRed
                                )
                            }
                        }
                    }
                }

                // Galeria Rápida das Últimas Fotos da Semana (v5)
                Text(
                    text = "GALERIA RÁPIDA DA SEMANA S${"%02d".format(currentWeek.weekNumber)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (weekQuickPhotos.isEmpty()) {
                    OutlinedButton(
                        onClick = onOpenCommunityTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dashboard_quick_gallery_empty_cta")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SwissRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Nenhuma foto nesta semana — Registrar produção com CameraX")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("dashboard_quick_gallery_row"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        weekQuickPhotos.forEach { (caption, b64) ->
                            val bmp = remember(b64) {
                                runCatching {
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }.getOrNull()
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { onOpenCommunityTab() }
                            ) {
                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = caption,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(84.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Text(
                                        text = caption,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct Shortcuts to the Native In-App Didactic Library (6 Eixos — Zero Downloads)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "BIBLIOTECA DIDÁTICA • CONSULTA NATIVA DOS 6 EIXOS NO APP",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CurriculumWeeksAndGuides.brochures.forEach { brochure ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (brochure.axisNumber == currentWeek.axisId) SwissRed else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            atelierVm.openDidacticLibraryAxis(brochure.axisNumber)
                            onOpenTheoryTab()
                        }
                        .testTag("brochure_shortcut_${brochure.axisNumber}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SwissRed
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(brochure.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Eixo ${brochure.axisNumber} • Leitura nativa no app (tabelas, PCCs e cartões)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = InfoBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatChip(
    label: String,
    value: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}
