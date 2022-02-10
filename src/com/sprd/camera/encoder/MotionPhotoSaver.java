package com.sprd.camera.encoder;

import android.graphics.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.XmpBuilder;

import java.io.File;

public class MotionPhotoSaver {
    // jpeg
    private static final String TAG = "MotionPhotoSaver";
    private byte[] mJpegData;
    private String mJpegTitle;
    private long mJpegDate;
    private int mJpegWidth, mJpegHeight, mJpegOrientation;
    private ExifInterface mJpegExif;
    private Location mJpegLocation;
    private MediaSaver mMediaSaver;
    private MediaSaver.OnMediaSavedListener mOnMediaSavedListener;

    private boolean mJpegReady = false;
    private boolean mRecordReady = false;

    // video
    private long mTimeUS;
    private String mRecordFilepath;

    // xmp builder
    XmpBuilder mXmpBuilder;

    HandlerThread mHandlerThread;
    Handler mHandler;

    MediaSaver.OnMediaSavedListener onMediaSavedListenerProxy = new MediaSaver.OnMediaSavedListener(){

        @Override
        public void onMediaSaved(Uri uri, String title) {
            if(mOnMediaSavedListener != null){
                mOnMediaSavedListener.onMediaSaved(uri,title);
//                deleteData();
            }
        }
    };

    Runnable mSaveRunnable = new Runnable() {
        @Override
        public void run() {
//            mMediaSaver.addImage(mJpegData, mJpegTitle, mJpegDate, mJpegLocation,
//                    mJpegWidth, mJpegHeight, mJpegOrientation, mJpegExif, onMediaSavedListenerProxy);
//            // genera xmp data
            mXmpBuilder = generateXmpBuilder();

//            // save file and update DB
            mMediaSaver.addImage(mJpegData, mJpegTitle, mJpegDate, mJpegLocation,
                    mJpegWidth, mJpegHeight, mJpegOrientation, mJpegExif,
                    onMediaSavedListenerProxy,"image/jpeg", mRecordFilepath, mXmpBuilder );
        }
    };

    Runnable mDelRunnable = new Runnable() {
        @Override
        public void run() {
             if(mRecordFilepath != null){
                 File file = new File(mRecordFilepath);
                 if(file.exists()){
                     file.delete();
                 }
             }
        }
    };

    public MotionPhotoSaver(){
        mHandlerThread = new HandlerThread("montion photo");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }


    public void initData(final byte[] jpegData, String title, long date,
                         int width, int height, int orientation, ExifInterface exif,
                         Location location, MediaSaver.OnMediaSavedListener l, MediaSaver mediaSaver) {
        mJpegData = jpegData;
        mJpegTitle = title;
        mJpegDate = date;
        mJpegWidth = width;
        mJpegHeight = height;
        mJpegOrientation = orientation;
        mJpegExif = exif;
        mJpegLocation = location;
        mOnMediaSavedListener = l;
        mMediaSaver = mediaSaver;
        mJpegReady = true;
        android.util.Log.e(TAG, "initData  mJpegReady = " + true);
        saveData();
    }

    public byte[] getJpegData(){
        return mJpegData;
    }

    public void setRecordData(long recordTimeUS, String filePath) {
        mTimeUS = recordTimeUS;
        mRecordFilepath = filePath;
        mRecordReady = true;
        android.util.Log.e(TAG, "setRecordData  mRecordReady = " + true);
        saveData();
    }

    public boolean isSaving(){
        android.util.Log.e(TAG, "mJpegReady = " + mJpegReady);
        android.util.Log.e(TAG, "mRecordReady = " + mRecordReady);

        return mJpegReady || mRecordReady;
    }

    public void setSaving(boolean saving){
        mJpegReady = saving;
        mRecordReady = saving;
    }

    public void release(){
        mHandlerThread.quitSafely();
    }

    private void saveData() {
        if(mJpegReady && mRecordReady){
            mHandler.post(mSaveRunnable);
            mJpegReady = false;
            mRecordReady = false;
        }
    }

    public void deleateData(String filePath){
        mRecordFilepath = filePath;
        deleteData();
    }

    private void deleteData() {
        mHandler.post(mDelRunnable);
    }


    private XmpBuilder generateXmpBuilder() {
        XmpBuilder builder = new XmpBuilder();
        builder.setMPTimeStampUs(mTimeUS);
        int length = mJpegData.length -1  - CameraUtil.findJpegEndIndex(mJpegData);
        builder.setMPPaddingLength(length);
//        mRecordFilepath  = "/storage/emulated/0/DCIM/Camera/VID_20190521_153725.mp4";
        builder.setMPVideoLength(Util.getFileSize(mRecordFilepath));
        return builder;
    }
}
