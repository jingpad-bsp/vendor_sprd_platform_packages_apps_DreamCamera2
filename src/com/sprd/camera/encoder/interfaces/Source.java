package com.sprd.camera.encoder.interfaces;

import android.view.Surface;

public abstract class Source {

    protected SourceCallBack mCallBack;

    public static interface SourceCallBack{
        void onSourceDataUpdate(MediaDataStruct sourceData);
    }

    public abstract Surface getSurface();
    public abstract void config();

    public abstract void start();

    public abstract void stop();

    public abstract void release();

    public abstract String getTag();

    public void setCallBack(SourceCallBack callBack) {
        mCallBack = callBack;
    }
}
