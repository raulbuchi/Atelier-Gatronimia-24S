package com.example.data.curriculum

import com.example.domain.model.BrazilRegionMatrixSpec
import com.example.domain.model.ItalianFoundationRuleSpec
import com.example.domain.model.WorldAromaticBaseSpec
import com.example.domain.model.WorldSpiceBlendSpec

/**
 * Atlas Cultural & Científico Rápido (Especificação v5):
 * 1) Fundamentos da Cozinha Italiana Clássica (Marcella Hazan)
 * 2) Matrizes Regionais da Cozinha Brasileira (Luis Carlos Paravati & Carlos Alberto Dória)
 * 3) Volta ao Mundo: 7 Refogados Aromáticos & 9 Misturas de Especiarias (Mina Holland — 39 Cozinhas)
 */
object CulturalScientificAtlasV5 {

    val italianFoundationsHazan: List<ItalianFoundationRuleSpec> = listOf(
        ItalianFoundationRuleSpec(
            id = "hazan_battuto_soffritto_insaporire",
            pillarTitle = "1. A Tríade de Construção de Sabor",
            italianTerm = "Battuto → Soffritto → Insaporire",
            technicalPrinciple = "O battuto (vegetais e/ou lardo/pancetta picados na faca sobre tábua) evolui lentamente em gordura controlada até tornar-se soffritto translúcido e adocicado; em seguida, o ingrediente principal entra para insaporire (envolver-se e absorver a base aromática antes de qualquer líquido).",
            benchErrorToAvoid = "Dourar rápido em fogo alto (queima açúcares da cebola/alho) ou jogar caldo antes da etapa de insaporire."
        ),
        ItalianFoundationRuleSpec(
            id = "hazan_acqua_extraction",
            pillarTitle = "2. Cozimento com Água Pura (Acqua) vs. Caldo",
            italianTerm = "Cottura in Acqua & Deglaçagem Limpa",
            technicalPrinciple = "Marcella Hazan demonstra que pequenas adições de água quente em braseados e ragus permitem concentrar o sabor puro do próprio insumo sem mascará-lo com fundos industrializados ou excessivamente gelatinosos.",
            benchErrorToAvoid = "Afogar carnes em excesso de líquido desde o início em vez de evaporar e caramelizar ciclos curtos."
        ),
        ItalianFoundationRuleSpec(
            id = "hazan_fresh_vs_dry_pasta",
            pillarTitle = "3. Massa Fresca ao Ovo vs. Massa Seca de Grano Duro",
            italianTerm = "Sfoglia all'Uovo (T00) vs. Pasta di Semola",
            technicalPrinciple = "Massa fresca emiliana leva EXCLUSIVAMENTE farinha de trigo mole (Tipo 00) e ovos inteiros — SEM sal, SEM azeite/óleo e SEM água — criando superfície micro-rugosa e porosa perfeita para molhos de manteiga, creme e ragu. Já a massa seca de sêmola de grano duro exige molhos de azeite extravirgem, alho, frutos do mar e vegetais.",
            benchErrorToAvoid = "Adicionar óleo ou sal na massa fresca ao ovo: o óleo impermeabiliza a massa (o molho escorre!) e o sal endurece a rede de glúten dificultando a abertura no rolo."
        ),
        ItalianFoundationRuleSpec(
            id = "hazan_risotto_onda_vs_piemonte",
            pillarTitle = "4. Arquitetura do Risotto Regional",
            italianTerm = "Risotto all'Onda (Vêneto) vs. Mantecato Estruturado (Piemonte)",
            technicalPrinciple = "Tostatura do arroz superfino (Carnaroli/Vialone Nano) seguida de adições graduais de caldo fervente sob agitação para liberar amilopectina superficial. No Vêneto serve-se fluido (all'onda, que se espalha suavemente no prato); no Piemonte/Lombardia serve-se mais encorpado com mantecatura vigorosa fora do fogo (manteiga gelada + Parmigiano).",
            benchErrorToAvoid = "Lavar o arroz antes da tostatura (remove o amido superficial) ou fazer a mantecatura com a panela ainda sobre a chama acesa (separa a gordura da manteiga)."
        )
    )

