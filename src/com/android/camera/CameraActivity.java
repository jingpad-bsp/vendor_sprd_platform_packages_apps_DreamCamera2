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

import android.Manifest;
import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.CameraPerformanceTracker;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.CameraController;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.CameraServices;
import com.android.camera.app.CameraServicesImpl;
//import com.android.camera.app.FirstRunDialog;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MemoryManager;
import com.android.camera.app.ModuleManager;
import com.android.camera.app.ModuleManagerImpl;
import com.android.camera.app.MotionManager;
import com.android.camera.app.OrientationManager;
import com.android.camera.app.OrientationManagerImpl;
import com.android.camera.data.*;
import com.android.camera.data.LocalFilmstripDataAdapter.FilmstripItemListener;
import com.android.camera.debug.Log;
import com.android.camera.device.ActiveCameraDeviceTracker;
import com.android.camera.device.CameraId;
import com.android.camera.exif.ExifInterface;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera.module.ModuleController;
import com.android.camera.module.ModulesInfo;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.OneCameraModule;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.camera.settings.Keys;
import com.android.camera.settings.ResolutionSetting;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.stats.profiler.Profile;
import com.android.camera.stats.profiler.Profiler;
import com.android.camera.stats.profiler.Profilers;
import com.android.camera.superresolution.SuperResolutionNative;
import com.android.camera.tinyplanet.TinyPlanetFragment;
import com.android.camera.ui.DetailsDialog;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.ModeListView.ModeListVisibilityChangedListener;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Callback;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GalleryHelper;
import com.android.camera.util.IntentHelper;
import com.android.camera.util.QuickActivity;
import com.android.camera.util.Size;
import com.android.camera.widget.FilmstripView;
import com.android.camera.widget.Preloader;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamModule;
import com.dream.camera.modules.AudioPicture.AudioPictureModule;
import com.dream.camera.modules.qr.ReuseModule;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleCamera;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.settings.DataStructSetting;
import com.dream.camera.ucam.utils.Utils;
import com.dream.camera.util.Counter;
import com.dream.camera.util.DreamUtil;
import com.google.common.base.Optional;
import com.google.common.logging.eventprotos.ForegroundEvent.ForegroundSource;
import com.google.common.logging.eventprotos.NavigationChange;
import com.sprd.camera.storagepath.MultiStorage;
import com.sprd.camera.storagepath.StorageUtil;
import com.sprd.camera.storagepath.StorageUtilProxy;
import com.sprd.gallery3d.aidl.IFloatWindowController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.dream.camera.SlidePanelManager;

import android.os.SystemProperties;

import com.dream.camera.settings.DataModulePhoto;

import android.view.SurfaceHolder;
import android.app.ActivityManagerNative;
import com.android.camera.CameraModule.UpdateRunnbale;
import com.dream.camera.modules.filter.FilterModuleAbs;
import android.provider.DocumentsContract;
import android.content.DialogInterface;

