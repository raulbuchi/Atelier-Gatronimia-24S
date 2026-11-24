package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.media.CompressedAvatarPayload
import com.example.core.media.CompressedMediaPayload
import com.example.core.media.MediaCompressor
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.remote.PortfolioPost
import com.example.data.remote.UserProfile
import com.example.domain.model.RecipeSpec
import com.example.ui.components.AtelierFullLogoBanner
import com.example.ui.components.CameraXCaptureDialog
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.AtelierViewModel
import com.example.ui.viewmodel.ChefAiUiState
import com.example.ui.viewmodel.CommunityAiViewModel
import com.example.ui.viewmodel.StreamUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChefAiScreen(
    atelierVm: AtelierViewModel,
    communityVm: CommunityAiViewModel,
    defaultAuthorName: String
) {
    val context = LocalContext.current

    val pendingTransfer by atelierVm.pendingPhotoTransfer.collectAsStateWithLifecycle()
    val pendingSos by atelierVm.pendingChefSos.collectAsStateWithLifecycle()
    val selectedWeekGlobal by atelierVm.selectedWeekNumber.collectAsStateWithLifecycle()
    val publicMuralState by communityVm.publicMuralState.collectAsStateWithLifecycle()
    val myPostsState by communityVm.myPostsState.collectAsStateWithLifecycle()
    val profileState by communityVm.profileState.collectAsStateWithLifecycle()
    val selectedMedia by communityVm.selectedImagePayload.collectAsStateWithLifecycle()
    val pendingAvatar by communityVm.pendingAvatarPayload.collectAsStateWithLifecycle()
    val chefAiState by communityVm.chefAiState.collectAsStateWithLifecycle()
    val tutorChatState by communityVm.tutorChatState.collectAsStateWithLifecycle()
    val isPublishing by communityVm.isPublishing.collectAsStateWithLifecycle()
    val fullMediaCache by communityVm.loadedFullMediaCache.collectAsStateWithLifecycle()
    val mepSheetsMap by atelierVm.mepSheetsMap.collectAsStateWithLifecycle()

    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var activeCommentsPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeZoomPost by remember { mutableStateOf<PortfolioPost?>(null) }
    var showQuickMuralPublisher by rememberSaveable { mutableStateOf(false) }
    var showComposerCameraX by remember { mutableStateOf(false) }

    // Pre-filled Composer States (auto-populated when user taps "Registrar Foto da Produção" on MEP)
    var postWeek by rememberSaveable { mutableIntStateOf(selectedWeekGlobal) }
    var postMaterialType by rememberSaveable { mutableStateOf("prato_final") }
    var postRecipeId by rememberSaveable { mutableStateOf(SwissmilkAndBaseRecipesCatalog.allRecipes.first().id) }
    var postCaption by rememberSaveable { mutableStateOf("") }
    var postMepSummary by rememberSaveable { mutableStateOf("") }
    var postSelbstkontrolleScore by rememberSaveable { mutableIntStateOf(0) }
    var postErkenntnisse by rememberSaveable { mutableStateOf("") }
    var postOffenePunkte by rememberSaveable { mutableStateOf("") }
    var postIsPublic by rememberSaveable { mutableStateOf(true) }
    var initializedVisibilityFromProfile by rememberSaveable { mutableStateOf(false) }

    // Pre-filled Chef SOS States
    var sosContextSummary by rememberSaveable { mutableStateOf("") }
    var sosPrefillPrompt by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(profileState) {
        val loadedProfile = (profileState as? StreamUiState.Success)?.data
        if (loadedProfile != null && !initializedVisibilityFromProfile) {
            postIsPublic = loadedProfile.defaultVisibility != "private"
            initializedVisibilityFromProfile = true
        }
    }

    LaunchedEffect(pendingTransfer) {
        pendingTransfer?.let { transfer ->
            postWeek = transfer.weekNumber
            postRecipeId = transfer.recipeId
            postMepSummary = transfer.mepSummary
            if (transfer.executionScore1To5 in 1..5) {
                postSelbstkontrolleScore = transfer.executionScore1To5
            }
            if (transfer.block8Selbstkontrolle.isNotBlank()) {
                postErkenntnisse = transfer.block8Selbstkontrolle
            }
            if (transfer.localPhotoUri.isNotBlank() || transfer.cloudMediaId.isNotBlank()) {
                communityVm.preloadTransferredMepPhoto(
                    context = context,
                    localPhotoUri = transfer.localPhotoUri,
                    cloudMediaId = transfer.cloudMediaId,
                    thumbnailBase64 = transfer.photoThumbnailBase64
                )
            }
            subTab = 2 // Jump straight to Novo Registro + Chef IA Visual
            atelierVm.consumePhotoTransfer()
        }
    }

    LaunchedEffect(pendingSos) {
        pendingSos?.let { sos ->
            postWeek = sos.weekNumber
            sosContextSummary = sos.contextSummary
            sosPrefillPrompt = sos.prefilledPrompt
            subTab = 3 // Jump straight to Tutor Chef IA
            atelierVm.consumeChefSos()
        }
    }

    // Resolve live PortfolioPost for active comments sheet so commentCount updates reactively
    val activeCommentsPost: PortfolioPost? = remember(activeCommentsPostId, publicMuralState, myPostsState) {
        val targetId = activeCommentsPostId ?: return@remember null
        val publicMatch = (publicMuralState as? StreamUiState.Success)?.data?.find { it.id == targetId }
        val ownMatch = (myPostsState as? StreamUiState.Success)?.data?.find { it.id == targetId }
        publicMatch ?: ownMatch
    }

    if (showComposerCameraX) {
        val recTitle = SwissmilkAndBaseRecipesCatalog.findById(postRecipeId)?.title
            ?: mepSheetsMap[postRecipeId]?.title
            ?: "Criação Semana $postWeek"
        CameraXCaptureDialog(
            recipeId = postRecipeId,
            sheetTitle = recTitle,
            onPhotoCaptured = { capturedUri ->
                showComposerCameraX = false
                val targetSheet = mepSheetsMap[postRecipeId]
                if (targetSheet != null && !showQuickMuralPublisher) {
                    val lines = atelierVm.resolveSeedLinesForSheet(postRecipeId)
                    communityVm.captureAndLinkPhotoToMepSheet(
                        context = context,
                        uri = capturedUri,
                        sheet = targetSheet,
                        onLinked = { localUriStr, cloudLink ->
                            atelierVm.attachCreationPhotoToMepSheet(
                                sheet = targetSheet,
                                lines = lines,
                                localPhotoUri = localUriStr,
                                cloudLink = cloudLink
                            )
                        }
                    )
                } else {
                    communityVm.compressSelectedPhoto(context, capturedUri)
                }
            },
            onDismiss = { showComposerCameraX = false }
        )
    }

    if (activeCommentsPost != null || activeZoomPost != null || showQuickMuralPublisher) {
        BackHandler {
            when {
                showComposerCameraX -> showComposerCameraX = false
                activeCommentsPost != null -> activeCommentsPostId = null
                activeZoomPost != null -> activeZoomPost = null
                showQuickMuralPublisher -> showQuickMuralPublisher = false
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            communityVm.compressSelectedPhoto(context, uri)
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            communityVm.compressSelectedAvatar(context, uri)
        }
    }

    val currentAuthorName = when (val p = profileState) {
        is StreamUiState.Success -> p.data?.displayName?.ifBlank { defaultAuthorName } ?: defaultAuthorName
        else -> defaultAuthorName
    }

    val tabs = listOf(
        "Mural Comunitário",
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
            if (!communityVm.isAuthenticated && subTab in setOf(0, 1, 2, 4)) {
                TestModeCloudBanner()
            }

            when (subTab) {
                0 -> MuralComunitarioSection(
                    state = publicMuralState,
                    currentUserId = communityVm.currentUserId,
                    onOpenQuickPublish = { showQuickMuralPublisher = true },
                    onReact = { communityVm.toggleReaction(it) },
                    onOpenComments = { activeCommentsPostId = it.id },
                    onZoomPhoto = {
                        communityVm.loadFullResolutionPhoto(it.id)
                        activeZoomPost = it
                    },
                    onShareCard = { communityVm.exportShareablePngCard(context, it) },
                    onToggleVisibility = { communityVm.togglePostVisibility(it) },
                    onDeleteOwnPost = { communityVm.deleteOwnPost(it.id) }
                )
                1 -> MyPortfolioSection(
                    state = myPostsState,
                    currentUserId = communityVm.currentUserId,
                    onReact = { communityVm.toggleReaction(it) },
                    onOpenComments = { activeCommentsPostId = it.id },
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
                    isAuthenticated = communityVm.isAuthenticated,
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
                    selbstkontrolleScore = postSelbstkontrolleScore,
                    onScoreChange = { postSelbstkontrolleScore = it },
                    erkenntnisse = postErkenntnisse,
                    onErkenntnisseChange = { postErkenntnisse = it },
                    offenePunkte = postOffenePunkte,
                    onOffenePunkteChange = { postOffenePunkte = it },
                    isPublic = postIsPublic,
                    onPublicChange = { postIsPublic = it },
                    selectedMedia = selectedMedia,
                    chefAiState = chefAiState,
                    isPublishing = isPublishing,
                    onOpenCameraX = { showComposerCameraX = true },
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
                            recipeTitle = rec?.title ?: "Produção Livre — Semana $postWeek",
                            isSwissmilkClassic = rec?.isSwissmilkClassic ?: false,
                            isChefkochV4 = rec?.isChefkochV4 ?: false,
                            cantonCode = rec?.cantonCode.orEmpty(),
                            caption = postCaption,
                            mepSummary = postMepSummary,
                            selbstkontrolleScore = postSelbstkontrolleScore,
                            erkenntnisse = postErkenntnisse,
                            offenePunkte = postOffenePunkte,
                            aiFeedback = aiText,
                            onPublished = {
                                postCaption = ""
                                postErkenntnisse = ""
                                postOffenePunkte = ""
                                postSelbstkontrolleScore = 0
                                subTab = if (postIsPublic) 0 else 1
                            }
                        )
                    }
                )
                3 -> ChefTutorChatSection(
                    tutorState = tutorChatState,
                    selectedWeek = postWeek,
                    prefilledQuestion = sosPrefillPrompt,
                    activeContextSummary = sosContextSummary,
                    onAsk = { q ->
                        val ctx = sosContextSummary.ifBlank { "Semana $postWeek, Receita ativa ID=$postRecipeId" }
                        communityVm.askChefTutor(q, ctx)
                    }
                )
                4 -> StudentProfileSection(
                    profileState = profileState,
                    pendingAvatar = pendingAvatar,
                    defaultName = defaultAuthorName,
                    isAuthenticated = communityVm.isAuthenticated,
                    onPickAvatar = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSave = { name, bio, defVis, w, canton, badge ->
                        communityVm.saveProfile(name, bio, defVis, w, canton, badge)
                    }
                )
            }
        }
    }

    if (showQuickMuralPublisher && !showComposerCameraX) {
        MuralQuickPublishDialog(
            authorName = currentAuthorName,
            initialWeekNumber = selectedWeekGlobal,
            selectedMedia = selectedMedia,
            chefAiState = chefAiState,
            isPublishing = isPublishing,
            onOpenCameraX = { chosenRecipeId, chosenWeek ->
                postRecipeId = chosenRecipeId
                postWeek = chosenWeek
                showComposerCameraX = true
            },
            onPickGalleryPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRunChefAudit = { matType, recTitle, wk, notes ->
                communityVm.runMultimodalChefAudit(
                    materialType = matType,
                    recipeTitle = recTitle,
                    weekNumber = wk,
                    studentNotes = notes
                )
            },
            onSubmitPublicPost = { wk, matType, rec, cap, mep, aiText ->
                communityVm.publishPortfolioPost(
                    authorName = currentAuthorName,
                    visibility = "public",
                    weekNumber = wk,
                    cycleNumber = ((wk - 1) / 6) + 1,
                    axisId = rec?.axisId ?: 1,
                    materialType = matType,
                    recipeId = rec?.id ?: "livre",
                    recipeTitle = rec?.title ?: "Preparação Autoral — Semana $wk",
                    isSwissmilkClassic = rec?.isSwissmilkClassic ?: false,
                    isChefkochV4 = rec?.isChefkochV4 ?: false,
                    cantonCode = rec?.cantonCode.orEmpty(),
                    caption = cap,
                    mepSummary = mep,
                    aiFeedback = aiText,
                    onPublished = {
                        showQuickMuralPublisher = false
                        subTab = 0
                    }
                )
            },
            onDismiss = { showQuickMuralPublisher = false }
        )
    }

    activeCommentsPost?.let { post ->
        val comments by communityVm.observeCommentsForPost(post).collectAsStateWithLifecycle(initialValue = emptyList())
        MuralCommentsDialog(
            post = post,
            comments = comments,
            currentUserId = communityVm.currentUserId,
            currentAuthorName = currentAuthorName,
            onSubmitComment = { text ->
                communityVm.submitComment(post, currentAuthorName, text)
            },
            onDeleteComment = { comment ->
                communityVm.deleteComment(post, comment)
            },
            onDismiss = { activeCommentsPostId = null }
        )
    }

    activeZoomPost?.let { post ->
        MuralPhotoZoomDialog(
            post = post,
            fullBitmap = fullMediaCache[post.id],
            onDismiss = { activeZoomPost = null }
        )
    }
}

