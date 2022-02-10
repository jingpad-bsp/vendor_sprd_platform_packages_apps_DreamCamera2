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

package com.android.camera.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.CameraPerformanceTracker;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.AnimationManager;
import com.android.camera.ButtonManager;
import com.android.camera.CameraActivity;
import com.android.camera.ShutterButton;
import com.android.camera.SurfaceViewEx;
import com.android.camera.TextureViewHelper;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.ui.BottomBar;
import com.android.camera.ui.BoxGridLines;
import com.android.camera.ui.CaptureAnimationOverlay;
import com.android.camera.ui.GoldenGridLines;
import com.android.camera.ui.GridLines;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.ModeTransitionView;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.ReticleGridLines;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateAnimation;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.StickyBottomCaptureLayout;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.ui.focus.ChasingView;
import com.android.camera.ui.focus.FocusRing;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera.widget.FilmstripLayout;
import com.android.camera.widget.FilmstripView;
import com.android.camera.widget.ModeOptionsOverlay;
import com.android.camera.widget.RoundedThumbnailView;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.dream.camera.DreamOrientation;
import com.dream.camera.DreamUI;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.ZoomPanel;
import com.dream.camera.dreambasemodules.DreamInterface;
import com.dream.camera.modules.filter.DreamFilterModuleController;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DataStorageStruct;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout;
import com.dream.camera.settings.DreamUIPreferenceSettingLayout.SettingUIListener;
import com.dream.camera.ui.AdjustFlashPanel;
import com.dream.camera.ui.AdjustPanel;
import com.dream.camera.ui.BlurPanel;
import com.dream.camera.ui.DreamCaptureLayoutHelper;
import com.dream.camera.util.Counter;
import com.dream.camera.util.DreamUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * CameraAppUI centralizes control of views shared across modules. Whereas
 * module specific views will be handled in each Module UI. For example, we can
 * now bring the flash animation and capture animation up from each module to
 * app level, as these animations are largely the same for all modules. This
 * class also serves to disambiguate touch events. It recognizes all the swipe
 * gestures that happen on the preview by attaching a touch listener to a
 * full-screen view on top of preview TextureView. Since CameraAppUI has
 * knowledge of how swipe from each direction should be handled, it can then
 * redirect these events to appropriate recipient views.
 */

/*
 * SPRD Bug:519334 Refactor Rotation UI of Camera. @{ Original Android code:
 * public class CameraAppUI implements ModeListView.ModeSwitchListener,
 * TextureView.SurfaceTextureListener, ModeListView.ModeListOpenListener,
 * SettingsManager.OnSettingChangedListener,
 * ShutterButton.OnShutterButtonListener {
 */
public class CameraAppUI implements ModeListView.ModeSwitchListener,
        TextureView.SurfaceTextureListener, ModeListView.ModeListOpenListener,
        ShutterButton.OnShutterButtonListener,
        OrientationManager.OnOrientationChangeListener, AdjustFlashPanel.AdjustFlashPanelInterface {

    /**
     * BottomBarUISpec provides a structure for modules to specify their ideal
     * bottom bar mode options layout. Once constructed by a module, this class
     * should be treated as read only. The application then edits this spec
     * according to hardware limitations and displays the final bottom bar ui.
     */
    public static class BottomBarUISpec {
        /** Mode options UI */

        /**
         * Set true if the camera option should be enabled. If not set or false,
         * and multiple cameras are supported, the camera option will be
         * disabled. If multiple cameras are not supported, this preference is
         * ignored and the camera option will not be visible.
         */
        public boolean enableCamera;

        /**
         * Set true if the camera option should not be visible, regardless of
         * hardware limitations.
         */
        public boolean hideCamera;

        /**
         * Set true if the photo flash option should be enabled. If not set or
         * false, the photo flash option will be disabled. If the hardware does
         * not support multiple flash values, this preference is ignored and the
         * flash option will be disabled. It will not be made invisible in order
         * to preserve a consistent experience across devices and between front
         * and back cameras.
         */
        public boolean enableFlash;

        /**
         * Set true if the video flash option should be enabled. Same disable
         * rules apply as the photo flash option.
         */
        public boolean enableTorchFlash;

        /**
         * Set true if the HDR+ flash option should be enabled. Same disable
         * rules apply as the photo flash option.
         */
        public boolean enableHdrPlusFlash;

        /**
         * Set true if flash should not be visible, regardless of hardware
         * limitations.
         */
        public boolean hideFlash;

        /**
         * Set true if the hdr/hdr+ option should be enabled. If not set or
         * false, the hdr/hdr+ option will be disabled. Hdr or hdr+ will be
         * chosen based on hardware limitations, with hdr+ prefered. If hardware
         * supports neither hdr nor hdr+, then the hdr/hdr+ will not be visible.
         */
        public boolean enableHdr;

        /**
         * Set true if hdr/hdr+ should not be visible, regardless of hardware
         * limitations.
         */
        public boolean hideHdr;

        /**
         * Set true if grid lines should be visible. Not setting this causes
         * grid lines to be disabled. This option is agnostic to the hardware.
         */
        public boolean enableGridLines;

        /**
         * Set true if grid lines should not be visible.
         */
        public boolean hideGridLines;

        /**
         * Set true if the panorama orientation option should be visible. This
         * option is not constrained by hardware limitations.
         */
        public boolean enablePanoOrientation;

        /**
         * Set true if manual exposure compensation should be visible. This
         * option is not constrained by hardware limitations. For example, this
         * is false in HDR+ mode.
         */
        public boolean enableExposureCompensation;

        /**
         * Set true if the device and module support exposure compensation. Used
         * only to show exposure button in disabled (greyed out) state.
         */
        public boolean isExposureCompensationSupported;

        /** Intent UI */

        /**
         * Set true if the intent ui cancel option should be visible.
         */
        public boolean showCancel;
        /**
         * Set true if the intent ui done option should be visible.
         */
        public boolean showDone;
        /**
         * Set true if the intent ui retake option should be visible.
         */
        public boolean showRetake;
        /**
         * Set true if the intent ui review option should be visible.
         */
        public boolean showReview;

        /** Mode options callbacks */

        /**
         * A {@link com.android.camera.ButtonManager.ButtonCallback} that will
         * be executed when the camera option is pressed. This callback can be
         * null.
         */
        public ButtonManager.ButtonCallback cameraCallback;

        /**
         * A {@link com.android.camera.ButtonManager.ButtonCallback} that will
         * be executed when the flash option is pressed. This callback can be
         * null.
         */
        public ButtonManager.ButtonCallback flashCallback;

        /**
         * A {@link com.android.camera.ButtonManager.ButtonCallback} that will
         * be executed when the hdr/hdr+ option is pressed. This callback can be
         * null.
         */
        public ButtonManager.ButtonCallback hdrCallback;

        /**
         * A {@link com.android.camera.ButtonManager.ButtonCallback} that will
         * be executed when the grid lines option is pressed. This callback can
         * be null.
         */
        public ButtonManager.ButtonCallback gridLinesCallback;

        /**
         * A {@link com.android.camera.ButtonManager.ButtonCallback} that will
         * execute when the panorama orientation option is pressed. This
         * callback can be null.
         */
        public ButtonManager.ButtonCallback panoOrientationCallback;

        /** Intent UI callbacks */

        /**
         * A {@link android.view.View.OnClickListener} that will execute when
         * the cancel option is pressed. This callback can be null.
         */
        public View.OnClickListener cancelCallback;

        /**
         * A {@link android.view.View.OnClickListener} that will execute when
         * the done option is pressed. This callback can be null.
         */
        public View.OnClickListener doneCallback;

        /**
         * A {@link android.view.View.OnClickListener} that will execute when
         * the retake option is pressed. This callback can be null.
         */
        public View.OnClickListener retakeCallback;

        /**
         * A {@link android.view.View.OnClickListener} that will execute when
         * the review option is pressed. This callback can be null.
         */
        public View.OnClickListener reviewCallback;

        /**
         * A ExposureCompensationSetCallback that will execute when an expsosure
         * button is pressed. This callback can be null.
         */
        public interface ExposureCompensationSetCallback {
            public void setExposure(int value);
        }

        public ExposureCompensationSetCallback exposureCompensationSetCallback;

        /**
         * Exposure compensation parameters.
         */
        public int minExposureCompensation;
        public int maxExposureCompensation;
        public float exposureCompensationStep;

        /**
         * Whether self-timer is enabled.
         */
        public boolean enableSelfTimer = false;

        /**
         * Whether the option for self-timer should show. If true and
         * {@link #enableSelfTimer} is false, then the option should be shown
         * disabled.
         */
        public boolean showSelfTimer = false;
    }

    private final static Log.Tag TAG = new Log.Tag("CameraAppUI Dream");

    private final CameraActivity mController;
    private final boolean mIsCaptureIntent;
    private final AnimationManager mAnimationManager;

    // Swipe states:
    private final static int IDLE = 0;
    private final static int SWIPE_UP = 1;
    private final static int SWIPE_DOWN = 2;
    private final static int SWIPE_LEFT = 3;
    private final static int SWIPE_RIGHT = 4;
    private boolean mSwipeEnabled = true;

    // Shared Surface Texture properities.
    private SurfaceTexture mSurface;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    // Touch related measures:
    private final int mSlop;
    private final static int SWIPE_TIME_OUT_MS = 500;

    // Mode cover states:
    private final static int COVER_HIDDEN = 0;
    private final static int COVER_SHOWN = 1;
    private final static int COVER_WILL_HIDE_AT_NEXT_FRAME = 2;
    private final static int COVER_WILL_HIDE_AFTER_NEXT_TEXTURE_UPDATE = 3;
    private final static int COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE = 4;

    /**
     * Preview down-sample rate when taking a screenshot.
     */
    private final static int DOWN_SAMPLE_RATE_FOR_SCREENSHOT = 2;
    private final static int DOWN_SAMPLE_RATE_FOR_BLUR_SCREENSHOT = 4;

    // App level views:
    private final FrameLayout mCameraRootView;
    private final ModeTransitionView mModeTransitionView;
    private final MainActivityLayout mAppRootView;
    private ModeListView mModeListView;
    private FilmstripLayout mFilmstripLayout;
    private FilmstripView mFilmstripView;
    private TextureView mTextureView;
    /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
    private View mTextureViewCover;
    /* @} */
    private FrameLayout mModuleUI;
    public FrameLayout mBottomFrame;
    public FrameLayout mTopFrame;
    private ShutterButton mShutterButton;
    private ImageButton mCountdownCancelButton;
    private BottomBar mBottomBar;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private FrameLayout mUnderPreviewOverlay;
    private FrameLayout mOverPreviewOverlay;
    private FocusRing mFocusRing;
    private ChasingView mChasingRing;
    private AdjustPanel mAdjustPanel;
    private BlurPanel mBlurPanel;
//    private FrameLayout mTutorialsPlaceHolderWrapper;
    private StickyBottomCaptureLayout mStickyBottomCaptureLayout;
    private TextureViewHelper mTextureViewHelper;
    private final GestureDetector mGestureDetector;
    private int mLastRotation;
    private int mSwipeState = IDLE;
    private PreviewOverlay mPreviewOverlay;
    private View mSettingLayoutBackground;
    private GridLines mGridLines;
    /* SRPD: add more gridline type @{ */
    private GridLines mNineGridLines;
    private ReticleGridLines mReticleGridLines;
    private BoxGridLines mBoxGridLines;
    private GoldenGridLines mGoldenGridLines;
    /* @} */
    private ZoomPanel mZoomPanel;
    private CaptureAnimationOverlay mCaptureOverlay;
    private PreviewStatusListener mPreviewStatusListener;
    private int mModeCoverState = COVER_HIDDEN;
    private FilmstripContentPanel mFilmstripPanel;
    DreamUIPreferenceSettingLayout mSettingLayout;

    private TextView mCurrentModuleAddTip;
    private TextView mBlurEffectTip;//Add another tips for blur
    private TextView mCurrentModuleAddSecondTip;
    private Runnable mHideCurrentModuleTipRunnable;
    private Runnable mshowRefocusModuleTipRunnable;
    // SPRD: Fix 474843 Add for Filter Feature
    private DreamFilterModuleController mDreamFilterModuleController;

    private ImageView mSwitchPreview;
    private View mSwitchBackGround;
    private boolean mStartSwitchAnimation = false;
    private Bitmap mSwitchBitmap = null;
    private float mAspectRatio = 1.7778f;

    private Runnable mCurrentHideRunnable;
    private Runnable mHideCoverRunnable;
    private Runnable mFreeScreenHideRunnable;
    private Runnable mModeSelectHideRunnable;

    private final View.OnLayoutChangeListener mPreviewLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (mPreviewStatusListener != null) {
                mPreviewStatusListener.onPreviewLayoutChanged(v, left, top,
                        right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            }
        }
    };
    private RoundedThumbnailView mRoundedThumbnailView;
    private final DreamCaptureLayoutHelper mCaptureLayoutHelper;

    public int mBottomHeightOptimal = 1;

    private boolean mDisableAllUserInteractions;
    /** Whether to prevent capture indicator from being triggered. */
    private boolean mSuppressCaptureIndicator;

    /** Supported HDR mode (none, hdr, hdr+). */
    private String mHdrSupportMode;

    /** Used to track the last scope used to update the bottom bar UI. */
    private String mCurrentCameraScope;
    private String mCurrentModuleScope;

    // Add for dream settings.
    private DataModuleBasic mDataModule;
    // ADD for bug 612207.
    private boolean mIsDestroyed = false;

    /**
     * Provides current preview frame and the controls/overlay from the module
     * that are shown on top of the preview.
     */
    public interface CameraModuleScreenShotProvider {
        /**
         * Returns the current preview frame down-sampled using the given
         * down-sample factor.
         * 
         * @param downSampleFactor
         *            the down sample factor for down sampling the preview
         *            frame. (e.g. a down sample factor of 2 means to scale down
         *            the preview frame to 1/2 the width and height.)
         * @return down-sampled preview frame
         */
        public Bitmap getPreviewFrame(int downSampleFactor);
        public Bitmap getPreviewFrameWithoutTransform(int downSampleFactor);

        /**
         * @return the controls and overlays that are currently showing on top
         *         of the preview drawn into a bitmap with no scaling applied.
         */
        public Bitmap getPreviewOverlayAndControls();

        /**
         * Returns a bitmap containing the current screenshot.
         * 
         * @param previewDownSampleFactor
         *            the downsample factor applied on the preview frame when
         *            taking the screenshot
         */
        public Bitmap getScreenShot(int previewDownSampleFactor);
        // SPRD: Fix bug 574388 the preview screen will be black in filter to switch
        public Bitmap getScreenShot(Bitmap bmp);
        // SPRD: Fix bug 595400 the freeze screen for gif
        public Bitmap getScreenShot(Bitmap bmp, RectF previewArea);
        public Bitmap getScreenShot(Bitmap bmp, boolean hasViews);
        public Bitmap getBlackPreviewFrame(int downSampleFactor);

        public Bitmap getBlackPreviewFrameWithButtons();
    }

    /**
     * This listener gets called when the size of the window (excluding the
     * system decor such as status bar and nav bar) has changed.
     */
    public interface NonDecorWindowSizeChangedListener {
        void onNonDecorWindowSizeChanged(int width, int height, int rotation);
    }

    private final CameraModuleScreenShotProvider mCameraModuleScreenShotProvider
            = new CameraModuleScreenShotProvider() {
        @Override
        public Bitmap getPreviewFrame(int downSampleFactor) {
            if (mCameraRootView == null || mTextureView == null) {
                return null;
            }
            // Gets the bitmap from the preview TextureView.
            Bitmap preview = mTextureViewHelper
                    .getPreviewBitmap(downSampleFactor);
            return preview;
        }

        @Override
        public Bitmap getPreviewFrameWithoutTransform(int downSampleFactor) {
            if (mCameraRootView == null || mTextureView == null) {
                return null;
            }
            // Gets the bitmap from the preview TextureView.
            Bitmap preview = mTextureViewHelper
                    .getPreviewBitmapWithoutTransform(downSampleFactor);
            return preview;
        }

        @Override
        public Bitmap getPreviewOverlayAndControls() {
            Bitmap overlays = Bitmap.createBitmap(mCameraRootView.getWidth(),
                    mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlays);
            if (slidePanel == null) {
                mCameraRootView.draw(canvas);
                return overlays;
            }
            slidePanel.setVisibility(View.INVISIBLE);
            mUnderPreviewOverlay.setVisibility(View.INVISIBLE);
            mOverPreviewOverlay.setVisibility(View.INVISIBLE);
            // hotspot method
            mCameraRootView.draw(canvas);
            slidePanel.setVisibility(View.VISIBLE);
            mUnderPreviewOverlay.setVisibility(View.VISIBLE);
            mOverPreviewOverlay.setVisibility(View.VISIBLE);
            return overlays;
        }

        @Override
        public Bitmap getScreenShot(int previewDownSampleFactor) {
            Bitmap preview;
            /*SPRD:fix bug629900 add freeze blur @{*/
            if (CameraUtil.isFreezeBlurEnable()) {
                // use DOWN_SAMPLE_RATE_FOR_BLUR_SCREENSHOT will save 200ms times
                preview = mTextureViewHelper.getPreviewBitmap(DOWN_SAMPLE_RATE_FOR_BLUR_SCREENSHOT);
                preview = CameraUtil.blurBitmap(preview, mController.getAndroidContext());
            } else {
                preview = mTextureViewHelper.getPreviewBitmap(previewDownSampleFactor);
            }
            /* @} */
            return getScreenShot(preview, mTextureViewHelper.getPreviewArea());
        }

        /* SPRD: Fix bug 574388 the preview screen will be black in filter to switch @{ */
        @Override
        public Bitmap getScreenShot(Bitmap bmp) {
            return getScreenShot(bmp, mTextureViewHelper.getPreviewArea());
        }
        /* @} */

        /* SPRD: Fix bug 595400 the freeze screen for gif @{ */
        @Override
        public Bitmap getScreenShot(Bitmap bmp, RectF previewArea) {
            Bitmap screenshot = Bitmap.createBitmap(mCameraRootView.getWidth(),
                    mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(screenshot);
            if (!mStartSwitchAnimation) {
                canvas.drawARGB(255, 0, 0, 0);
                if (bmp != null) {
                    if(previewArea.right > previewArea.bottom){
                        Log.i(TAG, "previewArea1: " + previewArea);
                        previewArea.set(0 , 0 , previewArea.bottom , previewArea.right);
                    }
                    Log.i(TAG, "previewArea2: " + previewArea);
                    
                    canvas.drawBitmap(bmp, null, previewArea, null);
                }
                Bitmap overlay = getPreviewOverlayAndControls();
                if (overlay != null) {
                    canvas.drawBitmap(overlay, 0f, 0f, null);
                }
            } else {
                canvas.drawARGB(0, 0, 0, 0);
            }
            return screenshot;
        }
        /* @} */

        /**
         * get blurred screenShot bitmap from original screenshot<p>
         * NOTE: this method added for Camera performance increase about back ot front camera changes
         * @param bmp original screenshot bitmap
         * @param hasViews whether has the views of interface
         * @return blurred screenShot bitmap
         */
        @Override
        public Bitmap getScreenShot(Bitmap bmp, boolean hasViews) {
            RectF previewArea = getPreviewArea();
            if (!CameraUtil.isFreezeBlurEnable()) {
                return bmp;
            }
            long startTime = System.currentTimeMillis();
            // Bitmap screenshot = Bitmap.createBitmap(mCameraRootView.getWidth(),
            // mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            // OR use follow (may be cost more time than previous):
            Bitmap screenshot = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Log.i(TAG, "bmp.copy cost: " + (System.currentTimeMillis() - startTime));

            final float scale = 0.2f;
            Canvas canvas = new Canvas(screenshot);
            if (!mStartSwitchAnimation) {
                Bitmap blurredBitMap = CameraUtil.blurBitmap(CameraUtil.computeScale(bmp, scale),
                        mController.getAndroidContext());
                Rect rect = new Rect((int)(previewArea.left * scale),(int)(previewArea.top * scale),
                        (int)(previewArea.right * scale),(int)(previewArea.bottom * scale));
                
                if(previewArea.right > previewArea.bottom){
                    Log.i(TAG, "previewArea3: " + previewArea);
                    previewArea.set(0 , 0 , previewArea.bottom , previewArea.right);
                }
                Log.i(TAG, "previewArea4: " + previewArea);

                canvas.drawBitmap(blurredBitMap, rect, previewArea, null);
                Bitmap overlay = getPreviewOverlayAndControls();
                if (overlay != null) {
                    canvas.drawBitmap(overlay, 0f, 0f, null);
                }
            } else {
                canvas.drawARGB(0, 0, 0, 0);
            }
            return screenshot;
        }
        /* @} */

        /* SPRD: Fix bug 612561, get wrong background @{ */
        @Override
        public Bitmap getBlackPreviewFrame(int downSampleFactor) {
            if (mCameraRootView == null || mTextureView == null) {
                return null;
            }
            Bitmap preview = mTextureViewHelper.getPreviewBitmap(downSampleFactor);
            if (preview != null) {
                Canvas canvas = new Canvas(preview);
                canvas.drawARGB(255, 0, 0, 0);
            }
            return preview;
        }

        @Override
        public Bitmap getBlackPreviewFrameWithButtons() {
            Bitmap screenshot = Bitmap.createBitmap(mCameraRootView.getWidth(),
                    mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(screenshot);
            if (!mStartSwitchAnimation) {
                canvas.drawARGB(255, 0, 0, 0);
                Bitmap overlay = getPreviewOverlayAndControls();
                if (overlay != null) {
                    canvas.drawBitmap(overlay, 0f, 0f, null);
                }
            } else {
                canvas.drawARGB(0, 0, 0, 0);
            }
            return screenshot;
        }
    };

    private long mCoverHiddenTime = -1; // System time when preview cover was
                                        // hidden.

    public long getCoverHiddenTime() {
        return mCoverHiddenTime;
    }

    /**
     * This resets the preview to have no applied transform matrix.
     */
    public void clearPreviewTransform() {
        mTextureViewHelper.clearTransform();
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        mTextureViewHelper.updateAspectRatio(aspectRatio);
        updateSwitchPreviewPara();
    }

    /**
     * WAR: Reset the SurfaceTexture's default buffer size to the current view
     * dimensions of its TextureView. This is necessary to get the expected
     * behavior for the TextureView's HardwareLayer transform matrix (set by
     * TextureView#setTransform) after configuring the SurfaceTexture as an
     * output for the Camera2 API (which involves changing the default buffer
     * size). b/17286155 - Tracking a fix for this in HardwareLayer.
     */
    public void setDefaultBufferSizeToViewDimens() {
        if (mSurface == null || mTextureView == null) {
            Log.w(TAG,
                    "Could not set SurfaceTexture default buffer dimensions, not yet setup");
            return;
        }
        mSurface.setDefaultBufferSize(mTextureView.getWidth(),
                mTextureView.getHeight());
    }

    /**
     * Updates the preview matrix without altering it.
     * 
     * @param matrix
     * @param aspectRatio
     *            the desired aspect ratio for the preview.
     */
    public void updatePreviewTransformFullscreen(Matrix matrix,
            float aspectRatio) {
        mTextureViewHelper.updateTransformFullScreen(matrix, aspectRatio);
    }

    /**
     * @return the rect that will display the preview.
     */
    public RectF getFullscreenRect() {
        return mTextureViewHelper.getFullscreenRect();
    }

    /**
     * This is to support modules that calculate their own transform matrix
     * because they need to use a transform matrix to rotate the preview.
     * 
     * @param matrix
     *            transform matrix to be set on the TextureView
     */
    public void updatePreviewTransform(Matrix matrix) {
        mTextureViewHelper.updateTransform(matrix);
    }

    public interface AnimationFinishedListener {
        public void onAnimationFinished(boolean success);
    }

    private class MyTouchListener implements View.OnTouchListener {
        private boolean mScaleStarted = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mScaleStarted = false;
            } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                mScaleStarted = true;
            }
            return (!mScaleStarted) && mGestureDetector.onTouchEvent(event);
        }
    }
    private class HideCoverRunnable implements Runnable {
        @Override
        public void run() {
            mModeTransitionView.hideModeCover(null);
            ((CameraActivity)mController).initModelistview();
        }
    }
    private class ModeSelectCoverRunnable implements Runnable {
        @Override
        public void run() {
            mModeListView.startModeSelectionAnimation();
            waitToHide = false;
        }
    }
    private class FreezeCoverRunnable implements Runnable {
        @Override
        public void run() {
            mModeTransitionView.hideImageCover();
        }
    }
    /**
     * This gesture listener finds out the direction of the scroll gestures and
     * sends them to CameraAppUI to do further handling.
     */
    private class MyGestureListener extends
            GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev,
                float distanceX, float distanceY) {
            if (ev.getEventTime() - ev.getDownTime() > SWIPE_TIME_OUT_MS
                    || mSwipeState != IDLE || mIsCaptureIntent
                    || !mSwipeEnabled) {
                return false;
            }
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (!doGestureScrollActionMove(deltaX,deltaY))
                    return false;
            }
            return true;
        }

        private boolean doGestureScrollActionMove(int deltaX,int deltaY){
            if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                // Calculate the direction of the swipe.
                if (deltaX >= Math.abs(deltaY)) {
                    if (mModeListView == null || mModeListView.getModeListSize() == 1) {
                        return false;
                    }
                    // Swipe right.
                    if(mDreamUI != null && !mDreamUI.isFreezeFrameShow() && !mController.isSecureCamera())
                        setSwipeState(SWIPE_RIGHT);
                } else if (deltaX <= -Math.abs(deltaY)) {
                    // Swipe left.
                    // SPRD Bug:519334 Refactor Rotation UI of Camera.
                    // if (nowOrientation % 180 == 0)
                    // setSwipeState(SWIPE_LEFT);
                    if (mController != null) {
                        // SPRD Bug:607389 if zoomPanel visible, can not swith to DreamFilterModule
                        if (mZoomPanel != null  && (mZoomPanel.isZoomSimple() && mZoomPanel.getVisibility() == View.VISIBLE)) {
                            return false;
                        }
                        setSwipeState(SWIPE_LEFT);
                    }
                }
            }
            return true;
        }

        private void setSwipeState(int swipeState) {
            mSwipeState = swipeState;
            // Notify new swipe detected.
            onSwipeDetected(swipeState);
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            mDown = MotionEvent.obtain(ev);
            mSwipeState = IDLE;
            return false;
        }

        /*
         * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
         */
        // @Override
        // public boolean onFling(MotionEvent e1, MotionEvent e2, float
        // velocityX,
        // float velocityY) {
        // if (((Math.abs(velocityX) < velocityY) && nowOrientation == 90)) {
        // Log.i(TAG, "nowOrientation=" + nowOrientation + "-showFilmstrip");
        // ((Activity) mController)
        // .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        // showFilmstrip();
        // } else if ((-Math.abs(velocityX) > velocityY) && nowOrientation ==
        // 270) {
        // Log.i(TAG, "nowOrientation=" + nowOrientation + "-showFilmstrip");
        // ((Activity) mController)
        // .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        // showFilmstrip();
        // } else if (((Math.abs(velocityX) < velocityY) && nowOrientation ==
        // 270)
        // || (-Math.abs(velocityX) > velocityY) && nowOrientation == 90) {
        // Log.i(TAG, "nowOrientation=" + nowOrientation + "-showmodelist");
        // }
        // return false;
        // }
        /* @} */
    }

    public CameraAppUI(AppController controller,
            MainActivityLayout appRootView, boolean isCaptureIntent) {
        mSlop = ViewConfiguration.get(controller.getAndroidContext())
                .getScaledTouchSlop();
        mController = (CameraActivity) controller;
        mIsCaptureIntent = isCaptureIntent;
        mshowRefocusModuleTipRunnable = new Runnable() {
            @Override
            public void run() {
                if (mCurrentModuleAddTip != null) {
                    mCurrentModuleAddTip.setVisibility(View.GONE);
                }
                if(mController.getCurrentModule() != null){
                    mController.getCurrentModule().forceOnSensorSelfShot();
                }
            }
        };
        mAppRootView = appRootView;
        mCurrentModuleAddTip = (TextView) appRootView
                .findViewById(R.id.current_module_tip);
        mCurrentModuleAddSecondTip = (TextView) mAppRootView
                .findViewById(R.id.current_module_second_tip);
        mBlurEffectTip = (TextView) mAppRootView
                .findViewById(R.id.blur_effect_tip);
        mCameraRootView = (FrameLayout) appRootView
                .findViewById(R.id.camera_app_root);
        mModeTransitionView = (ModeTransitionView) mAppRootView
                .findViewById(R.id.mode_transition_view);
        mBottomFrame=(FrameLayout)appRootView.findViewById(R.id.bottom_frame);
        mTopFrame=(FrameLayout)appRootView.findViewById(R.id.top_frame);
        mGestureDetector = new GestureDetector(controller.getAndroidContext(),
                new MyGestureListener());
        Resources res = controller.getAndroidContext().getResources();
        mCaptureLayoutHelper = new DreamCaptureLayoutHelper(
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_min),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_max),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
        mCaptureLayoutHelper.setActivity(mController);
        mModeListView = (ModeListView) appRootView
                .findViewById(R.id.mode_list_layout);
        mAnimationManager = new AnimationManager();

        mAppRootView.setNonDecorWindowSizeChangedListener(mCaptureLayoutHelper);
        mSuppressCaptureIndicator = false;

        mDataModule = DataModuleManager.getInstance(
                mController.getAndroidContext()).getDataModuleCamera();

        initAllPanels();
