
package com.dream.camera.modules.tdvideo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.camera.settings.Keys;
import com.android.camera2.R;
import com.android.camera.CameraActivity;
import com.android.camera.VideoController;
import com.dream.camera.MakeupController;
import com.android.camera.VideoUI;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.util.DreamUtil;

public class TDVideoUI extends VideoUI {

    private ImageButton mSettingsButton;
    private ImageButton mSwitchPreviewButton;
    private View topPanel;

    public TDVideoUI(CameraActivity activity, VideoController controller, View parent) {
        super(activity, controller, parent);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

        if(mController.isMakeUpEnable()){
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.td_video_top_makeup_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.td_video_top_panel,
                        topPanelParent);
            }
        }

        mActivity.getButtonManager().load(topPanel);
        //mSettingsButton = (ImageButton) topPanel.findViewById(R.id.settings_button_dream);
        mSwitchPreviewButton = (ImageButton) topPanel.findViewById(R.id.td_switch_preview_button);

        //bindSettingsButton(mSettingsButton);

        bindCameraButton();

        bindSwitchPreviewButton(mSwitchPreviewButton);

        if(mController.isMakeUpEnable()){
            bindMakeUpDisplayButton();
        }

    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {

        if (mController.isMakeUpEnable()) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                // mFreezeFrame = extendPanelParent;
                View extendPanel = lf.inflate(R.layout.video_extend_panel,
                        extendPanelParent);
                new MakeupController(extendPanel, mController,mActivity);
        }

    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

//    @Override
//    public void updateSlidePanel() {
//        super.updateSlidePanel();
//    }

}
