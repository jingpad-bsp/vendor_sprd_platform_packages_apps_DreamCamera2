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

package com.android.camera;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.provider.MediaStore.Video;

import com.android.camera.app.MediaSaver;
import com.android.camera.app.MediaSaver.OnMediaSavedListener;
import com.android.camera.data.FilmstripItemData;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.settings.Keys;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.XmpBuilder;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;
import com.sprd.camera.storagepath.StorageUtil;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleBasic;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import android.database.Cursor;
import android.provider.MediaStore;

// SPRD: Fix 474843 Add for Filter Feature

/**
 * A class implementing {@link com.android.camera.app.MediaSaver}.
 */
public class MediaSaverImpl implements MediaSaver {
    private static final Log.Tag TAG = new Log.Tag("MediaSaverImpl");
    private static final String VIDEO_BASE_URI = "content://media/external/video/media";
    public static final String PHOTO_ERROR_URI = "content://media/external/images/media/error";

    /** The memory limit for unsaved image is 30MB. */
    // TODO: Revert this back to 20 MB when CaptureSession API supports saving
    // bursts.
    // SPRD: modify for bug 625296, Limit the sPoolWorkQueue memory size.
    private static final int SAVE_TASK_MEMORY_LIMIT = /*30 * 1024 * 1024*/staticGetLargeMemory();

    /* SPRD:Fix bug 555811 Can not slide around after taking a picture @{ */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
    private static final Executor THREAD_POOL_EXECUTOR
        = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    /* @} */

    /**
     * SPRD: Add for bug 625296, to prevent the occurrence of OOM.
     * Limit the sPoolWorkQueue memory size to 0.6 for heapsize.
     */
    public static int staticGetLargeMemory() {
        // vm heap size, and assume it is in megabytes and thus ends with "m".
        String vmHeapSize = SystemProperties.get("dalvik.vm.heapsize", "100m");
        double largeMemory = Double.parseDouble(vmHeapSize.substring(0, vmHeapSize.length() - 1))
                * 0.6 * 1024 * 1024;
        return (int) largeMemory;
    }
    /* @} */

    private final ContentResolver mContentResolver;

    /** Memory used by the total queued save request, in bytes. */
    private long mMemoryUse;

    // SPRD: Fix 474843 Add for Filter Feature
    private static final int MAX_IMAGE_COUNT_IN_LOW_RAM = 2;
    private int mCurrentImageCount = 0;
    /* @} */

    private QueueListener mQueueListener;

    /* SPRD: Fix bug 547144 that app is killed because of low memory @{ */
    private final boolean IS_LOW_RAM = ActivityManager.isLowRamDeviceStatic();
    /* @} */

    // SPRD: Fix 474843 Add for Filter Feature
    private Context mAppContext = null;
    private DataModuleBasic mDataModuleCurrent;
    /**
     * @param contentResolver The {@link android.content.ContentResolver} to be
     *                 updated.
     */
    public MediaSaverImpl(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        mMemoryUse = 0;
    }

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    public MediaSaverImpl(Context context) {
        mAppContext = context;
        mContentResolver = context.getContentResolver();
        mMemoryUse = 0;
    }
    /* @} */

    /*SPRD:fix bug 388273 @{*/
    private Listener mListener;

    /*
     * SPRD: fix bug 681226 Take photo with FilterModule take time longer than compared mobile  @{
     */
    private FilterListener mFilterListener;

    @Override
    public void setFilterListener(FilterListener l) {
        mFilterListener = l;
    }
    /* @} */

    @Override
    public void setListener(Listener l) {
        mListener = l;
    }

    @Override
    public boolean isEmptyQueue() {
        return mMemoryUse == 0;
    }
    /* @}*/

    @Override
    public void addMemoryUse(long length) {
        mMemoryUse += length;
    }

    @Override
    public void reduceMemoryUse(long length) {
        mMemoryUse -= length;
    }

    @Override
    public boolean isQueueFull() {
        return (mMemoryUse >= SAVE_TASK_MEMORY_LIMIT);
    }

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    private boolean isQueueFull(boolean specialCase) {

        /* SPRD: Fix bug 547144 that app is killed because of low memory @{ */
        if (specialCase && IS_LOW_RAM
                && mCurrentImageCount >= MAX_IMAGE_COUNT_IN_LOW_RAM) {

            Log.i(TAG, "isQueueFull true");
            return true;
        }
        /* @} */

        return isQueueFull();
    }
    /* @} */

    @Override
    public void addImage(final byte[] data, String title, long date, Location loc, int width,
            int height, int orientation, ExifInterface exif, OnMediaSavedListener l) {
        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        addImage(data, title, date, loc, width, height, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG);
         */
        addImage(data, title, date, loc, width, height, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG, null);
        /* @} */
    }

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    @Override
    public void addImage(final byte[] data, String title, long date,
            Location loc, int width, int height, int orientation,
            ExifInterface exif, OnMediaSavedListener l, String photoVoicePath) {
        addImage(data, title, date, loc, width, height, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG, photoVoicePath);
    }
   /* @} */

