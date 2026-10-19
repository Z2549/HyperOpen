package com.hyperos.quickopen.view;

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
    private boolean isSelected = false;

    private int activeStyle;
    private final int sizePx;
    private float blurRadiusDp;
    private int paddingPercent;
    private final QuickOpenConfig config;
    private final ImageView iconView;

    @Nullable
    private Drawable systemBackdropBlurDrawable;

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

    private float getEffectiveCircleRadiusPx() {
        int w = getWidth() > 0 ? getWidth() : sizePx;
        int h = getHeight() > 0 ? getHeight() : sizePx;
        return Math.max(1f, Math.min(w, h) / 2f - 2f);
    }

    private int getBlurRadiusPx() {
        if (blurRadiusDp <= 0f) return 0;
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, blurRadiusDp, getResources().getDisplayMetrics()
        ));
    }

    /**
     * 确保 SurfaceFlinger 硬件 BackgroundBlurDrawable 已绑定至当前活跃的 ViewRootImpl，
     * 并与光学层合成为统一的圆形气泡背景（圆角半径严格内缩 2px，不触碰正方形四边，不触发 3D 多边形阴影）。
     */
    private void ensureBlurAndRefreshBackground() {
        int blurPx = getBlurRadiusPx();
        float radiusPx = getEffectiveCircleRadiusPx();
        int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);

        if (blurPx <= 0) {
            systemBackdropBlurDrawable = null;
        } else {
            if (systemBackdropBlurDrawable == null) {
                systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, radiusPx, blurPx, tint);
            } else {
                HyperOSBlurHelper.updateBlurDrawable(systemBackdropBlurDrawable, radiusPx, blurPx, tint);
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
            float radiusPx = getEffectiveCircleRadiusPx();
            int tint = HyperOSMaterialFactory.getUnifiedBlurTint(activeStyle);
            systemBackdropBlurDrawable = HyperOSBlurHelper.createSystemBackdropBlurDrawable(this, radiusPx, blurPx, tint);
        }

        if (blurPx > 0 && systemBackdropBlurDrawable != null) {
            LayerDrawable layer = new LayerDrawable(new Drawable[]{systemBackdropBlurDrawable, opticDrawable});
            layer.setLayerInset(0, 2, 2, 2, 2);
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
        systemBackdropBlurDrawable = null;
        refreshBackground();
    }

    public void setIconAlpha(float alpha) {
        if (iconView != null) {
            iconView.setAlpha(alpha);
        }
    }

    public void setSelectedState(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            refreshBackground();

            float targetScale = 1.0f;
            if (selected) {
                targetScale = config != null ? config.selectedIconScale : 1.15f;
            }

            iconView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(120)
                .start();
        }
    }

    public boolean isSelectedState() {
        return isSelected;
    }

    public void applyScale(float scale) {
        this.currentScale = scale;
        setScaleX(scale);
        setScaleY(scale);
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
