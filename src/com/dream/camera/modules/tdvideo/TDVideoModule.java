package com.dream.camera.modules.tdvideo;

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

public class TDVideoModule extends VideoModule  {
    private static final Log.Tag TAG = new Log.Tag("TDVideoModule");

    /**
     * Construct a new video module.
     *
     * @param app
     */
    public TDVideoModule(AppController app) {
        super(app);
    }

    @Override
    public VideoUI createUI(CameraActivity activity) {
        return new TDVideoUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        super.onCameraAvailable(cameraProxy);
        // init preview mode
        set3DReviewMode(mCurrentPreviewMode);

    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void pause() {
        // reset to default
        set3DReviewMode(Integer.MAX_VALUE);
        super.pause();
    }

    @Override
    protected void requestCamera(int id) {
        // Note the request camera id is different from saved id
        mCameraId = CameraUtil.TD_CAMERA_ID;//SPRD:fix bug621277
        mActivity.getCameraProvider().requestCamera(CameraUtil.TD_CAMERA_ID);
        Log.i(TAG, "3d VideoModule start Front camera");
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return null;
    }

//    @Override
//    protected void updateTimeLapse() {
//
//    }
//
//    @Override
//    protected void setSlowmotionParameters() {
//
//    }
//
//    @Override
//    protected void updateParametersAntibanding() {
//
//    }

    @Override
    protected String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mActivity.getString(R.string.video_file_name_format));

        return dateFormat.format(date) + "_3DVideo";
    }

    @Override
    protected String getVideoQualityKey() {
        return Keys.KEY_VIDEO_QUALITY_FRONT_3D;
    }

    /* Fix bug 585183 Adds new features 3D recording @{  @{ */
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity,isSecureCamera,isCaptureIntent);
        CameraUtil.toastHint(mActivity,R.string.tdvideo_mutex);
    }
    /* @} */

    @Override
    protected int getRecorderOrientationHint(int rotation) {
        // SPRD: Fix bug 600110 that the rotation of 3d video content is not correct
        // do support for camera hal
        return 0;
    }

    public void switchPreview(){
        // TODO
        on3DReviewModeSwitch();
    }

    private int mCurrentPreviewMode = 0;

    private int getNextPreviewMode() {
        switch (mCurrentPreviewMode) {
            case 0:
                return 1;
            case 1:
                return 0;
            default:
                return mCurrentPreviewMode;
        }
    }

    private void set3DReviewMode(int previewMode) {
        if (mCameraSettings != null && mCameraDevice != null) {
            mCameraSettings.set3DPreviewMode(previewMode);
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    private void on3DReviewModeSwitch() {
        mCurrentPreviewMode = getNextPreviewMode();
        set3DReviewMode(mCurrentPreviewMode);
    }

    /* SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{ */
    public String getFileDir() {
        return StorageUtil.getInstance().getFileDirFor3D();
    }
    /* @} */

    @Override
    public void onOrientationChanged(OrientationManager orientationManager, OrientationManager.DeviceOrientation deviceOrientation) {
        int deviceRotation = deviceOrientation.getDegrees();
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setDeviceRotationFor3DRecord(deviceRotation);
            mCameraDevice.applySettings(mCameraSettings);
        }
        super.onOrientationChanged(orientationManager, deviceOrientation);
    }

    private static int MAIN_PREVIEW_MODE = 0;
    private void reset3DPreviewMode(){
        if (mCurrentPreviewMode == MAIN_PREVIEW_MODE){
            return;
        }
        mCurrentPreviewMode = MAIN_PREVIEW_MODE;
        set3DReviewMode(mCurrentPreviewMode);
    }
    @Override
    protected boolean isTakeSnapShot(){
        return false;
    }
}
