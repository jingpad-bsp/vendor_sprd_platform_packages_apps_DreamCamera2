
package com.dream.camera.modules.highresolutionphoto;

import android.view.View;
import android.view.ViewStub;

import com.android.camera.ButtonManager;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoModule;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.settings.Keys;
import com.android.camera.ui.SmallAdvancedColorEffectPhoto;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.modules.autophoto.AutoPhotoUI;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.ucam.utils.UiUtils;
import com.dream.camera.util.DreamUtil;

import java.util.List;

public class HighResolutionPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("HighResolutionPhotoModule");
    public SmallAdvancedColorEffectPhoto mSmallAdvancedColorEffectPhoto;
    public String mSavedColorEffectType;
    public final ButtonManager.ButtonCallback mColorEffectPhotoCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateColorEffectPhotoDisplay();
            updateMakeUpDisplay();
        }
    };
    @Override
    protected void updateColorEffectPhotoDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY)) {
            return;
        }
        ((HighResolutionPhotoUI)mUI).updateColorEffectPhotoUI(mDataModuleCurrent.getBoolean(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY));
        if(!mDataModuleCurrent.getBoolean(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY)){
            mSavedColorEffectType = mDataModuleCurrent.getString(Keys.KEY_CAMERA_COLOR_EFFECT);
            mSmallAdvancedColorEffectPhoto.setColorEffectType("NONE");
        }else{
            mSmallAdvancedColorEffectPhoto.setColorEffectType(mSavedColorEffectType);
        }

        mSmallAdvancedColorEffectPhoto.resetDreamColorEffectType();
    }
    public HighResolutionPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        HighResolutionPhotoUI mUI = new HighResolutionPhotoUI(activity, this, activity.getModuleLayoutRoot());
        mSmallAdvancedColorEffectPhoto = mUI.getSmallAdvancedColorEffectPhoto();
        return mUI;
    }
    @Override
    public void onSurfaceTextureUpdated() {
        super.onSurfaceTextureUpdated();
        mSmallAdvancedColorEffectPhoto.requestLayout();
    }
    public boolean isSupportTouchAFAE() {
        return false;
    }

    public boolean isSupportManualMetering() {
        return false;
    }
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
                     boolean isCaptureIntent) {
        UiUtils.initialize(activity);
        super.init(activity, isSecureCamera, isCaptureIntent);
    }
    @Override
    public void onPreviewStartedAfter() {
        ((HighResolutionPhotoUI)mUI).updateColorEffectPhotoUI(mDataModuleCurrent.getBoolean(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY));
        getmHandler().post(new Runnable(){
            @Override
            public void run(){
                mSmallAdvancedColorEffectPhoto.setDreamColorEffectType(getConvertedtype());
            }

        });
    }
    public int getConvertedtype() {
        String type = mDataModuleCurrent.getString(Keys.KEY_CAMERA_COLOR_EFFECT);
        switch (type) {
            case "NONE": {
                return 1;
            }
            case "MONO": {
                return 2;
            }
            case "NEGATIVE": {
                return 3;
            }
            case "SEPIA": {
                return 4;
            }
            case "COLD": {
                return 5;
            }
            case "ANTIQUE": {
                return 6;
            }
        }
        return 1;
    }
    @Override
    public void updateParametersColorEffect() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_COLOR_EFFECT)){
            return;
        }
        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();
        String colorEffect = mDataModuleCurrent
                .getString(Keys.KEY_CAMERA_COLOR_EFFECT);
        if (colorEffect == null) {
            return;
        }
        mColorEffect = stringifier.colorEffectFromString(colorEffect);

        Log.d(TAG, "update ColorEffect = " + mColorEffect);
        mCameraSettings.setColorEffect(mColorEffect);
    }
    @Override
    protected void requestCameraOpen() {
        mCameraId = DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(mCameraId) ? CameraUtil.FRONT_HIGH_RESOLUTION_CAMERA_ID
                :CameraUtil.BACK_HIGH_RESOLUTOIN_CAMERA_ID;

        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
    }

    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }
    /* SPRD: optimize camera launch time @{ */
    public boolean useNewApi() {
        // judge VGesture enable state first will be better, like:
        // if (isShouldShowVGesture()) return false;
        // but isShouldShowVGesture will throw exception if useNewApi() is
        // called before module initialized, and no negative effect is found
        // until now, so just ignore this judgement temporarily.
        if (isUseSurfaceView()) {
            return true;
        }
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(null);
    }
    /* @} */

    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isNormalNeedThumbCallback()) {
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
            mCameraSettings.setThumbCallBack(1);
        } else {
            super.updateParametersThumbCallBack();
        }
    }



    @Override
    protected void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
            return;
        }
/*
        List<Size> supported = Size.convert(mCameraCapabilities
                .getSupportedPhotoSizes());
        CameraPictureSizesCacher.updateSizesForCamera(
                mAppController.getAndroidContext(),
                mCameraDevice.getCameraId(), supported);
*/
        List<Size> supported = Size.convert(mCameraCapabilities
                .getSupportedPhotoSizes());

        Size pictureSize;

        pictureSize = supported.get(0);
        Log.d(TAG, "Picture size is " + pictureSize);
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

}
