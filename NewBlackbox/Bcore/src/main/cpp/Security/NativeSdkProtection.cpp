#include <jni.h>
#include <android/log.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <setjmp.h>
#include <dlfcn.h>

#define LOG_TAG "NativeSdkProtection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static sigjmp_buf crash_jmp_buf;

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_mediateLibraryLoadingNative(JNIEnv*, jobject) {
    LOGD("mediateLibraryLoadingNative enabled");
}

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_ensureSignalCompatibilityNative(JNIEnv*, jobject) {
    // Do not replace UE4's signal handlers.  Unreal and Android's debuggerd
    // both depend on their own signal chains; installing a competing handler
    // here caused native startup instability in the loader process.
    LOGD("ensureSignalCompatibilityNative: using UE4-safe no-op");
}

// Optional: JNI function to set a recovery point (called from Java before running game code)
extern "C" JNIEXPORT jboolean JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_setCrashRecoveryPoint(JNIEnv*, jobject) {
    if (sigsetjmp(crash_jmp_buf, 1) == 0) {
        return JNI_TRUE;  // First time, set the point
    } else {
        LOGD("Recovered from SIGSEGV via longjmp");
        return JNI_FALSE; // Recovered from crash
    }
}

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_security_SdkProtectionManager_virtualizePathsNative(JNIEnv*, jobject) {
    LOGD("virtualizePathsNative enabled");
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("NativeSdkProtection loaded");
    return JNI_VERSION_1_6;
}
