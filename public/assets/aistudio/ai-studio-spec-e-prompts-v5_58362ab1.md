# Pacote de Especificação v5 para Google AI Studio — Curso de Gastronomia (24 Semanas + Cadastro, Mural e Biblioteca Clássica & Contemporânea Ampliada)

Esta versão (**`v5`**) atualiza integralmente a especificação do aplicativo **"Atelier Gastronomia 24S"** (mantendo compatibilidade total com **Cadastro + Mural Comunitário no Supabase** da `v3` e com a base de **45 receitas e 60 flashcards** do `curso-24-semanas-dados-v4.json`) para incorporar as **6 brochuras atualizadas (`eixo-1-...-v3.pdf` a `eixo-6-...-v3.pdf`)** e o novo acervo de tratados clássicos e contemporâneos adicionados ao caderno:

- **Tradição Francesa & Molhos Contemporâneos:** *Institut Paul Bocuse Gastronomique*, *Paul Bocuse’s French Cooking*, *The Cuisine of Paul Bocuse*, *The Escoffier Cookbook* (*Le Guide Culinaire*), *New Larousse Gastronomique*, *Sauces: Classical and Contemporary Sauce Making* (James Peterson) e *Técnicas de Cozinha* (George Bento).
- **Cozinhas Regionais & Patrimônio Global:** *Fundamentos da Cozinha Italiana Clássica* (Marcella Hazan), *Cozinha Brasileira* (Luis Carlos Paravati) e *O Atlas Gastronômico* (Mina Holland — 39 cozinhas).
- **Confeitaria, Espessantes & Ciência:** *Espessantes na Confeitaria* (Sandra Canella-Rawls) e *Manual Prático de Confeitaria Senac* (Diego Rodrigues Costa et al.), somados a *Le Cordon Bleu: Sobremesas e suas Técnicas*.
- **Formação Suíça Profissional:** *Schnittarten*, *Kochgeschirrarten*, *Klassische_Gartechniken*, *Spezielle_Gartechniken (Sous-vide)*, *Rind*, *Teige*, *Kalte Süssspeisen*, *Ernaehrung*, *Betriebsorganisation*, *Brauner Kalbsfond*, *Geflügelgalantine*, *Vanilleauflauf*, *Berner Platte*, *Bollito misto* e *Bison-Mignons*, além dos **23 clássicos cantonais da *Swissmilk***.

---

## 1. Mapa Completo de Arquivos do Projeto (AI Studio + Supabase)

| Arquivo no Studio | Papel no Aplicativo | Onde Usar no Google AI Studio / Supabase |
| :--- | :--- | :--- |
| **`ai-studio-spec-e-prompts-v5.md`** *(este arquivo)* | Especificação atualizada de telas, prompts v5 e tabelas técnicas consolidadas | Guia mestre + prompts para Build e Gemini |
| **`curso-24-semanas-dados-v4.json`** | Base JSON estruturada (4 ciclos, 24 semanas, 45 receitas, 60 flashcards SM-2, `technical_references`, `community_and_portfolio_schema`) | Importar como fonte de dados única no AI Studio Build |
| **`eixo-1-...-v3.pdf` a `eixo-6-...-v3.pdf`** | As **6 brochuras v3 atualizadas** com todas as novas fontes, gráficos e flashcards | Pasta `public/docs/` (Material de Apoio & consulta) |
| **`ficha-mep-modelo.pdf` e `ficha-mep-exemplo-zurcher-rosti.pdf`** | Ficha MEP em branco (8 blocos) e exemplo comentado da Semana 8 | Botões de download e exemplo na Tela 3 |
| **`supabase-mural-setup-v3.sql`** | Script SQL com tabelas (`profiles`, `portfolio_posts`, `post_reactions`, `post_comments`), RLS e bucket `atelier-media` | Executar no **SQL Editor** do Supabase |
| **`supabaseClient.ts` e `mediaUpload.ts`** | Cliente Supabase (`VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` + fallback `localStorage`) e compressor `<canvas>` (máx. 1200 px, JPEG 0.82, sem EXIF) | Colocar em `src/lib/` na aba **Code** |
| **`atelier_gastronomia_24s_logo.jpg` e `atelier_gastronomia_24s_icon.jpg`** | Identidade visual (Precisão Suíça) para cabeçalho, login, favicon e cards PNG | Pasta `public/` ou `src/assets/` |

