package com.dream.camera.settings;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Space;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.content.res.Resources;

import com.android.camera2.R;

public class DreamUIPreferenceItemList extends ListPreference implements
        DreamUIPreferenceItemInterface {

    public DreamUIPreferenceItemList(Context context) {
        super(context);
    }

    public DreamUIPreferenceItemList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DreamUIPreferenceItemList(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DreamUIPreferenceItemList(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public static final String TAG = "DreamUIPreferenceItemList";
    private DreamUISettingPartBasic mSettingPart;

    @Override
    protected boolean persistString(String value) {

        if (shouldPersist()) {
            // Shouldn't store null
            if (TextUtils.equals(value,getPersistedString(null))) {
                // It's already there, so the same as persisting
                return true;
            }

            if (mSettingPart != null) {
                return mSettingPart.persistString(getKey(), value);
            }

            return true;
        }
        return false;

    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {

        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        if (mSettingPart != null) {
            String value = mSettingPart.getPersistedString(getKey());
            return value == null ? defaultReturnValue : value;
        }

        return defaultReturnValue;
    }

    @Override
    public void initializeData(DreamUISettingPartBasic settingPart) {
        mSettingPart = settingPart;
        update();

    }

    AtomicBoolean updateDialog = new AtomicBoolean(false);

    @Override
    public void update() {
        this.setEntries(mSettingPart.getListEntries(getKey()));
        this.setEntryValues(mSettingPart.getListEntryValues(getKey()));
        this.setSummary(mSettingPart.getListSummaryFromKey(getKey()));
        this.setValue(mSettingPart.getListValue(getKey()));
        Log.e(TAG,
                "update key = " + getKey() + " value = "
                        + mSettingPart.getListValue(getKey()) + " summary = "
                        + mSettingPart.getListSummaryFromKey(getKey()) + " entries.length = "
                        + (mSettingPart.getListEntries(getKey()) == null ? 0 : mSettingPart.getListEntries(getKey()).length) + " entryValues.length = "
                        + (mSettingPart.getListEntryValues(getKey()) == null ? 0 : mSettingPart.getListEntryValues(getKey()).length));

        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            updateDialog.set(false);
            return;
        }

        updateDialog.set(true);
        dialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Dialog dialogOld = getDialog();
        boolean callShowDialog = true;
        if(dialogOld != null && dialogOld.getOwnerActivity() != null
            && (dialogOld.getOwnerActivity().isFinishing()
            || dialogOld.getOwnerActivity().isDestroyed())){
            callShowDialog = false;
        }

        super.onDismiss(dialog);
        if(updateDialog.get()){
            updateDialog.set(false);
            if(callShowDialog == true)
                showDialog(null);
        }
    }

    private boolean fromSummary = false;

    @Override
    public void setValue(String value) {
        fromSummary = false;
        super.setValue(value);
    }

    @Override
    public void setValueIndex(int index) {
        fromSummary = false;
        super.setValueIndex(index);
    }

    @Override
    public void setSummary(CharSequence summary) {
        fromSummary = true;
        super.setSummary(summary);
    }

    @Override
    public void setEnabled(boolean enabled) {
        fromSummary = false;
        super.setEnabled(enabled);

    }

    @Override
    protected void notifyChanged() {
        if (fromSummary) {
            return;
        }
        super.notifyChanged();
        if (mSettingPart != null) {
            this.setSummary(mSettingPart.getListSummaryFromKey(getKey()));
            // mSettingPart.notifyChanged(this);
        }
    }

    /*
     * Sprd:Fix bug807569 dialog UI need adjust
     */
    @Override
    protected void showDialog(Bundle state) {
        setNegativeButtonText(null);
        super.showDialog(state);
        setCustomDialogStyle();
    }
    private void setCustomDialogStyle() {
        Dialog mDialog = getDialog();
        if (mDialog == null)
            return;
        final Resources res = mDialog.getContext().getResources();
        //set titlePanel padding 24dp
        int titlePanelPanelId = res.getIdentifier("title_template", "id", "android");
        LinearLayout titlePanel = (LinearLayout) getDialog().findViewById(titlePanelPanelId);
        if (titlePanel != null) {
            titlePanel.setPadding(dp2px(res,24),dp2px(res,24),dp2px(res,24),0);
        }
        //set height of divider 16dp
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams (LayoutParams.MATCH_PARENT,dp2px(res,16));
        int dividerId = res.getIdentifier("titleDividerNoCustom", "id", "android");
        Space dividerSpace = (Space) getDialog().findViewById(dividerId);
        if (dividerSpace != null) {
            dividerSpace.setLayoutParams(params);
        }
        //set height of bottom 10dp
        int listPanelId = res.getIdentifier("select_dialog_listview", "id", "android");
        ListView listPanel = (ListView) getDialog().findViewById(listPanelId);
        if (listPanel != null) {
            listPanel.setPadding(0,0,0,dp2px(res,10));
        }
    }
    public int dp2px(Resources res, float dpValue) {
        final float scale = res.getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
