
package com.dream.camera.modules.macrophoto;


import android.graphics.*;
import com.android.camera.CameraActivity;
import com.android.camera.Exif;
import com.android.camera.PhotoModule;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.superresolution.SuperResolutionNative;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.DreamModule;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MacroPhotoModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("MacroPhotoModule");

    public MacroPhotoModule(AppController app) {
        super(app);
        if (CameraUtil.isSRFusionEnable()) {
            isAlgorithmProcessOnApp = true;
        }
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        if (activity == null) {
            return null;
        }
        return new MacroPhotoUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return false;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    protected void requestCameraOpen() {
        mCameraId = CameraUtil.BACK_MACRO_CAMERA_ID;
        if (CameraUtil.isHighResolutionScaleTest())
            mCameraId = 0;
        Log.i(TAG, "requestCameraOpen mCameraId:" + mCameraId);
        mActivity.getCameraProvider().requestCamera(mCameraId, useNewApi());
    }

    public boolean isUseSurfaceView() {
        return CameraUtil.isSurfaceViewAlternativeEnabled();
    }

    /* SPRD: optimize camera launch time @{ */
    public boolean useNewApi() {
        // judge VGesture enable state first will be better, like:
        // if (isShouldShowVGesture()) return false;
        // but isShouldShowVGesture will throw exception if useNewApi() is
        // called before module initialized, and no negative effect is found
        // until now, so just ignore this judgement temporarily.
        if (isUseSurfaceView()) {
            return true;
        }
        return GservicesHelper.useCamera2ApiThroughPortabilityLayer(null);
    }
    /* @} */

    @Override
    protected void updateParametersThumbCallBack() {
        if (CameraUtil.isNormalNeedThumbCallback()) {
            Log.i(TAG, "setNeedThumbCallBack true ");
            mCameraSettings.setNeedThumbCallBack(true);
            mCameraSettings.setThumbCallBack(1);
        } else {
            super.updateParametersThumbCallBack();
        }
    }

    @Override
    synchronized public JpegBundle doPreSaveFinalPhoto(JpegBundle dataBundle) {
        if (CameraUtil.isSRFusionEnable()) {
            int newWidth = 0;
            int newHeight = 0;
            int scale = CameraUtil.getHighResolutionScale();

            int paramindex = dataBundle.jpegData.length - 12;

            byte[] tmpbyte = CameraUtil.subByte(dataBundle.jpegData,paramindex+8,4);
            int yuvsize = CameraUtil.byteArrToInt(tmpbyte);
            Log.v(TAG, "doPreSaveFinalPhoto jpegsize:" + dataBundle.jpegData.length);
            Log.v(TAG, "doPreSaveFinalPhoto yuvsize:" + yuvsize);
            tmpbyte = CameraUtil.subByte(dataBundle.jpegData,paramindex+4,4);
            newHeight = CameraUtil.byteArrToInt(tmpbyte);
            Log.v(TAG, "doPreSaveFinalPhoto newHeight:" + newHeight);
            tmpbyte = CameraUtil.subByte(dataBundle.jpegData,paramindex,4);
            newWidth = CameraUtil.byteArrToInt(tmpbyte);
            Log.v(TAG, "doPreSaveFinalPhoto newWidth:" + newWidth);

                byte[] oriyuv = CameraUtil.subByte(dataBundle.jpegData,paramindex-yuvsize,yuvsize);;
//                CameraUtil.saveBytes(oriyuv,"oriyuv");
                if (CameraUtil.isHighResolutionScaleTest()){
                    oriyuv = CameraUtil.yuvScaleDown(oriyuv,newWidth,newHeight,scale);
                    newWidth = newWidth/scale;
                    newHeight = newHeight/scale;
//                    CameraUtil.saveBytes(oriyuv,"oriyuv_scale");
                }
            try {
                SuperResolutionNative.init();
                final byte[] yuvData = SuperResolutionNative.process(oriyuv, newWidth, newHeight,scale);
                SuperResolutionNative.deinit();

//                CameraUtil.saveBytes(yuvData,"yuv");

                YuvImage image = new YuvImage(yuvData, ImageFormat.NV21, newWidth*scale, newHeight*scale, null);
                if (image != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0, 0, newWidth*scale, newHeight*scale), 100, stream);

                    dataBundle.jpegData = stream.toByteArray();
                    Log.v(TAG, "doPreSaveFinalPhoto after yuv to jpeg jpegsize:" + dataBundle.jpegData.length);
                    stream.close();
                }

                if (!CameraUtil.isHighResolutionScaleTest()){
                    final ExifInterface exif = dataBundle.exif;
                    int width = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                    int height = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                    exif.setTagValue(ExifInterface.TAG_PIXEL_X_DIMENSION, new Integer(
                            width*scale));
                    exif.setTagValue(ExifInterface.TAG_PIXEL_Y_DIMENSION, new Integer(
                            height*scale));
                }
            } catch (Exception ex) {
                Log.e(TAG, "doPreSaveFinalPhoto Error:" + ex.getMessage());
                return dataBundle;
            } finally {
                System.gc();
            }
            return dataBundle;
        } else {
            return dataBundle;
        }
    }
    @Override
    protected void updateParametersPictureSize() {
        if (mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
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
        Log.d(TAG, "Picture size is " + pictureSize);
        mCameraSettings.setPhotoSize(pictureSize.toPortabilitySize());

        if (CameraUtil.isSRFusionEnable()) {
            Log.d(TAG,"setSuperMacroPhotoEnable 1");
            mCameraSettings.setSuperMacroPhotoEnable(1);
        } else {
            Log.d(TAG,"setSuperMacroPhotoEnable 0");
            mCameraSettings.setSuperMacroPhotoEnable(0);
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
        if (optimalSize != null) { // Bug 1159232 (NULL_RETURNS)
            if (!optimalSize.equals(original)) {
                Log.i(TAG, "setting preview size. optimal: " + optimalSize
                        + "original: " + original);
                mCameraSettings.setPreviewSize(optimalSize.toPortabilitySize());
            }

            if (optimalSize.width() != 0 && optimalSize.height() != 0) {
                Log.i(TAG, "updating aspect ratio");
                mUI.updatePreviewAspectRatio((float) optimalSize.width()
                        / (float) optimalSize.height());
            }
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }

    @Override
    public int getModuleTpye() {
        return DreamModule.MACRO_PHOTO_MODULE;
    }
}
