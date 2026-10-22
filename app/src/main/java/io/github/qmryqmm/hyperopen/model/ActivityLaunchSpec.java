package io.github.qmryqmm.hyperopen.model;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 「启动 Activity (ACTIVITY)」动作的结构化 Intent 规格 —— actionPayload 的 JSON 形态。
 *
 * JSON 字段：
 * - action     : Intent Action（可空；编辑界面提供预设与自定义）
 * - data       : Data URI 字符串（可空）
 * - component  : 目标组件「仅类名」（可空；兼容旧数据「包名/类名」）
 * - packageName: 目标包名（可空；component 为空时按包名启动/解析）
 * - extras     : [{key,type,value}]，type ∈ string/int/long/bool/float
 * - newTask    : 是否加 Intent.FLAG_ACTIVITY_NEW_TASK（默认 true）
 * - useRoot    : 是否经 root（桥接 su）am start 打开（默认 false）
 */
public final class ActivityLaunchSpec {
    private static final String TAG = "HyperOpen_ActivitySpec";

    public static final String TYPE_STRING = "string";
    public static final String TYPE_INT = "int";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_FLOAT = "float";

    public static final class Extra {
        public String key = "";
        public String type = TYPE_STRING;
        public String value = "";

        public Extra() {}

        public Extra(String key, String type, String value) {
            this.key = key == null ? "" : key;
            this.type = type == null || type.isEmpty() ? TYPE_STRING : type;
            this.value = value == null ? "" : value;
        }
    }

    public String action = "";
    public String data = "";
    public String component = "";
    public String packageName = "";
    public List<Extra> extras = new ArrayList<>();
    public boolean newTask = true;
    public boolean useRoot = false;

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("action", action == null ? "" : action);
            o.put("data", data == null ? "" : data);
            o.put("component", component == null ? "" : component);
            o.put("packageName", packageName == null ? "" : packageName);
            o.put("newTask", newTask);
            o.put("useRoot", useRoot);
            JSONArray arr = new JSONArray();
            for (Extra e : extras) {
                if (e == null) continue;
                JSONObject j = new JSONObject();
                j.put("key", e.key == null ? "" : e.key);
                j.put("type", e.type == null || e.type.isEmpty() ? TYPE_STRING : e.type);
                j.put("value", e.value == null ? "" : e.value);
                arr.put(j);
            }
            o.put("extras", arr);
            return o.toString();
        } catch (Throwable t) {
            Log.e(TAG, "ActivityLaunchSpec 序列化失败", t);
            return "{}";
        }
    }

    public static ActivityLaunchSpec fromJson(String payload) {
        ActivityLaunchSpec spec = new ActivityLaunchSpec();
        if (payload == null || payload.trim().isEmpty()) return spec;
        try {
            JSONObject o = new JSONObject(payload);
            spec.action = o.optString("action", "");
            spec.data = o.optString("data", "");
            spec.component = o.optString("component", "");
            spec.packageName = o.optString("packageName", "");
            spec.newTask = o.optBoolean("newTask", true);
            spec.useRoot = o.optBoolean("useRoot", false);
            JSONArray arr = o.optJSONArray("extras");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject j = arr.optJSONObject(i);
                    if (j == null) continue;
                    spec.extras.add(new Extra(
                            j.optString("key", ""),
                            j.optString("type", TYPE_STRING),
                            j.optString("value", "")));
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "ActivityLaunchSpec 解析失败，按空规格处理: " + payload, t);
        }
        return spec;
    }

    /** 组装最终 Intent（newTask=true 时带 FLAG_ACTIVITY_NEW_TASK） */
    public Intent buildIntent() {
        Intent intent = new Intent();
        String a = normalizeAction(action);
        if (!a.isEmpty()) intent.setAction(a);
        String d = data == null ? "" : data.trim();
        if (!d.isEmpty()) {
            try {
                intent.setData(Uri.parse(d));
            } catch (Throwable t) {
                Log.w(TAG, "Data URI 解析失败，忽略: " + d, t);
            }
        }
        ComponentName cn = resolveComponent();
        if (cn != null) {
            intent.setComponent(cn);
        } else {
            String comp = component == null ? "" : component.trim();
            if (!comp.isEmpty()) {
                Log.w(TAG, "目标组件无法解析（需 目标包名+类名；兼容旧式 包名/类名），忽略: " + comp);
            }
            String pkg = packageName == null ? "" : packageName.trim();
            if (!pkg.isEmpty()) intent.setPackage(pkg);
        }
        for (Extra e : extras) {
            if (e == null) continue;
            String k = e.key == null ? "" : e.key.trim();
            if (k.isEmpty()) continue;
            String v = e.value == null ? "" : e.value;
            String type = e.type == null ? TYPE_STRING : e.type.trim();
            try {
                switch (type) {
                    case TYPE_INT:   intent.putExtra(k, Integer.parseInt(v.trim())); break;
                    case TYPE_LONG:  intent.putExtra(k, Long.parseLong(v.trim())); break;
                    case TYPE_BOOL:  intent.putExtra(k, "true".equalsIgnoreCase(v.trim())); break;
                    case TYPE_FLOAT: intent.putExtra(k, Float.parseFloat(v.trim())); break;
                    default:         intent.putExtra(k, v); break;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Extra 解析失败，按字符串放入: " + k + "=" + v, t);
                intent.putExtra(k, v);
            }
        }
        if (newTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /** 图标自动解析用：目标包名（component 优先，其次 packageName）；都为空返回 null */
    public String resolvePackage() {
        ComponentName cn = resolveComponent();
        if (cn != null && cn.getPackageName() != null && !cn.getPackageName().isEmpty()) {
            return cn.getPackageName();
        }
        String pkg = packageName == null ? "" : packageName.trim();
        return pkg.isEmpty() ? null : pkg;
    }

    /**
     * 规范化目标组件，容错三种输入：
     * 1) "包名/类名"（旧数据兼容，标准 ComponentName.flatten 形式）
     * 2) "类名"（如 com.follow.clash.MainActivity）—— 拼上「目标包名」
     * 3) ".相对类名"（如 .MainActivity）—— 拼上「目标包名」前缀
     * 解析不出来返回 null（例如只填了类名但没填包名）。
     */
    public ComponentName resolveComponent() {
        String comp = component == null ? "" : component.trim();
        if (comp.isEmpty()) return null;
        String pkg = packageName == null ? "" : packageName.trim();
        if (comp.indexOf('/') >= 0) {
            return ComponentName.unflattenFromString(comp);
        }
        if (pkg.isEmpty()) return null;
        String cls = comp.startsWith(".") ? pkg + comp : comp;
        return new ComponentName(pkg, cls);
    }

    /**
     * 规范化 Action：带 '.' 的原样返回（标准全限定名或自定义 scheme action）；
     * 不带 '.' 的简写（main/view/send/...，大小写不敏感）映射为 android.intent.action.*，
     * SETTINGS 单独映射到 android.settings.SETTINGS。
     */
    public static String normalizeAction(String action) {
        String a = action == null ? "" : action.trim();
        if (a.isEmpty()) return "";
        if (a.indexOf('.') >= 0) return a;
        String up = a.toUpperCase(java.util.Locale.ROOT);
        if ("SETTINGS".equals(up)) return "android.settings.SETTINGS";
        return "android.intent.action." + up;
    }
}
