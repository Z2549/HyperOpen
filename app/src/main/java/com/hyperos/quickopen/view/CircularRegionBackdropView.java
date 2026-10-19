package com.hyperos.quickopen.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyperos.quickopen.factory.HyperOSBlurHelper;
import com.hyperos.quickopen.factory.HyperOSMaterialFactory;

/**
 * 圆形视界区域背景视图：
 * 真正的实时系统级局部背景毛玻璃（Real-time Live Backdrop Blur）：
 * 1. 每次唤起展开均重新校准 ViewRootImpl 硬件管道，解决第二次及后续唤出模糊丢失的生命周期问题；
 * 2. 彻底移除任何静态壁纸或死黑图层遮挡，确保 100% 实时穿透看清前台正在运行的应用；
 * 3. 由 SurfaceFlinger 硬件图层合成器通过 BackgroundBlurDrawable 在正圆物理区域内实施硬件高斯模糊；
 * 4. 严格正圆形 OutlineProvider 硬件裁切，确保圆内实时高斯模糊、圆外 100% 清晰原始画面；
 * 5. 叠加水润通透次表面散射、液态玻璃天穹切面高光与菲涅尔镜面圆环；
 * 6. 与全屏模糊彻底解耦，独立受控于自身的模糊半径与透明度。
 */
public class CircularRegionBackdropView extends FrameLayout {
    private static final String TAG = "QuickOpen_RegionBlur";

    private int style = HyperOSMaterialFactory.STYLE_SOFT_GLASS;
    private float regionAlpha = 1.0f;
    private float blurRadiusDp = 25f;
    private float currentRadiusPx = 0f;

    private OverlayDrawView overlayView;
    private Drawable systemBackdropBlurDrawable;

    public CircularRegionBackdropView(@NonNull Context context) {
        this(context, (ClassLoader) null);
    }

    public CircularRegionBackdropView(@NonNull Context context, @Nullable ClassLoader classLoader) {
        super(context);
        init();
    }

    public CircularRegionBackdropView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClipChildren(false);

        // 1. 设置严格正正圆形 OutlineProvider 硬件裁切 (圆内高斯模糊，圆外完全清晰)
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        setClipToOutline(true);

