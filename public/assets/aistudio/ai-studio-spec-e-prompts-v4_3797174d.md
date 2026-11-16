# Pacote de Especificação v4 para Google AI Studio — Curso de Gastronomia (24 Semanas + Cadastro, Mural e Formação Técnica Suíça Ampliada)

Esta versão (`v4`) atualiza a especificação do aplicativo **"Atelier Gastronomia 24S"** (evoluindo a `v3` com **Cadastro + Mural Comunitário no Supabase**) para incorporar integralmente as **novas fontes técnicas suíças e de confeitaria** adicionadas ao caderno: cortes padronizados (*Schnittarten*), bateria e lâminas trilíngues (*Kochgeschirrarten*), regras avançadas de cocção clássica (*Klassische Gartechniken*), protocolo *Sous-vide* com salmoura (*Spezielle Gartechniken*), anatomia bovina suíça (*Rind*), novas fichas profissionais passo a passo (*Brauner Kalbsfond*, *Geflügelgalantine*, *Berner Platte*, *Bison-Mignons*, *Bollito misto*, *Vanilleauflauf*, *Gestürzte Karamellcreme*), massas folhadas suíças (*Teige*), sobremesas geladas e caldas (*Le Cordon Bleu*), carboidratos e fibras (*Ernaehrung*) e estrutura clássica de brigada e organogramas (*Betriebsorganisation*).

---

## 1. Arquitetura de Arquivos e Configuração (AI Studio + Supabase)

| Arquivo | Função no Projeto | Onde Usar |
| :--- | :--- | :--- |
| **`ai-studio-spec-e-prompts-v4.md`** *(este arquivo)* | Especificação completa de telas, prompts atualizados e tabelas técnicas de referência para o Build | Guia de construção e prompts no Google AI Studio |
| `curso-24-semanas-dados-v3.json` *(ou futura `v4`)* | Base JSON do curso (24 semanas, 4 ciclos, 35 receitas incluindo os 23 clássicos *Swissmilk*, 48 flashcards e schema da comunidade) | Importar no projeto do Google AI Studio Build |
| `supabase-mural-setup-v3.sql` | Script SQL completo (`profiles`, `portfolio_posts`, `post_reactions`, `post_comments`, RLS e bucket `atelier-media`) | Executar no **SQL Editor** do Supabase |
| `supabaseClient.ts` e `mediaUpload.ts` | Cliente Supabase com fallback em `localStorage` e compressor `<canvas>` (máx. 1200 px, JPEG 0.82, sem EXIF) | Colocar em `src/lib/` na aba **Code** do AI Studio |
| `atelier_gastronomia_24s_logo.jpg` e `atelier_gastronomia_24s_icon.jpg` | Identidade visual (Precisão Suíça) para cabeçalho, login, favicon e cards compartilháveis | Pasta `public/` ou `src/assets/` |
| `eixo-1-...-v2.pdf` a `eixo-6-...-v2.pdf` + `ficha-mep-modelo.pdf` + `ficha-mep-exemplo-zurcher-rosti.pdf` | Material de apoio, bibliografias e modelos de Ficha MEP (em branco e exemplo *Zürcher Geschnetzeltes + Rösti*) | Pasta de documentos de apoio / consulta no app |

> **Nota sobre compatibilidade de dados:** Este `.md` (`v4`) mantém total compatibilidade com o banco Supabase (`supabase-mural-setup-v3.sql`), com `supabaseClient.ts` e com `mediaUpload.ts`. As novas técnicas, tabelas de referência e receitas suíças detalhadas na **Seção 4** estão descritas de forma autossuficiente para o AI Studio Build injetar no app diretamente a partir deste documento e da base JSON.

---

## 2. Prompt de Construção do App v4 (Copiar e Colar no AI Studio Build)

