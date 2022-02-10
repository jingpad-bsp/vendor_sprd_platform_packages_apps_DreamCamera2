package com.sprd.camera.encoder.video;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.sprd.camera.encoder.Util;
import com.sprd.camera.encoder.interfaces.EncoderASync;

public class EncoderVideoAsync extends EncoderASync {


    private static final String TAG = "EncoderVideoAsync";
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC ;
    private static final int COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
    private static final int I_FRAME_DURATION = 2;

    private String mMimeType = MIME_TYPE;
    private int mColorFormat = COLOR_FORMAT;
//    private int mIFrameDuration = I_FRAME_DURATION;
    public int mFrameWidth;
    public int mFrameHeight;
    private int mFrameRate;
    private int mBitRate;



    public EncoderVideoAsync(int frameWidth, int frameHeight, int frameRate) {
        super();
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;
        mFrameRate = frameRate;
        mBitRate = Util.calcBitRate(mFrameRate, mFrameWidth, mFrameHeight);

        Util.log(getTag(),"MediaVideoEncoder mFrameWidth = " + mFrameWidth + " mFrameHeight =" +
                mFrameHeight + " mFrameRate = " + mFrameRate + " mBitRate = " + mBitRate);
    }

    @Override
    public MediaFormat initializeMediaFormat() {
        final MediaCodecInfo videoCodecInfo = Util.selectVideoCodec(mMimeType);
        if (videoCodecInfo == null) {
            Util.log(TAG, "Unable to find an appropriate codec for " + mMimeType);
            return null;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mMimeType, mFrameWidth, mFrameHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);    // API >= 18
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);

        Util.log(TAG, " initializeMediaFormat : mMimeType = " +mMimeType + " mFrameWidth = " +mFrameWidth +
                " mFrameHeight = " + mFrameHeight + " mFrameRate ="+ mFrameRate
                + " mBitRate = " + mBitRate + " mColorFormat=" + mColorFormat);

        return mediaFormat;
    }

    @Override
    protected byte[] changeData(byte[] mediaData) {
        if(mMirror){
            Util.mirror(mediaData,mFrameWidth,mFrameHeight,mOrientation);
        }
        final byte[] i420 = Util.NV21toI420SemiPlanar(mediaData,mFrameWidth,mFrameHeight);
        return i420;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    private volatile boolean mMirror = false;
    public void setMirror(boolean mirror){
        mMirror = mirror;
    }

    private volatile int mOrientation = 0;
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }
}
