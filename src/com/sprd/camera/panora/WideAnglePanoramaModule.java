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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Environment;
import android.os.Trace;
import android.os.ParcelFileDescriptor;
import com.android.camera.debug.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.view.WindowManager;
import android.media.MediaActionSound;

import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.OrientationManager;
import com.android.camera.app.OrientationManagerImpl;
import com.android.camera.data.FilmstripItemData;
import com.android.camera.exif.ExifInterface;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.settings.Keys;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.ToastUtil;
import com.android.camera.CameraActivity;
import com.android.camera.CameraModule;
//import com.android.camera.SoundClips;
import com.android.camera.Storage;
import com.android.camera.PanoUtil;
import com.android.camera.PanoProgressBar;
import com.android.camera.Mosaic;
import com.android.camera.ButtonManager;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataStructSetting;
import com.dream.camera.util.DreamUtil;
import com.sprd.camera.storagepath.ExternalStorageUtil;
import com.sprd.camera.storagepath.StorageUtil;
import com.sprd.camera.storagepath.StorageUtilProxy;
import com.android.camera.module.ModuleController;
import com.android.camera2.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;
import android.os.HandlerThread;
import java.util.HashMap;
import java.util.Map;

import com.android.camera.util.Size;
import com.android.camera.settings.SettingsScopeNamespaces;
/**
 * Activity to handle panorama capturing.
 */
