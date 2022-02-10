package com.android.camera.widget.selector.interfaces;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.view.View;

public interface InterfaceSelectorItem {
    public class ItemDataStruct{
        public String mKey;
        public int mDesRId;
        public int mPicSelectedId;
        public int mPicUnSelectedId;
        public int mPicDisableId;
        public int mDesSelectedColorId;
        public int mDesUnSelectedColorId;
        public int mDesDisabledColorId;
        public String mValue;

        @SuppressLint("ResourceType")
        public ItemDataStruct(TypedArray type) {
            mKey = type.getString(0);
            mDesRId = type.getResourceId(1, -1);
            mPicSelectedId = type.getResourceId(2, -1);
            mPicUnSelectedId = type.getResourceId(3, -1);
            mPicDisableId = type.getResourceId(4, -1);
            mDesSelectedColorId = type.getResourceId(5, -1);
            mDesUnSelectedColorId = type.getResourceId(6, -1);
            mDesDisabledColorId = type.getResourceId(7, -1);
            mValue = type.getString(8);
        }
    }


    int STATUS_SELECTED = 0;
    int STATUS_UNSELECTED = 1;
    int STATUS_DISABLE = 2;

    void setStruct(ItemDataStruct itemStruct);
    View getView();
    void setStatus(int status);
    int getStatus();
    String getKey();

}
