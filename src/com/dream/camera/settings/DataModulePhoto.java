package com.dream.camera.settings;

import android.content.Context;
import android.graphics.Camera;
import android.util.ArraySet;
import android.util.Log;

import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.dream.camera.settings.DataConfig.CategoryType;
import com.dream.camera.util.DreamUtil;

import java.util.Set;

public class DataModulePhoto extends DataModuleInterfacePV {

    public static final String TAG = "DataModulePhoto";

    private boolean isSupportedSmileInited = false;
    private boolean mSupportedSmileEnable = false;
    private boolean isSupportedAttributesInited = false;
    private boolean mSupportedAttributesEnable = false;
    private boolean isAuto3DnrFromCapabilities = false;
    private boolean mSupportedAISceneEnable = false;
    private boolean mSupportedLogoWaterMarkEnable = false;
    private boolean mSupportedTimeWaterMarkEnable = false;
    private boolean mSupportedLevelEnable = false;

    public DataModulePhoto(Context context) {
        super(context);
        mCategory = CategoryType.CATEGORY_PHOTO;
        mDefaultStorePosition = DataConfig.SettingStoragePosition.positionList[2];
    }

    @Override
    public void changeCurrentModule(DataStructSetting dataSetting) {
        super.changeCurrentModule(dataSetting);
        originalAIDetectDataStorageStruct = null;
        // Change the handle of sharepreference
        changSPB(dataSetting);
        initializeData(dataSetting);
    }

