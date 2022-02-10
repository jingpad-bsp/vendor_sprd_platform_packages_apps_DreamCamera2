package com.android.camera.widget.selector;

import android.content.Context;
import android.view.View;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorScroller;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorSwitcher;

public abstract class SelectorControllerWithSwitcher extends SelectorController {

    protected InterfaceSelectorSwitcher mSwithcer;

    public SelectorControllerWithSwitcher(Context context, InterfaceSelectorScroller scroller,
                                          InterfaceSelectorContainer container,
                                          InterfaceSelectorSwitcher switcher,
                                          CallBack callBack,
                                          InitializeCallBack initializeCallBack){
        super(context, scroller,container, callBack, initializeCallBack);
        mSwithcer = switcher;
        initializeSwitcher();

    }

    private void initializeSwitcher() {
        int switchId = getSwitcherID();
        mSwithcer.setStruct(new InterfaceSelectorSwitcher.SwitcherDataStruct(
                mContext.getResources().obtainTypedArray(switchId)));
        mSwithcer.setOpreationEnable(true);
        mSwithcer.getView().setOnClickListener(mOnClickListener);
    }

    protected abstract int getSwitcherID();

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean nextStatus = !mSwithcer.getOperationEnable();
            enableFeatureOperation(nextStatus);
            mCallBack.onFeatureOperationEnableChange(nextStatus);
        }
    };

    @Override
    public void enableFeatureOperation(boolean enable){
        mSwithcer.setOpreationEnable(enable);
        super.enableFeatureOperation(enable);
    }

}
