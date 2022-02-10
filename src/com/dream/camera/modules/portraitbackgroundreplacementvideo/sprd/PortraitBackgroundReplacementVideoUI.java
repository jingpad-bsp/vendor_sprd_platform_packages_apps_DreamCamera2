package com.dream.camera.modules.portraitbackgroundreplacementvideo.sprd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;

import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera2.R;
import com.android.camera.CameraActivity;
import com.android.camera.VideoController;
import com.dream.camera.MakeupController;
import com.android.camera.VideoUI;
import com.dream.camera.portraitbackgroundreplacement.SmallAdvancedPortraitBackgroundReplacement;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.util.DreamUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class PortraitBackgroundReplacementVideoUI extends VideoUI  implements DataModuleManager.ResetListener,
        OrientationManager.OnOrientationChangeListener,
        CameraAppUI.PanelsVisibilityListener {
    private static final Log.Tag TAG = new Log.Tag("PortraitBackgroundReplacementVideoUI");
    private View topPanel;
    protected SmallAdvancedPortraitBackgroundReplacement mSmallAdvancedPortraitBackgroundReplacement;
    public PortraitBackgroundReplacementVideoUI(CameraActivity activity, VideoController controller, View parent) {
        super(activity, controller, parent);
        mActivity.getOrientationManager().addOnOrientationChangeListener(this);
        mActivity.getCameraAppUI().initAiSceneView();
        RelativeLayout.LayoutParams param = null;
        param = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        ViewStub viewStubMagiclensBottom = (ViewStub) mActivity
                .findViewById(R.id.layout_ucam_magiclens_bottom_id);
        if (viewStubMagiclensBottom != null) {
            viewStubMagiclensBottom.inflate();
        }
        mSmallAdvancedPortraitBackgroundReplacement = new SmallAdvancedPortraitBackgroundReplacement(mActivity,
                (ViewGroup) mRootView);
        mSmallAdvancedPortraitBackgroundReplacement.initvideo();
        mSmallAdvancedPortraitBackgroundReplacement.setVisibility(View.VISIBLE);
        mSmallAdvancedPortraitBackgroundReplacement.setOrientation(mActivity.getCameraAppUI()
                .getNowOrientation());
    }
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        super.onOrientationChanged(orientationManager, deviceOrientation);
        int orientation = 0;
        if (mActivity.getOrientationManager() != null) {
            orientation = mActivity.getOrientationManager()
                    .getDeviceOrientation().getDegrees();
        }

        if (mSmallAdvancedPortraitBackgroundReplacement != null)
            mSmallAdvancedPortraitBackgroundReplacement.setOrientation(orientation);
    }
    public SmallAdvancedPortraitBackgroundReplacement getSmallAdvancedPortraitBackgroundReplacement() {
        return mSmallAdvancedPortraitBackgroundReplacement;
    }
    @Override
    public void onSettingReset() {
    }
    @Override
    public void onPanelsHidden() {
        if (mSmallAdvancedPortraitBackgroundReplacement != null) {
            mSmallAdvancedPortraitBackgroundReplacement.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPanelsShown() {
        if (mSmallAdvancedPortraitBackgroundReplacement != null) {
            mSmallAdvancedPortraitBackgroundReplacement.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onResume() {
//        DataModuleManager.getInstance(mActivity).addListener(this);
        isPause.set(false);
        super.onResume();
    }

    @Override
    public void onPause() {
        isPause.set(true);
        showPortraitTip(false,-1);
        super.onPause();
        DataModuleManager.getInstance(mActivity).removeListener(this);

    }
    public void destroy() {
        if (mActivity != null && mActivity.getOrientationManager() != null) {
            mActivity.getOrientationManager().removeOnOrientationChangeListener(this);
        }
        if (mSmallAdvancedPortraitBackgroundReplacement != null)
            mSmallAdvancedPortraitBackgroundReplacement.setVisibility(View.GONE);
    }
//    @Override
//    public void updateUI(float aspectRation) {
//        super.updateUI(aspectRation);
//        if (mSmallAdvancedPortraitBackgroundReplacement != null)
//            mSmallAdvancedPortraitBackgroundReplacement.updateUI(aspectRation);
//    }
    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

        if (mController.isMakeUpEnable()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.portrait_background_replacement_video_makeup_top_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.portrait_background_replacement_video_top_panel,
                        topPanelParent);
            }
        }
        mActivity.getButtonManager().load(topPanel);
        //bindSettingsButton(mSettingsButton);

        bindCameraButton();

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

    @Override
    public void onPreviewStarted() {
        super.onPreviewStarted();
        mBasicModule.updatePortraitBackgroundReplacementType();
    }
//    @Override
//    public void updateSlidePanel() {
//        super.updateSlidePanel();
//    }
    @Override
    public void updatePortraitBackgroundReplacementUI(int visible) {
        mSmallAdvancedPortraitBackgroundReplacement.setVisibility(visible);
    }
    @Override
    protected void showFaceDetectedTips(boolean show){
        Log.e(TAG, " faces show  = " + show);
        if(show){
           showPortraitTip(true,R.string.lpt_noface);
        } else {
           showPortraitTip(false,-1);
        }
    }
    public void showPortraitTip(boolean show, int resid){
        if(mActivity == null) return;

        TextView tipTextView = (TextView)mActivity.findViewById(R.id.light_portrait_tip);

        if(show && !isPause.get()) {
            tipTextView.setText(resid);
            tipTextView.setVisibility(View.VISIBLE);
        } else if(!show){
            tipTextView.setVisibility(View.GONE);
        }

        Log.d(TAG,"show Portrait Tip :" + show);
    }
    private AtomicBoolean isPause = new AtomicBoolean(false);
}



