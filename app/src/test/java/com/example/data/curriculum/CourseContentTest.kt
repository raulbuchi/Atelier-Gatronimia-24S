package com.example.data.curriculum

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CourseContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun preload_readsCourseContentFromAssets() = runTest {
        CourseContent.preload(context)

        assertTrue(CourseContent.isLoaded)
        assertEquals(24, CourseJsonDatasetLoader.dataset.weeks.size)
        assertEquals(45, CourseJsonDatasetLoader.dataset.recipes.size)
        assertEquals(60, CourseJsonDatasetLoader.dataset.flashcards.size)
        assertEquals(6, DidacticLibraryLoader.catalog.axes.size)
    }

    /** Guarda contra regressões de empacotamento da pasta de uploads do AI Studio. */
    @Test
    fun packagedAssets_containOnlyRuntimeContent() {
        val rootAssets = context.assets.list("").orEmpty().toList()

        assertTrue("curso-24-semanas-dados-v4_3370cebc.json" in rootAssets)
        assertTrue("brochuras-didaticas-app_0f609061.json" in rootAssets)
        assertEquals(5, rootAssets.count { it.endsWith(".m4a") })

        // Robolectric também lista assets do framework Android; verifique só os uploads do AI Studio.
        val unwanted = rootAssets.filter {
            it.startsWith("ai-studio-") || it.startsWith("mediaUpload") ||
                it.startsWith("atelier_gastronomia_24s_") || it.contains("__1__") || it.startsWith("eixo-")
        }
        assertTrue("Assets não usados empacotados: $unwanted", unwanted.isEmpty())
    }

    /** Conteúdo não pode voltar a ser recurso Java (duplicaria os ~67 MB de áudio no APK). */
    @Test
    fun courseContent_isNotPackagedAsJavaResource() {
        val loader = CourseContent::class.java.classLoader!!
        assertEquals(null, loader.getResource("curso-24-semanas-dados-v4_3370cebc.json"))
        assertEquals(null, loader.getResource("episodio_01_ciclo_1_2a9ef7b8.m4a"))
    }
}
