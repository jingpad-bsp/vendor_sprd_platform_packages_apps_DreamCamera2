
package com.dream.camera.modules.intervalphoto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.Exif;
import com.android.camera.ui.RotateImageView;
import com.android.camera.util.CameraUtil;
import com.android.camera.data.FilmstripItemUtils;

import com.android.camera2.R;
import com.sprd.camera.storagepath.ExternalStorageUtil;
import com.sprd.camera.storagepath.StorageUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DreamFreezeFrameDisplayView extends LinearLayout {
    private static final Log.Tag TAG = new Log.Tag("FFDisplayView ");
    private ArrayList<Uri> mCurIntervalList;
    private ArrayList<Uri> mGiveupList;
    private ArrayList<Uri> mSaveList;
    private boolean mFinishDeleted = false;
    private LinearLayout mLayout;
    private GridLayout mGridLayout;

    private FrameLayout intervalImage1;
    private FrameLayout intervalImage2;
    private FrameLayout intervalImage3;
    private FrameLayout intervalImage4;

    private ImageView mImage1;
    private ImageView mImage2;
    private ImageView mImage3;
    private ImageView mImage4;

    private CheckBox mCheckbox1;
    private CheckBox mCheckbox2;
    private CheckBox mCheckbox3;
    private CheckBox mCheckbox4;

    private RotateImageView mDone;
    private RotateImageView mCancel;
    private ControlOnClickListener mClickListener;
    private AsyncDeleteResource mDeleteTask; // delete resource taks in work thread
    private DreamProxyFreezeFrameClick mListener;

    private CheckedChangedListener mOnCheckedListener;
    private ViewHandler sHandler;

    private ContentResolver mContentResolver;
    private AlertDialog mAlertDialog = null;

    public DreamFreezeFrameDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
    }

    private boolean mSaveAll;
    /**
     * After Delete some files, through this interface to update ui and adapter.
     * 
     * @author SPREADTRUM\liyan.zhu
     */
    public static interface DreamProxyFreezeFrameClick {
        // The method runs in main thread
        public void dreamProxyDoneClicked(boolean save);

        // The method runs in main thread
        public void dreamProxyFinishDeleted(Uri uri);

    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate(); // Bug 1159187 (coverity: Missing call to superclass)
        mLayout = (LinearLayout) findViewById(R.id.preview_camera_freeze_frame_display);
        if (CameraUtil.isNavigationEnable()) {
            mLayout.setPaddingRelative(mLayout.getPaddingStart(), mLayout.getPaddingTop(), mLayout.getPaddingEnd(), CameraUtil.getNormalNavigationBarHeight());
        }
        mGridLayout = (GridLayout) findViewById(R.id.displayFrame);
        intervalImage1 = (FrameLayout) mGridLayout
                .findViewById(R.id.interval_freeze_image1);
        intervalImage2 = (FrameLayout) mGridLayout
                .findViewById(R.id.interval_freeze_image2);
        intervalImage3 = (FrameLayout) mGridLayout
                .findViewById(R.id.interval_freeze_image3);
        intervalImage4 = (FrameLayout) mGridLayout
                .findViewById(R.id.interval_freeze_image4);

        mImage1 = (ImageView) intervalImage1.findViewById(R.id.imageview);
        mImage2 = (ImageView) intervalImage2.findViewById(R.id.imageview);
        mImage3 = (ImageView) intervalImage3.findViewById(R.id.imageview);
        mImage4 = (ImageView) intervalImage4.findViewById(R.id.imageview);
         /*SPRD:fix bug1039661  @{ */
        //mImageMaxWidth = mImage1.getMaxWidth();
        freeze_padding = getResources().getDimension(R.dimen.freeze_padding);
        mImageMaxWidth = (int)Math.ceil(getResources().getDisplayMetrics().widthPixels-4*freeze_padding)/2;
        if(CameraUtil.isNavigationEnable()){
            mImageMaxHeight = (int)Math.ceil(getResources().getDisplayMetrics().heightPixels-4*freeze_padding-
                    getResources().getDimension(R.dimen.navigation_bar_height)-
                    getResources().getDimension(R.dimen.done_cancel_button_height))/2;
        } else {
            mImageMaxHeight = (int) Math.ceil(getResources().getDisplayMetrics().heightPixels-4*freeze_padding-
                    getResources().getDimension(R.dimen.done_cancel_button_height))/2;
        }
        //mImageMaxHeight = mImage1.getMaxHeight();
        /* @} */

        for (int i = 0; i < mGridLayout.getChildCount(); i++){
            ImageView image = (ImageView)((ViewGroup)mGridLayout.getChildAt(i)).getChildAt(0);
            image.setMaxHeight(mImageMaxHeight);
            image.setMaxWidth(mImageMaxWidth);
        }

        mCheckbox1 = (CheckBox) intervalImage1.findViewById(R.id.checkbox);
        mCheckbox2 = (CheckBox) intervalImage2.findViewById(R.id.checkbox);
        mCheckbox3 = (CheckBox) intervalImage3.findViewById(R.id.checkbox);
        mCheckbox4 = (CheckBox) intervalImage4.findViewById(R.id.checkbox);

        mDone = (RotateImageView) findViewById(R.id.btn_freeze_frame_done);
        mCancel = (RotateImageView) findViewById(R.id.btn_feeze_frame_cancel);

        mDeleteTask = new AsyncDeleteResource();
        mClickListener = new ControlOnClickListener();
        mDone.setOnClickListener(mClickListener);
        mCancel.setOnClickListener(mClickListener);
        sHandler = new ViewHandler();

        mOnCheckedListener = new CheckedChangedListener();
        mCheckbox1.setOnCheckedChangeListener(mOnCheckedListener);
        mCheckbox2.setOnCheckedChangeListener(mOnCheckedListener);
        mCheckbox3.setOnCheckedChangeListener(mOnCheckedListener);
        mCheckbox4.setOnCheckedChangeListener(mOnCheckedListener);
    }

    public void init() {
        mDone.setOnClickListener(mClickListener);
        mCancel.setOnClickListener(mClickListener);
        setVisibility(View.VISIBLE);
    }

    private class AsyncDeleteResource implements Runnable {

        /* package */static final int VAL_STATE_DELETE = 11;
        // mutex lock object
        /* package */final Object mMutexLock = new Object();
        // _data column
        private String[] mColumns = new String[] {
                ImageColumns.DATA
        };

        // default construct
        private AsyncDeleteResource() {
        }

        public void execute() {
            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            synchronized (mMutexLock) {
                int i = 0;
                if (mGiveupList == null) {
                    Log.d(TAG, "pictures had been deleted");
                    return;
                }
                for (i = 0; i < mGiveupList.size(); i++) {
                    Uri delUri = mGiveupList.get(i);
                    boolean result = false;
                    Log.d(TAG, "delete picture uri = " + delUri);
                    if (delUri != null) {
                        ContentResolver resolver = getContext().getContentResolver();
                        // search item from database
                        Cursor cursor = null;
                        String path = null;
                        try {
                            cursor = resolver.query(delUri, mColumns, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                path = cursor.getString(0);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "serach data from database failed, PLS ignore. URI = "
                                    + delUri, e);
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                        Log.d(TAG, "delete file path = " + path);

                        if (result = (path != null)) {
                            try {
                                // delete database data by delUri
                                resolver.delete(delUri, null, null);
                            } catch (Exception e) {
                                Log.e(TAG, "delete data from database failed, PLS ignore. URI = "
                                        + delUri, e);
                            } finally {
                                // delete file
                                File d = new File(path);
                                StorageUtil storageUtil = StorageUtil.getInstance();
                                ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
                                if (d.exists() && d.isFile() && !d.isHidden()) {
                                    if (storageUtil.isInternalPath(path)) {
                                        result = d.delete();
                                    } else {
                                        String filePath = storageUtil.getFileName(d);
                                        Uri uri = storageUtil.getCurrentAccessUri(path);
                                        result = externalStorageUtil.deleteDocument(resolver,uri,filePath);
                                    }
                                }
                                Log.i(TAG, "delete picture success = " + result);
                            }
                        }
                    }
                    if (result && i == mGiveupList.size()-1){
                        mFinishDeleted = true;
                    }
                    if (result) {
                        Message msg = sHandler.obtainMessage(VAL_STATE_DELETE, delUri);
                        sHandler.sendMessage(msg);
                    }
                }
                clearGiveupPicureList();

            }
        }
    }
    public boolean isFinishDeleted(){
        return mFinishDeleted;
    }

    /**
     * For VAL_STATE_DELETE op. must in main thread, so define this Handler.
     * 
     * @author SPREADTRUM\liyan.zhu
     */
    private class ViewHandler extends Handler {
        // default construct
        private ViewHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;

            switch (what) {
                case AsyncDeleteResource.VAL_STATE_DELETE:
                    synchronized (mDeleteTask.mMutexLock) {
                        Uri delUri = ((Uri) msg.obj);
                        if (mListener != null) {
                            mListener.dreamProxyFinishDeleted(delUri);
                        }
                        break;
                    }

            }
        }
    }

    public void setListener(DreamProxyFreezeFrameClick listener) {
        mListener = listener;
    }

    /**
     * All click operations in freezeDisplayView defined in this class.
     * 
     * @author SPREADTRUM\liyan.zhu
     */
    private class ControlOnClickListener implements View.OnClickListener {

        private Runnable sRunnableDelete;

        // default construct
        private ControlOnClickListener() {
            sRunnableDelete = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "execute delete picture work thread ... ...");
                    mDeleteTask.execute();
                }
            };
        }

        // Done and Cancel
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_freeze_frame_done:
                    if (mListener == null)
                        break;
                    getGiveupPicureList(false);
                    Log.d(TAG, "on done click");
                    mCancel.setOnClickListener(null);
                    mDeleteTask.execute();
                    mListener.dreamProxyDoneClicked(true);// ATTENTION: this controls ui, so should in
                                                      // main thread
                    break;
                case R.id.btn_feeze_frame_cancel:
                    if (mListener == null)
                        break;
                    Log.d(TAG, "on cancle click");
                    mDone.setOnClickListener(null);//SPRD:fix bug940922
                    showAlertDialog();
                    break;

            }
        }

        // Canel Dialog
        public void showAlertDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            //final AlertDialog alertDialog = builder.create();
            Context ctx = getContext();
            builder.setMessage(ctx.getString(R.string.dream_freeze_frame_confirm_delete_text));
            builder.setPositiveButton(ctx.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            getGiveupPicureList(true);
                            sRunnableDelete.run();
                            if (mListener != null) {
                                mListener.dreamProxyDoneClicked(false);// ATTENTION: this controls ui, so should in main thread
                            }
                        }
                    });
            builder.setNegativeButton(ctx.getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            mAlertDialog.dismiss();
                            mAlertDialog = null;
                            mDone.setOnClickListener(mClickListener);
                        }
                    });
            builder.setCancelable(false);//SPRD:fix bug940922
            mAlertDialog = builder.create();//SPRD:fix bug1223509
            if (!mAlertDialog.isShowing()) {
                mAlertDialog.show();
            }

        }
    }

    /**
     * copy all/part of file uri in mCurIntervalList to mGiveupList.
     * 
     * @param isDeleteAll:Delete All files in mCurIntervalList or just delete part of it.
     */
    private void getGiveupPicureList(boolean isDeleteAll) {

        mGiveupList = new ArrayList<Uri>();
        mSaveList = new ArrayList<Uri>(mCurIntervalList);
        if (isDeleteAll) {
            mGiveupList.addAll(mCurIntervalList);
        } else {
            if (!mCheckbox1.isChecked() && mCurIntervalList.size() > 0) {
                mGiveupList.add(mCurIntervalList.get(0));
            }
            if (!mCheckbox2.isChecked() && mCurIntervalList.size() > 1) {
                mGiveupList.add(mCurIntervalList.get(1));
            }
            if (!mCheckbox3.isChecked() && mCurIntervalList.size() > 2) {
                mGiveupList.add(mCurIntervalList.get(2));
            }
            if (!mCheckbox4.isChecked() && mCurIntervalList.size() > 3) {
                mGiveupList.add(mCurIntervalList.get(3));
            }
        }
        mSaveAll = mGiveupList.size() == 0 ? true : false;
        for (int i = 0;i < mGiveupList.size();i++){
            mSaveList.remove(mGiveupList.get(i));
        }
    }

    private void clearGiveupPicureList() {
        if (mGiveupList != null) {
            mGiveupList.clear();
            mGiveupList = null;
        }
    }
    public void clearSaveList(){
        if (mSaveList != null){
            mSaveList.clear();
            mSaveList = null;
        }
        mFinishDeleted = false;
    }

    public boolean isSaveAll(){
        return mSaveAll;
    }
    public void showAlertDialog() {
        if (mClickListener != null) {
            mClickListener.onClick(mCancel);
        }
    }

    private class CheckedChangedListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton button, boolean isChecked) {
            boolean enable = false;
            List<CheckBox> checkBoxs = new ArrayList<CheckBox>();
            checkBoxs.add(mCheckbox1);
            checkBoxs.add(mCheckbox2);
            checkBoxs.add(mCheckbox3);
            checkBoxs.add(mCheckbox4);
            for (int i = 0; i < mCurIntervalList.size(); i++) {
                if (checkBoxs.get(i).isChecked()) {
                    enable = true;
                    break;
                }
            }
            mDone.setEnabled(enable);
            /*
            if (isChecked) {
                if (mCheckbox1.isChecked() || mCheckbox2.isChecked() || mCheckbox3.isChecked()
                        || mCheckbox4.isChecked()) {
                    mDone.setEnabled(true);
                }
            } else {
                if (!mCheckbox1.isChecked() && !mCheckbox2.isChecked() && !mCheckbox3.isChecked()
                        && !mCheckbox4.isChecked()) {
                    mDone.setEnabled(false);
                }
            }
            */
        }
    }
    //bugfix 1014523
    public void setCurIntervalList(ArrayList<Uri> intervalDisplayList){
        mCurIntervalList = intervalDisplayList;
    }
    /* nj dream camera test 84 */
    public void reset() {
        intervalImage1.setVisibility(View.INVISIBLE);
        intervalImage2.setVisibility(View.GONE);
        intervalImage3.setVisibility(View.INVISIBLE);
        intervalImage4.setVisibility(View.GONE);
        mImage1.setImageBitmap(null);
        mImage2.setImageBitmap(null);
        mImage3.setImageBitmap(null);
        mImage4.setImageBitmap(null);
        if (mCurIntervalList != null) {
            mCurIntervalList.clear();
        }
        if (mBmpFinal != null && !mBmpFinal.isRecycled()) {
            mBmpFinal.recycle();
            mBmpFinal = null;
        }
        if (mAlertDialog != null){ //SPRD:fix bug1223509
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
    /* @} */

    public int getGiveupNum() {
        if (mGiveupList != null) {
            return mGiveupList.size();
        }

        return -1;
    }
    Bitmap mBmpFinal = null;

    class IntervalCombo {
        // Bug 839474: CCN optimization
        // this class is added to reduce CCN of prepare()
        ImageView iv;
        CheckBox cb;
        FrameLayout fl;

        IntervalCombo() {
            iv = null;
            cb = null;
            fl = null;
        }

        void switchByIndex(int index) {
            switch (index) {
                case 0:
                    iv = mImage1;
                    cb = mCheckbox1;
                    fl = intervalImage1;
                    break;
                case 1:
                    iv = mImage2;
                    cb = mCheckbox2;
                    fl = intervalImage2;
                    break;
                case 2:
                    iv = mImage3;
                    cb = mCheckbox3;
                    fl = intervalImage3;
                    break;
                case 3:
                    iv = mImage4;
                    cb = mCheckbox4;
                    fl = intervalImage4;
                    break;
            }
        }

        boolean check() {
            return(iv != null & cb != null & fl != null);
        }
    }

    public void prepare(final int index, final Uri uri) {
        IntervalCombo ic = new IntervalCombo();
        ic.switchByIndex(index);// Bug 839474: CCN optimization
        // split to another 2 methods of class IntervalCombo for CCN optimization , from 15 to 7+5+1

        /*SPRD:fix bug625571 change interval decode for OOM @{ */
        try {
            InputStream inputStream = mContentResolver.openInputStream(uri);

            int viewWidth = mImageMaxWidth;
            int viewHeight = mImageMaxHeight;

            if (mImageWidth > mImageHeight) {
                int dummy = mImageHeight;
                mImageHeight = mImageWidth;
                mImageWidth = dummy;

                // pls ref Comment 2 of Bug 688335
                viewHeight = mImageMaxWidth;
                viewWidth = mImageMaxWidth * mImageWidth / mImageHeight;
            }

            Bitmap bmp = FilmstripItemUtils
                    .loadImageThumbnailFromStream(
                            inputStream,
                            mImageWidth,
                            mImageHeight,
                            viewWidth , viewHeight ,
                            0, MAX_PEEK_BITMAP_PIXELS);//make sure width is smaller than height
            if (mOrientation != 0 && bmp != null) { // Bug 1159188 (NULL_RETURNS)
                Matrix matrix = new Matrix();
                matrix.postRotate(mOrientation);
                mBmpFinal = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                        bmp.getHeight(), matrix, true);
            } else {
                mBmpFinal = bmp;
            }
            //if (mImage != null && mCheckbox != null && intervalImage != null) {
            if (ic.check()) {
                ((CameraActivity) getContext()).getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        //if (mImage != null && mCheckbox != null && intervalImage != null) {
                        if (ic.check()) {
                            ic.iv.setImageBitmap(mBmpFinal);
                            ic.cb.setChecked(true);
                            ic.fl.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "prepare error:" + e.getMessage());
        }
    }

    public Uri getLastSaveUri(){
        return mSaveList.get(mSaveList.size()-1);
    }
    private int mImageWidth = 0;
    private int mImageHeight = 0;
    private int mOrientation = 0;
    private int mImageMaxWidth = 0;
    private int mImageMaxHeight = 0;
    private float freeze_padding = 0f;
    private static final int MAX_PEEK_BITMAP_PIXELS = 1600000; // 1.6 * 4 MBs.
    protected void setPictureInfo(int width, int height, int orientation) {
        mImageWidth = width;
        mImageHeight = height;
        mOrientation = orientation;
    }
    /* @} */
}
