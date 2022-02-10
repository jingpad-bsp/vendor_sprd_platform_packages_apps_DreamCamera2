/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.util.Set;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build.VERSION_CODES;
import android.util.SparseArray;

import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.android.camera.device.CameraId;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraCharacteristics;
import com.android.camera.one.OneCameraManager;
import com.android.camera.util.AndroidServices;
import com.android.camera.util.ApiHelper;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

/**
 * Pick camera ids from a list of devices based on defined characteristics.
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
public class Camera2OneCameraManagerImpl implements OneCameraManager {
    private static final Tag TAG = new Tag("Camera2OneCamMgr");
    /**
     * Create a new camera2 api hardware manager.
     */
    public static Optional<Camera2OneCameraManagerImpl> create() {
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
        Camera2OneCameraManagerImpl hardwareManager =
              new Camera2OneCameraManagerImpl(cameraManager);
        return Optional.of(hardwareManager);
    }

    private final CameraManager mCameraManager;

    public Camera2OneCameraManagerImpl(CameraManager cameraManger) {
        mCameraManager = cameraManger;
    }


    @Override
    public OneCameraCharacteristics getOneCameraCharacteristics(
          @Nonnull CameraId key)
          throws OneCameraAccessException {
        return new OneCameraCharacteristicsImpl(getCameraCharacteristics(key));
    }

    public CameraCharacteristics getCameraCharacteristics(
          @Nonnull CameraId key)
          throws OneCameraAccessException {
        try {
            return mCameraManager.getCameraCharacteristics(key.getValue());
        } catch (CameraAccessException ex) {
            throw new OneCameraAccessException("Unable to get camera characteristics", ex);
        }
    }

    /*SPRD: bug 890774 change prop to tag
     * @{
     */
    @Override
    public int[] getSprdFeatureCapabilities() throws OneCameraAccessException {
        int cameraId = 0;
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            if (cameraIds != null && cameraIds.length >= 1) {
                cameraId = Integer.parseInt(cameraIds[0]);
            }
        } catch (CameraAccessException ex) {
            Log.w(TAG, "Unable to getCameraIdList", ex);
        }
        int[] mSprdFeatureCapabilities = getCameraCharacteristics(cameraId).get(
              CameraCharacteristics.ANDROID_SPRD_FEATURE_LIST);
        if (mSprdFeatureCapabilities == null || mSprdFeatureCapabilities.length == 0) {
            return null;
        }
        return mSprdFeatureCapabilities;
    }
    /*@} */



    @Override
    public float[] getSprdZoomRatioSection(int cameraid) throws OneCameraAccessException {
        float[] mSprdZoomRatioSection = getCameraCharacteristics(cameraid).get(
                CameraCharacteristics.ANDROID_SPRD_ZOOM_RATIO_SECTION);
        if (mSprdZoomRatioSection == null || mSprdZoomRatioSection.length == 0) {
            return null;
        }
        return mSprdZoomRatioSection;
    }

    @Override
    public void getLogicalCameraId(int[] logical) {
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            logical[0] = 0;
            logical[1] = 0;
            if (cameraIds.length <= 2) return ;
            for (String cameraId : cameraIds) {
                Log.i(TAG, "cameraId = " + cameraId);
                CameraCharacteristics characteristics = mCameraManager
                      .getCameraCharacteristics(cameraId);
                int[] caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                if (arrayContains(caps, CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)) {
                    Log.i(TAG, "cameraId = " + cameraId);
                    Set<String> physicalIds = characteristics.getPhysicalCameraIds();
                    for (String physicalId: physicalIds) {
                        Log.i(TAG, "physicalId = " + physicalId);
                    }
                    if (physicalIds.size() != 2) {
                        Log.w(TAG, "3 or more physical cameras are not yet supported");
                        continue;
                    }
                    String[] physicalIdsArray = physicalIds.toArray(new String[2]);
                    Log.i(TAG, "physicalIdsArray[0] = " + physicalIdsArray[0] + "physicalIdsArray[1] = " + physicalIdsArray[1]);
                    CameraCharacteristics pc =
                            mCameraManager.getCameraCharacteristics(physicalIdsArray[0]);
                    if (CameraCharacteristics.LENS_FACING_BACK == pc.get(CameraCharacteristics.LENS_FACING)) {
                        logical[0] = Integer.parseInt(cameraId);
                    }
                    Log.i(TAG, "LENS_FACING = " + pc.get(CameraCharacteristics.LENS_FACING));
                    if (CameraCharacteristics.LENS_FACING_FRONT == pc.get(CameraCharacteristics.LENS_FACING)) {
                        logical[1] = Integer.parseInt(cameraId);
                    }
                }
            }
        } catch (CameraAccessException ex) {
            Log.w(TAG, "Unable to get camera ID", ex);
        }
    }

    private static boolean arrayContains(int[] arr, int needle) {
        if (arr == null) {
            return false;
        }

        for (int elem : arr) {
            if (elem == needle) {
                return true;
            }
        }

        return false;
    }

    // SPRD: Bug922759 close some feature when special sensor
    private SparseArray<CameraCharacteristics> mCharacteristicsList = new SparseArray<CameraCharacteristics>();
    public CameraCharacteristics getCameraCharacteristics(int cameraId) throws OneCameraAccessException {

        try {
            CameraCharacteristics pc = mCharacteristicsList.get(cameraId);
            if (pc == null) {
                pc = mCameraManager.getCameraCharacteristics(Integer.toString(cameraId));
                mCharacteristicsList.put(cameraId,pc);
            }
            return pc;
        } catch (Exception ex) {
            throw new OneCameraAccessException("Unable to get camera characteristics", ex);
        }
    }


    @Override
    public int getAvailableSensor(int cameraId) throws OneCameraAccessException {
        int mAvailableSensor = 0;
        try{
            mAvailableSensor = getCameraCharacteristics(cameraId).get(CameraCharacteristics.ANDROID_SPRD_AVAILABLESENSORTYPE);
            Log.i(TAG,"getAvailableSensor cameraId="+cameraId+" mAvailableSensor="+mAvailableSensor);
        } catch (Exception e) {
            Log.i(TAG,"this tag is not avaliable in this product ");
        }
        return mAvailableSensor;
    }
}
