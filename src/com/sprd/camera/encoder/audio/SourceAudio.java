package com.sprd.camera.encoder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import com.sprd.camera.encoder.Util;
import com.sprd.camera.encoder.interfaces.MediaDataStruct;
import com.sprd.camera.encoder.interfaces.Source;

import java.nio.ByteBuffer;

public class SourceAudio extends Source {
    private static final String TAG = "SourceAudio";

    protected static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_DEFAULT;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    private static final int STATUS_START = 0x00000001;
    private static final int STATUS_STOP = STATUS_START<<1;
    private static final int STATUS_RELEASE = STATUS_START<<2;
    private volatile int mStatus = STATUS_STOP;
    private Object mRecordingLock = new Object();
    private AudioRecordThread mRecordThread;

    private AudioRecord mAudioRecord;

    private int mBufferSize;
    private int mSampleRate;
    private int mChannel;
    private int mAudioFormat;
    private int mAudioSource;

    private class AudioRecordThread extends Thread{


        @Override
        public void run() {

            while (true){

                if ((mStatus & STATUS_START) != 0) {
                    mAudioRecord.startRecording();
                    Util.log(TAG, "mAudioRecord.startRecording()");
                    while ((mStatus & STATUS_START) != 0){
                        int readSize;
                        final ByteBuffer buf = ByteBuffer.allocateDirect(mBufferSize);
                        buf.clear();
                        readSize = mAudioRecord.read(buf, mBufferSize);
//                        byte[] inputData = new byte[mBufferSize];
//                        readSize = mAudioRecord.read(inputData,0,mBufferSize);
//                        Util.log(TAG, " mBufferSize = " + mBufferSize + " readsize = " + readSize);

                        if (readSize == AudioRecord.ERROR_INVALID_OPERATION ||
                                readSize == AudioRecord.ERROR_BAD_VALUE) {
                            Util.log(TAG, "ERROR_INVALID_OPERATION  ERROR_BAD_VALUE");
                            break;
                        }
                        if (readSize > 0 && mCallBack != null) {
                        byte[] inputData = new byte[readSize];
                        buf.get(inputData,0,readSize);
//                        buf.clear();
                              mCallBack.onSourceDataUpdate(new MediaDataStruct(Util.getPTSUs(),
                                    inputData));

                        }
                    }

                    Util.log(TAG, "startRecording     end");

                } if ((mStatus & STATUS_STOP) != 0) {
                    synchronized (mRecordingLock){
                        try {
                            if(mAudioRecord != null){
                                Util.log(TAG, "mAudioRecord.stop() start");
                                mAudioRecord.stop();
                                Util.log(TAG, "mAudioRecord.stop() end");
                            }
                            if((mStatus & STATUS_STOP) != 0){
                                mRecordingLock.wait();
                            }
                            
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } if ((mStatus & STATUS_RELEASE) != 0) {
                    if(mAudioRecord != null){
                        Util.log(TAG, "mAudioRecord.stop() start");
                        mAudioRecord.stop();
                        Util.log(TAG, "mAudioRecord.stop() end");
                    }
                    Util.log(TAG, "mAudioRecord.release() start");
                    mAudioRecord.release();
                    Util.log(TAG, "mAudioRecord.release() end");
                    break;
                }
            }
        }
    }

    public SourceAudio(){
        Util.log(TAG, "SourceAudio()");
        mSampleRate = SAMPLE_RATE;
        mChannel = CHANNEL;
        mAudioFormat = AUDIO_FORMAT;
        mAudioSource = AUDIO_SOURCE;
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, mAudioFormat);

    }

    @Override
    public void config() {
        Util.log(TAG, "config()");
        mRecordThread = new AudioRecordThread();
        mRecordThread.start();
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate,
                mChannel, mAudioFormat, mBufferSize);
    }

    @Override
    public void start(){
        Util.log(TAG, "start()");
        changeStatus(STATUS_START);

    }
    @Override
    public void stop(){
        Util.log(TAG, "stop()");
        changeStatus(STATUS_STOP);


    }

    @Override
    public void release() {
        Util.log(TAG, "release()");
        changeStatus(STATUS_RELEASE);
    }

    private void changeStatus(int status){
        mStatus = status;
        synchronized (mRecordingLock){
            mRecordingLock.notify();
        }
    }

    @Override
    public Surface getSurface() {
        return null;
    }


    @Override
    public String getTag() {
        return TAG;
    }
}
