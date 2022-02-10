
package com.dream.camera.modules.intentvideo;

import com.android.camera.app.AppController;
import com.android.camera.CameraActivity;
import com.android.camera.VideoUI;

import com.android.camera.VideoModule;

public class DreamIntentVideoModule extends VideoModule {

    public DreamIntentVideoModule(AppController app) {
        super(app);
    }

    public VideoUI createUI(CameraActivity activity) {
        return new DreamIntentVideoUI(activity, this, activity.getModuleLayoutRoot());
    }

}
