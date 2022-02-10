package com.dream.camera.settings;

import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera2.R;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic.DataStorageStruct;

import android.R.integer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import com.android.camera.util.CameraUtil;
import android.view.View;

public class DreamSettingUtil {

    private static final Log.Tag TAG = new Log.Tag("DreamSetting");

    /**
     * open preferences associated with file path
     * 
     * @param context
     * @param settingPath
     * @return
     */
    public static SharedPreferences openPreferences(Context context,
            String settingPath) {
        synchronized (DreamSettingUtil.class) {
            SharedPreferences preferences;
            preferences = context.getSharedPreferences(settingPath,
                    Context.MODE_PRIVATE);
            return preferences;
        }
    }

    private static int getSupportDataFrontVideo(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_AUTO:
                resourceID = R.array.video_front_mode_auto_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_PIV_VIV:
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_TD:
                resourceID = R.array.video_3d_quality_front_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_3DNR:
                resourceID = R.array.video_front_mode_3dnr_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_3DNR_PRO:
                resourceID = R.array.video_front_mode_3dnr_pro_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_INTENT_CAPTURE:
                resourceID = R.array.video_front_mode_auto_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.video_front_mode_portraitbackgroundreplacement_setting;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getSupportDataBackVideo(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_AUTO:
                resourceID = R.array.video_back_mode_auto_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_TIME_LAPSE:
                resourceID = R.array.video_back_mode_time_lapse_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_SLOW_MOTION:
                resourceID = R.array.video_back_mode_slowmotion_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_PIP_VIV:
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_3DNR:
                resourceID = R.array.video_back_mode_3dnr_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_3DNR_PRO:
                resourceID = R.array.video_back_mode_3dnr_pro_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_INTENT_CAPTURE:
                resourceID = R.array.video_back_mode_auto_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_MACRO:
                resourceID = R.array.video_back_mode_macro_setting;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.video_back_mode_portraitbackgroundreplacement_setting;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getSupportDataFrontPhoto(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_INTERVAL_PICTURE:
                resourceID = R.array.photo_front_interval_pic_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_AUTO:
                resourceID = R.array.photo_front_auto_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_UCAM_FILTER:
                if (CameraUtil.isUseSprdFilter()) {
                    resourceID = R.array.photo_front_mode_sprd_filter_setting;
                }
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_INTENT_CAPTURE:
                resourceID = R.array.photo_front_mode_intent_capture_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_SOUND_PICTURE:
                resourceID = R.array.photo_front_mode_sound_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_TD:
                resourceID = R.array.photo_3d_quality_front_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_BLUR:
                resourceID = R.array.photo_front_blur_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_3DNR:
                resourceID = R.array.photo_front_3dnr_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_3DNR_PRO:
                resourceID = R.array.photo_front_3dnr_pro_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_PORTRAIT:
                resourceID = R.array.photo_front_portrait_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_HIGH_RESOLUTION:
                resourceID = R.array.photo_front_high_resolution_photo_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.photo_front_mode_protrait_background_replacement_photo_setting;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getSupportDataBackPhoto(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_AUTO:
                resourceID = R.array.photo_back_auto_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_MANUAL:
                resourceID = R.array.photo_back_manual_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_CONTINUE_PICTURE:
                resourceID = R.array.photo_back_continue_pic_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PANORAMA:
                resourceID = R.array.photo_back_panorama_setting;
                break;

            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_SCENE:
                resourceID = R.array.photo_back_scene_setting;
                break;

            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_UCAM_FILTER:
                resourceID = getResourceIdBackUcamFilter(resourceID);
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_INTENT_CAPTURE:
                resourceID = R.array.photo_back_mode_intent_capture_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_QRCODE:
                resourceID = R.array.photo_back_mode_qrcode_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_SOUND_PICTURE:
                resourceID = R.array.photo_back_mode_sound_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PIP_VIV:
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_REFOCUS:
                resourceID = getResourceIdBackRefocus(resourceID);
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_3DNR:
                resourceID = R.array.photo_back_3dnr_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_3DNR_PRO:
                resourceID = R.array.photo_back_3dnr_pro_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_ULTRA_WIDE_ANGLE:
                resourceID = R.array.photo_back_ultra_wide_angle_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PORTRAIT:
                resourceID = R.array.photo_back_portrait_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_HIGH_RESOLUTION:
                resourceID = R.array.photo_back_high_resolution_photo_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_IR:
                resourceID = R.array.photo_back_ir_photo_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_FOCUS_LENGTH_FUSION:
                resourceID = R.array.photo_back_focus_length_fusion_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_MACRO:
                resourceID = R.array.photo_back_macro_photo_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.photo_back_mode_protrait_background_replacement_photo_setting;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_FDR:
                resourceID = R.array.photo_back_fdr_setting;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getResourceIdBackUcamFilter(int id) {
        int ret = id;
        if (CameraUtil.isUseSprdFilter()) ret = R.array.photo_back_mode_sprd_filter_setting;
        return ret;
    }

    private static int getResourceIdBackRefocus(int id) {
        int ret = id;
        if (CameraUtil.getRefocusModeSupport().equals("stillImageRefocus")) {
            ret = R.array.photo_back_mode_refocus_setting;
        } else if (CameraUtil.getRefocusModeSupport().equals("blurRefocus")) {
            ret = R.array.photo_back_blur_setting;
        }
        return ret;
    }

    public static int getSupportDataResourceID(DataStructSetting dataSetting) {
        int resourceID = -1;
        // camera configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_CAMERA)) {
            resourceID = R.array.camera_public_setting;
            return resourceID;
        }

        // photo configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            if (dataSetting.mIsFront) {
                resourceID = getSupportDataFrontPhoto(dataSetting.mMode , resourceID);
            } else {
                resourceID = getSupportDataBackPhoto(dataSetting.mMode , resourceID);
            }
            return resourceID;
        }

        // video configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
            if (dataSetting.mIsFront) {
                resourceID = getSupportDataFrontVideo(dataSetting.mMode , resourceID);
            } else {
                resourceID = getSupportDataBackVideo(dataSetting.mMode , resourceID);
            }
            return resourceID;
        }

        return resourceID;
    }

