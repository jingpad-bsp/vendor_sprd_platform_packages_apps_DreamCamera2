/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.camera.ui.Rotatable;
import com.android.camera.util.ApiHelper;
import com.android.camera2.R;


public class Level extends View {

    public Context mContext;
    public String TAG ="Level";

    private boolean mHasLevelAngle;
    private int mUiRotation;
    private double mLevelAngle;
    private boolean mHasPitchAngle;
    private double mPitchAngle ;
    private boolean mHasGeoDirection ;
    private double mGeoDirection ;
    private int mRotation;
    private double mOriglevelangle;
    public void setHasLevelAngle(boolean hasLevelAngle){
        mHasLevelAngle = hasLevelAngle;
    }
    public void setUIRotation(int UIRotation){
        mUiRotation = UIRotation;
    }
    public void setLevelAngle(double LevelAngle){
        mLevelAngle = LevelAngle;
    }
    public void setHasPitchAngle(boolean hasPitchAngle){
        mHasPitchAngle = hasPitchAngle;
    }
    public void setPitchAngle(double PitchAngle){
        mPitchAngle = PitchAngle;
    }
    public void setHasGeoDirection(boolean hasGeoDirection){
        mHasGeoDirection = hasGeoDirection;
    }
    public void setGeoDirection(double GeoDirection){
        mGeoDirection = GeoDirection;
    }
    public void setRotation(int rotation){
        this.mRotation =rotation;
    }
    public void setOrigLevelAngle(double OrigLevelAngle){
        mOriglevelangle = OrigLevelAngle;
    }
    public Level(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void onDraw(Canvas canvas){
        Paint p = new Paint();
        p.setAntiAlias(true);
        RectF draw_rect = new RectF();
        int mLongrectfWitch = (int) (90 * getContext().getResources().getDisplayMetrics().density + 0.5f); // convert dps to pixels
        int mHalflongrectfHeight = (int) (0.5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        int mRadiusRound = (int)(18 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        int mRoundRadius = (int) (17.5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        int mRoundWidth = (int) (1 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        int mShortrectfWitch = (int)(8 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        int mHalfshortrectfHeight = (int)(1.5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
        if( mHasLevelAngle ) {

            double angle = - mOriglevelangle;

            switch (this.mRotation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    angle -= 90.0;
                    break;
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                default:
                    break;
            }

            int cx = canvas.getWidth()/2;
            int cy = (int) (272 * getContext().getResources().getDisplayMetrics().density + 0.5f);

            boolean isLevel = false;
            if( Math.abs(mLevelAngle) <= 1.0f ) {
                isLevel = true;
            }

            canvas.save();
            canvas.rotate((float)angle, cx, cy);



            int hthickness = 1;
            p.setStyle(Paint.Style.FILL);

            // draw outline
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x40);
            // can't use drawRoundRect(left, top, right, bottom, ...) as that requires API 21
            canvas.drawRect(cx - mLongrectfWitch - mRadiusRound - 2 * hthickness, cy - hthickness - mHalflongrectfHeight, cx - mRadiusRound, cy + hthickness + mHalflongrectfHeight,p);

            canvas.drawRect(cx + mRadiusRound , cy - hthickness - mHalflongrectfHeight, cx + mLongrectfWitch + mRadiusRound + 2 * hthickness, cy + hthickness + mHalflongrectfHeight,p);

            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);
            canvas.drawRect(cx - mLongrectfWitch - mRadiusRound - 3 * hthickness - mShortrectfWitch, cy - hthickness - mHalfshortrectfHeight, cx - mLongrectfWitch - mRadiusRound - hthickness, cy +  hthickness + mHalfshortrectfHeight , p);

            canvas.drawRect(cx + mRadiusRound + mLongrectfWitch + hthickness, cy - hthickness - mHalfshortrectfHeight, cx + mLongrectfWitch + mRadiusRound + 3 * hthickness + mShortrectfWitch, cy + hthickness + mHalfshortrectfHeight , p);

            p.setColor(mContext.getResources().getColor(R.color.white));
            if(isLevel){
                p.setAlpha(0xFF);
            }else{
                p.setAlpha(0x80);
            }


            canvas.drawRect(cx - mLongrectfWitch - mRadiusRound - hthickness, cy -  mHalflongrectfHeight, cx - mRadiusRound - hthickness, cy + mHalflongrectfHeight , p);

            canvas.drawRect(cx + mRadiusRound + hthickness, cy - mHalflongrectfHeight, cx + mLongrectfWitch + mRadiusRound + hthickness, cy + mHalflongrectfHeight , p);

            if(isLevel){
                p.setColor(mContext.getResources().getColor(R.color.dream_yellow));
            }else{
                p.setColor(mContext.getResources().getColor(R.color.white));
            }
            p.setAlpha(0xFF);
            canvas.drawRect(cx - mLongrectfWitch - mRadiusRound - mShortrectfWitch - 2 * hthickness, cy - mHalfshortrectfHeight, cx - mRadiusRound - mLongrectfWitch - 2 * hthickness, cy + mHalfshortrectfHeight , p);

            canvas.drawRect(cx + mRadiusRound + mLongrectfWitch + 2 * hthickness, cy - mHalfshortrectfHeight, cx + mLongrectfWitch + mRadiusRound + mShortrectfWitch + 2 * hthickness, cy + mHalfshortrectfHeight , p);

            canvas.restore();
            canvas.save();
            if((angle > -45 && angle <= 0) || (angle < -315) || (angle > -225 && angle <= -135)){
                canvas.rotate(0,cx,cy);
            } else{
                canvas.rotate(90,cx,cy);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);

            p.setStrokeWidth(mRoundWidth + 2);
            RectF rect = new RectF(cx - mRoundRadius ,cy - mRoundRadius ,cx + mRoundRadius ,cy + mRoundRadius );
            canvas.drawArc(rect,11,158,false,p);
            canvas.drawArc(rect,-11,-158,false,p);
            p.setColor(mContext.getResources().getColor(R.color.white));
            p.setAlpha(0xFF);

            p.setStrokeWidth(mRoundWidth);

            canvas.drawArc(rect,11,158,false,p);
            canvas.drawArc(rect,-11,-158,false,p);

            p.setStyle(Paint.Style.FILL);

            // draw outline
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);
            canvas.drawRect(cx - mRadiusRound - hthickness, cy - hthickness - mHalflongrectfHeight, cx + mRadiusRound + hthickness, cy + hthickness + mHalflongrectfHeight , p);

            if(isLevel){
                p.setColor(mContext.getResources().getColor(R.color.dream_yellow));
            }else{
                p.setColor(mContext.getResources().getColor(R.color.white));
            }
            p.setAlpha(0xFF);
            canvas.drawRect(cx - mRadiusRound , cy - mHalflongrectfHeight, cx + mRadiusRound , cy + mHalflongrectfHeight , p);



            p.setAlpha(255);
            p.setStyle(Paint.Style.FILL); // reset

            canvas.restore();
        }else{
            int dx = canvas.getWidth()/2;
            int dy = (int) (272 * getContext().getResources().getDisplayMetrics().density + 0.5f);
            canvas.rotate((float)0, dx, dy);
            int hthickness = 1;
            p.setStyle(Paint.Style.FILL);

            // draw outline
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x40);
            // can't use drawRoundRect(left, top, right, bottom, ...) as that requires API 21
            canvas.drawRect(dx - mLongrectfWitch - mRadiusRound - 2 * hthickness, dy - hthickness - mHalflongrectfHeight, dx - mRadiusRound, dy + hthickness + mHalflongrectfHeight,p);

            canvas.drawRect(dx + mRadiusRound , dy - hthickness - mHalflongrectfHeight, dx + mLongrectfWitch + mRadiusRound + 2 * hthickness, dy + hthickness + mHalflongrectfHeight,p);

            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);
            canvas.drawRect(dx - mLongrectfWitch - mRadiusRound - 3 * hthickness - mShortrectfWitch, dy - hthickness - mHalfshortrectfHeight, dx - mLongrectfWitch - mRadiusRound - hthickness, dy +  hthickness + mHalfshortrectfHeight , p);

            canvas.drawRect(dx + mRadiusRound + mLongrectfWitch + hthickness, dy - hthickness - mHalfshortrectfHeight, dx + mLongrectfWitch + mRadiusRound + 3 * hthickness + mShortrectfWitch, dy + hthickness + mHalfshortrectfHeight , p);

            p.setColor(mContext.getResources().getColor(R.color.white));
            p.setAlpha(0xFF);
            canvas.drawRect(dx - mLongrectfWitch - mRadiusRound - hthickness, dy -  mHalflongrectfHeight, dx - mRadiusRound - hthickness, dy + mHalflongrectfHeight , p);

            canvas.drawRect(dx + mRadiusRound + hthickness, dy - mHalflongrectfHeight, dx + mLongrectfWitch + mRadiusRound + hthickness, dy + mHalflongrectfHeight , p);
            p.setColor(mContext.getResources().getColor(R.color.dream_yellow));
            p.setAlpha(0xFF);
            canvas.drawRect(dx - mLongrectfWitch - mRadiusRound - mShortrectfWitch - 2 * hthickness, dy - mHalfshortrectfHeight, dx - mRadiusRound - mLongrectfWitch - 2 * hthickness, dy + mHalfshortrectfHeight , p);

            canvas.drawRect(dx + mRadiusRound + mLongrectfWitch + 2 * hthickness, dy - mHalfshortrectfHeight, dx + mLongrectfWitch + mRadiusRound + mShortrectfWitch + 2 * hthickness, dy + mHalfshortrectfHeight , p);
            
            canvas.save();
            canvas.rotate(0,dx,dy);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);

            p.setStrokeWidth(mRoundWidth + 2);
            RectF rect = new RectF(dx - mRoundRadius ,dy - mRoundRadius ,dx + mRoundRadius ,dy + mRoundRadius );
            canvas.drawArc(rect,11,158,false,p);
            canvas.drawArc(rect,-11,-158,false,p);
            p.setColor(mContext.getResources().getColor(R.color.white));
            p.setAlpha(0xFF);

            p.setStrokeWidth(mRoundWidth);

            canvas.drawArc(rect,11,158,false,p);
            canvas.drawArc(rect,-11,-158,false,p);

            p.setStyle(Paint.Style.FILL);

            // draw outline
            p.setColor(mContext.getResources().getColor(R.color.dream_black));
            p.setAlpha(0x80);
            canvas.drawRect(dx - mRadiusRound - hthickness, dy - hthickness - mHalflongrectfHeight, dx + mRadiusRound + hthickness, dy + hthickness + mHalflongrectfHeight , p);
            p.setColor(mContext.getResources().getColor(R.color.dream_yellow));
            p.setAlpha(0xFF);
            canvas.drawRect(dx - mRadiusRound , dy - mHalflongrectfHeight, dx + mRadiusRound , dy + mHalflongrectfHeight , p);



            p.setAlpha(255);
            p.setStyle(Paint.Style.FILL); // reset

            canvas.restore();
        }
    }

}
