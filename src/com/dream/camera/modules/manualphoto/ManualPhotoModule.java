
package com.dream.camera.modules.manualphoto;

import android.view.MotionEvent;

import com.android.camera.app.AppController;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;

import com.android.camera.PhotoModule;
import com.dream.camera.ButtonManagerDream;
import com.android.camera2.R;
import com.android.camera.util.CameraUtil;
import com.dream.camera.ui.ManualAeAfPanel;
import com.android.camera.debug.Log;

public class ManualPhotoModule extends PhotoModule implements ManualAeAfPanel.Listener {

    private static final Log.Tag TAG = new Log.Tag("ManualPhotoModule");
    private ManualAeAfPanel mAEAFPanel;

    public ManualPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        return new ManualPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    @Override
    public boolean isSupportTouchEV() {
        return false;
    }

    @Override
    public boolean isManualMode() {

        return CameraUtil.isAEAFSeparateEnable() ? true : false;
    }

    @Override
    public void resume() {
        mAEAFPanel = mActivity.getModuleLayoutRoot().findViewById(R.id.manual_ae_af_panel);
        if(mAEAFPanel != null) {
            mAEAFPanel.setActive(true);
            mAEAFPanel.setGestureListener(mUI.getGestureListener());
            mAEAFPanel.setManualListener(this);
        }
        super.resume();
    }

    @Override
    public void pause() {
        if(mAEAFPanel != null) {
            mAEAFPanel.setActive(false);
            mAEAFPanel.setGestureListener(null);
            mAEAFPanel.setManualListener(null);
        }
        super.pause();
    }

    /* SPRD:fix bug1404813 add autofocus when reset to auto @{ */
    @Override
    public void updateParametersFocusDistanceByManual() {
        updateParametersFocusDistance();
        if (isManualFocusEnabled() && !isFocusModeFixed() && mFocusManager != null && mFocusManager.isAEAFDraging()) {
            if (mCameraSettings != null && mCameraDevice != null) {
                mCameraDevice.applySettings(mCameraSettings);//SPRD:fix bug1414036
            }
            mFocusManager.autoFocus();
        }
    }
    /* @} */

    @Override
    public void manualautoFocus(int x, int y) {
        Log.i(TAG, " manualautoFocus");
        if (mCameraSettings != null) {
            mFocusManager.initializeFocusAreas(x, y);
            setFocusAreasIfSupported();
        }
        if (isFocusModeFixed() || isShutterClicked()) return;//SPRD:fix bug1404303
        if (mCameraSettings != null && mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
            if (mFocusManager.isInStateFocusing()) {
                mCameraDevice.cancelAutoFocus();
            }
            mFocusManager.autoFocus();
        }
    }

    @Override
    public void manualAEMetering(int x, int y) {
        Log.i(TAG, " manualAEMetering, (" + x + "," + y + ")");
        if (isShutterClicked()) return;
        mFocusManager.initializeMeteringAreas(x, y);
        if (mCameraSettings != null && mCameraDevice != null) {
            setMeteringAreasIfSupported();
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    public boolean isSupportManualMetering() {
        return true;
    }
    @Override
    public void onLongPress(MotionEvent var1){}
}
