package com.example.data.curriculum

import com.example.domain.model.BrigadeStationSpec
import com.example.domain.model.CookwareBladeSpec
import com.example.domain.model.FiberEquivalentSpec
import com.example.domain.model.FlashcardSpec
import com.example.domain.model.OrgChartModelSpec
import com.example.domain.model.SwissBeefPrimalSpec
import com.example.domain.model.SwissCutSpec

object TechnicalAtlasCatalog {

    // 4.1 Matriz de Cortes Padronizados (Schnittarten) — 12 cortes oficiais
    val swissCuts: List<SwissCutSpec> = listOf(
        SwissCutSpec("brunoise", "Brunoise", "Cubos de ~1,5 mm de aresta", "Chefmesser (Faca do Chef bem afiada)", "Cenoura, alho-poró, aipo-rábano, cogumelos, pimentão, Bündnerfleisch"),
        SwissCutSpec("jardiniere", "Jardinière", "Cubos de ~5 mm de aresta", "Chefmesser", "Cenoura, aipo-rábano, pepino, couve-rábano (Kohlrabi), bacon cozido"),
        SwissCutSpec("macedoine", "Macédoine", "Cubos de ~8 mm de aresta", "Chefmesser", "Legumes (abóbora, cenoura, aipo) e frutas (pêssego, manga, maçã, pera)"),
        SwissCutSpec("paysanne", "Paysanne", "Lâminas finas quadradas/triangulares de ~1 cm", "Chefmesser", "Cenoura, alho-poró, repolho crespo (Wirz), aipo, batata para sopas rústicas"),
        SwissCutSpec("julienne", "Julienne", "Tiras finas de ~1 mm × 3 cm", "Chefmesser", "Cenoura, alho-poró, aipo, cogumelos, presunto cru/cozido"),
        SwissCutSpec("batonnets", "Bâtonnets", "Bastões de ~5 mm × 3 cm", "Chefmesser", "Batatas (fritura dupla ≤ 175 °C), cenouras, couve-rábano, bacon cozido"),
        SwissCutSpec("chiffonnade", "Chiffonnade", "Tiras finíssimas em fita enrolada", "Chefmesser afiada (sem esmagar)", "Folhas delicadas: alface, alho-de-urso (Bärlauch), azedinha, manjericão"),
        SwissCutSpec("demidoff", "Demidoff", "Rodelas caneladas/onduladas de ~5 mm", "Buntschneidemesser (faca ondulada)", "Cenoura, pepino, abobrinha, aipo-rábano estufados"),
        SwissCutSpec("rhomben", "Rhomben", "Losangos oblíquos (~1–1,5 cm)", "Gemüsemesser / Santoku", "Alho-poró, cenoura, salsão, pimentão e ervilha-torta para salteado em Wok/Antihaft"),
        SwissCutSpec("gemueseperlen", "Gemüseperlen (Parisienne)", "Esferas torneadas com boleador", "Pariser Ausstecher (Boleador)", "Melão, batata, cenoura, abobrinha glacê"),
        SwissCutSpec("tourniertes", "Tourniertes Gemüse", "Oval alongado de ~3 cm com 7 faces iguais", "Tourniermesser (Faca de tornear curva)", "Cenoura, aipo, pepino, abobrinha, beterraba e batata inglesa clássica"),
        SwissCutSpec("concasses", "Concassés", "Cubos regulares de ~8 mm sem pele e sem sementes", "Officemesser / Rüstmesser", "Tomate branqueado 10s, chocado em gelo, pelado, despolpado e cubado")
    )

    const val DULL_BLADE_QUALITY_RULE =
        "REGRA DE SEGURANÇA & QUALIDADE (Schnittarten): Facas cegas esmagam paredes celulares vegetais, aumentam drasticamente a perda de suco celular na tábua, aceleram a oxidação enzimática e elevam o risco de escorregamento e acidentes."

