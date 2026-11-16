package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.media.CompressedMediaPayload
import com.example.core.media.MediaCompressor
import com.example.core.media.ShareableCardGenerator
import com.example.data.ai.ChefAiRepository
import com.example.data.ai.MultimodalChefAudit
import com.example.data.remote.CommunityRepository
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

    private val _selectedImagePayload = MutableStateFlow<CompressedMediaPayload?>(null)
    val selectedImagePayload: StateFlow<CompressedMediaPayload?> = _selectedImagePayload.asStateFlow()

    private val _chefAiState = MutableStateFlow<ChefAiUiState>(ChefAiUiState.Idle)
    val chefAiState: StateFlow<ChefAiUiState> = _chefAiState.asStateFlow()

    private val _tutorChatState = MutableStateFlow<ChefAiUiState>(ChefAiUiState.Idle)
    val tutorChatState: StateFlow<ChefAiUiState> = _tutorChatState.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

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

    fun saveProfile(
        displayName: String,
        bio: String,
        currentWeek: Int,
        cantonFocus: String,
        specialtyBadge: String
    ) {
        viewModelScope.launch {
            communityRepo.upsertProfile(
                displayName = displayName,
                bio = bio,
                avatarUrl = null,
                currentWeek = currentWeek,
                cantonFocus = cantonFocus,
                specialtyBadge = specialtyBadge
            ).onSuccess {
                _feedbackMessage.value = "Perfil gastronômico salvo no Firestore!"
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
                    _feedbackMessage.value =
                        "Foto comprimida sem EXIF (${payload.width}×${payload.height}px, pronta p/ Mural e Chef IA)."
                }
                .onFailure { err ->
                    _feedbackMessage.value = "Falha ao comprimir imagem: ${err.localizedMessage}"
                }
        }
    }

    fun clearSelectedPhoto() {
        _selectedImagePayload.value = null
        _chefAiState.value = ChefAiUiState.Idle
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
                    appendLine("Nota Estimada Suíça: ${"%.1f".format(audit.swissGradeEstimate)} / 6.0")
                    appendLine("✓ Elogio 1: ${audit.praiseOne}")
                    appendLine("✓ Elogio 2: ${audit.praiseTwo}")
                    appendLine("➔ Correção Prática: ${audit.practicalCorrection}")
                    append("PCC & Geometria: ${audit.pccAndTechnicalCompliance}")
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
        aiFeedback: String,
        onPublished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val media = _selectedImagePayload.value
            communityRepo.createPostWithMedia(
                authorName = authorName,
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
    }

    fun submitComment(post: PortfolioPost, authorName: String, text: String) {
        viewModelScope.launch {
            communityRepo.addComment(post, authorName, text).onFailure { err ->
                _feedbackMessage.value = "Erro ao comentar: ${err.localizedMessage}"
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
                _feedbackMessage.value = "Falha ao gerar Card PNG: ${err.localizedMessage}"
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
