package com.hyperos.quickopen;

import android.util.Log;

import com.hyperos.quickopen.hook.QuickOpenHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "HyperOS_QuickOpen";
    public static final String TARGET_PACKAGE = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        Log.i(TAG, "检测到小米 SystemUI 进程加载，开始部署指纹快捷方式增强 Hook: " + lpparam.processName);
        QuickOpenHook.hook(lpparam);
    }
}
