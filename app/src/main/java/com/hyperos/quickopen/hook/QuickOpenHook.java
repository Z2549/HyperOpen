package com.hyperos.quickopen.hook;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.view.CustomQuickOpenContainer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QuickOpenHook {
    private static final String TAG = "QuickOpen_Hook";
    private static final String CONTAINER_TAG = "hyperos_custom_quickopen_container";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        final ClassLoader cl = lpparam.classLoader;

        try {
            final Class<?> quickOpenViewClass = XposedHelpers.findClass(
                "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenView", cl
            );

            // 1. Hook 构造函数：注入 CustomQuickOpenContainer 并挂载到 view 上
            XposedBridge.hookAllConstructors(quickOpenViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout view = (FrameLayout) param.thisObject;
                    Context context = view.getContext();

                    QuickOpenConfig config = QuickOpenConfig.loadFromXposed();
                    CustomQuickOpenContainer container = new CustomQuickOpenContainer(context, config, cl);
                    container.setTag(CONTAINER_TAG);
                    container.setVisibility(View.GONE);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    view.addView(container, lp);
                    XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);

                    Log.i(TAG, "已成功向 MiuiGxzwQuickOpenView 注入 CustomQuickOpenContainer！");
                }
            });

            // 2. Hook startShowQuickOpenItemAnimation：完全接管展开流程，阻止系统默认5项图标与扇形黑底
            XposedHelpers.findAndHookMethod(quickOpenViewClass, "startShowQuickOpenItemAnimation", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout view = (FrameLayout) param.thisObject;

                    // 完全阻止系统原生 startShowQuickOpenItemAnimation 执行 (避免创建原生5项View、动画和扇形黑底)
                    param.setResult(null);

                    // 允许触摸穿透并更新 WindowManager 布局标记 (相当于系统原生的 flags |= 4)
                    try {
                        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) XposedHelpers.getObjectField(view, "mLayoutParams");
                        if (lp != null) {
                            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
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

                    // 取消原生扇形黑底背景，改为柔和半透明暗色蒙版
                    view.setBackgroundColor(0x22000000);

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
                        QuickOpenConfig config = QuickOpenConfig.loadFromXposed();
                        container = new CustomQuickOpenContainer(view.getContext(), config, cl);
                        container.setTag(CONTAINER_TAG);
                        view.addView(container, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        ));
                        XposedHelpers.setAdditionalInstanceField(view, "customContainer", container);
                    }

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
            });

            // 3. Hook dismiss(int)：退出并复位自定义容器
            XposedHelpers.findAndHookMethod(quickOpenViewClass, "dismiss", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    FrameLayout view = (FrameLayout) param.thisObject;
                    CustomQuickOpenContainer container = (CustomQuickOpenContainer)
                        XposedHelpers.getAdditionalInstanceField(view, "customContainer");
                    if (container != null) {
                        container.resetBubbles();
                        container.setVisibility(View.GONE);
                    }
                }
            });

            Log.i(TAG, "已成功安装 MiuiGxzwQuickOpenView 核心生命周期接管 Hook！");

        } catch (Throwable t) {
            Log.e(TAG, "Hook MiuiGxzwQuickOpenView 异常", t);
        }

        // 4. Hook MiuiGxzwIconView$2.onPointerEvent(MotionEvent) 拦截手势事件并驱动蜂窝鱼眼
        try {
            Class<?> iconView2Class = XposedHelpers.findClass(
                "com.miui.keyguard.biometrics.fod.MiuiGxzwIconView$2", cl
            );
            XposedHelpers.findAndHookMethod(iconView2Class, "onPointerEvent", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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
            });
            Log.i(TAG, "已成功安装 MiuiGxzwIconView$2.onPointerEvent 触摸拦截 Hook！");
        } catch (Throwable t) {
            Log.e(TAG, "Hook MiuiGxzwIconView$2 失败", t);
        }
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
