
package com.dream.camera.modules.qr;

import android.view.View;
import android.view.ViewGroup;
import com.android.camera.ButtonManager;
import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.ui.RotateImageView;
import com.android.camera2.R;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamOrientation;
import com.dream.camera.dreambasemodules.DreamInterface;
import com.android.camera.PhotoUI;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.modules.qr.ReuseModule;
import com.dream.camera.modules.qr.QrCodePhotoModule;
import android.hardware.Camera;

public  abstract class ReusePhotoUI implements DreamInterface {

    private static final Log.Tag TAG = new Log.Tag("ReusePhotoUI");
    protected ReuseModule mBasicModule;
    protected CameraActivity mActivity;
    protected final ReuseController mController;
    protected final View mRootView;
    protected boolean isClickEnabled = true;

    public ReusePhotoUI(CameraActivity activity, ReuseController controller,
            View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        initIndicators();
        initUI();
        activity.getCameraAppUI().setDreamInterface(this);
    }


    public void initUI() {

        mBasicModule = (ReuseModule) mController;

        // Generate a view to fit top panel.
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        topPanelParent.removeAllViews();
        fitTopPanel(topPanelParent);

        // Generate views to fit extend panel.
        ViewGroup extendPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.extend_panel_parent);
        extendPanelParent.removeAllViews();
        fitExtendPanel(extendPanelParent);

        // Update icons on bottom panel.
        //updateBottomPanel();

        // Update item on slide panel.
        updateSlidePanel();
        mActivity.getCameraAppUI().updateButtomBar();

    }

    @Override
    public void updateBottomPanel() {
        mActivity.getCameraAppUI().updateSwitchModeBtn(this);
    }

    @Override
    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS,View.INVISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(SlidePanelManager.CAPTURE, false);
    }

    public void setButtonOrientation(int orientation) {

    }

    public void bindFlashButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity.getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_GIF_PHOTO_FLASH_DREAM,
                mBasicModule.mFlashCallback);
    }

    public void bindCameraButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_CAMERA_DREAM,
                mBasicModule.mCameraCallback);
    }

    public void bindQrCodeButton(View qrCode) {
        if (qrCode != null) {
            qrCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isClickEnabled == true){
                        ((QrCodePhotoModule)mController).scanPicture();
                    }
                    isClickEnabled = false;
                }
            });
        }
    }

    // SPRD:fix bug 607184 gallery button is only clicked once
    public void updateGalleryButton(){
        isClickEnabled = true;
    }

    public ButtonManager.ButtonCallback getDisableCameraButtonCallback(
            final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mActivity.getButtonManager().disableButton(conflictingButton);
            }
        };
    }

    private void initIndicators() {
        // TODO init toggle buttons on bottom bar here
    }
    public void setDisplayOrientation(int orientation) {}
    public void onResume(){}
    public void onPause() {}
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int type) {
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public int getAiSceneTip() {
        return -1;
    }

    public void setButtonVisibility(int buttonId, int visibility) {}

    public void onOrientationChanged(int orientation) {
        DreamOrientation.setOrientation(mRootView, orientation, true);
    }
}
