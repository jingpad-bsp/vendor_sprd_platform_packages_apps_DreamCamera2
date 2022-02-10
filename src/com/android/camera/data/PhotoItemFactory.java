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
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.android.camera.data.FilmstripContentQueries.CursorToFilmstripItemFactory;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;

import java.util.List;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

public class PhotoItemFactory implements CursorToFilmstripItemFactory<PhotoItem> {
    private static final Log.Tag TAG = new Log.Tag("PhotoItemFact");

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final PhotoDataFactory mPhotoDataFactory;

    public PhotoItemFactory(Context context,
          ContentResolver contentResolver, PhotoDataFactory photoDataFactory) {
        mContext = context;
        mContentResolver = contentResolver;
        mPhotoDataFactory = photoDataFactory;
    }

    @Override
    public PhotoItem get(Cursor c) {
        /*
         * SPRD: Fix bug 535110, Photo voice record. @{
         * Original Android code :
        FilmstripItemData data = mPhotoDataFactory.fromCursor(c);
         */
        PhotoItemData data = mPhotoDataFactory.fromCursor(mContentResolver, c);
        /* @} */
        if (data != null) {
            /* SPRD: Fix bug 656429, use data.getMimeType() directly and handle burst images first to reduce time of updating thumbnail @{ */
            int fileTag = c.getInt(c.getColumnIndexOrThrow("file_flag"));
            /* @} */

            if (data.getFilePath() != null) {
                int index = data.getFilePath().lastIndexOf('.');
                if (index != -1) {
                    if (data.getFilePath().substring(0, index).endsWith("_3DPhoto")) {
                        return new PhotoItem3D(mContext, data, this);
                    }
                }
            }
            /* @} */
            /* SPRD: Fix bug737255*/
            if (fileTag == FilmstripItemData.TYPE_THUMBNAIL) {
                return null;
            }
            /* SPRD:fix bug1182127 filter is pending data for secure camera@{ */
            int isPending = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_PENDING));
            if (1 == isPending) {
                Log.w(TAG, "data is pending, return null");
                return null;
            }
            /* @} */
            return new PhotoItem(mContext, data, this);
        } else {
            final int dataIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            Log.w(TAG, "skipping item with null data, returning null for item:" + c.getString(dataIndex));
            return null;
        }
    }

    public PhotoItem get(Uri uri) {
        PhotoItem newData = null;
        Cursor c = null;
        try {
            c = mContentResolver.query(uri, PhotoDataQuery.QUERY_PROJECTION,
                    null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    newData = get(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();//SPRD:fix bug1173825
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return newData;
    }

    /** Query for a single data item */
    public PhotoItem queryContentUri(Uri uri) {
        // TODO: Consider refactoring this, this approach may be slow.
        return queryLatest();
    }

    /** Query for the latest photo data item */
    public PhotoItem queryLatest() {
        return (PhotoItem)(FilmstripContentQueries
                        .forCameraPathLimitLatest(mContentResolver, PhotoDataQuery.CONTENT_URI, PhotoDataQuery.QUERY_PROJECTION, PhotoDataQuery.THUMBNAIL_QUERY_ORDER, this));
    }
}
