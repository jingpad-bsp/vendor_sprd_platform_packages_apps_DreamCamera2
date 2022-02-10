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

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.media.MediaActionSound;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.camera.app.AppController;
import com.android.camera.app.MotionManager;
import com.android.camera.debug.Log;
import com.android.camera.one.Settings3A;
import com.android.camera.settings.Keys;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.ui.focus.ChasingView;
import com.android.camera.util.ApiHelper;
import com.android.camera.ui.focus.CameraCoordinateTransformer;
import com.android.camera.ui.focus.FocusRing;
import com.android.camera.util.CameraUtil;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.dream.camera.settings.DataModuleManager;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/* A class that handles everything about focus in still picture mode.
 * This also handles the metering area because it is the same as focus area.
 *
 * The test cases:
 * (1) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is not in progress.
 * (2) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is in progress.
 * (3) The camera has face detection. Point the camera at some faces. Hold the
 *     shutter. Release to take a picture.
 * (4) The camera has face detection. Point the camera at some faces. Single tap
 *     the shutter to take a picture.
 * (5) The camera has autofocus. Single tap the shutter to take a picture.
 * (6) The camera has autofocus. Hold the shutter. Release to take a picture.
 * (7) The camera has no autofocus. Single tap the shutter and take a picture.
 * (8) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Take a picture.
 * (9) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Wait until it times out.
 * (10) The camera has no autofocus and supports metering area. Touch the screen
 *     to change metering area.
 */
