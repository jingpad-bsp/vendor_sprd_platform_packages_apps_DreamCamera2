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

package com.android.camera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.camera.CameraActivity;
import com.android.camera.data.FilmstripItem;
import com.android.camera.data.FilmstripItem.VideoClickedCallback;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera.filmstrip.FilmstripDataAdapter;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class FilmstripView extends ViewGroup {
    /**
     * An action callback to be used for actions on the local media data items.
     */
    public static class PlayVideoIntent implements VideoClickedCallback {
        private final WeakReference<CameraActivity> mActivity;

        /**
         * The given activity is used to start intents. It is wrapped in a weak
         * reference to prevent leaks.
         */
        public PlayVideoIntent(CameraActivity activity) {
            mActivity = new WeakReference<CameraActivity>(activity);
        }

        /**
         * Fires an intent to play the video with the given URI and title.
         */
        @Override
        public void playVideo(Uri uri, String title) {
            CameraActivity activity = mActivity.get();
            if (activity != null) {
              CameraUtil.playVideo(activity, uri, title);
            }
        }
    }


    private static final Log.Tag TAG = new Log.Tag("FilmstripView");

    private CameraActivity mActivity;
    private VideoClickedCallback mVideoClickedCallback;
    private FilmstripDataAdapter mDataAdapter;

    private FilmstripControllerImpl mController;
    private ViewItem mViewItem = null;

    private FilmstripController.FilmstripListener mListener;

    private final SparseArray<Queue<View>> recycledViews = new SparseArray<>();

    /**
     * A helper class to tract and calculate the view coordination.
     */
    private static class ViewItem {

        private final FilmstripView mFilmstrip;
        private final View mView;

        private int mIndex;
        /** The position of the left of the view in the whole filmstrip. */
        private int mLeftPosition;
        private FilmstripItem mData;

        /**
         * Constructor.
         *
         * @param index The index of the data from
         *            {@link com.android.camera.filmstrip.FilmstripDataAdapter}.
         * @param v The {@code View} representing the data.
         */
        public ViewItem(int index, View v, FilmstripItem data, FilmstripView filmstrip) {
            mFilmstrip = filmstrip;
            mView = v;

            mIndex = index;
            mData = data;
            mLeftPosition = -1;
        }

        public FilmstripItem getData() {
            return mData;
        }

        public void setData(FilmstripItem item) {
            mData = item;
        }

        /**
         * Returns the index from
         * {@link com.android.camera.filmstrip.FilmstripDataAdapter}.
         */
        public int getAdapterIndex() {
            return mIndex;
        }

        /**
         * Sets the index used in the
         * {@link com.android.camera.filmstrip.FilmstripDataAdapter}.
         */
        public void setIndex(int index) {
            mIndex = index;
        }

        /** Forwarding of {@link android.view.View#getMeasuredWidth()}. */
        public int getMeasuredWidth() {
            return mView.getMeasuredWidth();
        }

        /**
         * Forwarding of {@link android.view.View#getHitRect(android.graphics.Rect)}.
         */
        public void getHitRect(Rect rect) {
            mView.getHitRect(rect);
        }

        public int getCenterX() {
            return mLeftPosition + mView.getMeasuredWidth() / 2;
        }

        /** Forwarding of {@link android.view.View#getVisibility()}. */
        public int getVisibility() {
            return mView.getVisibility();
        }

        /** Forwarding of {@link android.view.View#setVisibility(int)}. */
        public void setVisibility(int visibility) {
            mView.setVisibility(visibility);
        }

        /**
         * Removes from the hierarchy.
         */
        public void removeViewFromHierarchy() {
            mFilmstrip.removeView(mView);
            mData.recycle(mView);
        }

        /**
         * Brings the view to front by
         * {@link #bringChildToFront(android.view.View)}
         */
        public void bringViewToFront() {
            mFilmstrip.bringChildToFront(mView);
        }

        private View getView() {
            return mView;
        }
    }

    /** Constructor. */
    public FilmstripView(Context context) {
        super(context);
        init((CameraActivity) context);
    }

    /** Constructor. */
    public FilmstripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init((CameraActivity) context);
    }

    /** Constructor. */
    public FilmstripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init((CameraActivity) context);
    }

    private void init(CameraActivity cameraActivity) {
        setWillNotDraw(false);
        mActivity = cameraActivity;
        mVideoClickedCallback = new PlayVideoIntent(mActivity);
        mController = new FilmstripControllerImpl();
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    private View getRecycledView(int index) {
        final int viewType = mDataAdapter.getItemViewType(index);
        Queue<View> recycledViewsForType = recycledViews.get(viewType);
        View view = null;
        if (recycledViewsForType != null) {
            view = recycledViewsForType.poll();
        }
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        Log.v(TAG, "getRecycledView, recycled=" + (view != null));
        return view;
    }

    /**
     * Returns the controller.
     *
     * @return The {@code Controller}.
     */
    public FilmstripController getController() {
        return mController;
    }

    public FilmstripItem getCurrentData() {
        return mViewItem.getData();
    }

    private void setListener(FilmstripController.FilmstripListener l) {
        mListener = l;
    }

    private ViewItem buildViewItemAt(int index) {
        if (mActivity.isDestroyed()) {
            // Loading item data is call from multiple AsyncTasks and the
            // activity may be finished when buildViewItemAt is called.
            Log.d(TAG, "Activity destroyed, don't load data");
            return null;
        }
        FilmstripItem data = mDataAdapter.getFilmstripItemAt(index);
        if (data == null) {
            return null;
        }

        // Always scale by fixed filmstrip scale, since we only show items when
        // in filmstrip. Preloading images with a different scale and bounds
        // interferes with caching.

        View recycled = getRecycledView(index);
        View v = mDataAdapter.getView(recycled, index, mVideoClickedCallback);
        if (v == null) {
            return null;
        }
        ViewItem item = new ViewItem(index, v, data, this);
        return item;
    }

    /**
     * Returns the index of the current item, or -1 if there is no data.
     */
    private int getCurrentItemAdapterIndex() {
        return (mViewItem == null) ? -1 : mViewItem.getAdapterIndex();
    }

    @Override
    public void onDraw(Canvas c) {
        // TODO: remove layoutViewItems() here.
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    private void updateInsertion(int index) {
        ViewItem viewItem = buildViewItemAt(index);
        if (viewItem == null) {
            Log.w(TAG, "unable to build inserted item from data");
            return;
        }

        mViewItem = viewItem;
    }

    private void setDataAdapter(FilmstripDataAdapter adapter) {
        mDataAdapter = adapter;
        mDataAdapter.setListener(new FilmstripDataAdapter.Listener() {
            @Override
            public void onFilmstripItemLoaded() {
                reload();
            }

            @Override
            public void onFilmstripItemUpdated(FilmstripDataAdapter.UpdateReporter reporter) {
                update(reporter);
            }

            @Override
            public void onFilmstripItemInserted(int index, FilmstripItem item) {
                if (mViewItem == null) {
                    // empty now, simply do a reload.
                    reload();
                } else {
                    updateInsertion(index);
                }
                if (mListener != null) {
                    mListener.onDataFocusChanged(index, getCurrentItemAdapterIndex());
                }
            }

            @Override
            public void onFilmstripItemRemoved(int index, FilmstripItem item) {
                reload();
            }
        });
    }

    /** Some of the data is changed. */
    private void update(FilmstripDataAdapter.UpdateReporter reporter) {
        // No data yet.
        if(mViewItem == null) {
            reload();
            return;
        }
    }

    /**
     * The whole data might be totally different. Flush all and load from the
     * start. Filmstrip will be centered on the first item, i.e. the camera
     * preview.
     */
    private void reload() {
        mViewItem = buildViewItemAt(0);
    }

    /**
     * MyController controls all the geometry animations. It passively tells the
     * geometry information on demand.
     */
    private class FilmstripControllerImpl implements FilmstripController {

        @Override
        public int getCurrentAdapterIndex() {
            return FilmstripView.this.getCurrentItemAdapterIndex();
        }

        @Override
        public void setDataAdapter(FilmstripDataAdapter adapter) {
            FilmstripView.this.setDataAdapter(adapter);
        }

        @Override
        public void setListener(FilmstripListener listener) {
            FilmstripView.this.setListener(listener);
        }
    }
}
