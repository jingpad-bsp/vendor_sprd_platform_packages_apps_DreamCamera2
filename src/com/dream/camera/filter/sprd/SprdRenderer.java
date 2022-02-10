package com.dream.camera.filter.sprd;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.android.camera.debug.Log;

import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.dream.camera.modules.filter.DreamFilterArcControlInterface;
import com.sprd.imagefilter.*;


public class SprdRenderer implements GLSurfaceView.Renderer {
    private static final Log.Tag TAG = new Log.Tag("SprdRenderer");
    private SprdGLSurfaceView m_GLSurfaceView = null;
    private SprdImageFilterEngine m_Engine = null;
    private SurfaceTexture m_SurfaceTexture = null;
    private int m_FilterType = SprdImageFilterEngine.ImageFilterType.NoneFilter;
    private boolean m_GridMode = false;
    private int m_OutputWidth = 0;
    private int m_OutputHeight = 0;
    private float[] m_SurfaceTextureMatrix = new float[16];
    private final Queue<Runnable> m_RunOnDraw;
    private int m_onDrawCount = 0;
    private List<Integer> m_FilterList = null;
    private List<String> m_FilterTextList = null;
    private boolean mRenderOnceFlg = false;
    private Bitmap mBitmap = null;
    private DreamFilterArcControlInterface mUI = null;
    private Object sync = new Object();
    private final long WAIT_TIMEOUT_MS = 3000;
    private final long WAIT_INTERVAL_MS = 20;
    private boolean m_bPreviewStarted = false;


    public SprdRenderer(SprdGLSurfaceView glSurfaceView, DreamFilterArcControlInterface control) {
        m_GLSurfaceView = glSurfaceView;
        m_Engine = SprdImageFilterEngine.getEngine();
        m_Engine.setGridModeGrid(3);
        m_Engine.setGridModeMargin(new Rect(10, 10, 10, 10));
        m_Engine.setGridModePadding(10, 10);
        m_Engine.setFontMargin(20, 10);
        m_RunOnDraw = new LinkedList<Runnable>();
        mUI = control;
        m_Engine.setSensorOrientation(m_GLSurfaceView.getSensorOrientation());
        m_Engine.setIsFacingFront(m_GLSurfaceView.getSensorFacingFront());
    }

    public void deinit() {
        Log.d(TAG, "deinit");

        if (m_Engine != null) {
            m_Engine.releaseEngine();
            m_Engine = null;
        }

        m_GLSurfaceView = null;
        m_SurfaceTexture = null;
        m_bPreviewStarted = false;
        mUI = null;
    }

    public void SetPreviewStarted(boolean bStart) {
        m_bPreviewStarted = bStart;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        m_SurfaceTexture = m_GLSurfaceView.createOESTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged, width = " + width + " height = " + height);
        if (mUI != null) {
            mUI.onSurfaceTextureAvailableForFilter(m_SurfaceTexture, width, height);
        }
        m_OutputWidth = width;
        m_OutputHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.v(TAG, String.format("onDrawFrame %d", m_onDrawCount++));

        if (m_SurfaceTexture != null) {
            m_SurfaceTexture.updateTexImage();
            m_SurfaceTexture.getTransformMatrix(m_SurfaceTextureMatrix);
        }

        /* SPRD:fix bug836183 show preview data of last frame @{ */
        if (!m_bPreviewStarted) {
            Log.e(TAG, "wait preview start");
            if (m_Engine != null) {
                int temType = m_FilterType;
                if (temType == SprdImageFilterEngine.ImageFilterType.NoneFilter) {
                    temType = SprdImageFilterEngine.ImageFilterType.CalciteFilter; //SPRD: fix Bug1467255
                }
                m_Engine.setFilterType(temType);//SPRD:fix bug1413941
            }
            return;
        }
        /* @} */

        if (m_Engine != null) {

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            synchronized (this) {
                if (m_GridMode && m_FilterList != null && m_FilterTextList != null) {
                    m_Engine.draw(m_FilterList, m_FilterTextList, m_GLSurfaceView.getOESTextureID(), m_OutputWidth, m_OutputHeight, m_SurfaceTextureMatrix);
                } else {
                    m_Engine.draw(m_FilterType, m_GLSurfaceView.getOESTextureID(), m_OutputWidth, m_OutputHeight, m_SurfaceTextureMatrix);
                }
                if (mRenderOnceFlg) {
                    initializePreviewBitmap();
                    synchronized (sync) {
                        mRenderOnceFlg = false;
                        sync.notifyAll();
                    }
                }
            }
        }
    }

    public void runOnDraw(final Runnable runnable) {
        synchronized (m_RunOnDraw) {
            m_RunOnDraw.add(runnable);
        }
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                Runnable runnable = queue.poll();
                if (runnable != null)
                    runnable.run();
            }
        }
    }

    public void setFilterType(int filterType) {
        Log.d(TAG, "setFilterType filterType = " + filterType);
        synchronized (this) {
            m_FilterType = filterType;
        }
    }

    public void setGridMode(boolean gridMode) {
        Log.d(TAG, "setGridMode");
        synchronized (this) {
            m_GridMode = gridMode;
        }
    }

    public void setGridModeFilters(List<Integer> filters, List<String> texts) {
        Log.d(TAG, "setGridModeFilters");
        synchronized (this) {
            m_FilterList = filters;
            m_FilterTextList = texts;
        }
    }
    private void initializePreviewBitmap() {
        int x = 0;
        int y = 0;
        int w = m_OutputWidth;
        int h = m_OutputHeight;
        int b[] = new int[w * (y + h)];
        int bt[] = new int[w * h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(x, 0, w, y + h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);//@see android.opengl.GLES20

        for (int i = 0, k = 0; i < h; i++, k++) {
            // OpenGLES bitmap is incompatible with Android bitmap
            // and so, some corrections need to be done.
            for (int j = 0; j < w; j++) {
                int pix = b[i * w + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(h - k - 1) * w + j] = pix1;
            }
        }
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mBitmap = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
    }
    public Bitmap getPreviewData() {
        /*
         * render once to trigger preview bitmap initialization which should be done
         * in process of TsAdvancedFilterNativeRender.onDrawFrame @{
         */
        mRenderOnceFlg = true;
        m_GLSurfaceView.requestRender();
        /* @} */

        long startMs = System.currentTimeMillis();
        synchronized (sync) {
            while (mRenderOnceFlg) {
                try {
                    sync.wait(WAIT_INTERVAL_MS);

                    if (System.currentTimeMillis() - startMs > WAIT_TIMEOUT_MS) {
                        Log.w(TAG, "Timeout waiting");
                        break;
                    }
                } catch (InterruptedException ex) {
                }
            }
        }
        Log.i(TAG, "getPreviewData cost: " + (System.currentTimeMillis() - startMs));
        return mBitmap;
    }
}
