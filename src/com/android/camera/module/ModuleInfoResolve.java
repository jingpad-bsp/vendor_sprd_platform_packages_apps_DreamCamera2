package com.android.camera.module;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.NotFoundException;

import com.android.camera.debug.Log;
import com.android.camera2.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import com.android.camera.app.CameraApp;
import com.android.camera.util.CameraUtil;


/**
 * Created by SPREADTRUM\matchbox.chang on 17-12-28.
 */

public class ModuleInfoResolve {

    public static class ModuleItem {
        public int moduelId;
        public int visible;
        public int cameraSupprot;
        public int modeSupport;
        //public String nameSpace;
        public String text;
        public String desc;
        public int unSelectIconId;
        public int selectIconId;
        public int coverIconId;
        public int captureIconId;
    }

    private static final Log.Tag TAG = new Log.Tag("ModuleInfoResolve");
    private ArrayList<Integer> mVisibleModuleList;
    private HashMap<Integer,ModuleItem> mModuleInfo;

    public ModuleInfoResolve(){
        mVisibleModuleList = new ArrayList<>();
        mModuleInfo= new HashMap<>();
    }

    private void getModuleInfo(TypedArray moduleRes){
        if(moduleRes == null){
            Log.e(TAG,"resolve module return null");
            return;
        }
        int moduleId = -1;
        ModuleItem item = new ModuleItem();
        moduleId = moduleRes.getInteger(0,-1);
        if(1 == moduleRes.getInteger(1,0)){
            mVisibleModuleList.add(moduleId);
        }
        //init ModuleItem
        item.moduelId = moduleId;
        item.visible = moduleRes.getInteger(1,0);
        item.cameraSupprot = moduleRes.getInteger(2,0);
        item.modeSupport = moduleRes.getInteger(3,0);
        //item.nameSpace = moduleRes.getString(4);
        item.text = moduleRes.getString(5);
        item.desc = moduleRes.getString(6);
        item.unSelectIconId = moduleRes.getResourceId(7,-1);
        item.selectIconId = moduleRes.getResourceId(8,-1);
        item.coverIconId = moduleRes.getResourceId(9,-1);
        item.captureIconId = moduleRes.getResourceId(10,-1);
        mModuleInfo.put(moduleId,item);
    }

    // SPRD: Bug922759 close some feature when 4in1
    private void updateModuleInfo(Context context) {
        update3DNRModuleInfo(context);
        update3DNRProModuleInfo(context);
        updatePortraitModuleInfo(context);
        updateHighResolutionModuleInfo(context);
        updateMacroModuleInfo(context);
    }

    //update 3dnr module info
    private void update3DNRModuleInfo(Context context) {
        if (CameraUtil.isFront4in1Sensor() || CameraUtil.isFrontYUVSensor()) {
            recycleModuleArray(context,R.array.tdnr_photo_module,R.integer.camera_support_back);
            recycleModuleArray(context,R.array.tdnr_video_module,R.integer.camera_support_back);
        } else if (CameraUtil.isBack4in1Sensor() || CameraUtil.isBackYUVSensor()) {
            recycleModuleArray(context,R.array.tdnr_photo_module,R.integer.camera_support_front);
            recycleModuleArray(context,R.array.tdnr_video_module,R.integer.camera_support_front);
        }
    }

    //update 3dnr pro module info
    private void update3DNRProModuleInfo(Context context) {
        if (CameraUtil.isFront4in1Sensor() || CameraUtil.isFrontYUVSensor()) {
            recycleModuleArray(context,R.array.tdnr_pro_photo_module,R.integer.camera_support_back);
            recycleModuleArray(context,R.array.tdnr_pro_video_module,R.integer.camera_support_back);
        } else if (CameraUtil.isBack4in1Sensor() || CameraUtil.isBackYUVSensor()) {
            recycleModuleArray(context,R.array.tdnr_pro_photo_module,R.integer.camera_support_front);
            recycleModuleArray(context,R.array.tdnr_pro_video_module,R.integer.camera_support_front);
        }
    }

    private void recycleModuleArray(Context context, int arrayId, int supportValue){
        TypedArray moduleArray = context.getResources().obtainTypedArray(arrayId);
        int moduleId = 0;
        try {
            moduleId = moduleArray.getInteger(0, -1);
            if (moduleId != -1) {
                ModuleItem item = mModuleInfo.get(moduleId);
                item.cameraSupprot = context.getResources().getInteger(supportValue);
            }
        } catch (NotFoundException e) {
            Log.e(TAG,"occur Exception:"+e);
        } finally{
            moduleArray.recycle();
        }
    }

