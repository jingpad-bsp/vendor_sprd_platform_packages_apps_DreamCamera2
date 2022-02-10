package com.sprd.camera.encoder.interfaces;

import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ByteBuffer;

public interface Encoder {

    public interface EncoderCallBack {
        void onFormatChanged(MediaFormat format);
        void onEncodeDataAvailable(MediaFormat mFormat, MediaCodec.BufferInfo mBufferInfo,
                                   ByteBuffer mEncodedData);
        void onEncodeEnd(MediaFormat format, long lastTimeUS);
    }

    public abstract void config();

    public abstract void startRecord();
    public abstract void stopRecord();
    public abstract void stop();
    public abstract void release();
    public abstract void queueData(MediaDataStruct bufferData);
    public abstract void setCallBack(EncoderCallBack callBack);

    public abstract MediaFormat initializeMediaFormat();

    public abstract String getTag();


}
