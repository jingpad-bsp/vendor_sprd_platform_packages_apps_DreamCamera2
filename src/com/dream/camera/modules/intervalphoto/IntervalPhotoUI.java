package com.dream.camera.modules.intervalphoto;

import java.util.ArrayList;
import java.util.HashMap;

import android.widget.LinearLayout;
import com.android.camera.CameraActivity;
import com.android.camera.PhotoController;
import com.android.camera.debug.Log;
import com.android.camera.PhotoUI;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.camera.PhotoModule;
import com.android.camera.settings.Keys;
import com.android.camera.ui.RotateTextView;
import com.android.camera.widget.ModeOptionsOverlay;
import com.android.camera2.R;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.ui.TopPanelListener;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.android.camera.MultiToggleImageButton;
import com.android.camera.util.CameraUtil;
import com.dream.camera.MakeupController;

public class IntervalPhotoUI extends PhotoUI implements
        DreamFreezeFrameDisplayView.DreamProxyFreezeFrameClick, DreamSettingChangeListener {

    private static final Log.Tag TAG = new Log.Tag("IntvalPhUI");

    //private ImageButton mSettingsButton;
    private View topPanel;
    private DreamFreezeFrameDisplayView mFreezeFrame;

    public IntervalPhotoUI(CameraActivity activity, PhotoController controller,
            View parent) {
        super(activity, controller, parent);

        if (mFreezeFrame == null) {
            mActivity.getLayoutInflater().inflate(
                    R.layout.interval_freeze_frame_display, getModuleRoot(), true);

            mFreezeFrame = (DreamFreezeFrameDisplayView) getModuleRoot()
                    .findViewById(R.id.preview_camera_freeze_frame_display);
            mFreezeFrame.setListener(this);
            mFreezeFrame.setCurIntervalList(((IntervalPhotoModule)mController).getDreamIntervalDisplayList()); //bugfix 1014523
        }
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        if (mBasicModule.isBeautyCanBeUsed()) {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.intervalphoto_top_makeup_panel,
                        topPanelParent);
            }
        } else {
            if (topPanel == null) {
                LayoutInflater lf = LayoutInflater.from(mActivity);
                topPanel = lf.inflate(R.layout.intervalphoto_top_panel,
                        topPanelParent);
            }
        }

        mActivity.getButtonManager().load(topPanel);

