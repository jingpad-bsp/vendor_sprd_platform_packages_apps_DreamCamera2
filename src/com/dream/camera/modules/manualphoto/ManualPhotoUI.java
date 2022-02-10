package com.dream.camera.modules.manualphoto;

import java.util.HashMap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.ImageView;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.app.AppController;

import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

import com.android.camera.PhotoUI;
import com.android.ex.camera2.portability.debug.Log;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DataStorageStruct;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModulePhoto;
import com.dream.util.SettingsList;
import com.dream.camera.settings.DreamSettingUtil;
import com.dream.camera.ui.AutoCenterHorizontalScrollView;
import com.dream.camera.ui.ManualAeAfPanel;
import java.util.ArrayList;
import java.util.List;
import com.dream.camera.ui.HorizontalAdapter;
import com.dream.camera.util.DreamUtil;
import com.android.camera.util.CameraUtil;

import android.os.Handler;
import java.util.Random;

public class ManualPhotoUI extends PhotoUI implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    // top panel
    private View topPanel;
    // extend panel
    private View extendPanel;
    private ManualAeAfPanel mAEAFPanel;

    private View eExposureBtn;
    private ImageView eExposureIcon;
    private TextView eExposureTv;
    private View eExposurePanel;
    private SeekBar eExposureSeekBar;

    private View eIsoBtn;
    private ImageView eIsoIcon;
    private TextView eIsoTv;
    private View eIsoPanel;
    private SeekBar eIsoSeekBar;

    private View eExposureTimeBtn;
    private ImageView eExposureTimeIcon;
    private TextView eExposureTimeTv;
    private View eExposureTimePanel;
    //private SeekBar eExposureTimeSeekBar;

    private View eFocusDistanceBtn;
    private ImageView eFocusDistanceIcon;
    private TextView eFocusDistanceTv;
    private View eFocusDistancePanel;
    private SeekBar eFocusDistanceSeekBar;

    private View eWbBtn;
    private ImageView eWbIcon;
    private TextView eWbTv;
    private View eWbPanel;
    private SeekBar eWbSeekBar;

    private View eStyleBtn;
    private ImageView eStyleIcon;
    private TextView eStyleTv;
    private View eStylePanel;

    private SeekBar eContrastSeekBar;
    private SeekBar eSaturationSeekBar;
    private Handler mHandler;

    public ManualPhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);
        if (CameraUtil.isAEAFSeparateEnable()) {
            if (mAEAFPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                View extendPanel = lf.inflate(R.layout.layout_manual_ae_af_panel,
                        getModuleRoot(), true);
                mAEAFPanel = extendPanel.findViewById(R.id.manual_ae_af_panel);
            }
        }
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {

        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            topPanel = lf.inflate(R.layout.manualphoto_top_panel,
                    topPanelParent);
        }

        bindTopButtons();
    }

    private void bindTopButtons() {
        mActivity.getButtonManager().load(topPanel);

//        tSettingsButton = (ImageButton) topPanel
//                .findViewById(R.id.settings_button_dream);
//
//        bindSettingsButton(tSettingsButton);
        bindFlashButton();
        //bindCountDownButton();
        bindMeteringButton();
        bindCameraButton();
        bindMeteringButton();
    }

    protected void updateExposureTimeUI() {
        initHorizontal(mExposureTimeList);
    }
    /*SPRD:fix bug 622818 add for exposure to adapter auto @{*/
    @Override
    protected void updateExposureUI() {
        int exposure = DataModuleManager.getInstance(mActivity)
                .getDataModulePhoto().getInt(Keys.KEY_EXPOSURE);
        int exposureIndex = exposure + 3;//Sprd:fix bug957185
        //int exposureIndex = convertExposureValueToIndex(exposure);
        if (eExposureTv != null) {
            eExposureTv.setText(mActivity.getAndroidContext().getString(
                    SettingsList.EXPOSURE_DISPLAY[exposureIndex]));
        }
        if (eExposureSeekBar != null) {
            eExposureSeekBar.setProgress(exposureIndex);
        }
    }
    /* @} */

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        if (extendPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);
            extendPanel = lf.inflate(R.layout.manualphoto_extend_panel,
                    extendPanelParent);
        }

        // EXPOSURE
        eExposureBtn = extendPanelParent.findViewById(R.id.e_exposure_btn);
        eExposureBtn.setOnClickListener(this);

        eExposureIcon = (ImageView) extendPanelParent
                .findViewById(R.id.e_exposure_icon);
        eExposureTv = (TextView) extendPanelParent
                .findViewById(R.id.e_exposure_tv);
        //eExposureTv.setText(mActivity.getAndroidContext().getString(
        //        SettingsList.EXPOSURE_DISPLAY[exposureIndex]));

        eExposurePanel = extendPanelParent.findViewById(R.id.e_exposure_panel);

        eExposureSeekBar = (SeekBar) extendPanelParent
                .findViewById(R.id.e_exposure_seekbar);
        eExposureSeekBar.setMax(SettingsList.EXPOSURE_DISPLAY.length - 1);
        //eExposureSeekBar.setProgress(exposureIndex);
        eExposureSeekBar.setOnSeekBarChangeListener(this);
        updateExposureUI();

        //ExposureTime ShutterSpeed
        if (CameraUtil.isManualShutterEnabled()) {
            eExposureTimeBtn = extendPanelParent.findViewById(R.id.e_exposure_time_btn);
            eExposureTimeBtn.setVisibility(View.VISIBLE);
            eExposureTimeBtn.setOnClickListener(this);
            eExposureTimePanel = extendPanelParent.findViewById(R.id.e_exposure_time_panel);
            eExposureTimeIcon = extendPanelParent.findViewById(R.id.e_exposure_time_icon);
            eExposureTimeTv = extendPanelParent.findViewById(R.id.e_exposure_time_tv);
        }

        // focus distance
        if (CameraUtil.isManualFocusEnabled()) {
            eFocusDistanceBtn = extendPanelParent.findViewById(R.id.e_focus_distance_btn);
            eFocusDistanceBtn.setVisibility(View.VISIBLE);
            eFocusDistanceBtn.setOnClickListener(this);
            int focusDistanceIndex = DreamSettingUtil.convertToInt(DataModuleManager.getInstance(mActivity).getDataModulePhoto()
                    .getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_CAMERA_FOCUS_DISTANCE, "0"));
            eFocusDistanceIcon = (ImageView) extendPanelParent.findViewById(R.id.e_focus_distance_icon);
            eFocusDistanceTv = (TextView) extendPanelParent.findViewById(R.id.e_focus_distance_tv);
            int displayIndex = getFocusDistanceDisplayIndex(focusDistanceIndex);
            eFocusDistanceIcon.setImageResource(SettingsList.FOCUSDISTANCE_DISPLAY_ICON[displayIndex * 2 + 1]);
            eFocusDistanceTv.setText(SettingsList.FOCUSDISTANCE_DISPLAY[displayIndex]);
            eFocusDistancePanel = extendPanelParent.findViewById(R.id.e_focus_distance_panel);
            eFocusDistanceSeekBar = (SeekBar) extendPanelParent.findViewById(R.id.e_focus_distance_seekbar);
            eFocusDistanceSeekBar.setMax(100);
            eFocusDistanceSeekBar.setProgress(focusDistanceIndex);
            eFocusDistanceSeekBar.setOnSeekBarChangeListener(this);
        }

        // ISO
        eIsoBtn = extendPanelParent.findViewById(R.id.e_iso_btn);
        eIsoBtn.setOnClickListener(this);

        String iso = DataModuleManager.getInstance(mActivity)
                .getDataModulePhoto().getString(Keys.KEY_CAMERA_ISO);
        int isoIndex = SettingsList.indexOf(iso, SettingsList.ISO,
                SettingsList.ISO_DEFAULT);

        eIsoIcon = (ImageView) extendPanelParent.findViewById(R.id.e_iso_icon);
        eIsoTv = (TextView) extendPanelParent.findViewById(R.id.e_iso_tv);
        eIsoTv.setText(mActivity.getAndroidContext().getString(
                SettingsList.ISO_DISPLAY[isoIndex]));

        eIsoPanel = extendPanelParent.findViewById(R.id.e_iso_panel);

        eIsoSeekBar = (SeekBar) extendPanelParent
                .findViewById(R.id.e_iso_seekbar);
        eIsoSeekBar.setMax(SettingsList.ISO_DISPLAY.length - 1);
        eIsoSeekBar.setProgress(isoIndex);
        eIsoSeekBar.setOnSeekBarChangeListener(this);

        // WHITE_BALANCE
        eWbBtn = extendPanelParent.findViewById(R.id.e_wb_btn);
        eWbBtn.setOnClickListener(this);

        String wb = DataModuleManager.getInstance(mActivity)
                .getDataModulePhoto().getString(Keys.KEY_WHITE_BALANCE);
        int wbIndex = SettingsList.indexOf(wb, SettingsList.WHITE_BALANCE,
                SettingsList.WHITE_BALANCE_DEFAULT);

        eWbIcon = (ImageView) extendPanelParent.findViewById(R.id.e_wb_icon);
        eWbTv = (TextView) extendPanelParent.findViewById(R.id.e_wb_tv);
        eWbTv.setText(mActivity.getAndroidContext().getString(
                SettingsList.WHITE_BALANCE_DISPLAY[wbIndex]));

        eWbPanel = extendPanelParent.findViewById(R.id.e_wb_panel);

        eWbSeekBar = (SeekBar) extendPanelParent
                .findViewById(R.id.e_wb_seekbar);
        eWbSeekBar.setMax(SettingsList.WHITE_BALANCE_DISPLAY.length - 1);
        eWbSeekBar.setProgress(wbIndex);
        eWbSeekBar.setOnSeekBarChangeListener(this);

        // Style
        eStyleBtn = extendPanelParent.findViewById(R.id.e_style_btn);
        eStyleBtn.setOnClickListener(this);

        eStyleIcon = (ImageView) extendPanelParent
                .findViewById(R.id.e_style_icon);
        eStyleTv = (TextView) extendPanelParent.findViewById(R.id.e_style_tv);

        eStylePanel = extendPanelParent.findViewById(R.id.e_style_panel);

        // CONTRAST
        String contrast = DataModuleManager.getInstance(mActivity)
                .getDataModulePhoto().getString(Keys.KEY_CAMERA_CONTRAST);
        int contrastIndex = SettingsList.indexOf(contrast,
                SettingsList.CONTRAST, SettingsList.CONTRAST_DEFAULT);

        eContrastSeekBar = (SeekBar) extendPanelParent
                .findViewById(R.id.e_contrast_seekbar);
        eContrastSeekBar.setMax(SettingsList.CONTRAST_DISPLAY.length - 1);
        eContrastSeekBar.setProgress(contrastIndex);
        eContrastSeekBar.setOnSeekBarChangeListener(this);

        // SATURATION
        String saturation = DataModuleManager.getInstance(mActivity)
                .getDataModulePhoto().getString(Keys.KEY_CAMERA_SATURATION);
        int saturatioIndex = SettingsList.indexOf(saturation,
                SettingsList.SATURATION, SettingsList.SATURATION_DEFAULT);

        eSaturationSeekBar = (SeekBar) extendPanelParent
                .findViewById(R.id.e_saturation_seekbar);
        eSaturationSeekBar.setMax(SettingsList.SATURATION_DISPLAY.length - 1);
        eSaturationSeekBar.setProgress(saturatioIndex);
        eSaturationSeekBar.setOnSeekBarChangeListener(this);

        changeColorStyleDescription();
        updateEVStatus();
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        super.updateSlidePanel();
    }

    private void hideAllSeekbar() {
        if (eExposurePanel != null) {
            eExposurePanel.setVisibility(View.GONE);
        }

        if (eExposureTimePanel != null) {
            eExposureTimePanel.setVisibility(View.GONE);
        }
        if (eFocusDistancePanel != null) {
            eFocusDistancePanel.setVisibility(View.GONE);
        }

        if (eIsoPanel != null) {
            eIsoPanel.setVisibility(View.GONE);
        }
        if (eWbPanel != null) {
            eWbPanel.setVisibility(View.GONE);
        }
        if (eStylePanel != null) {
            eStylePanel.setVisibility(View.GONE);
        }
    }

    private int getFocusDistanceDisplayIndex(int progress) {
        int index = 0;
        if (progress == 0) {
            index = 0;
        } else if (progress <= 40) {
            index = 1;
        } else if (progress >= 80) {
            index = 3;
        } else {
            index = 2;
        }
        return index;
    }

    private void updateFocusDistanceDisplayUI(int progress) {
        int index = getFocusDistanceDisplayIndex(progress);
        eFocusDistanceIcon.setImageResource(SettingsList.FOCUSDISTANCE_DISPLAY_ICON[index * 2]);
        eFocusDistanceTv.setText(SettingsList.FOCUSDISTANCE_DISPLAY[index]);
        eFocusDistanceTv.setTextColor(((AppController) mActivity).getAndroidContext().getResources()
                .getColor(R.color.dream_yellow));
    }

    private void changeOneIconUI(View button , ImageView icon , View view , TextView tv , int visible ,
                                 boolean showOrHide , int res_selected , int res_unselected){
        if (button != null) {
            showOrHide = (view == button && visible == View.VISIBLE);
            if (showOrHide == true)
            {
                icon.setImageResource(res_selected);
                tv.setTextColor(((AppController) mActivity)
                        .getAndroidContext().getResources()
                        .getColor(R.color.dream_yellow));
            } else {
                icon.setImageResource(res_unselected);
                tv.setTextColor(Color.WHITE);
            }
        }
    }

    private void changeIconUI(View view, int visible) {
        boolean showOrHide = false;
        // Bug 839474: CCN optimization
        // split to another 1 method for CCN optimization , from 17 to 1+4
        changeOneIconUI(eExposureBtn , eExposureIcon , view , eExposureTv , visible , showOrHide ,
                R.drawable.ic_operate_ev_sprd_selected,
                R.drawable.ic_operate_ev_sprd_unselected);

        changeOneIconUI(eExposureTimeBtn , eExposureTimeIcon , view , eExposureTimeTv , visible , showOrHide ,
                R.drawable.ic_operate_shutter_selected,
                R.drawable.ic_operate_shutter_unselected);

        int selectedIndex = 0;
        int unSelectedIndex = 0;
        if (eFocusDistanceSeekBar != null) {
            selectedIndex = getFocusDistanceDisplayIndex(eFocusDistanceSeekBar.getProgress()) * 2;
            unSelectedIndex = getFocusDistanceDisplayIndex(eFocusDistanceSeekBar.getProgress()) * 2 + 1;
        }
        changeOneIconUI(eFocusDistanceBtn , eFocusDistanceIcon , view , eFocusDistanceTv , visible , showOrHide ,
                SettingsList.FOCUSDISTANCE_DISPLAY_ICON[selectedIndex],
                SettingsList.FOCUSDISTANCE_DISPLAY_ICON[unSelectedIndex]);

        changeOneIconUI(eIsoBtn , eIsoIcon , view , eIsoTv , visible , showOrHide ,
                R.drawable.ic_operate_iso_sprd_selected,
                R.drawable.ic_operate_iso_sprd_unselected);

        changeOneIconUI(eWbBtn , eWbIcon , view , eWbTv , visible , showOrHide ,
                R.drawable.ic_operate_wb_auto_sprd_selected,
                R.drawable.ic_operate_wb_auto_sprd_unselected);

        changeOneIconUI(eStyleBtn , eStyleIcon , view , eStyleTv , visible , showOrHide ,
                R.drawable.ic_operate_art_style_sprd_selected,
                R.drawable.ic_operate_art_style_sprd_unselected);

        updateEVStatus();
    }

    private int onClickOneBtn(View panel)
    {
        int ret = 0;
        int visibility = panel.getVisibility();
        if (visibility == View.GONE) {
            hideAllSeekbar();
            ret = View.VISIBLE;
        } else if (visibility == View.VISIBLE) {
            ret = View.GONE;
        }
        panel.setVisibility(ret);
        return ret;
    }

    private boolean isTheBtn(View button , View view)
    {
        return (button != null && view == button);
    }

    @Override
    public void onClick(View view) {
        int visible = View.VISIBLE;
        // Bug 839474: CCN optimization
        // split to 2 methods for CCN optimization , from 17 to 5+3+2
        if (isTheBtn(eExposureBtn , view)) {
            visible = onClickOneBtn(eExposurePanel);
            changeIconUI(view, visible);
            return;
        }

        if (isTheBtn(eExposureTimeBtn , view)) {
            visible = onClickOneBtn(eExposureTimePanel);
            changeIconUI(view, visible);
            mAutoCenterHorizontalScrollView.setVisibility(visible);
            return;
        }

        if (isTheBtn(eFocusDistanceBtn , view)) {
            visible = onClickOneBtn(eFocusDistancePanel);
            changeIconUI(view, visible);
            return;
        }

        if (isTheBtn(eStyleBtn , view)) {
            visible = onClickOneBtn(eStylePanel);
            changeIconUI(view, visible);
            return;
        }

        if (isTheBtn(eIsoBtn , view)) {
            visible = onClickOneBtn(eIsoPanel);
            changeIconUI(view, visible);
            return;
        }

        if (isTheBtn(eWbBtn , view)) {
            visible = onClickOneBtn(eWbPanel);
            changeIconUI(view, visible);
            return;
        }

        if (isTheBtn(eStyleBtn , view)) {
            visible = onClickOneBtn(eStylePanel);
            changeIconUI(view, visible);
            return;
        }
    }

    private void onExposureChanged(DataModuleBasic currentDataModule , SeekBar seekBar , int progress)
    {
        if (eExposureSeekBar != null && seekBar == eExposureSeekBar) {
            currentDataModule.set(Keys.KEY_EXPOSURE_COMPENSATION_ENABLED, true);
            /*SPRD:fix bug 622818 add for exposure to adapter auto @{*/
            if (mExposureCompensationStep != 0.0f) {
                //int exposureValue = Math.round((progress - 3) / mExposureCompensationStep);
                //int exposureValue = Math.round(progress - 3);
                int exposureValue = progress - 3;
                mBasicModule.setExposureCompensation(exposureValue);
                mBasicModule.applySettings();
                // nj dream test 124
                eExposureTv.setText(mActivity.getAndroidContext().getString(
                        SettingsList.EXPOSURE_DISPLAY[progress]));
            }
            /* @} */
        }
    }

    private void onExposureTimeChanged(DataModuleBasic currentDataModule, int index) {
        // update KEY_SHUTTER_EXPOSURE_TIME
        // Random random = new Random();
        // index = random.nextInt(mExposureTimeList.size() - 1);
        if (mExposureTimeList != null && index < mExposureTimeList.size()) {
            mBasicModule.setExposureTime(index);
            eExposureTimeTv.setText(mExposureTimeList.get(index));
            updateEVStatus();
        }
    }

    private void onFocusDistanceChanged(DataModuleBasic currentDataModule, SeekBar seekBar,
            int progress) {
        if (eFocusDistanceSeekBar != null && seekBar == eFocusDistanceSeekBar) {
            mBasicModule.setFocusDistance(progress);
            updateFocusDistanceDisplayUI(progress);
        }
    }

    private void onIsoChanged(DataModuleBasic currentDataModule , SeekBar seekBar , int progress)
    {
        if (eIsoSeekBar != null && seekBar == eIsoSeekBar) {
            currentDataModule.set(Keys.KEY_CAMERA_ISO,
                    SettingsList.ISO[progress]);
            mBasicModule.updateParametersISO();
            mBasicModule.applySettings();
            // nj dream test 124
            eIsoTv.setText(mActivity.getAndroidContext().getString(
                    SettingsList.ISO_DISPLAY[progress]));
        }
    }

    private void onWbChanged(DataModuleBasic currentDataModule , SeekBar seekBar , int progress)
    {
        if (eWbSeekBar != null && seekBar == eWbSeekBar) {
            currentDataModule.set(Keys.KEY_WHITE_BALANCE,
                    SettingsList.WHITE_BALANCE[progress]);
            mBasicModule.updateParametersWhiteBalance();
            mBasicModule.applySettings();
            // nj dream test 124
            eWbTv.setText(mActivity.getAndroidContext().getString(
                    SettingsList.WHITE_BALANCE_DISPLAY[progress]));
        }
    }

    private void onOther3Changed(DataModuleBasic currentDataModule , SeekBar seekBar , int progress)
    {
        if (eContrastSeekBar != null && seekBar == eContrastSeekBar) {
            currentDataModule.set(Keys.KEY_CAMERA_CONTRAST,
                    SettingsList.CONTRAST[progress]);
            mBasicModule.updateParametersContrast();
            mBasicModule.applySettings();
        }

        if (eSaturationSeekBar != null && seekBar == eSaturationSeekBar) {
            currentDataModule.set(Keys.KEY_CAMERA_SATURATION,
                    SettingsList.SATURATION[progress]);
            mBasicModule.updateParametersSaturation();
            mBasicModule.applySettings();
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        if (!fromUser)
            return;

        DataModuleBasic currentDataModule = DataModuleManager.getInstance(
                mActivity).getDataModulePhoto();

        // Bug 839474: CCN optimization
        // split to 4 methods for CCN optimization , from 17 to 2+4+3+3+7
        onExposureChanged(currentDataModule , seekBar , progress);

        //onExposureTimeChanged(currentDataModule, seekBar, progress);
        onFocusDistanceChanged(currentDataModule, seekBar, progress);

        onIsoChanged(currentDataModule , seekBar , progress);
        onWbChanged(currentDataModule , seekBar , progress);
        onOther3Changed(currentDataModule , seekBar , progress);

        changeColorStyleDescription();
        updateEVStatus();
    }

    private void changeColorStyleDescription() {
        DataModuleBasic currentDataModule = DataModuleManager.getInstance(
                mActivity).getDataModulePhoto();
        // Monkey crash happened on 6-23 020
        // currentDataModule.getString(Keys.KEY_CAMERA_CONTRAST) may be null
        if (SettingsList.CONTRAST[SettingsList.CONTRAST_DEFAULT].equals(currentDataModule
                .getString(Keys.KEY_CAMERA_CONTRAST))
                && SettingsList.SATURATION[SettingsList.SATURATION_DEFAULT]
                        .equals(currentDataModule
                                .getString(Keys.KEY_CAMERA_SATURATION))){
            eStyleTv.setText(R.string.extend_panel_style_default);
        } else {
            eStyleTv.setText(R.string.extend_panel_style_customize);
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onSettingReset() {
        /*
        if (eExposureSeekBar != null) {
            updateExposureUI();//SPRD:fix bug622818
            //mBasicModule.updateParametersExposureCompensation();
        }

        if (eIsoSeekBar != null) {
            String iso = DataModuleManager.getInstance(mActivity)
                    .getDataModulePhoto().getString(Keys.KEY_CAMERA_ISO);
            int isoIndex = SettingsList.indexOf(iso, SettingsList.ISO,
                    SettingsList.ISO_DEFAULT);

            eIsoTv.setText(mActivity.getAndroidContext().getString(
                    SettingsList.ISO_DISPLAY[isoIndex]));

            eIsoSeekBar.setProgress(isoIndex);
            //mBasicModule.updateParametersISO();
        }

        if (eWbSeekBar != null) {
            String wb = DataModuleManager.getInstance(mActivity)
                    .getDataModulePhoto().getString(Keys.KEY_WHITE_BALANCE);
            int wbIndex = SettingsList.indexOf(wb, SettingsList.WHITE_BALANCE,
                    SettingsList.WHITE_BALANCE_DEFAULT);

            eWbTv.setText(mActivity.getAndroidContext().getString(
                    SettingsList.WHITE_BALANCE_DISPLAY[wbIndex]));

            eWbSeekBar.setProgress(wbIndex);
            //mBasicModule.updateParametersWhiteBalance();
        }

        if (eContrastSeekBar != null) {
            String contrast = DataModuleManager.getInstance(mActivity)
                    .getDataModulePhoto().getString(Keys.KEY_CAMERA_CONTRAST);
            int contrastIndex = SettingsList.indexOf(contrast,
                    SettingsList.CONTRAST, SettingsList.CONTRAST_DEFAULT);

            eContrastSeekBar.setProgress(contrastIndex);
            //mBasicModule.updateParametersContrast();
        }

        if (eSaturationSeekBar != null) {
            String saturation = DataModuleManager.getInstance(mActivity)
                    .getDataModulePhoto().getString(Keys.KEY_CAMERA_SATURATION);
            int saturatioIndex = SettingsList.indexOf(saturation,
                    SettingsList.SATURATION, SettingsList.SATURATION_DEFAULT);

            eSaturationSeekBar.setProgress(saturatioIndex);
            //mBasicModule.updateParametersSaturation();
        }

        if (eBrightnessSeekBar != null) {
            String brightness = DataModuleManager.getInstance(mActivity)
                    .getDataModulePhoto().getString(Keys.KEY_CAMERA_BRIGHTNESS);
            int brightnessIndex = SettingsList.indexOf(brightness,
                    SettingsList.BRIGHTNESS, SettingsList.BRIGHTNESS_DEFAULT);

            eBrightnessSeekBar.setProgress(brightnessIndex);
            //mBasicModule.updateParametersBrightness();
        }
        //mBasicModule.applySettings();
        eStyleTv.setText(R.string.extend_panel_style_default);
        */
    }

    private int convertExposureValueToIndex(int exposure) {
        if (mExposureCompensationStep == 0.0f) {
            return 0;
        }
        return (int)(exposure * mExposureCompensationStep) + 3;
    }

    @Override
    public void onSingleTapUp() {
        int visible = View.VISIBLE;
        if (eExposureBtn != null) {
            if (eExposurePanel.getVisibility() == View.VISIBLE) {
                eExposurePanel.setVisibility(View.GONE);
                changeIconUI(eExposureBtn, View.GONE);
            }
        }

        if (eExposureTimeBtn != null) {
            if (eExposureTimePanel.getVisibility() == View.VISIBLE) {
                eExposureTimePanel.setVisibility(View.GONE);
                changeIconUI(eExposureTimeBtn, View.GONE);
            }
        }

        if (eFocusDistanceBtn != null) {
            if (eFocusDistancePanel.getVisibility() == View.VISIBLE) {
                eFocusDistancePanel.setVisibility(View.GONE);
                changeIconUI(eFocusDistanceBtn, View.GONE);
            }
        }

        if (eIsoBtn != null) {
            if (eIsoPanel.getVisibility() == View.VISIBLE) {
                eIsoPanel.setVisibility(View.GONE);
                changeIconUI(eIsoBtn, View.GONE);
            }
        }

        if (eWbBtn != null) {
            if (eWbPanel.getVisibility() == View.VISIBLE) {
                eWbPanel.setVisibility(View.GONE);
                changeIconUI(eWbBtn, View.GONE);
            }
        }

        if (eStyleBtn != null) {
            if (eStylePanel.getVisibility() == View.VISIBLE) {
                eStylePanel.setVisibility(View.GONE);
                changeIconUI(eStyleBtn, View.GONE);
            }
        }
    }

    public boolean isManualControlPressed() {
        return ((mAutoCenterHorizontalScrollView != null && mAutoCenterHorizontalScrollView.isScrolling) ||
                (eExposureSeekBar != null && eExposureSeekBar.isPressed()) ||
                (eIsoSeekBar != null && eIsoSeekBar.isPressed()) ||
                (eWbSeekBar != null && eWbSeekBar.isPressed()) ||
                (eContrastSeekBar != null && eContrastSeekBar.isPressed()) ||
                (eSaturationSeekBar != null && eSaturationSeekBar.isPressed()) ||
                (eFocusDistanceSeekBar != null && eFocusDistanceSeekBar.isPressed()));
    }

    public void setManualControlEnable(boolean isEnabled) {
        if (eExposureSeekBar != null) {
            eExposureSeekBar.setEnabled(isEnabled);
        }
        if (mAutoCenterHorizontalScrollView != null) {
            mAutoCenterHorizontalScrollView.setScrollable(isEnabled);
        }
        if (eIsoSeekBar != null) {
            eIsoSeekBar.setEnabled(isEnabled);
        }
        if (eWbSeekBar != null) {
            eWbSeekBar.setEnabled(isEnabled);
        }
        if (eContrastSeekBar != null) {
            eContrastSeekBar.setEnabled(isEnabled);
        }
        if (eSaturationSeekBar != null) {
            eSaturationSeekBar.setEnabled(isEnabled);
        }
        if (eFocusDistanceSeekBar != null) {
            eFocusDistanceSeekBar.setEnabled(isEnabled);
        }
    }
    private AutoCenterHorizontalScrollView mAutoCenterHorizontalScrollView;
    private void initHorizontal(ArrayList timeList) {
        if (mAutoCenterHorizontalScrollView != null) return;
        DataModuleBasic currentDataModule = DataModuleManager.getInstance(mActivity).getDataModulePhoto();
        mHandler = mBasicModule.getHandler();
        mAutoCenterHorizontalScrollView = mActivity.findViewById(R.id.shuttertime_list);
        HorizontalAdapter hadapter = new HorizontalAdapter(mActivity, timeList);

        int index = DreamSettingUtil.convertToInt(currentDataModule.getString(DataConfig.SettingStoragePosition.positionList[3], Keys.KEY_EXPOSURE_SHUTTERTIME, "0"));
        mAutoCenterHorizontalScrollView.setHandler(mHandler);
        mAutoCenterHorizontalScrollView.setCurrentIndex(index);
        mAutoCenterHorizontalScrollView.setAdapter(hadapter);
        mAutoCenterHorizontalScrollView
                .setOnSelectChangeListener(new AutoCenterHorizontalScrollView.OnSelectChangeListener() {
                    @Override
                    public void onSelectChange(int position) {
                        onExposureTimeChanged(currentDataModule, position);
                    }
                });
    }

    private void updateEVStatus(){
        DataModuleBasic currentDataModule = DataModuleManager.getInstance(
                mActivity).getDataModulePhoto();

        if(!currentDataModule.isEnableSettingConfig(Keys.KEY_CAMERA_ISO) ||
                !currentDataModule.isEnableSettingConfig(Keys.KEY_EXPOSURE_SHUTTERTIME)) {
            return;
        }
        boolean isoIsAuto = currentDataModule.
                getString(Keys.KEY_CAMERA_ISO).equals(SettingsList.ISO[0]);
        boolean evTimeIsAuto = currentDataModule.getString(Keys.KEY_EXPOSURE_SHUTTERTIME).equals("0");

        if(!isoIsAuto&&!evTimeIsAuto){
            eExposureBtn.setEnabled(false);
            eExposureIcon.setImageResource(R.drawable.ic_operate_ev_sprd_disabled);
            eExposureTv.setTextColor(((AppController) mActivity)
                    .getAndroidContext().getResources()
                    .getColor(R.color.make_up_color_disable));

        } else {
            eExposureBtn.setEnabled(true);
            eExposureIcon.setImageResource(R.drawable.ic_operate_ev_sprd_unselected);
            eExposureTv.setTextColor(Color.WHITE);
        }

    }
}
