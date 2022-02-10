
package com.dream.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar.OnSeekBarChangeListener;

import android.widget.TextView;
import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.util.CameraUtil;
import com.dream.camera.ui.VerticalZoomBar;
import com.android.camera2.R;

public class ZoomPanel extends FrameLayout implements PreviewOverlay.OnDreamZoomUIChangedListener
        ,VerticalZoomBar.OnExtendTipListener, VerticalZoomBar.OnProgressStateChangeListener {
    private static final Log.Tag TAG = new Log.Tag("ZoomPanel");
    private VerticalZoomBar mZoomBar;
    private CameraActivity mActivity;

    private TextView mExtendTipView;

    public ZoomPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (CameraActivity) context;
    }

    @Override
    public void setZoomVisible(int state) {
        this.setVisibility(state);
        if (mZoomBar.isSimple()){
            if(View.VISIBLE == state){
                mActivity.getCameraAppUI().updateExtendPanelUI(View.GONE);
                // SPRD: nj dream camera test debug 120
                mActivity.getCameraAppUI().updateFilterPanelUI(View.GONE);
            } else {
                mActivity.getCameraAppUI().updateExtendPanelUI(View.VISIBLE);
                // SPRD: nj dream camera test debug 120
                mActivity.getCameraAppUI().updateFilterPanelUI(View.VISIBLE);
            }
        }

        if (View.VISIBLE != state) {
            hideTip();
            mZoomBar.resetState();
        }

    }

    @Override
    public int getZoomVisible() {
        return getVisibility();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate(); // Bug 1159311 (coverity: Missing call to superclass)
        mZoomBar = (VerticalZoomBar) findViewById(R.id.zoom_bar);
        mZoomBar.setOnExtendTipListener(this);
        mZoomBar.setOnProgressStateChangeListener(this);
        mExtendTipView = (TextView) findViewById(R.id.current_zoom_value);
        Log.d(TAG, "init " + mZoomBar);
    }

    @Override
    public void initZoomLevel(float minZoomValue, float maxZoomValue, float curZoomValue) {
        mZoomBar.setInitValue(minZoomValue,maxZoomValue,curZoomValue);
    }

    @Override
    public void updateZoomLevel(float minZoomValue, float maxZoomValue, float curZoomValue) {
        mZoomBar.updateZoomValue(minZoomValue,maxZoomValue,curZoomValue);
    }

    @Override
    public void initZoomLevel(float[] zoomSection, float curZoomValue){
        mZoomBar.setInitValue(zoomSection,curZoomValue);
    }

    @Override
    public void updateZoomLevel(float[] zoomSection, float curZoomValue){
        mZoomBar.updateZoomValue(zoomSection,curZoomValue);
    }

    @Override
    public float getZoomRatioFromProgess(int progress){
        return mZoomBar.getRatioFromProgess(progress);
    }

    @Override
    public void updateZoomValueText(float curZoomValue) {
        mZoomBar.setCurrentValue(curZoomValue);
    }

    public void setOnProgressChangeListener(OnSeekBarChangeListener listener) {
        mZoomBar.setOnSeekBarChangeListener(listener);
    }

    @Override
    public void setVisibility(int visibility) {
        if (!CameraUtil.isZoomPanelEnabled()
                /*|| (mActivity != null && mActivity.getCurrentModule() != null && mActivity.getCurrentModule().isAudioRecording())*/) { // bugfix 1019160
            super.setVisibility(View.GONE);
        } else {
            super.setVisibility(visibility);
        }
    }

    @Override
    public void setZoomSimple(boolean isSimple){
        mZoomBar.setSimple(isSimple);
    }

    @Override
    public boolean isZoomSimple(){
        return mZoomBar.isSimple();
    }

    @Override
    public void setDetector(boolean isDetector){
        mZoomBar.setDetector(isDetector);
    }

    @Override
    public void showTip() {
        if (mExtendTipView != null && mExtendTipView.getVisibility() != View.VISIBLE)
            mExtendTipView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideTip() {
        if (mExtendTipView != null && mExtendTipView.getVisibility() != View.INVISIBLE)
            mExtendTipView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateTipContent(String tipContent) {
        if (mExtendTipView != null && mExtendTipView.getVisibility() != View.GONE)
            mExtendTipView.setText(tipContent);
    }

    @Override
    public void onProgressVisibleChange(boolean visible){
        if(visible){
            mActivity.getCameraAppUI().updateExtendPanelUI(View.GONE);
            // SPRD: nj dream camera test debug 120
            mActivity.getCameraAppUI().updateFilterPanelUI(View.GONE);
        } else {
            mActivity.getCameraAppUI().updateExtendPanelUI(View.VISIBLE);
            // SPRD: nj dream camera test debug 120
            mActivity.getCameraAppUI().updateFilterPanelUI(View.VISIBLE);
        }
    }
}