```text
Crie uma aplicação web responsiva em português (pt-BR) chamada "Atelier Gastronomia 24S", funcionando como um curso autoguiado de 24 semanas com comunidade de alunos, partindo dos fundamentos técnicos e segurança alimentar até a alta confeitaria, técnicas profissionais suíças, culinária internacional e gestão de brigada.

Utilize o arquivo `curso-24-semanas-dados-v3.json` como base estrutural do curso (4 ciclos, 24 semanas, 35 receitas iniciais incluindo os 23 clássicos da Swissmilk, 4 níveis de equipamentos, 48 flashcards SM-2 e schema da comunidade) e expanda os módulos técnicos e o catálogo de preparações com o Núcleo Profissional Suíço e de Sobremesas especificado abaixo.

IDENTIDADE VISUAL, BACKEND E PERSISTÊNCIA:
- Utilize `atelier_gastronomia_24s_logo.jpg` no cabeçalho/login e `atelier_gastronomia_24s_icon.jpg` como ícone do app (paleta Precisão Suíça: vermelho suíço sóbrio, cinza-ardósia e fundo claro de alto contraste para bancada).
- Conecte ao Supabase usando `supabaseClient.ts` (`VITE_SUPABASE_URL` e `VITE_SUPABASE_ANON_KEY`, com fallback configurável via `localStorage` na tela de Perfil) e `mediaUpload.ts` (compressão automática em `<canvas>` para largura máx. 1200 px em JPEG 0.82 sem EXIF, envio para `atelier-media` e criação de registros em `portfolio_posts`).
- Mantenha o progresso local (timer de 80 min, despensa FIFO, inventário de equipamentos, estado da Ficha MEP e revisões SM-2) em `localStorage` com exportação/importação de backup JSON.

IMPLEMENTE 6 TELAS PRINCIPAIS + EXPANSÕES TÉCNICAS v4:

1. PAINEL DO CURSO (24 SEMANAS):
   - Linha do tempo nos 4 ciclos de 6 semanas: Ciclo 1 (Base: Sem. 1–6), Ciclo 2 (Intermediário: Sem. 7–12), Ciclo 3 (Avançado: Sem. 13–18) e Ciclo 4 (Chefkoch / Gestor: Sem. 19–24).
   - Card da "Semana Atual" exibindo: botão "Iniciar Bloco de Estudo (80 min)", receitas da semana (com badge de cantão para os 23 clássicos Swissmilk e badge "Chefkoch Step-by-Step" para as fichas profissionais), foco da Ficha MEP, atalhos para as brochuras de apoio (`eixo-1-...-v2.pdf` a `eixo-6-...-v2.pdf`), revisões SM-2 pendentes e resumo de fotos enviadas.

2. AULA TEÓRICA, DICIONÁRIOS TÉCNICOS & FLASHCARDS (SM-2):
   - Exibe os tópicos da semana no ciclo de 80 minutos (10 min Conceito → 20 min Visualização → 30 min Aplicação → 15 min Treino → 5 min Síntese) com cronômetro embutido.
   - Aba **"Material de Apoio & Atlas Técnico"**:
     * Acesso às 6 brochuras e ao catálogo completo dos 23 Clássicos da Swissmilk filtrável por cantão (AG a ZH).
     * **Guia de Cortes Suíços (`Schnittarten`):** tabela interativa com dimensões e ferramentas — *Brunoise* (~1,5 mm), *Jardinière* (~5 mm), *Macédoine* (~8 mm), *Paysanne* (~1 cm fino), *Julienne* (1 mm × 3 cm), *Bâtonnets* (5 mm × 3 cm), *Demidoff* (~5 mm c/ *Buntschneidemesser*), *Chiffonnade*, *Rhomben* (losangos p/ wok), *Gemüseperlen*, *Tourniertes Gemüse* (~3 cm) e *Concassés* (~8 mm). Incluir regra de segurança/qualidade: facas cegas esmagam células vegetais, aumentam perda de suco e oxidação.
     * **Atlas de Bateria e Lâminas (`Kochgeschirrarten`):** busca trilíngue (Português / Alemão / Francês) cobrindo *Rôtissoire*, *Braisière*, *Poissonnière*, *Rondeau*, *Sauteuse*, *Sautoir*, *Poêle lyonnaise*, *Poêle antiadhésif*, *Cul de poule* e facas (*Rüstmesser*, *Tourniermesser*, *Santoku*, *Ausbeinmesser*, *Filetiermesser*, *Buntschneidemesser*, *Kanneliermesser*, *Zestenmesser*).
     * **Mapa Anatômico Bovino Suíço (`Rind`):** os 8 grandes cortes (**A** *Stotzen*, **B** *Nierstück*, **C** *Lempen*, **D** *Rücken*, **E** *Schulter*, **F** *Federstück*, **G** *Brust*, **H** *Hals*) divididos por teor de tecido conectivo e subcortes do *Nierstück* (*Filetkopf/Chateaubriand*, *Filetherz/Tournedos*, *Filetspitz/Mignons*, *Roastbeef/Entrecote*, *T-Bone/Porterhouse*, *Huft/Huftdeckel*) e do *Stotzen* (*Unterspälte*, *Runder Mocken*, *Eckstück*, *Nuss*, *Schenkel*), ligados às técnicas ideais (*Sautieren*, *Grillieren*, *Braten*, *Schmoren*, *Sieden*).
   - Flashcards interativos com algoritmo SM-2 (notas 0–5), incluindo novos cards sobre acrilamida (175 °C), perda de vitamina C no aço, salmoura *Sous-vide* (5–10%), temperatura interna da galantina (65 °C), folhados suíços, fibras (≥ 30 g/dia) e brigada clássica.

3. RECEITAS, MODO COZINHA & FICHA MEP DIGITAL (8 BLOCOS):
   - Além das 35 receitas iniciais da base JSON, integre ao catálogo estas **8 fichas profissionais e preparos clássicos adicionais** (com seletor rápido **4 vs. 10 porções** ou escala dinâmica pelo Fator de Conversão `FC = Alvo / base_servings`):
     1) **Brauner Kalbsfond (Fundo Escuro de Vitela, Sem. 3 — 5 L):** 4 kg ossos picados + 1 kg pés de vitela + 800 g *mirepoix* + 120 g extrato de tomate + 1 kg vinho branco + 7 kg água + 40 g sal; deglaçar 2–3x até *glace*, cozinhar em fervura branda por 3 h, ervas na última hora e passar em pano.
     2) **Berner Platte (Sem. 10 — 4/10 pess.):** controle paralelo de cocções distintas — língua defumada (~120 min), *Siedfleisch Hohrücken* (~90 min), bacon (~60 min), costeletas e *Berner Zungenwurst* pochadas a ~82 °C, vagens branqueadas com choque térmico e estufadas com *Bohnenkraut*.
     3) **Bollito misto mit Salsa verde (Sem. 11 — 4/10 pess.):** *Huftdeckel*, ombro de vitela e língua (~1 h), *Zampone* (~50 min), frango pochado no fundo de ave e *Salsa verde* emulsionada (salsa, aliche, alho, alcaparra, gema pasteurizada, azeite e limão).
     4) **Bison-Mignons mit Thymiankruste (Sem. 12 — 4/10 pess.):** mignons de 60 g com corte em grade de 2 mm, mostarda inglesa + vinho branco + crosta de tomilho/cebolinha/farinha, purê de cebolinha reduzida no vinho tinto, cocção *saignant* e *Wild-Demi-glace* com creme 35% e gin.
     5) **Deutscher / Halb / Blitzblätterteig (Sem. 14 — base 1 kg):** 420 g farinha T400 + 300 g margarina de folhado + 65 g manteiga + 210 g água + 10 g sal + 2 g malte em 4 dobras duplas (ou 3 dobras duplas no *Halbblätterteig*), com guia de 13 formatos de *Blätterteigkonfekt*.
     6) **Vanilleauflauf / Suflê de Baunilha (Sem. 16 — 4/10 pess.):** ramequins untados c/ manteiga e farinha T550, massa cozida tipo *roux*+leite infundido até soltar da panela, gemas + claras em neve até ¾ da forma, pré-aquecimento em banho-maria no fogão e forno a 200 °C por 15–20 min.
     7) **Gestürzte Karamellcreme (Sem. 16 — 10 pess.):** caramelo claro nas formas (200 g açúcar + 50 g água), aparelho de leite (625 g), baunilha, ovos (190 g), gemas (50 g) e açúcar (95 g) coberto com filme plástico e pochado em banho-maria a 80 °C (descanso de meio dia refrigerado antes de desenformar).
     8) **Geflügelgalantine (Sem. 17 — 10 pess.) & Protocolos Sous-vide:** frango de 1 kg desossado pelas costas mantendo pele íntegra, *farce* de coxa + creme 35% passada no *tamis* sobre gelo com frutas secas marinadas em *Maraschino*, pochado no fundo de ave até **65 °C internos**; mais fichas *Sous-vide* de **Filé de Bacalhau (55 °C / 2h30)**, **Salmão 25 mm (57,5 °C / 125 min)** e **Peito de Vitela (62 °C / 15 h após 3 h em salmoura)**.
   - **Alertas Inteligentes de Bateria e Calor no Modo Cozinha:**
     * Exibir qual utensílio específico (`Braisière`, `Sautoir`, `Rondeau`, `Lyoner Pfanne` ou `Antihaft-Pfanne`) deve ser usado na etapa, alertando que **frigideiras de aço puro destroem a vitamina C de vegetais salteados** e não são indicadas para construir molhos.
     * Em preparos fritos por imersão, exibir alerta duplo: **pré-fritar a 140 °C** e **finalizar a 165–175 °C (teto máximo de 175 °C para evitar acrilamida)**.
     * Em *Poelieren*, indicar forno tampado sobre *matignon* a **140–160 °C** sem líquido adicionado.
   - **Ficha MEP Digital (8 Blocos) + Botão "Registrar Foto do Treino":** espelho completo da `ficha-mep-modelo.pdf` e acesso ao exemplo `ficha-mep-exemplo-zurcher-rosti.pdf`, com pré-preenchimento direto para o Mural/Portfólio.

4. COMPRAS INTELIGENTES, SALMOURA & BATERIA DE EQUIPAMENTOS:
   - Aba Compras: consolida ingredientes das receitas da semana (incluindo os 23 clássicos *Swissmilk* e as 8 novas fichas) por setor, aplicando `PB = PL * FC` e controle **FIFO**.
   - Aba Equipamentos: inventário nos 4 níveis enriquecido com os itens de bateria e lâminas especiais de `Kochgeschirrarten`, alertando automaticamente utensílios faltantes antes do preparo.

5. AUTOAVALIAÇÃO, CALCULADORAS & GESTÃO DE BRIGADA / NUTRIÇÃO:
   - Formulário *Selbstkontrolle* (notas 1–5 nos 4 critérios + *Erkenntnisse* e *Offene Punkte*) e exportador *Dossier Lernreflexion*.
   - **Calculadoras Integradas:**
     * (a) Perdas em Cascata (*Fachrechnen* — `Compra = Porção / prod(1 - perda_i)`);
     * (b) Precificação Suíça (`BVP`, `EVP` c/ MWST 8%, `Prime-Cost`, `BE1`, `BE2`);
     * (c) Ponto de Equilíbrio (`x_cr = Kf / (p - kv)`) e Conversor de Nota Suíça 1–6;
     * (d) **Calculadora de Salmoura *Sous-vide*** (5% a 10% = 50 a 100 g de sal por litro de água; indicando 10 min para peixes e 3–4 h para carnes grandes, além de ajuste de banho +0,5 a +1 °C acima do centro térmico);
     * (e) **Simulador de Folhado Suíço** (calcula massa base e recomenda 3 dobras duplas para ~0,5 kg de gordura/kg de farinha ou 4 dobras duplas para 0,6–0,7 kg/kg, alertando contra armazenamento prolongado a 5 °C).
   - **Módulo Nutrição (*Ernaehrung*) & Brigada (*Betriebsorganisation*):**
     * Painel de carboidratos e meta de **≥ 30 g/dia de fibras** (explicando o sequestro de ácidos biliares na redução de colesterol) com equivalências rápidas para 10 g de fibra (20 g farelo de trigo, 50 g damasco seco, 55 g feijão branco, 100 g lentilhas, 150 g pão Graham).
     * Simulador dos **13 postos da brigada clássica** (*Küchenchef* ao *Hilfskoch*, destacando *Diätkoch* e *Mitarbeiterkoch*) e comparação dos **3 organogramas operacionais** (Hotel, Restaurante e Restauração Coletiva).

6. CADASTRO, PORTFÓLIO & MURAL COMUNITÁRIO (SUPABASE):
   - Mantenha integralmente o cadastro de perfil (`profiles`), abas "Meu Portfólio" (`private`/`public`) e "Mural do Atelier" (`public`), compressão `<canvas>` (`1200 px`, JPEG `0.82`, sem EXIF), filtros combinados (Semana, Ciclo, Eixo, Tipo de Material, *Clássicos Swissmilk* e *Chefkoch Step-by-Step*), reações, comentários, gerador de Card PNG compartilhável com a logo do app e avaliação visual opcional pelo Chef IA.
```

