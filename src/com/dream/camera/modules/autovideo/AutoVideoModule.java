
package com.dream.camera.modules.autovideo;

import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import com.android.camera.ButtonManager;
import com.android.camera.app.AppController;
import com.android.camera.CameraActivity;
import com.android.camera.VideoUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.ui.SmallAdvancedFilterVideo;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;

import com.android.camera.VideoModule;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.ucam.utils.UiUtils;

import java.io.IOException;

public class AutoVideoModule extends VideoModule {

    public SmallAdvancedFilterVideo mSmallAdvancedFilterVideo;
    public String mMemory = "NONE";
    public AutoVideoModule(AppController app) {
        super(app);
    }
    public final ButtonManager.ButtonCallback mFilterVideoCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateFilterVideoDisplay();
            updateMakeUpDisplay();
        }
    };
    @Override
    protected void updateFilterVideoDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
            return;
        }
        ((AutoVideoUI)mUI).updateFilterVideoUI(mDataModuleCurrent.getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY));
        if(!mDataModuleCurrent.getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)){
           mMemory = mDataModuleCurrent.getString(Keys.KEY_VIDEO_COLOR_EFFECT);
           mSmallAdvancedFilterVideo.setFilterType("NONE");
        }else{
           mSmallAdvancedFilterVideo.setDreamFilterType(getConvertedtype(mMemory));
        }
        if(mDataModuleCurrent.getString(Keys.KEY_VIDEO_COLOR_EFFECT) == "NONE") {
           mSmallAdvancedFilterVideo.resetDreamFilterType();
        }
    }
    @Override
    public VideoUI createUI(CameraActivity activity) {
        AutoVideoUI mUI = new AutoVideoUI(activity, this, activity.getModuleLayoutRoot());
        mSmallAdvancedFilterVideo = mUI.getSmallAdvancedFilterVideo();
        return mUI;
    }
    @Override
    public void onSurfaceTextureUpdated() {
        super.onSurfaceTextureUpdated();
        mSmallAdvancedFilterVideo.requestLayout();
    }
    @Override
    public boolean useNewApi() {
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity.getContentResolver());
    }

    @Override
    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
                     boolean isCaptureIntent) {
        UiUtils.initialize(activity);
        super.init(activity, isSecureCamera, isCaptureIntent);
    }
    @Override
    protected void startVideoRecording() {
        if(mDataModuleCurrent.getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
            ((AutoVideoUI) mUI).updateFilterVideoUI(false);
        }
        super.startVideoRecording();
    }
    @Override
    protected boolean stopVideoRecording(boolean shouldSaveVideo,boolean checkStorage) {
        if(mDataModuleCurrent.getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
            ((AutoVideoUI) mUI).updateFilterVideoUI(true);
        }
        return super.stopVideoRecording(shouldSaveVideo , checkStorage);
    }
    @Override
    public void onPreviewStartedAfter() {
        ((AutoVideoUI)mUI).updateFilterVideoUI(mDataModuleCurrent.getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY));
        getmHandler().post(new Runnable(){
                    @Override
                    public void run(){
                        mSmallAdvancedFilterVideo.setDreamFilterType(getConvertedtype());
                    }

                });
    }
    public int getConvertedtype() {
        String type = mDataModuleCurrent.getString(Keys.KEY_VIDEO_COLOR_EFFECT);
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
    public int getConvertedtype(String type) {
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
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEO_COLOR_EFFECT)) {
            return;
        }
        if (isCameraOpening()) {
            return;
        }
        CameraCapabilities.Stringifier stringifier = mCameraCapabilities.getStringifier();
        String sColorEffect = mDataModuleCurrent.getString(Keys.KEY_VIDEO_COLOR_EFFECT);
        if (sColorEffect == null) {
            return;
        }
        CameraCapabilities.ColorEffect colorEffect = stringifier
                .colorEffectFromString(sColorEffect);
        mCameraSettings.setColorEffect(colorEffect);
        mSmallAdvancedFilterVideo.setDreamFilterType(getConvertedtype(sColorEffect));
    }
}
