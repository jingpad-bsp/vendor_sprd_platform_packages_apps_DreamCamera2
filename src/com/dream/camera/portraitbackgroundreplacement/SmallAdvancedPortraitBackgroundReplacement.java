package com.dream.camera.portraitbackgroundreplacement;
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

import com.android.camera.CameraActivity;
import com.dream.camera.modules.portraitbackgroundreplacementphoto.sprd.PortraitBackgroundReplacementPhotoModule;
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

public class SmallAdvancedPortraitBackgroundReplacement implements View.OnTouchListener /*, TsAdvancedPortraitBackgroundReplacementNative.OnReceiveBufferListener */ {
    private static final String TAG = "SmallAdvancedPortraitBackgroundReplacement";

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
    private PortraitBackgroundReplacementSmallGridAdapter mAdapter;
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
    private SparseArray<int []>portraitbackgroundreplacementsTable;
    private SparseIntArray gpuEffectTypes;
    private Handler handler = new Handler();
    private int mPortraitBackgroundReplacementType;
    private ArrayList<String> textName;
    private ArrayList<Integer> mPortraitBackgroundReplacementImage;
    private ArrayList<Integer> mPortraitBackgroundReplacementSelectedImage;
    private boolean SupRealPreviewThum = false;
    private static Toast mToast;

    private int mPreposition = 0;
    private DataModuleBasic mDataModuleCurrent;
    private int mOrientation;
    private float itemW;
    private CameraActivity mActivity;

    public int NonePortraitBacgroundReplacement = 1;
    public int MonePortraitBacgroundReplacement = 2;
    public int UrbanPortraitBacgroundReplacement = 3;
    public int RelaxationPortraitBacgroundReplacement = 4;
    public int NightPortraitBacgroundReplacement = 5;
    public int BeachPortraitBacgroundReplacement = 6;
    public int PalacePortraitBacgroundReplacement = 7;
    public int MotionPortraitBacgroundReplacement = 8;
    public SmallAdvancedPortraitBackgroundReplacement(Context context, ViewGroup viewGroup) {
        this.context = context;
        this.viewGroup = viewGroup;
        mActivity = (CameraActivity) context;
    }

    public void initphoto() {
        initPortraitBackgroundReplacementsPhotoTable();
        initPhotoData();
        initImgGirdView();
    }
    public void initvideo() {
        initPortraitBackgroundReplacementsVideoTable();
        initVideoData();
        initImgGirdView();
    }
    private void initPortraitBackgroundReplacementsPhotoTable() {
        portraitbackgroundreplacementsTable = new SparseArray<int[]>();
        int itable = 0;
        /*
         * small portraitbackgroundreplacement types
         */


        /******************************************add new portraitbackgroundreplacements **********************************************************************/
        /**
         * TODO: NOTE if you change some one of portraitbackgroundreplacementsTable, you should modify dream_camera_arrays_photo_part.xml's code;
         * otherwise will make current portraitbackgroundreplacement type can not hold when restart Camera
         * AND you should make sure modify "gpuEffectTypes.put(*" code blow to matching this
         */
        portraitbackgroundreplacementsTable.put(itable++,
                new int[]{NonePortraitBacgroundReplacement ,
                        MonePortraitBacgroundReplacement ,
                        UrbanPortraitBacgroundReplacement ,
                        RelaxationPortraitBacgroundReplacement ,
                        NightPortraitBacgroundReplacement ,
                        BeachPortraitBacgroundReplacement ,
                        PalacePortraitBacgroundReplacement
                });

        /*
         * Big preveiw portraitbackgroundreplacement types
         */
        int gpuIndex = 0;
        /**
         * TODO: NOTE if you change some one of gpuEffectTypes, you should modify dream_camera_arrays_photo_part.xml's code
         * AND make sure modify portraitbackgroundreplacementsTable's value before to matching this
         * otherwise will make current portraitbackgroundreplacement type can not hold when restart Camera
         */
        gpuEffectTypes = new SparseIntArray();
        initEffectPhotoTypes(gpuEffectTypes, gpuIndex);

    }
    private void initPortraitBackgroundReplacementsVideoTable() {
        portraitbackgroundreplacementsTable = new SparseArray<int[]>();
        int itable = 0;
        /*
         * small portraitbackgroundreplacement types
         */


        /******************************************add new portraitbackgroundreplacements **********************************************************************/
        /**
         * TODO: NOTE if you change some one of portraitbackgroundreplacementsTable, you should modify dream_camera_arrays_photo_part.xml's code;
         * otherwise will make current portraitbackgroundreplacement type can not hold when restart Camera
         * AND you should make sure modify "gpuEffectTypes.put(*" code blow to matching this
         */
        portraitbackgroundreplacementsTable.put(itable++,
                new int[]{NonePortraitBacgroundReplacement ,
                        MonePortraitBacgroundReplacement ,
                        UrbanPortraitBacgroundReplacement ,
                        RelaxationPortraitBacgroundReplacement ,
                        NightPortraitBacgroundReplacement ,
                        BeachPortraitBacgroundReplacement ,
                        PalacePortraitBacgroundReplacement ,
                        MotionPortraitBacgroundReplacement
                });

        /*
         * Big preveiw portraitbackgroundreplacement types
         */
        int gpuIndex = 0;
        /**
         * TODO: NOTE if you change some one of gpuEffectTypes, you should modify dream_camera_arrays_photo_part.xml's code
         * AND make sure modify portraitbackgroundreplacementsTable's value before to matching this
         * otherwise will make current portraitbackgroundreplacement type can not hold when restart Camera
         */
        gpuEffectTypes = new SparseIntArray();
        initEffectVideoTypes(gpuEffectTypes, gpuIndex);

    }

