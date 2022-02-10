package com.android.camera.widget.selector;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.camera.widget.selector.interfaces.InterfaceSelectorContainer;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorController;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorScroller;

import java.util.ArrayList;


public abstract class SelectorController implements InterfaceSelectorController {

    public interface InitializeCallBack{
        int getConfigListId();

        int getItemLayoutId();

        void filterItems(ArrayList<InterfaceSelectorItem.ItemDataStruct> itemStructs);

    }

    protected InterfaceSelectorContainer mContainer;
    protected InterfaceSelectorScroller mScroller;
    protected ArrayList<InterfaceSelectorItem.ItemDataStruct> mItemStructs;
    protected Context mContext;
    protected CallBack mCallBack;
    protected InitializeCallBack mInitializeCallBack;

    public SelectorController(Context context, InterfaceSelectorScroller scroller,
                              InterfaceSelectorContainer container, CallBack callBack,
                              InitializeCallBack initializeCallBack ){
        mScroller = scroller;
        mContainer = container;
        mScroller.setContainer(mContainer);
        mContext = context;
        mInitializeCallBack = initializeCallBack;
        mCallBack = callBack;
        initialize();
    }

    private void initialize(){
        if(mInitializeCallBack != null){
            int itemConfigListId = mInitializeCallBack.getConfigListId();
            int itemLayoutId = mInitializeCallBack.getItemLayoutId();

            mItemStructs = generateItemDataStructs(itemConfigListId);

            mInitializeCallBack.filterItems(mItemStructs);

            addItemToContainer(itemLayoutId, mItemStructs);
        }
    }

    protected ArrayList<InterfaceSelectorItem.ItemDataStruct>  generateItemDataStructs(int itemConfigList) {
        ArrayList<InterfaceSelectorItem.ItemDataStruct> itemStructs = new ArrayList<InterfaceSelectorItem.ItemDataStruct>();
        TypedArray types = mContext.getResources().obtainTypedArray(itemConfigList);
        if (types != null) {
            for (int i = 0; i < types.length(); i++) {
                TypedArray type = mContext.getResources().obtainTypedArray(
                        types.getResourceId(i, -1));
                if (type != null) {
                    itemStructs.add(new InterfaceSelectorItem.ItemDataStruct(type));
                    type.recycle();
                }
            }
        }
        types.recycle();

        return itemStructs;
    }

    private void addItemToContainer(int itemLayoutId, ArrayList<InterfaceSelectorItem.ItemDataStruct> itemStructs) {
        // add items
        LayoutInflater inflater = (LayoutInflater)(mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        for(int i = 0; i < itemStructs.size(); i++){
            SelectorItem item = (SelectorItem) inflater.inflate(itemLayoutId,null);
            item.setStruct(itemStructs.get(i));
            item.setStatus(InterfaceSelectorItem.STATUS_UNSELECTED);
            mContainer.addItem(item);
            item.getView().setOnClickListener(mItemOnClickListener);
        }
    }


    private View.OnClickListener mItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SelectorItem item = (SelectorItem)view;
            if(item.getStatus() == InterfaceSelectorItem.STATUS_SELECTED){
                return;
            }

            int index = mContainer.getIndexOfItem(item);
            mContainer.select(index);
            mScroller.smoothMoveTo(index);
            mCallBack.onItemSelectedChange(index, mItemStructs.get(index).mKey, mItemStructs.get(index).mValue);
        }
    };


    @Override
    public void enableFeatureOperation(boolean enable){
        if(enable){
            mContainer.enable();
        } else {
            mContainer.disable();
        }
    }

    @Override
    public void selectItem(int index){
        mContainer.select(index);
        mScroller.smoothMoveTo(index);
    }

    @Override
    public void selectItemByKey(String key) {
        for (int i = 0; i < mItemStructs.size(); i++){
            if(mItemStructs.get(i).mKey.equals(key)){
                selectItem(i);
            }
        }
    }

    @Override
    public void selectItemByValue(String value) {
        for (int i = 0; i < mItemStructs.size(); i++){
            if(mItemStructs.get(i).mValue.equals(value)){
                selectItem(i);
            }
        }
    }

    @Override
    public int getSelectedIndex(){
        return mContainer.getSelectedIndex();
    }

    @Override
    public String getKey(int index){
        return mItemStructs.get(index).mKey;
    }

    @Override
    public String getValue(int index){
        return mItemStructs.get(index).mValue;
    }

    public int getSize(){
        return mContainer.getChildSize();
    }
}
