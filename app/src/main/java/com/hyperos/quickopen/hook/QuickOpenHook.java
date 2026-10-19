package com.hyperos.quickopen.hook;

import android.content.Context;
import android.graphics.Point;
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

import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.view.CustomQuickOpenContainer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QuickOpenHook {
    private static final String TAG = "QuickOpen_Hook";
    private static final String CONTAINER_TAG = "hyperos_custom_quickopen_container";

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

    /** 已被注入 CustomQuickOpenContainer 的宿主 View（弱引用，避免拖住 SystemUI 视图树） */
    private static final List<WeakReference<FrameLayout>> HOSTS =
        Collections.synchronizedList(new ArrayList<WeakReference<FrameLayout>>());

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;

        // 每次部署前清空历史句柄，避免重复部署时句柄表里混入上一轮的失效项
        HANDLES.clear();
        active = true;

        try {
            final Class<?> quickOpenViewClass = XposedHelpers.findClass(
                "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenView", cl
            );

            // 1. Hook 构造函数：注入 CustomQuickOpenContainer 并挂载到 view 上
            record(XposedBridge.hookAllConstructors(quickOpenViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    FrameLayout view = (FrameLayout) param.thisObject;
                    Context context = view.getContext();

                    QuickOpenConfig config = QuickOpenConfig.load(context);
                    final CustomQuickOpenContainer container = new CustomQuickOpenContainer(context, config, cl);
                    container.setTag(CONTAINER_TAG);
                    container.setVisibility(View.GONE);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    view.addView(container, lp);
                    XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);
                    rememberHost(view);

                    // 注册配置实时更新广播监听
                    try {
                        android.content.IntentFilter filter = new android.content.IntentFilter(QuickOpenConfig.ACTION_CONFIG_CHANGED);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.registerReceiver(new android.content.BroadcastReceiver() {
                                @Override
                                public void onReceive(Context ctx, android.content.Intent intent) {
                                    Log.i(TAG, "收到模块配置变更广播，实时热重载！");
                                    QuickOpenConfig newConfig = QuickOpenConfig.load(ctx);
                                    container.updateConfig(newConfig);
                                }
                            }, filter, Context.RECEIVER_EXPORTED);
                        } else {
                            context.registerReceiver(new android.content.BroadcastReceiver() {
                                @Override
                                public void onReceive(Context ctx, android.content.Intent intent) {
                                    Log.i(TAG, "收到模块配置变更广播，实时热重载！");
                                    QuickOpenConfig newConfig = QuickOpenConfig.load(ctx);
                                    container.updateConfig(newConfig);
                                }
                            }, filter);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "注册配置变更广播失败", t);
                    }

                    Log.i(TAG, "已成功向 MiuiGxzwQuickOpenView 注入 CustomQuickOpenContainer！");
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

                    // 每次展开前，实时从 ContentProvider 同步最新配置
                    QuickOpenConfig latestConfig = QuickOpenConfig.load(view.getContext());

                    // 全屏高斯模糊与全屏遮罩独立渲染：只受 fullscreenBlurMaskAlpha 调控
                    try {
                        Object blurUtilsDelegate = XposedHelpers.getObjectField(view, "mBlurUtils");
                        if (blurUtilsDelegate != null) {
                            Object blurUtils = XposedHelpers.getObjectField(blurUtilsDelegate, "blurUtils");
                            if (blurUtils != null) {
                                Object vri = XposedHelpers.callMethod(view, "getViewRootImpl");
                                if (latestConfig.fullscreenBlurMaskAlpha <= 0.001f) {
                                    // 用户关闭全屏模糊遮罩：彻底移除全屏模糊，确保背景完全清晰透亮
                                    if (vri != null) {
                                        XposedHelpers.callMethod(blurUtils, "applyBlur", vri, 0);
                                    }
                                    view.setBackground(null);
                                    Log.i(TAG, "已独立关闭全屏模糊遮罩，全屏壁纸保持原始清晰，圆形视界由局部引擎独立模糊！");
                                } else {
                                    // 用户开启全屏模糊遮罩：根据比例动态设置全屏模糊与系统暗色底盘
                                    float blurRatio = Math.max(0.0f, Math.min(1.0f, latestConfig.fullscreenBlurMaskAlpha));
                                    float blurRadius = (float) XposedHelpers.callMethod(blurUtils, "blurRadiusOfRatio", blurRatio);
                                    if (vri != null) {
                                        XposedHelpers.callMethod(blurUtils, "applyBlur", vri, (int) blurRadius);
                                    }
                                    try {
                                        view.setBackgroundResource(0x7f080b49);
                                        if (view.getBackground() != null) {
                                            int maskAlpha = (int) (latestConfig.fullscreenBlurMaskAlpha * 255f);
                                            view.getBackground().setAlpha(maskAlpha);
                                        }
                                    } catch (Throwable t) {
                                        int maskAlpha = (int) (latestConfig.fullscreenBlurMaskAlpha * 0x33);
                                        view.setBackgroundColor((maskAlpha << 24) | 0x000000);
                                    }
                                    Log.i(TAG, "已独立开启全屏模糊遮罩: 半径=" + blurRadius + ", 比例=" + blurRatio);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "设置全屏背景模糊失败", t);
                    }

                    // 允许触摸穿透并更新 WindowManager 布局标记 (开启 FLAG_BLUR_BEHIND 赋能局部模糊)
                    try {
                        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) XposedHelpers.getObjectField(view, "mLayoutParams");
                        if (lp != null) {
                            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                            if (view.isAttachedToWindow()) {
                                WindowManager wm = (WindowManager) XposedHelpers.getObjectField(view, "mWindowManager");
                                if (wm != null) {
                                    wm.updateViewLayout(view, lp);
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
                        if (!CONTAINER_TAG.equals(child.getTag())) {
                            child.setVisibility(View.GONE);
                        }
                    }

                    // 获取或创建 CustomQuickOpenContainer
                    CustomQuickOpenContainer container = (CustomQuickOpenContainer)
                        XposedHelpers.getAdditionalInstanceField(view, "customContainer");

                    if (container == null) {
                        container = new CustomQuickOpenContainer(view.getContext(), latestConfig, cl);
                        container.setTag(CONTAINER_TAG);
                        view.addView(container, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        ));
                        XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);
                    }

                    // 每次展开前，热更新容器配置！
                    container.updateConfig(latestConfig);

                    // 若容器已处于展示状态，则无需重复初始化
                    if (container.getVisibility() == View.VISIBLE) {
                        return;
                    }

                    // 获取 FOD 精准指纹中心坐标
                    float fodX, fodY;
                    try {
                        Class<?> utilsClass = XposedHelpers.findClass(
                            "com.miui.keyguard.biometrics.fod.MiuiGxzwUtils", cl
                        );
                        XposedHelpers.callStaticMethod(utilsClass, "caculateGxzwIconSize", view.getContext());
                        int iconX = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_X");
                        int iconY = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_Y");
                        int iconW = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_WIDTH");
                        int iconH = XposedHelpers.getStaticIntField(utilsClass, "GXZW_ICON_HEIGHT");
                        fodX = iconX + iconW / 2.0f;
                        fodY = iconY + iconH / 2.0f;
                    } catch (Throwable t) {
                        Point displaySize = getDisplaySize(view.getContext());
                        fodX = displaySize.x / 2.0f;
                        fodY = displaySize.y * 0.795f;
                    }

                    container.setVisibility(View.VISIBLE);
                    container.bringToFront();
                    container.setupAndExpand(fodX, fodY);
                    Log.i(TAG, "CustomQuickOpenContainer 展开成功，指纹中心: (" + fodX + ", " + fodY + ")");
                }
            }));

            // 3. Hook dismiss(int)：退出并复位自定义容器
            record(XposedHelpers.findAndHookMethod(quickOpenViewClass, "dismiss", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!active) return;
                    FrameLayout view = (FrameLayout) param.thisObject;
                    CustomQuickOpenContainer container = (CustomQuickOpenContainer)
                        XposedHelpers.getAdditionalInstanceField(view, "customContainer");
                    if (container != null) {
                        container.resetBubbles();
                        container.setVisibility(View.GONE);
                    }

                    // 确保全屏高斯模糊复位清除
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
                        Object iconView = XposedHelpers.getObjectField(listener, "this$0");
                        if (iconView == null) return;

                        final Object quickOpenView = XposedHelpers.getObjectField(iconView, "mMiuiGxzwQuickOpenView");
                        if (quickOpenView == null) return;

                        final FrameLayout frameLayout = (FrameLayout) quickOpenView;
                        final CustomQuickOpenContainer container = (CustomQuickOpenContainer)
                            XposedHelpers.getAdditionalInstanceField(frameLayout, "customContainer");

                        // 只有在自定义快捷面板处于展示态时，才拦截后续手势
                        if (container == null || container.getVisibility() != View.VISIBLE) return;

                        MotionEvent event = (MotionEvent) param.args[0];
                        int action = event.getActionMasked();
                        final float rawX = event.getRawX();
                        final float rawY = event.getRawY();

                        if (action == MotionEvent.ACTION_MOVE) {
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                container.onFingerMove(rawX, rawY);
                            } else {
                                frameLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        container.onFingerMove(rawX, rawY);
                                    }
                                });
                            }
                            param.setResult(null); // 完全拦截原生滑动处理，避免原生逻辑干扰
                        } else if (action == MotionEvent.ACTION_UP) {
                            Runnable upTask = new Runnable() {
                                @Override
                                public void run() {
                                    container.onFingerUp();
                                    try {
                                        XposedHelpers.callMethod(quickOpenView, "dismiss", 0);
                                    } catch (Throwable t) {
                                        Log.e(TAG, "调用 dismiss 异常", t);
                                    }
                                }
                            };
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                upTask.run();
                            } else {
                                frameLayout.post(upTask);
                            }
                            param.setResult(null); // 完全拦截原生松手动作
                        } else if (action == MotionEvent.ACTION_CANCEL) {
                            Runnable cancelTask = new Runnable() {
                                @Override
                                public void run() {
                                    container.resetBubbles();
                                    try {
                                        XposedHelpers.callMethod(quickOpenView, "dismiss", 0);
                                    } catch (Throwable t) {
                                        Log.e(TAG, "调用 dismiss 异常", t);
                                    }
                                }
                            };
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                cancelTask.run();
                            } else {
                                frameLayout.post(cancelTask);
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
     *   3. 把已注入的 CustomQuickOpenContainer 从视图树里摘除并复位，
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

        // 2. 拆除视图树上已注入的自定义容器
        synchronized (HOSTS) {
            for (WeakReference<FrameLayout> ref : HOSTS) {
                FrameLayout host = ref.get();
                if (host == null) continue;
                try {
                    CustomQuickOpenContainer container = (CustomQuickOpenContainer)
                        XposedHelpers.getAdditionalInstanceField(host, "customContainer");
                    if (container != null) {
                        container.resetBubbles();
                        container.setVisibility(View.GONE);
                        if (container.getParent() instanceof ViewGroup) {
                            ((ViewGroup) container.getParent()).removeView(container);
                        }
                    }
                    XposedHelpers.removeAdditionalInstanceField(host, "customContainer");
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
