/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.settings;

import android.content.Context;
import android.content.res.Resources;

import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.dream.camera.settings.DataModuleBasic;

/**
 * Keys is a class for storing SharedPreferences keys and configuring their
 * defaults.
 * <p>
 * For each key that has a default value and set of possible values, it stores
 * those defaults so they can be used by the SettingsManager on lookup. This
 * step is optional, and it can be done anytime before a setting is accessed by
 * the SettingsManager API.
 */
public class Keys {

    /**
     * add key for dream camera
     */
    public static final String KEY_CAMERA_VOLUME_FUNCTION = "pref_camera_volume_key_function";
    public static final String KEY_CAMERA_COMPOSITION_LINE = "pref_camera_composition_line_key";
    public static final String KEY_CAMERA_PHOTOGRAPH_STABILIZATION = "pref_camera_photograph_stabilization_key";
    public static final String KEY_CAMERA_HDR_NORMAL_PIC = "pref_camera_hdr_normal_pic_key";
    public static final String KEY_CAMERA_GRADIENTER_KEY = "pref_camera_gradienter_key";
    public static final String KEY_CAMERA_TOUCHING_PHOTOGRAPH = "pref_camera_touching_photograph_key";
    public static final String KEY_CAMERA_VIDEO_STABILIZATION = "pref_camera_video_stabilization_key";
    public static final String KEY_CAMERA_MICROPHONE_SWITCH = "pref_camera_microphone_switch_key";
    public static final String KEY_DREAM_CAMERA_RESET = "pref_dream_camera_reset_key";
    public static final String KEY_DREAM_COMPOSITION_LINE = "pref_video_composition_line_key";
    public static final String KEY_DREAM_FLASH_GIF_PHOTO_MODE = "pref_camera_gif_photo_flashmode_key";
    public static final String KEY_DREAM_ZOOM_ENABLE_PHOTO_MODULE = "pref_camera_zoom_enable_key";
    // SPRD: Fix bug 585183 Adds new features 3D recording
    public static final String KEY_VIDEO_QUALITY_FRONT_3D = "pref_3d_video_quality_front_key";
 // SPRD: Fix 597206 Adds new features 3DPhoto
    public static final String KEY_PICTURE_SIZE_FRONT_3D = "pref_3d_photo_quality_front_key";
    /* SPRD: Add for bug 594960, beauty video recoding @{ */
    public static final String KEY_VIDEO_BEAUTY_ENTERED = "pref_video_beauty_entered_key";
    public static final String KEY_MAKEUP_VIDEO_LEVEL = "pref_makeup_video_level_key";
    /* @} */
    public static final String KEY_VIDEO_QUALITY_BACK_MACRO = "pref_video_macro_quality_back_key";
    public static final String KEY_BLUR_REFOCUS_LEVEL = "pref_blur_refocus_level_key";
    public static final int shutter = 1;
    public static final int zoom = 2;
    public static final int volume = 3;
    // SPRD: Fix bug 584710, When the screen is unlocked, the flash of the state should be turned on
    public static final String KEY_QRSCANCAMERA_FLASH_MODE = "pref_camera_qrscan_flashmode_key";
    public static final String PREF_KEY_QRCODE = "pref_key_qrcode";
    public static final String KEY_ADD_LEVEL= "pref_add_level_key";
    // motionphoto
    public static final String PREF_KEY_MONTIONPHOTO = "pref_key_montionphoto";
    /*
     * SCOPE_GLOBAL VALUE
     */
    public static final String KEY_BACK_PHOTO_MODE = "pref_back_photo_mode";
    public static final String KEY_FRONT_PHOTO_MODE = "pref_front_photo_mode";
    public static final String KEY_BACK_VIDEO_MODE = "pref_back_video_mode";
    public static final String KEY_FRONT_VIDEO_MODE = "pref_front_video_mode";
    public static final String KEY_CAMERA_AND_MODE = "pref_camera_and_mode";
    public static final String KEY_VIDEO_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_RECORD_LOCATION = "pref_camera_recordlocation_key";
    public static final String KEY_VIDEO_QUALITY_BACK = "pref_video_quality_back_key";
    public static final String KEY_VIDEO_QUALITY_BACK_LAST = "pref_video_quality_back_last_key";
    public static final String KEY_VIDEO_QUALITY_FRONT = "pref_video_quality_front_key";
    public static final String KEY_PICTURE_SIZE_BACK = "pref_camera_picturesize_back_key";
    public static final String KEY_PICTURE_SIZE_FRONT = "pref_camera_picturesize_front_key";
    // SPRD:Modify for jpeg quality
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpeg_quality_key";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_EXPOSURE_SHUTTERTIME = "pref_camera_exposure_shuttertime_key";
    public static final String KEY_CAMERA_FOCUS_DISTANCE = "pref_camera_focus_distance_key";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    public static final String KEY_CAMERA_SWITCH = "pref_camera_switch_key";
    public static final String KEY_INTENT_CAMERA_SWITCH = "pref_intent_camera_switch_key";
    public static final String KEY_CAMERA_HDR = "pref_camera_hdr_key";
    public static final String KEY_CAMERA_HDR_PLUS = "pref_camera_hdr_plus_key";
    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";
    public static final String KEY_STARTUP_MODULE_INDEX = "camera.startup_module";
    public static final String KEY_CAMERA_MODULE_LAST_USED = "pref_camera_module_last_used_index";
    public static final String KEY_CAMERA_PANO_ORIENTATION = "pref_camera_pano_orientation";
    public static final String KEY_CAMERA_GRID_LINES = "pref_camera_grid_lines";
    public static final String KEY_RELEASE_DIALOG_LAST_SHOWN_VERSION = "pref_release_dialog_last_shown_version";

