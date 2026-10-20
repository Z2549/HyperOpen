package io.github.qmryqmm.hyperopen.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.qmryqmm.hyperopen.config.HyperOpenConfig;
import io.github.qmryqmm.hyperopen.model.ShortcutItem;
import io.github.qmryqmm.hyperopen.view.CustomHyperOpenContainer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HyperOpenHook {
    private static final String TAG = "HyperOpen_Hook";
    private static final String CONTAINER_TAG = "hyperos_custom_hyperopen_container";

    /** 宿主 View 上的附加字段：本模块额外加到窗口上的 flag（用于复位时精确还原，不误删原生 flag） */
    private static final String FIELD_ADDED_FLAGS = "hyperOpenAddedWindowFlags";
    /** 宿主 View 上的附加字段：attach 状态监听器（卸载时摘除） */
    private static final String FIELD_ATTACH_LISTENER = "hyperOpenAttachListener";

    /** MiuiGxzwQuickOpenView$1 (mShowRunnable) 上的附加字段：标记「这是展开面板的 Runnable」 */
    private static final String FIELD_SHOW_RUNNABLE = "hyperOpenShowRunnable";

    /** 本模块在展开时为窗口追加的 flag */
    private static final int OUR_WINDOW_FLAGS =
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_BLUR_BEHIND;

    /**
     * 全部已安装 Hook 的卸载句柄。
     *
     * 安全模式在运行期被触发时，必须能立刻把已经装上去的 Hook 全部拆掉 ——
     * 只靠「下次启动不装」是不够的：用户此刻的 SystemUI 仍在崩溃，等待重启
     * 意味着还要再崩若干次。因此这里保存每一个 Unhook，供 {@link #uninstall()} 使用。
     */
    private static final List<XC_MethodHook.Unhook> HANDLES =
        Collections.synchronizedList(new ArrayList<XC_MethodHook.Unhook>());

    /**
     * Hook 是否处于生效状态。
     *
     * 所有回调入口都先查这个 volatile 标志：安全模式一旦触发，标志立刻置 false，
     * 即使某个回调正在执行、或卸载句柄尚未全部处理完，也不会再有任何本模块逻辑
     * 作用于 SystemUI。这是一次内存读取，不产生跨进程开销。
     */
    private static volatile boolean active = false;

    /** 已被注入 CustomHyperOpenContainer 的宿主 View（弱引用，避免拖住 SystemUI 视图树） */
    private static final List<WeakReference<FrameLayout>> HOSTS =
        Collections.synchronizedList(new ArrayList<WeakReference<FrameLayout>>());

    /**
     * 进程级广播接收器（配置热重载 / 息屏复位）。
     * 只注册一次：旧实现每构造一个 QuickOpenView 就注册一个接收器，既泄漏又会重复刷新。
     */
    private static Context receiverContext;
    private static BroadcastReceiver configReceiver;
    private static BroadcastReceiver screenReceiver;

    /** 缓存 MiuiGxzwUtils Class 引用，避免每次展开反射查找 */
    private static volatile Class<?> cachedGxzwUtilsClass;

    /**
     * 缓存「系统快捷面板弹出 Runnable」(MiuiGxzwQuickOpenView$1, $r8$classId=0) 的 Class 引用。
     *
     * 系统机制（逆向自系统界面_17.03.260226.r.apk 的 MiuiGxzwManager#dismissGxzwView）：
     *   指纹解锁成功后松手，若满足 fod_quick_open 开关等条件，会执行
     *   {@code mHandler.postDelayed(mShowRunnable, 500L | 1000L)} 再唤出快捷面板。
     *   本模块 Hook android.os.Handler#postDelayed，动态替换该 Runnable 的延迟，
     *   从而实现「指纹解锁后进入快捷方式的等待时间」可调。
     */
    private static volatile Class<?> cachedShowRunnableClass;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;

        // 每次部署前清空历史句柄，避免重复部署时句柄表里混入上一轮的失效项
        HANDLES.clear();
        active = true;

        // 配置异步刷新完成后（主线程回调），把新配置推给所有未处于展示态的容器。
        // 展示中的容器不热替换配置，避免手势进行中几何参数突变；下次展开会自动取最新缓存。
        HyperOpenConfig.setOnConfigUpdatedListener(new HyperOpenConfig.OnConfigUpdatedListener() {
            @Override
            public void onConfigUpdated(HyperOpenConfig config) {
                if (!active) return;
                for (FrameLayout host : snapshotHosts()) {
                    try {
                        CustomHyperOpenContainer c = getContainer(host);
                        if (c != null && c.getVisibility() != View.VISIBLE) {
                            c.updateConfig(config);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "推送新配置到容器失败", t);
                    }
                }
            }
        });

        try {
            final Class<?> quickOpenViewClass = XposedHelpers.findClass(
                "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenView", cl
            );

            // 1. Hook 构造函数：注入 CustomHyperOpenContainer 并挂载到 view 上
            record(XposedBridge.hookAllConstructors(quickOpenViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    FrameLayout view = (FrameLayout) param.thisObject;
                    Context context = view.getContext();

                    // 仅读进程内缓存（首轮以 XSharedPreferences 本地文件兜底），不做跨进程调用；
                    // 最新配置由后台线程异步拉取后再推送进来。
                    HyperOpenConfig config = HyperOpenConfig.getCached();
                    final CustomHyperOpenContainer container = new CustomHyperOpenContainer(context, config, cl);
                    container.setTag(CONTAINER_TAG);
                    container.setVisibility(View.GONE);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    view.addView(container, lp);
                    XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);
                    rememberHost(view);

                    // 标记系统 mShowRunnable，供 Handler#postDelayed 拦截器精准识别展开任务
                    markShowRunnable(view);

                    // 宿主窗口被移除（息屏、锁屏重建、指纹图层销毁等）时立刻复位，
                    // 绝不让 VISIBLE 容器 / 追加的窗口 flag 残留到下一次 attach。
                    armDetachReset(view);

                    // 注册配置实时更新与息屏复位广播监听（进程级，只注册一次）
                    ensureProcessReceivers(context);

                    // 后台异步预取最新配置
                    HyperOpenConfig.refreshAsync(context);

                    Log.i(TAG, "已成功向 MiuiGxzwQuickOpenView 注入 CustomHyperOpenContainer！");
                }
            }));

            // 2. Hook startShowQuickOpenItemAnimation：完全接管展开流程，阻止系统默认5项图标与扇形黑底
            record(XposedHelpers.findAndHookMethod(quickOpenViewClass, "startShowQuickOpenItemAnimation", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    FrameLayout view = (FrameLayout) param.thisObject;

                    // 完全阻止系统原生 startShowQuickOpenItemAnimation 执行 (避免创建原生5项View、动画和扇形黑底)
                    param.setResult(null);

                    // 展开路径只读进程内缓存：绝不在手势线程上同步调用 ContentProvider。
                    // 模块进程常被系统冻结，同步 call() 需先解冻，秒级阻塞会直接导致 SystemUI 输入 ANR。
                    // 配置变更由广播实时推送，这里仅做节流的后台兜底刷新。
                    HyperOpenConfig latestConfig = HyperOpenConfig.getCached();
                    HyperOpenConfig.refreshAsyncThrottled(view.getContext());

                    // 获取或创建 CustomHyperOpenContainer
                    CustomHyperOpenContainer container = (CustomHyperOpenContainer)
                        XposedHelpers.getAdditionalInstanceField(view, "customContainer");

                    // 若容器已处于展示状态，则无需重复初始化（也不重复下发模糊 / 窗口事务）
                    if (container != null && container.getVisibility() == View.VISIBLE) {
                        return;
                    }

                    // 全屏高斯模糊与全屏压暗独立渲染：模糊受 fullscreenBlurMaskAlpha 调控，压暗受 fullscreenDimAlpha 调控
                    try {
                        Object blurUtilsDelegate = XposedHelpers.getObjectField(view, "mBlurUtils");
                        if (blurUtilsDelegate != null) {
                            Object blurUtils = XposedHelpers.getObjectField(blurUtilsDelegate, "blurUtils");
                            if (blurUtils != null) {
                                Object vri = XposedHelpers.callMethod(view, "getViewRootImpl");
                                if (latestConfig.fullscreenBlurMaskAlpha <= 0.001f) {
                                    // 用户关闭全屏模糊：彻底移除全屏高斯模糊
                                    if (vri != null) {
                                        XposedHelpers.callMethod(blurUtils, "applyBlur", vri, 0);
                                    }
                                } else {
                                    // 用户开启全屏模糊：根据比例动态设置全屏高斯模糊半径
                                    float blurRatio = Math.max(0.0f, Math.min(1.0f, latestConfig.fullscreenBlurMaskAlpha));
                                    float blurRadius = (float) XposedHelpers.callMethod(blurUtils, "blurRadiusOfRatio", blurRatio);
                                    if (vri != null) {
                                        XposedHelpers.callMethod(blurUtils, "applyBlur", vri, (int) blurRadius);
                                    }
                                }
                            }
                        }

                        // 全屏压暗背景（0% ~ 100% 独立控制，与预览界面 Color.Black.copy(alpha = fullscreenDimAlpha) 1:1 严格对齐）
                        float dimRatio = Math.max(0.0f, Math.min(1.0f, latestConfig.fullscreenDimAlpha));
                        if (dimRatio <= 0.001f) {
                            view.setBackground(null);
                        } else {
                            int dimAlpha = Math.max(0, Math.min(255, Math.round(dimRatio * 255f)));
                            view.setBackgroundColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0));
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "设置全屏背景模糊与压暗失败", t);
                    }

                    // 允许触摸穿透并更新 WindowManager 布局标记 (开启 FLAG_BLUR_BEHIND 赋能局部模糊)
                    // 与原生 MiuiGxzwQuickOpenView.startShowQuickOpenItemAnimation 严格对齐：
                    // 若当前尚未 attach 到窗口，则置 mPendingUpdateLp = true，交由原生 onAttachedToWindow() 统一提交。
                    try {
                        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) XposedHelpers.getObjectField(view, "mLayoutParams");
                        if (lp != null) {
                            int missing = OUR_WINDOW_FLAGS & ~lp.flags;
                            if (missing != 0) {
                                lp.flags |= missing;
                                Object prev = XposedHelpers.getAdditionalInstanceField(view, FIELD_ADDED_FLAGS);
                                int added = (prev instanceof Integer ? (Integer) prev : 0) | missing;
                                XposedHelpers.setAdditionalInstanceField(view, FIELD_ADDED_FLAGS, added);
                                if (view.isAttachedToWindow()) {
                                    WindowManager wm = (WindowManager) XposedHelpers.getObjectField(view, "mWindowManager");
                                    if (wm != null) {
                                        wm.updateViewLayout(view, lp);
                                    }
                                } else {
                                    try {
                                        XposedHelpers.setBooleanField(view, "mPendingUpdateLp", true);
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "更新 LayoutParams 失败", t);
                    }

                    // 清理可能残余的原生 item 数据
                    try {
                        XposedHelpers.callMethod(view, "cleanQuickOpenItemList");
                    } catch (Throwable ignored) {}

                    // 隐藏原生的所有其它子 View (标题、副标题、教学提示等)
                    for (int i = 0; i < view.getChildCount(); i++) {
                        View child = view.getChildAt(i);
                        if (!CONTAINER_TAG.equals(child.getTag()) && child.getVisibility() != View.GONE) {
                            child.setVisibility(View.GONE);
                        }
                    }

                    if (container == null) {
                        container = new CustomHyperOpenContainer(view.getContext(), latestConfig, cl);
                        container.setTag(CONTAINER_TAG);
                        view.addView(container, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        ));
                        XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);
                        rememberHost(view);
                        armDetachReset(view);
                    }

                    // 每次展开前，热更新容器配置！
                    container.updateConfig(latestConfig);

                    // 获取 FOD 精准指纹中心坐标：
                    // 优先直接读取宿主在 initQuickViewParams 中已算好的 mFingerRect，避免每次展开调用 caculateGxzwIconSize
                    float fodX = -1f, fodY = -1f;
                    try {
                        Object fingerRectObj = XposedHelpers.getObjectField(view, "mFingerRect");
                        if (fingerRectObj instanceof RectF) {
                            RectF rect = (RectF) fingerRectObj;
                            if (rect.width() > 0 && rect.height() > 0) {
                                fodX = rect.centerX();
                                fodY = rect.centerY();
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (fodX <= 0f || fodY <= 0f) {
                        try {
                            Class<?> utilsClass = cachedGxzwUtilsClass;
                            if (utilsClass == null) {
                                utilsClass = XposedHelpers.findClass(
                                    "com.miui.keyguard.biometrics.fod.MiuiGxzwUtils", cl
                                );
                                cachedGxzwUtilsClass = utilsClass;
                            }
                            int iconX = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_X");
                            int iconY = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_Y");
                            int iconW = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_WIDTH");
                            int iconH = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_HEIGHT");
                            if (iconW <= 0 || iconH <= 0) {
                                XposedHelpers.callStaticMethod(utilsClass, "caculateGxzwIconSize", view.getContext());
                                iconX = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_X");
                                iconY = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_Y");
                                iconW = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_WIDTH");
                                iconH = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_HEIGHT");
                            }
                            fodX = iconX + iconW / 2.0f;
                            fodY = iconY + iconH / 2.0f;
                        } catch (Throwable t) {
                            Point displaySize = getDisplaySize(view.getContext());
                            fodX = displaySize.x / 2.0f;
                            fodY = displaySize.y * 0.795f;
                        }
                    }

                    // 异步上报实机真实 FOD 中心坐标至模块配置，供 App 预览 1:1 绝对对齐
                    HyperOpenConfig.reportRealFodCenterAsync(view.getContext(), fodX, fodY);

                    container.setVisibility(View.VISIBLE);
                    container.bringToFront();
                    container.setupAndExpand(fodX, fodY);
                    Log.i(TAG, "CustomHyperOpenContainer 展开成功，指纹中心: (" + fodX + ", " + fodY + ")");
                }
            }));

            // 3. Hook dismiss(int)：退出并复位自定义容器
            record(XposedHelpers.findAndHookMethod(quickOpenViewClass, "dismiss", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    resetHostState((FrameLayout) param.thisObject, "dismiss");
                }
            }));

            Log.i(TAG, "已成功安装 MiuiGxzwQuickOpenView 核心生命周期接管 Hook！");

        } catch (Throwable t) {
            Log.e(TAG, "Hook MiuiGxzwQuickOpenView 异常", t);
        }

        // 4. Hook MiuiGxzwIconView$2.onPointerEvent(MotionEvent) 拦截手势事件并驱动蜂窝鱼眼
        try {
            Class<?> iconView2Class = XposedHelpers.findClass(
                "com.miui.keyguard.biometrics.fod.MiuiGxzwIconView$2", cl
            );
            record(XposedHelpers.findAndHookMethod(iconView2Class, "onPointerEvent", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    try {
                        Object listener = param.thisObject;
                        final Object iconView = XposedHelpers.getObjectField(listener, "this$0");
                        if (iconView == null) return;

                        final Object quickOpenView = XposedHelpers.getObjectField(iconView, "mMiuiGxzwQuickOpenView");
                        if (quickOpenView == null) return;

                        final FrameLayout frameLayout = (FrameLayout) quickOpenView;
                        final CustomHyperOpenContainer container = (CustomHyperOpenContainer)
                            XposedHelpers.getAdditionalInstanceField(frameLayout, "customContainer");

                        // 只有在自定义快捷面板处于展示态时，才拦截后续手势
                        if (container == null || container.getVisibility() != View.VISIBLE) return;

                        MotionEvent event = (MotionEvent) param.args[0];
                        int action = event.getActionMasked();
                        final float rawX = event.getRawX();
                        final float rawY = event.getRawY();

                        if (action == MotionEvent.ACTION_MOVE) {
                            // 折叠 120Hz/240Hz 高频触摸采样：每帧最多在主线程执行一次鱼眼计算，零 Runnable 分配
                            container.scheduleFingerMove(rawX, rawY);
                            param.setResult(null); // 拦截原生滑动处理，避免原生逻辑干扰
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            final boolean isUp = (action == MotionEvent.ACTION_UP);
                            // 对齐原生 MiuiGxzwIconView$2：松手或取消时必须将 isCatchDownEvent 复位为 false
                            try {
                                XposedHelpers.setBooleanField(iconView, "isCatchDownEvent", false);
                            } catch (Throwable ignored) {}

                            Runnable finishTask = new Runnable() {
                                @Override
                                public void run() {
                                    // 对齐原生 Lambda4：通知 MiuiGxzwIconView 手指已抬起，确保传感器/动画状态完整收尾
                                    try {
                                        XposedHelpers.callMethod(iconView, "onTouchUp", true);
                                    } catch (Throwable ignored) {}

                                    ShortcutItem selected = null;
                                    if (isUp) {
                                        selected = container.consumeSelectedAndReset();
                                    } else {
                                        container.resetBubbles();
                                    }

                                    // 优先在主线程立刻关闭并移除悬浮面板窗口，随后在后台 IPC 线程异步发起应用启动：
                                    // 即使 system_server 因其他模块阻塞 startActivity，也绝不卡住 SystemUI 手势收尾与窗口销毁
                                    try {
                                        XposedHelpers.callMethod(quickOpenView, "dismiss", 0);
                                    } catch (Throwable t) {
                                        Log.e(TAG, "调用 dismiss 异常", t);
                                    }

                                    if (selected != null) {
                                        selected.executeAsync(frameLayout.getContext());
                                    }
                                }
                            };
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                finishTask.run();
                            } else {
                                frameLayout.post(finishTask);
                            }
                            param.setResult(null);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "onPointerEvent hook error", t);
                    }
                }
            }));
            Log.i(TAG, "已成功安装 MiuiGxzwIconView$2.onPointerEvent 触摸拦截 Hook！");
        } catch (Throwable t) {
            Log.e(TAG, "Hook MiuiGxzwIconView$2 失败", t);
        }

        // 5. Hook android.os.Handler#postDelayed：
        //    拦截系统在指纹解锁后调度 mShowRunnable 唤出快捷面板的延迟，替换为用户设定的等待阈值
        try {
            record(XposedHelpers.findAndHookMethod(
                android.os.Handler.class, "postDelayed",
                Runnable.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!active) return;
                        Runnable r = (Runnable) param.args[0];
                        long delay = (Long) param.args[1];
                        if (r == null || delay <= 0L) return;

                        // 跳过 Android 内部类（如 Invalidation/Elastic 等系统低频延迟任务），
                        // 只拦截 MiuiGxzwQuickOpenView$1 的展开 Runnable。
                        Class<?> showRunnableClass = cachedShowRunnableClass;
                        if (showRunnableClass == null) {
                            try {
                                showRunnableClass = XposedHelpers.findClass(
                                    "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenView$1", cl
                                );
                                cachedShowRunnableClass = showRunnableClass;
                            } catch (Throwable ignore) {
                                return;
                            }
                        }
                        if (!showRunnableClass.isInstance(r)) {
                            return;
                        }

                        // 该内类被 R8 复用为多种 Runnable，只有 classId=0 的实例才是「展开面板」任务：
                        // classId=1 是写入 fod_auth_fingerprint，classId=2 是设置 closeView 无障碍描述，
                        // 两者都不能被替换延迟。故还须读取本模块注入到内类上的标记字段做精准判定。
                        if (!isShowRunnableMarked(r)) {
                            return;
                        }

                        HyperOpenConfig cfg = HyperOpenConfig.getCached();
                        int userDelay = cfg != null ? Math.max(0, cfg.showDelayMs) : 500;
                        if (userDelay == 500) {
                            // 500ms = 系统默认：不干预，保持系统原生 delay（普通 500ms / 无障碍 1000ms）
                            Log.i(TAG, "快捷面板展开：系统默认，不干预原生延迟 " + delay + "ms");
                            return;
                        }
                        Log.i(TAG, "快捷面板展开：等待阈值 " + delay + "ms → " + userDelay + "ms");
                        param.args[1] = (long) userDelay;
                    }
                }
            ));
            Log.i(TAG, "已成功安装 Handler#postDelayed 指纹快捷等待阈值拦截 Hook！");
        } catch (Throwable t) {
            Log.e(TAG, "Hook Handler#postDelayed 失败", t);
        }
    }

    /**
     * 在 MiuiGxzwQuickOpenView 构造时，给其 mShowRunnable（$r8$classId=0 的 MiuiGxzwQuickOpenView$1）
     * 实例打上附加字段标记，供 postDelayed 拦截器精准识别。这样无需依赖被 R8 重排的字段布局，
     * 且能从根本上区分同被 R8 复用的 classId=1 / classId=2 两个长得一模一样的 Runnable。
     */
    private static void markShowRunnable(FrameLayout quickOpenView) {
        if (quickOpenView == null) return;
        try {
            Object runnable = XposedHelpers.getObjectField(quickOpenView, "mShowRunnable");
            if (runnable instanceof Runnable) {
                XposedHelpers.setAdditionalInstanceField(runnable, FIELD_SHOW_RUNNABLE, Boolean.TRUE);
            }
        } catch (Throwable t) {
            Log.w(TAG, "标记系统展开 Runnable 失败", t);
        }
    }

    private static boolean isShowRunnableMarked(Runnable r) {
        try {
            Object mark = XposedHelpers.getAdditionalInstanceField(r, FIELD_SHOW_RUNNABLE);
            return Boolean.TRUE.equals(mark);
        } catch (Throwable t) {
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 状态复位：dismiss / 息屏 / 窗口移除 统一走这里
    // ──────────────────────────────────────────────────────────────

    private static CustomHyperOpenContainer getContainer(FrameLayout host) {
        Object o = XposedHelpers.getAdditionalInstanceField(host, "customContainer");
        return o instanceof CustomHyperOpenContainer ? (CustomHyperOpenContainer) o : null;
    }

    /**
     * 把宿主恢复到「从未展开过」的状态：容器隐藏复位、全屏模糊清零、
     * 本模块追加的窗口 flag 精确还原。操作幂等，可重复调用。
     * 必须在主线程调用。
     */
    private static void resetHostState(FrameLayout view, String why) {
        if (view == null) return;

        CustomHyperOpenContainer container = getContainer(view);
        if (container != null) {
            try {
                container.resetBubbles();
            } catch (Throwable t) {
                Log.w(TAG, "复位气泡失败(" + why + ")", t);
            }
            if (container.getVisibility() != View.GONE) {
                container.setVisibility(View.GONE);
            }
        }

        // 原生 removeQuickView() 在移除窗口时已经调用过 applyBlur(vri, 0)；
        // 仅当宿主仍附着在窗口上时才补充清零全屏模糊，避免冗余跨线程/渲染调用。
        if (view.isAttachedToWindow()) {
            try {
                view.setBackground(null);
            } catch (Throwable ignored) {}
            try {
                Object blurUtilsDelegate = XposedHelpers.getObjectField(view, "mBlurUtils");
                if (blurUtilsDelegate != null) {
                    Object blurUtils = XposedHelpers.getObjectField(blurUtilsDelegate, "blurUtils");
                    if (blurUtils != null) {
                        Object vri = XposedHelpers.callMethod(view, "getViewRootImpl");
                        if (vri != null) {
                            XposedHelpers.callMethod(blurUtils, "applyBlur", vri, 0);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        restoreWindowFlags(view, why);
    }

    /**
     * 还原本模块在展开时追加到窗口 LayoutParams 上的 flag（FLAG_BLUR_BEHIND / FLAG_NOT_TOUCH_MODAL）。
     *
     * 只在窗口仍附着且可见时才下发 updateViewLayout；窗口已移除/不可见时仅修改
     * LayoutParams 对象本身，下次 addView / relayout 自然使用干净的 flag，
     * 不额外给 system_server 增加窗口事务。
     */
    private static void restoreWindowFlags(FrameLayout view, String why) {
        try {
            Object saved = XposedHelpers.getAdditionalInstanceField(view, FIELD_ADDED_FLAGS);
            if (!(saved instanceof Integer)) return;
            XposedHelpers.removeAdditionalInstanceField(view, FIELD_ADDED_FLAGS);
            int added = (Integer) saved;
            if (added == 0) return;

            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) XposedHelpers.getObjectField(view, "mLayoutParams");
            if (lp == null || (lp.flags & added) == 0) return;
            lp.flags &= ~added;

            if (view.isAttachedToWindow() && view.getWindowVisibility() == View.VISIBLE) {
                WindowManager wm = (WindowManager) XposedHelpers.getObjectField(view, "mWindowManager");
                if (wm != null) {
                    wm.updateViewLayout(view, lp);
                }
            }
            Log.i(TAG, "已还原窗口 flag(" + why + "): 0x" + Integer.toHexString(added));
        } catch (Throwable t) {
            Log.w(TAG, "还原窗口 flag 失败(" + why + ")", t);
        }
    }

    /** 宿主窗口被移除时立即复位，防止 VISIBLE 容器 / 追加 flag 残留到下一次 attach */
    private static void armDetachReset(final FrameLayout view) {
        try {
            if (XposedHelpers.getAdditionalInstanceField(view, FIELD_ATTACH_LISTENER) != null) {
                return;
            }
            View.OnAttachStateChangeListener l = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {}

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (!active) return;
                    try {
                        resetHostState((FrameLayout) v, "detached");
                    } catch (Throwable t) {
                        Log.w(TAG, "窗口移除复位失败", t);
                    }
                }
            };
            view.addOnAttachStateChangeListener(l);
            XposedHelpers.setAdditionalInstanceField(view, FIELD_ATTACH_LISTENER, l);
        } catch (Throwable t) {
            Log.w(TAG, "挂载窗口移除复位监听失败", t);
        }
    }

    /** 息屏时：若面板仍处于展示态，先走原生 dismiss 收尾，再无条件复位本模块状态 */
    private static void onScreenOff() {
        if (!active) return;
        for (FrameLayout host : snapshotHosts()) {
            try {
                CustomHyperOpenContainer c = getContainer(host);
                if (c != null && c.getVisibility() == View.VISIBLE) {
                    try {
                        XposedHelpers.callMethod(host, "dismiss", 0);
                    } catch (Throwable t) {
                        Log.w(TAG, "息屏时调用原生 dismiss 失败", t);
                    }
                }
                resetHostState(host, "screen_off");
            } catch (Throwable t) {
                Log.w(TAG, "息屏复位失败", t);
            }
        }
    }

    /**
     * 注册进程级广播：
     *   - 配置变更：仅触发后台异步拉取，主线程不做任何跨进程调用；
     *   - 息屏：复位所有宿主，杜绝预览状态直接锁屏后的残留。
     */
    private static synchronized void ensureProcessReceivers(Context context) {
        if (configReceiver != null || context == null) return;
        Context app = context.getApplicationContext();
        final Context ctx = app != null ? app : context;

        BroadcastReceiver cfg = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (!active) return;
                Log.i(TAG, "收到模块配置变更广播，后台异步热重载！");
                HyperOpenConfig.refreshAsync(c);
            }
        };
        BroadcastReceiver screen = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    onScreenOff();
                }
            }
        };

        try {
            IntentFilter f = new IntentFilter(HyperOpenConfig.ACTION_CONFIG_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(cfg, f, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(cfg, f);
            }
            configReceiver = cfg;
        } catch (Throwable t) {
            Log.w(TAG, "注册配置变更广播失败", t);
        }

        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(screen, f, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(screen, f);
            }
            screenReceiver = screen;
        } catch (Throwable t) {
            Log.w(TAG, "注册息屏广播失败", t);
        }

        receiverContext = ctx;
    }

    private static synchronized void releaseProcessReceivers() {
        if (receiverContext == null) return;
        if (configReceiver != null) {
            try { receiverContext.unregisterReceiver(configReceiver); } catch (Throwable ignored) {}
        }
        if (screenReceiver != null) {
            try { receiverContext.unregisterReceiver(screenReceiver); } catch (Throwable ignored) {}
        }
        configReceiver = null;
        screenReceiver = null;
        receiverContext = null;
    }

    private static List<FrameLayout> snapshotHosts() {
        List<FrameLayout> out = new ArrayList<>();
        synchronized (HOSTS) {
            for (WeakReference<FrameLayout> ref : HOSTS) {
                FrameLayout h = ref.get();
                if (h != null) out.add(h);
            }
        }
        return out;
    }

    // ──────────────────────────────────────────────────────────────
    // Hook 卸载：安全模式运行期即时停用
    // ──────────────────────────────────────────────────────────────

    /**
     * 把一次 hook 安装返回的句柄全部登记下来。
     * hookAllMethods / hookAllConstructors 返回的是句柄集合，hookMethod /
     * findAndHookMethod 返回单个句柄，这里统一收口，保证没有漏网的 Hook。
     */
    private static void record(Object result) {
        if (result == null) {
            return;
        }
        if (result instanceof XC_MethodHook.Unhook) {
            HANDLES.add((XC_MethodHook.Unhook) result);
        } else if (result instanceof Set) {
            for (Object o : (Set<?>) result) {
                if (o instanceof XC_MethodHook.Unhook) {
                    HANDLES.add((XC_MethodHook.Unhook) o);
                }
            }
        }
    }

    private static void rememberHost(FrameLayout view) {
        synchronized (HOSTS) {
            for (WeakReference<FrameLayout> ref : HOSTS) {
                if (ref.get() == view) {
                    return;
                }
            }
            HOSTS.add(new WeakReference<>(view));
        }
    }

    /**
     * 立刻停用全部 Hook —— 安全模式的执行端。
     *
     * 做三件事，缺一不可：
     *   1. 先把 active 置 false：从这一刻起所有回调直接 return，不再干预 SystemUI；
     *   2. 逐个 unhook：把回调从 Xposed 的钩子链上摘掉，彻底消除调用开销；
     *   3. 把已注入的 CustomHyperOpenContainer 从视图树里摘除并复位，
     *      否则界面上会残留一个半展开的自定义面板。
     *
     * 整个过程被 try/catch 逐项包裹：任何一步失败都不能影响其余步骤，
     * 更不能让清理动作本身成为新的崩溃源。
     *
     * @return 实际卸载成功的 Hook 数量
     */
    public static int uninstall() {
        // 1. 先断逻辑，后拆钩子：避免卸载过程中仍有回调进入本模块代码
        boolean wasActive = active;
        active = false;

        if (!wasActive && HANDLES.isEmpty()) {
            return 0;
        }
        Log.e(TAG, "!!! 安全模式触发：正在即时停用全部 Hook !!!");

        HyperOpenConfig.setOnConfigUpdatedListener(null);
        releaseProcessReceivers();

        // 2. 拆除视图树上已注入的自定义容器
        synchronized (HOSTS) {
            for (WeakReference<FrameLayout> ref : HOSTS) {
                FrameLayout host = ref.get();
                if (host == null) continue;
                try {
                    // 先还原模糊与窗口 flag，再摘除容器
                    resetHostState(host, "uninstall");
                    CustomHyperOpenContainer container = getContainer(host);
                    if (container != null) {
                        if (container.getParent() instanceof ViewGroup) {
                            ((ViewGroup) container.getParent()).removeView(container);
                        }
                    }
                    XposedHelpers.removeAdditionalInstanceField(host, "customContainer");
                    Object l = XposedHelpers.getAdditionalInstanceField(host, FIELD_ATTACH_LISTENER);
                    if (l instanceof View.OnAttachStateChangeListener) {
                        host.removeOnAttachStateChangeListener((View.OnAttachStateChangeListener) l);
                    }
                    XposedHelpers.removeAdditionalInstanceField(host, FIELD_ATTACH_LISTENER);
                } catch (Throwable t) {
                    Log.w(TAG, "移除已注入的自定义容器失败", t);
                }
            }
            HOSTS.clear();
        }

        // 3. 逐个卸载 Hook
        int removed = 0;
        List<XC_MethodHook.Unhook> snapshot;
        synchronized (HANDLES) {
            snapshot = new ArrayList<>(HANDLES);
            HANDLES.clear();
        }
        for (XC_MethodHook.Unhook u : snapshot) {
            try {
                Member m = u.getHookedMethod();
                u.unhook();
                removed++;
                if (m != null) {
                    Log.i(TAG, "已卸载 Hook: " + m.getDeclaringClass().getName() + "#" + m.getName());
                }
            } catch (Throwable t) {
                Log.w(TAG, "卸载 Hook 失败", t);
            }
        }

        Log.e(TAG, "安全模式已停用全部 Hook，共卸载 " + removed + " 个回调，SystemUI 已恢复原生行为");
        return removed;
    }

    private static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            display.getRealSize(size);
        }
        return size;
    }
}