---

## 3. System Instructions do Tutor IA v4 (Copiar e Colar no Gemini / AI Studio)

```text
Você é o Chef Instrutor do curso "Atelier Gastronomia 24S", especialista na formação profissional suíça (Hotel & Gastro formation / Chefkoch / Swissmilk 23 Clássicos / Betty Bossi / Pauli), nos tratados clássicos (Chef Profissional CIA, Le Cordon Bleu — Técnicas Culinárias & Sobremesas, Escoffier, Dumas), na ciência dos alimentos (Mariana Koppmann, Modernist Cuisine), na gastronomia brasileira e mediterrânea (Dória, Embrapa, Kalil, Pelt, Canto), em análise sensorial/bebidas (WSET SAT, SCA CVA, IBA) e em gestão/nutrição (Fachrechnen, BAB/BeBu, KATAG, Führung, Betriebsorganisation, Ernaehrung, TBCA-USP, ANVISA RDC 216/2004).

MATERIAL DE APOIO DO CURSO:
- As 6 brochuras técnicas (`eixo-1-...-v2.pdf` a `eixo-6-...-v2.pdf`) e a Ficha MEP em 8 blocos (`ficha-mep-modelo.pdf` e `ficha-mep-exemplo-zurcher-rosti.pdf`).
- O Núcleo Suíço Ampliado: 23 receitas cantonais *Swissmilk* + fichas profissionais passo a passo (*Brauner Kalbsfond*, *Geflügelgalantine*, *Berner Platte*, *Bollito misto*, *Bison-Mignons*, *Vanilleauflauf*, *Gestürzte Karamellcreme*, *Kabeljau* e *Kalbsbrust Sous-vide*).

REGRAS DE ATUAÇÃO:
1. Idioma: Responda sempre em português do Brasil, preservando termos técnicos originais (ex.: mise en place, brunoise, Jardinière, Braisière, Sautoir, Gschwellti, Bratbutter, Sous-vide, Maillard, Prime-Cost, Selbstkontrolle, Nahrungsfasern) com explicação objetiva.
2. Rigor sanitário e técnico:
   - Segurança alimentar: Zona de Perigo 5–60 °C (máx. 4 h); refrigeração ≤ 5 °C; cocção segura ≥ 70 °C geral / 74 °C aves / 71 °C carne moída / 63 °C peixe / 65 °C galantina de ave; lavagem (99%) vs. desinfecção das mãos após secagem completa (99,99%); uso pontual de luvas descartáveis.
   - Controle de bateria e toxinas: alerte que frigideiras de aço puro destroem vitamina C ao saltear vegetais (usar antiaderente) e não servem para construir molhos; em frituras por imersão, pré-fritar a 140 °C e finalizar a 165–175 °C sem ultrapassar **175 °C** para evitar acrilamida.
   - Protocolo *Sous-vide*: salmoura de **5% a 10%** (50–100 g/L — 10 min p/ peixes, 3–4 h p/ carnes grandes), banho **0,5 a 1 °C** acima do centro térmico desejado e peças até 7 cm de espessura sem ossos.
3. Diagnóstico em Modo Cozinha: Diante de falhas relatadas (fondue talhado, folhado encolhendo ou acinzentado por ficar longo tempo a 5 °C, gordura vazando por falta de dobras, suflê murchando, caramelo amargo ou creme caramelado empolado), diagnostique a causa físico-química em 1 frase e dê a correção imediata passo a passo.
4. Avaliação Visual de Fotos no Portfólio/Mural (Multimodal):
   - `prato_final`: avalie cor de douramento (Maillard sem queima > 175 °C), brilho/emulsão do molho (*nappé*), limpeza de bordas, contraste e proporção — sempre 2 elogios objetivos + 1 sugestão prática, sem inventar aroma ou sabor.
   - `cortes_mep`: verifique padrão geométrico (*Brunoise* 1,5 mm, *Jardinière* 5 mm, *Macédoine* 8 mm, *Julienne* 1 mm × 3 cm, *Bâtonnets* 5 mm × 3 cm), integridade celular (faca afiada sem esmagar), organização da bateria correta e separação cru × pronto.
   - `ficha_preenchida`: audite preenchimento de PB/PL/FC, limites térmicos (PCC), escolha do utensílio correto e reflexão (*Erkenntnisse* / *Offene Punkte*).
   - `resumo_esquema`: avalie síntese própria em português e cumprimento da proporção de ~25% de diagramas/tabelas (*Portfolio Unterricht*).
5. Tutor Socrático em Gestão, Nutrição e Brigada: Em *Fachrechnen*, exija cálculo passo a passo com produto `(1 - perda_i)` na ordem inversa; em nutrição (*Ernaehrung*), oriente metas de fibras (≥ 30 g/dia e remoção de ácidos biliares) e estrutura de amidos; em organização (*Betriebsorganisation*), relacione a tarefa ao posto exato entre os 13 cargos da brigada clássica.
```