public class CameraActivity extends QuickActivity implements AppController,
        CameraAgent.CameraOpenCallback, ResetListener,
        DreamSettingChangeListener/*, DreamVoiceImageOnclickListener */{

    private static final Log.Tag TAG = new Log.Tag("CameraActivity Dream ");

    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE =
            "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE =
            "android.media.action.IMAGE_CAPTURE_SECURE";

    // The intent extra for camera from secure lock screen. True if the gallery
    // should only show newly captured pictures. sSecureAlbumId does not
    // increment. This is used when switching between camera, camcorder, and
    // panorama. If the extra is not set, it is in the normal camera mode.
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";
    public static final String GOOGLE_PHOTOS = "com.google.android.apps.photos";
    public static final String GOOGLE_PHOTOS_GO = "com.google.android.apps.photosgo";

    private static final String EXTRA_FRONT_CAMERA = "android.intent.extra.USE_FRONT_CAMERA";
    private static final String INTENT_EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";
    private static final String REF_VALUE = "android-app://com.google.android.googlequicksearchbox/https/www.google.com";

    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 2;
    private static final long SCREEN_DELAY_MS = 150 * 1000; // 2.5 mins.
    /*
     *SPRD Feature:627281
     */
    private static long mDestoryDelayTimes = 0;
    private static final int MSG_DESTORY_CAMER = 3;
    /** Load metadata for 10 items ahead of our current. */
    private static final int FILMSTRIP_PRELOAD_AHEAD_ITEMS = 10;
    private static final int PERMISSIONS_ACTIVITY_REQUEST_CODE = 1;
    private static final int PERMISSIONS_RESULT_CODE_OK = 1;
    private static final int PERMISSIONS_RESULT_CODE_FAILED = 2;
    private static final int NO_DEVICE = -1;
    private static final int SCOPE_REQUEST_CODE_SD = 11;
    private static final int SCOPE_REQUEST_CODE_OTG = 12;
    private int mRequestCode;
    private String mUsbKey;
    private boolean mIsUsedGooglePhotos = false;
    private boolean mIsUsedGooglePhotosGo = false;
    protected boolean mOpenCameraOnly = true;
    protected int mTimeDurationS = 3;
    protected boolean mVoiceIntent = false;
    /** Should be used wherever a context is needed. */
    private Context mAppContext;

    /**
     * Camera fatal error handling: 1) Present error dialog to guide users to exit the app. 2) If
     * users hit home button, onPause should just call finish() to exit the app.
     */
    private boolean mCameraFatalError = false;

    /**
     * Whether onResume should reset the view to the preview.
     */
    private boolean mResetToPreviewOnResume = true;

    /**
     * This data adapter is used by FilmStripView.
     */
    private VideoItemFactory mVideoItemFactory;
    private PhotoItemFactory mPhotoItemFactory;
    protected LocalFilmstripDataAdapter mDataAdapter;

    private ActiveCameraDeviceTracker mActiveCameraDeviceTracker;
    private OneCameraManager mOneCameraManager;
    private DataModuleManager mDataModuleManager;
    private ResolutionSetting mResolutionSetting;
    private ModeListView mModeListView;
    /* SPRD: Fix bug 659315, optimize camera launch time @{ */
    private ModeListView.ModeListViewHelper mModeListViewHelper;
    private boolean mModeListVisible = false;
    private int mCurrentModeIndex = -1;
    private int mNextModuleIndex = -1;
    private CameraModule mCurrentModule;
    private ModuleManagerImpl mModuleManager;
    private FrameLayout mAboveFilmstripControlLayout;
    private FilmstripController mFilmstripController;
    private boolean mFilmstripVisible;
    /** Whether the filmstrip fully covers the preview. */
    private boolean mFilmstripCoversPreview = false;
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    private OnScreenHint mStorageHint;
    private final Object mStorageSpaceLock = new Object();
    private long mStorageSpaceBytes = Storage.LOW_STORAGE_THRESHOLD_BYTES;
    private long mInternalSpaceBytes = Storage.LOW_STORAGE_THRESHOLD_BYTES;
    private boolean mAutoRotateScreen;
    private boolean mSecureCamera;
    private int mSecureCameraModeIndex = SettingsScopeNamespaces.AUTO_PHOTO; //SPRD: bugfix 1001985
    private long mSecureEnterTime = -1L;
    private OrientationManagerImpl mOrientationManager;
    private LocationManager mLocationManager;
    private ButtonManager mButtonManager;
    private Handler mMainHandler;
//    private PanoramaViewHelper mPanoramaViewHelper;
    private ActionBar mActionBar;
    private boolean mIsActivityRunning = false;
    private FatalErrorHandler mFatalErrorHandler;
    private boolean mHasCriticalPermissions;
    private static boolean mFlagForResumeFeaturelist = false;
    private boolean isSupportGps;// SPRD: fix for bug 499642 delete location save function
    private boolean mResumed;
    private boolean mSuccess = false;
    private int mFilmStripCount;// SPRD:fix bug520618 Video is still appear if it is deleted from
                                // fileManager

    private final Uri[] mNfcPushUris = new Uri[1];

    private FilmstripContentObserver mLocalImagesObserver;
    private FilmstripContentObserver mLocalVideosObserver;

    private boolean mPendingDeletion = false;

    private CameraController mCameraController;
    private boolean mPaused;
    private CameraAppUI mCameraAppUI;

    private Intent mGalleryIntent;
    private long mOnCreateTime;

    private Menu mActionBarMenu;
    private Preloader<Integer, AsyncTask> mPreloader;

    /** Can be used to play custom sounds. */
    private SoundPlayer mSoundPlayer;

    // Add for dream settings.
    private DataModuleBasic mDataModule;

    /* SPRD:fix bug524433 Filmstripview is not refresh when sdcard removed */
    private MyBroadcastReceiver mReceiver;
    /* SPRD:fix bug641569 the storage path was changed after restart mobile */
    private MyBroadcastReceiverShutDown mReceiverShutDown;
    // SPRD:fix bug599645 VideoModule recivedBroadcast later than CameraActivity, cause be killed
    private BroadcastReceiver mModuleMediaBroadcastReceiver;

    private BatteryBroadcastReciver mBatteryReceiver;

    // SPRD: Fix 474843 Add for Filter Feature
    private int mCameraId = 0;
    private static long mLastReceiveEventTime = 0;

    /* SPRD:Fix bug 597486 Can not slide around after taking a picture @{ */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
    public static final Executor THREAD_POOL_EXECUTOR
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    /* @} */

    private static final int LIGHTS_OUT_DELAY_MS = 4000;
    private final int BASE_SYS_UI_VISIBILITY =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    private final Runnable mLightsOutRunnable = new Runnable() {
        @Override
        public void run() {
            getWindow().getDecorView().setSystemUiVisibility(
                    BASE_SYS_UI_VISIBILITY | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    };
    private MemoryManager mMemoryManager;
    private MotionManager mMotionManager;

    /* SPRD: Fix bug 605818, wait for background camera closed @{ */
    private static final int MSG_CAMERA_REQUEST_PENDING = 3;
    private static final int CAMERA_REQUEST_PENDING_TIME = 5000;
    private ConcurrentHashMap<String, Boolean> mCameraAvailableMap;
    private boolean mIsCameraRequestedOnCreate = false;
    private Handler mCameraRequestHandler;
    private CameraManager mCameraManager;
    // SPRD: Fix bug 656429, record FilmstripItems loading start time for debug
    private long mFilmstripLoadStartTime = 0;
    public int nextCameraId = 0;

    /*
     SPRD: Fix bug 911616. add flag for battery low.
     make module can get real battery status before receive battery changed broadcast
    */
    private boolean mIsBatteryLow;
    public boolean getIsButteryLow() {
        return mIsBatteryLow;
    }

    protected ArrayList<Long> securePhotoList = null;

    private Runnable mResumeRunnable = new Runnable() {
        public void run() {
            if (!mPaused && mCurrentModule != null) {
                Log.i(TAG, "mResumeRunnable doing");
                mCurrentModule.resume();
            }
        }
    };
    private CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {

        @Override
        public void onCameraAvailable(String cameraId) {
            if (mCameraAvailableMap != null)
                mCameraAvailableMap.put(cameraId, true);

            boolean requestPending = mCameraRequestHandler.hasMessages(MSG_CAMERA_REQUEST_PENDING);
            Log.i(TAG, "onCameraAvailable requestPending="+requestPending);
            if (requestPending
                    && checkAllCameraAvailable()
                    && !mPaused && mCurrentModule != null) {

                Log.i(TAG, "camera is available, resume mode now");
                mCameraRequestHandler.removeMessages(MSG_CAMERA_REQUEST_PENDING);

                mMainHandler.post(mResumeRunnable);
            }
        }

        public void onCameraUnavailable(String cameraId) {
            Log.i(TAG, "onCameraUnavailable");
            if (mCameraAvailableMap != null)
                mCameraAvailableMap.put(cameraId, false);

            checkAllCameraAvailable();
        }
    };
    /* @} */

    /* SPRD: add for mutex between float window and camera @{ */
    private boolean mServiceBinded = false;
    private ServiceConnection mIFloatWindowServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            // TODO Auto-generated method stub
            IFloatWindowController iFloatWindowController = IFloatWindowController.Stub
                    .asInterface(arg1);

            try {
                if (iFloatWindowController.closeFloatWindow()) {
                    CameraUtil.toastHint(CameraActivity.this,R.string.close_float_video);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
    /* @} */

    private final Profiler mProfiler = Profilers.instance().guard();

    /** First run dialog */
    //private FirstRunDialog mFirstRunDialog;

    private Dialog detailDialog = null;

    @Override
    public CameraAppUI getCameraAppUI() {
        return mCameraAppUI;
    }

    public OneCameraManager getOneCameraManger() {
        return mOneCameraManager;
    }

    @Override
    public ModuleManager getModuleManager() {
        return mModuleManager;
    }

    /**
     * Close activity when secure app passes lock screen or screen turns off.
     */
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive intent="+intent);
            /* Sprd： fix bug880154/922977 secureCamera may not opened sometimes
            finish();
            */
            if (mSecurePaused) {
                finish();
            } else {
                Log.w(TAG, "Ignoring screen off when activity has resumed!");
            }
        }
    };

    /**
     * Whether the screen is kept turned on.
     */
    private boolean mKeepScreenOn;
    private int mLastLayoutOrientation;
/*
    private class CameraAvailRunnable implements Runnable {
        private CameraAgent.CameraProxy mCamera;

        public void setCamera(CameraAgent.CameraProxy camera) {
            mCamera = camera;
        }

        @Override
        public void run() {
            try {
                if (!mPaused && mCurrentModule != null) {
                    mCurrentModule.onCameraAvailable(mCamera);
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error connecting to camera", ex);
                mFatalErrorHandler.onCameraOpenFailure();
            }
        }
    }

    private CameraAvailRunnable mCameraAvailRunnable = new CameraAvailRunnable();*/

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        Log.i(TAG, "onCameraOpened, CameraProxy = " + camera);
        if (mPaused) {
            // We've paused, but just asynchronously opened the camera. Close it
            // because we should be releasing the camera when paused to allow
            // other apps to access it.
            Log.i(TAG, "received onCameraOpened but activity is paused, closing Camera");
            mCameraController.closeCamera(false);
            return;
        }

        if (!mModuleManager.getModuleAgent(mCurrentModeIndex).requestAppForCamera()) {
            // We shouldn't be here. Just close the camera and leave.
            mCameraController.closeCamera(false);
            throw new IllegalStateException("Camera opened but the module shouldn't be " +
                    "requesting");
        }
        if (mCurrentModule != null) {
            try {
                mCurrentModule.waitInitDataSettingCounter();
                mDataModuleManager.getCurrentDataModule().initializeStaticParams(camera);
                mCurrentModule.onCameraAvailable(camera);
            }  catch (RuntimeException ex) {
                Log.e(TAG, "Error connecting to camera", ex);
                mFatalErrorHandler.onCameraOpenFailure();
            }
        } else {
            Log.v(TAG, "mCurrentModule null, not invoking onCameraAvailable");
        }
        Log.v(TAG, "invoking onChangeCamera");
        if (mDataModule != null) {
            int cameraId = mDataModule.getIntDefault(Keys.KEY_CAMERA_ID);
            if (CameraUtil.getLowBatteryNoFlashLevel() > 0 && cameraId == 0) {
                IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                registerReceiver(mBatteryReceiver, batteryIntentFilter);
            }
        }
        getDreamHandler().post(new Runnable(){
            @Override
            public void run() {
                mCameraAppUI.onChangeCamera();
            }
        });
    }

    private void resetExposureCompensationToDefault(CameraAgent.CameraProxy camera) {
        // Reset the exposure compensation before handing the camera to module.
        CameraSettings cameraSettings = camera.getSettings();
        // SPRD: Fix bug 626721, fix NullPointerException about CameraSettings.setExposureCompensationIndex
        if(cameraSettings != null){
            cameraSettings.setExposureCompensationIndex(0);
            camera.applySettings(cameraSettings);
        }
    }

    @Override
    public void onCameraDisabled(int cameraId) {
        Log.w(TAG, "Camera disabled: " + cameraId);
        mMainHandler.post(new Runnable(){
            @Override
            public void run(){
                if (!mPaused) {
                    mFatalErrorHandler.onCameraDisabledFailure();
                }
            }
        });
    }

    @Override
    public void onDisconnected( int cameraId ){
        Log.w(TAG, "Camera onDisconnected: " + cameraId);

    }

    @Override
    public void onDeviceOpenFailure(int cameraId, String info) {
        Log.w(TAG, "Camera open failure: " + info);
        mMainHandler.post(new Runnable(){
            @Override
            public void run(){
                if (!mPaused) {
                    mFatalErrorHandler.onCameraOpenFailure();
                }
            }
        });
    }

    @Override
    public void onDeviceOpenedAlready(int cameraId, String info) {
        Log.w(TAG, "Camera open already: " + cameraId + "," + info);
        mMainHandler.post(new Runnable(){
            @Override
            public void run(){
                if (!mPaused) {
                    mFatalErrorHandler.onGenericCameraAccessFailure();
                }
            }
        });
    }

    @Override
    public void onReconnectionFailure(CameraAgent mgr, String info) {
        Log.w(TAG, "Camera reconnection failure:" + info);
        mMainHandler.post(new Runnable(){
            @Override
            public void run(){
                if (!mPaused) {
                    mFatalErrorHandler.onCameraReconnectFailure();
                }
            }
        });
    }


    private static class MainHandler extends Handler {
        final WeakReference<CameraActivity> mActivity;

        public MainHandler(CameraActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<CameraActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraActivity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case MSG_CLEAR_SCREEN_ON_FLAG: {
                    if (!activity.mPaused) {
                        activity.getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    break;
                }
                case MSG_DESTORY_CAMER: {
                    if (!CameraUtil.isIdleSleepEnable() || activity.mSecureCamera) {
                        return;
                    } else  if (activity.mCurrentModule instanceof VideoModule && activity.mCurrentModule.isVideoRecording()) {
                        activity.mLastReceiveEventTime = System.currentTimeMillis();
                        activity.mMainHandler.removeMessages(MSG_DESTORY_CAMER);
                        activity.mDestoryDelayTimes = activity.getDestoryDelayTimes();
                        activity.mMainHandler.sendEmptyMessageDelayed(MSG_DESTORY_CAMER, activity.mDestoryDelayTimes);
                        return;
                    }
                    if (!activity.mPaused) { // Bug 1151568 ,  2 time points maybe 1567172896462 and 1567172896461
                        Intent intent = new Intent("android.camera.action.IDLE_SLEEP");
                        activity.startActivity(intent);
                    }
                    break;
                }
            }
        }
    }

    private String fileNameFromAdapterAtIndex(int index) {
        final FilmstripItem filmstripItem = mDataAdapter.getItemAt(index);
        if (filmstripItem == null) {
            return "";
        }

        File localFile = new File(filmstripItem.getData().getFilePath());
        return localFile.getName();
    }

    private float fileAgeFromAdapterAtIndex(int index) {
        final FilmstripItem filmstripItem = mDataAdapter.getItemAt(index);
        if (filmstripItem == null) {
            return 0;
        }

        File localFile = new File(filmstripItem.getData().getFilePath());
        return 0.001f * (System.currentTimeMillis() - localFile.lastModified());
    }

    private int dataAdapterLengthBeforeShow;
    private long mLastPhotoId;

    private final FilmstripContentPanel.Listener mFilmstripListener =
            new FilmstripContentPanel.Listener() {

                @Override
                public void onDataReloaded() {
                    if (!mFilmstripVisible) {
                        return;
                    }
                    updateUiByData(mFilmstripController.getCurrentAdapterIndex());
                }

                @Override
                public void onDataUpdated(int adapterIndex) {
                    if (!mFilmstripVisible) {
                        return;
                    }
                    updateUiByData(mFilmstripController.getCurrentAdapterIndex());
                }

                @Override
                public void onDataFocusChanged(final int prevIndex, final int newIndex) {
                    if (!mFilmstripVisible) {
                        return;
                    }
                    // TODO: This callback is UI event callback, should always
                    // happen on UI thread. Find the reason for this
                    // runOnUiThread() and fix it.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUiByData(newIndex);
                        }
                    });
                }
            };

    private final FilmstripItemListener mFilmstripItemListener =
            new FilmstripItemListener() {
                @Override
                public void onMetadataUpdated(List<Integer> indexes) {
                    if (mPaused) {
                        // Callback after the activity is paused.
                        return;
                    }
                    int currentIndex = mFilmstripController.getCurrentAdapterIndex();
                    for (Integer index : indexes) {
                        if (index == currentIndex) {
                            updateUiByData(index);
                            // Currently we have only 1 data can be matched.
                            // No need to look for more, break.
                            break;
                        }
                    }
                }
            };

    private void setupNfcBeamPush() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mAppContext);
        if (adapter == null) {
            return;
        }

        if (!ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
            // Disable beaming
            adapter.setNdefPushMessage(null, CameraActivity.this);
            return;
        }

        adapter.setBeamPushUris(mNfcPushUris, CameraActivity.this);
        adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
            @Override
            public Uri[] createBeamUris(NfcEvent event) {
                return mNfcPushUris;
            }
        }, CameraActivity.this);
    }

    @Override
    public Context getAndroidContext() {
        return mAppContext;
    }

    @Override
    public Dialog createDialog() {
        return new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void launchActivityByIntent(Intent intent) {
        // Starting from L, we prefer not to start edit activity within camera's task.
        mResetToPreviewOnResume = false;
        // SPRD: Add For bug670083 after video playing, flash a preview frame.
        if (mCurrentModule.isUseSurfaceView() && intent.getType() != null
                && intent.getType().indexOf("video/") >= 0) {
            startActivity(intent);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        startActivity(intent);
    }

    @Override
    public int getCurrentModuleIndex() {
        return mCurrentModeIndex;
    }

    @Override
    public String getModuleScope() {
          return null;
    }

    @Override
    public ModuleController getCurrentModuleController() {
        return mCurrentModule;
    }

    @Override
    public int getQuickSwitchToModuleId(int currentModuleIndex) {
        return 0;
    }

    @Override
    public SurfaceTexture getPreviewBuffer() {
        // TODO: implement this
        return null;
    }

    @Override
    public void onPreviewReadyToStart() {
        mCameraAppUI.onPreviewReadyToStart();
    }

    @Override
    public void onPreviewStarted() {
        if (CameraUtil.isIdleSleepEnable()) {
            mLastReceiveEventTime = System.currentTimeMillis();
            mMainHandler.removeMessages(MSG_DESTORY_CAMER);
            mMainHandler.sendEmptyMessageDelayed(MSG_DESTORY_CAMER, mDestoryDelayTimes);
        }
        mCameraAppUI.onPreviewStarted();

        /* SPRD: Fix bug 572631, optimize camera launch time @{ */
        if (mExtraWorkNeeded && !mFilmstripVisible) { //SPRD: fix bug668989
            mExtraWorkNeeded = false;
            updateStorageSpaceAndHint(null);
            loadFilmstripItems();
        }
        if(mCurrentModule.getModuleTpye() != DreamModule.AUDIOPICTURE_MODULE||!mCurrentModule.isShutterClicked())
        getCameraAppUI().setBottomPanelLeftRightClickable(true);
        /* @} */
    }

    /* SPRD: Fix bug 659315, optimize camera launch time @{ */
    Runnable mModeListLoadTask = new Runnable() {
        @Override
        public void run() {
            /*
             * should be at front of mCameraAppUI.updateModeList()
             */
            mModeListViewHelper = new ModeListView.ModeListViewHelper(CameraActivity.this,
                    mModuleManager.getSupportedModeIndexList());
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mModeListView != null){
                        mModeListView.init(mModeListViewHelper);
                        mCameraAppUI.updateModeList();
                    }
                }
            });
            /* @} */
        }
    };

    public void onSurfaceTextureUpdate() {
        if(null != mModeListView && 0 == mModeListView.getModeListSize() && !mHasPostModeList) { // SPRD: Fix bug 811712
            Log.i(TAG , "now , modes = 0 , need to init by a ModeListViewHelper : " + mModeListViewHelper + " mHasPostModeList = " + mHasPostModeList);
            mHasPostModeList = true;//SPRD:fix bug820235
            mOnCreateAsyncHandler.post(mModeListLoadTask);
        }
        if (mHasDelayWorkOnCreate) {
            mHasDelayWorkOnCreate = false;
            /*
             * SPRD: Fix bug 658759 that blurBitmap() costs too much time
             */
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    long start = System.currentTimeMillis();
                    RenderScript rs = RenderScript.create(getAndroidContext());
                    if(rs != null) {
                        ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                    }
                    long cost = System.currentTimeMillis() - start;
                    if (cost > 100) {
                        Log.i(TAG, "ScriptIntrinsicBlur init cost: " + cost);
                    }
                }
            });
            /* @} */
        }
    }
    /* @} */

    @Override
    public void addPreviewAreaSizeChangedListener(
            PreviewStatusListener.PreviewAreaChangedListener listener) {
        mCameraAppUI.addPreviewAreaChangedListener(listener);
    }

    @Override
    public void removePreviewAreaSizeChangedListener(
            PreviewStatusListener.PreviewAreaChangedListener listener) {
        mCameraAppUI.removePreviewAreaChangedListener(listener);
    }

    @Override
    public void setupOneShotPreviewListener() {
        mCameraController.setOneShotPreviewCallback(mMainHandler,
                new CameraAgent.CameraPreviewDataCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, CameraAgent.CameraProxy camera) {
                        mCurrentModule.onPreviewInitialDataReceived();
                        mCameraAppUI.onNewPreviewFrame();
                    }
                }
                );
    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        mCameraAppUI.updatePreviewAspectRatio(aspectRatio);
    }

    @Override
    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {
        mCameraAppUI.updatePreviewTransformFullscreen(matrix, aspectRatio);
    }

    @Override
    public RectF getFullscreenRect() {
        return mCameraAppUI.getFullscreenRect();
    }

    @Override
    public void updatePreviewTransform(Matrix matrix) {
        mCameraAppUI.updatePreviewTransform(matrix);
    }

    @Override
    public void setPreviewStatusListener(PreviewStatusListener previewStatusListener) {
        mCameraAppUI.setPreviewStatusListener(previewStatusListener);
    }

    @Override
    public FrameLayout getModuleLayoutRoot() {
        return mCameraAppUI.getModuleRootView();
    }

    @Override
    public void setShutterEventsListener(ShutterEventsListener listener) {
        // TODO: implement this
    }

    @Override
    public void setShutterEnabled(boolean enabled) {
        mCameraAppUI.setShutterButtonEnabled(enabled);
    }

    @Override
    public boolean isShutterEnabled() {
        return mCameraAppUI.isShutterButtonEnabled();
    }

    @Override
    public void startFlashAnimation(boolean shortFlash) {
        mCameraAppUI.startFlashAnimation(shortFlash);
    }

    @Override
    public void cancelFlashAnimation() {
        mCameraAppUI.cancelFlashAnimation();
    }

    @Override
    public void startPreCaptureAnimation() {
        // TODO: implement this
    }

    @Override
    public void cancelPreCaptureAnimation() {
        // TODO: implement this
    }

    @Override
    public void startPostCaptureAnimation() {
        // TODO: implement this
    }

    @Override
    public void startPostCaptureAnimation(Bitmap thumbnail) {
        // TODO: implement this
    }

    @Override
    public void cancelPostCaptureAnimation() {
        // TODO: implement this
    }

    @Override
    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    @Override
    public void lockOrientation() {
        if (mOrientationManager != null) {
            mOrientationManager.lockOrientation();
        }
    }

    @Override
    public void unlockOrientation() {
        if (mOrientationManager != null) {
            mOrientationManager.unlockOrientation();
        }
    }

    /**
     * If not in filmstrip, this shows the capture indicator.
     */
    private void indicateCapture(final Bitmap indicator, final int rotationDegrees) {
//        if (mFilmstripVisible) {
//            return;
//        }

        /*mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mCameraAppUI.onThumbnail(indicator);
            }
        });

        // Don't show capture indicator in Photo Sphere.
        // TODO: Don't reach into resources to figure out the current mode.
        final int photosphereModuleId = getApplicationContext().getResources().getInteger(
                R.integer.camera_mode_photosphere);
        if (mCurrentModeIndex == photosphereModuleId) {
            return;
        }*/
        Log.i(TAG, "indicateCapture call");
        if(mainUiThumbnailRunnable != null && mMainHandler != null){
            mMainHandler.removeCallbacks(mainUiThumbnailRunnable);
        }

        mainUiThumbnailRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "indicateCapture");
                mCameraAppUI.startCaptureIndicatorRevealAnimation(mCurrentModule
                        .getPeekAccessibilityString());
                mCameraAppUI.updateCaptureIndicatorThumbnail(indicator, rotationDegrees);
            }
        };
        if (Looper.getMainLooper() != Looper.myLooper() && mMainHandler != null) {
            //SPRD：Fix bug927252
            mCameraAppUI.getCounterInitRoundedThumbnailView().waitCount();
            mMainHandler.post(mainUiThumbnailRunnable);
        } else {
            mainUiThumbnailRunnable.run();
        }
    }

    @Override
    /**
     * SPRD: fix bug438428
    public void notifyNewMedia(Uri uri) {
     */
    public void notifyNewMedia(final Uri uri) {
        // TODO: This method is running on the main thread. Also we should get
        // rid of that AsyncTask.
        if (mCurrentModule != null && mCurrentModule.getModuleTpye() == DreamModule.CONTINUE_MODULE
                && mCurrentModule.getBurstHasCaptureCount() < 2) {
            updateStorageSpaceAndHint(null);
        }
        if (mCurrentModule != null && mCurrentModule.isNeedUpdateUri()) {
            mRedirectionUri = uri;
        }
        new AsyncTask<Void, Void, FilmstripItem>() {
            @Override
            protected FilmstripItem doInBackground(Void... arg) {
                Log.i(TAG, "notifyNewMedia uri = " + uri);
                ContentResolver cr = mAppContext.getContentResolver();
                String mimeType = cr.getType(uri);
                FilmstripItem newData = null;
                if (FilmstripItemUtils.isMimeTypeVideo(mimeType)) {
                    sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
                    if (mSecureCamera)
                        newData = mVideoItemFactory.get(uri);
                    else
                        newData = mVideoItemFactory.queryContentUri(uri);
                    if (newData == null) {
                        Log.e(TAG, "Can't find video data in content resolver:" + uri);
                        return newData;
                    }
                } else if (FilmstripItemUtils.isMimeTypeImage(mimeType)) {
                    CameraUtil.broadcastNewPicture(mAppContext, uri);
                    if (mSecureCamera || mCurrentModule != null && mCurrentModule.getModuleTpye() == DreamModule.WIDEANGLE_MODULE)//SPRD:fix bug1183235
                        newData = mPhotoItemFactory.get(uri);
                    else
                        newData = mPhotoItemFactory.queryContentUri(uri);
                    if (newData == null) {
                        Log.e(TAG, "Can't find photo data in content resolver:" + uri);
                        return newData;
                    }
                } else {
                    Log.w(TAG, "Unknown new media with MIME type:" + mimeType + ", uri:" + uri);
                    return newData;
                }
                if (newData != null) {
                    MetadataLoader.loadMetadata(getAndroidContext(), newData);
                }
                return newData;
            }

            @Override
            protected void onPostExecute(final FilmstripItem data) {
                // TODO: Figure out why sometimes the data is aleady there.

                /* SPRD: fix bug 502496 data may be null and NullPointerException may happen @{ */
                if (data == null) {
                    return;
                }
                mDataAdapter.addOrUpdate(data);

                /*
                 * SPRD: fix bug 681226 Take photo with FilterModule take time longer than compared mobile  @{
                 */
                if (mCurrentModule != null && mCurrentModule instanceof FilterModuleAbs) {
                    return;
                }
                /* @} */

                // Legacy modules don't use CaptureSession, so we show the capture indicator when
                // the item was safed.
               // SPRD:fix bug 501739 when capture in burst mode a thumbnail appears
                //nj dream camera test 122
                if (mCurrentModule != null
                        && (!(mCurrentModule instanceof PhotoModule))) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            InputStream stream = null;
                            if (data.getItemViewType() == FilmstripItemType.PHOTO) {
                                try {
                                    ExifInterface exif = new ExifInterface();
                                    stream = new FileInputStream(data.getData().getFilePath());
                                    exif.readExif(stream);
                                    Bitmap bitmap = exif.getThumbnailBitmap();
                                    if (bitmap != null) {
                                        // SPRD: Fix bug 578330, remove this extra rotation after the driver bug is fixed
                                        bitmap = CameraUtil.rotate(bitmap, Exif.getOrientation(exif));
                                        indicateCapture(bitmap, 0);
                                        return;
                                    }
                                } catch (Exception e) {
                                    Log.i(TAG, "notifyNewMedia <PhotoModule> get thumbnail from exif error:" + e.getMessage());
                                }finally {
                                    if (stream != null) { //fix bug698756 remove sdcard, camera stop run
                                        try {
                                            stream.close();
                                        } catch (IOException e) {
                                            // do nothing here
                                        }
                                    }
                                }
                            }
                            final Optional<Bitmap> bitmap = data.generateThumbnail(
                                    mAboveFilmstripControlLayout.getWidth(),
                                    mAboveFilmstripControlLayout.getMeasuredHeight());
                            if (bitmap.isPresent() && data.getItemViewType() != FilmstripItemType.VIDEO) {
                                indicateCapture(bitmap.get(), 0);
                            }
                        }
                    };
                    runSyncThumbnail(runnable);
                }
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
        return;
    }

    public void startPeekAnimation(Bitmap bm, int ...args) {
        Log.i(TAG, "startPeekAnimation");
        if (mFilmstripVisible) {
            return;
        }
        if (bm != null && !bm.isRecycled()) {
            indicateCapture(bm, (args.length != 0) ? args[0] : 0);
        }
    }

    @Override
    public void enableKeepScreenOn(boolean enabled) {
        if (mPaused) {
            return;
        }

        mKeepScreenOn = enabled;
        if (mKeepScreenOn) {
            mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            keepScreenOnForAWhile();
        }
    }

    @Override
    public CameraProvider getCameraProvider() {
        return mCameraController;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_details:
                showDetailsDialog(mFilmstripController.getCurrentAdapterIndex());
                return true;
                /*
                 * SPRD Bug:488399 Remove Google Help and Feedback. @{ Original Android code: case
                 * R.id.action_help_and_feedback: mResetToPreviewOnResume = false; new
                 * GoogleHelpHelper(this).launchGoogleHelp(); return true;
                 */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isCaptureIntent() {
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(getIntent().getAction())
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Note: Make sure this callback is unregistered properly when the activity is destroyed since
     * we're otherwise leaking the Activity reference.
     */
    private final CameraExceptionHandler.CameraExceptionCallback mCameraExceptionCallback = new CameraExceptionHandler.CameraExceptionCallback() {
        @Override
        public void onCameraError(int errorCode) {
            // Not a fatal error. only do Log.e().
            Log.e(TAG, "Camera error callback. error=" + errorCode);
        }

        @Override
        public void onCameraException(
                RuntimeException ex, String commandHistory, int action, int state) {
            Log.e(TAG, "Camera Exception", ex);
            onFatalError();
        }

        @Override
        public void onDispatchThreadException(RuntimeException ex) {
            Log.e(TAG, "DispatchThread Exception", ex);
            onFatalError();
        }

        private void onFatalError() {
            if (mCameraFatalError) {
                return;
            }
            mCameraFatalError = true;

            // If the activity receives exception during onPause, just exit the app.
            if (mPaused && !isFinishing()) {
                Log.e(TAG, "Fatal error during onPause, call Activity.finish()");
                finish();
            } else {
                mFatalErrorHandler
                        .handleFatalError(FatalErrorHandler.Reason.CANNOT_CONNECT_TO_CAMERA);
            }
        }
    };

    @Override
    public void onNewIntentTasks(Intent intent) {
        /* SPRD: ui check 49 gifModule ui @{ */
        /* SPRD: Fix bug 694193,NullPointer happens if not check permissions @{ */
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onNewIntentTasks: Missing critical permissions.");
            return;
        }
        /* @} */
        if (mCurrentModeIndex != getModeIndex()) {
            onModeSelected(getModeIndex());
        }
        /* @} */
    }

    private Handler mOnCreateAsyncHandler;
    private boolean mExtraWorkNeeded;
    private boolean mHasDelayWorkOnCreate = true;
    private boolean mSkipFirstChangeOnResume = true;
    private boolean mHasPostModeList = false;

    String resultString = "";
    private String mSecureCheck;
    public static final String DAFAULT_SYSTEM = "0";
    public static final String DAFAULT_SYSTEM_OK = "1";
    public static final String DAFAULT_LOCATION_OK = "1";
    public static final String DAFAULT_PERMISSION_OK = "2";

    //SPRD:fix Bug 616294 add warn location express when enter into Camera
    private void checkCameraPermission(){
        if (!isCaptureIntent()) {//SPRD:fix bug519999 Create header photo, pop permission error
            checkPermissions();

            if (!mHasCriticalPermissions) {
                Log.v(TAG, "onCreate: Missing critical permissions.");
                return;
            }
        }
    }
    public void checkIsTogglable() {
        int bCameraId = getCameraProvider().getFirstBackCameraId();
        int fCameraId = getCameraProvider().getFirstFrontCameraId();
        ButtonManagerDream buttonManager = (ButtonManagerDream)getButtonManager();
        int cameraId = mDataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().getInt(Keys.KEY_CAMERA_ID);
        if ((bCameraId != NO_DEVICE) && (fCameraId != NO_DEVICE)) {
            buttonManager.mIsTogglable = true;
            if (-1 == cameraId || 0 == cameraId) {
                mDataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID, bCameraId);//SPRD:fix bug1155273
            }
        } else {
            buttonManager.mIsTogglable = false;
            if (fCameraId != NO_DEVICE) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mPaused) {
                            CameraUtil.toastHint(CameraActivity.this, R.string.back_camera_not_available);
                        }
                    }
                });
                if (-1 == cameraId || 1 == cameraId) {
                    mDataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID, fCameraId);
                }
            } else if (bCameraId != NO_DEVICE) {
                if (-1 == cameraId || 0 == cameraId) {
                    mDataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID, bCameraId);
                }
            }
        }
        if (bCameraId != NO_DEVICE && bCameraId != 0 && cameraId != 1) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mPaused) {
                        CameraUtil.toastHint(CameraActivity.this, R.string.main_camera_not_available);
                    }
                }
            });
        }
    }
    //SPRD:fix Bug 616294 add warn location express when enter into Camera
    private void checkSystemPermission(){
        Intent intent = new Intent(this, SecurityAccessLocation.class);
        startActivityForResult(intent, 1);
        finish();
        return;
    }



    public void initializeHandlerThread(){
        HandlerThread handlerThread = new HandlerThread("OnCreateAsync Task");
        handlerThread.start();
        mOnCreateAsyncHandler = new Handler(handlerThread.getLooper());

        HandlerThread CameraRequest = new HandlerThread("CameraRequest Task");
        CameraRequest.start();
        /* SPRD: Fix bug 605818, wait for background camera closed @{ */
        mCameraRequestHandler = new Handler(CameraRequest.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CAMERA_REQUEST_PENDING: {
                        // do not check whether camera is available
                        // if there's error, let it go
                        if (!mPaused && mCurrentModule != null) {
                            Log.i(TAG, "We have wait 5s, resume mode now");
                            mMainHandler.post(mResumeRunnable);
                        }
                    }
                }
            }
        };
        mCameraAvailableMap = new ConcurrentHashMap<String, Boolean>();
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, mCameraRequestHandler);
        /* @} */
        mMainHandler = new MainHandler(this, getMainLooper());
        mAppContext = getApplicationContext();
    }

    private Counter mCounterOncreateWait = new Counter(4);
