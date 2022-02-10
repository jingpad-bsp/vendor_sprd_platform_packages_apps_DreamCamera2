
package com.android.camera.module;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.android.camera.PhotoModule;
import com.android.camera.VideoModule;
import com.dream.camera.modules.highresolutionphoto.HighResolutionPhotoModule;
import com.dream.camera.modules.irphoto.IRPhotoModule;
import com.dream.camera.modules.macrophoto.MacroPhotoModule;
import com.dream.camera.modules.macrovideo.MacroVideoModule;
import com.dream.camera.modules.portraitbackgroundreplacementphoto.sprd.PortraitBackgroundReplacementPhotoModule;
import com.dream.camera.modules.portraitbackgroundreplacementvideo.sprd.PortraitBackgroundReplacementVideoModule;
import com.dream.camera.modules.rangefind.TDRangeFindModule;
import com.dream.camera.modules.portraitphoto.PortraitPhotoModule;
import com.dream.camera.modules.tdvideo.TDVideoModule;
import com.android.camera.app.AppController;
import com.android.camera.app.ModuleManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera2.R;
import com.dream.camera.modules.autophoto.AutoPhotoModule;
import com.dream.camera.modules.autovideo.AutoVideoModule;
import com.dream.camera.modules.continuephoto.ContinuePhotoModule;
import com.dream.camera.modules.intervalphoto.IntervalPhotoModule;
import com.dream.camera.modules.manualphoto.ManualPhotoModule;
import com.dream.camera.modules.panoramadream.DreamPanoramaModule;
import com.dream.camera.modules.scenephoto.ScenePhotoModule;
import com.dream.camera.modules.slowmotionvideo.SlowmotionVideoModule;
import com.dream.camera.modules.timelapsevideo.TimelapseVideoModule;
import com.dream.camera.modules.intentcapture.DreamIntentCaptureModule;
import com.dream.camera.modules.intentvideo.DreamIntentVideoModule;
import com.dream.camera.modules.AudioPicture.AudioPictureModule;
import com.dream.camera.modules.qr.QrCodePhotoModule;
import com.dream.camera.modules.tdphotomodule.TDPhotoModule;
import com.dream.camera.modules.blurrefocus.BlurRefocusModule;
import com.dream.camera.modules.blurrefocus.FrontBlurRefocusModule;
import com.dream.camera.modules.tdnrphoto.TDNRPhotoModule;
import com.dream.camera.modules.tdnrvideo.TDNRVideoModule;
import com.dream.camera.modules.tdnrprophoto.TDNRProPhotoModule;
import com.dream.camera.modules.tdnrprovideo.TDNRProVideoModule;
import com.dream.camera.modules.filter.sprd.FilterModuleSprd;
import com.dream.camera.modules.ultrawideangle.UltraWideAngleModule;
import com.dream.camera.modules.focuslengthfusion.FocusLengthFusionPhotoModule;
import com.dream.camera.modules.fdrphoto.FDRPhotoModule;
public class ModulesInfo {
    private static final Log.Tag TAG = new Log.Tag("ModulesInfo");

    public static void setupModules(Context context, ModuleManager moduleManager) {
        setupDreamModules(context, moduleManager);
    }

