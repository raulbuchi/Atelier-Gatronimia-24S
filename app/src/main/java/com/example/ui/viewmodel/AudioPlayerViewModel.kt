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

data class AudioPlayerChromeState(
    val isSheetExpanded: Boolean = false,
    val isMiniBarMinimized: Boolean = false
)

/**
 * ViewModel responsável por gerenciar a reprodução persistente de arquivos de áudio
 * empacotados na pasta `assets` (100% offline, sem internet e com zero custo em nuvem).
 *
 * Sobrevive a rotações de tela e à troca entre todas as abas do aplicativo,
 * mantendo controles sincronizados de play, pause, seek, salto de 15s, velocidade
 * e checkpoint automático de progresso em SharedPreferences.
 */
class AudioPlayerViewModel(appContext: Context) : ViewModel() {

    val audioNarrator: KitchenAudioNarrator = KitchenAudioNarrator(appContext)
    val podcastPlayer: OfflinePodcastPlayer = OfflinePodcastPlayer(appContext)

    val playerState: StateFlow<OfflinePodcastState> = podcastPlayer.state

    private val _chromeState = MutableStateFlow(AudioPlayerChromeState())
    val chromeState: StateFlow<AudioPlayerChromeState> = _chromeState.asStateFlow()

    init {
        // Exclusão mútua entre narração TTS das brochuras e reprodução de áudios locais em assets
        audioNarrator.onBeforeStart = { podcastPlayer.pause() }
        podcastPlayer.onBeforeStart = { audioNarrator.stop() }

        // Lê apenas cabeçalhos de duração dos arquivos em assets via Dispatchers.IO (sem decodificar)
        viewModelScope.launch(Dispatchers.IO) {
            podcastPlayer.preloadDurations()
        }
    }

    fun playAsset(assetPath: String, resumeFromSaved: Boolean = false) {
        podcastPlayer.playAssetPath(assetPath = assetPath, resumeFromSaved = resumeFromSaved)
    }

    fun playEpisode(episode: CoursePodcastEpisode, resumeFromSaved: Boolean = true) {
        podcastPlayer.playEpisode(episode = episode, resumeFromSaved = resumeFromSaved)
    }

    fun togglePlayPause(episode: CoursePodcastEpisode) {
        podcastPlayer.togglePlayPause(episode)
    }

    fun pause() {
        podcastPlayer.pause()
    }

    fun resume() {
        podcastPlayer.resume()
    }

    fun seekTo(positionMs: Int, targetEpisodeId: String? = null) {
        podcastPlayer.seekTo(positionMs = positionMs, targetEpisodeId = targetEpisodeId)
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
        _chromeState.update { it.copy(isSheetExpanded = true, isMiniBarMinimized = false) }
    }

    fun collapseSheet() {
        _chromeState.update { it.copy(isSheetExpanded = false) }
    }

    fun setMiniBarMinimized(minimized: Boolean) {
        _chromeState.update { it.copy(isMiniBarMinimized = minimized) }
    }

    override fun onCleared() {
        audioNarrator.onBeforeStart = null
        podcastPlayer.onBeforeStart = null
        audioNarrator.release()
        podcastPlayer.release()
        super.onCleared()
    }
}
