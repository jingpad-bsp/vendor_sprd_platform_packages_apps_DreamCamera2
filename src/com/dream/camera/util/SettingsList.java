
package com.dream.util;

import com.android.camera2.R;

public class SettingsList {

    public final static String[] EXPOSURE = {
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6"
    };

    public final static int EXPOSURE_DEFAULT = 3;

    public final static int[] EXPOSURE_DISPLAY = {
            R.string.pref_entry_negative_value_three,
            R.string.pref_entry_negative_value_two,
            R.string.pref_entry_negative_value_one,
            R.string.pref_entry_value_zero,
            R.string.pref_entry_value_one,
            R.string.pref_entry_value_two,
            R.string.pref_entry_value_three
    };

    public final static int[] FOCUSDISTANCE_DISPLAY_ICON = {
        R.drawable.ic_operate_focus_auto_selected,
        R.drawable.ic_operate_focus_auto_unselected,
        R.drawable.ic_operate_focus_micro_selected,
        R.drawable.ic_operate_focus_micro_unselected,
        R.drawable.ic_operate_focus_manual_selected,
        R.drawable.ic_operate_focus_manual_unselected,
        R.drawable.ic_operate_focus_infinity_selected,
        R.drawable.ic_operate_focus_infinity_unselected
    };

    public final static int[] FOCUSDISTANCE_DISPLAY = {
        R.string.pref_camera_focus_distance_entry_auto,
        R.string.pref_entry_focus_distance_value_40,
        R.string.pref_entry_focus_distance_value_80,
        R.string.pref_entry_focus_distance_value_100,
    };

    public final static String[] ISO = {
            "auto",
            "iso_100",
            "iso_200",
            "iso_400",
            "iso_800",
            "iso_1600"
    };

    public final static int ISO_DEFAULT = 0;

    public final static int[] ISO_DISPLAY = {
            R.string.pref_camera_iso_entry_auto,
            R.string.pref_entry_value_100,
            R.string.pref_entry_value_200,
            R.string.pref_entry_value_400,
            R.string.pref_entry_value_800,
            R.string.pref_entry_value_1600,
    };

    public final static String[] WHITE_BALANCE = {
            "auto",
            "daylight",
            "cloudy-daylight",
            "incandescent",
            "fluorescent"
    };

    public final static int WHITE_BALANCE_DEFAULT = 0;

    public final static int[] WHITE_BALANCE_DISPLAY = {
            R.string.pref_camera_whitebalance_entry_auto,
            R.string.pref_camera_whitebalance_entry_daylight,
            R.string.pref_camera_whitebalance_entry_cloudy,
            R.string.pref_camera_whitebalance_entry_incandescent,
            R.string.pref_camera_whitebalance_entry_fluorescent

    };

    public final static String[] CONTRAST = {
            "contrast_zero",
            "contrast_one",
            "contrast_two",
            "contrast_three",
            "contrast_four",
            "contrast_five",
            "contrast_six"
    };

    public final static int CONTRAST_DEFAULT = 3;

    public final static int[] CONTRAST_DISPLAY = {
            R.string.pref_entry_negative_value_three,
            R.string.pref_entry_negative_value_two,
            R.string.pref_entry_negative_value_one,
            R.string.pref_entry_value_zero,
            R.string.pref_entry_value_one,
            R.string.pref_entry_value_two,
            R.string.pref_entry_value_three
    };

    public final static String[] SATURATION = {
            "saturation_zero",
            "saturation_one",
            "saturation_two",
            "saturation_three",
            "saturation_four",
            "saturation_five",
            "saturation_six"
    };

    public final static int SATURATION_DEFAULT = 3;

    public final static int[] SATURATION_DISPLAY = {
            R.string.pref_entry_negative_value_three,
            R.string.pref_entry_negative_value_two,
            R.string.pref_entry_negative_value_one,
            R.string.pref_entry_value_zero,
            R.string.pref_entry_value_one,
            R.string.pref_entry_value_two,
            R.string.pref_entry_value_three
    };

    public static int indexOf(String str, String[] strArray, int defaultValue) {
        if (str == null || strArray == null || strArray.length == 0) {
            return defaultValue;
        }

        for (int i = 0; i < strArray.length; i++) {
            if (str.equals(strArray[i]))
                return i;
        }

        return defaultValue;
    }

}
