
package com.dream.camera;

public class DreamModule {

    public final static int UNDEFINED = -1;
    public final static int INTERVAL_MODULE = 0;
    public final static int AUDIOPICTURE_MODULE = 1;
    public final static int FILTER_MODULE = 4;
    public final static int QR_MODULE = 5;
    public final static int CONTINUE_MODULE = 6;
    public final static int WIDEANGLE_MODULE = 7;
    public final static int TD_PHOTO_MODULE = 21;
    public final static int REFOCUS_MODULE = 22;
    public final static int FRONT_REFOCUS_MODULE = 23;
    public final static int ULTRAWIDEANGLE_MODULE = 24;
    public final static int TDNR_PHOTO_MODULE = 25;
    public final static int TDNR_PRO_PHOTO_MODULE = 26;
    public final static int FUSION_PHOTO_MODULE = 35;
    public final static int PORTRAITBACKGROUNDREPLACEMENT = 48;

    public final static int MACRO_PHOTO_MODULE = 31;

    /*camera pic type*/
    public final static int TYPE_NORMAL = 0;
    public final static int TYPE_BURST = 51;
    public final static int TYPE_HDR = 52;
    public final static int TYPE_AUDIO_PIC = 53;
    public final static int TYPE_AUDIO_HDR_PIC = 54;
    public final static int TYPE_BURST_COVER = 55;
    public final static int TYPE_THUMBNAIL = 56;
    public final static int TYPE_MODE_BLUR_BOKEH = 12;//0X000C blur has bokeh
    public final static int TYPE_MODE_BLUR = 268;//0X010C blur not bokeh
    public final static int TYPE_MODE_BOKEH_BOKEH = 16;//0X0010 real-bokeh has bokeh
    public final static int TYPE_MODE_BOKEH = 272;//0X0110 real-bokeh not bokeh
    public final static int TYPE_MODE_BOKEH_HDR_BOKEH = 17;//0X0011 real-bokeh with hdr has bokeh
    public final static int TYPE_MODE_BOKEH_HDR = 273;//0X0111 real-bokeh with hdr not bokeh
    public final static int TYPE_AI_PHOTO = 36; //AI Pic
    public final static int TYPE_AI_HDR = 37;
    public final static int TYPE_PORTRAITBACKGROUNDREPLACEMENT = 48;
    /*add for FDR*/
    public final static int TYPE_FDR = 57;
    public final static int TYPE_AUDIO_FDR_PIC = 58;
    public final static int TYPE_MODE_BOKEH_FDR_BOKEH = 18;
    public final static int TYPE_MODE_BOKEH_FDR = 274;
    public final static int TYPE_AI_FDR = 38;

    /*camera video type*/
    public final static int TYPE_SLOW_MOTION = 375; //slow motion
    public final static int TYPE_VIDEO = 376;// not slow motion video

    public int getModuleTpye() {
        return UNDEFINED;
    }

    public boolean isFreezeFrameDisplayShow() {
        return false;
    }

    public boolean canShutter() {
        return true;
    }

    public boolean isBurstThumbnailNotInvalid() {
        return false;
    }

    public void restoreCancelBurstTag() {
    }

    public void onStopRecordVoiceClicked() {}

    public boolean isAudioRecording() {
        return false;
    }

    public boolean isVideoRecording() {
        return false;
    }
    public boolean getCaptureState() {
        return false;
    }
    public boolean getCameraPreviewState() {
        return false;
    }
    //Sprd :Fix bug745376
    public boolean isEnableDetector(){
        return true;
    }

    public void forceOnSensorSelfShot(){}

    public boolean isNeedThumbCallBack() {
        return false;
    }

    public boolean isNeedUpdateUri() {
        return true;
    }

    public boolean isSupportTouchEV() {
        return com.android.camera.util.CameraUtil.isTouchEvEnable();
    }

    public boolean isSupportVideoCapture() {
        return true;
    }

    public boolean isFlashSupported(){
        return false;
    }
    public void updateFlashLevelUI(){}
}
