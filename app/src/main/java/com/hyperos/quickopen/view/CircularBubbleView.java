package com.hyperos.quickopen.view;

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

import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.factory.HyperOSBlurHelper;
import com.hyperos.quickopen.factory.HyperOSMaterialFactory;
import com.hyperos.quickopen.model.ShortcutItem;

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
    private final QuickOpenConfig config;
    private final ImageView iconView;

    @Nullable
    private Drawable systemBackdropBlurDrawable;
    @Nullable
    private ValueAnimator selectedScaleAnimator;
    @Nullable
    private Runnable onScaleChangedListener;

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader, QuickOpenConfig config) {
        super(context);
        this.item = item;
        this.activeStyle = style;
        this.sizePx = sizePx;
        this.config = config;
        this.blurRadiusDp = config != null ? config.bubbleBlurRadius : 25f;
        this.paddingPercent = config != null ? config.bubblePaddingPercent : 18;

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(this::ensureBlurAndRefreshBackground);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
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
     * 计算考虑当前 View 缩放倍率后的屏幕空间圆角半径：
     * Android 的 BackgroundBlurDrawable.PositionUpdateListener 会按 View 的 scaleX/scaleY 放大模糊矩形 mRect，
     * 但不会自动将 mCornerRadius 乘以 scale，若不随缩放同步放大圆角，放大时模糊区域就会退化为圆角正方形并从四角溢出圆形边框。
     */
    private float getScaledBlurCornerRadiusPx() {
        float scale = Math.max(0.1f, Math.max(getScaleX(), currentScale));
        return getEffectiveCircleRadiusPx() * scale + 2f;
    }

    private int getBlurRadiusPx() {
        if (blurRadiusDp <= 0f) return 0;
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, blurRadiusDp, getResources().getDisplayMetrics()
        ));
    }

    /**
     * 确保 SurfaceFlinger 硬件 BackgroundBlurDrawable 已绑定至当前活跃的 ViewRootImpl，
     * 并与光学层合成为统一的圆形气泡背景（圆角半径严格内缩 3px 并随缩放动态同步，不触发 3D 多边形阴影且绝不溢出边框）。
     */
    private void ensureBlurAndRefreshBackground() {
        int blurPx = getBlurRadiusPx();
        float scaledRadiusPx = getScaledBlurCornerRadiusPx();
        int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);

        if (blurPx <= 0) {
            systemBackdropBlurDrawable = null;
        } else {
            if (systemBackdropBlurDrawable == null) {
                systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, scaledRadiusPx, blurPx, tint);
            } else {
                HyperOSBlurHelper.updateBlurDrawable(systemBackdropBlurDrawable, scaledRadiusPx, blurPx, tint);
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
        Drawable opticDrawable = HyperOSMaterialFactory.createCircularBubbleDrawable(
            activeStyle, item.tintColor, isSelected, glowEnabled
        );

        int blurPx = getBlurRadiusPx();
        if (blurPx > 0 && systemBackdropBlurDrawable == null && isAttachedToWindow()) {
            float scaledRadiusPx = getScaledBlurCornerRadiusPx();
            int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);
            systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, scaledRadiusPx, blurPx, tint);
        }

        if (blurPx > 0 && systemBackdropBlurDrawable != null) {
            HyperOSBlurHelper.updateCornerRadius(systemBackdropBlurDrawable, getScaledBlurCornerRadiusPx());
            LayerDrawable layer = new LayerDrawable(new Drawable[]{systemBackdropBlurDrawable, opticDrawable});
            layer.setLayerInset(0, 3, 3, 3, 3);
            setBackground(layer);
        } else {
            setBackground(opticDrawable);
        }
    }

    public void setMaterialStyle(int style) {
        if (this.activeStyle != style) {
            this.activeStyle = style;
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
        systemBackdropBlurDrawable = null;
        refreshBackground();
    }

    public void setIconAlpha(float alpha) {
        if (iconView != null) {
            iconView.setAlpha(alpha);
        }
    }

    public float getIconAlpha() {
        return iconView != null ? iconView.getAlpha() : 1.0f;
    }

    public void setSelectedState(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            refreshBackground();

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

    public boolean isSelectedState() {
        return isSelected;
    }

    public void applyScale(float scale) {
        this.fisheyeScale = scale;
        applyEffectiveScale();
    }

    private void applyEffectiveScale() {
        this.currentScale = fisheyeScale * selectedScale;
        setScaleX(this.currentScale);
        setScaleY(this.currentScale);
        syncBlurCornerRadiusWithViewScale();
    }

    /**
     * 在属性动画或缩放变更时实时将 BackgroundBlurDrawable 圆角与当前 View 缩放对齐，
     * 防止放大时模糊层退化为非圆形或溢出圆形边框。
     */
    public void syncBlurCornerRadiusWithViewScale() {
        if (systemBackdropBlurDrawable != null) {
            HyperOSBlurHelper.updateCornerRadius(systemBackdropBlurDrawable, getScaledBlurCornerRadiusPx());
            systemBackdropBlurDrawable.invalidateSelf();
            invalidate();
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
