package com.android.camera.widget.selector.lightPortrait;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import com.android.camera2.R;

public class LightPortraitOverLay extends View {

    private int mPortraitTopPadding;
    private int mLandscapeTopPadding;
    private int mRadius;
    private int mBorderWidth;
    private int mBorderColorNormal;
    private int mBorderColorHightLight;
    private int mColorMask;

    private int mPortraitCenterX;
    private int mPortraitCenterY;
    private int mLandscapeCenterX;
    private int mLandscapeCenterY;

    private Bitmap mBitmap;
    private Canvas mOverLayCanvas;
    private Paint mPaintCircle;
    private Paint mPaintNormalBorder;
    private Paint mPaintHighLightBorder;
    private boolean mStatus = false;
    private int mOrientation = 90;

    public LightPortraitOverLay(Context context) {
        super(context);
        initAttr(context,null);
    }

    public LightPortraitOverLay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context,attrs);
    }

    public LightPortraitOverLay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context,attrs);
    }

    public LightPortraitOverLay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context,attrs);
    }

    private void initAttr(Context context, AttributeSet attrs){
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LightPortraitOverLay);
            mRadius = typedArray.getDimensionPixelSize(R.styleable.LightPortraitOverLay_radius,1);
            mBorderWidth = typedArray.getDimensionPixelSize(R.styleable.LightPortraitOverLay_border_width,1);
            mPortraitTopPadding = typedArray.getDimensionPixelSize(R.styleable.LightPortraitOverLay_portrait_padding,1);
            mLandscapeTopPadding = typedArray.getDimensionPixelSize(R.styleable.LightPortraitOverLay_landscape_padding,1);;

            mBorderColorNormal = typedArray.getColor(R.styleable.LightPortraitOverLay_border_color_normal,1);
            mBorderColorHightLight  = typedArray.getColor(R.styleable.LightPortraitOverLay_border_color_high_light,1);
            mColorMask  = typedArray.getColor(R.styleable.LightPortraitOverLay_border_color_mask,1);
            typedArray.recycle();
        }

        mPaintCircle = new Paint();
        mPaintCircle.setColor(Color.TRANSPARENT);
        mPaintCircle.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintCircle.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mPaintNormalBorder = new Paint();
        mPaintNormalBorder.setColor(mBorderColorNormal);
        mPaintNormalBorder.setStyle(Paint.Style.STROKE);
        mPaintNormalBorder.setStrokeWidth(mBorderWidth);
        mPaintNormalBorder.setFlags(Paint.ANTI_ALIAS_FLAG);

        mPaintHighLightBorder = new Paint();
        mPaintHighLightBorder.setColor(mBorderColorHightLight);
        mPaintHighLightBorder.setStyle(Paint.Style.STROKE);
        mPaintHighLightBorder.setStrokeWidth(mBorderWidth);
        mPaintHighLightBorder.setFlags(Paint.ANTI_ALIAS_FLAG);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mLandscapeCenterX = mPortraitCenterX = getWidth() / 2;
        mPortraitCenterY = mPortraitTopPadding + mRadius;
        mLandscapeCenterY = mLandscapeTopPadding + mRadius;

        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mOverLayCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawMask();
        drawCircle();
        drawBorder();

        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    private void drawBorder() {
        mOverLayCanvas.drawCircle(getCenterX(),getCenterY(),mRadius,getBorderPaint());
    }


    private void drawCircle() {
        mOverLayCanvas.drawCircle(getCenterX(),getCenterY(),mRadius,mPaintCircle);
    }

    private void drawMask() {
        mBitmap.eraseColor(Color.TRANSPARENT);
        mOverLayCanvas.drawColor(mColorMask);
    }

    private Paint getBorderPaint() {
        if(mStatus){
            return mPaintHighLightBorder;
        } else {
            return mPaintNormalBorder;
        }
    }

    private int getCenterY() {
        if(mOrientation % 180 != 0){
            return mPortraitCenterY;
        } else {
            return mLandscapeCenterY;
        }
    }

    private int getCenterX() {
        if(mOrientation % 180 != 0){
            return mPortraitCenterX;
        } else {
            return mLandscapeCenterX;
        }
    }

    public void setOrientation(int orientation){
        mOrientation = orientation;
        postInvalidate();
    }

    public void setStatus(boolean ok){
        mStatus = ok;
        postInvalidate();
    }
}
