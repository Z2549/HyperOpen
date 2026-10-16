package com.hyperos.quickopen.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.hyperos.quickopen.factory.HyperOSMaterialFactory;
import com.hyperos.quickopen.model.ShortcutItem;

public class CircularBubbleView extends FrameLayout {

    public final ShortcutItem item;
    public float targetX;
    public float targetY;
    public float currentScale = 1.0f;
    private boolean isSelected = false;

    private int activeStyle;
    private final ImageView iconView;
    private View systemBlurView;

    public CircularBubbleView(@NonNull Context context, ShortcutItem item, int style, int sizePx, ClassLoader classLoader) {
        super(context);
        this.item = item;
        this.activeStyle = style;

        setLayoutParams(new LayoutParams(sizePx, sizePx));
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
        int iconPadding = (int) (sizePx * 0.22f);
        iconView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

        // 绑定图标 (优先从包名解析应用图标，或使用默认彩色矢量图形)
        loadItemIcon();

        LayoutParams iconLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        iconLp.gravity = Gravity.CENTER;
        addView(iconView, iconLp);
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
            // 选中时图标轻微放大与提亮
            iconView.animate()
                .scaleX(selected ? 1.12f : 1.0f)
                .scaleY(selected ? 1.12f : 1.0f)
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
        try {
            if (ShortcutItem.ACTION_APP.equals(item.actionType)) {
                Drawable appIcon = getContext().getPackageManager().getApplicationIcon(item.actionPayload);
                iconView.setImageDrawable(appIcon);
                return;
            }
        } catch (Throwable ignored) {}

        // 默认按图标 tint 渲染内置系统符号
        iconView.setColorFilter(item.tintColor);
        // 使用内置圆形矢量图
        iconView.setImageResource(android.R.drawable.ic_menu_compass);
    }
}
