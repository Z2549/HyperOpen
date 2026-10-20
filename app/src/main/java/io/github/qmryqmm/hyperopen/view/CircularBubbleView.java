package io.github.qmryqmm.hyperopen.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.qmryqmm.hyperopen.config.HyperOpenConfig;
import io.github.qmryqmm.hyperopen.factory.HyperOSBlurHelper;
import io.github.qmryqmm.hyperopen.factory.HyperOSMaterialFactory;
import io.github.qmryqmm.hyperopen.model.ShortcutItem;

public class CircularBubbleView extends FrameLayout {

    public final ShortcutItem item;
    public float targetX;
    public float targetY;
    public float currentScale = 1.0f;
    private float fisheyeScale = 1.0f;
    private float selectedScale = 1.0f;
    private boolean isSelected = false;

    private int activeStyle;
    private final int sizePx;
    private float blurRadiusDp;
    private int paddingPercent;
    private final HyperOpenConfig config;
    private final ImageView iconView;

    @NonNull
    private final HyperOSMaterialFactory.CircularBubbleDrawable opticDrawable;
    @Nullable
    private Drawable systemBackdropBlurDrawable;
    @Nullable
    private WeakReference<Object> boundVriRef;
    private float lastBlurCornerRadiusPx = -1f;
    private boolean systemBlurEnabled = true;
    @Nullable
    private ValueAnimator selectedScaleAnimator;
    @Nullable
    private Runnable onScaleChangedListener;

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader, HyperOpenConfig config) {
        super(context);
        this.item = item;
        this.activeStyle = style;
        this.sizePx = sizePx;
        this.config = config;
        this.blurRadiusDp = config != null ? config.bubbleBlurRadius : 25f;
        this.paddingPercent = config != null ? config.bubblePaddingPercent : 18;

        boolean glowEnabled = config == null || config.iconGlowEffect;
        this.opticDrawable = new HyperOSMaterialFactory.CircularBubbleDrawable(
            activeStyle, item.tintColor, isSelected, glowEnabled
        );

        setLayoutParams(new LayoutParams(sizePx, sizePx));
        setClipChildren(false);
        setClipToPadding(false);

        // 应用与圆形视界底衬统一的光学材质与背景模糊
        refreshBackground();

        // 中心图标
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        applyIconPadding();

        LayoutParams iconLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        iconLp.gravity = Gravity.CENTER;
        addView(iconView, iconLp);

        // 应用图标透明度
        iconView.setAlpha(config != null ? config.iconAlpha : 1.0f);

        // 绑定图标 (优先从包名解析应用图标，或使用默认彩色矢量图形)
        loadItemIcon();
    }

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader) {
        this(context, item, style, sizePx, classLoader, null);
    }

    public void setOnScaleChangedListener(@Nullable Runnable listener) {
        this.onScaleChangedListener = listener;
    }

    /**
     * 控制是否启用底层窗口级 SurfaceFlinger BackgroundBlurDrawable。
     * 在主应用预览界面 (HyperOpenPreviewView) 中，已由 Compose 的 textureBlur 渲染实时壁纸模糊，
     * 禁用窗口级硬件模糊可避免在 Activity 窗口上叠加双层毛玻璃底色导致样式偏亮或闪烁。
     */
    public void setSystemBlurEnabled(boolean enabled) {
        if (this.systemBlurEnabled != enabled) {
            this.systemBlurEnabled = enabled;
            if (!enabled) {
                systemBackdropBlurDrawable = null;
                boundVriRef = null;
                lastBlurCornerRadiusPx = -1f;
            }
            ensureBlurAndRefreshBackground();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (systemBlurEnabled) {
            post(this::ensureBlurAndRefreshBackground);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && systemBlurEnabled && (w != oldw || h != oldh)) {
            ensureBlurAndRefreshBackground();
        }
    }

    private void applyIconPadding() {
        int iconPadding = (int) (sizePx * (Math.max(0, Math.min(40, paddingPercent)) / 100f));
        iconView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
    }

    /**
     * 获取内缩 3px 后的基础模糊圆半径（严格位于 2.2px 描边内侧，绝不溢出边框）
     */
    private float getEffectiveCircleRadiusPx() {
        int w = getWidth() > 0 ? getWidth() : sizePx;
        int h = getHeight() > 0 ? getHeight() : sizePx;
        return Math.max(1f, Math.min(w, h) / 2f - 3f);
    }

    /**
     * 计算用于 SurfaceFlinger BackgroundBlurDrawable 的正圆圆角半径：
     * Skia 的 SkRRect::setRectRadii 在 RenderThread 合成时会自动将大于等于 mRect 宽高一半的圆角
     * 精确钳制为当前缩放后 mRect 宽高的一半（保持 100% 几何正圆），无需在 UI 线程每帧反射修改圆角触发重绘闪烁。
     */
    private float getScaledBlurCornerRadiusPx() {
        return getEffectiveCircleRadiusPx() * 4.0f;
    }

    private int getBlurRadiusPx() {
        if (!systemBlurEnabled || blurRadiusDp <= 0f) return 0;
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, blurRadiusDp, getResources().getDisplayMetrics()
        ));
    }

    /**
     * 确保 SurfaceFlinger 硬件 BackgroundBlurDrawable 已绑定至当前活跃的 ViewRootImpl，
     * 并与光学层合成为统一的圆形气泡背景。
     */
    public void ensureBlurAndRefreshBackground() {
        int blurPx = getBlurRadiusPx();
        float scaledRadiusPx = getScaledBlurCornerRadiusPx();
        int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);

        if (blurPx <= 0) {
            systemBackdropBlurDrawable = null;
            boundVriRef = null;
            lastBlurCornerRadiusPx = -1f;
        } else {
            Object currentVri = HyperOSBlurHelper.findViewRootImpl(this);
            Object boundVri = boundVriRef != null ? boundVriRef.get() : null;

            if (systemBackdropBlurDrawable == null || (currentVri != null && currentVri != boundVri)) {
                systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, scaledRadiusPx, blurPx, tint);
                boundVriRef = currentVri != null ? new WeakReference<>(currentVri) : null;
                lastBlurCornerRadiusPx = scaledRadiusPx;
            } else {
                HyperOSBlurHelper.updateBlurDrawable(systemBackdropBlurDrawable, scaledRadiusPx, blurPx, tint);
                lastBlurCornerRadiusPx = scaledRadiusPx;
            }
            if (systemBackdropBlurDrawable != null) {
                try {
                    systemBackdropBlurDrawable.setVisible(true, false);
                } catch (Throwable ignored) {}
            }
        }
        refreshBackground();
    }

    private void refreshBackground() {
        boolean glowEnabled = config == null || config.iconGlowEffect;
        opticDrawable.setStyle(activeStyle);
        opticDrawable.setSelected(isSelected);
        opticDrawable.setGlowEnabled(glowEnabled);

        int blurPx = getBlurRadiusPx();
        if (blurPx > 0 && systemBackdropBlurDrawable == null && isAttachedToWindow()) {
            float scaledRadiusPx = getScaledBlurCornerRadiusPx();
            int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);
            Object currentVri = HyperOSBlurHelper.findViewRootImpl(this);
            systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, scaledRadiusPx, blurPx, tint);
            boundVriRef = currentVri != null ? new WeakReference<>(currentVri) : null;
            lastBlurCornerRadiusPx = scaledRadiusPx;
        }

        if (blurPx > 0 && systemBackdropBlurDrawable != null) {
            syncBlurCornerRadiusWithViewScale();
            Drawable currentBg = getBackground();
            if (currentBg instanceof LayerDrawable && ((LayerDrawable) currentBg).getNumberOfLayers() == 2
                    && ((LayerDrawable) currentBg).getDrawable(0) == systemBackdropBlurDrawable
                    && ((LayerDrawable) currentBg).getDrawable(1) == opticDrawable) {
                currentBg.invalidateSelf();
            } else {
                LayerDrawable layer = new LayerDrawable(new Drawable[]{systemBackdropBlurDrawable, opticDrawable});
                layer.setLayerInset(0, 3, 3, 3, 3);
                setBackground(layer);
            }
        } else {
            if (getBackground() != opticDrawable) {
                setBackground(opticDrawable);
            } else {
                opticDrawable.invalidateSelf();
            }
        }
    }

    public void setMaterialStyle(int style) {
        if (this.activeStyle != style) {
            this.activeStyle = style;
            opticDrawable.setStyle(style);
            ensureBlurAndRefreshBackground();
        }
    }

    public void setBlurRadius(float radiusDp) {
        float clamped = Math.max(0f, radiusDp);
        if (Math.abs(this.blurRadiusDp - clamped) > 0.01f) {
            this.blurRadiusDp = clamped;
            ensureBlurAndRefreshBackground();
        }
    }

    public void setBubblePaddingPercent(int percent) {
        if (this.paddingPercent != percent) {
            this.paddingPercent = percent;
            applyIconPadding();
            invalidate();
        }
    }

    public void resetBlur() {
        animate().cancel();
        if (selectedScaleAnimator != null) {
            selectedScaleAnimator.cancel();
            selectedScaleAnimator = null;
        }
        this.isSelected = false;
        this.fisheyeScale = 1.0f;
        this.selectedScale = 1.0f;
        this.currentScale = 1.0f;
        setScaleX(1.0f);
        setScaleY(1.0f);
        opticDrawable.setSelected(false);
        systemBackdropBlurDrawable = null;
        boundVriRef = null;
        lastBlurCornerRadiusPx = -1f;
        refreshBackground();
    }

    public void setIconAlpha(float alpha) {
        if (iconView != null && Math.abs(iconView.getAlpha() - alpha) > 0.002f) {
            iconView.setAlpha(alpha);
        }
    }

    public float getIconAlpha() {
        return iconView != null ? iconView.getAlpha() : 1.0f;
    }

    public void setSelectedState(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            boolean glowEnabled = config == null || config.iconGlowEffect;
            opticDrawable.setGlowEnabled(glowEnabled);
            opticDrawable.setSelected(selected);

            float targetSelectedScale = 1.0f;
            if (selected) {
                targetSelectedScale = config != null ? config.selectedIconScale : 1.15f;
            }

            if (selectedScaleAnimator != null) {
                selectedScaleAnimator.cancel();
            }
            selectedScaleAnimator = ValueAnimator.ofFloat(this.selectedScale, targetSelectedScale);
            selectedScaleAnimator.setDuration(120);
            selectedScaleAnimator.addUpdateListener(anim -> {
                this.selectedScale = (float) anim.getAnimatedValue();
                applyEffectiveScale();
                if (onScaleChangedListener != null) {
                    onScaleChangedListener.run();
                }
            });
            selectedScaleAnimator.start();
        }
    }

    public void applyScale(float scale) {
        if (Math.abs(this.fisheyeScale - scale) > 0.0005f) {
            this.fisheyeScale = scale;
            applyEffectiveScale();
        }
    }

    private void applyEffectiveScale() {
        float newScale = fisheyeScale * selectedScale;
        if (Math.abs(this.currentScale - newScale) > 0.0005f || Math.abs(getScaleX() - newScale) > 0.0005f) {
            this.currentScale = newScale;
            setScaleX(newScale);
            setScaleY(newScale);
        }
    }

    /**
     * 保持 BackgroundBlurDrawable 圆角半径始终覆盖缩放后正圆（仅在数值发生变化时才触发反射更新）
     */
    public void syncBlurCornerRadiusWithViewScale() {
        if (systemBackdropBlurDrawable != null) {
            float r = getScaledBlurCornerRadiusPx();
            if (Math.abs(r - lastBlurCornerRadiusPx) > 0.5f) {
                lastBlurCornerRadiusPx = r;
                HyperOSBlurHelper.updateCornerRadius(systemBackdropBlurDrawable, r);
            }
        }
    }

    public void loadItemIcon() {
        // 统一由 IconGlyphFactory 解析，传入当前实时配置（裁切模式、Miuix 底垫及颜色等）
        Drawable d = IconGlyphFactory.resolve(getContext(), item, config);
        iconView.setImageDrawable(d);
        iconView.setColorFilter(null);
        if (d != null) {
            d.setCallback(iconView);
        }
        iconView.requestLayout();
        iconView.invalidate();
        invalidate();
    }
}
