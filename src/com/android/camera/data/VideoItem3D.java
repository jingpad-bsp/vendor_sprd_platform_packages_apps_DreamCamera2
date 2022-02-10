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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.android.camera.debug.Log;
import com.android.camera2.R;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

/**
 * Backing data for a single 3d video displayed in the filmstrip.
 */
public class VideoItem3D extends VideoItem {


    private static final Log.Tag TAG = new Log.Tag("VideoItem3D");

    public VideoItem3D(Context context, VideoItemData data,
                       VideoItemFactory videoItemFactory) {
        super(context, data, videoItemFactory);
    }

    @Override
    public View getView(Optional<View> optionalView,
                        LocalFilmstripDataAdapter adapter, boolean isInProgress,
                        final VideoClickedCallback videoClickedCallback) {

        View view;
        VideoViewHolder viewHolder;

        if (!optionalView.isPresent()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.filmstrip_video_3d, null);
            view.setTag(R.id.mediadata_tag_viewtype, getItemViewType().ordinal());
            ImageView videoView = (ImageView) view.findViewById(R.id.video_view);
            ImageView playButton = (ImageView) view.findViewById(R.id.play_button_image);

            viewHolder = new VideoViewHolder(videoView, playButton);
            view.setTag(R.id.mediadata_tag_target_3d, viewHolder);
            return super.getView(Optional.of(view), adapter, isInProgress, videoClickedCallback);
        } else {
            return super.getView(optionalView, adapter, isInProgress, videoClickedCallback);
        }
    }

    protected VideoViewHolder getViewHolder(@Nonnull View view) {
        Object container = view.getTag(R.id.mediadata_tag_target_3d);
        if (container instanceof VideoViewHolder) {
            return (VideoViewHolder) container;
        }

        return null;
    }

    @Override
    public FilmstripItemType getItemViewType() {
        return FilmstripItemType.TDVIDEO;
    }
}