//    private Counter mCounterOncreateOpenCamera = new Counter(4);
    private Counter mCounterOncreateOpenCamera = new Counter(2);
    private Counter mCounterCamAppUI = new Counter(0);
    private Counter mCounterCamAppUIPrepareModuleUI = new Counter(1);
    private Counter mCounterOneCameraManager = new Counter(1);
    private Counter mCounterDataModule = new Counter(1);
    private Counter mCounterMemoryManager = new Counter(1);
    private Counter mCounterUtil = new Counter(1);
    private Counter mCounterPause = new Counter(2);


    //Sprd：Fix bug739085
    private Counter mCounterUpdateSettings = new Counter(1);
    public void waitUpdateSettingsCounter (){
        mCounterUpdateSettings.waitCount();
    }

    private boolean isRefocusModeOn(int cameraId) {
        if (cameraId == DreamUtil.BACK_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1
                || cameraId == DreamUtil.FRONT_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1) {
            return true;
        }
        return false;
    }

    // oncreate - request open camera
    private Runnable mOncreateOpencamera = new Runnable() {
        @Override
        public void run() {
            mCounterOncreateOpenCamera.waitCount();
            mOnCreateTime = System.currentTimeMillis();
            int cameraid = mDataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().getInt(Keys.KEY_CAMERA_ID);
            if (!isKeyguardLocked()
                    /*&& mCurrentModeIndex == SettingsScopeNamespaces.AUTO_PHOTO*/
                    && !isRefocusModeOn(cameraid)
                    && checkAllCameraAvailable()) {
                mIsCameraRequestedOnCreate = true;
                /* SPRD: w+t module just for back camrea and auto photo module@{ */
                if (CameraUtil.isWPlusTEnable() && !isCaptureIntent()){
                    CameraUtil.resetWPlusTThreshold(CameraPictureSizesCacher.getSizesForCamera(CameraUtil.BACK_W_PLUS_T_PHOTO_ID, mAppContext));
                }

                if (CameraUtil.isWPlusTAbility(mAppContext,true) && !isCaptureIntent()){
                    cameraid = CameraUtil.BACK_W_PLUS_T_PHOTO_ID;
                }
                /*@}*/
                if (CameraUtil.isTcamAbility(mAppContext,cameraid) && !isCaptureIntent()) {
                    cameraid = CameraUtil.BACK_TRI_CAMERA_ID;
                }

                Log.e(TAG , "UltraWideAngle Enable = " + CameraUtil.isUltraWideAngleEnabled());
                if (mHasCriticalPermissions) {
                    mCameraController.requestCamera(cameraid,
                            mCurrentModule == null ? true : mCurrentModule.useNewApi());
                }
            }
        }
    };

    // oncreate - module data setting
    private Runnable mOnCreateDataSetting = new Runnable() {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            mCounterUtil.waitCount();

            mDataModuleManager = DataModuleManager
                    .getInstance(CameraActivity.this);
            mDataModuleManager.initializeDataModuleCamera();
            mDataModule = mDataModuleManager.getDataModuleCamera();
            mCounterDataModule.count();
            mCounterOncreateWait.count();
        }
    };

    // oncreate - camera controller
    private Runnable mOncreateCameraController = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, " mOncreateCameraController run");
            long startTime = System.currentTimeMillis();
            mActiveCameraDeviceTracker = ActiveCameraDeviceTracker.instance();
            try {
                mCameraController = new CameraController(mAppContext,
                        CameraActivity.this, mCameraRequestHandler,
                        CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                                CameraAgentFactory.CameraApi.API_1),
                        CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                                CameraAgentFactory.CameraApi.AUTO),
                        mActiveCameraDeviceTracker);
                mCameraController
                        .setCameraExceptionHandler(new CameraExceptionHandler(
                                mCameraExceptionCallback, mMainHandler));
            } catch (AssertionError e) {
                Log.e(TAG, "Creating camera controller failed.", e);
                mFatalErrorHandler.onGenericCameraAccessFailure();
            }
            Log.i(TAG, " mOncreateCameraController run end");
            mCameraController.initCameraDeviceInfo();
            checkIsTogglable();
            Log.i(TAG, " checkIsTogglable end");
            mCounterOncreateOpenCamera.count();
        }
    };

    // oncreate - onecameramanager
    private Runnable mOncreateOneCameraManager = new Runnable() {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                mOneCameraManager = OneCameraModule.provideOneCameraManager();
            } catch (OneCameraException e) {
                Log.e(TAG, "Creating camera manager failed.", e);
                mFatalErrorHandler.onGenericCameraAccessFailure();
            }
            mCounterOneCameraManager.count();
        }
    };

    // oncreate modules info and set current module
    private Runnable mOncreateModulesInfoAndCurrentModule = new Runnable() {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            mModuleManager = new ModuleManagerImpl();
            mCounterUtil.waitCount();
            CameraUtil.mModuleInfoResovle.resolve(mAppContext);
            ModulesInfo.setupModules(mAppContext, mModuleManager);
            setModuleFromModeIndex(getModeIndexDefault());
            mCounterCamAppUIPrepareModuleUI.count();
        }
    };

    private Runnable mOnresumeModulesAndFeatureList = new Runnable() {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            CameraUtil.initCameraData();
            CameraUtil.mModuleInfoResovle.resolve(mAppContext);
            ModulesInfo.setupModules(mAppContext, mModuleManager);
            Log.i(TAG, " mOnresumeModulesAndFeatureList coase time :" + (System.currentTimeMillis() - startTime) + " ms");
        }
    };

    // oncreate manager initialize
    private Runnable mOncreateManagerInitialize = new Runnable() {

        @Override
        public void run() {
            // SPRD:fix bug 473462 add for burst capture
//            CameraUtil.initialize(CameraActivity.this);
            CameraUtil.waitCameraUtilCounter();

            isSupportGps = CameraUtil.isRecordLocationEnable();

            mCounterUtil.count();
            mCounterOncreateOpenCamera.count();

            mLocationManager = new LocationManager(CameraActivity.this);
            mCounterOncreateWait.count();
            mMotionManager = getServices().getMotionManager();
            mMemoryManager = getServices().getMemoryManager();
            mCounterMemoryManager.count();
        }
    };

    private Runnable mOncreateOtherTask = new Runnable() {

        @Override
        public void run() {
            /*
             * SPRD:fix bug524433 Filmstripview is not refresh when sdcard
             * removed @{
             */
            IntentFilter intentFilter = new IntentFilter(
                    Intent.ACTION_MEDIA_EJECT);
            intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addDataScheme("file");
            mReceiver = new MyBroadcastReceiver();
            CameraActivity.this.registerReceiver(mReceiver, intentFilter);
            /* @} */

            /*
             * SPRD:fix bug641569 the storage path was changed after restart
             * mobile @{
             */
            IntentFilter intentFilterShotDown = new IntentFilter(
                    Intent.ACTION_SHUTDOWN);
            mReceiverShutDown = new MyBroadcastReceiverShutDown();
            CameraActivity.this.registerReceiver(mReceiverShutDown, intentFilterShotDown);
            /* @} */

            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    mCounterMemoryManager.waitCount();
                    HashMap memoryData = mMemoryManager.queryMemory();
                }
            });

            // get battery status


            setupNfcBeamPush();
            mIsBatteryLow = getCurrentBattery() <= CameraUtil.getLowBatteryNoFlashLevel();

            mCounterOneCameraManager.waitCount();
            mResolutionSetting = new ResolutionSetting(mOneCameraManager,
                    getContentResolver());

            mCounterDataModule.waitCount();
            StorageUtil storageUtil = StorageUtil.getInstance();
            storageUtil.initialize(mDataModule,
                    CameraActivity.this.getContentResolver());
            //Sprd：Fix bug739085
            mCounterUpdateSettings.count();
            // Bug #533869 new feature: check UI 27,28: dream camera of intent
            // capture
            if (!isCaptureIntent()) {
                DreamUtil dreamUtil = new DreamUtil();
                dreamUtil.savaToCameraMode(CameraActivity.this, DataModuleManager.getInstance(mAppContext).getTempCameraModule(),
                        mDataModule.getInt(Keys.KEY_CAMERA_ID),
                        getModeIndexDefault());
            }

            // profile.stop("dream initialization");
            mUriMap = new LinkedHashMap<String, Uri>(16);
            mRunnbaleMap = new LinkedHashMap<String, UpdateRunnbale>(16);
        }
    };

    private Runnable mOncreateOtherTaskInOncreateProcess = new Runnable() {

        @Override
        public void run() {

            mCounterDataModule.waitCount();
            // Bug #533869 new feature: check UI 27,28: dream camera of intent
            // capture
            if (!isCaptureIntent()) {
                mDataModule.set(Keys.KEY_CAMERA_SWITCH, true);
                mDataModule.set(Keys.KEY_INTENT_CAMERA_SWITCH, false);
                mDataModule.set(Keys.KEY_BACK_PHOTO_MODE, 0);
                mDataModule.set(Keys.KEY_FRONT_PHOTO_MODE, 0);
                mDataModule.set(Keys.KEY_BACK_VIDEO_MODE, 9);
                mDataModule.set(Keys.KEY_FRONT_VIDEO_MODE, 9);
            } else {
                mDataModule.set(Keys.KEY_INTENT_CAMERA_SWITCH, true);
                mDataModule.set(Keys.KEY_CAMERA_SWITCH, false);
            }
            mCounterOncreateWait.count();

        }
    };

    /**
     * this function will cost 200+ms, so use threadpool to run
     */
    private Runnable mUsedGooglePhotos = new Runnable() {
        @Override
        public void run() {
                mIsUsedGooglePhotos = IntentHelper.isPackageInstalled(mAppContext,GOOGLE_PHOTOS);
            if (mIsUsedGooglePhotos) {
                return;
            } else {
                mIsUsedGooglePhotosGo = IntentHelper.isPackageInstalled(mAppContext,GOOGLE_PHOTOS_GO);
            }
        }
    };

    protected boolean alreadyTriggerOnce  = false;

    public void doCaptureSpecial() {
        if(mSecureCamera && !alreadyTriggerOnce){
            alreadyTriggerOnce = true;
        }
    }

    private Runnable mLoadFilmStrip = new Runnable() {

        @Override
        public void run() {
            mLocalImagesObserver = new FilmstripContentObserver();
            mLocalVideosObserver = new FilmstripContentObserver();

            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
                    mLocalImagesObserver);
            getContentResolver().registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                    mLocalVideosObserver);
            if (mSecureCamera) {
                mLocalImagesObserver.setForegroundChangeListener(new FilmstripContentObserver.ChangeListener() {
                    @Override
                    public void onChange(Uri uri) {
                        Log.i(TAG, "onChange uri=" + uri);
                        String lastSegment = null;
                        if (uri != null)
                            lastSegment = uri.getLastPathSegment();

                        if (mPaused && lastSegment != null && TextUtils.isDigitsOnly(lastSegment)) {//if add photo when paused from gallery

                            FilmstripItem newData = mPhotoItemFactory.get(uri);
                            if (newData == null) {
                                Log.e(TAG, "Can't find photo data in content resolver:" + uri);
                                return;
                            }
                            if(!newData.getData().getFilePath().contains(StorageUtil.DEFAULT_DIR)){
                                return;
                            }

                            if(!alreadyTriggerOnce){
                                return;
                            }

                            if (securePhotoList != null && !securePhotoList.contains(ContentUris.parseId(uri))) {
                                securePhotoList.add(ContentUris.parseId(uri));//SPRD:fix bug1141166
                            }
                            mDataAdapter.addOrUpdate(newData);
                        }
                    }
                });
            }
            ContentResolver appContentResolver = mAppContext.getContentResolver();
            mPhotoItemFactory = new PhotoItemFactory(mAppContext, appContentResolver,
                    new PhotoDataFactory());
            mVideoItemFactory = new VideoItemFactory(mAppContext, appContentResolver,
                    new VideoDataFactory());

            preloadFilmstripItems();
            mCounterOncreateWait.count();
        }
    };

    @Override
    public void onCreateTasks(Bundle state) {
        if (isInMultiWindowMode()) {

            CameraUtil.toastHint(this, R.string.multi_window_tip);
            finish();
            return;
        }

        Profile profile = mProfiler.create("CameraActivity.onCreateTasks").start();
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_START);

        profile.mark();
        initializeHandlerThread();
        profile.mark("register camera request handler");

        THREAD_POOL_EXECUTOR.execute(mOncreateCameraController);
        THREAD_POOL_EXECUTOR.execute(mOncreateManagerInitialize);
        profile.mark();
        preInit();
        profile.mark("preInit()");

        profile.mark();
        mCounterUtil.waitCount();
        mSuccess = checkPermissionOnCreate();
        profile.mark("checkPermissionOnCreate()");

        /*
         * SPRD: Fix bug 696868, without permissions,
         * NullPointerException happens while run monkey @{
         */
        if (!mSuccess) {
            return;
        }
        /* @} */

        profile.mark();

        // open camera task
        THREAD_POOL_EXECUTOR.execute(mOncreateOpencamera);

        THREAD_POOL_EXECUTOR.execute(mOncreateModulesInfoAndCurrentModule);
        THREAD_POOL_EXECUTOR.execute(mOnCreateDataSetting);

        // other task

        THREAD_POOL_EXECUTOR.execute(mOncreateOneCameraManager);
        THREAD_POOL_EXECUTOR.execute(mOncreateOtherTaskInOncreateProcess);
        THREAD_POOL_EXECUTOR.execute(mOncreateOtherTask);
        THREAD_POOL_EXECUTOR.execute(mUsedGooglePhotos);

        mOnCreateAsyncHandler.post(mLoadFilmStrip);

        profile.mark("THREAD_POOL_EXECUTOR.exeture");

        profile.mark();
        setContentView(R.layout.dream_main);
//        mCounterOncreateOpenCamera.count();
        profile.mark("setContentView");

        mSecureEnterTime = System.currentTimeMillis();
        profile.mark();
        postInit();
        profile.mark("postInit");

        profile.mark();
        mCounterOncreateWait.waitCount();
        profile.stop("mOncreateWaitCounter.waitCount");
        // after setcontent view & initialize cameraAPPUI, load the stub view & add view
    }

    private void preInit() {
        shutdown = false;


        mFatalErrorHandler = new FatalErrorHandlerImpl(CameraActivity.this);

        // SPRD: Fix bug 572473 add for usb storage support
        // SPRD: fix bug 620061 first start app, will not show OTG storage
        MultiStorage.getInstance().initialize(mAppContext);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        // We suppress this flag via theme when drawing the system preview
        // background, but once we create activity here, reactivate to the
        // default value. The default is important for L, we don't want to
        // change app behavior, just starting background drawable layout.
//        if (ApiHelper.isLOrHigher()) {
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//        }
        /* SPRD: Add for bug 561548 @{ */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON // SPRD: Add for bug640303, add to support launch camera directly when screen is off
                | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        /* @} */
        if (ApiHelper.HAS_ROTATION_ANIMATION) {
            setRotationAnimation();
        }

        // Check if this is in the secure camera mode.
        Intent intent = getIntent();
        String action = intent.getAction();
        if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)
                || ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
            mSecureCamera = true;
        } else {
            mSecureCamera = intent.getBooleanExtra(SECURE_CAMERA_EXTRA , false);
        }
        if (mSecureCamera) {
            // Change the window flags so that secure camera can show when
            // locked
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            // SPRD: fix bug543384/511212 add open camera acquirement
            /*
             * SPRD: Modify for bug640303, move flag FLAG_TURN_SCREEN_ON to
             * onCreateTasks also need add flag FLAG_TURN_SCREEN_ON for
             * CameraActivity @{ params.flags |=
             * (WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
             * WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
             */
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            win.setAttributes(params);
            /* @} */

            // Filter for screen off so that we can finish secure camera
            // activity when screen is off.
            IntentFilter filter_screen_off = new IntentFilter(
                    Intent.ACTION_SCREEN_OFF);
            registerReceiver(mShutdownReceiver, filter_screen_off);

            // Filter for phone unlock so that we can finish secure camera
            // via this UI path:
            // 1. from secure lock screen, user starts secure camera
            // 2. user presses home button
            // 3. user unlocks phone
            IntentFilter filter_user_unlock = new IntentFilter(
                    Intent.ACTION_USER_PRESENT);
            CameraActivity.this.registerReceiver(mShutdownReceiver, filter_user_unlock);
            //Sprd :fix bug880861
            IntentFilter filter_home = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            registerReceiver(mShutdownReceiver, filter_home);
        }

        //check gts assistant camera
        if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)){
            String ref = intent.getStringExtra(INTENT_EXTRA_REFERRER_NAME);
            if (!isVoiceInteractionRoot() && (ref == null || !ref.equals(REF_VALUE)))
                return;
            boolean front = intent.getBooleanExtra(EXTRA_FRONT_CAMERA , false);
            if (front) {
                DataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID,1);
            } else {
                DataModuleManager.getInstance(CameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID,0);
            }
        }
    }

    private void postInit() {
        Profile profile = mProfiler.create("postInit").start();
        // init before setContentView, because of TopRightWeightedLayout
        mOrientationManager = new OrientationManagerImpl(this, mMainHandler);

        getDreamHandler().post(new Runnable() {
            @Override
            public void run() {
                captureShutterButtonIcon = getResources().getDrawable(R.drawable.ic_capture_camera_sprd);
            }
        });
        
        // A window background is set in styles.xml for the system to show a
        // drawable background with gray color and camera icon before the
        // activity is created. We set the background to null here to prevent
        // overdraw, all views must take care of drawing backgrounds if
        // necessary. This call to setBackgroundDrawable must occur after
        // setContentView, otherwise a background may be set again from the
        // style.
        getWindow().setBackgroundDrawable(null);

        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mActionBar.hide();

        profile.mark();
        mCounterCamAppUI.waitCount();
        mCameraAppUI = new CameraAppUI(this,
                (MainActivityLayout) findViewById(R.id.activity_root_view), isCaptureIntent());
        getDreamHandler().post(new Runnable() {
            @Override
            public void run() {
                inflateFilmstrip();
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyed()) return;
                        initFilmstrip();
                    }
                });
            }
        });

        profile.mark("new CameraAppUI");

        profile.mark();
        mCounterCamAppUIPrepareModuleUI.waitCount();
        mCameraAppUI.prepareModuleUI();
        profile.mark("prepareModuleUI");

        // must done after CameraAppUI.prepareModuleUI
        mOrientationManager.addOnOrientationChangeListener(mCameraAppUI);

        // must done after CameraAppUI
        profile.mark();
        mCurrentModule.init(this, isSecureCamera(), isCaptureIntent());
        profile.mark("CurrentModule.init");

        profile.stop("stop");
    }

    public void initModelistview(){
        ViewStub viewStubModeListPanel = (ViewStub) findViewById(R.id.dream_mode_list_layout_id);
        if (viewStubModeListPanel == null) {
            return;
        }
        viewStubModeListPanel.inflate();
        mModeListView = (ModeListView) findViewById(R.id.mode_list_layout);
        mModeListView.init(null);
        mModeListView.setVisibilityChangedListener(new ModeListVisibilityChangedListener() {
            @Override
            public void onVisibilityChanged(boolean visible) {
                mModeListVisible = visible;
                mCameraAppUI.setShutterButtonImportantToA11y(!visible);
                updatePreviewVisibility();
            }
        });
        mCameraAppUI.initModeListView();
    }

    /**
     * Get the current mode index from the Intent or from persistent settings.
     */
    public int getModeIndex() {
        int modeIndex = -1;
        int photoIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        int videoIndex = SettingsScopeNamespaces.AUTO_VIDEO;
        int gcamIndex = SettingsScopeNamespaces.GCAM;
        int captureIntentIndex = SettingsScopeNamespaces.INTENTCAPTURE;
        int videoIntentIndex = SettingsScopeNamespaces.INTENTVIDEO;

        String intentAction = getIntent().getAction();
        /* SPRD:fix bug535425 Open Camera from gallery, gif can not form
         * Android original code
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(intentAction)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(intentAction)) {
        */
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(intentAction)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(intentAction)) {
            modeIndex =  videoIntentIndex;
        } else if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
            // Capture intent.
            modeIndex = captureIntentIndex;
        /* SPRD:fix bug535425 Open Camera from gallery, gif can not form
         * Android original code
        } else if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(intentAction)
                ||MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
        */
        } else if ((MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction))) {
            modeIndex = mSecureCameraModeIndex;
            if (modeIndex == photoIndex){
                if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.BACK_CAMERA){
                    if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                            Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1)
                        modeIndex = SettingsScopeNamespaces.REFOCUS;
                    else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                            Context.MODE_PRIVATE).getString(Keys.KEY_ULTRA_WIDE_ANGLE,"0")) == 1) // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
                        modeIndex = SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE;
                    else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                            Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO,"0")) == 1)
                        modeIndex = SettingsScopeNamespaces.FILTER;
                } else if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID, "0")) == DreamUtil.FRONT_CAMERA) {
                    if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                            Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS, "0")) == 1) {
                        modeIndex = SettingsScopeNamespaces.FRONT_BLUR;
                    } else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                            Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO, "0")) == 1) {
                        modeIndex = SettingsScopeNamespaces.FILTER;
                    }
                }
            }
        } else if(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(intentAction)){
            modeIndex = DataModuleManager.getInstance(this).getDataModuleCamera()
                    .getInt(Keys.KEY_STARTUP_MODULE_INDEX);
        } else {
            // If the activity has not been started using an explicit intent,
            // read the module index from the last time the user changed modes
            modeIndex = DataModuleManager.getInstance(this).getDataModuleCamera()
                    .getInt(Keys.KEY_STARTUP_MODULE_INDEX);

            if(modeIndex == SettingsScopeNamespaces.REFOCUS ||  modeIndex == SettingsScopeNamespaces.FRONT_BLUR || modeIndex
                    == SettingsScopeNamespaces.AUTO_PHOTO || modeIndex == SettingsScopeNamespaces.FILTER){
                if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.BACK_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1) {
                    modeIndex = SettingsScopeNamespaces.REFOCUS;
                } else if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.FRONT_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1) {
                    modeIndex = SettingsScopeNamespaces.FRONT_BLUR;
                } else if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.FRONT_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO,"0")) == 1
                        || Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.BACK_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO,"0")) == 1) {
                    modeIndex = SettingsScopeNamespaces.FILTER;
                } else{
                    modeIndex = SettingsScopeNamespaces.AUTO_PHOTO;
                }
            }
            if (modeIndex == SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE || modeIndex == SettingsScopeNamespaces.AUTO_PHOTO) {
                if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.BACK_CAMERA && Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_ULTRA_WIDE_ANGLE,"0")) == 1) // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
                    modeIndex = SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE;
                else
                    modeIndex = SettingsScopeNamespaces.AUTO_PHOTO;
            }
            if (modeIndex == gcamIndex || modeIndex < 0) {
                modeIndex = photoIndex;
            }
            /**@}*/
        }
        if (isCameraAndModeNotMatched(modeIndex, captureIntentIndex, videoIntentIndex)) {
            Log.e(TAG,"modeIndex="+modeIndex+" cameraId="+mDataModule.getInt(Keys.KEY_CAMERA_ID));
            if (mCurrentModule instanceof VideoModule){
                return videoIndex;
            }
            return photoIndex;
        }
        return modeIndex;
    }

    private boolean isCameraAndModeNotMatched(int modeIndex, int captureIntentIndex, int videoIntentIndex) {
       return (mDataModule != null && mModeListView != null && !mModeListView.isSupportMC(modeIndex,mDataModule.getInt(Keys.KEY_CAMERA_ID))
              && modeIndex != captureIntentIndex && modeIndex != videoIntentIndex);
    }
    /**
     * Get the current mode index from the Intent or from persistent settings.
     */
    public int getModeIndexDefault() {
        int modeIndex = -1;
        int photoIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        int captureIntentIndex = SettingsScopeNamespaces.INTENTCAPTURE;
        int videoIntentIndex = SettingsScopeNamespaces.INTENTVIDEO;

        String intentAction = getIntent().getAction();
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(intentAction)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(intentAction)) {
            modeIndex = videoIntentIndex;
        } else if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
            // Capture intent.
            modeIndex = captureIntentIndex;
        } else if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(intentAction)
                || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(intentAction)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(intentAction)) {
            modeIndex = photoIndex;
        } else {
            modeIndex = photoIndex;
        }
        return modeIndex;
    }
    /**
     * Call this whenever the mode drawer or filmstrip change the visibility state.
     */
    private void updatePreviewVisibility() {
        if (mCurrentModule == null) {
            return;
        }

        int visibility = getPreviewVisibility();
        mCameraAppUI.onPreviewVisiblityChanged(visibility);
        updatePreviewRendering(visibility);
        mCurrentModule.onPreviewVisibilityChanged(visibility);
    }

    private void updatePreviewRendering(int visibility) {
        if (visibility == ModuleController.VISIBILITY_HIDDEN) {
            mCameraAppUI.pausePreviewRendering();
        } else {
            mCameraAppUI.resumePreviewRendering();
        }
    }

    private int getPreviewVisibility() {
        if (mFilmstripCoversPreview) {
            return ModuleController.VISIBILITY_HIDDEN;
        } else if (mModeListVisible) {
            return ModuleController.VISIBILITY_COVERED;
        } else {
            return ModuleController.VISIBILITY_VISIBLE;
        }
    }

    private void setRotationAnimation() {
        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE;
        rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!isFinishing()) {
            keepScreenOnForAWhile();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (CameraUtil.isIdleSleepEnable() && ev.getActionMasked() == MotionEvent.ACTION_DOWN && mCurrentModule.getCameraPreviewState()) {
            mLastReceiveEventTime = System.currentTimeMillis();
            mMainHandler.removeMessages(MSG_DESTORY_CAMER);
            mMainHandler.sendEmptyMessageDelayed(MSG_DESTORY_CAMER, mDestoryDelayTimes);
        }
        boolean result = super.dispatchTouchEvent(ev);
        return result;
    }

    private boolean mHasKeyEventEnter = false;

    /* SPRD:fix bug934439 use enter event to capture @{ */
    public void resetHasKeyEventEnter() {
        mHasKeyEventEnter = false;
    }

    public boolean hasKeyEventEnter() {
        return mHasKeyEventEnter;
    }
    /* @} */

    /* SPRD:fix bug499275 Front DC can not capture through the Bluetooth@{ */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if ((event.getKeyCode()  == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode()  == KeyEvent.KEYCODE_VOLUME_DOWN || event.getKeyCode()  == KeyEvent.KEYCODE_CAMERA) &&
                (isWaitToChangeMode() || mCameraAppUI.getFreezeScreenFlag())){
            //during switch camera don`t answer picture key event
            return true;
        }
        if (CameraUtil.isIdleSleepEnable() && event.getAction() == KeyEvent.ACTION_DOWN && mCurrentModule.getCameraPreviewState()) {
            mLastReceiveEventTime = System.currentTimeMillis();
            mMainHandler.removeMessages(MSG_DESTORY_CAMER);
            mMainHandler.sendEmptyMessageDelayed(MSG_DESTORY_CAMER, mDestoryDelayTimes);
        }
        // TODO Auto-generated method stub
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            /* SPRD:fix bug670989 add for bluetooth capture @{ */
            mHasKeyEventEnter = true;
            /* @} */
            //return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /* }@ */

    @Override
    public void onPauseTasks() {
        mResumed = false;
        Profile profile = mProfiler.create("CameraActivity.onPause").start();
        Log.i(TAG, "onPauseTasks start!");
        /* SPRD: Fix bug 583585, Turn off camera permissions on the fixed screen mode ,Will be reported to the wrong @{ */
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onPauseTasks: Missing critical permissions.");
            return;
        }
        /* @} */

        if(mSecureCamera){
            mSecureCameraModeIndex = mCurrentModeIndex;//SPRD: bugfix 1001985
        }

        mPaused = true;
        //mMainHandler.removeCallbacks(mCameraAvailRunnable);

        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onPause unbind service");
                /* SPRD: add for mutex between float window and camera @{ */
                if (mServiceBinded) {
                    unbindService(mIFloatWindowServiceConn);
                    mServiceBinded = false;
                }
                Log.i(TAG, "onPause unbind IFloatWindowServiceConn end");

                if (CameraUtil.getLowBatteryNoFlashLevel() > 0) {
                    unregisterReceiver(mBatteryReceiver);
                }
                Log.i(TAG, "onPause unregisterreceiver BatteryReceiver end");

                // Always stop recording location when paused. Resume will start
                // location recording again if the location setting is on.
                mLocationManager.recordLocation(false);
                /* @} */
                Log.i(TAG, "onPause unregister LocationManager end");
                mCounterPause.count();
            }
        });

        /*
         * Save the last module index after all secure camera and icon launches, not just on mode
         * switches. Right now we exclude capture intents from this logic, because we also ignore
         * the cross-Activity recovery logic in onStart for capture intents.
         */
        if (!isCaptureIntent() && !mSecureCamera) {
            DataModuleManager.getInstance(this).getDataModuleCamera()
                    .set(Keys.KEY_STARTUP_MODULE_INDEX, mCurrentModeIndex);
        }

        mCurrentModule.pause();

        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                if (mCameraFatalError && !isFinishing()) {
                    Log.v(TAG, "onPause when camera is in fatal state, call Activity.finish()");
                } else {
                    // Close the camera and wait for the operation done.
                    Log.i(TAG, "onPause closing camera start");
                    if (mCameraController != null) {
                        mCameraController.closeCamera(false);//SPRD:fix bug1131052
                        Log.i(TAG, "onPause closing camera end");
                    }
                }

                mCounterPause.count();
            }
        });



        mHasKeyEventEnter = false;

        // SPRD:fix bug 666911
        CameraUtil.resetHint();
        CameraUtil.resetDialog();
        /*
         * SPRD: fix bug 498612 If the detailDialog is shown, it should dismiss when the activity is
         * onPause. @{
         */
        if (detailDialog != null) {
            detailDialog.dismiss();
            detailDialog = null;
        }
        if (mActionBarMenu != null) {
            mActionBarMenu.close();
        }
        /* @} */

        /* SPRD: Fix bug 605818, wait for background camera closed @{ */
        mCameraRequestHandler.removeMessages(MSG_CAMERA_REQUEST_PENDING);
        mMainHandler.removeCallbacks(mResumeRunnable);
        /* @} */

        /* SPRD: fix bug521990 Back to Camera from lockscreen, the screen is freeze */
        getCameraAppUI().pause();

        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_PAUSE);

        /*
         * SPRD: CameraActivity onPause need to updateStorage ensure OnResume to check storage
         * update to updateAdapter
         */
        StorageUtil storageUtil = StorageUtil.getInstance();
        storageUtil.updateStorage();
        DataModuleManager.getInstance(this).getCurrentDataModule().set(Keys.KEY_VOICE_CAMERA_DONE,0);
        // add this line code Because nj monkey crash 6-22.
        // steps: camera in filterModule, backpressed; entry camera ,now is in autophotomodule,
        // click shuttersound switch, crash.
        DataModuleManager.getInstance(this).getDataModuleCamera().removeListener(this,this);
        DataModuleManager.getInstance(this).removeListener(this);
        mCameraAppUI.hideCaptureIndicator();
        //mFirstRunDialog.dismiss();

        mOrientationManager.pause();
