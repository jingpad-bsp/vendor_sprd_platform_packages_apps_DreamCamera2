/*
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.CameraProfile;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.android.camera.PhotoModule.NamedImages.NamedEntity;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.MemoryManager;
import com.android.camera.app.MemoryManager.MemoryListener;
import com.android.camera.app.MotionManager;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.exif.Rational;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.hardware.HeadingSensor;
import com.android.camera.module.ModuleController;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.settings.Keys;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.AndroidServices;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera.util.XmpBuilder;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamModule;
import com.dream.camera.LightPortraitControllerDream;
import com.dream.camera.MakeupController;
import com.dream.camera.modules.continuephoto.ContinuePhotoModule;
import com.dream.camera.modules.manualphoto.ManualPhotoUI;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataStructSetting;
import com.dream.camera.settings.DreamSettingUtil;
import com.dream.camera.ui.AELockPanel;
import com.dream.camera.ui.AELockSeekBar;
import com.dream.camera.ui.TopPanelListener;
import com.dream.camera.util.Counter;
import com.dream.camera.util.DreamUtil;
import com.sprd.camera.storagepath.StorageUtilProxy;
import com.sprd.camera.voice.PhotoVoiceMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import android.view.SurfaceHolder;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.graphics.Rect;

import com.android.camera.data.FilmstripItem;
import com.android.camera.data.FilmstripItemData;
import static com.android.camera.MediaSaverImpl.PHOTO_ERROR_URI;
import static com.android.camera.MediaSaverImpl.UPDATE_URI_TITLE;
import static com.android.camera.settings.SettingsScopeNamespaces.AUTO_PHOTO;
import static com.android.camera.settings.SettingsScopeNamespaces.FILTER;

public class PhotoModule extends CameraModule implements PhotoController,
        ModuleController, MemoryListener, FocusOverlayManager.Listener,
        OnSeekBarChangeListener, CameraAgent.CameraHdrDetectionCallback,
        CameraAgent.CameraAiSceneCallback,
        CameraAgent.CameraBlurCaptureCallback,
        CameraAgent.CameraAutoChasingTraceRegionCallback,
        CameraAgent.CameraIsAuto3DnrSceneDetectionCallback,
        /**
         * SPRD: fix bug 388273 CountDownView.OnCountDownStatusListener {
         */
        CountDownView.OnCountDownStatusListener,
        MediaSaverImpl.Listener,
        DreamSettingChangeListener,
        CameraAgent.CancelBurstCaptureCallback,
        OrientationManager.OnOrientationChangeListener,
        FocusOverlayManager.TouchListener, CameraAgent.CameraSensorSelfShotCallback,
        TopPanelListener, LightPortraitControllerDream.LightPortraitListener {

    private static final Log.Tag TAG = new Log.Tag("PhotoModule");

    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_CROP = 1000;

    // Messages defined for the UI thread handler.
    private static final int MSG_FIRST_TIME_INIT = 1;
    private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
    private static final int MSG_RESET_KEY_ENTER = 7;
    private static final int RESET_KEY_ENTER_DELAY = 150;

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    protected static final int UPDATE_PARAM_ALL = -1;

    private static final String DEBUG_IMAGE_PREFIX = "DEBUG_";

    protected CameraActivity mActivity;
    protected CameraProxy mCameraDevice;
    protected int mCameraId;
    protected CameraCapabilities mCameraCapabilities;
    protected CameraSettings mCameraSettings;
    private HardwareSpec mHardwareSpec;
    protected boolean mPaused;
    private boolean mCancelBurst = false;
    private boolean mThumbnailHasInvalid = false;
    protected PhotoUI mUI;
    public boolean isAlgorithmProcessOnApp = false;
    // The activity is going to switch to the specified camera id. This is
    // needed because texture copy is done in GL thread. -1 means camera is not
    // switching.
    protected int mPendingSwitchCameraId = -1;

    // When setCameraParametersWhenIdle() is called, we accumulate the subsets
    // needed to be updated in mUpdateSet.
    private int mUpdateSet;

    private float mZoomValue; // The current zoom ratio.
    private int mTimerDuration;

    private boolean mCameraButtonClickedFlag = false;// SPRD: fix bug 473602 add
                                                     // for half-press

    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mAeLockSupported;
    private boolean mAwbLockSupported;
    private boolean mContinuousFocusSupported;
    private boolean mAutoHdrSupported;
    private boolean mAiSceneSupported;
    private boolean mAutoChasingSupported;
    private boolean mAuto3DnrSupported;
    private int mAiSceneType = -1;
    private boolean mFaceAttributeSupported;
    private boolean mSmileSupported;

    private static final String sTempCropFilename = "crop-temp";

    public boolean mFaceDetectionStarted = false;

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    private Uri mDebugUri;

    // Add for dream settings.
    protected DataModuleBasic mDataModule;
    protected DataModuleBasic mDataModuleCurrent;

    // We use a queue to generated names of the images to be used later
    // when the image is ready to be saved.
    private NamedImages mNamedImages;
    private boolean isHdrOn = true;

    /*SPRD: fix bug 599542 add for touch capture and count down @{*/
    private boolean isOnSingleTapUp = false;
    private int mTapUpX= 0;
    private int mTapUpY= 0;
    private boolean isTouchCapturing = false;

    private boolean mCanSensorSelfShot = true;
    protected AELockPanel mAELockPanel;
    protected AELockSeekBar mVerticalSeekBar;
    private int AECompensation = 0;
    protected boolean isAELock = false;
    private boolean isAELockPanding = false;
    private int x = 0;
    private int y = 0;
    private boolean isHdrScene = false;
    private boolean isHdrPicture = false;
    private boolean isAuto3DnrScene = false;

    protected final Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            onShutterButtonClick();
        }
    };

    /**
     * An unpublished intent flag requesting to return as soon as capturing is
     * completed. TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";

    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for UI components like indicators.
    protected int mDisplayOrientation;
    // The value for cameradevice.CameraSettings.setPhotoRotationDegrees.
    protected int mJpegRotation;
    // Indicates whether we are using front camera
    private boolean mMirror;
    private boolean mFirstTimeInitialized;
    public boolean mIsImageCaptureIntent;

    protected int mCameraState = PREVIEW_STOPPED;
    public boolean mSnapshotOnIdle = false;
    protected boolean mNeedCancelAutoFocus = true;//SPRD:fix bug1116153

    private ContentResolver mContentResolver;

    protected AppController mAppController;
    private boolean mPreviewing = false; // True if preview is started.
    protected OneCameraManager mOneCameraManager;

    private final PostViewPictureCallback mPostViewPictureCallback = new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback = new RawPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final Object mAutoFocusMoveCallback = ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK ? new AutoFocusMoveCallback()
            : null;

    private long mFocusStartTime;
    private long mShutterCallbackTime;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private long mOnResumeTime;
    private byte[] mJpegImageData;
    /** Touch coordinate for shutter button press. */
    private TouchCoordinate mShutterTouchCoordinate;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;
    public long mCaptureStartTime;

    // This handles everything about focus.
    public FocusOverlayManager mFocusManager;

    private final int mGcamModeIndex;
    private SoundPlayer mCountdownSoundPlayer;

    private CameraCapabilities.SceneMode mSceneMode;

    /* SPRD: fix bug 473462 add burst capture @{ */
    public int mContinuousCaptureCount;
    public int mHasCaptureCount = 0;
    protected int mCaptureCount = 0;
    protected boolean mSmileCapture = false;

    private boolean mShutterSoundEnabled = false;// SPRD:fix bug 474665
    public boolean mBurstWorking = false;
    public boolean mIsContinousCaptureFinish = false;
    private static final int PICTURE_SIZE_13M = 4160 * 3120;
    /* @} */

    protected final Handler mHandler = new MainHandler(this);
    private boolean mIsFirstCallback = false;
    private boolean mIsNormalHdrDone = false;
    private boolean mIsHdrPicture = false;
    private boolean mIsNeedUpdatThumb = !CameraUtil.isUseGooglePhotos();//SPRD: bug978545 Add for google photos when fast capture,it can be ture when use native gallery

    protected boolean mFirstHasStartCapture = false;//add for thumb callback:framework has post jpg.onPictureTaken,then module enter pause and remove all message and runnable
    private boolean mNeedRemoveMessgeDelay = false;

    private boolean mQuickCapture;

    /** Used to detect motion. We use this to release focus lock early. */
    private MotionManager mMotionManager;
    private HeadingSensor mHeadingSensor;

    /** True if all the parameters needed to start preview is ready. */
    /**
     * SPRD: Fix bug 572631, optimize camera launch time
     * Original Code
     *
    private boolean mCameraPreviewParamsReady = false;
     */

    private CameraCapabilities.Antibanding mAntibanding;// SPRD:Add for
                                                        // antibanding
    protected Boolean mFace;// SPRD:Add for ai detect
    private String mDetectValueBeforeSceneMode;// SPRD:Add for ai detect
    protected CameraCapabilities.ColorEffect mColorEffect;// SPRD:Add for color
                                                        // effect Bug 474727
    protected boolean mBurstNotShowShutterButton = false;

    protected boolean mBurstAllDone;
    private String mUUID;

    private int countHdr = 0;
    private int countUri = 0;
    Uri[] mUri = new Uri[2];
    FilmstripItem normalHdrData = null;

    protected boolean mIsBlurRefocusPhoto = true;
    public boolean mBackPressed = false; // SPRD: fix Bug 707864
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
    private final MediaSaver.OnMediaSavedListener mOnMediaSavedListener = new MediaSaver.OnMediaSavedListener() {

        @Override
        public void onMediaSaved(Uri uri, String title) {
            Log.i(TAG, "onMediaSaved uri = " + uri + " , title = " + title);
            if (isNeedThumbCallBack() && mActivity != null && uri != null) {
                if (UPDATE_URI_TITLE.equals(title)) {
                    return;
                }
                UpdateRunnbale updateRunnable = mActivity.getUpdateRun(title);
                if (updateRunnable == null) {
                    mActivity.putThumbUri(title, uri);
                } else {
                    updateRunnable.setUri(uri);
                    if (mActivity.getCreatAsyncHandler() != null) {
                        mActivity.getCreatAsyncHandler().postAtFrontOfQueue(updateRunnable);
                    }
                }
            }

            if (uri != null) {
                /*
                 * Add For Dream Camera in IntervalPhotoModule
                 * 
                 * @{
                 */
                mediaSaved(uri);
                /* @} */

                mActivity.notifyNewMedia(uri);
            } else {
                /* SPRD: Fix bug 547952, wont show error dialog, not serious problem @{
                 * orginal code
                onError();
                */
                Log.w(TAG, "null uri got");
               /* @} */
            }
            mAppController.getCameraAppUI().decTakePictureCount();
        }
    };

    /**
     * Displays error dialog and allows use to enter feedback. Does not shut
     * down the app.
     */
    private void onError() {
        mAppController.getFatalErrorHandler().onMediaStorageFailure();
    }

    private boolean mShouldResizeTo16x9 = false;

    /**
     * We keep the flash setting before entering scene modes (HDR) and restore
     * it after HDR is off. SPRD:MUTEX
     */
    private String PREF_BEFORE = "PREF_BEFORE";
    private String PREF_BEFORE_SWITCH = "PREF_BEFORE_SWITCH";
    private String mFlashModeBeforeSceneMode;
    private String mExposureCompensationBefore;
    private String mSceneModeBefore;
    private String mCountineCaptureBefore;
    private String mWhiteBalanceBefore;
    private String mColorEffectBefore;
    private String mISOBefore;
    private String mContrastBefore;
    private String mSaturationBefore;
    private boolean mAutoExposureLockBefore;
    private boolean isToastExit = false;

    private void checkDisplayRotation() {
        // Need to just be a no-op for the quick resume-pause scenario.
        if (mPaused) {
            return;
        }
        // Set the display orientation if display rotation has changed.
        // Sometimes this happens when the device is held upside
        // down and camera app is opened. Rotation animation will
        // take some time and the rotation value we have got may be
        // wrong. Framework does not have a callback for this now.
        if (CameraUtil.getDisplayRotation() != mDisplayRotation) {
            setDisplayOrientation();
        }
        if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkDisplayRotation();
                }
            }, 100);
        }
    }

    private boolean isSupportSensoSelfShotModule() {
        if (!CameraUtil.isSensorSelfShotEnable()){
            return false;
        }
        int autoFrontIndex = AUTO_PHOTO;
        int intervalIndex = SettingsScopeNamespaces.INTERVAL;
        int audioPictureIndex = SettingsScopeNamespaces.AUDIO_PICTURE;

        int currentIndex = mActivity.getCurrentModuleIndex();
        if (currentIndex == autoFrontIndex || currentIndex == intervalIndex
                || currentIndex == audioPictureIndex) {
            return true;
        }
        return false;
    }

    @Override
    public void onIsAuto3DnrSceneDetection(boolean isAuto3DnrScene) {
        this.isAuto3DnrScene = isAuto3DnrScene;
    }

    @Override
    public void onHdrDetection(boolean isHdrScene) {
        Log.d(TAG,"onHdrDeteCtion: " + isHdrScene);
        //Sprd:Fix bug959314/bug1011831
        if (isShutterClicked() || mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 0) {
            return;
        }
        mUI.showHdrTips(isHdrScene);
        if (this.isHdrScene == isHdrScene) {
            return;
        }
        this.isHdrScene = isHdrScene;
        updateParametersNormalHDR();
    }

    private int mCountNight;
    private int mCountPortrait;
    public void onAiScene(int aiSceneType) {
        Log.d(TAG,"aiSceneType = " + aiSceneType );
        if(mAiSceneSupported == false || mPaused){
            return;
        }

        mAiSceneType = aiSceneType;
        // ui
        mActivity.getCameraAppUI().updateAiSceneView(View.VISIBLE , aiSceneType);

        if(mCameraState != SNAPSHOT_IN_PROGRESS){
            mCountNightOrPortraitNumber();
        }
    }

    public static boolean mNotModeListViewClick;
    private int mAutoToPortraitNumber = CameraUtil.getAutoToPortraitDefaultNumber();
    private int mAutoTo3DNRNumber = CameraUtil.getAutoTo3DNRDefaultNumber();

    public void mCountNightOrPortraitNumber(){
        if(mActivity.isSecureCamera() || mUI.isCountingDown() || mDataModuleCurrent == null
                || mDataModuleCurrent.getDataSetting() == null || !CameraUtil.isAISceneProEnable()){
            return;
        }

        if((CameraUtil.isBackPortraitPhotoEnable() && !mDataModuleCurrent.getDataSetting().mIsFront)
                || (CameraUtil.isFrontPortraitPhotoEnable() && mDataModuleCurrent.getDataSetting().mIsFront)){
            if(mAiSceneType == 2){
                ++mCountPortrait;
            } else {
                if(mCountPortrait > 0){
                    --mCountPortrait;
                }
            }
        }

        if(CameraUtil.is3DNREnable() && !mDataModuleCurrent.getDataSetting().mIsFront){
            if(mAiSceneType == 5){
                ++mCountNight;
            } else {
                if(mCountNight > 0){
                    --mCountNight;
                }
            }
        }

        if(mCountPortrait == mAutoToPortraitNumber && mActivity != null){
            switchTo3DNROrPortrait(SettingsScopeNamespaces.PORTRAIT_PHOTO);
        }
        if(mCountNight == mAutoTo3DNRNumber && mActivity != null){
            switchTo3DNROrPortrait(SettingsScopeNamespaces.TDNR_PHOTO);
        }
    }

    public void switchTo3DNROrPortrait(int settingsScopeNamespaces){
        mNotModeListViewClick = true;
        if(mActivity != null && mActivity.getCameraAppUI() != null && mActivity.getCameraAppUI().isSettingLayoutOpen()){
            mActivity.getCameraAppUI().hideSettingLayout();
        }
        mActivity.switchTo3DNROrPortrait(settingsScopeNamespaces);

    }

    @Override
    public void onAutoChasingTraceRegion(int state, Rect rect){
        boolean autoChasingOpen = mDataModuleCurrent
                .getBoolean(Keys.KEY_AUTO_TRACKING);
        if (mAutoChasingSupported && autoChasingOpen && !mPaused) {
            mFocusManager.onAutoChasingTraceRegion(state,rect);
        }
    }

    @Override
    public Boolean mIsFirstHasStartCapture(){return mFirstHasStartCapture;}

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    /**
     * SPRD: fix bug 473462 private static class MainHandler extends Handler {
     */
    private class MainHandler extends Handler {
        private final WeakReference<PhotoModule> mModule;

        public MainHandler(PhotoModule module) {
            super(Looper.getMainLooper());
            mModule = new WeakReference<PhotoModule>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            PhotoModule module = mModule.get();
            if (module == null) {
                return;
            }
            Log.d(TAG, "handleMsg " + msg.what);
            switch (msg.what) {
                case MSG_FIRST_TIME_INIT: {
                    module.initializeFirstTime();
                    break;
                }

                case MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    module.setCameraParametersWhenIdle(0);
                    break;
                }
            /* SPRD: Fix bug 535110, Photo voice record. @{ */
                case PhotoVoiceMessage.MSG_RECORD_AUDIO: {
                    module.startAudioRecord();
                    break;
                }

                case PhotoVoiceMessage.MSG_RECORD_STOPPED: {
                    module.onAudioRecordStopped();
                    break;
                }

                case MSG_RESET_KEY_ENTER: {
                    module.resetHasKeyEventEnter();
                    break;
                }
            /* @} */
            }
        }
    }

    private void resetHasKeyEventEnter() {
        if (mActivity != null) {
            mActivity.resetHasKeyEventEnter();
        }
    }

    /*SPRD:Fix bug 543925 @{*/
    private BroadcastReceiver mReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,"onReceive action="+action);
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                Uri uri = intent.getData();
                String path = uri.getPath();
                if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    mActivity.getCameraAppUI().dismissDialog(Keys.KEY_CAMERA_STORAGE_PATH);
                    //SPRD:fix bug678784
                    if(path.equals(StorageUtilProxy.getExternalStoragePath().toString())){
                        CameraUtil.toastHint(mActivity,R.string.sdcard_changeto_phone);
                    } else {
                        CameraUtil.toastHint(mActivity,R.string.usb_remove_photo_or_video);
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                    CameraUtil.toastHint(mActivity,R.string.wait);
                }
                /**SPRD:Bug742659 file is not exit when pull out the SD card @{*/
                if(mSaveUri !=null
                        && mSaveUri.toString().contains("com.android.email.fileprovider")){
                    mActivity.setResultEx(Activity.RESULT_CANCELED, new Intent()
                                                  .putExtra("extra_photo_save_info", "file not exit"));
                    mActivity.finish();
                    return;
                }
                /** @}*/
            }
        }
    }
    private void installIntentFilter() {
        // install an intent filter to receive SD card related events.
        //IntentFilter intentFilter =
        //        new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        //intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        //intentFilter.addDataScheme("file");
        mReceiver = new MyBroadcastReceiver();
        //mActivity.registerReceiver(mReceiver, intentFilter);
        mActivity.registerMediaBroadcastReceiver(mReceiver);
    }
    /* @} */

    public boolean inBurstMode() {
        return false;
    }

    private void switchToGcamCapture() {
        if (mActivity != null && mGcamModeIndex != 0) {
            mDataModule.set(Keys.KEY_CAMERA_HDR_PLUS, true);

            // Disable the HDR+ button to prevent callbacks from being
            // queued before the correct callback is attached to the button
            // in the new module. The new module will set the enabled/disabled
            // of this button when the module's preferred camera becomes
            // available.
            ButtonManager buttonManager = mActivity.getButtonManager();

            buttonManager.disableButtonClick(ButtonManager.BUTTON_HDR_PLUS);

            mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();

            // Do not post this to avoid this module switch getting interleaved
            // with
            // other button callbacks.
            mActivity.onModeSelected(mGcamModeIndex);

            buttonManager.enableButtonClick(ButtonManager.BUTTON_HDR_PLUS);
        }
    }

    /**
     * Constructs a new photo module.
     */
    public PhotoModule(AppController app) {
        super(app);

        mGcamModeIndex = SettingsScopeNamespaces.GCAM;
    }

    @Override
    public String getPeekAccessibilityString() {
        return mAppController.getAndroidContext().getResources()
                .getString(R.string.photo_accessibility_peek);
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera,
            boolean isCaptureIntent) {
        Log.i(TAG, "init Camera");

        mActivity = activity;
        // TODO: Need to look at the controller interface to see if we can get
        // rid of passing in the activity directly.
        mAppController = mActivity;

        /*
         * sync settings
         * 
         * @{
         */
        mDataModule = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getDataModuleCamera();
        mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
        mOneCameraManager = mActivity.getOneCameraManger();

        /* SPRD: Fix bug 681215 optimization about that DV change to DC is slow @{ */
        if (mActivity.isActivityResumed()) {
            long start = System.currentTimeMillis();
            requestCameraOpen();
            Log.i(TAG, "init: requestCameraOpen cost: " + (System.currentTimeMillis() - start));
        }
        /* @} */

        // SPRD initialize mJpegQualityController.
        mJpegQualityController = new JpegQualityController();
        mUI = createUI(mActivity);
        CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable(){
            @Override
            public void run() {
                performInitAsync();
            }
        });
        mActivity.setPreviewStatusListener(mUI);
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientation = (orientation + 45) / 90 * 90;
                mCurrentOrientation = orientation % 360;
            }
        };
        mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);

        Log.i(TAG, " init " + mAppController.getModuleScope() + ","
                + mCameraId);
        mContentResolver = mActivity.getContentResolver();

        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        mIsImageCaptureIntent = isImageCaptureIntent();
        mQuickCapture = mActivity.getIntent().getBooleanExtra(
                EXTRA_QUICK_CAPTURE, false);
        mHeadingSensor = new HeadingSensor(AndroidServices.instance()
                .provideSensorManager());
    }
    private final SensorEventListener mAccelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            onAccelerometerSensorChanged(event);
            updateLevelPresentation();
            Log.i(TAG,"updateLevel");

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
    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keyList) {
        if (mCameraDevice == null) {
            return;
        }
        HashMap<String, String> keys = new HashMap<String, String>();
        keys.putAll(keyList);

        for (String key : keys.keySet()) {
            Log.d(TAG, "onSettingChanged key = " + key + " value = " + keys.get(key));
            switch (key) {
            case Keys.KEY_PICTURE_SIZE_BACK:
                /* SPRD: w+t module just for back camrea and auto photo module@{ */
                if (CameraUtil.isWPlusTEnable()){
                    int suggestCameraId = mCameraId;
                    if (SettingsScopeNamespaces.AUTO_PHOTO == mActivity.getCurrentModuleIndex()) {
                        suggestCameraId = CameraUtil.getSuggestWPlusTModeCameraId(mCameraId, SettingsUtil.sizeFromSettingString(keys.get(key)));
                        updateFlashTopButton();
                    }
                    reRequestCameraOpen(suggestCameraId);
                } else {
                    restartPreview(true);
                }
                /*@}*/
                return;
            case Keys.KEY_PICTURE_SIZE_FRONT_3D:
            case Keys.KEY_PICTURE_SIZE_FRONT:
                restartPreview(true);
                return;
            case Keys.KEY_JPEG_QUALITY:
                updateParametersPictureQuality();
                break;
            case Keys.KEY_CAMERA_COMPOSITION_LINE:
                break;
            case Keys.KEY_CAMER_ANTIBANDING:
                updateParametersAntibanding();
                break;
            case Keys.KEY_CAMERA_COLOR_EFFECT:
                updateParametersColorEffect();
                break;
            case Keys.KEY_CAMERA_PHOTOGRAPH_STABILIZATION:
                break;
            case Keys.KEY_CAMERA_HDR_NORMAL_PIC:
                updateParametersNormalHDR();
                break;
            case Keys.KEY_CAMERA_GRADIENTER_KEY:
                break;
            case Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH:
                break;
            case Keys.KEY_CAMERA_ZSL:
                restartPreview(false);
                break; // SPRD: fixed bug 651380 open hdr mode and the faceview invisible.
            case Keys.KEY_CAMERA_BEAUTY_ENTERED:
                if (mPaused
                        || mAppController.getCameraProvider()
                                .waitingForCamera()) {
                    return;
                }
                updateMakeLevel();
                break;
            case Keys.KEY_FLASH_MODE:
                if (mPaused
                        || mAppController.getCameraProvider()
                                .waitingForCamera()) {
                    return;
                }
                updateParametersFlashMode();
                break;
            case Keys.KEY_COUNTDOWN_DURATION:
                break;
            case Keys.KEY_CAMERA_HDR:
                if (isHdr()) {
                    resetAF();
                }
                updateParametersHDR();
                updateParametersNormalHDR();//SPRD:fix bug840683 for hdr normal is open,but hdr is close,then open hdr
                break;
            case Keys.KEY_CAMER_METERING:
                updateParametersMetering();
                break;
            case Keys.KEY_EXPOSURE:
                updateParametersExposureCompensation();
                break;
            case Keys.KEY_EXPOSURE_SHUTTERTIME:
                updateParametersExposureTime();
                break;
            case Keys.KEY_CAMERA_FOCUS_DISTANCE:
                updateParametersFocusDistanceByManual();
                break;
            case Keys.KEY_CAMERA_ISO:
                updateParametersISO();
                break;
            case Keys.KEY_WHITE_BALANCE:
                updateParametersWhiteBalance();
                break;
            case Keys.KEY_CAMERA_CONTRAST:
                updateParametersContrast();
                break;
            case Keys.KEY_CAMERA_SATURATION:
                updateParametersSaturation();
                break;
            case Keys.KEY_SCENE_MODE:
                updateParametersSceneMode();
                break;
            case Keys.KEY_FRONT_CAMERA_MIRROR:
                restartPreview(false);
                return;
            case Keys.KEY_CAMERA_GRID_LINES:
                updateParametersGridLine();
                break;
            case Keys.KEY_CAMERA_SENSOR_SELF_SHOT:
                updateParametersSensorSelfShot();
                break;
            case Keys.KEY_REFOCUS:
                mActivity.switchBlurModuel();
                return;
            case Keys.KEY_ADJUST_FLASH_LEVEL:
                updateParametersFlashLevel();
                break;
            case Keys.KEY_CAMERA_AI_SCENE_DATECT:
                updateParametersAiSceneDetect();
                updateBeautyButton();
                break;
            case Keys.KEY_AUTO_TRACKING:
                updateParametersAutoChasingRegion();
                break;
            case Keys.KEY_AI_DETECT_SMILE:
            case Keys.KEY_AI_DETECT_FACE_ATTRIBUTES:
                updateParametersAiDetect();
                break;
            case Keys.KEY_AUTO_3DNR_PARAMETER:
                updateAuto3DnrPatameter();
                break;
            case Keys.KEY_AUTO_ADD_LOGOWATERMARK:
                updateParametersLogoWatermark();
                break;
            case Keys.KEY_AUTO_ADD_TIMEWATERMARK:
                updateParametersTimeWatermark();
                break;
            case Keys.KEY_FILTER_PHOTO:
                Log.i(TAG, "chenyun1025_case Keys.KEY_FILTER_PHOTO ");
                if (mAppController.getCurrentModuleIndex() == FILTER && mActivity.getCameraAppUI().getExtendPanelParent().getVisibility() != View.VISIBLE) {
                    mActivity.getCameraAppUI().updateFilterPanelUI(View.VISIBLE);
                    mActivity.getCameraAppUI().getExtendPanelParent().setVisibility(View.VISIBLE);
                } else {
                    mActivity.switchFilterModuel();
                }
                return;
            case Keys.KEY_LIGHT_PORTIAIT:
                updateLightPortrait();
                updateLightPortraitDisplay();
                break;
            case Keys.KEY_PORTRAIT_REFOCUS_KEY:
            case Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY:
                updateLPortraitRefocus();
                break;
            case Keys.KEY_ADD_LEVEL:
                updateLevel();
                if(!getHasLevelAngle() && mDataModuleCurrent.getBoolean(Keys.KEY_ADD_LEVEL)){
                    mUI.makeLevelAppear();
                }
                break;
            case Keys.KEY_AE_LOCK:
                updateFlashTopButtonEnable();
                break;
            }
        }

        if (mCameraDevice != null && !mPaused) {
            mCameraDevice.applySettings(mCameraSettings);
            mCameraSettings = mCameraDevice.getSettings();
        }
    }

    private void updateFlashTopButtonEnable() {
        if(mDataModuleCurrent == null){
            return;
        }
        /*SPRD 1440733*/
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FLASH_MODE)){
            return;
        }
        try {
            if(mDataModuleCurrent.getBoolean(Keys.KEY_AE_LOCK)) {
                mAppController.getButtonManager().disableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
            } else {
                if (!((CameraActivity) mAppController).getIsButteryLow()) {
                    mAppController.getButtonManager().enableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
                }
            }
        } catch (IllegalStateException e){
            Log.d(TAG,"no flash button");
        }
    }

    private void updateParametersFlashLevel() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FLASH_MODE)){
            return;
        }
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_ADJUST_FLASH_LEVEL)){
            return;
        }

        mCameraSettings.setFlashLevel(mDataModuleCurrent.getInt(Keys.KEY_ADJUST_FLASH_LEVEL));
    }

    protected void restartPreview(boolean freezeScreen) {
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
        if (freezeScreen && !isUseSurfaceView()) {
            mAppController.freezeScreenUntilPreviewReady();
        }
        /* SPRD: fix bug620875 need make sure freeze screen draw on Time @{ */
        mHandler.post(new Runnable() {
            public void run() {
                if (!mPaused && mCameraDevice != null) {
                    startPreview(false);
                }
            }
        });
        /* @ } */
    }

    public void restoreToDefaultSettings(){
        // UI part reset
        updateUIGridLineToDefault();

        // camera device setting part reset
        /**
        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
        */
    }

    private void updateUIGridLineToDefault() {
        if (mDataModuleCurrent == null
                || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_GRID_LINES)) {
            return;
        }

        String grid = mDataModuleCurrent.getStringDefault(Keys.KEY_CAMERA_GRID_LINES);
        mAppController.getCameraAppUI().updateScreenGridLines(grid);
        Log.d(TAG, "updateUIGridLineToDefault = " + grid);
    }

    protected void resetShutterButton() {}

    protected void cancelCountDown() {
        //SPRD:fix bug 688106,updatePreviewUI has not been implemented when  cancelCountDown
        // Cancel on-going countdown.
        mUI.cancelCountDown();
        resetShutterButton();
        mUI.enablePreviewOverlayHint(true);//SPRD:fix bug648477
        mUI.showZoomProcessorIfNeeded();
        if (mDataModuleCurrent != null
                && !mActivity.getCameraAppUI().isBottomBarNull()
                && !mActivity.getCameraAppUI().isInIntentReview()// SPRD:Fix bug 497077
                && !mActivity.getCameraAppUI().isInFreezeReview()){// SPRD:Fix bug 398341
            mAppController.getCameraAppUI().transitionToCapture();
            mAppController.setShutterEnabled(true);
            isTouchCapturing = false;
            // SPRD:cancel CountDown cannot swipe
            mAppController.getCameraAppUI().setSwipeEnabled(true);

            /* SPRD: fix bug 677432 cancel CountDown cannot click @{ */
            if(getModuleTpye() == DreamModule.AUDIOPICTURE_MODULE && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)){
                mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
            }
            /* @} */

        }
        /* SPRD: fix bug 473462 add burst capture @{ */
        if (mIsContinousCaptureFinish) {
            mIsContinousCaptureFinish = false;
        }
        /* @} */
        /*SPRD: fix bug 599542 add for touch capture and count down @{*/
        isOnSingleTapUp = false;
        mTapUpX = 0;
        mTapUpY = 0;
        /* @} */
    }


    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    private void initializeControlByIntent() {
        if (mIsImageCaptureIntent) {
            setupCaptureParams();
        }
    }

    protected void changeCameraState() {
        Log.i(TAG, "changeCameraState mCameraState=" + mCameraState
                + " mContinuousCaptureCount=" + mContinuousCaptureCount);
        if (mContinuousCaptureCount <= 0 || !isBurstCapture()
                || (mCameraState == PREVIEW_STOPPED)) {// SPRD:Fix bug 388289/682650
            setCameraState(IDLE);
        }
    }

    protected void onPreviewStarted() {
        mAppController.onPreviewStarted();
        /**
         * SPRD: fix bug 473462 add burst capture @{
         * mAppController.setShutterEnabled(true); setCameraState(IDLE);
         */
        changeCameraState();//SPRD fix bug688349

        doOnPreviewStartedSpecial(isCameraIdle(), isHdr(), mActivity, mCameraDevice, mHandler,
                mDisplayOrientation, isCameraFrontFacing(), mUI.getRootView());
        /*
         * SPRD:Modify for ai detect @{ startFaceDetection();
         */
        updateFace();
        /* @} */
        onPreviewStartedAfter();
        if(isAELockPanding){
            Log.d(TAG,"set aelock before preview so do it in onPreviewStarted");
            mFocusManager.onLongPress(x,y);
            isAELockPanding = false;
        }
    }
    public void onPreviewStartedAfter() {}

    @Override
    public void onPreviewUIReady() {
        Log.i(TAG, "onPreviewUIReady");
        startPreview(true);
    }

    @Override
    public void onPreviewUIDestroyed() {
        Log.i(TAG, "onPreviewUIDestroyed,CameraDevice = " + mCameraDevice);
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.setPreviewTexture(null);
        stopPreview();
    }

    @Override
    public void startPreCaptureAnimation() {
        mAppController.startFlashAnimation(true);
    }

    @Override
    public void onShowImageDone(){
        if(mIsImageCaptureIntent && !isShutterEnabled()){
            Log.d(TAG,"show cover image done then set setShutterEnabled true");
            mAppController.setShutterEnabled(true);
        }
    }

    private void onCameraOpened() {
        openCameraCommon();
        initializeControlByIntent();
    }

    protected void switchCamera() {
        mActivity.switchFrontAndBackMode();
    }

    protected void mustDo() {
        mCameraId = mPendingSwitchCameraId;
        mMirror = isCameraFrontFacing();
        mFocusManager.setMirror(mMirror);
    }

    /**
     * Uses the {@link CameraProvider} to open the currently-selected camera
     * device, using {@link GservicesHelper} to choose between API-1 and API-2.
     */
    protected void requestCameraOpen() {
        /**
         * SPRD: fix bug47362 Log.v(TAG, "requestCameraOpen");
         */
        /*
         * SPRD:Fix bug 513841 KEY_CAMERA_ID may be changed during the camera in
         * background if user do switch camera action in contacts, and then we
         * resume the camera, the init function will not be called, so the
         * mCameraId will not be the latest one. Which will cause the switch
         * camera function can not be used. @{
         */
        if (mCameraId != mDataModule.getInt(Keys.KEY_CAMERA_ID)) {
            mCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
            /*SPRD:Fix bug 673906 After switching camera under secure camera, start normal camera, mode list does not update */
            mActivity.getCameraAppUI().updateModeList();
        }

        if (mCameraId == DreamUtil.FRONT_CAMERA && isSupportSensoSelfShotModule()) {
            mCameraId = CameraUtil.SENSOR_SELF_SHOT_CAMERA_ID;
        }

        /* @} */

        /*start add for w+t*/
        if (CameraUtil.isWPlusTAbility(mActivity,mActivity.getCurrentModuleIndex(),mCameraId)){
            mCameraId = CameraUtil.BACK_W_PLUS_T_PHOTO_ID;
        }
        /*end add for w+t*/

//        if (mActivity.getCurrentModuleIndex() == SettingsScopeNamespaces.AUTO_PHOTO && !DreamUtil.isFrontCamera(mActivity,mCameraId)) {
        if (CameraUtil.isTcamAbility(mActivity,mActivity.getCurrentModuleIndex(),mCameraId)) {
            mCameraId = CameraUtil.BACK_TRI_CAMERA_ID;
        }
        /**
         * SPRD: Change for New Feature VGesture and dream camera
         * original code
         * @{
        mActivity.getCameraProvider().requestCamera(
                mCameraId,
                GservicesHelper.useCamera2ApiThroughPortabilityLayer(mActivity
                         .getContentResolver()));
          */
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        doCameraOpen(mCameraId);
       /**
         * @}
         */
    }

    protected void doCameraOpen(int cameraId){
        mActivity.getCameraProvider().requestCamera(cameraId, useNewApi());
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

            mPendingSwitchCameraId = state;

            Log.d(TAG, "bbbb Start to switch camera. cameraId=" + state);
            // We need to keep a preview frame for the animation before
            // releasing the camera. This will trigger
            // onPreviewTextureCopied.
            // TODO: Need to animate the camera switch
            switchCamera();
        }
    };

    public final ButtonManager.ButtonCallback mFlashCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {

            if (mPaused
                    || mAppController.getCameraProvider().waitingForCamera()) {
                return;
            }

            Log.d(TAG, "mFlashCallback=" + state);

            // updateParametersFlashMode();
            // if (mCameraDevice != null) {
            // mCameraDevice.applySettings(mCameraSettings);
            // }
        }
    };