public class WideAnglePanoramaModule
        extends CameraModule
        implements WideAnglePanoramaController,
        ModuleController,
        DreamSettingChangeListener,
        OrientationManager.OnOrientationChangeListener,
        SurfaceTexture.OnFrameAvailableListener {

    public static final int DEFAULT_SWEEP_ANGLE = 160;
    public static final int DEFAULT_SWEEP_ANGLE_NEW = 360;
    public static final int MAX_SWEEP_ANGLE = CameraUtil.isNewWideAngle() ? DEFAULT_SWEEP_ANGLE_NEW : DEFAULT_SWEEP_ANGLE;
    public static final int LARGER_SWEEP_ANGLE_H = 60;
    public static final int DEFAULT_BLEND_MODE = Mosaic.BLENDTYPE_HORIZONTAL;
    public static final int DEFAULT_CALLBACK_WIDTH = 3264;
    public static final int DEFAULT_CALLBACK_PIXELS = 2448 * DEFAULT_CALLBACK_WIDTH;
    public static final int DEFAULT_VIEW_HEIGTH = 1080;
    public static final int DEFAULT_CAPTURE_PIXELS = 2376 * DEFAULT_VIEW_HEIGTH;//SPRD:fix bug 614910/1016944/1031430 make sure the preview is clear,max is 2.2 ration of 1080p
    // SPRD: FixBug256298 Panoramic camera memory is too large, change the resolution to reduce
    // memory usage.
    // public static final int DEFAULT_CAPTURE_PIXELS = 720 * 540;
    /* SPRD:fix bug 328907 adjust sweep angle @{ */
    public static float DEFAULT_SWEEP_ANGLE_FACTOR = 2f;
    /* @} */
    private static final int MSG_LOW_RES_FINAL_MOSAIC_READY = 1;
    private static final int MSG_GENERATE_FINAL_MOSAIC_ERROR = 2;
    private static final int MSG_END_DIALOG_RESET_TO_PREVIEW = 3;
    private static final int MSG_CLEAR_SCREEN_DELAY = 4;
    private static final int MSG_RESET_TO_PREVIEW = 5;
    private static final int OPEN_CAMERA_FAIL = 6;
    private static final int CHECK_STORAGE_SPACE = 7;
    private static final int MSG_RESET_KEY_ENTER = 8;
    private static final int RESET_KEY_ENTER_DELAY = 250;

    private static final int SCREEN_DELAY = 150 * 1000;
    @SuppressWarnings("unused")
    private static final Log.Tag TAG = new Log.Tag("CAM_WidePanoModule");
    private static final int PREVIEW_STOPPED = 0;
    private static final int PREVIEW_ACTIVE = 1;
    public static final int CAPTURE_STATE_VIEWFINDER = 0;
    public static final int CAPTURE_STATE_MOSAIC = 1;

    // The unit of speed is degrees per frame.
    private static final float PANNING_SPEED_THRESHOLD = 2.5f;
    private static final float PANNING_SPEED_THRESHOLD_NEW = 6f;//SPRD:fix bug 907288/958621
    private static final boolean DEBUG = false;

    private ContentResolver mContentResolver;
    protected WideAnglePanoramaUI mUI;

    private MosaicPreviewRenderer mMosaicPreviewRenderer;
    private Object mRendererLock = new Object();
    private Object mWaitObject = new Object();

    private String mPreparePreviewString;
    private String mDialogTitle;
    private String mDialogOkString;
    private String mDialogPanoramaFailedString;
    private String mDialogWaitingPreviousString;

    private int mPreviewUIWidth;
    private int mPreviewUIHeight;
    private boolean mUsingFrontCamera;
    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private int mCameraCallbackWidth = 0;
    private int mCameraCallbackHeight = 0;
    private int mCameraState;
    private int mCaptureState;
    private PowerManager.WakeLock mPartialWakeLock;
    private MosaicFrameProcessor mMosaicFrameProcessor;
    private boolean mMosaicFrameProcessorInitialized;
    private AsyncTask<Void, Void, Void> mWaitProcessorTask;
    private long mTimeTaken;
    private Handler mMainHandler;
    private SurfaceTexture mCameraTexture;
    private boolean mThreadRunning;
    private boolean mCancelComputation;
    private float mHorizontalViewAngle;
    private float mVerticalViewAngle;
    private boolean mResetToPreviewWithoutSave;

    // Prefer FOCUS_MODE_INFINITY to FOCUS_MODE_CONTINUOUS_VIDEO because of
    // getting a better image quality by the former.
    private String mTargetFocusMode = "continuous-picture";

    //private PanoOrientationEventListener mOrientationEventListener;
    // The value could be 0, 90, 180, 270 for the 4 different orientations measured in clockwise
    // respectively.
    private int mDeviceOrientation;
    private int mDeviceOrientationAtCapture;
    private int mCameraOrientation;
    private int mOrientationCompensation;

    // private SoundClips.Player mSoundPlayer;

    private Runnable mOnFrameAvailableRunnable;

    protected AppController mAppController;
    private CameraActivity mActivity;
    private View mRootView;
    private CameraProxy mCameraDevice;
    private CameraCapabilities mCameraCapabilities;
    private boolean mPaused;
    private boolean mHasEndSaveHigh = false;

    // private OrientationManagerImpl mOrientationManager;
    private boolean mMosaicPreviewConfigured;
    private boolean mPreviewFocused = true;

    private MediaActionSound mCameraSound;
    public boolean mSavingPanorama = false;

    // CID 125023: UuF: Unused field (FB.UUF_UNUSED_FIELD)
    //private float mZoomValue; // The current zoom ratio.

    private boolean mPreviewing = false;

    private boolean mAeLockSupported;

    protected CameraSettings mCameraSettings;
    protected DataModuleBasic mDataModuleCurrent;
    private float mFastToastThreshold = PANNING_SPEED_THRESHOLD_NEW;

    @Override
    public void onPreviewUIReady() {
        Log.i(TAG, "onPreviewUIReady");
        configMosaicPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {

    }

    private class MosaicJpeg {
        public MosaicJpeg(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.isValid = true;
        }

        public MosaicJpeg() {
            this.data = null;
            this.width = 0;
            this.height = 0;
            this.isValid = false;
        }

        public final byte[] data;
        public final int width;
        public final int height;
        public final boolean isValid;
    }

    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        mDeviceOrientation = deviceOrientation.getDegrees();
        Log.i(TAG, "onOrientationChanged mDeviceOrientation = " + mDeviceOrientation);
        int orientationCompensation = mDeviceOrientation
                + CameraUtil.getDisplayRotation() % 360;
        if (mOrientationCompensation != orientationCompensation) {
            mOrientationCompensation = orientationCompensation;
        }
    }

    private class PanoOrientationEventListener extends OrientationEventListener {
        public PanoOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            mDeviceOrientation = CameraUtil.roundOrientation(orientation, mDeviceOrientation);
            /*SPRD:fix bug627179 show black view when back from filmstripview at landspace @{ */
            if (mUI != null) {
                mUI.setDisplayOrientation(mDeviceOrientation);
            }
            /* @ */

            /* SPRD:Add for bug 479120 wideangle module rotate issue @{ */
//            if (mDeviceOrientation == 0 || mDeviceOrientation == 180) {
//                mUI.getProgressLayout().setPadding(0, 0, 0, 220);
//            } else {
//                mUI.getProgressLayout().setPadding(0, 0, 0, 20);
//            }
            /* @} */

            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mDeviceOrientation
                    + CameraUtil.getDisplayRotation() % 360;
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
            }
        }
    }

    public WideAnglePanoramaModule(AppController app) {
        super(app);
    }

    @Override
    public void destroy() {
        Log.i(TAG, "destroy");
        // TODO: implement this.
        if (CameraUtil.isNewWideAngle()) {
            if (mPreviewDataCallbackThread != null) {
                mPreviewDataCallbackThread.quitSafely();
                mPreviewDataCallbackThread = null;
            }
            if (mCaptureProcessThread != null) {
                mCaptureProcessThread.quitSafely();
                mCaptureProcessThread = null;
            }
            if (mUI != null) {
                mUI.onDestroy();
            }
        }
    }

    @Override
    public String getPeekAccessibilityString() {
        return mAppController.getAndroidContext()
                .getResources().getString(R.string.photo_accessibility_peek);
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        return;
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        mUI.onPreviewVisibilityChanged();
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        mUI.onConfigurationChanged(mThreadRunning);
    }

    /* SPRD:fix bug 644527 check camera id and api for proxy @ */
    public boolean checkCameraProxy() {
        return (useNewApi() == getCameraProvider().isNewApi()) &&
                (getCameraProvider().getFirstBackCameraId() == getCameraProvider().getCurrentCameraId().getLegacyValue());
    }
    /* @} */

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable mThreadRunning = " + mThreadRunning);
        if (mPaused) {
            return;
        }
        /* SPRD: fix bug549564  CameraProxy uses the wrong API @{ */
        if (!checkCameraProxy()) {
            if (cameraProxy != null) {
                mActivity.getCameraProvider().releaseCamera(cameraProxy.getCameraId());
            }
            resume();
            Log.d(TAG, "cameraProxy is error, resumed!");
            return;
        }
        /* @} */
        mCameraDevice = cameraProxy;

        int cameraId = mActivity.getCameraProvider().getFirstBackCameraId();
        Characteristics info =
                mActivity.getCameraProvider().getCharacteristics(cameraId);
        mCameraOrientation = info.getSensorOrientation();
        if (cameraId == mActivity.getCameraProvider().getFirstFrontCameraId())
            mUsingFrontCamera = true;

        mCameraCapabilities = mCameraDevice.getCapabilities();
        mAeLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        mMaxRatio = mCameraCapabilities.getMaxZoomRatio();
        mCameraSettings = mCameraDevice.getSettings();
        setupCaptureParams();
        applySettings();

        // Check if another panorama instance is using the mosaic frame processor.
        mUI.dismissAllDialogs();
        if (!mThreadRunning && mMosaicFrameProcessor.isMosaicMemoryAllocated() && !mHasEndSaveHigh && !CameraUtil.isNewWideAngle()) {//SPRD:fix bug928848
            mUI.showWaitingDialog(mDialogWaitingPreviousString);
            // If stitching is still going on, make sure switcher and shutter button
            // are not showing
            mWaitProcessorTask = new WaitProcessorTask().execute();
        } else {
            Point size = mUI.getPreviewAreaSize();
            // Point size = mActivity.getCameraAppUI().getPreviewAreaSize();
            mPreviewUIWidth = size.x;
            mPreviewUIHeight = size.y;
            // Camera must be initialized before MosaicFrameProcessor is
            // initialized. The preview size has to be decided by camera device.
            initMosaicFrameProcessorIfNeeded();
            configMosaicPreview();
            /*
             * SPRD: fixbug 267724 if not in preview,do not display hint
             * @orig mActivity.updateStorageSpaceAndHint();
             * @{
             */
            if (mCameraState == PREVIEW_ACTIVE && mMainHandler != null) {
                mMainHandler.sendEmptyMessage(CHECK_STORAGE_SPACE);
            }
            /* @} */

        }
    }

    @Override
    public HardwareSpec getHardwareSpec() {
        return mCameraCapabilities != null ?
                new HardwareSpecImpl(getCameraProvider(), mCameraCapabilities,false) : null;
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return new CameraAppUI.BottomBarUISpec();
    }

    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    private final Object objLock = new Object();
    private void initMosaicProcessorAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (objLock) {
                    if (mMosaicFrameProcessor == null ) {
                        mMosaicFrameProcessor = MosaicFrameProcessor.getInstance();
                    }
                }
            }
        }).start();
    }
    private boolean mStartCapture = false;
    @Override
    public boolean getCaptureState() {
        return mStartCapture;
    }
    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        Log.i(TAG, "init Camera");
        initMosaicProcessorAsync();
        mActivity = activity;
        // mRootView = mActivity.getModuleLayoutRoot();
        mRootView = mActivity.getCameraAppUI().getModuleView();
        mRootView.setVisibility(View.VISIBLE);
        mAppController = mActivity;
        // change the data storage module
        int cameraId = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getDataModuleCamera().getInt(Keys.KEY_CAMERA_ID);

        DataStructSetting dataSetting = new DataStructSetting(
                DreamUtil.intToString(getMode()), DreamUtil.isFrontCamera(
                mAppController.getAndroidContext(), cameraId),
                mActivity.getCurrentModuleIndex(), cameraId);
        long start = System.currentTimeMillis();
        // will cost much time
        if (mActivity.isActivityResumed()) {
            requestCameraOpen();
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "init: requestCameraOpen cost: " + (end - start) + "ms");
        // will cost much time
        DataModuleManager.getInstance(mAppController.getAndroidContext())
                .changeModuleStatus(dataSetting);
        mDataModuleCurrent = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getCurrentDataModule();
        mDataModuleCurrent.addListener(mActivity,this);
        Log.d(TAG, "init: changeModuleStatus cost: " + (System.currentTimeMillis() - end) + "ms");

        mActivity.getCameraAppUI().setLayoutAspectRation(0f);
        // mOrientationManager = new OrientationManagerImpl(activity);
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        // will cost much time
        mUI = createUI(activity);

        mUI.onOrientationChanged(mAppController.getOrientationManager().getDeviceOrientation());


        // mActivity.setPreviewStatusListener(mUI);
        mActivity.getCameraAppUI().setStatusListener(mUI);

        mUI.setShutterButtonVisible(false);
        // add for switch from photomodule shutter button display error
        mUI.setShutterButtonVisible(true);

        mUI.setCaptureProgressOnDirectionChangeListener(
                new PanoProgressBar.OnDirectionChangeListener() {
                    @Override
                    public void onDirectionChange(int direction) {
                        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
                            mUI.showDirectionIndicators(direction);
                        }
                    }
                });

        mContentResolver = mActivity.getContentResolver();
        // This runs in UI thread.
        mOnFrameAvailableRunnable = new Runnable() {
            @Override
            public void run() {
                // Frames might still be available after the activity is paused.
                // If we call onFrameAvailable after pausing, the GL thread will crash.
                if (mPaused)
                    return;

                MosaicPreviewRenderer renderer = null;
                synchronized (mRendererLock) {
                    if (mMosaicPreviewRenderer == null) {
                        return;
                    }
                    renderer = mMosaicPreviewRenderer;
                }
                if (CameraUtil.isNewWideAngle()) {
                    renderer.showPreviewFrame();
                    /*SPRD:fix bug696804 hide cover use the callback @{*/
                    if (frameCount <= MAX_FRAME_COUNT) {
                        if (frameCount == MAX_FRAME_COUNT) {
                            mActivity.getCameraAppUI().onSurfaceTextureUpdated();
                        }
                        frameCount++;
                    }
                    /* @} */
                } else {
                    if (mRootView.getVisibility() != View.VISIBLE) {
                        renderer.showPreviewFrameSync();
                        mRootView.setVisibility(View.VISIBLE);
                    } else {
                        if (mCaptureState == CAPTURE_STATE_VIEWFINDER) {
                            renderer.showPreviewFrame();
                        } else {
                            renderer.alignFrameSync();
                            mMosaicFrameProcessor.processFrame();
                        }
                    }
                }
            }
        };

        PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Panorama");

        //mOrientationEventListener = new PanoOrientationEventListener(mActivity);
        synchronized (objLock) {
            if (mMosaicFrameProcessor == null) {
                mMosaicFrameProcessor = MosaicFrameProcessor.getInstance();
            }
        }
        Resources appRes = mActivity.getResources();
        mPreparePreviewString = appRes.getString(R.string.pano_dialog_prepare_preview);
        mDialogTitle = appRes.getString(R.string.pano_dialog_title);
        mDialogOkString = appRes.getString(R.string.dialog_ok);
        mDialogPanoramaFailedString = appRes.getString(R.string.pano_dialog_panorama_failed);
        mDialogWaitingPreviousString = appRes.getString(R.string.pano_dialog_waiting_previous);

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOW_RES_FINAL_MOSAIC_READY:
                        if (!CameraUtil.isNewWideAngle()) {
                            onBackgroundThreadFinished();
                            showFinalMosaic((Bitmap) msg.obj);
                        }
                        mHasEndSaveHigh = false;
                        saveHighResMosaic();
                        break;
                    case MSG_GENERATE_FINAL_MOSAIC_ERROR:
                        mStartCapture = false;
                        onBackgroundThreadFinished();
                        if (mPaused) {
                            resetToPreviewIfPossible();
                        } else {
                            mUI.showAlertDialog(
                                    mDialogTitle, mDialogPanoramaFailedString,
                                    mDialogOkString, new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                            resetToPreviewIfPossible();
                        }
                        clearMosaicFrameProcessorIfNeeded();
                        mHasEndSaveHigh = true;
                        break;
                    case MSG_END_DIALOG_RESET_TO_PREVIEW:
                        /*
                         * SPRD:Fix bug349157 Toast message shown when the image can not been saved
                         * from panorama.@{
                         */
                        Log.d(TAG, "MSG_END_DIALOG_RESET_TO_PREVIEW mResetToPreviewWithoutSave = "
                                + mResetToPreviewWithoutSave);
                        if (mResetToPreviewWithoutSave) {
                            CameraUtil.toastHint(mActivity,R.string.cannot_save_image);
                            mResetToPreviewWithoutSave = false;
                        }
                        /* }@ */
                        onBackgroundThreadFinished();
                        resetToPreviewIfPossible();
                        mStartCapture = false;
                        clearMosaicFrameProcessorIfNeeded();
                        mHasEndSaveHigh = true;
                        break;
                    case MSG_CLEAR_SCREEN_DELAY:
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.
                                FLAG_KEEP_SCREEN_ON);
                        break;
                    case MSG_RESET_TO_PREVIEW:
                        resetToPreviewIfPossible();
                        break;
                    case OPEN_CAMERA_FAIL: {
                        /*
                         * CameraUtil.showErrorAndFinish(mActivity, R.string.cannot_connect_camera);
                         */
                        break;
                    }
                    case CHECK_STORAGE_SPACE:
                        if (mActivity != null) {
                            mActivity.updateStorageSpaceAndHint(null);
                        }
                        break;
                    case MSG_RESET_KEY_ENTER:
                        if (mActivity != null) {
                            mActivity.resetHasKeyEventEnter();
                        }
                        break;
                }
            }
        };

        if (CameraUtil.isNewWideAngle()) {
            mPreviewDataCallbackThread = new HandlerThread("preview thread");
            mPreviewDataCallbackThread.start();
            mCaptureProcessThread = new HandlerThread("capture thread");
            mCaptureProcessThread.start();
            mCaptureHandler = new Handler(mCaptureProcessThread.getLooper());
        }
    }

    @Override
    public boolean useNewApi() {
        if (!CameraUtil.isNewWideAngle()) {
            return false;
        }
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity.getContentResolver());
    }

    /**
     * Opens camera and sets the parameters.
     *
     * @return Whether the camera was opened successfully.
     */
    private void requestCameraOpen() {
        int cameraId = mActivity.getCameraProvider().getFirstBackCameraId();
        if (cameraId == -1)
            cameraId = 0;
        mActivity.getCameraProvider().requestCamera(cameraId, useNewApi());
    }

    private void releaseCamera() {
        if (mCameraDevice != null) {
            mActivity.getCameraProvider().releaseCamera(mCameraDevice.getCameraId());
            mCameraDevice = null;
            mCameraState = PREVIEW_STOPPED;
        }
        mPreviewing = false;
    }

    /**
     * Opens the camera device. The back camera has priority over the front one.
     *
     * @return Whether the camera was opened successfully.
     */

    private boolean findBestPreviewSize(List<Size> supportedSizes, boolean need16To9,
            boolean needSmaller) {
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        int pixelsDiffCapture = DEFAULT_CALLBACK_PIXELS;
        boolean hasFound = false;
        com.android.camera.util.Size displayRealSize = CameraUtil.getDefaultDisplayRealSize();
        float targetRation = (float)(displayRealSize.getHeight() - CameraUtil.getCutoutHeight()) / (float)displayRealSize.getWidth() >= 18/9f ? 18/9f : 16/9f;
        for (Size size : supportedSizes) {
            int h = size.height();
            int w = size.width();
            if (CameraUtil.isWideSupport8MEnable()) {
                int diff = DEFAULT_CALLBACK_PIXELS - h * w;
                if (needSmaller && diff < 0) {
                    continue;
                }
                if (need16To9 &&(float)w/h != targetRation) {
                    continue;
                }
                if (w > DEFAULT_CALLBACK_WIDTH) {
                    continue;
                }
                diff = Math.abs(diff);
                if (diff < pixelsDiffCapture) {
                    mCameraCallbackWidth = 2592;//3264;// w
                    mCameraCallbackHeight = 1944;//2448;// h
                    pixelsDiffCapture = diff;
                }
            }
            // we only want 16:9 format.
            int d = DEFAULT_CAPTURE_PIXELS - h * w;
            if (needSmaller && d < 0) { // no bigger preview than 960x720.
                continue;
            }
            if (need16To9 &&(float)w/h != targetRation) {//SPRD:fix bug 614910 change the preview size to 16:9
                continue;
            }
            if (h > DEFAULT_VIEW_HEIGTH) {
                continue;
            }
            d = Math.abs(d);
            if (d < pixelsDiff) {
                mCameraPreviewWidth = 1600; // w
                mCameraPreviewHeight = 1200; // h
                pixelsDiff = d;
                hasFound = true;
            }
        }
        return hasFound;
    }

    private void setupCaptureParams() {
        mCameraSettings.setDefault(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD == CameraUtil.getFrontFlashMode(), mCameraCapabilities);//SPRD:fix bug616836 add for photo use api1 or api2 use reconnect

        List<Size> supportedSizes = Size.convert(mCameraCapabilities.getSupportedPreviewSizes());
        if (!findBestPreviewSize(supportedSizes, true, true)) {
            Log.w(TAG, "No 16:9 ratio preview size supported.");
            if (!findBestPreviewSize(supportedSizes, false, true)) {
                Log.w(TAG, "Can't find a supported preview size smaller than 960x720.");
                findBestPreviewSize(supportedSizes, false, false);
            }
        }
        Log.d(TAG, "camera preview h = " + mCameraPreviewHeight + " , w = " + mCameraPreviewWidth + " callback  w = " + mCameraCallbackWidth + " h =" + mCameraCallbackHeight);
        mCameraSettings.setPreviewSize(new Size(mCameraPreviewWidth, mCameraPreviewHeight).toPortabilitySize());
        if (!CameraUtil.isWideSupport8MEnable() || mCameraCallbackWidth == 0 || mCameraCallbackHeight == 0) {
            mCameraCallbackWidth = mCameraPreviewWidth;
            mCameraCallbackHeight = mCameraPreviewHeight;
        }
        mCameraSettings.setCallbackSize(new Size(mCameraCallbackWidth, mCameraCallbackHeight).toPortabilitySize());
        List<com.android.ex.camera2.portability.Size> supportedPhotoSizes = mCameraCapabilities.getSupportedPhotoSizes();
        mCameraSettings.setPhotoSize(supportedPhotoSizes != null ? supportedPhotoSizes.get(supportedPhotoSizes.size() - 1)
                : new Size(mCameraPreviewWidth, mCameraPreviewHeight).toPortabilitySize());//must set PhotoSize, Otherwise can not applysetting//SPRD:fix bug1210107

        List<int[]> frameRates = mCameraCapabilities.getSupportedPreviewFpsRange();
        if (!CameraUtil.isNewWideAngle()) {
            int last = frameRates.size() - 1;
            int minFps = (frameRates.get(last))[0];
            int maxFps = (frameRates.get(last))[1];
            mCameraSettings.setPreviewFpsRange(minFps, maxFps);
            Log.d(TAG, "preview fps: " + minFps + ", " + maxFps);
        } else {
            int[] fpsRange = CameraUtil
                    .getPhotoPreviewFpsRange(frameRates, useNewApi(), false);
            if (fpsRange != null && fpsRange.length > 0) {
                mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                Log.d(TAG, "preview fps: " + fpsRange[0] + ", " + fpsRange[1]);
            }
        }

        if (mCameraCapabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE)) {
            mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
        } else {
            // Use the default focus mode and log a message
            Log.w(TAG, "Cannot set the focus mode to " + mTargetFocusMode +
                    " becuase the mode is not supported.");
        }

        mCameraSettings.setRecordingHintEnabled(false);
        mCameraSettings.set3DNREnable(0);
        mCameraSettings.setAppModeId(mActivity.getCurrentModuleIndex());

        /* SPRD:fix bug 328907 adjust sweep angle @{ */
        float angleFactor = (float)mCameraPreviewWidth * mCameraPreviewHeight / (960 * 720);
        if (angleFactor > DEFAULT_SWEEP_ANGLE_FACTOR) {
            angleFactor = DEFAULT_SWEEP_ANGLE_FACTOR;
        }
        if (CameraUtil.isWideSupport8MEnable()) {
            mFastToastThreshold = PANNING_SPEED_THRESHOLD_NEW * (3264.0f / mCameraCallbackWidth);
        } else {
            mFastToastThreshold = PANNING_SPEED_THRESHOLD_NEW * (1080.0f / mCameraPreviewHeight);
        }

        if (!CameraUtil.isNewWideAngle() || !useNewApi()) {
            Parameters parameters = mCameraDevice.getParameters();
            Log.i(TAG, " angleFactor = " + angleFactor + " getHorizontalViewAngle = " + parameters.getHorizontalViewAngle() + " getVerticalViewAngle = " + parameters.getVerticalViewAngle());
            /* SPRD:fix bug 641857 modify horizontal view angle for landspace @{ */
            if (parameters.getHorizontalViewAngle()/parameters.getVerticalViewAngle() > 4/3f || parameters.getHorizontalViewAngle() > LARGER_SWEEP_ANGLE_H) {
                mHorizontalViewAngle = parameters.getHorizontalViewAngle() * angleFactor;
            } else {
                mHorizontalViewAngle = parameters.getHorizontalViewAngle() * angleFactor * 4/3;
            }
            mVerticalViewAngle = parameters.getVerticalViewAngle() * angleFactor ;
            /* @} */
        }
    }

    private void updateFocusMode(boolean cafmode) {
        if (mCameraDevice == null) {
            return;
        }
        if (cafmode) {
            if (mCameraCapabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE)) {
                mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
            } else {
                // Use the default focus mode and log a message
                Log.w(TAG, "Cannot set the focus mode to " + mTargetFocusMode +
                        " becuase the mode is not supported.");
            }
            if (mAeLockSupported && CameraUtil.isWideCaptureLockAEEnabled()) {
                mCameraSettings.setAutoExposureLock(false);
                mCameraSettings.setAutoWhiteBalanceLock(false);
            }
        } else {
            mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.AUTO);
            if (mAeLockSupported && CameraUtil.isWideCaptureLockAEEnabled()) {
                mCameraSettings.setAutoExposureLock(true);//SPRD:fix bug732162
                mCameraSettings.setAutoWhiteBalanceLock(true);//SPRD:fix bug1084395
            }
        }
        applySettings();
    }

