package com.dream.camera.modules.continuephoto;

import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.util.AndroidServices;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.dream.camera.DreamModule;
import com.android.camera.PhotoModule;

public class ContinuePhotoModule extends PhotoModule implements View.OnTouchListener {
    private static final Log.Tag TAG = new Log.Tag("ContinuePhotoModule");
    // SPRD: Fix bug 672886, limit the maximum amount of continuous capture images in burst mode
    public static final int MAX_BURST_COUNT = 30;
    private byte[] mBurstFirstJpegData = null;

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity, isSecureCamera, isCaptureIntent);
        //showBurstHint();
    }

    //dream ui check 224
    public void showBurstHint() {
        boolean shouldBurstHint = mDataModule.getBoolean(Keys.KEY_CAMERA_BURST_HINT);
        if (shouldBurstHint == true) {
            CameraUtil.toastHint(mActivity,R.string.dream_continuephotomodule_module_warning);
            mDataModule.set(Keys.KEY_CAMERA_BURST_HINT, false);
        }
    }

    @Override
    public boolean inBurstMode() {
        return true;
    }

    public ContinuePhotoModule(AppController app) {
        super(app);
        mHasCaptureCount = 0;
    }

    @Override
    public void resume() {
        super.resume();
        mBurstNotShowShutterButton = false;
        mBurstCaptureCountOnCanceled = -1;
        mBurstCaptureType = -1;
        mAppController.getCameraAppUI().setShutterButtonEnabled(true);
        ((ContinuePhotoUI) mUI).changeOtherUIVisible(false, View.VISIBLE);

        mActivity.getModuleLayoutRoot().findViewById(R.id.shutter_button)
                .setOnTouchListener(this);// SPRD: fix bug473462
        clearScreenHint();
    }

    private void clearScreenHint() {
        if(mHandler != null && showBurstScreenHintRunnable != null){
            mHandler.removeCallbacks(showBurstScreenHintRunnable);
        }
        mUI.dismissBurstScreenHit();
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        return new ContinuePhotoUI(activity, this,
                activity.getModuleLayoutRoot());
    }

    @Override
    public void pause() {
        ((ContinuePhotoUI) mUI).changeExtendPanelUI(View.GONE);
        mAppController.getCameraAppUI().setBursting(false);

        /* SPRD: fix bug 473462 add burst capture @{ */
        if (mBurstWorking) {
            if (mAppController.isPlaySoundEnable()) {
                AndroidServices.instance().provideAudioManager().abandonAudioFocus(null);
            }
            cancelBurstCapture();
            mBurstWorking = false;
        }
        mCaptureCount = 0;
        mUI.dismissBurstScreenHit();
        mUI.enablePreviewOverlayHint(true);// SPRD:Fix bug 467044
        /* @} */

        mActivity.getModuleLayoutRoot().findViewById(R.id.shutter_button)
                .setOnTouchListener(null);// SPRD: fix bug 473462

        super.pause();
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    public boolean isSupportTouchEV() {
        return false;
    }

    Runnable showBurstScreenHintRunnable;

    @Override
    protected void doSomethingWhenonPictureTaken(byte[] jpeg) {
        Log.i(TAG, "mBurstWorking = " + mBurstWorking + " mHasCaptureCount = " + mHasCaptureCount + " mBurstCaptureCountOnCanceled = " + mBurstCaptureCountOnCanceled);
        if (mBurstWorking) {
            mHasCaptureCount++;
            if (mHasCaptureCount == 1) {
                mBurstFirstJpegData = jpeg;
            }
            ((ContinuePhotoUI) mUI).updateCaptureUI("" + mHasCaptureCount);
        } else {
            //SPRD:fix bug 704224
            if (mBurstCaptureCountOnCanceled != -1) {
                mHasCaptureCount++;
            } else {
                /*SPRD: Fix bug941722 abandon focus shutterbutton up before the pictureTaken.*/
                if (mAppController.isPlaySoundEnable()) {
                    AndroidServices.instance().provideAudioManager().abandonAudioFocus(null);
                }
                startPeekAnimation(jpeg);
            }
        }

        if (mHasCaptureCount == MAX_BURST_COUNT) {
            mBurstAllDone = true;
            handleActionUp();
        }

        if (mBurstCaptureCountOnCanceled <= mHasCaptureCount && mHasCaptureCount != 0 && mBurstCaptureCountOnCanceled != -1|| mBurstAllDone) {
            final int hasCaptureCount = mHasCaptureCount;
            showBurstScreenHintRunnable = new Runnable() {
                @Override
                public void run() {
                    mUI.showBurstScreenHint(hasCaptureCount);
                }
            };
            mHandler.post(showBurstScreenHintRunnable);

            //SPRD:Fix Bug665117
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((ContinuePhotoUI) mUI).changeExtendPanelUI(View.GONE);
                }
            },1000);

            if (mBurstFirstJpegData != null) {
                startPeekAnimation(mBurstFirstJpegData);
            }
            mBurstFirstJpegData = null;
            mBurstCaptureCountOnCanceled = -1;
            mBurstCaptureType = -1;
            mHasCaptureCount = 0;
            mBurstWorking = false;
            mContinuousCaptureCount = 0;
            mIsContinousCaptureFinish = false;

            if (mAppController.isPlaySoundEnable()) {
                AndroidServices.instance().provideAudioManager().abandonAudioFocus(null);
            }

            Log.i(TAG, "burst capture completed!");
        }
    }

    @Override
    protected void doSomethingWhenonShutterButtonClick() {
        // show extend panel xx/xx
    }

    @Override
    public void doCaptureSpecial() {
        Log.i(TAG, "doCaptureSpecial isBurstCapture = " + isBurstCapture());
        /*SPRD:fix bug 672841 enter wrong burst when not up event @{ */
        if (!isBurstCapture()) {
            return;
        }
        /* @} */
        mHasCaptureCount = 0;
        ((ContinuePhotoUI) mUI).updateCaptureUI("" + mHasCaptureCount);
        mActivity.getCameraAppUI().updateExtendPanelUI(View.VISIBLE);
        ((ContinuePhotoUI) mUI).changeExtendPanelUI(View.VISIBLE);
        ((ContinuePhotoUI) mUI).changeOtherUIVisible(true, View.INVISIBLE);
        setAELockPanel(false);

        if (mAppController.isPlaySoundEnable()) {
            AndroidServices.instance().provideAudioManager().requestAudioFocus(null,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    @Override
    protected void handleActionDown(int action) {
        if (mBurstCaptureType != -1) {
            Log.e(TAG, "handleActionDown is running while handleActionUp not");
            return;
        }
        //Sprd Fix bug:665197
        if (mUI.isZooming()){
            Log.i(TAG, "camera can not burst capture when zooming");
            return;
        }
        //SPRD:Fix Bug670446
        if (mAppController.getCameraAppUI().isThumbnailViewPressed()
                || !mAppController.getCameraAppUI().isShutterButtonClickable()) {
            Log.i(TAG, "isThumbnailViewPressed()="
                    + mAppController.getCameraAppUI().isThumbnailViewPressed() +
                    " isShutterButtonClickable()="
                    + mAppController.getCameraAppUI().isShutterButtonClickable());
            return;
        }
        mUI.enablePreviewOverlayHint(false);
        mAppController.getCameraAppUI().setBursting(true);//SPRD:fix bug672841
        super.handleActionDown(action);
    }

    @Override
    protected void handleActionUp() {
        /* SPRD:fix bug972828/980124 hide ae lock when burst capture @{ */
        setAELockPanel(true);
        /* @} */
        mUI.enablePreviewOverlayHint(true);
        ((ContinuePhotoUI) mUI).changeOtherUIVisible(false, View.VISIBLE);
        super.handleActionUp();
    }

    @Override
    public void touchCapture(){

    }

    @Override
    public void updateBatteryLevel(int level) {
        // if you don't want open the flah in this module do not call super method
        Log.d(TAG, "updateBatteryLevel: donothing");
    }

    @Override
    public void onHideBurstScreenHint() {
        if(mBurstNotShowShutterButton || mHasCaptureCount == MAX_BURST_COUNT) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onHideBurstScreenHint()");
                    mUI.dismissBurstScreenHit();
                    mBurstNotShowShutterButton = false;
                    mAppController.getCameraAppUI().setShutterButtonEnabled(true);
                    if(mHasCaptureCount == MAX_BURST_COUNT) {
                        ((ContinuePhotoUI) mUI).changeExtendPanelUI(View.GONE);
                        ((ContinuePhotoUI) mUI).changeOtherUIVisible(false, View.VISIBLE);
                    }
                }
            }, 1500);
        }

        if (mAppController.getCameraAppUI().isInFreezeReview()) {// SPRD :BUG398284
            mAppController.getCameraAppUI().setSwipeEnabled(false);
            mAppController.getCameraAppUI().onShutterButtonClick();
        } else {
            mAppController.getCameraAppUI().setSwipeEnabled(true);
        }
    }

    @Override
    public int getModuleTpye() {
        return DreamModule.CONTINUE_MODULE;
    }
    /*SPRD:Fix bug651120, burst camera and switch camera*/

    @Override
    public boolean isShutterClicked(){
        return !mAppController.isShutterEnabled() || mBurstWorking;
    }

    private int mBurstCaptureCountOnCanceled = -1;//the count of requests sent to hal

    @Override
    public boolean capture() {
        mBurstCaptureCountOnCanceled = -1;//SPRD:fix bug1085123
        if(!mBurstWorking && mIsContinousCaptureFinish){
            Log.i(TAG,"capture cancel");
            mAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
            mAppController.getCameraAppUI().setSwipeEnabled(true);
            mBurstNotShowShutterButton = false;
            return false;
        }
        return  super.capture();
    }

    @Override
    public void onCanceled(int count) {//SPRD:Fix Bug665117
        Log.i(TAG, "onCanceled " + count);
        mBurstCaptureCountOnCanceled = count;

        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mHasCaptureCount == 0 && mBurstCaptureCountOnCanceled == 0 && mHasRemove.get()) {
                    Log.i(TAG, " getHandler().post( onCanceled " + count + "mHasCaptureCount " + mHasCaptureCount);
                    mBurstFirstJpegData = null;
                    mBurstCaptureCountOnCanceled = -1;
                    mBurstCaptureType = -1;
                    mHasCaptureCount = 0;
                    mBurstWorking = false;
                    mContinuousCaptureCount = 0;
                    mIsContinousCaptureFinish = false;
                    onHideBurstScreenHint();
                    onMemoryStateChanged(com.android.camera.app.MemoryManager.STATE_OK);
                    ((ContinuePhotoUI) mUI).changeExtendPanelUI(View.GONE);
                    changeCameraState();
        /* @} */
                }
        }});



    }

    private int mPointId = 0;
    /*
     * SPRD: fix bug 473462 add for burst capture @{
     *
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        if (mUI != null && !mUI.isOnTouchInside(event, mPointId)) {
            Log.i(TAG, " onTouch out side action = " + action);
            if (action == MotionEvent.ACTION_MOVE) {
                if(mBurstCaptureType == 0)
                    handleActionUp();
            }
            if (!isActionUp(action)) {
                return false;
            }
        }
        switch (v.getId()) {
            case R.id.shutter_button:
                if (mUI == null) return true; // Bug 1159179 (FORWARD_NULL)
                Log.i(TAG, "shutter_button onTouch action = " + action);
                if (action == MotionEvent.ACTION_DOWN) {
                    if (inBurstMode()) {
                        mPointId = event.getPointerId(0);//SPRD:fix bug977378
                        handleActionDown(0);
                    }
                } else if (isActionUp(action) || isActionPointerUp(event)) {
                    if(mBurstCaptureType == 0) {
                        handleActionUp();
                    }
                }
                break;
        }
        return true;
    }

    /* SPRD:fix bug1094122 deal with action pointer up @{ */
    private boolean isActionPointerUp(MotionEvent event) {
        return (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) && (event.getPointerId(event.getActionIndex()) == mPointId);
    }
    /* @} */

    private boolean isActionUp(int action) {
        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
    }

    @Override
    protected void updateCameraParametersInitialize() {
        int[] fpsRange = CameraUtil
                .getMaxPreviewFpsRange(mCameraCapabilities.getSupportedPreviewFpsRange());
        if (fpsRange != null && fpsRange.length > 0) {
            mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
            Log.d(TAG, "preview fps: " + fpsRange[0] + ", " + fpsRange[1]);// Bug 1159178 (FORWARD_NULL)
        }
    }
}
