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
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

import com.android.camera.data.FilmstripContentQueries.CursorToFilmstripItemFactory;
import com.android.camera.debug.Log;

import java.util.List;

public class VideoItemFactory implements CursorToFilmstripItemFactory<VideoItem> {
    private static final Log.Tag TAG = new Log.Tag("VideoItemFact");
    private static final String QUERY_ORDER = MediaStore.Video.VideoColumns.DATE_TAKEN
          + " DESC, " + MediaStore.Video.VideoColumns._ID + " DESC";

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final VideoDataFactory mVideoDataFactory;

    public VideoItemFactory(Context context,
          ContentResolver contentResolver, VideoDataFactory videoDataFactory) {
        mContext = context;
        mContentResolver = contentResolver;
        mVideoDataFactory = videoDataFactory;
    }

    @Override
    public VideoItem get(Cursor c) {
        VideoItemData data = mVideoDataFactory.fromCursor(c);
        if (data != null) {
            /* Fix bug 585183 Adds new features 3D recording @{ */
            if (data.getFilePath() != null) {
                int index = data.getFilePath().lastIndexOf('.');
                if (index != -1) {
                    if (data.getFilePath().substring(0, index).endsWith("_3DVideo")) {
                        return new VideoItem3D(mContext, data, this);
                    }
                }
            }
            /* @} */
            /* SPRD:1227522, video thumbnail does not refresh when delete form native gallery
             * natvie gallery will update is_pending to 1 when delete pic, not delete database item right now
             @{ */
            int isPending = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_PENDING));
            if (1 == isPending) {
                Log.w(TAG, "data is pending, return null");
                return null;
            }
            /* @} */
            return new VideoItem(mContext, data, this);
        } else {
            final int dataIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            Log.w(TAG, "skipping item with null data, returning null for item:" + c.getString(dataIndex));
            return null;
        }
    }

    /** Query for a single video data item */
    public VideoItem get(Uri uri) {
        VideoItem newData = null;
        Cursor c = mContext.getContentResolver().query(uri, VideoDataQuery.QUERY_PROJECTION, null,
              null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                newData = get(c);
            }
            c.close();
        }

        return newData;
    }

    /** Query for a single data item */
    public VideoItem queryContentUri(Uri uri) {
        // TODO: Consider refactoring this, this approach may be slow.
        return queryLatest();
    }

    /** Query for the latest video data item */
    public VideoItem queryLatest() {
        return (VideoItem)(FilmstripContentQueries
                        .forCameraPathLimitLatest(mContentResolver, VideoDataQuery.CONTENT_URI, VideoDataQuery.QUERY_PROJECTION, QUERY_ORDER, this));
    }
}
