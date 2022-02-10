/*
 * Copyright (C) 2010 ZXing authors
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

package com.dream.camera.modules.qr;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.regex.Pattern;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.camera.util.Size;
import java.util.List;

import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.camera.util.CameraUtil;
import com.android.camera.app.AppController;
import com.dream.camera.settings.DataModuleManager;

final class CameraConfigurationManager {

    private static final String TAG = CameraConfigurationManager.class.getSimpleName();

    private static final int TEN_DESIRED_ZOOM = 27;
    private static final int DESIRED_SHARPNESS = 30;

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;
    protected int mCameraPreviewWidth;
    protected int mCameraPreviewHeight;
    int bestPreviewWidth;
    int bestPreviewHeight;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    /**
     * Sets the camera up to take preview images which are used for both preview and decoding. We
     * detect the preview format here so that buildLuminanceSource() can build an appropriate
     * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
     * and the planar Y can be used for barcode scanning without a copy in some cases.
     */
    void setDesiredCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Log.d(TAG, "Setting preview size: " + cameraResolution);
        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        setFlash(parameters);
        setZoom(parameters);

        setDisplayOrientation(camera, 90);
        camera.setParameters(parameters);
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    int getPreviewFormat() {
        return previewFormat;
    }

    String getPreviewFormatString() {
        return previewFormatString;
    }

    private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
        int tenBestValue = 0;
        for (String stringValue : COMMA_PATTERN.split(stringValues)) {
            stringValue = stringValue.trim();
            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException nfe) {
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }

    private void setFlash(Camera.Parameters parameters) {

       /* if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) {
            parameters.set("flash-value", 1);
        } else {
            parameters.set("flash-value", 2);
        }
        parameters.set("flash-mode", "off");*/
    }

    private void setZoom(Camera.Parameters parameters) {
        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString != null && !Boolean.parseBoolean(zoomSupportedString)) {
            return;
        }

        int tenDesiredZoom = TEN_DESIRED_ZOOM;

        String maxZoomString = parameters.get("max-zoom");
        try {
            int tenMaxZoom = (int) (10.0 * Double.parseDouble(maxZoomString));
            if (tenDesiredZoom > tenMaxZoom) {
                tenDesiredZoom = tenMaxZoom;
            }
        } catch (NullPointerException | NumberFormatException e) {
            Log.w(TAG, "Bad max-zoom: " + maxZoomString);
        }

        String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
        try {
            int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
            if (tenDesiredZoom > tenMaxZoom) {
                tenDesiredZoom = tenMaxZoom;
            }
        } catch (NumberFormatException nfe) {
            Log.w(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
        }

        String motZoomValuesString = parameters.get("mot-zoom-values");
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
        }

        String motZoomStepString = parameters.get("mot-zoom-step");
        try {
            double motZoomStep = Double.parseDouble(motZoomStepString.trim());
            int tenZoomStep = (int) (10.0 * motZoomStep);
            if (tenZoomStep > 1) {
                tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
            }
        } catch (NullPointerException | NumberFormatException e) {
            Log.e(TAG , "Bad motZoomStepString: " + motZoomStepString);
        }

        if (maxZoomString != null || motZoomValuesString != null) {
            parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
        }

        if (takingPictureZoomMaxString != null) {
            parameters.set("taking-picture-zoom", tenDesiredZoom);
        }
    }

    public static int getDesiredSharpness() {
        return DESIRED_SHARPNESS;
    }

    /**
     * compatible 1.6
     *
     * @param camera
     * @param angle
     */
    protected void setDisplayOrientation(Camera camera, int angle) {
        Method downPolymorphic;
        try
        {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] {
                    int.class
            });
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[] {
                        angle
                });
        } catch (Exception e1)
        {
        }
    }

    int getCameraPreviewWidth(){
        return bestPreviewWidth;
    }

    int getCameraPreviewHeight(){
        return bestPreviewHeight;
    }

    //SPRD: Fix bug 881427 add api2 support
    void initFromCameraParameters(CameraProxy mCameraDevice,CameraSettings mCameraSettings,
            CameraCapabilities mCameraCapabilities,AppController mAppController,int mCameraId ) {
        previewFormat = 17;
        previewFormatString = "yuv420sp";

        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenResolution = new Point(display.getWidth(), display.getHeight());

        Point screenResolutionForCamera = new Point();
        screenResolutionForCamera.x = screenResolution.x;
        screenResolutionForCamera.y = screenResolution.y;

        if (screenResolution.x < screenResolution.y) {
            screenResolutionForCamera.x = screenResolution.y;
            screenResolutionForCamera.y = screenResolution.x;
        }

        findBestWidthAndHeight(mCameraSettings, screenResolutionForCamera, mCameraDevice, mCameraCapabilities, mAppController,mCameraId);
    }

    public void findBestWidthAndHeight(CameraSettings mCameraSettings, Point screenResolution,
            CameraProxy mCameraDevice, CameraCapabilities mCameraCapabilities,AppController mAppController,int mCameraId) {

        List<Size> previewSizes = Size.convert(mCameraCapabilities
                .getSupportedPreviewSizes());

        int diffs = Integer.MAX_VALUE;
        for (int i = 0; i < previewSizes.size(); i++) {
            Size size = previewSizes.get(i);
            mCameraPreviewWidth = size.getWidth();
            mCameraPreviewHeight = size.getHeight();

            int newDiffs = Math.abs(mCameraPreviewWidth - screenResolution.x) + Math.abs(mCameraPreviewHeight - screenResolution.y);
            if(newDiffs == 0){
                bestPreviewWidth = mCameraPreviewWidth;
                bestPreviewHeight = mCameraPreviewHeight;
                break;
            }
            if(diffs > newDiffs){
                bestPreviewWidth = mCameraPreviewWidth;
                bestPreviewHeight = mCameraPreviewHeight;
                diffs = newDiffs;
            }
        }
        cameraResolution = new Point();
        cameraResolution.x = bestPreviewWidth;
        cameraResolution.y = bestPreviewHeight;
    }
}
