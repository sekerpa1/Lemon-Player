package com.lemon.player.utils;

import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.lemon.player.BuildConfig;

public class LogUtils {

    private LogUtils() {
        //no instance
    }

    public static void logException(String tag, String message, @Nullable Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message + "\nThrowable: " + (throwable != null ? throwable.getMessage() : null));
            if (throwable != null) {
                throwable.printStackTrace();
            }
        } else {
            Crashlytics.log(Log.ERROR, tag, message + "\nThrowable: " + (throwable != null ? throwable.getMessage() : null));
            Crashlytics.logException(throwable);
        }
    }
}