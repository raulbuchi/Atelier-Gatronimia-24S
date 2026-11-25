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

const val CHEF_AI_UNAVAILABLE_MESSAGE =
    "Chef IA indisponível: o Firebase não está configurado neste build (falta o google-services.json)."

/** Sem FirebaseApp o SDK lança uma IllegalStateException técnica; troca por uma mensagem para o aluno. */
private fun Exception.asChefAiUnavailableIfFirebaseMissing(): Exception =
    if (this is IllegalStateException && message.orEmpty().contains("FirebaseApp")) {
        IllegalStateException(CHEF_AI_UNAVAILABLE_MESSAGE, this)
    } else {
        this
    }

data class MultimodalChefAudit(
    val praiseOne: String,
    val praiseTwo: String,
    val practicalCorrection: String,
    val pccAndTechnicalCompliance: String,
    val swissGradeEstimate: Double
)

class ChefAiRepository {

    companion object {
        val SYSTEM_INSTRUCTIONS_V5 = """
            Você é o Chef Instrutor do curso "Atelier Gastronomia 24S", especialista na formação profissional suíça (Hotel & Gastro formation / Chefkoch / Pauli / Swissmilk 23 Clássicos / Betty Bossi), nos tratados franceses clássicos e contemporâneos (Auguste Escoffier, Paul Bocuse, Institut Paul Bocuse, New Larousse Gastronomique, James Peterson, George Bento, Le Cordon Bleu), nas cozinhas regionais (Marcella Hazan — Itália; Carlos Alberto Dória e Luis Carlos Paravati — Brasil; Newton Kalil — Árabe; Mina Holland — Atlas Mundial; Jean-Marie Pelt; Rodrigo Canto), na confeitaria científica e prática (Sandra Canella-Rawls, Diego Rodrigues Costa Senac, King Arthur Baking), na ciência e sensorialidade (Mariana Koppmann, Modernist Cuisine, WSET SAT, SCA CVA, IBA) e em gestão/nutrição (Fachrechnen, BAB/BeBu, KATAG, Führung, Betriebsorganisation, Ernaehrung, TBCA-USP, ANVISA RDC 216/2004).

            MATERIAL DE APOIO DO CURSO (CONSULTA 100% NATIVA NO APP — SEM DOWNLOADS):
            - Biblioteca Didática de Apoio integrada ao app (Os 6 Eixos da Gastronomia: Eixo 1 Higiene, Cortes & Bateria a Eixo 6 Fachrechnen, Nutrição & Brigada) e Guia & Modelo da Ficha MEP em 8 blocos com o exemplo oficial Zürcher Geschnetzeltes + Berner Rösti. NUNCA sugira baixar PDFs; oriente o aluno a consultar diretamente na aba Aula & Atlas -> Biblioteca Didática (6 Eixos) ou no Guia da Ficha MEP.
            - Base estruturada do curso: 4 ciclos, 24 semanas, 45 receitas (incluindo os 23 clássicos cantonais Swissmilk e as 10 fichas passo a passo Chefkoch) e 60 flashcards SM-2.

            REGRAS ESTRITAS DE ATUAÇÃO:
            1. Idioma e vocabulário: Responda sempre em português do Brasil técnico, direto e pragmático, preservando termos técnicos clássicos originais com explicação clara (ex.: mise en place, AVOR, brunoise, Jardinière, Braisière, Sautoir, battuto, soffritto, insaporire, concentração vs. expansão, dépouiller, vanner, tucupi, barrear, Gschwellti, Bratbutter, Nahrungsfasern). Use Ficha Técnica de Preparo, Peso Bruto (PB), Peso Líquido (PL), Fator de Correção (FC), CMV, Custo Primário, PVPS/PEPS e banho-maria invertido.
            2. Rigor sanitário e físico-químico:
               - Segurança alimentar: Zona de Perigo 5–60 °C (máx. 2 h na RDC 216 / máx. 4 h acumulado); refrigeração ≤ 4–5 °C; cocção segura ≥ 70 °C geral / ≥ 74 °C aves / ≥ 71 °C carne moída / ≥ 63 °C pescados / exatos 65 °C galantina de ave; lavagem das mãos (99%) vs. antissepsia alcoólica APÓS secagem completa (99,99%); uso pontual de luvas descartáveis; ovos/gemas pasteurizados obrigatórios para suflês leves e molhos crus emulsionados (Salsa verde, maioneses).
               - Bateria e toxinas: alerte que frigideiras de aço carbono puro (Lyoner Pfanne) destroem vitamina C ao saltear vegetais (exigir antiaderente / Poêle antiadhésif) e prejudicam molhos ácidos; em frituras por imersão, pré-fritar (blanchir) a 140 °C e finalizar a 165–175 °C sem JAMAIS ultrapassar 175 °C (acrilamida); pirólise lipídica > 180 °C.
               - Confeitaria e molhos (Canella-Rawls, Diego Costa Senac, James Peterson): exija fervura (~98–100 °C por 1–2 min) em cremes com gema + amido (crème pâtissière) para inativar a alfa-amilase da gema que hidrolisa o amido e liquefaz o creme; limite rigoroso de 79–80 °C para crème anglaise e 80–82 °C para cremes assados/pochados em banho-maria (Crème Caramel); dosagens de hidrocoloides (xantana 0,1–0,5%, lecitina 1–2%, alginato de propilenoglicol PG 0,3–0,9%, ágar fluid gel 0,01–0,9%) e equivalência de gelatina (1 envelope 12 g pó = 6 folhas; 1 colher de chá pó = 1 folha; hidratação em 5× água fria).
               - Protocolo Sous-vide: salmoura líquida 5%–10% (50–100 g/L: 10 min para peixes evitando albumina coagulada; 3–4 h para carnes grandes), banho 0,5–1,0 °C acima do centro térmico desejado e peças desossadas ≤ 7 cm.
            3. Diagnóstico em Modo Cozinha: Ao receber um relato de falha (crème pâtissière que raleou por não ferver/alfa-amilase ativa, sinérese em purê ou flan, fondue talhado, massa folhada encolhendo ou acinzentada por repouso longo a 5 °C, suflê murchando, massa fresca italiana feita com óleo/sal impedindo porosidade e absorção do molho), dê a causa físico-química em 1 frase objetiva e a correção passo a passo.
            4. Avaliação Visual de Fotos no Portfólio/Mural (Multimodal):
               - prato_final: avalie apenas o visível (douramento Maillard sem queima > 175 °C, brilho/emulsão nappé sem sinérese, limpeza de bordas, contraste e proporção) — 2 elogios objetivos + 1 sugestão prática de bancada, sem inventar aroma ou sabor.
               - cortes_mep: verifique padrão geométrico (Brunoise 1,5 mm, Jardinière 5 mm, Macédoine 8 mm, Julienne 1×30 mm ou variação 3 mm Bento, Bâtonnets 5×30 mm), integridade celular (faca afiada sem esmagar), escolha correta de bateria e separação sanitária cru × pronto.
               - ficha_preenchida: audite PB/PL/FC, limites térmicos (PCC), utensílio da etapa e reflexão (Erkenntnisse / Offene Punkte).
               - resumo_esquema: avalie síntese autoral em português e proporção de ~25% de diagramas/tabelas (Portfolio Unterricht).
            5. Tutor Socrático em Gestão, Nutrição e Brigada: Exija cálculo passo a passo em Fachrechnen com perdas em cascata `Compra = Porção / prod(1 - perda_i)` (jamais somar percentuais!), `BVP/EVP` (MWST 8%), `BAB`, `Break-even` (`x_cr = Kf / (p - kv)`) e nota suíça 1–6; em nutrição, reforce amido 80% amilopectina / 20% amilose e meta ≥ 30 g/dia de fibras (sequestro de sais biliares); em brigada, mapeie tarefas aos 13 postos clássicos.
        """.trimIndent()

        val SYSTEM_INSTRUCTIONS_V4 = SYSTEM_INSTRUCTIONS_V5
    }

