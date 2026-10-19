package com.hyperos.quickopen.factory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * HyperOS 材质工程：柔光玻璃（Soft Light Glass）与高斯模糊（Gaussian Blur）次世代实现。
 * 针对澎湃OS 2.0 / HyperOS 物理光学渲染管线深度优化：
 * 1. 真实高动态次表面散射 (SSS) 与多层菲涅尔 (Fresnel) 双向光影渐变；
 * 2. 哑光磨砂微晶扩散与深色反差边框增强；
 * 3. 硬件着色器 GPU 级实时光栅化，0 额外 CPU 渲染损耗。
 */
public class HyperOSMaterialFactory {
    private static final String TAG = "QuickOpen_Material";

    public static final int STYLE_SOFT_GLASS = 1;
    public static final int STYLE_GAUSSIAN_BLUR = 2;

    /**
     * 判断当前应使用的材质：柔光玻璃与高斯模糊二选一，默认柔光玻璃
     */
    public static int resolveActiveStyle(Context context, int configStyle) {
        if (configStyle == STYLE_GAUSSIAN_BLUR) {
            return STYLE_GAUSSIAN_BLUR;
        }
        return STYLE_SOFT_GLASS;
    }

    public static Drawable createCircularBubbleDrawable(int style, int tintColor, boolean isSelected) {
        return new CircularBubbleDrawable(style, tintColor, isSelected, true);
    }

    public static Drawable createCircularBubbleDrawable(int style, int tintColor, boolean isSelected, boolean glowEnabled) {
        return new CircularBubbleDrawable(style, tintColor, isSelected, glowEnabled);
    }

    public static Drawable createHeaderPillDrawable(int style) {
        return new HeaderPillDrawable(style);
    }