    const val BRAZIL_BIODIVERSITY_CHAIN =
        "CADEIA ESTRUTURAL BRASILEIRA (Paravati & Dória): Biodiversidade de Bioma → Ingredientes Nativos → Produtos Processados Tradicionais (ex.: mandioca brava → tucupi/goma/farinha d'água) → Receitas Patrimoniais."

    val brazilFiveRegionsMatrices: List<BrazilRegionMatrixSpec> = listOf(
        BrazilRegionMatrixSpec(
            regionName = "1. Norte (Bioma Amazônico)",
            signatureIngredients = "Mandioca-brava (Manihot esculenta), jambú (Acmella oleracea), pimenta-de-cheiro, pirarucu, tambaqui, tucumã, castanha-do-brasil.",
            derivedProducts = "Tucupi amarelo fermentado e fervido, goma de tapioca, farinha d'água de Bragança, maniva moída cozida por 7 dias.",
            emblemRecipes = "Tacacá na cuia, Pato no Tucupi com Jambú, Maniçoba paraense, Caldeirada de Tambaqui.",
            technicalControlNote = "PCC Toxicológico: o tucupi cru contém glicosídeos cianogênicos (ácido cianídrico volátil) — exigir fervura prolongada validada; maniva da maniçoba exige 7 dias de cocção térmica."
        ),
        BrazilRegionMatrixSpec(
            regionName = "2. Nordeste (Recôncavo, Litoral & Sertão)",
            signatureIngredients = "Feijão-fradinho, azeite de dendê, camarão seco defumado, mandioca, vinagreira (hibisco), carne de sol, manteiga de garrafa.",
            derivedProducts = "Massa de acarajé batida aerada (feijão-fradinho descascado), aguardente de mandioca Tiquira (MA), leite de coco fresco.",
            emblemRecipes = "Acarajé (frito no dendê) vs. Abará (cozido no vapor em folha de bananeira), Arroz de Cuxá maranhense (vinagreira + gergelim), Baião de Dois.",
            technicalControlNote = "Controle de fritura no dendê: evitar fumaça/pirólise (> 180 °C) que degrada carotenoides e gera acroleína irritante."
        ),
        BrazilRegionMatrixSpec(
            regionName = "3. Sudeste (Matrizes Caiçara, Mineira & Capixaba)",
            signatureIngredients = "Banana-verde/nanica caiçara, ora-pro-nóbis (Pereskia aculeata — PANC proteica), urucum, robalo/badejo, fubá de moinho d'água, porco caipira.",
            derivedProducts = "Tintura oleosa de urucum, queijo minas artesanal curado, polvilho azedo fermentado ao sol.",
            emblemRecipes = "Peixe Azul-Marinho caiçara (ácido tânico da banana-verde + ferro da panela geram tom azulado), Frango com Ora-pro-nóbis, Moqueca Capixaba na panela de barro (sem dendê e sem leite de coco).",
            technicalControlNote = "Na ora-pro-nóbis, saltear rapidamente em alta temperatura ou incorporar no final sem mexer em excesso para controlar a mucilagem natural."
        ),
        BrazilRegionMatrixSpec(
            regionName = "4. Sul (Litoral Paranaense, Pampa & Serra)",
            signatureIngredients = "Cortes bovinos dianteiros ricos em colágeno (acém, peito, músculo), toucinho, cominho, louro, pinhão, erva-mate.",
            derivedProducts = "Farinha de mandioca fina de Morretes (para vedação e pirão escaldado), charque pampeano curado e desidratado.",
            emblemRecipes = "Barreado Paranaense (cocção lenta por 12–16 h em panela de barro 'barreada'/vedada com goma de farinha e cinza), Arroz de Carreteiro de Charque.",
            technicalControlNote = "No Barreado, a vedação hermética retém vapor aromático a ~98 °C hidrolisando 100% do colágeno até as fibras se desfiarem sem secar."
        ),
        BrazilRegionMatrixSpec(
            regionName = "5. Centro-Oeste (Pantanal & Cerrado)",
            signatureIngredients = "Pintado, pacu, pequi (Caryocar brasiliense), guariroba (palmito amargo), mandioca mansa, banana-da-terra.",
            derivedProducts = "Óleo e conserva de pequi, farinha de jatobá, paçoca pantaneira de carne seca pilada.",
            emblemRecipes = "Mojica de Pintado com Mandioca (ensopado pantaneiro engrossado pelo próprio amido da mandioca), Arroz com Pequi e Frango Caipira.",
            technicalControlNote = "Na Mojica de Pintado, os cubos de peixe entram apenas nos minutos finais sobre a mandioca já macia para atingir ≥ 63 °C sem desmanchar."
        )
    )

