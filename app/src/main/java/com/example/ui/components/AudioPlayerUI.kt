package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.audio.CoursePodcastEpisode
import com.example.core.audio.OfflinePodcastState
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.PureSurface
import com.example.ui.theme.Slate200
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AudioPlayerViewModel
import kotlin.math.abs

private val DeepStudioCharcoal = Color(0xFF14181E)
private val StudioElevatedSurface = Color(0xFF252C36)

/**
 * Componente Compose `AudioPlayerUI` conectado ao `AudioPlayerViewModel`.
 *
 * Oferece barra de controles persistente (Play/Pause, Seek interativo direto na barra,
 * retrocesso de 15 s e modo compacto) ancorada em todas as telas do aplicativo, além de
 * folha modal expandida com playlist completa dos arquivos offline da pasta `assets`.
 */
@Composable
fun AudioPlayerUI(
    viewModel: AudioPlayerViewModel,
    currentCycleNumber: Int,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val chromeState by viewModel.chromeState.collectAsStateWithLifecycle()

    AudioPlayerUI(
        currentCycleNumber = currentCycleNumber,
        playerState = playerState,
        isMinimized = chromeState.isMiniBarMinimized,
        isSheetExpanded = chromeState.isSheetExpanded,
        onTogglePlayPause = { episode -> viewModel.togglePlayPause(episode) },
        onSelectAndPlay = { episode -> viewModel.playEpisode(episode) },
        onSeekTo = { positionMs, episodeId -> viewModel.seekTo(positionMs, episodeId) },
        onSkipBy = { deltaMs -> viewModel.skipBy(deltaMs) },
        onPlayPrevious = { viewModel.playPrevious(currentCycleNumber) },
        onPlayNext = { viewModel.playNext(currentCycleNumber) },
        onSelectSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
        onStopAndReset = { viewModel.stopAndReset() },
        onExpandSheet = { viewModel.expandSheet() },
        onDismissSheet = { viewModel.collapseSheet() },
        onSetMinimized = { minimized -> viewModel.setMiniBarMinimized(minimized) },
        modifier = modifier
    )
}

/**
 * Versão stateless de `AudioPlayerUI` para reutilização, testes e desacoplamento de UI.
 */
@Composable
fun AudioPlayerUI(
    currentCycleNumber: Int,
    playerState: OfflinePodcastState,
    isMinimized: Boolean,
    isSheetExpanded: Boolean,
    onTogglePlayPause: (CoursePodcastEpisode) -> Unit,
    onSelectAndPlay: (CoursePodcastEpisode) -> Unit,
    onSeekTo: (positionMs: Int, targetEpisodeId: String) -> Unit,
    onSkipBy: (Int) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onStopAndReset: () -> Unit,
    onExpandSheet: () -> Unit,
    onDismissSheet: () -> Unit,
    onSetMinimized: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = playerState.displayEpisodeForCycle(currentCycleNumber) ?: return
    val isActiveEpisode = playerState.activeEpisodeId == episode.id
    val isPlaying = isActiveEpisode && playerState.isPlaying
    val effectiveDurationMs = when {
        isActiveEpisode && playerState.durationMs > 0 -> playerState.durationMs
        else -> playerState.episodeDurationsMs[episode.id] ?: 0
    }
    val effectivePositionMs = when {
        isActiveEpisode -> playerState.positionMs
        else -> playerState.savedPositionsMs[episode.id] ?: 0
    }

    val targetFraction = if (effectiveDurationMs > 0) {
        (effectivePositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "audio_player_ui_progress"
    )

    val studioGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(DeepStudioCharcoal, AtelierCharcoal, Color(0xFF222933))
        )
    }

    Surface(
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_ui_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(studioGradient)
        ) {
            // Linha superior de progresso contínuo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(PureSurface.copy(alpha = 0.14f))
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(if (isPlaying) SwissRed else PccAmber)
                    )
                }
            }

            AnimatedContent(
                targetState = isMinimized,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium))
                },
                label = "audio_player_ui_bar_mode"
            ) { compact ->
                if (compact) {
                    AudioPlayerMinimizedStrip(
                        episode = episode,
                        isPlaying = isPlaying,
                        positionMs = effectivePositionMs,
                        durationMs = effectiveDurationMs,
                        onTogglePlayPause = { onTogglePlayPause(episode) },
                        onRestore = { onSetMinimized(false) },
                        onExpandSheet = onExpandSheet
                    )
                } else {
                    AudioPlayerPersistentBarContent(
                        episode = episode,
                        isPlaying = isPlaying,
                        isActive = isActiveEpisode,
                        positionMs = effectivePositionMs,
                        durationMs = effectiveDurationMs,
                        speed = playerState.playbackSpeed,
                        onTogglePlayPause = { onTogglePlayPause(episode) },
                        onSeekTo = { posMs -> onSeekTo(posMs, episode.id) },
                        onSkipBack = { onSkipBy(-15_000) },
                        onExpandSheet = onExpandSheet,
                        onMinimize = { onSetMinimized(true) }
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = PureSurface.copy(alpha = 0.08f))
        }
    }

    if (isSheetExpanded) {
        AudioPlayerExpandedSheet(
            episode = episode,
            playerState = playerState,
            onDismiss = onDismissSheet,
            onSelectAndPlay = onSelectAndPlay,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSkipBy = onSkipBy,
            onPlayPrevious = onPlayPrevious,
            onPlayNext = onPlayNext,
            onSelectSpeed = onSelectSpeed,
            onStopAndReset = onStopAndReset
        )
    }
}

