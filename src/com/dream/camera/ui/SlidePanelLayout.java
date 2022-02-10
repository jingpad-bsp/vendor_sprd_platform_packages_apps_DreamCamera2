
package com.dream.camera.ui;

import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import android.app.Activity;
import com.android.camera.CameraActivity;
import android.content.Context;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SlidePanelLayout extends LinearLayout {

    private LinearLayout mSlidePanelContainer = null;
    private LinearLayout mSlidePanelDot3 = null;

    public SlidePanelLayout(Context context) {
        super(context);
    }

    public SlidePanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidePanelLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SlidePanelLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private final static String TAG = "CAM_SlidePanelLayout";

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSlidePanelContainer = (LinearLayout) findViewById(R.id.slide_panel_parent);
        mSlidePanelDot3 = (LinearLayout) findViewById(R.id.slide_panel_dot_3_layout);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mSlidePanelDot3.setVisibility(visibility);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSlidePanelContainer != null && CameraUtil.isNavigationEnable()) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSlidePanelContainer.getLayoutParams();
            lp.bottomMargin = CameraUtil.getNormalNavigationBarHeight() + getContext().getResources()
                    .getDimensionPixelSize(R.dimen.navigation_slide_panel_height);
            mSlidePanelContainer.setLayoutParams(lp);
        } else if(mSlidePanelContainer != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSlidePanelContainer.getLayoutParams();
            final float scale = getContext().getResources().getDisplayMetrics().scaledDensity;
            int slidePanelParentHeight = (int) (24 * scale + 0.5f) / 2;
            lp.bottomMargin = slidePanelParentHeight;
            mSlidePanelContainer.setLayoutParams(lp);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
