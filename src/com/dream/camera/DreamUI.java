
package com.dream.camera;

import android.graphics.Bitmap;

import com.android.camera.CameraActivity;
import com.android.camera.settings.Keys;
import com.android.camera.ui.RotateImageView;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.ui.AdjustFlashPanel.AdjustFlashPanelInterface;
import com.dream.camera.util.DreamUtil;
import android.view.View;

public abstract class DreamUI implements AdjustFlashPanelInterface{

    public final static int UNDEFINED = -1;
    public final static int DREAM_WIDEANGLEPANORAMA_UI = 1;
    public final static int DREAM_FILTER_UI = 2;
    public final static int DREAM_REFOCUS_UI = 3;

    private int mTopPanelValue = -1;

    public int getUITpye() {
        return UNDEFINED;
    }

    public void showPanels() {
    }

    public void hidePanels() {
    }

    public void onThumbnail(final Bitmap thumbmail) {
    }

    public void adjustUI(int orientation) {
    }

    public void onCloseModeListOrSettingLayout() {
    }

    public boolean isFreezeFrameShow() {
        return false;
    }

    public boolean isReviewImageShow() {
        return false;
    }

    public void updateTopPanelValue(CameraActivity mActivity) {
        DreamUtil dreamUtil = new DreamUtil();
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            mTopPanelValue = DreamUtil.BACK_CAMERA;
        } else {
            mTopPanelValue = DreamUtil.FRONT_CAMERA;
        }
    }

    public int getTopPanelValue() {
        return mTopPanelValue;
    }

    public boolean isSupportSettings() {
        return true;
    }

    public void onDestroy() {
    }

    public void updateAiSceneView(RotateImageView view , int visible , int type) {
    }

    public int getAiSceneTip() {
        return -1;
    }

    public boolean isCountingDown() {
        return false;
    }

    public void onSingleTapUp() {}

    @Override
    public void initializeadjustFlashPanel(int maxValue, int minvalue, int currentValue) {
    }

    @Override
    public void showAdjustFlashPanel() {
    }

    @Override
    public void hideAdjustFlashPanel() {
    }
}
