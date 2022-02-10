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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import com.android.camera.data.FilmstripItemUtils;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera2.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.MotionEvent;
import com.dream.camera.ui.AELockPanel;
import com.dream.camera.ui.AELockSeekBar;

import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.MemoryManager;
import com.android.camera.app.MemoryManager.MemoryListener;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.stats.profiler.Profile;
import com.android.camera.stats.profiler.Profilers;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.AndroidServices;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.MakeupController;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataStructSetting;
import com.dream.camera.util.Counter;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.settings.DataConfig;
import com.google.common.logging.eventprotos;
import com.sprd.camera.storagepath.ExternalStorageUtil;
import com.sprd.camera.storagepath.MultiStorage;
import com.sprd.camera.storagepath.StorageUtil;
import com.sprd.camera.storagepath.StorageUtilProxy;
import com.dream.camera.util.DreamProxy;

import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.view.SurfaceHolder;
import android.media.MediaMetadataRetriever;
import android.media.AudioSystem;

import com.android.camera.util.XmpBuilder;
import com.android.camera.util.XmpConstants;
import com.android.camera.util.XmpUtil;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.dream.camera.DreamModule;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.view.OrientationEventListener;
import com.android.camera.Storage;
import android.telephony.PreciseCallState;
import android.graphics.Rect;
import java.lang.reflect.Method;


