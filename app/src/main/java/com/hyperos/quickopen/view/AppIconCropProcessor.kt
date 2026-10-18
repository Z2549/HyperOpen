package com.hyperos.quickopen.view

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.hyperos.quickopen.config.QuickOpenConfig

/**
 * 应用图标圆形化与自适应外扩满铺裁切处理器
 */
object AppIconCropProcessor {

    private const val BITMAP_SIZE = 192

    /**
     * 根据全局裁切模式对应用图标进行圆形处理
     */
    @JvmStatic
    fun process(src: Drawable?, cropMode: Int): Drawable? {
        if (src == null) return null

        return try {
            when (cropMode) {
                QuickOpenConfig.CROP_BITMAP_CIRCLE -> createStandardCircle(src)
                QuickOpenConfig.CROP_ADAPTIVE_EXPAND -> createAdaptiveExpandedCircle(src)
                else -> createAdaptiveExpandedCircle(src)
            }
        } catch (t: Throwable) {
            src
        }
    }

    /**
     * 标准抗锯齿圆形裁切 (BitmapShader)
     */
    private fun createStandardCircle(drawable: Drawable): Drawable {
        val size = BITMAP_SIZE
        val srcBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(srcCanvas)

        val outBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(outBmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(srcBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        outCanvas.drawCircle(radius, radius, radius, paint)

        // 添加一层超细微半透明边缘保护线，防止在浅色/白底壁纸下边缘发虚
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(28, 255, 255, 255)
        }
        outCanvas.drawCircle(radius, radius, radius - 1f, strokePaint)

        return BitmapDrawable(null, outBmp)
    }

    /**
     * 自适应留白外扩并圆形满铺裁切 (专为 AdaptiveIcon 和带白底的应用打造)
     */
    private fun createAdaptiveExpandedCircle(drawable: Drawable): Drawable {
        val size = BITMAP_SIZE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            // AdaptiveIconDrawable 规范自带 25% 安全边距 (四周各保留 1/6)
            // 外扩 1.333 倍绘制后切出中心正方再裁圆，核心图标图案正好铺满正圆
            val scale = 1.3333f
            val drawSize = (size * scale).toInt()
            val offset = (drawSize - size) / 2

            val drawBmp = Bitmap.createBitmap(drawSize, drawSize, Bitmap.Config.ARGB_8888)
            val drawCanvas = Canvas(drawBmp)
            drawable.setBounds(0, 0, drawSize, drawSize)
            drawable.draw(drawCanvas)

            val croppedBmp = Bitmap.createBitmap(drawBmp, offset, offset, size, size)
            val outBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val outCanvas = Canvas(outBmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(croppedBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            val radius = size / 2f
            outCanvas.drawCircle(radius, radius, radius, paint)

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.argb(32, 255, 255, 255)
            }
            outCanvas.drawCircle(radius, radius, radius - 1f, strokePaint)

            return BitmapDrawable(null, outBmp)
        }

        // 非自适应图标回退到标准圆切
        return createStandardCircle(drawable)
    }
}
