package com.sprd.camera.encoder.audio;

import com.sprd.camera.encoder.interfaces.Buffer;
import com.sprd.camera.encoder.interfaces.Encoder;
import com.sprd.camera.encoder.interfaces.EncoderWrapper;
import com.sprd.camera.encoder.interfaces.Source;

public class EncoderWrapperAsyncAudio extends EncoderWrapper {

    private static final String TAG = "EncoderWrapperAsyncAudio";

    public EncoderWrapperAsyncAudio() {
        int enconderType = TYPE_AUDIO;
        Source source = new SourceAudio();
        Buffer buffer = new Buffer("audio");
        Encoder encoder = new EncoderAudioAsync();

        initialize(enconderType,source,buffer,encoder);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
