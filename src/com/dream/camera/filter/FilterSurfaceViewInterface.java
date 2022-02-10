package com.dream.camera.filter;

import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.ArrayList;

/**
 * Created by SPREADTRUM\ying.sun on 18-1-4.
 */

public interface FilterSurfaceViewInterface {
    void setFilterType(int filterType);
    void initFiltersTable(SparseArray<int[]> tables, int index);
    void initEffectTypes(SparseIntArray effectTypes, int startIndex);
    void initRes(ArrayList<String> textName, ArrayList<Integer> mFilterImage, ArrayList<Integer> mFilterSelectedImage, boolean SupRealPreviewThum);
    void SetPreviewStarted(boolean bStart);//SRPD:fix bug836183
}
