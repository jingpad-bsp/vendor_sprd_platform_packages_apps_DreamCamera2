package com.android.camera.widget.selector.interfaces;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.view.View;

public interface InterfaceSelectorSwitcher {

    public class SwitcherDataStruct{
//        public String mKey;
        public int mPicSelectedId;
        public int mPicUnSelectedId;
        public int mDesSelectedColorId;
        public int mDesUnSelectedColorId;
        public int mDesSelectedId;
        public int mDesUnSelectedId;

        @SuppressLint("ResourceType")
        public SwitcherDataStruct(TypedArray type) {
//            mKey = type.getString(0);
            mPicSelectedId = type.getResourceId(1, -1);
            mPicUnSelectedId = type.getResourceId(2, -1);
            mDesSelectedColorId = type.getResourceId(3, -1);
            mDesUnSelectedColorId = type.getResourceId(4, -1);
            mDesSelectedId = type.getResourceId(5, -1);
            mDesUnSelectedId = type.getResourceId(6, -1);
            type.recycle();
        }
    }

    void setStruct(SwitcherDataStruct itemStruct);
    void setOpreationEnable(boolean enable);
    boolean getOperationEnable();
    View getView();
}
