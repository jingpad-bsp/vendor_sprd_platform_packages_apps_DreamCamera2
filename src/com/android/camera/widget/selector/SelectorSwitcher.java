package com.android.camera.widget.selector;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorSwitcher;

public class SelectorSwitcher extends LinearLayout implements InterfaceSelectorSwitcher {

    private ImageView mPic;
    private TextView mDes;
    private SwitcherDataStruct mSwitcherDataStruct;
    private boolean selected = false;

    public SelectorSwitcher(Context context) {
        super(context);
    }

    public SelectorSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectorSwitcher(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectorSwitcher(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPic = (ImageView) getChildAt(0);
        mDes = (TextView) getChildAt(1);
    }


    @Override
    public void setStruct(SwitcherDataStruct itemStruct) {
        mSwitcherDataStruct = itemStruct;
    }

    @Override
    public void setOpreationEnable(boolean enable) {
        selected = enable;
        if(enable){
            mPic.setImageResource(mSwitcherDataStruct.mPicSelectedId);
            mDes.setTextColor(getResources().getColor(mSwitcherDataStruct.mDesSelectedColorId));
            mDes.setText(getResources().getText(mSwitcherDataStruct.mDesSelectedId));
        } else {
            mPic.setImageResource(mSwitcherDataStruct.mPicUnSelectedId);
            mDes.setTextColor(getResources().getColor(mSwitcherDataStruct.mDesUnSelectedColorId));
            mDes.setText(getResources().getText(mSwitcherDataStruct.mDesUnSelectedId));
        }
    }

    @Override
    public boolean getOperationEnable() {
        return selected;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !isEnabled()?true:super.onInterceptTouchEvent(ev);
    }
}
