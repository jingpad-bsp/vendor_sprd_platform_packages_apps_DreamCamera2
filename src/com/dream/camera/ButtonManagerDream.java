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

package com.dream.camera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.camera.CameraActivity;
import com.android.camera.ButtonManager;
import com.android.camera.ButtonManager.ButtonStatusListener;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.ui.RadioOptions;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.widget.ModeOptions;
import com.android.camera2.R;
import com.android.camera.MultiToggleImageButton;
import com.dream.camera.util.DreamUtil;
import com.dream.camera.settings.DataModuleBasic;
import com.dream.camera.settings.DataModuleBasic.DreamSettingChangeListener;
import com.dream.camera.settings.DataModuleManager;
import com.dream.camera.settings.DataModuleManager.ResetListener;
import com.dream.camera.settings.DreamSettingUtil;
import com.android.camera.app.OrientationManager;

/**
 * A class for generating pre-initialized {@link #android.widget.ImageButton}s.
 */
public class ButtonManagerDream extends ButtonManager {
    private static final Log.Tag TAG = new Log.Tag("BMDream");
    public static final int BUTTON_FLASH_DREAM = 20;
    public static final int BUTTON_CAMERA_DREAM = 21;
    public static final int BUTTON_HDR_DREAM = 22;
    public static final int BUTTON_COUNTDOWN_DREAM = 23;
    public static final int BUTTON_METERING_DREAM = 24;
    public static final int BUTTON_TORCH_DREAM = 25;
    public static final int BUTTON_VIDEO_FLASH_DREAM = 27;
    public static final int BUTTON_GIF_PHOTO_FLASH_DREAM = 29;
    public static final int BUTTON_SETTING_DREAM = 30;
    public static final int BUTTON_REFOCUS_DREAM = 32;
    public static final int BUTTON_DIRECTOR_DREAM = 33;
    public static final int BUTTON_COLOR_EFFECT_PHOTO_DREAM = 39;
    public static final int BUTTON_MONTIONPHOTO_DREAM = 34;
    public static final int BUTTON_LIGHT_PORTRAIT_DISPLAY_DREAM = 35;
    public static final int BUTTON_MAKE_UP_DISPLAY_DREAM = 36;
    public static final int BUTTON_FILTER_DREAM = 40;
    public static final int BUTTON_PORTRAIT_REFOCUS_DREAM = 37;
    public static final int BUTTON_FILTER_VIDEO_DREAM = 38;

    public boolean mIsTogglable = true;

    /** Bottom bar options toggle buttons. */
    private MultiToggleImageButton mButtonCameraDream;
    private MultiToggleImageButton mButtonFlashDream;
    private MultiToggleImageButton mButtonHdrDream;
    private MultiToggleImageButton mButtonMeteringDream;
    private MultiToggleImageButton mButtonCountdownDream;
    private MultiToggleImageButton mButtonRefocusDream;
    private MultiToggleImageButton mButtonVideoFlashDream;
    private MultiToggleImageButton mButtonGifPhotoFlashDreamButton;
    private ImageButton mButtonSettingDream;
    private MultiToggleImageButton mButtonDirectorDream;
    private MultiToggleImageButton mButtonMotionPhoto;
    private MultiToggleImageButton mButtonLightPortraitDisplayDream;
    private MultiToggleImageButton mButtonFilterDream;
    private MultiToggleImageButton mButtonMakeUpDisplayDream;
    private MultiToggleImageButton mButtonFilterVideoDream;
    private MultiToggleImageButton mButtonPortraitRefocusDream;
    private MultiToggleImageButton mButtonColorEffectPhotoDream;

    /** Intent UI buttons. */
    /*
     * private ImageButton mButtonCancel; private ImageButton mButtonDone;
     * private ImageButton mButtonRetake; // same as review.
     */
    private static final int NO_RESOURCE = -1;

    /** Whether Camera Button can be enabled by generic operations. */
    private boolean mIsCameraButtonBlocked;

    private final AppController mAppController;

    // Add for dream settings.
    private DataModuleBasic mDataModule;

    /**
     * Get a new global ButtonManager.
     */
    public ButtonManagerDream(AppController app) {
        super(app);
        mAppController = app;

        mDataModule = DataModuleManager.getInstance(
                mAppController.getAndroidContext()).getDataModuleCamera();

    }

    /**
     * Gets references to all known buttons.
     */
    protected void getButtonsReferencesDream(View root) {
        mButtonCameraDream = (MultiToggleImageButton) root
                .findViewById(R.id.camera_toggle_button_dream);
        mButtonFlashDream = (MultiToggleImageButton) root
                .findViewById(R.id.flash_toggle_button_dream);
        mButtonHdrDream = (MultiToggleImageButton) root
                .findViewById(R.id.hdr_toggle_button_dream);
//        mButtonCountdownDream = (MultiToggleImageButton) root
//                .findViewById(R.id.countdown_toggle_button_dream);
        mButtonRefocusDream = (MultiToggleImageButton) root
                .findViewById(R.id.refocus_toggle_button_dream);
        mButtonMeteringDream = (MultiToggleImageButton) root
                .findViewById(R.id.metering_toggle_button_dream);
        mButtonVideoFlashDream = (MultiToggleImageButton) root
                .findViewById(R.id.video_flash_toggle_button_dream);
        mButtonGifPhotoFlashDreamButton = (MultiToggleImageButton) root
                .findViewById(R.id.gif_photo_flash_toggle_button_dream);
        mButtonSettingDream = (ImageButton) root
                .findViewById(R.id.settings_button_dream);
        mButtonDirectorDream = (MultiToggleImageButton) root
                .findViewById(R.id.director_toggle_button_dream);
        mButtonMotionPhoto = (MultiToggleImageButton) root
                .findViewById(R.id.montionphoto_toggle_button_dream);
        mButtonLightPortraitDisplayDream = (MultiToggleImageButton) root
                .findViewById(R.id.light_portrait_display_toggle_button_dream);
        mButtonFilterDream = (MultiToggleImageButton) root
                .findViewById(R.id.filter_toggle_button_dream);
        mButtonMakeUpDisplayDream = (MultiToggleImageButton) root
                .findViewById(R.id.make_up_display_toggle_button_dream);
        mButtonFilterVideoDream = (MultiToggleImageButton) root
                .findViewById(R.id.filter_video_toggle_button_dream);
        mButtonPortraitRefocusDream = (MultiToggleImageButton) root
                .findViewById(R.id.portrait_refocus_toggle_button_dream);
        mButtonColorEffectPhotoDream = (MultiToggleImageButton) root
                .findViewById(R.id.color_effect_photo_toggle_button_dream);

    }

