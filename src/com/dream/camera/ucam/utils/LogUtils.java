/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 */
package com.dream.camera.ucam.utils;

import java.util.StringTokenizer;

import android.graphics.RectF;
import android.hardware.Camera.Parameters;
import android.util.Log;

public abstract class LogUtils {

    private static final String TAG = "[UCAM]";

    private static boolean sDebugEnabled = true;

    private LogUtils() {}

    public static void enableDebug() { sDebugEnabled = true;  }
    public static void disableDebug(){ sDebugEnabled = false; }
    public static boolean isDebugEnabled() { return sDebugEnabled; }

    public static void debug(String tag, String msg, Object ... args) {
        if (sDebugEnabled) {
            Log.d(TAG + tag, String.format(msg, args));
        }
    }

    public static void error(String tag, String msg) {
        Log.e(TAG + tag, msg);
    }

    public static void error(String tag, String msg, Exception e) {
        Log.e(TAG + tag, msg, e);
    }

    public static void dumpRect(RectF rect, String msg) {
        Log.v(TAG, msg + String.format("[%f,%f,%f,%f]",
               rect.left,rect.top,rect.right,rect.bottom));
    }

    public static void printStackTrace() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            Log.v(TAG, stack[i].toString());
        }
    }
}
