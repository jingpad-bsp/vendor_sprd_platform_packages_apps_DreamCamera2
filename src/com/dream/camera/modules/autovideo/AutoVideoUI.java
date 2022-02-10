
package com.dream.camera.modules.autovideo;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;

import com.android.camera.app.OrientationManager;
import com.android.camera.settings.Keys;
import com.android.camera.ui.SmallAdvancedFilterVideo;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.CameraActivity;
import com.android.camera.VideoController;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamOrientation;
import com.dream.camera.MakeupController;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.MakeupController.MakeupListener;
import com.android.camera.VideoUI;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.util.DreamUtil;

public class AutoVideoUI extends VideoUI {


    //private ImageButton mSettingsButton;
    private View topPanel;
    protected SmallAdvancedFilterVideo mSmallAdvancedFilterVideo;
    public AutoVideoUI(CameraActivity activity, VideoController controller, View parent) {
        super(activity, controller, parent);
        
        mSmallAdvancedFilterVideo = new SmallAdvancedFilterVideo(mActivity,
                (ViewGroup) mRootView);
        mSmallAdvancedFilterVideo.initvideo();
        mSmallAdvancedFilterVideo.setOrientation(mActivity.getCameraAppUI()
                .getNowOrientation());
    }
    public SmallAdvancedFilterVideo getSmallAdvancedFilterVideo() {
        return mSmallAdvancedFilterVideo;
    }
    @Override
    public void onPanelsHidden() {
        if (mSmallAdvancedFilterVideo != null) {
            mSmallAdvancedFilterVideo.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPanelsShown() {
        if (mSmallAdvancedFilterVideo != null) {
            mSmallAdvancedFilterVideo.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();
        DreamOrientation.setOrientation(mRootView, orientation, true);
        if (mSmallAdvancedFilterVideo != null)
            mSmallAdvancedFilterVideo.setOrientation(orientation);
    }
    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

        DreamUtil dreamUtil = new DreamUtil();
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {

            if(mController.isMakeUpEnable() && CameraUtil.isFilterVideoEnable()){
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_makeup_filtervideo_top_panel,
                            topPanelParent);
                }
            }else if(mController.isMakeUpEnable() && !CameraUtil.isFilterVideoEnable()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_makeup_top_panel,
                            topPanelParent);
                }
            }else if(!mController.isMakeUpEnable() && CameraUtil.isFilterVideoEnable()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_filtervideo_top_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_top_panel,
                            topPanelParent);
                }
            }
            mActivity.getButtonManager().load(topPanel);

        } else {

            if(mController.isMakeUpEnable() && CameraUtil.isFilterVideoEnable()){
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_makeup_filtervideo_front_top_panel,
                            topPanelParent);
                }
            } else if(mController.isMakeUpEnable() && !CameraUtil.isFilterVideoEnable()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_makeup_front_top_panel,
                            topPanelParent);
                }
            } else if(!mController.isMakeUpEnable() && CameraUtil.isFilterVideoEnable()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_filtervideo_front_top_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autovideo_top_panel,
                            topPanelParent);
                }
            }

            mActivity.getButtonManager().load(topPanel);
        }


//        mSettingsButton = (ImageButton) topPanel.findViewById(R.id.settings_button_dream);
//        bindSettingsButton(mSettingsButton);

        if(mController.isMakeUpEnable()){
            bindMakeUpDisplayButton();
        }
        bindFlashButton();
        if(CameraUtil.isFilterVideoEnable()) {
            bindFilterVideoButton();
        }
        bindCameraButton();
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if(mController.isMakeUpEnable()) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            View extendPanel = lf.inflate(R.layout.sprd_autophoto_extend_panel, extendPanelParent);
            new MakeupController(extendPanel, mController,mActivity);
        }
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
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
    public void onDestroy() {
        super.onDestroy();
        if (mSmallAdvancedFilterVideo != null)
            mSmallAdvancedFilterVideo.setVisibility(View.GONE);
    }
    @Override
    public void onPreviewStarted() {
        super.onPreviewStarted();
        mBasicModule.updateParametersColorEffect();
        mSmallAdvancedFilterVideo.requestLayout();
    }
    public void updateFilterVideoUI(boolean visible) {
        if(visible) {
            mSmallAdvancedFilterVideo.setVisibility(View.VISIBLE);
        }
        else{
            mSmallAdvancedFilterVideo.setVisibility(View.GONE);
        }
    }
    public void bindFilterVideoButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_FILTER_VIDEO_DREAM,
                ((AutoVideoModule)mBasicModule).mFilterVideoCallback);
    }
}