    @Override
    protected void setMutex(String key, Object newValue, Set<String> keyList) {
        String entryValue = getString(key);

        switch (key) {
        case Keys.KEY_AUTO_3DNR_PARAMETER:
            setMutexAuto3Dnr(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_FACE_DATECT:
            setMutexAIDetect(key, entryValue, keyList);
            break;
        case Keys.KEY_FLASH_MODE:
            setMutexFlash(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_HDR:
            setMutexHDR(key, entryValue, keyList);
            break;
        //case Keys.KEY_CAMERA_HDR_NORMAL_PIC:
            //setMutexNormalHDR(key, entryValue, keyList);
            //break;
        case Keys.KEY_SCENE_MODE:
            setMutexSceneMode(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_BEAUTY_ENTERED:
            setMutexBeauty(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_AI_BEAUTY_ENTERED:
            setMutexAiBeauty(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_ZSL:
            setMutexZSL(key, entryValue, keyList);
            break;
        case Keys.KEY_HIGH_ISO:
            setMutexHighISO(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_FILTER_TYPE:
            setMutexFilterType(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE:
            setMutexPortraitBackgroundPeplacementType(key, entryValue, keyList);
            break;
        case Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE:
            setMutexGifPhotoFlash(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_AI_SCENE_DATECT:
            setMutexAISceneDetect(key, entryValue, keyList);
            break;
        case Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH:
             setMutexTouchingPhoto(key, entryValue, keyList);
             break;
        case Keys.KEY_AUTO_TRACKING:
            setMutexAutoTracking(key, entryValue, keyList);
            break;
        case Keys.KEY_MAKE_UP_DISPLAY:
            setMutexMakeUpDisplay(key, entryValue, keyList);
            break;
        case Keys.KEY_LIGHT_PORTIAIT_DISPLAY:
            setMutexLightPortraitDisplay(key, entryValue, keyList);
            break;
        case Keys.KEY_LIGHT_PORTIAIT:
            if(CameraUtil.isPortraitAndRefocusMutex()){
                setMutexLightPortrait(key, entryValue, keyList);
            }
            break;
        case Keys.KEY_PORTRAIT_REFOCUS_KEY:
        case Keys.KEY_PORTIAIT_REFORCUS_CLOSE:
            if(CameraUtil.isPortraitAndRefocusMutex()){
                setMutexPortraitRefocus(key, entryValue, keyList);
            }
            break;
        case Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY:
            setMutexColorEffectPhotoDisplay(key, entryValue, keyList);
            break;
        case Keys.KEY_AE_LOCK:
            setMutexAELock(key, entryValue, keyList);
            break;
        case Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY:
            if (CameraUtil.isPortraitRefocusEnable()) {
                setMutexPortraitRefocusDisplay(key, entryValue, keyList);
            }
            break;
        /* @} */
        default:
            break;
        }

    }

    private void setMutexAELock(String key, String entryValue, Set<String> keyList) {
        if(getBoolean(Keys.KEY_AE_LOCK)){
            if (!"off".equals(getString(Keys.KEY_FLASH_MODE))) {
                continueSetMutex(Keys.KEY_FLASH_MODE, "off", keyList,
                        "AELock mutex with flash");
            }
        }
    }

    private void setMutexLightPortrait(String key, String entryValue, Set<String> keyList) {
        if(getInt(Keys.KEY_LIGHT_PORTIAIT) != 0){
            continueSetMutex(Keys.KEY_PORTRAIT_REFOCUS_KEY,false,keyList,
                    "light portrait mutex with refocus");
            if (CameraUtil.isPortraitRefocusEnable()) {
                continueSetMutex(Keys.KEY_PORTIAIT_REFORCUS_CLOSE,false,keyList,
                        "light portrait mutex with refocus");
            }
        }
    }


    private void setMutexPortraitRefocus(String key, String entryValue, Set<String> keyList) {
        if(getBoolean(Keys.KEY_PORTRAIT_REFOCUS_KEY)){
            continueSetMutex(Keys.KEY_LIGHT_PORTIAIT,0,keyList,
                    "light portrait display mutex with make up display");
        }
        if(CameraUtil.isPortraitRefocusEnable() && getBoolean(Keys.KEY_PORTIAIT_REFORCUS_CLOSE)){
            continueSetMutex(Keys.KEY_LIGHT_PORTIAIT,0,keyList,
                    "refocus mutex with light portrait");
        }
    }


    private void setMutexColorEffectPhotoDisplay(String key, String entryValue,
                                            Set<String> keyList) {
        if(getBoolean(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY)){
            continueSetMutex(Keys.KEY_MAKE_UP_DISPLAY,false,keyList,
                    "color effect photo display mutex with make up display");
            if(!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    getString(Keys.KEY_CAMERA_COLOR_EFFECT)))
                continueSetMutex(Keys.KEY_CAMERA_BEAUTY_ENTERED, false, keyList,
                        "color mutex with beauty");
        }
    }

    private void setMutexPortraitRefocusDisplay(String key, String entryValue, Set<String> keyList) {
        if(getBoolean(Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY)){
            continueSetMutex(Keys.KEY_MAKE_UP_DISPLAY,false,keyList,
                    "portrait refocus display mutex with make up display");
            continueSetMutex(Keys.KEY_LIGHT_PORTIAIT_DISPLAY,false,keyList,
                    "portrait refocus display mutex with light portrait display");
        }
    }

    private void setMutexLightPortraitDisplay(String key, String entryValue, Set<String> keyList) {
        if(getBoolean(Keys.KEY_LIGHT_PORTIAIT_DISPLAY)){
            continueSetMutex(Keys.KEY_MAKE_UP_DISPLAY,false,keyList,
                    "light portrait display mutex with make up display");
            if (CameraUtil.isPortraitRefocusEnable()) {
                continueSetMutex(Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY,false,keyList,
                        "light portrait display mutex with portrait refocus display");
            }
        }
    }

    private void setMutexMakeUpDisplay(String key, String entryValue, Set<String> keyList) {
        if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)){
            continueSetMutex(Keys.KEY_LIGHT_PORTIAIT_DISPLAY,false,keyList,
                    "make up display mutex with light portrait display");
            if (CameraUtil.isPortraitRefocusEnable()) {
                continueSetMutex(Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY, false, keyList,
                        "lmake up display mutex with portrait refocus display");
            }
        }
        if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)){
            if(getBoolean(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY)) {
                continueSetMutex(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY, false, keyList,
                        "make up display mutex with color effect photo display");
            }
            if(!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    entryValue)) {
                continueSetMutex(
                        Keys.KEY_CAMERA_COLOR_EFFECT,
                        mContext.getString(R.string.pref_camera_color_effect_entry_value_none),
                        keyList, "beauty mutex with color effect");
            }
        }
    }

    private void setMutexGifPhotoFlash(String key, String entryValue,
            Set<String> keyList) {
        // GIF FLASH --- PHOTO FLASH SYNC
        if(getString(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE).equals("torch")){
            continueSetMutex(Keys.KEY_FLASH_MODE, "on", keyList,
                    "gif flash sync with photo flash");
        }else{
            continueSetMutex(Keys.KEY_FLASH_MODE, "off", keyList,
                    "gif flash sync with photo flash");
        }

    }

    private String mOriginalAIValueString = null;
    private void setMutexFilterType(String key, String entryValue,
            Set<String> keyList) {
        int filterTypeValue = getInt(Keys.KEY_CAMERA_FILTER_TYPE);
        Log.i(TAG, "filter type value = " + filterTypeValue);
        if (101 == filterTypeValue ) {
            // Filter - AI
            mOriginalAIValueString = getString(Keys.KEY_CAMERA_FACE_DATECT);
            if (!mContext.getString(R.string.pref_ai_detect_entry_value_off)
                    .equals(getString(Keys.KEY_CAMERA_FACE_DATECT))) {
                continueSetMutex(
                        Keys.KEY_CAMERA_FACE_DATECT,
                        mContext.getString(R.string.pref_ai_detect_entry_value_off),
                        keyList, "filter mutex with ai");
            }
            replaceAIDataStruct(R.array.pref_camera_face_detect_off_key_array, keyList);

        } else {
            restoreAIDataStruct(keyList);
            continueSetRestore(Keys.KEY_CAMERA_FACE_DATECT, keyList,
                    "filter restore ai");
        }
    }
    private void setMutexPortraitBackgroundPeplacementType(String key, String entryValue,
                                    Set<String> keyList) {
        int portraitbackgroundreplacementTypeValue = getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        Log.i(TAG, "portraitbackgroundreplacement type value = " + portraitbackgroundreplacementTypeValue);
        if (101 == portraitbackgroundreplacementTypeValue ) {
            // Filter - AI
            mOriginalAIValueString = getString(Keys.KEY_CAMERA_FACE_DATECT);
            if (!mContext.getString(R.string.pref_ai_detect_entry_value_off)
                    .equals(getString(Keys.KEY_CAMERA_FACE_DATECT))) {
                continueSetMutex(
                        Keys.KEY_CAMERA_FACE_DATECT,
                        mContext.getString(R.string.pref_ai_detect_entry_value_off),
                        keyList, "portraitbackgroundreplacement mutex with ai");
            }
            replaceAIDataStruct(R.array.pref_camera_face_detect_off_key_array, keyList);

        } else {
            restoreAIDataStruct(keyList);
            continueSetRestore(Keys.KEY_CAMERA_FACE_DATECT, keyList,
                    "portraitbackgroundreplacement restore ai");
        }
    }

    private void setMutexHighISO(String key, String entryValue,
            Set<String> keyList) {
        // SPRD:Add for highISO Mutex
        if (getBoolean(Keys.KEY_HIGH_ISO)) {
            // highISO --- zsl
            if (!"0".equals(getString(Keys.KEY_CAMERA_ZSL))) {
                continueSetMutex(
                        Keys.KEY_CAMERA_ZSL,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "highISO mutex with zsl");
            }

            // highISO - FLASH
            if (!"off".equals(getString(Keys.KEY_FLASH_MODE))) {
                continueSetMutex(Keys.KEY_FLASH_MODE, "off", keyList,
                        "highISO mutex with flash");
            }
        } else {
            continueSetRestore(Keys.KEY_CAMERA_ZSL, keyList,
                    "highISO restore zsl");
            continueSetRestore(Keys.KEY_FLASH_MODE, keyList,
                    "highISO restore falsh");
        }
    }

    private void setMutexZSL(String key, String entryValue, Set<String> keyList) {
        if (getBoolean(Keys.KEY_CAMERA_ZSL)) {

            // HDR MUTEX
            if (getBoolean(Keys.KEY_CAMERA_HDR)) {
                Log.e(TAG, "setMutexFlash   current hdr value = " + true
                        + "  need set mutex");
                continueSetMutex(Keys.KEY_CAMERA_HDR, false, keyList,
                        "zsl mutex with hdr");
            }

            // SPRD Add ZSL-highISO
            if (getBoolean(Keys.KEY_HIGH_ISO)) {
                continueSetMutex(Keys.KEY_HIGH_ISO, false, keyList,
                        "zsl mutex with highISO");
            }
        } else {
            continueSetRestore(Keys.KEY_CAMERA_HDR, keyList, "zsl restore hdr");
            continueSetRestore(Keys.KEY_HIGH_ISO, keyList,
                    "zsl restore highISO");
        }
    }

    private void setMutexSceneMode(String key, String entryValue,
            Set<String> keyList) {

        if (!"auto".equals(getString(Keys.KEY_SCENE_MODE))) {
            // SCENE MODE --- MUTEX FLASH
            if (!"off".equals(getString(Keys.KEY_FLASH_MODE))) {
                continueSetMutex(Keys.KEY_FLASH_MODE, "off", keyList,
                        "scene mode mutex with flash");
            }
        } else {
            continueSetRestore(Keys.KEY_FLASH_MODE, keyList,
                    "scene mode restore flash");
        }
    }

    /**
     * SPRD:fix bug840683 break the relation between hdr and hdr_normal
    private void setMutexNormalHDR(String key, String entryValue, Set<String> keyList) {
        if (getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC)) {
            if(!getBoolean(Keys.KEY_CAMERA_HDR)){
                continueSetMutex(Keys.KEY_CAMERA_HDR, true, keyList,
                        "hdr mutex with normal");
            }
        } else {
            continueSetRestore(Keys.KEY_CAMERA_HDR, keyList,
                    "hdr restore normal");
        }
    }
     */

    private void setMutexHDR(String key, String entryValue, Set<String> keyList) {
        /**
         * SPRD:fix bug840683 break the relation between hdr and hdr_normal
        if ("0".equals(entryValue)) {
            if (getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC)) {
                continueSetMutex(Keys.KEY_CAMERA_HDR_NORMAL_PIC, false, keyList,
                        "hdr mutex with zsl");
            }
        }
         */
        // HDR --- MUTEX WITH FLASH & COLOR EFFECT
        if (!"0".equals(entryValue)) {
            // HDR - FLASH
            if (!"off".equals(getString(Keys.KEY_FLASH_MODE))) {
                continueSetMutex(Keys.KEY_FLASH_MODE, "off", keyList,
                        "hdr mutex with flash");
            }

            // HDR - COLOR EFFECT
            if (!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    getString(Keys.KEY_CAMERA_COLOR_EFFECT))) {
                continueSetMutex(
                        Keys.KEY_CAMERA_COLOR_EFFECT,
                        mContext.getString(R.string.pref_camera_color_effect_entry_value_none),
                        keyList, "hdr mutex with color effect");
            }

            // HDR - ZSL
            if (!CameraUtil.isHdrZslEnable()) {
                if (getBoolean(Keys.KEY_CAMERA_ZSL)) {
                    continueSetMutex(Keys.KEY_CAMERA_ZSL, false, keyList,
                            "hdr mutex with zsl");
                }
            }

            // HDR - AUTO3DNR
            if (getBoolean(Keys.KEY_AUTO_3DNR_PARAMETER)) {
                continueSetMutex(Keys.KEY_AUTO_3DNR_PARAMETER, false, keyList,
                        "hdr mutex with auto3dnr");
            }
        }
        // RESTORE
        else {
            if (!CameraUtil.isHdrZslEnable()) {
                continueSetMutex(Keys.KEY_CAMERA_ZSL , CameraUtil.isZslEnable() , keyList , "hdr mutex with zsl");
            }

            continueSetRestore(Keys.KEY_FLASH_MODE, keyList,
                    "hdr restore falsh");
            continueSetRestore(Keys.KEY_CAMERA_COLOR_EFFECT, keyList,
                    "hdr restore color effect");
            continueSetRestore(Keys.KEY_CAMERA_ZSL, keyList,
                    "hdr restore zsl");
            continueSetRestore(Keys.KEY_AUTO_3DNR_PARAMETER, keyList,
                    "hdr restore auto3dnr");
        }

    }

    private void setMutexFlash(String key, String entryValue,
            Set<String> keyList) {
        // FLASH --- MUTEX WITH HDR
        if (!"off".equals(entryValue)) {
            // FLASH - HDR
            if (getBoolean(Keys.KEY_CAMERA_HDR)) {
                continueSetMutex(Keys.KEY_CAMERA_HDR, false, keyList,
                        "flash mutex with hdr");
            }

            // FLASH - SCENE MODE
            if (!"auto".equals(getString(Keys.KEY_SCENE_MODE))) {
                continueSetMutex(Keys.KEY_SCENE_MODE, "auto", keyList,
                        "flash mutex with scene mode");
            }

            // FLASH-highISO
            if (getBoolean(Keys.KEY_HIGH_ISO)) {
                continueSetMutex(Keys.KEY_HIGH_ISO, false, keyList,
                        "flash mutex with highISO");
            }
        }
        // RESTORE
        else {
            continueSetRestore(Keys.KEY_CAMERA_HDR, keyList,
                    "flash restore hdr");
            continueSetRestore(Keys.KEY_SCENE_MODE, keyList,
                    "flash restore scene mode");
            continueSetRestore(Keys.KEY_HIGH_ISO, keyList,
                    "flash restore highISO");
        }

        if(!entryValue.equals("on")){
            continueSetMutex(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE, "off", keyList, "sync photo flash with gif flash");
        }else {
            continueSetMutex(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE, "torch", keyList, "sync photo flash with gif flash");
        }
    }


    private void setMutexAuto3Dnr(String key, String entryValue,
            Set<String> keyList) {

        //Auto3Dnr - HDR
        if (getBoolean(Keys.KEY_CAMERA_HDR)) {
            continueSetMutex(Keys.KEY_CAMERA_HDR, false, keyList,
                        "auto3dnr mutex with hdr");
            } else {
            continueSetRestore(Keys.KEY_CAMERA_HDR, keyList,
                    "auto3dnr resotre hdr");
        }
    }

    private void setMutexAIDetect(String key, String entryValue,
            Set<String> keyList) {
        // FACE --- MUTEX WITH color effect
        if (mContext.getString(R.string.pref_ai_detect_entry_value_face).equals(entryValue) || mContext.getString(R.string.pref_ai_detect_entry_value_smile).equals(entryValue)) {
            //FACE -MUTEX WITH auto tracking
            if (!mContext.getString(R.string.preference_switch_item_default_value_false).equals(
                    getString(Keys.KEY_AUTO_TRACKING))) {
                continueSetMutex(Keys.KEY_AUTO_TRACKING,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "ai mutex with auto tracking");
            }
        }
    }

    private void setMutexAISceneDetect(String key, String entryValue,
                                       Set<String> keyList) {
        //AI Sence Detect -- mutex with auto tracking
        if (mContext.getString(R.string.preference_switch_item_default_value_true)
                .equals(entryValue)) {
            if (!mContext.getString(R.string.preference_switch_item_default_value_false).equals(
                    getString(Keys.KEY_AUTO_TRACKING))) {
                continueSetMutex(Keys.KEY_AUTO_TRACKING,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "ai sence detect mutex with auto tracking");
            }
        }
    }

    private void setMutexTouchingPhoto(String key, String entryValue,
                                       Set<String> keyList) {
        //touching photograph -- mutex with auto tracking
        if (mContext.getString(R.string.preference_switch_item_default_value_true)
                .equals(entryValue)) {
            if (!mContext.getString(R.string.preference_switch_item_default_value_false).equals(
                    getString(Keys.KEY_AUTO_TRACKING))) {
                continueSetMutex(Keys.KEY_AUTO_TRACKING,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "touching photograph mutex with auto tracking");
            }
        }
    }

    private void setMutexAutoTracking(String key, String entryValue,
                                      Set<String> keyList) {
        if (mContext.getString(R.string.preference_switch_item_default_value_true)
                .equals(entryValue)) {
            //auto tracking --- mutex with ai detect
            if (mContext.getString(R.string.pref_ai_detect_entry_value_face).equals(getString(Keys.KEY_CAMERA_FACE_DATECT))
                    || mContext.getString(R.string.pref_ai_detect_entry_value_smile).equals(getString(Keys.KEY_CAMERA_FACE_DATECT)))
                continueSetMutex(Keys.KEY_CAMERA_FACE_DATECT,
                        mContext.getString(R.string.pref_ai_detect_entry_value_off),
                        keyList, "auto tracking mutex with ai detect");
            //auto tracking --- mutex with touching photograph
            if (mContext.getString(R.string.preference_switch_item_default_value_true).equals(getString(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)))
                continueSetMutex(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "auto tracking mutex with touching photograph");
            //auto tracking --- mutex with beauty
            if (getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
                continueSetMutex(Keys.KEY_CAMERA_BEAUTY_ENTERED,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "auto tracking mutex with beauty");
            }
            //auto tracking --- mutex with ai sence detect
            if (mContext.getString(R.string.preference_switch_item_default_value_true).equals(getString(Keys.KEY_CAMERA_AI_SCENE_DATECT))) {
                continueSetMutex(Keys.KEY_CAMERA_AI_SCENE_DATECT,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "auto tracking mutex with ai sence detect");
            }
        }

    }
    private DataStorageStruct originalAIDetectDataStorageStruct = null;

    private void replaceAIDataStruct(int arrayResourceID, Set<String> keylist) {
        originalAIDetectDataStorageStruct = (DataStorageStruct) mSupportDataMap
                .get(Keys.KEY_CAMERA_FACE_DATECT);
        if(mOriginalAIValueString != null){
            originalAIDetectDataStorageStruct.mRestorageValue = mOriginalAIValueString;
        }
        DataStorageStruct struct = generateSingleDataStorageStruct(arrayResourceID);
        mSupportDataMap.put(Keys.KEY_CAMERA_FACE_DATECT, struct);
        keylist.add(Keys.KEY_CAMERA_FACE_DATECT);
    }

    private void restoreAIDataStruct(Set<String> keylist) {
        if (originalAIDetectDataStorageStruct != null) {
            mSupportDataMap.put(Keys.KEY_CAMERA_FACE_DATECT,
                    originalAIDetectDataStorageStruct);
            keylist.add(Keys.KEY_CAMERA_FACE_DATECT);
        }
    }

    private void setMutexBeauty(String key, String entryValue,
            Set<String> keyList) {
        if (getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
            //Beauty -- mutex with auto tracking
            if (!mContext.getString(R.string.preference_switch_item_default_value_false).equals(
                    getString(Keys.KEY_AUTO_TRACKING))) {
                continueSetMutex(Keys.KEY_AUTO_TRACKING,
                        mContext.getString(R.string.preference_switch_item_default_value_false),
                        keyList, "beauty mutex with auto tracking");
            }
        } else {
            continueSetRestore(Keys.KEY_CAMERA_COLOR_EFFECT, keyList,
                    "beauty restore color effect");
        }
    }

    // Bug 1018708 - hide or show BeautyButton on topPanel
    private void setMutexAiBeauty(String key, String entryValue,
                                Set<String> keyList) {
        if (getBoolean(Keys.KEY_CAMERA_AI_BEAUTY_ENTERED)) {
            if (!mContext.getString(
                    R.string.pref_camera_color_effect_entry_value_none).equals(
                    getString(Keys.KEY_CAMERA_COLOR_EFFECT))) {
                continueSetMutex(
                        Keys.KEY_CAMERA_COLOR_EFFECT,
                        mContext.getString(R.string.pref_camera_color_effect_entry_value_none),
                        keyList, "ai beauty mutex with color effect");
            }
        } else {
            continueSetRestore(Keys.KEY_CAMERA_COLOR_EFFECT, keyList,
                    "ai beauty restore color effect");
        }
    }

    @Override
    protected int showToast(String key, String oldValue, String newValue) {
        int toastResId = -1;

        return toastResId;
    }

    @Override
    protected void fillEntriesAndSummaries() {
        if (mPictureSizes != null) {
            // front picture size
            setEVSPicturesize(Keys.KEY_PICTURE_SIZE_FRONT,
                    CameraUtil.filterSize(
                            mPictureSizes.frontCameraSizes,
                            Keys.KEY_PICTURE_SIZE_FRONT,
                            mDataSetting.mMode, mContext));

            // back picture size
            setEVSPicturesize(Keys.KEY_PICTURE_SIZE_BACK,
                    CameraUtil.filterSize(
                            mPictureSizes.backCameraSizes,
                            Keys.KEY_PICTURE_SIZE_BACK,
                            mDataSetting.mMode, mContext));
        }

        /* SPRD:Fix bug 447953 @{ */
        setEVSAIDetect();

        setAntibanding();

    }

    private void updateProfessional() {
        if(!CameraUtil.isManualShutterEnabled()) {
            mSupportDataMap.remove(Keys.KEY_EXPOSURE_SHUTTERTIME);
        }
        if(!CameraUtil.isManualFocusEnabled()) {
            mSupportDataMap.remove(Keys.KEY_CAMERA_FOCUS_DISTANCE);
        }
        setManualFlash();
    }

    private void setManualFlash() {
        if (isEnableSettingConfig(Keys.KEY_FLASH_MODE) && (mChildModule == SettingsScopeNamespaces.MANUAL)) {
            DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_FLASH_MODE);
            if (data == null) {
                Log.i(TAG, "setManualFlash() KEY_FLASH_MODE data == null");
                return;
            }
            data.mEntryValues = mContext.getResources().getTextArray(
                    R.array.pref_camera_manual_flashmode_entryvalues);
            data.mEntries = mContext.getResources().getTextArray(
                        R.array.pref_camera_manual_flashmode_entryvalues);
            data.mDefaultValue = data.mEntryValues[0].toString();
            Set<String> keySet = new ArraySet<String>();
            keySet.add(Keys.KEY_FLASH_MODE);
            notifyKeyChange(keySet);
        }
    }

