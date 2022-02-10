
package com.dream.camera.modules.portraitphoto;

import android.view.MotionEvent;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.app.MediaSaver;
import com.android.camera.data.FilmstripItemData;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.PhotoModule;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.DreamModule;
import com.dream.camera.BlurRefocusController.BlurRefocusFNumberListener;

import com.android.camera.app.OrientationManager;

import android.location.Location;


public class PortraitPhotoModule extends PhotoModule implements BlurRefocusFNumberListener {
    private static final Log.Tag TAG = new Log.Tag("PortraitPhotoModule");

    public PortraitPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        mFirstPortrait = true;
        showBlurRefocusHind();
        return new PortraitPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    public boolean isSupportTouchAFAE() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSupportManualMetering() {
        // TODO Auto-generated method stub
        return false;
    }



    public void showBlurRefocusHind() {
        boolean shouldShowBlurRefocusHind = mDataModule.getBoolean(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT);
        if (shouldShowBlurRefocusHind == true) {
            mDataModule.set(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT, false);
        }
    }

    @Override
    public void onLongPress(MotionEvent var1){}

    @Override
    protected void requestCameraOpen() {

        mCameraId = DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(mCameraId)?CameraUtil.FRONT_PORTRAIT_ID
                :CameraUtil.BACK_PORTRAIT_ID;
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());

    }

    @Override
    protected void startPreview(boolean optimize) {
        //SPRD:fix Bug746853
        if (mPaused || mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            // do nothing
            return;
        }
        int deviceOrientation = mAppController.getOrientationManager()
                .getDeviceOrientation().getDegrees();
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setDeviceOrientation(deviceOrientation);
            mCameraDevice.applySettings(mCameraSettings);
        }
        Log.i(TAG, "startPreview deviceOrientation:" + deviceOrientation);

        if(mCameraSettings != null && mCameraDevice != null) {
            mCameraSettings.setFNumberValue(mFNumber);
            Log.i(TAG, "startPreview F_Number:" + mFNumber);
        }

        super.startPreview(optimize);
    }

    @Override
    protected void updateLPortraitRefocus() {

        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
            return;
        }

        if(mCameraSettings != null && mCameraDevice != null) {
            int refocusEnable = mDataModuleCurrent.getBoolean(Keys.KEY_PORTRAIT_REFOCUS_KEY) ? 1 : 0;
            if (CameraUtil.isPortraitRefocusEnable() && !isCameraFrontFacing()) {
                refocusEnable = mDataModuleCurrent.getBoolean(Keys.KEY_PORTIAIT_REFORCUS_CLOSE) ? 1 : 0;
            }
            mCameraSettings.setRefocusEnable(refocusEnable);
            Log.i(TAG, "startPreview updateLPortraitRefocus:" + refocusEnable);
        }
    }

    @Override
    public void onBlurClosed(boolean enable) {
        mDataModuleCurrent.changeSettings(Keys.KEY_PORTIAIT_REFORCUS_CLOSE,enable);
        if(mCameraSettings != null && mCameraDevice != null) {
            Log.i(TAG, "onBlurClosed: " + enable);
            mCameraSettings.setRefocusEnable(enable ? 1 : 0);
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    @Override
    public void onOrientationChanged(OrientationManager orientationManager, OrientationManager.DeviceOrientation deviceOrientation) {
        int deviceRotation = deviceOrientation.getDegrees();
        Log.i(TAG, "back blur onOrientationChanged  deviceOrientation = " + deviceRotation);
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setDeviceOrientation(deviceRotation);
            mCameraDevice.applySettings(mCameraSettings);
        }
        super.onOrientationChanged(orientationManager, deviceOrientation);
    }

    /*@Override*/
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height,
                         int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
        Log.i(TAG, "addImage TAG_CAMERATYPE_IFD val="+val);
        if (val == null || val == 0) {
            mIsBlurRefocusPhoto = true;
        }
        getServices().getMediaSaver().addImage(
                data, title, date, loc, width, height,
                orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG, null);
    }
    /* @} */

    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }

    public boolean useNewApi() {
        return true;
    }

    private int mFNumber = CameraUtil.getPortraitDefaultFNumber();
    public void onFNumberValueChanged(int value) {
        Log.i(TAG, " onFNumberValueChanged = " + value);
        mFNumber = value;
        if(mCameraSettings != null && mCameraDevice != null) {
            mCameraSettings.setFNumberValue(mFNumber);
            if (!mFirstHasStartCapture){
                mCameraDevice.applySettings(mCameraSettings);
            }
        }
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        if (CameraUtil.isPortraitRefocusEnable() && !isCameraFrontFacing()) {
            if (mCameraState == FOCUSING) {
                return;
            }
            if (mCameraSettings != null) {
                mCameraSettings.setFNumberValue(mFNumber);
                super.onSingleTapUp(view, x, y);
            }
        }
    }

    @Override
    public void pause() {
        mFirstPortrait = false;
        if(mCameraDevice != null && mNotModeListViewClick && mDataModule != null){
            mNotModeListViewClick = false;
            mDataModule.set(Keys.KEY_STARTUP_MODULE_INDEX, SettingsScopeNamespaces.AUTO_PHOTO);
            if(mDataModule.getInt(Keys.KEY_CAMERA_ID) != DreamUtil.getRightCamera(mCameraId)){
                mDataModule.set(Keys.KEY_SWITCH_FRONTCAMERA_TO_BACK_AUTOMODE, true);
            }
            mDataModule.set(Keys.KEY_SWITCH_PHOTO_TO_VIDEO, false);
            mCameraDevice.setAiSceneCallback(null, null);
            mCameraDevice.setAiSceneWork(false);
        }

        super.pause();
        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
        mAppController.getCameraAppUI().setBlurEffectTipVisibity(View.GONE);
    }


    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isBlurNeedThumbCallback() &&
                DreamUtil.BACK_CAMERA == DreamUtil.getRightCamera(mCameraId)){
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
        } else {
            Log.i(TAG, "setNeedThumbCallBack false");
            mCameraSettings.setNeedThumbCallBack(false);
        }
    }

    @Override
    public int getModuleTpye() {
        int s = DreamUtil.BACK_CAMERA == DreamUtil.getRightCamera(mCameraId)
                ? DreamModule.REFOCUS_MODULE:DreamModule.FRONT_REFOCUS_MODULE;

        Log.i(TAG, "getModuleTpye = " + s);

        return s;
    }

    @Override
    public void onAiScene(int aiSceneType) {
        Log.i(TAG, "Portrait aiSceneType = " + aiSceneType);
        if(mPaused){
            return;
        }

        mAiSceneTypeForPortrait = aiSceneType;
        if(aiSceneType == 2){
            mActivity.getCameraAppUI().updateAiSceneView(View.VISIBLE , aiSceneType);
        } else {
            mActivity.getCameraAppUI().updateAiSceneView(View.INVISIBLE , aiSceneType);
        }

        if(mFirstPortrait){
            mPortraitToAutoNumber = CameraUtil.getPortraitToAutoDefaultNumber();
        }
        if(mCameraState != SNAPSHOT_IN_PROGRESS){
            remainingPortraitAnd3DNRDefaultNumber(mAiSceneTypeForPortrait);
        }
    }

    protected void updateParametersAiSceneDetect(){
        Log.i(TAG, "mNotModeListViewClick = " + mNotModeListViewClick);
        if(mNotModeListViewClick){
            Log.i(TAG, "updateParametersAiSceneDetect : 1");
            mDataModule.set(Keys.KEY_SWITCH_PHOTO_TO_VIDEO, true);
            mCameraDevice.setAiSceneCallback(mHandler, this);
            mCameraDevice.setAiSceneWork(true);
            mCameraSettings.setCurrentAiSenceEnable(1);
            sendDeviceOrientation();
        }
    }

    private int mPortraitToAutoNumber;
    private int mAiSceneTypeForPortrait;
    private boolean mFirstPortrait = false;
    private int mCountPortraitInfo;

    private void remainingPortraitAnd3DNRDefaultNumber(int aiSceneType) {
        if(mCountPortraitInfo != mPortraitToAutoNumber){
            if(aiSceneType == 2){
                if(mCountPortraitInfo > 0){
                    --mCountPortraitInfo;
                }
            } else {
                ++mCountPortraitInfo;
            }
        } else {
            switchTo3DNROrPortrait(SettingsScopeNamespaces.AUTO_PHOTO);
        }
        mFirstPortrait = false;
    }

}
