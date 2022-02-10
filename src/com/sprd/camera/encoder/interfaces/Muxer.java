package com.sprd.camera.encoder.interfaces;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.google.android.util.AbstractMessageParser;
import com.sprd.camera.encoder.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Muxer implements EncoderWrapper.EncoderWrapperCallback{

    MuxerCallBack mCallBack;

    public static interface MuxerCallBack{
        void onRecordEnd();
    }

    public static final String TAG = "Muxer";

    HashMap<MediaFormat, Integer> mTrackMap = new HashMap<>();
    HashMap<MediaFormat,Long> mTrackStartTimeUs = new HashMap<>();
    AtomicInteger mTrackCount = new AtomicInteger(0);
    volatile int endCount = 0;
    Object mMuxerLock = new Object();

    MediaMuxer mMuxer;
    String mFilePath;
    private long mstartTimeUS = 0;
    int mOrientation = 90;

    HashMap<MediaFormat, Integer> mFrameCount = new HashMap<>();

    @Override
    public void onFormatChanged(MediaFormat format) {
        Util.log(TAG,"onFormatChanged");
    }

    @Override
    public void onEncodeDataAvailable(MediaFormat format, MediaCodec.BufferInfo bufferInfo,
                                      ByteBuffer encodedData) {

//        Util.log(TAG," onEncodeDataAvailable mTrackCount = " + mTrackCount + " pretime = " + bufferInfo.presentationTimeUs );
        // start muxer
        if(mTrackMap.size() != mTrackCount.get() &&
                mTrackMap.get(format) == null){
            synchronized (mMuxerLock){
                if(mTrackMap.size() != mTrackCount.get() && mTrackMap.get(format) == null){
                    int track = mMuxer.addTrack(format);
                    Util.log(TAG,"onEncodeDataAvailable add track id = " + track + " format = " + format.toString());
                    mTrackMap.put(format,track);
                    mFrameCount.put(format,0);
                    if(mTrackMap.size() == mTrackCount.get()){
                        muxerStart();
                    } else {
                        return;
                    }
                }
            }
        }

        // write data
        if(mTrackMap.size() == mTrackCount.get()){
            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs / 1000l;

            if(mstartTimeUS == 0){
                mstartTimeUS = bufferInfo.presentationTimeUs;
                bufferInfo.presentationTimeUs = 0;
            } else {
                if(bufferInfo.presentationTimeUs < mstartTimeUS){
                    encodedData.clear();
                    return;
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - mstartTimeUS;
            }

//            Util.log(TAG,"onEncodeDataAvailable writeSampleData buffer size = " + bufferInfo.size + "" +
//                    " pretime = " + bufferInfo.presentationTimeUs +
//                    " offset = " + bufferInfo.offset +
//                    " track = " + mTrackMap.get(format));
            mFrameCount.put(format,mFrameCount.get(format) +1);
            synchronized (mMuxerLock){
                mMuxer.writeSampleData(mTrackMap.get(format),encodedData,bufferInfo);
            }
            encodedData.clear();
            return;
        }
    }

    @Override
    public void onEncodeEnd(MediaFormat format, long lastTimeUS) {
        synchronized (mMuxerLock){
            if(mTrackMap.size() == 0){
                return;
            }
            endCount++;
            Util.log(TAG,"onEncodeEnd .... endCount = " + endCount);
            if(endCount == mTrackCount.get()){
                for (MediaFormat tempformat : mTrackMap.keySet()){
                    Util.log(TAG,"onEncodeEnd Track = " + mTrackMap.get(tempformat)
                            + " framecount = " + mFrameCount.get(tempformat));
                }
                resetStatus();
                Util.log(TAG,"onEncodeEnd ");
                muxerStop();
            }
        }
    }

    private void resetStatus(){
        mTrackMap.clear();
        mFrameCount.clear();
        endCount= 0;
        mTrackStartTimeUs.clear();
        mstartTimeUS = 0;
    }

    public void muxerInit(String filePath) {
        Util.log(TAG, "muxerInit");
        try {
            mFilePath = filePath;
            mMuxer = new MediaMuxer(mFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxer.setOrientationHint(mOrientation);
            Util.log(TAG, "filepath = " + mFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOrientation(int orientation){
        mOrientation = orientation;
    }

    private void muxerStart() {
        Util.log(TAG, "muxerStart");
        mMuxer.start();
    }

    private void muxerStop() {
        Util.log(TAG, "muxerStop");
        try {
            if(mMuxer != null){
                mMuxer.stop();
                mMuxer.release();
            }
        } catch (IllegalStateException e){
            e.printStackTrace();
        }

        mMuxer = null;

        if(mCallBack != null){
            mCallBack.onRecordEnd();
        }

    }

    public synchronized void increaceTrack() {

        mTrackCount.incrementAndGet();
        Util.log(TAG," increaceTrack mTrackCount = " + mTrackCount.get());
    }

    public void decreaceTrack() {
        mTrackCount.decrementAndGet();
        Util.log(TAG," decreaceTrack mTrackCount = " + mTrackCount.get());
    }

    public void setCallBack(MuxerCallBack callBack){
        mCallBack = callBack;
    }
}
