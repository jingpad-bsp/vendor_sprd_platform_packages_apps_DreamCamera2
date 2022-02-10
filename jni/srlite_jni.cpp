//
// Created by SPREADTRUM\yu.wang on 20-2-11.
//
#include <jni.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <android/log.h> //需要在mk中添加引用

#include "srlite_jni.h"
//#include "SrLite.h"
#include "srlite_xnnc.h"

#ifndef LOG_TAG
#define LOG_TAG "srlite_jni"
#define LOGV(...) __android_log_print(ANDROID_LOG_SILENT, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

using namespace srlite::SrLite;
void *handle = 0;
//char* input_masked = "/storage/emulated/0/input_masked.bmp";
//char* input_mask = "/storage/emulated/0/input_mask.bmp";

JNIEXPORT jint

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_init
        (JNIEnv *, jobject) {

    LOGE("call SrLiteInit start");
//    int success = SrLiteInit(&handle, "/system/etc/models/wdsr_quant_256.tflite", 4,
//                                  false);
    int success = SrLiteXNNCInit(&handle, "wdsr", 4,1,"/system/firmware/");
    //LOGE("call SrLiteInit end %d", success);
    LOGE("call SrLiteInit end");
    return 0;
}

JNIEXPORT jint

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_deinit
        (JNIEnv *, jobject) {
    LOGE(" call SrLiteDeInit start");
//    int success = 0;
//    int success = SrLiteDeInit(handle);
    int success = SrLiteXNNCDeInit(handle);
    //LOGE(" call SrLiteDeInit end %d", success);
    LOGE(" call SrLiteDeInit end");
    return success;
}

JNIEXPORT jbyteArray

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_process
        (JNIEnv *env, jobject thiz, jbyteArray image, jint image_width,
         jint image_height, jint scale) {

    int image_channels = 3;
    LOGE("process start");
    std::vector<uint8_t> pred(image_width * image_height * image_channels * scale * scale);

    jbyte *image_byte = env->GetByteArrayElements(image, 0);

    LOGE("image size: %d x %d, image channel: %d scale: %d", image_width, image_height, image_channels, scale);
//    int result = SrLiteRun(handle, (uint8_t *) image_byte, image_width,
//                                image_height,
//                                image_channels, pred.data(),stride,scale);
    int result = SrLiteXNNCRun(handle, (uint8_t *) image_byte, image_width,
                                image_height,
                                image_channels, pred.data(),320,scale,320,320);

    LOGE("SrLiteRun %d ", result);

    env->ReleaseByteArrayElements(image, image_byte, 0);

    jbyteArray bytes = env->NewByteArray(image_width * image_height * image_channels * scale * scale);
    if (bytes != 0) {
        env->SetByteArrayRegion(bytes, 0, image_width * image_height * image_channels * scale * scale,
                                (jbyte *) pred.data());
    }
    LOGE("process end");
    return bytes;
}

