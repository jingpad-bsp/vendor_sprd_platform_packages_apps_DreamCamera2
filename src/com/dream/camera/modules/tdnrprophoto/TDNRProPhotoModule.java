package com.dream.camera.modules.tdnrprophoto;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.PhotoModule;


public class TDNRProPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("TDNRProPhotoModule");

    public TDNRProPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        return new TDNRProPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    protected void updateParameters3DNR() {
        if (!CameraUtil.is3DNRProEnable()) {
            return;
        }
        Log.i(TAG, "updateParameters3DNRPro set3DNRProEnable : 1");
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
        Log.i(TAG, "TDNRPro setZslModeEnable: 0");
        mCameraSettings.setZslModeEnable(0);
    }

    public int getModuleTpye() {
        return TDNR_PRO_PHOTO_MODULE;
    }
}