public class VideoModule extends CameraModule implements
        FocusOverlayManager.Listener, MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener, MemoryListener, OnSeekBarChangeListener,
        OrientationManager.OnOrientationChangeListener, VideoController,
        CameraAgent.CameraStartVideoCallback,
        CameraAgent.CameraAutoChasingTraceRegionCallback,
        DreamSettingChangeListener {

    private static final Log.Tag TAG = new Log.Tag("VideoModule");
    // Messages defined for the UI thread handler.
    private static final int MSG_CHECK_DISPLAY_ROTATION = 4;
    private static final int MSG_UPDATE_RECORD_TIME = 5;
    private static final int MSG_ENABLE_SHUTTER_BUTTON = 6;
    private static final int MSG_SWITCH_CAMERA = 8;
    private static final int MSG_SWITCH_CAMERA_START_ANIMATION = 9;
    private static final int MSG_ENABLE_SHUTTER_PAUSE_BUTTON = 10;
    private static final int MSG_CAMERA_VIDEO_START = 11;
    private static final long CAMERA_VIDEO_START_TIMEOUT = 600L;//
    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms
    private static final long ENABLE_SHUTTER_PAUSE_BUTTON_TIMEOUT = 3000L;

    private static final long KEY_CLICK_INTERVAL_TIMEOUT = 400L;

    private static final int MAX_VIDEO_DURATION_FOR_CIF = 13*60*60*1000;
    private final String TARGET_CAMERA_UNFREEZE_TIME = "persist.sys.cam.unfreeze_time";
    private final int UNFREEZE_TIME = android.os.SystemProperties.getInt(
            TARGET_CAMERA_UNFREEZE_TIME, 400);
    /**
     * An unpublished intent flag requesting to start recording straight away
     * and return as soon as recording is stopped. TODO: consider publishing by
     * moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    /*
     * SPRD Bug:474704 Feature:Video Recording Pause. @{
     */
    private static final int RECORD_LIMIT_TIME = 3000; // 3s
    // SPRD: Fix bug 540246 recording and music work together after we end call
    private final String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
    private Handler mHandler;

    // module fields
    protected CameraActivity mActivity;
    private boolean mIsPhotoUpdate = false;
    private final MediaSaver.OnMediaSavedListener mOnPhotoSavedListener = new MediaSaver.OnMediaSavedListener() {
        @Override
        public void onMediaSaved(Uri uri, String title) {
            if (uri != null) {
                mIsPhotoUpdate= true;
                mActivity.notifyNewMedia(uri);
            }
        }
    };
    protected AppController mAppController;
    private boolean mPreviewing = false; // True if preview is started.
    private boolean mPaused;
    // if, during and intent capture, the activity is paused (e.g. when app
    // switching or reviewing a
    // shot video), we don't want the bottom bar intent ui to reset to the
    // capture button
    private boolean mDontResetIntentUiOnResume;
    protected int mCameraId;
    protected CameraSettings mCameraSettings;
    protected CameraCapabilities mCameraCapabilities;
    private HardwareSpec mHardwareSpec;
    private boolean mIsInReviewMode;
    private boolean mSnapshotInProgress = false;
    // Preference must be read before starting preview. We check this before
    // starting
    // preview.
    private boolean mPreferenceRead;
    private boolean mIsVideoCaptureIntent;
    private boolean mQuickCapture;
    private MediaRecorder mMediaRecorder;
    /** Manager used to mute sounds and vibrations during video recording. */
    private AudioManager mAudioManager;
    /*
     * The ringer mode that was set when video recording started. We use this to
     * reset the mode once video recording has stopped.
     */
    private int mOriginalRingerMode;
    private boolean mSwitchingCamera;
    protected boolean mMediaRecorderRecording = false;
    private boolean mMediaRecorderPrepareing = false;
    private long mRecordingStartTime;
    private boolean mRecordingTimeCountsDown = false;
    private long mOnResumeTime;
    // The video file that the hardware camera is about to record into
    // (or is recording into.
    private String mVideoFilename;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private ParcelFileDescriptor mExternalVideoFD;
    // The video file that has already been recorded, and that is being
    // examined by the user.
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private final View.OnClickListener mDoneCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onReviewDoneClicked(v);
        }
    };
    private boolean mCurrentVideoUriFromMediaSaved;
    private ContentValues mCurrentVideoValues;
    protected CamcorderProfile mProfile;
    private final View.OnClickListener mReviewCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onReviewPlayClicked(v);
        }
    };
    // The video duration limit. 0 means no limit.
    private int mMaxVideoDurationInMs;
    // The display rotation in degrees. This is only valid when mPreviewing is
    // true.
    private int mDisplayRotation;
    private int mCameraDisplayOrientation;
    protected int mDesiredPreviewWidth;
    protected int mDesiredPreviewHeight;
    private ContentResolver mContentResolver;
    protected Point desiredPreviewSize = null;
    private final View.OnClickListener mCancelCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onReviewCancelClicked(v);
        }
    };
    private LocationManager mLocationManager;
    private int mPendingSwitchCameraId;
    protected VideoUI mUI;

    protected CameraProxy mCameraDevice;

    private float mZoomValue; // The current zoom ratio.

    private int mTimeLapseMultiple;// SPRD: for bug 509708 add
                                                    // time lapse
    private boolean mCaptureTimeLapse;// SPRD: for bug 509708 add time lapse

    // Add for dream settings.
    protected DataModuleBasic mDataModule;
    protected DataModuleBasic mDataModuleCurrent;
    private DataModuleBasic mTempModule;

    private boolean played = false;

    private long mLastReceiveEventTime = 0;

    // SPRD: Fix bug 650297 that video thumbnail is showed slowly
    private int mSnapshotCount = 0;

    // SPRD: add for mutext between float window and camera
    public static final String KEY_DISABLE_FLOAT_WINDOW = "disable-float-window";

    // SPRD: optimize bug 699216 that surface shows too slowly
    private Handler mAsyncHandler;
    private OrientationEventListener mOrientationEventListener;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetic;
    private boolean mHasGravity;
    private float[] mGravity = new float[3];
    private static final float mSensorAlpha = 0.8f;
    private boolean mHasPitchAngle;
    private double mPitchAngle;
    private boolean mHasLevelAngle;
    private double mNaturalLevelAngle;
    private int mCurrentOrientation;
    private boolean mHasGeomagnetic;
    private float[] mGeomagnetic = new float[3];
    private final float [] mDeviceRotation = new float[9];
    private final float [] mDeviceInclination = new float[9];
    private final float [] mCameraRotation = new float[9];
    private boolean mHasGeoDirection;
    private final float [] mNewGeoDirection = new float[3];
    private final float [] mGeoDirection = new float[3];
    private double mLevelAngle;
    private double mOrigLevelAngle;
    private final MediaSaver.OnMediaSavedListener mOnVideoSavedListener = new MediaSaver.OnMediaSavedListener() {
        @Override
        public void onMediaSaved(Uri uri, String title) {
            /*
             * SPRD:fix bug523146 Airtel, Shark L shows Camera force close@{ if
             * (uri != null) {
             */
            if (uri != null && mAppController.getCameraProvider() != null) {
            /* @} */
                mCurrentVideoUri = uri;
                mCurrentVideoUriFromMediaSaved = true;
                onVideoSaved();
                mIsPhotoUpdate = false;
                mActivity.notifyNewMedia(uri);
            }
        }
    };

    public final ButtonManager.ButtonCallback mFlashCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            if (mPaused) {
                return;
            }
            // Update flash parameters.
            enableTorchMode(true);
        }
    };

    public final ButtonManager.ButtonCallback mMakeUpDisplayCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateMakeUpDisplay();
            updateFilterVideoDisplay();
        }
    };
    protected void updateFilterVideoDisplay() {
    }
    protected void updateMakeUpDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY)) {
            return;
        }
        updateMakeUpDisplayStatus(mDataModuleCurrent.getBoolean(Keys.KEY_MAKE_UP_DISPLAY));
        if (mDataModuleCurrent.getBoolean(Keys.KEY_MAKE_UP_DISPLAY)) {
            mUI.updatePortraitBackgroundReplacementUI(View.GONE);
        } else {
            mUI.updatePortraitBackgroundReplacementUI(View.VISIBLE);
        }
    }
    private void updateMakeUpDisplayStatus(boolean show){
        if (mMakeUpController == null) {
            return;
        }
        if(show){
            mMakeUpController.resume(mDataModuleCurrent.getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED));
        } else {
            mMakeUpController.pause();
        }
    }
    private String mFlashModeBeforeSceneMode;
    public FocusOverlayManager mFocusManager;
    private final CameraAgent.CameraAFCallback mAutoFocusCallback = new CameraAgent.CameraAFCallback() {
        @Override
        public void onAutoFocus(boolean focused, CameraProxy camera) {
            if (mPaused) {
                return;
            }
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused, false);
        }
    };
    private final Object mAutoFocusMoveCallback = ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK ? new CameraAgent.CameraAFMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
            mFocusManager.onAutoFocusMoving(moving);
        }
    }
            : null;
    private boolean mMirror;
    public final ButtonManager.ButtonCallback mCameraCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            if (mPaused
                    || mAppController.getCameraProvider().waitingForCamera()) {
                return;
            }
            ButtonManager buttonManager = mActivity.getButtonManager();
            buttonManager.disableCameraButtonAndBlock();
            mPendingSwitchCameraId = state;
            Log.d(TAG, "Start to copy texture.");

            // Disable all camera controls.
            mSwitchingCamera = true;
            switchCamera();
        }
    };
    private boolean mAeLockSupported;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mAutoChasingSupported;
    private boolean mAutoChasingOn;
    private BroadcastReceiver mReceiver = null;
    private int mShutterIconId;
    //SPRD:fix bug545455 video may not switch camera
    private boolean isShutterButtonClicked = false;
    private boolean mPauseRecorderRecording = false;
    private long mPauseTime = 0;
    /* @} */
    private long mResumeTime = 0;
    private long mResultTime = 0;
    private long mAllPauseTime = 0;

    // SPRD:fix bug 699367
    private boolean mPendingUpdateStorageSpace = false;
    private Surface mRecordingSurface;

    protected int mCameraState = PREVIEW_STOPPED;
    protected Boolean mFace;
    public boolean mFaceDetectionStarted = false;
    /**
     * Construct a new video module.
     */
    public VideoModule(AppController app) {
        super(app);
        mHandler = new MainHandler(app.getAndroidContext().getMainLooper());
    }

    public Handler getmHandler() {
        return mHandler;
    }

    /**
     * Calculates the preview size and stores it in mDesiredPreviewWidth and
     * mDesiredPreviewHeight.
     * <p>
     * This function checks
     * {@link com.android.camera.cameradevice.CameraCapabilities#getPreferredPreviewSizeForVideo()}
     * but also considers the current preview area size on screen and make sure
     * the final preview size will not be smaller than 1/2 of the current on
     * screen preview area in terms of their short sides. This function has
     * highest priority of WYSIWYG, 1:1 matching as its best match, even if
     * there's a larger preview that meets the condition above.
     * </p>
     *
     * @return The preferred preview size or {@code null} if the camera is not
     * opened yet.
     */
    private Point getDesiredPreviewSize(CameraCapabilities capabilities,
                                               CamcorderProfile profile, Point previewScreenSize) {
        if (capabilities.getSupportedVideoSizes() == null) {
            // Driver doesn't support separate outputs for preview and video.
            return new Point(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        final int previewScreenShortSide = (previewScreenSize.x < previewScreenSize.y ? previewScreenSize.x
                : previewScreenSize.y);
        List<Size> sizes = Size
                .convert(capabilities.getSupportedPreviewSizes());
        Size preferred = new Size(
                capabilities.getPreferredPreviewSizeForVideo());
        final int preferredPreviewSizeShortSide = (preferred.width() < preferred
                .height() ? preferred.width() : preferred.height());
        if (preferredPreviewSizeShortSide * 2 < previewScreenShortSide) {
            preferred = new Size(profile.videoFrameWidth,
                    profile.videoFrameHeight);
        }
        int product = preferred.width() * preferred.height();
        Iterator<Size> it = sizes.iterator();
        // Remove the preview sizes that are not preferred.
        while (it.hasNext()) {
            Size size = it.next();
            if (size.width() * size.height() > product) {
                it.remove();
            }
        }

        // Take highest priority for WYSIWYG when the preview exactly matches
        // video frame size. The variable sizes is assumed to be filtered
        // for sizes beyond the UI size.
        if(is3dnrOn() || isSlowMotionOn() || isBackGroudMode()){      //bugfix 1055134
            for (Size size : sizes) {
                if (size.width() == profile.videoFrameWidth
                        && size.height() == profile.videoFrameHeight) {
                    Log.v(TAG, "Selected =" + size.width() + "x" + size.height()
                            + " on WYSIWYG Priority");
                    return new Point(profile.videoFrameWidth,
                            profile.videoFrameHeight);
                }
            }
        }

        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes,
                (double) profile.videoFrameWidth / profile.videoFrameHeight);
        Point result;
        if (optimalSize == null)
            result = new Point(0,0);
        else
            result = new Point(optimalSize.width(), optimalSize.height());
        return result;
    }

    protected boolean isBackGroudMode(){
        return false;
    }

    private static void setCaptureRate(MediaRecorder recorder, double fps) {
        recorder.setCaptureRate(fps);
    }
    private final SensorEventListener mAccelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            onAccelerometerSensorChanged(event);
            updateLevelPresentation();

        }
    };

    private final SensorEventListener mMagneticListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            onMagneticSensorChanged(event);
        }
    };
    public void onAccelerometerSensorChanged(SensorEvent event) {

        mHasGravity = true;
        for(int i=0;i<3;i++) {
            mGravity[i] = mSensorAlpha * mGravity[i] + (1.0f-mSensorAlpha) * event.values[i];
        }
        calculateGeoDirection();

        double x = mGravity[0];
        double y = mGravity[1];
        double z = mGravity[2];
        double mag = Math.sqrt(x*x + y*y + z*z);

        mHasPitchAngle = false;
        if( mag > 1.0e-8 ) {
            mHasPitchAngle = true;
            mPitchAngle = Math.asin(- z / mag) * 180.0 / Math.PI;

            if(Math.abs(mPitchAngle) > 70.0 ) {
                mHasLevelAngle = false;
            }
            else {
                mHasLevelAngle = true;
                mNaturalLevelAngle = Math.atan2(-x, y) * 180.0 / Math.PI;
                if( mNaturalLevelAngle < -0.0 ) {
                    mNaturalLevelAngle += 360.0;
                }

                updateLevelAngles();
            }
        }
        else {
            mHasLevelAngle = false;
        }

    }
    public void onMagneticSensorChanged(SensorEvent event) {
        mHasGeomagnetic = true;
        for(int i=0;i<3;i++) {
            mGeomagnetic[i] = mSensorAlpha *mGeomagnetic[i] + (1.0f-mSensorAlpha) * event.values[i];
        }
        calculateGeoDirection();
    }
    private void calculateGeoDirection() {
        if( !mHasGravity || !mHasGeomagnetic ) {
            return;
        }
        if( !SensorManager.getRotationMatrix(mDeviceRotation, mDeviceInclination, mGravity, mGeomagnetic) ) {
            return;
        }
        SensorManager.remapCoordinateSystem(mDeviceRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, mCameraRotation);
        boolean mHasOldGeoDirection = mHasGeoDirection;
        mHasGeoDirection = true;
        SensorManager.getOrientation(mCameraRotation, mNewGeoDirection);
        for(int i=0;i<3;i++) {
            float mOldCompass = (float)Math.toDegrees(mGeoDirection[i]);
            float mNewCompass = (float)Math.toDegrees(mNewGeoDirection[i]);
            if( mHasOldGeoDirection ) {
                float mSmoothFactorCompass = 0.1f;
                float mSmoothThresholdCompass = 10.0f;
                mOldCompass = lowPassFilter(mOldCompass, mNewCompass, mSmoothFactorCompass, mSmoothThresholdCompass);
            }
            else {
                mOldCompass = mNewCompass;
            }
            mGeoDirection[i] = (float)Math.toRadians(mOldCompass);
        }
    }
    private float lowPassFilter(float oldValue, float newValue, float smoothFactorCompass, float smoothThresholdCompass) {
        float diff = Math.abs(newValue - oldValue);
        if( diff < 180 ) {
            if( diff > smoothThresholdCompass ) {
                oldValue = newValue;
            }
            else {
                oldValue = oldValue + smoothFactorCompass * (newValue - oldValue);
            }
        }
        else {
            if( 360.0 - diff > smoothThresholdCompass ) {
                oldValue = newValue;
            }
            else {
                if( oldValue > newValue ) {
                    oldValue = (oldValue + smoothFactorCompass * ((360 + newValue - oldValue) % 360) + 360) % 360;
                }
                else {
                    oldValue = (oldValue - smoothFactorCompass * ((360 - newValue + oldValue) % 360) + 360) % 360;
                }
            }
        }
        return oldValue;
    }
    public void updateLevelAngles() {
        if( mHasLevelAngle ) {
            mLevelAngle = mNaturalLevelAngle;
            double mCalibratedLevelAngle = 0.0f;
            mLevelAngle -= mCalibratedLevelAngle;
            mOrigLevelAngle = mLevelAngle;
            mLevelAngle -= (float)mCurrentOrientation;
            if( mLevelAngle < -180.0 ) {
                mLevelAngle += 360.0;
            }
            else if( mLevelAngle > 180.0 ) {
                mLevelAngle -= 360.0;
            }
        }
    }
    public int getUIRotation(){
        int degrees = CameraUtil.getDisplayRotation();
        int mRelativeOrientation = (mCurrentOrientation + degrees) % 360;
        return (360 - mRelativeOrientation) % 360;
    }
    public double getLevelAngle(){
        return mLevelAngle;
    }
    public boolean getHasPitchAngle(){
        return mHasPitchAngle;
    }
    public double getPitchAngle(){
        return mPitchAngle;
    }
    public boolean getHasGeoDirection(){
        return mHasGeoDirection;
    }
    public double getGeoDirection(){
        return mGeoDirection[0];
    }
    public boolean getHasLevelAngle(){
        return mHasLevelAngle;
    }
    public int getRotation(){
        return mActivity.getWindowManager().getDefaultDisplay().getRotation();
    }
    public double getOrigLevelAngle() {
        return this.mOrigLevelAngle;
    }
    private static String millisecondToTimeString(long milliSeconds,
                                                  boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    @Override
    public String getPeekAccessibilityString() {
        return mAppController.getAndroidContext().getResources()
                .getString(R.string.video_accessibility_peek);
    }

    // The date (in milliseconds) used to generate the last name.
    private static long mLastDate = 0;

    // Number of names generated for the same second.
    private static int mSameSecondCount = 0;

    protected String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mActivity.getString(R.string.video_file_name_format));

        String result = dateFormat.format(date);

        if (dateTaken / 1000 == mLastDate / 1000) {
            mSameSecondCount++;
            result += "_" + mSameSecondCount;
        } else {
            mLastDate = dateTaken;
            mSameSecondCount = 0;
        }

        return result;

    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
                     boolean isCaptureIntent) {
        mActivity = activity;
        // TODO: Need to look at the controller interface to see if we can get
        // rid of passing in the activity directly.
        mAppController = mActivity;

        mDataModule = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getDataModuleCamera();
        mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
        /*
         * SPRD: Fix bug 666039 that there's error when YouTuBe calls camera app to record video
         * SPRD: Fix bug 681215 optimization about that DV change to DC is slow
         */
        if (mActivity.isActivityResumed()) {
            long start = System.currentTimeMillis();
            requestCamera(getCameraId());
            Log.i(TAG, "init requestCamera cost: " + (System.currentTimeMillis() - start));
        }
        /* @} */

        DataStructSetting dataSetting = new DataStructSetting(
                DreamUtil.intToString(getMode()), DreamUtil.isFrontCamera(
                mAppController.getAndroidContext(), mCameraId),
                mActivity.getCurrentModuleIndex(), mCameraId);

        // change the data storage module
        DataModuleManager.getInstance(mAppController.getAndroidContext())
                .changeModuleStatus(dataSetting);

        mDataModuleCurrent = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getCurrentDataModule();
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientation = (orientation + 45) / 90 * 90;
                mCurrentOrientation = orientation % 360;
            }
        };
        // get temp module of photo for some setting such as timestamp status
        DataStructSetting tempPhotoDataSetting = new DataStructSetting(
                DataConfig.CategoryType.CATEGORY_PHOTO,
                DreamUtil.isFrontCamera(mAppController.getAndroidContext(), mCameraId),
                1,
                mCameraId);
        mTempModule = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getTempModule(tempPhotoDataSetting);

        mDataModuleCurrent.addListener(mActivity,this);
        mDataModule.addListener(mActivity,this);

        mAudioManager = AndroidServices.instance().provideAudioManager();

        mActivity.updateStorageSpaceAndHint(null);

        mUI = createUI(mActivity);
        mActivity.setPreviewStatusListener(mUI);

        mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);

        /*
         * To reduce startup time, we start the preview in another thread. We
         * make sure the preview is started at the end of onCreate.
         */
        /*
         * SPRD: Fix bug 666039 that there's error when YouTuBe calls camera app to record video
         * Original Code
         *
        requestCamera(mCameraId);
         */

        mContentResolver = mActivity.getContentResolver();

        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        mIsVideoCaptureIntent = isVideoCaptureIntent();

        mQuickCapture = mActivity.getIntent().getBooleanExtra(
                EXTRA_QUICK_CAPTURE, false);
        mLocationManager = mActivity.getLocationManager();

        mUI.setOrientationIndicator(0, false);
        setDisplayOrientation();

        mPendingSwitchCameraId = -1;
        mShutterIconId = CameraUtil.mModuleInfoResovle.getModuleCaptureIcon(mAppController.getCurrentModuleIndex());

        /*
         * SPRD: optimize bug 699216 that surface shows too slowly
         */
        HandlerThread thread = new HandlerThread("");
        thread.start();
        mAsyncHandler = new Handler(thread.getLooper());
        /* @} */
    }

    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    public void initializeControlByIntent() {
        if (isVideoCaptureIntent()) {
            if (!mDontResetIntentUiOnResume) {
                mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            }
            // reset the flag
            mDontResetIntentUiOnResume = false;
        }
    }

    /* SPRD: fix bug 553567 slow motion does not support takeSnapShot and zoom @{ */
    private boolean isSlowMotionOn() {
        String slowMotionValue = (mDataModuleCurrent != null) ? mDataModuleCurrent
                .getString(Keys.KEY_VIDEO_SLOW_MOTION) : null; // Bug 1159144 (FORWARD_NULL)
        int slow_motion = Integer
                .valueOf(slowMotionValue == null ? Keys.SLOWMOTION_DEFAULT_VALUE
                        : slowMotionValue);
        Log.i(TAG, "slow_motion = " + slow_motion + " boolean = "
                + (slow_motion == 1));
        return !(slow_motion == 1);
    }
    /* @} */

    private boolean isRecordingIn4k() {
        String videoQualityKey = isCameraFrontFacing() ? Keys.KEY_VIDEO_QUALITY_FRONT
                : Keys.KEY_VIDEO_QUALITY_BACK;
        String videoQuality = mDataModuleCurrent
                .getString(videoQualityKey);
        int quality = SettingsUtil.getVideoQuality(videoQuality, mCameraId);
        Log.d(TAG, "Selected video quality for '" + videoQuality + "' is " + quality);
        if (quality == CamcorderProfile.QUALITY_2160P) {
            return true;
        }
        return false;
    }

    /* SPRD:fix bug 615391 EOIS and 4k not support snap shot @ {*/
    private boolean isEOISOn() {
        String eoisKey = isCameraFrontFacing() ? Keys.KEY_EOIS_DV_FRONT
                : Keys.KEY_EOIS_DV_BACK;
        if (mDataModuleCurrent != null &&
                mDataModuleCurrent.isEnableSettingConfig(eoisKey)) {
            boolean eoisEnable = isCameraFrontFacing() ? CameraUtil.isEOISDvFrontEnabled()
                    : CameraUtil.isEOISDvBackEnabled();
            return eoisEnable && mDataModuleCurrent.getBoolean(eoisKey, false);
        }
        return false;
    }
    /* @} */

    protected boolean is3dnrOn() {
        return false;
    }

    protected AELockPanel mAELockPanel;
    protected AELockSeekBar mVerticalSeekBar;
    private int AECompensation = 0;
    protected boolean isAELock = false;

    public void setExposureCompensation(int value) {
        int max = mCameraCapabilities.getMaxExposureCompensation();
        int min = mCameraCapabilities.getMinExposureCompensation();
        Log.d(TAG, "setExposureCompensation vaule" + value);
        if (value >= min && value <= max) {
            mCameraSettings.setExposureCompensationIndex(value);
            mDataModuleCurrent.set(Keys.KEY_EXPOSURE, value);
        } else {
            Log.w(TAG, "invalid exposure range: " + value);
        }
    }

    protected void updateAELockExposureCompensation() {
        if(isAELock){
            setExposureCompensation(AECompensation);
        }
    }

    public void updateCompensationDirectly(int value){
        if (mCameraSettings != null) {
            int max = mCameraCapabilities.getMaxExposureCompensation();
            int min = mCameraCapabilities.getMinExposureCompensation();
            Log.d(TAG, "setAECompensate: " + value);
            if(value < min || value > max){
                Log.e(TAG,"AE compensation value is not  in the range [-6,6]");
                return;
            }
            mCameraSettings.setExposureCompensationIndex(value);
        }
    }

    /*
     * different form @setExposureCompensation()
     * this function just set Exposure Compensation
     * to HAL directly and don`t updata settings
     */
    public void setCompensationDirectly(int value){
        if (mCameraSettings != null) {
            int max = mCameraCapabilities.getMaxExposureCompensation();
            int min = mCameraCapabilities.getMinExposureCompensation();
            Log.d(TAG, "setAECompensate: " + value);
            if(value < min || value > max){
                Log.e(TAG,"AE compensation value is not  in the range [-6,6]");
                return;
            }
            mCameraSettings.setExposureCompensationIndex(value);
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    public boolean isAELock(){
        return isAELock;
    }

    private boolean aeValue;
    public boolean getAeLockValue(){
        return aeValue;
    }

    private void setAutoExposureLockIfSupported() {
        if (mAeLockSupported) {
            mCameraSettings.setAutoExposureLock(mFocusManager.getAeAwbLock());
        }
    }

    private  void procForAELockInPause() {
        // split this method for CCN optimization , Bug 839474
        if (mAELockPanel != null && mVerticalSeekBar != null) {
            mAELockPanel.setActive(false);
            updateAELock(false);
            mVerticalSeekBar.setProgress(0);
            mVerticalSeekBar.setOnSeekBarChangeListener(null);
        }
    }

    protected void updateAELock(boolean value){
        if (mDataModuleCurrent != null) {
            mDataModuleCurrent.set(Keys.KEY_AE_LOCK,value);
            aeValue = mDataModuleCurrent.getBoolean(Keys.KEY_AE_LOCK);
        }
        if (mCameraSettings != null && mCameraDevice != null) {
            boolean prevAELock = isAELock; // Bug 1138796 , after captured
            Log.d(TAG, "setAELock: " + value);
            isAELock = value;
            if(mFocusManager != null) {
                mFocusManager.setAeAwbLock(value);
                mFocusManager.setAELockState(value);
                setAutoExposureLockIfSupported();
            }
            if(!value){ //unlock ae then set AECompensation to 0
                updateCompensationDirectly(0);
                if(mAELockPanel != null && prevAELock != value) {
                    mAELockPanel.hide();
                }
            }
        }
    }

    protected void setAELockPanel(boolean show) {
        if (show) {
            if(mAELockPanel != null && isAELock && mVerticalSeekBar != null) {
                mAELockPanel.show();
                mAELockPanel.showtipText(true);
                mAELockPanel.setSeekBarVisibility(true);
                mVerticalSeekBar.setOnSeekBarChangeListener(this);
            }
        } else {
            if(mAELockPanel != null && (isAELock || isSupportTouchEV()) && mVerticalSeekBar != null) {
                mAELockPanel.hide();
                mVerticalSeekBar.setOnSeekBarChangeListener(null);
            }
        }
    }

    @Override
    public void setAELock(boolean value) {
        if (mDataModuleCurrent != null) {
            mDataModuleCurrent.changeSettings(Keys.KEY_AE_LOCK,value);
            aeValue = mDataModuleCurrent.getBoolean(Keys.KEY_AE_LOCK);
        }
        if (mCameraSettings != null && mCameraDevice != null) {
            Log.d(TAG, "setAELock: " + value);
            isAELock = value;
            if(mFocusManager != null) {
                mFocusManager.setAELockState(value);
            }
            if (mAeLockSupported) {
                mCameraSettings.setAutoExposureLock(value);
            }
            mCameraDevice.applySettings(mCameraSettings);
            if(!value){ //unlock ae then set AECompensation to 0
                setCompensationDirectly(0);
                if(mAELockPanel != null) {
                    mAELockPanel.hide();
                }
            }
        }
    }

    @Override
    public boolean isEnableDetector(){
        return mUI == null ? false : mUI.isEnableDetector();
    }

    private boolean doNothingWhenOnLongPress(MotionEvent var){
        int x = (int)var.getX();
        int y = (int)var.getY();
        if(null != mActivity && (null != mActivity.getCameraAppUI())
                && null != mActivity.getCameraAppUI().getPreviewArea()
                && (!mActivity.getCameraAppUI().getPreviewArea().contains(x, y) || y >= (CameraUtil.getDefaultDisplaySize().getHeight()-mActivity.getCameraAppUI().mBottomFrame.getMeasuredHeight()))){
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent var1){
        if (doNothingWhenOnLongPress(var1) || !isEnableDetector() || mMediaRecorderRecording) {
            return;
        }
        if (isCameraFrontFacing() || !mAeLockSupported || !mFocusAreaSupported) {
            return;
        }
        if(isSupportTouchEV()&& mVerticalSeekBar != null) {
            mVerticalSeekBar.setOnSeekBarChangeListener(this);//SPRD:fix bug1404592
        }
        mFocusManager.onLongPress((int)var1.getX(),(int)var1.getY());
    }

    @Override
    public void onProgressChanged(SeekBar SeekBarId, int progress, boolean fromUser) {
        Log.d(TAG,"onProgressChanged" + progress);
        if(progress != AECompensation) {
            AECompensation = progress;
            setCompensationDirectly(AECompensation);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar SeekBarId) {
        Log.d(TAG,"onStartTrackingTouch");
        if (mFocusManager != null && !isAELock) {
            mFocusManager.removeMessages();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar SeekBarId) {
        Log.d(TAG,"onStopTrackingTouch");
        if (!isAELock) {
            if (mFocusManager != null) {
                mFocusManager.sendEmptyMessageDelayed();
            }
            if (mCameraSettings != null) {
                mCameraSettings.setExposureCompensationIndex(0);
            }
        }
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        /*
         * SPRD Bug:509945,not start preview,intercept snapshot@{ Original
         * Android code: if (mPaused || mCameraDevice == null) {
         */
        Log.v(TAG, "onSingleTapup mPreviewing=" + mPreviewing + " FreezeFlag="
                + mActivity.getCameraAppUI().getFreezeScreenFlag());
        if (mPaused || mCameraDevice == null || !mPreviewing
                || mActivity.getCameraAppUI().getFreezeScreenFlag()) {//Sprd:fix bug736524
        /* @} */
            return;
        }
        /*
         * SPRD: Fix bug 751782 that allow autoFocus in mediaRecording @{
         */
        if (mMediaRecorderRecording) {
            if (!CameraUtil.isAFAEInRecordingEnable() && !(mAutoChasingOn && mAutoChasingSupported)) {
                return;
            }
            /* @} */
        }


        // Check if metering area or focus area is supported.
        if (!mFocusAreaSupported && !mMeteringAreaSupported) {
            return;
        }
        if((isAELock || isSupportTouchEV()) && mAELockPanel != null && mVerticalSeekBar != null) {
            mAELockPanel.hide();
            mVerticalSeekBar.setOnSeekBarChangeListener(this);
        }
        // Tap to focus.
        mFocusManager.onSingleTapUp(x, y, isSupportTouchEV());
    }
    protected boolean isTakeSnapShot(){
        return true;
    }
    public void takeASnapshot() {
        if(!isTakeSnapShot()){
            return;
        }
        // Only take snapshots if video snapshot is supported by device
        if (!mCameraCapabilities
                .supports(CameraCapabilities.Feature.VIDEO_SNAPSHOT)) {
            Log.w(TAG,
                    "Cannot take a video snapshot - not supported by hardware");
            return;
        }
        // Do not take the picture if there is not enough storage.
        if ((mActivity.getStorageSpaceBytes() >> 20) <= (Storage.LOW_STORAGE_THRESHOLD_BYTES >> 20)
            || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES) {
            // use MB instead of B to compare. B >> 20 = MB
            Log.i(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            return;
        }

        if (!mIsVideoCaptureIntent) {
            if (!mMediaRecorderRecording || mPaused || mSnapshotInProgress
                    || !mAppController.isShutterEnabled()
                    || mCameraDevice == null) {
                return;
            }
            mUI.enablePreviewOverlayHint(false);
            Location loc = mLocationManager.getCurrentLocation();
            CameraUtil.setGpsParameters(mCameraSettings, loc);
            mCameraDevice.applySettings(mCameraSettings);

            // Set JPEG orientation. Even if screen UI is locked in portrait,
            // camera orientation
            // should
            // still match device orientation (e.g., users should always get
            // landscape photos while
            // capturing by putting device in landscape.)
            Characteristics info = mActivity.getCameraProvider()
                    .getCharacteristics(mCameraId);
            int sensorOrientation = info.getSensorOrientation();
            int deviceOrientation = mAppController.getOrientationManager()
                    .getDeviceOrientation().getDegrees();
            boolean isFrontCamera = info.isFacingFront();
            int jpegRotation = CameraUtil.getImageRotation(sensorOrientation,
                    deviceOrientation, isFrontCamera);
            Log.i(TAG, " sensorOrientation = " + sensorOrientation
                    + " ,deviceOrientation = " + deviceOrientation
                    + " isFrontCamera = " + isFrontCamera);
            mCameraDevice.setJpegOrientation(jpegRotation);

            Log.i(TAG, "Video snapshot start");
            mCounterWaitSnapshot = new Counter(1);
            mSnapshotInProgress = true;
            mCameraDevice.takePicture(mAsyncHandler, null, null, null,
                    new JpegPictureCallback(loc));
            showVideoSnapshotUI(true);
        }
    }

    private void updateAutoFocusMoveCallback() {
        if (mPaused || mCameraDevice == null) {
            return;
        }

        if (mCameraSettings.getCurrentFocusMode() == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
            mCameraDevice.setAutoFocusMoveCallback(mHandler,
                    (CameraAgent.CameraAFMoveCallback) mAutoFocusMoveCallback);
        } else {
            mCameraDevice.setAutoFocusMoveCallback(null, null);
        }
    }

    private void removeAutoFocusMoveCallback() {
        if (mCameraDevice != null) {
            mCameraDevice.setAutoFocusMoveCallback(null, null);
        }
    }

    /**
     * @return Whether the currently active camera is front-facing.
     */
    protected boolean isCameraFrontFacing() {
        return mAppController.getCameraProvider().getCharacteristics(mCameraId)
                .isFacingFront();
    }

    /**
     * @return Whether the currently active camera is back-facing.
     */
    private boolean isCameraBackFacing() {
        return mAppController.getCameraProvider().getCharacteristics(mCameraId)
                .isFacingBack();
    }

    /**
     * The focus manager gets initialized after camera is available.
     */
    private void initializeFocusManager() {
        // Create FocusManager object. startPreview needs it.
        // if mFocusManager not null, reuse it
        // otherwise create a new instance
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
        } else {
            mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = mActivity
                    .getResources()
                    .getStringArray(R.array.pref_camera_focusmode_default_array);
            CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                    .getStringifier();
            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>();
            for (String modeString : defaultFocusModesStrings) {
                CameraCapabilities.FocusMode mode = stringifier
                        .focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            mAutoChasingOn = mDataModuleCurrent
                    .getBoolean(Keys.KEY_AUTO_TRACKING);
            mFocusManager = new FocusOverlayManager(mAppController,
                    defaultFocusModes, mCameraCapabilities, this, mMirror,
                    mActivity.getMainLooper(), mUI.getFocusRing(), mUI.getChasingRing(),
                    mAutoChasingOn && mAutoChasingSupported);
        }
        mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
    }

    protected void sendDeviceOrientation() {}

    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        mUI.onOrientationChanged(orientationManager, deviceOrientation);
    }

    @Override
    public HardwareSpec getHardwareSpec() {
        if (mHardwareSpec == null) {
            mHardwareSpec = (mCameraSettings != null ? new HardwareSpecImpl(
                    getCameraProvider(), mCameraCapabilities,
                    isCameraFrontFacing()) : null);
        }
        return mHardwareSpec;
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();

        bottomBarSpec.enableCamera = true;
        bottomBarSpec.cameraCallback = mCameraCallback;
        bottomBarSpec.enableTorchFlash = true && !(mActivity.getIsButteryLow());
        bottomBarSpec.flashCallback = mFlashCallback;
        bottomBarSpec.hideHdr = true;
        bottomBarSpec.enableGridLines = true;
        bottomBarSpec.enableExposureCompensation = false;
        bottomBarSpec.isExposureCompensationSupported = false;

        if (isVideoCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = mDoneCallback;
            bottomBarSpec.showReview = true;
            bottomBarSpec.reviewCallback = mReviewCallback;
        }

        return bottomBarSpec;
    }

    public boolean checkCameraProxy() {
        boolean preferNewApi = useNewApi();
        return (preferNewApi == getCameraProvider().isNewApi())
                && mCameraId == getCameraProvider().getCurrentCameraId().getLegacyValue();
    }

    @Override
    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable cameraProxy = " + cameraProxy);
        if (cameraProxy == null) {
            Log.w(TAG, "onCameraAvailable returns a null CameraProxy object");
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
        mCameraCapabilities = mCameraDevice.getCapabilities();
        mAppController.getCameraAppUI().showAccessibilityZoomUI(
                mCameraCapabilities.getMaxZoomRatio());
        mCameraSettings = mCameraDevice.getSettings();
        if (mCameraSettings == null) return;
        mAeLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        mFocusAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.FOCUS_AREA);
        mMeteringAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.METERING_AREA);
        if (isRecordingIn4k()){
            mMaxRatio = CameraUtil.getZoomMaxInFourKVideo(mCameraCapabilities.getMaxZoomRatio());
        } else {
            mMaxRatio = mCameraCapabilities.getMaxZoomRatio();
        }
        mZoomRatioSection = mCameraCapabilities.getZoomRatioSection();
        if (mZoomRatioSection != null) {
            mMinRatio = mZoomRatioSection[0];
            for (float max : mZoomRatioSection){
                mMaxRatio = mMaxRatio > max ? mMaxRatio : max;
            }
        } else {
            mMinRatio = 1.0f;
        }

        mAutoChasingSupported = CameraUtil.isAutoChasingSupport();

        if (CameraUtil.isVideoAEAFLockEnable() || isSupportTouchEV()) {
            if(mVerticalSeekBar == null){
                mVerticalSeekBar = (AELockSeekBar)mActivity.getModuleLayoutRoot().findViewById(R.id.aeseekbar);
            }
            if(mVerticalSeekBar != null) {
                mVerticalSeekBar.setMax(mCameraCapabilities.getMaxExposureCompensation());
                mVerticalSeekBar.setMin(mCameraCapabilities.getMinExposureCompensation());
                mVerticalSeekBar.setProgress(0);
                mVerticalSeekBar.setOnSeekBarChangeListener(this);
            }
        }

        readVideoPreferences();
        updateDesiredPreviewSize();
        resizeForPreviewAspectRatio();
        initializeFocusManager();
        // TODO: Having focus overlay manager caching the parameters is prone to
        // error,
        // we should consider passing the parameters to focus overlay to ensure
        // the
        // parameters are up to date.
        mFocusManager.updateCapabilities(mCameraCapabilities);
        if (mAutoChasingSupported) {
            mCameraDevice.setAutoChasingTraceRegionCallback(mHandler,this);
        }

        startPreview(true);
        initializeVideoSnapshot();
        mUI.initializeZoom(mCameraSettings, mCameraCapabilities);
        if (isSlowMotionOn()) {
            mUI.hideZoomProcessorIfNeeded();
/*            if(!mActivity.isFilmstripVisible())
                CameraUtil.toastHint(mActivity, R.string.slow_motion_does_not_support_zoom_change);*/
        }
//        initializeControlByIntent();

        mHardwareSpec = new HardwareSpecImpl(getCameraProvider(),
                mCameraCapabilities,isCameraFrontFacing());
        mAppController.getCameraAppUI().setSwipeEnabled(true);
        mCameraAvailable = true;
        updateFlashTopButton();
        updateMakeUpDisplay();
    }

    private void updateFlashTopButton() {
        if(CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing())){
            if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEO_FLASH_MODE)){
                mUI.setButtonVisibility(ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM,View.VISIBLE);
                return;
            }
        }
        Log.i(TAG, "Flash is not supported");
        mUI.setButtonVisibility(ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM,View.INVISIBLE);
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(mCurrentVideoUri,
                convertOutputFormatToMimeType(mProfile.fileFormat));

        // SPRD: add for mutex between float window and camera
        intent.putExtra(KEY_DISABLE_FLOAT_WINDOW, true);

        try {
            mActivity.launchActivityByIntent(intent);
            played = true;
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + mCurrentVideoUri, ex);
        }
    }

    @Override
    public void onReviewPlayClicked(View v) {
        startPlayVideoActivity();
    }

    @Override
    public void onReviewDoneClicked(View v) {
        mIsInReviewMode = false;
        doReturnToCaller(true);
    }

    @Override
    public void onReviewCancelClicked(View v) {
        // TODO: It should be better to not even insert the URI at all before we
        // confirm done in review, which means we need to handle temporary video
        // files in a quite different way than we currently had.
        // Make sure we don't delete the Uri sent from the video capture intent.
        if (mCurrentVideoUriFromMediaSaved) {
            mContentResolver.delete(mCurrentVideoUri, null, null);
        }
        mIsInReviewMode = false;
        doReturnToCaller(false);
    }

    @Override
    public boolean isInReviewMode() {
        return mIsInReviewMode;
    }

    private void onStopVideoRecording() {
        /* SPRD: fix for bug 535167 @{ */
        // mAppController.getCameraAppUI().enableCameraToggleButton();
        /* @} */
        mAppController.getCameraAppUI().setSwipeEnabled(true);
        boolean recordFail = stopVideoRecording();
        setCameraState(IDLE);
        if (shouldShowResult()) {
            if (mQuickCapture) {
                doReturnToCaller(!recordFail);
            } else if (!recordFail) {
                showCaptureResult();
            }
        } else if (!recordFail) {
            // Start capture animation.
            if (!mPaused && ApiHelper.HAS_SURFACE_TEXTURE_RECORDING) {
                // The capture animation is disabled on ICS because we use
                // SurfaceView
                // for preview during recording. When the recording is done, we
                // switch
                // back to use SurfaceTexture for preview and we need to stop
                // then start
                // the preview. This will cause the preview flicker since the
                // preview
                // will not be continuous for a short period of time.
                mAppController.startFlashAnimation(false);
            }
        }
    }

    public void onVideoSaved() {
        if (shouldShowResult()) {
            showCaptureResult();
        }
    }

    public void onProtectiveCurtainClick(View v) {
        // Consume clicks
    }

    @Override
    public void onShutterButtonClick() {
        /*
         * SPRD Bug:509945,not start preview,intercept recording @{ Original
         * Android code: if (mSwitchingCamera) {
         */
        if (mSwitchingCamera || !mPreviewing
                || mAppController.getCameraAppUI().isModeListOpen()
                || mMediaRecorderPrepareing
                || (mActivity.getStorageSpaceBytes() >> 20) <= (Storage.LOW_STORAGE_THRESHOLD_BYTES >> 20)
                || (mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES)
                // to avoid the case such as: 50003968 < 50000000 , but 3968 bytes cannot save new video file
                // use MB instead of B to compare. B >> 20 = MB
                || mPendingUpdateStorageSpace) {
        /* @} */
            /* SPRD: fix bug 538868 video may not switch camera@{*/
            //mAppController.getCameraAppUI().enableCameraToggleButton();
            /* @} */
            return;
        }

        //Sprd fix bug819907
        if (mAppController.getCameraAppUI().isSettingLayoutOpen()) {
            Log.i(TAG, "settingui is showing, do not record");
            return;
        }

        if (isPhoneCalling(mActivity) || mIsTwoVT) {
            Log.i(TAG, "video won't start due to telephone is running");
            CameraUtil.toastHint(mActivity, R.string.phone_does_not_support_video);
            return;
        }

        Log.i(TAG, "onShutterButtonClick");
        //SPRD:fix bug545455 video may not switch camera
        isShutterButtonClicked = true;
        boolean stop = (mMediaRecorderRecording || mMediaRecorderPrepareing);
        if (stop) {
            // CameraAppUI mishandles mode option enable/disable
            // for video, override that
            // Sprd: Add for bug 529369 stop Video recording before switch
            // camera
            //mAppController.getCameraAppUI().enableCameraToggleButton();
            onStopVideoRecording();
        } else {
            if (isAudioRecording() && !isAudioSilence(mIsVideoCaptureIntent)) {
                Log.i(TAG,"audio source has been occupyed, video will has not audio track");
                CameraUtil.toastHint(mActivity, R.string.recorder_without_audio_track);
            }
            if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES
                || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES) {
                return;
            }
            mAppController.cancelFlashAnimation();
            startVideoRecording();
        }
        mAppController.setShutterEnabled(false);
        if (mCameraSettings != null && !isAELock) {
            mFocusManager.onShutterUp(mCameraSettings.getCurrentFocusMode());
        }

        // Keep the shutter button disabled when in video capture intent
        // mode and recording is stopped. It'll be re-enabled when
        // re-take button is clicked.
        if (!(shouldShowResult() && stop) && !useNewApi()) {
            mHandler.sendEmptyMessageDelayed(MSG_ENABLE_SHUTTER_BUTTON,
                    SHUTTER_BUTTON_TIMEOUT);
        }
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
        // Do nothing.
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // TODO: Remove this when old camera controls are removed from the UI.
        // Sprd: Add for bug 529369 stop Video recording before switch camera
        //Log.d(TAG, "mMediaRecorderRecording = " + mMediaRecorderRecording
        //        + ",pressed" + pressed);
        //if (pressed && !mMediaRecorderRecording) {
        //    mAppController.getCameraAppUI().disableCameraToggleButton();
        ///*SPRD:fix bug545455 video may not switch camera@{*/
        //    isShutterButtonClicked = false;
        //} else if (!pressed && !mMediaRecorderRecording && !isShutterButtonClicked) {
        //    mAppController.getCameraAppUI().enableCameraToggleButton();
        ///*@}*/
        //}
    }

    private void readVideoPreferences() {
        // The preference stores values from ListPreference and is thus string
        // type for all values.
        // We need to convert it to int manually.
        /**
         * SPRD: Fix bug 585183 Adds new features 3D recording @{
         * Original Code
         *
        String videoQualityKey = isCameraFrontFacing() ? Keys.KEY_VIDEO_QUALITY_FRONT
                : Keys.KEY_VIDEO_QUALITY_BACK;
        */
        //String videoQualityKey = getVideoQualityKey();
        /* @} */
        /**
        String videoQuality = mDataModuleCurrent.getString(videoQualityKey);
        int quality = SettingsUtil.getVideoQuality(videoQuality, mCameraId);
        Log.d(TAG, "Selected video quality for '" + videoQuality + "' is "
                + quality);
        */
        int quality = getVideoQuality();
        Log.d(TAG, "Selected video quality " + quality);
        // Set video quality.
        Intent intent = mActivity.getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality = intent.getIntExtra(
                    MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                quality = CamcorderProfile.QUALITY_HIGH;
            } else { // 0 is mms.
                quality = CamcorderProfile.QUALITY_LOW;
            }
        }

        // Set video duration limit. The limit is read from the preference,
        // unless it is specified in the intent.
        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            int seconds = intent
                    .getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
            mMaxVideoDurationInMs = 1000 * seconds;
        } else {
            mMaxVideoDurationInMs = SettingsUtil.getMaxVideoDuration(mActivity
                    .getAndroidContext());
        }

        // If quality is not supported, request QUALITY_HIGH which is always
        // supported.
        if (CamcorderProfile.hasProfile(mCameraId, quality) == false) {
            quality = CamcorderProfile.QUALITY_HIGH;
        }
        mProfile = getCamcorderProfile(quality);
        mPreferenceRead = true;
    }

    protected CamcorderProfile getCamcorderProfile(int quality) {
        return CamcorderProfile.get(mCameraId, quality);
    }

    /* SPRD: Fix bug 585183 Adds new features 3D recording @{ */
    /* SPRD: Fix bug 603519 The beauty and the resolution of the mutex @{ */
    protected int getVideoQuality() {
        String videoQuality = mDataModuleCurrent.getString(getVideoQualityKey());
        return SettingsUtil.getVideoQuality(videoQuality, mCameraId);
    }
    /**
     * Calculates and sets local class variables for Desired Preview sizes. This
     * function should be called after every change in preview camera resolution
     * and/or before the preview starts. Note that these values still need to be
     * pushed to the CameraSettings to actually change the preview resolution.
     * Does nothing when camera pointer is null.
     */
    protected void updateDesiredPreviewSize() {
        if (mCameraDevice == null) {
            return;
        }

        mCameraSettings = mCameraDevice.getSettings();

        desiredPreviewSize = getDesiredPreviewSize(mCameraCapabilities,
                mProfile, mUI.getPreviewScreenSize());
        mDesiredPreviewWidth = desiredPreviewSize.x;
        mDesiredPreviewHeight = desiredPreviewSize.y;
        mUI.setPreviewSize(mDesiredPreviewWidth, mDesiredPreviewHeight);

        Log.v(TAG, "Updated DesiredPreview=" + mDesiredPreviewWidth + "x"
                + mDesiredPreviewHeight);
    }

    private void resizeForPreviewAspectRatio() {
        mUI.setAspectRatio((float) mProfile.videoFrameWidth
                / mProfile.videoFrameHeight);
    }

    private void installIntentFilter() {
        // SPRD:fix bug599645 VideoModule recivedBroadcast later than CameraActivity, cause be killed
        // install an intent filter to receive SD card related events.
        // IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        // intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        // intentFilter.addDataScheme("file");
        mReceiver = new MyBroadcastReceiver();
        // mActivity.registerReceiver(mReceiver, intentFilter);
        mActivity.registerMediaBroadcastReceiver(mReceiver);
    }

    private void setDisplayOrientation() {
        mDisplayRotation = CameraUtil.getDisplayRotation();
        Characteristics info = mActivity.getCameraProvider()
                .getCharacteristics(mCameraId);
        if(info != null) {
            mCameraDisplayOrientation = info
                    .getPreviewOrientation(mDisplayRotation);
        }
        // Change the camera display orientation
        if (mCameraDevice != null) {
            mCameraDevice.setDisplayOrientation(mDisplayRotation);
        }
        if (mFocusManager != null) {
            mFocusManager.setDisplayOrientation(mCameraDisplayOrientation);
        }
    }

    @Override
    public void updateCameraOrientation() {
        if (mMediaRecorderRecording) {
            return;
        }
        if (mDisplayRotation != CameraUtil.getDisplayRotation()) {
            setDisplayOrientation();
        }
    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    /**
     * Returns current Zoom value, with 1.0 as the value for no zoom.
     */
    private float currentZoomValue() {
        return mCameraSettings.getCurrentZoomRatio();
    }

    @Override
    public void onZoomChanged(float ratio) {
        // Not useful to change zoom value when the activity is paused.
        if (mPaused) {
            return;
        }
        if (isSlowMotionOn()) {
            mUI.hideZoomProcessorIfNeeded();
            CameraUtil.toastHint(mActivity, R.string.slow_motion_does_not_support_zoom_change);
            return;
        }
        mZoomValue = ratio;
        if (mCameraSettings == null || mCameraDevice == null) {
            return;
        }
        // Set zoom parameters asynchronously
        mCameraSettings.setZoomRatio(mZoomValue);
        mCameraDevice.applySettings(mCameraSettings);
    }

    private void startPreview(boolean optimize) {
        Log.i(TAG, "startPreview");
        /*
         * SPRD: Fix bug 613015 add SurfaceView support @{
         * Original Code
         *
        if (!mPreferenceRead || surfaceTexture == null || mPaused == true
                || mCameraDevice == null) {
            return;
        }
        */
       if (notStartPrview())
           return;
       /* @} */

        if (mPreviewing == true) {
            stopPreview();
        }

        setDisplayOrientation();
        mCameraDevice.setDisplayOrientation(mDisplayRotation);
        setCameraParameters();

        cancelFocus();

        // This is to notify app controller that preview will start next, so app
        // controller can set preview callbacks if needed. This has to happen
        // before
        // preview is started as a workaround of the framework issue related to
        // preview
        // callbacks that causes preview stretch and crash. (More details see
        // b/12210027
        // and b/12591410. Don't apply this to L, see b/16649297.
        setOneShotCallback();
        try {
            /*
             * SPRD: Fix bug 613015 add SurfaceView support @{
             * Original Code
             *
            mCameraDevice.setPreviewTexture(surfaceTexture);
             */
            setDisplay(optimize);

            /* @} */
            // SPRD: Fix bug 677589 black preview occurs when we switch to video module from photo
            // no need to handle surface update in process of the second onPreviewStarted()
            final boolean handleSurfaceUpdate = !mPreviewing;
            CameraAgent.CameraStartPreviewCallback startPreviewCallback = new CameraAgent.CameraStartPreviewCallback() {
                @Override
                public void onPreviewStarted() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            VideoModule.this.onPreviewStarted();
                        }
                    });

                    if(!isUseSurfaceView() || mHandler == null) {
                        return;
                    }
                    if (!handleSurfaceUpdate) {
                        return;
                    }
                    final Runnable hideModeCoverRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!mPaused) {
                                final CameraAppUI cameraAppUI = mActivity.getCameraAppUI();
                                cameraAppUI.onSurfaceTextureUpdated(cameraAppUI.getSurfaceTexture());
                                cameraAppUI.pauseTextureViewRendering();
                            }
                        }
                    };
                    if (useNewApi()) {
                        if (mRecordingSurface == null) {//SPRD:fix bug1181442
                            mCameraDevice.setSurfaceViewPreviewCallback(
                                    new CameraAgent.CameraSurfaceViewPreviewCallback() {
                                        @Override
                                        public void onSurfaceUpdate() {
                                            Log.d(TAG, "SurfaceView: CameraSurfaceViewPreviewCallback");
                                            if (mPaused || mCameraDevice == null || !mPreviewing) {//SPRD:fix bug681598
                                                return;
                                            }
                                            mActivity.getCameraAppUI().hideModeCover();
                                            mHandler.post(hideModeCoverRunnable);
                                            // set callback to null
                                            mCameraDevice.setSurfaceViewPreviewCallback(null);
                                        }
                                    });
                        }
                    } else {
                        mAsyncHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mPaused || mCameraDevice == null || !mPreviewing) {//SPRD:fix bug752287
                                    return;
                                }
                                mActivity.getCameraAppUI().hideModeCover();
                                mHandler.post(hideModeCoverRunnable);

                            }
                        }, UNFREEZE_TIME);
                    }
                }
            };
            doStartPreview(startPreviewCallback, mCameraDevice);