    private static void registerBlurRefocusModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new BlurRefocusModule(app);
            }
        });
    }

    private static void registerFrontBlurRefocusModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new FrontBlurRefocusModule(app);
                }
            });
    }

    private static void registerIntentVideoModule(ModuleManager moduleManager,
            final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new DreamIntentVideoModule(app);
            }
        });
    }

    // Dream Camera Modules
    public static void setupDreamModules(Context context, ModuleManager moduleManager) {

        Resources res = context.getResources();

        registerAutoPhotoModule(moduleManager,SettingsScopeNamespaces.AUTO_PHOTO);
        moduleManager.setDefaultModuleIndex(SettingsScopeNamespaces.AUTO_PHOTO);

        if (CameraUtil.isManualPhotoEnable()) {
            registerManualPhotoModule(moduleManager,SettingsScopeNamespaces.MANUAL);
        }

        if (CameraUtil.isContinuePhotoEnabled()) {
            registerContinuePhotoModule(moduleManager,SettingsScopeNamespaces.CONTINUE);
        }

        if (CameraUtil.isIntervalPhotoEnabled()) {
            registerIntervalPhotoModule(moduleManager,SettingsScopeNamespaces.INTERVAL);
        }

        registerIntentCaptureModule(moduleManager,SettingsScopeNamespaces.INTENTCAPTURE);

        registerIntentVideoModule(moduleManager,SettingsScopeNamespaces.INTENTVIDEO);

        if (CameraUtil.isWideAngleEnable()) {
            registerDreamPanoramaModule(moduleManager,SettingsScopeNamespaces.DREAM_PANORAMA);
        }

        if (CameraUtil.hasBlurRefocusCapture()) {
            registerBlurRefocusModule(moduleManager, SettingsScopeNamespaces.REFOCUS);
        }

        if (CameraUtil.hasFrontBlurRefocusCapture()){
            registerFrontBlurRefocusModule(moduleManager, SettingsScopeNamespaces.FRONT_BLUR);
        }

        // #Bug 892088  disable SceneModule temporary
        //registerScenePhotoModule(moduleManager,SettingsScopeNamespaces.SCENE);

        // registerPipModule(moduleManager, res.getInteger(R.integer.camera_mode_scene),
        // SettingsScopeNamespaces.PIP);

        registerAutoVideoModule(moduleManager,SettingsScopeNamespaces.AUTO_VIDEO);
        // registerVivModule(moduleManager, res.getInteger(R.integer.camera_mode_viv),
        // SettingsScopeNamespaces.VIV);

        if (CameraUtil.isTimelapseEnabled()) {
            registerTimelapseVideoModule(moduleManager,SettingsScopeNamespaces.TIMELAPSE);
        }

        if (CameraUtil.isSlowMotionEnabled()) {
            registerSlowmotionVideoModule(moduleManager,SettingsScopeNamespaces.SLOWMOTION);
        }

        if (CameraUtil.isUseSprdFilter()) {
            registerUcamFilterPhotoModule(moduleManager,SettingsScopeNamespaces.FILTER);
        }

        if (CameraUtil.isVoicePhotoEnable()) {
            registerAudioPictureModule(moduleManager,SettingsScopeNamespaces.AUDIO_PICTURE);
        }

        if (CameraUtil.isQrCodeEnabled()) {
            registerQrCodeModule(moduleManager,SettingsScopeNamespaces.QR_CODE);
        }

        /* SPRD: Fix bug 585183 Adds new features 3D recording @{ */
        if (CameraUtil.isTDVideoEnable()) {
            registerTDVideoModule(moduleManager,SettingsScopeNamespaces.TDVIDEO);
        }
        /* @} */
        if (CameraUtil.isTDPhotoEnable()) {
            registerTDPhotoModule(moduleManager,SettingsScopeNamespaces.TDPHOTO);
        }

        /* SPRD:  Fix bug 585183 Adds new feature real-time distance measurement @{ */
        if (CameraUtil.isTDRangeFindEnable()) {
            registerTDRangFindModule(moduleManager,SettingsScopeNamespaces.TDRANGFINDEnable);
        }

        if (CameraUtil.is3DNREnable()) {
            registerTDNRPhotoModule(moduleManager, SettingsScopeNamespaces.TDNR_PHOTO);
            registerTDNRVideoModule(moduleManager, SettingsScopeNamespaces.TDNR_VIDEO);
        }

        if (CameraUtil.is3DNRProEnable()) {
            registerTDNRProPhotoModule(moduleManager, SettingsScopeNamespaces.TDNR_PRO_PHOTO);
            registerTDNRProVideoModule(moduleManager, SettingsScopeNamespaces.TDNR_PRO_VIDEO);
        }
        if(CameraUtil.isARPhotoEnabled()){
            registerARPhotoModule(moduleManager, SettingsScopeNamespaces.AR_PHOTO);
        }

        if(CameraUtil.isARVideoEnabled()){
            registerARVideoModule(moduleManager, SettingsScopeNamespaces.AR_VIDEO);
        }

        if(CameraUtil.isUltraWideAngleEnabled()) {
            registerUltraWideAngleModule(moduleManager , SettingsScopeNamespaces.BACK_ULTRA_WIDE_ANGLE);
        }

//        if (CameraUtil.isPortraitPhotoEnable()){
            registerPortraitPhotoModule(moduleManager, SettingsScopeNamespaces.PORTRAIT_PHOTO);

//        }
        if (CameraUtil.isFrontHighResolutionPhotoEnable() || CameraUtil.isBackHighResolutionPhotoEnable()){
            registerHighResolutionPhotoModule(moduleManager, SettingsScopeNamespaces.HIGH_RESOLUTION_PHOTO);
        }

        if (CameraUtil.isIRPhotoEnable()) {
            registerIRPhotoModule(moduleManager, SettingsScopeNamespaces.IR_PHOTO);
        }

        if (CameraUtil.isFocusLengthFusionPhotoEnable()) {
            registerFocusLengthFusionPhotoModule(moduleManager, SettingsScopeNamespaces.FOCUS_LENGTH_FUSION_PHOTO);
        }

        if (CameraUtil.isMacroPhotoEnable() || CameraUtil.isSRFusionEnable()) {
            registerMacroPhotoModule(moduleManager, SettingsScopeNamespaces.MACRO_PHOTO);
        }

        if (CameraUtil.isMacroVideoEnable()) {
            registerMacroVideoModule(moduleManager, SettingsScopeNamespaces.MACRO_VIDEO);
        }
        if (CameraUtil.isPortraitBackgroundReplacementEnable()) {
            registerPortraitBackgroundReplacementPhotoModule(moduleManager, SettingsScopeNamespaces.PORTRAIT_BACKGROUNDREPLACEMENT_PHOTO);
        }
        if (CameraUtil.isPortraitBackgroundReplacementEnable()) {
            registerPortraitBackgroundReplacementVideoModule(moduleManager, SettingsScopeNamespaces.PORTRAIT_BACKGROUNDREPLACEMENT_VIDEO);
        }
        if (CameraUtil.isFDRPhotoEnable()) {
            registerFDRPhotoModule(moduleManager, SettingsScopeNamespaces.FDR_PHOTO);
        }
    }

    private static void registerAutoPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                Log.i(TAG, "Create Module moduleId=" + moduleId);
                return new AutoPhotoModule(app);
            }
        });
    }

    private static void registerManualPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                Log.i(TAG, "Create Module moduleId=" + moduleId );
                return new ManualPhotoModule(app);
            }
        });
    }

    private static void registerContinuePhotoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {

                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    Log.i(TAG, "Create Module moduleId=" + moduleId);
                    return new ContinuePhotoModule(app);
                }
            });
    }

    private static void registerIntervalPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    Log.i(TAG, "Create Module moduleId=" + moduleId);
                    return new IntervalPhotoModule(app);
                }
            });
    }

    private static void registerDreamPanoramaModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    Log.i(TAG, "Create Module moduleId=" + moduleId);
                    return new DreamPanoramaModule(app);
                }
            });
    }

    private static void registerScenePhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                Log.i(TAG, "Create Module moduleId=" + moduleId);
                return new ScenePhotoModule(app);
            }
        });
    }

    private static void registerAutoVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {

            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                Log.i(TAG, "Create Module moduleId=" + moduleId);
                return new AutoVideoModule(app);
            }
        });
    }

    private static void registerTimelapseVideoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {

                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    Log.i(TAG, "Create Module moduleId=" + moduleId);
                    return new TimelapseVideoModule(app);
                }
            });
    }

    private static void registerSlowmotionVideoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {

                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    Log.i(TAG, "Create Module moduleId=" + moduleId);
                    return new SlowmotionVideoModule(app);
                }
            });
    }

    private static void registerUcamFilterPhotoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    if (CameraUtil.isUseSprdFilter()) {
                        Log.d(TAG,"Create FilterModuleSprd");
                        return new FilterModuleSprd(app);
                    }
                    return null;
                }
            });
    }

    private static void registerIntentCaptureModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new DreamIntentCaptureModule(app);
            }
        });
    }


    private static void registerAudioPictureModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app,
                                                     Intent intent) {
                    return new AudioPictureModule(app);
                }
            });
    }

    private static void registerTDPhotoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new TDPhotoModule(app);
                }
            });
    }

    /*  SPRD: Fix bug 585183 Adds new features 3D recording @{ */
    private static void registerTDVideoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new TDVideoModule(app);
                }
            });
    }

    private static void registerQrCodeModule(ModuleManager moduleManager,
                                             final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app,
                                                     Intent intent) {
                    return new QrCodePhotoModule(app);
                }
            });
    }

    /*  SPRD: Fix bug 585183 Adds new feature real-time distance measurement @{ */
    private static void registerTDRangFindModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new TDRangeFindModule(app);
                }
            });
    }

    private static void registerTDNRPhotoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new TDNRPhotoModule(app);
                }
            });
    }

    private static void registerTDNRVideoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return new TDNRVideoModule(app);
                }
            });
    }

    private static void registerTDNRProPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new TDNRProPhotoModule(app);
            }
        });
    }

    private static void registerTDNRProVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent()  {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new TDNRProVideoModule(app);
            }
        });
    }

    public static void registerARPhotoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return null;
                }
            });
    }

    public static void registerARVideoModule(ModuleManager moduleManager, final int moduleId) {
            moduleManager.registerModule(new ModuleManager.ModuleAgent() {
                @Override
                public int getModuleId() {
                    return moduleId;
                }

                @Override
                public boolean requestAppForCamera() {
                    return true;
                }

                @Override
                public ModuleController createModule(AppController app, Intent intent) {
                    return null;
                }
            });
    }

    public static void registerUltraWideAngleModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new UltraWideAngleModule(app);
            }
        });

    }

    public static void registerPortraitPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new PortraitPhotoModule(app);
            }
        });
    }
    public static void registerHighResolutionPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new HighResolutionPhotoModule(app);
            }
        });
    }

    public static void registerIRPhotoModule(ModuleManager moduleManager, final int moduleId) {

        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new IRPhotoModule(app);
            }
        });
    }

	public static void registerFocusLengthFusionPhotoModule(ModuleManager moduleManager, final int moduleId) {

        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new FocusLengthFusionPhotoModule(app);
            }
        });
    }

    public static void registerMacroPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new MacroPhotoModule(app);
            }
        });
    }

    public static void registerMacroVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new MacroVideoModule(app);
            }
        });
    }
    public static void registerPortraitBackgroundReplacementPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new PortraitBackgroundReplacementPhotoModule(app);
            }
        });
    }
    public static void registerPortraitBackgroundReplacementVideoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new PortraitBackgroundReplacementVideoModule(app);
            }
        });
    }
    public static void registerFDRPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                return true;
            }

            @Override
            public ModuleController createModule(AppController app, Intent intent) {
                return new FDRPhotoModule(app);
            }
        });
    }
}
