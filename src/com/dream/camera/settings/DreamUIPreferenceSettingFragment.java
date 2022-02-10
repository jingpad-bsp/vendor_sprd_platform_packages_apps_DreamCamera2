package com.dream.camera.settings;

import java.util.Set;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.camera.CameraActivity;

import com.android.camera2.R;
import com.dream.camera.settings.DreamUISettingPartBasic.OnCameraSettingChangeListener;
import com.android.camera.settings.Keys;
import com.sprd.camera.storagepath.StorageUtil;

public class DreamUIPreferenceSettingFragment extends PreferenceFragment
        implements OnCameraSettingChangeListener {

    private static final String TAG = "DreamUIPreferenceSettingFragment";

    DataStructSetting mDataSetting;

    PreferenceScreen mRoot;
    DreamUISettingPartBasic mCameraPart;
    DreamUISettingPartBasic mPhotoPart;
    DreamUISettingPartBasic mVideoPart;

    public DreamUIPreferenceSettingFragment() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataSetting = DataModuleManager.getInstance(getActivity())
                .getCurrentDataSetting();

        /*
         * SPRD: Fix nj monkey test: 2016-6-23 Caused by:
         * java.lang.NullPointerException: Attempt to read from field
         * 'java.lang.String
         * com.dream.camera.settings.DataStructSetting.mCategory' on a null
         * object reference .
         *
         * solution: when restore from activity oncreate method, if the
         * datasetting is null, do nothing until module initialize and bind the
         * setting button
         *
         * @{
         */
        if (mDataSetting == null) {
            Log.e(TAG,"mDataSetting == null in onCreate");
            return;
        }
        /* @} */
        // add xml
        addPreferencesFromResource(R.xml.dream_camera_preferences);

        // initialize UI part
        initialize();

        // change the visibility of preference
        changeUIVisibility();
    }

    /*SPRD: fix bug 606536 not add ui change listener when back from secure camera @*/
    @Override
    public void onResume() {
        super.onResume();
        /*Bug:631567 occur NullPointerException*/
        if (mDataSetting == null) {
            Log.e(TAG,"mDataSetting == null in onResume");
            return;
        }

        // delete some code for Bug 908781
        // the function of older bug535077 do not needed

        mCameraPart.addListener((CameraActivity)getActivity());
        if (mDataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            mPhotoPart.addListener((CameraActivity)getActivity());
        } else if (mDataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_VIDEO)){
            mVideoPart.addListener((CameraActivity)getActivity());
        }
    }
    /* @ */

    private void initialize() {
        mRoot = (PreferenceScreen) findPreference(getResIDString(R.string.preference_key_screen_camera_root));
        mCameraPart = (DreamUISettingPartBasic) findPreference(getResIDString(R.string.preference_key_category_camera_root));
        mPhotoPart = (DreamUISettingPartBasic) findPreference(getResIDString(R.string.preference_key_category_photo_root));
        mVideoPart = (DreamUISettingPartBasic) findPreference(getResIDString(R.string.preference_key_category_video_root));

        mCameraPart.changContent();
    }

    private void changeUIVisibility() {
        if (mDataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
            setPhotoModuleVisiblity();
        } else if (mDataSetting.mCategory
                .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
            setVideoModuleVisibility();
        }

    }

    private void setPhotoModuleVisiblity() {
        recursiveDelete(mRoot, mVideoPart);
        mPhotoPart.changContent();
    }

    private void setVideoModuleVisibility() {
        recursiveDelete(mRoot, mPhotoPart);
        mVideoPart.changContent();
    }

    private String getResIDString(int resID) {
        return getResources().getString(resID);
    }

    // private void updateUIItem() {
    // if (mDataSetting.mCategory
    // .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
    //
    // } else if (mDataSetting.mCategory
    // .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
    //
    // }
    //
    // }

    /**
     * Recursively traverses the tree from the given group as the route and
     * tries to delete the preference. Traversal stops once the preference was
     * found and removed.
     */
    private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
        if (group == null) {
            Log.d(TAG, "attempting to delete from null preference group");
            return false;
        }
        if (preference == null) {
            Log.d(TAG, "attempting to delete null preference");
            return false;
        }
        if (group.removePreference(preference)) {
            // Removal was successful.
            return true;
        }

        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                if (recursiveDelete((PreferenceGroup) pref, preference)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCameraSettingChange(Set<String> paramaterList) {

    }

    public void resetSettings() {
        updateAllPreference(mRoot);
    }

    private void updateAllPreference(PreferenceGroup group) {
        if (group == null) {
            Log.d(TAG, "attempting to delete from null preference group");
            return;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                updateAllPreference((PreferenceGroup) pref);
            } else if (pref instanceof DreamUIPreferenceItemInterface) {
                ((DreamUIPreferenceItemInterface) pref).update();
            }
        }
    }

    public void releaseResource() {
        if (mCameraPart != null) {
            mCameraPart.releaseSource( (CameraActivity)getActivity());
        }
        if (mDataSetting != null) {
            if (mDataSetting.mCategory
                    .equals(DataConfig.CategoryType.CATEGORY_PHOTO)) {
                mPhotoPart.releaseSource( (CameraActivity)getActivity());
            } else if (mDataSetting.mCategory
                    .equals(DataConfig.CategoryType.CATEGORY_VIDEO)) {
                mVideoPart.releaseSource( (CameraActivity)getActivity());
            }
        }
    }
    public void dialogDismiss(String key){
        ListPreference storagePreference = (ListPreference) findPreference(key);
        if (storagePreference != null && storagePreference.getDialog() != null) {
            storagePreference.getDialog().dismiss();
        }
    }

    /*SPRD:fix bug607898 fix setting ui when back from home/secure camera, last time pause camera by pressing home @{ */
    public DataStructSetting getDataSetting() {
        return mDataSetting;
    }
    /* @} */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference != null && preference instanceof ListPreference) {
            keyOfDialog = preference.getKey();
            Log.d(TAG, "onPreferenceTreeClick preference:" + preference + " keyOfDialog:" + keyOfDialog);
        }
        //Sprd fix bug812099
        if (((CameraActivity)getActivity()).getCameraAppUI().isShutterClicked()) {
            Log.i(TAG, "camera is recording or capturing, return");
            dismissDialogIfNecessary();
            return true;
        }
        if (preference != null) // Bug 1159277 (FORWARD_NULL)
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        else
            return false;
    }

    private String keyOfDialog;
    @Override
    public void onPause() {
        super.onPause();
        dismissDialogIfNecessary();
    }
    public void dismissDialogIfNecessary(){
        if (keyOfDialog != null) {
            Log.i(TAG, "dismissDialogIfNecessary keyOfDialog="+keyOfDialog);
            dialogDismiss(keyOfDialog);
        }
        keyOfDialog = null;
    }

    //SPRD: Fix bug 944410
    public void updateSettingAeLock(String key,boolean enabled) {
        Preference pf = null;
        pf = findPreference(key);

        if (null != pf) {
            pf.setEnabled(!enabled);
        }
    }
}
