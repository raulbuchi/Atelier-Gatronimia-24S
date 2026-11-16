package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CompressedMediaPayload(
    val fullBitmap: Bitmap,
    val fullBase64: String,
    val thumbnailBase64: String,
    val width: Int,
    val height: Int
)

object MediaCompressor {

    /**
     * Decodes a photo URI, strips all EXIF metadata by re-encoding raw pixel buffers,
     * downscales to max 1200px at JPEG quality 82 (matching v4 mediaUpload.ts spec),
     * and creates a compact 320px thumbnail for low-cost Firestore feed queries.
     */
    suspend fun compressFromUri(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1200,
        thumbnailMaxPx: Int = 320
    ): Result<CompressedMediaPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            require(boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                "Não foi possível decodificar a imagem selecionada."
            }

            var sampleSize = 1
            val largestSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
            while (largestSide / (sampleSize * 2) >= maxDimensionPx) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: error("Falha ao abrir fluxo da imagem.")

            val fullScaled = scaleDownIfNeeded(rawBitmap, maxDimensionPx)
            val thumbScaled = scaleDownIfNeeded(fullScaled, thumbnailMaxPx)

            // Re-encoding Bitmap pixels into fresh JPEG byte stream drops all EXIF tags (GPS, device)
            val fullBytes = encodeJpegUnderCeiling(fullScaled, initialQuality = 82, maxBytes = 380_000)
            val thumbBytes = encodeJpegUnderCeiling(thumbScaled, initialQuality = 72, maxBytes = 42_000)

            CompressedMediaPayload(
                fullBitmap = fullScaled,
                fullBase64 = Base64.encodeToString(fullBytes, Base64.NO_WRAP),
                thumbnailBase64 = Base64.encodeToString(thumbBytes, Base64.NO_WRAP),
                width = fullScaled.width,
                height = fullScaled.height
            )
        }
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun scaleForAiInference(bitmap: Bitmap, maxPx: Int = 1024): Bitmap {
        return scaleDownIfNeeded(bitmap, maxPx)
    }

    private fun scaleDownIfNeeded(source: Bitmap, maxPx: Int): Bitmap {
        val maxSide = max(source.width, source.height)
        if (maxSide <= maxPx) return source
        val ratio = maxPx.toFloat() / maxSide.toFloat()
        val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun encodeJpegUnderCeiling(
        bitmap: Bitmap,
        initialQuality: Int,
        maxBytes: Int
    ): ByteArray {
        var quality = initialQuality
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality >= 40)
        return bytes
    }
}
