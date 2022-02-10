package com.dream.camera.modules.highresolutionphoto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.PhotoUI;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.SmallAdvancedColorEffectPhoto;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamOrientation;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.util.DreamUtil;


public class HighResolutionPhotoUI extends PhotoUI implements ResetListener {
    private static final Log.Tag TAG = new Log.Tag("HighResolutionPhotoUI");
    protected SmallAdvancedColorEffectPhoto mSmallAdvancedColorEffectPhoto;
    private View topPanel;

    public HighResolutionPhotoUI(CameraActivity activity, PhotoController controller,
                            View parent) {
        super(activity, controller, parent);
        mActivity.getCameraAppUI().initAiSceneView();
        mSmallAdvancedColorEffectPhoto = new SmallAdvancedColorEffectPhoto(mActivity,
                (ViewGroup) mRootView);
        mSmallAdvancedColorEffectPhoto.initphoto();
        mSmallAdvancedColorEffectPhoto.setOrientation(mActivity.getCameraAppUI()
                .getNowOrientation());
    }
    public SmallAdvancedColorEffectPhoto getSmallAdvancedColorEffectPhoto() {
        return mSmallAdvancedColorEffectPhoto;
    }
    @Override
    public void onPanelsHidden() {
        if (mSmallAdvancedColorEffectPhoto != null) {
            mSmallAdvancedColorEffectPhoto.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPanelsShown() {
        if (mSmallAdvancedColorEffectPhoto != null) {
            mSmallAdvancedColorEffectPhoto.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();
        DreamOrientation.setOrientation(mRootView, orientation, true);
        if (mSmallAdvancedColorEffectPhoto != null)
            mSmallAdvancedColorEffectPhoto.setOrientation(orientation);
    }
    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int index) {
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
//        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
//                .getInstance(mActivity).getDataModuleCamera()
//                .getInt(Keys.KEY_CAMERA_ID))) {
        if(mBasicModule.isBeautyCanBeUsed() && CameraUtil.isColorEffectEnabled()){
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.high_resolution_photo_makeup_color_effect_top_panel,
                        topPanelParent);
            }
        }else if(mBasicModule.isBeautyCanBeUsed() && !CameraUtil.isColorEffectEnabled()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.high_resolution_photo_makeup_top_panel,
                        topPanelParent);
            }
        }else if(!mBasicModule.isBeautyCanBeUsed() && CameraUtil.isColorEffectEnabled()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.high_resolution_photo_color_effect_top_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.high_resolution_photo_top_panel,
                        topPanelParent);
            }
        }
            mActivity.getButtonManager().load(topPanel);
//        }
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();
        }
        bindFlashButton();
        if(CameraUtil.isColorEffectEnabled()) {
            bindColorEffectPhotoButton();
        }
        bindCameraButton();

    }
    @Override
    public void onDestroy() {
        if (mSmallAdvancedColorEffectPhoto != null){
            mSmallAdvancedColorEffectPhoto.setVisibility(View.GONE);
        }
        super.onDestroy();
    }
    @Override
    public void onPreviewStarted() {
        super.onPreviewStarted();
        mBasicModule.updateParametersColorEffect();
        mSmallAdvancedColorEffectPhoto.requestLayout();
    }
    public void updateColorEffectPhotoUI(boolean visible) {
        if(visible) {
            mSmallAdvancedColorEffectPhoto.setVisibility(View.VISIBLE);
        }
        else{
            mSmallAdvancedColorEffectPhoto.setVisibility(View.GONE);
        }
    }
    public void bindColorEffectPhotoButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_COLOR_EFFECT_PHOTO_DREAM,
                ((HighResolutionPhotoModule)mBasicModule).mColorEffectPhotoCallback);
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
    public void onSettingReset() {
    }

    @Override
    public void onResume(){
        DataModuleManager.getInstance(mActivity).addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        DataModuleManager.getInstance(mActivity).removeListener(this);
    }
}
