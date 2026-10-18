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

    /** 应用图标裁切方案：0=默认气泡留白, 1=抗锯齿圆形裁切, 2=自适应外扩满铺, 3=全屏满铺气泡 */
    public static final int CROP_DEFAULT_PADDING = 0;
    public static final int CROP_BITMAP_CIRCLE = 1;
    public static final int CROP_ADAPTIVE_EXPAND = 2;
    public static final int CROP_FULL_BLEED = 3;

    /** Miuix 矢量图标底垫渲染模式：0=彩色底板+白色图标, 1=微透磨砂底板, 2=反色(微透浅底+彩色图标) */
    public static final int GLYPH_PAD_MODE_SOLID = 0;
    public static final int GLYPH_PAD_MODE_TRANSLUCENT = 1;
    public static final int GLYPH_PAD_MODE_INVERTED = 2;

    /** 界面风格：0=Material You 深色玻璃, 1=MIUIX 浅色卡片 */
    public static final int UI_STYLE_MATERIAL = 0;
    public static final int UI_STYLE_MIUIX = 1;

    /** 主题模式：0=跟随系统, 1=浅色, 2=深色 */
    public static final int THEME_FOLLOW_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    /** 页面切换手势：0=关闭, 1=横滑切换 */
    public static final int PAGE_GESTURE_OFF = 0;
    public static final int PAGE_GESTURE_HORIZONTAL = 1;

    public int materialStyle = MATERIAL_SOFT_GLASS;
    /** 设置界面风格（仅 App 内生效）：UI_STYLE_MATERIAL / UI_STYLE_MIUIX */
    public int uiStyle = UI_STYLE_MATERIAL;
    /** 主题模式（仅 App 内生效）：THEME_FOLLOW_SYSTEM / THEME_LIGHT / THEME_DARK */
    public int themeMode = THEME_FOLLOW_SYSTEM;
    /** Monet 动态取色（仅 App 内 Material 轨生效） */
    public boolean monetColors = false;
    /** 顶栏/底栏毛玻璃 */
    public boolean barBlur = true;
    /** Apple 风格悬浮底栏 */
    public boolean floatingNavBar = false;
    /** 预测性返回手势（Android 13+） */
    public boolean predictiveBack = true;
    /** 横移返回手势 */
    public boolean edgeBack = true;
    /** 页面切换手势：PAGE_GESTURE_OFF / PAGE_GESTURE_HORIZONTAL */
    public int pageSwitchGesture = PAGE_GESTURE_HORIZONTAL;
    public int offsetY = 280;          // dp
    public int regionDiameter = 330;   // dp
    public boolean showRegionBg = true;
    public boolean showHeaderPillBg = true;
    public int iconSize = 58;          // dp
    public float maxScale = 1.40f;
    public int influenceRadius = 120;  // dp

    /** 应用图标裁切方案 (CROP_DEFAULT_PADDING / CROP_BITMAP_CIRCLE / CROP_ADAPTIVE_EXPAND / CROP_FULL_BLEED) */
    public int appIconCropMode = CROP_ADAPTIVE_EXPAND;
    /** Miuix 矢量图标是否启用圆形底垫 */
    public boolean glyphPadEnabled = true;
    /** Miuix 矢量图标底垫颜色 (ARGB) */
    public int glyphPadColor = 0xFF2A82E4;
    /** Miuix 矢量图标底垫质感模式 (GLYPH_PAD_MODE_SOLID / TRANSLUCENT / INVERTED) */
    public int glyphPadMode = GLYPH_PAD_MODE_TRANSLUCENT;
    /** 图标呼吸/激活外发光辉光动效 */
    public boolean iconGlowEffect = true;

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
                    config.showHeaderPillBg = bundle.getBoolean("show_header_pill_bg", true);
                    config.iconSize = bundle.getInt("icon_size", 58);
                    config.maxScale = bundle.getFloat("max_scale", 1.40f);
                    config.influenceRadius = bundle.getInt("influence_radius", 120);
                    config.appIconCropMode = bundle.getInt("app_icon_crop_mode", CROP_ADAPTIVE_EXPAND);
                    config.glyphPadEnabled = bundle.getBoolean("glyph_pad_enabled", true);
                    config.glyphPadColor = bundle.getInt("glyph_pad_color", 0xFF2A82E4);
                    config.glyphPadMode = bundle.getInt("glyph_pad_mode", GLYPH_PAD_MODE_TRANSLUCENT);
                    config.iconGlowEffect = bundle.getBoolean("icon_glow_effect", true);

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
        config.showHeaderPillBg = xPrefs.getBoolean("show_header_pill_bg", true);
        config.iconSize = xPrefs.getInt("icon_size", 58);
        config.maxScale = xPrefs.getFloat("max_scale", 1.40f);
        config.influenceRadius = xPrefs.getInt("influence_radius", 120);
        config.appIconCropMode = xPrefs.getInt("app_icon_crop_mode", CROP_ADAPTIVE_EXPAND);
        config.glyphPadEnabled = xPrefs.getBoolean("glyph_pad_enabled", true);
        config.glyphPadColor = xPrefs.getInt("glyph_pad_color", 0xFF2A82E4);
        config.glyphPadMode = xPrefs.getInt("glyph_pad_mode", GLYPH_PAD_MODE_TRANSLUCENT);
        config.iconGlowEffect = xPrefs.getBoolean("icon_glow_effect", true);

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
        config.uiStyle = sp.getInt("ui_style", UI_STYLE_MATERIAL);
        config.themeMode = sp.getInt("theme_mode", THEME_FOLLOW_SYSTEM);
        config.monetColors = sp.getBoolean("monet_colors", false);
        config.barBlur = sp.getBoolean("bar_blur", true);
        config.floatingNavBar = sp.getBoolean("floating_nav_bar", false);
        config.predictiveBack = sp.getBoolean("predictive_back", true);
        config.edgeBack = sp.getBoolean("edge_back", true);
        config.pageSwitchGesture = sp.getInt("page_switch_gesture", PAGE_GESTURE_HORIZONTAL);
        config.offsetY = sp.getInt("offset_y", 280);
        config.regionDiameter = sp.getInt("region_diameter", 330);
        config.showRegionBg = sp.getBoolean("show_region_bg", true);
        config.showHeaderPillBg = sp.getBoolean("show_header_pill_bg", true);
        config.iconSize = sp.getInt("icon_size", 58);
        config.maxScale = sp.getFloat("max_scale", 1.40f);
        config.influenceRadius = sp.getInt("influence_radius", 120);
        config.appIconCropMode = sp.getInt("app_icon_crop_mode", CROP_ADAPTIVE_EXPAND);
        config.glyphPadEnabled = sp.getBoolean("glyph_pad_enabled", true);
        config.glyphPadColor = sp.getInt("glyph_pad_color", 0xFF2A82E4);
        config.glyphPadMode = sp.getInt("glyph_pad_mode", GLYPH_PAD_MODE_TRANSLUCENT);
        config.iconGlowEffect = sp.getBoolean("icon_glow_effect", true);

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
            .putInt("ui_style", uiStyle)
            .putInt("theme_mode", themeMode)
            .putBoolean("monet_colors", monetColors)
            .putBoolean("bar_blur", barBlur)
            .putBoolean("floating_nav_bar", floatingNavBar)
            .putBoolean("predictive_back", predictiveBack)
            .putBoolean("edge_back", edgeBack)
            .putInt("page_switch_gesture", pageSwitchGesture)
            .putInt("offset_y", offsetY)
            .putInt("region_diameter", regionDiameter)
            .putBoolean("show_region_bg", showRegionBg)
            .putBoolean("show_header_pill_bg", showHeaderPillBg)
            .putInt("icon_size", iconSize)
            .putFloat("max_scale", maxScale)
            .putInt("influence_radius", influenceRadius)
            .putInt("app_icon_crop_mode", appIconCropMode)
            .putBoolean("glyph_pad_enabled", glyphPadEnabled)
            .putInt("glyph_pad_color", glyphPadColor)
            .putInt("glyph_pad_mode", glyphPadMode)
            .putBoolean("icon_glow_effect", iconGlowEffect)
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