    // update portrait module
    private void updatePortraitModuleInfo(Context context) {
        int moduleId = -1;
        TypedArray mModuleArray = null;
        mModuleArray = context.getResources().obtainTypedArray(R.array.portrait_photo_module);
        moduleId = mModuleArray.getInteger(0, -1);
        if (moduleId != -1) {
            ModuleItem item = mModuleInfo.get(moduleId);
            item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_none);
            if(CameraUtil.isPortraitPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_all);
            }else if(CameraUtil.isFrontPortraitPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_front);
            }else if(CameraUtil.isBackPortraitPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_back);
            }
        }
        mModuleArray.recycle();
    }

    // update high resolution module
    private void updateHighResolutionModuleInfo(Context context) {
        int moduleId = -1;
        TypedArray mModuleArray = null;
        mModuleArray = context.getResources().obtainTypedArray(R.array.high_resolution_photo_module);
        moduleId = mModuleArray.getInteger(0, -1);
        if (moduleId != -1) {
            ModuleItem item = mModuleInfo.get(moduleId);
            item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_none);
            if(CameraUtil.isHighResolutionPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_all);
            }else if(CameraUtil.isFrontHighResolutionPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_front);
            }else if(CameraUtil.isBackHighResolutionPhotoEnable()){
                item.cameraSupprot = context.getResources().getInteger(R.integer.camera_support_back);
            }
        }
        mModuleArray.recycle();
    }

    // update macro module
    private void updateMacroModuleInfo(Context context) {
        int moduleId = -1;
        TypedArray mModuleArray = null;
        mModuleArray = context.getResources().obtainTypedArray(R.array.macro_photo_module);
        moduleId = mModuleArray.getInteger(0, -1);
        if (moduleId != -1) {
            ModuleItem item = mModuleInfo.get(moduleId);
            item.text = context.getResources().getString(R.string.camera_mode_macro);
            if(CameraUtil.isSRFusionEnable()){
                item.text = context.getResources().getString(R.string.camera_mode_super_macro);
            }
        }
        mModuleArray.recycle();
    }

    public void resolve(Context context){
        synchronized (this) {
            mVisibleModuleList.clear();
            initModuleInfoFromArray(context); //Unisoc bugfix: 1405101, only update once.
            updateModuleInfo(context);
        }
    }

    private void initModuleInfoFromArray(Context context) {
        if (!CameraApp.backGroundConfigChanged && mModuleInfo.size() > 0) return;
        CameraApp.backGroundConfigChanged = false;
        mModuleInfo.clear();
        TypedArray moduleListRes = context.getResources().obtainTypedArray(R.array.module_list);
        TypedArray moduleRes = null;
        if(moduleListRes == null){
            Log.e(TAG,"resolve module list array return null");
            return;
        }
        for (int i = 0; i < moduleListRes.length(); i++) {
            int moduleResId = moduleListRes.getResourceId(i, -1);
            if (moduleResId < 0) {
                continue;
            }
            moduleRes = context.getResources().obtainTypedArray(moduleResId);
            getModuleInfo(moduleRes);
            moduleRes.recycle();
        }
        moduleListRes.recycle();
    }

    public boolean isModuleVisible(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            return false;
        }
        return 1 == item.visible;
    }

    public String getModuleText(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            return null;
        }
        return item.text;
    }

    public String getModuleDescription(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            return null;
        }
        return item.desc;
    }

    public int getModuleUnselectIcon(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            //return -1;
            return R.drawable.ic_auto_mode_sprd_unselected;
        }
        return item.unSelectIconId;
    }

    public int getModuleSelectIcon(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            //return -1;
            return R.drawable.ic_auto_mode_sprd_selected;
        }
        return item.selectIconId;
    }

    public int getModuleCoverIcon(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            //return -1;
            return R.drawable.ic_camera_blanket;
        }
        return item.coverIconId;
    }

    public int getModuleCaptureIcon(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            //return -1;
            return R.drawable.ic_capture_camera_sprd;
        }
        return item.captureIconId;
    }

    public int getModuleSupportMode(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            return -1;
        }
        return item.modeSupport;
    }

    public int getModuleSupportCamera(int moduleId){
        ModuleItem item = mModuleInfo.get(moduleId);
        if(item == null){
            Log.e(TAG,"moduleId :"+ moduleId + " is not support");
            return -1;
        }
        return item.cameraSupprot;
    }

    public HashMap<Integer,ModuleItem> getModuleItemList(){
        return mModuleInfo;
    }
}
