package com.dream.camera.modules.filter.sprd;

import android.view.View;
import android.graphics.RectF;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.android.camera.util.CameraUtil;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.dream.camera.filter.FilterSurfaceViewInterface;
import com.dream.camera.filter.sprd.SprdGLSurfaceView;
import com.dream.camera.modules.filter.DreamFilterLogicControlInterface;
import com.dream.camera.modules.filter.FilterModuleUIAbs;
import com.android.camera2.R;
import com.dream.camera.filter.ArcsoftSmallAdvancedFilter;

public class FilterModuleUISprd extends FilterModuleUIAbs {
    private static final Tag TAG = new Tag("FilterModuleUISprd");
    private SprdGLSurfaceView mSprdFilterGlView = null;

    public FilterModuleUISprd(CameraActivity activity,
                              PhotoController controller, View parent,
                              DreamFilterLogicControlInterface filterController) {
        super(activity, controller, parent, filterController);

        mSprdFilterGlView = new SprdGLSurfaceView(mActivity, this);
        mSprdFilterGlView.init();
        LayoutParams param = null;
        param = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mPreviewLayout = (FrameLayout) mRootView
                .findViewById(R.id.frame_layout);
        if (mPreviewLayout != null) {
            Log.i(TAG, "mpreviewlayout != null");
            mPreviewLayout.addView(mSprdFilterGlView, param);
        }
        mSprdFilterGlView.setVisibility(View.VISIBLE);

        ViewStub viewStubMagiclensBottom = (ViewStub) mActivity
                .findViewById(R.id.layout_ucam_magiclens_bottom_id);
        if (viewStubMagiclensBottom != null) {
            viewStubMagiclensBottom.inflate();
        }
        mArcsoftSmallAdvancedFilter = new ArcsoftSmallAdvancedFilter(mActivity,
                (ViewGroup) mRootView);
        mArcsoftSmallAdvancedFilter.init((FilterSurfaceViewInterface)mSprdFilterGlView);
        mArcsoftSmallAdvancedFilter.setVisibility(View.VISIBLE);
        mArcsoftSmallAdvancedFilter.setOrientation(mActivity.getCameraAppUI()
                .getNowOrientation());

    }

    @Override
    public void setTransformMatrix() {
        float scaledTextureWidth, scaledTextureHeight;
        if (mAspectRatio == 0) {
            return;
        }
        if (mWidth > mHeight) {
            scaledTextureWidth = Math.min(mWidth,
                    (int) (mHeight * mAspectRatio + 0.5));// SPRD:fix bug601061
            scaledTextureHeight = Math.min(mHeight, (int) (mWidth
                    / mAspectRatio + 0.5));
        } else {
            scaledTextureWidth = Math.min(mWidth,
                    (int) (mHeight / mAspectRatio + 0.5));
            scaledTextureHeight = Math.min(mHeight, (int) (mWidth
                    * mAspectRatio + 0.5));
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSprdFilterGlView
                .getLayoutParams();
        params.width = (int) scaledTextureWidth;
        params.height = (int) scaledTextureHeight;
        RectF rect = mActivity.getCameraAppUI().getPreviewArea();

        // horizontal direction
        if (mWidth > mHeight) {
            params.setMargins((int) rect.left, 0, 0, 0);
        } else {
            params.setMargins(0, (int) rect.top, 0, 0);
        }
        mSprdFilterGlView.setLayoutParams(params);

        int currentDisplayOrientation = CameraUtil.getDisplayRotation();
        if ((mLastDisplayOrientation != 0 && currentDisplayOrientation == 0)) {
            // Set of 'gone then visible' is same as requestLayout(), but
            // requestLayout does not work sometimes.
            // The 30ms delay is needed, or set of 'gone then visible' will be
            // useless.

            if (mSprdFilterGlView.getVisibility() == View.VISIBLE) {
                mSprdFilterGlView.setVisibility(View.GONE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSprdFilterGlView != null) {
                            mSprdFilterGlView.setVisibility(View.VISIBLE);
                        }
                    }
                }, 30);
            }
        }

        mLastDisplayOrientation = currentDisplayOrientation;

        Log.i(TAG, "setTransformMatrix(): width = " + mWidth + " height = "
                + mHeight + " scaledTextureWidth = " + scaledTextureWidth
                + " scaledTextureHeight = " + scaledTextureHeight
                + " mAspectRatio = " + mAspectRatio + " displayOrientation = "
                + currentDisplayOrientation);
    }

    public SprdGLSurfaceView getSprdGLSurfaceView() {
        return mSprdFilterGlView;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        mPaused = false;
        if (mSprdFilterGlView != null) {
            mSprdFilterGlView.onResume();
            mSprdFilterGlView.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSprdFilterGlView != null) {
            mSprdFilterGlView.onPause();
            mSprdFilterGlView.SetPreviewStarted(false);
        }
    }

    public void destroy() {
        super.destroy();
        if (mPaused) {
            if (mSprdFilterGlView != null) {
                mSprdFilterGlView.deinit();
//                /* SPRD:fix bug931117 need post delayed GlSurfaceView @{ */
//                if (mRootView != null) {
//                    mRootView.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
                            if (mPaused && mSprdFilterGlView != null) {
                                mSprdFilterGlView.setVisibility(View.GONE);
                                if (mPreviewLayout != null) {
                                    mPreviewLayout.removeView(mSprdFilterGlView);
                                    mSprdFilterGlView = null;
                                    Log.i(TAG, "mpreviewlayout removeView2 end");
                                }
                            }
//                        }
//                    },50);
//                    /* @} */
//                }
            }
            if (mArcsoftSmallAdvancedFilter != null)
                mArcsoftSmallAdvancedFilter.setVisibility(View.GONE);
        }
    }

    public void setFilterEnable(boolean enable) {
        mArcsoftSmallAdvancedFilter.setFilterUIEnable(enable);
    }
}