    // 4.2 Atlas de Bateria e Lâminas (Kochgeschirrarten) + 4 Níveis de Equipamento
    val cookwareAndBlades: List<CookwareBladeSpec> = listOf(
        CookwareBladeSpec("rotissoire", "Assadeira funda de forno", "Bratgeschirr", "Rôtissoire", "Bateria Térmica", 2,
            "Dourar ossos sem gordura no forno (ex.: Brauner Kalbsfond) e assados grandes abertos."),
        CookwareBladeSpec("braisiere", "Caçarola alta de brasear c/ tampa", "Schmorgeschirr", "Braisière", "Bateria Térmica", 2,
            "Brasear carnes ricas em colágeno (Schmoren) e Poelieren (140–160 °C tampado sobre matignon sem líquido)."),
        CookwareBladeSpec("rondeau", "Caçarola baixa e larga c/ 2 alças", "Niedriger Bratentopf", "Rondeau", "Bateria Térmica", 3,
            "Selar carnes cuja borra (sucs) gera molho escuro, Poelieren e cozimentos rasos uniformes."),
        CookwareBladeSpec("sautoir", "Caçarola rasa de paredes retas e cabo longo", "Flachkasserolle", "Sautoir", "Bateria Térmica", 2,
            "Saltear proteínas (ex.: Zürcher Geschnetzeltes, Bison-Mignons) para construir molho na deglaçagem."),
        CookwareBladeSpec("sauteuse", "Frigideira alta de bordas cônicas/inclinadas", "Schwenkkasserolle", "Sauteuse", "Bateria Térmica", 2,
            "Saltear agitando (tossing) e reduzir/emulsionar molhos delicados com movimento circular."),
        CookwareBladeSpec("poele_antiadhesif", "Frigideira antiaderente", "Bratpfanne (Antihaft)", "Poêle antiadhésif", "Bateria Térmica", 1,
            "OBRIGATÓRIA para saltear vegetais (frigideira de aço puro destrói a vitamina C!), ovos e peixes delicados."),
        CookwareBladeSpec("poele_lyonnaise", "Frigideira de ferro/aço carbono", "Lyoner Pfanne", "Poêle lyonnaise", "Bateria Térmica", 2,
            "Saltear carnes/batatas Rösti em alta temperatura SEM construir molho na panela; tostar cebola cortada ao meio (Berner Platte)."),
        CookwareBladeSpec("poissonniere", "Peixeira alongada com grelha interna", "Fischkessel", "Poissonnière", "Bateria Térmica", 4,
            "Pochar peixes inteiros delicados em court-bouillon a 75–80 °C sem quebrar ao retirar."),
        CookwareBladeSpec("cul_de_poule", "Tigela hemisférica de inox", "Schlagschüssel", "Cul de poule", "Confeitaria & Precisão", 1,
            "Emulsões, sabayon em banho-maria, claras em neve e resfriamento de farce mousseline sobre gelo."),
        CookwareBladeSpec("chefmesser", "Faca do Chef 20–24 cm", "Kochmesser", "Couteau de chef", "Lâminas & Corte", 1,
            "Cortes geométricos base: Brunoise, Jardinière, Macédoine, Julienne, Bâtonnets e Chiffonnade."),
        CookwareBladeSpec("ruestmesser", "Faca de ofício / descascar reta", "Rüstmesser", "Couteau d'office", "Lâminas & Corte", 1,
            "Limpeza precisa de legumes, aparas finas, pelar tomates Concassés e incisões de 2 mm (Bison-Mignons)."),
        CookwareBladeSpec("tourniermesser", "Faca de tornear bico de pássaro", "Tourniermesser", "Couteau à tourner", "Lâminas & Corte", 2,
            "Torneamento clássico de 7 faces de ~3 cm (Tourniertes Gemüse)."),
        CookwareBladeSpec("santoku", "Faca japonesa alveolar", "Santoku", "Couteau Santoku", "Lâminas & Corte", 2,
            "Lâminas ultrafinas vegetais e cortes em losango (Rhomben) para wok."),
        CookwareBladeSpec("ausbeinmesser", "Faca de desossa rígida/semiflexível", "Ausbeinmesser", "Couteau à désosser", "Lâminas & Corte", 3,
            "Desossa dorsal íntegra de aves (Geflügelgalantine) e limpeza de membranas argênteas no Rind."),
        CookwareBladeSpec("filetiermesser", "Faca de filetar longa e flexível", "Filetiermesser", "Couteau à fileter", "Lâminas & Corte", 3,
            "Filetagem precisa de perca, truta alpina (Rötel) e salmão sem dilacerar miômeros."),
        CookwareBladeSpec("buntschneidemesser", "Faca ondulada decorativa", "Buntschneidemesser", "Coupe-légumes ondulé", "Lâminas & Corte", 3,
            "Corte padronizado Demidoff (~5 mm) e batatas gaufrettes."),
        CookwareBladeSpec("kanneliermesser", "Canelador de cítricos e vegetais", "Kanneliermesser", "Canneleur", "Lâminas & Corte", 3,
            "Sulcos longitudinais em limão, cenoura e pepino (rodelas histriadas)."),
        CookwareBladeSpec("zestenmesser", "Zester extrator de fios cítricos", "Zestenmesser", "Zesteur", "Lâminas & Corte", 2,
            "Extração do flavedo aromático sem a parte branca amarga (albedo)."),
        CookwareBladeSpec("tamis", "Peneira fina de tambor", "Haarsieb / Passiersieb", "Tamis", "Confeitaria & Precisão", 3,
            "Passar farce mousseline da Geflügelgalantine e purês sedosos eliminando fibras conjuntivas."),
        CookwareBladeSpec("sous_vide_thermo", "Termocirculador + Seladora a Vácuo + Sonda", "Sous-vide-Garer & Kerntemperaturfühler", "Thermoplongeur & Sonde", "Confeitaria & Precisão", 4,
            "Banhos precisos (±0,1 °C) para protocolos Sous-vide e auditoria de PCC no centro térmico.")
    )

