package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.curriculum.DidacticAxisBrochure
import com.example.data.curriculum.DidacticBlock
import com.example.data.curriculum.DidacticSection
import com.example.domain.model.CourseWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

private const val TAG = "KitchenAudioNarrator"

data class NarrationSegment(
    val index: Int,
    val sectionTitle: String,
    val spokenText: String
)

data class AudioNarrationState(
    val isEngineReady: Boolean = false,
    val engineError: String? = null,
    val activeSourceId: String? = null,
    val activeTitle: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val totalSegments: Int = 0,
    val currentSectionTitle: String = "",
    val currentSegmentPreview: String = "",
    val speechRate: Float = 1.0f,
    val isVolumeMutedWarning: Boolean = false,
    val resolvedLanguageLabel: String = "pt-BR"
)

/**
 * Motor de narração em áudio 100% nativo (on-device TextToSpeech pt-BR).
 * - Zero latência de rede, zero consumo de tokens/Firestore, funciona offline na bancada.
 * - Fragmenta textos longos em segmentos naturais (~280 caracteres) para suporte imediato a
 *   Pausar, Retomar, Avançar/Voltar seção e controle de velocidade (0.85x a 1.5x).
 */
class KitchenAudioNarrator(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _state = MutableStateFlow(AudioNarrationState())
    val state: StateFlow<AudioNarrationState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var currentSegments: List<NarrationSegment> = emptyList()
    private var pendingStartAfterInit: (() -> Unit)? = null
    private var sessionToken: Long = 0L
    var onBeforeStart: (() -> Unit)? = null

    init {
        initEngine()
    }

    private fun initEngine() {
        try {
            tts = TextToSpeech(appContext) { status ->
                mainHandler.post {
                    if (status == TextToSpeech.SUCCESS) {
                        val engine = tts ?: return@post
                        val audioAttrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        engine.setAudioAttributes(audioAttrs)

                        val ptBr = Locale.forLanguageTag("pt-BR")
                        val ptGeneric = Locale.forLanguageTag("pt")
                        val resPtBr = engine.setLanguage(ptBr)
                        val resolvedLocaleLabel = when {
                            resPtBr != TextToSpeech.LANG_MISSING_DATA &&
                                resPtBr != TextToSpeech.LANG_NOT_SUPPORTED -> "pt-BR"
                            else -> {
                                val resPt = engine.setLanguage(ptGeneric)
                                if (resPt != TextToSpeech.LANG_MISSING_DATA &&
                                    resPt != TextToSpeech.LANG_NOT_SUPPORTED
                                ) {
                                    "pt"
                                } else {
                                    engine.setLanguage(Locale.getDefault())
                                    Locale.getDefault().toLanguageTag()
                                }
                            }
                        }

                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                // State updated synchronously in speakSegmentAt
                            }

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post {
                                    handleUtteranceFinished(utteranceId)
                                }
                            }

                            @Deprecated("Deprecated in Android API", ReplaceWith("onError(utteranceId, errorCode)"))
                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    handleUtteranceError(utteranceId, -1)
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                mainHandler.post {
                                    handleUtteranceError(utteranceId, errorCode)
                                }
                            }
                        })

                        _state.update {
                            it.copy(
                                isEngineReady = true,
                                engineError = null,
                                resolvedLanguageLabel = resolvedLocaleLabel
                            )
                        }
                        pendingStartAfterInit?.invoke()
                        pendingStartAfterInit = null
                    } else {
                        Log.e(TAG, "TextToSpeech init failed with status=$status")
                        _state.update {
                            it.copy(
                                isEngineReady = false,
                                isPlaying = false,
                                engineError = "Motor de voz do Android indisponível neste dispositivo/emulador (código $status)."
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct TextToSpeech", e)
            _state.update {
                it.copy(
                    isEngineReady = false,
                    engineError = "Não foi possível inicializar a narração em áudio: ${e.localizedMessage}"
                )
            }
        }
    }

    fun playSegments(
        sourceId: String,
        title: String,
        segments: List<NarrationSegment>,
        startIndex: Int = 0
    ) {
        if (segments.isEmpty()) return

        if (!_state.value.isEngineReady) {
            pendingStartAfterInit = { playSegments(sourceId, title, segments, startIndex) }
            if (tts == null) initEngine()
            _state.update {
                it.copy(
                    activeSourceId = sourceId,
                    activeTitle = title,
                    totalSegments = segments.size,
                    currentSegmentIndex = startIndex.coerceIn(0, segments.lastIndex),
                    currentSectionTitle = segments[startIndex.coerceIn(0, segments.lastIndex)].sectionTitle
                )
            }
            return
        }

        onBeforeStart?.invoke()
        currentSegments = segments
        val clamped = startIndex.coerceIn(0, segments.lastIndex)
        sessionToken += 1L
        val volumeMuted = isMediaVolumeZero()

        _state.update {
            it.copy(
                activeSourceId = sourceId,
                activeTitle = title,
                totalSegments = segments.size,
                currentSegmentIndex = clamped,
                isPlaying = true,
                isPaused = false,
                engineError = null,
                isVolumeMutedWarning = volumeMuted
            )
        }
        speakSegmentAt(clamped, sessionToken)
    }

    fun togglePlayPause(
        fallbackSourceId: String,
        fallbackTitle: String,
        fallbackSegmentsProvider: () -> List<NarrationSegment>
    ) {
        val current = _state.value
        if (current.activeSourceId == fallbackSourceId && currentSegments.isNotEmpty()) {
            if (current.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            playSegments(
                sourceId = fallbackSourceId,
                title = fallbackTitle,
                segments = fallbackSegmentsProvider(),
                startIndex = 0
            )
        }
    }

    fun pause() {
        sessionToken += 1L
        runCatching { tts?.stop() }
        _state.update { it.copy(isPlaying = false, isPaused = true) }
    }

    fun resume() {
        if (currentSegments.isEmpty()) return
        onBeforeStart?.invoke()
        val idx = _state.value.currentSegmentIndex.coerceIn(0, currentSegments.lastIndex)
        sessionToken += 1L
        val volumeMuted = isMediaVolumeZero()
        _state.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                isVolumeMutedWarning = volumeMuted,
                engineError = null
            )
        }
        speakSegmentAt(idx, sessionToken)
    }

    fun skipNext() {
        if (currentSegments.isEmpty()) return
        val next = (_state.value.currentSegmentIndex + 1).coerceAtMost(currentSegments.lastIndex)
        if (_state.value.isPlaying) {
            sessionToken += 1L
            speakSegmentAt(next, sessionToken)
        } else {
            val seg = currentSegments[next]
            _state.update {
                it.copy(
                    currentSegmentIndex = next,
                    currentSectionTitle = seg.sectionTitle,
                    currentSegmentPreview = seg.spokenText
                )
            }
        }
    }

    fun skipPrevious() {
        if (currentSegments.isEmpty()) return
        val prev = (_state.value.currentSegmentIndex - 1).coerceAtLeast(0)
        if (_state.value.isPlaying) {
            sessionToken += 1L
            speakSegmentAt(prev, sessionToken)
        } else {
            val seg = currentSegments[prev]
            _state.update {
                it.copy(
                    currentSegmentIndex = prev,
                    currentSectionTitle = seg.sectionTitle,
                    currentSegmentPreview = seg.spokenText
                )
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.75f, 1.75f)
        _state.update { it.copy(speechRate = clamped) }
        runCatching { tts?.setSpeechRate(clamped) }
        if (_state.value.isPlaying && currentSegments.isNotEmpty()) {
            val idx = _state.value.currentSegmentIndex.coerceIn(0, currentSegments.lastIndex)
            sessionToken += 1L
            speakSegmentAt(idx, sessionToken)
        }
    }

    fun stop() {
        sessionToken += 1L
        runCatching { tts?.stop() }
        _state.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                currentSegmentIndex = 0
            )
        }
    }

    fun release() {
        sessionToken += 1L
        pendingStartAfterInit = null
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun speakSegmentAt(index: Int, token: Long) {
        val engine = tts ?: return
        val seg = currentSegments.getOrNull(index) ?: return
        _state.update {
            it.copy(
                currentSegmentIndex = index,
                currentSectionTitle = seg.sectionTitle,
                currentSegmentPreview = seg.spokenText,
                isPlaying = true,
                isPaused = false
            )
        }
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.setSpeechRate(_state.value.speechRate)
        val utteranceId = "${token}_$index"
        val res = engine.speak(seg.spokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (res == TextToSpeech.ERROR) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    engineError = "Falha ao reproduzir narração no motor de voz do sistema."
                )
            }
        }
    }

    private fun handleUtteranceFinished(utteranceId: String?) {
        val parts = utteranceId?.split("_") ?: return
        val token = parts.getOrNull(0)?.toLongOrNull() ?: return
        val idx = parts.getOrNull(1)?.toIntOrNull() ?: return
        if (token != sessionToken || !_state.value.isPlaying) return

        val nextIdx = idx + 1
        if (nextIdx < currentSegments.size) {
            speakSegmentAt(nextIdx, token)
        } else {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isPaused = false,
                    currentSegmentIndex = 0
                )
            }
        }
    }

    private fun handleUtteranceError(utteranceId: String?, errorCode: Int) {
        val token = utteranceId?.substringBefore("_")?.toLongOrNull()
        if (token != null && token != sessionToken) return
        Log.w(TAG, "TTS utterance error code=$errorCode id=$utteranceId")
        _state.update {
            it.copy(
                isPlaying = false,
                engineError = "Erro na síntese de voz (código $errorCode). Verifique se o pacote de voz Português (Brasil) está ativo no Android."
            )
        }
    }

    private fun isMediaVolumeZero(): Boolean {
        return runCatching {
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1) == 0
        }.getOrDefault(false)
    }

    companion object {

        fun buildWeekLessonSegments(week: CourseWeek): List<NarrationSegment> {
            val rawPairs = mutableListOf<Pair<String, String>>()
            rawPairs.add(
                "Abertura • Semana ${week.weekNumber} (Eixo ${week.axisId})" to
                    "Semana ${week.weekNumber}, Eixo ${week.axisId}: ${week.title}. Foco da Ficha Técnica: ${week.mepFocus}."
            )
            if (week.theoryTopics.isNotEmpty()) {
                rawPairs.add(
                    "Tópicos Técnicos da Semana" to
                        "Principais tópicos desta aula: ${week.theoryTopics.joinToString(". ")}."
                )
            }
            rawPairs.add("1. Conceito Técnico (10 min)" to "Etapa 1, Conceito Técnico. ${week.concept10Min}")
            rawPairs.add("2. Visualização & Geometria (20 min)" to "Etapa 2, Visualização e Geometria. ${week.visualization20Min}")
            rawPairs.add("3. Aplicação Estruturada (30 min)" to "Etapa 3, Aplicação Estruturada. ${week.application30Min}")
            rawPairs.add("4. Treino Deliberado (15 min)" to "Etapa 4, Treino Deliberado. ${week.practice15Min}")
            rawPairs.add("5. Síntese & PCC (5 min)" to "Etapa 5, Síntese e Pontos Críticos de Controle. ${week.synthesis5Min}")
            return chunkPairsToSegments(rawPairs)
        }

        fun buildAxisSegments(axis: DidacticAxisBrochure): List<NarrationSegment> {
            val rawPairs = mutableListOf<Pair<String, String>>()
            rawPairs.add(
                "Eixo ${axis.axisNumber} • Introdução" to
                    "Eixo ${axis.axisNumber}: ${axis.title}. ${axis.subtitle}."
            )
            axis.overviewCards.forEach { card ->
                rawPairs.add("Visão Geral • ${card.label}" to "${card.label}: ${card.text}")
            }
            axis.sections.forEach { sec ->
                appendSectionPairs(sec, rawPairs)
            }
            return chunkPairsToSegments(rawPairs)
        }

        fun buildSingleSectionSegments(axisNumber: Int, section: DidacticSection): List<NarrationSegment> {
            val rawPairs = mutableListOf<Pair<String, String>>()
            rawPairs.add(
                "Eixo $axisNumber • Seção ${section.number}" to
                    "Eixo $axisNumber, Seção ${section.number}: ${section.title}."
            )
            appendSectionPairs(section, rawPairs)
            return chunkPairsToSegments(rawPairs)
        }

        private fun appendSectionPairs(
            section: DidacticSection,
            out: MutableList<Pair<String, String>>
        ) {
            val secLabel = "Seção ${section.number} • ${section.title}"
            out.add(secLabel to "Seção ${section.number}: ${section.title}.")
            section.blocks.forEach { block ->
                when (block) {
                    is DidacticBlock.Paragraph -> out.add(secLabel to block.text)
                    is DidacticBlock.Bullets -> {
                        block.items.forEach { item -> out.add(secLabel to item) }
                    }
                    is DidacticBlock.Callout -> {
                        out.add(secLabel to "${block.title}. ${block.body}")
                    }
                    is DidacticBlock.MetricStrip -> {
                        val metricsText = block.items.joinToString(". ") { item ->
                            buildString {
                                append(item.label)
                                if (item.value.isNotBlank()) append(": ").append(item.value)
                                if (item.detail.isNotBlank()) append(" — ").append(item.detail)
                            }
                        }
                        out.add(secLabel to "${block.title}. $metricsText.")
                    }
                    is DidacticBlock.Table -> {
                        if (block.caption.isNotBlank()) {
                            val summaryRows = block.rows.take(8).joinToString(". ") { row ->
                                row.filter { it.isNotBlank() }.joinToString(", ")
                            }
                            out.add(secLabel to "Tabela técnica: ${block.caption}. $summaryRows.")
                        }
                    }
                }
            }
        }

        fun sanitizeForSpeech(raw: String): String {
            return raw
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("°C", " graus Celsius")
                .replace("≥", " maior ou igual a ")
                .replace("≤", " menor ou igual a ")
                .replace("→", ", levando a ")
                .replace("➔", ", ")
                .replace("×", " por ")
                .replace("•", ". ")
                .replace("—", ", ")
                .replace("–", " a ")
                .replace("|", ", ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun chunkPairsToSegments(pairs: List<Pair<String, String>>): List<NarrationSegment> {
            val result = mutableListOf<NarrationSegment>()
            var idx = 0
            for ((sectionTitle, rawText) in pairs) {
                val cleaned = sanitizeForSpeech(rawText)
                if (cleaned.isBlank()) continue
                val sentences = cleaned.split(Regex("(?<=[.!?;])\\s+"))
                val buffer = StringBuilder()
                for (sentence in sentences) {
                    val s = sentence.trim()
                    if (s.isEmpty()) continue
                    if (buffer.isNotEmpty() && buffer.length + s.length + 1 > 320) {
                        result.add(NarrationSegment(idx++, sectionTitle, buffer.toString().trim()))
                        buffer.clear()
                    }
                    if (buffer.isNotEmpty()) buffer.append(' ')
                    buffer.append(s)
                }
                if (buffer.isNotEmpty()) {
                    result.add(NarrationSegment(idx++, sectionTitle, buffer.toString().trim()))
                }
            }
            return result
        }
    }
}
