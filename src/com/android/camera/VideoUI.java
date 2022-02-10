/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.camera.app.CameraAppUI;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.*;
import com.android.camera.ui.focus.ChasingView;
import com.android.camera.ui.focus.FocusRing;
import com.android.camera.util.CameraUtil;
import com.android.camera.widget.ModeOptionsOverlay;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.ButtonManagerDream;
import com.android.camera.ButtonManager;
import com.dream.camera.DreamOrientation;
import com.dream.camera.DreamUI;
import com.dream.camera.SlidePanelManager;
import com.android.camera.settings.Keys;
import android.graphics.drawable.Drawable;
import com.dream.camera.dreambasemodules.DreamInterface;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout;

import android.view.SurfaceHolder;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout.SettingUIListener;
import com.dream.camera.util.DreamUtil;
import com.android.camera.VideoModule;

import com.android.ex.camera2.portability.CameraAgent;
import android.hardware.Camera.Face;
import com.sprd.camera.freeze.FaceDetectionController;

public class VideoUI extends DreamUI implements CameraAgent.CameraFaceDetectionCallback, PreviewStatusListener, SurfaceHolder.Callback, DreamInterface, SettingUIListener, FaceDetectionController , CameraAppUI.PanelsVisibilityListener{
    private static final Log.Tag TAG = new Log.Tag("VideoUI");

    private final static float UNSET = 0f;
    private final PreviewOverlay mPreviewOverlay;
    // module fields
    protected final CameraActivity mActivity;
    protected final View mRootView;
    private final FocusRing mFocusRing;
    private final ChasingView mChasingRing;
    // An review image having same size as preview. It is displayed when
    // recording is stopped in capture intent.
    private ImageView mReviewImage;
    protected TextView mRecordingTimeView;
    private LinearLayout mLabelsLinearLayout;
    private RotateLayout mRecordingTimeRect;
    protected boolean mRecordingStarted = false;
    protected final VideoController mController;
    private float mZoomMax;
    private float mZoomMin;
    private float[] mZoomRatioSection;

    private float mAspectRatio = UNSET;
    private final AnimationManager mAnimationManager;
    private PreviewStatusListener.PreviewAreaChangedListener mPreviewAreaChangedListener;
    protected VideoModule mBasicModule;
    private final FaceView mFaceView;
    private final Level mLevel;
    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right,
                                       int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    @Override
    public void onPreviewFlipped() {
        mController.updateCameraOrientation();
    }

