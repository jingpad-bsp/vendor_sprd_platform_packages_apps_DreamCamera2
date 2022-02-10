/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dream.camera.modules.filter;

import android.graphics.SurfaceTexture;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.app.CameraAppUI.PanelsVisibilityListener;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.MultiToggleImageButton;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamUI;
import com.dream.camera.SlidePanelManager;
import com.android.camera.PhotoUI;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.filter.ArcsoftSmallAdvancedFilter;
import com.android.camera2.R;

abstract public class FilterModuleUIAbs extends PhotoUI implements
        DreamFilterArcControlInterface,
        OrientationManager.OnOrientationChangeListener,
        PanelsVisibilityListener {
    private static final Log.Tag TAG = new Log.Tag("DreamFilterModuleUICopy");

    protected FrameLayout mPreviewLayout = null;
    protected ArcsoftSmallAdvancedFilter mArcsoftSmallAdvancedFilter;
    protected DreamFilterLogicControlInterface mFilterController;

    protected boolean mPaused = false;//SPRD:bug 961149
    protected int mLastDisplayOrientation = 0;
    protected View topPanel;

    public FilterModuleUIAbs(CameraActivity activity,
                             PhotoController controller, View parent,
                             DreamFilterLogicControlInterface filterController) {
        super(activity, controller, parent);

        mFilterController = filterController;
        // SPRD: Fix bug 578679, Add Filter type of symmetry right.
        mActivity.getOrientationManager().addOnOrientationChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        mPaused = true;
        super.onPause();
    }

    public void destroy() {
        if (mActivity != null && mActivity.getOrientationManager() != null) {
            mActivity.getOrientationManager().removeOnOrientationChangeListener(this);
        }
    }

    abstract public void setTransformMatrix();

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSurfaceTextureAvailableForFilter(SurfaceTexture surface,
                                                   int width, int height) {
        mFilterController.onPreviewUIReadyForFilter();

    }

    /* SPRD: Fix bug 578679, Add Filter type of symmetry right. @{ */
    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
                                     OrientationManager.DeviceOrientation deviceOrientation) {
        super.onOrientationChanged(orientationManager, deviceOrientation);
        int orientation = 0;
        if (mActivity.getOrientationManager() != null) {
            orientation = mActivity.getOrientationManager()
                    .getDeviceOrientation().getDegrees();
            Log.d(TAG, "onOrientationChanged: orientation = " + orientation);
        }

        if (mArcsoftSmallAdvancedFilter != null)
            mArcsoftSmallAdvancedFilter.setOrientation(orientation);
    }
    /* @} */

    public ArcsoftSmallAdvancedFilter getArcsoftSmallAdvancedFilter() {
        return mArcsoftSmallAdvancedFilter;
    }

    @Override
    public void onPanelsHidden() {
        if (mArcsoftSmallAdvancedFilter != null) {
            mArcsoftSmallAdvancedFilter.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPanelsShown() {
        if (mArcsoftSmallAdvancedFilter != null) {
            mArcsoftSmallAdvancedFilter.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateUI(float aspectRation) {
        super.updateUI(aspectRation);
        if (mArcsoftSmallAdvancedFilter != null)
            mArcsoftSmallAdvancedFilter.updateUI(aspectRation);
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        // TODO Auto-generated method stub
        DreamUtil dreamUtil = new DreamUtil();
        if (DreamUtil.BACK_CAMERA == dreamUtil.getRightCamera(DataModuleManager
                .getInstance(mActivity).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID))) {
            if (mBasicModule.isBeautyCanBeUsed()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_top_makeup_montionphoto_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_top_montionphoto_panel,
                            topPanelParent);
                }
            }
        } else {
            if (mBasicModule.isBeautyCanBeUsed()) {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_front_top_makeup_montionphoto_panel,
                            topPanelParent);
                }
            } else {
                if (topPanel == null) {
                    LayoutInflater lf = LayoutInflater.from(mActivity);
                    topPanel = lf.inflate(R.layout.autophoto_front_top_montionphoto_panel,
                            topPanelParent);
                }
            }
        }
        mActivity.getButtonManager().load(topPanel);
        bindAutoTopButtons();
        updateButtonState();
    }

    private void updateButtonState(){
        if (topPanel == null)
            return;
        MultiToggleImageButton montionPhotoButton = (MultiToggleImageButton)topPanel.findViewById(R.id.montionphoto_toggle_button_dream);
        MultiToggleImageButton refocusButton = (MultiToggleImageButton)topPanel.findViewById(R.id.refocus_toggle_button_dream);
        MultiToggleImageButton hdrButton = (MultiToggleImageButton)topPanel.findViewById(R.id.hdr_toggle_button_dream);
        MultiToggleImageButton makeupButton = (MultiToggleImageButton)topPanel.findViewById(R.id.make_up_display_toggle_button_dream);
        if (refocusButton != null) {
            refocusButton.setEnabled(false);
            //refocusButton.setState(0);
        }
        if (hdrButton != null) {
            hdrButton.setEnabled(false);
            hdrButton.setState(0);
        }
        if (makeupButton != null) {
            makeupButton.setEnabled(false);
            makeupButton.setState(0);
        }
        if(montionPhotoButton != null){
            montionPhotoButton.setEnabled(false);
            montionPhotoButton.setState(0);
        }
    }

    @Override
    public int getUITpye() {
        return DreamUI.DREAM_FILTER_UI;
    }

    @Override
    public void setButtonVisibility(int buttonId, int visibility) {

        if (ButtonManagerDream.BUTTON_FLASH_DREAM == buttonId
                || ButtonManagerDream.BUTTON_GIF_PHOTO_FLASH_DREAM == buttonId
                || ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM == buttonId) {
            View gap1 = mRootView.findViewById(R.id.flash_gap_1);
            if (gap1 != null) {
                gap1.setVisibility(visibility);
            }
            int visible = visibility == View.GONE ? View.VISIBLE : View.GONE;

            View gap2 = mRootView.findViewById(R.id.flash_gap_2);
            if (gap2 != null) {
                gap2.setVisibility(visible);
            }
            View gap3 = mRootView.findViewById(R.id.flash_gap_3);
            if (gap3 != null) {
                gap3.setVisibility(visible);
            }
        }

        super.setButtonVisibility(buttonId, visibility);
    }

    @Override
    public void updateSlidePanel() {
        if (!mActivity.isSecureCamera()) {
             SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                 SlidePanelManager.SETTINGS,View.VISIBLE);
             SlidePanelManager.getInstance(mActivity).focusItem(
                 SlidePanelManager.CAPTURE, false);
        } else {
             SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                 SlidePanelManager.MODE,View.INVISIBLE);
             SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                 SlidePanelManager.SETTINGS,View.VISIBLE);
             SlidePanelManager.getInstance(mActivity).focusItem(
                 SlidePanelManager.CAPTURE, false);
        }
    }
}
