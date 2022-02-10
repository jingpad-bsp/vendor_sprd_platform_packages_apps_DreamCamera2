
package com.dream.camera.modules.portraitbackgroundreplacementphoto.sprd;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.View;
import android.view.MotionEvent;
import android.widget.TextView;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoModule;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.dream.camera.DreamModule;
import com.dream.camera.filter.ArcsoftSmallAdvancedFilter;
import com.dream.camera.filter.sprd.SprdGLSurfaceView;
import com.dream.camera.modules.filter.DreamFilterLogicControlInterface;
import com.dream.camera.modules.filter.FilterModuleAbs;
import com.dream.camera.modules.filter.FilterModuleUIAbs;
import com.dream.camera.modules.filter.FilterTranslationUtils;
import com.dream.camera.modules.filter.sprd.FilterModuleSprd;
import com.dream.camera.modules.filter.sprd.FilterModuleUISprd;
import com.dream.camera.portraitbackgroundreplacement.SmallAdvancedPortraitBackgroundReplacement;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.ucam.utils.UiUtils;
import com.dream.camera.util.DreamUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PortraitBackgroundReplacementPhotoModule extends PhotoModule{
    private static final Log.Tag TAG = new Log.Tag("PortraitBackgroundReplacementPhotoModule");
    private PortraitBackgroundReplacementPhotoUI mPortraitBackgroundReplacementUI;
    protected SmallAdvancedPortraitBackgroundReplacement mSmallAdvancedPortraitBackgroundReplacement;
    public PortraitBackgroundReplacementPhotoModule(AppController app) {
        super(app);
    }
    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        Log.d(TAG, "create ui");
        Log.d(TAG, "makeModuleUI E.");
        // initialize BaseUI object
        mPortraitBackgroundReplacementUI = new PortraitBackgroundReplacementPhotoUI(mActivity, this,
                mActivity.getModuleLayoutRoot());
        mSmallAdvancedPortraitBackgroundReplacement = mPortraitBackgroundReplacementUI.getSmallAdvancedPortraitBackgroundReplacement();

        return mPortraitBackgroundReplacementUI;
    }

    public boolean isSupportTouchAFAE() {
        return false;
    }
    @Override
    public void onSingleTapUp(View view, int x, int y) { }
    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent var1){}

    @Override
    public void destroy() {
        super.destroy();// modify for oom
        if (mUI != null) {
            ((PortraitBackgroundReplacementPhotoUI) mUI).destroy();
        }
    }

    @Override
    public void updatePortraitBackgroundReplacementType() {
        int portraitbackgroundreplacementType = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        Log.i(TAG, "updateportraitbackgroundreplacementType portraitbackgroundreplacementType = " + portraitbackgroundreplacementType);
        if(mCameraDevice != null && mCameraSettings != null) {
            mCameraSettings.setPortraitBackgroundReplacementType(portraitbackgroundreplacementType);
            mCameraDevice.applySettings(mCameraSettings);
        }
    }
    @Override
    public void onPreviewStartedAfter() {
        Log.i(TAG, "onPreviewStartedAfter");
        int type = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        mSmallAdvancedPortraitBackgroundReplacement.setDreamPortraitBackgroundReplacementType(type);

    }




    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
                     boolean isCaptureIntent) {
        UiUtils.initialize(activity);
        super.init(activity, isSecureCamera, isCaptureIntent);
    }

    @Override
    protected void requestCameraOpen() {
        mCameraId = DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(mCameraId)?CameraUtil.FRONT_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID
                :CameraUtil.BACK_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID;
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
    public int getModuleTpye(){
        return DreamModule.PORTRAITBACKGROUNDREPLACEMENT;
    }
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