    private final GestureDetector.OnGestureListener mPreviewGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
        @Override
        public void onLongPress(MotionEvent var1){
            if (CameraUtil.isVideoAEAFLockEnable()) {
                mController.onLongPress(var1);
            }
        }
    };

    public VideoUI(CameraActivity activity, VideoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        ViewGroup moduleRoot = (ViewGroup) mRootView.findViewById(R.id.module_layout);
        mActivity.getLayoutInflater().inflate(R.layout.dream_video_module,
                moduleRoot, true);

        mPreviewOverlay = (PreviewOverlay) mRootView.findViewById(R.id.preview_overlay);
        mLevel = (Level) mRootView.findViewById(R.id.level);
        initializeMiscControls();
        mAnimationManager = new AnimationManager();
        mFocusRing = (FocusRing) mRootView.findViewById(R.id.focus_ring);
        mChasingRing = (ChasingView) mRootView.findViewById(R.id.chasing_ring);
        mFaceView = (FaceView) mRootView.findViewById(R.id.face_view);
        mActivity.getCameraAppUI().updateAiSceneView(View.GONE, 0);
        initUI();
    }

    public void setPreviewSize(int width, int height) {
        if (width == 0 || height == 0) {
            Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        float aspectRatio;
        if (width > height) {
            aspectRatio = (float) width / height;
        } else {
            aspectRatio = (float) height / width;
        }
        setAspectRatio(aspectRatio);
    }

    public FocusRing getFocusRing() {
        return mFocusRing;
    }

    public ChasingView getChasingRing() {return mChasingRing;}
    /**
     * Cancels on-going animations
     */
    public void cancelAnimations() {
        mAnimationManager.cancelAnimations();
    }

    public void setOrientationIndicator(int orientation, boolean animation) {
        // We change the orientation of the linearlayout only for phone UI
        // because when in portrait the width is not enough.
        if (mLabelsLinearLayout != null) {
            if (((orientation / 90) & 1) == 0) {
                mLabelsLinearLayout.setOrientation(LinearLayout.VERTICAL);
            } else {
                mLabelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            }
        }

        /*
         * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
         * Original Android code:

        mRecordingTimeRect.setOrientation(0, animation);

         */
    }
    public void setLevelVisiable(boolean visiable){
        if(visiable) {
            mLevel.setVisibility(View.VISIBLE);
        }else {
            mLevel.setVisibility(View.GONE);
        }
    }
    public void updateLevel(){
        mLevel.setHasLevelAngle(mBasicModule.getHasLevelAngle());
        mLevel.setUIRotation(mBasicModule.getUIRotation());
        mLevel.setLevelAngle(mBasicModule.getLevelAngle());
        mLevel.setHasPitchAngle(mBasicModule.getHasPitchAngle());
        mLevel.setPitchAngle(mBasicModule.getPitchAngle());
        mLevel.setHasGeoDirection(mBasicModule.getHasGeoDirection());
        mLevel.setGeoDirection(mBasicModule.getGeoDirection());
        mLevel.setRotation(mBasicModule.getRotation());
        mLevel.setOrigLevelAngle(mBasicModule.getOrigLevelAngle());
        if(mBasicModule.getHasLevelAngle()) {
            mLevel.invalidate();
        }
    }
    public void makeLevelAppear(){
        mLevel.invalidate();
    }
    private void initializeMiscControls() {
        mReviewImage = (ImageView) mRootView.findViewById(R.id.review_image);
        mRecordingTimeView = (TextView) mRootView.findViewById(R.id.recording_time);
        mRecordingTimeRect = (RotateLayout) mRootView.findViewById(R.id.recording_time_rect);
        // The R.id.labels can only be found in phone layout.
        // That is, mLabelsLinearLayout should be null in tablet layout.
        mLabelsLinearLayout = (LinearLayout) mRootView.findViewById(R.id.labels);

        // SPRD Bug:474704 Feature:Video Recording Pause.
        mPauseButton = (ImageView) mRootView.findViewById(R.id.btn_video_pause);

        if (mReviewImage != null) {
            if (mReviewImage instanceof RotateImageView) {
                ((RotateImageView) mReviewImage).enableScaleup();
            }

            mPreviewAreaChangedListener = new PreviewStatusListener.PreviewAreaChangedListener() {
                @Override
                public void onPreviewAreaChanged(RectF previewArea) {
                    FrameLayout.LayoutParams params =
                            (FrameLayout.LayoutParams) mReviewImage.getLayoutParams();
                    params.width = (int) previewArea.width();
                    params.height = (int) previewArea.height();
                    params.setMargins((int) previewArea.left, (int) previewArea.top, 0, 0);
                    mReviewImage.setLayoutParams(params);
                    if (mFaceView != null) {
                        mFaceView.onPreviewAreaChanged(previewArea);
                    }
                }
            };

            mActivity.getCameraAppUI().addPreviewAreaChangedListener(mPreviewAreaChangedListener);
        }
    }
    public void onResume() {
    }
    public void updateOnScreenIndicators(CameraSettings settings) {
    }

    public void setAspectRatio(float ratio) {
        if (ratio <= 0) {
            return;
        }
        float aspectRatio = ratio > 1 ? ratio : 1 / ratio;
        if (aspectRatio != mAspectRatio) {
            mAspectRatio = aspectRatio;
            mController.updatePreviewAspectRatio(mAspectRatio);
        }
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public void showPreviewBorder(boolean enable) {
        // TODO: mPreviewFrameLayout.showBorder(enable);
    }

    public void showRecordingUI(boolean recording, int orientation) {

        // SPRD Bug:474704 Feature:Video Recording Pause.
        if (recording == false)
            mPauseRecording = false;

        if (mActivity.getCameraAppUI().getPauseButton() != null) {
            mActivity.getCameraAppUI().getPauseButton()
                    .setImageResource(
                            mPauseRecording ? R.drawable.ic_start_sprd
                                    : R.drawable.ic_pause_sprd);
        }

        mRecordingStarted = recording;
        if (recording) {
            Drawable recordingIcon = mActivity.getResources().getDrawable(R.drawable.ic_recording_indicator);
            recordingIcon.setBounds(0, 0, recordingIcon.getMinimumWidth(), recordingIcon.getMinimumHeight());
            mRecordingTimeView.setCompoundDrawables(recordingIcon, null, null, null);
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            mRecordingTimeView
                    .announceForAccessibility(mActivity.getResources()
                            .getString(R.string.video_recording_started));
            mActivity.getCameraAppUI().setShutterPartInBottomBarShow(
                    View.VISIBLE, true);
            mActivity.getCameraAppUI().changeToRecordingUI();
            // nj dream camera test 66 - 68
            mActivity.getCameraAppUI().updateTopPanelUI(View.GONE);
            mActivity.getCameraAppUI().updateSlidePanelUI(View.GONE);
            mActivity.getCameraAppUI().updateExtendPanelUI(View.GONE);
            mActivity.getCameraAppUI().updateUltraWideAngleView(View.GONE);

        } else {
            mRecordingTimeView
                    .announceForAccessibility(mActivity.getResources()
                            .getString(R.string.video_recording_stopped));
            mRecordingTimeView.setVisibility(View.GONE);
            mActivity.getCameraAppUI().setShutterPartInBottomBarShow(View.GONE,
                    true);

            mActivity.getCameraAppUI().changeToVideoReviewUI();
            // nj dream camera test 66 - 68
            mActivity.getCameraAppUI().updateTopPanelUI(View.VISIBLE);
            mActivity.getCameraAppUI().updateSlidePanelUI(View.VISIBLE);
            mActivity.getCameraAppUI().updateExtendPanelUI(View.VISIBLE);
            mActivity.getCameraAppUI().updateUltraWideAngleView(View.VISIBLE);
        }

    }

    public void showReviewImage(Bitmap bitmap) {
        mReviewImage.setImageBitmap(bitmap);
        mReviewImage.setVisibility(View.VISIBLE);
    }

    public void showReviewControls() {
        mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        mReviewImage.setVisibility(View.VISIBLE);
    }

    private boolean isRecordingIn4k(int cameraId) {
        String videoQualityKey = DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(cameraId) ? Keys.KEY_VIDEO_QUALITY_FRONT
                : Keys.KEY_VIDEO_QUALITY_BACK;
        String videoQuality = DataModuleManager.getInstance(mActivity).getCurrentDataModule()
                .getString(videoQualityKey);
        int quality = SettingsUtil.getVideoQuality(videoQuality, cameraId);
        Log.d(TAG, "Selected video quality for '" + videoQuality + "' is " + quality);
        if (quality == CamcorderProfile.QUALITY_2160P) {
            return true;
        }
        return false;
    }


    public void initializeZoom(CameraSettings settings, CameraCapabilities capabilities) {
        if (DataModuleManager.getInstance(mActivity).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) &&
                !DataModuleManager.getInstance(mActivity).getCurrentDataModule().getBoolean(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) || !CameraUtil.isZoomPanelEnabled()) {
            return;
        }

        int cameraId = DataModuleManager.getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID);

        if (isRecordingIn4k(cameraId)){
            mZoomMax = CameraUtil.getZoomMaxInFourKVideo(capabilities.getMaxZoomRatio());
        } else {
            mZoomMax = capabilities.getMaxZoomRatio();
        }
        mZoomRatioSection = capabilities.getZoomRatioSection();
        if (mZoomRatioSection != null) {
            mZoomMin = mZoomRatioSection[0];
            int i = mZoomRatioSection.length - 1;
            for (;i>0;i--){
                if (mZoomRatioSection[i] != 0){
                    mZoomMax = mZoomRatioSection[i];
                }
            }
        } else {
            mZoomMin = 1.0f;
        }

        boolean isSimple = true;
        if (CameraUtil.isTcamAbility(mActivity,mActivity.getCurrentModuleIndex(),cameraId))
            isSimple = false;
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        // TODO: setup zoom through App UI.
        mPreviewOverlay.setupZoom(mZoomMax, settings.getCurrentZoomRatio(), mZoomRatioSection,
                new ZoomChangeListener(), isSimple);
    }

    /* SPRD: fix bug 553567 slow motion does not support takeSnapShot and zoom @{ */
    public void hideZoomProcessorIfNeeded() {
        mPreviewOverlay.hideZoomProcessorIfNeeded();
    }

    public void resetZoomSimple(){
        if (mPreviewOverlay != null)
            mPreviewOverlay.resetZoomSimple();
    }

    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        return false;
    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    /**
     * Hide the focus indicator.
     */
    public void hidePassiveFocusIndicator() {
        if (mFocusRing != null) {
            Log.v(TAG, "mFocusRing.stopFocusAnimations()");
            mFocusRing.stopFocusAnimations();
        }
    }

    /**
     * Show the passive focus indicator.
     */
    public void showPassiveFocusIndicator() {
        if (mFocusRing != null) {
            mFocusRing.startPassiveFocus();
        }
    }


    /**
     * @return The size of the available preview area.
     */
    public Point getPreviewScreenSize() {
        return new Point(mRootView.getMeasuredWidth(), mRootView.getMeasuredHeight());
    }

    /**
     * Adjust UI to an orientation change if necessary.
     */
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();
        DreamOrientation.setOrientation(mRootView, orientation, true);
    }
    @Override
    public void onPanelsHidden() {

    }
    @Override
    public void onPanelsShown() {

    }
    private class ZoomChangeListener implements PreviewOverlay.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(float ratio) {
            mController.onZoomChanged(ratio);
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    }

    public void enablePreviewOverlayHint(boolean enable) {
        mPreviewOverlay.setDetector(enable);
    }

    public boolean isEnableDetector() {
        if (mPreviewOverlay == null) {
            return true;
        }
        return mPreviewOverlay.isEnableDetector();
    }

    /* SPRD: Fix bug 568154 @{ */
    public void setPreviewOverlayZoom(float zoom) {
        mPreviewOverlay.setZoom(zoom);
    }
    /* @} */

    public void hideZoomUI() {
        if (mPreviewOverlay.isZoomSimple()) {
            mPreviewOverlay.hideZoomUI();
        } else {
            mPreviewOverlay.stopZoomState();
        }
    }

    // SurfaceTexture callbacks
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (((VideoModule) mController).isUseSurfaceView()) {
            return;
        }
        mController.onPreviewUIReady();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mController.onPreviewUIDestroyed();
        Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mController instanceof VideoModule) {
            ((VideoModule) mController).onSurfaceTextureUpdated();
        }
    }

    public void onPause() {
        if (mFaceView != null) {
            mFaceView.clear();
        }
        // recalculate aspect ratio when restarting.
        mAspectRatio = 0.0f;
        mPreviewOverlay.stopZoomState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreviewAreaChangedListener != null) {
            mActivity.getCameraAppUI().removePreviewAreaChangedListener(mPreviewAreaChangedListener);
        }
    }

    /*
     * SPRD Bug:474704 Feature:Video Recording Pause. @{
     */
    private ImageView mPauseButton;
    protected boolean mPauseRecording = false;

    private void setPauseButtonLayout(int orientation) {
        Log.i(TAG, "setPauseButtonLayout   orientation = " + orientation);

        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        float m = mActivity.getResources().getDimension(R.dimen.video_play_button_magin);
        if (orientation == 0) {
            layout.setMargins((int) m, 0, 0, 0);
        } else if (orientation == 90) {
            layout.setMargins(0, (int) m, 0, 0);
        } else if (orientation == 180) {
            layout.setMargins((int) m, 0, 0, 0);
        } else if (orientation == 270) {
            layout.setMargins(0, 0, 0, (int) m);
        }
        mPauseButton.setLayoutParams(layout);
    }

    public void onPauseClicked(boolean mPauseRecorderRecording) {
        // reset pause button icon
        mPauseRecording = mPauseRecorderRecording;
        mActivity
                .getCameraAppUI()
                .getPauseButton()
                .setImageResource(
                        mPauseRecorderRecording ? R.drawable.ic_start_sprd
                                : R.drawable.ic_pause_sprd);
        Drawable recording = mActivity.getResources().getDrawable(R.drawable.ic_recording_indicator);
        Drawable pasuing = mActivity.getResources().getDrawable(R.drawable.ic_recording_pause_indicator);
        if (mPauseRecorderRecording) {
            pasuing.setBounds(0, 0, pasuing.getMinimumWidth(), pasuing.getMinimumHeight());
            mRecordingTimeView.setCompoundDrawables(pasuing, null, null, null);
        } else {
            recording.setBounds(0, 0, recording.getMinimumWidth(), recording.getMinimumHeight());
            mRecordingTimeView.setCompoundDrawables(recording, null, null, null);
        }
    }

    /* SPRD: Add for bug 559531 @{ */
    public void enablePauseButton(boolean enable) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enable);
        }
    }
    /* @} */

    public void initUI() {
        mBasicModule = (VideoModule) mController;
        mActivity.getCameraAppUI().setDreamInterface(this);
//        // Generate a view to fit top panel.
//        ViewGroup topPanelParent = (ViewGroup) mRootView
//                .findViewById(R.id.top_panel_parent);
//        topPanelParent.removeAllViews();
        updateTopPanelValue(mActivity);
//        fitTopPanel(topPanelParent);
//        // Generate views to fit extend panel.
//        //mActivity.getCameraAppUI().initExtendPanel();
//
//        // Update item on slide panel.
//        updateSlidePanel();
    }

    public boolean isReviewImageShow() {
        if (mReviewImage != null && mReviewImage.getVisibility() == View.VISIBLE)
            return true;

        return false;
    }

    /* SPRD: Fix bug 613015 add SurfaceView support @{ */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: " + holder + " " + width + " " + height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: " + holder);
        mController.onPreviewUIReady();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: " + holder);
        mController.onPreviewUIDestroyed();
    }
    /* @} */

    public void setButtonVisibility(int buttonId, int visibility) {
        ((ButtonManagerDream) mActivity.getButtonManager()).setButtonVisibility(buttonId, visibility);
    }

    private boolean mHasDelayWorkOnInit = true;

    /* SPRD: Fix bug 659315, optimize camera launch time @{ */
    public void onPreviewStarted() {
        if (!mHasDelayWorkOnInit) {
            return;
        }
        mHasDelayWorkOnInit = false;

        mActivity.getCameraAppUI().inflateStub();

        // Generate a view to fit top panel.
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        updateTopPanelValue(mActivity);
        ViewGroup extendPanelParent =mActivity.getCameraAppUI().getExtendPanelParent();
        topPanelParent.removeAllViews();
        fitTopPanel(topPanelParent);
        fitExtendPanel(extendPanelParent);
        // Generate views to fit extend panel.
        //mActivity.getCameraAppUI().initExtendPanel();

        // Update item on slide panel.
        updateSlidePanel();

        mActivity.getCameraAppUI().initBottomBar();
        mActivity.getButtonManager().load(mRootView);
        mActivity.getCameraAppUI().initializeBottomBarSpec();
        ((VideoModule) mController).initializeControlByIntent();

        // Update icons on bottom panel.
        updateBottomPanel();

        //SPRD:bug903373 updateButtomBar after preview started。
        mActivity.getCameraAppUI().updateButtomBar();
        mActivity.getCameraAppUI().addShutterListener();
    }

    @Override
    public void initializeadjustFlashPanel(int maxValue, int minvalue, int currentValue) {
        mActivity.getCameraAppUI().initializeadjustFlashPanel(maxValue, minvalue, currentValue);
    }

    @Override
    public void showAdjustFlashPanel() {
        mActivity.getCameraAppUI().showAdjustFlashPanel();
    }

    @Override
    public void hideAdjustFlashPanel() {
        mActivity.getCameraAppUI().hideAdjustFlashPanel();
    }

    /* SPRD: Fix bug 898421, The inheritance structure of the refactoring model @{ */
    @Override
    public void updateBottomPanel() {
        mActivity.getCameraAppUI().updateSwitchModeBtn(this);
    }

    @Override
    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS, View.VISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(
                SlidePanelManager.CAPTURE, false);
    }

    public void bindSettingsButton(View settingsButton) {
        if (settingsButton != null) {
            final DreamUIPreferenceSettingLayout dps = (DreamUIPreferenceSettingLayout) mRootView
                    .findViewById(R.id.dream_ui_preference_setting_layout);
            dps.changeModule(VideoUI.this);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBasicModule.isShutterClicked()) {
                        return;
                    }
                    // mActivity.onSettingsSelected();
                    // update UI
                    if (!mRecordingStarted) {
                        dps.changeVisibilty(View.VISIBLE);
                        mActivity.getCameraAppUI().updatePreviewUI(View.GONE, false);
                    }
                }
            });
        }
    }

    public void bindSwitchPreviewButton(View settingsButton) {
        if (settingsButton != null) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBasicModule.isShutterClicked()) {
                        return;
                    }
                    mBasicModule.switchPreview();
                }
            });
        }
    }

    public void bindFlashButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.setFlashMode(DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID)), CameraUtil.getFrontFlashMode(), mActivity.getCurrentModuleIndex());
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM,
                mBasicModule.mFlashCallback);
    }

    public void bindCameraButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_CAMERA_DREAM,
                mBasicModule.mCameraCallback);
    }

    public void bindMakeUpDisplayButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_MAKE_UP_DISPLAY_DREAM,
                mBasicModule.mMakeUpDisplayCallback);
    }

    /**
     * update preview ui after settings closed
     */
    public void onSettingUIHide() {
        if (!mRecordingStarted) {
            mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, false);
        }
    }

    public void onCloseModeListOrSettingLayout() {
        if (mRecordingStarted) {
            mActivity.getCameraAppUI().updateTopPanelUI(View.GONE);
            mActivity.getCameraAppUI().updateSlidePanelUI(View.GONE);
            mActivity.getCameraAppUI().updateUltraWideAngleView(View.GONE);
        }
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
    }
    /*  @} */
    protected void showFaceDetectedTips(boolean show){
    }
    public void updatePortraitBackgroundReplacementUI(int visible) {
    }
    @Override
    public void onFaceDetection(Face[] faces, int[] attributes, boolean faceChange) {
        if(faces.length == 0 && mFaceView != null && !mFaceView.isPause() && !mBasicModule.isShutterClicked()){
            showFaceDetectedTips(true);
        }
        if (isNeedClearFaceView() && mFaceView != null) {
            mFaceView.clear();
            return;
        }
        if (mFaceView != null && faces != null) {
            if ((((VideoModule) mController).mFocusManager.getMeteringAreas() != null)) {
                mFaceView.clear();
            } else {
                mFaceView.setFaces(faces);
            }
            if (faceChange && faces.length != 0) {
                showFaceDetectedTips(false);
                mFaceView.disappearFaceView();
            }
            mFaceView.setNeedShowFace(false);
        }
    }
    public void onStartFaceDetection(int orientation, boolean mirror) {
        if (mFaceView != null) {
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(orientation);
            mFaceView.setMirror(mirror);
            mFaceView.resume();
        }
    }

    @Override
    public void pauseFaceDetection() {
        if (mFaceView != null) {
            mFaceView.pause();
        }
    }

    @Override
    public void resumeFaceDetection() {
        if (mFaceView != null) {
            mFaceView.resume();
        }
    }

    public void clearFaces() {
        if (mFaceView != null) {
            mFaceView.clear();
        }
    }

    public boolean isNeedClearFaceView() {
        return (!mBasicModule.mFaceDetectionStarted || mBasicModule.isRecording());//SPRD:fix bug957181/1006791/1115422
        // }

    }
}
