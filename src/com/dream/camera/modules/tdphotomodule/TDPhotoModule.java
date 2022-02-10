
package com.dream.camera.modules.tdphotomodule;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.MediaSaver;
import com.android.camera.data.FilmstripItemData;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.PhotoModule;
import com.sprd.camera.voice.PhotoVoiceMessage;
import com.dream.camera.DreamModule;

import android.location.Location;

import java.util.List;

public class TDPhotoModule extends PhotoModule implements CameraAppUI.PanelsVisibilityListener{
    private ImageView mPreviewSwitchButton = null;
    private static final Log.Tag TAG = new Log.Tag("TDPhotoModule");

    public TDPhotoModule(AppController app) {
        super(app);
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

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        return new TDPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity, isSecureCamera, isCaptureIntent);
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = super.getBottomBarSpec();
        bottomBarSpec.enableCamera = false;
        bottomBarSpec.hideCamera = true;
        bottomBarSpec.enableGridLines = false;
        bottomBarSpec.hideGridLines = true;
        bottomBarSpec.enableFlash = false;
        bottomBarSpec.hideFlash = true;
        bottomBarSpec.enableHdr = false;
        bottomBarSpec.hideHdr = true;
        // SPRD: Fix 608453 Remove exposure option
        bottomBarSpec.enableExposureCompensation = false;
        return bottomBarSpec;
    }

    @Override
    public void resume() {
        Log.i(TAG, "resume start!");
        // ISP 3A matching needs the property set before camera opened
        //SystemProperties.set("sys.cam.refocus", "5");
        super.resume();

        if (mPreviewSwitchButton != null) {
            mPreviewSwitchButton.setVisibility(View.VISIBLE);
        }
        Log.i(TAG, "resume end!");
    }

    @Override
    public void pause() {
        Log.i(TAG, "pause start!");
        super.pause();
        // ISP 3A matching needs the property set to zero here
        //SystemProperties.set("sys.cam.refocus", "0");
        // parent class will also set this listener
        mActivity.getCameraAppUI().setPanelsVisibilityListener(null);
        // reset to default
        set3DReviewMode(Integer.MAX_VALUE);

        /* SPRD: Fix bug 609677 Change the location of the 3D Icon @{ */
        if (mPreviewSwitchButton != null) {
            mPreviewSwitchButton.setVisibility(View.GONE);
        }
        /* @} */
        Log.i(TAG, "pause end!");
    }

    @Override
    protected void requestCameraOpen() {
        Log.d(TAG, "requestCameraOpen mCameraId:" + CameraUtil.TD_PHOTO_ID);
        mCameraId = CameraUtil.TD_PHOTO_ID;
        mActivity.getCameraProvider().requestCamera(CameraUtil.TD_PHOTO_ID, useNewApi());
        //mCameraOpen = true;
    }

    @Override
    protected String getNameTitle(String title) {
        return title + "_3DPhoto";
    }

    @Override
    public void startFaceDetection() {

    }

    @Override
    protected void updateParametersBurstMode() {
        mCameraSettings.setBurstModeEnable(0);
        Log.i(TAG, "setBurstModeEnable : 0");
    }
    // open zsl.
    @Override
    protected void updateParametersZsl() {
        Log.i(TAG, "setZslModeEnable: 1");
        mCameraSettings.setZslModeEnable(1);
        // SPRD: Fix bug 592205 The Super Night Set as Default
    }

    @Override
    protected void updateParametersPictureSize() {
        Log.d(TAG,"updateParametersPictureSize");
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }

        String tdPictureSize = mDataModuleCurrent.getString(Keys.KEY_PICTURE_SIZE_FRONT_3D);
        if (TextUtils.isEmpty(tdPictureSize)) {
            mDataModuleCurrent.set(Keys.KEY_PICTURE_SIZE_FRONT_3D, mDataModuleCurrent.getStringDefault(Keys.KEY_PICTURE_SIZE_FRONT_3D));
            tdPictureSize = mActivity.getResources().getString(R.string.camera_picturesize_3d_default);
        }
        Size pictureSize;
        if (tdPictureSize != null && tdPictureSize.contains("x")) {
            String[] pictureSizes = tdPictureSize.split("x");
            pictureSize = new Size(Integer.parseInt(pictureSizes[0]), Integer.parseInt(pictureSizes[1]));
        } else {
            Log.d(TAG, "this 3d picture size is not support " + tdPictureSize);
            mAppController.getFatalErrorHandler().onGenericCameraAccessFailure();
            return;
        }
        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());
        // SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(pictureSize,
                mAppController.getCameraProvider()).toPortabilitySize());
        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = Size.convert(mCameraCapabilities.getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes,
                (double) pictureSize.width() / pictureSize.height());

        if (optimalSize !=  null) { // Bug 1159225 (NULL_RETURNS)
            mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());

            if (optimalSize.width() != 0 && optimalSize.height() != 0) {
                mUI.updatePreviewAspectRatio((float) optimalSize.width()
                        / (float) optimalSize.height());
            }
        }
        mCameraDevice.applySettings(mCameraSettings);

        Log.d(TAG, "Preview size is " + optimalSize);
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        super.onCameraAvailable(cameraProxy);
        // parent class will also set this listener
        mActivity.getCameraAppUI().setPanelsVisibilityListener(this);
        // init preview mode
        set3DReviewMode(mCurrentPreviewMode);
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

    private void on3DReviewModeSwitch() {
        mCurrentPreviewMode = getNextPreviewMode();
        set3DReviewMode(mCurrentPreviewMode);
    }

    private void set3DReviewMode(int previewMode) {
        CameraSettings cameraSettings = mCameraSettings;
        CameraAgent.CameraProxy cameraDevice = mCameraDevice;
        if (cameraSettings != null && cameraDevice != null) {
            cameraSettings.set3DPreviewMode(previewMode);
            cameraDevice.applySettings(cameraSettings);
        }
    }

    @Override
    public void onPanelsHidden() {
        if (mPreviewSwitchButton != null) {
            mPreviewSwitchButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPanelsShown() {
        if (mPreviewSwitchButton != null) {
            mPreviewSwitchButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void handleActionUp() {
        super.handleActionUp();
        // we can not take the second picture because mCaptureCount is not zero,
        // onShutterButtonClick will return then, do workaround here by setting zero
        mCaptureCount = 0;
    }

    /* SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{ */
    /*@Override*/
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height,
                         int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l) {
        getServices().getMediaSaver().addImage(
                data, title, date, loc, width, height,
                orientation, exif, l,
                FilmstripItemData.MIME_TYPE_3D_JPEG, null);
    }
    /* @} */

    // if not set, the width and height may be reversed
    @Override
    public boolean shouldFollowOriginalSize() {
        return true;
    }

    @Override
    public boolean useNewApi() {
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();

        // SPRD: Fix Bug607972 beauty button can not be clicked
        // after switch 3D photo mode from filter mode
    }
    /*
    public void onStopRecordVoiceClicked(View v) {
        Log.e(TAG, "onStopRecordVoiceClicked isAudioRecording = " + isAudioRecording);
        mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_AUDIO);
        if (isAudioRecording) {
            mPhotoVoiceRecorder.stopAudioRecord();
        }
    }
    */
    @Override
    public void switchPreview(){
        on3DReviewModeSwitch();
    }

    private static int MAIN_PREVIEW_MODE = 0;
    private void reset3DPreviewMode(){
        if (mCurrentPreviewMode == MAIN_PREVIEW_MODE){
            return;
        }
        mCurrentPreviewMode = MAIN_PREVIEW_MODE;
        set3DReviewMode(mCurrentPreviewMode);
    }

    /**
     * SPRD fix bug 615287 the 3D capture not normal. @{
     */
    @Override
    protected void updateParametersSceneMode() {
        if (isHdr()) {
            return;
        }
        super.updateParametersSceneMode();
    }
	/* @} */
    @Override
    public int getModuleTpye() {
        return DreamModule.TD_PHOTO_MODULE;
    }

}
