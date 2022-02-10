package com.dream.camera.filter;
import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.dream.camera.ucam.utils.LogUtils;
import com.dream.camera.ucam.utils.UiUtils;
import com.android.camera2.R;

import android.graphics.Paint;
import android.util.AttributeSet;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.widget.Toast;

import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;

public class ArcsoftSmallAdvancedFilter implements View.OnTouchListener /*, TsAdvancedFilterNative.OnReceiveBufferListener */ {
    private static final String TAG = "ArcsoftSmallAdvancedFilter";

    private static final int SNAP_VELOCITY = 80;
    private ViewGroup viewGroup;
    private Context context;
    private LinearLayout layout;
    private HorizontalScrollView hscrllV;
    private boolean isDown;
    private float downX;
    private float lastMovedX;
    private VelocityTracker mVelocityTracker;
    private GridView imgGridView;
    private ArcsoftFilterSmallGridAdapter mAdapter;
    private ArrayList<View> chacheViews;
    private int scrnNumber;
    private int numberPerSrcn;
    private int currentScrn;
    private int imgW, imgH;
    private float itemOffsetW;
    /**
     * screen width
     */
    private int scrnW;
    private boolean isReleaseEffectRes;
    private SparseArray<int []>filtersTable;
    private SparseIntArray gpuEffectTypes;
    private Handler handler = new Handler();
    private int mFilterType;
    private ArrayList<String> textName;
    private ArrayList<Integer> mFilterImage;
    private ArrayList<Integer>mFilterSelectedImage;
    private boolean SupRealPreviewThum = false;
    private static Toast mToast;

    private int mPreposition = 0;
    private DataModuleBasic mDataModuleCurrent;
    private int mOrientation;
    private float itemW;
    private FilterSurfaceViewInterface mGlSurfaceView;

    public ArcsoftSmallAdvancedFilter(Context context, ViewGroup viewGroup) {
        /*
         * ArcsoftSmallAdvancedFilter implements TsAdvancedFilterNative.OnReceiveBufferListener interface,
         * to callback onReceiveBuffer(int, int[], int, int) method
         * This must do.
         */
        this.context = context;
        this.viewGroup = viewGroup;
//        mCamera = (CameraActivity)context;
        //initData();
        //initImgGirdView();

//        mSettingManager = new SettingsManager(context);

        //setFilterType(ArcFilterGlobalDefine.ARCFILTER_EFFECTID_FILTER_LOMOMAGIC_SOFT);
    }

    public void init(FilterSurfaceViewInterface gl) {
        this.mGlSurfaceView = gl;
        initFiltersTable();
        initData();
        initImgGirdView();
    }

    private void initFiltersTable() {
        filtersTable = new SparseArray<int[]>();
        int itable = 0;
        /*
         * small filter types
         */


        /******************************************add new filters **********************************************************************/
        /**
         * TODO: NOTE if you change some one of filtersTable, you should modify dream_camera_arrays_photo_part.xml's code;
         * otherwise will make current filter type can not hold when restart Camera
         * AND you should make sure modify "gpuEffectTypes.put(*" code blow to matching this
         */
        if (CameraUtil.isUseSprdFilter()) {
            mGlSurfaceView.initFiltersTable(filtersTable, itable++);
        }

        /*
         * Big preveiw filter types
         */
        int gpuIndex = 0;
        /**
         * TODO: NOTE if you change some one of gpuEffectTypes, you should modify dream_camera_arrays_photo_part.xml's code
         * AND make sure modify filtersTable's value before to matching this
         * otherwise will make current filter type can not hold when restart Camera
         */
        gpuEffectTypes = new SparseIntArray();
        if (CameraUtil.isUseSprdFilter()){
            mGlSurfaceView.initEffectTypes(gpuEffectTypes, gpuIndex);
        }

    }

    private void initData() {
        scrnW = UiUtils.screenWidth() + UiUtils.screenWidth() / (CameraUtil.isUseSprdFilterPlus() ? 13 : 10);
        currentScrn = 0;
        numberPerSrcn = 6;
        scrnNumber = filtersTable.size();
        chacheViews = new ArrayList<View>();
        isDown = false;
        downX = 0.0f;
        lastMovedX = 0.0f;
        textName = new ArrayList<String>();
        mFilterImage = new ArrayList<Integer>();
        mFilterSelectedImage = new ArrayList<Integer>();

        if(CameraUtil.isUseSprdFilter()){
            mGlSurfaceView.initRes(textName,mFilterImage,mFilterSelectedImage,SupRealPreviewThum);
        }
    }

