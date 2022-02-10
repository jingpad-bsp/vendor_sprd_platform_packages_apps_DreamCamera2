package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.BitmapFactory;
import com.android.camera2.R;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.VectorDrawable;
import android.util.Log;

/**
 * GridLines is a view which directly overlays the preview and draws evenly spaced grid lines.
 */
public class GoldenGridLines extends View
        implements PreviewStatusListener.PreviewAreaChangedListener {

    private RectF mDrawBounds;
    private Rect mDrawBoundsDst = new Rect();
    private RectF mDrawBoundsSrc = new RectF();
    private VectorDrawable mGoldenDrawable;
    private static float mGoldenHeight = 583.0F;
    private static float mGoldenWidth = 360.0F;

    public GoldenGridLines(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGoldenDrawable = (VectorDrawable) context.getResources().getDrawable(R.drawable.helix_g);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawBounds != null) {
            float ratio = mDrawBounds.height() / mDrawBounds.width();
            if (Math.abs(ratio - 4.0 / 3.0) < Math.abs(ratio - 16.0 / 9.0)) {
                // 4:3, fit height
                float glayoutWidth = (mDrawBounds.height() / mGoldenHeight) * mGoldenWidth;
                float glayoutLeft = (mDrawBounds.width() - glayoutWidth) / 2;
                float glayoutRight = glayoutLeft + glayoutWidth;
                mDrawBoundsSrc.set(glayoutLeft, mDrawBounds.top, glayoutRight, mDrawBounds.bottom);
            } else {
                // 16:9, fit width
                float glayoutHeight = (mDrawBounds.width() / mGoldenWidth) * mGoldenHeight;
                float glayoutTop =  Math.max(mDrawBounds.top,(mDrawBounds.height() - glayoutHeight) / 2);
                float glayoutBottom = glayoutTop + glayoutHeight;
                mDrawBoundsSrc.set(mDrawBounds.left, glayoutTop, mDrawBounds.right, glayoutBottom);
            }
            mDrawBoundsSrc.round(mDrawBoundsDst);
            mGoldenDrawable.setBounds(mDrawBoundsDst);
            mGoldenDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            mGoldenDrawable.draw(canvas);
        }
    }

    @Override
    public void onPreviewAreaChanged(final RectF previewArea) {
        setDrawBounds(previewArea);
    }

    private void setDrawBounds(final RectF previewArea) {
        mDrawBounds = new RectF(previewArea);
        invalidate();
    }
}
