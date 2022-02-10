package com.sprd.camera.encoder.interfaces;

import android.util.Log;

import com.sprd.camera.encoder.Util;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Buffer {

    private String TAG = "MidiaBuffer";

    // nano seconds
    private static final long BUFFER_DURATION = (long)(1.5 * 1000 * 1000 * 1000l);
    private static final long BUFFER_TOLERANCE =100 * 1000 * 1000l;

    BufferCallBack mCallBack;
    public static interface BufferCallBack{
        // can't block method
        void onRecordDataAvailable(MediaDataStruct bufferData);
        // can't block method
        void onRecordEnd();
    }

    volatile boolean isRecording = false;
    long mRecordTimeUS;
    long mMinRecordTimeUS;
    long mMaxRecordTimeUS;

    public Buffer(String tag) {
        TAG = TAG + tag;
    }

    ConcurrentHashMap<Long,MediaDataStruct> mBufferHashMap = new ConcurrentHashMap<>();
    ArrayList<Long> mBufferIndex = new ArrayList<>();

    public synchronized void queueData(MediaDataStruct sourceData){
        Long currentTimeUS = sourceData.mTimeUS;
//        Util.log(TAG," time nano seconds = " + currentTimeUS);
        mBufferHashMap.put(currentTimeUS, sourceData);
        mBufferIndex.add(currentTimeUS);

        // find the remove item
        ArrayList<Long> removeList = new ArrayList<>();
        for (Long i : mBufferIndex) {
            long duration = currentTimeUS - i;
            if (duration > BUFFER_DURATION && duration - BUFFER_DURATION > BUFFER_TOLERANCE) {
                removeList.add(i);
            }
        }

        // remove the item
        for (Long item : removeList) {
            if(mBufferIndex.contains(item)){
                mBufferIndex.remove(item);
                mBufferHashMap.remove(item);
            }
        }
//        Util.log(TAG,"removeList length = " + removeList.size());
//        Util.log(TAG,"buffer length = " + mBufferHashMap.size());

        if (isRecording && mCallBack != null) {
            if (currentTimeUS <= mMaxRecordTimeUS) {
                mCallBack.onRecordDataAvailable(sourceData);
//                Util.log(TAG, "onRecordDataAvailable");
            } else if (currentTimeUS > mMaxRecordTimeUS) {
                if(currentTimeUS - mMaxRecordTimeUS < BUFFER_TOLERANCE){
                    mCallBack.onRecordDataAvailable(sourceData);
                } else {
                    Util.log(TAG, "stopRecord");
                    stopRecord();
                }
            }
        }
    }

    public synchronized long record(long timeUS) {
        isRecording = true;
        mRecordTimeUS = timeUS;
        mMinRecordTimeUS = mRecordTimeUS - BUFFER_DURATION;
        mMaxRecordTimeUS = mRecordTimeUS + BUFFER_DURATION;

        Util.log(TAG," mRecordTimeUS = " + mRecordTimeUS + " \n" +
                " mMinRecordTimeUS = " + mMinRecordTimeUS + " \n" +
                " mMaxRecordTimeUS = " + mMaxRecordTimeUS + " \n" +
                " BUFFER_DURATION = " + BUFFER_DURATION);

        // output the data which are already in buffer
        int i = 0;
        long startTimeUS = 0;
        long recordTimeUS = timeUS;
        for (Long item : mBufferIndex){
            if(timeUS - item < BUFFER_DURATION){
                if(mCallBack != null){
                    if(i == 0){
                        startTimeUS = item;
                    }
                    i++;
                    recordTimeUS = item;
                    mCallBack.onRecordDataAvailable(mBufferHashMap.get(item));

                }
            }
        }
        // return millsecond
        Util.log(TAG, " record timeus = " + recordTimeUS/1000L);
        Util.log(TAG, " record start timeus = " + startTimeUS/1000L);
        return (recordTimeUS - startTimeUS)/1000L;
    }

    public void stopRecord() {
        isRecording = false;
        if(mCallBack!= null){
            mCallBack.onRecordEnd();
        }
    }

    public synchronized void stop() {
        mBufferHashMap.clear();
        mBufferIndex.clear();

    }

    public void setCallBack(BufferCallBack callBack){
        mCallBack = callBack;
    }
}
