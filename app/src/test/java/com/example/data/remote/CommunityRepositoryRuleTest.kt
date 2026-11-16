package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CommunityRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertProfile_andObserveOwnProfile_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@atelier.ch")
        val repository = CommunityRepository(firestore)

        val upsertRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertProfile(
                displayName = "Chef Alice",
                bio = "Foco em Saucier e Sous-vide",
                avatarUrl = null,
                currentWeek = 17,
                cantonFocus = "ZH",
                specialtyBadge = "Saucier"
            )
        }
        assertTrue("Expected upsertProfile success: ${upsertRes.exceptionOrNull()}", upsertRes.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Chef Alice", profile?.displayName)
        assertEquals(17, profile?.currentWeek)
    }

    @Test
    fun privatePost_blocksCrossUserAccess_whilePublicPostAllowsMuralAndReaction() = runBlocking {
        val aliceUid = signInTestUser("alice_post_${UUID.randomUUID()}@atelier.ch")
        val aliceRepo = CommunityRepository(firestore)

        val privPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPostWithMedia(
                authorName = "Chef Alice",
                visibility = "private",
                weekNumber = 17,
                cycleNumber = 3,
                axisId = 4,
                materialType = "prato_final",
                recipeId = "gefluegelgalantine_v4",
                recipeTitle = "Geflügelgalantine",
                isSwissmilkClassic = false,
                isChefkochV4 = true,
                cantonCode = "ZH",
                caption = "Treino privado galantina 65 °C",
                mepSummary = "PCC 65 °C",
                aiFeedback = "",
                thumbnailBase64 = "dGh1bWI=",
                fullImageBase64 = "aW1hZ2U=",
                imageWidth = 1000,
                imageHeight = 800
            ).getOrThrow()
        }

        val pubPostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPostWithMedia(
                authorName = "Chef Alice",
                visibility = "public",
                weekNumber = 10,
                cycleNumber = 2,
                axisId = 3,
                materialType = "cortes_mep",
                recipeId = "berner_platte_v4",
                recipeTitle = "Berner Platte",
                isSwissmilkClassic = true,
                isChefkochV4 = true,
                cantonCode = "BE",
                caption = "Cocções escalonadas 120m / 90m / 60m",
                mepSummary = "FC 1.0 | 82 °C pochar",
                aiFeedback = "",
                thumbnailBase64 = "dGh1bWI=",
                fullImageBase64 = "aW1hZ2U=",
                imageWidth = 1200,
                imageHeight = 900
            ).getOrThrow()
        }

        // Switch to Bob
        val bobUid = signInTestUser("bob_post_${UUID.randomUUID()}@atelier.ch")
        val bobRepo = CommunityRepository(firestore)

        // Bob cannot read Alice's private post or private media
        val privReadResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getPostById(privPostId) }
        assertTrue(privReadResult.isFailure)
        val fsEx = privReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        val privMediaResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getFullMedia(privPostId) }
        assertTrue(privMediaResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            privMediaResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Bob CAN observe public mural and toggle reaction on Alice's public post
        val muralPosts = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePublicMural(bobUid).first { list -> list.any { it.id == pubPostId } }
        }
        val targetPub = muralPosts.first { it.id == pubPostId }
        val reactRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.toggleReaction(targetPub) }
        assertTrue("Expected Bob reaction to succeed: ${reactRes.exceptionOrNull()}", reactRes.isSuccess)

        val commentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.addComment(targetPub, "Chef Bob", "Perfeita separação térmica na Berner Platte!")
        }
        assertTrue("Expected Bob comment to succeed: ${commentRes.exceptionOrNull()}", commentRes.isSuccess)
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CommunityRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicMural("unauth_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated mural listener")
        } catch (e: Exception) {
            val fsException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
