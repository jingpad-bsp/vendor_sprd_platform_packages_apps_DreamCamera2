
package com.dream.camera.modules.autophoto;

import com.android.camera.app.AppController;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera2.R;

import com.android.camera.PhotoModule;

import android.location.Location;
import android.net.Uri;
import android.view.Surface;
import android.view.View;
import android.view.ViewStub;

import android.media.MediaActionSound;

import com.android.ex.camera2.portability.Size;
import com.dream.camera.ButtonManagerDream;

import com.dream.camera.settings.DataModuleManager;
import com.sprd.camera.encoder.MotionPhotoSaver;
import com.sprd.camera.encoder.RecorderWrapper;
import com.sprd.camera.encoder.Util;
import com.sprd.camera.encoder.interfaces.Recorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class AutoPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("AutoPhotoModule");

    public AutoPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        return new AutoPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }
    /* SPRD: optimize camera launch time @{ */
    public boolean useNewApi() {
        // judge VGesture enable state first will be better, like:
        // if (isShouldShowVGesture()) return false;
        // but isShouldShowVGesture will throw exception if useNewApi() is
        // called before module initialized, and no negative effect is found
        // until now, so just ignore this judgement temporarily.
        if (isUseSurfaceView()) {
            return true;
        }
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(null);
    }
    /* @} */

    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isNormalNeedThumbCallback()) {
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
            mCameraSettings.setThumbCallBack(1);
        } else {
            super.updateParametersThumbCallBack();
        }
    }
    // SPRD:Fix bug 948896 add for ai scene detect enable
    protected void updateParametersAiSceneDetect(){

        if(!hasAiSceneSupported() || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_AI_SCENE_DATECT))
            return;
        if(!isCameraFrontFacing()){
            if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1 ){

                mCameraSettings.setCurrentAiSenceEnable(1);
                Log.i(TAG, "updateParametersAiSceneDetect : 1");

                // Bug 1014851 - send Device Orientation to HAL for AI Scene Detection
                sendDeviceOrientation();
            }else{
                mCameraSettings.setCurrentAiSenceEnable(0);
                Log.i(TAG, "updateParametersAiSceneDetect : 0");
            }
        } else if(isCameraFrontFacing()){

            if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1 ){

                mCameraSettings.setCurrentAiSenceEnable(2);
                Log.i(TAG, "updateParametersAiSceneDetect : 2");

                // Bug 1014851 - send Device Orientation to HAL for AI Scene Detection
                sendDeviceOrientation();
            }else{
                mCameraSettings.setCurrentAiSenceEnable(0);
                Log.i(TAG, "updateParametersAiSceneDetect : 0");
            }
        }
    }

    // Bug 1018708 - hide or show BeautyButton on topPanel
    protected void updateBeautyButton() {
        boolean on = (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1);
        mUI.showBeautyButton(!on);
        if(!on){
            updateMakeLevel();
        }

    }
    @Override
    protected void updateFilterTopButton() {
        if (CameraUtil.isUseSprdFilter()) {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FILTER_DREAM,View.VISIBLE);
        } else {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FILTER_DREAM,View.GONE);
        }
    }
    protected void updateMakeUpDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY)) {
            return;
        }
        boolean on = (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1);
        updateMakeUpDisplayStatus(mDataModuleCurrent.getBoolean(Keys.KEY_MAKE_UP_DISPLAY) && !on);
    }

    @Override
    protected void updateFlashTopButton() {
        if(CameraUtil.isWPlusTEnable()){
            if(!CameraUtil.isWPlusTAbility(mActivity.getApplicationContext(),true)){
                ((ButtonManagerDream)mActivity.getButtonManager()).enableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
                mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FLASH_DREAM,View.VISIBLE);
                return;
            }
            ((ButtonManagerDream)mActivity.getButtonManager()).disableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
        }else{
            super.updateFlashTopButton();
        }
    }

    // *********************** recorder wrapper ***********************
    RecorderWrapper mRecorderWrapper;
    MotionPhotoSaver mMotionPhotoSaver;

    Recorder.RecorderCallBack mRecorderCallBack = new Recorder.RecorderCallBack() {
        @Override
        public void onRecordEnd(long recordTimeUS, String filePath) {
            if(mPaused){
                Log.e(TAG," onRecordEnd mPaused  = true");
                mMotionPhotoSaver.deleateData(filePath);
                return;
            }
            Log.e(TAG," onRecordEnd timeus = " + Util.getPTSUs());
            mMotionPhotoSaver.setRecordData(recordTimeUS, filePath);
            startAnimation();
            if (mAppController.isPlaySoundEnable()) {
                if (mCameraSound != null) {
                    mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                }
            }

        }
    };

    AtomicInteger refreshThumbnail = new AtomicInteger(0);
    private void startAnimation(){
        if(refreshThumbnail.incrementAndGet() == 2 ){
            Log.e(TAG," mMotionPhotoSaver getJpegData != null");
            startPeekAnimation(mMotionPhotoSaver.getJpegData());
            refreshThumbnail.set(0);
            changeCameraState();
        }
    }


    public void startPeekAnimation(final byte[] jpegData) {
        if(isMotionPhotoOn() && refreshThumbnail.get() != 2){
            return;
        }else {
            super.startPeekAnimation(jpegData);
        }
    }


    @Override
    protected void initData(byte[] jpegData, String title, long date,
                            int width, int height, int orientation, ExifInterface exif,
                            Location location,
                            MediaSaver.OnMediaSavedListener onMediaSavedListener) {
        Log.e(TAG,"initData timeus = " + Util.getPTSUs());
        // receive the jpeg data
        title+="_MP";
        mMotionPhotoSaver.initData(jpegData, title, date, width, height,
                orientation, exif, location, onMediaSavedListener,
                getServices().getMediaSaver());
        startAnimation();
    }

    @Override
    public void onPreviewStartedAfter() {
        if(mMotionPhotoSaver != null && mMotionPhotoSaver.isSaving()){
            Log.e(TAG," mMotionPhotoSaver.isSaving() = true");
            mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(false);
            mAppController.getCameraAppUI().setSwipeEnabled(false);
            setCameraState(SNAPSHOT_IN_PROGRESS);
        }
    }

    @Override
    protected void mediaSaved(Uri uri) {
        mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
        mAppController.getCameraAppUI().setSwipeEnabled(true);
        Log.e(TAG,"mediaSaved timeus = " + Util.getPTSUs());
    }

    private void initMotionPhotoRecorder(int width, int height) {
        Log.d(TAG,"initMotionPhotoRecorder width = " + width + " height = " + height);
        mRecorderWrapper = new RecorderWrapper(mActivity.getAndroidContext(), width,height);
        mRecorderWrapper.setCallBack(mRecorderCallBack);
        mRecorderWrapper.config();
    }

    private void startMotionRecord() {
        Log.d(TAG,"startMotionRecord");
        // start recording
        mRecorderWrapper.record(Util.getPTSUs());

        // animate motionphoto button
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.startAnimation(ButtonManagerDream.BUTTON_MONTIONPHOTO_DREAM);
    }

    boolean isMotionPhotoOn(){
        boolean isMotionPhotoOn = mDataModuleCurrent != null && mDataModuleCurrent.isEnableSettingConfig(Keys.PREF_KEY_MONTIONPHOTO) &&
                "on".equals (mDataModuleCurrent.getString(Keys.PREF_KEY_MONTIONPHOTO));
        Log.d(TAG, "isMontionPhotoOn = " + isMotionPhotoOn);
        return isMotionPhotoOn;
    }

    private void updateMotionPhoto() {
        if(!CameraUtil.isIsMotionPhotoEnabled()){
            return;
        }
        if(isMotionPhotoOn() && !mRecorderWrapper.isStarting()) {
            Log.d(TAG,"updateMotionPhoto start");
            mUI.showMotionPhotoTipText(true);
            mRecorderWrapper.start();
            mCameraDevice.setMontionPhotoRecordOn(true);
        }

        if(!isMotionPhotoOn() && mRecorderWrapper.isStarting()) {
            Log.d(TAG,"updateMotionPhoto stop");
            mUI.showMotionPhotoTipText(false);
            mRecorderWrapper.stop();
            mCameraDevice.setMontionPhotoRecordOn(false);
        }
    }

    @Override
    protected void updateParametersMirror() {
        super.updateParametersMirror();
        updateMotionMirror();

    }

    private void updateMotionMirror() {
        if(CameraUtil.isIsMotionPhotoEnabled()){
            if (isCameraFrontFacing()) {
                if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FRONT_CAMERA_MIRROR)){
                    return;
                }
                if(mRecorderWrapper != null){
                    mRecorderWrapper.setMirror(mDataModuleCurrent
                            .getBoolean(Keys.KEY_FRONT_CAMERA_MIRROR));
                }
            }
        }
    }

    @Override
    protected void doSomethingWhenonPictureTaken(byte[] jpeg) {
        super.doSomethingWhenonPictureTaken(jpeg);
        if(CameraUtil.isIsMotionPhotoEnabled() && isMotionPhotoOn()){
            mAppController.setShutterEnabled(false);
        }
    }

    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keyList) {
        for (Map.Entry<String, String> entry : keyList.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case Keys.PREF_KEY_MONTIONPHOTO:
                    Log.d(TAG,"onSettingChanged key = " + key + " value = "+ entry.getValue());
                    updateMotionPhoto();
                    break;
            }
        }
        super.onDreamSettingChangeListener(keyList);
    }

    @Override
    public void doCaptureSpecial() {
        super.doCaptureSpecial();
        if(CameraUtil.isIsMotionPhotoEnabled() && isMotionPhotoOn()){
            Log.d(TAG," doCaptureSpecial timeus = " + Util.getPTSUs());
            if(mRecorderWrapper != null){
                if(mRecorderWrapper.isRecording()){
                    return;
                }

                int sensorOrientation = mActivity.getCameraProvider()
                        .getCharacteristics(mCameraId).getSensorOrientation();
                int deviceOrientation = mAppController.getOrientationManager()
                        .getDeviceOrientation().getDegrees();
                int rotation = CameraUtil.getImageRotation(sensorOrientation,
                        deviceOrientation, isCameraFrontFacing());
                Log.d(TAG, " mDisplayOrientation = " + mDisplayOrientation +
                        " sensorOrientation = " + sensorOrientation +
                        " deviceOrientation = " +deviceOrientation +
                        " rotation = " + rotation);
                mRecorderWrapper.setOrientation(rotation);
                startMotionRecord();
            }
        }
    }

    @Override
    protected void dosetPreviewDisplayspecial() {
        if(CameraUtil.isIsMotionPhotoEnabled()){
            Log.d(TAG,"dosetPreviewDisplayspecial");
            if(mRecorderWrapper != null && (mRecorderWrapper.isRecording())){
                Log.e(TAG,"dosetPreviewDisplayspecial return");
                return;
            }
//        Size photosize = mCameraSettings.getCurrentPreviewSize();
            Size photosize = mCameraSettings.getCurrentPreviewSize();

            if(mRecorderWrapper == null || mRecorderWrapper.isRelease()){
                Log.e(TAG,"dosetPreviewDisplayspecial mRecorderWrapper == " + mRecorderWrapper + " mRecorderWrapper.isRelease()");
                initMotionPhotoRecorder(photosize.width(),photosize.height());
            }
            if(mRecorderWrapper != null){
                if (photosize.width() != mRecorderWrapper.getmWidth()
                        || photosize.height()!= mRecorderWrapper.getmHeight()){
                    Log.d(TAG,"dosetPreviewDisplayspecial width X heght not equal");
                    resetMontionPhotoRecorder(photosize.width(),photosize.height());
                }
            }
            Log.d(TAG,"dosetPreviewDisplayspecial setRecordSurfaces = " + mRecorderWrapper.getSurface() );
            mCameraDevice.setRecordSurfaces(mRecorderWrapper.getSurface());

            updateMotionPhoto();
            updateMotionMirror();

//        startMotionRecord();
        }

    }

    private void resetMontionPhotoRecorder(int width, int height) {
        Log.d(TAG,"resetMontionPhotoRecorder");
        mRecorderWrapper.resetSize(width,height);
    }

    @Override
    public void resume() {
        super.resume();
        refreshThumbnail.set(0);
        if(CameraUtil.isIsMotionPhotoEnabled() && mMotionPhotoSaver == null){
            mMotionPhotoSaver = new MotionPhotoSaver();
        }
    }

    @Override
    public void pause() {
        mPaused = true;
        if(CameraUtil.isIsMotionPhotoEnabled()){
            if(mRecorderWrapper != null && !mRecorderWrapper.isRelease() ){
                mRecorderWrapper.stop();
                mRecorderWrapper.release();
            }

            if(mMotionPhotoSaver != null && mMotionPhotoSaver.isSaving()){
                mMotionPhotoSaver.setSaving(false);
            }

            if(mCameraDevice != null){
                mCameraDevice.setRecordSurfaces(null);
            }
        }

        super.pause();
    }

    @Override
    public void destroy() {
        super.destroy();
        // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
        mAppController.getCameraAppUI().deinitUltraWideAngleSwitchView();
        if(CameraUtil.isIsMotionPhotoEnabled() && mMotionPhotoSaver != null){
            mMotionPhotoSaver.release();
        }

    }

    @Override
    protected boolean needInitData() {
        return CameraUtil.isIsMotionPhotoEnabled() && isMotionPhotoOn();
    }

    @Override
    protected boolean isNeedPlaySound() {
        if(CameraUtil.isIsMotionPhotoEnabled() && isMotionPhotoOn()){
            return false;
        }
        return super.isNeedPlaySound();
    }
}
