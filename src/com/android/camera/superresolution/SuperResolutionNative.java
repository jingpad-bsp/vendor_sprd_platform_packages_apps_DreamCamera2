package com.android.camera.superresolution;

import android.util.Log;

public class SuperResolutionNative {

    static {
        Log.d("SuperResolutionNative", "static initializer: load jni_sprd_srlite start");
        System.loadLibrary("jni_sprd_srlite");
        Log.d("SuperResolutionNative", "static initializer: load jni_sprd_srlite end");
    }

    public static native int init();

    public static native int deinit();

    public static native byte[] process(byte[] image, int image_width, int image_height, int scale);
}