//    public final ButtonManager.ButtonCallback mMakeupCallback = new ButtonManager.ButtonCallback() {
//        @Override
//        public void onStateChanged(int state) {
//            // At the time this callback is fired, the camera id
//            // has be set to the desired camera.
//
//            if (mPaused
//                    || mAppController.getCameraProvider().waitingForCamera()) {
//                return;
//            }
//            if (mUI != null) {
//                updateMakeLevel();
//            }
//        }
//    };

    public final ButtonManager.ButtonCallback mMakeUpDisplayCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateExtendDisplay();
            updateColorEffectPhotoDisplay();
        }
    };
    protected void updateColorEffectPhotoDisplay() {
    }
    public Handler getmHandler() {
        return mHandler;
    }
    public final ButtonManager.ButtonCallback mLightPortraitDisplayCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateExtendDisplay();
        }
    };

    public final ButtonManager.ButtonCallback mPortraitRefocusDisplayCallback = new ButtonManager.ButtonCallback() {
        @Override
        public void onStateChanged(int state) {
            updateExtendDisplay();
        }
    };

    private void updateExtendDisplay() {
        updateLightPortraitDisplay();
        updateMakeUpDisplay();
        if (CameraUtil.isPortraitRefocusEnable() && !isCameraFrontFacing()) {
            updatePortraitRefocusDisplay();
        }
    }
        
    protected void updatePortraitRefocusDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY)) {
            return;
        }
        mUI.showPortraitRefocusOverLay(mDataModuleCurrent.getBoolean(Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY));
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

    protected void updateMakeUpDisplayStatus(boolean show){
        if(mMakeupController == null){
            return;
        }

        if(show){
            mMakeupController.resume(mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED));
        } else {
            mMakeupController.pause();
        }
    }

    protected void updateLightPortraitDisplay() {
        if (!DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule()
                .isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)) {
            return;
        }
        boolean display = mDataModuleCurrent.getBoolean(Keys.KEY_LIGHT_PORTIAIT_DISPLAY);
        if (mLightPortraitControllerDream != null) {
            mLightPortraitControllerDream.updateLightPortraitDisplay(display);
            showLightPortraitOverLay();
        }
    }

    private final View.OnClickListener mCancelCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onCaptureCancelled();
        }
    };

    private final View.OnClickListener mDoneCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onCaptureDone();
        }
    };

    private final View.OnClickListener mRetakeCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            onCaptureRetake();
        }
    };

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
//        bottomBarSpec.enableFlash = mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR) && !mIsBatteryLow;
        bottomBarSpec.enableHdr = true;
        //bottomBarSpec.hdrCallback = mHdrPlusCallback;
        bottomBarSpec.enableGridLines = false;
        if (mCameraCapabilities != null) {
            bottomBarSpec.enableExposureCompensation = true;
            bottomBarSpec.minExposureCompensation = mCameraCapabilities
                    .getMinExposureCompensation();
            bottomBarSpec.maxExposureCompensation = mCameraCapabilities
                    .getMaxExposureCompensation();
            bottomBarSpec.exposureCompensationStep = mCameraCapabilities
                    .getExposureCompensationStep();
        }

        bottomBarSpec.enableSelfTimer = true;
        bottomBarSpec.showSelfTimer = true;

        if (isImageCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = mDoneCallback;
            bottomBarSpec.showRetake = true;
            bottomBarSpec.retakeCallback = mRetakeCallback;
        }

        return bottomBarSpec;
    }

    // either open a new camera or switch cameras
    private void openCameraCommon() {
        mUI.onCameraOpened(mCameraCapabilities, mCameraSettings);
        /*
         * SPRD: All MUTEX OPERATION in onSettingsChanged function.
         * updateSceneMode();
         */
    }

    /* SPRD: fix bug 474665 add shutter sound switch @{ */
    private void updateCameraShutterSound() {
        Log.d(TAG, "updateCameraShutterSound mShutterSoundEnabled ="
                + mShutterSoundEnabled);
        if (mCameraDevice != null) {
            mCameraDevice.enableShutterSound(mShutterSoundEnabled);
        }
    }

    /* @} */

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized || mPaused) {
            return;
        }

        mUI.initializeFirstTime();

        // We set the listener only when both service and shutterbutton
        // are initialized.
        getServices().getMemoryManager().addListener(this);

        mNamedImages = new NamedImages();

        mFirstTimeInitialized = true;
        addIdleHandler();

        /**
         * SPRD: Fix bug 572631, optimize camera launch time,
         * Original Code
         *
        mActivity.updateStorageSpaceAndHint(null);
         */
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
        getServices().getMemoryManager().addListener(this);
        mNamedImages = new NamedImages();
        mUI.initializeSecondTime(mCameraCapabilities, mCameraSettings);
    }

    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Storage.ensureOSXCompatible();
                return false;
            }
        });
    }

    /**
     * The method implement from FocusOverlayManager.Listener interface;
     * To controller Face detection function start by call CameraProxy.startFaceDetection method
     */
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
        mFaceDetectionStarted = true;
        mUI.onStartFaceDetection(mDisplayOrientation, isCameraFrontFacing());
        /*
         * }
         * 
         * @}
         */
    }

    @Override
    public void stopFaceDetection() {
        mUI.clearFaces();
        Log.d(TAG, "stopFaceDetection mFaceDetectionStarted=" + mFaceDetectionStarted);
        if (mCameraDevice == null) {//SPRD:fix bug625571 for OOM
            return;
        }
        /*
         * SPRD:Modify for ai detect @{ if
         * (mCameraCapabilities.getMaxNumOfFacesSupported() > 0) {
         */
        mCameraDevice.setFaceDetectionCallback(null, null);
        mCameraDevice.stopFaceDetection();
        mFaceDetectionStarted = false;
        /* SPRD:fix bug501085 Click screen,the face detect still appear@{ */
        mUI.pauseFaceDetection();
        /* }@ */
        mUI.clearFaces();
        /*
         * }
         * 
         * @}
         */
    }

    private final class ShutterCallback implements CameraShutterCallback {

        private final boolean mNeedsAnimation;

        public ShutterCallback(boolean needsAnimation) {
            mNeedsAnimation = needsAnimation;
        }

        @Override
        public void onShutter(CameraProxy camera) {
            mShutterCallbackTime = System.currentTimeMillis();
            mShutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.i(TAG, "mShutterLag = " + mShutterLag + "ms");
            if (mNeedsAnimation) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateAfterShutter();
                    }
                });
            }
        }
    }

    private final class PostViewPictureCallback implements CameraPictureCallback {
        @Override
        public void onPictureTaken(byte[] data, CameraProxy camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToPostViewCallbackTime = "
                    + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                    + "ms");
        }
    }

    private final class RawPictureCallback implements CameraPictureCallback {
        @Override
        public void onPictureTaken(byte[] rawData, CameraProxy camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    }

    private static class ResizeBundle {
        byte[] jpegData;
        float targetAspectRatio;
        ExifInterface exif;
    }

    public static class JpegBundle {
        public byte[] jpegData;
        public float targetAspectRatio;
        public ExifInterface exif;
    }

    /**
     * @return Cropped image if the target aspect ratio is larger than the jpeg
     *         aspect ratio on the long axis. The original jpeg otherwise.
     */
    private ResizeBundle cropJpegDataToAspectRatio(ResizeBundle dataBundle) {

        final byte[] jpegData = dataBundle.jpegData;
        final ExifInterface exif = dataBundle.exif;
        float targetAspectRatio = dataBundle.targetAspectRatio;

        Bitmap original = BitmapFactory.decodeByteArray(jpegData, 0,
                jpegData.length);
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int newWidth;
        int newHeight;

        if (originalWidth > originalHeight) {
            newHeight = (int) (originalWidth / targetAspectRatio);
            newWidth = originalWidth;
        } else {
            newWidth = (int) (originalHeight / targetAspectRatio);
            newHeight = originalHeight;
        }
        int xOffset = (originalWidth - newWidth) / 2;
        int yOffset = (originalHeight - newHeight) / 2;

        if (xOffset < 0 || yOffset < 0) {
            return dataBundle;
        }

        Bitmap resized = Bitmap.createBitmap(original, xOffset, yOffset,
                newWidth, newHeight);
        exif.setTagValue(ExifInterface.TAG_PIXEL_X_DIMENSION, new Integer(
                newWidth));
        exif.setTagValue(ExifInterface.TAG_PIXEL_Y_DIMENSION, new Integer(
                newHeight));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        resized.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        dataBundle.jpegData = stream.toByteArray();
        return dataBundle;
    }

    protected boolean isNeedPlaySound() {
        if(mAppController.isPlaySoundEnable() && ( mIsFirstCallback || inBurstMode())) {
            return true;
        }
        return false;
    }

    protected MediaActionSound mCameraSound;// SPRD:fix bug473462

    protected void procNotIntent(final byte[] data) {
        // split this method for CCN optimization , Bug 839474
        if (!inBurstMode()) {
            /* SPRD:fix bug620386 move up the thumbnail at Interval module @{ */
            if (getModuleTpye() != DreamModule.INTERVAL_MODULE) {
                mThumbnailHasInvalid = true;
            }
            /* @} */
            if (mIsFirstCallback || isHdrNormalOn() && (isHdr() && (mIsHdrPicture && isNeedThumbCallBack() || !isNeedThumbCallBack()) || isHdrPicture)) {
                startPeekAnimation(data);
            }
        }
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
            mHasCaptureCount = 0;
            //mTotalCaputureCount = 0;
        }

        private void judgePlaySoundAndShutterAgain(final byte[] data) {
            // split this method for CCN optimization , Bug 839474
            Log.e(TAG,"judgePlaySoundAndShutterAgain isNeedPlaySound = " + isNeedPlaySound());
            if (isNeedPlaySound()) {
                if (mCameraSound != null) {
                    mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                }
            }
            /*
             * Add For Dream Camera in IntervalPhotoModule/ContinuePhotoModule
             *
             * @{
             */
            if (!shutterAgain() && !isAlgorithmProcessOnApp) {
                if(!mBurstNotShowShutterButton && !getServices().getMediaSaver().isQueueFull() && !mIsImageCaptureIntent && getModuleTpye()!=DreamModule.AUDIOPICTURE_MODULE) {
                    mAppController.setShutterEnabled(true);
                }
                /* dream test 50 @{ */
                doSomethingWhenonPictureTaken(data); // for dream camera if it want to do something
            }
            /* @} */
        }

        private boolean needReturnFromPictureTaken() {
            // split this method for CCN optimization , Bug 839474
            if (mPaused && (mIsFirstCallback || !isNeedThumbCallBack()))
                return true;
            return false;
        }


        private void procNamedImages() {
            // split this method for CCN optimization , Bug 839474
            if (mNamedImages == null) {
                mNamedImages = new NamedImages();
            }
            if (isNeedThumbCallBack() && !mIsFirstCallback && mIsNeedUpdatThumb) {
                mNamedImages.nameNewImageLarge(mCaptureStartTime);
            } else {
                mNamedImages.nameNewImage(mCaptureStartTime);
            }
        }

        private boolean judgeInIntervalAndBurst() { // split this method for CCN optimization , Bug 839474
            // SPRD: fix Bug 707864
            // if picture return later than press "back" , do not save it
            if(getModuleTpye() == DreamModule.INTERVAL_MODULE && mBackPressed) {
                mBackPressed = false;
                mUI.enablePreviewOverlayHint(true);
                Log.i(TAG, "onPictureTaken enablePreviewOverlayHint!");
                mBurstAllDone = false;
                return false;
            }

            /* SPRD:Fix bug 411062/688352 @{ */
            if (getContinuousCount() <= 1
                    && getModuleTpye() != DreamModule.AUDIOPICTURE_MODULE
                    && getModuleTpye() != DreamModule.INTERVAL_MODULE//SPRD:Fix bug927249
                    && !shutterAgain()
                    && isLastCallback()) {
                mUI.enablePreviewOverlayHint(true);
                Log.i(TAG, "onPictureTaken enablePreviewOverlayHint!");
            }
            /* @} */
            return true;
        }

        @Override
        public void onPictureTaken(final byte[] originalJpegData,
                final CameraProxy camera) {
            if(mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_3DNR_PARAMETER)){
                mCameraSettings.set3DNREnable(0);
            }
            Log.i(TAG, "onPictureTaken mCameraState=" + mCameraState
                    + " mContinuousCaptureCount = " + mContinuousCaptureCount
                    + " isBurstCapture()= " + isBurstCapture()
                    + " mIsContinousCaptureFinish ="
                    + mIsContinousCaptureFinish + "mHasCaptureCount = "
                    + mHasCaptureCount + " isAlgorithmProcessOnApp = " + isAlgorithmProcessOnApp);
            final boolean burstMode = isBurstCapture() || CameraUtil.instance().isSameDateTaken(mCaptureStartTime);
            mThumbnailHasInvalid = false;

            if (mHandler.hasMessages(MSG_RESET_KEY_ENTER)) {
                mHandler.removeMessages(MSG_RESET_KEY_ENTER);
            }
            mHandler.sendEmptyMessageDelayed(MSG_RESET_KEY_ENTER, RESET_KEY_ENTER_DELAY);//SPRD:fix bug1165998

            if (needReturnFromPictureTaken()) { // CCN = 4
                mBurstAllDone = false;
                return;
            }
            //SPRD: fix bug925703
            judgePlaySoundAndShutterAgain(originalJpegData); // CCN = 7

            mUI.enableUIAfterTakepicture();

            if (mContinuousCaptureCount > 0) {
                mContinuousCaptureCount--;
            }

            if (!mIsImageCaptureIntent) {
                procNotIntent(originalJpegData); // CCN = 10
                if (isNeedThumbCallBack() && mIsFirstCallback && !mIsNeedUpdatThumb) {
                    mAppController.getCameraAppUI().setThumbButtonClick(true);
                    mAppController.getCameraAppUI().setThumbButtonUnClickable(true);
                    mIsFirstCallback = false;
                    mFirstHasStartCapture = true;
                    mNeedRemoveMessgeDelay = false;
                    return;
                }
            } else {
                /* SPRD: fix bug793544 must cancel autofcous after af when caf mode @{*/
                if (mFocusManager.getFocusMode(mCameraSettings
                        .getCurrentFocusMode(),mDataModule.getString(Keys.KEY_FOCUS_MODE)) == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                    mCameraDevice.cancelAutoFocus();
                }
                /* @} */
                stopPreview();
            }
            if (mSceneMode == CameraCapabilities.SceneMode.HDR) {
                mUI.setSwipingEnabled(true);
            }

            procNamedImages(); // CCN = 4

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime = mPostViewPictureCallbackTime
                        - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime = mRawPictureCallbackTime
                        - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mRawPictureCallbackTime;
            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");

            /**
             * SPRD: fix bug 473462 add burst capture if
             * (!mIsImageCaptureIntent) {
             */
            if (!mIsImageCaptureIntent
                    && mContinuousCaptureCount <= 0 && isLastCallback()) {
                setupPreview();
            }

            if (isLastCallback()) {
                isTouchCapturing = false;//Sprd: fix bug940438
                if(getModuleTpye() != DreamModule.AUDIOPICTURE_MODULE)
                mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
                mAppController.getCameraAppUI().setThumbButtonUnClickable(false);
            } else {
                mAppController.getCameraAppUI().setThumbButtonClick(true);
            }

            if(false == judgeInIntervalAndBurst()) // CCN = 7
                return;

            long now = System.currentTimeMillis();
            mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            Log.v(TAG, "mJpegCallbackFinishTime = " + mJpegCallbackFinishTime + "ms");
            mJpegPictureCallbackTime = 0;

            final ExifInterface exif = Exif.getExif(originalJpegData);
            final NamedEntity name = mNamedImages.getNextNameEntity();

            if (mShouldResizeTo16x9) {
                Log.i(TAG, "mShouldResizeTo16x9");
                final ResizeBundle dataBundle = new ResizeBundle();
                dataBundle.jpegData = originalJpegData;
                dataBundle.targetAspectRatio = ResolutionUtil.NEXUS_5_LARGE_16_BY_9_ASPECT_RATIO;
                dataBundle.exif = exif;
                new AsyncTask<ResizeBundle, Void, ResizeBundle>() {

                    @Override
                    protected ResizeBundle doInBackground(
                            ResizeBundle... resizeBundles) {
                        return cropJpegDataToAspectRatio(resizeBundles[0]);
                    }

                    @Override
                    protected void onPostExecute(ResizeBundle result) {
                        saveFinalPhoto(result.jpegData, name, result.exif,
                                camera,burstMode);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataBundle);
            } else {
                if (name != null) {
                    if (isAlgorithmProcessOnApp) {
                        final JpegBundle jpegBundle = new JpegBundle();
                        jpegBundle.jpegData = originalJpegData;
                        jpegBundle.exif = exif;
                        new AsyncTask<JpegBundle, Void, JpegBundle>() {
                            @Override
                            protected JpegBundle doInBackground(
                                    JpegBundle... jpegBundles) {
                                Log.d(TAG, "doPreSaveFinalPhoto doInBackground ");
                                return doPreSaveFinalPhoto(jpegBundles[0]);
                            }
                            @Override
                            protected void onPostExecute(JpegBundle result) {
                                Log.d(TAG, "doPreSaveFinalPhoto end ");
                                if (!mBurstNotShowShutterButton && !getServices().getMediaSaver().isQueueFull() && !mIsImageCaptureIntent && getModuleTpye() != DreamModule.AUDIOPICTURE_MODULE) {
                                    mAppController.setShutterEnabled(true);
                                    mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, true);
                                }
                                mIsFirstCallback = false;
                                saveFinalPhoto(result.jpegData, name, result.exif,
                                        camera, burstMode);
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jpegBundle);
                    }
                    else{
                        saveFinalPhoto(originalJpegData, name, exif, camera, burstMode);
                    }
                }
            }
            if (!isAlgorithmProcessOnApp)
                mIsFirstCallback = false;
            mBurstAllDone = false;
        }

        public void procOthersForSaveFinalPhoto(final byte[] jpegData) {

            // Check this in advance of each shot so we don't add to shutter
            // latency. It's true that someone else could write to the SD card
            // in the mean time and fill it, but that could have happened
            // between the shutter press and saving the JPEG too.
            /* SPRD: fix bug620875 continue caputre not need more AsyncTask @{ */
            if (!isBurstCapture()
                    || (mContinuousCaptureCount > 1 && mIsContinousCaptureFinish)
                    || mHasCaptureCount == getContinuousCount()
                    || (mHasCaptureCount % 10)  == 0)
                mActivity.updateStorageSpaceAndHint(null);
            /* @} */
            Log.i(TAG, "saveFinalPhoto end! mCameraState=" + mCameraState
                    + " mContinuousCaptureCount=" + mContinuousCaptureCount);
            installIntentFilter();
        }

        private Size getSize(final ExifInterface exif , int orientation) {
            // split this method for CCN optimization , Bug 839474
            Size r = null;
            Integer exifWidth = exif
                    .getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
            Integer exifHeight = exif
                    .getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);

            if (mShouldResizeTo16x9 && exifWidth != null && exifHeight != null) {
                r = new Size(exifWidth , exifHeight);
            } else {
                Size s = new Size(mCameraSettings.getCurrentPhotoSize());

                int scale = 1;
                if (getModuleTpye() == DreamModule.MACRO_PHOTO_MODULE && CameraUtil.isSRFusionEnable() && !CameraUtil.isHighResolutionScaleTest()) {
                    scale = CameraUtil.getHighResolutionScale();
                }
                if ((mJpegRotation + orientation) % 180 == 0 || getModuleTpye() == DreamModule.TD_PHOTO_MODULE)
                    r = new Size(s.width() * scale, s.height() * scale);
                else
                    r = new Size(s.height() * scale, s.width() * scale);
            }
            return r;
        }

        private String needAddHdr(String title) {
            // split this method for CCN optimization , Bug 839474
            String ret = title;
            if(saveHdrPicture() || isHdrNormalOn() && (isHdr() && (!isNeedThumbCallBack() && mIsFirstCallback  || isNeedThumbCallBack() && mIsHdrPicture) || isHdrPicture && mIsFirstCallback)){
                if(CameraUtil.isFdrSupport()){
                    ret = ret + "_FDR";
                }else {
                    ret = ret + "_HDR";
                }
            }
            return ret;
        }

        private void modifyCameraPicType(final ExifInterface exif , boolean burstMode , String title) {
            // split this method for CCN optimization , Bug 839474
            if (burstMode && getModuleTpye() == DreamModule.CONTINUE_MODULE) {
                exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_BURST));
            } else if (getModuleTpye() == DreamModule.AUDIOPICTURE_MODULE) {
                if (isHdr() || isHdrPicture) {
                    if(CameraUtil.isFdrSupport()){
                        exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_AUDIO_FDR_PIC));
                    }else {
                        exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_AUDIO_HDR_PIC));
                    }
                } else {
                    exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_AUDIO_PIC));
                }
            } else if (title.contains("_HDR") && getModuleTpye() != DreamModule.REFOCUS_MODULE) {
                Integer cameraType = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
                if (cameraType != null && cameraType.intValue() == TYPE_AI_PHOTO) {
                    exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_AI_HDR));
                } else {
                    exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_HDR));
                }
            } else if (title.contains("_FDR") && getModuleTpye() != DreamModule.REFOCUS_MODULE) {
                Integer cameraType = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
                if (cameraType != null && cameraType.intValue() == TYPE_AI_PHOTO) {
                    exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_AI_FDR));
                } else {
                    exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(TYPE_FDR));
                }
            }
        }

        private boolean isConditionForBurstPhoto(boolean burstMode) {
            // split this method for CCN optimization , Bug 839474
            return (burstMode && CameraUtil.isBurstAlbumEnabled() && getModuleTpye() == DreamModule.CONTINUE_MODULE);
        }

        private boolean isConditionForHdrPhoto() {
            // split this method for CCN optimization , Bug 839474
            return (saveHdrPicture() ||  isHdrNormalOn() && (isHdr() && mIsHdrPicture || isHdrPicture && mIsFirstCallback));
        }

        private void procHdrNormalCapture(String title , final byte[] jpegData ,
                final ExifInterface exif , long date , int width , int height , int orientation) {
            // split this method for CCN optimization , Bug 839474
            if (mIsHdrPicture) {
                Uri updateUri = mActivity != null ? mActivity
                        .getThumbUri(title) : null;
                Log.i(TAG, "updateUri = " + updateUri);
                updateImage(updateUri, jpegData, title, date, mLocation,
                        width, height, orientation, exif,
                        mOnMediaSavedListener, FilmstripItemData.MIME_TYPE_JPEG);
                if (!isHdrNormalOn()) {
                    removePauseDelayedMessage();
                }
                mIsHdrPicture = false;
            } else {
                addImage(jpegData, title, date, mLocation, width, height,
                        orientation, exif, mOnMediaSavedListener);
                removePauseDelayedMessage();
            }
        }

        private String debugMode(String s , final byte[] jpegData) {
            // split this method for CCN optimization , Bug 839474
            String title = s;
            if (mDebugUri != null) {
                // If using a debug uri, save jpeg there.
                saveToDebugUri(jpegData);

                // Adjust the title of the debug image shown in mediastore.
                if (s != null) {
                    title = DEBUG_IMAGE_PREFIX + s;
                }
            }
            return title;
        }

        void saveFinalPhoto(final byte[] jpegData, NamedEntity name,
                final ExifInterface exif, CameraProxy camera, boolean burstMode) {
            Log.i(TAG, "saveFinalPhoto start!");
            int orientation = Exif.getOrientation(exif);

            Log.i(TAG, "saveFinalPhoto title=" + name.title + ".jpg");// SPRD:Fix
                                                                      // bug
                                                                      // 419844
            mShutterTouchCoordinate = null;

            Log.i(TAG, "saveFinalPhoto mIsImageCaptureIntent="
                    + mIsImageCaptureIntent);
            // SPRD: Fix 597206 Adds new features 3DPhoto
            name.title = getNameTitle(name.title);
            if (!mIsImageCaptureIntent) {
                // Calculate the width and the height of the jpeg.
                int width, height;
                Size s = getSize(exif , orientation);   // CCN = 6
                width = s.width();
                height = s.height();

                String title = name.title;
                long date = name.date;

                // Handle debug mode outputs
                title = debugMode(title , jpegData); // CCN = 3

                if (title == null) {
                    Log.e(TAG, "Unbalanced name/data pair");
                } else {
                    if (date == -1) {
                        date = mCaptureStartTime;
                    }
                    int heading = mHeadingSensor.getCurrentHeading();
                    if (heading != HeadingSensor.INVALID_HEADING) {
                        // heading direction has been updated by the sensor.
                        ExifTag directionRefTag = exif.buildTag(
                                ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                                ExifInterface.GpsTrackRef.MAGNETIC_DIRECTION);
                        ExifTag directionTag = exif.buildTag(
                                ExifInterface.TAG_GPS_IMG_DIRECTION,
                                new Rational(heading, 1));
                        exif.setTag(directionRefTag);
                        exif.setTag(directionTag);
                    }

                    title = needAddHdr(title); // CCN = 8

                    /* modify camera pic type */
                    modifyCameraPicType(exif , burstMode , title); // CCN = 7

                    /* modify camera pic type */
                    /*
                     * SPRD: Fix bug 535110, Photo voice record. @{
                     * Original Android code :
                    getServices().getMediaSaver().addImage(jpegData, title,
                            date, mLocation, width, height, orientation, exif,
                            mOnMediaSavedListener);
                     */
                    if (needInitData()) {
                        initData(jpegData, title, date, width, height,
                                orientation, exif, mLocation,
                                mOnMediaSavedListener);
                    } else {
                        if (isConditionForBurstPhoto(burstMode)) {
                            XmpBuilder xmpBuilder = new XmpBuilder();
                            xmpBuilder.setBurstId(mUUID);
                            if (title.split("_").length == 3) {
                                title = title + "_COVER";
                                exif.setTagValue(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(
                                        TYPE_BURST_COVER));
                                xmpBuilder.setIsPrimaryInBurst(true);
                            } else {
                                xmpBuilder.setIsPrimaryInBurst(false);
                            }
                            title = modifyBurstImgName(title);
                            getServices().getMediaSaver().addImage(
                                        jpegData, title, date, mLocation, width, height,
                                        orientation, exif, mOnMediaSavedListener, FilmstripItemData.MIME_TYPE_JPEG, null, xmpBuilder);
                        } else {
                            if (isNeedThumbCallBack() && mIsNeedUpdatThumb) {
                                if (mIsFirstCallback) {
                                    mFirstHasStartCapture = true;
                                    mNeedRemoveMessgeDelay = false;
                                    Size thumbSize = new Size(mCameraSettings.getExifThumbnailSize());
                                    if (mJpegRotation % 180 != 0) {
                                        height = thumbSize.getWidth();
                                        width = thumbSize.getHeight();
                                    } else {
                                        width = thumbSize.getWidth();
                                        height = thumbSize.getHeight();
                                    }

                                    Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
                                    Log.i(TAG, "TAG_CAMERATYPE_IFD before = " + val );
                                    exif.setTag(exif.buildTag(ExifInterface.TAG_CAMERATYPE_IFD, new Integer(
                                            TYPE_THUMBNAIL)));
                                    exif.setTag(exif.buildTag(ExifInterface.TAG_ORIENTATION,
                                            ExifInterface.getOrientationValueForRotation(orientation)));
                                    getServices().getMediaSaver().addImage(
                                            jpegData, title, date, null, width,height,
                                            orientation, exif, mOnMediaSavedListener,
                                            FilmstripItemData.MIME_TYPE_JPEG_THUMB, null);
                                    val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
                                    Log.i(TAG, "TAG_CAMERATYPE_IFD after = " + val + " orientation = " + Exif.getOrientation(exif));
                                } else {
                                    //add for hdr normal fast capture
                                    procHdrNormalCapture(title , jpegData , exif , date , width , height , orientation); // CCN = 4
                                }
                            } else {
                                if(isConditionForHdrPhoto()) { // CCN = 4
                                    getServices().getMediaSaver().addImage(
                                            jpegData, title, date, mLocation, width,height,//Sprd:fix bug1016414 hdr photo need location info
                                            orientation, exif, mOnMediaSavedListener,
                                            FilmstripItemData.MIME_TYPE_JPEG, null);
                                    mIsHdrPicture = false;
                                } else {
                                    addImage(jpegData, title, date, mLocation, width, height,
                                            orientation, exif, mOnMediaSavedListener);
                                }
                                removePauseDelayedMessage(); // move to here for Bug 1166905
                            }
                        }
                        mUI.setPictureInfo(width, height,orientation);//SPRD:fix bug 625571
                    }
                    /* @} */
                }
                // Animate capture with real jpeg data instead of a preview
                // frame.
                /**
                 * SPRD:fix bug 473462 add burs capture
                 * mUI.animateCapture(jpegData, orientation, mMirror);
                 */
            } else {
                Log.i(TAG, "saveFinalPhoto mQuickCapture=" + mQuickCapture);
                mJpegImageData = jpegData;
                if (!mQuickCapture) {
                    Log.v(TAG, "showing UI");
                    mUI.showCapturedImageForReview(jpegData, orientation,false);//SPRD:fix bug1278119
                    //Sprd:Fix bug961659
                    if(mAELockPanel != null) {
                        mAELockPanel.hide();
                    }
                    /* SPRD:fix bug1000660 need reset ae/af lock when intent capture @{*/
                    if (isAELock) {
                        setAELock(false);
                    }
                    /* @} */
                } else {
                    onCaptureDone();
                }
            }

            procOthersForSaveFinalPhoto(jpegData); // CCN = 12
        }
    }

    synchronized public JpegBundle doPreSaveFinalPhoto(JpegBundle jpegBundle) {
        return jpegBundle;
    }

    protected boolean needInitData() {
        return false;
    }

    /**
     * this method is for modify burst file name to making burst files could be recognized by google photos whether you are signed in or not
     * @param name like "IMG_20180615_130149"
     * @return burstName like "00000IMG_00000_BURST20180615130125_COVER" or "00001IMG_00001_BURST20180615130125"
     */
    private String modifyBurstImgName(String name) {
        if (name == null) {
            return null;
        }
        String burstName = null;
        String[] splitName = name.split("_");
        int picCount;
        if (splitName[3].equals("COVER")) {
            burstName = String.format("%05d", 0) + "IMG_"+String.format("%05d", 0) + "_BURST" + splitName[1] + splitName[2] + "_COVER";
        } else {
            picCount = Integer.valueOf(splitName[3]).intValue() ;
            burstName = String.format("%05d", picCount) + "IMG_"+String.format("%05d", picCount) + "_BURST" + splitName[1] + splitName[2];
        }
        return burstName;
    }

    private void removePauseDelayedMessage() {
        mFirstHasStartCapture = false;
        if (mNeedRemoveMessgeDelay && mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void startPeekAnimation(final byte[] jpegData) {
        ExifInterface exif = Exif.getExif(jpegData);
        Bitmap bitmap = exif.getThumbnailBitmap();
        Log.i(TAG, "startPeekAnimation bitmap = " + bitmap);
        /* remove this extra rotation after the driver bug is fixed @{ */
        int orientation = Exif.getOrientation(exif);
        if (bitmap == null) {
            long t1 = System.currentTimeMillis();
            //use for test large yuv data
            byte[] decodeBuffer = new byte[32 * 1024];
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 8;
            opts.inTempStorage = decodeBuffer;
            bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length/*, opts*/);
            long t2 = System.currentTimeMillis();
            Log.v(TAG, "startPeekAnimation, decode jpg time = " + (t2 - t1));
        }
        long t3 = System.currentTimeMillis();
        if (orientation != 0 && bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.setRotate(orientation);
            Bitmap rotatedBitmap = bitmap;
            bitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            rotatedBitmap.recycle();
        }
        Log.v(TAG, "startPeekAnimation, rotatedBitmap time = " + (System.currentTimeMillis() - t3));
        /* @} */
        /* SPRD:fix bug620386 move up the thumbnail at Interval module @{ */
        if (getModuleTpye() == DreamModule.INTERVAL_MODULE) {
            if (bitmap != null) {
                updateIntervalThumbnail(bitmap);
            }
        } else {
            mActivity.startPeekAnimation(bitmap);
        }
        /* @} */
    }

    private final class AutoFocusCallback implements CameraAFCallback {
        @Override
        public void onAutoFocus(boolean focused, CameraProxy camera) {
            if (mPaused) {
                return;
            }

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            Log.i(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms   focused = "
                    + focused);
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused, false);
        }
    }

    private final class AutoFocusMoveCallback implements CameraAFMoveCallback {
        @Override
        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
            // SPRD: ui check 210
            if (!mUI.isCountingDown() && isShutterClicked()) return;
            mFocusManager.onAutoFocusMoving(moving);
        }
    }

    /**
     * This class is just a thread-safe queue for name,date holder objects.
     */
    public static class NamedImages {
        private final Vector<NamedEntity> mQueue;

        public NamedImages() {
            mQueue = new Vector<NamedEntity>();
        }

        public void nameNewImage(long date) {
            NamedEntity r = new NamedEntity();
            r.title = CameraUtil.instance().createJpegName(date);
            r.date = date;
            mQueue.add(r);
        }

        //SPRD:fix bug761679
        public void nameNewImageLarge(long date) {
            NamedEntity r = new NamedEntity();
            r.title = CameraUtil.instance().createJpegNameLarge(date);
            r.date = date;
            mQueue.add(r);
        }

        public NamedEntity getNextNameEntity() {
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    return mQueue.remove(0);
                }
            }
            return null;
        }

        public static class NamedEntity {
            public String title;
            public long date;
        }
    }

    protected void setCameraState(int state) {
        mCameraState = state;
        switch (state) {
        case PREVIEW_STOPPED:
        case SNAPSHOT_IN_PROGRESS:
        case SWITCHING_CAMERA:
            // TODO: Tell app UI to disable swipe
            break;
        case PhotoController.IDLE:
            // TODO: Tell app UI to enable swipe
            break;
        }
    }

    private void animateAfterShutter() {
        // Only animate when in full screen capture mode
        // i.e. If monkey/a user swipes to the gallery during picture taking,
        // don't show animation
        if (!mIsImageCaptureIntent) {
            mUI.animateFlash();
        }
    }

    @Override
    public boolean capture() {
        Log.i(TAG, "capture");
        // If we are already in the middle of taking a snapshot or the image
        // save request is full then ignore.
        if (mCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA) {
            return false;
        }
        setCameraState(SNAPSHOT_IN_PROGRESS);

        mCaptureStartTime = System.currentTimeMillis();

        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;


        /* SPRD: add hide shutter animation after capture when HDR is on
        final boolean animateBefore = (mSceneMode == CameraCapabilities.SceneMode.HDR);
        if (animateBefore) {
            animateAfterShutter();
        }
        */
        updateFilterType();
        /* SPRD: fix bug672841 add for cancel burst when focusing state, burst can not stop*/
        if (!mIsImageCaptureIntent) {
            updateParametersBurstCount();
            mContinuousCaptureCount = getContinuousCount();
        }
        /* @} */
        Location loc = mActivity.getLocationManager().getCurrentLocation();
        Log.i(TAG,"location info:"+loc);
        CameraUtil.setGpsParameters(mCameraSettings, loc);
        boolean isHasEnteredBeauty = mDataModule.getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED);
        if (!isHasEnteredBeauty) {
            setParametersHighISO(true);
        }
        /* SPRD:fix bug 823475 clear ae corrdinate before capture when after taf @ */
        if (mFocusManager != null) {
            mFocusManager.updateFocusState();
            setMeteringAreasIfSupported();
        }
        /* @} */
        if(isAutoHdr() && isHdrScene){
            Log.e(TAG,"auto hdr detect scene is hdr so change the scene to hdr");
            mCameraSettings.setSceneMode(CameraCapabilities.SceneMode.HDR);
        }
        if (mSmileCapture) {
            mCameraSettings.setSmileCapture(1);
            mSmileCapture = false;
        }
        mCameraDevice.applySettings(mCameraSettings);

        // Set JPEG orientation. Even if screen UI is locked in portrait, camera
        // orientation should
        // still match device orientation (e.g., users should always get
        // landscape photos while
        // capturing by putting device in landscape.)
        Characteristics info = mActivity.getCameraProvider()
                .getCharacteristics(mCameraId);
        int sensorOrientation = info.getSensorOrientation();
        int deviceOrientation = mAppController.getOrientationManager()
                .getDeviceOrientation().getDegrees();
        boolean isFrontCamera = info.isFacingFront();
        mJpegRotation = CameraUtil.getImageRotation(sensorOrientation,
                deviceOrientation, isFrontCamera);
        Log.i(TAG, " sensorOrientation = " + sensorOrientation
                + " ,deviceOrientation = " + deviceOrientation
                + " isFrontCamera = " + isFrontCamera);
        mCameraDevice.setJpegOrientation(mJpegRotation);
        Log.i(TAG, "takePicture start!");
        isHdrPicture = isAutoHdr() && isHdrScene;
        if (mReceiver != null) {
            //mActivity.unregisterReceiver(mReceiver);
            mActivity.unRegisterMediaBroadcastReceiver();
            mReceiver = null;
        }

        mIsHdrPicture = true;
        mIsFirstCallback = true;
        mFirstHasStartCapture = false;
        mIsNormalHdrDone = false;//SPRD:fix bug784774
        doCaptureSpecial();
        mCameraDevice.takePicture(mHandler,
        /**
         * SPRD: fix bug462021 remove capture animation
         * 
         * @{ new ShutterCallback(!animateBefore),
         */
        new ShutterCallback(CameraUtil.isCaptureAnimatationEnable() && !isBurstCapture() && !isAudioCapture() &&!isCameraFrontFacing()),//SPRD:fix bug1154938/1137366/1162992/1201491
        /**
         * @}
         */
        mRawPictureCallback, mPostViewPictureCallback, new JpegPictureCallback(loc));

        /**
         * SPRD: fix bug 473462 add for burst capture
         * mNamedImages.nameNewImage(mCaptureStartTime);
         */

        mFaceDetectionStarted = false;
        return true;
    }

    @Override
    public void setFocusParameters() {
        if (mCameraState != PREVIEW_STOPPED) {
            setCameraParameters(UPDATE_PARAM_PREFERENCE);
        }
    }

    /*SPRD:fix bug 620875 avoid 3d photo use burst mode @{ */
    public boolean checkCameraProxy() {
        boolean preferNewApi = useNewApi();
        return (preferNewApi == getCameraProvider().isNewApi())
                && mCameraId == getCameraProvider().getCurrentCameraId().getLegacyValue();
    }
    /* @} */

    @Override
    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (mPaused) {
            return;
        }

        /*SPRD:fix bug 620875 avoid 3d photo use burst mode @{ */
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
        CameraUtil.setHDRSupportable(mCameraDevice.getCapabilities().supports(CameraCapabilities.SceneMode.HDR));
        if(mDataModuleCurrent != null && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && getModuleTpye() != DreamModule.REFOCUS_MODULE && getModuleTpye() != DreamModule.FUSION_PHOTO_MODULE
                && !(getModuleTpye() == DreamModule.MACRO_PHOTO_MODULE && CameraUtil.isSRFusionEnable())) {
            CameraUtil.setFDRSupportable(true);
        } else {
            CameraUtil.setFDRSupportable(false);
        }
        initializeCapabilities();

        //get compensation capaliblity and set it
        if(mVerticalSeekBar == null){
            mVerticalSeekBar = (AELockSeekBar)mActivity.getModuleLayoutRoot().findViewById(R.id.aeseekbar);
        }
        if(mVerticalSeekBar != null) {
            mVerticalSeekBar.setMax(mCameraCapabilities.getMaxExposureCompensation());
            mVerticalSeekBar.setMin(mCameraCapabilities.getMinExposureCompensation());
            mVerticalSeekBar.setProgress(0);
            mVerticalSeekBar.setOnSeekBarChangeListener(this);
        }

        // Reset zoom value index.
        mZoomValue = 1.0f;
        if (mFocusManager == null) {
            initializeFocusManager();
        }
        mFocusManager.updateCapabilities(mCameraCapabilities);

        // Do camera parameter dependent initialization.
        mCameraSettings = mCameraDevice.getSettings();
        if (mCameraSettings == null) return;
        // Set a default flash mode and focus mode
        if (mCameraSettings.getCurrentFlashMode() == null) {
            mCameraSettings.setFlashMode(CameraCapabilities.FlashMode.NO_FLASH);
        }
        if (mCameraSettings.getCurrentFocusMode() == null) {
            mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.AUTO);
        }

        /**
         * SPRD: Fix bug 572631, optimize camera launch time,
         * Original Code
         *
        setCameraParameters(UPDATE_PARAM_ALL);
         */

        /**
         * SPRD: Fix bug 572631, optimize camera launch time
         * Original Code
         *
        mCameraPreviewParamsReady = true;
         */

        startPreview(true);
        onCameraOpened();

        mHardwareSpec = new HardwareSpecImpl(
                getCameraProvider(), mCameraCapabilities,
                isCameraFrontFacing());

        mCameraAvailable = true;
    }

    protected void updateFlashTopButton() {
        if(CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing())){
            if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FLASH_MODE)){
                mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FLASH_DREAM,View.VISIBLE);
                return;
            }
        }
        Log.i(TAG, "Flash is not supported");
        mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FLASH_DREAM,View.GONE);
    }

    private void updateRefocusTopButton() {
        if (mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_REFOCUS) && (DreamUtil.getRightCamera(mCameraId) == DreamUtil.BACK_CAMERA && CameraUtil.hasBlurRefocusCapture()
                || DreamUtil.getRightCamera(mCameraId) == DreamUtil.FRONT_CAMERA && CameraUtil.hasFrontBlurRefocusCapture())) {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_REFOCUS_DREAM,View.VISIBLE);
        } else {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_REFOCUS_DREAM,View.GONE);
        }
    }


    @Override
    public void onCaptureCancelled() {
        mActivity.setResultEx(Activity.RESULT_CANCELED, new Intent());
        mActivity.finish();
    }

    @Override
    public void onCaptureRetake() {
        Log.i(TAG, "onCaptureRetake");
        if (mPaused) {
            return;
        }
        mUI.hidePostCaptureAlert();
        //Fix bug840006,add for shutter button can be normal.
        onShowImageDone();
        mUI.hideIntentReviewImageView();
        setupPreview();

        /*
         *SPRD: Fix Bug 678792, add contact and take head-img,
         *      click to retake after take a picture with count down module,
         *      top buttons can't be clicked any more.@{
         */
        isTouchCapturing = false;
        /* @} */
    }

    @Override
    public void onCaptureDone() {
        Log.i(TAG, "onCaptureDone");
        /*
         * SPRD: onCaptureDone could be called in monkey test while image capture
         * has not completed, then NullPointerException happens @{
         * orginal code
        if (mPaused) {
            return;
        }
         */

        if (mPaused || mJpegImageData == null) {
            return;
        }
        /* @} */


        byte[] data = mJpegImageData;

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value. If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    /*
                     * SPRD: fix bug 568162 If ContentResolver open URI failed,
                     * try to use new file to save data.@{
                     * original code
                     *
                     outputStream = mContentResolver.openOutputStream(mSaveUri);
                     */
                    final String scheme = mSaveUri.getScheme();
                    if ("content".equals(scheme)) {
                        outputStream = mContentResolver.openOutputStream(mSaveUri);
                    } else if ("file".equals(scheme)) {
                        File picFile = new File(mSaveUri.getPath());
                        File picParentFile = picFile.getParentFile();
                        if (picParentFile.exists() || picParentFile.mkdirs()) {
                            outputStream = new FileOutputStream(picFile);
                        } else {
                            throw new IOException("mkdirs failed: " + mSaveUri);
                        }
                    } else {
                        throw new IOException("unSupported URI: " + mSaveUri);
                    }
                    /* @} */
                    outputStream.write(data);
                    outputStream.close();

                    Log.i(TAG, "saved result to URI: " + mSaveUri);
                    mActivity.setResultEx(Activity.RESULT_OK);
                    mActivity.finish();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    onError();
                } finally {
                    CameraUtil.closeSilently(outputStream);
                }
            } else {
                ExifInterface exif = Exif.getExif(data);
                int orientation = Exif.getOrientation(exif);
                Bitmap bitmap = CameraUtil.makeBitmap(data, 50 * 1024);
                bitmap = CameraUtil.rotate(bitmap, orientation);
                Log.v(TAG, "inlined bitmap into capture intent result");
                mActivity.setResultEx(Activity.RESULT_OK, new Intent(
                        "inline-data").putExtra("data", bitmap));
                mActivity.finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            captureDoneSub(data);
        }
    }

    private void captureDoneSub(byte[] data) {
        //split to 1 methods for CCN optimization , from 15 to 10+6
        Uri tempUri = null;
        FileOutputStream tempStream = null;
        try {
            File path = mActivity.getFileStreamPath(sTempCropFilename);
            path.delete();
            tempStream = mActivity.openFileOutput(sTempCropFilename, 0);
            tempStream.write(data);
            tempStream.close();
            tempUri = Uri.fromFile(path);
            Log.i(TAG, "wrote temp file for cropping to: "
                    + sTempCropFilename);
        } catch (FileNotFoundException ex) {
            Log.w(TAG, "error writing temp cropping file to: "
                    + sTempCropFilename, ex);
            mActivity.setResultEx(Activity.RESULT_CANCELED);
            onError();
            return;
        } catch (IOException ex) {
            Log.w(TAG, "error writing temp cropping file to: "
                    + sTempCropFilename, ex);
            mActivity.setResultEx(Activity.RESULT_CANCELED);
            onError();
            return;
        } finally {
            CameraUtil.closeSilently(tempStream);
        }

        Bundle newExtras = new Bundle();
        if (mCropValue.equals("circle")) {
            newExtras.putString("circleCrop", "true");
        }
        if (mSaveUri != null) {
            Log.i(TAG, "setting output of cropped file to: " + mSaveUri);
            newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
        } else {
            newExtras.putBoolean(CameraUtil.KEY_RETURN_DATA, true);
        }
        if (mActivity.isSecureCamera()) {
            newExtras.putBoolean(CameraUtil.KEY_SHOW_WHEN_LOCKED, true);
        }

        // TODO: Share this constant.
        final String CROP_ACTION = "com.android.camera.action.CROP";
        Intent cropIntent = new Intent(CROP_ACTION);

        cropIntent.setData(tempUri);
        cropIntent.putExtras(newExtras);
        Log.i(TAG, "starting CROP intent for capture");
        mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
        mShutterTouchCoordinate = coord;
    }

    /*
     * SPRD: fix bug 498954 If the function of flash is on and the camera is
     * counting down, the flash should not run here but before the capture.@{
     */
    private boolean isCountDownRunning() {
        if (mActivity != null) {
            int countDownDuration = 0;
            if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_COUNTDOWN_DURATION)){
                countDownDuration = mDataModuleCurrent
                        .getInt(Keys.KEY_COUNTDOWN_DURATION);
            }
            Log.i(TAG, "isCountDownRunning countDownDuration = "
                    + countDownDuration);
            if (countDownDuration > 0) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /* @} */

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // Do nothing. We don't support half-press to focus anymore.
        /* SPRD: fix bug 473602 add for half-press @{ */
        Log.i(TAG, "onShutterButtonFocus pressed = " + pressed);
        if (mPaused || invalidCameraState(false) || shtterButtonCanNotFocus(pressed)) {
            /* @} */
            if (!pressed) {
                mCaptureCount = 0;
            }
            return;
        }
        if (pressed) {
            mFocusManager.onShutterDown(CameraCapabilities.FocusMode.AUTO);
        } else {
            // for countdown mode, we need to postpone the shutter release
            // i.e. lock the focus during countdown.
            if (mCameraButtonClickedFlag) {
                mCameraButtonClickedFlag = false;
            }
            mCaptureCount = 0;
            if (!mUI.isCountingDown() && !isAELock && !isOnSingleTapUp) {//SPRD:fix bug981128/1270933
                mFocusManager.onShutterUp(CameraCapabilities.FocusMode.AUTO);
            }
        }
        /* @} */
    }

    private boolean shtterButtonCanNotFocus(boolean pressed) {
        return (!mCameraButtonClickedFlag && pressed
                || !isShutterEnabled()
                || (!isFlashOn() && !mCameraButtonClickedFlag && !isForceFocus())
                || isFocusModeFixed()
                /*
                 * SPRD: fix bug 498954 If the function of flash is on and the
                 * camera is counting down, the flash should not run here but
                 * before the capture.@{
                 */
                || (!isFlashOff() && isCountDownRunning())
                || (!mIsImageCaptureIntent && mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES)
                || (!mIsImageCaptureIntent && mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES));
    }

    private boolean invalidCameraState(boolean isShutterButtonClick) {
        if (isShutterButtonClick) {
            return ((mCameraState == SWITCHING_CAMERA)
                    || (mCameraState == PREVIEW_STOPPED));
        } else {
            return ((mCameraState == SWITCHING_CAMERA)
                    || (mCameraState == PREVIEW_STOPPED)
                    || (mCameraState == SNAPSHOT_IN_PROGRESS));
        }

    }
    /* SPRD:Fix bug 473602 CAF do not need AF only flash on @{ */
    private boolean isFlashOn() {
        String flashValues = mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE);
        Log.i(TAG, "isFlashOn  flashValues = " + flashValues);
        return "on".equals(flashValues);
    }

    public boolean isShutterEnabled() {
        return mAppController.isShutterEnabled();
    }

    public boolean isTouchCapturing(){
        return isTouchCapturing;
    }

    /* @} */
    @Override
    public boolean isShutterClicked(){
        return !mAppController.isShutterEnabled() || mUI.isCountingDown();
    }

    /* @} */
    /*SPRD:fix bug529235 hdr and flash are both on@{*/
    private boolean isFlashOff() {
        return mCameraSettings == null ? true: mCameraSettings.getCurrentFlashMode() == CameraCapabilities.FlashMode.OFF;//SRPD:fix bug1007184
    }
    /* @} */
    /**
     * SPRD:Fix bug 531648 CAF and flash not off need force Focus
     * @return
     */
    private boolean isForceFocus(){
        String flashValues = mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE);
        String focusMode = mDataModule.getString(Keys.KEY_FOCUS_MODE);
        return !"off".equals(flashValues) && "continuous-picture".equals(focusMode);
    }

    /**
     * Add For Dream Camera in IntervalPhotoModule
     * 
     * @return true means will shutter again; false means won't shutter again.
     * @{
     */
    public boolean shutterAgain() {
        return false;
    }

    public boolean isInShutter() {
        return false;
    }

    /* @} */

    @Override
    public void onShutterButtonClick() {
        onShutterButtonClick(true);
    }

    public void onShutterButtonClick(boolean checkShutterOnBurstWorking) {
        /**
         * SPRD: fix bug 476432 add for burst capture @{ if (mPaused ||
         * (mCameraState == SWITCHING_CAMERA) || (mCameraState ==
         * PREVIEW_STOPPED) || !mAppController.isShutterEnabled()) {
         * mVolumeButtonClickedFlag = false; return; }
         */
        Log.i(TAG, "onShutterButtonClick mPaused:" + mPaused + ",mCameraState:"
                + mCameraState + " isShutterEnabled=" + isShutterEnabled()
                + " mCaptureCount = " + mCaptureCount);
        if(mUI instanceof ManualPhotoUI){//ban capture during changing ae compensation
            if(((ManualPhotoUI) mUI).isManualControlPressed()){
                return;
            }
        }

        if(mAELockPanel != null && mAELockPanel.isSeekbarChanging()){ //ae/af Locked and changing ae compensation so return
            return;
        }
        if (shutterButtonCanNotClick(checkShutterOnBurstWorking)) {
            resetShutterButton();//Sprd:fix bug1002729
            return;
        }
        mCaptureCount++;
        /**
         * SPRD: fix bug 388808 Log.d(TAG, "onShutterButtonClick: mCameraState="
         * + mCameraState + " mVolumeButtonClickedFlag=" +
         * mVolumeButtonClickedFlag); mAppController.setShutterEnabled(false);
         */

        if(isAuto3DnrScene && mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_3DNR_PARAMETER)){
            mCameraSettings.set3DNREnable(1);
        }
        doSomethingWhenonShutterButtonClick();
        /* SPRD: fix bug 476432/389377 add for burst capture @{ */
        if (!mIsImageCaptureIntent) {
            updateParametersBurstCount();
            mContinuousCaptureCount = getContinuousCount();
        }

        updateParametersBurstMode();
        Log.i(TAG, "onShutterButtonClick: mCameraState=" + mCameraState
                + " mContinuousCaptureCount=" + mContinuousCaptureCount);

        mAppController.getCameraAppUI().setSwipeEnabled(false);
        if (getContinuousCount() > 1) {
            mAppController.getCameraAppUI().hideCaptureIndicator();
            mUUID = java.util.UUID.randomUUID().toString();
        }
        /* @} */

        /*
         * SPRD:Fix bug 502060 close the function of Zoom only during the
         * Continuous Capture@{
         * SPRD:fix bug 596882/617069/648477 disable zoom when capture, if zoom in zsl capture, next capture will fail
         */
        mUI.enablePreviewOverlayHint(false);// SPRD:Fix bug 411062
        mUI.hideZoomProcessorIfNeeded();
        /* @} */

        int countDownDuration  = 0;
        if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_COUNTDOWN_DURATION)){
            countDownDuration = mDataModuleCurrent
                    .getInt(Keys.KEY_COUNTDOWN_DURATION);
        }

        if (mActivity.getVoiceIntentCamera() && mDataModuleCurrent.getInt(Keys.KEY_VOICE_CAMERA_DONE,0) != 1) {
            mDataModuleCurrent
                    .set(Keys.KEY_VOICE_CAMERA_DONE,1);
            countDownDuration = mDataModuleCurrent
                    .getInt(Keys.KEY_VOICE_COUNTDOWN_DURATION);
        }
        if (getModuleTpye() == DreamModule.INTERVAL_MODULE) {
            countDownDuration = mDataModuleCurrent
                    .getInt(Keys.KEY_SELF_TIMER_INTERVAL);
        }
        mTimerDuration = countDownDuration;
        if (countDownDuration > 0) {
            initCancelButton();
            // Start count down.
            mAppController.getCameraAppUI().transitionToCancel();
            mUI.startCountdown(countDownDuration);
            return;
        } else {
            focusAndCapture();
        }
    }

    private boolean someConditionsToReturn() {
        // split this method for CCN optimization , Bug 839474
        return (mPaused
                || mActivity.isWaitToChangeMode()
                || invalidCameraState(true)
                || (!shutterAgain() && !isShutterEnabled()) // Add For Dream Camera in IntervalPhotoModule
                || mActivity.isFilmstripCoversPreview()
                || mCaptureCount > 0
                || mUI.isCountingDown()
                || isAudioRecording()
                || mAppController.getCameraAppUI().isModeListOpen());
    }
    protected boolean shutterButtonCanNotClick(boolean checkShutterOnBurstWorking) {
        if(someConditionsToReturn()) { // CCN = 8
            Log.i(TAG, "onShutterButtonClick is return !");
            if (!checkShutterOnBurstWorking) {
                mBurstWorking = false;
                mBurstCaptureType = -1;
            }
            return true;
        }
        /**
         * @}
         */
        // Do not take the picture if there is not enough storage.
        if (!mIsImageCaptureIntent && (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES
                                        || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES)) {
            Log.i(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            return true;
        }
        //Sprd Fix Bug: 665197/672846/676172
        if (checkShutterOnBurstWorking && mBurstWorking) {
            Log.i(TAG,"burst capture is running, can not perform onShutterButtonClick! mBurstCaptureType="
                    + mBurstCaptureType);
            return true;
        }
        if (mUI.isZooming()) {
            Log.i(TAG, "camera is zooming,can not perform onShutterbuttonClick");
            return true;
        }
        //Sprd Fix bug:981918
        if (mActivity.getCameraAppUI().getFreezeScreenFlag()) {
            Log.i(TAG, "preview has been frozen, return");
            return true;
        }
        return false;
    }

    protected void setShutterButtonState() {
        /* SPRD: Bug385148 Disable shuterbutton after click */
        if (!isBurstCapture()) {
            mAppController.setShutterEnabled(false);
        }
        mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(false);
    }

    protected void focusAndCapture() {
        Log.i(TAG, "focusAndCapture");
        if (mSceneMode == CameraCapabilities.SceneMode.HDR) {
            mUI.setSwipingEnabled(false);
        }
        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)) {
            if (!mIsImageCaptureIntent) {
                mSnapshotOnIdle = true;
            }
            /* SPRD: fix bug1141978 can not click any button,test case 3&4 @{*/
            if (mBurstWorking) {
                mBurstWorking = false;
                mContinuousCaptureCount = 0;
            }
            /* @} */
            return;
        }

        /* SPRD:fix bug 1425776 disable extendPanel when take picture @{ */
        mUI.disableUIWhenTakepicture();

        mAppController.getCameraAppUI().incTakePictureCount();
        setShutterButtonState();//SPRD:fix bug1141978 test case 2
        mSnapshotOnIdle = false;
        /* SPRD:fix bug 672665/975672 query flash state before capture @{ */
        if (!mCameraButtonClickedFlag && !isFlashOff() && mCameraDevice != null && !isFocusModeFixed() && !mActivity.getIsButteryLow()) {
            boolean needAutoFocus = mCameraDevice.isNeedAFBeforeCapture();
            if (!needAutoFocus) {
                mFocusManager.focusAndCapture(mCameraSettings.getCurrentFocusMode());
            } else {
                mFocusManager.focusAfterCountDownFinishWhileFlashOn();
            }
            return;
        }
        /* @} */
        /*
         * SPRD: fix bug 498954 If the function of flash is on and the camera is
         * counting down, the flash should not run here but before the
         * capture.@{
         */
        /*
         * SPRD: fix bug 502445 And judgement of whether the phone support Focus
         * function or not.
         */
        if (!isFlashOff() && mTimerDuration > 0 && !isFocusModeFixed()) {
            mFocusManager.focusAfterCountDownFinishWhileFlashOn();
            return;
        }
        /* @} */
        mFocusManager.focusAndCapture(mCameraSettings.getCurrentFocusMode());

    }

    @Override
    public void onRemainingSecondsChanged(int remainingSeconds) {
        /* SPRD:fix bug 474665 @{ */
        if (remainingSeconds == 1 && mAppController.isPlaySoundEnable()) {
            if (mCountdownSoundPlayer != null) {
                mCountdownSoundPlayer.play(R.raw.timer_final_second, 0.6f);
            }
        } else if ((remainingSeconds == 2 || remainingSeconds == 3) && mAppController.isPlaySoundEnable()) {
            if (mCountdownSoundPlayer != null) {
                mCountdownSoundPlayer.play(R.raw.timer_increment, 0.6f);
            }
            /* @} */
            /**
             * SRPD: fix bug 388289 }
             */
        } else if (remainingSeconds == 0) {
            if (!mIsContinousCaptureFinish) {
                if (!isBurstCapture()) {
                    mAppController.setShutterEnabled(false);
                    isTouchCapturing = true;
                }
            }
        }
    }

    /* SPRD:fix bug 550298 @{ */
    private boolean isInSilentMode() {
        AudioManager mAudioManager = (AudioManager)mAppController.getAndroidContext().getSystemService(mAppController.getAndroidContext().AUDIO_SERVICE);
        int ringerMode = mAudioManager.getRingerMode();
        return (AudioManager.RINGER_MODE_SILENT == ringerMode || AudioManager.RINGER_MODE_VIBRATE == ringerMode);
    }
    /* @} */

    @Override
    public void onCountDownFinished() {
        mAppController.getCameraAppUI().transitionToCapture();
        /**
         * SPRD: fix bug 473462
         * mAppController.getCameraAppUI().showModeOptions(); if (mPaused) {
         * return; }
         */
        if (mPaused || mIsContinousCaptureFinish) {
            mAppController.getCameraAppUI().setSwipeEnabled(true);
            mIsContinousCaptureFinish = false;
            return;
        }
        /*SPRD: fix bug 599542 add for touch capture and count down @{*/
        if (isOnSingleTapUp) {
            mFocusManager.onSingleTapUp(mTapUpX, mTapUpY, isSupportTouchEV());
            mTapUpX = 0;
            mTapUpY = 0;
            return;
        }
        /* @} */
        /* SPRD: fix bug 634947. In monkey test, mCameraState maybe wrongly changed to PREVIEW_STOPPED during countdown @{ */
        if (mCameraState == PREVIEW_STOPPED) {
            Log.i(TAG, "State is wrongly change to PREVIEW_STOPPED during countdown.");
            return;
        }
        /* @} */
        focusAndCapture();
    }

    @Override
    public void resume() {
        Log.i(TAG, "resume start!");
        mAppController.getCameraAppUI().resetTakePictureCount();
        mPaused = false;
        mNeedCancelAutoFocus = true;
        mFirstHasStartCapture = false; //bugfix:1165276
        installIntentFilter();
        mAELockPanel = mActivity.getModuleLayoutRoot().findViewById(R.id.ae_lock_panel);
        if(mAELockPanel != null) {
            mAELockPanel.setActive(true);
        }
        mVerticalSeekBar = (AELockSeekBar) mActivity.getModuleLayoutRoot().findViewById(R.id.aeseekbar);
        /**
         * SPRD: Fix bug 572631, optimize camera launch time
         * Original Code
         *
        mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
        mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
         */
        mActivity.getDreamHandler().post(new Runnable(){
            @Override
            public void run() {
                if (mPaused || mActivity.isDestroyed()) {
                    return;
                }
                waitInitDataSettingCounter ();
                mDataModuleCurrent.addListener(mActivity,PhotoModule.this);
                mUI.intializeAIDetection(mDataModuleCurrent);
            }
        });
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
                }
            }
        });
        if (mFocusManager != null) {
            // If camera is not open when resume is called, focus manager will
            // not be initialized yet, in which case it will start listening to
            // preview area size change later in the initialization.
            mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
        }
        mAppController.addPreviewAreaSizeChangedListener(mUI);

        CameraProvider camProvider = mActivity.getCameraProvider();
        if (camProvider == null) {
            // No camera provider, the Activity is destroyed already.
            return;
        }
        requestCameraOpen();

        mJpegPictureCallbackTime = 0;
        mZoomValue = 1.0f;

        mOnResumeTime = SystemClock.uptimeMillis();
        checkDisplayRotation();

        // If first time initialization is not finished, put it in the
        // message queue.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(MSG_FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }

        mHeadingSensor.activate();

        /* SPRD:fix bug 473462 add for burst capture @{ */
        getServices().getMediaSaver().setListener(this);// SPRD BUG:388273

        mUI.onResume();

        //add for hal return buffer fail
        mActivity.setSurfaceVisibility(false);
        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        if (isUseSurfaceView()) {
            mActivity.setSurfaceHolderListener(mUI);
            mActivity.setSurfaceVisibility(true);
        }
        /* @} */

        /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
        if(null != mActivity.getCameraAppUI())
            mActivity.getCameraAppUI().refreshTextureViewCover();
        /* @} */
    }

    /**
     * @return Whether the currently active camera is front-facing.
     */
    protected boolean isCameraFrontFacing() {
        return mAppController.getCameraProvider().getCharacteristics(mCameraId)
                .isFacingFront();
    }

    /**
     * The focus manager is the first UI related element to get initialized, and
     * it requires the RenderOverlay, so initialize it here
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
            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes = new ArrayList<CameraCapabilities.FocusMode>();
            CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                    .getStringifier();
            for (String modeString : defaultFocusModesStrings) {
                CameraCapabilities.FocusMode mode = stringifier
                        .focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }

            boolean autoChasingOpen = mDataModuleCurrent
                    .getBoolean(Keys.KEY_AUTO_TRACKING);
            mFocusManager = new FocusOverlayManager(mAppController,
                    defaultFocusModes, mCameraCapabilities, this, mMirror,
                    mActivity.getMainLooper(), mUI.getFocusRing(),mUI.getChasingRing(),
                    autoChasingOpen && mAutoChasingSupported,this);
            mMotionManager = getServices().getMotionManager();
            if (mMotionManager != null) {
                mMotionManager.addListener(mFocusManager);
            }
        }
        mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
    }

    /**
     * @return Whether we are resuming from within the lockscreen.
     */
    private boolean isResumeFromLockscreen() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action) || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                .equals(action));
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

    @Override
    public void pause() {
        Log.i(TAG, "pause");
        isFirstShowToast = true;
        isBatteryAgainLowLevel = false;
        mPaused = true;
        /* SPRD: bug699823, Reset settingchange to avoid settings that can not be apply to request @{ */
        settingchange = false;
        /* @} */
        isAELockPanding = false;
        mSmileCapture = false;

        procForAELockInPause(); // CCN = 3
        resumeTextureView();
        if(CameraUtil.isLevelEnabled()) {
            CameraActivity.THREAD_POOL_EXECUTOR.execute(new Runnable(){
                @Override
                public void run() {
                    if (mSensorManager != null) {
                        mSensorManager.unregisterListener(mAccelerometerListener);
                        mSensorManager.unregisterListener(mMagneticListener);
                    }
                    mOrientationEventListener.disable();
                }
            });
        }
        mUI.resetZoomSimple();

        mHeadingSensor.deactivate();
        mActivity.getCameraAppUI().dismissDialog(Keys.KEY_CAMERA_STORAGE_PATH);
        mAppController.getCameraAppUI().setThumbButtonUnClickable(false);
        mAppController.getCameraAppUI().updateAiSceneView(View.INVISIBLE , 0);
        // Reset the focus first. Camera CTS does not guarantee that
        // cancelAutoFocus is allowed after preview stops.
        /*
         * SPRD: Fix bug 681215 optimization about that DV change to DC is slow
         * Original Code
         *
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            mCameraDevice.cancelAutoFocus();
        }
         */

        if (mCameraDevice != null) {
            mCameraDevice.cancelAutoFocus();
        }
        /* @} */

        //Sprd:fix bug1138042
        if(mAiSceneSupported && mCameraDevice != null) {
            mCameraDevice.setAiSceneCallback(null, null);
            mCameraDevice.setAiSceneWork(false);
        }

        //Sprd:fix bug937739
        if(isAutoHdrSupported() && mCameraDevice != null) {
            mCameraDevice.setHdrDetectionCallback(null, null);
            mCameraDevice.setHdrDetectionWork(false);
        }

        if(mDataModuleCurrent != null && mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_3DNR_PARAMETER) && mCameraDevice != null) {
            mCameraDevice.setAuto3DnrSceneDetectionCallback(null, null);
            mCameraDevice.setAuto3DnrSceneDetectionWork(false);
        }

        removeAutoFocusMoveCallback();
        restoreToDefaultSettings();
        // If the camera has not been opened asynchronously yet,
        // and startPreview hasn't been called, then this is a no-op.
        // (e.g. onResume -> onPause -> onResume).
        stopPreview();
        cancelCountDown();
        dosomethingWhenPause();
        recoverToPreviewUI();
        /**
         * SPRD: Fix bug 572631, optimize camera launch time
         * Original Code
         *
        mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
         */

        /* SPRD:Fix bug 499558 @{ */
        if (!mActivity.getCameraAppUI().isBottomBarNull() &&
                !mActivity.getCameraAppUI().isInIntentReview()) {
            if (!mFirstHasStartCapture) {
                mNamedImages = null;
            }
            // If we are in an image capture intent and has taken
            // a picture, we just clear it in onPause.
            mJpegImageData = null;
        }
        /* @} */

        // Remove the messages and runnables in the queue.
        if (mFirstHasStartCapture) {
            mNeedRemoveMessgeDelay = true;
        } else {
            mHandler.removeCallbacksAndMessages(null);
        }

        if (mMotionManager != null) {
            mMotionManager.removeListener(mFocusManager);
            mMotionManager = null;
        }

        closeCamera();
        mActivity.enableKeepScreenOn(false);
        mUI.onPause();

        if (mReceiver != null) {
            //mActivity.unregisterReceiver(mReceiver);
            mActivity.unRegisterMediaBroadcastReceiver();
            mReceiver = null;
        }

        mPendingSwitchCameraId = -1;
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
            mFocusManager.clearAfArea();//SPRD:fix bug948018/931965
        }
        getServices().getMemoryManager().removeListener(this);
        mAppController.removePreviewAreaSizeChangedListener(mFocusManager);
        mAppController.removePreviewAreaSizeChangedListener(mUI);
        if (mDataModuleCurrent != null) {
            mDataModuleCurrent.removeListener(mActivity,this);
        }
        if(mDataModule != null){
            mDataModule.removeListener(mActivity,this);
        }
        //DataModuleManager.getInstance(mActivity).removeListener(this);

        if (mAppController != null && mAppController.getOrientationManager() != null)
            mAppController.getOrientationManager().removeOnOrientationChangeListener(this);

        mAppController.getCameraAppUI().resetTakePictureCount();
        mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
        /* SPRD: 682407 don't accept touch events when camerastate is idle @{ */
        mUI.enablePreviewOverlayHint(true);
        /* @} */
        /* SPRD: Fix bug 613015 add SurfaceView support @{ */
        if (isUseSurfaceView()) {
            mActivity.setSurfaceHolderListener(null);
        }
        /* @} */
        // CameraAppUI.resumeTextureViewRendering triggers onSurfaceTextureUpdate(),
        // and onSurfaceTextureUpdate() calls hideImageCover, we don't want that happen
        // so do some preparatory work here
        mAppController.getCameraAppUI().prepareForNextRoundOfSurfaceUpdate();
        Log.i(TAG, "pause end!");
    }

    private void resumeTextureView() {
        if (isUseSurfaceView() && (mActivity.getCameraAppUI() != null)) {
            /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
            mActivity.getCameraAppUI().refreshTextureViewCover();
            /* @} */
            mActivity.getCameraAppUI().resumeTextureViewRendering();
        }
    }

    private void recoverToPreviewUI() {
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, true);
    }

    @Override
    public void destroy() {
        /*
         * SPRD: Fix bug 572631, optimize camera launch time
         * Original Android code:
        mCountdownSoundPlayer.release();
         */
        AsyncTask.execute(new Runnable() {// use SerialExecutor to sync
            @Override
            public void run() {
                Log.i(TAG, "Sound release start");
                if (mCountdownSoundPlayer != null) {
                    mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
                    mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
                    mCountdownSoundPlayer.release();
                    mCountdownSoundPlayer = null;
                }

                if (mCameraSound != null) {
                    mCameraSound.release();
                    mCameraSound = null;
                }
            }
        });

        if (mActivity.getCameraAppUI().isInIntentReview()) {
            mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
        }

        if (mUI != null) {
            mUI.hideIntentReviewImageView();
            mUI.setCountdownFinishedListener(null);
            mUI.onDestroy();
        }

        /* @} */
        Log.i(TAG, "destroy");
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();

        if (isBeautyCanBeUsed()) {
            mUI.setButtonOrientation(CameraUtil.getDisplayRotation());// SPRD:fix
                                                                      // bug474672
                                                                      // add
                                                                      // for
                                                                      // ucam
        }
    }

    @Override
    public void updateCameraOrientation() {
        if (mDisplayRotation != CameraUtil.getDisplayRotation()) {
            setDisplayOrientation();
        }
    }

    private boolean canTakePicture() {
        Log.i(TAG, "canTakePicture");
        return isCameraIdle()
                && (mActivity.getStorageSpaceBytes() > Storage.LOW_STORAGE_THRESHOLD_BYTES
                    || mActivity.getInternalStorageSpaceBytes() > Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES);
    }

    @Override
    public void autoFocus() {
        if (mCameraDevice == null) {
            return;
        }
        Log.i(TAG, "Starting auto focus");
        mNeedCancelAutoFocus = true;
        mFocusStartTime = System.currentTimeMillis();
        mCameraDevice.autoFocus(mHandler, mAutoFocusCallback);
        setCameraState(FOCUSING);
        Log.i(TAG, "autoFocus end!");
    }

    @Override
    public void cancelAutoFocus() {
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.cancelAutoFocus();
        setCameraState(IDLE);
        if (mFace != null && mFace
                && isCameraIdle() /* && !isHdr() SPRD:Fix bug 459572*/) {
            // now face is not mutex with ai in UE's doc.
            startFaceDetection();
        }

        setCameraParameters(UPDATE_PARAM_PREFERENCE);
    }

    @Override
    public void setAutoChasingParameters(){
        if (mCameraState != PREVIEW_STOPPED) {
            setCameraParameters(UPDATE_PARAM_PREFERENCE);
        }
    }

    protected boolean resetExtendPanel(){
        if(mAppController != null && mAppController.getCurrentModuleIndex() == FILTER) {
            if (mActivity.getCameraAppUI().getExtendPanelParent() != null && mActivity.getCameraAppUI().getExtendPanelParent().getVisibility() == View.VISIBLE) {
                mActivity.getCameraAppUI().updateFilterPanelUI(View.GONE);
                mActivity.getCameraAppUI().getExtendPanelParent().setVisibility(View.INVISIBLE);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {

        if  (resetExtendPanel()){
            return;
        }

        if (doNothingWhenOnSingleTapUp(x, y)) {
            return;
        }
        // Check if metering area or focus area is supported.
        /*SPRD:fix bug533976 add touch AE for FF@{
         * android original code
        if (!mFocusAreaSupported && !mMeteringAreaSupported) {
            return;
        }
        */

        /*SPRD:fix bug1105014 send touch areas and Orientation when face attributes on @{*/
        if (mFaceAttributeSupported && mDataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES)){
            int deviceOrientation = mActivity.getOrientationManager().getDeviceOrientation().getDegrees();
            List<android.hardware.Camera.Area> mTouchAreas = mFocusManager.initializeTouchRegions(x,y);
            if(mCameraDevice != null && mCameraSettings != null) {
                mCameraSettings.setDeviceOrientation(deviceOrientation);
                Log.i(TAG , "send DeviceOrientation for face attributes" + deviceOrientation + " to HAL");
                mCameraSettings.setTouchAreas(mTouchAreas);
                Log.i(TAG,"send touch areas for face attributes " + mTouchAreas + "to HAL");
            }
        }
        /* @} */
        /* SPRD:fix bug 600946 add touch capture for FF and front camera, Dream Camera test ui check 20 @{*/
        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)
                && mCameraSettings != null && (mFocusManager.getFocusMode(mCameraSettings.getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE)) == CameraCapabilities.FocusMode.FIXED
                    || (mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_FOCUS_DISTANCE) && isFocusModeFixed()))) {
            touchandCapture();
            return;
        }
        /* @} */
        if (mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_FOCUS_DISTANCE) && mCameraSettings != null && mCameraSettings.getFocusDistance() <= mCameraCapabilities.getFocusDistanceScale()) {
            //auto focus mode off
            mFocusManager.onSingleTapUpInManual(x, y);
            return;
        }
        if (!mMeteringAreaSupported) {
            return;
        }
        /*@}*/
        /*SPRD: fix bug 599542 add for touch capture and count down @{*/
        if (isCountDownRunning() && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)) {
            isOnSingleTapUp = true;
            mTapUpX = x;
            mTapUpY = y;
            touchandCapture();
            return;
        }
        /* @} */

        if((isAELock || isSupportTouchEV()) && mAELockPanel != null) { // Bug 1138796 , touch
            mAELockPanel.hide();
        }

        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)) {
            isOnSingleTapUp = true;
            mUI.enablePreviewOverlayHint(false);// SPRD:Fix bug 653186
            mUI.hideZoomProcessorIfNeeded();
        }

        mUI.onSingleTapUp();//SPRD:fix bug786565
        mFocusManager.onSingleTapUp(x, y, isSupportTouchEV());
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
                updateFlashTopButtonEnable();
            }
        }
    }

    /* SPRD:fix bug1134550 should not unlock ae when audio picture capture @{ */
    protected void setAELockPanel(boolean show) {
        if (show) {
            if(mAELockPanel != null && isAELock && mVerticalSeekBar != null) {
                mAELockPanel.show();
                mAELockPanel.showtipText(true);
                mAELockPanel.setSeekBarVisibility(true);
                mVerticalSeekBar.setOnSeekBarChangeListener(this);
            }
        } else {
            if(mAELockPanel != null && isAELock && mVerticalSeekBar != null) {
                mAELockPanel.hide();
                mVerticalSeekBar.setOnSeekBarChangeListener(null);
            }
        }
    }
    /* @} */

    @Override
    public boolean isManualMode() {
        return false;
    }

    @Override
    public boolean isTouchCapture() {
        return isOnSingleTapUp;
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
    public void onLongPress(MotionEvent var1){
        if (doNothingWhenOnLongPress(var1) || !isEnableDetector()) {//SPRD:fix bug1003932
            return;
        }
        if (isCameraFrontFacing() || !mAeLockSupported || !mFocusAreaSupported) {//SPRD:fix bug1093813
            return;
        }
        if(mDataModuleCurrent != null && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR) && mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) != 0) {
            mDataModuleCurrent.changeSettings(Keys.KEY_CAMERA_HDR,0);
            if (!CameraUtil.isHdrZslEnable()) {
                Log.d(TAG,"HDR mutex with zsl, Now first close HDR and open ZSL, then set ae/af Lock after previewed");
                isAELockPanding = true;
                x = (int)var1.getX();
                y = (int)var1.getY();
                return;
            }
        }
        mFocusManager.onLongPress((int)var1.getX(),(int)var1.getY());
    }
    @Override
    public void onProgressChanged(SeekBar SeekBarId, int progress, boolean fromUser) {
        Log.d(TAG,"onProgressChanged " + progress);
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
        }
    }

    private boolean doNothingWhenOnSingleTapUp(int x, int y) {
        if (mPaused || mCameraDevice == null || !mFirstTimeInitialized
                || invalidCameraState(false)) {
            Log.i(TAG, "onSingleTapUp return!");
            return true;
        }

        if(null != mActivity && (null != mActivity.getCameraAppUI())
                && null != mActivity.getCameraAppUI().getPreviewArea() && !mActivity.getCameraAppUI().getPreviewArea().contains(x, y)){
            return true;
        }
        // Sprd: Fix bug704075
        if (null != mActivity
                && (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES
                    || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES)) {
            Log.i(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            return true;
        }
        return false;
    }
    private boolean doNothingWhenOnLongPress(MotionEvent var){
        x = (int)var.getX();
        y = (int)var.getY();
        if(null != mActivity && (null != mActivity.getCameraAppUI())
                && null != mActivity.getCameraAppUI().getPreviewArea()
                && (!mActivity.getCameraAppUI().getPreviewArea().contains(x, y) || y >= (CameraUtil.getDefaultDisplaySize().getHeight()-mActivity.getCameraAppUI().mBottomFrame.getMeasuredHeight()))){
            return true;
        }
        return false;
    }

    private void touchandCapture(){
        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)) {
            onShutterButtonClick();
            setCaptureCount(0);
        }
    }

    @Override
    public void touchCapture(){
        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH) && isOnSingleTapUp) {
            isOnSingleTapUp = false;
            isTouchCapturing = true;
            mAppController.setShutterEnabled(false);
            capture();
        }
    }

    @Override
    public boolean onBackPressed() {
        return mUI.onBackPressed();
    }

    private boolean isFreezing() {
        // split this method for CCN optimization , Bug 839474
        return (mBurstNotShowShutterButton);
    }

    private void procOnKeyCamera(int keyCode , KeyEvent event) {
        // split this method for CCN optimization , Bug 839474
        // SPRD: Freeze display don't action the key_camera event
        if (isFreezing()) // CCN = 3
            return;
        if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
            if (!mBurstWorking) {
                handleActionDown(keyCode);
            }
        }
    }

    private void procVolumeShutter(int keyCode , KeyEvent event) {
        // split this method for CCN optimization , Bug 839474
        if(isFreezing()) // CCN = 3
            return;
        if (event.getRepeatCount() == 0) {
                        /* SPRD:fix bug 473602 add for half-press @{ */
            if (keyCode == KeyEvent.KEYCODE_FOCUS) {
                mCameraButtonClickedFlag = true;
            }
                        /* @} */
            if (keyCode == KeyEvent.KEYCODE_FOCUS) {
                if (!(mUI.isCountingDown() && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH))) {
                    onShutterButtonFocus(true);
                }
            } else {
                if (!mBurstWorking) {
                    handleActionDown(keyCode);
                }
            }
        }
    }

    private void procVolumeZoom(int keyCode , KeyEvent event) {
        // split this method for CCN optimization , Bug 839474
        if(mDataModuleCurrent != null &&
                mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE) &&
                !mDataModuleCurrent.getBoolean(Keys.KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE)){
            CameraUtil.toastHint(mActivity,R.string.current_module_does_not_support_zoom_change);
            return;
        }
        if ((mBurstCaptureType >= 0) /* SPRD: fix bug 668588, can not set zoom in burstCaptureType*/
                || mBurstWorking || !mAppController.isShutterEnabled() || mUI.isCountingDown() || !mUI.isEnableDetector()) {//SPRD:fix bug655910
            return;
        }
        float zoomValue;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            zoomValue=increaseZoomValue(mZoomValue);
        } else {
            zoomValue=reduceZoomValue(mZoomValue);
        }
        onZoomChanged(zoomValue);
        mUI.setPreviewOverlayZoom(mZoomValue);
    }

    private void procOnKeyDpadCenter(int keyCode, KeyEvent event) {
        // split this method for CCN optimization , Bug 839474
        // If we get a dpad center event without any focused view, move
        // the focus to the shutter button and press it.
        if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
            // Start auto-focus immediately to reduce shutter lag. After
            // the shutter button gets the focus, onShutterButtonFocus()
            // will be called again but it is fine.
            onShutterButtonFocus(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mDataModuleCurrent == null || mActivity.getCameraAppUI().isBottomBarNull())
            return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_ENTER://SPRD:fix bug1148714
                if (/* TODO: mActivity.isInCameraApp() && */mFirstTimeInitialized &&
                        !mActivity.getCameraAppUI().isInIntentReview()) {
                /* SPRD:Bug 535058 New feature: volume */
                    int volumeStatus = getVolumeControlStatus(mActivity);
                    if (volumeStatus == Keys.shutter || keyCode == KeyEvent.KEYCODE_FOCUS) {
                        if (mActivity.hasKeyEventEnter() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                            return true;//SPRD:fix bug1141978 test case1
                        }
                        procVolumeShutter(keyCode, event);
                        return true;
                    } else if (volumeStatus == Keys.zoom) {
                        procVolumeZoom(keyCode, event);
                        return true;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_CAMERA:
                procOnKeyCamera(keyCode, event); // CCN = 5
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                procOnKeyDpadCenter(keyCode, event); // CCN = 3
                return true;
        }
        return false;
    }

    private boolean isTouchAndVolumeCaptureTogether(){
        return isOnSingleTapUp || mCameraState == SNAPSHOT_IN_PROGRESS && !isBurstCapture();
    }

    private void procOnKeyUpVolumeShutter(int keyCode , KeyEvent event) {
        if(getModuleTpye() == DreamModule.INTERVAL_MODULE && !canShutter()){
            return;
        }
        // SPRD: Freeze display don't action the key_up event
        if (isFreezing()) {
            return;
        }
        if (mUI.isCountingDown()) {
            cancelCountDown();
            mBurstCaptureType = -1;
        } else {
            if (mBurstCaptureType == keyCode){
                handleActionUp();
            } else if (!mIsImageCaptureIntent) {
                if (mBurstWorking) {
                    Log.e(TAG, "burst capture is triggered by other key, can not cancel burst capture!");
                    return;
                }
            }
            // SPRD:fix bug673430 volume and touchcapture same time, capture failed
            if (isTouchAndVolumeCaptureTogether()) {
                return;
            }
            //SPRD:fix bug671372 not show focus ring when audio recording
            if (!mBurstWorking && !isAudioRecording() && mBurstCaptureType != 0){//SPRD:fix bug672848
                onShutterButtonFocus(true);
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    mCaptureCount = 0;//SPRD:fix bug1141978
                    if (mHandler.hasMessages(MSG_RESET_KEY_ENTER)) {
                        mHandler.removeMessages(MSG_RESET_KEY_ENTER);
                    }
                }
                onShutterButtonClick();
                /* SPRD:fix bug 496864 Can not capture twice using key of volumn @{ */
                onShutterButtonFocus(false);//Sprd:fix bug929786
                /* }@ */
            } else if (isAudioRecording()) {
                onStopRecordVoiceClicked();
            }
        }
    }

    private void procOnKeyUpFocus(int keyCode , KeyEvent event) {
        if(getModuleTpye() == DreamModule.INTERVAL_MODULE && !canShutter()){
            return;
        }

        if (mFirstTimeInitialized) {
            onShutterButtonFocus(false);
        }
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mDataModuleCurrent == null || mActivity.getCameraAppUI().isBottomBarNull())
            return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_ENTER:
                int volumeStatus = getVolumeControlStatus(mActivity);
            /*
             * SPRD:fix bug518054 ModeListView is appear when begin to capture using volume
             * key@{
             */
                mActivity.getCameraAppUI().hideModeList();
            /* }@ */
                if (/* mActivity.isInCameraApp() && */mFirstTimeInitialized &&
                        !mActivity.getCameraAppUI().isInIntentReview()) {
                /* SPRD:Bug 535058 New feature: volume */
                    if (volumeStatus == Keys.shutter || keyCode == KeyEvent.KEYCODE_CAMERA) {
                        if (mActivity.hasKeyEventEnter() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                            if (mHandler.hasMessages(MSG_RESET_KEY_ENTER)) {
                                mHandler.removeMessages(MSG_RESET_KEY_ENTER);
                            }
                            mActivity.resetHasKeyEventEnter();
                            return true;
                        }
                        procOnKeyUpVolumeShutter(keyCode,event);
                        return true;
                    } else if (volumeStatus == Keys.zoom) {
                        mUI.hideZoomUI();
                        return true;
                    } else if (volumeStatus == Keys.volume) {
                        return false;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_FOCUS:
                procOnKeyUpFocus(keyCode,event);
                return true;
        }
        return false;
    }

    protected void closeCamera() {
        Log.i(TAG, "closeCamera will! mCameraDevice=" + mCameraDevice);
        mCameraAvailable = false;

        // SPRD: fix bug 706642, when mCameraAvailable == false , KeyUp cannot be dispatched.
        // onKeyUp method cannot be executed. So zoom processor cannot be hided when switch camera
        mUI.hideZoomProcessorIfNeeded();

        /**
         * SPRD: fix bug 434570 if (mCameraDevice != null) {
         * stopFaceDetection(); mCameraDevice.setZoomChangeListener(null);
         * mCameraDevice.setFaceDetectionCallback(null, null);
         * mFaceDetectionStarted = false;
         * mActivity.getCameraProvider().releaseCamera
         * (mCameraDevice.getCameraId()); mCameraDevice = null;
         * setCameraState(PREVIEW_STOPPED); mFocusManager.onCameraReleased(); }
         */

        /*
         * SPRD: fix bug 496029 If turn off the screen during the Continuous
         * Capture, the mContinuousCaptureCount should set to be 0. @{
         */
        mContinuousCaptureCount = 0;
        /* @} */

        if (mCameraDevice == null) {
            Log.i(TAG, "already stopped.");
            Log.i(TAG, "closeCamera end!");
            return;
        }
        stopFaceDetection();
        doCloseCameraSpecial(mActivity, mCameraDevice);
        mCameraDevice.setZoomChangeListener(null);

        // already call by stopFaceDetection
        // mCameraDevice.setFaceDetectionCallback(null, null);
        // mFaceDetectionStarted = false;
        mActivity.getCameraProvider().releaseCamera(mCameraDevice.getCameraId());
        mCameraDevice = null;
        mPreviewing = false;
        setCameraState(PREVIEW_STOPPED);

        if (mFocusManager != null)
            mFocusManager.onCameraReleased();

        Log.i(TAG, "closeCamera end!");
    }

    protected void setDisplayOrientation() {
        mDisplayRotation = CameraUtil.getDisplayRotation();
        if (mCameraId != -1) {
            Characteristics info = mActivity.getCameraProvider()
                    .getCharacteristics(mCameraId);
            if (info != null) {
                mDisplayOrientation = info.getPreviewOrientation(mDisplayRotation);
            }
        }
        if (mUI != null) {
            mUI.setDisplayOrientation(mDisplayOrientation);
        }
        if (mFocusManager != null) {
            mFocusManager.setDisplayOrientation(mDisplayOrientation);
        }
        // Change the camera display orientation
        if (mCameraDevice != null) {
            mCameraDevice.setDisplayOrientation(mDisplayRotation);
        }
        Log.v(TAG, "setDisplayOrientation (screen:preview) " + mDisplayRotation
                + ":" + mDisplayOrientation);
    }

    /** Only called by UI thread. */
    private void setupPreview() {
        Log.i(TAG, "setupPreview");
        if (!mNeedCancelAutoFocus || mNeedCancelAutoFocus && mFocusManager.getFocusAreas() == null) {//SPRD:fix bug1188012
            mFocusManager.resetTouchFocus();
        }
        /*this is for GTS test testAssistantTakePhotoWithVoiceInteraction*/
        if (mDataModuleCurrent != null && mActivity.getVoiceIntentCamera())
        mDataModuleCurrent
                .set(Keys.KEY_VOICE_CAMERA_DONE,0);
        startPreview(true);
    }

    /**
     * Returns whether we can/should start the preview or not.
     */
    protected boolean checkPreviewPreconditions() {
        if (mPaused) {
            return false;
        }

        if (mCameraDevice == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        }

        /*
         * SPRD: Fix bug 613015 add SurfaceView support @{
         * Original Code
         *
        SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
        if (st == null) {
            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
            return false;
        }
         */
        if (isUseSurfaceView()) {
            SurfaceHolder sh = mActivity.getCameraAppUI().getSurfaceHolder();
            if (sh == null) {
                Log.w(TAG, "startPreview: SurfaceHolder is not ready.");
                return false;
            }
            /*
             * SPRD: Fix bug 666033
             * 
            SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
            if (st == null) {
                Log.w(TAG, "startPreview: surfaceTexture is not ready. need wait for blur.");
                return false;
            }
            */
        } else {
            SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
            if (st == null) {
                Log.w(TAG, "startPreview: surfaceTexture is not ready.");
                return false;
            }
        }
        /* @} */

        /**
         * SPRD: Fix bug 572631, optimize camera launch time,
         * Original Code
         *
        if (!mCameraPreviewParamsReady) {
            Log.w(TAG, "startPreview: parameters for preview is not ready.");
            return false;
        }
         */
        return true;
    }

    boolean settingchange = false;
    protected void updateSettingAfterOpencamera(boolean optimize){
        // Nexus 4 must have picture size set to > 640x480 before other
        // parameters are set in setCameraParameters, b/18227551. This call to
        // updateParametersPictureSize should occur before setCameraParameters
        // to address the issue.

        if (mCameraSettings == null) return;
        if (!mSnapshotOnIdle && mNeedCancelAutoFocus) {
            // If the focus mode is continuous autofocus, call cancelAutoFocus
            // to resume it because it may have been paused by autoFocus call.
            if (mFocusManager.getFocusMode(mCameraSettings
                    .getCurrentFocusMode(),mDataModule.getString(Keys.KEY_FOCUS_MODE)) == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                mCameraDevice.cancelAutoFocus();
                mNeedCancelAutoFocus = false;
            }
            mFocusManager.setAeAwbLock(false); // Unlock AE and AWB.
        }
        updateSettingsBeforeStartPreview();
        if (mCameraState == PREVIEW_STOPPED) {
            applySettings();
            // update settings after startpreview to optimize launch time
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    if (mPaused || mActivity.isDestroyed()) {
                        return;
                    }
                    Log.e(TAG,"updateLeftSettings after startpreview");
                    updateLeftSettings();
                }
            });
        } else {
            // update settings before startpreview when take picture to avoid preview jump
            updateLeftSettings();
        }

        if (CameraUtil.isSensorSelfShotEnable()
                || CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_3
                || isBackRealDualCamVersion()
                || CameraUtil.getCurrentBlurCoveredId() != CameraUtil.BLUR_COVERED_ID_OFF) {
            mCameraDevice.setSensorSelfShotCallback(mHandler, this);
        }
    }
    private void updateSettingsBeforeStartPreview() {
        //Sprd：Fix bug739085
        mActivity.waitUpdateSettingsCounter();
        Log.v(TAG, "updateSettingsBeforeStartPreview begin");
        updateParametersPictureSize();
        updateCameraParametersInitialize();
        mCameraSettings.setDefault(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD == CameraUtil.getFrontFlashMode(), mCameraCapabilities);//SPRD:fix bug616836 add for photo use api1 or api2 use reconnect
        mCameraSettings.setEnterVideoMode(false);
        if (!mIsImageCaptureIntent) {
            updateParametersFlashMode();
            updateParametersZsl();
            updateParametersBurstMode();
            updateParametersMirror();
            updateParametersEOIS();
            updateParameters3DNR();
            updateParametersColorEffect();
            updateParametersThumbCallBack();
            updateParametersWhiteBalance();//SPRD:fix bug909826
            updateParametersAiSceneDetect();
            //Sprd:fix bug1005605
            updateParametersContrast();
            updateParametersSaturation();
            updateAuto3DnrPatameter();
            updateParametersAutoChasingRegion();
            updateParametersLogoWatermark();
            updateParametersTimeWatermark();
            updateLevel();
            //Bug1028950 set ae compensation before start preview
            if(isAELock){
                setExposureCompensation(AECompensation);
            } else {
                updateParametersExposureCompensation();
            }

            updateParametersISO(); //SPRD: Fix bug 1203408, update ISO too late.
            updateParametersFaceAttribute();//SPRD: Fix bug 1105014 add face attribute enable tag

            updateLPortraitRefocus();
            sendDeviceOrientation();//SPRD:fix bug1154830

        }
        updateParametersAppModeId();
        Log.v(TAG, "updateSettingsBeforeStartPreview end");
    }
    private void updateLeftSettings() {
        Log.v(TAG, "updateLeftSettings begin");
        /*SPRD: fix bug606414 burst mode not need to set display orientation, because it will modify jpeg orientation @ */
        if (!mBurstWorking) {
            setDisplayOrientation();
        }
        updateCameraParametersZoom();
        setAutoExposureLockIfSupported();
        setAutoWhiteBalanceLockIfSupported();
        setFocusAreasIfSupported();
        setMeteringAreasIfSupported();

        mFocusManager.overrideFocusMode(null);
        mCameraSettings.setFocusMode(mFocusManager.getFocusMode(mCameraSettings
                .getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE)));

        if (mIsImageCaptureIntent) {
            updateParametersFlashMode();
            mDataModuleCurrent.set(Keys.KEY_CAMERA_HDR, false);
        } else {
            updateParametersAntibanding();

            updateParametersPictureQuality();

            if(isAELock){
                setExposureCompensation(AECompensation);
                if (mAeLockSupported) {
                    mCameraSettings.setAutoExposureLock(true);
                }
            }

            updateParametersSceneMode();

            //updateParametersWhiteBalance();//SPRD:fix bug909826

            updateParametersBurstCount();

            updateParametersFlashMode();

            updateParametersMetering();

            updateParametersHDR();
            updateParametersNormalHDR();

            updateParametersSensorSelfShot();

            updateParametersExposureTime();
            updateParametersFocusDistance();
            updateParametersAutoChasingRegion();
            updateParametersSmileCapture();
        }

        updateCameraShutterSound();

        updateParametersGridLine();

        if (mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            updateAutoFocusMoveCallback();
        }
        updateMakeLevel();
        updateBeautyButton();
        updateMakeUpDisplay();
        updateLightPortraitDisplay();
        updateLightPortrait();
        updatePortraitRefocusDisplay();

        updateParametersFlashLevel();
        applySettings();
        Log.v(TAG, "updateLeftSettings end");
    }

    /**
     * The start/stop preview should only run on the UI thread.
     */
    protected void startPreview(boolean optimize) {
        if (mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            // do nothing
            return;
        }

        if (!checkPreviewPreconditions()) {
            updateSettingAfterOpencamera(true);
            settingchange = true;
            return;
        }

        // ui
        /* SPRD: add for bug 380597: switch camera preview has a frame error @{ */
        mActivity.getCameraAppUI().resetPreview();
        /* @} */

        if(settingchange){
            settingchange = false;
        } else {
            updateSettingAfterOpencamera(optimize);
        }

        dosetPreviewDisplayspecial();

        if (isUseSurfaceView()) {
            if (optimize) {
                mCameraDevice.setPreviewDisplay(mActivity.getCameraAppUI().getSurfaceHolder());
            } else {
                mCameraDevice.setPreviewDisplayWithoutOptimize(mActivity.getCameraAppUI().getSurfaceHolder());
            }
        } else {
            if (optimize) {
                mCameraDevice.setPreviewTexture(mActivity.getCameraAppUI().getSurfaceTexture());
            } else {
                mCameraDevice.setPreviewTextureWithoutOptimize(mActivity.getCameraAppUI()
                        .getSurfaceTexture());
            }
        }

        if (CameraUtil.isSensorSelfShotEnable()
                || CameraUtil.getCurrentBackBlurRefocusVersion() == CameraUtil.BLUR_REFOCUS_VERSION_3
                || isBackRealDualCamVersion()
                || CameraUtil.getCurrentBlurCoveredId() != CameraUtil.BLUR_COVERED_ID_OFF) {
            mCameraDevice.setSensorSelfShotCallback(mHandler, this);
        }

        // SPRD: Fix bug 677589 black preview occurs when we switch to video module from photo
        // no need to handle surface update in process of the second onPreviewStarted()
        final boolean handleSurfaceUpdate = !mPreviewing;

        // If we're using API2 in portability layers, don't use
        // startPreviewWithCallback()
        // b/17576554
        CameraAgent.CameraStartPreviewCallback startPreviewCallback = new CameraAgent.CameraStartPreviewCallback() {
            @Override
            public void onPreviewStarted() {
                mHandler.post(new Runnable() {
                    public void run() {
                        mFocusManager.onPreviewStarted();
                        PhotoModule.this.onPreviewStarted();
                    }
                });
                if (mSnapshotOnIdle) {
                    mHandler.post(mDoSnapRunnable);
                }
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
                final Runnable takePictureRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!mPaused) {
                            if (mActivity.getVoiceIntentCamera() && !mActivity.getOpenCameraOnly()) {
                                mDataModuleCurrent
                                        .set(Keys.KEY_VOICE_COUNTDOWN_DURATION,mActivity.getTimeDurationS());
                                onShutterButtonClick();
                                setCaptureCount(0);
                            }
                        }
                    }
                };
                if (useNewApi()) {
                    mCameraDevice.setSurfaceViewPreviewCallback(
                            new CameraAgent.CameraSurfaceViewPreviewCallback() {
                                @Override
                                public void onSurfaceUpdate() {
                                    Log.d(TAG, "SurfaceView: CameraSurfaceViewPreviewCallback");
                                    if (mPaused || mCameraDevice == null) {//SPRD:fix bug681598
                                        return;
                                    }
                                    mActivity.getCameraAppUI().hideModeCover();
                                    mHandler.post(hideModeCoverRunnable);
                                    Log.d(TAG,"Keys.KEY_VOICE_CAMERA_DONE is "+mDataModuleCurrent.getInt(Keys.KEY_VOICE_CAMERA_DONE,0));
                                    if (mActivity.getVoiceIntentCamera() && mDataModuleCurrent.getInt(Keys.KEY_VOICE_CAMERA_DONE,0) != 1) {
                                        mHandler.post(takePictureRunnable);
                                    }
                                    // set callback to null
                                    mCameraDevice.setSurfaceViewPreviewCallback(null);
                                }
                            });
                } else {
                    mHandler.postDelayed(hideModeCoverRunnable, 450);
                }
            }
        };

        doStartPreviewSpecial(isCameraIdle(), isHdr(), mActivity, mCameraDevice, mHandler,
                mDisplayOrientation, isCameraFrontFacing(), mUI.getRootView(), mCameraSettings);//SPRD:fix bug624871
        doStartPreview(startPreviewCallback, mCameraDevice);
        if(getModuleTpye() != DreamModule.AUDIOPICTURE_MODULE||!isShutterClicked())
        mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
        Log.i(TAG, "startPreview end!");
    }

    protected void dosetPreviewDisplayspecial() {
    }

    @Override
    public void stopPreview() {
        CameraProxy device = mCameraDevice;
        Log.i(TAG, "stopPreview start!mCameraDevice=" + device);
        /* SPRD: BUG 1036281 clear the faceview when stopPreview @{ */
        mUI.clearFaces();
        if (device != null && mCameraState != PREVIEW_STOPPED) {
            Log.i(TAG, "stopPreview");
            /* SPRD: fix bug677344 intent capture should not stop preview with flush @{ */
            if (isUseSurfaceView() && !mIsImageCaptureIntent) {
                if ((mActivity != null && mActivity.isPaused() && isShutterClicked())) {
                    device.stopPreviewWithOutFlush();//SPRD:fix bug1207611
                } else {
                    device.stopPreview();
                }
            } else {
                device.stopPreviewWithOutFlush();
            }
            /* @} */
            mFaceDetectionStarted = false;
            device.setSensorSelfShotCallback(null, null);
            mPreviewing = false;
        }
        setCameraState(PREVIEW_STOPPED);
        if (mFocusManager != null) {
            mFocusManager.onPreviewStopped();
        }
        Log.i(TAG, "stopPreview end!");
    }

    protected void updateCameraParametersInitialize() {
        // Reset preview frame rate to the maximum because it may be lowered by
        // video camera application.
        int[] fpsRange = CameraUtil
                .getPhotoPreviewFpsRange(mCameraCapabilities, useNewApi(), false);
        if (fpsRange != null && fpsRange.length > 0) {
            mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        }

        mCameraSettings.setRecordingHintEnabled(false);

        if (mCameraCapabilities
                .supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
            mCameraSettings.setVideoStabilization(false);
        }
    }

    private void updateCameraParametersZoom() {
        // Set zoom.
        if (mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            if (mZoomRatioSection != null && mZoomRatioSection[0] > 0){
                mCameraSettings.setZoomMin(mZoomRatioSection[0]);
            }
            mCameraSettings.setZoomRatio(mZoomValue);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setAutoExposureLockIfSupported() {
        if (mAeLockSupported) {
            mCameraSettings.setAutoExposureLock(mFocusManager.getAeAwbLock());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setAutoWhiteBalanceLockIfSupported() {
        if (mAwbLockSupported) {
            mCameraSettings.setAutoWhiteBalanceLock(mFocusManager
                    .getAeAwbLock());
        }
    }

    protected void setFocusAreasIfSupported() {
        if (mFocusAreaSupported) {
            mCameraSettings.setFocusAreas(mFocusManager.getFocusAreas());
        }
    }

    private void setAutoTrackingIfSupported() {
        if (mAutoChasingSupported){
            mCameraSettings.setCurrentAutoChasingRegion(mFocusManager.getAutoChasingRegion());
        }
    }

    protected void setMeteringAreasIfSupported() {
        if (mMeteringAreaSupported) {
            mCameraSettings.setMeteringAreas(mFocusManager.getMeteringAreas());
        }
    }

    private void updateCameraParametersPreference() {
        DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getCurrentDataModule();
        // some monkey tests can get here when shutting the app down
        // make sure mCameraDevice is still valid, b/17580046
        if (mCameraDevice == null) {
            return;
        }

        mCameraSettings.setDefault(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD == CameraUtil.getFrontFlashMode(),mCameraCapabilities);//SPRD:fix bug616836 add for photo use api1 or api2 use reconnect
        mCameraSettings.setEnterVideoMode(false);
        setAutoExposureLockIfSupported();
        setAutoWhiteBalanceLockIfSupported();
        setFocusAreasIfSupported();
        setAutoTrackingIfSupported();
        setMeteringAreasIfSupported();

        // Initialize focus mode.
        mFocusManager.overrideFocusMode(null);
        mCameraSettings.setFocusMode(mFocusManager.getFocusMode(mCameraSettings
                .getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE)));
        //SPRD Bug:1012986 update ai scene parameters.
        updateParametersAiSceneDetect();
        if (mIsImageCaptureIntent) {
            updateParametersFlashMode();
            mDataModuleCurrent.set(Keys.KEY_CAMERA_HDR, false);
        } else {
            // SPRD:Add for antibanding
            updateParametersAntibanding();

            // Set JPEG quality.
            updateParametersPictureQuality();

            // For the following settings, we need to check if the settings are
            // still supported by latest driver, if not, ignore the settings.
            // Set exposure compensation
            updateParametersExposureCompensation();

            // Set the scene mode: also sets flash and white balance.
            updateParametersSceneMode();

            // SPRD:Modify for add whitebalance bug 474737
            updateParametersWhiteBalance();

            // SPRD:Add for color effect Bug 474727
            updateParametersColorEffect();

            /*
             * SPRD:Add for ai detect @{ now face is not mutex with ai in UE's
             * doc. faceDatectMutex();
             * 
             * @}
             */
            updateParametersBurstCount();

            updateParametersFlashMode();

            // SPRD Bug:474721 Feature:Contrast.

            updateParametersContrast();

            // SPRD Bug:474724 Feature:ISO.
            updateParametersISO();

            updateParametersMetering();

            // SPRD Bug: 505155 Feature: zsl
            updateParametersZsl();

            // SPRD:Add for mirror
            updateParametersMirror();

            // SPRD : Fature : OIS && EIS
            updateParametersEOIS();

            // SPRD: Feature HDR
            updateParametersHDR();

            // SPRD: Feature Normal Hdr
            updateParametersNormalHDR();
            // SPRD: Feature BurstMode
            updateParametersBurstMode();

            // SPRD: Feature SensorSelfShot
            updateParametersSensorSelfShot();

            updateParameters3DNR();

            updateParametersThumbCallBack();

            updateParametersExposureTime();
            updateParametersFocusDistance();

            updateLPortraitRefocus();
            updateParametersSmileCapture();

        }

        updateParametersFaceAttribute();//SPRD: Fix bug 1105014 add face attribute enable tag
        // SPRD Bug:474722 Feature:Saturation.
        updateParametersSaturation();

        // SPRD:fix 501883 After closing shutter sound in camera ,it plays sound
        // when takepictures
        // from contact enter camera
        updateCameraShutterSound();

        updateParametersGridLine();

        if (mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            updateAutoFocusMoveCallback();
        }
        updateParametersFlashLevel();
        updateParametersAppModeId();
        /* SPRD: Fix Bug 975095 & 972105 update ai scene parameters @{ */
        //updateParametersAiSceneDetect();
        updateMakeLevel();
        updateLightPortraitDisplay();
        updateLightPortrait();
        updateAuto3DnrPatameter();
        updateParametersLogoWatermark();
        updateParametersTimeWatermark();
        updateLevel();
    }

    /* SPRD: Fix Bug 534257 New Feature EIS&OIS @{ */
    private void updateParametersEOIS() {
        if (isCameraFrontFacing() && CameraUtil.isEOISDcFrontEnabled()) {
            Log.i(TAG,
                    "front camera eois = " + mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DC_FRONT));
            if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EOIS_DC_FRONT)){
                return;
            }
            mCameraSettings.setEOISEnable(mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DC_FRONT));
            return;
        }

        if (!isCameraFrontFacing() && CameraUtil.isEOISDcBackEnabled()) {
            Log.i(TAG, "back camera eois = " + mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DC_BACK));
            if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EOIS_DC_BACK)){
                return;
            }
            mCameraSettings
                    .setEOISEnable(mDataModuleCurrent.getBoolean(Keys.KEY_EOIS_DC_BACK));
            return;
        }
    }
    /* @} */

    /**
     * This method sets picture size parameters. Size parameters should only be
     * set when the preview is stopped, and so this method is only invoked in
     * {@link #startPreview()} just before starting the preview.
     */
    protected void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }
/*
        List<Size> supported = Size.convert(mCameraCapabilities
                .getSupportedPhotoSizes());
        CameraPictureSizesCacher.updateSizesForCamera(
                mAppController.getAndroidContext(),
                mCameraDevice.getCameraId(), supported);
*/
        OneCamera.Facing cameraFacing = isCameraFrontFacing() ? OneCamera.Facing.FRONT
                : OneCamera.Facing.BACK;
        Size pictureSize;
        try {
            pictureSize = mAppController.getResolutionSetting()
                    .getPictureSize(DataModuleManager.getInstance(mAppController.getAndroidContext()),
                            mAppController.getCameraProvider()
                                    .getCurrentCameraId(), cameraFacing);
        } catch (OneCameraAccessException ex) {
            mAppController.getFatalErrorHandler()
                    .onGenericCameraAccessFailure();
            return;
        }
        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());

        if (ApiHelper.IS_NEXUS_5) {
            if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(pictureSize)) {
                mShouldResizeTo16x9 = true;
            } else {
                mShouldResizeTo16x9 = false;
            }
        }

        // SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
        mCameraSettings.setExifThumbnailSize(CameraUtil.getAdaptedThumbnailSize(pictureSize,
                mAppController.getCameraProvider()).toPortabilitySize());

        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = Size.convert(mCameraCapabilities
                .getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes,
                (double) pictureSize.width() / pictureSize.height());
        Size original = new Size(mCameraSettings.getCurrentPreviewSize());
        if (optimalSize != null && !optimalSize.equals(original)) {
            Log.i(TAG, "setting preview size. optimal: " + optimalSize
                    + "original: " + original);
            mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());
        }

        if (optimalSize != null && optimalSize.width() != 0 && optimalSize.height() != 0) {
            Log.i(TAG, "updating aspect ratio");
            mUI.updatePreviewAspectRatio((float) optimalSize.width()
                    / (float) optimalSize.height());
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }

    private void updateParametersAiDetect() {
        //unlock ae/af if locked
        if(isAELock){
            setAELock(false);
            cancelAutoFocus();
            if(mFocusManager != null) {
                mFocusManager.resetTouchFocus();
                mFocusManager.updateFocusState();
            }
        }
        mUI.intializeAIDetection(mDataModuleCurrent);
        updateFace();
        mUI.changeAiDetect();
        updateParametersFaceAttribute();
    }

    private void updateParametersFaceAttribute() {
        /* SPRD: Fix bug 1105014 face attibute change and add enable tag @{ */
        if (!(mFaceAttributeSupported || mSmileSupported))
            return;
        if(!(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES)||
                mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AI_DETECT_SMILE))){
            return;
        }

        if (mDataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES) ||
                mDataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_SMILE)){
            mCameraSettings.setCurrentFaceAttributes(1);
        }else{
            mCameraSettings.setCurrentFaceAttributes(0);
        }

        if (!mDataModuleCurrent.getBoolean(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES))
            mCameraSettings.setTouchAreas(null);
        /* @} */
    }

    /*
     * SPRD:Modify for jpegquality @{ private void
     * updateParametersPictureQuality() { int jpegQuality =
     * CameraProfile.getJpegEncodingQualityParameter(mCameraId,
     * CameraProfile.QUALITY_HIGH);
     * mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality); }
     */
    protected void updateParametersPictureQuality() {
        if (mJpegQualityController != null) {
            if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_JPEG_QUALITY)){
                return;
            }
            String quality = mDataModuleCurrent
                    .getString(Keys.KEY_JPEG_QUALITY);
            int jpegQuality = mJpegQualityController.findJpegQuality(quality);
            mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality);
        }
    }

    /* @} */

    // SPRD:Add for antibanding
    protected void updateParametersAntibanding() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMER_ANTIBANDING)){
            return;
        }
        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();
        String mAntibanding = mDataModuleCurrent
                .getString(Keys.KEY_CAMER_ANTIBANDING);
        if (mAntibanding == null) {
            return;
        }
        mCameraSettings.setAntibanding(stringifier
                .antibandingModeFromString(mAntibanding));
    }

    /* SPRD:Add for color effect Bug 474727 @{ */
    public void updateParametersColorEffect() {
//        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_COLOR_EFFECT)){
//            return;
//        }
//        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
//                .getStringifier();
//        String colorEffect = mDataModuleCurrent
//                .getString(Keys.KEY_CAMERA_COLOR_EFFECT);
//        if (colorEffect == null) {
//            return;
//        }
//        mColorEffect = stringifier.colorEffectFromString(colorEffect);
//
//        Log.d(TAG, "update ColorEffect = " + mColorEffect);
//        mCameraSettings.setColorEffect(mColorEffect);
    }

    public void updateParametersExposureCompensation() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EXPOSURE)) {
            return;
        }
        int value = mDataModuleCurrent.getInt(Keys.KEY_EXPOSURE);
        Log.i(TAG, "Keys.KEY_EXPOSURE = " + value);
        setExposureCompensation(value);
    }

    public void updateParametersExposureTime() {
        if (!CameraUtil.isManualShutterEnabled() || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_EXPOSURE_SHUTTERTIME)) return;
        int index = DreamSettingUtil.convertToInt(DataModuleManager.getInstance(mActivity).getDataModulePhoto().getString(
                DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_EXPOSURE_SHUTTERTIME, "0"));
        ArrayList<Long> timeList = mCameraCapabilities.getSupportedExposureTimeList();
        if (index == 0) {
            mCameraSettings.setExposureTime(0);
        } else {
            mCameraSettings.setExposureTime((long)(1e9/timeList.get(index).intValue()));
        }
    }

    protected boolean isManualFocusEnabled() {
        if (CameraUtil.isManualFocusEnabled() && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_FOCUS_DISTANCE)) {
            return true;
        }
        return false;
    }

    public void updateParametersFocusDistanceByManual() {

    }

    public void updateParametersFocusDistance() {
        if (!isManualFocusEnabled())
            return;
        int progress = DreamSettingUtil.convertToInt(DataModuleManager.getInstance(mActivity).getDataModulePhoto()
                .getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_CAMERA_FOCUS_DISTANCE, "0"));
        if (progress != 0
                && CameraCapabilities.FocusMode.FIXED != mCameraSettings.getCurrentFocusMode()) {
            mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.FIXED);
        }
        if (progress == 0) {
            mCameraSettings.setFocusMode(mFocusManager.getFocusMode(mCameraSettings
                    .getCurrentFocusMode(), mDataModule.getString(Keys.KEY_FOCUS_MODE)));
        }
        float distance = mCameraCapabilities.getFocusDistanceScale();
        float degreeScale = (float) (Math.round((distance * 100) / 100)) / 99;
        float focusValue = distance - degreeScale * (progress - 1);
        mCameraSettings.setFocusDistance(focusValue);
    }

    // SPRD:Fix bug 657472 add for saving normal pic for HDR
    private void updateParametersNormalHDR(){
        if (!CameraUtil.isNormalHdrEnabled() || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR_NORMAL_PIC)) {
            return;
        }
        if(mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 1 || (isAutoHdr() && isHdrScene))){//SPRD:fix bug840683,934697,set hdr normal on when hdr is close,capture will fail
            //mSceneMode = CameraCapabilities.SceneMode.HDR_NORMAL;
            //mCameraSettings.setSceneMode(mSceneMode);
            mCameraSettings.setNormalHdrModeEnable(1);
            Log.i(TAG, "updateParametersNormalHdr setNormalHdrModeEnable : 1");
        }else{
            mCameraSettings.setNormalHdrModeEnable(0);
            Log.i(TAG, "updateParametersNormalHdr setNormalHdrModeEnable : 0");
        }
    }
    protected void updateParametersLogoWatermark() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AUTO_ADD_LOGOWATERMARK)){
            return;
        }
        if(mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_ADD_LOGOWATERMARK)){
            mCameraSettings.setLogoWatermarkEnable(1);
            Log.i(TAG, "updateParametersLogoWatermark setLogoWatermarkEnable : 1");
        }else{
            mCameraSettings.setLogoWatermarkEnable(0);
            Log.i(TAG, "updateParametersLogoWatermark setLogoWatermarkEnable : 0");
        }
    }
    protected void updateParametersTimeWatermark() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AUTO_ADD_TIMEWATERMARK)){
            return;
        }
        if(mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_ADD_TIMEWATERMARK)){
            mCameraSettings.setTimeWatermarkEnable(1);
            Log.i(TAG, "updateParametersTimeWatermark setTimeWatermarkEnable : 1");
        }else{
            mCameraSettings.setTimeWatermarkEnable(0);
            Log.i(TAG, "updateParametersTimeWatermark setTimeWatermarkEnable : 0");
        }
    }

    private boolean lastLightPortraitEnable = false;

    protected void updateLightPortrait() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_LIGHT_PORTIAIT)){
            return;
        }
        int lightPortraitValue = mDataModuleCurrent.getInt(Keys.KEY_LIGHT_PORTIAIT);
        mCameraSettings.setLightPortraitType(lightPortraitValue);
        Log.i(TAG, "updateLightPortrait lightPortraitValue = " + lightPortraitValue);

        onLightPortraitEnable(!LightPortraitControllerDream.CLOSE_VALUE.equals(
                "" + lightPortraitValue));
        showLightPortraitOverLay();

    }

    protected void updateLPortraitRefocus() { }



    /* SPRD:fix bug1042598 reset af @{ */
    protected void resetAF() {
        if(mFocusManager != null) {
            mFocusManager.clearAfArea();
            mFocusManager.updateFocusState();
            mCameraSettings.setFocusMode(mFocusManager.getFocusMode(mCameraSettings
                    .getCurrentFocusMode(),mDataModule.getString(Keys.KEY_FOCUS_MODE)));
        }
        if (mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            updateAutoFocusMoveCallback();
        }
    }
    /* @} */


    private void updateParametersHDR() {
        /* SPRD:fix bug619441 HDR is one of scene mode, not need to effect scene mode setting @ */
        if (mCameraDevice == null || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)) {
            return;
        }
        /* @}　*/
        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();

        Log.d(TAG, "updateParametersHDR mSceneMode=" + mSceneMode + ","
                + isHdr() + ",isAutoHdr() " + isAutoHdr());
        if (isHdr()) {
            mSceneMode = CameraCapabilities.SceneMode.HDR;
            updateAELock(false);
        } else {
            if(isAutoHdr())
                updateAELock(false);
            mSceneMode = stringifier.sceneModeFromString("auto");
        }

        if(!mCameraCapabilities.supports(mSceneMode)){
            mSceneMode = CameraCapabilities.SceneMode.AUTO;
        }

        if (mCameraSettings.getCurrentSceneMode() != mSceneMode) {
            mCameraSettings.setSceneMode(mSceneMode);
        }
        Log.d(TAG, "updateParametersSceneMode mSceneMode=" + mSceneMode);

        if(isAutoHdrSupported()) {
            mCameraSettings.setAutoHdr(isAutoHdr());
            if(isAutoHdr()) {
                mCameraDevice.setHdrDetectionCallback(mHandler, this);
                mCameraDevice.setHdrDetectionWork(true);
            } else {
                mCameraDevice.setHdrDetectionWork(false);
            }
        }

        if(isHdr()){//Sprd:fix bug934697
            mUI.showHdrTips(true);
        } else if(!isAutoHdr()){
            mUI.showHdrTips(false);
        }
    }

    protected void updateParametersSceneMode() {
        /* SPRD:fix bug517304 NullPointer Exception */
        if (mCameraDevice == null) {
            return;
        }

        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_SCENE_MODE)){
            return;
        }

        CameraCapabilities.Stringifier stringifier = mCameraCapabilities
                .getStringifier();
        String sceneMode = mDataModuleCurrent.getString(Keys.KEY_SCENE_MODE);

        if (sceneMode == null) {
            return;
        }
        if ("normal".equals(sceneMode)) {// Fix bug 411026
            sceneMode = "auto";
        }
        mSceneMode = stringifier.sceneModeFromString(sceneMode);

        /* @} */
        if(!mCameraCapabilities.supports(mSceneMode)){
            mSceneMode = CameraCapabilities.SceneMode.AUTO;
        }

        if (mCameraSettings.getCurrentSceneMode() != mSceneMode) {
            mCameraSettings.setSceneMode(mSceneMode);
        }
        Log.d(TAG, "updateParametersSceneMode mSceneMode=" + mSceneMode);

    }

    /* SPRD:Modify for add whitebalance bug 474737 @{ */
    public void updateParametersWhiteBalance() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_WHITE_BALANCE)){
            return;
        }
        String wb = mDataModuleCurrent.getString(Keys.KEY_WHITE_BALANCE);
        Log.d(TAG, "updateParametersWhiteBalance = " + wb);
        if (wb == null) {
            return;
        }
        CameraCapabilities.WhiteBalance whiteBalance = mCameraCapabilities
                .getStringifier().whiteBalanceFromString(wb);

        if (mCameraCapabilities.supports(whiteBalance)) {
            mCameraSettings.setWhiteBalance(whiteBalance);
        }
    }

    /* @} */

    protected void updateParametersFlashMode() {
        if (mCameraCapabilities == null || !CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing())) return;
        updateFlashLevelUI();
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FLASH_MODE)){
            Log.i(TAG,"this module does not support flash,return");
            return;
        }
        String flash = mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE);
        if (flash == null) {
            Log.i(TAG,"flash is null,return");
            return;
        }
        //SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
        if (!mAppController.getButtonManager().isEnabled(ButtonManagerDream.BUTTON_FLASH_DREAM)) {
            Log.i(TAG,"flash is disabled,return");
            return;
        }
        if (mActivity.getIsButteryLow() && !"off".equals(flash)) {
            flash = "off";
        }

        CameraCapabilities.FlashMode flashMode = mCameraCapabilities
                .getStringifier().flashModeFromString(flash);

        if(mFocusManager.getAELockState()){
            flashMode = mCameraCapabilities
                    .getStringifier().flashModeFromString("off");
        }

        if (mCameraCapabilities.supports(flashMode)) {
            mCameraSettings.setFlashMode(flashMode);
            if (DreamUtil.getRightCamera(mCameraId) == DreamUtil.FRONT_CAMERA && CameraUtil.getFrontFlashMode() == CameraUtil.VALUE_FRONT_FLASH_MODE_LCD) {
                Log.e(TAG,"front camera's flash type is lcd");
                mCameraSettings.setFlashType(CameraUtil.VALUE_FRONT_FLASH_MODE_LCD);
            } else {
                mCameraSettings.setFlashType(CameraUtil.VALUE_FRONT_FLASH_MODE_LED);
            }
        }

        Log.d(TAG, "updateParametersFlashMode = " + flashMode);
    }

    //nj dream camera test 70, 75
    protected void updateParametersGridLine() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_GRID_LINES)){
            return;
        }
        mAppController.getCameraAppUI().initGridlineView();
        String grid = mDataModuleCurrent.getString(Keys.KEY_CAMERA_GRID_LINES);
        mAppController.getCameraAppUI().updateScreenGridLines(grid);
        Log.d(TAG, "updateParametersGridLine = " + grid);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateAutoFocusMoveCallback() {
        if (mCameraDevice == null) {
            return;
        }
        if (mCameraSettings.getCurrentFocusMode() == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
            mCameraDevice.setAutoFocusMoveCallback(mHandler,
                    (CameraAFMoveCallback) mAutoFocusMoveCallback);
        } else {
            mCameraDevice.setAutoFocusMoveCallback(null, null);
        }
    }
    protected void updateParametersBurstMode() {
        Log.i(TAG, "updateParametersBurstMode");
        if(!CameraUtil.isBurstModeEnable()){
            return;
        }
        if (isHdr()) {
            Log.i(TAG, "setBurstModeEnable : 0");
            mCameraSettings.setBurstModeEnable(0);
        } else {
            Log.i(TAG, "setBurstModeEnable : 1");
            mCameraSettings.setBurstModeEnable(1);
        }
    }

    private void updateParametersSensorSelfShot() {
        Log.i(TAG, "updateParametersSensorSelfShot");
        if (!CameraUtil.isSensorSelfShotEnable()) {
            return;
        }
        if (!mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_SENSOR_SELF_SHOT)) {
            Log.i(TAG, "setSensorSelfShotEnable : 0");
            mCameraSettings.setSensorSelfShotEnable(0);
        } else {
            Log.i(TAG, "setSensorSelfShotEnable : 1");
            mCameraSettings.setSensorSelfShotEnable(1);
        }
    }

    protected void updateParameters3DNR() {
        if (!CameraUtil.is3DNREnable()) {
            return;
        }
        Log.i(TAG, "updateParameters3DNR set3DNREnable : 0");
        mCameraSettings.set3DNREnable(0);
    }

    protected void updateParametersThumbCallBack() {
        if (!CameraUtil.isNeedThumbCallback()) {
            return;
        }
        Log.i(TAG, "updateParametersThumbCallBack false");
        mCameraSettings.setNeedThumbCallBack(false);
        mCameraSettings.setThumbCallBack(0);
    }

    private void removeAutoFocusMoveCallback() {
        if (mCameraDevice != null) {
            mCameraDevice.setAutoFocusMoveCallback(null, null);
        }
    }

    public void setExposureTime(int index) {
        mDataModuleCurrent.changeSettings(Keys.KEY_EXPOSURE_SHUTTERTIME, index);
    }

    public void setFocusDistance(int progress) {
        //change and notify to update parameters.
        mDataModuleCurrent.changeSettings(Keys.KEY_CAMERA_FOCUS_DISTANCE, progress);
        //updateParametersFocusDistance
    }

    /**
     * Sets the exposure compensation to the given value and also updates
     * settings.
     * 
     * @param value
     *            exposure compensation value to be set
     */
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

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    protected void setCameraParameters(int updateSet) {
        if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
            updateCameraParametersInitialize();
        }

        if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
            updateCameraParametersZoom();
        }

        if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
            updateCameraParametersPreference();
        }
        Log.d(TAG, "setCameraParameters mCameraDevice = " + mCameraDevice);
        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    // If the Camera is idle, update the parameters immediately, otherwise
    // accumulate them in mUpdateSet and update later.
    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        mUpdateSet |= additionalUpdateSet;
        if (mCameraDevice == null) {
            // We will update all the parameters when we open the device, so
            // we don't need to do anything now.
            mUpdateSet = 0;
            return;
        } else if (isCameraIdle()) {
            setCameraParameters(mUpdateSet);
            /*
             * SPRD: All MUTEX OPERATION in onSettingsChanged function.
             * updateSceneMode();
             */
            mUpdateSet = 0;
        } else {
            if (!mHandler.hasMessages(MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
                mHandler.sendEmptyMessageDelayed(
                        MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
            }
        }
    }

    @Override
    public boolean isCameraIdle() {
        return (mCameraState == IDLE)
                || (mCameraState == PREVIEW_STOPPED)
                || ((mFocusManager != null) && mFocusManager.isFocusCompleted() && (mCameraState != SWITCHING_CAMERA));
    }

    @Override
    public boolean isImageCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE
                .equals(action));
    }

    private void setupCaptureParams() {
        Bundle myExtras = mActivity.getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            mCropValue = myExtras.getString("crop");
        }
    }

    private void initializeCapabilities() {
        mCameraCapabilities = mCameraDevice.getCapabilities();
        mFocusAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.FOCUS_AREA);
        mMeteringAreaSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.METERING_AREA);
        mAeLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        mAwbLockSupported = mCameraCapabilities
                .supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK);
        mContinuousFocusSupported = mCameraCapabilities
                .supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
        mMaxRatio = mCameraCapabilities.getMaxZoomRatio();

        mZoomRatioSection = mCameraCapabilities.getZoomRatioSection();
        if (mZoomRatioSection != null) {
            mMinRatio = mZoomRatioSection[0];
            for (float max : mZoomRatioSection){
                mMaxRatio = mMaxRatio > max ? mMaxRatio : max;
            }
        } else {
            mMinRatio = 1.0f;
        }

        mAutoHdrSupported = mCameraCapabilities.supportAutoHdr();

        mAuto3DnrSupported = mCameraCapabilities.supportAuto3Dnr();
        // for Ai Scene Detection
        mAiSceneSupported = mCameraCapabilities.getSupportAiScene() &&
                (mActivity.getCurrentModuleIndex() == AUTO_PHOTO);

        if(mAiSceneSupported) {
            CameraUtil.setAiSceneDetectSupportable(mAiSceneSupported);
            mCameraDevice.setAiSceneCallback(mHandler, this);
            mCameraDevice.setAiSceneWork(true);
        }

        mAutoChasingSupported = CameraUtil.isAutoChasingSupport();
        if (mAutoChasingSupported) {
            mCameraDevice.setAutoChasingTraceRegionCallback(mHandler,this);
        }

        mFaceAttributeSupported = mCameraCapabilities.getSupportedFaceAttributesEnable();
        mSmileSupported = mCameraCapabilities.getSupportedSmileEnable();
    }

    protected boolean hasAiSceneSupported() {
        return mAiSceneSupported;
    }

    protected boolean isAuto3DnrSupported(){
        return mAuto3DnrSupported;
    }

    public boolean isAutoHdrSupported(){return mAutoHdrSupported;}

    @Override
    public void onZoomChanged(float ratio) {
        // Not useful to change zoom value when the activity is paused.
        if (mPaused) {
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

    @Override
    public int getCameraState() {
        return mCameraState;
    }

    @Override
    public void onMemoryStateChanged(int state) {
        /**
         * SPRD: fix bug 473462 add for burst capture @{
         * mAppController.setShutterEnabled(state == MemoryManager.STATE_OK);
         */
        Log.i(TAG, "onMemoryStateChanged,(state == MemoryManager.STATE_OK)"
                + (state == MemoryManager.STATE_OK)
                + " mContinuousCaptureCount = " + mContinuousCaptureCount);
        if (mContinuousCaptureCount <= 0 ) {
            if(!mBurstNotShowShutterButton) {
                mAppController.setShutterEnabled(state == MemoryManager.STATE_OK);
                if (!isNeedThumbCallBack()) {
                    mAppController.getCameraAppUI().setShutterButtonClick(state == MemoryManager.STATE_OK);
                }
            }
            if (!(state == MemoryManager.STATE_OK) && mActivity != null) {
                CameraUtil.toastHint(mActivity,R.string.message_save_task_memory_limit);
            }

        }
    }

    @Override
    public void onLowMemory() {
        // Not much we can do in the photo module.
    }

    // For debugging only.
    public void setDebugUri(Uri uri) {
        mDebugUri = uri;
    }

    // For debugging only.
    private void saveToDebugUri(byte[] data) {
        if (mDebugUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = mContentResolver.openOutputStream(mDebugUri);
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while writing debug jpeg file", e);
            } finally {
                CameraUtil.closeSilently(outputStream);
            }
        }
    }

    /* SPRD: porting new feature JPEG quality start @{ */
    private JpegQualityController mJpegQualityController;

    private class JpegQualityController {
        private static final String LOG_TAG = "CameraJpegQualityController";
        private final Log.Tag TAG = new Log.Tag("PhotoModule");
        private static final String VAL_NORMAL = "normal";
        private static final String VAL_HIGHT = "hight";
        private static final String VAL_SUPER = "super";
        private static final int VAL_DEFAULT_QUALITY = 85;

        // default construct
        /* package */
        JpegQualityController() {
        }

        public int findJpegQuality(String quality) {
            Log.d(TAG, "findJpegQuality");
            int convertQuality = getConvertJpegQuality(quality);
            int result = VAL_DEFAULT_QUALITY;
            Log.d(TAG, "findJpegQuality convertQuality = " + convertQuality
                    + " VAL_DEFAULT_QUALITY != convertQuality = "
                    + (VAL_DEFAULT_QUALITY != convertQuality));
            if (VAL_DEFAULT_QUALITY != convertQuality) {
                result = CameraProfile.getJpegEncodingQualityParameter(convertQuality);
                Log.d(TAG, "findJpegQuality result = " + result);
            }
            Log.d(TAG, "findJpegQuality result = " + result);
            return result;
        }

        private int getConvertJpegQuality(String quality) {
            Log.d(TAG, "getConvertJpegQuality");
            int result = VAL_DEFAULT_QUALITY;
            if (quality != null) {
                if (VAL_NORMAL.equals(quality))
                    result = CameraProfile.QUALITY_LOW;
                else if (VAL_HIGHT.equals(quality))
                    result = CameraProfile.QUALITY_MEDIUM;
                else if (VAL_SUPER.equals(VAL_SUPER))
                    result = CameraProfile.QUALITY_HIGH;
            }
            Log.d(TAG, "getConvertJpegQuality result = " + result);
            return result;
        }
    }

    /* }@ dev new feature jpeg quality end */

    /* SPRD:Add for ai detect @{ */
    protected void updateFace() {
       if (mCameraDevice == null) {
            Log.i(TAG, "mCameraDevice is null ");
            return;
        }
       if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_FACE_DATECT)){
           return;
       }
        mFace = mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_FACE_DATECT);
        if (mFace == null) {
            Log.i(TAG, "mFace is null ");
            return;
        }
        Log.d(TAG, "face = " + mFace + "; hdrState = " + isHdr());
        if (mFace && isCameraIdle()) {
//            mDataModuleCurrent.set(Keys.KEY_CAMERA_AI_DATECT,mFace);
            startFaceDetection();
        } else if (!mFace || !isCameraIdle()) {
//            mDataModuleCurrent.set(Keys.KEY_CAMERA_AI_DATECT,mFace);
            stopFaceDetection();
        }
        mCameraDevice.applySettings(mCameraSettings); // SPRD: BUG 531871 Smile capture is invalid
    }

    protected boolean isHdr() {
        return mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 1);
    }

    public boolean isAutoHdr() {
        return mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 2);
    }
    // SPRD:Add for ai detect
    public void setCaptureCount(int count) {
        Log.d(TAG, "setCaptureCount count=" + count);
        mCaptureCount = count;
    }

    public void setSmileCapture(boolean smile) {
        Log.i(TAG, " setSmileCapture smile = " + smile);
        mSmileCapture = smile;
    }

    /* @} */

    /* SPRD: BUG 397228 stop or start face detection start @{ */
    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        // SPRD BUG: 397097
        if (visibility == ModuleController.VISIBILITY_VISIBLE) {
            mUI.resumeFaceDetection();
        } else {
            mUI.pauseFaceDetection();
            mUI.clearFaces();
        }
    }

    /* }@ BUG 397228 end */

    /*
     * SPRD: Fix 473602 bug: flash is on while shutter in photoModule then the
     * process is slowly
     * 
     * @{
     */
    protected boolean isFocusModeFixed() {
        if (mCameraSettings == null
                || mCameraSettings.getCurrentFocusMode() == CameraCapabilities.FocusMode.FIXED) {
            return true;
        }
        return false;
    }

    /* SPRD:fix bug471950 add burst for Camera Key @{ */
    protected void handleActionDown(int action) {
        Log.d(TAG, "handleActionDown inBurstMode =" + inBurstMode() + "," + mIsImageCaptureIntent
                + "," + checkStorage()+" action="+action);
        //SPRD:fix bug1046485
        if(inBurstMode() && !checkStorage()){
            mUI.enablePreviewOverlayHint(true);
        }
        if (inBurstMode() && checkStorage() && !mIsImageCaptureIntent) {
            mBurstCaptureType = action;
            /* SPRD: fix bug 640448 beauty(makeup) module do not support batch shooting @{ */
            if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_BEAUTY_ENTERED) && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED)){
                return;
            }
            /* @} */
            Log.d(TAG, "mActivity =" + mActivity);
            if (mActivity != null && inBurstMode()) {
                Log.d(TAG, "start continue capture");
                mCancelBurst = false;
                mBurstWorking = true;
                mIsContinousCaptureFinish = false;
                mCaptureCount = 0;
                mHasRemove.set(false);
                onShutterButtonClick(false/* checkShutterOnBurstWorking */);
            }
        }
    }
    /* @} */

    protected void handleActionUp() {
        Log.i(TAG, " handleActionUp ");
        if (mActivity != null && inBurstMode()) {
            Log.i(TAG, " handleActionUp enter mBurstWorking = " + mBurstWorking);
            if (mBurstWorking) {
                mBurstNotShowShutterButton = true;
                mActivity.getCameraAppUI().setShutterButtonEnabled(false);
                cancelBurstCapture();
            }
        }
        mBurstCaptureType = -1;
    }

    private boolean checkStorage() {
        if (mActivity == null) {
            return false;
        }
        if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES
            || mActivity.getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES) {
            Log.i(TAG, "Not enough space or storage not ready.");
            return false;
        }
        return true;
    }

    public AtomicBoolean mHasRemove = new AtomicBoolean(false);

    public void cancelBurstCapture() {
        Log.i(TAG, "cancelBurstCapture mCameraState = " + mCameraState
                + " mHasCaptureCount = " + mHasCaptureCount
                + " mCameraDevice = " + mCameraDevice);
        mCancelBurst = true;
        if (mCameraState == SNAPSHOT_IN_PROGRESS) {
            if (mCameraDevice != null) {
                mIsContinousCaptureFinish = true;
                // SPRD:fix bug 497854 when cancel 10 burst capture,the count of pics saveing is wrong
                mHasRemove.set(mCameraDevice.cancelBurstCapture(this));
            }
        } else {
            /*SPRD:fix bug693617 enter wrong burst working and don't reset @{*/
            if (mCameraState == FOCUSING && mCameraDevice != null) {
                mHasRemove.set(mCameraDevice.cancelBurstCapture(this));
            }
            mBurstWorking = false;
            if (mActivity != null) {
                mActivity.getCameraAppUI().setShutterButtonEnabled(true);
            }
            /* @} */
            mIsContinousCaptureFinish = true;
            return;
        }
    }

    @Override
    public void onCanceled(int count) {

    }

    public int getBurstHasCaptureCount() {
        return mHasCaptureCount;
    }

    public void onHideBurstScreenHint() {
        mCanSensorSelfShot = true;
        mUI.dismissBurstScreenHit();
        if (!mAppController.getCameraAppUI().isBottomBarNull()/* SPRD: fix bug698018 BottomBar is null sometimes so need check at first*/
                && mAppController.getCameraAppUI().isInFreezeReview()) {// SPRD :BUG
                                                                 // 398284
            mAppController.getCameraAppUI().setSwipeEnabled(false);
            mAppController.getCameraAppUI().onShutterButtonClick();
        } else {
            if(getModuleTpye() != DreamModule.INTERVAL_MODULE)
                mAppController.getCameraAppUI().setSwipeEnabled(true);
        }
    }

    public int getContinuousCaptureCount() {
        return mContinuousCaptureCount;
    }

    public int getContinuousCount() {
        int continuousCount;
        if (!(inBurstMode() && mBurstWorking)) {
            continuousCount = 1;
        } else {
            continuousCount = ContinuePhotoModule.MAX_BURST_COUNT;
        }
        return continuousCount;
    }

    protected boolean isBurstCapture() {
        if (getContinuousCount() > 1) {
            return true;
        }
        return false;
    }

    protected boolean isAudioCapture() {
        return false;
    }

    private void updateParametersBurstCount() {
        if (!inBurstMode()) {
            return;
        }
        mCameraSettings.setBurstPicNum(!mBurstWorking ? 1 : ContinuePhotoModule.MAX_BURST_COUNT);
    }

    private void updateParametersBurstPictureSize() {
        Log.i(TAG, "updateParametersBurstPictureSize");
        if (!isCameraFrontFacing()) {
            Size selectSize = new Size(mCameraSettings.getCurrentPhotoSize());
            if (selectSize != null
                    && selectSize.width() * selectSize.height() >= PICTURE_SIZE_13M) {
                List<Size> supported = Size.convert(mCameraCapabilities
                        .getSupportedPhotoSizes());
                List<Size> pictureSizes = ResolutionUtil
                        .getDisplayableSizesFromSupported(supported, true);
                if (pictureSizes.size() >= 2) {
                    Size secondSize = pictureSizes.get(1);
                    mCameraSettings
                            .setPhotoSize(secondSize.toPortabilitySize());
                    Log.i(TAG, " secondSize = " + secondSize);
                }
                int jpegQuality = mJpegQualityController
                        .findJpegQuality("normal");
                mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality);
            }
        }
    }

    /**
     * @}
     */

    // SPRD Bug:474721 Feature:Contrast.
    public void updateParametersContrast() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_CONTRAST)){
            return;
        }
        String sContrast = mDataModuleCurrent.getString(Keys.KEY_CAMERA_CONTRAST);
        Log.d(TAG, "updateParametersContrast = " + sContrast);
        if (sContrast == null) {
            return;
        }
        CameraCapabilities.Contrast contrast = mCameraCapabilities
                .getStringifier().contrastFromString(sContrast);
        if (mCameraCapabilities.supports(contrast)) {
            mCameraSettings.setContrast(contrast);
        }
    }

    /* SPRD: fix bug 474672 add for ucam beauty @{ */
    @Override
    public void onBeautyValueChanged(int[] value) {
        Log.i(TAG, "onBeautyValueChanged setParameters Value = " + Arrays.toString(value));
        if (mCameraSettings != null) {
            mCameraSettings.setSkinWhitenLevel(value);
            if (mCameraDevice != null) {
                if (!mFirstHasStartCapture){
                    mCameraDevice.applySettings(mCameraSettings);
                }
            }
        }
    }


    // SPRD Bug:474724 Feature:ISO.
    public void updateParametersISO() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_ISO)){
            return;
        }
        String sISO = mDataModuleCurrent.getString(Keys.KEY_CAMERA_ISO);
        Log.d(TAG, "updateParametersISO = " + sISO);
        if (sISO == null) {
            return;
        }
        CameraCapabilities.ISO iso = mCameraCapabilities.getStringifier()
                .isoModeFromString(sISO);
        if (mCameraCapabilities.supports(iso)) {
            mCameraSettings.setISO(iso);
        }
    }

    // SPRD Bug:474718 Feature:Metering.
    protected void updateParametersMetering() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMER_METERING)){
            return;
        }
        Log.i(TAG, "updateParametersMetering");
        String sMetering = mDataModuleCurrent.getString(Keys.KEY_CAMER_METERING);
        if (sMetering == null) {
            return;
        }
        CameraCapabilities.Metering metering = mCameraCapabilities
                .getStringifier().meteringFromString(sMetering);
        if (mCameraCapabilities.supports(metering)) {
            mCameraSettings.setMetering(metering);
        }
    }

    // SPRD Bug:474722 Feature:Saturation.
    public void updateParametersSaturation() {
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_SATURATION)){
            return;
        }
        String sSaturation = mDataModuleCurrent.getString(Keys.KEY_CAMERA_SATURATION);
        if (sSaturation == null) {
            return;
        }
        Log.v(TAG, "updateParametersSaturation = " + sSaturation);
        CameraCapabilities.Saturation saturation = mCameraCapabilities
                .getStringifier().saturationFromString(sSaturation);
        if (mCameraCapabilities.supports(saturation)) {
            mCameraSettings.setSaturation(saturation);
        }
    }

    // SPRD Bug:514488 Click Button when rotation.
    public void postDelayed(Runnable r, long delayMillis) {
        mHandler.postDelayed(r, delayMillis);
    }

    public PhotoUI createUI(CameraActivity activity) {
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        return new PhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public void singleTapAEAF(int x, int y) {
        Log.d(TAG, "singleTapAEAF " + isSupportTouchAFAE());
        if (isSupportTouchAFAE()) {
            mFocusManager.onSingleTapUp(x, y);
        }
    }

    // SPRD Bug:505155 Feature:zsl.
    protected void updateParametersZsl() {

        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_ZSL)){
            return;
        }

        boolean zslProp = CameraUtil.isZslEnable();
        int zsl = mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_ZSL) && zslProp ? 1 : 0;
        Log.i(TAG, "updateParametersZsl prop = " + zslProp + " , zsl = " + zsl + " , setZslModeEnable(" + zsl + ")");
        mCameraSettings.setZslModeEnable(zsl);
    }

    // SPRD Add for highiso
    private void setParametersHighISO(boolean highisoenable) {
        if (!CameraUtil.isHighISOEnable()) {
            return;
        }
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_HIGH_ISO)) {
            return;
        }
        if (mDataModuleCurrent.getBoolean(Keys.KEY_HIGH_ISO)) {
            if (highisoenable) {
                Log.i(TAG, "updateParametersHighISO setHighISOModeEnable : 1");
                mCameraSettings.setHighISOEnable(1);
            } else {
                Log.i(TAG, "updateParametersHighISO setHighISOModeEnable : 0");
                mCameraSettings.setHighISOEnable(0);
            }
        }
    }

    public void applySettings() {
        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    protected void mediaSaved(Uri uri) {

    }

    protected int getDeviceCameraId() {
        return mCameraDevice.getCameraId();
    }

    public void doOnPreviewStartedSpecial(boolean isCameraIdle, boolean isHdrOn,
            CameraActivity activity, CameraAgent.CameraProxy cameraDevice, Handler h,
            int displayOrientation, boolean mirror, View rootView) {
    }

    public void doCaptureSpecial() {
        if(getModuleTpye() == DreamModule.FRONT_REFOCUS_MODULE || getModuleTpye() == DreamModule.REFOCUS_MODULE) {
            mCameraDevice.setBlurCaptureCallback(mHandler, this);
        }
        mActivity.doCaptureSpecial();
    }

    public void doCloseCameraSpecial(CameraActivity activity, CameraProxy cameraDevice) {
    }

    protected void doStartPreviewSpecial(boolean isCameraIdle, boolean isHdrOn,
            CameraActivity activity, CameraAgent.CameraProxy cameraDevice, Handler h,
            int displayOrientation, boolean mirror, View rootView, CameraSettings cameraSettings) {
    }

    protected void doStartPreview(CameraAgent.CameraStartPreviewCallback startPreviewCallback, CameraAgent.CameraProxy cameraDevice) {
        if (useNewApi()) {
            mCameraDevice.startPreview();
            startPreviewCallback.onPreviewStarted();
        } else {
            mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()),
                    startPreviewCallback);
        }
        mPreviewing = true;

        // SPRD: Fix bug 659315, optimize camera launch time
        mUI.onPreviewStarted();
        //SPRD: Fix bug 1200379
        mUI.enableUIAfterTakepicture();
        mActivity.getButtonManager().enableCameraButton();// SPRD: Fix bug839922, switch icon is disabled
        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        orientationManager.addOnOrientationChangeListener(this);
        mUI.onOrientationChanged(orientationManager,
                orientationManager.getDeviceOrientation());
    }
    /* @} */
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
    /* SPRD:Add for mirror @{ */
    protected void updateParametersMirror() {
        if (isCameraFrontFacing()) {
            if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FRONT_CAMERA_MIRROR)){
                return;
            }
            mCameraSettings.setFrontCameraMirror(mDataModuleCurrent
                    .getBoolean(Keys.KEY_FRONT_CAMERA_MIRROR));
        }
    }

    /*
     * for dream camera if it want to do something
     * @{
     */
    /* dream test 50 @{ */
    protected void doSomethingWhenonPictureTaken(byte[] jpeg) {
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, true);
        if(mBlurHint) {
            mBlurHint = false;
            if (mUI != null)
                mUI.dismissBlurScreenHit();//SPRD:fix bug968611
        } else {
            if ((getModuleTpye() == DreamModule.FRONT_REFOCUS_MODULE || getModuleTpye() == DreamModule.REFOCUS_MODULE) && mCameraDevice != null) {
                mCameraDevice.setBlurCaptureCallback(null, null);
            }
        }
    }
    /* @} */

    protected void doSomethingWhenonShutterButtonClick() {

    }

    // Bug 1014851/1154830 - send Device Orientation to HAL for AI Scene Detection
    protected void sendDeviceOrientation() {
        if (mCameraDevice != null && mCameraSettings != null && mActivity != null && mActivity.getOrientationManager() != null) {
            int deviceOrientation = mActivity.getOrientationManager().getDeviceOrientation().getDegrees();
            mCameraSettings.setDeviceOrientation(deviceOrientation);
            Log.i(TAG, "send DeviceOrientation " + deviceOrientation + " to HAL");
        }
    }

    /* @} */
    /*
     * Add for ui check 122 @{
     */
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
            OrientationManager.DeviceOrientation deviceOrientation) {
        sendDeviceOrientation();
        if(mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
        mUI.onOrientationChanged(orientationManager, deviceOrientation);
    }
    /*
     * @}
     */
    //dream test 11
    @Override
    public void updateParameter(String key) {
        switch(key){
            case Keys.KEY_CAMERA_SHUTTER_SOUND:
                updateCameraShutterSound();
                break;
            default:
                    break;
        }
    }
    /* dream test 84 @{ */
    protected void dosomethingWhenPause() {
        if (mBlurHint) {
            mBlurHint = false;
            mUI.dismissBlurScreenHit();
        }
        mUI.showLightPortraitOverLay(false);
    }
    /* @} */

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    protected void startAudioRecord() {}

    protected void onAudioRecordStopped() {}

    /**
     * updateBatteryLevel
     * @param level the battery level when level changed
     */
    public boolean isFirstShowToast = true;
    public boolean isBatteryAgainLowLevel = false;

    @Override
    public void updateBatteryLevel(int level) {
        if (mDataModuleCurrent == null
                || !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_FLASH_MODE)) {
            return;
        }
        String BEFORE_LOW_BATTERY = "_before_low_battery";
        // the limen value which set up in system properties
        int batteryLevel = CameraUtil.getLowBatteryNoFlashLevel();
        String beforeMode = mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE + BEFORE_LOW_BATTERY);
        String currentMode = mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE);
        if (level <= batteryLevel) {
            // step 1. save current value: on, off, torch
            if (TextUtils.isEmpty(beforeMode)) {
                mDataModuleCurrent.set(Keys.KEY_FLASH_MODE + BEFORE_LOW_BATTERY, currentMode);
            }
            // step 2. set flash mode off and write into sp
            /*
             * SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
            mDataModuleCurrent.set(Keys.KEY_FLASH_MODE, "off");
             */
            // step 3. set batterylow flag

            // step 4. if flash is on, turn off the flash
            /* Fix Bug631122 this setting may be applied during capturing which cause error
             */
            if (!"off".equals(currentMode) && mCameraSettings != null) {
                if (!isPhotoFocusing()) {
                    updateParametersFlashMode();
                }
            }
            // step 5. set button disabled and show toast to users
            mAppController.getButtonManager().disableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
            //SPRD: add for bug 908294
            if(isFirstShowToast || (!isFirstShowToast && isBatteryAgainLowLevel)){
                if(isFirstShowToast == false){
                    isBatteryAgainLowLevel = false;
                }
                isFirstShowToast = false;
                //Sprd:fix bug932845
                CameraUtil.toastHint(mActivity,R.string.battery_level_low);
            }
        } else {
            isBatteryAgainLowLevel = true;
            // never lower than limen
            if (TextUtils.isEmpty(beforeMode)) {
                return;
            }
            // step 1.set before state value to current value
            /*
             * SPRD: Fix bug 631061 flash may be in error state when receive lowbattery broadcast
            mDataModuleCurrent.set(Keys.KEY_FLASH_MODE, beforeMode);
             */
            // step 2.set before state value null
            mDataModuleCurrent.set(Keys.KEY_FLASH_MODE + BEFORE_LOW_BATTERY, null);
            // step 3.set button disabled or enabled and set BatteryLow flag
            mAppController.getButtonManager().enableButton(ButtonManagerDream.BUTTON_FLASH_DREAM);
            // step 4.according to before state value turn on flash
            // SPRD: Fix bug 580978 HDR and flash is effective at the same time
            boolean hdrState = mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR);
            // if (!"off".equals(beforeMode) && !hdrState
            if (!hdrState && mCameraSettings != null) {
                // open video flash
                updateParametersFlashMode();
            }
        }
        //Fix Bug631122 this setting may be applied during capturing which cause error
        if (mCameraDevice != null) {
            if (!isPhotoFocusing()) {
                mCameraDevice.applySettings(mCameraSettings);
            }
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    protected void initData(byte[] jpegData, String title, long date,
            int width, int height, int orientation, ExifInterface exif,
            Location location,
            MediaSaver.OnMediaSavedListener onMediaSavedListener) {
    }
    /* @} */
    //Bug#533869 add the feature of volume
    protected int getVolumeControlStatus(CameraActivity mActivity) {
        return mActivity.getVolumeControlStatus();
    }

    /* SPRD: Fix bug 592600, InterValPhotoModule Thumbnail display abnormal. @{ */
    public void updateIntervalThumbnail(final Bitmap indicator){}
    /* @} */

    protected int mBurstCaptureType = -1;

    public boolean isFreezeFrameDisplayShow() {
        return false;
    }

    /*
     * fix bug 601158 thumbnail does not generate and display
     */
    public boolean isBurstThumbnailNotInvalid() {
        return mCancelBurst && !mThumbnailHasInvalid;
    }
    public void restoreCancelBurstTag() {
        mCancelBurst = false;
        mThumbnailHasInvalid = false;
    }

    protected MakeupController mMakeupController;
    protected LightPortraitControllerDream mLightPortraitControllerDream;

    @Override
    public void setMakeUpController(MakeupController makeUpController) {
        mMakeupController = makeUpController;
    }

    @Override
    public void setLightPortraitController(LightPortraitControllerDream controller) {
        mLightPortraitControllerDream = controller;
    }

    @Override
    public void onLightPortraitEnable(boolean enable) {
//        mCameraDevice.setFaceDetectionCallback(mHandler, mUI);
//        mCameraDevice.startFaceDetection();
//        mFaceDetectionStarted = true;
//        mUI.onStartFaceDetection(mDisplayOrientation, isCameraFrontFacing());

        if(lastLightPortraitEnable == enable){
            return;
        } else {
            lastLightPortraitEnable = enable;
        }

        if(enable){
            mCameraDevice.setLightPortraitCallback(mHandler,mUI);
        } else {
            mCameraDevice.setLightPortraitCallback(null,null);
        }

        mUI.onLightPortraitEnable(enable);
    }


    public void showLightPortraitOverLay() {
        int lightPortraitValue = mDataModuleCurrent.getInt(Keys.KEY_LIGHT_PORTIAIT);
        mUI.showLightPortraitOverLay(LightPortraitControllerDream.CLASSIC_VALUE.equals("" + lightPortraitValue)
                || LightPortraitControllerDream.STAGE_VALUE.equals("" + lightPortraitValue));
    }

    public boolean isBeautyCanBeUsed() {
        return CameraUtil.isCameraBeautyEnable()
                && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_BEAUTY_ENTERED)
                && !isImageCaptureIntent();
    }

    /*start :add for w+t mode
    * back camera
    * size less than 8m
    * auto photo module
    * */

    public void reRequestCameraOpen(int cameraId){
        Log.d(TAG,"reRequestCameraOpen old camera id = " + mCameraId + " new camera id = " + cameraId);
        if (mCameraId == cameraId){
            restartPreview(true);
        } else {
            closeCamera();
            doCameraOpen(cameraId);
            //reCreate surfaceview after close camera(it will release surface )
            if (isUseSurfaceView()) {
                mActivity.setSurfaceVisibility(false);
                mActivity.setSurfaceHolderListener(mUI);
                mActivity.setSurfaceVisibility(true);
            }
            mCameraId = cameraId;
        }
    }

    /*end : add for w+t mode*/

    // Bug 1018708 - hide or show BeautyButton on topPanel
    public void startAiBeauty(boolean flag) {
        // do not sent parameter if ASD has been TURNED OFF already
        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_AI_SCENE_DATECT))
            mMakeupController.startBeautyWithoutController(flag);
    }

    @Override
    public void onFeatureOperationEnableChange(boolean enable){
        DataModuleManager.getInstance(mActivity)
                .getCurrentDataModule().changeSettings(Keys.KEY_CAMERA_BEAUTY_ENTERED,enable);
    }

    @Override
    public void updateMakeLevel() {
        /* SPRD: fix bug 474672 add for beauty @{ */

        Log.d(TAG,
                "updateMakeLevel "
                        + DataModuleManager.getInstance(mActivity)
                                .getCurrentDataModule()
                                .getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED));
        // mMakeupSeekBar != null means this module contains makeupToggleButton
        // in top panel
        if (isBeautyCanBeUsed()
                && DataModuleManager.getInstance(mActivity)
                        .getCurrentDataModule()
                        .isEnableSettingConfig(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
            if (mMakeupController == null) {
                return;
            }

            // 1018708 ， do not show makeup controller while AI Scene Detection
            if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_AI_SCENE_DATECT)) {
                if (mAiSceneType == 2)
                    mMakeupController.startBeautyWithoutController(true);
                    return; // (Beauty ON , ASD ON , PAUSE-RESUME)
            }

            onBeautyValueChanged(mMakeupController.getValue(Keys.KEY_CAMERA_BEAUTY_ENTERED));
        }
        /* @} */
    }
    @Override
    public boolean isPhotoFocusing(){
        return mCameraState == FOCUSING || mCameraState == SNAPSHOT_IN_PROGRESS;
    }

    /* SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{ */
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height,
            int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l) {
        getServices().getMediaSaver().addImage(
                data, title, date, loc, width, height, orientation, exif, l);
    }
    /* @} */

    public void updateImage(Uri uri, byte[] data, String title, long date, Location loc, int width, int height,
            int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l, String mimeType) {
        if (uri != null) {
            /* SPRD:fix bug761111 add for oom @ */
            if (PHOTO_ERROR_URI.equals(uri.toString())) {
                return;
            }
            getServices().getMediaSaver().updateImage(uri,
                    data, title, date, loc, width, height, orientation, exif, l, mimeType, null);
        } else {
            if (isNeedThumbCallBack() && mActivity != null) {
                mActivity.putUpdateRun(title, new UpdateRunnbale(data, title, date, loc, width,
                        height, orientation, mimeType, exif, l));
            }
        }
    }
    /* SPRD: Fix 597206 Adds new features 3DPhoto @{ */
    protected String getNameTitle(String title) {
        return title;
    }
    /* @} */
    public boolean shouldFollowOriginalSize() {
        return false;
    }
    public void switchPreview(){}

    protected boolean mCameraAvailable = false;

    @Override
    public boolean isCameraAvailable(){
        return mCameraAvailable;
    }

    @Override
    public boolean useNewApi() {
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(null);
    }

    private int mSensorSelfShotValue = 0;
    public void forceOnSensorSelfShot(){
        if (mSensorSelfShotValue == 0){
            Log.i(TAG, "mSensorSelfShotValue has not been set");
            return;
        }
        onSensorSelfShot(true, mSensorSelfShotValue);
    }
    @Override
    public void onSensorSelfShot(boolean bool, int value) {
        if (mActivity.isFilmstripCoversPreview()) {
            return;
        }
        mSensorSelfShotValue = value;
        int blurRefocusIndex = SettingsScopeNamespaces.REFOCUS;

        int frontBlurRefocusIndex = SettingsScopeNamespaces.FRONT_BLUR;

        int currentIndex = mActivity.getCurrentModuleIndex();
        Log.i(TAG, "SprdCaptureResult onSensorSelfShot bool = " + bool + "value = " + value);

        if (refreshBlurUITip(currentIndex,blurRefocusIndex,bool,value))
            return;
        if (refreshFrontBlurUITip(currentIndex,frontBlurRefocusIndex,bool,value))
            return;

        if (!isSupportSensoSelfShotModule()) {
            return;
        }
        if (mDataModuleCurrent != null && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_SENSOR_SELF_SHOT)) {
            Log.i(TAG, "bool = " + bool + ", " +
                    "canShutter = " + canShutter() + ", " +
                    "mCanSensorSelfShot = " + mCanSensorSelfShot
            );
            if (bool && canShutter() && mCanSensorSelfShot) {
                mCanSensorSelfShot = false;
                onShutterButtonClick();
                mCaptureCount = 0;
            }
        }
    }

    private boolean refreshBlurUITip(int currentIndex, int blurRefocusIndex,boolean bool, int value){
        boolean result = false;
        if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,2)) {
            CameraUtil.toastHint(mActivity,R.string.blur_refocus_covered);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,50)) {
            //can take refocus picture
            mAppController.getCameraAppUI().setBlurEffectHighlight(true);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,51)) {
            //object is too far when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_move_closer_tip);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,52)) {
            //object is too close when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_move_further_tip);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,53)) {
            //object is too dark when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_need_more_light_tip);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,54)) {
            //object is too dark when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.GONE,
                    R.string.refocus_move_further_tip);
            result = true;
        } else if (checkBlurUITipType(currentIndex,blurRefocusIndex,bool,value,6)) {
            mIsBlurRefocusPhoto = false;
            CameraUtil.toastHint(mActivity,R.string.blur_refocus_unsupported);
            result = true;
        }
        return result;
    }

    private boolean checkBlurUITipType(int currentIndex,int blurRefocusIndex,boolean bool,int value,int type){
        return currentIndex == blurRefocusIndex
                && CameraUtil.getRefocusModeSupport().equals("blurRefocus") && bool
                && value == type;
    }

    private boolean checkFrontBlurUITipType(int currentIndex,int frontBlurRefocusIndex,boolean checkRealDual,boolean bool,int value,int type){
        return currentIndex == frontBlurRefocusIndex
                && (checkRealDual?isFrontRealDualCamVersion():true) && bool
                && value == type;
    }

    private boolean refreshFrontBlurUITip(int currentIndex,int frontBlurRefocusIndex,boolean bool,int value){
        boolean result = false;
        if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,false,bool,value,6)) {
            mIsBlurRefocusPhoto = false;
            CameraUtil.toastHint(mActivity,R.string.blur_refocus_unsupported);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,false,bool,value,2)) {
            CameraUtil.toastHint(mActivity,R.string.blur_refocus_covered);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,true,bool,value,50)) {
            //can take refocus picture
            mAppController.getCameraAppUI().setBlurEffectHighlight(true);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,true,bool,value,51)) {
            //object is too far when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_move_closer_tip);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,true,bool,value,52)) {
            //object is too close when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_move_further_tip);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,true,bool,value,53)) {
            //object is too dark when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.VISIBLE,
                    R.string.refocus_need_more_light_tip);
            result = true;
        } else if (checkFrontBlurUITipType(currentIndex,frontBlurRefocusIndex,true,bool,value,54)) {
            //object is too dark when preview boken
            mAppController.getCameraAppUI().setBlurEffectHighlight(false);
            mAppController.getCameraAppUI().setCurrentModuleAddSecondTip(View.GONE,
                    R.string.refocus_move_further_tip);
            result = true;
        }
        return result;
    }

    /* SPRD: Fix bug 659315,572631 optimize camera launch time @{ */
    private boolean mSoundInitialized = false;

    public void onSurfaceTextureUpdated() {
        mActivity.onSurfaceTextureUpdate();
        if (!mSoundInitialized) {
            mSoundInitialized = true;
            AsyncTask.execute(new Runnable() {// use SerialExecutor to sync
                @Override
                public void run() {
                    Log.i(TAG, "SoundInitialized start");
                    if (mCountdownSoundPlayer == null) {
                        mCountdownSoundPlayer = new SoundPlayer(mAppController.getAndroidContext());

                        mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
                        mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
                    }

                    if (mCameraSound == null) {
                        mCameraSound = new MediaActionSound();
                        // Not required, but reduces latency when playback is requested later
                        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
                    }
                }
            });
        }
    }
    /* @} */

    @Override
    public void updateTopPanel(){
        if (!mCameraDevice.getCapabilities().supports(CameraCapabilities.SceneMode.HDR)) {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_HDR_DREAM,View.GONE);
        } else {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_HDR_DREAM,View.VISIBLE);
        }

        if(CameraUtil.isIsMotionPhotoEnabled()){
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_MONTIONPHOTO_DREAM,View.VISIBLE);
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_HDR_DREAM,View.GONE);
        } else {
            mUI.setButtonVisibility(ButtonManagerDream.BUTTON_MONTIONPHOTO_DREAM,View.GONE);
        }

        if(isUpdateFlashTopButton()){
            updateFlashTopButton();
            if(CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing())){
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateBatteryLevel(mActivity.getCurrentBattery());
                    }
                });
            }
        }
        updateRefocusTopButton();
        updateFilterTopButton();

        ButtonManager buttonManager = mActivity.getButtonManager();
        buttonManager.enableCameraButton();
    }

    protected void updateFilterTopButton() {

    }

    protected boolean isUpdateFlashTopButton(){
        return true;
    }

    private View mCancelButton;
    private void initCancelButton() {
        if (mCancelButton == null){
            mCancelButton = mActivity.findViewById(R.id.shutter_cancel_button);
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // SPRD: Fix bug 537444 if paused, ignore this event
                    if (mPaused) return;

                    cancelCountDown();
                }
            });
        }
    }
    private void performInitAsync(){
        DataStructSetting dataSetting = new DataStructSetting(
                DreamUtil.intToString(getMode()),
                DreamUtil.isFrontCamera(mAppController.getAndroidContext(), mCameraId),
                mActivity.getCurrentModuleIndex(), mCameraId);
        DataModuleManager.getInstance(mAppController.getAndroidContext())
                .changeModuleStatus(dataSetting);

        mDataModuleCurrent = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getCurrentDataModule();
        mCounterInitDataSettingWait.count();
    }

    private Counter mCounterInitDataSettingWait = new Counter(1);
    public void waitInitDataSettingCounter (){
        mCounterInitDataSettingWait.waitCount();
    }

    @Override
    public boolean getCameraPreviewState() {
        return mPreviewing;
    }

    private boolean isHdrNormalOn() {
        return CameraUtil.isNormalHdrEnabled() && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC) && (isHdr()||isAutoHdr() && isHdrScene);
    }

    private boolean saveHdrPicture(){
        if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && !mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 1 || isHdrPicture)){
            return true;
        } else if (mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_HDR) == 1 || isHdrPicture)
                && !mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR_NORMAL_PIC)){
            return true;
        }
        return false;
    }
    public boolean isHdrPicture(){
        return isAutoHdr() && isHdrScene;
    }
    @Override
    public boolean isEnableDetector(){
        return mUI == null ? false : mUI.isEnableDetector();
    }

    /**
     * eg.
    if (!isNeedThumbCallBack()) {
        setupPreview();
    } else {
        if (isHdrNormalOn()) {
            if (!mIsHdrPicture) {
                setupPreview();
            }
        } else if (!mIsFirstCallback) {
            setupPreview();
        }
    }
    */
    private boolean isLastCallback() {
        if (!isNeedThumbCallBack() && (!isHdrNormalOn() || isHdrNormalOn() && !mIsFirstCallback) || isNeedThumbCallBack() && (isHdrNormalOn() && !mIsHdrPicture || !isHdrNormalOn() && !mIsFirstCallback)) {
            return true;
        }
        return false;
    }

    protected boolean isNoBokehPicture(Integer val) {
        if (val != null && (val == TYPE_MODE_BLUR || val == TYPE_MODE_BOKEH )) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isBokehPicture(Integer val) {
        if (val != null && (val == TYPE_MODE_BLUR_BOKEH || val == TYPE_MODE_BOKEH_BOKEH)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isNeedThumbCallBack() {
        return mCameraSettings == null ? false : mCameraSettings.getNeedThumbCallBack();
    }

    public boolean isBackRealDualCamVersion() {
        int backRefocusVersion = CameraUtil.getCurrentBackBlurRefocusVersion();
        return  (backRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_6
                || backRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_7
                || backRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_8
                || backRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_9);
    }

    public boolean isFrontRealDualCamVersion() {
        int forntRefocusVersion = CameraUtil.getCurrentFrontBlurRefocusVersion();
        return (forntRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_6
                || forntRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_7
                || forntRefocusVersion == CameraUtil.BLUR_REFOCUS_VERSION_8);
    }

    public void updateFlashLevelUI() {
        if (mDataModuleCurrent == null){
            return;
        }
        if (!mActivity.getIsButteryLow()
                && "torch".equals(mDataModuleCurrent.getString(Keys.KEY_FLASH_MODE))
                && CameraUtil.isFlashLevelSupported(mCameraCapabilities)) {
            mUI.showAdjustFlashPanel();
        }else{
            mUI.hideAdjustFlashPanel();
        }
    }
    public boolean isFlashSupported(){
        if (mCameraCapabilities != null){
            return CameraUtil.isFlashSupported(mCameraCapabilities, isCameraFrontFacing());
        } else
            return false;
    }

    public void updateFilterType() {}
    public void updatePortraitBackgroundReplacementType() {}

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
        return DreamUtil.PHOTO_MODE;
    }

    @Override
    public boolean isSupportTouchAFAE(){
        return true;
    }

    @Override
    public boolean isSupportManualMetering(){
        return false;
    }

    // SPRD:Fix bug 948896 add for ai scene detect enable
    protected void updateParametersAiSceneDetect() {}

    // Bug 1018708 - hide or show BeautyButton on topPanel
    protected void updateBeautyButton() {}

    /* SPRD:fix bug947586 face blur cost much time @{ */
    private boolean mBlurHint = false;
    @Override
    public void onBlurCapture() {
        if (mCameraDevice != null) {
            mCameraDevice.setBlurCaptureCallback(null, null);
        }
        if (mUI != null) {
            mBlurHint = true;
            mUI.showBlurScreenHint();
        }
    }
    /* @} */

    /* SPRD:fix bug1172230 add flag for smile capture */
    public void updateParametersSmileCapture(){
        mCameraSettings.setSmileCapture(0);
    }
    /* @} */

    public void updateParametersAutoChasingRegion(){
        if (!mAutoChasingSupported){
            return;
        }
        boolean autoChasingOpen = mDataModuleCurrent
                .getBoolean(Keys.KEY_AUTO_TRACKING);
        if (autoChasingOpen) {
            mCameraSettings.setAutoChasingEnable(1);
        } else {
            mCameraSettings.setAutoChasingEnable(0);
        }
        mFocusManager.setAutoChasingEnable(autoChasingOpen);
        mFocusManager.cancelAutoChasing();
    }

    public void updateAuto3DnrPatameter(){
        Log.i(TAG, "updateAuto3DnrPatameter isAuto3DnrSupported= " + isAuto3DnrSupported());
        //SPRD:Fix bug 1166737 delete auto3dnr when 4in1sersor supported
        if(!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_AUTO_3DNR_PARAMETER)){
            return;
        }

        if(isAuto3DnrSupported() && mCameraDevice != null){
            if (mDataModuleCurrent.getBoolean(Keys.KEY_AUTO_3DNR_PARAMETER)) {
                mCameraSettings.setAuto3DnrEnable(1);
                mCameraDevice.setAuto3DnrSceneDetectionCallback(mHandler, this);
                mCameraDevice.setAuto3DnrSceneDetectionWork(true);
            } else {
                mCameraSettings.setAuto3DnrEnable(0);
                mCameraDevice.setAuto3DnrSceneDetectionWork(false);
            }
        }
    }
}
