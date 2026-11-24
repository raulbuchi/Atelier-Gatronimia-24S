package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.CoursePodcastEpisode
import com.example.core.audio.KitchenAudioNarrator
import com.example.core.audio.OfflinePodcastPlayer
import com.example.core.audio.OfflinePodcastState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersistentAudioUiState(
    val isSheetExpanded: Boolean = false,
    val isMiniBarMinimized: Boolean = false
)

/**
 * ViewModel dedicado de áudio persistente.
 * Sobrevive a rotações de tela, mudanças de configuração e troca entre todas as 6 abas do app,
 * mantendo a reprodução offline dos arquivos em `assets/` sem interrupção.
 */
class PersistentAudioViewModel(appContext: Context) : ViewModel() {

    val audioNarrator: KitchenAudioNarrator = KitchenAudioNarrator(appContext)
    val podcastPlayer: OfflinePodcastPlayer = OfflinePodcastPlayer(appContext)

    val podcastState: StateFlow<OfflinePodcastState> = podcastPlayer.state

    private val _uiState = MutableStateFlow(PersistentAudioUiState())
    val uiState: StateFlow<PersistentAudioUiState> = _uiState.asStateFlow()

    init {
        // Exclusão mútua entre narração TTS das brochuras e episódios Deep Dive offline
        audioNarrator.onBeforeStart = { podcastPlayer.pause() }
        podcastPlayer.onBeforeStart = { audioNarrator.stop() }

        viewModelScope.launch(Dispatchers.IO) {
            podcastPlayer.preloadDurations()
        }
    }

    fun playEpisode(episode: CoursePodcastEpisode, resumeFromSaved: Boolean = true) {
        podcastPlayer.playEpisode(episode, resumeFromSaved = resumeFromSaved)
    }

    fun togglePlayPause(episode: CoursePodcastEpisode) {
        podcastPlayer.togglePlayPause(episode)
    }

    fun seekTo(positionMs: Int) {
        podcastPlayer.seekTo(positionMs)
    }

    fun skipBy(deltaMs: Int) {
        podcastPlayer.skipBy(deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        podcastPlayer.setPlaybackSpeed(speed)
    }

    fun playNext(currentCycleNumber: Int) {
        podcastPlayer.playNextEpisode(currentCycleNumber)
    }

    fun playPrevious(currentCycleNumber: Int) {
        podcastPlayer.playPreviousEpisode(currentCycleNumber)
    }

    fun stopAndReset() {
        podcastPlayer.stop()
    }

    fun expandSheet() {
        _uiState.update { it.copy(isSheetExpanded = true, isMiniBarMinimized = false) }
    }

    fun collapseSheet() {
        _uiState.update { it.copy(isSheetExpanded = false) }
    }

    fun setMiniBarMinimized(minimized: Boolean) {
        _uiState.update { it.copy(isMiniBarMinimized = minimized) }
    }

    override fun onCleared() {
        audioNarrator.onBeforeStart = null
        podcastPlayer.onBeforeStart = null
        audioNarrator.release()
        podcastPlayer.release()
        super.onCleared()
    }
}
