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

package com.android.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.camera.app.OrientationManager;
import com.android.camera.CaptureActivity;

import com.android.camera.ui.RotateImageButton;

import com.android.camera2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * TopRightWeightedLayout is a LinearLayout that reorders its
 * children such that the right most child is the top most child
 * on an orientation change.
 */
/*
 * SPRD Bug:527741 Button disappeared. @{
 * Original Android code:

public class TopRightWeightedLayout extends LinearLayout {

 */
public class TopRightWeightedLayout extends LinearLayout implements
        OrientationManager.OnOrientationChangeListener {

    public TopRightWeightedLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        Configuration configuration = getContext().getResources().getConfiguration();
        checkOrientation(configuration.orientation);

        // SPRD Bug:527741 Button disappeared.
        if (getContext() != null && getContext() instanceof CaptureActivity) {
            ((CaptureActivity) getContext()).getOrientationManager()
                    .addOnOrientationChangeListener(this);
            canelBtn = (RotateImageButton) ((CaptureActivity) getContext())
                    .findViewById(R.id.cancel_button);
            doneBtn = (RotateImageButton) ((CaptureActivity) getContext())
                    .findViewById(R.id.done_button);
            retakeBtn = (RotateImageButton) ((CaptureActivity) getContext())
                    .findViewById(R.id.retake_button);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    /**
     * Set the orientation of this layout if it has changed,
     * and center the elements based on the new orientation.
     */
    /**
     *SPRD: Change for New Feature Gif
     *original code
     * @{
    private void checkOrientation(int orientation) {
     */
    public void checkOrientation(int orientation) {
    /**
     * @}
     */
        final boolean isHorizontal = LinearLayout.HORIZONTAL == getOrientation();
        final boolean isPortrait = Configuration.ORIENTATION_PORTRAIT == orientation;
        if (isPortrait && !isHorizontal) {
            // Portrait orientation is out of sync, setting to horizontal
            // and reversing children
            fixGravityAndPadding(LinearLayout.HORIZONTAL);
            setOrientation(LinearLayout.HORIZONTAL);
            reverseChildren();
            requestLayout();
        } else if (!isPortrait && isHorizontal) {
            // Landscape orientation is out of sync, setting to vertical
            // and reversing children
            fixGravityAndPadding(LinearLayout.VERTICAL);
            setOrientation(LinearLayout.VERTICAL);
            reverseChildren();
            requestLayout();
        }
    }

    /**
     * Reverse the ordering of the children in this layout.
     * Note: bringChildToFront produced a non-deterministic ordering.
     */
    private void reverseChildren() {
        List<View> children = new ArrayList<View>();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            children.add(getChildAt(i));
        }
        for (View v : children) {
            bringChildToFront(v);
        }
    }

    /**
     * Swap gravity:
     * left for bottom
     * right for top
     * center horizontal for center vertical
     * etc
     *
     * also swap left|right padding for bottom|top
     */
    private void fixGravityAndPadding(int direction) {
        for (int i = 0; i < getChildCount(); i++) {
            // gravity swap
            View v = getChildAt(i);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) v.getLayoutParams();
            int gravity = layoutParams.gravity;

            if (direction == LinearLayout.VERTICAL) {
                if ((gravity & Gravity.LEFT) != 0) {   // if gravity left is set . . .
                    gravity &= ~Gravity.LEFT;          // unset left
                    gravity |= Gravity.BOTTOM;         // and set bottom
                }
            } else {
                if ((gravity & Gravity.BOTTOM) != 0) { // etc
                    gravity &= ~Gravity.BOTTOM;
                    gravity |= Gravity.LEFT;
                }
            }

            if (direction == LinearLayout.VERTICAL) {
                if ((gravity & Gravity.RIGHT) != 0) {
                    gravity &= ~Gravity.RIGHT;
                    gravity |= Gravity.TOP;
                }
            } else {
                if ((gravity & Gravity.TOP) != 0) {
                    gravity &= ~Gravity.TOP;
                    gravity |= Gravity.RIGHT;
                }
            }

            // don't mess with children that are centered in both directions
            if ((gravity & Gravity.CENTER) != Gravity.CENTER) {
                if (direction == LinearLayout.VERTICAL) {
                    if ((gravity & Gravity.CENTER_VERTICAL) != 0) {
                        gravity &= ~ Gravity.CENTER_VERTICAL;
                        gravity |= Gravity.CENTER_HORIZONTAL;
                    }
                } else {
                    if ((gravity & Gravity.CENTER_HORIZONTAL) != 0) {
                        gravity &= ~ Gravity.CENTER_HORIZONTAL;
                        gravity |= Gravity.CENTER_VERTICAL;
                    }
                }
            }

            layoutParams.gravity = gravity;

            // padding swap
            int paddingLeft = v.getPaddingLeft();
            int paddingTop = v.getPaddingTop();
            int paddingRight = v.getPaddingRight();
            int paddingBottom = v.getPaddingBottom();
            v.setPadding(paddingBottom, paddingRight, paddingTop, paddingLeft);
        }
    }

    // SPRD Bug:527741 Button disappeared.
    private RotateImageButton canelBtn;
    private RotateImageButton doneBtn;
    private RotateImageButton retakeBtn;

    @Override
    public void onOrientationChanged(OrientationManager orientationManager,
            OrientationManager.DeviceOrientation deviceOrientation) {
        int orientation = deviceOrientation.getDegrees();

        if (canelBtn != null) {
            canelBtn.setOrientation(orientation, true);
        }
        if (doneBtn != null) {
            doneBtn.setOrientation(orientation, true);
        }
        if (retakeBtn != null) {
            retakeBtn.setOrientation(orientation, true);
        }
    }
}