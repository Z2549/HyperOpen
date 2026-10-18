package com.hyperos.quickopen.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.factory.HyperOSMaterialFactory;
import com.hyperos.quickopen.model.ShortcutItem;

public class CircularBubbleView extends FrameLayout {

    public final ShortcutItem item;
    public float targetX;
    public float targetY;
    public float currentScale = 1.0f;
    private boolean isSelected = false;

    private int activeStyle;
    private final QuickOpenConfig config;
    private final ImageView iconView;
    private View systemBlurView;

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader, QuickOpenConfig config) {
        super(context);
        this.item = item;
        this.activeStyle = style;
        this.config = config;

        setLayoutParams(new LayoutParams(sizePx, sizePx));
        setClipChildren(false);

        // 设置纯正圆形 OutlineProvider，并在放大时以真正圆形向外扩张，绝不出现矩形边界裁切
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        setClipToOutline(true);

        // 【方案一】：优先尝试添加官方 miuix.blur.widget.BlurView 控件
        systemBlurView = HyperOSMaterialFactory.tryCreateSystemBlurView(context, classLoader);
        if (systemBlurView != null) {
            addView(systemBlurView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        // 应用材质 Drawable (配合方案一增益高光，或作为方案二纯自绘底衬)
        refreshBackground();

        // 中心图标
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        boolean isFullBleed = config != null && config.appIconCropMode == QuickOpenConfig.CROP_FULL_BLEED;
        int iconPadding = isFullBleed ? 0 : (int) (sizePx * 0.22f);
        iconView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

        // 绑定图标 (优先从包名解析应用图标，或使用默认彩色矢量图形)
        loadItemIcon();

        LayoutParams iconLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        iconLp.gravity = Gravity.CENTER;
        addView(iconView, iconLp);
    }

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader) {
        this(context, item, style, sizePx, classLoader, null);
    }

    private void refreshBackground() {
        setBackground(HyperOSMaterialFactory.createCircularBubbleDrawable(activeStyle, item.tintColor, isSelected));
    }

    public void setMaterialStyle(int style) {
        this.activeStyle = style;
        refreshBackground();
    }

    public void setSelectedState(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            refreshBackground();

            boolean glow = config == null || config.iconGlowEffect;
            float targetScale = selected ? (glow ? 1.18f : 1.12f) : 1.0f;

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

    private void loadItemIcon() {
        // 统一由 IconGlyphFactory 解析，传入当前实时配置（裁切模式、Miuix 底垫及颜色等）
        iconView.setImageDrawable(IconGlyphFactory.resolve(getContext(), item, config));
        iconView.setColorFilter(null);
    }
}