---

## 2. Prompt de Construção do App v5 (Copiar e Colar no AI Studio Build)

```text
Crie uma aplicação web responsiva em português (pt-BR) chamada "Atelier Gastronomia 24S", funcionando como um curso autoguiado de 24 semanas com comunidade de alunos, unindo a formação profissional suíça (Chefkoch / Swissmilk / Betty Bossi), os tratados clássicos e contemporâneos franceses (Escoffier, Paul Bocuse, Institut Paul Bocuse, James Peterson, New Larousse, George Bento), as cozinhas italiana (Marcella Hazan), brasileira (Dória, Paravati, Embrapa) e mundial (Mina Holland), confeitaria de precisão (Canella-Rawls, Diego Costa Senac, Le Cordon Bleu, King Arthur) e gestão/nutrição (Fachrechnen, BAB, KATAG, Führung, Ernaehrung, Betriebsorganisation, TBCA-USP, ANVISA RDC 216/2004).

Utilize `curso-24-semanas-dados-v4.json` como fonte única de verdade (4 ciclos, 24 semanas, 45 receitas incluindo os 23 clássicos Swissmilk e as 10 fichas profissionais Chefkoch, 4 níveis de equipamentos, 60 flashcards SM-2, `technical_references` e `community_and_portfolio_schema`) e conecte as 6 brochuras atualizadas (`eixo-1-...-v3.pdf` a `eixo-6-...-v3.pdf`) como material de apoio.

IDENTIDADE VISUAL, SUPABASE E PERSISTÊNCIA:
- Use `atelier_gastronomia_24s_logo.jpg` no cabeçalho/login e `atelier_gastronomia_24s_icon.jpg` como ícone do app (paleta Precisão Suíça: vermelho suíço sóbrio, cinza-ardósia e fundo claro de alto contraste).
- Conecte ao Supabase usando `supabaseClient.ts` (`VITE_SUPABASE_URL` e `VITE_SUPABASE_ANON_KEY`, com formulário de fallback em `localStorage` na tela de Perfil) e `mediaUpload.ts` (compressão `<canvas>` máx. 1200 px, JPEG 0.82, sem EXIF, bucket `atelier-media` e tabela `portfolio_posts`).
- Salve o progresso local (timer de 80 min, despensa FIFO, inventário de equipamentos, Fichas MEP e revisões SM-2) em `localStorage` com botões de Exportar/Importar Backup JSON.

IMPLEMENTE 6 TELAS PRINCIPAIS COM TODAS AS EXPANSÕES v5:

1. PAINEL DO CURSO (24 SEMANAS):
   - Linha do tempo nos 4 ciclos de 6 semanas: Ciclo 1 (Base: Sem. 1–6), Ciclo 2 (Intermediário: Sem. 7–12), Ciclo 3 (Avançado: Sem. 13–18) e Ciclo 4 (Chefkoch / Gestor: Sem. 19–24).
   - Card da "Semana Atual" com botão "Iniciar Bloco de Estudo (80 min)", receitas da semana (badges "Swissmilk + Cantão" e "Chefkoch Step-by-Step"), foco da Ficha MEP, links para as brochuras `-v3.pdf` da semana, revisões SM-2 pendentes hoje e galeria rápida das últimas fotos da semana.

2. AULA TEÓRICA, ATLAS TÉCNICO, BROCHURAS v3 & FLASHCARDS (SM-2):
   - Exibe os tópicos da semana no ciclo de 80 minutos (10 min Conceito → 20 min Visualização → 30 min Aplicação → 15 min Treino → 5 min Síntese) com cronômetro embutido.
   - Aba **"Material de Apoio & Atlas Técnico"**:
     * Acesso às 6 brochuras `-v3.pdf` e ao catálogo completo dos 23 Clássicos da Swissmilk filtrável por cantão (AG a ZH).
     * **Guia de Cortes Suíços (`Schnittarten`):** *Brunoise* (1,5 mm), *Jardinière* (5 mm), *Macédoine* (8 mm), *Paysanne* (1 cm), *Julienne* (1 mm × 3 cm / 3 mm Bento), *Bâtonnets* (5 mm × 3 cm), *Demidoff* (5 mm ondulado), *Chiffonnade*, *Rhomben* (wok), *Tourniertes Gemüse* (3 cm) e *Concassés* (8 mm), destacando a regra de facas afiadas contra esmagamento celular e oxidação.
     * **Atlas de Bateria e Lâminas (`Kochgeschirrarten`):** busca trilíngue (PT/DE/FR) de panelas (*Rôtissoire*, *Braisière*, *Poissonnière*, *Rondeau*, *Sauteuse*, *Sautoir*, *Lyoner Pfanne*, *Poêle antiadhésif*) e facas (*Rüstmesser*, *Tourniermesser*, *Santoku*, *Buntschneidemesser*).
     * **Mapa Anatômico Bovino (`Rind`):** os 8 grandes cortes (**A** *Stotzen* a **H** *Hals*) e seus subcortes com técnicas ideais.
     * **Atlas Cultural & Científico Rápido:**
       - Cozinha Italiana (*Marcella Hazan*): sequência *battuto → soffritto → insaporire*, *acqua*, massa fresca (ovos + farinha mole, sem sal/óleo/água) vs. seca e *risotto* (*all'onda* vs. piemontês).
       - Cozinha Brasileira (*Paravati* & *Dória*): mapa das 5 regiões (tucupi/jambú/maniçoba no Norte; acarajé/abará/vinagreira/tiquira no Nordeste; peixe azul-marinho/ora-pro-nóbis/moqueca capixaba no Sudeste; barreado/mojica no Sul/Centro-Oeste) + cadeia Biodiversidade → Ingredientes → Produtos → Receitas.
       - Volta ao Mundo (*Mina Holland*): os 7 refogados aromáticos regionais e as 9 misturas de especiarias globais (*Ras el Hanout*, *Berbere*, *Baharat*, *Dukkah*, *Garam Masala*, *Panch Phoron*, *Za'atar*, *Advieh*, *5 Especiarias*).
   - Flashcards interativos com algoritmo SM-2 (notas 0 a 5) cobrindo os 60 cartões de `flashcards_bank`.

3. RECEITAS (45 PREPAROS), MODO COZINHA & FICHA MEP DIGITAL (8 BLOCOS):
   - Exibe as 45 receitas do catálogo filtráveis por trilha (`base_global`, `swissmilk_23`, `chefkoch_step_by_step`), cantão ou semana, com seletor dinâmico de porções (`FC = Alvo / base_servings` e atalho rápido **4 vs. 10 porções**).
   - **Alertas Inteligentes de Bateria, Calor & Molhos no Modo Cozinha:**
     * Utensílio recomendado (`Braisière`, `Sautoir`, `Rondeau`, `Lyoner Pfanne`, `Antihaft-Pfanne`), alertando que **aço comum destrói vitamina C de vegetais salteados** e altera sabor de molhos.
     * Em frituras: **pré-fritar a 140 °C e finalizar a 165–175 °C (teto 175 °C contra acrilamida)**; em *Poelieren*: forno tampado sobre *matignon* a **140–160 °C**; em cocção por **Concentração vs. Expansão** (*Institut Paul Bocuse*).
     * Em cremes e molhos: alerta de **fervura (~98–100 °C) para inativar alfa-amilase** quando houver gema + amido (*Canella-Rawls*), limite **≤ 79–80 °C** no *crème anglaise*, e dicas de estabilização modernista (*James Peterson*: xantana 0,1–0,5%, lecitina 1–2%, alginato PG 0,3–0,9%, *fluid gel* de ágar).
   - **Ficha MEP Digital (8 Blocos)** espelhada na `ficha-mep-modelo.pdf` com botões para baixar o modelo em branco, abrir o exemplo preenchido (`ficha-mep-exemplo-zurcher-rosti.pdf`), botão **"Registrar Foto do Treino no Portfólio/Mural"** e botão **"Pedir Socorro ao Chef IA"**.

4. COMPRAS INTELIGENTES, SALMOURA & BATERIA DE EQUIPAMENTOS:
   - Aba Compras: consolida ingredientes por setor (`PB = PL * FC`) e controla despensa (FIFO).
   - Aba Equipamentos: inventário nos 4 níveis enriquecido com itens de `Kochgeschirrarten`, alertando utensílios faltantes antes do preparo.

5. AUTOAVALIAÇÃO, CALCULADORAS (INCL. ESPESSANTES/SALMOURA/FOLHADO) & GESTÃO DE BRIGADA:
   - Formulário *Selbstkontrolle* (notas 1–5) + exportador Markdown do *Dossier Lernreflexion*.
   - **Calculadoras Integradas:** (a) Perdas em Cascata (*Fachrechnen*); (b) Precificação Suíça (`BVP`, `EVP` c/ MWST 8%, `Prime-Cost`, `BE1`, `BE2`); (c) Ponto de Equilíbrio (`x_cr = Kf / (p - kv)`) e Nota Suíça 1–6; (d) **Salmoura *Sous-vide*** (5–10% = 50–100 g/L; 10 min peixes / 3–4 h carnes; banho +0,5–1 °C); (e) **Simulador de Folhado Suíço** (*Deutscher* 4 duplas vs. *Halb* 3 duplas); e (f) **Conversor de Gelatina & Hidrocoloides** (1 envelope 12 g pó = 6 folhas; 1 cc pó = 1 folha; dosagens % de xantana, ágar, carragena, pectina e Ultra-Sperse 3).
   - **Módulo Nutrição (`Ernaehrung`) & Brigada (`Betriebsorganisation`):** meta de **≥ 30 g/dia de fibras** (sequestro de ácidos biliares) com tabela de equivalências para 10 g de fibra, composição do amido (80% amilopectina / 20% amilose), os **13 postos da brigada clássica** e os **3 organogramas** (Hotel, Restaurante e Restauração Coletiva).

6. CADASTRO, PORTFÓLIO & MURAL COMUNITÁRIO (SUPABASE):
   - Cadastro/login (E-mail/Senha e Google opcional), perfil editável (`profiles`), abas **"Meu Portfólio"** (`private`/`public`) e **"Mural do Atelier"** (`public`).
   - Compressão `<canvas>` (`1200 px`, JPEG `0.82`, sem EXIF), filtros combinados (Semana, Ciclo, Eixo, Tipo de Material, *Swissmilk* por Cantão e *Chefkoch Step-by-Step*), reações, comentários, gerador de Card PNG compartilhável com a logo do app e análise visual opcional pelo Chef IA.
```

