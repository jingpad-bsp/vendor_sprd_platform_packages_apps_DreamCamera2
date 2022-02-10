
package com.dream.camera.modules.filter;

import android.os.Handler;
import com.android.camera.CameraActivity;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeTransitionView;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class DreamFilterModuleController {
    private static final Log.Tag TAG = new Log.Tag("DreamFilterModuleController");
    private AppController mAppController;
    private final static int SWIPE_LEFT = 3;
    private final static int SWIPE_RIGHT = 4;
    private int mPhotoIndex;
    private int mFilterIndex;
    private Bitmap mScreenShot;
    private final ModeTransitionView mModeTransitionView;
    private final MainActivityLayout mAppRootView;
    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    private static final String TARGET_CAMERA_UNFREEZE_FRAME_COUNT = "persist.sys.cam.unfreeze_cnt";
    private final int MAX_FRAME_COUNT = android.os.SystemProperties.getInt(
            TARGET_CAMERA_UNFREEZE_FRAME_COUNT, 0);
    // Fix bug 585183 Adds new feature real-time distance measurement
    private static final int MAX_FRAME_COUNT_FOR_FILTER = 2;
    public static final int MAX_FRAME_COUNT_FOR_FILTER_ARC = 2;
    private int mFrameCount = 0;
    /* @} */
    private FilterModuleAbs mFilterModule;
    private CameraAppUI mCameraAppUI;

    public DreamFilterModuleController(AppController controller, MainActivityLayout appRootView) {
        mAppController = controller;
        mAppRootView = appRootView;
        mPhotoIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        mFilterIndex = SettingsScopeNamespaces.FILTER;
        mModeTransitionView = (ModeTransitionView) mAppRootView
                .findViewById(R.id.mode_transition_view);
    }

    public boolean switchMode(int swipeState) {
        if (swipeState == SWIPE_LEFT && mAppController != null
                && mAppController.getCurrentModuleIndex() == mPhotoIndex) {
            mAppController.freezeScreenUntilPreviewReady();
            mAppController.onModeSelected(mFilterIndex);
            return true;
        } else if (swipeState == SWIPE_RIGHT && mAppController != null
                && mAppController.getCurrentModuleIndex() == mFilterIndex) {
                mFilterModule = (FilterModuleAbs)((CameraActivity)mAppController).getCurrentModule();
                /* SPRD: Fix bug 597195 the freeze screen for switch mode @{ */
                //mFilterModule.delayUiPause();
                mFilterModule.freezeScreen(CameraUtil.isFreezeBlurEnable(), false);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mAppController.isPaused()){
                            mAppController.onModeSelected(mPhotoIndex);
                        }
                    }
                });
                /* @} */
            return true;
        }
        return false;
    }

    private void setScreenShot() {
        if (mFilterModule != null) {
            // disable preview data callback to pause preview
           // mFilterModule.setPreviewDataCallback(false);
            Bitmap preview = mFilterModule.getPreviewBitmap();
            mScreenShot = Bitmap.createBitmap(getCameraAppUI().getModuleRootView().getWidth(),getCameraAppUI().getModuleRootView().getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mScreenShot);
            canvas.drawARGB(255, 0, 0, 0);
            if (preview != null) {
                canvas.drawBitmap(preview, null,
                        getCameraAppUI().getPreviewArea(), null);
                preview.recycle();
            }
            Bitmap overlay = getCameraAppUI().getPreviewOverlayAndControls();
            if (overlay != null) {
                canvas.drawBitmap(overlay, 0f, 0f, null);
                overlay.recycle();
            }
        }
    }

    private void freezeScreen(Bitmap screenShot) {
        // SPRD: nj dream camera monkey test
        if (screenShot != null) {
            Log.i(TAG,"freezeScreen");
            mModeTransitionView.showImageCover(screenShot);
        }
        resetFrameCount();
    }

    public void checkFrameCount() {
        //Sprd:Fix bug672824
        if (mAppController == null || (mAppController != null && mAppController.isPaused())){
            Log.i(TAG, "Camera has been paused, return");
            return;
        }
        int maxCount = MAX_FRAME_COUNT;
        /* SPRD: Fix bug 585183 Adds new feature real-time distance measurement @{ */
        if (mAppController.getCurrentModuleIndex() == mFilterIndex) {
                maxCount = MAX_FRAME_COUNT_FOR_FILTER_ARC;
                Log.i(TAG, "checkFrameCount maxCount="+maxCount);
        }
        /* @} */
        Log.i(TAG, "checkFrameCount mFrameCount="+mFrameCount);
        if (mFrameCount < maxCount) {
            mFrameCount++;
        } else if (mFrameCount == maxCount) {
            mFrameCount++;
            Log.i(TAG,"hideImageCover");
            // TODO if you want setup mode Cover in filter Module
            // you should notice this palace
            // mModeTransitionView.hideImageCover();
            final CameraAppUI cameraAppUI = getCameraAppUI();
            cameraAppUI.hideModeCover();
            cameraAppUI.showCurrentModuleTip(mAppController.getCurrentModuleIndex());
            /*SPRD:fix bug612383 filter not need textureview @ {*/
            if ((mAppController != null && mAppController.getCurrentModuleIndex() == mFilterIndex)) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mAppController.isPaused()){
                            cameraAppUI.onSurfaceTextureUpdated(cameraAppUI.getSurfaceTexture());
                        }
                    }
                });
                cameraAppUI.pauseTextureViewRendering();
            }
            /* @} */
        }

    }

    public void resetFrameCount() {
        mFrameCount = 0;
    }

    public CameraAppUI getCameraAppUI() {
        if (mCameraAppUI == null) {
            mCameraAppUI = mAppController.getCameraAppUI();
        }
        return mCameraAppUI;
    }

    /* SPRD: nj dream camera test debug 120 @{ */
    public void updateFilterPanelUI(int visible) {
        if (mAppController != null
                && mAppController.getCurrentModuleIndex() == mFilterIndex) {
            ((DreamFilterLogicControlInterface) ((CameraActivity) mAppController)
                    .getCurrentModule()).updateFilterPanelUI(visible);
        }
    }
    /* @} */

    // set a larger value to avoid CameraAppUI.onSurfaceTextureUpdated()
    // calling hideImageCover() without preview started
    public void maximizeFrameCount() {
        mFrameCount = (MAX_FRAME_COUNT < MAX_FRAME_COUNT_FOR_FILTER)
                ? MAX_FRAME_COUNT_FOR_FILTER + 1 : MAX_FRAME_COUNT + 1;
    }
}
