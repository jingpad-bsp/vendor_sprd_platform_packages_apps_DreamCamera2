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

package com.android.camera.filmstrip;

import android.view.View;

import com.android.camera.app.CameraAppUI;
import com.android.camera.data.FilmstripItem;

/**
 * An interface which defines the controller of filmstrip.
 * A filmstrip has 4 states:
 * <ol>
 *     <li>Filmstrip</li>
 *     Images are scaled down and the user can navigate quickly by swiping.
 *     Action bar and controls are shown.
 *     <li>Full-screen</li>
 *     One single image occupies the whole screen. Action bar and controls are
 *     hidden.
 *     <li>Zoom view</li>
 *     Zoom in to view the details of one single image.
 * </ol>
 * Only the following state transitions can happen:
 * <ol>
 * <li>filmstrip --> full-screen</li>
 * <li>full-screen --> filmstrip</li>
 * <li>full-screen --> full-screen with UIs</li>
 * <li>full-screen --> zoom view</li>
 * <li>zoom view --> full-screen</li>
 * </ol>
 *
 * Upon entering/leaving each of the states, the
 * {@link com.android.camera.filmstrip.FilmstripController.FilmstripListener} will be notified.
 */
public interface FilmstripController {

    /**
     * Sets the listener for filmstrip events.
     *
     * @param listener
     */
    public void setListener(FilmstripListener listener);

    /**
     * @return The ID of the current item, or -1.
     */
    public int getCurrentAdapterIndex();

    /**
     * Sets the {@link FilmstripDataAdapter}.
     */
    public void setDataAdapter(FilmstripDataAdapter adapter);

    /**
     * An interface which defines the FilmStripView UI action listener.
     */
    interface FilmstripListener {

        /**
         * Called when all the data has been reloaded.
         */
        public void onDataReloaded();

        /**
         * Called when data is updated.
         *
         * @param adapterIndex The ID of the updated data.
         */
        public void onDataUpdated(int adapterIndex);

        /**
         * The callback when the data focus changed.
         *
         * @param prevIndex The ID of the previously focused data or {@code -1} if
         *                   none.
         * @param newIndex The ID of the focused data of {@code -1} if none.
         */
        public void onDataFocusChanged(int prevIndex, int newIndex);
    }
}
