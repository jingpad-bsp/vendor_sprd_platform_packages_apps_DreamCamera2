package com.android.camera.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The set of auto creations that can be disabled using the
 * {@link OemXmpConstants#PROPERTY_DISABLE_AUTO_CREATION} property.
 */
enum AutoCreation {
  /** Animated GIFs of two or more images. */
  ANIMATION(XmpConstants.VALUE_DISABLE_ANIMATIONS),
  /** A still collage JPEG consisting of two or more images combined. */
  COLLAGE(XmpConstants.VALUE_DISABLE_COLLAGE),
  /** A single extra wide image consistent of two or more images combined. */
  PANO(XmpConstants.VALUE_DISABLE_PANO),
  /** A movie of one or more images and/or videos*/
  MOVIES(XmpConstants.VALUE_DISABLE_MOVIES);

  private final String value;

  AutoCreation(String value) {
    this.value = value;
  }

  String getValue() {
    return value;
  }

  static AutoCreation fromValue(String value) {
    for (AutoCreation autoCreation : values()) {
      if (autoCreation.getValue().equals(value)) {
        return autoCreation;
      }
    }
    return null;
  }

  static String prettyPrint() {
    List<String> stringValues = new ArrayList<>();
    for (AutoCreation creation : values()) {
      stringValues.add(creation.getValue());
    }
    return Arrays.toString(stringValues.toArray(new String[stringValues.size()]));
  }
}
