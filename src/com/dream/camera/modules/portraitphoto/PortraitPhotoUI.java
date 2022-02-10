package com.dream.camera.modules.portraitphoto;

import com.dream.camera.modules.blurrefocus.BlurRefocusUI;
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
import com.android.camera.ui.RotateImageView;
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
import com.dream.camera.settings.DataModuleManager;

import java.util.concurrent.atomic.AtomicBoolean;


public class PortraitPhotoUI extends PhotoUI  {
    private static final Log.Tag TAG = new Log.Tag("PortraitPhotoUI");
    private View topPanel;
    private DreamUtil dreamUtil;
    public PortraitPhotoUI(CameraActivity activity, PhotoController controller, View parent//,
            /*MakeupController.MakeupInterface makeupInterface*/) {
        super(activity, controller, parent/*, makeupInterface*/);
        dreamUtil = new DreamUtil();
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            if (mBasicModule.isBeautyCanBeUsed() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                    isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY)) {

                // back camera
                if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                        .getInstance(mActivity).getDataModuleCamera()
                        .getInt(Keys.KEY_CAMERA_ID))) {
                    if(CameraUtil.isLightPortraitEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)){
                        if(CameraUtil.isBackPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_blur_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_top_panel,
                                    topPanelParent);
                        }
                    } else {
                        if(CameraUtil.isBackPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_blur_without_light_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_without_light_top_panel,
                                    topPanelParent);
                        }
                    }
                } else {
                    // front camera
                    if(CameraUtil.isLightPortraitFrontEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)){
                        if(CameraUtil.isFrontPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_blur_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_top_panel,
                                    topPanelParent);
                        }
                    } else {
                        if(CameraUtil.isFrontPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_blur_without_light_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_makeup_without_light_top_panel,
                                    topPanelParent);
                        }
                    }
                }
            } else {
                // back camera
                if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                        .getInstance(mActivity).getDataModuleCamera()
                        .getInt(Keys.KEY_CAMERA_ID))) {
                    if(CameraUtil.isLightPortraitEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)){
                        if(CameraUtil.isBackPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_blur_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_top_panel,
                                    topPanelParent);
                        }

                    } else {
                        if(CameraUtil.isBackPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_blur_without_light_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_without_light_top_panel,
                                    topPanelParent);
                        }

                    }
                } else {
                    // front camera
                    if(CameraUtil.isLightPortraitFrontEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)){
                        if(CameraUtil.isFrontPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_blur_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_top_panel,
                                    topPanelParent);
                        }
                    } else {
                        if(CameraUtil.isFrontPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                                isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                            topPanel = lf.inflate(R.layout.portrait_photo_blur_without_light_top_panel,
                                    topPanelParent);
                        } else {
                            topPanel = lf.inflate(R.layout.portrait_photo_without_light_top_panel,
                                    topPanelParent);
                        }
                    }
                }
            }
        }


        mActivity.getButtonManager().load(topPanel);
        if (mBasicModule.isBeautyCanBeUsed() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY)) {
            bindMakeUpDisplayButton();
        }

        // back camera
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            if(CameraUtil.isLightPortraitEnable() &&
                    DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT)){
                bindLightPortraitDisplayButton();
            }
        } else {
            // front camera
            if(CameraUtil.isLightPortraitFrontEnable() &&
                    DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                            isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT)){
                bindLightPortraitDisplayButton();
            }
        }

        // back camera
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            if(CameraUtil.isBackPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                    isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                bindPortraitRefocusButton();
            }
        } else {
            // front camera
            if(CameraUtil.isFrontPortraitBlurEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                    isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
                bindPortraitRefocusButton();
            }
        }
        bindCameraButton();

    }

    @Override
    public void onPreviewStarted() {
        super.onPreviewStarted();
//        showFaceDetectedTips(true);
    }
    @Override
    protected void showFaceDetectedTips(boolean show){
        Log.e(TAG, " faces show  = " + show);
        if(show){
            if (!(CameraUtil.isPortraitRefocusEnable() && DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                    .getInstance(mActivity).getDataModuleCamera()
                    .getInt(Keys.KEY_CAMERA_ID)))) {
                showPortraitTip(true,R.string.lpt_noface);
            }
        } else {
            showPortraitTip(false,-1);
        }
    }
    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int index) {
        if (view != null) {
            if (index == 2) {
                view.setImageResource(R.drawable.ai_scene_portrait);
                view.setVisibility(visible);
            } else {
                Log.i(TAG , "AiSceneView INVISIBLE");
                view.setVisibility(View.INVISIBLE);
            }
        }
    }

    boolean mLightPortraitEnable = false;
    private static final int LPT_OK = 0;
    private static final int LPT_NOFACE = 1;
    private static final int LPT_FACEPSOE_TOOBIG = 2;
    private static final int LPT_FACE_TOOFAR = 3;
    private static final int LPT_FACE_TOOCLOSE = 4;
    private static final int LPT_FACE_NOTONLY = 5;
    public void onLightPortraitEnable(boolean enable) {
        mLightPortraitEnable = enable;
        if (CameraUtil.isPortraitRefocusEnable() &&
                DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                        .getInstance(mActivity).getDataModuleCamera()
                        .getInt(Keys.KEY_CAMERA_ID))
                && !enable) {
            showPortraitTip(false,-1);
        }
    }

    public void showLightPortraitOverLay(boolean b){
        mLightPortraitOverLay.setVisibility(b?View.VISIBLE:View.GONE);
    }

    public void showPortraitRefocusOverLay(boolean b){
        if (mBlurRefocusController != null) {
            mBlurRefocusController.setBlurRefocusVisibility(b?View.VISIBLE:View.GONE);
            mBlurRefocusController.enableFeatureOperation(DataModuleManager
                    .getInstance(mActivity).getCurrentDataModule()
                    .getBoolean(Keys.KEY_PORTIAIT_REFORCUS_CLOSE));
        }
    }

    @Override
    public void onLightPortraitStatusChange(int status) {
        if(mLightPortraitEnable){
            showLightPortraitStatus(status);
        }
    }

    private void showLightPortraitStatus(int status){
        if(status == LPT_OK){
            showPortraitTip(false,-1);
            mLightPortraitOverLay.setStatus(true);
            return;
        } else {
            int uiStatus = -1;
            switch (status){
                case LPT_NOFACE:
                    uiStatus = R.string.lpt_noface;
                    break;
                case LPT_FACEPSOE_TOOBIG:
                    uiStatus = R.string.lpt_facepsoe_toobig;
                    break;
                case LPT_FACE_TOOFAR:
                    uiStatus = R.string.lpt_face_toofar;
                    break;
                case LPT_FACE_TOOCLOSE:
                    uiStatus = R.string.lpt_face_tooclose;
                    break;
                case LPT_FACE_NOTONLY:
                    uiStatus = R.string.lpt_face_notonly;
                    break;
            }

            showPortraitTip(true,uiStatus);
            mLightPortraitOverLay.setStatus(false);
        }
    }

    public void showPortraitTip(boolean show, int resid){
        if(mActivity == null) return;

        TextView tipTextView = (TextView)mActivity.findViewById(R.id.light_portrait_tip);

        if(show && !isPause.get()){
            if (!(CameraUtil.isPortraitRefocusEnable() && resid == R.string.lpt_noface &&
                    DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                    .getInstance(mActivity).getDataModuleCamera()
                    .getInt(Keys.KEY_CAMERA_ID)))) {
                tipTextView.setText(resid);
                tipTextView.setVisibility(View.VISIBLE);
            } else {
                tipTextView.setVisibility(View.GONE);
            }
        } else if(!show){
            tipTextView.setVisibility(View.GONE);
        }

        Log.d(TAG,"show Portrait Tip :" + show);
    }


    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if(mBasicModule.isBeautyCanBeUsed() &&
                DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                        isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY) ) {
            initMakeupControl(extendPanelParent);
        }

        if(((CameraUtil.isFrontPortraitPhotoEnable() || CameraUtil.isBackPortraitPhotoEnable() )
                && DataModuleManager.getInstance(mActivity).getCurrentDataModule().
                        isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT))){
            initLightPortraitControl(extendPanelParent);
        }

        if(CameraUtil.isPortraitRefocusEnable() &&
                DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                        .getInstance(mActivity).getDataModuleCamera()
                        .getInt(Keys.KEY_CAMERA_ID))){
            LayoutInflater lf = LayoutInflater.from(mActivity);
            lf.inflate(R.layout.blur_refocus_photo_extend_panel, extendPanelParent);
            initBlurRefocusControl(extendPanelParent);
        }
    }

    protected BlurRefocusController mBlurRefocusController;
    public void initBlurRefocusControl(View extendPanelParent) {
        PortraitPhotoModule blurRefocusModule = (PortraitPhotoModule)mController;
        mBlurRefocusController = new BlurRefocusController(extendPanelParent,
                blurRefocusModule,
                Keys.KEY_PORTIAIT_REFORCUS_LEVEL, CameraUtil.getPortraitDefaultFNumber());
        mBlurRefocusController.setBlurCloeseVisibility(View.VISIBLE);
    }

    private AtomicBoolean isPause = new AtomicBoolean(false);

    @Override
    public void onPause() {
        isPause.set(true);
        showPortraitTip(false,-1);
        super.onPause();
    }

    @Override
    public void onResume() {
        isPause.set(false);
        super.onResume();
    }
}
