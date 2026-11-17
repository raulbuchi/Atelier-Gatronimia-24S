package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.media.CompressedAvatarPayload
import com.example.core.media.CompressedMediaPayload
import com.example.core.media.MediaCompressor
import com.example.core.media.ShareableCardGenerator
import com.example.data.ai.ChefAiRepository
import com.example.data.ai.MultimodalChefAudit
import com.example.data.local.MepSheetEntity
import com.example.data.remote.CommunityRepository
import com.example.data.remote.MepSheetCloudLink
import com.example.data.remote.PortfolioMedia
import com.example.data.remote.PortfolioPost
import com.example.data.remote.PostComment
import com.example.data.remote.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "CommunityAiVM"

sealed interface StreamUiState<out T> {
    data object Loading : StreamUiState<Nothing>
    data class Success<T>(val data: T) : StreamUiState<T>
    data class Error(val message: String) : StreamUiState<Nothing>
}

sealed interface ChefAiUiState {
    data object Idle : ChefAiUiState
    data object Loading : ChefAiUiState
    data class Streaming(val partialText: String) : ChefAiUiState
    data class Success(val text: String) : ChefAiUiState
    data class AuditReady(val audit: MultimodalChefAudit, val formattedText: String) : ChefAiUiState
    data class Error(val message: String) : ChefAiUiState
}

