package com.dream.camera.settings;

import android.content.Context;
import android.media.CamcorderProfile;
import android.util.Log;

import com.android.camera.settings.Keys;
import com.android.camera.settings.PictureSizeLoader;
import com.android.camera.settings.PictureSizeLoader.PictureSizes;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.settings.SettingsUtil.SelectedVideoQualities;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.dream.camera.settings.DataConfig.SettingStoragePosition;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.dream.camera.settings.DataModuleBasic.DataSPAndPath;
import com.dream.camera.settings.DataConfig.CategoryType;

public abstract class DataModuleInterfacePV extends DataModuleBasic {

    public static final String TAG = "DataModuleInterfacePV";

    // current childmodule, ex: auto/manual..
    protected int mChildModule;
    // current camera id
    protected int mCameraID;
    // current camera type
    protected boolean mIsFront;

    // storage path start
    // photo/video module setting
    protected DataSPAndPath mCategorySPB;
    // photo/video module - front/back path
    protected DataSPAndPath mCategoryFBSPB;
    // photo/video module - front/back - ex:auto/manual path
    protected DataSPAndPath mCategoryFBModuleSPB;
    // photo/video module - front/back - ex:auto/manual - camera id path
    protected DataSPAndPath mCategoryFBModuleCIDSPB;

    // SPRD: Add for ai detect/antibanding
    // Selected resolutions for the different cameras and sizes.
    public static PictureSizes mPictureSizes;
    public static String[] mCamcorderProfileNames;
    private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");

    protected boolean isSupportedAntibandAutoInited = false;
    protected boolean mSupportedAntibandAutoEnable = false;

    public DataModuleInterfacePV(Context context) {
        super(context);
        if (mCamcorderProfileNames == null) {
            mCamcorderProfileNames = mContext.getResources().getStringArray(
                    R.array.camcorder_profile_names);
        }

        if (loader == null) {
            loader = new PictureSizeLoader(mContext);
        }

    }

