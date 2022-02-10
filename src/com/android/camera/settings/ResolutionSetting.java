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
import android.graphics.ImageFormat;

import com.android.camera.debug.Log;
import com.android.camera.device.CameraId;
import com.android.camera.exif.Rational;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCamera.Facing;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraCharacteristics;
import com.android.camera.one.OneCameraManager;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.Size;
import com.dream.camera.settings.DataConfig;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataStructSetting;
import com.dream.camera.util.DreamUtil;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the picture resolution setting stored in SharedPreferences keyed by
 * Keys.KEY_PICTURE_SIZE_BACK and Keys.KEY_PICTURE_SIZE_FRONT.
 */
public class ResolutionSetting {
    private static final Log.Tag TAG = new Log.Tag("ResolutionSettings");

    private final OneCameraManager mOneCameraManager;
    private final String mResolutionBlackListBack;
    private final String mResolutionBlackListFront;
    int mPosition  = DataConfig.SettingStoragePosition.positionList[3];

    public ResolutionSetting(OneCameraManager oneCameraManager,
            ContentResolver contentResolver) {
        mOneCameraManager = oneCameraManager;

        mResolutionBlackListBack = GservicesHelper.getBlacklistedResolutionsBack(contentResolver);
        mResolutionBlackListFront = GservicesHelper.getBlacklistedResolutionsFront(contentResolver);
    }

    /**
     * Changes the picture size settings for the cameras with specified facing.
     * Pick the largest picture size with the specified aspect ratio.
     *
     * @param cameraId The specific camera device.
     * @param aspectRatio The chosen aspect ratio.
     */
    public void setPictureAspectRatio(DataModuleManager dataModuleManager, CameraId cameraId, Rational aspectRatio)
            throws OneCameraAccessException {
        OneCameraCharacteristics cameraCharacteristics =
                mOneCameraManager.getOneCameraCharacteristics(cameraId);

        Facing cameraFacing = cameraCharacteristics.getCameraDirection();

        // Pick the largest picture size with the selected aspect ratio and save
        // the choice for front camera.
        final String pictureSizeSettingKey = cameraFacing == OneCamera.Facing.FRONT ?
                Keys.KEY_PICTURE_SIZE_FRONT : Keys.KEY_PICTURE_SIZE_BACK;
        final String blacklist = cameraFacing == OneCamera.Facing.FRONT ? mResolutionBlackListFront
                : mResolutionBlackListBack;

        DataStructSetting dataSetting = new DataStructSetting(
                DreamUtil.intToString(DreamUtil.PHOTO_MODE), cameraFacing == OneCamera.Facing.FRONT,
                SettingsScopeNamespaces.AUTO_PHOTO, 1);
        DataModuleBasic dataModuleBasic = dataModuleManager.getTempModule(dataSetting);
 
        // All resolutions supported by the camera.
        List<Size> supportedPictureSizes = cameraCharacteristics
                .getSupportedPictureSizes(ImageFormat.JPEG);

        // Filter sizes which we are showing to the user in settings.
        // This might also add some new resolution we support on some devices
        // non-natively.
        supportedPictureSizes = ResolutionUtil.getDisplayableSizesFromSupported(
                supportedPictureSizes, cameraFacing == OneCamera.Facing.BACK);

        // Filter the remaining sizes through our backlist.
        supportedPictureSizes = ResolutionUtil.filterBlackListedSizes(supportedPictureSizes,
                blacklist);

        final Size chosenPictureSize =
                ResolutionUtil.getLargestPictureSize(aspectRatio, supportedPictureSizes);
        dataModuleBasic.set(mPosition,
                pictureSizeSettingKey,
                SettingsUtil.sizeToSettingString(chosenPictureSize));
    }

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    public Size getPictureSize(CameraId cameraId, Facing cameraFacing)
            throws OneCameraAccessException {
        return getPictureSize(cameraId, cameraFacing, null);
    }
    /* @} */

