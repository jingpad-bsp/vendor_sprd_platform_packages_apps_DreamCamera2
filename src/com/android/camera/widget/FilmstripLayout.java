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

package com.android.camera.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera2.R;

/**
 * A {@link android.widget.FrameLayout} used for the parent layout of a
 * {@link com.android.camera.widget.FilmstripView} to support animating in/out the
 * filmstrip.
 */
public class FilmstripLayout extends FrameLayout implements FilmstripContentPanel {

    /**
     * The layout containing the {@link com.android.camera.widget.FilmstripView}
     * and other controls.
     */

    public FilmstripLayout(Context context) {
        super(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilmstripLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
