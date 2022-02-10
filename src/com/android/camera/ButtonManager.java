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

package com.android.camera;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera2.R;

/**
 * A  class for generating pre-initialized
 * {@link #android.widget.ImageButton}s.
 */
public class ButtonManager {
    public static final int BUTTON_FLASH = 0;
    public static final int BUTTON_TORCH = 1;
    public static final int BUTTON_HDR_PLUS_FLASH = 2;
    public static final int BUTTON_CAMERA = 3;
    public static final int BUTTON_HDR_PLUS = 4;
    public static final int BUTTON_HDR = 5;
    public static final int BUTTON_CANCEL = 6;
    public static final int BUTTON_DONE = 7;
    public static final int BUTTON_RETAKE = 8;
    public static final int BUTTON_REVIEW = 9;
    public static final int BUTTON_GRID_LINES = 10;
    public static final int BUTTON_EXPOSURE_COMPENSATION = 11;
    public static final int BUTTON_COUNTDOWN = 12;

    /** For two state MultiToggleImageButtons, the off index. */
    public static final int OFF = 0;
    /** For two state MultiToggleImageButtons, the on index. */
    public static final int ON = 1;

    private static final int NO_RESOURCE = -1;
    private final static Log.Tag TAG = new Log.Tag("ButtonManager");

    /** Bottom bar options toggle buttons. */
    private MultiToggleImageButton mButtonCamera;
    private MultiToggleImageButton mButtonFlash;
    private MultiToggleImageButton mButtonHdr;
    private MultiToggleImageButton mButtonGridlines;
    private MultiToggleImageButton mButtonCountdown;

    /** Intent UI buttons. */
    public ImageButton mButtonCancel;
    public ImageButton mButtonDone;
    public ImageButton mButtonRetake; // same as review.

    private ImageButton mButtonExposureCompensation;

    private int mMinExposureCompensation;
    private int mMaxExposureCompensation;
    private float mExposureCompensationStep;

    /** A listener for button enabled and visibility
        state changes. */
    private ButtonStatusListener mListener;

    /** An reference to the gcam mode index. */
    private static int sGcamIndex;

    /** Whether Camera Button can be enabled by generic operations. */
    private boolean mIsCameraButtonBlocked;

    private final AppController mAppController;

    /**
     * Get a new global ButtonManager.
     */
    public ButtonManager(AppController app) {
        mAppController = app;

        Context context = app.getAndroidContext();
        sGcamIndex = SettingsScopeNamespaces.GCAM;
        /*SPRD:fix bug 561378 JavaCrash:java.lang.NullPointerException @{*/
        if (mAppController != null && (mAppController instanceof CameraActivity)) {
            CameraActivity cameraActivity = (CameraActivity)mAppController;
            if (cameraActivity.isDestroyed()) {
                return;
            }
        }
        /* @} */
    }

    /**
     * Load references to buttons under a root View.
     * Call this after the root clears/reloads all of its children
     * to prevent stale references button views.
     */
    public void load(View root) {
        getButtonsReferences(root);
    }

    /**
     * ButtonStatusListener provides callbacks for when button's
     * visibility changes and enabled status changes.
     */
    public interface ButtonStatusListener {
        /**
         * A button's visibility has changed.
         */
        public void onButtonVisibilityChanged(ButtonManager buttonManager, int buttonId);

        /**
         * A button's enabled state has changed.
         */
        public void onButtonEnabledChanged(ButtonManager buttonManager, int buttonId);
    }

    /**
     * Sets the ButtonStatusListener.
     */
    public void setListener(ButtonStatusListener listener) {
        mListener = listener;
    }