//        initBottomButtons();
        // SPRD: Fix 474843 Add for Filter Feature
        mDreamFilterModuleController = new DreamFilterModuleController(
                controller, appRootView);
        // SPRD: Fix 533869 Add for voice picture of new ui
        mSwitchPreview = (ImageView)mAppRootView.findViewById(R.id.switch_transition_view);
        mSwitchBackGround = mAppRootView.findViewById(R.id.switch_background_view);
        mAdjustPanel = (AdjustPanel) mAppRootView.findViewById(R.id.adjust_panel);
        mBlurPanel = (BlurPanel) mAppRootView.findViewById(R.id.blur_panel);
    }

    /**
     * Freeze what is currently shown on screen until the next preview frame comes in.
     */
    public void freezeScreenUntilPreviewReady() {
        Log.v(TAG, "freezeScreenUntilPreviewReady default");
        freezeScreen(DOWN_SAMPLE_RATE_FOR_SCREENSHOT);
    }

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    public void freezeScreenUntilPreviewReady(Bitmap bitmap) {
        Log.v(TAG, "freezeScreenUntilPreviewReady with bitmap: " + bitmap);
        /* SPRD: Fix bug 574388 the preview screen will be black in filter to switch @{ */
        freezeScreen(bitmap);
        /* @} */
     }
    /* @} */

    /* SPRD: Fix bug 595400 the freeze screen for gif @{ */
    public void freezeScreenUntilPreviewReady(Bitmap bitmap, RectF previewArea) {
        Log.v(TAG, "freezeScreenUntilPreviewReady bitmap=" + bitmap + "; previewArea=" + previewArea);
        freezeScreen(bitmap, previewArea);
    }
    /* @} */

    /**
     * freeze Screen Until PreviewReady witch bitmap generate from SurfaceControl
     * @param bitmap the screen shot of camera interface
     * @param hasViews if the bitmap generate from SurfaceControl and has other views,
     *                 it should be true, this param not be used right now
     */
    public void freezeScreenUntilPreviewReady(Bitmap bitmap, boolean hasViews) {
        Log.v(TAG, "freezeScreenUntilPreviewReady bitmap=" + bitmap + "; hasViews=" + hasViews);
        RectF previewArea = getPreviewArea();
        // judge preview is 4/3 or 16/9, 0.12 is a empirical value
        boolean isPreviewShorter = (float)previewArea.width() / (float)previewArea.height() >= (3f / 4f);
        Size displayRealSize = CameraUtil.getDefaultDisplayRealSize();
        //Bug795513 Android Go'Screen is 5:3
        boolean isScreenShorter = (float)displayRealSize.getHeight() / (float)displayRealSize.getWidth() < (16f / 9f);
        // if the preview width to height ratio of 4 to 3 and the current module is photoModule
        // do freeze screen with new method optimized
        if (isPreviewShorter && !isScreenShorter) {
            Log.d(TAG, "freezeScreenUntilPreviewReady: 4:3");
            freezeScreen(bitmap, hasViews);
        } else {
            if (!CameraUtil.isFreezeBlurEnable()) {
                // if need not blur, set it to image cover directly
                freezeScreen(bitmap, hasViews);
            } else {
                previewArea = new RectF(0, 0, displayRealSize.getWidth(), displayRealSize.getHeight());
                Bitmap blurredBitMap = CameraUtil.blurBitmap(CameraUtil.computeScale(bitmap, 0.2f),
                        mController.getAndroidContext());
                freezeScreen(blurredBitMap, previewArea);
            }
        }
    }

    public void freezeScreenBlackUntilPreviewReady() {
        Log.v(TAG, "freezeScreenBlackUntilPreviewReady");
        freezeScreen();
    }
    public boolean getFreezeScreenFlag(){
        if(mModeCoverState != COVER_HIDDEN){
            return true;
        }else {
            return false;
        }
    }

    public boolean isShutterButtonPressed() {
        if(mShutterButton!= null){
            return mShutterButton.isButtonPressed();
        }else {
            return false;
        }
    }

    /**
     * freeze screen with bitmap or screen shot bitmap until the camera preview call back
     * @param ts Uncertain type and number of parameters, you can call this method with a bitmap
     *           or with a part of bitmap or a int number of down sample rate for screen shot
     * @param <T> any type of {@link Integer}, {@link Bitmap}, {@link RectF}
     */
    @SafeVarargs
    private final <T> void freezeScreen(T... ts) {
        Bitmap bitmap;
        if (ts == null || ts.length == 0) {
            bitmap = mCameraModuleScreenShotProvider.getBlackPreviewFrameWithButtons();
        } else if (ts.length == 1 && ts[0] instanceof Bitmap) {
            //Filter module
            bitmap = mCameraModuleScreenShotProvider.getScreenShot((Bitmap) ts[0]);
        } else if (ts.length >= 2 && ts[0] instanceof Bitmap && ts[1] instanceof RectF) {
            //SurfaceView with not 4:3 size
            bitmap = mCameraModuleScreenShotProvider.getScreenShot((Bitmap) ts[0], (RectF) ts[1]);
        } else if (ts.length >= 2 && ts[0] instanceof Bitmap && ts[1] instanceof Boolean) {
            //SurfaceView with 4：3 size or cif
            bitmap = mCameraModuleScreenShotProvider.getScreenShot((Bitmap) ts[0], (Boolean) ts[1]);
        } else {
            //Not surfaceView or filter
            bitmap = mCameraModuleScreenShotProvider.getScreenShot(DOWN_SAMPLE_RATE_FOR_SCREENSHOT);
        }
        if (ts != null && ts.length > 0 && ts[0] instanceof Bitmap) {
            ((Bitmap) ts[0]).recycle();
        }
        mModeTransitionView.showImageCover(bitmap);
        if (mFreeScreenHideRunnable == null) {
            mFreeScreenHideRunnable = new FreezeCoverRunnable();
        }
        if (mCurrentHideRunnable != null && mCurrentHideRunnable != mFreeScreenHideRunnable) {
            mAppRootView.post(mCurrentHideRunnable);
        }
        mCurrentHideRunnable = mFreeScreenHideRunnable;
        mModeCoverState = COVER_SHOWN;
    }

    /**
     * Enable or disable swipe gestures. We want to disable them e.g. while we
     * record a video.
     */
    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
        if(mModeListView != null){
            mModeListView.setTouchable(enabled && initModeListViewEnd);// SPRD:Fix bug428712
        }
        // TODO: This can be removed once we come up with a new design for
        // handling swipe
        // on shutter button and mode options. (More details: b/13751653)
        mAppRootView.setSwipeEnabled(enabled);
    }
    //Sprd:fix bug795960
    public boolean getSwipeEnable(){
        return mSwipeEnabled;
    }

    public void pause(){
        /* SPRD: Fix bug 961882 hide mode cover only for HideCoverRunnable @{ */
        if (!(mCurrentHideRunnable instanceof HideCoverRunnable)) {
            hideModeCover();
        }
        hideModeList();
        hideSettingLayout();
        mAppRootView.removeCallbacks(mshowRefocusModuleTipRunnable);
    }
    public void onDestroy() {
        SlidePanelManager.getInstance(mController).onDestroy();
        mIsDestroyed = true;
        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        if (mController.getCurrentModule().isUseSurfaceView()) {
            mTextureViewHelper.removePreviewAreaSizeChangedListener(
                    mSurfaceView.getPreviewAreaChangedListener());
        }
        /* @} */
        if (mSettingLayout != null){
            DataModuleManager.getInstance(mController.getAndroidContext()).removeListener(mSettingLayout);
        }
        mHideCoverRunnable = null;
        mFreeScreenHideRunnable = null;
        mModeSelectHideRunnable = null;
    }

    /**
     * Redirects touch events to appropriate recipient views based on swipe
     * direction. More specifically, swipe up and swipe down will be handled by
     * the view that handles mode transition; swipe left will be send to
     * filmstrip; swipe right will be redirected to mode list in order to bring
     * up mode list.
     */
    private void onSwipeDetected(int swipeState) {
        Log.d(TAG, " onSwipeDetected swipeState=" + swipeState);
        if (swipeState == SWIPE_UP || swipeState == SWIPE_DOWN) {
            // TODO: Polish quick switch after this release.
            // Quick switch between modes.
            int currentModuleIndex = mController.getCurrentModuleIndex();
            final int moduleToTransitionTo = mController
                    .getQuickSwitchToModuleId(currentModuleIndex);
            if (currentModuleIndex != moduleToTransitionTo) {
                mAppRootView.redirectTouchEventsTo(mModeTransitionView);
                int shadeColorId = R.color.camera_gray_background;
                int iconRes = CameraUtil.mModuleInfoResovle.getModuleCoverIcon(moduleToTransitionTo);
                /*AnimationFinishedListener listener = new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished(boolean success) {
                        if (success) {
                            mCurrentHideRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    mModeTransitionView.startPeepHoleAnimation();
                                }
                            };
                            mModeCoverState = COVER_SHOWN;
                            // Go to new module when the previous operation is
                            // successful.
                            mController.onModeSelected(moduleToTransitionTo);
                        }
                    }
                };*/
            }
        } else if (swipeState == SWIPE_LEFT) {
            // Pass the touch sequence to filmstrip layout.
            // Dream Camera: FilmStripView is not show by this way.
            // mAppRootView.redirectTouchEventsTo(mFilmstripLayout);
            // SPRD Bug:528553 icons disappear.
            // hidePanels();
            // SPRD: Fix 474843 Add for Filter Feature.
            if(mModeListView != null && mModeListView.isShown()){
                return;
            }
            showSettingsUI(swipeState);
            //mDreamFilterModuleController.switchMode(swipeState);
        } else if (swipeState == SWIPE_RIGHT) {
            // Pass the touch to mode switcher
            // mModeListView.getBackground().setAlpha(100);
            /* SPRD: Fix bug 474843 Add for Filter Feature @{
             * orginal code
            mModeListView.setVisibility(View.VISIBLE);
            mAppRootView.redirectTouchEventsTo(mModeListView);
             */
            //!showSettingsUI(swipeState)
            if (!showSettingsUI(swipeState) && (mSettingLayout == null
                    || !mSettingLayout.isShown()) && mModeListView != null) {
                mModeListView.setVisibility(View.VISIBLE);
                mAppRootView.redirectTouchEventsTo(mModeListView);
            }
            /* @} */
        }
    }

    private boolean showSettingsUI(int swipeState) {
        int panoramaIndex = SettingsScopeNamespaces.DREAM_PANORAMA;
        int qrIndex = SettingsScopeNamespaces.QR_CODE;
        int captureIntentIndex = SettingsScopeNamespaces.INTENTCAPTURE;
        int videoIntentIndex = SettingsScopeNamespaces.INTENTVIDEO;
        int rangeFindIndex = SettingsScopeNamespaces.TDRANGFINDEnable;

        if (swipeState == SWIPE_LEFT && mController != null
                && mController.getCurrentModuleIndex() != panoramaIndex
                && mController.getCurrentModuleIndex() != qrIndex
                && mController.getCurrentModuleIndex() != captureIntentIndex
                && mController.getCurrentModuleIndex() != videoIntentIndex
                && mController.getCurrentModuleIndex() != rangeFindIndex) {
            // update UI
            initSettingLayout((SettingUIListener)getDreamInterface());
            mSettingLayout.changeVisibilty(View.VISIBLE);
            mAppRootView.redirectTouchEventsTo(mSettingLayout);
            /*//SPRD: Fix bug 944410 //SPRD: Fix bug 1034472 cancel AE/AF lock mutex to FD
            mAppRootView.post(new Runnable() {
                public void run() {
                    mSettingLayout.updateLayoutByAeLock(mController.isUpdateSmileItemByAeLock());
                }
            });*/
            return true;
        }
        return false;
    }

    /**
     * Gets called when activity resumes in preview.
     */
    public void resume() {
        // Show mode theme cover until preview is ready
        if (!mIsCaptureIntent) {
            showModeCoverUntilPreviewReady();
        }
        if(mPreviewOverlay != null){
            mPreviewOverlay.stopZoomState();
        }

        mController.rollTask(new Runnable() {
            @Override
            public void run() {
                // Hide action bar first since we are in full screen mode first, and
                // switch the system UI to lights-out mode.
                //SPRD: Fix bug705216 mFilmstripPanel is null when double click power key.

                if(mController != null && mController.getButtonManager() != null){
                    /**SPRD:Bug771170 java crash in monkey test @{*/
                    if(mController.getCurrentModule() != null){
                        mController.getCurrentModule().waitInitDataSettingCounter();
                    }
                    /** @}*/
                    mController.getButtonManager().refreshButtonState();
                }
                mIsDestroyed = false;
            }
        });

    }

    /**
     * Opens the mode list (e.g. because of the menu button being pressed) and
     * adapts the rest of the UI.
     */
    public void openModeList() {
        //Sprd :Fix bug745376
        if (mController == null || mController.getCurrentModule() == null
                || !mController.getCurrentModule().isEnableDetector())
            return;
        if(mModeListView != null){
            mModeListView.onMenuPressed();
        }
    }

    public void showAccessibilityZoomUI(float maxZoom) {
    }

    public void hideAccessibilityZoomUI() {
    }

    /**
     * A cover view showing the mode theme color and mode icon will be visible
     * on top of preview until preview is ready (i.e. camera preview is started
     * and the first frame has been received).
     */
    public void showModeCoverUntilPreviewReady() {
        if (mModeCoverState == COVER_SHOWN) {
            return;
        }
        Log.i(TAG, "showModeCoverUntilPreviewReady");
        int modeId = mController.getCurrentModuleIndex();
        int colorId = R.color.camera_gray_background;
        int iconId = CameraUtil.mModuleInfoResovle.getModuleCoverIcon(modeId);
        mModeTransitionView.setupModeCover(colorId, iconId);
        if (mHideCoverRunnable == null) {
            mHideCoverRunnable = new HideCoverRunnable();
        }
        if (mCurrentHideRunnable != null && mCurrentHideRunnable != mHideCoverRunnable) {
            mAppRootView.post(mCurrentHideRunnable);
        }
        mCurrentHideRunnable = mHideCoverRunnable;
        mModeCoverState = COVER_SHOWN;
    }

    private void showShimmyDelayed() {
        if (!mIsCaptureIntent) {
            // Show shimmy in SHIMMY_DELAY_MS
            mModeListView.showModeSwitcherHint();
        }
    }

    /*
     * SPRD: Fix bug 506469 Hide Mode Cover when entry FilmStripView @{
     */
    public void hardHideModeCover() {
        hideModeCover();
    }

    /* @} */

    /**
     * SPRD:Fix bug 401681 private void hideModeCover() {
     */
    public void hideModeCover() {
        if (mCurrentHideRunnable != null) {
            mAppRootView.post(mCurrentHideRunnable);
            mCurrentHideRunnable = null;
        }
        mModeCoverState = COVER_HIDDEN;
        if (mCoverHiddenTime < 0) {
            mCoverHiddenTime = System.currentTimeMillis();
        }
        /*SPRD:fix bug629900 add switch animation @{*/
        if (CameraUtil.isSwitchAnimationEnable()) {
            mAppRootView.postDelayed(new Runnable() {
                public void run() {
                    hideSwitchPreview();
                }
            }, 0);
        }
    }

    /* SPRD :fix bug979853 set back cover when switch to AR @{ */
    public void setSwitchBackGroundVisibility(int visibility) {
        if (mSwitchBackGround != null) {
            mSwitchBackGround.setVisibility(visibility);
        }
    }
    /* @} */

    private void hideSwitchPreview() {
        mStartSwitchAnimation = false;
        if (mSwitchPreview.getAnimation() != null) {
            mSwitchPreview.clearAnimation();
        }
        mSwitchPreview.setVisibility(View.GONE);
        mSwitchBackGround.setVisibility(View.GONE);
        updateSwitchPreviewPara();
        if (mSwitchBitmap != null && !mSwitchBitmap.isRecycled()) {
            mSwitchBitmap.recycle();
            mSwitchBitmap = null;
        }
    }
    /* @} */

    public void onPreviewVisiblityChanged(int visibility) {
        if (visibility == ModuleController.VISIBILITY_HIDDEN) {
            setIndicatorBottomBarWrapperVisible(false);
        } else {
            setIndicatorBottomBarWrapperVisible(true);
        }
    }

    /**
     * Call to stop the preview from being rendered. Sets the entire capture
     * root view to invisible which includes the preview plus focus indicator
     * and any other auxiliary views for capture modes.
     */
    public void pausePreviewRendering() {
        mCameraRootView.setVisibility(View.INVISIBLE);
    }

    /* SPRD: fix bug474690 add for pano @{ */
    public void resumeTextureViewRendering() {
        mTextureView.setVisibility(View.VISIBLE);
    }

    public void pauseTextureViewRendering() {
        mTextureView.setVisibility(View.INVISIBLE);
    }

    /* @} */

    /**
     * Call to begin rendering the preview and auxiliary views again.
     */
    public void resumePreviewRendering() {
        mCameraRootView.setVisibility(View.VISIBLE);
    }

    /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
    public void refreshTextureViewCover() {
        mTextureViewCover.setVisibility(mController.getCurrentModule().isUseSurfaceView()? View.VISIBLE:View.GONE);
    }
    /* @} */

    /**
     * Returns the transform associated with the preview view.
     * 
     * @param m
     *            the Matrix in which to copy the current transform.
     * @return The specified matrix if not null or a new Matrix instance
     *         otherwise.
     */
    public Matrix getPreviewTransform(Matrix m) {
        return mTextureView.getTransform(m);
    }

    @Override
    public void onOpenFullScreen() {
        // Do nothing.
    }

    @Override
    public void onModeListOpenProgress(float progress) {
        // When the mode list is in transition, ensure the large layers are
        // hardware accelerated.
    }

    @Override
    public void onModeListClosed() {
        // Convert hardware layers back to default layer types when animation
        // stops
        // to prevent accidental artifacting.
        if (mShutterButton != null) {
            mShutterButton.setAlpha(ShutterButton.ALPHA_WHEN_ENABLED);
        }
    }

    /**
     * Called when the back key is pressed.
     * 
     * @return Whether the UI responded to the key event.
     */
    public boolean onBackPressed() { 
        if (mSettingLayout != null && mSettingLayout.getVisibility() == View.VISIBLE) {
            return mSettingLayout.onBackPressed();
        } else if (mModeListView != null){
            return mModeListView.onBackPressed();
        }
        return false;
    }

    /**
     * Sets a {@link com.android.camera.ui.PreviewStatusListener} that listens
     * to SurfaceTexture changes. In addition, listeners are set on dependent
     * app ui elements.
     * 
     * @param previewStatusListener
     *            the listener that gets notified when SurfaceTexture changes
     */
    public void setPreviewStatusListener(
            PreviewStatusListener previewStatusListener) {
        mPreviewStatusListener = previewStatusListener;
        if (mPreviewStatusListener != null) {
            onPreviewListenerChanged();
        }
    }

    /**
     * When the PreviewStatusListener changes, listeners need to be set on the
     * following app ui elements: {@link com.android.camera.ui.PreviewOverlay},
     * {@link com.android.camera.ui.BottomBar},
     * {@link com.android.camera.ui.IndicatorIconController}.
     */
    private void onPreviewListenerChanged() {
        // Set a listener for recognizing preview gestures.
        GestureDetector.OnGestureListener gestureListener = mPreviewStatusListener
                .getGestureListener();
        if (gestureListener != null) {
            mPreviewOverlay.setGestureListener(gestureListener);
        }

        View.OnTouchListener touchListener = mPreviewStatusListener
                .getTouchListener();
        if (touchListener != null) {
            mPreviewOverlay.setTouchListener(touchListener);
        }

        mTextureViewHelper.setAutoAdjustTransform(mPreviewStatusListener
                .shouldAutoAdjustTransformMatrixOnLayout());
    }

    public void setSettingLayoutBackground(boolean bool) {
        if (bool) {
            mSettingLayoutBackground.setVisibility(View.VISIBLE);
        } else {
            mSettingLayoutBackground.setVisibility(View.GONE);

        }
    }
    /**
     * This method should be called in onCameraOpened. It defines CameraAppUI
     * specific changes that depend on the camera or camera settings.
     */
    public void onChangeCamera() {
//        ModuleController moduleController = mController
//                .getCurrentModuleController();
//        HardwareSpec hardwareSpec = moduleController.getHardwareSpec();
//        Log.d(TAG, "onChangeCamera " + mController.getCurrentModuleIndex());
//        /**
//         * The current UI requires that the flash option visibility in front-
//         * facing camera be * disabled if back facing camera supports flash *
//         * hidden if back facing camera does not support flash We save whether
//         * back facing camera supports flash because we cannot get this in front
//         * facing camera without a camera switch. If this preference is cleared,
//         * we also need to clear the camera facing setting so we default to
//         * opening the camera in back facing camera, and can save this flash
//         * support value again.
//         */
//
//        applyModuleSpecs(hardwareSpec, moduleController.getBottomBarSpec(),
//                true /* skipScopeCheck */);
        //syncModeOptionIndicators();
    }

    public void initializeBottomBarSpec() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        HardwareSpec hardwareSpec = moduleController.getHardwareSpec();
        /**
         * The current UI requires that the flash option visibility in front-
         * facing camera be * disabled if back facing camera supports flash *
         * hidden if back facing camera does not support flash We save whether
         * back facing camera supports flash because we cannot get this in front
         * facing camera without a camera switch. If this preference is cleared,
         * we also need to clear the camera facing setting so we default to
         * opening the camera in back facing camera, and can save this flash
         * support value again.
         */

        applyModuleSpecs(hardwareSpec, moduleController.getBottomBarSpec(),
                true /* skipScopeCheck */);
        //syncModeOptionIndicators();
    }

    /**
     * Adds a listener to receive callbacks when preview area changes.
     */
    public void addPreviewAreaChangedListener(
            PreviewStatusListener.PreviewAreaChangedListener listener) {
        mTextureViewHelper.addPreviewAreaSizeChangedListener(listener);
    }

    /**
     * Removes a listener that receives callbacks when preview area changes.
     */
    public void removePreviewAreaChangedListener(
            PreviewStatusListener.PreviewAreaChangedListener listener) {
        mTextureViewHelper.removePreviewAreaSizeChangedListener(listener);
    }

    /**
     * This inflates generic_module layout, which contains all the shared views
     * across modules. Then each module inflates their own views in the given
     * view group. For now, this is called every time switching from a
     * not-yet-refactored module to a refactored module. In the future, this
     * should only need to be done once per app start.
     */
    public void prepareModuleUI() {
        mModuleUI = (FrameLayout) mCameraRootView
                .findViewById(R.id.module_layout);
        mTextureView = (TextureView) mCameraRootView
                .findViewById(R.id.preview_content);
        mTextureViewHelper = new TextureViewHelper(mTextureView,
                mCaptureLayoutHelper, mController.getCameraProvider(),
                mController);
        mTextureViewHelper.setSurfaceTextureListener(this);
        mTextureViewHelper
                .setOnLayoutChangeListener(mPreviewLayoutChangeListener);
        /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
        mTextureViewCover = (View)mCameraRootView.
                findViewById(R.id.preview_content_cover);
        /* @} */
        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        mSurfaceView = (SurfaceViewEx) mCameraRootView.findViewById(R.id.preview_content_surfaceview);
        if ((null != mSurfaceView) && mController.getCurrentModule().isUseSurfaceView()) {
            mSurfaceView.setCameraAppUI(this);
            mTextureViewHelper.addPreviewAreaSizeChangedListener(mSurfaceView.getPreviewAreaChangedListener());
        }
        /* @} */

        mSettingLayoutBackground = mCameraRootView.findViewById(R.id.setting_layout_background);
        mPreviewOverlay = (PreviewOverlay) mCameraRootView
                .findViewById(R.id.preview_overlay);

        mPreviewOverlay.setOnTouchListener(new MyTouchListener());

        mCaptureOverlay = (CaptureAnimationOverlay) mCameraRootView
                .findViewById(R.id.capture_overlay);
        mTextureViewHelper.addPreviewAreaSizeChangedListener(mPreviewOverlay);
        mTextureViewHelper.addPreviewAreaSizeChangedListener(mCaptureOverlay);

        mController.getButtonManager().load(mCameraRootView);
        mController.getButtonManager().setListener(null);
        mUnderPreviewOverlay = (FrameLayout) mCameraRootView.findViewById(R.id.under_preview_drawing_chart);
        mOverPreviewOverlay = (FrameLayout) mCameraRootView.findViewById(R.id.over_preview_drawing_chart);
        mFocusRing = (FocusRing) mCameraRootView.findViewById(R.id.focus_ring);
        mChasingRing = (ChasingView) mCameraRootView.findViewById(R.id.chasing_ring);
        mTextureViewHelper
                .addAspectRatioChangedListener(new PreviewStatusListener.PreviewAspectRatioChangedListener() {
                    @Override
                    public void onPreviewAspectRatioChanged(float aspectRatio) {
                        if (mBottomBar != null) {
                            mBottomBar.requestLayout();
                        }
                        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
                        if (null != mSurfaceView) {
                            mSurfaceView.setAspectRatio(aspectRatio);
                        }
                        /* @} */
                    }
                });
    }

    /**
     * Called indirectly from each module in their initialization to get a view
     * group to inflate the module specific views in.
     * 
     * @return a view group for modules to attach views to
     */
    public FrameLayout getModuleRootView() {
        // TODO: Change it to mModuleUI when refactor is done
        return mCameraRootView;
    }

    /**
     * Remove all the module specific views.
     */
    public void clearModuleUI() {
        if (mModuleUI != null) {
            mModuleUI.removeAllViews();
        }
        removeShutterListener(mController.getCurrentModuleController());
//        mTutorialsPlaceHolderWrapper.removeAllViews();
//        mTutorialsPlaceHolderWrapper.setVisibility(View.GONE);

        setShutterButtonEnabled(true);
        mPreviewStatusListener = null;
        previewStatusListener = null;
        mPreviewOverlay.reset();

        Log.v(TAG, "mFocusRing.stopFocusAnimations()");
        mFocusRing.stopFocusAnimations();
        mChasingRing.clear();
    }

    /**
     * Gets called when preview is ready to start. It sets up one shot preview
     * callback in order to receive a callback when the preview frame is
     * available, so that the preview cover can be hidden to reveal preview. An
     * alternative for getting the timing to hide preview cover is through
     * {@link CameraAppUI#onSurfaceTextureUpdated(android.graphics.SurfaceTexture)}
     * , which is less accurate but therefore is the fallback for modules that
     * manage their own preview callbacks (as setting one preview callback will
     * override any other installed preview callbacks), or use camera2 API.
     */
    public void onPreviewReadyToStart() {
        if (mModeCoverState == COVER_SHOWN) {
            mModeCoverState = COVER_WILL_HIDE_AT_NEXT_FRAME;
            mController.setupOneShotPreviewListener();
        }
    }

    /**
     * Gets called when preview is started.
     */
    public void onPreviewStarted() {
        /**
         * SPRD:fix bug474690 Log.v(TAG, "onPreviewStarted");
         */
        Log.i(TAG, "onPreviewStarted mModeCoverState = " + mModeCoverState);
        if(mAspectRatio>1.5){
            mBottomFrame.setVisibility(View.VISIBLE);
            mTopFrame.setVisibility(View.VISIBLE);
        }
        else{
            mBottomFrame.setVisibility(View.GONE);
            mTopFrame.setVisibility(View.GONE);
        }
        // SPRD: Fix 474843 Add for Filter Feature
        mDreamFilterModuleController.resetFrameCount();
        mStartSwitchAnimation = false;
        if (mModeCoverState == COVER_SHOWN) {
            // This is a work around of the face detection failure in
            // b/20724126.
            // In particular, we need to drop the first preview frame in order
            // to
            // make face detection work and also need to hide this preview frame
            // to
            // avoid potential janks. We do this only for L, Nexus 6 and
            // Haleakala.
            if (ApiHelper.isLorLMr1() && ApiHelper.IS_NEXUS_6) {
                mModeCoverState = COVER_WILL_HIDE_AFTER_NEXT_TEXTURE_UPDATE;
            } else {
                mModeCoverState = COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE;
            }
        }
        // SPRD:Fix bug 400555 && bug391138
        if (!isBottomBarNull() && !isInIntentReview() /*&& !isInFreezeReview()*/) {
            /*SPRD:fix bug607898 fix setting ui when back from home/secure camera, last time pause camera by pressing home @{ */
            if (isSettingLayoutOpen()) {
                updatePreviewUI(View.GONE, false);//SPRD:fix bug 607898
            }
            /* @ */
        }
    }

    /**
     * Gets notified when next preview frame comes in.
     */
    public void onNewPreviewFrame() {
        Log.v(TAG, "onNewPreviewFrame");
        CameraPerformanceTracker
                .onEvent(CameraPerformanceTracker.FIRST_PREVIEW_FRAME);
        hideModeCover();
    }

    @Override
    public void onShutterButtonClick() {
        /*
         * Set the mode options toggle unclickable, generally throughout the
         * app, whenever the shutter button is clicked. This could be done in
         * the OnShutterButtonListener of the ModeOptionsOverlay, but since it
         * is very important that we can clearly see when the toggle becomes
         * clickable again, keep all of that logic at this level.
         */
        // disableModeOptions();
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
        // Do nothing.
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // noop
    }

    @Override
    public void onShutterButtonLongPressed() {
        // noop
    }

    /**
     * SPRD: Add for bug 529369 stop Video recording before switch camera
     * 
     * @{
     */
    //public void disableCameraToggleButton() {
    //    mController.getButtonManager().disableButtonClick(
    //            ButtonManager.BUTTON_CAMERA);
    //}

    //public void enableCameraToggleButton() {
    //    if (!mDisableAllUserInteractions) {
    //        mController.getButtonManager().enableButtonClick(
    //                ButtonManager.BUTTON_CAMERA);
    //    }
    //}

    /* @} */

    public void setDisableAllUserInteractions(boolean disable) {
        if (disable) {
            setShutterButtonEnabled(false);
            setSwipeEnabled(false);
            mModeListView.hideAnimated();
        } else {
            setShutterButtonEnabled(true);
            setSwipeEnabled(true);
        }
        mDisableAllUserInteractions = disable;
    }

    @Override
    public void onModeButtonPressed(int modeIndex) {
        // TODO: Make CameraActivity listen to ModeListView's events.
        int pressedModuleId = mController.getModuleId(modeIndex);
        int currentModuleId = mController.getCurrentModuleIndex();
        if (pressedModuleId != currentModuleId) {
            hideCaptureIndicator();
        }
    }

    /**
     * Gets called when a mode is selected from
     * {@link com.android.camera.ui.ModeListView}
     *
     * @param modeIndex
     *            mode index of the selected mode
     */
    @Override
    public void onModeSelected(int modeIndex) {
        waitToHide = true;
        if (mModeSelectHideRunnable == null) {
            mModeSelectHideRunnable = new ModeSelectCoverRunnable();
        }
        if (mCurrentHideRunnable != null && mCurrentHideRunnable != mModeSelectHideRunnable) {
            mAppRootView.post(mCurrentHideRunnable);
        }
        mCurrentHideRunnable = mModeSelectHideRunnable;

        // SPRD: Fix 949200
        if (mShutterButton == null && mCameraRootView != null) {
            mShutterButton = (ShutterButton) mCameraRootView
                    .findViewById(R.id.shutter_button);
        }

        if (mShutterButton != null)
            mShutterButton.setAlpha(ShutterButton.ALPHA_WHEN_ENABLED);
        mModeCoverState = COVER_SHOWN;

        /**
         * SPRD:fix bug687045 should not hidecover before onModeSelect
        int lastIndex = mController.getCurrentModuleIndex();
        // Actual mode teardown / new mode initialization happens here
        mController.onModeSelected(modeIndex);
        int currentIndex = mController.getCurrentModuleIndex();

        if (lastIndex == currentIndex) {
            hideModeCover();
        }
        */
        mController.onModeSelected(modeIndex);

        // updateModeSpecificUIColors();
    }

    private void updateModeSpecificUIColors() {
        setBottomBarColorsForModeIndex(mController.getCurrentModuleIndex());
    }

    @Override
    public void onSettingsSelected() {
        mModeListView.setShouldShowSettingsCling(false);
    }

    @Override
    public int getCurrentModeIndex() {
        return mController.getCurrentModuleIndex();
    }

    /********************** Capture animation **********************/
    /*
     * TODO: This session is subject to UX changes. In addition to the generic
     * flash animation and post capture animation, consider designating a
     * parameter for specifying the type of animation, as well as an animation
     * finished listener so that modules can have more knowledge of the status
     * of the animation.
     */

    /**
     * Turns on or off the capture indicator suppression.
     */
    public void setShouldSuppressCaptureIndicator(boolean suppress) {
        mSuppressCaptureIndicator = suppress;
    }

    /**
     * Starts the capture indicator pop-out animation.
     * 
     * @param accessibilityString
     *            An accessibility String to be announced during the peek
     *            animation.
     */
    public void startCaptureIndicatorRevealAnimation(String accessibilityString) {
        if (mSuppressCaptureIndicator || mRoundedThumbnailView == null) {
            return;
        }
        mRoundedThumbnailView
                .startRevealThumbnailAnimation(accessibilityString);
    }

    /**
     * Updates the thumbnail image in the capture indicator.
     * 
     * @param thumbnailBitmap
     *            The thumbnail image to be shown.
     */
    public void updateCaptureIndicatorThumbnail(Bitmap thumbnailBitmap,
            int rotation) {
        /* SPRD:fix bug 694505 thumbnail should not use after recycle @{*/
        if (mSuppressCaptureIndicator || mRoundedThumbnailView == null || thumbnailBitmap == null
                || thumbnailBitmap.isRecycled()) {
            return;
        }
        /* @} */
        mRoundedThumbnailView.setThumbnail(thumbnailBitmap, rotation);
    }

    /**
     * Hides the capture indicator.
     */
    public void hideCaptureIndicator() {
        // mRoundedThumbnailView.hideThumbnail();
    }

    /**
     * Starts the flash animation.
     */
    public void startFlashAnimation(boolean shortFlash) {
        mCaptureOverlay.startFlashAnimation(shortFlash);
    }

    public void cancelFlashAnimation() {
        mCaptureOverlay.cancelFlashAnimation();
    }

    /**
     * Cancels the pre-capture animation.
     */
    public void cancelPreCaptureAnimation() {
        mAnimationManager.cancelAnimations();
    }

    /**
     * Cancels the post-capture animation.
     */
    public void cancelPostCaptureAnimation() {
        mAnimationManager.cancelAnimations();
    }

    public FilmstripContentPanel getFilmstripContentPanel() {
        return mFilmstripPanel;
    }

    /*************************** SurfaceTexture Api and Listener *********************************/

    /**
     * Return the shared surface texture.
     */
    public SurfaceTexture getSurfaceTexture() {
        return mSurface;
    }

    /**
     * Return the shared {@link android.graphics.SurfaceTexture}'s width.
     */
    public int getSurfaceWidth() {
        return mSurfaceWidth;
    }

    /**
     * Return the shared {@link android.graphics.SurfaceTexture}'s height.
     */
    public int getSurfaceHeight() {
        return mSurfaceHeight;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
            int height) {
        mSurface = surface;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        Log.i(TAG, "SurfaceTexture is available");
        if (mPreviewStatusListener != null) {
            mPreviewStatusListener.onSurfaceTextureAvailable(surface, width,
                    height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
            int height) {
        mSurface = surface;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        if (mPreviewStatusListener != null) {
            mPreviewStatusListener.onSurfaceTextureSizeChanged(surface, width,
                    height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        Log.v(TAG, "SurfaceTexture is destroyed");
        if (mPreviewStatusListener != null) {
            return mPreviewStatusListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    public void onSurfaceTextureUpdated() {
        Log.i(TAG, "onSurfaceTextureUpdated() mModeCoverState = "
                + mModeCoverState);
        if (mModeCoverState == COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE) {
            Log.i(TAG, "hiding cover via onSurfaceTextureUpdated");
            onNewPreviewFrame();
            if (!isInIntentReview()
                    && !isInFreezeReview()
                    && mController != null
                    && mController.getCurrentModule() != null) {// SPRD:Fix bug 414865
                setSwipeEnabled(true);
            }
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        mSurface = surface;

        // Do not show the first preview frame. Due to the bug b/20724126, we
        // need to have
        // a WAR to request a preview frame followed by 5-frame ZSL burst before
        // the repeating
        // preview and ZSL streams. Need to hide the first preview frame since
        // it is janky.
        // We do this only for L, Nexus 6 and Haleakala.
        if (mModeCoverState == COVER_WILL_HIDE_AFTER_NEXT_TEXTURE_UPDATE) {
            mModeCoverState = COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE;
        } else if (mModeCoverState == COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE) {
            Log.v(TAG, "hiding cover via onSurfaceTextureUpdated 1");
            CameraPerformanceTracker
                    .onEvent(CameraPerformanceTracker.FIRST_PREVIEW_FRAME);
            /*
             * SPRD: Fix bug 574388 the preview screen will be black in filter to switch
             * Original Code @{
             *
            hideModeCover();
             */
        }
        // SPRD: Fix 474843 Add for Filter Feature
        mDreamFilterModuleController.checkFrameCount();

        if (mPreviewStatusListener != null) {
            mPreviewStatusListener.onSurfaceTextureUpdated(surface);
        }
    }

    /**************************** Grid lines api ******************************/

    /**
     * Show a set of evenly spaced lines over the preview. The number of lines
     * horizontally and vertically is determined by
     * {@link com.android.camera.ui.GridLines}.
     */
    public void showGridLines() {
        if (mGridLines != null && !mGridLines.isShown()) {
            mGridLines.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide the set of evenly spaced grid lines overlaying the preview.
     */
    public void hideGridLines() {
        if (mGridLines != null && mGridLines.isShown()) {
            mGridLines.setVisibility(View.INVISIBLE);
        }
    }

    /**************************** Bottom bar api ******************************/

    /**
     * Sets up the bottom bar and mode options with the correct shutter button
     * and visibility based on the current module.
     */
    public void resetBottomControls(ModuleController module, int moduleIndex) {
        if (areBottomControlsUsed(module)) {
            setBottomBarShutterIcon(moduleIndex);
            mCaptureLayoutHelper.setShowBottomBar(true);
        } else {
            mCaptureLayoutHelper.setShowBottomBar(false);
        }
    }

    /**
     * Show or hide the mode options and bottom bar, based on whether the
     * current module is using the bottom bar. Returns whether the mode options
     * and bottom bar are used.
     */
    private boolean areBottomControlsUsed(ModuleController module) {
        if (module.isUsingBottomBar()) {
            showBottomBar();
            return true;
        } else {
            hideBottomBar();
            return false;
        }
    }

    /**
     * Set the bottom bar visible.
     */
    public void showBottomBar() {
        mBottomBar.setVisibility(View.VISIBLE);
    }

    /**
     * Set the bottom bar invisible.
     */
    public void hideBottomBar() {
        mBottomBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Sets the color of the bottom bar.
     */
    public void setBottomBarColor(int colorId) {
        mBottomBar.setBackgroundColor(colorId);
    }

    /**
     * Sets the pressed color of the bottom bar for a camera mode index.
     */
    public void setBottomBarColorsForModeIndex(int index) {
        mBottomBar.setColorsForModeIndex(index);
    }

    /* SPRD: fix bug474672 add for ucam beauty @{ */
    private int mModuleIndex = -1;

    public int getModuleIndex() {
        return mModuleIndex;
    }

    /* @} */

    /**
     * Sets the shutter button icon on the bottom bar, based on the mode index.
     */
    public void setBottomBarShutterIcon(int modeIndex) {
        mModuleIndex = modeIndex;// SPRD:fix bug 474672
        int shutterIconId = CameraUtil.mModuleInfoResovle.getModuleCaptureIcon(modeIndex);
        mBottomBar.setShutterButtonIcon(shutterIconId);
    }

    public void animateBottomBarToVideoStop(int shutterIconId) {
        mBottomBar.animateToVideoStop(shutterIconId);
    }

    public void animateBottomBarToFullSize(int shutterIconId) {
        mBottomBar.animateToFullSize(shutterIconId);
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        if (!mDisableAllUserInteractions) {
            if(mBottomBar == null){
                return;
            }
            mBottomBar.setShutterButtonEnabled(enabled);

        }
    }

    /* SPRD: fix bug978545 show toast hint for fast capture @{ */
    private boolean mThumbUnClick = false;
    public void setThumbButtonUnClickable(boolean clickable) {
        mThumbUnClick = clickable;
    }
    /* @} */

    public void setThumbButtonClick(boolean clickable) {
        if (mRoundedThumbnailView != null)
            mRoundedThumbnailView.setClickable(clickable);
    }

    public void setSwitchButtonClick(boolean clickable) {
        if (switchModeBtn != null)
            switchModeBtn.setClickable(clickable);
    }

    public void setShutterButtonClick(boolean clickable) {
        if (mShutterButton != null)
            mShutterButton.setClickable(clickable);
    }

    public void setShutterButtonImportantToA11y(boolean important) {
        if (mBottomBar != null) {
            mBottomBar.setShutterButtonImportantToA11y(important);
        }
    }

    public boolean isShutterButtonEnabled() {
        if (mBottomBar == null) {
            return false;
        }
        return mBottomBar.isShutterButtonEnabled();
    }

    public void setIndicatorBottomBarWrapperVisible(boolean visible) {
        /*
         * SPRD Bug:519334 Refactor Rotation UI of Camera. @{ Original Android
         * code: mStickyBottomCaptureLayout.setVisibility(visible ? View.VISIBLE
         * : View.INVISIBLE);
         */
        if ( mStickyBottomCaptureLayout != null) {
            mStickyBottomCaptureLayout
                    .setVisibility(visible && showPanel ? View.VISIBLE
                            : View.INVISIBLE);
        }
    }

    /**
     * Set the visibility of the bottom bar.
     */
    // TODO: needed for when panorama is managed by the generic module ui.
    public void setBottomBarVisible(boolean visible) {
        mBottomBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Add a {@link #ShutterButton.OnShutterButtonListener} to the shutter button.
     */
    public void addShutterListener(
            ShutterButton.OnShutterButtonListener listener) {
        mShutterButton.addOnShutterButtonListener(listener);
    }

    /**
     * Remove a {@link #ShutterButton.OnShutterButtonListener} from the shutter button.
     */
    public void removeShutterListener(
            ShutterButton.OnShutterButtonListener listener) {
        if(null != mShutterButton) {
            mShutterButton.removeOnShutterButtonListener(listener);
        }
    }

    /**
     * Sets or replaces the "cancel shutter" button listener.
     * <p>
     * TODO: Make this part of the interface the same way shutter button
     * listeners are.
     */
    public void setCancelShutterButtonListener(View.OnClickListener listener) {
        mCountdownCancelButton.setOnClickListener(listener);
    }

    /**
     * Performs a transition to the capture layout of the bottom bar.
     */
    public void transitionToCapture() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(),
                moduleController.getBottomBarSpec());
        mBottomBar.transitionToCapture();
    }

    /**
     * Displays the Cancel button instead of the capture button.
     */
    public void transitionToCancel() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(),
                moduleController.getBottomBarSpec());
        mBottomBar.transitionToCancel();
    }

    /**
     * Performs a transition to the global intent layout.
     */
    public void transitionToIntentCaptureLayout() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(),
                moduleController.getBottomBarSpec());
        mBottomBar.transitionToIntentCaptureLayout();
        showPreviewTopExtend();
    }

    /**
     * Performs a transition to the global intent review layout.
     */
    public void transitionToIntentReviewLayout() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(),
                moduleController.getBottomBarSpec());
        mBottomBar.transitionToIntentReviewLayout();

        // Hide the preview snapshot since the screen is frozen when users tap
        // shutter button in capture intent.
        hideModeCover();
        hidePreviewTopExtend();
    }

    /**
     * @return whether UI is in intent review mode
     */
    public boolean isInIntentReview() {
        if(mBottomBar == null)
            return false;

        return mBottomBar.isInIntentReview();
    }

    /**
     * Applies a {@link com.android.camera.CameraAppUI.BottomBarUISpec} to the
     * bottom bar mode options based on limitations from a
     * {@link com.android.camera.hardware.HardwareSpec}. Options not supported
     * by the hardware are either hidden or disabled, depending on the option.
     * Otherwise, the option is fully enabled and clickable.
     */
    public void applyModuleSpecs(HardwareSpec hardwareSpec,
            BottomBarUISpec bottomBarSpec) {
        applyModuleSpecs(hardwareSpec, bottomBarSpec, false /* skipScopeCheck */);
    }

    private void applyModuleSpecs(final HardwareSpec hardwareSpec,
            final BottomBarUISpec bottomBarSpec, boolean skipScopeCheck) {
        if (hardwareSpec == null || bottomBarSpec == null) {
            return;
        }

        ButtonManager buttonManager = mController.getButtonManager();

        /** Intent UI */
        if (bottomBarSpec.showCancel) {
            buttonManager.initializePushButton(ButtonManager.BUTTON_CANCEL,
                    bottomBarSpec.cancelCallback);
        }
        if (bottomBarSpec.showDone) {
            buttonManager.initializePushButton(ButtonManager.BUTTON_DONE,
                   bottomBarSpec.doneCallback);
        }
        if (bottomBarSpec.showRetake) {
            buttonManager.initializePushButton(ButtonManager.BUTTON_RETAKE,
                    bottomBarSpec.retakeCallback, R.drawable.ic_back,
                    R.string.retake_button_description);
        }
        if (bottomBarSpec.showReview) {
            buttonManager.initializePushButton(ButtonManager.BUTTON_REVIEW,
                    bottomBarSpec.reviewCallback, R.drawable.ic_play,
                    R.string.review_button_description);
        }
    }

    /**
     * Returns a {@link com.android.camera.ButtonManager.ButtonCallback} that
     * will disable the button identified by the parameter.
     * 
     * @param conflictingButton
     *            The button id to be disabled.
     */
    private ButtonManager.ButtonCallback getDisableButtonCallback(
            final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mController.getButtonManager().disableButton(conflictingButton);
            }
        };
    }

    /*
     * SPRD: Bug 502464: swiping left when switching camera that will show the
     * freeze screen and return button in Actionbar @{
     */
    private ButtonManager.ButtonCallback getDisableCameraButtonCallback(
            final int conflictingButton) {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                mController.getButtonManager().disableButton(conflictingButton);
                setSwipeEnabled(false);
                // //Sprd: Add for bug 529369 stop Video recording before switch
                // camera
                setShutterButtonEnabled(false);
            }
        };
    }

    /* @} */

    private String getResourceString(int stringId) {
        try {
            return mController.getAndroidContext().getResources()
                    .getString(stringId);
        } catch (Resources.NotFoundException e) {
            // String not found, returning empty string.
            return "";
        }
    }

    /**
     * Shows the given tutorial on the screen.
     */
