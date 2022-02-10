package com.dream.camera.modules.rangefind;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.ButtonManager;
import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.ui.LineView;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.ToastUtil;
import com.android.camera2.R;
import com.dream.camera.ButtonManagerDream;
import com.dream.camera.DreamUI;
import com.dream.camera.SlidePanelManager;
import com.dream.camera.dreambasemodules.DreamInterface;
import com.dream.camera.util.DreamUtil;

import java.util.ArrayList;

public class TDRangeFindModuleUI extends DreamUI implements
        PreviewStatusListener, DreamInterface {

    private final static Log.Tag TAG = new Log.Tag("TDRangFindModuleUI");
    private CameraActivity mActivity;
    private TDRangeFindModuleController mController;
    private ViewGroup mRootView;
    private LineView mLinView = null;
    private View mPreviewBorder;
    private ImageView mRangeFindPoint_1 = null;
    private ImageView mRangeFindPoint_2 = null;
    private TextView mRangeFindDistanceText = null;
    private boolean mDistanceCalculating = false;

    private boolean mRangeFindPoint_1_Touching = false;
    private boolean mRangeFindPoint_2_Touching = false;
    private String mUnitMeter3D = "CM";

    private Integer[] mLastPoints = new Integer[2/*coordinate dimension*/ * 2/*point count*/];
    private boolean mSurfaceTextureUpdated = false;
    private int mPreviewAreaLeft;
    private int mPreviewAreaTop;
    private int mPreviewAreaRight;
    private int mPreviewAreaBottom;

    private Matrix mPreviewAreaToNativePreviewMatrix = new Matrix();

    private static final int CHECK_INTERVAL = 3000;

    public TDRangeFindModuleUI(
            CameraActivity activity,
            TDRangeFindModuleController controller,
            ViewGroup root) {
        mActivity = activity;
        mController = controller;
        mRootView = root;
        mUnitMeter3D = mActivity.getResources().getString(R.string.td_range_find_distance_des);
        prepareModuleLayout();

        activity.getCameraAppUI().setDreamInterface(this);

        initUI();
    }

    private void prepareModuleLayout() {
        LayoutInflater inflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup moduleRoot = (ViewGroup) mRootView.findViewById(R.id.module_layout);
        inflater.inflate(R.layout.td_range_find_module, moduleRoot, true);
        mLinView = (LineView) moduleRoot.findViewById(R.id.range_find_line_view);
        mPreviewBorder = moduleRoot.findViewById(R.id.preview_area_border);
        mRangeFindDistanceText = (TextView) moduleRoot.findViewById(R.id.range_find_distance_text);
        mRangeFindPoint_1 = (ImageView) moduleRoot.findViewById(R.id.range_find_point_1);
        mRangeFindPoint_2 = (ImageView) moduleRoot.findViewById(R.id.range_find_point_2);

        mRangeFindPoint_1.setOnTouchListener(mRangePointsTouchListener);
        mRangeFindPoint_2.setOnTouchListener(mRangePointsTouchListener);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Log.e(TAG, " onSurfaceTextureAvailable");
        mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        mController.onPreviewUIDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if (!mSurfaceTextureUpdated) {
            mSurfaceTextureUpdated = true;

            RectF previewAreaRectF = mActivity.getCameraAppUI().getPreviewArea();

            mPreviewAreaLeft = (int) previewAreaRectF.left;
            mPreviewAreaTop = (int) previewAreaRectF.top;
            mPreviewAreaRight = (int) previewAreaRectF.right;
            mPreviewAreaBottom = (int) previewAreaRectF.bottom;

            initPreviewCoordinateTransformer();

            drawRangeFindLine();
        }
    }

    public void initPreviewCoordinateTransformer() {
        RectF nativePreviewAreaRectF = mController.getNativePreviewArea();
        if (nativePreviewAreaRectF == null) {
            return;
        }

        RectF previewAreaRectF = mActivity.getCameraAppUI().getPreviewArea();
        previewAreaRectF.offset(-previewAreaRectF.left, -previewAreaRectF.top);

        mPreviewAreaToNativePreviewMatrix.setRectToRect(previewAreaRectF
                , nativePreviewAreaRectF, Matrix.ScaleToFit.FILL);
    }

    private void drawRangeFindLine() {
        mLinView.setPoints(getRangeFindPointsPosition());
    }

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right,
                                       int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        drawRangeFindLine();
    }

    private View.OnTouchListener mRangePointsTouchListener = new View.OnTouchListener() {

        private MotionEvent mMotionEvent_1;
        private MotionEvent mMotionEvent_2;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mActivity == null || mRangeFindDistanceText == null
                    || mRangeFindPoint_1 == null || mRangeFindPoint_2 == null) {
                return false;
            }

            if (mDistanceCalculating) {
                CameraUtil.toastHint(mActivity, R.string.td_range_find_please_wait);
            }

            int action = motionEvent.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (view.getId() == R.id.range_find_point_1) {
                        mRangeFindPoint_1_Touching = true;
                        mMotionEvent_1 = motionEvent.copy();
                    } else {
                        mRangeFindPoint_2_Touching = true;
                        mMotionEvent_2 = motionEvent.copy();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    mRangeFindDistanceText.setVisibility(View.INVISIBLE);

                    if (view.getId() == R.id.range_find_point_1) {
                        //float pointY = view.getY() + motionEvent.getY() - (float) mRangeFindPoint_1.getHeight() / 2;
                        float pointY = view.getY() + (motionEvent.getY() - mMotionEvent_1.getY());
                        float maxPointY = mPreviewAreaBottom - (float) mRangeFindPoint_1.getHeight() / 2;
                        float minPointY = mPreviewAreaTop - (float) mRangeFindPoint_1.getHeight() / 2;
                        mRangeFindPoint_1.setY(CameraUtil.clamp(pointY, minPointY, maxPointY));

                        //float pointX = view.getX() + motionEvent.getX() - (float) mRangeFindPoint_1.getWidth() / 2;
                        float pointX = view.getX() + (motionEvent.getX() - mMotionEvent_1.getX());
                        float maxPointX = mPreviewAreaRight - (float) mRangeFindPoint_1.getWidth() / 2;
                        float minPointX = mPreviewAreaLeft - (float) mRangeFindPoint_1.getWidth() / 2;
                        mRangeFindPoint_1.setX(CameraUtil.clamp(pointX, minPointX, maxPointX));
                    } else {
                        //float pointY = view.getY() + motionEvent.getY() - (float) mRangeFindPoint_2.getHeight() / 2;
                        float pointY = view.getY() + (motionEvent.getY() - mMotionEvent_2.getY());
                        float maxPointY = mPreviewAreaBottom - (float) mRangeFindPoint_2.getHeight() / 2;
                        float minPointY = mPreviewAreaTop - (float) mRangeFindPoint_2.getHeight() / 2;
                        mRangeFindPoint_2.setY(CameraUtil.clamp(pointY, minPointY, maxPointY));

                        //float pointX = view.getX() + motionEvent.getX() - (float) mRangeFindPoint_2.getWidth() / 2;
                        float pointX = view.getX() + (motionEvent.getX() - mMotionEvent_2.getX());
                        float maxPointX = mPreviewAreaRight - (float) mRangeFindPoint_2.getWidth() / 2;
                        float minPointX = mPreviewAreaLeft - (float) mRangeFindPoint_2.getWidth() / 2;
                        mRangeFindPoint_2.setX(CameraUtil.clamp(pointX, minPointX, maxPointX));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (view.getId() == R.id.range_find_point_1) {
                        mRangeFindPoint_1_Touching = false;
                        mMotionEvent_1.recycle();
                    } else {
                        mMotionEvent_2.recycle();
                        mRangeFindPoint_2_Touching = false;
                    }
                    handleDistanceMeasurement();
                    break;
            }
            drawRangeFindLine();
            return false;
        }
    };

    public void onResume() {
        CameraUtil.toastHint(mActivity, R.string.td_range_find_between_two_points);
        hideSetting();

        // only show mode list button in bottom bar
        mActivity.getCameraAppUI().showModeListBtnOnly(true);
    }

    public void onPause() {
        resetDistanceMeasurement();
        mActivity.getModuleLayoutRoot().findViewById(R.id.settings_button)
                .setVisibility(View.VISIBLE);

        // reset all button visibility in bottom bar
        mActivity.getCameraAppUI().showModeListBtnOnly(false);

        ToastUtil.cancelToast();
    }

    private void hideSetting() {
        mActivity.getModuleLayoutRoot().findViewById(R.id.settings_button)
                .setVisibility(View.INVISIBLE);
        ((ModeListView) mActivity.findViewById(R.id.mode_list_layout))
                .setShouldShowSettingsCling(false);
    }

    // done in main thread
    public void on3DRangeFindDistanceReceived(final int resultCode, final double distance) {
        if (!mDistanceCalculating) {
            Log.w(TAG, "should not be here");
        }
        Log.i(TAG, "onRangeFindDistanceReceived " + resultCode + " " + distance);
        String text = null;
        String textNote = null;
        switch (resultCode) {

            case 1: {
                textNote = mActivity.getResources().getString(R.string.td_range_find_distance_note);
            }
            case 0: {
                text = mActivity.getResources().getString(R.string.td_range_find_distance,
                        String.format("%.2f", distance) + mUnitMeter3D);
                if (!TextUtils.isEmpty(textNote)) {
                    text = text + '\n' + textNote;
                }
                break;
            }
            case -1: {
                text = mActivity.getResources().getString(R.string.td_range_find_fail);
            }
            default:
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreviewBorder.setVisibility(View.VISIBLE);
                    }
                });
                break;
        }

        final String tips = text;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.cancelToast();

                mRangeFindDistanceText.setText(tips);
                mRangeFindDistanceText.setVisibility(View.VISIBLE);
            }
        });

        resetDistanceMeasurement();
    }

    private void on3DRangeFindPointsSet(Integer[] points) {
        Log.i(TAG, "on3DRangeFindPointsSet: " + TextUtils.join(",", points));

        mPreviewBorder.setVisibility(View.INVISIBLE);

        mController.on3DRangeFindPointsSet(points);
        mMainHandler.sendEmptyMessageDelayed(0, CHECK_INTERVAL);

        CameraUtil.toastHint(mActivity, R.string.td_range_find_please_wait);
    }

    private void handleDistanceMeasurement() {
        if (!mRangeFindPoint_1_Touching && !mRangeFindPoint_2_Touching) {
            if (mDistanceCalculating) {
                resetRangeFindPointsPosition(mLastPoints);
                return;
            }
            mDistanceCalculating = true;
            mLastPoints = getRangeFindPointsPosition();
            on3DRangeFindPointsSet(convertToNative(mLastPoints));
        }
    }

    private void resetDistanceMeasurement() {
        if (mMainHandler.hasMessages(0)) {
            mMainHandler.removeMessages(0);
        }

        mDistanceCalculating = false;
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            resetDistanceMeasurement();

            CameraUtil.toastHint(mActivity, R.string.td_range_find_timeout);
        }
    };

    private Integer[] getRangeFindPointsPosition() {
        ArrayList<Integer> points = new ArrayList<>();
        points.add((int) mRangeFindPoint_1.getX() + mRangeFindPoint_1.getMeasuredWidth() / 2);
        points.add((int) mRangeFindPoint_1.getY() + mRangeFindPoint_1.getMeasuredHeight() / 2);
        points.add((int) mRangeFindPoint_2.getX() + mRangeFindPoint_2.getMeasuredWidth() / 2);
        points.add((int) mRangeFindPoint_2.getY() + mRangeFindPoint_2.getMeasuredHeight() / 2);
        return points.toArray(new Integer[0]);
    }

    private Integer[] convertToNative(Integer[] rangeFindPointsPosition) {
        if (mPreviewAreaToNativePreviewMatrix == null) {
            Log.w(TAG, "convertToNative should not be here");
            return rangeFindPointsPosition;
        }

        ArrayList<Integer> points = new ArrayList<>();
        int x1 = rangeFindPointsPosition[0];
        int y1 = rangeFindPointsPosition[1] - mPreviewAreaTop;
        int x2 = rangeFindPointsPosition[2];
        int y2 = rangeFindPointsPosition[3] - mPreviewAreaTop;

        RectF point = new RectF();
        mPreviewAreaToNativePreviewMatrix.mapRect(point, new RectF(0, 0, x1, y1));
        points.add((int) point.right);
        points.add((int) point.bottom);

        mPreviewAreaToNativePreviewMatrix.mapRect(point, new RectF(0, 0, x2, y2));
        points.add((int) point.right);
        points.add((int) point.bottom);

        return points.toArray(new Integer[0]);
    }

    private void resetRangeFindPointsPosition(Integer[] rangeFindPoints) {
        if (rangeFindPoints == null) {
            return;
        }
        mRangeFindPoint_1.setX(rangeFindPoints[0] - mRangeFindPoint_1.getMeasuredWidth() / 2);
        mRangeFindPoint_1.setY(rangeFindPoints[1] - mRangeFindPoint_1.getMeasuredHeight() / 2);
        mRangeFindPoint_2.setX(rangeFindPoints[2] - mRangeFindPoint_2.getMeasuredWidth() / 2);
        mRangeFindPoint_2.setY(rangeFindPoints[3] - mRangeFindPoint_2.getMeasuredHeight() / 2);
    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return null;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    @Override
    public void onPreviewFlipped() {
    }

    private View topPanel;

    private void initUI() {
        ViewGroup topPanelParent = (ViewGroup) mRootView.findViewById(R.id.top_panel_parent);
        if (topPanelParent != null) {
            fitTopPanel(topPanelParent);
        }

        updateTopPanelValue(mActivity);

        ViewGroup extendPanelParent = (ViewGroup) mRootView.findViewById(R.id.extend_panel_parent);
        if (extendPanelParent != null) {
            extendPanelParent.removeAllViews();
        }

        updateBottomPanel();
        updateSlidePanel();
    }

    @Override
    public void fitTopPanel(ViewGroup topPanelParent) {
        topPanelParent.removeAllViews();
        if (topPanel == null) {
            LayoutInflater lf = LayoutInflater.from(mActivity);

            topPanel = lf.inflate(R.layout.rangefind_top_panel, topPanelParent);
        }
        mActivity.getButtonManager().load(topPanel);
        bindCameraButton();
    }

    private void bindCameraButton() {
        ButtonManagerDream buttonManager = (ButtonManagerDream) mActivity
                .getButtonManager();
        buttonManager.initializeButton(ButtonManagerDream.BUTTON_CAMERA_DREAM,
                new ButtonManager.ButtonCallback() {
                    @Override
                    public void onStateChanged(int state) {
                        mActivity.switchFrontAndBackMode();
                        mActivity.getCameraAppUI().updateModeList();
                    }
                });
    }

    @Override
    public void fitExtendPanel(ViewGroup extendPanelParent) {

    }

    @Override
    public void updateBottomPanel() {

    }

    @Override
    public void updateSlidePanel() {
        SlidePanelManager.getInstance(mActivity).udpateSlidePanelShow(
                SlidePanelManager.SETTINGS, View.INVISIBLE);
        SlidePanelManager.getInstance(mActivity).focusItem(
                SlidePanelManager.CAPTURE, false);
    }
}
