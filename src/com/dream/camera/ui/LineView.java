package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class LineView extends View {

    private int mStartX1, mStartY1, mStartX2, mStartY2;
    private Paint mPaint;

    public LineView(Context context) {
        super(context);
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(mPaint);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        canvas.drawLine(mStartX1, mStartY1, mStartX2, mStartY2, mPaint);
        super.onDraw(canvas);
    }

    public void setPoints(Integer[] points) {
        if (points == null || points.length < 4) {
            return;
        }
        mStartX1 = points[0];
        mStartY1 = points[1];
        mStartX2 = points[2];
        mStartY2 = points[3];
        invalidate();
    }
}