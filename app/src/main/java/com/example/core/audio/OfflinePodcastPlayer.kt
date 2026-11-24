package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream

private const val TAG = "OfflinePodcastPlayer"
private const val POSITION_POLL_MS = 500L

data class CoursePodcastEpisode(
    val id: String,
    val filePrefix: String,
    val cycleNumber: Int?, // null = Visão Geral (24 semanas); 1..4 = Ciclos 1..4
    val shortLabel: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val weeksLabel: String,
    val assetPath: String
)

data class OfflinePodcastState(
    val availableEpisodes: List<CoursePodcastEpisode> = emptyList(),
    val activeEpisodeId: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isVolumeMutedWarning: Boolean = false,
    val errorMessage: String? = null
) {
    val activeEpisode: CoursePodcastEpisode?
        get() = availableEpisodes.find { it.id == activeEpisodeId }
}

private data class EpisodeCatalogSpec(
    val id: String,
    val filePrefix: String,
    val cycleNumber: Int?,
    val shortLabel: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val weeksLabel: String
)

/**
 * Player de áudio nativo 100% offline baseado em Android MediaPlayer + AssetFileDescriptor.
 * - Zero dependência de internet, zero custo de nuvem/Firestore, zero download externo.
 * - Descobre automaticamente os arquivos de áudio empacotados em `assets/audio/` ou na raiz de `assets/`
 *   (incluindo arquivos anexados no AI Studio com sufixo de hash).
 * - Suporta scrubbing instantâneo, avanço/retrocesso de 15 s e controle de velocidade (0,85x a 1,5x).
 */
class OfflinePodcastPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    var onBeforeStart: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null

    private val _state = MutableStateFlow(
        OfflinePodcastState(availableEpisodes = discoverAvailableEpisodes(appContext))
    )
    val state: StateFlow<OfflinePodcastState> = _state.asStateFlow()

    private val positionTicker = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                val pos = runCatching { mp.currentPosition }.getOrDefault(_state.value.positionMs)
                val dur = runCatching { mp.duration }.getOrDefault(_state.value.durationMs).coerceAtLeast(0)
                _state.update {
                    it.copy(
                        positionMs = pos.coerceAtLeast(0),
                        durationMs = if (dur > 0) dur else it.durationMs,
                        isVolumeMutedWarning = isMediaVolumeZero()
                    )
                }
                mainHandler.postDelayed(this, POSITION_POLL_MS)
            }
        }
    }

    fun recommendedEpisodeForCycle(cycleNumber: Int): CoursePodcastEpisode? {
        val episodes = _state.value.availableEpisodes
        return episodes.find { it.cycleNumber == cycleNumber } ?: episodes.firstOrNull()
    }

    fun playEpisode(episode: CoursePodcastEpisode) {
        onBeforeStart?.invoke()
        stopTicker()
        releaseMediaPlayer()

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }

            setAssetDataSource(mp, episode)
            mp.prepare()

            val targetSpeed = _state.value.playbackSpeed
            applySpeedIfSupported(mp, targetSpeed)

            mp.setOnCompletionListener {
                mainHandler.post {
                    stopTicker()
                    val finalDur = runCatching { mp.duration }.getOrDefault(_state.value.durationMs)
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isPaused = false,
                            positionMs = finalDur.coerceAtLeast(0),
                            durationMs = finalDur.coerceAtLeast(0)
                        )
                    }
                }
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra for ${episode.assetPath}")
                mainHandler.post {
                    stopTicker()
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isPaused = false,
                            errorMessage = "Erro ao reproduzir áudio offline (código $what/$extra)."
                        )
                    }
                }
                true
            }

            mp.start()
            mediaPlayer = mp

            val duration = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(0)
            _state.update {
                it.copy(
                    activeEpisodeId = episode.id,
                    isPlaying = true,
                    isPaused = false,
                    positionMs = 0,
                    durationMs = duration,
                    isVolumeMutedWarning = isMediaVolumeZero(),
                    errorMessage = null
                )
            }
            startTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir episódio offline ${episode.assetPath}", e)
            releaseMediaPlayer()
            _state.update {
                it.copy(
                    activeEpisodeId = episode.id,
                    isPlaying = false,
                    isPaused = false,
                    errorMessage = "Não foi possível carregar o áudio offline: ${e.localizedMessage ?: "arquivo indisponível"}."
                )
            }
        }
    }

    fun togglePlayPause(targetEpisode: CoursePodcastEpisode) {
        val current = _state.value
        if (current.activeEpisodeId == targetEpisode.id && mediaPlayer != null) {
            if (current.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            playEpisode(targetEpisode)
        }
    }

    fun pause() {
        stopTicker()
        val mp = mediaPlayer ?: return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
            }
        }
        val pos = runCatching { mp.currentPosition }.getOrDefault(_state.value.positionMs)
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = true,
                positionMs = pos.coerceAtLeast(0)
            )
        }
    }

    fun resume() {
        val mp = mediaPlayer
        if (mp == null) {
            _state.value.activeEpisode?.let { playEpisode(it) }
            return
        }
        onBeforeStart?.invoke()
        runCatching {
            val dur = mp.duration.coerceAtLeast(0)
            if (dur > 0 && mp.currentPosition >= dur - 500) {
                mp.seekTo(0)
            }
            applySpeedIfSupported(mp, _state.value.playbackSpeed)
            mp.start()
        }.onSuccess {
            _state.update {
                it.copy(
                    isPlaying = true,
                    isPaused = false,
                    isVolumeMutedWarning = isMediaVolumeZero(),
                    errorMessage = null
                )
            }
            startTicker()
        }.onFailure { err ->
            Log.e(TAG, "Falha ao retomar reprodução", err)
            _state.value.activeEpisode?.let { playEpisode(it) }
        }
    }

    fun stop() {
        stopTicker()
        releaseMediaPlayer()
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                positionMs = 0,
                errorMessage = null
            )
        }
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        val maxDur = _state.value.durationMs.coerceAtLeast(0)
        val clamped = if (maxDur > 0) positionMs.coerceIn(0, maxDur) else positionMs.coerceAtLeast(0)
        runCatching { mp.seekTo(clamped) }
        _state.update { it.copy(positionMs = clamped) }
    }

    fun skipBy(deltaMs: Int) {
        val currentPos = runCatching { mediaPlayer?.currentPosition }.getOrNull() ?: _state.value.positionMs
        seekTo(currentPos + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.75f)
        _state.update { it.copy(playbackSpeed = clamped) }
        val mp = mediaPlayer ?: return
        // Apply immediately only while playing; calling setPlaybackParams while paused may auto-start playback on Android.
        if (_state.value.isPlaying) {
            applySpeedIfSupported(mp, clamped)
        }
    }

    fun release() {
        stopTicker()
        releaseMediaPlayer()
    }

    private fun setAssetDataSource(mp: MediaPlayer, episode: CoursePodcastEpisode) {
        try {
            appContext.assets.openFd(episode.assetPath).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        } catch (openFdError: Exception) {
            // Defensive fallback if an asset was compressed in a custom build variant
            Log.w(TAG, "openFd indisponível para ${episode.assetPath}; usando cache interno privado.", openFdError)
            val ext = episode.assetPath.substringAfterLast('.', "m4a")
            val cached = File(appContext.cacheDir, "offline_podcast_${episode.id}.$ext")
            if (!cached.exists() || cached.length() == 0L) {
                appContext.assets.open(episode.assetPath).use { input ->
                    FileOutputStream(cached).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            mp.setDataSource(cached.absolutePath)
        }
    }

    private fun applySpeedIfSupported(mp: MediaPlayer, speed: Float) {
        runCatching {
            val params: PlaybackParams = mp.playbackParams
            params.speed = speed
            mp.playbackParams = params
        }.onFailure { err ->
            Log.w(TAG, "Velocidade de reprodução ${speed}x não suportada pelo codec do dispositivo", err)
        }
    }

    private fun startTicker() {
        stopTicker()
        mainHandler.postDelayed(positionTicker, POSITION_POLL_MS)
    }

    private fun stopTicker() {
        mainHandler.removeCallbacks(positionTicker)
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
    }

    private fun isMediaVolumeZero(): Boolean {
        val mgr = audioManager ?: return false
        return runCatching { mgr.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }.getOrDefault(false)
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("m4a", "mp3", "ogg", "wav", "aac", "opus")

        private val EPISODE_SPECS = listOf(
            EpisodeCatalogSpec(
                id = "visao_geral_24s",
                filePrefix = "visao_geral_24s",
                cycleNumber = null,
                shortLabel = "Visão Geral (24S)",
                badge = "DEEP DIVE • JORNADA 24S",
                title = "Visão Geral em Áudio — A Jornada de 24 Semanas",
                subtitle = "Blocos de 80 min, SM-2, Ficha MEP/AVOR, Cortes, Acrilamida 175 °C, Sous-Vide e Gestão Chefkoch",
                weeksLabel = "Semanas 1 a 24"
            ),
            EpisodeCatalogSpec(
                id = "episodio_01_ciclo_1",
                filePrefix = "episodio_01_ciclo_1",
                cycleNumber = 1,
                shortLabel = "Ep. 1 • Ciclo 1",
                badge = "EPISÓDIO 1 • CICLO 1 (S1–S6)",
                title = "Episódio 1: Alicerces da Cozinha Profissional",
                subtitle = "AVOR, Cortes Schnittarten, Bateria de Panelas, Fundos Escuros, 5 Molhos-Mãe, Roux × Slurry e RDC 216",
                weeksLabel = "Semanas 1 a 6"
            ),
            EpisodeCatalogSpec(
                id = "episodio_02_ciclo_2",
                filePrefix = "episodio_02_ciclo_2",
                cycleNumber = 2,
                shortLabel = "Ep. 2 • Ciclo 2",
                badge = "EPISÓDIO 2 • CICLO 2 (S7–S12)",
                title = "Episódio 2: Calor, Maillard, Emulsões & Tradição Suíça",
                subtitle = "Maillard 140 °C, teto 175 °C (acrilamida), Rösti (sinérese), Fondue, Maionese, Berner Platte 82 °C e Insaporire",
                weeksLabel = "Semanas 7 a 12"
            ),
            EpisodeCatalogSpec(
                id = "episodio_03_ciclo_3",
                filePrefix = "episodio_03_ciclo_3",
                cycleNumber = 3,
                shortLabel = "Ep. 3 • Ciclo 3",
                badge = "EPISÓDIO 3 • CICLO 3 (S13–S18)",
                title = "Episódio 3: Panificação, Levain, Folhados, Confeitaria & Sous-Vide",
                subtitle = "Autólise, Zopf, Folhados (5 °C), Alfa-amilase, Metilcelulose, Temperagem Beta-V, Sous-Vide e Biomas BR",
                weeksLabel = "Semanas 13 a 18"
            ),
            EpisodeCatalogSpec(
                id = "episodio_04_ciclo_4",
                filePrefix = "episodio_04_ciclo_4",
                cycleNumber = 4,
                shortLabel = "Ep. 4 • Ciclo 4",
                badge = "EPISÓDIO 4 • CICLO 4 (S19–S24)",
                title = "Episódio 4: O Nível Chefkoch / Gestor",
                subtitle = "Umami, Teste PROP, WSET/SCA, IBA & Apéro, Fachrechnen/Direct Costing, Brigada 13 Estações e Nutrição Clínica",
                weeksLabel = "Semanas 19 a 24"
            )
        )

        fun discoverAvailableEpisodes(context: Context): List<CoursePodcastEpisode> {
            val assets = context.assets
            val audioDirFiles = runCatching { assets.list("audio").orEmpty().map { "audio/$it" } }
                .getOrDefault(emptyList())
            val rootAssetFiles = runCatching { assets.list("").orEmpty().toList() }
                .getOrDefault(emptyList())

            val candidatePaths = (audioDirFiles + rootAssetFiles).filter { path ->
                val ext = path.substringAfterLast('.', "").lowercase()
                ext in SUPPORTED_EXTENSIONS
            }

            return EPISODE_SPECS.mapNotNull { spec ->
                val matchedPath = candidatePaths.firstOrNull { path ->
                    val fileName = path.substringAfterLast('/').lowercase()
                    fileName.startsWith(spec.filePrefix)
                }
                matchedPath?.let { path ->
                    CoursePodcastEpisode(
                        id = spec.id,
                        filePrefix = spec.filePrefix,
                        cycleNumber = spec.cycleNumber,
                        shortLabel = spec.shortLabel,
                        badge = spec.badge,
                        title = spec.title,
                        subtitle = spec.subtitle,
                        weeksLabel = spec.weeksLabel,
                        assetPath = path
                    )
                }
            }
        }
    }
}
