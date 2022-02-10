LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
#LOCAL_DEX_PREOPT:=false
##################################### compile config ###########################
#filter feature compile config
ifeq ($(strip $(PRODUCT_USE_CAM_FILTER)),false)
FILTER_FEATURE_SWITCH_SPRD:=false
else
FILTER_FEATURE_SWITCH_SPRD:=true
endif
#panorama feature compile config
ifeq ($(strip $(PRODUCT_USE_CAM_PANORAMA)),false)
PANORAMA_FEATURE_SWITCH:=false
else
PANORAMA_FEATURE_SWITCH:=true
endif

##################################### feature source file ##########################
#sprd filter feature source file dir
FILTER_SOURCE_FILE_DIR_SPRD += $(call all-java-files-under, src/com/dream/camera/filter/sprd)
FILTER_SOURCE_FILE_DIR_SPRD += $(call all-java-files-under, src/com/dream/camera/modules/filter/sprd)
#panorama feature source file dir
PANORAMA_SOURCE_FILE_DIR += $(call all-java-files-under, src/com/dream/camera/modules/panoramadream)
PANORAMA_SOURCE_FILE_DIR += $(call all-java-files-under, src/com/sprd/camera/panora)

##################################### feature res file ############################
#public filter feature res file dir
FILTER_RES_FILE_DIR += $(LOCAL_PATH)/res_filter
#sprd filter feature res file dir
FILTER_RES_FILE_DIR_SPRD += $(LOCAL_PATH)/res_filter_sprd

################################### java static lib compile #######################
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += android-ex-camera2-portability
LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit
LOCAL_STATIC_JAVA_LIBRARIES += guava
LOCAL_STATIC_JAVA_LIBRARIES += jsr305
#LOCAL_JAVA_LIBRARIES += sprd-framework
LOCAL_STATIC_JAVA_LIBRARIES += zxing

#sprd filter jar
ifeq ($(strip $(FILTER_FEATURE_SWITCH_SPRD)),true)
    LOCAL_STATIC_JAVA_LIBRARIES += sprdfilter
endif

################################### src compile ######################################
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd)
#LOCAL_SRC_FILES += $(call all-java-files-under, src_pd_gcam)
LOCAL_SRC_FILES += src/com/sprd/gallery3d/aidl/IFloatWindowController.aidl


#remove the sprd filter source file
ifeq ($(strip $(FILTER_FEATURE_SWITCH_SPRD)),false)
$(foreach v,$(FILTER_SOURCE_FILE_DIR_SPRD),\
    $(eval LOCAL_SRC_FILES := $(filter-out $(v), $(LOCAL_SRC_FILES)))\
)
LOCAL_SRC_FILES += $(call all-java-files-under, src_fake/com/dream/camera/modules/filter/sprd)
endif

# panorama compile config start
ifeq ($(strip $(PANORAMA_FEATURE_SWITCH)),false)
$(foreach v,$(PANORAMA_SOURCE_FILE_DIR),\
    $(eval LOCAL_SRC_FILES := $(filter-out $(v), $(LOCAL_SRC_FILES)))\
)
#LOCAL_SRC_FILES += $(call all-java-files-under, src_fake/src/com/sprd/camera/panora)
LOCAL_SRC_FILES += $(call all-java-files-under, src_fake/com/dream/camera/modules/panorama)
endif
#filterend

################################### res compile ######################################
LOCAL_RESOURCE_DIR += \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/res_p \
    $(FILTER_RES_FILE_DIR)

ifeq ($(strip $(FILTER_FEATURE_SWITCH_SPRD)),true)
    LOCAL_RESOURCE_DIR += $(FILTER_RES_FILE_DIR_SPRD)
endif

################################## add dream mk ##########################################
DREAM_UI:=true
ifeq ($(strip $(TARGET_TS_DRAM_UI_NENABLE)),false)
    DREAM_UI := false
endif

ifeq ($(strip $(DREAM_UI)),true)
ifneq ($(wildcard $(LOCAL_PATH)/Dream.mk),)
-include $(LOCAL_PATH)/Dream.mk
endif
endif

include $(LOCAL_PATH)/version.mk
LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --version-name "$(version_name_package)" \
        --version-code $(version_code_package) \
#                --extra-packages com.ucamera.UCamera \
#        --extra-packages com.ucamera.ugallery:com.ucamera.ucomm.puzzle:com.ucamera.ucomm.sns:com.ucamera.ucomm.downloadcenter \

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := DreamCamera2
LOCAL_OVERRIDES_PACKAGES := Camera2
LOCAL_CERTIFICATE := platform
#LOCAL_SDK_VERSION := current
LOCAL_PRIVATE_PLATFORM_APIS := true
#LOCAL_DEX_PREOPT:=false
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifeq ($(PRODUCT_USE_DYNAMIC_PARTITIONS),true)
LOCAL_PRODUCT_MODULE := true
endif

