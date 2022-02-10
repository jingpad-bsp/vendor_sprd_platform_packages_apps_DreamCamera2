package com.dream.camera.modules.ultrawideangle;

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
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.settings.Keys;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.PhotoModule;
import com.dream.camera.DreamModule;

import com.dream.camera.settings.DataModuleManager;
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


public class UltraWideAngleModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("UltraWideAngleModule");


    public UltraWideAngleModule(AppController app) {
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
        ViewStub viewStubAEPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAEPanel != null) {
            viewStubAEPanel.inflate();
        }
        mAppController.getCameraAppUI().setAdjustPanel((AdjustPanel) activity.findViewById(R.id.adjust_panel));
        mAppController.getCameraAppUI().setBlurPanel((BlurPanel) activity.findViewById(R.id.blur_panel));

        return new UltraWideAngleUI(activity, this, activity.getModuleLayoutRoot());
    }

    @Override
    protected void requestCameraOpen() {
        mCameraId = CameraUtil.getBackUltraWideAngleCameraId();
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
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
    protected void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }
/*
        List<Size> supported = Size.convert(mCameraCapabilities
                .getSupportedPhotoSizes());
        CameraPictureSizesCacher.updateSizesForCamera(
                mAppController.getAndroidContext(),
                mCameraDevice.getCameraId(), supported);
*/
        OneCamera.Facing cameraFacing = mAppController.getCameraProvider().getCharacteristics(mCameraId)
                .isFacingFront() ? OneCamera.Facing.FRONT : OneCamera.Facing.BACK;
        Size pictureSize;
        try {
            pictureSize = mAppController.getResolutionSetting()
                    .getPictureSize(DataModuleManager.getInstance(mAppController.getAndroidContext()),
                            mAppController.getCameraProvider()
                                    .getCurrentCameraId(), cameraFacing);
        } catch (OneCameraAccessException ex) {
            mAppController.getFatalErrorHandler()
                    .onGenericCameraAccessFailure();
            return;
        }

        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());


        // SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(pictureSize,
                mAppController.getCameraProvider()).toPortabilitySize());

        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = Size.convert(mCameraCapabilities
                .getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes,
                (double) pictureSize.width() / pictureSize.height());
        Size original = new Size(mCameraSettings.getCurrentPreviewSize());
        if (optimalSize != null) { // Bug 1159232 (NULL_RETURNS)
            if (!optimalSize.equals(original)) {
                Log.i(TAG, "setting preview size. optimal: " + optimalSize
                        + "original: " + original);
                mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());
            }

            if (optimalSize.width() != 0 && optimalSize.height() != 0) {
                Log.i(TAG, "updating aspect ratio");
                mUI.updatePreviewAspectRatio((float) optimalSize.width()
                        / (float) optimalSize.height());
            }
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }

    /*@Override*/
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height,
                         int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
        Log.i(TAG, "addImage TAG_CAMERATYPE_IFD val=" + val);
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
    public void resume() {
        super.resume();
//        if(CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1) {
//            mAdjustPanel = (AdjustPanel)mActivity.getModuleLayoutRoot().findViewById(R.id.adjust_panel);
//            mAdjustPanel.setActive(true);
//            mVerticalSeekBar = (VerticalSeekBar)mActivity.getModuleLayoutRoot().findViewById(R.id.vseekbar);
//            mVerticalSeekBar.setProgress(127);
//            mVerticalSeekBar.setOnSeekBarChangeListener(this);
//        }
    }

    @Override
    public void pause() {
        super.pause();
//        mAppController.getCameraAppUI().setRefocusModuleTipVisibility(View.GONE);
//        if(CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_1) {
//            if (mAdjustPanel != null && mVerticalSeekBar != null) {
//                mAdjustPanel.setActive(false);
//                mVerticalSeekBar.setProgress(127);
//                mVerticalSeekBar.setOnSeekBarChangeListener(null);
//            }
//        }
//        mAppController.getCameraAppUI().setBlurEffectTipVisibity(View.GONE);
    }

//    @Override
//    public void onStartTrackingTouch(SeekBar SeekBarId) {
//    }
//
//    @Override
//    public void onStopTrackingTouch(SeekBar SeekBarId) {
//    }

    @Override
    public int getModuleTpye() {
        return DreamModule.ULTRAWIDEANGLE_MODULE;
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

    @Override
    public void destroy() {
        super.destroy();
        // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
        mAppController.getCameraAppUI().deinitUltraWideAngleSwitchView();
    }
}
