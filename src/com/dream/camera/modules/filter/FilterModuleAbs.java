package com.dream.camera.modules.filter;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI.PanelsVisibilityListener;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.camera.PhotoModule;
import com.dream.camera.ucam.utils.UiUtils;
import com.android.camera2.R;

abstract public class FilterModuleAbs extends PhotoModule implements
        DreamFilterLogicControlInterface {
    private static final Tag TAG = new Tag("FilterModuleAbs");

    private static final int NO_DEVICE_AVAILABLE = -1;

    // ********* follow part need to be override ***************
    abstract public Bitmap getPreviewBitmap();

    abstract public PhotoUI createUI(CameraActivity activity);

    abstract public void onPreviewStartedAfter();

    abstract public int getPPEffectType(int filterType);

    abstract protected boolean checkPreviewPreconditions();

    abstract public void updateFilterPanelUI(int visible);

    @Override
    public void pause() {
        // SPRD: nj dream camera debug 117
        // bug626090
        if (mDataModuleCurrent != null) {
            mDataModuleCurrent.destroy();
        }
        if (mActivity.getCameraAppUI() != null) {
            mActivity.getCameraAppUI().setPanelsVisibilityListener(null);
            mActivity.getCameraAppUI().resumeTextureViewRendering();
        }
        super.pause();
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, true);

        // SPRD: fix bug 681226 Take photo with FilterModule take time longer
        // than compared mobile
        getServices().getMediaSaver().setFilterListener(null);
    }

    // ********************** public function part **********************
    public FilterModuleAbs(AppController app) {
        super(app);
    }

    protected void restartPreview(boolean freezeScreen) {
        if(mAELockPanel != null){
            mAELockPanel.hide();
        }

        if(isAELock){
            Log.d(TAG,"restart and reset ae/af lock");
            setAELock(false);
            if(mFocusManager != null) {
                mFocusManager.resetTouchFocus();
            }
            cancelAutoFocus();
        }
        stopPreview();
        if (freezeScreen) {
            freezeScreen(CameraUtil.isFreezeBlurEnable(), false);
        }
        mHandler.post(new Runnable() {
            public void run() {
                if (!mPaused && mCameraDevice != null) {
                    startPreview(false);
                }
            }
        });
        /* @ } */
    }

    public void StartPreveiwForFilter(SurfaceTexture surface, boolean optimize) {
        Log.i(TAG, "StartPreveiwForFilter start!");
        if (mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            // do nothing
            return;
        }

        if (surface == null) {
            return;
        }
        /* SPRD: add for bug 380597: switch camera preview has a frame error @{ */
        // mActivity.getCameraAppUI().resetPreview();
        /* @} */
        setDisplayOrientation();

        if (!mSnapshotOnIdle) {
            // If the focus mode is continuous autofocus, call cancelAutoFocus
            // to resume it because it may have been paused by autoFocus call.
            if (mFocusManager.getFocusMode(
                    mCameraSettings.getCurrentFocusMode(),
                    mDataModule.getString(Keys.KEY_FOCUS_MODE)) == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                mCameraDevice.cancelAutoFocus();
            }
            mFocusManager.setAeAwbLock(false); // Unlock AE and AWB.
        }

        // Nexus 4 must have picture size set to > 640x480 before other
        // parameters are set in setCameraParameters, b/18227551. This call to
        // updateParametersPictureSize should occur before setCameraParameters
        // to address the issue.
        updateParametersPictureSize();

        updateSettingAfterOpencamera(true);

        if (optimize) {
            mCameraDevice.setPreviewTexture(surface);
        } else {
            mCameraDevice.setPreviewTextureWithoutOptimize(surface);
        }

        Log.i(TAG, "startPreview");

        CameraAgent.CameraStartPreviewCallback startPreviewCallback = new CameraAgent.CameraStartPreviewCallback() {
            @Override
            public void onPreviewStarted() {
                mFocusManager.onPreviewStarted();
                FilterModuleAbs.this.onPreviewStarted();
                if (mSnapshotOnIdle) {
                    mHandler.post(mDoSnapRunnable);
                }
            }
        };

        // mCameraDevice.startPreview();
        // startPreviewCallback.onPreviewStarted();
        doStartPreview(startPreviewCallback, mCameraDevice);
        mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
        setCameraState(IDLE);// SPRD:fix bug660012 fix according to videomodule
        Log.i(TAG, "startPreview end!");
    }

    @Override
    public void onPreviewUIReadyForFilter() {
        Log.i(TAG, "onPreviewUIReadyForFilter");
        mHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (!mPaused && mCameraState == PREVIEW_STOPPED) {
                    startPreview(true);
                }
            }
        });
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
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
    public int getModuleTpye() {
        return FILTER_MODULE;
    }

    public void hideFaceForSpecialFilter() {
        if (!mAppController
                .getAndroidContext()
                .getResources()
                .getString(R.string.pref_ai_detect_entry_value_face)
                .equals(mDataModuleCurrent.getString(Keys.KEY_CAMERA_FACE_DATECT))) {
            return;
        }
        int mFilterType = mDataModuleCurrent
                .getInt(Keys.KEY_CAMERA_FILTER_TYPE);
        Log.e(TAG, "mFilterType  = " + mFilterType);
          startFaceDetection();
    }

    /* SPRD: fix bug549564 CameraProxy uses the wrong API @{ */
    @Override
    public boolean checkCameraProxy() {
        return (getCameraProvider().isNewApi())
                && (mCameraId == getCameraProvider().getCurrentCameraId()
                .getLegacyValue());
    }
    /* @} */

    @Override
    protected void switchCamera() {
    /* SPRD: Fix bug 597195 the freeze screen for switch @{ */
        freezeScreen(CameraUtil.isFreezeBlurEnable(),
                CameraUtil.isSwitchAnimationEnable());
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (!mPaused) {
                    mActivity.switchFrontAndBackMode();
                }
            }
        });
    /* @} */
    }

    @Override
    public boolean useNewApi() {
        return true;
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
                     boolean isCaptureIntent) {
        UiUtils.initialize(activity);
        super.init(activity, isSecureCamera, isCaptureIntent);
    }

    @Override
    public void freezeScreen(boolean needBlur, boolean needSwitch) {
        // disable preview data callback to pause preview
        Bitmap freezeBitmap = null;
        freezeBitmap = getPreviewBitmap();
        if (needBlur || needSwitch) {
            freezeBitmap = CameraUtil.blurBitmap(
                    CameraUtil.computeScale(freezeBitmap, 0.2f),
                    (Context) mActivity);
        }

        if (needSwitch) {
            mAppController.getCameraAppUI().startSwitchAnimation(freezeBitmap);
        }
        // freezeScreen with the preview bitmap
        mAppController.freezeScreenUntilPreviewReady(freezeBitmap);

    }

    @Override
    public void resume() {
        super.resume();

        if (mActivity.getCameraAppUI() != null) {
            mActivity.getCameraAppUI().setPanelsVisibilityListener((PanelsVisibilityListener) mUI);
        }

        /*
         * SPRD: fix bug 681226 Take photo with FilterModule take time longer
         * than compared mobile @{
         */
        getServices().getMediaSaver().setFilterListener(
                new MediaSaver.FilterListener() {
                    @Override
                    public void onFilterThumbnailReady(final Bitmap thumbnail) {
                        if (thumbnail != null && mActivity != null
                                && !mActivity.isDestroyed()) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /*
                                     * SPRD:fix bug688399 filter bitmap will use
                                    * after recycle @{
                                     */
                                    if (!mPaused) {
                                        mActivity.startPeekAnimation(thumbnail);
                                    }
                                    /* @} */
                                }
                            });

                        }
                    }
                });
        /* @} */
    }

    /* SPRD:fix bug605522 filter mode will normal preview when back camera @{ */
    @Override
    public void destroy() {
        super.destroy();// modify for oom
        if (mUI != null) {
            ((FilterModuleUIAbs) mUI).destroy();
        }
    }

    @Override
    public void updateFilterType() {
        int filterType = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_FILTER_TYPE);
        int mPPEffectType = getPPEffectType(filterType);
        Log.i(TAG, "updateFilterType filterType = " + filterType
                + " mPPEffectType = " + mPPEffectType);
        mCameraSettings.setFilterType(mPPEffectType);
    }
}
