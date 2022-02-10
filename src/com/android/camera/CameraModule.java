/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.view.KeyEvent;
import android.view.View;

import com.android.camera.app.AppController;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.CameraServices;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.Keys;
import com.dream.camera.DreamModule;

import android.location.Location;
import android.net.Uri;
import com.android.camera.app.MediaSaver.OnMediaSavedListener;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;

import android.content.Intent;

public abstract class CameraModule extends DreamModule implements ModuleController {
    /** Provides common services and functionality to the module. */
    private final CameraServices mServices;
    private final AppController mAppController;
    protected float mMinRatio = 1.0f;
    protected float mMaxRatio;
    protected float[] mZoomRatioSection;
    private static final Log.Tag TAG = new Log.Tag("CameraModule");
    public CameraModule(AppController app) {
        mServices = app.getServices();
        mAppController = app;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        // Do nothing.
    }

    @Deprecated
    public abstract boolean onKeyDown(int keyCode, KeyEvent event);

    @Deprecated
    public abstract boolean onKeyUp(int keyCode, KeyEvent event);

    @Deprecated
    public void onSingleTapUp(View view, int x, int y) {
    }

    /**
     * @return An instance containing common services to be used by the module.
     */
    protected CameraServices getServices() {
        return mServices;
    }

    /**
     * @return An instance used by the module to get the camera.
     */
    protected CameraProvider getCameraProvider() {
        return mAppController.getCameraProvider();
    }

    /**
     * Requests the back camera through {@link CameraProvider}.
     * This calls {@link
     * com.android.camera.app.CameraProvider#requestCamera(int)}. The camera
     * will be returned through {@link
     * #onCameraAvailable(com.android.ex.camera2.portability.CameraAgent.CameraProxy)}
     * when it's available. This is a no-op when there's no back camera
     * available.
     */
    protected void requestBackCamera() {
        int backCameraId = getCameraProvider().getFirstBackCameraId();
        if (backCameraId != -1) {
            getCameraProvider().requestCamera(backCameraId);
        }
    }
    public Boolean mIsFirstHasStartCapture(){return false;}
    public void onPreviewInitialDataReceived() {}

    /**
     * Releases the back camera through {@link CameraProvider}.
     * This calls {@link
     * com.android.camera.app.CameraProvider#releaseCamera(int)}.
     * This is a no-op when there's no back camera available.
     */
    protected void releaseBackCamera() {
        int backCameraId = getCameraProvider().getFirstBackCameraId();
        if (backCameraId != -1) {
            getCameraProvider().releaseCamera(backCameraId);
        }
    }

    /**
     * @return An accessibility String to be announced during the peek animation.
     */
    public abstract String getPeekAccessibilityString();

    @Override
    public void onShutterButtonLongPressed() {
        // noop
    }

    public int getBurstHasCaptureCount() {return 0;}//SPRD:fix bug 473462

    public void updateBatteryLevel(int level) {
        // noop
    }

    public boolean isPhoneCalling(Context context) {
        boolean isInCall = false;
        //SPRD:fix bug 1224587
        TelecomManager telecomManager = (TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);
        try{
            if (telecomManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
                isInCall = true;
            }
        } catch (NullPointerException e){
            Log.i(TAG,"phone is unable");
            return false;
        }
        return isInCall;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {};

    /* SPRD:Bug 535058 New feature: volume start @{ */
    protected int getVolumeControlStatus(CameraActivity mActivity) {
        return 0;
    }

    protected float increaseZoomValue(float mZoomValue) {
        mZoomValue = mZoomValue + 0.1f;
        if (mZoomValue >= mMaxRatio) {
            mZoomValue = mMaxRatio;
        } else if (mZoomValue <= mMinRatio) {
            mZoomValue = mMinRatio;
        }
        return mZoomValue;
    }

    protected float reduceZoomValue(float mZoomValue) {
        mZoomValue = mZoomValue - 0.1f;
        if (mZoomValue >= mMaxRatio) {
            mZoomValue = mMaxRatio;
        } else if (mZoomValue <= mMinRatio) {
            mZoomValue = mMinRatio;
        }
        return mZoomValue;
    }
    /* }@ New feature: volume end */

    /* Add for dream camera
    * @{
    */
    protected int getDeviceCameraId(){return 0;}

    public int getMode(){ return 0;}

    public boolean showExtendAlways() {
        return false;
    }

    public boolean showBottomAlways() {
        return false;
    }
    /* @} */

    public void updateParameter(String key) {}

    public boolean isShutterClicked(){
        return false ;
    }

    /* SPRD: optimize camera launch time @{ */
    public boolean useNewApi() {
        return false;
    }
    /* @} */

    public boolean isUseSurfaceView() {
        return false;
    }

    public boolean isRefreshThumbnail(){
        return true;
    }

  //SPRD:fix bug 613799 save the panorama exception
    public boolean isSavingPanorama(){
        return false;
    }

    public void freezeScreen(boolean needBlur, boolean needSwitch) {}

    public boolean isCameraAvailable(){
        return true;
    }
    public boolean isPhotoFocusing(){
        return false;
    }
    public void doSometingWhenFilmStripShow(){}

    /* SPRD: Fix bug 681215 optimization about that DV change to DC is slow @{ */
    public void stopPreview() { }
    /* @} */

    public boolean isTouchCapturing() {
        return false ;
    }
    public void waitInitDataSettingCounter (){}

    //Sprd:fix bug941988
    public boolean needLoadFilmstripItems(){
        return true;
    }
    protected class UpdateRunnbale implements Runnable {

        private final byte[] data;
        private final String title;
        private final long date;
        private final Location loc;
        private final int width, height;
        private final int orientation;
        private final String mimeType;
        private final ExifInterface exif;
        private final OnMediaSavedListener listener;
        private Uri uri;

        public UpdateRunnbale(byte[] data, String title, long date, Location loc,
                int width, int height, int orientation, String mimeType,
                ExifInterface exif, OnMediaSavedListener listener) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.mimeType = mimeType;
            this.exif = exif;
            this.listener = listener;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }

        public long dataLength() {
            return data.length;
        }

        @Override
        public void run() {
            Log.i(TAG, "UpdateRunnbale uri = " + uri);
            if (uri != null) {
                getServices().getMediaSaver().updateImage(uri,
                        data, title, date, loc, width, height, orientation, exif, listener, mimeType, null);
            }
        }
    }
}
