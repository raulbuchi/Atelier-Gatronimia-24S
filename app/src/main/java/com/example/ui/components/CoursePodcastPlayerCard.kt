package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoursePodcastPlayerCard(
    currentCycleNumber: Int,
    podcastState: OfflinePodcastState,
    onSelectAndPlay: (CoursePodcastEpisode) -> Unit,
    onTogglePlayPause: (CoursePodcastEpisode) -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkipBy: (Int) -> Unit,
    onStop: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableEpisodes = podcastState.availableEpisodes
    if (availableEpisodes.isEmpty()) return

    val defaultEpisode = remember(currentCycleNumber, availableEpisodes) {
        availableEpisodes.find { it.cycleNumber == currentCycleNumber } ?: availableEpisodes.first()
    }
    var selectedEpisodeId by remember(currentCycleNumber, podcastState.activeEpisodeId, availableEpisodes) {
        mutableStateOf(
            when {
                podcastState.isPlaying || podcastState.isPaused -> podcastState.activeEpisodeId ?: defaultEpisode.id
                else -> defaultEpisode.id
            }
        )
    }

    val displayedEpisode = availableEpisodes.find { it.id == selectedEpisodeId } ?: defaultEpisode
    val isDisplayedActive = podcastState.activeEpisodeId == displayedEpisode.id
    val isPlayingDisplayed = isDisplayedActive && podcastState.isPlaying
    val isPausedDisplayed = isDisplayedActive && podcastState.isPaused

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValueMs by remember { mutableFloatStateOf(0f) }

    val durationMs = if (isDisplayedActive) podcastState.durationMs else 0
    val displayPosMs = when {
        isScrubbing -> scrubValueMs.toInt()
        isDisplayedActive -> podcastState.positionMs
        else -> 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AtelierCharcoal),
        border = BorderStroke(
            width = if (isPlayingDisplayed) 2.dp else 1.5.dp,
            color = if (isPlayingDisplayed) SwissRed else Slate200.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("course_podcast_player_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = stringResource(R.string.cd_podcast_audio),
                        tint = if (isPlayingDisplayed) SwissRed else PureSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    TagPill(
                        text = if (isPlayingDisplayed) {
                            stringResource(R.string.podcast_status_playing)
                        } else {
                            stringResource(R.string.podcast_status_offline)
                        },
                        containerColor = if (isPlayingDisplayed) PccGreenBg else SwissRedContainer,
                        contentColor = if (isPlayingDisplayed) PccGreen else OnSwissRedContainer
                    )
                }
                Text(
                    text = "${availableEpisodes.size}/5 OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = Slate200
                )
            }

            // Episode selector chips (only shows bundled episodes — zero dead ends)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableEpisodes.forEach { episode ->
                    val isSelected = episode.id == displayedEpisode.id
                    val isPlayingThisChip = podcastState.activeEpisodeId == episode.id && podcastState.isPlaying
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedEpisodeId = episode.id
                            onSelectAndPlay(episode)
                        },
                        label = {
                            Text(
                                text = if (isPlayingThisChip) "▶ ${episode.shortLabel}" else episode.shortLabel,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PureSurface.copy(alpha = 0.12f),
                            labelColor = PureSurface,
                            selectedContainerColor = SwissRed,
                            selectedLabelColor = PureSurface
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("podcast_episode_chip_${episode.id}")
                    )
                }
            }

            // Active Episode Title & Summary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayedEpisode.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = SwissRedContainer
                )
                Text(
                    text = displayedEpisode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PureSurface
                )
                Text(
                    text = displayedEpisode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate200,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Interactive Timeline & Timestamps
            if (isDisplayedActive && durationMs > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Slider(
                        value = displayPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                        onValueChange = {
                            isScrubbing = true
                            scrubValueMs = it
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeekTo(scrubValueMs.toInt())
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = SwissRed,
                            activeTrackColor = SwissRed,
                            inactiveTrackColor = PureSurface.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("podcast_seek_slider")
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatMillisMmSs(displayPosMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = PureSurface
                        )
                        Text(
                            text = formatMillisMmSs(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = Slate200
                        )
                    }
                }
            }

            if (podcastState.isVolumeMutedWarning && isPlayingDisplayed) {
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
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (!podcastState.errorMessage.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccDangerBg),
                    border = BorderStroke(1.dp, PccDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = podcastState.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = PccDanger,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Kitchen-Ergonomic Transport Bar (all touch targets >= 52 dp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDisplayedActive) {
                    OutlinedButton(
                        onClick = { onSkipBy(-15_000) },
                        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSurface),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("podcast_rewind_15s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = stringResource(R.string.cd_podcast_rewind)
                        )
                    }
                }

                Button(
                    onClick = { onTogglePlayPause(displayedEpisode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingDisplayed) PccAmber else SwissRed,
                        contentColor = PureSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("podcast_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlayingDisplayed) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_podcast_toggle)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isPlayingDisplayed -> stringResource(R.string.podcast_action_pause)
                            isPausedDisplayed -> stringResource(R.string.podcast_action_resume)
                            else -> stringResource(R.string.podcast_action_play)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDisplayedActive) {
                    OutlinedButton(
                        onClick = { onSkipBy(15_000) },
                        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSurface),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("podcast_forward_15s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = stringResource(R.string.cd_podcast_forward)
                        )
                    }
                    OutlinedButton(
                        onClick = onStop,
                        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureSurface),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("podcast_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_podcast_stop)
                        )
                    }
                }
            }

            // Playback Speed Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.85f to "0,85×", 1.0f to "1,0×", 1.25f to "1,25×", 1.5f to "1,5×").forEach { (speed, label) ->
                    val isSelectedSpeed = abs(podcastState.playbackSpeed - speed) < 0.05f
                    FilterChip(
                        selected = isSelectedSpeed,
                        onClick = { onSelectSpeed(speed) },
                        label = { Text(label, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PureSurface.copy(alpha = 0.1f),
                            labelColor = Slate200,
                            selectedContainerColor = SwissRedContainer,
                            selectedLabelColor = OnSwissRedContainer
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("podcast_speed_${label.replace(",", "_")}")
                    )
                }
            }
        }
    }
}

private fun formatMillisMmSs(millis: Int): String {
    val totalSec = (millis / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}
