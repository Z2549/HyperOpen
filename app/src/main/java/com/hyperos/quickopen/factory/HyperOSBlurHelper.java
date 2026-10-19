package com.hyperos.quickopen.factory;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * HyperOS 4 / Android 12+ 系统级背景模糊辅助类：
 * 通过反射无缝穿透获取 ViewRootImpl.createBackgroundBlurDrawable()，
 * 由 SurfaceFlinger 硬件图层合成器对 View 背后内容实施真正的实时毛玻璃模糊 (Backdrop Blur)。
 */
public final class HyperOSBlurHelper {
    private static final String TAG = "QuickOpen_BlurHelper";

    private HyperOSBlurHelper() {}

    /**
     * 为目标 View 创建系统原生 BackgroundBlurDrawable
     */
    @Nullable
    public static Drawable createSystemBackdropBlurDrawable(@Nullable View view, float cornerRadiusPx, int blurRadiusPx, int tintColor) {
        if (view == null || blurRadiusPx <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }
        try {
            Object vri = findViewRootImpl(view);
            if (vri == null) {
                Log.d(TAG, "未能从 View 层级树中获取到 ViewRootImpl (可能尚未 Attached)");
                return null;
            }

            Method createMethod = null;
            Class<?> clazz = vri.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    createMethod = clazz.getDeclaredMethod("createBackgroundBlurDrawable");
                    createMethod.setAccessible(true);
                    break;
                } catch (Throwable ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (createMethod != null) {
                Drawable blurDrawable = (Drawable) createMethod.invoke(vri);
                if (blurDrawable != null) {
                    updateBlurDrawable(blurDrawable, cornerRadiusPx, blurRadiusPx, tintColor);
                    Log.i(TAG, "成功由 ViewRootImpl 创建并绑定官方 BackgroundBlurDrawable! 半径=" + blurRadiusPx + ", 圆角=" + cornerRadiusPx);
                    return blurDrawable;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "createBackgroundBlurDrawable 异常: " + t.getMessage());
        }
        return null;
    }

    /**
     * 沿着 View 树向上递归查找有效的 ViewRootImpl
     */
    @Nullable
    public static Object findViewRootImpl(@NonNull View view) {
        try {
            Method m = View.class.getDeclaredMethod("getViewRootImpl");
            m.setAccessible(true);
            Object vri = m.invoke(view);
            if (vri != null) return vri;
        } catch (Throwable ignored) {}

        View current = view;
        while (current.getParent() instanceof View) {
            current = (View) current.getParent();
            try {
                Method m = View.class.getDeclaredMethod("getViewRootImpl");
                m.setAccessible(true);
                Object vri = m.invoke(current);
                if (vri != null) return vri;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Method sSetBlurRadiusMethod;
    private static boolean sSetBlurRadiusSearched = false;
    private static Method sSetCornerRadiusMethod;
    private static boolean sSetCornerRadiusSearched = false;
    private static Method sSetColorMethod;
    private static boolean sSetColorSearched = false;

    /**
     * 仅高频更新已有 BackgroundBlurDrawable 的圆角半径（供气泡鱼眼/选中缩放动画逐帧同步，避免圆角小于缩放后 बॉर्डर导致变成圆角矩形或溢出边框）
     */
    public static void updateCornerRadius(@Nullable Drawable blurDrawable, float cornerRadiusPx) {
        if (blurDrawable == null) return;
        try {
            if (!sSetCornerRadiusSearched) {
                sSetCornerRadiusSearched = true;
                try {
                    sSetCornerRadiusMethod = blurDrawable.getClass().getMethod("setCornerRadius", float.class);
                    sSetCornerRadiusMethod.setAccessible(true);
                } catch (Throwable t) {
                    try {
                        sSetCornerRadiusMethod = blurDrawable.getClass().getDeclaredMethod("setCornerRadius", float.class);
                        sSetCornerRadiusMethod.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
            }
            if (sSetCornerRadiusMethod != null) {
                sSetCornerRadiusMethod.invoke(blurDrawable, Math.max(0f, cornerRadiusPx));
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 更新已有 BackgroundBlurDrawable 的模糊半径、圆角和颜色
     */
    public static void updateBlurDrawable(@Nullable Drawable blurDrawable, float cornerRadiusPx, int blurRadiusPx, int tintColor) {
        if (blurDrawable == null) return;

        // 1. 设置模糊半径
        try {
            if (!sSetBlurRadiusSearched) {
                sSetBlurRadiusSearched = true;
                try {
                    sSetBlurRadiusMethod = blurDrawable.getClass().getMethod("setBlurRadius", int.class);
                    sSetBlurRadiusMethod.setAccessible(true);
                } catch (Throwable t) {
                    try {
                        sSetBlurRadiusMethod = blurDrawable.getClass().getDeclaredMethod("setBlurRadius", int.class);
                        sSetBlurRadiusMethod.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
            }
            if (sSetBlurRadiusMethod != null) {
                sSetBlurRadiusMethod.invoke(blurDrawable, Math.max(0, blurRadiusPx));
            }
        } catch (Throwable ignored) {}

        // 2. 设置圆角
        updateCornerRadius(blurDrawable, cornerRadiusPx);

        // 3. 设置混合颜色 (带微透 Alpha)
        try {
            if (!sSetColorSearched) {
                sSetColorSearched = true;
                try {
                    sSetColorMethod = blurDrawable.getClass().getMethod("setColor", int.class);
                    sSetColorMethod.setAccessible(true);
                } catch (Throwable t) {
                    try {
                        sSetColorMethod = blurDrawable.getClass().getDeclaredMethod("setColor", int.class);
                        sSetColorMethod.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
            }
            if (sSetColorMethod != null) {
                sSetColorMethod.invoke(blurDrawable, tintColor);
            }
        } catch (Throwable ignored) {}
    }
}
