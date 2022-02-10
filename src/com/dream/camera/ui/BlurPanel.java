
package com.dream.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.camera.CameraActivity;

public class BlurPanel extends LinearLayout {
    private boolean active = false;

    public BlurPanel(Context context) {
        super(context);
    }

    public BlurPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVisibility(int visibility) {
        if (!active && visibility == View.VISIBLE) {
            return;
        }
        super.setVisibility(visibility);
    }

    public void show() {
        if (active
                && !((CameraActivity) getContext()).getCameraAppUI().isModeListOpen()
                && !((CameraActivity) getContext()).getCameraAppUI().isSettingLayoutOpen()) {
            setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            hide();
        }
    }

    public boolean isActive() {
        return active;
    }
}
