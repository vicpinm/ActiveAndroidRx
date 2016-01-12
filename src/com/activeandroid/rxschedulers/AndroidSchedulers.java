package com.activeandroid.rxschedulers;

import android.os.Handler;
import android.os.Looper;

import rx.Scheduler;

/** Android-specific Schedulers. */
public final class AndroidSchedulers {
    private AndroidSchedulers() {
        throw new AssertionError("No instances");
    }

    private static final Scheduler MAIN_THREAD_SCHEDULER =
            new HandlerScheduler(new Handler(Looper.getMainLooper()));

    /** A {@link Scheduler} which executes actions on the Android UI thread. */
    public static Scheduler mainThread() {
        Scheduler scheduler =
                RxAndroidPlugins.getInstance().getSchedulersHook().getMainThreadScheduler();
        return scheduler != null ? scheduler : MAIN_THREAD_SCHEDULER;
    }
}