LOCAL_JNI_SHARED_LIBRARIES := libjni_tinyplanet_dream libjni_jpegutil_dream libjni_sprd_srlite libSrLiteXNNC libcamIP
LOCAL_REQUIRED_MODULES := libjni_mosaic_dream libself_portrait_jni libjni_mosaic_new libSprdImageFilterResource libSrLiteXNNC libjni_sprd_srlite libcamIP
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := zxing:libs/core.jar \
                                        sprdfilter:libs/SprdImageFilter.jar \

include $(BUILD_MULTI_PREBUILT)

ifeq ($(strip $(FILTER_FEATURE_SWITCH_SPRD)),true)
include $(CLEAR_VARS)
LIB_NAME := libSprdImageFilterResource
LOCAL_MODULE := $(LIB_NAME)
LOCAL_MODULE_STEM_32 := $(LIB_NAME).so
LOCAL_MODULE_STEM_64 := $(LIB_NAME).so
LOCAL_MODULE_TAGS := optional
#LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
ifeq ($(strip $(TARGET_ARCH)),arm64)
LOCAL_SRC_FILES_64 := libs/arm64-v8a/libSprdImageFilterResource.so
else
ifeq ($(strip $(TARGET_ARCH)),x86_64)
LOCAL_SRC_FILES_64 := libs/x86-64/libSprdImageFilterResource.so
else
ifeq ($(strip $(TARGET_ARCH)),arm)
LOCAL_SRC_FILES_32 := libs/armeabi-v7a/libSprdImageFilterResource.so
else
LOCAL_SRC_FILES_32 := libs/x86/libSprdImageFilterResource.so
endif
endif
endif

ifeq ($(PRODUCT_USE_DYNAMIC_PARTITIONS),true)
LOCAL_PRODUCT_MODULE := true
endif

include $(BUILD_PREBUILT)
endif

ifeq ($(strip $(PANORAMA_FEATURE_SWITCH)),true)
include $(CLEAR_VARS)
LIB_NAME := libjni_mosaic_new
LOCAL_MODULE := $(LIB_NAME)
LOCAL_MODULE_STEM_32 := $(LIB_NAME).so
LOCAL_MODULE_STEM_64 := $(LIB_NAME).so
LOCAL_MODULE_TAGS := optional
#LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
ifeq ($(strip $(TARGET_ARCH)),arm64)
LOCAL_SRC_FILES_64 := libs/arm64-v8a/libjni_mosaic_new.so
else
ifeq ($(strip $(TARGET_ARCH)),x86_64)
LOCAL_SRC_FILES_64 := libs/x86-64/libjni_mosaic_new.so
else
LOCAL_SRC_FILES := libs/armeabi-v7a/libjni_mosaic_new.so
endif
endif

ifeq ($(PRODUCT_USE_DYNAMIC_PARTITIONS),true)
LOCAL_PRODUCT_MODULE := true
endif

include $(BUILD_PREBUILT)
endif

#vendor prebuild lib end

include $(CLEAR_VARS)
LIB_NAME := libSrLiteXNNC
LOCAL_MODULE := $(LIB_NAME)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MULTILIB := both
LOCAL_MODULE_STEM_32 := $(LIB_NAME).so
LOCAL_MODULE_STEM_64 := $(LIB_NAME).so
LOCAL_SRC_FILES_64 := libs/arm64-v8a/libSrLiteXNNC.so
LOCAL_SRC_FILES_32 := libs/armeabi-v7a/libSrLiteXNNC.so
ifeq ($(PRODUCT_USE_DYNAMIC_PARTITIONS),true)
LOCAL_PRODUCT_MODULE := true
endif
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LIB_NAME := libcamIP
LOCAL_MODULE := $(LIB_NAME)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MULTILIB := both
LOCAL_MODULE_STEM_32 := $(LIB_NAME).so
LOCAL_MODULE_STEM_64 := $(LIB_NAME).so
LOCAL_SRC_FILES_64 := libs/arm64-v8a/libcamIP.so
LOCAL_SRC_FILES_32 := libs/armeabi-v7a/libcamIP.so
ifeq ($(PRODUCT_USE_DYNAMIC_PARTITIONS),true)
LOCAL_PRODUCT_MODULE := true
endif
include $(BUILD_PREBUILT)

include $(call all-makefiles-under, $(LOCAL_PATH))
