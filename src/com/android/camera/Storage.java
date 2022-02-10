/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.ContentValues;
// SPRD: Fix bug 535110, Photo voice record.
import android.database.Cursor;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.ParcelFileDescriptor;
// SPRD: Fix bug 535110, Photo voice record.
import android.provider.MediaStore;

import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.LruCache;

import com.android.camera.app.MediaSaver.OnMediaSavedListener;
import com.android.camera.data.FilmstripItemData;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Size;
import com.dream.camera.DreamModule;
import com.google.common.base.Optional;
import com.sprd.camera.storagepath.ExternalStorageUtil;
import com.sprd.camera.storagepath.StorageUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import android.content.Context;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import com.android.camera.util.XmpBuilder;
import com.android.camera.util.XmpConstants;
import com.android.camera.util.XmpUtil;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import java.util.Arrays;
import com.android.camera.util.CameraUtil;

public class Storage {
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static final String DIRECTORY = DCIM + "/Camera";
    public static final File DIRECTORY_FILE = new File(DIRECTORY);
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String GIF_POSTFIX = ".gif";
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long ACCESS_FAILURE = -4L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;
    public static final long MIN_INTERNAL_STORAGE_THRESHOLD_BYTES = 10000000;
    public static final String CAMERA_SESSION_SCHEME = "camera_session";
    private static final Log.Tag TAG = new Log.Tag("Storage");
    private static final String GOOGLE_COM = "google.com";
    private static HashMap<Uri, Uri> sSessionsToContentUris = new HashMap<>();
    private static HashMap<Uri, Uri> sContentUrisToSessions = new HashMap<>();
    private static LruCache<Uri, Bitmap> sSessionsToPlaceholderBitmap =
            // 20MB cache as an upper bound for session bitmap storage
            new LruCache<Uri, Bitmap>(20 * 1024 * 1024) {
                @Override
                protected int sizeOf(Uri key, Bitmap value) {
                    return value.getByteCount();
                }
            };
    private static HashMap<Uri, Point> sSessionsToSizes = new HashMap<>();
    private static HashMap<Uri, Integer> sSessionsToPlaceholderVersions = new HashMap<>();

