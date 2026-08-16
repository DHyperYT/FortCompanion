package com.dhyper.fncompanion.ui.utils

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.dhyper.fncompanion.data.models.ParsedLockerItem
import com.dhyper.fncompanion.ui.components.getRarityColor
import com.dhyper.fncompanion.ui.components.getRarityRank
import com.dhyper.fncompanion.ui.theme.SleekSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LockerImageGenerator {

    private const val CARD_SIZE = 400
    private const val MARGIN = 15
    private const val MAX_IMAGE_DIMENSION = 8192 // Safe limit for most Android devices

    suspend fun generateLockerImage(
        context: Context,
        items: List<ParsedLockerItem>,
        title: String,
        isPreview: Boolean = false,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (items.isEmpty()) return@withContext null

        val sortedItems = items.sortedWith(
            compareByDescending<ParsedLockerItem> { getRarityRank(it.rarity) }
            .thenBy { it.rarity }
            .thenBy { it.name }
        )
        val count = sortedItems.size
        
        // Dynamic scaling: Preview is small to keep it fast, Export is full-res
        val scale = if (isPreview) 0.15f else 1.0f
        
        // If we have a ton of items, adjust base card size for memory safety
        val baseCardSize = when {
            count > 600 -> 180
            count > 300 -> 250
            else -> CARD_SIZE
        }

        val cardSize = (baseCardSize * scale).toInt()
        val margin = (MARGIN * scale).toInt()
        val headerHeight = (140 * scale).toInt()
        
        val sideCount = kotlin.math.ceil(kotlin.math.sqrt(count.toDouble())).toInt().coerceAtLeast(3)
        val cols = sideCount
        val rows = (count + cols - 1) / cols
        
        var width = sideCount * (cardSize + margin) + margin
        var height = rows * (cardSize + margin) + margin + headerHeight
        
        // Force 1:1 Square
        val finalSide = Math.max(width, height).coerceAtMost(MAX_IMAGE_DIMENSION)
        width = finalSide
        height = finalSide

        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            try {
                Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888)
            } catch (e2: Exception) {
                return@withContext null
            }
        } ?: return@withContext null

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#090A10"))

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title.uppercase(), margin.toFloat() + (20 * scale), 90f * scale, headerPaint)

        val imageLoader = ImageLoader(context)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val cardBgColor = SleekSurfaceVariant.toArgb()

        sortedItems.forEachIndexed { index, item ->
            val col = index % cols
            val row = index / cols
            
            val left = col * (cardSize + margin) + margin
            val top = row * (cardSize + margin) + margin + headerHeight
            val right = left + cardSize
            val bottom = top + cardSize
            
            val cardRect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBgColor }
            canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, bgPaint)
            
            val rarityColor = getRarityColor(item.rarity)
            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, top.toFloat(), 0f, bottom.toFloat(),
                    rarityColor.copy(alpha = 0.55f).toArgb(), // Slightly more alpha for bitmap vibrancy
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, gradientPaint)
            
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = rarityColor.copy(alpha = 0.7f).toArgb() // Slightly more alpha for bitmap clarity
                style = Paint.Style.STROKE
                strokeWidth = 4f * scale
            }
            canvas.drawRoundRect(cardRect, 24f * scale, 24f * scale, borderPaint)

            if (!item.iconUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(item.iconUrl)
                    .allowHardware(false) 
                    .size(cardSize) 
                    .build()
                
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val iconBitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                    
                    val padding = (6 * scale).toInt()
                    val iconDest = Rect(
                        left + padding, 
                        top + padding, 
                        right - padding, 
                        bottom - padding
                    )
                    drawBitmapCentered(canvas, iconBitmap, iconDest, paint)
                }
            }

            val textBgPaint = Paint().apply {
                color = Color.BLACK
                alpha = 180
            }
            canvas.drawRect(left.toFloat() + 2, (bottom - (65 * scale)), right.toFloat() - 2, bottom.toFloat() - 2, textBgPaint)

            val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            
            val textX = left + cardSize / 2f
            val textY = bottom - (35 * scale)
            
            val maxWidth = cardSize - 20
            val staticLayout = StaticLayout.Builder.obtain(item.name, 0, item.name.length, namePaint, maxWidth.coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(1)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            
            canvas.save()
            canvas.translate(textX, textY)
            staticLayout.draw(canvas)
            canvas.restore()

            onProgress?.invoke((index + 1).toFloat() / count)
        }

        return@withContext bitmap
    }

    private fun drawBitmapCentered(canvas: Canvas, bitmap: Bitmap, targetRect: Rect, paint: Paint) {
        val bWidth = bitmap.width.toFloat()
        val bHeight = bitmap.height.toFloat()
        val tWidth = targetRect.width().toFloat()
        val tHeight = targetRect.height().toFloat()

        val scale = Math.min(tWidth / bWidth, tHeight / bHeight)
        val dx = (tWidth - bWidth * scale) / 2f
        val dy = (tHeight - bHeight * scale) / 2f

        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(targetRect.left + dx, targetRect.top + dy)

        canvas.drawBitmap(bitmap, matrix, paint)
    }
}
