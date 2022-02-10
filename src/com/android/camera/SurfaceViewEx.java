/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 *
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.Size;

import java.io.ByteArrayOutputStream;

import com.dream.camera.ucam.utils.LogUtils;

public class SurfaceViewEx extends SurfaceView {

    private final Log.Tag TAG = new Log.Tag("SurfaceViewEx");

    private byte[] mYUVBuffer;
    private float mAspectRatio = 0;
    private SurfaceHolder mSurfaceHolder;
    private CameraAppUI mCameraAppUI = null;
    private SurfaceHolder.Callback mSurfaceHolderListener;
    private CameraAgent.CameraProxy mCameraProxy;


    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "surfaceChanged: " + holder + " " + width + " " + height);
            if (mSurfaceHolderListener != null) {
                mSurfaceHolderListener.surfaceChanged(holder, format, width, height);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated: " + holder);
            mSurfaceHolder = holder;
            if (mSurfaceHolderListener != null) {
                mSurfaceHolderListener.surfaceCreated(holder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "surfaceDestroyed: " + holder);
            mSurfaceHolder = null;
            if (mSurfaceHolderListener != null) {
                mSurfaceHolderListener.surfaceDestroyed(holder);
            }
        }
    };

    public SurfaceViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(mSurfaceCallback);
    }

    public void setCameraAppUI(CameraAppUI cameraAppUI) {
        mCameraAppUI = cameraAppUI;
    }

    public void setSurfaceHolderListener(SurfaceHolder.Callback surfaceHolderListener) {
        mSurfaceHolderListener = surfaceHolderListener;
        /* SPRD: fix bug847583 should reset surfaceholder when pause @{ */
        if (surfaceHolderListener == null) {
            mSurfaceHolder = null;
        }
        /* @} */
    }

    // do not use main thread, or getPreviewBitmap() will return wrong result because of timeout
    public void onPreviewUpdated(byte[] data, CameraAgent.CameraProxy camera) {
        mCameraProxy = camera;

        if (mYUVBuffer == null || (mYUVBuffer.length != data.length)) {
            mYUVBuffer = new byte[data.length];
        }
        System.arraycopy(data, 0, mYUVBuffer, 0, data.length);

        /*if (mWaitingYUVBufferReady) {
            synchronized (this) {
                mWaitingYUVBufferReady = false;
                this.notifyAll();
            }
        }*/
    }

    public SurfaceHolder getSurfaceHolder() {
        Log.i(TAG, "mSurfaceHolder =" + mSurfaceHolder);
        return mSurfaceHolder;
    }

    public PreviewStatusListener.PreviewAreaChangedListener getPreviewAreaChangedListener() {
        return mPreviewAreaChangedListener;
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
    }

    private PreviewStatusListener.PreviewAreaChangedListener mPreviewAreaChangedListener =
            new PreviewStatusListener.PreviewAreaChangedListener() {

                @Override
                public void onPreviewAreaChanged(RectF previewArea) {
                    setTransformMatrix(Math.round(previewArea.width()),
                            Math.round((int) previewArea.height()));
                }

                private void setTransformMatrix(int previewWidth, int previewHeight) {
                    float scaledTextureWidth, scaledTextureHeight;
                    if (mAspectRatio == 0 || mCameraAppUI == null) {
                        return;
                    }
                    if (previewWidth > previewHeight) {
                        scaledTextureWidth = Math.min(previewWidth,
                                (int) (previewHeight * mAspectRatio));
                        scaledTextureHeight = Math.min(previewHeight,
                                (int) (previewWidth / mAspectRatio));
                    } else {
                        scaledTextureWidth = Math.min(previewWidth,
                                (int) (previewHeight / mAspectRatio));
                        scaledTextureHeight = Math.min(previewHeight,
                                (int) (previewWidth * mAspectRatio));
                    }
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
                    params.width = (int) scaledTextureWidth;
                    params.height = (int) scaledTextureHeight;
                    RectF rect = mCameraAppUI.getPreviewArea();
                    // horizontal direction
                    params.setMargins((int) rect.left, (int) rect.top, 0, 0);

                    setLayoutParams(params);

                    Log.i(TAG, "setTransformMatrix(): width = " + previewWidth
                            + " height = " + previewHeight
                            + " scaledTextureWidth = " + scaledTextureWidth
                            + " scaledTextureHeight = " + scaledTextureHeight
                            + " mAspectRatio = " + mAspectRatio);
                }
            };

    public Bitmap getPreviewBitmap() {
        long start = System.currentTimeMillis();

        if (mCameraProxy == null) {
            return null;
        }

        if (mYUVBuffer == null || mYUVBuffer.length == 0) {
            return null;
        }
        /*mWaitingYUVBufferReady = true;
        synchronized (this) {
            while (mWaitingYUVBufferReady) {
                try {
                    this.wait(WAIT_INTERVAL_MS);

                    if (System.currentTimeMillis() - start > WAIT_TIMEOUT_MS) {
                        Log.w(TAG, "Timeout waiting");
                        break;
                    }
                } catch (InterruptedException ex) {
                }
            }

            if (mWaitingYUVBufferReady) {
                return null;
            }
        }*/

        Size previewSize = mCameraProxy.getSettings().getCurrentPreviewSize();
        int width = previewSize.width();
        int height = previewSize.height();

        int sensorOrientation = mCameraProxy.getCharacteristics().getSensorOrientation();
        int cameraId = mCameraProxy.getCameraId();

        Bitmap bitmap = null;
        synchronized (this) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(mYUVBuffer.length);
            YuvImage yuvImage = new YuvImage(mYUVBuffer, ImageFormat.NV21, width, height, null);
            try {
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, os);
                Bitmap originalBitmap = BitmapFactory.decodeByteArray(os.toByteArray(), 0,
                        os.toByteArray().length);
                if (sensorOrientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(sensorOrientation);
                    matrix.postScale((cameraId == 1) ? -1 : 1, 1);
                    bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true);
                    originalBitmap.recycle();
                }
            } catch (Exception e) {
                Log.i(TAG, "catch exception " + e.getMessage());
            }
        }

        long end = System.currentTimeMillis();
        if ((end - start) > 500) {
            Log.i(TAG, "getBitmap cost: " + (end - start) + " previewSize " + previewSize);
        }
        return bitmap;
    }

}
