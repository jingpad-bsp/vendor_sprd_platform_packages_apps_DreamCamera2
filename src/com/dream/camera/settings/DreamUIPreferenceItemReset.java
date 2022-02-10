package com.dream.camera.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;
import com.android.camera2.R;
import com.android.camera.CameraActivity;

public class DreamUIPreferenceItemReset extends Preference {

    public DreamUIPreferenceItemReset(Context context) {
        super(context);
    }

    public DreamUIPreferenceItemReset(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DreamUIPreferenceItemReset(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DreamUIPreferenceItemReset(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onClick() {
        showAlertDialog();
    }

    private AlertDialog lastDialog;

    public void showAlertDialog() {
        if (lastDialog != null && lastDialog.isShowing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final AlertDialog alertDialog = builder.create();
        builder.setTitle(getContext().getString(R.string.dream_pref_restore_detail));
        builder.setMessage(getContext().getString(R.string.dream_restore_message));
        builder.setPositiveButton(getContext().getString(R.string.restore_done),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Sprd fix bug812099
                        if (((CameraActivity)getContext()).getCameraAppUI().isShutterClicked()){
                            android.util.Log.i("DreamUIPreferenceItemReset", "camera is recording or capturing, reset return");
                            return;
                        }
                        DataModuleManager.getInstance(getContext()).reset();
                    }
                });
        builder.setNegativeButton(getContext().getString(R.string.restore_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        lastDialog = dialog;
    }
}