    @Override
    public void addImage(final byte[] data, String title, long date, Location loc, int width,
            int height, int orientation, ExifInterface exif, OnMediaSavedListener l,
            String mimeType, String photoVoicePath) {
        addImage(data, title, date, loc, width, height, orientation, exif, l,
                mimeType, photoVoicePath, null);
    }

    @Override
    /*
     * SPRD: Fix bug 535110, Photo voice record. @{
     * Original Android code :
    public void addImage(final byte[] data, String title, long date, Location loc, int width,
            int height, int orientation, ExifInterface exif, OnMediaSavedListener l,
            String mimeType) {
             */
    public void addImage(final byte[] data, String title, long date, Location loc, int width,
            int height, int orientation, ExifInterface exif, OnMediaSavedListener l,
            String mimeType, String photoVoicePath, XmpBuilder builder) {
           /* @} */

        /*  SPRD: Fix 625296 revert original code @{     */
        if (isQueueFull()) {
            /* SPRD:fix bug761111 add for write fail and avid to oom @ */
            if (l != null) {
                l.onMediaSaved(Uri.parse(PHOTO_ERROR_URI), title);
            }
            onQueueFull();
            /* @} */
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        /* @} */

        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        ImageSaveTask t = new ImageSaveTask(data, title, date,
                (loc == null) ? null : new Location(loc),
                width, height, orientation, mimeType, exif, mContentResolver, l);
          */
        ImageSaveTask t = new ImageSaveTask(data, title, date,
                (loc == null) ? null : new Location(loc),
                width, height, orientation, mimeType, exif, mContentResolver, l, photoVoicePath, builder);
        /* @} */

        mMemoryUse += data.length;
        /*
         * SPRD: Fix 474843 Add for Filter Feature
         * original code @{
         *
        if (isQueueFull()) {
            onQueueFull();
        }
         */
        mCurrentImageCount++;
        if (isQueueFull(false)) {
            onQueueFull();
        }
        /* @} */

        /**
         * SPRD:fix bug 390702
        t.execute();
         */
        // SPRD:Fix bug 555811 Can not slide around after taking a picture
        mDataModuleCurrent = DataModuleManager.getInstance(mAppContext).getCurrentDataModule();
        if(mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR_NORMAL_PIC)
                && mDataModuleCurrent.isEnableSettingConfig(Keys.KEY_CAMERA_HDR)
                && mDataModuleCurrent.getBoolean(Keys.KEY_CAMERA_HDR)){
            t.execute(); //SPRD: Fix 657472 Add for NormalHdr Feature
        }else{
            t.executeOnExecutor(THREAD_POOL_EXECUTOR);
        }
    }

    public void updateImage(Uri uri, byte[] data, String title, long date, Location loc,
            int width, int height, int orientation, ExifInterface exif,
            OnMediaSavedListener l, String mimeType, String photoVoicePath) {
        if (isQueueFull()) {
            onQueueFull();
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }

        ImageUpdateTask t = new ImageUpdateTask(data, title, date,
                (loc == null) ? null : new Location(loc),
                width, height, orientation, mimeType, exif, mContentResolver, l, photoVoicePath, uri);

        mMemoryUse += data.length;
        mCurrentImageCount++;

        t.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    @Override
    public void addImage(final byte[] data, String title, long date, Location loc, int orientation,
            ExifInterface exif, OnMediaSavedListener l) {
        // When dimensions are unknown, pass 0 as width and height,
        // and decode image for width and height later in a background thread
        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        addImage(data, title, date, loc, 0, 0, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG);
         */
        addImage(data, title, date, loc, 0, 0, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG, null);
        /* @} */
    }
    @Override
    public void addImage(final byte[] data, String title, Location loc, int width, int height,
            int orientation, ExifInterface exif, OnMediaSavedListener l) {
        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        addImage(data, title, System.currentTimeMillis(), loc, width, height, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG);
          */
        addImage(data, title, System.currentTimeMillis(), loc, width, height, orientation, exif, l,
                FilmstripItemData.MIME_TYPE_JPEG, null);
        /* @} */
    }

    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    @Override
    public void addImage(final byte[] data, String title, Location loc,
            int width, int height, int orientation, ExifInterface exif,
            OnMediaSavedListener l, String photoVoicePath) {
        addImage(data, title, System.currentTimeMillis(), loc, width, height,
                orientation, exif, l, FilmstripItemData.MIME_TYPE_JPEG,
                photoVoicePath);
    }
    /* @} */

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    @Override
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height,
            int orientation, ExifInterface exif, OnMediaSavedListener l,  String photoVoicePath, boolean filterHandle) {
        /* SPRD: Add for bug 625296, to prevent the occurrence of OOM. @{ */
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        /* @} */
        ImageSaveTask t = new ImageSaveTask(data, title,  System.currentTimeMillis(),
                (loc == null) ? null : new Location(loc)
                , width, height, orientation, FilmstripItemData.MIME_TYPE_JPEG
                , exif, mContentResolver, l, photoVoicePath, filterHandle);

        mMemoryUse += data.length;
        mCurrentImageCount++;

        if (isQueueFull(filterHandle)) {
            onQueueFull();
        }
        // SPRD:Fix bug 555811 Can not slide around after taking a picture
        t.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }
    /* @} */

