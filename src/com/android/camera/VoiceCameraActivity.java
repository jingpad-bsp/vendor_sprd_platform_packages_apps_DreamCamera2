package com.android.camera;

import android.os.Bundle;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;
import com.android.camera.settings.Keys;
import com.android.camera.debug.Log;
import com.dream.camera.settings.DataModuleManager;

public class VoiceCameraActivity extends CameraActivity {
    private static final String EXTRA_USE_FRONT_CAMERA = "android.intent.extra.USE_FRONT_CAMERA";
    private static final String EXTRA_CAMERA_OPEN_ONLY = "com.google.assistant.extra.CAMERA_OPEN_ONLY";
    private static final String EXTRA_TIMER_DURATION_SECONDS = "com.google.assistant.extra.TIMER_DURATION_SECONDS";
    private static final String INTENT_EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";
    private static final String REF_VALUE = "android-app://com.google.android.googlequicksearchbox/https/www.google.com";
    private static final String INTENT_VOICE_CAMERA = "android.camera.action.VOICE_CAMERA";
    private static final Log.Tag TAG = new Log.Tag("VoiceCameraActivity");

    @Override
    public void onCreateTasks(Bundle state) {
        Intent intent = getIntent();
        String action = intent.getAction();
        //check gts assistant camera
        Log.d(TAG,"isVoiceInteractionRoot is "+isVoiceInteractionRoot()
                +", CAMERA_OPEN_ONLY is "+intent.getExtra(EXTRA_CAMERA_OPEN_ONLY)
                +", TIMER_DURATION_SECONDS is "+intent.getExtra(EXTRA_TIMER_DURATION_SECONDS)
                +", REFERRER_NAME is "+intent.getExtra(INTENT_EXTRA_REFERRER_NAME)
                +", action is "+action);
        if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)){
            String ref = intent.getStringExtra(INTENT_EXTRA_REFERRER_NAME);
            if (!isVoiceInteractionRoot() && (ref == null || !ref.equals(REF_VALUE)))
                return;
            if (!isVoiceInteractionRoot()){
                setOpenCameraOnly(true);
            } else {
                boolean openCameraOnly = intent.getBooleanExtra(EXTRA_CAMERA_OPEN_ONLY,false);
                int DurationS = intent.getIntExtra(EXTRA_TIMER_DURATION_SECONDS,3);
                Log.d(TAG,"CAMERA_OPEN_ONLY is "+openCameraOnly+", TIMER_DURATION_SECONDS is "+DurationS);
                setVoiceIntentCamera(true);
                setOpenCameraOnly(openCameraOnly);
                setTimeDurationS(DurationS);
            }
        } else if (INTENT_VOICE_CAMERA.equals(action)) {
            boolean openCameraOnly = intent.getBooleanExtra(EXTRA_CAMERA_OPEN_ONLY,false);
            int DurationS = intent.getIntExtra(EXTRA_TIMER_DURATION_SECONDS,3);
            Log.d(TAG,"CAMERA_OPEN_ONLY is "+openCameraOnly+", TIMER_DURATION_SECONDS is "+DurationS);
            setVoiceIntentCamera(true);
            setOpenCameraOnly(openCameraOnly);
            setTimeDurationS(DurationS);
        }
        boolean useFrontCam = intent.getBooleanExtra(EXTRA_USE_FRONT_CAMERA,false);
        Log.d(TAG,"USE_FRONT_CAMERA is "+useFrontCam);
        if (useFrontCam) {
            DataModuleManager.getInstance(VoiceCameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID,1);
        } else {
            DataModuleManager.getInstance(VoiceCameraActivity.this).getTempCameraModule().set(Keys.KEY_CAMERA_ID,0);
        }
        super.onCreateTasks(state);
    }

    @Override
    public void onRestartTasks() {
        Log.d(TAG,"onRestartTasks");
        setVoiceIntentCamera(false);
    }
}