    // 4.3 Mapa Anatômico Bovino Suíço (Rind A–H)
    val swissBeefPrimals: List<SwissBeefPrimalSpec> = listOf(
        SwissBeefPrimalSpec(
            "A", "Stotzen", "Cuisse", "Alcatra / Coxão Traseiro Completo",
            "Médio a Baixo tecido conectivo (varia por subcorte)",
            listOf("Huft (Alcatra macia — muitas vezes agrupada c/ Nierstück)", "Nuss (Patinho)", "Eckstück (Coxão mole)", "Unterspälte (Coxão duro)", "Runder Mocken (Lagarto)", "Rosenstück", "Weisses Stück", "Schenkel (Músculo traseiro — alto colágeno)"),
            listOf("Braten (Eckstück/Nuss)", "Schmoren (Unterspälte/Runder Mocken)", "Sieden / Ossobuco (Schenkel)")
        ),
        SwissBeefPrimalSpec(
            "B", "Nierstück", "Aloyau", "Lombo Nobre (Contrafilé, Filé Mignon & Alcatra)",
            "Baixíssimo tecido conectivo (máxima maciez)",
            listOf("Filet: Filetkopf (Chateaubriand), Filetherz (Tournedos/Medaillons), Filetspitz (Mignons / Geschnetzeltes)", "Roastbeef: Entrecote, Doppeltes Entrecote", "Com osso: Kotelett, Club-Steak, T-Bone, Porterhouse", "Huft: Huftdeckel (Picanha suíça — excelente p/ Bollito misto e grelha), Huftzapfen"),
            listOf("Sautieren", "Grillieren", "Braten (Roastbeef rosado 54 °C)", "Sieden nobre (Huftdeckel no Bollito misto)")
        ),
        SwissBeefPrimalSpec(
            "C", "Lempen", "Flanc", "Fraldinha / Vazio Abdominal",
            "Alto tecido conectivo com fibras longas saborosas",
            listOf("Dünner Lempen (Bavette)", "Dicker Lempen"),
            listOf("Sieden (caldos ricos)", "Schmoren", "Grillieren rápido (apenas Bavette limpa contra a fibra)")
        ),
        SwissBeefPrimalSpec(
            "D", "Rücken (Hohrücken)", "Train de côtes", "Acém Nobre / Costela do Lombo",
            "Médio tecido conectivo bem marmorizado",
            listOf("Hohrücken (Ribeye / Côte de boeuf)", "Hohrücken-Steak"),
            listOf("Braten", "Grillieren", "Sieden clássico (Siedfleisch Hohrücken ~90 min na Berner Platte)")
        ),
        SwissBeefPrimalSpec(
            "E", "Schulter", "Épaule", "Paleta Bovina",
            "Médio-Alto tecido conectivo gelatinoso",
            listOf("Dicke Schulter", "Schulterfilet (Peixinho)", "Schulterspitz", "Bugstück", "Schenkel dianteiro"),
            listOf("Schmoren (Brasato al Merlot)", "Sieden", "Ragout / Voressen")
        ),
        SwissBeefPrimalSpec(
            "F", "Federstück", "Côte plate", "Costela Dianteira / Ponta de Agulha",
            "Alto teor de colágeno e gordura entremeada",
            listOf("Federstück c/ osso (Short Ribs)", "Federstück desossado"),
            listOf("Sieden lento (Pot-au-feu / Siedfleisch)", "Schmoren longo")
        ),
        SwissBeefPrimalSpec(
            "G", "Brust", "Poitrine", "Peito Bovino",
            "Alto tecido conectivo e fáscia peitoral",
            listOf("Brustkern (Ponta de peito)", "Brustspitz", "Nachbrust"),
            listOf("Sieden (Siedfleisch clássico)", "Sous-vide longo (15–24h)", "Schmoren")
        ),
        SwissBeefPrimalSpec(
            "H", "Hals", "Cou", "Pescoço Bovino",
            "Altíssimo tecido conectivo (músculo de suporte intenso)",
            listOf("Rindshals inteiro", "Cubos p/ Voressen e carne moída"),
            listOf("Schmoren (Goulash / Voressen)", "Hackfleisch (Carne moída PCC ≥ 71 °C)")
        )
    )