    @Override
    public void addVideo(String path, ContentValues values, OnMediaSavedListener l) {
        // We don't set a queue limit for video saving because the file
        // is already in the storage. Only updating the database.
        new VideoSaveTask(path, values, l, mContentResolver).execute();
    }

    @Override
    public void setQueueListener(QueueListener l) {
        mQueueListener = l;
        if (l == null) {
            return;
        }
        l.onQueueStatus(isQueueFull());
    }

    private void onQueueFull() {
        if (mQueueListener != null) {
            mQueueListener.onQueueStatus(true);
        }
    }

    private void onQueueAvailable() {
        if (mQueueListener != null) {
            mQueueListener.onQueueStatus(false);
        }
    }

    private class ImageSaveTask extends AsyncTask <Void, Void, Uri> {
        private final byte[] data;
        private final String title;
        private final long date;
        private final Location loc;
        private int width, height;
        private final int orientation;
        private final String mimeType;
        private final ExifInterface exif;
        private final ContentResolver resolver;
        private final OnMediaSavedListener listener;
        // SPRD: Fix bug 474843, New feature of filter.
        private final boolean filterHandle;
        // SPRD: Fix bug 535110, Photo voice record.
        private final String photoVoicePath;
        //SPRD: add XMP TAG to jpeg
        private final XmpBuilder builder;

        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        public ImageSaveTask(byte[] data, String title, long date, Location loc,
                             int width, int height, int orientation, String mimeType,
                             ExifInterface exif, ContentResolver resolver,
                             OnMediaSavedListener listener) {
          */
        public ImageSaveTask(byte[] data, String title, long date, Location loc,
                int width, int height, int orientation, String mimeType,
                ExifInterface exif, ContentResolver resolver,
                OnMediaSavedListener listener, String photoVoicePath, XmpBuilder builder) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.mimeType = mimeType;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            // SPRD: Fix bug 535110, Photo voice record.
            this.photoVoicePath = photoVoicePath;

            // SPRD: Fix 474843 Add for Filter Feature
            this.filterHandle = false;
            this.builder = builder;
        }

        /* SPRD: Fix 474843 Add for Filter Feature @{ */
        public ImageSaveTask(byte[] data, String title, long date, Location loc,
                int width, int height, int orientation, String mimeType,
                ExifInterface exif, ContentResolver resolver,
                OnMediaSavedListener listener, String photoVoicePath, boolean filterHandle) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.mimeType = mimeType;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            this.photoVoicePath = photoVoicePath;
            this.filterHandle = filterHandle;
            this.builder = null;
        }
        /* @} */

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
            if (width == 0 || height == 0) {
                // Decode bounds
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                width = options.outWidth;
                height = options.outHeight;
            }
            try {
                    return Storage.addImage(resolver, title, date, loc,
                            orientation, exif, data, width, height, mimeType, photoVoicePath, builder);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write data", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) {
                listener.onMediaSaved(uri, title);
            }
            /*SPRD:modify for Coverity 109140
             * Orginal android code
            boolean previouslyFull = isQueueFull();
            */

            mMemoryUse -= data.length;
            // SPRD: Fix 474843 Add for Filter Feature
            mCurrentImageCount--;

