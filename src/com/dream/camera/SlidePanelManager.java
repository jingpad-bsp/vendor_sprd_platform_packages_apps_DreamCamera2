
package com.dream.camera;

import android.graphics.Color;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

public class SlidePanelManager {

    private static SlidePanelManager mSlidePanelManager;
    private static CameraActivity mActivity;

    public final static int MODE = 0;
    public final static int CAPTURE = 1;
    public final static int SETTINGS = 2;

    private View slidePanelParent;
    private ImageView modeDotView;
    private ImageView captureDotView;
    private ImageView settingsDotView;

    private SlidePanelManager(CameraActivity activity) {
        mActivity = activity;
        init();
    }

    public static SlidePanelManager getInstance(CameraActivity activity) {
         /*
          * if we start camera via SecureCameraActivity, we should recreate SlidePanelManager to fresh
          * slidePanelParent
         */
        if (mSlidePanelManager == null || activity != mActivity) {
            mSlidePanelManager = new SlidePanelManager(activity);
        }
        return mSlidePanelManager;
    }

    public void init() {
        if (mActivity == null)
            return;
        ViewStub viewStubSlidePanel = (ViewStub) mActivity
                .findViewById(R.id.slide_panel_id);
        if (viewStubSlidePanel != null) {
            viewStubSlidePanel.inflate();
        }
        if (slidePanelParent == null){
            slidePanelParent = mActivity.findViewById(R.id.slide_panel_parent);
            mActivity.getCameraAppUI().initSlidePanel(slidePanelParent);

            modeDotView = (ImageView) mActivity.findViewById(R.id.slide_panel_dot_mode);
            captureDotView = (ImageView) mActivity.findViewById(R.id.slide_panel_dot_capture);
            settingsDotView = (ImageView) mActivity.findViewById(R.id.slide_panel_dot_settings);
        }
    }

    public void focusItem(int id, boolean animation) {
        switch (id) {
            case MODE:
                modeDotView.setEnabled(true);
                captureDotView.setEnabled(false);
                settingsDotView.setEnabled(false);
                break;
            case CAPTURE:
                modeDotView.setEnabled(false);
                captureDotView.setEnabled(true);
                settingsDotView.setEnabled(false);
                break;
            case SETTINGS:
                modeDotView.setEnabled(false);
                captureDotView.setEnabled(false);
                settingsDotView.setEnabled(true);
                break;
        }
        return;
    }

    public void udpateSlidePanelShow(int id, int visible) {
        switch (id) {
            case MODE:
                modeDotView.setVisibility(visible);
                break;
            case CAPTURE:
                captureDotView.setVisibility(visible);
                break;
            case SETTINGS:
                settingsDotView.setVisibility(visible);
                break;
        }
        return;
    }

    public void onDestroy() {
        mSlidePanelManager = null;
        mActivity = null;
    }

    public View getSlidePanelParent() {
        if (slidePanelParent != null)
            return slidePanelParent;
        return mActivity.findViewById(R.id.slide_panel_parent);
    }
}
