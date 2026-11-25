package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
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
private const val CHECKPOINT_SAVE_INTERVAL_MS = 5_000L
private const val PREFS_NAME = "atelier_offline_audio_prefs"
private const val KEY_LAST_EPISODE_ID = "last_episode_id"
private const val KEY_PLAYBACK_SPEED = "playback_speed"
private const val KEY_POS_PREFIX = "pos_"
private const val KEY_DUR_PREFIX = "dur_"

data class CoursePodcastEpisode(
    val id: String,
    val filePrefix: String,
    val cycleNumber: Int?, // null = Visão Geral (24 semanas) ou lição avulsa; 1..4 = Ciclos 1..4
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
    val lastEpisodeId: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val savedPositionsMs: Map<String, Int> = emptyMap(),
    val episodeDurationsMs: Map<String, Int> = emptyMap(),
    val isVolumeMutedWarning: Boolean = false,
    val errorMessage: String? = null
) {
    val activeEpisode: CoursePodcastEpisode?
        get() = availableEpisodes.find { it.id == activeEpisodeId }

    fun displayEpisodeForCycle(cycleNumber: Int): CoursePodcastEpisode? {
        return activeEpisode
            ?: availableEpisodes.find { it.id == lastEpisodeId }
            ?: availableEpisodes.find { it.cycleNumber == cycleNumber }
            ?: availableEpisodes.firstOrNull()
    }
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
 * Engine de áudio nativo 100% offline baseado em Android MediaPlayer + AssetFileDescriptor.
 * - Zero dependência de internet, zero custo de nuvem/Firestore, zero download externo.
 * - Suporta os 5 episódios oficiais da Jornada 24S e qualquer áudio local adicional em `assets/audio/`.
 * - Persiste automaticamente posição por episódio, último episódio ouvido e velocidade em SharedPreferences.
 * - Gerencia AudioFocus defensivamente para pausar em chamadas/interrupções de sistema.
 */
class OfflinePodcastPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onBeforeStart: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null
    private var lastCheckpointSaveRealtimeMs: Long = 0L
    private var pausedByTransientFocusLoss: Boolean = false
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        mainHandler.post {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    pausedByTransientFocusLoss = false
                    if (_state.value.isPlaying) pause(abandonFocus = false)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (_state.value.isPlaying) {
                        pausedByTransientFocusLoss = true
                        pause(abandonFocus = false)
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (pausedByTransientFocusLoss && _state.value.isPaused) {
                        pausedByTransientFocusLoss = false
                        resume()
                    }
                }
            }
        }
    }

    private val _state: MutableStateFlow<OfflinePodcastState>
    val state: StateFlow<OfflinePodcastState>

    init {
        val episodes = discoverAvailableEpisodes(appContext)
        val savedSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f).coerceIn(0.75f, 1.75f)
        val savedLastId = prefs.getString(KEY_LAST_EPISODE_ID, null)
            ?.takeIf { id -> episodes.any { it.id == id } }
        val savedPositions = episodes.associate { ep ->
            ep.id to prefs.getInt(KEY_POS_PREFIX + ep.id, 0).coerceAtLeast(0)
        }.filterValues { it > 0 }
        val cachedDurations = episodes.associate { ep ->
            ep.id to prefs.getInt(KEY_DUR_PREFIX + ep.id, 0).coerceAtLeast(0)
        }.filterValues { it > 0 }

        _state = MutableStateFlow(
            OfflinePodcastState(
                availableEpisodes = episodes,
                lastEpisodeId = savedLastId,
                playbackSpeed = savedSpeed,
                savedPositionsMs = savedPositions,
                episodeDurationsMs = cachedDurations
            )
        )
        state = _state.asStateFlow()
    }

    private val positionTicker = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                val pos = runCatching { mp.currentPosition }.getOrDefault(_state.value.positionMs).coerceAtLeast(0)
                val dur = runCatching { mp.duration }.getOrDefault(_state.value.durationMs).coerceAtLeast(0)
                val activeId = _state.value.activeEpisodeId

                val now = android.os.SystemClock.elapsedRealtime()
                if (activeId != null && now - lastCheckpointSaveRealtimeMs >= CHECKPOINT_SAVE_INTERVAL_MS) {
                    lastCheckpointSaveRealtimeMs = now
                    persistPosition(activeId, pos)
                }

                _state.update {
                    val updatedSaved = if (activeId != null) {
                        it.savedPositionsMs + (activeId to pos)
                    } else {
                        it.savedPositionsMs
                    }
                    it.copy(
                        positionMs = pos,
                        durationMs = if (dur > 0) dur else it.durationMs,
                        savedPositionsMs = updatedSaved,
                        isVolumeMutedWarning = isMediaVolumeZero()
                    )
                }
                mainHandler.postDelayed(this, POSITION_POLL_MS)
            }
        }
    }

    /**
     * Lê metadados de duração de cada asset em thread IO sem decodificar o áudio (somente header).
     */
    fun preloadDurations() {
        val episodes = _state.value.availableEpisodes
        if (episodes.isEmpty()) return

        val resolved = mutableMapOf<String, Int>()
        episodes.forEach { ep ->
            val existing = _state.value.episodeDurationsMs[ep.id] ?: 0
            if (existing > 0) {
                resolved[ep.id] = existing
                return@forEach
            }
            val durationMs = extractAssetDurationMs(ep.assetPath)
            if (durationMs > 0) {
                resolved[ep.id] = durationMs
                prefs.edit().putInt(KEY_DUR_PREFIX + ep.id, durationMs).apply()
            }
        }

        if (resolved.isNotEmpty()) {
            _state.update { current ->
                current.copy(episodeDurationsMs = current.episodeDurationsMs + resolved)
            }
        }
    }

    fun recommendedEpisodeForCycle(cycleNumber: Int): CoursePodcastEpisode? {
        val episodes = _state.value.availableEpisodes
        return episodes.find { it.cycleNumber == cycleNumber } ?: episodes.firstOrNull()
    }

    fun playEpisode(episode: CoursePodcastEpisode, resumeFromSaved: Boolean = true) {
        onBeforeStart?.invoke()
        pausedByTransientFocusLoss = false

        // Salva checkpoint do episódio anterior antes de trocar
        saveCurrentPositionCheckpoint()
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

            val duration = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(0)
            if (duration > 0) {
                prefs.edit().putInt(KEY_DUR_PREFIX + episode.id, duration).apply()
            }

            val savedPos = if (resumeFromSaved) {
                (_state.value.savedPositionsMs[episode.id] ?: prefs.getInt(KEY_POS_PREFIX + episode.id, 0))
                    .coerceAtLeast(0)
            } else {
                0
            }
            val initialPos = if (duration > 0 && savedPos in 2_000..(duration - 4_000).coerceAtLeast(2_000)) {
                runCatching { mp.seekTo(savedPos) }
                savedPos
            } else {
                0
            }

            val targetSpeed = _state.value.playbackSpeed
            applySpeedIfSupported(mp, targetSpeed)

            mp.setOnCompletionListener {
                mainHandler.post {
                    stopTicker()
                    abandonAudioFocus()
                    val finalDur = runCatching { mp.duration }.getOrDefault(_state.value.durationMs).coerceAtLeast(0)
                    persistPosition(episode.id, 0)
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isPaused = false,
                            positionMs = finalDur,
                            durationMs = finalDur,
                            savedPositionsMs = it.savedPositionsMs - episode.id
                        )
                    }
                }
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra for ${episode.assetPath}")
                mainHandler.post {
                    stopTicker()
                    abandonAudioFocus()
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

            requestAudioFocus()
            mp.start()
            mediaPlayer = mp
            lastCheckpointSaveRealtimeMs = android.os.SystemClock.elapsedRealtime()
            prefs.edit().putString(KEY_LAST_EPISODE_ID, episode.id).apply()

            _state.update {
                it.copy(
                    activeEpisodeId = episode.id,
                    lastEpisodeId = episode.id,
                    isPlaying = true,
                    isPaused = false,
                    positionMs = initialPos,
                    durationMs = duration,
                    episodeDurationsMs = if (duration > 0) it.episodeDurationsMs + (episode.id to duration) else it.episodeDurationsMs,
                    isVolumeMutedWarning = isMediaVolumeZero(),
                    errorMessage = null
                )
            }
            startTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir episódio offline ${episode.assetPath}", e)
            abandonAudioFocus()
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
            playEpisode(targetEpisode, resumeFromSaved = true)
        }
    }

    fun playNextEpisode(currentCycleNumber: Int) {
        val episodes = _state.value.availableEpisodes
        if (episodes.isEmpty()) return
        val currentEp = _state.value.displayEpisodeForCycle(currentCycleNumber)
        val idx = episodes.indexOfFirst { it.id == currentEp?.id }
        val next = episodes[(idx + 1).mod(episodes.size)]
        playEpisode(next, resumeFromSaved = true)
    }

    fun playPreviousEpisode(currentCycleNumber: Int) {
        val episodes = _state.value.availableEpisodes
        if (episodes.isEmpty()) return
        val currentEp = _state.value.displayEpisodeForCycle(currentCycleNumber)
        val idx = episodes.indexOfFirst { it.id == currentEp?.id }
        val prev = episodes[(idx - 1).mod(episodes.size)]
        playEpisode(prev, resumeFromSaved = true)
    }

    fun pause() {
        pausedByTransientFocusLoss = false
        pause(abandonFocus = true)
    }

    private fun pause(abandonFocus: Boolean) {
        stopTicker()
        val mp = mediaPlayer ?: return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
            }
        }
        val pos = runCatching { mp.currentPosition }.getOrDefault(_state.value.positionMs).coerceAtLeast(0)
        val activeId = _state.value.activeEpisodeId
        if (activeId != null) {
            persistPosition(activeId, pos)
        }
        if (abandonFocus) {
            abandonAudioFocus()
        }
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = true,
                positionMs = pos,
                savedPositionsMs = if (activeId != null && pos > 0) it.savedPositionsMs + (activeId to pos) else it.savedPositionsMs
            )
        }
    }

    fun resume() {
        val mp = mediaPlayer
        if (mp == null) {
            _state.value.activeEpisode?.let { playEpisode(it, resumeFromSaved = true) }
            return
        }
        onBeforeStart?.invoke()
        pausedByTransientFocusLoss = false
        requestAudioFocus()
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
            _state.value.activeEpisode?.let { playEpisode(it, resumeFromSaved = true) }
        }
    }

    fun stop() {
        pausedByTransientFocusLoss = false
        stopTicker()
        abandonAudioFocus()
        val activeId = _state.value.activeEpisodeId
        if (activeId != null) {
            persistPosition(activeId, 0)
        }
        releaseMediaPlayer()
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                positionMs = 0,
                savedPositionsMs = if (activeId != null) it.savedPositionsMs - activeId else it.savedPositionsMs,
                errorMessage = null
            )
        }
    }

    fun playAssetPath(assetPath: String, resumeFromSaved: Boolean = false) {
        val normalized = assetPath.trim().removePrefix("/")
        val existing = _state.value.availableEpisodes.firstOrNull {
            it.assetPath.equals(normalized, ignoreCase = true) ||
                it.assetPath.endsWith("/$normalized", ignoreCase = true)
        }
        if (existing != null) {
            playEpisode(existing, resumeFromSaved = resumeFromSaved)
            return
        }
        val rawName = normalized.substringAfterLast('/').substringBeforeLast('.')
        val cleanLabel = rawName
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Lição em Áudio" }
        val adHocEpisode = CoursePodcastEpisode(
            id = "asset_${rawName.lowercase()}",
            filePrefix = rawName.lowercase(),
            cycleNumber = null,
            shortLabel = cleanLabel.take(20),
            badge = "ÁUDIO LOCAL • ASSETS",
            title = cleanLabel,
            subtitle = "Reprodução offline direta de assets/$normalized",
            weeksLabel = "Offline",
            assetPath = normalized
        )
        _state.update { current ->
            val updatedList = if (current.availableEpisodes.any { it.id == adHocEpisode.id }) {
                current.availableEpisodes
            } else {
                current.availableEpisodes + adHocEpisode
            }
            current.copy(availableEpisodes = updatedList)
        }
        playEpisode(adHocEpisode, resumeFromSaved = resumeFromSaved)
    }

    fun seekTo(positionMs: Int, targetEpisodeId: String? = null) {
        val current = _state.value
        val resolvedId = targetEpisodeId ?: current.activeEpisodeId ?: current.lastEpisodeId
        val maxDur = when {
            resolvedId != null && resolvedId == current.activeEpisodeId && current.durationMs > 0 -> current.durationMs
            resolvedId != null -> current.episodeDurationsMs[resolvedId] ?: current.durationMs
            else -> current.durationMs
        }.coerceAtLeast(0)
        val clamped = if (maxDur > 0) positionMs.coerceIn(0, maxDur) else positionMs.coerceAtLeast(0)

        val mp = mediaPlayer
        if (mp != null && (targetEpisodeId == null || targetEpisodeId == current.activeEpisodeId)) {
            runCatching { mp.seekTo(clamped) }
        }
        if (resolvedId != null) {
            persistPosition(resolvedId, clamped)
        }
        _state.update {
            val isTargetActive = resolvedId == null || resolvedId == it.activeEpisodeId
            it.copy(
                positionMs = if (isTargetActive) clamped else it.positionMs,
                savedPositionsMs = if (resolvedId != null) {
                    if (clamped > 0) it.savedPositionsMs + (resolvedId to clamped)
                    else it.savedPositionsMs - resolvedId
                } else {
                    it.savedPositionsMs
                }
            )
        }
    }

    fun skipBy(deltaMs: Int) {
        val currentPos = runCatching { mediaPlayer?.currentPosition }.getOrNull() ?: _state.value.positionMs
        seekTo(currentPos + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.75f)
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, clamped).apply()
        _state.update { it.copy(playbackSpeed = clamped) }
        val mp = mediaPlayer ?: return
        // Apply immediately only while playing; calling setPlaybackParams while paused may auto-start playback on Android.
        if (_state.value.isPlaying) {
            applySpeedIfSupported(mp, clamped)
        }
    }

    fun release() {
        saveCurrentPositionCheckpoint()
        stopTicker()
        abandonAudioFocus()
        releaseMediaPlayer()
    }

    private fun saveCurrentPositionCheckpoint() {
        val activeId = _state.value.activeEpisodeId ?: return
        val pos = runCatching { mediaPlayer?.currentPosition }.getOrNull() ?: _state.value.positionMs
        if (pos > 0) {
            persistPosition(activeId, pos)
        }
    }

    private fun persistPosition(episodeId: String, positionMs: Int) {
        prefs.edit().putInt(KEY_POS_PREFIX + episodeId, positionMs.coerceAtLeast(0)).apply()
    }

    private fun extractAssetDurationMs(assetPath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            appContext.assets.openFd(assetPath).use { afd ->
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Metadados de duração indisponíveis via openFd para $assetPath", e)
            0
        } finally {
            runCatching { retriever.release() }
        }
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

    private fun requestAudioFocus() {
        val mgr = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                focusRequest = req
                mgr.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                mgr.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        val mgr = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { mgr.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                mgr.abandonAudioFocus(audioFocusChangeListener)
            }
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

            val matchedPaths = mutableSetOf<String>()
            val curated = EPISODE_SPECS.mapNotNull { spec ->
                val matchedPath = candidatePaths.firstOrNull { path ->
                    val fileName = path.substringAfterLast('/').lowercase()
                    fileName.startsWith(spec.filePrefix)
                }
                matchedPath?.let { path ->
                    matchedPaths.add(path)
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

            // Também descobre automaticamente qualquer arquivo extra em `assets/audio/`
            val extras = audioDirFiles
                .filter { it !in matchedPaths }
                .filter { path -> path.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS }
                .sorted()
                .map { path ->
                    val rawName = path.substringAfterLast('/').substringBeforeLast('.')
                    val cleanLabel = rawName
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .trim()
                        .split(' ')
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                        .ifBlank { "Lição em Áudio" }
                    CoursePodcastEpisode(
                        id = "local_${rawName.lowercase()}",
                        filePrefix = rawName.lowercase(),
                        cycleNumber = null,
                        shortLabel = cleanLabel.take(20),
                        badge = "LIÇÃO LOCAL • ASSETS",
                        title = cleanLabel,
                        subtitle = "Áudio didático empacotado localmente em assets para estudo 100% offline",
                        weeksLabel = "Offline",
                        assetPath = path
                    )
                }

            return curated + extras
        }
    }
}
