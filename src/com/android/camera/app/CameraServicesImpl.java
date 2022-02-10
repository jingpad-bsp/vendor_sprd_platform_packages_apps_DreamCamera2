/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.camera.app;

import android.content.Context;

import com.android.camera.MediaSaverImpl;
import com.android.camera.util.AndroidContext;

/**
 * Functionality available to all modules and services.
 */
public class CameraServicesImpl implements CameraServices {
    /**
     * Fast, thread safe singleton initialization.
     */
    private static class Singleton {
        private static final CameraServicesImpl INSTANCE = new CameraServicesImpl(
              AndroidContext.instance().get());
    }

    /**
     * @return a single instance of of the global camera services.
     */
    public static CameraServicesImpl instance() {
        return Singleton.INSTANCE;
    }

    private final MediaSaver mMediaSaver;
    private final MemoryManagerImpl mMemoryManager;
    private final MotionManager mMotionManager;

    private CameraServicesImpl(Context context) {
        /*
         * SPRD: Fix Bug 474843,  New feature of Filter. @{
         * Original Android code:
        mMediaSaver = new MediaSaverImpl(context.getContentResolver());
        */
        mMediaSaver = new MediaSaverImpl(context);
        /* @} */

        mMemoryManager = MemoryManagerImpl.create(context, mMediaSaver);

        mMotionManager = new MotionManager(context);
    }


    @Override
    public MemoryManager getMemoryManager() {
        return mMemoryManager;
    }

    @Override
    public MotionManager getMotionManager() {
        return mMotionManager;
    }

    @Override
    @Deprecated
    public MediaSaver getMediaSaver() {
        return mMediaSaver;
    }
}