//    public void showTutorial(AbstractTutorialOverlay tutorial,
//            LayoutInflater inflater) {
//        tutorial.show(mTutorialsPlaceHolderWrapper, inflater);
//    }

    /**
     * Whether the capture ratio selector dialog must be shown on this device.
     */
    public boolean shouldShowAspectRatioDialog() {
        final boolean isAspectRatioDevice = ApiHelper.IS_NEXUS_4
                || ApiHelper.IS_NEXUS_5 || ApiHelper.IS_NEXUS_6;
        return isAspectRatioDevice;
    }

    /*************************** Filmstrip api *****************************/

    public int getFilmstripVisibility() {
        return mFilmstripLayout.getVisibility();
    }

    /************************** Add API for FreezeDisplay ***********************************/

    public void transitionToFreezeReivewLayout() {
        ModuleController moduleController = mController
                .getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(),
                moduleController.getBottomBarSpec());
        mBottomBar.transitionToFreezeReivewLayout();
    }

    public boolean isInFreezeReview() {
        if (mBottomBar == null) {
            return false;
        }
        return mBottomBar.isInFreezeReview();
    }

    /* SPRD: add for bug 380597: switch camera preview has a frame error @{ */
    public void resetPreview() {
        mTextureViewHelper.resetPreview();
    }

    /* @} */
    /* SPRD:Add for bug 507813 The flash icon is not sync @{ */
    public void resetIndicatorIcon() {
        mController.getButtonManager().setListener(null);
    }
    // SPRD Bug:515425 Runnable has been redefined after unlock.
    private boolean waitToHide = false;

    public void stop() {
        if (waitToHide && mAppRootView != null) {
            hideModeCover();
        }
    }

    /*
     * SPRD:fix bug518054 ModeListView is appear when begin to capture using
     * volume key
     */
    public void hideModeList() {
        if (mModeListView != null && mModeListView.isShown()) {
            mModeListView.onBackPressed();
        }
    }

    public void hideSettingLayout() {
        if (mSettingLayout != null && mSettingLayout.isShown()) {
            mSettingLayout.hideSettingLayout();
        }
    }

    /* SPRD:fix bug521990 Back to Camera from lockscreen, the screen is freeze */
    public void hideImageCover() {
        mModeTransitionView.hideImageCover();
    }
    /*
     * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
     */
    private int nowOrientation;

    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
            OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();

        DreamOrientation.setOrientation(mCameraRootView, orientation, true);// ui check 122
        nowOrientation = orientation;

        // SPRD Bug WideAngle Rotate
        if (mDreamUI != null && mDreamUI.getUITpye() == DreamUI.DREAM_WIDEANGLEPANORAMA_UI) {
            mDreamUI.adjustUI(orientation);
        }
    }

    public static void setOrientation(View view, int orientation, boolean animation) {
        if (view == null) {
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(orientation, animation);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                setOrientation(group.getChildAt(i), orientation, animation);
            }
        }
    }

    public int getNowOrientation() {
        return nowOrientation;
    }

    private boolean showPanel = true;

    /* SPRD: Fix bug 558883 @{ */
    private PanelsVisibilityListener mPanelsVisibilityListener;

    public interface PanelsVisibilityListener {
        void onPanelsHidden();
        void onPanelsShown();
    }

    public void setPanelsVisibilityListener(PanelsVisibilityListener panelsVisibilityListener) {
        mPanelsVisibilityListener = panelsVisibilityListener;
    }
    /* @} */

    public void showPanels() {
        if ((mDreamUI != null && (mDreamUI.isFreezeFrameShow() || mDreamUI.isReviewImageShow()))
                || isInIntentReview())
            return;

        setBottomPanelLeftRightClickable(true);
        if (mStickyBottomCaptureLayout != null) {
            mStickyBottomCaptureLayout.setVisibility(View.VISIBLE);
            showPanel = true;
            if (mDreamUI != null) {
                mDreamUI.showPanels();
            }
            /* SPRD: Fix bug 558883 @{ */
            if (mPanelsVisibilityListener != null) {
                mPanelsVisibilityListener.onPanelsShown();
            }
            /* @} */
        }
        showBlurPanel();
        onCloseFilmStrip();
    }

    public void hidePanels() {
        if (mStickyBottomCaptureLayout != null) {
            mStickyBottomCaptureLayout.setVisibility(View.INVISIBLE);
            showPanel = false;
            if (mDreamUI != null) {
                mDreamUI.hidePanels();
            }
            /* SPRD: Fix bug 558883 @{ */
            if (mPanelsVisibilityListener != null) {
                mPanelsVisibilityListener.onPanelsHidden();
            }
            /* @} */
        }
        hideBlurAndAdjustPanel();
        onOpenFilmStrip();
    }

    public boolean getPanel() {
        return showPanel;
    }

    public void setPanel(boolean show) {
        showPanel = show;
    }
    /* @} */

    /**
     * SPRD: fix bug474690 add for pano
     */
    public void setLayoutAspectRation(float aspectRation) {
        mTextureViewHelper.setAspectRation(aspectRation);
        mCaptureLayoutHelper.setAspectRation(aspectRation);
    }

    public FrameLayout getModuleView() {
        return mModuleUI;
    }

    public int mBottomHeightMax = 0;

    public int getBottomHeight() {
        WindowManager wm = (WindowManager) mController.getAndroidContext()
                .getSystemService(Context.WINDOW_SERVICE);
        int windowHeight = wm.getDefaultDisplay().getHeight();
        int previewRectBottom = (int) mCaptureLayoutHelper
                .getUncoveredPreviewRect().bottom;
        int bottomHeight = windowHeight - previewRectBottom;
        Resources res = mController.getAndroidContext().getResources();
        int maxHeight = res
                .getDimensionPixelSize(R.dimen.bottom_bar_height_max);
        int optimalHeight = res
                .getDimensionPixelSize(R.dimen.bottom_bar_height_optimal);
        if (bottomHeight == optimalHeight || bottomHeight == windowHeight) {
            return mBottomHeightOptimal;
        } else {
            return mBottomHeightMax;
        }
    }

    /*
     * SPRD Bug:529008 Animate just once. @{
     */
    public void animateBottomBarToVideoStop(int fromId, int toId) {
        mBottomBar.animateToVideoStop(fromId, toId);
    }

    public void animateBottomBarToFullSize(int fromId, int toId) {
        mBottomBar.animateToFullSize(fromId, toId);
    }

    /* @} */

    // DreamInterface to control new ui.
    private DreamInterface mDreamInterface;

    public void setDreamInterface(DreamInterface dreamInterface) {
        mDreamInterface = dreamInterface;

        if (mDreamInterface != null && mDreamInterface instanceof DreamUI)
            setDreamUI((DreamUI) mDreamInterface);
    }

    public DreamInterface getDreamInterface() {
        return mDreamInterface;
    }

    // For Mode List
    private View topPanel;
    private View extendPanel;
    private View bottomPanel;
    private View slidePanel;
    private View bottomPanelLeft;
    private View bottomPanelRight;
    private View bottomPanelCenter;

    private RotateImageView mUltraWideAngleSwitchView;
    private RotateImageView mAiSceneIconView;

    public void initAiSceneView() {
        mAiSceneIconView = mCameraRootView.findViewById(R.id.ai_scene_icon_view);
        mAiSceneIconView.setVisibility(View.INVISIBLE);
        if(CameraUtil.isNavigationEnable()){
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mAiSceneIconView.getLayoutParams();
            layoutParams.setMargins(0,0,0,mController.getResources()
                    .getDimensionPixelSize(R.dimen.seekbar_navigation_height));
            mAiSceneIconView.setLayoutParams(layoutParams);
        }
        mAiSceneIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                //SPRD:fix bug1373034
                if(mController == null || mController.getCurrentModuleIndex() == SettingsScopeNamespaces.PORTRAIT_PHOTO
                        ||mController.getCurrentModuleIndex() == SettingsScopeNamespaces.TDNR_PHOTO){
                    return;
                }

                // Bug 958836 , visibility == INVISIBLE while onClick
                if(mAiSceneIconView.getVisibility() != View.VISIBLE) {
                    Log.i(TAG , "visibility == INVISIBLE while onClick");
                    return;
                }

                int tipsID = mDreamInterface.getAiSceneTip();
                Toast toast = Toast.makeText(mController , tipsID , Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT , 0 , 0);
                toast.show();
            }
        });
    }

    // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
    public void initUltraWideAngleSwitchView(boolean enter) {
        Log.i(TAG , "init UltraWideAngleSwitchView , " + enter);
        mUltraWideAngleSwitchView = mCameraRootView.findViewById(R.id.switch_ultra_wide_angle_module);

        if(enter)
            mUltraWideAngleSwitchView.setBackgroundResource(R.drawable.ic_ultra_wide_angle_mode_sprd_selected);
        else
            mUltraWideAngleSwitchView.setBackgroundResource(R.drawable.ic_ultra_wide_angle_mode_sprd_unselected);

        mUltraWideAngleSwitchView.setVisibility(View.VISIBLE);

        mUltraWideAngleSwitchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                Log.i(TAG , "click UltraWideAngle , leave = " + (!enter));
                mController.switchUltraWideAngleModule(!enter);

                DataModuleManager
                        .getInstance(mController.getAndroidContext())
                        .getCurrentDataModule()
                        .set(Keys.KEY_ULTRA_WIDE_ANGLE, !enter); // only modify preference value
            }
        });
    }

    public void deinitUltraWideAngleSwitchView() {
        Log.i(TAG , "deinit UltraWideAngleSwitchView");
        if (mUltraWideAngleSwitchView != null) {
            mUltraWideAngleSwitchView.setVisibility(View.GONE);
            mUltraWideAngleSwitchView = null;
        }
    }

    public void initAllPanels() {
        if (mCameraRootView != null) {
            topPanel = mCameraRootView.findViewById(R.id.top_panel_parent);
        }
    }

    public void initSlidePanel(View slidePanelParent){
        slidePanel = slidePanelParent;
        if (!mIsCaptureIntent) {
            slidePanel.setVisibility(View.VISIBLE);
        }
    }
    public void onCloseFilmStrip() {
        if (topPanel != null)
            topPanel.setVisibility(View.VISIBLE);
        if (extendPanel != null)
            extendPanel.setVisibility(View.VISIBLE);
        if (bottomPanel != null)
            bottomPanel.setVisibility(View.VISIBLE);
        if (slidePanel != null && !mIsCaptureIntent){
            slidePanel.setVisibility(View.VISIBLE);
        }
        if (mZoomPanel != null && !mZoomPanel.isZoomSimple())
            mZoomPanel.setVisibility(View.VISIBLE);
        showBlurPanel();
    }

    public void onOpenFilmStrip() {
        if (topPanel != null)
            topPanel.setVisibility(View.INVISIBLE);
        if (extendPanel != null)
            extendPanel.setVisibility(View.INVISIBLE);
        //SPRD Bug:607389 when zoom, zoomBar display unnormal
        if (mZoomPanel != null && mPreviewOverlay != null){
            mZoomPanel.setVisibility(View.GONE);
            mPreviewOverlay.removeZoomHideMsg();
        }
        if (bottomPanel != null)
            bottomPanel.setVisibility(View.INVISIBLE);
        if (slidePanel != null)
            slidePanel.setVisibility(View.INVISIBLE);
        hideBlurAndAdjustPanel();
    }

    private boolean isModeListOpen = false;

    public boolean isModeListOpen(){
        return isModeListOpen;
    }

    public void onCloseModeList() {
        isModeListOpen = false;

        if (topPanel != null)
            topPanel.setVisibility(View.VISIBLE);
        if (extendPanel != null)
            extendPanel.setVisibility(View.VISIBLE);
        if (bottomPanel != null)
            bottomPanel.setVisibility(View.VISIBLE);
        if (mZoomPanel != null && !mZoomPanel.isZoomSimple())
            mZoomPanel.setVisibility(View.VISIBLE);

        /* SPRD: Fix bug 612207 cameraActivity instances leak leads to OOM. @{ */
        if (mDreamInterface != null && !mIsDestroyed) {
            mDreamInterface.updateSlidePanel();
        }
        /* @} */
        if (mDreamUI != null) {
            mDreamUI.onCloseModeListOrSettingLayout();
        }
        updateAiSceneView(View.VISIBLE , -1);
        showBlurPanel();
        updateUltraWideAngleView(View.VISIBLE);
    }

    public void onOpenModeList() {
        isModeListOpen = true;
        if (topPanel != null)
            topPanel.setVisibility(View.INVISIBLE);
        if (extendPanel != null)
            extendPanel.setVisibility(View.INVISIBLE);
        //SPRD Bug:607389 when zoom, zoomBar display unnormal
        if (mZoomPanel != null && mPreviewOverlay != null){
            mZoomPanel.setZoomVisible(View.GONE);
            mPreviewOverlay.removeZoomHideMsg();
        }
        if (bottomPanel != null)
            bottomPanel.setVisibility(View.INVISIBLE);
        updateAiSceneView(View.INVISIBLE , 0);
        SlidePanelManager.getInstance(mController).focusItem(
                SlidePanelManager.MODE, false);
        hideBlurAndAdjustPanel();
        updateUltraWideAngleView(View.GONE);
    }

    public void updateModeList() {
        int mode = 0;
        int camera = 0;

        // if (mDreamInterface != null) {
        mode = mController.getCurrentModule().getMode();
        // }
        if (mController != null) {
            camera = mDataModule.getIndexOfCurrentValue(Keys.KEY_CAMERA_ID);
            camera = DreamUtil.getRightCamera(camera);
        }
        if (mModeListView != null) {
            mModeListView.updateList(mode, camera);
        }
    }

    // Switch Mode Button on bottom-right.
    private ImageView switchModeBtn;
    private ImageView videoPauseBtn;
    private ImageView videoCaptureBtn;

    public void initBottomButtons() {
        switchModeBtn = (ImageView) mCameraRootView
                .findViewById(R.id.btn_mode_switch);
        videoPauseBtn = (ImageView) mCameraRootView
                .findViewById(R.id.btn_video_pause);
        videoCaptureBtn = (ImageView) mCameraRootView
                .findViewById(R.id.video_capture_button);
        bottomPanel = mCameraRootView
                .findViewById(R.id.bottom_panel_parent);
        bottomPanelLeft = mCameraRootView
                .findViewById(R.id.bottom_panel_left);
        bottomPanelRight = mCameraRootView
                .findViewById(R.id.bottom_panel_right);
        bottomPanelCenter = mCameraRootView
                .findViewById(R.id.bottom_panel_center);
        mController.getButtonManager().load(mCameraRootView);
    }

    public ImageView getPauseButton() {
        return videoPauseBtn;
    }

    public void updateSwitchModeBtn(DreamInterface dreamInterface) {
        if (switchModeBtn == null)
            switchModeBtn = (ImageView) mCameraRootView
                    .findViewById(R.id.btn_mode_switch);
        if (/* dreamInterface != null && */switchModeBtn != null) {
            switchModeBtn.setImageResource(DreamUtil
                    .getSwitchBtnResId((mController)
                            .getCurrentModule().getMode() + 1));
        }
    }

    public void onThumbnail(final Bitmap thumbmail) {
        if (mDreamUI != null) {
            mDreamUI.onThumbnail(thumbmail);
            if (extendPanel.getVisibility() != View.VISIBLE)
                extendPanel.setVisibility(View.VISIBLE);
        }
    }

    // SPRD Bug WideAngle Rotate
    private PreviewStatusListener previewStatusListener;

    public void setStatusListener(PreviewStatusListener previewStatusListener) {
        this.previewStatusListener = previewStatusListener;
    }

    /*
     * Add for dream camera for gif module
     */
    public void setShutterPartInBottomBarShow(int visible,
            boolean forceShutterShow) {
        Log.d(TAG, "bbbb setShutterPartInBottomBarShow visible=" + visible);
        if(null != mShutterButton){ // SPRD: Fix Bug 1035655
            if (forceShutterShow) {
                mShutterButton.setVisibility(View.VISIBLE);
            } else {
                mShutterButton.setVisibility(visible);
            }
        }
        if(null != mRoundedThumbnailView){
            mRoundedThumbnailView.setVisibility(View.GONE - visible);
        }
        if(null != switchModeBtn){
            switchModeBtn.setVisibility(View.GONE - visible);
        }
    }
    /**
     * Video Recording Bottom UI
     * 
     * @{
     */
    /* SPRD: Dream Camera ui check 71 @{ */
    public void hideVideoCapture() {
        videoCaptureBtn.setVisibility(View.GONE);
    }
    /* @} */

    public void changeToRecordingUI() {
        // SPRD: Fix Bug 616857 At the same time click on the record video and thumbnail buttons
        // recording video interface abnormalities
        mRoundedThumbnailView.setEnabled(false);
        videoPauseBtn.setVisibility(View.VISIBLE);
        if (mController.getCurrentModule().isSupportVideoCapture()) {
            videoCaptureBtn.setVisibility(View.VISIBLE);
            videoCaptureBtn.setEnabled(true);
        } else {
            videoCaptureBtn.setVisibility(View.GONE);
            videoCaptureBtn.setEnabled(false);
        }
    }

    public void setSlowMotionCaptureButtonDisable() {
        if (mController.getCurrentModule().isSupportVideoCapture()) {
            videoCaptureBtn.setVisibility(View.VISIBLE);
            videoCaptureBtn.setEnabled(false);
        } else {
            videoCaptureBtn.setVisibility(View.GONE);
            videoCaptureBtn.setEnabled(false);
        }
    }

    public void changeToVideoReviewUI() {
        // SPRD: Fix Bug 616857 At the same time click on the record video and thumbnail buttons
        // recording video interface abnormalities
        if(mRoundedThumbnailView != null){
            mRoundedThumbnailView.setEnabled(true);
        }

        if(videoPauseBtn != null){
            videoPauseBtn.setVisibility(View.GONE);
        }

        if(videoCaptureBtn != null){
            videoCaptureBtn.setVisibility(View.GONE);
        }
    }

    /* @} */

    /**
     * Current Module Tip UI
     * 
     * @param moduleId
     * @{
     */
    public void showCurrentModuleTip(int moduleId) {
        if (mCurrentModuleAddTip.getVisibility() == View.VISIBLE) {
            mCurrentModuleAddTip.setVisibility(View.GONE);
        }
        if (moduleId == SettingsScopeNamespaces.AUDIO_PICTURE)
            showModuleTip(Keys.KEY_CAMERA_RECORD_VOICE_HINT,R.string.camera_record_voice_tip);
        if (moduleId == SettingsScopeNamespaces.CONTINUE)
            showModuleTip(Keys.KEY_CAMERA_BURST_HINT,R.string.dream_continuephotomodule_module_warning);
        showRefocusModuleTip(moduleId);
    }

    private void showModuleTip(String key,int resourceId) {
        if(!DataModuleManager
                .getInstance(mController.getAndroidContext())
                .getCurrentDataModule()
                .getBoolean(key)) {
            mCurrentModuleAddTip.setText(resourceId);
            mCurrentModuleAddTip.setVisibility(View.VISIBLE);
            DataModuleManager
                    .getInstance(mController.getAndroidContext())
                    .getCurrentDataModule()
                    .set(key, true);
            mAppRootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mCurrentModuleAddTip != null) {
                        mCurrentModuleAddTip.setVisibility(View.GONE);
                    }
                }
            }, 4000);
        }
    }
    
    public void showRefocusModuleTip(int moduleId) {
        if (invalidModule(moduleId)) {
            return;
        }
        if ( moduleId == SettingsScopeNamespaces.REFOCUS) {
            mCurrentModuleAddTip.setText(R.string.refocus_distance_tip);
        } else {
            if (CameraUtil.getCurrentFrontBlurRefocusVersion() != CameraUtil.BLUR_REFOCUS_VERSION_7 && CameraUtil.getCurrentFrontBlurRefocusVersion() != CameraUtil.BLUR_REFOCUS_VERSION_8) {
                mCurrentModuleAddTip.setText(R.string.refocus_front_distance_tip);
            }
        }
        if(View.INVISIBLE != mCurrentModuleAddTip.getVisibility()) {
            mCurrentModuleAddTip.setVisibility(View.VISIBLE);
            if(mshowRefocusModuleTipRunnable != null) {
                mAppRootView.removeCallbacks(mshowRefocusModuleTipRunnable);
            }
            mAppRootView.postDelayed(mshowRefocusModuleTipRunnable, 4000);
        }
    }

    public void setRefocusModuleTipVisibility(int v) {
        if (null != mCurrentModuleAddTip)
            mCurrentModuleAddTip.setVisibility(v);
    }

