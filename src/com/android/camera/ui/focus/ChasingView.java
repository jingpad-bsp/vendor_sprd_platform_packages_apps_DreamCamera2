package com.android.camera.ui.focus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import com.android.camera2.R;

/**
 * TODO: document your custom view class.
 */
public class ChasingView extends View {

    private Paint inCirclePaint;//paint for circle
    private Paint outCirclePaint;

    private final int ChasingCircleColor;
    private static final int ChasingCircleLostColor = Color.WHITE;
    private final float ChasingCircleStrokeWidth;
    private final float ChasingOutCircleRadius;
    private final float ChasingInCircleRadius;
    private final float ChasingCenterCircleRadius;
    private final float ChasingInCircleLineSize;

    private List<Rect> chasingTraceRegions;

    private RectF mPreviewSize;

    private static final int CHASING_SUCCESS = 0;
    private static final int CHASING_TEMP_FAILURE = 1;
    private static final int CHASING_FAILURE = 2;


    public ChasingView(Context context) {
        this(context, null);
    }

    public ChasingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChasingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources res = getResources();
        ChasingCircleColor = res.getColor(R.color.auto_tracking_color);

        ChasingOutCircleRadius = res.getDimensionPixelSize(R.dimen.auto_chasing_out_circle_radius);
        ChasingInCircleRadius = res.getDimensionPixelSize(R.dimen.auto_chasing_in_circle_radius);
        ChasingCenterCircleRadius = res.getDimensionPixelSize(R.dimen.auto_chasing_center_circle_radius);
        ChasingCircleStrokeWidth = res.getDimensionPixelSize(R.dimen.auto_chasing_circle_stroke);
        ChasingInCircleLineSize = res.getDimensionPixelSize(R.dimen.auto_chasing_in_circle_line_size);

        setClickable(false);
        initPointValue();
        initPaint();
    }

    private void initPointValue(){
        chasingTraceRegions = new ArrayList<Rect>();
    }

    public void setChasingTraceRegions(List<Rect> pointRegions){
        chasingTraceRegions.clear();
        chasingTraceRegions.addAll(pointRegions);
        invalidate();
    }

    public void setChasingTraceRegions(int state, Rect rect){
        Log.d("ChasingView","setChasingTraceRegions state = " + state + " , rect = " + rect.toString());
        chasingTraceRegions.clear();
        if (state != CHASING_FAILURE) {
            chasingTraceRegions.add(rect);
        }
        setState(state);
    }

    public void configurePreviewDimensions(RectF previewArea) {
        mPreviewSize = previewArea;

    }

    private void setState(int state){
        if (state == CHASING_SUCCESS){
            inCirclePaint.setColor(ChasingCircleColor);
            outCirclePaint.setColor(ChasingCircleColor);
        } else if (state ==  CHASING_TEMP_FAILURE){
            inCirclePaint.setColor(ChasingCircleLostColor);
            outCirclePaint.setColor(ChasingCircleLostColor);
        }
        invalidate();
    }

    public void clear(){
        chasingTraceRegions.clear();
        invalidate();
    }

    private void initPaint() {
        inCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inCirclePaint.setStyle(Paint.Style.FILL);
        inCirclePaint.setColor(ChasingCircleColor);
        inCirclePaint.setStrokeWidth(ChasingCircleStrokeWidth);

        outCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outCirclePaint.setStyle(Paint.Style.STROKE);
        outCirclePaint.setColor(ChasingCircleColor);
        outCirclePaint.setStrokeWidth(ChasingCircleStrokeWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        if (mPreviewSize != null) {
//            canvas.clipRect(mPreviewSize, Region.Op.REPLACE);
//        }
//        for (Rect p :chasingTraceRegions){
//            if (!p.isEmpty())
//                drawChasingTracePoint(canvas,p);
//        }
        /*Bug 1122289 chang android:targetSdkVersion to 28, Region.Op.REPLACE is not supported*/
        if (mPreviewSize != null) {
            canvas.save();
            canvas.clipRect(mPreviewSize);
            for (Rect p :chasingTraceRegions){
                if (!p.isEmpty())
                    drawChasingTracePoint(canvas,p);
            }
            canvas.restore();
        } else {
            for (Rect p :chasingTraceRegions){
                if (!p.isEmpty())
                    drawChasingTracePoint(canvas,p);
            }
        }
    }

    private void drawChasingTracePoint(Canvas canvas,Rect rect){
        drawChasingTracePoint(canvas,rect.centerX(),rect.centerY());
    }

    private void drawChasingTracePoint(Canvas canvas,int x,int y){
        canvas.save();
        canvas.drawCircle(x,y,ChasingOutCircleRadius,outCirclePaint);
        canvas.drawCircle(x,y,ChasingInCircleRadius,outCirclePaint);
        canvas.drawCircle(x,y,ChasingCenterCircleRadius,inCirclePaint);

        canvas.drawLine(x-ChasingInCircleRadius ,y,x -ChasingInCircleRadius - ChasingInCircleLineSize,y,outCirclePaint);
        canvas.drawLine(x+ChasingInCircleRadius,y,x + ChasingInCircleRadius + ChasingInCircleLineSize,y,outCirclePaint);
        canvas.drawLine(x,y - ChasingInCircleRadius,x,y - ChasingInCircleRadius - ChasingInCircleLineSize,outCirclePaint);
        canvas.drawLine(x,y + ChasingInCircleRadius,x,y + ChasingInCircleRadius + ChasingInCircleLineSize,outCirclePaint);

        canvas.restore();
    }
}
