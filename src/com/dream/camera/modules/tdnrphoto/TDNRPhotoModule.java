package com.dream.camera.modules.tdnrphoto;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.PhotoModule;
import android.view.View;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.dream.camera.util.DreamUtil;

public class TDNRPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("TDNRPhotoModule");

    public TDNRPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        mFirstIn3DNR = true;
        return new TDNRPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    protected void updateParameters3DNR() {
        if (!CameraUtil.is3DNREnable()) {
            return;
        }

        Log.i(TAG, "updateParameters3DNR set3DNREnable : 1");
        mCameraSettings.set3DNREnable(1);
    }

    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isNormalNeedThumbCallback()){
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
            mCameraSettings.setThumbCallBack(1);
        } else {
            super.updateParametersThumbCallBack();
        }
    }

    @Override
    protected void updateParametersZsl() {
        Log.i(TAG, "TDNR setZslModeEnable: 0");
        mCameraSettings.setZslModeEnable(0);
    }

    public int getModuleTpye() {
        return TDNR_PHOTO_MODULE;
    }

    @Override
    public void pause() {
        mFirstIn3DNR = false;
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
    }

    private boolean mFirstIn3DNR;
    private int m3DNRToAutoNumber;
    private int mAiSceneTypeFor3DNR;
    private int mCount3DNRInfo;

    @Override
    public void onAiScene(int aiSceneType) {
        Log.i(TAG, "TDNR aiSceneType = " + aiSceneType);
        if(mPaused){
            return;
        }

        mAiSceneTypeFor3DNR = aiSceneType;
        if(aiSceneType == 5){
            mActivity.getCameraAppUI().updateAiSceneView(View.VISIBLE , aiSceneType);
        } else {
            mActivity.getCameraAppUI().updateAiSceneView(View.INVISIBLE , aiSceneType);
        }

        if(mFirstIn3DNR){
            m3DNRToAutoNumber = CameraUtil.get3DNRToAutoDefaultNumber();
        }
        if(mCameraState != SNAPSHOT_IN_PROGRESS){
            remainingPortraitAnd3DNRDefaultNumber(mAiSceneTypeFor3DNR);
        }
    }

    private void remainingPortraitAnd3DNRDefaultNumber(int aiSceneType) {
        Log.i(TAG, "TDNR mCount3DNRInfo = " + mCount3DNRInfo);
        if(mCount3DNRInfo != m3DNRToAutoNumber){
            if(aiSceneType == 5){
                if(mCount3DNRInfo > 0){
                    --mCount3DNRInfo;
                }
            } else {
                ++mCount3DNRInfo;
            }
        } else {
            switchTo3DNROrPortrait(SettingsScopeNamespaces.AUTO_PHOTO);
        }
        mFirstIn3DNR = false;
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

}

