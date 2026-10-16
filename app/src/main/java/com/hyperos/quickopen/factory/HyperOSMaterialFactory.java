package com.hyperos.quickopen.factory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class HyperOSMaterialFactory {
    private static final String TAG = "QuickOpen_Material";

    public static final int STYLE_SOFT_GLASS = 1;
    public static final int STYLE_GAUSSIAN_BLUR = 2;

    /**
     * 判断当前应使用的材质：根据配置与系统环境自动决定
     */
    public static int resolveActiveStyle(Context context, int configStyle) {
        if (configStyle == STYLE_SOFT_GLASS) return STYLE_SOFT_GLASS;
        if (configStyle == STYLE_GAUSSIAN_BLUR) return STYLE_GAUSSIAN_BLUR;

        // 跟随系统判断 (STYLE_FOLLOW_SYSTEM)
        try {
            int blurLevel = Settings.System.getInt(context.getContentResolver(), "blur_level", 1);
            boolean highEnd = Settings.System.getInt(context.getContentResolver(), "high_end_graphics_enabled", 1) == 1;
            if (!highEnd) {
                return STYLE_GAUSSIAN_BLUR; // 低配降级为标准模糊
            }
            return blurLevel >= 2 ? STYLE_SOFT_GLASS : STYLE_GAUSSIAN_BLUR;
        } catch (Throwable t) {
            return STYLE_SOFT_GLASS;
        }
    }

    /**
     * 【方案一】：尝试通过反射在 SystemUI 中复用官方 miuix.blur.widget.BlurView
     */
    @Nullable
    public static View tryCreateSystemBlurView(Context context, ClassLoader classLoader) {
        try {
            Class<?> blurViewClass = classLoader.loadClass("miuix.blur.widget.BlurView");
            Constructor<?> ctor = blurViewClass.getConstructor(Context.class);
            View blurView = (View) ctor.newInstance(context);

            Method setBlurEnabled = blurViewClass.getMethod("setBlurEnabled", boolean.class);
            setBlurEnabled.invoke(blurView, true);

            // 设为纯圆形轮廓裁切
            blurView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            blurView.setClipToOutline(true);

            Log.i(TAG, "成功复用 SystemUI 内部官方 miuix.blur.widget.BlurView！");
            return blurView;
        } catch (Throwable t) {
            Log.d(TAG, "SystemUI 未找到 miuix.blur.widget.BlurView 或反射失败，平滑切换至方案二兜底: " + t.getMessage());
            return null;
        }
    }

    /**
     * 【方案二兜底】：Android 12+ 原生 RenderEffect + 色彩饱和度补偿 (还原澎湃OS高通透质感)
     */
    public static void applyNativeBackdropBlur(View view, float blurRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // 1. 基础大半径高斯模糊
                RenderEffect blur = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP);

                // 2. 色彩饱和度提升 180% (去除灰色泛白雾气，保留澎湃OS鲜明色调)
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(1.80f);
                RenderEffect colorBoost = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm));

                RenderEffect combined = RenderEffect.createChainEffect(colorBoost, blur);
                view.setRenderEffect(combined);
            } catch (Throwable t) {
                Log.e(TAG, "应用 RenderEffect 失败", t);
            }
        }
    }

    /**
     * 创建纯圆形质感图标 Drawable (支持柔光玻璃与高斯模糊)
     */
    public static Drawable createCircularBubbleDrawable(int style, int tintColor, boolean isSelected) {
        return new CircularBubbleDrawable(style, tintColor, isSelected);
    }

    /**
     * 专属自定义圆形材质 Drawable (纯几何 Shader 实时渲染，0 损耗)
     */
    public static class CircularBubbleDrawable extends Drawable {
        private final int style;
        private final int tintColor;
        private final boolean isSelected;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CircularBubbleDrawable(int style, int tintColor, boolean isSelected) {
            this.style = style;
            this.tintColor = tintColor;
            this.isSelected = isSelected;
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.5f);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(cx, cy) - 2f;

            if (style == STYLE_SOFT_GLASS) {
                // ================= 柔光玻璃 =================
                // 1. 渐变半透底
                int c1 = isSelected ? 0x73FFFFFF : 0x4DFFFFFF;
                int c2 = isSelected ? 0x33FFFFFF : 0x1FFFFFFF;
                int c3 = isSelected ? 0x1AFFFFFF : 0x0AFFFFFF;

                LinearGradient bgShader = new LinearGradient(
                    0, 0, width, height,
                    new int[]{c1, c2, c3},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(bgShader);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                // 2. 顶部弧形反射切面高光
                int hlTop = isSelected ? 0xB3FFFFFF : 0x73FFFFFF;
                LinearGradient hlShader = new LinearGradient(
                    0, 0, 0, cy * 0.9f,
                    new int[]{hlTop, 0x1AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.65f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(hlShader);
                canvas.save();
                canvas.clipRect(0, 0, width, cy);
                canvas.drawCircle(cx, cy, radius, highlightPaint);
                canvas.restore();

                // 3. 亮边缘与菲涅尔描边
                borderPaint.setColor(isSelected ? 0xFFFFFFFF : 0x59FFFFFF);
                canvas.drawCircle(cx, cy, radius, borderPaint);

            } else {
                // ================= 高斯模糊 (哑光磨砂) =================
                bgPaint.setShader(null);
                bgPaint.setColor(isSelected ? 0x52FFFFFF : 0x26FFFFFF);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                borderPaint.setColor(isSelected ? 0xB3FFFFFF : 0x38FFFFFF);
                canvas.drawCircle(cx, cy, radius, borderPaint);
            }

            // 4. 中心 App 主题色自发光漫反射晶核
            int glowAlpha = isSelected ? 0x99 : 0x4D;
            int coreColor = (tintColor & 0x00FFFFFF) | (glowAlpha << 24);
            RadialGradient glowShader = new RadialGradient(
                cx, cy, radius * 0.85f,
                new int[]{coreColor, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glowShader);
            canvas.drawCircle(cx, cy, radius, glowPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            bgPaint.setAlpha(alpha);
            borderPaint.setAlpha(alpha);
            highlightPaint.setAlpha(alpha);
            glowPaint.setAlpha(alpha);
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
