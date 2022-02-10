/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_android_camera_superresolution_SuperResolutionNative */

#ifndef _Included_com_android_camera_superresolution_SuperResolutionNative
#define _Included_com_android_camera_superresolution_SuperResolutionNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_android_camera_superresolution_SuperResolutionNative
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jint

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_init
        (JNIEnv *, jobject);
/*
 * Class:     com_android_camera_superresolution_SuperResolutionNative
 * Method:    deinit
 * Signature: ()I
 */
JNIEXPORT jint

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_deinit
        (JNIEnv *, jobject);

/*
 * Class:     com_android_camera_superresolution_SuperResolutionNative
 * Method:    process
 * Signature: ([B[BII)[B
 */
JNIEXPORT jbyteArray

JNICALL Java_com_android_camera_superresolution_SuperResolutionNative_process
        (JNIEnv *env, jobject thiz, jbyteArray image, jint image_width,
         jint image_height, jint scale);

#ifdef __cplusplus
}
#endif
#endif