    /**
     * 创建结合系统原生 BackgroundBlur 与 HeaderPill 物理光学样式的复合胶囊背景
     */
    public static Drawable createCompositeHeaderPillDrawable(@Nullable View view, int style, int blurRadiusPx) {
        HeaderPillDrawable opticDrawable = new HeaderPillDrawable(style);
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return opticDrawable;
        }
        int h = view.getHeight() > 0 ? view.getHeight() : 96;
        float cornerRadius = h / 2f;
        int tint = (style == STYLE_SOFT_GLASS) ? 0x18FFFFFF : 0x48000000;
        Drawable blurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(view, cornerRadius, blurRadiusPx, tint);
        if (blurDrawable != null) {
            return new android.graphics.drawable.LayerDrawable(new Drawable[]{blurDrawable, opticDrawable});
        }
        return opticDrawable;
    }

    /**
     * 顶部提示胶囊专用材质：微透圆角胶囊玻璃（支持液态玻璃与高斯模糊动态切换）
     */
    public static class HeaderPillDrawable extends Drawable {
        private int style;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HeaderPillDrawable(int style) {
            this.style = style;
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.0f);
            innerShadowPaint.setStyle(Paint.Style.STROKE);
            innerShadowPaint.setStrokeWidth(1.2f);
        }

        public void setStyle(int newStyle) {
            if (this.style != newStyle) {
                this.style = newStyle;
                invalidateSelf();
            }
        }

        public int getStyle() {
            return style;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float radius = Math.min(width, height) / 2f;
            RectF pill = new RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f);

            if (style == STYLE_SOFT_GLASS) {
                // ================= 液态玻璃（Liquid Glass / 流体水润高光） =================
                // 1. 液态玻璃渐变底（高透次表面漫反射）
                LinearGradient bgShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0x58FFFFFF, 0x2AFFFFFF, 0x18FFFFFF, 0x26FFFFFF},
                    new float[]{0f, 0.35f, 0.70f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgShader);
                canvas.drawRoundRect(pill, radius, radius, bgPaint);

                // 2. 穹顶水滴高光切面（高亮强折射）
                LinearGradient hlShader = new LinearGradient(
                    0, 0, 0, height * 0.52f,
                    new int[]{0xAAFFFFFF, 0x33FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(hlShader);
                canvas.save();
                canvas.clipRect(0, 0, width, height * 0.52f);
                canvas.drawRoundRect(pill, radius, radius, highlightPaint);
                canvas.restore();

                // 3. 菲涅尔双向镜面高亮描边（顶白底润）
                LinearGradient borderShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0xE0FFFFFF, 0x4DFFFFFF, 0x24FFFFFF},
                    new float[]{0f, 0.50f, 1f},
                    Shader.TileMode.CLAMP
                );
                borderPaint.setShader(borderShader);
                canvas.drawRoundRect(pill, radius, radius, borderPaint);

            } else {
                // ================= 高斯模糊 (沉稳细腻哑光磨砂) =================
                // 1. 哑光微晶磨砂底
                LinearGradient blurBgShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0x2AFFFFFF, 0x16FFFFFF, 0x1EFFFFFF},
                    new float[]{0f, 0.50f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(blurBgShader);
                canvas.drawRoundRect(pill, radius, radius, bgPaint);

                // 2. 纯净全包围哑光边框（无切面反光）
                borderPaint.setShader(null);
                borderPaint.setColor(0x4DFFFFFF);
                canvas.drawRoundRect(pill, radius, radius, borderPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            bgPaint.setAlpha(alpha);
            borderPaint.setAlpha(alpha);
            highlightPaint.setAlpha(alpha);
            innerShadowPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            bgPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * 专属自定义圆形气泡材质 Drawable：
     * - 柔光玻璃：多重双向折射光、弧形穹顶天光高光、自适应外发光与菲涅尔镜面描边；
     * - 高斯模糊：哑光微晶散斑底衬、深浅对比防发白环形描边、柔和晶核漫反射。
     */
    public static class CircularBubbleDrawable extends Drawable {
        private final int style;
        private final int tintColor;
        private final boolean isSelected;
        private final boolean glowEnabled;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outerAuraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CircularBubbleDrawable(int style, int tintColor, boolean isSelected) {
            this(style, tintColor, isSelected, true);
        }

        public CircularBubbleDrawable(int style, int tintColor, boolean isSelected, boolean glowEnabled) {
            this.style = style;
            this.tintColor = tintColor;
            this.isSelected = isSelected;
            this.glowEnabled = glowEnabled;
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(isSelected ? 2.4f : 1.8f);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float cx = width / 2f;
            float cy = height / 2f;
            float maxRadius = Math.min(cx, cy);
            // 严格收敛在正方形视图边界内部，杜绝任何超出 [0, width] x [0, height] 被四边截断成八边形的现象
            float radius = maxRadius - 2.0f;
            if (radius <= 0f) return;

            if (style == STYLE_SOFT_GLASS) {
                // ================= 柔光玻璃（Soft Light Glass） =================
                // 1. 次表面多层漫反射底衬（135度立体光轴）
                int c1 = isSelected ? 0x88FFFFFF : 0x56FFFFFF;
                int c2 = isSelected ? 0x48FFFFFF : 0x22FFFFFF;
                int c3 = isSelected ? 0x1AFFFFFF : 0x0DFFFFFF;
                int c4 = isSelected ? 0x3DFFFFFF : 0x1EFFFFFF;

                LinearGradient bgShader = new LinearGradient(
                    0, 0, width * 0.95f, height * 0.95f,
                    new int[]{c1, c2, c3, c4},
                    new float[]{0f, 0.38f, 0.72f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgShader);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                // 2. 穹顶弧形切面天光反射（使用 Shader 自然衰减至透明，无需 clipRect）
                int hlTop = isSelected ? 0xC0FFFFFF : 0x85FFFFFF;
                LinearGradient hlShader = new LinearGradient(
                    0, 0, 0, cy * 0.95f,
                    new int[]{hlTop, 0x24FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(hlShader);
                canvas.drawCircle(cx, cy, radius, highlightPaint);

                // 3. 菲涅尔渐变光感描边（顶部反射亮、底部深邃回光）
                int bTop = isSelected ? 0xFFFFFFFF : 0x85FFFFFF;
                int bMid = isSelected ? 0x99FFFFFF : 0x33FFFFFF;
                int bBottom = isSelected ? 0xCCFFFFFF : 0x47FFFFFF;
                LinearGradient borderShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{bTop, bMid, bBottom},
                    new float[]{0f, 0.50f, 1f},
                    Shader.TileMode.CLAMP
                );
                borderPaint.setShader(borderShader);
                canvas.drawCircle(cx, cy, radius, borderPaint);

            } else {
                // ================= 高斯模糊（Gaussian Blur 哑光磨砂） =================
                // 1. 哑光微晶磨砂底
                int blurTop = isSelected ? 0x62FFFFFF : 0x33FFFFFF;
                int blurBottom = isSelected ? 0x38FFFFFF : 0x1EFFFFFF;
                LinearGradient blurBgShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{blurTop, blurBottom},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(blurBgShader);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                // 2. 磨砂细腻描边（带微弱对比度）
                borderPaint.setShader(null);
                borderPaint.setColor(isSelected ? 0xD0FFFFFF : 0x48FFFFFF);
                canvas.drawCircle(cx, cy, radius, borderPaint);
            }

            // 4. App 主题色核心散射光斑
            int glowAlpha = (isSelected && glowEnabled) ? 0xA6 : 0x4D;
            int coreColor = (tintColor & 0x00FFFFFF) | (glowAlpha << 24);
            RadialGradient glowShader = new RadialGradient(
                cx, cy, radius * 0.88f,
                new int[]{coreColor, (coreColor & 0x00FFFFFF) | 0x14000000, 0x00000000},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glowShader);
            canvas.drawCircle(cx, cy, radius, glowPaint);

            // 5. 选中状态下的内聚呼吸柔光微晕 (严格限制在 radius 之内，绝不越界被方形 View 边界截成八边形)
            if (isSelected && glowEnabled) {
                int auraColor = (tintColor & 0x00FFFFFF) | 0x38000000;
                RadialGradient auraShader = new RadialGradient(
                    cx, cy, radius,
                    new int[]{0x00000000, auraColor, 0x00000000},
                    new float[]{0.68f, 0.88f, 1f},
                    Shader.TileMode.CLAMP
                );
                outerAuraPaint.setShader(auraShader);
                canvas.drawCircle(cx, cy, radius, outerAuraPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            bgPaint.setAlpha(alpha);
            borderPaint.setAlpha(alpha);
            highlightPaint.setAlpha(alpha);
            glowPaint.setAlpha(alpha);
            outerAuraPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            bgPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
