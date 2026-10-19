package io.github.qmryqmm.hyperopen;

import android.content.Context;
import android.util.Log;

import io.github.qmryqmm.hyperopen.hook.HyperOpenHook;
import io.github.qmryqmm.hyperopen.safety.SafetyGuard;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "HyperOpen";
    public static final String TARGET_PACKAGE = "com.android.systemui";

    /** 防止同一进程内重复部署（多个 Context 回调点都可能触发） */
    private static boolean deployed = false;
    /** Provider 未就绪时的重试次数上限 */
    private static final int MAX_SAFETY_RETRY = 6;
    private static int safetyRetry = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        Log.i(TAG, "检测到小米 SystemUI 进程加载，开始部署指纹快捷方式增强 Hook: " + lpparam.processName);

        // 1. 若此刻 Application 已就绪，直接进入安全模式闸门
        Context ctx = currentApplication(lpparam);
        if (ctx != null) {
            deploy(lpparam, ctx);
            return;
        }

        // 2. 否则挂到最早的可用 Context 回调点上，拿到 Context 后再过闸门
        if (!armContextCallbacks(lpparam)) {
            Log.e(TAG, "未能挂载任何 Context 回调点，本次启动放弃部署 Hook");
        }
    }

    /**
     * 安全模式闸门：只有 SafetyGuard 判定允许，才真正安装 Hook。
     */
    private static synchronized void deploy(XC_LoadPackage.LoadPackageParam lpparam, Context ctx) {
        if (deployed) {
            return;
        }

        Boolean allowed;
        try {
            allowed = SafetyGuard.install(ctx);
        } catch (Throwable t) {
            Log.e(TAG, "安全模式检查异常，为保护 SystemUI 本次不安装 Hook", t);
            return;
        }

        if (allowed == null) {
            // Provider 尚未就绪：延迟重试，避免在无法确认安全状态时贸然安装 Hook
            if (safetyRetry < MAX_SAFETY_RETRY) {
                safetyRetry++;
                final int delay = 400 * safetyRetry;
                Log.w(TAG, "安全模式状态暂不可查，第 " + safetyRetry + " 次重试将在 " + delay + "ms 后进行");
                try {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            deploy(lpparam, ctx);
                        }
                    }, delay);
                } catch (Throwable t) {
                    Log.e(TAG, "安排安全模式重试失败", t);
                }
            } else {
                Log.e(TAG, "多次重试后仍无法确认安全模式状态，为保护 SystemUI 本次不安装 Hook");
            }
            return;
        }

        deployed = true;

        if (!allowed) {
            Log.e(TAG, "安全模式已激活或 Hook 开关已关闭 → 本次启动不安装任何 Hook，SystemUI 将以原生状态运行");
            return;
        }

        try {
            // 注册「运行期即时停用」执行器：崩溃哨兵凑够阈值的那一刻，
            // 由 SafetyGuard 直接回调这里，把已装好的 Hook 全部拆掉。
            SafetyGuard.setDeactivator(new Runnable() {
                @Override
                public void run() {
                    HyperOpenHook.uninstall();
                }
            });
            HyperOpenHook.hook(lpparam);
            Log.i(TAG, "Hook 部署流程结束");
        } catch (Throwable t) {
            Log.e(TAG, "Hook 部署发生未预期异常", t);
            try {
                SafetyGuard.reportDeployFailure(t);
            } catch (Throwable ignored) {}
        }
    }

    private static Context currentApplication(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> helper = XposedHelpers.findClass("android.app.AndroidAppHelper", lpparam.classLoader);
            Object ctx = XposedHelpers.callStaticMethod(helper, "currentApplication");
            return ctx instanceof Context ? (Context) ctx : null;
        } catch (Throwable t) {
            Log.w(TAG, "获取当前 Application 失败: " + t);
            return null;
        }
    }

    /**
     * 依次尝试 Application#attachBaseContext / Application#onCreate /
     * Instrumentation#callApplicationOnCreate 三个回调点，任一触发即完成部署。
     */
    private static boolean armContextCallbacks(XC_LoadPackage.LoadPackageParam lpparam) {
        boolean armed = false;

        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                "attachBaseContext", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object self = param.thisObject;
                        Context ctx = self instanceof Context ? (Context) self : null;
                        if (ctx == null && param.args.length > 0 && param.args[0] instanceof Context) {
                            ctx = (Context) param.args[0];
                        }
                        if (ctx != null) {
                            deploy(lpparam, ctx);
                        }
                    }
                });
            armed = true;
            Log.i(TAG, "已挂载 Application#attachBaseContext 部署点");
        } catch (Throwable t) {
            Log.w(TAG, "挂载 Application#attachBaseContext 失败", t);
        }

        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.thisObject instanceof Context) {
                            deploy(lpparam, (Context) param.thisObject);
                        }
                    }
                });
            armed = true;
            Log.i(TAG, "已挂载 Application#onCreate 部署点");
        } catch (Throwable t) {
            Log.w(TAG, "挂载 Application#onCreate 失败", t);
        }

        try {
            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader,
                "callApplicationOnCreate", android.app.Application.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length > 0 && param.args[0] instanceof Context) {
                            deploy(lpparam, (Context) param.args[0]);
                        }
                    }
                });
            armed = true;
            Log.i(TAG, "已挂载 Instrumentation#callApplicationOnCreate 部署点");
        } catch (Throwable t) {
            Log.w(TAG, "挂载 Instrumentation#callApplicationOnCreate 失败", t);
        }

        return armed;
    }
}
