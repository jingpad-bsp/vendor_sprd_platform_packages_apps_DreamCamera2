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
import android.os.AsyncTask;
import android.view.View;

import com.android.camera.Storage;
import com.android.camera.data.FilmstripItem.VideoClickedCallback;
import com.android.camera.debug.Log;
import com.android.camera.util.Callback;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * A {@link LocalFilmstripDataAdapter} that provides data in the camera folder.
 */
public class CameraFilmstripDataAdapter implements LocalFilmstripDataAdapter {
    private static final Log.Tag TAG = new Log.Tag("CameraDataAdapter");

    /* SPRD:Fix bug 398945 start */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
    public static final Executor THREAD_POOL_EXECUTOR
    = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    /* SPRD:Fix bug 398945 end */
    private static ThreadPoolExecutor QUERY_TASK_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    private static final int DEFAULT_DECODE_SIZE = 1600;

    private final Context mContext;
    private final PhotoItemFactory mPhotoItemFactory;
    private final VideoItemFactory mVideoItemFactory;

    private FilmstripItemList mFilmstripItems;

    private Listener mListener;
    private FilmstripItemListener mFilmstripItemListener;

    private int mSuggestedWidth = DEFAULT_DECODE_SIZE;
    private int mSuggestedHeight = DEFAULT_DECODE_SIZE;
    private long mLastPhotoId = FilmstripItemBase.QUERY_ALL_MEDIA_ID;

    public CameraFilmstripDataAdapter(Context context,
            PhotoItemFactory photoItemFactory, VideoItemFactory videoItemFactory) {
        mContext = context;
        mFilmstripItems = new FilmstripItemList();
        mPhotoItemFactory = photoItemFactory;
        mVideoItemFactory = videoItemFactory;
    }

    @Override
    public void setLocalDataListener(FilmstripItemListener listener) {
        mFilmstripItemListener = listener;
    }

    @Override
    public void requestLoad(Callback<Void> onDone) {
        QueryTask qtask = new QueryTask(onDone);
        qtask.executeOnExecutor(QUERY_TASK_EXECUTOR, mContext);
    }

    @Override
    public AsyncTask updateMetadataAt(int index) {
        return updateMetadataAt(index, false);
    }

    private AsyncTask updateMetadataAt(int index, boolean forceItemUpdate) {
        MetadataUpdateTask result = new MetadataUpdateTask(forceItemUpdate);
        /**
         * SPRD:fix bug 398945
        result.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, index);
         */
        result.executeOnExecutor(THREAD_POOL_EXECUTOR, index);
        return result;
    }

    @Override
    public boolean isMetadataUpdatedAt(int index) {
        if (index < 0 || index >= mFilmstripItems.size()) {
            return true;
        }
        return mFilmstripItems.get(index).getMetadata().isLoaded();
    }

    @Override
    public int getItemViewType(int index) {
        if (index < 0 || index >= mFilmstripItems.size()) {
            return -1;
        }

        return mFilmstripItems.get(index).getItemViewType().ordinal();
    }

    @Override
    public FilmstripItem getItemAt(int index) {
        if (index < 0 || index >= mFilmstripItems.size()) {
            return null;
        }
        return mFilmstripItems.get(index);
    }

    @Override
    public int getTotalNumber() {
        return mFilmstripItems.size();
    }

    @Override
    public FilmstripItem getFilmstripItemAt(int index) {
        return getItemAt(index);
    }

    @Override
    public View getView(View recycled, int index,
            VideoClickedCallback videoClickedCallback) {
        if (index >= mFilmstripItems.size() || index < 0) {
            return null;
        }

        FilmstripItem item = mFilmstripItems.get(index);
        item.setSuggestedSize(mSuggestedWidth, mSuggestedHeight);

        return item.getView(Optional.fromNullable(recycled), this, /* inProgress */ false,
              videoClickedCallback);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
        if (mFilmstripItems.size() != 0) {
            mListener.onFilmstripItemLoaded();
        }
    }

    @Override
    public void removeAt(int index) {
        FilmstripItem d = mFilmstripItems.remove(index);
        if (d == null) {
            return;
        }
    }

    @Override
    public boolean addOrUpdate(FilmstripItem item) {
        final Uri uri = item.getData().getUri();
        if (FilmstripItemData.TYPE_THUMBNAIL == item.getData().getFileTag()) {
            Log.v(TAG, "addOrUpdate new photos fileTag is TYPE_THUMBNAIL");
            return false;
        }
        int pos = findByContentUri(uri);
        if (pos != -1) {
            // a duplicate one, just do a substitute.
            Log.v(TAG, "found duplicate data: " + uri);
            updateItemAt(pos, item);
            return false;
        } else {
            // a new data.
            insertItem(item);
            return true;
        }
    }

