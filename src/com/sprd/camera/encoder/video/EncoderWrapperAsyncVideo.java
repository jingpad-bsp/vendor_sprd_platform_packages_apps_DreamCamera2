package com.sprd.camera.encoder.video;

import com.sprd.camera.encoder.interfaces.Buffer;
import com.sprd.camera.encoder.interfaces.Encoder;
import com.sprd.camera.encoder.interfaces.EncoderWrapper;
import com.sprd.camera.encoder.interfaces.Source;

public class EncoderWrapperAsyncVideo extends EncoderWrapper {

    private static final String TAG = "EncoderWrapperAsyncVideo";

    public EncoderWrapperAsyncVideo(int width, int height) {
        int enconderType = TYPE_VIDEO;
        Source source = new SourceVideo(width,height);
        Buffer buffer = new Buffer("video");
        Encoder encoder = new EncoderVideoAsync(width,height,10);

        initialize(enconderType,source,buffer,encoder);
    }

    @Override
    public void setMirror(boolean mirror) {
        ((EncoderVideoAsync)mEncoder).setMirror(mirror);
    }
    @Override
    public void setOrientation(int orientation){
        ((EncoderVideoAsync)mEncoder).setOrientation(orientation);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
