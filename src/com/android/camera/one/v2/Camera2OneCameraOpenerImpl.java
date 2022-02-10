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

package com.android.camera.one.v2;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;

import com.android.camera.FatalErrorHandler;
import com.android.camera.SoundPlayer;
import com.android.camera.async.MainThread;
import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.android.camera.device.ActiveCameraDeviceTracker;
import com.android.camera.device.CameraId;
import com.android.camera.one.OneCameraOpener;
import com.android.camera.util.AndroidServices;
import com.android.camera.util.ApiHelper;
import com.google.common.base.Optional;

/**
 * The {@link com.android.camera.one.OneCameraOpener} implementation on top of Camera2 API.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2OneCameraOpenerImpl implements OneCameraOpener {
    private static final Tag TAG = new Tag("OneCamera1Opnr");

    private final Context mContext;
    private final CameraManager mCameraManager;
    private final DisplayMetrics mDisplayMetrics;

    public static Optional<OneCameraOpener> create(
            Context context,
            DisplayMetrics displayMetrics) {
        if (!ApiHelper.HAS_CAMERA_2_API) {
            return Optional.absent();
        }
        CameraManager cameraManager;
        try {
            cameraManager = AndroidServices.instance().provideCameraManager();
        } catch (IllegalStateException ex) {
            Log.e(TAG, "camera2.CameraManager is not available.");
            return Optional.absent();
        }
        OneCameraOpener oneCameraOpener = new Camera2OneCameraOpenerImpl(
                context,
                cameraManager,
                displayMetrics);
        return Optional.of(oneCameraOpener);
    }

    /**
     * Instantiates a new {@link com.android.camera.one.OneCameraOpener} for Camera2 API.
     *
     * @param cameraManager the underlying Camera2 camera manager.
     */
    public Camera2OneCameraOpenerImpl(
            Context context,
            CameraManager cameraManager,
            DisplayMetrics displayMetrics) {
        mContext = context;
        mCameraManager = cameraManager;
        mDisplayMetrics = displayMetrics;
    }

}