    private void initPhotoData() {
        scrnW = UiUtils.screenWidth() + UiUtils.screenWidth() / 9;
        currentScrn = 0;
        numberPerSrcn = 6;
        scrnNumber = portraitbackgroundreplacementsTable.size();
        chacheViews = new ArrayList<View>();
        isDown = false;
        downX = 0.0f;
        lastMovedX = 0.0f;
        textName = new ArrayList<String>();
        mPortraitBackgroundReplacementImage = new ArrayList<Integer>();
        mPortraitBackgroundReplacementSelectedImage = new ArrayList<Integer>();
        initPhotoRes(textName,mPortraitBackgroundReplacementImage,mPortraitBackgroundReplacementSelectedImage,SupRealPreviewThum);

    }
    private void initVideoData() {
        scrnW = UiUtils.screenWidth() + UiUtils.screenWidth() / 9;
        currentScrn = 0;
        numberPerSrcn = 6;
        scrnNumber = portraitbackgroundreplacementsTable.size();
        chacheViews = new ArrayList<View>();
        isDown = false;
        downX = 0.0f;
        lastMovedX = 0.0f;
        textName = new ArrayList<String>();
        mPortraitBackgroundReplacementImage = new ArrayList<Integer>();
        mPortraitBackgroundReplacementSelectedImage = new ArrayList<Integer>();
        initVideoRes(textName,mPortraitBackgroundReplacementImage,mPortraitBackgroundReplacementSelectedImage,SupRealPreviewThum);

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
            PortraitBackgroundReplacementImageView imgV = new PortraitBackgroundReplacementImageView(context , itemLayoutW , itemLayoutH);
            imgV.setScaleType(ScaleType.FIT_CENTER);
            imgV.setLayoutParams(itemLayoutLp);
            if (SupRealPreviewThum) {
//                imgV.setBackgroundResource(R.drawable.ucam_cpu_effect_btn_selector);
            } else {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mPortraitBackgroundReplacementImage.get(i));
            }
            if (i == 0) {
                /* Dream Camera ui check 190, 191 */
                imgV.setImageResource(mPortraitBackgroundReplacementSelectedImage.get(i));
            }
            imgV.setPadding(padding, padding, padding, padding);
            imgV.setText(textName.get(i));
            chacheViews.add(imgV);
        }
        mAdapter = new PortraitBackgroundReplacementSmallGridAdapter(chacheViews);
        imgGridView.setAdapter(mAdapter);
        imgGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int firstIndex = firstIndexInCurrentScrn();
                int lastIndex = lastIndexInCurrentScrn();
                /* Dream Camera ui check 190, 191 */
                ((ImageView) view).setImageResource(mPortraitBackgroundReplacementSelectedImage.get(position));
                if (mPreposition != position) {
                    /* Dream Camera ui check 190, 191 */
                    ((ImageView) imgGridView.getChildAt(mPreposition)).setImageResource(mPortraitBackgroundReplacementImage.get(mPreposition));
                    mPreposition = position;
                }
                int portraitbackgroundreplacementType = gpuEffectTypes.get(position);
                setPortraitBackgroundReplacementType(portraitbackgroundreplacementType);
                mActivity.getCurrentModuleController().updatePortraitBackgroundReplacementType();

            }
        });
        // to select last saved portraitbackgroundreplacement type
        mDataModuleCurrent = DataModuleManager.getInstance(context.getApplicationContext()).getCurrentDataModule();
        mPortraitBackgroundReplacementType = mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        Log.d(TAG, "initImgGirdView: " + mPortraitBackgroundReplacementType);
    }

    public void updateUI(float aspectRatio) {

        Log.d(TAG, "updateUI: " + mPortraitBackgroundReplacementType);
        final int scrolx = (int)itemW * gpuEffectTypes.indexOfValue(mPortraitBackgroundReplacementType);
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

    public int getPortraitBackgroundReplacementType(){
        return mPortraitBackgroundReplacementType;
    }

    /*
     * Change portraitbackgroundreplacement type
     * @param portraitbackgroundreplacementType  portraitbackgroundreplacement type
     */
    public void setPortraitBackgroundReplacementType(int portraitbackgroundreplacementType) {
        Log.i(TAG,"setPortraitBackgroundReplacementType = " + portraitbackgroundreplacementType);

        mDataModuleCurrent = DataModuleManager.getInstance(context).getCurrentDataModule();
        if (mDataModuleCurrent.getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE) != portraitbackgroundreplacementType) {
            mDataModuleCurrent.changeSettings(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE, portraitbackgroundreplacementType);
            mPortraitBackgroundReplacementType = portraitbackgroundreplacementType;
        }
    }

    private void updatePortraitBackgroundReplacementType() {

        mPortraitBackgroundReplacementType = DataModuleManager.getInstance(context)
                .getCurrentDataModule().getInt(Keys.KEY_CAMERA_PORTRAITBACKGROUNDREPLACEMENT_TYPE);
        Log.i(TAG, "updatePortraitBackgroundReplacementType = " + mPortraitBackgroundReplacementType);
        final int scrolx = (int) itemW * gpuEffectTypes.indexOfValue(mPortraitBackgroundReplacementType);
        hscrllV.scrollTo(scrolx, 0);
    }

    public boolean isVisible () {
       return layout.getVisibility() == View.VISIBLE;
    }

    public void setDreamPortraitBackgroundReplacementType(int portraitbackgroundreplacementType) {
        updatePortraitBackgroundReplacementType();
        for (int i = 0; i < gpuEffectTypes.size(); i++) {
            if (portraitbackgroundreplacementType == gpuEffectTypes.get(i) && i != 0 && i < imgGridView.getChildCount()) {
                // SPRD: add for bug 610478 monkey test cause getChildAt(i) return a null object
                try{
                    ((ImageView)imgGridView.getChildAt(i)).setImageResource(mPortraitBackgroundReplacementSelectedImage.get(i));
                    ((ImageView)imgGridView.getChildAt(0)).setImageResource(mPortraitBackgroundReplacementImage.get(0));
                    mPreposition = i;
                    setPortraitBackgroundReplacementType(portraitbackgroundreplacementType);
                } catch (NullPointerException e) {
                    Log.e(TAG, "setDreamPortraitBackgroundReplacementType: i = " + i + "; msg:" +  e.getMessage());
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

    public void initEffectPhotoTypes(SparseIntArray effectTypes, int startIndex) {
        effectTypes.put(startIndex++, NonePortraitBacgroundReplacement);
        effectTypes.put(startIndex++, MonePortraitBacgroundReplacement);
        effectTypes.put(startIndex++, UrbanPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, RelaxationPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, NightPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, BeachPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, PalacePortraitBacgroundReplacement);

    }
    public void initEffectVideoTypes(SparseIntArray effectTypes, int startIndex) {
        effectTypes.put(startIndex++, NonePortraitBacgroundReplacement);
        effectTypes.put(startIndex++, MonePortraitBacgroundReplacement);
        effectTypes.put(startIndex++, UrbanPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, RelaxationPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, NightPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, BeachPortraitBacgroundReplacement);
        effectTypes.put(startIndex++, PalacePortraitBacgroundReplacement);
        effectTypes.put(startIndex++, MotionPortraitBacgroundReplacement);

    }

    public void initPhotoRes(ArrayList<String> textName, ArrayList<Integer> mPortraitBackgroundReplacementImage, ArrayList<Integer> mPortraitBackgroundReplacementSelectedImage, boolean SupRealPreviewThum) {

        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_none));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_color_retention));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_urban));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_leisure));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_nightscape));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_beach));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_palace));


        if (!SupRealPreviewThum) {
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_none);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_mono);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_urban);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_relaxation);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_night);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_beach);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_palace);


            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_none_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_mono_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_urban_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_relaxation_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_night_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_beach_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_palace_selected);

        }

    }
    public void initVideoRes(ArrayList<String> textName, ArrayList<Integer> mPortraitBackgroundReplacementImage, ArrayList<Integer> mPortraitBackgroundReplacementSelectedImage, boolean SupRealPreviewThum) {

        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_none));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_color_retention));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_urban));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_leisure));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_nightscape));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_beach));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_palace));
        textName.add(mActivity.getResources().getString(R.string.portrait_background_replacement_name_motion));


        if (!SupRealPreviewThum) {
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_none);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_mono);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_urban);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_relaxation);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_night);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_beach);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_palace);
            mPortraitBackgroundReplacementImage.add(R.drawable.portrait_bg_edit_motion);



            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_none_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_mono_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_urban_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_relaxation_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_night_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_beach_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_palace_selected);
            mPortraitBackgroundReplacementSelectedImage.add(R.drawable.portrait_bg_edit_motion_selected);

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

class PortraitBackgroundReplacementImageView extends ImageView {
    public PortraitBackgroundReplacementImageView(Context context , int width , int height) {
        super(context);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public PortraitBackgroundReplacementImageView(Context context, int width , int height , AttributeSet attrs) {
        super(context, attrs);
        prepareToDraw(width , height);
        // TODO Auto-generated constructor stub
    }

    public PortraitBackgroundReplacementImageView(Context context, int width , int height , AttributeSet attrs, int defStyle) {
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
