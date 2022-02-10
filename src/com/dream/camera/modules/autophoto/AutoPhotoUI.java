package com.dream.camera.modules.autophoto;

import java.util.HashMap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.PhotoModule;

import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.ui.RotateImageView;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import android.widget.FrameLayout;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.SlidePanelManager;
import com.android.camera.PhotoUI;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.util.DreamUtil;
import android.hardware.Camera.Face;
import com.android.camera.ui.FaceView;
import android.graphics.RectF;
import com.dream.camera.MakeupController;
import com.dream.camera.MakeupController.*;
import android.graphics.Color;
import android.widget.TextView;

import static com.android.camera.settings.Keys.KEY_CAMERA_AI_BEAUTY_ENTERED;

public class AutoPhotoUI extends PhotoUI implements ResetListener {
    private static final Log.Tag TAG = new Log.Tag("AutoPhotoUI");
    private static final int AI_SCENE_DEFAULT_INDEX = 0; // AI_SCENE_DEFAULT in isp_com.h is 0
    private static final int AI_SCENE_MAX_INDEX = 16; // AI_SCENE_MAX in isp_com.h is 16

    //private ImageButton mSettingsButton;
    private View topPanel;
    private DreamUtil dreamUtil;
    private TextView tipTextView;

    int aiSceneIcons[] = {
            0 ,
            R.drawable.ai_scene_food ,      //  1
            R.drawable.ai_scene_portrait ,  //  2
            R.drawable.ai_scene_foliage ,   //  3
            R.drawable.ai_scene_sky ,       //  4
            R.drawable.ai_scene_night ,     //  5
            R.drawable.ai_scene_backlight , //  6
            R.drawable.ai_scene_text ,      //  7
            R.drawable.ai_scene_sunrise ,   //  8
            R.drawable.ai_scene_building ,  //  9
            R.drawable.ai_scene_landscape , //  10
            R.drawable.ai_scene_snow ,      //  11
            R.drawable.ai_scene_firework ,  //  12
            R.drawable.ai_scene_beach ,     //  13
            R.drawable.ai_scene_pet ,       //  14
            R.drawable.ai_scene_flower      //  15
    };

    int aiSceneTipsID[] = {
            0 ,
            R.string.ai_scene_tips_food ,
            R.string.ai_scene_tips_portrait ,
            R.string.ai_scene_tips_foliage ,
            R.string.ai_scene_tips_sky ,
            R.string.ai_scene_tips_night ,
            R.string.ai_scene_tips_backlight ,
            R.string.ai_scene_tips_text ,
            R.string.ai_scene_tips_sunrise ,
            R.string.ai_scene_tips_building ,
            R.string.ai_scene_tips_landscape ,
            R.string.ai_scene_tips_snow ,
            R.string.ai_scene_tips_firework ,
            R.string.ai_scene_tips_beach ,
            R.string.ai_scene_tips_pet ,
            R.string.ai_scene_tips_flower
    };

    private int mAiSceneIndex = -1;
    private int mAiScenePrevIndex = -1;

    public AutoPhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
        dreamUtil = new DreamUtil();
        mActivity.getCameraAppUI().initAiSceneView();

        // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
        if(CameraUtil.isUltraWideAngleEnabled() && DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID)))
            mActivity.getCameraAppUI().initUltraWideAngleSwitchView(false);
    }

    public int getAiSceneTip() {
        return aiSceneTipsID[mAiSceneIndex];
    }

    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int index) {
        if(index != mAiScenePrevIndex) {
            mAiSceneIndex = index;
            processForSceneResult(index , mAiScenePrevIndex);
            mAiScenePrevIndex = index;
        }
        else
            return;

        if (view != null) {
            if (index > AI_SCENE_DEFAULT_INDEX && index < AI_SCENE_MAX_INDEX) {
                view.setImageResource(aiSceneIcons[mAiSceneIndex]);
                view.setVisibility(visible);
            }
            else {
                Log.i(TAG , "AiSceneView INVISIBLE");
                view.setVisibility(View.INVISIBLE);
                mAiSceneIndex = -1;
            }
        }
    }

    private void processForSceneResult(int oldSceneIndex , int newSceneIndex) {
        // for AI SceneDetection
        if (newSceneIndex == 2 || oldSceneIndex == 2) {
            // 2 means "HAL_AI_SCENE_PORTRAIT" in "enum hal_ai_scene_type" , in isp_com.h
            processForScenePORTRAIT();
            return;
        }
    }

    private void processForScenePORTRAIT() {
        // for AI Beauty
        if (mAiSceneIndex == 2) { // others to Portrait:
            // turn ON AI Beauty
            switchBeauty(true);
        } else { // Portrait to others:
            switchBeauty(false);
        }
    }

    private void switchBeauty(boolean onFlag) {
        // for AI Beauty
        Log.i(TAG , "switch AI Beauty : " + onFlag);
        DataModuleManager.getInstance(mActivity).getDataModulePhoto().changeSettings(KEY_CAMERA_AI_BEAUTY_ENTERED , onFlag);
        mBasicModule.startAiBeauty(onFlag);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
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

        } else {
            if (mBasicModule.isBeautyCanBeUsed()) {
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
        }
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();
        }
        bindFlashButton();
        //bindCountDownButton();
        bindRefocusButton();
        bindHdrButton();
        bindFilterButton();
        bindMontionPhotoButton();
        bindCameraButton();

        // Bug 1018708 - hide or show BeautyButton on topPanel
        boolean isAiSceneDetectionUsing = DataModuleManager.getInstance(
                mActivity.getAndroidContext()).getCurrentDataModule().getInt(Keys.KEY_CAMERA_AI_SCENE_DATECT) == 1;
        showBeautyButton(!isAiSceneDetectionUsing);
    }

    @Override
    public void showBeautyButton(boolean show) {
        if (topPanel != null && mBasicModule.isBeautyCanBeUsed()) {
            View v = topPanel.findViewById(R.id.make_up_blank);
            FrameLayout fl = topPanel.findViewById(R.id.make_up_frame_layout);
            if (v != null && fl != null) {
                if (show == true) {
                    Log.i(TAG , "make Beauty Button VISIBLE");
                    v.setVisibility(View.VISIBLE);
                    fl.setVisibility(View.VISIBLE);
                    int isBeauty = DataModuleManager.getInstance(
                            mActivity.getAndroidContext()).getCurrentDataModule().getInt(Keys.KEY_MAKE_UP_DISPLAY);
                    if (isBeauty == 1) // show makeup controller
                        mMakeupController.resume(DataModuleManager.getInstance(
                                mActivity.getAndroidContext()).getCurrentDataModule().getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED));
                    else
                        mMakeupController.pause();
                } else {
                    Log.i(TAG , "make Beauty Button GONE");
                    v.setVisibility(View.GONE);
                    fl.setVisibility(View.GONE);
                    mMakeupController.pause();
                }
            } else
                Log.w(TAG , "cannot find 'make_up_blank' and (or) 'make_up_frame_layout'"); // e.g. Bug 1128153
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
    public void onSettingReset() {
        DreamUtil dreamUtil = new DreamUtil();
        int cameraid = dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID));
        if (DreamUtil.BACK_CAMERA != cameraid) {
            mBasicModule.updateMakeLevel();
        }
    }

    @Override
    public void onResume(){
        DataModuleManager.getInstance(mActivity).addListener(this);
        //updateSettingsButton(mSettingsButton);//SPRD:fix bug 607898
    }

    @Override
    public void onPause() {
        super.onPause();
        DataModuleManager.getInstance(mActivity).removeListener(this);

        if(tipTextView != null){
            tipTextView.setVisibility(View.GONE);
        }

        if(mActivity != null && mActivity.getModuleLayoutRoot() != null
                && mShowMontionPhotoTipRunnable != null){
            mActivity.getModuleLayoutRoot().removeCallbacks(mShowMontionPhotoTipRunnable);
        }
    }

    public boolean isInFrontCamera() {
        DreamUtil dreamUtil = new DreamUtil();
        return DreamUtil.FRONT_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID));
    }

    @Override
    public void showMotionPhotoTipText(boolean visible){
        if(mActivity == null) return;
        if(tipTextView == null){
            tipTextView = (TextView)mActivity.findViewById(R.id.motion_photo_tip);
        }

        Log.d(TAG,"show motionPhotoTip :" + visible);
        if(visible){
            setMotionPhotoTipStyle(tipTextView, R.string.motion_photo_on,
                    mActivity.getColor(R.color.blur_effect_highlight) , R.drawable.blur_effect_highlight, View.VISIBLE);
        } else {
            setMotionPhotoTipStyle(tipTextView, R.string.motion_photo_off,
                    Color.WHITE,R.drawable.blur_effect_disable, View.VISIBLE);
        }
    }


    public void setMotionPhotoTipStyle(TextView tipTextView, int message, int mColor, int backgroundResource, int visible){
        if(tipTextView != null){
            tipTextView.setText(message);
            tipTextView.setTextColor(mColor);
            int _pL = tipTextView.getPaddingLeft();
            int _pR = tipTextView.getPaddingLeft();
            int _pT = tipTextView.getPaddingTop();
            int _pB = tipTextView.getPaddingBottom();
            tipTextView.setBackgroundResource(backgroundResource);
            tipTextView.setPadding(_pL, _pT, _pR, _pB);
            tipTextView.setVisibility(visible);
        }

        if(mShowMontionPhotoTipRunnable != null && mActivity != null && mActivity.getModuleLayoutRoot() != null) {
            mActivity.getModuleLayoutRoot().removeCallbacks(mShowMontionPhotoTipRunnable);
            mActivity.getModuleLayoutRoot().postDelayed(mShowMontionPhotoTipRunnable, 3000);
        }
    }

    protected final Runnable mShowMontionPhotoTipRunnable = new Runnable() {
        @Override
        public void run() {
            if (tipTextView != null) {
                tipTextView.setVisibility(View.GONE);
            }
        }
    };
}
