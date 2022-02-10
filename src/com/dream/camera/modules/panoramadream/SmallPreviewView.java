package com.dream.camera.modules.panoramadream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

public class SmallPreviewView extends View {

    public static final int SMALL_PREVIEW_FACTOR = 8;
    public static final int LEFT_TO_RIGHT = 1;
    public static final int RIGHT_TO_LEFT = 2;
    public static final int CAPTURE_STATE = 1;
    public static final int PREVIEW_STATE = 2;
    private static final Log.Tag TAG = new Log.Tag("SmallPreviewView");
    private static final int START_MOVE_ANGLE = 3;
    private RectF mDrawBounds;
    private RectF mCaptureDrawBounds = new RectF();
    private RectF mLastCaptureDrawBounds = new RectF();
    Paint mPaintBackground = new Paint();
    Paint mPaintLine = new Paint();
    private Bitmap mPanoArrowLeft;
    private Bitmap mPanoArrowRight;
    private int mPanoArrowGap;
    private int mLeftGapLand;
    private int mRightGapLand;
    private int mCaptureDirector = LEFT_TO_RIGHT;
    private float mWidth;
    private float mHeight;
    private float mMaxProgress = 0;
    private float mProgressX = 0;
    private float mLastProgressX = 0;
    private float mProgressY = 0;
    private float mProgressXOffset = 0;
    private float mProgressYOffset = 0;
    private float mStartY;
    private int mState = PREVIEW_STATE;
    private CaputerPreviewListener mListener;

    public interface CaputerPreviewListener {
        public void setProgressViewPort(float progress);

        public void stopCapture();
    }