    /**
     * restore all variable associated with child module and camera id
     * 
     * @param dataSetting
     */
    public void changeCurrentModule(DataStructSetting dataSetting) {
        clear();
        mChildModule = dataSetting.mMode;
        mCameraID = dataSetting.mCameraID;
        mIsFront = dataSetting.mIsFront;

    }
    protected void changSPB(DataStructSetting dataSetting){//boolean isFront, String childModule, int cameraID) {
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            mCategorySPB = new DataSPAndPath(
                    DataConfig.SettingStoragePosition.POSITION_CATEGORY,
                    DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_PHOTO_SETTING);
            if (dataSetting.mIsFront) {
                mCategoryFBSPB = new DataSPAndPath(
                        DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF,
                        DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_PHOTO_FRONT_SETTING);
            } else {
                mCategoryFBSPB = new DataSPAndPath(
                        DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF,
                        DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_PHOTO_BACK_SETTING);
            }
	    } else {
            mCategorySPB = new DataSPAndPath(
                    DataConfig.SettingStoragePosition.POSITION_CATEGORY,
                    DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_VIDEO_SETTING);
            if (dataSetting.mIsFront) {
                mCategoryFBSPB = new DataSPAndPath(
                        DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF,
                        DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_VIDEO_FRONT_SETTING);
            } else {
                mCategoryFBSPB = new DataSPAndPath(
                        DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF,
                        DataConfig.DataStoragePath.PRE_CAMERA_CATEGORY_VIDEO_BACK_SETTING);
            }
        }
        mCategoryFBModuleSPB = new DataSPAndPath(
                DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF_MODULE,
                mCategoryFBSPB.mPath + dataSetting.mMode);
        mCategoryFBModuleCIDSPB = new DataSPAndPath(
                DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF_MODULE_ID,
                mCategoryFBModuleSPB.mPath + dataSetting.mCameraID);
    }
    private DataSPAndPath getStorageHandler(int position) {
        switch (position) {
        case SettingStoragePosition.POSITION_CATEGORY:
            return mCategorySPB;
        case SettingStoragePosition.POSITION_CATEGORY_BF:
            return mCategoryFBSPB;
        case SettingStoragePosition.POSITION_CATEGORY_BF_MODULE:
            return mCategoryFBModuleSPB;
        case SettingStoragePosition.POSITION_CATEGORY_BF_MODULE_ID:
            return mCategoryFBModuleCIDSPB;
        default:
            return null;
        }
    }

    @Override
    public boolean isSet(int position, String key) {
        DataSPAndPath spb = getStorageHandler(position);
        return ((spb != null) ? spb.isSet(key) : false); // Bug 1159255 (NULL_RETURNS)
    }

    @Override
    public void set(int position, String key, String value) {
        DataSPAndPath spb = getStorageHandler(position);
        if (spb != null) // Bug 1159255 (NULL_RETURNS)
            spb.set(key, value);
    }

    @Override
    public String getString(int position, String key, String defaultValue) {
        DataSPAndPath spb = getStorageHandler(position);
        return spb == null ? defaultValue : spb.getString(key, defaultValue);
    }

    @Override
    public void clear() {

        if (mCategorySPB != null) {
            mCategorySPB.clear();
        }
        if (mCategoryFBModuleCIDSPB != null) {
            mCategoryFBModuleCIDSPB.clear();
        }
        if (mCategoryFBModuleSPB != null) {
            mCategoryFBModuleSPB.clear();
        }
        if (mCategoryFBSPB != null) {
            mCategoryFBSPB.clear();
        }
    }

    @Override
    public void destroy() {
        clear();
    }

    /**
     * @param size
     *            The photo resolution.
     * @return A human readable and translated string for labeling the picture
     *         size in megapixels.
     */
    protected String getSizeSummaryString(Size size) {
        Size approximateSize = ResolutionUtil.getApproximateSize(size);
        int scale = 1;

        if (mCameraID == CameraUtil.BACK_MACRO_CAMERA_ID && CameraUtil.isSRFusionEnable() && !CameraUtil.isHighResolutionScaleTest())
            scale = CameraUtil.getHighResolutionScale();

        String megaPixels = sMegaPixelFormat.format((size.width()* scale * size
                .height()*scale) / 1e6);

        try {
            if("15.9".equals(megaPixels)) {
                megaPixels = "16.0";                
            } else if("11.9".equals(megaPixels)) {
                megaPixels = "12.0";                
            }                
        } catch (Exception e) {
            //TODO: handle exception
        }        

        int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
        int denominator = ResolutionUtil
                .aspectRatioDenominator(approximateSize);
		float mega = Float.parseFloat(megaPixels);
        String result = mContext.getResources().getString(
                R.string.setting_summary_aspect_ratio_and_megapixels,
                numerator, denominator, (int)(mega * 100));
        return result;
    }

    protected void setEVSVideoQualities(String key,
            SelectedVideoQualities selectedQualities) {
        if (selectedQualities == null) {
            return;
        }

        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(key);

        if (data == null) {
            return;
        }
        //Bug 474696 slow motion with 720p
        int VALUE_720p = 5;
        String entryValue_720p = null;

        /* Bug 474696 set the back camera video is default 720p when opened slow motion @{ */
        if (key.equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
            if (selectedQualities.large == VALUE_720p) {
                entryValue_720p = "large";
            } else if (selectedQualities.medium == VALUE_720p) {
                entryValue_720p = "medium";
            } else if (selectedQualities.small == VALUE_720p) {
                entryValue_720p = "small";
            }
        }
        if (null != (DataStorageStruct) mSupportDataMap
                .get(Keys.KEY_VIDEO_SLOW_MOTION)) {
            data.mEntries = new String[] { (mCamcorderProfileNames[VALUE_720p]) };
            data.mEntryValues = new String[] { entryValue_720p };
            data.mDefaultValue = data.mEntryValues[0].toString();
        } else {
            // Avoid adding double entries at the bottom of the list which
            // indicates that not at least 3 qualities are supported.
            ArrayList<String> entries = new ArrayList<String>();
            entries.add(mCamcorderProfileNames[selectedQualities.large]);
            if (selectedQualities.medium != selectedQualities.large) {
                entries.add(mCamcorderProfileNames[selectedQualities.medium]);
            }
            if (selectedQualities.small != selectedQualities.medium) {
                entries.add(mCamcorderProfileNames[selectedQualities.small]);
            }
            data.mEntries = entries.toArray(new String[0]);

            data.mRestorageValue = data.mDefaultValue = mContext
                    .getString(R.string.pref_video_quality_large);

            if (key.equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                if (CamcorderProfile.hasProfile(0,
                        CamcorderProfile.QUALITY_2160P)) {
                    data.mRestorageValue = data.mDefaultValue = mContext
                            .getString(R.string.pref_video_quality_medium);
                }
            }
        }
        Log.e(TAG, "setEVSVideoQualities() " + data.toString());
    }

    protected void setEVSPicturesize(String key, List<Size> selectedSizes) {
        if (selectedSizes == null || selectedSizes.size() == 0) {
            Log.e(TAG, "setEVSPicturesize: selectedSizes object is null or size is 0");
            return;
        }

        DataStorageStruct data = (DataStorageStruct) mSupportDataMap.get(key);

        if (data == null) {
            return;
        }
        String[] entries = new String[selectedSizes.size()];
        String[] entryValues = new String[selectedSizes.size()];

        for (int i = 0; i < selectedSizes.size(); i++) {
            Size size = selectedSizes.get(i);
            entries[i] = getSizeSummaryString(size);
            entryValues[i] = SettingsUtil.sizeToSettingString(size);
        }
        data.mEntries = entries;
        data.mEntryValues = entryValues;
        /*start add for w+t mode default selected second picture size < 8m*/
        data.mDefaultValue = data.mEntryValues[0].toString();

        if (key.equals(Keys.KEY_PICTURE_SIZE_BACK) && CameraUtil.isWPlusTEnable()){
            for (Size size:selectedSizes){
                if (CameraUtil.isResolutionWPlusTSupport(size)){
                    data.mDefaultValue = SettingsUtil.sizeToSettingString(size).toString();
                    break;
                }
            }
        }

        if (key.equals(Keys.KEY_PICTURE_SIZE_BACK) && CameraUtil.isDefaultQuarterSizeEnable()){
            long range = selectedSizes.get(0).area() / 4;
            for (Size size:selectedSizes){
                if (size.area() <= range){
                    data.mDefaultValue = SettingsUtil.sizeToSettingString(size).toString();
                    break;
                }
            }
        }

        /*end add*/
        Log.i(TAG, "setEVSPicturesize() " + data.toString());
    }

    private DataStructSetting getSettingData() {
        return new DataStructSetting(mCategory, mIsFront, mChildModule,
                mCameraID);
    }

    /**
     * This method gets the selected picture sizes for S,M,L and populates
     * {@link #mPictureSizes} accordingly.
     */
//    protected void loadSizes() {
//        if (mInfos == null) {
//            Log.w(TAG, "null deviceInfo, cannot display resolution sizes");
//            return;
//        }
//
//        mPictureSizes = loader.computePictureSizes();
//    }

    public PictureSizes getPictureSizes() {
        return mPictureSizes;
    }

    public static PictureSizeLoader loader;

    @Override
    public void initializeStaticParams(CameraProxy proxy) {
        if (mPictureSizes == null) {
            mPictureSizes = new PictureSizes();
        }
    }
}
