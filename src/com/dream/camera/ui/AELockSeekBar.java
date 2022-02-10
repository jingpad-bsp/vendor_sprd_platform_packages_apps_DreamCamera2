
package com.dream.camera.ui;

import com.dream.camera.ui.AdjustPanel;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.os.Handler;
import android.os.Message;
import com.android.camera2.R;

public class AELockSeekBar extends SeekBar {
    private AdjustPanel adjustPanel;
    private int scrollFriction = 40;//default value

    public AELockSeekBar(Context context) {
        super(context);
        getProgressDrawable().setAlpha(0);
    }

    public AELockSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        getProgressDrawable().setAlpha(0);
    }

    public AELockSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getProgressDrawable().setAlpha(0);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas canvas) {
        canvas.rotate(-90);
        canvas.translate(-getHeight(), 0);
        //getProgressDrawable().setAlpha(0);
        super.onDraw(canvas);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    public void setInCrease(int increase){
        setProgress(getProgress() + increase);
    }

    public void setProgressDrawableAlpha(int alpha){
        getProgressDrawable().setAlpha(alpha);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (adjustPanel == null) {
            adjustPanel = (AdjustPanel) getRootView().findViewById(R.id.adjust_panel);
        }

        if (adjustPanel != null && adjustPanel.getVisibility() == View.VISIBLE) {
            adjustPanel.resetHideTime();
        }

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return false;
    }

    public int getScrollFriction(){return scrollFriction;}
    public void setScrollFriction(int value){
        scrollFriction = value;
    }

}
