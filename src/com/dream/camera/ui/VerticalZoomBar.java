package com.dream.camera.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

import com.android.camera.ui.Rotatable;
import com.android.camera2.R;

import java.math.BigDecimal;
import java.util.ArrayList;

public class VerticalZoomBar extends View implements Rotatable {

    private final String TAG = this.getClass().getSimpleName();
    private final boolean DEBUG = false;

    private int COLOR_ALPHA_BASE = 255;
    private int BACK_CIRCLE_ALPHA = 51;
    private int PROGRESS_BAR_ALPHA_BASE = 204;
    private int LITTLE_TIP_CIRCLE_ALPHA_BASE = 126;
    private int LITTLE_TIP_CIRCLE_FRONT_ALPHA_BASE = 77;

    private int mCenterTipCircleradius = 0;//(int) (mScaleTextSize * 1.6f);
    private int mCircleSpace = 0;
    private String mScaleUnit = "X";

    private Paint mScaleTextPaint;//paint for min and max scale
    private Paint mTipTextPaint;//paint for tip text
    private Paint mCircleTipTextPaint;//paint for tip text

    private boolean isPressed = false;
    private boolean isLongPressed = false;
    private SEEK_BAR_STATE showState = SEEK_BAR_STATE.DISPEAR;
    private int mColorAlphaPercent = 0;

    private float mDownY;

    private int TIP_CALCULATE_SCALE = 4;
    private int TIP_SHOW_SCALE = 1;

    //SW+W+T UI element
    Bitmap mProgressIndicator;
    private ZOOM_TYPE mZoomType = ZOOM_TYPE.TYPE_SINGLE;
    private int mStartProgressX = 0;
    private int mStartProgressY = 0;
    private int mProgressHeight = 176;//progress bar hight
    private int mProgressWidth = 2;//progress bar width
    private int mProgressTwoPartRadio;//first part is dot line，second part is stock line
    private int mProgressDottedItemSpace = 3;//dot line part，dot unit space 3dp
    private int mProgressDottedItemHeight = 1;//dot line part，dot unit height 1dp
    private int mScaleTextSize = 30;//size of tip text in center circle
    private int mProgressTipTextSize = 20;// size of text on the side of progress bar
    private int mCenterLittleCircleradius = 3;//radius of little cricle
    private int mLightColor = Color.WHITE;//light color
    private int mDarkColor = Color.BLACK;//dart color
    private int mBackGroundColorAlpha = BACK_CIRCLE_ALPHA;//percent of alpha

    private Paint mBackGroundPaint;
    private Paint mProgressPaint;
    private Paint mProgressTipPaint;
    private Paint mProgressIndicatePaint;
    private Paint mCenterTipCirclePaint;//paint for cycle
    private Paint mCenterTipBgCirclePaint;//paint for cycle
    private Paint mLittleCirclePaint;
    private Paint mLittleCircleBgPaint;

    private float mScaleMin = 1;
    private float mScaleMax = 10;

    private float mScaleTextTop = 0;
    private float mScaleTextBottom = 0;


    private float mTipTextTop = 0;
    private float mTipTextBottom = 0;

    private float[] mScaleSection;

    private ArrayList<PartialUnit> partialUnits;

    private int mWidth;
    private int mHeight;
    private float diffHeight;
    private int mProgress;//0-mProgressMax
    private int mProgressMax = 10000;

    private boolean mDrawTipCircle = false;
    private boolean mDrawBackgroundProgress = false;
    private boolean mIsDetector = true;
    private SeekBar.OnSeekBarChangeListener mSeekChangeistener = null;
    private OnExtendTipListener onExtendTipListener = null;
    private OnProgressStateChangeListener onProgressStateChangeListener = null;

    private Runnable showAnimaRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPressed = true;
            startShowAnimationMotion();
        }
    };

    private Runnable dispearAnimaRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPressed = false;
            startDispearAnimationMotion();
        }
    };

    public VerticalZoomBar(Context context) {
        super(context);
        initPaint();
        initPara();
    }

    public VerticalZoomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        initPaint();
        initPara();

    }

    public VerticalZoomBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        initPaint();
        initPara();

    }

    public VerticalZoomBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context, attrs);
        initPaint();
        initPara();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG," getTipValue = " +getTipValue(TIP_CALCULATE_SCALE));
        calculateCurrentOrientationDegree();