---

## 4. Referências Técnicas Integradas para Implementação no App (Novas Fontes)

### 4.1 Matriz de Cortes Padronizados (*Schnittarten*)

| Corte | Dimensão / Formato Suíço | Ferramenta & Aplicações Típicas |
| :--- | :--- | :--- |
| **Brunoise** | Cubos de **~1,5 mm** de aresta | Faca do chef; cenoura, alho-poró, aipo-rábano, cogumelos, pimentão, *Bündner Fleisch* |
| **Jardinière** | Cubos de **~5 mm** de aresta | Cenoura, aipo-rábano, pepino, couve-rábano (*Kohlrabi*), bacon cozido |
| **Macédoine** | Cubos de **~8 mm** de aresta | Legumes (abóbora, cenoura, aipo) e frutas (pêssego, manga, maçã, pera) |
| **Paysanne** | Lâminas finas de **~1 cm** | Faca do chef; cenoura, alho-poró, repolho crespo (*Wirz*), aipo, batata |
| **Julienne** | Tiras de **~1 mm × 3 cm** | Cenoura, alho-poró, aipo, cogumelos, presunto cru/cozido |
| **Bâtonnets** | Bastões de **~5 mm × 3 cm** | Batatas, cenouras, couve-rábano, bacon cozido |
| **Chiffonnade** | Tiras finíssimas em fita | Folhas (alface, alho-de-urso, azedinha) |
| **Demidoff** | Rodelas onduladas de **~5 mm** | **Buntschneidemesser**; cenoura, pepino, abobrinha, aipo-rábano |
| **Rhomben** | Losangos rústicos | **Gemüsemesser**; alho-poró, cenoura, salsão, pimentão e ervilha-torta para **Wok** |
| **Tourniertes Gemüse** | Oval alongado de **~3 cm** (7 faces) | **Tourniermesser**; cenoura, aipo, pepino, abobrinha, beterraba |
| **Concassés** | Cubos de **~8 mm** (tomate) | Tomate branqueado, pelado, cortado ao meio e sem sementes |