    public SmallPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaintBackground.setStyle(Style.FILL);
        mPaintLine.setColor(getResources().getColor(R.color.pano_director_line));
        mPaintLine.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.pano_director_line_front));
        mPanoArrowLeft = BitmapFactory.decodeResource(context.getResources(), R.drawable.pano_arrows_left);
        mPanoArrowRight = BitmapFactory.decodeResource(context.getResources(), R.drawable.pano_arrows_right);
        mPanoArrowGap = context.getResources().getDimensionPixelSize(R.dimen.pano_arrow_gap);
        // stand for left gap only in 270, and for right gap in 90
        mLeftGapLand = context.getResources().getDimensionPixelSize(R.dimen.pano_left_gap_land);
        //it's reverse with mLeftGapLand
        mRightGapLand = context.getResources().getDimensionPixelSize(R.dimen.pano_right_gap_land);
        if (CameraUtil.isNavigationEnable()) {
            mRightGapLand += CameraUtil.getNormalNavigationBarHeight();//SPRD:fix bug1128312
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawBounds != null) {
            if (mState == PREVIEW_STATE) {
                Log.i(TAG, " mDrawBounds = " + mDrawBounds + " mLeftGapLand = " + mLeftGapLand);
                //canvas.drawRect(mDrawBounds, mPaintBackground);//draw back ground
                //draw director line
                canvas.drawLine(mDrawBounds.left, mDrawBounds.bottom - mDrawBounds.height() / 2,
                        mDrawBounds.right, mDrawBounds.bottom - mDrawBounds.height() / 2, mPaintLine);
                if (isCaptureLeft()) {
                    //draw left arrow
                    canvas.drawBitmap(mPanoArrowLeft, mDrawBounds.left + mPanoArrowGap,
                            mDrawBounds.bottom - mDrawBounds.height() / 2 - mPanoArrowGap, null);
                } else {
                    //draw right arrow
                    canvas.drawBitmap(mPanoArrowRight, mDrawBounds.right - mPanoArrowGap * 3,
                            mDrawBounds.bottom - mDrawBounds.height() / 2 - mPanoArrowGap, null);
                }
            } else {
                mCaptureDrawBounds.set(mDrawBounds.left + (isCaptureLeft() ? mProgressX : 0),
                        mDrawBounds.top,
                        isCaptureLeft() ? mDrawBounds.right : mDrawBounds.left + mProgressX,
                        mDrawBounds.bottom);
                if (mCaptureDrawBounds.width() <= mLastCaptureDrawBounds.width()
                        || mLastCaptureDrawBounds.width() == 0) {
                    mLastCaptureDrawBounds.set(mCaptureDrawBounds);
                    //draw director line
                    canvas.drawLine(mCaptureDrawBounds.left,
                            mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2,
                            mCaptureDrawBounds.right,
                            mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2,
                            mPaintLine);
                } else {
                    //draw director line
                    canvas.drawLine(mLastCaptureDrawBounds.left,
                            mLastCaptureDrawBounds.bottom - mLastCaptureDrawBounds.height() / 2,
                            mLastCaptureDrawBounds.right,
                            mLastCaptureDrawBounds.bottom - mLastCaptureDrawBounds.height() / 2,
                            mPaintLine);
                }
                if (isCaptureLeft()) {
                    if (mProgressX <= (mWidth - 3 * mPanoArrowGap)) {
                        //draw left arrow
                        canvas.drawBitmap(mPanoArrowLeft, mCaptureDrawBounds.left + mPanoArrowGap,
                                mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2 - mPanoArrowGap + mProgressY,
                                null);
                    } else {
                        //draw left arrow
                        canvas.drawBitmap(mPanoArrowLeft, mDrawBounds.right - 2 * mPanoArrowGap,
                                mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2 - mPanoArrowGap + mProgressY,
                                null);
                    }
                } else {
                    if (mProgressX >= 3 * mPanoArrowGap) {
                        //draw right arrow
                        canvas.drawBitmap(mPanoArrowRight, mCaptureDrawBounds.right - mPanoArrowGap * 3,
                                mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2 - mPanoArrowGap + mProgressY,
                                null);
                    } else {
                        //draw right arrow
                        canvas.drawBitmap(mPanoArrowRight, mDrawBounds.left,
                                mCaptureDrawBounds.bottom - mCaptureDrawBounds.height() / 2 - mPanoArrowGap + mProgressY,
                                null);
                    }
                }
            }
        }
    }

    public void setBackgroundArea(final RectF previewArea) {
        mDrawBounds = new RectF(previewArea);
        mWidth = mDrawBounds.width();
        mHeight = mDrawBounds.height() / 2;
        invalidate();
    }

    public void setCaptureDirector(int director) {
        mCaptureDirector = director;
        //invalidate();
    }

    public int getCaptureDirector() {
        return mCaptureDirector;
    }

    public boolean isCaptureLeft() {
        return mCaptureDirector == LEFT_TO_RIGHT;
    }

    public int getLeftGap() {
        return mLeftGapLand;
    }

    public int getRightGap() {
        return mRightGapLand;
    }

    public void setMaxProgress(int progress) {
        mMaxProgress = progress;
    }

    public void setProgress(int progressX, int progressY) {
        if (mState == CAPTURE_STATE) {
//            mProgressX = progressX * mWidth / mMaxProgress;
            mProgressX = progressX;
            if (Math.abs(mProgressX) >= mWidth) {
                mListener.stopCapture();
                return;
            }
            mProgressY = progressY /* mHeight / 100*/;
            mProgressY = Math.min(mHeight, Math.abs(Math.max(0, Math.abs(mProgressY))));
            if (progressY < 0) {
                mProgressY *= -1;
            }
            if (Math.abs(mProgressX) >= START_MOVE_ANGLE) {
                if (Math.abs(mLastProgressX) < START_MOVE_ANGLE) {
                    mStartY = mProgressY;
                }
                mProgressYOffset = mProgressY - mStartY;
            }
            mLastProgressX = mProgressX;
            mProgressX += mProgressXOffset;
            mProgressX = Math.min(mWidth, Math.max(0, mProgressX));
//            mListener.setProgressViewPort(mProgressX);//for small preview in capture state
            Log.d(TAG, " progressX = " + progressX + " mProgressX = "
                    + mProgressX + " mProgressYOffset = " + mProgressYOffset + " mStartY = "
                    + mStartY + " mProgressY = " + mProgressY);
            invalidate();
        }
    }

    public void reset() {
        mProgressX = 0;
        mProgressY = 0;
        mLastProgressX = 0;
        if (isCaptureLeft()) {
            mProgressXOffset = 0;
        } else {
            mProgressXOffset = mWidth;
        }
        mProgressYOffset = 0;
        mStartY = 0;
        mLastCaptureDrawBounds.setEmpty();
    }

    public void setCaptureState(int state) {
        mState = state;
    }

    public void setListener(CaputerPreviewListener listener) {
        mListener = listener;
    }

    public void setVisible(boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
    }
}
