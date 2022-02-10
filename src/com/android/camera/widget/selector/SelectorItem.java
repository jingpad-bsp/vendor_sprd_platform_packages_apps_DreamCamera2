package com.android.camera.widget.selector;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;

public class SelectorItem extends LinearLayout implements InterfaceSelectorItem {

    private ImageView mPic;
    private TextView mDes;
    private int mStatus;
    private ItemDataStruct mItemDataStruct;

    public SelectorItem(Context context) {
        super(context);
    }

    public SelectorItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectorItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectorItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPic = (ImageView) getChildAt(0);
        mDes = (TextView) getChildAt(1);
    }

    @Override
    public void setStruct(ItemDataStruct itemStruct) {
        mItemDataStruct = itemStruct;
        mDes.setText(mItemDataStruct.mDesRId);

    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void setStatus(int status) {
        switch (status){
            case STATUS_SELECTED:
                mPic.setImageResource(mItemDataStruct.mPicSelectedId);
                mDes.setTextColor(getResources().getColor(mItemDataStruct.mDesSelectedColorId));
                break;
            case STATUS_UNSELECTED:
                mPic.setImageResource(mItemDataStruct.mPicUnSelectedId);
                mDes.setTextColor(getResources().getColor(mItemDataStruct.mDesUnSelectedColorId));
                break;
            case STATUS_DISABLE:
                mPic.setImageResource(mItemDataStruct.mPicDisableId);
                mDes.setTextColor(getResources().getColor(mItemDataStruct.mDesDisabledColorId));
                break;
        }
        mStatus = status;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public String getKey() {
        return mItemDataStruct.mKey;
    }
}
