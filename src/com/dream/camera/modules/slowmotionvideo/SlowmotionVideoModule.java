
package com.dream.camera.modules.slowmotionvideo;

import android.view.MotionEvent;
import com.android.camera.CameraActivity;
import com.android.camera.VideoUI;
import com.android.camera.app.AppController;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.camera.settings.Keys;
import com.android.camera.VideoModule;
import com.android.camera2.R;
import com.android.camera.util.CameraUtil;

public class SlowmotionVideoModule extends VideoModule {

    public SlowmotionVideoModule(AppController app) {
        super(app);
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity, isSecureCamera, isCaptureIntent);
        showSlowmotionHint();
    }
    //dream ui check 226
    public void showSlowmotionHint() {
        boolean shouldSlowmotionHint = mDataModule.getBoolean(Keys.KEY_CAMERA_SLOWMOTION_HINT);
        if (shouldSlowmotionHint == true) {
            //SPRD Fix Bug:658194 slowmotionhint should not show when only one slowmotion rate is supported
            String slowMotionValue = CameraPictureSizesCacher.getCacheSlowMotionForCamera(mAppController.getAndroidContext());
            if (slowMotionValue != null && slowMotionValue.split(",").length > 3) {
                CameraUtil.toastHint(mActivity, R.string.dream_slowmotion_module_warning);
            }
            mDataModule.set(Keys.KEY_CAMERA_SLOWMOTION_HINT, false);
        }
    }

    @Override
    public void onLongPress(MotionEvent var1){}

    @Override
    public VideoUI createUI(CameraActivity activity) {
        return new SlowmotionVideoUI(activity, this, activity.getModuleLayoutRoot());
    }
}