    @Override
    public int findByContentUri(Uri uri) {
        // LocalDataList will return in O(1) if the uri is not contained.
        // Otherwise the performance is O(n), but this is acceptable as we will
        // most often call this to find an element at the beginning of the list.
        return mFilmstripItems.indexOf(uri);
    }

    @Override
    public void clear() {
        replaceItemList(new FilmstripItemList());
    }

    @Override
    public void refresh(Uri uri) {
        final int pos = findByContentUri(uri);
        if (pos == -1) {
            return;
        }

        FilmstripItem data = mFilmstripItems.get(pos);
        FilmstripItem refreshedData = data.refresh();

        // Refresh failed. Probably removed already.
        if (refreshedData == null && mListener != null) {
            removeAt(pos);
            mListener.onFilmstripItemRemoved(pos, data);
            return;
        }
        updateItemAt(pos, refreshedData);
    }

    @Override
    public void updateItemAt(final int pos, FilmstripItem item) {
        MetadataLoader.loadMetadata(mContext, item);// SPRD:Add for bug 669895
        mFilmstripItems.set(pos, item);
        updateMetadataAt(pos, true /* forceItemUpdate */);
    }

    private void insertItem(FilmstripItem item) {
        // Since this function is mostly for adding the newest data,
        // a simple linear search should yield the best performance over a
        // binary search.
        int pos = 0;
        Comparator<FilmstripItem> comp = new NewestFirstComparator(
                new Date());
        for (; pos < mFilmstripItems.size()
                && comp.compare(item, mFilmstripItems.get(pos)) > 0; pos++) {
        }
        mFilmstripItems.add(pos, item);
        if (mListener != null) {
            mListener.onFilmstripItemInserted(pos, item);
        }
    }

    /** Update all the data */
    private void replaceItemList(FilmstripItemList list) {
        if (list.size() == 0 && mFilmstripItems.size() == 0) {
            return;
        }

        boolean needReplace = false;

        if (mFilmstripItems != null && mFilmstripItems.size() == list.size()) {
            try {
                for (int i = 0; i < mFilmstripItems.size(); i++) {
                    if (!mFilmstripItems.get(i).getData().getUri()
                            .equals(list.get(i).getData().getUri())
                            || !mFilmstripItems.get(i).getData().getMimeType()
                                    .equals(list.get(i).getData().getMimeType())) {
                        needReplace = true;
                        break;
                    }
                }
            } catch (Exception e) {
                needReplace = true;
                e.printStackTrace();
            }
        } else {
            needReplace = true;
        }

        if (needReplace) {
            mFilmstripItems = list;
        }

        if (mListener != null) {
            mListener.onFilmstripItemLoaded();
        }
    }

    @Override
    public List<AsyncTask> preloadItems(List<Integer> items) {
        List<AsyncTask> result = new ArrayList<>();
        for (Integer id : items) {
            if (!isMetadataUpdatedAt(id)) {
                result.add(updateMetadataAt(id));
            }
        }
        return result;
    }

    @Override
    public void cancelItems(List<AsyncTask> loadTokens) {
        for (AsyncTask asyncTask : loadTokens) {
            if (asyncTask != null) {
                asyncTask.cancel(false);
            }
        }
    }

    @Override
    public List<Integer> getItemsInRange(int startPosition, int endPosition) {
        List<Integer> result = new ArrayList<>();
        for (int i = Math.max(0, startPosition); i < endPosition; i++) {
            result.add(i);
        }
        return result;
    }

    @Override
    public int getCount() {
        return getTotalNumber();
    }

    @Override
    public void requestLoadSecure(Callback<Void> onDone) {
        RefreshSecureTask refreahTask = new RefreshSecureTask(onDone, mFilmstripItems);
        refreahTask.executeOnExecutor(THREAD_POOL_EXECUTOR, mContext.getContentResolver());
    }

    private class RefreshSecureTask extends AsyncTask<ContentResolver, Void, FilmstripItemList> {

        private FilmstripItemList mSecureData = null;
        private boolean mSelfChange = false;
        private final Callback<Void> mRefreshDoneCallback;
        public RefreshSecureTask(Callback<Void> doneCallback, FilmstripItemList itemList) {
            mSecureData = itemList;
            mRefreshDoneCallback = doneCallback;
        }

        @Override
        protected FilmstripItemList doInBackground(ContentResolver... arg0) {
            FilmstripItemList listTmp = new FilmstripItemList();
            synchronized(mSecureData){
                for(int k=0; k < mSecureData.size(); k++){
                    listTmp.add(mSecureData.get(k));
                }
            }

            ArrayList<Uri> removedItem = new ArrayList<Uri>();
            for(int i=0; i< listTmp.size(); i++){
                FilmstripItem item = listTmp.get(i);
                Uri itemUri = item.getData().getUri();
                if(itemUri == null) continue;
                FilmstripItem newData = item.refresh();
                if(newData == null){
                    removedItem.add(itemUri);
                    mSelfChange = true;
                }
            }
            listTmp.removeAll(removedItem);
            return listTmp;
        }