    public void load(View root) {
        try {
            super.load(mAppController.getCameraAppUI().getModuleRootView());
            Log.d(TAG, "load");
            getButtonsReferencesDream(root);
        } catch (Exception ex) {
            Log.w(TAG, " ButtonManager load error", ex);
        }
    }


    /**
     * Returns the appropriate {@link com.android.camera.MultiToggleImageButton}
     * based on button id. An IllegalStateException will be throw if the button
     * could not be found in the view hierarchy.
     */
    protected MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
            case BUTTON_FLASH_DREAM:
                checkButtonNull(mButtonFlashDream , "Flash button could not be found.");
                return mButtonFlashDream;
            case BUTTON_TORCH_DREAM:
                checkButtonNull(mButtonFlashDream , "Torch button could not be found.");
                return mButtonFlashDream;
            case BUTTON_CAMERA_DREAM:
                checkButtonNull(mButtonCameraDream , "Camera button could not be found.");
                return mButtonCameraDream;
            case BUTTON_HDR_DREAM:
                checkButtonNull(mButtonHdrDream , "Hdr button could not be found.");
                return mButtonHdrDream;
            case BUTTON_COUNTDOWN_DREAM:
                checkButtonNull(mButtonCountdownDream , "Countdown button could not be found.");
                return mButtonCountdownDream;
            case BUTTON_METERING_DREAM:
                checkButtonNull(mButtonMeteringDream , "Metering button could not be found.");
                return mButtonMeteringDream;
            case BUTTON_VIDEO_FLASH_DREAM:
                checkButtonNull(mButtonVideoFlashDream , "Video Flash button could not be found.");
                return mButtonVideoFlashDream;
            case BUTTON_GIF_PHOTO_FLASH_DREAM:
                checkButtonNull(mButtonGifPhotoFlashDreamButton , "gif photo flash button could not be found.");
                return mButtonGifPhotoFlashDreamButton;
            case BUTTON_REFOCUS_DREAM:
                checkButtonNull(mButtonRefocusDream , "refocus button could not be found.");
                return mButtonRefocusDream;
            case BUTTON_DIRECTOR_DREAM:
                checkButtonNull(mButtonDirectorDream , "director button could not be found.");
                return mButtonDirectorDream;
            case  BUTTON_MONTIONPHOTO_DREAM:
                checkButtonNull(mButtonMotionPhoto,"motionphoto button could not be found");
                return mButtonMotionPhoto;
            case BUTTON_LIGHT_PORTRAIT_DISPLAY_DREAM:
                checkButtonNull(mButtonLightPortraitDisplayDream,"light portrait display button could not be found");
                return mButtonLightPortraitDisplayDream;
            case BUTTON_FILTER_DREAM:
                checkButtonNull(mButtonFilterDream,"filter button could not be found");
                return mButtonFilterDream;
            case BUTTON_MAKE_UP_DISPLAY_DREAM:
                checkButtonNull(mButtonMakeUpDisplayDream,"make up display button could not be found");
                return mButtonMakeUpDisplayDream;
            case BUTTON_FILTER_VIDEO_DREAM:
                checkButtonNull(mButtonFilterVideoDream,"filter video button could not be found");
                return mButtonFilterVideoDream;
            case BUTTON_PORTRAIT_REFOCUS_DREAM:
                checkButtonNull(mButtonPortraitRefocusDream,"make up display button could not be found");
                return mButtonPortraitRefocusDream;
            case BUTTON_COLOR_EFFECT_PHOTO_DREAM:
                checkButtonNull(mButtonColorEffectPhotoDream,"color effect photo button could not be found");
                return mButtonColorEffectPhotoDream;
            default:
                return super.getButtonOrError(buttonId);
        }
    }

    /**
     * Returns the appropriate {@link android.widget.ImageButton} based on
     * button id. An IllegalStateException will be throw if the button could not
     * be found in the view hierarchy.
     */
    protected ImageButton getImageButtonOrError(int buttonId) {
        switch (buttonId) {
        case BUTTON_CANCEL:
            if (mButtonCancel == null) {
                throw new IllegalStateException(
                        "Cancel button could not be found.");
            }
            return mButtonCancel;
        case BUTTON_DONE:
            if (mButtonDone == null) {
                throw new IllegalStateException(
                        "Done button could not be found.");
            }
            return mButtonDone;
        case BUTTON_RETAKE:
            if (mButtonRetake == null) {
                throw new IllegalStateException(
                        "Retake button could not be found.");
            }
            return mButtonRetake;
        case BUTTON_REVIEW:
            if (mButtonRetake == null) {
                throw new IllegalStateException(
                        "Review button could not be found.");
            }
            return mButtonRetake;
        case BUTTON_SETTING_DREAM:
            if (mButtonSettingDream == null) {
                throw new IllegalStateException(
                        "gif setting button could not be found.");
            }
            return mButtonSettingDream;

        default:
            return super.getImageButtonOrError(buttonId);
        }
    }

    /**
     * Initialize a known button by id with a state change callback, and then
     * enable the button.
     * 
     * @param buttonId
     *            The id if the button to be initialized.
     * @param cb
     *            The callback to be executed after the button state change.
     */
    public void initializeButton(int buttonId, ButtonCallback cb) {
        initializeButton(buttonId, cb, null);
    }

    public void initializeButton(int buttonId, ButtonCallback cb, int resId) {
        initializeButton(buttonId, cb, null, resId);
    }

    public void initializeButton(int buttonId, ButtonCallback cb,
            ButtonCallback preCb, int resId) {
        MultiToggleImageButton button = null;
        try {
            button = getButtonOrError(buttonId);
        } catch (Exception e) {
            Log.w(TAG, " button find error", e);
        }
        if (button == null) return;
        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        if (orientationManager != null) {
            int degree = orientationManager.getDeviceOrientation().getDegrees();
            button.setTargetDegree(degree);
        } else {
            Log.d(TAG,"OrientationManager is null and can not set degree");
        }
        switch (buttonId) {
            case BUTTON_FLASH_DREAM:
                initializeFlashButton(button, cb, preCb, resId);
                break;
            // case BUTTON_TORCH_DREAM:
            // initializeTorchButton(button, cb, preCb,
            // R.array.video_flashmode_icons);
            // break;

            case BUTTON_CAMERA_DREAM:
                initializeCameraButton(button, cb, preCb, resId);
                break;

            case BUTTON_HDR_DREAM:
                initializeHdrButton(button, cb, preCb, resId);
                break;

            case BUTTON_COUNTDOWN_DREAM:
                initializeCountdownButton(button, cb, preCb, resId);
                break;

            case BUTTON_METERING_DREAM:
                initializeMeteringButton(button, cb, preCb, resId);
                break;
            case BUTTON_VIDEO_FLASH_DREAM:
                initializeVideoFlashButton(button, cb, preCb, resId);
                break;
            case BUTTON_GIF_PHOTO_FLASH_DREAM:
                initializeGifPhotoFlashButton(button, cb, preCb, resId);
                break;
            case BUTTON_COLOR_EFFECT_PHOTO_DREAM:
                initializeColorEffectPhotoButton(button, cb, preCb,
                        R.array.dream_color_effect_photo_icons);
                break;
            default:
                super.initializeButton(buttonId, cb, preCb);
                break;
        }

        showButton(buttonId);
        enableButton(buttonId);
    }

    /**
     * Initialize a known button by id, with a state change callback and a state
     * pre-change callback, and then enable the button.
     * 
     * @param buttonId
     *            The id if the button to be initialized.
     * @param cb
     *            The callback to be executed after the button state change.
     * @param preCb
     *            The callback to be executed before the button state change.
     */
    public void initializeButton(int buttonId, ButtonCallback cb,
            ButtonCallback preCb) {
        MultiToggleImageButton button = null;
        try {
            button = getButtonOrError(buttonId);
        } catch (Exception e) {
            Log.w(TAG, " button find error", e);
        }
        if (button == null) return;
        OrientationManager orientationManager = mAppController
                .getOrientationManager();
        if (orientationManager != null) {
            int degree = orientationManager.getDeviceOrientation().getDegrees();
            button.setTargetDegree(degree);
        } else {
            Log.d(TAG,"OrientationManager is null and can not set degree");
        }
        switch (buttonId) {
            case BUTTON_FLASH_DREAM:
                initializeFlashButton(button, cb, preCb,
                		getFlashResID(BUTTON_FLASH_DREAM));
                break;
            // case BUTTON_TORCH_DREAM:
            // initializeTorchButton(button, cb, preCb,
            // R.array.video_flashmode_icons);
            // break;
            case BUTTON_CAMERA_DREAM:
                 if (!mIsTogglable) {
                     setButtonVisibility(buttonId,View.GONE);
                 } else {
                     setButtonVisibility(buttonId,View.VISIBLE);
                 }
                initializeCameraButton(button, cb, preCb,
                        R.array.dream_camera_id_icons);
                break;

            case BUTTON_HDR_DREAM:
                if(CameraUtil.isFdrSupport()){
                    Log.d(TAG,"set fdr icon");
                    if (mAppController.isAutoHdrSupported()) {
                        initializeHdrButton(button, cb, preCb,
                                R.array.dream_camera_fdr_with_auto_icons);
                    } else {
                        initializeHdrButton(button, cb, preCb,
                                R.array.dream_camera_fdr_icons);
                    }
                } else {
                    if (mAppController.isAutoHdrSupported()) {
                        initializeHdrButton(button, cb, preCb,
                                R.array.dream_camera_hdr_with_auto_icons);
                    } else {
                        initializeHdrButton(button, cb, preCb,
                                R.array.dream_camera_hdr_icons);
                    }
                }
                break;

            case BUTTON_COUNTDOWN_DREAM:
                initializeCountdownButton(button, cb, preCb,
                        R.array.dream_countdown_duration_icons);
                break;

            case BUTTON_REFOCUS_DREAM:
                initializeRefocusButton(button, cb, preCb,
                        R.array.refocus_icons);
                break; 

            case BUTTON_METERING_DREAM:
                initializeMeteringButton(button, cb, preCb,
                        R.array.dream_metering_icons);
                break;
            case BUTTON_VIDEO_FLASH_DREAM:
                initializeVideoFlashButton(button, cb, preCb,
                		getFlashResID(BUTTON_VIDEO_FLASH_DREAM));
                break;

            case BUTTON_GIF_PHOTO_FLASH_DREAM:
                initializeGifPhotoFlashButton(button, cb, preCb,
                        R.array.dream_video_flashmode_icons);
                break;

            case BUTTON_DIRECTOR_DREAM:
                initializeDirectorButton(button, cb, preCb,
                        R.array.director_icons);
                break;

            case BUTTON_MONTIONPHOTO_DREAM:
                initializeMotionPhotoButton(button, cb, preCb,
                        R.array.dream_camera_motionphoto_icons);
                break;
            case BUTTON_LIGHT_PORTRAIT_DISPLAY_DREAM:
                initializeLightPortraitDisplayButton(button, cb, preCb,
                        R.array.light_portrait_display_icons);
                break;
            case BUTTON_FILTER_DREAM:
                initializeFilterButton(button, cb, preCb,
                        R.array.filter_icons);
                break;
            case BUTTON_MAKE_UP_DISPLAY_DREAM:
                initializeMakeUpDisplayButton(button, cb, preCb,
                        R.array.make_up_display_icons);
                break;
            case BUTTON_FILTER_VIDEO_DREAM:
                initializeFilterVideoButton(button, cb, preCb,
                        R.array.dream_filter_video_icons);
                break;
            case BUTTON_PORTRAIT_REFOCUS_DREAM:
                initializePortraitRefocusButton(button, cb, preCb,
                        R.array.portrait_refocus_icons);
                break;
            case BUTTON_COLOR_EFFECT_PHOTO_DREAM:
                initializeColorEffectPhotoButton(button, cb, preCb,
                        R.array.dream_color_effect_photo_icons);
                break;
            default:
                super.initializeButton(buttonId, cb, preCb);
        }

        showButton(buttonId);
        enableButton(buttonId);
    }
    private void initializeColorEffectPhotoButton(MultiToggleImageButton button,
                                             final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.dream_color_effect_photo_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "color effect photo display button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY,
                                state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });

    }
    /**
     * Initialize a known button with a click listener and a drawable resource
     * id, and a content description resource id. Sets the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb,
            int imageId, int contentDescriptionId) {
        ImageButton button = getImageButtonOrError(buttonId);
        button.setOnClickListener(cb);
        if (imageId != NO_RESOURCE) {
            button.setImageResource(imageId);
        }
        if (contentDescriptionId != NO_RESOURCE) {
            button.setContentDescription(mAppController.getAndroidContext()
                    .getResources().getString(contentDescriptionId));
        }

        if (!button.isEnabled()) {
            button.setEnabled(true);
        }
        button.setTag(R.string.tag_enabled_id, buttonId);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Initialize a known button with a click listener and a resource id. Sets
     * the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb,
            int imageId) {
        initializePushButton(buttonId, cb, imageId, NO_RESOURCE);
    }

    /**
     * Initialize a known button with a click listener. Sets the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb) {
        initializePushButton(buttonId, cb, NO_RESOURCE, NO_RESOURCE);
    }

    /**
     * Sets the camera button in its disabled (greyed out) state and blocks it
     * so no generic operation can enable it until it's explicitly re-enabled by
     * calling {@link #enableCameraButton()}.
     */
    public void disableCameraButtonAndBlock() {
        // SPRD: Bug 502464
        mAppController.getCameraAppUI().setSwipeEnabled(false);

        mIsCameraButtonBlocked = true;
        disableButton(BUTTON_CAMERA_DREAM);
    }

    /**
     * Sets a button in its disabled (greyed out) state.
     */
    public void disableButton(int buttonId) {
        Log.d(TAG, "disableButton: " + buttonId);
        View button = null;
        if (buttonId == BUTTON_SETTING_DREAM) {
            button = getImageButtonOrError(buttonId);
        } else {
            button = getButtonOrError(buttonId);
        }
        if (buttonId == BUTTON_HDR_DREAM) {
            initializeHdrButtonIcons((MultiToggleImageButton) button,
                    R.array.pref_camera_hdr_icons);
        }

        if (button.isEnabled()) {
            button.setEnabled(false);
        }
        button.setTag(R.string.tag_enabled_id, null);
    }

    public void setButtonVisibility(int buttonId, int visibility) {
        Log.d(TAG,"setButtonVisibility: " + buttonId + ", visibility is " + visibility);
        try {
            View button = getButtonOrError(buttonId);
            ((View)button.getParent()).setVisibility(visibility);
        } catch (IllegalStateException e) {
            Log.e(TAG,"button does not found");
        }
    }

    /**
     * Enables the camera button and removes the block that was set by
     * {@link #disableCameraButtonAndBlock()}.
     */
    public void enableCameraButton() {
        // SPRD: Bug 502464
        mAppController.getCameraAppUI().setSwipeEnabled(true);

        mIsCameraButtonBlocked = false;
        enableButton(BUTTON_CAMERA_DREAM);
    }

    /**
     * Enables a button that has already been initialized.
     */
    public void enableButton(int buttonId) {
        // If Camera Button is blocked, ignore the request.
        if (buttonId == BUTTON_CAMERA_DREAM && mIsCameraButtonBlocked) {
            return;
        }
        View button = null;
        if (buttonId == BUTTON_SETTING_DREAM) {
            button = getImageButtonOrError(buttonId);
        } else {
            button = getButtonOrError(buttonId);
        }

        // SPRD:fix bug528520 flash icon do not show after close hdr
        button.setTag(R.string.tag_enabled_id, buttonId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
        }
        /*
         * SPRD:fix bug528520 flash icon do not show after close hdr android
         * original code button.setTag(R.string.tag_enabled_id, buttonId);
         */
    }

    /**
     * Disable click reactions for a button without affecting visual state. For
     * most cases you'll want to use {@link #disableButton(int)}.
     * 
     * @param buttonId
     *            The id of the button.
     */
    public void disableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(false);
        }
    }

    /**
     * Enable click reactions for a button without affecting visual state. For
     * most cases you'll want to use {@link #enableButton(int)}.
     * 
     * @param buttonId
     *            The id of the button.
     */
    public void enableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(true);
        }
    }

    /**
     * Hide a button by id.
     */
    public void hideButton(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == View.VISIBLE) {
            button.setVisibility(View.GONE);
        }
    }

    /**
     * Show a button by id.
     */
    public void showButton(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
        }
    }

    /*
     * public void setToInitialState() {
     * mModeOptions.setMainBar(ModeOptions.BAR_STANDARD); } public void
     * setExposureCompensationCallback(final CameraAppUI.BottomBarUISpec
     * .ExposureCompensationSetCallback cb) { if (cb == null) {
     * mModeOptionsExposure.setOnOptionClickListener(null); } else {
     * mModeOptionsExposure .setOnOptionClickListener(new
     * RadioOptions.OnOptionClickListener() {
     * 
     * @Override public void onOptionClicked(View v) { int comp =
     * Integer.parseInt((String)(v.getTag())); if (mExposureCompensationStep !=
     * 0.0f) { int compValue = Math.round(comp / mExposureCompensationStep);
     * cb.setExposure(compValue); } } }); } }
     */
    /**
     * Set the exposure compensation parameters supported by the current camera
     * mode.
     * 
     * @param min
     *            Minimum exposure compensation value.
     * @param max
     *            Maximum exposure compensation value.
     * @param step
     *            Expsoure compensation step value.
     */
    /*
     * public void setExposureCompensationParameters(int min, int max, float
     * step) { mMaxExposureCompensation = max; mMinExposureCompensation = min;
     * mExposureCompensationStep = step; // SPRD Bug:474711 Feature:Exposure
     * Compensation. setVisible(mExposureN3, (Math.round(min * step) <= -3));
     * setVisible(mExposureN2, (Math.round(min * step) <= -2));
     * setVisible(mExposureN1, (Math.round(min * step) <= -1));
     * setVisible(mExposureP1, (Math.round(max * step) >= 1));
     * setVisible(mExposureP2, (Math.round(max * step) >= 2)); // SPRD
     * Bug:474711 Feature:Exposure Compensation. setVisible(mExposureP3,
     * (Math.round(max * step) >= 3)); updateExposureButtons(); }
     */

    private static void setVisible(View v, boolean visible) {
        if (visible) {
            v.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @return The exposure compensation step value.
     **/
    /*
     * public float getExposureCompensationStep() { return
     * mExposureCompensationStep; }
     */

    /**
     * Check if a button is enabled with the given button id..
     */
    public boolean isEnabled(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        } catch (IllegalStateException e){
            Log.i(TAG, buttonId + "button not found");
            return false;
        }

        Integer enabledId = (Integer) button.getTag(R.string.tag_enabled_id);
        if (enabledId != null) {
            return (enabledId.intValue() == buttonId) && button.isEnabled();
        } else {
            return false;
        }
    }

    /**
     * Check if a button is visible.
     */
    public boolean isVisible(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        return (button.getVisibility() == View.VISIBLE);
    }

    /**
     * Initialize a flash button.
     */
    private void initializeFlashButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        if (mModuleId == SettingsScopeNamespaces.MANUAL) {
            button.overrideContentDescriptions(R.array.camera_manual_flash_descriptions);
        } else if (mModuleId == SettingsScopeNamespaces.MACRO_PHOTO) {
            button.overrideContentDescriptions(R.array.camera_torch_flash_descriptions);
        } else {
            button.overrideContentDescriptions(R.array.camera_flash_descriptions);
        }

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_FLASH_MODE);

        int default_index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfDefaultValue(Keys.KEY_FLASH_MODE);

        int index_set = index >= 0 ? index : default_index;
        button.setState(index_set, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_FLASH_MODE, state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeVideoFlashButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.video_flash_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_VIDEO_FLASH_MODE);
        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                Log.e(TAG, "Keys.KEY_VIDEO_FLASH_MODE    state = " + state);
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_VIDEO_FLASH_MODE, state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeGifPhotoFlashButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.video_flash_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);

        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                Log.e(TAG, "Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE    state = " + state);
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE, state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }



    /**
     * Initialize a camera button.
     */
    private void initializeCameraButton(final MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }

        int index = mDataModule.getIndexOfCurrentValue(Keys.KEY_CAMERA_ID);

        button.setState(index >= 0 && index <= 1 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                /*
                 * SPRD: fix bug 516434,519391 If the state is the same with
                 * current camera ID, the function will not run.
                 */

                int prefCameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
                Log.d(TAG, "bbbb initializeCameraButton stateChanged state="
                        + state + "," + prefCameraId);

                if (state != prefCameraId) {
                    Log.d(TAG,"switch camera");
                    //SPRD:Bug 851756 avoid changing CameraID and changing mode out of sync.
                    //mDataModule.set(Keys.KEY_CAMERA_ID, state);
                    //int cameraId = mDataModule.getInt(Keys.KEY_CAMERA_ID);
                    Log.d(TAG,
                            "bbbb initializeCameraButton stateChanged state="
                                    + state);
                    // This is a quick fix for ISE in Gcam module which can be
                    // found by rapid pressing camera switch button. The
                    // assumption
                    // here is that each time this button is clicked, the
                    // listener
                    // will do something and then enable this button again.
                    button.setEnabled(false);
                    if (cb != null) {
                        cb.onStateChanged(state);
                    }
                    mAppController.getCameraAppUI().onChangeCamera();
                }
            }
        });
    }

    private void initializeHdrPlusButtonIcons(MultiToggleImageButton button,
            int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_plus_descriptions);
    }

    /**
     * Initialize an hdr button.
     */
    private void initializeHdrButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        initializeHdrButtonIcons(button, resIdImages);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_CAMERA_HDR);

        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_CAMERA_HDR, state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeHdrButtonIcons(MultiToggleImageButton button,
            int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_descriptions);
    }

    /**
     * Initialize a countdown timer button.
     */
    private void initializeCountdownButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_COUNTDOWN_DURATION);
        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_COUNTDOWN_DURATION,
                                state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    /**
     * Initialize a refocus button.
     */
    private void initializeRefocusButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_REFOCUS);
        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                Log.i(TAG,"RefocusButton click state = " + state);
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_REFOCUS,
                                state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    /**
     * Initialize a panorama director button.
     */
    private void initializeDirectorButton(MultiToggleImageButton button,
                                         final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_DIRECTOR);
        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                Log.i(TAG,"DirectorButton click state = " + state);
                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_DIRECTOR,
                                state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });
    }

    /**
     * Initialize a metering button.
     */
    private void initializeMeteringButton(final MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.metering_descriptions);
        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_CAMER_METERING);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                int prefstate = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_CAMER_METERING);

                if (state != prefstate) {
                    DataModuleManager
                            .getInstance(mAppController.getAndroidContext())
                            .getCurrentDataModule()
                            .changeSettingsByIndex(Keys.KEY_CAMER_METERING,
                                    state);
                    if (cb != null) {
                        // cb.onStateChanged(state);
                    }
                }

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DreamSettingUtil.showDialog((Activity) mAppController,
                        Keys.KEY_CAMER_METERING,
                        R.string.pref_camera_metering_title,
                        R.array.dream_metering_icons,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                button.setState(which >= 0 ? which : 0, true);
                                dialog.dismiss();
                            }
                        });
            }
        });

    }