    private void initImgGirdView() {
        layout = (LinearLayout) viewGroup.findViewById(R.id.cpu_small_effects_layout_id);
        hscrllV = (HorizontalScrollView) viewGroup.findViewById(R.id.cpu_small_effects_horizontalScrollV_id);
        //hscrllV.setOnTouchListener(this);//SPRD:Fix bug 464000 filter menu double icons
        int effectNumber = gpuEffectTypes.size();
        int horizontalSpace = computeK(3.0f, scrnW);
        int itemLayoutW = scrnW / numberPerSrcn - horizontalSpace;
        int itemLayoutH = itemLayoutW;
        //float offset = 0.2f;
        float offset = 0;
        itemW = scrnW / (numberPerSrcn + offset);
        int layoutW = (int) (itemW * effectNumber);
        itemOffsetW = itemLayoutW * offset;
        imgGridView = (GridView) viewGroup.findViewById(R.id.cpu_small_effects_gridv_id);
        imgGridView.setNumColumns(effectNumber);
        imgGridView.setLayoutParams(new LinearLayout.LayoutParams(layoutW,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        imgGridView.setHorizontalSpacing(horizontalSpace);
        GridView.LayoutParams itemLayoutLp = new GridView.LayoutParams(itemLayoutW, itemLayoutH);
        int padding = computeK(3.0f, scrnW);
        imgW = itemLayoutW - padding * 2;
        imgH = imgW;
        for (int i = 0; i < effectNumber; i++) {
            //ImageView imgV = new ImageView(context);
            FilterImageView imgV = new FilterImageView(context , itemLayoutW , itemLayoutH);
            imgV.setScaleType(ScaleType.FIT_CENTER);
            imgV.setLayoutParams(itemLayoutLp);
            if (SupRealPreviewThum) {
//                imgV.setBackgroundResource(R.drawable.ucam_cpu_effect_btn_selector);
            } else {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mFilterImage.get(i));
            }
            if (i == 0) {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mFilterSelectedImage.get(i));
            }
            imgV.setPadding(padding, padding, padding, padding);
            imgV.setText(textName.get(i));
            chacheViews.add(imgV);
        }
        mAdapter = new ArcsoftFilterSmallGridAdapter(chacheViews);
//        imgGridView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return MotionEvent.ACTION_MOVE == event.getAction() ? true : false;
//            }
//        });
        imgGridView.setAdapter(mAdapter);
        imgGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int firstIndex = firstIndexInCurrentScrn();
                int lastIndex = lastIndexInCurrentScrn();
                /* SPRD:Fix bug 464000 filter menu double icons @{
                if (position > lastIndex || position < firstIndex) {
                    return;
                }
                @} */
                /* Dream Camera ui check 190, 191 */
                ((ImageView) view).setImageResource(mFilterSelectedImage.get(position));
                if (mPreposition != position) {
                    /* Dream Camera ui check 190, 191 */
                    ((ImageView) imgGridView.getChildAt(mPreposition)).setImageResource(mFilterImage.get(mPreposition));
                    mPreposition = position;
                }
                int filterType = gpuEffectTypes.get(position);
                setFilterType(filterType);
                //hideFaceForSpecialFilter(isFaceDetection);
            }
        });
    }

    public void updateUI(float aspectRatio) {
        /** bug802833 move to bottombar
        Resources res = context.getResources();
        int mBottomBarOptimalHeight = res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal);
        int mBottomBarMaxHeight = res.getDimensionPixelSize(R.dimen.bottom_bar_height_max);
        int top = layout.getPaddingTop();
        if (aspectRatio > 4f / 3f) {
            if (CameraUtil.isNavigationEnable()) {
                int bottomBarHeight = res.getDimensionPixelSize(R.dimen.bottom_bar_height);
                layout.setPadding(0, top, 0, bottomBarHeight);
            } else {
                layout.setPadding(0, top, 0, mBottomBarOptimalHeight);
            }

        } else {
            layout.setPadding(0, top, 0, mBottomBarMaxHeight);
        }
        */
        Log.d(TAG, "updateUI: " + mFilterType);
        final int scrolx = (int)itemW * gpuEffectTypes.indexOfValue(mFilterType);
        hscrllV.scrollTo(scrolx, 0);
    }

    public void updateBottomPadding(int height) {
        if (layout != null) {
            Log.i(TAG, "updateBottomPadding =" + height);
            layout.setPadding(0, 0, 0, height + 1);
        }
    }

    private int computeK(float k, float screenWidth) {
        float sf = screenWidth / 540.0f;
        int rp = (int) (k * sf);
        int si = (int) sf;
        float fs = sf - si;
        if (fs > 0.5f) {
            rp += 1;
        }
        return rp;
    }

    public void requestLayout(){
        if(layout != null){
            layout.requestLayout();
        }
    }

    public void setVisibility(int visiblity) {
        layout.setVisibility(visiblity);
        if (visiblity != View.VISIBLE) {
            isReleaseEffectRes = true;
        }
    }

    public int getFilterType(){
        return mFilterType;
    }

    /*
     * Change filter type
     * @param filterType  filter type
     */
    public void setFilterType(int filterType) {
        Log.i(TAG,"setFilterType = " + filterType);

        mDataModuleCurrent = DataModuleManager.getInstance(context).getCurrentDataModule();
        if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_FILTER_TYPE) != filterType) {
            mDataModuleCurrent.changeSettings(Keys.KEY_CAMERA_FILTER_TYPE, filterType);
            mFilterType = filterType;
        }
        updateFilterTypeUI();
    }

    private void updateFilterType() {

        mFilterType = DataModuleManager.getInstance(context)
                .getCurrentDataModule().getInt(Keys.KEY_CAMERA_FILTER_TYPE);
        Log.i(TAG, "updateFilterType = " + mFilterType);
        final int scrolx = (int) itemW * gpuEffectTypes.indexOfValue(mFilterType);
        hscrllV.scrollTo(scrolx, 0);
    }

    public boolean isVisible () {
       return layout.getVisibility() == View.VISIBLE;
    }

    // SPRD: nj dream camera debug 117
    public void setDreamFilterType(int filterType) {
        updateFilterType();
        for (int i = 0; i < gpuEffectTypes.size(); i++) {
            if (filterType == gpuEffectTypes.get(i) && i < imgGridView.getChildCount()) {
                // SPRD: add for bug 610478 monkey test cause getChildAt(i) return a null object
                try{
                    ((ImageView)imgGridView.getChildAt(mPreposition)).setImageResource(mFilterImage.get(mPreposition));//SRPD:fix bug1414602
                    ((ImageView)imgGridView.getChildAt(i)).setImageResource(mFilterSelectedImage.get(i));
                    mPreposition = i;
                    setFilterType(filterType);
                } catch (NullPointerException e) {
                    Log.e(TAG, "setDreamFilterType: i = " + i + "; msg:" +  e.getMessage());
                }
                return;
            }
        }
    }

    private synchronized void changeScrn(int which) {
        boolean isEdge = false;
        if (which >= scrnNumber) {
            which = scrnNumber - 1;
            isEdge = true;
        } else if (which < 0) {
            which = 0;
            isEdge = true;
        }
        currentScrn = which;
        if (isEdge) {
            return;
        }
        float lsw = itemOffsetW * which;
        int tox = (int) (currentScrn * scrnW - lsw);
        hscrllV.scrollTo(tox, 0);
    }

    private synchronized int getCurrentScrn() {
        return currentScrn;
    }

    private int[] filtersInCurrentScrn() {
        int cs = getCurrentScrn();
        return filtersTable.get(cs);
    }

    private int firstIndexInCurrentScrn() {
        int index = getCurrentScrn() * numberPerSrcn;
        return index;
    }

    private int lastIndexInCurrentScrn() {
        int fIndex = firstIndexInCurrentScrn();
        int lIndex = fIndex + numberPerSrcn - 1;
        return lIndex;
    }

    private View getItemViewFromGridView(int index) {
        final ArrayList<View> lv = chacheViews;
        if (index < 0 || index >= lv.size()) {
            return null;
        }
        View v = lv.get(index);
        return v;
    }

    /*
     * Receive YUV frame data from camera, and push YUV data to sub thread to process
     *
     */
    public void setPreviewData(final byte[] yuvData, final int frameWidth, final int frameHeight) {
        if (yuvData != null) {
        }
    }

    private void startDown(float downX) {
        if (!isDown) {
            this.downX = downX;
            isDown = true;
        }
    }

    private void endUp() {
        isDown = false;
        downX = 0.0f;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void scrollBy(int byX) {
        if (currentScrn <= 0 && byX > 0) {
            return;
        }
        if (currentScrn >= scrnNumber - 1 && byX < 0) {
            return;
        }
        hscrllV.scrollBy(byX, 0);
    }

    private void scrollToDst(MotionEvent event) {
        float upX = event.getX();
        int dx = (int) (downX - upX);
        int whichScrn = (dx + scrnW / 2 + currentScrn * scrnW) / scrnW;
        changeScrn(whichScrn);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        final int action = event.getAction();
        LogUtils.debug(TAG + "OwenX", "onTouch(): action is " + action);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                int moved = (int) (lastMovedX - x);
                int startix = (int) lastMovedX;
                startDown(x);
                lastMovedX = x;
                if (startix != 0) {
                    scrollBy(moved);
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                if (velocityX > SNAP_VELOCITY) {
                    changeScrn(currentScrn - 1);
                } else if (velocityX < -SNAP_VELOCITY) {
                    changeScrn(currentScrn + 1);
                } else {
                    scrollToDst(event);
                }
                endUp();
                break;
        }
        return true;
    }

    public void onDestroy() {
//        TsAdvancedFilterNative.destroy();
    }

    public boolean isSupportRealPreviewThum() {
        return SupRealPreviewThum;
    }

    /*
     * SPRD:Fix bug 484608 Face detection is mutual exclusion with some filter
     * mode.@{
     */
    public static void showToast(Context context, int msg, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, duration);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }

    //private boolean isFaceDetection = true;

    /*public void hideFaceForSpecialFilter(boolean face) {
        android.util.Log.e(TAG, "face  = " + face);
        isFaceDetection = face;
        if (!face) {return;}
        android.util.Log.e(TAG, "mFilterType  = " + mFilterType);
        if (mFilterType == 103 || mFilterType == 101 || mFilterType == 405) {
            mUcamFilterPhotoModule.stopFaceDetection();
            showToast(context, R.string.face_filter_mutex, Toast.LENGTH_SHORT);
        } else {
            mUcamFilterPhotoModule.startFaceDetection();
        }
    }*/

    /* SPRD: Fix bug 578679,  Add Filter type of symmetry right. @{ */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
        updateFilterTypeUI();
    }

    public void updateFilterTypeUI() {
        if (CameraUtil.isUseSprdFilter()) {
            if (mGlSurfaceView != null)
                mGlSurfaceView.setFilterType(mFilterType);
        }
    }
    /* @}*/

    public void resetFilterItem() {
        imgGridView.getChildAt(0).setBackgroundResource(R.drawable.ucam_cpu_effect_bg_selected);
//        imgGridView.getChildAt(mPreposition).setBackgroundResource(R.drawable.ucam_cpu_effect_btn_selector);
        imgGridView.getChildAt(mPreposition).setBackgroundColor(Color.TRANSPARENT);
        mPreposition = 0;
       int filterType = gpuEffectTypes.get(0);
       setFilterType(filterType);
    }

    public void removeOnReceiveBufferListener() {
        //TsAdvancedFilterNative.removeOnReceiveBufferListener(this);
    }

    public void setFilterUIEnable(boolean enable){
        imgGridView.setEnabled(enable);
    }
}

class FilterImageView extends ImageView {
    public FilterImageView(Context context , int width , int height) {
        super(context);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public FilterImageView(Context context, int width , int height , AttributeSet attrs) {
        super(context, attrs);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public FilterImageView(Context context, int width , int height , AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    String text;
    public Paint paint = null;
    Rect targetRect = null;
    int baseline;

    public void setText(String t) {
        text = t;
    }

    public void prepareToDraw(int w , int h) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(3);
        paint.setTextSize(23);
        paint.setAlpha(0);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        paint.setTypeface(font);
        paint.setShadowLayer(20, 2, 2, Color.BLACK);
        int scrnW = UiUtils.screenWidth() + UiUtils.screenWidth() / 9;
        int padding = (int) (scrnW / 540.0f * 12f);
        targetRect = new Rect(0, (h - padding * 2) * 4 / 5 + padding, w, h - padding);
        FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        baseline = targetRect.top
                + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top)
                / 2 - fontMetrics.top;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.TRANSPARENT);
        canvas.drawRect(targetRect, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, targetRect.centerX(), baseline, paint);
    }
}
