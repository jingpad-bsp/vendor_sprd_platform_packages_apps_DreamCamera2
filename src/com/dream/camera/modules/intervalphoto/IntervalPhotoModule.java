
package com.dream.camera.modules.intervalphoto;

import java.util.ArrayList;
import java.util.HashMap;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoUI;
import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.dream.camera.DreamModule;
import com.android.camera.PhotoModule;

import android.net.Uri;
import android.view.View;

import com.android.camera.settings.Keys;
import com.android.camera.PhotoUI;
import com.dream.camera.settings.DataModuleManager;

import android.graphics.Bitmap;
public class IntervalPhotoModule extends PhotoModule {

    private static final Log.Tag TAG = new Log.Tag("IntervalPhotoModule");

    public static int INTERVAL_NUM = 4;
    public static int mNum = INTERVAL_NUM;
    private IntervalPhotoUI ui;
    protected ArrayList<Uri> mDreamIntervalDisplayList = new ArrayList<Uri>();
    private Bitmap mThumbBitmap = null;
    protected HashMap<Uri,Bitmap> mBitmapRecord = new HashMap<>();

    public IntervalPhotoModule(AppController app) {
        super(app);
    }

    @Override
    public PhotoUI createUI(CameraActivity activity) {
        ui = new IntervalPhotoUI(activity, this, activity.getModuleLayoutRoot());
        return ui;
    }

    @Override
    public boolean isSupportTouchAFAE() {
        return true;
    }

    @Override
    public boolean isSupportManualMetering() {
        return false;
    }

