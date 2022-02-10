package com.dream.camera.modules.macrovideo;

import android.hardware.Camera.CameraInfo;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.VideoUI;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.VideoModule;
import com.dream.camera.modules.autovideo.AutoVideoUI;
import com.sprd.camera.storagepath.StorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.android.camera.app.OrientationManager;

/**
 * Created by chencl on 16-7-14.
 */

public class MacroVideoModule extends VideoModule  {
    private static final Log.Tag TAG = new Log.Tag("MacroVideoModule");

    /**
     * Construct a new video module.
     *
     * @param app
     */
    public MacroVideoModule(AppController app) {
        super(app);
    }

    @Override
    public VideoUI createUI(CameraActivity activity) {
        return new MacroVideoUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        super.onCameraAvailable(cameraProxy);

    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void pause() {
        // reset to default
        super.pause();
    }

    @Override
    protected void requestCamera(int id) {
        // Note the request camera id is different from saved id
        mCameraId = CameraUtil.BACK_MACRO_CAMERA_ID;//SPRD:fix bug621277
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
        Log.i(TAG, "Macro request camera id:" + mCameraId + " useNewApi()" + useNewApi());
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return null;
    }

    /* Fix bug 585183 Adds new features 3D recording @{  @{ */
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity,isSecureCamera,isCaptureIntent);
    }
    /* @} */

    public void switchPreview(){
    }

    @Override
    protected String getVideoQualityKey() {
        return Keys.KEY_VIDEO_QUALITY_BACK_MACRO;
    }

    @Override
    public void onOrientationChanged(OrientationManager orientationManager, OrientationManager.DeviceOrientation deviceOrientation) {
        super.onOrientationChanged(orientationManager, deviceOrientation);
    }

    @Override
    protected boolean isTakeSnapShot(){
        return false;
    }
}