class CommunityAiViewModel(
    private val communityRepo: CommunityRepository,
    private val chefAiRepo: ChefAiRepository,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<StreamUiState<UserProfile?>> = communityRepo.observeProfile(currentUserId)
        .map<UserProfile?, StreamUiState<UserProfile?>> { StreamUiState.Success(it) }
        .catch { error -> emit(unwrapToErrorState(error, "Falha ao carregar perfil")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val myPostsState: StateFlow<StreamUiState<List<PortfolioPost>>> = communityRepo.observeMyPosts(currentUserId)
        .map<List<PortfolioPost>, StreamUiState<List<PortfolioPost>>> { StreamUiState.Success(it) }
        .catch { error -> emit(unwrapToErrorState(error, "Falha ao carregar Meu Portfólio")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val publicMuralState: StateFlow<StreamUiState<List<PortfolioPost>>> = communityRepo.observePublicMural(currentUserId)
        .map<List<PortfolioPost>, StreamUiState<List<PortfolioPost>>> { StreamUiState.Success(it) }
        .catch { error -> emit(unwrapToErrorState(error, "Falha ao carregar Mural do Atelier")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val myMepLinksState: StateFlow<StreamUiState<List<MepSheetCloudLink>>> = communityRepo.observeMyMepSheetLinks(currentUserId)
        .map<List<MepSheetCloudLink>, StreamUiState<List<MepSheetCloudLink>>> { StreamUiState.Success(it) }
        .catch { error -> emit(unwrapToErrorState(error, "Falha ao sincronizar links das Fichas Técnicas")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    private val _selectedImagePayload = MutableStateFlow<CompressedMediaPayload?>(null)
    val selectedImagePayload: StateFlow<CompressedMediaPayload?> = _selectedImagePayload.asStateFlow()

    private val _pendingAvatarPayload = MutableStateFlow<CompressedAvatarPayload?>(null)
    val pendingAvatarPayload: StateFlow<CompressedAvatarPayload?> = _pendingAvatarPayload.asStateFlow()

    private val _chefAiState = MutableStateFlow<ChefAiUiState>(ChefAiUiState.Idle)
    val chefAiState: StateFlow<ChefAiUiState> = _chefAiState.asStateFlow()

    private val _tutorChatState = MutableStateFlow<ChefAiUiState>(ChefAiUiState.Idle)
    val tutorChatState: StateFlow<ChefAiUiState> = _tutorChatState.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _isSyncingMepPhoto = MutableStateFlow(false)
    val isSyncingMepPhoto: StateFlow<Boolean> = _isSyncingMepPhoto.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _loadedFullMediaCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val loadedFullMediaCache: StateFlow<Map<String, Bitmap>> = _loadedFullMediaCache.asStateFlow()

    init {
        // Ensure culinary student profile exists on first authenticated session
        viewModelScope.launch {
            val currentSnap = communityRepo.observeProfile(currentUserId)
            try {
                // Lightweight check: only initialize if missing when user triggers save or lazily
                Log.d(TAG, "Session active for UID=$currentUserId ($initialDisplayName)")
            } catch (_: Exception) {
            }
        }
    }

    fun compressSelectedAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            MediaCompressor.compressAvatarFromUri(context, uri)
                .onSuccess { payload ->
                    _pendingAvatarPayload.value = payload
                    val sizeFmt = MediaCompressor.formatBytes(payload.compressedSizeBytes.toLong())
                    _feedbackMessage.value =
                        "Avatar otimizado (${payload.width}×${payload.height} px • $sizeFmt • −${payload.savingsPercent}% sem EXIF). Toque em Salvar Perfil."
                }
                .onFailure { err ->
                    _feedbackMessage.value = "Falha ao otimizar avatar: ${err.localizedMessage}"
                }
        }
    }

    fun saveProfile(
        displayName: String,
        bio: String,
        defaultVisibility: String = "public",
        currentWeek: Int,
        cantonFocus: String,
        specialtyBadge: String
    ) {
        viewModelScope.launch {
            val existingAvatar = (profileState.value as? StreamUiState.Success)?.data?.avatarBase64
            val resolvedAvatar = _pendingAvatarPayload.value?.avatarBase64 ?: existingAvatar
            communityRepo.upsertProfile(
                displayName = displayName,
                bio = bio,
                avatarUrl = null,
                avatarBase64 = resolvedAvatar,
                defaultVisibility = defaultVisibility,
                currentWeek = currentWeek,
                cantonFocus = cantonFocus,
                specialtyBadge = specialtyBadge
            ).onSuccess {
                _pendingAvatarPayload.value = null
                _feedbackMessage.value = "Perfil gastronômico salvo na nuvem!"
            }.onFailure { err ->
                _feedbackMessage.value = "Erro ao salvar perfil: ${err.localizedMessage}"
            }
        }
    }

    fun compressSelectedPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            MediaCompressor.compressFromUri(context, uri)
                .onSuccess { payload ->
                    _selectedImagePayload.value = payload
                    val sizeFmt = MediaCompressor.formatBytes(payload.compressedSizeBytes.toLong())
                    val savingsLabel = if (payload.savingsPercent > 0) " • −${payload.savingsPercent}%" else ""
                    _feedbackMessage.value =
                        "Foto otimizada em fundo branco sem EXIF (${payload.width}×${payload.height} px • $sizeFmt$savingsLabel)."
                }
                .onFailure { err ->
                    _feedbackMessage.value = "Falha ao processar imagem: ${err.localizedMessage}"
                }
        }
    }

    fun clearSelectedPhoto() {
        _selectedImagePayload.value = null
        _chefAiState.value = ChefAiUiState.Idle
    }

    fun captureAndLinkPhotoToMepSheet(
        context: Context,
        uri: Uri,
        sheet: MepSheetEntity,
        onLinked: (String, MepSheetCloudLink) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncingMepPhoto.value = true
            MediaCompressor.compressFromUri(context, uri)
                .onSuccess { payload ->
                    _selectedImagePayload.value = payload
                    communityRepo.saveMepSheetCreationPhoto(
                        recipeId = sheet.recipeId,
                        title = sheet.title,
                        weekNumber = sheet.weekNumber,
                        stationBrigade = sheet.stationBrigade,
                        targetServings = sheet.targetServings,
                        pccTargetTempC = sheet.pccTargetTempC,
                        executionScore1To5 = sheet.executionScore1To5,
                        thumbnailBase64 = payload.thumbnailBase64,
                        fullImageBase64 = payload.fullBase64,
                        imageWidth = payload.width,
                        imageHeight = payload.height,
                        localPhotoUri = payload.localUriString
                    ).onSuccess { cloudLink ->
                        _loadedFullMediaCache.value =
                            _loadedFullMediaCache.value + (cloudLink.mediaId to payload.fullBitmap)
                        _isSyncingMepPhoto.value = false
                        onLinked(payload.localUriString, cloudLink)
                        _feedbackMessage.value =
                            "Criação associada à Ficha Técnica e link salvo no Firestore (${cloudLink.imageLink})."
                    }.onFailure { err ->
                        _isSyncingMepPhoto.value = false
                        _feedbackMessage.value =
                            "Erro ao gravar link da imagem no Firestore: ${err.localizedMessage}"
                    }
                }
                .onFailure { err ->
                    _isSyncingMepPhoto.value = false
                    _feedbackMessage.value =
                        "Falha ao comprimir foto da câmera: ${err.localizedMessage}"
                }
        }
    }

    fun preloadTransferredMepPhoto(
        context: Context,
        localPhotoUri: String,
        cloudMediaId: String,
        thumbnailBase64: String
    ) {
        if (_selectedImagePayload.value != null) return
        viewModelScope.launch {
            if (localPhotoUri.isNotBlank()) {
                val parsed = runCatching { Uri.parse(localPhotoUri) }.getOrNull()
                if (parsed != null) {
                    val localRes = MediaCompressor.compressFromUri(context, parsed)
                    if (localRes.isSuccess) {
                        _selectedImagePayload.value = localRes.getOrNull()
                        return@launch
                    }
                }
            }
            if (cloudMediaId.isNotBlank()) {
                val media = communityRepo.getFullMedia(cloudMediaId).getOrNull()
                val bmp = media?.imageBase64?.let { MediaCompressor.decodeBase64ToBitmap(it) }
                if (media != null && bmp != null) {
                    _selectedImagePayload.value = CompressedMediaPayload(
                        fullBitmap = bmp,
                        fullBase64 = media.imageBase64,
                        thumbnailBase64 = thumbnailBase64,
                        width = media.width,
                        height = media.height
                    )
                }
            }
        }
    }

    fun runMultimodalChefAudit(
        materialType: String,
        recipeTitle: String,
        weekNumber: Int,
        studentNotes: String,
        overrideBitmap: Bitmap? = null
    ) {
        val targetBitmap = overrideBitmap ?: _selectedImagePayload.value?.fullBitmap
        if (targetBitmap == null) {
            _chefAiState.value = ChefAiUiState.Error("Selecione primeiro uma foto do prato, cortes ou Ficha MEP.")
            return
        }
        viewModelScope.launch {
            _chefAiState.value = ChefAiUiState.Loading
            chefAiRepo.evaluateCulinaryPhoto(
                bitmap = targetBitmap,
                materialType = materialType,
                recipeTitle = recipeTitle,
                weekNumber = weekNumber,
                studentNotes = studentNotes
            ).onSuccess { audit ->
                val formatted = buildString {
                    appendLine("Nota Técnica Estimada: ${"%.1f".format(audit.swissGradeEstimate)} / 6,0")
                    appendLine("✓ Ponto Forte 1: ${audit.praiseOne}")
                    appendLine("✓ Ponto Forte 2: ${audit.praiseTwo}")
                    appendLine("➔ Ajuste de Bancada: ${audit.practicalCorrection}")
                    append("PCC & Padronização: ${audit.pccAndTechnicalCompliance}")
                }
                _chefAiState.value = ChefAiUiState.AuditReady(audit, formatted)
            }.onFailure { err ->
                _chefAiState.value = ChefAiUiState.Error(
                    err.localizedMessage ?: "Falha na avaliação visual do Chef IA."
                )
            }
        }
    }

    fun askChefTutor(question: String, activeContext: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _tutorChatState.value = ChefAiUiState.Loading
            try {
                var finalResponse = ""
                chefAiRepo.streamChefTutorGuidance(question, activeContext).collect { partial ->
                    finalResponse = partial
                    _tutorChatState.value = ChefAiUiState.Streaming(partial)
                }
                _tutorChatState.value = ChefAiUiState.Success(finalResponse)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tutor query failed", e)
                _tutorChatState.value = ChefAiUiState.Error(
                    e.localizedMessage ?: "Falha ao consultar o Chef Instrutor IA."
                )
            }
        }
    }

    fun publishPortfolioPost(
        authorName: String,
        visibility: String,
        weekNumber: Int,
        cycleNumber: Int,
        axisId: Int,
        materialType: String,
        recipeId: String,
        recipeTitle: String,
        isSwissmilkClassic: Boolean,
        isChefkochV4: Boolean,
        cantonCode: String,
        caption: String,
        mepSummary: String,
        selbstkontrolleScore: Int = 0,
        erkenntnisse: String = "",
        offenePunkte: String = "",
        aiFeedback: String,
        onPublished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val media = _selectedImagePayload.value
            val denormalizedAvatar = _pendingAvatarPayload.value?.avatarBase64
                ?: (profileState.value as? StreamUiState.Success)?.data?.avatarBase64
            communityRepo.createPostWithMedia(
                authorName = authorName,
                authorAvatarBase64 = denormalizedAvatar,
                visibility = visibility,
                weekNumber = weekNumber,
                cycleNumber = cycleNumber,
                axisId = axisId,
                materialType = materialType,
                recipeId = recipeId,
                recipeTitle = recipeTitle,
                isSwissmilkClassic = isSwissmilkClassic,
                isChefkochV4 = isChefkochV4,
                cantonCode = cantonCode,
                caption = caption,
                mepSummary = mepSummary,
                selbstkontrolleScore = selbstkontrolleScore,
                erkenntnisse = erkenntnisse,
                offenePunkte = offenePunkte,
                aiFeedback = aiFeedback,
                thumbnailBase64 = media?.thumbnailBase64.orEmpty(),
                fullImageBase64 = media?.fullBase64,
                imageWidth = media?.width ?: 0,
                imageHeight = media?.height ?: 0
            ).onSuccess {
                _isPublishing.value = false
                _selectedImagePayload.value = null
                _chefAiState.value = ChefAiUiState.Idle
                _feedbackMessage.value = if (visibility == "public") {
                    "Publicado no Mural do Atelier e no Meu Portfólio!"
                } else {
                    "Salvo de forma privada no Meu Portfólio!"
                }
                onPublished()
            }.onFailure { err ->
                _isPublishing.value = false
                _feedbackMessage.value = "Erro ao publicar: ${err.localizedMessage}"
            }
        }
    }

    fun togglePostVisibility(post: PortfolioPost) {
        val nextVis = if (post.visibility == "public") "private" else "public"
        viewModelScope.launch {
            communityRepo.updatePostVisibilityOrFeedback(
                postId = post.id,
                visibility = nextVis,
                caption = post.caption,
                mepSummary = post.mepSummary,
                aiFeedback = post.aiFeedback
            ).onSuccess {
                _feedbackMessage.value = "Visibilidade alterada para ${if (nextVis == "public") "Público (Mural)" else "Privado"}."
            }.onFailure { err ->
                _feedbackMessage.value = "Erro ao alterar visibilidade: ${err.localizedMessage}"
            }
        }
    }

    fun toggleReaction(post: PortfolioPost) {
        viewModelScope.launch {
            communityRepo.toggleReaction(post).onFailure { err ->
                _feedbackMessage.value = "Erro ao reagir: ${err.localizedMessage}"
            }
        }
    }

    fun observeCommentsForPost(post: PortfolioPost): Flow<List<PostComment>> {
        return communityRepo.observeComments(post.id, post.visibility, currentUserId)
            .catch { error ->
                unwrapToErrorState(error, "Falha ao carregar comentários")
                emit(emptyList())
            }
    }

    fun submitComment(post: PortfolioPost, authorName: String, text: String) {
        viewModelScope.launch {
            communityRepo.addComment(post, authorName, text).onSuccess {
                _feedbackMessage.value = "Comentário técnico publicado no Mural!"
            }.onFailure { err ->
                _feedbackMessage.value = "Erro ao comentar: ${err.localizedMessage}"
            }
        }
    }

    fun deleteComment(post: PortfolioPost, comment: PostComment) {
        viewModelScope.launch {
            communityRepo.deleteComment(post, comment).onSuccess {
                _feedbackMessage.value = "Comentário removido."
            }.onFailure { err ->
                _feedbackMessage.value = "Erro ao remover comentário: ${err.localizedMessage}"
            }
        }
    }

    fun deleteOwnPost(postId: String) {
        viewModelScope.launch {
            communityRepo.deletePost(postId).onSuccess {
                _feedbackMessage.value = "Registro removido do portfólio."
            }.onFailure { err ->
                _feedbackMessage.value = "Erro ao excluir: ${err.localizedMessage}"
            }
        }
    }

    fun loadFullResolutionPhoto(postId: String) {
        if (_loadedFullMediaCache.value.containsKey(postId)) return
        viewModelScope.launch {
            communityRepo.getFullMedia(postId).onSuccess { media: PortfolioMedia? ->
                val bmp = media?.imageBase64?.let { MediaCompressor.decodeBase64ToBitmap(it) }
                if (bmp != null) {
                    _loadedFullMediaCache.value = _loadedFullMediaCache.value + (postId to bmp)
                }
            }
        }
    }

    fun exportShareablePngCard(context: Context, post: PortfolioPost) {
        viewModelScope.launch {
            val fullBmp = _loadedFullMediaCache.value[post.id]
                ?: communityRepo.getFullMedia(post.id).getOrNull()?.imageBase64?.let {
                    MediaCompressor.decodeBase64ToBitmap(it)
                }
                ?: MediaCompressor.decodeBase64ToBitmap(post.thumbnailBase64)

            ShareableCardGenerator.exportAndShareCard(
                context = context,
                recipeTitle = post.recipeTitle,
                authorName = post.authorName,
                weekNumber = post.weekNumber,
                cantonCode = post.cantonCode,
                materialType = post.materialType,
                caption = post.caption,
                mepSummary = post.mepSummary,
                aiFeedback = post.aiFeedback,
                photoBitmap = fullBmp
            ).onFailure { err ->
                _feedbackMessage.value = "Falha ao gerar Cartão Técnico: ${err.localizedMessage}"
            }
        }
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    private fun unwrapToErrorState(error: Throwable, fallback: String): StreamUiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return StreamUiState.Error(root.localizedMessage ?: fallback)
    }
}
