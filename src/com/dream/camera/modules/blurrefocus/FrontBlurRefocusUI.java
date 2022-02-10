package com.dream.camera.modules.blurrefocus;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import com.android.camera.debug.Log;
import com.android.camera2.R;
import com.android.camera.CameraActivity;
import com.android.camera.MultiToggleImageButton;
import com.android.camera.PhotoController;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.dream.camera.BlurRefocusController;
import com.android.camera.PhotoUI;
import com.dream.camera.SlidePanelManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.hardware.Camera.Face;
import com.android.ex.camera2.portability.CameraAgent;

public class FrontBlurRefocusUI extends PhotoUI {

    private static final Log.Tag TAG = new Log.Tag("FrontBlurRefocusUI");
    private View topPanel;

    public FrontBlurRefocusUI(CameraActivity activity, PhotoController controller, View parent//,
                     /*MakeupController.MakeupInterface makeupInterface*/) {
        super(activity, controller, parent/*, makeupInterface*/);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        // TODO Auto-generated method stub
        //topPanelParent.removeAllViews();
        /**
        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            if (CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_6 && CameraUtil.getCurrentSbsVersion() != CameraUtil.SBS_VERSION_OFF) {
                topPanel = lf.inflate(R.layout.frontblurreofucsphoto_top_panel, topPanelParent);
                mActivity.getButtonManager().load(topPanel);
            } else {
                topPanel = lf.inflate(R.layout.frontblurreofucsphoto_top_makeup_panel, topPanelParent);
                mActivity.getButtonManager().load(topPanel);
                bindMakeupButton();
            }
        }
        //bindFlashButton();
        bindCameraButton();
        **/

        if (mBasicModule.isBeautyCanBeUsed() && !isSbsVersion()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.autophoto_front_top_makeup_montionphoto_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.autophoto_front_top_montionphoto_panel,
                        topPanelParent);
            }
        }
        mActivity.getButtonManager().load(topPanel);
        if (mBasicModule.isBeautyCanBeUsed() && !isSbsVersion()) {
            bindMakeUpDisplayButton();
        }
        bindFlashButton();
        bindRefocusButton();
        bindHdrButton();
        bindFilterButton();
        bindCameraButton();
        bindMontionPhotoButton();
        updateButtonState();
    }
    private void updateButtonState(){
        if (topPanel == null)
            return;
        MultiToggleImageButton montionPhotoButton = (MultiToggleImageButton)topPanel.findViewById(R.id.montionphoto_toggle_button_dream);
        MultiToggleImageButton flasButton = (MultiToggleImageButton)topPanel.findViewById(R.id.flash_toggle_button_dream);
        MultiToggleImageButton hdrButton = (MultiToggleImageButton)topPanel.findViewById(R.id.hdr_toggle_button_dream);
        MultiToggleImageButton filterButton = (MultiToggleImageButton)topPanel.findViewById(R.id.filter_toggle_button_dream);
        if (flasButton != null) {
            flasButton.setEnabled(false);
            flasButton.setState(0);
        }
        if (filterButton != null) {
            filterButton.setEnabled(false);
            filterButton.setState(0);
        }
        if (hdrButton != null) {
            hdrButton.setEnabled(false);
            hdrButton.setState(0);
        }
        if(montionPhotoButton != null){
            montionPhotoButton.setEnabled(false);
            montionPhotoButton.setState(0);
        }
    }
    @Override
    public void updateSlidePanel() {
        if (!mActivity.isSecureCamera()) {
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.SETTINGS,View.VISIBLE);
            SlidePanelManager.getInstance(mActivity).focusItem(
                    SlidePanelManager.CAPTURE, false);
        } else {
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.MODE,View.INVISIBLE);
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.SETTINGS,View.VISIBLE);
            SlidePanelManager.getInstance(mActivity).focusItem(
                    SlidePanelManager.CAPTURE, false);
        }
    }

    @Override
    public void updateBottomPanel() {
        // TODO Auto-generated method stub
        super.updateBottomPanel();
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if(!isSbsVersion()){
            super.fitExtendPanel(extendPanelParent);
        }
    }

    private boolean isSbsVersion() {
        return (CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_7 || CameraUtil.getCurrentFrontBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_8);
    }
    @Override
    public void onPreviewStarted() {
        updateButtonState();
        super.onPreviewStarted();
    }
}
