package com.dream.camera.modules.tdnrprophoto;

import java.util.HashMap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.PhotoModule;

import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera2.R;

import android.widget.FrameLayout;
import com.dream.camera.SlidePanelManager;
import com.android.camera.PhotoUI;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.util.DreamUtil;
import android.hardware.Camera.Face;
import com.android.camera.ui.FaceView;
import android.graphics.RectF;
import com.dream.camera.MakeupController;
import com.dream.camera.MakeupController.*;

public class TDNRProPhotoUI extends PhotoUI implements ResetListener {
    private static final Log.Tag TAG = new Log.Tag("TDNRPhotoUI");

    //private ImageButton mSettingsButton;
    private View topPanel;

    public TDNRProPhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        DreamUtil dreamUtil = new DreamUtil();
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.tdnrphoto_top_panel,
                        topPanelParent);
            }
            mActivity.getButtonManager().load(topPanel);

        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.tdnrphoto_top_panel,
                        topPanelParent);
            }
            mActivity.getButtonManager().load(topPanel);
        }

        //bindCountDownButton();
        bindCameraButton();
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        super.updateSlidePanel();
    }

    @Override
    public void onSettingReset() {
    }

    @Override
    public void onResume(){
        DataModuleManager.getInstance(mActivity).addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        DataModuleManager.getInstance(mActivity).removeListener(this);
    }

    public boolean isInFrontCamera() {
        DreamUtil dreamUtil = new DreamUtil();
        return DreamUtil.FRONT_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID));
    }
}
