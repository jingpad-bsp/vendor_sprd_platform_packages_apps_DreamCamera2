package com.dream.camera.modules.filter.sprd;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.dream.camera.filter.ArcsoftSmallAdvancedFilter;
import com.dream.camera.filter.sprd.SprdGLSurfaceView;
import com.dream.camera.modules.filter.FilterModuleAbs;
import com.android.camera.settings.Keys;
import android.view.ViewStub;
import com.android.camera2.R;

public class FilterModuleSprd extends FilterModuleAbs {
    private static final Tag TAG = new Tag("FilterModuleSprd");
    private FilterModuleUISprd mFilterUI;
    protected ArcsoftSmallAdvancedFilter mArcsoftSmallAdvancedFilter;
    private SprdGLSurfaceView mSprdFilterGlView;

    public FilterModuleSprd(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        Log.d(TAG, "create ui");
        Log.d(TAG, "makeModuleUI E.");
        // initialize BaseUI object
        ViewStub viewStubAjustPanel = (ViewStub) activity.findViewById(R.id.layout_ae_lock_panel_id);
        if (viewStubAjustPanel != null) {
            viewStubAjustPanel.inflate();
        }
        mFilterUI = new FilterModuleUISprd(mActivity, this,
                mActivity.getModuleLayoutRoot(), this);
        mUI = mFilterUI;
        mActivity.setPreviewStatusListener(mUI);

        mSprdFilterGlView = mFilterUI.getSprdGLSurfaceView();
        mArcsoftSmallAdvancedFilter = mFilterUI.getArcsoftSmallAdvancedFilter();

        Log.d(TAG, "makeModuleUI X.");

        return mUI;
    }

    @Override
    public void onSurfaceTextureUpdated(){
        super.onSurfaceTextureUpdated();
        mArcsoftSmallAdvancedFilter.requestLayout();
    }

    @Override
    public Bitmap getPreviewBitmap() {
        if (mSprdFilterGlView != null) {
            return mSprdFilterGlView.getPreviewData();
        }
        return null;
    }

    @Override
    public void pause() {
        if (mSprdFilterGlView != null) {
            mSprdFilterGlView.SetPreviewStarted(false);
        }
        super.pause();
    }

    @Override
    protected void startPreview(boolean optimize) {
        if (mSprdFilterGlView != null) {
            SurfaceTexture surface = mSprdFilterGlView.getSurfaceTexture();
            Log.i(TAG, "surface = " + surface);
            if (surface != null) {
                Log.i(TAG, "StartPreveiwForFilter");
                StartPreveiwForFilter(surface, optimize);
            }
        }
    }

    @Override
    public void onPreviewStartedAfter() {
        Log.i(TAG, "onPreviewStartedAfter");
        mActivity.getCameraAppUI().textureViewRequestLayout();
        if (mSprdFilterGlView != null) {
            mSprdFilterGlView.SetPreviewStarted(true);
        }
        int type = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_FILTER_TYPE);
        mArcsoftSmallAdvancedFilter.setDreamFilterType(type);

    }

    @Override
    public int getPPEffectType(int filterType) {
        return filterType;
    }

    @Override
    protected boolean checkPreviewPreconditions() {
        if (mPaused) {
            return false;
        }

        if (mCameraDevice == null) {
            Log.i(TAG, "startPreview: camera device not ready yet.");
            return false;
        }

        if (mSprdFilterGlView == null)
            return false;
        SurfaceTexture surface = mSprdFilterGlView.getSurfaceTexture();
        if (surface == null) {
            Log.i(TAG, "startPreview: mSprdFilterGlView is not ready.");
            return false;
        }

        // if (!mCameraPreviewParamsReady) {
        // Log.i(TAG, "startPreview: parameters for preview is not ready.");
        // return false;
        // }
        return true;

    }

    @Override
    public void updateFilterPanelUI(int visible) {
        if (mArcsoftSmallAdvancedFilter != null) {
            mArcsoftSmallAdvancedFilter.setVisibility(visible);
        }
    }
}
