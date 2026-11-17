package com.example.data.curriculum

import com.example.data.local.MepSheetEntity
import com.example.domain.model.BrochureGuideSpec
import com.example.domain.model.CourseWeek

/**
 * Cronograma oficial de 24 Semanas (4 Ciclos), 6 Brochuras (-v3.pdf) e Ficha MEP Exemplo
 * carregados a partir de `curso-24-semanas-dados-v4.json` + expansões bibliográficas v5.
 */
object CurriculumWeeksAndGuides {

    val brochures: List<BrochureGuideSpec>
        get() = CourseJsonDatasetLoader.dataset.brochures

    val weeks: List<CourseWeek>
        get() = CourseJsonDatasetLoader.dataset.weeks

    val blankMepTemplateMarkdown: String = """
        # MODELO EM BRANCO — FICHA TÉCNICA MEP (8 BLOCOS) • ATELIER GASTRONOMIA 24S (v4/v5)
        - **Fluxo Oficial de Consulta no App:** Preencher **Frente (Blocos 1–4)** antes de acender o fogo (AVOR) e **Verso (Blocos 5–8)** durante e após a execução.
        - **Preparação:** ______________________________________ | **Semana:** ____ | **Praça da Brigada:** __________________
        - **Rendimento Base:** ____ porções | **Alvo Escalonado:** ____ porções (`FC Escala = Alvo ÷ Base`)
        - **Quebra Térmica Estimada (%):** ______% | **Meta CMV (%):** ______% | **Preço Sugerido (BVP / EVP c/ MWST 8%):** ____________

        ## 1. Identificação do Treino & Escala da Receita (Frente)
        - Trilha (`base_global` | `swissmilk_23` | `chefkoch_step_by_step`) e Peso da porção pós-cocção (g):

        ## 2. Mapa de Insumos, Cortes & Rendimento (`PB = PL × FC`)
        | Insumo | Setor PVPS | Unid. | PL Escalonado | FC (`1 / (1 - apara)`) | PB Compra | Custo Unit. | Custo Linha |
        | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
        | 1. | | | | | | | |
        | 2. | | | | | | | |
        | 3. | | | | | | | |

        ## 3. Bateria de Cozinha, Equipamentos & Pré-Aquecimento (`Kochgeschirrarten`)
        - Panelas / Caçarolas (atenção: antiaderente obrigatório p/ vegetais ricos em Vit. C; Sautoir/Rondeau p/ molhos):
        - Cutelaria & Precisão (`Chefmesser`, `Rüstmesser`, `Tamis`, Termômetro de Sonda):

        ## 4. Pré-Preparo, Padronização de Cortes (`Schnittarten`) & AVOR
        - Cortes milimétricos (`Brunoise` 1,5 mm, `Jardinière` 5 mm, `Julienne` 1×30 mm / 3 mm George Bento, `Bâtonnets` 5×30 mm):
        - Salmouras (5–10%), Marinadas (≤ 5 °C) ou Hidratação de Hidrocoloides:

        ## 5. Sequência Cronológica & Controle Térmico / PCC (Verso)
        - Método (`Concentração` vs. `Expansão`, `Poelieren` 140–160 °C, Fritura ≤ 175 °C, Alfa-amilase 98–100 °C, Anglaise ≤ 80 °C):
        - Centro térmico obrigatório (°C) e resfriamento rápido em 2 estágios (60→21 °C ≤ 2h; 21→5 °C ≤ +4h):

        ## 6. Montagem, Serviço, Empratamento & Aproveitamento de Aparas (`Parüren`)
        - Temperatura do prato (°C), disposição espacial, ponto do molho (`nappé` / `vanner`) e elemento crocante:

        ## 7. Segurança Sanitária (RDC 216 / APPCC / 4 POPs) & Alergênicos
        - Higienização (99%) + antissepsia em mãos secas (99,99%), sanitização 200 ppm/15 min e alergênicos declarados:

        ## 8. Degustação, Autoavaliação (`Selbstkontrolle` 1–5) & Lições Aprendidas
        - Peso real aferido pós-cocção (g) e desvio (%):
        - Aprendizados consolidados (`Erkenntnisse`) e Pontos abertos (`Offene Punkte`):
    """.trimIndent()

    val exampleMepZurcherRosti = MepSheetEntity(
        recipeId = "rec-zurcher-geschnetzeltes",
        title = "Zürcher Geschnetzeltes com Berner Rösti (ZH • Exemplo Oficial)",
        weekNumber = 8,
        stationBrigade = "Saucier",
        baseServings = 4,
        targetServings = 4,
        portionWeightGrams = 350.0,
        cookingShrinkagePercent = 14.0,
        targetCmvPercent = 27.0,
        directLaborCost = 9.5,
        pccTargetTempC = 68,
        plateTempC = 55,
        allergensCsv = "Lactose, Glúten, Sulfitos",
        actualBenchYieldGrams = 1415.0,
        executionScore1To5 = 5,
        isCustomSheet = false,
        block1Identification = "Zürcher Geschnetzeltes com Berner Rösti (Exemplo Oficial de Consulta • Semana 8) | Rendimento: 4 porções (600 g vitela + Rösti Gschwellti)",
        block2IngredientsPbPlFc = "Vitela em tiras (Kalbsgeschnetzeltes) PL 600g × FC 1,05 = PB 630g | Cogumelos de Paris PL 200g × FC 1,10 = PB 220g | Vinho branco seco 100ml + Caldo 100ml | Creme integral Vollrahm 200ml | Manteiga clarificada Bratbutter 25g",
        block3CookwareBlades = "Caçarola rasa Sautoir / Flachkasserolle (selagem por concentração em 2 lotes e molho de deglaçagem), frigideira antiaderente ou Lyoner Pfanne (disco dourado de Berner Rösti), Chefmesser afiada",
        block4PrePrepCuts = "Cebola e salsinha lisa em brunoise fina (~1,5 mm); cogumelos fatiados regulares; raspa fina de ½ limão no Zestenmesser; tiras de vitela bem secas em papel-toalha polvilhadas com 1 c. sopa de farinha na hora.",
        block5ThermalFlowPcc = "Selar a vitela em 2 porções pequenas em Bratbutter bem quente (~1 min/lote, Maillard sem soltar água) e reservar aquecida; suar cebola + cogumelos, deglaçar com 100 ml de vinho branco + caldo reduzindo à metade; juntar 200 ml de Vollrahm com 1 c. chá de amido até nappé aveludado (82 °C); retornar a vitela apenas para reaquecer SEM ferver (PCC ≥ 68 °C).",
        block6PlatingFinishing = "Prato aquecido a 55 °C: disco dourado e crocante de Berner Rösti à direita, Zürcher Geschnetzeltes brilhante à esquerda preservando a crocância da batata, salsinha fresca salpicada.",
        block7HygieneAllergens = "Alergênicos: Lactose (Vollrahm, Bratbutter), Glúten (farinha de polvilhamento), Sulfitos (vinho branco). Cadeia fria ≤ 4 °C até a selagem; higienização (99%) + antissepsia das mãos secas (99,99%).",
        block8Selbstkontrolle = "Erkenntnisse: Saltear em 2 lotes manteve a Sautoir acima de 160 °C sem cozinhar a vitela no vapor. Offene Punkte: Sincronizar a virada da Berner Rösti exatamente com o início da deglaçagem do vinho."
    )
}
