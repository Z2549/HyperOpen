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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private QuickOpenConfig config;

    private RadioGroup rgMaterial;
    private RadioButton rbMatSystem, rbMatSoftGlass, rbMatGaussian;

    private Slider sliderOffsetY, sliderRegionDiameter, sliderIconSize, sliderMaxScale, sliderInfluenceRadius;
    private TextView tvOffsetYVal, tvRegionDiameterVal, tvIconSizeVal, tvMaxScaleVal, tvInfluenceRadiusVal;
    private MaterialSwitch switchShowRegionBg;

    private RecyclerView rvShortcuts;
    private ShortcutAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = QuickOpenConfig.loadFromApp(this);

        initViews();
        bindData();
        setupListeners();
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
        sliderOffsetY.setValue(clamp(config.offsetY, 150, 450));
        tvOffsetYVal.setText(String.format(Locale.getDefault(), "%d dp", config.offsetY));

        sliderRegionDiameter.setValue(clamp(config.regionDiameter, 220, 420));
        tvRegionDiameterVal.setText(String.format(Locale.getDefault(), "%d dp", config.regionDiameter));

        switchShowRegionBg.setChecked(config.showRegionBg);

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