    // 4.4 Nutrição (Ernaehrung) — Equivalências para 10 g de fibra (meta diária ≥ 30 g)
    val fiberEquivalentsFor10g: List<FiberEquivalentSpec> = listOf(
        FiberEquivalentSpec("Farelo de trigo integral (Weizenkleie)", 20, "Enriquecimento de pães rústicos, panquecas e farofas funcionais."),
        FiberEquivalentSpec("Damasco seco (Dörraprikosen)", 50, "Excelente em compotas alpinas, recheio de galantina e matinais."),
        FiberEquivalentSpec("Feijão branco seco (Weisse Bohnen)", 55, "Base de cassoulets, minestrone ticinese e saladas mornas."),
        FiberEquivalentSpec("Lentilhas secas (Linsen)", 100, "Guarnição clássica de caça, cotechino e proteínas vegetais."),
        FiberEquivalentSpec("Pão Graham integral (Grahambrot)", 150, "Panificação suíça de moagem integral sem peneiramento.")
    )

    // 4.4 Estrutura Clássica de Brigada (13 Postos — Betriebsorganisation)
    val classicBrigade13Stations: List<BrigadeStationSpec> = listOf(
        BrigadeStationSpec(1, "Küchenchef", "Chef de cuisine", "Chef Executivo", "Planejamento de menus, fichas técnicas, BAB/Prime-Cost, compras estratégicas, higiene HACCP e liderança geral."),
        BrigadeStationSpec(2, "Sous-chef", "Sous-chef de cuisine", "Subchef Executivo", "Substituto direto do Küchenchef; comanda o passe (annonce), controla qualidade dos pratos e treina a brigada."),
        BrigadeStationSpec(3, "Saucier", "Chef saucier", "Chefe de Molhos e Salteados", "Fundos escuros/claros, molhos-mãe e derivados, carnes/aves/peixes salteados, braseados e poelieren."),
        BrigadeStationSpec(4, "Rôtisseur", "Chef rôtisseur", "Chefe de Assados e Grelhados", "Carnes e aves assadas no forno, grelhados (Grillieren), caça e frituras por imersão (≤ 175 °C)."),
        BrigadeStationSpec(5, "Entremetier", "Chef entremetier", "Chefe de Guarnições, Sopas e Ovos", "Sopas, vegetais (em Antihaft-Pfanne!), batatas (Rösti, Gratin), massas (Spätzli), arroz e pratos de ovos."),
        BrigadeStationSpec(6, "Garde-manger", "Chef garde-manger", "Chefe da Cozinha Fria e Açougue", "Porcionamento/desossa de carnes e peixes, galantinas/terrinas, molhos frios (Salsa verde), saladas e buffets frios."),
        BrigadeStationSpec(7, "Pâtissier", "Chef pâtissier", "Chefe de Confeitaria e Panificação", "Massas folhadas (Blätterteig), tortas suíças, suflês (Vanilleauflauf), cremes pochados, caldas e sobremesas geladas."),
        BrigadeStationSpec(8, "Tournant", "Chef tournant", "Chefe Volante Polivalente", "Domina todas as praças quentes e frias para cobrir folgas e picos operacionais."),
        BrigadeStationSpec(9, "Chef de garde (Wache)", "Chef de garde", "Cozinheiro de Plantão / Fora de Horário", "Assegura o serviço contínuo entre turnos (room service, chegadas tardias) e monitora câmaras frias."),
        BrigadeStationSpec(10, "Mitarbeiterkoch", "Cuisinier du personnel", "Cozinheiro da Equipe (Família)", "Planeja e executa refeições nutritivas, variadas e de custo controlado para os colaboradores do estabelecimento.", isHighlightedSpecialRole = true),
        BrigadeStationSpec(11, "Diätkoch", "Cuisinier en diététique", "Cozinheiro Dietético Especializado", "Adapta cardápios clínicos, alergênicos severos, texturas modificadas e metas metabólicas (fibras ≥ 30g, sódio, DM2).", isHighlightedSpecialRole = true),
        BrigadeStationSpec(12, "Lernender (EFZ / EBA)", "Apprenti cuisinier", "Aprendiz em Formação Dual Suíça", "Roda sistematicamente por todas as praças executando Ficha MEP, portfólio e treinos práticos supervisionados."),
        BrigadeStationSpec(13, "Hilfskoch / Küchenangestellter", "Aide de cuisine", "Auxiliar Operacional de Cozinha", "Apoia pré-preparo higienizado, higienização de bateria (plonge) e organização FIFO de recebimento.")
    )

