package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val avatarBase64: String? = null,
    val defaultVisibility: String = "public", // "private" | "public"
    val bio: String? = null,
    val currentWeek: Int = 1,
    val cantonFocus: String = "ZH",
    val specialtyBadge: String = "Lernender EFZ",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PortfolioPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarBase64: String? = null,
    val visibility: String = "private", // "private" | "public"
    val weekNumber: Int = 1,
    val cycleNumber: Int = 1,
    val axisId: Int = 1,
    val materialType: String = "prato_final", // "prato_final" | "cortes_mep" | "ficha_preenchida" | "resumo_esquema"
    val recipeId: String = "",
    val recipeTitle: String = "",
    @get:PropertyName("isSwissmilkClassic")
    @PropertyName("isSwissmilkClassic")
    val isSwissmilkClassic: Boolean = false,
    @get:PropertyName("isChefkochV4")
    @PropertyName("isChefkochV4")
    val isChefkochV4: Boolean = false,
    val cantonCode: String = "",
    val caption: String = "",
    val mepSummary: String = "",
    val selbstkontrolleScore: Int = 0,
    val erkenntnisse: String = "",
    val offenePunkte: String = "",
    val aiFeedback: String = "",
    val thumbnailBase64: String = "",
    val imageLink: String? = null,
    val reactedUserIds: List<String> = emptyList(),
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PortfolioMedia(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "private",
    val imageBase64: String = "",
    val width: Int = 0,
    val height: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MepSheetCloudLink(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipeId: String = "",
    val title: String = "",
    val weekNumber: Int = 1,
    val stationBrigade: String = "Saucier",
    val targetServings: Int = 4,
    val pccTargetTempC: Int = 70,
    val executionScore1To5: Int = 4,
    val mediaId: String = "",
    val imageLink: String = "",
    val thumbnailBase64: String = "",
    val localPhotoUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postOwnerId: String = "",
    val postVisibility: String = "public",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
