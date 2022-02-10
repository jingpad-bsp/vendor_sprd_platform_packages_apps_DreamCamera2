
package com.dream.camera.modules.intentcapture;

import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import android.os.Handler;
import android.os.Looper;
import com.android.ex.camera2.portability.CameraAgent;

import com.android.camera.PhotoModule;
import android.hardware.Camera.Parameters;
import com.android.camera.CameraActivity;
import android.view.View;
import android.view.ViewStub;
import com.android.camera2.R;

import com.dream.camera.util.DreamUtil;
import com.dream.camera.settings.DataModuleManager;

public class DreamIntentCaptureModule extends PhotoModule {
    private static final Log.Tag TAG = new Log.Tag("DreamIntentCaptureModule");

    public DreamIntentCaptureModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        // TODO Auto-generated method stub
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        return new DreamIntentCaptureUI(activity, this, activity.getModuleLayoutRoot());
    }

    public boolean isSupportTouchAFAE() {
        return true;
    }

    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    public boolean isBeautyCanBeUsed() {
        return CameraUtil.isCameraBeautyEnable();
    }
}
