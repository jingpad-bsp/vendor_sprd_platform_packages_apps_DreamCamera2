
package com.sprd.camera.storagepath;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Environment;
import android.os.storage.VolumeInfo;
import android.os.storage.IStorageManager;
import android.util.Log;

public class StorageUtilProxy {

    public static final String TAG = "StorageUtilProxy";
    private static Class<?> environmentExClazz = null;
    private static Class<?> environmentClazz = null;
    private static String mInternalState;
    private static long mLastInternalStateGetTime = 0l;
    private static String mExternalState;
    private static long mLastExternalStateGetTime = 0l;

    private static boolean OLD_SDK_VERSION = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? true : false;
    static {
        try {
            environmentClazz = Class.forName("android.os.Environment");
            environmentExClazz = Class.forName("android.os.EnvironmentEx");
        } catch (Exception e) {
            Log.e(TAG, "get class of Environment or EnvironmentEx error:" + e.getMessage());
        }

    }

    public static File getInternalStoragePath() {
        Method method;
        File file = null;
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getInternalStoragePath");
                file = (File) method.invoke(environmentClazz);
            } else {
                method = environmentExClazz.getMethod("getInternalStoragePath");
                file = (File) method.invoke(environmentExClazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "getInternalStoragePath error:" + e.getMessage());
        }

        return file;

    }

    public static File getExternalStoragePath() {
        Method method;
        File file = null;
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getExternalStoragePath");
                file = (File) method.invoke(environmentClazz);
            } else {
                method = environmentExClazz.getMethod("getExternalStoragePath");
                file = (File) method.invoke(environmentExClazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "getExternalStoragePath error:" + e.getMessage());
        }

        return file;
    }

    public static File getSecondaryStorageDirectory() {
        Method method;
        File file = null;
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getSecondaryStorageDirectory");
                file = (File) method.invoke(environmentClazz);
            } else {
                method = environmentExClazz.getMethod("getSecondaryStorageDirectory");
                file = (File) method.invoke(environmentExClazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSecondaryStorageDirectory error:" + e.getMessage());
        }

        return file;
    }

    public synchronized static String getInternalStoragePathState() {
        long current = System.currentTimeMillis();
        if ((current - mLastInternalStateGetTime) < 50) {
            return mInternalState;
        }
        mLastInternalStateGetTime = current;

        Method method;
        String state = "";
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getInternalStoragePathState");
                state = (String) method.invoke(environmentClazz);
            } else {
                method = environmentExClazz.getMethod("getInternalStoragePathState");
                state = (String) method.invoke(environmentExClazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "getInternalStoragePathState error:" + e.getMessage());
        }
        mInternalState = state;
        return state;
    }

    public synchronized static String getExternalStoragePathState() {
        long current = System.currentTimeMillis();
        if ((current - mLastExternalStateGetTime) < 50) {
            return mExternalState;
        }
        mLastExternalStateGetTime = current;

        Method method;
        String state = "";
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getExternalStoragePathState");
                state = (String) method.invoke(environmentClazz);
            } else {
                // SPRD: Fix bug 979095 get external storge state.
                method = environmentExClazz.getMethod("getExternalStoragePath");
                File volumePath = (File) method.invoke(environmentExClazz);
                state = Environment.getExternalStorageState(volumePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "getExternalStoragePathState error:" + e.getMessage());
        }

        mExternalState = state;
        return state;
    }

    public static String getUsbdiskVolumeState(File path) {
        Method method;
        String state = "";
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getUsbdiskVolumeState", File.class);
                state = (String) method.invoke(environmentClazz, path);
            } else {
                method = environmentExClazz.getMethod("getUsbdiskVolumeState", File.class);
                state = (String) method.invoke(environmentExClazz, path);
            }
        } catch (Exception e) {
            try {
                if (OLD_SDK_VERSION) {
                    method = environmentClazz.getMethod("getExternalStorageState", File.class);
                    state = (String) method.invoke(environmentClazz, path);
                } else {
                    Log.e(TAG, "getUsbdiskVolumeState error:" + e.getMessage());
                }
            } catch (Exception e2) {
                Log.e(TAG, "getUsbdiskVolumeState error:" + e2.getMessage());
            }
        }

        return state;
    }
    
    public static List<VolumeInfo> getUsbdiskVolumes() {
        Method method;
        List<VolumeInfo> info = null;
        try {
            if (OLD_SDK_VERSION) {
                method = environmentClazz.getMethod("getUsbdiskVolumes");
                info = (List<VolumeInfo>) method.invoke(environmentClazz);
            } else {
                method = environmentExClazz.getMethod("getUsbdiskVolumes");
                info = (List<VolumeInfo>) method.invoke(environmentExClazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUsbdiskVolumes error:" + e.getMessage());
            try {
                if (OLD_SDK_VERSION) {
                    List<VolumeInfo> vols;
                    final ArrayList<VolumeInfo> res = new ArrayList<>();

                    final IStorageManager mountService = IStorageManager.Stub.asInterface(android.os.ServiceManager.getService("mount"));

                    try {
                        vols = Arrays.asList(mountService.getVolumes(0));
                    } catch (android.os.RemoteException e3) {
                        throw e3.rethrowAsRuntimeException();
                    }

                    for (VolumeInfo vol : vols) {
                        if (vol.disk != null && vol.disk.isUsb()) {
                            res.add(vol);
                        }
                    }
                    return res;
                }
            } catch (Exception e2) {
                // TODO: handle exception
                Log.e(TAG, "getUsbdiskVolumes error:" + e2.getMessage());
            }
        }

        return info;
    }
}
