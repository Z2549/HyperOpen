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

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader, QuickOpenConfig config) {
        super(context);
        this.item = item;
        this.activeStyle = style;
        this.config = config;

        setLayoutParams(new LayoutParams(sizePx, sizePx));
        setClipChildren(false);

        // 设置纯正圆形 OutlineProvider，并在放大时以真正圆形向外扩张，使用 sizePx 兜底初次未测量状态，绝不裁空图标
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                int w = view.getWidth() > 0 ? view.getWidth() : sizePx;
                int h = view.getHeight() > 0 ? view.getHeight() : sizePx;
                outline.setOval(0, 0, w, h);
            }
        });
        setClipToOutline(true);

        // 应用纯正圆形光学材质 Drawable (100% 纯 Canvas 正圆形着色渲染，彻底杜绝系统多边形干扰)
        refreshBackground();

        // 中心图标
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // 留白比例计算：直接按用户设置的留白滑块百分比计算（0% 即彻底满铺，无需单独开关）
        int paddingPercent = config != null ? config.bubblePaddingPercent : 18;
        int iconPadding = (int) (sizePx * (paddingPercent / 100f));
        iconView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

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

    private void refreshBackground() {
        setBackground(HyperOSMaterialFactory.createCircularBubbleDrawable(activeStyle, item.tintColor, isSelected));
    }

    public void setMaterialStyle(int style) {
        this.activeStyle = style;
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            invalidateOutline();
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
