package io.github.qmryqmm.hyperopen.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.qmryqmm.hyperopen.config.HyperOpenConfig;
import io.github.qmryqmm.hyperopen.safety.SafetyStore;

public class SettingsProvider extends ContentProvider {
    private static final String TAG = "HyperOpen_Provider";
    public static final String AUTHORITY = "io.github.qmryqmm.hyperopen.provider";

    public static final String METHOD_GET_CONFIG = "get_config";
    public static final String METHOD_SAVE_FOD_CENTER = "save_fod_center";

    /** 安全模式：启动闸门查询（SystemUI 每次启动时调用，并据此决定是否安装 Hook） */
    public static final String METHOD_SAFETY_CHECK = "safety_check";
    /** 安全模式：即时上报一次 Hook 相关崩溃 */
    public static final String METHOD_SAFETY_REPORT = "safety_report_crash";
    /** 安全模式：存活确认（Hook 装好后一段时间仍活着 → 本次会话视为健康） */
    public static final String METHOD_SAFETY_HEARTBEAT = "safety_heartbeat";
    /** 安全模式：只读状态查询（供模块 UI 显示） */
    public static final String METHOD_SAFETY_STATE = "safety_state";
    /** 安全模式：退出安全模式并重新启用 Hook */
    public static final String METHOD_SAFETY_EXIT = "safety_exit";
    /** 安全模式：清空崩溃计数与崩溃日志 */
    public static final String METHOD_SAFETY_CLEAR = "safety_clear";
    /** 安全模式：Hook 总开关 */
    public static final String METHOD_SAFETY_SET_ENABLED = "safety_set_enabled";
    /** 安全模式：把本次 SystemUI 会话登记为「已安装 Hook」，其异常退出才计入崩溃队列 */
    public static final String METHOD_SAFETY_MARK_HOOKED = "safety_mark_hooked";

