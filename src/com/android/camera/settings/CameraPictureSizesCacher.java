/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;

import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import android.graphics.ImageFormat;
import android.util.Log;
import com.android.camera.device.CameraId;
import com.android.camera.one.OneCameraCharacteristics;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.OneCameraModule;
import com.dream.camera.settings.DataModuleManager;

import com.google.common.base.Optional;

import java.util.List;

/**
 * Facilitates caching of camera supported picture sizes, which is slow to
 * query. Will update cache if Build ID changes.
 */
public class CameraPictureSizesCacher {
    private static final String PICTURE_SIZES_BUILD_KEY = "CachedSupportedPictureSizes_Build_Camera";
    private static final String PICTURE_SIZES_SIZES_KEY = "CachedSupportedPictureSizes_Sizes_Camera";

    private static final String TAG = "CameraPictureSizesCacher";

    private static List<Size> delelteSize(List<Size> list)
    {
        int length = list.size();

        for (int i = 0; i < length; i++) {
            Size size = list.get(i);

            Log.d(TAG, "skyy cache delelteSize, size.height() = " + size.height() + "; size.width() =  " + size.width());

            if ((float) size.width() / (float) size.height() > 1.7f)
            {
                list.remove(i);
                i--;
                length--;
            }
        }

        return list;
    }

    /**
     * Opportunistically update the picture sizes cache, if needed.
     * 
     * @param cameraId
     *            cameraID we have sizes for.
     * @param sizes
     *            List of valid sizes.
     */
    public static void updateSizesForCamera(Context context, int cameraId,
            List<Size> sizes) {
        String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
        /*
        SharedPreferences defaultPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        */
        String thisCameraCachedBuild = DataModuleManager.getInstance(context).getDataModuleCamera()
                .getString(key_build);
        // Write to cache.
        if (thisCameraCachedBuild == null) {
            String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;
            /*
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
            */
            DataModuleManager.getInstance(context).getDataModuleCamera().set(key_build, Build.DISPLAY);
            DataModuleManager.getInstance(context).getDataModuleCamera().set(key_sizes, Size.listToString(sizes));
        }
    }

    /**
     * Return list of Sizes for provided cameraId. Check first to see if we have
     * it in the cache for the current android.os.Build. Note: This method calls
     * Camera.open(), so the camera must be closed before calling or null will
     * be returned if sizes were not previously cached.
     * 
     * @param cameraId
     *            cameraID we would like sizes for.
     * @param context
     *            valid android application context.
     * @return List of valid sizes, or null if the Camera can not be opened.
     */
    public static List<Size> getSizesForCamera(int cameraId, Context context) {
        Optional<List<Size>> cachedSizes = getCachedSizesForCamera(cameraId,
                context);
        Log.e(TAG, "skyy getSizesForCamera 11  start" );
        if (cachedSizes.isPresent()) {
            return cachedSizes.get();
        }
        Log.e(TAG, "skyy getSizesForCamera 12  start" );
        /**
         * SPRD: Fix bug 592976 that optimize the first boot time @{
         * Original Code
         *
        // No cached value, so need to query Camera API.
        Camera thisCamera;
        try {
            thisCamera = Camera.open(cameraId);
        } catch (RuntimeException e) {
            // Camera open will fail if already open.
            return null;
        }
        if (thisCamera != null) {
            String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
            String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;
            SharedPreferences defaultPrefs = PreferenceManager
                    .getDefaultSharedPreferences(context);

            List<Size> sizes = Size.buildListFromCameraSizes(thisCamera
                    .getParameters().getSupportedPictureSizes());
            thisCamera.release();
            SharedPreferences.Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
            return sizes;
        }
        */
        try {
            String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
            String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;

            CameraId cameraIdIdentifier = CameraId.fromLegacyId(cameraId);
            OneCameraManager oneCameraManager = OneCameraModule.provideOneCameraManager();
            OneCameraCharacteristics cameraCharacteristicsFront =
                    oneCameraManager.getOneCameraCharacteristics(cameraIdIdentifier);
            List<Size> sizes = cameraCharacteristicsFront.getSupportedPictureSizes(ImageFormat.JPEG);
            if((cameraId == CameraUtil.getBackBlurCameraId() || cameraId == CameraUtil.getFrontBlurCameraId()
                    || cameraId == CameraUtil.BACK_PORTRAIT_ID || cameraId == CameraUtil.FRONT_PORTRAIT_ID)) {
                /* SPRD:fix bug1171640 remove preview and thumb size @{ */
                if (sizes.size() > 2) {
                    sizes.remove(sizes.size() - 1);
                    sizes.remove(sizes.size() - 1);
                } else if (sizes.size() > 1) {
                    sizes.remove(sizes.size() - 1);
                }
                /* @} */
            }
            DataModuleManager.getInstance(context).getDataModuleCamera().set(key_build, Build.DISPLAY);
            DataModuleManager.getInstance(context).getDataModuleCamera().set(key_sizes, Size.listToString(sizes));

            delelteSize(sizes);

            return sizes;
        } catch (Exception e) {
            Log.w(TAG, "getSizesForCamera error:" + e.getMessage());
        }
        return null;
    }

