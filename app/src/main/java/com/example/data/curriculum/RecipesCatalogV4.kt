package com.example.data.curriculum

import com.example.domain.model.CookingStepSpec
import com.example.domain.model.IngredientSpec
import com.example.domain.model.RecipeSpec
import com.example.domain.model.SmartKitchenAlertType

object RecipesCatalogV4 {

    /**
     * As 8 Fichas Profissionais Passo a Passo da Especificação v4 (Chefkoch Step-by-Step)
     * + Protocolos Sous-Vide Validados.
     */
    val chefkochV4Recipes: List<RecipeSpec> = listOf(
        // 1) Brauner Kalbsfond (Sem. 3 — base 10 porções / 5 L)
        RecipeSpec(
            id = "brauner_kalbsfond_v4",
            weekNumber = 3,
            cycleNumber = 1,
            axisId = 2,
            title = "Brauner Kalbsfond (Fundo Escuro de Vitela — 5 L)",
            subtitle = "Extração clássica de colágeno com tripla deglaçagem até glace",
            baseServings = 10,
            cantonCode = "CH",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("rotissoire", "chefmesser", "tamis"),
            pccTargetSummary = "Fervura branda 88–92 °C por 3h | Resfriamento Cook-and-Chill ≤ 5 °C em < 2h",
            ingredients = listOf(
                IngredientSpec("Ossos de vitela picados pequenos", 4000.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Pés de vitela limpos (colágeno)", 1000.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Mirepoix (cenoura, aipo-rábano, cebola, alho-poró)", 800.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Extrato de tomate concentrado (Tomatenpüree)", 120.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Vinho branco seco para deglaçagem", 1000.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Água fria", 7000.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Sal marinho", 40.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Sachet d'épices (louro, tomilho, pimenta em grão, cravo)", 25.0, "g", 1.0, "Especiarias & Fundos")
            ),
            steps = listOf(
                CookingStepSpec(1, "Assar ossos a seco", "Distribuir os 4 kg de ossos e 1 kg de pés de vitela na Rôtissoire sem gordura. Assar a 200 °C até douramento uniforme de Maillard (sem carbonizar).", "Bratgeschirr / Rôtissoire", 200, 40),
                CookingStepSpec(2, "Mirepoix & Pinçage", "Escorrer excesso de gordura, juntar os 800 g de mirepoix e dourar. Adicionar 120 g de extrato de tomate (pincage) mexendo até cor ferrugem aromática.", "Bratgeschirr / Rôtissoire", 180, 12),
                CookingStepSpec(3, "Deglaçagem 2–3x até Glace", "Transferir para caldeirão alto, deglaçar a assadeira com frações do vinho branco (1 kg total) por 2 a 3 vezes, reduzindo cada adição até xarope espesso (glace).", "Rondeau / Caldeirão", 98, 20),
                CookingStepSpec(4, "Fervura branda 3h & Ervas na última hora", "Cobrir com os 7 kg de água fria + 40 g de sal. Escumar impurezas constantemente; manter a 88–92 °C por 3 horas. Adicionar ervas/especiarias APENAS na última hora.", "Caldeirão de Fundo", 90, 180),
                CookingStepSpec(5, "Passar em pano & Resfriar", "Coar delicadamente através de pano fino úmido (étamine) sem pressionar sólidos. Resfriar imediatamente em banho de gelo até ≤ 5 °C.", "Tamis + Pano Étamine", 5, 30)
            )
        ),

        // 2) Berner Platte (Sem. 10 — 4/10 pess.)
        RecipeSpec(
            id = "berner_platte_v4",
            weekNumber = 10,
            cycleNumber = 2,
            axisId = 3,
            title = "Berner Platte Clássica",
            subtitle = "Controle paralelo de 5 cocções escalonadas com vagens e chucrute",
            baseServings = 4,
            cantonCode = "BE",
            isSwissmilkClassic = true,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("poele_lyonnaise", "poele_antiadhesif", "chefmesser"),
            pccTargetSummary = "Submersão branda Sieden ~82–88 °C | Costeleta & Zungenwurst pochadas a 82 °C",
            ingredients = listOf(
                IngredientSpec("Língua bovina defumada (Geräucherte Rindszunge)", 320.0, "g", 1.25, "Açougue & Peixaria"),
                IngredientSpec("Siedfleisch Hohrücken (Acém nobre suíço)", 400.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Speck / Bacon entremeado defumado", 200.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Costeletas suínas defumadas (Kasseler / Rippli)", 240.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Berner Zungenwurst (embutido bernês)", 240.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Vagens verdes frescas ou Dörrbohnen", 320.0, "g", 1.12, "Hortifrúti"),
                IngredientSpec("Segurelha fresca (Bohnenkraut)", 8.0, "g", 1.15, "Hortifrúti"),
                IngredientSpec("Chucrute cru (Sauerkraut) c/ zimbro", 400.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Batatas firmes p/ Salzkartoffeln", 400.0, "g", 1.18, "Hortifrúti")
            ),
            steps = listOf(
                CookingStepSpec(1, "Tostar cebola & Iniciar Língua (T-120m)", "Tostar meias cebolas com casca na Lyoner Pfanne seca até escurecer. Iniciar a língua defumada em caldo aromático a 85 °C (~120 min totais).", "Lyoner Pfanne + Caldeirão", 85, 120, SmartKitchenAlertType.PARALLEL_POACHING_82C, "CRONOGRAMA PARALELO: Língua entra em T-120 min; Hohrücken em T-90 min; Bacon em T-60 min."),
                CookingStepSpec(2, "Entrada do Siedfleisch Hohrücken (T-90m)", "Mergulhar o Hohrücken no caldo já quente (~88 °C) para selar proteínas superficiais e hidratar colágeno por ~90 min.", "Caldeirão de Sieden", 88, 90, SmartKitchenAlertType.PARALLEL_POACHING_82C, "Manter sub-ebulição tranquila (85–88 °C): ferver forte resseca as fibras do Hohrücken!"),
                CookingStepSpec(3, "Entrada do Speck & Chucrute (T-60m)", "Adicionar o bacon (~60 min) e estufar o chucrute à parte com banha/manteiga, vinho branco e bagas de zimbro.", "Braisière", 85, 60),
                CookingStepSpec(4, "Vagens c/ Bohnenkraut em Antihaft", "Branquear as vagens em água salgada fervente, dar choque térmico imediato em gelo e saltear/estufar em frigideira ANTIADERENTE com manteiga, cebola Brunoise e segurelha (Bohnenkraut).", "Bratpfanne (Antihaft) / Poêle antiadhésif", 90, 12, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "PROIBIDO AÇO PURO: Use Poêle antiadhésif — frigideiras de aço oxidam e destroem a vitamina C das vagens!"),
                CookingStepSpec(5, "Pochar Rippli & Zungenwurst a 82 °C", "Nos 20 min finais, pochar as costeletas e a Berner Zungenwurst a exatos ~82 °C (não ferver para não estourar a tripa). Fatiar todas as carnes contra a fibra e servir sobre travessa aquecida.", "Caçarola de Pochar", 82, 20, SmartKitchenAlertType.PARALLEL_POACHING_82C, "Teto de 82 °C para Zungenwurst: acima disso a emulsão dilata e rompe o invólucro.")
            )
        ),

        // 3) Bollito misto mit Salsa verde (Sem. 11 — 4/10 pess.)
        RecipeSpec(
            id = "bollito_misto_v4",
            weekNumber = 11,
            cycleNumber = 2,
            axisId = 3,
            title = "Bollito misto mit Salsa verde",
            subtitle = "Huftdeckel, vitela, língua, Zampone e ave pochada com emulsão herbácea",
            baseServings = 4,
            cantonCode = "TI",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("chefmesser", "cul_de_poule"),
            pccTargetSummary = "Carnes ≥ 75 °C (ave ≥ 74 °C) | Salsa verde obrigatoriamente com gema pasteurizada ≤ 5 °C",
            ingredients = listOf(
                IngredientSpec("Huftdeckel bovino (capa de alcatra / picanha suíça)", 280.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Ombro de vitela (Kalbsschulter)", 240.0, "g", 1.10, "Açougue & Peixaria"),
                IngredientSpec("Língua de vitela pré-curada", 200.0, "g", 1.22, "Açougue & Peixaria"),
                IngredientSpec("Zampone pré-cozido tradicional", 200.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Supremo de frango c/ pele", 240.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Salsa lisa fresca (p/ Salsa verde)", 60.0, "g", 1.20, "Hortifrúti"),
                IngredientSpec("Filés de aliche, alcaparras e alho", 35.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Gema de ovo PASTEURIZADA", 40.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Azeite extravirgem + suco de limão", 120.0, "g", 1.0, "Mercearia Seca")
            ),
            steps = listOf(
                CookingStepSpec(1, "Cocção de Huftdeckel, Vitela e Língua (~60m)", "Imersão em fundo aromático fervente, baixando para 85 °C por ~1 hora até maciez suculenta.", "Caldeirão Sieden", 85, 60, SmartKitchenAlertType.PARALLEL_POACHING_82C, "Controlar fervura suave a 85 °C mantendo panelas separadas para o Zampone."),
                CookingStepSpec(2, "Cocção separada do Zampone (~50m)", "Cozinhar o Zampone envolto em folha/pano em água dedicada a 85 °C por ~50 min.", "Caçarola", 85, 50),
                CookingStepSpec(3, "Pochar o Frango no Fundo de Ave", "Pochar o frango delicadamente em fundo claro de ave até atingir ≥ 74 °C no centro sem ressecar.", "Sautoir / Caçarola", 80, 18),
                CookingStepSpec(4, "Emulsionar Salsa Verde na Faca", "Picar salsa, alcaparras, aliche e alho finamente com faca afiada (sem esmagar). Em Cul de poule, emulsionar gema pasteurizada, limão e azeite em fio.", "Chefmesser + Cul de poule", 5, 12)
            )
        ),

        // 4) Bison-Mignons mit Thymiankruste (Sem. 12 — 4/10 pess.)
        RecipeSpec(
            id = "bison_mignons_v4",
            weekNumber = 12,
            cycleNumber = 2,
            axisId = 3,
            title = "Bison-Mignons mit Thymiankruste",
            subtitle = "Mignons de 60 g escarificados em 2 mm, purê de cebolinha no tinto e Wild-Demi-glace c/ gin",
            baseServings = 4,
            cantonCode = "GR",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("sautoir", "ruestmesser", "tamis"),
            pccTargetSummary = "Cocção saignant 52–54 °C internos + repouso sobre grelha aquecida",
            ingredients = listOf(
                IngredientSpec("Mignons de bisão (2 × 60 g por pessoa)", 480.0, "g", 1.06, "Açougue & Peixaria"),
                IngredientSpec("Mostarda inglesa em pó + vinho branco", 30.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Tomilho fresco, cebolinha e farinha (crosta)", 50.0, "g", 1.10, "Hortifrúti"),
                IngredientSpec("Cebolinhas/échalotes p/ purê", 320.0, "g", 1.15, "Hortifrúti"),
                IngredientSpec("Vinho tinto seco encorpado", 200.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Wild-Demi-glace (fundo escuro de caça reduzido)", 240.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Creme de leite 35% gordura + Gin seco", 90.0, "g", 1.0, "Laticínios & Ovos")
            ),
            steps = listOf(
                CookingStepSpec(1, "Purê de Cebolinha no Vinho Tinto", "Suar as échalotes fatiadas sem queimar, regar com vinho tinto, reduzir até secar quase por completo, processar e passar no tamis.", "Sauteuse + Tamis", 92, 25),
                CookingStepSpec(2, "Incisão em Grade de 2 mm & Crosta", "Com Rüstmesser afiada, fazer corte em grade superficial de exatos 2 mm no topo dos mignons de 60 g. Pincelar pasta de mostarda inglesa + vinho branco e pressionar na mistura de tomilho, cebolinha e farinha.", "Rüstmesser", 4, 10),
                CookingStepSpec(3, "Selagem Saignant & Repouso", "Selar em Sautoir com Bratbutter quente dourando a crosta sem queimá-la; atingir 52–54 °C (saignant) e repousar 5 min sobre grelha aquecida.", "Flachkasserolle / Sautoir", 53, 8),
                CookingStepSpec(4, "Wild-Demi-glace com Creme 35% e Gin", "Deglaçar a Sautoir, incorporar o Wild-Demi-glace, montar com creme 35% e perfumar fora do fogo com toque preciso de gin.", "Flachkasserolle / Sautoir", 85, 6)
            )
        ),

        // 5) Deutscher / Halb / Blitzblätterteig (Sem. 14 — base 10 porções / 1 kg)
        RecipeSpec(
            id = "deutscher_blaetterteig_v4",
            weekNumber = 14,
            cycleNumber = 3,
            axisId = 5,
            title = "Deutscher / Halb / Blitzblätterteig (1 kg)",
            subtitle = "Laminação suíça em 4 dobras duplas (farinha T400 + malte) e 13 formatos de Konfekt",
            baseServings = 10,
            cantonCode = "CH",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("chefmesser", "cul_de_poule"),
            pccTargetSummary = "Laminação a 14–16 °C | Descansos ≥ 30 min | Forno vapor inicial 210–220 °C",
            ingredients = listOf(
                IngredientSpec("Farinha de trigo suíça T400 (Weissmehl)", 420.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Margarina de folhado (Ziehbutter/Margarine)", 300.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Manteiga sem sal (p/ détrempe)", 65.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Água fria", 210.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Sal refinado", 10.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Malte diastático enzimático", 2.0, "g", 1.0, "Mercearia Seca")
            ),
            steps = listOf(
                CookingStepSpec(1, "Détrempe (Grundteig) & Descanso", "Misturar 420 g farinha T400, 65 g manteiga macia, 10 g sal, 2 g malte e 210 g água fria sem desenvolver excesso de tenacidade elástica. Cortar em cruz e descansar ≥ 30 min refrigerada.", "Cul de poule", 5, 30),
                CookingStepSpec(2, "Empastamento & Dobras Duplas (Touren)", "Envelopar o bloco de 300 g de gordura plástica (15 °C). Executar 4 dobras duplas (Touren doubles — ou 3 dobras duplas no Halbblätterteig), respeitando ≥ 30 min de descanso frio coberto a cada 2 dobras.", "Bancada fria + Rolo", 15, 60, SmartKitchenAlertType.PUFF_PASTRY_OXIDATION, "PROIBIDO ESTOCAR CRUA LONGO TEMPO A 5 °C: A massa acidifica e fica acinzentada por oxidação! Congele (-18 °C) se não assar no dia."),
                CookingStepSpec(3, "Modelagem dos 13 Formatos & Forno", "Abrir a 3–4 mm, cortar com lâmina vertical afiada (sem amassar bordas folhadas), descansar 20 min antes do forno e assar a 210 °C até expansão seca e dourada.", "Assadeira perfurada", 210, 22)
            )
        ),

        // 6) Vanilleauflauf / Suflê de Baunilha (Sem. 16 — 4/10 pess.)
        RecipeSpec(
            id = "vanilleauflauf_v4",
            weekNumber = 16,
            cycleNumber = 3,
            axisId = 5,
            title = "Vanilleauflauf (Suflê Quente de Baunilha)",
            subtitle = "Base cozida em leite infundido, pré-aquecimento em banho-maria e subida reta a 200 °C",
            baseServings = 4,
            cantonCode = "CH",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("sauteuse", "cul_de_poule"),
            pccTargetSummary = "Pré-aquecimento em banho-maria no fogão → Forno estático 200 °C por 15–20 min",
            ingredients = listOf(
                IngredientSpec("Leite integral", 250.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Manteiga sem sal (+ p/ untar)", 50.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Farinha T550 (+ p/ polvilhar ramequins)", 50.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Açúcar refinado", 60.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Gemas de ovo", 60.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Claras de ovo frescas", 120.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Fava de baunilha Bourbon", 1.0, "un", 1.0, "Especiarias & Fundos")
            ),
            steps = listOf(
                CookingStepSpec(1, "Preparo Técnico dos Ramequins", "Pincelar ramequins em movimentos estritamente verticais de baixo para cima com manteiga amolecida, refrigerar, dar segunda camada e encamisar com farinha T550.", "Ramequins de porcelana", 4, 10),
                CookingStepSpec(2, "Massa Base Cozida (Brandmasse-Roux)", "Infundir leite com a fava de baunilha. Fazer roux claro com manteiga e farinha T550, juntar o leite e cozinhar mexendo vigorosamente até a massa desprender do fundo da panela. Amornar e incorporar as gemas uma a uma.", "Sauteuse", 85, 10),
                CookingStepSpec(3, "Incorporação das Claras & ¾ da Forma", "Bater claras em neve cremosa (pico médio brilhante, não ressecado) com o açúcar. Envolver em 2 etapas na base morna e preencher os ramequins até exatos ¾ da altura, limpando a borda com o polegar.", "Cul de poule", 22, 8),
                CookingStepSpec(4, "Banho-Maria no Fogão + Forno 200 °C", "Posicionar os ramequins em assadeira com água quente na boca do fogão para iniciar o impulso térmico da base e levar imediatamente ao forno pré-aquecido a 200 °C por 15–20 min sem abrir a porta.", "Rôtissoire + Forno", 200, 18)
            )
        ),

        // 7) Gestürzte Karamellcreme (Sem. 16 — base 10 pess.)
        RecipeSpec(
            id = "gestuerzte_karamellcreme_v4",
            weekNumber = 16,
            cycleNumber = 3,
            axisId = 5,
            title = "Gestürzte Karamellcreme (Pudim Clássico Suíço — 10 pess.)",
            subtitle = "Pochado a exatos 80 °C coberto com filme plástico para textura sedosa sem furinhos",
            baseServings = 10,
            cantonCode = "CH",
            isSwissmilkClassic = true,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("cul_de_poule", "tamis", "rotissoire"),
            pccTargetSummary = "Caramelo ~165 °C | Banho-maria controlado a 80 °C | Maturação fria ≤ 5 °C por ≥ 12h",
            ingredients = listOf(
                IngredientSpec("Açúcar p/ caramelo claro", 200.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Água p/ calda do caramelo", 50.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Leite integral", 625.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Ovos inteiros", 190.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Gemas de ovo", 50.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Açúcar do aparelho", 95.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Fava de baunilha", 1.0, "un", 1.0, "Especiarias & Fundos")
            ),
            steps = listOf(
                CookingStepSpec(1, "Caramelo Claro nas Formas", "Ferver 200 g de açúcar com 50 g de água limpando as bordas com pincel úmido até caramelo âmbar claro (~165 °C, antes de amargar). Espalhar imediatamente no fundo das 10 forminhas.", "Caçarola de cobre/inox", 165, 10),
                CookingStepSpec(2, "Aparelho Real sem Espuma", "Aquecer 625 g de leite com baunilha. Misturar sem incorporar ar 190 g ovos + 50 g gemas + 95 g açúcar, verter o leite morno aos poucos e passar no tamis fino retirando toda espuma superficial.", "Cul de poule + Tamis", 60, 10),
                CookingStepSpec(3, "Pochar a 80 °C Coberto com Filme", "Distribuir nas formas, cobrir com filme plástico resistente ao calor e pochar em banho-maria controlado a exatos 80 °C por ~40–45 min até firmar o centro tremulante (jamais ultrapassar 85 °C para não empolar!).", "Rôtissoire / Steamer", 80, 45),
                CookingStepSpec(4, "Maturação de Meio Dia a ≤ 5 °C", "Resfriar rápido e descansar refrigerado (≤ 5 °C) por pelo menos 12 horas para o caramelo liquefazer totalmente antes de desenformar com lâmina fina.", "Câmara Fria", 4, 720)
            )
        ),

        // 8) Geflügelgalantine & Protocolos Sous-vide (Sem. 17 — base 10 pess.)
        RecipeSpec(
            id = "gefluegelgalantine_v4",
            weekNumber = 17,
            cycleNumber = 3,
            axisId = 4,
            title = "Geflügelgalantine & Protocolos Sous-Vide",
            subtitle = "Desossa dorsal íntegra, farce no tamis sobre gelo (65 °C) + Bacalhau, Salmão e Vitela SV",
            baseServings = 10,
            cantonCode = "CH",
            isSwissmilkClassic = false,
            isChefkochV4 = true,
            requiredEquipmentIds = listOf("ausbeinmesser", "tamis", "cul_de_poule", "sous_vide_thermo"),
            pccTargetSummary = "Galantina: 65 °C internos | Sous-vide: Bacalhau 55 °C/2h30, Salmão 57,5 °C/125m, Vitela 62 °C/15h",
            ingredients = listOf(
                IngredientSpec("Frango inteiro p/ desossa dorsal (pele íntegra)", 1000.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Carne de coxa de ave gelada (p/ farce)", 350.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Creme de leite fresco 35% gelado (2 °C)", 300.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Frutas secas e pistaches marinados em Maraschino", 120.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Fundo claro de ave p/ pochar", 2500.0, "g", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Sal marinho (p/ salmouras Sous-vide 5–10%)", 100.0, "g", 1.0, "Especiarias & Fundos")
            ),
            steps = listOf(
                CookingStepSpec(1, "Desossa pelas Costas (Pele Íntegra)", "Com Ausbeinmesser afiada, abrir o frango de 1 kg longitudinalmente pelo dorso, desarticulando asas e coxas e retirando a carcaça sem perfurar a pele do peito. Manter a ≤ 4 °C.", "Ausbeinmesser", 4, 25),
                CookingStepSpec(2, "Farce Mousseline no Tamis sobre Gelo", "Processar a carne de coxa gelada com sal (solubilização da actomiosina), passar pelo tamis e incorporar o creme 35% em Cul de poule apoiado sobre gelo picado; envolver as frutas marinadas em Maraschino.", "Tamis + Cul de poule s/ gelo", 2, 20, SmartKitchenAlertType.GALANTINE_PCC_65C, "CONTROLE DE EMULSÃO: Nunca permita que a farce passe de 10 °C durante o batimento, ou a gordura separa na cocção!"),
                CookingStepSpec(3, "Enrolar & Pochar até 65 °C Internos", "Rechear a pele, enrolar firme em pano/filme + alumínio formando cilindro tenso. Pochar em fundo de ave a 75 °C até atingir EXATOS 65 °C no centro térmico com sonda. Resfriar em gelo sob leve prensa.", "Caldeirão + Sonda", 65, 55, SmartKitchenAlertType.GALANTINE_PCC_65C, "PCC GALANTINA: Centro térmico obrigatório de 65 °C seguido de resfriamento rápido em água e gelo (≤ 3 °C)."),
                CookingStepSpec(4, "Protocolos Sous-Vide Anexos (Bacalhau, Salmão, Vitela)", "Aplicar salmoura prévia 5–10% (10 min p/ peixes; 3h p/ peito de vitela) e ajustar banho +0,5 a +1 °C acima do alvo: Filé de Bacalhau 55 °C / 2h30 | Salmão 25 mm 57,5 °C / 125 min | Peito de Vitela 62 °C / 15 h.", "Sous-vide-Garer", 62, 150, SmartKitchenAlertType.SOUS_VIDE_PROTOCOL, "PROTOCOLO SOUS-VIDE: Salmoura 50–100 g/L (10m peixes / 3–4h carnes); peças ≤ 7 cm sem ossos; banho +0,5 a +1 °C.")
            )
        )
    )
}
