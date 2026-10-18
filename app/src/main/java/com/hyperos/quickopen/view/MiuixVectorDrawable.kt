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

/**
 * 将 Compose Miuix [ImageVector] 真实矢量指令转为 Android 平台 [Drawable]，
 * 可直接用于 SystemUI View 视图层（如 ImageView / CircularBubbleView）。
 */
class MiuixVectorDrawable(
    val vector: ImageVector,
    tintColor: Int,
) : Drawable() {

    private data class PathElement(
        val path: android.graphics.Path,
        val paint: Paint,
    )

    private val elements = mutableListOf<PathElement>()
    private val renderColor: Int = blendWithWhite(if (tintColor == 0) 0xFFFFFFFF.toInt() else tintColor, 0.18f)

    init {
        fun traverse(group: VectorGroup) {
            for (node in group) {
                when (node) {
                    is VectorPath -> {
                        val p = node.pathData.toPath().asAndroidPath()
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = renderColor
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

                        elements.add(PathElement(p, paint))
                    }
                    is VectorGroup -> traverse(node)
                }
            }
        }
        traverse(vector.root)
    }

    override fun draw(canvas: Canvas) {
        val b: Rect = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        val vw = if (vector.viewportWidth > 0f) vector.viewportWidth else 24f
        val vh = if (vector.viewportHeight > 0f) vector.viewportHeight else 24f
        val scale = Math.min(b.width() / vw, b.height() / vh) * 0.82f

        canvas.save()
        canvas.translate(b.centerX() - (vw / 2f) * scale, b.centerY() - (vh / 2f) * scale)
        canvas.scale(scale, scale)

        for (elem in elements) {
            canvas.drawPath(elem.path, elem.paint)
        }

        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        for (elem in elements) {
            elem.paint.alpha = alpha
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        for (elem in elements) {
            elem.paint.colorFilter = colorFilter
        }
    }

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