//        drawBG(canvas);
        if (mDrawBackgroundProgress) {
            drawProgressBar(canvas);
            drawProgressBarTip(canvas);
            drawIndicator(canvas);
        }
        if (mDrawTipCircle) {
            if (mZoomType != ZOOM_TYPE.TYPE_SINGLE)
                drawLittleCircle(canvas);
            drawTipCircle(canvas, getWidth() / 2, getHeight() / 2);
        }
    }

    private void initPara() {
        mCenterTipCircleradius = (int) (mScaleTextSize * 1.6f);
        partialUnits = new ArrayList<PartialUnit>();
        setmZoomType(ZOOM_TYPE.TYPE_SINGLE);
        resetPartialUnit();
        showState = SEEK_BAR_STATE.DISPEAR;
    }

    private void resetPartialUnit() {
        if (partialUnits == null)
            return;

        partialUnits.clear();

        if (mZoomType == ZOOM_TYPE.TYPE_SINGLE || mScaleSection == null) {
            PartialUnit unit = new PartialUnit();
            unit.tipMax = mScaleMax;
            unit.tipMin = mScaleMin;
            unit.height = 0;
            unit.height_weight = 100;
            unit.progressType = LINE_TYPE.TYPE_ACTIVE;
            partialUnits.add(unit);
        } else {
            for (int i = 0;i < mScaleSection.length -1;i++) {
                if (mScaleSection[i] == 0 || mScaleSection[i+1]==0)
                    continue;

                PartialUnit unit = new PartialUnit();
                unit.tipMax = mScaleSection[i+1];
                unit.tipMin = mScaleSection[i];
                unit.height = 0;
                unit.circle_center_y = 0;
                if (unit.tipMax == mScaleMax) {
                    unit.height_weight = mProgressTwoPartRadio;
                    unit.progressType = LINE_TYPE.TYPE_DOTTED;
                } else {
                    unit.height_weight = 100;
                    unit.progressType = LINE_TYPE.TYPE_ACTIVE;
                }
                partialUnits.add(0,unit);
            }
        }

        float allweight = 0;

        for (PartialUnit tmp : partialUnits) {
            allweight += tmp.height_weight;
        }

        for (PartialUnit tmp : partialUnits) {
            tmp.height_weight = (int) (tmp.height_weight * mProgressMax / allweight + 0.5);
            tmp.height = (int) (mProgressHeight * tmp.height_weight / mProgressMax + 0.5);
        }
    }

    private void initPaint() {
        mLittleCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLittleCirclePaint.setStyle(Paint.Style.STROKE);
        mLittleCirclePaint.setColor(mDarkColor);
        mLittleCirclePaint.setAlpha(LITTLE_TIP_CIRCLE_FRONT_ALPHA_BASE);
        mLittleCirclePaint.setStrokeWidth(1);

        mLittleCircleBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLittleCircleBgPaint.setStyle(Paint.Style.FILL);
        mLittleCircleBgPaint.setColor(mLightColor);
        mLittleCircleBgPaint.setAlpha(LITTLE_TIP_CIRCLE_ALPHA_BASE);
        mLittleCircleBgPaint.setStrokeWidth(1);

        mCenterTipCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterTipCirclePaint.setStyle(Paint.Style.STROKE);
        mCenterTipCirclePaint.setColor(mLightColor);
        mCenterTipCirclePaint.setAlpha(COLOR_ALPHA_BASE);
        mCenterTipCirclePaint.setStrokeWidth(3);

        mCenterTipBgCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterTipBgCirclePaint.setStyle(Paint.Style.FILL);
        mCenterTipBgCirclePaint.setColor(mDarkColor);
        mCenterTipBgCirclePaint.setAlpha(BACK_CIRCLE_ALPHA);
        mCenterTipBgCirclePaint.setStrokeWidth(1);

        mScaleTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mScaleTextPaint.setTextSize(mScaleTextSize);
        mScaleTextPaint.setColor(mLightColor);
        mScaleTextPaint.setTypeface(Typeface.SANS_SERIF);
        mScaleTextPaint.setFakeBoldText(true);
        mScaleTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = mScaleTextPaint.getFontMetrics();
        mScaleTextTop = fm.top;
        mScaleTextBottom = fm.bottom;

        mTipTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTipTextPaint.setTextSize(mProgressTipTextSize);
        mTipTextPaint.setColor(mLightColor);
        mTipTextPaint.setTypeface(Typeface.SANS_SERIF);
        mTipTextPaint.setFakeBoldText(true);
        mTipTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics tipfm = mTipTextPaint.getFontMetrics();
        mTipTextTop = tipfm.top;
        mTipTextBottom = tipfm.bottom;

        mCircleTipTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mCircleTipTextPaint.setTextSize(mScaleTextSize);
        mCircleTipTextPaint.setColor(mLightColor);
        mCircleTipTextPaint.setTypeface(Typeface.SANS_SERIF);
        mCircleTipTextPaint.setFakeBoldText(true);
        mCircleTipTextPaint.setTextAlign(Paint.Align.CENTER);

        mBackGroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackGroundPaint.setColor(Color.LTGRAY);
        mBackGroundPaint.setStyle(Paint.Style.FILL);
        mBackGroundPaint.setAlpha(BACK_CIRCLE_ALPHA);
        mBackGroundPaint.setStrokeWidth(1);


        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setColor(Color.WHITE);
        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setStrokeWidth(mProgressWidth);
        mProgressPaint.setAlpha(PROGRESS_BAR_ALPHA_BASE);

        mProgressTipPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mProgressTipPaint.setTextSize(mScaleTextSize);
        mProgressTipPaint.setColor(mLightColor);
        mProgressTipPaint.setTypeface(Typeface.SANS_SERIF);
        mProgressTipPaint.setFakeBoldText(true);
        mProgressTipPaint.setTextAlign(Paint.Align.CENTER);
        mProgressIndicator = getBitmapFromVectorDrawable(this, R.drawable.ic_zoom_slide_control_sprd);

        mProgressIndicatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressIndicatePaint.setStyle(Paint.Style.FILL);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VertialZoomBar);
            mScaleMin = typedArray.getFloat(R.styleable.VertialZoomBar_vertial_zoombar_scale_min, mScaleMin);
            mScaleMax = typedArray.getFloat(R.styleable.VertialZoomBar_vertial_zoombar_scale_max, mScaleMax);
            mScaleTextSize = typedArray.getDimensionPixelSize(R.styleable.VertialZoomBar_vertial_zoombar_scale_text_size, mScaleTextSize);
            mProgressTipTextSize = typedArray.getDimensionPixelSize(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_bg_text_size, mProgressTipTextSize);
            mBackGroundColorAlpha = typedArray.getInt(R.styleable.VertialZoomBar_vertial_zoombar_background_color_alpha, mBackGroundColorAlpha);
            mCenterTipCircleradius = typedArray.getDimensionPixelSize(R.styleable.VertialZoomBar_vertial_zoombar_scale_center_circle_radius, mCenterTipCircleradius);
            mCenterLittleCircleradius = typedArray.getDimensionPixelSize(R.styleable.VertialZoomBar_vertial_zoombar_scale_little_circle_radius, mCenterLittleCircleradius);
            mProgressTwoPartRadio = typedArray.getInt(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_bg_two_part_radio, mProgressTwoPartRadio);
            mProgressHeight = typedArray.getDimensionPixelOffset(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_bg_height, mProgressHeight);
            mProgressWidth = typedArray.getDimensionPixelOffset(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_bg_width, mProgressWidth);
            mProgressDottedItemSpace = typedArray.getDimensionPixelOffset(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_dotted_item_space, mProgressDottedItemSpace);
            mProgressDottedItemHeight = typedArray.getDimensionPixelOffset(R.styleable.VertialZoomBar_vertial_zoombar_progress_bar_dotted_item_height, mProgressDottedItemHeight);
            mCircleSpace = typedArray.getDimensionPixelOffset(R.styleable.VertialZoomBar_vertial_zoombar_circle_space,mCircleSpace);
            typedArray.recycle();
        }
    }

    public void setInitValue(float minValue, float maxValue, float currentValue) {
        Log.d(TAG, "setInitValue  min = " + minValue + " ,max = " + maxValue + ", currentvalue = " + currentValue + "mProgress = " + mProgress);
        if (maxValue < minValue)
            return;
        mScaleMin = minValue;
        mScaleMax = maxValue;
        mScaleSection = null;
        resetPartialUnit();
        transfromTipToInnerProperty(currentValue);
//        calculateCircleTipPaintVar(100);
//        calculateBackgroundPaintVar(100);
        invalidate();
    }

    public void updateZoomValue(float minValue, float maxValue, float currentValue){
        setInitValue(minValue,maxValue,currentValue);
    }

    public void setInitValue(float[] scaleSection, float currentValue){

        if (scaleSection == null) {
            return;
        }
        mScaleSection = scaleSection;

        mScaleMin = mScaleSection[0];
        mScaleMax = mScaleSection[0];

        for (float max : mScaleSection){
            mScaleMax = mScaleMax > max ? mScaleMax : max;
        }

        resetPartialUnit();
        transfromTipToInnerProperty(currentValue);
        invalidate();

        printDetail();
    }

    public void updateZoomValue(float[] scaleSection, float currentValue){
        setInitValue(scaleSection,currentValue);
        showZoomNoSimpleAwhile();
        if (onExtendTipListener != null)
            onExtendTipListener.updateTipContent(getTipOutString(getTipValue(TIP_SHOW_SCALE)));
    }

    public void showZoomNoSimpleAwhile(){
        removeCallbacks(dispearAnimaRunnable);
        if (showState == SEEK_BAR_STATE.DISPEAR || showState == SEEK_BAR_STATE.START_DISPEAR) {
            removeCallbacks(showAnimaRunnable);
            post(showAnimaRunnable);
        }
        postDelayed(dispearAnimaRunnable, 500);
    }

    public void setCurrentValue(float currentValue) {
        Log.d(TAG, "setCurrentValue = " + currentValue);
        transfromTipToInnerProperty(currentValue);
        invalidate();
    }

    private void drawBG(Canvas canvas) {
        canvas.save();
        canvas.drawRect(0, 0, mWidth, mHeight, mBackGroundPaint);
        canvas.restore();
    }


    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        mSeekChangeistener = listener;
    }

    public void setOnExtendTipListener(OnExtendTipListener listener){
        onExtendTipListener = listener;
    }

    public void setOnProgressStateChangeListener(OnProgressStateChangeListener listener){
        onProgressStateChangeListener = listener;
    }

    public void resetState(){
        showState = SEEK_BAR_STATE.DISPEAR;
        setmZoomType(mZoomType);
    }

    private void drawTipCircle(Canvas canvas, float center_x, float center_y) {
        canvas.save();
        canvas.drawCircle(center_x, center_y, mCenterTipCircleradius + 2, mCenterTipBgCirclePaint);
        canvas.drawCircle(center_x, center_y, mCenterTipCircleradius, mCenterTipCirclePaint);
        canvas.rotate(-mCurrentDegree, center_x, center_y);
        canvas.drawText(getTipString(getTipValue(TIP_SHOW_SCALE)), center_x, center_y - (mScaleTextBottom + mScaleTextTop) / 2, mCircleTipTextPaint);
        canvas.rotate(mCurrentDegree, center_x, center_y);
        canvas.restore();
    }

    private void drawLittleTipCircle(Canvas canvas, float center_x, float center_y) {
        canvas.drawCircle(center_x, center_y, mCenterLittleCircleradius, mLittleCircleBgPaint);
        canvas.drawCircle(center_x, center_y, mCenterLittleCircleradius, mLittleCirclePaint);
    }

    private void drawLittleCircle(Canvas canvas) {
        //draw two or three little circle. neighbouring tip circle
        int width = mWidth / 2;
        int height = mHeight / 2;

        //according current tip, calculate the location of the little circle

        float tipvalue = getTipValue(TIP_SHOW_SCALE);
        int tmpHeight = height;

        int middle_index = 0;
        for (PartialUnit unit:partialUnits){
            if (unit.tipMax == tipvalue){
                middle_index--;
                middle_index = middle_index > 0? middle_index:0;
                break;
            }
            if (unit.tipMin <= tipvalue && unit.tipMax > tipvalue) {
                break;
            }
            middle_index++;
        }


        canvas.save();
        int index = 0;
        for (PartialUnit unit:partialUnits) {
            if (index == middle_index) {
                index++;
                unit.circle_center_y = height;
                continue;
            }

            int diff = mCenterTipCircleradius + mCenterLittleCircleradius * (1 + Math.abs(index - middle_index) * 2) + Math.abs(index - middle_index) * mCircleSpace;
            if (index > middle_index) {
                tmpHeight = height + diff;
            } else {
                tmpHeight = height - diff;
            }
            unit.circle_center_y = tmpHeight;
            drawLittleTipCircle(canvas, width, tmpHeight);
            index++;
        }

        canvas.restore();
    }

    private void drawProgressBarActive(Canvas canvas, int top, int bottom) {
        canvas.drawLine(mStartProgressX + mProgressWidth / 2, top, mStartProgressX + mProgressWidth / 2, bottom, mProgressPaint);
    }

    private void drawProgressBarDotted(Canvas canvas, int top, int bottom) {
        int tmpH = top;
        do {
            canvas.drawLine(mStartProgressX + mProgressWidth / 2, tmpH, mStartProgressX + mProgressWidth / 2, tmpH + mProgressDottedItemHeight, mProgressPaint);
            tmpH += mProgressDottedItemHeight + mProgressDottedItemSpace;
        } while (tmpH < bottom);
    }

    private void drawProgressBarTip(Canvas canvas) {
        canvas.save();
        int start = mStartProgressY;
        for (PartialUnit unit : partialUnits) {
            int tmp_start = start;
            int tmp_end = tmp_start + unit.height;

            //draw max x and calculate max x local
            drawTextTip(canvas, mStartProgressX * 11 / 25, tmp_start, getTipString(unit.tipMax));
            //draw min x and calculate min x local
            drawTextTip(canvas, mStartProgressX * 11 / 25, tmp_end, getTipString(unit.tipMin));
            start = tmp_end;
        }
        canvas.restore();
    }

    private void drawTextTip(Canvas canvas, float center_x, float center_y, String text) {
        canvas.rotate(-mCurrentDegree, center_x, center_y);
        canvas.drawText(text, center_x, center_y - (mTipTextBottom + mTipTextTop) / 2, mTipTextPaint);
        canvas.rotate(mCurrentDegree, center_x, center_y);
    }

    private void drawProgressBar(Canvas canvas) {
        canvas.save();
        int start = mStartProgressY;
        for (PartialUnit unit : partialUnits) {
            int tmp_start = start;
            int tmp_end = tmp_start + unit.height;
            if (unit.progressType == LINE_TYPE.TYPE_DOTTED) {
                drawProgressBarDotted(canvas, tmp_start, tmp_end);
            } else {
                drawProgressBarActive(canvas, tmp_start, tmp_end);
            }
            start = tmp_end;
        }
        canvas.restore();
    }

    private void drawIndicator(Canvas canvas) {
        //绘制圆角矩形
        //canvas.drawRoundRect();
        canvas.save();
        canvas.drawBitmap(mProgressIndicator, mStartProgressX - mProgressIndicator.getWidth() / 2 + +mProgressWidth / 2,
                mStartProgressY - mProgressIndicator.getHeight() / 2 + diffHeight, mProgressIndicatePaint);
        canvas.restore();
    }

    public Bitmap getBitmapFromVectorDrawable(View view, int drawableId) {
        Drawable drawable = view.getResources().getDrawable(drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    //end new UI

    private String getTipString(float tipValue) {
        if (mScaleMin < 1.0f && tipValue == mScaleMin) {
            return this.getResources().getString(R.string.super_wide_tip);
        }
        return tipValue + mScaleUnit;
    }

    private String getTipOutString(float tipValue) {
        return tipValue + mScaleUnit;
    }

    private float getTipValue(int scale) {
        float tip = mScaleMax;
        //tip calculate need change

        int calProgress = 0;//for calculate partial unit

        for (PartialUnit unit : partialUnits) {
            if (calProgress <= mProgress) {
                tip -= (unit.tipMax - unit.tipMin) * Math.min(mProgress - calProgress, unit.height_weight) / unit.height_weight;
            } else {
                break;
            }
            calProgress += unit.height_weight;
        }

        //        tip -= (mScaleMax - mScaleMin) * mProgress / 100;
        BigDecimal b = new BigDecimal(tip);
        float tipTmp = b.setScale(scale, BigDecimal.ROUND_HALF_UP).floatValue();
        return tipTmp;
    }


    public float getRatioFromProgess(int pregress) {
        float tip = mScaleMax;
        //tip calculate need change

        int calProgress = 0;//for calculate partial unit

        for (PartialUnit unit : partialUnits) {
            if (calProgress <= pregress) {
                tip -= (unit.tipMax - unit.tipMin) * Math.min(pregress - calProgress, unit.height_weight) / unit.height_weight;
            } else {
                break;
            }
            calProgress += unit.height_weight;
        }

        //        tip -= (mScaleMax - mScaleMin) * mProgress / 100;
        BigDecimal b = new BigDecimal(tip);
        float tipTmp = b.setScale(TIP_CALCULATE_SCALE, BigDecimal.ROUND_HALF_UP).floatValue();
        return tipTmp;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        switch (wSpecMode) {
            case MeasureSpec.EXACTLY:
                mWidth = wSpecSize;
                break;
            case MeasureSpec.AT_MOST:
                mWidth = mCenterTipCircleradius * 2;
                break;
        }
        switch (hSpecMode) {
            case MeasureSpec.EXACTLY:
                mHeight = hSpecSize;
                break;
            case MeasureSpec.AT_MOST:
                mHeight = (int) (mProgressHeight * 2.2);
                break;
        }
        setMeasuredDimension(mWidth, mHeight);

        mStartProgressY = (mHeight - mProgressHeight) / 2;
        mStartProgressX = (mWidth - mProgressWidth) / 2;
        printDetail();
    }

    //动画会有交叠闪烁的现象
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isSimple() && mIsDetector && isTouchVisibleArea(event.getY())) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isPressed = true;
                    isLongPressed = false;
                    handleActionDown(event);
                    removeCallbacks(showAnimaRunnable);
                    removeCallbacks(dispearAnimaRunnable);
                    postDelayed(showAnimaRunnable, 500);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if ((showState == SEEK_BAR_STATE.SHOW)) {
                        if (handleActionMove(event))
                            return true;
                    }
                    if (showState == SEEK_BAR_STATE.DISPEAR) {
                        removeCallbacks(showAnimaRunnable);
                        showAnimaRunnable.run();
                    }
                    if (showState == SEEK_BAR_STATE.DISPEAR || showState == SEEK_BAR_STATE.START_SHOW) {
                        handleActionDown(event);
                    }

                    break;
                case MotionEvent.ACTION_CANCEL:
                    removeCallbacks(showAnimaRunnable);
                    if (showState == SEEK_BAR_STATE.SHOW || showState == SEEK_BAR_STATE.START_SHOW)
                        postDelayed(dispearAnimaRunnable, 500);
                    break;
                case MotionEvent.ACTION_UP:
                    if (isLongPressed == false && isPressed == true && isClickArea(event.getX(),event.getY())) {

                        float nextTipValue = getNextPointTipValue(event.getX(),event.getY());
                        transfromTipToInnerProperty(nextTipValue);

                        if (mSeekChangeistener != null)
                            mSeekChangeistener.onProgressChanged(null, mProgress, true);

                        if (onExtendTipListener != null)
                            onExtendTipListener.updateTipContent(getTipOutString(getTipValue(TIP_SHOW_SCALE)));
                        invalidate();
                    }
                    removeCallbacks(showAnimaRunnable);
                    if (showState == SEEK_BAR_STATE.SHOW || showState == SEEK_BAR_STATE.START_SHOW)
                        postDelayed(dispearAnimaRunnable, 500);
                    isPressed = false;
                    isLongPressed = false;
                    break;
            }
            return super.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }


    private boolean isTouchVisibleArea(float touchY){
        boolean touchVisibleArea = true;
        if (showState == SEEK_BAR_STATE.DISPEAR) {
            float visibleTop = 0;
            float visibleBottom = getHeight();
            int unitSize = partialUnits.size();
            if (unitSize >= 1) {
                visibleTop = partialUnits.get(0).circle_center_y - mCenterTipCircleradius;
                visibleBottom = partialUnits.get(unitSize - 1).circle_center_y + mCenterTipCircleradius;
            }
            if (touchY < visibleBottom && touchY > visibleTop)
                touchVisibleArea = true;
            else
                touchVisibleArea = false;
        }
        return touchVisibleArea;
    }

    private boolean isClickArea(float touchX, float touchY){
        return isTouchCenterCircle(touchX,touchY) || isTouchLittleCenterCircle(touchX,touchY);
    }

    private boolean isTouchCenterCircle(float touchX, float touchY){
        float x = touchX - getWidth() / 2;
        float y = touchY - getHeight() / 2;
        return Math.abs(x) <= mCenterTipCircleradius && Math.abs(y) <= mCenterTipCircleradius;
    }

    private boolean isTouchLittleCenterCircle(float touchX, float touchY){
        boolean result = false;

        float x = touchX - getWidth() / 2;
        for (PartialUnit unit:partialUnits){
            float y = touchY - unit.circle_center_y;
            if (Math.abs(x) <= mCenterTipCircleradius && Math.abs(y) <= mCenterTipCircleradius) {
             return true;
            }
        }
        return result;
    }

    private void handleActionDown(MotionEvent event) {
        mDownY = event.getY();
    }

    private boolean handleActionMove(MotionEvent event) {
        float mTouchY = event.getY();
        if (isTouch(mTouchY)) {
            float tmpHeight = mTouchY - mDownY;
            diffHeight += tmpHeight;
            if (diffHeight >= 0 && diffHeight <= mProgressHeight) {
                invalidate();
            } else {
                diffHeight -= tmpHeight;
            }
            mDownY = mTouchY;
            int tmpProgress = transformDiffToProgress();
            if (mProgress != tmpProgress) {
                mProgress = tmpProgress;
                mSeekChangeistener.onProgressChanged(null, mProgress, true);
                if (onExtendTipListener != null && !isSimple())
                    onExtendTipListener.updateTipContent(getTipOutString(getTipValue(TIP_SHOW_SCALE)));
            }
            return true;
        }
        return false;
    }

    private float getNextPointTipValue(float touchX, float touchY){
        if (isTouchCenterCircle(touchX,touchY)){
            return getCenterNextPointTipValue();
        } else {
            return getlittleCircleMinTipValueFromHeight(touchY);
        }
    }


    private float getCenterNextPointTipValue(){
        float currentTipValue = getTipValue(TIP_CALCULATE_SCALE);
        float result = mScaleMin;
        for (PartialUnit unit: partialUnits){
            if (Math.abs(getTipValue(TIP_CALCULATE_SCALE) - unit.tipMin) < 0.1f) {
                result = unit.tipMax == mScaleMax ? mScaleMin : unit.tipMax;
            }

            if (currentTipValue > unit.tipMin && currentTipValue < unit.tipMax) {
                result = unit.tipMin;
            }
        }
        return result;
    }

    private float getlittleCircleMinTipValueFromHeight(float touchY){
        float result = mScaleMin;
        for (PartialUnit unit:partialUnits){
            float y = touchY - unit.circle_center_y;
            if (Math.abs(y) <= mCenterTipCircleradius) {
                result = unit.tipMin;
                break;
            }
        }
        return result;
    }

    private void transfromTipToInnerProperty(float tipValue) {
        float tmpProgress = 0;

        for (PartialUnit unit : partialUnits) {
            float tmp = (unit.tipMax - Math.max(tipValue, unit.tipMin)) * unit.height_weight / (unit.tipMax - unit.tipMin);
            tmpProgress += tmp;
            if (tipValue > unit.tipMin)
                break;
        }

        mProgress = (int) (tmpProgress + 0.5);

        diffHeight = transformProgressToDiff();
    }

    private int transformDiffToProgress() {
        //calculate 1
        int tmpProgress = (int) ((diffHeight / mProgressHeight) * mProgressMax + 0.5);
        return tmpProgress;
    }

    private float transformProgressToDiff() {
        //calculate 1
        float diffTmp = mProgressHeight * mProgress / mProgressMax;
        return diffTmp;
    }


    private boolean isTouch(float touchY) {
        int tmpHeight = Math.max(mHeight / 2, mProgressHeight) * 3 / 2;
        return touchY - (mHeight / 2 - tmpHeight) > 0.01f && (mHeight / 2 + tmpHeight) - touchY > 0.01f;
    }

    private void startDispearAnimationMotion() {
        Log.d(TAG, "startDispearAnimationMotion");
        ValueAnimator animator = ValueAnimator.ofInt(100, 0);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (showState != SEEK_BAR_STATE.START_DISPEAR)
                    return;

                mColorAlphaPercent = ((int) animation.getAnimatedValue());
                if (mZoomType == ZOOM_TYPE.TYPE_SINGLE) {
                    calculateCircleTipPaintVar(mColorAlphaPercent);
                } else {
                    calculateBackgroundPaintVar(mColorAlphaPercent);
                }                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                showState = SEEK_BAR_STATE.START_DISPEAR;
                if (onExtendTipListener != null)
                    onExtendTipListener.hideTip();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mSeekChangeistener.onStopTrackingTouch(null);
                if (showState != SEEK_BAR_STATE.START_DISPEAR)
                    return;
                showState = SEEK_BAR_STATE.DISPEAR;
                switch (mZoomType) {
                    case TYPE_DOUBLE:
                    case TYPE_TRIPLE:
                        mDrawTipCircle = true;
                        mDrawBackgroundProgress = false;
                        break;
                    case TYPE_SINGLE:
                    default:
                        mDrawTipCircle = true;
                        mDrawBackgroundProgress = false;
                }
                invalidate();

                if (onProgressStateChangeListener != null)
                    onProgressStateChangeListener.onProgressVisibleChange(false);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    private void startShowAnimationMotion() {
        Log.d(TAG, "startShowAnimationMotion mColorAlphaPercent");
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (showState != SEEK_BAR_STATE.START_SHOW)
                    return;
                mColorAlphaPercent = ((int) animation.getAnimatedValue());
                if (mZoomType == ZOOM_TYPE.TYPE_SINGLE) {
                    calculateCircleTipPaintVar(mColorAlphaPercent);
                } else {
                    calculateBackgroundPaintVar(mColorAlphaPercent);
                }
                invalidate();
            }
        });
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mSeekChangeistener.onStartTrackingTouch(null);

                if (onProgressStateChangeListener != null)
                    onProgressStateChangeListener.onProgressVisibleChange(true);

                showState = SEEK_BAR_STATE.START_SHOW;
                switch (mZoomType) {
                    case TYPE_DOUBLE:
                    case TYPE_TRIPLE:
                        mDrawTipCircle = false;
                        mDrawBackgroundProgress = true;
                        break;
                    case TYPE_SINGLE:
                    default:
                        mDrawTipCircle = true;
                        mDrawBackgroundProgress = false;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (showState != SEEK_BAR_STATE.START_SHOW)
                    return;
                showState = SEEK_BAR_STATE.SHOW;
                if (onExtendTipListener != null && !isSimple())
                    onExtendTipListener.showTip();
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }


    private void calculateBackgroundPaintVar(int alphaPercent) {
        mScaleTextPaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
//        mBackGroundPaint.setAlpha(BACK_CIRCLE_ALPHA * alphaPercent / 100);
        mProgressPaint.setAlpha(PROGRESS_BAR_ALPHA_BASE * alphaPercent / 100);

        mProgressTipPaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
        mProgressIndicatePaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
        mTipTextPaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
    }

    private void calculateCircleTipPaintVar(int alphaPercent) {
        mCenterTipCirclePaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
        mCenterTipBgCirclePaint.setAlpha(BACK_CIRCLE_ALPHA * alphaPercent / 100);

        mLittleCirclePaint.setAlpha(LITTLE_TIP_CIRCLE_FRONT_ALPHA_BASE * alphaPercent / 100);
        mLittleCircleBgPaint.setAlpha(LITTLE_TIP_CIRCLE_ALPHA_BASE * alphaPercent / 100);

        mCircleTipTextPaint.setAlpha(COLOR_ALPHA_BASE * alphaPercent / 100);
    }

    public void setmZoomType(ZOOM_TYPE type) {
        mZoomType = type;
        switch (mZoomType) {
            case TYPE_DOUBLE:
                mDrawBackgroundProgress = false;
                mDrawTipCircle = true;
                break;
            case TYPE_SINGLE:
            default:
                mDrawBackgroundProgress = false;
                mDrawTipCircle = true;
                break;
        }
        calculateCircleTipPaintVar(100);
        calculateBackgroundPaintVar(100);
    }

    public void setSimple(boolean isSimple) {
        setmZoomType(isSimple?ZOOM_TYPE.TYPE_SINGLE:ZOOM_TYPE.TYPE_DOUBLE);
    }

    public boolean isSimple(){
        return mZoomType == ZOOM_TYPE.TYPE_SINGLE;
    }


    public void setDetector(boolean isDetector) {
        mIsDetector = isDetector;
    }

    private void printDetail() {
        if (!DEBUG)
            return;

        String info = "info \n";
        info = info + "mWidth: " + mWidth + "\n";
        info = info + "mHeight: " + mHeight + "\n";
        info = info + "mZoomType: " + mZoomType + "\n";
        info = info + "mStartProgressX: " + mStartProgressX + "\n";
        info = info + "mStartProgressY: " + mStartProgressY + "\n";
        info = info + "mProgressHeight: " + mProgressHeight + "\n";
        info = info + "mProgressWidth: " + mProgressWidth + "\n";
        info = info + "mProgressTwoPartRadio: " + mProgressTwoPartRadio + "\n";
        info = info + "mProgressDottedItemSpace: " + mProgressDottedItemSpace + "\n";
        info = info + "mProgressDottedItemHeight: " + mProgressDottedItemHeight + "\n";
        info = info + "mScaleMin: " + mScaleMin + "\n";
        info = info + "mScaleMax: " + mScaleMax + "\n";
        info = info + "diffHeight: " + diffHeight + "\n";
        info = info + "mProgress: " + mProgress + "\n";
        String sectionString = "mScaleSection: ";
        if (mScaleSection != null) {
            for (float unit:mScaleSection) {
                sectionString += unit + ",";
            }
            sectionString +="\n";
            info = info + sectionString;
        }
        for (PartialUnit unit: partialUnits){
            info = info + " unit height = " + unit.height + " weight = " + unit.height_weight +  " \n " + " min = " + unit.tipMin + " max = " + unit.tipMax + " type" + unit.progressType +"\n";
        }
        info = info + "tip value: " + getTipValue(TIP_CALCULATE_SCALE) + "\n";
        info = info + "tip text: " + getTipString(getTipValue(TIP_CALCULATE_SCALE)) + "\n";
        info = info + "tip mDrawTipCircle: " + mDrawTipCircle + "\n";
        info = info + "mDrawBackgroundProgress: " + mDrawBackgroundProgress + "\n";
        info = info + "mColorAlphaPercent: "+mColorAlphaPercent+"\n";
        Log.d(TAG, info);
    }


    public enum ZOOM_TYPE {TYPE_SINGLE, TYPE_DOUBLE, TYPE_TRIPLE}

    public enum SEEK_BAR_STATE {START_DISPEAR, DISPEAR, START_SHOW, SHOW}

    public enum LINE_TYPE {TYPE_ACTIVE, TYPE_DOTTED}

    class PartialUnit {
        float tipMax;
        float tipMin;
        int height;
        int height_weight;
        int circle_center_y;
        LINE_TYPE progressType;
    }


    private static final int ANIMATION_SPEED = 270; // 270 deg/sec

    private int mCurrentDegree = 0; // [0, 359]
    private int mStartDegree = 0;
    private int mTargetDegree = 0;

    private boolean mClockwise = false, mEnableAnimation = true;

    private long mAnimationStartTime = 0;
    private long mAnimationEndTime = 0;

    // Rotate the view counter-clockwise
    @Override
    public void setOrientation(int degree, boolean animation) {
        mEnableAnimation = animation;
        // make sure in the range of [0, 359]
        degree = degree >= 0 ? degree % 360 : degree % 360 + 360;
        if (degree == mTargetDegree)
            return;

        mTargetDegree = degree;
        if (mEnableAnimation) {
            mStartDegree = mCurrentDegree;
            mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();

            int diff = mTargetDegree - mCurrentDegree;
            diff = diff >= 0 ? diff : 360 + diff; // make it in range [0, 359]
            // Make it in range [-179, 180]. That's the shorted distance between the
            // two angles
            diff = diff > 180 ? diff - 360 : diff;

            mClockwise = diff >= 0;
            mAnimationEndTime = mAnimationStartTime
                    + Math.abs(diff) * 1000 / ANIMATION_SPEED;
        } else {
            mCurrentDegree = mTargetDegree;
        }

        invalidate();
    }

    private void calculateCurrentOrientationDegree() {
        if (mCurrentDegree != mTargetDegree) {
            long time = AnimationUtils.currentAnimationTimeMillis();
            if (time < mAnimationEndTime) {
                int deltaTime = (int) (time - mAnimationStartTime);
                int degree = mStartDegree + ANIMATION_SPEED
                        * (mClockwise ? deltaTime : -deltaTime) / 1000;
                degree = degree >= 0 ? degree % 360 : degree % 360 + 360;
                mCurrentDegree = degree;
                invalidate();
            } else {
                mCurrentDegree = mTargetDegree;
            }
        }
    }

    public interface OnExtendTipListener {
        public void showTip();
        public void hideTip();
        public void updateTipContent(String tipContent);
    }

    public interface OnProgressStateChangeListener {
        public void onProgressVisibleChange(boolean visible);
    }
}
