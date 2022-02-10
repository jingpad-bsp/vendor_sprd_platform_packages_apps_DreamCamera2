package com.android.camera.widget.selector.filter;

import android.content.Context;

import com.android.camera.util.CameraUtil;
import com.android.camera.widget.selector.SelectorControllerWithSwitcher;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorScroller;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorSwitcher;

import java.util.ArrayList;

import com.android.camera2.R;

public class MakeUpControllerImpl extends SelectorControllerWithSwitcher {

    public MakeUpControllerImpl(Context context, InterfaceSelectorScroller scroller,
                                InterfaceSelectorContainer container,
                                InterfaceSelectorSwitcher switcher,
                                CallBack callBack,
                                InitializeCallBack initializeCallBack){
        super(context,scroller,container, switcher, callBack, initializeCallBack);
    }

    @Override
    protected int getSwitcherID() {
        return R.array.switcher_makeup;
    }

}
