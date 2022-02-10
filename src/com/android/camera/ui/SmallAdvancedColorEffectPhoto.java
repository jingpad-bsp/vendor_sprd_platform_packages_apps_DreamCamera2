package com.android.camera.ui;

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
import android.widget.*;
import android.widget.ImageView.ScaleType;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoModule;
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

import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;

public class SmallAdvancedColorEffectPhoto implements View.OnTouchListener /*, TsAdvancedFilterNative.OnReceiveBufferListener */ {
    private static final String TAG = "SmallAdvancedColorEffectPhoto";

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
    private ColorEffectPhotoSmallGridAdapter mAdapter;
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
    private SparseArray<int []>ColorEffectTable;
    private SparseIntArray gpuEffectTypes;
    private Handler handler = new Handler();
    private int mColorEffectType;
    private String mConvertedColorEffectType;
    private ArrayList<String> textName;
    private ArrayList<Integer> mColorEffectImage;
    private ArrayList<Integer> mColorEffectSelectedImage;
    private boolean SupRealPreviewThum = false;
    private static Toast mToast;

    private int mPreposition = 0;
    private DataModuleBasic mDataModuleCurrent;
    private int mOrientation;
    private float itemW;
    private CameraActivity mActivity;

    public int None = 1;
    public int Mono = 2;
    public int Negative = 3;
    public int Sepia = 4;
    public int Cold = 5;
    public int Antique = 6;
    public SmallAdvancedColorEffectPhoto(Context context, ViewGroup viewGroup) {
        this.context = context;
        this.viewGroup = viewGroup;
        mActivity = (CameraActivity) context;
    }

    public void initphoto() {
        initColorEffectPhotoTable();
        initPhotoData();
        initImgGirdView();
    }
    private void initColorEffectPhotoTable() {
        ColorEffectTable = new SparseArray<int[]>();
        int itable = 0;
        /*
         * small Filter types
         */


        /******************************************add new Filter **********************************************************************/
        /**
         * TODO: NOTE if you change some one of FilterTable, you should modify dream_camera_arrays_photo_part.xml's code;
         * otherwise will make current Filter type can not hold when restart Camera
         * AND you should make sure modify "gpuEffectTypes.put(*" code blow to matching this
         */
        ColorEffectTable.put(itable++,
                new int[]{None ,
                        Mono ,
                        Negative ,
                        Sepia ,
                        Cold ,
                        Antique ,
                });

        /*
         * Big preveiw Filter types
         */
        int gpuIndex = 0;
        /**
         * TODO: NOTE if you change some one of gpuEffectTypes, you should modify dream_camera_arrays_photo_part.xml's code
         * AND make sure modify FilterTable's value before to matching this
         * otherwise will make current Filter type can not hold when restart Camera
         */
        gpuEffectTypes = new SparseIntArray();
        initColorEffectPhotoTypes(gpuEffectTypes, gpuIndex);

    }

    private void initPhotoData() {
        scrnW = UiUtils.screenWidth() + UiUtils.screenWidth() / 9;
        currentScrn = 0;
        numberPerSrcn = 6;
        scrnNumber = ColorEffectTable.size();
        chacheViews = new ArrayList<View>();
        isDown = false;
        downX = 0.0f;
        lastMovedX = 0.0f;
        textName = new ArrayList<String>();
        mColorEffectImage = new ArrayList<Integer>();
        mColorEffectSelectedImage = new ArrayList<Integer>();
        initPhotoRes(textName,mColorEffectImage,mColorEffectSelectedImage,SupRealPreviewThum);

    }

