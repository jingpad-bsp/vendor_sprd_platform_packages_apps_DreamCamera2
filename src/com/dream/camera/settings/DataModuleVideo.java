package com.dream.camera.settings;

import java.util.ArrayList;
import java.util.Set;

import android.content.Context;
import android.media.CamcorderProfile;
import android.util.ArraySet;
import android.util.Log;

import com.android.camera2.R;
import com.android.camera.settings.Keys;
import com.dream.camera.settings.DataConfig.CategoryType;
import com.android.camera.util.CameraUtil;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.camera.settings.SettingsUtil.SelectedVideoQualities;
import com.dream.camera.settings.DataModuleBasic.DataStorageStruct;

public class DataModuleVideo extends DataModuleInterfacePV {
    private String mSlowMotion = null;
    private int lastCameraId = -1;
    private Boolean mSupportedFilterVideoEnable = false;
    public DataModuleVideo(Context context) {
        super(context);
        mCategory = CategoryType.CATEGORY_VIDEO;
        mDefaultStorePosition = DataConfig.SettingStoragePosition.positionList[2];
    }

    @Override
    public void changeCurrentModule(DataStructSetting dataSetting) {
        super.changeCurrentModule(dataSetting);

        // Change the handle of sharepreference
        changSPB(dataSetting);

        initializeData(dataSetting);
    }
    private boolean mSupportedLevelEnable = false;
    @Override
    protected void setEVSVideoQualities(String key,
                                        SelectedVideoQualities selectedQualities) {
        Log.i(TAG, "child setEVSVideoQualities/in");
        super.setEVSVideoQualities(key, selectedQualities);
        Set<String> keyList = new ArraySet<String>();
        keyList.add(key);
        notifyKeyChange(keyList);
    }
    @Override
    protected void fillEntriesAndSummaries() {
        if(mPictureSizes != null){
            if(mPictureSizes.videoQualitiesFront != null){
                // front picture size
                setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_FRONT,
                        mPictureSizes.videoQualitiesFront.orNull());
            }

            if(mPictureSizes.videoQualitiesBack != null){
                // back picture size
                setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_BACK,
                        mPictureSizes.videoQualitiesBack.orNull());
            }
            if(mPictureSizes.videoQualitiesFront3D != null){
                setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_FRONT_3D,
                        mPictureSizes.videoQualitiesFront3D.orNull());
            }
            if (mPictureSizes.videoQualitiesBackMacro != null) {
                setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_BACK_MACRO,
                        mPictureSizes.videoQualitiesBackMacro.orNull());
            }
        }
        // set slow motion
        setSlowMotion(Keys.KEY_VIDEO_SLOW_MOTION);

        setAntibanding();
    }

    private void setSlowMotion(String key) {
        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(key);
        if (data == null) {
            return;
        }
        if (mSlowMotion != null) {
            String[] slowMotionValues = mSlowMotion.split(",");
            int k = 0;
            if (slowMotionValues != null && slowMotionValues.length > 1
                    && "0".equals(slowMotionValues[0])
                    && ("1".equals(slowMotionValues[1]) || "0".equals(slowMotionValues[1]))) {
                k = 1;
            }

            if (slowMotionValues != null) {
                String[] slowMotionEntries = new String[slowMotionValues.length
                        - k - 1];
                String[] slowMotionEntryValues = new String[slowMotionValues.length
                        - k - 1];
                int i = 0;
                for (String entryValue : slowMotionValues) {
                    if ("0".equals(entryValue) || "1".equals(entryValue)) {
                        continue;
                    } else {
                        slowMotionEntries[i] = entryValue;
                        slowMotionEntryValues[i] = entryValue;
                    }
                    ++i;
                }

                if (slowMotionEntries[0] == null) {
                    slowMotionEntries[0] = "1";
                    slowMotionEntryValues[0] = "1";
                }
                data.mEntries = slowMotionEntries;
                data.mEntryValues = slowMotionEntryValues;
                data.mDefaultValue = data.mEntryValues[0].toString();
            }
        }
    }

    protected void setAntibanding(){
        if (!isEnableSettingConfig(Keys.KEY_VIDEO_ANTIBANDING)){
            return;
        }
        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_VIDEO_ANTIBANDING);
        if (data == null) {
            Log.i(TAG, "setAntibanding() data == null");
            return;
        }
        data.mEntryValues = mContext.getResources().getTextArray(
                R.array.pref_camera_antibanding_entryvalues);
        if (mSupportedAntibandAutoEnable && isSupportedAntibandAutoInited) {
            data.mEntries = mContext.getResources().getTextArray(
                    R.array.pref_camera_antibanding_entries_addauto);
            data.mDefaultValue = data.mEntryValues[2].toString();
        }
    }

    @Override
    protected void setMutex(String key, Object newValue, Set<String> keyList) {
        String entryValue = getString(key);
        switch (key) {
        case Keys.KEY_EOIS_DV_BACK:
            setMutexEoisDvBack(key, entryValue, keyList);
            break;
        case Keys.KEY_VIDEO_BEAUTY_ENTERED:
            setMutexVideoBeauty(key, entryValue, keyList);
            break;
        case Keys.KEY_VIDEO_QUALITY_BACK: // SPRD Bug 1087282: mutex between 4K & eois (back)
            setMutexVideoQualityBack(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_VIDEO_FACE_DETECT:
            setMutexFaceDetect(key, entryValue, keyList);
            break;
        case Keys.KEY_AUTO_TRACKING:
            setMutexAutoTracking(key, entryValue, keyList);
            break;
        case Keys.KEY_FILTER_VIDEO_DISPLAY:
            setMutexFilterVideoDisplay(key, entryValue, keyList);
        case Keys.KEY_MAKE_UP_DISPLAY:
            setMutexMakeUpDisplay(key, entryValue, keyList);
        case Keys.KEY_VIDEO_WHITE_BALANCE:
            setMutexWhiteBalance(key, entryValue, keyList);
        case Keys.KEY_VIDEO_COLOR_EFFECT:
            setMutexVideoColorEffect(key, entryValue, keyList);
        default:
            break;
        }

    }
    private void setMutexVideoColorEffect(String key, String entryValue,
                                      Set<String> keyList) {
        if(!"auto".equals(
                getString(Keys.KEY_VIDEO_WHITE_BALANCE))) {
            continueSetMutex(Keys.KEY_VIDEO_WHITE_BALANCE, mContext.getString(
                    R.string.pref_camera_whitebalance_entry_auto), keyList,
                    "video color effect with video white balance");
        }
    }
    private void setMutexWhiteBalance(String key, String entryValue,
                                            Set<String> keyList) {
        if(!"auto".equals(
                getString(Keys.KEY_VIDEO_WHITE_BALANCE))){
            if(getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
                continueSetMutex(Keys.KEY_FILTER_VIDEO_DISPLAY, false, keyList,
                        "white balance mutex with filter video display");
            }
            if(!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    getString(Keys.KEY_VIDEO_COLOR_EFFECT))) {
                continueSetMutex(
                        Keys.KEY_VIDEO_COLOR_EFFECT,
                        mContext.getString(R.string.pref_camera_color_effect_entry_value_none),
                        keyList, "white balance mutex with color effect");
            }
        }
    }
    private void setMutexFilterVideoDisplay(String key, String entryValue,
                                            Set<String> keyList) {
        if(getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)){
            if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)) {
                continueSetMutex(Keys.KEY_MAKE_UP_DISPLAY, false, keyList,
                        "filter video display mutex with make up display");
            }
            if(getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED)) {
                continueSetMutex(Keys.KEY_VIDEO_BEAUTY_ENTERED, false, keyList,
                        "filter video display mutex with beauty");
            }
            if(!"auto".equals(
                    getString(Keys.KEY_VIDEO_WHITE_BALANCE))) {
                continueSetMutex(Keys.KEY_VIDEO_WHITE_BALANCE, mContext.getString(
                        R.string.pref_camera_whitebalance_entry_auto), keyList,
                        "filter video display mutex with video white balance");
            }
        }
    }
    private void setMutexMakeUpDisplay(String key, String entryValue,
                                            Set<String> keyList) {
        if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)){
            if(getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
                continueSetMutex(Keys.KEY_FILTER_VIDEO_DISPLAY, false, keyList,
                        "make up display mutex with filter video display");
            }
            if(!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    getString(Keys.KEY_VIDEO_COLOR_EFFECT))) {
                continueSetMutex(
                        Keys.KEY_VIDEO_COLOR_EFFECT,
                        mContext.getString(R.string.pref_camera_color_effect_entry_value_none),
                        keyList, "beauty mutex with color effect");
            }
        }
    }
    private void setMutexFaceDetect(String key, String entryValue,
                                  Set<String> keyList) {
        //FACE -MUTEX WITH auto tracking
        if (!mContext.getString(R.string.preference_switch_item_default_value_false).equals(
                getString(Keys.KEY_AUTO_TRACKING))) {
            continueSetMutex(Keys.KEY_AUTO_TRACKING,
                    mContext.getString(R.string.preference_switch_item_default_value_false),
                    keyList, "face detect mutex with auto tracking");
            }
    }

    private void setMutexAutoTracking(String key, String entryValue,
                                      Set<String> keyList) {
        if (mContext.getString(R.string.preference_switch_item_default_value_true)
                .equals(entryValue)) {
            //auto tracking --- mutex with face detect
            if (mContext.getString(R.string.preference_switch_item_default_value_true).equals(getString(Keys.KEY_CAMERA_VIDEO_FACE_DETECT)))
                continueSetMutex(Keys.KEY_CAMERA_VIDEO_FACE_DETECT,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "auto tracking mutex with face detect");
//            //auto tracking --- mutex with beauty
//            if (getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
//                continueSetMutex(Keys.KEY_CAMERA_BEAUTY_ENTERED,
//                        mContext.getString(R.string.preference_switch_item_default_value_false),
//                        keyList, "auto tracking mutex with beauty");
//            }
        }
    }
    private void setMutexVideoBeauty(String key, String entryValue,
            Set<String> keyList) {
        if (getBoolean(key)) {
            // mutex with back video quality
            if (isFourKSelected() && !keyList.contains(Keys.KEY_VIDEO_QUALITY_BACK)) {
                mOriginalVideoQualityBackValueString = getString(Keys.KEY_VIDEO_QUALITY_BACK);
                continueSetMutex(Keys.KEY_VIDEO_QUALITY_BACK, "medium", keyList,
                        "4k mutex with back beauty");
                replaceVideoQualityBackStruct(Keys.KEY_VIDEO_QUALITY_BACK, keyList);
            }
            // mutex with eois
            if (getBoolean(Keys.KEY_EOIS_DV_BACK)) {
                continueSetMutex(Keys.KEY_EOIS_DV_BACK, false, keyList,
                        "back eois mutex with video beauty");
            }
        } else {
            if (isFourKSelected() ) {
                restoreVideoQualityBackStruct(Keys.KEY_VIDEO_QUALITY_BACK, keyList);
                continueSetRestore(Keys.KEY_VIDEO_QUALITY_BACK, keyList,
                        "4k resotre back video beauty");
            }
            continueSetRestore(Keys.KEY_EOIS_DV_BACK, keyList,
                    "video beauty resotre back eois");
        }
    }

    /* SPRD:replace && restore 4k @{ */
    private DataStorageStruct originalVideoQualityBackDataStorageStruct = null;
    private String mOriginalVideoQualityBackValueString = null;

    private void replaceVideoQualityBackStruct(String key, Set<String> keylist) {

        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(key);

        if (data == null) {
            return;
        }
        originalVideoQualityBackDataStorageStruct = new DataStorageStruct();
        originalVideoQualityBackDataStorageStruct.copy(data);

        if (mOriginalVideoQualityBackValueString != null) {
            originalVideoQualityBackDataStorageStruct.mRestorageValue = mOriginalVideoQualityBackValueString;
        }

        ArrayList<String> entries = new ArrayList<String>();
        ArrayList<String> entryValues = new ArrayList<String>();

        for (int i = 0; i < data.mEntries.length; i++) {
            if (i == 0) {
                continue;
            } else {
                entries.add(data.mEntries[i].toString());
            }
        }

        for (int i = 0; i < data.mEntryValues.length; i++) {
            if (i == 0) {
                continue;
            } else {
                entryValues.add(data.mEntryValues[i].toString());
            }
        }

        data.mDefaultValue = data.mRestorageValue = mContext
                .getString(R.string.pref_video_quality_medium);
        data.mEntries = entries.toArray(new String[0]);
        data.mEntryValues = entryValues.toArray(new String[0]);
        if (mOriginalVideoQualityBackValueString.equals(mContext
                .getString(R.string.pref_video_quality_large))) {
            set(Keys.KEY_VIDEO_QUALITY_BACK,
                    mContext.getString(R.string.pref_video_quality_medium));
        } else {
            set(Keys.KEY_VIDEO_QUALITY_BACK,
                    mOriginalVideoQualityBackValueString);
        }

        keylist.add(key);
    }

    private void restoreVideoQualityBackStruct(String key, Set<String> keylist) {
        if (originalVideoQualityBackDataStorageStruct != null) {
            mSupportDataMap.put(key,
                    originalVideoQualityBackDataStorageStruct);
            keylist.add(key);
        }
    }
    private boolean isFourKSelected() {
        return CamcorderProfile.hasProfile(mDataSetting.mCameraID, CamcorderProfile.QUALITY_2160P);
    }
    /* @} */
    private void setMutexEoisDvBack(String key, String entryValue,
            Set<String> keyList) {
        if (getBoolean(Keys.KEY_EOIS_DV_BACK)) {
            if (CamcorderProfile.hasProfile(mDataSetting.mCameraID, CamcorderProfile.QUALITY_2160P)) {
                if (getString(Keys.KEY_VIDEO_QUALITY_BACK).equals("large")) {
                    continueSetMutex(Keys.KEY_VIDEO_QUALITY_BACK, "medium", keyList,
                            "back eis mutex with 4k");
                }
            }
            // mutex with video beauty
            if(getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED)){
                continueSetMutex(Keys.KEY_VIDEO_BEAUTY_ENTERED, false, keyList,
                        "back eois mutex with video beauty");
            }
        } else {
            continueSetRestore(Keys.KEY_VIDEO_QUALITY_BACK, keyList,
                    "back eis resotre 4k");
            continueSetRestore(Keys.KEY_VIDEO_BEAUTY_ENTERED, keyList,
                    "back eois resotre video beauty");
        }
    }

    // SPRD Bug 1087282: mutex between 4K & eois (back)
    private void setMutexVideoQualityBack(String key, String entryValue,
                                    Set<String> keyList) {
        if (isFourKSelected()) {
            if (getString(Keys.KEY_VIDEO_QUALITY_BACK).equals("large")) {
                // mutex with eois
                if (getBoolean(Keys.KEY_EOIS_DV_BACK)) {
                    continueSetMutex(Keys.KEY_EOIS_DV_BACK, false, keyList,
                            "4k mutex with back eois");
                }

                // mutex with video beauty
                if (getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED)) {
                    continueSetMutex(Keys.KEY_VIDEO_BEAUTY_ENTERED, false, keyList,
                            "4k mutex with video beauty");
                }
            } else {
                continueSetRestore(Keys.KEY_EOIS_DV_BACK, keyList,
                        "4k resotre back eois");
                continueSetRestore(Keys.KEY_VIDEO_BEAUTY_ENTERED, keyList,
                        "4k resotre video beauty");
            }
        }
    }

    @Override
    public void initializeStaticParams(CameraProxy proxy) {
        super.initializeStaticParams(proxy);
        if (loader != null) {
            Set<String> keyList = new ArraySet<String>();
            // video qualities
            if (mDataSetting.mIsFront) {
                if (mPictureSizes.videoQualitiesFront == null) {
                    mPictureSizes.videoQualitiesFront = loader
                            .loadFrontVideoQualities(mDataSetting.mCameraID);
                    // front picture size
                    setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_FRONT,
                            mPictureSizes.videoQualitiesFront.orNull());
                    keyList.add(Keys.KEY_VIDEO_QUALITY_FRONT);
                }
                if(mPictureSizes.videoQualitiesFront3D == null && CameraUtil.isTDVideoEnable()){
                    mPictureSizes.videoQualitiesFront3D = loader
                            .loadFrontVideoQualities3D(CameraUtil.TD_CAMERA_ID);
                    // front picture size
                    setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_FRONT_3D,
                            mPictureSizes.videoQualitiesFront3D.orNull());
                    keyList.add(Keys.KEY_VIDEO_QUALITY_FRONT_3D);
                }
            } else {
                int currentCameraId = proxy.getCameraId();
                if (mPictureSizes.videoQualitiesBack == null || lastCameraId != currentCameraId) {
                    mPictureSizes.videoQualitiesBack  = loader.loadBackVideoQualities(currentCameraId);
                    // back picture size
                    lastCameraId = currentCameraId;
                    setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_BACK,
                            mPictureSizes.videoQualitiesBack.orNull());
                    keyList.add(Keys.KEY_VIDEO_QUALITY_BACK);
                }
                if (mPictureSizes.videoQualitiesBackMacro == null && CameraUtil.isMacroVideoEnable()){
                    mPictureSizes.videoQualitiesBackMacro = loader
                            .loadBackVideoQualitiesMacro(CameraUtil.BACK_MACRO_CAMERA_ID);
                    // back picture size
                    setEVSVideoQualities(Keys.KEY_VIDEO_QUALITY_BACK_MACRO,
                            mPictureSizes.videoQualitiesBackMacro.orNull());
                    keyList.add(Keys.KEY_VIDEO_QUALITY_BACK_MACRO);
                }
            }

            if (mSlowMotion == null) {
                mSlowMotion = CameraPictureSizesCacher
                        .getCacheSlowMotionForCamera(mContext);
                if (null == mSlowMotion) {
                    mSlowMotion = CameraPictureSizesCacher
                            .getSlowMotionForCamera(mContext, proxy);
                }
                setSlowMotion(Keys.KEY_VIDEO_SLOW_MOTION);
                keyList.add(Keys.KEY_VIDEO_SLOW_MOTION);
            }
            if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)){
                changeAndNotify(Keys.KEY_MAKE_UP_DISPLAY, getString(Keys.KEY_MAKE_UP_DISPLAY) );
            }
            if(getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED)){
                changeAndNotify(Keys.KEY_VIDEO_BEAUTY_ENTERED, getString(Keys.KEY_VIDEO_BEAUTY_ENTERED) );
            }
            if(getBoolean(Keys.KEY_FILTER_VIDEO_DISPLAY)){
                changeAndNotify(Keys.KEY_FILTER_VIDEO_DISPLAY, getString(Keys.KEY_FILTER_VIDEO_DISPLAY) );
            }
            if (!isSupportedAntibandAutoInited && (proxy != null)) {
                mSupportedAntibandAutoEnable = proxy.getCapabilities().getSupportedAntibandAutoEnable();
                isSupportedAntibandAutoInited = true;
                setAntibanding();
                keyList.add(Keys.KEY_VIDEO_ANTIBANDING);
            }
            if (keyList.size() > 0) {
                notifyKeyChange(keyList);
            }
        }

        // front flash mode
        setFrontFlash(proxy);
        updateLevelSwitch();
        // set flash level
        setFlashLevel(proxy);
        updateFilterVideoSwitch();
    }
    private void updateLevelSwitch() {
        mSupportedLevelEnable = CameraUtil.isLevelEnabled();
        Log.i(TAG, "isSupportedLevelEnable = " + mSupportedLevelEnable);
        if (!mSupportedLevelEnable) {
            mSupportDataMap.remove(Keys.KEY_ADD_LEVEL);
        }
    }
    private void setFlashLevel(CameraProxy proxy) {
        if (proxy == null) return; // Bug 1159267 (FORWARD_NULL)
        CameraCapabilities capabilities = proxy.getCapabilities();
        if (isEnableSettingConfig(Keys.KEY_VIDEO_FLASH_MODE)) {
            if (CameraUtil.isFlashLevelSupported(capabilities)) {
                int supportedFlashLevel = capabilities.getSupportedFlashLevel();

                int minValue = 1;
                int maxValue = supportedFlashLevel;
                int defaultValue = 1;

                DataStorageStruct data = new DataStorageStruct();

                data.setDefaults(defaultValue);
                data.mKey = Keys.KEY_ADJUST_FLASH_LEVEL;
                data.mStorePosition = DataConfig.SettingStoragePosition.positionList[3];
                int length = maxValue;
                data.mEntryValues = new CharSequence[length];
                data.mEntries = new CharSequence[length];

                for (int i = 0; i < length; i++) {
                    data.mEntries[i] = String.valueOf(minValue + i);
                    data.mEntryValues[i] = String.valueOf(minValue + i);
                }

                mSupportDataMap.put(Keys.KEY_ADJUST_FLASH_LEVEL, data);
            }
        }
    }
    private void setFrontFlash(CameraProxy proxy) {
        if(mDataSetting.mIsFront && proxy != null){ // Bug 1159267 (FORWARD_NULL)
            if (CameraUtil.isFlashSupported(proxy.getCapabilities(), proxy.getCharacteristics().isFacingFront())) {
                switch (CameraUtil.getFrontFlashMode()) {
                    case CameraUtil.VALUE_FRONT_FLASH_MODE_LCD:
                        mSupportDataMap.remove(Keys.KEY_VIDEO_FLASH_MODE);
                        // default data setting in xml
                        break;
                    case CameraUtil.VALUE_FRONT_FLASH_MODE_LED:
                        break;
                    }
            } else {
                // remove the front flash data setting
                mSupportDataMap.remove(Keys.KEY_VIDEO_FLASH_MODE);
            }
        }
    }
    private void updateFilterVideoSwitch() {
        mSupportedFilterVideoEnable = CameraUtil.isFilterVideoEnable();
        Log.i(TAG, "isSupportedFilterVideoEnable = " + mSupportedFilterVideoEnable);
        if (!mSupportedFilterVideoEnable) {
            mSupportDataMap.remove(Keys.KEY_FILTER_VIDEO_DISPLAY);
        }
    }
}