//        mPanoramaViewHelper.onPause();

        if(!mSecureCamera){
            mLocalImagesObserver.setForegroundChangeListener(null);
        }

        mLocalImagesObserver.setActivityPaused(true);
        mLocalVideosObserver.setActivityPaused(true);
        if (mPreloader != null) {
            mPreloader.cancelAllLoads();
        }
        resetScreenOn();

        mMotionManager.stop();

        // Camera is in fatal state. A fatal dialog is presented to users, but users just hit home
        // button. Let's just kill the process.
        if (mCameraFatalError && !isFinishing()) {
            Log.v(TAG, "onPause when camera is in fatal state, call Activity.finish()");
            finish();
        }



        // SPRD: fix bug520618,490383 Video is still appear if it is deleted from fileManager
        // SPRD: fix bug571335 camera anr
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                mFilmStripCount = getFilmStripCount();
            }
        });
        profile.stop();

        mCounterPause.waitCount();
        Log.i(TAG, "onPause end!");
    }

    @Override
    public void onResumeTasks() {
        Log.i(TAG, "onResumeTasks start!");

        mPaused = false;
        // ModeCover may not be drawed when monkey test
        checkPermissions();
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onResume: Missing critical permissions.");
            /**
             * SPRD:fix bug519999 Create header photo, pop permission error original code finish();
             */
            return;
        }
        if (mFlagForResumeFeaturelist) {
            mFlagForResumeFeaturelist = false;
            THREAD_POOL_EXECUTOR.execute(mOnresumeModulesAndFeatureList);
        }
        if (mResetToPreviewOnResume) {
            mCameraAppUI.resume();
            mResetToPreviewOnResume = false;
        }
        mCameraAppUI.setSwitchBackGroundVisibility(View.GONE);
        THREAD_POOL_EXECUTOR.execute(mOnResumeUriPermission);
        mCameraRequestHandler.post(mOnResumeForPerform);
        mCounterPause = new Counter(2);
        if (!mSecureCamera) {
            try {
                resume();
            } catch (AssertionError e) {
                Log.e(TAG, "Creating camera controller failed.", e);
                mFatalErrorHandler.onGenericCameraAccessFailure();
            }
        } else {
            // In secure mode from lockscreen, we go straight to camera and will
            // show first run dialog next time user enters launcher.
            Log.i(TAG, "in secure mode, skipping first run dialog check");
            /**
             * if start SecureCameraActivity,update RoundedThumbnailView here
             */
            runSyncThumbnail();
            resume();
        }
        /*
         Sprd: bug 868190 dont show welcome view when use capture intent to start Camera firstly
         */
        if (!isCaptureIntent() && !mVoiceIntent) {
            showCameraWelcome();
        }
        updateShutterSound();
        if (CameraUtil.getLowBatteryNoFlashLevel() > 0) {
            mBatteryReceiver = new BatteryBroadcastReciver();
            IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mBatteryReceiver, batteryIntentFilter);
        }

        // SPRD: Fix bug 572631, optimize camera launch time
        mExtraWorkNeeded = true;
        Log.i(TAG, "onResume end!");
    }

    private Runnable mOnResumeUriPermission = new Runnable() {

        @Override
        public void run() {
            String storagePath = mDataModule.getString(Keys.KEY_CAMERA_STORAGE_PATH);
            StorageUtil storageUtil = StorageUtil.getInstance();
            waitUpdateSettingsCounter();
            if(storageUtil.getCurrentAccessUri(storagePath) == null) {
                Log.i(TAG,"mOnResumeUriPermission changeSettings to internal ");
                mDataModule.changeSettings(Keys.KEY_CAMERA_STORAGE_PATH,MultiStorage.KEY_DEFAULT_INTERNAL);
            }
        }
    };

    private Runnable mOnResumeForPerform = new Runnable() {

        @Override
        public void run() {
            Profile profile = mProfiler.create("mOnResumeForPerform").start();
            /* SPRD: add for mutex between float window and camera @{ */
            Intent floatWindowIntent = new Intent();
            floatWindowIntent.setAction("android.gallery3d.action.FloatWindowAIDLService");
            floatWindowIntent.setPackage("com.android.gallery3d");
            mServiceBinded = bindService(floatWindowIntent, mIFloatWindowServiceConn, BIND_AUTO_CREATE);
            /* @} */
            profile.mark("FloatWindowAIDLService");

            mOrientationManager.resume();
            profile.mark("mOrientationManager.resume");

            // Enable location recording if the setting is on.
            updateLocationRecordingEnabled();
            mMotionManager.start();
            profile.stop();
        }
    };

    private long getDestoryDelayTimes() {
       long systemSleepTime = Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_OFF_TIMEOUT,-1);
       long time = 0;
       if (systemSleepTime < 60000) {
           time = 60000;
       } else {
           time = 120000;
       }
       return time;
    }

    /* SPRD: add for new feature: camera welcome */
    private void showCameraWelcome() {
        boolean shouldWelcome = DataModuleManager.getInstance(this)
                .getDataModuleCamera().getBoolean(Keys.KEY_CAMERA_WELCOME);

        if(shouldWelcome == true){
            ViewStub viewStub = (ViewStub) findViewById(R.id.layout_dream_welcome_id);
            if(viewStub != null){
                viewStub.inflate();
            }
        }
        View welcomeView = findViewById(R.id.dream_welcome);

        if (welcomeView != null) {
            if (shouldWelcome == true) {
                Log.i(TAG, "show camera welcome interface");
                CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox_save_location);
                if (checkBox != null) {
                    if (isSupportGps) {
                        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView,
                                    boolean isChecked) {
                                Log.i(TAG, "the checkbox isChecked = " + isChecked);
                                DataModuleManager.getInstance(CameraActivity.this)
                                        .getDataModuleCamera()
                                        .set(Keys.KEY_RECORD_LOCATION, isChecked);
                            }
                        });
                        checkBox.setChecked(false);
                    } else {
                        checkBox.setVisibility(View.INVISIBLE);
                    }

                }
                welcomeView.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "hide camera welcome interface");
                welcomeView.setVisibility(View.GONE);
            }
        }
    }
    /* @} */

    /**
     * Checks if any of the needed Android runtime permissions are missing. If they are, then launch
     * the permissions activity under one of the following conditions: a) The permissions dialogs
     * have not run yet. We will ask for permission only once. b) If the missing permissions are
     * critical to the app running, we will display a fatal error dialog. Critical permissions are:
     * camera, microphone and storage. The app cannot run without them. Non-critical permission is
     * location.
     */
    private void checkPermissions() {
        if (!ApiHelper.isMOrHigher()) {
            Log.v(TAG, "not running on M, skipping permission checks");
            mHasCriticalPermissions = true;
            return;
        }

        if (!mFlagForResumeFeaturelist)
            mFlagForResumeFeaturelist = false;
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mHasCriticalPermissions = true;
        } else {
            mFlagForResumeFeaturelist = true;
            mHasCriticalPermissions = false;
        }
        if (mHasCriticalPermissions
                &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mHasCriticalPermissions = true;
        } else {
            mHasCriticalPermissions = false;
        }

        /* SPRD:fix bug 611957 check gps permission should not continue to do task @ */
        if (isSupportGps
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                !DataModuleManager.getInstance(this).getDataModuleCamera().getBoolean(Keys.KEY_HAS_SEEN_PERMISSIONS_DIALOGS)) {
            mHasCriticalPermissions = false;
        }
        /* @ */

        if (/*(isSupportGps
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                !DataModuleManager.getInstance(this).getDataModuleCamera().getBoolean(Keys.KEY_HAS_SEEN_PERMISSIONS_DIALOGS))
                ||*/
                !mHasCriticalPermissions) {
            // SPRD:fix bug501493,502701,498680,499555 problem caused by permission strategy
            Intent cameraIntent = getIntent();
            Bundle data = new Bundle();
            data.putParcelable("cameraIntent", cameraIntent);
            Intent intent = new Intent(this, PermissionsActivity.class);
            /* SPRD:fix bug498680 Open camera from contact,it shows modelistview @{ */
            // intent.setAction(getIntent().getAction());
            intent.putExtras(data);
            /* }@ */
            /* SPRD:fix bug519999 Create header photo, pop permission error */
            if (!isCaptureIntent()) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);//SPRD:fix bug704535/1080851
                startActivity(intent);
                finish();
            } else {
                startActivityForResult(intent, 1);
            }
        }
    }

    private void preloadFilmstripItems() {
        if (mDataAdapter == null) {
            mDataAdapter = new CameraFilmstripDataAdapter(mAppContext,
                    mPhotoItemFactory, mVideoItemFactory);
            mDataAdapter.setLocalDataListener(mFilmstripItemListener);
            mPreloader = new Preloader<Integer, AsyncTask>(FILMSTRIP_PRELOAD_AHEAD_ITEMS,
                    mDataAdapter,
                    mDataAdapter);
            if (mSecureCamera) {
                // Put a lock placeholder as the last image by setting its date to
                // 0.
                ImageView v = (ImageView) getLayoutInflater().inflate(
                        R.layout.secure_album_placeholder, null);
                v.setTag(R.id.mediadata_tag_viewtype,
                        FilmstripItemType.SECURE_ALBUM_PLACEHOLDER.ordinal());
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startGallery();
                        finish();
                    }
                });
                v.setContentDescription(getString(R.string.accessibility_unlock_to_camera));
                mDataAdapter = new FixedLastProxyAdapter(
                        mAppContext,
                        mDataAdapter,
                        new PlaceholderItem(
                                v,
                                FilmstripItemType.SECURE_ALBUM_PLACEHOLDER,
                                v.getDrawable().getIntrinsicWidth(),
                                v.getDrawable().getIntrinsicHeight()));
                // Flush out all the original data.
                mDataAdapter.clear();
            }
        }
    }

    private void resume() {
        Profile profile = mProfiler.create("CameraActivity.resume").start();
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_RESUME);
        if (!mCameraAppUI.getPanel() && !isFilmstripVisible()) {
            updateUI();
            mCameraAppUI.setPanel(true);
        }
        mCameraAppUI.resetIndicatorIcon();// SPRD:Add for bug 507813 The flash icon is not sync
        mCameraAppUI.reAddDreamUIPrefRestListener();

        /**
         * SPRD: Fix bug 572631, optimize camera launch time, done in onPreviewStarted() @{
         * Original Code
         *
        updateStorageSpaceAndHint(null);
         */

        mLastLayoutOrientation = getResources().getConfiguration().orientation;

        // Foreground event logging. ACTION_STILL_IMAGE_CAMERA and
        // INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE are double logged due to
        // lockscreen onResume->onPause->onResume sequence.

        mGalleryIntent = IntentHelper.getGalleryIntent(mAppContext);
//        if (ApiHelper.isLOrHigher()) {
            // hide the up affordance for L devices, it's not very Materially
//            mActionBar.setDisplayShowHomeEnabled(false);
//        }
        mActionBar.setDisplayShowHomeEnabled(false);

        profile.mark();
        Log.i(TAG, "mCurrentModule:" + mCurrentModule);

        /*
         * sync settings @{
         */
        skipFirstChangeonResume();
        /* @} */
        DataModuleManager.getInstance(this).getDataModuleCamera().addListener(this,this);
        DataModuleManager.getInstance(this).addListener(this);

        /* SPRD: Fix bug 605818, wait for background camera closed @{
         * Original code
         *
        mCurrentModule.resume();
         */
        waitToModuleResume();
        /* @} */
        //SPRD:fix bug 613799 save the panorama exception
        if(!mCurrentModule.isSavingPanorama()){
           getCameraAppUI().showCurrentModuleTip(mCurrentModeIndex);
        }

        setSwipingEnabled(true);
        waitToChangeMode = false;
        profile.mark("mCurrentModule.resume");

        if (!mResetToPreviewOnResume) {
            rollTask(new Runnable() {
                @Override
                public void run() {
                    if (isDestroyed()) return;
                    if(!mSecureCamera) {
                        FilmstripItem item = mDataAdapter.getItemAt(
                                mFilmstripController.getCurrentAdapterIndex());
                        if (item != null) {
                            mDataAdapter.refresh(item.getData().getUri());
                        }
                    } else {
                        FilmstripItem item = null;
                        for (int i = mDataAdapter.getCount() - 1 ; i >= 0 ; --i) {
                            item = mDataAdapter.getItemAt(i);
                            if (item != null)
                                mDataAdapter.refresh(item.getData().getUri());
                        }
                    }
                }
            });
        }

        // Default is showing the preview, unless disabled by explicitly
        // starting an activity we want to return from to the filmstrip rather
        // than the preview.
        mResetToPreviewOnResume = true;

        if (mLocalVideosObserver.isMediaDataChangedDuringPause()
                || mLocalImagesObserver.isMediaDataChangedDuringPause()) {
            if (!mSecureCamera) {
                // If it's secure camera, requestLoad() should not be called
                // as it will load all the data.
                /*
                 * SPRD:fix bug520618 Video is still appear if it is deleted from fileManager@{
                 * original code if (!mFilmstripVisible) {
                 */
                //boolean haveDeletion = getFilmStripCount() < mFilmStripCount;
                if (!mFilmstripVisible) {
                    /* @} */
                    mDataAdapter.requestLoad(new Callback<Void>() {
                        @Override
                        public void onCallback(Void result) {
                            // SPRD: Fix bug 605974 that should update current item
                            updateUiByData(mFilmstripController.getCurrentAdapterIndex());
                        }
                    });
                } else {
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (mFilmStripCount > getFilmStripCount()) {
                                    mDataAdapter.requestLoad(new Callback<Void>() {
                                        @Override
                                        public void onCallback(Void result) {
                                            fillTemporarySessions();
                                            // SPRD: Fix bug 605974 that should update current item
                                            updateUiByData(mFilmstripController.getCurrentAdapterIndex());
                                        }
                                    });
                                }
                            }
                        });
                }
            } else {
                mDataAdapter.requestLoadSecure(new Callback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        runSyncThumbnail();
                    }
                });
            }
        }
        mLocalImagesObserver.setActivityPaused(false);
        mLocalVideosObserver.setActivityPaused(false);
        /* SPRD:fix bug932732 observer the change of data @{ */
        if (!mSecureCamera) {
            mLocalImagesObserver.setForegroundChangeListener(
                    new FilmstripContentObserver.ChangeListener() {
                        @Override
                        public void onChange(Uri uri) {
                            String lastSegment = null;
                            if (uri != null)
                                lastSegment = uri.getLastPathSegment();
                            if (lastSegment != null && !TextUtils.isDigitsOnly(lastSegment) && mRedirectionUri != null) {
                                if (mCurrentModule.needLoadFilmstripItems()) {//Sprd:fix bug941988
                                    loadFilmstripItems();
                                }
                            }
                        }
                    });
        }
        /* @} */

        keepScreenOnForAWhile();

        // Lights-out mode at all times.
        final View rootView = findViewById(R.id.activity_root_view);
        mLightsOutRunnable.run();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        mMainHandler.removeCallbacks(mLightsOutRunnable);
                        mMainHandler.postDelayed(mLightsOutRunnable, LIGHTS_OUT_DELAY_MS);
                    }
                });

        profile.mark();