    private void initImgGirdView() {
        layout = (LinearLayout) viewGroup.findViewById(R.id.cpu_small_effects_layout_id);
        hscrllV = (HorizontalScrollView) viewGroup.findViewById(R.id.cpu_small_effects_horizontalScrollV_id);
        int effectNumber = gpuEffectTypes.size();
        int horizontalSpace = computeK(3.0f, scrnW);
        int itemLayoutW = scrnW / numberPerSrcn - horizontalSpace;
        int itemLayoutH = itemLayoutW;
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
            ColorEffectImageView imgV = new ColorEffectImageView(context , itemLayoutW , itemLayoutH);
            imgV.setScaleType(ScaleType.FIT_CENTER);
            imgV.setLayoutParams(itemLayoutLp);
            if (SupRealPreviewThum) {
//                imgV.setBackgroundResource(R.drawable.ucam_cpu_effect_btn_selector);
            } else {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mColorEffectImage.get(i));
            }
            if (i == 0) {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mColorEffectSelectedImage.get(i));
            }
            imgV.setPadding(padding, padding, padding, padding);
            imgV.setText(textName.get(i));

            chacheViews.add(imgV);
        }

        mAdapter = new ColorEffectPhotoSmallGridAdapter(chacheViews);
        imgGridView.setAdapter(mAdapter);
        imgGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int firstIndex = firstIndexInCurrentScrn();
                int lastIndex = lastIndexInCurrentScrn();
                /* Dream Camera ui check 190, 191 */
                ((ImageView) view).setImageResource(mColorEffectSelectedImage.get(position));
                if (mPreposition != position) {
                    /* Dream Camera ui check 190, 191 */
                    ((ImageView) imgGridView.getChildAt(mPreposition)).setImageResource(mColorEffectImage.get(mPreposition));
                    mPreposition = position;
                }
                int FilterType = gpuEffectTypes.get(position);
                String ConvertedFilterType = getConvertedColorEffectType(FilterType);
                setColorEffectType(ConvertedFilterType);
                ((PhotoModule)(mActivity.getCurrentModule())).updateParametersColorEffect();

            }
        });
        // to select last saved Filter type
    }
    public String getConvertedColorEffectType(int ColorEffectType){
        switch(ColorEffectType) {
            case 1 : {
                return "NONE";
            }
            case 2 : {
                return "MONO";
            }
            case 3 : {
                return "NEGATIVE";
            }
            case 4 : {
                return "SEPIA";
            }
            case 5 : {
                return "COLD";
            }
            case 6 : {
                return "ANTIQUE";
            }
        }
        return "NONE";
    }
    public void updateUI() {

        Log.d(TAG, "updateUI: " + mColorEffectType);
        final int scrolx = (int)itemW * gpuEffectTypes.indexOfValue(mColorEffectType);
        hscrllV.scrollTo(scrolx, 0);
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

    public int getColorEffectType(){
        return mColorEffectType;
    }

    /*
     * Change Filter type
     * @param FilterType  Filter type
     */
    public void setColorEffectType(String ColorEffectType) {
        Log.i(TAG,"setColorEffectType = " + ColorEffectType);

        mDataModuleCurrent = DataModuleManager.getInstance(context).getCurrentDataModule();
        if (mDataModuleCurrent.getString(Keys.KEY_CAMERA_COLOR_EFFECT) != ColorEffectType) {
            mDataModuleCurrent.changeSettings(Keys.KEY_CAMERA_COLOR_EFFECT, ColorEffectType);
            mConvertedColorEffectType = ColorEffectType;
        }
    }

    private void updateColorEffectType() {
        switch(DataModuleManager.getInstance(context)
                .getCurrentDataModule().getString(Keys.KEY_CAMERA_COLOR_EFFECT)) {
            case "NONE": {
                mColorEffectType = 1;
                break;
            }
            case "MONO": {
                mColorEffectType = 2;
                break;
            }
            case "NEGATIVE": {
                mColorEffectType = 3;
                break;
            }
            case "SEPIA": {
                mColorEffectType = 4;
                break;
            }
            case "COLD": {
                mColorEffectType = 5;
                break;
            }
            case "ANTIQUE": {
                mColorEffectType = 6;
                break;
            }
        }
        Log.i(TAG, "updateColorEffectType = " + mColorEffectType);
        final int scrolx = (int) itemW * gpuEffectTypes.indexOfValue(mColorEffectType);
        hscrllV.scrollTo(scrolx, 0);
    }

    public boolean isVisible () {
        return layout.getVisibility() == View.VISIBLE;
    }

    public void setDreamColorEffectType(int ColorEffectType) {
        updateColorEffectType();
        for (int i = 0; i < gpuEffectTypes.size(); i++) {
            if (ColorEffectType == gpuEffectTypes.get(i) && i != 0 && i < imgGridView.getChildCount()) {
                // SPRD: add for bug 610478 monkey test cause getChildAt(i) return a null object
                try{
                    ((ImageView)imgGridView.getChildAt(i)).setImageResource(mColorEffectSelectedImage.get(i));
                    ((ImageView)imgGridView.getChildAt(0)).setImageResource(mColorEffectImage.get(0));
                    mPreposition = i;
                    setColorEffectType(getConvertedColorEffectType(ColorEffectType));
                } catch (NullPointerException e) {
                    Log.e(TAG, "setDreamColorEffectType: i = " + i + "; msg:" +  e.getMessage());
                }
                return;
            }
        }
    }
    public void resetDreamColorEffectType(){
        if(mDataModuleCurrent.getString(Keys.KEY_CAMERA_COLOR_EFFECT) == "NONE" && imgGridView.getChildAt(0) != null){
            ((ImageView)imgGridView.getChildAt(0)).setImageResource(mColorEffectSelectedImage.get(0));
            mPreposition = 0;
            for (int i = 1; i < 6; i++) {
                ((ImageView)imgGridView.getChildAt(i)).setImageResource(mColorEffectImage.get(i));
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


    private int firstIndexInCurrentScrn() {
        int index = getCurrentScrn() * numberPerSrcn;
        return index;
    }

    private int lastIndexInCurrentScrn() {
        int fIndex = firstIndexInCurrentScrn();
        int lIndex = fIndex + numberPerSrcn - 1;
        return lIndex;
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

    public void initColorEffectPhotoTypes(SparseIntArray effectTypes, int startIndex) {
        effectTypes.put(startIndex++, None);
        effectTypes.put(startIndex++, Mono);
        effectTypes.put(startIndex++, Negative);
        effectTypes.put(startIndex++, Sepia);
        effectTypes.put(startIndex++, Cold);
        effectTypes.put(startIndex++, Antique);

    }

    public void initPhotoRes(ArrayList<String> textName, ArrayList<Integer> mFilterImage, ArrayList<Integer> mFilterSelectedImage, boolean SupRealPreviewThum) {

        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_none));
        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_mono));
        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_negative));
        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_antique));
        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_cold));
        textName.add(mActivity.getResources().getString(R.string.pref_camera_color_effect_entry_sepia));


        if (!SupRealPreviewThum) {
            mFilterImage.add(R.drawable.ic_filter_preview_original_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_mono_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_negative_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_antique_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_cold_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_sepia_unisoc);



            mFilterSelectedImage.add(R.drawable.ic_filter_preview_original_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_mono_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_negative_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_antique_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_cold_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_sepia_selected_unisoc);

        }

    }
    public static void showToast(Context context, int msg, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, duration);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }
}

class ColorEffectImageView extends ImageView {
    public ColorEffectImageView(Context context , int width , int height) {
        super(context);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public ColorEffectImageView(Context context, int width , int height , AttributeSet attrs) {
        super(context, attrs);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public ColorEffectImageView(Context context, int width , int height , AttributeSet attrs, int defStyle) {
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