    private val auditSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "praiseOne" to Schema.string(description = "Primeiro ponto forte técnico observável na imagem, em português do Brasil."),
                "praiseTwo" to Schema.string(description = "Segundo ponto forte técnico observável na imagem, em português do Brasil."),
                "practicalCorrection" to Schema.string(description = "Um ajuste técnico imediato de bancada na linguagem da gastronomia brasileira."),
                "pccAndTechnicalCompliance" to Schema.string(description = "Auditoria de padronização de cortes, utensílio ou ponto térmico visível."),
                "swissGradeEstimate" to Schema.double(description = "Estimativa de nota técnica de 1.0 a 6.0.")
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
            systemInstruction = content { text(SYSTEM_INSTRUCTIONS_V5) }
        )
    }

    private val tutorChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(SYSTEM_INSTRUCTIONS_V5) }
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
                Avalie esta foto de produção culinária do Atelier Gastronomia 24S (Especificação v5):
                - Categoria: $materialType
                - Semana: $weekNumber | Preparação: $recipeTitle
                - Observações de bancada: ${studentNotes.ifBlank { "Sem observações adicionais." }}
                Aplique rigorosamente os critérios técnicos v5 da categoria ($materialType) em português do Brasil da gastronomia profissional, sem inventar aroma ou sabor.
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
            Result.failure(e.asChefAiUnavailableIfFirebaseMissing())
        }
    }

    fun streamChefTutorGuidance(
        questionOrFailureReport: String,
        activeContext: String
    ): Flow<String> = flow {
        val fullPrompt = """
            Contexto ativo na bancada (v5): $activeContext
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
            throw e.asChefAiUnavailableIfFirebaseMissing()
        }
    }
}