    val operationalOrgCharts: List<OrgChartModelSpec> = listOf(
        OrgChartModelSpec(
            "org_hotel",
            "1. Grande Hotel (Hotelaria Completa & Banquetes)",
            "Küchenchef → Sous-chefs (Restaurante + Banquetes) → Todas as praças especializadas ativas (Saucier, Rôtisseur, Entremetier, Garde-manger, Pâtissier, Chef de garde, Mitarbeiterkoch e Tournant).",
            listOf("Küchenchef", "Sous-chef", "Saucier", "Rôtisseur", "Entremetier", "Garde-manger", "Pâtissier", "Chef de garde", "Mitarbeiterkoch", "Tournant", "Lernender EFZ"),
            "Alta especialização e capacidade simultânea (à la carte + room service 24h + eventos), porém elevado custo fixo de folha (Personalkosten)."
        ),
        OrgChartModelSpec(
            "org_restaurant",
            "2. Restaurante Gastronômico / Bistrô Independente",
            "Küchenchef (atua no passe + Saucier) → Fusão de praças: Quente Proteínas (Saucier+Rôtisseur), Quente Guarnições (Entremetier), Fria/Sobremesas (Garde-manger+Pâtissier) + Lernender.",
            listOf("Küchenchef", "Sous-chef / Saucier", "Entremetier", "Garde-manger + Pâtissier", "Tournant", "Lernender EFZ"),
            "Estrutura enxuta, ágil e com menor Prime-Cost; exige alta polivalência e MEP rigoroso antes do serviço."
        ),
        OrgChartModelSpec(
            "org_collective",
            "3. Restauração Coletiva, Hospitalar & Clínicas",
            "Küchenleiter (Gestor de Produção) → Produção em Volume (Sieden/Dämpfen/Sous-vide Cook-and-Chill), Praça Dietética obrigatória (Diätkoch), Porcionamento em Esteira e Logística Térmica.",
            listOf("Küchenchef / Betriebsleiter", "Diätkoch (Central)", "Entremetier / Caldeirões", "Garde-manger", "Pâtissier", "Hilfskoch"),
            "Horários regulares, compras centralizadas de alto giro e foco absoluto em rastreabilidade HACCP, nutrição (≥ 30g fibras) e dietoterapia."
        )
    )

