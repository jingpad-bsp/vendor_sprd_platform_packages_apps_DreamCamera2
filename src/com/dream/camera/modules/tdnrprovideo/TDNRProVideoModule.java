
package com.dream.camera.modules.tdnrprovideo;

import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.CameraActivity;
import com.android.camera.VideoUI;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import android.graphics.Point;

import com.android.camera.VideoModule;

public class TDNRProVideoModule extends VideoModule {
    private static final Log.Tag TAG = new Log.Tag("TDNRVideoModule");
    public TDNRProVideoModule(AppController app) {
        super(app);
    }

    @Override
    public VideoUI createUI(CameraActivity activity) {
        return new TDNRProVideoUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    public boolean useNewApi() {
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity.getContentResolver());
    }

    @Override
    protected void updateParameters3DNR() {
        if (!CameraUtil.is3DNRProEnable()) {
            return;
        }

        Log.i(TAG, "updateParameters3DNRPRO set3DNREnable : 1");
        mCameraSettings.set3DNREnable(1);
    }

    @Override
    protected boolean is3dnrOn() {
        return true;
    }

    @Override
    protected void updateDesiredPreviewSize() {
        if (mProfile.videoFrameWidth == 1920 && null != mCameraDevice) {
            mCameraSettings = mCameraDevice.getSettings();
            desiredPreviewSize = new Point(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mDesiredPreviewWidth = desiredPreviewSize.x;
            mDesiredPreviewHeight = desiredPreviewSize.y;
            mUI.setAspectRatio((float)16/9);//SPRD:fix bug711494
        }else{
            super.updateDesiredPreviewSize();
        }
    }
}
