package com.sprd.camera.encoder.interfaces;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.sprd.camera.encoder.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class EncoderWrapper {

    EncoderWrapperCallback mCallBack;

    public static interface EncoderWrapperCallback{
        void onFormatChanged(MediaFormat format);
        void onEncodeDataAvailable(MediaFormat format, MediaCodec.BufferInfo bufferInfo,
                                   ByteBuffer encodedData);
        void onEncodeEnd(MediaFormat format, long lastTimeUS);
    }

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;
    int mEncoderType;
    protected Source mSource;
    Buffer mBuffer;
    protected Encoder mEncoder;
    long mRecordTimes;

    Source.SourceCallBack mSourCallBack = new Source.SourceCallBack() {
        @Override
        public void onSourceDataUpdate(MediaDataStruct sourceData) {
//            Util.log(getTag(),"mSourCallBack  mBuffer.queueData");
            mBuffer.queueData(sourceData);
        }
    };

    Buffer.BufferCallBack mBufferCallBack = new Buffer.BufferCallBack() {
        @Override
        public void onRecordDataAvailable(MediaDataStruct bufferData) {
//            Util.log(getTag(),"mBufferCallBack mEncoder.queueData");
            mEncoder.queueData(bufferData);

            if(mEncoderType == TYPE_VIDEO){
                if(end.get()){
                    end.set(false);
                    i.set(0);
                }
//                writetofile(bufferData.mMediaData, bufferData.mTimeUS/1000l);
            }
        }

        @Override
        public void onRecordEnd() {
            Util.log(getTag(),"mBufferCallBack onRecordEnd ");
            mEncoder.queueData(MediaDataStruct.endData);
            end.set(true);
        }
    };


    Encoder.EncoderCallBack mEncoderCallBack = new Encoder.EncoderCallBack() {
        @Override
        public void onFormatChanged(MediaFormat format) {
            Util.log(getTag(),"mEncoderCallBack onFormatChanged ");
            if(mCallBack != null){
                mCallBack.onFormatChanged(format);
            }
        }

        @Override
        public void onEncodeDataAvailable(MediaFormat format, MediaCodec.BufferInfo bufferInfo,
                                          ByteBuffer encodedData) {
//            Util.log(getTag(),"mEncoderCallBack onEncodeDataAvailable ");
            if(mCallBack != null){
                mCallBack.onEncodeDataAvailable(format,bufferInfo,encodedData);
            }
        }

        @Override
        public void onEncodeEnd(MediaFormat format, long lastTimeUS) {
            Util.log(getTag(),"mEncoderCallBack onEncodeEnd ");
            if(mCallBack != null){
                mCallBack.onEncodeEnd(format,lastTimeUS);
            }
        }
    };

    public EncoderWrapper(){

    }

//    public Handler mHandler;
//    public HandlerThread mHandlerThread;

    protected void initialize(int enconderType, Source source, Buffer buffer, Encoder encoder){
        mEncoderType = enconderType;
        mSource = source;
        mBuffer = buffer;
        mEncoder = encoder;

        mSource.setCallBack(mSourCallBack);
        mBuffer.setCallBack(mBufferCallBack);
        mEncoder.setCallBack(mEncoderCallBack);

//        mHandlerThread = new HandlerThread("encoder");
//        mHandlerThread.start();
//        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void config(){
        mSource.config();
    }

    public void start(){
        mSource.start();
    }

    public void record(long timeUS){
        mRecordTimes = mBuffer.record(timeUS);
        mEncoder.startRecord();
    }

    public void stopRecord(){
        mBuffer.stopRecord();
    }

    public void stop(){
        mSource.stop();
        mBuffer.stop();
    }

    public void release(){
        mSource.release();
        mEncoder.release();
    }

    public int getMediaType(){
        return mEncoderType;
    }

    public Surface getSurface(){
        return mSource.getSurface();
    }
    
    public void setEncoderWraperCallBack(EncoderWrapperCallback callBack){
        mCallBack = callBack;
    }

    protected abstract String getTag();

    public long getRecordTimes() {
        return mRecordTimes;
    }

    public void setMirror(boolean mirror) {
    }
    public void setOrientation(int orientation){

    }

    AtomicInteger i = new AtomicInteger(0);
    AtomicBoolean end = new AtomicBoolean(true);
//    private void writetofile(byte[] nv21, long timeus) {
//
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (EncoderWrapper.this){
//                    String filepath = Util.getCaptureFile(Environment.DIRECTORY_DCIM, "_" + i.getAndIncrement() + ".t", timeus).toString();
//                    Util.log("filepath", "filepath = " + filepath);
//                    File file = new File(filepath);
//                    try {
//                        FileOutputStream outputStream = new FileOutputStream(file);
//                        outputStream.write(nv21,0,nv21.length);
//                        outputStream.close();
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//    }
}
