
package com.dream.camera.modules.macrovideo;

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

public class MacroVideoUI extends VideoUI {

    private View topPanel;

    public MacroVideoUI(CameraActivity activity, VideoController controller, View parent) {
        super(activity, controller, parent);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

//        if(mController.isMakeUpEnable()){
//            if (topPanel == null) {
//                LayoutInflater lf = LayoutInflater.from(mActivity);
//                topPanel = lf.inflate(R.layout.macro_video_top_makeup_panel,
//                        topPanelParent);
//            }
//        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.macro_video_top_panel,
                        topPanelParent);
            }
//        }

        mActivity.getButtonManager().load(topPanel);
        //bindSettingsButton(mSettingsButton);

        bindCameraButton();
        bindFlashButton(); //SPRD:fix bug 1227018

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
