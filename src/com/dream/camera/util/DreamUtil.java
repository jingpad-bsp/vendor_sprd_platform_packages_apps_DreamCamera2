package com.dream.camera.util;

import android.content.Context;
import android.content.res.TypedArray;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;

public class DreamUtil {
    public static final int BACK_CAMERA_PHOTO_MODE = 0;
    public static final int FRONT_CAMERA_PHOTO_MODE = 1;
    public static final int BACK_CAMERA_VIDEO_MODE = 2;
    public static final int FRONT_CAMERA_VIDEO_MODE = 3;
    public static final int PHOTO_MODE = 0;
    public static final int VIDEO_MODE = 1;
    public static final int BACK_CAMERA = 0;
    public static final int FRONT_CAMERA = 1;
//    public static final int BACK_CAMERA_SUB3 = 3;
    private static final Log.Tag TAG = new Log.Tag("DreamUtil ");

    public static int getSwitchBtnResId(int mode) {
        switch (mode % 2) {
            case PHOTO_MODE:
                return R.drawable.ic_switch_to_camera_sprd;
            case VIDEO_MODE:
                return R.drawable.ic_switch_to_video_sprd;
            default:
                return R.drawable.ic_switch_to_video_sprd;
        }
    }

    /*
     * settings cameraId to boolean back/front.
     */
    public static Boolean isFrontCamera(Context context, int cameraId) {
        return DreamUtil.intToBoolean(DreamUtil.getRightCamera(cameraId));
    }

    /*
     * mode int to settings string.
     */
    public static String intToString(int mode) {
        switch (mode) {
            case PHOTO_MODE:
                return DataConfig.CategoryType.CATEGORY_PHOTO;
            case VIDEO_MODE:
                return DataConfig.CategoryType.CATEGORY_VIDEO;
            default:
                return DataConfig.CategoryType.CATEGORY_PHOTO;
        }

    }

    /*
     * back/front int to settings boolean.
     */
    public static boolean intToBoolean(int backOrFront) {
        switch (backOrFront) {
            case BACK_CAMERA:
                return false;
            case FRONT_CAMERA:
                return true;
            default:
                return false;
        }
    }

    public static int getRightCamera(int cameraId) {
        int result = BACK_CAMERA;
        switch (cameraId) {
            case BACK_CAMERA:
            case CameraUtil.BACK_IR_CAMERA_ID:
            case CameraUtil.BACK_MACRO_CAMERA_ID:
            case CameraUtil.BACK_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID:
            case CameraUtil.BLUR_REFOCUS_PHOTO_ID:
            case CameraUtil.BACK_W_PLUS_T_PHOTO_ID:
            case CameraUtil.BACK_TRI_CAMERA_ID:
            case CameraUtil.BACK_WT_FUSION_ID:
            case CameraUtil.BACK_HIGH_RESOLUTOIN_CAMERA_ID:
            case CameraUtil.BACK_PORTRAIT_ID:
            //case BACK_CAMERA_SUB3:
                result = BACK_CAMERA;
                break;
            case FRONT_CAMERA:
            case CameraUtil.TD_CAMERA_ID:
            case CameraUtil.TD_PHOTO_ID:
            case CameraUtil.SENSOR_SELF_SHOT_CAMERA_ID:
            case CameraUtil.FRONT_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID:
            case CameraUtil.FRONT_BLUR_REFOCUS_PHOTO_ID:
            case CameraUtil.FRONT_HIGH_RESOLUTION_CAMERA_ID:
            case CameraUtil.FRONT_PORTRAIT_ID:
                result = FRONT_CAMERA;
                break;
        }
        return result;
    }

    public static int getRightWPlusTCamera(int cameraId) {
        int result = cameraId;
        switch (cameraId) {
            case 11:
                result = BACK_CAMERA;
                break;
        }
        return result;
    }


    public static int getRightCameraId(int camera) {
        return (camera == BACK_CAMERA || camera == FRONT_CAMERA) ? camera : BACK_CAMERA;
    }

    public String getGlobleKey(int module, int cameraId) {
        String key = Keys.KEY_BACK_PHOTO_MODE;
        switch (module) {
            case PHOTO_MODE:
                switch (cameraId) {
                    case BACK_CAMERA:
                    //case BACK_CAMERA_SUB3:
                    case CameraUtil.BACK_IR_CAMERA_ID:
                    case CameraUtil.BACK_MACRO_CAMERA_ID:
                        key = Keys.KEY_BACK_PHOTO_MODE;
                        break;
                    case FRONT_CAMERA:
                        key = Keys.KEY_FRONT_PHOTO_MODE;
                        break;
                }
                break;
            case VIDEO_MODE:
                switch (cameraId) {
                    case BACK_CAMERA:
                    //case BACK_CAMERA_SUB3:
                    case CameraUtil.BACK_IR_CAMERA_ID:
                    case CameraUtil.BACK_MACRO_CAMERA_ID:
                        key = Keys.KEY_BACK_VIDEO_MODE;
                        break;
                    case FRONT_CAMERA:
                        key = Keys.KEY_FRONT_VIDEO_MODE;
                        break;
                }
                break;
            case 999:
                switch (cameraId) {
                    case BACK_CAMERA:
                    //case BACK_CAMERA_SUB3:
                    case CameraUtil.BACK_IR_CAMERA_ID:
                    case CameraUtil.BACK_MACRO_CAMERA_ID:
                        key = Keys.KEY_BACK_PHOTO_MODE;
                        break;
                    case FRONT_CAMERA:
                        key = Keys.KEY_FRONT_PHOTO_MODE;
                        break;
                }
                break;
        }
        return key;
    }

    public int getDefaultValue(int module, int cameraId) {
        return (module == VIDEO_MODE) ? SettingsScopeNamespaces.AUTO_VIDEO : SettingsScopeNamespaces.AUTO_PHOTO;
    }

    public int getRightMode(DataModuleBasic dataModule, int module, int cameraId) {
        Log.d(TAG,
                "getRightMode cameraId=" + cameraId + ",module=" + module);
        return dataModule.getInt(
                getGlobleKey(module, cameraId), getDefaultValue(module, cameraId));
    }

    /**
     * Every time mode selected, we should save to the corresponding camera mode
     * KEY_BACK_PHOTO_MODE/KEY_FRONT_PHOTO_MODE/KEY_BACK_VIDEO_MODE/KEY_FRONT_VIDEO_MODE
     *
     * @param context
     * @param DataModuleBasic
     * @param cameraId        : 0/1 - back/front
     * @param saveMode        : current mode.
     */
    public void savaToCameraMode(Context context, DataModuleBasic dataModule, int cameraId,
                                 int saveMode) {
        Log.d(TAG, "savaRightMode cameraId=" + cameraId + ",saveMode=" + saveMode);
        // Now in photo or video
//        int[] modeSupportList = context.getResources().getIntArray(
//                R.array.dream_module_mode_support_list);

        // save value to responding keys.
        if (saveMode == SettingsScopeNamespaces.FRONT_BLUR) {
            dataModule.set(Keys.KEY_FRONT_PHOTO_MODE, saveMode);
            return;
        } else if (saveMode == SettingsScopeNamespaces.REFOCUS) {
            dataModule.set(Keys.KEY_BACK_PHOTO_MODE, saveMode);
            return;
        }
        //dataModule.set(getGlobleKey(modeSupportList[saveMode], cameraId), saveMode);
        dataModule.set(getGlobleKey(CameraUtil.mModuleInfoResovle.getModuleSupportMode(saveMode), cameraId), saveMode);
    }
}
