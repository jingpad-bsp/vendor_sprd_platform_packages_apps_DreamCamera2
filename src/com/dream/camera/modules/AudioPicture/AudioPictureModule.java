
package com.dream.camera.modules.AudioPicture;

import com.android.camera.app.AppController;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera2.R;

import android.os.Handler;
import android.os.Looper;

import com.android.ex.camera2.portability.CameraAgent;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamModule;
import com.android.camera.PhotoModule;

import android.hardware.Camera.Parameters;

import com.android.camera.CameraActivity;

import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Canvas;

import com.dream.camera.util.DreamUtil;
import com.dream.camera.settings.DataModuleManager;

import android.location.Location;

import com.sprd.camera.voice.PhotoVoiceMessage;
import com.sprd.camera.voice.PhotoVoiceRecorder;
import com.android.camera.util.CameraUtil;

public class AudioPictureModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("AudioPictureModule");
    private final Handler mHandler;
    private boolean isAudioRecording = false;
    private PhotoVoiceRecorder mPhotoVoiceRecorder;
    protected AppController mAPMAppController;
    private ImageView mVoicePreview;
    //private byte[] mImageData; //for oom
    private Bitmap mFreezeScreen;
    private boolean mShutterClicked = false;
    BitmapFactory.Options opts = new BitmapFactory.Options();

    public AudioPictureModule(AppController app) {
        super(app);
        mHandler = getHandler();
        mAPMAppController = app;
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        ViewStub viewStubRecordProgess = (ViewStub) activity.findViewById(R.id.layout_photo_voice_record_progess_id);
        if (viewStubRecordProgess != null) {
            viewStubRecordProgess.inflate();
        }
        ViewStub viewStubVoicePreview = (ViewStub) activity.findViewById(R.id.layout_voice_preview_id);
        if (viewStubVoicePreview != null) {
            viewStubVoicePreview.inflate();
        }
        mVoicePreview = (ImageView) mActivity.findViewById(R.id.voice_preview);
        // SPRD: ui check 208
        //showRecordVoiceHind();
        return new AudioPictureUI(activity, this, activity.getModuleLayoutRoot());
    }

    public void showRecordVoiceHind() {
        boolean shouldShowRecordVoiceHind = mDataModule.getBoolean(Keys.KEY_CAMERA_RECORD_VOICE_HINT);
        if (shouldShowRecordVoiceHind == true) {
            CameraUtil.toastHint(mActivity,R.string.camera_record_voice_tip);
            mDataModule.set(Keys.KEY_CAMERA_RECORD_VOICE_HINT, false);
        }
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    /* nj dream camera test 24 */
    @Override
    public void destroy() {
        super.destroy();
    }
    /* @} */

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public void onStopRecordVoiceClicked() {
        Log.e(TAG, "onStopRecordVoiceClicked isAudioRecording = " + isAudioRecording);
        mShutterClicked = false;
        mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_AUDIO);
        if (isAudioRecording) {
            mPhotoVoiceRecorder.stopAudioRecord();
        }
    }

    @Override
    protected void startAudioRecord() {
        Log.i(TAG,"startAudioRecord");
        if(isHdr() || isHdrPicture()) {
            mUI.showHdrTips(false);
        }
        boolean result = mPhotoVoiceRecorder.startAudioRecord();
        mShutterClicked = result;//SRPD:fix bug957941
        if (result) {
            isAudioRecording = true;
            ((AudioPictureUI)mUI).showAudioNoteProgress();
            mActivity.getCameraAppUI().updateRecordVoiceUI(View.INVISIBLE);
            freezePreview();
        } else {
            mPhotoVoiceRecorder.savePhoto(null);
            mAPMAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
        }
        if(mDataModuleCurrent.getBoolean(Keys.KEY_ADD_LEVEL)){
            mUI.setLevelVisiable(false);
        }
    }
    @Override
    protected void updateFilterTopButton() {
        mUI.setButtonVisibility(ButtonManagerDream.BUTTON_FILTER_DREAM,View.GONE);
    }
    @Override
    protected void onAudioRecordStopped() {
        Log.i(TAG,"onAudioRecordStopped");
        if(isHdr() || isHdrPicture()) {
            mUI.showHdrTips(true);
        }
        mShutterClicked = false;
        setAELockPanel(true);
        if (mUI != null) {
            ((AudioPictureUI)mUI).hideAudioNoteProgress();
            mActivity.getCameraAppUI().updateRecordVoiceUI(View.VISIBLE);
            mUI.enablePreviewOverlayHint(true);
            mAPMAppController.getCameraAppUI().setBottomPanelLeftRightClickable(true);
            if (mVoicePreview != null) {
                mVoicePreview.setVisibility(View.GONE);
            }
            if (mFreezeScreen != null && !mFreezeScreen.isRecycled()) {
                mFreezeScreen.recycle();
                mFreezeScreen = null;
            }

            startFaceDetection(); // Bug 1131066
        }
        isAudioRecording = false;
        if(mDataModuleCurrent.getBoolean(Keys.KEY_ADD_LEVEL)){
            mUI.setLevelVisiable(true);
        }
        mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_STOPPED);
    }

    @Override
    public void startFaceDetection() { // Bug 1131066
        if (mCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS) {
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
    public boolean  isAudioRecording() {
        return isAudioRecording;
    }

    @Override
    protected void initData(byte[] jpegData, String title, long date,
            int width, int height, int orientation, ExifInterface exif,
            Location location,
            MediaSaver.OnMediaSavedListener onMediaSavedListener) {
        //mImageData = jpegData;//for oom
        mPhotoVoiceRecorder.initData(jpegData, title, date, width, height,
                orientation, exif, location, onMediaSavedListener,
                getServices().getMediaSaver());
        mAPMAppController.getCameraAppUI().setBottomPanelLeftRightClickable(false);
        ((AudioPictureUI)mUI).setTopPanelVisible(View.INVISIBLE);
        /* SPRD: Fix bug615557 that photo voice recorded the shutter sound @{ */
        if (mDataModule.getBoolean(
                Keys.KEY_CAMERA_SHUTTER_SOUND)) {
            mHandler.sendEmptyMessageDelayed(PhotoVoiceMessage.MSG_RECORD_AUDIO, 680);
        } else {
            mHandler.sendEmptyMessageDelayed(PhotoVoiceMessage.MSG_RECORD_AUDIO, 300);
        }
        /* @} */
        if (isUseSurfaceView()) {
            Log.i(TAG, "decode freezescreen begin");
            opts.inSampleSize = width / CameraUtil.getDefaultDisplayRealSize().getWidth();
            mFreezeScreen = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length , opts);
            Log.i(TAG, "decode freezescreen end");
        }
        mPhotoVoiceRecorder.requestAudioFocus();
    }
    @Override
    public void resume() {
        super.resume();
        mPhotoVoiceRecorder = new PhotoVoiceRecorder(mAPMAppController);
        mPhotoVoiceRecorder.setHandler(mHandler);
    }

    @Override
    public void pause() {
        super.pause();
        mShutterClicked = false;
        mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_AUDIO);
        if (mPhotoVoiceRecorder != null && mPhotoVoiceRecorder.isAudioFocusGain && !isAudioRecording) {
            /*SPRD Fix bug941007 save as normal photo for user cancel audio*/
            mPhotoVoiceRecorder.savePhoto(null);
            mPhotoVoiceRecorder.abandonAudioFocus();
        } else if (isAudioRecording) {
            if(mPhotoVoiceRecorder != null){
                mPhotoVoiceRecorder.stopAudioRecord();
            }
            onAudioRecordStopped();
        }
    }

    @Override
    public boolean isShutterClicked(){
        return mShutterClicked;
    }

    @Override
    public void resetShutterButton() {
        mShutterClicked = false;
    }

    @Override
    public void onShutterButtonClick() {
        if (isPhoneCalling(mActivity)) {
            Log.i(TAG, "Audio picture won't start due to telephone is running");
            CameraUtil.toastHint(mActivity, R.string.phone_does_not_support_audio_picture);
            return;
        }
        /*SPRD Fix Bug #637017 the icon may be mess when audiopicturemodule@{*/
        if (!mAPMAppController.getCameraAppUI().isShutterButtonClickable()) {
            return;
        }
        mShutterClicked = true;
        /*@}*/
        super.onShutterButtonClick();
    }

    @Override
    public boolean onBackPressed() {
        mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_AUDIO);
        mShutterClicked = false;
        if (isAudioRecording) {
            mPhotoVoiceRecorder.stopAudioRecord();
            return true;
        } else if (mPhotoVoiceRecorder != null && mPhotoVoiceRecorder.isAudioFocusGain && !isAudioRecording) {
            mPhotoVoiceRecorder.abandonAudioFocus();
            onAudioRecordStopped();//SPRD:fix bug943140
        } else {
            mHandler.removeMessages(PhotoVoiceMessage.MSG_RECORD_STOPPED);
        }
        return super.onBackPressed();
    }

    private void freezePreview() {
        setAELockPanel(false);
        setVoiceReviewParams();
        if (!isUseSurfaceView()) {
            mFreezeScreen = mActivity.getCameraAppUI().getPreviewShotWithoutTransform();
        }
        if(mFreezeScreen != null){// Bug 1159164 (NULL_RETURNS)
            Bitmap bitmap = makeBitmap(mFreezeScreen);
            if (bitmap != null && !bitmap.isRecycled()){
                mVoicePreview.setImageBitmap(bitmap);
            }
        }
        mVoicePreview.setVisibility(View.VISIBLE);
    }

    private void setVoiceReviewParams() {
        Size size = new Size(mCameraSettings.getCurrentPreviewSize());
        float aspectRatio= (float)size.getWidth() / size.getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mVoicePreview.getLayoutParams();
        RectF rect = mActivity.getCameraAppUI().getPreviewArea();
        if (rect == null) {
            return;
        }
        WindowManager wm = mActivity.getWindowManager();
        Display display = wm.getDefaultDisplay();
        Point realSize = new Point();
        display.getRealSize(realSize);
        int width = realSize.x;
        int height = realSize.y;
        if (width > height) {
            height = width;
        }
        if(CameraUtil.hasCutout()){
            params.setMargins(0, (int)rect.top, 0, (int)(height - rect.right * aspectRatio - rect.top - CameraUtil.getCutoutHeight()));
        }else {
            params.setMargins(0, (int) rect.top, 0,0);
        }
        mVoicePreview.setLayoutParams(params);
    }

    public Bitmap makeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int previewWidth = (int)mActivity.getCameraAppUI().getPreviewArea().width();
        int previewHeight = (int)mActivity.getCameraAppUI().getPreviewArea().height();
        int scalevalue = previewWidth > previewHeight ? previewWidth : previewHeight;
        float scaleHeight = ((float) scalevalue) / height ;
        Matrix matrix = new Matrix();
        int mDeviceOrientation = mAPMAppController.getOrientationManager().getDeviceOrientation().getDegrees();
        if (isUseSurfaceView()) {
            if (mJpegRotation % 180 == 0) {//SPRD:fix bug903613
                matrix.preRotate((mJpegRotation + 90) % 360);
            } else {
                if (mDeviceOrientation == 0 || mDeviceOrientation == 180)
                matrix.preRotate(mDeviceOrientation);
            }
        }
        matrix.postScale(scaleHeight, scaleHeight); // SPRD: Modify for bug670303, need scale x also here
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        if (isAudioRecording || !mAPMAppController.getCameraAppUI().isShutterButtonClickable()) {
            return;
        }
        super.onSingleTapUp(view, x, y);
        if (mDataModuleCurrent != null
                && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)
                && mUI != null
                && !mUI.isEnableDetector()) {
            mAPMAppController.getCameraAppUI().setBottomPanelLeftRightClickable(false);
        }
    }
    /* @} */
    @Override
    public int getModuleTpye() {
        return DreamModule.AUDIOPICTURE_MODULE;
    }

    @Override
    public boolean canShutter() {
        return mAPMAppController.getCameraAppUI().isShutterButtonClickable();
    }

    @Override
    protected void updateParametersGridLine() {
        if (!mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_GRID_LINES) || isAudioRecording) {
            return;
        }
        mAPMAppController.getCameraAppUI().initGridlineView();
        String grid = mDataModuleCurrent.getString(Keys.KEY_CAMERA_GRID_LINES);
        mAPMAppController.getCameraAppUI().updateScreenGridLines(grid);
        Log.d(TAG, "updateParametersGridLine = " + grid);
    }

    @Override
    protected boolean needInitData() {
        return true;
    }

    @Override
    protected boolean isAudioCapture() {
        Log.i(TAG , "isAudioCapture");
        return true;
    }
}
