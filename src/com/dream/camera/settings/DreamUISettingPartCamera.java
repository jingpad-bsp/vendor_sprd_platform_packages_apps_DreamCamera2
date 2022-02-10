
package com.dream.camera.settings;

import java.util.Set;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.android.camera.app.AppController;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import android.os.SystemProperties;
import android.preference.ListPreference;
import com.dream.camera.settings.DataModuleBasic.DreamSettingResourceChangeListener;
import com.sprd.camera.storagepath.StorageUtil;

import android.preference.Preference.OnPreferenceChangeListener;

public class DreamUISettingPartCamera extends DreamUISettingPartBasic implements DreamSettingResourceChangeListener, OnPreferenceChangeListener{

    public DreamUISettingPartCamera(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public DreamUISettingPartCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public DreamUISettingPartCamera(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
    }

    public DreamUISettingPartCamera(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // TODO Auto-generated constructor stub
    }

    private static final String TAG = "DreamUISettingPartCamera";

    // SPRD: fix for bug 499642 delete location save function
    private boolean isSupportGps =  CameraUtil.isRecordLocationEnable();

    @Override
    public void changContent() {
        mDataModule = DataModuleManager.getInstance(getContext())
                .getDataModuleCamera();
        mDataModule.addListener((DreamSettingResourceChangeListener)this);
        super.changContent();
    }

    public void onDreamSettingResourceChange(){
        changContent();
    }

    /*SPRD: fix bug 606536 not add ui change listener when back from secure camera @*/
    @Override
    public void addListener(Context context) {
        super.addListener(context);
    }
    /* @ */

    @Override
    public void releaseSource(Context context){
        if(mDataModule != null){
            super.releaseSource(context);
            mDataModule.removeListener((DreamSettingResourceChangeListener)this);
        }
    }

    @Override
    protected void updatePreItemsAccordingProperties() {

        // record location according if support gps
        updateVisibilityRecordLocation();

        // quick capture
        updateQuickCaptureShow();

        //hide storage path for audio picture and video modules
        updateVisibilityStoragePath();
    }

    private void updateVisibilityStoragePath() {
        if (isWithoutStoragePath()) {
            recursiveDelete(this, findPreference(Keys.KEY_CAMERA_STORAGE_PATH));
        }
    }

    private boolean isWithoutStoragePath () {
        //Sprd fix bug987316
        int modeId = ((AppController) getContext()).getCurrentModuleIndex();
        if (CameraUtil.isVideoExternalEnable()){
            return (SettingsScopeNamespaces.CONTINUE == modeId ||
                    SettingsScopeNamespaces.AUDIO_PICTURE == modeId);
        } else {
            return (SettingsScopeNamespaces.CONTINUE == modeId ||
                    SettingsScopeNamespaces.AUDIO_PICTURE == modeId ||
                    SettingsScopeNamespaces.AUTO_VIDEO == modeId ||
                    SettingsScopeNamespaces.TDNR_VIDEO == modeId ||
                    SettingsScopeNamespaces.SLOWMOTION == modeId ||
                    SettingsScopeNamespaces.TIMELAPSE == modeId);
        }
    }

    private void updateVisibilityRecordLocation() {
        if (!isSupportGps) {
            recursiveDelete(this, findPreference(Keys.KEY_RECORD_LOCATION));
        }
    }

    private void updateQuickCaptureShow() {
        if (CameraUtil.isQuickCaptureEnabled()) {
            ListPreference speedCapturePref = (ListPreference) findPreference(Keys.KEY_QUICK_CAPTURE);
            if (SystemProperties.getBoolean("persist.sys.cam.hascamkey", false)) {
                speedCapturePref.setTitle(R.string.pref_camera_quick_capture_title_camkey);
                speedCapturePref.setDialogTitle(R.string.pref_camera_quick_capture_title_camkey);
            }
        } else {
            recursiveDelete(this, findPreference(Keys.KEY_QUICK_CAPTURE));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(Keys.KEY_CAMERA_STORAGE_PATH) && !StorageUtil.KEY_DEFAULT_INTERNAL.equals(newValue)){
            return ((AppController) getContext()).requestScopedDirectoryAccess((String) newValue);
        }
        return super.onPreferenceChange(preference,newValue);
    }
}
