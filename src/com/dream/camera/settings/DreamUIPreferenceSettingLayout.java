package com.dream.camera.settings;

import com.android.camera2.R;
import com.android.camera.util.CameraUtil;
import com.android.internal.app.ToolbarActionBar;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.SlidePanelManager;
import android.view.MotionEvent;

import android.app.Activity;
import com.android.camera.CameraActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.animation.ObjectAnimator;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;

public class DreamUIPreferenceSettingLayout extends LinearLayout implements
        ResetListener {

    public interface SettingUIListener{
        public void onSettingUIHide();
    }

    public DreamUIPreferenceSettingLayout(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public DreamUIPreferenceSettingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public DreamUIPreferenceSettingLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
    }

    public DreamUIPreferenceSettingLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // TODO Auto-generated constructor stub
    }


    CameraActivity mActivity;
    DreamUIPreferenceSettingFragment mCurrentFragment;
    ImageView mReturnButton;
    FrameLayout mSettingFragContainer;
    SettingUIListener mSettingUIListener;
    private int mDeviceWidth;
    private static final Log.Tag TAG = new Log.Tag("CAM_DreamUIPreferenceSettingLayout");
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mActivity = (CameraActivity) getContext();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        mDeviceWidth = metrics.widthPixels;
        setTranslationX(mDeviceWidth);
        mReturnButton = (ImageView) findViewById(R.id.return_image);
        mSettingFragContainer = (FrameLayout) findViewById(R.id.setting_frament_container);
        mReturnButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                hideSettingLayout();
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSettingFragContainer != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mSettingFragContainer.getLayoutParams();
            int slideHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.slide_panel_height);
            lp.bottomMargin = CameraUtil.getNormalNavigationBarHeight() + slideHeight;
            mSettingFragContainer.setLayoutParams(lp);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    /*SPRD:fix bug607898 fix setting ui when back from home/secure camera, last time pause camera by pressing home @{ */
    public boolean isNeedUpdateModule() {
        return mCurrentFragment != null && mCurrentFragment.getDataSetting() != null 
                    && !mCurrentFragment.getDataSetting().equals(DataModuleManager.getInstance(mActivity).getCurrentDataSetting());
    }

    public void updateModule(SettingUIListener listener) {
        mCurrentFragment.releaseResource();
        mCurrentFragment = new DreamUIPreferenceSettingFragment();
        mActivity.getFragmentManager().beginTransaction()
                .replace(R.id.setting_frament_container, mCurrentFragment)
                .commitAllowingStateLoss();//SPRD:fix bug 611031
        mSettingUIListener = listener;
    }
    /* @} */

    public void changeModule(SettingUIListener listener){

        if(mCurrentFragment != null){
            mCurrentFragment.releaseResource();
        }

        mCurrentFragment = new DreamUIPreferenceSettingFragment();
        mActivity.getFragmentManager().beginTransaction()
                .replace(R.id.setting_frament_container, mCurrentFragment)
                .commitAllowingStateLoss();//SPRD:fix bug 611031
        mSettingUIListener = listener;
    }

    public void changeModule(){

        if(mCurrentFragment != null){
            mCurrentFragment.releaseResource();
        }

        mCurrentFragment = new DreamUIPreferenceSettingFragment();
        mActivity.getFragmentManager().beginTransaction()
                .replace(R.id.setting_frament_container, mCurrentFragment)
                .commitAllowingStateLoss();//SPRD:fix bug 611031
    }

    public void changeVisibilty(int visiblty){
        this.setVisibility(visiblty);
        if(visiblty == GONE) {
            if (mSettingUIListener != null) {
                mSettingUIListener.onSettingUIHide();
            } else {
                mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, false);
            }
        }
    }

    @Override
    public void onSettingReset() {
        if(mCurrentFragment != null){
            mCurrentFragment.resetSettings();
        }
        hideSettingLayoutInstant();
    }

    private void hideSettingLayoutInstant(){
        setTranslationX(mDeviceWidth);
        mSettingLayoutShow = false;
        setHideSettingUI();
    }
    public boolean onBackPressed() {
        if(this.getVisibility() == View.VISIBLE){
            hideSettingLayout();
            return true;
        }
        return false;
    }
    public void dialogDismiss(String key){
        if(mCurrentFragment != null){
            mCurrentFragment.dialogDismiss(key);
        }
    }
    //Sprd:fix bug841813
    public void dismissDialogIfNecessary(){
        if(mCurrentFragment != null){
            mCurrentFragment.dismissDialogIfNecessary();
        }
    }

    private int mLastX;
    private int mLastY;
    private int mLastXIntercept;
    private int mLastYIntercept;
    private int mSaveDeltaX;
    private ValueAnimator mSettingLayoutAnimator;
    private boolean mSettingLayoutShow = false;
    private final static int SLIDE_THRESHOLD = 4;
    private final static int ANIMATE_DURATION = 200;
    private final static int SLIDE_XY_THRESHOLD = 3;

    public boolean fastRejectWhenTouch() {
        if (mSettingLayoutAnimator != null && mSettingLayoutAnimator.isRunning()) {
            return true;
        }
        if (!mActivity.getCameraAppUI().getSwipeEnable()){
            return true;
        }

        //Sprd fix bug819907
        if (mActivity.getCameraAppUI().isShutterClicked()){
            Log.i(TAG, "shutterbutton has been clicked, not show settingui");
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (fastRejectWhenTouch()) // split to another 1 method for CCN optimization , from 15 to 12+5
            return false;

        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaX = x - mLastX;
                int deltaY = y - mLastY;
                mSaveDeltaX = deltaX;
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    int translationX = (int) getTranslationX() + deltaX;
                    if (translationX >= 0 && translationX <= mDeviceWidth) {
                        setShowSettingUI();
                        setTranslationX(translationX);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                //int mUpDownDeltaX = x - mDownX;
                if (!mSettingLayoutShow) {
                    if (/*-mUpDownDeltaX >= mDeviceWidth / SLIDE_THRESHOLD &&*/ mSaveDeltaX <= 0) {
                        showSettingLayout();
                    } else {
                        hideSettingLayout();
                    }
                } else {
                    if (/*mUpDownDeltaX >= mDeviceWidth / SLIDE_THRESHOLD &&*/ mSaveDeltaX >= 0) {
                        hideSettingLayout();
                    } else {
                        showSettingLayout();
                    }
                }
                mSaveDeltaX = 0;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                hideSettingLayout();
                break;
            }
            default:
                break;

        }
        mLastX = x;
        mLastY = y;
        return true;
    }

    private void setShowSettingUI() {
        //Sprd:fix bug795960
        mSettingLayoutShow = true;
        setVisibility(View.VISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(SlidePanelManager.SETTINGS, false);
        mActivity.getCameraAppUI().setSettingLayoutBackground(true);
        mActivity.getCameraAppUI().updatePreviewUI(View.GONE, false);
    }

    private void setHideSettingUI() {
        setVisibility(GONE);
        mActivity.getCameraAppUI().setSettingLayoutBackground(false);
        mActivity.getCameraAppUI().updatePreviewUI(View.VISIBLE, false);
        mActivity.getCameraAppUI().getDreamInterface().updateSlidePanel();
    }

    public void showSettingLayout() {
        Log.i(TAG, "showSettingLayout");
        setShowSettingUI();
        mSettingLayoutAnimator = ObjectAnimator.ofFloat(this, "translationX", 0);
        mSettingLayoutAnimator.setDuration(ANIMATE_DURATION);
        mSettingLayoutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSettingLayoutAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
            }
        });
        mSettingLayoutAnimator.start();
    }

    public void hideSettingLayout() {
        Log.i(TAG, "hideSettingLayout");
        mSettingLayoutAnimator = ObjectAnimator.ofFloat(this, "translationX", mDeviceWidth);
        mSettingLayoutAnimator.setDuration(ANIMATE_DURATION);
        mSettingLayoutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSettingLayoutAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                mSettingLayoutShow = false;
                setHideSettingUI();
                mSettingLayoutAnimator = null;
            }
        });
        mSettingLayoutAnimator.start();
    }

    public boolean isShown() {
        return mSettingLayoutShow;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (mSettingLayoutShow) {
                    intercepted = false;
                } else {
                    intercepted = true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int deltaX = x - mLastXIntercept;
                int deltaY = y - mLastYIntercept;
                int delta = Math.abs(deltaX) - Math.abs(deltaY);
                if (deltaX > 0 && delta > SLIDE_XY_THRESHOLD) {
                    intercepted = true;
                } else {
                    intercepted = false;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                break;
            }
            default:
                break;
        }
        mLastXIntercept = x;
        mLastYIntercept = y;
        mLastX = x;
        mLastY = y;
        return intercepted;
    }
    
    //Sprd:fix bug841813
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mSettingLayoutAnimator != null && mSettingLayoutAnimator.isRunning()){
            Log.i(TAG,"mSettingLayoutAnimator.isRunning()");
            return true;
        }
        if (mActivity.getCameraAppUI().isShutterClicked()) {
            Log.i(TAG, "camera is recording or capturing, not dispatchTouchEvent");
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    //SPRD: Fix bug 944410
    public void updateLayoutByAeLock(boolean enabled) {
        mCurrentFragment.updateSettingAeLock(Keys.KEY_CAMERA_FACE_DATECT,enabled);
    }
}