public class FocusOverlayManager implements PreviewStatusListener.PreviewAreaChangedListener,
        MotionManager.MotionListener {
    private static final Log.Tag TAG = new Log.Tag("FocusOverlayMgr");

    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int TOUCH_EV_FOCUS_DISPLAY = 1;

    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = Settings3A.getFocusHoldMillis();
    private static final int TOUCH_EV_FOCUS_DISPLAY_DELAY_MILLIS = 500;

    public static final float AF_REGION_BOX = Settings3A.getAutoFocusRegionWidth();
    public static final float AE_REGION_BOX = Settings3A.getMeteringRegionWidth();

    private int mState = STATE_IDLE;
    private static final int STATE_IDLE = 0; // Focus is not active.
    private static final int STATE_FOCUSING = 1; // Focus is in progress.
    // Focus is in progress and the camera should take a picture after focus finishes.
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_SUCCESS = 3; // Focus finishes and succeeds.
    private static final int STATE_FAIL = 4; // Focus finishes and fails.

    private boolean mInitialized;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mLockAeAwbNeeded;
    private boolean mAeAwbLock;
    private boolean mAutoChasingSupported;
    private CameraCoordinateTransformer mCoordinateTransformer;

    private boolean mMirror; // true if the camera is front-facing.
    private int mDisplayOrientation;
    private List<Area> mFocusArea; // focus area in driver format
    private List<Area> mMeteringArea; // metering area in driver format
    private List<Area> mRefocusArea;
    private List<Area> mChasingRegion; // auto tracking focus and max three point in the further
    private List<Area> mTouchArea;
    private CameraCapabilities.FocusMode mFocusMode;
    private final List<CameraCapabilities.FocusMode> mDefaultFocusModes;
    private CameraCapabilities.FocusMode mOverrideFocusMode;
    private CameraCapabilities mCapabilities;
    private final AppController mAppController;
    private final Handler mHandler;
    Listener mListener;
    TouchListener mTouchListener;
    private boolean mPreviousMoving;
    private final FocusRing mFocusRing;
    private final ChasingView mChasingView;
    private final Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private boolean mFocusLocked;
    private boolean mAutoChasingEnable;// enable auto chasing


    private MediaActionSound mCameraSound;

    /** Manual tap to focus parameters */
    private TouchCoordinate mTouchCoordinate;
    private long mTouchTime;
    private boolean isAFLock = false;
    private boolean mIsSupportTEV = false;

    public interface Listener {
        public void autoFocus();
        public void cancelAutoFocus();
        public boolean capture();
        public void startFaceDetection();
        public void stopFaceDetection();
        public void setFocusParameters();
        public void setAELock(boolean value);
        public void setAutoChasingParameters();
        public boolean isManualMode();
        public boolean isTouchCapture();//SPRD:fix bug1405038
    }
    /* Dream Camera test ui check 20 @{ */
    public interface TouchListener {
        public void touchCapture();
    }
    /* @} */

    /**
     * TODO: Refactor this so that we either don't need a handler or make
     * mListener not be the activity.
     */
    private static class MainHandler extends Handler {
        /**
         * The outer mListener at the moment is actually the CameraActivity,
         * which we would leak if we didn't break the GC path here using a
         * WeakReference.
         */
        final WeakReference<FocusOverlayManager> mManager;
        public MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            mManager = new WeakReference<FocusOverlayManager>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            FocusOverlayManager manager = mManager.get();
            if (manager == null) {
                return;
            }

            switch (msg.what) {
                case RESET_TOUCH_FOCUS: {
                    manager.cancelAutoFocus();
                    //manager.mListener.startFaceDetection();//SPRD:Modify for ai detect
                    break;
                }
                case TOUCH_EV_FOCUS_DISPLAY: {
                    manager.showTouchEv();
                    break;
                }
            }
        }
    }

    public FocusOverlayManager(AppController appController,
            List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities,
            Listener listener, boolean mirror, Looper looper, FocusRing focusRing) {
        mAppController = appController;
        mHandler = new MainHandler(this, looper);
        mDefaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>(defaultFocusModes);
        updateCapabilities(capabilities);
        mListener = listener;
        setMirror(mirror);
        mFocusRing = focusRing;
        mFocusLocked = false;
        mChasingView = null;
        mAutoChasingEnable = false;
    }
    /* Dream Camera test ui check 20 @{ */
    public FocusOverlayManager(AppController appController,
            List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities,
            Listener listener, boolean mirror, Looper looper, FocusRing focusRing, TouchListener touchListener) {
        mAppController = appController;
        mHandler = new MainHandler(this, looper);
        mDefaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>(defaultFocusModes);
        updateCapabilities(capabilities);
        mListener = listener;
        setMirror(mirror);
        mFocusRing = focusRing;
        mFocusLocked = false;
        mChasingView = null;
        mAutoChasingEnable = false;
        mTouchListener = touchListener;
    }

    public FocusOverlayManager(AppController appController,
                               List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities,
                               Listener listener, boolean mirror, Looper looper, FocusRing focusRing,
                               ChasingView chasingView, boolean autoChasingEnable) {
        mAppController = appController;
        mHandler = new MainHandler(this, looper);
        mDefaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>(defaultFocusModes);
        updateCapabilities(capabilities);
        mListener = listener;
        setMirror(mirror);
        mFocusRing = focusRing;
        mFocusLocked = false;
        mChasingView = chasingView;
        mAutoChasingEnable = autoChasingEnable;
        mTouchListener = null;
    }

    public FocusOverlayManager(AppController appController,
                               List<CameraCapabilities.FocusMode> defaultFocusModes, CameraCapabilities capabilities,
                               Listener listener, boolean mirror, Looper looper, FocusRing focusRing,
                               ChasingView chasingView, boolean autoChasingEnable,TouchListener touchListener) {
        mAppController = appController;
        mHandler = new MainHandler(this, looper);
        mDefaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>(defaultFocusModes);
        updateCapabilities(capabilities);
        mListener = listener;
        setMirror(mirror);
        mFocusRing = focusRing;
        mFocusLocked = false;
        mChasingView = chasingView;
        mAutoChasingEnable = autoChasingEnable;
        mTouchListener = touchListener;
    }

        /* @} */
    public void updateCapabilities(CameraCapabilities capabilities) {
        // capabilities can only be null when onConfigurationChanged is called
        // before camera is open. We will just return in this case, because
        // capabilities will be set again later with the right capabilities after
        // camera is open.
        if (capabilities == null) {
            return;
        }
        mCapabilities = capabilities;
        mFocusAreaSupported = mCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA);
        mMeteringAreaSupported = mCapabilities.supports(CameraCapabilities.Feature.METERING_AREA);
        mLockAeAwbNeeded = (mCapabilities.supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK)
                || mCapabilities.supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK));
        mAutoChasingSupported = CameraUtil.isAutoChasingSupport();
    }

    /** This setter should be the only way to mutate mPreviewRect. */
    public void setPreviewRect(Rect previewRect) {
        if (!mPreviewRect.equals(previewRect)) {
            mPreviewRect.set(previewRect);
            mFocusRing.configurePreviewDimensions(CameraUtil.rectToRectF(mPreviewRect));
            mChasingView.configurePreviewDimensions(CameraUtil.rectToRectF(mPreviewRect));
            resetCoordinateTransformer();
            mInitialized = true;
        }
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        setPreviewRect(CameraUtil.rectFToRect(previewArea));
    }

    public void setMirror(boolean mirror) {
        mMirror = mirror;
        resetCoordinateTransformer();
    }

    public void setAutoChasingEnable(boolean enable){
        Log.d(TAG,"setAutoChasingEnable enable = " + enable);
        mAutoChasingEnable = enable;
    }

    public void onAutoChasingTraceRegion(int state, Rect rect){
        //tmp need draw trace circle // do not decide value of state yet
        if (mChasingView != null){
            Rect result = computePreviewFromCameraRectCoordinates(rect);
            mChasingView.setChasingTraceRegions(state, result);
            if (state == 2){
                Log.d(TAG,"onAutoChasingTraceRegion state = 2 cancel AutoChasing");
                cancelAutoChasing();
                mListener.setAutoChasingParameters();
            }

        }
        else {
            Log.d(TAG,"onAutoChasingTraceRegion mChasingView = null");
        }
    }

    public void cancelAutoChasing(){
        initializeAutoChasingRegion(0,0);
        mChasingView.clear();
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        resetCoordinateTransformer();
    }

    private void resetCoordinateTransformer() {
        if (mPreviewRect.width() > 0 && mPreviewRect.height() > 0) {
            mCoordinateTransformer = new CameraCoordinateTransformer(mMirror, mDisplayOrientation,
                  CameraUtil.rectToRectF(mPreviewRect));
        } else {
            Log.w(TAG, "The coordinate transformer could not be built because the preview rect"
                  + "did not have a width and height");
        }
    }


    private void lockAeAwbIfNeeded() {
        if (mLockAeAwbNeeded && !mAeAwbLock) {
            mAeAwbLock = true;
            mListener.setFocusParameters();
        }
    }

    private void unlockAeAwbIfNeeded() {
        if (mLockAeAwbNeeded && mAeAwbLock && (mState != STATE_FOCUSING_SNAP_ON_FINISH)) {
            mAeAwbLock = false;
            mListener.setFocusParameters();
        }
    }

    /* SPRD: fix bug 473602 add for half-press @{*/
    public void onShutterDown(CameraCapabilities.FocusMode currentFocusMode) {
        if (!mInitialized) return;

        boolean autoFocusCalled = false;
        if (needAutoFocusCall(currentFocusMode) && mState == STATE_IDLE) {
            // Do not focus if touch focus has been triggered.
            if (mPreviewRect.width() == 0 || mPreviewRect.height() == 0) {
                return;
            }

            autoFocus();
            autoFocusCalled = true;
        }

        if (!autoFocusCalled) lockAeAwbIfNeeded();
    }

    public void updateFocusUI() {
        Log.i(TAG, "updateFocusUI mInitialized="+mInitialized+" mState="+mState);
        if (!mInitialized) {
            // Show only focus indicator or face indicator.
            return;
        }
        if (mState == STATE_IDLE) {
            if (mFocusArea != null) {
                // Users touch on the preview and the indicator represents the
                // metering area. Either focus area is not supported or
                // autoFocus call is not required.
                mFocusRing.startActiveFocus();
            }
        } else if (mState == STATE_FOCUSING || mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            if (mFocusArea == null) {
                mFocusRing.centerFocusLocation();
            }
            mFocusRing.startActiveFocus();
        } else {
            updateFocusFinsh();
        }
    }

    private void updateFocusFinsh() {
        if (!mIsSupportTEV) {
            if (mListener.isManualMode() && mFocusArea != null && !mListener.isTouchCapture()) {
                if (!mFocusRing.isAEAFDraging()) {
                    mFocusRing.showAEAFFocus();
                }
            } else {
                mFocusRing.startActiveFocusedFocus();
            }
        }
        /* @}*/
        if (mState == STATE_SUCCESS) {
            if (mFocusArea != null) {
                if (mAppController.isPlaySoundEnable() && !mFocusRing.isAEAFDraging()) {
                    mCameraSound.play(MediaActionSound.FOCUS_COMPLETE);
                }
            } else if (!mIsSupportTEV) {
                mFocusRing.centerFocusLocation();
            }
        }
        /*SPRD:fix bug596278 touch capture can not work when af fail @{*/
        if (mTouchListener != null) {
            mTouchListener.touchCapture();
        }
    }

    public void onShutterUp(CameraCapabilities.FocusMode currentFocusMode) {
        if (!mInitialized || mFocusRing.isAEAFDraging()) {
            return;
        }

        if (needAutoFocusCall(currentFocusMode)) {
            // User releases half-pressed focus key.
            if (mState == STATE_FOCUSING || mState == STATE_SUCCESS
                    || mState == STATE_FAIL) {
                cancelAutoFocus();
            }
        }

        // Unlock AE and AWB after cancelAutoFocus. Camera API does not
        // guarantee setParameters can be called during autofocus.
        unlockAeAwbIfNeeded();
    }

    public void focusAndCapture(CameraCapabilities.FocusMode currentFocusMode) {
        if (!mInitialized) {
            return;
        }
        if(isAFLock && mState != STATE_FOCUSING){
            capture();
            return;
        }

        /**
         * SPRD:fix bug 473602 add for half-press @{
        if (!needAutoFocusCall(currentFocusMode)) {
         */
        if (!needAutoFocusCall(currentFocusMode) && mState != STATE_FOCUSING) {
        /**
         * @}
         */
            // Focus is not needed.
            Log.i(TAG, "Focus is not needed.");
            capture();
        } else if (mState == STATE_SUCCESS || mState == STATE_FAIL) {
            // Focus is done already.
            Log.i(TAG, "Focus is done already.");
            capture();
        } else if (mState == STATE_FOCUSING) {
            // Still focusing and will not trigger snap upon finish.
            Log.i(TAG, "till focusing and will not trigger snap upon finish.");
            mState = STATE_FOCUSING_SNAP_ON_FINISH;
        } else if (mState == STATE_IDLE) {
            autoFocusAndCapture();
        }
    }

    public void onAutoFocus(boolean focused, boolean shutterButtonPressed) {
        Log.i(TAG, "onAutoFocus focused:" + focused + ",mState:" + mState);
        /**SPRD:Bug1005594 set isAELock in advance @{*/

        if(isAFLock) {
            mListener.setAELock(true);
            mFocusRing.setTipTextVisible(true);
            mFocusRing.setSeekBarVisibility(true);
        }
        if (mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            if(!isAFLock && !mIsSupportTEV) {
                updateFocusUI();
            } else {
                if (mIsSupportTEV) {
                    updateFocusFinsh();
                    mFocusRing.setSeekBarVisibility(true);
                }
                mFocusRing.setAELockColor(true);
            }
            capture();
        } else if (mState == STATE_FOCUSING || mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            // If this is triggered by touch focus, cancel focus after a
            // while.
            if (mFocusArea != null) {
                mFocusLocked = true;
                if(!isAFLock && !mFocusRing.isAEAFDraging()) {
                    mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS);
                }
            }
            if(!isAFLock) {
                if (!mIsSupportTEV) {
                    updateFocusUI();//SPRD:fix bug 473602 add for half-press
                } else {
                    updateFocusFinsh();
                    mFocusRing.setSeekBarVisibility(true);
                    mFocusRing.setAELockColor(true);
                }
                autoChase();
            } else {
                mFocusRing.setAELockColor(true);
            }
            if (shutterButtonPressed) {
                // Lock AE & AWB so users can half-press shutter and recompose.
                lockAeAwbIfNeeded();
            }
        } else if (mState == STATE_IDLE) {
            // User has released the focus key before focus completes.
            // Do nothing.
        }
    }

    public void onAutoFocusMoving(boolean moving) {
        //moving == true, begin, moving == false, success/fail.
        Log.i(TAG, "onAutoFocusMoving moving = " + moving + ",mState =" + mState);
        if (!mInitialized) {
            return;
        }

        // Ignore if we have requested autofocus. This method only handles
        // continuous autofocus.
        if (mState != STATE_IDLE) {
            return;
        }

        // animate on false->true trasition only b/8219520
        if (moving && !mPreviousMoving) {
            // Auto focus at the center of the preview.
            mFocusRing.centerFocusLocation();
            mFocusRing.startPassiveFocus();
        /**
         * SPRD:fix bug594887
         * original code
        } else if (!moving && mFocusRing.isPassiveFocusRunning()) {
            mFocusRing.stopFocusAnimations();
         */
        } else if (!moving) {
            if (mFocusRing.isPassiveFocusRunning())
                mFocusRing.stopFocusAnimations();
            if (mPreviousMoving) {//SPRD:fix bug1171360
                mFocusRing.centerFocusLocation();
                mFocusRing.startPassiveFocusedFocus();
            }
        }
        mPreviousMoving = moving;
    }

    /** Returns width of auto focus region in pixels. */
    private int getAFRegionSizePx() {
        return (int) (Math.min(mPreviewRect.width(), mPreviewRect.height()) * AF_REGION_BOX);
    }

    /** Returns width of metering region in pixels. */
    private int getAERegionSizePx() {
        return (int) (Math.min(mPreviewRect.width(), mPreviewRect.height()) * AE_REGION_BOX);
    }
    //SPRD: Fix bug 1105014
    private int getTouchRegionSizePx() {
        return (int) (Math.min(mPreviewRect.width(), mPreviewRect.height()) * 0.01f);
    }
    public void onSingleTapUpInFrontBlur(int x, int y) {
        initializeFrontReocusAreas(x, y);
    }

    private void initializeFrontReocusAreas(int x, int y) {
        if (mRefocusArea == null) {
            mRefocusArea = new ArrayList<Area>();
            mRefocusArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        mRefocusArea.get(0).rect = computeCameraRectFromPreviewCoordinates(x, y, getAFRegionSizePx());
    }

    public List<Area> getFrontRefocusAreas() {
        return mRefocusArea;
    }

    public void initializeFocusAreas(int x, int y) {
        if (mFocusArea == null) {
            mFocusArea = new ArrayList<Area>();
            mFocusArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        mFocusArea.get(0).rect = computeCameraRectFromPreviewCoordinates(x, y, getAFRegionSizePx());
    }

    private void initializeAutoChasingRegion(int x, int y){
        if (mChasingRegion == null) {
            mChasingRegion = new ArrayList<Area>();
            mChasingRegion.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        if (x != 0 && y != 0)
            mChasingRegion.get(0).rect = computeCameraRectFromPreviewCoordinates(x, y, getAFRegionSizePx());
        else
            mChasingRegion.get(0).rect.setEmpty();
        Log.d(TAG,"initializeAutoChasingRegion Region = " + mChasingRegion.get(0).rect);
    }

    private boolean isAutoChasing(){
        if (mChasingRegion != null && !mChasingRegion.get(0).rect.isEmpty()){
            return true;
        }
        return false;
    }

    public List<Area> getAutoChasingRegion(){
        return mChasingRegion;
    }

    public void initializeMeteringAreas(int x, int y) {
        if (mMeteringArea == null) {
            mMeteringArea = new ArrayList<Area>();
            mMeteringArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        mMeteringArea.get(0).rect = computeCameraRectFromPreviewCoordinates(x, y, getAERegionSizePx());
    }
    /* SPRD: Fix bug 1105014 face attibute change and add enable tag @{ */
    public List<Area> initializeTouchRegions(int x,int y){
        if (mTouchArea == null){
            mTouchArea = new ArrayList<>();
            mTouchArea.add(new Area(new Rect(), 1));
        }

        mTouchArea.get(0).rect = computeCameraRectFromPreviewCoordinates(x, y, getTouchRegionSizePx());
        return mTouchArea;
    }
    /* @} */
    public void setAELockState(boolean value){
        isAFLock = value;
    }

    public boolean getAELockState(){
        return isAFLock;
    }

    public void onLongPress(int x, int y){
        if(mState == STATE_FOCUSING){
            Log.d(TAG,"state is focusing so can`t lock");
            return;
        }
        if (mAutoChasingSupported && mAutoChasingEnable){
            Log.d(TAG,"onLongPress auto chasing do not ae lock");
            return;
        }
        mIsSupportTEV = false;
        //first remove the TOUCH_FOCUS message
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
        mFocusRing.setAELockPanelPos((float) x,(float) y);
        mFocusRing.showAELockPanel();
        mFocusRing.setTipTextVisible(false);
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH || mState == STATE_FOCUSING ) {
            return;
        }
        if ((mFocusArea != null) && (mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            cancelAutoFocus();
        }
        if (mPreviewRect.width() == 0 || mPreviewRect.height() == 0) {
            return;
        }
        if (mFocusAreaSupported) {
            initializeFocusAreas(x, y);
            mFocusRing.setFocusLocation(x, y);
        }
        // Initialize mMeteringArea.
        if (mMeteringAreaSupported) {
            initializeMeteringAreas(x, y);
        }
        //mListener.stopFaceDetection();//bugfix 1034472

        /* SPRD:fix bug986452 unlock ae when longpress again @{*/
        if(isAFLock) {
            setAeAwbLock(false);
        }
        /* @} */
        isAFLock = true;
        mListener.setFocusParameters();
        longPressautoFocus(STATE_FOCUSING);
    }

    public void onSingleTapUpInManual(int x, int y) {
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH || mState == STATE_FOCUSING ) {
            return;
        }

        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            cancelAutoFocus();
        }
        if (mPreviewRect.width() == 0 || mPreviewRect.height() == 0) {
            return;
        }
        // Initialize mMeteringArea.
        if (mMeteringAreaSupported) {
            initializeMeteringAreas(x, y);
        }

        // Stop face detection because we want to specify focus and metering area.
        mListener.stopFaceDetection();

        // Set the focus area and metering area.(update ae region)
        mListener.setFocusParameters();
        // Just show the indicator in all other cases.
        // Reset the metering area in 4 seconds.
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
        mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS);
    }

    private void showTouchEv() {
        if (!mFocusAreaSupported) {
            mFocusRing.showAELockPanel();
        }
        mFocusRing.setSeekBarVisibility(true);
        mFocusRing.setAELockColor(true);
    }

    private Point lastSingleTap = new Point(0,0);

    public void onSingleTapUp(int x, int y) {
        onSingleTapUp(x, y, false);
    }

    public void onSingleTapUp(int x, int y, boolean supportTEV) {
        Log.i(TAG,"focus overlay onSingleTapUp" );
        /**
         * SPRD: fix bug473602 add for hal-press @{
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            return;
        }

        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_FOCUSING ||
                    mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            cancelAutoFocus();
        }
         */
        lastSingleTap.set(x,y);
        if(isAFLock) {
            mListener.setAELock(false);
            isAFLock = false;
        }
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH) { // Bug 1026624
            return;
        }
        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_FOCUSING ||
                mState == STATE_SUCCESS || mState == STATE_FAIL)) { // Bug 1026624
            cancelAutoFocus();
        }
        if (mPreviewRect.width() == 0 || mPreviewRect.height() == 0) {
            return;
        }
        /**
         * @}
         */
        // Initialize variables.
        if (mAutoChasingSupported && mAutoChasingEnable){
            boolean isChasing = isAutoChasing();
            if (isChasing) {
                initializeAutoChasingRegion(0,0);
                Log.d(TAG,"set chasing Region x = 0 , y = 0");
                mListener.setAutoChasingParameters();
            }
        }

        // Initialize mFocusArea.
        if (mFocusAreaSupported) {
            initializeFocusAreas(x, y);
            //SPRD:fix bug533976 add touch AE for FF
            //mFocusRing.startActiveFocus();
            mFocusRing.setFocusLocation(x, y);
        }
        // Initialize mMeteringArea.
        if (mMeteringAreaSupported) {
            initializeMeteringAreas(x, y);
        }

        /*SPRD:fix bug533976 add touch AE for FF
         * android original code
        mFocusRing.startActiveFocus();
        mFocusRing.setFocusLocation(x, y);
        */

        // Log manual tap to focus.
        mTouchCoordinate = new TouchCoordinate(x, y, mPreviewRect.width(), mPreviewRect.height());
        mTouchTime = System.currentTimeMillis();

        // Stop face detection because we want to specify focus and metering area.
        //mListener.stopFaceDetection();//bugfix 1034472

        // Set the focus area and metering area.
        mListener.setFocusParameters();
        mIsSupportTEV = supportTEV;
        if (mIsSupportTEV) {
            mFocusRing.setAELockPanelPos((float) x,(float) y);
            if (mFocusAreaSupported) {
                mFocusRing.showAELockPanel();
            }
            mFocusRing.setTipTextVisible(false);
            if (mFocusRing.isPassiveFocusRunning()) {
                mFocusRing.stopFocusAnimations();//SPRD:fix bug1407610
            }
        }
        if (mFocusAreaSupported) {
            if (mListener.isManualMode()) {
                mFocusRing.setAEAFPanelPos((float) x,(float) y);
            }
            autoFocus();
        } else {  // Just show the indicator in all other cases.
            // Reset the metering area in 4 seconds.
            if (mIsSupportTEV) {
                mHandler.removeMessages(TOUCH_EV_FOCUS_DISPLAY);
                mHandler.sendEmptyMessageDelayed(TOUCH_EV_FOCUS_DISPLAY, TOUCH_EV_FOCUS_DISPLAY_DELAY_MILLIS);
            }
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
            mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS + TOUCH_EV_FOCUS_DISPLAY_DELAY_MILLIS);
        }
    }

    public void onPreviewStarted() {
        /* SPRD:fix bug669464 should not change state when capture,otherwise app will not capture when af back @{ */
        if (mState != STATE_FOCUSING_SNAP_ON_FINISH && !mFocusRing.isAEAFDraging()) {
            mState = STATE_IDLE;
        }
        /* @} */
        // Avoid resetting touch focus if N4, b/18681082.
        if (!ApiHelper.IS_NEXUS_4 && !mFocusRing.isAEAFDraging()) {
            resetTouchFocus();
        }
        /*
         * SPRD: Fix bug 659315, optimize camera launch time @{
         *
        if (mCameraSound == null){
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }
         */
    }

    public void onPreviewStopped() {
        // If auto focus was in progress, it would have been stopped.
        mState = STATE_IDLE;
    }

    public void onCameraReleased() {
        onPreviewStopped();
        if (mCameraSound != null) {
            mCameraSound.release();
            mCameraSound = null;
        }
    }

    @Override
    public void onMoving() {
        if (mFocusLocked) {
            Log.d(TAG, "onMoving: Early focus unlock.");
            cancelAutoFocus();
        }
    }

    /**
     * Triggers the autofocus and sets the specified state.
     *
     * @param focusingState The state to use when focus is in progress.
     */
    private void autoFocus(int focusingState) {
        Log.i(TAG, "autoFocus focusingState:" + focusingState);
        mListener.autoFocus();
        mState = focusingState;
        if (!mIsSupportTEV && !mFocusRing.isAEAFDraging()) {
            updateFocusUI();//SPRD:fix bug 594887
        }
        mHandler.removeMessages(RESET_TOUCH_FOCUS);

        initializeCameraSound();
    }

    private void longPressautoFocus(int focusingState) {
        Log.i(TAG, "longPressautoFocus focusingState:" + focusingState);
//        isAFLock = true;
        mListener.autoFocus();
        mState = focusingState;
        initializeCameraSound();
    }

    //Sprd:Fix bug853773
    private void initializeCameraSound(){
        /* SPRD: Fix bug 659315, optimize camera launch time @{ */
        if (mCameraSound == null) {
            mCameraSound = new MediaActionSound();
            mCameraSound.load(MediaActionSound.FOCUS_COMPLETE);
        }
        /* @} */
    }
    /**
     * Triggers the autofocus and set the state to indicate the focus is in
     * progress.
     */
    public void autoFocus() {
        autoFocus(STATE_FOCUSING);
    }

    /**
     * Triggers the autofocus and set the state to which a capture will happen
     * in the following autofocus callback.
     */
    private void autoFocusAndCapture() {
        Log.i(TAG, "autoFocusAndCapture.");
        autoFocus(STATE_FOCUSING_SNAP_ON_FINISH);
    }

    /* SPRD: fix bug 498954 If the function of flash is on and the camera is counting
     * down, the flash should not run here but before the capture.@{*/
    public void focusAfterCountDownFinishWhileFlashOn() {
        Log.i(TAG, "focusAfterCountDownFinishWhileFlashOn. mState = " + mState);
        /* SPRD: fix bug798240 should not af when after taf in 3s @{*/
        if(mState == STATE_SUCCESS || mState == STATE_FAIL ||isAFLock && mState != STATE_FOCUSING) {
            capture();
        } else if (mState == STATE_FOCUSING) {
            Log.i(TAG, "auto focus before capture, till focusing and will not trigger snap upon finish.");
            mState = STATE_FOCUSING_SNAP_ON_FINISH;//SPRD:fix bug1314541
        } else {
            autoFocusAndCapture();
        }
        /* @} */
    }
    /* @} */

    /* SPRD: fix bug847646
     * used for blur version 2 & 3
     */
    public void forceFocusBeforeCapture(){
        autoFocusAndCapture();
    }

    private void cancelAutoFocus() {
        Log.v(TAG, "Cancel autofocus.");
        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        /* SPRD:fix bug1154897 make sure cancelautofocus before applysetting @{ */
        clearAfArea();
        mMeteringArea = null;
        mListener.cancelAutoFocus();
        /* @} */
        resetTouchFocus();
        mState = STATE_IDLE;
        mFocusLocked = false;
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    private void capture() {
        if (mListener.capture()) {
            if (!mFocusRing.isAEAFDraging()) {
                mState = STATE_IDLE;
            }
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }
    }

    private void autoChase(){
        // Initialize variables.
        if (mAutoChasingSupported && mAutoChasingEnable){
//            boolean isChasing = isAutoChasing();
//            if (isChasing) {
//
//                x = 0;
//                y = 0;
//            }
            Log.d(TAG,"set chasing Region x = " + lastSingleTap.x +" , y = " + lastSingleTap.y);
            initializeAutoChasingRegion(lastSingleTap.x,lastSingleTap.y);
            mListener.setAutoChasingParameters();
        }
    }

    public CameraCapabilities.FocusMode getFocusMode(
            final CameraCapabilities.FocusMode currentFocusMode) {
        if (mOverrideFocusMode != null) {
            Log.i(TAG, "returning override focus: " + mOverrideFocusMode);
            return mOverrideFocusMode;
        }
        if (mCapabilities == null) {
            Log.i(TAG, "no capabilities, returning default AUTO focus mode");
            return CameraCapabilities.FocusMode.AUTO;
        }

        if (mFocusAreaSupported && mFocusArea != null) {
            Log.i(TAG, "in tap to focus, returning AUTO focus mode");
            // Always use autofocus in tap-to-focus.
            mFocusMode = CameraCapabilities.FocusMode.AUTO;
        } else {
            String focusSetting = DataModuleManager
                    .getInstance(mAppController.getAndroidContext())
                    .getDataModuleCamera().getString(Keys.KEY_FOCUS_MODE);

            Log.i(TAG, "stored focus setting for camera: " + focusSetting);
            // The default is continuous autofocus.
            mFocusMode = mCapabilities.getStringifier().focusModeFromString(focusSetting);
            Log.i(TAG, "focus mode resolved from setting: " + mFocusMode);
            // Try to find a supported focus mode from the default list.
            if (mFocusMode == null) {
                for (CameraCapabilities.FocusMode mode : mDefaultFocusModes) {
                    if (mCapabilities.supports(mode)) {
                        mFocusMode = mode;
                        Log.v(TAG, "selected supported focus mode from default list" + mode);
                        break;
                    }
                }
            }
        }
        if (!mCapabilities.supports(mFocusMode)) {
            // For some reasons, the driver does not support the current
            // focus mode. Fall back to auto.
            if (mCapabilities.supports(CameraCapabilities.FocusMode.AUTO)) {
                Log.v(TAG, "no supported focus mode, falling back to AUTO");
                mFocusMode = CameraCapabilities.FocusMode.AUTO;
            } else {
                Log.v(TAG, "no supported focus mode, falling back to current: " + currentFocusMode);
                mFocusMode = currentFocusMode;
            }
        }
        return mFocusMode;
    }

    public List<Area> getFocusAreas() {
        return mFocusArea;
    }

    public List<Area> getMeteringAreas() {
        return mMeteringArea;
    }

    public void resetTouchFocus() {
        Log.i(TAG, "resetTouchFocus mInitialized:" + mInitialized + "aeLock: " + isAFLock);
        if (!mInitialized || isAFLock) {
            return;
        }
        if (mIsSupportTEV) {
            mIsSupportTEV = false;
            mFocusRing.hideAELockPanel();
        }
        if (mListener.isManualMode()) {
            mFocusRing.hideAEAFFocus();
        }
        mFocusArea = null;
        mMeteringArea = null;
        // This will cause current module to call getFocusAreas() and
        // getMeteringAreas() and send updated regions to camera.
        mListener.setFocusParameters();

        if (mTouchCoordinate != null) {
            mTouchCoordinate = null;
        }
    }

    private Rect computeCameraRectFromPreviewCoordinates(int x, int y, int size) {
        int left = CameraUtil.clamp(x - size / 2, mPreviewRect.left,
                mPreviewRect.right - size);
        int top = CameraUtil.clamp(y - size / 2, mPreviewRect.top,
                mPreviewRect.bottom - size);

        RectF rectF = new RectF(left, top, left + size, top + size);
        return CameraUtil.rectFToRect(mCoordinateTransformer.toCameraSpace(rectF));
    }

    private Rect computePreviewFromCameraRectCoordinates(Rect rect){
        return CameraUtil.rectFToRect(mCoordinateTransformer.toPreviewSpace(CameraUtil.rectToRectF(rect)));
    }

    /* package */ int getFocusState() {
        return mState;
    }

    /* SPRD:Add for bug 443439 @{ */
    public boolean isInStateFocusing() {
        return mState == STATE_FOCUSING;
    }
    /* @} */

    public boolean isAEAFDraging() {
        if (mFocusRing != null) {
            return mFocusRing.isAEAFDraging();
        }
        return false;
    }

    public boolean isFocusCompleted() {
        return mState == STATE_SUCCESS || mState == STATE_FAIL;
    }

    public boolean isFocusingSnapOnFinish() {
        return mState == STATE_FOCUSING_SNAP_ON_FINISH;
    }

    public void updateFocusState() {
        if (!mFocusRing.isAEAFDraging()) {
            mState = STATE_IDLE;
        }
        mMeteringArea = null;
        removeMessages();
    }

    public void clearAfArea() {
        mFocusArea = null;
    }

    public void removeMessages() {
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
        if (mIsSupportTEV) {
            mHandler.removeMessages(TOUCH_EV_FOCUS_DISPLAY);//SPRD:fix bug1405737
        }
    }

    public void sendEmptyMessageDelayed() {
        mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS);
    }

    public void overrideFocusMode(CameraCapabilities.FocusMode focusMode) {
        mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock) {
        mAeAwbLock = lock;
    }

    public boolean getAeAwbLock() {
        return mAeAwbLock;
    }

    /**
     * SPRD: fix bug 473602 CAF do not need AF @{
    private boolean needAutoFocusCall(CameraCapabilities.FocusMode focusMode) {
        return !(focusMode == CameraCapabilities.FocusMode.INFINITY
                || focusMode == CameraCapabilities.FocusMode.FIXED
                || focusMode == CameraCapabilities.FocusMode.EXTENDED_DOF);
    }
    */
    private boolean needAutoFocusCall(CameraCapabilities.FocusMode focusMode) {
        return !(focusMode == CameraCapabilities.FocusMode.INFINITY
                || focusMode == CameraCapabilities.FocusMode.FIXED
                || focusMode == CameraCapabilities.FocusMode.EXTENDED_DOF
                || focusMode == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE
                || focusMode == CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
    }
    /**
     * @}
     */
    /**
     * Add For Dream Camera, new Interface for Settings.
     * @param currentFocusMode: original param,mCameraSettings.focusMode
     * @param settingsFocusMode: String focusSetting
     * @return
     */
    public CameraCapabilities.FocusMode getFocusMode(
            final CameraCapabilities.FocusMode currentFocusMode, final String settingsFocusMode) {
        if (mOverrideFocusMode != null) {
            Log.v(TAG, "returning override focus: " + mOverrideFocusMode);
            return mOverrideFocusMode;
        }
        if (mCapabilities == null) {
            Log.v(TAG, "no capabilities, returning default AUTO focus mode");
            return CameraCapabilities.FocusMode.AUTO;
        }

        Log.d(TAG,"AA getFocusMode "+mFocusAreaSupported+","+mFocusArea);
        if (mFocusAreaSupported && mFocusArea != null) {
            Log.v(TAG, "in tap to focus, returning AUTO focus mode");
            // Always use autofocus in tap-to-focus.
            mFocusMode = CameraCapabilities.FocusMode.AUTO;
        } else {
            Log.i(TAG, "AA getFocusMode stored focus setting for camera: " + settingsFocusMode);
            // The default is continuous autofocus.
            mFocusMode = mCapabilities.getStringifier().focusModeFromString(settingsFocusMode);
            Log.v(TAG, "focus mode resolved from setting: " + mFocusMode);
            // Try to find a supported focus mode from the default list.
            if (mFocusMode == null) {
                for (CameraCapabilities.FocusMode mode : mDefaultFocusModes) {
                    if (mCapabilities.supports(mode)) {
                        mFocusMode = mode;
                        Log.v(TAG, "selected supported focus mode from default list" + mode);
                        break;
                    }
                }
            }
        }
        if (!mCapabilities.supports(mFocusMode)) {
            // For some reasons, the driver does not support the current
            // focus mode. Fall back to auto.
            if (mCapabilities.supports(CameraCapabilities.FocusMode.AUTO)) {
                Log.v(TAG, "no supported focus mode, falling back to AUTO");
                mFocusMode = CameraCapabilities.FocusMode.AUTO;
            } else {
                Log.v(TAG, "no supported focus mode, falling back to current: " + currentFocusMode);
                mFocusMode = currentFocusMode;
            }
        }
        return mFocusMode;
    }
}