    /* SPRD: Fix 474843 Add for Filter Feature @{ */
    public Size getPictureSize(DataModuleManager dataModuleManager, CameraId cameraId, Facing cameraFacing)
            throws OneCameraAccessException {
        return getPictureSize(dataModuleManager, cameraId, cameraFacing, null);
    }
    /* @} */

    /**
     * Reads the picture size setting for the cameras with specified facing.
     * This specifically avoids reading camera characteristics unless the size
     * is blacklisted or is not cached to prevent a crash.
     */

    /*
     * SPRD: Fix 474843 Add for Filter Feature
     * original code @{
     *
    public Size getPictureSize(CameraId cameraId, Facing cameraFacing)
            throws OneCameraAccessException {
     */
    public Size getPictureSize(CameraId cameraId, Facing cameraFacing, Size preferredMaxSize)
            throws OneCameraAccessException {
//    /* @} */
//        final String pictureSizeSettingKey = cameraFacing == OneCamera.Facing.FRONT ?
//                Keys.KEY_PICTURE_SIZE_FRONT : Keys.KEY_PICTURE_SIZE_BACK;
//
//        Size pictureSize = null;
//
//        String blacklist = "";
//        if (cameraFacing == OneCamera.Facing.BACK) {
//            blacklist = mResolutionBlackListBack;
//        } else if (cameraFacing == OneCamera.Facing.FRONT) {
//            blacklist = mResolutionBlackListFront;
//        }
//
//        // If there is no saved picture size preference or the saved on is
//        // blacklisted., pick a largest size with 4:3 aspect
//        boolean isPictureSizeSettingSet =
//                mSettingsManager.isSet(SettingsManager.SCOPE_GLOBAL, pictureSizeSettingKey);
//        boolean isPictureSizeBlacklisted = false;
//
//        // If a picture size is set, check whether it's blacklisted.
//        if (isPictureSizeSettingSet) {
//            pictureSize = SettingsUtil.sizeFromSettingString(
//                    mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL,
//                            pictureSizeSettingKey));
//            /* SPRD: Fix 474843 Add for Filter Feature @{ */
//            if (preferredMaxSize != null
//                    && pictureSize.height() * pictureSize.width() > preferredMaxSize.width() * preferredMaxSize.height()) {
//                pictureSize = preferredMaxSize;
//            }
//            /* @} */
//            isPictureSizeBlacklisted = pictureSize == null ||
//                    ResolutionUtil.isBlackListed(pictureSize, blacklist);
//        }
//
//        // Due to b/21758681, it is possible that an invalid picture size has
//        // been saved to the settings. Therefore, picture size is set AND is not
//        // blacklisted, but completely invalid. In these cases, need to take the
//        // fallback, instead of the saved value. This logic should now save a
//        // valid picture size to the settings and self-correct the state of the
//        // settings.
//        final boolean isPictureSizeFromSettingsValid = pictureSize != null &&
//                pictureSize.width() > 0 && pictureSize.height() > 0;
//
//        if (!isPictureSizeSettingSet || isPictureSizeBlacklisted || !isPictureSizeFromSettingsValid) {
//            final Rational aspectRatio = ResolutionUtil.ASPECT_RATIO_4x3;
//
//            OneCameraCharacteristics cameraCharacteristics =
//                    mOneCameraManager.getOneCameraCharacteristics(cameraId);
//
//            final List<Size> supportedPictureSizes =
//                    ResolutionUtil.filterBlackListedSizes(
//                            cameraCharacteristics.getSupportedPictureSizes(ImageFormat.JPEG),
//                            blacklist);
//            final Size fallbackPictureSize =
//                    ResolutionUtil.getLargestPictureSize(aspectRatio, supportedPictureSizes);
//            mSettingsManager.set(
//                    SettingsManager.SCOPE_GLOBAL,
//                    pictureSizeSettingKey,
//                    SettingsUtil.sizeToSettingString(fallbackPictureSize));
//            pictureSize = fallbackPictureSize;
//            Log.e(TAG, "Picture size setting is not set. Choose " + fallbackPictureSize);
//            // Crash here if invariants are violated
//            Preconditions.checkNotNull(fallbackPictureSize);
//            Preconditions.checkState(fallbackPictureSize.width() > 0
//                    && fallbackPictureSize.height() > 0);
//        }
//        return pictureSize;
        return null;
    }

