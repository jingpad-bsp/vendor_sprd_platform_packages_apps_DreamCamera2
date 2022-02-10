package com.dream.camera.modules.fdrphoto;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoModule;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.dream.camera.util.DreamUtil;

import java.util.List;

public class FDRPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("FDRPhotoModule");

    public FDRPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        return new FDRPhotoUI(activity, this, activity.getModuleLayoutRoot());
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