//        mPanoramaViewHelper.onResume();
//        profile.mark("mPanoramaViewHelper.onResume()");

        final int previewVisibility = getPreviewVisibility();
        updatePreviewRendering(previewVisibility);

        mResumed = true;
        profile.stop();
    }

    public boolean isActivityResumed() {
        return mResumed;
    }

    private void fillTemporarySessions() {
        Log.i(TAG, "fillTemporarySessions");
        if (mSecureCamera) {
            return;
        }
        /*
         * Add for thumbnail is not show when entry camera.
         * @{
         */
        if (mCurrentModule != null && !mCurrentModule.isFreezeFrameDisplayShow()){
            runSyncThumbnail();
        }
        /* @} */
    }

    @Override
    public void onStartTasks() {
        Log.i(TAG, "onStartTasks start!");
        /* SPRD: fix bug689991 Add Mode cover for captureIntent @{ */
        if (isCaptureIntent())
            mCameraAppUI.showModeCoverUntilPreviewReady();
        /* @} */
        mDestoryDelayTimes = getDestoryDelayTimes();//SPRD:fix bug925560
        /* SPRD: Fix bug 583585, Turn off camera permissions on the fixed screen mode ,Will be reported to the wrong @{ */
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onStartTasks: Missing critical permissions.");
            return;
        }
        /* @} */
        mIsActivityRunning = true;
//        mPanoramaViewHelper.onStart();

        /*
         * If we're starting after launching a different Activity (lockscreen), we need to use the
         * last mode used in the other Activity, and not the old one from this Activity. This needs
         * to happen before CameraAppUI.resume() in order to set the mode cover icon to the actual
         * last mode used. Right now we exclude capture intents from this logic.
         */
        int modeIndex = getModeIndex();
        /* Bug #533869 new feature: check UI 27,28: dream camera of intent capture @{ */
        Log.i(TAG, "Keys.KEY_CAMERA_SWITCH " + mDataModule.getBoolean(Keys.KEY_CAMERA_SWITCH));
        Log.i(TAG, "Keys.KEY_INTENT_CAMERA_SWITCH " +
                mDataModule.getBoolean(Keys.KEY_INTENT_CAMERA_SWITCH));
        if (!isCaptureIntent() && mDataModule.getBoolean(Keys.KEY_INTENT_CAMERA_SWITCH)) {
            int intentModeIndex;
            int module = mCurrentModule.getMode();
            int nextCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
            DreamUtil dreamUtil = new DreamUtil();
            intentModeIndex = dreamUtil.getRightMode(mDataModule,
                    module, nextCameraId);
            onModeSelected(intentModeIndex);
        } else if (!isCaptureIntent() && mCurrentModeIndex != modeIndex
                || DreamUtil.getRightCamera(mDataModule.getInt(Keys.KEY_CAMERA_ID))
                != mCameraAppUI.getDreamUI().getTopPanelValue() && mCurrentModule.getModuleTpye() != DreamModule.QR_MODULE) {
            onModeSelected(modeIndex);
        }
        mCameraAppUI.showModeCoverUntilPreviewReady(); //Sprd:fix bug940224 bug1001985
        if (!isCaptureIntent()) {
            mDataModule.set(Keys.KEY_INTENT_CAMERA_SWITCH, false);
        } else {
            mDataModule.set(Keys.KEY_CAMERA_SWITCH, false);
        }
        /* @} */
        Log.i(TAG, "onStartTasks end!");
    }

    @Override
    protected void onStopTasks() {
        mResumed = false;
        Log.i(TAG, "onStopTasks start");
        /* SPRD: Fix bug 583585, Turn off camera permissions on the fixed screen mode ,Will be reported to the wrong @{ */
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onStartTasks: Missing critical permissions.");
            return;
        }
        /* @} */
        /* SPRD: Fix bug 630560 camera will occur error when press power key twice @{*/
        mPaused = true;
        /* @} */
        mCameraAppUI.stop();

        mIsActivityRunning = false;
//        mPanoramaViewHelper.onStop();

        mLocationManager.disconnect();

        Log.i(TAG, "onStop end!");
    }

    @Override
    public void onDestroyTasks() {
        Log.i(TAG, "onDestroyTasks start!");
        CameraUtil.release();
        mPermissionDialog = null;
        releaseHalAndListener();
        releaseRes();
        if (dreamHandlerThread != null) {
            dreamHandlerThread.quit();
        }
        /* @} */

        if (!isInMultiWindowMode() && mSuccess) {
            CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.API_1);
            CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.AUTO);
        }

        Log.i(TAG, "onDestroy end!");
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        if (isInMultiWindowMode()) {
            CameraUtil.toastHint(this, R.string.multi_window_tip);
            finish();
            return;
        }


        /* SPRD: Fix bug 761069, do not have permissions, do nothing@{ */
        if (!mHasCriticalPermissions) {
            Log.i(TAG, "onConfigurationChanged: Missing critical permissions.");
            return;
        }
        /* @} */

        Log.v(TAG, "onConfigurationChanged");
        if (config.orientation == Configuration.ORIENTATION_UNDEFINED) {
            return;
        }

        if (mLastLayoutOrientation != config.orientation && null != mCurrentModule) {//SPRD: fix bug 712479
            mLastLayoutOrientation = config.orientation;
            mCurrentModule.onLayoutOrientationChanged(
                    mLastLayoutOrientation == Configuration.ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK && mCurrentModule != null && !mCurrentModule.isCameraAvailable()) return true;
        //Sprd:fix bug1002522
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && isPhoneRing()){
            return true;
        }
        if (!mFilmstripVisible && !mCameraAppUI.isModeListOpen() && !mCameraAppUI.isSettingLayoutOpen()) {
            if (mCurrentModule != null && mCurrentModule.onKeyDown(keyCode, event)) {
                return true;
            }
            // Prevent software keyboard or voice search from showing up.
            if (keyCode == KeyEvent.KEYCODE_SEARCH
                    || keyCode == KeyEvent.KEYCODE_MENU) {
                if (event.isLongPress()) {
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (consumeKeyEvent(keyCode))
            return true;
        //Sprd:fix bug1002522
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && isPhoneRing()){
            return true;
        }
        if (inPreview()) {
            // If a module is in the middle of capture, it should
            // consume the key event.
            if (mCurrentModule.onKeyUp(keyCode, event)) {
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // Let the mode list view consume the event.
                if (!isCaptureIntent()
                        && !mCameraAppUI.isBottomBarNull()
                        && getCameraAppUI().getSwipeEnable() // SPRD: FixBug 828228
                        && !mCameraAppUI.isInFreezeReview()) { // SPRD: FixBug 395783
                    mCameraAppUI.openModeList();
                }
                return true;
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                if (mCameraAppUI.isModeListOpen()) {
                    mCameraAppUI.openModeList();
                    return true;
                // SPRD: Fix bug 687144, details menu is shown in SettingsLayout
                } else if (mCameraAppUI.isSettingLayoutOpen()) {
                    return true;
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

//    @Override
//    public void finish() {
//        super.finish();
//        this.overridePendingTransition(0, R.anim.main_task_close_exit);
//    }

   //SPRD:613721  contacts stop running after closed permission when lock the screen begin
    public boolean isInLockTaskMode() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public void onBackPressed() {
        Log.i(TAG,"onBackPressed");
        if (isInLockTaskMode()) {
            showLockTaskEscapeMessage();
            return;
        }

        if (!mCameraAppUI.onBackPressed()) {
            /* SPRD: fix bug 556990 Avoid crash when the onBackPressed run after onSaveInstanceState @{*/
            /*
            if (!mCurrentModule.onBackPressed()) {
            */
            if (!mCurrentModule.onBackPressed() && isActivityResumed()) {
                super.onBackPressed();
            }
            /* }@ */
        }
    }

    @Override
    public boolean isAutoRotateScreen() {
        // TODO: Move to OrientationManager.
        return mAutoRotateScreen;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filmstrip_menu, menu);
        mActionBarMenu = menu;

        /*
         * SPRD:Fix bug 494050 First come in Camera,then capture,the gallery icon in camera
         * disappears@{
         */
        if (mGalleryIntent == null) {
            mGalleryIntent = IntentHelper.getGalleryIntent(mAppContext);
        }
        /* @} */
        // add a button for launching the gallery
        if (mGalleryIntent != null) {
            CharSequence appName = IntentHelper.getGalleryAppName(mAppContext, mGalleryIntent);
            if (appName != null) {
                MenuItem menuItem = menu.add(appName);
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menuItem.setIntent(mGalleryIntent);
                /* SPRD:Fix bug 496077 Long press the gallery icon in Camera, and show error hint@ */
                menuItem.setTitle(R.string.switch_to_gallery);
                /* @} */

                Drawable galleryLogo = IntentHelper.getGalleryIcon(mAppContext, mGalleryIntent);
                if (galleryLogo != null) {
                    menuItem.setIcon(galleryLogo);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        if (isSecureCamera() && !ApiHelper.isLOrHigher()) {
            // Compatibility pre-L: launching new activities right above
            // lockscreen does not reliably work, only show help if not secure
            /*
             * SPRD Bug:488399 Remove Google Help and Feedback. @{ Original Android code:
             * menu.removeItem(R.id.action_help_and_feedback);
             */
//        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * SPRD:fix bug 474690 modify for Pano original code protected long getStorageSpaceBytes() {
     */
    public long getStorageSpaceBytes() {
        /**
         * @}
         */
        synchronized (mStorageSpaceLock) {
            return mStorageSpaceBytes;
        }
    }

    public long getInternalStorageSpaceBytes(){
        /**
         * @}
         */
        synchronized (mStorageSpaceLock) {
            return mInternalSpaceBytes;
        }
    }

    protected interface OnStorageUpdateDoneListener {
        public void onStorageUpdateDone(long bytes);
    }

    /**
     * SPRD:fix bug 474690 modify for Pano original code protected void
     * updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback) {
     */
    public void updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback) {
        /**
         * @}
         */
        /*
         * We execute disk operations on a background thread in order to free up the UI thread.
         * Synchronizing on the lock below ensures that when getStorageSpaceBytes is called, the
         * main thread waits until this method has completed. However, .execute() does not ensure
         * this execution block will be run right away (.execute() schedules this AsyncTask for
         * sometime in the future. executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) tries to
         * execute the task in parellel with other AsyncTasks, but there's still no guarantee). e.g.
         * don't call this then immediately call getStorageSpaceBytes(). Instead, pass in an
         * OnStorageUpdateDoneListener.
         */
        (new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... arg) {
                synchronized (mStorageSpaceLock) {
                    StorageUtil storageutil = StorageUtil.getInstance();
                    mInternalSpaceBytes = storageutil.getInternalAvaliableSpace();
                    mStorageSpaceBytes = storageutil.getAvailableSpace();
                    return mStorageSpaceBytes;
                }
            }

            @Override
            protected void onPostExecute(Long bytes) {
                updateStorageHint(bytes);
                // This callback returns after I/O to check disk, so we could be
                // pausing and shutting down. If so, don't bother invoking.
                if (callback != null && !mPaused) {
                    callback.onStorageUpdateDone(bytes);
                } else {
                    Log.v(TAG, "ignoring storage callback after activity pause");
                }

                // SPRD: Fix bug 572631, optimize camera launch time
                Log.i(TAG, "updateStorageSpaceAndHint end");
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * SPRD:fix bug 474690 modify for Pano protected void updateStorageHint(long storageSpace) {
     */
    public void updateStorageHint(long storageSpace) {
        if (!mIsActivityRunning) {
            return;
        }
        if (mCurrentModule instanceof PhotoModule && ((PhotoModule)mCurrentModule).isImageCaptureIntent()){
            return;
        }
        String message = null;
        if (storageSpace == Storage.UNAVAILABLE) {
            message = getString(R.string.no_storage);
        } else if (storageSpace == Storage.PREPARING) {
            message = getString(R.string.preparing_sd);
        } else if (storageSpace == Storage.UNKNOWN_SIZE) {
            message = getString(R.string.access_sd_fail);
        } else if (getInternalStorageSpaceBytes() <= Storage.MIN_INTERNAL_STORAGE_THRESHOLD_BYTES){// internal storage is toot low to can`t insert database
            message = getString(R.string.Internal_spaceIsMIN_content);
        } else if (storageSpace >> 20 <= Storage.LOW_STORAGE_THRESHOLD_BYTES >> 20) { // SPRD: fix bug 902106 @{ */
            // to avoid the case such as: 50003968 < 50000000 , but 3968 bytes cannot save new video file
            // use MB instead of B to compare. B >> 20 = MB @} */

            /* SPRD: fix bug 522261 Show different string with External and Internal @{ */
            if (getCurrentStorage().equals("External")) {
                message = getString(R.string.spaceIsLow_content);
            } else if (getCurrentStorage().equals("Internal")) {
                message = getString(R.string.Internal_spaceIsLow_content);
            }else if (getCurrentStorage()!=null) {//SPRD: Fix bug 667316,add low memory toast for OTG device
                message = getString(R.string.uDiskIsLow_content);
            }
            /* @} */
        }
        if (message != null) {
            Log.w(TAG, "Storage warning: " + message);
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(CameraActivity.this, message);
            } else {
                mStorageHint.setText(message);
            }
            mStorageHint.show();

            // Disable all user interactions,
            /*
             * SPRD:Fix bug 427865 mCameraAppUI.setDisableAllUserInteractions(true);
             */
            mCameraAppUI.setDisableAllUserInteractions(false);
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;

            // Re-enable all user interactions.
            mCameraAppUI.setDisableAllUserInteractions(false);
        }
    }

    /**
     * SPRD: Change for New Feature scenery original code
     *
     * @{ protected void setResultEx(int resultCode) {
     */
    public void setResultEx(int resultCode) {
        /**
         * @}
         */
        mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    /**
     * SPRD: Change for New Feature scenery original code
     *
     * @{ protected void setResultEx(int resultCode, Intent data) {
     */
    public void setResultEx(int resultCode, Intent data) {
        /**
         * @}
         */
        mResultCodeForTesting = resultCode;
        mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return mResultCodeForTesting;
    }

    public Intent getResultData() {
        return mResultDataForTesting;
    }

    @Override
    public boolean isSecureCamera() {
        return mSecureCamera;
    }

    @Override
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public int getPreferredChildModeIndex(int modeIndex) {

        return modeIndex;
    }

    @Override
    public void onModeSelected(int modeIndex) {
        Log.i(TAG, "onModeSelected mCurrentModeIndex=" + mCurrentModeIndex + ",change to " + modeIndex);

        //SPRD: fix bug979853 Optimize switching speed
        if(modeIndex == SettingsScopeNamespaces.AR_PHOTO ||
                modeIndex == SettingsScopeNamespaces.AR_VIDEO) {
            int index = modeIndex;
            modeIndex = mCurrentModeIndex;
            mCameraAppUI.setSwitchBackGroundVisibility(View.VISIBLE);
            gotoAR(index);
            Log.d(TAG , "AR , back to previous module: " + modeIndex);
            return;
        }
        waitToChangeMode = true;
        /*
         * if (mCurrentModeIndex == modeIndex) { return; }
         */
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.MODE_SWITCH_START);
        // Record last used camera mode for quick switching
        if (modeIndex == SettingsScopeNamespaces.AUTO_PHOTO
                || modeIndex == SettingsScopeNamespaces.GCAM) {
            // Bug #533869 new feature: check UI 27,28: dream camera of intent capture
            if (!isCaptureIntent()) {
                DataModuleManager.getInstance(this)
                        .getDataModuleCamera().set(Keys.KEY_CAMERA_MODULE_LAST_USED, modeIndex);
            }
        }

        // SPRD: Fix bug 681215 optimization about that DV change to DC is slow
        mCameraRequestHandler.removeCallbacks(mPreSwitchRunnable);

        closeModule(mCurrentModule);
        // SPRD: Fix bug 572631, optimize camera launch time
        destroyModule(mCurrentModule);
        Log.i(TAG, "onModeSelected mCurrentModule=" + mCurrentModule + ",modeIndex=" + modeIndex);


        // Select the correct module index from the mode switcher index.
//        modeIndex = getPreferredChildModeIndex(modeIndex);
        // change current module, change camera if needed
        setModuleFromModeIndex(modeIndex);
        mCameraAppUI.dismissDialogIfNecessary();//SPRD:Bug845370 the index value has been changed when the dialog was not dismissed.

        openModule(mCurrentModule);
        /**
         * Every time mode selected, we should save to the corresponding camera mode
         * KEY_BACK_PHOTO_MODE/KEY_FRONT_PHOTO_MODE/KEY_BACK_VIDEO_MODE/KEY_FRONT_VIDEO_MODE
         *
         * @{
         */
        if (!isCaptureIntent()) { // Bug #533869 new feature: check UI 27,28: dream camera of intent capture
            new DreamUtil().savaToCameraMode(this, mDataModule,
                    mDataModule.getInt(Keys.KEY_CAMERA_ID), modeIndex);
        }
        /* @} */
        waitToChangeMode = false;

        long start = System.currentTimeMillis();
        Log.i(TAG, "resetBottomControls cost: " + (System.currentTimeMillis() - start));
    }

    private void gotoAR(int index) {
        Intent startAR = IntentHelper.getARIntent(this);
        if (startAR == null) {
            Log.w(TAG , "intent for AR is null , cannot start AR app");
            CameraUtil.toastHint(this, R.string.goto_ar_failed);
            return;
        }
        int cameraModel = (index == SettingsScopeNamespaces.AR_PHOTO) ? 0 : 1;
        startAR.putExtra("cameraModel" , cameraModel);
        startAR.putExtra("storage", StorageUtil.getInstance().getFileDir());
        startAR.putExtra("volumeStatus",getVolumeControlStatus());
        Log.i(TAG , "setAction & startActivity for AR , model = " + cameraModel + " , storage = " + StorageUtil.getInstance().getFileDir() +
            "volumeStatus = " + getVolumeControlStatus());
        startAR.setAction(Intent.ACTION_VIEW);
        startActivityForResult(startAR , 1);
    }

    private void freezeScreenCommon() {
        long startTime = System.currentTimeMillis();
        if (mCurrentModule.isUseSurfaceView()) {
            Size displayRealSize = CameraUtil.getDefaultDisplayRealSize();
            Bitmap screenShot = SurfaceControl.screenshot(new Rect(), displayRealSize.getWidth(), displayRealSize.getHeight(),Surface.ROTATION_0);
            if (screenShot == null) {
                freezeScreenUntilPreviewReady();//SPRD:fix bug1116034
            } else {
                Bitmap screenShotCopy = screenShot.createAshmemBitmap(Bitmap.Config.ARGB_8888);
                screenShot.recycle();
                //CameraUtil.saveBitmapToFile(screenShot);
                mCameraAppUI.freezeScreenUntilPreviewReady(screenShotCopy, true);
            }
        } else {
            freezeScreenUntilPreviewReady();
        }
        Log.i(TAG, "freezeScreenCommon cost: " + (System.currentTimeMillis() - startTime));
    }

    @Override
    public void freezeScreenUntilPreviewReady() {
        mCameraAppUI.freezeScreenUntilPreviewReady();
    }

    // SPRD: Fix 474843 Add for Filter Feature.
    @Override
    public void freezeScreenUntilPreviewReady(Bitmap bitmap) {
        mCameraAppUI.freezeScreenUntilPreviewReady(bitmap);
    }

    /* SPRD: Fix bug 595400 the freeze screen for gif @{ */
    @Override
    public void freezeScreenUntilPreviewReady(Bitmap bitmap, RectF previewArea) {
        mCameraAppUI.freezeScreenUntilPreviewReady(bitmap, previewArea);
    }
    /* @} */

    @Override
    public int getModuleId(int modeIndex) {
        ModuleManagerImpl.ModuleAgent agent = mModuleManager.getModuleAgent(modeIndex);
        if (agent == null) {
            return -1;
        }
        return agent.getModuleId();
    }

    /**
     * Sets the mCurrentModuleIndex, creates a new module instance for the given index an sets it as
     * mCurrentModule.
     */
    private void setModuleFromModeIndex(int modeIndex) {
        if (modeIndex == SettingsScopeNamespaces.AUTO_PHOTO) {
            if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                    Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID,"0")) == DreamUtil.BACK_CAMERA) {
                if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS,"0")) == 1)
                    modeIndex = SettingsScopeNamespaces.REFOCUS;
                else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_ULTRA_WIDE_ANGLE,"0")) == 1) // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
                    modeIndex = SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE;
                else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_back_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO,"0")) == 1)
                    modeIndex = SettingsScopeNamespaces.FILTER;
            } else if (Integer.parseInt(this.getSharedPreferences("camera_camera_setting",
                    Context.MODE_PRIVATE).getString(Keys.KEY_CAMERA_ID, "0")) == DreamUtil.FRONT_CAMERA) {
                if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_REFOCUS, "0")) == 1) {
                    modeIndex = SettingsScopeNamespaces.FRONT_BLUR;
                } else if (Integer.parseInt(this.getSharedPreferences("camera_category_photo_front_setting",
                        Context.MODE_PRIVATE).getString(Keys.KEY_FILTER_PHOTO, "0")) == 1) {
                    modeIndex = SettingsScopeNamespaces.FILTER;
                }
            }
        }
        ModuleManagerImpl.ModuleAgent agent = mModuleManager.getModuleAgent(modeIndex);
        if (agent == null) {
            return;
        }
        if (!agent.requestAppForCamera()) {
            mCameraController.closeCamera(true);
        }
        mCurrentModeIndex = agent.getModuleId();
        mCurrentModule = (CameraModule) agent.createModule(this, getIntent());
        if (!isCaptureIntent() && !mSecureCamera) { // Bug #533869 new feature: check UI 27,28: dream camera of intent capture
            DataModuleManager.getInstance(this).getTempCameraModule()
                    .set(Keys.KEY_STARTUP_MODULE_INDEX, mCurrentModeIndex);
        }
        Log.i(TAG, "setModuleFromModeIndex modeIndex=" + modeIndex);
    }

    @Override
    public ResolutionSetting getResolutionSetting() {
        return mResolutionSetting;
    }

    @Override
    public CameraServices getServices() {
        return CameraServicesImpl.instance();
    }

    @Override
    public FatalErrorHandler getFatalErrorHandler() {
        return mFatalErrorHandler;
    }

    public List<String> getSupportedModeNames() {
        List<Integer> indices = mModuleManager.getSupportedModeIndexList();
        List<String> supported = new ArrayList<String>();

        for (Integer modeIndex : indices) {
            String name = CameraUtil.mModuleInfoResovle.getModuleText(modeIndex);
            if (name != null && !name.equals("")) {
                supported.add(name);
            }
        }
        return supported;
    }

    @Override
    public ButtonManager getButtonManager() {
        if (mButtonManager == null) {
            mButtonManager = new ButtonManagerDream(this);
        }
        return mButtonManager;
    }

    @Override
    public SoundPlayer getSoundPlayer() {
        return mSoundPlayer;
    }

    /**
     * Launches an ACTION_EDIT intent for the given local data item. If 'withTinyPlanet' is set,
     * this will show a disambig dialog first to let the user start either the tiny planet editor or
     * another photo editor.
     *
     * @param data The data item to edit.
     */
    public void launchEditor(FilmstripItem data) {
        Log.i(TAG, "launchEditor isUcamEditSupport false");
        launchGalleryEdit(data);
    }

    public void launchGalleryEdit(FilmstripItem data) {
        Log.i(TAG, "launchGalleryEdit ImageFileName=" + data.getData().getFilePath());
        Intent intent = new Intent(Intent.ACTION_EDIT)
                .setDataAndType(data.getData().getUri(), data.getData().getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            launchActivityByIntent(intent);
        } catch (ActivityNotFoundException e) {
            final String msgEditWith = getResources().getString(R.string.edit_with);
            launchActivityByIntent(Intent.createChooser(intent, msgEditWith));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filmstrip_context_menu, menu);
    }

    /**
     * Launch the tiny planet editor.
     *
     * @param data The data must be a 360 degree stereographically mapped panoramic image. It will
     *            not be modified, instead a new item with the result will be added to the
     *            filmstrip.
     */
    public void launchTinyPlanetEditor(FilmstripItem data) {
        TinyPlanetFragment fragment = new TinyPlanetFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TinyPlanetFragment.ARGUMENT_URI, data.getData().getUri().toString());
        bundle.putString(TinyPlanetFragment.ARGUMENT_TITLE, data.getData().getTitle());
        fragment.setArguments(bundle);
        fragment.show(getFragmentManager(), "tiny_planet");
    }

    /**
     * Returns what UI mode (capture mode or filmstrip) we are in. Returned number one of
     * {@link com.google.common.logging.eventprotos.NavigationChange.Mode}
     */
    private int currentUserInterfaceMode() {
        int mode = NavigationChange.Mode.UNKNOWN_MODE;
        if (mCurrentModeIndex == SettingsScopeNamespaces.AUTO_PHOTO) {
            mode = NavigationChange.Mode.PHOTO_CAPTURE;
        }
        if (mCurrentModeIndex == SettingsScopeNamespaces.AUTO_VIDEO) {
            mode = NavigationChange.Mode.VIDEO_CAPTURE;
        }
        if (mCurrentModeIndex == SettingsScopeNamespaces.REFOCUS) {
            mode = NavigationChange.Mode.LENS_BLUR;
        }
        if (mCurrentModeIndex == SettingsScopeNamespaces.GCAM) {
            mode = NavigationChange.Mode.HDR_PLUS;
        }
        if (mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photosphere)) {
            mode = NavigationChange.Mode.PHOTO_SPHERE;
        }
        if (mCurrentModeIndex == SettingsScopeNamespaces.DREAM_PANORAMA) {
            mode = NavigationChange.Mode.PANORAMA;
        }
        if (mFilmstripVisible) {
            mode = NavigationChange.Mode.FILMSTRIP;
        }
        return mode;
    }

    private void openModule(CameraModule module) {
        long start = System.currentTimeMillis();
        module.init(this, isSecureCamera(), isCaptureIntent());
        Log.i(TAG, "init totally cost: " + (System.currentTimeMillis() - start));
        if(mCameraAppUI != null && mCameraAppUI.getModuleView() != null) {
            mCameraAppUI.getModuleView().setVisibility(View.VISIBLE);
        }

        if (mCameraAppUI != null)
            getCameraAppUI().updateModeList();

        if (mModeListView == null || mModeListView.getModeListSize() == 1 || isSecureCamera()) {
            SlidePanelManager.getInstance(this).udpateSlidePanelShow(SlidePanelManager.MODE,View.INVISIBLE);
        } else {
            SlidePanelManager.getInstance(this).udpateSlidePanelShow(SlidePanelManager.MODE,View.VISIBLE);
        }

        if (!mPaused) {
            Log.d(TAG, "module:" + module);
            module.resume();
            updatePreviewVisibility();
        }
    }

    /**
     * call module's pause life method and clear current module ui
     * @param module module object
     */
    private void closeModule(CameraModule module) {
        module.pause();
        mCameraAppUI.clearModuleUI();
    }

    /* SPRD: Fix bug 572631, optimize camera launch time @{ */
    private void destroyModule(CameraModule module) {
        module.destroy();
    }
    /* @} */

    /**
     * Enable/disable swipe-to-filmstrip. Will always disable swipe if in capture intent.
     *
     * @param enable {@code true} to enable swipe.
     */
    public void setSwipingEnabled(boolean enable) {
        // TODO: Bring back the functionality.
        if (isCaptureIntent()) {
            // lockPreview(true);
        } else {
            // lockPreview(!enable);
        }
    }

    // Accessor methods for getting latency times used in performance testing
    public long getFirstPreviewTime() {
        if (mCurrentModule instanceof PhotoModule) {
            long coverHiddenTime = getCameraAppUI().getCoverHiddenTime();
            if (coverHiddenTime != -1) {
                return coverHiddenTime - mOnCreateTime;
            }
        }
        return -1;
    }

    public long getAutoFocusTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mAutoFocusTime : -1;
    }

    public long getShutterLag() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mShutterLag : -1;
    }

    public long getShutterToPictureDisplayedTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mShutterToPictureDisplayedTime : -1;
    }

    public long getPictureDisplayedToJpegCallbackTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mPictureDisplayedToJpegCallbackTime : -1;
    }

    public long getJpegCallbackFinishTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mJpegCallbackFinishTime : -1;
    }

    public long getCaptureStartTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mCaptureStartTime : -1;
    }

    public boolean isRecording() {
        return (mCurrentModule instanceof VideoModule) ?
                ((VideoModule) mCurrentModule).isRecording() : false;
    }

    public CameraAgent.CameraOpenCallback getCameraOpenErrorCallback() {
        return mCameraController;
    }

    // For debugging purposes only.
    public CameraModule getCurrentModule() {
        return mCurrentModule;
    }

