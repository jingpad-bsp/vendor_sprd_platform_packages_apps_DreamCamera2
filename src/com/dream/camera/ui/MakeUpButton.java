package com.dream.camera.ui;

import com.android.camera.app.AppController;
import com.android.camera.debug.Log;

import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;  
import com.android.camera2.R;
import android.graphics.Color;

public class MakeUpButton extends LinearLayout {
    private static final Log.Tag TAG = new Log.Tag("MakeupButton");

    private ImageView mBt;
    private TextView mBtShow;
    private int mBtKey;
    private Context mContext;

    public MakeUpButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.make_up_button, this);
    }

    @Override
    public void onFinishInflate() {
        mBt = (ImageView) findViewById(R.id.make_up_icon);
        mBtShow = (TextView) findViewById(R.id.make_up_tv);
        Log.e(TAG,"MakeupButton onFinishInflate","mBt is "+mBt+", mBtShow is "+mBtShow);
    }

    public void setButtonKey (int makeupKey) {
        mBtKey = makeupKey;
        switch (mBtKey){
            case MakeUpKey.SKIN_SMOOTH:
                mBtShow.setText(R.string.extend_panel_skin_smooth);
                break;
            case MakeUpKey.REMOVE_BLEMISH:
                mBtShow.setText(R.string.extend_panel_remove_blemish);
                break;
            case MakeUpKey.SKIN_BRIGHT:
                mBtShow.setText(R.string.extend_panel_skin_bright);
                break;
            case MakeUpKey.SKIN_COLOR:
                mBtShow.setText(R.string.extend_panel_skin_colour);
                break;
            case MakeUpKey.SKIN_COLOR_WHITE:
                mBtShow.setText(R.string.extend_panel_skincolor_white);
                break;
            case MakeUpKey.SKIN_COLOR_ROSY:
                mBtShow.setText(R.string.extend_panel_skincolor_rosy);
                break;
            case MakeUpKey.SKIN_COLOR_WHEAT:
                mBtShow.setText(R.string.extend_panel_skincolor_wheat);
                break;
            case MakeUpKey.LARGE_EYES:
                mBtShow.setText(R.string.extend_panel_enlarge_eyes);
                break;
            case MakeUpKey.SLIM_FACE:
                mBtShow.setText(R.string.extend_panel_slim_face);
                break;
            case MakeUpKey.LIPS_COLOR:
                mBtShow.setText(R.string.extend_panel_red_lips);
                break;
            case MakeUpKey.LIPS_COLOR_CRIMSON:
                mBtShow.setText(R.string.extend_panel_lipscolor_crimson);
                break;
            case MakeUpKey.LIPS_COLOR_PINK:
                mBtShow.setText(R.string.extend_panel_lipscolor_pink);
                break;
            case MakeUpKey.LIPS_COLOR_FUCHSIA:
                mBtShow.setText(R.string.extend_panel_lipscolor_fuchsia);
                break;
            default:
                Log.d(TAG,"this makeup button has no key");
                break;
        }
    }

    public void setSelect (boolean selected) {
        switch (mBtKey){
            case MakeUpKey.SKIN_SMOOTH:
                setSkinSmoothSelected(selected);
                break;
            case MakeUpKey.REMOVE_BLEMISH:
                setRemoveBlemishSelected(selected);
                break;
            case MakeUpKey.SKIN_BRIGHT:
                setSkinBrightSelected(selected);
                break;
            case MakeUpKey.SKIN_COLOR:
                setSkinColorSelected(selected);
                break;
            case MakeUpKey.SKIN_COLOR_WHITE:
                setSkinColorWhiteSelected(selected);
                break;
            case MakeUpKey.SKIN_COLOR_ROSY:
                setSkinColorRosySelected(selected);
                break;
            case MakeUpKey.SKIN_COLOR_WHEAT:
                setSkinColorWheatSelected(selected);
                break;
            case MakeUpKey.LARGE_EYES:
                setLargeEyesSelected(selected);
                break;
            case MakeUpKey.SLIM_FACE:
                setSlimFaceSelected(selected);
                break;
            case MakeUpKey.LIPS_COLOR:
                setLipsColorSelected(selected);
                break;
            case MakeUpKey.LIPS_COLOR_CRIMSON:
                setLipsColorCrimsonSelected(selected);
                break;
            case MakeUpKey.LIPS_COLOR_PINK:
                setLipsColorPinkSelected(selected);
                break;
            case MakeUpKey.LIPS_COLOR_FUCHSIA:
                setLipsColorFuchsiaSelected(selected);
                break;
            default:
                Log.d(TAG,"no makeup key do nothing");
                break;
        }
    }

    private void setSkinSmoothSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_smooth_sprd_selected
                : R.drawable.ic_operate_skin_smooth_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setRemoveBlemishSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_remove_blemish_sprd_selected
                : R.drawable.ic_operate_remove_blemish_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSkinBrightSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_bright_sprd_selected
                : R.drawable.ic_operate_skin_bright_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSkinColorSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_color_sprd_selected
                : R.drawable.ic_operate_skin_color_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSkinColorWhiteSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_color_white_sprd_selected
                : R.drawable.ic_operate_skin_color_white_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSkinColorRosySelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_color_rosy_sprd_selected
                : R.drawable.ic_operate_skin_color_rosy_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSkinColorWheatSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_skin_color_wheat_sprd_selected
                : R.drawable.ic_operate_skin_color_wheat_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setLargeEyesSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_large_eyes_sprd_selected
                : R.drawable.ic_operate_large_eyes_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setSlimFaceSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_slim_face_sprd_selected
                : R.drawable.ic_operate_slim_face_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setLipsColorSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_lip_color_sprd_selected
                : R.drawable.ic_operate_lip_color_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setLipsColorCrimsonSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_lip_color_crimson_sprd_selected
                : R.drawable.ic_operate_lip_color_crimson_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setLipsColorPinkSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_lip_color_pink_sprd_selected
                : R.drawable.ic_operate_lip_color_pink_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }

    private void setLipsColorFuchsiaSelected(boolean selected) {
        mBt.setImageResource(selected ? R.drawable.ic_operate_lip_color_fuchsia_sprd_selected
                : R.drawable.ic_operate_lip_color_fuchsia_sprd_unselected);
        mBtShow.setTextColor(selected ? mContext.getResources()
                .getColor(R.color.dream_yellow) : Color.WHITE);
    }
}