package io.github.qmryqmm.hyperopen.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Looper;
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

import io.github.qmryqmm.hyperopen.config.HyperOpenConfig;
import io.github.qmryqmm.hyperopen.factory.HyperOSMaterialFactory;
import io.github.qmryqmm.hyperopen.layout.CircularHoneycombPacking;
import io.github.qmryqmm.hyperopen.model.ShortcutItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomHyperOpenContainer extends FrameLayout {

    private static final OvershootInterpolator EXPAND_INTERPOLATOR = new OvershootInterpolator(1.2f);

    private HyperOpenConfig config;
    private final ClassLoader systemuiClassLoader;
    private int resolvedStyle;
    private int lastBuiltIconSizePx = -1;

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

    // 解锁时图标延迟展开任务：若 config.expandDelayMs > 0，等待指定毫秒后再展示面板与散开动画
    private volatile boolean isDelayWaiting = false;
    private final Runnable delayedExpandTask = new Runnable() {
        @Override
        public void run() {
            isDelayWaiting = false;
            if (getVisibility() == View.VISIBLE) {
                setAlpha(1.0f);
                playExpandAnimation();
            }
        }
    };

    // 240Hz 触摸采样折叠（Coalescing）：每帧最多向主线程投递一次 onFingerMove，实现零 Runnable 分配
    private volatile float pendingTouchX;
    private volatile float pendingTouchY;
    private final AtomicBoolean movePending = new AtomicBoolean(false);
    private final Runnable coalescedMoveTask = new Runnable() {
        @Override
        public void run() {
            movePending.set(false);
            if (getVisibility() == View.VISIBLE && !isDelayWaiting) {
                onFingerMove(pendingTouchX, pendingTouchY);
            }
        }
    };

    public CustomHyperOpenContainer(Context context, HyperOpenConfig config, ClassLoader classLoader) {
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
        regionBackdrop = new CircularRegionBackdropView(getContext(), systemuiClassLoader);
        regionBackdrop.setStyle(resolvedStyle);
        if (config != null) {
            regionBackdrop.setBlurRadius(config.regionBlurRadius);
                regionBackdrop.setDimAlpha(config.regionDimAlpha);
        }
        addView(regionBackdrop);

        // 图标容器 (允许子 View 放大溢出，不被矩形容器拦截)
        bubblesContainer = new FrameLayout(getContext());
        bubblesContainer.setClipChildren(false);
        bubblesContainer.setClipToPadding(false);
        setClipChildren(false);
        setClipToPadding(false);
        addView(bubblesContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void updateConfig(HyperOpenConfig newConfig) {
        if (newConfig != null) {
            this.config = newConfig;
            this.resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(getContext(), config.materialStyle);
            if (regionBackdrop != null) {
                regionBackdrop.setStyle(resolvedStyle);
                regionBackdrop.setBlurRadius(config.regionBlurRadius);
                regionBackdrop.setDimAlpha(config.regionDimAlpha);
            }
            // 顶部提示参数（位置、文字大小、居中、模糊与背景）热生效
            applyHeaderPillStyleAndLayout();
            // 气泡材质、模糊半径、留白比例与透明度热生效
            for (CircularBubbleView b : bubbles) {
                b.setMaterialStyle(resolvedStyle);
                b.setBlurRadius(config.bubbleBlurRadius);
                b.setDimAlpha(config.bubbleDimAlpha);
                b.setBubblePaddingPercent(config.bubblePaddingPercent);
                b.setIconAlpha(config.iconAlpha);
            }
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

    /**
     * 统一执行自定义震动反馈（支持开关、强弱、波形模式与自定义时长）
     */
    public static void performConfiguredHaptic(Vibrator vibrator, HyperOpenConfig cfg) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (cfg != null && !cfg.hapticEnabled) return;

        int mode = cfg != null ? cfg.hapticMode : HyperOpenConfig.HAPTIC_MODE_CLICK;
        int strength = cfg != null ? Math.max(1, Math.min(255, cfg.hapticStrength)) : 140;
        int customDur = cfg != null ? Math.max(4, Math.min(80, cfg.hapticDurationMs)) : 12;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect;
                switch (mode) {
                    case HyperOpenConfig.HAPTIC_MODE_TICK:
                        effect = VibrationEffect.createOneShot(7, Math.max(1, (int) (strength * 0.75f)));
                        break;
                    case HyperOpenConfig.HAPTIC_MODE_HEAVY:
                        effect = VibrationEffect.createOneShot(22, Math.min(255, (int) (strength * 1.25f)));
                        break;
                    case HyperOpenConfig.HAPTIC_MODE_DOUBLE:
                        effect = VibrationEffect.createWaveform(
                            new long[]{0, 9, 32, 11},
                            new int[]{0, Math.max(1, (int) (strength * 0.8f)), 0, strength},
                            -1
                        );
                        break;
                    case HyperOpenConfig.HAPTIC_MODE_CUSTOM:
                        effect = VibrationEffect.createOneShot(customDur, strength);
                        break;
                    case HyperOpenConfig.HAPTIC_MODE_CLICK:
                    default:
                        effect = VibrationEffect.createOneShot(12, strength);
                        break;
                }
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(customDur);
            }
        } catch (Throwable ignored) {}
    }

    private void triggerHaptic() {
        performConfiguredHaptic(vibrator, config);
    }

    private void buildHeader() {
        headerPill = new LinearLayout(getContext());
        headerPill.setOrientation(LinearLayout.VERTICAL);
        headerPill.setGravity(Gravity.CENTER);
        int padH = dpToPx(24);
        int padV = dpToPx(10);
        headerPill.setPadding(padH, padV, padH, padV);

        // 胶囊轮廓裁切：确保模糊/底衬都沿胶囊形渲染，alpha=0 禁用 3D 阴影多边形本影
        headerPill.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                if (w <= 0 || h <= 0) return;
                float r = Math.min(w, h) / 2f;
                outline.setRoundRect(0, 0, w, h, r);
                outline.setAlpha(0.0f);
            }
        });
        headerPill.setClipToOutline(headerPill.getBackground() != null);

        // 尺寸/样式变化时同步刷新背景与硬件模糊圆角
        headerPill.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int w = r - l;
            int h = b - t;
            if (w > 0 && h > 0 && (w != (or - ol) || h != (ob - ot))) {
                applyHeaderPillBackground();
            }
        });

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

        applyHeaderPillStyleAndLayout();
    }

    /**
     * 统一应用顶部提示的显隐、位置高度、字号大小、是否水平居中及胶囊毛玻璃背景
     */
    private void applyHeaderPillStyleAndLayout() {
        if (headerPill == null) return;
        boolean showHeader = config == null || config.showHeaderPill;
        headerPill.setVisibility(showHeader ? View.VISIBLE : View.GONE);

        int titleSp = config != null ? Math.max(12, Math.min(26, config.headerTitleSize)) : 17;
        int subtitleSp = config != null ? Math.max(9, Math.min(18, config.headerSubtitleSize)) : 11;
        boolean centered = config == null || config.headerTextCentered;
        int topMarginDp = config != null ? Math.max(20, Math.min(420, config.headerTopMargin)) : 100;

        if (headerTitle != null) {
            headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp);
            headerTitle.setGravity(centered ? Gravity.CENTER : Gravity.START);
        }
        if (headerSubtitle != null) {
            headerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSp);
            headerSubtitle.setGravity(centered ? Gravity.CENTER : Gravity.START);
        }
        headerPill.setGravity(centered ? Gravity.CENTER : (Gravity.START | Gravity.CENTER_VERTICAL));

        ViewGroup.LayoutParams rawLp = headerPill.getLayoutParams();
        LayoutParams lp = (rawLp instanceof LayoutParams)
            ? (LayoutParams) rawLp
            : new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = (centered ? Gravity.CENTER_HORIZONTAL : Gravity.START) | Gravity.TOP;
        lp.topMargin = dpToPx(topMarginDp);
        lp.leftMargin = centered ? 0 : dpToPx(24);
        lp.rightMargin = centered ? 0 : dpToPx(24);
        headerPill.setLayoutParams(lp);

        applyHeaderPillBackground();
    }

    /**
     * 按 showHeaderPillBg 开关与 headerBlurRadius 独立模糊半径铺设/移除顶部提示的胶囊玻璃背景。
     * 关闭时背景置 null 且 outline 置空，只保留标题/副标题纯文字悬浮。
     */
    private void applyHeaderPillBackground() {
        if (headerPill == null) return;
        boolean show = config == null || config.showHeaderPillBg;
        if (show) {
            int blurDp = config != null ? config.headerBlurRadius : 25;
            int blurPx = blurDp > 0 ? dpToPx(blurDp) : 0;
            headerPill.setBackground(HyperOSMaterialFactory.createCompositeHeaderPillDrawable(headerPill, resolvedStyle, blurPx, config != null ? config.headerDimAlpha : 0f));
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
        applyHeaderPillStyleAndLayout();
        if (headerPill.getParent() == this) {
            headerPill.bringToFront();
        }
        headerPill.invalidate();
    }

    /**
     * 判断当前已构建的气泡集合是否与最新启用项完全一致，一致则直接复用 View 实例避免重复 inflation 与 GC
     */
    private boolean canReuseExistingBubbles(List<ShortcutItem> activeItems, int iconSizePx) {
        if (lastBuiltIconSizePx != iconSizePx || bubbles.size() != activeItems.size() || bubbles.isEmpty()) {
            return false;
        }
        for (int i = 0; i < activeItems.size(); i++) {
            ShortcutItem a = bubbles.get(i).item;
            ShortcutItem b = activeItems.get(i);
            if (a == null || b == null) return false;
            if (!Objects.equals(a.id, b.id)
                    || !Objects.equals(a.title, b.title)
                    || !Objects.equals(a.subtitle, b.subtitle)
                    || !Objects.equals(a.actionType, b.actionType)
                    || !Objects.equals(a.actionPayload, b.actionPayload)
                    || !Objects.equals(a.iconPackage, b.iconPackage)
                    || !Objects.equals(a.iconGlyph, b.iconGlyph)
                    || a.tintColor != b.tintColor
                    || a.glyphPadEnabled != b.glyphPadEnabled
                    || a.glyphPadColor != b.glyphPadColor
                    || a.glyphPadMode != b.glyphPadMode
                    || a.appIconCropMode != b.appIconCropMode) {
                return false;
            }
        }
        return true;
    }

    /**
     * 设置指纹中心并根据最新配置构建网格与散开动画
     */
    public void setupAndExpand(float fodX, float fodY) {
        this.fodCenterX = fodX;
        this.fodCenterY = fodY;
        this.currentSelectedBubble = null;
        removeCallbacks(delayedExpandTask);
        isDelayWaiting = false;

        // 重置顶部提示文字为初始状态
        if (headerTitle != null && headerSubtitle != null) {
            headerTitle.setText("滑动选择快捷方式");
            headerTitle.setTextColor(Color.WHITE);
            headerSubtitle.setText("移至目标图标后松手直接打开");
        }

        // 胶囊头部自愈：确保每次展开时胶囊背景都是胶囊形且在最上层
        ensureHeaderPillIntact();

        float regionDiameterPx = dpToPx(config.regionDiameter);
        float regionRadiusPx = regionDiameterPx / 2f;
        float regionCenterY = fodCenterY - dpToPx(config.offsetY);
        float iconSizePx = dpToPx(config.iconSize);
        int iconSizeInt = (int) iconSizePx;

        // 更新底衬大小与位置
        regionBackdrop.setVisibility(config.showRegionBg ? View.VISIBLE : View.GONE);
        regionBackdrop.setStyle(resolvedStyle);
        regionBackdrop.setBlurRadius(config.regionBlurRadius);
                regionBackdrop.setDimAlpha(config.regionDimAlpha);
        regionBackdrop.updateGeometry(regionRadiusPx);

        ViewGroup.LayoutParams existingRLp = regionBackdrop.getLayoutParams();
        LayoutParams rLp = (existingRLp instanceof LayoutParams)
            ? (LayoutParams) existingRLp
            : new LayoutParams((int) regionDiameterPx, (int) regionDiameterPx);
        rLp.width = (int) regionDiameterPx;
        rLp.height = (int) regionDiameterPx;
        rLp.leftMargin = (int) (fodCenterX - regionRadiusPx);
        rLp.topMargin = (int) (regionCenterY - regionRadiusPx);
        regionBackdrop.setLayoutParams(rLp);

        // 筛选启用的快捷项
        List<ShortcutItem> activeItems = new ArrayList<>(config.shortcuts.size());
        for (ShortcutItem item : config.shortcuts) {
            if (item.enabled) activeItems.add(item);
        }

        // 计算圆形蜂窝坐标（传入紧凑度偏移量）
        float spacingOffsetPx = dpToPx(config.iconSpacingOffset);
        List<PointF> points = CircularHoneycombPacking.calculatePoints(
            activeItems.size(),
            fodCenterX,
            regionCenterY,
            regionRadiusPx,
            iconSizePx,
            spacingOffsetPx
        );

        if (canReuseExistingBubbles(activeItems, iconSizeInt)) {
            // 复用已有气泡实例：仅更新目标坐标、Margin 与模糊通道，消除展开时的 View 树重建开销
            for (int i = 0; i < points.size(); i++) {
                CircularBubbleView bubble = bubbles.get(i);
                PointF pt = points.get(i);
                ShortcutItem item = activeItems.get(i);
                float finalX = pt.x + dpToPx(item.customOffsetX);
                float finalY = pt.y + dpToPx(item.customOffsetY);
                bubble.targetX = finalX;
                bubble.targetY = finalY;
                bubble.setMaterialStyle(resolvedStyle);
                bubble.setBlurRadius(config.bubbleBlurRadius);
            bubble.setDimAlpha(config.bubbleDimAlpha);
                bubble.setBubblePaddingPercent(config.bubblePaddingPercent);
                bubble.setIconAlpha(config.iconAlpha);
                bubble.ensureBlurAndRefreshBackground();

                LayoutParams bLp = (LayoutParams) bubble.getLayoutParams();
                int newLeft = (int) (finalX - iconSizePx / 2f);
                int newTop = (int) (finalY - iconSizePx / 2f);
                if (bLp.leftMargin != newLeft || bLp.topMargin != newTop || bLp.width != iconSizeInt || bLp.height != iconSizeInt) {
                    bLp.width = iconSizeInt;
                    bLp.height = iconSizeInt;
                    bLp.leftMargin = newLeft;
                    bLp.topMargin = newTop;
                    bubble.setLayoutParams(bLp);
                }
            }
        } else {
            bubblesContainer.removeAllViews();
            bubbles.clear();
            lastBuiltIconSizePx = iconSizeInt;

            // 实例化圆形气泡
            for (int i = 0; i < points.size(); i++) {
                ShortcutItem item = activeItems.get(i);
                PointF pt = points.get(i);
                float finalX = pt.x + dpToPx(item.customOffsetX);
                float finalY = pt.y + dpToPx(item.customOffsetY);

                CircularBubbleView bubble = new CircularBubbleView(
                    getContext(), item, resolvedStyle, iconSizeInt, systemuiClassLoader, config
                );
                bubble.targetX = finalX;
                bubble.targetY = finalY;

                LayoutParams bLp = new LayoutParams(iconSizeInt, iconSizeInt);
                bLp.leftMargin = (int) (finalX - iconSizePx / 2f);
                bLp.topMargin = (int) (finalY - iconSizePx / 2f);
                bubble.setLayoutParams(bLp);

                bubblesContainer.addView(bubble);
                bubbles.add(bubble);
            }
        }

        int delayMs = config != null ? Math.max(0, config.expandDelayMs) : 0;
        if (delayMs > 0) {
            // 延迟展开：等待期内隐藏容器与气泡，若用户在此期间提前松手则不展示快捷图标
            isDelayWaiting = true;
            setAlpha(0f);
            for (int i = 0; i < bubbles.size(); i++) {
                CircularBubbleView b = bubbles.get(i);
                b.animate().cancel();
                b.setAlpha(0f);
            }
            postDelayed(delayedExpandTask, delayMs);
        } else {
            setAlpha(1.0f);
            playExpandAnimation();
        }
    }

    private void playExpandAnimation() {
        for (int i = 0; i < bubbles.size(); i++) {
            final CircularBubbleView b = bubbles.get(i);
            b.animate().cancel();
            float startTranslationX = fodCenterX - b.targetX;
            float startTranslationY = fodCenterY - b.targetY;

            b.setTranslationX(startTranslationX);
            b.setTranslationY(startTranslationY);
            b.setScaleX(0.1f);
            b.setScaleY(0.1f);
            b.setAlpha(0f);
            b.syncBlurCornerRadiusWithViewScale();

            // SkRRect::setRectRadii 在 RenderThread 合成时自动钳制圆角保持 100% 正圆，
            // 无需设置每帧 ValueAnimator UpdateListener 反射调用 updateCornerRadius
            b.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(EXPAND_INTERPOLATOR)
                .setDuration(320)
                .setStartDelay(i * 15L)
                .setUpdateListener(null)
                .withEndAction(() -> b.applyScale(1.0f))
                .start();
        }
    }

    /**
     * 供手势监听线程（如 MiuiGestureMonitor binder 线程）高频调用：
     * 折叠多个 120Hz/240Hz ACTION_MOVE 采样点，每帧最多在主线程执行一次 {@link #onFingerMove(float, float)}，
     * 零 Runnable 对象分配。
     */
    public void scheduleFingerMove(float rawX, float rawY) {
        this.pendingTouchX = rawX;
        this.pendingTouchY = rawY;
        if (isDelayWaiting) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            movePending.set(false);
            onFingerMove(rawX, rawY);
        } else if (movePending.compareAndSet(false, true)) {
            post(coalescedMoveTask);
        }
    }

    /**
     * 手指滑动时调用：计算实时鱼眼动力学与命中目标
     */
    public void onFingerMove(float touchX, float touchY) {
        if (isDelayWaiting) return;
        float influenceRadiusPx = dpToPx(config.influenceRadius);
        float iconSizePx = dpToPx(config.iconSize);
        float hitDistanceThreshold = iconSizePx * 0.95f;

        CircularBubbleView closest = null;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0, n = bubbles.size(); i < n; i++) {
            CircularBubbleView b = bubbles.get(i);
            float dx = touchX - b.targetX;
            float dy = touchY - b.targetY;
            float dist = (float) Math.hypot(dx, dy);
            if (dist < minDistance) {
                minDistance = dist;
                closest = b;
            }

            // 连续余弦鱼眼缩放与动态放大透明度曲线
            float scale = 1.0f;
            float currentAlpha = config.iconAlpha;
            if (dist < influenceRadiusPx) {
                float factor = (float) Math.cos((dist / influenceRadiusPx) * (Math.PI / 2.0));
                scale = 1.0f + (config.maxScale - 1.0f) * factor;
                currentAlpha = config.iconAlpha + (config.fisheyeZoomAlpha - config.iconAlpha) * factor;
            }
            b.applyScale(scale);
            b.setIconAlpha(currentAlpha);
        }

        // 判定选中项
        if (closest != null && minDistance < hitDistanceThreshold) {
            if (currentSelectedBubble != closest) {
                if (currentSelectedBubble != null) {
                    currentSelectedBubble.setSelectedState(false);
                }
                closest.setSelectedState(true);
                // 将被选中的气泡置于最顶层，避免放大时被相邻气泡遮挡
                closest.bringToFront();
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
     * 提取当前选中的快捷方式、触发触觉反馈并立即复位气泡状态（供 SystemUI 异步分发启动使用，避免阻塞主线程 dismiss）
     */
    public ShortcutItem consumeSelectedAndReset() {
        ShortcutItem selected = null;
        if (!isDelayWaiting && currentSelectedBubble != null) {
            selected = currentSelectedBubble.item;
            triggerHaptic();
        }
        resetBubbles();
        return selected;
    }

    /**
     * 手指松开时调用：执行目标动作或取消
     */
    public void onFingerUp() {
        ShortcutItem selected = consumeSelectedAndReset();
        if (selected != null) {
            selected.execute(getContext());
        }
    }

    public void resetBubbles() {
        isDelayWaiting = false;
        removeCallbacks(delayedExpandTask);
        movePending.set(false);
        removeCallbacks(coalescedMoveTask);
        setAlpha(1.0f);
        for (int i = 0, n = bubbles.size(); i < n; i++) {
            CircularBubbleView b = bubbles.get(i);
            b.setSelectedState(false);
            b.applyScale(1.0f);
            b.setIconAlpha(config != null ? config.iconAlpha : 1.0f);
            b.resetBlur();
        }
        currentSelectedBubble = null;
        if (regionBackdrop != null) {
            regionBackdrop.reset();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
