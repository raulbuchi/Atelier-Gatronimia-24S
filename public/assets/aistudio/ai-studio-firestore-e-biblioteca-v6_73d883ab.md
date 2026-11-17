# Guia Rápido v6 — Biblioteca Didática Nativa no App + Adaptação para Firestore (Google AI Studio)

Este complemento resolve dois pontos práticos da sua montagem no **Google AI Studio**:
1. **Substituir os PDFs para download por um leitor nativo completo dentro do app**, usando o novo arquivo **`brochuras-didaticas-app.json`** (todos os 6 Eixos, 23 seções, tabelas, faixas numéricas, guias da Ficha MEP, 60 flashcards e bibliografia completa).
2. **Garantir que o banco criado no Cloud Firestore pelo AI Studio funcione exatamente como planejado** para o cadastro simples, a privacidade (`private` × `public`) e o mural de fotos.

---

## 1. Como integrar as Brochuras Nativas (`brochuras-didaticas-app.json`) no AI Studio

1. Suba o arquivo **`brochuras-didaticas-app.json`** para o projeto no **Google AI Studio Build** (na mesma pasta de `curso-24-semanas-dados-v4.json`).
2. Cole este prompt direto no chat do **AI Studio Build**:

```text
Importe o arquivo `brochuras-didaticas-app.json` e transforme a aba "Material de Apoio" (na Tela 2 e acessível pelos atalhos da Semana Atual na Tela 1) em uma BIBLIOTECA DIDÁTICA INTERATIVA 100% nativa dentro do aplicativo (remova links de download de PDF para as brochuras).

REQUISITOS DO LEITOR DIDÁTICO:
1. Navegação em 2 painéis (desktop) ou seletor superior + leitura limpa (mobile):
   - Seletor dos 6 Eixos (`axes_brochures`) mostrando título, subtítulo, tempo estimado de leitura e badges.
   - Índice de seções do eixo selecionado, com filtro rápido "Apenas seções da Semana Atual" (comparando `section.weeks` com a semana ativa).
   - Barra de busca global em tempo real que procura em títulos, textos, tabelas, `search_keywords` e flashcards dos 6 eixos simultaneamente.
2. Renderizador nativo dos blocos (`section.blocks`):
   - `paragraph`: texto editorial confortável (suportando **negrito** e *itálico*), largura máxima legível.
   - `bullets`: lista com destaque nos termos-chave.
   - `table`: tabela responsiva com cabeçalho fixo/estilizado, rolagem horizontal suave no mobile e legenda (`caption`).
   - `callout`: caixa destacada por `variant`:
     * `critical` → borda vermelha suíça (limites térmicos, acrilamida, OR/direito);
     * `mep` → borda azul-ardósia (como preencher os 8 blocos da Ficha MEP);
     * `troubleshooting` → borda âmbar (resgate de emulsões/erros);
     * `tip` → borda verde-sálvia (fichas passo a passo como Brauner Kalbsfond e Rösti).
   - `metric_strip`: grade de cards numéricos compactos (ex.: temperaturas Sous-vide, estágios do açúcar, equivalências de fibras).
3. Rodapé de cada Eixo:
   - Acordeão/abas com os **Flashcards de Revisão** do eixo (com botão "Praticar estes cards no SM-2") e a **Bibliografia Integrada**.
4. Mantenha apenas a Ficha MEP (`ficha-mep-modelo.pdf` / versão imprimível) com opção de impressão caso o aluno queira levar à bancada física.
```

---

## 2. Como garantir que o Firestore funcione para Cadastro + Mural de Fotos

Como o Google AI Studio criou a integração diretamente com o **Cloud Firestore**, o sistema funciona perfeitamente para cadastro, portfólio pessoal e mural comunitário, desde que você valide **4 ajustes essenciais** no projeto Firebase:

### A. Desnormalização no documento do Post (evita multiplicar leituras e custos)
Na edição Standard do Firestore não há `JOIN` SQL nativo, e o faturamento no plano Blaze ocorre por leitura, gravação e exclusão de documentos. Por isso, peça ao AI Studio para salvar **nome e avatar do autor já dentro de cada documento em `portfolio_posts`**, além da coleção `profiles`:

- **Coleção `profiles/{userId}`:** `display_name`, `avatar_url`, `city_region`, `current_level`, `default_visibility`, `bio`, `updated_at`.
- **Coleção `portfolio_posts/{postId}`:**
  - `user_id` (UID do Firebase Auth)
  - `author_display_name` e `author_avatar_url` *(cópia rápida para o feed não precisar fazer 1 leitura extra em `profiles` para cada foto exibida)*
  - `title`, `caption`, `image_url`, `image_path`
  - `material_type` (`prato_final` | `cortes_mep` | `ficha_preenchida` | `resumo_esquema`)
  - `visibility` (`private` | `public`)
  - `cycle_number`, `week_number`, `axis_id`, `recipe_id`, `recipe_name`
  - `is_swissmilk_classic`, `swiss_canton`, `selbstkontrolle_scores`, `erkenntnisse`, `offene_punkte`, `ai_feedback`
  - `likes_count` (contador numérico) e `created_at`
