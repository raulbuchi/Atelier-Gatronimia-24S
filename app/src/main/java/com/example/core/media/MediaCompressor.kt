package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class CompressedMediaPayload(
    val fullBitmap: Bitmap,
    val fullBase64: String,
    val thumbnailBase64: String,
    val width: Int,
    val height: Int,
    val localUriString: String = "",
    val originalSizeBytes: Long = 0L,
    val compressedSizeBytes: Int = 0,
    val savingsPercent: Int = 0
)

data class CompressedAvatarPayload(
    val avatarBitmap: Bitmap,
    val avatarBase64: String,
    val width: Int,
    val height: Int,
    val compressedSizeBytes: Int,
    val savingsPercent: Int
)

object MediaCompressor {

    const val MAX_INPUT_BYTES: Long = 10L * 1024L * 1024L // 10 MB (mediaUpload.ts parity)
    const val POST_MAX_DIMENSION_PX: Int = 1200
    const val AVATAR_MAX_DIMENSION_PX: Int = 400

    private val ACCEPTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "${kb.roundToInt()} KB"
        return "%.2f MB".format(kb / 1024.0)
    }

    /**
     * Creates a dedicated File + FileProvider Uri inside filesDir/mep_creations/
     * for CameraX ImageCapture output associated with a culinary Ficha Técnica.
     */
    fun createMepCreationPhotoFile(
        context: Context,
        recipeId: String
    ): Pair<File, Uri> {
        val safeSlug = recipeId
            .lowercase()
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .take(36)
            .ifBlank { "ficha_mep" }
        val creationsDir = File(context.filesDir, "mep_creations").apply { mkdirs() }
        val photoFile = File(creationsDir, "mep_${safeSlug}_${System.currentTimeMillis()}.jpg")
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        return photoFile to contentUri
    }

    /**
     * Validates input size (<= 10 MB) and MIME type (JPEG/PNG/WebP), decodes with EXIF orientation,
     * composites onto a solid #FFFFFF white canvas (preventing black areas on transparent PNG/WebP),
     * strips all EXIF tags, downscales to <= 1200px JPEG quality 82, and computes compression savings.
     */
    suspend fun compressFromUri(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = POST_MAX_DIMENSION_PX,
        thumbnailMaxPx: Int = 320,
        maxInputBytes: Long = MAX_INPUT_BYTES
    ): Result<CompressedMediaPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val originalBytes = validateAndMeasureInput(context, uri, maxInputBytes)
            val rawBitmap = decodeOrientedBitmap(context, uri, maxDimensionPx)
            val fullScaled = compositeOnWhiteCanvas(scaleDownIfNeeded(rawBitmap, maxDimensionPx))
            val thumbScaled = compositeOnWhiteCanvas(scaleDownIfNeeded(fullScaled, thumbnailMaxPx))

            // Re-encoding Bitmap pixels into fresh JPEG byte stream drops all EXIF tags (GPS, device)
            val fullBytes = encodeJpegUnderCeiling(fullScaled, initialQuality = 82, maxBytes = 380_000)
            val thumbBytes = encodeJpegUnderCeiling(thumbScaled, initialQuality = 72, maxBytes = 42_000)

            val savingsPct = if (originalBytes > 0L) {
                (((originalBytes - fullBytes.size).toDouble() / originalBytes.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceAtLeast(0)
            } else {
                0
            }

            CompressedMediaPayload(
                fullBitmap = fullScaled,
                fullBase64 = Base64.encodeToString(fullBytes, Base64.NO_WRAP),
                thumbnailBase64 = Base64.encodeToString(thumbBytes, Base64.NO_WRAP),
                width = fullScaled.width,
                height = fullScaled.height,
                localUriString = uri.toString(),
                originalSizeBytes = originalBytes,
                compressedSizeBytes = fullBytes.size,
                savingsPercent = savingsPct
            )
        }
    }

    /**
     * Compresses a user profile avatar to <= 400px JPEG quality 80 on #FFFFFF white background
     * (mediaUpload.ts uploadProfileAvatar parity, capped under 38 KB for Firestore profile documents).
     */
    suspend fun compressAvatarFromUri(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = AVATAR_MAX_DIMENSION_PX,
        maxInputBytes: Long = MAX_INPUT_BYTES
    ): Result<CompressedAvatarPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val originalBytes = validateAndMeasureInput(context, uri, maxInputBytes)
            val rawBitmap = decodeOrientedBitmap(context, uri, maxDimensionPx)
            val avatarScaled = compositeOnWhiteCanvas(scaleDownIfNeeded(rawBitmap, maxDimensionPx))
            val avatarBytes = encodeJpegUnderCeiling(avatarScaled, initialQuality = 80, maxBytes = 38_000)

            val savingsPct = if (originalBytes > 0L) {
                (((originalBytes - avatarBytes.size).toDouble() / originalBytes.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceAtLeast(0)
            } else {
                0
            }

            CompressedAvatarPayload(
                avatarBitmap = avatarScaled,
                avatarBase64 = Base64.encodeToString(avatarBytes, Base64.NO_WRAP),
                width = avatarScaled.width,
                height = avatarScaled.height,
                compressedSizeBytes = avatarBytes.size,
                savingsPercent = savingsPct
            )
        }
    }

    /**
     * Composites any Bitmap (including alpha-channel PNG/WebP) onto an opaque #FFFFFF canvas
     * so JPEG encoding never renders transparent areas as solid black.
     */
    fun compositeOnWhiteCanvas(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            source.width.coerceAtLeast(1),
            source.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        ).apply {
            eraseColor(Color.WHITE)
        }
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun validateAndMeasureInput(
        context: Context,
        uri: Uri,
        maxInputBytes: Long
    ): Long {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase()?.trim()
        if (!mime.isNullOrBlank() && mime !in ACCEPTED_MIME_TYPES && !mime.startsWith("image/")) {
            throw IllegalArgumentException("Formato não suportado ($mime). Envie uma imagem JPEG, PNG ou WebP.")
        }

        var byteLength = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        byteLength = cursor.getLong(idx)
                    }
                }
            }
        }
        if (byteLength <= 0L && uri.scheme == "file") {
            byteLength = uri.path?.let { File(it).length() } ?: 0L
        }
        if (byteLength > maxInputBytes) {
            throw IllegalArgumentException(
                "A imagem excede o limite máximo de ${formatBytes(maxInputBytes)}."
            )
        }
        return byteLength
    }

    private fun decodeOrientedBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int
    ): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

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

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("Falha ao abrir fluxo da imagem.")

        return applyExifRotation(decoded, orientation)
    }

    private fun applyExifRotation(source: Bitmap, exifOrientation: Int): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
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