@Composable
private fun MyPortfolioSection(
    state: StreamUiState<List<PortfolioPost>>,
    currentUserId: String,
    onReact: (PortfolioPost) -> Unit,
    onOpenComments: (PortfolioPost) -> Unit,
    onToggleVisibility: (PortfolioPost) -> Unit,
    onDelete: (PortfolioPost) -> Unit,
    onShareCard: (PortfolioPost) -> Unit,
    onZoomPhoto: (PortfolioPost) -> Unit
) {
    when (state) {
        is StreamUiState.Loading -> CircularProgressIndicator(color = SwissRed)
        is StreamUiState.Error -> Text(state.message, color = PccDanger)
        is StreamUiState.Success -> {
            if (state.data.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = InfoBlueBg), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Seu portfólio individual ainda está vazio. Registre fotos de pratos finalizados, cortes de mise en place ou fichas técnicas preenchidas.",
                        color = InfoBlue,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            state.data.forEach { post ->
                MuralPostCard(
                    post = post,
                    isLiked = currentUserId in post.reactedUserIds,
                    showOwnerControls = true,
                    onReact = { onReact(post) },
                    onComment = { onOpenComments(post) },
                    onShareCard = { onShareCard(post) },
                    onZoom = { onZoomPhoto(post) },
                    onToggleVisibility = { onToggleVisibility(post) },
                    onDelete = { onDelete(post) }
                )
            }
        }
    }
}

