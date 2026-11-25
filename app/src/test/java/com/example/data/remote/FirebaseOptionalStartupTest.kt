package com.example.data.remote

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.ChefAiRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sem `google-services.json` o FirebaseApp não existe. O app precisa abrir mesmo assim
 * (modo local): criar os repositórios na abertura não pode lançar exceção.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseOptionalStartupTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun repositoriesCanBeCreatedWithoutFirebaseConfig() {
        assertTrue("pré-condição: nenhum FirebaseApp configurado", FirebaseApp.getApps(app).isEmpty())
        CommunityRepository(app)
        ChefAiRepository()
    }

    @Test
    fun chefTutor_withoutFirebase_failsWithFriendlyMessage() = runTest {
        val error = runCatching { ChefAiRepository().streamChefTutorGuidance("Teste", "contexto").toList() }
            .exceptionOrNull()
        assertTrue("deveria falhar sem Firebase", error != null)
        assertTrue(error!!.message.orEmpty(), error.message.orEmpty().contains("Chef IA indisponível"))
    }
}