@Composable
private fun AudioPlayerPersistentBarContent(
    episode: CoursePodcastEpisode,
    isPlaying: Boolean,
    isActive: Boolean,
    positionMs: Int,
    durationMs: Int,
    speed: Float,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkipBack: () -> Unit,
    onExpandSheet: () -> Unit,
    onMinimize: () -> Unit
) {
    var isScrubbing by remember(episode.id) { mutableStateOf(false) }
    var scrubMs by remember(episode.id) { mutableFloatStateOf(positionMs.toFloat()) }
    val displayPosMs = if (isScrubbing) scrubMs.toInt() else positionMs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandSheet)
                .padding(start = 12.dp, end = 6.dp)
                .testTag("audio_player_expand_area")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPlaying) SwissRed else StudioElevatedSurface)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                    contentDescription = stringResource(R.string.cd_podcast_audio),
                    tint = PureSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = episode.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PureSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = stringResource(R.string.persistent_player_offline_ready),
                        tint = if (isPlaying) PccGreenBg else Slate200.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                val statusSubtitle = buildString {
                    append(episode.weeksLabel)
                    append(" • ")
                    append(if (isPlaying) "${formatSpeedLabel(speed)} OFFLINE" else "OFFLINE")
                }
                Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = if (isPlaying) SwissRedContainer else Slate200,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActive) {
                IconButton(
                    onClick = onSkipBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_rewind_15s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = stringResource(R.string.cd_podcast_rewind),
                        tint = PureSurface
                    )
                }
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) PccAmber else SwissRed,
                    contentColor = PureSurface
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("audio_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.cd_podcast_toggle)
                )
            }

            IconButton(
                onClick = onExpandSheet,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("audio_player_expand_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = stringResource(R.string.cd_persistent_player_expand),
                    tint = Slate200
                )
            }

            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("audio_player_minimize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.cd_persistent_player_minimize),
                    tint = Slate200.copy(alpha = 0.75f)
                )
            }
        }

        // Controle persistente de Seek diretamente acessível na barra fixa
        if (durationMs > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = formatAudioMmSs(displayPosMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = PureSurface
                )
                Slider(
                    value = displayPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = {
                        isScrubbing = true
                        scrubMs = it
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        onSeekTo(scrubMs.toInt())
                    },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isPlaying) SwissRed else PccAmber,
                        activeTrackColor = if (isPlaying) SwissRed else PccAmber,
                        inactiveTrackColor = PureSurface.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("audio_player_seek_slider")
                )
                Text(
                    text = formatAudioMmSs(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = Slate200
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerMinimizedStrip(
    episode: CoursePodcastEpisode,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onTogglePlayPause: () -> Unit,
    onRestore: () -> Unit,
    onExpandSheet: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("audio_player_minimized_strip")
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isPlaying) SwissRed else PccAmber)
        )
        Text(
            text = "${episode.shortLabel} • ${formatAudioMmSs(positionMs)}${if (durationMs > 0) " / ${formatAudioMmSs(durationMs)}" else ""}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = PureSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("audio_player_minimized_play_pause")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_podcast_toggle),
                tint = if (isPlaying) PccAmber else SwissRed
            )
        }
        IconButton(
            onClick = onExpandSheet,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("audio_player_minimized_expand")
        ) {
            Icon(
                imageVector = Icons.Default.ExpandLess,
                contentDescription = stringResource(R.string.cd_persistent_player_restore),
                tint = Slate200
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AudioPlayerExpandedSheet(
    episode: CoursePodcastEpisode,
    playerState: OfflinePodcastState,
    onDismiss: () -> Unit,
    onSelectAndPlay: (CoursePodcastEpisode) -> Unit,
    onTogglePlayPause: (CoursePodcastEpisode) -> Unit,
    onSeekTo: (positionMs: Int, targetEpisodeId: String) -> Unit,
    onSkipBy: (Int) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onStopAndReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isActiveEpisode = playerState.activeEpisodeId == episode.id
    val isPlaying = isActiveEpisode && playerState.isPlaying

    var isScrubbing by remember(episode.id) { mutableStateOf(false) }
    var scrubValueMs by remember(episode.id) { mutableFloatStateOf(0f) }

    val durationMs = when {
        isActiveEpisode && playerState.durationMs > 0 -> playerState.durationMs
        else -> playerState.episodeDurationsMs[episode.id] ?: 0
    }
    val displayPosMs = when {
        isScrubbing -> scrubValueMs.toInt()
        isActiveEpisode -> playerState.positionMs
        else -> playerState.savedPositionsMs[episode.id] ?: 0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepStudioCharcoal,
        contentColor = PureSurface,
        modifier = Modifier.testTag("audio_player_expanded_sheet")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagPill(
                        text = stringResource(R.string.persistent_player_badge),
                        containerColor = if (isPlaying) PccGreenBg else SwissRedContainer,
                        contentColor = if (isPlaying) PccGreen else OnSwissRedContainer
                    )
                    Text(
                        text = "${playerState.availableEpisodes.size} OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = Slate200
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_persistent_player_close_sheet),
                        tint = Slate200
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                border = BorderStroke(
                    width = if (isPlaying) 2.dp else 1.dp,
                    color = if (isPlaying) SwissRed else PureSurface.copy(alpha = 0.16f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = episode.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = SwissRedContainer
                        )
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = stringResource(R.string.persistent_player_offline_ready),
                            tint = PccGreenBg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureSurface
                    )
                    Text(
                        text = episode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200
                    )
                    Text(
                        text = stringResource(R.string.persistent_player_storage_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate200.copy(alpha = 0.78f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Slider(
                    value = if (durationMs > 0) displayPosMs.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
                    onValueChange = {
                        if (durationMs > 0) {
                            isScrubbing = true
                            scrubValueMs = it
                        }
                    },
                    onValueChangeFinished = {
                        if (durationMs > 0) {
                            isScrubbing = false
                            onSeekTo(scrubValueMs.toInt(), episode.id)
                        }
                    },
                    enabled = durationMs > 0,
                    valueRange = 0f..(durationMs.coerceAtLeast(1)).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = SwissRed,
                        activeTrackColor = SwissRed,
                        inactiveTrackColor = PureSurface.copy(alpha = 0.22f),
                        disabledThumbColor = Slate200,
                        disabledActiveTrackColor = PccAmber.copy(alpha = 0.65f),
                        disabledInactiveTrackColor = PureSurface.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_sheet_seek_slider")
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatAudioMmSs(displayPosMs),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = PureSurface
                    )
                    Text(
                        text = if (durationMs > 0) formatAudioMmSs(durationMs) else "--:--",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = Slate200
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onPlayPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_sheet_prev_episode")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_persistent_player_previous),
                        tint = PureSurface
                    )
                }

                IconButton(
                    onClick = { onSkipBy(-15_000) },
                    enabled = isActiveEpisode,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_sheet_rewind_15s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = stringResource(R.string.cd_podcast_rewind),
                        tint = if (isActiveEpisode) PureSurface else Slate200.copy(alpha = 0.4f)
                    )
                }

                FilledIconButton(
                    onClick = { onTogglePlayPause(episode) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) PccAmber else SwissRed,
                        contentColor = PureSurface
                    ),
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("audio_sheet_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_podcast_toggle),
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { onSkipBy(15_000) },
                    enabled = isActiveEpisode,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_sheet_forward_15s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = stringResource(R.string.cd_podcast_forward),
                        tint = if (isActiveEpisode) PureSurface else Slate200.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = onPlayNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_sheet_next_episode")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.cd_persistent_player_next),
                        tint = PureSurface
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0.85f to "0,85×", 1.0f to "1,0×", 1.25f to "1,25×", 1.5f to "1,5×").forEach { (speed, label) ->
                    val selected = abs(playerState.playbackSpeed - speed) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSpeed(speed) },
                        label = {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StudioElevatedSurface,
                            labelColor = Slate200,
                            selectedContainerColor = SwissRedContainer,
                            selectedLabelColor = OnSwissRedContainer
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("audio_sheet_speed_${label.replace(",", "_")}")
                    )
                }

                if (isActiveEpisode || displayPosMs > 0) {
                    OutlinedButton(
                        onClick = onStopAndReset,
                        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSurface),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("audio_sheet_stop_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_podcast_stop),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.persistent_player_stop_reset))
                    }
                }
            }

            if (playerState.isVolumeMutedWarning && isPlaying) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccAmberBg),
                    border = BorderStroke(1.dp, PccAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.audio_volume_muted_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = PccAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!playerState.errorMessage.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccDangerBg),
                    border = BorderStroke(1.dp, PccDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = playerState.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = PccDanger,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            HorizontalDivider(color = PureSurface.copy(alpha = 0.12f))

            Text(
                text = stringResource(R.string.persistent_player_playlist_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PureSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playerState.availableEpisodes.forEach { item ->
                    val isItemActive = playerState.activeEpisodeId == item.id
                    val isItemPlaying = isItemActive && playerState.isPlaying
                    val itemDurationMs = playerState.episodeDurationsMs[item.id] ?: 0
                    val savedPosMs = if (isItemActive) {
                        playerState.positionMs
                    } else {
                        playerState.savedPositionsMs[item.id] ?: 0
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isItemActive) Color(0xFF2E2226) else StudioElevatedSurface
                        ),
                        border = BorderStroke(
                            width = if (isItemActive) 1.5.dp else 1.dp,
                            color = if (isItemActive) SwissRed else PureSurface.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAndPlay(item) }
                            .testTag("audio_playlist_item_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isItemPlaying) SwissRed else PureSurface.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = if (isItemPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_podcast_toggle),
                                    tint = PureSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.shortLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PureSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (savedPosMs > 2_000) {
                                    Text(
                                        text = stringResource(
                                            R.string.persistent_player_resume_at,
                                            formatAudioMmSs(savedPosMs)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = PccAmberBg
                                    )
                                }
                            }

                            Text(
                                text = if (itemDurationMs > 0) formatAudioMmSs(itemDurationMs) else item.weeksLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (isItemActive) SwissRedContainer else Slate200
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatAudioMmSs(millis: Int): String {
    val totalSec = (millis / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}

private fun formatSpeedLabel(speed: Float): String {
    return when {
        abs(speed - 0.85f) < 0.05f -> "0,85×"
        abs(speed - 1.25f) < 0.05f -> "1,25×"
        abs(speed - 1.5f) < 0.05f -> "1,5×"
        else -> "1,0×"
    }
}
