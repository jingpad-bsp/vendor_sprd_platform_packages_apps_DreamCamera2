package com.dream.camera.modules.rangefind;

import android.graphics.RectF;
/**
 * The interface that controls range find module
 */
public interface TDRangeFindModuleController {

    void onPreviewUIReady();

    void onPreviewUIDestroyed();

    void on3DRangeFindPointsSet(Integer[] points);

    RectF getNativePreviewArea();

}
