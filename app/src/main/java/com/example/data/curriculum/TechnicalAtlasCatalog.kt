package com.example.data.curriculum

import com.example.domain.model.BrigadeStationSpec
import com.example.domain.model.CookwareBladeSpec
import com.example.domain.model.EquipmentTierItemSpec
import com.example.domain.model.EquipmentTierSpec
import com.example.domain.model.FiberEquivalentSpec
import com.example.domain.model.FlashcardSpec
import com.example.domain.model.OrgChartModelSpec
import com.example.domain.model.SousVideTargetSpec
import com.example.domain.model.SugarStageSpec
import com.example.domain.model.SwissBeefPrimalSpec
import com.example.domain.model.SwissCutSpec

/**
 * Atlas Técnico Oficial alimentado por `curso-24-semanas-dados-v4.json` (`technical_references`,
 * `equipment_tiers` e `flashcards_bank`) enriquecido com as correspondências brasileiras e v5.
 */
object TechnicalAtlasCatalog {

    const val DULL_BLADE_QUALITY_RULE =
        "REGRA DE OURO DE CUTELARIA & QUALIDADE: Facas cegas aumentam o risco de acidentes por deslizamento e esmagam as paredes celulares dos vegetais, provocando extravasamento de suco na tábua e oxidação enzimática acelerada."

    // 4.1 Matriz de 12 Cortes Suíços (swiss_cuts_matrix + variação 3 mm George Bento v5)
    val swissCuts: List<SwissCutSpec>
        get() = CourseJsonDatasetLoader.dataset.swissCuts

    // 4.2 Bateria & Cutelaria Trilíngue (DE / FR / PT)
    val cookwareAndBlades: List<CookwareBladeSpec>
        get() = CourseJsonDatasetLoader.dataset.cookwareAndBlades

    // 4.2b Enxoval Oficial de Equipamentos em 4 Níveis (equipment_tiers: 37 itens eq-...)
    val equipmentTiers: List<EquipmentTierSpec>
        get() = CourseJsonDatasetLoader.dataset.equipmentTiers

    val allEquipmentTierItems: List<EquipmentTierItemSpec>
        get() = CourseJsonDatasetLoader.dataset.allEquipmentItems

    // 4.3 Protocolos Sous-Vide Validados & Estágios do Açúcar LCB do JSON oficial
    val sousVideValidatedTargets: List<SousVideTargetSpec>
        get() = CourseJsonDatasetLoader.dataset.sousVideTargets

    val sugarStagesLcb: List<SugarStageSpec>
        get() = CourseJsonDatasetLoader.dataset.sugarStagesLcb

    // 4.4 Mapa Anatômico Bovino Suíço (Rind A–H) com subcortes e equivalentes brasileiros
    val swissBeefPrimals: List<SwissBeefPrimalSpec> = listOf(
        SwissBeefPrimalSpec(
            "A", "Stotzen", "Cuisse", "Traseiro Completo (Coxão Mole, Coxão Duro, Patinho, Lagarto e Músculo)",
            "Misto (baixo colágeno no Eckstück; alto no Schenkel/Rosenstück)",
            listOf(
                "Eckstück (Coxão mole / Chã de dentro — Schnitzel, Geschnetzeltes)",
                "Nuss (Patinho) & Unterspälte (Coxão duro — Fachrechnen 1.3)",
                "Runder Mocken (Lagarto) & Schenkel (Músculo / Garrão p/ Ossobuco)"
            ),
            listOf("Sautieren (Eckstück)", "Schmoren / Brasear", "Dünsten", "Sieden (Cozido)")
        ),
        SwissBeefPrimalSpec(
            "B", "Nierstück", "Aloyau", "Lombo Nobre (Filé-Mignon, Contrafilé / Roastbeef e Alcatra / Huft)",
            "Baixo (cortes nobres de cocção rápida por concentração)",
            listOf(
                "Filetkopf (Chateaubriand), Filetherz (Tournedos) e Filetspitz (Mignons / Tatar)",
                "Roastbeef / Contre-filet (Peça inteira, Entrecôte, Doppeltes Entrecôte)",
                "Com osso: Kotelett, Club-Steak, T-Bone, Porterhouse",
                "Huft / Huftdeckel / Huftzapfen (Rumpsteak, Picanha p/ Bollito misto, Geschnetzeltes)"
            ),
            listOf("Sautieren em Sautoir", "Grillieren", "Braten", "Sieden / Schmoren (Huftdeckel)")
        ),
        SwissBeefPrimalSpec(
            "C", "Lempen", "Flanc", "Fraldinha / Vazio / Aba da Costela",
            "Alto tecido conjuntivo e fibras longas",
            listOf("Dünner Lempen / Bavette (Fraldinha limpa)", "Dicker Lempen (Vazio espesso)"),
            listOf("Grelhar rápido contra a fibra (Bavette)", "Schmoren (Brasear)", "Sieden (Caldos)")
        ),
        SwissBeefPrimalSpec(
            "D", "Rücken", "Train de côtes", "Entrecôte Dianteiro / Acém Nobre (Hohrücken)",
            "Misto com excelente marmoreio intramuscular",
            listOf("Hohrücken / Côte de boeuf (Bife Ancho / Ribeye)", "Siedfleisch Hohrücken (Berner Platte ~90 min)"),
            listOf("Grillieren", "Braten", "Sieden em água quente salgada (Berner Platte)")
        ),
        SwissBeefPrimalSpec(
            "E", "Schulter", "Épaule", "Paleta Bovina (Raquete, Peixinho e Miolo da Paleta)",
            "Alto colágeno (converte-se em gelatina untuosa em cocção longa)",
            listOf("Dicke Schulter (Miolo da paleta)", "Schulterfilet (Peixinho)", "Bugstück (Raquete / Sete)"),
            listOf("Schmoren (Thurgauer Sauerbraten / Brasato)", "Ragout", "Barreado lento")
        ),
        SwissBeefPrimalSpec(
            "F", "Federstück", "Côte plate", "Costela Ponta de Agulha / Costela Minga",
            "Alto teor de colágeno intermuscular e gordura entremeada",
            listOf("Short Ribs / Costela em tiras com osso", "Costela desossada p/ cozido"),
            listOf("Sieden lento (Pot-au-feu / Siedfleisch)", "Schmoren / Baixa temperatura")
        ),
        SwissBeefPrimalSpec(
            "G", "Brust", "Poitrine", "Peito Bovino / Maçã do Peito (Brisket)",
            "Alto tecido conjuntivo e membrana peitoral",
            listOf("Brustkern (Maçã do peito)", "Brustspitz (Ponta de peito)"),
            listOf("Sieden aromático", "Sous-vide prolongado (62 °C / 15 h)", "Braseado lento")
        ),
        SwissBeefPrimalSpec(
            "H", "Hals", "Cou", "Acém do Pescoço / Pescoço Bovino",
            "Altíssimo teor de colágeno (musculatura de sustentação)",
            listOf("Pescoço desossado em cubos", "Moagem profissional (PCC ≥ 70–71 °C)"),
            listOf("Braseados longos (Ragout / Goulash)", "Blends e recheios cozidos")
        )
    )

