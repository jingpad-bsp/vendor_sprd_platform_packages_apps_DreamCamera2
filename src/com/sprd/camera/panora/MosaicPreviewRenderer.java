/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sprd.camera.panora;

import android.graphics.SurfaceTexture;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.camera.SurfaceTextureRenderer;
import com.android.camera.MosaicRenderer;
import com.android.camera.util.CameraUtil;

import javax.microedition.khronos.opengles.GL10;

public class MosaicPreviewRenderer {

    @SuppressWarnings("unused")
    private static final String TAG = "CAM_MosaicPreviewRenderer";

    private int mWidth; // width of the view in UI
    private int mHeight; // height of the view in UI
    private float mViewPortX; // small preview start coordinate x
    private float mViewPortY; // small preview start coordinate y
    private int mFactor; // small preview scale factor

    private boolean mIsLandscape = true;
    private final float[] mTransformMatrix = new float[16];

    private ConditionVariable mEglThreadBlockVar = new ConditionVariable();
    private HandlerThread mEglThread;
    private MyHandler mHandler;
    private SurfaceTextureRenderer mSTRenderer;

    private SurfaceTexture mInputSurfaceTexture;

    private class MyHandler extends Handler {
        public static final int MSG_INIT_SYNC = 0;
        public static final int MSG_SHOW_PREVIEW_FRAME_SYNC = 1;
        public static final int MSG_SHOW_PREVIEW_FRAME = 2;
        public static final int MSG_ALIGN_FRAME_SYNC = 3;
        public static final int MSG_RELEASE = 4;

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_SYNC:
                    doInit();
                    mEglThreadBlockVar.open();
                    break;
                case MSG_SHOW_PREVIEW_FRAME_SYNC:
                    doShowPreviewFrame();
                    mEglThreadBlockVar.open();
                    break;
                case MSG_SHOW_PREVIEW_FRAME:
                    doShowPreviewFrame();
                    break;
                case MSG_ALIGN_FRAME_SYNC:
                    doAlignFrame();
                    mEglThreadBlockVar.open();
                    break;
                case MSG_RELEASE:
                    doRelease();
                    mEglThreadBlockVar.open();
                    break;
            }
        }

        private void doAlignFrame() {
            mInputSurfaceTexture.updateTexImage();
            mInputSurfaceTexture.getTransformMatrix(mTransformMatrix);

            /* SPRD: fix bug 246307 start @{ */
            //MosaicRenderer.setWarping(true);
            MosaicRenderer.setWarping(false);
            /* fix bug 246307 end @} */
            // Call preprocess to render it to low-res and high-res RGB textures.
            MosaicRenderer.preprocess(mTransformMatrix);
            // Now, transfer the textures from GPU to CPU memory for processing
            if (CameraUtil.isNewWideAngle()) {
                
            } else {
                MosaicRenderer.transferGPUtoCPU();
            }
            MosaicRenderer.updateMatrix();
            MosaicRenderer.step();
        }

        private void doShowPreviewFrame() {
            mInputSurfaceTexture.updateTexImage();
            mInputSurfaceTexture.getTransformMatrix(mTransformMatrix);

            MosaicRenderer.setWarping(false);
            // Call preprocess to render it to low-res and high-res RGB textures.
            MosaicRenderer.preprocess(mTransformMatrix);
            MosaicRenderer.updateMatrix();
            MosaicRenderer.step();
        }

        private void doInit() {

            /*** SPRD bug289431 ***/
            if ((mWidth == 0) || (mHeight == 0)) {
                return;
            }
            /*** SPRD bug289431 ***/

            mInputSurfaceTexture = new SurfaceTexture(MosaicRenderer.init());
            MosaicRenderer.reset(mWidth, mHeight, mIsLandscape);
        }

        private void doRelease() {
            releaseSurfaceTexture(mInputSurfaceTexture);
            mEglThread.quit();
        }

        private void releaseSurfaceTexture(SurfaceTexture st) {
            st.release();
        }

        // Should be called from other thread.
        public void sendMessageSync(int msg) {
            mEglThreadBlockVar.close();
            sendEmptyMessage(msg);
            mEglThreadBlockVar.block();
        }
    }

    /**
     * Constructor.
     *
     * @param tex The {@link SurfaceTexture} for the final UI output.
     * @param w The width of the UI view.
     * @param h The height of the UI view.
     * @param isLandscape The UI orientation. {@code true} if in landscape,
     *                    false if in portrait.
     */
    public MosaicPreviewRenderer(SurfaceTexture tex, int w, int h, boolean isLandscape) {
        mIsLandscape = isLandscape;

        mEglThread = new HandlerThread("PanoramaRealtimeRenderer");
        mEglThread.start();
        mHandler = new MyHandler(mEglThread.getLooper());
        mWidth = w;
        mHeight = h;

        SurfaceTextureRenderer.FrameDrawer dummy = new SurfaceTextureRenderer.FrameDrawer() {
            @Override
            public void onDrawFrame(GL10 gl) {
                // nothing, we have our draw functions.
            }
        };
        mSTRenderer = new SurfaceTextureRenderer(tex, mHandler, dummy);

        // We need to sync this because the generation of surface texture for input is
        // done here and the client will continue with the assumption that the
        // generation is completed.
        mHandler.sendMessageSync(MyHandler.MSG_INIT_SYNC);
    }

    public void release() {
        mSTRenderer.release();
        mHandler.sendMessageSync(MyHandler.MSG_RELEASE);
    }

    public void showPreviewFrameSync() {
        mHandler.sendMessageSync(MyHandler.MSG_SHOW_PREVIEW_FRAME_SYNC);
        mSTRenderer.draw(true);
    }

    public void showPreviewFrame() {
        mHandler.sendEmptyMessage(MyHandler.MSG_SHOW_PREVIEW_FRAME);
        mSTRenderer.draw(false);
    }

    public void alignFrameSync() {
        mHandler.sendMessageSync(MyHandler.MSG_ALIGN_FRAME_SYNC);
        mSTRenderer.draw(true);
    }

    public SurfaceTexture getInputSurfaceTexture() {
        return mInputSurfaceTexture;
    }

    public void setViewPort(float viewPortX, float viewPortY, int factor, float factor2, float factor3, int orientation, int director) {
        mViewPortX = viewPortX;
        mViewPortY = viewPortY;
        mFactor = factor;
        MosaicRenderer.setViewPort(mViewPortX, mViewPortY, mFactor, factor2, factor3, orientation, director);
    }

    public void hideSmallView(boolean hide) {
        MosaicRenderer.hideSmallView(hide);
    }

    public static void setCaptureViewPort(float viewPortX, float viewPortY, int width, int height, boolean land) {
        MosaicRenderer.setCaptureViewPort(viewPortX, viewPortY, width, height, land);
    }
}
