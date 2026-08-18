#include <jni.h>
#include <string>
#include <backends/oxorany.h>
#include <sys/stat.h>
#include "backends/ModsLoader.h"

extern "C" JNIEXPORT jstring JNICALL 
Java_com_zoro_loader_BoxApplication_BoxApp(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF(oxorany("TESTINGSDK"));
}