//            mPreviewing = true;
            // SPRD: Fix bug 659315, optimize camera launch time
//            mUI.onPreviewStarted();
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }
    }

    private boolean notStartPrview() {
        SurfaceTexture surfaceTexture = mActivity.getCameraAppUI().getSurfaceTexture();
        SurfaceHolder surfaceHolder = mActivity.getCameraAppUI().getSurfaceHolder();
        if (!mPreferenceRead || mPaused == true || mCameraDevice == null) {
            return true;
        }
        if (isUseSurfaceView()) {
            if (surfaceHolder == null) {
                Log.w(TAG, "startPreview: SurfaceHolder is not ready.");
                return true;
            }
        } else {
            if (surfaceTexture == null) {
                Log.w(TAG, "startPreview: surfaceTexture is not ready.");
                return true;
            }
        }
        return false;
    }

    private void cancelFocus() {
        if (mFocusManager != null) {
            // If the focus mode is continuous autofocus, call cancelAutoFocus
            // to resume it because it may have been paused by autoFocus call.
            CameraCapabilities.FocusMode focusMode = mFocusManager
                    .getFocusMode(mCameraSettings.getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE));
            if (focusMode == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                mCameraDevice.cancelAutoFocus();
            }
        }
        setCameraState(IDLE);
    }

    private void setOneShotCallback() {
        if (!ApiHelper.isLOrHigher()) {
            Log.v(TAG, "calling onPreviewReadyToStart to set one shot callback");
            mAppController.onPreviewReadyToStart();
        } else {
            Log.v(TAG, "on L, no one shot callback necessary");
        }
    }

    private void setDisplay(boolean optimize) {
        SurfaceTexture surfaceTexture = mActivity.getCameraAppUI().getSurfaceTexture();
        SurfaceHolder surfaceHolder = mActivity.getCameraAppUI().getSurfaceHolder();
        if (isUseSurfaceView()) {
            if (optimize) {
                mCameraDevice.setPreviewDisplay(surfaceHolder);
            } else {
                mCameraDevice.setPreviewDisplayWithoutOptimize(surfaceHolder);
            }
        } else {
            if (optimize) {
                mCameraDevice.setPreviewTexture(surfaceTexture);
            } else {
                mCameraDevice.setPreviewTextureWithoutOptimize(surfaceTexture);
            }

        }
    }

    protected void doStartPreview(CameraAgent.CameraStartPreviewCallback startPreviewCallback, CameraAgent.CameraProxy cameraDevice) {
        if (useNewApi()) {
            mCameraDevice.startPreview();
            startPreviewCallback.onPreviewStarted();
        } else {
            mCameraDevice.startPreviewWithCallback(new Handler(mAsyncHandler.getLooper()),
                    startPreviewCallback);
        }
        mPreviewing = true;

        // SPRD: Fix bug 659315, optimize camera launch time
        mUI.onPreviewStarted();

        // bug 934808 , icon cannot rotate while switch from DC to DV under landscape orientation
        mActivity.getButtonManager().enableCameraButton();
        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        orientationManager.addOnOrientationChangeListener(this);
        mUI.onOrientationChanged(orientationManager,
                orientationManager.getDeviceOrientation());
    }

    private void onPreviewStarted() {
        mAppController.setShutterEnabled(true);
        mAppController.onPreviewStarted();
        setCameraState(IDLE);
        updateFace(true);
        if (mFocusManager != null) {
            mFocusManager.onPreviewStarted();
        }
        onPreviewStartedAfter();
    }

    protected void setCameraState(int state) {
        mCameraState = state;
    }

    public boolean isCameraIdle() {
        return (mCameraState == IDLE)
                || (mCameraState == PREVIEW_STOPPED)
                || ((mFocusManager != null) && mFocusManager.isFocusCompleted() && (mCameraState != SWITCHING_CAMERA));
    }

    protected void updateFace(boolean applySetting) {
        if (mCameraDevice == null) {
            Log.i(TAG, "mCameraDevice is null ");
            return;
        }
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_VIDEO_FACE_DETECT)){
            Log.i(TAG,"current module does not support face detect");
            return;
        }
        mFace = mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_VIDEO_FACE_DETECT);
        if (mFace == null) {
            Log.i(TAG, "mFace is null ");
            return;
        }
        if (mFace && isCameraIdle()) {
            startFaceDetection();
        } else if (!mFace || !isCameraIdle()) {
            stopFaceDetection();
        }
        if (applySetting) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    @Override
    public void onPreviewInitialDataReceived() {
    }

    @Override
    public void stopPreview() {
        if (!mPreviewing) {
            Log.v(TAG, "Skip stopPreview since it's not mPreviewing");
            return;
        }
        if (mCameraDevice == null) {
            Log.v(TAG, "Skip stopPreview since mCameraDevice is null");
            return;
        }

        Log.v(TAG, "stopPreview");
        mCameraDevice.stopPreview();
        if (mFocusManager != null) {
            mFocusManager.onPreviewStopped();
        }
        mPreviewing = false;
        setCameraState(PREVIEW_STOPPED);
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera");
        mCameraAvailable = false;
        if (mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        mUI.hideZoomProcessorIfNeeded();
        stopFaceDetection();
        mCameraDevice.setZoomChangeListener(null);
        mActivity.getCameraProvider().releaseCamera(mCameraDevice.getCameraId());
        mCameraDevice = null;
        mPreviewing = false;
        mSnapshotInProgress = false;
        if (mFocusManager != null) {
            mFocusManager.onCameraReleased();
        }
        setCameraState(PREVIEW_STOPPED);
    }

    @Override
    public boolean onBackPressed() {
        if (mPaused) {
            return true;
        }
        if (mMediaRecorderRecording) {
            onStopVideoRecording();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mPaused) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            /* SPRD:Bug 535058 New feature: volume @{ */
                return doSomethingAboutSetting(keyCode);
            /* }@ */
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                    return true;
                }
            case KeyEvent.KEYCODE_MENU:
                // Consume menu button presses during capture.
                return mMediaRecorderRecording;
        }
        return false;
    }

    private boolean doSomethingAboutSetting(int keyCode) {
        int volumeStatus = getVolumeControlStatus(mActivity);
        if (volumeStatus == Keys.shutter) {
            return true;
        } else if (volumeStatus == Keys.zoom) {
            if(mDataModuleCurrent != null &&
                    mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) &&
                    !mDataModuleCurrent.getBoolean(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE)){
                CameraUtil.toastHint(mActivity,R.string.current_module_does_not_support_zoom_change);
                return true;
            }
            if (mPaused || mSnapshotInProgress) {
                return true;
            }
            float zoomValue;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                zoomValue = increaseZoomValue(mZoomValue);
            } else {
                zoomValue = reduceZoomValue(mZoomValue);
            }
            onZoomChanged(zoomValue);
            mUI.setPreviewOverlayZoom(mZoomValue);
            return true;
        } else if (volumeStatus == Keys.volume) {
            return false;
        }
        return false;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        long time = System.currentTimeMillis() - mLastReceiveEventTime;
        mLastReceiveEventTime = System.currentTimeMillis();
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            /*
             * SPRD:fix bug 546663 We will not handle the event because activity is in review
             * mode@{
             */
                if (isInReviewMode()) {
                    return true;
                }
            /* }@ */
            /*
             * SPRD:fix bug518054 ModeListView is appear when begin to capture using volume
             * key@{
             */
                mActivity.getCameraAppUI().hideModeList();
            /* }@ */
            /* SPRD:Bug 535058 New feature: volume @{ */
                int volumeStatus = getVolumeControlStatus(mActivity);
                if (volumeStatus == Keys.shutter || keyCode == KeyEvent.KEYCODE_CAMERA) {
                    if (mActivity.hasKeyEventEnter() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                        mActivity.resetHasKeyEventEnter();
                        return true;
                    }
                    /*SPRD Bug 1037598 double click key in 400ms will be seen as one click */
                    if (time > KEY_CLICK_INTERVAL_TIMEOUT || mMediaRecorderRecording || mMediaRecorderPrepareing) {
                        onShutterButtonFocus(true);
                        onShutterButtonClick();
                    }
                    return true;
                } else if (volumeStatus == Keys.zoom) {
                    mUI.hideZoomUI();
                    return true;
                } else if (volumeStatus == Keys.volume) {
                    return false;
                }
                return false;
            /* }@ */
            case KeyEvent.KEYCODE_MENU:
                // Consume menu button presses during capture.
                return mMediaRecorderRecording;
        }
        return false;
    }

    @Override
    public boolean isVideoCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(action)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(action));
    }

    public boolean shouldShowResult() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.ACTION_VIDEO_CAPTURE.equals(action));
    }

    private void doReturnToCaller(boolean valid) {
        if(!played && mVideoFileDescriptor == null){
            Log.e(TAG, "VideoThumbnail loading is not complete");
            return;
        }
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = Activity.RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            resultCode = Activity.RESULT_CANCELED;
        }
        mActivity.setResultEx(resultCode, resultIntent);
        mActivity.finish();
    }

    private void cleanupEmptyFile() {
        if (mVideoFilename != null) {
            File f = new File(mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + mVideoFilename);
                mVideoFilename = null;
            }
        }
    }

    // Prepares media recorder.
    private void initializeRecorder() {
        Log.i(TAG, "initializeRecorder: " + Thread.currentThread());
        // If the mCameraDevice is null, then this activity is going to finish
        if (mCameraDevice == null) {
            Log.w(TAG, "null camera proxy, not recording");
            return;
        }

        updateCameraShutterSound();



        long requestedSizeLimit = 0;
        closeVideoFileDescriptor();//SPRD:fix bug941768
        requestedSizeLimit = getRequestedSizeLimit();
        mCurrentVideoUriFromMediaSaved = false;

        // SPRD: For bug 539723 add log
        Profile guard = Profilers.instance().guard("initializeRecorder");

        /*
         * SPRD Bug:590419 set 64-bit data file flag interface for camera use
         *
         * @{ Original Android code:
         * mMediaRecorder = new MediaRecorder();
         */
        mMediaRecorder = (MediaRecorder) DreamProxy.getMediaRecoder();
        /*
         * @}
         */
        // Unlock the camera object before passing it to media recorder.
        mCameraDevice.unlock();
        // We rely here on the fact that the unlock call above is synchronous
        // and blocks until it occurs in the handler thread. Thereby ensuring
        // that we are up to date with handler requests, and if this proxy had
        // ever been released by a prior command, it would be null.
        if (!useNewApi()) {
            Camera camera = mCameraDevice.getCamera();
            Log.i(TAG, "camera = " + camera);
            // If the camera device is null, the camera proxy is stale and recording
            // should be ignored.
            if (camera == null) {
                Log.w(TAG, "null camera within proxy, not recording");
                return;
            }

            mMediaRecorder.setCamera(camera);
        }

        /* SPRD: Fix bug 578587 that Video module does not support 4G+ video recording @{ */
        DreamProxy.setParam64BitFileOffset(!isVFat());
        /* @} */

        boolean isAudioSilence = isAudioSilence(mIsVideoCaptureIntent);
        boolean isAudioRecording = isAudioRecording();
        if (!isAudioSilence && !isAudioRecording){
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        }


        /* @} */

        if (useNewApi()) {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        } else {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        }
        // SPRD Bug:474701 Feature:Video Encoding Type.
        mProfile.videoCodec = getVideoEncodeType();

        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);

        if(!isAudioSilence && !isAudioRecording){
            mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
        }

        // SPRD: Fix bug1021537 , limit CIF time 30h
        int quality = getVideoQuality();
        Log.i(TAG, "quality for CIF is 3 = " + quality);
        if(quality == CamcorderProfile.QUALITY_CIF){
            mMediaRecorder.setMaxDuration(MAX_VIDEO_DURATION_FOR_CIF);
        } else {
            mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        }

        if (mCaptureTimeLapse) {
            // SPRD: Fix bug675012, get time lapse by videoFrameRate
            int timeBetweenTimeLapseFrameCaptureMs = (int) (1000.0f / (float) mProfile.videoFrameRate + 0.5f) * mTimeLapseMultiple;
            double fps = 1000 / (double) timeBetweenTimeLapseFrameCaptureMs;
            Log.d(TAG, "VideoModule.initializeRecorder mProfile.videoFrameRate:" +
                    mProfile.videoFrameRate + " fps:" + fps);
            setCaptureRate(mMediaRecorder, fps);
        }
        //Fix Bug631188 Video need know fps of slow motion
        String slowMotionValue = mDataModuleCurrent.getString(Keys.KEY_VIDEO_SLOW_MOTION);
        if (slowMotionValue != null) {
            int slow_motion = Integer.valueOf(slowMotionValue);
            /*
             * SPRD: Fix bug 663921 that slow motion record occurs error @{
             *
            double fps = slow_motion * 30;
             */
            double fps = slow_motion * mProfile.videoFrameRate;
            /* @} */
            setCaptureRate(mMediaRecorder, fps);
        }
        setRecordLocation();

        // Set output file.
        // Try Uri in the intent first. If it doesn't exist, use our own
        // instead.
        if (mVideoFileDescriptor != null) {
            mMediaRecorder.setOutputFile(mVideoFileDescriptor
                    .getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat, slowMotionValue);
            StorageUtil storageUtil = StorageUtil.getInstance();
            if (storageUtil.isInternalPath(mVideoFilename)) {
                mMediaRecorder.setOutputFile(mVideoFilename);
            } else {
                ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
                Uri uri = storageUtil.getCurrentAccessUri(mVideoFilename);
                mExternalVideoFD = externalStorageUtil.createParcelFd(mContentResolver,uri,mVideoFilename,"video/*");
                if (mExternalVideoFD != null) {
                    mMediaRecorder.setOutputFile(mExternalVideoFD.getFileDescriptor());
                }
            }
        }

        // Set maximum file size.
        long maxFileSize = mActivity.getStorageSpaceBytes()
                - Storage.LOW_STORAGE_THRESHOLD_BYTES;
        if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
            maxFileSize = requestedSizeLimit;
        }

        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (RuntimeException exception) {
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            // on the size restriction.
        }

        int sensorOrientation = mActivity.getCameraProvider()
                .getCharacteristics(mCameraId).getSensorOrientation();
        int deviceOrientation = mAppController.getOrientationManager()
                .getDeviceOrientation().getDegrees();
        int rotation = CameraUtil.getImageRotation(sensorOrientation,
                deviceOrientation, isCameraFrontFacing());
        /**
         * SPRD: Add for bug 585183, 3d video recoding @{
         * Original Code
         *
        mMediaRecorder.setOrientationHint();
         */
        mMediaRecorder.setOrientationHint(getRecorderOrientationHint(rotation));
        /* @} */
        //Sprd fix bug987497
        mRecordingSurface = MediaCodec.createPersistentInputSurface();
        mMediaRecorder.setInputSurface(mRecordingSurface);
        /*
        Bug 1052426 enable eois using MediaRecorder API(setVideoStabilization) for back camera
         */
        if (!isCameraFrontFacing() && CameraUtil.isEOISDvBackEnabled()) {
            if (mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EOIS_DV_BACK) && mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_BACK)) {
                Log.d(TAG, "back video eois = " + mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_BACK));
                try {
                    Class<?> mediaRecorder = Class.forName("android.media.MediaRecorder");
                    Method setVideoStabilization = mediaRecorder.getDeclaredMethod("setVideoStabilization");
                    setVideoStabilization.invoke(mMediaRecorder, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,"method setVideoStabilization does not found or something wrong");
                }

            }

        }
        /*Bug 1052426*/
        try {
            // SPRD: For bug 539723 add log
            guard.mark();
            mMediaRecorder.prepare();
            // SPRD: For bug 539723 add log
            guard.stop("MediaRecorder prepare");
        } catch (IOException e) {
            Log.e(TAG, "prepare failed for " + mVideoFilename, e);
            releaseMediaRecorder();
            mAppController.getCameraAppUI().setSwipeEnabled(true);// SPRD:fix
            // 527653
            mAppController.getFatalErrorHandler().onMediaStorageFailure();
            return;
        }

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
        Log.i(TAG, "initializeRecorder end");
    }

    private long getRequestedSizeLimit() {
        Intent intent = mActivity.getIntent();
        Bundle myExtras = intent.getExtras();
        if (mIsVideoCaptureIntent && myExtras != null) {
            Uri saveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor = mContentResolver.openFileDescriptor(
                            saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    // invalid uri
                    Log.e(TAG, ex.toString());
                }
            }
            return myExtras.getLong(MediaStore.EXTRA_SIZE_LIMIT);
        }
        return 0;
    }

    private boolean isVFat() {
        boolean vFatType = false;
        String storagePath = StorageUtil.getInstance().getFileDir();
        StorageManager storageManager = mActivity.getSystemService(StorageManager.class);
        List<VolumeInfo> vols = storageManager.getVolumes();

        for (VolumeInfo vol : vols) {
            if (vol != null) {
                String fsType = vol.fsType;
                String path = vol.path;
                if (null != path && "vfat".equals(fsType) && storagePath.startsWith(path)) {
                    showNoSupportHint();
                    vFatType = true;
                    break;
                }
            }
        }
        return vFatType;
    }

    /* SPRD: Fix bug 625678 add hint when sd card supports 4k recording at the most in the first*/
    public void showNoSupportHint() {
        boolean shouldNoSupportHint = mDataModule.getBoolean(Keys.KEY_CAMERA_VIDEO_HINT);
        if (shouldNoSupportHint == true) {
            CameraUtil.toastHint(mActivity,R.string.video_maximum_size_limit);
            mDataModule.set(Keys.KEY_CAMERA_VIDEO_HINT, false);
        }
    }
    /* @} */

    private void setRecordLocation() {
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            mMediaRecorder.setLocation((float) loc.getLatitude(),
                    (float) loc.getLongitude());
        }
    }

    private void releaseMediaRecorder() {
        Log.i(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mVideoFilename = null;
    }

    private void generateVideoFilename(int outputFileFormat, String videoType) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        // Used when emailing.
        String filename = title
                + convertOutputFormatToFileExt(outputFileFormat);
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        /*
         * SPRD: Change storage videopath for storage path Feature String path =
         * SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{
         * Original Code
         * Storage.DIRECTORY + '/' + filename;
        StorageUtil storageUtil = StorageUtil.getInstance();
        String path = storageUtil.getFileDir() + '/' + filename;
         */
        String path;
        if (CameraUtil.isVideoExternalEnable()) {
            path = getFileDir() + '/' + filename;
        } else {
            path = StorageUtil.getInstance().getInternalFileDir() + '/' + filename;
            /* SPRD: Fix bug 1027008 check InternalFileDir*/
            File dir = new File(path).getParentFile();
            if (!dir.exists())
                dir.mkdir();
            /* @} */
        }
        /* @} */
        String tmpPath = path + ".tmp";
        mCurrentVideoValues = new ContentValues(10);
        mCurrentVideoValues.put(Video.Media.TITLE, title);
        mCurrentVideoValues.put(Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mCurrentVideoValues.put(Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues.put(Video.Media.DATA, path);
        mCurrentVideoValues.put(Video.Media.WIDTH, mProfile.videoFrameWidth);
        mCurrentVideoValues.put(Video.Media.HEIGHT, mProfile.videoFrameHeight);
        mCurrentVideoValues.put(
                Video.Media.RESOLUTION,
                Integer.toString(mProfile.videoFrameWidth) + "x"
                        + Integer.toString(mProfile.videoFrameHeight));
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            mCurrentVideoValues.put(Video.Media.LATITUDE, loc.getLatitude());
            mCurrentVideoValues.put(Video.Media.LONGITUDE, loc.getLongitude());
        }
        if (videoType != null){
            mCurrentVideoValues.put("file_flag", DreamModule.TYPE_SLOW_MOTION);
        } else {
            mCurrentVideoValues.put("file_flag", DreamModule.TYPE_VIDEO);
        }
        mVideoFilename = tmpPath;
        Log.v(TAG, "New video filename: " + mVideoFilename);
    }

    public String getFileName(long timeValue){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date(timeValue);
        String dateStr = simpleDateFormat.format(date);
        return "VID_" + dateStr;
    }

    public String getPath(String curName){
        if(curName != null) {
            return curName.substring(0, curName.length() - 23);
        }
        return null;
    }

    long durationTime;
    private void saveVideo() {
        if (mVideoFileDescriptor != null)
            return;
        long curTime = System.currentTimeMillis();
        final String finalName = mCurrentVideoValues.getAsString(Video.Media.DATA);
        final String newName = mCurrentVideoValues.getAsString(Video.Media.DISPLAY_NAME);
        final String titleName = getFileName(curTime);
        final String dataName = getPath(finalName) + titleName + ".mp4";
        final String displayName = titleName + ".mp4";
        mCurrentVideoValues.put(Video.Media.SIZE, new File(mCurrentVideoFilename).length());
        mCurrentVideoValues.put(MediaColumns.DATE_MODIFIED, curTime / 1000);
        mCurrentVideoValues.put(Video.Media.DATE_TAKEN, curTime);
        mCurrentVideoValues.put(Video.Media.TITLE,titleName);
        mCurrentVideoValues.put(Video.Media.DATA,dataName);
        mCurrentVideoValues.put(Video.Media.DISPLAY_NAME,displayName);

        final String currentVideoFilename = mCurrentVideoFilename;
        final int snapshotCount = mSnapshotCount;
        final ContentValues currentVideoValues = mCurrentVideoValues;
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... voids) {
                //Sprd:fix bug959107
                // use MediaMetadataRetriever
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(currentVideoFilename);
                    // get duration time
                    durationTime = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    // get thumbnail bitmap
                    long beforStopTime = SystemClock.uptimeMillis();
                    Bitmap bitmap = retriever.getFrameAtTime();
                    Log.i(TAG, "get video thumbnail cost:"+(SystemClock.uptimeMillis() - beforStopTime));
                    mActivity.startPeekAnimation(bitmap);
                } catch (Exception e) {
                    Log.e(TAG, "fail when use MediaMetadataRetriever" + e.getMessage());
                } finally {
                    retriever.release();
                }
                StorageUtil storageUtil = StorageUtil.getInstance();
                ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
                long start = System.currentTimeMillis();
                File finalFile = new File(dataName);
                if (storageUtil.isInternalPath(finalName)) {
                    new File(currentVideoFilename).renameTo(finalFile);
                } else {
                    Uri uri = storageUtil.getCurrentAccessUri(currentVideoFilename);
                    externalStorageUtil.renameDocument(mContentResolver,uri,
                            "DCIM/Camera/" + newName + ".tmp",displayName);
                }
                long cost = System.currentTimeMillis() - start;
                if (cost > 200) {
                    Log.i(TAG, "renameTo cost " + cost);
                }
                currentVideoValues.put(Video.Media.DURATION, durationTime);
                Log.d(TAG, "saveVideo duration = " + durationTime);
                getServices().getMediaSaver().addVideo(dataName, currentVideoValues, mOnVideoSavedListener);
                /*
                write xmpdate
                 */
                if ((currentVideoValues.getAsInteger("file_flag")).intValue() == DreamModule.TYPE_SLOW_MOTION) {
                    Log.i(TAG,"save slow motion video, add xmpMeta to file for google photos start");
                    long writexmpcost = System.currentTimeMillis() - start;
                    XmpBuilder builder = Storage.setSpecialTypeId(DreamModule.TYPE_SLOW_MOTION);
                    Log.i(TAG,"slow motion builder is "+builder);
                    XMPMeta xmpMeta = null;
                    if (builder != null) {
                        try {
                            xmpMeta = builder.build();
                        } catch (XMPException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (xmpMeta != null) {
                        XmpUtil.writeXMPMeta(finalName, xmpMeta);
                        Log.i(TAG,"save slow motion video, add xmpMeta to file for google photos end");
                    }
                    if (writexmpcost > 200) {
                        Log.i(TAG, "slow motion writexmp cost " + writexmpcost);
                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        /*
        File finalFile = new File(finalName);
        if (new File(mCurrentVideoFilename).renameTo(finalFile)) {
            final ContentValues contentValuesForThread = new ContentValues(mCurrentVideoValues);
            final String filenameForThread = mCurrentVideoFilename;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // use MediaMetadataRetriever
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(finalName);

                        // get duration time
                        long durationTime = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        contentValuesForThread.put(Video.Media.DURATION, durationTime);
                        Log.d(TAG, "saveVideo duration = " + durationTime);

                        getServices().getMediaSaver().addVideo(filenameForThread , contentValuesForThread , mOnVideoSavedListener);

                        // show video's thumbnail just when no snapshot ; if there's snapshot, snapshot will show pic's thumbnail
                        if (mSnapshotCount == 0) {
                            // get thumbnail bitmap
                            Bitmap bitmap = retriever.getFrameAtTime();
                            mActivity.startPeekAnimation(bitmap);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "fail when use MediaMetadataRetriever" + e.getMessage());
                    } finally {
                        retriever.release();
                    }
                }

            }).start();
        }
        */
        mCurrentVideoValues = null;
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    // from MediaRecorder.OnErrorListener
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            stopVideoRecording();
            mActivity.updateStorageSpaceAndHint(null);
        }
    }

    // from MediaRecorder.OnInfoListener
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            if (mMediaRecorderRecording) {
                onStopVideoRecording();
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mMediaRecorderRecording) {
                onStopVideoRecording();
            }

            // Show the toast.
            CameraUtil.toastHint(mActivity, R.string.video_reach_size_limit);
        }
        /* SPRD: Add for bug 559531 @{ */
//        else if (what == MediaRecorder.MEDIA_RECORDER_INFO_TRACKS_HAVE_DATA) {
//            if (mHandler.hasMessages(MSG_ENABLE_SHUTTER_PAUSE_BUTTON)) {
//                mHandler.removeMessages(MSG_ENABLE_SHUTTER_PAUSE_BUTTON);
//            }
//            mHandler.sendEmptyMessage(MSG_ENABLE_SHUTTER_PAUSE_BUTTON);
//        }
        /* @} */
    }

    /*
     * Make sure we're not recording music playing in the background, ask the
     * MediaPlaybackService to pause playback.
     */
    /*
     * SPRD: fix bug492439 Ture on FM, Camera can not record @{
     * original code

    private void silenceSoundsAndVibrations() {
        // Get the audio focus which causes other music players to stop.
        // SPRD:fix bug514208 when phone rings,camere can't record
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        // Store current ringer mode so we can set it once video recording is
        // finished.
        mOriginalRingerMode = mAudioManager.getRingerMode();
        // Make sure no system sounds and vibrations happen during video
        // recording.
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }
     */
    private boolean silenceSoundsAndVibrations() {
        // Get the audio focus which causes other music players to stop.
        int ret = mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        Log.i(TAG, "requestAudioFocus " + ret);
        if (ret != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return false;
        }
        // Store current ringer mode so we can set it once video recording is
        // finished.
        mOriginalRingerMode = mAudioManager.getRingerMode();
        // TODO: Use new DND APIs to properly silence device

        return true;
    }
    /* @} */

    private void restoreRingerMode() {
        // First check if ringer mode was changed during the recording. If not,
        // re-set the mode that was set before video recording started.
        if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            // TODO: Use new DND APIs to properly restore device notification/alarm settings
        }
    }

    // For testing.
    public boolean isRecording() {
        return mMediaRecorderRecording;
    }

    private Runnable mMediaRecoderStartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPaused || mCameraDevice == null || mMediaRecorder == null) {
                Log.w(TAG, "Could not run mMediaRecoderStartRunnable");
                return;
            }
            try {
                /* SPRD:fix 520894 silence the value of STREAM_MUSIC @{*/
                /* SPRD:fix bug492439 Ture on FM, Camera can not record@{*/
                /* SPRD:fix bug514208 when phone rings,camere can't record@{*/
                if (!silenceSoundsAndVibrations()) {
                    // Fix bug 540246 recording and music work together after we end call
                    mActivity.sendBroadcast(new Intent(PAUSE_ACTION));
                }
                /*}@*/
//                if (useNewApi() && mShutterSoundEnable) {
//                    if (mCameraSound != null) {
//                        mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
//                    }
//                }
                mActivity.resetHasKeyEventEnter();
                mMediaRecorder.start(); // Recording is now started
            } catch (IllegalStateException exception) {
                Log.e(TAG, "Could not start media recorder(start failed). ", exception);
                mAppController.getCameraAppUI().setSwipeEnabled(true);
                CameraUtil.toastHint(mActivity, R.string.recorder_in_progress_error);
                isShutterButtonClicked = false;
                mAppController.getCameraAppUI()
                        .setShouldSuppressCaptureIndicator(false);
                mUI.showRecordingUI(false, mActivity.getOrientationManager()
                        .getDisplayRotation().getDegrees());
                setAELockPanel(true);
                releaseMediaRecorder();
                restoreRingerMode();
                if (useNewApi()) {
                    mCameraDevice.setStartVideoCallback(null);
                    stopPreview();
                    startPreview(false);
                }
                enableShutterAndPauseButton(true);
                mCameraDevice.lock();
                return;
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
                mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                mAppController.getFatalErrorHandler().onGenericCameraAccessFailure();
                releaseMediaRecorder();
                /* SPRD:fix 520894 recover the value of STREAM_MUSIC @{*/
                restoreRingerMode();
                /*}@*/
                // If start fails, frameworks will not lock the camera for us.
                if (useNewApi()) {
                    mCameraDevice.setStartVideoCallback(null);
                    stopPreview();
                    startPreview(false);
                }
                mCameraDevice.lock();
                return;
            }
            // Make sure we stop playing sounds and disable the
            // vibrations during video recording. Post delayed to avoid
            // silencing the recording start sound.
            /* SPRD:fix bug492439 Ture on FM, Camera can not record@{
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    silenceSoundsAndVibrations();
                }
            }, 250);
            }@*/
            /* SPRD:fix 527653 Start recording, Camera can not swipe. @{
            mAppController.getCameraAppUI().setSwipeEnabled(false);
            }@*/

            // The parameters might have been altered by MediaRecorder already.
            // We need to force mCameraDevice to refresh before getting it.
            mCameraDevice.refreshSettings();
            // The parameters may have been changed by MediaRecorder upon starting
            // recording. We need to alter the parameters if we support camcorder
            // zoom. To reduce latency when setting the parameters during zoom, we
            // update the settings here once.
            mCameraSettings = mCameraDevice.getSettings();

            // SPRD Bug:474704 Feature:Video Recording Pause.
            mResultTime = 0;
            mAllPauseTime = 0;

            mMediaRecorderRecording = true;

                    /*
                     * SPRD Bug:519334 Refactor Rotation UI of Camera.
                     *
                     * @{ Original Android code:
                     * mActivity.lockOrientation();
                     */

            mRecordingStartTime = SystemClock.uptimeMillis();

                    /*
                     * SPRD Bug:529008 Animate just once. @{ Original
                     * Android code: mAppController.getCameraAppUI
                     * ().animateBottomBarToVideoStop
                     * (R.drawable.ic_stop);
                     */
            // ui check 81
            mAppController.getCameraAppUI()
                    .animateBottomBarToVideoStop(
                            mShutterIconId, R.drawable.dream_ic_capture_video_stop);
                    /* @} */

                    /* SPRD: Fix bug 559531 @{ */
            enableShutterAndPauseButton(false);
            if (useNewApi()) {
                mAppController.setShutterEnabled(true);
            }
            // bug 612193 pause video at 0 second can be done after stop video ant 1 second
            if (mHandler.hasMessages(MSG_ENABLE_SHUTTER_PAUSE_BUTTON)) {
                mHandler.removeMessages(MSG_ENABLE_SHUTTER_PAUSE_BUTTON);
            }
            mHandler.sendEmptyMessageDelayed(
                    MSG_ENABLE_SHUTTER_PAUSE_BUTTON, ENABLE_SHUTTER_PAUSE_BUTTON_TIMEOUT);

            if (!isAELock) {
                setFocusParameters();
            }

            updateRecordingTime();
            mActivity.enableKeepScreenOn(true);
            /* SPRD: Dream Camera ui check 71 @{ */
            if (disableSnapShotInVideo()) {//SPRD:fix bug 615391 EOIS and 4k not support snap shot
                mAppController.getCameraAppUI().setSlowMotionCaptureButtonDisable();
            }
            /* @} */

            // SPRD: Fix bug 650297 that video thumbnail is showed slowly
            mSnapshotCount = 0;
        }
    };

    protected void startVideoRecording() {
        Log.i(TAG, "startVideoRecording: " + Thread.currentThread());
        setCameraState(SNAPSHOT_IN_PROGRESS);

         /*
          * SPRD Bug:474704 Feature:Video Recording Pause. @{
          * Original Android code: mUI.showRecordingUI(true);
          */
        mUI.showRecordingUI(true, mActivity
                .getOrientationManager()
                .getDisplayRotation().getDegrees());
         /* @} */

        setAELockPanel(false);
        mUI.cancelAnimations();
        /*
         * SPRD:fix 527653 Monkey is so fast that 2ms later after
         * startVideoRecording called, it shows the filmStrip, and then edit a
         * photo, which will take a long time, which caused ANR. setSwipeEnabled
         * at the begin of startVideoRecording to avoid ANR. @{
         */
        mAppController.getCameraAppUI().setSwipeEnabled(false);
        /* }@ */
        mUI.setSwipingEnabled(false);
        mUI.hidePassiveFocusIndicator();
        mAppController.getCameraAppUI().hideCaptureIndicator();
        mAppController.getCameraAppUI().setShouldSuppressCaptureIndicator(true);
        mAppController.getCameraAppUI().dismissDialogIfNecessary();//Sprd:fix bug841813
        mUI.updatePortraitBackgroundReplacementUI(View.GONE);
        mUI.showFaceDetectedTips(false);
        mPendingUpdateStorageSpace = true;
        mActivity.updateStorageSpaceAndHint(new CameraActivity.OnStorageUpdateDoneListener() {
            @Override
            public void onStorageUpdateDone(long bytes) {
                mPendingUpdateStorageSpace = false;
                if ((bytes << 20) <= (Storage.LOW_STORAGE_THRESHOLD_BYTES << 20)) {
                    // use MB instead of B to compare. B >> 20 = MB
                    Log.w(TAG, "Storage issue, ignore the start request");
                    /* SPRD: fix bug 538868 video may not switch camera@{*/
                    //mAppController.getCameraAppUI().enableCameraToggleButton();
                    /* @} */
                    mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                    isShutterButtonClicked = false;
                    mAppController.getCameraAppUI()
                            .setShouldSuppressCaptureIndicator(false);
                    mUI.showRecordingUI(false, mActivity.getOrientationManager()
                            .getDisplayRotation().getDegrees());
                    setAELockPanel(true);
                } else {
                    if (mCameraDevice == null) {
                        Log.v(TAG, "in storage callback after camera closed");
                        mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                        /* SPRD: fix bug 538868 video may not switch camera@{*/
                        //mAppController.getCameraAppUI().enableCameraToggleButton();
                        /* @} */
                        return;
                    }
                    if (mPaused == true) {
                        Log.v(TAG, "in storage callback after module paused");
                        mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                        /* SPRD: fix bug 538868 video may not switch camera@{*/
                        //mAppController.getCameraAppUI().enableCameraToggleButton();
                        /* @} */
                        return;
                    }

                    // Monkey is so fast so it could trigger startVideoRecording twice. To prevent
                    // app crash (b/17313985), do nothing here for the second storage-checking
                    // callback because recording is already started.
                    if (mMediaRecorderRecording) {
                        Log.v(TAG, "in storage callback after recording started");
                        mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                        /* SPRD: fix bug 538868 video may not switch camera@{*/
                        //mAppController.getCameraAppUI().enableCameraToggleButton();
                        /* @} */
                        return;
                    }

                    mCurrentVideoUri = null;

                    initializeRecorder();
                    if (mMediaRecorder == null) {
                        Log.e(TAG, "Fail to initialize media recorder");
                        mAppController.getCameraAppUI().setSwipeEnabled(true);//SPRD:fix 527653
                        /* SPRD: fix bug 538868 video may not switch camera@{*/
                        //mAppController.getCameraAppUI().enableCameraToggleButton();
                        /* @} */
                        return;
                    }
                    if (!useNewApi()) {
                        mActivity.runOnUiThread(mMediaRecoderStartRunnable);
                    } else {
                        mMediaRecorderPrepareing = true;
                        if (useNewApi() && mShutterSoundEnable) {              //bugfix 922875
                            if (mCameraSound != null) {
                                mCameraSound.play(MediaActionSound.START_VIDEO_RECORDING);
                            }
                        }
			            mHandler.sendEmptyMessageDelayed(MSG_CAMERA_VIDEO_START, CAMERA_VIDEO_START_TIMEOUT);
                    }
                }
            }
        });

    }

    private boolean disableSnapShotInVideo() {
        return (isSlowMotionOn() || isEOISOn() ||!isTakeSnapShot() || is3dnrOn());
    }

    @Override
    public void onVideoStart() {
        mMediaRecorderPrepareing = false;
        Log.d(TAG , "onVideoStart , mPaused = " + mPaused); // add log for Bug 915151. callback be set to null after null-judgement
        if (mActivity != null && !mPaused && mCameraDevice != null && mMediaRecorder != null) {
            mActivity.runOnUiThread(mMediaRecoderStartRunnable);
        }
    }

    private Bitmap getVideoThumbnail() {
        Bitmap bitmap = null;
        if (mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(
                    mVideoFileDescriptor.getFileDescriptor(),
                    mDesiredPreviewWidth);
        } else if (mCurrentVideoUri != null) {
            try {
                mVideoFileDescriptor = mContentResolver.openFileDescriptor(
                        mCurrentVideoUri, "r");
                bitmap = Thumbnail.createVideoThumbnailBitmap(
                        mVideoFileDescriptor.getFileDescriptor(),
                        mDesiredPreviewWidth);
            } catch (java.io.FileNotFoundException ex) {
                // invalid uri
                Log.e(TAG, ex.toString());
            }
        }

        if (bitmap != null) {
            // MetadataRetriever already rotates the thumbnail. We should rotate
            // it to match the UI orientation (and mirror if it is front-facing
            // camera).
            bitmap = CameraUtil.rotateAndMirror(bitmap, 0,
                    isCameraFrontFacing());
        }
        return bitmap;
    }

    private void showCaptureResult() {
        mIsInReviewMode = true;
        played = false;
        Bitmap bitmap = getVideoThumbnail();
        if (bitmap != null) {
            mUI.showReviewImage(bitmap);
        }
        mUI.showReviewControls();
    }

    /* SPRD: Fix bug548010 Camera occurs error when connect USB during video recording @{ */
    private boolean stopVideoRecording() {
        return stopVideoRecording(true,true);
    }

    /* SPRD: Fix bug548010/685517 Camera occurs error when connect USB during video recording @{
     * original code
    private boolean stopVideoRecording() {
     */
    // Sprd : fix bug870406
    private Counter mCounterWaitSnapshot = new Counter(0);
    protected boolean stopVideoRecording(boolean shouldSaveVideo,boolean checkStorage) {
    /* @} */
        // Do nothing if camera device is still capturing photo. Monkey test can trigger app crashes
        // (b/17313985) without this check. Crash could also be reproduced by continuously tapping
        // on shutter button and preview with two fingers.
        /*
         * SPRD Bug:510390 open error when snap shot is processing onpause@{
         * Original Android code: if (mSnapshotInProgress) {
         */
        /* SPRD: fix for bug 537147 video recording and pull SD card, camera can not switch the camera @{ */
        //mAppController.getCameraAppUI().enableCameraToggleButton();
        /* @} */
        if (mSnapshotInProgress && !mPaused) {
            /* @} */
            Log.v(TAG, "Skip stopVideoRecording since snapshot in progress");
            return true;
        }
        Log.i(TAG, "stopVideoRecording");

        // Re-enable sound as early as possible to avoid interfering with stop
        // recording sound.
        abandonAudioPlayback();
        restoreRingerMode();

        mUI.setSwipingEnabled(true);
        /*
         * SPRD:fix bug 501841 video record stop,focus ring shows
         * mUI.showPassiveFocusIndicator();
         */

        enableShutterAndPauseButton(true);
        mAppController.getCameraAppUI()
                .setShouldSuppressCaptureIndicator(false);
        mUI.updatePortraitBackgroundReplacementUI(View.VISIBLE);
        mUI.showFaceDetectedTips(true);
        boolean fail = false;
        if (mMediaRecorderRecording) {
            boolean shouldAddToMediaStoreNow = false;

            try {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                // Sprd : fix bug870406
                if (mSnapshotInProgress) {
                    Log.i(TAG, "Snapshot is in progress, wait until onpictureTaken");
                    //Sprd:Fix bug904189
                    mCounterWaitSnapshot.waitCount(2500);
                }

                // SPRD Bug:474704 Feature:Video Recording Pause.
                long beforStopTime = SystemClock.uptimeMillis();
                Log.i(TAG, "MediaRecorder.stop ......    starttime = "
                        + beforStopTime);
                mMediaRecorder.stop();

                // SPRD Bug:474704 Feature:Video Recording Pause.
                long afterStopTime = SystemClock.uptimeMillis();
                mResultTime += (afterStopTime - beforStopTime);
                Log.i(TAG, "MediaRecorder.stop ......    endtime = "
                        + (afterStopTime - beforStopTime));

                shouldAddToMediaStoreNow = true;
                mCurrentVideoFilename = mVideoFilename;
                Log.v(TAG, "stopVideoRecording: current video filename: "
                        + mCurrentVideoFilename);
            } catch (RuntimeException e) {
                Log.e(TAG, "stop fail", e);
                if (mVideoFilename != null) {
                    deleteVideoFile(mVideoFilename);
                }
                fail = true;
            }
            if (useNewApi() && mShutterSoundEnable) {//Sprd:fix bug959107
                CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mCameraSound != null) {
                            mCameraSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                        }
                    }
                });
            }
            mMediaRecorderRecording = false;
            isShutterButtonClicked = false;
            mMediaRecorderPrepareing = false;
            /*
             * SPRD Bug:519334 Refactor Rotation UI of Camera. @{ Original
             * Android code: mActivity.unlockOrientation();
             */

            // If the activity is paused, this means activity is interrupted
            // during recording. Release the camera as soon as possible because
            // face unlock or other applications may need to use the camera.
            if (mPaused) {
                // b/16300704: Monkey is fast so it could pause the module while
                // recording.
                // stopPreview should definitely be called before switching off.
                stopPreview();
                closeCamera();
            }

            /*
             * SPRD Bug:474704 Feature:Video Recording Pause. @{ Original
             * Android code: mUI.showRecordingUI(false);
             */
            mUI.showRecordingUI(false, mActivity.getOrientationManager()
                    .getDisplayRotation().getDegrees());
            /* @} */
            setAELockPanel(true);

            // The orientation was fixed during video recording. Now make it
            // reflect the device orientation as video recording is stopped.
            mUI.setOrientationIndicator(0, true);
            mActivity.enableKeepScreenOn(false);
            /* SPRD: fix bug548010 Camera occurs error when connect USB during video recording @{
            if (shouldAddToMediaStoreNow && !fail) {
             */
            saveOrShowCaptureResult(shouldAddToMediaStoreNow, fail, shouldSaveVideo);
            if (useNewApi() && !mPaused) {//Sprd:fix bug959107
                mCameraDevice.setStartVideoCallback(null);
                stopPreview();
                startPreview(false);
            }
            if (mRecordingSurface != null) {
                mRecordingSurface.release();
                mRecordingSurface = null;
            }
        }
        // release media recorder
        releaseMediaRecorder();
        if (mExternalVideoFD != null) {
            try {
                long start = System.currentTimeMillis();
                mExternalVideoFD.close();
                long cost = System.currentTimeMillis() - start;
                if (cost > 100) {
                    Log.i(TAG, "closeExternalVideoFD cost: " + cost);
                }
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            mExternalVideoFD = null;
        }

        if (useNewApi()) {
//            startPreview(false);
        }

        /*
         * SPRD Bug:529008 Animate just once. @{ Original Android code:
         * mAppController
         * .getCameraAppUI().animateBottomBarToFullSize(mShutterIconId);
         */

        mAppController.getCameraAppUI().animateBottomBarToFullSize(R.drawable.dream_ic_capture_video_stop,
                mShutterIconId);
        /* @} */

        mAppController.getCameraAppUI().setSwipeEnabled(true);// SPRD:Fix
        // bug391820

        if (!mPaused && mCameraDevice != null) {
            if (!isAELock) {
                setFocusParameters();
            }
            mCameraDevice.lock();
            if (!ApiHelper.HAS_SURFACE_TEXTURE_RECORDING) {
                stopPreview();
                // Switch back to use SurfaceTexture for preview.
                startPreview(true);
            }
            // Update the parameters here because the parameters might have been
            // altered
            // by MediaRecorder.
            mCameraSettings = mCameraDevice.getSettings();
        }

        // Check this in advance of each shot so we don't add to shutter
        // latency. It's true that someone else could write to the SD card
        // in the mean time and fill it, but that could have happened
        // between the shutter press and saving the file too.
        if(checkStorage){
            mActivity.updateStorageSpaceAndHint(null);
        }

        // SPRD Bug:474704 Feature:Video Recording Pause.
        mPauseRecorderRecording = false;
        return fail;
    }

    private void saveOrShowCaptureResult(boolean shouldAddToMediaStoreNow, boolean stopFail, boolean shouldSaveVideo) {
        if (shouldAddToMediaStoreNow && !stopFail && shouldSaveVideo) {
            /* @} */
            if (mVideoFileDescriptor == null) {
                saveVideo();
            } else if (shouldShowResult()) {
                // if no file save is needed, we can show the post capture
                // UI now
                showCaptureResult();
            }
        }
    }

    private void updateRecordingTime() {
        if (!mMediaRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();

        /*
         * SPRD Bug:474704 Feature:Video Recording Pause. @{ Original Android
         * code: long delta = now - mRecordingStartTime;
         */
        long delta = now - mRecordingStartTime - mResultTime;
        /* @} */

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0 && delta >= mMaxVideoDurationInMs - 60000);

        long deltaAdjusted = delta;
        if (countdownRemainingTime) {
            deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
        }
        String text;

        long targetNextUpdateDelay;

        text = millisecondToTimeString(deltaAdjusted, false);
        targetNextUpdateDelay = 1000;

        mUI.setRecordingTime(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = mActivity.getResources().getColor(
                    R.color.recording_time_remaining_text);

            mUI.setRecordingTimeTextColor(color);
        }

        long actualNextUpdateDelay = targetNextUpdateDelay
                - (delta % targetNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_RECORD_TIME,
                actualNextUpdateDelay);
    }
    /* @} */

    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keys) {
        Log.e(TAG, "dream video module onDreamSettingChangeListener  ");
        if (mCameraDevice == null) {
            return;
        }
        for (String key : keys.keySet()) {
            Log.e(TAG,
                    "onSettingChanged key = " + key + " value = "
                            + keys.get(key));

            if (onlyRestartPreview(key,isCameraFrontFacing())) {
                //fourk recording do not support 8x zoom. support 4x zoom only
                if (isRecordingIn4k()){
                    mMaxRatio = CameraUtil.getZoomMaxInFourKVideo(mCameraCapabilities.getMaxZoomRatio());
                } else {
                    mMaxRatio = mCameraCapabilities.getMaxZoomRatio();
                }
                if (mZoomValue > mMaxRatio) {
                    mZoomValue = mMinRatio;
                    mCameraSettings.setZoomRatio(mZoomValue);
                }
                mUI.initializeZoom(mCameraSettings, mCameraCapabilities);
                restartPreview();
                return;
            }
            updateParaAboutKey(key);
        }

        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
            mCameraSettings = mCameraDevice.getSettings();
        }
        // Update UI based on the new parameters.
        mUI.updateOnScreenIndicators(mCameraSettings);
    }

    private boolean onlyRestartPreview(String key, boolean isFront) {
        return (isFront && (key.equals(Keys.KEY_VIDEO_QUALITY_FRONT_3D) || key.equals(Keys.KEY_VIDEO_QUALITY_FRONT))
                || (!isFront && key.equals(Keys.KEY_VIDEO_QUALITY_BACK)) || key.equals(Keys.KEY_VIDEO_QUALITY_BACK_MACRO));
    }

    private void updateParaAboutKey(String key) {
        switch (key) {
//                case Keys.KEY_VIDEO_ENCODE_TYPE:
//                    break;
            case Keys.KEY_VIDEO_ANTIBANDING:
                updateParametersAntibanding();
                break;
//            case Keys.KEY_DREAM_COMPOSITION_LINE:
//                break;
            case Keys.KEY_VIDEO_WHITE_BALANCE:
                updateParametersWhiteBalance();
                break;
            case Keys.KEY_VIDEO_COLOR_EFFECT:
                updateParametersColorEffect();
                break;
//            case Keys.KEY_CAMERA_VIDEO_STABILIZATION:
//                break;
//            case Keys.KEY_CAMERA_MICROPHONE_SWITCH:
//                break;
            case Keys.KEY_VIDEO_SLOW_MOTION:
                setSlowmotionParameters();
                break;
            case Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL:
                updateTimeLapse();// SPRD: for bug 509708 add time lapse
                break;
            case Keys.KEY_VIDEOCAMERA_FLASH_MODE:
                if (mPaused) {
                    break;
                }
                // Update flash parameters.
                enableTorchMode(true);
                break;
            case Keys.KEY_ADJUST_FLASH_LEVEL:
                updateParametersFlashLevel();
                break;
            case Keys.KEY_EOIS_DV_FRONT:
            case Keys.KEY_EOIS_DV_BACK:
                //updateParametersEOIS();
                restartPreview();
                break;
            //nj dream camera test 70
            case Keys.KEY_CAMERA_GRID_LINES:
                updateParametersGridLine();
                break;
            case Keys.KEY_VIDEO_BEAUTY_ENTERED:
                updateMakeUpDisplay();
                updateMakeLevel();
                break;
            case Keys.KEY_ADD_LEVEL:
                updateLevel();
                if(!getHasLevelAngle() && mDataModuleCurrent.getBoolean(Keys.KEY_ADD_LEVEL)){
                    mUI.makeLevelAppear();
                }
                break;
            case Keys.KEY_AUTO_TRACKING:
                updateParametersAutoChasingRegion();
                break;
            case Keys.KEY_CAMERA_VIDEO_FACE_DETECT:
                updateFace(false);
                break;
            default:
                break;
        }
    }

    private void restartPreview() {
        if(mAELockPanel != null){
            mAELockPanel.hide();
        }

        if(isAELock){
            Log.d(TAG,"restart and reset ae/af lock");
            setAELock(false);
            if(mFocusManager != null) {
                mFocusManager.resetTouchFocus();
            }
            cancelAutoFocus();
        }

        stopPreview();
        mFocusManager.resetTouchFocus();
        if (!isVideoCaptureIntent() && !isUseSurfaceView())
        mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();
        /* SPRD: fix bug620875 need make sure freeze screen draw on Time @{ */
        mHandler.post(new Runnable() {
            public void run() {
                if (!mPaused && mCameraDevice != null) {
                    startPreview(false);
                }
            }
        });
        /* @} */
    }

    protected void updateFpsRange() {
        if (CameraUtil.isDynamicFpsEnable()) {
            int[] fpsRange = CameraUtil
                    .getPhotoPreviewFpsRange(mCameraCapabilities, useNewApi(), true);
            if (fpsRange != null && fpsRange.length > 0) {
                mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                Log.i(TAG, "preview fps: " + fpsRange[0] + ", " + fpsRange[1]);
            }
        } else {
            mCameraSettings.setPreviewFrameRate(mProfile.videoFrameRate);
        }
    }

    @SuppressWarnings("deprecation")
    private void setCameraParameters() {
        // Update Desired Preview size in case video camera resolution has
        // changed.
        mCameraSettings.setDefault(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD == CameraUtil.getFrontFlashMode(), mCameraCapabilities);//SPRD:fix bug616836 add for api1 use reconnect
        mCameraSettings.setEnterVideoMode(true);
        readVideoPreferences();
        updateDesiredPreviewSize();

        Size previewSize = new Size(mDesiredPreviewWidth, mDesiredPreviewHeight);
        mCameraSettings.setPreviewSize(previewSize.toPortabilitySize());
        // This is required for Samsung SGH-I337 and probably other Samsung S4
        // versions
        if (Build.BRAND.toLowerCase().contains("samsung")) {
            mCameraSettings.setSetting("video-size", mProfile.videoFrameWidth
                    + "x" + mProfile.videoFrameHeight);
        }
        /**
         * SPRD:fix bug 657675 modify the frame rate setting
        int[] fpsRange = CameraUtil.getMaxPreviewFpsRange(mCameraCapabilities
                .getSupportedPreviewFpsRange());
        if (fpsRange.length > 0) {
            mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        } else {
            mCameraSettings.setPreviewFrameRate(mProfile.videoFrameRate);
        }
        */
        updateFpsRange();


        if (mActivity.getCameraAppUI().getFilmstripVisibility() != View.VISIBLE) {
            enableTorchMode(CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing()), false);
        }

        // Set zoom.
        if (mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            if (mZoomRatioSection != null && mZoomRatioSection[0] > 0){
                mCameraSettings.setZoomMin(mZoomRatioSection[0]);
            }
            mCameraSettings.setZoomRatio(mZoomValue);
        }
        updateFocusParameters();

        setAutoTrackingIfSupported();
        /* SPRD Bug: 495676 update antibanding */
        updateParametersAntibanding();

        /* SPRD: Fix bug 535139, update color effect */
        updateParametersColorEffect();
        updateLevel();
        /* SPRD: Fix bug 535139, update white balance */
        updateParametersWhiteBalance();

        updateAELockExposureCompensation();

        mCameraSettings.setRecordingHintEnabled(true);

        if (mCameraCapabilities
                .supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
            mCameraSettings.setVideoStabilization(true);
        }

        // Set picture size.
        // The logic here is different from the logic in still-mode camera.
        // There we determine the preview size based on the picture size, but
        // here we determine the picture size based on the preview size.
        List<Size> supported = Size.convert(mCameraCapabilities
                .getSupportedPhotoSizes());
        /**
         * SPRD:fix bug640098 use video profile size for video snap size instead of preview size
         * Original
        Size optimalSize = CameraUtil.getOptimalVideoSnapshotPictureSize(
                supported, mDesiredPreviewWidth, mDesiredPreviewHeight);
         */
        Size optimalSize = CameraUtil.getOptimalVideoSnapshotPictureSize(
                supported, mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        if (optimalSize == null)
            optimalSize = new Size(0,0);
        Size original = new Size(mCameraSettings.getCurrentPhotoSize());
        if (!original.equals(optimalSize)) {
            mCameraSettings.setPhotoSize(optimalSize.toPortabilitySize());
        }
        /* SPRD:fix bug 618724/917328 need set thumbnail size for video snap shot @{ */
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(optimalSize,
                mAppController.getCameraProvider()).toPortabilitySize());
        /* @} */

        Log.d(TAG, "Video snapshot size is " + optimalSize);

        // Set JPEG quality.
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(
                mCameraId, CameraProfile.QUALITY_HIGH);
        mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality);

        updateTimeLapse();// SPRD: for bug 509708 add time lapse

        // SPRD : Fature : OIS && EIS
        updateParametersEOIS();

        // SPRD Bug:474696 Feature:Slow-Motion.
        setSlowmotionParameters();

        // SPRD： update makeup
        updateMakeLevel();
        updateMakeUpDisplay();

        updateParameters3DNR();
        updateParametersAppModeId();
        sendDeviceOrientation();

        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
            // Nexus 5 through KitKat 4.4.2 requires a second call to
            // .setParameters() for frame rate settings to take effect.
            // mCameraDevice.applySettings(mCameraSettings);
        }

        // SPRD Bug:474665 Feature Bug:Video Shutter Sound.
        updateCameraShutterSound();

        //nj dream camera test 70
        updateParametersGridLine();
        // Update UI based on the new parameters.
        updateParametersFlashLevel();

        mUI.updateOnScreenIndicators(mCameraSettings);

    }
    protected void updateLevel() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_ADD_LEVEL)){
            mUI.setLevelVisiable(false);
            return;
        }
        if(mDataModuleCurrent.getBoolean(Keys.KEY_ADD_LEVEL)){
            mUI.setLevelVisiable(true);
        }else{
            mUI.setLevelVisiable(false);
        }
    }
    public void updateLevelPresentation(){
        mUI.updateLevel();
    }
    /* SPRD: New Feature EIS&OIS @{ */
    private void updateParametersEOIS() {
        // SPRD: add for NullPointerException when call isCameraFrontFacing at activity destroyed
        if (isCameraOpening() || mActivity.isDestroyed()) {
            return;
        }
        Log.d(TAG, "updateParametersEOIS video eois = " + CameraUtil.isEOISDvFrontEnabled() + isCameraFrontFacing());
        if (isCameraFrontFacing() && CameraUtil.isEOISDvFrontEnabled()) {
            if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EOIS_DV_FRONT)) {
                return;
            }
            Log.d(TAG, "front video eois = " + mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_FRONT));
            mCameraSettings
                    .setEOISEnable(mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_FRONT));
            return;
        }

        if (!isCameraFrontFacing() && CameraUtil.isEOISDvBackEnabled()) {
            if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EOIS_DV_BACK)) {
                return;
            }
            Log.d(TAG, "back video eois = " + mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_BACK));
            mCameraSettings
                    .setEOISEnable(mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DV_BACK));
            return;
        }
    }

    //nj dream camera test 70, 75
    private void updateParametersGridLine() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_GRID_LINES)) {
            return;
        }
        mAppController.getCameraAppUI().initGridlineView();
        String grid = mDataModuleCurrent.getString(Keys.KEY_CAMERA_GRID_LINES);
        mAppController.getCameraAppUI().updateScreenGridLines(grid);
        Log.d(TAG, "updateParametersGridLine = " + grid);
    }

    //nj dream camera test 74
    private boolean updateParametersMircophone() {
        if (mDataModuleCurrent == null || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_MICROPHONE)) {
            return false;
        } else {
            return mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_MICROPHONE);
        }
    }

    protected void updateParameters3DNR() {
        if (!CameraUtil.is3DNREnable()) {
            return;
        }

        Log.i(TAG, "updateParameters3DNR set3DNREnable : 0");
        mCameraSettings.set3DNREnable(0);
    }

    private void setMeteringAreasIfSupported() {
        if (mMeteringAreaSupported) {
            mCameraSettings.setMeteringAreas(mFocusManager.getMeteringAreas());
        }
    }

    private void updateFocusParameters() {
        // Set continuous autofocus. During recording, we use "continuous-video"
        // auto focus mode to ensure smooth focusing. Whereas during preview
        // (i.e.
        // before recording starts) we use "continuous-picture" auto focus mode
        // for faster but slightly jittery focusing.
        Set<CameraCapabilities.FocusMode> supportedFocus = mCameraCapabilities
                .getSupportedFocusModes();
        if (mMediaRecorderRecording) {
            if(CameraUtil.isAFAEInRecordingEnable) {
                mFocusManager.overrideFocusMode(null);
                if (mCameraCapabilities
                        .supports(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO)) {
                    mCameraSettings.setFocusMode(mFocusManager
                            .getFocusMode(mCameraSettings.getCurrentFocusMode(), "continuous-video"));
                    if (mFocusAreaSupported) {
                        mCameraSettings
                                .setFocusAreas(mFocusManager.getFocusAreas());
                    }
                }
            } else {
                if (mCameraCapabilities
                        .supports(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO)) {
                    mCameraSettings
                            .setFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
                    mFocusManager
                            .overrideFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
                } else {
                    mFocusManager.overrideFocusMode(null);
                }
            }
        } else {
            // FIXME(b/16984793): This is broken. For some reasons,
            // CONTINUOUS_PICTURE is not on
            // when preview starts.
            mFocusManager.overrideFocusMode(null);
            if (mCameraCapabilities
                    .supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE)) {
                mCameraSettings.setFocusMode(mFocusManager
                        .getFocusMode(mCameraSettings.getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE)));
                if (mFocusAreaSupported) {
                    mCameraSettings
                            .setFocusAreas(mFocusManager.getFocusAreas());
                }
            }
        }
        updateAutoFocusMoveCallback();
    }

    public void updateParametersAutoChasingRegion(){
        mAutoChasingOn = mDataModuleCurrent
                .getBoolean(Keys.KEY_AUTO_TRACKING);
        mFocusManager.setAutoChasingEnable(mAutoChasingOn);
        mFocusManager.cancelAutoChasing();
    }
    @Override
    public void resume() {
        Log.i(TAG, "resume");
        if (isVideoCaptureIntent()) {
            mDontResetIntentUiOnResume = mPaused;
        }

        mPaused = false;
        mDataModuleCurrent.addListener(mActivity,this);
        installIntentFilter();
        mAppController.setShutterEnabled(false);
        mZoomValue = 1.0f;

        if (CameraUtil.isVideoAEAFLockEnable() || isSupportTouchEV()) {
            mAELockPanel = mActivity.getModuleLayoutRoot().findViewById(R.id.ae_lock_panel);
            if(mAELockPanel != null) {
                mAELockPanel.setActive(true);
            }
            mVerticalSeekBar = (AELockSeekBar) mActivity.getModuleLayoutRoot().findViewById(R.id.aeseekbar);
        }

        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        orientationManager.addOnOrientationChangeListener(this);
        mUI.onOrientationChanged(orientationManager,
                orientationManager.getDeviceOrientation());
        CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable(){
            @Override
            public void run() {
                if(CameraUtil.isLevelEnabled() && !mPaused) {
                    mOrientationEventListener.enable();
                    mSensorManager = (SensorManager)mActivity.getSystemService(Context.SENSOR_SERVICE);
                    // accelerometer sensor (for device orientation)
                    if( mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ) {
                        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    }
                    if( mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null ) {
                        mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    }

                    mSensorManager.registerListener(mAccelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    mSensorManager.registerListener(mMagneticListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
                    Log.i(TAG,"Level Listener has been registered");
                }
            }
        });
        //must new in main thread or pass Looper
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (mMediaRecorderRecording) {
                            stopVideoRecording();
                        }
                        break;
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        mPreciseCallStateListener = new PhoneStateListener() {
            @Override
            public void onPreciseCallStateChanged(PreciseCallState callState) {
                int preciseCallState = callState.getRingingCallState();
                switch (preciseCallState) {
                    case PreciseCallState.PRECISE_CALL_STATE_WAITING:
                        mIsTwoVT = true;
                        break;
                    default:
                        mIsTwoVT = false;
                        break;
                }
                super.onPreciseCallStateChanged(callState);
            }
        };
        TelephonyManager mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        if (mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        if (mPreciseCallStateListener != null) {
            mTelephonyManager.listen(mPreciseCallStateListener, PhoneStateListener.LISTEN_PRECISE_CALL_STATE);
        }

        showVideoSnapshotUI(false);
        mUI.onResume();
        if (!mPreviewing) {
            requestCamera(getCameraId());
        } else {
            // preview already started
            mAppController.setShutterEnabled(true);
        }

        if (mFocusManager != null) {
            // If camera is not open when resume is called, focus manager will
            // not
            // be initialized yet, in which case it will start listening to
            // preview area size change later in the initialization.
            mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
        }

        if (mPreviewing) {
            mOnResumeTime = SystemClock.uptimeMillis();
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_DISPLAY_ROTATION, 100);
        }
        getServices().getMemoryManager().addListener(this);
        /*
         * SPRD:Add for bug 496130 when recording video ,snap photo the
         * optionbar shows @{
         */
        getServices().getMediaSaver().setListener(
                new MediaSaverImpl.Listener() {
                    public void onHideBurstScreenHint() {
                        if (isRecording()) {
                            // SPRD: Fix bug 541750 The photo module can't change mode after take a picture.
                            showVideoSnapshotUI(false);
                        }
                    }

                    public int getContinuousCaptureCount() {
                        return 0;
                    }
                });
        /* @} */

        //add for hal return buffer fail
        mActivity.setSurfaceVisibility(false);

        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        if (isUseSurfaceView()) {
            mActivity.setSurfaceHolderListener(mUI);
            mActivity.setSurfaceVisibility(true);
        }
        /* @} */
        /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
        if(null != mActivity.getCameraAppUI()) {
            mActivity.getCameraAppUI().refreshTextureViewCover();
        }
        /* @} */
    }

    private int getCameraId() {
        // Bug #533869 new feature: check UI 27,28: dream camera of intent capture
        if (!mIsVideoCaptureIntent && mDataModule.getBoolean(Keys.KEY_INTENT_CAMERA_SWITCH) ||
                mIsVideoCaptureIntent && mDataModule.getBoolean(Keys.KEY_CAMERA_SWITCH)) {
            return (mDataModule.getInt(Keys.KEY_CAMERA_ID));
        } else {
            if (CameraUtil.isTcamAbility(mActivity,mActivity.getCurrentModuleIndex(),mCameraId)) {
                mCameraId = CameraUtil.BACK_TRI_CAMERA_ID;
            }
            return mCameraId;
        }
    }

    @Override
    public void pause() {
        Log.i(TAG, "pause");
        /* SPRD: fix bug Bug 577352 Slow recording process, repeatedly tap the screen,the
         * phone interface will now show time "slow record does not support the camera @{ */
        //ToastUtil.cancelToast();
        /* @} */
        isFirstShowToast = true;
        isBatteryAgainLowLevel = false;
        mPaused = true;
        procForAELockInPause();
        if (mActivity.getCameraAppUI() != null) {
            /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
            mActivity.getCameraAppUI().refreshTextureViewCover();
            /* @} */
            mActivity.getCameraAppUI().resumeTextureViewRendering();
        }
        restoreToDefaultSettings();
        mDataModuleCurrent.removeListener(mActivity,this);
        mDataModule.removeListener(mActivity,this);
        if(CameraUtil.isLevelEnabled()) {
            CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable(){
                @Override
                public void run() {
                    mSensorManager.unregisterListener(mAccelerometerListener);
                    mSensorManager.unregisterListener(mMagneticListener);
                    mOrientationEventListener.disable();
                    Log.i(TAG,"Level Listener has been unregistered");
                }
            });
        }
        mActivity.getCameraAppUI().dismissDialog(Keys.KEY_CAMERA_STORAGE_PATH);
        mUI.resetZoomSimple();//SPRD:fix bug626587

        mAppController.getOrientationManager().removeOnOrientationChangeListener(this);
        /* SPRD: Fix bug 580448 slow should be closed beyond video @{ */
        if (mCameraSettings != null && mCameraDevice != null) {
            mCameraSettings.setVideoSlowMotion(Keys.SLOWMOTION_DEFAULT_VALUE);
            mCameraDevice.applySettings(mCameraSettings);
        }
        /* @} */
        if (mFocusManager != null) {
            // If camera is not open when resume is called, focus manager will
            // not
            // be initialized yet, in which case it will start listening to
            // preview area size change later in the initialization.
            mAppController.removePreviewAreaSizeChangedListener(mFocusManager);
            mFocusManager.removeMessages();
        }

        removeAutoFocusMoveCallback();

        /*
         * SPRD: Fix bug 443439
         * SPRD: Fix bug 681215 optimization about that DV change to DC is slow
         */
        if (mCameraDevice != null) {
            mCameraDevice.cancelAutoFocus();
            if (useNewApi()) {
                mCameraDevice.setStartVideoCallback(null);
            }
        }
        /* @} */

        if (mMediaRecorderRecording) {
            // Camera will be released in onStopVideoRecording.
            onStopVideoRecording();
        } else {
            // It may has changed ui in main thread but not started recording when onStorageUpdateDone.
            if (needRefreshUI()) {//Sprd fix bug818967
                mUI.showRecordingUI(false, mActivity.getOrientationManager()
                        .getDisplayRotation().getDegrees());
            }
            mAppController.getCameraAppUI().setShouldSuppressCaptureIndicator(false);
            stopPreview();
            closeCamera();
            releaseMediaRecorder();
            if (mRecordingSurface != null) {
                mRecordingSurface.release();
                mRecordingSurface = null;
            }
        }

        if(!mIsVideoCaptureIntent){
            closeVideoFileDescriptor();
        }

        if (mReceiver != null) {
            // SPRD:fix bug599645 VideoModule recivedBroadcast later than CameraActivity, cause be killed
            // mActivity.unregisterReceiver(mReceiver);
            mActivity.unRegisterMediaBroadcastReceiver();
            mReceiver = null;
        }

        mHandler.removeCallbacksAndMessages(null);
        mPendingSwitchCameraId = -1;
        mSwitchingCamera = false;
        mPreferenceRead = false;
        mPendingUpdateStorageSpace = false;
        isShutterButtonClicked = false;
        mMediaRecorderPrepareing = false;
        getServices().getMemoryManager().removeListener(this);
        mUI.onPause();

        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        if (isUseSurfaceView()) {
            mActivity.setSurfaceHolderListener(null);
        }
        /* @} */

        // CameraAppUI.resumeTextureViewRendering triggers onSurfaceTextureUpdate(),
        // and onSurfaceTextureUpdate() calls hideImageCover, we don't want that happen
        // so do some preparatory work here
        mAppController.getCameraAppUI().prepareForNextRoundOfSurfaceUpdate();

        TelephonyManager mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        if (mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mPreciseCallStateListener != null) {
            mTelephonyManager.listen(mPreciseCallStateListener, PhoneStateListener.LISTEN_NONE);
        }
        Log.i(TAG, "pause end!");
    }

    private boolean needRefreshUI() {
        return (mUI != null && mActivity != null && mActivity.getOrientationManager() != null
                && mActivity.getOrientationManager().getDisplayRotation() != null
                && !isInReviewMode());
    }

    public void restoreToDefaultSettings(){
        // UI part reset
        updateParametersGridLineToDefault();

    }

    private void updateParametersGridLineToDefault() {
        if (mDataModuleCurrent == null
                || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_GRID_LINES)) {
            return;
        }
        String grid = mDataModuleCurrent.getStringDefault(Keys.KEY_CAMERA_GRID_LINES);
        mAppController.getCameraAppUI().updateScreenGridLines(grid);
        Log.d(TAG, "updateParametersGridLine = " + grid);
    }

    @Override
    public void destroy() {
        AsyncTask.execute(new Runnable() {// use SerialExecutor to sync
            @Override
            public void run() {
                Log.i(TAG, "Sound release start");
                if (mCameraSound != null) {
                    mCameraSound.release();
                    mCameraSound = null;
                }
            }
        });
        if (mUI != null) {
            mUI.onDestroy();
        }
        /*
         * SPRD: optimize bug 699216 that surface shows too slowly
         */
        if (mAsyncHandler != null) {
            mAsyncHandler.getLooper().quitSafely();
        }
        /* @} */
        if(mIsVideoCaptureIntent){//SPRD: Fix bug1189995
            closeVideoFileDescriptor();
        }
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    protected void switchCamera() {
        mActivity.switchFrontAndBackMode();
    }

    private void initializeVideoSnapshot() {
        if (mCameraSettings == null) {
            return;
        }
    }

    void showVideoSnapshotUI(boolean enabled) {
        if (mCameraSettings == null) {
            return;
        }
        if (mCameraCapabilities
                .supports(CameraCapabilities.Feature.VIDEO_SNAPSHOT)
                && !mIsVideoCaptureIntent) {
            if (enabled) {
                mAppController.startFlashAnimation(false);
            } else {
                mUI.showPreviewBorder(enabled);
            }
            mAppController.setShutterEnabled(!enabled);
        }
    }

    /**
     * Used to update the flash mode. Video mode can turn on the flash as torch
     * mode, which we would like to turn on and off when we switching in and out
     * to the preview.
     *
     * @param enable Whether torch mode can be enabled.
     */
    private void enableTorchMode(boolean enable) {
        enableTorchMode(enable, true);
    }

    private void enableTorchMode(boolean enable, boolean shouldApplySettings) {
        updateFlashLevelUI();
        if (isCameraOpening()) {
            return;
        }
        if (mCameraSettings.getCurrentFlashMode() == null) {
            return;
        }
        //SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEOCAMERA_FLASH_MODE)){
            return;
        }
        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();
        CameraCapabilities.FlashMode flashMode;
        if (enable && !mActivity.getIsButteryLow()) { // add buttery low status for Bug 911616
            flashMode = stringifier.flashModeFromString(mDataModuleCurrent
                    .getString(Keys.KEY_VIDEOCAMERA_FLASH_MODE));
        } else {
            flashMode = CameraCapabilities.FlashMode.OFF;
        }
        Log.d(TAG, "AA enableTorchMode " + flashMode);
        if (mCameraCapabilities.supports(flashMode)) {
            mCameraSettings.setFlashMode(flashMode);
            if (DreamUtil.getRightCamera(mCameraId) == DreamUtil.FRONT_CAMERA && CameraUtil.getFrontFlashMode() == CameraUtil.VALUE_FRONT_FLASH_MODE_LCD) {
                Log.e(TAG,"front camera's flash type is lcd");
                mCameraSettings.setFlashType(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD);
            } else {
                mCameraSettings.setFlashType(CameraUtil.VALUE_FRONT_FLASH_MODE_LED);
            }
        }

        if (shouldApplySettings) {
            if (mCameraDevice != null) {
                mCameraDevice.applySettings(mCameraSettings);
                mCameraSettings = mCameraDevice.getSettings();
            }
        }
        mUI.updateOnScreenIndicators(mCameraSettings);
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        if (mPreviewing) {
            /**
             * SPRD BUG 507795: won't open the flash when enlarge the picture in
             * picture preview UI @{ Original Code enableTorchMode(visibility ==
             * ModuleController.VISIBILITY_VISIBLE );
             */

            // SPRD:fix bug519856 return DV, torch will flash first,then turn on
            // enableTorchMode(visibility == ModuleController.VISIBILITY_VISIBLE
            // &&
            // mActivity.getCameraAppUI().getFilmstripVisibility() !=
            // View.VISIBLE);
            enableTorchMode(visibility != ModuleController.VISIBILITY_HIDDEN
                    && mActivity.getCameraAppUI().getFilmstripVisibility() != View.VISIBLE);
            /* @} */
        }
        if (visibility == ModuleController.VISIBILITY_VISIBLE) {
            mUI.resumeFaceDetection();
        } else {
            mUI.pauseFaceDetection();
            mUI.clearFaces();
        }
    }

    private void storeImage(final byte[] data, Location loc) {
        long dateTaken = System.currentTimeMillis();
        String title = CameraUtil.instance().createJpegName(dateTaken);
        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
        //PAY ATTENTION: NEED NEXT DEBUG
        /*Boolean gridLinesOn = Keys.areGridLinesOn(mDataModuleCurrent);*/

        // Make sure next single tap we get the right memory.
        getServices().getMediaSaver().addImage(data, title, dateTaken, loc,
                orientation, exif, mOnPhotoSavedListener);
        mActivity.updateStorageSpaceAndHint(null);
        Log.i(TAG, "storeImage end!");
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }

    private void closeVideoFileDescriptor() {
        if (mVideoFileDescriptor != null) {
            try {
                // SPRD: fix bug571335 camera anr
                long start = System.currentTimeMillis();
                mVideoFileDescriptor.close();
                /* SPRD: fix bug571335 camera anr @{ */
                long cost = System.currentTimeMillis() - start;
                if (cost > 100) {
                    Log.i(TAG, "closeVideoFileDescriptor cost: " + cost);
                }
                /* @} */
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            mVideoFileDescriptor = null;
        }
    }

    @Override
    public void onPreviewUIReady() {
        startPreview(true);
    }

    @Override
    public void onPreviewUIDestroyed() {
        stopPreview();
    }

    protected void requestCamera(int id) {
        doCameraOpen(id);
//        mActivity.getCameraProvider().requestCamera(id);
    }

    /* nj dream camera test 78, 128 @{ */
    protected void requestCameraOpen() {
        if (mCameraId != mDataModule.getInt(Keys.KEY_CAMERA_ID)) {
            mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
        }

        if (CameraUtil.isTcamAbility(mActivity,mActivity.getCurrentModuleIndex(),mCameraId)) {
            mCameraId = CameraUtil.BACK_TRI_CAMERA_ID;
        }
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
//        requestCamera(mCameraId);
        doCameraOpen(mCameraId);
    }

    @Override
    public boolean useNewApi() {
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(null);
    }

    protected void doCameraOpen(int cameraId){
        mActivity.getCameraProvider().requestCamera(cameraId, useNewApi());
    }

    /* @} */
    @Override
    public void onMemoryStateChanged(int state) {
        mAppController.setShutterEnabled(state == MemoryManager.STATE_OK);
    }

    @Override
    public void onLowMemory() {
        // Not much we can do in the video module.
    }

    /*********************** FocusOverlayManager Listener ****************************/
    @Override
    public void autoFocus() {
        if (mCameraDevice != null) {
            mCameraDevice.autoFocus(mHandler, mAutoFocusCallback);
        }
        setCameraState(FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        if (mCameraDevice != null) {
            mCameraDevice.cancelAutoFocus();
            setFocusParameters();
        }
        setCameraState(IDLE);
    }

    @Override
    public void setAutoChasingParameters(){
        if (mCameraDevice != null) {
            setCameraParameters();
        }
    }

    @Override
    public boolean capture() {
        return false;
    }

    @Override
    public boolean isManualMode() {
        return false;
    }

    @Override
    public boolean isTouchCapture() {
        return false;
    }

    @Override
    public void startFaceDetection() {
        if (mFaceDetectionStarted || mCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS) {
            return;
        }
        Log.d(TAG, "startFaceDetection ");
        /*
         * SPRD:Modify for ai detect @{
         * if (mCameraCapabilities.getMaxNumOfFacesSupported() > 0) {
         */
        mCameraDevice.setFaceDetectionCallback(mHandler, mUI);
        mCameraDevice.startFaceDetection();
        //mUI.resumeFaceDetection();
        mUI.onStartFaceDetection(mCameraDisplayOrientation, isCameraFrontFacing());
        mFaceDetectionStarted = true;
    }

    @Override
    public void stopFaceDetection() {
        Log.d(TAG, "stopFaceDetection mFaceDetectionStarted=" + mFaceDetectionStarted);
        mUI.clearFaces();
        if (mCameraDevice == null) {//SPRD:fix bug625571 for OOM
            return;
        }
        mCameraDevice.setFaceDetectionCallback(null, null);
        mCameraDevice.stopFaceDetection();
        mFaceDetectionStarted = false;
        mUI.pauseFaceDetection();
        /* }@ */
        mUI.clearFaces();
    }

    @Override
    public void setFocusParameters() {
        if (mCameraDevice != null) {
            updateFocusParameters();
            setMeteringAreasIfSupported();
            mCameraDevice.applySettings(mCameraSettings);
            mCameraSettings = mCameraDevice.getSettings();
        }
    }

    public void onPauseClicked() {
        Log.i(TAG, "onPauseClicked");
        long timePasedAfterRecording = SystemClock.uptimeMillis()
                - mRecordingStartTime;
        Log.d(TAG, "time passed after recording started: "
                + timePasedAfterRecording);
        if (mMediaRecorderRecording) {
            Log.d(TAG, String.format("mMediaRecorder execute %s",
                    (mPauseRecorderRecording ? "resume" : "pause")));
            // current is pause state
            if (mPauseRecorderRecording) {

                // Dependence:SPRD MediaRecorder.
                mMediaRecorder.resume();

                calculatePauseDuration(mPauseRecorderRecording);
                updateRecordingTime();
            }
            // current is recording state
            else {
                mHandler.removeMessages(MSG_UPDATE_RECORD_TIME);

                // Dependence:SPRD MediaRecorder.
                mMediaRecorder.pause();

                calculatePauseDuration(mPauseRecorderRecording);
            }
            // reverse pause state
            mPauseRecorderRecording = !mPauseRecorderRecording;
        }
        Log.d(TAG, "onPauseClicked mMediaRecorderRecording:"
                + mMediaRecorderRecording + ",mPauseRecorderRecording:"
                + mPauseRecorderRecording);
        mUI.onPauseClicked(mPauseRecorderRecording);
    }

    private void calculatePauseDuration(boolean isPause) {
        if (isPause) {
            mResumeTime = SystemClock.uptimeMillis();
            mResultTime += (mResumeTime - mPauseTime);
            mAllPauseTime += (mResumeTime - mPauseTime);
        } else {
            mPauseTime = SystemClock.uptimeMillis();
        }
    }

    /*
     * SPRD Bug:474701 Feature:Video Encoding Type. @{
     */
    public int getVideoEncodeType() {
        if(isRecordingIn4k() && CameraUtil.isSupportH265()){
            return CameraUtil.H265;
        }
        if(CameraUtil.isSupportH264()){
            return MediaRecorder.VideoEncoder.H264;
        } else if(CameraUtil.isSupportH265()){
            return CameraUtil.H265;
        } else if(CameraUtil.isSupportMPEG4()){
            return MediaRecorder.VideoEncoder.MPEG_4_SP;
        } else if (CameraUtil.isSupportH263()){
            return MediaRecorder.VideoEncoder.H263;
        } else {
            Log.e(TAG,"getVideoEncodeType don`t support h264/h265/mpeg4/h263 so retrun default");
            return MediaRecorder.VideoEncoder.DEFAULT;
        }
    }

    boolean mShutterSoundEnable = true;
    /* SPRD: fix bug 474665 add shutter sound switch @{ */
    private void updateCameraShutterSound() {
        if (mCameraDevice != null) {
            mCameraDevice.enableShutterSound(mShutterSoundEnable = mAppController.isPlaySoundEnable());
        }
    }

    // SPRD Bug:495676 update antibanding for DV
    protected void updateParametersAntibanding() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEO_ANTIBANDING)) {
            return;
        }
        if (isCameraOpening()) {
            return;
        }

        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();

        String mAntibanding = mDataModuleCurrent
                .getString(Keys.KEY_VIDEO_ANTIBANDING);
        if (mAntibanding == null) {
            return;
        }
        mCameraSettings.setAntibanding(stringifier
                .antibandingModeFromString(mAntibanding));

    }
    /* @} */

    private void setAutoTrackingIfSupported() {
        if (mAutoChasingSupported){
            mCameraSettings.setCurrentAutoChasingRegion(mFocusManager.getAutoChasingRegion());
        }
    }

    @Override
    public void onAutoChasingTraceRegion(int state, Rect rect){
        if (mAutoChasingSupported && mAutoChasingOn && !mPaused) {
            mFocusManager.onAutoChasingTraceRegion(state,rect);
        }
    }

    /* SPRD:fix bug492439 Ture on FM, Camera can not record @{ */
    private void abandonAudioPlayback() {
        mAudioManager.abandonAudioFocus(null);
    }

    /* @} */

    /*
     * SPRD Bug:509708 Feature:Time Lapse. @{
     */
    protected void updateTimeLapse() {

        if (isCameraOpening()) {
            return;
        }
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL)) {
            return;
        }
        String timeLapseMultipleStr = mDataModuleCurrent
                .getString(Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        Log.d(TAG, "updateTimeLapse timeLapseMultipleStr=" + timeLapseMultipleStr);
        if (timeLapseMultipleStr == null) {
            return;
        }
        mTimeLapseMultiple = Integer
                .parseInt(timeLapseMultipleStr);
        mCaptureTimeLapse = (mTimeLapseMultiple != 1);
    }

    /* @} */

    /*
     * SPRD Bug:474696 Feature:Slow-Motion. @{
     */
    protected void setSlowmotionParameters() {
        if (isCameraOpening()) {
            return;
        }

        String slow_motion = mDataModuleCurrent
                .getString(Keys.KEY_VIDEO_SLOW_MOTION);
        if (mIsVideoCaptureIntent) {
            mCameraSettings.setVideoSlowMotion(Keys.SLOWMOTION_DEFAULT_VALUE);
        } else {
            mCameraSettings.setVideoSlowMotion(slow_motion);
        }
    }

    public boolean isAudioSilence(boolean isVideoCaptureIntent){
        String sSlowMotion = Keys.SLOWMOTION_DEFAULT_VALUE;
        if (mDataModuleCurrent != null) {
            sSlowMotion = mDataModuleCurrent.getString(
                    Keys.KEY_VIDEO_SLOW_MOTION, Keys.SLOWMOTION_DEFAULT_VALUE);
        }

        if ((Integer.parseInt(sSlowMotion) > 1 && !isVideoCaptureIntent)
                || mCaptureTimeLapse || !updateParametersMircophone()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isAudioRecording() {
        boolean isRecodring = false;
        if (AudioSystem.isSourceActive(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                || AudioSystem.isSourceActive(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                || AudioSystem.isSourceActive(MediaRecorder.AudioSource.CAMCORDER)) {
            isRecodring = true;
        }
        return isRecodring;
    }


    /* @} */
    /* SPRD: Fix bug 535139, Add for video color effect @{ */
    public void updateParametersColorEffect() {
    }
    /* @} */

    /* SPRD: Fix bug 535139, Add for add whitebalance @{ */
    private void updateParametersWhiteBalance() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEO_WHITE_BALANCE)) {
            return;
        }
        if (isCameraOpening()) {
            return;
        }
        String wb = mDataModuleCurrent.getString(Keys.KEY_VIDEO_WHITE_BALANCE);
        if (wb == null) {
            return;
        }
        CameraCapabilities.WhiteBalance whiteBalance = mCameraCapabilities.getStringifier()
                .whiteBalanceFromString(wb);
        if (mCameraCapabilities.supports(whiteBalance)) {
            mCameraSettings.setWhiteBalance(whiteBalance);
        }
    }
    /* @} */

    /* SPRD: Add for bug 559531 @{ */
    private void enableShutterAndPauseButton(boolean enable) {
        if (mUI != null) {
            mUI.enablePauseButton(enable);
        }
        if (mAppController != null) {
            mAppController.setShutterEnabled(enable);
        }
    }

    /**
     * updateBatteryLevel
     * @param level the battery level when level changed
     */
    public boolean isFirstShowToast = true;
    public boolean isBatteryAgainLowLevel = false;

    @Override
    public void updateBatteryLevel(int level) {
        if (mDataModuleCurrent == null
                || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            return;
        }
        String BEFORE_LOW_BATTERY = "_before_low_battery";
        // the limen value which set up in system properties
        int batteryLevel = CameraUtil.getLowBatteryNoFlashLevel();
        String beforeMode = mDataModuleCurrent.getString(Keys.KEY_VIDEOCAMERA_FLASH_MODE + BEFORE_LOW_BATTERY);
        String currentMode = mDataModuleCurrent.getString(Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        if (level <= batteryLevel && !isRecording()) {
            // step 1. save current value: on, off, torch
            if (TextUtils.isEmpty(beforeMode)) {
                mDataModuleCurrent.set(
                        Keys.KEY_VIDEOCAMERA_FLASH_MODE + BEFORE_LOW_BATTERY, currentMode);
            }
            // step 2. set flash mode off and write into sp
            /*SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
            mDataModuleCurrent.set(Keys.KEY_VIDEOCAMERA_FLASH_MODE, "off");
            */
            // step 3. if flash is on, turn off the flash

            if (mCameraSettings != null && mCameraCapabilities != null) {
                enableTorchMode(false);
            }
            // step 4. set button disabled and show toast to users
            mAppController.getButtonManager().disableButton(ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM);
            //SPRD: add for bug 908294
            if(isFirstShowToast || (!isFirstShowToast && isBatteryAgainLowLevel)){
                if(isFirstShowToast == false){
                    isBatteryAgainLowLevel = false;
                }
                isFirstShowToast = false;
                CameraUtil.toastHint(mActivity, R.string.battery_level_low);
            }
        } else if (level > batteryLevel && !isRecording()) {
            isBatteryAgainLowLevel = true;
            // never lower than limen
            if (TextUtils.isEmpty(beforeMode)) {
                return;
            }
            // step 1.set before state value to current value
            /*SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
            mDataModuleCurrent.set(Keys.KEY_VIDEOCAMERA_FLASH_MODE, beforeMode);
            */
            // step 2.set before state value null
            mDataModuleCurrent.set(
                    Keys.KEY_VIDEOCAMERA_FLASH_MODE + BEFORE_LOW_BATTERY, null);
            // step 4.set button disabled or enabled
            mAppController.getButtonManager().enableButton(ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM);
            // step 3.according to before state value turn on flash
            //if (!"off".equals(beforeMode)) {
                // open video flash
                if (mCameraSettings != null && mCameraCapabilities != null) {
                    enableTorchMode(true);
                }
            //}
        }
    }
    public void onPreviewStartedAfter() {
    }
    public VideoUI createUI(CameraActivity activity) {
        return  new VideoUI(activity, this, activity.getModuleLayoutRoot());
    }

    protected int getDeviceCameraId() {
        return mCameraDevice.getCameraId();
    }

    //dream test 11
    @Override
    public void updateParameter(String key) {
        switch (key) {
            case Keys.KEY_CAMERA_SHUTTER_SOUND:
                updateCameraShutterSound();
                break;
            default:
                break;
        }
    }

    //Bug#533869 add the feature of volume
    protected int getVolumeControlStatus(CameraActivity mActivity) {
        return mActivity.getVolumeControlStatus();
    }

    protected boolean isCameraOpening() {
        return mCameraSettings == null || mCameraCapabilities == null;
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application.
     */
    private class MainHandler extends Handler {

        public MainHandler(Looper looper) {
               super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_ENABLE_SHUTTER_BUTTON:
                    if (mActivity.hasKeyEventEnter()) mActivity.resetHasKeyEventEnter();
                    mAppController.setShutterEnabled(true);
                    break;

                case MSG_UPDATE_RECORD_TIME: {
                    updateRecordingTime();
                    break;
                }

                case MSG_CHECK_DISPLAY_ROTATION: {
                    // Restart the preview if display rotation has changed.
                    // Sometimes this happens when the device is held upside
                    // down and camera app is opened. Rotation animation will
                    // take some time and the rotation value we have got may be
                    // wrong. Framework does not have a callback for this now.
                    if ((CameraUtil.getDisplayRotation() != mDisplayRotation)
                            && !mMediaRecorderRecording && !mSwitchingCamera) {
                        startPreview(true);
                    }
                    if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
                        mHandler.sendEmptyMessageDelayed(
                                MSG_CHECK_DISPLAY_ROTATION, 100);
                    }
                    break;
                }

                case MSG_SWITCH_CAMERA: {
                    switchCamera();
                    break;
                }

                case MSG_SWITCH_CAMERA_START_ANIMATION: {
                    // TODO:
                    // ((CameraScreenNail)
                    // mActivity.mCameraScreenNail).animateSwitchCamera();

                    // Enable all camera controls.
                    mSwitchingCamera = false;
                    break;
                }
                case MSG_ENABLE_SHUTTER_PAUSE_BUTTON: {
                    enableShutterAndPauseButton(true);
                    break;
                }
                case MSG_CAMERA_VIDEO_START: {
                    mCameraDevice.startVideoRecording(mRecordingSurface, VideoModule.this, isUseSurfaceView() ? true : false);
                    break;
                }

                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "onReceive action = " + action);

            Uri uri = intent.getData();
            String path = uri.getPath();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                mActivity.getCameraAppUI().dismissDialog(Keys.KEY_CAMERA_STORAGE_PATH);

                String preStorage = intent.getStringExtra("pre_storage");
                String storagePath =  preStorage!= null?preStorage:DataModuleManager.getInstance(mActivity)
                        .getDataModuleCamera().getString(Keys.KEY_CAMERA_STORAGE_PATH);

                Log.d(TAG, "onReceive: storagePath = " + storagePath);
                String currentPath = null;
                if (MultiStorage.KEY_DEFAULT_INTERNAL.equals(storagePath)) {
                    currentPath = StorageUtilProxy.getInternalStoragePath().toString();
                } else if (MultiStorage.KEY_DEFAULT_EXTERNAL.equals(storagePath)) {
                    currentPath = StorageUtilProxy.getExternalStoragePath()
                            .toString();
                } else {
                    // SPRD: Fix bug 572473 add for usb storage support
                    currentPath = MultiStorage.getUsbStoragePath(storagePath);
                }
                Log.d(TAG, "onReceive: path = " + path + " currentPath = " + currentPath);

                if (path.equals(currentPath)) {
                    /* SPRD: fix bug548010 Camera occurs error when connect USB during video recording @{
                     * original code
                    stopVideoRecording();
                     */
                    if (mMediaRecorderRecording) {
                        stopVideoRecording(false,false);
                        /* @} */
                        //SPRD:fix bug538109 pull sdcard, recording stop in 4~5s
                        if(!"External".equals(storagePath)){//SPRD:fix bug671918 Modify the prompt for videoModule
                            CameraUtil.toastHint(mActivity,R.string.usb_remove);
                        }else{
                            CameraUtil.toastHint(mActivity,R.string.sdcard_remove);
                        }
                    } else {
                        if(path.equals(StorageUtilProxy.getExternalStoragePath().toString())){
                            CameraUtil.toastHint(mActivity,R.string.sdcard_changeto_phone);
                        } else {
                            CameraUtil.toastHint(mActivity,R.string.usb_remove_photo_or_video);
                        }
                    }
                } else {
                    if(path.equals(StorageUtilProxy.getExternalStoragePath().toString())){//SPRD:fix bug678784
                        CameraUtil.toastHint(mActivity,R.string.sdcard_changeto_phone);
                    } else {
                        CameraUtil.toastHint(mActivity,R.string.usb_remove_photo_or_video);
                    }
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                CameraUtil.toastHint(mActivity,R.string.wait);
            }
            Log.d(TAG, "MyBroadcastReceiver onReceive: end");
        }
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        @Override
        public void onPictureTaken(byte[] jpegData, CameraProxy camera) {
            Log.i(TAG, "Video snapshot taken.");
            mCounterWaitSnapshot.count();
            mSnapshotInProgress = false;
            /*SPRD: Fix bug 541750 The photo module can't change mode after take a picture. @{
            showVideoSnapshotUI(false);
            @}*/
            storeImage(jpegData, mLocation);

            // SPRD: Fix bug 650297 that video thumbnail is showed slowly
            mSnapshotCount++;
            mUI.enablePreviewOverlayHint(true);
        }
    }
    @Override
    public boolean isShutterClicked(){
        //SPRD:Fix Bug671480
        //return mMediaRecorderRecording;
        return isShutterButtonClicked;
    }

    /* SPRD: fix bug 474672 add for ucam beauty 
     * @{ */

    public MakeupController mMakeUpController;

    @Override
    public void setMakeUpController(MakeupController makeUpController) {
        mMakeUpController = makeUpController;
    }

    @Override
    public void onBeautyValueChanged(int[] value) {
        Log.i(TAG, "onBeautyValueChanged setParameters Value = " + Arrays.toString(value));
        if (mCameraSettings != null) {
            mCameraSettings.setSkinWhitenLevel(value);
            if (mCameraDevice != null) {
                mCameraDevice.applySettings(mCameraSettings);
            }
        }
    }

    @Override
    public void onFeatureOperationEnableChange(boolean enable){
        DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule().changeSettings(Keys.KEY_VIDEO_BEAUTY_ENTERED,enable);
    }

    @Override
    public void updateMakeLevel() {
        Log.d(TAG,
                "initializeMakeupControllerView "
                        + DataModuleManager.getInstance(mActivity)
                                .getCurrentDataModule()
                                .getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED));

        if (isMakeUpEnable() && mMakeUpController != null) {
            onBeautyValueChanged(mMakeUpController.getValue(Keys.KEY_VIDEO_BEAUTY_ENTERED));
        }
    }

    @Override
    public boolean isMakeUpEnable(){
        return CameraUtil.isMakeupVideoEnable() && DataModuleManager.getInstance(mActivity).getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_VIDEO_BEAUTY_ENTERED);
    }
    /* @{ */

    /* Fix bug 585183 Adds new features 3D recording @{ */
    protected String getVideoQualityKey() {
        return isCameraFrontFacing() ? Keys.KEY_VIDEO_QUALITY_FRONT : Keys.KEY_VIDEO_QUALITY_BACK;
    }
    /* @} */

    protected int getRecorderOrientationHint(int rotation) {
        return rotation;
    }
    /* SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{ */
    public String getFileDir() {
        return StorageUtil.getInstance().getFileDir();
    }
    public void switchPreview(){
    }

    protected boolean mCameraAvailable = false;
    @Override
    public boolean isCameraAvailable(){
        return mCameraAvailable;
    }

    private PhoneStateListener mPhoneStateListener;
    private PhoneStateListener mPreciseCallStateListener;
    private boolean mIsTwoVT = false;

    @Override
    public boolean isVideoRecording() {
        return mMediaRecorderRecording;
    }

    @Override
    public boolean getCameraPreviewState() {
        return mPreviewing;
    }

    public boolean isSupportVideoCapture() {
        return mCameraCapabilities.supports(CameraCapabilities.Feature.VIDEO_SNAPSHOT);
    }

    public void updateFlashLevelUI() {
        if (mDataModuleCurrent == null){
            return;
        }
        if (!mActivity.getIsButteryLow()
                && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEOCAMERA_FLASH_MODE)
                && "torch".equals(mDataModuleCurrent.getString(Keys.KEY_VIDEOCAMERA_FLASH_MODE))
                && CameraUtil.isFlashLevelSupported(mCameraCapabilities)) {
            mUI.showAdjustFlashPanel();
        } else {
            mUI.hideAdjustFlashPanel();
        }
    }

    private void updateParametersFlashLevel() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            return;
        }
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_ADJUST_FLASH_LEVEL)) {
            return;
        }
        mCameraSettings.setFlashLevel(mDataModuleCurrent.getInt(Keys.KEY_ADJUST_FLASH_LEVEL));
    }
    public boolean isFlashSupported(){
        if (mCameraCapabilities != null){
            return CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing());
        } else
            return false;
        }

    private boolean mSoundInitialized = false;
    private MediaActionSound mCameraSound;

    public void onSurfaceTextureUpdated() {
        if (!mSoundInitialized && useNewApi()) {
            mSoundInitialized = true;
            AsyncTask.execute(new Runnable() {// use SerialExecutor to sync
                @Override
                public void run() {
                    Log.i(TAG, "SoundInitialized start");

                    if (mCameraSound == null) {
                        mCameraSound = new MediaActionSound();
                        // Not required, but reduces latency when playback is requested later
                        mCameraSound.load(MediaActionSound.START_VIDEO_RECORDING);
                        mCameraSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
                    }
                }
            });
        }
    }

    protected void updateParametersAppModeId() {
        Log.i(TAG, "updateParametersAppModeId");
        mCameraSettings.setAppModeId(mActivity.getCurrentModuleIndex());
    }
    @Override
    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }

    @Override
    public int getMode() {
        return DreamUtil.VIDEO_MODE;
    }
    @Override
    public void updatePortraitBackgroundReplacementType(){}
}