### 4.2 Bateria Profissional (*Kochgeschirrarten*) e Regras de Uso (*Klassische Gartechniken*)

| Alemão (DE) | Francês (FR) | Inglês / Função | Regra Técnica no Modo Cozinha |
| :--- | :--- | :--- | :--- |
| *Bratgeschirr* | ***Rôtissoire*** | Assadeira funda de forno | Dourar ossos sem gordura no forno (ex.: *Brauner Kalbsfond*) |
| *Schmorgeschirr* | ***Braisière*** | Caçarola de brasear com tampa | Brasear carnes ricas em colágeno e *Poelieren* (140–160 °C tampado) |
| *Niedriger Bratentopf* | ***Rondeau*** | Caçarola baixa e larga c/ alças | Selar carnes cuja borra gera molho escuro; *Poelieren* |
| *Flachkasserolle* | ***Sautoir*** | Caçarola rasa de cabo longo | Saltear proteínas para construir molho na deglaçagem |
| *Schwenkkasserolle* | ***Sauteuse*** | Frigideira alta de bordas inclinadas | Saltear agitando e finalizar molhos com movimento |
| *Bratpfanne (Antihaft)* | ***Poêle antiadhésif*** | Frigideira antiaderente | **Obrigatória para saltear vegetais** (frigideira de aço destrói vitamina C) |
| *Lyoner Pfanne* | ***Poêle lyonnaise*** | Frigideira de ferro/aço | Saltear carnes/peixes sem molho na panela; tostar cebola cortada (*Berner Platte*) |
| *Fischkessel* | ***Poissonnière*** | Peixeira com grelha interna | Pochar peixes inteiros delicados em *court-bouillon* |

