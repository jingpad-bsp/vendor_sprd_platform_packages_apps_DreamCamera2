package com.dream.camera;

import android.view.View;
import android.widget.LinearLayout;
import android.os.Handler;
import android.util.Log;

import com.android.camera.CameraActivity;
import com.android.camera.settings.Keys;
import com.android.camera.widget.selector.SelectorContainer;
import com.android.camera.widget.selector.SelectorController;
import com.android.camera.widget.selector.SelectorScroller;
import com.android.camera.widget.selector.SelectorSwitcher;
import com.android.camera.widget.selector.filter.MakeUpControllerImpl;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorController;
import com.android.camera.widget.selector.interfaces.InterfaceSelectorItem;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DreamSettingUtil;
import com.android.camera2.R;
import com.dream.camera.ui.MakeUpButton;
import com.dream.camera.ui.MakeUpKey;
import com.dream.camera.ui.MakeUpLevel;
import com.android.camera.util.CameraUtil;
import com.android.camera.settings.SettingsScopeNamespaces;

import java.util.ArrayList;

public class MakeupController implements View.OnClickListener, MakeUpLevel.MakeupLevelListener, InterfaceSelectorController.CallBack, SelectorController.InitializeCallBack {

    private final String TAG = "MakeupController";

    public interface MakeupListener {

        public void onBeautyValueChanged(int[] value);

        public void setMakeUpController(MakeupController makeUpController);

        public void updateMakeLevel();

        void onFeatureOperationEnableChange(boolean enable);
    }

    private class UpdateRunnable implements Runnable{
        @Override
        public void run() {
            Log.d(TAG,"UpdateRunnable beauty level change");
            mController.updateMakeLevel();
        }

    }

    public class ClickSubUIParam{
        // this class is for make up features which contains sub item
        // now , includes 2 features: "skinColor" & "lipsColor"
        int mCount; // count of sub items
        int mType;   // mMakeUpParameter[type]
        int mKeys[]; // keys array of sub items
        MakeUpButton mButtons[]; // buttons array of sub items
        int mDefaultIndex; // default index for which one is selected statue
        String mPrefTypeKey; // KEY_MAKEUP_SKIN_COLOR_TYPE

        ClickSubUIParam(int count , int type , int keys[] , MakeUpButton buttons[] , int defaultIndex , String perfTypeKey) {
            mCount = count;
            mType = type;
            mKeys = keys;
            mButtons = buttons;
            mDefaultIndex = defaultIndex;
            mPrefTypeKey = perfTypeKey;
        }

        public void setItemType() {
            for(int i = 0 ; i < mCount ; ++i) {
                if (mMakeUpParameter[mType] == mKeys[i]) {
                    if (mButtons[i] != null) {
                        mButtons[i].setSelect(true);
                    }
                    return;
                }
            }
            if(mButtons[mDefaultIndex] != null)
                mButtons[mDefaultIndex].setSelect(true);
            return;
        }

        boolean useThisFeature(View view) {
            for(int i = 0 ; i < mCount ; ++i) {
                if(mButtons[i] != null && view == mButtons[i])
                    return true;
            }
            return false;
        }
    }

    private boolean mFirstInit = false;

    private MakeUpButton skinColorButtons[] = {null , null , null};
    private MakeUpButton lipsColorButtons[] = {null , null , null};

    // front camera default values
    private final String F_SkIN_SMOOTH_DV = "6";
    private final String F_SkIN_BRIGTH_DV = "6";
    private final String F_SLIM_FACE_DV = "2";
    private final String F_ENLARGE_EYES_DV = "2";

    // back camera default values
    private final String B_SkIN_SMOOTH_DV = "6";
    private final String B_SkIN_BRIGTH_DV = "6";
    private final String B_SLIM_FACE_DV = "2";
    private final String B_ENLARGE_EYES_DV = "2";

    private String mSkinSmoothDefaultValue = B_SkIN_SMOOTH_DV;
    /**this is for arc beatuy lib **/
    private String mSkinBrightDefaultValue = B_SkIN_BRIGTH_DV;

    /**0:white, 1:rosy, 2:wheat, this is for sprd beatuy lib**/
    private final String mSkinColorDefaultType = "0";
    private final String mSkinColorDefaultLevel = "0";

    private String mEnlargeEyesDefaultValue = B_SLIM_FACE_DV;
    private String mSlimFaceDefaultValue = B_ENLARGE_EYES_DV;
    private final String mRemoveBlemishDefaultValue = "1";

