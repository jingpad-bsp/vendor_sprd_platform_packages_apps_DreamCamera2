/**
 * 
 */

package com.sprd.camera.aidetection;

import com.android.camera.settings.Keys;
import android.util.Log;

/**
 * @author SPRD
 */
public class AIDetectionController {

    private static final String TAG = "CAM_PhotoModule_AIDetection";

    private int sSmileScoreCount = 0;
    private static int SMILE_STEP_INCRMENT = 1;
    private static int SMILE_STEP_DECREASE = -1;
    public static int SMILE_STEP_MAX = 10;
    public static int SMILE_SCORE_X = 7;
    private static Boolean strValueSmile;
    private static Boolean strValueFaceAttributes;

    public AIDetectionController() {
    }// SPRD: BUG 330578

    public Boolean getSmileValue(Boolean value) {
        strValueSmile = value;
        Log.d(TAG, " getSmileValue strValueSmile=" + strValueSmile);
        return strValueSmile;
    }
    public Boolean getFaceAttributesValue(Boolean value) {
        strValueFaceAttributes = value;
        Log.d(TAG, " getChooseValue strValueFaceAttributes=" + strValueFaceAttributes);
        return strValueFaceAttributes;
    }

    public boolean isChooseAttributes() {
        return (strValueFaceAttributes);
    }
    public boolean isChooseSmile() {
        return (strValueSmile);
    }

    public void setSmileScoreCount(int num) {
        Log.d(TAG, " setSmileScoreCount sSmileScoreCount=" + sSmileScoreCount + "   num=" + num);
        sSmileScoreCount += num;
        if (sSmileScoreCount < 0)
            sSmileScoreCount = 0;
        if (sSmileScoreCount > SMILE_STEP_MAX) {
            sSmileScoreCount = 0;
        }

    }

    public void resetSmileScoreCount(boolean isIncrement) {
        setSmileScoreCount(isIncrement ? SMILE_STEP_INCRMENT : SMILE_STEP_DECREASE);
    }

    public int getSmileScoreCount() {
        return sSmileScoreCount;
    }
}
