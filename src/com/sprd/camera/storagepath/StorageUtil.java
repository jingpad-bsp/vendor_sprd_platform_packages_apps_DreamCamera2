package com.sprd.camera.storagepath;

import android.content.ContentResolver;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.android.camera.data.FilmstripItemData;
import com.android.camera.settings.Keys;
import com.dream.camera.settings.DataModuleBasic;
import com.sprd.camera.storagepath.StorageUtilProxy;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.provider.Settings;
import android.content.ContentResolver;
/*Sprd added: functions with Storage path, add Image, and so on.
 *
 */
public class StorageUtil {
    public static final String DCIM = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static final String DIRECTORY = DCIM + "/Camera";
    // SPRD: Fix bug 602360 that keep 2d media separate from 3d media
    public static final String DEFAULT_DIR_3D = "/DCIM/3DCamera";
    public static final String KEY_DEFAULT_INTERNAL = "Internal";
    public static final String KEY_DEFAULT_EXTERNAL = "External";
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String GIF_POSTFIX = ".gif";
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    /*
     * For dream camera side panel icon.
     * @{
     */
    public final static int UNKNOW = -1;
    public final static int EXTERNAL = 0;
    public final static int INTERNAL = 1;
    public final static int USB = 2;
    private static final String TAG = "StorageUtil";
    public static final String DEFAULT_DIR = "/DCIM/Camera";
    /* @} */
    /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
    private static StorageUtil mInstance;
    private DataModuleBasic mDataModuleCamera;
    private String mStorage;
    public ContentResolver mContentResolver;

    public static synchronized StorageUtil getInstance() {
        if (mInstance == null) {
            mInstance = new StorageUtil();
        }
        return mInstance;
    }


