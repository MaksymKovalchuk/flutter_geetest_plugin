package com.example.flutter_geetest;

import android.util.Log;


class LogUtil {
    private static final String TAG = "Flutter_GeetestPlugin";

    private static boolean DEBUG_ENABLE = BuildConfig.DEBUG;

    static void log(String msg) {
        if(DEBUG_ENABLE) Log.d(TAG, msg);
    }
}
