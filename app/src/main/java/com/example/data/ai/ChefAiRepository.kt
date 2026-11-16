package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.media.MediaCompressor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ChefAiRepository"

data class MultimodalChefAudit(
    val praiseOne: String,
    val praiseTwo: String,
    val practicalCorrection: String,
    val pccAndTechnicalCompliance: String,
    val swissGradeEstimate: Double
)

class ChefAiRepository {

    companion object {
        val SYSTEM_INSTRUCTIONS_V4 = """
            Você é o Chef Instrutor do curso "Atelier Gastronomia 24S", especialista na formação profissional suíça (Hotel & Gastro formation / Chefkoch / Swissmilk 23 Clássicos / Betty Bossi / Pauli), nos tratados clássicos (Chef Profissional CIA, Le Cordon Bleu — Técnicas Culinárias & Sobremesas, Escoffier, Dumas), na ciência dos alimentos (Mariana Koppmann, Modernist Cuisine), na gastronomia brasileira e mediterrânea, em análise sensorial/bebidas (WSET SAT, SCA CVA, IBA) e em gestão/nutrição (Fachrechnen, BAB/BeBu, KATAG, Führung, Betriebsorganisation, Ernaehrung, TBCA-USP, ANVISA RDC 216/2004).

            REGRAS DE ATUAÇÃO:
            1. Idioma: Responda sempre em português do Brasil técnico, direto e pragmático, preservando termos originais (mise en place, brunoise, Jardinière, Braisière, Sautoir, Gschwellti, Bratbutter, Sous-vide, Maillard, Prime-Cost, Selbstkontrolle, Nahrungsfasern).
            2. Rigor sanitário e técnico:
               - Zona de Perigo 5–60 °C (máx. 4 h); refrigeração ≤ 5 °C; cocção segura ≥ 70 °C geral / 74 °C aves / 71 °C carne moída / 63 °C peixe / 65 °C galantina de ave; lavagem (99%) vs. desinfecção das mãos secas (99,99%).
               - Frigideiras de aço puro destroem vitamina C ao saltear vegetais (exigir Antihaft-Pfanne) e não servem para construir molhos.
               - Frituras por imersão: pré-fritar a 140 °C e finalizar a 165–175 °C sem JAMAIS ultrapassar 175 °C para evitar acrilamida.
               - Sous-vide: salmoura 5–10% (50–100 g/L — 10 min peixes, 3–4 h carnes grandes), banho 0,5 a 1 °C acima do centro térmico e peças ≤ 7 cm sem ossos.
               - Massa folhada suíça: nunca guardar crua por longo tempo a 5 °C (acidifica e acinzenta por oxidação).
            3. Diagnóstico em Modo Cozinha: Diante de falhas relatadas (fondue talhado, folhado encolhendo/acinzentado, suflê murchando, caramelo amargo ou creme caramelado empolado), diagnostique a causa físico-química em 1 frase e dê a correção imediata passo a passo.
            4. Avaliação Visual de Fotos (Multimodal):
               - prato_final: avalie douramento Maillard sem queima > 175 °C, nappé/emulsão, limpeza de bordas, contraste e proporção — sempre 2 elogios objetivos + 1 sugestão prática, sem inventar aroma ou sabor.
               - cortes_mep: verifique padrão geométrico suíço (Brunoise 1,5 mm, Jardinière 5 mm, Macédoine 8 mm, Julienne 1 mm × 3 cm, Bâtonnets 5 mm × 3 cm), integridade celular (faca afiada sem esmagar), bateria correta e separação cru × pronto.
               - ficha_preenchida: audite PB/PL/FC, PCC térmico, utensílio correto e Erkenntnisse / Offene Punkte.
               - resumo_esquema: avalie síntese própria em português e proporção de ~25% de diagramas/tabelas (Portfolio Unterricht).
            5. Tutor Socrático em Gestão, Nutrição e Brigada: Em Fachrechnen, exija produto (1 - perda_i) na ordem inversa; em Ernaehrung, meta ≥ 30 g/dia de fibras (sequestro de ácidos biliares); em Betriebsorganisation, relacione aos 13 cargos da brigada clássica.
        """.trimIndent()
    }

    private val auditSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "praiseOne" to Schema.string(description = "Primeiro elogio técnico objetivo observável na foto."),
                "praiseTwo" to Schema.string(description = "Segundo elogio técnico objetivo observável na foto."),
                "practicalCorrection" to Schema.string(description = "Uma sugestão prática e imediata de melhoria técnica."),
                "pccAndTechnicalCompliance" to Schema.string(description = "Auditoria de geometria suíça, utensílio ou ponto térmico visível."),
                "swissGradeEstimate" to Schema.double(description = "Estimativa de nota técnica suíça entre 1.0 e 6.0.")
            )
        )
    }

    private val structuredVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = auditSchema
            },
            systemInstruction = content { text(SYSTEM_INSTRUCTIONS_V4) }
        )
    }

    private val tutorChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(SYSTEM_INSTRUCTIONS_V4) }
        )
    }

    suspend fun evaluateCulinaryPhoto(
        bitmap: Bitmap,
        materialType: String,
        recipeTitle: String,
        weekNumber: Int,
        studentNotes: String
    ): Result<MultimodalChefAudit> {
        return try {
            val optimizedBitmap = MediaCompressor.scaleForAiInference(bitmap, maxPx = 1024)
            val promptText = """
                Avalie esta foto de treino culinário do Atelier Gastronomia 24S:
                - Categoria: $materialType
                - Semana: $weekNumber | Preparo: $recipeTitle
                - Notas do aluno: ${studentNotes.ifBlank { "Sem observações adicionais." }}
                Aplique rigorosamente os critérios suíços da categoria ($materialType), sem inventar aroma ou sabor.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredVisionModel.generateContent(
                    content {
                        image(optimizedBitmap)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text ?: error("O modelo Chef IA retornou resposta vazia.")
            val json = JSONObject(rawJson)
            Result.success(
                MultimodalChefAudit(
                    praiseOne = json.optString("praiseOne", "Boa apresentação geral."),
                    praiseTwo = json.optString("praiseTwo", "Organização visível de bancada."),
                    practicalCorrection = json.optString("practicalCorrection", "Padronizar arestas geométricas."),
                    pccAndTechnicalCompliance = json.optString("pccAndTechnicalCompliance", "Verificar PCC térmico na ficha."),
                    swissGradeEstimate = json.optDouble("swissGradeEstimate", 5.0).coerceIn(1.0, 6.0)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Multimodal evaluation timed out", e)
            Result.failure(IllegalStateException("Avaliação visual expirou após 60s. Verifique sua conexão e tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal evaluation failed", e)
            Result.failure(e)
        }
    }

    fun streamChefTutorGuidance(
        questionOrFailureReport: String,
        activeContext: String
    ): Flow<String> = flow {
        val fullPrompt = """
            Contexto ativo na bancada: $activeContext
            Solicitação do aluno / Diagnóstico: $questionOrFailureReport
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                tutorChatModel.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("O Chef IA retornou uma resposta em branco.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tutor stream timed out", e)
            throw IllegalStateException("Tempo limite de 60s atingido ao consultar o Chef IA.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tutor stream failed", e)
            throw e
        }
    }
}