    /*
     * SPRD: Fix 474843 Add for Filter Feature
     * original code @{
     *
    public Size getPictureSize(CameraId cameraId, Facing cameraFacing)
            throws OneCameraAccessException {
     */
    public Size getPictureSize(DataModuleManager dataModuleManager, CameraId cameraId, Facing cameraFacing, Size preferredMaxSize)
            throws OneCameraAccessException {
    /* @} */
        String pictureSizeSettingKey = Keys.KEY_PICTURE_SIZE_BACK;
        Size pictureSize = null;

        String blacklist = "";
        if (cameraFacing == OneCamera.Facing.BACK) {
            pictureSizeSettingKey = Keys.KEY_PICTURE_SIZE_BACK;
            blacklist = mResolutionBlackListBack;
        } else if (cameraFacing == OneCamera.Facing.FRONT) {
            pictureSizeSettingKey = Keys.KEY_PICTURE_SIZE_FRONT;
            blacklist = mResolutionBlackListFront;
        }

        /*
         * SPRD: Fix 623994 Add for Filter Feature
         * @{
        DataStructSetting dataSetting = new DataStructSetting(
                DreamUtil.intToString(DreamUtil.PHOTO_MODE), cameraFacing == OneCamera.Facing.FRONT,
                "" + 1, 1);
        DataModuleBasic dataModuleBasic = dataModuleManager.getTempModule(dataSetting);
        */
        DataModuleBasic dataModuleBasic = dataModuleManager.getCurrentDataModule();

        // If there is no saved picture size preference or the saved on is
        // blacklisted., pick a largest size with 4:3 aspect
        /*
         * SPRD: Fix 623994 Add for Filter Feature
         * @{
        boolean isPictureSizeSettingSet =
                dataModuleBasic.isSet(mPosition,pictureSizeSettingKey);
        */
        boolean isPictureSizeSettingSet =
                  dataModuleBasic.isEnableSettingConfig(pictureSizeSettingKey);
        //boolean isPictureSizeBlacklisted = false;

        // If a picture size is set, check whether it's blacklisted.
        if (isPictureSizeSettingSet) {
//            pictureSize = SettingsUtil.sizeFromSettingString(
//                    dataModuleBasic.getString(mPosition,pictureSizeSettingKey, null));
            pictureSize = getPictureSizeAfterFilter(dataModuleBasic,cameraId,preferredMaxSize,pictureSizeSettingKey,blacklist);
            /* @} */
        }

        //isPictureSizeBlacklisted = pictureSize == null ||
        //        ResolutionUtil.isBlackListed(pictureSize, blacklist);
        // Due to b/21758681, it is possible that an invalid picture size has
        // been saved to the settings. Therefore, picture size is set AND is not
        // blacklisted, but completely invalid. In these cases, need to take the
        // fallback, instead of the saved value. This logic should now save a
        // valid picture size to the settings and self-correct the state of the
        // settings.
        //final boolean isPictureSizeFromSettingsValid = pictureSize != null &&
        //        pictureSize.width() > 0 && pictureSize.height() > 0;

        //combine isPictureSizeBlacklisted and isPictureSizeFromSettingsValid to one boolean variable
        final boolean isPictureSizeVaild = isPictureSizeValid(pictureSize,blacklist);
        if (!isPictureSizeSettingSet || !isPictureSizeVaild) {
            final Rational aspectRatio = ResolutionUtil.ASPECT_RATIO_4x3;

            OneCameraCharacteristics cameraCharacteristics =
                    mOneCameraManager.getOneCameraCharacteristics(cameraId);

            final List<Size> supportedPictureSizes =
                    ResolutionUtil.filterBlackListedSizes(
                            cameraCharacteristics.getSupportedPictureSizes(ImageFormat.JPEG),
                            blacklist);
            final Size fallbackPictureSize =
                    ResolutionUtil.getLargestPictureSize(aspectRatio, supportedPictureSizes);
            /*
             * SPRD: Fix 623994 Add for Filter Feature
             * @{
            dataModuleBasic.set(mPosition,pictureSizeSettingKey,
                    SettingsUtil.sizeToSettingString(fallbackPictureSize));
            */
            dataModuleBasic.set(pictureSizeSettingKey,
                    SettingsUtil.sizeToSettingString(fallbackPictureSize));
            pictureSize = fallbackPictureSize;
            Log.e(TAG, "Picture size setting is not set. Choose " + fallbackPictureSize);
            // Crash here if invariants are violated
            Preconditions.checkNotNull(fallbackPictureSize);
            Preconditions.checkState(fallbackPictureSize.width() > 0
                    && fallbackPictureSize.height() > 0);
        }
        return pictureSize;
    }

