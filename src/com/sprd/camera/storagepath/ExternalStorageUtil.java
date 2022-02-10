package com.sprd.camera.storagepath;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by SPREADTRUM\na.hao on 18-8-7.
 */

public class ExternalStorageUtil {

    private static final String TAG = ExternalStorageUtil.class.getSimpleName();
    private static ExternalStorageUtil mInstance;
    private static final String DEFAULT_DIR = "/DCIM/Camera";

    public static synchronized ExternalStorageUtil getInstance() {
        if (mInstance == null) {
            mInstance = new ExternalStorageUtil();
        }
        return mInstance;
    }

    public Uri createDir(ContentResolver cr, Uri uri, String dirName, String subDir, File fileDir) {
        Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
        Uri dirDCIM;
        Uri dirCamera;
        Log.i(TAG, "createDir doc " + doc);
        try {
            if (!fileDir.getParentFile().exists()) {
                // create DCIM Document
                dirDCIM = DocumentsContract.createDocument(cr, doc, DocumentsContract.Document.MIME_TYPE_DIR, dirName);
            } else {
                dirDCIM = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri) + "/" + dirName);
            }
            //create Camera Document
            dirCamera = DocumentsContract.createDocument(cr, dirDCIM, DocumentsContract.Document.MIME_TYPE_DIR, subDir);
            Log.i(TAG, "createDir dirCamera " + dirCamera);
        } catch (Exception e) {
            dirDCIM = null;
            Log.e(TAG, "Create external directory failed.");
        }
        return dirDCIM;
    }

    //Sprd:fix bug952246
    public ParcelFileDescriptor createParcelFd(ContentResolver cr, Uri uri, String filePath, String mimetype){
        ParcelFileDescriptor pfd = null;
        String[] pathName = filePath.split("/");
        String picName = pathName[pathName.length - 1];
        try {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri) + DEFAULT_DIR);
            Uri pic = DocumentsContract.createDocument(cr, doc, mimetype, picName);
            Log.i(TAG, "createFileDescriptor pic: " + pic);
            pfd = cr.openFileDescriptor(pic, "rw");
        } catch (Exception e) {
            Log.e(TAG, "Create createFileDescriptor failed.");
            e.printStackTrace();
        }
        return pfd;
    }

    //Sprd:fix bug952246
    public Uri createDocUri(ContentResolver cr, Uri uri, String filePath){
        Uri pic = null;
        String[] pathName = filePath.split("/");
        String picName = pathName[pathName.length - 1];
        try {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri) + DEFAULT_DIR);
            pic = DocumentsContract.createDocument(cr, doc, "image/*", picName);
            Log.i(TAG, "createDocUri pic: " + pic);
        } catch (Exception e) {
            Log.e(TAG, "Create Doc Uri failed.");
            e.printStackTrace();
        }
        return pic;
    }

    public FileOutputStream createOutputStream(ParcelFileDescriptor pfd) {
        return new FileOutputStream(pfd.getFileDescriptor());
    }
    public FileInputStream createInputStream(ParcelFileDescriptor pfd) {
        return new FileInputStream(pfd.getFileDescriptor());
    }

    public boolean deleteDocument(ContentResolver cr, Uri uri, String filePath) {
        try {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri) + filePath);
            Log.i(TAG, "deleteDocument doc: " + doc);
            return DocumentsContract.deleteDocument(cr, doc);
        } catch (Exception e) {
            Log.i(TAG, "Delete external document failed.");
            return false;
        }
    }
    public Uri renameDocument(ContentResolver cr, Uri uri, String filePath, String newName) {
        try {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri) + filePath);
            Log.i(TAG, "renameDocument doc: " + doc);
            return DocumentsContract.renameDocument(cr, doc,newName);
        } catch (Exception e) {
            Log.i(TAG, "Rename external document failed.");
            return null;
        }
    }


    protected static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
                // ignored
            }
        }
    }
}