//        mSettingsButton = (ImageButton) topPanel
//                .findViewById(R.id.settings_button_dream);
//
//        bindSettingsButton(mSettingsButton);

        //bindCountDownButton();
        if (mBasicModule.isBeautyCanBeUsed()) {
            bindMakeUpDisplayButton();
        }

        bindFlashButton();
        bindCameraButton();
    }

    @Override
    public void updateTopPanel() {
        ViewGroup topPanelParent = (ViewGroup) mRootView
                .findViewById(R.id.top_panel_parent);

        /**
         * make top panel display normal 
         */
        if (mBasicModule.isBeautyCanBeUsed()) {
            View gap1 = topPanelParent.findViewById(R.id.interval_gap1);
            View gap2 = topPanelParent.findViewById(R.id.interval_gap2);
            MultiToggleImageButton flash = (MultiToggleImageButton)topPanelParent.findViewById(R.id.flash_toggle_button_dream);
            if (((View)flash.getParent()).getVisibility() == View.GONE) {
                gap1.setVisibility(View.GONE);
                gap2.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void bindCountDownButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(
                ButtonManagerDream.BUTTON_COUNTDOWN_DREAM, null,
                R.array.dream_countdown_duration_icons_without_off);
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {
        LayoutInflater lf = LayoutInflater.from(mActivity);
        View extendPanel = null;
        if (mBasicModule.isBeautyCanBeUsed()) {
            // mFreezeFrame = extendPanelParent;
            if (CameraUtil.getCurrentBeautyVersion() == CameraUtil.ENABLE_ARC_BEAUTY ||
                    (CameraUtil.getCurrentBeautyVersion() == CameraUtil.ENABLE_SPRD_BEAUTY && !CameraUtil.isCameraBeautyAllFeatureEnabled())) {
                extendPanel = lf.inflate(
                        R.layout.intervalphoto_extend_panel, extendPanelParent);
            } else if (CameraUtil.getCurrentBeautyVersion() == CameraUtil.ENABLE_SPRD_BEAUTY) {
                extendPanel = lf.inflate(
                        R.layout.sprd_intervalphoto_extend_panel, extendPanelParent);
            }
            mMakeupController = new MakeupController(extendPanelParent,
                    mController,mActivity);
        } else {
            extendPanel = lf.inflate(
                    R.layout.interval_photo_extend_panel, extendPanelParent);
        }
        if(CameraUtil.isNavigationEnable()){
            View fourList = mActivity.findViewById(R.id.four_list);
            LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
            layoutParam.setMargins(0, 0, 0, mActivity.getResources()
                    .getDimensionPixelSize(R.dimen.intervalphoto_bottom_spacing));
            fourList.setLayoutParams(layoutParam);
        }
    }

    @Override
    public void updateBottomPanel() {
        super.updateBottomPanel();
    }

    @Override
    public void updateSlidePanel() {
        super.updateSlidePanel();
    }

    public void showIntevalFreezeFrame() {
        mFreezeFrame.init();
        saved = false;
    }

    private boolean saved = false;

    public int getGiveupNum(){
        if (mFreezeFrame != null) {
            Log.i(TAG,"zxt giveupnum="+mFreezeFrame.getGiveupNum());
            return mFreezeFrame.getGiveupNum();
        }
        return 0;
    }

    private void syncThumbnail(){
        Uri uri = mFreezeFrame.getLastSaveUri();
        mActivity.SyncThumbnailForIntervalPhoto(((IntervalPhotoModule) mController).getBitmapForThum(uri),uri);
        mFreezeFrame.clearSaveList();
    }
    public void dreamProxyDoneClicked(boolean save) {
        saved = save;

        if (saved && mFreezeFrame != null && mFreezeFrame.isSaveAll()) {
            syncThumbnail();
            ((IntervalPhotoModule) mController).clear(true);
        }else {
            ((IntervalPhotoModule) mController).clear(false);
        }
        Log.d(TAG, "dreamProxyDoneClicked , set swipeEnabled = true");
        mActivity.getCameraAppUI().setSwipeEnabled(true);
        if (mFreezeFrame != null) { // Bug 1159190 (FORWARD_NULL)
            mFreezeFrame.reset();
            mFreezeFrame.setVisibility(View.GONE);
        }
        //((IntervalPhotoModule) mController).clear();
        // ui check 72
        updateIntervalFreezeFrameUI(View.VISIBLE);
        // ui check 129,dream camera test 121
        // interval self own
        if (mMakeupController != null
                && mBasicModule.isBeautyCanBeUsed()
                && DataModuleManager.getInstance(mActivity).getCurrentDataModule()
                        .getBoolean(Keys.KEY_MAKE_UP_DISPLAY)) {
            mMakeupController.resume(DataModuleManager.getInstance(
                    mActivity.getAndroidContext()).getCurrentDataModule().getBoolean(Keys.KEY_CAMERA_BEAUTY_ENTERED));
        }
    }

    @Override
    public void dreamProxyFinishDeleted(Uri uri) {
        Log.d(TAG, "dreamProxyFinishDeleted " + uri);
        mActivity.removeDataByUri(uri);
        if (saved == true && mFreezeFrame.isFinishDeleted()) {
            syncThumbnail();
            ((IntervalPhotoModule) mController).clear(true);
        }
    }

    private int fourListState = 0;

    public void showFourList(boolean show) {
        View bottomBar = mActivity.findViewById(R.id.bottom_bar);

        View fourList = mActivity.findViewById(R.id.four_list);

        if (bottomBar != null && fourList != null) {
            if (show) {
                bottomBar.setVisibility(View.GONE);
                fourList.setVisibility(View.VISIBLE);
                ((IntervalPhotoModule) mController).setCanShutter(false);
            } else {
                bottomBar.setVisibility(View.VISIBLE);
                fourList.setVisibility(View.GONE);
                ((IntervalPhotoModule) mController).setCanShutter(true);
            }
        }

        if (check4Preview()) {
            previewOne = (ImageView) mActivity.findViewById(R.id.preview_1);
            previewTwo = (ImageView) mActivity.findViewById(R.id.preview_2);
            previewThree = (ImageView) mActivity.findViewById(R.id.preview_3);
            previewFour = (ImageView) mActivity.findViewById(R.id.preview_4);
            if (check4Preview())
                return;
        }

        previewOne.setImageBitmap(null);
        previewTwo.setImageBitmap(null);
        previewThree.setImageBitmap(null);
        previewFour.setImageBitmap(null);

        fourListState = 0;

    }

    private ImageView previewOne;
    private ImageView previewTwo;
    private ImageView previewThree;
    private ImageView previewFour;

    private boolean check4Preview() {
        return(previewOne == null || previewTwo == null || previewThree == null || previewFour == null);
    }

    public void onThumbnail(final Bitmap thumbmail) {
        if (check4Preview()) {// Bug 839474: CCN optimization
            // split to another 1 method for CCN optimization , from 15 to 9+4
            previewOne = (ImageView) mActivity.findViewById(R.id.preview_1);
            previewTwo = (ImageView) mActivity.findViewById(R.id.preview_2);
            previewThree = (ImageView) mActivity.findViewById(R.id.preview_3);
            previewFour = (ImageView) mActivity.findViewById(R.id.preview_4);
            if (check4Preview())
                return;
        }

        switch (fourListState) {
        case 0:
            previewOne.setImageBitmap(thumbmail);
            break;
        case 1:
            previewTwo.setImageBitmap(thumbmail);
            break;
        case 2:
            previewThree.setImageBitmap(thumbmail);
            break;
        case 3:
            previewFour.setImageBitmap(thumbmail);
            if (mActivity.getMainHandler() != null)
                mActivity.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (fourListState != 0) {//SPRD:fix bug600908 not on pause
                            showFourList(false);
                            ((IntervalPhotoModule) mController).showIntervalFreezeFrame();
                        }
                    }
                }, 1000);
            break;
        default:
            break;
        }
        fourListState++;
    }

    /*
     * ui check 8, 71
     * 
     * @{
     */
    public void updateStartCountDownUI() {
        // top panel hidden
        mActivity.getCameraAppUI().updateTopPanelUI(View.GONE);
        // bottom bar left and right hidden
        mActivity.getCameraAppUI().setBottomBarLeftAndRightUI(View.GONE);
        // shutter button hidden
        mActivity.getCameraAppUI().updateShutterButtonUI(View.GONE);
        // slide panel hidden
        mActivity.getCameraAppUI().updateSlidePanelUI(View.GONE);
        mActivity.getCameraAppUI().hideAdjustFlashPanel();

        // ui check 129
        // interval self own
        //mMakeupController.pauseMakeupControllerView();
        if (mMakeupController != null && mBasicModule.isBeautyCanBeUsed()) {
            mMakeupController.pause();
        }
    }

    /* @} */

    /**
     * UI CHECK 72
     */
    public void updateIntervalFreezeFrameUI(int visibility) {
        mActivity.getCameraAppUI().updatePreviewUI(visibility, true);
    }

    @Override
    public void onResume(){
        if(isFreezeFrameShow()){
            updateStartCountDownUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        DataModuleManager.getInstance(mActivity).getCurrentDataModule()
                .removeListener(mActivity,this);
        //SPRD:Bugfix 1048680 
        if (mFreezeFrame != null)
            mFreezeFrame.reset();
        /*SPRD:fix bug 1108525 @{ */
        if (isFreezeFrameShow())
            mFreezeFrame.setVisibility(View.GONE);
        /* @} */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFreezeFrame != null) {
            /**SPRD:Bug1006432 make mFreezeFrame gone @{*/
            mFreezeFrame.reset();
            mFreezeFrame.setVisibility(View.GONE);
            mFreezeFrame.setListener(null);
        }
    }

    @Override
    public void onDreamSettingChangeListener(HashMap<String, String> keys) {
        for (String key : keys.keySet()) {
            switch (key) {
            case Keys.KEY_CAMERA_BEAUTY_ENTERED:
                mBasicModule.updateMakeLevel();
                break;

            default:
                break;
            }
        }

    }

    @Override
    public boolean onBackPressed(){
        return super.onBackPressed();
    }

    public boolean isFreezeFrameShow() {
        if (mFreezeFrame != null && mFreezeFrame.getVisibility() == View.VISIBLE) {
            return true;
        }

        return false;
    }

    public void prepareFreezeFrame(int index, Uri uri) {
        mFreezeFrame.prepare(index, uri);
    }

    /*SPRD:fix bug625571 change interval decode for OOM @{ */
    @Override
    protected void setPictureInfo(int width, int height, int orientation) {
        mFreezeFrame.setPictureInfo(width, height,orientation);
    }
    /* @} */
}
