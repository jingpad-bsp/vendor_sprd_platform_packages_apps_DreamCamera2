package com.sprd.camera.encoder;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

public class Util {

    private static final String TAG = "Util";

    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return null if no codec matched
     */
    public static final MediaCodecInfo selectVideoCodec(final String mimeType) {
//         Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {	// skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
//                    Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }
    /**
     * select color format available on specific codec and we can use.
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
//        if (DEBUG) Log.i(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
//        if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;
    static {
        recognizedFormats = new int[] {
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final float BPP = 0.1f * 8 * 3 / 2;
    public static int calcBitRate(int frameRate,int frameWidth, int frameHeight) {
        final int bitrate = (int)(BPP * frameRate * frameWidth * frameHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

//    public static final File getCaptureFile(final String type, final String ext, long milsecond) {
//
//        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), "Camera");
//        Log.d(TAG, "path=" + dir.toString());
//        dir.mkdirs();
//        if (dir.canWrite()) {
//            //return new File(dir, getDateTimeStringinMills(milsecond)+ "_" + milsecond + ext);
//            return new File(dir, getDateTimeString()+ "_" + milsecond + ext);
//
//        }
//        return null;
//    }

    public static final File getCaptureFile(Context context, final String ext) {
        String fileName = getDateTimeString() + ext;
        File file = null;
        try {
            file = File.createTempFile(fileName,null,context.getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private static final String getDateTimeStringinMills(long milsecond) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        return dateTimeFormat.format(new Date(milsecond));
    }

    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        return dateTimeFormat.format(now.getTime());
    }

    public static byte[] getNV21FromImage(Image img) {
        //long t1 = System.currentTimeMillis();
        final int NUM_PLANES = 3;
        try {
            final Image.Plane[] planeList = img.getPlanes();
            ByteBuffer[] planeBuf = new ByteBuffer[NUM_PLANES];

            for (int i = 0; i < NUM_PLANES; i++) {
                Image.Plane plane = planeList[i];
                planeBuf[i] = plane.getBuffer();
            }

            ByteBuffer buf = planeBuf[0];
            int yLength = buf.remaining();
            byte[] imageBytes = new byte[yLength * 3 / 2];

            buf.get(imageBytes, 0, yLength);
            buf.clear();

            buf = planeBuf[1];
            int uLength = buf.remaining();
            buf.get(imageBytes, yLength + 1, uLength);
            buf.clear();

            buf = planeBuf[2];
            buf.get(imageBytes, yLength, 1);
            buf.clear();
//            Log.e(TAG, "getNV21FromImage cost " + (System.currentTimeMillis() - t1));
            return imageBytes;
        }catch (IllegalStateException e){
            e.printStackTrace();
            return null;
        } catch (NullPointerException e){
            e.printStackTrace();
            return null;
        }
    }

    public static void mirror(byte[] imageBytes, int width, int height,int cameraOrientation) {
        if(cameraOrientation == 90 ||  cameraOrientation == 270){
            mirrorYAxis(imageBytes,width,height);
        } else {
            mirrorXAxis(imageBytes,width,height);

        }
    }

    private static void mirrorYAxis(byte[] imageBytes, int width, int height) {
        if(imageBytes != null){
            int yLength = width * height;
            int yWidth = Math.max(width,height);
            int yHeight = Math.min(width,height);

            //x - axis
            // mirror y
            byte temp;
            int wLength= yWidth;
            int hLength = yHeight;
            int index = 0;
            for(int hcount = 0; hcount < hLength /2 ; hcount++){
                int startIndexS = index + wLength * hcount;
                int startIndexE = index + wLength *(hLength - hcount -1);
                for (int wcount = 0; wcount<wLength; wcount++){
                    int x = startIndexS + wcount;
                    int y = startIndexE + wcount;
                    temp = imageBytes[x];
                    imageBytes[x] = imageBytes[y];
                    imageBytes[y] = temp;
                }
            }
            // mirror u&v
            wLength= yWidth;
            hLength = yHeight/2;
            index = yLength;
            for(int hcount = 0;hcount < hLength / 2 ; hcount++){
                int startIndexS = index + wLength * hcount;

                int startIndexE = index  + wLength * (hLength - hcount -1);
                for (int wcount = 0; wcount<wLength; wcount++){
                    int x = startIndexS + wcount;
                    int y = startIndexE + wcount;

                    temp = imageBytes[x];
                    imageBytes[x] = imageBytes[y];
                    imageBytes[y] = temp;
                }
            }
        }
    }

    private static void mirrorXAxis(byte[] imageBytes, int width, int height) {
        if(imageBytes != null){
            int yLength = width * height;
            int yWidth = Math.max(width,height);
            int yHeight = Math.min(width,height);

            // y - axis
            // mirror y
            byte temp;
            int wLength= yWidth;
            int hLength = yHeight;
            int index = 0;
            // mirror y
            for(int hcount = 0;hcount<hLength; hcount++){
                int startIndex = index + wLength * hcount;
                int endIndex = startIndex + wLength -1;
                for (int wcount = 0; wcount<wLength/2; wcount++){
                    int x = startIndex + wcount;
                    int y = endIndex - wcount;
                    temp = imageBytes[x];
                    imageBytes[x] = imageBytes[y];
                    imageBytes[y] = temp;
                }
            }

            // mirror u&v
            wLength= yWidth;
            hLength = yHeight/2;
            index = yLength;
            for(int hcount = 0;hcount<hLength; hcount++){
                int startIndex = index + wLength * hcount;
                int endIndex = startIndex + wLength -2;
                for (int wcount = 0; wcount<wLength/4; wcount++){
                    int u1 = startIndex + wcount*2;
                    int u2 = endIndex - 2*wcount;
                    int v1 = u1 +1;
                    int v2 = u2 +1;
                    temp = imageBytes[u1];
                    imageBytes[u1] = imageBytes[u2];
                    imageBytes[u2] = temp;
                    temp = imageBytes[v1];
                    imageBytes[v1] = imageBytes[v2];
                    imageBytes[v2] = temp;
                }
            }
        }
    }

    public static byte[] NV21toI420SemiPlanar( byte[] nv21bytes, int width, int height) {
        byte[] inputData = new byte[width * height * 3 / 2];
        final int iSize = width * height;
        System.arraycopy(nv21bytes, 0, inputData, 0, iSize);

        for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
            inputData[iSize + iIndex / 2 + iSize / 4] = nv21bytes[iSize + iIndex]; // V
            inputData[iSize + iIndex / 2] = nv21bytes[iSize + iIndex + 1]; // U
        }
        return inputData;
    }
//
//    public static byte[] NV21toI420SemiPlanar(byte[] nv21bytes, int width, int height) {
//        byte[] inputData = new byte[width * height * 3 / 2];
//        final int iSize = width * height;
//        System.arraycopy(nv21bytes, 0, inputData, 0, iSize);
//
//        for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
//            inputData[iSize + iIndex + 1] = nv21bytes[iSize + iIndex]; // V
//            inputData[iSize + iIndex] = nv21bytes[iSize + iIndex + 1]; // U
//        }
//        return inputData;
//    }

    public static byte[] NV21RotateTo90(byte[] nv21_data, int width, int height) {
        byte[] out_data = new byte[width * height * 3 / 2];
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        //byte[] nv21_rotated = new byte[buffser_size];
        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1)*width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                out_data[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++) {
                out_data[i] = nv21_data[offset + x];
                i--;
                out_data[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }

        //reset width and height
        return out_data;
    }

    /**
     * get next encoding presentationTimeUs
     * @return
     */
    public static long getPTSUs() {
        long result = System.nanoTime();
        return result;
    }

    public static long getFileSize(String filePath) {
        long length = 0;
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            length = file.length();
        }
        return length;
    }

    public static void log(String tag, String message){
        Log.e(tag,message);
    }
}