    private void setHDR(CameraProxy proxy) {
        /*Bug 1182989 if not support hdr, remove her setting*/
        if (!proxy.getCapabilities().supports(CameraCapabilities.SceneMode.HDR)) {
            mSupportDataMap.remove(Keys.KEY_CAMERA_HDR);
            return;
        }
        if (isEnableSettingConfig(Keys.KEY_CAMERA_HDR) &&
                !CameraUtil.isAutoHDRSupported(proxy.getCapabilities())) {
            DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_CAMERA_HDR);
            if (data == null) {
                Log.i(TAG, "setFrontFlashLED() KEY_FLASH_MODE data == null");
                return;
            }

            CharSequence[] entryValues = new CharSequence[data.mEntryValues.length - 1];

            CharSequence[] entries = new CharSequence[data.mEntries.length - 1];

            System.arraycopy(data.mEntryValues, 0, entryValues, 0, data.mEntryValues.length - 1);
            System.arraycopy(data.mEntries, 0, entries, 0, data.mEntries.length - 1);

            data.mEntryValues = entryValues;
            data.mEntries = entries;
            return;
        }
    }

    private void setFrontFlash(CameraProxy proxy) {
        if(mDataSetting.mIsFront){
            if(CameraUtil.isFlashSupported(proxy.getCapabilities(), proxy.getCharacteristics().isFacingFront())){
                switch (CameraUtil.getFrontFlashMode()) {
                case CameraUtil.VALUE_FRONT_FLASH_MODE_LCD:
                    mSupportDataMap.remove(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);
                    // default data setting in xml
                    break;
                case CameraUtil.VALUE_FRONT_FLASH_MODE_LED:
                    setFrontFlashLED();
                    break;
                }
                Set<String> keySet = new ArraySet<String>();
                keySet.add(Keys.KEY_FLASH_MODE);
                notifyKeyChange(keySet);
            } else {
                // remove the front flash data setting
                mSupportDataMap.remove(Keys.KEY_FLASH_MODE);
                mSupportDataMap.remove(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);
            }
        }

    }

    private void setFrontFlashLED() {
        if (isEnableSettingConfig(Keys.KEY_FLASH_MODE)){
            DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_FLASH_MODE);
            if (data == null) {
                Log.i(TAG, "setFrontFlashLED() KEY_FLASH_MODE data == null");
                return;
            }
            data.mEntryValues = mContext.getResources().getTextArray(
                    R.array.pref_camera_flashmode_led_entryvalues);
            data.mEntries = mContext.getResources().getTextArray(
                        R.array.pref_camera_flashmode_led_entryvalues);
            data.mDefaultValue = data.mEntryValues[1].toString();
            return;
        }
    }

    private void setEVSAIDetect() {
        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_CAMERA_FACE_DATECT);
        if (!mSupportedSmileEnable && isSupportedSmileInited) {
            if (data == null) {
                return;
            }
            data.mEntries = mContext.getResources().getTextArray(
                    R.array.pref_camera_ai_detect_entries_removesmile);
            data.mEntryValues = mContext.getResources().getTextArray(
                    R.array.pref_camera_ai_detect_entryvalues_removesmile);
            Log.e(TAG, "setEVSAIDetect() " + data.toString());
        }

    }



    public Boolean getSupportedAttributesEnable() {
        return mSupportedAttributesEnable;
    }
    public Boolean getSmileEnable() {
        return mSupportedSmileEnable;
    }
    protected void setAntibanding(){
        if (!isEnableSettingConfig(Keys.KEY_CAMER_ANTIBANDING)){
            return;
        }
        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_CAMER_ANTIBANDING);
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

    /*SPRD:fix bug 622818 add for exposure to adapter auto @{*/