        @Override
        protected void onPostExecute(FilmstripItemList newData) {
            if(mSelfChange){
                mSelfChange = false;
                mFilmstripItems = newData;
                if(mRefreshDoneCallback != null){
                    mRefreshDoneCallback.onCallback(null);
                }
            }
        }
    }

    private class QueryTaskResult {
        public FilmstripItemList mFilmstripItemList;
        public long mLastPhotoId;

        public QueryTaskResult(FilmstripItemList filmstripItemList, long lastPhotoId) {
            mFilmstripItemList = filmstripItemList;
            mLastPhotoId = lastPhotoId;
        }
    }

    private class QueryTask extends AsyncTask<Context, Void, QueryTaskResult> {
        // The maximum number of data to load metadata for in a single task.
        private final Callback<Void> mDoneCallback;

        public QueryTask(Callback<Void> doneCallback) {
            mDoneCallback = doneCallback;
        }

        /**
         * Loads all the photo and video data in the camera folder in background
         * and combine them into one single list.
         *
         * @param contexts {@link Context} to load all the data.
         * @return An {@link CameraFilmstripDataAdapter.QueryTaskResult} containing
         *  all loaded data and the highest photo id in the dataset.
         */
        @Override
        protected QueryTaskResult doInBackground(Context... contexts) {
            final Context context = contexts[0];
            FilmstripItemList l = new FilmstripItemList();
            // Photos and videos
            PhotoItem photoData = mPhotoItemFactory.queryLatest();
            VideoItem videoData = mVideoItemFactory.queryLatest();

            long lastPhotoId = FilmstripItemBase.QUERY_ALL_MEDIA_ID;

            if(videoData == null && photoData == null) {
                // do nothing , return directly
            }
            else {
                Date videoDate = (videoData == null) ? new Date(0L) : videoData.getData().getCreationDate();
                Date photoDate = (photoData == null) ? new Date(0L) : photoData.getData().getCreationDate();

                if(videoData == null){ //videoData is null select photoData
                    l.add(photoData);
                } else if(photoData == null){//photoData is null select videoData
                    l.add(videoData);
                }else {//photoData & videoData all not-null ,select the latest
                    if (videoDate.after(photoDate))
                        l.add(videoData);
                    else
                        l.add(photoData);
                }
                FilmstripItem data = l.get(0);
                lastPhotoId = data.getData().getContentId();
                MetadataLoader.loadMetadata(context, data);
            }

            return new QueryTaskResult(l, lastPhotoId);
        }

        @Override
        protected void onPostExecute(QueryTaskResult result) {
            // Since we're wiping away all of our data, we should always replace any existing last
            // photo id with the new one we just obtained so it matches the data we're showing.
            mLastPhotoId = result.mLastPhotoId;
            replaceItemList(result.mFilmstripItemList);
            if (mDoneCallback != null) {
                mDoneCallback.onCallback(null);
            }
        }
    }

    private class MetadataUpdateTask extends AsyncTask<Integer, Void, List<Integer> > {
        private final boolean mForceUpdate;

        MetadataUpdateTask(boolean forceUpdate) {
            super();
            mForceUpdate = forceUpdate;
        }

        MetadataUpdateTask() {
            this(false);
        }

        @Override
        protected List<Integer> doInBackground(Integer... dataId) {
            List<Integer> updatedList = new ArrayList<>();
            for (Integer id : dataId) {
                if (id < 0 || id >= mFilmstripItems.size()) {
                    continue;
                }
                final FilmstripItem data = mFilmstripItems.get(id);
                if (data != null && (MetadataLoader.loadMetadata(mContext, data) || mForceUpdate)) {//SPRD:fix bug610488
                    updatedList.add(id);
                }
            }
            return updatedList;
        }

        @Override
        protected void onPostExecute(final List<Integer> updatedData) {
            // Since the metadata will affect the width and height of the data
            // if it's a video, we need to notify the DataAdapter listener
            // because ImageData.getWidth() and ImageData.getHeight() now may
            // return different values due to the metadata.
            if (mListener != null) {
                mListener.onFilmstripItemUpdated(new UpdateReporter() {
                    @Override
                    public boolean isDataRemoved(int index) {
                        return false;
                    }

                    @Override
                    public boolean isDataUpdated(int index) {
                        return updatedData.contains(index);
                    }
                });
            }
            if (mFilmstripItemListener == null) {
                return;
            }
            mFilmstripItemListener.onMetadataUpdated(updatedData);
        }
    }

    /* SPRD:fix bug 619231 thumbnail not sync @{ */
    @Override
    public long getLastPhotoId() {
        return mLastPhotoId;
    }
    /* @} */

    public void onDestroy() {
        if (QUERY_TASK_EXECUTOR != null && QUERY_TASK_EXECUTOR.getQueue() != null) {
            QUERY_TASK_EXECUTOR.getQueue().clear();
        }
    }

}