### 4.3 Anatomia Bovina Suíça (*Rind*) e Protocolos *Sous-Vide*

- **Os 8 Grandes Cortes (*Rind*):** **A. *Stotzen*** (*Cuisse* — *Unterspälte*, *Runder Mocken*, *Rosenstück*, *Eckstück*, *Nuss*, *Schenkel*, *Weisses Stück*); **B. *Nierstück*** (*Aloyau* — *Filet* [*Filetkopf*, *Filetherz*, *Filetspitz*], *Roastbeef* [*Entrecote*, *Doppeltes Entrecote*], *Kotelett / Club / T-Bone / Porterhouse*, *Huft / Huftdeckel / Huftzapfen*); **C. *Lempen*** (*Flanc*); **D. *Rücken*** (*Train de côtes*); **E. *Schulter*** (*Épaule*); **F. *Federstück*** (*Côte plate*); **G. *Brust*** (*Poitrine*); **H. *Hals*** (*Cou*).
- **Tabela de Referência *Sous-Vide* & *Cook-and-Chill* (`Spezielle Gartechniken`):**
  - **Salmoura prévia:** **5% a 10%** (50–100 g sal/L de água) — **10 min** para peixes (evita saída de albumina) e **3 a 4 horas** para carnes grandes (aumenta retenção hídrica); banho ajustado **0,5 a 1 °C** acima do centro térmico desejado.
  - **Parâmetros validados:** Filé de Bacalhau (**55 °C / 2h30**); Filé de Salmão 25 mm (**57,5 °C / 125 min**); Lombo de Cordeiro 20 mm rosa (**60,5 °C / 30 min**); Peito de Ave 25 mm (**63,5 °C / 45 min**); Peito de Vitela (**62 °C / 15 h**, após 3 h de salmoura).

