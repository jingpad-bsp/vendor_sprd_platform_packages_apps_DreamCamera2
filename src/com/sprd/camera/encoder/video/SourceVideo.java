package com.sprd.camera.encoder.video;


import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.sprd.camera.encoder.Util;
import com.sprd.camera.encoder.interfaces.MediaDataStruct;
import com.sprd.camera.encoder.interfaces.Source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SourceVideo extends Source implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "SourceVideo";

    private static final int INPUT_MEDIA_FORMAT = ImageFormat.YUV_420_888;
    private static final int OUTPUT_MEDIA_FORMAT = ImageFormat.YUV_420_888;
    private static final int MAX_IMAGES = 1;

    volatile boolean isStarted = false;

    ImageReader mImageReader;
    Handler mImageReaderHander;
    HandlerThread mImageReaderHandlerThread;
    Surface mSurface;

    int mImageWidth;
    int mImageHeight;
    int mInputImageFormat;
//    int mMaxImages;

    public SourceVideo(int width, int height){
        mImageWidth = width;
        mImageHeight = height;
        mInputImageFormat = INPUT_MEDIA_FORMAT;
//        mMaxImages = MAX_IMAGES;
    }

    @Override
    public void config() {
        Util.log(getTag(), "config");
        mImageReaderHandlerThread = new HandlerThread("image reader");
        mImageReaderHandlerThread.start();
        mImageReaderHander = new Handler(mImageReaderHandlerThread.getLooper());
        mImageReader = ImageReader.newInstance(mImageWidth, mImageHeight, mInputImageFormat, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(this, mImageReaderHander);
        mSurface = mImageReader.getSurface();
    }

    @Override
    public void start() {
        isStarted = true;
    }

    @Override
    public void stop() {
        isStarted = false;
    }

    @Override
    public synchronized void release() {
        Util.log(getTag(), "release");
        mSurface = null;
        mImageReader.close();
        mImageReaderHandlerThread.quitSafely();
    }

    @Override
    public synchronized void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireNextImage();
        transform(image);
        if(image != null){
            image.close();
        }
    }

    private void transform(Image image) {
        final byte[] nv21 =  Util.getNV21FromImage(image);
//        final byte[] i420 = Util.NV21toI420SemiPlanar(nv21,mImageWidth,mImageHeight);
        // Util.log(getTag(), "onImageAvailable length = " + nv21.length);
        if(isStarted && mCallBack != null && nv21 != null){
            mCallBack.onSourceDataUpdate(new MediaDataStruct(Util.getPTSUs(),nv21));
        }
    }

    @Override
    public Surface getSurface() {
        Util.log(getTag(), "getSurface");
        return mSurface;
    }

    @Override
    public String getTag() {
        return TAG;
    }

}