    /**
     * Save the image with default JPEG MIME type and add it to the MediaStore.
     *
     * @param resolver The The content resolver to use.
     * @param title The title of the media file.
     * @param date The date for the media file.
     * @param location The location of the media file.
     * @param orientation The orientation of the media file.
     * @param exif The EXIF info. Can be {@code null}.
     * @param jpeg The JPEG data.
     * @param width The width of the media file after the orientation is
     *              applied.
     * @param height The height of the media file after the orientation is
     *               applied.
     */
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height) throws IOException {

        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height,
              FilmstripItemData.MIME_TYPE_JPEG);
         */
        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height,
                FilmstripItemData.MIME_TYPE_JPEG, null, null);
        /* @} */
    }

    /**
     * Saves the media with a given MIME type and adds it to the MediaStore.
     * <p>
     * The path will be automatically generated according to the title.
     * </p>
     *
     * @param resolver The The content resolver to use.
     * @param title The title of the media file.
     * @param data The data to save.
     * @param date The date for the media file.
     * @param location The location of the media file.
     * @param orientation The orientation of the media file.
     * @param exif The EXIF info. Can be {@code null}.
     * @param width The width of the media file after the orientation is
     *            applied.
     * @param height The height of the media file after the orientation is
     *            applied.
     * @param mimeType The MIME type of the data.
     * @return The URI of the added image, or null if the image could not be
     *         added.
     */
    /*
     * SPRD: Fix bug 535110, Photo voice record. @{
     * Original Android code :
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] data, int width,
            int height, String mimeType) throws IOException {
      */
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] data, int width,
            int height, String mimeType, String photoVoicePath, XmpBuilder builder) throws IOException {
     /* @} */

        StorageUtil storageUtil = StorageUtil.getInstance();
        String path = null;
        //Feature bug896062 store burst picture in emmc
        Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
        if(photoVoicePath == null) { // for Bug 981153: if photoVoicePath == null , modify file type to NORMAL or HDR;
            if (val != null) // Bug 1159126 (NULL_RETURNS)
                val = (val == FilmstripItemData.TYPE_AUDIO_HDR_PIC) ?
                        FilmstripItemData.TYPE_HDR :
                        (val == FilmstripItemData.TYPE_AUDIO_PIC ? FilmstripItemData.TYPE_NORMAL : val);
                val = (val == DreamModule.TYPE_AUDIO_FDR_PIC) ?
                    DreamModule.TYPE_FDR :
                    (val == FilmstripItemData.TYPE_AUDIO_PIC ? FilmstripItemData.TYPE_NORMAL : val);
        }

        //save to internal storage for burst and audio photo
        if ((val != null) && (val == FilmstripItemData.TYPE_BURST || val == FilmstripItemData.TYPE_BURST_COVER ||
                val == FilmstripItemData.TYPE_AUDIO_PIC || val == FilmstripItemData.TYPE_AUDIO_HDR_PIC || val == DreamModule.TYPE_AUDIO_FDR_PIC)) {// Bug 1159126 (NULL_RETURNS)
            Log.i(TAG,"addimage val ="+val);
            path = storageUtil.generateInternalFilePath(title, mimeType);
        }
        else {
            path = storageUtil.generateFilePath(title, mimeType);
        }
        if(path == null){
            return null;
        }
        /*
         * SPRD: Change storage imagepath for storage path Feature
        String path = generateFilepath(title, mimeType);
        */
        Log.i(TAG, "For_Test addImage path = " + path);
        int fileTag = 0;
        if (val != null) {
            fileTag = val.intValue();
            if (builder == null) {
            builder = setSpecialTypeId(fileTag);
            }
        }
        /**
         * SPRD Bug 1149886
         *Original process: firstly, write file  then insert database
         * we will change to firstly insert database with "is_pending" equals 0, then write file
         * if file is complete, will update "is_pending" to 1, else will
         * delete item from database
         */
        Uri insertUri = addImageToMediaStore(resolver, title, date, location, orientation, 0,
                path, width, height, mimeType, photoVoicePath, fileTag, 1);
        Log.i(TAG,"insert uri is "+insertUri);
        long fileLength = writeFile(resolver, path, data, exif, builder);
        if (fileLength >= 0) {
            /*
             * SPRD: Fix bug 535110, Photo voice record. @{
             * Original Android code :
            return addImageToMediaStore(resolver, title, date, location, orientation, fileLength,
                    path, width, height, mimeType);
             */
            if (photoVoicePath != null) {
                ExifTag[] jpegLenTag = new ExifTag[] {exif.buildTag(ExifInterface.TAG_JPEG_LENGTH, new Long(fileLength))};
                Collection<ExifTag> reWritePara = Arrays.asList(jpegLenTag);
                exif.rewriteExif(path, reWritePara);
                FileInputStream fin = null;
                RandomAccessFile randomAccessFile = null;
                try {
                    fin = new FileInputStream(photoVoicePath);
                    int length = fin.available();
                    byte[] buffer = new byte[length];
                    fin.read(buffer);

                    randomAccessFile = new RandomAccessFile(path,"rw");
                    randomAccessFile.seek(new File(path).length());
                    randomAccessFile.write(buffer);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    fin.close();//SPRD:fix bug1201887
                    randomAccessFile.close();
                    File d = new File(photoVoicePath);
                    if (d.exists() && d.isFile() && !d.isHidden()) {
                        d.delete();
                    }
                }
            }
            if(insertUri != null){
                updateImageToMediaStore(resolver, insertUri, title, date, location, orientation, fileLength,
                        path, width, height, mimeType, photoVoicePath, fileTag, 0);
            }
            return insertUri;
            /* @} */
        } else {
            resolver.delete(insertUri, null, null);
            Log.e(TAG,"File is not complete and delete item in media store");
            return null;
        }

    }

    /**
     * this is google requirement, OEM special type should embed XMP Metadata with GCamera:SpecialTypeID
     * @param fileFlag
     * @return XmpBuilder
     */
    public static XmpBuilder setSpecialTypeId(int fileFlag) {
        XmpBuilder xmpBuilder = new XmpBuilder();
        switch (fileFlag) {
            case DreamModule.TYPE_AUDIO_PIC:
            case DreamModule.TYPE_AUDIO_HDR_PIC:
            case DreamModule.TYPE_AUDIO_FDR_PIC:
                xmpBuilder.setSpecialTypeId("AUDIO_PHOTO_TYPE");
                break;
            case DreamModule.TYPE_HDR:
                xmpBuilder.setSpecialTypeId("HDR_PHOTO_TYPE");
                break;
            case DreamModule.TYPE_FDR:
                xmpBuilder.setSpecialTypeId("FDR_PHOTO_TYPE");
                break;
            case DreamModule.TYPE_SLOW_MOTION:
                xmpBuilder.setSpecialTypeId("SLOW_MOTION_VIDEO_TYPE");
                break;
            case DreamModule.TYPE_AI_PHOTO:
            case DreamModule.TYPE_AI_HDR:
            case DreamModule.TYPE_AI_FDR:
                xmpBuilder.setSpecialTypeId("AI_PHOTO_TYPE");
                break;
            case DreamModule.TYPE_MODE_BOKEH:
            case DreamModule.TYPE_MODE_BLUR_BOKEH:
            case DreamModule.TYPE_MODE_BLUR:
            case DreamModule.TYPE_MODE_BOKEH_BOKEH:
            case DreamModule.TYPE_MODE_BOKEH_FDR:
            case DreamModule.TYPE_MODE_BOKEH_FDR_BOKEH:
            case DreamModule.TYPE_MODE_BOKEH_HDR:
            case DreamModule.TYPE_MODE_BOKEH_HDR_BOKEH:{
                if (!CameraUtil.hasGdepth()) {
                    xmpBuilder.setSpecialTypeId("BOKEH_PHOTO_TYPE");
                } else {
                    xmpBuilder = null;
                }
                break;
            }
            default:
                xmpBuilder = null;
                break;
        }
        return xmpBuilder;
    }
    /**
     * Add the entry for the media file to media store.
     *
     * @param resolver The The content resolver to use.
     * @param title The title of the media file.
     * @param date The date for the media file.
     * @param location The location of the media file.
     * @param orientation The orientation of the media file.
     * @param width The width of the media file after the orientation is
     *            applied.
     * @param height The height of the media file after the orientation is
     *            applied.
     * @param mimeType The MIME type of the data.
     * @return The content URI of the inserted media file or null, if the image
     *         could not be added.
     */
    /*
     * SPRD: Fix bug 535110, Photo voice record. @{
     * Original Android code :
    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
            Location location, int orientation, long jpegLength, String path, int width, int height,
            String mimeType) {
      */

    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
            Location location, int orientation, long jpegLength, String path, int width, int height,
            String mimeType, String photoVoicePath) {
        return addImageToMediaStore(resolver, title, date,
                location, orientation, jpegLength, path, width, height,
                mimeType, photoVoicePath, 0);
    }
    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
                                           Location location, int orientation, long jpegLength, String path, int width, int height,
                                           String mimeType, String photoVoicePath, int fileTag) {
        return addImageToMediaStore(resolver, title, date,
                location, orientation, jpegLength, path, width, height,
                mimeType, photoVoicePath, 0, 0);
    }

    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
            Location location, int orientation, long jpegLength, String path, int width, int height,
            String mimeType, String photoVoicePath, int fileTag, int isPending) {
     /* @} */
        // Insert into MediaStore.
        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        ContentValues values =
                getContentValuesForData(title, date, location, orientation, jpegLength, path, width,
                        height, mimeType);
          */
        ContentValues values =
                getContentValuesForData(resolver, title, date, location, orientation, jpegLength, path, width,
                        height, mimeType, photoVoicePath, fileTag, isPending);
        /* @} */

        Uri uri = null;
        Cursor cursor = null;
        try {
            long start = System.currentTimeMillis();
            StorageUtil storageUtil = StorageUtil.getInstance();
            Uri insertUri = storageUtil.getStorageImagesUri(path);

            uri = resolver.insert(insertUri, values);
            Log.i(TAG, "resolver.insert cost: " + (System.currentTimeMillis() - start));
            /*
             * SPRD: Fix bug 1101850
             */
            if (uri == null){
                cursor = resolver.query(insertUri,new String[]{MediaStore.Images.ImageColumns._ID},ImageColumns.DATA+"='" + path +"'",null,null);
                long id = -1;
                if (cursor!=null){
                    if (cursor.moveToFirst()){
                        id = cursor.getLong(0);
                    }
                }
                if (id !=-1){
                    uri = Uri.parse(insertUri + "/" + id);
                    Log.e(TAG,"query uri = " + uri);
                }
            }
            /* @} */
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }finally {
            if (cursor != null)
                cursor.close();
        }
        return uri;
    }

    public static void updateImageToMediaStore(ContentResolver resolver, Uri updateUri, String title, long date,
                                               Location location, int orientation, long jpegLength, String path, int width, int height,
                                               String mimeType, String photoVoicePath, int fileTag, int isPending) {
        ContentValues values =
                getContentValuesForData(resolver, title, date, location, orientation, jpegLength, path, width,
                        height, mimeType, photoVoicePath, fileTag, isPending);

        resolver.update(updateUri, values, null, null);
    }
    // Get a ContentValues object for the given photo data
    /*
     * SPRD: Fix bug 535110, Photo voice record. @{
     * Original Android code :
    public static ContentValues getContentValuesForData(String title,
            long date, Location location, int orientation, long jpegLength,
            String path, int width, int height, String mimeType) {
     */
    public static ContentValues getContentValuesForData(ContentResolver resolver, String title,
            long date, Location location, int orientation, long jpegLength,
            String path, int width, int height, String mimeType, String photoVoicePath, int fileTag, int isPending) {
    /* @} */
        File file = new File(path);
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());

        ContentValues values = new ContentValues(11);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, mimeType);
        values.put(ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);
        values.put(MediaColumns.IS_PENDING, isPending);
        values.put("file_flag", fileTag);
        setImageSize(values, width, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }

        return values;
    }

    /**
     * Add a placeholder for a new image that does not exist yet.
     *
     * @param placeholder the placeholder image
     * @return A new URI used to reference this placeholder
     */
    public static Uri addPlaceholder(Bitmap placeholder) {
        Uri uri = generateUniquePlaceholderUri();
        replacePlaceholder(uri, placeholder);
        return uri;
    }

    /**
     * Remove a placeholder from in memory storage.
     */
    public static void removePlaceholder(Uri uri) {
        sSessionsToSizes.remove(uri);
        sSessionsToPlaceholderBitmap.remove(uri);
        sSessionsToPlaceholderVersions.remove(uri);
    }

    /**
     * Add or replace placeholder for a new image that does not exist yet.
     *
     * @param uri the uri of the placeholder to replace, or null if this is a
     *            new one
     * @param placeholder the placeholder image
     * @return A URI used to reference this placeholder
     */
    public static void replacePlaceholder(Uri uri, Bitmap placeholder) {
        Log.v(TAG, "session bitmap cache size: " + sSessionsToPlaceholderBitmap.size());
        Point size = new Point(placeholder.getWidth(), placeholder.getHeight());
        sSessionsToSizes.put(uri, size);
        sSessionsToPlaceholderBitmap.put(uri, placeholder);
        Integer currentVersion = sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, currentVersion == null ? 0 : currentVersion + 1);
    }

    /**
     * Creates an empty placeholder.
     *
     * @param size the size of the placeholder in pixels.
     * @return A new URI used to reference this placeholder
     */
    @Nonnull
    public static Uri addEmptyPlaceholder(@Nonnull Size size) {
        Uri uri = generateUniquePlaceholderUri();
        sSessionsToSizes.put(uri, new Point(size.getWidth(), size.getHeight()));
        sSessionsToPlaceholderBitmap.remove(uri);
        Integer currentVersion = sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, currentVersion == null ? 0 : currentVersion + 1);
        return uri;
    }

    /**
     * Take jpeg bytes and add them to the media store, either replacing an existing item
     * or a placeholder uri to replace
     * @param imageUri The content uri or session uri of the image being updated
     * @param resolver The content resolver to use
     * @param title of the image
     * @param date of the image
     * @param location of the image
     * @param orientation of the image
     * @param exif of the image
     * @param jpeg bytes of the image
     * @param width of the image
     * @param height of the image
     * @param mimeType of the image
     * @return The content uri of the newly inserted or replaced item.
     */
    public static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date,
           Location location, int orientation, ExifInterface exif,
           byte[] jpeg, int width, int height, String mimeType) throws IOException {
        /*
         * SPRD: Change storage imagepath for storage path Feature
        String path = generateFilepath(title, mimeType);
        */
        StorageUtil storageUtil = StorageUtil.getInstance();
        String path = storageUtil.generateFilePath(title, mimeType);
        Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
        int fileTag = 0;
        XmpBuilder builder = new XmpBuilder();
        if (val != null) {
            fileTag = val.intValue();
            builder = setSpecialTypeId(fileTag);
        }
        writeFile(resolver, path, jpeg, exif, builder);
        return updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, path,
                width, height, mimeType, null, fileTag);
    }

    public static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif,
            byte[] jpeg, int width, int height, String mimeType, String photoVoicePath) throws IOException {
         /*
          * SPRD: Change storage imagepath for storage path Feature
         String path = generateFilepath(title, mimeType);
         */
         StorageUtil storageUtil = StorageUtil.getInstance();
         String path = storageUtil.generateFilePath(title, mimeType);
         Integer val = exif.getTagIntValue(ExifInterface.TAG_CAMERATYPE_IFD);
         int fileTag = 0;
         XmpBuilder builder = new XmpBuilder();
         if (val != null) {
             fileTag = val.intValue();
             builder = setSpecialTypeId(fileTag);
         }
         writeFile(resolver, path, jpeg, exif, builder);
         return updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, path,
                 width, height, mimeType, photoVoicePath, fileTag);
     }

    private static Uri generateUniquePlaceholderUri() {
        Uri.Builder builder = new Uri.Builder();
        String uuid = UUID.randomUUID().toString();
        builder.scheme(CAMERA_SESSION_SCHEME).authority(GOOGLE_COM).appendPath(uuid);
        return builder.build();
    }

    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    public static long writeFile(ContentResolver cr, String path, byte[] jpeg, ExifInterface exif) throws IOException {
        return writeFile(cr, path, jpeg, exif, null);
    }
    /**
     * Writes the JPEG data to a file. If there's EXIF info, the EXIF header
     * will be added.
     *
     * @param path The path to the target file.
     * @param jpeg The JPEG data.
     * @param exif The EXIF info. Can be {@code null}.
     *
     * @return The size of the file. -1 if failed.
     */
    public static long writeFile(ContentResolver cr, String path, byte[] jpeg, ExifInterface exif, XmpBuilder builder) throws IOException {
        Log.i(TAG, "For_Test public function writeFile path = " + path);
        if (!createDirectoryIfNeeded(path)) {
            if (path != null) {
                File parentFile = new File(path).getParentFile();
                if (!parentFile.exists()) {
                    Log.e(TAG, "Failed to create parent directory for file: " + path);
                    return -1;
                }
            } else {
                return -1;
            }
        }
        if (exif != null) {
            ExifTag jpeg_lenght = exif.buildTag(ExifInterface.TAG_JPEG_LENGTH, new Long(0));
            exif.setTag(jpeg_lenght);
            XMPMeta xmpMeta = null;
            if (builder != null) {
                try {
                    xmpMeta = builder.build();
                } catch (XMPException e) {
                    throw new RuntimeException(e);
                }
            }
            StorageUtil storageUtil = StorageUtil.getInstance();
            Uri uri = storageUtil.getCurrentAccessUri(path);
            ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
            if (storageUtil.isInternalPath(path)) {
                exif.writeExif(jpeg, path);
                if (xmpMeta != null) {
                    XmpUtil.writeXMPMeta(path, xmpMeta);
                }
            } else {
                //Sprd:fix bug952246
                Uri picUri = externalStorageUtil.createDocUri(cr, uri, path);
                if(picUri == null){
                    return 0;
                }
                ParcelFileDescriptor pfd = cr.openFileDescriptor(picUri, "w");
                if (pfd != null) {
                    FileOutputStream outputStream = externalStorageUtil.createOutputStream(pfd);
                    /**
                     * Bug 1159131/1159130, fix coverity 228909/229527, Resource leak on an exceptional path (RESOURCE_LEAK)
                     * for pfd/outputstream
                     */
                    try {
                        exif.writeExif(jpeg, outputStream);
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        outputStream.close();
                        pfd.close();
                    }
                    if (xmpMeta != null) {
                        XmpUtil.writeXMPMeta(cr, picUri, xmpMeta);
                    }
                } else {
                    return 0;
                }
            }
            return new  File(path).length();
        } else {
            return writeFile(cr, path, jpeg, builder);
        }
//        return -1;
    }

    /**
     * Renames a file.
     *
     * <p/>
     * Can only be used for regular files, not directories.
     *
     * @param inputPath the original path of the file
     * @param newFilePath the new path of the file
     * @return false if rename was not successful
     */
    public static boolean renameFile(File inputPath, File newFilePath) {
        if (newFilePath.exists()) {
            Log.e(TAG, "File path already exists: " + newFilePath.getAbsolutePath());
            return false;
        }
        if (inputPath.isDirectory()) {
            Log.e(TAG, "Input path is directory: " + inputPath.getAbsolutePath());
            return false;
        }
        if (!createDirectoryIfNeeded(newFilePath.getAbsolutePath())) {
            Log.e(TAG, "Failed to create parent directory for file: " +
                    newFilePath.getAbsolutePath());
            return false;
        }
        return inputPath.renameTo(newFilePath);
    }

    /**
     * Writes the data to a file.
     *
     * @param path The path to the target file.
     * @param data The data to save.
     *
     * @return The size of the file. -1 if failed.
     */
    private static long writeFile(ContentResolver resolver, String path, byte[] data, XmpBuilder builder) {
        FileOutputStream out = null;
        ParcelFileDescriptor pfd = null;
        Log.i(TAG, "For_Test private function writeFile path = " + path);
        long start = System.currentTimeMillis();
        XMPMeta xmpMeta = null;
        if (builder != null) {
            try {
              xmpMeta = builder.build();
            } catch (XMPException e) {
              throw new RuntimeException(e);
            }
        }
        StorageUtil storageUtil = StorageUtil.getInstance();
        Uri uri = storageUtil.getCurrentAccessUri(path);
        ExternalStorageUtil externalStorageUtil = ExternalStorageUtil.getInstance();
        try {
            if (storageUtil.isInternalPath(path)) {
                out = new FileOutputStream(path);
                out.write(data);
                if (xmpMeta != null) {
                    XmpUtil.writeXMPMeta(path, xmpMeta);
                    Log.i(TAG, "writeFile cost: " + (System.currentTimeMillis() - start));
                    return new File(path).length();
                } else {
                    Log.i(TAG, "xmpMeta null, writeFile cost: " + (System.currentTimeMillis() - start));
                    return data.length;
                }
            } else {
                pfd = externalStorageUtil.createParcelFd(resolver, uri, path,"image/*");
                out = externalStorageUtil.createOutputStream(pfd);
                out.write(data);
                if (xmpMeta != null) {
                    FileInputStream fileInputStream = externalStorageUtil.createInputStream(pfd);
                    XmpUtil.writeXMPMeta(fileInputStream,out,xmpMeta);
                    fileInputStream.close();
                    Log.i(TAG, "external writeFile cost: " + (System.currentTimeMillis() - start));
                    return new File(path).length();
                } else {
                    Log.i(TAG, "external xmpMeta null, writeFile cost: " + (System.currentTimeMillis() - start));
                    return data.length;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
                if (pfd != null) {
                    pfd.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return -1;
    }

    /**
     * Given a file path, makes sure the directory it's in exists, and if not
     * that it is created.
     *
     * @param filePath the absolute path of a file, e.g. '/foo/bar/file.jpg'.
     * @return Whether the directory exists. If 'false' is returned, this file
     *         cannot be written to since the parent directory could not be
     *         created.
     */
    private static boolean createDirectoryIfNeeded(String filePath) {
        if(filePath == null){
            return false;
        }
        File parentFile = new File(filePath).getParentFile();

        // If the parent exists, return 'true' if it is a directory. If it's a
        // file, return 'false'.
        if (parentFile.exists()) {
             Log.i(TAG, "For_Test createDirectoryIfNeeded/out parentFile.isDirectory() = "
                    + parentFile.isDirectory());
            return parentFile.isDirectory();
        }

        // If the parent does not exists, attempt to create it and return
        // whether creating it succeeded.
        return parentFile.mkdirs();
    }

    /** Updates the image values in MediaStore. */
    private static Uri updateImage(Uri imageUri, ContentResolver resolver, String title,
            long date, Location location, int orientation, int jpegLength,
            String path, int width, int height, String mimeType, String photoVoicePath, int fileTag) {

        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        ContentValues values =
                getContentValuesForData(title, date, location, orientation, jpegLength, path,
                        width, height, mimeType);
         */
        ContentValues values =
                getContentValuesForData(resolver, title, date, location, orientation, jpegLength, path,
                        width, height, mimeType, photoVoicePath, fileTag,0);
        /* @} */


        Uri resultUri = imageUri;
        if (Storage.isSessionUri(imageUri)) {
            // If this is a session uri, then we need to add the image
            /*
             * SPRD: Fix bug 535110, Photo voice record. @{
             * Original Android code :
            resultUri = addImageToMediaStore(resolver, title, date, location, orientation,
                    jpegLength, path, width, height, mimeType);
             */
            resultUri = addImageToMediaStore(resolver, title, date, location, orientation,
                    jpegLength, path, width, height, mimeType, photoVoicePath, fileTag);
            /* @} */
            sSessionsToContentUris.put(imageUri, resultUri);
            sContentUrisToSessions.put(resultUri, imageUri);
        } else {
            // Update the MediaStore
            resolver.update(imageUri, values, null, null);
        }
        return resultUri;
    }


    public static String generateFilepath(String directory, String title, String mimeType) {
        String extension = null;
        if (FilmstripItemData.MIME_TYPE_JPEG.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else if (FilmstripItemData.MIME_TYPE_GIF.equals(mimeType)) {
            extension = GIF_POSTFIX;
        } else {
            throw new IllegalArgumentException("Invalid mimeType: " + mimeType);
        }
        return (new File(directory, title + extension)).getAbsolutePath();
    }

    /**
     * Returns the jpeg bytes for a placeholder session
     *
     * @param uri the session uri to look up
     * @return The bitmap or null
     */
    public static Optional<Bitmap> getPlaceholderForSession(Uri uri) {
        return Optional.fromNullable(sSessionsToPlaceholderBitmap.get(uri));
    }

    /**
     * @return Whether a placeholder size for the session with the given URI
     *         exists.
     */
    public static boolean containsPlaceholderSize(Uri uri) {
        return sSessionsToSizes.containsKey(uri);
    }

    /**
     * Returns the dimensions of the placeholder image
     *
     * @param uri the session uri to look up
     * @return The size
     */
    public static Point getSizeForSession(Uri uri) {
        return sSessionsToSizes.get(uri);
    }

    /**
     * Takes a session URI and returns the finished image's content URI
     *
     * @param uri the uri of the session that was replaced
     * @return The uri of the new media item, if it exists, or null.
     */
    public static Uri getContentUriForSessionUri(Uri uri) {
        return sSessionsToContentUris.get(uri);
    }

    /**
     * Takes a content URI and returns the original Session Uri if any
     *
     * @param contentUri the uri of the media store content
     * @return The session uri of the original session, if it exists, or null.
     */
    public static Uri getSessionUriFromContentUri(Uri contentUri) {
        return sContentUrisToSessions.get(contentUri);
    }

    /**
     * Determines if a URI points to a camera session
     *
     * @param uri the uri to check
     * @return true if it is a session uri.
     */
    public static boolean isSessionUri(Uri uri) {
        return uri.getScheme().equals(CAMERA_SESSION_SCHEME);
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }
}
