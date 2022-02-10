/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.sprd.camera.aidetection.AIDetectionController;
import android.graphics.Rect;
import com.android.camera.util.CameraUtil;

public class FaceView extends View
    implements Rotatable, PreviewStatusListener.PreviewAreaChangedListener {
    private static final Log.Tag TAG = new Log.Tag("FaceView");
    private static final boolean LOGV = false;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mDisplayOrientation;
    // The orientation compensation for the face indicator to make it look
    // correctly in all device orientations. Ex: if the value is 90, the
    // indicator should be rotated 90 degrees counter-clockwise.
    private int mOrientation;
    private boolean mMirror;
    private boolean mPause;
    private Matrix mMatrix = new Matrix();
    private RectF mRect = new RectF();
    // As face detection can be flaky, we add a layer of filtering on top of it
    // to avoid rapid changes in state (eg, flickering between has faces and
    // not having faces)
    private Face[] mFaces;
    private int mFaceColor;
    private Paint mFacePaint;
    private int mAttributesColor;
    private Paint mAttributesPaint;
    private volatile boolean mBlocked;
    private int[] mAttributes;
    /*
    the unit of below constant is dp, need use the method "dpConvertToPx" to transform to px
    */
    private static final int AGE_BITMAP_SIDE_LENGTH = 16;
    private static final int DISTANCE_FROM_FACE_RECT = 20;
    private static final int MARGINS_IN_AGE = 8;

    private int mAgeBitmapSideLenght;
    private int mDistanceFromFaceRect;
    private int mMarginsInAge;

    private static final int MSG_SWITCH_FACES = 1;
    private static final int SWITCH_DELAY = 70;
    private static final int MSG_CLEAR_FACES = 2;//SPRD:Add for ai detect
    private static final int CLEAR_DELAY = 300;//SPRD:Add for ai detect
    private static final int DISAPPEAR_DELAY = 3000;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            /* SPRD:Add for ai detect @{ */
            case MSG_CLEAR_FACES:
                clear();
                break;
            /* @} */
            }
        }
    };
    private final RectF mPreviewArea = new RectF();

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        mFaceColor = res.getColor(R.color.face_detect_color);
        mFacePaint = new Paint();
        mFacePaint.setAntiAlias(true);
        mFacePaint.setStyle(Style.STROKE);
        mFacePaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));
        mAttributesColor = res.getColor(R.color.face_detect_color);
        mAttributesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAttributesPaint.setStrokeWidth(2);
        mAttributesPaint.setTextSize(28);
        mAgeBitmapSideLenght = dpConvertToPx(AGE_BITMAP_SIDE_LENGTH);
        mDistanceFromFaceRect = dpConvertToPx(DISTANCE_FROM_FACE_RECT);
        mMarginsInAge = dpConvertToPx(MARGINS_IN_AGE);
    }

    public void setFaces(Face[] faces,int[] attributes) {
        // Change the Strategy of log print, I think it should not always print a same log
        // this log.v is the hotspot in this method so comment out it
        /*if (mFaces == null || faces.length != mFaces.length) {
            Log.v(TAG, "Num of faces=" + faces.length);
        }*/
        /* @} */
        if (mPause) return;
        mAttributes = attributes;
        /* SPRD: Fix bug 651199 that face view disappears slowly than ref phone @{
        if (mFaces != null) {
            if ((faces.length > 0 && mFaces.length == 0)
                    || (faces.length == 0 && mFaces.length > 0)) {
                mPendingFaces = faces;
                if (!mStateSwitchPending) {
                    mStateSwitchPending = true;
                    mHandler.sendEmptyMessageDelayed(MSG_SWITCH_FACES, SWITCH_DELAY);
                }
                return;
            }
        }
        if (mStateSwitchPending) {
            mStateSwitchPending = false;
            mHandler.removeMessages(MSG_SWITCH_FACES);
        }
        /* @} */
        mFaces = faces;
        invalidate();
    }

    public void setFaces(Face[] faces){
        //mFaceColor = getResources().getColor(R.color.face_detect_color);
        setFaces(faces,null);
    }
    public void setDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
        if (LOGV) {
            Log.v(TAG, "mDisplayOrientation=" + orientation);
        }
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        /*
         * SPRD Bug:519334 Refactor Rotation UI of Camera. @{
         * Original Android code:
        */
        mOrientation = orientation;
        invalidate();


    }

    public void setMirror(boolean mirror) {
        mMirror = mirror;
        if (LOGV) {
            Log.v(TAG, "mMirror=" + mirror);
        }
    }

    public boolean faceExists() {
        return (mFaces != null && mFaces.length > 0);
    }

    public void clear() {
        // Face indicator is displayed during preview. Do not clear the
        // drawable.
        mFaces = null;
        mNeedDisappear = true;
        invalidate();
    }
    //SPRD: Fix bug 1105014
    boolean isShowAttributeOnly = false;
    public void setShowAttributeOnly(){
        isShowAttributeOnly = true;
    }
    public void pause() {
        mPause = true;
    }

    public void resume() {
        mPause = false;
    }

    public void setBlockDraw(boolean block) {
        mBlocked = block;
    }

    /* SPRD:Add for ai detect @{ */
    public void clearFacesDelayed() {
        if (!mHandler.hasMessages(MSG_CLEAR_FACES)) {
            mHandler.sendEmptyMessageDelayed(MSG_CLEAR_FACES, CLEAR_DELAY);
        }
    }
    /* @} */

    /* SPRD:fix bug967653 should disappear faceview after 3 seconds @{ */
    boolean mNeedDisappear = false;
    public void disappearFaceView() {
        if (mHandler.hasMessages(MSG_CLEAR_FACES)) {
            mHandler.removeMessages(MSG_CLEAR_FACES);
        }
        mNeedDisappear = false;
        mHandler.sendEmptyMessageDelayed(MSG_CLEAR_FACES, DISAPPEAR_DELAY);
    }
    /* @} */


    private boolean needShowFace = true;
    public void setNeedShowFace(boolean isChooseAttributes){
        needShowFace =  !mNeedDisappear || isChooseAttributes;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mBlocked && (mFaces != null) && (mFaces.length > 0) && needShowFace) {
            int rw, rh;
            rw = (int) mPreviewArea.width();
            rh = (int) mPreviewArea.height();
            // Prepare the matrix.
            if (((rh > rw) && ((mDisplayOrientation == 0) || (mDisplayOrientation == 180)))
                    || ((rw > rh) && ((mDisplayOrientation == 90) || (mDisplayOrientation == 270)))) {
                int temp = rw;
                rw = rh;
                rh = temp;
            }
            CameraUtil.prepareMatrix(mMatrix, mMirror, mDisplayOrientation, rw, rh);
            // Focus indicator is directional. Rotate the matrix and the canvas
            // so it looks correctly in all orientations.
            canvas.save();
            mMatrix.postRotate(mOrientation); // postRotate is clockwise
            canvas.rotate(-mOrientation); // rotate is counter-clockwise (for canvas)
            for (int i = 0; i < mFaces.length; i++) {
                // Transform the coordinates.
                mRect.set(mFaces[i].rect);
                CameraUtil.dumpRect(mRect, "Original rect",LOGV);
                mMatrix.mapRect(mRect);
                CameraUtil.dumpRect(mRect, "Transformed rect",LOGV);

                if (mOrientation == 0)
                    mRect.offset(mPreviewArea.left, mPreviewArea.top);
                if (mOrientation == 90)
                    mRect.offset(-mPreviewArea.top,0);
                if (mOrientation == 270)
                    mRect.offset(mPreviewArea.top, 0);
                if (mOrientation == 180)
                    mRect.offset(mPreviewArea.left, -mPreviewArea.top);
                /* SPRD: Fix bug 1105014 face attibute change and add enable tag @{ */
                if (mAttributes == null || mAttributes[i] == 0 || isOnlyDrawFace(mAttributes[i])){
                    Log.i(TAG,"mAttributes data is null,0,or gender and race is 0");
                    if (!isShowAttributeOnly){
                        mFaceColor = getResources().getColor(R.color.face_detect_color);
                        mFacePaint.setColor(mFaceColor);
                        canvas.drawRoundRect(mRect, 20.0f, 20.0f, mFacePaint);
                    }
                    continue;
                }
                /* @} */

                /*
                  get face attributes
                */
                int raceInt = mAttributes[i] /1000;
                int temp = mAttributes[i] % 1000;
                int gender = temp / 100;
                int age = temp % 100;

                // Combinate String
                StringBuilder stringBuilder = new StringBuilder();
                if(raceInt > 0){
                    String race = null;
                    switch (raceInt){
                        case 1: race = mContext.getString(R.string.face_race_yellow);break;
                        case 2: race = mContext.getString(R.string.face_race_white);break;
                        case 3: race = mContext.getString(R.string.face_race_black);break;
                        case 4: race = mContext.getString(R.string.face_race_india);break;
                    }
                    stringBuilder.append(race);
                    //stringBuilder.append(" ");
                }
//                if (age >  0){
//                    stringBuilder.append(String.format(mContext.getString(R.string.face_age), age));
//                }
                String finalAttribute = stringBuilder.toString();


                //Calculate background
                RectF backgroundRect = null;
                float backgroundRectWidth;
                float backgroundRectHeight;
                Paint.FontMetrics fontMetrics = mAttributesPaint.getFontMetrics();
                if (stringBuilder.length() > 0){
                    float ageWidth = (int)mAttributesPaint.measureText(finalAttribute);
                    float ageHeight =  (int) Math.ceil(fontMetrics.descent - fontMetrics.top) + 2;

                    backgroundRectWidth = ageWidth + 3*mMarginsInAge + mAgeBitmapSideLenght;
                    backgroundRectHeight = (ageHeight > mAgeBitmapSideLenght ? ageHeight : mAgeBitmapSideLenght);
                    backgroundRect = new RectF(mRect.left+((mRect.right-mRect.left)/2 - backgroundRectWidth/2),
                            mRect.top-(backgroundRectHeight+mDistanceFromFaceRect+mAgeBitmapSideLenght),
                            mRect.right-((mRect.right-mRect.left)/2 - backgroundRectWidth/2),
                            mRect.top-mDistanceFromFaceRect);

                } else if (stringBuilder.length() == 0 && gender > 0){
                    backgroundRectWidth = 2*mMarginsInAge + mAgeBitmapSideLenght;
                    backgroundRectHeight = mAgeBitmapSideLenght;
                    backgroundRect = new RectF(mRect.left+((mRect.right-mRect.left)/2 - backgroundRectWidth/2),
                            mRect.top-(backgroundRectHeight+mDistanceFromFaceRect+mAgeBitmapSideLenght),
                            mRect.right-((mRect.right-mRect.left)/2 - backgroundRectWidth/2),
                            mRect.top-mDistanceFromFaceRect);
                }

                //set color and bitmap by genger
                Bitmap bitmapgender = null;
                if (gender == 0){
                    mAttributesColor = mContext.getColor(R.color.age_male_color); //bugfix 1101483 default gener is male
                    bitmapgender = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_age_male);
                    mFaceColor = mContext.getColor(R.color.age_male_color);
                } else if(gender == 1){
                    mAttributesColor = mContext.getColor(R.color.age_male_color);
                    bitmapgender = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_age_male);
                    mFaceColor = mContext.getColor(R.color.age_male_color);
                }else if (gender == 2){
                    mAttributesColor = mContext.getColor(R.color.age_female_color);
                    bitmapgender = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_age_female);
                    mFaceColor = mContext.getColor(R.color.age_female_color);
                }

                //draw background
                mAttributesPaint.setColor(mAttributesColor);
                if (backgroundRect!=null)
                    canvas.drawRect(backgroundRect, mAttributesPaint);

                //draw gender
                RectF bitmapRect = null;
                if (bitmapgender != null){
                    bitmapRect = new RectF(backgroundRect.left+mMarginsInAge,
                            backgroundRect.centerY()-mMarginsInAge,
                            backgroundRect.left+mMarginsInAge+mAgeBitmapSideLenght,
                            backgroundRect.centerY()-mMarginsInAge +mAgeBitmapSideLenght);
                    canvas.drawBitmap(bitmapgender,null,bitmapRect,null);
                }

                //draw text
                if (stringBuilder.length() > 0){
                    mAttributesPaint.setColor(Color.WHITE);
                    if (bitmapRect != null)
                        canvas.drawText(finalAttribute, bitmapRect.right+mMarginsInAge, backgroundRect.centerY() - (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.top , mAttributesPaint);
                    else
                        canvas.drawText(finalAttribute,backgroundRect.left+mMarginsInAge,backgroundRect.centerY() - (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.top , mAttributesPaint);
                }

                //draw face
                mFacePaint.setColor(mFaceColor);
                canvas.drawRoundRect(mRect, 20.0f, 20.0f, mFacePaint);
            }
            canvas.restore();
            isShowAttributeOnly = false;
        }
        super.onDraw(canvas);
    }

    private boolean isOnlyDrawFace(int attributes){
        int raceInt = attributes /1000;
        int temp = attributes % 1000;
        int gender = temp / 100;
        if (raceInt == 0 && gender == 0) //don't show age
            return true;
        return false;
    }
    private int dpConvertToPx(int dp) {
        DisplayMetrics mDisplayMetrics = mContext.getResources().getDisplayMetrics();
        int densityDpi = mDisplayMetrics.densityDpi;
        int px = dp * (densityDpi/160);
        return px;
    }
    /* SPRD:Add for ai detect @{ */
    public boolean isPause() {
        return mPause;
    }
    /* @} */

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        mPreviewArea.set(previewArea);
    }

    /* SPRD: New Feature for VGesture @{ */
    public Rect[] getFaceRect(Face[] faces){
        if (mPause)
            return null;// SPRD BUG : 397097
        RectF vrect = new RectF();
        Rect[] rect = null;
         if ((faces != null) && (faces.length > 0)) {
             rect = new Rect[faces.length];
             int rw, rh;
             rw = (int) mPreviewArea.width();
             rh = (int) mPreviewArea.height();
             // Prepare the matrix.
             if (((rh > rw) && ((mDisplayOrientation == 0) || (mDisplayOrientation == 180)))
                     || ((rw > rh) && ((mDisplayOrientation == 90) || (mDisplayOrientation == 270)))) {
                 int temp = rw;
                 rw = rh;
                 rh = temp;
             }
             CameraUtil.prepareMatrix(mMatrix, mMirror, mDisplayOrientation, rw, rh);
             // Focus indicator is directional. Rotate the matrix and the canvas
             // so it looks correctly in all orientations.
             mMatrix.postRotate(mOrientation); // postRotate is clockwise
//             canvas.rotate(-mOrientation); // rotate is counter-clockwise (for canvas)
             for (int i = 0; i < faces.length; i++) {

                 // Transform the coordinates.
                 rect[i] = new Rect();
                 vrect.set(faces[i].rect);
                 mMatrix.mapRect(vrect);
                 vrect.offset(mPreviewArea.left, mPreviewArea.top);
                 vrect.roundOut(rect[i]);
             }
         }
         return rect;
    }

    public Rect getRealArea(){
        Rect rect = new Rect();
       mPreviewArea.roundOut(rect);
       return rect;
    }
    /* @} */


    @Override
    public void setVisibility(int visibility) {
        if (!CameraUtil.isAIDetectEnabled()) {
            super.setVisibility(View.GONE);
        } else {
            super.setVisibility(visibility);
        }
    }
}
