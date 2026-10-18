package com.hyperos.quickopen.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import com.hyperos.quickopen.config.QuickOpenConfig

/**
 * 将 Compose Miuix [ImageVector] 真实矢量指令转为 Android 平台 [Drawable]，
 * 支持圆形纯色/微透/反色衬底渲染与呼吸辉光。
 */
class MiuixVectorDrawable(
    val vector: ImageVector,
    tintColor: Int,
    private val padEnabled: Boolean = false,
    private val padColor: Int = 0xFF2A82E4.toInt(),
    private val padMode: Int = QuickOpenConfig.GLYPH_PAD_MODE_TRANSLUCENT,
) : Drawable() {

    private data class PathElement(
        val path: android.graphics.Path,
        val paint: Paint,
    )

    private val elements = mutableListOf<PathElement>()
    private val padPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val padBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val totalBounds = android.graphics.RectF()

    // 图标前景色
    private val iconColor: Int = when {
        !padEnabled -> blendWithWhite(if (tintColor == 0) 0xFFFFFFFF.toInt() else tintColor, 0.18f)
        padMode == QuickOpenConfig.GLYPH_PAD_MODE_INVERTED -> if (tintColor != 0) tintColor else padColor
        else -> 0xFFFFFFFF.toInt() // 饱满底色或微透底色上始终使用高清晰纯白
    }

    init {
        val totalCombinedPath = android.graphics.Path()

        if (padEnabled) {
            val baseColor = if (padColor != 0) padColor else tintColor
            padPaint.style = Paint.Style.FILL
            padPaint.color = when (padMode) {
                QuickOpenConfig.GLYPH_PAD_MODE_SOLID -> Color.rgb(Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                QuickOpenConfig.GLYPH_PAD_MODE_INVERTED -> Color.argb(48, 255, 255, 255)
                else -> Color.argb(165, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)) // 微透
            }

            padBorderPaint.style = Paint.Style.STROKE
            padBorderPaint.strokeWidth = 2.5f
            padBorderPaint.color = Color.argb(45, 255, 255, 255)
        }

        fun traverse(group: VectorGroup, parentMatrix: android.graphics.Matrix) {
            val groupMatrix = android.graphics.Matrix(parentMatrix).apply {
                preTranslate(group.translationX, group.translationY)
                preTranslate(group.pivotX, group.pivotY)
                preRotate(group.rotation)
                preScale(group.scaleX, group.scaleY)
                preTranslate(-group.pivotX, -group.pivotY)
            }

            for (node in group) {
                when (node) {
                    is VectorPath -> {
                        val originalPath = node.pathData.toPath().asAndroidPath()
                        val transformedPath = android.graphics.Path()
                        originalPath.transform(groupMatrix, transformedPath)
                        totalCombinedPath.addPath(transformedPath)

                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = iconColor
                        }

                        if (node.stroke != null) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = if (node.strokeLineWidth > 0f) node.strokeLineWidth else 1.75f
                            paint.strokeCap = when (node.strokeLineCap) {
                                StrokeCap.Round -> Paint.Cap.ROUND
                                StrokeCap.Square -> Paint.Cap.SQUARE
                                else -> Paint.Cap.BUTT
                            }
                            paint.strokeJoin = when (node.strokeLineJoin) {
                                StrokeJoin.Round -> Paint.Join.ROUND
                                StrokeJoin.Bevel -> Paint.Join.BEVEL
                                else -> Paint.Join.MITER
                            }
                            paint.strokeMiter = node.strokeLineMiter
                        } else {
                            paint.style = Paint.Style.FILL
                        }

                        elements.add(PathElement(transformedPath, paint))
                    }
                    is VectorGroup -> traverse(node, groupMatrix)
                }
            }
        }
        traverse(vector.root, android.graphics.Matrix())
        totalCombinedPath.computeBounds(totalBounds, true)
    }

    override fun draw(canvas: Canvas) {
        val b: Rect = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        // 1) 绘制圆形底垫（如启用）
        if (padEnabled) {
            val radius = Math.min(b.width(), b.height()) / 2f
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), radius, padPaint)
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), radius - 1.25f, padBorderPaint)
        }

        // 2) 绘制 Miuix 矢量线条
        // 自动计算内容实际外接矩形或声明的 viewport，实现精准自适应居中与缩放
        val hasBounds = totalBounds.width() > 0f && totalBounds.height() > 0f
        val contentW = if (hasBounds) totalBounds.width() else (if (vector.viewportWidth > 0f) vector.viewportWidth else 24f)
        val contentH = if (hasBounds) totalBounds.height() else (if (vector.viewportHeight > 0f) vector.viewportHeight else 24f)
        val contentCenterX = if (hasBounds) totalBounds.centerX() else (if (vector.viewportWidth > 0f) vector.viewportWidth / 2f else 12f)
        val contentCenterY = if (hasBounds) totalBounds.centerY() else (if (vector.viewportHeight > 0f) vector.viewportHeight / 2f else 12f)

        val contentScaleFactor = if (padEnabled) 0.58f else 0.82f
        val scale = Math.min(b.width() / contentW, b.height() / contentH) * contentScaleFactor

        canvas.save()
        canvas.translate(b.exactCenterX(), b.exactCenterY())
        canvas.scale(scale, scale)
        canvas.translate(-contentCenterX, -contentCenterY)

        for (elem in elements) {
            canvas.drawPath(elem.path, elem.paint)
        }

        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        for (elem in elements) {
            elem.paint.alpha = alpha
        }
        padPaint.alpha = alpha
        padBorderPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        for (elem in elements) {
            elem.paint.colorFilter = colorFilter
        }
    }

    override fun getIntrinsicWidth(): Int = (if (vector.viewportWidth > 0f) vector.viewportWidth else 24f).toInt() * 4
    override fun getIntrinsicHeight(): Int = (if (vector.viewportHeight > 0f) vector.viewportHeight else 24f).toInt() * 4

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private fun blendWithWhite(color: Int, ratio: Float): Int {
            val r = ratio.coerceIn(0f, 1f)
            val a = Color.alpha(color)
            val cr = Math.round(Color.red(color) * (1 - r) + 255 * r)
            val cg = Math.round(Color.green(color) * (1 - r) + 255 * r)
            val cb = Math.round(Color.blue(color) * (1 - r) + 255 * r)
            return Color.argb(a, cr, cg, cb)
        }
    }
}
