package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.firebase.signOutGoogle
import com.example.core.media.MediaCompressor
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.remote.PortfolioPost
import com.example.ui.components.AtelierFullLogoBanner
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel
import com.example.ui.viewmodel.ChefAiUiState
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.StreamUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityChefAiScreen(
    atelierVm: AtelierViewModel,
    communityVm: CommunityAiViewModel,
    defaultAuthorName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val pendingTransfer by atelierVm.pendingPhotoTransfer.collectAsStateWithLifecycle()
    val publicMuralState by communityVm.publicMuralState.collectAsStateWithLifecycle()
    val myPostsState by communityVm.myPostsState.collectAsStateWithLifecycle()
    val profileState by communityVm.profileState.collectAsStateWithLifecycle()
    val selectedMedia by communityVm.selectedImagePayload.collectAsStateWithLifecycle()
    val chefAiState by communityVm.chefAiState.collectAsStateWithLifecycle()
    val tutorChatState by communityVm.tutorChatState.collectAsStateWithLifecycle()
    val isPublishing by communityVm.isPublishing.collectAsStateWithLifecycle()
    val fullMediaCache by communityVm.loadedFullMediaCache.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var activeCommentsPost by remember { mutableStateOf<PortfolioPost?>(null) }
    var activeZoomPost by remember { mutableStateOf<PortfolioPost?>(null) }

    // Pre-filled Composer States (auto-populated when user taps "Registrar Foto do Treino" on MEP)
    var postWeek by rememberSaveable { mutableIntStateOf(1) }
    var postMaterialType by rememberSaveable { mutableStateOf("prato_final") }
    var postRecipeId by rememberSaveable { mutableStateOf(SwissmilkAndBaseRecipesCatalog.allRecipes.first().id) }
    var postCaption by rememberSaveable { mutableStateOf("") }
    var postMepSummary by rememberSaveable { mutableStateOf("") }
    var postIsPublic by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(pendingTransfer) {
        pendingTransfer?.let { transfer ->
            postWeek = transfer.weekNumber
            postRecipeId = transfer.recipeId
            postMepSummary = transfer.mepSummary
            subTab = 2 // Jump straight to Novo Registro + Chef IA Visual
            atelierVm.consumePhotoTransfer()
        }
    }

    if (activeCommentsPost != null || activeZoomPost != null) {
        BackHandler {
            activeCommentsPost = null
            activeZoomPost = null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            communityVm.compressSelectedPhoto(context, uri)
        }
    }

    val currentAuthorName = when (val p = profileState) {
        is StreamUiState.Success -> p.data?.displayName?.ifBlank { defaultAuthorName } ?: defaultAuthorName
        else -> defaultAuthorName
    }

    val tabs = listOf(
        "Mural do Atelier",
        "Meu Portfólio",
        "Registrar + Chef IA",
        "Tutor Chef IA",
        "Perfil & Conta"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = subTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("community_subtab_$idx")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (subTab) {
                0 -> PublicMuralFeedSection(
                    state = publicMuralState,
                    currentUserId = communityVm.currentUserId,
                    onReact = { communityVm.toggleReaction(it) },
                    onOpenComments = { activeCommentsPost = it },
                    onZoomPhoto = {
                        communityVm.loadFullResolutionPhoto(it.id)
                        activeZoomPost = it
                    },
                    onShareCard = { communityVm.exportShareablePngCard(context, it) }
                )
                1 -> MyPortfolioSection(
                    state = myPostsState,
                    onToggleVisibility = { communityVm.togglePostVisibility(it) },
                    onDelete = { communityVm.deleteOwnPost(it.id) },
                    onShareCard = { communityVm.exportShareablePngCard(context, it) },
                    onZoomPhoto = {
                        communityVm.loadFullResolutionPhoto(it.id)
                        activeZoomPost = it
                    }
                )
                2 -> ComposerAndVisualAiSection(
                    authorName = currentAuthorName,
                    weekNumber = postWeek,
                    onWeekChange = { postWeek = it },
                    materialType = postMaterialType,
                    onMaterialChange = { postMaterialType = it },
                    recipeId = postRecipeId,
                    onRecipeChange = { postRecipeId = it },
                    caption = postCaption,
                    onCaptionChange = { postCaption = it },
                    mepSummary = postMepSummary,
                    onMepChange = { postMepSummary = it },
                    isPublic = postIsPublic,
                    onPublicChange = { postIsPublic = it },
                    selectedMedia = selectedMedia,
                    chefAiState = chefAiState,
                    isPublishing = isPublishing,
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRunChefAudit = { title ->
                        communityVm.runMultimodalChefAudit(
                            materialType = postMaterialType,
                            recipeTitle = title,
                            weekNumber = postWeek,
                            studentNotes = "$postCaption | $postMepSummary"
                        )
                    },
                    onPublish = { rec, aiText ->
                        communityVm.publishPortfolioPost(
                            authorName = currentAuthorName,
                            visibility = if (postIsPublic) "public" else "private",
                            weekNumber = postWeek,
                            cycleNumber = ((postWeek - 1) / 6) + 1,
                            axisId = rec?.axisId ?: 1,
                            materialType = postMaterialType,
                            recipeId = rec?.id.orEmpty(),
                            recipeTitle = rec?.title ?: "Treino Livre Semana $postWeek",
                            isSwissmilkClassic = rec?.isSwissmilkClassic ?: false,
                            isChefkochV4 = rec?.isChefkochV4 ?: false,
                            cantonCode = rec?.cantonCode.orEmpty(),
                            caption = postCaption,
                            mepSummary = postMepSummary,
                            aiFeedback = aiText,
                            onPublished = {
                                postCaption = ""
                                subTab = if (postIsPublic) 0 else 1
                            }
                        )
                    }
                )
                3 -> ChefTutorChatSection(
                    tutorState = tutorChatState,
                    selectedWeek = postWeek,
                    onAsk = { q ->
                        communityVm.askChefTutor(q, "Semana $postWeek, Receita ativa ID=$postRecipeId")
                    }
                )
                4 -> StudentProfileAndSignOutSection(
                    profileState = profileState,
                    defaultName = defaultAuthorName,
                    onSave = { name, bio, w, canton, badge ->
                        communityVm.saveProfile(name, bio, w, canton, badge)
                    },
                    onSignOut = {
                        signOutGoogle(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }
    }

    // Comments Dialog
    activeCommentsPost?.let { post ->
        val comments by communityVm.observeCommentsForPost(post).collectAsStateWithLifecycle(initialValue = emptyList())
        var newComment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { activeCommentsPost = null },
            title = { Text("Comentários • ${post.recipeTitle}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (comments.isEmpty()) {
                        Text("Seja o primeiro a comentar com feedback construtivo.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        comments.forEach { c ->
                            Text("${c.authorName}: ${c.text}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Adicionar comentário técnico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newComment.isNotBlank()) {
                            communityVm.submitComment(post, currentAuthorName, newComment)
                            newComment = ""
                        }
                    }
                ) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { activeCommentsPost = null }) { Text("Fechar") }
            }
        )
    }

    // Full-Resolution 1200px Photo Modal
    activeZoomPost?.let { post ->
        val fullBmp = fullMediaCache[post.id] ?: remember(post.thumbnailBase64) {
            MediaCompressor.decodeBase64ToBitmap(post.thumbnailBase64)
        }
        AlertDialog(
            onDismissRequest = { activeZoomPost = null },
            title = { Text(post.recipeTitle, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fullBmp != null) {
                        Image(
                            bitmap = fullBmp.asImageBitmap(),
                            contentDescription = "Foto em alta resolução de ${post.recipeTitle}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (post.aiFeedback.isNotBlank()) {
                        Text(post.aiFeedback, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeZoomPost = null }) { Text("Fechar") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublicMuralFeedSection(
    state: StreamUiState<List<PortfolioPost>>,
    currentUserId: String,
    onReact: (PortfolioPost) -> Unit,
    onOpenComments: (PortfolioPost) -> Unit,
    onZoomPhoto: (PortfolioPost) -> Unit,
    onShareCard: (PortfolioPost) -> Unit
) {
    var filterCycle by rememberSaveable { mutableIntStateOf(0) } // 0 = All
    var filterMaterial by rememberSaveable { mutableStateOf("TODOS") }
    var onlySwissmilk by rememberSaveable { mutableStateOf(false) }
    var onlyChefkoch by rememberSaveable { mutableStateOf(false) }

    Text("FILTROS COMBINADOS DO MURAL COMUNITÁRIO", style = MaterialTheme.typography.labelLarge)
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..4).forEach { c ->
            FilterChip(
                selected = filterCycle == c,
                onClick = { filterCycle = c },
                label = { Text(if (c == 0) "Todos Ciclos" else "Ciclo $c") },
                modifier = Modifier.height(48.dp)
            )
        }
        FilterChip(
            selected = onlySwissmilk,
            onClick = { onlySwissmilk = !onlySwissmilk },
            label = { Text("Clássicos Swissmilk") },
            modifier = Modifier.height(48.dp)
        )
        FilterChip(
            selected = onlyChefkoch,
            onClick = { onlyChefkoch = !onlyChefkoch },
            label = { Text("Chefkoch Step-by-Step") },
            modifier = Modifier.height(48.dp)
        )
    }
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("TODOS", "prato_final", "cortes_mep", "ficha_preenchida", "resumo_esquema").forEach { m ->
            FilterChip(
                selected = filterMaterial == m,
                onClick = { filterMaterial = m },
                label = { Text(m) },
                modifier = Modifier.height(48.dp)
            )
        }
    }

    when (state) {
        is StreamUiState.Loading -> CircularProgressIndicator()
        is StreamUiState.Error -> Text(state.message, color = PccDanger)
        is StreamUiState.Success -> {
            val filtered = state.data.filter { post ->
                (filterCycle == 0 || post.cycleNumber == filterCycle) &&
                    (filterMaterial == "TODOS" || post.materialType == filterMaterial) &&
                    (!onlySwissmilk || post.isSwissmilkClassic) &&
                    (!onlyChefkoch || post.isChefkochV4)
            }
            if (filtered.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = InfoBlueBg), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Nenhum registro público para este filtro ainda. Publique seu treino na aba 'Registrar + Chef IA'!",
                        color = InfoBlue,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            filtered.forEach { post ->
                PortfolioPostCard(
                    post = post,
                    isLiked = currentUserId in post.reactedUserIds,
                    showOwnerControls = false,
                    onReact = { onReact(post) },
                    onComment = { onOpenComments(post) },
                    onShareCard = { onShareCard(post) },
                    onZoom = { onZoomPhoto(post) },
                    onToggleVisibility = {},
                    onDelete = {}
                )
            }
        }
    }
}

@Composable
private fun MyPortfolioSection(
    state: StreamUiState<List<PortfolioPost>>,
    onToggleVisibility: (PortfolioPost) -> Unit,
    onDelete: (PortfolioPost) -> Unit,
    onShareCard: (PortfolioPost) -> Unit,
    onZoomPhoto: (PortfolioPost) -> Unit
) {
    when (state) {
        is StreamUiState.Loading -> CircularProgressIndicator()
        is StreamUiState.Error -> Text(state.message, color = PccDanger)
        is StreamUiState.Success -> {
            if (state.data.isEmpty()) {
                Text("Seu portfólio individual ainda está vazio. Registre fotos de pratos, cortes MEP ou fichas preenchidas.")
            }
            state.data.forEach { post ->
                PortfolioPostCard(
                    post = post,
                    isLiked = false,
                    showOwnerControls = true,
                    onReact = {},
                    onComment = {},
                    onShareCard = { onShareCard(post) },
                    onZoom = { onZoomPhoto(post) },
                    onToggleVisibility = { onToggleVisibility(post) },
                    onDelete = { onDelete(post) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortfolioPostCard(
    post: PortfolioPost,
    isLiked: Boolean,
    showOwnerControls: Boolean,
    onReact: () -> Unit,
    onComment: () -> Unit,
    onShareCard: () -> Unit,
    onZoom: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBmp = remember(post.thumbnailBase64) {
        MediaCompressor.decodeBase64ToBitmap(post.thumbnailBase64)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TagPill("S${"%02d".format(post.weekNumber)} • ${post.materialType}", SwissRedContainer, OnSwissRedContainer)
                if (post.cantonCode.isNotBlank()) TagPill("CANTÃO ${post.cantonCode}", InfoBlueBg, InfoBlue)
                if (post.isChefkochV4) TagPill("CHEFKOCH V4", PccAmberBg, PccAmber)
                TagPill(if (post.visibility == "public") "PÚBLICO" else "PRIVADO", PccGreenBg, PccGreen)
            }
            Text(post.recipeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Brigada: ${post.authorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (thumbBmp != null) {
                Image(
                    bitmap = thumbBmp.asImageBitmap(),
                    contentDescription = "Foto de ${post.recipeTitle}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { onZoom() }
                )
            }

            if (post.caption.isNotBlank()) Text(post.caption, style = MaterialTheme.typography.bodyMedium)
            if (post.mepSummary.isNotBlank()) {
                Text(post.mepSummary, style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMonoFamily)
            }
            if (post.aiFeedback.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = PccGreenBg)) {
                    Text(post.aiFeedback, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!showOwnerControls) {
                    OutlinedButton(onClick = onReact, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = "Aplaudir", tint = if (isLiked) SwissRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("${post.reactionCount}")
                    }
                    OutlinedButton(onClick = onComment, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comentários")
                        Spacer(Modifier.width(4.dp))
                        Text("${post.commentCount}")
                    }
                } else {
                    OutlinedButton(onClick = onToggleVisibility, modifier = Modifier.height(48.dp)) {
                        Icon(
                            if (post.visibility == "public") Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = "Alternar visibilidade"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (post.visibility == "public") "Tornar Privado" else "Publicar no Mural")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir post", tint = PccDanger)
                    }
                }
                OutlinedButton(onClick = onShareCard, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_shareable_card))
                    Spacer(Modifier.width(4.dp))
                    Text("Card PNG")
                }
            }
        }
    }
}

@Composable
private fun ComposerAndVisualAiSection(
    authorName: String,
    weekNumber: Int,
    onWeekChange: (Int) -> Unit,
    materialType: String,
    onMaterialChange: (String) -> Unit,
    recipeId: String,
    onRecipeChange: (String) -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    mepSummary: String,
    onMepChange: (String) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    selectedMedia: com.example.core.media.CompressedMediaPayload?,
    chefAiState: ChefAiUiState,
    isPublishing: Boolean,
    onPickPhoto: () -> Unit,
    onRunChefAudit: (String) -> Unit,
    onPublish: (com.example.domain.model.RecipeSpec?, String) -> Unit
) {
    val activeRecipe = SwissmilkAndBaseRecipesCatalog.allRecipes.find { it.id == recipeId }
    val aiFeedbackText = (chefAiState as? ChefAiUiState.AuditReady)?.formattedText.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("UPLOAD OTIMIZADO (≤1200px JPEG 0.82 SEM EXIF) + CHEF IA", SwissRedContainer, OnSwissRedContainer)

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "prato_final" to "Prato Final",
                    "cortes_mep" to "Cortes & MEP",
                    "ficha_preenchida" to "Ficha MEP",
                    "resumo_esquema" to "Esquema 25% Visual"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = materialType == id,
                        onClick = { onMaterialChange(id) },
                        label = { Text(label) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SwissmilkAndBaseRecipesCatalog.allRecipes.take(16).forEach { r ->
                    FilterChip(
                        selected = recipeId == r.id,
                        onClick = {
                            onRecipeChange(r.id)
                            onWeekChange(r.weekNumber)
                        },
                        label = { Text("S${"%02d".format(r.weekNumber)} ${r.title.take(18)}") },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onPickPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("pick_culinary_photo_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedMedia == null) "Selecionar Foto (Photo Picker Seguro)" else "Trocar Foto (${selectedMedia.width}×${selectedMedia.height}px)")
            }

            selectedMedia?.let { media ->
                Image(
                    bitmap = media.fullBitmap.asImageBitmap(),
                    contentDescription = "Pré-visualização da foto culinária sem EXIF",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )
                Button(
                    onClick = { onRunChefAudit(activeRecipe?.title ?: "Treino Semana $weekNumber") },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("run_visual_chef_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Avaliar Foto com Chef Instrutor IA (Gemini Multimodal)", fontWeight = FontWeight.Bold)
                }
            }

            when (chefAiState) {
                is ChefAiUiState.Loading -> CircularProgressIndicator()
                is ChefAiUiState.Error -> Text(chefAiState.message, color = PccDanger)
                is ChefAiUiState.AuditReady -> {
                    Card(colors = CardDefaults.cardColors(containerColor = PccGreenBg)) {
                        Text(chefAiState.formattedText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {}
            }

            OutlinedTextField(caption, onCaptionChange, label = { Text("Observações do treino / autoavaliação") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(mepSummary, onMepChange, label = { Text("Resumo Ficha MEP / PCC") }, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = isPublic, onCheckedChange = onPublicChange)
                Text(if (isPublic) "Visibilidade: Público no Mural do Atelier" else "Visibilidade: Privado (apenas Meu Portfólio)")
            }

            Button(
                onClick = { onPublish(activeRecipe, aiFeedbackText) },
                enabled = !isPublishing,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("publish_portfolio_post_button")
            ) {
                Text(if (isPublishing) "Sincronizando no Firestore..." else "Salvar Registro ($authorName)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChefTutorChatSection(
    tutorState: ChefAiUiState,
    selectedWeek: Int,
    onAsk: (String) -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("CHEF INSTRUTOR IA • DIAGNÓSTICO DE BANCADA & SOCRÁTICO", InfoBlueBg, InfoBlue)
            Text(
                "Consulte substituições suíças↔brasileiras, correção de emulsões talhadas, pontos de calda ou regras de acrilamida/Sous-vide.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Pergunte ao Chef Instrutor (Semana $selectedWeek)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chef_tutor_input")
            )
            Button(
                onClick = { onAsk(question) },
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("ask_chef_tutor_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ask_chef_ai), fontWeight = FontWeight.Bold)
            }

            when (tutorState) {
                is ChefAiUiState.Loading -> CircularProgressIndicator()
                is ChefAiUiState.Streaming -> Text(tutorState.partialText, style = MaterialTheme.typography.bodyMedium)
                is ChefAiUiState.Success -> Text(tutorState.text, style = MaterialTheme.typography.bodyMedium)
                is ChefAiUiState.Error -> Text(tutorState.message, color = PccDanger)
                else -> {}
            }
        }
    }
}

@Composable
private fun StudentProfileAndSignOutSection(
    profileState: StreamUiState<com.example.data.remote.UserProfile?>,
    defaultName: String,
    onSave: (String, String, Int, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val existing = (profileState as? StreamUiState.Success)?.data
    var name by remember(existing) { mutableStateOf(existing?.displayName ?: defaultName) }
    var bio by remember(existing) { mutableStateOf(existing?.bio ?: "Estudante autoguiado da culinária profissional suíça.") }
    var canton by remember(existing) { mutableStateOf(existing?.cantonFocus ?: "ZH") }
    var badge by remember(existing) { mutableStateOf(existing?.specialtyBadge ?: "Lernender EFZ") }
    val week = existing?.currentWeek ?: 1

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtelierFullLogoBanner(maxHeight = 88.dp)
            Text("CADASTRO DO ALUNO • PERFIL NO MURAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, label = { Text("Nome / Assinatura na Brigada") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(canton, { canton = it }, label = { Text("Cantão Suíço Favorito (ex.: ZH, BE, GR, TI, VD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(badge, { badge = it }, label = { Text("Insígnia de Posto (ex.: Lernender EFZ, Saucier)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bio, { bio = it }, label = { Text("Bio Gastronômica") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { onSave(name, bio, week, canton, badge) },
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Salvar Perfil na Nuvem", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}