- **Subcoleções em `portfolio_posts/{postId}`:**
  - `reactions/{userId}` (1 documento por usuário que curtiu)
  - `comments/{commentId}` (`user_id`, `author_display_name`, `body`, `created_at`)
  *(Lembrete importante do Firestore: excluir um documento pai em `portfolio_posts` não apaga automaticamente suas subcoleções `reactions` e `comments` — o código de exclusão deve remover os filhos ou rodar em lote.)*

### B. Atenção ao Storage de Fotos (Spark vs. Blaze)
Desde **3 de fevereiro de 2026**, o **Cloud Storage for Firebase** exige que o projeto esteja no plano **Blaze** (*pay-as-you-go*, que mantém a mesma franquia gratuita mensal antes de cobrar o excedente).
- **Se você ativar o plano Blaze no Firebase:** use o **Cloud Storage** normalmente com as fotos já comprimidas no navegador (`1200 px`, JPEG `0.82` via `mediaUpload.ts`) para manter o consumo baixíssimo, e configure alertas de orçamento no Google Cloud Console *(lembrando que os alertas enviam notificações, sem pausa automática no Firestore)*.
- **Se quiser testar no plano gratuito Spark sem cartão por enquanto:** peça ao AI Studio para comprimir a foto no navegador em largura menor (ex.: `800 px`, JPEG `0.70`, resultando em ~80–150 KB em string Base64/DataURL, bem abaixo do limite de 1 MB por documento do Firestore) e salvá-la temporariamente no próprio campo `image_url` do documento até migrar para o Storage.

### C. Regras de Segurança (`firestore.rules`) — Nunca deixe em modo de teste aberto
No console do Firebase (**Firestore Database → Rules**), substitua qualquer regra temporária (`allow read, write: if true`) pelas regras equivalentes ao RLS do Supabase:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Perfis: leitura por usuários logados; escrita apenas pelo próprio dono
    match /profiles/{userId} {
      allow read: if request.auth != null;
      allow create, update, delete: if request.auth != null && request.auth.uid == userId;
    }

    // Publicações do Portfólio e Mural
    match /portfolio_posts/{postId} {
      allow read: if request.auth != null && (
        resource.data.visibility == 'public' ||
        resource.data.user_id == request.auth.uid
      );

      allow create: if request.auth != null
        && request.resource.data.user_id == request.auth.uid
        && request.resource.data.visibility in ['private', 'public']
        && request.resource.data.material_type in [
          'prato_final', 'cortes_mep', 'ficha_preenchida', 'resumo_esquema'
        ];

      allow update: if request.auth != null && (
        // O autor pode editar tudo mantendo seu user_id
        (resource.data.user_id == request.auth.uid
          && request.resource.data.user_id == resource.data.user_id)
        // Ou outro usuário logado atualiza apenas o contador de curtidas em post público
        || (resource.data.visibility == 'public'
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['likes_count']))
      );

      allow delete: if request.auth != null && resource.data.user_id == request.auth.uid;

      // Subcoleção de curtidas
      match /reactions/{reactorId} {
        allow read: if request.auth != null;
        allow create, delete: if request.auth != null && request.auth.uid == reactorId;
      }

      // Subcoleção de comentários
      match /comments/{commentId} {
        allow read: if request.auth != null;
        allow create: if request.auth != null
          && request.resource.data.user_id == request.auth.uid
          && request.resource.data.body is string
          && request.resource.data.body.size() >= 1
          && request.resource.data.body.size() <= 800;
        allow delete: if request.auth != null && resource.data.user_id == request.auth.uid;
      }
    }
  }
}
```

### D. Índices Compostos para os Filtros do Mural
No Firestore, combinar `where('visibility', '==', 'public')` + outro filtro (`week_number`, `axis_id`, `material_type` ou `is_swissmilk_classic`) + `orderBy('created_at', 'desc')` exige **índices compostos**.
- Ao testar o app no Preview, se algum filtro der erro no console do navegador com um **link direto do Firebase para criar o índice**, basta clicar nesse link para criar o índice automático em ~1 minuto — ou pedir ao AI Studio para filtrar por `visibility == 'public'` ordenado por `created_at` e aplicar os subfiltros secundários em memória quando o mural ainda tiver poucas centenas de posts.