    public static final String KEY_UPGRADE_VERSION = "pref_upgrade_version";
    public static final String KEY_REQUEST_RETURN_HDR_PLUS = "pref_request_return_hdr_plus";
    public static final String KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING = "pref_should_show_refocus_viewer_cling";
    public static final String KEY_EXPOSURE_COMPENSATION_ENABLED = "pref_camera_exposure_compensation_key";
    // SPRD: fix bug 473462 add burst capture
    public static final String KEY_CAMERA_CONTINUE_CAPTURE = "pref_camera_burst_key";
    /**
     * Whether the user has chosen an aspect ratio on the first run dialog.
     */
    //删除settingsmanager
    //public static final String KEY_USER_SELECTED_ASPECT_RATIO = "pref_user_selected_aspect_ratio";
    public static final String KEY_COUNTDOWN_DURATION = "pref_camera_countdown_duration_key";
    public static final String KEY_VOICE_COUNTDOWN_DURATION = "pref_camera_voice_countdown_duration_key";
    public static final String KEY_VOICE_CAMERA_DONE = "pref_camera_voice_camera_done";
    public static final String KEY_SELF_TIMER_INTERVAL = "pref_camera_self_timer_interval_key";
    public static final String KEY_REFOCUS = "pref_camera_refocus_key";
    public static final String KEY_HDR_PLUS_FLASH_MODE = "pref_hdr_plus_flash_mode";
    public static final String KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING = "pref_should_show_settings_button_cling";
    public static final String KEY_HAS_SEEN_PERMISSIONS_DIALOGS = "pref_has_seen_permissions_dialogs";
    public static final String KEY_QUICK_CAPTURE = "pref_camera_quick_capture_key";
    /* SPRD: New Feature About Camera FreezePictureDisplay */
    public static final String KEY_FREEZE_FRAME_DISPLAY = "pref_freeze_frame_display_key";
    /* SPRD: New Feature About Antibanding */
    public static final String KEY_CAMER_ANTIBANDING = "pref_camera_antibanding_key";
    /* SPRD: New Feature About mirror */
    public static final String KEY_FRONT_CAMERA_MIRROR = "pref_front_camera_mirror_key";
    public static final String KEY_CAMERA_AI_DATECT_LAST = "pref_camera_ai_detect_last_key";
    // SPRD : New Feature About face detect
    public static final String KEY_CAMERA_FACE_DATECT = "pref_camera_face_detect_key";
    // SPRD: New Feature About video face detect
    public static final String KEY_CAMERA_VIDEO_FACE_DETECT = "pref_camera_video_face_detect_key";
    /* SPRD: New Feature About face detect */
    public static final String CAMERA_AI_DATECT_VAL_OFF = "off";
    public static final String CAMERA_AI_DETECT_FACE_ATTRIBUTES = "face_attributes";
    // SPRD : New Feature About ai scene detect
    public static final String KEY_CAMERA_AI_SCENE_DATECT = "pref_camera_ai_scene_detect_key";
    /* SPRD : New Feature About Color Effect */
    public static final String KEY_CAMERA_COLOR_EFFECT = "pref_camera_color_effect_key";
    /* SPRD : New Feature About White Balance */
    public static final String KEY_WHITE_BALANCE = "pref_camera_whitebalance_key";
    /* SPRD : New Feature torage path */
    public static final String KEY_CAMERA_STORAGE_PATH = "pref_camera_storage_path";
    // SPRD: fix bug 474665
    public static final String KEY_CAMERA_SHUTTER_SOUND = "pref_shutter_sound_key";
    // SPRD: fix bug 474672
    public static final String KEY_CAMERA_BEAUTY_ENTERED = "pref_camera_beauty_entered_key";
    // Bug 1018708 - hide or show BeautyButton on topPanel
    public static final String KEY_CAMERA_AI_BEAUTY_ENTERED = "pref_camera_ai_beauty_entered_key";
    // SPRD: fix bug 487525 save makeup level for makeup module
    public static final String KEY_MAKEUP_MODE_LEVEL = "pref_makeup_mode_level_key";
    //SPRD: arc & sprd beatuy parameters
    public static final String KEY_MAKEUP_SKIN_SMOOTH_LEVEL = "pref_makeup_skinsmooth_level_key";
    public static final String KEY_MAKEUP_REMOVE_BLEMISH_LEVEL = "pref_makeup_removeblemish_level_key";
    public static final String KEY_MAKEUP_SKIN_BRIGHT_LEVEL = "pref_makeup_skinbright_level_key";
    public static final String KEY_MAKEUP_SKIN_COLOR_TYPE = "pref_makeup_skincolor_type_key";
    public static final String KEY_MAKEUP_SKIN_COLOR_LEVEL = "pref_makeup_skincolor_level_key";
    public static final String KEY_MAKEUP_ENLARGE_EYES_LEVEL = "pref_makeup_enlarge_eyes_level_key";
    public static final String KEY_MAKEUP_SLIM_FACE_LEVEL = "pref_makeup_slim_face_level_key";
    public static final String KEY_MAKEUP_LIPS_COLOR_TYPE = "pref_makeup_lipscolor_type_key";
    public static final String KEY_MAKEUP_LIPS_COLOR_LEVEL = "pref_makeup_lipscolor_level_key";