//
//    public int getMakeupButtonState() {
//        // add for get Makeup Button State for AI Scene Detection
//        MultiToggleImageButton button;
//        try {
//            button = getButtonOrError(BUTTON_MAKE_UP_DREAM);
//        } catch (IllegalArgumentException e) {
//            Log.e(TAG , "can not found BUTTON_MAKE_UP_DREAM , return -2");
//            return -2;
//        }
//        return (button.getState());
//    }

    /**
     * Initialize a make up button.
     */
    private void initializeMotionPhotoButton(MultiToggleImageButton button,
                                             final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.montionphoto_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.PREF_KEY_MONTIONPHOTO);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "montionphoto button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.PREF_KEY_MONTIONPHOTO,
                                state);
                if (cb != null) {
                    // cb.onStateChanged(state);
                }
            }
        });

    }

    private void initializeLightPortraitDisplayButton(MultiToggleImageButton button,
                                             final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.light_portrait_display_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_LIGHT_PORTIAIT_DISPLAY);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "light portrait display button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_LIGHT_PORTIAIT_DISPLAY,
                                state);
                if (cb != null) {
                     cb.onStateChanged(state);
                }
            }
        });

    }

    private void initializeFilterButton(MultiToggleImageButton button,
                                               final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.filter_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_FILTER_PHOTO);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                if(mAppController.getCameraAppUI().getExtendPanelParent().getVisibility() == View.INVISIBLE){
                    DataModuleManager.getInstance(mAppController.getAndroidContext()) .getCurrentDataModule().
                            changeSettingsByIndex(Keys.KEY_FILTER_PHOTO,1);
                    mButtonFilterDream.setState(1);
                } else {
                    DataModuleManager
                            .getInstance(mAppController.getAndroidContext())
                            .getCurrentDataModule()
                            .changeSettingsByIndex(Keys.KEY_FILTER_PHOTO,
                                    state);
                }
                Log.e(TAG, "chenyun1025_filter display button state = " + state);
                if (cb != null) {
                    //cb.onStateChanged(state);
                }
            }
        });

    }

    private void initializeMakeUpDisplayButton(MultiToggleImageButton button,
                                             final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.make_up_display_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_MAKE_UP_DISPLAY);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "make up display button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_MAKE_UP_DISPLAY,
                                state);
                if (cb != null) {
                     cb.onStateChanged(state);
                }
            }
        });

    }
    private void initializeFilterVideoButton(MultiToggleImageButton button,
                                               final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.dream_filter_video_descriptions);

        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(Keys.KEY_FILTER_VIDEO_DISPLAY);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "filter video display button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(Keys.KEY_FILTER_VIDEO_DISPLAY,
                                state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });

    }
    private void initializePortraitRefocusButton(MultiToggleImageButton button,
                                               final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.portrait_refocus_descriptions);

        final String keys = CameraUtil.isPortraitRefocusEnable() && cb != null ? Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY : Keys.KEY_PORTRAIT_REFOCUS_KEY;
        int index = DataModuleManager
                .getInstance(mAppController.getAndroidContext())
                .getCurrentDataModule()
                .getIndexOfCurrentValue(keys);
        button.setState(index >= 0 ? index : 0, true);

        setPreChangeCallback(button, preCb);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {

                Log.e(TAG, "portrait refocus display button state = " + state);

                DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .changeSettingsByIndex(keys,
                                state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });

    }


    private void setPreChangeCallback(MultiToggleImageButton button,
            final ButtonCallback preCb) {
        button.setOnPreChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if (preCb != null) {
                    preCb.onStateChanged(state);
                }
            }
        });
    }

    private DreamSettingChangeListener photoSettingChangeListener = new DreamSettingChangeListener() {

        @Override
        public void onDreamSettingChangeListener(HashMap<String, String> keys) {
            updatePhotoButtonItems(keys.keySet());
        }
    };

    private DreamSettingChangeListener videoSettingChangeListener = new DreamSettingChangeListener() {

        @Override
        public void onDreamSettingChangeListener(HashMap<String, String> keys) {
            updateVideoButtonItems(keys.keySet());
        }

    };

    private ResetListener dataResetListener = new ResetListener() {

        @Override
        public void onSettingReset() {
            Log.e(TAG, "updatePhotoButtonItems onreset................. ");
            Set<String> photoKeys = new HashSet<String>();
            Set<String> VideoKeys = new HashSet<String>();
            Set<String> CameraKeys = new HashSet<String>();

            photoKeys.add(Keys.KEY_CAMERA_BEAUTY_ENTERED);
            photoKeys.add(Keys.KEY_FLASH_MODE);
            photoKeys.add(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);
            photoKeys.add(Keys.KEY_COUNTDOWN_DURATION);
            photoKeys.add(Keys.KEY_CAMERA_HDR);
            photoKeys.add(Keys.KEY_CAMER_METERING);
            VideoKeys.add(Keys.KEY_VIDEO_FLASH_MODE);
            VideoKeys.add(Keys.KEY_VIDEO_BEAUTY_ENTERED);
            CameraKeys.add(Keys.KEY_CAMERA_ID);

            updatePhotoButtonItems(photoKeys);
            updateVideoButtonItems(VideoKeys);
            updateCameraButtonItems(CameraKeys);

        }

    };

    @Override
    public void setListener(ButtonStatusListener listener) {
//        super.setListener(listener);

        DataModuleManager dataModuleManager = DataModuleManager
                .getInstance(mAppController.getAndroidContext());

        dataModuleManager.getDataModulePhoto().addListener(
                (CameraActivity)mAppController,photoSettingChangeListener);

        dataModuleManager.getDataModuleVideo().addListener(
                (CameraActivity)mAppController,videoSettingChangeListener);

        dataModuleManager.addListener(dataResetListener);

        // hideOrShowButtonAccordingConfig();

    }

    private void updatePhotoButtonItems(Set<String> keys) {
        for (String key : keys) {
            Log.e(TAG, "updatePhotoButtonItems key = " + key);
            int index = 0;
            MultiToggleImageButton button = null;
            switch (key) {
                case Keys.KEY_LIGHT_PORTIAIT_DISPLAY:
                    index = DataModuleManager
                            .getInstance(mAppController.getAndroidContext())
                            .getCurrentDataModule()
                            .getIndexOfCurrentValue(Keys.KEY_LIGHT_PORTIAIT_DISPLAY);
                    button = mButtonLightPortraitDisplayDream;
                    break;
            case Keys.KEY_FILTER_PHOTO:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_FILTER_PHOTO);
                button = mButtonFilterDream;
                break;
            case Keys.KEY_MAKE_UP_DISPLAY:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_MAKE_UP_DISPLAY);
                button = mButtonMakeUpDisplayDream;
                break;
            case Keys.KEY_PORTRAIT_REFOCUS_KEY:
            case Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY:
                index = DataModuleManager
                         .getInstance(mAppController.getAndroidContext())
                         .getCurrentDataModule()
                         .getIndexOfCurrentValue(CameraUtil.isPortraitRefocusEnable() && DreamUtil.BACK_CAMERA == DreamUtil.getRightCamera(DataModuleManager
                                 .getInstance(mAppController.getAndroidContext()).getDataModuleCamera()
                                 .getInt(Keys.KEY_CAMERA_ID)) ? Keys.KEY_PORTRAIT_REFOCUS_DISPLAY_KEY : Keys.KEY_PORTRAIT_REFOCUS_KEY);
                button = mButtonPortraitRefocusDream;
                break;
            case Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);
                if(mButtonGifPhotoFlashDreamButton != null){
                    button = mButtonGifPhotoFlashDreamButton;
                }
                break;
            case Keys.KEY_FLASH_MODE:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_FLASH_MODE);
                if(mButtonFlashDream != null){
                    button = mButtonFlashDream;
                }
                break;
            case Keys.KEY_COUNTDOWN_DURATION:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_COUNTDOWN_DURATION);
                button = mButtonCountdownDream;
                break;
            case Keys.KEY_CAMERA_HDR:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_CAMERA_HDR);
                button = mButtonHdrDream;
                break;
            case Keys.KEY_CAMER_METERING:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_CAMER_METERING);
                button = mButtonMeteringDream;
                break;
            case Keys.PREF_KEY_MONTIONPHOTO:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.PREF_KEY_MONTIONPHOTO);
                button = mButtonMotionPhoto;
                break;
            case Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY:
                index = DataModuleManager
                         .getInstance(mAppController.getAndroidContext())
                         .getCurrentDataModule()
                         .getIndexOfCurrentValue(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY);
                button = mButtonColorEffectPhotoDream;
                break;
            }
            Log.e(TAG, "updatePhotoButtonItems index = " + index);
            if (button != null) {
                button.setState(index >= 0 ? index : 0, false);
            }
        }

    }

    private void updateVideoButtonItems(Set<String> keys) {

        for (String key : keys) {
            int index = 0;
            MultiToggleImageButton button = null;
            switch (key) {
            case Keys.KEY_VIDEO_FLASH_MODE:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_VIDEO_FLASH_MODE);
                button = mButtonVideoFlashDream;
                break;
             case Keys.KEY_LIGHT_PORTIAIT_DISPLAY:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_LIGHT_PORTIAIT_DISPLAY);
                button = mButtonLightPortraitDisplayDream;
                break;
            case Keys.KEY_MAKE_UP_DISPLAY:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getCurrentDataModule()
                        .getIndexOfCurrentValue(Keys.KEY_MAKE_UP_DISPLAY);
                button = mButtonMakeUpDisplayDream;
                break;
            case Keys.KEY_FILTER_VIDEO_DISPLAY:
                index = DataModuleManager
                         .getInstance(mAppController.getAndroidContext())
                         .getCurrentDataModule()
                         .getIndexOfCurrentValue(Keys.KEY_FILTER_VIDEO_DISPLAY);
                button = mButtonFilterVideoDream;
                break;
            }
            if (button != null) {
                button.setState(index >= 0 ? index : 0, false);
            }
        }

    }

    private void updateCameraButtonItems(Set<String> keys) {
        for (String key : keys) {
            int index = 0;
            MultiToggleImageButton button = null;
            switch (key) {
            case Keys.KEY_CAMERA_ID:
                index = DataModuleManager
                        .getInstance(mAppController.getAndroidContext())
                        .getDataModuleCamera()
                        .getIndexOfCurrentValue(Keys.KEY_CAMERA_ID);
                button = mButtonCameraDream;
                break;
            }
            if (button != null) {
                button.setState(index >= 0 ? index : 0, false);
            }
        }
    }

    public void refreshButtonState() {
        Set<String> photoKeys = new HashSet<String>();
        Set<String> VideoKeys = new HashSet<String>();
        Set<String> CameraKeys = new HashSet<String>();
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_CAMERA_BEAUTY_ENTERED)) {//SPRD:fix bug944254
            photoKeys.add(Keys.KEY_CAMERA_BEAUTY_ENTERED);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_FILTER_PHOTO)) {
            photoKeys.add(Keys.KEY_FILTER_PHOTO);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_MAKE_UP_DISPLAY)) {//SPRD:fix bug944254
            photoKeys.add(Keys.KEY_MAKE_UP_DISPLAY);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_FILTER_VIDEO_DISPLAY)) {
            photoKeys.add(Keys.KEY_FILTER_VIDEO_DISPLAY);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_PORTRAIT_REFOCUS_KEY)) {//SPRD:fix bug944254
            photoKeys.add(Keys.KEY_PORTRAIT_REFOCUS_KEY);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY)) {
            photoKeys.add(Keys.KEY_COLOR_EFFECT_PHOTO_DISPLAY);
        }
        if (DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_FLASH_MODE)) {//SPRD:fix bug938988
            photoKeys.add(Keys.KEY_FLASH_MODE);
        }
        photoKeys.add(Keys.KEY_DREAM_FLASH_GIF_PHOTO_MODE);
        photoKeys.add(Keys.KEY_COUNTDOWN_DURATION);
        if(DataModuleManager                                     //bugfix 929569
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.KEY_CAMERA_HDR)){
            photoKeys.add(Keys.KEY_CAMERA_HDR);
        }
        photoKeys.add(Keys.KEY_CAMER_METERING);

        if(DataModuleManager
                .getInstance(mAppController.getAndroidContext()).getCurrentDataModule().isEnableSettingConfig(Keys.PREF_KEY_MONTIONPHOTO)){
            photoKeys.add(Keys.PREF_KEY_MONTIONPHOTO);
        }

        VideoKeys.add(Keys.KEY_VIDEO_FLASH_MODE);

        CameraKeys.add(Keys.KEY_CAMERA_ID);

        updatePhotoButtonItems(photoKeys);
        updateVideoButtonItems(VideoKeys);
        updateCameraButtonItems(CameraKeys);
    }
    public void correctRefocusButtonState(){
        if(mButtonRefocusDream != null){
            mButtonRefocusDream.setState(mAppController.getCurrentModuleIndex()== SettingsScopeNamespaces.AUTO_PHOTO?0:1);
        }
    }

    public void correctFilterButtonState(){
        if(mButtonFilterDream != null){
            mButtonFilterDream.setState(mAppController.getCurrentModuleIndex()== SettingsScopeNamespaces.AUTO_PHOTO?0:1);
        }
    }

    private boolean mIsFrontCamera;
    private int mFlashMode;
    private int mModuleId = -1;

    public void setFlashMode(boolean isFrontCamera, int flashMode, int moduleId) {
        mIsFrontCamera = isFrontCamera;
        mFlashMode = flashMode;
        mModuleId = moduleId;
    }

    private int getFlashResID(int button) {
        if (mIsFrontCamera
                && mFlashMode == CameraUtil.VALUE_FRONT_FLASH_MODE_LED) {
            switch (button) {
                case ButtonManagerDream.BUTTON_FLASH_DREAM:
                    return R.array.dream_camera_front_led_flashmode_icons;
                case ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM:
                    return R.array.dream_video_front_led_flashmode_icons;
            }

        } else {
            switch (button) {
                case ButtonManagerDream.BUTTON_FLASH_DREAM:
                    if (mModuleId == SettingsScopeNamespaces.MANUAL) {
                        return R.array.dream_camera_manual_flashmode_icons;
                    } else if (mModuleId == SettingsScopeNamespaces.MACRO_PHOTO) {
                        return R.array.dream_camera_torch_flashmode_icons;
                    }
                    return R.array.dream_camera_flashmode_icons;
                case ButtonManagerDream.BUTTON_VIDEO_FLASH_DREAM:
                    return R.array.dream_video_flashmode_icons;
            }
        }
		return -1;
	}

	public void startAnimation(int buttonID){
        ImageView imageView = getButtonOrError(buttonID);
        AnimationDrawable animationDrawable = (AnimationDrawable)imageView.getDrawable();
        animationDrawable.stop();
        animationDrawable.start();
    }
}
