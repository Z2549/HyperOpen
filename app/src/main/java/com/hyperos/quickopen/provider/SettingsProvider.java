package com.hyperos.quickopen.provider;

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

import com.hyperos.quickopen.config.QuickOpenConfig;

public class SettingsProvider extends ContentProvider {
    private static final String TAG = "QuickOpen_Provider";
    public static final String AUTHORITY = "com.hyperos.quickopen.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final String METHOD_GET_CONFIG = "get_config";

    @Override
    public boolean onCreate() {
        Log.i(TAG, "SettingsProvider 初始化成功");
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (METHOD_GET_CONFIG.equals(method)) {
            Context ctx = getContext();
            if (ctx != null) {
                SharedPreferences sp = ctx.getSharedPreferences(QuickOpenConfig.PREF_NAME, Context.MODE_PRIVATE);
                Bundle bundle = new Bundle();
                bundle.putInt("material_style", sp.getInt("material_style", QuickOpenConfig.MATERIAL_SOFT_GLASS));
                bundle.putInt("offset_y", sp.getInt("offset_y", 280));
                bundle.putInt("region_diameter", sp.getInt("region_diameter", 330));
                bundle.putBoolean("show_region_bg", sp.getBoolean("show_region_bg", true));
                bundle.putInt("icon_size", sp.getInt("icon_size", 58));
                bundle.putFloat("max_scale", sp.getFloat("max_scale", 1.40f));
                bundle.putInt("influence_radius", sp.getInt("influence_radius", 120));
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
        return "vnd.android.cursor.item/vnd.com.hyperos.quickopen.config";
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
}