    /* SPRD: for bug 509708 add time lapse */
    public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
    public static final String TIME_LAPSE_DEFAULT_VALUE = "0";
    /* SPRD: New Feature About TimeStamp */
    public static final String KEY_CAMERA_SENSOR_SELF_SHOT = "pref_sensor_self_shot";
    // SPRD:New Feature Gif
    public static final String KEY_GIF_FLASH_MODE = "pref_gif_flashmode_key";
    /*SPRD: New Feature zsl */
    public static final String KEY_CAMERA_ZSL = "pref_camera_zsl_key";
    public static final String KEY_AUTO_TRACKING = "pref_auto_tracking_key";
    /* SPRD: Fix bug 560276 reset scenery @{ */
    public static final String KEY_GIF_MODE_SWITCHER = "pref_gif_mode_switcher_key";
    public static final String KEY_SCENERY_MODE_RESET = "pref_scenery_mode_reset_key";
    /* SPRD: Fix Bug 535139, Feature:video color effect. */
    public static final String KEY_VIDEO_COLOR_EFFECT = "pref_video_color_effect_key";
    /* SPRD: Fix Bug 535139, Feature:video white balance. */
    public static final String KEY_VIDEO_WHITE_BALANCE = "pref_video_whitebalance_key";
    /* SPRD: Fix bug 474851, Feature:Photo voice record. */
    public static final String KEY_CAMERA_RECORD_VOICE = "pref_camera_record_voice_key";
    /* SPRD: fix bug 474843, New featuren for filter. */
    public static final String KEY_CAMERA_FILTER_TYPE = "pref_camera_filter_type_key";
    public static final String KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE = "pref_camera_portraitbackgroundreplacement_type_key";
    public static final String KEY_CAMERA_FILTER_ENTERED = "pref_camera_filter_entered_key";
    /* SPRD: Fix bug 534257 New Feature EIS&OIS @{ */
    public static final String KEY_EOIS_RESOLUTION = "pref_eois_resolution";
    public static final String KEY_EOIS_DC_BACK = "pref_eois_dc_back_key";
    public static final String KEY_EOIS_DC_FRONT = "pref_eois_dc_front_key";
    public static final String KEY_EOIS_DV_BACK = "pref_eois_dv_back_key";
    public static final String KEY_EOIS_DV_FRONT = "pref_eois_dv_front_key";
    /* SPRD:Bug 535058 New feature: volume */
    public static final String KEY_CAMERA_VOLUME = "pref_camera_volume_key";
    /* SPRD: New Feature About High ISO */
    public static final String KEY_HIGH_ISO = "pref_high_iso_key";
    /* SPRD: Fix bug 533869 reset scenery .*/
    public static final String KEY_CAMERA_SCENERY_TYPE = "pref_camera_scenery_type_key";
    /* SPRD: add for new feature: video MICROPHONE @{ */
    public static final String KEY_CAMERA_MICROPHONE = "pref_camera_microphone_switch_key";
    /* SPRD: add for new feature: camera welcome */
    public static final String KEY_CAMERA_WELCOME = "pref_camera_welcome";
    /* SPRD Bug:495676 add antibanding for DV. */
    public static final String KEY_VIDEO_ANTIBANDING = "pref_video_antibanding_key";
    // SPRD Bug:474721 Feature:Contrast.
    public static final String KEY_CAMERA_CONTRAST = "pref_camera_contrast_key";
    // SPRD Bug:474724 Feature:ISO.
    public static final String KEY_CAMERA_ISO = "pref_camera_iso_key";
    // SPRD Bug:474718 Feature:Metering.
    public static final String KEY_CAMER_METERING = "pref_camera_metering_key";
    // SPRD Bug:474722 Feature:Saturation.
    public static final String KEY_CAMERA_SATURATION = "pref_camera_saturation_key";
    /*
     * SPRD Bug:474694 Feature:Reset Settings. @{
     */
    public static final String KEY_CAMER_RESET = "pref_camera_reset_key";
    public static final String KEY_VIDEO_RESET = "pref_video_reset_key";
    // SPRD Bug:494930 Do not show Location Dialog when resetting settings.
    public static final int RECORD_LOCATION_ON = 1;
    public static final int RECORD_LOCATION_OFF = 0;
    /*
     * SPRD Bug:474696 Feature:Slow-Motion. @{
     */
    public static final String KEY_VIDEO_SLOW_MOTION = "pref_video_slow_motion_key";
    /* @} */
    public static final String KEY_VIDEO_SLOW_MOTION_ALL = "pref_video_slow_motion_key_all";
    public static final String SLOWMOTION_DEFAULT_VALUE = "1";

