package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.audio.AudioNarrationState
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioNarrationPlayerCard(
    sourceId: String,
    title: String,
    subtitle: String,
    narrationState: AudioNarrationState,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onStop: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val isThisSourceActive = narrationState.activeSourceId == sourceId
    val isPlayingThis = isThisSourceActive && narrationState.isPlaying
    val isPausedThis = isThisSourceActive && narrationState.isPaused
    val progress = if (isThisSourceActive && narrationState.totalSegments > 0) {
        (narrationState.currentSegmentIndex + 1).toFloat() / narrationState.totalSegments.toFloat()
    } else {
        0f
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = InfoBlueBg.copy(alpha = 0.45f)),
        border = BorderStroke(1.5.dp, if (isPlayingThis) SwissRed else InfoBlue),
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_card_$sourceId")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.cd_audio_narration),
                        tint = if (isPlayingThis) SwissRed else InfoBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    TagPill(
                        text = if (isPlayingThis) "NARRAÇÃO ATIVA (${narrationState.resolvedLanguageLabel})" else "ÁUDIO DE BANCADA • ${narrationState.resolvedLanguageLabel}",
                        containerColor = if (isPlayingThis) PccGreenBg else InfoBlueBg,
                        contentColor = if (isPlayingThis) PccGreen else InfoBlue
                    )
                }
                if (isThisSourceActive && narrationState.totalSegments > 0) {
                    Text(
                        text = "${narrationState.currentSegmentIndex + 1}/${narrationState.totalSegments}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = InfoBlue
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isThisSourceActive && narrationState.currentSectionTitle.isNotBlank()) {
                        narrationState.currentSectionTitle
                    } else {
                        subtitle
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isThisSourceActive && narrationState.currentSegmentPreview.isNotBlank()) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = if (isPlayingThis) SwissRed else InfoBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                Text(
                    text = "\"${narrationState.currentSegmentPreview}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (narrationState.isVolumeMutedWarning && isPlayingThis) {
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

            if (!narrationState.engineError.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccDangerBg),
                    border = BorderStroke(1.dp, PccDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = narrationState.engineError,
                        style = MaterialTheme.typography.labelSmall,
                        color = PccDanger,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onTogglePlayPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingThis) PccAmber else InfoBlue
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("audio_play_pause_$sourceId")
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_audio_toggle)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isPlayingThis -> "Pausar Áudio"
                            isPausedThis -> "Retomar Áudio"
                            else -> "Ouvir em Áudio"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isThisSourceActive) {
                    OutlinedButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("audio_prev_$sourceId")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Trecho anterior")
                    }
                    OutlinedButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("audio_next_$sourceId")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Próximo trecho")
                    }
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("audio_stop_$sourceId")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Parar narração")
                    }
                }
            }

            if (isThisSourceActive) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.85f to "0,85×", 1.0f to "1,0×", 1.25f to "1,25×", 1.5f to "1,5×").forEach { (rate, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(narrationState.speechRate - rate) < 0.05f,
                            onClick = { onSelectSpeed(rate) },
                            label = { Text(label, fontFamily = JetBrainsMonoFamily) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("audio_secondary_$sourceId")
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = SwissRed)
                    Spacer(Modifier.width(6.dp))
                    Text(secondaryActionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
