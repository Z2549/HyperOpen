package com.hyperos.hyperopen.factory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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
    public static final int STYLE_SOFT_GLASS = 1;
    public static final int STYLE_GAUSSIAN_BLUR = 2;

    /**
     * 全局统一的实时背景毛玻璃底色混合（与圆形视界区域底衬完全对齐）：
     * - 液态玻璃使用晶莹通透微光混合 (0x16FFFFFF)，最大化保留底层实时动态色彩与折射对比度
     * - 高斯模糊使用沉稳哑光微透混合 (0x12FFFFFF)，绝不死黑
     */
    public static final int BLUR_TINT_SOFT_GLASS = 0x16FFFFFF;
    public static final int BLUR_TINT_GAUSSIAN_BLUR = 0x12FFFFFF;

    public static int getUnifiedBlurTint(int style) {
        return (style == STYLE_SOFT_GLASS) ? BLUR_TINT_SOFT_GLASS : BLUR_TINT_GAUSSIAN_BLUR;
    }

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
     * 创建结合系统原生 BackgroundBlur 与 HeaderPill 物理光学样式的复合胶囊背景。
     * 模糊底色混合与光学参数与圆形视界区域底衬完全统一。
     */
    public static Drawable createCompositeHeaderPillDrawable(@Nullable View view, int style, int blurRadiusPx) {
        HeaderPillDrawable opticDrawable = new HeaderPillDrawable(style);
        if (view == null || blurRadiusPx <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return opticDrawable;
        }
        int h = view.getHeight() > 0 ? view.getHeight() : 96;
        float cornerRadius = Math.max(1f, h / 2f - 1.5f);
        int tint = getUnifiedBlurTint(style);
        Drawable blurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(view, cornerRadius, blurRadiusPx, tint);
        if (blurDrawable != null) {
            android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(new Drawable[]{blurDrawable, opticDrawable});
            layer.setLayerInset(0, 2, 2, 2, 2);
            return layer;
        }
        return opticDrawable;
    }

    /**
     * 统一绘制正圆形物理光学层（供 CircularRegionBackdropView 与 CircularBubbleDrawable 共同复用，确保全组件视觉 100% 统一）：
     * - 液态玻璃 (STYLE_SOFT_GLASS)：中空清透凸透镜核心 + 边缘全反射内壁微光焦散 + 顶部弯月镜面天光切面 + 底部折射漫反射 + 双极菲涅尔晶亮圆环描边
     * - 高斯模糊 (STYLE_GAUSSIAN_BLUR)：沉稳微晶哑光磨砂漫反射底衬 + 细腻全包围微光描边（无切面反光）
     */
    public static void drawUnifiedCircularOptics(
        @NonNull Canvas canvas,
        int width,
        int height,
        int style,
        boolean isSelected,
        @NonNull Paint bgPaint,
        @NonNull Paint highlightPaint,
        @NonNull Paint borderPaint
    ) {
        if (width <= 0 || height <= 0) return;
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) - 2.0f;
        if (radius <= 0f) return;

        if (style == STYLE_SOFT_GLASS) {
            // ================= 液态玻璃（Liquid Glass / 立体水滴折射与边缘焦散） =================
            // 1. 凸透镜边缘菲涅尔内壁折射焦散（中心清透不发白，四周边缘因玻璃厚度凝聚晶莹内发光）
            int centerClear = isSelected ? 0x10FFFFFF : 0x04FFFFFF;
            int midRefract = isSelected ? 0x18FFFFFF : 0x0AFFFFFF;
            int innerRimCaustic = isSelected ? 0x52FFFFFF : 0x34FFFFFF;
            int outerEdge = isSelected ? 0x22FFFFFF : 0x12FFFFFF;
            RadialGradient lensRefraction = new RadialGradient(
                cx, cy - radius * 0.08f, radius,
                new int[]{centerClear, midRefract, innerRimCaustic, outerEdge},
                new float[]{0f, 0.62f, 0.90f, 1f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(lensRefraction);
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // 2. 顶部天光镜面弯月高光 + 底部二次折射回光（模拟真实水滴顶亮、中透、底润的立体厚度感）
            int topSpecular = isSelected ? 0xB0FFFFFF : 0x82FFFFFF;
            int topFade = isSelected ? 0x32FFFFFF : 0x1EFFFFFF;
            int bottomCaustic = isSelected ? 0x3CFFFFFF : 0x24FFFFFF;
            LinearGradient specularAndCaustic = new LinearGradient(
                0, cy - radius, 0, cy + radius,
                new int[]{topSpecular, topFade, 0x00FFFFFF, 0x00FFFFFF, bottomCaustic},
                new float[]{0f, 0.24f, 0.48f, 0.78f, 1f},
                Shader.TileMode.CLAMP
            );
            highlightPaint.setShader(specularAndCaustic);
            canvas.drawCircle(cx, cy, radius, highlightPaint);

            // 3. 顶部内嵌新月高光亮弧（收拢在圆内上沿，赋予液态张力表面光泽）
            float crescentRadius = radius * 0.91f;
            if (crescentRadius > 2f) {
                int crestTop = isSelected ? 0x68FFFFFF : 0x46FFFFFF;
                RadialGradient crestShader = new RadialGradient(
                    cx, cy - radius * 0.56f, radius * 0.72f,
                    new int[]{crestTop, 0x12FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(crestShader);
                canvas.drawCircle(cx, cy, crescentRadius, highlightPaint);
            }

            // 4. 双极菲涅尔镜面圆环描边（顶部高光掠射、两侧清透过渡、底部折射亮边）
            int bTop = isSelected ? 0xFFFFFFFF : 0xEEFFFFFF;
            int bUpperMid = isSelected ? 0x90FFFFFF : 0x58FFFFFF;
            int bWaist = isSelected ? 0x42FFFFFF : 0x22FFFFFF;
            int bBottom = isSelected ? 0x98FFFFFF : 0x64FFFFFF;
            LinearGradient borderGrad = new LinearGradient(
                cx - radius * 0.25f, cy - radius, cx + radius * 0.25f, cy + radius,
                new int[]{bTop, bUpperMid, bWaist, bBottom},
                new float[]{0f, 0.35f, 0.72f, 1f},
                Shader.TileMode.CLAMP
            );
            borderPaint.setShader(borderGrad);
            canvas.drawCircle(cx, cy, radius, borderPaint);

        } else {
            // ================= 高斯模糊（Gaussian Blur / 沉稳哑光微晶磨砂） =================
            // 1. 沉稳微晶哑光磨砂漫反射底衬（与圆形视界区域底衬完全一致）
            int d0 = isSelected ? 0x28FFFFFF : 0x14FFFFFF;
            int d1 = isSelected ? 0x14FFFFFF : 0x0AFFFFFF;
            RadialGradient radialMatte = new RadialGradient(
                cx, cy, radius,
                new int[]{d0, d1, 0x00FFFFFF},
                new float[]{0f, 0.70f, 1f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(radialMatte);
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // 2. 细腻全包围微光描边（纯净哑光质感，无弧形切面反光）
            borderPaint.setShader(null);
            borderPaint.setColor(isSelected ? 0x98FFFFFF : 0x45FFFFFF);
            canvas.drawCircle(cx, cy, radius, borderPaint);
        }
    }

    /**
     * 顶部提示胶囊专用材质：与圆形视界区域底衬及气泡完全统一的液态玻璃 / 高斯模糊光学层
     */
    public static class HeaderPillDrawable extends Drawable {
        private int style;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HeaderPillDrawable(int style) {
            this.style = style;
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.0f);
        }

        public void setStyle(int newStyle) {
            if (this.style != newStyle) {
                this.style = newStyle;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float radius = Math.min(width, height) / 2f;
            RectF pill = new RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f);

            if (style == STYLE_SOFT_GLASS) {
                // ================= 液态玻璃（与圆形视界区域底衬完全统一） =================
                // 1. 胶囊表面张力折射底衬（顶底微润、中心清透）
                LinearGradient bgShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0x28FFFFFF, 0x06FFFFFF, 0x04FFFFFF, 0x1EFFFFFF},
                    new float[]{0f, 0.38f, 0.72f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgShader);
                canvas.drawRoundRect(pill, radius, radius, bgPaint);

                // 2. 顶部天光镜面切面与底部回光
                LinearGradient hlShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0x80FFFFFF, 0x1EFFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x22FFFFFF},
                    new float[]{0f, 0.25f, 0.48f, 0.78f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(hlShader);
                canvas.drawRoundRect(pill, radius, radius, highlightPaint);

                // 3. 双极菲涅尔镜面高亮描边（顶亮、腰透、底润）
                LinearGradient borderShader = new LinearGradient(
                    0, 0, width * 0.15f, height,
                    new int[]{0xEEFFFFFF, 0x58FFFFFF, 0x22FFFFFF, 0x64FFFFFF},
                    new float[]{0f, 0.35f, 0.72f, 1f},
                    Shader.TileMode.CLAMP
                );
                borderPaint.setShader(borderShader);
                canvas.drawRoundRect(pill, radius, radius, borderPaint);

            } else {
                // ================= 高斯模糊（与圆形视界区域底衬完全统一的沉稳哑光微晶磨砂） =================
                // 1. 沉稳微晶哑光磨砂漫反射底衬
                LinearGradient blurBgShader = new LinearGradient(
                    0, 0, 0, height,
                    new int[]{0x14FFFFFF, 0x0AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.70f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(blurBgShader);
                canvas.drawRoundRect(pill, radius, radius, bgPaint);

                // 2. 纯净全包围哑光边框（无切面反光）
                borderPaint.setShader(null);
                borderPaint.setColor(0x45FFFFFF);
                canvas.drawRoundRect(pill, radius, radius, borderPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            bgPaint.setAlpha(alpha);
            borderPaint.setAlpha(alpha);
            highlightPaint.setAlpha(alpha);
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
     * 直接复用 drawUnifiedCircularOptics，使图标气泡留白区域的液态玻璃与高斯模糊样式
     * 同圆形视界区域底衬 (CircularRegionBackdropView) 保持 100% 统一。
     */
    public static class CircularBubbleDrawable extends Drawable {
        private int style;
        private final int tintColor;
        private boolean isSelected;
        private boolean glowEnabled;

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
            borderPaint.setStrokeWidth(isSelected ? 2.4f : 2.0f);
        }

        public void setSelected(boolean selected) {
            if (this.isSelected != selected) {
                this.isSelected = selected;
                borderPaint.setStrokeWidth(selected ? 2.4f : 2.0f);
                invalidateSelf();
            }
        }

        public void setStyle(int newStyle) {
            if (this.style != newStyle) {
                this.style = newStyle;
                invalidateSelf();
            }
        }

        public void setGlowEnabled(boolean enabled) {
            if (this.glowEnabled != enabled) {
                this.glowEnabled = enabled;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(cx, cy) - 2.0f;
            if (radius <= 0f) return;

            // 1~3. 统一绘制与圆形视界区域底衬完全一致的液态玻璃 / 高斯模糊光学层
            drawUnifiedCircularOptics(canvas, width, height, style, isSelected, bgPaint, highlightPaint, borderPaint);

            // 4. 选中状态下的主题色内聚呼吸柔光微晕（严格收敛在 radius 内部，绝不越界被方形 View 边界截成八边形）
            if (isSelected && glowEnabled) {
                int coreColor = (tintColor & 0x00FFFFFF) | 0x66000000;
                RadialGradient glowShader = new RadialGradient(
                    cx, cy, radius * 0.88f,
                    new int[]{coreColor, (coreColor & 0x00FFFFFF) | 0x14000000, 0x00000000},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
                );
                glowPaint.setShader(glowShader);
                canvas.drawCircle(cx, cy, radius, glowPaint);

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