    /* SPRD: Fix bug 533869 add hint when user uses burst in the first*/
    public static final String KEY_CAMERA_BURST_HINT = "pref_camera_burst_hint";
    public static final String KEY_CAMERA_RECORD_VOICE_HINT = "pref_camera_record_voice_hint";
    public static final String KEY_CAMERA_BLUR_REFOCUS_HINT = "pref_camera_blur_refocus_hint";
    /* @} */

    /* SPRD: Fix bug 625678 add hint when sd card supports 4k recording at the most in the first*/
    public static final String KEY_CAMERA_VIDEO_HINT = "pref_camera_video_hint";
    /* @} */

    /* SPRD: Fix bug 533869 add hint when user uses slowmotionVideo in the first*/
    public static final String KEY_CAMERA_SLOWMOTION_HINT = "pref_camera_slowmotion_hint";
    /* @} */

    /* SPRD: Fix bug 533869 add hint when user uses timelapseVideo in the first*/
    public static final String KEY_CAMERA_TIMELAPSE_HINT = "pref_camera_timelapse_hint";
    /* @} */

    public static final String KEY_ADJUST_FLASH_LEVEL = "pref_adjust_flash_level";

    //SPRD: Fix bug 944410
    public static final String KEY_AE_LOCK = "pre_ae_lock_key";