    @Override
    public boolean onCreate() {
        Log.i(TAG, "SettingsProvider 初始化成功");
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        // ── 安全模式相关方法：写入方只在本进程（模块 App），SystemUI 侧只读或上报 ──
        // 注意：call() 内的任何未捕获异常都会跨进程抛回 SystemUI 并可能导致其崩溃，
        // 因此这里必须整段兜底，绝不让异常逃逸出 Provider。
        if (isSafetyMethod(method)) {
            Context sctx = getContext();
            if (sctx == null) {
                return wrap(null);
            }
            try {
                if (METHOD_SAFETY_CHECK.equals(method)) {
                    return wrap(SafetyStore.safetyCheck(sctx));
                }
                if (METHOD_SAFETY_REPORT.equals(method)) {
                    return wrap(SafetyStore.reportCrash(sctx, arg));
                }
                if (METHOD_SAFETY_HEARTBEAT.equals(method)) {
                    return wrap(SafetyStore.heartbeat(sctx));
                }
                if (METHOD_SAFETY_STATE.equals(method)) {
                    return wrap(SafetyStore.getState(sctx));
                }
                if (METHOD_SAFETY_EXIT.equals(method)) {
                    Log.w(TAG, "收到退出安全模式请求，Hook 将在 SystemUI 下次启动时恢复");
                    return wrap(SafetyStore.exitSafeMode(sctx));
                }
                if (METHOD_SAFETY_CLEAR.equals(method)) {
                    return wrap(SafetyStore.clearCrash(sctx));
                }
                if (METHOD_SAFETY_SET_ENABLED.equals(method)) {
                    boolean enabled = extras == null || extras.getBoolean("enabled", true);
                    return wrap(SafetyStore.setHookEnabled(sctx, enabled));
                }
                if (METHOD_SAFETY_MARK_HOOKED.equals(method)) {
                    return wrap(SafetyStore.markSessionHooked(sctx));
                }
            } catch (Throwable t) {
                Log.e(TAG, "安全模式方法 " + method + " 执行异常，返回保守结果", t);
                return wrap(null);
            }
        }

        if (METHOD_SAVE_FOD_CENTER.equals(method)) {
            Context ctx = getContext();
            if (ctx != null && extras != null) {
                try {
                    float fx = extras.getFloat("fod_x", 0f);
                    float fy = extras.getFloat("fod_y", 0f);
                    if (fx > 0f && fy > 0f) {
                        SharedPreferences sp = ctx.getSharedPreferences(HyperOpenConfig.PREF_NAME, Context.MODE_PRIVATE);
                        sp.edit()
                            .putFloat("last_real_fod_x", fx)
                            .putFloat("last_real_fod_y", fy)
                            .apply();
                    }
                } catch (Throwable ignored) {}
            }
            return new Bundle();
        }

        if (METHOD_GET_CONFIG.equals(method)) {
            Context ctx = getContext();
            if (ctx != null) {
                SharedPreferences sp = ctx.getSharedPreferences(HyperOpenConfig.PREF_NAME, Context.MODE_PRIVATE);
                Bundle bundle = new Bundle();
                bundle.putInt("material_style", sp.getInt("material_style", HyperOpenConfig.MATERIAL_GAUSSIAN_BLUR));
                bundle.putInt("offset_y", sp.getInt("offset_y", 100));
                bundle.putInt("region_diameter", sp.getInt("region_diameter", 220));
                bundle.putBoolean("show_region_bg", sp.getBoolean("show_region_bg", false));
                bundle.putBoolean("show_header_pill_bg", sp.getBoolean("show_header_pill_bg", false));
                bundle.putBoolean("show_header_pill", sp.getBoolean("show_header_pill", true));
                bundle.putInt("header_top_margin", sp.getInt("header_top_margin", 100));
                bundle.putInt("header_title_size", sp.getInt("header_title_size", 17));
                bundle.putInt("header_subtitle_size", sp.getInt("header_subtitle_size", 11));
                bundle.putInt("header_blur_radius", sp.getInt("header_blur_radius", 25));
                bundle.putBoolean("header_text_centered", sp.getBoolean("header_text_centered", true));
                bundle.putInt("icon_size", sp.getInt("icon_size", 60));
                bundle.putFloat("max_scale", sp.getFloat("max_scale", 1.40f));
                bundle.putInt("influence_radius", sp.getInt("influence_radius", 70));
                bundle.putFloat("icon_alpha", sp.getFloat("icon_alpha", 1.0f));
                bundle.putFloat("fisheye_zoom_alpha", sp.getFloat("fisheye_zoom_alpha", 1.0f));
                bundle.putFloat("fullscreen_blur_mask_alpha", sp.getFloat("fullscreen_blur_mask_alpha", 1.0f));
                bundle.putFloat("fullscreen_dim_alpha", sp.getFloat("fullscreen_dim_alpha", 0.45f));
                bundle.putInt("expand_delay_ms", sp.getInt("expand_delay_ms", 0));
                bundle.putBoolean("haptic_enabled", sp.getBoolean("haptic_enabled", true));
                bundle.putInt("haptic_strength", sp.getInt("haptic_strength", 140));
                bundle.putInt("haptic_duration_ms", sp.getInt("haptic_duration_ms", 12));
                bundle.putInt("haptic_mode", sp.getInt("haptic_mode", HyperOpenConfig.HAPTIC_MODE_CLICK));
                bundle.putFloat("last_real_fod_x", sp.getFloat("last_real_fod_x", 0f));
                bundle.putFloat("last_real_fod_y", sp.getFloat("last_real_fod_y", 0f));
                bundle.putInt("region_blur_radius", sp.getInt("region_blur_radius", 25));
                bundle.putInt("icon_spacing_offset", sp.getInt("icon_spacing_offset", 5));
                bundle.putInt("bubble_padding_percent", sp.getInt("bubble_padding_percent", 0));
                bundle.putInt("bubble_blur_radius", sp.getInt("bubble_blur_radius", 25));
                bundle.putFloat("selected_icon_scale", sp.getFloat("selected_icon_scale", 1.15f));
                bundle.putInt("app_icon_crop_mode", sp.getInt("app_icon_crop_mode", HyperOpenConfig.CROP_ADAPTIVE_EXPAND));
                bundle.putBoolean("glyph_pad_enabled", sp.getBoolean("glyph_pad_enabled", true));
                bundle.putInt("glyph_pad_color", sp.getInt("glyph_pad_color", 0xFF2A82E4));
                bundle.putInt("glyph_pad_mode", sp.getInt("glyph_pad_mode", HyperOpenConfig.GLYPH_PAD_MODE_TRANSLUCENT));
                bundle.putBoolean("icon_glow_effect", sp.getBoolean("icon_glow_effect", true));
                bundle.putString("shortcuts_json", sp.getString("shortcuts_json", null));
                Log.d(TAG, "成功响应 SystemUI 配置查询: offsetY=" + bundle.getInt("offset_y"));
                return bundle;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.item/vnd.io.github.qmryqmm.hyperopen.config";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    private static boolean isSafetyMethod(@Nullable String method) {
        if (method == null) {
            return false;
        }
        return METHOD_SAFETY_CHECK.equals(method)
            || METHOD_SAFETY_REPORT.equals(method)
            || METHOD_SAFETY_HEARTBEAT.equals(method)
            || METHOD_SAFETY_STATE.equals(method)
            || METHOD_SAFETY_EXIT.equals(method)
            || METHOD_SAFETY_CLEAR.equals(method)
            || METHOD_SAFETY_SET_ENABLED.equals(method)
            || METHOD_SAFETY_MARK_HOOKED.equals(method);
    }

    /**
     * SafetyStore 返回的 Bundle 可能为 null（例如 Context 获取失败），
     * 这里统一兜底为一个「安全模式已激活」的空结果，绝不把 null 抛回 SystemUI：
     * 对调用方而言「查不到状态」与「不允许安装 Hook」应当同向处理，宁可少装一次 Hook。
     */
    @NonNull
    private Bundle wrap(@Nullable Bundle b) {
        if (b != null) {
            return b;
        }
        Log.e(TAG, "安全模式状态读取失败，返回保守结果（禁止安装 Hook）");
        Bundle fallback = new Bundle();
        fallback.putBoolean("allowed", false);
        fallback.putBoolean("safe_mode", true);
        fallback.putBoolean("hook_enabled", false);
        fallback.putBoolean("just_triggered", false);
        fallback.putBoolean("premature_exit", false);
        fallback.putInt("crash_count", 0);
        fallback.putInt("total_crash", 0);
        fallback.putInt("threshold", SafetyStore.CRASH_THRESHOLD);
        return fallback;
    }
}
