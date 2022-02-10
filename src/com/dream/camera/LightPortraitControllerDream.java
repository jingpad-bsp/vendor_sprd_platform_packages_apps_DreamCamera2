package com.dream.camera;

import android.app.Activity;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.settings.Keys;
import com.android.camera.widget.selector.SelectorContainer;
import com.android.camera.widget.selector.SelectorScroller;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorController;
import com.android.camera.widget.selector.lightPortrait.LightPortraitController;
import com.android.camera2.R;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;

public class LightPortraitControllerDream implements InterfaceSelectorController.CallBack {

    public interface LightPortraitListener {

        void setLightPortraitController(LightPortraitControllerDream controller);

        void onLightPortraitEnable(boolean enable);

    }

    public static final String CLOSE_VALUE = "0";
    public static final String CLASSIC_VALUE = "6";
    public static final String STAGE_VALUE = "5";

    Activity mActivity;
    private DataModuleBasic mCurrentDataModule;
    LightPortraitListener mListener;
    LightPortraitController mLightPortraitController;
    SelectorScroller mLightPortraitScroller;
    SelectorContainer mLightPortraitContainer;

    public LightPortraitControllerDream(View extendPanelParent, LightPortraitListener listener, CameraActivity activity) {
        mActivity = activity;
        mCurrentDataModule = DataModuleManager.getInstance(mActivity).getCurrentDataModule();
        mListener = listener;
        if (extendPanelParent != null) {
            mLightPortraitScroller = extendPanelParent.findViewById(R.id.light_portrait_scroller);
            mLightPortraitContainer = extendPanelParent.findViewById(R.id.light_portrait_container);
            mLightPortraitController = new LightPortraitController(mActivity,
                    mLightPortraitScroller,mLightPortraitContainer,this);
        }

        mListener.setLightPortraitController(this);
    }

    public void updateLightPortraitDisplay(boolean visible) {
        mLightPortraitScroller.setVisibility(visible?View.VISIBLE:View.GONE);
        if(visible){
            mLightPortraitController.selectItemByValue(mCurrentDataModule.getString(Keys.KEY_LIGHT_PORTIAIT));
        }
    }


    @Override
    public void onFeatureOperationEnableChange(boolean enable) {
//        mListener.onLightPortraitEnable(enable);
    }

    @Override
    public void onItemSelectedChange(int index, String key, String value) {
        String preValue = mCurrentDataModule.getString(Keys.KEY_LIGHT_PORTIAIT);
//        if(CLOSE_VALUE.equals(value)){
//            onFeatureOperationEnableChange(false);
//        } else if(CLOSE_VALUE.equals(preValue)){
//            onFeatureOperationEnableChange(true);
//        }
        mCurrentDataModule.changeSettings(Keys.KEY_LIGHT_PORTIAIT,value);
    }

    public void setEnable(boolean enable) {
        mLightPortraitScroller.setEnabled(enable);
    }
}
