package com.dream.camera.modules.filter.sprd;

import android.graphics.Bitmap;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.dream.camera.modules.filter.FilterModuleAbs;

/**
 * Created by SPREADTRUM\ying.sun on 18-1-4.
 */

public class FilterModuleSprd extends FilterModuleAbs {
    public FilterModuleSprd(AppController app) {
        super(app);
    }

    public Bitmap getPreviewBitmap() {
        return null;
    }

    public PhotoUI createUI(CameraActivity activity) {
        return null;
    }

    protected void startPreview(boolean optimize) {
    }

    public void onPreviewStartedAfter() {
    }

    public int getPPEffectType(int filterType) {
        return -1;
    }

    protected boolean checkPreviewPreconditions() {
        return false;
    }

    public void updateFilterPanelUI(int visible) {
    }
}
