package com.dream.camera.modules.portraitbackgroundreplacementphoto.sprd;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.PhotoUI;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.ui.RotateImageView;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.filter.ArcsoftSmallAdvancedFilter;
import com.dream.camera.modules.filter.DreamFilterArcControlInterface;
import com.dream.camera.portraitbackgroundreplacement.SmallAdvancedPortraitBackgroundReplacement;
import com.dream.camera.modules.filter.DreamFilterLogicControlInterface;
import com.dream.camera.modules.filter.FilterModuleUIAbs;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.util.DreamUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class PortraitBackgroundReplacementPhotoUI extends PhotoUI implements ResetListener,
        OrientationManager.OnOrientationChangeListener,
        CameraAppUI.PanelsVisibilityListener {
    private static final Log.Tag TAG = new Log.Tag("PortraitBackgroundReplacementPhotoUI");

    private View topPanel;

    protected SmallAdvancedPortraitBackgroundReplacement mSmallAdvancedPortraitBackgroundReplacement;

    public PortraitBackgroundReplacementPhotoUI(CameraActivity activity,
                                                PhotoController controller, View parent) {
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
        mSmallAdvancedPortraitBackgroundReplacement.initphoto();
        mSmallAdvancedPortraitBackgroundReplacement.setVisibility(View.VISIBLE);
        mSmallAdvancedPortraitBackgroundReplacement.setOrientation(mActivity.getCameraAppUI()
                .getNowOrientation());

    }

    @Override
    public void updateAiSceneView(RotateImageView view, int visible, int index) {
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        if (mBasicModule.isBeautyCanBeUsed()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.portrait_background_replacement_photo_makeup_top_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.portrait_background_replacement_photo_top_panel,
                        topPanelParent);
            }
        }
        mActivity.getButtonManager().load(topPanel);
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();
        }
        bindCameraButton();

    }

    @Override
    public void onPreviewStarted() {
        super.onPreviewStarted();
        mBasicModule.updatePortraitBackgroundReplacementType();
    }
    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        if (!mActivity.isSecureCamera()) {
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.SETTINGS, View.VISIBLE);
            SlidePanelManager.getInstance(mActivity).focusItem(
                    SlidePanelManager.CAPTURE, false);
        } else {
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.MODE, View.INVISIBLE);
            SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                    SlidePanelManager.SETTINGS, View.VISIBLE);
            SlidePanelManager.getInstance(mActivity).focusItem(
                    SlidePanelManager.CAPTURE, false);
        }
    }

    @Override
    public void onSettingReset() {
    }
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        super.onOrientationChanged(orientationManager, deviceOrientation);
        int orientation = 0;
        if (mActivity.getOrientationManager() != null) {
            orientation = mActivity.getOrientationManager()
                    .getDeviceOrientation().getDegrees();
            Log.d(TAG, "onOrientationChanged: orientation = " + orientation);
        }

        if (mSmallAdvancedPortraitBackgroundReplacement != null)
            mSmallAdvancedPortraitBackgroundReplacement.setOrientation(orientation);
    }
    public SmallAdvancedPortraitBackgroundReplacement getSmallAdvancedPortraitBackgroundReplacement() {
        return mSmallAdvancedPortraitBackgroundReplacement;
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
    public void updateUI(float aspectRation) {
        super.updateUI(aspectRation);
        if (mSmallAdvancedPortraitBackgroundReplacement != null)
            mSmallAdvancedPortraitBackgroundReplacement.updateUI(aspectRation);
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
    @Override
    public void updatePortraitBackgroundReplacementUI(int visible) {
        mSmallAdvancedPortraitBackgroundReplacement.setVisibility(visible);
    }
    public void showPortraitTip(boolean show, int resid){
        if(mActivity == null) return;

        TextView tipTextView = (TextView)mActivity.findViewById(R.id.light_portrait_tip);

        if(show && !isPause.get()){
            tipTextView.setText(resid);
            tipTextView.setVisibility(View.VISIBLE);
        } else if(!show){
            tipTextView.setVisibility(View.GONE);
        }

        Log.d(TAG,"show Portrait Tip :" + show);
    }
    private AtomicBoolean isPause = new AtomicBoolean(false);
    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        isPause.set(false);
        DataModuleManager.getInstance(mActivity).addListener(this);
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
    }



