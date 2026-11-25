package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * O Firestore é obtido sob demanda: sem `google-services.json` (FirebaseApp ausente) o app abre em
 * modo local e nenhuma chamada ao Firestore é feita enquanto não há login.
 */
class CommunityRepository(firestoreProvider: () -> FirebaseFirestore) {

    constructor(db: FirebaseFirestore) : this({ db })

    constructor(context: Context) : this({
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    })

    private val db: FirebaseFirestore by lazy(firestoreProvider)

    private fun requireUserId(): String {
        throw IllegalStateException("Login Google desabilitado temporariamente para testes.")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val path = "profiles/$userId"
        return db.collection("profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowFirestoreError(error, OperationType.GET, path, userId)
                throw error
            }
    }

    suspend fun upsertProfile(
        displayName: String,
        bio: String?,
        avatarUrl: String?,
        avatarBase64: String? = null,
        defaultVisibility: String = "public",
        currentWeek: Int,
        cantonFocus: String,
        specialtyBadge: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("profiles").document(uid)
        val safeDefaultVis = if (defaultVisibility == "private") "private" else "public"
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim().take(80).ifEmpty { "Aluno Chef" },
                    "defaultVisibility" to safeDefaultVis,
                    "currentWeek" to currentWeek.coerceIn(1, 24),
                    "cantonFocus" to cantonFocus.trim().take(10).ifEmpty { "ZH" },
                    "specialtyBadge" to specialtyBadge.trim().take(60).ifEmpty { "Lernender EFZ" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!bio.isNullOrBlank()) {
                    updates["bio"] = bio.trim().take(240)
                }
                if (!avatarUrl.isNullOrBlank()) {
                    updates["avatarUrl"] = avatarUrl.trim().take(500)
                }
                if (!avatarBase64.isNullOrBlank()) {
                    updates["avatarBase64"] = avatarBase64.take(59000)
                }
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().take(80).ifEmpty { "Aluno Chef" },
                    "bio" to bio?.trim()?.take(240)?.takeIf { it.isNotEmpty() },
                    "avatarUrl" to avatarUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                    "avatarBase64" to avatarBase64?.take(59000)?.takeIf { it.isNotEmpty() },
                    "defaultVisibility" to safeDefaultVis,
                    "currentWeek" to currentWeek.coerceIn(1, 24),
                    "cantonFocus" to cantonFocus.trim().take(10).ifEmpty { "ZH" },
                    "specialtyBadge" to specialtyBadge.trim().take(60).ifEmpty { "Lernender EFZ" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeMyPosts(userId: String): Flow<List<PortfolioPost>> {
        val path = "portfolio_posts"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PortfolioPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowFirestoreError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observePublicMural(currentUserId: String): Flow<List<PortfolioPost>> {
        val path = "portfolio_posts"
        return db.collection(path)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PortfolioPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowFirestoreError(error, OperationType.LIST, path, currentUserId)
                throw error
            }
    }

    suspend fun createPostWithMedia(
        authorName: String,
        authorAvatarBase64: String? = null,
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
        thumbnailBase64: String,
        fullImageBase64: String?,
        imageWidth: Int,
        imageHeight: Int
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("portfolio_posts").document()
        val postId = postRef.id
        val safeVisibility = if (visibility == "public") "public" else "private"
        val safeMaterial = if (materialType in setOf("prato_final", "cortes_mep", "ficha_preenchida", "resumo_esquema")) {
            materialType
        } else {
            "prato_final"
        }

        val postPayload = mapOf(
            "userId" to uid,
            "authorName" to authorName.trim().take(80).ifEmpty { "Chef Aluno" },
            "authorAvatarBase64" to authorAvatarBase64?.take(59000)?.takeIf { it.isNotBlank() },
            "visibility" to safeVisibility,
            "weekNumber" to weekNumber.coerceIn(1, 24),
            "cycleNumber" to cycleNumber.coerceIn(1, 4),
            "axisId" to axisId.coerceIn(1, 6),
            "materialType" to safeMaterial,
            "recipeId" to recipeId.trim().take(80).ifEmpty { "livre" },
            "recipeTitle" to recipeTitle.trim().take(120).ifEmpty { "Treino Técnico" },
            "isSwissmilkClassic" to isSwissmilkClassic,
            "isChefkochV4" to isChefkochV4,
            "cantonCode" to cantonCode.trim().take(10),
            "caption" to caption.trim().take(1000).ifEmpty { "Registro técnico de bancada." },
            "mepSummary" to mepSummary.trim().take(1500),
            "selbstkontrolleScore" to selbstkontrolleScore.coerceIn(0, 5).takeIf { it > 0 },
            "erkenntnisse" to erkenntnisse.trim().take(600).takeIf { it.isNotEmpty() },
            "offenePunkte" to offenePunkte.trim().take(600).takeIf { it.isNotEmpty() },
            "aiFeedback" to aiFeedback.trim().take(2500),
            "thumbnailBase64" to thumbnailBase64.take(80000),
            "imageLink" to if (!fullImageBase64.isNullOrBlank()) "firestore://portfolio_media/$postId" else null,
            "reactedUserIds" to emptyList<String>(),
            "reactionCount" to 0,
            "commentCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            val batch = db.batch()
            batch.set(postRef, postPayload)
            if (!fullImageBase64.isNullOrBlank()) {
                val mediaRef = db.collection("portfolio_media").document(postId)
                val mediaPayload = mapOf(
                    "userId" to uid,
                    "visibility" to safeVisibility,
                    "imageBase64" to fullImageBase64.take(590000),
                    "width" to imageWidth.coerceIn(1, 2000),
                    "height" to imageHeight.coerceIn(1, 2000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(mediaRef, mediaPayload)
            }
            batch.commit().await()
            postId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            throw e
        }
    }

    suspend fun getFullMedia(postId: String): Result<PortfolioMedia?> = runCatching {
        val docRef = db.collection("portfolio_media").document(postId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    PortfolioMedia::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeMyMepSheetLinks(userId: String): Flow<List<MepSheetCloudLink>> {
        val path = "mep_sheets"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MepSheetCloudLink::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowFirestoreError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun saveMepSheetCreationPhoto(
        recipeId: String,
        title: String,
        weekNumber: Int,
        stationBrigade: String,
        targetServings: Int,
        pccTargetTempC: Int,
        executionScore1To5: Int,
        thumbnailBase64: String,
        fullImageBase64: String,
        imageWidth: Int,
        imageHeight: Int,
        localPhotoUri: String? = null
    ): Result<MepSheetCloudLink> = runCatching {
        val uid = requireUserId()
        val safeRecipeId = recipeId.trim().take(80).ifEmpty { "ficha_autoral" }
        val safeDocId = sanitizeFirestoreDocId("${uid}_$safeRecipeId")
        val sheetRef = db.collection("mep_sheets").document(safeDocId)

        val generatedMediaToken = db.collection("portfolio_media").document().id
        val mediaId = sanitizeFirestoreDocId("mep_media_$generatedMediaToken")
        val mediaRef = db.collection("portfolio_media").document(mediaId)
        val canonicalImageLink = "firestore://portfolio_media/$mediaId"

        val safeTitle = title.trim().take(120).ifEmpty { safeRecipeId }
        val safeBrigade = stationBrigade.trim().take(40).ifEmpty { "Saucier" }
        val safeWeek = weekNumber.coerceIn(1, 24)
        val safeServings = targetServings.coerceIn(1, 500)
        val safePcc = pccTargetTempC.coerceIn(-30, 250)
        val safeScore = executionScore1To5.coerceIn(1, 5)
        val safeThumb = thumbnailBase64.take(80000).ifEmpty { "dGh1bWI=" }
        val safeLocalUri = localPhotoUri?.trim()?.take(500)?.takeIf { it.isNotEmpty() }

        try {
            val existingQuery = db.collection("mep_sheets")
                .whereEqualTo("userId", uid)
                .whereEqualTo("recipeId", safeRecipeId)
                .limit(1)
                .get()
                .await()
            val existingDoc = existingQuery.documents.firstOrNull()
            val targetSheetRef = existingDoc?.reference ?: sheetRef
            val previousMediaId = existingDoc?.getString("mediaId")

            val mediaPayload = mapOf(
                "userId" to uid,
                "visibility" to "private",
                "imageBase64" to fullImageBase64.take(590000),
                "width" to imageWidth.coerceIn(1, 2000),
                "height" to imageHeight.coerceIn(1, 2000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(mediaRef, mediaPayload)

            if (existingDoc != null) {
                val updatePayload = mutableMapOf<String, Any>(
                    "title" to safeTitle,
                    "weekNumber" to safeWeek,
                    "stationBrigade" to safeBrigade,
                    "targetServings" to safeServings,
                    "pccTargetTempC" to safePcc,
                    "executionScore1To5" to safeScore,
                    "mediaId" to mediaId,
                    "imageLink" to canonicalImageLink,
                    "thumbnailBase64" to safeThumb,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeLocalUri != null) {
                    updatePayload["localPhotoUri"] = safeLocalUri
                }
                batch.update(targetSheetRef, updatePayload)
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "recipeId" to safeRecipeId,
                    "title" to safeTitle,
                    "weekNumber" to safeWeek,
                    "stationBrigade" to safeBrigade,
                    "targetServings" to safeServings,
                    "pccTargetTempC" to safePcc,
                    "executionScore1To5" to safeScore,
                    "mediaId" to mediaId,
                    "imageLink" to canonicalImageLink,
                    "thumbnailBase64" to safeThumb,
                    "localPhotoUri" to safeLocalUri,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(targetSheetRef, createPayload)
            }

            if (!previousMediaId.isNullOrBlank() &&
                previousMediaId != mediaId &&
                previousMediaId.startsWith("mep_media_")
            ) {
                val oldMediaRef = db.collection("portfolio_media").document(previousMediaId)
                batch.delete(oldMediaRef)
            }

            batch.commit().await()

            MepSheetCloudLink(
                id = targetSheetRef.id,
                userId = uid,
                recipeId = safeRecipeId,
                title = safeTitle,
                weekNumber = safeWeek,
                stationBrigade = safeBrigade,
                targetServings = safeServings,
                pccTargetTempC = safePcc,
                executionScore1To5 = safeScore,
                mediaId = mediaId,
                imageLink = canonicalImageLink,
                thumbnailBase64 = safeThumb,
                localPhotoUri = safeLocalUri
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sheetRef.path)
            throw e
        }
    }

    private fun sanitizeFirestoreDocId(raw: String): String {
        return raw
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)
            .ifEmpty { "doc_${System.currentTimeMillis()}" }
    }

    suspend fun getPostById(postId: String): Result<PortfolioPost?> = runCatching {
        val docRef = db.collection("portfolio_posts").document(postId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    PortfolioPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updatePostVisibilityOrFeedback(
        postId: String,
        visibility: String,
        caption: String,
        mepSummary: String,
        aiFeedback: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val safeVis = if (visibility == "public") "public" else "private"
        val postRef = db.collection("portfolio_posts").document(postId)
        try {
            postRef.update(
                mapOf(
                    "visibility" to safeVis,
                    "caption" to caption.trim().take(1000).ifEmpty { "Registro técnico." },
                    "mepSummary" to mepSummary.trim().take(1500),
                    "aiFeedback" to aiFeedback.trim().take(2500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val mediaRef = db.collection("portfolio_media").document(postId)
            val mediaSnap = mediaRef.get().await()
            if (mediaSnap.exists()) {
                mediaRef.update(
                    mapOf(
                        "visibility" to safeVis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    suspend fun toggleReaction(post: PortfolioPost): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID obrigatório" }
        val postRef = db.collection("portfolio_posts").document(post.id)
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(postRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Publicação não encontrada no Mural.")
                }
                val currentList = (snap.get("reactedUserIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val updatedList = if (uid in currentList) {
                    currentList.filterNot { it == uid }
                } else {
                    (currentList + uid).distinct().take(500)
                }
                transaction.update(
                    postRef,
                    mapOf(
                        "reactedUserIds" to updatedList,
                        "reactionCount" to updatedList.size,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    fun observeComments(
        postId: String,
        postVisibility: String,
        currentUserId: String
    ): Flow<List<PostComment>> {
        val path = "portfolio_posts/$postId/comments"
        val baseCol = db.collection("portfolio_posts").document(postId).collection("comments")
        val query = if (postVisibility == "public") {
            baseCol.whereEqualTo("postVisibility", "public")
        } else {
            baseCol.whereEqualTo("postOwnerId", currentUserId)
        }
        return query
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PostComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowFirestoreError(error, OperationType.LIST, path, currentUserId)
                throw error
            }
    }

    suspend fun addComment(
        post: PortfolioPost,
        authorName: String,
        text: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(post.id.isNotBlank()) { "Post ID obrigatório" }
        val cleanText = text.trim().take(500)
        require(cleanText.isNotEmpty()) { "Comentário vazio" }

        val postRef = db.collection("portfolio_posts").document(post.id)
        val commentRef = postRef.collection("comments").document()

        val commentPayload = mapOf(
            "postId" to post.id,
            "postOwnerId" to post.userId,
            "postVisibility" to post.visibility,
            "userId" to uid,
            "authorName" to authorName.trim().take(80).ifEmpty { "Chef Aluno" },
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            val batch = db.batch()
            batch.set(commentRef, commentPayload)
            batch.update(
                postRef,
                mapOf(
                    "commentCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    suspend fun deleteComment(
        post: PortfolioPost,
        comment: PostComment
    ): Result<Unit> = runCatching {
        requireUserId()
        require(post.id.isNotBlank() && comment.id.isNotBlank()) { "IDs obrigatórios" }
        val postRef = db.collection("portfolio_posts").document(post.id)
        val commentRef = postRef.collection("comments").document(comment.id)
        try {
            val batch = db.batch()
            batch.delete(commentRef)
            if (post.commentCount > 0) {
                batch.update(
                    postRef,
                    mapOf(
                        "commentCount" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            throw e
        }
    }

    /**
     * Cascade-deletes child comments in portfolio_posts/{postId}/comments alongside
     * portfolio_media/{postId} and portfolio_posts/{postId} in batches (v6.md Section 2.A).
     */
    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("portfolio_posts").document(postId)
        val mediaRef = db.collection("portfolio_media").document(postId)
        try {
            val childCommentsSnap = postRef.collection("comments")
                .whereEqualTo("postOwnerId", uid)
                .get()
                .await()
            val mediaSnap = mediaRef.get().await()

            // Firestore write batch limit is 500 operations; chunk safely if needed
            childCommentsSnap.documents.chunked(450).forEach { chunk ->
                val commentBatch = db.batch()
                chunk.forEach { commentDoc -> commentBatch.delete(commentDoc.reference) }
                commentBatch.commit().await()
            }

            val rootBatch = db.batch()
            rootBatch.delete(postRef)
            if (mediaSnap.exists()) {
                rootBatch.delete(mediaRef)
            }
            rootBatch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            throw e
        }
    }

    private fun logFlowFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && expectedUid.isNotBlank()) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