@Composable
private fun TestModeCloudBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PccAmberBg),
        border = BorderStroke(1.dp, PccAmber),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("test_mode_cloud_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TagPill(stringResource(R.string.test_mode_badge) + " • FIRESTORE PAUSADO", SwissRedContainer, OnSwissRedContainer)
            Text(
                text = stringResource(R.string.test_mode_cloud_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.test_mode_cloud_desc),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ComposerAndVisualAiSection(
    authorName: String,
    isAuthenticated: Boolean,
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
    selbstkontrolleScore: Int,
    onScoreChange: (Int) -> Unit,
    erkenntnisse: String,
    onErkenntnisseChange: (String) -> Unit,
    offenePunkte: String,
    onOffenePunkteChange: (String) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    selectedMedia: CompressedMediaPayload?,
    chefAiState: ChefAiUiState,
    isPublishing: Boolean,
    onOpenCameraX: () -> Unit,
    onPickPhoto: () -> Unit,
    onRunChefAudit: (String) -> Unit,
    onPublish: (RecipeSpec?, String) -> Unit
) {
    var recipeWeekFilter by rememberSaveable { mutableIntStateOf(0) }
    val visibleRecipes = remember(recipeWeekFilter) {
        if (recipeWeekFilter == 0) SwissmilkAndBaseRecipesCatalog.allRecipes
        else SwissmilkAndBaseRecipesCatalog.recipesForWeek(recipeWeekFilter)
    }
    val activeRecipe = SwissmilkAndBaseRecipesCatalog.findById(recipeId)
    val aiFeedbackText = (chefAiState as? ChefAiUiState.AuditReady)?.formattedText.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("IMAGEM OTIMIZADA (≤ 1200 px FUNDO BRANCO SEM EXIF) + CHEF IA", SwissRedContainer, OnSwissRedContainer)

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("prato_final", "cortes_mep", "ficha_preenchida", "resumo_esquema").forEach { id ->
                    FilterChip(
                        selected = materialType == id,
                        onClick = { onMaterialChange(id) },
                        label = { Text(formatMuralMaterialTypePtBr(id)) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf(0) + (1..24).toList()).forEach { wk ->
                    FilterChip(
                        selected = recipeWeekFilter == wk,
                        onClick = { recipeWeekFilter = wk },
                        label = { Text(if (wk == 0) "Todas (45)" else "S${"%02d".format(wk)}", fontFamily = JetBrainsMonoFamily) },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleRecipes.forEach { r ->
                    FilterChip(
                        selected = recipeId == r.id,
                        onClick = {
                            onRecipeChange(r.id)
                            onWeekChange(r.weekNumber)
                        },
                        label = { Text("S${"%02d".format(r.weekNumber)} ${r.title.take(22)}") },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpenCameraX,
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp)
                        .testTag("camerax_composer_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_camerax_mep), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("pick_culinary_photo_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (selectedMedia == null) "Galeria" else "Trocar (${selectedMedia.width}px)")
                }
            }

            selectedMedia?.let { media ->
                Image(
                    bitmap = media.fullBitmap.asImageBitmap(),
                    contentDescription = "Pré-visualização da foto da produção sem metadados",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                if (media.compressedSizeBytes > 0) {
                    val fmt = MediaCompressor.formatBytes(media.compressedSizeBytes.toLong())
                    val sav = if (media.savingsPercent > 0) " • −${media.savingsPercent}% vs. original" else ""
                    TagPill("${media.width}×${media.height} px • JPEG $fmt$sav (EXIF removido)", InfoBlueBg, InfoBlue)
                }
                Button(
                    onClick = { onRunChefAudit(activeRecipe?.title ?: "Produção — Semana $weekNumber") },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("run_visual_chef_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Avaliar Produção com Chef Instrutor IA", fontWeight = FontWeight.Bold)
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

            OutlinedTextField(caption, onCaptionChange, label = { Text("Observações de bancada / apresentação") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(mepSummary, onMepChange, label = { Text("Resumo da Ficha Técnica / PCC") }, modifier = Modifier.fillMaxWidth())

            Text("Autoavaliação de Bancada (Selbstkontrolle 1–5 opcional):", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..5).forEach { score ->
                    FilterChip(
                        selected = selbstkontrolleScore == score,
                        onClick = { onScoreChange(score) },
                        label = { Text(if (score == 0) "Sem nota" else "Nota $score/5", fontFamily = JetBrainsMonoFamily) },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            OutlinedTextField(
                value = erkenntnisse,
                onValueChange = onErkenntnisseChange,
                label = { Text("Aprendizados consolidados (Erkenntnisse — opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = offenePunkte,
                onValueChange = onOffenePunkteChange,
                label = { Text("Pontos abertos p/ próximo treino (Offene Punkte — opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = isPublic, onCheckedChange = onPublicChange)
                Text(if (isPublic) "Visibilidade: Público no Mural Comunitário" else "Visibilidade: Privado (apenas Meu Portfólio)")
            }

            if (isAuthenticated) {
                Button(
                    onClick = { onPublish(activeRecipe, aiFeedbackText) },
                    enabled = !isPublishing,
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("publish_portfolio_post_button")
                ) {
                    Text(
                        text = if (isPublishing) "Publicando registro na nuvem…" else "Publicar Registro ($authorName)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChefTutorChatSection(
    tutorState: ChefAiUiState,
    selectedWeek: Int,
    prefilledQuestion: String = "",
    activeContextSummary: String = "",
    onAsk: (String) -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(prefilledQuestion) {
        if (prefilledQuestion.isNotBlank()) {
            question = prefilledQuestion
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("CHEF INSTRUTOR IA (v5) • DIAGNÓSTICO DE BANCADA & TUTORIA", InfoBlueBg, InfoBlue)
            if (activeContextSummary.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccAmberBg),
                    border = BorderStroke(1.dp, PccAmber)
                ) {
                    Text(
                        text = "CONTEXTO ATIVO DO SOCORRO: $activeContextSummary",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Text(
                "Consulte equivalências de cortes, Concentração vs. Expansão (Bocuse), alfa-amilase em cremes (Canella-Rawls), hidrocoloides (Peterson), matrizes de Hazan/Paravati/Holland ou tabelas sous-vide.",
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
private fun StudentProfileSection(
    profileState: StreamUiState<UserProfile?>,
    pendingAvatar: CompressedAvatarPayload?,
    defaultName: String,
    isAuthenticated: Boolean,
    onPickAvatar: () -> Unit,
    onSave: (String, String, String, Int, String, String) -> Unit
) {
    val existing = (profileState as? StreamUiState.Success)?.data
    var name by remember(existing) { mutableStateOf(existing?.displayName ?: defaultName) }
    var bio by remember(existing) { mutableStateOf(existing?.bio ?: "Cozinheiro em formação dedicado à gastronomia profissional.") }
    var defaultVis by remember(existing) { mutableStateOf(existing?.defaultVisibility ?: "public") }
    var canton by remember(existing) { mutableStateOf(existing?.cantonFocus ?: "BR") }
    var badge by remember(existing) { mutableStateOf(existing?.specialtyBadge ?: "Cozinheiro em Formação") }
    val week = existing?.currentWeek ?: 1

    val avatarBmp = remember(pendingAvatar, existing?.avatarBase64) {
        pendingAvatar?.avatarBitmap
            ?: existing?.avatarBase64?.let { MediaCompressor.decodeBase64ToBitmap(it) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtelierFullLogoBanner(maxHeight = 88.dp)
            Text("PERFIL PROFISSIONAL • IDENTIFICAÇÃO NA BRIGADA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (avatarBmp != null) {
                    Image(
                        bitmap = avatarBmp.asImageBitmap(),
                        contentDescription = "Avatar do perfil na brigada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Sem foto de avatar",
                        tint = SwissRed,
                        modifier = Modifier.size(64.dp)
                    )
                }
                OutlinedButton(
                    onClick = onPickAvatar,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pick_profile_avatar_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Avatar (≤ 400 px sem EXIF)", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedTextField(name, { name = it }, label = { Text("Nome / Assinatura na Brigada") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(canton, { canton = it }, label = { Text("Região / Escola de Referência (ex.: BR, SP, ZH, BE, GR)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(badge, { badge = it }, label = { Text("Praça / Função na Brigada (ex.: Saucier, Garde-manger, Confeitaria)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bio, { bio = it }, label = { Text("Apresentação Profissional") }, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = defaultVis == "public",
                    onCheckedChange = { isPub -> defaultVis = if (isPub) "public" else "private" }
                )
                Text(
                    if (defaultVis == "public") "Visibilidade padrão: Público no Mural Comunitário"
                    else "Visibilidade padrão: Privado no Meu Portfólio"
                )
            }

            if (isAuthenticated) {
                Button(
                    onClick = { onSave(name, bio, defaultVis, week, canton, badge) },
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_profile_button")
                ) {
                    Text("Salvar Perfil na Nuvem", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
