package com.hyperos.quickopen.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.factory.HyperOSMaterialFactory;
import com.hyperos.quickopen.layout.CircularHoneycombPacking;
import com.hyperos.quickopen.model.ShortcutItem;

import java.util.ArrayList;
import java.util.List;

public class CustomQuickOpenContainer extends FrameLayout {

    private QuickOpenConfig config;
    private final ClassLoader systemuiClassLoader;
    private int resolvedStyle;

    private float fodCenterX = 540f;
    private float fodCenterY = 1920f;

    private LinearLayout headerPill;
    private TextView headerTitle;
    private TextView headerSubtitle;

    private CircularRegionBackdropView regionBackdrop;
    private FrameLayout bubblesContainer;
    private final List<CircularBubbleView> bubbles = new ArrayList<>();
    private CircularBubbleView currentSelectedBubble = null;

    private Vibrator vibrator;

    public CustomQuickOpenContainer(Context context, QuickOpenConfig config, ClassLoader classLoader) {
        super(context);
        this.config = config;
        this.systemuiClassLoader = classLoader;
        init();
    }

    private void init() {
        resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(getContext(), config.materialStyle);
        initVibrator();
        buildHeader();

        // 圆形区域底衬
        regionBackdrop = new CircularRegionBackdropView(getContext());
        regionBackdrop.setStyle(resolvedStyle);
        addView(regionBackdrop);

        // 图标容器
        bubblesContainer = new FrameLayout(getContext());
        addView(bubblesContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void updateConfig(QuickOpenConfig newConfig) {
        if (newConfig != null) {
            this.config = newConfig;
            this.resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(getContext(), config.materialStyle);
            if (regionBackdrop != null) {
                regionBackdrop.setStyle(resolvedStyle);
            }
            // 胶囊背景开关热生效
            applyHeaderPillBackground();
        }
    }

    private void initVibrator() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (Throwable ignored) {}
    }

    private void triggerHaptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(12);
            }
        } catch (Throwable ignored) {}
    }

    private void buildHeader() {
        headerPill = new LinearLayout(getContext());
        headerPill.setOrientation(LinearLayout.VERTICAL);
        headerPill.setGravity(Gravity.CENTER);
        int padH = dpToPx(24);
        int padV = dpToPx(10);
        headerPill.setPadding(padH, padV, padH, padV);

        // 胶囊轮廓裁切：确保模糊/底衬都沿胶囊形渲染，防止被画成圆形
        // （背景本体按 showHeaderPillBg 开关在 applyHeaderPillBackground() 中决定是否铺设）
        headerPill.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                if (w <= 0 || h <= 0) return;
                if (view.getBackground() == null) {
                    // 无背景时给一个真实胶囊 outline 而非空 outline：
                    // clipToOutline 会在空 outline 时把整个 View 内容裁没（文字消失根因），
                    // 配合 applyHeaderPillBackground() 中的 setClipToOutline(false) 使用
                    outline.setRoundRect(0, 0, w, h, Math.min(w, h) / 2f);
                    return;
                }
                float r = Math.min(w, h) / 2f;
                outline.setRoundRect(0, 0, w, h, r);
            }
        });
        // 只有存在背景时才沿 outline 裁切；无背景时必须关闭，否则内容被裁空
        headerPill.setClipToOutline(headerPill.getBackground() != null);

        // 尺寸/样式变化时同步刷新背景，避免背景残留在过期 bounds 上
        headerPill.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (v.getWidth() > 0 && v.getHeight() > 0 && v.getBackground() instanceof HyperOSMaterialFactory.HeaderPillDrawable) {
                v.getBackground().invalidateSelf();
            }
        });

        applyHeaderPillBackground();

        headerTitle = new TextView(getContext());
        headerTitle.setText("滑动选择快捷方式");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        headerTitle.getPaint().setFakeBoldText(true);
        headerPill.addView(headerTitle);

        headerSubtitle = new TextView(getContext());
        headerSubtitle.setText("移至目标图标后松手直接打开");
        headerSubtitle.setTextColor(0xAAFFFFFF);
        headerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        headerPill.addView(headerSubtitle);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        lp.topMargin = dpToPx(100);
        addView(headerPill, lp);
    }

    /**
     * 按 showHeaderPillBg 开关铺设/移除顶部提示的胶囊玻璃背景。
     * 关闭时背景置 null 且 outline 置空，只保留标题/副标题纯文字悬浮。
     */
    private void applyHeaderPillBackground() {
        if (headerPill == null) return;
        boolean show = config == null || config.showHeaderPillBg;
        if (show) {
            if (!(headerPill.getBackground() instanceof HyperOSMaterialFactory.HeaderPillDrawable)) {
                headerPill.setBackground(HyperOSMaterialFactory.createHeaderPillDrawable(resolvedStyle));
            }
            headerPill.setClipToOutline(true);
        } else {
            if (headerPill.getBackground() != null) {
                headerPill.setBackground(null);
            }
            // 关键：必须先关掉裁切再清背景，否则空 outline 会把整个 View 内容裁没
            headerPill.setClipToOutline(false);
        }
        headerPill.invalidateOutline();
        headerPill.invalidate();
    }

    /**
     * 展开前强制恢复胶囊头部的胶囊形背景与 Z 序：
     * 配置热重载、原生提示 View 竞争注入等场景都可能导致
     * headerPill 背景被覆盖或形状回落为圆形，这里统一自愈。
     * showHeaderPillBg 关闭时反向自愈：确保背景保持移除状态。
     */
    public void ensureHeaderPillIntact() {
        if (headerPill == null) return;
        applyHeaderPillBackground();
        if (headerPill.getVisibility() != View.VISIBLE) {
            headerPill.setVisibility(View.VISIBLE);
        }
        if (headerPill.getParent() == this) {
            headerPill.bringToFront();
        }
        headerPill.invalidate();
    }

    /**
     * 设置指纹中心并根据最新配置构建网格与散开动画
     */
    public void setupAndExpand(float fodX, float fodY) {
        this.fodCenterX = fodX;
        this.fodCenterY = fodY;

        // 胶囊头部自愈：确保每次展开时胶囊背景都是胶囊形且在最上层
        ensureHeaderPillIntact();

        bubblesContainer.removeAllViews();
        bubbles.clear();
        currentSelectedBubble = null;

        float regionDiameterPx = dpToPx(config.regionDiameter);
        float regionRadiusPx = regionDiameterPx / 2f;
        float regionCenterY = fodCenterY - dpToPx(config.offsetY);
        float iconSizePx = dpToPx(config.iconSize);

        // 更新底衬大小与位置
        regionBackdrop.setVisibility(config.showRegionBg ? View.VISIBLE : View.GONE);
        LayoutParams rLp = new LayoutParams((int) regionDiameterPx, (int) regionDiameterPx);
        rLp.leftMargin = (int) (fodCenterX - regionRadiusPx);
        rLp.topMargin = (int) (regionCenterY - regionRadiusPx);
        regionBackdrop.setLayoutParams(rLp);

        // 筛选启用的快捷项
        List<ShortcutItem> activeItems = new ArrayList<>();
        for (ShortcutItem item : config.shortcuts) {
            if (item.enabled) activeItems.add(item);
        }

        // 计算圆形蜂窝坐标
        List<PointF> points = CircularHoneycombPacking.calculatePoints(
            activeItems.size(),
            fodCenterX,
            regionCenterY,
            regionRadiusPx,
            iconSizePx
        );

        // 实例化圆形气泡
        for (int i = 0; i < points.size(); i++) {
            ShortcutItem item = activeItems.get(i);
            PointF pt = points.get(i);

            CircularBubbleView bubble = new CircularBubbleView(
                getContext(), item, resolvedStyle, (int) iconSizePx, systemuiClassLoader, config
            );
            bubble.targetX = pt.x;
            bubble.targetY = pt.y;

            LayoutParams bLp = new LayoutParams((int) iconSizePx, (int) iconSizePx);
            bLp.leftMargin = (int) (pt.x - iconSizePx / 2f);
            bLp.topMargin = (int) (pt.y - iconSizePx / 2f);
            bubble.setLayoutParams(bLp);

            bubblesContainer.addView(bubble);
            bubbles.add(bubble);
        }

        // 播放散开弹射动画 (从指纹中心散开)
        playExpandAnimation(iconSizePx);
    }

    private void playExpandAnimation(float iconSizePx) {
        for (int i = 0; i < bubbles.size(); i++) {
            CircularBubbleView b = bubbles.get(i);
            float startTranslationX = fodCenterX - b.targetX;
            float startTranslationY = fodCenterY - b.targetY;

            b.setTranslationX(startTranslationX);
            b.setTranslationY(startTranslationY);
            b.setScaleX(0.1f);
            b.setScaleY(0.1f);
            b.setAlpha(0f);

            b.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setDuration(320)
                .setStartDelay(i * 15L)
                .start();
        }
    }

    /**
     * 手指滑动时调用：计算实时鱼眼动力学与命中目标
     */
    public void onFingerMove(float touchX, float touchY) {
        float influenceRadiusPx = dpToPx(config.influenceRadius);
        float iconSizePx = dpToPx(config.iconSize);
        float hitDistanceThreshold = iconSizePx * 0.95f;

        CircularBubbleView closest = null;
        float minDistance = Float.MAX_VALUE;

        for (CircularBubbleView b : bubbles) {
            float dist = (float) Math.hypot(touchX - b.targetX, touchY - b.targetY);
            if (dist < minDistance) {
                minDistance = dist;
                closest = b;
            }

            // 连续余弦鱼眼缩放曲线
            float scale = 1.0f;
            if (dist < influenceRadiusPx) {
                float factor = (float) Math.cos((dist / influenceRadiusPx) * (Math.PI / 2.0));
                scale = 1.0f + (config.maxScale - 1.0f) * factor;
            }
            b.applyScale(scale);
        }

        // 判定选中项
        if (closest != null && minDistance < hitDistanceThreshold) {
            if (currentSelectedBubble != closest) {
                if (currentSelectedBubble != null) {
                    currentSelectedBubble.setSelectedState(false);
                }
                closest.setSelectedState(true);
                currentSelectedBubble = closest;

                // 更新顶部标题
                headerTitle.setText(closest.item.title);
                headerTitle.setTextColor(closest.item.tintColor);
                headerSubtitle.setText(closest.item.subtitle);

                triggerHaptic();
            }
        } else {
            if (currentSelectedBubble != null) {
                currentSelectedBubble.setSelectedState(false);
                currentSelectedBubble = null;
                headerTitle.setText("滑动选择快捷方式");
                headerTitle.setTextColor(Color.WHITE);
                headerSubtitle.setText("移至目标图标后松手直接打开");
            }
        }
    }

    /**
     * 手指松开时调用：执行目标动作或取消
     */
    public void onFingerUp() {
        if (currentSelectedBubble != null) {
            // 执行动作
            currentSelectedBubble.item.execute(getContext());
            triggerHaptic();
        }
        resetBubbles();
    }

    public void resetBubbles() {
        for (CircularBubbleView b : bubbles) {
            b.setSelectedState(false);
            b.applyScale(1.0f);
        }
        currentSelectedBubble = null;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
