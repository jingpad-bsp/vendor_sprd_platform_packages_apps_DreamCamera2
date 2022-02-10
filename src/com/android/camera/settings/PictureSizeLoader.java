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

package com.android.camera.settings;

import android.content.ContentResolver;
import android.content.Context;

import com.android.camera.settings.SettingsUtil.CameraDeviceSelector;
import com.android.camera.settings.SettingsUtil.SelectedVideoQualities;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.google.common.base.Optional;
import com.android.camera.util.CameraUtil;
import com.dream.camera.util.DreamUtil;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Loads the camera picture sizes that can be set by the user.
 * <p>
 * This class is compatible with pre-Lollipop since it uses the compatibility
 * layer to access the camera metadata.
 */
@ParametersAreNonnullByDefault
public class PictureSizeLoader {
    /**
     * Holds the sizes for the back- and front cameras which will be available
     * to the user for selection form settings.
     */
    @ParametersAreNonnullByDefault
    public static class PictureSizes {
        public List<Size> backCameraSizes;
        public List<Size> frontCameraSizes;
        public Optional<SelectedVideoQualities> videoQualitiesBack;
        public Optional<SelectedVideoQualities> videoQualitiesFront;
        // SPRD: Fix bug 585183 Adds new features 3D recording
        public Optional<SelectedVideoQualities> videoQualitiesFront3D = null;

        public Optional<SelectedVideoQualities> videoQualitiesBackMacro = null;


        PictureSizes(List<Size> backCameraSizes, List<Size> frontCameraSizes,
                Optional<SelectedVideoQualities> videoQualitiesBack,
                Optional<SelectedVideoQualities> videoQualitiesFront) {
            this.backCameraSizes = backCameraSizes;
            this.frontCameraSizes = frontCameraSizes;
            this.videoQualitiesBack = videoQualitiesBack;
            this.videoQualitiesFront = videoQualitiesFront;
        }
        /* SPRD: Fix bug 585183 Adds new features 3D recording @{ */
        PictureSizes(List<Size> backCameraSizes,
                     List<Size> frontCameraSizes,
                     Optional<SelectedVideoQualities> videoQualitiesBack,
                     Optional<SelectedVideoQualities> videoQualitiesFront,
                     Optional<SelectedVideoQualities> videoQualitiesFront3D,
                     Optional<SelectedVideoQualities> videoQualitiesBackMacro) {
            this.backCameraSizes = backCameraSizes;
            this.frontCameraSizes = frontCameraSizes;
            this.videoQualitiesBack = videoQualitiesBack;
            this.videoQualitiesFront = videoQualitiesFront;
            this.videoQualitiesFront3D = videoQualitiesFront3D;
            this.videoQualitiesBackMacro = videoQualitiesBackMacro;
        }
        /* @} */
        public PictureSizes() {

        }
    }

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final boolean mCachedOnly;

    /**
     * Initializes a new picture size loader.
     * <p>
     * This constructor will default to using the camera devices if the size
     * values were not found in cache.
     * 
     * @param context
     *            used to load caches sizes from preferences.
     */
    public PictureSizeLoader(Context context) {
        this(context, false);
    }

