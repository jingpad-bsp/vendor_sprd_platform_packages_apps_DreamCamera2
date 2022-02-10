
package com.dream.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Rect;

import com.android.camera.CameraActivity;
import com.android.camera.CameraModule;
import com.android.camera.PhotoModule;
import com.android.camera.VideoModule;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import static java.lang.Math.abs;

public class AELockPanel extends FrameLayout {
    private static final int RESET_SEEKBAR_ALPHA = 0;
    private static final int DELAY_TIME = 3000;
    private boolean active = false;
    private AELockSeekBar verticalSeekBar;
    private ImageView adjustCircle;
    private ImageView centerCircle;
    private TextView tipText;
    private int progress = 0;
    private boolean SeekbarRectTouchDown = false;

    private HandlerThread myHandlerThread ;
    private Handler handler ;
    private Context mContext;

    private static final Log.Tag TAG = new Log.Tag("AELockPanel");

    public AELockPanel(Context context) {
        super(context);
        mContext = context;
        initHandler();
    }

    public AELockPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initHandler();
    }

    public AELockPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initHandler();
    }

    public void initHandler(){
        myHandlerThread = new HandlerThread( "handler-thread") ;
        myHandlerThread.start();
        handler = new Handler( myHandlerThread.getLooper() ){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d( TAG, "MSG:" + msg.what + "thread:" + Thread.currentThread().getName()  ) ;
                switch (msg.what){
                    case RESET_SEEKBAR_ALPHA: // hide seekbar`s progressDrawable
                        verticalSeekBar.setProgressDrawableAlpha(0);
                        break;
                }
            }
        };
    }

    @Override
    protected void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        if (myHandlerThread != null) {
            myHandlerThread.quitSafely();
        }
    }

    public void initLisener(){
        setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v,MotionEvent event){
                Rect seekRect = new Rect();
                if(verticalSeekBar == null){
                    verticalSeekBar = (AELockSeekBar) getRootView().findViewById(R.id.aeseekbar);
                }
                if (verticalSeekBar.getVisibility() != View.VISIBLE) {//SPRD:fix bug946451
                    return false;
                }
                verticalSeekBar.getHitRect(seekRect);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //seekbar add rect :x -100 ~ x + 100;
                        if (event.getY() >= verticalSeekBar.getTranslationY() &&
                                event.getY() <= (verticalSeekBar.getTranslationY() + verticalSeekBar.getHeight()) &&
                                event.getX() >= verticalSeekBar.getTranslationX() - 100 &&
                                event.getX() <= verticalSeekBar.getTranslationX() + 100) {
                            Log.i(TAG, "ACTION_DOWN seekbar rect:" + event.getY() + " " + verticalSeekBar.getTranslationY());
                            progress = (int)event.getY();
                            SeekbarRectTouchDown = true;
                            verticalSeekBar.setProgressDrawableAlpha(255); //show progress
                            onStartTrackingTouch();
                            return true;
                        } else { //don`t in seekbar Rect so return
                            if(SeekbarRectTouchDown) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                        //break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL://SPRD:fix bug1192887
                        if(SeekbarRectTouchDown){
                            SeekbarRectTouchDown = false;
                            onStopTrackingTouch();
                            handler.sendEmptyMessageDelayed(RESET_SEEKBAR_ALPHA,DELAY_TIME);
                            return true;
                        }else {
                            return false;
                        }
                        //break;
                    case MotionEvent.ACTION_MOVE:
                        if(SeekbarRectTouchDown && handler.hasMessages(RESET_SEEKBAR_ALPHA)){
                            handler.removeMessages(RESET_SEEKBAR_ALPHA);
                        }
                        if(abs(event.getY() - progress) > verticalSeekBar.getScrollFriction() &&
                                isCameraIdle()){
                            verticalSeekBar.setInCrease((progress - (int)event.getY()) / verticalSeekBar.getScrollFriction());
                            progress = (int)event.getY();
                        }
                        return true;
                        //break;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if(verticalSeekBar == null){
            verticalSeekBar = (AELockSeekBar) getRootView().findViewById(R.id.aeseekbar);
        }
        if (visibility == View.VISIBLE) {
            //resetHideTime();
            initLisener();
            verticalSeekBar.setProgressDrawableAlpha(0);
        }
        verticalSeekBar.setVisibility(View.INVISIBLE);
    }

    public void show() {
        if (active
                && !((CameraActivity) getContext()).getCameraAppUI().isModeListOpen()
                && !((CameraActivity) getContext()).getCameraAppUI().isSettingLayoutOpen()) {
            setVisibility(View.VISIBLE);
        }
    }

    public void changeColor(boolean isFocused) {
        if (verticalSeekBar == null) {
            verticalSeekBar = (AELockSeekBar) getRootView().findViewById(R.id.aeseekbar);
        }

        if (adjustCircle == null) {
            adjustCircle = (ImageView) getRootView().findViewById(R.id.af_circle);
        }

        if(centerCircle == null) {
            centerCircle = (ImageView) getRootView().findViewById(R.id.center_circle);
        }

        if (verticalSeekBar == null || adjustCircle == null || centerCircle == null) {
            return;
        }
        if(isFocused) {
            centerCircle.setImageDrawable(getResources().getDrawable(R.drawable.lock_center_circle_focused_shape));
            adjustCircle.setImageDrawable(getResources().getDrawable(R.drawable.lock_circle_focused_shape));
        } else {
            centerCircle.setImageDrawable(getResources().getDrawable(R.drawable.lock_center_circle_shape));
            adjustCircle.setImageDrawable(getResources().getDrawable(R.drawable.lock_circle_shape));
        }
    }

    public void showtipText(boolean visible){
        if(tipText == null){
            tipText = (TextView)getRootView().findViewById(R.id.lock_ae_tip);
        }
        Log.d(TAG,"show tip text :" + visible);
        if(visible){
            tipText.setText(R.string.lock_ae_and_af);
            tipText.setVisibility(View.VISIBLE);
        } else {
            tipText.setVisibility(View.GONE);
        }
    }

    public void setSeekBarVisibility(boolean visible) {
        if (verticalSeekBar == null) {
            verticalSeekBar = (AELockSeekBar) getRootView().findViewById(R.id.aeseekbar);
        }
        if (visible) {
            verticalSeekBar.setVisibility(View.VISIBLE);//SPRD:fix bug946451
        } else {
            verticalSeekBar.setVisibility(View.GONE);
        }
    }

    public void hide() {
        showtipText(false);
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
            verticalSeekBar = (AELockSeekBar) getRootView().findViewById(R.id.aeseekbar);
        }

        if (adjustCircle == null) {
            adjustCircle = (ImageView) getRootView().findViewById(R.id.af_circle);
        }

        if(centerCircle == null) {
            centerCircle = (ImageView) getRootView().findViewById(R.id.center_circle);
        }

        if (verticalSeekBar == null || adjustCircle == null || centerCircle == null) {
            return;
        }
        changeColor(false);

        int vWidth = verticalSeekBar.getWidth();
        int vHeight = verticalSeekBar.getHeight();

        int cWidth = adjustCircle.getWidth();
        int cHeight = adjustCircle.getHeight();

        int sWidth = centerCircle.getWidth();
        int sHeight = centerCircle.getHeight();

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
            float sX = 0;
            float sY = 0;

            if (x + cWidth / 2 + vWidth > rectPreview.right) {
                vX = x - cWidth / 2 - vWidth;
                cX = x - cWidth / 2;
                sX = x - sWidth / 2;
            } else {
                vX = x + cWidth / 2;
                cX = x - cWidth / 2;
                sX = x - sWidth / 2;
            }

            if (y - vHeight / 2 < adjustPanelTop - rectPreview.top) {
                vY = adjustPanelTop - rectPreview.top;
                cY = y - cHeight / 2;
                sY = y - sHeight / 2;
            } else if (y + vHeight / 2 > Math.min(adjustPanelBottom - rectPreview.top, rectPreview.bottom - rectPreview.top)) {
                vY = Math.min(adjustPanelBottom - rectPreview.top, rectPreview.bottom - rectPreview.top) - vHeight;
                cY = y - cHeight / 2;
                sY = y - sHeight / 2;
            } else {
                vY = y - vHeight / 2;
                cY = y - cHeight / 2;
                sY = y - sHeight / 2;
            }

            verticalSeekBar.setTranslationX(vX);
            adjustCircle.setTranslationX(cX);
            centerCircle.setTranslationX(sX);

            verticalSeekBar.setTranslationY(vY);
            adjustCircle.setTranslationY(cY);
            centerCircle.setTranslationY(sY);

            verticalSeekBar.setProgress(0);//set Progerss to original
        }
    }


    /**
     * SPRD:fix bug1167059 focusring not occureed
     * @
    **/
    public void updateAELockLayout(Rect previewRect) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = Math.round(previewRect.width());
        params.height = Math.round(previewRect.height());
        params.setMargins(previewRect.left, previewRect.top, 0, 0);
        setLayoutParams(params);
    }
    /* @} */

    public boolean isSeekbarChanging(){
        return SeekbarRectTouchDown;
    }

    public boolean isCameraIdle(){
        CameraModule mCurrentModule = ((CameraActivity) mContext).getCurrentModule();
        if(mCurrentModule instanceof PhotoModule){
            return ((PhotoModule) mCurrentModule).getCameraState() == PhotoModule.IDLE;
        }
        if(mCurrentModule instanceof VideoModule){
            return true;
        }
        return false;
    }

    private void onStartTrackingTouch() {
        CameraModule mCurrentModule = ((CameraActivity) mContext).getCurrentModule();
        if(mCurrentModule instanceof PhotoModule){
            ((PhotoModule) mCurrentModule).onStartTrackingTouch(null);
        }
        if(mCurrentModule instanceof VideoModule){
            ((VideoModule) mCurrentModule).onStartTrackingTouch(null);
        }
    }

    private void onStopTrackingTouch() {
        CameraModule mCurrentModule = ((CameraActivity) mContext).getCurrentModule();
        if(mCurrentModule instanceof PhotoModule){
            ((PhotoModule) mCurrentModule).onStopTrackingTouch(null);
        }
        if(mCurrentModule instanceof VideoModule){
            ((VideoModule) mCurrentModule).onStopTrackingTouch(null);
        }
    }
}
