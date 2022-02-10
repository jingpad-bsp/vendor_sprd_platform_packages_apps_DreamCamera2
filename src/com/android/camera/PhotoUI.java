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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.Face;
import android.os.AsyncTask;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.Level;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.focus.ChasingView;
import com.android.camera.ui.focus.FocusRing;
import com.android.camera.widget.selector.lightPortrait.LightPortraitOverLay;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.LightPortraitControllerDream;
import com.dream.camera.ui.TopPanelListener;
import com.dream.camera.util.DreamUtil;
import com.sprd.camera.freeze.FaceDetectionController;
import com.sprd.camera.aidetection.AIDetectionController;
import com.android.camera.app.OrientationManager;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.view.WindowManager;
import android.view.Display;
import android.widget.TextView;

import com.android.camera.util.CameraUtil;
import com.android.camera.ui.RotateImageView;
import android.view.LayoutInflater;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout.SettingUIListener;
import com.dream.camera.DreamOrientation;
import com.dream.camera.modules.blurrefocus.BlurRefocusUI;
import com.dream.camera.modules.filter.sprd.FilterModuleUISprd;
import com.dream.camera.modules.manualphoto.ManualPhotoUI;
import com.dream.camera.DreamUI;
import com.dream.camera.MakeupController;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.SlidePanelManager;

import android.view.SurfaceHolder;
import com.android.camera.captureintent.PictureDecoder;
import com.dream.camera.dreambasemodules.DreamInterface;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout;

import static com.dream.camera.ButtonManagerDream.BUTTON_MONTIONPHOTO_DREAM;


