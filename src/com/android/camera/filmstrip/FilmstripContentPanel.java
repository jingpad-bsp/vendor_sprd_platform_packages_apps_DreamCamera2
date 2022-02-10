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

import com.android.camera.widget.FilmstripLayout;

/**
 * The filmstrip panel holding the filmstrip and other controls/widgets.
 */
public interface FilmstripContentPanel {
    /**
     * An listener interface extending {@link
     * com.android.camera.filmstrip.FilmstripController.FilmstripListener} defining extra callbacks
     * for filmstrip being shown and hidden.
     */
    interface Listener extends FilmstripController.FilmstripListener {

    }
}