//    public int getPreviewBufSize() {
//        PixelFormat pixelInfo = new PixelFormat();
//        PixelFormat.getPixelFormatInfo(mCameraDevice.getParameters().getPreviewFormat(), pixelInfo);
//        // TODO: remove this extra 32 byte after the driver bug is fixed.
//        return (mCameraPreviewWidth * mCameraPreviewHeight * pixelInfo.bitsPerPixel / 8) + 32;
//    }

    private void applySettings() {
        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    /**
     * Configures the preview renderer according to the dimension defined by {@code mPreviewUIWidth}
     * and {@code mPreviewUIHeight}. Will stop the camera preview first.
     */
    private void configMosaicPreview() {
        /** SPRD: fix bug 320287 native crash @ { */
        if (mPreviewUIWidth == 0 || mPreviewUIHeight == 0
                || mUI.getSurfaceTexture() == null || mCameraDevice == null) {
            /** @}*/
            return;
        }

        stopCameraPreview();
        synchronized (mRendererLock) {
            if (mMosaicPreviewRenderer != null) {
                mMosaicPreviewRenderer.release();
            }
            mMosaicPreviewRenderer = null;
        }
        final boolean isLandscape =
                (mActivity.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE);
        mUI.flipPreviewIfNeeded();
        MosaicPreviewRenderer renderer = new MosaicPreviewRenderer(
                mUI.getSurfaceTexture(),
                mPreviewUIWidth, mPreviewUIHeight, isLandscape);
        synchronized (mRendererLock) {
            mMosaicPreviewRenderer = renderer;
            mCameraTexture = mMosaicPreviewRenderer.getInputSurfaceTexture();

            if (!mPaused && !mThreadRunning && mWaitProcessorTask == null) {
                mMainHandler.sendEmptyMessage(MSG_RESET_TO_PREVIEW);
            }
            mRendererLock.notifyAll();
        }
        mMosaicPreviewConfigured = true;
        // SPRD: fix bug545514 same with MSG_RESET_TO_PREVIEW above, deleted to reduce the times of startPreview/stopPreview
        // resetToPreviewIfPossible();
    }

    @Override
    public void stopCapture() {
        stopCapture(false);
    }

    private int mOrientationCapture = 0;
    @Override
    public void setViewPort(final float viewPortX, final float viewPortY, final int factor, final int orientation, final int director) {
        int previewShortEdge = (mCameraCallbackWidth > mCameraCallbackHeight) ? mCameraCallbackHeight : mCameraCallbackWidth;
        int uiShortEdge = (mPreviewUIHeight > mPreviewUIWidth) ? mPreviewUIWidth : mPreviewUIHeight;
        final float ratioPreviewToUI = previewShortEdge * 1.0f / uiShortEdge;
        int previewLongEdge = (mCameraCallbackWidth < mCameraCallbackHeight) ? mCameraCallbackHeight : mCameraCallbackWidth;
        int uiLongEdge = (mPreviewUIHeight < mPreviewUIWidth) ? mPreviewUIWidth : mPreviewUIHeight;
        final float ratioPreviewToUILong = previewLongEdge * 1.0f / uiLongEdge;
        Log.i(TAG, "ratioPreviewToUI " + ratioPreviewToUI + " ratioPreviewToUILong = " + ratioPreviewToUILong);
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {//add for rotation when catpure
            mOrientationCapture = orientation;//SPRD:fix bug697843
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mMosaicPreviewRenderer != null && !mPaused && (orientation == mDeviceOrientation && mProgressLand || !mProgressLand)) {//SPRD:fix bug958600
                        mMosaicPreviewRenderer.setViewPort(viewPortX, viewPortY, factor, ratioPreviewToUI, ratioPreviewToUILong, orientation, director);
                    }
                }
            }, 100);
            return;
        }
        if (mMosaicPreviewRenderer != null) {
            mMosaicPreviewRenderer.setViewPort(viewPortX, viewPortY, factor, ratioPreviewToUI, ratioPreviewToUILong, orientation, director);
        }
    }

    @Override
    public void setCaptureViewPort(final float viewPortX, final float viewPortY, final int width, final int height) {
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {//add for rotation when catpure
            stopCapture(false);
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mMosaicPreviewRenderer != null && !mPaused && (mOrientationCapture == mDeviceOrientation && mProgressLand || !mProgressLand)) {
                        mMosaicPreviewRenderer.setCaptureViewPort(viewPortX, viewPortY, width, height, mProgressLand);
                    }
                }
            }, 100);
            return;
        }
        if (mMosaicPreviewRenderer != null) {
            mMosaicPreviewRenderer.setCaptureViewPort(viewPortX, viewPortY, width, height, mProgressLand);
        }
    }
    /**
     * Receives the layout change event from the preview area. So we can initialize the mosaic
     * preview renderer.
     */
    @Override
    public void onPreviewUILayoutChange(int l, int t, int r, int b) {
        Log.d(TAG, "layout change: " + (r - l) + "/" + (b - t));
        /*SPRD: Fix bug 539829 @{ */
        if ((mPreviewUIWidth == r - l) && (mPreviewUIHeight == b - t) || mActivity.isFilmstripVisible()) {//SPRD:fix bug 627179
            Log.d(TAG, "layout change: " + mPreviewUIWidth + "/" + mPreviewUIHeight + ", return");
            return;
        }
        /* }@ */
        mPreviewUIWidth = r - l;
        mPreviewUIHeight = b - t;
        configMosaicPreview();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surface) {
        /*
         * This function may be called by some random thread, so let's be safe and jump back to ui
         * thread. No OpenGL calls can be done here.
         */
        mActivity.runOnUiThread(mOnFrameAvailableRunnable);
    }

    private int mOrientationAtCaptureCount = 0;
    private int mOrientationDiffCaptureCount = 0;
    public void startCapture() {
        // Reset values so we can do this again.
        Log.i(TAG, "startCapture ");
        if (CameraUtil.isNewWideAngle() && CameraUtil.isWideLowPowerEnable()) {
            mCameraDevice.setPreviewDataCallback(new Handler(mPreviewDataCallbackThread.getLooper()), new BufferedPreviewCallback());
            if (useNewApi()) {
                applySettings();//SPRD:fix bug1078648
            }
        }
        mCancelComputation = false;
        mTimeTaken = System.currentTimeMillis();
        mActivity.getCameraAppUI().setSwipeEnabled(false);
        mCaptureState = CAPTURE_STATE_MOSAIC;
        mUI.onStartCapture();

        mMosaicFrameProcessor.setProgressListener(new MosaicFrameProcessor.ProgressListener() {
            @Override
            public void onProgress(boolean isFinished, float panningRateX, float panningRateY,
                    float progressX, float progressY) {
                if (CameraUtil.isNewWideAngle()) {
                    if (mDeviceOrientation != mDeviceOrientationAtCapture) {
                        mOrientationDiffCaptureCount++;
                    } else {
                        mOrientationAtCaptureCount++;
                    }
                    if (isFinished) {
                        stopCapture(false);
                    } else {
                        mUI.updateCaptureProgress(panningRateX, panningRateY,
                                progressX, progressY,
                                mFastToastThreshold);
                    }
                } else {
                    float accumulatedHorizontalAngle = progressX * mHorizontalViewAngle;
                    float accumulatedVerticalAngle = progressY * mVerticalViewAngle;
                    /* SPRD:fix bug641752 picture is rotate 90 degree @{ */
                    if (mDeviceOrientation != mDeviceOrientationAtCapture) {
                        mOrientationDiffCaptureCount++;
                    } else {
                        mOrientationAtCaptureCount++;
                    }
                    /* @} */
                    if (isFinished
                            || (Math.abs(accumulatedHorizontalAngle) >= MAX_SWEEP_ANGLE)
                            || (Math.abs(accumulatedVerticalAngle) >= MAX_SWEEP_ANGLE)) {
                        stopCapture(false);
                    } else {
                        float panningRateXInDegree = panningRateX * mHorizontalViewAngle;
                        float panningRateYInDegree = panningRateY * mVerticalViewAngle;
                        mUI.updateCaptureProgress(panningRateXInDegree, panningRateYInDegree,
                                accumulatedHorizontalAngle, accumulatedVerticalAngle,
                                PANNING_SPEED_THRESHOLD);
                    }
                }
            }
        });

        mUI.resetCaptureProgress();
        // TODO: calculate the indicator width according to different devices to reflect the actual
        // angle of view of the camera device.
        mUI.setMaxCaptureProgress(MAX_SWEEP_ANGLE);
        if (!CameraUtil.isNewWideAngle()) {
            mUI.showCaptureProgress();
        } else {
            updateFocusMode(false);
        }
        mDeviceOrientationAtCapture = mDeviceOrientation;
        mOrientationAtCaptureCount = 0;
        mOrientationDiffCaptureCount = 0;
        keepScreenOn();
        // TODO: mActivity.getOrientationManager().lockOrientation();
        // mOrientationManager.lockOrientation();
        // mAppController.lockOrientation();
        int degrees = CameraUtil.getDisplayRotation();
        int cameraId = mActivity.getCameraProvider().getFirstBackCameraId();
        Characteristics info =
                mActivity.getCameraProvider().getCharacteristics(cameraId);
        int orientation = info.getPreviewOrientation(degrees);

        // nj dream test 103
        if (mDeviceOrientation == 180) {
            mUI.setProgressOrientation(orientation);
        } else {
            mUI.setProgressOrientation((mDeviceOrientation + orientation) % 360);
        }
    }

    private void stopCapture(boolean aborted) {
        Log.i(TAG, "stopCapture ");
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        if (CameraUtil.isNewWideAngle()) {
            if (CameraUtil.isWideLowPowerEnable()) {
                mCameraDevice.setPreviewDataCallback(null, null);
                if (useNewApi()) {
                    applySettings();
                }
            }
            mCaptureHandler.removeCallbacksAndMessages(null);
            mMosaicFrameProcessor.stopCapture(true);
            mMosaicPreviewRenderer.hideSmallView(true);
            updateFocusMode(true);
        }
        mUI.onStopCapture();
        mAppController.getCameraAppUI().setSwipeEnabled(false);
        mMosaicFrameProcessor.setProgressListener(null);
        /* SPRD:fix bug641752 picture is rotate 90 degree @{ */
        if (mOrientationAtCaptureCount < mOrientationDiffCaptureCount) {
            mDeviceOrientationAtCapture = mDeviceOrientation;
        }
        mOrientationAtCaptureCount = 0;
        mOrientationDiffCaptureCount = 0;
        /* @} */
        /**
         * SPRD: the latest preview image before stopCapture will show first when preview resumes.
         * do not stop preview here @{
         * /
        stopCameraPreview();

        mCameraTexture.setOnFrameAvailableListener(null);
        @} */

        if (!aborted && !mThreadRunning) {
            // SPRD:Add for bug 479120 wideangle module rotate issue
            if (!CameraUtil.isNewWideAngle() && mDeviceOrientation == 0) {
                mUI.showWaitingDialog(mPreparePreviewString);
            }
            // Hide shutter button, shutter icon, etc when waiting for
            // panorama to stitch
            if (!CameraUtil.isNewWideAngle()) {
                runBackgroundThread(new Thread() {
                    @Override
                    public void run() {
                        Log.i(TAG, "runBackgroundThread, begin  generateFinalMosaic(false)");
                        MosaicJpeg jpeg = generateFinalMosaic(false);

                        if (jpeg != null && jpeg.isValid) {
                            Bitmap bitmap = null;
                            bitmap = BitmapFactory.decodeByteArray(jpeg.data, 0, jpeg.data.length);
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                    MSG_LOW_RES_FINAL_MOSAIC_READY, bitmap));
                        } else {
                            mResetToPreviewWithoutSave = true;
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                    MSG_END_DIALOG_RESET_TO_PREVIEW));
                        }
                    }
                });
            } else {
                mMainHandler.sendMessage(mMainHandler.obtainMessage(
                        MSG_LOW_RES_FINAL_MOSAIC_READY, null));
            }
        }
        keepScreenOnAwhile();
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // Do nothing.
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coordinate) {

    }

    @Override
    public void onShutterButtonClick() {
        // If mCameraTexture == null then GL setup is not finished yet.
        // No buttons can be pressed.
        Log.i(TAG, "onShutterButtonClick : mPaused = " + mPaused + " mThreadRunning = "
                + mThreadRunning + " mCameraTexture = " + mCameraTexture);
        if (mPaused || mThreadRunning || mCameraTexture == null) {
            return;
        }
        // Since this button will stay on the screen when capturing, we need to check the state
        // right now.
        boolean isShutterOn = mAppController.isPlaySoundEnable();
        switch (mCaptureState) {
            case CAPTURE_STATE_VIEWFINDER:
                mStartCapture = true;
                /* SPRD: add Storage update before check storageSpaceBytes @{ */
                mActivity.updateStorageSpaceAndHint(null);
                /* @} */
                final long storageSpaceBytes = mActivity.getStorageSpaceBytes();
                if (storageSpaceBytes <= Storage.LOW_STORAGE_THRESHOLD_BYTES
                    || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES) {
                    Log.w(TAG, "Low storage warning: " + storageSpaceBytes);
                    return;
                }
                // mSoundPlayer.play(SoundClips.START_VIDEO_RECORDING);
                if (isShutterOn && mCameraSound != null) {
                    mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
                }
                doSomethingWhenonShutterStateViewFinder();
                startCapture();
                break;
            case CAPTURE_STATE_MOSAIC:
                // mSoundPlayer.play(SoundClips.STOP_VIDEO_RECORDING);
                if (mFrameCount < 1) {
                    CameraUtil.toastHint(mActivity, R.string.time_too_short);//SPRD:fix bug1196291
                    return;
                }
                if (isShutterOn && mCameraSound != null) {
                    mCameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                }
                if (mMainHandler.hasMessages(MSG_RESET_KEY_ENTER)) {
                    mMainHandler.removeMessages(MSG_RESET_KEY_ENTER);
                }
                mMainHandler.sendEmptyMessageDelayed(MSG_RESET_KEY_ENTER, RESET_KEY_ENTER_DELAY);//SPRD:fix bug1279657
                stopCapture(false);
                break;
            default:
                Log.w(TAG, "Unknown capture state: " + mCaptureState);
                break;
        }
    }

    public void reportProgress() {
        mUI.resetSavingProgress();
        Thread t = new Thread() {
            @Override
            public void run() {
                while (mThreadRunning) {
                    final int progress = mMosaicFrameProcessor.reportProgress(
                            true, mCancelComputation);

                    try {
                        synchronized (mWaitObject) {
                            mWaitObject.wait(50);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Panorama reportProgress failed", e);
                    }
                    // Update the progress bar
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.updateSavingProgress(progress);
                        }
                    });
                }
            }
        };
        t.start();
    }

    private int getCaptureOrientation() {
        // The panorama image returned from the library is oriented based on the
        // natural orientation of a camera. We need to set an orientation for the image
        // in its EXIF header, so the image can be displayed correctly.
        // The orientation is calculated from compensating the
        // device orientation at capture and the camera orientation respective to
        // the natural orientation of the device.
        int orientation;
        if (mUsingFrontCamera) {
            // mCameraOrientation is negative with respect to the front facing camera.
            // See document of android.hardware.Camera.Parameters.setRotation.
            orientation = (mDeviceOrientationAtCapture - mCameraOrientation + 360) % 360;
        } else {
            orientation = (mDeviceOrientationAtCapture + mCameraOrientation) % 360;
        }
        return orientation;
    }

    /**
     * The orientation of the camera image. The value is the angle that the camera image needs to be
     * rotated clockwise so it shows correctly on the display in its natural orientation. It should
     * be 0, 90, 180, or 270.
     */
    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    private void createFinalMosaic() {
        mSavingPanorama = true;
        mPartialWakeLock.acquire();
        MosaicJpeg jpeg;
        try {
            jpeg = generateFinalMosaic(true);
        } finally {
            mPartialWakeLock.release();
        }

        if (jpeg == null) { // Cancelled by user.
            mMainHandler.sendEmptyMessage(MSG_END_DIALOG_RESET_TO_PREVIEW);
        } else if (!jpeg.isValid) { // Error when generating mosaic.
            mMainHandler.sendEmptyMessage(MSG_GENERATE_FINAL_MOSAIC_ERROR);
        } else {
            int orientation = getCaptureOrientation();
            final Uri uri = savePanorama(jpeg.data, jpeg.width, jpeg.height, orientation);
            if (uri != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.notifyNewMedia(uri);
                    }
                });
            }
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_END_DIALOG_RESET_TO_PREVIEW));
        }
        mSavingPanorama = false;
    }

    public void saveHighResMosaic() {
        hideSideAndSlideWhenSaveHighResMosaic();
        Log.i(TAG, " isNewWideAngle = " + CameraUtil.isNewWideAngle() + " mCaptureHandler = " + mCaptureHandler);
        if (CameraUtil.isNewWideAngle() && mCaptureHandler != null) {
            mCaptureHandler.post(new Runnable() {
                @Override
                public void run() {
                    mThreadRunning = true;
                    createFinalMosaic();
                }
            });
        } else {
            runBackgroundThread(new Thread() {
                @Override
                public void run() {
                    createFinalMosaic();
                }
            });
            reportProgress();
        }
    }

    private void runBackgroundThread(Thread thread) {
        mThreadRunning = true;
        thread.start();
    }

    private void onBackgroundThreadFinished() {
        mThreadRunning = false;
        mUI.dismissAllDialogs();
    }

    private void cancelHighResComputation() {
        mCancelComputation = true;
        synchronized (mWaitObject) {
            mWaitObject.notify();
        }
    }

    // This function will be called upon the first camera frame is available.
    private void reset() {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        mAppController.getCameraAppUI().setSwipeEnabled(true);
        // mOrientationManager.unlockOrientation();
        // mAppController.unlockOrientation();
        if (CameraUtil.isNewWideAngle() && mMosaicPreviewRenderer != null) {
            mMosaicPreviewRenderer.hideSmallView(false);
        }
        mUI.reset();
        // Orientation change will trigger onLayoutChange->configMosaicPreview->
        // resetToPreview. Do not show the capture UI in film strip.
        if (mPreviewFocused) {
            mUI.showPreviewUI();
            showSomethingWhenonShutterStateMosaic();
        }
        mMosaicFrameProcessor.reset();
    }

    private void resetToPreviewIfPossible() {
        /**
         * SPRD: fixbug 261883, Fix skipped hide review layout calls before getSurfaceTexture merge
         * from 4.4.1_ri orig @{
         */
        Log.i(TAG, "resetToPreviewIfPossible mPaused = " + mPaused
                + " mThreadRunning = " + mThreadRunning
                + " mMosaicFrameProcessorInitialized = "
                + mMosaicFrameProcessorInitialized
                + " mMosaicPreviewConfigured = " + mMosaicPreviewConfigured);
        if (mThreadRunning) {
            return;
        }
        reset();
        /** @} */
        if (!mMosaicFrameProcessorInitialized
                || mUI.getSurfaceTexture() == null
                || !mMosaicPreviewConfigured) {
            return;
        }
        if (!mPaused && mCameraState != PREVIEW_ACTIVE) {
            startCameraPreview();
        }
    }

    private void showFinalMosaic(Bitmap bitmap) {
        mUI.showFinalMosaic(bitmap, getCaptureOrientation());
    }

    private Uri savePanorama(byte[] jpegData, int width, int height, int orientation) {
        if (jpegData != null) {
            String filename = PanoUtil.createName(
                    mActivity.getResources().getString(R.string.pano_file_name_format), mTimeTaken);
            StorageUtil storageUtil = StorageUtil.getInstance();
            ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
            String filepath = storageUtil.generateFilePath(filename,
                    FilmstripItemData.MIME_TYPE_JPEG);
            Uri uri = null;
            if (filepath == null) {
                return null;
            }
            uri = storageUtil.getCurrentAccessUri(filepath);//CID 747193
            if (CameraUtil.isNewWideAngle()) {
                orientation = 0;
            }
            Location loc = mActivity.getLocationManager().getCurrentLocation();
            ExifInterface exif = new ExifInterface();
            /**
             * SPRD Bug 1149886
             *Original process: firstly, write file  then insert database
             * we will change to firstly insert database with "is_pending" equals 0, then write file
             * if file is complete, will update "is_pending" to 1, else will
             * delete item from database
             */
            Uri insertUri = Storage.addImageToMediaStore(mContentResolver, filename, mTimeTaken, loc,
                    orientation,
                    0, filepath, width, height, FilmstripItemData.MIME_TYPE_JPEG, null, 0, 1);
            try {
                exif.readExif(jpegData);
                exif.addGpsDateTimeStampTag(mTimeTaken);
                exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, mTimeTaken,
                        TimeZone.getDefault());
                exif.setTag(exif.buildTag(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.getOrientationValueForRotation(orientation)));
                writeLocation(loc, exif);
                if (storageUtil.isInternalPath(filepath)) {
                    exif.writeExif(jpegData, filepath);
                } else {
                    Log.i(TAG,"savePanorama External writeExif");
                    String[] pathName = filepath.split("/");
                    String picName = pathName[pathName.length -1];
                    ParcelFileDescriptor pfd = externalStorageUtil.createParcelFd(mContentResolver,uri,picName,"image/*");
                    if (pfd != null) {
                        FileOutputStream outputStream = externalStorageUtil.createOutputStream(pfd);
                        exif.writeExif(jpegData,outputStream);
                        outputStream.close();
                        pfd.close();
                    } else {
                        mContentResolver.delete(insertUri, null, null);
                        return null;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Cannot set exif for " + filepath, e);
                // Storage.writeFile(filepath, jpegData);
            }
            int jpegLength = (int) (new File(filepath).length());
            if (jpegLength > 0) {
                Storage.updateImageToMediaStore(mContentResolver, insertUri, filename, mTimeTaken, loc,
                        orientation,
                        jpegLength, filepath, width, height, FilmstripItemData.MIME_TYPE_JPEG, null, 0, 0);
                return insertUri;
            } else {
                mContentResolver.delete(insertUri, null, null);
                Log.e(TAG,"File is not complete and delete item in media store");
                return null;
            }
        }
        return null;
    }

    private static void writeLocation(Location location, ExifInterface exif) {
        if (location == null) {
            return;
        }
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_PROCESSING_METHOD, location.getProvider()));
    }

    private void clearMosaicFrameProcessorIfNeeded() {
        Log.i(TAG, "clearMosaicFrameProcessorIfNeeded mPaused = " + mPaused
                + " mThreadRunning = " + mThreadRunning
                + " mMosaicFrameProcessorInitialized = "
                + mMosaicFrameProcessorInitialized);
        if (!mPaused || mThreadRunning)
            return;
        // Only clear the processor if it is initialized by this activity
        // instance. Other activity instances may be using it.
        if (mMosaicFrameProcessorInitialized) {
            mMosaicFrameProcessor.clear();
            mMosaicFrameProcessorInitialized = false;
        }
    }

    private void initMosaicFrameProcessorIfNeeded() {
        Log.i(TAG, "initMosaicFrameProcessorIfNeeded mPaused = " + mPaused
                + " mThreadRunning = " + mThreadRunning
                + " mCameraCallbackWidth = " + mCameraCallbackWidth
                + " mCameraCallbackHeight = " + mCameraCallbackHeight);
        if (mPaused || mThreadRunning || mMosaicFrameProcessorInitialized) {// SPRD Fix bug 466627
            return;
        }
        /*** SPRD bug289431 ***/
        if ((mCameraCallbackWidth == 0) || (mCameraCallbackHeight == 0)) {
            return;
        }
        /*** SPRD bug289431 ***/

        Size realsize = CameraUtil.getDefaultDisplayRealSize();
        int screenWidth = realsize.getWidth();
        int screenHeight = realsize.getHeight();
        int previewShortEdge = (mCameraCallbackWidth > mCameraCallbackHeight) ? mCameraCallbackHeight : mCameraCallbackWidth;
        int uiShortEdge = (screenHeight > screenWidth) ? screenWidth : screenHeight;
        final float ratioPreviewToUI = previewShortEdge * 1.0f / uiShortEdge;
        int previewLongEdge = (mCameraCallbackWidth < mCameraCallbackHeight) ? mCameraCallbackHeight : mCameraCallbackWidth;
        int uiLongEdge = (screenHeight < screenWidth) ? screenWidth : screenHeight;
        final float ratioPreviewToUILong = previewLongEdge * 1.0f / uiLongEdge;
        Log.i(TAG, "ratioPreviewToUI 11111111111" + ratioPreviewToUI + " ratioPreviewToUILong = " + ratioPreviewToUILong);

        mMosaicFrameProcessor.initialize(
                mCameraCallbackWidth, mCameraCallbackHeight, 0, ratioPreviewToUILong, ratioPreviewToUI);
        mMosaicFrameProcessorInitialized = true;
    }

    @Override
    public void pause() {
        mPaused = true;
        mHasEndSaveHigh = false;

        // mOrientationManager.pause();
        //mOrientationEventListener.disable();
        if (mCameraDevice == null) {
            // Camera open failed. Nothing should be done here.
            return;
        }
        // Stop the capturing first.
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
            stopCapture(true);
            reset();
        }
        mMainHandler.removeMessages(MSG_RESET_TO_PREVIEW);
        mDataModuleCurrent.removeListener(mActivity,this);
        // SPRD: Disable preview cover for preformance.
        // mUI.showPreviewCover();
        if (!CameraUtil.isNewWideAngle())//SPRD:fix bug957942
        mUI.showPreviewCover(); // SPRD: FixBug 302158.
        stopCameraPreview();
        releaseCamera();
        mMosaicFrameProcessor.setHandler(null);
        synchronized (mRendererLock) {
            mCameraTexture = null;

            // The preview renderer might not have a chance to be initialized
            // before onPause().
            if (mMosaicPreviewRenderer != null) {
                mMosaicPreviewRenderer.release();
                mMosaicPreviewRenderer = null;
            }
        }

        clearMosaicFrameProcessorIfNeeded();
        if (mWaitProcessorTask != null) {
            mWaitProcessorTask.cancel(true);
            mWaitProcessorTask = null;
        }
        resetScreenOn();
        // use SerialExecutor to sync
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (mCameraSound != null) {
                    mCameraSound.release();
                    mCameraSound = null;
                }
            }
        });
        System.gc();
        if (mAppController.getOrientationManager() != null)
            mAppController.getOrientationManager().removeOnOrientationChangeListener(this);
        mAppController.getCameraAppUI().prepareForNextRoundOfSurfaceUpdate();//SPRD:fix bug735766
        mActivity.getModuleLayoutRoot().findViewById(R.id.settings_button)
                .setVisibility(View.VISIBLE);
        //mActivity.getCameraAppUI().resumeTextureViewRendering();//SPRD:fix bug616230 not need this operation any more

        // SPRD:Add for bug 479120 wideangle module rotate issue
        // SPRD Bug:519334 Refactor Rotation UI of Camera.
        // setScreenOrientationAuto();
    }

    @Override
    public void resume() {
        mPaused = false;
        //mOrientationEventListener.enable();

        mCaptureState = CAPTURE_STATE_VIEWFINDER;

        mDataModuleCurrent.addListener(mActivity,this);
        mMosaicFrameProcessor.setHandler(mMainHandler);
        hideSetting();
        if (mThreadRunning) {
            mActivity.getCameraAppUI().hideModeCover();
        }
        requestCameraOpen();
        // Set up sound playback for shutter button
        // mSoundPlayer = SoundClips.getPlayer(mActivity);

        keepScreenOnAwhile();
        mActivity.getCameraAppUI().showCurrentModuleTip(SettingsScopeNamespaces.DREAM_PANORAMA);
        mUI.hidePreviewCover();

        // mOrientationManager.resume();

        loadSoundAync();

        // SPRD:Add for bug 479120 wideangle module rotate issue
        // SPRD Bug:519334 Refactor Rotation UI of Camera.
        // setScreenOrientation();
        //Sprd:Fix bug771726
        mActivity.setSurfaceVisibility(false);
        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        orientationManager.addOnOrientationChangeListener(this);
        mDeviceOrientation = orientationManager.getDeviceOrientation().getDegrees();
        mUI.setDisplayOrientation(mDeviceOrientation);
    }

    private void loadSoundAync() {
        // use SerialExecutor to sync
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (mCameraSound == null) {
                    mCameraSound = new MediaActionSound();
                    // Not required, but reduces latency when playback is requested later.
                    mCameraSound.load(MediaActionSound.START_VIDEO_RECORDING);
                    mCameraSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
                }
            }
        });
    }

    private void hideSetting() {
        mActivity.getModuleLayoutRoot().findViewById(R.id.settings_button)
                .setVisibility(View.INVISIBLE);
    }

    /**
     * Generate the final mosaic image.
     *
     * @param highRes flag to indicate whether we want to get a high-res version.
     * @return a MosaicJpeg with its isValid flag set to true if successful; null if the generation
     *         process is cancelled; and a MosaicJpeg with its isValid flag set to false if there is
     *         an error in generating the final mosaic.
     */
    public MosaicJpeg generateFinalMosaic(boolean highRes) {
        int mosaicReturnCode = mMosaicFrameProcessor.createMosaic(highRes);
        if (mosaicReturnCode == Mosaic.MOSAIC_RET_CANCELLED) {
            return null;
        } else if (mosaicReturnCode == Mosaic.MOSAIC_RET_ERROR) {
            return new MosaicJpeg();
        }

        byte[] imageData = mMosaicFrameProcessor.getFinalMosaicNV21();
        if (imageData == null) {
            Log.e(TAG, "getFinalMosaicNV21() returned null.");
            return new MosaicJpeg();
        }

        int len = imageData.length - 8;
        int width = (imageData[len + 0] << 24) + ((imageData[len + 1] & 0xFF) << 16)
                + ((imageData[len + 2] & 0xFF) << 8) + (imageData[len + 3] & 0xFF);
        int height = (imageData[len + 4] << 24) + ((imageData[len + 5] & 0xFF) << 16)
                + ((imageData[len + 6] & 0xFF) << 8) + (imageData[len + 7] & 0xFF);
        Log.d(TAG, "ImLength = " + (len) + ", W = " + width + ", H = " + height);

        if (width <= 0 || height <= 0) {
            // TODO: pop up an error message indicating that the final result is not generated.
            Log.e(TAG, "width|height <= 0!!, len = " + (len) + ", W = " + width + ", H = " +
                    height);
            return new MosaicJpeg();
        }

        if (!highRes && width == 208 && height == 112) { // SPRD:Fix bug 455384/616701
            return null;
        }

        YuvImage yuvimage = new YuvImage(imageData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        try {
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception in storing final mosaic", e);
            return new MosaicJpeg();
        }
        return new MosaicJpeg(out.toByteArray(), width, height);
    }

    int frameCount = 0;
    final static int MAX_FRAME_COUNT = 2;
    private HandlerThread mPreviewDataCallbackThread = null;
    private HandlerThread mCaptureProcessThread = null;
    private Handler mCaptureHandler;

    private void startCameraPreview() {
        if (mCameraDevice == null) {
            // Camera open failed. Return.
            return;
        }

        if (mUI.getSurfaceTexture() == null) {
            // UI is not ready.
            return;
        }

        // This works around a driver issue. startPreview may fail if
        // stopPreview/setPreviewTexture/startPreview are called several times
        // in a row. mCameraTexture can be null after pressing home during
        // mosaic generation and coming back. Preview will be started later in
        // onLayoutChange->configMosaicPreview. This also reduces the latency.
        synchronized (mRendererLock) {
            if (mCameraTexture == null)
                return;

            // If we're previewing already, stop the preview first (this will
            // blank the screen).
            if (mCameraState != PREVIEW_STOPPED)
                stopCameraPreview();

            // Set the display orientation to 0, so that the underlying mosaic
            // library can always get undistorted mCameraPreviewWidth x mCameraPreviewHeight
            // image data from SurfaceTexture.
            mCameraDevice.setDisplayOrientation(90, false);

            if (CameraUtil.isNewWideAngle()) {
                mUI.setPreviewArea(mPreviewUIWidth, mPreviewUIHeight);
                updateCaptureDirector(false);
                frameCount = 0;
            }
            mCameraTexture.setOnFrameAvailableListener(this);
            /* SPRD:fix bug981172 callback must set before configstream use api2 @{ */
            if (CameraUtil.isNewWideAngle()) {
                if (!CameraUtil.isWideLowPowerEnable()) {//SPRD:fix bug777693
                    mCameraDevice.setPreviewDataCallback(new Handler(mPreviewDataCallbackThread.getLooper()), new BufferedPreviewCallback());
                } else if (useNewApi()) {
                    mCameraDevice.setPreviewCallbackConfig(true);//SPRD:fix bug1078648
                }
            }
            /* @} */
            mCameraDevice.setPreviewTexture(mCameraTexture);
        }

        mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()),
                new CameraAgent.CameraStartPreviewCallback() {
                    @Override
                    public void onPreviewStarted() {
                        // SPRD: Fix bug 545514 post message to handle the onPreviewStarted event so as to
                        //solve bug that previous preview picture from last module always displays first, then
                        //normal preview of wide module.
                        WideAnglePanoramaModule.this.onPreviewStarted();
                        if (!CameraUtil.isNewWideAngle()) {
                            mMainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!mPaused) {
                                        mActivity.getCameraAppUI().onSurfaceTextureUpdated();
                                    }
                                }
                            }, 300);
                        }
                    }
                });
        mPreviewing = true;
        mCameraState = PREVIEW_ACTIVE;
    }

    private int mFrameCount = 0;
    private static final int ALGORITHM_FRAME_GAP = CameraUtil.getWideFrameGapCount();//SPRD:fix bug794337/757675 use for control frmae rate for lib
    private final class BufferedPreviewCallback implements CameraAgent.CameraPreviewDataCallback {
        @Override
        public void onPreviewFrame(final byte[] data, CameraProxy camera) {
            if (mCameraDevice == null || mPaused || mCaptureState == CAPTURE_STATE_VIEWFINDER) {
                Log.v(TAG, "mCameraDevice==" + mCameraDevice + " mCaptureState = " + mCaptureState);
                mFrameCount = 0;
                return;
            }
            Log.i(TAG, " onPreviewFrame");
            synchronized (mRendererLock) {
                if (mMosaicPreviewRenderer == null) {
                    return;
                }
            }
            if (mFrameCount % ALGORITHM_FRAME_GAP == 0) {
                mCaptureHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, " post showAndCapture mCaptureState = " + mCaptureState);
                        if (mCaptureState == CAPTURE_STATE_MOSAIC && !mPaused) {
                            mMosaicFrameProcessor.processFrame(data);
                        }
                    }
                });
                //mMosaicFrameProcessor.processFrame(data);
            }
            mFrameCount++;
        }
    }

    private void onPreviewStarted() {
        mAppController.onPreviewStarted();
        //mActivity.getCameraAppUI().pauseTextureViewRendering();// SPRD:fix bug 474828,fix bug616230 not need this operation any more
    }

    /* SPRD: Fix bug 681215 optimization about that DV change to DC is slow @{ */
    @Override
    public void stopPreview() {
        stopCameraPreview();
    }
    /* @} */

    private void stopCameraPreview() {
        CameraProxy device = mCameraDevice;//SPRD:fix bug926761
        if (device != null && mCameraState != PREVIEW_STOPPED) {
            device.stopPreview();
            if (CameraUtil.isNewWideAngle()) {
                device.setPreviewDataCallback(null, null);
                device.setPreviewCallbackConfig(false);
            }
        }
        mPreviewing = false;
        mCameraState = PREVIEW_STOPPED;
    }

    @Override
    public boolean onBackPressed() {
        // If panorama is generating low res or high res mosaic, ignore back
        // key. So the activity will not be destroyed.
        /* SPRD: fix bug250329 {@ */
        // if (mThreadRunning) return true;
        if (mThreadRunning) {
            /**
             * SPRD: fixbug 312774 In Render processing,appear the ModuleSwitchView and
             * ShutterButton @{
             */
            if (mPaused
                    || (!mMosaicFrameProcessor.isMosaicMemoryAllocated() && mCameraTexture == null))
                return true;
            /** @} fixbug 312774 End */
            cancelHighResComputation();
            return true;
        }
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
            stopCapture(true);
            resetToPreviewIfPossible();
            return true;
        }
        /* fix bug250329 end @} */
        return false;
    }

    private void resetScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private class WaitProcessorTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mMosaicFrameProcessor) {
                while (!isCancelled() && mMosaicFrameProcessor.isMosaicMemoryAllocated()) {
                    try {
                        mMosaicFrameProcessor.wait();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            // mActivity.updateStorageSpace();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mWaitProcessorTask = null;
            mUI.dismissAllDialogs();
            // TODO (shkong): mGLRootView.setVisibility(View.VISIBLE);
            initMosaicFrameProcessorIfNeeded();
            Point size = mUI.getPreviewAreaSize();
            mPreviewUIWidth = size.x;
            mPreviewUIHeight = size.y;
            configMosaicPreview();
            resetToPreviewIfPossible();
            mActivity.updateStorageHint(mActivity.getStorageSpaceBytes());
        }
    }

    @Override
    public void cancelHighResStitching() {
        /**
         * SPRD: fixbug 312774 In Render processing,appear the ModuleSwitchView and ShutterButton @{
         */
        if (mPaused || (!mThreadRunning && mCameraTexture == null))
            return;
        /** @} fixbug 312774 End */
        cancelHighResComputation();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
            // SPRD: Add For bug 529146
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
                /* SPRD:Bug 535058 New feature: volume @{ */
                int volumeStatus = getVolumeControlStatus(mActivity);
                if (volumeStatus == 1 || keyCode == KeyEvent.KEYCODE_CAMERA) {
                    return true;
                } else if (volumeStatus == 2) {
                    return true;
                } else if (volumeStatus == 3) {
                    return false;
                }
                return false;
        } /* }@ */
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /* SPRD:Bug 535058 New feature: volume @{ */
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
                /*
                 * SPRD:fix bug518054 ModeListView is appear when begin to capture using volume
                 * key@{
                 */
                mActivity.getCameraAppUI().hideModeList();
                /* }@ */
                int volumeStatus = getVolumeControlStatus(mActivity);
                if (volumeStatus == Keys.shutter || keyCode == KeyEvent.KEYCODE_CAMERA) {
                    if (mActivity.hasKeyEventEnter() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                        if (mMainHandler.hasMessages(MSG_RESET_KEY_ENTER)) {
                            mMainHandler.removeMessages(MSG_RESET_KEY_ENTER);
                        }
                        mActivity.resetHasKeyEventEnter();
                        return true;
                    }
                    onShutterButtonClick();
                    return true;
                } else if (volumeStatus == Keys.zoom) {
                    CameraUtil.toastHint(mActivity, R.string.current_module_does_not_support_zoom_change);//SPRD:fix bug671302 change toast hint method
                    return true;
                } else if (volumeStatus == Keys.volume) {
                    return false;
                }
                return false;
        }
        return false;
    } /* }@ */

    /* SPRD: fixbug 267736 add broadcast handled while broadcast receive @{ */
    // CID 109260 : UrF: Unread field (FB.URF_UNREAD_FIELD)
    // private BroadcastReceiver mReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                String externalPath = null;
                if (StorageUtilProxy.getExternalStoragePath() != null) {
                    externalPath = StorageUtilProxy.getExternalStoragePath().getAbsolutePath();
                }
                StorageUtil util = StorageUtil.getInstance();
                String storagePath = util.getFileDir();
                if (storagePath == null || externalPath == null
                        || !storagePath.contains(externalPath))
                    return;
                if (mThreadRunning) {
                    if (mPaused || mCameraTexture == null)
                        return;
                    CameraUtil.toastHint(mActivity,R.string.sdcard_remove);
                    cancelHighResComputation();
                    return;
                }
                if (mCaptureState == CAPTURE_STATE_MOSAIC) {
                    CameraUtil.toastHint(mActivity,R.string.sdcard_remove);
                    stopCapture(true);
                    resetToPreviewIfPossible();
                    return;
                }
            }
        }
    }

    /* end @} */

    /* SPRD:Add for bug 479120 wideangle module rotate issue @{ */
    protected void setScreenOrientation() {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    protected void setScreenOrientationAuto() {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
    /* @} */

    public WideAnglePanoramaUI createUI(CameraActivity activity) {
        return new WideAnglePanoramaUI(activity, this, activity.getCameraAppUI().getModuleView());
    }

    /*Dream Camera test ui check 38 and 39 */
    protected void doSomethingWhenonShutterStateViewFinder() {

    }

    /*Dream Camera test ui check 38 */
    protected void showSomethingWhenonShutterStateMosaic() {

    }
    /*Dream Camera test ui check 43 */
    protected void hideSideAndSlideWhenSaveHighResMosaic() {

    }

    //Bug#533869 add the feature of volume
    protected int getVolumeControlStatus(CameraActivity mActivity) {
        return mActivity.getVolumeControlStatus();
    }

  //SPRD:fix bug 613799 save the panorama exception
    @Override
    public boolean isSavingPanorama(){
        return mSavingPanorama;
    }

    /* SPRD:fix bug948703 add port capture for panorama @{ */
    private boolean mProgressLand = true;
    private void updateCaptureDirector(boolean manul) {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_DIRECTOR)){
            return;
        }
        String director = mDataModuleCurrent.getString(Keys.KEY_DIRECTOR);
        Log.d(TAG, "updateCaptureDirector = " + director + " manul = " + manul);
        if (director == null || mUI == null) {
            return;
        }

        mUI.switchDirector(mProgressLand = "land".equals(director), manul);
    }

    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keyList) {
        for (Map.Entry<String, String> entry: keyList.entrySet()) {
            String key = entry.getKey();
            Log.e(TAG,"onSettingChanged key = " + key + " value = "+ entry.getValue());
            switch (key) {
                case Keys.KEY_DIRECTOR:
                    updateCaptureDirector(true);
                    break;
            }
        }
    }

    public final ButtonManager.ButtonCallback mCameraCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            // At the time this callback is fired, the camera id
            // has be set to the desired camera.

            if (mPaused
                    || mAppController.getCameraProvider().waitingForCamera()) {
                return;
            }

            ButtonManager buttonManager = mActivity.getButtonManager();
            buttonManager.disableCameraButtonAndBlock();

            Log.d(TAG, "bbbb Start to switch camera. cameraId=" + state);
            // We need to keep a preview frame for the animation before
            // releasing the camera. This will trigger
            // onPreviewTextureCopied.
            // TODO: Need to animate the camera switch
            switchCamera();
        }
    };

    protected void switchCamera() {
        freezeScreen(CameraUtil.isFreezeBlurEnable(),
                CameraUtil.isSwitchAnimationEnable());
        mMainHandler.post(new Runnable() {

            @Override
            public void run() {
                if (!mPaused) {
                    mActivity.switchFrontAndBackMode();
                }
            }
        });
    }
    /* @} */

    @Override
    public void freezeScreen(boolean needBlur, boolean needSwitch) {
        Bitmap freezeBitmap = null;
        if (CameraUtil.isNewWideAngle()) {
            mUI.setSmallPreviewVisible(false);
        }
        freezeBitmap = getPreviewBitmap();

        if (needBlur || needSwitch) {
            freezeBitmap = CameraUtil.blurBitmap(CameraUtil.computeScale(freezeBitmap, 0.2f), (Context)mActivity);
        }

        if (needSwitch) {
            mAppController.getCameraAppUI().startSwitchAnimation(freezeBitmap);
        }

        mAppController.freezeScreenUntilPreviewReady(freezeBitmap, new RectF(0, 0, mPreviewUIWidth, mPreviewUIHeight));
    }

    private Bitmap getPreviewBitmap() {
        return mUI.getPreviewBitmap(mPreviewUIWidth, mPreviewUIHeight);
    }

    @Override
    public int getModuleTpye() {
        return WIDEANGLE_MODULE;
    }

    @Override
    public boolean isShutterClicked(){
        return mCaptureState == CAPTURE_STATE_MOSAIC;
    }

    @Override
    public boolean getCameraPreviewState() {
        return mPreviewing;
    }

    //SPRD:Fix bug1023504
    @Override
    public boolean isCameraAvailable(){
        return mCameraDevice != null;
    }
    @Override
    public void updatePortraitBackgroundReplacementType(){}
}
