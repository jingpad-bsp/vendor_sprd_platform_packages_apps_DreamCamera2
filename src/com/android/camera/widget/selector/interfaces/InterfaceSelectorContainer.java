package com.android.camera.widget.selector.interfaces;

public interface InterfaceSelectorContainer {

    void addItem(InterfaceSelectorItem item);
    void enable();
    void disable();
    void select(int index);
    int getItemLeft(int index);
    int getItemRight(int index);
    int getItemWidth(int width);
    int getIndexOfItem(InterfaceSelectorItem item);
    int childCount();
    int getContainerWidth();

    int getSelectedIndex();

    int getChildSize();
}