    public static final String KEY_DIRECTOR = "pref_director";

    public static final String KEY_ULTRA_WIDE_ANGLE = "pref_camera_ultra_wide_angle_key";

    public static final String KEY_FILTER_PHOTO = "pref_filter_photo_key";
    public static final String KEY_AI_DETECT_SMILE = "pref_ai_detect_smile_key";
    public static final String KEY_AI_DETECT_FACE_ATTRIBUTES = "pref_ai_detect_face_attributes_key";
    public static final String KEY_AUTO_3DNR_PARAMETER = "pref_auto3dnr_param_key";
    public static final String KEY_AUTO_ADD_LOGOWATERMARK = "pref_auto_add_logowatermark_key";
    public static final String KEY_AUTO_ADD_TIMEWATERMARK = "pref_auto_add_timewatermark_key";
    public static final String KEY_MAKE_UP_DISPLAY = "pref_make_up_display_key";
    public static final String KEY_FILTER_VIDEO_DISPLAY = "pref_filter_video_display_key";
    public static final String KEY_PORTRAIT_REFOCUS_KEY = "pref_portrait_refocus_key";
    public static final String KEY_LIGHT_PORTIAIT_DISPLAY = "pref_light_portrait_display_key";
    public static final String KEY_LIGHT_PORTIAIT= "pref_light_portrait_key";
    public static final String KEY_PORTIAIT_REFORCUS_CLOSE = "pref_portrait_refocus_close_key";
    public static final String KEY_PORTIAIT_REFORCUS_LEVEL = "pref_portrait_refocus_leve_key";
    public static final String KEY_PORTRAIT_REFOCUS_DISPLAY_KEY = "pref_portrait_refocus_display_key";
    public static final String KEY_SWITCH_FRONTCAMERA_TO_BACK_AUTOMODE = "pre_switch_frontcamera_to_back_automode";
    public static final String KEY_SWITCH_PHOTO_TO_VIDEO = "pre_switch_photo_to_video";
    public static final String KEY_COLOR_EFFECT_PHOTO_DISPLAY = "pref_color_effect_photo_display_key";
    public static boolean isCameraBackFacing(DataModuleBasic mDataModule) {
        String vDefault = mDataModule.getStringDefault(KEY_CAMERA_ID);
        String vValue = mDataModule.getString(KEY_CAMERA_ID);
        return vDefault.equals(vValue);
    }

}