    public Uri getStorageImagesUri(String storagePath){
        if (storagePath.equals(KEY_DEFAULT_INTERNAL) || isInternalPath(storagePath)) {
            return android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        return android.provider.MediaStore.Images.Media.getContentUri(getExactStorageName(storagePath).toLowerCase());
    }

    public Uri getStorageVideoUri(String storagePath){
        if (storagePath.equals(KEY_DEFAULT_INTERNAL) || isInternalPath(storagePath)) {
            return android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        return android.provider.MediaStore.Video.Media.getContentUri(getExactStorageName(storagePath).toLowerCase());
    }

    public Uri getCurrentAccessUri(String storagePath) {
        if (storagePath.equals(KEY_DEFAULT_INTERNAL) || isInternalPath(storagePath)) {
            return null;
        }
        String exactStorageName = getExactStorageName(storagePath);
        List<UriPermission> uriPermissions = mContentResolver.getPersistedUriPermissions();
        Log.i(TAG, "getCurrentAccessUri exactStorageName " + exactStorageName);
        for (UriPermission permission : uriPermissions) {
            Log.i(TAG, "getCurrentAccessUri permission: " + permission.toString());
            if (exactStorageName != null && permission.getUri().toString().contains(exactStorageName)) {
                Log.i(TAG, "getCurrentAccessUri return " + permission.getUri());
                return permission.getUri();
            }
        }
        Log.i(TAG, "getCurrentAccessUri return null");
        return null;
    }

    private String getExactStorageName(String storagePath) {
        Log.i(TAG, "getExactStorageName, storagePath: " + storagePath);
        String path;
        String[] pathName;
        if (storagePath.equals(KEY_DEFAULT_EXTERNAL) || isExternalPath(storagePath)) {
            path = StorageUtilProxy.getExternalStoragePath().toString();
            pathName = path.split("/");
            return pathName[pathName.length - 1];
        } else {
            Log.i(TAG, "split length: " + storagePath.split("/").length);
            if (storagePath.split("/").length < 2) {
                path = MultiStorage.getUsbStoragePath(storagePath);
            } else {
                path = storagePath;
            }
            if (path != null) {
                pathName = path.split("/");
                return pathName[2];
            }
        }
        return null;
    }

    /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
    public static boolean getStoragePathState(String storage) {
        if (KEY_DEFAULT_EXTERNAL.equals(storage)) {
            return Environment.MEDIA_MOUNTED.equals(StorageUtilProxy.getExternalStoragePathState());
        } else if (KEY_DEFAULT_INTERNAL.equals(storage)) {
            return Environment.MEDIA_MOUNTED.equals(StorageUtilProxy.getInternalStoragePathState());
        } else {
            /* SPRD: Fix bug 572473 add for usb storage support @{ */
            String usbStoragePath = MultiStorage.getUsbStoragePath(storage);
            if (usbStoragePath != null) {
                return Environment.MEDIA_MOUNTED.equals(
                        StorageUtilProxy.getUsbdiskVolumeState(new File(usbStoragePath)));
            } else {
                return false;
            }
            /* @} */
        }
    }

    public static Map<String, String> supportedRootDirectory() {
        Map<String, String> result = null;
        String internal = (getStoragePathState(KEY_DEFAULT_INTERNAL)
                ? StorageUtilProxy.getInternalStoragePath().getAbsolutePath() : null);
        String external = (getStoragePathState(KEY_DEFAULT_EXTERNAL)
                ? StorageUtilProxy.getExternalStoragePath().getAbsolutePath() : null);

        // result = new HashMap<String, String>(VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
        result = new HashMap<String, String>();
        result.put(KEY_DEFAULT_INTERNAL, internal);
        result.put(KEY_DEFAULT_EXTERNAL, external);

        // SPRD: Fix bug 572473 add for usb storage support
        Map<String, String> usbMap = MultiStorage.getUsbStorageMap();
        if (usbMap != null) {
            result.putAll(usbMap);
        }
        /* @} */
        return result;
    }

    public static synchronized final String getImageBucketId(String filePath) {
        return String.valueOf(filePath.toLowerCase().hashCode());
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.i(TAG, "Failed to delete image: " + uri);
        }
    }

    public void initialize(DataModuleBasic dataModuleCamera,ContentResolver contentResolver) {
        mDataModuleCamera = dataModuleCamera;
        mContentResolver = contentResolver;
    }

    /**
     * get current storage path
     *
     * @return string of current camera storage path
     */
    public String getCurrentStorage() {
        if (mDataModuleCamera != null) {
            return mDataModuleCamera.getString(Keys.KEY_CAMERA_STORAGE_PATH, "");
        }
        return null;
    }

    public long getInternalAvaliableSpace(){
        String path = getFileDir();
        String state = null;
        path = getInternalFileDir();
        if (path == null) {
            Log.e(TAG,"getInternalAvaliableSpace: internal path is null");
            return UNAVAILABLE;
        }

        state = StorageUtilProxy.getInternalStoragePathState();
        if (Environment.MEDIA_CHECKING.equals(state)) {
            Log.e(TAG,"getInternalAvaliableSpace: internal state is MEDIA_CHECKING");
            return PREPARING;
        }
        File dir = new File(path);
        dir.mkdirs();
        /*Bug 549528 insert SD card with memory space is insufficient. @{ */
        if (dir.exists() && (!dir.isDirectory() || !dir.canWrite())) {
                Log.e(TAG,"getInternalAvaliableSpace: internal dir can`t write");
                return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(path.replace(DEFAULT_DIR, "")); /*Bug 549528 @} */
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
        } catch (Exception e) {
            Log.i(TAG, "Fail to access storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * get current storage available space
     *
     * @return long the current storage available space size
     */
    public long getAvailableSpace() {
        String path = getFileDir();
        String state = null;
        Map<String, String> roots = supportedRootDirectory();
        String internal = roots.get(KEY_DEFAULT_INTERNAL);
        String external = roots.get(KEY_DEFAULT_EXTERNAL);
        //bug521124 there is no edited photo
        // if external storage is available but internal storage disable, force change the storage to external
        if (external != null && (!isStorageSetting() || path == null)) {
            forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
            path = getFileDir();
        }
//        else if (external == null) {
//            forceUpdateStorageSetting(KEY_DEFAULT_INTERNAL);
//            path = getFileDir();
//        }

        if (path == null) {
            return UNAVAILABLE;
        }

        // judge the path state
        if (internal != null && path.contains(internal)) {
            state = StorageUtilProxy.getInternalStoragePathState();
        } else if (external != null && path.contains(external)) {
            state = StorageUtilProxy.getExternalStoragePathState();
        }

        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        File dir = new File(path);
        if (internal != null && path.contains(internal)) {
            dir.mkdirs();
            /*Bug 549528 insert SD card with memory space is insufficient. @{ */
            if (dir.exists() && (!dir.isDirectory() || !dir.canWrite())) {
                return UNAVAILABLE;
            }
        } else if (!dir.exists() && (getCurrentAccessUri(path) != null)) {
            ExternalStorageUtil externalUtil = ExternalStorageUtil.getInstance();
            externalUtil.createDir(mContentResolver, getCurrentAccessUri(path), Environment.DIRECTORY_DCIM, "Camera", dir);
        }
        try {
            StatFs stat = new StatFs(path.replace(DEFAULT_DIR, "")); /*Bug 549528 @} */
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
        } catch (Exception e) {
            Log.i(TAG, "Fail to access storage", e);
        }
        return UNKNOWN_SIZE;
    }
    // SPRD: Fix bug 602360 that keep 2d media separate from 3d media
    public String getFileDirFor3D() {
        /* SPRD: Fix bug 602360 that keep 2d media separate from 3d media @{ */
        String path = getFileDir(DEFAULT_DIR_3D);
        boolean result = new File(path).mkdir();
        if (!result) {
            Log.d(TAG, "create DCIM/3DCamera failed");
        }
        /* @} */
        return path;
    }

    // SPRD: Fix bug 602360 that keep 2d media separate from 3d media
    public String getFileDir() {
        return getFileDir(DEFAULT_DIR);
    }
    public String getInternalFileDir(){
        return getStoragePathState(KEY_DEFAULT_INTERNAL) ?
                StorageUtilProxy.getInternalStoragePath().toString() + DEFAULT_DIR : null;
    }
    public String getFileDir(final String defaultDir) {
        String currentStorage = getCurrentStorage();
        if (KEY_DEFAULT_INTERNAL.equals(currentStorage)) {
            /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
            return getStoragePathState(KEY_DEFAULT_INTERNAL) ?
                    StorageUtilProxy.getInternalStoragePath().toString() + defaultDir : null;
        } else if (KEY_DEFAULT_EXTERNAL.equals(currentStorage)) {
            /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
            if (getStoragePathState(KEY_DEFAULT_EXTERNAL)) {
                return StorageUtilProxy.getExternalStoragePath().toString() + defaultDir;
            } else {
                forceUpdateStorageSetting(KEY_DEFAULT_INTERNAL);
                return getFileDir();
            }
        } else {
            /* SPRD: Fix bug 494188 No SD card, and can not open Camera and show error message */
            /* SPRD: Fix bug 572473 add for usb storage support */
            if (getStoragePathState(currentStorage)) {
                String FileDir = MultiStorage.getUsbStoragePath(currentStorage);
                if (FileDir != null) {
                    return FileDir + defaultDir;
                } else {
                    forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
                    return getFileDir();
                }
            } else {
//                forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
//                return getFileDir();
                return null;
            }
        }
    }

    public String generateFilePath(String title, String mimeType) {
        String extension = null;

        // SPRD: Fix bug 602360 that keep 2d media separate from 3d media
        if (FilmstripItemData.MIME_TYPE_JPEG.equals(mimeType)
                || FilmstripItemData.MIME_TYPE_3D_JPEG.equals(mimeType)
                || FilmstripItemData.MIME_TYPE_JPEG_THUMB.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else if (FilmstripItemData.MIME_TYPE_GIF.equals(mimeType)) {
            extension = GIF_POSTFIX;
        } else {
            Log.w(TAG, " Invalid mimeType:" + mimeType);
            return null;
        }

        String fileDir = getFileDir();
        if(fileDir == null){
            Log.w(TAG, " Invalid fileDir");
            return null;//SPRD:fix bug1183813
        }
     // SPRD: Fix bug 602360 that keep 2d media separate from 3d media 227
        String filePath;
        if (FilmstripItemData.MIME_TYPE_3D_JPEG.equals(mimeType)) {
            filePath = (new File(getFileDirFor3D(), title + extension)).getAbsolutePath();
        } else {
            filePath = (new File(getFileDir(), title + extension)).getAbsolutePath();
        }
        Log.i(TAG, "generateFilePath FileDir = " + getFileDir()
                + " title = " + title
                + "path = " + filePath);
        return filePath;
    }
    //Feature bug896062 store burst picture in emmc
    public String generateInternalFilePath(String title, String mimeType) {
        String extension = null;

        if (FilmstripItemData.MIME_TYPE_JPEG.equals(mimeType)
                || FilmstripItemData.MIME_TYPE_3D_JPEG.equals(mimeType)
                || FilmstripItemData.MIME_TYPE_JPEG_THUMB.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else {
            Log.w(TAG, " Invalid mimeType:" + mimeType);
            return null;
        }

        String fileDir = getInternalFileDir();
        if(fileDir == null){
            Log.w(TAG, " Invalid fileDir");
            return null;//SPRD:fix bug1183813
        }
        String filePath;
        filePath = (new File(fileDir, title + extension)).getAbsolutePath();
        Log.i(TAG, "generateInternalFilePath FileDir = " + getInternalFileDir()
                + " title = " + title
                + "path = " + filePath);
        return filePath;
    }
    public void updateStorage() {
        String storage = getCurrentStorage();
        mStorage = storage;
    }

    public boolean isStorageUpdated() {
        if (mStorage == null) {
            mStorage = getCurrentStorage();
            return true;
        }
        return (!mStorage.equals(getCurrentStorage()));
    }

    /**
     * force update current storage path when the storage path has changed
     *
     * @param storage the name of path want to change
     */
    public void forceUpdateStorageSetting(String storage) {
        if (mDataModuleCamera != null) {
            mDataModuleCamera.set(Keys.KEY_CAMERA_STORAGE_PATH, storage);
        }
    }

    public void forceChangeStorageSetting(String storage) {
        if (mDataModuleCamera != null) {
            mDataModuleCamera.changeSettings(Keys.KEY_CAMERA_STORAGE_PATH, storage);
        }
    }

    /*SPRD:fix bug521124 there is no edited photo*/
    private boolean isStorageSetting() {
        if (mDataModuleCamera != null) {
            return mDataModuleCamera.isSet(Keys.KEY_CAMERA_STORAGE_PATH);
        }
        return false;
    }

    //SPRD:fix bug537451 pull sd card, edit and puzzle can not work.
    public String getStorageState() {
        String state = Environment.MEDIA_UNMOUNTED;
        /*SPRD: Fix bug 540799 @{ */
        String currentStorage = getCurrentStorage();
        if (currentStorage == null) {
            return state;
        }
        /* @} */
        switch (currentStorage) {
            case KEY_DEFAULT_INTERNAL:
                state = StorageUtilProxy.getInternalStoragePathState();
                break;
            case KEY_DEFAULT_EXTERNAL:
                state = StorageUtilProxy.getExternalStoragePathState();
                break;
            default:
                // SPRD: Fix bug 572473 add for usb storage support
                String usbStoragePath = MultiStorage.getUsbStoragePath(currentStorage);
                if (usbStoragePath != null) {
                    state = StorageUtilProxy.getUsbdiskVolumeState(new File(usbStoragePath));
                }
                break;
                /* @} */
        }
        return state;
    }

    public int getSideStorageState() {
        String currentStorage = getCurrentStorage();
        if (KEY_DEFAULT_INTERNAL.equals(currentStorage)) {
            return getStoragePathState(KEY_DEFAULT_INTERNAL) ? INTERNAL : UNKNOW;
        } else if (KEY_DEFAULT_EXTERNAL.equals(currentStorage)) {
            return getStoragePathState(KEY_DEFAULT_EXTERNAL) ? EXTERNAL :
                    getStoragePathState(KEY_DEFAULT_INTERNAL) ? INTERNAL : UNKNOW;
        } else {
            // for usb storage
            if (getStoragePathState(currentStorage)) {
                return USB;
            } else {
//                forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
//                return getSideStorageState();
                return UNKNOW;
            }
        }
    }
    /* @} */
    public boolean isInternalPath(String path) {
        String internal = (getStoragePathState(KEY_DEFAULT_INTERNAL)
                ? StorageUtilProxy.getInternalStoragePath().getAbsolutePath() : null);
        return (internal != null && path.contains(internal));
    }

    public boolean isExternalPath(String path) {
        String external = (getStoragePathState(KEY_DEFAULT_EXTERNAL)
                ? StorageUtilProxy.getExternalStoragePath().getAbsolutePath() : null);
        return (external != null && path.contains(external));
    }

    public String getFileName(File path) {
        String externalStoragePath = StorageUtilProxy.getExternalStoragePath().toString();
        return path.getAbsolutePath().substring(externalStoragePath.length());
    }
    /* SPRD: Fix bug 535110, Photo voice record. @{ */
    public String getPhotoVoiceDirectory() {
        String tmp = getInternalFileDir();
        if (null == tmp) return null;//CID 225699
        return tmp.substring(0, tmp.lastIndexOf("Camera")) + ".PhotoVoice";
    }
    /* @} */

}