    /**0:crimson, 1:pink, 2:fuchsia, this is for sprd beatuy lib**/
    private final String mLipsColorDefaultType = "0";
    private final String mLipsColorDefaultLevel = "0";
    private final String mLipsColorDefaultLevelForAI = "0";

    protected final CameraActivity mActivity;
    MakeUpControllerImpl mMakeUpController;
    SelectorScroller mMakeUpScroller;
    private MakeupListener mController;
    private SelectorSwitcher mMakeUpSwitcher;
    private SelectorContainer mMakeUpContainer;
    Handler mHandler;
    UpdateRunnable runnAble;
    private DataModuleBasic mCurrentDataModule;

    private LinearLayout mMakeupControllerView;
    private MakeUpLevel mSkinSmoothPanel;
    private MakeUpLevel mSkinBrightPanel;
    private MakeUpLevel mEnlargeEyesPanel;
    private MakeUpLevel mSlimFacePanel;
    private MakeUpLevel mRemoveBlemishPanel;
    private MakeUpLevel mSkinColorLevel;
    private MakeUpLevel mLipsColorLevel;
    private LinearLayout mSkinColorControlPanel;
    private LinearLayout mLipsColorControlPanel;

    private MakeUpButton mSkinColorWhiteBt;
    private MakeUpButton mSkinColorRosyBt;
    private MakeUpButton mSkinColorWheatBt;
    private MakeUpButton mLipsColorCrimsonBt;
    private MakeUpButton mLipsColorPinkBt;
    private MakeUpButton mLipsColorFuchsiaBt;

    private static final int mMakeUpParameterNum = 9;
    private final int KEY_REMOVEBLEMISHFLAG = 0;
    private final int KEY_SKINSMOOTHLEVEL = 1;
    private final int KEY_SKINCOLORTYPE = 2;
    private final int KEY_SKINCOLORLEVEL = 3;
    private final int KEY_SKINBRIGHTLEVEL = 4;
    private final int KEY_LIPCOLORTYPE = 5;
    private final int KEY_LIPCOLORLEVEL = 6;
    private final int KEY_SLIMFACELEVEL = 7;
    private final int KEY_LARGEEYELEVEL = 8;

    private int[] mMakeUpParameter = new int[]{0,0,0,0,0,0,0,0,0};


    ClickSubUIParam skinColorSubItems = null;
    ClickSubUIParam lipsColorSubItems = null;

    private int skinColorTypeKeys[] = {MakeUpKey.SKIN_COLOR_TYPE_WHITE , MakeUpKey.SKIN_COLOR_TYPE_ROSY , MakeUpKey.SKIN_COLOR_TYPE_WHEAT};
    private int lipsColorTypeKeys[] = {MakeUpKey.LIPS_COLOR_TYPE_CRIMSON , MakeUpKey.LIPS_COLOR_TYPE_PINK , MakeUpKey.LIPS_COLOR_TYPE_FUCHSIA};

    /*mNotSupportFeature[] contains not support feature
    * Index description:0 RemoveBlemish;1 SkinColor; 2 LipsColor*/
    private final int REMOVEBLEMISH_INDEX = 0;
    private final int SKINCOLOR_INDEX = 1;
    private final int LIPCOLOR_INDEX = 2;
    private boolean mNotSupportFeature[] = new boolean[]{false,false,false};

    public MakeupController(View extendPanelParent, MakeupListener listener, CameraActivity activity) {
        mActivity = activity;
        mController = listener;
        mHandler = new Handler(extendPanelParent.getContext().getMainLooper());
        runnAble = new UpdateRunnable();
        mCurrentDataModule = DataModuleManager.getInstance(mActivity).getCurrentDataModule();
        if (extendPanelParent != null) {

            mMakeUpScroller = (SelectorScroller)extendPanelParent.findViewById(R.id.make_up_scroller);
            mMakeUpContainer = (SelectorContainer)extendPanelParent.findViewById(R.id.make_up_container);
            mMakeUpSwitcher = (SelectorSwitcher)extendPanelParent.findViewById(R.id.make_up_switcher);
            mMakeUpController = new MakeUpControllerImpl(mActivity,
                    mMakeUpScroller,mMakeUpContainer,mMakeUpSwitcher,this, this);

            mMakeupControllerView = (LinearLayout) extendPanelParent
                    .findViewById(R.id.dream_make_up_panel);

            initMakeUpLevelUI();

            initMakeUpButtonUI();

            mController.setMakeUpController(this);
            initMakeupLevel();

            mController.updateMakeLevel();
        }
    }

