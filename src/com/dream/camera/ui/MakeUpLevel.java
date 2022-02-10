package com.dream.camera.ui;

import com.android.camera.debug.Log;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.android.camera2.R;
import com.android.camera.settings.Keys;
import com.dream.camera.MakeupController;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleManager;

public class MakeUpLevel extends LinearLayout implements OnSeekBarChangeListener {
    private static final Log.Tag TAG = new Log.Tag("MakeUpLevel");
    private Context mContext;
    private SeekBar mSeekBar;
    private TextView mCurrentLevel;
    private TextView mMaxLevel;
    private TextView mMinLevel;
    private int mKey;
    private MakeupLevelListener mListener;

    public interface MakeupLevelListener {
        public void onBeautyLevelChanged(int key, int value);
        public void onBeautyLevelWillChanged(int key, int value);
    }

    public MakeUpLevel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.make_up_level, this);
    }

    @Override
    public void onFinishInflate() {
        mSeekBar = (SeekBar) findViewById(R.id.makeup_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mCurrentLevel = (TextView) findViewById(R.id.current_makeup_level);
        mMaxLevel = (TextView) findViewById(R.id.max_makeup_level);
        mMinLevel = (TextView) findViewById(R.id.min_makeup_level);
        Log.e(TAG,"MakeupLevel onFinishInflate","mSeekBar is "+mSeekBar+", mCurrentLevel is "+mCurrentLevel+", mMaxLevel is "+mMaxLevel+", mMinLevel is "+mMinLevel);
    }

    public void setKey(int makeupKey) {
        mKey = makeupKey;
    }

    public void setListener(MakeupLevelListener l) {
        mListener = l;
    }
    public void setMaxLevel(int level) {
        if (mKey == MakeUpKey.REMOVE_BLEMISH_LEVEL) {
            mMaxLevel.setText(R.string.remove_blemish_on);
            mSeekBar.setMax(level);
        } else {
            mMaxLevel.setText("" + level);
        }
    }

    public void setMinLevel(int level) {
        if (mKey == MakeUpKey.REMOVE_BLEMISH_LEVEL) {
            mMinLevel.setText(R.string.remove_blemish_off);
        } else {
            mMinLevel.setText("" + level);
        }
    }

    public void setLevel(int value) {
        int proValue = value/2;
        if (mKey == MakeUpKey.REMOVE_BLEMISH_LEVEL) {
            if (value == 1) {
                mCurrentLevel.setText(R.string.remove_blemish_on);
                mSeekBar.setProgress(value);
            } else {
                mCurrentLevel.setText(R.string.remove_blemish_off);
                mSeekBar.setProgress(value);
            }
        } else {
            mCurrentLevel.setText(""+proValue);
            mSeekBar.setProgress(proValue);
        }
    }

    public void setCurrentLevel(int value) {
        if (mKey == MakeUpKey.REMOVE_BLEMISH_LEVEL) {
            if (value == 1) {
                mCurrentLevel.setText(R.string.remove_blemish_on);
            } else {
                mCurrentLevel.setText(R.string.remove_blemish_off);
            }
        } else {
            mCurrentLevel.setText(""+value);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        Log.d(TAG, "onProgressChanged " + progress);
        int progressPra = 0;
        if (progress != 0) {
            progressPra = 2 * progress;
        } else {
            progressPra = progress;
        }
        if (mKey == MakeUpKey.REMOVE_BLEMISH_LEVEL) {
            progressPra = progress;
        }
        switch (mKey){
            case MakeUpKey.SKIN_SMOOTH_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_SKIN_SMOOTH_LEVEL,progressPra);
                break;
            case MakeUpKey.REMOVE_BLEMISH_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_REMOVE_BLEMISH_LEVEL,progressPra);
                break;
            case MakeUpKey.SKIN_BRIGHT_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_SKIN_BRIGHT_LEVEL,progressPra);
                break;
            case MakeUpKey.SKIN_COLOR_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_SKIN_COLOR_LEVEL,progressPra);
                break;
            case MakeUpKey.LARGE_EYES_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_ENLARGE_EYES_LEVEL,progressPra);
                break;
            case MakeUpKey.SLIM_FACE_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_SLIM_FACE_LEVEL,progressPra);
                break;
            case MakeUpKey.LIPS_COLOR_LEVEL:
                setDateToFile(Keys.KEY_MAKEUP_LIPS_COLOR_LEVEL,progressPra);
                break;
            default:
                Log.d(TAG,"this makeup level has no key");
                break;
        }
        setCurrentLevel(progress);
        mListener.onBeautyLevelWillChanged(mKey, progressPra);
    }

    @Override
    public void onStartTrackingTouch(SeekBar SeekBarId) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar SeekBarId) {
        int changeValue = SeekBarId.getProgress();
        mListener.onBeautyLevelChanged(mKey, changeValue);
    }

    public void setDateToFile(String key, int value) {
        DataModuleManager
        .getInstance(mContext)
        .getCurrentDataModule()
        .set(DataConfig.SettingStoragePosition.positionList[3], key,
                "" + value);
    }

    public void setEnable(boolean enable){
        mSeekBar.setEnabled(enable);
    }
}