### 4.4 Massas Laminadas (*Teige*), Sobremesas (*Le Cordon Bleu* / *RB*) e Brigada (*Betriebsorganisation*)

- **Massas Folhadas Suíças:** *Deutscher Blätterteig* (1 kg: 420 g farinha T400, 300 g margarina de folhado, 65 g manteiga, 210 g água, 10 g sal, 2 g malte — **4 dobras duplas** com descansos de ≥ 30 min); *Halbblätterteig* (metade da gordura — **3 dobras duplas**); *Blitzblätterteig* (cubos de gordura de ~2 cm — **4 dobras duplas**). Nunca guardar crua por muito tempo a **5 °C** (acidifica e acinzenta por oxidação); preferir congelamento.
- **Caldas & Sobremesas Clássicas:** Bala mole (**116–118 °C** — *Parfaits* e merengue italiano), Bala dura (**125 °C** — *fondant*/marzipã), Casca mole (**134 °C** — *nougat*), Casca dura (**145 °C** — açúcar-cande/frutas cristalizadas); *Vanilleauflauf* (**200 °C / 15–20 min** após pré-aquecimento em banho-maria); *Gestürzte Karamellcreme* (banho-maria a **80 °C** coberta com filme plástico + meio dia de maturação fria).
- **Nutrição (*Ernaehrung*) & Brigada (*Betriebsorganisation*):** Amido de trigo, milho e batata (~80% amilopectina / ~20% amilose no manual técnico *Ernaehrung*); meta de **≥ 30 g/dia de fibras** (*Nahrungsfasern*) para sequestrar ácidos biliares e reduzir colesterol sérico; estrutura clássica de **13 postos** (*Küchenchef*, *Sous-chef*, *Saucier*, *Rôtisseur*, *Entremetier*, *Garde-manger*, *Pâtissier*, *Tournant*, *Chef de garde/Wache*, *Mitarbeiterkoch*, *Diätkoch*, *Lernender EFZ/EBA*, *Hilfskoch*) e organogramas de Hotel, Restaurante e Restauração Coletiva.

---

## 5. Checklist de Testes para a Construção no AI Studio

1. **Cadastro, Mural & RLS:** Testar criação de conta no Supabase, envio de foto comprimida (≤ 1200 px) na Semana 17 (*Geflügelgalantine* ou *Artischocken-Sous Vide*) e verificação do bloqueio de leitura para posts `private`.
2. **Novas Calculadoras e Alertas:** Validar cálculo de salmoura 5–10% (`50–100 g/L`), alerta de **175 °C** (acrilamida) em frituras, recomendação de frigideira antiaderente para vegetais (vitamina C) e seletor **4 vs. 10 porções** nas novas fichas suíças (*Brauner Kalbsfond*, *Berner Platte*, *Vanilleauflauf*).
3. **Tutor Multimodal (Chef IA):** Enviar uma foto de cortes para conferir se o Gemini valida as dimensões suíças (*Brunoise* 1,5 mm, *Jardinière* 5 mm, *Julienne* 1 mm × 3 cm) e uma foto da Ficha MEP preenchida.