//    private void setExposureCompensationValue(CameraProxy proxy) {
//        if (proxy != null) {
//            if (!isEnableSettingConfig(Keys.KEY_EXPOSURE)){
//                return;
//            }
//
//            DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_EXPOSURE);
//            if (data == null) {
//                Log.i(TAG, "setExposureCompensationValue data == null");
//                return;
//            }
//
//            int minExposureCompensation = proxy.getCapabilities().getMinExposureCompensation();
//            int maxExposureCompensation = proxy.getCapabilities().getMaxExposureCompensation();
//            float exposureCompensationStep = proxy.getCapabilities().getExposureCompensationStep();
//            int step = (int)(1/exposureCompensationStep);
//
//            String[] entryValues = new String[(mContext.getResources().getTextArray(
//                    R.array.pref_camera_exposure_key_entryvalues)).length];
//
//            /* SPRD:fix bug861106 add for exposure  to adapter @{*/
//            int tempExposure = 0 - step;
//            for (int i = entryValues.length/2; i < entryValues.length &&  (tempExposure += step) <= maxExposureCompensation; i++) {
//                entryValues[i] = Integer.toString(tempExposure);
//            }
//            tempExposure = 0;
//            for (int i = entryValues.length/2 -1; i >= 0 &&  (tempExposure -= step) >= minExposureCompensation; i--) {
//                entryValues[i] = Integer.toString(tempExposure);
//            }
//            /* @} */
//            data.mEntryValues = entryValues;
//
//        }
//    }
    /* @} */

    @Override
    public void initializeStaticParams(CameraProxy proxy) {
        super.initializeStaticParams(proxy);
        if (loader != null) {
            Set<String> keyList = new ArraySet<String>();
            // picture size
            if (mDataSetting.mIsFront) {
                mPictureSizes.frontCameraSizes = loader
                        .loadFrontPictureSize(proxy.getCameraId(), proxy);
                // front picture size
                setEVSPicturesize(Keys.KEY_PICTURE_SIZE_FRONT,
                        CameraUtil.filterSize(
                                mPictureSizes.frontCameraSizes,
                                Keys.KEY_PICTURE_SIZE_FRONT,
                                    mDataSetting.mMode, mContext));
                keyList.add(Keys.KEY_PICTURE_SIZE_FRONT);
            } else {
                /*start : add w+t*/
                int cameraid = proxy.getCameraId();
                if (CameraUtil.isWPlusTEnable()){
                    cameraid = DreamUtil.getRightWPlusTCamera(cameraid);
                }
                /*end : add w+t*/
                mPictureSizes.backCameraSizes = loader.loadBackPictureSize(
                        cameraid, proxy);
                // back picture size
                setEVSPicturesize(Keys.KEY_PICTURE_SIZE_BACK,
                        CameraUtil.filterSize(
                                mPictureSizes.backCameraSizes,
                                Keys.KEY_PICTURE_SIZE_BACK,
                                mDataSetting.mMode, mContext));
                keyList.add(Keys.KEY_PICTURE_SIZE_BACK);
            }

            if(getBoolean(Keys.KEY_VIDEO_BEAUTY_ENTERED)){
                changeAndNotify(Keys.KEY_VIDEO_BEAUTY_ENTERED, getString(Keys.KEY_VIDEO_BEAUTY_ENTERED) );
            }

            if(getBoolean(Keys.KEY_MAKE_UP_DISPLAY)){
                changeAndNotify(Keys.KEY_MAKE_UP_DISPLAY, getString(Keys.KEY_MAKE_UP_DISPLAY) );
            }
            //setExposureCompensationValue(proxy);

            if (!isSupportedSmileInited) {
                mSupportedSmileEnable = proxy.getCapabilities().getSupportedSmileEnable();
                isSupportedSmileInited = true;
            }
            if (!isSupportedAttributesInited){
                mSupportedAttributesEnable = proxy.getCapabilities().getSupportedFaceAttributesEnable();
                isSupportedAttributesInited = true;
            }

            if (isSupportedAttributesInited && isSupportedSmileInited){
                setEVSAIDetect();
                keyList.add(Keys.KEY_CAMERA_FACE_DATECT);
            }

            if (!isSupportedAntibandAutoInited) {
                mSupportedAntibandAutoEnable = proxy.getCapabilities().getSupportedAntibandAutoEnable();
                isSupportedAntibandAutoInited = true;
                setAntibanding();
                keyList.add(Keys.KEY_CAMER_ANTIBANDING);
            }
            if(keyList.size() > 0){
                notifyKeyChange(keyList);
            }
        }

        // front flash mode
        setFrontFlash(proxy);

        updateProfessional();

        // set flash level
        setFlashLevel(proxy);

        updateAuto3DnrSwitch(proxy);

	// set hdr
        setHDR(proxy);
        updateSmileSwitch();
        updateFaceAttributesSwitch();
        updateAISceneSwitch(proxy);
        updateLogoWaterMarkSwitch(proxy);
        updateTimeWaterMarkSwitch(proxy);
        // update light portrait
        updateLightPortrait();
        updateFilterDefaultEffectIndex(proxy);
        updatePortraitRefocus();
        updateLevelSwitch();
    }

    private void updatePortraitRefocus() {
        if((mIsFront && !CameraUtil.isFrontPortraitBlurEnable())
                || (!mIsFront && !CameraUtil.isBackPortraitBlurEnable())){
            mSupportDataMap.remove(Keys.KEY_PORTRAIT_REFOCUS_KEY);
        }
    }

    private void updateLightPortrait() {
        if((mIsFront && !CameraUtil.isLightPortraitFrontEnable())
                || (!mIsFront && !CameraUtil.isLightPortraitEnable())){
            mSupportDataMap.remove(Keys.KEY_LIGHT_PORTIAIT_DISPLAY);
            mSupportDataMap.remove(Keys.KEY_LIGHT_PORTIAIT);
        }
    }

    private void updateFilterDefaultEffectIndex(CameraProxy proxy) { // add for distinguish BASE or PLUS
        if (CameraUtil.isUseSprdFilterPlus()) {
            DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(Keys.KEY_CAMERA_FILTER_TYPE);
            if (null != data) {
                data.setDefaults(0); //SPRD:Bug1467284
                Log.i(TAG, "filter plus default = " + data.getDefaultInteger());
            }
	}
    }

    private void updateAuto3DnrSwitch(CameraProxy proxy){
        isAuto3DnrFromCapabilities = proxy.getCapabilities().supportAuto3Dnr();
        Log.i(TAG, "isAuto3DnrFromCapabilities = " + isAuto3DnrFromCapabilities);
        if(!isAuto3DnrFromCapabilities || (CameraUtil.isFront4in1Sensor() && mDataSetting.mIsFront)
                || (CameraUtil.isBack4in1Sensor() && !mDataSetting.mIsFront)){
            mSupportDataMap.remove(Keys.KEY_AUTO_3DNR_PARAMETER);
        }
    }

    private void updateSmileSwitch() {
        Log.i(TAG, "isSupportedSmileEnable = " + mSupportedSmileEnable);
        if (!mSupportedSmileEnable) {
            mSupportDataMap.remove(Keys.KEY_AI_DETECT_SMILE);
        }
    }
    private void updateFaceAttributesSwitch() {
        Log.i(TAG, "isSupportedFaceAttributesEnable = " + mSupportedAttributesEnable);
        if (!mSupportedAttributesEnable) {
            mSupportDataMap.remove(Keys.KEY_AI_DETECT_FACE_ATTRIBUTES);
        }
    }
    private void updateLevelSwitch() {
        mSupportedLevelEnable = CameraUtil.isLevelEnabled();
        Log.i(TAG, "isSupportedLevelEnable = " + mSupportedLevelEnable);
        if (!mSupportedLevelEnable) {
            mSupportDataMap.remove(Keys.KEY_ADD_LEVEL);
        }
    }
    private void updateAISceneSwitch(CameraProxy proxy) {
        mSupportedAISceneEnable = proxy.getCapabilities().getSupportAiScene();
        Log.i(TAG, "isSupportedAISceneEnable = " + mSupportedAISceneEnable);
        if (!mSupportedAISceneEnable) {
            mSupportDataMap.remove(Keys.KEY_CAMERA_AI_SCENE_DATECT);
        }
    }
    private void updateLogoWaterMarkSwitch(CameraProxy proxy) {
        mSupportedLogoWaterMarkEnable = proxy.getCapabilities().getSupportLogoWatermark();
        Log.i(TAG, "isSupportedLogoWaterMarkEnable = " + mSupportedLogoWaterMarkEnable);
        if (!mSupportedLogoWaterMarkEnable) {
            mSupportDataMap.remove(Keys.KEY_AUTO_ADD_LOGOWATERMARK);
        }
    }
    private void updateTimeWaterMarkSwitch(CameraProxy proxy) {
        mSupportedTimeWaterMarkEnable = proxy.getCapabilities().getSupportTimeWatermark();
        Log.i(TAG, "isSupportedTimeWaterMarkEnable = " + mSupportedTimeWaterMarkEnable);
        if (!mSupportedTimeWaterMarkEnable) {
            mSupportDataMap.remove(Keys.KEY_AUTO_ADD_TIMEWATERMARK);
        }
    }
    private void setFlashLevel(CameraProxy proxy) {
        CameraCapabilities capabilities = proxy.getCapabilities();
        if (isEnableSettingConfig(Keys.KEY_FLASH_MODE)) {
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

    @Override
    public void destroy(){
        Set<String> keyList = new ArraySet<String>();
        restoreAIDataStruct(keyList);
        continueSetRestore(Keys.KEY_CAMERA_FACE_DATECT, keyList,
                "filter restore ai");
        originalAIDetectDataStorageStruct = null;
    }

    @Override
    public void clearRestoreData() {
        super.clearRestoreData();
        originalAIDetectDataStorageStruct = null;
    }
}
