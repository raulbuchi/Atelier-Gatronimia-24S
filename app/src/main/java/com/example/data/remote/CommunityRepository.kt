package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

class CommunityRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário deve estar autenticado com Google.")
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
        currentWeek: Int,
        cantonFocus: String,
        specialtyBadge: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("profiles").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim().take(80).ifEmpty { "Aluno Chef" },
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
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().take(80).ifEmpty { "Aluno Chef" },
                    "bio" to bio?.trim()?.take(240)?.takeIf { it.isNotEmpty() },
                    "avatarUrl" to avatarUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
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
            "aiFeedback" to aiFeedback.trim().take(2500),
            "thumbnailBase64" to thumbnailBase64.take(80000),
            "reactedUserIds" to emptyList<String>(),
            "reactionCount" to 0,
            "commentCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

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
            // Sync media visibility if media document exists
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
        val currentList = post.reactedUserIds
        val updatedList = if (uid in currentList) {
            currentList.filterNot { it == uid }
        } else {
            (currentList + uid).distinct().take(500)
        }
        try {
            postRef.update(
                mapOf(
                    "reactedUserIds" to updatedList,
                    "reactionCount" to updatedList.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        requireUserId()
        val postRef = db.collection("portfolio_posts").document(postId)
        val mediaRef = db.collection("portfolio_media").document(postId)
        try {
            val mediaSnap = mediaRef.get().await()
            val batch = db.batch()
            batch.delete(postRef)
            if (mediaSnap.exists()) {
                batch.delete(mediaRef)
            }
            batch.commit().await()
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
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
