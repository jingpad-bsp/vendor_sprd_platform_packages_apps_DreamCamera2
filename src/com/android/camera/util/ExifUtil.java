/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.camera.util;

import android.hardware.camera2.CaptureResult;
import android.location.Location;
import android.os.Build;

import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.Rational;
import com.google.common.base.Optional;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Exif utility functions.
 */
public class ExifUtil {
    private static final double LOG_2 = Math.log(2); // natural log of 2

    private final ExifInterface mExif;

    /**
     * Construct a new ExifUtil object.
     * @param exif The EXIF object to populate.
     */
    public ExifUtil(ExifInterface exif) {
        mExif = exif;
    }


    /**
     * Adds the given location to the EXIF object.
     *
     * @param location The location to add.
     */
    public void addLocationToExif(Location location) {
        final Long ALTITUDE_PRECISION = 1L; // GPS altitude isn't particularly accurate (determined empirically)

        mExif.addGpsTags(location.getLatitude(), location.getLongitude());
        mExif.addGpsDateTimeStampTag(location.getTime());

        if (location.hasAltitude()) {
            double altitude = location.getAltitude();
            addExifTag(ExifInterface.TAG_GPS_ALTITUDE, rational(altitude, ALTITUDE_PRECISION));
            short altitudeRef = altitude < 0 ? ExifInterface.GpsAltitudeRef.SEA_LEVEL_NEGATIVE
                    : ExifInterface.GpsAltitudeRef.SEA_LEVEL;
            addExifTag(ExifInterface.TAG_GPS_ALTITUDE_REF, altitudeRef);
        }
    }

    private void addExifVersionToExif() {
        addExifTag(ExifInterface.TAG_EXIF_VERSION, ExifInterface.EXIF_VERSION);
    }

    private void addTimestampToExif() {
        final Long MS_TO_S = 1000L; // Milliseconds per second
        final String subSecondFormat = "000";

        Long timestampMs = System.currentTimeMillis();
        TimeZone timezone = Calendar.getInstance().getTimeZone();
        mExif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, timestampMs, timezone);
        mExif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME_DIGITIZED, timestampMs, timezone);
        mExif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME_ORIGINAL, timestampMs, timezone);

        Long subSeconds = timestampMs % MS_TO_S;
        String subSecondsString = new DecimalFormat(subSecondFormat).format(subSeconds);
        addExifTag(ExifInterface.TAG_SUB_SEC_TIME, subSecondsString);
        addExifTag(ExifInterface.TAG_SUB_SEC_TIME_ORIGINAL, subSecondsString);
        addExifTag(ExifInterface.TAG_SUB_SEC_TIME_DIGITIZED, subSecondsString);
    }

    private void addMakeAndModelToExif() {
        addExifTag(ExifInterface.TAG_MAKE, Build.MANUFACTURER);
        addExifTag(ExifInterface.TAG_MODEL, Build.MODEL);
    }

    private void addExifTag(int tagId, Object val) {
        if (val != null) {
            mExif.setTag(mExif.buildTag(tagId, val));
        }
    }

    private Rational ratio(Long numerator, Long denominator) {
        if (numerator != null && denominator != null) {
            return new Rational(numerator, denominator);
        }
        return null;
    }
    private Rational rational(Float value, Long precision) {
        if (value != null && precision != null) {
            return new Rational((long) (value * precision), precision);
        }
        return null;
    }

    private Rational rational(Double value, Long precision) {
        if (value != null && precision != null) {
            return new Rational((long) (value * precision), precision);
        }
        return null;
    }

    private Double log2(Float value) {
        if (value != null) {
            return Math.log(value) / LOG_2;
        }
        return null;
    }

    private Double log2(Double value) {
        if (value != null) {
            return Math.log(value) / LOG_2;
        }
        return null;
    }
}
