package com.dream.camera.modules.AudioPicture;

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
import com.android.camera2.R;

import android.widget.FrameLayout;

import com.dream.camera.MakeupController;
import com.dream.camera.SlidePanelManager;
import com.android.camera.PhotoUI;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.util.DreamUtil;
import android.hardware.Camera.Face;
import com.android.camera.ui.FaceView;
import android.graphics.RectF;
// SPRD: Fix bug 535110, Photo voice record.
import com.android.camera.ui.PhotoVoiceRecordProgress;
import com.android.camera.util.CameraUtil;

public class AudioPictureUI extends PhotoUI implements ResetListener {
    private static final Log.Tag TAG = new Log.Tag("AudioPictureUI");
    //private ImageButton mSettingsButton;
    private View topPanel;

    // SPRD: Fix bug 535110, Photo voice record.
    private PhotoVoiceRecordProgress mPhotoVoiceRecordProgress;

    public AudioPictureUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
        mPhotoVoiceRecordProgress = (PhotoVoiceRecordProgress) mRootView.findViewById(R.id.photo_voice_record_progress);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        DreamUtil dreamUtil = new DreamUtil();
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            if (mBasicModule.isBeautyCanBeUsed()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_top_makeup_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_top_panel,
                            topPanelParent);
                }
            }

            mActivity.getButtonManager().load(topPanel);

        } else {
            if (mBasicModule.isBeautyCanBeUsed()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_front_top_makeup_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_front_top_panel,
                            topPanelParent);
                }
            }
            mActivity.getButtonManager().load(topPanel);
        }
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();
        }
//        mSettingsButton = (ImageButton) topPanel
//                .findViewById(R.id.settings_button_dream);
//        bindSettingsButton(mSettingsButton);
        bindFlashButton();
        //bindCountDownButton();
        bindRefocusButton();
        bindHdrButton();
        bindCameraButton();
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS,View.VISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(
                SlidePanelManager.CAPTURE, false);
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
    }

    @Override
    public void onPause() {
        super.onPause();
        DataModuleManager.getInstance(mActivity).removeListener(this);
    }

    public boolean isInFrontCamera() {
        DreamUtil dreamUtil = new DreamUtil();
        return DreamUtil.FRONT_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID));
    }

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public void showAudioNoteProgress() {
        mPhotoVoiceRecordProgress.startVoiceRecord();
        mActivity.getCameraAppUI().showStopRecordVoiceButton();
        mActivity.getCameraAppUI().hideCaptureIndicator();
    }

    public void hideAudioNoteProgress() {
        mPhotoVoiceRecordProgress.stopVoiceRecord();
        mActivity.getCameraAppUI().hideStopRecordVoiceButton();
        mPhotoVoiceRecordProgress.hideTip();
    }
    /* @} */
    public void setTopPanelVisible(int visible){
        if (topPanel != null)
            topPanel.setVisibility(visible);
    }

    @Override
    public boolean isNeedClearFaceView() { // Bug 1115422
        return (!mBasicModule.mFaceDetectionStarted || mBasicModule.isShutterClicked());
    }
}
