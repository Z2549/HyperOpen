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
    public static final String ACTION_XIAOAI = "XIAOAI";
    public static final String ACTION_MUTE = "MUTE";
    public static final String ACTION_DND = "DND";
    public static final String ACTION_CAMERA = "CAMERA";

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
    /** 该快捷方式独享的应用图标形状裁切方案：1=标准抗锯齿圆形裁切, 2=自适应外扩满铺 */
    public int appIconCropMode = 2;

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload, int tintColor, boolean enabled) {
        this(id, title, subtitle, actionType, actionPayload, "", "", tintColor, enabled, false, 0xFF2A82E4, 1, 2);
    }

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload,
                        String iconPackage, String iconGlyph, int tintColor, boolean enabled) {
        this(id, title, subtitle, actionType, actionPayload, iconPackage, iconGlyph, tintColor, enabled, false, 0xFF2A82E4, 1, 2);
    }

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload,
                        String iconPackage, String iconGlyph, int tintColor, boolean enabled,
                        boolean glyphPadEnabled, int glyphPadColor, int glyphPadMode) {
        this(id, title, subtitle, actionType, actionPayload, iconPackage, iconGlyph, tintColor, enabled, glyphPadEnabled, glyphPadColor, glyphPadMode, 2);
    }

    public ShortcutItem(String id, String title, String subtitle, String actionType, String actionPayload,
                        String iconPackage, String iconGlyph, int tintColor, boolean enabled,
                        boolean glyphPadEnabled, int glyphPadColor, int glyphPadMode, int appIconCropMode) {
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
        this.appIconCropMode = appIconCropMode > 0 ? appIconCropMode : 2;
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
            json.put("appIconCropMode", appIconCropMode);
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
            json.optInt("glyphPadMode", 1),
            json.optInt("appIconCropMode", 2)
        );
    }

    public ShortcutItem copy() {
        return new ShortcutItem(
            id, title, subtitle, actionType, actionPayload,
            iconPackage, iconGlyph, tintColor, enabled,
            glyphPadEnabled, glyphPadColor, glyphPadMode, appIconCropMode
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
                    try {
                        Intent intent = Intent.parseUri(actionPayload, Intent.URI_INTENT_SCHEME);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (SecurityException se) {
                        Log.w(TAG, "直接拉起 Intent 被安全权限拦截，尝试通过 Root/Shell 兜底执行: " + actionPayload, se);
                        // 当从 MainActivity 运行且遇到非导出组件权限问题时，通过 su 执行 am start
                        runRootAmStart(actionPayload);
                    } catch (Throwable t) {
                        Log.e(TAG, "启动 Intent 异常: " + actionPayload, t);
                        // 尝试应用启动兜底
                        if (actionPayload.contains("com.tencent.mm")) {
                            Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                            if (launch != null) {
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(launch);
                            }
                        } else if (actionPayload.contains("com.eg.android.AlipayGphone") || actionPayload.contains("alipays://")) {
                            Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.eg.android.AlipayGphone");
                            if (launch != null) {
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(launch);
                            }
                        } else {
                            showToast(context, "启动失败: " + t.getMessage());
                        }
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
                case ACTION_XIAOAI: {
                    wakeUpXiaoAi(context);
                    break;
                }
                case ACTION_MUTE: {
                    toggleSilentMode(context);
                    break;
                }
                case ACTION_DND: {
                    toggleDndMode(context);
                    break;
                }
                case ACTION_CAMERA: {
                    openCamera(context);
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

    private static void wakeUpXiaoAi(Context context) {
        try {
            Intent wakeBroadcast = new Intent("com.miui.voiceassist.WAKEUP");
            wakeBroadcast.setPackage("com.miui.voiceassist");
            context.sendBroadcast(wakeBroadcast);
        } catch (Throwable ignored) {}

        try {
            Intent intent = new Intent("android.intent.action.VOICE_COMMAND");
            intent.setPackage("com.miui.voiceassist");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            Intent assistIntent = new Intent(Intent.ACTION_ASSIST);
            assistIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(assistIntent);
        } catch (Throwable t) {
            Log.e(TAG, "唤醒小爱失败", t);
            showToast(context, "唤醒小爱失败: " + t.getMessage());
        }
    }

    private static void toggleSilentMode(Context context) {
        try {
            android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int currentMode = am.getRingerMode();
                if (currentMode == android.media.AudioManager.RINGER_MODE_SILENT || currentMode == android.media.AudioManager.RINGER_MODE_VIBRATE) {
                    am.setRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL);
                    showToast(context, "已开启响铃模式");
                } else {
                    am.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT);
                    showToast(context, "已开启静音模式");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "切换静音失败", t);
            showToast(context, "切换静音失败: " + t.getMessage());
        }
    }

    private static void toggleDndMode(Context context) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                int currentFilter = nm.getCurrentInterruptionFilter();
                if (currentFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                    nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    showToast(context, "已开启勿扰模式");
                } else {
                    nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
                    showToast(context, "已关闭勿扰模式");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "切换勿扰模式失败", t);
            showToast(context, "切换勿扰失败: " + t.getMessage());
        }
    }

    private static void openCamera(Context context) {
        try {
            Intent intent = new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Throwable t2) {
                Log.e(TAG, "打开相机失败", t2);
                showToast(context, "打开相机失败: " + t2.getMessage());
            }
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

    private static void runRootAmStart(String payload) {
        new Thread(() -> {
            DataOutputStream os = null;
            Process p = null;
            try {
                p = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(p.getOutputStream());
                // 使用双引号包裹 payload，避免 Intent URI 中的分号被 sh 截断识别为命令分隔符
                String escaped = payload.replace("\"", "\\\"");
                os.writeBytes("am start \"" + escaped + "\"\n");
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();
            } catch (Throwable t) {
                Log.e(TAG, "Root am start 失败: " + payload, t);
            } finally {
                if (os != null) {
                    try { os.close(); } catch (Throwable ignored) {}
                }
                if (p != null) {
                    try { p.destroy(); } catch (Throwable ignored) {}
                }
            }
        }).start();
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