    @Override
    public void onFeatureOperationEnableChange(boolean enable) {
        mController.onFeatureOperationEnableChange(enable);
        if(enable){
            resetStatus();
            selectDefaultIndex();
        } else {
            resetAllIcon();
            // hide all seek bar
            hideAllSeekbar();
        }
    }

    private void selectDefaultIndex(){
        // selected default feature button & show level ui
        mSkinSmoothPanel.setVisibility(View.VISIBLE);
        mMakeUpController.selectItem(0);
    }

    @Override
    public void onItemSelectedChange(int index, String key, String value) {
        LinearLayout panel = null;
        ClickSubUIParam param = null;
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_skinsmooth))){
            panel = mSkinSmoothPanel;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_removeblemish))){
            panel = mRemoveBlemishPanel;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_skinbright))) {
            panel = mSkinBrightPanel;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_skincolor))) {
            panel = mSkinColorControlPanel;
            param = skinColorSubItems;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_enlargeeyes))) {
            panel = mEnlargeEyesPanel;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_slimface))) {
            panel = mSlimFacePanel;
        }
        if(key.equals(mActivity.getResources().getString(R.string.make_up_key_lipscolor))) {
            panel = mLipsColorControlPanel;
            param = lipsColorSubItems;
        }

        hideAllSeekbar();

        if(panel != null){
            panel.setVisibility(View.VISIBLE);
        }

        if(param != null){
            param.setItemType();
        }

    }



    public void pause(){
        if (mMakeupControllerView != null) {
            Log.i(TAG, "pause Makeup Controller View");
            mMakeupControllerView.setVisibility(View.GONE);
        }
    }

    public void resume(boolean enable){
        if (enable && mMakeupControllerView != null && mMakeUpController.getSize() > 0) {
            Log.i(TAG, "resume Makeup Controller View");
            resetAllIcon();
            int index = mMakeUpController.getSelectedIndex();
            index = index == -1 ? 0 : index;
            mMakeUpController.selectItem(index);
            String key = mMakeUpController.getKey(index);
            String value = mMakeUpController.getValue(index);
            onItemSelectedChange(index,key,value);
            // show UI
        }

        if(!enable){
            // reset all second feature icon
            resetAllIcon();
            // hide all seek bar
            hideAllSeekbar();
            mMakeUpController.enableFeatureOperation(false);
        }


        mMakeupControllerView.setVisibility(View.VISIBLE);
    }

    private void resetStatus(){
        // reset all second feature icon
        resetAllIcon();

        // hide all seek bar
        hideAllSeekbar();

        // update UI
        initMakeupLevel();

    }

    private void initMakeUpButtonUI() {

        mSkinColorWhiteBt = setButton(R.id.skincolor_white_btn, MakeUpKey.SKIN_COLOR_WHITE);
        mSkinColorRosyBt = setButton(R.id.skincolor_rosy_btn, MakeUpKey.SKIN_COLOR_ROSY);
        mSkinColorWheatBt = setButton(R.id.skincolor_wheat_btn, MakeUpKey.SKIN_COLOR_WHEAT);
        skinColorButtons[0] = mSkinColorWhiteBt;
        skinColorButtons[1] = mSkinColorRosyBt;
        skinColorButtons[2] = mSkinColorWheatBt;
        skinColorSubItems = new ClickSubUIParam(3, KEY_SKINCOLORTYPE, skinColorTypeKeys, skinColorButtons, 0, Keys.KEY_MAKEUP_SKIN_COLOR_TYPE);


        mLipsColorCrimsonBt = setButton(R.id.lipscolor_crimson_btn, MakeUpKey.LIPS_COLOR_CRIMSON);
        mLipsColorPinkBt = setButton(R.id.lipscolor_pink_btn, MakeUpKey.LIPS_COLOR_PINK);
        mLipsColorFuchsiaBt = setButton(R.id.lipscolor_fuchsia_btn, MakeUpKey.LIPS_COLOR_FUCHSIA);
        lipsColorButtons[0] = mLipsColorCrimsonBt;
        lipsColorButtons[1] = mLipsColorPinkBt;
        lipsColorButtons[2] = mLipsColorFuchsiaBt;
        lipsColorSubItems = new ClickSubUIParam(3, KEY_LIPCOLORTYPE, lipsColorTypeKeys, lipsColorButtons, 0, Keys.KEY_MAKEUP_LIPS_COLOR_TYPE);

    }

    private void initMakeUpLevelUI() {
        mSkinSmoothPanel = (MakeUpLevel) mMakeupControllerView.findViewById(R.id.skin_smooth_panel);
        mSkinSmoothPanel.setKey(MakeUpKey.SKIN_SMOOTH_LEVEL);
        mSkinSmoothPanel.setListener(this);
        mSkinBrightPanel = (MakeUpLevel) mMakeupControllerView.findViewById(R.id.skin_bright_panel);
        mSkinBrightPanel.setKey(MakeUpKey.SKIN_BRIGHT_LEVEL);
        mSkinBrightPanel.setListener(this);
        mEnlargeEyesPanel = (MakeUpLevel)  mMakeupControllerView.findViewById(R.id.enlarge_eyes_panel);
        mEnlargeEyesPanel.setKey(MakeUpKey.LARGE_EYES_LEVEL);
        mEnlargeEyesPanel.setListener(this);
        mSlimFacePanel = (MakeUpLevel)  mMakeupControllerView.findViewById(R.id.slim_face_panel);
        mSlimFacePanel.setKey(MakeUpKey.SLIM_FACE_LEVEL);
        mSlimFacePanel.setListener(this);

        /* the below items only display when using sprd beauty lib, so add null pointer  protection*/
        if (!mNotSupportFeature[REMOVEBLEMISH_INDEX]) {
            mRemoveBlemishPanel = (MakeUpLevel) mMakeupControllerView.findViewById(R.id.remove_blemish_panel);
            if (mRemoveBlemishPanel != null) {
                mFirstInit = true;
                mRemoveBlemishPanel.setKey(MakeUpKey.REMOVE_BLEMISH_LEVEL);
                mRemoveBlemishPanel.setListener(this);
                mRemoveBlemishPanel.setMaxLevel(1);
                mRemoveBlemishPanel.setMinLevel(0);
            }
        }

        if (!mNotSupportFeature[SKINCOLOR_INDEX]) {
            mSkinColorLevel = (MakeUpLevel) mMakeupControllerView.findViewById(R.id.skincolor_level_panel);
            if (mSkinColorLevel != null) {
                mSkinColorLevel.setKey(MakeUpKey.SKIN_COLOR_LEVEL);
                mSkinColorLevel.setListener(this);
            }
            mSkinColorControlPanel = (LinearLayout) mMakeupControllerView.findViewById(R.id.skincolor_panel);
            if (mSkinColorControlPanel != null){
                mSkinColorControlPanel.setOnClickListener(this);
            }
        }

        if (!mNotSupportFeature[LIPCOLOR_INDEX]) {
            mLipsColorLevel =  (MakeUpLevel) mMakeupControllerView.findViewById(R.id.lipscolor_level_panel);
            if (mLipsColorLevel != null) {
                mLipsColorLevel.setKey(MakeUpKey.LIPS_COLOR_LEVEL);
                mLipsColorLevel.setListener(this);
            }
            mLipsColorControlPanel = (LinearLayout)  mMakeupControllerView.findViewById(R.id.lipscolor_panel);
            if (mLipsColorControlPanel != null) {
                mLipsColorControlPanel.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View view) {
        // buttons of LEVEL - 2
        boolean skip = false;
        skip = clickOneItem(skip , view , skinColorSubItems , mSkinColorControlPanel , mSkinColorLevel);
        clickOneItem(skip , view , lipsColorSubItems , mLipsColorControlPanel , mLipsColorLevel);
    }

    boolean clickOneItem(boolean skip , View view , ClickSubUIParam subItem , LinearLayout panel , MakeUpLevel level) {
        // click on a LEVEL-2 item's icon
        if (skip == true) return true;
        if (subItem.useThisFeature(view)) {
            panel.setVisibility(View.VISIBLE);
            level.setVisibility(View.VISIBLE);
            changeItemIconUI(view , subItem);
            initMakeupLevel();
            Log.d(TAG,"beauty color change");
            mController.onBeautyValueChanged(getValue());
            return true;
        }
        return false;
    }

    private void changeItemIconUI(View view , ClickSubUIParam subUIParam) {
        for(int i = 0 ; i < subUIParam.mCount ; ++i) {
            // set clicked button to display as YELLOW , and set indicate parameters
            if(subUIParam.mButtons[i] == view) {
                subUIParam.mButtons[i].setSelect(true);
                mCurrentDataModule.set(DataConfig.SettingStoragePosition.positionList[3], subUIParam.mPrefTypeKey,
                        "" + subUIParam.mKeys[i]);
                mMakeUpParameter[subUIParam.mType] = subUIParam.mKeys[i];
            }
            else // set no-clicked button to display as WHITE
                subUIParam.mButtons[i].setSelect(false);
        }
    }

    private void hideAllSeekbar() {
        if (mSkinSmoothPanel != null) {
            mSkinSmoothPanel.setVisibility(View.GONE);
        }
        if (mSkinBrightPanel != null) {
            mSkinBrightPanel.setVisibility(View.GONE);
        }
        if (mEnlargeEyesPanel != null) {
            mEnlargeEyesPanel.setVisibility(View.GONE);
        }
        if (mSlimFacePanel != null) {
            mSlimFacePanel.setVisibility(View.GONE);
        }
        if (mRemoveBlemishPanel != null) {
            mRemoveBlemishPanel.setVisibility(View.GONE);
        }
        if (mSkinColorControlPanel != null) {
            mSkinColorControlPanel.setVisibility(View.GONE);
        }
        if (mLipsColorControlPanel != null) {
            mLipsColorControlPanel.setVisibility(View.GONE);
        }
    }


    @Override
    public void onBeautyLevelWillChanged(int key, int value) {
        int changeKey = key;
        int changeValue = value;
        int parameterKey = 0;
        switch (changeKey) {
            case MakeUpKey.SKIN_SMOOTH_LEVEL:
                parameterKey = KEY_SKINSMOOTHLEVEL;
                break;
            case MakeUpKey.REMOVE_BLEMISH_LEVEL:
                parameterKey = KEY_REMOVEBLEMISHFLAG;
                break;
            case MakeUpKey.SKIN_BRIGHT_LEVEL:
                parameterKey = KEY_SKINBRIGHTLEVEL;
                break;
            case MakeUpKey.SKIN_COLOR_LEVEL:
                parameterKey = KEY_SKINCOLORLEVEL;
                break;
            case MakeUpKey.LARGE_EYES_LEVEL:
                parameterKey = KEY_LARGEEYELEVEL;
                break;
            case MakeUpKey.SLIM_FACE_LEVEL:
                parameterKey = KEY_SLIMFACELEVEL;
                break;
            case MakeUpKey.LIPS_COLOR_LEVEL:
                parameterKey = KEY_LIPCOLORLEVEL;
                break;
            default:
                break;
        }
        if (changeKey == MakeUpKey.REMOVE_BLEMISH_LEVEL && mFirstInit) {
            mFirstInit = false;
            return;
        }
        mMakeUpParameter[parameterKey] = changeValue;
        Log.d(TAG,"beauty level will change");
        mHandler.removeCallbacks(runnAble);
        mHandler.postAtTime(runnAble, 100);
    }

    @Override
    public void onBeautyLevelChanged(int key, int value) {
        int changeKey = key;
        int changeValue = value;
        int parameterKey = 0;
        switch (changeKey) {
            case MakeUpKey.SKIN_SMOOTH_LEVEL:
                parameterKey = KEY_SKINSMOOTHLEVEL;
                break;
            case MakeUpKey.REMOVE_BLEMISH_LEVEL:
                parameterKey = KEY_REMOVEBLEMISHFLAG;
                break;
            case MakeUpKey.SKIN_BRIGHT_LEVEL:
                parameterKey = KEY_SKINBRIGHTLEVEL;
                break;
            case MakeUpKey.SKIN_COLOR_LEVEL:
                parameterKey = KEY_SKINCOLORLEVEL;
                break;
            case MakeUpKey.LARGE_EYES_LEVEL:
                parameterKey = KEY_LARGEEYELEVEL;
                break;
            case MakeUpKey.SLIM_FACE_LEVEL:
                parameterKey = KEY_SLIMFACELEVEL;
                break;
            case MakeUpKey.LIPS_COLOR_LEVEL:
                parameterKey = KEY_LIPCOLORLEVEL;
                break;
            default:
                break;
        }
        mMakeUpParameter[parameterKey] = changeValue;
        Log.d(TAG,"beauty level change");
        mController.onBeautyValueChanged(getValue());
    }

    private void initMakeupLevel() {
        int curLevel = 0;
        if(mCurrentDataModule.getDataSetting().mIsFront){
            mSkinSmoothDefaultValue = F_SkIN_SMOOTH_DV;
            mSkinBrightDefaultValue = F_SkIN_BRIGTH_DV;
            mSlimFaceDefaultValue = F_SLIM_FACE_DV;
            mEnlargeEyesDefaultValue = F_ENLARGE_EYES_DV;

        } else {
            mSkinSmoothDefaultValue = B_SkIN_SMOOTH_DV;
            mSkinBrightDefaultValue = B_SkIN_BRIGTH_DV;
            mSlimFaceDefaultValue = B_SLIM_FACE_DV;
            mEnlargeEyesDefaultValue = B_ENLARGE_EYES_DV;
        }
        if (mSkinSmoothPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_SKIN_SMOOTH_LEVEL, "" + mSkinSmoothDefaultValue));
            mSkinSmoothPanel.setLevel(curLevel);
            mMakeUpParameter[KEY_SKINSMOOTHLEVEL] = curLevel;
        }
        if (mSkinBrightPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_SKIN_BRIGHT_LEVEL, "" + mSkinBrightDefaultValue));
            mSkinBrightPanel.setLevel(curLevel);
            mMakeUpParameter[KEY_SKINBRIGHTLEVEL] = curLevel;
        }
        if (mEnlargeEyesPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_ENLARGE_EYES_LEVEL, "" + mEnlargeEyesDefaultValue));
            mEnlargeEyesPanel.setLevel(curLevel);
            mMakeUpParameter[KEY_LARGEEYELEVEL] = curLevel;
        }
        if (mSlimFacePanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_SLIM_FACE_LEVEL, "" + mSlimFaceDefaultValue));
            mSlimFacePanel.setLevel(curLevel);
            mMakeUpParameter[KEY_SLIMFACELEVEL] = curLevel;
        }
        if (mRemoveBlemishPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_REMOVE_BLEMISH_LEVEL, "" + mRemoveBlemishDefaultValue));
            mRemoveBlemishPanel.setLevel(curLevel);
            mMakeUpParameter[KEY_REMOVEBLEMISHFLAG] = curLevel;
        }
        if (mSkinColorLevel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_SKIN_COLOR_LEVEL, "" + mSkinColorDefaultLevel));
            mSkinColorLevel.setLevel(curLevel);
            mMakeUpParameter[KEY_SKINCOLORLEVEL] = curLevel;
        }
        if (mSkinColorControlPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_SKIN_COLOR_TYPE, "" + mSkinColorDefaultType));
            setSkinColorType(curLevel);
            mMakeUpParameter[KEY_SKINCOLORTYPE] = curLevel;
        }
        if (mLipsColorLevel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_LIPS_COLOR_LEVEL, "" + mLipsColorDefaultLevel));
            mLipsColorLevel.setLevel(curLevel);
            mMakeUpParameter[KEY_LIPCOLORLEVEL] = curLevel;
        }
        if (mLipsColorControlPanel != null) {
            curLevel = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_MAKEUP_LIPS_COLOR_TYPE, "" + mLipsColorDefaultType));
            setLipsColorType(curLevel);
            mMakeUpParameter[KEY_LIPCOLORTYPE] = curLevel;
        }
    }

    protected static final int[] MAKE_UP_DEFAULT_VALUE = new int[]{0,0,0,0,0,0,0,0,0};


    public int[] getValue(String key){
        if(!mCurrentDataModule.getBoolean(key)){
            return MAKE_UP_DEFAULT_VALUE;
        } else {
            return getValue();
        }
    }

    private int[] getValue(){
        int[] curLevel = new int[mMakeUpParameterNum];
        curLevel[KEY_SKINSMOOTHLEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_SKIN_SMOOTH_LEVEL, mSkinSmoothDefaultValue));
        curLevel[KEY_SKINBRIGHTLEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_SKIN_BRIGHT_LEVEL, mSkinBrightDefaultValue));
        curLevel[KEY_LARGEEYELEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_ENLARGE_EYES_LEVEL, mEnlargeEyesDefaultValue));
        curLevel[KEY_SLIMFACELEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_SLIM_FACE_LEVEL, mSlimFaceDefaultValue));
        curLevel[KEY_SKINCOLORTYPE] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_SKIN_COLOR_TYPE, mSkinColorDefaultType));
        curLevel[KEY_SKINCOLORLEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_SKIN_COLOR_LEVEL, mSkinColorDefaultLevel));
        curLevel[KEY_LIPCOLORTYPE] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_LIPS_COLOR_TYPE, mLipsColorDefaultType));
        curLevel[KEY_LIPCOLORLEVEL] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_LIPS_COLOR_LEVEL, mLipsColorDefaultLevel));
        curLevel[KEY_REMOVEBLEMISHFLAG] = DreamSettingUtil.convertToInt(mCurrentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3],Keys.KEY_MAKEUP_REMOVE_BLEMISH_LEVEL, mRemoveBlemishDefaultValue));
        if (mNotSupportFeature[REMOVEBLEMISH_INDEX]) {
            curLevel[KEY_REMOVEBLEMISHFLAG] = 0;
        }
        if (mNotSupportFeature[LIPCOLOR_INDEX]) {
            curLevel[KEY_LIPCOLORLEVEL] = 0;
        }
        return curLevel;
    }

    // Bug 1018708 - hide or show BeautyButton on topPanel, and this is for ai beauty
    private int[] getDefaultBeautyValue() {
        int[] curLevel = new int[mMakeUpParameterNum];
        curLevel[KEY_SKINSMOOTHLEVEL] = DreamSettingUtil.convertToInt(mSkinSmoothDefaultValue);
        curLevel[KEY_SKINBRIGHTLEVEL] = DreamSettingUtil.convertToInt(mSkinBrightDefaultValue);
        curLevel[KEY_LARGEEYELEVEL] = DreamSettingUtil.convertToInt(mEnlargeEyesDefaultValue);
        curLevel[KEY_SLIMFACELEVEL] = DreamSettingUtil.convertToInt(mSlimFaceDefaultValue);
        curLevel[KEY_SKINCOLORTYPE] = DreamSettingUtil.convertToInt(mSkinColorDefaultType);
        curLevel[KEY_SKINCOLORLEVEL] = DreamSettingUtil.convertToInt(mSkinColorDefaultLevel);
        curLevel[KEY_LIPCOLORTYPE] = DreamSettingUtil.convertToInt(mLipsColorDefaultType);
        curLevel[KEY_LIPCOLORLEVEL] = DreamSettingUtil.convertToInt(mLipsColorDefaultLevelForAI);
        curLevel[KEY_REMOVEBLEMISHFLAG] = DreamSettingUtil.convertToInt(mRemoveBlemishDefaultValue);
        if (mNotSupportFeature[REMOVEBLEMISH_INDEX]) {
            curLevel[KEY_REMOVEBLEMISHFLAG] = 0;
        }
        if (mNotSupportFeature[LIPCOLOR_INDEX]) {
            curLevel[KEY_LIPCOLORLEVEL] = 0;
        }
        return curLevel;
    }

    private void setSkinColorType(int colorType) {
        int curColorType = colorType;
        switch (curColorType) {
            case MakeUpKey.SKIN_COLOR_TYPE_WHITE: {
                if (mSkinColorWhiteBt != null) {
                    mSkinColorWhiteBt.setSelect(true);
                }
            }
            break;
            case MakeUpKey.SKIN_COLOR_TYPE_ROSY: {
                if (mSkinColorRosyBt != null) {
                    mSkinColorRosyBt.setSelect(true);
                }
            }
            break;
            case MakeUpKey.SKIN_COLOR_TYPE_WHEAT: {
                if (mSkinColorWheatBt != null) {
                    mSkinColorWheatBt.setSelect(true);
                }
            }
            break;
            default: {
                if (mSkinColorWhiteBt != null) {
                    mSkinColorWhiteBt.setSelect(true);
                }
            }
            break;
        }
    }

    private void setLipsColorType(int colorType) {
        int lipColorType = colorType;
        switch (lipColorType) {
            case MakeUpKey.LIPS_COLOR_TYPE_CRIMSON: {
                if (mLipsColorCrimsonBt != null) {
                    mLipsColorCrimsonBt.setSelect(true);
                }
            }
            break;
            case MakeUpKey.LIPS_COLOR_TYPE_PINK: {
                if (mLipsColorPinkBt != null) {
                    mLipsColorPinkBt.setSelect(true);
                }
            }
            break;
            case MakeUpKey.LIPS_COLOR_TYPE_FUCHSIA: {
                if (mLipsColorFuchsiaBt != null) {
                    mLipsColorFuchsiaBt.setSelect(true);
                }
            }
            break;
            default:{
                if (mLipsColorCrimsonBt != null) {
                    mLipsColorCrimsonBt.setSelect(true);
                }
            }
            break;
        }
    }

    // SPRD:Bug 839474. split to another 1 method for CCN optimization
    protected MakeUpButton setButton(int id , int key) {
        MakeUpButton button = (MakeUpButton)mMakeupControllerView.findViewById(id);
        if(button != null) {
            button.setOnClickListener(this);
            button.setButtonKey(key);
        }
        return button;
    }

    // Bug 1018708 - hide or show BeautyButton on topPanel
    public void startBeautyWithoutController(boolean flag) {
        if (flag)
            mController.onBeautyValueChanged(getDefaultBeautyValue());
        else
            mController.onBeautyValueChanged(MAKE_UP_DEFAULT_VALUE);
    }

    private void resetAllIcon() {
        // lips items
        if(lipsColorSubItems != null) {
            for (int i = 0 ; i < lipsColorSubItems.mCount ; ++i)
                lipsColorSubItems.mButtons[i].setSelect(false);
        }

        // skin items
        if(skinColorSubItems != null) {
            for (int i = 0 ; i < skinColorSubItems.mCount ; ++i)
                skinColorSubItems.mButtons[i].setSelect(false);
        }
    }

    @Override
    public int getConfigListId() {
        if (mActivity.getCurrentModuleIndex() == SettingsScopeNamespaces.AUTO_VIDEO) {
            mNotSupportFeature[REMOVEBLEMISH_INDEX] = true;
            return R.array.make_up_video_mode_list;
        }
        return R.array.make_up_mode_list;
    }

    @Override
    public int getItemLayoutId() {
        return R.layout.layout_selector_item;
    }

    @Override
    public void filterItems(ArrayList<InterfaceSelectorItem.ItemDataStruct> itemStructs) {
        ArrayList<InterfaceSelectorItem.ItemDataStruct> removeList = new ArrayList<InterfaceSelectorItem.ItemDataStruct>();
        if (CameraUtil.getCurrentBeautyVersion() == CameraUtil.ENABLE_ARC_BEAUTY ||
                (CameraUtil.getCurrentBeautyVersion() == CameraUtil.ENABLE_SPRD_BEAUTY && !CameraUtil.isCameraBeautyAllFeatureEnabled())) {
            for (int i = 0; i<itemStructs.size();i++){
                if (itemStructs.get(i).mKey.equals(mActivity.getString(R.string.make_up_key_skincolor))) {
                    removeList.add(itemStructs.get(i));
                    mNotSupportFeature[SKINCOLOR_INDEX] = true;
                }
                if (itemStructs.get(i).mKey.equals(mActivity.getString(R.string.make_up_key_lipscolor))) {
                    removeList.add(itemStructs.get(i));
                    mNotSupportFeature[LIPCOLOR_INDEX] = true;
                }
                if (itemStructs.get(i).mKey.equals(mActivity.getString(R.string.make_up_key_removeblemish))) {
                    removeList.add(itemStructs.get(i));
                    mNotSupportFeature[REMOVEBLEMISH_INDEX] = true;
                }
            }
        }
        itemStructs.removeAll(removeList);
    }

    public void setEnable(boolean enable){
        mMakeUpScroller.setEnabled(enable);
        mMakeUpSwitcher.setEnabled(enable);
        if(mSkinSmoothPanel != null && View.VISIBLE == mSkinSmoothPanel.getVisibility()){
            mSkinSmoothPanel.setEnable(enable);
        }
        if(mSkinBrightPanel != null && View.VISIBLE == mSkinBrightPanel.getVisibility()){
            mSkinBrightPanel.setEnable(enable);
        }
        if(mEnlargeEyesPanel != null && View.VISIBLE == mEnlargeEyesPanel.getVisibility()){
            mEnlargeEyesPanel.setEnable(enable);
        }
        if(mSlimFacePanel != null && View.VISIBLE == mSlimFacePanel.getVisibility()){
            mSlimFacePanel.setEnable(enable);
        }
        if(mRemoveBlemishPanel != null && View.VISIBLE == mRemoveBlemishPanel.getVisibility()){
            mRemoveBlemishPanel.setEnable(enable);
        }
        if(mSkinColorLevel != null && View.VISIBLE == mSkinColorLevel.getVisibility()){
            mSkinColorLevel.setEnable(enable);
            for(MakeUpButton makeUpButton : skinColorButtons){
                makeUpButton.setEnabled(enable);
            }
        }
        if(mLipsColorLevel != null && View.VISIBLE == mLipsColorLevel.getVisibility()){
            mLipsColorLevel.setEnable(enable);
            for(MakeUpButton makeUpButton : lipsColorButtons){
                makeUpButton.setEnabled(enable);
            }
        }
    }
}