public class PhotoUI extends DreamUI implements PreviewStatusListener,
    CameraAgent.CameraFaceDetectionCallback, PreviewStatusListener.PreviewAreaChangedListener,
        FaceDetectionController, SurfaceHolder.Callback, DreamInterface, SettingUIListener, CameraAgent.CameraLightPortraitCallback , CameraAppUI.PanelsVisibilityListener{

    private static final Log.Tag TAG = new Log.Tag("PhotoUI");
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private static final float UNSET = 0f;

    private final PreviewOverlay mPreviewOverlay;
    private final FocusRing mFocusRing;
    private final ChasingView mChasingRing;
    protected final CameraActivity mActivity;
    protected final PhotoController mController;
    protected MakeupController mMakeupController;
    protected LightPortraitControllerDream mLightPortraitControllerDream;

    protected final View mRootView;
    private Dialog mDialog = null;

    // TODO: Remove face view logic if UX does not bring it back within a month.
    private final FaceView mFaceView;
    private final Level mLevel;
    private DecodeImageForReview mDecodeTaskForReview = null;

    private float mZoomMax;
    private float mZoomMin;
    private float[] mZoomRatioSection;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    protected float mAspectRatio = UNSET;
    public int mWidth;
    public int mHeight;

    private ImageView mIntentReviewImageView;
    private AIDetectionController mAIController;//SPRD:Add for ai detect

    private ShutterButton mShutterButton;//SPRD:fix bug 473462

    protected PhotoModule mBasicModule;
    protected TextView mHdrTips;
    protected LightPortraitOverLay mLightPortraitOverLay;

    private final GestureDetector.OnGestureListener mPreviewGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
        @Override
        public void onLongPress(MotionEvent var1){
            mController.onLongPress(var1);
        }
    };
    private final DialogInterface.OnDismissListener mOnDismissListener
            = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            mDialog = null;
        }
    };
    protected CountDownView mCountdownView;

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right,
            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        mWidth = right - left;
        mHeight = bottom - top;
        if (mPreviewWidth != mWidth || mPreviewHeight != mHeight) {
            mPreviewWidth = mWidth;
            mPreviewHeight = mHeight;
        }
        setTransformMatrix();
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }
    @Override
    public void onPanelsHidden() {

    }
    @Override
    public void onPanelsShown() {

    }
    @Override
    public void onPreviewFlipped() {
        mController.updateCameraOrientation();
    }

    /**
     * Starts the countdown timer.
     *
     * @param sec seconds to countdown
     */
    public void startCountdown(int sec) {
        updateStartCountDownUI();
        mCountdownView.startCountDown(sec);
    }

    /**
     * Sets a listener that gets notified when the countdown is finished.
     */
    public void setCountdownFinishedListener(CountDownView.OnCountDownStatusListener listener) {
        if (mCountdownView != null) {
            mCountdownView.setCountDownStatusListener(listener);
        }
    }

    /**
     * Returns whether the countdown is on-going.
     */
    public boolean isCountingDown() {
        if (mCountdownView == null) {
            return false;
        }
        return mCountdownView.isCountingDown();
    }

    /**
     * Cancels the on-going countdown, if any.
     */
    public void cancelCountDown() {
        if (mCountdownView != null) {
            mCountdownView.cancelCountDown();
        }
        //Sprd:Fix bug782358
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, true);
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        if (mFaceView != null) {
            mFaceView.onPreviewAreaChanged(previewArea);
        }
        if (mCountdownView != null) {
            mCountdownView.onPreviewAreaChanged(previewArea);
        }
        setTransformMatrix();
    }

    public void showHdrTips(boolean show){
        if(((PhotoModule)mController).isAELock())
            return;
        if(show){
            Log.d(TAG,"show tips on");
            if(CameraUtil.isFdrSupport()){
                mHdrTips.setText("FDR");
            } else {
                mHdrTips.setText("HDR");
            }
            mHdrTips.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG,"show tips off");
            mHdrTips.setVisibility(View.GONE);
        }
    }

    public void showCanNotLockAETips(boolean show){
        if(show){
            mHdrTips.setText(R.string.can_not_lock_ae_and_af);
            mHdrTips.setVisibility(View.VISIBLE);
            mRootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mHdrTips.setVisibility(View.GONE);
                }
            },1500);
        } else {
            mHdrTips.setVisibility(View.GONE);
        }
    }

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private final int mOrientation;
        private final boolean mMirror;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            mData = data;
            mOrientation = orientation;
            mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Decode image in background.
            return PictureDecoder.decode(mData, DOWN_SAMPLE_FACTOR, mOrientation, mMirror);
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                mController.onShowImageDone();
                return;
            }
            /* SPRD: Fix bug 538131 scale the bitmap to fill in the screen @{ */
            if (mIntentReviewImageView instanceof RotateImageView) {
                ((RotateImageView) mIntentReviewImageView).enableScaleup();
            }
            /* @} */
            mIntentReviewImageView.setImageBitmap(bitmap);
            showIntentReviewImageView();
            mController.onShowImageDone();
            mDecodeTaskForReview = null;
        }
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        mFocusRing = (FocusRing) mRootView.findViewById(R.id.focus_ring);
        mChasingRing = (ChasingView) mRootView.findViewById(R.id.chasing_ring);
        mPreviewOverlay = (PreviewOverlay) mRootView.findViewById(R.id.preview_overlay);
        mHdrTips = (TextView) mRootView.findViewById(R.id.lock_ae_tip);
        mLightPortraitOverLay = (LightPortraitOverLay) mRootView.findViewById(R.id.light_portrait_overlay);

        // Show faces if we are in debug mode.
        /*SPRD:Modify for ai detect @{
        if (DebugPropertyHelper.showCaptureDebugUI()) {
        */
            mFaceView = (FaceView) mRootView.findViewById(R.id.face_view);
            mLevel = (Level) mRootView.findViewById(R.id.level);
        /*
        } else {
            mFaceView = null;
        }
        @} */

        if (mController.isImageCaptureIntent()) {
            initIntentReviewImageView();
        }

        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);//SPRD:fix bug 473462

        initUI();
        activity.getCameraAppUI().setDreamInterface(this);
    }

    private void initIntentReviewImageView() {
        mIntentReviewImageView = (ImageView) mRootView.findViewById(R.id.intent_review_imageview);
        mActivity.getCameraAppUI().addPreviewAreaChangedListener(
                new PreviewStatusListener.PreviewAreaChangedListener() {
                    @Override
                    public void onPreviewAreaChanged(RectF previewArea) {
                        FrameLayout.LayoutParams params =
                            (FrameLayout.LayoutParams) mIntentReviewImageView.getLayoutParams();
                        params.width = (int) previewArea.width();
                        params.height = (int) previewArea.height();
                        params.setMargins((int) previewArea.left, (int) previewArea.top, 0, 0);
                        mIntentReviewImageView.setLayoutParams(params);
                    }
                });
    }

    /**
     * Show the image review over the live preview for intent captures.
     */
    public void showIntentReviewImageView() {
        if (mIntentReviewImageView != null) {
            mIntentReviewImageView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide the image review over the live preview for intent captures.
     */
    public void hideIntentReviewImageView() {
        if (mIntentReviewImageView != null) {
            mIntentReviewImageView.setVisibility(View.INVISIBLE);
        }
    }


    public FocusRing getFocusRing() {
        return mFocusRing;
    }

    public ChasingView getChasingRing() {
        return mChasingRing;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1f) {
            aspectRatio = 1f / aspectRatio;
        }

        updateUI(aspectRatio);

        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            // Update transform matrix with the new aspect ratio.
            mController.updatePreviewAspectRatio(mAspectRatio);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if(((PhotoModule) mController).isUseSurfaceView()){
            return;
        }
        mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mController.onPreviewUIDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mController instanceof PhotoModule) {
            ((PhotoModule) mController).onSurfaceTextureUpdated();
        }
    }

    public void onCameraOpened(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
        initializeExposureCompensation(capabilities);
        initExposureTimeScroll(capabilities);
    }

    protected void updateExposureTimeUI() {};
    protected ArrayList<String> mExposureTimeList = new ArrayList();
    private void initExposureTimeScroll(CameraCapabilities capabilities) {
        if (!CameraUtil.isManualShutterEnabled() || !DataModuleManager.getInstance(mActivity).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_EXPOSURE_SHUTTERTIME) || capabilities == null) {
            return;
        }
        mExposureTimeList.clear();
        mExposureTimeList.add(mActivity.getAndroidContext().getString(R.string.pref_exposure_shuttertime_default));
        ArrayList<Long> timeList = capabilities.getSupportedExposureTimeList();
        for (int i=1; i<timeList.size(); i++) {
            mExposureTimeList.add(mActivity.getAndroidContext().getString(R.string.exposure_time_names, "" + timeList.get(i)));
        }
        updateExposureTimeUI();
    }
    /*SPRD:fix bug 622818 add for exposure to adapter auto @{*/
    protected float mExposureCompensationStep = 0.0f;
    private void initializeExposureCompensation(CameraCapabilities capabilities) {
        if (!DataModuleManager.getInstance(mActivity).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_EXPOSURE) || capabilities == null) {
            return;
        }

        mExposureCompensationStep = capabilities.getExposureCompensationStep();
        updateExposureUI();
    }

    protected void updateExposureUI() {};
    /* @} */

    public void animateCapture(final byte[] jpegData, int orientation, boolean mirror) {
        // Decode jpeg byte array and then animate the jpeg
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.execute();
    }

    // called from onResume but only the first time
    public void initializeFirstTime() {

    }

    // called from onResume every other time
    public void initializeSecondTime(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
        if (mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        }
    }

    public void initializeZoom(CameraCapabilities capabilities, CameraSettings settings) {
        if(DataModuleManager.getInstance(mActivity).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) &&
                !DataModuleManager.getInstance(mActivity).getCurrentDataModule().getBoolean(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) || !CameraUtil.isZoomPanelEnabled()){
            mPreviewOverlay.resetZoomSimple();
            return;
        }
        if ((capabilities == null) || settings == null ||
                !capabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            mPreviewOverlay.resetZoomSimple();
            return;
        }
        mZoomMax = capabilities.getMaxZoomRatio();

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
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        // TODO: Need to setup a path to AppUI to do this
        /*add for w+t*/
        int cameraId = DataModuleManager.getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID);

        boolean isSimple = true;
        if (CameraUtil.isWPlusTAbility(mActivity,mActivity.getCurrentModuleIndex(),cameraId)){
            isSimple = false;
        }
        if (CameraUtil.isTcamAbility(mActivity,mActivity.getCurrentModuleIndex(),cameraId))
            isSimple = false;

        mPreviewOverlay.setupZoom(mZoomMax, settings.getCurrentZoomRatio(), mZoomRatioSection,
                new ZoomChangeListener(),isSimple);
    }

    public void resetZoomSimple(){
        if (mPreviewOverlay != null)
            mPreviewOverlay.resetZoomSimple();
    }

    public void animateFlash() {
        mController.startPreCaptureAnimation();
    }

    public boolean onBackPressed() {
        // In image capture mode, back button should:
        // 1) if there is any popup, dismiss them, 2) otherwise, get out of
        // image capture
        if (mController.isImageCaptureIntent()) {
            mController.onCaptureCancelled();
            return true;
        } else if (!mController.isCameraIdle()) {
            // ignore backs while we're taking a picture
            return true;
        } else {
            return false;
        }
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        mDecodeTaskForReview.execute();

        mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        pauseFaceDetection();
    }

    protected void hidePostCaptureAlert() {
        if (mDecodeTaskForReview != null) {
            mDecodeTaskForReview.cancel(true);
        }
        resumeFaceDetection();
    }

    public void setDisplayOrientation(int orientation) {
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(orientation);
        }
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

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public void onResume() {
        mPreviewOverlay.stopZoomState(); // Bug 1142384 , still press vol to zoom and switch camera
    }

    public void onPause() {
        showHdrTips(false);//Sprd:fix bug937739
        if (mFaceView != null) {
            mFaceView.clear();
        }
        if (mDialog != null) {
            mDialog.dismiss();
        }
        // recalculate aspect ratio when restarting.
        mAspectRatio = 0.0f;
        mPreviewOverlay.stopZoomState(); // Bug 1142384 , still press vol to zoom and switch camera
    }

    public void clearFaces() {
        if (mFaceView != null) {
            mFaceView.clear();
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

    public void onStartFaceDetection(int orientation, boolean mirror) {
        if (mFaceView != null) {
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(orientation);
            mFaceView.setMirror(mirror);
            mFaceView.resume();
        }
    }

    /* SPRD:Add for ai detect @{ */
    private int mFaceSimleCount = 0;

    /* SPRD:Modify for add ai detect bug 474723 @{
    @Override
    public void onFaceDetection(Face[] faces, CameraAgent.CameraProxy camera) {
        if (mFaceView != null) {
            mFaceView.setFaces(faces);
        }
    }
    */

    protected void showFaceDetectedTips(boolean show){
    }
    public void updatePortraitBackgroundReplacementUI(int visible) {

    }
    private boolean isAiDetectChange = false;
    public void changeAiDetect(){
        isAiDetectChange = true;
    }
    @Override
    public void onFaceDetection(Face[] faces,int[] attributes,boolean faceChange) {

        if(faces.length == 0 && mFaceView != null && !mFaceView.isPause()){//SPRD:fix bug1177012
            showFaceDetectedTips(true);
        }

        if ((mAIController == null || isNeedClearFaceView()) && mFaceView != null) {//now face is not mutex with ai in UE's doc. // Bug 1115422
            mFaceView.clear();
            return;
        }

        // SPRD: Add for new feature VGesture but just empty interface
        if (mFaceView != null && faces != null) {
            /* SPRD: Fix bug 1105014 face attibute change and add enable tag @{ */
            if (((PhotoModule)mController).isAELock()){
                mFaceView.clear();
            }else if ((((PhotoModule)mController).mFocusManager.getMeteringAreas()!=null)){
                if (mAIController.isChooseAttributes()){
                    mFaceView.setShowAttributeOnly();
                    mFaceView.setFaces(faces,attributes);
                }else {
                    mFaceView.clear();
                }
            }else{
                if (mAIController.isChooseAttributes())
                    mFaceView.setFaces(faces,attributes);
                else
                    mFaceView.setFaces(faces);
            }

            if ((faceChange || isAiDetectChange) && faces.length != 0 && !mAIController.isChooseAttributes()) {//SPRD:Bugfix 1106096)
                showFaceDetectedTips(false);
                mFaceView.disappearFaceView();
            }

            isAiDetectChange = false;
            mFaceView.setNeedShowFace(mAIController.isChooseAttributes());
            if (mAIController.isChooseSmile()) {
                if (isCountingDown()) {
                    return;
                }
                int length = faces.length;
                int smileScore = 0;
                for (int i = 0, len = faces.length; i < len; i++) {
                    Log.i(TAG, " len=" + len + " faces[" + i + "].score=" + faces[i].score);
                    // mAIController.resetSmileScoreCount(faces[i].score >= 90);
                    // SPRD: Fix bug 536674 The smile face score is low.
                    if (faces[i].score > CameraUtil.getSmileScoreThreshold() && faces[i].score < 100) {
                        smileScore = faces[i].score;
                    }
                }
                mAIController.resetSmileScoreCount(smileScore != 0);//SPRD:fix bug1141088
                if (isSmileAvailabe() && mAIController.getSmileScoreCount() > 5) {
                    mAIController.setSmileScoreCount(AIDetectionController.SMILE_STEP_MAX + 1);//SPRD:fix bug1116153
                    Log.i(TAG, "smileScore = " + smileScore + " Do Capture ... ...");
                    //SPRD: fix bug 613650 close smile capture when open camera settings
                    if (mActivity.getCameraAppUI().isSettingLayoutOpen()) {
                        return;
                    }
                    mController.setSmileCapture(true);
                    mController.onShutterButtonClick();
                        /* SPRD:fix bug 530633 setCaptureCount after capture @*/
                    mController.setCaptureCount(0);
                    mActivity.enableKeepScreenOn(false);//Sprd:Fix bug940243
                }
            }
        }
    }

    private boolean isSmileAvailabe() {
        SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
        return (!mActivity.isPaused() && st != null && mController.isShutterEnabled()
                && !mFaceView.isPause());
    }

    public void intializeAIDetection(DataModuleBasic dataModuleCurrent) {

        mAIController = new AIDetectionController();
        mAIController.getSmileValue(mBasicModule.mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AI_DETECT_SMILE) && dataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_SMILE));
        mAIController.getFaceAttributesValue(mBasicModule.mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES) && dataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES));
    }
    /* @} */

    /* SPRD:fix bug 473462 add burst capture @*/
    private OnScreenHint mBurstHint;

    public void showBurstScreenHint(int count) {
        String message = String.format(mActivity.getResources().getQuantityString(R.plurals.burst_mode_saving, count, count));
        if (mBurstHint == null) {
            mBurstHint = OnScreenHint.makeText(mActivity, message);
        } else {
            mBurstHint.setText(message);
        }
        mBurstHint.show();
    }

    public void dismissBurstScreenHit() {
        if (mBurstHint != null) {
            mBurstHint.cancel();
            mBurstHint = null;
            enablePreviewOverlayHint(true);
        }
    }

    public boolean isReviewShow() {// SPRD BUG:402084
        if (mIntentReviewImageView == null) {
            return false;
        }
        return mIntentReviewImageView.getVisibility() == View.VISIBLE;
    }

    public boolean isOnTouchInside(MotionEvent ev, int pointId) {
        if (mShutterButton == null) {
            mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);//SPRD:fix bug 473462
        }
        if (mShutterButton != null) {
            return mShutterButton.isOnTouchInside(ev, pointId);
        }
        return false;
    }
    /* @}*/

    /* SPRD:Fix bug 411062 @{ */
    public void enablePreviewOverlayHint(boolean enable) {
        mPreviewOverlay.setDetector(enable);
    }
    /* @} */

    public boolean isEnableDetector() {
        if (mPreviewOverlay == null) {
            return true;
        }
        return mPreviewOverlay.isEnableDetector();
    }

    /*SPRD:Fix bug 502060 hide the ZoomProcessor if it shown after the button clicked @{*/
    public void hideZoomProcessorIfNeeded() {
        mPreviewOverlay.hideZoomProcessorIfNeeded();
    }
    /* @} */

    public void showZoomProcessorIfNeeded() {
        mPreviewOverlay.showZoomProcessorIfNeeded();
    }

    public void setButtonOrientation(int orientation) {
        Log.i(TAG, "setButtonOrientation   orientation = "+orientation);

        FrameLayout.LayoutParams layoutBeautiy = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER);

        float margin = mActivity.getResources().getDimension(R.dimen.filter_make_up_button_magin);
        float buttomsize = mActivity.getResources().getDimension(R.dimen.filter_make_up_button_half_size);

        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        // CID 123679 : DLS: Dead local store (FB.DLS_DEAD_LOCAL_STORE)
        // int height = display.getHeight();
        float mPort = width/2 - margin - buttomsize;
        // CID 123679 : DLS: Dead local store (FB.DLS_DEAD_LOCAL_STORE)
        // float mLand = height/2 - margin - buttomsize;

        /*
         * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
         * Original Android code:

        if (orientation == 0){
            layoutBeautiy.setMargins((int)mPort,0,0,0);
        } else if(orientation == 90){
            layoutBeautiy.setMargins(0,0,0,(int)mLand);
        } else if(orientation == 180){
            layoutBeautiy.setMargins(0,0,(int)mPort,0);
        } else if(orientation == 270){
            layoutBeautiy.setMargins(0,(int)mLand,0,0);
        }

         */
    }

    /*
     * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
     */
    public void showPanels() {

    }

    public void hidePanels() {

    }
    /* @} */
    public void setLevelVisiable(boolean visiable){
        if(visiable) {
            mLevel.setVisibility(View.VISIBLE);
        }else {
            mLevel.setVisibility(View.GONE);
        }
    }

    public void initUI() {
        mBasicModule = (PhotoModule) mController;
        long start = System.currentTimeMillis();
        updateTopPanelValue(mActivity);
        long end = System.currentTimeMillis();
        Log.i(TAG, "initUI cost: " + (end - start));
    }

    public ViewGroup getModuleRoot(){
        return (ViewGroup) mRootView.findViewById(R.id.dream_module_layout);
    }

    // UI CHECK 71
    public void updateStartCountDownUI() {
        mActivity.getCameraAppUI().updatePreviewUI(View.GONE, true);
    }

    /**
     * Extract code into function
     *
     * @{
     */
    public boolean isNeedClearFaceView() {
        return (!mBasicModule.mFaceDetectionStarted || mBasicModule.isAudioRecording());//SPRD:fix bug957181/1006791/1115422
    }

    public View getRootView() {
        return mRootView;
    }
    /**
     * @}
     */

    /*
     * Add for ui check 122 @{
     */
    public void onOrientationChanged(OrientationManager orientationManager,
            OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();
        DreamOrientation.setOrientation(mRootView, orientation, true);
        mLightPortraitOverLay.setOrientation(orientation);

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
    /*
     * @}
     */

    public void onSettingReset() {}
    public boolean getBurstHintVisibility() {
        return mBurstHint != null;
    }

    public void setButtonVisibility(int buttonId, int visibility) {
        ((ButtonManagerDream)mActivity.getButtonManager()).setButtonVisibility(buttonId,visibility);
        if(visibility==View.GONE) {
            try {
                mActivity.getButtonManager().getButtonOrError(buttonId);
                switch (buttonId) {
                    case ButtonManagerDream.BUTTON_HDR_DREAM:
                        updateHDRorFLASHView();
                        break;
                    case ButtonManagerDream.BUTTON_FLASH_DREAM:
                        updateHDRorFLASHView();
                        break;
                    case ButtonManagerDream.BUTTON_REFOCUS_DREAM:
                        updateREFOCUSView();
                        break;
                    case ButtonManagerDream.BUTTON_FILTER_DREAM:
                        updateFilterView();
                        break;
                    case BUTTON_MONTIONPHOTO_DREAM:
                        updateMotionPhotoView();
                        break;
                }
            } catch (Exception e) {
            }
        }
    }

    private void updateFilterView() {
        View mRootView=mActivity.getModuleLayoutRoot();
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        View filterTapView=(View)topPanelParent.findViewById(R.id.filter_view);
        if(filterTapView != null){
            filterTapView.setVisibility(View.GONE);
        }
    }

    private void updateMotionPhotoView() {
        View mRootView=mActivity.getModuleLayoutRoot();
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        View montionPhotoTapView=(View)topPanelParent.findViewById(R.id.montionphoto_view);
        if(montionPhotoTapView != null){
            montionPhotoTapView.setVisibility(View.GONE);
        }
    }

    public void updateHDRorFLASHView(){
        View mRootView=mActivity.getModuleLayoutRoot();
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        View HDRorFlashTopView=(View)topPanelParent.findViewById(R.id.hdr_view);
        HDRorFlashTopView.setVisibility(View.GONE);
    }
    public void updateREFOCUSView(){
        View mRootView=mActivity.getModuleLayoutRoot();
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        View REFOCUSTopView=(View)topPanelParent.findViewById(R.id.refocus_view);
        REFOCUSTopView.setVisibility(View.GONE);
    }


    protected void setPictureInfo(int width, int height, int orientation){}//SPRD:fix bug 625571

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
    //Sprd Fix Bug: 665197
    public boolean isZooming(){
        if (mPreviewOverlay == null) {
            return false;
        }
        return mPreviewOverlay.isZooming();
    }

    /* SPRD: Fix bug 659315, optimize camera launch time @{ */
    private boolean mHasDelayWorkOnInit = true;

    public void onPreviewStarted() {
        long start = System.currentTimeMillis();
        if (mHasDelayWorkOnInit) {
            mHasDelayWorkOnInit = false;

            mActivity.getCameraAppUI().inflateStub();

            inflateLayout();
            updateLayout();
            waitInflateEnd();

            updateTopPanel();
        }
        showZoomProcessorIfNeeded();
        long end = System.currentTimeMillis();
        Log.i(TAG, "onPreviewStarted cost: " + (end - start));
    }
    /* @} */
    protected void initModuleLayout() {
        ViewGroup moduleRoot = (ViewGroup) mRootView.findViewById(R.id.module_layout);
        moduleRoot.addView(mActivity.getLayoutInflater().inflate(R.layout.photo_module, null, false));

    }

    @Override
    public void initializeadjustFlashPanel(int maxValue, int minvalue,
            int currentValue) {
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

    /* SPRD: add for filter function @{ */
    public void setButtonOrientation(OrientationManager.DeviceOrientation DeviceOrientation) { }

    public void setTransformMatrix() {}
    public void updateUI(float aspectRatio) {}
    public void resetUI() {}
    /*  @} */

    /* SPRD: Fix bug 898421, The inheritance structure of the refactoring model @{ */
    @Override
    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS,View.VISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(
                SlidePanelManager.CAPTURE, false);
    }

    @Override
    public void updateBottomPanel() {
        mActivity.getCameraAppUI().updateSwitchModeBtn(this);
    }

    public void bindSettingsButton(View settingsButton) {
        if (settingsButton != null) {
            final DreamUIPreferenceSettingLayout dps = (DreamUIPreferenceSettingLayout) mRootView
                    .findViewById(R.id.dream_ui_preference_setting_layout);
            dps.changeModule(PhotoUI.this);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mBasicModule.isShutterClicked()) {
                        return;
                    }
                    // mActivity.onSettingsSelected();
                    /*
                     * DataStructSetting dataSetting = new DataStructSetting(
                     * DataConfig.CategoryType.CATEGORY_PHOTO, false,
                     * DataConfig.PhotoModeType.PHOTO_MODE_BACK_REFOCUS, 1); //
                     * change the data storage module
                     * DataModuleManager.getInstance
                     * (mActivity).changeModuleStatus(dataSetting);
                     */
                    // update UI
                    dps.changeVisibilty(View.VISIBLE);
                    mActivity.getCameraAppUI().updatePreviewUI(View.GONE, false);
                }
            });
        }
    }

    protected void bindAutoTopButtons() {
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();;
        }
        bindFlashButton();
        bindRefocusButton();
        bindFilterButton();
        bindHdrButton();
        bindCameraButton();
        bindMontionPhotoButton();
    }

    public void bindFlashButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.setFlashMode(DreamUtil.BACK_CAMERA != DreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID)),CameraUtil.getFrontFlashMode(), mActivity.getCurrentModuleIndex());
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_FLASH_DREAM,
                mBasicModule.mFlashCallback);
    }

    public void bindCountDownButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_COUNTDOWN_DREAM, null);
    }

    public void bindFilterButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_FILTER_DREAM, null);
    }

    public void bindRefocusButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_REFOCUS_DREAM, null);
    }

    public void bindMontionPhotoButton(){
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(BUTTON_MONTIONPHOTO_DREAM,null);
    }

    public void bindHdrButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_HDR_DREAM,null);
    }

    public void bindCameraButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_CAMERA_DREAM,
                mBasicModule.mCameraCallback);
    }
    public void bindSwitchPreviewButton(View settingsButton) {
        if (settingsButton != null) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mBasicModule.isShutterClicked()) {
                        return;
                    }
                    mBasicModule.switchPreview();
                }
            });
        }
    }
    public void bindMeteringButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_METERING_DREAM, null);
    }

    public void bindMakeUpDisplayButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_MAKE_UP_DISPLAY_DREAM,
                mBasicModule.mMakeUpDisplayCallback);
    }

    public void bindLightPortraitDisplayButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_LIGHT_PORTRAIT_DISPLAY_DREAM,
                mBasicModule.mLightPortraitDisplayCallback);
    }

    public void bindPortraitRefocusButton(){
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_PORTRAIT_REFOCUS_DREAM,CameraUtil.isPortraitRefocusEnable()
                && DreamUtil.BACK_CAMERA == DreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID)) ? mBasicModule.mPortraitRefocusDisplayCallback : null);
    }

    private ButtonManager.ButtonCallback getDisableCameraButtonCallback(
            final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mActivity.getButtonManager().disableButton(conflictingButton);
            }
        };
    }

    /**
     * update preview ui after settings closed
     */
    public void onSettingUIHide() {
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, false);
    }

    /**
     * MakeupControl begin
     *
     * @{
     */
    private long mLastTime = 0;

    public void initMakeupControl(ViewGroup extendPanelParent) {
        LayoutInflater lf = LayoutInflater.from(mActivity);
        lf.inflate(R.layout.sprd_autophoto_extend_panel, extendPanelParent);

        mMakeupController = new MakeupController(extendPanelParent,
                mController,mActivity);
    }

    public void initLightPortraitControl(ViewGroup extendPanelParent) {
        LayoutInflater lf = LayoutInflater.from(mActivity);

        lf.inflate(R.layout.layout_light_portrait_scroller, extendPanelParent);
        mLightPortraitControllerDream = new LightPortraitControllerDream(extendPanelParent,
                mBasicModule,mActivity);
    }


    public void updateLayout(){
        CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            public void run() {
                mCountdownView = (CountDownView) mRootView.findViewById(R.id.count_down_view);
                setCountdownFinishedListener((CountDownView.OnCountDownStatusListener)mBasicModule);

                mActivity.getCameraAppUI().updateButtomBar();

                mActivity.getCameraAppUI().addShutterListener();

                // Update icons on bottom panel.
                updateBottomPanel();

                // Update item on slide panel.
                updateSlidePanel();

                mActivity.getCameraAppUI().initializeBottomBarSpec();

                latch.countDown();
            }
        });
    }

    public void inflateLayout(){
        ViewGroup extendPanelParent =mActivity.getCameraAppUI().getExtendPanelParent();
        extendPanelParent.removeAllViews();

        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);
        topPanelParent.removeAllViews();

        CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            public void run() {
                fitExtendPanel(extendPanelParent);

                fitTopPanel(topPanelParent);
                ((TopPanelListener)mController).updateTopPanel();

                latch.countDown();
            }
        });
        initializeViewByIntent();//SPRD:fix bug926165

    }

    CountDownLatch latch = new CountDownLatch(2);

    public void waitInflateEnd(){
        try{
            latch.await();
        }catch (InterruptedException e){
            Log.e(TAG,e.toString());
        }

    }

  //Sprd:fix bug900869
    private void initializeViewByIntent() {
        if (mBasicModule != null && mBasicModule.mIsImageCaptureIntent) {
            /*
             * SPRD: fix bug 497077 If the mode of bottom bar is
             * MODE_INTENT_REVIEW, then the IntentReviewLayout should be shown
             *
             * @{
             */
            if (!mActivity.getCameraAppUI().isInIntentReview()) {
                mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            } else {
                mActivity.getCameraAppUI().transitionToIntentReviewLayout();
            }
            /* @} */
        }
    }
    public void updateTopPanel() {}

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if(mBasicModule.isBeautyCanBeUsed()) {
            initMakeupControl(extendPanelParent);
        }
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent){}
    /*  @} */

    public void showBeautyButton(boolean show) {}

    public void showMotionPhotoTipText(boolean visible){}

    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int index) {
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public int getAiSceneTip() {
        return -1;
    }

    private OnScreenHint mBlurtHint;

    public void showBlurScreenHint() {
        String message = mActivity.getResources().getString(R.string.blur_process_toast);
        if (mBlurtHint == null) {
            mBlurtHint = OnScreenHint.makeText(mActivity, message);
        } else {
            mBlurtHint.setText(message);
        }
        mBlurtHint.show();
    }

    public void dismissBlurScreenHit() {
        if (mBlurtHint != null) {
            mBlurtHint.cancel();
            mBlurtHint = null;
        }
    }

    @Override
    public void onLightPortraitStatusChange(int status) { }

    public void onLightPortraitEnable(boolean enable) { }

    public void showLightPortraitOverLay(boolean b){}

    public void showPortraitRefocusOverLay(boolean b){}


    public void enableUIAfterTakepicture() {
        enableUIWhenTakePicture(true);
    }

    public void disableUIWhenTakepicture() {
        enableUIWhenTakePicture(false);
    }

    private void enableUIWhenTakePicture(boolean enable){
        // manual ui
        if(this instanceof ManualPhotoUI){
            ((ManualPhotoUI) this).setManualControlEnable(enable);
        }

        // filter ui
        if(this instanceof FilterModuleUISprd){
            ((FilterModuleUISprd) this).setFilterEnable(enable);
        }

        // blur number
        if(this instanceof BlurRefocusUI){
            ((BlurRefocusUI) this).setFNumberEnable(enable);
        }

        // makeup ui
        if(mMakeupController != null){
            mMakeupController.setEnable(enable);
        }

        // lightportrait ui
        if(mLightPortraitControllerDream != null){
            mLightPortraitControllerDream.setEnable(enable);
        }
    }

}