    public static List<Size> getSizesForCamera(int cameraId, Context context,
            CameraProxy thisCamera) {
        Optional<List<Size>> cachedSizes = getCachedSizesForCamera(cameraId,
                context);
        Log.e(TAG, "skyy getSizesForCamera 21  start" );
        if (cachedSizes.isPresent()) {
            return cachedSizes.get();
        }
        Log.e(TAG, "skyy getSizesForCamera 22  start" );
        if (thisCamera != null) {
            try {
                String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
                String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;

                CameraId cameraIdIdentifier = CameraId.fromLegacyId(cameraId);
                OneCameraManager oneCameraManager = OneCameraModule.provideOneCameraManager();
                OneCameraCharacteristics cameraCharacteristicsFront =
                        oneCameraManager.getOneCameraCharacteristics(cameraIdIdentifier);
                List<Size> sizes = cameraCharacteristicsFront.getSupportedPictureSizes(ImageFormat.JPEG);

                DataModuleManager.getInstance(context).getDataModuleCamera().set(key_build, Build.DISPLAY);
                DataModuleManager.getInstance(context).getDataModuleCamera().set(key_sizes, Size.listToString(sizes));

                delelteSize(sizes);

                return sizes;
            } catch (Exception e) {
                Log.w(TAG, "getSizesForCamera error:" + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Returns the cached sizes for the current camera. See
     * {@link #getSizesForCamera} for details.
     * 
     * @param cameraId
     *            cameraID we would like sizes for.
     * @param context
     *            valid android application context.
     * @return Optional ist of valid sizes. Not present if the sizes for the
     *         given camera were not cached.
     */
    public static Optional<List<Size>> getCachedSizesForCamera(int cameraId,
            Context context) {
        String key_build = PICTURE_SIZES_BUILD_KEY + cameraId;
        String key_sizes = PICTURE_SIZES_SIZES_KEY + cameraId;
        // Return cached value for cameraId and current build, if available.
        String thisCameraCachedBuild = DataModuleManager.getInstance(context).getDataModuleCamera()
                .getString(key_build);
        if (thisCameraCachedBuild != null
                && thisCameraCachedBuild.equals(Build.DISPLAY)) {
            String thisCameraCachedSizeList = DataModuleManager.getInstance(context)
                    .getDataModuleCamera().getString(key_sizes);
            if (thisCameraCachedSizeList != null) {
                return Optional.of(Size.stringToList(thisCameraCachedSizeList));
            }
        }
        return Optional.absent();
    }

//    public static String getSlowMotionForCamera(Context context) {
//        Camera thisCamera;
//        try {
//            thisCamera = Camera.open(0);
//        } catch (RuntimeException e) {
//            // Camera open will fail if already open.
//            return null;
//        }
//        String slowMotion = null;
//        if (thisCamera != null) {
//            List<String> listSlowMotion = thisCamera.getParameters()
//                    .getSupportedSlowmotion();
//            thisCamera.release();
//            if (listSlowMotion != null) {
//                slowMotion = listSlowMotion.get(0);
//                for (int i = 1; i < listSlowMotion.size(); i++) {
//                    slowMotion = slowMotion + "," + listSlowMotion.get(i);
//                }
//            }
//            saveSlowMotion(context, slowMotion);
//        }
//        return slowMotion;
//    }

    public static String getSlowMotionForCamera(Context context,
            CameraProxy thisCamera) {
        String slowMotion = null;
        if (thisCamera != null) {
            List<String> listSlowMotion = thisCamera.getCapabilities()
                    .getSupportedSlowMotion();
            if (listSlowMotion != null && listSlowMotion.size() > 0) {
                slowMotion = listSlowMotion.get(0);
                for (int i = 1; i < listSlowMotion.size(); i++) {
                    slowMotion = slowMotion + "," + listSlowMotion.get(i);
                }
            }
            saveSlowMotion(context, slowMotion);
        }
        return slowMotion;
    }

    public static void saveSlowMotion(Context context, String slowMotion) {
        DataModuleManager.getInstance(context).getDataModuleCamera().set(Keys.KEY_VIDEO_SLOW_MOTION_ALL, slowMotion);
    }

    public static String getCacheSlowMotionForCamera(Context context) {
        String slowMotionValues = DataModuleManager.getInstance(context).getDataModuleCamera()
                .getString(Keys.KEY_VIDEO_SLOW_MOTION_ALL);
        return slowMotionValues;
    }
}
