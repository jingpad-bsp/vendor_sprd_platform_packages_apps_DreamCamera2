package com.dream.camera.filter.sprd;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import com.android.camera.debug.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.android.camera.CameraActivity;
import com.android.camera.util.CameraUtil;
import com.dream.camera.filter.FilterSurfaceViewInterface;
import com.dream.camera.modules.filter.DreamFilterArcControlInterface;
import com.dream.camera.modules.filter.DreamFilterModuleController;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;
import com.android.camera2.R;
import com.sprd.imagefilter.SprdImageFilterEngine.ImageFilterType;

public class SprdGLSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener, FilterSurfaceViewInterface {
    private static final Log.Tag TAG = new Log.Tag("SprdGLSurfaceView");
    private SprdRenderer m_Renderer = null;
    private SurfaceTexture m_SurfaceTexture = null;
    private int m_OESTextureID = -1;
    private CameraActivity mActivity = null;
    private DreamFilterArcControlInterface mUI = null;
    private int mFrameCount = 0;

    private int m_sensorOrientation = 90;
    private boolean m_isFacingFront = false;

    public SprdGLSurfaceView(Context context, DreamFilterArcControlInterface control) {
        super(context);
        mActivity = (CameraActivity) context;
        mUI = control;
        if (!supportOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        m_sensorOrientation = getSensorOrientation(context , mActivity.getCameraId());
        m_isFacingFront = getSensorFacingFront(context , mActivity.getCameraId());
        Log.i(TAG , "sensorOrientation = " + m_sensorOrientation + ", sensorFacingFront = " + m_isFacingFront);
    }

    public SprdGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!supportOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        m_sensorOrientation = getSensorOrientation(context , mActivity.getCameraId());
        Log.i(TAG , "sensorOrientation = " + m_sensorOrientation + ", sensorFacingFront = " + m_isFacingFront);
    }