---

## 3. System Instructions do Tutor IA v5 (Copiar e Colar no Gemini / AI Studio)

```text
Você é o Chef Instrutor do curso "Atelier Gastronomia 24S", especialista na formação profissional suíça (Hotel & Gastro formation / Chefkoch / Pauli / Swissmilk 23 Clássicos / Betty Bossi), nos tratados franceses clássicos e contemporâneos (Auguste Escoffier, Paul Bocuse, Institut Paul Bocuse, New Larousse Gastronomique, James Peterson, George Bento, Le Cordon Bleu), nas cozinhas regionais (Marcella Hazan — Itália; Carlos Alberto Dória e Luis Carlos Paravati — Brasil; Newton Kalil — Árabe; Mina Holland — Atlas Mundial; Jean-Marie Pelt; Rodrigo Canto), na confeitaria científica e prática (Sandra Canella-Rawls, Diego Rodrigues Costa Senac, King Arthur Baking), na ciência e sensorialidade (Mariana Koppmann, Modernist Cuisine, WSET SAT, SCA CVA, IBA) e em gestão/nutrição (Fachrechnen, BAB/BeBu, KATAG, Führung, Betriebsorganisation, Ernaehrung, TBCA-USP, ANVISA RDC 216/2004).

MATERIAL DE APOIO DO CURSO:
- As 6 brochuras atualizadas (`eixo-1-...-v3.pdf` a `eixo-6-...-v3.pdf`) e a Ficha MEP em 8 blocos (`ficha-mep-modelo.pdf` e `ficha-mep-exemplo-zurcher-rosti.pdf`).
- Base estruturada `curso-24-semanas-dados-v4.json` (45 receitas, 60 flashcards e referências técnicas).

REGRAS DE ATUAÇÃO:
1. Idioma e vocabulário: Responda sempre em português do Brasil, preservando termos técnicos originais com explicação clara (ex.: mise en place, AVOR, brunoise, Jardinière, Braisière, Sautoir, battuto, soffritto, insaporire, concentração vs. expansão, dépouiller, vanner, tucupi, barrear, Gschwellti, Bratbutter, Nahrungsfasern).
2. Rigor sanitário e físico-químico:
   - Segurança alimentar: Zona de Perigo 5–60 °C (máx. 4 h); refrigeração ≤ 5 °C; cocção segura ≥ 70 °C geral / 74 °C aves / 71 °C moída / 63 °C peixe / 65 °C galantina de ave; lavagem (99%) vs. desinfecção após secagem completa (99,99%); uso pontual de luvas descartáveis; ovos pasteurizados para suflês e molhos crus emulsionados.
   - Bateria e toxinas: alerte que frigideiras de aço puro destroem vitamina C ao saltear vegetais (usar antiaderente) e prejudicam molhos; em frituras, pré-fritar a 140 °C e finalizar a 165–175 °C sem ultrapassar **175 °C** (acrilamida); pirólise > 180 °C.
   - Confeitaria e molhos: lembre da inativação da alfa-amilase (~98–100 °C) em cremes com gema + amido; limite de 79–80 °C para crème anglaise e 80–82 °C para cremes assados em banho-maria; dosagens de hidrocoloides (xantana 0,1–0,5%, lecitina 1–2%, alginato PG 0,3–0,9%, ágar fluid gel 0,01–0,9%) e equivalência de gelatina (12 g pó = 6 folhas).
   - Protocolo *Sous-vide*: salmoura 5%–10% (10 min peixes; 3–4 h carnes), banho 0,5–1 °C acima do centro térmico e peças ≤ 7 cm sem ossos.
3. Diagnóstico em Modo Cozinha: Ao receber um relato de falha (creme pâtissière que raleou por não ferver/alfa-amilase, sinérese em purê ou flan, fondue talhado, folhado encolhendo/acinzentado a 5 °C, suflê murchando, massa fresca italiana com óleo/sal impedindo absorção), dê a causa físico-química em 1 frase e a correção passo a passo.
4. Avaliação Visual de Fotos no Portfólio/Mural (Multimodal):
   - `prato_final`: avalie apenas o visível (douramento Maillard sem queima > 175 °C, brilho/emulsão sem sinérese, limpeza de bordas, contraste e proporção) — 2 elogios objetivos + 1 sugestão prática, sem inventar aroma/sabor.
   - `cortes_mep`: verifique padrão geométrico (*Brunoise* 1,5 mm, *Jardinière* 5 mm, *Macédoine* 8 mm, *Julienne* 1×30 mm, *Bâtonnets* 5×30 mm), integridade celular (faca afiada), escolha correta de bateria e separação cru × pronto.
   - `ficha_preenchida`: audite PB/PL/FC, limites térmicos (PCC), utensílio da etapa e reflexão (*Erkenntnisse* / *Offene Punkte*).
   - `resumo_esquema`: avalie síntese autoral em português e proporção de ~25% de diagramas/tabelas (*Portfolio Unterricht*).
5. Tutor Socrático em Gestão, Nutrição e Brigada: Exija cálculo passo a passo em *Fachrechnen* (`prod(1 - perda_i)`), `BVP/EVP` (MWST 8%), `BAB`, `Break-even` e nota suíça 1–6; em nutrição, reforce amido 80/20 e fibras ≥ 30 g/dia; em brigada, mapeie tarefas aos 13 postos clássicos.
```

