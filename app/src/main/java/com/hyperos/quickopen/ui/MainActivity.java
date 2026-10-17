package com.hyperos.quickopen.ui;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.hyperos.quickopen.R;
import com.hyperos.quickopen.config.QuickOpenConfig;
import com.hyperos.quickopen.safety.SafetyStore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private QuickOpenConfig config;

    private RadioGroup rgMaterial;
    private RadioButton rbMatSystem, rbMatSoftGlass, rbMatGaussian;

    private Slider sliderOffsetY, sliderRegionDiameter, sliderIconSize, sliderMaxScale, sliderInfluenceRadius;
    private TextView tvOffsetYVal, tvRegionDiameterVal, tvIconSizeVal, tvMaxScaleVal, tvInfluenceRadiusVal;
    private MaterialSwitch switchShowRegionBg;
    private MaterialSwitch switchHeaderPillBg;

    private RecyclerView rvShortcuts;
    private ShortcutAdapter adapter;

    // 安全模式卡片控件
    private TextView tvSafetyStatus, tvSafetyCrashCount, tvSafetyTotalCrash, tvSafetySession, tvSafetyLastCrash, tvSafetyLog;
    private MaterialSwitch switchHookEnabled;
    private boolean safetyBinding = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = QuickOpenConfig.loadFromApp(this);

        initViews();
        bindData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSafety();
    }

    private void initViews() {
        rgMaterial = findViewById(R.id.rg_material);
        rbMatSystem = findViewById(R.id.rb_mat_system);
        rbMatSoftGlass = findViewById(R.id.rb_mat_soft_glass);
        rbMatGaussian = findViewById(R.id.rb_mat_gaussian);

        sliderOffsetY = findViewById(R.id.slider_offset_y);
        tvOffsetYVal = findViewById(R.id.tv_offset_y_val);

        sliderRegionDiameter = findViewById(R.id.slider_region_diameter);
        tvRegionDiameterVal = findViewById(R.id.tv_region_diameter_val);

        switchShowRegionBg = findViewById(R.id.switch_show_region_bg);
        switchHeaderPillBg = findViewById(R.id.switch_header_pill_bg);

        sliderIconSize = findViewById(R.id.slider_icon_size);
        tvIconSizeVal = findViewById(R.id.tv_icon_size_val);

        sliderMaxScale = findViewById(R.id.slider_max_scale);
        tvMaxScaleVal = findViewById(R.id.tv_max_scale_val);

        sliderInfluenceRadius = findViewById(R.id.slider_influence_radius);
        tvInfluenceRadiusVal = findViewById(R.id.tv_influence_radius_val);

        rvShortcuts = findViewById(R.id.rv_shortcuts);
        rvShortcuts.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            config.shortcuts = QuickOpenConfig.getDefaultShortcuts();
            adapter.notifyDataSetChanged();
            saveConfig();
            Toast.makeText(this, "已恢复默认快捷项目", Toast.LENGTH_SHORT).show();
        });

        // 安全模式卡片控件
        tvSafetyStatus = findViewById(R.id.tv_safety_status);
        tvSafetyCrashCount = findViewById(R.id.tv_safety_crash_count);
        tvSafetyTotalCrash = findViewById(R.id.tv_safety_total_crash);
        tvSafetySession = findViewById(R.id.tv_safety_session);
        tvSafetyLastCrash = findViewById(R.id.tv_safety_last_crash);
        tvSafetyLog = findViewById(R.id.tv_safety_log);
        switchHookEnabled = findViewById(R.id.switch_hook_enabled);

        findViewById(R.id.btn_exit_safe_mode).setOnClickListener(v -> {
            Bundle st = SafetyStore.exitSafeMode(this);
            applySafetyState(st);
            Toast.makeText(this, R.string.toast_exit_safe_mode, Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.btn_clear_crash).setOnClickListener(v -> {
            Bundle st = SafetyStore.clearCrash(this);
            applySafetyState(st);
            Toast.makeText(this, R.string.toast_clear_crash, Toast.LENGTH_SHORT).show();
        });

        switchHookEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
            if (safetyBinding) return;
            Bundle st = SafetyStore.setHookEnabled(this, isChecked);
            applySafetyState(st);
            Toast.makeText(this, isChecked ? R.string.toast_hook_enabled : R.string.toast_hook_disabled,
                Toast.LENGTH_LONG).show();
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 安全模式卡片
    // ──────────────────────────────────────────────────────────────

    private void refreshSafety() {
        applySafetyState(SafetyStore.getState(this));
    }

    private void applySafetyState(@Nullable Bundle st) {
        if (st == null || tvSafetyStatus == null) {
            return;
        }
        boolean safeMode = st.getBoolean("safe_mode", false);
        boolean hookEnabled = st.getBoolean("hook_enabled", true);
        int crashCount = st.getInt("crash_count", 0);
        int threshold = st.getInt("threshold", SafetyStore.CRASH_THRESHOLD);
        int total = st.getInt("total_crash", 0);
        long lastTs = st.getLong("last_crash_ts", 0L);
        boolean sessionHealthy = st.getBoolean("session_healthy", true);
        String log = st.getString("crash_log", "");

        if (safeMode) {
            tvSafetyStatus.setText(R.string.safety_status_safe);
            tvSafetyStatus.setTextColor(getColor(R.color.accent));
        } else if (!hookEnabled) {
            tvSafetyStatus.setText(R.string.safety_status_disabled);
            tvSafetyStatus.setTextColor(getColor(R.color.text_secondary));
        } else {
            tvSafetyStatus.setText(R.string.safety_status_ok);
            tvSafetyStatus.setTextColor(getColor(R.color.primary));
        }

        tvSafetyCrashCount.setText(getString(R.string.safety_crash_count, crashCount, threshold));
        tvSafetyTotalCrash.setText(getString(R.string.safety_total_crash, total));
        tvSafetySession.setText(getString(R.string.safety_session_state,
            getString(sessionHealthy ? R.string.safety_session_yes : R.string.safety_session_no)));

        if (lastTs > 0L) {
            String fmt = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(lastTs));
            tvSafetyLastCrash.setText(getString(R.string.safety_last_crash, fmt));
        } else {
            tvSafetyLastCrash.setText(R.string.safety_last_crash_none);
        }

        tvSafetyLog.setText(log == null || log.trim().isEmpty()
            ? getString(R.string.safety_crash_log_empty) : log);

        safetyBinding = true;
        // 开关显示的是「Hook 实际生效状态」：安全模式激活时 Hook 已被强制停用，
        // 此时开关呈关闭态；用户把它重新打开即等同于退出安全模式并恢复 Hook。
        switchHookEnabled.setChecked(hookEnabled && !safeMode);
        safetyBinding = false;
    }

    private void bindData() {
        // 材质
        if (config.materialStyle == QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM) {
            rbMatSystem.setChecked(true);
        } else if (config.materialStyle == QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) {
            rbMatGaussian.setChecked(true);
        } else {
            rbMatSoftGlass.setChecked(true);
        }

        // 滑块初始值
        sliderOffsetY.setValue(clamp(config.offsetY, 100, 450));
        tvOffsetYVal.setText(String.format(Locale.getDefault(), "%d dp", config.offsetY));

        sliderRegionDiameter.setValue(clamp(config.regionDiameter, 220, 420));
        tvRegionDiameterVal.setText(String.format(Locale.getDefault(), "%d dp", config.regionDiameter));

        switchShowRegionBg.setChecked(config.showRegionBg);
        switchHeaderPillBg.setChecked(config.showHeaderPillBg);

        sliderIconSize.setValue(clamp(config.iconSize, 40, 80));
        tvIconSizeVal.setText(String.format(Locale.getDefault(), "%d dp", config.iconSize));

        sliderMaxScale.setValue(clamp(config.maxScale, 1.10f, 1.80f));
        tvMaxScaleVal.setText(String.format(Locale.getDefault(), "%.2f x", config.maxScale));

        sliderInfluenceRadius.setValue(clamp(config.influenceRadius, 60, 200));
        tvInfluenceRadiusVal.setText(String.format(Locale.getDefault(), "%d dp", config.influenceRadius));

        // 列表
        adapter = new ShortcutAdapter(config.shortcuts, this::saveConfig);
        rvShortcuts.setAdapter(adapter);
    }

    private void setupListeners() {
        rgMaterial.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_mat_system) {
                config.materialStyle = QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM;
            } else if (checkedId == R.id.rb_mat_gaussian) {
                config.materialStyle = QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR;
            } else {
                config.materialStyle = QuickOpenConfig.MATERIAL_SOFT_GLASS;
            }
            saveConfig();
        });

        sliderOffsetY.addOnChangeListener((slider, value, fromUser) -> {
            config.offsetY = (int) value;
            tvOffsetYVal.setText(String.format(Locale.getDefault(), "%d dp", config.offsetY));
            if (fromUser) saveConfig();
        });

        sliderRegionDiameter.addOnChangeListener((slider, value, fromUser) -> {
            config.regionDiameter = (int) value;
            tvRegionDiameterVal.setText(String.format(Locale.getDefault(), "%d dp", config.regionDiameter));
            if (fromUser) saveConfig();
        });

        switchShowRegionBg.setOnCheckedChangeListener((btn, isChecked) -> {
            config.showRegionBg = isChecked;
            saveConfig();
        });

        switchHeaderPillBg.setOnCheckedChangeListener((btn, isChecked) -> {
            config.showHeaderPillBg = isChecked;
            saveConfig();
        });

        sliderIconSize.addOnChangeListener((slider, value, fromUser) -> {
            config.iconSize = (int) value;
            tvIconSizeVal.setText(String.format(Locale.getDefault(), "%d dp", config.iconSize));
            if (fromUser) saveConfig();
        });

        sliderMaxScale.addOnChangeListener((slider, value, fromUser) -> {
            config.maxScale = value;
            tvMaxScaleVal.setText(String.format(Locale.getDefault(), "%.2f x", config.maxScale));
            if (fromUser) saveConfig();
        });

        sliderInfluenceRadius.addOnChangeListener((slider, value, fromUser) -> {
            config.influenceRadius = (int) value;
            tvInfluenceRadiusVal.setText(String.format(Locale.getDefault(), "%d dp", config.influenceRadius));
            if (fromUser) saveConfig();
        });
    }

    private void saveConfig() {
        config.save(this);
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