    val worldSevenAromaticBases: List<WorldAromaticBaseSpec> = listOf(
        WorldAromaticBaseSpec(
            "mirepoix_fr", "1. Mirepoix Clássico", "França (Escoffier / Bocuse)",
            "50% cebola + 25% cenoura + 25% salsão (aipo); no Matignon soma-se presunto cru e tomilho.",
            "Suado lentamente em manteiga clarificada (fundos claros) ou dourado com pinçage de tomate (fundos escuros)."
        ),
        WorldAromaticBaseSpec(
            "soffritto_it", "2. Soffritto Italiano", "Itália (Marcella Hazan)",
            "Cebola + cenoura + salsão em battuto fino (frequentemente acrescido de alho, salsinha ou pancetta conforme a região).",
            "Cozimento lento e paciente em azeite extravirgem e/ou manteiga até transparência doce, sem escurecer."
        ),
        WorldAromaticBaseSpec(
            "refogado_br", "3. Refogado Brasileiro", "Brasil (Paravati & Dória)",
            "Cebola brunoise suada primeiro + alho amassado com sal adicionado em seguida (+ tomate concassé, cheiro-verde ou urucum/dendê).",
            "Banha suína, manteiga de garrafa, óleo vegetal neutro ou azeite de dendê; o alho entra após a cebola murchar para não amargar."
        ),
        WorldAromaticBaseSpec(
            "sofrito_es_latam", "4. Sofrito Espanhol & Caribenho (Recaito)", "Espanha & América Latina",
            "Espanha: cebola, alho, pimentão e tomate ralado reduzido com páprica (pimentón); Caribe: coentro-bravo (culantro), pimenta-doce (ají dulce), alho e cebola.",
            "Azeite de oliva reduzido quase a compota concentrada para paellas, arrozes e ensopados."
        ),
        WorldAromaticBaseSpec(
            "holy_trinity_cajun", "5. Holy Trinity (Santíssima Trindade Cajun)", "Louisiana / Crioula-Cajun (EUA)",
            "Partes iguais ou 2:1:1 de cebola, salsão (aipo) e pimentão-verde em cubos regulares (+ alho, chamado 'o Papa').",
            "Adicionada diretamente sobre um roux escuro cor de chocolate para gumbos, jambalayas e étouffées."
        ),
        WorldAromaticBaseSpec(
            "tadka_tempering_in", "6. Tadka / Chhonk (Tempering de Especiarias)", "Índia (Norte e Sul)",
            "Sementes inteiras (cominho, mostarda-preta, feno-grego) + folhas de curry, malagueta seca, gengibre, alho e assafétida.",
            "Estaladas em ghee ou óleo quente no início ou vertidas chiando sobre dals e curries na finalização para extrair óleos essenciais lipossolúveis."
        ),
        WorldAromaticBaseSpec(
            "trinity_cn", "7. Trindade Aromática Chinesa", "China (Cantão & Sichuan)",
            "Gengibre fresco em julienne/lâminas + alho levemente esmagado + parte branca da cebolinha (scallion) (+ pasta doubanjiang em Sichuan).",
            "Infusão relâmpago (10–15 s) em óleo de alto ponto de fumaça na wok bem quente antes dos vegetais em corte oblíquo (Rhomben)."
        )
    )

