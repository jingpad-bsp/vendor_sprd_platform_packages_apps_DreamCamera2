package com.dream.camera.modules.rangefind;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.KeyEvent;

import com.android.camera.CameraActivity;
import com.android.camera.CameraModule;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.device.CameraId;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.module.ModuleController;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.ui.focus.CameraCoordinateTransformer;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.camera2.R;

import java.util.List;

public class TDRangeFindModule
        extends CameraModule
        implements TDRangeFindModuleController,
        ModuleController {

    private TDRangeFindModuleUI mUI;
    private CameraActivity mActivity;
    private AppController mAppController;
    private CameraProxy mCameraDevice;
    private boolean mPaused;
    private int mCameraId;
    private CameraCapabilities mCameraCapabilities;
    private CameraSettings mCameraSettings;
    private int mDisplayRotation = 0;
    private boolean mPreviewing = false;
    //private int mDisplayOrientation = 0;
    //private CameraCoordinateTransformer mCoordinateTransformer;
    private final Log.Tag TAG = new Log.Tag("TDRangFindModule");

    public TDRangeFindModule(AppController controller) {
        super(controller);
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        mActivity = activity;
        mAppController = activity;
        mUI = new TDRangeFindModuleUI(mActivity, this, mActivity.getModuleLayoutRoot());
        mActivity.setPreviewStatusListener(mUI);
    }

    @Override
    public void onPreviewUIReady() {
        Log.i(TAG, "onPreviewUIReady");
        startPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {
        Log.i(TAG, "onPreviewUIDestroyed");
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.setPreviewTexture(null);
        stopPreview();
    }

    @Override
    public void stopPreview() {
        if (mCameraDevice != null) {
            Log.i(TAG, "stopPreview");
            mCameraDevice.stopPreview();
        }
        mPreviewing = false;
    }

    private void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
            return;
        }

        List<Size> supported = Size.convert(mCameraCapabilities.getSupportedPhotoSizes());
        final double ASPECT_TOLERANCE = 0.02;
        for (Size size : supported) {
            if (Math.abs((double) 4 / 3 - (double) size.getWidth() / (double) size.getHeight()) < ASPECT_TOLERANCE) {
                mCameraSettings.setPhotoSize(size.toPortabilitySize());
                Log.i(TAG, "Photo size is " + size);
                break;
            }
        }

        List<Size> sizes = Size.convert(mCameraCapabilities.getSupportedPreviewSizes());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(sizes, 4 / 3f);

        if (optimalSize != null) {
            mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());
            // Bug 1159223 (FORWARD_NULL)
            if (optimalSize.width() != 0 && optimalSize.height() != 0) {
                updatePreviewAspectRatio((float) optimalSize.width()
                        / (float) optimalSize.height());
            }
        }

        mCameraDevice.applySettings(mCameraSettings);
        mCameraSettings = mCameraDevice.getSettings();

        mUI.initPreviewCoordinateTransformer();

        Log.d(TAG, "Preview size is " + optimalSize);
    }

    public RectF getNativePreviewArea() {
        if (mCameraSettings != null) {
            return new RectF(0, 0, mCameraSettings.getCurrentPreviewSize().height()
                    , mCameraSettings.getCurrentPreviewSize().width());
        }
        return null;
    }

    private void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0) {
            return;
        }
        if (aspectRatio < 1f) {
            aspectRatio = 1f / aspectRatio;
        }
        mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    private void setDisplayOrientation() {
        mDisplayRotation = CameraUtil.getDisplayRotation();
        /*
        CameraDeviceInfo.Characteristics info =
                mActivity.getCameraProvider().getCharacteristics(mCameraId);
        mDisplayOrientation = info.getPreviewOrientation(mDisplayRotation);
        RectF rectF = mActivity.getCameraAppUI().getPreviewArea();
        if (rectF != null && rectF.width() > 0 && rectF.height() > 0) {
            mCoordinateTransformer = new CameraCoordinateTransformer(false, mDisplayOrientation, rectF);
        }*/

        if (mCameraDevice != null) {
            mCameraDevice.setDisplayOrientation(mDisplayRotation);
        }
    }


    @Override
    public void on3DRangeFindPointsSet(Integer[] points) {
        if (mCameraSettings == null || mCameraDevice == null) {
            return;
        }
        /*
        Rect x1 = computeCameraRectFromPreviewCoordinates(l, t, 10);
        Rect x2 = computeCameraRectFromPreviewCoordinates(r, b, 10);
        int[] rangeFindPoints = new int[4];
        rangeFindPoints[0] = x1.centerX();
        rangeFindPoints[1] = x1.centerY();
        rangeFindPoints[2] = x2.centerX();
        rangeFindPoints[3] = x2.centerY();*/

        int[] intArray = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            intArray[i] = points[i];
        }
        mCameraSettings.set3DRangeFindPoints(intArray);
        mCameraDevice.applySettings(mCameraSettings);
    }

    private void requestCameraOpen() {
        mCameraId = CameraUtil.TDRANGFIND_CAMERA_ID;
        Log.d(TAG, " the camera id is " + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, true);
    }

    private void startPreview() {
        if (!checkPreviewPreconditions()) {
            return;
        }
        setDisplayOrientation();

        updateParametersPictureSize();

        mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
        mCameraDevice.applySettings(mCameraSettings);

        mCameraDevice.setPreviewTexture(mActivity.getCameraAppUI().getSurfaceTexture());
        mCameraDevice.startPreview();
        mPreviewing = true;
        mAppController.onPreviewStarted();
    }

    private boolean checkPreviewPreconditions() {
        if (mPaused) {
            return false;
        }

        if (mCameraDevice == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        }

        SurfaceTexture st = mActivity.getCameraAppUI().getSurfaceTexture();
        if (st == null) {
            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
            return false;
        }

        return true;
    }

    @Override
    public void resume() {
        // ISP 3A matching needs the property set before camera opened
        mPaused = false;
        mUI.onResume();
        requestCameraOpen();
        /* SPRD: fix bug 700467 TextureView need cover by using SurfaceView @{ */
        if(null != mActivity.getCameraAppUI()) {
            mActivity.getCameraAppUI().refreshTextureViewCover();
        }
        /* @} */
    }

    @Override
    public void pause() {
        mPaused = true;
        mUI.onPause();

        stopPreview();
        closeCamera();
        // ISP 3A matching needs the property set to zero here
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera! mCameraDevice=" + mCameraDevice);

        if (mCameraDevice == null) {
            Log.i(TAG, "already stopped.");
            return;
        }
        mActivity.getCameraProvider().releaseCamera(mCameraDevice.getCameraId());
        mPreviewing = false;
        mCameraDevice = null;
    }

    @Override
    public void onCameraAvailable(CameraProxy cameraProxy) {
        if(cameraProxy == null) // Bug 1159222 (REVERSE_INULL)
            return;

        mCameraDevice = cameraProxy;
        mCameraSettings = mCameraDevice.getSettings();
        mCameraDevice.set3DRangeFindDistanceCallback(mRangeFindDistanceCallback);
        mCameraCapabilities = mCameraDevice.getCapabilities();
        startPreview();
    }

    private CameraAgent.RangeFindDistanceCallback mRangeFindDistanceCallback = new CameraAgent.RangeFindDistanceCallback() {
        @Override
        public void onRangeFindDistanceReceived(final int resultCode, final double distance) {
            mUI.on3DRangeFindDistanceReceived(resultCode, distance);
        }
    };

    /*
    private Rect computeCameraRectFromPreviewCoordinates(int x, int y, int size) {
        if (mCoordinateTransformer == null) {
            RectF previewRectF = mActivity.getCameraAppUI().getPreviewArea();
            if (previewRectF != null && previewRectF.width() > 0 && previewRectF.height() > 0) {
                mCoordinateTransformer = new CameraCoordinateTransformer(false, mDisplayOrientation, previewRectF);
            }
        }

        Rect rect = CameraUtil.rectFToRect(mActivity.getCameraAppUI().getPreviewArea());
        int left = CameraUtil.clamp(x - size / 2, rect.left,
                rect.right - size);
        int top = CameraUtil.clamp(y - size / 2, rect.top,
                rect.bottom - size);

        RectF rectF = new RectF(left, top, left + size, top + size);
        return CameraUtil.rectFToRect(mCoordinateTransformer.toCameraSpace(rectF));
    }*/

    @Override
    public HardwareSpec getHardwareSpec() {
        return null;
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return null;
    }

    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {

    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {

    }

    @Override
    public void onShutterButtonClick() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public String getPeekAccessibilityString() {
        return null;
    }

    @Override
    public boolean getCameraPreviewState() {
        return mPreviewing;
    }
    @Override
    public void updatePortraitBackgroundReplacementType(){}
}
