package com.sprd.camera.encoder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.sprd.camera.encoder.Util;
import com.sprd.camera.encoder.interfaces.EncoderASync;

public class EncoderAudioAsync extends EncoderASync {
    private static final String TAG = "EncoderAudioAsync";

    protected static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_DEFAULT;
    private static final int BIT_RATE = SAMPLE_RATE*CHANNEL*16;

    private String mMimeType;
    private int mSampleRate;
    private int mAudioFormat;
    private int mChannel;
    private int mBitRate;

    public EncoderAudioAsync() {
        mMimeType = MIME_TYPE;
        mSampleRate = SAMPLE_RATE;
        mAudioFormat = AUDIO_FORMAT;
        mChannel = CHANNEL;
        mBitRate = BIT_RATE;

        Util.log(getTag(),"MediaAudioEncoder mMimeType = " + mMimeType + " mSampleRate =" +
                mSampleRate + " mAudioFormat = " +mAudioFormat+ " mChannel = " + mChannel +
                " mBitRate = " + mBitRate);
    }

    @Override
    public MediaFormat initializeMediaFormat() {
        int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, mAudioFormat);

        MediaFormat format = MediaFormat.createAudioFormat(mMimeType, mSampleRate, mChannel);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannel);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,mBufferSize * 2);
        Util.log(getTag()," initializeMediaFormat mMimeType = " + mMimeType + " mSampleRate = " + mSampleRate +
                " mAudioFormat = " +mAudioFormat + " mChannel = " + mChannel + " mBitRate = " + mBitRate
                + " mBufferSize = " + mBufferSize);

        return format;
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