//    @Override
//    public void showTutorial(AbstractTutorialOverlay tutorial) {
//        mCameraAppUI.showTutorial(tutorial, getLayoutInflater());
//    }

    @Override
    public void finishActivityWithIntentCompleted(Intent resultIntent) {
        finishActivityWithIntentResult(Activity.RESULT_OK, resultIntent);
    }

    @Override
    public void finishActivityWithIntentCanceled() {
        finishActivityWithIntentResult(Activity.RESULT_CANCELED, new Intent());
    }

    private void finishActivityWithIntentResult(int resultCode, Intent resultIntent) {
        mResultCodeForTesting = resultCode;
        mResultDataForTesting = resultIntent;
        setResult(resultCode, resultIntent);
        finish();
    }

    private void keepScreenOnForAWhile() {
        if (mKeepScreenOn) {
            return;
        }
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_ON_FLAG, SCREEN_DELAY_MS);
    }

    private void resetScreenOn() {
        mKeepScreenOn = false;
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * @return {@code true} if the Gallery is launched successfully.
     */
    private boolean startGallery() {
        if (mGalleryIntent == null) {
            return false;
        }
        try {
            Intent startGalleryIntent = new Intent(mGalleryIntent);
            int currentIndex = mFilmstripController.getCurrentAdapterIndex();
            FilmstripItem currentFilmstripItem = mDataAdapter.getItemAt(currentIndex);
            if (currentFilmstripItem != null) {
                GalleryHelper.setContentUri(startGalleryIntent,
                        currentFilmstripItem.getData().getUri());
            }
            launchActivityByIntent(startGalleryIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Failed to launch gallery activity, closing");
        }
        return false;
    }

    private void setNfcBeamPushUriFromData(FilmstripItem data) {
        final Uri uri = data.getData().getUri();
        if (uri != Uri.EMPTY) {
            mNfcPushUris[0] = uri;
        } else {
            mNfcPushUris[0] = null;
        }
    }

    /**
     * Updates the visibility of the filmstrip bottom controls and action bar.
     */
    private void updateUiByData(final int index) {
        final FilmstripItem currentData = mDataAdapter.getItemAt(index);
        if (currentData == null) {
            Log.w(TAG, "Current data ID not found.");
            return;
        }
        if (currentData.getData() != null
                && FilmstripItemData.TYPE_THUMBNAIL == currentData.getData().getFileTag()) {
            Log.w(TAG, "updateUiByData Current data fileTag is TYPE_THUMBNAIL");
            return;
        }

        updateActionBarMenu(currentData);

        if (isSecureCamera()) {
            // We cannot show buttons in secure camera since go to other
            // activities might create a security hole.
            return;
        }

        setNfcBeamPushUriFromData(currentData);

        if (!mDataAdapter.isMetadataUpdatedAt(index)) {
            mDataAdapter.updateMetadataAt(index);
        }
    }

    private void showDetailsDialog(int index) {
        final FilmstripItem data = mDataAdapter.getItemAt(index);
        if (data == null) {
            return;
        }
        Optional<MediaDetails> details = data.getMediaDetails();
        if (!details.isPresent()) {
            return;
        }
        /*
         * SPRD: fix bug 498612 If the detailDialog is shown,it should dismiss when the activity is
         * onPause. Dialog detailDialog = DetailsDialog.create(CameraActivity.this, details.get());
         */
        detailDialog = DetailsDialog.create(CameraActivity.this, details.get());
        detailDialog.show();
    }

    /**
     * Show or hide action bar items depending on current data type.
     */
    private void updateActionBarMenu(FilmstripItem data) {
        if (mActionBarMenu == null) {
            return;
        }

        MenuItem detailsMenuItem = mActionBarMenu.findItem(R.id.action_details);
        if (detailsMenuItem == null) {
            return;
        }

        boolean showDetails = data.getAttributes().hasDetailedCaptureInfo();
        detailsMenuItem.setVisible(showDetails);
    }

    public void removeDataByUri(Uri uri) {
        int pos = mDataAdapter.findByContentUri(uri);
        Log.i(TAG, "pos = " + pos);
        if (pos >= 0) {
            mDataAdapter.removeAt(pos);
        }
    }

    public void loadFilmstripItemsForReset() {
        if (!mSecureCamera) {// SPRD:Fix bug 520083
            if (!isCaptureIntent()) {
                // SPRD: Fix bug 656429, record FilmstripItems loading start time for debug
                mFilmstripLoadStartTime = SystemClock.uptimeMillis();
                mDataAdapter.requestLoad(new Callback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        fillTemporarySessions();
                    }
                });
            }
        }
    }

    /*
     * SPRD: Fix bug 572631, optimize camera launch time
     * @see preloadFilmstripItems()
     */
    public void loadFilmstripItems() {
        if (!mSecureCamera) {// SPRD:Fix bug 520083
            if (!isCaptureIntent()) {
                // SPRD: Fix bug 656429, record FilmstripItems loading start time for debug
                mFilmstripLoadStartTime = SystemClock.uptimeMillis();
                mDataAdapter.requestLoad(new Callback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        fillTemporarySessions();
                    }
                });
            }
        }
    }
    /* @} */

    /*
     * SPRD Bug:474704 Feature:Video Recording Pause. @{
     */
    public void onPauseClicked(View v) {
        if (mCurrentModule != null) {
            if (mCurrentModule instanceof VideoModule) {
                VideoModule videoModule = (VideoModule) mCurrentModule;
                videoModule.onPauseClicked();
            }
        }
    }

    /**
     * Dream Camera takeASnapshot
     * @param v
     */
    public void takeASnapshot(View v) {
        if (mCurrentModule != null) {
            if (mCurrentModule instanceof VideoModule) {
                VideoModule videoModule = (VideoModule) mCurrentModule;
                videoModule.takeASnapshot();
            }
        }
    }

    /* @} */
    // SPRD:Fix bug 399745
    public boolean isFilmstripCoversPreview() {
        return mFilmstripCoversPreview;
    }

    /*
     * PRD: fix bug474859 Add new Feature of Scenery. @{
     */
    public boolean isFilmstripVisible() {
        return mFilmstripVisible;
    }
    /* @} */

    /* SPRD:fix bug498710 When SoundRecording is running,Camera can not record video @{ */
