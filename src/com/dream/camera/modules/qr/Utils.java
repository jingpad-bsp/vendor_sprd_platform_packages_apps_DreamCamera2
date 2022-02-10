package com.dream.camera.modules.qr;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import com.sprd.camera.storagepath.StorageUtilProxy;
import android.os.storage.VolumeInfo;
import java.util.List;
import com.sprd.camera.storagepath.MultiStorage;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.FileDescriptor;

import com.android.camera.debug.Log;
import libcore.io.IoUtils;
import android.content.ContentResolver;
import android.content.ContentProviderClient;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public class Utils {

    private static final Log.Tag TAG = new Log.Tag("QrUtils");

    private static Uri getUri(String type) {
        if ("image".equals(type)) {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else{
                    Map<String, String> storagePathMap = MultiStorage.getUsbStorageMap();
                    for(Map.Entry<String, String> entry : storagePathMap.entrySet()) {
                        String FileDir = entry.getValue();
                        if(FileDir != null){
                            String usbFilePath = "";
                            if(FileDir.contains(type)){
                            // usb
                                List<VolumeInfo> usblist = StorageUtilProxy.getUsbdiskVolumes();
                                if(usblist != null) {
                                    VolumeInfo usbInfo = usblist.get(0);
                                if(usbInfo != null){
                                    usbFilePath = usbInfo.path;
                                }
                            }
                            return usbFilePath + "/" + split[1];
                            }
                            }
                        }
                    return StorageUtilProxy.getSecondaryStorageDirectory() + "/" + split[1];
                    }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                /*try{
                    final String docId = DocumentsContract.getDocumentId(uri);
                    if("downloads".equals(docId)) {
                        File downloadroot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if(downloadroot != null) {
                            return downloadroot.getAbsolutePath();
                        }
                   }
                    final String[] split = docId.split(":");
                    final String id = split[0];
                    if(split.length > 1){
                        String path =  split[1];
                        return path;
                    } else {
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/all_downloads"),
                                Long.valueOf(id));
                        return getDataColumn(context, contentUri, null, null);
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, uri + " is not a download uri");
                    return null;
                }*/

                //SPRD: Fix bug 1170718
                File file = null;
                ParcelFileDescriptor pFd = null;
                try {
                    ContentResolver resolver = context.getContentResolver();
                    ContentProviderClient client =  resolver.acquireUnstableContentProviderClient(uri);
                    if(client == null){
                        Log.w(TAG, "Unknown URI: " +  uri);
                        return null;
                    }
                    pFd = client.openFile(uri, "r", null);
                    if(pFd == null){
                        Log.w(TAG, "Can't open file: " +  uri);
                        return null;
                    }
                    FileDescriptor fd = pFd.getFileDescriptor();
                    if(fd == null){
                        Log.w(TAG, "Can't get fd");
                        return null;
                    }
                    file = pFd.getFile(fd);
                    if(file == null){
                        Log.w(TAG, "Can't get file ");
                        return null;
                    }
               } catch(IOException|RemoteException e) {
                  Log.d(TAG,"Exception " + e);
                  return null;
               } finally {
                   IoUtils.closeQuietly(pFd);
               }
                return file.getPath();
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = getUri(type);
                if (null == contentUri) return null;

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri,
            String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

}