---

## 4. Resumo Rápido das Atualizações v5 (Checklist de Conferência no AI Studio)

| Área Atualizada | Fontes Integradas na `v5` & Brochuras `-v3.pdf` | Onde Aparece no App |
| :--- | :--- | :--- |
| **Cortes, Bateria, Fundos & Cocções** | *Schnittarten*, *Kochgeschirrarten*, *Klassische Gartechniken*, *Institut Paul Bocuse*, *Paul Bocuse*, *Escoffier Cookbook*, *George Bento*, *New Larousse* | Tela 2 (Atlas Técnico) + Tela 3 (Alertas Vit. C / 175 °C / Concentração vs. Expansão) |
| **Molhos Clássicos & Modernistas** | *Sauces* (James Peterson) + *Escoffier* + *Brauner Kalbsfond* | Tela 3 (Receitas + dicas de xantana, lecitina, ágar *fluid gel*) |
| **Cozinhas Regionais & Patrimônio** | *Swissmilk* (23), *Berner Platte*, *Bollito*, *Bison*, *Marcella Hazan* (Itália), *Paravati* & *Dória* (Brasil), *Mina Holland* (39 cozinhas) | Tela 2 (Catálogo Swissmilk + Atlas Mundial/Brasil/Itália) + Tela 6 (Mural) |
| **Confeitaria, Folhados & Espessantes** | *Teige*, *Vanilleauflauf*, *Kalte Süssspeisen*, *Canella-Rawls*, *Diego Costa (Senac)*, *Le Cordon Bleu Sobremesas* | Tela 3 + Tela 5 (Simulador de Folhado, Equivalência Gelatina & Hidrocoloides) |
| **Nutrição, Fibras & Brigada** | *Ernaehrung*, *Betriebsorganisation*, *TBCA-USP*, *Figueira*, *Fachrechnen/BAB/Führung* | Tela 5 (Painel Fibras ≥ 30 g/dia, 13 Postos da Brigada, 3 Organogramas & Calculadoras) |

---

## 5. Ordem de Subida na Terça-feira (Passo a Passo)

1. No **Supabase**, rode **`supabase-mural-setup-v3.sql`** no *SQL Editor*, confira **Authentication → Providers** (Email/Google) e **URL Configuration**, e copie `Project URL` + `anon public key`.
2. No **Google AI Studio Build**, suba **`curso-24-semanas-dados-v4.json`**, as **2 imagens da marca** (`atelier_gastronomia_24s_logo.jpg` e `atelier_gastronomia_24s_icon.jpg`), as **6 brochuras `-v3.pdf`** e as **2 Fichas MEP** (`ficha-mep-modelo.pdf` e `ficha-mep-exemplo-zurcher-rosti.pdf`).
3. Cole o **Prompt de Construção v5** (Seção 2) para gerar o app, adicione **`supabaseClient.ts`** e **`mediaUpload.ts`** na pasta `src/lib/`, configure **`VITE_SUPABASE_URL`** e **`VITE_SUPABASE_ANON_KEY`** (ou informe direto no fallback da tela de Perfil) e cole as **System Instructions v5** (Seção 3) no modelo Gemini.
