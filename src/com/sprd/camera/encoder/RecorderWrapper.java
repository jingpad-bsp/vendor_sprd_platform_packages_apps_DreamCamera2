package com.sprd.camera.encoder;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.sprd.camera.encoder.audio.EncoderWrapperAsyncAudio;
import com.sprd.camera.encoder.interfaces.EncoderWrapper;
import com.sprd.camera.encoder.video.EncoderWrapperAsyncVideo;
import com.sprd.camera.encoder.interfaces.Muxer;
import com.sprd.camera.encoder.interfaces.Recorder;

import java.util.ArrayList;
import java.util.List;

public class RecorderWrapper extends Recorder implements Muxer.MuxerCallBack {

    public static final String TAG = "RecorderWrapper";

    private volatile boolean isRecording = false;
    private volatile boolean isStarting = false;
    private volatile boolean isRelease = false;

    ArrayList<EncoderWrapper> mEncoderWrapperList = new ArrayList<>();
    Muxer mMuxer;
    String mFilePath;
    volatile long mRecordTimeUS;
    int mWidth;
    int mHeight;
    private Context mContext;

    MyHandler mHandler;
    HandlerThread mHandlerThread;

    class MyHandler extends Handler{

        public static final int START = 1;
        public static final int RECORD = 2;
        public static final int STOP_RECORD = 3;
        public static final int STOP = 4;
        public static final int RELEASE = 5;

        public MyHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:
                    interStart();
                    break;
                case RECORD:
                    interRecord(mRecordTimeUS);
                    break;
                case STOP_RECORD:
                    interStopRecord();
                    break;
                case STOP:
                    interStop();
                    break;
                case RELEASE:
                    interRelease();
                    break;
            }
        }
    }

    public RecorderWrapper(Context context, int width, int height){
        mContext = context;
        mWidth = width;
        mHeight = height;

        mHandlerThread = new HandlerThread("record wrapper");
        mHandlerThread.start();
        mHandler= new MyHandler(mHandlerThread.getLooper());

        mMuxer = new Muxer();
        mMuxer.setCallBack(this);
        addEncoderWrapper(new EncoderWrapperAsyncVideo(width,height));
        addEncoderWrapper(new EncoderWrapperAsyncAudio());
    }

    @Override
    public void addEncoderWrapper(EncoderWrapper encoderWrapper) {
        Util.log(TAG, "addEncoderWrapper");
        mEncoderWrapperList.add(encoderWrapper);
        mMuxer.increaceTrack();
        encoderWrapper.setEncoderWraperCallBack(mMuxer);
    }

    @Override
    public void config() {
        Util.log(TAG, "config");
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.config();
        }
    }

    @Override
    public List<Surface>  getSurface() {
        Util.log(TAG, "getSurface");
        List<Surface> surfaces = new ArrayList<>();
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            if(encoderWrapper.getMediaType() == EncoderWrapper.TYPE_VIDEO)
                surfaces.add(encoderWrapper.getSurface());
        }

        return surfaces;
    }

    public void setMirror(boolean mirror){
        Util.log(TAG, "setMirror = " + mirror);
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            if(encoderWrapper.getMediaType() == EncoderWrapper.TYPE_VIDEO){
                encoderWrapper.setMirror(mirror);
            }

        }

    }

    @Override
    public void start() {
        mHandler.sendEmptyMessage(MyHandler.START);
    }

    @Override
    public void record(long timeUS) {
        Util.log(TAG, "record");
        mRecordTimeUS = timeUS;
        mHandler.sendEmptyMessage(MyHandler.RECORD);
    }

    @Override
    public void stopRecord() {
        mHandler.sendEmptyMessage(MyHandler.STOP_RECORD);
    }


    @Override
    public void stop() {
        mHandler.sendEmptyMessage(MyHandler.STOP);
    }

    @Override
    public void release() {
        mHandler.sendEmptyMessage(MyHandler.RELEASE);
    }

    @Override
    public void setOrientation(int displayOrientation){
        mMuxer.setOrientation(displayOrientation);
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            if(encoderWrapper.getMediaType()== EncoderWrapper.TYPE_VIDEO){
                encoderWrapper.setOrientation(displayOrientation);
            }
        }
    }

    private void interStart(){
        Util.log(TAG, "start");
        isStarting = true;
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.start();
        }
    }

    private void interRecord(long timeUS){
        isRecording = true;
//        mFilePath = Util.getCaptureFile(Environment.DIRECTORY_DCIM, ".mp4").toString();
        mFilePath = Util.getCaptureFile(mContext, ".mp4").toString();
        mMuxer.muxerInit(mFilePath);
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.record(timeUS);
            if(encoderWrapper.getMediaType() == EncoderWrapper.TYPE_VIDEO){
                mRecordTimeUS = encoderWrapper.getRecordTimes();
            }
        }
    }

    private void interStopRecord(){
        Util.log(TAG, "stopRecord");
        isRecording = false;
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.stopRecord();
        }
    }

    private void interStop(){
        Util.log(TAG, "stop");
        isStarting = false;
        if(isRecording){
            interStopRecord();
        }

        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.stop();
        }
    }

    private void interRelease(){
        Util.log(TAG, "release");
        isRelease = true;
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            encoderWrapper.release();
        }
        mCallBack = null;
        mHandlerThread.quitSafely();
    }

    @Override
    public void resetSize(int width, int height){
        Util.log(TAG, "resetSize width = " + width + " height = " + height);
        mWidth = width;
        mHeight = height;

        ArrayList<EncoderWrapper> removeList = new ArrayList<>();
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            if(encoderWrapper.getMediaType()== EncoderWrapper.TYPE_VIDEO){
                encoderWrapper.stop();
                encoderWrapper.release();
                mMuxer.decreaceTrack();
                removeList.add(encoderWrapper);
            }
        }
        mEncoderWrapperList.removeAll(removeList);

        addEncoderWrapper(new EncoderWrapperAsyncVideo(mWidth,mHeight));
        for(EncoderWrapper encoderWrapper : mEncoderWrapperList){
            if(encoderWrapper.getMediaType()== EncoderWrapper.TYPE_VIDEO){
                encoderWrapper.config();
            }
        }

        if(isStarting){
            start();
        }
    }

    @Override
    public void onRecordEnd() {
        isRecording = false;
        if(mCallBack != null){
            Util.log(TAG, "onRecordEnd");
            mCallBack.onRecordEnd(mRecordTimeUS,mFilePath);
        }
    }

    public boolean isStarting() {
        return isStarting;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isRelease() {
        return isRelease;
    }

    public int getmWidth() {
        return mWidth;
    }

    public int getmHeight() {
        return mHeight;
    }

}
