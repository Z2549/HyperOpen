package io.github.qmryqmm.hyperopen.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

/**
 * SystemUI 进程内的跨进程调用专用后台线程。
 *
 * 所有发往模块 App 的 ContentResolver.call() 都必须走这里，绝不能出现在
 * SystemUI 主线程 / 手势线程上：模块 App 进程经常被系统冻结（frozen）或已被杀，
 * Provider 响应前要先解冻 / 冷启动，耗时可达秒级；同期 system_server 的 binder
 * 线程若被其它模块拖慢，阻塞时间还会进一步放大，直接导致 SystemUI 输入 ANR。
 */
public final class IpcExecutor {
    private static final String TAG = "HyperOpen_Ipc";

    private static volatile Handler sBg;
    private static volatile Handler sMain;

    private IpcExecutor() {}

    private static Handler bg() {
        Handler h = sBg;
        if (h == null) {
            synchronized (IpcExecutor.class) {
                h = sBg;
                if (h == null) {
                    HandlerThread t = new HandlerThread("HyperOpen-IPC", Process.THREAD_PRIORITY_BACKGROUND);
                    t.start();
                    h = new Handler(t.getLooper());
                    sBg = h;
                }
            }
        }
        return h;
    }

    private static Handler main() {
        Handler h = sMain;
        if (h == null) {
            h = new Handler(Looper.getMainLooper());
            sMain = h;
        }
        return h;
    }

    /** 在后台 IPC 线程执行（串行，异常被吞掉并记录，绝不外抛）。 */
    public static void runAsync(final Runnable r) {
        runAsyncDelayed(r, 0);
    }

    public static void runAsyncDelayed(final Runnable r, long delayMs) {
        try {
            bg().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Log.w(TAG, "后台任务异常", t);
                    }
                }
            }, Math.max(0, delayMs));
        } catch (Throwable t) {
            Log.w(TAG, "投递后台任务失败", t);
        }
    }

    /** 切回主线程执行；若当前已在主线程则直接执行。 */
    public static void runOnMain(final Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                r.run();
            } catch (Throwable t) {
                Log.w(TAG, "主线程任务异常", t);
            }
            return;
        }
        try {
            main().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Log.w(TAG, "主线程任务异常", t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "投递主线程任务失败", t);
        }
    }
}
