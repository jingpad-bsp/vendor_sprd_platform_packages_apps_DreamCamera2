package com.sprd.camera.encoder.interfaces;

public class MediaDataStruct {

    public static MediaDataStruct endData = new MediaDataStruct(-1,null);
    public long mTimeUS;
    public byte[] mMediaData;

    public MediaDataStruct(long timeUS, byte[] mediaData){
        mTimeUS = timeUS;
        mMediaData = mediaData;
    }

    public void release() {
        mMediaData = null;
    }
}
