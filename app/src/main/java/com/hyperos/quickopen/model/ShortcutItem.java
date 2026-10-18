package com.hyperos.quickopen.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;

public class ShortcutItem {
    private static final String TAG = "QuickOpen_Shortcut";

    public static final String ACTION_APP = "APP";
    public static final String ACTION_INTENT_URI = "INTENT_URI";
    public static final String ACTION_BROADCAST = "BROADCAST";
    public static final String ACTION_FLASHLIGHT = "FLASHLIGHT";
    public static final String ACTION_SHELL = "SHELL";

    public String id;
    public String title;
    public String subtitle;
    public String actionType;
    public String actionPayload;
    /** 手动指定图标来源包名（优先级最高，留空则自动解析） */
    public String iconPackage;
    /** 手动指定内置符号 key（如 scan/flashlight/camera/calc/note/mic/music/settings/bus/wallet/sparkle） */
    public String iconGlyph;
    public int tintColor;
    public boolean enabled;

    /** 该快捷方式独享的 Miuix 矢量底垫配置 */
    public boolean glyphPadEnabled = false;
    public int glyphPadColor = 0xFF2A82E4;
    public int glyphPadMode = 1; // 0=SOLID, 1=TRANSLUCENT, 2=INVERTED

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload, int tintColor, boolean enabled) {
        this(id, title, subtitle, actionType, actionPayload, "", "", tintColor, enabled, false, 0xFF2A82E4, 1);
    }

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload,
                        String iconPackage, String iconGlyph, int tintColor, boolean enabled) {
        this(id, title, subtitle, actionType, actionPayload, iconPackage, iconGlyph, tintColor, enabled, false, 0xFF2A82E4, 1);
    }

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload,
                        String iconPackage, String iconGlyph, int tintColor, boolean enabled,
                        boolean glyphPadEnabled, int glyphPadColor, int glyphPadMode) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.actionType = actionType;
        this.actionPayload = actionPayload;
        this.iconPackage = iconPackage == null ? "" : iconPackage;
        this.iconGlyph = iconGlyph == null ? "" : iconGlyph;
        this.tintColor = tintColor;
        this.enabled = enabled;
        this.glyphPadEnabled = glyphPadEnabled;
        this.glyphPadColor = glyphPadColor;
        this.glyphPadMode = glyphPadMode;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("subtitle", subtitle);
            json.put("actionType", actionType);
            json.put("actionPayload", actionPayload);
            json.put("iconPackage", iconPackage == null ? "" : iconPackage);
            json.put("iconGlyph", iconGlyph == null ? "" : iconGlyph);
            json.put("tintColor", tintColor);
            json.put("enabled", enabled);
            json.put("glyphPadEnabled", glyphPadEnabled);
            json.put("glyphPadColor", glyphPadColor);
            json.put("glyphPadMode", glyphPadMode);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing ShortcutItem to JSON", e);
        }
        return json;
    }

    public static ShortcutItem fromJson(JSONObject json) {
        return new ShortcutItem(
            json.optString("id", ""),
            json.optString("title", "未命名"),
            json.optString("subtitle", ""),
            json.optString("actionType", ACTION_APP),
            json.optString("actionPayload", ""),
            json.optString("iconPackage", ""),
            json.optString("iconGlyph", ""),
            json.optInt("tintColor", Color.CYAN),
            json.optBoolean("enabled", true),
            json.optBoolean("glyphPadEnabled", false),
            json.optInt("glyphPadColor", 0xFF2A82E4),
            json.optInt("glyphPadMode", 1)
        );
    }

    public ShortcutItem copy() {
        return new ShortcutItem(
            id, title, subtitle, actionType, actionPayload,
            iconPackage, iconGlyph, tintColor, enabled,
            glyphPadEnabled, glyphPadColor, glyphPadMode
        );
    }

    /**
     * 在 SystemUI 上下文中分发执行该快捷方式动作
     */
    public void execute(Context context) {
        if (context == null) return;
        try {
            switch (actionType) {
                case ACTION_APP: {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(actionPayload);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                    } else {
                        showToast(context, "未找到目标应用: " + actionPayload);
                    }
                    break;
                }
                case ACTION_INTENT_URI: {
                    Intent intent = Intent.parseUri(actionPayload, Intent.URI_INTENT_SCHEME);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else if ("com.tencent.mm".equals(intent.getPackage()) || actionPayload.contains("com.tencent.mm")) {
                        // 微信未适配快捷 Intent 时的兜底
                        Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(launch);
                        } else {
                            showToast(context, "未找到微信应用");
                        }
                    } else if (actionPayload.contains("alipays://") || "com.eg.android.AlipayGphone".equals(intent.getPackage())) {
                        // 支付宝未注册平台协议时的兜底
                        Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.eg.android.AlipayGphone");
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(launch);
                        } else {
                            showToast(context, "未找到支付宝应用");
                        }
                    } else {
                        // 尝试直接启动
                        context.startActivity(intent);
                    }
                    break;
                }
                case ACTION_BROADCAST: {
                    Intent intent = new Intent(actionPayload);
                    context.sendBroadcast(intent);
                    break;
                }
                case ACTION_FLASHLIGHT: {
                    toggleFlashlight(context);
                    break;
                }
                case ACTION_SHELL: {
                    runShellCommand(actionPayload);
                    break;
                }
                default:
                    Log.w(TAG, "未知动作类型: " + actionType);
                    break;
            }
        } catch (Throwable t) {
            Log.e(TAG, "执行快捷动作失败: " + title, t);
            showToast(context, "启动失败: " + t.getMessage());
        }
    }

    private static boolean isFlashlightOn = false;

    private static void toggleFlashlight(Context context) {
        try {
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                String[] cameraIdList = cm.getCameraIdList();
                if (cameraIdList.length > 0) {
                    String cameraId = cameraIdList[0];
                    isFlashlightOn = !isFlashlightOn;
                    cm.setTorchMode(cameraId, isFlashlightOn);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "切换手电筒状态失败", t);
        }
    }

    private static void runShellCommand(String cmd) {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("sh");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes(cmd + "\n");
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();
            } catch (Throwable t) {
                Log.e(TAG, "Shell 脚本执行异常", t);
            }
        }).start();
    }

    private static void showToast(Context context, String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show()
        );
    }
}
