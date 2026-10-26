package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.CanvasLayer
import com.example.data.LayerType
import com.example.data.ShapeKind
import com.example.data.StudioFont
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ThumbnailCanvasRenderer {

    fun parseColorSafe(hex: String, fallback: Int = Color.WHITE): Int {
        if (hex.isBlank()) return fallback
        return try {
            val clean = if (hex.startsWith("#")) hex else "#$hex"
            Color.parseColor(clean)
        } catch (e: Exception) {
            fallback
        }
    }

    fun resolveTypeface(context: Context, fontName: String, bold: Boolean = true): Typeface {
        val fontRes = when (fontName) {
            StudioFont.BEBAS_NEUE.displayName -> R.font.bebas_neue
            StudioFont.OSWALD.displayName -> R.font.oswald
            StudioFont.MONTSERRAT.displayName -> R.font.montserrat
            StudioFont.SPACE_GROTESK.displayName -> R.font.space_grotesk
            StudioFont.PLAYFAIR_DISPLAY.displayName -> R.font.playfair_display
            StudioFont.INTER.displayName -> R.font.inter
            else -> R.font.bebas_neue
        }
        val base = try {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
        return Typeface.create(base, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun buildColorGradingFilter(
        exposure: Int,
        contrast: Int,
        saturation: Int,
        temperature: Int
    ): ColorMatrixColorFilter {
        val cm = ColorMatrix()

        // 1. Saturation (-100 to 100 -> 0f to 2f)
        val satValue = ((saturation + 100f) / 100f).coerceIn(0f, 2.5f)
        cm.setSaturation(satValue)

        // 2. Contrast & Exposure
        val cScale = 1f + (contrast / 100f) * 0.75f
        val expOffset = (exposure / 100f) * 90f
        val translate = (-0.5f * cScale + 0.5f) * 255f + expOffset

        // 3. Temperature (warm adds R/G, reduces B; cool adds B, reduces R)
        val rTemp = 1f + (temperature / 100f) * 0.18f
        val bTemp = 1f - (temperature / 100f) * 0.18f

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                cScale * rTemp, 0f, 0f, 0f, translate,
                0f, cScale, 0f, 0f, translate,
                0f, 0f, cScale * bTemp, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        return ColorMatrixColorFilter(cm)
    }

    /**
     * Renders the complete multi-layer thumbnail composition onto a high-resolution Bitmap
     * at the exact project dimensions (e.g., 1280x720).
     */
    fun renderCompositeBitmap(
        context: Context,
        width: Int,
        height: Int,
        backgroundBitmap: Bitmap?,
        primaryBgHex: String,
        secondaryBgHex: String,
        accentHex: String,
        exposure: Int,
        contrast: Int,
        saturation: Int,
        temperature: Int,
        vignette: Int,
        backgroundBlur: Int,
        layers: List<CanvasLayer>,
        showRuleOfThirdsGrid: Boolean = false
    ): Bitmap {
        val safeW = width.coerceIn(320, 4096)
        val safeH = height.coerceIn(320, 4096)
        val outBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // 1. Render Background
        if (backgroundBitmap != null) {
            val sourceToDraw = if (backgroundBlur > 4) {
                fastBoxScaleBlur(backgroundBitmap, backgroundBlur)
            } else {
                backgroundBitmap
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = buildColorGradingFilter(exposure, contrast, saturation, temperature)
            }
            drawCenterCrop(canvas, sourceToDraw, safeW.toFloat(), safeH.toFloat(), paint)
        } else {
            // Studio dark atmospheric backdrop before image generation or for manual template editing
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, safeW.toFloat(), safeH.toFloat(),
                    intArrayOf(
                        parseColorSafe(primaryBgHex, Color.parseColor("#0B0F19")),
                        parseColorSafe(secondaryBgHex, Color.parseColor("#1E293B")),
                        Color.parseColor("#090D16")
                    ),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                colorFilter = buildColorGradingFilter(exposure, contrast, saturation, temperature)
            }
            canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), bgPaint)

            // Subtle directional studio rim glow on the right third
            val rimColor = parseColorSafe(accentHex, Color.parseColor("#F59E0B"))
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    safeW * 0.75f,
                    safeH * 0.48f,
                    max(safeW, safeH) * 0.45f,
                    Color.argb(65, Color.red(rimColor), Color.green(rimColor), Color.blue(rimColor)),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), glowPaint)
        }

        // 2. Render Vignette + Negative Space Contrast Protection
        if (vignette > 0) {
            val alpha = ((vignette / 100f) * 210f).toInt().coerceIn(0, 235)
            val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    safeW * 0.5f,
                    safeH * 0.5f,
                    max(safeW, safeH) * 0.72f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(alpha / 2, 0, 0, 0), Color.argb(alpha, 0, 0, 0)),
                    floatArrayOf(0.35f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), vigPaint)
        }

        // 3. Render Layers sorted by zIndex
        val scaleFactor = safeW / 1280f
        layers.filter { it.visible }.sortedBy { it.zIndex }.forEach { layer ->
            canvas.save()
            val left = (layer.xPercent / 100f) * safeW
            val top = (layer.yPercent / 100f) * safeH
            val boxW = (layer.widthPercent / 100f) * safeW
            val boxH = (layer.heightPercent / 100f) * safeH
            val centerX = left + boxW / 2f
            val centerY = top + boxH / 2f
            if (layer.rotation != 0f) {
                canvas.rotate(layer.rotation, centerX, centerY)
            }

            val layerAlpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()

            when (layer.type) {
                LayerType.SUBJECT, LayerType.OVERLAY, LayerType.GENERATED_ELEMENT, LayerType.BACKGROUND -> {
                    layer.bitmap?.let { bmp ->
                        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = layerAlpha
                            colorFilter = buildColorGradingFilter(
                                layer.brightness.toInt(),
                                layer.contrast.toInt(),
                                0,
                                0
                            )
                        }
                        val dstRect = RectF(left, top, left + boxW, top + boxH)
                        canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
                    }
                }

                LayerType.SHAPE -> {
                    val fillCol = parseColorSafe(layer.fillColorHex, Color.parseColor("#EF4444"))
                    val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = fillCol
                        alpha = layerAlpha
                        style = Paint.Style.FILL
                    }
                    val rect = RectF(left, top, left + boxW, top + boxH)
                    when (layer.shapeKind) {
                        ShapeKind.RECTANGLE, ShapeKind.BADGE -> {
                            val radius = if (layer.shapeKind == ShapeKind.BADGE) boxH / 2f else 12f * scaleFactor
                            canvas.drawRoundRect(rect, radius, radius, shapePaint)
                        }
                        ShapeKind.CIRCLE -> {
                            canvas.drawOval(rect, shapePaint)
                        }
                        ShapeKind.LINE -> {
                            shapePaint.strokeWidth = max(6f * scaleFactor, boxH * 0.25f)
                            canvas.drawLine(left, centerY, left + boxW, centerY, shapePaint)
                        }
                        ShapeKind.ARROW -> {
                            drawArrowShape(canvas, rect, shapePaint, scaleFactor)
                        }
                    }
                }

                LayerType.TEXT -> {
                    val rawText = layer.text
                    if (rawText.isNotBlank()) {
                        val displayText = if (layer.uppercase) rawText.uppercase() else rawText
                        val tf = resolveTypeface(context, layer.fontName, layer.fontWeightBold)
                        val pxSize = layer.fontSizeSp * 1.45f * scaleFactor

                        val align = when (layer.textAlign) {
                            "CENTER" -> Layout.Alignment.ALIGN_CENTER
                            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                            else -> Layout.Alignment.ALIGN_NORMAL
                        }

                        val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            typeface = tf
                            textSize = pxSize
                            color = parseColorSafe(layer.textColorHex, Color.WHITE)
                            alpha = layerAlpha
                            letterSpacing = (layer.letterSpacing / 40f).coerceIn(-0.05f, 0.4f)
                            if (layer.shadowRadius > 0f) {
                                setShadowLayer(
                                    layer.shadowRadius * scaleFactor,
                                    0f,
                                    4f * scaleFactor,
                                    Color.argb(210, 0, 0, 0)
                                )
                            }
                        }

                        val maxTextW = max(120, boxW.toInt())
                        val staticLayout = StaticLayout.Builder
                            .obtain(displayText, 0, displayText.length, fillPaint, maxTextW)
                            .setAlignment(align)
                            .setLineSpacing(0f, layer.lineHeightMultiplier.coerceIn(0.8f, 1.6f))
                            .setIncludePad(false)
                            .build()

                        // Optional background pill/label behind text
                        if (layer.backgroundBadgeHex.isNotBlank()) {
                            val badgeColor = parseColorSafe(layer.backgroundBadgeHex, Color.parseColor("#EF4444"))
                            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = badgeColor
                                alpha = (layerAlpha * 0.92f).toInt()
                            }
                            var maxLineW = 0f
                            for (i in 0 until staticLayout.lineCount) {
                                maxLineW = max(maxLineW, staticLayout.getLineWidth(i))
                            }
                            val padX = 18f * scaleFactor
                            val padY = 10f * scaleFactor
                            val badgeRect = RectF(
                                left - padX,
                                top - padY,
                                left + maxLineW + padX,
                                top + staticLayout.height + padY
                            )
                            canvas.drawRoundRect(badgeRect, 10f * scaleFactor, 10f * scaleFactor, badgePaint)
                        }

                        canvas.translate(left, top)

                        // Draw crisp outline stroke first if enabled
                        if (layer.outlineWidth > 0f) {
                            val strokePaint = TextPaint(fillPaint).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = layer.outlineWidth * 1.6f * scaleFactor
                                strokeJoin = Paint.Join.ROUND
                                color = parseColorSafe(layer.outlineColorHex, Color.BLACK)
                                alpha = layerAlpha
                                clearShadowLayer()
                            }
                            val strokeLayout = StaticLayout.Builder
                                .obtain(displayText, 0, displayText.length, strokePaint, maxTextW)
                                .setAlignment(align)
                                .setLineSpacing(0f, layer.lineHeightMultiplier.coerceIn(0.8f, 1.6f))
                                .setIncludePad(false)
                                .build()
                            strokeLayout.draw(canvas)
                        }

                        staticLayout.draw(canvas)
                    }
                }
            }
            canvas.restore()
        }

        // 4. Optional Rule-of-Thirds Composition Guide Overlay
        if (showRuleOfThirdsGrid) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(110, 56, 189, 248)
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(safeW / 3f, 0f, safeW / 3f, safeH.toFloat(), gridPaint)
            canvas.drawLine(safeW * 2f / 3f, 0f, safeW * 2f / 3f, safeH.toFloat(), gridPaint)
            canvas.drawLine(0f, safeH / 3f, safeW.toFloat(), safeH / 3f, gridPaint)
            canvas.drawLine(0f, safeH * 2f / 3f, safeW.toFloat(), safeH * 2f / 3f, gridPaint)
        }

        return outBitmap
    }

    private fun drawArrowShape(canvas: Canvas, rect: RectF, paint: Paint, scale: Float) {
        val path = Path()
        val cy = rect.centerY()
        val headW = min(rect.width() * 0.38f, rect.height() * 0.9f)
        val shaftHalfH = max(4f * scale, rect.height() * 0.22f)
        path.moveTo(rect.left, cy - shaftHalfH)
        path.lineTo(rect.right - headW, cy - shaftHalfH)
        path.lineTo(rect.right - headW, rect.top)
        path.lineTo(rect.right, cy)
        path.lineTo(rect.right - headW, rect.bottom)
        path.lineTo(rect.right - headW, cy + shaftHalfH)
        path.lineTo(rect.left, cy + shaftHalfH)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawCenterCrop(canvas: Canvas, source: Bitmap, dstW: Float, dstH: Float, paint: Paint) {
        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)
        val scale = max(dstW / srcW, dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val dx = (dstW - scaledW) / 2f
        val dy = (dstH - scaledH) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(source, matrix, paint)
    }

    private fun fastBoxScaleBlur(source: Bitmap, intensity: Int): Bitmap {
        val downscale = (1 + (intensity / 8)).coerceIn(2, 14)
        val smallW = (source.width / downscale).coerceAtLeast(16)
        val smallH = (source.height / downscale).coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, true)
    }

    fun saveBitmapToInternalFile(context: Context, bitmap: Bitmap, prefix: String = "thumb"): String {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun exportToGalleryAndShare(
        context: Context,
        bitmap: Bitmap,
        projectTitle: String
    ): Result<String> {
        return try {
            val cleanName = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "Thumbnail" }
            val fileName = "${cleanName}_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ThumbnailAIStudio")
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Could not create MediaStore entry.")

            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, projectTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Export High-Res Thumbnail").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            Result.success("Saved $fileName (${bitmap.width}×${bitmap.height} PNG) to Gallery")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