    // 48+ Flashcards SM-2 cobrindo todas as semanas + novos cards obrigatórios da v4
    val flashcards: List<FlashcardSpec> = listOf(
        FlashcardSpec("fc_01", 1, "Schnittarten", "Qual a dimensão oficial suíça do corte Brunoise e qual o efeito de uma faca cega?", "Cubos de ~1,5 mm de aresta. Facas cegas esmagam células vegetais, aumentando perda de suco na tábua e oxidação enzimática."),
        FlashcardSpec("fc_02", 1, "Schnittarten", "Diferencie Jardinière, Macédoine e Concassés no padrão suíço.", "Jardinière: cubos ~5 mm; Macédoine: cubos ~8 mm (legumes/frutas); Concassés: cubos ~8 mm de tomate pelado e sem sementes."),
        FlashcardSpec("fc_03", 1, "Schnittarten", "Quais as medidas de Julienne e Bâtonnets?", "Julienne: tiras de ~1 mm × 3 cm. Bâtonnets: bastões de ~5 mm × 3 cm."),
        FlashcardSpec("fc_04", 1, "Schnittarten", "Quais ferramentas específicas exigem os cortes Demidoff e Tourniertes Gemüse?", "Demidoff (~5 mm ondulado): Buntschneidemesser. Tourniertes Gemüse (~3 cm, 7 faces): Tourniermesser."),
        FlashcardSpec("fc_05", 1, "Higiene", "Qual a eficácia da lavagem simples vs. desinfecção das mãos na cozinha?", "Lavagem correta remove ~99% da flora transitória; desinfecção alcoólica aplicada APÓS secagem completa atinge 99,99%."),
        FlashcardSpec("fc_06", 1, "Higiene", "Defina a Zona de Perigo térmico e o tempo máximo acumulado tolerado.", "5 °C a 60 °C, por no máximo 4 horas acumuladas. Refrigeração segura deve ser mantida ≤ 5 °C."),
        FlashcardSpec("fc_07", 2, "Bateria & Toxinas", "Por que é proibido saltear vegetais ricos em vitamina C em frigideira de aço puro (Lyoner Pfanne)?", "Íons metálicos do aço puro catalisam a oxidação e destroem rapidamente a vitamina C, além de alterar cor e sabor; usar obrigatoriamente Poêle antiadhésif (Antihaft-Pfanne)."),
        FlashcardSpec("fc_08", 2, "Kochgeschirrarten", "Traduza para Alemão e Francês a caçarola rasa de paredes retas usada para saltear proteínas e construir molho.", "Alemão: Flachkasserolle | Francês: Sautoir (diferente da Lyoner Pfanne, que não serve para construir molhos)."),
        FlashcardSpec("fc_09", 2, "Kochgeschirrarten", "Qual a diferença funcional entre Sautoir e Sauteuse (Schwenkkasserolle)?", "Sautoir tem paredes retas baixas (máxima área p/ selar proteínas e deglaçar); Sauteuse tem paredes cônicas inclinadas (ideal p/ saltear em movimento e reduzir/emulsionar molhos)."),
        FlashcardSpec("fc_10", 3, "Chefkoch v4", "No Brauner Kalbsfond (5 L), quantas vezes se deglaça o mirepoix + extrato de tomate antes de cobrir com água?", "Deglaça-se de 2 a 3 vezes com o vinho branco (1 kg total), reduzindo a cada vez até consistência de glace para extrair cor e umami sem amargor."),
        FlashcardSpec("fc_11", 3, "Chefkoch v4", "Quando entram as ervas aromáticas nas 3 horas de fervura branda do Brauner Kalbsfond e como se filtra?", "As ervas e especiarias entram apenas na ÚLTIMA HORA para não volatilizar aromas ou amargar; filtra-se passando em pano fino (étamine)."),
        FlashcardSpec("fc_12", 4, "Ernaehrung", "Qual a proporção típica de amilopectina e amilose nos amidos de trigo, milho e batata segundo o manual Ernaehrung?", "Aproximadamente 80% de amilopectina (cadeia ramificada que confere viscosidade) e 20% de amilose (cadeia linear que geleifica/retrograda)."),
        FlashcardSpec("fc_13", 5, "Swissmilk", "Quais os 2 queijos clássicos do Fondue Moitié-Moitié (Cantão de Friburgo) e o que impede o molho de talhar?", "50% Gruyère AOP + 50% Vacherin Fribourgeois AOP; a acidez tartárica do vinho branco seco + amido de milho estabilizam a caseína e a gordura."),
        FlashcardSpec("fc_14", 6, "Acrilamida", "Qual o protocolo térmico duplo de fritura por imersão e o teto máximo contra acrilamida?", "Pré-fritar (blanchir) a 140 °C e finalizar a 165–175 °C, respeitando o teto máximo absoluto de 175 °C para evitar formação de acrilamida neurotóxica."),
        FlashcardSpec("fc_15", 6, "Swissmilk", "Por que a batata para Rösti clássico é cozida com casca na véspera (Gschwellti)?", "O resfriamento promove retrogradação parcial do amido, permitindo ralar fios firmes que colam na superfície externa crocante sem virar purê."),
        FlashcardSpec("fc_16", 7, "Swissmilk", "Por que a vitela do Zürcher Geschnetzeltes é selada em 2 lotes rápidos na Sautoir com Bratbutter?", "Um lote grande resfria a chapa abaixo de 140 °C, fazendo a carne cozinhar no próprio suco exsudado e endurecer antes da reação de Maillard."),
        FlashcardSpec("fc_17", 8, "Swissmilk", "Qual cereal compõe a massa tradicional dos Pizzoccheri de Poschiavo (GR)?", "Farinha de trigo-sarraceno (Buchweizen / Grano saraceno, ~80%) combinada com farinha de trigo branca (~20%)."),
        FlashcardSpec("fc_18", 9, "Kochgeschirrarten", "Qual utensílio (DE/FR) é usado para pochar peixes inteiros delicados e qual o PCC seguro de peixes?", "Fischkessel / Poissonnière (com grelha interna); temperatura interna segura ≥ 63 °C (ou 55–58 °C em protocolo Sous-vide validado)."),
        FlashcardSpec("fc_19", 10, "Chefkoch v4", "Quais os tempos escalonados de cocção na Berner Platte (Sem. 10)?", "Língua defumada ~120 min; Siedfleisch Hohrücken ~90 min; Bacon ~60 min; Costeletas suínas e Berner Zungenwurst pochadas a ~82 °C no final."),
        FlashcardSpec("fc_20", 10, "Chefkoch v4", "Qual erva aromática clássica acompanha obrigatoriamente as vagens na Berner Platte?", "Segurelha (Bohnenkraut / Sarriette), adicionada ao estufar as vagens previamente branqueadas e chocadas em gelo."),
        FlashcardSpec("fc_21", 11, "Chefkoch v4", "No Bollito misto mit Salsa verde, qual corte bovino suíço entra junto ao ombro de vitela, língua e Zampone?", "Huftdeckel (capa da alcatra / picanha suíça, subcorte do Nierstück), cozido por ~1 h em fervura suave."),
        FlashcardSpec("fc_22", 11, "Higiene", "Qual cuidado sanitário é obrigatório na Salsa verde emulsionada do Bollito misto?", "Usar gema PASTEURIZADA (junto a salsa, aliche, alho, alcaparra, azeite e limão), pois a emulsão não sofre tratamento térmico."),
        FlashcardSpec("fc_23", 12, "Rind", "Liste os 8 grandes cortes bovinos suíços (Rind A–H) em Alemão e Francês.", "A: Stotzen (Cuisse), B: Nierstück (Aloyau), C: Lempen (Flanc), D: Rücken (Train de côtes), E: Schulter (Épaule), F: Federstück (Côte plate), G: Brust (Poitrine), H: Hals (Cou)."),
        FlashcardSpec("fc_24", 12, "Rind", "Quais são os 3 subcortes do Filet dentro do Nierstück?", "Filetkopf (Chateaubriand), Filetherz (Tournedos / Medaillons) e Filetspitz (Mignons / Émincé)."),
        FlashcardSpec("fc_25", 12, "Chefkoch v4", "Como é preparada a superfície dos Bison-Mignons (60 g) para aderir a Thymiankruste?", "Faz-se uma incisão superficial em grade de 2 mm de profundidade, pincela-se mostarda inglesa diluída em vinho branco e aplica-se a crosta de tomilho, cebolinha e farinha."),
        FlashcardSpec("fc_26", 13, "Gartechniken", "Defina a técnica clássica Poelieren (utensílio, base aromática, temperatura e regra de líquido).", "Cocção em forno tampado (Braisière ou Rondeau) sobre matignon aromático a 140–160 °C SEM NENHUM líquido adicionado."),
        FlashcardSpec("fc_27", 14, "Teige", "Quantas dobras duplas levam o Deutscher Blätterteig e o Halbblätterteig?", "Deutscher Blätterteig (~0,6–0,87 kg gordura/kg farinha): 4 dobras duplas. Halbblätterteig (metade da gordura): 3 dobras duplas."),
        FlashcardSpec("fc_28", 14, "Teige", "Por que NUNCA se deve guardar massa folhada crua por longo tempo a 5 °C?", "Sob refrigeração prolongada a 5 °C, a massa acidifica e fica acinzentada por oxidação enzimática da farinha; deve-se congelar (-18 °C)."),
        FlashcardSpec("fc_29", 15, "Swissmilk", "Quais são os 3 patrimônios de confeitaria cantonal de Grisões (GR), Argóvia (AG) e Zug (ZG)?", "GR: Engadiner Nusstorte (caramelo + creme + nozes); AG: Aargauer Rüeblitorte (cenoura + amêndoas); ZG: Zuger Kirschtorte (japonais + creme de manteiga + Kirsch)."),
        FlashcardSpec("fc_30", 16, "Chefkoch v4", "Qual o segredo de forno e pré-cocção do Vanilleauflauf (Suflê de Baunilha)?", "Ramequins untados c/ manteiga e farinha T550 cheios até ¾, pré-aquecidos em banho-maria na boca do fogão e assados a 200 °C por 15–20 min."),
        FlashcardSpec("fc_31", 16, "Chefkoch v4", "Como evitar que a Gestürzte Karamellcreme fique furada/empolada?", "Cobrir as formas com filme plástico e pochar em banho-maria controlado a exatos 80 °C (nunca ferver), seguido de meio dia de descanso refrigerado antes de desenformar."),
        FlashcardSpec("fc_32", 16, "Cordon Bleu", "Quais as temperaturas exatas dos 4 estágios de calda de açúcar (Bala mole, Bala dura, Casca mole, Casca dura)?", "Bala mole: 116–118 °C (Parfaits/merengue italiano); Bala dura: 125 °C (fondant/marzipã); Casca mole: 134 °C (nougat); Casca dura: 145 °C (açúcar-cande)."),
        FlashcardSpec("fc_33", 17, "Chefkoch v4", "Qual a temperatura interna obrigatória da Geflügelgalantine e como é feita a farce?", "65 °C no centro térmico. A farce de coxa + creme 35% é processada gelada e passada no tamis sobre banho de gelo com frutas secas marinadas em Maraschino."),
        FlashcardSpec("fc_34", 17, "Sous-Vide", "Qual a concentração e os tempos da salmoura prévia no protocolo Sous-vide suíço?", "Salmoura de 5% a 10% (50 a 100 g de sal por litro de água): 10 minutos para peixes (evita saída de albumina) e 3 a 4 horas para carnes grandes."),
        FlashcardSpec("fc_35", 17, "Sous-Vide", "Como ajustar a temperatura do banho Sous-vide e qual a espessura máxima da peça?", "Ajustar o banho 0,5 a 1,0 °C ACIMA do centro térmico desejado; usar apenas peças de até 7 cm de espessura e sem ossos."),
        FlashcardSpec("fc_36", 17, "Sous-Vide", "Cite os parâmetros validados de Sous-vide para Bacalhau, Salmão 25 mm e Peito de Vitela.", "Filé de Bacalhau: 55 °C / 2h30 | Salmão 25 mm: 57,5 °C / 125 min | Peito de Vitela (Kalbsbrust): 62 °C / 15 h (após 3 h de salmoura)."),
        FlashcardSpec("fc_37", 18, "Confeitaria", "O que compõe a sobremesa outonal suíça Vermicelles?", "Purê cozido de castanha portuguesa (Marroni) adoçado com baunilha e Kirsch, extrudado em fios finos sobre merengue seco e chantilly sem açúcar."),
        FlashcardSpec("fc_38", 19, "Fundos", "Por que o fumet de peixe ou fundo de crustáceos não deve passar de 25–30 minutos de fervura?", "Acima de 30 min, sais de cálcio das espinhas/carapaças dissolvem-se gerando sabor calcário, amargo e turbidez irreversível."),
        FlashcardSpec("fc_39", 20, "Ernaehrung", "Qual a meta diária mínima de fibras (Nahrungsfasern) e como elas reduzem o colesterol sanguíneo?", "Meta ≥ 30 g/dia. As fibras sequestram ácidos biliares no lúmen intestinal (excretando-os), obrigando o fígado a captar colesterol sérico para sintetizar novos sais biliares."),
        FlashcardSpec("fc_40", 20, "Ernaehrung", "Quanto de farelo de trigo, damasco seco, feijão branco, lentilha ou pão Graham fornece 10 g de fibra?", "20 g farelo de trigo = 50 g damasco seco = 55 g feijão branco = 100 g lentilhas = 150 g pão Graham."),
        FlashcardSpec("fc_41", 21, "Fachrechnen", "Qual a fórmula correta de Compra com múltiplas perdas em cascata (ex.: 20% limpeza e 15% cocção)?", "Compra = Porção / [(1 - 0,20) × (1 - 0,15)] = Porção / (0,80 × 0,85) = Porção / 0,68. Somar 20% + 15% = 35% gera erro grave de falta de produto!"),
        FlashcardSpec("fc_42", 22, "Gestão BAB", "O que compõe o Prime-Cost na contabilidade gastronômica suíça (BeBu/KATAG)?", "Prime-Cost = Warenkosten (CMV / Custo de Mercadoria) + Personalkosten diretos (Custo de Pessoal)."),
        FlashcardSpec("fc_43", 22, "Gestão BAB", "Diferencie BVP e EVP na precificação suíça.", "BVP (Bruttoverkaufspreis / Nettoerlös antes do imposto) é o preço líquido de venda; EVP (Endverkaufspreis) inclui o MWST (IVA suíço, referência de cálculo 8%)."),
        FlashcardSpec("fc_44", 22, "Gestão BAB", "Escreva a fórmula do Ponto de Equilíbrio em unidades (x_cr) e a conversão de Nota Suíça 1–6.", "x_cr = Kf / (p - kv), onde Kf = custos fixos, p = preço unitário, kv = custo variável unitário. Nota Suíça = 1 + 5 × (Pontos Obtidos / Pontos Máximos)."),
        FlashcardSpec("fc_45", 23, "Brigada", "Quais as funções específicas do Diätkoch e do Mitarbeiterkoch entre os 13 cargos da brigada clássica?", "Diätkoch: elabora e adapta refeições dietoterápicas/alergênicas com rigor clínico; Mitarbeiterkoch: cozinha dedicada à alimentação saudável da equipe interna."),
        FlashcardSpec("fc_46", 23, "Brigada", "Compare a praça do Entremetier com a do Rôtisseur.", "Entremetier: sopas, legumes, batatas (Rösti), massas, arroz e ovos; Rôtisseur: assados de forno, grelhados, espetos e frituras por imersão."),
        FlashcardSpec("fc_47", 24, "Portfólio", "No Portfolio Unterricht suíço (resumo_esquema), qual a proporção recomendada de elementos visuais?", "Aproximadamente 25% do documento deve ser composto por diagramas, fluxogramas térmicos ou tabelas estruturadas autorais."),
        FlashcardSpec("fc_48", 24, "Higiene PCC", "Resuma as temperaturas mínimas seguras de cocção para aves, carne moída, geral e galantina.", "Aves inteiras/cortes: ≥ 74 °C | Carne moída (Hackfleisch): ≥ 71 °C | Pratos gerais/regeneração: ≥ 70 °C | Galantina de ave pochada: 65 °C | Peixes: ≥ 63 °C.")
    )
}
