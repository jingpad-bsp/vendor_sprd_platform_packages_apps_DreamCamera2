package com.android.camera.widget.selector.lightPortrait;

import android.content.Context;

import com.android.camera.widget.selector.SelectorController;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorScroller;
import com.android.camera2.R;
import java.util.ArrayList;

public class LightPortraitController extends SelectorController {



    public LightPortraitController(Context context, InterfaceSelectorScroller scroller,
                                   InterfaceSelectorContainer container, CallBack callBack){
        super(context, scroller, container, callBack, new SelectorController.InitializeCallBack(){

            @Override
            public int getConfigListId() {
                return R.array.light_portrait_mode_list;
            }

            @Override
            public int getItemLayoutId() {
                return R.layout.layout_selector_item;
            }

            @Override
            public void filterItems(ArrayList<InterfaceSelectorItem.ItemDataStruct> itemStructs) {

            }
        });
    }

}
