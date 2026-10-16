package com.hyperos.quickopen.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.hyperos.quickopen.model.ShortcutItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;

public class QuickOpenConfig {
    private static final String TAG = "QuickOpen_Config";

    public static final String PACKAGE_NAME = "com.hyperos.quickopen";
    public static final String PREF_NAME = "quick_open_settings";
    public static final String ACTION_CONFIG_CHANGED = "com.hyperos.quickopen.ACTION_CONFIG_CHANGED";

    public static final int MATERIAL_FOLLOW_SYSTEM = 0;
    public static final int MATERIAL_SOFT_GLASS = 1;
    public static final int MATERIAL_GAUSSIAN_BLUR = 2;

    public int materialStyle = MATERIAL_SOFT_GLASS;
    public int offsetY = 280;          // dp
    public int regionDiameter = 330;   // dp
    public boolean showRegionBg = true;
    public int iconSize = 58;          // dp
    public float maxScale = 1.40f;
    public int influenceRadius = 120;  // dp
    public List<ShortcutItem> shortcuts = new ArrayList<>();

    private static XSharedPreferences xPrefs;

    /**
     * SystemUI 优先通过 ContentProvider 查询跨进程配置，失败时回退 XSharedPreferences
     */
    public static QuickOpenConfig load(Context context) {
        if (context != null) {
            try {
                android.os.Bundle bundle = context.getContentResolver().call(
                    android.net.Uri.parse("content://com.hyperos.quickopen.provider"),
                    "get_config", null, null
                );
                if (bundle != null) {
                    QuickOpenConfig config = new QuickOpenConfig();
                    config.materialStyle = bundle.getInt("material_style", MATERIAL_SOFT_GLASS);
                    config.offsetY = bundle.getInt("offset_y", 280);
                    config.regionDiameter = bundle.getInt("region_diameter", 330);
                    config.showRegionBg = bundle.getBoolean("show_region_bg", true);
                    config.iconSize = bundle.getInt("icon_size", 58);
                    config.maxScale = bundle.getFloat("max_scale", 1.40f);
                    config.influenceRadius = bundle.getInt("influence_radius", 120);

                    String jsonStr = bundle.getString("shortcuts_json", null);
                    config.shortcuts = parseShortcutsJson(jsonStr);
                    Log.i(TAG, "成功通过 ContentProvider 读取最新配置: offsetY=" + config.offsetY + ", items=" + config.shortcuts.size());
                    return config;
                }
            } catch (Throwable t) {
                Log.w(TAG, "ContentProvider 查询异常，回退 XSharedPreferences: " + t.getMessage());
            }
        }
        return loadFromXposed();
    }

    /**
     * SystemUI 进程加载配置 (XSharedPreferences 兜底)
     */
    public static QuickOpenConfig loadFromXposed() {
        if (xPrefs == null) {
            xPrefs = new XSharedPreferences(PACKAGE_NAME, PREF_NAME);
            xPrefs.makeWorldReadable();
        } else {
            xPrefs.reload();
        }

        QuickOpenConfig config = new QuickOpenConfig();
        config.materialStyle = xPrefs.getInt("material_style", MATERIAL_SOFT_GLASS);
        config.offsetY = xPrefs.getInt("offset_y", 280);
        config.regionDiameter = xPrefs.getInt("region_diameter", 330);
        config.showRegionBg = xPrefs.getBoolean("show_region_bg", true);
        config.iconSize = xPrefs.getInt("icon_size", 58);
        config.maxScale = xPrefs.getFloat("max_scale", 1.40f);
        config.influenceRadius = xPrefs.getInt("influence_radius", 120);

        String jsonStr = xPrefs.getString("shortcuts_json", null);
        config.shortcuts = parseShortcutsJson(jsonStr);
        return config;
    }

    /**
     * 模块配置 Activity 加载配置
     */
    public static QuickOpenConfig loadFromApp(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        QuickOpenConfig config = new QuickOpenConfig();
        config.materialStyle = sp.getInt("material_style", MATERIAL_SOFT_GLASS);
        config.offsetY = sp.getInt("offset_y", 280);
        config.regionDiameter = sp.getInt("region_diameter", 330);
        config.showRegionBg = sp.getBoolean("show_region_bg", true);
        config.iconSize = sp.getInt("icon_size", 58);
        config.maxScale = sp.getFloat("max_scale", 1.40f);
        config.influenceRadius = sp.getInt("influence_radius", 120);

        String jsonStr = sp.getString("shortcuts_json", null);
        config.shortcuts = parseShortcutsJson(jsonStr);
        return config;
    }

