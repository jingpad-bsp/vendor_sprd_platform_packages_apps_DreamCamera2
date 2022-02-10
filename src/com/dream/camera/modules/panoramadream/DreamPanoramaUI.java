
package com.dream.camera.modules.panoramadream;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.CameraActivity;
import com.android.camera.ui.RotateImageView;

import com.sprd.camera.panora.WideAnglePanoramaController;
import com.sprd.camera.panora.WideAnglePanoramaUI;

import com.android.camera2.R;

import com.dream.camera.dreambasemodules.DreamInterface;
import com.dream.camera.DreamUI;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.ButtonManagerDream;

public class DreamPanoramaUI extends WideAnglePanoramaUI implements DreamInterface {

    private View topPanel;

    public DreamPanoramaUI(CameraActivity activity, WideAnglePanoramaController controller,
            ViewGroup root) {
        super(activity, controller, root);
        activity.getCameraAppUI().setDreamInterface(this);
    }

    @Override
    public void initUI() {

        // Generate a view to fit top panel.
        ViewGroup topPanelParent = (ViewGroup) mActivity.getModuleLayoutRoot().findViewById(
                R.id.top_panel_parent);
        topPanelParent.removeAllViews();
        updateTopPanelValue(mActivity);
        fitTopPanel(topPanelParent);

        // Generate views to fit extend panel.
        ViewGroup extendPanelParent = (ViewGroup) mActivity.getModuleLayoutRoot().findViewById(
                R.id.extend_panel_parent);
        extendPanelParent.removeAllViews();
        fitExtendPanel(extendPanelParent);

        // Update icons on bottom panel.
        updateBottomPanel();

        // Update item on slide panel.
        updateSlidePanel();
        mActivity.getCameraAppUI().updateButtomBar();
        mActivity.getCameraAppUI().addShutterListener();
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            topPanel = lf.inflate(R.layout.panorama_top_panel,
                    topPanelParent);
        }

        mActivity.getButtonManager().load(topPanel);
        bindCameraButton();
        bindDirectorButton();
    }

    public void bindCameraButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_CAMERA_DREAM,
                mBasicModule.mCameraCallback);
    }

    public void bindDirectorButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_DIRECTOR_DREAM,null);
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {

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

    /* Dream Camera ui check 41*/
    public void adjustUI(int orientation) {
        super.adjustUIDreamBefore(orientation);
    }

    public void changeOtherUIVisible(Boolean shuttering, int visible) {
        mActivity.getCameraAppUI().setBottomBarLeftAndRightUI(visible);
        mActivity.getCameraAppUI().updateSlidePanelUI(visible);
    }

    public void changeSomethingUIVisible(Boolean shuttering, int visible) {
        mActivity.getCameraAppUI().updateSlidePanelUI(visible);
        mActivity.getCameraAppUI().setBottomBarLeftAndRightUI(visible);
    }

    public void changeMosaicSideAndSlideVisible(Boolean shuttering, int visible) {
        mActivity.getCameraAppUI().updateSlidePanelUI(visible);
    }

    @Override
    public int getUITpye() {
        return DreamUI.DREAM_WIDEANGLEPANORAMA_UI;
    }


    @Override
    public boolean isSupportSettings() {
        return false;
    }

    //Sprd:Fix bug951575
    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int type) {
        if (view != null)
            view.setVisibility(View.GONE);
    }
}