    // 4.5 Equivalências oficiais para 10 g de fibra alimentar (10 itens de ten_grams_fiber_equivalents)
    val fiberEquivalentsFor10g: List<FiberEquivalentSpec>
        get() = CourseJsonDatasetLoader.dataset.fiberEquivalentsFor10g

    // 4.6 As 13 Funções Clássicas da Brigada Suíça (classic_13_roles) + 3 Organogramas
    val classicBrigade13Stations: List<BrigadeStationSpec>
        get() = CourseJsonDatasetLoader.dataset.classicBrigade13Stations

    val operationalOrgCharts: List<OrgChartModelSpec> = listOf(
        OrgChartModelSpec(
            "org_hotel",
            "1. Hotel (Management, Admin, F&B, Rooms Division)",
            "Direção Geral → F&B / Küchenchef → Sous-chef → Todas as praças especializadas ativas (Saucier, Rôtisseur, Entremetier, Garde-manger, Pâtissier, Chef de garde, Mitarbeiterkoch e Tournant).",
            listOf("Küchenchef", "Sous-chef", "Saucier", "Rôtisseur", "Entremetier", "Garde-manger", "Pâtissier", "Chef de garde", "Mitarbeiterkoch", "Tournant", "Lernender EFZ"),
            "Operação simultânea (restaurante, banquetes e room service), exigindo controle rigoroso de Prime-Cost (~72%)."
        ),
        OrgChartModelSpec(
            "org_restaurant",
            "2. Restaurante (Management, Admin, F&B, Stewarding)",
            "Gerência + Küchenchef no passe → Praças agrupadas (Quente: Saucier + Rôtisseur | Guarnições: Entremetier | Fria & Doce: Garde-manger + Pâtissier) + Stewarding integrado.",
            listOf("Küchenchef", "Sous-chef / Saucier", "Entremetier", "Garde-manger + Pâtissier", "Tournant", "Stewarding"),
            "Estrutura enxuta (margem líquida benchmark KATAG 1–5%) dependente de AVOR pré-serviço impecável."
        ),
        OrgChartModelSpec(
            "org_collective",
            "3. Restauração Coletiva (Gemeinschaftsgastronomie)",
            "Gestão de Produção + Diätkoch → Cocção em volume (Kombisteamer, Cook-and-Chill, Sous-vide), praça de dietoterapia clínica isolada e porcionamento rastreável.",
            listOf("Küchenchef", "Diätkoch", "Entremetier Integral", "Garde-manger Sanitizado", "Pâtissier", "Hilfskoch"),
            "Previsibilidade de demanda, desperdício zero e rigor absoluto em dietoterapia (DRC, celíacos, alergênicos) e APPCC."
        )
    )

    // 4.7 Banco Oficial de 60 Flashcards SM-2 (fc-e1-1 a fc-e6-10)
    val flashcards: List<FlashcardSpec>
        get() = CourseJsonDatasetLoader.dataset.flashcards
}
