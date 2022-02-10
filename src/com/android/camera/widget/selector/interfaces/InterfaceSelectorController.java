package com.android.camera.widget.selector.interfaces;

public interface InterfaceSelectorController {

    public interface CallBack{
        void onFeatureOperationEnableChange(boolean enable);
        void onItemSelectedChange(int index, String key, String value);
    }

    void enableFeatureOperation(boolean enable);
    void selectItem(int index);
    void selectItemByKey(String key);
    void selectItemByValue(String value);
    int getSelectedIndex();
    String getKey(int index);
    String getValue(int index);
}
