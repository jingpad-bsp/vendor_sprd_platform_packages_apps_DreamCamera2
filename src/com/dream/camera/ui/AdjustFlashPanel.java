
package com.dream.camera.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera2.R;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;

public class AdjustFlashPanel extends LinearLayout implements
        OnSeekBarChangeListener {

    public AdjustFlashPanel(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AdjustFlashPanel(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AdjustFlashPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdjustFlashPanel(Context context) {
        super(context);
    }

    private TextView mMinValueText;
    private TextView mMaxValueText;
    private TextView mCurrentValueText;
    private SeekBar mAdjustFlashSeekBar;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DataModuleBasic dataModule = DataModuleManager.getInstance(
                    getContext()).getCurrentDataModule();
            if (dataModule.isEnableSettingConfig(Keys.KEY_ADJUST_FLASH_LEVEL)) {
                dataModule
                        .changeSettings(Keys.KEY_ADJUST_FLASH_LEVEL, msg.what);
            }
        }
    };

    @Override
    protected void onFinishInflate() {
        mMinValueText = (TextView) findViewById(R.id.min_flash_level);
        mMaxValueText = (TextView) findViewById(R.id.max_flash_level);
        mCurrentValueText = (TextView) findViewById(R.id.current_flash_level);
        mAdjustFlashSeekBar = (SeekBar) findViewById(R.id.adjust_flash_seekbar);
        mAdjustFlashSeekBar.setOnSeekBarChangeListener(this);
    }

    int[] valueArray;

    public void initialize(int maxValue, int minvalue, int currentValue) {
        valueArray = new int[maxValue - minvalue + 1];
        for (int i = 0; i < valueArray.length; i++) {
            valueArray[i] = minvalue + i;
        }
        mMaxValueText.setText(String.valueOf(maxValue));
        mMinValueText.setText(String.valueOf(minvalue));
        mCurrentValueText.setText(String.valueOf(currentValue));
        mAdjustFlashSeekBar.setMax(maxValue - minvalue);
        mAdjustFlashSeekBar.setProgress(getRightProgress(currentValue));
    }

    public void initialize(int currentValue) {
        mCurrentValueText.setText(String.valueOf(currentValue));
        mAdjustFlashSeekBar.setProgress(getRightProgress(currentValue));
    }

    private int getRightProgress(int currentValue) {
        // TODO Auto-generated method stub
        for (int i = 0; i < valueArray.length; i++) {
            if (valueArray[i] == currentValue) {
                return i;
            }
        }
        return 0;
    }

    private int getRightValue(int progress) {
        return valueArray[progress];
    }

    private volatile int mPreProgress;

    @Override
    public void onProgressChanged(SeekBar arg0, int progress, boolean fromuser) {
        int value = getRightValue(progress);
        mCurrentValueText.setText(String.valueOf(value));
        mHandler.removeMessages(mPreProgress);
        mPreProgress = value;
        mHandler.sendEmptyMessageDelayed(value, 100);

    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    public interface AdjustFlashPanelInterface {
        public void initializeadjustFlashPanel(int maxValue, int minvalue,
                int currentValue);

        public void showAdjustFlashPanel();

        public void hideAdjustFlashPanel();
    }
}
