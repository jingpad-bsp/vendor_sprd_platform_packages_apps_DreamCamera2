/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.android.camera.ShutterButton.OnShutterButtonListener;
import com.dream.camera.MakeupController.MakeupListener;

public interface PhotoController extends OnShutterButtonListener,MakeupListener {

    public static final int PREVIEW_STOPPED = 0;
    public static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    public static final int FOCUSING = 2;
    public static final int SNAPSHOT_IN_PROGRESS = 3;
    // Switching between cameras.
    public static final int SWITCHING_CAMERA = 4;

    public void onZoomChanged(float requestedZoom);

    public boolean isImageCaptureIntent();

    public boolean isCameraIdle();

    public void onCaptureDone();

    public void onCaptureCancelled();

    public void onCaptureRetake();

    public void cancelAutoFocus();

    public void stopPreview();

    public int getCameraState();

    public void onSingleTapUp(View view, int x, int y);

    public void onLongPress(MotionEvent var1);

    public void updatePreviewAspectRatio(float aspectRatio);

    public void updateCameraOrientation();

    /**
     * This is the callback when the UI or buffer holder for camera preview,
     * such as {@link android.graphics.SurfaceTexture}, is ready to be used.
     * The controller can start the camera preview after or in this callback.
     */
    public void onPreviewUIReady();


    /**
     * This is the callback when the UI or buffer holder for camera preview,
     * such as {@link android.graphics.SurfaceTexture}, is being destroyed.
     * The controller should try to stop the preview in this callback.
     */
    public void onPreviewUIDestroyed();

    /********************** Capture animation **********************/

    /**
     * Starts the pre-capture animation.
     */
    public void startPreCaptureAnimation();

    public boolean isShutterEnabled();//SPRD:Add for ai detect

    public void setCaptureCount(int count);// SPRD:Add for ai detect
    public void onShowImageDone();//SPRD:add for imagecover done to reset capture button
    public boolean isSupportTouchAFAE();
    public boolean isSupportManualMetering();
    public void setSmileCapture(boolean smile);
}