/**
 * check if currentModule is blur or frontblur module
 * @param moduleId
 * @return true if currntModule is blur or frontblur module
 */
   private boolean invalidModule(int moduleId) {
       return (moduleId != SettingsScopeNamespaces.REFOCUS
               && moduleId != SettingsScopeNamespaces.FRONT_BLUR);
   }

    public void showCurrentModuleUI(int moduleId) {
        showCurrentModuleTip(moduleId);
        updateModeList();
    }

    /* @} */
    boolean mBursting = false;
    public void setBursting(boolean bursting){
        mBursting = bursting;
    }

    public boolean getBursting() {
        return mBursting;
    }

    /**
     * onShutterButtonClick enter UI
     * 
     * @{
     */
    public void setBottomBarLeftAndRightUI(int visible) {

        if(mBursting && visible == View.VISIBLE){
            return;
        }

        if (mRoundedThumbnailView != null) {
            mRoundedThumbnailView.setVisibility(visible);
        }
        if (switchModeBtn != null) {
            switchModeBtn.setVisibility(visible);
        }

    }

    public void setBottomBarRightUI(int visible){
        if (switchModeBtn != null) {
            switchModeBtn.setVisibility(visible);
        }
    }

    public void updatePreviewUI(int visible, boolean updateSlide) {
        if(isInIntentReview())
            return;

        updateTopPanelUI(visible);
        if(mZoomPanel != null && (mZoomPanel.getVisibility() == View.GONE || !mZoomPanel.isZoomSimple())){
	        updateExtendPanelUI(visible);
	        updateFilterPanelUI(visible);//Sprd:Fix bug782358
        }
        setBottomBarLeftAndRightUI(visible);
        updateAdjustFlashPanelUI(visible);
        if (mShutterButton != null) {
            mShutterButton.setVisibility(visible);
        }
        if(updateSlide) {
            updateSlidePanelUI(visible);
        }
        if (visible == View.VISIBLE) {
            showBlurPanel();
        } else {
            hideBlurAndAdjustPanel();
        }
        updateUltraWideAngleView(visible);
    }

    public void updateUltraWideAngleView(int visible) {
        if (mUltraWideAngleSwitchView != null)
            mUltraWideAngleSwitchView.setVisibility(visible);
    }

    public void updateTopPanelUI(int visible) {
        if(mBursting && visible == View.VISIBLE){
            return;
        }

        if (topPanel != null) {
            topPanel.setVisibility(visible);
        }
    }

    public void updateExtendPanelUI(int visible) {
        if(mController.getCurrentModule() != null
                && mController.getCurrentModule().isAudioRecording()
                && visible == View.VISIBLE)
            return;
        //Sprd:Fix bug782358
        if (mDreamUI != null
                && mDreamUI.isCountingDown()){
            return;
        }
        if (extendPanel != null) {
            extendPanel.setVisibility(visible);
        }
    }
    public void updateAdjustFlashPanelUI(int visible) {
        if(mController.getCurrentModule() != null){
            if (visible == View.VISIBLE){
                mController.getCurrentModule().updateFlashLevelUI();
            } else {
                hideAdjustFlashPanel();
            }
        }
    }

    public void updateShutterButtonUI(int visible) {
        if (mShutterButton != null) {
            mShutterButton.setVisibility(visible);
        }
    }

    public void updateSlidePanelUI(int visible) {

        if(mBursting && visible == View.VISIBLE){
            return;
        }

        if (slidePanel != null && !mIsCaptureIntent){
            slidePanel.setVisibility(visible);
        }
    }

    public void updateAiSceneView(int visible , int index) {
        if (null != mAiSceneIconView && null != mDreamInterface) {//Sprd:fix bug948058  // Bug 1159109 (FORWARD_NULL)
            mDreamInterface.updateAiSceneView(mAiSceneIconView, visible, index);
        }
    }

    /* @} */

