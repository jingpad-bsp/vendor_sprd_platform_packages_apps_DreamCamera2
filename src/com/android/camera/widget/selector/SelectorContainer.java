package com.android.camera.widget.selector;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;

import com.android.camera2.R;

public class SelectorContainer extends LinearLayout implements InterfaceSelectorContainer {

    private static int DISPLAY_CHILD_COUNT = 4;
    private int mDisplayCount = DISPLAY_CHILD_COUNT;

    private int mCurrentSelectedIndex = -1;

    public SelectorContainer(Context context) {
        super(context);
        initAttr(context,null);
    }

    public SelectorContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context,attrs);
    }

    public SelectorContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context,attrs);
    }

    public SelectorContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context,attrs);
    }

    private void initAttr(Context context, AttributeSet attrs){
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelectorContainer);
            mDisplayCount = typedArray.getInt(R.styleable.SelectorContainer_display_count, mDisplayCount);
            typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int childCount = getChildCount();
        final int usedWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();

        int childWidth = usedWidth;
        if(childCount <= mDisplayCount){
            childWidth = usedWidth/childCount;
        } else {
            childWidth = usedWidth / (mDisplayCount * 2 + 1) * 2;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            int widthMeasureSpc = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
            child.measure(widthMeasureSpc,child.getMeasuredHeightAndState());
        }

        setMeasuredDimension(MeasureSpec.makeMeasureSpec(childWidth * childCount, MeasureSpec.EXACTLY),getMeasuredHeight());


//        //calculate the child's width&height
//        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.UNSPECIFIED),heightMeasureSpec);
//
//        int childCount = getChildCount();
//        int usedWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
//
//        if(childCount == 0){
//            return;
//        }
//
//        if(childCount <= mDisplayCount){
//            // calculate the total width of the childs
//            int totalChildsWidth = 0;
//            for(int i = 0; i < childCount; i++){
//                View child = getChildAt(i);
//                totalChildsWidth += child.getMeasuredWidth();
//            }
//
//            // calculate the left&right margin
//            mLeftAndRightMargin = ( usedWidth - totalChildsWidth) / (childCount * 2);
//        } else {
//
//            // calculate the total width of the childs
//            int totalChildsWidth = 0;
//            for(int i = 0; i < mDisplayCount; i++){
//                View child = getChildAt(i);
//                totalChildsWidth += child.getMeasuredWidth();
//            }
//            totalChildsWidth += getChildAt(mDisplayCount).getMeasuredWidth() / 2;
//
//            // calculate the left&right margin
//            mLeftAndRightMargin = (usedWidth - totalChildsWidth)
//                    / (mDisplayCount * 2 + 1);
//        }
//
//        int totalUsedWidth = 0;
//        for (int i = 0; i < childCount; i++) {
//            final View child = getChildAt(i);
//            int itemWidth = mLeftAndRightMargin * 2 + child.getMeasuredWidth();
//            int widthMeasureSpc = MeasureSpec.makeMeasureSpec(itemWidth,MeasureSpec.EXACTLY);
//            child.measure(widthMeasureSpc,child.getMeasuredHeightAndState());
//            totalUsedWidth += itemWidth;
//        }
//
//        setMeasuredDimension(MeasureSpec.makeMeasureSpec(totalUsedWidth,MeasureSpec.EXACTLY),getMeasuredHeight());
    }

    @Override
    public void addItem(InterfaceSelectorItem item) {
        addView(item.getView());
    }

    @Override
    public void enable() {
        for(int i = 0; i < getChildCount(); i++){
            SelectorItem item = (SelectorItem)(getChildAt(i));
            item.setEnabled(true);
            item.setStatus(InterfaceSelectorItem.STATUS_UNSELECTED);
        }
    }

    @Override
    public void disable() {
        for(int i = 0; i < getChildCount(); i++){
            SelectorItem item = (SelectorItem)(getChildAt(i));
            item.setEnabled(false);
            item.setStatus(InterfaceSelectorItem.STATUS_DISABLE);
        }
    }

    @Override
    public void select(int index) {
        for(int i = 0; i < getChildCount(); i++){
            SelectorItem item = (SelectorItem)(getChildAt(i));
            if( index != i){
                item.setStatus(InterfaceSelectorItem.STATUS_UNSELECTED);
            } else {
                item.setStatus(InterfaceSelectorItem.STATUS_SELECTED);
                mCurrentSelectedIndex = i;
            }
        }
    }

    @Override
    public int getItemLeft(int index) {
        return getChildAt(index).getLeft();
    }

    @Override
    public int getItemRight(int index) {
        return getChildAt(index).getRight();
    }

    @Override
    public int getItemWidth(int index) {
        return getChildAt(index).getWidth();
    }

    @Override
    public int getIndexOfItem(InterfaceSelectorItem item) {
        return indexOfChild(item.getView());
    }

    @Override
    public int childCount() {
        return getChildCount();
    }

    @Override
    public int getContainerWidth() {
        return getWidth();
    }
    @Override
    public int getSelectedIndex(){
        return mCurrentSelectedIndex;
    }

    @Override
    public int getChildSize(){
        return getChildCount();
    }
}
