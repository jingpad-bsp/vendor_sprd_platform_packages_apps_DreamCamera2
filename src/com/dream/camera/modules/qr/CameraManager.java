/*
 * Copyright (C) 2008 ZXing authors
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

package com.dream.camera.modules.qr;

import java.io.IOException;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import com.android.camera2.R;
import com.android.camera.app.AppController;
import android.os.HandlerThread;
import android.hardware.Camera;
import com.android.camera.debug.Log;

import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 */
public final class CameraManager {

    private static CameraManager cameraManager;
    private static final Log.Tag TAG = new Log.Tag("QrcodeCameraManager");


    static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
    static {
        int sdkInt;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException nfe) {
            // Just to be safe
            sdkInt = 10000;
        }
        SDK_INT = sdkInt;
    }

    private final Context context;
    private static CameraConfigurationManager configManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    //private boolean initialized;
    //private final boolean useOneShotPreviewCallback;
    private boolean mPaused;
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    //private final PreviewCallback previewCallback;
    /** Autofocus callbacks arrive here, and are dispatched to the Handler which requested them. */
    private AutoFocusCallback autoFocusCallback;
    private CameraProxy camera;
    private PreviewCallback previewCallback;

    /**
     * Initializes this static object with the Context of the calling Activity.
     *
     * @param context The Activity which wants to use the camera.
     */
    public static void init(Context context) {
        if (cameraManager == null) {
            cameraManager = new CameraManager(context);
        }
    }

    /**
     * Gets the CameraManager singleton instance.
     *
     * @return A reference to the CameraManager singleton.
     */
    public static CameraManager get() {
        return cameraManager;
    }

    private CameraManager(Context context) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);

        // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
        // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
        // the more efficient one shot callback, as the older one can swamp the system and cause it
        // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
        // useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) >
        // Build.VERSION_CODES.CUPCAKE;
        //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake

        //previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
        previewCallback = new PreviewCallback();
        autoFocusCallback = new AutoFocusCallback();
    }

    public static CameraConfigurationManager getConfigManager(){
        return configManager;
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public void openDriver(CameraProxy cameraProxy){
        camera = cameraProxy;
    }

    public void requestPreviewFrame(Handler handler, int message) {
        previewCallback.setHandler(handler, message);
        camera.setPreviewDataCallback(handler, previewCallback);
    }

    //SPRD: Fix bug 881427 add api2 support
    private static class PreviewCallback implements CameraAgent.CameraPreviewDataCallback {

        private Handler previewHandler;
        private int previewMessage;

        void setHandler(Handler previewHandler, int previewMessage) {
            this.previewHandler = previewHandler;
            this.previewMessage = previewMessage;
        }

        public void onPreviewFrame(byte[] data, CameraProxy camera) {
            Point cameraResolution = configManager.getCameraResolution();
            if (previewHandler != null ) {
                Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
                        cameraResolution.y, data);
                message.sendToTarget();
                previewHandler = null;
            } else {
                Log.v(TAG, "Got preview callback, but no handler for it");
            }
        }

    }

    public void requestAutoFocus(Handler handler, int message) {
        autoFocusCallback.setHandler(handler, message);
        autoFocus(autoFocusCallback,handler);
    }

    public void autoFocus(AutoFocusCallback autoFocusCallback, Handler handler) {
        if (camera == null) {
            return;
        }
        camera.autoFocus(handler, autoFocusCallback);
    }

    private static class AutoFocusCallback implements CameraAFCallback {
        private static final long AUTOFOCUS_INTERVAL_MS = 1500L;
        private Handler autoFocusHandler;
        private int autoFocusMessage;

        void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
            this.autoFocusHandler = autoFocusHandler;
            this.autoFocusMessage = autoFocusMessage;
        }
        @Override
        public void onAutoFocus(boolean success, CameraProxy camera) {
            if (autoFocusHandler != null) {
                Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
                autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
                autoFocusHandler = null;
            } else {
                Log.d(TAG, "Got auto-focus callback, but no handler for it");
            }
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device far
     * enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public Rect getFramingRect() {
        Point screenResolution = configManager.getScreenResolution();
        if(screenResolution == null){
            return null;
        }
        //int width = context.getResources().getDimensionPixelSize(R.dimen.qrcode_framingRect_width);
        //int height = context.getResources().getDimensionPixelSize(R.dimen.qrcode_framingRect_height);
        int width = screenResolution.x * 3 / 4;
        int height = width;
        int leftOffset = (screenResolution.x - width) / 2;
        int topOffset = (screenResolution.y - height) / 2;
        framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        return framingRect;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame, not UI /
     * screen.
     */
    public Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect rect = new Rect(getFramingRect());
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    /**
     * Converts the result points from still resolution coordinates to screen coordinates.
     *
     * @param points The points returned by the Reader subclass through Result.getResultPoints().
     * @return An array of Points scaled to the size of the framing rect and offset appropriately so
     *         they can be drawn in screen coordinates.
     */

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format of the
     * preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        int previewFormat = configManager.getPreviewFormat();
        String previewFormatString = configManager.getPreviewFormatString();
        switch (previewFormat) {
        // This is the standard Android format which all devices are REQUIRED to support.
        // In theory, it's the only one we should ever care about.
            case PixelFormat.YCbCr_420_SP:
                // This format has never been seen in the wild, but is compatible as we only care
                // about the Y channel, so allow it.
            case PixelFormat.YCbCr_422_SP:
                return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                        rect.width(), rect.height());
            default:
                // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
                // Fortunately, it too has all the Y data up front, so we can read it.
                if ("yuv420p".equals(previewFormatString)) {
                    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                            rect.width(), rect.height());
                }
        }
        throw new IllegalArgumentException("Unsupported picture format: " +
                previewFormat + '/' + previewFormatString);
    }

    public Context getContext() {
        return context;
    }

    /* SPRD:fix bug 672207 should close camera after paused @{ */
    protected void setPaused(boolean pause) {
        mPaused = pause;
    }
    /* @} */
}
