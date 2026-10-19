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

    public static final int MATERIAL_SOFT_GLASS = 1;
    public static final int MATERIAL_GAUSSIAN_BLUR = 2;

    /** 应用图标裁切方案：1=抗锯齿圆形裁切, 2=自适应外扩满铺 (留白大小由留白滑块独立控制) */
    public static final int CROP_BITMAP_CIRCLE = 1;
    public static final int CROP_ADAPTIVE_EXPAND = 2;

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

    public int materialStyle = MATERIAL_GAUSSIAN_BLUR;
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
    public int offsetY = 100;          // dp
    public int regionDiameter = 220;   // dp
    public boolean showRegionBg = false;
    public boolean showHeaderPillBg = false;
    public int iconSize = 60;          // dp
    public float maxScale = 1.40f;
    public int influenceRadius = 70;  // dp

    /** 图标透明度 (0.0f ~ 1.0f，默认 1.0f) */
    public float iconAlpha = 1.0f;
    /** 鱼眼效果放大时的图标透明度 (0.0f ~ 1.0f，默认 1.0f) */
    public float fisheyeZoomAlpha = 1.0f;
    /** 唤醒指纹快捷方式时全屏模糊遮罩背景透明度 (0.0f ~ 1.0f，默认 1.0f) */
    public float fullscreenBlurMaskAlpha = 1.0f;
    /** 图标的圆形视界区域背景高斯模糊半径 (0 ~ 50 dp，默认 25 dp，0 为关闭) */
    public int regionBlurRadius = 25;

    /** 图标分布紧凑度间距微调 (-10dp ~ 20dp，默认 5dp，负值更紧凑，正值更松散) */
    public int iconSpacingOffset = 5;
    /** 气泡内部图标留白比例 (0% ~ 40%，默认 0%，0% 即全屏满铺) */
    public int bubblePaddingPercent = 0;
    /** 图标气泡留白区域高斯模糊半径 (0 ~ 50 dp，默认 25 dp，0 为关闭模糊，样式与圆形视界底衬一致) */
    public int bubbleBlurRadius = 25;
    /** 手指选中图标时气泡内图标的缩放倍率 (1.00x ~ 1.50x，默认 1.15x) */
    public float selectedIconScale = 1.15f;

    /** 应用图标裁切方案 (CROP_BITMAP_CIRCLE / CROP_ADAPTIVE_EXPAND) */
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
                    config.materialStyle = bundle.getInt("material_style", MATERIAL_GAUSSIAN_BLUR);
                    config.offsetY = bundle.getInt("offset_y", 100);
                    config.regionDiameter = bundle.getInt("region_diameter", 220);
                    config.showRegionBg = bundle.getBoolean("show_region_bg", false);
                    config.showHeaderPillBg = bundle.getBoolean("show_header_pill_bg", false);
                    config.iconSize = bundle.getInt("icon_size", 60);
                    config.maxScale = bundle.getFloat("max_scale", 1.40f);
                    config.influenceRadius = bundle.getInt("influence_radius", 70);
                    config.iconAlpha = bundle.getFloat("icon_alpha", 1.0f);
                    config.fisheyeZoomAlpha = bundle.getFloat("fisheye_zoom_alpha", 1.0f);
                    config.fullscreenBlurMaskAlpha = bundle.getFloat("fullscreen_blur_mask_alpha", 1.0f);
                    config.regionBlurRadius = bundle.getInt("region_blur_radius", 25);
                    config.iconSpacingOffset = bundle.getInt("icon_spacing_offset", 5);
                    config.bubblePaddingPercent = bundle.getInt("bubble_padding_percent", 0);
                    config.bubbleBlurRadius = bundle.getInt("bubble_blur_radius", 25);
                    config.selectedIconScale = bundle.getFloat("selected_icon_scale", 1.15f);
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
        config.materialStyle = xPrefs.getInt("material_style", MATERIAL_GAUSSIAN_BLUR);
        config.offsetY = xPrefs.getInt("offset_y", 100);
        config.regionDiameter = xPrefs.getInt("region_diameter", 220);
        config.showRegionBg = xPrefs.getBoolean("show_region_bg", false);
        config.showHeaderPillBg = xPrefs.getBoolean("show_header_pill_bg", false);
        config.iconSize = xPrefs.getInt("icon_size", 60);
        config.maxScale = xPrefs.getFloat("max_scale", 1.40f);
        config.influenceRadius = xPrefs.getInt("influence_radius", 70);
        config.iconAlpha = xPrefs.getFloat("icon_alpha", 1.0f);
        config.fisheyeZoomAlpha = xPrefs.getFloat("fisheye_zoom_alpha", 1.0f);
        config.fullscreenBlurMaskAlpha = xPrefs.getFloat("fullscreen_blur_mask_alpha", 1.0f);
        config.regionBlurRadius = xPrefs.getInt("region_blur_radius", 25);
        config.iconSpacingOffset = xPrefs.getInt("icon_spacing_offset", 5);
        config.bubblePaddingPercent = xPrefs.getInt("bubble_padding_percent", 0);
        config.bubbleBlurRadius = xPrefs.getInt("bubble_blur_radius", 25);
        config.selectedIconScale = xPrefs.getFloat("selected_icon_scale", 1.15f);
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
        config.materialStyle = sp.getInt("material_style", MATERIAL_GAUSSIAN_BLUR);
        config.uiStyle = sp.getInt("ui_style", UI_STYLE_MATERIAL);
        config.themeMode = sp.getInt("theme_mode", THEME_FOLLOW_SYSTEM);
        config.monetColors = sp.getBoolean("monet_colors", false);
        config.barBlur = sp.getBoolean("bar_blur", true);
        config.floatingNavBar = sp.getBoolean("floating_nav_bar", false);
        config.predictiveBack = sp.getBoolean("predictive_back", true);
        config.edgeBack = sp.getBoolean("edge_back", true);
        config.pageSwitchGesture = sp.getInt("page_switch_gesture", PAGE_GESTURE_HORIZONTAL);
        config.offsetY = sp.getInt("offset_y", 100);
        config.regionDiameter = sp.getInt("region_diameter", 220);
        config.showRegionBg = sp.getBoolean("show_region_bg", false);
        config.showHeaderPillBg = sp.getBoolean("show_header_pill_bg", false);
        config.iconSize = sp.getInt("icon_size", 60);
        config.maxScale = sp.getFloat("max_scale", 1.40f);
        config.influenceRadius = sp.getInt("influence_radius", 70);
        config.iconAlpha = sp.getFloat("icon_alpha", 1.0f);
        config.fisheyeZoomAlpha = sp.getFloat("fisheye_zoom_alpha", 1.0f);
        config.fullscreenBlurMaskAlpha = sp.getFloat("fullscreen_blur_mask_alpha", 1.0f);
        config.regionBlurRadius = sp.getInt("region_blur_radius", 25);
        config.iconSpacingOffset = sp.getInt("icon_spacing_offset", 5);
        config.bubblePaddingPercent = sp.getInt("bubble_padding_percent", 0);
        config.bubbleBlurRadius = sp.getInt("bubble_blur_radius", 25);
        config.selectedIconScale = sp.getFloat("selected_icon_scale", 1.15f);
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
            .putFloat("icon_alpha", iconAlpha)
            .putFloat("fisheye_zoom_alpha", fisheyeZoomAlpha)
            .putFloat("fullscreen_blur_mask_alpha", fullscreenBlurMaskAlpha)
            .putInt("region_blur_radius", regionBlurRadius)
            .putInt("icon_spacing_offset", iconSpacingOffset)
            .putInt("bubble_padding_percent", bubblePaddingPercent)
            .putInt("bubble_blur_radius", bubbleBlurRadius)
            .putFloat("selected_icon_scale", selectedIconScale)
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

        // 1. 微信扫一扫 (智能助理标准入口，配 Miuix 扫一扫矢量图标 + 微信绿微透底垫)
        items.add(new ShortcutItem(
            "wx_scan", "微信扫一扫", "直接唤起扫码镜头",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10000000;package=com.tencent.mm;component=com.tencent.mm/.plugin.scanner.ui.BaseScanUI;end",
            "", "Scan:Regular",
            0xFF07C160, true,
            true, 0xFF07C160, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 2. 微信付款码 (智能助理官方付款码入口，配 Miuix 银行卡矢量图标 + 微信绿微透底垫)
        items.add(new ShortcutItem(
            "wx_pay", "微信付款码", "出示收银台向商家付款",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;launchFlags=0x10000000;component=com.tencent.mm/com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI;i.key_entry_scene=2;end",
            "", "BankCards:Regular",
            0xFF07C160, true,
            true, 0xFF07C160, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 3. 微信收款码 (智能助理官方面对面二维码收款入口，配 Miuix 二维码矢量图标 + 微信绿微透底垫)
        items.add(new ShortcutItem(
            "wx_receive", "微信收款码", "面对面二维码收款",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10000000;package=com.tencent.mm;component=com.tencent.mm/.plugin.collect.ui.CollectMainUI;end",
            "", "QrCode:Regular",
            0xFF07C160, true,
            true, 0xFF07C160, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 4. 支付宝扫一扫 (官方扫码协议，配 Miuix 扫一扫图标 + 支付宝蓝微透底垫)
        items.add(new ShortcutItem(
                "alipay_scan", "支付宝扫一扫", "扫码识别或骑行单车",
                ShortcutItem.ACTION_INTENT_URI, "intent://platformapi/startapp?appId=10000007#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end",
                "", "Scan:Regular",
                0xFF1677FF, true,
                true, 0xFF1677FF, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 5. 支付宝付款码 (智能助理官方 Scheme，配 Miuix 银行卡/支付矢量图标 + 支付宝蓝微透底垫)
        items.add(new ShortcutItem(
            "alipay_pay", "支付宝付款码", "出示收银台付款",
            ShortcutItem.ACTION_INTENT_URI, "alipays://platformapi/startapp?appId=20000056&chInfo=ch_xiaomiLeft",
            "", "BankCards:Regular",
            0xFF1677FF, true,
            true, 0xFF1677FF, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 6. 支付宝收款码 (智能助理官方收钱码协议，配 Miuix 二维码图标 + 支付宝蓝微透底垫)
        items.add(new ShortcutItem(
            "alipay_receive", "支付宝收款码", "面对面二维码收钱",
            ShortcutItem.ACTION_INTENT_URI, "alipays://platformapi/startapp?appId=20000123&chInfo=ch_xiaomi_left&sceneCode=KF_CHANGSHANG&shareUserId=2088831085791813&partnerId=ch_xiaomi_left&pikshemo=YES",
            "", "QrCode:Regular",
            0xFF1677FF, true,
            true, 0xFF1677FF, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 7. 乘车码 (交通乘车码统一入口，配 Miuix 二维码矢量图标 + 银联绿底垫)
        items.add(new ShortcutItem(
                "bus_code", "乘车码", "地铁公交一卡通刷码",
                ShortcutItem.ACTION_INTENT_URI, "intent://platformapi/startapp?appId=200011235#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end",
                "", "QrCode:Regular",
                0xFF52C41A, true,
                true, 0xFF52C41A, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 8. 手电筒 (系统底层 Camera Torch，配 Miuix 手电筒图标 + 暖阳橙微透底垫)
        items.add(new ShortcutItem(
            "flashlight", "手电筒", "开关后置 LED 强光",
            ShortcutItem.ACTION_FLASHLIGHT, "",
            "", "Flashlight:Regular",
            0xFFFAAD14, true,
            true, 0xFFFAAD14, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        // 9. 街拍相机 (系统原生相机标准 Action，配 Miuix 相机图标 + 活力红底垫)
        items.add(new ShortcutItem(
            "camera", "街拍相机", "秒级开启抓拍",
            ShortcutItem.ACTION_INTENT_URI, "intent:#Intent;action=android.media.action.STILL_IMAGE_CAMERA;end",
            "", "Camera:Regular",
            0xFFFF4D4F, true,
            true, 0xFFFF4D4F, GLYPH_PAD_MODE_TRANSLUCENT
        ));

        return items;
    }
}
