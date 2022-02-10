
package com.dream.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import com.android.camera.CameraActivity;
import com.android.camera2.R;

public class AdjustPanel extends FrameLayout {
    private boolean active = false;
    private VerticalSeekBar verticalSeekBar;
    private ImageView adjustCircle;

    public AdjustPanel(Context context) {
        super(context);
    }

    public AdjustPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdjustPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static final int HIDE = 1;
    private static final int DELAY_HIDE_TIME = 3000;

    private Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE:
                    hide();
                    break;
            }
        }
    };

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            resetHideTime();
        }
    }

    public void resetHideTime() {
        mHander.removeMessages(HIDE);
        mHander.sendEmptyMessageDelayed(HIDE, DELAY_HIDE_TIME);
    }

    public void show() {
        if (active
                && !((CameraActivity) getContext()).getCameraAppUI().isModeListOpen()
                && !((CameraActivity) getContext()).getCameraAppUI().isSettingLayoutOpen()) {
            setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        setVisibility(View.INVISIBLE);
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

    public void setTranslation(float x, float y) {
        if (verticalSeekBar == null) {
            verticalSeekBar = (VerticalSeekBar) getRootView().findViewById(R.id.vseekbar);
        }

        if (adjustCircle == null) {
            adjustCircle = (ImageView) getRootView().findViewById(R.id.adjust_circle);
        }

        if (verticalSeekBar == null || adjustCircle == null)
            return;

        int vWidth = verticalSeekBar.getWidth();
        int vHeight = verticalSeekBar.getHeight();

        int cWidth = adjustCircle.getWidth();
        int cHeight = adjustCircle.getHeight();

        RectF rectPreview = null;
        int adjustPanelTop = 0;
        int adjustPanelBottom = 0;

        if (getContext() != null && ((CameraActivity) getContext()).getCameraAppUI() != null) {
            rectPreview = ((CameraActivity) getContext()).getCameraAppUI().getPreviewArea();
            adjustPanelTop = ((CameraActivity) getContext()).getCameraAppUI().getAdjustPanelTop();
            adjustPanelBottom = ((CameraActivity) getContext()).getCameraAppUI().getAdjustPanelBottom();
        }

        if (rectPreview != null) {
            y = y - rectPreview.top;

            float vX = 0;
            float vY = 0;
            float cX = 0;
            float cY = 0;

            if (x + cWidth / 2 + vWidth > rectPreview.right) {
                vX = x - cWidth / 2 - vWidth;
                cX = x - cWidth / 2;
            } else {
                vX = x + cWidth / 2;
                cX = x - cWidth / 2;
            }

            if (y - vHeight / 2 < adjustPanelTop - rectPreview.top) {
                vY = adjustPanelTop - rectPreview.top;
                cY = y - cHeight / 2;
            } else if (y + vHeight / 2 > Math.min(adjustPanelBottom - rectPreview.top, rectPreview.bottom - rectPreview.top)) {
                vY = Math.min(adjustPanelBottom - rectPreview.top, rectPreview.bottom - rectPreview.top) - vHeight;
                cY = y - cHeight / 2;
            } else {
                vY = y - vHeight / 2;
                cY = y - cHeight / 2;
            }

            verticalSeekBar.setTranslationX(vX);
            adjustCircle.setTranslationX(cX);

            verticalSeekBar.setTranslationY(vY);
            adjustCircle.setTranslationY(cY);
        }
    }

    protected boolean setFrame(int left, int top, int right, int bottom) {
        RectF rectPreview = null;
        if (getContext() != null && ((CameraActivity) getContext()).getCameraAppUI() != null) {
            rectPreview = ((CameraActivity) getContext()).getCameraAppUI().getPreviewArea();
        }

        if (rectPreview == null) {
            return super.setFrame(left, top, right, bottom);
        }

        return super.setFrame((int) rectPreview.left, (int) rectPreview.top, (int) rectPreview.right, (int) rectPreview.bottom);
    }
}