    /**
     * 保存配置 (支持全局只读分享给 SystemUI + 实时广播通知)
     */
    public void save(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .putInt("material_style", materialStyle)
            .putInt("offset_y", offsetY)
            .putInt("region_diameter", regionDiameter)
            .putBoolean("show_region_bg", showRegionBg)
            .putInt("icon_size", iconSize)
            .putFloat("max_scale", maxScale)
            .putInt("influence_radius", influenceRadius)
            .putString("shortcuts_json", serializeShortcutsJson(shortcuts))
            .apply();

        // 开放目录与文件可读权限，为 XSharedPreferences 回退提供支持
        try {
            java.io.File dataDir = context.getDataDir();
            dataDir.setReadable(true, false);
            dataDir.setExecutable(true, false);
            java.io.File prefsDir = new java.io.File(dataDir, "shared_prefs");
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false);
                prefsDir.setExecutable(true, false);
            }
            java.io.File prefsFile = new java.io.File(prefsDir, PREF_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {}

        // 发送广播通知 SystemUI 实时热重载配置
        try {
            android.content.Intent intent = new android.content.Intent(ACTION_CONFIG_CHANGED);
            intent.setPackage("com.android.systemui");
            context.sendBroadcast(intent);
        } catch (Throwable ignored) {}
    }

    public static List<ShortcutItem> parseShortcutsJson(String jsonStr) {
        List<ShortcutItem> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return getDefaultShortcuts();
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(ShortcutItem.fromJson(obj));
            }
        } catch (Throwable t) {
            Log.e(TAG, "解析快捷方式 JSON 失败，回退到默认项", t);
            return getDefaultShortcuts();
        }
        return list;
    }

    public static String serializeShortcutsJson(List<ShortcutItem> items) {
        JSONArray arr = new JSONArray();
        for (ShortcutItem item : items) {
            arr.put(item.toJson());
        }
        return arr.toString();
    }

    public static List<ShortcutItem> getDefaultShortcuts() {
        List<ShortcutItem> items = new ArrayList<>();
        items.add(new ShortcutItem("wx_scan", "微信扫一扫", "直接唤起扫码镜头",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=com.tencent.mm.action.BIZSHORTCUT;S.LauncherUI.From.Scaner.Shortcut=true;end",
            0xFF07C160, true));

        items.add(new ShortcutItem("alipay_pay", "支付宝付款码", "出示收银台付款",
            ShortcutItem.ACTION_INTENT_URI, "intent://platformapi/startapp?appId=20000056#Intent;scheme=alipays;end",
            0xFF1677FF, true));

        items.add(new ShortcutItem("flashlight", "手电筒", "开关后置 LED 强光",
            ShortcutItem.ACTION_FLASHLIGHT, "",
            0xFFFAAD14, true));

        items.add(new ShortcutItem("alipay_scan", "支付宝扫一扫", "扫码识别或骑行单车",
            ShortcutItem.ACTION_INTENT_URI, "intent://platformapi/startapp?appId=10000007#Intent;scheme=alipays;end",
            0xFF1677FF, true));

        items.add(new ShortcutItem("bus_code", "乘车码", "地铁公交一卡通刷码",
            ShortcutItem.ACTION_INTENT_URI, "intent://platformapi/startapp?appId=200011235#Intent;scheme=alipays;end",
            0xFF52C41A, true));

        items.add(new ShortcutItem("camera", "街拍相机", "秒级开启抓拍",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=android.media.action.STILL_IMAGE_CAMERA;end",
            0xFFFF4D4F, true));

        items.add(new ShortcutItem("calc", "小窗计算器", "开启悬浮轻量计算",
            ShortcutItem.ACTION_APP, "com.miui.calculator",
            0xFF13C2C2, true));

        items.add(new ShortcutItem("notes", "新建便签", "随时记录脑海灵感",
            ShortcutItem.ACTION_APP, "com.miui.notes",
            0xFF722ED1, true));

        items.add(new ShortcutItem("wx_pay", "微信收款码", "出示面对面收款二维码",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=com.tencent.mm.action.BIZSHORTCUT;S.LauncherUI.From.Scaner.Shortcut=false;end",
            0xFF07C160, true));

        items.add(new ShortcutItem("record", "高清录音", "一键启动系统录音机",
            ShortcutItem.ACTION_APP, "com.android.soundrecorder",
            0xFFFA541C, true));

        items.add(new ShortcutItem("music", "听歌识曲", "快速识别环境播放音乐",
            ShortcutItem.ACTION_APP, "com.tencent.qqmusic",
            0xFFEB2F96, true));

        items.add(new ShortcutItem("settings", "系统设置", "进入常用设置中心",
            ShortcutItem.ACTION_APP, "com.android.settings",
            0xFF8C8C8C, true));

        return items;
    }
}
