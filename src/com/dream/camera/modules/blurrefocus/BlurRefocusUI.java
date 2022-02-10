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
import com.dream.camera.util.DreamUtil;
import com.dream.camera.SlidePanelManager;

import android.hardware.Camera.Face;
import com.android.ex.camera2.portability.CameraAgent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class BlurRefocusUI extends PhotoUI {

    private static final Log.Tag TAG = new Log.Tag("BlurRefocusUI");
    protected View topPanel;

    public BlurRefocusUI(CameraActivity activity, PhotoController controller, View parent//,
                     /*MakeupController.MakeupInterface makeupInterface*/) {
        super(activity, controller, parent/*, makeupInterface*/);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        // TODO Auto-generated method stub
        /**
         * sprd add refocus button to top panel start
         */
      /*
        topPanelParent.removeAllViews();
        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            topPanel = lf.inflate(R.layout.blurreofucsphoto_top_makeup_panel, topPanelParent);
        }
        mActivity.getButtonManager().load(topPanel);
        //bindMakeupButton();

        //mSettingsButton = (ImageButton) topPanel.findViewById(R.id.settings_button_dream);
        //bindSettingsButton(mSettingsButton);
        //bindFlashButton();
        bindCameraButton();
  */
        /**
         * sprd add refocus button to top panel end
         */
        if (mBasicModule.isBeautyCanBeUsed()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.autophoto_top_makeup_montionphoto_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.autophoto_top_montionphoto_panel,
                        topPanelParent);
            }
        }
        mActivity.getButtonManager().load(topPanel);
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();;
        }
        bindFlashButton();
        //bindCountDownButton();
        bindRefocusButton();
        bindFilterButton();
        bindHdrButton();
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
        MultiToggleImageButton makeupButton = (MultiToggleImageButton)topPanel.findViewById(R.id.make_up_display_toggle_button_dream);
        MultiToggleImageButton filterButton = (MultiToggleImageButton)topPanel.findViewById(R.id.filter_toggle_button_dream);
        if (flasButton != null) {
            flasButton.setEnabled(false);
            flasButton.setState(0);
        }
        if (filterButton != null) {
            filterButton.setEnabled(false);
            filterButton.setState(0);
        }
        if (!CameraUtil.isHdrBlurSupported()) {
            if (hdrButton != null) {
                hdrButton.setEnabled(false);
                hdrButton.setState(0);
            }
        } else {
            if (!mBasicModule.isAutoHdrSupported() && mBasicModule.isAutoHdr()) {
                hdrButton.setState(0);
            }
        }
        if (makeupButton != null) {
            makeupButton.setEnabled(false);
            makeupButton.setState(0);
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
        // TODO Auto-generated method stub
        if (CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_8) {
            return;
        } else {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            lf.inflate(R.layout.blur_refocus_photo_extend_panel, extendPanelParent);
            initBlurRefocusControl(extendPanelParent);
        }
    }
    protected BlurRefocusController mBlurRefocusController;
    public void initBlurRefocusControl(View extendPanelParent) {
        Log.i(TAG, "initBlurRefocusControl this = " + this);
        Log.i(TAG, "initBlurRefocusControl mController = " + mController);
        BlurRefocusModule blurRefocusModule = (BlurRefocusModule)mController;
        mBlurRefocusController = new BlurRefocusController(extendPanelParent,
                blurRefocusModule,
                Keys.KEY_BLUR_REFOCUS_LEVEL, 4);
    }

    public void resetBlurSeekBar() {
        if (mBlurRefocusController != null) {
            mBlurRefocusController.resumeFNumberControllerView();
        }
    }

    public void onPause() {
        super.onPause();
        if (mBlurRefocusController != null) {
            mBlurRefocusController.resetFNumberControllerView();
        }
    }
    @Override
    public void onPreviewStarted() {
        if (mBlurRefocusController != null) {
            Log.i(TAG, "To resumeFNNumberControllerView");
            mBlurRefocusController.resumeFNumberControllerView();
        }
        updateButtonState();
        super.onPreviewStarted();
    }

    public void setFNumberEnable(boolean enable){
        mBlurRefocusController.setFNumberEnable(enable);
    }
}
