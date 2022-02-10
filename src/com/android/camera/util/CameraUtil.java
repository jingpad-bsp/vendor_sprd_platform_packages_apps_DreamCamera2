/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.camera.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.location.Location;
import android.media.AudioManager;
import android.media.EncoderCapabilities;
import android.media.EncoderCapabilities.VideoEncoderCap;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import com.android.camera.CameraActivity;
import com.android.camera.CameraDisabledException;
import com.android.camera.FatalErrorHandler;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraProvider;
import com.android.camera.debug.Log;
import com.android.camera.device.CameraId;
import com.android.camera.module.ModuleInfoResolve;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraModule;
import com.android.camera.one.OneCameraManager;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.settings.SettingsUtil;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.util.Counter;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataStructSetting;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;

//import java.util.Stack;

/**
 * Collection of utility functions used in this package.
 */
public class CameraUtil {
    private static final Log.Tag TAG = new Log.Tag("CameraUtil");

    private static class Singleton {
        private static final CameraUtil INSTANCE = new CameraUtil(
              AndroidContext.instance().get());
    }

    /**
     * Thread safe CameraUtil instance.
     */
    public static CameraUtil instance() {
        return Singleton.INSTANCE;
    }

    // For calculate the best fps range for still image capture.
    private final static int MAX_PREVIEW_FPS_TIMES_1000 = 400000;
    private final static int MIN_PREVIEW_FPS_TIMES_1000 = 0;
    private final static int PREFERRED_PREVIEW_FPS_TIMES_1000 = 30000;

    // For creating crop intents.
    public static final String KEY_RETURN_DATA = "return-data";
    public static final String KEY_SHOW_WHEN_LOCKED = "showWhenLocked";

    /** Orientation hysteresis amount used in rounding, in degrees. */
    public static final int ORIENTATION_HYSTERESIS = 5;

    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
    /** See android.hardware.Camera.ACTION_NEW_PICTURE. */
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    /** See android.hardware.Camera.ACTION_NEW_VIDEO. */
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";

    /**
     * Broadcast Action: The camera application has become active in
     * picture-taking mode.
     */
    public static final String ACTION_CAMERA_STARTED = "com.android.camera.action.CAMERA_STARTED";
    /**
     * Broadcast Action: The camera application is no longer in active
     * picture-taking mode.
     */
    public static final String ACTION_CAMERA_STOPPED = "com.android.camera.action.CAMERA_STOPPED";
    /**
     * When the camera application is active in picture-taking mode, it listens
     * for this intent, which upon receipt will trigger the shutter to capture a
     * new picture, as if the user had pressed the shutter button.
     */
    public static final String ACTION_CAMERA_SHUTTER_CLICK =
            "com.android.camera.action.SHUTTER_CLICK";

    // Fields for the show-on-maps-functionality
    private static final String MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final String MAPS_CLASS_NAME = "com.google.android.maps.MapsActivity";

    /** Has to be in sync with the receiving MovieActivity. */
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    public static final String KEY_DISABLE_FLOAT_WINDOW = "disable-float-window";

    /** Private intent extras. Test only. */
    private static final String EXTRAS_CAMERA_FACING =
            "android.intent.extras.CAMERA_FACING";

    //private final ImageFileNamer mImageFileNamer;SPRD:fix bug519299
    private static ImageFileNamer mImageFileNamer;
    public static final String KEY_IS_SECURE_CAMERA = "isSecureCamera";

    /* value of persist.sys.cam.wideangle & wideAngleVersion
     * 0: close this module
     * 1: google wideangle
     * 2: sprd wideangle
     * default: 2
     */
    private static int wideAngleVersion = 2;
    private final static String TARGET_CAMERA_WIDEANGLE_VERSION = "persist.sys.cam.wideangle";
    private static int mCurrentBeautyVersion = 0;
    private static boolean isBeautyAllFeature = false;
    private final static String TARGET_TS_BEAUTY_ALL_FEATYRE = "persist.sys.cam.beauty.fullfuc";
    public final static int ENABLE_SPRD_BEAUTY = 2;
    public final static int ENABLE_ARC_BEAUTY = 1;
    public final static int DISABLE_BEAUTY = 0;
    /**
     * TARGET_DRV_TEST is true, CameraActivity does not intercept KEYCODE_DPAD_CENTER and MainActivityLayout does not setDescendantFocusability
     */
    public final static String TARGET_DRV_TEST = "persist.sys.cam.drv.test";
    private static boolean isDrvTest = false;
    public static Toast toast;
    private static int mhint;
    private static int mBackBlurRefocusVersion = 0;
    private static int mFrontBlurRefocusVersion = 0;
    private static int mFilterVersion = -1;
    private static int mFilterPlus = -1;
    public static ModuleInfoResolve mModuleInfoResovle = new ModuleInfoResolve();
    private static Size size;
    private static Size realSize;
    private static final DisplayInfo mDisplayInfo = new DisplayInfo();
    // SPRD: Bug922759 close some feature when special sensor
    private static int frontSensorVersion = SensorVersion.NORMAL_SENSOR.ordinal();
    private static int backSensorVersion = SensorVersion.NORMAL_SENSOR.ordinal();

    /*add for w+t mode picture size threshold*/
    private static double mWPlusTThreshold = 8;

    private CameraUtil(Context context) {
        mImageFileNamer = new ImageFileNamer();
    }

    private static final int mZoomFourKMax = 4;