            /**
             * SPRD: fix bug 388273 @{
            if (isQueueFull() != previouslyFull) {
                onQueueAvailable();
            }
            */
            Log.i(TAG, "mListener=" + mListener + "  ,mMemoryUse=" + mMemoryUse + " ,SAVE_TASK_MEMORY_LIMIT=" + SAVE_TASK_MEMORY_LIMIT);
            if(mListener != null && mMemoryUse == 0 && mListener.getContinuousCaptureCount() == 0){//SPRD BUG:388273
                mListener.onHideBurstScreenHint();
                onQueueAvailable();
            }
        }
    }

    private class VideoSaveTask extends AsyncTask <Void, Void, Uri> {
        /*
         * SPRD: Fix bug 650297 that video thumbnail is showed slowly @{
         * Original Code
         *
        private String path;
         */
        private String path;
        private final ContentValues values;
        private final OnMediaSavedListener listener;
        private final ContentResolver resolver;

        public VideoSaveTask(String path, ContentValues values, OnMediaSavedListener l,
                             ContentResolver r) {
            /*
             * SPRD: Fix bug 650297 that video thumbnail is showed slowly @{
             * Original Code
             *
            this.path = path;
             */
            this.path = path;
            this.values = new ContentValues(values);
            this.listener = l;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            Uri uri = null;
            try {
                Uri videoTable = null;
                long start = System.currentTimeMillis();
                StorageUtil storageUtil = StorageUtil.getInstance();
                videoTable = storageUtil.getStorageVideoUri(path);
                /*SPRD:Bug 1137554, when use DocumentsContract.renameDocument to rename file, Document UI will insert data to database
                * so dream camera have not to insert*/
                //if (path.equals(StorageUtil.KEY_DEFAULT_EXTERNAL) || storageUtil.isExternalPath(path)) {
                    Cursor cursor = resolver.query(Uri.parse(VIDEO_BASE_URI),new String[] {
                            MediaStore.Video.VideoColumns._ID
                    },Video.Media.DATA+"='" + path +"'",null,null);
                    long id = -1;
                    if (cursor!=null){
                        Log.i(TAG,"cursor!=null");
                        if (cursor.moveToFirst()){
                            id = cursor.getLong(0);
                        }
                    }
                    if (id !=-1){
                        uri = Uri.parse(Uri.parse(VIDEO_BASE_URI) + "/" + id);
                        Log.e(TAG,"query uri = " + uri);
                    }
                    if (cursor != null)
                        Log.i(TAG,"cursor != null");
                        cursor.close();
                    if (uri == null) {
                        Log.i(TAG,"uri == null");
                        Log.i(TAG,"resolver.insert");
                        uri = resolver.insert(videoTable, values);
                    }
                //} else {
                //    Log.i(TAG,"internalPath");
                //    uri = resolver.insert(videoTable, values);
                //}
                Log.i(TAG, "resolver.insert cost: " + (System.currentTimeMillis() - start));

                // Rename the video file to the final name. This avoids other
                // apps reading incomplete data.  We need to do it after we are
                // certain that the previous insert to MediaProvider is completed.
                /*
                 * SPRD: Fix bug 650297 that video thumbnail is showed slowly @{
                 * Original Code
                 *
                String finalName = values.getAsString(Video.Media.DATA);
                File finalFile = new File(finalName);
                if (new File(path).renameTo(finalFile)) {
                    path = finalName;
                }
                resolver.update(uri, values, null, null);
                 */
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                Log.e(TAG, "failed to add video to media store", e);
                uri = null;
            } finally {
                Log.v(TAG, "Current video URI: " + uri);
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) {
                listener.onMediaSaved(uri, null);
            }
        }
    }

    public static final String UPDATE_URI_TITLE = "updateUri";
    private class ImageUpdateTask extends AsyncTask <Void, Void, Void> {
        private final byte[] data;
        private final String title;
        private final long date;
        private final Location loc;
        private int width, height;
        private final int orientation;
        private final String mimeType;
        private final ExifInterface exif;
        private final ContentResolver resolver;
        private final OnMediaSavedListener listener;
        private final String photoVoicePath;
        private final Uri uri;

        public ImageUpdateTask(byte[] data, String title, long date, Location loc,
                int width, int height, int orientation, String mimeType,
                ExifInterface exif, ContentResolver resolver,
                OnMediaSavedListener listener, String photoVoicePath, Uri uri) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.mimeType = mimeType;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            // SPRD: Fix bug 535110, Photo voice record.
            this.photoVoicePath = photoVoicePath;
            this.uri = uri;

        }


        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Void doInBackground(Void... v) {
            if (width == 0 || height == 0) {
                // Decode bounds
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                width = options.outWidth;
                height = options.outHeight;
            }
            try {
                Storage.updateImage(uri,resolver, title, date, loc,
                        orientation, exif, data, width, height, mimeType, photoVoicePath);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write data", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (listener != null) {
                listener.onMediaSaved(uri, UPDATE_URI_TITLE);
            }

            mMemoryUse -= data.length;
            // SPRD: Fix 474843 Add for Filter Feature
            mCurrentImageCount--;

            /**
             * SPRD: fix bug 388273 @{
            if (isQueueFull() != previouslyFull) {
                onQueueAvailable();
            }
            */
            Log.i(TAG, "mListener=" + mListener + "  ,mMemoryUse=" + mMemoryUse + " ,SAVE_TASK_MEMORY_LIMIT=" + SAVE_TASK_MEMORY_LIMIT);
            if(mListener != null && mMemoryUse == 0 && mListener.getContinuousCaptureCount() == 0){//SPRD BUG:388273
                mListener.onHideBurstScreenHint();
                onQueueAvailable();
            }
        }
    }
}