        // 2. 顶层水润高光切面、次表面微晶漫反射与菲涅尔双向镜面圆环 (超高透光率，不遮挡底层实时画面)
        overlayView = new OverlayDrawView(getContext());
        addView(overlayView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        refreshVisuals();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(() -> {
            float r = currentRadiusPx > 0 ? currentRadiusPx : (getWidth() > 0 ? getWidth() / 2f : 300f);
            updateGeometry(r);
        });
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE && currentRadiusPx > 0) {
            post(() -> updateGeometry(currentRadiusPx));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            updateGeometry(w / 2f);
        }
    }

    public void setStyle(int style) {
        if (this.style != style) {
            this.style = style;
            refreshVisuals();
            if (currentRadiusPx > 0) {
                updateGeometry(currentRadiusPx);
            }
        }
    }

    public void setRegionAlpha(float alpha) {
        this.regionAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        refreshVisuals();
    }

    public void setBlurRadius(float radiusDp) {
        this.blurRadiusDp = Math.max(0f, radiusDp);
        refreshVisuals();
        if (currentRadiusPx > 0) {
            updateGeometry(currentRadiusPx);
        }
    }

    /**
     * 重置模糊实例，避免跨生命周期持有失效的旧 ViewRootImpl 句柄
     */
    public void reset() {
        systemBackdropBlurDrawable = null;
        setBackground(null);
    }

    /**
     * 实时设定物理圆形视界半径并同步更新 SurfaceFlinger 实时背景模糊通道
     */
    public void updateGeometry(float radiusPx) {
        if (radiusPx <= 0) return;
        this.currentRadiusPx = radiusPx;

        float blurPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, blurRadiusDp, getResources().getDisplayMetrics()
        );
        // 液态玻璃使用通透白微光混合 (0x1CFFFFFF)，高斯模糊使用沉稳微透 (0x12FFFFFF)，绝不死黑
        int tint = (style == HyperOSMaterialFactory.STYLE_SOFT_GLASS) ? 0x1CFFFFFF : 0x12FFFFFF;

        // 每次重新展开时强制检查并绑定当前活跃的 ViewRootImpl，彻底消除复用失效引用的 Bug
        Drawable bd = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, radiusPx, (int) blurPx, tint);
        if (bd != null) {
            systemBackdropBlurDrawable = bd;
            setBackground(systemBackdropBlurDrawable);
            try {
                systemBackdropBlurDrawable.setVisible(true, true);
            } catch (Throwable ignored) {}
        } else if (systemBackdropBlurDrawable != null) {
            HyperOSBlurHelper.updateBlurDrawable(systemBackdropBlurDrawable, radiusPx, (int) blurPx, tint);
            try {
                systemBackdropBlurDrawable.setVisible(true, false);
            } catch (Throwable ignored) {}
        }

        if (overlayView != null) {
            overlayView.invalidate();
        }
        invalidate();
    }

    private void refreshVisuals() {
        setAlpha(Math.max(0.0f, Math.min(1.0f, regionAlpha)));

        float blurPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, blurRadiusDp, getResources().getDisplayMetrics()
        );
        int tint = (style == HyperOSMaterialFactory.STYLE_SOFT_GLASS) ? 0x1CFFFFFF : 0x12FFFFFF;

        if (systemBackdropBlurDrawable != null) {
            HyperOSBlurHelper.updateBlurDrawable(systemBackdropBlurDrawable, currentRadiusPx, (int) blurPx, tint);
        }

        if (overlayView != null) {
            overlayView.invalidate();
        }
        invalidate();
    }

    /**
     * 顶层深邃光影与双向菲涅尔镜面圆环 (超高透光率，绝不遮挡实时透过的前台应用画面)
     */
    private class OverlayDrawView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public OverlayDrawView(Context context) {
            super(context);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.0f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(cx, cy) - 2f;
            if (radius <= 0) return;

            if (style == HyperOSMaterialFactory.STYLE_SOFT_GLASS) {
                // ================= 液态玻璃（Soft Light Glass / 水润高动态流体） =================
                // 1. 次表面水润微光（透光率 90%+，保留底层前台应用色彩）
                RadialGradient radialLight = new RadialGradient(
                    cx, cy, radius,
                    new int[]{0x1EFFFFFF, 0x0AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.65f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(radialLight);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                // 2. 穹顶弧形切面天光反射（水滴晶莹切面高光）
                LinearGradient hlShader = new LinearGradient(
                    0, 0, 0, cy * 0.95f,
                    new int[]{0x70FFFFFF, 0x1AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
                );
                highlightPaint.setShader(hlShader);
                canvas.save();
                canvas.clipRect(0, 0, w, cy * 0.95f);
                canvas.drawCircle(cx, cy, radius, highlightPaint);
                canvas.restore();

                // 3. 菲涅尔动态梯度镜面圆环描边（顶亮底润）
                LinearGradient borderGrad = new LinearGradient(
                    0, 0, 0, h,
                    new int[]{0xD5FFFFFF, 0x48FFFFFF, 0x22FFFFFF},
                    new float[]{0f, 0.50f, 1f},
                    Shader.TileMode.CLAMP
                );
                borderPaint.setShader(borderGrad);
                canvas.drawCircle(cx, cy, radius, borderPaint);

            } else {
                // ================= 高斯模糊（Gaussian Blur / 沉稳哑光微晶磨砂） =================
                // 1. 沉稳微晶哑光磨砂漫反射底衬 (高透光率，不遮挡前台画面)
                RadialGradient radialDark = new RadialGradient(
                    cx, cy, radius,
                    new int[]{0x14FFFFFF, 0x0AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.70f, 1f},
                    Shader.TileMode.CLAMP
                );
                bgPaint.setShader(radialDark);
                canvas.drawCircle(cx, cy, radius, bgPaint);

                // 2. 细腻全包围微光描边（纯净哑光质感，无弧形切面反光）
                borderPaint.setShader(null);
                borderPaint.setColor(0x45FFFFFF);
                canvas.drawCircle(cx, cy, radius, borderPaint);
            }
        }
    }
}
