package com.sprd.camera.encoder.interfaces;

import android.view.Surface;

import java.util.List;

public abstract class Recorder {

    public abstract void setOrientation(int mDisplayOrientation);

    public static interface RecorderCallBack{
        void onRecordEnd(long recordTimeUS, String filePath);
    }

    protected RecorderCallBack mCallBack;

    public void setCallBack(RecorderCallBack callBack){
        mCallBack = callBack;
    }

    public abstract void addEncoderWrapper(EncoderWrapper encoderWrapper);
    public abstract void config();
    public abstract List<Surface> getSurface();
    public abstract void start();
    public abstract void record(long timeUS);
    public abstract void stopRecord();
    public abstract void stop();
    public abstract void release();

    public abstract void resetSize(int width, int height);

}