//    protected boolean proxyIsAudioRecording() {
//        Log.i(TAG, "Check whether record device is running");
//        boolean result = false;
//        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//        result = audioManager.isAudioRecording();
//        return result;
//    }

    /* }@ */

    //SPRD:fix bug535425 Open Camera from gallery, gif can not form
    //private boolean returnFromOtherActivity = false;
    /* SPRD:fix bug519999 Create header photo, pop permission error */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult resultcode:" + resultCode);
        if (resultCode == RESULT_CANCELED && isCaptureIntent()) {
            finish();
        }
        if (requestCode == mRequestCode && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri persistUri = data.getData();
                String documentId = DocumentsContract.getTreeDocumentId(persistUri);
                if (!documentId.endsWith(":") || "primary:".equals(documentId)) {
                    CameraUtil.toastHint(this, R.string.external_access_failed);
                    //SPRD:fix bug535425 Open Camera from gallery, gif can not form
                    //returnFromOtherActivity = true;
                    if (mCurrentModule != null) {
                        mCurrentModule.onActivityResult(requestCode, resultCode, data);
                    }
                    return;
                }
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (persistUri != null) {
                    getContentResolver().takePersistableUriPermission(persistUri,takeFlags);
                    if (mRequestCode == SCOPE_REQUEST_CODE_SD) {
                         mDataModule.changeSettings(Keys.KEY_CAMERA_STORAGE_PATH,MultiStorage.KEY_DEFAULT_EXTERNAL);
                    } else if (mRequestCode == SCOPE_REQUEST_CODE_OTG) {
                         mDataModule.changeSettings(Keys.KEY_CAMERA_STORAGE_PATH,mUsbKey);
                    }
                } else {
                    CameraUtil.toastHint(this, R.string.external_access_failed);
                }
            }
        } else if (requestCode == mRequestCode && resultCode == Activity.RESULT_CANCELED) {
            mFatalErrorHandler.onExternalStorageAccessFailure();
        }
        //SPRD:fix bug535425 Open Camera from gallery, gif can not form
        //returnFromOtherActivity = true;
        if (mCurrentModule != null) {
            mCurrentModule.onActivityResult(requestCode, resultCode, data);
        }
    }

    // SPRD:fix bug520618 Video is still appear if it is deleted from fileManager
    private int getFilmStripCount() {
        long start = System.currentTimeMillis();
        int imageCount = 0;
        int videoCount = 0;
        int sum = 0;
        Cursor imageCursor = null;
        Cursor videoCursor = null;

        try {
            imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    , new String[] {
                        "count(*)"
                    }, null, null, null);
            videoCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    , new String[] {
                        "count(*)"
                    }, null, null, null);
            if (imageCursor != null && imageCursor.moveToFirst()) {
                sum += imageCursor.getInt(0);
            }
            if (videoCursor != null && videoCursor.moveToFirst()) {
                sum += videoCursor.getInt(0);
            }
        } catch (Exception e) {
            Log.i(TAG, "getFilmStripCount " + e);
        } finally {
            if (imageCursor != null) {
                imageCursor.close();
            }
            if (videoCursor != null) {
                videoCursor.close();
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "getFilmStripCount cost: " + (end - start));
        return sum;
    }

    /* SPRD: fix bug 522261 Show different string with External and Internal @{ */
    private String getCurrentStorage() {
        return DataModuleManager.getInstance(this).getDataModuleCamera()
                .getString(Keys.KEY_CAMERA_STORAGE_PATH);
    }
    /* }@ */

    /**
     * SPRD: add fix bug 599645 @{
     * set broadcast receiver to listen sdcard and otg devices action
     * @param b BroadcastReceiver the receiver to listen
     */
    public void registerMediaBroadcastReceiver (BroadcastReceiver b) {
        mModuleMediaBroadcastReceiver = b;
    }

    public void unRegisterMediaBroadcastReceiver() {
        mModuleMediaBroadcastReceiver = null;
    }
    /* }@ */

    /* SPRD:fix bug641569 the storage path was changed after restart mobile @{ */
    public boolean shutdown = false;
    private class MyBroadcastReceiverShutDown extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_SHUTDOWN)){
                shutdown = true;
            }
        }
    }
    /* @} */

    private static String preAction;
    private static String preStorage;

    /* SPRD:fix bug524433 Filmstripview is not refresh when sdcard removed */
    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(shutdown){
                return;
            }
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_MEDIA_EJECT)){
                if(preAction == null ){
                    preAction = Intent.ACTION_MEDIA_EJECT;
                    preStorage = DataModuleManager.getInstance(CameraActivity.this)
                            .getDataModuleCamera().getString(Keys.KEY_CAMERA_STORAGE_PATH,"");
                }
                intent.putExtra("pre_storage", preStorage);
            } else {
                preAction = null;
                preStorage = null;
            }

            // SPRD: fix bug
            if (mModuleMediaBroadcastReceiver != null) {
                Log.d(TAG, "on media onReceive mModuleMediaBroadcastReceiver != null" + " , Activity.this = " + CameraActivity.this);
                mModuleMediaBroadcastReceiver.onReceive(context,intent);
                preAction = null; // Bug 1130870 , ref 723103
                // if SecureCameraActivity on foreground & CameraActivity on background ,
                // mModuleMediaBroadcastReceiver of CameraActivity is null
            }

            // fix CID 123735 : DLS: Dead local store (FB.DLS_DEAD_LOCAL_STORE)
            // Uri uri = intent.getData();
            String currentStorage = DataModuleManager.getInstance(CameraActivity.this)
                    .getDataModuleCamera().getString(Keys.KEY_CAMERA_STORAGE_PATH,"");
            StorageUtil storageutil = StorageUtil.getInstance();
            Log.d(TAG, "on media currentStorage: " + currentStorage);
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                /* SPRD: Fix bug 572473 add for usb storage support @{ */
                if (!StorageUtil.getStoragePathState(currentStorage)) {
                    Log.d(TAG, "on media scanner: " + currentStorage + " EJECT updatePreferredStorage");
                    updatePreferredStorage();
                } else {
                    // when usb storages are eject we should use this method reset preference list
                    storageutil.forceChangeStorageSetting(currentStorage);
                }
                /* @} */
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                Log.d(TAG, "on media scanner: " + action);

            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.d(TAG, "on media scanner: " + action);
                // when usb storages are mounted we should use this method reset preference list
                storageutil.forceChangeStorageSetting(currentStorage);
            }
            storageUpdateThread();
            // This line code can not be put in onDreamSettingChangeListener, Because
            // onDreamSettingChangeListener would be called in updateStorageSpaceAndHint() which in
            // storageUpdateThread that is not in main thread but a sub thread.
        }
    }

    private class BatteryBroadcastReciver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCameraController == null || mCameraController != null && mCameraController.isCameraProxyNull()){
                return;
            }

            int flashId;
            if (mCurrentModule instanceof VideoModule) {
                flashId = ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM;
            } else if (mCurrentModule instanceof ReuseModule) {
                flashId = ButtonManagerDream.BUTTON_GIF_PHOTO_FLASH_DREAM;
            } else {
                flashId = ButtonManagerDream.BUTTON_FLASH_DREAM;
            }
            View FlashButton = null;
            try {
                FlashButton = getButtonManager().getButtonOrError(flashId);
            } catch (IllegalStateException e) {
                Log.e(TAG, "flashId =" + flashId + " can not find");
                return;
            }

            int cameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
            if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED) && mCurrentModule.isFlashSupported()){
                final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mIsBatteryLow = level <= CameraUtil.getLowBatteryNoFlashLevel();
                Log.i(TAG, "action_battery_changed level = " + level + " , isBatteryLow = " + mIsBatteryLow);
                mCurrentModule.updateBatteryLevel(level);
            }
        }
    }
    /**
     * SPRD: add for feature bug 572473 OTG storage support
     * <p>
     * update preferred storage from storage list
     */
    public void updatePreferredStorage() {
        StorageUtil storageutil = StorageUtil.getInstance();
        List<String> supportedStorages = MultiStorage.getSupportedStorage();
        if (supportedStorages.contains(StorageUtil.KEY_DEFAULT_INTERNAL)) {
            storageutil.forceChangeStorageSetting(StorageUtil.KEY_DEFAULT_INTERNAL);
            // SPRD: Fix bug 577390, pictures captured from QuickCamera are always saved in built-in storage
            Settings.Global.putString(this.getContentResolver()
                    , "camera_quick_capture_storage_path", MultiStorage.KEY_DEFAULT_INTERNAL);
        } else if (supportedStorages.contains(StorageUtil.KEY_DEFAULT_EXTERNAL)) {
            storageutil.forceChangeStorageSetting(StorageUtil.KEY_DEFAULT_EXTERNAL);
            // SPRD: Fix bug 577390, pictures captured from QuickCamera are always saved in built-in storage
            Settings.Global.putString(this.getContentResolver()
                    , "camera_quick_capture_storage_path", MultiStorage.KEY_DEFAULT_EXTERNAL);
        } else {
            // TODO: in OTG mode to modify the quick camera
            if (supportedStorages.size() != 0) {
                storageutil.forceChangeStorageSetting(supportedStorages.get(0));
            }
        }
    }

    public void storageUpdateThread(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                if (mSecureCamera) { // Bug 1163428 , eject USB or SD card in secure camera
                    mDataAdapter.requestLoadSecure(new Callback<Void>() {
                        @Override
                        public void onCallback(Void result) {
                            runSyncThumbnail();
                        }
                    });
                } else {
                    mDataAdapter.requestLoad(new Callback<Void>() {
                        @Override
                        public void onCallback(Void result) {
                            fillTemporarySessions();
                            //SPRD:fix bug974863 & 962256
                            if (mFilmstripController != null){
                                mFilmstripController.setDataAdapter(mDataAdapter);
                            }
                        }
                    });
                }
                updateStorageSpaceAndHint(null);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * switch video and photo, and promise the mode is right. BACK_VIDEO <-> BACK_PHOTO /
     * FRONT_VIDEO <-> FRONT_PHOTO
     */
    public void switchMode(View v) {
        Log.i(TAG, "switchMode start");
        if (mCurrentModule.isShutterClicked() || waitToChangeMode
                || !mCurrentModule.isEnableDetector() //SPRD:fix bug766112
                ||mCameraAppUI.getFreezeScreenFlag()) {//SPRD:Bug731123 the light bulb icon is displaying in a unsuitable situation
            return;
        }
        long start = System.currentTimeMillis();

        waitToChangeMode = true;

        // SPRD: Fix bug 681215 optimization about that DV change to DC is slow
        mCameraRequestHandler.post(mPreSwitchRunnable);

        if (mCurrentModule.getModuleTpye() == DreamModule.FILTER_MODULE
                || mCurrentModule.getModuleTpye() == DreamModule.WIDEANGLE_MODULE) {
            mCurrentModule.freezeScreen(CameraUtil.isFreezeBlurEnable(), false);
            Log.i(TAG, "switchMode freezeScreen cost: " + (System.currentTimeMillis() - start));
        } else {
            cancelFlashAnimation();//SPRD:fix bug1252601
            freezeScreenCommon();
        }
        /*SPRD: fix bug 616844 make sure freeze view draw on time @{ */
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    switchModeSpecial();
                }
                waitToChangeMode = false;
            }
        });
        /* @} */
    }

    public boolean mIsSpecialPortraitOr3DNRToVideo = false;
    /* SPRD:fix bug616685 add freeze for module from gif to other @{ */
    public void switchModeSpecial() {
        int curModule = mCurrentModule.getMode();

        int nextModule = DreamUtil.VIDEO_MODE;
        switch (curModule) {
            case DreamUtil.PHOTO_MODE:
                nextModule = DreamUtil.VIDEO_MODE;
                break;
            case DreamUtil.VIDEO_MODE:
                nextModule = DreamUtil.PHOTO_MODE;
                break;
        }

        DreamUtil dreamUtil = new DreamUtil();

        int cameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
        int modeIndex;
        if(mIsSpecialPortraitOr3DNRToVideo){
            modeIndex = SettingsScopeNamespaces.AUTO_PHOTO;
            mIsSpecialPortraitOr3DNRToVideo = false;
        } else {
            modeIndex = dreamUtil.getRightMode(mDataModule,
                    nextModule, cameraId);
        }

        if(mDataModule.getBoolean(Keys.KEY_SWITCH_PHOTO_TO_VIDEO)){
            mIsSpecialPortraitOr3DNRToVideo = true;
        }
        // For BACK to BACK, FRONT to FRONT.
        mDataModule.set(Keys.KEY_CAMERA_ID, cameraId);

        mCameraAppUI.changeToVideoReviewUI();

        Log.d(TAG, "modeIndex=" + modeIndex);
        onModeSelected(modeIndex);
    }
    /* @} */

    /* SPRD: Fix bug 681215 optimization about that DV change to DC is slow @{ */
    private Runnable mPreSwitchRunnable = new Runnable() {
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            mCurrentModule.stopPreview();
            Log.i(TAG, "stopPreview cost: " + (System.currentTimeMillis() - start));
        }
    };
    /* @} */

    /**
     * switch camera front and back, and promise the mode is right. BACK_VIDEO <-> FRONT_VIDEO,
     * BACK_PHOTO <-> FRONT_PHOTO.
     */
    public void switchFrontAndBackMode() {
        Log.i(TAG, "switchFrontAndBackMode ");
        /* Bug #533869 new feature: check UI 27,28: dream camera of intent capture @{ */
        if (!isCaptureIntent()) {
            mDataModule.set(Keys.KEY_CAMERA_SWITCH, true);
        } else {
            mDataModule.set(Keys.KEY_INTENT_CAMERA_SWITCH, true);
        }
        int module = mCurrentModule.getMode();// current module

        int cameraId = mCurrentModule.getDeviceCameraId();// will change to camera id

        DreamUtil dreamUtil = new DreamUtil();

        if (DreamUtil.BACK_CAMERA == DreamUtil.getRightCamera(cameraId)) {
            nextCameraId = DreamUtil.getRightCameraId(DreamUtil.FRONT_CAMERA);
        } else {
            nextCameraId = DreamUtil.getRightCameraId(DreamUtil.BACK_CAMERA);
            int bCameraId = getCameraProvider().getFirstBackCameraId();
            if (nextCameraId != bCameraId) {
                nextCameraId = bCameraId;
            }
        }
        Log.d(TAG, "" + DreamUtil.BACK_CAMERA + "," + cameraId + "," + nextCameraId);
        int modeIndex;
        if(mDataModule.getBoolean(Keys.KEY_SWITCH_FRONTCAMERA_TO_BACK_AUTOMODE)){
            modeIndex = SettingsScopeNamespaces.AUTO_PHOTO;
            mDataModule.set(Keys.KEY_SWITCH_FRONTCAMERA_TO_BACK_AUTOMODE, false);
        } else {
            modeIndex = dreamUtil.getRightMode(mDataModule, module, nextCameraId);
        }
        // Bug #533869 new feature: check UI 27,28: dream camera of intent capture
        if (isCaptureIntent()) {
//            modeIndex = getResources().getInteger(R.integer.camera_mode_capture_intent);
            modeIndex = getModeIndex();
        }
        /* SPRD: Fix bug 595400 the freeze screen for gif @{ */
        final int moduleType = mCurrentModule.getModuleTpye();
        if ( moduleType != DreamModule.FILTER_MODULE
                && moduleType != DreamModule.QR_MODULE) {
            if (CameraUtil.isSwitchAnimationEnable()) {
                mCameraAppUI.startSwitchAnimation(null);
            }
            // SPRD: Fix bug 681215 optimization about that DV change to DC is slow
            mCameraRequestHandler.post(mPreSwitchRunnable);
            freezeScreenCommon();
        }
        final int index = modeIndex;
        /* SPRD:fix bug616685 add freeze for module from gif to other @{ */
        // TODO using lambda expression replace: mMainHandler.post(() -> onModeSelected(index));
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    // For BACK to FRONT, FRONT to BACK.
                    mDataModule.set(Keys.KEY_CAMERA_ID, nextCameraId);
                    onModeSelected(index);
                }
            }
        });

        mCameraAppUI.updateModeList();
    }
    /* @} */

    public Handler getMainHandler() {
        return mMainHandler;
    }

    public void onConfirmWelcome(View view) {
        View welcomeView = findViewById(R.id.dream_welcome);
        if (welcomeView != null) {
            welcomeView.setVisibility(View.GONE);
        }

        DataModuleManager.getInstance(this).getDataModuleCamera()
                .set(Keys.KEY_CAMERA_WELCOME, false);
        if (isSupportGps) {
            boolean recordLocationValue = DataModuleManager.getInstance(this).getDataModuleCamera()
                    .getBoolean(Keys.KEY_RECORD_LOCATION);
            DataModuleManager.getInstance(this)
                    .getDataModuleCamera()
                    .changeSettings(Keys.KEY_RECORD_LOCATION, recordLocationValue);
            Log.i(TAG, "set the Keys.KEY_RECORD_LOCATION value = " + recordLocationValue);
        }
    }

    public void onWelcome(View view) {
    }

    /**
     * Add for thumbnail is not show when entry camera. @{
     */
    public void syncThumbnail() {
        FilmstripItem data = mDataAdapter.getItemAt(0);
        if (mSecureCamera && mDataAdapter.getTotalNumber() == 1) {
            data = null;
        }
        if (data == null) {
            mRedirectionUri = null;
            if(!mCurrentModule.mIsFirstHasStartCapture()) {
                syncDefaultThumbnail();
            }
        } else {
            /* SPRD : Fix bug 910608 just sync thumbnail when thumbnail changed @{*/
            Uri uri = data.getData().getUri();
            if (uri != null && uri.equals(mRedirectionUri))
            {
                Log.d(TAG , "same Uri , do not show animation");
                return;
            }
            mRedirectionUri = uri;

            Log.d(TAG, "syncThumbnail");
            /* @} */
            if(mAboveFilmstripControlLayout==null){
                return;
            }
            int width = mAboveFilmstripControlLayout.getWidth();
            int height = mAboveFilmstripControlLayout.getMeasuredHeight();

            if (width == 0 && height == 0 && mMainHandler != null) {
                if(dreamHandler == null){
                    initHandlerThread();
                }

                if (dreamHandler != null) {
                    clearDreamHandler();
                    dreamHandler.postDelayed(thumbnailRunnable, 100);
                }
                return;
            }
            syncThumbnailFromData(data, width, height);
        }
    }

    private void syncDefaultThumbnail() {
        Bitmap defaultBitmap;
        if (!mSecureCamera) {
            defaultBitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R
                    .drawable.ic_gallery_defult_sprd);
        } else {
            defaultBitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R
                    .drawable.ic_gallery_defult_security_camera_sprd);
        }
        indicateCapture(defaultBitmap, 0);
        Log.d(TAG, "syncThumbnail default");
    }

    private void syncThumbnailFromData(FilmstripItem data, int width, int height) {
        InputStream stream = null;
        /* SPRD: Fix bug 656429, get thumbnail by EXIF to reduce time of updating thumbnail @{ */
        if (data.getItemViewType() == FilmstripItemType.PHOTO || data.getItemViewType() ==
                FilmstripItemType.BURST) {
            try {
                ExifInterface exif = new ExifInterface();
                stream = new FileInputStream(data.getData().getFilePath());
                exif.readExif(stream);
                Bitmap bitmap = exif.getThumbnailBitmap();
                if (bitmap != null) {
                    bitmap = CameraUtil.rotate(bitmap, Exif.getOrientation(exif));
                    indicateCapture(bitmap, 0);
                } else {
                    // Add for photos which cannot get thumbnail from Exif, such as photos taken by QuickCamera
                    final Optional<Bitmap> bmp = data.generateThumbnail(width, height);
                    if (bmp.isPresent()) {
                        indicateCapture(bmp.get(), 0);
                    } else {
                        Log.w(TAG , "Optional bitmap for image is absent , use default");
                        syncDefaultThumbnail();
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "syncThumbnail get thumbnail from exif error:" + e.getMessage());
                // Try again to get thumbnail by generateThumbnail() if exception thrown when readExif()
                final Optional<Bitmap> bmp = data.generateThumbnail(width, height);
                if (bmp.isPresent()) {
                    indicateCapture(bmp.get(), 0);
                }
            }finally {
                if (stream != null) { //fix bug698756 remove sdcard, camera stop run
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // do nothing here
                    }
                }
            }
        } else {
            final Optional<Bitmap> bitmap = data.generateThumbnail(width, height);
            if (bitmap.isPresent()) {
                indicateCapture(bitmap.get(), 0);
            } else {
                Log.w(TAG , "Optional bitmap is absent , use default");
                syncDefaultThumbnail();
            }
        }
        if (mFilmstripLoadStartTime != 0) {
            Log.d(TAG, "syncThumbnail update thumbnail cost:" + (SystemClock.uptimeMillis() - mFilmstripLoadStartTime));
        }
    }
    /* @} */

    /* SPRD:fix bug 624472 shutter sound not work when reset @{ */
    private void updateShutterSound() {
        mShutterSoundEnable = DataModuleManager.getInstance(this).getDataModuleCamera()
                .getBoolean(Keys.KEY_CAMERA_SHUTTER_SOUND);
    }

    @Override
    public void onSettingReset() {
        Log.d(TAG, "onSettingReset");
        updateShutterSound();
        updateLocationRecordingEnabled();
        int modeIndex = 0;
        if (mDataModuleManager.getCurrentDataModule() instanceof DataModulePhoto) {
            modeIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        } else {
            modeIndex = SettingsScopeNamespaces.AUTO_VIDEO;
        }
        final int finalModeIndex = modeIndex;
        if (DreamModule.FILTER_MODULE == mCurrentModule.getModuleTpye()) {
            mCurrentModule.freezeScreen(CameraUtil.isFreezeBlurEnable(), false);
        } else {
            if(!mCurrentModule.isUseSurfaceView()){
                freezeScreenUntilPreviewReady();
            } else {
                mCameraAppUI.freezeScreenBlackUntilPreviewReady();// SPRD:add for bug 667082
            }
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    loadFilmstripItemsForReset(); // SPRD: bug723260
                    onModeSelected(finalModeIndex);
                }
            }
        });
    }
    /* @} */

    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keys) {
        for (String key : keys.keySet()) {
            Log.e(TAG, "onDreamSettingChangeListener key = " + key + " value = " + keys.get(key));
            switch (key) {
                case Keys.KEY_QUICK_CAPTURE:
                    String mode = DataModuleManager.getInstance(this).getDataModuleCamera()
                            .getString(Keys.KEY_QUICK_CAPTURE);
                    Log.d(TAG, "Quick Capture mode id = " + mode);
                    // SPRD: add for bug 567394 porting from trunk branch
                    Settings.Global.putString(getContentResolver(), "camera_quick_capture_userid_"
                            + ActivityManager.getCurrentUser(), mode);
                    break;
                //dream test 11
                case Keys.KEY_CAMERA_SHUTTER_SOUND:
//                    mCurrentModule.updateParameter(Keys.KEY_CAMERA_SHUTTER_SOUND);
                    updateShutterSound();
                    break;
                case Keys.KEY_RECORD_LOCATION:
                    updateLocationRecordingEnabled();
                    break;
                // SPRD: Fix bug 593082 quickCamera storage abnormal
                case Keys.KEY_CAMERA_STORAGE_PATH:
                    String storagePath = mDataModule.getString(Keys.KEY_CAMERA_STORAGE_PATH);
                    if(StorageUtil.KEY_DEFAULT_EXTERNAL.equals(storagePath)){
                        Settings.Global.putString(getContentResolver(),
                                "camera_quick_capture_storage_path",
                                MultiStorage.KEY_DEFAULT_EXTERNAL);
                    }else if(StorageUtil.KEY_DEFAULT_INTERNAL.equals(storagePath)){
                        Settings.Global.putString(getContentResolver(),
                                "camera_quick_capture_storage_path",
                                MultiStorage.KEY_DEFAULT_INTERNAL);
                    }
                    // SPRD: add for bug 592614 updata filmScript item
                    storageUpdateThread();
                    break;
            }
        }
    }
    @Override
    public boolean requestScopedDirectoryAccess(String storagePath) {
        StorageUtil storageUtil = StorageUtil.getInstance();
        if(storageUtil.getCurrentAccessUri(storagePath) != null){
            return true;
        }

        if(isSecureCamera()) {
            CameraUtil.toastHint(this,R.string.security_camera_does_not_support_aquire_storage_permission);
            return false;
        }
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> volumes = storageManager.getStorageVolumes();
        if (storagePath.equals(StorageUtil.KEY_DEFAULT_EXTERNAL)) {
            storagePath = StorageUtilProxy.getExternalStoragePath().toString();
            mRequestCode = SCOPE_REQUEST_CODE_SD;
        } else {
            mUsbKey = storagePath;
            storagePath = MultiStorage.getUsbStoragePath(storagePath);
            mRequestCode = SCOPE_REQUEST_CODE_OTG;
        }
        for (StorageVolume volume : volumes) {
            File volumePath = volume.getPathFile();
            Log.i(TAG,"requestScopedDirectoryAccess volumePath: " + volumePath);
            if (!volume.isPrimary() && volumePath != null &&
                    Environment.getExternalStorageState(volumePath).equals(Environment.MEDIA_MOUNTED)
                    && storagePath != null && volumePath.toString().contains(storagePath)) {
                Log.i(TAG,"really createAccessIntent for : " + volumePath);
                final Intent intent = volume.createAccessIntent(null);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    //mCameraAppUI.hideSettingLayout();
                    showPermissionGuideDialog(CameraActivity.this,volume);
                } else {
                    if (intent != null) {
                        startActivityForResult(intent, mRequestCode);
                    }
                }
            }
        }
        return false;
    }

    private AlertDialog mPermissionDialog;
    public void showPermissionGuideDialog(Context context,StorageVolume volume) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater factory = LayoutInflater.from(context);
        View view = factory.inflate(R.layout.permission_dialog_layout, null);
        builder.setView(view);
        builder.setPositiveButton(R.string.permission_continue,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //allow to access permission
                final Intent intent = volume.createOpenDocumentTreeIntent();
                if (intent != null) {
                    startActivityForResult(intent, mRequestCode);
                }
            }
        });
        builder.setNegativeButton(R.string.permission_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //deny to access permisson
            }
        });
        if (mPermissionDialog == null) {
            mPermissionDialog = builder.create();
        } else {
            String msg = mAppContext.getResources().getString(R.string.permission_continue);
            mPermissionDialog.setButton(DialogInterface.BUTTON_POSITIVE,msg,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //allow to access permission
                    final Intent intent = volume.createOpenDocumentTreeIntent();
                    if (intent != null) {
                        startActivityForResult(intent, mRequestCode);
                    }
                }
            });
        }
        if (mPermissionDialog.isShowing()) {
            return;
        } else {
            mPermissionDialog.show();
        }
    }

    /*
     * SPRD: Fix 474843 Add for Filter Feature @{
     */
    public int getCameraId() {
        mCameraId = DataModuleManager.getInstance(this).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID);
        return mCameraId;
    }

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public void onStopRecordVoiceClicked(View v) {
        onStopRecordVoiceClicked();
    }

    public void onStopRecordVoiceClicked() {
        if(mCurrentModule != null){
            if (mCurrentModule instanceof AudioPictureModule) {
                AudioPictureModule AudioPictureModule = (AudioPictureModule) mCurrentModule;
                AudioPictureModule.onStopRecordVoiceClicked();
            }
        }
    }

    //SPRD : Fix bug 666446 this imageView is not used anymore
    /*@Override
    public void onVoicePlayClicked() {
        if (mFilmstripController.inFullScreen()) {
            mPhotoVoicePlayer.playPhotoVoice();
        }
    }*/
    /* @} */
    //Bug#533869 add the feature of volume
    public int getVolumeControlStatus(){
        String index = mDataModule.getString(Keys.KEY_CAMERA_VOLUME_FUNCTION);
        /* SPRD:fix bug670989 add for bluetooth capture @{ */
        if (mHasKeyEventEnter) {
            return Keys.shutter;
        }
        /* @} */
        return Integer.parseInt(index);
    }

    //SPRD: bug fix 603935 ,Turn off the phone automatically rotate function,rotate the phone, the picture is also rotating
    public boolean isFilmStripShown() {
        return mFilmstripVisible;
    }

    private HandlerThread dreamHandlerThread;
    private Handler dreamHandler;

    private void initHandlerThread() {
        dreamHandlerThread = new HandlerThread("dreamcamera");
        dreamHandlerThread.start();

        dreamHandler = new Handler(dreamHandlerThread.getLooper());
    }

    private Runnable thumbnailRunnable = new Runnable() {
        @Override
        public void run() {
            syncThumbnail();
        }
    };

    public void runSyncThumbnail() {
        if(dreamHandler == null){
            initHandlerThread();
        }

        if (dreamHandler != null) {
            clearDreamHandler();
            dreamHandler.post(thumbnailRunnable);
        }
    }

    private Runnable asyncRunnable;

    public void runSyncThumbnail(Runnable runnable) {
        if(dreamHandler == null){
            initHandlerThread();
        }

        if (dreamHandler != null) {
            clearDreamHandler();
            asyncRunnable = runnable;
            dreamHandler.post(asyncRunnable);
        }
    }

    private void clearDreamHandler(){
        if (dreamHandler != null) {
            if(asyncRunnable != null){
                dreamHandler.removeCallbacks(asyncRunnable);
            }
            dreamHandler.removeCallbacks(thumbnailRunnable);
        }
    }

    private Runnable mainUiThumbnailRunnable;

    public Handler getDreamHandler(){
        if(dreamHandler == null){
            initHandlerThread();
        }

        return dreamHandler;
    }

    boolean mShutterSoundEnable = false;

    @Override
    public boolean isPlaySoundEnable(){
        return !CameraUtil.isInSilentMode(this) && mShutterSoundEnable;
    }

    @Override
    public boolean isAutoHdrSupported(){
        return mCurrentModule instanceof PhotoModule && ((PhotoModule)mCurrentModule).isAutoHdrSupported();
    }

    public PhotoItemFactory photoItemFactory(){
        return mPhotoItemFactory;
    }

    public void SyncThumbnailForIntervalPhoto(Bitmap bm,Uri uri){
        if (mCurrentModeIndex == SettingsScopeNamespaces.INTERVAL){
            mRedirectionUri = uri;
            startPeekAnimation(bm);
        }
    }
    private Uri mRedirectionUri = null;
    private void launchGalleryPhoto() {
        if (null == mRedirectionUri) {
            mCameraAppUI.setBottomPanelLeftRightClickable(true);
            return;
        }
        Intent startGallery = new Intent(CameraUtil.REVIEW_ACTION);
        startGallery.setData(mRedirectionUri);
        startGallery.putExtra("camera_album", true);
        startGallery.putExtra("secure_camera", mSecureCamera);
        /**
         * google photos requirement:intent should include the package para
         */
        if (mIsUsedGooglePhotosGo) {
            startGallery.setPackage("com.google.android.apps.photosgo");
        }
        if (mIsUsedGooglePhotos) {
            startGallery.setPackage("com.google.android.apps.photos");
        }
        if (mSecureCamera) {
              long[] securePhotoIds = new long[securePhotoList.size()];
              for (int i = 0; i < securePhotoList.size(); i++ ){
                  securePhotoIds[i] = securePhotoList.get(i);
              }
              if (mIsUsedGooglePhotos || mIsUsedGooglePhotosGo) {
                  startGallery.putExtra("com.google.android.apps.photos.api.secure_mode", true);
                  startGallery.putExtra("com.google.android.apps.photos.api.secure_mode_ids", securePhotoIds);
              }else{
                  startGallery.putExtra("secure_camera_Photo_ids", securePhotoIds);
          }
        }

        //Sprd:fix bug895543/948503
        try {
            startActivity(startGallery);
        } catch (ActivityNotFoundException e) {
            Log.i(TAG, "Failed to start intent=" + startGallery + ": " + e);
            CameraUtil.toastHint(this, R.string.goto_gallery_failed);
            mCameraAppUI.setBottomPanelLeftRightClickable(true);//SPRD:fix bug 1133118
        }
    }

    public void doSometingWhenFilmStripShow(){
        launchGalleryPhoto();
        if (mCurrentModule != null) {
            mCurrentModule.doSometingWhenFilmStripShow();
        }
    }

    private boolean waitToChangeMode = false;

    public boolean isWaitToChangeMode() {
        return waitToChangeMode;
    }

    public void setWaitToChangeMode(boolean wait) {
        waitToChangeMode = wait;
    }

    private void updateUI() {
        /*
         * SPRD Bug:519334, 527627 Refactor Rotation UI of Camera. @{
         */
        if (mOrientationManager == null) {
            Log.e(TAG, "onFilmstripHidden returned,"
                    + "because cameraActivity is destroyed.");
            return;
        }

        mMainHandler.postDelayed(new Runnable() {

            int tryTime = 0;

            @Override
            public void run() {
                if (CameraActivity.this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        && CameraActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (CameraActivity.this.isDestroyed()) return; // SPRD: fix bug 531072 JavaCrash:java.lang.NullPointerException
                  //SPRD:fix bug 613799 save the panorama exception
                    if(!mCurrentModule.isSavingPanorama()){
                        mCameraAppUI.showPanels();
                    }
                    if (dataAdapterLengthBeforeShow != mDataAdapter.getCount()) {
                        updateStorageSpaceAndHint(null);
                    } else {
                        /*
                         * fix bug 601158 thumbnail does not generate and display
                         */
                        if (mCurrentModule.getModuleTpye() == DreamModule.CONTINUE_MODULE && mCurrentModule.isBurstThumbnailNotInvalid()) {
                            Log.i(TAG,"burst capture invalid thumbnail");
                            fillTemporarySessions();
                            mCurrentModule.restoreCancelBurstTag();
                        }
                        /* SPRD:fix bug 619231 thumbnail not sync @{ */
                        if (mLastPhotoId != mDataAdapter.getLastPhotoId()) {
                            fillTemporarySessions();
                        }
                        /* @} */
                    }
                    if (secureCameraSyncThubnail()) {
                            runSyncThumbnail();
                    }
                } else {
                    if (++tryTime <= 50) {
                        mMainHandler.postDelayed(this, 20);
                    } else {
                        Log.e(TAG, "resetOrientaion timeout.");
                    }
                }
            }
        }, 20);
    }

    private boolean secureCameraSyncThubnail() {
        if (mSecureCamera) {
            if (mDataAdapter != null // Bug 1159111 (REVERSE_INULL)
                    && null != mDataAdapter.getItemAt(0)
                    && dataAdapterLengthBeforeShow != mDataAdapter.getCount()
                    && mCurrentModule != null
                    && !mCurrentModule.isFreezeFrameDisplayShow()) {
                return true;
            }
        }
        return false;
    }
    /* SPRD: Fix bug 613015 add SurfaceView support @{ */
    public void setSurfaceHolderListener(SurfaceHolder.Callback surfaceHolderListener) {
        mCameraAppUI.setSurfaceHolderListener(surfaceHolderListener);
    }
    /* @} */

    public void setSurfaceVisibility(boolean visible) {
        mCameraAppUI.setSurfaceVisibility(visible);
    }

    public void rollTask(Runnable runnable) {
        getDreamHandler().post(new Runnable() {
            @Override
            public void run() {
                mMainHandler.post(runnable);
            }
        });
    }

    private void inflateFilmstrip() {
        mCameraAppUI.inflateFilmstrip();
    }

    private void initFilmstrip() {
        mCameraAppUI.initFilmstrip();
        mFilmstripController = ((FilmstripView) findViewById(R.id.filmstrip_view)).getController();
        mFilmstripController.setDataAdapter(mDataAdapter);
        mAboveFilmstripControlLayout =
                (FrameLayout) findViewById(R.id.camera_filmstrip_content_layout);
    }

    private boolean checkPermissionOnCreate() {
        // SPRD:fix Bug 616294 add warn location express when enter into Camera
        Intent intent = getIntent();
        Bundle bundle = (intent != null) ? intent.getExtras() : null;
        resultString = (bundle != null) ? bundle.getString("result") : null;

        mSecureCheck = SystemProperties.get("persist.support.securetest",
                DAFAULT_SYSTEM);
        switch (mSecureCheck) {
        case DAFAULT_SYSTEM_OK:
            if (resultString == null) {
                checkSystemPermission();
                return false;
            } else if (resultString.equals(DAFAULT_LOCATION_OK)
                    || resultString.equals(DAFAULT_PERMISSION_OK)) {
                checkCameraPermission();
            }
            break;
        default:
            checkCameraPermission();
            break;
        }

        if (!isCaptureIntent() && !mHasCriticalPermissions) {
            Log.i(TAG, "onCreate: Missing critical permissions.");
            finish();
            return false;
        }
        return true;
    }

    private Drawable captureShutterButtonIcon;

    public Drawable getCaptureShutterButtonIcon() {
        return captureShutterButtonIcon;
    }

    public boolean checkAllCameraAvailable(){
        if (mCameraAvailableMap == null)
            return false;
        Iterator iter = mCameraAvailableMap.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            String mCameraId = (String)entry.getKey();
            boolean mCameraAvailable = (boolean)entry.getValue();
            Log.i(TAG, "checkAllCameraAvailable mCameraId =" + mCameraId + " mCameraAvailable="
                    + mCameraAvailable);
            if (!mCameraAvailable)
                return false;
        }
        return true;
    }

    private LinkedHashMap<String, Uri> mUriMap;
    public void putThumbUri(String title, Uri uri) {
        if (mUriMap != null) {
            mUriMap.put(title, uri);
        }
    }

    public Uri getThumbUri(String title) {
        Uri uri = null;
        if (mUriMap != null) {
            uri = mUriMap.get(title);
            mUriMap.remove(title);
        }
        return uri;
    }

    public Handler getCreatAsyncHandler() {
        return mOnCreateAsyncHandler;
    }

    private LinkedHashMap<String, UpdateRunnbale> mRunnbaleMap;
    public void putUpdateRun(String title, UpdateRunnbale run) {
        Log.i(TAG, "putUpdateRun title = " + title);
        if (mRunnbaleMap != null) {
            mRunnbaleMap.put(title, run);
            getServices().getMediaSaver().addMemoryUse(run.dataLength());//SPRD:fix bug779875
        }
    }

    public UpdateRunnbale getUpdateRun(String title) {
        UpdateRunnbale run = null;
        Log.i(TAG, "getUpdateRun title = " + title);
        if (mRunnbaleMap != null) {
            run = mRunnbaleMap.get(title);
            if (run != null) {
                getServices().getMediaSaver().reduceMemoryUse(run.dataLength());//SPRD:fix bug779875
                mRunnbaleMap.remove(title);
            }
        }
        return run;
    }

    public void switchBlurModuel() {
        mCameraRequestHandler.post(mPreSwitchRunnable);//SPRD:fix bug968595
        freezeScreenCommon();
        mNextModuleIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        if (mDataModuleManager.getCurrentDataModule().getInt(Keys.KEY_REFOCUS) == 1) {
            if (DreamUtil.BACK_CAMERA == mDataModule.getInt(Keys.KEY_CAMERA_ID)) {
                mNextModuleIndex = SettingsScopeNamespaces.REFOCUS;
                Log.i(TAG, "module will switch to back refocus");
            } else {
                mNextModuleIndex = SettingsScopeNamespaces.FRONT_BLUR;
                Log.i(TAG, "module will switch to front refocus");
            }
        }
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    onModeSelected(mNextModuleIndex);
                }else{
                    ButtonManagerDream buttonManagerDream= (ButtonManagerDream)getButtonManager();
                    buttonManagerDream.correctRefocusButtonState();
                }
            }
        });
    }

    public void switchFilterModuel() {
        mCameraRequestHandler.post(mPreSwitchRunnable);
        freezeScreenCommon();
        mNextModuleIndex = SettingsScopeNamespaces.AUTO_PHOTO;
        if (mDataModuleManager.getCurrentDataModule().getInt(Keys.KEY_FILTER_PHOTO) == 1) {
            mNextModuleIndex = SettingsScopeNamespaces.FILTER;
        }
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    onModeSelected(mNextModuleIndex);
                } else {
                    ButtonManagerDream buttonManagerDream= (ButtonManagerDream)getButtonManager();
                    buttonManagerDream.correctFilterButtonState();
                }
            }
        });
    }

    // Bug 1024253 - NEW FEATURE: Ultra Wide Angle
    public void switchUltraWideAngleModule(boolean leave) {
        mCameraRequestHandler.post(mPreSwitchRunnable);
        freezeScreenCommon();
        mNextModuleIndex = (leave ? SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE : SettingsScopeNamespaces.AUTO_PHOTO);

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    onModeSelected(mNextModuleIndex);
                }
            }
        });
    }

    public void switchTo3DNROrPortrait(int moduleIndex) {
        mCameraRequestHandler.post(mPreSwitchRunnable);
        freezeScreenCommon();

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!mPaused) {
                    onModeSelected(moduleIndex);
                }
            }
        });
    }

    private void waitToModuleResume() {
        if (mIsCameraRequestedOnCreate || checkAllCameraAvailable()) {
            mIsCameraRequestedOnCreate = false;
            mCounterOncreateOpenCamera.waitCount();
            mCurrentModule.resume();
            mMainHandler.removeCallbacks(mResumeRunnable);
        } else {
            mCameraRequestHandler.sendEmptyMessageDelayed(
                    MSG_CAMERA_REQUEST_PENDING, CAMERA_REQUEST_PENDING_TIME);
            Log.i(TAG, "camera is not available, send message");
        }
    }

    private void skipFirstChangeonResume() {
        if (!mSkipFirstChangeOnResume) {
            int cameraId = DataModuleManager.getInstance(this).getDataModuleCamera()
                    .getInt(Keys.KEY_CAMERA_ID);

            if (CameraUtil.isTcamAbility(mAppContext, cameraId) && !isCaptureIntent()) {
                cameraId = CameraUtil.BACK_TRI_CAMERA_ID;
            }
            DataStructSetting dataSetting = new DataStructSetting(
                    DreamUtil.intToString(mCurrentModule.getMode()), DreamUtil.isFrontCamera(this,
                    cameraId),
                    mCurrentModeIndex, cameraId);

            // change the data storage module
            DataModuleManager.getInstance(this).changeModuleStatus(dataSetting);
        } else {
            mSkipFirstChangeOnResume = false;
        }
    }

    private void releaseHalAndListener() {
        if (mMainHandler != null) {
            mMainHandler.removeMessages(MSG_DESTORY_CAMER);
        }

        /* SPRD: Fix bug 572631, optimize camera launch time @{ */
        if (mOnCreateAsyncHandler != null) {
            mOnCreateAsyncHandler.getLooper().quitSafely();
        }
        /* @} */
        /* SPRD:fix bug681336 modify for oom @{ */
        if (mCameraRequestHandler != null) {
            mCameraRequestHandler.getLooper().quitSafely();
        }
        /* @} */

        if (mSecureCamera) {
            unregisterReceiver(mShutdownReceiver);
        }

        // Ensure anything that checks for "isPaused" returns true.
        mPaused = true;

        /* SPRD: Add for bug 612207 to prevent NullPointerException. @{ */
        if (mCameraManager != null) {
            mCameraManager.unregisterAvailabilityCallback(mAvailabilityCallback);
        }

        if (mDataModuleManager != null) {
            // SPRD: add for crash when click reset Settings
            mDataModuleManager.destory(CameraActivity.this);
        }
        /* @} */
        if (mCameraController != null) {
            mCameraController.removeCallbackReceiver();
            mCameraController.setCameraExceptionHandler(null);
        }
        if (mLocalImagesObserver != null) {
            if (mSecureCamera) {
                mLocalImagesObserver.setForegroundChangeListener(null);
            }
            getContentResolver().unregisterContentObserver(mLocalImagesObserver);
        }
        if (mLocalVideosObserver != null) {
            getContentResolver().unregisterContentObserver(mLocalVideosObserver);
        }
    }

    private void releaseRes() {
        /* SPRD: Fix bug 612207 cameraActivity instances leak leads to OOM. @{ */
        if (mCameraAppUI != null) {
            mCameraAppUI.onDestroy();
        }
        /* @} */
        /* nj dream camera test 24 */
        if (mCurrentModule != null) {
            mCurrentModule.destroy();
        }
        /* @} */

        if (mCameraAvailableMap != null) {
            mCameraAvailableMap.clear();
            mCameraAvailableMap = null;
        }

        if (mUriMap != null) {
            mUriMap.clear();
            mUriMap = null;
        }
        if (mRunnbaleMap != null) {
            mRunnbaleMap.clear();
            mRunnbaleMap = null;
        }
        if (mModeListView != null) {
            mModeListView.setVisibilityChangedListener(null);
        }
        if (mReceiver != null) {
            this.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (mReceiverShutDown != null) {
            this.unregisterReceiver(mReceiverShutDown);
            mReceiverShutDown = null;
        }
        mCameraController = null;
        mOrientationManager = null;
        mButtonManager = null;
        /**
         * SPRD: Fix bug 572631, optimize camera launch time
         *
         if (mSoundPlayer != null) {
         mSoundPlayer.release();
         }
         */
        if (mDataAdapter != null && mDataAdapter instanceof CameraFilmstripDataAdapter) {
            ((CameraFilmstripDataAdapter) mDataAdapter).onDestroy();
        }
    }

    private boolean consumeKeyEvent(int keyCode) {
        return (keyCode != KeyEvent.KEYCODE_BACK && mCurrentModule != null && !mCurrentModule.isCameraAvailable());
    }

    private boolean inPreview() {
        return (!mFilmstripVisible && !mCameraAppUI.isModeListOpen() && !mCameraAppUI.isSettingLayoutOpen());
    }

    public int getCurrentBattery() {
        BatteryManager batteryManager = (BatteryManager)(getSystemService(Context.BATTERY_SERVICE));
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    //SPRD: Fix bug 944410
    public boolean isUpdateSmileItemByAeLock(){
        return ((!(getApplicationContext().getResources().getString(R.string.pref_ai_detect_entry_value_off).equals(mDataModule.getString(Keys.KEY_CAMERA_FACE_DATECT)))
                && (mCurrentModule instanceof PhotoModule && ((PhotoModule)mCurrentModule).isAELock()))); //SPRD: Fix bug 1000249
    }

    //Sprd：fix bug1002522
    public boolean isPhoneRing() {
        boolean isInRing = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            try{
                if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(i)[0]) == TelephonyManager.CALL_STATE_RINGING) {
                    isInRing = true;
                    break;
                }
            } catch (NullPointerException e){
                Log.i(TAG,"phone is unable");
                return false;
            }
        }
        Log.i(TAG,"Is phone ringing:"+isInRing);
        return isInRing;
    }

    public void updateLocationRecordingEnabled(){
        final boolean locationRecordingEnabled =
                DataModuleManager.getInstance(this)
                        .getDataModuleCamera().getBoolean(Keys.KEY_RECORD_LOCATION);
        Log.i(TAG, "locationRecordingEnabled = " + locationRecordingEnabled);
        mLocationManager.recordLocation(locationRecordingEnabled);
    }

    @Override
    public void onRestartTasks() {

    }

    public void setOpenCameraOnly(boolean only) {
        mOpenCameraOnly = only;
    }

    public void setTimeDurationS(int duration) {
        mTimeDurationS = duration;
    }

    public void setVoiceIntentCamera(boolean voiceIntent) {
        mVoiceIntent = voiceIntent;
    }

    public boolean getOpenCameraOnly() {
        return mOpenCameraOnly;
    }

    public int getTimeDurationS() {
        return mTimeDurationS;
    }

    public boolean getVoiceIntentCamera() {
        return mVoiceIntent;
    }
}
