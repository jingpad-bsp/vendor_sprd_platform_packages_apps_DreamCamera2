package com.dream.camera.modules.panoramadream;

import android.view.KeyEvent;
import android.view.View;

import com.android.camera.CameraActivity;
import com.android.camera.CameraModule;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.module.ModuleController;
import com.android.camera.ui.TouchCoordinate;
import com.android.ex.camera2.portability.CameraAgent;

public class DreamPanoramaModule extends CameraModule implements ModuleController {
    public DreamPanoramaModule(AppController app) {
        super(app);
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
    }

    public void resume() {
    }

    public void destroy() {
    }

    public void pause() {
    }

    public boolean onBackPressed() {
        return false;
    }

    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
    }

    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        return null;
    }

    public boolean isUsingBottomBar() {
        return false;
    }

    public void onPreviewVisibilityChanged(int visibility) {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public HardwareSpec getHardwareSpec() {
        return null;
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
    }

    public void initializeModuleControls() {
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void onShutterButtonClick() {
    }

    public void onShutterButtonLongPressed() {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public String getPeekAccessibilityString() {
        return null;
    }

    public void onStopRecordVoiceClicked(View v) {
    }
}