    private boolean isPictureSizeValid(Size pictureSize,String blacklist){
        final  boolean isPictureSizeBlacklisted = pictureSize == null ||
                ResolutionUtil.isBlackListed(pictureSize, blacklist);
        final boolean isPictureSizeFromSettingsValid = pictureSize != null &&
                pictureSize.width() > 0 && pictureSize.height() > 0;
        return !isPictureSizeBlacklisted && isPictureSizeFromSettingsValid;
    }

    private Size getPictureSizeAfterFilter(DataModuleBasic dataModuleBasic, CameraId cameraId, Size preferredMaxSize, String pictureSizeSettingKey, String blacklist)
            throws OneCameraAccessException {
        Size pictureSize = SettingsUtil.sizeFromSettingString(
                dataModuleBasic.getString(pictureSizeSettingKey));
            /* SPRD: Fix 474843 Add for Filter Feature @{ */
        if (preferredMaxSize != null && pictureSize != null // Bug 1159124 (NULL_RETURNS)
                && pictureSize.height() * pictureSize.width() > preferredMaxSize.width() * preferredMaxSize.height()) {
                /* SPRD: Fix bug 563079 filter mode in not full screen @{ */
            OneCameraCharacteristics cameraCharacteristics =
                    mOneCameraManager.getOneCameraCharacteristics(cameraId);
            final List<Size> supportedPictureSizes =
                    ResolutionUtil.filterBlackListedSizes(
                            cameraCharacteristics.getSupportedPictureSizes(ImageFormat.JPEG),


                            blacklist);
            if (supportedPictureSizes.contains(preferredMaxSize)){
                pictureSize = preferredMaxSize;
            } else {
                Collections.sort(supportedPictureSizes, new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        // sorted in descending order
                        return rhs.height() * rhs.width() - lhs.height() * lhs.width();
                    }
                });
                for (Size size:supportedPictureSizes) {
                    if (size.height() * size.width() > preferredMaxSize.width() * preferredMaxSize.height()) {
                        continue;
                    }
                    if (Math.abs((float)size.width() / (float)size.height() -
                            (float)preferredMaxSize.width() / (float)preferredMaxSize.height()) > 0.00001) {
                        continue;
                    }
                    pictureSize = size;
                    break;
                }
            }
                /* @} */
        }
        return pictureSize;
    }

    /**
     * Obtains the preferred picture aspect ratio in terms of the picture size
     * setting.
     *
     * @param cameraId The specific camera device.
     * @return The preferred picture aspect ratio.
     * @throws OneCameraAccessException
     */
    public Rational getPictureAspectRatio(CameraId cameraId, Facing facing)
            throws OneCameraAccessException {
//        Size pictureSize = getPictureSize(cameraId, facing);
//        return new Rational(pictureSize.getWidth(), pictureSize.getHeight());
        return null;
    }
}
