package com.example.data.curriculum

import com.example.domain.model.CookingStepSpec
import com.example.domain.model.IngredientSpec
import com.example.domain.model.RecipeSpec
import com.example.domain.model.SmartKitchenAlertType

object SwissmilkAndBaseRecipesCatalog {

    private fun quickCantonalRecipe(
        id: String,
        week: Int,
        cycle: Int,
        axis: Int,
        canton: String,
        title: String,
        subtitle: String,
        pcc: String,
        equip: List<String>,
        ingredients: List<IngredientSpec>,
        steps: List<CookingStepSpec>,
        isSwissmilk: Boolean = true
    ) = RecipeSpec(
        id = id,
        weekNumber = week,
        cycleNumber = cycle,
        axisId = axis,
        title = title,
        subtitle = subtitle,
        baseServings = 4,
        cantonCode = canton,
        isSwissmilkClassic = isSwissmilk,
        isChefkochV4 = false,
        requiredEquipmentIds = equip,
        pccTargetSummary = pcc,
        ingredients = ingredients,
        steps = steps
    )

    private val baseAndSwissmilkRecipes: List<RecipeSpec> = listOf(
        // 1. ZH - Zürcher Geschnetzeltes mit Rösti
        quickCantonalRecipe(
            "swiss_zh_zurcher_geschnetzeltes", 7, 2, 3, "ZH",
            "Zürcher Geschnetzeltes mit Rösti",
            "Émincé de vitela salteado em Bratbutter com molho cremoso de cogumelos e Rösti dourada",
            "Selagem em 2 lotes a 180 °C | Vitela rosada ≥ 68 °C sem ferver no molho",
            listOf("sautoir", "poele_lyonnaise", "chefmesser"),
            listOf(
                IngredientSpec("Filé/Noz de vitela em émincé", 600.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Batata cerosa cozida na véspera (Gschwellti)", 800.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Cogumelos Paris frescos", 200.0, "g", 1.12, "Hortifrúti"),
                IngredientSpec("Creme de leite 35% gordura", 250.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Vinho branco seco + Demi-glace", 200.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Bratbutter (manteiga clarificada)", 60.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Ralar Gschwellti & Dourar Rösti", "Ralar grosso as batatas cozidas na véspera e dourar em Lyoner Pfanne com Bratbutter 12 min de cada lado formando bolo dourado.", "Lyoner Pfanne", 165, 24),
                CookingStepSpec(2, "Selar Vitela em 2 Lotes na Sautoir", "Aquecer Sautoir com Bratbutter a 180 °C, selar o émincé de vitela em 2 lotes rápidos (< 90s) e reservar aquecido.", "Flachkasserolle / Sautoir", 180, 4),
                CookingStepSpec(3, "Deglaçar & Reduzir Molho Nappé", "Suar cebola Brunoise e cogumelos, deglaçar com vinho branco, reduzir à metade, juntar demi-glace + creme 35% até nappé; reincorporar a vitela fora da fervura.", "Flachkasserolle / Sautoir", 82, 8)
            )
        ),
        // 2. AG - Aargauer Rüeblitorte
        quickCantonalRecipe(
            "swiss_ag_rueblitorte", 15, 3, 5, "AG",
            "Aargauer Rüeblitorte",
            "Torta úmida de cenoura ralada fina, farinha de amêndoas, Kirsch e decoração de marzipã",
            "Forno 180 °C por 45 min | Centro seco ≥ 92 °C",
            listOf("cul_de_poule", "zestenmesser"),
            listOf(
                IngredientSpec("Cenouras doces raladas finas", 300.0, "g", 1.16, "Hortifrúti"),
                IngredientSpec("Amêndoas moídas sem pele", 300.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Ovos separados (gemas + claras em neve)", 240.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Açúcar + Kirsch de Zug/Aargau", 220.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Bater Gemas & Incorporar Cenoura", "Bater gemas com açúcar e raspas de limão (Zestenmesser) até fita pálida; juntar cenoura ralada, amêndoas e Kirsch.", "Cul de poule", 20, 12),
                CookingStepSpec(2, "Dobrar Merengue & Assar", "Envolver claras em neve firme com delicadeza e assar a 180 °C por 45 min; cobrir com glacê de Kirsch e cenourinhas de marzipã.", "Forma de aro 24 cm", 180, 45)
            )
        ),
        // 3. FR - Fondue Moitié-Moitié
        quickCantonalRecipe(
            "swiss_fr_fondue", 5, 1, 2, "FR",
            "Fondue Moitié-Moitié Fribourgeois",
            "Emulsão quente 50% Gruyère AOP e 50% Vacherin Fribourgeois AOP em vinho branco",
            "Fusão gradual a 72–78 °C sem ferver para não separar caseína",
            listOf("chefmesser"),
            listOf(
                IngredientSpec("Gruyère AOP maturado ralado", 400.0, "g", 1.03, "Laticínios & Ovos"),
                IngredientSpec("Vacherin Fribourgeois AOP", 400.0, "g", 1.03, "Laticínios & Ovos"),
                IngredientSpec("Vinho branco Chasselas seco + suco de limão", 320.0, "ml", 1.0, "Mercearia Seca"),
                IngredientSpec("Amido de milho + Kirsch + alho", 35.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Esfregar Alho & Fundir Gruyère", "Esfregar alho na caquelon, aquecer vinho + limão + amido e fundir o Gruyère mexendo em '8'.", "Caquelon de cerâmica", 75, 10),
                CookingStepSpec(2, "Incorporar Vacherin fora da fervura", "Adicionar o Vacherin Fribourgeois delicadamente até emulsão cremosa homogênea; finalizar com Kirsch e noz-moscada.", "Caquelon", 74, 5)
            )
        ),
        // 4. GR - Bündner Gerstensuppe
        quickCantonalRecipe(
            "swiss_gr_gerstensuppe", 4, 1, 2, "GR",
            "Bündner Gerstensuppe",
            "Sopa cremosa dos Grisões com cevada perolada, Brunoise impecável e Bündnerfleisch",
            "Cozimento lento 90 °C | Serviço ≥ 68 °C",
            listOf("chefmesser", "braisiere"),
            listOf(
                IngredientSpec("Cevada perolada (Rollgerste)", 120.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Cenoura, aipo e alho-poró em Brunoise 1,5 mm", 240.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Bündnerfleisch + toucinho em Brunoise", 100.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Fundo bovino + creme 35%", 1200.0, "ml", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Corte Brunoise 1,5 mm & Suado", "Cortar vegetais e Bündnerfleisch em Brunoise de 1,5 mm com Chefmesser afiada; suar na manteiga com a cevada.", "Chefmesser + Braisière", 95, 15),
                CookingStepSpec(2, "Cocção Lenta & Liaison de Creme", "Cozinhar no caldo por 50 min até gelatinização macia da cevada e finalizar com creme 35% e cebolinha.", "Braisière", 90, 50)
            )
        ),
        // 5. GR - Capuns Sursilvans
        quickCantonalRecipe(
            "swiss_gr_capuns", 8, 2, 3, "GR",
            "Capuns Sursilvans",
            "Trouxinhas de folha de acelga recheadas com massa Spätzli e carne seca alpina",
            "Branqueamento + cocção suave em caldo e leite a 85 °C",
            listOf("poele_antiadhesif", "cul_de_poule"),
            listOf(
                IngredientSpec("Folhas grandes de acelga (Mangold)", 16.0, "un", 1.15, "Hortifrúti"),
                IngredientSpec("Farinha Spätzli, ovos e leite", 320.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Salsiz / Bündnerfleisch em cubinhos", 100.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Queijo da montanha ralado + caldo/leite", 350.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Branquear Acelga & Bater Massa", "Branquear folhas 30s e resfriar em gelo (preserva clorofila e vitamina C); bater massa Spätzli até bolhas e juntar embutidos.", "Cul de poule", 98, 15),
                CookingStepSpec(2, "Enrolar & Cozinhar no Leite-Caldo", "Modelar rolinhos firmes de 45 g, pochar 12 min em metade caldo/metade leite e gratinar com queijo alpino.", "Sautoir", 85, 15)
            )
        ),
        // 6. GR - Engadiner Nusstorte
        quickCantonalRecipe(
            "swiss_gr_nusstorte", 15, 3, 5, "GR",
            "Engadiner Nusstorte (Tuorta da Nusch)",
            "Massa sablée suíça recheada com nozes envoltas em caramelo e creme de leite",
            "Caramelo âmbar 168 °C | Forno 190 °C por 40 min",
            listOf("sauteuse", "cul_de_poule"),
            listOf(
                IngredientSpec("Farinha, manteiga gelada, açúcar e ovo (Mürbeteig)", 480.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Nozes quebradas grossas", 260.0, "g", 1.05, "Mercearia Seca"),
                IngredientSpec("Açúcar p/ caramelo + mel alpino", 240.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Creme de leite 35%", 200.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Mürbeteig & Caramelo de Nozes", "Descansar a massa quebradiça; caramelizar o açúcar, deglaçar com creme quente, mel e envolver as nozes.", "Sauteuse", 168, 15),
                CookingStepSpec(2, "Fechar Tampa Dupla & Assar", "Forrar forma, rechear morno, selar tampa de massa c/ furo central de vapor e assar a 190 °C.", "Forma 24 cm", 190, 40)
            )
        ),
        // 7. GR/TI - Pizzoccheri di Poschiavo
        quickCantonalRecipe(
            "swiss_gr_pizzoccheri", 8, 2, 3, "GR",
            "Pizzoccheri di Poschiavo",
            "Talharim curto de trigo-sarraceno cozido com batata, repolho Wirz e manteiga de sálvia",
            "Paysanne 1 cm | Manteiga noisette perfumada com sálvia a 150 °C",
            listOf("chefmesser", "poele_antiadhesif"),
            listOf(
                IngredientSpec("Farinha de trigo-sarraceno + trigo", 350.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Repolho crespo (Wirz) e batata em Paysanne", 400.0, "g", 1.20, "Hortifrúti"),
                IngredientSpec("Queijo Casera / Gruyère + Parmesão", 200.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Manteiga + alho + sálvia fresca", 90.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Abrir e Cortar Pizzoccheri", "Abrir massa a 2 mm e cortar tiras de 7 cm × 1 cm.", "Chefmesser", 20, 20),
                CookingStepSpec(2, "Cocção Conjunta & Manteiga Noisette", "Cozinhar batata, repolho e massa juntos; intercalar com queijo e regar com manteiga noisette de alho e sálvia.", "Poêle antiadhésif", 150, 15)
            )
        ),
        // 8. VD - Papet Vaudois au Saucisson IGP
        quickCantonalRecipe(
            "swiss_vd_papet", 13, 3, 3, "VD",
            "Papet Vaudois & Saucisson Vaudois IGP",
            "Compota estufada de alho-poró e batatas no vinho branco com embutido pochado",
            "Saucisson pochado a 80 °C sem furar a pele | Vegetais em Antihaft/Braisière",
            listOf("braisiere", "poele_antiadhesif"),
            listOf(
                IngredientSpec("Alho-poró em rodelas de 1 cm", 700.0, "g", 1.25, "Hortifrúti"),
                IngredientSpec("Batatas amiláceas em cubos", 450.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Saucisson Vaudois IGP", 480.0, "g", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Vinho branco Chasselas + creme", 180.0, "ml", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Estufar Alho-poró e Batata", "Suar alho-poró em manteiga sem dourar, juntar vinho branco e batatas tampado por 30 min.", "Braisière / Poêle antiadhésif", 90, 30, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "Use caçarola inox esmaltada ou antiaderente para preservar cor verde-clara e vitamina C do alho-poró."),
                CookingStepSpec(2, "Pochar Saucisson a 80 °C", "Pochar o Saucisson a 80 °C por 35 min sem furar o invólucro para reter toda a suculência.", "Caçarola", 80, 35)
            )
        ),
        // 9. VD - Malakoff de Vinzel
        quickCantonalRecipe(
            "swiss_vd_malakoff", 6, 1, 2, "VD",
            "Malakoff Vaudois (Bolinho Frito de Gruyère)",
            "Cúpula crocante de queijo Gruyère temperado com vinho branco frita sob controle de acrilamida",
            "Fritura controlada 170 °C (PROIBIDO passar de 175 °C contra acrilamida)",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Gruyère AOP ralado fino", 400.0, "g", 1.02, "Laticínios & Ovos"),
                IngredientSpec("Ovos, farinha, vinho branco, kirsch e alho", 180.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Discos de pão de forma + óleo de fritura alto-oleico", 500.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Pasta de Gruyère & Moldagem", "Misturar queijo, ovos, farinha, vinho e especiarias formando domos altos sobre discos de pão embebidos em vinho.", "Cul de poule", 8, 15),
                CookingStepSpec(2, "Fritura de Precisão ≤ 175 °C", "Fritar com a face de queijo para baixo a 168–172 °C por 3–4 min até inflar e dourar.", "Fritadeira c/ Termômetro", 170, 4, SmartKitchenAlertType.DEEP_FRY_ACRYLAMIDE, "TETO RÍGIDO 175 °C: Monitore o termômetro — jamais ultrapasse 175 °C para impedir formação de acrilamida!")
            )
        ),
        // 10. VS - Cholera Gommer (Torta Salgada do Valais)
        quickCantonalRecipe(
            "swiss_vs_cholera", 5, 1, 2, "VS",
            "Walliser Cholera",
            "Torta folhada em camadas de batata, alho-poró, maçã, pera e queijo Raclette AOP",
            "Forno 200 °C por 40 min | Vegetais pré-salteados em Antihaft",
            listOf("poele_antiadhesif", "chefmesser"),
            listOf(
                IngredientSpec("Massa folhada suíça (Blätterteig)", 500.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Batata Gschwellti, maçã, pera e alho-poró", 700.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Queijo Raclette du Valais AOP", 250.0, "g", 1.02, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Saltear Alho-poró em Antihaft", "Saltear alho-poró e cebola em Poêle antiadhésif preservando nutrientes e doçura.", "Poêle antiadhésif", 95, 8, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "Salteie o alho-poró em antiaderente para evitar perda oxidativa de vitamina C."),
                CookingStepSpec(2, "Montar Camadas & Assar", "Intercalar batata, queijo, alho-poró e frutas na massa folhada, selar chaminé de vapor e assar a 200 °C.", "Forma 26 cm", 200, 40)
            )
        ),
        // 11. OW/NW - Älplermagronen mit Apfelmus
        quickCantonalRecipe(
            "swiss_ow_aelplermagronen", 5, 1, 2, "OW",
            "Älplermagronen mit Apfelmus",
            "Macarrão alpino cozido com batatas no creme/leite, queijo Sbrinz/Gruyère, cebola crocante e purê de maçã",
            "Emulsão láctea a 85 °C | Cebola dourada ≤ 170 °C",
            listOf("braisiere", "poele_lyonnaise"),
            listOf(
                IngredientSpec("Macarrão Magronen + batatas em cubos 1 cm", 550.0, "g", 1.12, "Mercearia Seca"),
                IngredientSpec("Gruyère / Sbrinz ralado + creme/leite", 450.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Cebolas em anéis + maçãs Boskoop p/ compota", 500.0, "g", 1.18, "Hortifrúti")
            ),
            listOf(
                CookingStepSpec(1, "Cocção por Absorção (One-Pot Alpino)", "Cozinhar batatas e Magronen diretamente em caldo + leite até absorver o amido cremoso; envolver o queijo.", "Braisière", 92, 16),
                CookingStepSpec(2, "Anéis de Cebola & Compota de Maçã", "Dourar anéis de cebola na Lyoner Pfanne sem queimar e servir com Apfelmus morna.", "Lyoner Pfanne", 165, 10)
            )
        ),
        // 12. LU - Luzerner Chügelipastete
        quickCantonalRecipe(
            "swiss_lu_chuegelipastete", 14, 3, 5, "LU",
            "Luzerner Chügelipastete",
            "Caixa folhada imperial recheada com vitela, almôndegas Brätchügeli, cogumelos e passas ao Cognac",
            "Pastetli assado a 210 °C | Recheio PCC ≥ 74 °C",
            listOf("sautoir", "chefmesser"),
            listOf(
                IngredientSpec("Deutscher Blätterteig p/ caixa folhada", 500.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Vitela em cubos + Brätchügeli (almôndegas de vitela)", 500.0, "g", 1.06, "Açougue & Peixaria"),
                IngredientSpec("Cogumelos, fundo branco, creme 35% e passas", 380.0, "g", 1.08, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Assar Caixa Vol-au-vent", "Cortar discos de massa folhada fria, dourar com ovo sem escorrer nas bordas laterais e assar a 210 °C.", "Assadeira", 210, 22, SmartKitchenAlertType.PUFF_PASTRY_OXIDATION, "Trabalhe a massa folhada bem fria e nunca deixe crua dias a 5 °C."),
                CookingStepSpec(2, "Velouté Rico & Brätchügeli", "Pochar almôndegas e vitela no velouté de vitela com cogumelos e creme; preencher a caixa no momento do passe.", "Sautoir", 85, 15)
            )
        ),
        // 13. BS - Basler Mehlsuppe
        quickCantonalRecipe(
            "swiss_bs_mehlsuppe", 4, 1, 2, "BS",
            "Basler Mehlsuppe",
            "Sopa tradicional do Fasnacht da Basileia com farinha tostada a seco, cebola e caldo bovino",
            "Torra seca da farinha a 155 °C sem carbonizar | Fervura lenta 45 min",
            listOf("rondeau"),
            listOf(
                IngredientSpec("Farinha de trigo branca tostada a seco", 80.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Manteiga + cebola fatiada + vinho tinto", 180.0, "g", 1.12, "Hortifrúti"),
                IngredientSpec("Fundo escuro bovino + Gruyère ralado", 1200.0, "ml", 1.0, "Especiarias & Fundos")
            ),
            listOf(
                CookingStepSpec(1, "Torra Seca Controlada da Farinha", "Tostar a farinha sem gordura em Rondeau mexendo sem parar até cor castanho-avelã intensa; resfriar antes de hidratar.", "Rondeau", 155, 12),
                CookingStepSpec(2, "Hidratação & Fervura de 45 min", "Suar cebola na manteiga, juntar farinha fria, vinho tinto e caldo quente aos poucos; cozinhar 45 min e servir com Gruyère.", "Rondeau", 90, 45)
            )
        ),
        // 14. ZG - Zuger Kirschtorte
        quickCantonalRecipe(
            "swiss_zg_kirschtorte", 15, 3, 5, "ZG",
            "Zuger Kirschtorte IGP",
            "Discos crocantes de merengue de avelã (Japonais), pão de ló embebido em Kirsch e creme de manteiga",
            "Calda/creme a 20 °C para não talhar manteiga",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Claras, açúcar e avelãs moídas (Japonais)", 320.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Biscuit leve (ovos, açúcar, farinha)", 220.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Manteiga, creme confeiteiro e Zuger Kirsch AOP", 350.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Assar Discos Japonais & Biscuit", "Secar 2 discos finos de Japonais a 140 °C e assar o pão de ló a 180 °C.", "Forno Confeitaria", 140, 35),
                CookingStepSpec(2, "Embeber em Kirsch & Montar Creme", "Emulsionar manteiga aerada e creme na mesma temperatura (20 °C), montar camadas generosamente embebidas em calda de Kirsch e decorar com amêndoas tostadas.", "Cul de poule", 20, 20)
            )
        ),
        // 15. SG - St. Galler Bratwurst IGP mit Zwiebelsauce & Rösti
        quickCantonalRecipe(
            "swiss_sg_bratwurst", 7, 2, 3, "SG",
            "St. Galler Bratwurst IGP mit Zwiebelsauce",
            "Salsicha branca de vitela grelhada sem mostarda, servida com molho escuro de cebola caramelizada",
            "Centro térmico da Bratwurst ≥ 72 °C | Cebola caramelizada lenta",
            listOf("poele_lyonnaise", "sautoir"),
            listOf(
                IngredientSpec("St. Galler Kalbsbratwurst IGP", 4.0, "un", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Cebolas em meia-lua fina", 400.0, "g", 1.15, "Hortifrúti"),
                IngredientSpec("Brauner Kalbsfond + vinho tinto", 300.0, "ml", 1.0, "Especiarias & Fundos")
            ),
            listOf(
                CookingStepSpec(1, "Zwiebelsauce na Sautoir", "Caramelizar cebolas lentamente por 20 min, polvilhar leve farinha, deglaçar com vinho tinto e reduzir com Brauner Kalbsfond.", "Sautoir", 95, 25),
                CookingStepSpec(2, "Dourar Bratwurst Suavemente", "Dourar as salsichas em fogo médio na Lyoner Pfanne com Bratbutter sem estourar a pele (regra local: servir SEM mostarda!).", "Lyoner Pfanne", 150, 12)
            )
        ),
        // 16. TI - Brasato al Merlot del Ticino con Polenta
        quickCantonalRecipe(
            "swiss_ti_brasato", 11, 2, 3, "TI",
            "Brasato al Merlot con Polenta Bramata",
            "Paleta bovina marinada no Merlot ticinese braseada em Braisière com polenta cremosa",
            "Schmoren tampado a 150 °C até maciez de colher (≥ 85 °C)",
            listOf("braisiere", "chefmesser"),
            listOf(
                IngredientSpec("Schulter / Paleta bovina limpa", 800.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Merlot del Ticino + mirepoix + alecrim", 650.0, "g", 1.12, "Hortifrúti"),
                IngredientSpec("Fubá grosso Bramata p/ polenta + manteiga/queijo", 320.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Selar & Brasear em Braisière", "Marinar a peça 12h, secar bem, selar em Bratbutter, juntar mirepoix + Merlot reduzido + fundo escuro e brasear tampado a 150 °C por 2h30.", "Schmorgeschirr / Braisière", 150, 150),
                CookingStepSpec(2, "Polenta Bramata Longa", "Cozinhar a polenta em chuva mexendo por 45 min para hidratação total do amido e finalizar com manteiga e queijo.", "Caldeirão fundo grosso", 92, 45)
            )
        ),
        // 17. GE/NE - Filets de Perche Meunière
        quickCantonalRecipe(
            "swiss_ge_perche_meuniere", 9, 2, 3, "GE",
            "Filets de Perche Meunière du Léman",
            "Filés delicados de perca do Lago Genebra salteados com beurre noisette, limão e salsa",
            "Peixe delicado ≥ 63 °C | Beurre noisette espumante a 150 °C",
            listOf("poele_antiadhesif", "filetiermesser"),
            listOf(
                IngredientSpec("Filés frescos de perca (Eglifilets)", 600.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Manteiga fresca + Bratbutter", 100.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Limão siciliano, salsa picada e farinha leve", 80.0, "g", 1.15, "Hortifrúti")
            ),
            listOf(
                CookingStepSpec(1, "Empanar à la Française & Saltear", "Secar os filés, passar levemente em farinha batendo o excesso e saltear 90s de cada lado em Poêle antiadhésif.", "Poêle antiadhésif", 160, 4),
                CookingStepSpec(2, "Beurre Noisette Espumante", "Espumar manteiga fresca até aroma de avelã (150 °C), parar a cocção com gotas de limão (espuma instantânea) e regar sobre os filés com salsa.", "Sauteuse", 150, 2)
            )
        ),
        // 18. SH - Schaffhauser Bölletünne (Torta de Cebola)
        quickCantonalRecipe(
            "swiss_sh_boelletuenne", 5, 1, 2, "SH",
            "Schaffhauser Bölletünne",
            "Torta aberta de cebolas estufadas em aparelho real de creme, ovos e bacon",
            "Aparelho coagulado a 82–85 °C em forno 190 °C",
            listOf("poele_antiadhesif", "cul_de_poule"),
            listOf(
                IngredientSpec("Massa quebradiça salgada (Kuchenteig)", 300.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Cebolas amarelas fatiadas + cubinhos de Speck", 650.0, "g", 1.15, "Hortifrúti"),
                IngredientSpec("Creme 35%, leite, ovos e cominho (Kümmel)", 350.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Suar Cebolas sem Cor em Antihaft", "Estufar as cebolas lentamente em frigideira antiaderente até ficarem doces e translúcidas.", "Poêle antiadhésif", 95, 18, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "Use frigideira antiaderente ao suar cebolas para evitar sabor metálico e perda nutricional."),
                CookingStepSpec(2, "Verter Royale & Assar", "Espalhar cebolas frias sobre a massa furada, cobrir com mistura de ovos/creme/cominho e assar a 190 °C por 35 min.", "Forma de torta 28 cm", 190, 35)
            )
        ),
        // 19. GL - Glarner Kalberwurst an Zwiebelsauce
        quickCantonalRecipe(
            "swiss_gl_kalberwurst", 10, 2, 3, "GL",
            "Glarner Kalberwurst IGP c/ Ameixas",
            "Embutido branco cremoso de vitela, pão branco e leite pochado e dourado, servido com ameixas secas",
            "Pochar a 78–80 °C antes de dourar",
            listOf("poele_lyonnaise"),
            listOf(
                IngredientSpec("Glarner Kalberwurst IGP", 4.0, "un", 1.0, "Açougue & Peixaria"),
                IngredientSpec("Ameixas secas hidratadas em vinho tinto + canela", 200.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Cebola e fundo escuro p/ molho", 250.0, "g", 1.10, "Hortifrúti")
            ),
            listOf(
                CookingStepSpec(1, "Pochar a 80 °C & Dourar", "Aquecer as Kalberwurst em água a 80 °C por 12 min e dourar rapidamente em manteiga.", "Lyoner Pfanne", 80, 16),
                CookingStepSpec(2, "Servir com Molho de Cebola & Ameixas", "Acompanhar com molho de cebola e ameixas macias.", "Sauteuse", 85, 10)
            )
        ),
        // 20. AI/AR - Appenzeller Käsefladen & Siedwurst
        quickCantonalRecipe(
            "swiss_ai_kaesefladen", 6, 1, 2, "AI",
            "Appenzeller Käsefladen",
            "Torta rústica intensa com queijo Appenzeller de salmoura de ervas e cebola",
            "Forno 200 °C com base crocante",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Queijo Appenzeller Surchoix ralado", 320.0, "g", 1.03, "Laticínios & Ovos"),
                IngredientSpec("Massa fermentada/quebradiça + creme + ovos", 520.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Misturar Aparelho de Appenzeller", "Homogeneizar o queijo maturado com ovos, creme, cebola picada e pimenta.", "Cul de poule", 20, 8),
                CookingStepSpec(2, "Assar Base Crocante", "Assar na grade inferior do forno a 200 °C por 30 min.", "Assadeira redonda", 200, 30)
            )
        ),
        // 21. SO - Solothurner Wysuppe
        quickCantonalRecipe(
            "swiss_so_wysuppe", 4, 1, 2, "SO",
            "Solothurner Wysuppe (Sopa de Vinho Branco)",
            "Velouté aveludado de vinho branco, fundo de vitela, ligação de gema-creme e Julienne de legumes",
            "Liaison final máx. 80 °C (nunca ferver após juntar gema!)",
            listOf("sauteuse", "chefmesser"),
            listOf(
                IngredientSpec("Vinho branco seco + fundo claro de vitela", 1000.0, "ml", 1.0, "Especiarias & Fundos"),
                IngredientSpec("Cenoura, aipo e alho-poró em Julienne 1 mm × 3 cm", 160.0, "g", 1.18, "Hortifrúti"),
                IngredientSpec("Creme 35% + gemas (Liaison) + croutons", 180.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Julienne 1 mm × 3 cm em Antihaft", "Estufar a Julienne precisa em manteiga sem cor.", "Poêle antiadhésif", 90, 6, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "Salteie a Julienne em antiaderente para preservar vitamina C e cor viva."),
                CookingStepSpec(2, "Liaison a 80 °C sem Ebulição", "Engrossar levemente caldo + vinho, retirar do fogo e ligar com gema + creme batidos a máx. 80 °C.", "Sauteuse", 80, 10)
            )
        ),
        // 22. UR - Urner Hafechabis
        quickCantonalRecipe(
            "swiss_ur_hafechabis", 10, 2, 3, "UR",
            "Urner Hafechabis",
            "Guisado alpino de cordeiro/porco braseado lentamente com repolho branco e batatas",
            "Poelieren / Schmoren tampado em Braisière a 150 °C",
            listOf("braisiere", "chefmesser"),
            listOf(
                IngredientSpec("Paleta de cordeiro e porco em cubos", 600.0, "g", 1.10, "Açougue & Peixaria"),
                IngredientSpec("Repolho branco em Paysanne + batatas", 800.0, "g", 1.18, "Hortifrúti")
            ),
            listOf(
                CookingStepSpec(1, "Dourar Carnes & Camadas de Repolho", "Selar carnes, intercalar com repolho em Paysanne e especiarias na Braisière.", "Braisière", 150, 75),
                CookingStepSpec(2, "Finalizar com Batatas", "Juntar batatas nos 30 min finais até absorver o caldo aromático.", "Braisière", 150, 30)
            )
        ),
        // 23. JU - Tarte Tatin de Damassine / Toétché Jurassien
        quickCantonalRecipe(
            "swiss_ju_toetche", 15, 3, 5, "JU",
            "Toétché de Saint-Martin (Jura)",
            "Torta salgada-ácida festiva do Jura com creme azedo espesso (Crème acidulée) e açafrão",
            "Forno alto 230 °C por 18 min para manchas douradas características",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Massa levedada macia", 400.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Crème acidulée / Sauerrahm + ovo + pistilos de açafrão", 350.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Infundir Açafrão no Creme Ácido", "Misturar creme ácido, ovo, sal e infusão de açafrão.", "Cul de poule", 20, 10),
                CookingStepSpec(2, "Forno Alto 230 °C", "Assar rapidamente a 230 °C até borbulhar com pintas douradas.", "Assadeira", 230, 18)
            )
        ),
        // 24. SZ/TG/BL/NE Complementares + Técnicas Poelieren, Fritura, Sobremesas & Brasil/Mediterrâneo (até 35 base)
        quickCantonalRecipe(
            "swiss_tg_apfelkuechli", 6, 1, 2, "TG",
            "Thurgauer Apfelküchli c/ Creme Baunilha",
            "Rodelas de maçã da Turgóvia em massa de cerveja fritas sob controle de 170 °C",
            "Óleo entre 165–175 °C (jamais > 175 °C)",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Maçãs ácidas em rodelas de 8 mm", 500.0, "g", 1.20, "Hortifrúti"),
                IngredientSpec("Farinha, cerveja clara, ovos e canela", 300.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Massa de Cerveja & Fritura ≤ 175 °C", "Passar rodelas na massa aerada e fritar a 170 °C até dourado leve.", "Fritadeira", 170, 5, SmartKitchenAlertType.DEEP_FRY_ACRYLAMIDE, "Respeite o limite máximo de 175 °C para evitar acrilamida em massas amiláceas!")
            )
        ),
        quickCantonalRecipe(
            "tech_poelieren_kalbsnuss", 13, 3, 3, "CH",
            "Kalbsnuss Poeliert auf Matignon",
            "Noz de vitela assada tampada em Braisière sobre matignon aromático sem líquido adicionado",
            "Forno tampado 140–160 °C | Centro rosado 62–64 °C",
            listOf("braisiere", "chefmesser"),
            listOf(
                IngredientSpec("Kalbsnuss (Patinho de vitela amarrado)", 800.0, "g", 1.05, "Açougue & Peixaria"),
                IngredientSpec("Matignon (cenoura, aipo, cebola, presunto em Paysanne)", 240.0, "g", 1.16, "Hortifrúti"),
                IngredientSpec("Manteiga clarificada + Brauner Kalbsfond p/ deglaçar no final", 200.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Cama de Matignon & Poelieren Tampado", "Suar o matignon em manteiga na Braisière, acomodar a vitela pincelada com manteiga por cima, tampar hermeticamente e assar a 150 °C SEM LÍQUIDO ADICIONADO.", "Schmorgeschirr / Braisière", 150, 55, SmartKitchenAlertType.POELIEREN_MATIGNON, "REGRA POELIEREN: Forno tampado sobre matignon a 140–160 °C SEM adicionar caldo ou vinho durante a cocção!"),
                CookingStepSpec(2, "Deglaçagem Final do Matignon", "Retirar a vitela a 63 °C, destampar p/ caramelizar os sucos do matignon e deglaçar com Kalbsfond.", "Braisière", 95, 10)
            ),
            isSwissmilk = false
        ),
        quickCantonalRecipe(
            "patisserie_parfait_glace", 18, 3, 5, "CH",
            "Parfait Glacé Grand Marnier & Calda 118 °C",
            "Pâte à bombe esterilizada com calda bala mole (116–118 °C) e chantilly aerado",
            "Calda Bala Mole 116–118 °C | Congelamento ≤ -18 °C",
            listOf("cul_de_poule"),
            listOf(
                IngredientSpec("Gemas frescas + açúcar + água (calda 118 °C)", 260.0, "g", 1.0, "Laticínios & Ovos"),
                IngredientSpec("Creme de leite 35% batido em ponto souple + licor", 400.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Calda Bala Mole 116–118 °C na Pâte à Bombe", "Ferver açúcar e água até exatos 117 °C e verter em fio sobre as gemas batendo até esfriar (ponto fita).", "Caçarola + Cul de poule", 117, 12),
                CookingStepSpec(2, "Incorporar Chantilly & Moldar", "Envolver o licor e o creme batido macio e congelar a -18 °C.", "Cul de poule", -18, 240)
            ),
            isSwissmilk = false
        ),
        quickCantonalRecipe(
            "swiss_ti_vermicelles", 18, 3, 5, "TI",
            "Vermicelles de Castanha (Marroni) & Merengue",
            "Clássico outonal suíço de castanha portuguesa com Kirsch, suspiro seco e chantilly",
            "Merengue seco a 95 °C | Serviço frio ≤ 5 °C",
            listOf("tamis", "cul_de_poule"),
            listOf(
                IngredientSpec("Castanhas portuguesas cozidas + leite + açúcar + Kirsch", 450.0, "g", 1.05, "Mercearia Seca"),
                IngredientSpec("Claras + açúcar (merengues) + creme 35%", 350.0, "g", 1.0, "Laticínios & Ovos")
            ),
            listOf(
                CookingStepSpec(1, "Purê Fino no Tamis & Extrusão", "Passar o purê de castanhas com Kirsch no tamis e extrudar fios de Vermicelles sobre merengue e chantilly.", "Tamis + Prensa", 5, 20)
            )
        ),
        quickCantonalRecipe(
            "br_moqueca_mep", 19, 4, 6, "BR",
            "Moqueca de Robalo sob Precisão MEP",
            "Postas em salmoura curta de limão, Brunoise/Rhomben de pimentões e emulsão de leite de coco/dendê",
            "Salmoura ácida máx. 10 min | Peixe pochado no molho a 63–65 °C",
            listOf("rondeau", "chefmesser"),
            listOf(
                IngredientSpec("Lombo de robalo fresco em postas de 160 g", 640.0, "g", 1.08, "Açougue & Peixaria"),
                IngredientSpec("Tomate Concassés 8 mm, pimentões e cebola", 450.0, "g", 1.22, "Hortifrúti"),
                IngredientSpec("Leite de coco natural + azeite de dendê + coentro", 300.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Base Concassés & Pochê Curto", "Suar aromáticos na Rondeau, acomodar o peixe e pochar 8 min em fervura suave com leite de coco e dendê até 64 °C internos.", "Rondeau / Panela de Barro", 64, 12)
            ),
            isSwissmilk = false
        ),
        quickCantonalRecipe(
            "ernaehrung_menu_30g_fasern", 20, 4, 6, "CH",
            "Bowl Gastronômico Cardioprotetor (≥ 15 g Fibras)",
            "Lentilhas Puy, feijão branco, legumes em Rhomben salteados em Antihaft e damasco",
            "Salteado rápido em Antihaft-Pfanne preservando vitamina C + 15 g Nahrungsfasern/porção",
            listOf("poele_antiadhesif", "santoku"),
            listOf(
                IngredientSpec("Lentilhas verdes + feijão branco cozido al dente", 360.0, "g", 1.0, "Mercearia Seca"),
                IngredientSpec("Cenoura, salsão e pimentão em Rhomben", 320.0, "g", 1.15, "Hortifrúti"),
                IngredientSpec("Damasco seco picado + farelo de trigo tostado", 120.0, "g", 1.0, "Mercearia Seca")
            ),
            listOf(
                CookingStepSpec(1, "Saltear Rhomben em Poêle Antiadhésif", "Saltear os vegetais em losango rapidamente em antiaderente mantendo crocância e vitamina C.", "Poêle antiadhésif", 140, 5, SmartKitchenAlertType.NON_STICK_VITAMIN_C, "Obrigatório Poêle antiadhésif: preserva vitamina C e polifenóis antioxidantes.")
            ),
            isSwissmilk = false
        )
    )

    val allRecipes: List<RecipeSpec> by lazy {
        (RecipesCatalogV4.chefkochV4Recipes + baseAndSwissmilkRecipes)
            .sortedWith(compareBy({ it.weekNumber }, { !it.isChefkochV4 }, { it.title }))
    }

    val availableCantons: List<String> by lazy {
        allRecipes.map { it.cantonCode }
            .filter { it.isNotBlank() && it != "CH" && it != "BR" }
            .distinct()
            .sorted()
    }
}
