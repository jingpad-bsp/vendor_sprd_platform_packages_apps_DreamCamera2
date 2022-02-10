
package com.dream.camera.modules.portraitbackgroundreplacementvideo.sprd;

import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.view.View;
import android.view.MotionEvent;

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
import com.dream.camera.modules.portraitbackgroundreplacementphoto.sprd.PortraitBackgroundReplacementPhotoUI;
import com.dream.camera.portraitbackgroundreplacement.SmallAdvancedPortraitBackgroundReplacement;
import com.dream.camera.ucam.utils.UiUtils;
import com.dream.camera.util.DreamUtil;
import com.sprd.camera.storagepath.StorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.android.camera.app.OrientationManager;

/**
 * Created by chencl on 16-7-14.
 */

public class PortraitBackgroundReplacementVideoModule extends VideoModule  {
    private static final Log.Tag TAG = new Log.Tag("PortraitBackgroundReplacementVideoModule");
    private PortraitBackgroundReplacementVideoUI mPortraitBackgroundReplacementUI;
    protected SmallAdvancedPortraitBackgroundReplacement mSmallAdvancedPortraitBackgroundReplacement;

    /**
     * Construct a new video module.
     *
     * @param app
     */
    public PortraitBackgroundReplacementVideoModule(AppController app) {
        super(app);
    }

    @Override
    public VideoUI createUI(CameraActivity activity) {
        mPortraitBackgroundReplacementUI = new PortraitBackgroundReplacementVideoUI(mActivity, this,
                mActivity.getModuleLayoutRoot());
        mSmallAdvancedPortraitBackgroundReplacement = mPortraitBackgroundReplacementUI.getSmallAdvancedPortraitBackgroundReplacement();

        return mPortraitBackgroundReplacementUI;
//        return new PortraitBackgroundReplacementVideoUI(activity, this, activity.getModuleLayoutRoot());
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
    public void destroy() {
        super.destroy();// modify for oom
        if (mUI != null) {
            ((PortraitBackgroundReplacementVideoUI) mUI).destroy();
        }
    }
    @Override
    public void updatePortraitBackgroundReplacementType() {
        int portraitbackgroundreplacementType = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        Log.i(TAG, "updateportraitbackgroundreplacementType portraitbackgroundreplacementType = " + portraitbackgroundreplacementType);
        if (mCameraSettings != null){ //SPRD:Bug fix 1345770 NullPointerException
            mCameraSettings.setPortraitBackgroundReplacementType(portraitbackgroundreplacementType);
        }
        if(mCameraDevice != null)
        mCameraDevice.applySettings(mCameraSettings);
    }

    @Override
    public void onPreviewStartedAfter() {
        Log.i(TAG, "onPreviewStartedAfter");
        int type = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        mSmallAdvancedPortraitBackgroundReplacement.setDreamPortraitBackgroundReplacementType(type);

    }




    @Override
    protected void requestCamera(int id) {
        // Note the request camera id is different from saved id
        mCameraId = DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(mCameraId)?CameraUtil.FRONT_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID
                :CameraUtil.BACK_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID;
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
        Log.i(TAG, "PortraitBackgroundReplacement request camera id:" + mCameraId + " useNewApi()" + useNewApi());
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return null;
    }

    /* Fix bug 585183 Adds new features 3D recording @{  @{ */
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        UiUtils.initialize(activity);
        super.init(activity,isSecureCamera,isCaptureIntent);
    }
    /* @} */

    public void switchPreview(){
    }
    
    /* @} */

    @Override
    public void onOrientationChanged(OrientationManager orientationManager, OrientationManager.DeviceOrientation deviceOrientation) {
        super.onOrientationChanged(orientationManager, deviceOrientation);
        sendDeviceOrientation();
        if(mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    @Override
    protected void sendDeviceOrientation() {
        if (mCameraDevice != null && mCameraSettings != null && mActivity != null && mActivity.getOrientationManager() != null) {
            int deviceOrientation = mActivity.getOrientationManager().getDeviceOrientation().getDegrees();
            mCameraSettings.setDeviceOrientation(deviceOrientation);
            Log.i(TAG, "send DeviceOrientation " + deviceOrientation + " to HAL");
        }
    }

    @Override
    public void onLongPress(MotionEvent var1){}

    @Override
    public void onSingleTapUp(View view, int x, int y) {}
    @Override
    protected int getVideoQuality() {
        return CamcorderProfile.QUALITY_720P;
    }

    @Override
    protected CamcorderProfile getCamcorderProfile(int quality) {
        return CamcorderProfile.get(isCameraFrontFacing() ? 1 : 0, quality);
    }

    @Override
    protected boolean isBackGroudMode(){
        return true;
    }

    @Override
    protected boolean isTakeSnapShot(){
        return false;
    }
}
