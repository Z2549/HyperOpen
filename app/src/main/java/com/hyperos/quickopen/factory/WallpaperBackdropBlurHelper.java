package com.hyperos.quickopen.factory;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * HyperOS 锁屏壁纸精准采样与独立局部模糊辅助器：
 * 负责从系统底层获取当前锁屏壁纸源，供圆形视界与胶囊在全屏无模糊时实施独立的局部硬件毛玻璃渲染。
 */
public final class WallpaperBackdropBlurHelper {
    private static final String TAG = "QuickOpen_Wallpaper";

    private static Bitmap sCachedWallpaper = null;
    private static long sLastFetchTime = 0;

    private WallpaperBackdropBlurHelper() {}

    /**
     * 获取当前设备的真实锁屏壁纸 (优先读取 system 锁屏原始文件，带内存缓存)
     */
    @Nullable
    public static synchronized Bitmap getLockWallpaperBitmap(Context context) {
        long now = System.currentTimeMillis();
        if (sCachedWallpaper != null && !sCachedWallpaper.isRecycled() && (now - sLastFetchTime < 10000)) {
            return sCachedWallpaper;
        }

        Bitmap bmp = null;

        // 1. 尝试直接从系统锁屏壁纸私有路径读取 (SystemUI uid=1000 拥有原生读权限)
        try {
            File lockFile = new File("/data/system/users/0/wallpaper_lock");
            if (lockFile.exists() && lockFile.canRead()) {
                bmp = BitmapFactory.decodeFile(lockFile.getAbsolutePath());
                if (bmp != null) {
                    sCachedWallpaper = bmp;
                    sLastFetchTime = now;
                    Log.i(TAG, "成功由 /data/system/users/0/wallpaper_lock 解码真实锁屏壁纸: " + bmp.getWidth() + "x" + bmp.getHeight());
                    return bmp;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "直接读取 wallpaper_lock 失败: " + t.getMessage());
        }

        // 2. 尝试从备用壁纸路径读取
        try {
            File origFile = new File("/data/system/users/0/wallpaper_lock_orig");
            if (origFile.exists() && origFile.canRead()) {
                bmp = BitmapFactory.decodeFile(origFile.getAbsolutePath());
                if (bmp != null) {
                    sCachedWallpaper = bmp;
                    sLastFetchTime = now;
                    return bmp;
                }
            }
        } catch (Throwable ignored) {}

        // 3. 通过系统 WallpaperManager 提取
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            Drawable d = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    d = wm.getDrawable(WallpaperManager.FLAG_LOCK);
                } catch (Throwable ignored) {}
            }
            if (d == null) {
                d = wm.getDrawable();
            }

            if (d instanceof BitmapDrawable) {
                bmp = ((BitmapDrawable) d).getBitmap();
            } else if (d != null) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                int w = dm.widthPixels;
                int h = dm.heightPixels;
                bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, w, h);
                d.draw(c);
            }

            if (bmp != null) {
                sCachedWallpaper = bmp;
                sLastFetchTime = now;
                Log.i(TAG, "成功通过 WallpaperManager 提取壁纸: " + bmp.getWidth() + "x" + bmp.getHeight());
                return bmp;
            }
        } catch (Throwable t) {
            Log.d(TAG, "WallpaperManager 提取异常: " + t.getMessage());
        }

        return null;
    }
}