    /**
     * Rotates the bitmap by the specified degree. If a new bitmap is created,
     * the original bitmap is recycled.
     */
    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    /**
     * Rotates and/or mirrors the bitmap. If a new bitmap is created, the
     * original bitmap is recycled.
     */
    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees != 0 || mirror) && b != null) {
            Matrix m = new Matrix();
            // Mirror first.
            // horizontal flip + rotation = -rotation + horizontal flip
            if (mirror) {
                m.postScale(-1, 1);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate(b.getWidth(), 0);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate(b.getHeight(), 0);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }
            if (degrees != 0) {
                // clockwise
                m.postRotate(degrees,
                        (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            }

            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    /**
     * Compute the sample size as a function of minSideLength and
     * maxNumOfPixels. minSideLength is used to specify that minimal width or
     * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
     * pixels that is tolerable in terms of memory usage. The function returns a
     * sample size based on the constraints.
     * <p>
     * Both size and minSideLength can be passed in as -1 which indicates no
     * care of the corresponding constraint. The functions prefers returning a
     * sample size that generates a smaller bitmap, unless minSideLength = -1.
     * <p>
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way. For example,
     * BitmapFactory downsamples an image by 2 even though the request is 3. So
     * we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
                                                int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels < 0) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength < 0) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        } else if (minSideLength < 0) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
                    options);
            if (options.mCancel || options.outWidth == -1
                    || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(
                    options, -1, maxNumOfPixels);
            options.inJustDecodeBounds = false;

            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
                    options);
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Got oom exception ", ex);
            return null;
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }
    private static AlertDialog mAlertDialog;//SPRD:fix bug837695 window leak
    private static final Object mLock = new Object();
    /**
     * Shows custom error dialog. Designed specifically
     * for the scenario where the camera cannot be attached.
     * @deprecated Use {@link FatalErrorHandler} instead.
     */
    @Deprecated
    public static void showDialog(final Activity activity,final int dialogTitle, final int dialogMsgId, final int feedbackMsgId,
                                 final boolean finishActivity, final Exception ex) {
        final DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (finishActivity) {
                            mAlertDialog = null;
                            activity.finish();
                        }
                    }
                };

        DialogInterface.OnClickListener reportButtonListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new GoogleHelpHelper(activity).sendGoogleFeedback(feedbackMsgId, ex);
                        if (finishActivity) {
                            mAlertDialog = null;
                            activity.finish();
                        }
                    }
                };
        TypedValue out = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.alertDialogIcon, out, true);
        // Some crash reports indicate users leave app prior to this dialog
        // appearing, so check to ensure that the activity is not shutting down
        // before attempting to attach a dialog to the window manager.
        if (!activity.isFinishing()) {
            Log.e(TAG, "Show fatal error dialog");

            synchronized (mLock){
                if (mAlertDialog == null) {
                    mAlertDialog = new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle(dialogTitle)
                            .setMessage(dialogMsgId)
                            .setPositiveButton(R.string.dialog_dismiss, buttonListener)
                            //.setIcon(out.resourceId) Sprd:Fix bug 807826 dialog should not show icon
                            .show();
                } else if (mAlertDialog.isShowing()) {
                    return;
                } else {
                    mAlertDialog.setTitle(dialogTitle);
                    mAlertDialog.setMessage(activity.getText(dialogMsgId));
                    mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.dialog_dismiss);
                    mAlertDialog.show();
                }
            }
            /* @} */
        }
    }

    public static void release(){
        mAlertDialog = null;
    }

    public static <T> T checkNotNull(T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a == null ? false : a.equals(b));
    }

    public static int nextPowerOf2(int n) {
        // TODO: what happens if n is negative or already a power of 2?
        n -= 1;
        n |= n >>> 16;
        n |= n >>> 8;
        n |= n >>> 4;
        n |= n >>> 2;
        n |= n >>> 1;
        return n + 1;
    }

    public static float distance(float x, float y, float sx, float sy) {
        float dx = x - sx;
        float dy = y - sy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Clamps x to between min and max (inclusive on both ends, x = min --> min,
     * x = max --> max).
     */
    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * Clamps x to between min and max (inclusive on both ends, x = min --> min,
     * x = max --> max).
     */
    public static float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * Linear interpolation between a and b by the fraction t. t = 0 --> a, t =
     * 1 --> b.
     */
    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    /**
     * Given (nx, ny) \in [0, 1]^2, in the display's portrait coordinate system,
     * returns normalized sensor coordinates \in [0, 1]^2 depending on how the
     * sensor's orientation \in {0, 90, 180, 270}.
     * <p>
     * Returns null if sensorOrientation is not one of the above.
     * </p>
     */
    public static PointF normalizedSensorCoordsForNormalizedDisplayCoords(
            float nx, float ny, int sensorOrientation) {
        switch (sensorOrientation) {
            case 0:
                return new PointF(nx, ny);
            case 90:
                return new PointF(ny, 1.0f - nx);
            case 180:
                return new PointF(1.0f - nx, 1.0f - ny);
            case 270:
                return new PointF(1.0f - ny, nx);
            default:
                return null;
        }
    }

    /**
     * Given a size, return the largest size with the given aspectRatio that
     * maximally fits into the bounding rectangle of the original Size.
     *
     * @param size the original Size to crop
     * @param aspectRatio the target aspect ratio
     * @return the largest Size with the given aspect ratio that is smaller than
     *         or equal to the original Size.
     */
    public static Size constrainToAspectRatio(Size size, float aspectRatio) {
        float width = size.getWidth();
        float height = size.getHeight();

        float currentAspectRatio = width * 1.0f / height;

        if (currentAspectRatio > aspectRatio) {
            // chop longer side
            if (width > height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        } else if (currentAspectRatio < aspectRatio) {
            // chop shorter side
            if (width < height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        }

        return new Size((int) width, (int) height);
    }

    public static int getDisplayRotation() {
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        int rotation = windowManager.getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    // SPRD: Fix bug 605492 that video thumbnail is blurred, change private -> public
    public static Size getDefaultDisplaySize() {
        /*
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        Point res = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        return new Size(res);
        */
        return size;
    }
    public static Size getDefaultDisplayRealSize() {
        return realSize;
    }

    public static Size getOptimalPreviewSize(List<Size> sizes, double targetRatio) {
        int optimalPickIndex = getOptimalPreviewSizeIndex(sizes, targetRatio);
        if (optimalPickIndex == -1) {
            return null;
        } else {
            return sizes.get(optimalPickIndex);
        }
    }

    /**
     * Returns the index into 'sizes' that is most optimal given the current
     * screen and target aspect ratio..
     * <p>
     * This is using a default aspect ratio tolerance. If the tolerance is to be
     * given you should call
     * {@link #getOptimalPreviewSizeIndex(List, double, Double)}
     *
     * @param sizes the available preview sizes
     * @param targetRatio the target aspect ratio, typically the aspect ratio of
     *            the picture size
     * @return The index into 'previewSizes' for the optimal size, or -1, if no
     *         matching size was found.
     */
    public static int getOptimalPreviewSizeIndex(List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match. HTC 4:3
        // ratios is over .01 from true 4:3, so this value must be above .01,
        // see b/18241645.
        final double aspectRatioTolerance = 0.025;

        return getOptimalPreviewSizeIndex(sizes, targetRatio, aspectRatioTolerance);
    }

    /**
     * Returns the index into 'sizes' that is most optimal given the current
     * screen, target aspect ratio and tolerance.
     *
     * @param previewSizes the available preview sizes
     * @param targetRatio the target aspect ratio, typically the aspect ratio of
     *            the picture size
     * @param aspectRatioTolerance the tolerance we allow between the selected
     *            preview size's aspect ratio and the target ratio. If this is
     *            set to 'null', the default value is used.
     * @return The index into 'previewSizes' for the optimal size, or -1, if no
     *         matching size was found.
     */
    public static int getOptimalPreviewSizeIndex(
            List<Size> previewSizes, double targetRatio, Double aspectRatioTolerance) {
        if (previewSizes == null) {
            return -1;
        }

        // If no particular aspect ratio tolerance is set, use the default
        // value.
        if (aspectRatioTolerance == null) {
            return getOptimalPreviewSizeIndex(previewSizes, targetRatio);
        }

        int optimalSizeIndex = -1;
        double minDiff = Double.MAX_VALUE;
//        Stack<Integer> candicateSizes = new Stack<Integer>();

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Size defaultDisplaySize = getDefaultDisplaySize();
        int targetHeight = Math.min(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        // Try to find an size match aspect ratio and size
        for (int i = 0; i < previewSizes.size(); i++) {
            Size size = previewSizes.get(i);
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > aspectRatioTolerance) {
                continue;
            }

            double heightDiff = Math.abs(size.getHeight() - targetHeight);
            if (heightDiff < minDiff) {
                optimalSizeIndex = i;
                minDiff = heightDiff;
//                candicateSizes.push(new Integer(optimalSizeIndex));
//                Log.d(TAG, "push candicate size:"+previewSizes.get(optimalSizeIndex).getWidth()+"x"+previewSizes.get(optimalSizeIndex).getHeight());
            } else if (heightDiff == minDiff) {
                // Prefer resolutions smaller-than-display when an equally close
                // larger-than-display resolution is available
                if (size.getHeight() < targetHeight) {
                    optimalSizeIndex = i;
                    minDiff = heightDiff;
//                    candicateSizes.push(new Integer(optimalSizeIndex));
//                    Log.d(TAG, "push candicate size:"+previewSizes.get(optimalSizeIndex).getWidth()+"x"+previewSizes.get(optimalSizeIndex).getHeight());
                }
            }
        }

        /*SPRD: fix bug 460566 add for preview optimization @{*/
        /**
        int maxValue = Math.max(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        int minValue = Math.min(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        while (!candicateSizes.empty()) {
            int i = candicateSizes.pop();
            if (previewSizes.get(i).getWidth() < maxValue
                    || previewSizes.get(i).getHeight() < minValue) {
                Log.i(TAG, "candidate preview size:" + previewSizes.get(i).getWidth()
                        + "x" + previewSizes.get(i).getHeight() + " < display size:"
                        + defaultDisplaySize.getWidth() + "x"
                        + defaultDisplaySize.getHeight() + ", abandon it");
                continue;
            }
            optimalSizeIndex = i;
            Log.i(TAG,"select preview size:" + previewSizes.get(optimalSizeIndex).getWidth() + "x" + previewSizes.get(optimalSizeIndex).getHeight());
            android.util.Log.e("FF", "select preview size:" + previewSizes.get(optimalSizeIndex).getWidth() + "x" + previewSizes.get(optimalSizeIndex).getHeight());
            break;
        }
        */
        /* @}*/

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSizeIndex == -1) {
            Log.w(TAG, "No preview size match the aspect ratio. available sizes: " + previewSizes);
            minDiff = Double.MAX_VALUE;
            for (int i = 0; i < previewSizes.size(); i++) {
                Size size = previewSizes.get(i);
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSizeIndex = i;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }

        return optimalSizeIndex;
    }

    /**
     * Returns the largest picture size which matches the given aspect ratio,
     * except for the special WYSIWYG case where the picture size exactly
     * matches the target size.
     *
     * @param sizes a list of candidate sizes, available for use
     * @param targetWidth the ideal width of the video snapshot
     * @param targetHeight the ideal height of the video snapshot
     * @return the Optimal Video Snapshot Picture Size
     */
    public static Size getOptimalVideoSnapshotPictureSize(
            List<Size> sizes, int targetWidth,
            int targetHeight) {

        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;

        // WYSIWYG Override
        // We assume that physical display constraints have already been
        // imposed on the variables sizes
        for (Size size : sizes) {
            if (size.height() == targetHeight && size.width() == targetWidth) {
                return size;
            }
        }

        // Try to find a size matches aspect ratio and has the largest width
        final double targetRatio = (double) targetWidth / targetHeight;
        for (Size size : sizes) {
            double ratio = (double) size.width() / size.height();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (optimalSize == null || size.width() > optimalSize.width()) {
                optimalSize = size;
            }
        }

        // Cannot find one that matches the aspect ratio. This should not
        // happen. Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No picture size match the aspect ratio");
            for (Size size : sizes) {
                if (optimalSize == null || size.width() > optimalSize.width()) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }

    // This is for test only. Allow the camera to launch the specific camera.
    public static int getCameraFacingIntentExtras(Activity currentActivity) {
        int cameraId = -1;

        int intentCameraId =
                currentActivity.getIntent().getIntExtra(CameraUtil.EXTRAS_CAMERA_FACING, -1);

        if (isFrontCameraIntent(intentCameraId)) {
            // Check if the front camera exist
            int frontCameraId = ((CameraActivity) currentActivity).getCameraProvider()
                    .getFirstFrontCameraId();
            if (frontCameraId != -1) {
                cameraId = frontCameraId;
            }
        } else if (isBackCameraIntent(intentCameraId)) {
            // Check if the back camera exist
            int backCameraId = ((CameraActivity) currentActivity).getCameraProvider()
                    .getFirstBackCameraId();
            if (backCameraId != -1) {
                cameraId = backCameraId;
            }
        }
        return cameraId;
    }

    // SPRD: Fix bug 569496 front camera and slowmotion do mutex
    public static boolean isFrontCameraIntent(int intentCameraId) {
        return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private static boolean isBackCameraIntent(int intentCameraId) {
        return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private static int sLocation[] = new int[2];

    // This method is not thread-safe.
    public static boolean pointInView(float x, float y, View v) {
        v.getLocationInWindow(sLocation);
        return x >= sLocation[0] && x < (sLocation[0] + v.getWidth())
                && y >= sLocation[1] && y < (sLocation[1] + v.getHeight());
    }

    public static int[] getRelativeLocation(View reference, View view) {
        reference.getLocationInWindow(sLocation);
        int referenceX = sLocation[0];
        int referenceY = sLocation[1];
        view.getLocationInWindow(sLocation);
        sLocation[0] -= referenceX;
        sLocation[1] -= referenceY;
        return sLocation;
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null) {
            return false;
        }

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.e(TAG, "Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    public static void dumpRect(RectF rect, String msg,boolean logv) {
        if (logv)
            Log.v(TAG, msg + "=(" + rect.left + "," + rect.top
                + "," + rect.right + "," + rect.bottom + ")");
    }

    public static void inlineRectToRectF(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static Rect rectFToRect(RectF rectF) {
        Rect rect = new Rect();
        inlineRectToRectF(rectF, rect);
        return rect;
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
            int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

    public String createJpegName(long dateTaken) {
        synchronized (mImageFileNamer) {
            return mImageFileNamer.generateName(dateTaken);
        }
    }

    public String createJpegNameLarge(long dateTaken) {
        synchronized (mImageFileNamer) {
            return mImageFileNamer.generateNameLarge(dateTaken);
        }
    }

    public boolean isSameDateTaken(long dateTaken) {
        synchronized (mImageFileNamer) {
            return mImageFileNamer.isSameDate(dateTaken);
        }
    }

    public void setLastDate(long dateTaken) {
        synchronized (mImageFileNamer) {
            mImageFileNamer.setLastDate(dateTaken);
        }
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        // Keep compatibility
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static void fadeIn(View view, float startAlpha, float endAlpha, long duration) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(startAlpha, endAlpha);
        animation.setDuration(duration);
        view.startAnimation(animation);
    }

    public static void setGpsParameters(CameraSettings settings, Location loc) {
        // Clear previous GPS location from the parameters.
        settings.clearGpsData();

        boolean hasLatLon = false;
        double lat;
        double lon;
        // Set GPS location.
        if (loc != null) {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            hasLatLon = (lat != 0.0d) || (lon != 0.0d);
        }

        if (!hasLatLon) {
            // We always encode GpsTimeStamp even if the GPS location is not
            // available.
            settings.setGpsData(new CameraSettings
                    .GpsData(0f, 0f, 0f, System.currentTimeMillis() / 1000, null));
        } else {
            Log.d(TAG, "Set gps location");
            // for NETWORK_PROVIDER location provider, we may have
            // no altitude information, but the driver needs it, so
            // we fake one.
            // Location.getTime() is UTC in milliseconds.
            // gps-timestamp is UTC in seconds.
//            long utcTimeSeconds = loc.getTime() / 1000;
//            settings.setGpsData(new CameraSettings.GpsData(loc.getLatitude(), loc.getLongitude(),
//                    (loc.hasAltitude() ? loc.getAltitude() : 0),
//                    (utcTimeSeconds != 0 ? utcTimeSeconds : System.currentTimeMillis()),
//                    loc.getProvider().toUpperCase()));

            // gps-timestamp is UTC in milliseconds.
            long utcTimeMillis = loc.getTime();
            settings.setGpsData(new CameraSettings.GpsData(loc.getLatitude(), loc.getLongitude(),
                    (loc.hasAltitude() ? loc.getAltitude() : 0),
                    (utcTimeMillis != 0 ? utcTimeMillis : System.currentTimeMillis()),
                    loc.getProvider().toUpperCase()));

        }
    }

    /**
     * For still image capture, we need to get the right fps range such that the
     * camera can slow down the framerate to allow for less-noisy/dark
     * viewfinder output in dark conditions.
     *
     * @param capabilities Camera's capabilities.
     * @return null if no appropiate fps range can't be found. Otherwise, return
     *         the right range.
     */
    public static int[] getPhotoPreviewFpsRange(CameraCapabilities capabilities, boolean api2, boolean video) {
        return getPhotoPreviewFpsRange(capabilities.getSupportedPreviewFpsRange(), api2, video);
    }

    public static int[] getPhotoPreviewFpsRange(List<int[]> frameRates, boolean api2, boolean video) {
        if (frameRates.size() == 0) {
            Log.e(TAG, "No suppoted frame rates returned!");
            return null;
        }

        // Find the lowest min rate in supported ranges who can cover 30fps.
        int lowestMinRate = MAX_PREVIEW_FPS_TIMES_1000;
        int preferedFps = api2 ? PREFERRED_PREVIEW_FPS_TIMES_1000 / 1000 : PREFERRED_PREVIEW_FPS_TIMES_1000;
        int minPreFps = video ? api2 ? mPreferVideoMinFps : mPreferVideoMinFps * 1000 : MIN_PREVIEW_FPS_TIMES_1000;
        for (int[] rate : frameRates) {
            int minFps = rate[0];
            int maxFps = rate[1];
            if (maxFps >= preferedFps &&
                    minFps <= preferedFps &&
                    minFps >= minPreFps &&
                    minFps < lowestMinRate) {
                lowestMinRate = minFps;
            }
        }

        // Find all the modes with the lowest min rate found above, the pick the
        // one with highest max rate.
        int resultIndex = -1;
        int highestMaxRate = 0;
        for (int i = 0; i < frameRates.size(); i++) {
            int[] rate = frameRates.get(i);
            int minFps = rate[0];
            int maxFps = rate[1];
            if (minFps == lowestMinRate && highestMaxRate < maxFps) {
                highestMaxRate = maxFps;
                resultIndex = i;
            }
        }

        if (resultIndex >= 0) {
            return frameRates.get(resultIndex);
        }
        Log.e(TAG, "Can't find an appropiate frame rate range!");
        return null;
    }

    public static int[] getMaxPreviewFpsRange(List<int[]> frameRates) {
        if (frameRates != null && frameRates.size() > 0) {
            // The list is sorted. Return the last element.
            return frameRates.get(frameRates.size() - 1);
        }
        return new int[0];
    }

    public static void throwIfCameraDisabled() throws CameraDisabledException {
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = AndroidServices.instance().provideDevicePolicyManager();
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
    }

    /**
     * Generates a 1d Gaussian mask of the input array size, and store the mask
     * in the input array.
     *
     * @param mask empty array of size n, where n will be used as the size of
     *            the Gaussian mask, and the array will be populated with the
     *            values of the mask.
     */
    private static void getGaussianMask(float[] mask) {
        int len = mask.length;
        int mid = len / 2;
        float sigma = len;
        float sum = 0;
        for (int i = 0; i <= mid; i++) {
            float ex = (float) Math.exp(-(i - mid) * (i - mid) / (mid * mid))
                    / (2 * sigma * sigma);
            int symmetricIndex = len - 1 - i;
            mask[i] = ex;
            mask[symmetricIndex] = ex;
            sum += mask[i];
            if (i != symmetricIndex) {
                sum += mask[symmetricIndex];
            }
        }

        for (int i = 0; i < mask.length; i++) {
            mask[i] /= sum;
        }

    }

    /**
     * Add two pixels together where the second pixel will be applied with a
     * weight.
     *
     * @param pixel pixel color value of weight 1
     * @param newPixel second pixel color value where the weight will be applied
     * @param weight a float weight that will be applied to the second pixel
     *            color
     * @return the weighted addition of the two pixels
     */
    public static int addPixel(int pixel, int newPixel, float weight) {
        // TODO: scale weight to [0, 1024] to avoid casting to float and back to
        // int.
        int r = ((pixel & 0x00ff0000) + (int) ((newPixel & 0x00ff0000) * weight)) & 0x00ff0000;
        int g = ((pixel & 0x0000ff00) + (int) ((newPixel & 0x0000ff00) * weight)) & 0x0000ff00;
        int b = ((pixel & 0x000000ff) + (int) ((newPixel & 0x000000ff) * weight)) & 0x000000ff;
        return 0xff000000 | r | g | b;
    }

    /**
     * Apply blur to the input image represented in an array of colors and put
     * the output image, in the form of an array of colors, into the output
     * array.
     *
     * @param src source array of colors
     * @param out output array of colors after the blur
     * @param w width of the image
     * @param h height of the image
     * @param size size of the Gaussian blur mask
     */
    public static void blur(int[] src, int[] out, int w, int h, int size) {
        float[] k = new float[size];
        int off = size / 2;

        getGaussianMask(k);

        int[] tmp = new int[src.length];

        // Apply the 1d Gaussian mask horizontally to the image and put the
        // intermediat results in a temporary array.
        int rowPointer = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                for (int i = 0; i < k.length; i++) {
                    int dx = x + i - off;
                    dx = clamp(dx, 0, w - 1);
                    sum = addPixel(sum, src[rowPointer + dx], k[i]);
                }
                tmp[x + rowPointer] = sum;
            }
            rowPointer += w;
        }

        // Apply the 1d Gaussian mask vertically to the intermediate array, and
        // the final results will be stored in the output array.
        for (int x = 0; x < w; x++) {
            rowPointer = 0;
            for (int y = 0; y < h; y++) {
                int sum = 0;
                for (int i = 0; i < k.length; i++) {
                    int dy = y + i - off;
                    dy = clamp(dy, 0, h - 1);
                    sum = addPixel(sum, tmp[dy * w + x], k[i]);
                }
                out[x + rowPointer] = sum;
                rowPointer += w;
            }
        }
    }

    /**
     * Calculates a new dimension to fill the bound with the original aspect
     * ratio preserved.
     *
     * @param imageWidth The original width.
     * @param imageHeight The original height.
     * @param imageRotation The clockwise rotation in degrees of the image which
     *            the original dimension comes from.
     * @param boundWidth The width of the bound.
     * @param boundHeight The height of the bound.
     * @returns The final width/height stored in Point.x/Point.y to fill the
     *          bounds and preserve image aspect ratio.
     */
    public static Point resizeToFill(int imageWidth, int imageHeight, int imageRotation,
            int boundWidth, int boundHeight) {
        if (imageRotation % 180 != 0) {
            // Swap width and height.
            int savedWidth = imageWidth;
            imageWidth = imageHeight;
            imageHeight = savedWidth;
        }

        Point p = new Point();
        p.x = boundWidth;
        p.y = boundHeight;

        // In some cases like automated testing, image height/width may not be
        // loaded, to avoid divide by zero fall back to provided bounds.
        if (imageWidth != 0 && imageHeight != 0) {
            if (imageWidth * boundHeight > boundWidth * imageHeight) {
                p.y = imageHeight * p.x / imageWidth;
            } else {
                p.x = imageWidth * p.y / imageHeight;
            }
        } else {
            Log.w(TAG, "zero width/height, falling back to bounds (w|h|bw|bh):"
                    + imageWidth + "|" + imageHeight + "|" + boundWidth + "|"
                    + boundHeight);
        }

        return p;
    }

    public static Point resizeToFillFor16BitAl(int imageWidth, int imageHeight, int imageRotation,
            int boundWidth, int boundHeight) {
        if (imageRotation % 180 != 0) {
            // Swap width and height.
            int savedWidth = imageWidth;
            imageWidth = imageHeight;
            imageHeight = savedWidth;
        }

        Point p = new Point();
        p.x = boundWidth;
        p.y = boundHeight;

        // In some cases like automated testing, image height/width may not be
        // loaded, to avoid divide by zero fall back to provided bounds.
        int sampleSize = 1;
        int targetWidth = imageWidth;
        int targetHeight = imageHeight;
        if (imageWidth != 0 && imageHeight != 0) {
            while (targetHeight > boundHeight || targetWidth > boundWidth) {
                sampleSize <<= 1;
                targetWidth = imageWidth / sampleSize;
                targetHeight = imageWidth / sampleSize;
            }
            sampleSize = sampleSize > 1 ? (sampleSize >> 1) : 1;
            if (imageWidth * boundHeight > boundWidth * imageHeight) {
                p.x = imageWidth/sampleSize;
                p.y = imageHeight/sampleSize;
            } else {
                p.x = imageHeight/sampleSize;
                p.y = imageWidth/sampleSize;
            }
        } else {
            Log.w(TAG, "zero width/height, falling back to bounds (w|h|bw|bh):"
                    + imageWidth + "|" + imageHeight + "|" + boundWidth + "|"
                    + boundHeight);
        }

        return p;
    }

    private static class ImageFileNamer {

        // The date (in milliseconds) used to generate the last name.
        private static long mLastDate;

        // Number of names generated for the same second.
        private static int mSameSecondCount;

        public ImageFileNamer() {
        }

        public String generateName(long dateTaken) {
            SimpleDateFormat DateFormate = new SimpleDateFormat(AndroidContext.instance().get().getString(R.string.image_file_name_format));
            Date date = new Date(dateTaken);
            String result = DateFormate.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }

        public String generateNameLarge(long dateTaken) {
            SimpleDateFormat DateFormateLarge = new SimpleDateFormat(AndroidContext.instance().get().getString(R.string.image_file_name_format));
            Date date = new Date(dateTaken);
            String result = DateFormateLarge.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (mSameSecondCount != 0) {
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }

        public boolean isSameDate(long dateTaken){
            if (dateTaken / 1000 == mLastDate / 1000) {
                return true;
            }
            return false;
        }

        public static void setLastDate(long date) {
            mLastDate = date;
        }
    }

    public static void playVideo(CameraActivity activity, Uri uri, String title) {
        try {
            boolean isSecureCamera = activity.isSecureCamera();
            if (!isSecureCamera) {
                Intent intent = IntentHelper.getVideoPlayerIntent(uri)
                        .putExtra(Intent.EXTRA_TITLE, title)
                        .putExtra(KEY_DISABLE_FLOAT_WINDOW, true)
                        .putExtra(KEY_TREAT_UP_AS_BACK, true);
                activity.launchActivityByIntent(intent);
            } else {
                // In order not to send out any intent to be intercepted and
                // show the lock screen immediately, we just let the secure
                // camera activity finish.
                //activity.finish();

                /* SPRD: Fix bug 669233, user can watch video in SecureCamera mode @{ */
                Intent intent = IntentHelper.getVideoPlayerSecureIntent(uri,activity);
                if (intent != null) {
                    intent.putExtra(Intent.EXTRA_TITLE, title)
                          .putExtra(KEY_DISABLE_FLOAT_WINDOW, true)
                          .putExtra(KEY_IS_SECURE_CAMERA, true)
                          .putExtra(KEY_TREAT_UP_AS_BACK, true);
                    activity.launchActivityByIntent(intent);
                }
                /* @} */
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts GMM with the given location shown. If this fails, and GMM could
     * not be found, we use a geo intent as a fallback.
     *
     * @param activity the activity to use for launching the Maps intent.
     * @param latLong a 2-element array containing {latitude/longitude}.
     */
    public static void showOnMap(Activity activity, double[] latLong) {
        try {
            // We don't use "geo:latitude,longitude" because it only centers
            // the MapView to the specified location, but we need a marker
            // for further operations (routing to/from).
            // The q=(lat, lng) syntax is suggested by geo-team.
            String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?f=q&q=(%f,%f)",
                    latLong[0], latLong[1]);
            ComponentName compName = new ComponentName(MAPS_PACKAGE_NAME,
                    MAPS_CLASS_NAME);
            Intent mapsIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(uri)).setComponent(compName);
            mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            activity.startActivity(mapsIntent);
        } catch (ActivityNotFoundException e) {
            // Use the "geo intent" if no GMM is installed
            Log.e(TAG, "GMM activity not found!", e);
            String url = String.format(Locale.ENGLISH, "geo:%f,%f", latLong[0], latLong[1]);
            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(mapsIntent);
        }
    }

    /**
     * Dumps the stack trace.
     *
     * @param level How many levels of the stack are dumped. 0 means all.
     * @return A {@link java.lang.String} of all the output with newline between
     *         each.
     */
    public static String dumpStackTrace(int level) {
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        // Ignore the first 3 elements.
        level = (level == 0 ? elems.length : Math.min(level + 3, elems.length));
        String ret = new String();
        for (int i = 3; i < level; i++) {
            ret = ret + "\t" + elems[i].toString() + '\n';
        }
        return ret;
    }

    /**
     * Gets the theme color of a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return theme color of the mode if input index is valid, otherwise 0
     */
    public static int getCameraThemeColorId(int modeIndex, Context context) {

        // Find the theme color using id from the color array
        TypedArray colorRes = context.getResources()
                .obtainTypedArray(R.array.camera_mode_theme_color);
        if (modeIndex >= colorRes.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            colorRes.recycle();
            return 0;
        }
        int ret = colorRes.getResourceId(modeIndex, 0);
        colorRes.recycle();
        return ret;
    }

    /**
     * Gets the mode icon resource id of a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return icon resource id if the index is valid, otherwise 0
     */
//    public static int getCameraModeIconResId(int modeIndex, Context context) {
//        // Find the camera mode icon using id
//        TypedArray cameraModesIcons = context.getResources()
//                .obtainTypedArray(R.array.camera_mode_icon);
//        if (modeIndex >= cameraModesIcons.length() || modeIndex < 0) {
//            // Mode index not found
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            return 0;
//        }
//        return cameraModesIcons.getResourceId(modeIndex, 0);
//    }

    /**
     * Gets the mode text of a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return mode text if the index is valid, otherwise a new empty string
     */
//    public static String getCameraModeText(int modeIndex, Context context) {
//        // Find the camera mode icon using id
//        String[] cameraModesText = context.getResources()
//                .getStringArray(R.array.camera_mode_text);
//        if (modeIndex < 0 || modeIndex >= cameraModesText.length) {
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            return new String();
//        }
//        return cameraModesText[modeIndex];
//    }

    /**
     * Gets the mode content description of a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return mode content description if the index is valid, otherwise a new
     *         empty string
     */
//    public static String getCameraModeContentDescription(int modeIndex, Context context) {
//        String[] cameraModesDesc = context.getResources()
//                .getStringArray(R.array.camera_mode_content_description);
//        if (modeIndex < 0 || modeIndex >= cameraModesDesc.length) {
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            return new String();
//        }
//        return cameraModesDesc[modeIndex];
//    }

    /**
     * Gets the shutter icon res id for a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return mode shutter icon id if the index is valid, otherwise 0.
     */
//    public static int getCameraShutterIconId(int modeIndex, Context context) {
//        // Find the camera mode icon using id
//        TypedArray shutterIcons = context.getResources()
//                .obtainTypedArray(R.array.dream_camera_mode_shutter_icon);
//        if (modeIndex < 0 || modeIndex >= shutterIcons.length()) {
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            throw new IllegalStateException("Invalid mode index: " + modeIndex);
//        }
//        return shutterIcons.getResourceId(modeIndex, 0);
//    }

    /**
     * Gets the parent mode that hosts a specific mode in nav drawer.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return mode id if the index is valid, otherwise 0
     */
//    public static int getCameraModeParentModeId(int modeIndex, Context context) {
//        // Find the camera mode icon using id
//        int[] cameraModeParent = context.getResources()
//                .getIntArray(R.array.camera_mode_nested_in_nav_drawer);
//        if (modeIndex < 0 || modeIndex >= cameraModeParent.length) {
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            return 0;
//        }
//        return cameraModeParent[modeIndex];
//    }

    /**
     * Gets the mode cover icon resource id of a specific mode.
     *
     * @param modeIndex index of the mode
     * @param context current context
     * @return icon resource id if the index is valid, otherwise 0
     */
//    public static int getCameraModeCoverIconResId(int modeIndex, Context context) {
//        // Find the camera mode icon using id
//        TypedArray cameraModesIcons = context.getResources()
//                .obtainTypedArray(R.array.camera_mode_cover_icon);
//        if (modeIndex >= cameraModesIcons.length() || modeIndex < 0) {
//            // Mode index not found
//            Log.e(TAG, "Invalid mode index: " + modeIndex);
//            return 0;
//        }
//        return cameraModesIcons.getResourceId(modeIndex, 0);
//    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * <p>
     * Source: http://stackoverflow.com/questions/7962155/
     *
     * @return The number of cores, or 1 if failed to get result
     */
    public static int getNumCpuCores() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements java.io.FileFilter {
            @Override
            public boolean accept(java.io.File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (java.util.regex.Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            // Get directory containing CPU info
            java.io.File dir = new java.io.File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            java.io.File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            // Default to return 1 core
            Log.e(TAG, "Failed to count number of cores, defaulting to 1", e);
            return 1;
        }
    }

    /**
     * Given the device orientation and Camera2 characteristics, this returns
     * the required JPEG rotation for this camera.
     *
     * @param deviceOrientationDegrees the clockwise angle of the device orientation from its
     *                                 natural orientation in degrees.
     * @return The angle to rotate image clockwise in degrees. It should be 0, 90, 180, or 270.
     */
    public static int getJpegRotation(int deviceOrientationDegrees,
                                      CameraCharacteristics characteristics) {
        if (deviceOrientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }
        boolean isFrontCamera = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraMetadata.LENS_FACING_FRONT;
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return getImageRotation(sensorOrientation, deviceOrientationDegrees, isFrontCamera);
    }

    /**
     * Given the camera sensor orientation and device orientation, this returns a clockwise angle
     * which the final image needs to be rotated to be upright on the device screen.
     *
     * @param sensorOrientation Clockwise angle through which the output image needs to be rotated
     *                          to be upright on the device screen in its native orientation.
     * @param deviceOrientation Clockwise angle of the device orientation from its
     *                          native orientation when front camera faces user.
     * @param isFrontCamera True if the camera is front-facing.
     * @return The angle to rotate image clockwise in degrees. It should be 0, 90, 180, or 270.
     */
    public static int getImageRotation(int sensorOrientation,
                                       int deviceOrientation,
                                       boolean isFrontCamera) {
        // The sensor of front camera faces in the opposite direction from back camera.
        if (isFrontCamera) {
            deviceOrientation = (360 - deviceOrientation) % 360;
        }
        return (sensorOrientation + deviceOrientation) % 360;
    }

    /**
     * SPRD: add for new encoders
     */
    private static final List<VideoEncoderCap> videoEncoders = EncoderCapabilities
            .getVideoEncoders();
    private static ArrayList<Integer> mCodecs = new ArrayList<Integer>();
    public static final int H265 = 5;
    public static final int H264 = 2;
    public static final int MPEG4 = 3;
    public static final int H263 = 1;

    public static ArrayList<Integer> getVideoEncoders() {
        for (VideoEncoderCap cap : videoEncoders) {
            mCodecs.add(cap.mCodec);
            Log.i(TAG, "VideoEncoderCap mCodec = " + cap.mCodec);
        }
        return mCodecs;
    }

    public static boolean isSupportH265() {
        return getVideoEncoders().contains(H265);
    }
    public static boolean isSupportMPEG4() {
        return getVideoEncoders().contains(MPEG4);
    }
    public static boolean isSupportH264() {
        return getVideoEncoders().contains(H264);
    }
    public static boolean isSupportH263() { return getVideoEncoders().contains(H263);}
    /**
     * SPRD: fix bug 473462 add for burst capture
     */
    private static boolean isNinetynineBurstEnabled = false;
    private static boolean isZslBurstEnabled = false;
    private static boolean isZslEnable = false;
    private static boolean isFrontCameraMirrorEnable = false;
    private static boolean isColorEffectEnabled = false;
    private static boolean isContinuePhotoEnabled = false;
    private static boolean isIntervalPhotoEnabled = true;
    private static boolean isSlowMotionEnabled = false;
    private static boolean isTimelapseEnabled = false;
    private static boolean isHighISOEnable = false;
    private static boolean isTouchPhotoEnable = true;
    // SPRD: Fix bug 535110, Photo voice record.
    private static boolean isVoicePhotoEnabled = false;
    //SPRD:add for BurstMode album
    private static boolean isBurstAlbumEnabled = false;
    // SPRD: Fix bug 572309 camera GPS function
    private static boolean isRecordLocationEnabled = false;
    // SPRD: Add for bug 602877, select filter fps range
    private static boolean isFilterHighFpsEnable = false;
    private static boolean isFreezeDisplayEnabled = false;
    private static boolean isZoomPanelEnabled = false;
    private static boolean isAIDetectEnabled = false;
    private static int mLowBatteryNoFlashLevel = -1;
    private static boolean isManualShutterEnabled = true;
    private static boolean isManualFocusEnabled = true;
    private static boolean isFilterVideoEnable = true;
    private static int mSmileScoreThreshold = 0;
    private static boolean isVideoExternal = false;
    // SPRD: Fix bug 613015 add SurfaceView support
    private static boolean isSurfaceViewAlternativeEnabled = false;

    private static boolean isNavigationEnable = false;
    private static int mNavigationBarHeightPixel = 0;
    private static boolean isARPhotoEnabled = false;
    private static boolean isARVideoEnabled = false;
    private static boolean isUseGooglePhotos = true;
    private static boolean isMotionPhotoEnabled = false;
    private static boolean isFrontPortraitPhotoEnable = false;
    private static boolean isBackPortraitPhotoEnable = false;
    private static int portraitDefaultFNumber = 4;
    private static boolean isIRPhotoEnable = false;
    private static boolean isFDRPhotoEnable = false;
    private static boolean isFocusLengthFusionPhotoEnable = false;
    private static boolean isMacroPhotoEnable = false;
    private static boolean isMacroVideoEnable = false;
    private static boolean isPortraitBackgroundReplacementEnable = false;
	private static boolean isSRFusionEnable = false;
    private static boolean isFrontHighResolutionPhotoEnable = false;
    private static boolean isBackHighResolutionPhotoEnable = false;
    private static boolean is3DNRProEnable = false;
    private static boolean isLevelEnabled;
    private static boolean isLightPortraitEnable = false;
    private static boolean isLightPortraitFrontEnable = false;
    private static boolean isPortraitAndRefocusMutex = false;
    private static boolean isPortraitRefocusEnable = false;
    private static int autoToPortraitDefaultNum = 0;
    private static int portraitToAutoDefaultNum = 0;
    private static int autoTo3DNRDefaultNum = 0;
    private static int threeDNRToAutoDefaultNum = 0;

    private final static String TARGET_TS_AUTO_TO_PORTRAIT_DEFAULT_NUMBER = "persist.sys.cam.auto_to_portrait";
    private final static String TARGET_TS_PORTRAIT_TO_AUTO_DEFAULT_NUMBER = "persist.sys.cam.portrait_to_auto";
    private final static String TARGET_TS_AUTO_TO_3DNR_DEFAULT_NUMBER = "persist.sys.cam.auto_to_3dnr";
    private final static String TARGET_TS_3DNR_TOPO_AUTO_DEFAULT_NUMBER = "persist.sys.cam.3dnr_to_auto";

    private final static String TARGET_TS_BURST_NINETYNINE_ENABLE = "persist.sys.cam.ninetynine";
    private final static String TARGET_TS_ZSL_BURST_ENABLE = "persist.sys.cam.burst";
    private final static String TARGET_TS_ZSL_ENABLE = "persist.sys.cam.zsl";
    private final static String TARGET_TS_FRONT_CAMERA_MIRROR_ENABLE = "persist.front.camera.mirror";
    private final static String TARGET_COLOR_EFFECT_ENABLE = "persist.sys.cam.color";
    private final static String TARGET_CONTINUE_PHOTO_ENABLE = "persist.sys.cam.continue_photo";
    private final static String TARGET_INTERVAL_PHOTO_ENABLE = "persist.sys.cam.interval";
    private final static String TARGET_SLOW_MOTION_ENABLE = "persist.sys.cam.slow_motion";
    private final static String TARGET_TIME_LAPSE_ENABLE = "persist.sys.cam.time_lapse";
    private final static String TARGET_TS_HIGHISO = "persist.sys.cam.highiso";
    private final static String TARGET_IS_TOUCH_PHOTO = "persist.sys.cam.touchphoto";
    // SPRD: Fix bug 535110, Photo voice record.
    private final static String TARGET_CAMERA_VOICE_PHOTO_ENABLE = "persist.sys.cam.voicephoto";
    private final static String TARGET_CAMERA_BURST_ALBUM_ENABLE = "presist.sys.cam.burstalbum";
    // SPRD: Fix bug 572309 camera GPS function
    private final static String TARGET_RECORD_LOCATION_ENABLE = "persist.sys.cam.gps";
    private final static String TARGET_FREEZE_DISPLAY_ENABLE = "persist.sys.cam.freeze_display";
    private final static String TARGET_ZOOM_PANEL_ENABLE = "persist.sys.cam.zoom_panel";
    private final static String TARGET_AI_DETECT_ENABLE = "persist.sys.cam.ai_detect";
    private final static String TARGET_MANUAL_CONTROL_SHUTTER_ENABLE = "persist.sys.cam.manual.shutter";
    private final static String TARGET_MANUAL_CONTROL_FOCUS_ENABLE = "persist.sys.cam.manual.focus";
    private final static String TARGET_SMILE_THRESHOLD = "persist.sys.cam.smile.thr";
    private final static String TARGET_PORTRAIT_DEFAULT_FNUMBER = "persist.sys.cam.potrait.fnumber";
    private final static String TARGET_FILTER_VIDEO_ENABLE = "persist.sys.cam.filtervideo.enable";
    /* SPRD: Add for bug 594960, beauty video recoding @{ */
    private static boolean isMakeupVideoEnable = false;
    private static boolean isMakeup3DEnable = false;
    /* @} */

    /* SPRD: Add for bug 594960, beauty video recoding @{ */
    //private final static String TARGET_MAKE_UP_VIDEO = "persist.sys.cam.makeup.video";
    private final static String TARGET_MAKE_UP_3D = "persist.sys.cam.makeup.3d";
    /* @} */

    /* SPRD: New Feature EIS&OIS @{ */
    private static boolean isEOISDcBackEnabled = false;
    private static boolean isEOISDcFrontEnabled = false;
    private static boolean isEOISDvBackEnabled = false;
    private static boolean isEOISDvFrontEnabled = false;
    private final static String TARGET_IS_EOIS_DC_BACK_ENABLE = "persist.sys.cam.eois.dc.back";
    private final static String TARGET_IS_EOIS_DC_FRONT_ENABLE = "persist.sys.cam.eois.dc.front";
    private final static String TARGET_IS_EOIS_DV_BACK_ENABLE = "persist.sys.cam.eois.dv.back";
    private final static String TARGET_IS_EOIS_DV_FRONT_ENABLE = "persist.sys.cam.eois.dv.front";
    /* @} */
    /* SPRD: Fix bug 585183 Adds new features 3D recording @{ */
    private static boolean isTDVideoEnable;
    private final static String TARGET_TS_TDVideo_ENABLE = "persist.sys.ucam.tdvideo";
    public final static int TD_CAMERA_ID = 17;
    /* @} */
    /* SPRD: Fix 597206 Adds new features 3DPhoto @{ */
    private static boolean isTDPhotoEnable;
    private final static String TARGET_TS_TDPHOTO_ENABLE = "persist.sys.ucam.tdphoto";
    public final static int TD_PHOTO_ID = 19;
    public final static int SENSOR_SELF_SHOT_CAMERA_ID = 25;
    /* @} */
    public final static int BLUR_REFOCUS_PHOTO_ID = 24;
    public final static int FRONT_BLUR_REFOCUS_PHOTO_ID = 27;

    /*add for w+t mode*/
    public final static int BACK_W_PLUS_T_PHOTO_ID = 23;

    public final static int BACK_TRI_CAMERA_ID = 36;

    public final static int BACK_IR_CAMERA_ID = 2;
    public final static int BACK_MACRO_CAMERA_ID = 3;

    public final static int FRONT_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID = 52;
    public final static int BACK_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID = 53;
    public final static int BACK_PORTRAIT_ID = 38;

    public final static int BACK_HIGH_RESOLUTOIN_CAMERA_ID = 37;

    public final static int FRONT_HIGH_RESOLUTION_CAMERA_ID = 39;

    public final static int BACK_WT_FUSION_ID = 48;

    public final static int FRONT_PORTRAIT_ID = 42;

    /* SPRD: Fix bug 585183 Adds new feature real-time distance measurement @{ */
    private static boolean isTDRangeFindEnable;
    private final static String TARGET_TS_RANGEFIND_ENABLE = "persist.sys.ucam.tdrangefind";
    public final static int TDRANGFIND_CAMERA_ID = 18;
    /* @} */
    /*
     * fix bug 578797 New feature:QR code. @{
     */
    private static boolean isQrCodeEnabled = false;
    private final static String TARGET_IS_QRCODE_ENABLE = "persist.sys.cam.qrcode";
    /* @} */
    //SPRD : Add for bug 657472 Save normal hdr picture
    private static boolean isNormalHdrEnabled = false;
    private static boolean isHdrSupport = false;
    private static boolean isFdrSupport = false;
    //SPRD: Add for bug 948896 Add ai scene detect switcher
    private static boolean isAiSceneDetectSupport = false;
    private final static String TARGET_IS_NORMAL_HDR_ENABLE = "persist.sys.cam.normalhdr";
    private final static String TARGER_LOW_BATTERY_NO_FLASH = "persist.sys.cam.battery.flash";
    // SPRD: Add for bug 602877, select filter fps range
    private final static String TARGER_FILTER_HIGH_FPS = "persist.sys.cam.filter.highfps";

    private static boolean isSwitchAnimationEnable = false;
    private final static String TARGET_TS_SWITCH_ANIMATION_ENABLE = "persist.sys.cam.switch";

    private static boolean isFreezeBlurEnable = false;
    private final static String TARGET_TS_FREEZE_BLUR_ENABLE = "persist.sys.cam.blur";

    private static boolean isMSensorEnable = false;
    private final static String TARGET_MSENSOR_ENABLE = "persist.sys.cam.msensor";

    // SPRD: Fix bug 613015 add SurfaceView support
    private final static String TARGER_SURFACE_VIEW_ALTERNATIVE = "persist.sys.cam.sfv.alter";

    //SPRD:Feature 627281
    private final static String TARGET_TS_IDLE_SLEEP = "persist.sys.camera.idlesleep";
    private final static String TARGET_IS_LEVEL_ENABLE = "persist.sys.cam.level.enable";
    private static boolean isBurstModeEnable = false;
    private static boolean isSensorSelfShotEnable = false;
    private final static String TARGET_TS_BURST_MODE_ENABLE = "persist.sys.cam.burstmode";
    private final static String TARGET_TS_SENSOR_SELF_SHORT_ENABLE = "persist.sys.cam.covered.enable";
    public final static int BLUR_REFOCUS_VERSION_1 = 1;
    public final static int BLUR_REFOCUS_VERSION_2 = 2;
    public final static int BLUR_REFOCUS_VERSION_3 = 3;
    public final static int BLUR_REFOCUS_VERSION_6 = 6;
    /*BLUR_REFOCUS_VERSION_7 is sbs preview plan*/
    public final static int BLUR_REFOCUS_VERSION_7 = 7;
    /*BLUR_REFOCUS_VERSION_8 is sbs capture plan*/
    public final static int BLUR_REFOCUS_VERSION_8 = 8;
    public final static int BLUR_REFOCUS_VERSION_9 = 9;//support hdr + bokeh

    private final static String TARGET_IS_REFOCUS_WARNING_ENABLE = "persist.sys.cam.warning.enable";
    public final static int BLUR_COVERED_ID_OFF = 5;
    private static int mBlurCoveredId = -1;
    private static boolean isIdleSleep;

    private static boolean isModeSwitchAnimationEnabled = false;
    private final static String TARGET_MODE_SWITCH_ANIMATION_ENABLE = "persist.sys.cam.mode.swt.anim";

    public final static String TARGET_AF_AE_IN_RECORDING_ENABLE = "persist.sys.cam.record.afae";
    private final static String TARGET_IS_VIDEO_EXTERNAL = "persist.sys.cam.video.external";
    private final static String TARGET_IS_AISCENE_PRO_ENABLE = "persist.sys.cam.aiscenepro.enable";
    private static boolean isAISceneProEnable = false;

    public static void initialize(Context context) {
        Log.i(TAG, " initialize start");
        isNinetynineBurstEnabled = isEnable(TARGET_TS_BURST_NINETYNINE_ENABLE, true);
        isZslBurstEnabled = isEnable(TARGET_TS_ZSL_BURST_ENABLE, false);
        isZslEnable = isEnable(TARGET_TS_ZSL_ENABLE, true);
        isFrontCameraMirrorEnable = isEnable(TARGET_TS_FRONT_CAMERA_MIRROR_ENABLE, true);
        isColorEffectEnabled = isEnable(TARGET_COLOR_EFFECT_ENABLE, false);
        isContinuePhotoEnabled  = isEnable(TARGET_CONTINUE_PHOTO_ENABLE, true);
        isIntervalPhotoEnabled  = isEnable(TARGET_INTERVAL_PHOTO_ENABLE, true);
        isSlowMotionEnabled  = isEnable(TARGET_SLOW_MOTION_ENABLE, false);
        isTimelapseEnabled  = isEnable(TARGET_TIME_LAPSE_ENABLE, true);
        /* SPRD: New Feature EIS&OIS @{ */
        isEOISDcBackEnabled = isEnable(TARGET_IS_EOIS_DC_BACK_ENABLE, false);
        isEOISDcFrontEnabled = isEnable(TARGET_IS_EOIS_DC_FRONT_ENABLE, false);
        isEOISDvBackEnabled = isEnable(TARGET_IS_EOIS_DV_BACK_ENABLE, false);
        isEOISDvFrontEnabled = isEnable(TARGET_IS_EOIS_DV_FRONT_ENABLE, false);
        /* SPRD: Add for highiso 556862 */
        //isHighISOEnable = isEnable(TARGET_TS_HIGHISO, false);
        isHighISOEnable = false;
        isTouchPhotoEnable = isEnable(TARGET_IS_TOUCH_PHOTO, true);
        isVoicePhotoEnabled = isEnable(TARGET_CAMERA_VOICE_PHOTO_ENABLE, true);
        isBurstAlbumEnabled = isEnable(TARGET_CAMERA_BURST_ALBUM_ENABLE, true);
        // SPRD: Fix bug 572309 camera GPS function
        isRecordLocationEnabled = isEnable(TARGET_RECORD_LOCATION_ENABLE, true);
        mLowBatteryNoFlashLevel = android.os.SystemProperties.getInt(TARGER_LOW_BATTERY_NO_FLASH, 10);
        mSmileScoreThreshold = android.os.SystemProperties.getInt(TARGET_SMILE_THRESHOLD, 35);
        // fix bug 578797 New feature:QR code.
        isQrCodeEnabled = isEnable(TARGET_IS_QRCODE_ENABLE, true);
        //SPRD : Add for bug 657472 Save normal hdr picture
        isNormalHdrEnabled = isEnable(TARGET_IS_NORMAL_HDR_ENABLE, false);
        // SPRD: Add for bug 602877, select filter fps range
        isFilterHighFpsEnable = isEnable(TARGER_FILTER_HIGH_FPS, false);

        /* SPRD: Add for bug 594960, beauty video recoding @{ */
        //isMakeupVideoEnable = isEnable(TARGET_MAKE_UP_VIDEO, true);
        isMakeup3DEnable = isEnable(TARGET_MAKE_UP_3D, false);
        isSwitchAnimationEnable = isEnable(TARGET_TS_SWITCH_ANIMATION_ENABLE, false);
        isFreezeBlurEnable = isEnable(TARGET_TS_FREEZE_BLUR_ENABLE, true);
        /* @} */
        // SPRD: Fix bug 585183 Adds new features 3D recording
        isTDVideoEnable = isEnable(TARGET_TS_TDVideo_ENABLE, false);
        isTDPhotoEnable = isEnable(TARGET_TS_TDPHOTO_ENABLE, false);
        isBurstModeEnable = isEnable(TARGET_TS_BURST_MODE_ENABLE, false);
        isFreezeDisplayEnabled = isEnable(TARGET_FREEZE_DISPLAY_ENABLE, true);
        isZoomPanelEnabled = isEnable(TARGET_ZOOM_PANEL_ENABLE, true);
        isAIDetectEnabled = isEnable(TARGET_AI_DETECT_ENABLE, true);
        isVideoExternal = isEnable(TARGET_IS_VIDEO_EXTERNAL, true);
        isManualShutterEnabled = isEnable(TARGET_MANUAL_CONTROL_SHUTTER_ENABLE, false);
        isManualFocusEnabled = isEnable(TARGET_MANUAL_CONTROL_FOCUS_ENABLE, false);
        isFilterVideoEnable = isEnable(TARGET_FILTER_VIDEO_ENABLE, false);
        isNavigationEnable = hasNavigationBar(context);
        initNavigationBarHeight(context);

        isMSensorEnable = isEnable(TARGET_MSENSOR_ENABLE, false);

        // SPRD: Fix bug 613015 add SurfaceView support
        isSurfaceViewAlternativeEnabled = isEnable(TARGER_SURFACE_VIEW_ALTERNATIVE, true);

        isSensorSelfShotEnable = isEnable(TARGET_TS_SENSOR_SELF_SHORT_ENABLE, false);

        // SPRD: Fix bug 585183 Adds new feature real-time distance measurement
        isTDRangeFindEnable = isEnable(TARGET_TS_RANGEFIND_ENABLE, false);
        isIdleSleep = isEnable(TARGET_TS_IDLE_SLEEP, true);
        isModeSwitchAnimationEnabled = isEnable(TARGET_MODE_SWITCH_ANIMATION_ENABLE, false);
        is3DNREnable = isEnable(TARGET_3DNR_ENABLE, false);
        mFilterVersion = android.os.SystemProperties.getInt(TARGET_FILTER_VERSION, 2);
        mFilterPlus = android.os.SystemProperties.getInt(TARGET_IP_FILTER_PRO, 0);
        mHighResScale = android.os.SystemProperties.getInt(TARGET_HIGH_RESOLUTION_SCALE, 1);
        mHighResScaleTest = isEnable(TARGET_HIGH_RESOLUTION_SCALE_TEST,false);
        isBlurNeedThumbCallback = isEnable(TARGET_BLUR_NEED_THUMB_CALLBACK, false);
        isAFAEInRecordingEnable = isEnable(TARGET_AF_AE_IN_RECORDING_ENABLE, true);
        isVideoAEAFLockEnable = isEnable(TARGET_CAMERA_VIDEO_AEAF_LOCK, false);
        isQuickCaptureEnabled = isEnable(TARGET_TS_QUICK_CAPTURE_ENABL,false);
        isWideCaptureLockAEEnabled = isEnable(TARGET_WIDE_CAPTURE_LOCK_AE,true);
        isWideLowPowerEnable = isEnable(TARGET_WIDE_LOW_POWER, false);
        isWideSupport8MEnable = isEnable(TARGET_WIDE_SUPPORT_8M, false);
        isNormalNeedThumbCallback = isEnable(TARGET_NORMAL_NEED_THUMB_CALLBACK, false);
        wideAngleVersion = getIntProperties(TARGET_CAMERA_WIDEANGLE_VERSION,2);
        mWideFrameGapCount = getIntProperties(TARGET_WIDE_FRAME_GAP, 1);
        isBeautyAllFeature = android.os.SystemProperties.getBoolean(TARGET_TS_BEAUTY_ALL_FEATYRE, false);
        isDrvTest = android.os.SystemProperties.getBoolean(TARGET_DRV_TEST, false);

        isDynamicFpsEnable = isEnable(TARGET_CAMERA_DYNAMIC_FPS,true);
        mPreferVideoMinFps = getIntProperties(TARGET_CAMERA_PREFER_VIDEO_FPS, 20);//SPRD:fix bug1141953
        isARPhotoEnabled = isAppInstalled(context , "com.usens.usensar");
        isARVideoEnabled = isARPhotoEnabled;
        isCaptureAnimatationEnable = isEnable(TARGET_CAMERA_CAP_ANIM, false);
        isTouchEvEnable = isEnable(TARGET_CAMERA_TOUCH_EV, false);
        isAEAFSeparateEnable = isEnable(TARGET_CAMERA_AEAF_SEPARATE, false);
        portraitDefaultFNumber = getIntProperties(TARGET_PORTRAIT_DEFAULT_FNUMBER,4);

        autoToPortraitDefaultNum = getIntProperties(TARGET_TS_AUTO_TO_PORTRAIT_DEFAULT_NUMBER, 15);
        portraitToAutoDefaultNum = getIntProperties(TARGET_TS_PORTRAIT_TO_AUTO_DEFAULT_NUMBER, 15);
        isLevelEnabled = isEnable(TARGET_IS_LEVEL_ENABLE, false);
        autoTo3DNRDefaultNum = getIntProperties(TARGET_TS_AUTO_TO_3DNR_DEFAULT_NUMBER, 20);
        threeDNRToAutoDefaultNum = getIntProperties(TARGET_TS_3DNR_TOPO_AUTO_DEFAULT_NUMBER, 15);
        isAISceneProEnable = isEnable(TARGET_IS_AISCENE_PRO_ENABLE, false);


        //isUseGooglePhotos = isAppInstalled(context , "com.google.android.apps.photos");
        initDisplaysize();
        mCameraUtilCounter.count();
        Log.d(TAG, "initialize end");
    }

    public static void initCameraData() {
        initializeCapabilities();
        initializeLogicalCamera();
        mCameraUtilCounter.count();
    }

    private static Counter mCameraUtilCounter = new Counter(2);

    public static void waitCameraUtilCounter() {
        mCameraUtilCounter.waitCount();
    }
   //SPRD: BUG1139705 Modify the judgment for NavigationBar's config.
   private static boolean hasNavigationBar(Context context) {
        try {
            return WindowManagerGlobal.getWindowManagerService().hasNavigationBar(context.getDisplayId());
        } catch (RemoteException ex) {
            Log.d(TAG, "RemoteException: "+ ex);
        }
        return false;
    }

    private static boolean isAppInstalled(Context context , String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            Log.i(TAG , packageName + "app is installed");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG , packageName + "is not installed ...");
            return false;
        }
    }

    /*SPRD: BUG890774 change prop to tag
     * @{
     */
    private static int[] mSprdFeatureCapabilities = null;
    public static void initializeCapabilities(){
        Log.i(TAG, "initializeCapabilities start");
        try {
            OneCameraManager oneCameraManager = OneCameraModule.provideOneCameraManager();
            if (mSprdFeatureCapabilities == null || mSprdFeatureCapabilities.length == 0) {
                mSprdFeatureCapabilities = oneCameraManager.getSprdFeatureCapabilities();
                Log.i(TAG, "get capability from hal mSprdFeatureCapabilities ="+Arrays.toString(mSprdFeatureCapabilities));
                if (mSprdFeatureCapabilities == null || mSprdFeatureCapabilities.length == 0) {
                    Log.i(TAG , "get no capability from hal");
                    return;
                }
                initializeCapabilitiesFromFeatureList(mSprdFeatureCapabilities);
            } else {
                initializeCapabilitiesFromFeatureList(mSprdFeatureCapabilities);
            }
            backSensorVersion = oneCameraManager.getAvailableSensor(DreamUtil.BACK_CAMERA);
            frontSensorVersion = oneCameraManager.getAvailableSensor(DreamUtil.FRONT_CAMERA);
        } catch (OneCameraException e) {
            e.printStackTrace();
        } catch (OneCameraAccessException e) {//SPRD:fix bug1252379
            e.printStackTrace();
        }
        Log.i(TAG, "initializeCapabilities end");
    }
    private enum FeatureTagID {
        BEAUTYVERSION,
        BACKBLURVERSION,
        FRONTBLURVERSION,
        BLURCOVEREDID,
        FRONTFLASHMODE,
        BACKWPLUSTMODEENABLE,
        AUTOTRACKINGENABLE,
        BACKULTRAWIDEANGLEENABLE,
        GDEPTHENABLE,
        BACKPORTRAITEENABLE,
        FRONTPORTRAITENABLE,
        MONTIONENABLE,
        DEFAULTQUARTERSIZE,
        TCAMENABLE,
        HIGHRESBACKENABLE,
        HDRZSLENABLE,
        CALIBRATIONENABLE,
        IRPHOTOENABLE,
        MACROPHOTOENABLE,
        MACROVIDEOENABLE,
        HIGHRESFRONTENABLE,
        STL3DENABLE,
        VIDEOBEAUTY,
        FOCUSLENGTHFUSIONPHOTOENABLE,
        TDNRPROENABLE,
        LIGHTPORTRAITENABLE,
        LIGHTPORTRAITFRONTENABLE,
        FDRENABLE,
        PORTRAITBACKGROUNDREPLACEMENTENABLE,
        SUPERMACROPHOTOENABLE,
        PORTRAITANDREFOCUSMUTEX,
        FDRPHOTOENABLE,
        PORTRAITREFOCUSENABLE,
    };
    private static void initializeCapabilitiesFromFeatureList(int[] featureList) {
        try {
            mCurrentBeautyVersion = featureList[FeatureTagID.BEAUTYVERSION.ordinal()];
            mBackBlurRefocusVersion = featureList[FeatureTagID.BACKBLURVERSION.ordinal()];
            mFrontBlurRefocusVersion = featureList[FeatureTagID.FRONTBLURVERSION.ordinal()];
            mBlurCoveredId = featureList[FeatureTagID.BLURCOVEREDID.ordinal()];
            mFrontFlashMode = featureList[FeatureTagID.FRONTFLASHMODE.ordinal()];
            /* start add for w+t mode*/
            isWPlusTEnable = featureList[FeatureTagID.BACKWPLUSTMODEENABLE.ordinal()] == 1 ? true : false;
            /* end */

            // Bug 1024253 - Ultra Wide Angle
            isUltraWideAngleEnable = featureList[FeatureTagID.BACKULTRAWIDEANGLEENABLE.ordinal()] == 1 ? true : false;
            mAutoChasingSupported = featureList[FeatureTagID.AUTOTRACKINGENABLE.ordinal()] == 1 ? true : false;
            mHasGdepth = featureList[FeatureTagID.GDEPTHENABLE.ordinal()] == 1 ? true : false;
            isFrontPortraitPhotoEnable =  featureList[FeatureTagID.FRONTPORTRAITENABLE.ordinal()] == 1 ? true : false;
            isBackPortraitPhotoEnable =  featureList[FeatureTagID.BACKPORTRAITEENABLE.ordinal()] == 1 ? true : false;
            isMotionPhotoEnabled = featureList[FeatureTagID.MONTIONENABLE.ordinal()] == 1 ? true : false;
            mDefaultQuarterSizeEnable = featureList[FeatureTagID.DEFAULTQUARTERSIZE.ordinal()] == 1 ? true : false;
            mTcamEnable = featureList[FeatureTagID.TCAMENABLE.ordinal()] == 1 ? true : false;
            isBackHighResolutionPhotoEnable = featureList[FeatureTagID.HIGHRESBACKENABLE.ordinal()] == 1 ? true : false;
            isFrontHighResolutionPhotoEnable = featureList[FeatureTagID.HIGHRESFRONTENABLE.ordinal()] == 1 ? true : false;
            mHdrZslEnable = featureList[FeatureTagID.HDRZSLENABLE.ordinal()] == 1 ? true : false;
            isIRPhotoEnable = featureList[FeatureTagID.IRPHOTOENABLE.ordinal()] == 1 ? true : false;
            isFDRPhotoEnable = featureList[FeatureTagID.FDRPHOTOENABLE.ordinal()] == 1 ? true : false;
            isFocusLengthFusionPhotoEnable = featureList[FeatureTagID.FOCUSLENGTHFUSIONPHOTOENABLE.ordinal()] == 1 ? true : false;
            isMacroPhotoEnable = featureList[FeatureTagID.MACROPHOTOENABLE.ordinal()] == 1 ? true : false;
            isMacroVideoEnable = featureList[FeatureTagID.MACROVIDEOENABLE.ordinal()] == 1 ? true : false;
            isMakeupVideoEnable = featureList[FeatureTagID.VIDEOBEAUTY.ordinal()] == 1 ? true : false;
            is3DNRProEnable = featureList[FeatureTagID.TDNRPROENABLE.ordinal()] == 1 ? true : false;
            isLightPortraitEnable = featureList[FeatureTagID.LIGHTPORTRAITENABLE.ordinal()] == 1 ? true : false;
            isLightPortraitFrontEnable = featureList[FeatureTagID.LIGHTPORTRAITFRONTENABLE.ordinal()] == 1 ? true : false;
            isFdrEnable = featureList[FeatureTagID.FDRENABLE.ordinal()] == 1 ? true : false;
//            isPortraitRefocusEnable = isEnable("persist.sys.cam.refocus.display", false);
            isPortraitRefocusEnable = featureList[FeatureTagID.PORTRAITREFOCUSENABLE.ordinal()] == 1 ? true : false;
            isPortraitBackgroundReplacementEnable = featureList[FeatureTagID.PORTRAITBACKGROUNDREPLACEMENTENABLE.ordinal()] == 1 ? true : false;
            isSRFusionEnable = featureList[FeatureTagID.SUPERMACROPHOTOENABLE.ordinal()] == 1 ? true : false;
            isPortraitAndRefocusMutex = featureList[FeatureTagID.PORTRAITANDREFOCUSMUTEX.ordinal()] == 0 ? true : false;

        } catch(ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
    /*
     * @}
     */

    private final static int[] mLogicalCameraId = new int[] {-1, -1};
    private static int mBackLogicalCameraId = 0;
    private static int mFrontLogicalCameraId = 0;
    public static void initializeLogicalCamera() {
        Log.i(TAG, "initializeLogicalCamera start mLogicalCameraId[0] = " + mLogicalCameraId[0]);
        try {
            if (-1 == mLogicalCameraId[0]) {
                OneCameraManager oneCameraManager = OneCameraModule.provideOneCameraManager();
                oneCameraManager.getLogicalCameraId(mLogicalCameraId);
                changeBlurCameraId(mLogicalCameraId);
            } else {
                changeBlurCameraId(mLogicalCameraId);
            }
        } catch (OneCameraException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "initializeLogicalCamera end");
    }

    private static void changeBlurCameraId(int[] logical) {
        mBackLogicalCameraId = logical[0];
        mFrontLogicalCameraId = logical[1];
    }

    public  static int getBackBlurCameraId() {
        return  mBackLogicalCameraId != 0 ? mBackLogicalCameraId : CameraUtil.BLUR_REFOCUS_PHOTO_ID;
    }

    public  static int getFrontBlurCameraId() {
        return  mFrontLogicalCameraId != 0 ? mFrontLogicalCameraId : CameraUtil.FRONT_BLUR_REFOCUS_PHOTO_ID;
    }

    private static void initDisplaysize() {
        WindowManager windowManager = AndroidServices.instance().provideWindowManager();
        Point res = new Point();
        Point  realRes = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        windowManager.getDefaultDisplay().getRealSize(realRes);
        size = new Size(res);
        realSize = new Size(realRes);
        windowManager.getDefaultDisplay().getDisplayInfo(mDisplayInfo);
        Log.i(TAG, "displaySize="+size.toString()+" displayRealSize="+realSize.toString());
    }

    /* SPRD:fix bug1152619 add for cut out @{ */
    public static boolean hasCutout() {
        if(mDisplayInfo.displayCutout == null) {
            return false;
        } else if (mDisplayInfo.displayCutout.isEmpty()) {
            return false;
        }
        return true;
    }

    public static int getCutoutHeight() {
        if (hasCutout()) {
            return mDisplayInfo.displayCutout.getSafeInsetTop() - mDisplayInfo.displayCutout.getSafeInsetBottom();
        } else {
            return 0;
        }
    }
    /* @} */

    private enum SensorVersion {
        NORMAL_SENSOR,
        FOUR_IN_ONE_SENSOR,
        YUV_SENSOR
    };
    public static boolean isModeSwitchAnimationEnabled() {
        return isModeSwitchAnimationEnabled;
    }

    public static boolean isNavigationEnable() {
        return isNavigationEnable;
    }
    public static boolean isFilterVideoEnable() {
        return isFilterVideoEnable;
    }
    private static void initNavigationBarHeight(Context context) {
        try {
            if (isNavigationEnable) {
                final Resources res = context.getResources();
                int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    mNavigationBarHeightPixel = res.getDimensionPixelSize(resourceId);
                }
            }
        } catch (Throwable e) {
            mNavigationBarHeightPixel = 0;
        }
    }

    public static int getNormalNavigationBarHeight() {
        return mNavigationBarHeightPixel;
    }

    public static String getRefocusModeSupport() {
        if (mBackBlurRefocusVersion == 0) {
            return ("unsupport");
        } else if (mBackBlurRefocusVersion != 0) {
            return ("blurRefocus");
        } else {
            return ("error");
        }
    }

    public static boolean isIsMotionPhotoEnabled(){
        return isMotionPhotoEnabled;
    }

    public static boolean isARPhotoEnabled() {
        return isARPhotoEnabled;
    }

    public static boolean isARVideoEnabled() {
        return isARVideoEnabled;
    }

    public static boolean isPortraitPhotoEnable() {
        return isFrontPortraitPhotoEnable() && isBackPortraitPhotoEnable();
    }

    public static boolean isFrontPortraitPhotoEnable(){
        return isFrontPortraitPhotoEnable || isLightPortraitFrontEnable;
    }

    public static boolean isBackPortraitPhotoEnable(){
        return isBackPortraitPhotoEnable || isLightPortraitEnable;
    }

    public static boolean isPortraitAndRefocusMutex(){
        return isPortraitAndRefocusMutex;
    }

    public static boolean isFrontPortraitBlurEnable(){
        return isFrontPortraitPhotoEnable;
    }

    public static boolean isBackPortraitBlurEnable(){
        return isBackPortraitPhotoEnable;
    }

    public static boolean isLightPortraitEnable(){
        return isLightPortraitEnable;
    }

    public static boolean isLightPortraitFrontEnable(){
        return isLightPortraitFrontEnable;
    }

    public static int getPortraitDefaultFNumber(){
        return portraitDefaultFNumber;
    }

    public static boolean isPortraitRefocusEnable(){
        return isPortraitRefocusEnable;
    }

    public static boolean isFrontHighResolutionPhotoEnable(){
        return isFrontHighResolutionPhotoEnable;
    }

    public static boolean isBackHighResolutionPhotoEnable(){
        return isBackHighResolutionPhotoEnable;
    }

    public static boolean isHighResolutionPhotoEnable() {
        return isBackHighResolutionPhotoEnable && isFrontHighResolutionPhotoEnable;
    }

    public static boolean isIRPhotoEnable() {
        return isIRPhotoEnable;
    }
    public static boolean isFDRPhotoEnable() {
        return isFDRPhotoEnable;
    }

    public static boolean isFocusLengthFusionPhotoEnable() {
        return isFocusLengthFusionPhotoEnable;
    }

    public static boolean isMacroPhotoEnable() {
        return isMacroPhotoEnable;
    }

    public static boolean isPortraitBackgroundReplacementEnable() {
        return isPortraitBackgroundReplacementEnable;
    }

	public static boolean isSRFusionEnable() {
		return isSRFusionEnable;
	}

    public static boolean isMacroVideoEnable() {
        return isMacroVideoEnable;
    }
    public static boolean isUseGooglePhotos() {
        return isUseGooglePhotos;
    }

    private static boolean isEnable(String Key, boolean def) {
        return android.os.SystemProperties.getBoolean(Key, def);
    }

    private static int getIntProperties(String Key, int def){
        return android.os.SystemProperties.getInt(Key,def);
    }

    public static int getAutoToPortraitDefaultNumber(){
        return autoToPortraitDefaultNum;
    }

    public static int getPortraitToAutoDefaultNumber(){
        return portraitToAutoDefaultNum;
    }

    public static int getAutoTo3DNRDefaultNumber(){
        return autoTo3DNRDefaultNum;
    }

    public static int get3DNRToAutoDefaultNumber(){
        return threeDNRToAutoDefaultNum;
    }

    public static boolean isAISceneProEnable(){
        return isAISceneProEnable;
    }

    /* SPRD: Fix bug 585183 Adds new features 3D recording @{ */
    public static boolean isTDVideoEnable() {
        return isTDVideoEnable;
    }
    /* @} */
    /* SPRD: Adds new features 3D Photo @{ */
    public static boolean isTDPhotoEnable() {
        return isTDPhotoEnable;
    }
    /* @} */
    /*
     * fix bug 578797 New feature:QR code. @{
     */
    public static boolean isQrCodeEnabled(){
        return isQrCodeEnabled;
    }
    /* @} */
    public static void setHDRSupportable(boolean value){
        Log.i(TAG,"setHDRSupportable="+value);
        isHdrSupport = value;
    }
    public static void setFDRSupportable(boolean value){
        Log.i(TAG,"setFDRSupportable="+value);
        isFdrSupport = value;
    }
    /* SPRD: Add for bug 948896 Add ai scene detect switcher @｛*/
    public static void setAiSceneDetectSupportable(boolean value){
        Log.i(TAG,"setAiSceneDetectSupportable="+value);
        isAiSceneDetectSupport = value;
    }
    public static boolean isAiSceneDetectSupportable(){
        return isAiSceneDetectSupport;
    }
    /* @} */
    //SPRD : Add for bug 657472 Save normal hdr picture
    public static boolean isNormalHdrEnabled(){
        return isNormalHdrEnabled && isHdrSupport;
    }
    /* SPRD: Fix bug 572309 camera GPS function @{ */
    public static boolean isRecordLocationEnable() {
        return isRecordLocationEnabled;
    }
    /* @} */

    public static boolean isFrontCameraMirrorEnable() {
        return isFrontCameraMirrorEnable;
    }

    public static boolean isZslEnable() {
        return isZslEnable;

    }

    public static boolean isFdrSupport(){
        return isFdrSupport && isFdrEnable;
    }

    /* SPRD: Add for bug 594960, beauty video recoding @{ */
    public static boolean isMakeupVideoEnable() {
        return isMakeupVideoEnable;
    }

    public static boolean isMakeup3DEnable() {
        return isMakeup3DEnable;
    }
    /* @} */
    public static boolean isBurstModeEnable() {
        return isBurstModeEnable;
    }
    public static boolean isSensorSelfShotEnable() {
        return isSensorSelfShotEnable;
    }
    /* SPRD: New Feature EIS&OIS @{ */
    public static boolean isEOISDcBackEnabled() {
        return isEOISDcBackEnabled;
    }
    public static boolean isEOISDcFrontEnabled() {
        return isEOISDcFrontEnabled;
    }
    public static boolean isEOISDvBackEnabled() {
        return isEOISDvBackEnabled;
    }
    public static boolean isEOISDvFrontEnabled() {
        return isEOISDvFrontEnabled;
    }
    /* @} */

    public static boolean isNinetyNineBurstEnabled() {
        return isNinetynineBurstEnabled;
    }

    public static boolean isZslBurstEnabled() {
        return isZslBurstEnabled;
    }

    public static boolean isColorEffectEnabled() {
        return isColorEffectEnabled;
    }

    public static boolean isContinuePhotoEnabled() {
        return isContinuePhotoEnabled && !isBack4in1Sensor();
    }
    public static boolean isManualPhotoEnable() {
        return !isBackYUVSensor();
    }

    public static boolean isIntervalPhotoEnabled() {
        return isIntervalPhotoEnabled;
    }

    public static boolean isSlowMotionEnabled() {
        return isSlowMotionEnabled && !isBackYUVSensor();
    }

    public static boolean isTimelapseEnabled() {
        return isTimelapseEnabled && !isBackYUVSensor();
    }

    public static boolean isFreezeDisplayEnabled() {
        return isFreezeDisplayEnabled;
    }

    public static boolean isZoomPanelEnabled() {
        return isZoomPanelEnabled;
    }

    public static boolean isManualShutterEnabled() {
        return isManualShutterEnabled;
    }
    public static boolean isManualFocusEnabled() {
        return isManualFocusEnabled;
    }
    public static boolean isLevelEnabled() {
        return isLevelEnabled;
    }
    public static boolean isAIDetectEnabled() {
        return isAIDetectEnabled;
    }

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public static boolean isVoicePhotoEnable() {
        return isVoicePhotoEnabled;
    }
    /* }@ */

    public static boolean isIdleSleepEnable() {
        return isIdleSleep;
    }

   /*SPRD: add for burstmode album. @{ */
    public static boolean isBurstAlbumEnabled() {
        return isBurstAlbumEnabled;
    }
    /* }@ */

    public static int getLowBatteryNoFlashLevel() {
        return mLowBatteryNoFlashLevel;
    }

    public static int getSmileScoreThreshold() { return mSmileScoreThreshold; }

    public static int getCurrentBackBlurRefocusVersion() {
        return mBackBlurRefocusVersion;
    }

    public static int getCurrentBlurCoveredId() {
        return mBlurCoveredId;
    }

    public static int getCurrentFrontBlurRefocusVersion() {
        return mFrontBlurRefocusVersion;
    }

    /* SPRD: Add for bug 602877, select filter fps range @{ */
    public static boolean isFilterHighFpsEnable() { return isFilterHighFpsEnable; }
    /* @} */

    /* SPRD: Fix bug 585183 Adds new feature real-time distance measurement @{ */
    public static boolean isTDRangeFindEnable() {
        return isTDRangeFindEnable;
    }
    /* @} */

    public static boolean isMSensorEnable() {
        return isMSensorEnable;
    }
    public static boolean isVideoExternalEnable() {
        return isVideoExternal;
    }
    /* SPRD: Fix bug 613015 add SurfaceView support @{ */
    public static boolean isSurfaceViewAlternativeEnabled() {
        return isSurfaceViewAlternativeEnabled;
    }
    /* @} */
    
    /* SPRD: add for TimeStamp @{*/
    public static final String[] subPathA = {"/.UCam","/imagedigit"};
    public static final String subPathTimeStampRes = subPathA[0] + subPathA[1] ;
    public static final String subPathRootRes = subPathA[0] ;

    public static String getDateTimeFormat(ContentResolver resolver) {
        String strDateFormat = android.provider.Settings.System.getString(
                resolver, android.provider.Settings.System.DATE_FORMAT);
        strDateFormat = (strDateFormat == null ? "yyyy-MM-dd" : strDateFormat);

        return strDateFormat + " HH:mm:ss";
    }
    /*}@*/

    public static boolean isWideAngleEnable() {
        return 0 < wideAngleVersion;
    }

    public static boolean isCameraBeautyEnable() {
        return mCurrentBeautyVersion != DISABLE_BEAUTY;
    }

    public static int getCurrentBeautyVersion() {
        return mCurrentBeautyVersion;
    }

    /**
     * this is for sprd beauty algorithm,default value is false
     * @return if algorithm does not support acne\skincolour\lipscolour, return false, if supported, return true
     */
    public static boolean isCameraBeautyAllFeatureEnabled() {
        return isBeautyAllFeature;
    }

    public static boolean isDrvTest () {return isDrvTest;}

    public static boolean isNewWideAngle() {
        return 1 < wideAngleVersion;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }



    /* SPRD: add for Highiso 556862 }@ */
    public static boolean isHighISOEnable() {
        return isHighISOEnable;
    }

    public static boolean isTouchPhotoEnable() {
        return isTouchPhotoEnable;
    }

    /**
     * SPRD: add fix bug 555245 do not display thumbnail picture in MTP/PTP Mode at pc
     * return the optimalest supported thumbnail Size by the given ratio of picture width and height
     *
     * @param pictureSize the picture's size will to take
     * @param cameraProvider cameraProvider to provide Current CameraId
     * @return the optimalest supported thumbnail Size
     */
    public static Size getAdaptedThumbnailSize(Size pictureSize, CameraProvider cameraProvider) {
        final double ASPECT_TOLERANCE = 0.16;
        Size resultSize = new Size(0, 0);
        if (pictureSize.getWidth() <= 0 || pictureSize.getHeight() <= 0) {
            throw new IllegalArgumentException("pictureSize can not be less than 0");
        }
        double ratio = (double) pictureSize.getWidth() / (double) pictureSize.getHeight();
        try {
            CameraId cameraId = cameraProvider.getCurrentCameraId();
            Size[] thumbnailSize = OneCameraModule.provideOneCameraManager()
                    .getOneCameraCharacteristics(cameraId).getSupportedThumbnailSizes();
            for (Size size : thumbnailSize) {
                if (size.width() == 0 || size.height() == 0) {
                    continue;
                }
                if (Math.abs(ratio - (double) size.width() / (double) size.height()) < ASPECT_TOLERANCE) {
                    resultSize = size;
                    break;
                }
            }
        } catch (OneCameraAccessException e) {
            e.printStackTrace();
        } catch (OneCameraException e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "getAdaptedThumbnailSize:" + resultSize + "; with ratio: " + ratio);
            return resultSize;
        }
    }

    public static void playSound(AppController appController, String key,
                                 MediaActionSound cameraSound, int actionSound) {
        DataModuleBasic dataModule = DataModuleManager.getInstance(
                appController.getAndroidContext()).getDataModuleCamera();
        if (dataModule.getBoolean(key)) {
            //cameraSound.load(actionSound);
            cameraSound.play(actionSound);
        }
    }

    /* SPRD:fix bug 550298 @{ */
    public static boolean isInSilentMode(AppController appController) {
        AudioManager mAudioManager = (AudioManager)appController.getAndroidContext().getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = mAudioManager.getRingerMode();
        return (AudioManager.RINGER_MODE_SILENT == ringerMode || AudioManager.RINGER_MODE_VIBRATE == ringerMode);
    }
    /* @} */

    // add for filter module picture size

    private static Size sizeRestrictedLong = new Size(2048, 1152);
    private static Size sizeRestrictedShort = new Size(2048, 1536);

    private static List<Size> delelteSize(List<Size> list)
    {
        int length = list.size();

        for (int i = 0; i < length; i++) {
            Size size = list.get(i);

            Log.d(TAG, "skyy utils delelteSize, size.height() = " + size.height() + "; size.width() =  " + size.width());

            if ((float) size.width() / (float) size.height() > 1.7f)
            {
                list.remove(i);
                i--;
                length--;
            }
        }

        return list;
    }

    public static List<Size> filterSize(List<Size> list, String key, int mode, Context context) {
        Log.e(TAG, "skyy filterSize  mode = " + mode );
        if(list == null || mode == -1){
            return null;
        }
        Log.e(TAG, "skyy filterSize  list.size() = " + list.size());
        if(!(mode == DataConfig.PhotoModeType.PHOTO_MODE_BACK_UCAM_FILTER || mode == DataConfig.PhotoModeType.PHOTO_MODE_FRONT_UCAM_FILTER) || isUseSprdFilter()){
            delelteSize(list);
            return list;
        }
        Log.e(TAG, "skyy filterSize  getSizesForCamera" );
        int cameraId = key.equals(Keys.KEY_PICTURE_SIZE_BACK) ? 0 : 1;

        List<Size> supported = CameraPictureSizesCacher.getSizesForCamera(cameraId, context);

        List<Size> filterList = dofilterSize(list, supported);
        return filterList;
    }

    private static List<Size> dofilterSize(List<Size> list, List<Size> supported) {

        ArrayList<Size> filterList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Size size = list.get(i);
            Size optimalSizeRestricted;

            if ((float) size.width() / (float) size.height() > 4.0f / 3.0f) {
                optimalSizeRestricted = sizeRestrictedLong;
            } else {
                optimalSizeRestricted = sizeRestrictedShort;
            }

            if (size.height() * size.width()
                    > optimalSizeRestricted.height() * optimalSizeRestricted.width()) {
                if (!filterList.contains(optimalSizeRestricted) && supported != null && supported.contains(optimalSizeRestricted)) {
                    filterList.add(optimalSizeRestricted);
                }
            } else {
                if (!filterList.contains(size)) {
                    filterList.add(size);
                }
            }
        }
        return filterList;
    }

    public static boolean isSwitchAnimationEnable() {
        return isSwitchAnimationEnable;
    }

    public static boolean isFreezeBlurEnable() {
        return isFreezeBlurEnable;
    }

    public static Bitmap computeScale(Bitmap preview, float scale) {
        if (preview == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(preview, 0, 0, preview.getWidth(), preview.getHeight(), matrix, true);
    }

    public static Bitmap blurBitmap(Bitmap bitmap, Context context) {
        if (bitmap == null) {
            return null;
        }
        long startMs = System.currentTimeMillis();

        // Instantiate a new Renderscript
        RenderScript rs = RenderScript.create(context);
        if (rs == null) {
            return null;
        }
        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocation.getType();
        //Create allocation with the same type
        Allocation blurredAllocation = Allocation.createTyped(rs, t);
        // Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // Set the radius of the blur
        blurScript.setRadius(25);
        // Perform the Renderscript
        blurScript.setInput(allocation);
        // Call script for output allocation
        blurScript.forEach(blurredAllocation);
        // Copy script result into bitmap
        blurredAllocation.copyTo(bitmap);
        // After finishing everything, we destroy the Renderscript.
        allocation.destroy();
        blurredAllocation.destroy();
        blurScript.destroy();
        rs.destroy();
        Log.i(TAG, "blur bitmap coast = " + (System.currentTimeMillis() - startMs) + " ms");
//        saveBitmapToFile(bitmap);
        return bitmap;
    }

    public static void saveBitmapToFile(Bitmap bm) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = sDateFormat.format(new java.util.Date());

        File d = new File("/storage/emulated/0/bitmap/");
        File f = new File("/storage/emulated/0/bitmap/", date + ".png");

        if (!d.exists()) {
            d.mkdirs();
        }

        if (f.exists()) {
            f.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            Log.d(TAG, "CameraSaveBitmap saveBitmapToFile:" + f.getAbsolutePath());
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "CameraSaveBitmap saveBitmapToFileException:" + f.getAbsolutePath() + " Exception");
            e.printStackTrace();
        }
    }

    /**
     * @deprecated this method is deprecated, please use {@link #blurBitmap(Bitmap, Context)} instead
     * @param sentBitmap the bitmap want to blur
     * @param radius radius of blur
     * @param canReuseInBitmap whether the input bitmap can be over write
     * @return blured bitmap
     */
    @Deprecated
    public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        bitmap = buildBlurBitmap(bitmap,w,h,radius);
        return (bitmap);
    }

    private static Bitmap buildBlurBitmap(Bitmap bitmap,int w,int h,int radius){

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }
    //SPRD: Fix bug 643027 The prompt is not correct
    public static synchronized void toastHint(CameraActivity activity,int hint){
        boolean isSecureCamera = activity.isSecureCamera();
        Display display = activity.getWindowManager().getDefaultDisplay();
        int height = display.getHeight();
        if(toast == null || mhint != hint){
            toast = Toast.makeText(activity, hint,Toast.LENGTH_LONG);
        }
        //SPRD: Fix bug 942390
        if(isSecureCamera){
            toast.getWindowParams().flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        }

        mhint = hint;
        toast.setGravity(Gravity.TOP, 0, height / 9 + getCutoutHeight());
        toast.show();
    }

    // SPRD:fix bug 666911
    public static void resetHint() {
        /* SPRD: fix bug671302 need cancel toast when module pause @*/
        if (toast != null){
            toast.cancel();
        }
        /* @} */
        mhint = 0;
        toast = null;
    }

    public static void resetDialog() {
        try {
            if (mAlertDialog != null && mAlertDialog.isShowing()){
                mAlertDialog.dismiss();
            }
        } catch (IllegalArgumentException e){
            mAlertDialog = null;
        }

    }
    /*SPRD: fix bug 460566 add for preview optimization @{*/
    public static final int VALUE_FRONT_FLASH_MODE_LCD = 2;
    public static final int VALUE_FRONT_FLASH_MODE_LED = 1;
    public static final int VALUE_FRONT_FLASH_MODE_DEFAULT = VALUE_FRONT_FLASH_MODE_LED;

    public static boolean isFlashSupported(CameraCapabilities capabilities, boolean isFront) {
        Log.i(TAG, " capabilities = " + capabilities + " isFront = " + isFront);
        if(capabilities == null){
            return false;
        } else if (isFront){
            return mFrontFlashMode==VALUE_FRONT_FLASH_MODE_LCD;
        } else {
            return capabilities.isSupportFlash();//SPRD:fix bug1221519
        }
    }

    public static boolean isFlashSupported(CameraCharacteristics characteristics){
        return characteristics.get(FLASH_INFO_AVAILABLE) ? true : mFrontFlashMode == VALUE_FRONT_FLASH_MODE_LCD;
    }

    private static int mFrontFlashMode =  VALUE_FRONT_FLASH_MODE_DEFAULT;
    public static int getFrontFlashMode(){
        return mFrontFlashMode;
    }
    /* @}*/

    public static final String TARGET_3DNR_ENABLE = "persist.sys.cam.3dnr";

    public static final String TARGET_HIGH_RESOLUTION_SCALE = "persist.sys.cam.hrs";
    private  static final String TARGET_HIGH_RESOLUTION_SCALE_TEST = "persist.sys.cam.hrs.test";

    private static int mHighResScale = 1;
    private static boolean mHighResScaleTest = false;
    private static boolean is3DNREnable = false;
    public static boolean is3DNREnable() {
        return is3DNREnable &&
                !((isFront4in1Sensor() || isFrontYUVSensor()) &&
                (isBackYUVSensor() || isBack4in1Sensor()));
    }
    public static boolean is3DNRProEnable() {
        return is3DNRProEnable;
    }

    private final static String TARGET_FILTER_VERSION = "persist.sys.cam.filter.version";//0 for disable filter
    private final static String TARGET_IP_FILTER_PRO = "persist.sys.cam.ip.filter.pro";//0 for disable filterPro

    public static boolean isUseSprdFilter() {
        return (mFilterVersion == 2);
    }

    public static boolean isUseSprdFilterPlus() {
        return (mFilterPlus == 1);
    }

    public static int getHighResolutionScale(){return mHighResScale;}

    public static boolean isHighResolutionScaleTest(){return mHighResScaleTest;}

    public static boolean isAFAEInRecordingEnable = false;
    public static boolean isAFAEInRecordingEnable() {
        return isAFAEInRecordingEnable;
    }

    private static boolean isVideoAEAFLockEnable = false;
    private final static String TARGET_CAMERA_VIDEO_AEAF_LOCK = "persist.sys.cam.video.aeaf.lock";
    public static boolean isVideoAEAFLockEnable() {
        return isVideoAEAFLockEnable;
    }

    private final static String TARGET_BLUR_NEED_THUMB_CALLBACK = "persist.sys.cam.fast.blur";

    private static boolean isBlurNeedThumbCallback = false;
    public static boolean isBlurNeedThumbCallback() {
        return isBlurNeedThumbCallback;
    }

    private final static String TARGET_NORMAL_NEED_THUMB_CALLBACK = "persist.sys.cam.fast.normal";

    private static boolean isNormalNeedThumbCallback = false;
    public static boolean isNormalNeedThumbCallback() {
        return isNormalNeedThumbCallback;
    }

    public static boolean isNeedThumbCallback() {
        return isNormalNeedThumbCallback || isBlurNeedThumbCallback;
    }

    private final static String TARGET_TS_QUICK_CAPTURE_ENABL = "persist.sys.cam.quick";
    private static boolean isQuickCaptureEnabled ;
    public static boolean isQuickCaptureEnabled(){
        return isQuickCaptureEnabled;
    }

    private final static String TARGET_WIDE_CAPTURE_LOCK_AE = "persist.sys.cam.lock.ae";

    private static boolean isWideCaptureLockAEEnabled ;
    public static boolean isWideCaptureLockAEEnabled(){
        return isWideCaptureLockAEEnabled;
    }

    public static boolean isV8Board(){
        return android.os.SystemProperties.getBoolean("ro.build.blade_v8.prj", false);
    }

    public static boolean isGmsVersion() {
        String gmsVersion = android.os.SystemProperties.get("ro.com.google.gmsversion", null);
        if(gmsVersion != null && !gmsVersion.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private final static String TARGET_WIDE_LOW_POWER = "persist.sys.cam.wide.power";

    private static boolean isWideLowPowerEnable = false;
    public static boolean isWideLowPowerEnable() {
        return isWideLowPowerEnable;
    }

    private final static String TARGET_WIDE_FRAME_GAP = "persist.sys.cam.wide.frame.gap";

    private static int mWideFrameGapCount = 1;
    public static int getWideFrameGapCount() {
        return mWideFrameGapCount;
    }

    private final static String TARGET_WIDE_SUPPORT_8M = "persist.sys.cam.wide.8M";//SPRD:fix bug1206207

    private static boolean isWideSupport8MEnable = false;
    public static boolean isWideSupport8MEnable() {
        return isWideSupport8MEnable;
    }

    public static boolean isFlashLevelSupported(CameraCapabilities capabilities) {
        if(capabilities == null) {
            Log.e(TAG,"isFlashLevelSupported() ,capabilities is null");
            return false;
        }
        return capabilities.supportAdjustFlashLevel();
    }

    public static boolean isHdrBlurSupported() {
        return mBackBlurRefocusVersion == BLUR_REFOCUS_VERSION_9;
    }

    public static boolean hasBlurRefocusCapture() {
        if (mBackBlurRefocusVersion != 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean hasFrontBlurRefocusCapture() {
        if (!isFrontYUVSensor() && mFrontBlurRefocusVersion != 0) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isFront4in1Sensor() {
        return frontSensorVersion == SensorVersion.FOUR_IN_ONE_SENSOR.ordinal();
    }
    public static boolean isBack4in1Sensor() {
        return backSensorVersion == SensorVersion.FOUR_IN_ONE_SENSOR.ordinal();
    }
    public static boolean isFrontYUVSensor() {
        return frontSensorVersion == SensorVersion.YUV_SENSOR.ordinal();
    }
    public static boolean isBackYUVSensor() {
        return backSensorVersion == SensorVersion.YUV_SENSOR.ordinal();
    }
    /* start add for w+t mode*/
    private static boolean isWPlusTEnable = false;
    public static boolean isWPlusTEnable() {
        return isWPlusTEnable;
    }

    private static boolean mAutoChasingSupported = false;
    public static boolean isAutoChasingSupport() {
        return mAutoChasingSupported;
    }

    public static int getSuggestWPlusTModeCameraId(int cameraid,Size size){
        if (size == null)
            return cameraid;
        double tmp = (size.width() * size.height()) / 1e6;
        if (isWPlusTEnable){
            if (cameraid == Camera.CameraInfo.CAMERA_FACING_BACK && (mWPlusTThreshold >= tmp)) {
                cameraid = CameraUtil.BACK_W_PLUS_T_PHOTO_ID;
            } else if (cameraid == CameraUtil.BACK_W_PLUS_T_PHOTO_ID && (mWPlusTThreshold < tmp)){
                cameraid = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
        }
        return cameraid;
    }

    public static boolean isResolutionWPlusTSupport(Size size){
        if (mWPlusTThreshold  >= ((size.width() * size.height()) / 1e6))
            return true;
        else
            return false;
    }

    public static void resetWPlusTThreshold(List<Size> sizeList){
        if (sizeList == null)
            return;
        Size maxsize = new Size(0,0);
        for (Size size:sizeList){
            if (size.area() > maxsize.area())
                maxsize = size;
        }
        mWPlusTThreshold = (maxsize.area()/ 1e6);
    }

    public static boolean isWPlusTAbility(Context mContext,boolean currentModuleSupport){
        int cameraId = DataModuleManager.getInstance(mContext).getDataModuleCamera()
                .getInt(Keys.KEY_CAMERA_ID);

        return isWPlusTAbility(mContext,currentModuleSupport,!DreamUtil.isFrontCamera(mContext,cameraId));
    }

    public static boolean isWPlusTAbility(Context mContext,int mModuleIndex, int mCameraId){
        return isWPlusTAbility(mContext,SettingsScopeNamespaces.AUTO_PHOTO == mModuleIndex,!DreamUtil.isFrontCamera(mContext,mCameraId));
    }

    public static boolean isWPlusTAbility(Context mContext, boolean currentModuleSupport, boolean CameraSupport) {
        boolean isCan = false;
        if (CameraUtil.isWPlusTEnable() && currentModuleSupport && CameraSupport) {
            DataStructSetting dataSetting = new DataStructSetting(
                    DreamUtil.intToString(DreamUtil.PHOTO_MODE), false,
                    SettingsScopeNamespaces.AUTO_PHOTO, 1);
            DataModuleBasic dataModuleBasic = DataModuleManager.getInstance(mContext).getTempModule(dataSetting);

            String sizeString = dataModuleBasic.getString(DataConfig.SettingStoragePosition.POSITION_CATEGORY_BF, Keys.KEY_PICTURE_SIZE_BACK, null);
            Size pictureSize = SettingsUtil.sizeFromSettingString(sizeString);
            if (pictureSize == null || CameraUtil.isResolutionWPlusTSupport(pictureSize)) {
                isCan = true;
            }
        }
        return isCan;
    }
    /* end */

    /* SPRD:fix bug974137 config dynamic fps for video @{ */
    private static boolean isDynamicFpsEnable = true;
    private final static String TARGET_CAMERA_DYNAMIC_FPS = "persist.sys.cam.dynamic.fps";
    public static boolean isDynamicFpsEnable() {
        return isDynamicFpsEnable;
    }

    private static int mPreferVideoMinFps = 15;
    private final static String TARGET_CAMERA_PREFER_VIDEO_FPS = "persist.sys.cam.pre.video.fps";
    public static int getPreferVideoMinFps() {
        return mPreferVideoMinFps;
    }

    // Bug 1024253 - Ultra Wide Angle
    private static boolean isUltraWideAngleEnable = false;
    public static boolean isUltraWideAngleEnabled() {
        return isUltraWideAngleEnable;
    }
    public final static int BACK_ULTRA_WIDE_ANGLE_PHOTO_ID = 35;

    public static int getBackUltraWideAngleCameraId() {
        return mBackLogicalCameraId != 0 ? mBackLogicalCameraId : CameraUtil.BACK_ULTRA_WIDE_ANGLE_PHOTO_ID;

    }
    /* @} */

    private static boolean isCaptureAnimatationEnable = false;
    private final static String TARGET_CAMERA_CAP_ANIM = "persist.sys.cam.capture.anim";
    public static boolean isCaptureAnimatationEnable() {
        return isCaptureAnimatationEnable;
    }

    private static boolean isTouchEvEnable = false;
    private final static String TARGET_CAMERA_TOUCH_EV = "persist.sys.cam.touch.ev";
    public static boolean isTouchEvEnable() {
        return isTouchEvEnable;
    }

    private static boolean isAEAFSeparateEnable = false;
    private final static String TARGET_CAMERA_AEAF_SEPARATE = "persist.sys.cam.aeaf.separate";
    public static boolean isAEAFSeparateEnable() {
        return isAEAFSeparateEnable;
    }

    public static boolean isAutoHDRSupported(CameraCapabilities characteristics){
        return characteristics.supportAutoHdr();
    }

    public static int findJpegEndIndex(byte[] originalJpegData) {
        byte[] jpegend = {(byte)0xff,(byte)0xd9};

        int index = -1;
        for(int i = 0; i< originalJpegData.length-1; i ++){
            int j = i;

            byte[] temp = {originalJpegData[j],originalJpegData[j+1]};
            if(jpegend[0] == temp[0] && jpegend[1] == temp[1]){
                if(j == originalJpegData.length-2 || originalJpegData[j+2] != (byte)0xff){
                    index = j+1;
                    return index;
                }
            }
        }
        return index;
    }

    /*add for gdepth*/
    private static boolean mHasGdepth = false;
    public static boolean hasGdepth() {
        return mHasGdepth;
    }

    private static boolean mDefaultQuarterSizeEnable = false;

    public static boolean isDefaultQuarterSizeEnable(){
        return mDefaultQuarterSizeEnable;
    }

    private static boolean isFdrEnable = false;

    public static boolean isFdrEnable(){
        return isFdrEnable;
    }

    private static boolean mHdrZslEnable = false;

    public static boolean isHdrZslEnable(){
        return mHdrZslEnable;
    }

    public static boolean isLowRam() {
        return android.os.SystemProperties.getBoolean("ro.config.low_ram", false);
    }

    private static boolean mTcamEnable = false;
    public static boolean isTcamEnable() {
        Log.d(TAG,"isTcamEnable " + mTcamEnable);
        return  mTcamEnable;
    }

    public static boolean isTcamAbility(Context mContext,int mModuleIndex, int mCameraId){
        return isTcamEnable() &&
                ( SettingsScopeNamespaces.AUTO_PHOTO == mModuleIndex ||
                        SettingsScopeNamespaces.AUTO_VIDEO == mModuleIndex ||
                        SettingsScopeNamespaces.FOCUS_LENGTH_FUSION_PHOTO == mModuleIndex)
                && !DreamUtil.isFrontCamera(mContext,mCameraId);
    }

    public static boolean isTcamAbility(Context mContext, int mCameraId){
        return isTcamEnable() && !DreamUtil.isFrontCamera(mContext,mCameraId);
    }

    public static float getZoomMaxInFourKVideo(float ratio){
        return mZoomFourKMax < ratio ? mZoomFourKMax:ratio;
    }
    public static int byteArrToInt(byte[] arr){
        int x = ((arr[3] & 0xff) << 24 )|((arr[2]& 0xff) <<16 )|((arr[1] & 0xff)<<8)|(arr[0] & 0xff);
        return x;
    }

    public static byte[] subByte(byte[] b,int off,int length){
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    public static void saveBytes(byte[] bytes, String name) {
        OutputStream output = null;
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/DCIM", name
                    + "_bytes");
            output = new FileOutputStream(file);
            output.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**scale equals twu.
     *
     * |  Y  Y  Y  Y  |              |  Y    Y    |          | Y  Y |
     * |  Y  Y  Y  Y  |              |            |          | Y  Y |
     * |  Y  Y  Y  Y  |              |  Y    Y    |          | U  V |
     * |  Y  Y  Y  Y  |     ---->    |            |  ---->
     * |  U  V  U  V  |              |  U V       |
     * |  U  V  U  V  |              |            |
     *
     * @param bytes
     * @param width
     * @param height
     * @param scale
     * @return
     */
    public static byte[] yuvScaleDown(byte[] bytes, int width, int height, int scale){
        int resultlength = width * height * 3 / (2 * scale * scale);
        byte[] b1 = new byte[resultlength];
        int newWidth = width / scale;
        int newHeight = height / scale;
        for (int i = 0;i<newWidth;i++){
            for (int j = 0;j<newHeight;j++){
                b1[j*newWidth+i] = bytes[j*scale*width+i*scale];
            }
        }
        int uv_index = newHeight * newWidth;
        int uv_height = newHeight/2;
        int uv_width = newWidth/2;
        int ori_index = width * height;
        for (int uvi = 0;uvi<uv_width;uvi++){
            for (int uvj = 0;uvj<uv_height;uvj++){
                b1[uv_index + uvj * uv_width * 2 + uvi * 2] = bytes[ori_index + uvj * scale * width + uvi * scale *2];
                b1[uv_index + uvj * uv_width * 2 + uvi * 2 + 1] = bytes[ori_index + uvj * scale * width + uvi * scale *2 + 1];
            }
        }
        return b1;
    }
}
