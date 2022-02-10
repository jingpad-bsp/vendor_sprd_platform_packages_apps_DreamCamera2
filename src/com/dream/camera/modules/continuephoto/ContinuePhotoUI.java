package com.dream.camera.modules.continuephoto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;

import com.android.camera2.R;

import com.android.camera.PhotoUI;
import android.widget.TextView;

public class ContinuePhotoUI extends PhotoUI {
    private View topPanel;

    private TextView mCaptureAlready;
    private TextView mCaptureTotal;
    private View mExtendPanel;

    public ContinuePhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            topPanel = lf.inflate(R.layout.continuephoto_top_panel,
                    topPanelParent);
        }

        mActivity.getButtonManager().load(topPanel);
        bindCameraButton();
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if (extendPanelParent != null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            lf.inflate(R.layout.continuephoto_extend_panel,
                    extendPanelParent);

            mCaptureAlready = (TextView) extendPanelParent
                    .findViewById(R.id.capture_already);
            mCaptureTotal = (TextView) extendPanelParent
                    .findViewById(R.id.capture_total);
            mCaptureTotal.setText(" / " + ContinuePhotoModule.MAX_BURST_COUNT);
            mExtendPanel = (View) extendPanelParent
                    .findViewById(R.id.continuephoto_extend_panel);
        }
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        super.updateSlidePanel();
    }

    /**
     * update capture already ui
     */
    public void updateCaptureUI(String captureAlready) {
        if (mCaptureAlready != null) {
            mCaptureAlready.setText(captureAlready);
        }
    }

    public void changeExtendPanelUI(int visibility) {
        if (mExtendPanel == null) {
            return;
        }
        mExtendPanel.setVisibility(visibility);
    }

    public void changeOtherUIVisible(Boolean bursting, int visible) {
        mActivity.getCameraAppUI().setBursting(bursting);
        mActivity.getCameraAppUI().updateTopPanelUI(visible);
        mActivity.getCameraAppUI().setBottomBarLeftAndRightUI(visible);
        mActivity.getCameraAppUI().updateSlidePanelUI(visible);
    }

    public boolean getExtendPanelVisibility() {
        return (mExtendPanel != null) && View.VISIBLE == mExtendPanel.getVisibility();
    }
}