    /**
     * Gets references to all known buttons.
     */
    protected void getButtonsReferences(View root) {
        /*mButtonCamera
            = (MultiToggleImageButton) root.findViewById(R.id.camera_toggle_button);
        mButtonFlash
            = (MultiToggleImageButton) root.findViewById(R.id.flash_toggle_button);
        mButtonHdr
            = (MultiToggleImageButton) root.findViewById(R.id.hdr_plus_toggle_button);
        */
        /**
          * SPRD: Change for New Feature VGesture
          * original code
          * @{
        mButtonGridlines
            = (MultiToggleImageButton) root.findViewById(R.id.grid_lines_toggle_button);
           */
        /**
          * @}
          */
        mButtonCancel
            = (ImageButton) root.findViewById(R.id.cancel_button);
        mButtonDone
            = (ImageButton) root.findViewById(R.id.done_button);
        mButtonRetake
            = (ImageButton) root.findViewById(R.id.retake_button);
    }

    /**
     * A callback executed in the state listener of a button.
     *
     * Used by a module to set specific behavior when a button's
     * state changes.
     */
    public interface ButtonCallback {
        public void onStateChanged(int state);
    }

    /**
     * Returns the appropriate {@link com.android.camera.MultiToggleImageButton}
     * based on button id.  An IllegalStateException will be throw if the
     * button could not be found in the view hierarchy.
     */
    protected MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
            case BUTTON_FLASH:
                checkButtonNull(mButtonFlash,"Flash button could not be found.");
                return mButtonFlash;
            case BUTTON_TORCH:
                checkButtonNull(mButtonFlash,"Torch button could not be found.");
                return mButtonFlash;
            case BUTTON_HDR_PLUS_FLASH:
                checkButtonNull(mButtonFlash,"Hdr plus torch button could not be found.");
                return mButtonFlash;
            case BUTTON_CAMERA:
                checkButtonNull(mButtonCamera,"Camera button could not be found.");
                return mButtonCamera;
            case BUTTON_HDR_PLUS:
                checkButtonNull(mButtonHdr,"Hdr plus button could not be found.");
                return mButtonHdr;
            case BUTTON_HDR:
                checkButtonNull(mButtonHdr,"Hdr button could not be found.");
                return mButtonHdr;
            case BUTTON_GRID_LINES:
                checkButtonNull(mButtonGridlines,"Grid lines button could not be found.");
                return mButtonGridlines;
            case BUTTON_COUNTDOWN:
                checkButtonNull(mButtonCountdown,"Countdown button could not be found.");
                return mButtonCountdown;
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
        }
    }

    protected void checkButtonNull(MultiToggleImageButton bt, String message) {
        if (bt == null) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Returns the appropriate {@link android.widget.ImageButton}
     * based on button id.  An IllegalStateException will be throw if the
     * button could not be found in the view hierarchy.
     */
    protected ImageButton getImageButtonOrError(int buttonId) {
        switch (buttonId) {
            case BUTTON_CANCEL:
                if (mButtonCancel == null) {
                    throw new IllegalStateException("Cancel button could not be found.");
                }
                return mButtonCancel;
            case BUTTON_DONE:
                if (mButtonDone == null) {
                    throw new IllegalStateException("Done button could not be found.");
                }
                return mButtonDone;
            case BUTTON_RETAKE:
                if (mButtonRetake == null) {
                    throw new IllegalStateException("Retake button could not be found.");
                }
                return mButtonRetake;
            case BUTTON_REVIEW:
                if (mButtonRetake == null) {
                    throw new IllegalStateException("Review button could not be found.");
                }
                return mButtonRetake;
            case BUTTON_EXPOSURE_COMPENSATION:
                if (mButtonExposureCompensation == null) {
                    throw new IllegalStateException("Exposure Compensation button could not be found.");
                }
                return mButtonExposureCompensation;
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
        }
    }

    /**
     * Initialize a known button by id with a state change callback, and then
     * enable the button.
     *
     * @param buttonId The id if the button to be initialized.
     * @param cb The callback to be executed after the button state change.
     */
    public void initializeButton(int buttonId, ButtonCallback cb) {
        initializeButton(buttonId, cb, null);
    }

    /**
     * Initialize a known button by id, with a state change callback and a state
     * pre-change callback, and then enable the button.
     *
     * @param buttonId The id if the button to be initialized.
     * @param cb The callback to be executed after the button state change.
     * @param preCb The callback to be executed before the button state change.
     */
    public void initializeButton(int buttonId, ButtonCallback cb, ButtonCallback preCb) {
        MultiToggleImageButton button = null;
        try {
            button = getButtonOrError(buttonId);
        } catch (Exception e) {
            Log.w(TAG, " button find error", e);
        }
        if (button == null) return;
        switch (buttonId) {
            case BUTTON_FLASH:
                initializeFlashButton(button, cb, preCb, R.array.camera_flashmode_icons);
                break;
            case BUTTON_TORCH:
                initializeTorchButton(button, cb, preCb, R.array.video_flashmode_icons);
                break;
            case BUTTON_HDR_PLUS_FLASH:
                initializeHdrPlusFlashButton(button, cb, preCb, R.array.camera_flashmode_icons);
                break;
            case BUTTON_CAMERA:
                initializeCameraButton(button, cb, preCb, R.array.camera_id_icons);
                break;
            case BUTTON_HDR_PLUS:
                initializeHdrPlusButton(button, cb, preCb, R.array.pref_camera_hdr_plus_icons);
                break;
            case BUTTON_HDR:
                initializeHdrButton(button, cb, preCb, R.array.pref_camera_hdr_icons);
                break;
            case BUTTON_GRID_LINES:
                initializeGridLinesButton(button, cb, preCb, R.array.grid_lines_icons);
                break;
            case BUTTON_COUNTDOWN:
                initializeCountdownButton(button, cb, preCb, R.array.countdown_duration_icons);
                break;
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
        }

        showButton(buttonId);
        enableButton(buttonId);
    }

    /**
     * Initialize a known button with a click listener and a drawable resource id,
     * and a content description resource id.
     * Sets the button visible.
     */
    public void initializePushButton(int buttonId, View.OnClickListener cb,
            int imageId, int contentDescriptionId) {
        ImageButton button = getImageButtonOrError(buttonId);
        button.setOnClickListener(cb);
        if (imageId != NO_RESOURCE) {
            button.setImageResource(imageId);
        }
        if (contentDescriptionId != NO_RESOURCE) {
            button.setContentDescription(mAppController
                    .getAndroidContext().getResources().getString(contentDescriptionId));
        }

        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, buttonId);

        if (button.getVisibility() != View.VISIBLE) {
            button.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    /**
     * Initialize a known button with a click listener and a resource id.
     * Sets the button visible.
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
        disableButton(BUTTON_CAMERA);
    }

    /**
     * Sets a button in its disabled (greyed out) state.
     */
    public void disableButton(int buttonId) {
        View button;
        if (buttonId == BUTTON_EXPOSURE_COMPENSATION) {
            button = getImageButtonOrError(buttonId);
        } else {
            button = getButtonOrError(buttonId);
        }
        // HDR and HDR+ buttons share the same button object,
        // but change actual image icons at runtime.
        // This extra check is to ensure the correct icons are used
        // in the case of the HDR[+] button being disabled at startup,
        // e.g. app startup with front-facing camera.
        // b/18104680
        if (buttonId == BUTTON_HDR_PLUS) {
            initializeHdrPlusButtonIcons((MultiToggleImageButton) button, R.array.pref_camera_hdr_plus_icons);
        } else if (buttonId == BUTTON_HDR) {
            initializeHdrButtonIcons((MultiToggleImageButton) button, R.array.pref_camera_hdr_icons);
        }
        if (button.isEnabled()) {
            button.setEnabled(false);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, null);
    }

    /**
     * Enables the camera button and removes the block that was set by
     * {@link #disableCameraButtonAndBlock()}.
     */
    public void enableCameraButton() {
        // SPRD: Bug 502464
        mAppController.getCameraAppUI().setSwipeEnabled(true);

        mIsCameraButtonBlocked = false;
        enableButton(BUTTON_CAMERA);
    }

    /**
     * Enables a button that has already been initialized.
     */
    public void enableButton(int buttonId) {
        // If Camera Button is blocked, ignore the request.
        if(buttonId == BUTTON_CAMERA && mIsCameraButtonBlocked) {
            return;
        }
        ImageButton button;
        // Manual exposure uses a regular image button instead of a
        // MultiToggleImageButton, so it requires special handling.
        // TODO: Redesign ButtonManager's button getter methods into one method.
        if (buttonId == BUTTON_EXPOSURE_COMPENSATION) {
            button = getImageButtonOrError(buttonId);
        } else {
            button = getButtonOrError(buttonId);
        }
        // SPRD:fix bug528520 flash icon do not show after close hdr
        button.setTag(R.string.tag_enabled_id, buttonId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (mListener != null) {
                mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        /* SPRD:fix bug528520 flash icon do not show after close hdr
         * android original code
        button.setTag(R.string.tag_enabled_id, buttonId);
        */
    }

    /**
     * Disable click reactions for a button without affecting visual state.
     * For most cases you'll want to use {@link #disableButton(int)}.
     * @param buttonId The id of the button.
     */
    public void disableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(false);
        }
    }

    /**
     * Enable click reactions for a button without affecting visual state.
     * For most cases you'll want to use {@link #enableButton(int)}.
     * @param buttonId The id of the button.
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
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
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
            if (mListener != null) {
                mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }


    public void setToInitialState() {
//        mModeOptions.setMainBar(ModeOptions.BAR_STANDARD);
    }

    public void setExposureCompensationCallback(final CameraAppUI.BottomBarUISpec
                                        .ExposureCompensationSetCallback cb) {
    }

    /**
     * Set the exposure compensation parameters supported by the current camera mode.
     * @param min Minimum exposure compensation value.
     * @param max Maximum exposure compensation value.
     * @param step Expsoure compensation step value.
     */
    public void setExposureCompensationParameters(int min, int max, float step) {
        mMaxExposureCompensation = max;
        mMinExposureCompensation = min;
        mExposureCompensationStep = step;

        updateExposureButtons();
    }

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
    public float getExposureCompensationStep() {
        return mExposureCompensationStep;
    }

    /**
     * Check if a button is enabled with the given button id..
     */
    public boolean isEnabled(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
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
    }

    /**
     * Initialize video torch button
     */
    private void initializeTorchButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
    }

    /**
     * Initialize hdr plus flash button
     */
    private void initializeHdrPlusFlashButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
    }

    /**
     * Initialize a camera button.
     */
    private void initializeCameraButton(final MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
    }

    /**
     * Initialize an hdr plus button.
     */
    private void initializeHdrPlusButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
    }

    private void initializeHdrPlusButtonIcons(MultiToggleImageButton button, int resIdImages) {
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

    }

    private void initializeHdrButtonIcons(MultiToggleImageButton button, int resIdImages) {
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
    }

    /**
     * Update the visual state of the manual exposure buttons
     */
    public void updateExposureButtons() {
    }

    /**
     * Initialize a grid lines button.
     */
    private void initializeGridLinesButton(MultiToggleImageButton button,
            final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {
    }

   /**
     * Initialize a panorama orientation buttons.
     */
    public void initializePanoOrientationButtons(final ButtonCallback cb) {
    }

    private void updatePanoButtons() {
    }

    private void setPreChangeCallback(MultiToggleImageButton button, final ButtonCallback preCb) {
        button.setOnPreChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                Log.d(TAG, "PreChangeCallback on stateChanged");
                if(preCb != null) {
                    preCb.onStateChanged(state);
                }
            }
        });
    }

    public void refreshButtonState(){
    }

    public int getMakeupButtonState() {
        // add for get Makeup Button State for AI Scene Detection
        return -2;
    }
}