//    private void setMakeupUI(int visible) {
//        // TODO Auto-generated method stub
//        LinearLayout mMakeupControllerView = (LinearLayout) mCameraRootView
//                .findViewById(R.id.dream_make_up_panel);
//
//        if (mMakeupControllerView == null) {
//            return;
//        }
//
//        SeekBar mMakeupSeekBar = (SeekBar) mMakeupControllerView
//                .findViewById(R.id.make_up_seekbar);
//        TextView mCurMakeupLevel = (TextView) mMakeupControllerView
//                .findViewById(R.id.current_make_up_level);
//
//        if (DataModuleManager.getInstance(mController.getAndroidContext())
//                .getDataModulePhoto()
//                .getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
//            Log.e(TAG, "updatePreviewUI KEY_CAMERA_BEAUTY_ENTERED  = TRUE");
//            if (visible > View.VISIBLE) {
//                mMakeupControllerView.setVisibility(View.GONE);
//            } else {
//                if (mMakeupSeekBar != null) {
//                    int makeupLevel = DataModuleManager
//                            .getInstance(mController.getAndroidContext())
//                            .getCurrentDataModule()
//                            .getInt(Keys.KEY_MAKEUP_MODE_LEVEL,
//                                    mMakeupSeekBar.getProgress());
//                    mMakeupSeekBar.setProgress(makeupLevel);
//                    mCurMakeupLevel.setText("" + (makeupLevel / 11 + 1));
//                }
//                mMakeupControllerView.setVisibility(View.VISIBLE);
//            }
//        }
//
//    }

    /* SPRD: Fix 474859 Add new Feature of scenery . @{ */
    public RectF getPreviewArea() {
        return mTextureViewHelper.getPreviewArea();
    }
    /* @} */

    /* SPRD: Fix 474843 Add new Feature of Filter @{ */
    public void textureViewRequestLayout() {
        mTextureView.requestLayout();
    }

    public Bitmap getPreviewOverlayAndControls() {
        return mCameraModuleScreenShotProvider.getPreviewOverlayAndControls();
    }

    /* SPRD: nj dream camera test debug 120 @{ */
    public void updateFilterPanelUI(int visible) {
        //Sprd:Fix bug782358
        if (mDreamUI != null
                && mDreamUI.isCountingDown()){
            return;
        }
        if (mDreamFilterModuleController != null) {
            mDreamFilterModuleController.updateFilterPanelUI(visible);
        }
    }
    /* @} */
    /* @} */

    public Bitmap getPreviewShotWithoutTransform() {
        return mCameraModuleScreenShotProvider
                .getPreviewFrameWithoutTransform(DOWN_SAMPLE_RATE_FOR_SCREENSHOT);
    }
    /* @} */

    /* SRPD: add more gridline type @{ */
    /**
     * Show a set of evenly spaced lines over the preview. The number of lines
     * horizontally and vertically is determined by
     * {@link com.android.camera.ui.ReticleGridLines}.
     */
    public void showReticleGridLines() {
        if (mReticleGridLines != null && !mReticleGridLines.isShown()) {
            mReticleGridLines.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Hide the set of evenly spaced grid lines overlaying the preview.
     */
    public void hideReticleGridLines() {
        if (mReticleGridLines != null && mReticleGridLines.isShown()) {
            mReticleGridLines.setVisibility(View.INVISIBLE);
        }
    }
    /**
     * Show a set of evenly spaced lines over the preview. The number of lines
     * horizontally and vertically is determined by
     * {@link com.android.camera.ui.BoxGridLines}.
     */
    public void showBoxGridLines() {
        if (mBoxGridLines != null && !mBoxGridLines.isShown()) {
            mBoxGridLines.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Hide the set of evenly spaced grid lines overlaying the preview.
     */
    public void hideBoxGridLines() {
        if (mBoxGridLines != null && mBoxGridLines.isShown()) {
            mBoxGridLines.setVisibility(View.INVISIBLE);
        }
    }
    /**
     * Show a set of evenly spaced lines over the preview.  The number
     * of lines horizontally and vertically is determined by
     * {@link com.android.camera.ui.GridLines}.
     */
    public void showNineGridLines() {
        if (mNineGridLines != null && !mNineGridLines.isShown()) {
            mNineGridLines.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide the set of evenly spaced grid lines overlaying the preview.
     */
    public void hideNineGridLines() {
        if (mNineGridLines != null && mNineGridLines.isShown()) {
            mNineGridLines.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG,"updateParametersGridLine hideGridLines");
    }
    /**
     * Show a set of evenly spaced lines over the preview. The number of lines
     * horizontally and vertically is determined by
     * {@link com.android.camera.ui.BoxGridLines}.
     */
    public void showGoldenGridLines() {
        if (mGoldenGridLines != null && !mGoldenGridLines.isShown()) {
            mGoldenGridLines.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Hide the set of evenly spaced grid lines overlaying the preview.
     */
    public void hideGoldenGridLines() {
        if (mGoldenGridLines != null && mGoldenGridLines.isShown()) {
            mGoldenGridLines.setVisibility(View.INVISIBLE);
        }
    }
    /*@}*/

    public void hideBaseGridLines(View baseGridLines){
        if (baseGridLines != null && baseGridLines.isShown()) {
            baseGridLines.setVisibility(View.INVISIBLE);
        }
    }

    public void showBaseGridLines(View baseGridLines){
        if (baseGridLines != null && !baseGridLines.isShown()) {
            baseGridLines.setVisibility(View.VISIBLE);
        }
    }

    /* Bug #533869 new feature: check UI 27,28: dream camera of intent capture @{ */
    public void hideBottomPanelLeftRight() {
        if (bottomPanelLeft != null)
        bottomPanelLeft.setVisibility(View.INVISIBLE);
        if (bottomPanelRight != null)
        bottomPanelRight.setVisibility(View.INVISIBLE);
        updateAiSceneView(View.INVISIBLE , 0);
    }

    public void hideSlide() {
        if (slidePanel != null) {
            slidePanel.setVisibility(View.INVISIBLE);
        }
    }

    public void hidePreviewTopExtend() {
        if (topPanel != null)
            topPanel.setVisibility(View.INVISIBLE);
        if (extendPanel != null)
            extendPanel.setVisibility(View.INVISIBLE);
        //SPRD Bug:607389 when zoom, zoomBar display unnormal
        if (mZoomPanel != null && mPreviewOverlay != null){
            mZoomPanel.setVisibility(View.GONE);
            mPreviewOverlay.removeZoomHideMsg();
        }
    }

    public void showPreviewTopExtend() {
        if (topPanel != null)
            topPanel.setVisibility(View.VISIBLE);
        //Sprd fix bug984043
        if (extendPanel != null)
            extendPanel.setVisibility(View.VISIBLE);
    }
    /* @} */

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public void showStopRecordVoiceButton() {
        mBottomBar.showStopRecordVoiceButton();
    }

    public void hideStopRecordVoiceButton() {
        mBottomBar.hideStopRecordVoiceButton();
    }

    //SPRD : fix bug 666446 ,dreamVoiceImageView is removed from xml file
    /*public void setVoicePlayButtonVisible(boolean visible) {
        mFilmstripLayout.setVoicePlayButtonVisible(visible);
    }*/

    //SPRD: Fix bug 666446 this imageView is not used anymore
    /*public DreamVoiceImageView getDreamVoiceImageView() {
        return (DreamVoiceImageView)mFilmstripLayout.findViewById(R.id.photo_voice_icon);
    }*/

    public void updateRecordVoiceUI(int visible) {
        if (mDreamUI != null) {
            if (visible == View.VISIBLE) {
                mDreamUI.showPanels();
            } else {
                mDreamUI.hidePanels();
            }
        }
        if (topPanel != null)
            topPanel.setVisibility(visible);
        if (extendPanel != null)
            extendPanel.setVisibility(visible);
        if (slidePanel != null)
            slidePanel.setVisibility(visible);
        updateAiSceneView(visible , -1);
        mRoundedThumbnailView.setVisibility(visible);
        switchModeBtn.setVisibility(visible);
        updateAdjustFlashPanelUI(visible);

        // SPRD: fix bug 616012 ,the gridline does not disappear when recording audio pictures
        if (visible == View.VISIBLE) {
            String grid = DataModuleManager
                    .getInstance(mController.getAndroidContext())
                    .getCurrentDataModule()
                    .getString(Keys.KEY_CAMERA_GRID_LINES);
            if (mController
                    .getAndroidContext()
                    .getResources()
                    .getString(
                            R.string.pref_camera_composition_line_key_entry_values_grid)
                    .equals(grid)) {
                showNineGridLines();
            }
            if (mController
                    .getAndroidContext()
                    .getResources()
                    .getString(
                            R.string.pref_camera_composition_line_key_entry_values_reticle)
                    .equals(grid)) {
                showReticleGridLines();
            }
            if (mController
                    .getAndroidContext()
                    .getResources()
                    .getString(
                            R.string.pref_camera_composition_line_key_entry_values_box)
                    .equals(grid)) {
                showBoxGridLines();
            }
            if (mController
                    .getAndroidContext()
                    .getResources()
                    .getString(
                            R.string.pref_camera_composition_line_key_entry_values_goldensection)
                    .equals(grid)) {
                showGoldenGridLines();
            }

        } else {
            hideNineGridLines();
            hideReticleGridLines();
            hideBoxGridLines();
            hideGoldenGridLines();
        }
    }

    public void updateScreenGridLines(String grid) {
        //just modify for cyclomatic complexity should use || or && ,but we | and & instead of them

        if(grid == "close" & mGridLines == null & mNineGridLines == null & mReticleGridLines == null & mBoxGridLines == null & mGoldenGridLines == null)
            return; //  do not use this function yet

        // mNineGridLines
        mNineGridLines = updateEachGridLines(mNineGridLines,R.id.nine_grid_lines,R.string.pref_camera_composition_line_key_entry_values_grid,grid);
        // mReticleGridLines
        mReticleGridLines = updateEachGridLines(mReticleGridLines,R.id.reticle_grid_lines,R.string.pref_camera_composition_line_key_entry_values_reticle,grid);
        // mBoxGridLines
        mBoxGridLines = updateEachGridLines(mBoxGridLines,R.id.box_grid_lines,R.string.pref_camera_composition_line_key_entry_values_box,grid);
        // mGoldenGridLines
        mGoldenGridLines = updateEachGridLines(mGoldenGridLines,R.id.golden_grid_lines,R.string.pref_camera_composition_line_key_entry_values_goldensection,grid);
    }

    private <T extends View & PreviewStatusListener.PreviewAreaChangedListener> T updateEachGridLines(T gridLines,int gridId,int gridTypeid,String gridTypeValue){
        if(null == gridLines) {
            gridLines = (T) mCameraRootView.findViewById(gridId);
            if (gridLines instanceof PreviewStatusListener.PreviewAreaChangedListener)
                mTextureViewHelper.addPreviewAreaSizeChangedListener(gridLines);
        }
        if (mController.getAndroidContext().getResources()
                .getString(gridTypeid)
                .equals(gridTypeValue)) {
            showBaseGridLines(gridLines);
        } else {
            hideBaseGridLines(gridLines);
        }
        return gridLines;
    }

    private DreamUI mDreamUI;

    public void setDreamUI(DreamUI dreamUI) {
        mDreamUI = dreamUI;
    }

    public DreamUI getDreamUI() {
        return mDreamUI;
    }

    public boolean isSettingLayoutOpen() {
        // SPRD: Fix bug 658066, top and bottom panels are hidden during taking pictures sometimes
        if (mSettingLayout != null && mSettingLayout.isShown())
            return true;

        return false;
    }

    public void setBottomPanelLeftRightClickable(boolean clickable) {
        setThumbButtonClick(clickable);
        setSwitchButtonClick(clickable);
        setShutterButtonClick(clickable);
    }

    public boolean isShutterButtonClickable() {
        return mShutterButton.isClickable();
    }

    public void dismissDialog(String key) {
        if (mSettingLayout != null && mSettingLayout.getVisibility() == View.VISIBLE) {
            mSettingLayout.dialogDismiss(key);
        }
    }
    public void dismissDialogIfNecessary() {
        if (mSettingLayout != null) {
            mSettingLayout.dismissDialogIfNecessary();
        }
    }

    public void reAddDreamUIPrefRestListener() {
        if(mSettingLayout != null) {
            DataModuleManager.getInstance(mController.getAndroidContext()).addListener(mSettingLayout);
        }
    }
    public boolean isShutterClicked(){
        return mController.getCurrentModule().isShutterClicked();
    }

    /*SPRD:fix bug629900 add switch animation @{*/
    private void updateSwitchPreviewPara() {
        if (mSwitchPreview.getAnimation() != null) {
            return;
        }
        int width = mTextureView.getWidth();
        int height = mTextureView.getHeight();
        float scaledTextureWidth, scaledTextureHeight;
        if (width > height) {
            scaledTextureWidth = Math.min(width, (int) (height * mAspectRatio + 0.5));
            scaledTextureHeight = Math.min(height, (int) (width / mAspectRatio + 0.5));
        } else {
            scaledTextureWidth = Math.min(width, (int) (height / mAspectRatio + 0.5));
            scaledTextureHeight = Math.min(height, (int) (width * mAspectRatio + 0.5));
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSwitchPreview.getLayoutParams();
        params.width = (int)scaledTextureWidth;
        params.height = (int)scaledTextureHeight;

        RectF rect = getPreviewArea();
        if (width > height) {
            params.setMargins((int) rect.left, 0, 0, 0);
        } else {
            params.setMargins(0, (int) rect.top, 0, 0);
        }
        mSwitchPreview.setLayoutParams(params);
    }

    public void startSwitchAnimation(Bitmap preview) {
        mStartSwitchAnimation = true;
        long startMs = System.currentTimeMillis();
        if (preview == null) {
            mSwitchBitmap = mTextureViewHelper.getPreviewBitmapForSwitch(DOWN_SAMPLE_RATE_FOR_SCREENSHOT);
            long endGetBitmap = System.currentTimeMillis();
            Bitmap blurBitmap = CameraUtil.computeScale(mSwitchBitmap, 0.2f);
            //mSwitchBitmap = CameraUtil.doBlur(blurBitmap, 5, false);
            mSwitchBitmap = CameraUtil.blurBitmap(blurBitmap, mController.getAndroidContext());
            blurBitmap.recycle();
            long end = System.currentTimeMillis();
            Log.i(TAG, "get bitmap coast = " +  (endGetBitmap - startMs) + " ms" + " blur bitmap coast = " + (end - endGetBitmap) + " ms");
        } else {
            mSwitchBitmap = preview;
        }
        if (mSwitchBitmap == null) {
            return;
        }
        Drawable drawable =new BitmapDrawable(mSwitchBitmap);
        mSwitchPreview.setImageDrawable(drawable);
        mSwitchBackGround.setVisibility(View.VISIBLE);
        mSwitchPreview.setVisibility(View.VISIBLE);
        //Animation animation = new ScaleAnimation(-1.0f, 1.0f, 1.0f, 1.0f,
        //        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        final float centerX = mTextureView.getWidth() / 2.0f;
        final float centerY = mTextureView.getHeight() / 2.0f;
        RotateAnimation animation = new RotateAnimation(0, 180, centerX, centerY, centerX/2, false, 0);
        animation.setDuration(800);
        animation.setFillAfter(true);
        mSwitchPreview.startAnimation(animation);
    }
    /* SPRD: Fix bug 613015 add SurfaceView support @{ */
    private SurfaceViewEx mSurfaceView;
    public SurfaceHolder getSurfaceHolder() {
        if (mSurfaceView != null) {
            return mSurfaceView.getSurfaceHolder();
        }
        return null;
    }

    public void setSurfaceHolderListener(SurfaceHolder.Callback surfaceHolderListener) {
        if (mSurfaceView != null) {
            mSurfaceView.setSurfaceHolderListener(surfaceHolderListener);
        }
    }

    public void setSurfaceVisibility(boolean visible) {
        Log.i(TAG, "setSurfaceVisibility visible = " + visible);
        if (mSurfaceView != null) {
            mSurfaceView.setVisibility(!visible ? View.GONE : View.VISIBLE);
        }
    }

    public void onPreviewUpdated(byte[] data, CameraAgent.CameraProxy camera) {
        if (mSurfaceView != null) {
            mSurfaceView.onPreviewUpdated(data, camera);
        }
    }
    /* @} */

    public void setAdjustPanel(AdjustPanel adjustPanel){
        mAdjustPanel = adjustPanel;
    }
    public void setBlurPanel(BlurPanel blurPanel){
        mBlurPanel = blurPanel;
    }
    public void hideBlurAndAdjustPanel() {
        if (mAdjustPanel != null) {
            mAdjustPanel.hide();
        }
        if (mBlurPanel != null) {
            mBlurPanel.hide();
        }
    }

    public void showBlurPanel() {
        if (mBlurPanel != null) {
            mBlurPanel.show();
        }
    }

    public int getAdjustPanelTop() {
        if (topPanel != null) {
            return topPanel.getHeight();
        }
        return 0;
    }

    public int getAdjustPanelBottom() {
        if (mBottomBar != null) {
            return mBottomBar.getFrameTop();
        }
        return 0;
    }

    //SPRD:Fix Bug670446
    public boolean isThumbnailViewPressed(){
        if (mRoundedThumbnailView != null){
            return mRoundedThumbnailView.isPressed();
        }
        return false;
    }


    /**
     * SPRD: Fix bug 585183 Adds new feature real-time distance measurement @{
     *
     * @param enabled whether show mode list button only,
     *                if true, only mode list button will be shown in bottom bar.
     */
    public void showModeListBtnOnly(boolean enabled) {
        int visibility = enabled ? View.GONE : View.VISIBLE;
        if (bottomPanelLeft != null) {
            bottomPanelLeft.setVisibility(visibility);
        }
        if (bottomPanelRight != null) {
            bottomPanelRight.setVisibility(visibility);
        }
        if (bottomPanelCenter != null) {
            bottomPanelCenter.setVisibility(visibility);
        }
    }
    /* @} */
    private SettingUIListener uiListener = null;
    public void initSettingLayout(SettingUIListener listener){
        if (mCameraRootView == null) return;
        ViewStub viewStubSettingLayout = (ViewStub) mCameraRootView
                .findViewById(R.id.dream_ui_preference_setting_layout_id);
        if (viewStubSettingLayout != null) {
            viewStubSettingLayout.inflate();
            mSettingLayout = (DreamUIPreferenceSettingLayout) mCameraRootView
                    .findViewById(R.id.dream_ui_preference_setting_layout);
            DataModuleManager.getInstance(mController.getAndroidContext()).addListener(mSettingLayout);
        }
        if (listener != null && listener != uiListener) {
            mSettingLayout.changeModule(listener);
            uiListener = listener;
        }
    }

    public void inflateFilmstrip() {
        LayoutInflater inflater = LayoutInflater.from(mController);
        mFilmstripLayout = (FilmstripLayout) inflater
                .inflate(R.layout.dream_filmstrip, null, false);
    }

    public void initFilmstrip() {
        mAppRootView.addView(mFilmstripLayout, 1);
        mFilmstripView = (FilmstripView) mFilmstripLayout.findViewById(R.id.filmstrip_view);
    }
    private ViewGroup extendPanelParent;
    private AdjustFlashPanel mAdjustFlashPanel;
    public void initExtendPanel(){

        if (extendPanel == null) {
            extendPanel = mCameraRootView
                    .findViewById(R.id.extend_panel_parent);
            extendPanelParent = (ViewGroup) extendPanel;
        }
        extendPanelParent.removeAllViews();

        initZoomPanel();
    }

    public ViewGroup getExtendPanelParent(){
        return extendPanelParent;
    }

    public void initZoomPanel(){
        if (mZoomPanel == null){
            mZoomPanel = (ZoomPanel) mCameraRootView.findViewById(R.id.zoom_panel);
            if(CameraUtil.isNavigationEnable()){
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mZoomPanel.getLayoutParams();
                layoutParams.setMargins(0,0,0,mController.getResources()
                        .getDimensionPixelSize(R.dimen.seekbar_navigation_height));
                mZoomPanel.setLayoutParams(layoutParams);
            }
            mPreviewOverlay.setOnDreamZoomUIChangedListener(mZoomPanel);
            mZoomPanel.setOnProgressChangeListener(mPreviewOverlay);
        }
    }

    /* SPRD: Fix bug 677589 black preview occurs when we switch to video module from photo @{ */
    public void prepareForNextRoundOfSurfaceUpdate() {
        mDreamFilterModuleController.maximizeFrameCount();
    }
    /* @} */
    private boolean initModeListViewEnd = false;
    public void initModeListView(){
        mModeListView = (ModeListView) mAppRootView
                .findViewById(R.id.mode_list_layout);
        if (mModeListView != null) {
            mModeListView.setModeSwitchListener(this);
            mModeListView.setModeListOpenListener(this);
            mModeListView
                    .setCameraModuleScreenShotProvider(mCameraModuleScreenShotProvider);
            mModeListView.setCaptureLayoutHelper(mCaptureLayoutHelper);
            mTextureViewHelper.addPreviewAreaSizeChangedListener(mModeListView);
            initModeListViewEnd = true;
            mModeListView.setTouchable(true);
            mModeListView.setOrientation(nowOrientation, false);
        } else {
            Log.e(TAG, "Cannot find mode list in the view hierarchy");
        }
    }
    public void initBottomBar() {
        if (mBottomBar == null) {
            mBottomBar = (BottomBar) mCameraRootView.findViewById(R.id.bottom_bar);

            // int unpressedColor = mController.getAndroidContext().getResources()
            // .getColor(R.color.camera_gray_background);
            // setBottomBarColor(unpressedColor);
            // updateModeSpecificUIColors();

            mBottomBar.setCaptureLayoutHelper(mCaptureLayoutHelper);
        }

        if (mShutterButton == null) {
            mShutterButton = (ShutterButton) mCameraRootView
                    .findViewById(R.id.shutter_button);
        }

        if (mStickyBottomCaptureLayout == null) {
            mStickyBottomCaptureLayout = (StickyBottomCaptureLayout) mAppRootView
                    .findViewById(R.id.sticky_bottom_capture_layout);
            mStickyBottomCaptureLayout.setCaptureLayoutHelper(mCaptureLayoutHelper);
            mCountdownCancelButton = (ImageButton) mStickyBottomCaptureLayout
                    .findViewById(R.id.shutter_cancel_button);
        }
        // if (mRoundedThumbnailView == null){
        initRoundedThumbnailView();
        // }

        initBottomButtons();

    }

    //SPRD:bug903373 addShutterListener after preview started。
    public void addShutterListener(){
        addShutterListener(mController.getCurrentModuleController());
        addShutterListener(this);
    }

    public void updateButtomBar(){
        // Sets the visibility of the bottom bar and the mode options.
        resetBottomControls(mController.getCurrentModuleController(),
                mController.getCurrentModuleIndex());
    }

    //SPRD：Fix bug927252
    private Counter mCounterInitRoundedThumbnailView = new Counter(1);
    public Counter getCounterInitRoundedThumbnailView() {
        return mCounterInitRoundedThumbnailView;
    }

    public void initRoundedThumbnailView() {
        mRoundedThumbnailView = (RoundedThumbnailView) mCameraRootView
                .findViewById(R.id.rounded_thumbnail_view);
        mCounterInitRoundedThumbnailView.count();
        mRoundedThumbnailView.setCallback(new RoundedThumbnailView.Callback() {
            @Override
            public void onHitStateFinished() {
                if (mFilmstripView == null)
                    return;

                if (mController.getCurrentModule().isShutterClicked()) {
                    setBottomPanelLeftRightClickable(true);
                    return;
                }
                if (mThumbUnClick) {
                    CameraUtil.toastHint(mController, R.string.blur_process_toast);
                    return;
                }
                if(mTakePictureCount.get() == 0){
                    mController.doSometingWhenFilmStripShow();
                }
            }

            @Override
            public void onClick() {
                if(mTakePictureCount.get() == 0){
                    setBottomPanelLeftRightClickable(false);
                }
            }
        });
    }

    public void initGridlineView() {
        ViewStub viewStubGridline = (ViewStub) mCameraRootView
                .findViewById(R.id.layout_grid_lines_id);
        if(viewStubGridline != null){
            viewStubGridline.inflate();
        }
    }

    public boolean isBottomBarNull() {
        return mBottomBar == null;
    }

    public void setCurrentModuleAddSecondTip(int Visibility,int tip){
        if (mCurrentModuleAddSecondTip == null) {
            return;
        }
        if (mCurrentModuleAddTip.getVisibility() == View.VISIBLE){
            return;
        }
        mCurrentModuleAddSecondTip.setVisibility(Visibility);
        mCurrentModuleAddSecondTip.setText(tip);
    }
    public void setBlurEffectTipVisibity(int Visibility){
        if (mCurrentModuleAddSecondTip == null || mBlurEffectTip == null) {
            return;
        }
        if (mCurrentModuleAddTip.getVisibility() == View.VISIBLE){
            return;
        }
        mBlurEffectTip.setVisibility(Visibility);
        mCurrentModuleAddSecondTip.setVisibility(Visibility);
    }
    public void setBlurEffectHighlight(boolean highlight) {
        if (mCurrentModuleAddSecondTip == null || mBlurEffectTip == null) {
            return;
        }
        if (mCurrentModuleAddTip.getVisibility() == View.VISIBLE){
            return;
        }
        if (mBlurEffectTip.getVisibility() == View.GONE) {
            mBlurEffectTip.setVisibility(View.VISIBLE);
        }
        if (highlight) {
            mBlurEffectTip.setBackgroundResource(R.drawable.blur_effect_highlight);
            mBlurEffectTip.setTextColor(mController.getAndroidContext().getColor(
                    R.color.blur_effect_highlight));
            mCurrentModuleAddSecondTip.setVisibility(View.GONE);
        } else {
            mBlurEffectTip.setBackgroundResource(R.drawable.blur_effect_disable);
            mBlurEffectTip.setTextColor(mController.getAndroidContext().getColor(
                    R.color.blur_effect_disable));
        }
    }

    @Override
    public void initializeadjustFlashPanel(int maxValue, int minvalue,
            int currentValue) {
        if (mAdjustFlashPanel != null) {
            mAdjustFlashPanel.initialize(maxValue, minvalue, currentValue);
        }
    }

    @Override
    public void showAdjustFlashPanel() {
        Log.i(TAG, "showAdjustFlashPanel");
        // flash adjust panel
        ViewStub viewStubAdjustFlashPanel = (ViewStub) mCameraRootView
                .findViewById(R.id.layout_adjust_flash_panel_id);
        if (viewStubAdjustFlashPanel != null) {
            viewStubAdjustFlashPanel.inflate();
            mAdjustFlashPanel = (AdjustFlashPanel) mCameraRootView
                    .findViewById(R.id.adjust_flash_panel);
            DataStorageStruct data = (DataStorageStruct) DataModuleManager.getInstance(mController)
                    .getCurrentDataModule().getSupportSettingsList()
                    .get(Keys.KEY_ADJUST_FLASH_LEVEL);

            mAdjustFlashPanel.initialize(
                    Integer.valueOf(data.mEntryValues[data.mEntryValues.length - 1].toString()),
                    Integer.valueOf(data.mEntryValues[0].toString()),
                    DataModuleManager.getInstance(mController)
                            .getCurrentDataModule()
                            .getInt(Keys.KEY_ADJUST_FLASH_LEVEL));
        }

        if (mAdjustFlashPanel != null) {
            mAdjustFlashPanel.initialize(DataModuleManager.getInstance(mController)
                    .getCurrentDataModule()
                    .getInt(Keys.KEY_ADJUST_FLASH_LEVEL));
            mAdjustFlashPanel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideAdjustFlashPanel() {
        if (mAdjustFlashPanel != null) {
            mAdjustFlashPanel.setVisibility(View.GONE);
        }
    }

    public int getBottomBarBlackHeight() {
        if (mBottomBar == null) {
            return 0;
        }
        return mBottomBar.getBottomBlackHeight();
    }

    public void inflateStub(){

        Log.d(TAG,"inflateStub start");
        inflateLayout();
        initBottomBar();
        initExtendPanel();
        Log.e(TAG,"inflateStub end");

    }

    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mController).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS,View.VISIBLE);
        SlidePanelManager.getInstance(mController).focusItem(
                SlidePanelManager.CAPTURE, false);
    }

    protected void inflateLayout() {
        ViewGroup moduleRoot = (ViewGroup) mCameraRootView.findViewById(R.id.module_layout);
        moduleRoot.addView(mController.getLayoutInflater().inflate(R.layout.photo_module, null, false));

        ViewStub viewStubBottomBar = (ViewStub) mCameraRootView
                .findViewById(R.id.dream_sticky_bottom_capture_layout_id);
        if (viewStubBottomBar == null) {
            return;
        }

        if (viewStubBottomBar != null) {
            viewStubBottomBar.inflate();
        }

        ViewStub viewStubExtentPanel = (ViewStub) mCameraRootView.findViewById(R.id.extend_panel_id);
        if (viewStubExtentPanel != null) {
            viewStubExtentPanel.inflate();
        }

        ViewStub viewStubMagiclensBottom = (ViewStub) mCameraRootView
                .findViewById(R.id.layout_ucam_magiclens_bottom_id);
        if (viewStubMagiclensBottom != null) {
            viewStubMagiclensBottom.inflate();
        }
    }

    private AtomicInteger mTakePictureCount = new AtomicInteger(0);

    public void incTakePictureCount() {
        mTakePictureCount.incrementAndGet();
    }

    public void decTakePictureCount() {

        if(mTakePictureCount.decrementAndGet() < 0){
            mTakePictureCount.set(0);
        }
    }

    public void resetTakePictureCount() {
        mTakePictureCount.set(0);
    }

}
