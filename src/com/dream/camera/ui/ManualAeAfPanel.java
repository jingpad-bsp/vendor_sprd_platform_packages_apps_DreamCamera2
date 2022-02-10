
package com.dream.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.GestureDetector;
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
import com.android.camera2.R;
import static java.lang.Math.abs;

public class ManualAeAfPanel extends FrameLayout /*implements View.OnTouchListener*/{
    private ImageView mManualAFCircle;
    private ImageView mManualAFcenterCircle;
    private ImageView mManualAECircle;
    private ImageView mManualAEcenterCircle;
    private ImageView mManualAEAFCircle;
    private FrameLayout mManualAFLayout;
    private FrameLayout mManualAELayout;

    private Context mContext;
    private Paint mWhitePaint;
    private Paint mYellowPaint;
    private final float mDefaultRadiusPx;
    private float mAFTranslationX;
    private float mAFTranslationY;
    private float mAETranslationX;
    private float mAETranslationY;
    private float mCenterCircleTranslationX;
    private float mCenterCircleTranslationY;
    private RectF mAEAFRect;
    private RectF mAFRect;
    private RectF mAERect;

    private GestureDetector mGestureDetector = null;

    private boolean mActive = false;
    private boolean mHasMoveFocus = false;
    private boolean mHasMoveAEFocus = false;
    private Listener mListener;
    private long mDelayAECallUntilMillis = 0;
    private static final long AE_MINI_WAIT_MILLIS = 33;

    private static final Log.Tag TAG = new Log.Tag("ManualAeAfPanel");

    public interface Listener {
        public void manualautoFocus(int x, int y);
        public void manualAEMetering(int x, int y);
    }

