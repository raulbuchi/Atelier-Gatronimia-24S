package com.example.core.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareableCardGenerator {

    /**
     * Renders a 1080x1350 Swiss Precision shareable PNG card containing the Atelier Gastronomia 24S
     * emblem, dish/training photo, week/canton badges, MEP PCC summary, and Chef IA stamp,
     * then opens Android's native Share Sheet via FileProvider.
     */
    suspend fun exportAndShareCard(
        context: Context,
        recipeTitle: String,
        authorName: String,
        weekNumber: Int,
        cantonCode: String,
        materialType: String,
        caption: String,
        mepSummary: String,
        aiFeedback: String,
        photoBitmap: Bitmap?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val height = 1350
            val cardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(cardBitmap)

            // 1. Background (#F4F6F8) & Outer Frame (#C8CED6)
            canvas.drawColor(Color.parseColor("#F4F6F8"))
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = Color.parseColor("#C8CED6")
            }
            canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

            // 2. Top Brand Header Bar (#272E36 Charcoal Steel + Official Compass-Knife Icon)
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#272E36")
            }
            canvas.drawRoundRect(RectF(48f, 48f, width - 48f, 192f), 20f, 20f, headerPaint)

            val iconBmp = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.example.R.drawable.brand_icon_emblem
            )
            if (iconBmp != null) {
                val dstIcon = Rect(68, 66, 172, 170)
                canvas.drawBitmap(iconBmp, null, dstIcon, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#AE2127")
                }
                canvas.drawRoundRect(RectF(72f, 74f, 158f, 160f), 14f, 14f, redPaint)
            }

            val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("ATELIER GASTRONOMIA", 192f, 112f, brandPaint)

            // Official 24S Carmine Badge (#AE2127)
            val badge24Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#AE2127")
            }
            canvas.drawRoundRect(RectF(636f, 78f, 742f, 120f), 8f, 8f, badge24Paint)
            val badge24TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("24S", 689f, 109f, badge24TextPaint)

            val subBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C8CED6")
                textSize = 24f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val cantonBadge = if (cantonCode.isNotBlank()) " • CANTÃO $cantonCode" else ""
            canvas.drawText(
                "SEMANA $weekNumber$cantonBadge • ${materialType.uppercase()}",
                192f,
                154f,
                subBrandPaint
            )

            // 3. Photo Frame (y = 216..796)
            val photoRect = RectF(48f, 216f, width - 48f, 796f)
            val frameBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E3E7EC")
            }
            canvas.drawRoundRect(photoRect, 20f, 20f, frameBgPaint)

            if (photoBitmap != null) {
                val srcRatio = photoBitmap.width.toFloat() / photoBitmap.height.toFloat()
                val dstRatio = photoRect.width() / photoRect.height()
                val srcRect = if (srcRatio > dstRatio) {
                    val cropW = (photoBitmap.height * dstRatio).toInt()
                    val left = (photoBitmap.width - cropW) / 2
                    Rect(left, 0, left + cropW, photoBitmap.height)
                } else {
                    val cropH = (photoBitmap.width / dstRatio).toInt()
                    val top = (photoBitmap.height - cropH) / 2
                    Rect(0, top, photoBitmap.width, top + cropH)
                }
                val dstIntRect = Rect(
                    photoRect.left.toInt(),
                    photoRect.top.toInt(),
                    photoRect.right.toInt(),
                    photoRect.bottom.toInt()
                )
                canvas.drawBitmap(photoBitmap, srcRect, dstIntRect, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                val placeholderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#47515D")
                    textSize = 32f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText(
                    "REGISTRO TÉCNICO MEP • BANCADA SUÍÇA",
                    width / 2f,
                    photoRect.centerY(),
                    placeholderPaint
                )
            }

            // 4. Title & Author Metadata
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#272E36")
                textSize = 44f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(recipeTitle.take(36), 56f, 864f, titlePaint)

            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#AE2127")
                textSize = 26f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("EXECUTADO POR: ${authorName.uppercase()}", 56f, 906f, authorPaint)

            // 5. Caption & MEP / Chef IA Summary Box
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#343D48")
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val combinedNotes = buildString {
                append(caption.trim().take(180))
                if (mepSummary.isNotBlank()) {
                    append("\nMEP/PCC: ").append(mepSummary.trim().take(120))
                }
                if (aiFeedback.isNotBlank()) {
                    append("\nChef IA: ").append(aiFeedback.replace("\n", " ").trim().take(160))
                }
            }

            val staticLayout = StaticLayout.Builder
                .obtain(combinedNotes, 0, combinedNotes.length, bodyPaint, width - 112)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(8)
                .build()

            canvas.save()
            canvas.translate(56f, 940f)
            staticLayout.draw(canvas)
            canvas.restore()

            // Footer Stamp
            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5C6570")
                textSize = 22f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            canvas.drawText(
                "PRECISÃO SUÍÇA • CHEFKOCH STEP-BY-STEP • SWISSMILK • LE CORDON BLEU",
                56f,
                1298f,
                footerPaint
            )

            // Save to cacheDir/shared_cards/
            val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(dir, "atelier_24s_card_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "$recipeTitle — Semana $weekNumber no Atelier Gastronomia 24S"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Compartilhar Card PNG").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }
}
