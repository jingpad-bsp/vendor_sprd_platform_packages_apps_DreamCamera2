/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import java.math.BigDecimal;
import java.util.List;

/**
 * PreviewOverlay is a view that sits on top of the preview. It serves to disambiguate
 * touch events, as {@link com.android.camera.app.CameraAppUI} has a touch listener
 * set on it. As a result, touch events that happen on preview will first go through
 * the touch listener in AppUI, which filters out swipes that should be handled on
 * the app level. The rest of the touch events will be handled here in
 * {@link #onTouchEvent(android.view.MotionEvent)}.
 * <p/>
 * For scale gestures, if an {@link OnZoomChangedListener} is set, the listener
 * will receive callbacks as the scaling happens, and a zoom UI will be hosted in
 * this class.
 */
public class PreviewOverlay extends View
    implements PreviewStatusListener.PreviewAreaChangedListener, SeekBar.OnSeekBarChangeListener {

    public static final float ZOOM_MIN_RATIO = 1.0f;
    private static final int NUM_ZOOM_LEVELS = 7;
    private static final float MIN_ZOOM = 1f;

    private static final Log.Tag TAG = new Log.Tag("PreviewOverlay");

    /** Minimum time between calls to zoom listener. */
    private static final long ZOOM_MINIMUM_WAIT_MILLIS = 33;

    /** Next time zoom change should be sent to listener. */
    private long mDelayZoomCallUntilMillis = 0;
    private final ZoomGestureDetector mScaleDetector;
    private final ZoomProcessor mZoomProcessor = new ZoomProcessor();
    private GestureDetector mGestureDetector = null;
    private View.OnTouchListener mTouchListener = null;
    private OnZoomChangedListener mZoomListener = null;
    private OnDreamZoomUIChangedListener mDreamZoomUIListener = null;
    private OnPreviewTouchedListener mOnPreviewTouchedListener;
    private boolean isDetector = true;//SPRD: fix bug411062

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** Maximum zoom; intialize to 1.0 (disabled) */
    private float mMaxZoom = MIN_ZOOM;
    /**
     * Current zoom value in accessibility mode, ranging from MIN_ZOOM to
     * mMaxZoom.
     */
    private float mCurrA11yZoom = MIN_ZOOM;
    /**
     * Current zoom level ranging between 1 and NUM_ZOOM_LEVELS. Each level is
     * associated with a discrete zoom value.
     */
    private int mCurrA11yZoomLevel = 1;

    public interface OnZoomChangedListener {
        /**
         * This gets called when a zoom is detected and started.
         */
        void onZoomStart();

        /**
         * This gets called when zoom gesture has ended.
         */
        void onZoomEnd();

        /**
         * This gets called when scale gesture changes the zoom value.
         *
         * @param ratio zoom ratio, [1.0f,maximum]
         */
        void onZoomValueChanged(float ratio);  // only for immediate zoom
    }

    public interface OnPreviewTouchedListener {
        /**
         * This gets called on any preview touch event.
         */
        public void onPreviewTouched(MotionEvent ev);
    }

    /**
     * Dream Zoom
     * @author SPREADTRUM\liyan.zhu
     *
     */
    public interface OnDreamZoomUIChangedListener {

        public void initZoomLevel(float minZoomValue, float maxZoomValue, float curZoomValue);

        public void updateZoomLevel(float minZoomValue, float maxZoomValue, float curZoomValue);

        public void initZoomLevel(float[] zoomSection, float curZoomValue);

        public void updateZoomLevel(float[] zoomSection, float curZoomValue);

        public void setZoomVisible(int state);

        public int getZoomVisible();//Sprd:Fix bug782358

        public float getZoomRatioFromProgess(int progress);

        public void updateZoomValueText(float curZoomValue);

        public void setZoomSimple(boolean isSimple);

        public boolean isZoomSimple();

        public void setDetector(boolean isDetector);
    }

    public PreviewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleDetector = new ZoomGestureDetector();
    }


    /**
     * This sets up the zoom listener and zoom related parameters when
     * the range of zoom ratios is continuous.
     *
     * @param zoomMaxRatio max zoom ratio, [1.0f,+Inf)
     * @param zoom current zoom ratio, [1.0f,zoomMaxRatio]
     * @param zoomChangeListener a listener that receives callbacks when zoom changes
     */
    public void setupZoom(float zoomMaxRatio, float zoom,
                          OnZoomChangedListener zoomChangeListener) {
        mZoomListener = zoomChangeListener;
        mZoomProcessor.setupZoom(zoomMaxRatio, zoom);
        mZoomProcessor.setZoomSimple(true);
        mZoomProcessor.showZoomUI();
        mZoomProcessor.hideZoomUIDream();
    }

    public void setupZoom(float zoomMaxRatio, float zoom, float[] zoomRatioSection,
                          OnZoomChangedListener zoomChangeListener,boolean isSimple) {
        mZoomListener = zoomChangeListener;
        mZoomProcessor.setupZoom(zoomMaxRatio, zoom, zoomRatioSection);
        mZoomProcessor.setZoomSimple(isSimple);
        mZoomProcessor.showZoomUI();
        if (isSimple) {
            mZoomProcessor.hideZoomUIDream();
        } else {
            if (mhandler.hasMessages(EMPTY)) {
                mhandler.removeMessages(EMPTY);
            }
            mZoomProcessor.showZoomUIDream();
        }
    }

    /**
     * uZooms camera in when in accessibility mode.
     *
     * @param view is the current view
     * @param maxZoom is the maximum zoom value on the given device
     * @return float representing the current zoom value
     */
    public float zoomIn(View view, float maxZoom) {
        mCurrA11yZoomLevel++;
        mMaxZoom = maxZoom;
        mCurrA11yZoom = getZoomAtLevel(mCurrA11yZoomLevel);
        mZoomListener.onZoomValueChanged(mCurrA11yZoom);
        view.announceForAccessibility(String.format(
                view.getResources().
                        getString(R.string.accessibility_zoom_announcement), mCurrA11yZoom));
        return mCurrA11yZoom;
    }

    /**
     * Zooms camera out when in accessibility mode.
     *
     * @param view is the current view
     * @param maxZoom is the maximum zoom value on the given device
     * @return float representing the current zoom value
     */
    public float zoomOut(View view, float maxZoom) {
        mCurrA11yZoomLevel--;
        mMaxZoom = maxZoom;
        mCurrA11yZoom = getZoomAtLevel(mCurrA11yZoomLevel);
        mZoomListener.onZoomValueChanged(mCurrA11yZoom);
        view.announceForAccessibility(String.format(
                view.getResources().
                        getString(R.string.accessibility_zoom_announcement), mCurrA11yZoom));
        return mCurrA11yZoom;
    }

    /**
     * Method used in accessibility mode. Ensures that there are evenly spaced
     * zoom values ranging from MIN_ZOOM to NUM_ZOOM_LEVELS
     *
     * @param level is the zoom level being computed in the range
     * @return the zoom value at the given level
     */
    private float getZoomAtLevel(int level) {
        return (MIN_ZOOM + ((level - 1) * ((mMaxZoom - MIN_ZOOM) / (NUM_ZOOM_LEVELS - 1))));
    }

    @Override
    public boolean onTouchEvent(MotionEvent m) {
        /* SPRD:Fix bug 411062 @{ */
        if (!isDetector) {
            Log.i(TAG, "onTouchEvent isDetector:" + isDetector);
            invalidate();
            return false;
        }
        /* @} */
        // Pass the touch events to scale detector and gesture detector
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(m);
        }
        if (mTouchListener != null) {
            mTouchListener.onTouch(this, m);
        }
        mScaleDetector.onTouchEvent(m);
        if (mOnPreviewTouchedListener != null) {
            mOnPreviewTouchedListener.onPreviewTouched(m);
        }
        return true;
    }

    /**
     * Set an {@link OnPreviewTouchedListener} to be executed on any preview
     * touch event.
     */
    public void setOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        mOnPreviewTouchedListener = listener;
    }

    public void setOnDreamZoomUIChangedListener(OnDreamZoomUIChangedListener listener) {
        mDreamZoomUIListener = listener;
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        mZoomProcessor.layout((int) previewArea.left, (int) previewArea.top,
                (int) previewArea.right, (int) previewArea.bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //mZoomProcessor.draw(canvas);
    }

    /**
     * Each module can pass in their own gesture listener through App UI. When a gesture
     * is detected, the {@link GestureDetector.OnGestureListener} will be notified of
     * the gesture.
     *
     * @param gestureListener a listener from a module that defines how to handle gestures
     */
    public void setGestureListener(GestureDetector.OnGestureListener gestureListener) {
        if (gestureListener != null) {
            mGestureDetector = new GestureDetector(getContext(), gestureListener);
        }
    }

    /**
     * Set a touch listener on the preview overlay.  When a module doesn't support a
     * {@link GestureDetector.OnGestureListener}, this can be used instead.
     */
    public void setTouchListener(View.OnTouchListener touchListener) {
        mTouchListener = touchListener;
    }

    /**
     * During module switch, connections to the previous module should be cleared.
     */
    public void reset() {
        mZoomListener = null;
        mGestureDetector = null;
        mTouchListener = null;
        mCurrA11yZoomLevel = 1;
        mCurrA11yZoom = MIN_ZOOM;
        /* SPRD: fix bug671302 need cancel toast when module pause @*/
        CameraUtil.resetHint();
        /* @} */
        if (mZoomProcessor != null){
            mZoomProcessor.setZoomSimple(true);
        }
    }

    /**
     * Custom scale gesture detector that ignores touch events when no
     * {@link OnZoomChangedListener} is set. Otherwise, it calculates the real-time
     * angle between two fingers in a scale gesture.
     */
    private class ZoomGestureDetector extends ScaleGestureDetector {
        private float mDeltaX;
        private float mDeltaY;

        public ZoomGestureDetector() {
            super(getContext(), mZoomProcessor);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev)  {
            if (mDreamZoomUIListener == null) {
                Log.i(TAG,"mDreamZoomUIListener is not inited");
                return false;
            } else {
                boolean handled = super.onTouchEvent(ev);
                if (ev.getPointerCount() > 1) {
                    mDeltaX = ev.getX(1) - ev.getX(0);
                    mDeltaY = ev.getY(1) - ev.getY(0);
                }
                return handled;
            }
        }

        /**
         * Calculate the angle between two fingers. Range: [-pi, pi]
         */
        public float getAngle() {
            return (float) Math.atan2(-mDeltaY, mDeltaX);
        }
    }

    /* SPRD: Fix bug 568154 @{ */
    private final int EMPTY = 0;
    private final int ZOOM_UI_HIDE_DELAY = 2000;
    private final Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mZoomProcessor.hideZoomUIDream();
        }
    };

    public void setZoom(float ratio) {
        removeZoomHideMsg();
        isZooming = true;
        mZoomProcessor.setZoom(ratio);
        // Fix bug 568175 that no zoom ui displays
        if (mZoomProcessor.isZoomSimple())
            mZoomProcessor.showZoomUIDream();
        else
            mZoomProcessor.updateZoomUIDream();
    }
    /* @} */

    public void stopZoomState(){
        isZooming = false;
    }

    public boolean isZoomSimple(){
        boolean isSimple = true;
        if (mZoomProcessor != null)
            isSimple = mZoomProcessor.isZoomSimple();
        return isSimple;
    }

    public void resetZoomSimple(){
        if (mZoomProcessor != null){
            mZoomProcessor.setZoomSimple(true);
        }
    }

    public void hideZoomUI(){
        if (mhandler.hasMessages(EMPTY)) {
            mhandler.removeMessages(EMPTY);
        }
        isZooming = false;
        mhandler.sendEmptyMessageDelayed(EMPTY, ZOOM_UI_HIDE_DELAY);
    }
    /**
     * This class processes recognized scale gestures, notifies {@link OnZoomChangedListener}
     * of any change in scale, and draw the zoom UI on screen.
     */
    private class ZoomProcessor implements ScaleGestureDetector.OnScaleGestureListener {
        private final Log.Tag TAG = new Log.Tag("ZoomProcessor");

        // Diameter of Zoom UI as fraction of maximum possible without clipping.
        private static final float ZOOM_UI_SIZE = 0.8f;
        // Diameter of Zoom UI donut hole as fraction of Zoom UI diameter.
        private static final float ZOOM_UI_DONUT = 0.25f;

        private float mMinRatio = 1.0f;
        private float mMaxRatio;
        // Continuous Zoom level [0,1].
        private float mCurrentRatio;
        private final Paint mPaint;
        private float mOuterRadius;
        private final int mZoomStroke;
        private boolean mVisible = true;
        private float[] mZoomRatioSection;

        public ZoomProcessor() {
            Resources res = getResources();
            mZoomStroke = res.getDimensionPixelSize(R.dimen.zoom_stroke);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mZoomStroke);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        // Set maximum zoom ratio from Module.
        public void setZoomMax(float zoomMaxRatio) {
            mMaxRatio = zoomMaxRatio;
        }

        public void setZoomRatioSection(float[] zoomRatioSection) {
            mZoomRatioSection = zoomRatioSection;
            if (zoomRatioSection!= null && zoomRatioSection[0] != 0){
                mMinRatio = zoomRatioSection[0];
                for (float max : zoomRatioSection){
                    mMaxRatio = mMaxRatio > max ? mMaxRatio : max;
                }
            } else {
                mMinRatio = 1.0f;
            }
        }

        // Set current zoom ratio from Module.
        public void setZoom(float ratio) {
            mCurrentRatio = ratio;
        }

        public void layout(int l, int t, int r, int b) {
            // UI will extend from 20% to 80% of maximum inset circle.
            float insetCircleDiameter = Math.min(getWidth(), getHeight());
            mOuterRadius = insetCircleDiameter * 0.5f * ZOOM_UI_SIZE;
        }

/*        public void draw(Canvas canvas) {
            Log.d(TAG,"draw");
            if (!mVisible) {
                return;
            }
            // Draw background.
            mPaint.setAlpha(70);
            canvas.drawLine(mCenterX + mInnerRadius * (float) Math.cos(mFingerAngle),
                    mCenterY - mInnerRadius * (float) Math.sin(mFingerAngle),
                    mCenterX + mOuterRadius * (float) Math.cos(mFingerAngle),
                    mCenterY - mOuterRadius * (float) Math.sin(mFingerAngle), mPaint);
            canvas.drawLine(mCenterX - mInnerRadius * (float) Math.cos(mFingerAngle),
                    mCenterY + mInnerRadius * (float) Math.sin(mFingerAngle),
                    mCenterX - mOuterRadius * (float) Math.cos(mFingerAngle),
                    mCenterY + mOuterRadius * (float) Math.sin(mFingerAngle), mPaint);
            // Draw Zoom progress.
            mPaint.setAlpha(255);
            float fillRatio = (mCurrentRatio - mMinRatio) / (mMaxRatio - mMinRatio);
            float zoomRadius = mInnerRadius + fillRatio * (mOuterRadius - mInnerRadius);
            canvas.drawLine(mCenterX + mInnerRadius * (float) Math.cos(mFingerAngle),
                    mCenterY - mInnerRadius * (float) Math.sin(mFingerAngle),
                    mCenterX + zoomRadius * (float) Math.cos(mFingerAngle),
                    mCenterY - zoomRadius * (float) Math.sin(mFingerAngle), mPaint);
            canvas.drawLine(mCenterX - mInnerRadius * (float) Math.cos(mFingerAngle),
                    mCenterY + mInnerRadius * (float) Math.sin(mFingerAngle),
                    mCenterX - zoomRadius * (float) Math.cos(mFingerAngle),
                    mCenterY + zoomRadius * (float) Math.sin(mFingerAngle), mPaint);
        }*/

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG,"onScale");
            if (mZoomListener == null) {
                return true;
            }
            mZoomScaling = true;
            final float sf = detector.getScaleFactor();
            mCurrentRatio = (0.33f + mCurrentRatio) * sf * sf - 0.33f;
            if (mCurrentRatio < mMinRatio) {
                mCurrentRatio = mMinRatio;
            }
            if (mCurrentRatio > mMaxRatio) {
                mCurrentRatio = mMaxRatio;
            }

            // Only call the listener with a certain frequency. This is
            // necessary because these listeners will make repeated
            // applySettings() calls into the portability layer, and doing this
            // too often can back up its handler and result in visible lag in
            // updating the zoom level and other controls.
            long now = SystemClock.uptimeMillis();
            if (now > mDelayZoomCallUntilMillis) {
                if (mZoomListener != null) {
                    mZoomListener.onZoomValueChanged(mCurrentRatio);
                }
                mDelayZoomCallUntilMillis = now + ZOOM_MINIMUM_WAIT_MILLIS;
            }
            //invalidate();
            if (mZoomRatioSection == null)
                mDreamZoomUIListener.updateZoomLevel(mMinRatio, mMaxRatio, mCurrentRatio);
            else
                mDreamZoomUIListener.updateZoomLevel(mZoomRatioSection,mCurrentRatio);
            mZoomScaling = false;
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG,"onScaleBegin");
            if (mhandler.hasMessages(EMPTY)) {
                mhandler.removeMessages(EMPTY);
            }

            if (mZoomListener == null) {
                return true;
            }

            mDreamZoomUIListener.setZoomVisible(View.VISIBLE);

            if (mZoomListener != null) {
                mZoomListener.onZoomStart();
            }
            //invalidate();
            if (mZoomRatioSection == null)
                mDreamZoomUIListener.initZoomLevel(mMinRatio, mMaxRatio, mCurrentRatio);
            else
                mDreamZoomUIListener.initZoomLevel(mZoomRatioSection, mCurrentRatio);
            isZooming = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG,"onScaleEnd");
            //mZoomProcessor.hideZoomUI();
            if (mZoomListener != null) {
                mZoomListener.onZoomValueChanged(mCurrentRatio);//SPRD:Bug fix 1045022
                mZoomListener.onZoomEnd();
            } else {
                /* SPRD:fix bug929757 show toast on scale end @{ */
                if (!CameraUtil.isZoomPanelEnabled()) {
                    CameraUtil.toastHint((CameraActivity) getContext(),R.string.not_support_zoom_change);
                } else {
                    CameraUtil.toastHint((CameraActivity) getContext(),R.string.current_module_does_not_support_zoom_change);
                }
                return;
                /* @} */
            }

            if (mZoomProcessor.isZoomSimple()) {
                PreviewOverlay.this.hideZoomUI();
            } else {
                PreviewOverlay.this.stopZoomState();
            }
            //invalidate();
        }

        public boolean isVisible() {
            return mVisible;
        }

        public void showZoomUI() {
            if (mZoomListener == null) {
                return;
            }
            mVisible = true;
            invalidate();
        }
        public void hideZoomUI() {
            if (mZoomListener == null) {
                return;
            }
            mVisible = false;
            invalidate();
        }

        public void setZoomSimple(boolean isSimple){
            if (mDreamZoomUIListener != null)
                mDreamZoomUIListener.setZoomSimple(isSimple);
        }

        public boolean isZoomSimple(){
            if (mDreamZoomUIListener == null)
                return false;

            return mDreamZoomUIListener.isZoomSimple();
        }

        public void setDetector(boolean isDetector) {
            if (mDreamZoomUIListener != null)
                mDreamZoomUIListener.setDetector(isDetector);
        }

        public void showZoomUIDream(){
            mVisible = true;
            if (mZoomListener == null) {
                return;
            }
            if (mZoomListener != null) {
                mZoomListener.onZoomStart();
            }
            if(mDreamZoomUIListener == null){
                return;
            }
            mDreamZoomUIListener.setZoomVisible(View.VISIBLE);
            if (mZoomRatioSection == null)
                mDreamZoomUIListener.initZoomLevel(mMinRatio, mMaxRatio, mCurrentRatio);
            else
                mDreamZoomUIListener.initZoomLevel(mZoomRatioSection, mCurrentRatio);
        }

        public void updateZoomUIDream(){
            if (mZoomRatioSection == null)
                mDreamZoomUIListener.updateZoomLevel(mMinRatio, mMaxRatio, mCurrentRatio);
            else
                mDreamZoomUIListener.updateZoomLevel(mZoomRatioSection, mCurrentRatio);
        }

        public void hideZoomUIDream(){
            mVisible = false;
            if (mZoomListener != null) {
                mZoomListener.onZoomEnd();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (null != mDreamZoomUIListener
                            && mDreamZoomUIListener.getZoomVisible() != View.GONE && !mVisible) {// SPRD: fix bug710328
                        mDreamZoomUIListener.setZoomVisible(View.GONE);
                    }
                }
            });
        }

        private void setupZoom(float zoomMax, float zoom) {
//            setZoom(zoom);
            setupZoom(zoomMax,zoom,null);
        }

        public void setupZoom(float zoomMax, float zoom, float[] zoomRationSection){
            setZoomMax(zoomMax);
            setZoomRatioSection(zoomRationSection);
            setZoom(zoom);
        }
    };

    /* SPRD:Fix bug 411062 @{ */
    public void setDetector(boolean isDetector) {
        this.isDetector = isDetector;
        mZoomProcessor.setDetector(isDetector);
        invalidate();
        Log.i(TAG, "setDetector isDetector:" + isDetector);
    }
    /* @} */

    public boolean isEnableDetector() {
        return isDetector;
    }

    /*SPRD:Fix bug 502060 hide the ZoomProcessor if it shown after the button clicked @{*/
    public void hideZoomProcessorIfNeeded() {
        if (null != mDreamZoomUIListener && mZoomProcessor.isVisible()) {
            mZoomProcessor.hideZoomUIDream();
        }
    }
    /* @} */

    /*SPRD:Fix bug 502060 hide the ZoomProcessor if it shown after the button clicked @{*/
    public void showZoomProcessorIfNeeded() {
        if (null != mDreamZoomUIListener && !mZoomProcessor.isVisible() && !mZoomProcessor.isZoomSimple()) {
            mZoomProcessor.showZoomUIDream();
        }
    }
    /* @} */

    @Override
    public void onProgressChanged(SeekBar SeekBarId, int progress,
            boolean fromUser) {
        if(mZoomScaling || !fromUser || !isDetector) return;//SPRD:fix bug628511/648477

//        float currentValueF = (progress * (mZoomProcessor.mMaxRatio - mZoomProcessor.mMinRatio)) * 0.01f + mZoomProcessor.mMinRatio;
        float currentValueF = mDreamZoomUIListener.getZoomRatioFromProgess(progress);

        BigDecimal b = new BigDecimal(currentValueF);
        currentValueF = b.setScale(4, BigDecimal.ROUND_HALF_UP).floatValue();
        if(currentValueF > mZoomProcessor.mMaxRatio) {
            currentValueF = mZoomProcessor.mMaxRatio;
        }

        // Only call the listener with a certain frequency. This is
        // necessary because these listeners will make repeated
        // applySettings() calls into the portability layer, and doing this
        // too often can back up its handler and result in visible lag in
        // updating the zoom level and other controls.
        long now = SystemClock.uptimeMillis();
        if (now > mDelayZoomCallUntilMillis) {
            if (mZoomListener != null) {
                mZoomListener.onZoomValueChanged(currentValueF);
            }
            mDelayZoomCallUntilMillis = now + ZOOM_MINIMUM_WAIT_MILLIS;
        }
        mZoomProcessor.setZoom(currentValueF);
    }

    @Override
    public void onStartTrackingTouch(SeekBar SeekBarId) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar SeekBarId) {

        if (mZoomProcessor.isZoomSimple())
            hideZoomUI();
        else
            stopZoomState();
    }

    private boolean mZoomScaling = false;
    //Sprd Fix Bug: 665197
    private boolean isZooming = false;

    public boolean isZooming(){
        return isZooming;
    }
    /*
     * SPRD Bug:607389 when zoom, open modeList, zoomBar display unnormal
     */
    public void removeZoomHideMsg() {
        if (mhandler != null && mhandler.hasMessages(EMPTY)) {
            mhandler.removeMessages(EMPTY);
        }
    }
}
