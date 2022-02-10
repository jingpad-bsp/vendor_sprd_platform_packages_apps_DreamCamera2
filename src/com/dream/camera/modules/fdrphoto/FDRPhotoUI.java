package com.dream.camera.modules.fdrphoto;

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
import com.android.camera.ui.RotateImageView;

import android.graphics.RectF;
import com.dream.camera.MakeupController;
import com.dream.camera.MakeupController.*;

public class FDRPhotoUI extends PhotoUI implements ResetListener {
    private static final Log.Tag TAG = new Log.Tag("TDNRPhotoUI");

    //private ImageButton mSettingsButton;
    private View topPanel;

    public FDRPhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.tdnrphoto_top_panel,
                        topPanelParent);
            }
            mActivity.getButtonManager().load(topPanel);

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


    @Override
    public void updateAiSceneView(RotateImageView view , int visible , int index) {
        if (view != null) {
            if (index == 5) {
                view.setImageResource(R.drawable.ai_scene_night);
                view.setVisibility(visible);
            } else {
                Log.i(TAG , "AiSceneView INVISIBLE");
                view.setVisibility(View.INVISIBLE);
            }
        }
    }

}
