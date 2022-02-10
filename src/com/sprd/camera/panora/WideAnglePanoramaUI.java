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

package com.sprd.camera.panora;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera.CameraActivity;
import com.android.camera.PanoProgressBar;
import com.android.camera.ShutterButton;
import com.android.camera.app.OrientationManager;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.RotateLayout;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.dream.camera.DreamOrientation;
import com.dream.camera.DreamUI;
import com.dream.camera.modules.panoramadream.SmallPreviewView;

/**
 * The UI of {@link WideAnglePanoramaModule}.
 */
public class WideAnglePanoramaUI extends DreamUI implements
        TextureView.SurfaceTextureListener,
        PreviewStatusListener,
        SmallPreviewView.CaputerPreviewListener,
        View.OnLayoutChangeListener {

    private static final String TAG = "CAM_WidePanoramaUI";

    protected CameraActivity mActivity;
    private WideAnglePanoramaController mController;

    protected ViewGroup mRootView;
    private FrameLayout mCaptureLayout;
    private View mReviewLayout;
    private ImageView mReview;
    // CID 109176 : UuF: Unused field (FB.UUF_UNUSED_FIELD)
    // private ImageView mPreviewThumb;
    private View mPreviewBorder;
    private View mLeftIndicator;
    private View mRightIndicator;
    private View mCaptureIndicator;
    private PanoProgressBar mCaptureProgressBar;
    private PanoProgressBar mSavingProgressBar;
    private TextView mTooFastPrompt;
    private ViewGroup mReviewControl;
    private TextureView mTextureView;
    private ShutterButton mShutterButton;
    // CID 109176 : UuF: Unused field (FB.UUF_UNUSED_FIELD)
    // private boolean mEnablePreviwThumb;

    private Matrix mProgressDirectionMatrix = new Matrix();
    private float[] mProgressAngle = new float[2];

    private DialogHelper mDialogHelper;

    // Color definitions.
    private int mIndicatorColor;
    private int mIndicatorColorFast;
    private int mReviewBackground;
    private SurfaceTexture mSurfaceTexture;
    private View mPreviewCover;

    // SPRD:Add for bug 479120 wideangle module rotate issue
    private FrameLayout mProgressLayout;
    private SmallPreviewView mSmallPreviewView;
    private RotateLayout mPanoWaitBar;
    private TextView mPreviewIndicator;
    protected WideAnglePanoramaModule mBasicModule;
    private RotateLayout mPanoSmallPreview;

    public WideAnglePanoramaUI(CameraActivity activity, WideAnglePanoramaController controller,
                               ViewGroup root) {
        mActivity = activity;
        mController = controller;
        mBasicModule = (WideAnglePanoramaModule)controller;
        mRootView = root;

        createContentView();
        initUI();
    }

    public void onOrientationChanged( OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();
        ViewGroup topPanelParent = (ViewGroup) mActivity.getModuleLayoutRoot().findViewById(
                R.id.top_panel_parent);
        DreamOrientation.setOrientation(topPanelParent, orientation, true);
    }

    public void onStartCapture() {
        // SPRD Bug WideAngle Rotate
        int orientation = 0;
        if (mProgressLayout != null) {
            orientation = (int) mProgressLayout.getRotation();
        }
        if (orientation == 0 || orientation == 180) {
            if (mProgressLayout != null && mPanoBg != null && mCaptureProgressBar != null)
//                mProgressLayout.setPadding(0, 0, 0,
//                        mPanoBg.getHeight() - mCaptureProgressBar.getHeight());
                mProgressLayout.setPadding(0, 0, 0,mActivity.getResources().getDimensionPixelSize(R.dimen.pano_pan_progress_bar_padding));
        } else if (orientation == 90 || orientation == 270) {
            if (mProgressLayout != null)
                mProgressLayout.setPadding(0, 0, 0, 20);
        }

        mShutterButton.setImageResource(R.drawable.ic_capture_pano_stop);
        if (CameraUtil.isNewWideAngle()) {
            if (mIsLand){
                mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_capture_indication_dream));
            }else{
                mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_capture_indication_dream_vertical));
            }
            mSmallPreviewView.setCaptureState(SmallPreviewView.CAPTURE_STATE);
        } else {
            mCaptureIndicator.setVisibility(View.VISIBLE);
            showDirectionIndicators(PanoProgressBar.DIRECTION_NONE);
        }
        changeOtherUIVisible(View.INVISIBLE);//Sprd:fix bug954588
    }

    public void setShutterButtonVisible(boolean visible) {
        mShutterButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void showPreviewUI() {
        mCaptureLayout.setVisibility(View.VISIBLE);
        mActivity.getCameraAppUI().setBottomBarVisible(true);
        mShutterButton.setVisibility(View.VISIBLE);
        changeOtherUIVisible(View.VISIBLE);//Sprd:fix bug954588
    }

    public void onStopCapture() {
        hideTooFastIndication();
        if (CameraUtil.isNewWideAngle()) {
            mSmallPreviewView.setVisibility(View.INVISIBLE);
            if(mPanoWaitBar != null)
                mPanoWaitBar.setVisibility(View.VISIBLE);
            mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_dialog_prepare_preview));
            mSmallPreviewView.setCaptureState(SmallPreviewView.PREVIEW_STATE);
        } else {
            mCaptureIndicator.setVisibility(View.INVISIBLE);
            hideDirectionIndicators();
        }
        mActivity.getCameraAppUI().setBottomBarVisible(false);
        mShutterButton.setImageResource(R.drawable.ic_capture_pano);
        mShutterButton.setVisibility(View.GONE);
    }

    public void setCaptureProgressOnDirectionChangeListener(
            PanoProgressBar.OnDirectionChangeListener listener) {
        mCaptureProgressBar.setOnDirectionChangeListener(listener);
    }

    public void resetCaptureProgress() {
        mCaptureProgressBar.reset();
        mSmallPreviewView.reset();
    }

    public void setMaxCaptureProgress(int max) {
        mCaptureProgressBar.setMaxProgress(max);
        mSmallPreviewView.setMaxProgress(max);
    }

    public void showCaptureProgress() {
        mCaptureProgressBar.setVisibility(View.VISIBLE);
    }

    public void updateCaptureProgress(
            float panningRateXInDegree, float panningRateYInDegree,
            float progressHorizontalAngle, float progressVerticalAngle,
            float maxPanningSpeed) {

        if ((Math.abs(panningRateXInDegree) > maxPanningSpeed)
                || (Math.abs(panningRateYInDegree) > maxPanningSpeed)) {
            showTooFastIndication();
        } else {
            hideTooFastIndication();
        }

        // progressHorizontalAngle and progressVerticalAngle are relative to the
        // camera. Convert them to UI direction.
        mProgressAngle[0] = progressHorizontalAngle;
        mProgressAngle[1] = progressVerticalAngle;
        mProgressDirectionMatrix.mapPoints(mProgressAngle);

        int angleInMajorDirection =
                (Math.abs(mProgressAngle[0]) > Math.abs(mProgressAngle[1]))
                        ? (int) mProgressAngle[0]
                        : (int) mProgressAngle[1];
        if (CameraUtil.isNewWideAngle()) {
            mSmallPreviewView.setProgress((int)progressHorizontalAngle, (int)progressVerticalAngle);
        } else {
            mCaptureProgressBar.setProgress((angleInMajorDirection));
        }
    }

    public void setProgressOrientation(int orientation) {
        mProgressDirectionMatrix.reset();
        mProgressDirectionMatrix.postRotate(orientation);
    }

    public void showDirectionIndicators(int direction) {
        switch (direction) {
            case PanoProgressBar.DIRECTION_NONE:
                mLeftIndicator.setVisibility(View.VISIBLE);
                mRightIndicator.setVisibility(View.VISIBLE);
                break;
            case PanoProgressBar.DIRECTION_LEFT:
                mLeftIndicator.setVisibility(View.VISIBLE);
                mRightIndicator.setVisibility(View.INVISIBLE);
                break;
            case PanoProgressBar.DIRECTION_RIGHT:
                mLeftIndicator.setVisibility(View.INVISIBLE);
                mRightIndicator.setVisibility(View.VISIBLE);
                break;
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Log.d(TAG, " onSurfaceTextureAvailable");
        mSurfaceTexture = surfaceTexture;
        mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        mController.onPreviewUIDestroyed();
        mSurfaceTexture = null;
        Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // mActivity.getCameraAppUI().onSurfaceTextureUpdated(surfaceTexture);
    }

    private void hideDirectionIndicators() {
        mLeftIndicator.setVisibility(View.INVISIBLE);
        mRightIndicator.setVisibility(View.INVISIBLE);
    }

    public Point getPreviewAreaSize() {
        return new Point(
                mTextureView.getWidth(), mTextureView.getHeight());
    }

    public void reset() {
        mReviewLayout.setVisibility(View.GONE);
        mCaptureProgressBar.setVisibility(View.INVISIBLE);
        if(mPanoWaitBar != null)
            mPanoWaitBar.setVisibility(View.INVISIBLE);
        if (CameraUtil.isNewWideAngle()) {
            mSmallPreviewView.setVisibility(View.VISIBLE);
            mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_preview_indication_dream));
        }
        hideDirectionIndicators();//SPRD: Fix bug 539829
    }

    public void showFinalMosaic(Bitmap bitmap, int orientation) {
        if (bitmap != null && orientation != 0) {
            Matrix rotateMatrix = new Matrix();
            rotateMatrix.setRotate(orientation);
            bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    rotateMatrix, false);
        }

        mReview.setImageBitmap(bitmap);
        mCaptureLayout.setVisibility(View.GONE);
        mReviewLayout.setVisibility(View.VISIBLE);
        /* SPRD: fix bug337352 SavingProgressBar and cancel button didn't show @{ */
        mSavingProgressBar.setVisibility(View.VISIBLE);
        /* @} */
    }

    public void onConfigurationChanged(boolean threadRunning) {
        Drawable lowResReview = null;
        if (threadRunning)
            lowResReview = mReview.getDrawable();

        // Change layout in response to configuration change
        LayoutInflater inflater = (LayoutInflater)
                mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mReviewControl.removeAllViews();
        inflater.inflate(R.layout.pano_review_control, mReviewControl, true);

        // mRootView.bringChildToFront(mCameraControls);
        setViews(mActivity.getResources());
        if (threadRunning) {
            mReview.setImageDrawable(lowResReview);
            mCaptureLayout.setVisibility(View.GONE);
            mReviewLayout.setVisibility(View.VISIBLE);
            mSavingProgressBar.setVisibility(View.VISIBLE);// SPRD: BUG 348809
        }
    }

    public void resetSavingProgress() {
        mSavingProgressBar.reset();
        mSavingProgressBar.setRightIncreasing(true);
    }

    public void updateSavingProgress(int progress) {
        mSavingProgressBar.setProgress(progress);
    }

    @Override
    public void onLayoutChange(
            View v, int l, int t, int r, int b,
            int oldl, int oldt, int oldr, int oldb) {
        mController.onPreviewUILayoutChange(l, t, r, b);
    }

    public void showAlertDialog(
            String title, String failedString,
            String OKString, Runnable runnable) {
        mDialogHelper.showAlertDialog(title, failedString, OKString, runnable);
    }

    public void showWaitingDialog(String title) {
        mDialogHelper.showWaitingDialog(title);
    }

    public void dismissAllDialogs() {
        mDialogHelper.dismissAll();
    }

    private void createContentView() {
        long start = System.currentTimeMillis();
        LayoutInflater inflator = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflator.inflate(R.layout.panorama_module, mRootView, true);
        Log.d(TAG, "createContentView: inflate cost: " + (System.currentTimeMillis() - start));

        // SPRD: Fix bug 575389 and 581078
        if (mActivity.getOrientationManager() != null) {
            RotateLayout panoProgressRotateLayout =
                    (RotateLayout)mRootView.findViewById(R.id.pano_progress_rotate_layout);
            int degrees = mActivity.getOrientationManager().getDeviceOrientation().getDegrees();
            if (panoProgressRotateLayout != null) {
                panoProgressRotateLayout.setOrientation(degrees, true);
            }
            RotateLayout panoModuleReviewLayout =
                    (RotateLayout)mRootView.findViewById(R.id.pano_module_review_layout);
            if (panoModuleReviewLayout != null) {
                panoModuleReviewLayout.setOrientation(degrees, true);
            }
            mPanoSmallPreview =
                    (RotateLayout)mRootView.findViewById(R.id.pano_small_preview_rotate_layout);
            if (mPanoSmallPreview != null) {
                mPanoSmallPreview.setOrientation(degrees, true);
            }
            RotateLayout panoCaptureIndicator =
                    (RotateLayout)mRootView.findViewById(R.id.pano_preview_indicator_rotate_layout);
            if (panoCaptureIndicator != null) {
                panoCaptureIndicator.setOrientation(degrees, true);
            }
            mPanoWaitBar = (RotateLayout)mRootView.findViewById(R.id.pano_wait_rotate_layout);
            if (mPanoWaitBar != null) {
                mPanoWaitBar.setOrientation(degrees, true);
            }
        }
        Resources appRes = mActivity.getResources();
        mIndicatorColor = appRes.getColor(R.color.pano_progress_indication);
        mReviewBackground = appRes.getColor(R.color.review_background);
        mIndicatorColorFast = appRes.getColor(R.color.pano_progress_indication_fast);

        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        mReviewControl = (ViewGroup) mRootView.findViewById(R.id.pano_review_control);
        mReviewLayout = mRootView.findViewById(R.id.pano_review_layout);
        mReview = (ImageView) mRootView.findViewById(R.id.pano_reviewarea);
        mCaptureLayout = (FrameLayout) mRootView.findViewById(R.id.panorama_capture_layout);
        mCaptureProgressBar = (PanoProgressBar) mRootView.findViewById(R.id.pano_pan_progress_bar);
        mCaptureProgressBar.setBackgroundColor(appRes.getColor(R.color.pano_progress_empty));
        mCaptureProgressBar.setDoneColor(appRes.getColor(R.color.pano_progress_done));
        mCaptureProgressBar.setIndicatorColor(mIndicatorColor);
        mCaptureProgressBar.setIndicatorWidth(20);

        mPreviewBorder = mCaptureLayout.findViewById(R.id.pano_preview_area_border);

        mLeftIndicator = mRootView.findViewById(R.id.pano_pan_left_indicator);
        mRightIndicator = mRootView.findViewById(R.id.pano_pan_right_indicator);
        mLeftIndicator.setEnabled(false);
        mRightIndicator.setEnabled(false);
        mTooFastPrompt = (TextView) mRootView.findViewById(R.id.pano_capture_too_fast_textview);
        mCaptureIndicator = mRootView.findViewById(R.id.pano_capture_indicator);

        mShutterButton = (ShutterButton) mActivity.getModuleLayoutRoot().findViewById(
                R.id.shutter_button);
        // mShutterButton.setImageResource(R.drawable.btn_new_shutter);
        /* mShutterButton.setOnShutterButtonListener(mController); */
        mReview.setBackgroundColor(mReviewBackground);

        // TODO: set display change listener properly.
        mTextureView = (TextureView) mRootView.findViewById(R.id.pano_preview_textureview);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(this);

        mDialogHelper = new DialogHelper();
        setViews(appRes);

        // SPRD:Add for bug 479120 wideangle module rotate issue
        mProgressLayout = (FrameLayout) mRootView.findViewById(R.id.pano_progress_layout);

        mPanoBg = mRootView.findViewById(R.id.pano_prog_bg);

        mSmallPreviewView = (SmallPreviewView)mRootView.findViewById(R.id.pano_small_preview);
        mSmallPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float currentX = event.getX();
                float currentY = event.getY();
                Log.i(TAG, "mSmallPreviewArea = " + mSmallPreviewArea + " currentX = " + currentX + " currentY = " + currentY);
                /* SPRD: fix bug674725 can not change director after capture @{ */
                if (!mSmallPreviewArea.contains(currentX, currentY) || mController != null && ((WideAnglePanoramaModule)mController).isShutterClicked()) {
                    return false;
                }
                /* @} */
                mSmallPreviewView.setCaptureDirector(mSmallPreviewView.isCaptureLeft() ?
                        SmallPreviewView.RIGHT_TO_LEFT : SmallPreviewView.LEFT_TO_RIGHT);
                mTouchSmallPreview = true;
                setViewPort(mOrientation);
                mTouchSmallPreview = false;
                if (!mIsLand) {
                    mPanoSmallPreview.setOrientation(mOrientation, true);
                }
                setBackgroundArea();
                return false;
            }
        });
        mSmallPreviewView.setListener(this);
        if (CameraUtil.isNavigationEnable()) {
            mReviewControl.setPaddingRelative(
                    mReviewControl.getPaddingStart(), mReviewControl.getPaddingTop(), mReviewControl.getPaddingEnd(), CameraUtil.getNormalNavigationBarHeight());
        }
        if (CameraUtil.isNewWideAngle()) {
            mPreviewIndicator = (TextView) mRootView.findViewById(R.id.pano_preview_indicator);
            mPreviewIndicator.setVisibility(View.VISIBLE);
        }
    }

    /* SPRD:Add for bug 479120 wideangle module rotate issue @{ */
    public FrameLayout getProgressLayout() {
        return mProgressLayout;
    }

    /* @} */

    private void setViews(Resources appRes) {
        /** SPRD:fix bug 614910 make sure the full screen is true
         *
        int weight = appRes.getInteger(R.integer.SRI_pano_layout_weight);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mPreviewLayout.getLayoutParams();
        lp.weight = weight;
        mPreviewLayout.setLayoutParams(lp);

        lp = (LinearLayout.LayoutParams) mReview.getLayoutParams();
        lp.weight = weight;
        mPreviewLayout.setLayoutParams(lp);
        */

        mSavingProgressBar = (PanoProgressBar) mRootView
                .findViewById(R.id.pano_saving_progress_bar);
        mSavingProgressBar.setIndicatorWidth(0);
        mSavingProgressBar.setMaxProgress(100);
        mSavingProgressBar.setBackgroundColor(appRes.getColor(R.color.pano_progress_empty));
        mSavingProgressBar.setDoneColor(appRes.getColor(R.color.pano_progress_indication));
        /* SPRD: fix bug337352 SavingProgressBar and cancel button didn't show @{ */
        mSavingProgressBar.setVisibility(View.GONE);
        /* @} */

        View cancelButton = mRootView.findViewById(R.id.pano_review_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mShutterButton.setVisibility(View.VISIBLE);
                mController.cancelHighResStitching();
            }
        });
    }

    private void showTooFastIndication() {
        if (CameraUtil.isNewWideAngle()) {
            mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_too_fast_prompt));
        } else {
            mTooFastPrompt.setVisibility(View.VISIBLE);
         // The PreviewArea also contains the border for "too fast" indication.
            mCaptureProgressBar.setIndicatorColor(mIndicatorColorFast);
            mLeftIndicator.setEnabled(true);
            mRightIndicator.setEnabled(true);
        }
        mPreviewBorder.setVisibility(View.VISIBLE);
    }

    private void hideTooFastIndication() {
        if (CameraUtil.isNewWideAngle()) {
            if (mIsLand){
                mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_capture_indication_dream));
            }else{
                mPreviewIndicator.setText(mActivity.getResources().getString(R.string.pano_capture_indication_dream_vertical));
            }
        } else {
            mTooFastPrompt.setVisibility(View.GONE);
            mCaptureProgressBar.setIndicatorColor(mIndicatorColor);
            mLeftIndicator.setEnabled(false);
            mRightIndicator.setEnabled(false);
        }
        mPreviewBorder.setVisibility(View.INVISIBLE);
    }

    public void flipPreviewIfNeeded() {
        // Rotation needed to display image correctly clockwise
        int cameraOrientation = mController.getCameraOrientation();
        // Display rotated counter-clockwise
        int displayRotation = CameraUtil.getDisplayRotation();
        // Rotation needed to display image correctly on current display
        int rotation = (cameraOrientation - displayRotation + 360) % 360;
        if (rotation >= 180) {
            mTextureView.setRotation(180);
        } else {
            mTextureView.setRotation(0);
        }
    }

    public void showPreviewCover() {
        Log.d(TAG, "show mPreviewCover");
        mPreviewCover.setVisibility(View.VISIBLE);
    }

    public void hidePreviewCover() {
        if (mPreviewCover.getVisibility() != View.GONE) {
            Log.d(TAG, "hide mPreviewCover");
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    private class DialogHelper {
        private ProgressDialog mProgressDialog;
        private AlertDialog mAlertDialog;

        DialogHelper() {
            mProgressDialog = null;
            mAlertDialog = null;
        }

        public void dismissAll() {
            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
                mAlertDialog = null;
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }

        public void showAlertDialog(
                CharSequence title, CharSequence message,
                CharSequence buttonMessage, final Runnable buttonRunnable) {
            dismissAll();
            mAlertDialog = (new AlertDialog.Builder(mActivity))
                    .setTitle(title)
                    .setMessage(message)
                    .setNeutralButton(buttonMessage, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            buttonRunnable.run();
                        }
                    })
                    .show();
        }

        public void showWaitingDialog(CharSequence message) {
            dismissAll();
            mProgressDialog = ProgressDialog.show(mActivity, null, message, true, false);
        }
    }

    private static class FlipBitmapDrawable extends BitmapDrawable {

        public FlipBitmapDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            int cx = bounds.centerX();
            int cy = bounds.centerY();
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(180, cx, cy);
            super.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return null;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

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
        // mController.updateCameraOrientation();
    }

    public void initUI() {

    }
    // SPRD Bug WideAngle Rotate
    private View mPanoBg;

    public void adjustUI(int orientation) {
        if (orientation == 0 || orientation == 180) {
            if (mProgressLayout != null && mPanoBg != null && mCaptureProgressBar != null)
                mProgressLayout.setPadding(0, 0, 0, mActivity.getResources().getDimensionPixelSize(R.dimen.pano_pan_progress_bar_padding));
            if(mCaptureIndicator != null)
                mCaptureIndicator.setPadding(mActivity.getResources().getDimensionPixelSize(R.dimen.pano_pan_progress_bar_padding), 0, 0, 0);
            /*
                        mPanoBg.getHeight() - mCaptureProgressBar.getHeight());
                        */
        } else if (orientation == 90 || orientation == 270) {
            if (mProgressLayout != null)
                mProgressLayout.setPadding(0, 0, 0, 20);
            if(mCaptureIndicator != null)
                mCaptureIndicator.setPadding(40, 0, 0, 0);
        }

    }

    private boolean mIsLand = true;
    private boolean mTouchSmallPreview = false;
    private int getOrientation(int orientation, boolean land, boolean manul) {
        Log.i(TAG, " orientation = " + orientation + " land = " + land + " manul = " + manul);
        int orientationAfter = 0;
        if (!land) {
            orientationAfter = (orientation + 90) % 360;
        } else if (manul) {
            orientationAfter = (orientation +360 - 90) % 360;
        } else {
            orientationAfter = orientation;
        }
        return orientationAfter;
    }

    public void switchDirector(boolean isLand, boolean manul) {
        mIsLand = isLand;
        int orientation = getOrientation(mOrientation, mIsLand, manul);
        if (!manul && !isLand && mSmallPreviewView != null && orientation == 180) {
            mSmallPreviewView.setCaptureDirector(mSmallPreviewView.isCaptureLeft() ?
                    SmallPreviewView.RIGHT_TO_LEFT : SmallPreviewView.LEFT_TO_RIGHT);
        }
        if (mPanoSmallPreview != null) {
            mPanoSmallPreview.setOrientation(orientation, true);
        }
        adjustUIDream(orientation);
    }

    public void adjustUIDreamBefore(int orientation) {
        if (mBasicModule != null && !mBasicModule.getCameraPreviewState()) {
            setDisplayOrientation(orientation);
            return;
        }
        int orientationAfter = getOrientation(orientation, mIsLand, false);
        mPanoSmallPreview.setOrientation(orientationAfter, true);
        adjustUIDream(orientationAfter);
    }

    /* Dream Camera ui check 41*/
    public void adjustUIDream(int orientation) {
        int mod = orientation % 180; // Bug 839474: CCN optimization
        if (mod == 0) {
            if (mProgressLayout != null && mPanoBg != null && mCaptureProgressBar != null)
                mProgressLayout.setPadding(0, 0, 0, mActivity.getResources().getDimensionPixelSize(R.dimen.pano_pan_progress_bar_padding));
            hidePreviewCover();
        } else if (mod == 90) {
            if (mProgressLayout != null)
                mProgressLayout.setPadding(0, 0, 0, 20);
            if (mActivity.isFilmstripVisible()) {
                showPreviewCover();
            }
        }

        if (CameraUtil.isNavigationEnable()) {
            if (mod == 0) {
                mReviewControl.setPaddingRelative(
                        mReviewControl.getPaddingStart(), mReviewControl.getPaddingTop(), mReviewControl.getPaddingEnd(), CameraUtil.getNormalNavigationBarHeight());
            } else if (mod == 90) {
                mReviewControl.setPaddingRelative(
                        mReviewControl.getPaddingStart(), mReviewControl.getPaddingTop(), mReviewControl.getPaddingEnd(), 0);

            }
        }

        if (CameraUtil.isNewWideAngle()) {
            setViewPort(orientation);
            setCaptureViewPort(orientation);
            setDisplayOrientation(orientation);
            setBackgroundArea();
        }
    }

    /*SPRD:fix bug627179 show black view when back from filmstripview at landspace @{ */
    private int mOrientation = 0;

    protected void setDisplayOrientation(int orientation) {
        Log.i(TAG, " setDisplayOrientation orientation = " + orientation);
        mOrientation = orientation;
    }

    protected void onPreviewVisibilityChanged() {
        if (mActivity != null && mActivity.isFilmstripVisible() && (mOrientation + 360) % 180 != 0) {
            showPreviewCover();
        } else {
            hidePreviewCover();
        }
    }
    /* @} */

    public Bitmap getPreviewBitmap(int width, int height) {
        return mTextureView.getBitmap(width, height);
    }

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mViewPortX = 0;
    private float mViewPortY = 0;
    private RectF mBackgroundArea = new RectF();
    private RectF mSmallPreviewArea = new RectF();

    public void setPreviewArea(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    private void setViewPort(int orientation) {
        if (mSmallPreviewView == null || mPreviewWidth == 0 || mPreviewHeight == 0) {
            return;
        }

        int viewPortX = 0, viewPortY = 0;
        int left = 0,top = 0, right = mPreviewWidth, bottom = mPreviewHeight;
        int leftSmall = 0, rightSmall = 0;
        if (!mIsLand && !mTouchSmallPreview) {
            mSmallPreviewView.setCaptureDirector(SmallPreviewView.LEFT_TO_RIGHT);
        }
        switch (orientation) {
            case 0:
            case 180:
                viewPortY = (mPreviewHeight - mPreviewHeight / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                if (!mIsLand && orientation == 180 && !mTouchSmallPreview) {
                    mSmallPreviewView.setCaptureDirector(SmallPreviewView.RIGHT_TO_LEFT);
                }
                viewPortX = mSmallPreviewView.isCaptureLeft() ? 0 : mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR;
                /*if (orientation == 0) {
                    viewPortX = mSmallPreviewView.isCaptureLeft() ? 0 : mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR;
                } else {
                    viewPortX = !mSmallPreviewView.isCaptureLeft() ? 0 : mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR;
                }*/
                if (mSmallPreviewView.isCaptureLeft()) {
                    left = mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR;
                    right = mPreviewWidth;
                    leftSmall = 0;
                    rightSmall = left;
                } else {
                    left = 0;
                    right = mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR;
                    leftSmall = right;
                    rightSmall = mPreviewWidth;
                }
                top = viewPortY;
                bottom = (mPreviewHeight + mPreviewHeight / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                break;

            case 90:
                viewPortX = (mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                if (mSmallPreviewView.isCaptureLeft()) {
                    left = mSmallPreviewView.getRightGap() + mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR;
                    right = mPreviewHeight - mSmallPreviewView.getLeftGap();
                    viewPortY = mSmallPreviewView.getRightGap();
                    leftSmall = mSmallPreviewView.getRightGap();
                    rightSmall = left;
                } else {
                    left = mSmallPreviewView.getRightGap();
                    right = mPreviewHeight - mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR - mSmallPreviewView.getLeftGap();
                    viewPortY = right;
                    leftSmall = right;
                    rightSmall = mPreviewHeight - mSmallPreviewView.getLeftGap();
                }
                top = viewPortX;
                bottom = (mPreviewWidth + mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                break;

            case 270:
                viewPortX = (mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                if (mSmallPreviewView.isCaptureLeft()) {
                    left = mSmallPreviewView.getLeftGap() + mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR;
                    right = mPreviewHeight - mSmallPreviewView.getRightGap();
                    viewPortY = mPreviewHeight - mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR - mSmallPreviewView.getLeftGap();
                    leftSmall = mSmallPreviewView.getLeftGap();
                    rightSmall = left;
                } else {
                    left = mSmallPreviewView.getLeftGap();
                    right = mPreviewHeight - mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR - mSmallPreviewView.getRightGap();
                    viewPortY = mSmallPreviewView.getRightGap();
                    leftSmall = right;
                    rightSmall = mPreviewHeight - mSmallPreviewView.getRightGap();
                }
                top = viewPortX;
                bottom = (mPreviewWidth + mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                break;
        }
        mViewPortX = viewPortX;
        mViewPortY = viewPortY;
        mBackgroundArea.set(left, top, right, bottom);
        mSmallPreviewArea.set(leftSmall, top, rightSmall, bottom);
        Log.i(TAG, " orientation = " + orientation + " viewPortX = " + viewPortX + " viewPortY = " + viewPortY + " mBackgroundArea = " + mBackgroundArea + " mSmallPreviewArea = " + mSmallPreviewArea);
        mController.setViewPort(viewPortX, viewPortY, SmallPreviewView.SMALL_PREVIEW_FACTOR, orientation, mSmallPreviewView.getCaptureDirector());
    }

    private void setBackgroundArea() {
        if (mSmallPreviewView != null) {
            mSmallPreviewView.setBackgroundArea(mBackgroundArea);
        }
    }

    @Override
    public void setProgressViewPort(float progress) {
        if (mSmallPreviewView == null) {
            return;
        }
        int director = mSmallPreviewView.getCaptureDirector();
        switch (mOrientation) {
            case 0:
            case 180:
                mController.setViewPort(progress, mViewPortY, SmallPreviewView.SMALL_PREVIEW_FACTOR, mOrientation, director);
                break;
            case 90:
                mController.setViewPort(mViewPortX, progress + mSmallPreviewView.getRightGap(), SmallPreviewView.SMALL_PREVIEW_FACTOR, mOrientation, director);
                break;
            case 270:
                mController.setViewPort(mViewPortX, mSmallPreviewView.isCaptureLeft() ? (mViewPortY - progress)
                        : (mPreviewHeight - mPreviewHeight/SmallPreviewView.SMALL_PREVIEW_FACTOR - mSmallPreviewView.getLeftGap() - progress), SmallPreviewView.SMALL_PREVIEW_FACTOR, mOrientation, director);
                break;
        }
    }

    private void setCaptureViewPort(int orientation) {
        if (mSmallPreviewView == null) {
            return;
        }
        int viewPortX = 0, viewPortY = 0, width = 0, height = 0;
        switch (orientation) {
            case 0:
            case 180:
                viewPortY = (mPreviewHeight - mPreviewHeight / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                width = (int)(mBackgroundArea.width() + mSmallPreviewArea.width());
                height = (int)mBackgroundArea.height();
                break;
            case 90:
            case 270:
                viewPortX = (mPreviewWidth - mPreviewWidth / SmallPreviewView.SMALL_PREVIEW_FACTOR) / 2;
                viewPortY = mSmallPreviewView.getRightGap();
                height = (int)(mBackgroundArea.width() + mSmallPreviewArea.width());
                width = (int)mBackgroundArea.height();
                break;
        }
        mController.setCaptureViewPort(viewPortX, viewPortY, width, height);
    }

    @Override
    public void stopCapture() {
        mController.stopCapture();
    }

    public void setSmallPreviewVisible(boolean visible) {
        if (mSmallPreviewView != null) {
            mSmallPreviewView.setVisible(visible);
        }
    }

    @Override
    public void onDestroy() {
        if (mSmallPreviewView != null) {
            mSmallPreviewView.setOnTouchListener(null);
            mSmallPreviewView.setListener(null);
        }
    }

    public void changeOtherUIVisible(int visible) {
        mActivity.getCameraAppUI().updateTopPanelUI(visible);
    }
}