    @Override
    public boolean shutterAgain() {
        Log.d(TAG, "shutterAgain mNum=" + mNum);
        if (mNum > 0 && mNum < INTERVAL_NUM) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isInShutter() {
        Log.d(TAG, "isInShutter mNum=" + mNum);
        if (mNum >= 0 && mNum < INTERVAL_NUM) {
            if (mNum == 0) {
                mNum = INTERVAL_NUM;
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void onShutterButtonClick() {
        /**SPRD:Bug686758/Bug1000853 prevent the bottom panel can be set to gone @{*/
        if (shutterButtonCanNotClick(true)) {
            Log.i(TAG, "shutterbutton can not click");
            return;
        }
        /** @}*/
        canShutter = false;
        mBackPressed = false; // SPRD: fix Bug 707864
        super.onShutterButtonClick();
        if (mNum == INTERVAL_NUM) {
            ui.showFourList(true);
        }
        mNum--;
        setCaptureCount(0);
    }

    public Uri getUri(int i) {
        return mDreamIntervalDisplayList.get(i);
    }

    public int getSize() {
        return mDreamIntervalDisplayList.size();
    }

    public void clear() {
        mDreamIntervalDisplayList.clear();
        mBitmapRecord.clear();
    }
    public void clear(boolean needClearBitmapRecord) {
        mDreamIntervalDisplayList.clear();
        if (needClearBitmapRecord){
            mBitmapRecord.clear();
            mThumbBitmap = null;
        }
    }

    @Override
    protected void mediaSaved(final Uri uri) {
        if (isInShutter()) {
            mDreamIntervalDisplayList.add(uri);
            mBitmapRecord.put(uri,mThumbBitmap);
            final int index = mDreamIntervalDisplayList.size() - 1;
            mActivity.getDreamHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (ui != null) {
                        ui.prepareFreezeFrame(index, uri);
                    }
                }
            });
        }

        if (shutterAgain()) {
            onShutterButtonClick();
        }
    }
    //bugfix 1014523
    public ArrayList<Uri> getDreamIntervalDisplayList(){
        return mDreamIntervalDisplayList;
    }
    public void showIntervalFreezeFrame() {
        if (mDreamIntervalDisplayList.size() > 0) {
            mActivity.getCameraAppUI().setSwipeEnabled(false); // SPRD: FixBug 828228
            ui.showIntevalFreezeFrame();
            // ui check 72
            ui.updateIntervalFreezeFrameUI(View.GONE);
            mUI.enablePreviewOverlayHint(true);//SPRD:Fix bug927249
        }
    }
    /* dream test 50 @{ */
    protected void doSomethingWhenonPictureTaken(byte[] jpeg) {
    }
    /* @} */
    /* dream test 84 @{ */
    protected void dosomethingWhenPause() {
        mNum = INTERVAL_NUM;

        if (!ui.isFreezeFrameShow())
            clear();

        ui.updateIntervalFreezeFrameUI(View.VISIBLE);
        ui.showFourList(false);
        updateMakeLevel();
    }

    /* @} */
    /* nj dream camera test 84 */
    @Override
    public boolean onBackPressed() {
        if (!mUI.isCountingDown() && mNum + mDreamIntervalDisplayList.size() < INTERVAL_NUM){
            Log.i(TAG, "picture is saving");
            return true;
        }
        if (isInShutter() || mUI.isCountingDown()) {
            mNum = INTERVAL_NUM;
            // SPRD: fix Bug 707864
            mBackPressed = true;

            cancelCountDown();
            if (mDreamIntervalDisplayList.size() > 0) {
                mActivity.getDreamHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.getMainHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                if (ui != null) {
                                    showIntervalFreezeFrame();
                                    ui.showFourList(false);
                                }
                            }
                        });

                    }
                });
            } else {
                ui.updateIntervalFreezeFrameUI(View.VISIBLE);
                ui.showFourList(false);
                updateMakeLevel();
                updateMakeUpDisplay();//SPRD:Fix bug1312655
            }
            return true;
        }
        return super.onBackPressed();
    }
    /* @} */

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        if (mCameraState == PREVIEW_STOPPED) {
            return;
        }
        //Sprd:fix bug941486
        if(null != mActivity && (null != mActivity.getCameraAppUI())
                && null != mActivity.getCameraAppUI().getPreviewArea() && !mActivity.getCameraAppUI().getPreviewArea().contains(x, y)){
            return;
        }
        /* Dream Camera test ui check 20 @{ */
        if (mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_TOUCHING_PHOTOGRAPH)) {
            if (!canShutter()) {//SPRD:fix bug600767
                return;
            }
            onShutterButtonClick();
            /* @} */
        }
    }

    /* SPRD: Fix bug 592600, InterValPhotoModule Thumbnail display abnormal. @{ */
    @Override
    public void updateIntervalThumbnail(final Bitmap indicator){
        mActivity.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mActivity.getCameraAppUI().onThumbnail(indicator);
            }
        });
        mThumbBitmap = indicator;
    }
    /* @} */

    public Bitmap getBitmapForThum(Uri uri){
        return mBitmapRecord.get(uri);
    }

    @Override
    public int getModuleTpye() {
        return DreamModule.INTERVAL_MODULE;
    }

    private boolean canShutter = true;

    @Override
    public boolean canShutter(){
        if(canShutter && !ui.isFreezeFrameShow()){
            return true;
        }

        return false;
    }

    @Override
    public void resume(){
        super.resume();
        canShutter = true;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (ui != null) {
            ui.onDestroy();
        }
    }

    public void setCanShutter(boolean shutter){
        canShutter = shutter;
    }

    public boolean isFreezeFrameDisplayShow() {
        return ui.isFreezeFrameShow();
    }

    @Override
    public void updateMakeLevel() {

        // in top panel
        if (mDataModuleCurrent == null)
            return;
        if (isBeautyCanBeUsed()
                && DataModuleManager.getInstance(mActivity)
                        .getCurrentDataModule()
                        .isEnableSettingConfig(Keys.KEY_CAMERA_BEAUTY_ENTERED) && mMakeupController != null) {
            onBeautyValueChanged(mMakeupController.getValue(Keys.KEY_CAMERA_BEAUTY_ENTERED));
        }
        /* @} */
    }

    @Override
    protected void updateMakeUpDisplay() {
        if(!canShutter && mNum > 0 && mNum < INTERVAL_NUM ){
            return;
        }
        super.updateMakeUpDisplay();
    }

    //Sprd:fix bug941988
    public boolean needLoadFilmstripItems(){
        if (ui != null) {
            return ui.getGiveupNum() == 0;
        }
        return true;
    }
    public boolean isNeedUpdateUri() {
        return false;
    }
    public void updateFlashLevelUI() {
        if (mNum < INTERVAL_NUM){
            return;
        }
        super.updateFlashLevelUI();
    }
}
