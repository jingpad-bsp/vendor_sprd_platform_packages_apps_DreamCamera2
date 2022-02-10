package com.dream.camera.modules.blurrefocus;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.PhotoModule;
import com.sprd.camera.voice.PhotoVoiceMessage;
import com.dream.camera.DreamModule;
import com.dream.camera.BlurRefocusController;
import com.dream.camera.BlurRefocusController.BlurRefocusFNumberListener;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DreamSettingUtil;
import com.dream.camera.ui.AdjustPanel;
import com.dream.camera.ui.VerticalSeekBar;
import com.dream.camera.ui.BlurPanel;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.camera.app.OrientationManager;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;

import android.location.Location;

import java.util.List;
import android.net.Uri;

public class FrontBlurRefocusModule extends PhotoModule implements SeekBar.OnSeekBarChangeListener {
    private static final Log.Tag TAG = new Log.Tag("FrontBlurRefocusModule");
    private int mFNumber = 0;
    private BlurPanel mBlurPanel;
    private VerticalSeekBar mVerticalSeekBar;
    private TextView mBlurTv;
    private String[] fNumList = new String[] {"F0.95", "F1.4", "F2.0", "F2.8", "F4.0", "F5.6", "F8.0", "F11.0", "F13.0", "F16.0"};
    private int mDefaultValue = 4;
    private int mCurrentValue;

    public FrontBlurRefocusModule(AppController app) {
        super(app);
    }

    @Override
    public boolean isSupportTouchAFAE() {
        // TODO Auto-generated method stub
        return true;
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
        mAppController.getCameraAppUI().setAdjustPanel((AdjustPanel) activity.findViewById(R.id.adjust_panel));
        mAppController.getCameraAppUI().setBlurPanel((BlurPanel) activity.findViewById(R.id.blur_panel));
        showBlurRefocusHind();
        return new FrontBlurRefocusUI(activity, this, activity.getModuleLayoutRoot());
    }

    public void showBlurRefocusHind() {
        boolean shouldShowBlurRefocusHind = mDataModule.getBoolean(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT);
        if (shouldShowBlurRefocusHind == true) {
            CameraUtil.toastHint(mActivity,R.string.camera_blur_refocus_tip);
            mDataModule.set(Keys.KEY_CAMERA_BLUR_REFOCUS_HINT, false);
        }
    }

    @Override
    protected void requestCameraOpen() {
        Log.i(TAG, "requestCameraOpen mCameraId:" + CameraUtil.getFrontBlurCameraId());
        mCameraId = CameraUtil.getFrontBlurCameraId();
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
        //mCameraOpen = true;
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

    @Override
    public boolean useNewApi() {
        return true;
    }

    @Override
    public void resume() {
        super.resume();
//        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
        mBlurPanel = (BlurPanel)mActivity.getModuleLayoutRoot().findViewById(R.id.blur_panel);
        mBlurPanel.setActive(true);
        mBlurPanel.setVisibility(View.VISIBLE);
        mVerticalSeekBar = (VerticalSeekBar)mActivity.getModuleLayoutRoot().findViewById(R.id.blur_vseekbar);
        mVerticalSeekBar.setOnSeekBarChangeListener(this);
        waitInitDataSettingCounter();
        mCurrentValue = getValue() - 1;
        mVerticalSeekBar.setProgress(mCurrentValue);
        mBlurTv = (TextView)mActivity.getModuleLayoutRoot().findViewById(R.id.blur_tv);
        mBlurTv.setText(fNumList[mCurrentValue]);
    }

    private int getValue() {
        return DreamSettingUtil.convertToInt(DataModuleManager
                .getInstance(mVerticalSeekBar.getContext())
                .getCurrentDataModule()
                .getString(DataConfig.SettingStoragePosition.positionList[3],
                 Keys.KEY_BLUR_REFOCUS_LEVEL, "" + mDefaultValue));
    }

    private void setValue(int level) {
        DataModuleManager
        .getInstance(mVerticalSeekBar.getContext())
        .getCurrentDataModule()
        .set(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_BLUR_REFOCUS_LEVEL,
                "" + level);
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
            mCameraSettings.setFNumberValue(getValue());
            mCameraDevice.applySettings(mCameraSettings);
        }
        Log.i(TAG, "startPreview deviceOrientation:" + deviceOrientation);
        super.startPreview(optimize);
    }

    @Override
    public void pause() {
        super.pause();
        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
        /** SPRD:Bug839740 mBlurPanel was not initialized when the pause method is called @{ */
        if (mBlurPanel != null) {
            mBlurPanel.setActive(false);
        }
        if (mVerticalSeekBar != null) {
            mVerticalSeekBar.setOnSeekBarChangeListener(null);
        }
        /** @}*/
        mAppController.getCameraAppUI().setBlurEffectTipVisibity(View.GONE);
    }

    @Override
    public void onProgressChanged(SeekBar SeekBarId, int progress, boolean fromUser) {
        Log.i(TAG, " onProgressChanged = " + progress);
        if(mCameraSettings != null && mCameraDevice != null) {
            onFNumberValueChanged(progress + 1);
            setValue(progress + 1);
            mBlurTv.setText(fNumList[progress]);
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
    protected void focusAndCapture() {
        Log.i(TAG, "front blur focusAndCapture ");
        if(CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1
                || isFrontRealDualCamVersion()) {
            super.focusAndCapture();
            return;
        } else if (CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_2
                || CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_3) {
            setShutterButtonState();
            mFocusManager.forceFocusBeforeCapture();
        }
    }

    @Override
    public void onOrientationChanged(OrientationManager orientationManager, OrientationManager.DeviceOrientation deviceOrientation) {
        int deviceRotation = deviceOrientation.getDegrees();
        Log.i(TAG, "front blur onOrientationChanged  deviceOrientation = " + deviceRotation);
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setDeviceOrientation(deviceRotation);
            mCameraDevice.applySettings(mCameraSettings);
        }
        super.onOrientationChanged(orientationManager, deviceOrientation);
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        super.onSingleTapUp(view, x, y);
        if (mPaused || mCameraDevice == null
                || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA
                || mCameraState == PREVIEW_STOPPED
                || mFocusManager == null) {
            Log.i(TAG, "onSingleTapUp return!");
            return;
        }
        mFocusManager.onSingleTapUpInFrontBlur(x, y);
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setFocusAreas(mFocusManager.getFrontRefocusAreas());
            mCameraDevice.applySettings(mCameraSettings);
        }
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
    private void onFNumberValueChanged(int value) {
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
    public void onPreviewStarted(){
        super.onPreviewStarted();
        onFNumberValueChanged(getValue());
    }

    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isBlurNeedThumbCallback()
                && (CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_7
                || CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_8)){
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
        } else {
            Log.i(TAG, "setNeedThumbCallBack false");
            mCameraSettings.setNeedThumbCallBack(false);
        }
    }

    @Override
    public int getModuleTpye() {
        return DreamModule.FRONT_REFOCUS_MODULE;
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
