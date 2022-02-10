package com.sprd.camera.encoder.interfaces;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;

import com.sprd.camera.encoder.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;

public abstract class EncoderASync implements Encoder{

    HandlerThread mCallBackThread;
    Handler mCallBackHandler;
    EncoderCallBack mCallback;

    MediaCodec mMediaEncoder;
    MediaFormat mMediaFormat;

    @Override
    public void config(){
        Util.log(getTag(),"configEncoder");
        mCallBackThread = new HandlerThread(getTag());
        mCallBackThread.start();
        mCallBackHandler = new Handler(mCallBackThread.getLooper());

        try {
            MediaFormat mediaFormatTemp = initializeMediaFormat();
            if(mediaFormatTemp == null){
                Util.log(getTag(), "encoder start mMediaFormat = null");
                return;
            }
            mMediaEncoder = MediaCodec.createEncoderByType(mediaFormatTemp.getString(
                    MediaFormat.KEY_MIME));
            mMediaEncoder.setCallback(mCodecCallback,mCallBackHandler);
            mMediaEncoder.configure(mediaFormatTemp,null,null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startRecord(){
        Util.log(getTag(),"start Encoder");
        config();
        mMediaEncoder.start();
    }

    @Override
    public void stopRecord(){
        Util.log(getTag(),"stopRecord Encoder");
        if(mMediaEncoder != null){
            mMediaEncoder.flush();
            mMediaEncoder.stop();
            mMediaEncoder.release();
            mMediaEncoder = null;
        }
    }

    @Override
    public void stop(){
        if(mMediaEncoder != null){
            encodeQueue.clear();
            try {
                encodeQueue.put(MediaDataStruct.endData);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void release(){
        Util.log(getTag(),"release Encoder");
        if(mCallBackThread != null){
            mCallBackThread.quitSafely();
        }
    }

    BlockingQueue<MediaDataStruct> encodeQueue = new LinkedBlockingQueue<>();

    @Override
    public void queueData(MediaDataStruct bufferData){
        try {
//            Util.log(getTag(),"Encoder encodeQueue queueData");
            encodeQueue.put(bufferData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private MediaCodec.Callback mCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onOutputFormatChanged( MediaCodec codec, MediaFormat format) {
            Util.log(getTag(), "onEncoderOutputFormatChanged");
            mMediaFormat = format;
            if(mCallback != null){
                mCallback.onFormatChanged(mMediaFormat);
            }
        }

        @Override
        public void onInputBufferAvailable( MediaCodec codec, int index) {
//            Util.log(getTag(), " onInputBufferAvailable index = " + index);
            encode(index);
        }

        @Override
        public void onOutputBufferAvailable( MediaCodec codec, int index,  MediaCodec.BufferInfo info) {
//            Util.log(getTag(), " onOutputBufferAvailable index = " + index);
            drain(index,info);

        }

        @Override
        public void onError( MediaCodec codec,  MediaCodec.CodecException e) {
            Util.log(getTag(), "onError");
        }
    };


    private void encode(int bufferIndex){
        try {
            final ByteBuffer inputBuffer = mMediaEncoder.getInputBuffer(bufferIndex);
            inputBuffer.clear();

            MediaDataStruct inputData = encodeQueue.take();

            if(inputData == MediaDataStruct.endData){
                Util.log(getTag(), "encode BUFFER_FLAG_END_OF_STREAM");
                mMediaEncoder.queueInputBuffer(bufferIndex,0, 0,
                        0,BUFFER_FLAG_END_OF_STREAM);
                for(int i = 0; i<4; i++){
                    encodeQueue.put(new MediaDataStruct(0,new byte[0]));
                }
            } else {
                int inputDataLength = inputData.mMediaData.length;
                if(inputDataLength == 0){
                    mMediaEncoder.queueInputBuffer(bufferIndex,0, inputDataLength, inputData.mTimeUS,0);
                } else {
                    byte[] data = changeData(inputData.mMediaData);
                    inputBuffer.put(data,0,data.length);

//                    Util.log(getTag(), "encode offset = 0 " + " length = "
//                            + inputDataLength + " presentationTimeUs = " + inputData.mTimeUS);
                    mMediaEncoder.queueInputBuffer(bufferIndex,0, inputDataLength, inputData.mTimeUS,0);

                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalStateException e){
            e.printStackTrace();
        }

    }

    protected byte[] changeData(byte[] mediaData) {
        return mediaData;
    }


    private void drain(int index, MediaCodec.BufferInfo info){

        int flag = info.flags;
        if(flag == BUFFER_FLAG_END_OF_STREAM){
            Util.log(getTag(),"drain end BUFFER_FLAG_END_OF_STREAM");
            if(mCallback != null){
                mCallback.onEncodeEnd(mMediaFormat, Util.getPTSUs());
            }
            stopRecord();

        } else {
            if(info.presentationTimeUs == 0){
                Util.log(getTag(),"drain info.presentationTimeUs == 0");
                mMediaEncoder.releaseOutputBuffer(index, false);
                return;
            }
            try {
                ByteBuffer encodedData = mMediaEncoder.getOutputBuffer(index);

//                Util.log(getTag(), " drain length = " + encodedData.remaining()+
//                        " BufferInfo presentationTimeUs = " + info.presentationTimeUs +
//                        " size = "+ info.size + " offset " + info.offset + "info.flags = " + info.flags);
                if(mCallback != null){
                    mCallback.onEncodeDataAvailable(mMediaFormat,info,encodedData);
                }
            } catch (IllegalStateException e){
                e.printStackTrace();
            } finally {
                mMediaEncoder.releaseOutputBuffer(index, false);
            }
        }
    }

    @Override
    public void setCallBack(EncoderCallBack callBack){
        mCallback = callBack;
    }
}