    val worldNineSpiceBlends: List<WorldSpiceBlendSpec> = listOf(
        WorldSpiceBlendSpec(
            "ras_el_hanout", "1. Ras el Hanout", "Marrocos / Magrebe",
            "Cardamomo, cominho, coentro, canela, noz-moscada, macis, gengibre, cúrcuma, pimenta-da-jamaica e botões de rosa secos.",
            "Tajines de cordeiro ou frango com damascos/amêndoas, cuscuz marroquino e marinadas quentes."
        ),
        WorldSpiceBlendSpec(
            "berbere", "2. Berbere", "Etiópia & Eritreia",
            "Pimentas vermelhas secas, feno-grego, korarima (cardamomo etíope), ajwain, nigella, gengibre, alho seco, cravo e canela.",
            "Base estrutural do Doro Wat (ensopado de frango) e guisados de lentilhas vermelhas."
        ),
        WorldSpiceBlendSpec(
            "baharat", "3. Baharat (Sete Especiarias Árabes)", "Levante & Golfo Árabe",
            "Pimenta-do-reino, pimenta-da-jamaica, canela, cravo, cominho, coentro em grão, cardamomo e noz-moscada.",
            "Kaftas, quibes, arrozes festivos com cordeiro, recheios de charutos de folha de uva e legumes."
        ),
        WorldSpiceBlendSpec(
            "dukkah", "4. Dukkah", "Egito",
            "Avelãs ou amêndoas tostadas picadas grossas, sementes de gergelim, coentro em grão tostado, cominho e flor de sal.",
            "Condimento crocante de finalização para pães com azeite, peixes assados, ovos pochê e vegetais tostados."
        ),
        WorldSpiceBlendSpec(
            "garam_masala", "5. Garam Masala", "Norte da Índia",
            "Canela, cardamomo verde e preto, cravo, cominho tostado, louro indiano (tejpatta), macis e pimenta-do-reino.",
            "Polvilhado nos minutos finais da cocção para preservar notas florais voláteis sem amargar."
        ),
        WorldSpiceBlendSpec(
            "panch_phoron", "6. Panch Phoron (5 Sementes Inteiras)", "Bengala / Leste da Índia",
            "Partes iguais de sementes inteiras (NUNCA moídas): feno-grego, nigella (kalonji), cominho, mostarda-preta e erva-doce.",
            "Estalado em óleo de mostarda ou ghee para legumes, lentilhas (dal) e chutneys agridoce-picantes."
        ),
        WorldSpiceBlendSpec(
            "zaatar", "7. Za'atar", "Líbano, Palestina & Síria",
            "Hissopo/orégano-sírio seco, sumagre ácido vermelho-rubi, gergelim branco tostado e sal marinho.",
            "Misturado com azeite extravirgem sobre manakish, labneh (coalhada seca), aves assadas e abóboras."
        ),
        WorldSpiceBlendSpec(
            "advieh", "8. Advieh Persa", "Irã (Pérsia)",
            "Pétalas de rosa secas, canela, cardamomo verde, cominho, coentro, gengibre e limão-negro seco (loomi) ou açafrão.",
            "Arrozes persas amanteigados com crosta tahdig, ensopados khoresh e carnes grelhadas."
        ),
        WorldSpiceBlendSpec(
            "chinese_five_spice", "9. Cinco Especiarias Chinesas (Wu Xiang Fen)", "China & Sudeste Asiático",
            "Anis-estrelado, pimenta-de-Sichuan (huajiao), canela-cássia, cravo-da-índia e sementes de erva-doce.",
            "Pato laqueado, barriga suína braseada (Hong Shao Rou), caldos master stock e marinadas agridoce-umami."
        )
    )
}
