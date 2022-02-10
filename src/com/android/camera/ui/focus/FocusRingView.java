/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.camera.ui.focus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.android.camera.ui.motion.AnimationClock.SystemTimeClock;
import com.android.camera.ui.motion.DynamicAnimator;
import com.android.camera.ui.motion.Invalidator;
import com.android.camera.ui.motion.LinearScale;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import javax.annotation.Nullable;

import com.dream.camera.ui.AELockPanel;
import com.dream.camera.ui.AdjustPanel;
import com.dream.camera.ui.VerticalSeekBar;
import android.graphics.Rect;
import com.dream.camera.ui.ManualAeAfPanel;
/**
 * Custom view for running the focus ring animations.
 */
public class FocusRingView extends View implements Invalidator, FocusRing {
    private static final Tag TAG = new Tag("FocusRingView");
    private static final float FADE_IN_DURATION_MILLIS = 2000f;//SPRD:fix bug647089/672649
    private static final float FADE_OUT_DURATION_MILLIS = 250f;
    private static final float FADE_IN_DURATION_MILLIS_SUCCESS = 0f;//SPRD:fix 672649
    private static final float FADE_IN_DURATION_MILLIS_MANUAL = 3000f;

    private final AutoFocusRing mAutoFocusRing;
    private final ManualFocusRing mManualFocusRing;
    /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
    private final AutoFocusRing mAutoFocusSuccessRing;
    private final ManualFocusRing mManualFocusSuccessRing;
    /* @} */
    private final DynamicAnimator mAnimator;
    private final LinearScale mRatioScale;
    private final float mDefaultRadiusPx;

    private FocusRingRenderer currentFocusAnimation;
    private boolean isFirstDraw;
    private float mLastRadiusPx;

    @Nullable
    private RectF mPreviewSize;

    public FocusRingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = getResources();
        Paint paint = makePaint(res, R.color.focus_color);
        int successColor = CameraUtil.isTouchEvEnable() ? R.color.focus_success_color : R.color.focus_success_color_green;
        Paint successPaint = makePaint(res, successColor);

        float focusCircleMinSize = res.getDimensionPixelSize(R.dimen.focus_circle_min_size);
        float focusCircleMaxSize = res.getDimensionPixelSize(R.dimen.focus_circle_max_size);
        mDefaultRadiusPx = res.getDimensionPixelSize(R.dimen.focus_circle_initial_size);

        mRatioScale = new LinearScale(0, 1, focusCircleMinSize, focusCircleMaxSize);
        mAnimator = new DynamicAnimator(this, new SystemTimeClock());

        mAutoFocusRing = new AutoFocusRing(mAnimator, paint,
              FADE_IN_DURATION_MILLIS,
              FADE_OUT_DURATION_MILLIS);
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        mAutoFocusSuccessRing = new AutoFocusRing(mAnimator, successPaint,
                FADE_IN_DURATION_MILLIS_SUCCESS,
                FADE_OUT_DURATION_MILLIS);
        mManualFocusRing = new ManualFocusRing(mAnimator, paint,
                FADE_IN_DURATION_MILLIS_MANUAL,
                FADE_OUT_DURATION_MILLIS);
        mManualFocusSuccessRing = new ManualFocusRing(mAnimator, successPaint,
                FADE_IN_DURATION_MILLIS_SUCCESS,
                FADE_OUT_DURATION_MILLIS);

