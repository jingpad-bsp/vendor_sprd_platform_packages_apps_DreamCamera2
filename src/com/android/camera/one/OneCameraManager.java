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

package com.android.camera.one;

import android.hardware.camera2.CameraCharacteristics;

import com.android.camera.device.CameraId;

/**
 * The camera manager is responsible for providing details about the
 * available camera hardware on the current device.
 */
public interface OneCameraManager {

    /**
     * Retrieve the characteristics for the camera facing at the given
     * direction. The first camera found in the given direction will be chosen.
     *
     * @return A #{link com.android.camera.one.OneCameraCharacteristics} object
     *         to provide camera characteristics information. Returns null if
     *         there is no camera facing the given direction.
     */
    public OneCameraCharacteristics getOneCameraCharacteristics(CameraId cameraId)
          throws OneCameraAccessException;

    public int[] getSprdFeatureCapabilities() throws OneCameraAccessException;

    public float[] getSprdZoomRatioSection(int cameraid) throws OneCameraAccessException;

    // SPRD: Bug922759 close some feature when special sensor
    public int getAvailableSensor(int cameraId) throws OneCameraAccessException;

    public CameraCharacteristics getCameraCharacteristics(int cameraId) throws OneCameraAccessException;

    public void getLogicalCameraId(int[] logical);
}