    private static int getPrefUIFrontPhoto(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_AUTO:
                resourceID = R.array.photo_front_mode_auto_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_INTERVAL_PICTURE:
                resourceID = R.array.photo_front_mode_interval_picture_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_SOUND_PICTURE:
                resourceID = R.array.photo_front_mode_sound_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_PIP_VIV:
                resourceID = R.array.photo_front_mode_pip_viv_picture_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_UCAM_FILTER:
                resourceID = R.array.photo_front_mode_ucam_filter_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_INTENT_CAPTURE:
                resourceID = R.array.photo_front_mode_intent_capture_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_TD:
                resourceID = R.array.photo_3d_quality_front_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_BLUR:
                resourceID = R.array.photo_front_mode_blur_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_3DNR:
                resourceID = R.array.photo_front_mode_3dnr_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_3DNR_PRO:
                resourceID = R.array.photo_front_mode_3dnr_pro_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_PORTRAIT:
                resourceID = R.array.photo_front_mode_portrait_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_HIGH_RESOLUTION:
                resourceID = R.array.photo_front_mode_high_resolution_photo_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_FRONT_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.photo_front_mode_protrait_background_replacement_photo_setting_display;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getPrefUIBackRefocus(int id) {
        int ret = id;
        if (CameraUtil.getRefocusModeSupport().equals("stillImageRefocus")) {
            ret = R.array.photo_back_mode_refocus_setting_display;
        } else if (CameraUtil.getRefocusModeSupport().equals("blurRefocus")) {
            ret = R.array.photo_back_mode_blur_setting_display;
        }
        return ret;
    }

    private static int getPrefUIBackPhoto(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_AUTO:
                resourceID = R.array.photo_back_mode_auto_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_MANUAL:
                resourceID = R.array.photo_back_mode_manual_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PANORAMA:
                resourceID = R.array.photo_back_mode_panorama_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_REFOCUS:
                resourceID = getPrefUIBackRefocus(resourceID);
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_CONTINUE_PICTURE:
                resourceID = R.array.photo_back_mode_continue_pic_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_SCENE:
                resourceID = R.array.photo_back_mode_scene_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PIP_VIV:
                resourceID = R.array.photo_back_mode_pip_viv_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_SOUND_PICTURE:
                resourceID = R.array.photo_back_mode_sound_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_UCAM_SECNE:
                resourceID = R.array.photo_back_mode_ucam_scene_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_UCAM_FILTER:
                resourceID = R.array.photo_back_mode_ucam_filter_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_INTENT_CAPTURE:
                resourceID = R.array.photo_back_mode_intent_capture_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_QRCODE:
                resourceID = R.array.photo_back_mode_qrcode_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_3DNR:
                resourceID = R.array.photo_back_mode_3dnr_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_3DNR_PRO:
                resourceID = R.array.photo_back_mode_3dnr_pro_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_ULTRA_WIDE_ANGLE:
                resourceID = R.array.photo_back_mode_ultra_wide_angle_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PORTRAIT:
                resourceID = R.array.photo_back_mode_portrait_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_HIGH_RESOLUTION:
                resourceID = R.array.photo_back_mode_high_resolution_photo_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_IR:
                resourceID = R.array.photo_back_mode_ir_photo_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_FOCUS_LENGTH_FUSION:
                resourceID = R.array.photo_back_mode_focus_length_fusion_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_MACRO:
                resourceID = R.array.photo_back_mode_macro_photo_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.photo_back_mode_protrait_background_replacement_photo_setting_display;
                break;
            case DataConfig.PhotoModeType.PHOTO_MODE_BACK_FDR:
                resourceID = R.array.photo_back_mode_fdr_setting_display;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getPrefUIFrontVideo(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_AUTO:
                resourceID = R.array.video_front_mode_auto_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_PIV_VIV:
                resourceID = R.array.video_front_mode_pip_viv_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_TD:
                resourceID = R.array.video_3d_quality_front_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_3DNR:
                resourceID = R.array.video_front_mode_3dnr_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_3DNR_PRO:
                resourceID = R.array.video_front_mode_3dnr_pro_setting_display;
            case DataConfig.VideoModeType.VIDEO_MODE_FRONT_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.video_back_mode_portraitbackgroundreplacement_setting_display;
                break;
            default:
                break;
        }
        return resourceID;
    }

    private static int getPrefUIBackVideo(int mode , int id) {
        int resourceID = id;
        switch (mode) {
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_AUTO:
                resourceID = R.array.video_back_mode_auto_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_TIME_LAPSE:
                resourceID = R.array.video_back_mode_time_lapse_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_SLOW_MOTION:
                resourceID = R.array.video_back_mode_slow_motion_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_PIP_VIV:
                resourceID = R.array.video_back_mode_piv_viv_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_3DNR:
                resourceID = R.array.video_back_mode_3dnr_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_3DNR_PRO:
                resourceID = R.array.video_back_mode_3dnr_pro_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_MACRO:
                resourceID = R.array.video_back_mode_macro_setting_display;
                break;
            case DataConfig.VideoModeType.VIDEO_MODE_BACK_PORTRAIT_BACKGROUND_REPLACEMENT:
                resourceID = R.array.video_back_mode_portraitbackgroundreplacement_setting_display;
                break;
            default:
                break;
        }
        return resourceID;
    }

    public static int getPreferenceUIConfigureID(DataStructSetting dataSetting) {
        int resourceID = -1;
        // camera configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_CAMERA)) {
            resourceID = R.array.camera_public_setting_display;
            return resourceID;
        }
        // photo configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            if (dataSetting.mIsFront) {
                resourceID = getPrefUIFrontPhoto(dataSetting.mMode , resourceID);
            } else {
                resourceID = getPrefUIBackPhoto(dataSetting.mMode , resourceID);
            }
            return resourceID;
        }

        // video configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
            if (dataSetting.mIsFront) {
                resourceID = getPrefUIFrontVideo(dataSetting.mMode , resourceID);
            } else {
                resourceID = getPrefUIBackVideo(dataSetting.mMode , resourceID);
            }
            return resourceID;
        }

        return resourceID;
    }

    public static int getMutexDataResourceID(DataStructSetting dataSetting) {

        int resourceID = -1;
        // camera configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_CAMERA)) {
//            resourceID = R.array.camera_other_data_setting;
        }
        // photo configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            if (dataSetting.mIsFront) {
                resourceID = R.array.photo_front_other_mutex_setting;
            }else {
                resourceID = R.array.photo_back_other_mutex_setting;
            }
        }

        // video configuration resource
        if (dataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
            if (dataSetting.mIsFront) {
                resourceID = R.array.video_front_other_mutex_setting;
            }else {
                resourceID = R.array.video_back_other_mutex_setting;
            }
        }
        return resourceID;

    }

    public static void showDialog(Context context, String key, int titleID,
            int drawableIDs, final DialogInterface.OnClickListener listener) {
        // title
        String title = context.getResources().getString(titleID);
        // drable list
        TypedArray types = context.getResources().obtainTypedArray(drawableIDs);
        int[] mDrawableIDs = new int[types.length()];
        if (types != null) {
            for (int i = 0; i < types.length(); i++) {
                mDrawableIDs[i] = types.getResourceId(i, -1);

            }
            types.recycle();
        }

        // key
        DataStorageStruct ds = (DataStorageStruct) DataModuleManager
                .getInstance(context).getCurrentDataModule().mSupportDataMap.get(key);

        if (ds == null) {
            return;
        }

        int selectedIndex = DataModuleManager.getInstance(context)
                .getCurrentDataModule().getIndexOfCurrentValue(key);

        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                R.style.ThemeDeviceDefaultDialogAlert);
        DreamUIAdapterMeter adapter = new DreamUIAdapterMeter();
        adapter.setData(ds.mEntries, mDrawableIDs, selectedIndex);

        builder.setTitle(title)
                .setSingleChoiceItems(adapter, selectedIndex,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                if (listener != null) {
                                    listener.onClick(dialog, which);
                                }
                            }
                        })
                .setPositiveButton(null, null)
                .setCancelable(true)
                .setNegativeButton(
                        context.getResources().getString(
                                R.string.cancel_button_description),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).setPositiveButton(null, null);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();
    }

    /**
     * Package private conversion method to turn ints into preferred String
     * storage format.
     * 
     * @param value
     *            int to be stored in Settings
     * @return String which represents the int
     */
    static String convert(int value) {
        return Integer.toString(value);
    }

    /**
     * Package private conversion method to turn booleans into preferred String
     * storage format.
     * 
     * @param value
     *            boolean to be stored in Settings
     * @return String which represents the boolean
     */
    static String convert(boolean value) {
        return value ? "1" : "0";
    }

    /**
     * Package private conversion method to turn String storage format into
     * ints.
     * 
     * @param value
     *            String to be converted to int
     * @return int value of stored String
     */
    static public int convertToInt(String value) {
        return Integer.parseInt(value);
    }

    /**
     * Package private conversion method to turn String storage format into
     * booleans.
     * 
     * @param value
     *            String to be converted to boolean
     * @return boolean value of stored String
     */
    static boolean convertToBoolean(String value) {
        return Integer.parseInt(value) != 0;
    }

}