    public ManualAeAfPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        Resources res = getResources();
        mWhitePaint = makePaint(res, R.color.focus_color);
        mYellowPaint = makePaint(res, R.color.focus_success_color);
        mDefaultRadiusPx = res.getDimensionPixelSize(R.dimen.focus_circle_max_size);
        setWillNotDraw(false);
    }

    private Paint makePaint(Resources res, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(res.getColor(color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(res.getDimension(R.dimen.focus_circle_stroke));
        return paint;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mManualAFLayout = (FrameLayout)findViewById(R.id.manual_af);
        mManualAELayout = (FrameLayout)findViewById(R.id.manual_ae);
        mManualAFCircle = (ImageView)findViewById(R.id.manual_af_circle);
        mManualAEAFCircle = (ImageView)findViewById(R.id.manual_af_ae_circle);
    }

    @Override
    protected void onDetachedFromWindow(){
        super.onDetachedFromWindow();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw mAEAFRect = " + mAEAFRect);
        super.onDraw(canvas);
        if (mAEAFRect != null && mActive && !mHasMoveFocus) {
            canvas.drawArc(mAEAFRect, 272, 176, false, mWhitePaint);
            canvas.drawArc(mAEAFRect, 92, 176, false, mYellowPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        float x = event.getX();
        float y = event.getY();
        //Log.i(TAG, "onTouch " + " getAction :" + event.getAction()  + " x = " + x + " y = " + y + " mAFRect : " + mAFRect + " mAERect : " + mAERect);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return isInTouchFocus(x, y);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mAFRect != null && mAFRect.contains(x, y) && mListener != null && !mHasMoveAEFocus) {
                    mListener.manualautoFocus((int)x, (int)y);
                }
                if (mHasMoveAEFocus) {
                    mHasMoveAEFocus = false;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mHasMoveFocus && mAEAFRect != null && mAEAFRect.contains(x, y)) {
                    handleFirstTouchEvent();
                } else if (mHasMoveFocus) {
                    if (mAFRect == null && mAEAFRect.contains(x, y) || mAFRect != null && mAFRect.contains(x, y) && !mHasMoveAEFocus) {
                        setAFTranslation(x, y);
                    } else if (mAERect == null && mAEAFRect.contains(x, y) || mAERect != null && mAERect.contains(x, y)) {
                        setAETranslation(x, y);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private boolean isInTouchFocus(float x, float y) {
        if (mHasMoveFocus && (!(mAEAFRect != null && mAEAFRect.contains(x, y)
                                || mAFRect != null && mAFRect.contains(x, y)
                                || mAERect != null && mAERect.contains(x, y)))
            || !mHasMoveFocus && (mAEAFRect == null || !mAEAFRect.contains(x, y))) {
            Log.i(TAG, " isInTouchFocus return false");
            return false;
        } else {
            return true;
        }
    }

    private void handleFirstTouchEvent() {
        mHasMoveFocus = true;
        setManualAEAFCircleVisibility(View.GONE);
        setManualAELayoutVisibility(View.VISIBLE);
        setManualAFLayoutVisibility(View.VISIBLE);
        onStartTrackingTouch();
    }

    private void setAFTranslation(float x, float y) {
        RectF rectPreview = null;

        if (getContext() != null && ((CameraActivity) getContext()).getCameraAppUI() != null) {
            rectPreview = ((CameraActivity) getContext()).getCameraAppUI().getPreviewArea();
        }
        if (rectPreview != null) {
            float tempy = y + rectPreview.top;
            if (!rectPreview.contains(x, tempy)) return;
            mAFTranslationX = x - mDefaultRadiusPx / 2;
            mAFTranslationY = y - mDefaultRadiusPx / 2;
            mManualAFLayout.setTranslationX(mAFTranslationX);
            mManualAFLayout.setTranslationY(mAFTranslationY);
            mAFRect = new RectF(mAFTranslationX, mAFTranslationY, x + mDefaultRadiusPx / 2, y + mDefaultRadiusPx / 2);
            Log.i(TAG, " mAFTranslationX = " + mAFTranslationX + " mAFTranslationY = " + mAFTranslationY + " x = " + x + " y = " + y);
            invalidate();
        }
    }

    private void setAETranslation(float x, float y) {
        RectF rectPreview = null;
        mHasMoveAEFocus = true;

        if (getContext() != null && ((CameraActivity) getContext()).getCameraAppUI() != null) {
            rectPreview = ((CameraActivity) getContext()).getCameraAppUI().getPreviewArea();
        }
        if (rectPreview != null) {
            float tempy = y + rectPreview.top;
            if (!rectPreview.contains(x, tempy)) return;
            long now = SystemClock.uptimeMillis();
            if (now > mDelayAECallUntilMillis) {
                if (mListener != null) {
                    mListener.manualAEMetering((int)x, (int)y);
                }
                mDelayAECallUntilMillis = now + AE_MINI_WAIT_MILLIS;
            }
            mAETranslationX = x - mDefaultRadiusPx / 2;
            mAETranslationY = y - mDefaultRadiusPx / 2;
            mManualAELayout.setTranslationX(mAETranslationX);
            mManualAELayout.setTranslationY(mAETranslationY);
            mAEAFRect = mAERect = new RectF(mAETranslationX, mAETranslationY, x + mDefaultRadiusPx / 2, y + mDefaultRadiusPx / 2);
            Log.i(TAG, " mAETranslationX = " + mAETranslationX + " mAETranslationY = " + mAETranslationY + " x = " + x + " y = " + y);
            invalidate();
        }
    }

    public void setTranslation(float x, float y) {
        if (mManualAFCircle == null) {
            return;
        }

        int centerWidth = mManualAEAFCircle.getWidth();
        int centerHeight = mManualAEAFCircle.getHeight();
        Log.i(TAG, "setTranslation x = " + x + " y = " + y + " centerWidth = " + centerWidth + " centerHeight= " + centerHeight + " mDefaultRadiusPx = " + mDefaultRadiusPx);

        RectF rectPreview = null;

        if (getContext() != null && ((CameraActivity) getContext()).getCameraAppUI() != null) {
            rectPreview = ((CameraActivity) getContext()).getCameraAppUI().getPreviewArea();
        }

        if (rectPreview != null) {
            y = y - rectPreview.top;

            mAFTranslationX = mAETranslationX = x - mDefaultRadiusPx / 2;
            mAFTranslationY = mAETranslationY = y - mDefaultRadiusPx / 2;
            mCenterCircleTranslationX = x - centerWidth / 2;
            mCenterCircleTranslationY = y - centerHeight / 2;
            mManualAFLayout.setTranslationX(mAFTranslationX);
            mManualAELayout.setTranslationX(mAETranslationX);
            mManualAEAFCircle.setTranslationX(mCenterCircleTranslationX);

            mManualAFLayout.setTranslationY(mAFTranslationY);
            mManualAELayout.setTranslationY(mAETranslationY);
            mManualAEAFCircle.setTranslationY(mCenterCircleTranslationY);

            Log.i(TAG, " mCenterCircleTranslationX = " + mCenterCircleTranslationX + " mCenterCircleTranslationY = " + mCenterCircleTranslationY + " rectPreview.top = " + rectPreview.top);
            mAEAFRect = new RectF(mAFTranslationX, mAFTranslationY, x + mDefaultRadiusPx / 2, y + mDefaultRadiusPx / 2);
        }
    }

    public void updateAEAFLayout(Rect previewRect) {
        Log.i(TAG, "updateAEAFLayout");
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = previewRect.width();
        params.height = previewRect.height();
        params.setMargins(previewRect.left, previewRect.top, 0, 0);
        setLayoutParams(params);
    }

    private void setManualAFLayoutVisibility(int visibility) {
        if (mManualAELayout != null) {
            mManualAFLayout.setVisibility(visibility);
        }
    }

    private void setManualAELayoutVisibility(int visibility) {
        if (mManualAELayout != null) {
            mManualAELayout.setVisibility(visibility);
        }
    }

    private void setManualAEAFCircleVisibility(int visibility) {
        if (mManualAEAFCircle != null) {
            mManualAEAFCircle.setVisibility(visibility);
        }
    }

    public void showAEAFFocus() {
        Log.i(TAG, "showAEAFFocus mActive = " + mActive);
        if (mActive) {
            setManualAEAFCircleVisibility(View.VISIBLE);
            setVisibility(View.VISIBLE);
        }
    }

    public void setActive(boolean active) {
        mActive = active;
        if (!mActive) {
            hideAEAFFocus();
        }
    }

    public void hideAEAFFocus() {
        Log.i(TAG, "hideAEAFFocus");
        mHasMoveFocus = false;
        mHasMoveAEFocus = false;
        mAEAFRect = null;
        mAFRect = null;
        mAERect = null;
        setVisibility(View.GONE);
        setManualAELayoutVisibility(View.GONE);
        setManualAFLayoutVisibility(View.GONE);
    }

    public void setManualListener(Listener listener) {
        mListener = listener;
    }

    public void setGestureListener(GestureDetector.OnGestureListener gestureListener) {
        if (gestureListener != null) {
            mGestureDetector = new GestureDetector(getContext(), gestureListener);
        }
    }

    public boolean isAEAFDraging() {
        return mHasMoveFocus;
    }

    public boolean isCameraIdle(){
        CameraModule mCurrentModule = ((CameraActivity) mContext).getCurrentModule();
        if(mCurrentModule instanceof PhotoModule){
            return ((PhotoModule) mCurrentModule).getCameraState() == PhotoModule.IDLE;
        }
        if(mCurrentModule instanceof VideoModule){
            return ((VideoModule) mCurrentModule).isCameraIdle();
        }
        return false;
    }

    private void onStartTrackingTouch() {
        CameraModule mCurrentModule = ((CameraActivity) mContext).getCurrentModule();
        if(mCurrentModule instanceof PhotoModule){
            ((PhotoModule) mCurrentModule).onStartTrackingTouch(null);
        }
    }
}