        mAnimator.animations.add(mAutoFocusRing);
        mAnimator.animations.add(mManualFocusRing);
        mAnimator.animations.add(mAutoFocusSuccessRing);
        mAnimator.animations.add(mManualFocusSuccessRing);
        /* @} */
        isFirstDraw = true;
        mLastRadiusPx = mDefaultRadiusPx;
    }

    @Override
    public boolean isPassiveFocusRunning() {
        return mAutoFocusRing.isActive();
    }

    @Override
    public boolean isActiveFocusRunning() {
        return mManualFocusRing.isActive();
    }

    @Override
    public void startPassiveFocus() {
        mAnimator.update();
        mAnimator.invalidate();
        long tMs = mAnimator.getTimeMillis();

        if (mManualFocusRing.isActive() && !mManualFocusRing.isExiting()) {
            mManualFocusRing.stop(tMs);
        }
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        if (mManualFocusSuccessRing.isActive() && !mManualFocusSuccessRing.isExiting()) {
            mManualFocusSuccessRing.stop(tMs);
        }
        if (mAutoFocusSuccessRing.isActive() && !mAutoFocusSuccessRing.isExiting()) {
            mAutoFocusSuccessRing.stop(tMs);
        }
        /* @} */
        mAutoFocusRing.start(tMs, mLastRadiusPx, mLastRadiusPx);
        currentFocusAnimation = mAutoFocusRing;
        if(getAdjustPanel() != null){
            getAdjustPanel().hide();
        }
    }
    /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
    @Override
    public void startPassiveFocusedFocus() {
        mAnimator.invalidate();
        long tMs = mAnimator.getTimeMillis();

        if (mManualFocusRing.isActive() && !mManualFocusRing.isExiting()) {
            mManualFocusRing.stop(tMs);
        }
        if (mManualFocusSuccessRing.isActive() && !mManualFocusSuccessRing.isExiting()) {
            mManualFocusSuccessRing.stop(tMs);
        }
        if (mAutoFocusRing.isActive() && !mAutoFocusRing.isExiting()) {
            mAutoFocusRing.stop(tMs);
        }

        mAutoFocusSuccessRing.start(tMs, mLastRadiusPx, mLastRadiusPx);
        currentFocusAnimation = mAutoFocusSuccessRing;
        if(getAdjustPanel() != null){
            getAdjustPanel().hide();
        }
    }
    /* @} */
    @Override
    public void startActiveFocus() {
        mAnimator.update();//SPRD:fix bug594887
        mAnimator.invalidate();
        long tMs = mAnimator.getTimeMillis();
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        if (mAutoFocusRing.isActive() && !mAutoFocusRing.isExiting()) {
            mAutoFocusRing.stop(tMs);
        }
        if (mAutoFocusSuccessRing.isActive() && !mAutoFocusSuccessRing.isExiting()) {
            mAutoFocusSuccessRing.stop(tMs);
        }
        if (mManualFocusSuccessRing.isActive() && !mManualFocusSuccessRing.isExiting()) {
            mManualFocusSuccessRing.stop(tMs);
        }
        /* @} */
        mManualFocusRing.start(tMs, 0.0f, mLastRadiusPx);
        currentFocusAnimation = mManualFocusRing;
        if(getAdjustPanel() != null){
            getAdjustPanel().hide();
        }
    }
    /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
    @Override
    public void startActiveFocusedFocus() {
        mAnimator.invalidate();
        long tMs = mAnimator.getTimeMillis();

        if (mAutoFocusRing.isActive() && !mAutoFocusRing.isExiting()) {
            mAutoFocusRing.stop(tMs);
        }
        if (mAutoFocusSuccessRing.isActive() && !mAutoFocusSuccessRing.isExiting()) {
            mAutoFocusSuccessRing.stop(tMs);
        }
        if (mManualFocusRing.isActive() && !mManualFocusRing.isExiting()) {
            mManualFocusRing.stop(tMs);
        }

        mManualFocusSuccessRing.start(tMs, mLastRadiusPx, mLastRadiusPx);
        currentFocusAnimation = mManualFocusSuccessRing;
        if(getAdjustPanel() != null){
            getAdjustPanel().show();
        }
    }
    /* @} */
    @Override
    public void stopFocusAnimations() {
        long tMs = mAnimator.getTimeMillis();
        if (mManualFocusRing.isActive() && !mManualFocusRing.isExiting()
              /*&& !mManualFocusRing.isEntering()*/) {//SPRD:fix bug755107 focus ring will not disappear when fail immediately
            mManualFocusRing.exit(tMs);
        }

        if (mAutoFocusRing.isActive() && !mAutoFocusRing.isExiting()) {
            mAutoFocusRing.exit(tMs);
        }
    }

    @Override
    public void setFocusLocation(float viewX, float viewY) {
        mAutoFocusRing.setCenterX((int) viewX);
        mAutoFocusRing.setCenterY((int) viewY);
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        mManualFocusRing.setCenterX((int) viewX);
        mManualFocusRing.setCenterY((int) viewY);

        mAutoFocusSuccessRing.setCenterX((int) viewX);
        mAutoFocusSuccessRing.setCenterY((int) viewY);
        mManualFocusSuccessRing.setCenterX((int) viewX);
        mManualFocusSuccessRing.setCenterY((int) viewY);
        /* @} */
        setAdjustPanelPos(viewX, viewY);
    }

    @Override
    public void centerFocusLocation() {
        Point center = computeCenter();
        mAutoFocusRing.setCenterX(center.x);
        mAutoFocusRing.setCenterY(center.y);
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        mManualFocusRing.setCenterX(center.x);
        mManualFocusRing.setCenterY(center.y);

        mAutoFocusSuccessRing.setCenterX(center.x);
        mAutoFocusSuccessRing.setCenterY(center.y);
        mManualFocusSuccessRing.setCenterX(center.x);
        mManualFocusSuccessRing.setCenterY(center.y);
        /* @} */
        setAdjustPanelPos(center.x, center.y);
    }

    @Override
    public void setRadiusRatio(float ratio) {
        setRadius(mRatioScale.scale(mRatioScale.clamp(ratio)));
    }

    @Override
    public void configurePreviewDimensions(RectF previewArea) {
        mPreviewSize = previewArea;
        updateAELockLayout(CameraUtil.rectFToRect(previewArea));
        updateAEAFLayout(CameraUtil.rectFToRect(previewArea));
        mLastRadiusPx = mDefaultRadiusPx;

        if (!isFirstDraw) {
            centerAutofocusRing();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isFirstDraw) {
            isFirstDraw = false;
            centerAutofocusRing();
        }
//        if (mPreviewSize != null) {
//            canvas.clipRect(mPreviewSize, Region.Op.REPLACE);
//        }
//        mAnimator.draw(canvas);
        /*Bug 1122289 chang android:targetSdkVersion to 28, Region.Op.REPLACE is not supported*/
        if (mPreviewSize != null) {
            canvas.save();
            canvas.clipRect(mPreviewSize);
            mAnimator.draw(canvas);
            canvas.restore();
        } else {
            mAnimator.draw(canvas);
        }

    }

    private void setRadius(float radiusPx) {
        long tMs = mAnimator.getTimeMillis();
        // Some devices return zero for invalid or "unknown" diopter values.
        if (currentFocusAnimation != null && radiusPx > 0.1f) {
            currentFocusAnimation.setRadius(tMs, radiusPx);
            mLastRadiusPx = radiusPx;
        }
    }

    private void centerAutofocusRing() {
        Point center = computeCenter();
        mAutoFocusRing.setCenterX(center.x);
        mAutoFocusRing.setCenterY(center.y);
        /* SPRD: Add for focus ui modify: success/fail focus ring's color is green @{ */
        mAutoFocusSuccessRing.setCenterX(center.x);
        mAutoFocusSuccessRing.setCenterY(center.y);
        /* @} */

    }

    private Point computeCenter() {
        if (mPreviewSize != null && (mPreviewSize.width() * mPreviewSize.height() > 0.01f)) {
            Log.i(TAG, "Computing center via preview size.");
            return new Point((int) mPreviewSize.centerX(), (int) mPreviewSize.centerY());
        }
        Log.i(TAG, "Computing center via view bounds.");
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    private Paint makePaint(Resources res, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(res.getColor(color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(res.getDimension(R.dimen.focus_circle_stroke));
        return paint;
    }

    private AdjustPanel adjustPanel;
    private VerticalSeekBar vSeekBar;

    private AdjustPanel getAdjustPanel() {
        if (adjustPanel == null) {
            adjustPanel = (AdjustPanel) getRootView().findViewById(R.id.adjust_panel);
        }
        return adjustPanel;
    }

    private VerticalSeekBar getSeekBar() {
        if (vSeekBar == null) {
            vSeekBar = (VerticalSeekBar) getRootView().findViewById(R.id.vseekbar);
        }
        return vSeekBar;
    }

    private void setAdjustPanelPos(float x, float y) {
        if(getAdjustPanel() != null){
            getAdjustPanel().setTranslation(x, y);
        }
    }

    private AELockPanel mAELockPanel;
    private AELockPanel getAELockPanel() {
        if (mAELockPanel == null) {
            mAELockPanel = (AELockPanel) getRootView().findViewById(R.id.ae_lock_panel);
        }
        return mAELockPanel;
    }

    public void updateAELockLayout(Rect previewRect) {
        AELockPanel panel = getAELockPanel();
        if (panel == null) return;
        panel.updateAELockLayout(previewRect);
    }

    public void setAELockPanelPos(float x, float y) {
        if(getAELockPanel() != null){
            getAELockPanel().setTranslation(x, y);
        }
    }
    public void setAELockColor(boolean value){
        if(getAELockPanel() != null){
            getAELockPanel().changeColor(value);
        }
    }

    public void setTipTextVisible(boolean value){
        if(getAELockPanel() != null){
            getAELockPanel().showtipText(value);
        }
    }

    public void showAELockPanel(){
        if(getAELockPanel() != null){
            long tMs = mAnimator.getTimeMillis();
            if (mManualFocusSuccessRing.isActive()) {
                mManualFocusSuccessRing.stop(tMs);
            }
            if (mAutoFocusSuccessRing.isActive()) {
                mAutoFocusSuccessRing.stop(tMs);
            }
            getAELockPanel().show();
        }
    }

    public void hideAELockPanel(){
        if(getAELockPanel() != null){
            getAELockPanel().hide();
        }
    }

    public void setSeekBarVisibility(boolean visible) {
        if(getAELockPanel() != null){
            getAELockPanel().setSeekBarVisibility(visible);
        }
    }

    private ManualAeAfPanel mManualAEAFPanel;
    private ManualAeAfPanel getAEAFPanel() {
        if (mManualAEAFPanel == null) {
            mManualAEAFPanel = (ManualAeAfPanel) getRootView().findViewById(R.id.manual_ae_af_panel);
        }
        return mManualAEAFPanel;
    }

    public void updateAEAFLayout(Rect previewRect) {
        ManualAeAfPanel panel = getAEAFPanel();
        if (panel != null) {
            panel.updateAEAFLayout(previewRect);
        }
    }

    public void setAEAFPanelPos(float x, float y) {
        ManualAeAfPanel panel = getAEAFPanel();
        if (panel != null) {
            panel.setTranslation(x, y);
        }
    }

    public void showAEAFFocus() {
        ManualAeAfPanel panel = getAEAFPanel();
        if (panel != null) {
            long tMs = mAnimator.getTimeMillis();
            if (mManualFocusRing.isActive()) {
                mManualFocusRing.stop(tMs);
            }
            panel.showAEAFFocus();
        }
    }

    public void hideAEAFFocus() {
        ManualAeAfPanel panel = getAEAFPanel();
        if (panel != null) {
            panel.hideAEAFFocus();
        }
    }

    public boolean isAEAFDraging() {
        ManualAeAfPanel panel = getAEAFPanel();
        if (panel != null) {
            return panel.isAEAFDraging();
        }
        return false;
    }
}
