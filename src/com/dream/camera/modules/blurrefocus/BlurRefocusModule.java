package com.dream.camera.modules.blurrefocus;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.net.Uri;

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
import com.android.camera.util.ToastUtil;
import com.android.camera.widget.ModeOptions;
import com.android.camera.widget.ModeOptionsOverlay;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.PhotoModule;
import com.sprd.camera.voice.PhotoVoiceMessage;
import com.dream.camera.DreamModule;
import com.dream.camera.BlurRefocusController;
import com.dream.camera.BlurRefocusController.BlurRefocusFNumberListener;
import com.dream.camera.ui.BlurPanel;
import com.dream.camera.ui.VerticalSeekBar;
import com.dream.camera.ui.AdjustPanel;

import com.android.camera.app.OrientationManager;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.location.Location;

import java.util.List;
import android.net.Uri;

public class BlurRefocusModule extends PhotoModule implements BlurRefocusFNumberListener, SeekBar.OnSeekBarChangeListener {
    private static final Log.Tag TAG = new Log.Tag("BlurRefocusModule");
    private int mFNumber = 0;
    private AdjustPanel mAdjustPanel;
    private VerticalSeekBar mVerticalSeekBar;

    public BlurRefocusModule(AppController app) {
        super(app);
    }

    @Override
    public boolean isSupportTouchAFAE() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isAutoHdrSupported() {
        return super.isAutoHdrSupported();//SPRD:fix bug1042547
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
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_adjust_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        ViewStub viewStubBlurPanel = (ViewStub) activity.findViewById(R.id.layout_blur_panel_id);
        if (viewStubBlurPanel != null) {
            viewStubBlurPanel.inflate();
        }
        ViewStub viewStubAEPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAEPanel != null) {
            viewStubAEPanel.inflate();
        }
        mAppController.getCameraAppUI().setAdjustPanel((AdjustPanel) activity.findViewById(R.id.adjust_panel));
        mAppController.getCameraAppUI().setBlurPanel((BlurPanel) activity.findViewById(R.id.blur_panel));
        showBlurRefocusHind();
        return new BlurRefocusUI(activity, this, activity.getModuleLayoutRoot());
    }

    public void showBlurRefocusHind() {
        boolean shouldShowBlurRefocusHind = mDataModule.getBoolean(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT);
        if (shouldShowBlurRefocusHind == true) {
            CameraUtil.toastHint(mActivity,R.string.camera_blur_refocus_tip);
            mDataModule.set(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT, false);
        }
    }

    @Override
    public void onLongPress(MotionEvent var1){}

    @Override
    protected void requestCameraOpen() {
        Log.i(TAG, "requestCameraOpen mCameraId:" + CameraUtil.getBackBlurCameraId());
        mCameraId = CameraUtil.getBackBlurCameraId();
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
        //mCameraOpen = true;
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
        super.startPreview(optimize);
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

    @Override
    public void onBlurClosed(boolean close) {}

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
        if (mCameraState == FOCUSING) {
            return;
        }
        if (mCameraSettings != null) {
            mCameraSettings.setFNumberValue(mFNumber);
            super.onSingleTapUp(view, x, y);
        }
    }

    @Override
    public void resume() {
        super.resume();
//        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
        if(CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1) {
            mAdjustPanel = (AdjustPanel)mActivity.getModuleLayoutRoot().findViewById(R.id.adjust_panel);
            mAdjustPanel.setActive(true);
            mVerticalSeekBar = (VerticalSeekBar)mActivity.getModuleLayoutRoot().findViewById(R.id.vseekbar);
            mVerticalSeekBar.setProgress(127);
            mVerticalSeekBar.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    public void pause() {
        super.pause();
        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
        if(CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1) {
            if (mAdjustPanel != null && mVerticalSeekBar != null) {
                mAdjustPanel.setActive(false);
                mVerticalSeekBar.setProgress(127);
                mVerticalSeekBar.setOnSeekBarChangeListener(null);
            }
        }
        mAppController.getCameraAppUI().setBlurEffectTipVisibity(View.GONE);
    }

    @Override
    public void onProgressChanged(SeekBar SeekBarId, int progress, boolean fromUser) {
        Log.i(TAG, " onProgressChanged = " + progress);
        if(mCameraSettings != null && mCameraDevice != null) {
            mCameraSettings.setCircleSize(progress + 1);
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar SeekBarId) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar SeekBarId) {
    }
/*
    @Override
    protected void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }

        Size pictureSize = new Size(2592, 1944);
        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());

        // SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(pictureSize,
                mAppController.getCameraProvider()).toPortabilitySize());

        Size optimalSize = new Size(960, 720);
        Size original = new Size(mCameraSettings.getCurrentPreviewSize());
        mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());

        if (optimalSize.width() != 0 && optimalSize.height() != 0) {
            Log.i(TAG, "updating aspect ratio");
            mUI.updatePreviewAspectRatio((float) optimalSize.width()
                    / (float) optimalSize.height());
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }
*/
    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isBlurNeedThumbCallback()){
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
        } else {
            Log.i(TAG, "setNeedThumbCallBack false");
            mCameraSettings.setNeedThumbCallBack(false);
        }
    }

    @Override
    protected void focusAndCapture() {
        if(CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1
                || isBackRealDualCamVersion()) {
            super.focusAndCapture();
            return;
        } else if (CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_2
                || CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_3) {
            if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)) {
                if (!mIsImageCaptureIntent) {
                    mSnapshotOnIdle = true;
                }
                return;
            }

            /*
             * SPRD:Fix bug 502060 close the function of Zoom only during the
             * Continuous Capture@{
             * SPRD:fix bug 596882/617069 disable zoom when capture, if zoom in zsl capture, next capture will fail
             */
            mUI.enablePreviewOverlayHint(false);// SPRD:Fix bug 411062
            mUI.hideZoomProcessorIfNeeded();
            /* @} */

            setShutterButtonState();
            mSnapshotOnIdle = false;
            boolean focusSupported = mCameraCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA);
            if(focusSupported) {
                mFocusManager.forceFocusBeforeCapture();
            } else {
                capture();
            }
        }
    }

    @Override
    public int getModuleTpye() {
        return DreamModule.REFOCUS_MODULE;
    }

    @Override
    public boolean isSupportTouchEV() {
        return false;
    }

    @Override
    protected boolean isUpdateFlashTopButton(){
        // auto mode support flash
        if(CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing())){
            return false;
        } else {
            return true;
        }
    }
}
