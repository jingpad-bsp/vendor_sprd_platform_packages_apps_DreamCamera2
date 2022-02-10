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

package com.android.camera;

import android.app.Activity;

import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.google.common.logging.eventprotos;

public final class FatalErrorHandlerImpl implements FatalErrorHandler {
    private static final Log.Tag TAG = new Log.Tag("FatalErrorHandler");

    private final Activity mActivity;

    public FatalErrorHandlerImpl(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onMediaStorageFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Media Storage Failure:", ex);


        Reason reason = Reason.MEDIA_STORAGE_FAILURE;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onCameraOpenFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Camera Open Failure:", ex);

        Reason reason = Reason.CANNOT_CONNECT_TO_CAMERA;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onCameraReconnectFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Camera Reconnect Failure:", ex);

        Reason reason = Reason.CANNOT_CONNECT_TO_CAMERA;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onGenericCameraAccessFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Camera Access Failure:", ex);

        Reason reason = Reason.CANNOT_CONNECT_TO_CAMERA;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onCameraDisabledFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Camera Disabled Failure:", ex);

        Reason reason = Reason.CAMERA_DISABLED_BY_SECURITY_POLICY;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onMediaRecorderDisabledFailure() {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling MediaRecorder Disabled Failure:", ex);

        Reason reason = Reason.MEDIARECORDER_START_FAILURE;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }

    @Override
    public void onExternalStorageAccessFailure() {
        Reason reason = Reason.EXTERNAL_STORAGE_ACCESS_FAILURE;
        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, null);
    }

    @Override
    public void handleFatalError(Reason reason) {
        Exception ex = new Exception();
        // Log a stack trace to be sure we can track the source.
        Log.e(TAG, "Handling Fatal Error:", ex);

        boolean finishActivity = reason.doesFinishActivity();
        CameraUtil.showDialog(mActivity, reason.getDialogTitle(), reason.getDialogMsgId(), reason.getFeedbackMsgId(),
                finishActivity, ex);
    }
}
