
package com.dream.camera.dreambasemodules;

import android.view.ViewGroup;

import com.android.camera.ui.RotateImageView;

public interface DreamInterface {

    // Generate a view to fit top panel.
    public void fitTopPanel(ViewGroup topPanelParent);

    // Generate views to fit extend panel.
    public void fitExtendPanel(ViewGroup extendPanelParent);

    // Update icons on bottom panel.
    public void updateBottomPanel();

    // Update item on slide panel.
    public void updateSlidePanel();

    public void updateAiSceneView(RotateImageView view , int visible , int type);
    public int getAiSceneTip();
}
