package com.example.data.curriculum

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ponto único de acesso aos arquivos de conteúdo do curso (`public/assets/aistudio`, empacotados
 * como assets Android).
 *
 * O app chama [preload] em [Dispatchers.IO] antes de montar a UI; a partir daí os catálogos
 * síncronos ([CourseJsonDatasetLoader.dataset], [DidacticLibraryLoader.catalog] e as fachadas que
 * dependem deles) já estão em memória e nunca fazem I/O ou parse de JSON na main thread.
 */
object CourseContent {

    /** Pastas usadas fora do Android (testes JVM/Robolectric sem [install], ambiente do AI Studio). */
    private val FALLBACK_DIRS = listOf(
        "public/assets/aistudio",
        "../public/assets/aistudio",
        "/app/applet/public/assets/aistudio"
    )

    @Volatile
    private var assets: AssetManager? = null

    @Volatile
    var isLoaded: Boolean = false
        private set

    fun install(context: Context) {
        if (assets == null) {
            assets = context.applicationContext.assets
        }
    }

    /** Lê e parseia todo o conteúdo do curso fora da main thread. Idempotente. */
    suspend fun preload(context: Context) {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            install(context)
            CourseJsonDatasetLoader.dataset
            DidacticLibraryLoader.catalog
        }
        isLoaded = true
    }

    internal fun readText(fileName: String): String {
        assets?.let { am ->
            runCatching { am.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .getOrNull()
                ?.let { return it }
        }
        FALLBACK_DIRS.map { File(it, fileName) }.firstOrNull { it.exists() }?.let {
            return it.readText(Charsets.UTF_8)
        }
        error("Conteúdo do curso '$fileName' não encontrado nos assets do app nem em public/assets/aistudio.")
    }
}
