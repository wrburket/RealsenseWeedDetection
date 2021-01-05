#include <jni.h>
#include <string>

#include "librealsense2/rs.hpp"
#include <jni.h>
#include <jni.h>
#include <jni.h>


extern "C"
JNIEXPORT jint

JNICALL
Java_com_example_myapplication_MainActivity_nGetCamerasCountFromJNI(JNIEnv *env, jclass clazz) {
    rs2::context ctx;
    return ctx.query_devices().size();;
    // TODO: implement nGetCamerasCountFromJNI()
}

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_example_myapplication_MainActivity_nGetLibrealsenseVersionFromJNI(JNIEnv *env, jclass clazz) {
    return (*env).NewStringUTF(RS2_API_VERSION_STR);
    // TODO: implement nGetLibrealsenseVersionFromJNI()
}
