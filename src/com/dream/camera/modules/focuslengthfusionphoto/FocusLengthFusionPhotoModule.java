
package com.dream.camera.modules.focuslengthfusion;

import com.android.camera.app.AppController;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera2.R;

import com.dream.camera.DreamModule;
import com.android.camera.PhotoModule;

import android.location.Location;
import android.net.Uri;
import android.view.Surface;
import android.view.View;
import android.view.ViewStub;

import android.media.MediaActionSound;

import com.android.ex.camera2.portability.Size;
import com.dream.camera.ButtonManagerDream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class FocusLengthFusionPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("FocusLengthFusionPhotoModule");

    public FocusLengthFusionPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        return new FocusLengthFusionPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    @Override
    protected void requestCameraOpen() {
        mCameraId = CameraUtil.BACK_WT_FUSION_ID;
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
    }

    public boolean isSupportManualMetering() {
        return false;
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
    // SPRD:Fix bug 948896 add for ai scene detect enable
    protected void updateParametersAiSceneDetect(){

        if(!hasAiSceneSupported() || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_AI_SCENE_DATECT))
            return;
        if(!isCameraFrontFacing()){
            if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1 ){

                mCameraSettings.setCurrentAiSenceEnable(1);
                Log.i(TAG, "updateParametersAiSceneDetect : 1");

                // Bug 1014851 - send Device Orientation to HAL for AI Scene Detection
                sendDeviceOrientation();
            }else{
                mCameraSettings.setCurrentAiSenceEnable(0);
                Log.i(TAG, "updateParametersAiSceneDetect : 0");
            }
        } else if(isCameraFrontFacing()){

            if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1 ){

                mCameraSettings.setCurrentAiSenceEnable(2);
                Log.i(TAG, "updateParametersAiSceneDetect : 2");

                // Bug 1014851 - send Device Orientation to HAL for AI Scene Detection
                sendDeviceOrientation();
            }else{
                mCameraSettings.setCurrentAiSenceEnable(0);
                Log.i(TAG, "updateParametersAiSceneDetect : 0");
            }
        }
    }

    // Bug 1018708 - hide or show BeautyButton on topPanel
    protected void updateBeautyButton() {
        boolean on = (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1);
        mUI.showBeautyButton(!on);

    }

    @Override
    public int getModuleTpye() {
        return DreamModule.FUSION_PHOTO_MODULE;
    }

    @Override
    protected void updateFlashTopButton() {
        if(CameraUtil.isWPlusTEnable()){
            if(!CameraUtil.isWPlusTAbility(mActivity.getApplicationContext(),true)){
                ((ButtonManagerDream)mActivity.getButtonManager()).enableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
                mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FLASH_DREAM,View.VISIBLE);
                return;
            }
            ((ButtonManagerDream)mActivity.getButtonManager()).disableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
        }else{
            super.updateFlashTopButton();
        }
    }
}