    /**
     * Initializes a new picture size loader.
     * 
     * @param context
     *            used to load caches sizes from preferences.
     * @param cachedOnly
     *            if set to true, this will only check the cache for sizes. If
     *            the cache is empty, this will NOT attempt to open the camera
     *            devices in order to obtain the sizes.
     */
    public PictureSizeLoader(Context context, boolean cachedOnly) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mCachedOnly = cachedOnly;
    }

    private static List<Size> delelteSize(List<Size> list)
    {
        int length = list.size();

        for (int i = 0; i < length; i++) {
            Size size = list.get(i);

            Log.d("skyy", "skyy PictureSizeLoader delelteSize, size.height() = " + size.height() + "; size.width() =  " + size.width());

            if ((float) size.width() / (float) size.height() > 1.7f)
            {
                list.remove(i);
                i--;
                length--;
            }
        }

        return list;
    }

    /**
     * Computes the list of picture sizes that should be displayed by settings.
     * <p>
     * For this it will open the camera devices to determine the available
     * sizes, if the sizes are not already cached. This is to be compatible with
     * devices running Camera API 1.
     * <p>
     * We then calculate the resolutions that should be available and in the end
     * filter it in case a resolution is on the blacklist for this device.
     */
    public PictureSizes computePictureSizes() {
        List<Size> backCameraSizes = computeSizesForCamera(-1, DreamUtil.BACK_CAMERA);
        List<Size> frontCameraSizes = computeSizesForCamera(-1, DreamUtil.FRONT_CAMERA);
        Optional<SelectedVideoQualities> videoQualitiesBack = computeQualitiesForCamera(DreamUtil.BACK_CAMERA);
        Optional<SelectedVideoQualities> videoQualitiesFront = computeQualitiesForCamera(DreamUtil.FRONT_CAMERA);
        /**
         * SPRD: Fix bug 585183 Adds new features 3D recording @{
         * Original Code
         *
        return new PictureSizes(backCameraSizes, frontCameraSizes, videoQualitiesBack,
                videoQualitiesFront);
         */

        Optional<SelectedVideoQualities> videoQualitiesFront3D = null;

        Optional<SelectedVideoQualities> videoQualitiesBackMacro = null;
        if (CameraUtil.isTDVideoEnable()) {
            videoQualitiesFront3D = computeQualitiesForCamera(CameraUtil.TD_CAMERA_ID);

        }
        if (CameraUtil.isMacroVideoEnable()){
            videoQualitiesBackMacro = computeQualitiesForCamera(CameraUtil.BACK_MACRO_CAMERA_ID);
        }
        return new PictureSizes(backCameraSizes, frontCameraSizes, videoQualitiesBack,
                videoQualitiesFront, videoQualitiesFront3D, videoQualitiesBackMacro);

        /* @} */
    }

    private List<Size> computeSizesForCamera(int cameraID, int defaultid) {
        List<Size> sizes;
        int cameraId = cameraID;
        if(cameraId < 0 || (cameraId != CameraUtil.getBackBlurCameraId() && cameraId != CameraUtil.getFrontBlurCameraId() && cameraId != 3 && cameraId != 2 && cameraId != 1
            && cameraId != CameraUtil.BACK_ULTRA_WIDE_ANGLE_PHOTO_ID && cameraId != CameraUtil.BACK_TRI_CAMERA_ID && cameraId != CameraUtil.BACK_PORTRAIT_ID
            && cameraId != CameraUtil.BACK_HIGH_RESOLUTOIN_CAMERA_ID && cameraId != CameraUtil.FRONT_HIGH_RESOLUTION_CAMERA_ID
            && cameraId != CameraUtil.BACK_IR_CAMERA_ID && cameraId != CameraUtil.BACK_MACRO_CAMERA_ID && cameraId != CameraUtil.BACK_WT_FUSION_ID && cameraId != CameraUtil.FRONT_PORTRAIT_ID
            && cameraId != CameraUtil.FRONT_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID && cameraId != CameraUtil.BACK_PORTRAIT_BACKGROUND_REPLACEMENT_CAMERA_ID)){
            cameraId = defaultid;
        }
        Log.d("skyy", "skyy PictureSizeLoader computeSizesForCamera cameraID = " + cameraID);

        if (cameraId >= 0) {
            if (mCachedOnly) {
                sizes = CameraPictureSizesCacher.getCachedSizesForCamera(cameraId, mContext)
                        .orNull();
            } else {
                sizes = CameraPictureSizesCacher.getSizesForCamera(cameraId, mContext);
            }

            if (sizes != null) {
                sizes = ResolutionUtil
                        .getDisplayableSizesFromSupported(sizes,
                                defaultid == DreamUtil.BACK_CAMERA);
                String blacklisted = GservicesHelper
                        .getBlacklistedResolutionsBack(mContentResolver);
                sizes = ResolutionUtil.filterBlackListedSizes(sizes, blacklisted);

                delelteSize(sizes);

                return sizes;
            }
        }
        return new ArrayList<>(0);
    }

    private Optional<SelectedVideoQualities> computeQualitiesForCamera(int cameraId) {
        if (cameraId >= 0) {
            // This is guaranteed not to be null/absent.
            return Optional
                    .of(SettingsUtil.getSelectedVideoQualities(cameraId));
        }
        return Optional.absent();
    }

    public List<Size> loadFrontPictureSize(int mCameraID, CameraProxy proxy) {
        List<Size> frontCameraSizes = computeSizesForCamera(mCameraID, DreamUtil.FRONT_CAMERA);
        return frontCameraSizes;
    }

    public List<Size> loadBackPictureSize(int mCameraID, CameraProxy proxy) {
        List<Size> backCameraSizes = computeSizesForCamera(mCameraID, DreamUtil.BACK_CAMERA);
        return backCameraSizes;
    }

    public Optional<SelectedVideoQualities> loadFrontVideoQualities(
            int mCameraID) {
        Optional<SelectedVideoQualities> videoQualitiesFront = computeQualitiesForCamera(mCameraID);
        return videoQualitiesFront;
    }

    public Optional<SelectedVideoQualities> loadBackVideoQualities(int mCameraID) {
        Optional<SelectedVideoQualities> videoQualitiesBack = null;
        if (mCameraID == CameraUtil.BACK_TRI_CAMERA_ID) {
            videoQualitiesBack = computeQualitiesForCamera(mCameraID);
        } else {
            videoQualitiesBack = computeQualitiesForCamera(DreamUtil.BACK_CAMERA);
        }

        return videoQualitiesBack;
    }

    public Optional<SelectedVideoQualities> loadFrontVideoQualities3D(int cameraID) {
        Optional<SelectedVideoQualities> frontVideoQualities3D = computeQualitiesForCamera(cameraID);
        return frontVideoQualities3D;
    }

    public Optional<SelectedVideoQualities> loadBackVideoQualitiesMacro(int cameraID) {
        Optional<SelectedVideoQualities> backVideoQualitiesMacro = computeQualitiesForCamera(cameraID);
        return backVideoQualitiesMacro;
    }
}
