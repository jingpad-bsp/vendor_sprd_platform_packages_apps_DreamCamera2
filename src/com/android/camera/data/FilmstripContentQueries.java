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

package com.android.camera.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.android.camera.Storage;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.sprd.camera.storagepath.StorageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of queries for loading data from a content resolver.
 */
public class FilmstripContentQueries {
    private static final Log.Tag TAG = new Log.Tag("LocalDataQuery");
    private static final String CAMERA_PATH = Storage.DIRECTORY + "%";
    private static final String SELECT_BY_PATH = MediaStore.MediaColumns.DATA + " LIKE ?";

    public interface CursorToFilmstripItemFactory<I extends FilmstripItem> {

        /**
         * Convert a cursor at a given location to a Local Data object.
         *
         * @param cursor the current cursor state.
         * @return a LocalData object that represents the current cursor state.
         */
        public I get(Cursor cursor);
    }

    /**
     * Query latest video or photo from camera storage directory and convert it to local data
     * objects.
     *
     * @param contentResolver to resolve content with.
     * @param contentUri to resolve an item at
     * @param projection the columns to extract
     * @param minimumId the lower bound of results
     * @param orderBy the order by clause
     * @param factory an object that can turn a given cursor into a LocalData object.
     * @return A LocalData object that satisfy the query.
     */
    public static <I extends FilmstripItem> FilmstripItem forCameraPathLimitLatest(ContentResolver contentResolver,
            Uri contentUri, String[] projection, String orderBy, CursorToFilmstripItemFactory<I> factory) {

        String selection = null;
        StorageUtil storageutil = StorageUtil.getInstance();
        String cameraPath = "%" + storageutil.DEFAULT_DIR+ "%";
        String[] selectionArgs = null;
        orderBy = orderBy + " LIMIT 1";

        boolean supportTDVideoOrPhoto = false;
        if (CameraUtil.isTDVideoEnable() || CameraUtil.isTDPhotoEnable()) {
            supportTDVideoOrPhoto = true;
        }

        if (!supportTDVideoOrPhoto) {
            if(contentUri == PhotoDataQuery.CONTENT_URI) {
                selection = PhotoDataQuery.SELECT_BY_FILE_FLAG + " AND " + SELECT_BY_PATH;
                selectionArgs = new String[] {Integer.toString(FilmstripItemData.TYPE_BURST) , cameraPath};
            }
            else {
                selection = "(" + SELECT_BY_PATH + ")";
                selectionArgs = new String[] {cameraPath};
            }
        } else {
            String cameraPath3D = "%" + storageutil.DEFAULT_DIR_3D+ "%";
            if(contentUri == PhotoDataQuery.CONTENT_URI) {
                selection = PhotoDataQuery.SELECT_BY_FILE_FLAG + " AND (" + SELECT_BY_PATH + " OR " + SELECT_BY_PATH + ")";
                selectionArgs = new String[] {Integer.toString(FilmstripItemData.TYPE_BURST) , cameraPath , cameraPath3D};
            }
            else {
                selection = "(" + SELECT_BY_PATH + " OR " + SELECT_BY_PATH + ")";
                selectionArgs = new String[] {cameraPath , cameraPath3D};
            }
        }
        Cursor cursor;
        try {
            cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, orderBy);
            int counts = cursor.getCount();
            Log.i(TAG, "query finished , records count = " + counts);
            if(counts < 1) {
                cursor.close();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        cursor.moveToNext();
        FilmstripItem item = null;
        try {
            item = factory.get(cursor);
        } catch (IllegalStateException e){
            Log.e(TAG,"the main thread may be exited");
        }

        cursor.close();
        return item;
    }
}
