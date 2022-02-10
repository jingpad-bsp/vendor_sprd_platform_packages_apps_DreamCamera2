package com.dream.camera.ui;

import android.content.Context;
import android.os.Handler;
import com.android.internal.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class AutoCenterHorizontalScrollView extends HorizontalScrollView implements
                View.OnClickListener, View.OnTouchListener{

    private Handler handler;
    private static String tag = "AutoCenterHorizontalScrollView";
    private HAdapter mAdapter;
    List<RecyclerView.ViewHolder> mViewHolders = new ArrayList<>();

    private int mHVSCurrentIndex = 0;
    private long delayMillis = 100;

    private int mPaddingLeft = 0;
    private int mPaddingRight = 0;
    private float mTouchDownX;
    /**
     * lastScrollLeft, X
     */
    private long lastScrollLeft = -1;
    private long nowScrollLeft = -1;
    private int offset_target;
    private int offset_current;
    private int offset_child;

    public boolean isScrolling = false;

    private boolean isScrollable = true;

    public AutoCenterHorizontalScrollView(Context context) {
        super(context);
        //init();
    }

    public AutoCenterHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //init();
    }

    public AutoCenterHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //init();
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        //init();
    }

    public static interface HAdapter {
        int getCount();
        RecyclerView.ViewHolder getItemView(int position);
        void onSelectStateChanged(RecyclerView.ViewHolder itemView,int position,boolean isSelected);
    }


    private Runnable mScrollerTask = new Runnable() {
        @Override
        public void run() {
            if ((nowScrollLeft == lastScrollLeft)) {
                lastScrollLeft = nowScrollLeft;
                nowScrollLeft = -1;
                int index = getCurrentIndex();
                if(index == 0){
                    smoothScrollTo(offset_target + offset_child / 2, 0);
                }
                if (offset_target != offset_current) {
                    smoothScrollTo(offset_target + offset_child / 2, 0);
                }
            } else {
                lastScrollLeft = nowScrollLeft;
                postDelayed(this, delayMillis);
            }
        }
    };

    public void setAdapter(final HAdapter adapter) {
        if(adapter == null || adapter.getCount() == 0){
            return;
        }
        this.mAdapter = adapter;
        mViewHolders.clear();
        removeAllViews();
        LinearLayout linearLayout = new LinearLayout(getContext());
        for (int i = 0; i < adapter.getCount(); i++) {
            mViewHolders.add(adapter.getItemView(i));
            LinearLayout linearLayout1 = new LinearLayout(getContext());
            linearLayout1.addView(mViewHolders.get(i).itemView);
            linearLayout1.setTag(i);
            linearLayout1.setOnClickListener(AutoCenterHorizontalScrollView.this);
            linearLayout.addView(linearLayout1);
        }
        addView(linearLayout);
        init();
    }


    @Override
    public void onClick(View view) {
        if (isScrollable) {
            int index = (int) view.getTag();
            smoothScrollTo(getChildCenterPosition(index) + offset_child / 2, 0);
        }
    }

    public void setScrollable(boolean isScrollabled) {
        isScrollable = isScrollabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isScrollable) {
            return super.onTouchEvent(event);
        } else {
            return true;
        }
    }

    /**
     * get item X
     *
     * @param index
     * @return
     */
    private int getChildCenterPosition(int index) {
        offset_current = super.computeHorizontalScrollOffset();
        if (getChildCount() <= 0) {
            return 0;
        }
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        if (viewGroup == null || viewGroup.getChildCount() == 0) {
            return 0;
        }
        int offset_tmp = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            int child_width = child.getWidth();
            offset_tmp = offset_tmp + child_width;
            if (i == index) {
                offset_target = offset_tmp - child_width / 2 - viewGroup.getChildAt(0).getWidth() / 2;
                setCurrent(i);
                return offset_target;
            }
        }
        return 0;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            isScrolling = true;
            mTouchDownX = motionEvent.getX();
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            isScrolling = true;
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            isScrolling = false;
            if (mTouchDownX != motionEvent.getX()) {
                mTouchDownX = motionEvent.getX();
                handler.removeCallbacks(mScrollerTask);
                handler.postDelayed(mScrollerTask, delayMillis);
            }
        }
        return false;
    }

    private void init() {
        this.setOnTouchListener(AutoCenterHorizontalScrollView.this);

        if (getChildCount() <= 0) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        if (viewGroup == null || viewGroup.getChildCount() == 0) {
            return;
        }
        //set padding, the first itemview and the last auto center
        View first = viewGroup.getChildAt(0);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        first.measure(w, h);
        int first_width = first.getMeasuredWidth();
        View last = viewGroup.getChildAt(viewGroup.getChildCount() - 1);
        last.measure(w, h);
        int last_width = last.getMeasuredWidth();
        //mPaddingLeft = getScreenWidth(getContext()) / 2 - first_width /2;
        mPaddingLeft = (getScreenWidth(getContext()) - first_width * viewGroup.getChildCount()) / 2;
        //mPaddingRight = getScreenWidth(getContext()) / 2 - last_width / 2;
        mPaddingRight = mPaddingLeft;

        setPadding(mPaddingLeft, getPaddingTop(), mPaddingRight, getBottom());
        setCurrentIndex(mHVSCurrentIndex);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        nowScrollLeft = l;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange() + mPaddingLeft + mPaddingRight;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset() + mPaddingLeft;
    }

    private void setCurrent(int currentIndex) {
        if (mAdapter != null && mAdapter.getCount()>0 && currentIndex < mAdapter.getCount()) {
            mAdapter.onSelectStateChanged(mViewHolders.get(mHVSCurrentIndex),mHVSCurrentIndex,false);
        }
        this.mHVSCurrentIndex = currentIndex;
        if (mAdapter != null && mAdapter.getCount()>0 && currentIndex < mAdapter.getCount()) {
            mAdapter.onSelectStateChanged(mViewHolders.get(currentIndex),currentIndex,true);
            if(onSelectChangeListener!=null){
                onSelectChangeListener.onSelectChange(currentIndex);
            }
        }
    }

    public void setCurrentIndex(int currentIndex) {
        setCurrent(currentIndex);
        if (getChildCount() <= 0) {
            return ;
        }
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        if (viewGroup == null || viewGroup.getChildCount() == 0) {
            return ;
        }
        int tmpOffset = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            child.measure(w, h);
            int child_width = child.getMeasuredWidth();
            offset_child = child_width;
            tmpOffset = tmpOffset + child_width;
            if (i == currentIndex) {
                View child0 = viewGroup.getChildAt(0);
                child0.measure(w, h);
                int child_width0 = child0.getMeasuredWidth();
                offset_target = tmpOffset - child_width / 2 - child_width0 / 2;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        smoothScrollTo(offset_target + offset_child / 2, 0);
                    }
                });
                return;
            }
        }
    }

    public int getCurrentIndex() {
        offset_current = super.computeHorizontalScrollOffset();
        if (getChildCount() <= 0) {
            return 0;
        }
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        if (viewGroup == null || viewGroup.getChildCount() == 0) {
            return 0;
        }
        int offset_tmp = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            int child_width = child.getWidth();

            offset_tmp = offset_tmp + child_width;
            if (offset_tmp > offset_current) {
                offset_target = offset_tmp - child_width / 2 - viewGroup.getChildAt(0).getWidth() / 2;
                setCurrent(i);
                break;
            }
        }
        return mHVSCurrentIndex;
    }

    public OnSelectChangeListener onSelectChangeListener;

    public void setOnSelectChangeListener(OnSelectChangeListener onSelectChangeListener) {
        this.onSelectChangeListener = onSelectChangeListener;
        setCurrent(mHVSCurrentIndex);
    }

    public static interface OnSelectChangeListener {
        void onSelectChange( int position);
    }

    public static int getScreenWidth(Context context) {
        return getDisplayMetrics(context).widthPixels;
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        return context.getResources().getDisplayMetrics();
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }
}