    private boolean supportOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        if (configurationInfo == null)
            return false;
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }


    private int getSensorOrientation(final Context context , int cameraId) {
        Log.i(TAG , "filter effect cameraId = " + cameraId);
        CameraManager mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics props =
                    mCameraManager.getCameraCharacteristics(Integer.toString(cameraId));
            return props.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException ex) {
            Log.e(TAG , "cannot get correct camera characteristics!");
            return 90;
        }
    }

    public int getSensorOrientation() {
        return m_sensorOrientation;
    }

    private boolean getSensorFacingFront(final Context context , int cameraId) {
        //Log.i(TAG , "filter effect cameraId = " + cameraId);
        CameraManager mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics props =
                    mCameraManager.getCameraCharacteristics(Integer.toString(cameraId));
            return props.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT);
        } catch (CameraAccessException ex) {
            Log.e(TAG , "cannot get correct camera characteristics!");
            return false;
        }
    }

    public boolean getSensorFacingFront() { return m_isFacingFront;}

    public boolean init() {
        Log.d(TAG, "init");
        m_Renderer = new SprdRenderer(this, mUI);

        this.setPreserveEGLContextOnPause(true);
        this.setEGLContextClientVersion(2);
        this.setRenderer(m_Renderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        return true;
    }

    public void deinit() {
        Log.d(TAG, "deinit");
        GLES20.glDeleteTextures(1, new int[]{
                m_OESTextureID
        }, 0);

        if (m_SurfaceTexture != null) {
            m_SurfaceTexture.setOnFrameAvailableListener(null);
            m_SurfaceTexture.release();
            m_SurfaceTexture = null;
        }

        if (m_Renderer != null) {
            m_Renderer.deinit();
            m_Renderer = null;
        }
        mUI = null;
        mActivity = null;
    }

    public void setFilterType(int filterType) {
        if (m_Renderer != null) {
            m_Renderer.setFilterType(filterType);
        }
    }

    public void setCameraStartPreview(final Camera camera) {
        m_Renderer.runOnDraw(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "setCameraStartPreview");
                try {
                    camera.setPreviewTexture(m_SurfaceTexture);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void SetPreviewStarted(boolean bStart) {
        if (bStart) {
            mFrameCount = 0;
        }
        if (m_Renderer != null)
            m_Renderer.SetPreviewStarted(bStart);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.v(TAG, "onFrameAvailable");
        this.requestRender();
        if (mFrameCount <= DreamFilterModuleController.MAX_FRAME_COUNT_FOR_FILTER_ARC) {
            if (mActivity != null && mActivity.getCameraAppUI() != null) {
                mActivity.getCameraAppUI().onSurfaceTextureUpdated(mActivity.getCameraAppUI().getSurfaceTexture());
            }
            mFrameCount++;
        }
    }

    public SurfaceTexture createOESTexture() {
        Log.d(TAG, "createOESTexture");
        int[] tex = new int[1];

        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        m_OESTextureID = tex[0];

        if (m_SurfaceTexture != null)
            m_SurfaceTexture.release();

        m_SurfaceTexture = new SurfaceTexture(m_OESTextureID);
        m_SurfaceTexture.setOnFrameAvailableListener(this);

        return m_SurfaceTexture;
    }

    public int getOESTextureID() {
        return m_OESTextureID;
    }

    public SurfaceTexture getSurfaceTexture() {
        return m_SurfaceTexture;
    }

    public void setGridMode(boolean gridMode) {
        if (m_Renderer != null)
            m_Renderer.setGridMode(gridMode);
    }

    public void setGridModeFilters(List<Integer> filters, List<String> texts) {
        if (m_Renderer != null)
            m_Renderer.setGridModeFilters(filters, texts);
    }

    public Bitmap getPreviewData() {
        if (m_Renderer != null) {
            return m_Renderer.getPreviewData();
        }
        return null;
    }

    public void initFiltersTable(SparseArray<int[]> tables, int index) {
        if (CameraUtil.isUseSprdFilterPlus())
            tables.put(index,
                    new int[]{
                            ImageFilterType.NoneFilter,
                            ImageFilterType.LightFilter,
                            ImageFilterType.WaterFilter,
                            ImageFilterType.HaloFilter,
                            ImageFilterType.SoftWarmingFilter,
                            ImageFilterType.EarlybirdFilter,
                            ImageFilterType.Filmstock50Filter,
                            ImageFilterType.HorrorBlueFilter,
                            ImageFilterType.NashvilleFilter,
                            ImageFilterType.CrispWinterFilter,
                            ImageFilterType.BismuthFilter,
                            ImageFilterType.WarmLBA75Filter,
                            ImageFilterType.HistoryFilter
                    });
        else
            tables.put(index,
                    new int[]{ImageFilterType.NoneFilter,
                            ImageFilterType.SoftWarmingFilter,
                            ImageFilterType.EarlybirdFilter,
                            ImageFilterType.Filmstock50Filter,
                            ImageFilterType.HorrorBlueFilter,
                            ImageFilterType.NashvilleFilter,
                            ImageFilterType.CrispWinterFilter,
                            ImageFilterType.BismuthFilter,
                            ImageFilterType.WarmLBA75Filter,
                            ImageFilterType.HistoryFilter
                    });
    }

    public void initEffectTypes(SparseIntArray effectTypes, int startIndex) {
        effectTypes.put(startIndex++, ImageFilterType.NoneFilter);
        if (CameraUtil.isUseSprdFilterPlus()) {
            effectTypes.put(startIndex++, ImageFilterType.LightFilter);
            effectTypes.put(startIndex++, ImageFilterType.WaterFilter);
            effectTypes.put(startIndex++, ImageFilterType.HaloFilter);
        }
        effectTypes.put(startIndex++, ImageFilterType.SoftWarmingFilter);
        effectTypes.put(startIndex++, ImageFilterType.EarlybirdFilter);
        effectTypes.put(startIndex++, ImageFilterType.Filmstock50Filter);
        effectTypes.put(startIndex++, ImageFilterType.HorrorBlueFilter);
        effectTypes.put(startIndex++, ImageFilterType.NashvilleFilter);
        effectTypes.put(startIndex++, ImageFilterType.CrispWinterFilter);
        effectTypes.put(startIndex++, ImageFilterType.BismuthFilter);
        effectTypes.put(startIndex++, ImageFilterType.WarmLBA75Filter);
        effectTypes.put(startIndex++, ImageFilterType.HistoryFilter);
    }

    public void initRes(ArrayList<String> textName, ArrayList<Integer> mFilterImage, ArrayList<Integer> mFilterSelectedImage, boolean SupRealPreviewThum) {

        textName.add(mActivity.getResources().getString(R.string.filter_name_normal));
        if (CameraUtil.isUseSprdFilterPlus()) {
            textName.add(mActivity.getResources().getString(R.string.filter_name_light));
            textName.add(mActivity.getResources().getString(R.string.filter_name_water));
            textName.add(mActivity.getResources().getString(R.string.filter_name_halo));
        }
        textName.add(mActivity.getResources().getString(R.string.filter_name_soft_warming));
        textName.add(mActivity.getResources().getString(R.string.filter_name_early_bird));
        textName.add(mActivity.getResources().getString(R.string.filter_name_film_stock));
        textName.add(mActivity.getResources().getString(R.string.filter_name_horror_blue));
        textName.add(mActivity.getResources().getString(R.string.filter_name_nashville));
        textName.add(mActivity.getResources().getString(R.string.filter_name_crisp_winter));
        textName.add(mActivity.getResources().getString(R.string.filter_name_bismuth));
        textName.add(mActivity.getResources().getString(R.string.filter_name_warm));
        textName.add(mActivity.getResources().getString(R.string.filter_name_history));

        if (!SupRealPreviewThum) {
            mFilterImage.add(R.drawable.ic_filter_preview_original_unisoc);
            if (CameraUtil.isUseSprdFilterPlus()) {
                mFilterImage.add(R.drawable.ic_filter_preview_light_unisoc);
                mFilterImage.add(R.drawable.ic_filter_preview_water_unisoc);
                mFilterImage.add(R.drawable.ic_filter_preview_halo_unisoc);
            }
            mFilterImage.add(R.drawable.ic_filter_preview_soft_warming_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_early_bird_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_filmstock50_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_horror_blue_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_nashville_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_crisp_winter_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_bismuth_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_warm_unisoc);
            mFilterImage.add(R.drawable.ic_filter_preview_history_unisoc);

            mFilterSelectedImage.add(R.drawable.ic_filter_preview_original_selected_unisoc);
            if (CameraUtil.isUseSprdFilterPlus()) {
                mFilterSelectedImage.add(R.drawable.ic_filter_preview_light_selected_unisoc);
                mFilterSelectedImage.add(R.drawable.ic_filter_preview_water_selected_unisoc);
                mFilterSelectedImage.add(R.drawable.ic_filter_preview_halo_selected_unisoc);
            }
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_soft_warming_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_early_bird_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_filmstock50_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_horror_blue_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_nashville_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_crisp_winter_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_bismuth_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_warm_selected_unisoc);
            mFilterSelectedImage.add(R.drawable.ic_filter_preview_history_selected_unisoc);
        }
    }
}
