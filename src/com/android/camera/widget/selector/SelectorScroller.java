package com.android.camera.widget.selector;

import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLogTags;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorScroller;

public class SelectorScroller extends HorizontalScrollView implements InterfaceSelectorScroller {
    private static final String TAG = "SelectorScroller";

    InterfaceSelectorContainer mContainer;
    public SelectorScroller(Context context) {
        super(context);
    }

    public SelectorScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectorScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectorScroller(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setContainer(InterfaceSelectorContainer container) {
        mContainer = container;
    }

    private int mindex = 0;

    @Override
    public void smoothMoveTo(int index) {
        mindex = index;

        if(mContainer == null){
            return;
        }

        if (index == mContainer.childCount() - 1) {
            smoothScrollTo(mContainer.getContainerWidth() - getWidth(), 0);
        } else if (index == 0) {
            smoothScrollTo(0, 0);
        } else {
            int currentScrollx = getScrollX();
            int currentItemLeft = mContainer.getItemLeft(index);
            int preItemLeft = mContainer.getItemLeft(index - 1);

            if (currentItemLeft - currentScrollx < currentItemLeft - preItemLeft) {
                smoothScrollTo(preItemLeft, 0);
            } else if (currentItemLeft - currentScrollx > mContainer.getItemWidth(index)) {
                int nextRight = mContainer.getItemRight(index + 1);
                smoothScrollTo(nextRight - getWidth(), 0);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        smoothMoveTo(mindex);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !isEnabled()?true:super.onInterceptTouchEvent(ev);
    }
}
